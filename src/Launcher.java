import service.*;
import service.UpdateManager.ReleaseInfo;
import ui.SwingUpdatePrompt;
import util.AppLogger;
import util.AppPaths;
import util.ConfigManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Launcher {

    private static final String DEV_VERSION = "dev";

    public static void main(String[] args) {
        AppPaths.init();

        AuthService authService = new AuthService();
        AchievementService achievementService = new AchievementService();
        LevelService levelService = new LevelService();
        WorkflowService workflowService = new WorkflowService();
        SystemInfoService systemInfoService = new SystemInfoService();
        BDaysService bdaysService = new BDaysService();
        GreetingService greetingService = new GreetingService();
        RunningProcessService runningProcessService = new RunningProcessService();
        UserSession userSession = new UserSession();
        NotificationService notificationService = new NotificationService();

        Services services = new Services(
            authService, achievementService, levelService, workflowService,
            systemInfoService, bdaysService, greetingService, runningProcessService,
            userSession, notificationService
        );

        String currentVersion = readVersionFromManifest();
        AppLogger.info("Current version: " + currentVersion);

        if (!DEV_VERSION.equals(currentVersion)) {
            String skippedVersion = ConfigManager.loadSkippedVersion();

            UpdateManager updateManager = new UpdateManager();
            ReleaseInfo release = null;
            try {
                release = updateManager.fetchLatestRelease();
            } catch (Exception e) {
                AppLogger.error("Launcher: failed to check for updates: " + e.getMessage());
            }

            if (release != null) {
                boolean isNewer = updateManager.compareVersions(release.version(), currentVersion) > 0;
                boolean isSkipped = release.version().equals(skippedVersion);

                if (isNewer && !isSkipped) {
                    AppLogger.info("Update available: " + release.version());

                    SwingUpdatePrompt.Choice choice = SwingUpdatePrompt.show(
                            currentVersion, release.version(), release.notesMarkdown()
                    );

                    if (choice == SwingUpdatePrompt.Choice.UPDATE) {
                        try {
                            Path tempJar = Files.createTempFile("javamultitool-", ".jar");
                            updateManager.downloadRelease(release, tempJar);

                            String downloadedVersion = updateManager.readJarVersion(tempJar);
                            if (downloadedVersion == null) {
                                AppLogger.info("Could not verify downloaded JAR version, applying anyway");
                            }

                            UpdateApplier updateApplier = new UpdateApplier();
                            updateApplier.restartWithNewJar(tempJar);
                            return;
                        } catch (IOException | InterruptedException e) {
                            AppLogger.error("Launcher: update failed: " + e.getMessage());
                        }
                    } else {
                        ConfigManager.saveSkippedVersion(release.version());
                    }
                }
            }
        }

        Main.start(services, args);
    }

    private static String readVersionFromManifest() {
        try (InputStream in = Launcher.class.getResourceAsStream("/META-INF/MANIFEST.MF")) {
            if (in == null) return DEV_VERSION;
            Properties props = new Properties();
            props.load(in);
            String v = props.getProperty("Implementation-Version");
            return (v != null && !v.isBlank()) ? v.trim() : DEV_VERSION;
        } catch (Exception e) {
            return DEV_VERSION;
        }
    }
}

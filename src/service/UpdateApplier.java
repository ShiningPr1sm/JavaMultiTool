package service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UpdateApplier {
    private static final long RELAUNCH_DELAY_SECONDS = 2; // same wait the update .bat uses

    public void restartWithNewJar(Path newJar) {
        try {
            Path currentJarPath = Paths.get(
                    UpdateApplier.class.getProtectionDomain()
                            .getCodeSource().getLocation().toURI()
            ).toAbsolutePath();
            Path newJarPath = newJar.toAbsolutePath();
            Path scriptPath = currentJarPath.getParent().resolve("javamultitool_update.bat");

            String script = "@echo off\r\n" +
                    "timeout /t " + RELAUNCH_DELAY_SECONDS + " /nobreak > nul\r\n" +
                    ":loop\r\n" +
                    "del /f \"" + currentJarPath + "\"\r\n" +
                    "if exist \"" + currentJarPath + "\" (\r\n" +
                    "  timeout /t 1 > nul\r\n" +
                    "  goto loop\r\n" +
                    ")\r\n" +
                    "move /y \"" + newJarPath + "\" \"" + currentJarPath + "\"\r\n" +
                    "start javaw -jar \"" + currentJarPath + "\"\r\n" +
                    "del \"%~f0\"\r\n";

            Files.writeString(scriptPath, script);
            new ProcessBuilder("cmd", "/c", "start", "", scriptPath.toString()).start();
            System.exit(0);
        } catch (Exception e) {
            util.AppLogger.error("UpdateApplier: " + e.getMessage());
        }
    }

    /**
     * Schedules a relaunch of the running jar in a fresh process (language restart).
     * Exits the JVM on success, so this call never returns in that case.
     *
     * @return true if a relaunch was scheduled, false when not running from a jar (dev)
     */
    public static boolean relaunchApp() {
        try {
            Path jarPath = Paths.get(
                    UpdateApplier.class.getProtectionDomain()
                            .getCodeSource().getLocation().toURI()
            ).toAbsolutePath();
            if (!jarPath.getFileName().toString().endsWith(".jar")) {
                return false; // running from IDE/classes - caller keeps the manual-restart flow
            }
            Path scriptPath = jarPath.getParent().resolve("javamultitool_relaunch.bat");
            String script = "@echo off\r\n" +
                    "timeout /t " + RELAUNCH_DELAY_SECONDS + " /nobreak > nul\r\n" +
                    "start javaw -jar \"" + jarPath + "\"\r\n" +
                    "del \"%~f0\"\r\n";

            Files.writeString(scriptPath, script);
            new ProcessBuilder("cmd", "/c", "start", "", scriptPath.toString()).start();
            System.exit(0);
            return true;
        } catch (Exception e) {
            util.AppLogger.error("UpdateApplier: relaunch failed - " + e.getMessage());
            return false;
        }
    }
}
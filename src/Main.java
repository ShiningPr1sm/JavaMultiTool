import db.DatabaseProvider;
import ui.MainFrame;
import ui.AuthFrame;
import service.AchievementService;
import service.SystemInfoService;
import service.Services;
import ui.UIStyle;
import util.AppLogger;
import util.AppPaths;
import service.AuthService;
import javax.swing.*;

public class Main {
    public static void start(Services services, String[] args) {
        AppLogger.info("=== Application Starting ===");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            AppLogger.error("UIManager initialization: " + e.getMessage());
        }

        try {
            DatabaseProvider.getUserRepository().initializeDatabase();
            services.authService().initializeRights();
            DatabaseProvider.getWorkflowRepository().initializeDatabase();
            AppLogger.info("Core databases and rights initialized.");
        } catch (Exception e) {
            AppLogger.error("Core initialization failed: " + e.getMessage());
        }

        try {
            services.achievementService().initialize();
            AppLogger.info("Achievements system ready.");
        } catch (Exception e) {
            AppLogger.error("Achievements system failed: " + e.getMessage());
        }

        var userRepo = DatabaseProvider.getUserRepository();
        String savedLogin = userRepo.getAutoLoginUser();
        SwingUtilities.invokeLater(() -> {
            if (savedLogin != null && !savedLogin.isBlank()) {
                String userTheme = userRepo.getTheme(savedLogin);
                UIStyle.applyTheme(userTheme);
                userRepo.updateLastLoginDate(savedLogin);

                new MainFrame(savedLogin, services);
                AppLogger.info("Auto-login: User '" + savedLogin + "' entered the system.");
            } else {
                new AuthFrame(services);
                AppLogger.info("Waiting for manual login...");
            }
        });
        SystemInfoService unused = services.systemInfoService();
        unused.prepare();
    }
}

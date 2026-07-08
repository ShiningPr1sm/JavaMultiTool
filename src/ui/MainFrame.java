package ui;

import db.DatabaseProvider;
import service.*;
import ui.achievements.AchievementsPanel;
import ui.admin.AdminLogPanel;
import ui.daytab.BDaysNotifierPanel;
import ui.daytab.WorkflowPanel;
import ui.photovideotab.ImageToolsPanel;
import ui.photovideotab.MediaDownloaderPanel;
import ui.settings.SettingsPanel;
import ui.components.ExpandableSection;
import ui.utils.*;
import util.AchievementCallback;
import util.AppLogger;
import util.VersionInfo;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame implements AchievementCallback {
    private static final int SIDEBAR_WIDTH = 230;
    private static final int HEADER_HEIGHT = 70;
    private static final int FRAME_SIZE_WIDTH = 1200;
    private static final int FRAME_SIZE_HEIGHT = 720;

    private HeaderPanel headerPanel;
    private final String login;
    private JPanel contentPanel;
    private TrayManager trayManager;
    private final Services services;

    public MainFrame(String login, Services services) {
        this.login = login;
        this.services = services;
        services.userSession().setLogin(login);

        services.levelService().initialize(login);

        setTitle("MultiTool  |  v:" + VersionInfo.getVersion());
        setAppIcon();
        setResizable(false);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                handleClose();
            }
        });

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(
                (int) ((screenSize.getWidth() - FRAME_SIZE_WIDTH) / 2),
                (int) ((screenSize.getHeight() - FRAME_SIZE_HEIGHT) / 2),
                FRAME_SIZE_WIDTH,
                FRAME_SIZE_HEIGHT
        );

        headerPanel = new HeaderPanel(login, services.levelService(), services.achievementService(), services.authService(), this::openAchievements, this::openNotifications, this::openSettings);
        add(headerPanel, BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);

        services.achievementService().initialize();
        services.achievementService().syncUser(login);
        services.achievementService().addCallback(this);
        services.achievementService().complete(login, "first_login");

        if (services.levelService().getLevel(login) == 10) {
            services.achievementService().complete(login, "reach_10lvl");
        }

        contentPanel.removeAll();
        WelcomePanel welcomePanel = new WelcomePanel(login, services.greetingService());
        contentPanel.add(welcomePanel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();

        services.workflowService().startTracking();

        this.trayManager = new TrayManager(this);

        int notifCount = services.notificationService().checkBirthdayReminders();
        if (notifCount > 0) {
            headerPanel.setNotificationBadge(notifCount + services.notificationService().countActive());
        } else {
            int active = services.notificationService().countActive();
            if (active > 0) {
                headerPanel.setNotificationBadge(active);
            }
        }

        setVisible(true);
    }



    private JPanel createMainContent() {
        JPanel main = new JPanel(new BorderLayout());
        main.add(createSidebar(), BorderLayout.WEST);
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(UIStyle.BG_COLOR);
        main.add(contentPanel, BorderLayout.CENTER);

        return main;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(UIStyle.SIDE_BOX);
        sidebar.setPreferredSize(new Dimension(SIDEBAR_WIDTH, getHeight()));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(new ExpandableSection("Photo & Video",
                new String[]{"Media Downloader", "File Organizer", "Image Tools"},
                SIDEBAR_WIDTH, this::openTab));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(new ExpandableSection("Math", new String[]{"Unit Converter"},
                SIDEBAR_WIDTH, this::openTab));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(new ExpandableSection("Text", new String[]{"Find & Replace"},
                SIDEBAR_WIDTH, this::openTab));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(new ExpandableSection("Time", new String[]{"Workflow", "BDays notifier"},
                SIDEBAR_WIDTH, this::openTab));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        if (services.authService().isTester()) {
            sidebar.add(new ExpandableSection("Admin Panel", new String[]{"Admin CMD"},
                    SIDEBAR_WIDTH, this::openTab));
            sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        }
        return sidebar;
    }

    private LoadingPanel createLoadingPanel() {
        return new LoadingPanel();
    }

    private void openAchievements() {
        contentPanel.removeAll();
        contentPanel.add(new AchievementsPanel(login, services.achievementService()), BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    public void openSettings() {
        contentPanel.removeAll();

        LoadingPanel loadingPanel = createLoadingPanel();
        contentPanel.add(loadingPanel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();

        new SwingWorker<SettingsPanel, Void>() {
            @Override
            protected SettingsPanel doInBackground() {
                return new SettingsPanel(MainFrame.this, login, services.achievementService(), services.systemInfoService(), services);
            }

            @Override
            protected void done() {
                try {
                    SettingsPanel settingsPanel = get();
                    loadingPanel.fadeOut(() -> {
                        contentPanel.removeAll();
                        contentPanel.add(settingsPanel, BorderLayout.CENTER);
                        contentPanel.revalidate();
                        contentPanel.repaint();
                    });
                } catch (Exception ex) {
                    AppLogger.error("Failed to load Settings: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void openNotifications() {
        openTab("Notifications");
        headerPanel.setNotificationBadge(0);
    }

    public void openTab(String itemName) {
        contentPanel.removeAll();

        switch (itemName) {
            case "Media Downloader" ->
                    contentPanel.add(new MediaDownloaderPanel(), BorderLayout.CENTER);
            case "BDays notifier" ->
                    contentPanel.add(new BDaysNotifierPanel(login, services.bdaysService(), services.achievementService(), services.userSession()), BorderLayout.CENTER);
            case "Workflow" ->
                    contentPanel.add(new WorkflowPanel(services.workflowService(), services.runningProcessService()), BorderLayout.CENTER);
            case "Admin CMD" ->
                    contentPanel.add(new AdminLogPanel(), BorderLayout.CENTER);
            case "Settings" ->
                    openSettings();
            case "Image Tools" ->
                    contentPanel.add(new ImageToolsPanel(), BorderLayout.CENTER);
            case "Notifications" ->
                    contentPanel.add(new ui.notifications.NotificationsPanel(services.notificationService(), headerPanel), BorderLayout.CENTER);
            default ->
                    AppLogger.error("Attempted to open unknown tab: " + itemName);
        }

        contentPanel.revalidate();
        contentPanel.repaint();

        AppLogger.info("Tab switched to: " + itemName);
    }

    private void handleClose() {
        boolean trayEnabled = DatabaseProvider.getUserRepository().isCloseToTrayEnabled(login);
        AppLogger.info("Close clicked. Setting 'CloseToTray' is: " + trayEnabled);

        if (trayEnabled) {
            setVisible(false);
            AppLogger.info("Application minimized to system tray.");
        } else {
            AppLogger.info("Exiting application...");
            System.exit(0);
        }
    }

    @Override
    public void onAchievementLevelUp(String user, int amount) {
        services.levelService().addXP(user, amount);
        headerPanel.showXpGain(amount);
    }

    @Override
    public void onAchievementNotification(int amount) {
        headerPanel.showXpGain(amount);
    }

    public void updateAvatarImage(ImageIcon newIcon) {
        headerPanel.updateAvatarImage(newIcon);
    }

    public void updateNickName(String nickname) {
        headerPanel.updateNickName(nickname);
    }

    private void setAppIcon() {
        UIStyle.setAppIcon(this);
    }
}

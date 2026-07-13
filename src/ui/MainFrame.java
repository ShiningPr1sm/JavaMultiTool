package ui;

import db.DatabaseProvider;
import service.*;
import ui.achievements.AchievementsPanel;
import ui.admin.AdminLogPanel;
import ui.daytab.BirthdayTrackerPanel;
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
import java.net.URI;
import java.util.Objects;

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
    private AchievementsPanel achievementsPanel;
    private WorkflowPanel workflowPanel;
    private JLabel actualVerLabel;

    public MainFrame(String login, Services services) {
        this.login = login;
        this.services = services;
        services.userSession().setLogin(login);

        services.levelService().initialize(login);

        updateTitle("Welcome");
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

        headerPanel = new HeaderPanel(login, services.levelService(), services.achievementService(), services.authService(), this::openAchievements, this::openNotifications, this::openSettings, this::openWelcome);
        add(headerPanel, BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);

        services.achievementService().initialize();
        services.achievementService().syncUser(login);
        services.achievementService().addCallback(this);
        services.achievementService().complete(login, "first_login");

        if (services.levelService().getLevel(login) == 10) {
            services.achievementService().complete(login, "reach_10lvl");
        }

        contentPanel.removeAll();
        WelcomePanel welcomePanel = new WelcomePanel(login, services.greetingService(), services.quoteService().getDailyQuote());
        contentPanel.add(welcomePanel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();

        services.workflowService().startTracking();

        this.trayManager = new TrayManager(this);

        services.notificationService().checkBirthdayReminders();
        int active = services.notificationService().countActive();
        if (active > 0) {
            headerPanel.setNotificationBadge(active);
        }

        String lastLogin = DatabaseProvider.getUserRepository().getLastLoginDate(login);
        String today = java.time.LocalDate.now().toString();
        if (lastLogin == null || !lastLogin.startsWith(today)) {
            services.levelService().addXP(login, 20);
            headerPanel.showXpGain(20);
            AppLogger.info("Added 20 EXP for first login this day.");
        }
        DatabaseProvider.getUserRepository().updateLastLoginDate(login);

        setVisible(true);

        new SwingWorker<AchievementsPanel, Void>() {
            @Override
            protected AchievementsPanel doInBackground() {
                return new AchievementsPanel(login, services.achievementService());
            }

            @Override
            protected void done() {
                try {
                    achievementsPanel = get();
                } catch (Exception e) {
                    AppLogger.error("Failed to pre-load AchievementsPanel: " + e.getMessage());
                }
            }
        }.execute();
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
        sidebar.add(new ExpandableSection("Time", new String[]{"Workflow", "Birthday Tracker"},
                SIDEBAR_WIDTH, this::openTab));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(new ExpandableSection("Utils", new String[]{"Color Picker/Converter", "Password Generator", "QR Generator & Decoder", "Network Tools"},
                SIDEBAR_WIDTH, this::openTab));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        if (services.authService().isTester()) {
            sidebar.add(new ExpandableSection("Admin Panel", new String[]{"Admin CMD"},
                    SIDEBAR_WIDTH, this::openTab));
            sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        }
        return sidebar;
    }

    private void openWelcome() {
        contentPanel.removeAll();
        contentPanel.add(new WelcomePanel(login, services.greetingService(), services.quoteService().getDailyQuote()), BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
        updateTitle("Welcome");
    }

    private void openAchievements() {
        contentPanel.removeAll();
        if (achievementsPanel != null) {
            contentPanel.add(achievementsPanel, BorderLayout.CENTER);
        } else {
            contentPanel.add(new AchievementsPanel(login, services.achievementService()), BorderLayout.CENTER);
        }
        contentPanel.revalidate();
        contentPanel.repaint();
        updateTitle("Achievements");
    }

    public void openSettings() {
        contentPanel.removeAll();
        contentPanel.add(new SettingsPanel(this, login, services.achievementService(), services.systemInfoService(), services), BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
        updateTitle("Settings");
    }

    private void openNotifications() {
        openTab("Notifications");
    }

    public void openTab(String itemName) {
        AppLogger.info("openTab called with: " + itemName);
        contentPanel.removeAll();

        switch (itemName) {
            case "Media Downloader" ->
                    contentPanel.add(new MediaDownloaderPanel(), BorderLayout.CENTER);
            case "Birthday Tracker" ->
                    contentPanel.add(new BirthdayTrackerPanel(login, services.bdaysService(), services.achievementService(), services.userSession()), BorderLayout.CENTER);
            case "Workflow" -> {
                    if (workflowPanel == null) {
                        workflowPanel = new WorkflowPanel(services.workflowService(), services.runningProcessService());
                    }
                    contentPanel.add(workflowPanel, BorderLayout.CENTER);
                }
            case "Admin CMD" ->
                    contentPanel.add(new AdminLogPanel(), BorderLayout.CENTER);
            case "Settings" ->
                    openSettings();
            case "Image Tools" ->
                    contentPanel.add(new ImageToolsPanel(), BorderLayout.CENTER);
            case "Color Picker/Converter" ->
                    contentPanel.add(new ui.utilstab.ColorPickerPanel(), BorderLayout.CENTER);
            case "Password Generator" ->
                    contentPanel.add(new ui.utilstab.PasswordGeneratorPanel(), BorderLayout.CENTER);
            case "QR Generator & Decoder" ->
                    contentPanel.add(new ui.utilstab.QRToolsPanel(), BorderLayout.CENTER);
            case "Network Tools" ->
                    contentPanel.add(new ui.utilstab.NetworkToolsPanel(), BorderLayout.CENTER);
            case "Notifications" ->
                    contentPanel.add(new ui.notifications.NotificationsPanel(services.notificationService(), headerPanel), BorderLayout.CENTER);
            default ->
                    AppLogger.error("Attempted to open unknown tab: " + itemName);
        }

        contentPanel.revalidate();
        contentPanel.repaint();
        updateTitle(itemName);

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

    private void updateTitle(String location) {
        setTitle("JMT / JavaMultiTool/" + location);
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.LINE_AXIS));
        footer.setBackground(UIStyle.SIDE_BOX);
        footer.setPreferredSize(new Dimension(getWidth(), 40));
        footer.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

        JLabel currentVerLabel = new JLabel("Current version: " + VersionInfo.getVersion());
        currentVerLabel.setForeground(Color.LIGHT_GRAY);
        currentVerLabel.setFont(currentVerLabel.getFont().deriveFont(11f));

        JButton githubBtn = new JButton();
        try {
            ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/icons/menu/github_icon.png")));
            githubBtn.setIcon(icon);
            githubBtn.setPreferredSize(new Dimension(16, 16));
            githubBtn.setMaximumSize(new Dimension(16, 16));
        } catch (Exception e) {
            githubBtn.setText("GitHub");
        }
        githubBtn.setBorderPainted(false);
        githubBtn.setContentAreaFilled(false);
        githubBtn.setFocusPainted(false);
        githubBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        githubBtn.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/ShiningPr1sm/JavaMultiTool"));
            } catch (Exception ex) {
                AppLogger.error("Failed to open GitHub: " + ex.getMessage());
            }
        });

        actualVerLabel = new JLabel("Actual version: checking...");
        actualVerLabel.setForeground(Color.LIGHT_GRAY);
        actualVerLabel.setFont(actualVerLabel.getFont().deriveFont(11f));

        footer.add(Box.createHorizontalGlue());
        footer.add(currentVerLabel);
        footer.add(Box.createHorizontalStrut(10));
        footer.add(actualVerLabel);
        footer.add(Box.createHorizontalStrut(10));
        footer.add(githubBtn);

        fetchActualVersion();
        return footer;
    }

    private void fetchActualVersion() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                UpdateManager.ReleaseInfo release = new UpdateManager().fetchLatestRelease();
                return release != null ? release.version() : null;
            }

            @Override
            protected void done() {
                try {
                    String ver = get();
                    actualVerLabel.setText("Actual version: " + (ver != null ? ver : "unavailable"));
                } catch (Exception e) {
                    actualVerLabel.setText("Actual version: unavailable");
                }
            }
        }.execute();
    }
}

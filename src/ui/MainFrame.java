package ui;

import db.DatabaseProvider;
import service.*;
import ui.achievements.AchievementsPanel;
import ui.admin.AdminLogPanel;
import ui.daytab.BirthdayTrackerPanel;
import ui.daytab.WorkflowPanel;
import ui.dailytracker.DailyTrackerPanel;
import ui.photovideotab.ImageToolsPanel;
import ui.photovideotab.MediaDownloaderPanel;
import ui.settings.SettingsPanel;

import ui.components.ExpandableSection;
import ui.components.ProgressBarFooter;
import ui.StyledDialog;
import ui.utils.*;
import util.AchievementCallback;

import javax.swing.plaf.basic.BasicScrollBarUI;
import util.AppLogger;
import util.AppPaths;
import util.ConfigManager;
import util.I18n;
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
    private String currentTab;
    private AchievementsPanel achievementsPanel;
    private WorkflowPanel workflowPanel;
    private JLabel actualVerLabel;
    private final ProgressBarFooter downloadProgress = new ProgressBarFooter();
    private final JLabel fpsLabel = new JLabel("-- FPS");
    private int frameCount;

    public MainFrame(String login, Services services) {
        this.login = login;
        this.services = services;
        services.userSession().setLogin(login);

        String nickname = DatabaseProvider.getUserRepository().getNickname(login);
        AppPaths.migrateUserData(login, nickname);
        DatabaseProvider.reset();

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

        java.util.function.Consumer<String> searchRouter = key -> {
            switch (key) {
                case "Welcome" -> openWelcome();
                case "Achievements" -> openAchievements();
                default -> openTab(key); // also covers "Settings"
            }
        };
        headerPanel = new HeaderPanel(login, services.levelService(), services.achievementService(), services.authService(), this::openAchievements, this::openNotifications, this::openSettings, this::openWelcome, searchRouter);
        add(headerPanel, BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);

        // global search shortcut: Ctrl+K focuses the header search box
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_K, java.awt.event.InputEvent.CTRL_DOWN_MASK), "openSearch");
        getRootPane().getActionMap().put("openSearch", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                headerPanel.focusSearch();
            }
        });

        if ("dev".equals(VersionInfo.getVersion())) {
            new Timer(1000, e -> {
                fpsLabel.setText(frameCount + " FPS");
                frameCount = 0;
            }).start();

            JComponent fpsGlass = new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    if (fpsLabel.isVisible()) frameCount++;
                }

                @Override
                public boolean contains(int x, int y) {
                    return false;
                }
            };
            fpsGlass.setOpaque(false);
            setGlassPane(fpsGlass);
            fpsGlass.setVisible(true);
        }

        MediaDownloadService.setDownloadProgressCallback(
                (stage, percent) -> SwingUtilities.invokeLater(() -> {
                    if (percent < 100) {
                        downloadProgress.setActive(true);
                        if (percent < 0) {
                            downloadProgress.setProgress(0, -1, I18n.t("update.progress", stage));
                        } else {
                            downloadProgress.setProgress(percent, 100, I18n.t("update.progress", stage));
                        }
                    } else {
                        downloadProgress.setActive(false);
                    }
                }));

        services.achievementService().initialize();
        services.achievementService().addCallback(this);
        services.achievementService().complete(login, "first_login");

        if (services.levelService().getLevel(login) == 10) {
            services.achievementService().complete(login, "reach_10lvl");
        }

        contentPanel.removeAll();
        WelcomePanel welcomePanel = new WelcomePanel(login, services.greetingService(), dailyQuote());
        contentPanel.add(welcomePanel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
        currentTab = "Welcome";

        services.workflowService().startTracking();

        this.trayManager = new TrayManager(this);
        ErrorToast.install(this);

        if (Boolean.parseBoolean(ConfigManager.loadProperty("birthdayReminders", "true"))) {
            services.notificationService().checkBirthdayReminders();
        }
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

    private JScrollPane createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(UIStyle.SIDE_BOX);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(new ExpandableSection("sidebar.photoVideo",
                new String[]{"sidebar.mediaDownloader", "sidebar.fileOrganizer", "sidebar.imageTools"},
                SIDEBAR_WIDTH, this::openTab));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(new ExpandableSection("sidebar.math", new String[]{"sidebar.unitConverter"},
                SIDEBAR_WIDTH, this::openTab));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(new ExpandableSection("sidebar.text", new String[]{"sidebar.findReplace"},
                SIDEBAR_WIDTH, this::openTab));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(new ExpandableSection("sidebar.time", new String[]{"sidebar.workflow", "sidebar.birthdayTracker", "sidebar.dailyTracker"},
                SIDEBAR_WIDTH, this::openTab));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(new ExpandableSection("sidebar.utils", new String[]{"sidebar.colorPicker", "sidebar.passwordGenerator", "sidebar.qrGenerator", "sidebar.networkTools"},
                SIDEBAR_WIDTH, this::openTab));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        if (services.authService().isTester()) {
            sidebar.add(new ExpandableSection("sidebar.adminPanel", new String[]{"sidebar.adminCmd"},
                    SIDEBAR_WIDTH, this::openTab));
            sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        JScrollPane scrollPane = new JScrollPane(sidebar);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));
        scrollPane.setBackground(UIStyle.SIDE_BOX);
        scrollPane.getViewport().setBackground(UIStyle.SIDE_BOX);

        JScrollBar vb = scrollPane.getVerticalScrollBar();
        vb.setUnitIncrement(24);
        vb.setPreferredSize(new Dimension(0, 0));
        vb.setUI(new BasicScrollBarUI() {
            @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {}
            @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {}
            @Override protected JButton createDecreaseButton(int o) { return zeroBtn(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroBtn(); }
            private JButton zeroBtn() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                return b;
            }
        });

        return scrollPane;
    }

    private void openWelcome() {
        if ("Welcome".equals(currentTab)) return;
        contentPanel.removeAll();
        contentPanel.add(new WelcomePanel(login, services.greetingService(), dailyQuote()), BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
        updateTitle("Welcome");
        currentTab = "Welcome";
    }

    private void openAchievements() {
        if ("Achievements".equals(currentTab)) return;
        contentPanel.removeAll();
        if (achievementsPanel != null) {
            contentPanel.add(achievementsPanel, BorderLayout.CENTER);
        } else {
            contentPanel.add(new AchievementsPanel(login, services.achievementService()), BorderLayout.CENTER);
        }
        contentPanel.revalidate();
        contentPanel.repaint();
        updateTitle("Achievements");
        currentTab = "Achievements";
    }

    public void openSettings() {
        if ("Settings".equals(currentTab)) return;
        contentPanel.removeAll();
        contentPanel.add(new SettingsPanel(this, login, services.achievementService(), services.systemInfoService(), services), BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
        updateTitle("Settings");
        currentTab = "Settings";
    }

    private void openNotifications() {
        openTab("sidebar.notifications");
    }

    /** Tabs the openTab switch handles; anything else is not implemented yet. */
    private static final java.util.Set<String> KNOWN_TABS = java.util.Set.of(
            "sidebar.mediaDownloader", "sidebar.birthdayTracker", "sidebar.dailyTracker",
            "sidebar.workflow", "sidebar.adminCmd", "Settings", "sidebar.imageTools",
            "sidebar.colorPicker", "sidebar.passwordGenerator", "sidebar.qrGenerator",
            "sidebar.networkTools", "sidebar.notifications");

    public void openTab(String itemName) {
        if (itemName.equals(currentTab)) return;
        if (!KNOWN_TABS.contains(itemName)) {
            // placeholder sidebar/search entries (File Organizer, Unit Converter,
            // Find & Replace...) - show a hint instead of silently blanking the pane
            AppLogger.info("openTab: not implemented yet: " + itemName);
            StyledDialog.show(this, I18n.t("tab.comingSoon"));
            return;
        }
        AppLogger.info("openTab called with: " + itemName);
        contentPanel.removeAll();

        switch (itemName) {
            case "sidebar.mediaDownloader" ->
                    contentPanel.add(new MediaDownloaderPanel(), BorderLayout.CENTER);
            case "sidebar.birthdayTracker" ->
                    contentPanel.add(new BirthdayTrackerPanel(login, services.bdaysService(), services.achievementService(), services.userSession()), BorderLayout.CENTER);
            case "sidebar.dailyTracker" ->
                    contentPanel.add(new DailyTrackerPanel(), BorderLayout.CENTER);
            case "sidebar.workflow" -> {
                    if (workflowPanel == null) {
                        workflowPanel = new WorkflowPanel(services.workflowService(), services.runningProcessService());
                        workflowPanel.setOnPomodoroStartCallback(() ->
                                services.achievementService().complete(login, "timer_sec"));
                    }
                    contentPanel.add(workflowPanel, BorderLayout.CENTER);
                }
            case "sidebar.adminCmd" ->
                    contentPanel.add(new AdminLogPanel(), BorderLayout.CENTER);
            case "Settings" ->
                    openSettings();
            case "sidebar.imageTools" ->
                    contentPanel.add(new ImageToolsPanel(services, login), BorderLayout.CENTER);
            case "sidebar.colorPicker" ->
                    contentPanel.add(new ui.utilstab.ColorPickerPanel(), BorderLayout.CENTER);
            case "sidebar.passwordGenerator" ->
                    contentPanel.add(new ui.utilstab.PasswordGeneratorPanel(services, login), BorderLayout.CENTER);
            case "sidebar.qrGenerator" ->
                    contentPanel.add(new ui.utilstab.QRToolsPanel(services, login), BorderLayout.CENTER);
            case "sidebar.networkTools" ->
                    contentPanel.add(new ui.utilstab.NetworkToolsPanel(), BorderLayout.CENTER);
            case "sidebar.notifications" ->
                    contentPanel.add(new ui.notifications.NotificationsPanel(services.notificationService(), headerPanel), BorderLayout.CENTER);
            default ->
                    AppLogger.error("Attempted to open unknown tab: " + itemName);
        }

        contentPanel.revalidate();
        contentPanel.repaint();
        updateTitle(itemName);
        currentTab = itemName;

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
        headerPanel.refreshAchievementsText();
        if (achievementsPanel != null) {
            achievementsPanel.refreshData();
        }
    }

    public void updateAvatarImage(ImageIcon newIcon) {
        headerPanel.updateAvatarImage(newIcon);
    }

    private String dailyQuote() {
        return Boolean.parseBoolean(ConfigManager.loadProperty("dailyQuote", "true"))
                ? services.quoteService().getDailyQuote()
                : null;
    }

    public void updateNickName(String nickname) {
        headerPanel.updateNickName(nickname);
    }

    private void setAppIcon() {
        UIStyle.setAppIcon(this);
    }

    private void updateTitle(String location) {
        setTitle("JMT / JavaMultiTool/" + tabTitle(location));
    }

    /** Display name of a tab: identity strings ("Welcome"/"Settings") map to their own
     *  keys, sidebar keys ("sidebar.xxx") translate directly. */
    private String tabTitle(String identity) {
        return switch (identity) {
            case "Welcome" -> I18n.t("main.welcome");
            case "Achievements" -> I18n.t("main.achievements");
            case "Settings" -> I18n.t("main.settings");
            case "sidebar.notifications" -> I18n.t(identity);
            default -> identity.startsWith("sidebar.") ? I18n.t(identity) : identity;
        };
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.LINE_AXIS));
        footer.setBackground(UIStyle.SIDE_BOX);
        footer.setPreferredSize(new Dimension(getWidth(), 40));
        footer.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

        JLabel currentVerLabel = new JLabel(I18n.t("main.currentVersion", VersionInfo.getVersion()));
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

        actualVerLabel = new JLabel(I18n.t("main.actualChecking"));
        actualVerLabel.setForeground(Color.LIGHT_GRAY);
        actualVerLabel.setFont(actualVerLabel.getFont().deriveFont(11f));

        fpsLabel.setForeground(Color.LIGHT_GRAY);
        fpsLabel.setFont(fpsLabel.getFont().deriveFont(11f));
        fpsLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        fpsLabel.setVisible("dev".equals(VersionInfo.getVersion()));
        footer.add(fpsLabel);
        footer.add(downloadProgress);
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
        if (!util.ConfigManager.isInternetEnabled()) {
            actualVerLabel.setText(I18n.t("main.unavailable"));
            return;
        }
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
                    actualVerLabel.setText(ver != null
                            ? I18n.t("main.actualVersion", ver)
                            : I18n.t("main.unavailable"));
                } catch (Exception e) {
                    actualVerLabel.setText(I18n.t("main.unavailable"));
                }
            }
        }.execute();
    }
}

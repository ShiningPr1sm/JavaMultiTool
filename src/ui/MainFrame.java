package ui;

import db.AchievementDB;
import db.DB;
import db.LevelManager;
import ui.achievements.AchievementsPanel;
import ui.admin.AdminLogPanel;
import ui.daytab.BDaysNotifierPanel;
import ui.daytab.WorkflowPanel;
import ui.photovideotab.ImageToolsPanel;
import ui.photovideotab.MediaDownloaderPanel;
import ui.settings.SettingsPanel;
import ui.utils.AppLogger;
import ui.utils.AuthService;
import ui.utils.TrayManager;
import ui.utils.WorkflowService;

import javax.swing.*;
import java.awt.*;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainFrame extends JFrame {
    private static final int SIDEBAR_WIDTH = 230;
    private static final int HEADER_HEIGHT = 70;
    private static final int FRAME_SIZE_WIDTH = 1200;
    private static final int FRAME_SIZE_HEIGHT = 720;

    private static final File AVATAR_FILE = new File(System.getProperty("user.home") + "/Documents/MultiTool-Java-data/avatar.png");

    private JLabel avatarLabel;
    private final String login;
    private JPanel contentPanel;
    private JLabel levelLabel;
    private JLabel xpPopupLabel;
    private final JLabel loginLabel = new JLabel("");
    private TrayManager trayManager;

    public MainFrame(String login) {
        this.login = login;

        LevelManager.initializeDatabase();
        LevelManager.ensureUserEntry(login);

        setTitle("MultiTool - Welcome, " + DB.getNickname(login) + "!");
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

        add(createHeader(), BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);

        AchievementDB.initializeDatabase();
        AchievementDB.insertDefaultAchievements();
        AchievementDB.syncUserAchievements(login);
        AchievementDB.setMainFrame(this);
        AchievementDB.completeAchievement(login, "first_login");

        contentPanel.removeAll();
        WelcomePanel welcomePanel = new WelcomePanel(login);
        contentPanel.add(welcomePanel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();

        WorkflowService.startTracking();

        this.trayManager = new TrayManager(this);
        setVisible(true);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setPreferredSize(new Dimension(getWidth(), HEADER_HEIGHT));
        header.setBackground(UIStyle.HEADER_COLOR);

        JPanel profileBox = new JPanel();
        profileBox.setPreferredSize(new Dimension(SIDEBAR_WIDTH, HEADER_HEIGHT));
        profileBox.setBackground(UIStyle.SIDE_BOX);
        profileBox.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));

        avatarLabel = new JLabel();
        avatarLabel.setPreferredSize(new Dimension(55, 55));
        avatarLabel.setOpaque(true);
        avatarLabel.setBackground(Color.LIGHT_GRAY);

        if (AVATAR_FILE != null && AVATAR_FILE.exists()) {
            ImageIcon icon = new ImageIcon(AVATAR_FILE.getAbsolutePath());
            Image scaledImage = icon.getImage().getScaledInstance(55, 55, Image.SCALE_SMOOTH);
            ImageIcon scaledIcon = new ImageIcon(scaledImage);
            updateAvatarImage(scaledIcon);
        } else {
            System.out.println("Avatar file not found: " + AVATAR_FILE);
        }

        JPanel textBox = new JPanel();
        textBox.setLayout(new BoxLayout(textBox, BoxLayout.Y_AXIS));
        textBox.setBackground(UIStyle.SIDE_BOX);

        String roleColor = "#ffffff";
        if (AuthService.isAdmin()) {
            roleColor = "#c200ff";
            loginLabel.setText(
                    "<html><span style='color:" + roleColor + "; font-weight:bold;'>"
                            + "Admin"
                            + "</span>"
                            + " | "
                            + DB.getNickname(login)
                            + "</html>"
            );
        } else if (AuthService.isTester()) {
            roleColor = "#64c864";
            loginLabel.setText(
                    "<html><span style='color:" + roleColor + "; font-weight:bold;'>"
                            + "Tester"
                            + "</span>"
                            + " | "
                            + DB.getNickname(login)
                            + "</html>"
            );
        } else {
            loginLabel.setText(
                    "<html><span style='color:" + roleColor + "; font-weight:bold;'>"
                            + "</span>"
                            + DB.getNickname(login)
                            + "</html>"
            );
        }

        loginLabel.setForeground(Color.WHITE);
        loginLabel.setFont(loginLabel.getFont().deriveFont(14f));

        xpPopupLabel = new JLabel();
        xpPopupLabel.setForeground(new Color(0, 255, 255)); // бирюзовый
        xpPopupLabel.setVisible(false);
        xpPopupLabel.setFont(xpPopupLabel.getFont().deriveFont(11f));

        levelLabel = new JLabel(buildLevelText(login));
        levelLabel.setFont(levelLabel.getFont().deriveFont(11f));
        levelLabel.setForeground(Color.LIGHT_GRAY);

        JLabel achievementsLabel = new JLabel(buildAchievementsText(login));
        achievementsLabel.setFont(achievementsLabel.getFont().deriveFont(11f));
        achievementsLabel.setForeground(Color.LIGHT_GRAY);

        textBox.add(loginLabel);
        textBox.add(xpPopupLabel);
        textBox.add(levelLabel);
        textBox.add(achievementsLabel);

        profileBox.add(avatarLabel);
        profileBox.add(textBox);

        JButton achievementsBtn = new JButton();
        try {
            ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/icons/menu/achievements_icon.png")));
            Image scaled = icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
            achievementsBtn.setIcon(new ImageIcon(scaled));
        } catch (Exception e) {
            achievementsBtn.setText("⚙");
        }
        achievementsBtn.setPreferredSize(new Dimension(40, 40));
        achievementsBtn.setFocusPainted(false);
        achievementsBtn.setBorderPainted(false);
        achievementsBtn.setBackground(UIStyle.HEADER_COLOR);
        achievementsBtn.addActionListener(_ -> {
            contentPanel.removeAll();
            contentPanel.add(new AchievementsPanel(login), BorderLayout.CENTER);
            contentPanel.revalidate();
            contentPanel.repaint();
        });

        JButton settingsBtn = new JButton();
        try {
            ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/icons/menu/settings_icon.png")));
            Image scaled = icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
            settingsBtn.setIcon(new ImageIcon(scaled));
        } catch (Exception e) {
            settingsBtn.setText("⚙");
        }

        settingsBtn.setPreferredSize(new Dimension(40, 40));
        settingsBtn.setFocusPainted(false);
        settingsBtn.setBorderPainted(false);
        settingsBtn.setBackground(UIStyle.HEADER_COLOR);
        settingsBtn.addActionListener(_ -> openSettings());

        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 15));
        rightBox.setBackground(UIStyle.HEADER_COLOR);
        rightBox.add(achievementsBtn);
        rightBox.add(settingsBtn);

        header.add(profileBox, BorderLayout.WEST);
        header.add(rightBox, BorderLayout.EAST);

        return header;
    }

    public int getPercentOfAchievements(String login) {
        return (100/AchievementDB.getTotalAchievementsLevels()*AchievementDB.getTotalUserAchievementsLevels(login));
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
        sidebar.add(createExpandableSection("Photo & Video", new String[]{"Media Downloader", "File Organizer", "Image Tools"}));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(createExpandableSection("Math", new String[]{"Calculator", "Unit Converter"}));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(createExpandableSection("Text", new String[]{"Find & Replace"}));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(createExpandableSection("Time", new String[]{"Workflow", "Timer", "BDays notifier"}));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        if (AuthService.isTester()) {
            sidebar.add(createExpandableSection("Admin Panel", new String[]{"Admin CMD"}));
            sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        }
        return sidebar;
    }

    private JPanel createExpandableSection(String title, String[] items) {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(UIStyle.SIDE_BOX);

        JButton mainButton = new JButton(title + " | ▼");
        UIStyle.styleSidebarMainButton(mainButton, SIDEBAR_WIDTH);

        mainButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainButton.setMaximumSize(new Dimension(SIDEBAR_WIDTH - 20, 40));
        mainButton.setPreferredSize(new Dimension(SIDEBAR_WIDTH - 20, 40));
        mainButton.setMinimumSize(new Dimension(SIDEBAR_WIDTH - 20, 40));

        JPanel subPanel = new JPanel();
        subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.Y_AXIS));
        subPanel.setBackground(UIStyle.SIDE_BOX);
        subPanel.setMaximumSize(new Dimension(SIDEBAR_WIDTH, 0));
        subPanel.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));
        subPanel.setVisible(false);

        for (String item : items) {
            JButton subBtn = new JButton(item);
            UIStyle.styleSidebarSubButton(subBtn, SIDEBAR_WIDTH);
            subBtn.addActionListener(_ -> openTab(item));
            subPanel.add(Box.createVerticalStrut(2));
            subPanel.add(subBtn);
        }

        AtomicBoolean expanded = new AtomicBoolean(false);

        mainButton.addActionListener(_ -> {
            boolean isExpanding = !expanded.get();
            expanded.set(isExpanding);
            mainButton.setText(title + (isExpanding ? " | ▲" : " | ▼"));

            subPanel.setVisible(true);
            int targetHeight = isExpanding ? (items.length * 35) : 0;

            Timer timer = new Timer(10, new ActionListener() {
                int height = subPanel.getHeight();

                @Override
                public void actionPerformed(ActionEvent evt) {
                    if (isExpanding && height < targetHeight) {
                        height += 10;
                        if (height > targetHeight) height = targetHeight;
                    } else if (!isExpanding && height > 0) {
                        height -= 10;
                        if (height < 0) height = 0;
                    }

                    subPanel.setPreferredSize(new Dimension(SIDEBAR_WIDTH, height));
                    subPanel.setMaximumSize(new Dimension(SIDEBAR_WIDTH, height));
                    subPanel.revalidate();

                    if ((!isExpanding && height == 0) || (isExpanding && height == targetHeight)) {
                        if (!isExpanding) subPanel.setVisible(false);
                        ((Timer) evt.getSource()).stop();
                    }
                }
            });

            timer.start();
        });

        container.add(mainButton);
        container.add(Box.createVerticalStrut(5));
        container.add(subPanel);

        return container;
    }

    private LoadingPanel createLoadingPanel() {
        return new LoadingPanel();
    }

    private String buildLevelText(String user) {
        int lvl = LevelManager.getLevel(user);
        int xp  = LevelManager.getXP(user);
        return String.format("Level: %d (%d XP)", lvl, xp);
    }

    private String buildAchievementsText(String user) {
        int total_achievements = AchievementDB.getTotalAchievementsLevels();
        int tota_user_achievements  = AchievementDB.getTotalUserAchievementsLevels(user);
        return String.format(
                "Achievements: %d/%d (%d%%)",
                tota_user_achievements,
                total_achievements,
                getPercentOfAchievements(user)
        );
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
                return new SettingsPanel(MainFrame.this, login);
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

    /**
     * Переключает центральную панель на выбранную вкладку.
     * @param itemName Название вкладки (должно совпадать с названиями в сайдбаре!!!)
     */
    public void openTab(String itemName) {
        contentPanel.removeAll();

        switch (itemName) {
            case "Media Downloader" ->
                    contentPanel.add(new MediaDownloaderPanel(), BorderLayout.CENTER);
            case "BDays notifier" ->
                    contentPanel.add(new BDaysNotifierPanel(), BorderLayout.CENTER);
            case "Workflow" ->
                    contentPanel.add(new WorkflowPanel(), BorderLayout.CENTER);
            case "Admin CMD" ->
                    contentPanel.add(new AdminLogPanel(), BorderLayout.CENTER);
            case "Settings" ->
                    openSettings();
            case "Image Tools" ->
                    contentPanel.add(new ImageToolsPanel(), BorderLayout.CENTER);
            default ->
                    AppLogger.error("Attempted to open unknown tab: " + itemName);
        }

        contentPanel.revalidate();
        contentPanel.repaint();

        AppLogger.info("Tab switched to: " + itemName);
    }

    /**
     * Проверка значения close_to_tray в бд на выход с программы или минимизацию в трей.
     */
    private void handleClose() {
        boolean trayEnabled = db.DB.isCloseToTrayEnabled(login);
        AppLogger.info("Close clicked. Setting 'CloseToTray' is: " + trayEnabled);

        if (trayEnabled) {
            setVisible(false);
            AppLogger.info("Application minimized to system tray.");
        } else {
            AppLogger.info("Exiting application...");
            System.exit(0);
        }
    }

    /**
     * Начислить и показать анимацию опыта.
     */
    public void grantXP(String user, int amount) {
        LevelManager.addXP(user, amount);
        levelLabel.setText(buildLevelText(login));
        xpPopupLabel.setText("+" + amount + " XP");
        xpPopupLabel.setVisible(true);

        new java.util.Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> xpPopupLabel.setVisible(false));
            }
        }, 3000);
    }

    /**
     * Просто показать анимацию опыта.
     */
    public void demoGrantXP(int amount) {
        levelLabel.setText(buildLevelText(login));
        xpPopupLabel.setText("+" + amount + " XP");
        xpPopupLabel.setVisible(true);

        new java.util.Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> xpPopupLabel.setVisible(false));
            }
        }, 3000);
    }

    /**
     * Обновить аватарку в хедере.
     */
    public void updateAvatarImage(ImageIcon newIcon) {
        avatarLabel.setIcon(newIcon);
    }


    /**
     * Обновить ник-нейм в хедере.
     */
    public void updateNickName(String nickname) {
        String roleColor = "#ffffff";
        if (AuthService.isAdmin()) {
            roleColor = "#c200ff";
            loginLabel.setText(
                    "<html><span style='color:" + roleColor + "; font-weight:bold;'>"
                            + "Admin"
                            + "</span>"
                            + " | "
                            + DB.getNickname(login)
                            + "</html>"
            );
        } else if (AuthService.isTester()) {
            roleColor = "#64c864";
            loginLabel.setText(
                    "<html><span style='color:" + roleColor + "; font-weight:bold;'>"
                            + "Tester"
                            + "</span>"
                            + " | "
                            + DB.getNickname(login)
                            + "</html>"
            );
        } else {
            loginLabel.setText(
                    "<html><span style='color:" + roleColor + "; font-weight:bold;'>"
                            + "</span>"
                            + DB.getNickname(login)
                            + "</html>"
            );
        }
    }

    private void setAppIcon() {
        try {
            java.net.URL iconUrl = getClass().getResource("/project_icon.png");
            if (iconUrl == null) return;
            Image sourceImage = new ImageIcon(iconUrl).getImage();
            java.util.List<Image> images = new java.util.ArrayList<>();

            int[] sizes = {16, 32, 64, 128};
            for (int size : sizes) {
                images.add(sourceImage.getScaledInstance(size, size, Image.SCALE_SMOOTH));
            }
            this.setIconImages(images);
        } catch (Exception e) {
            ui.utils.AppLogger.error("Failed to set app icons: " + e.getMessage());
        }
    }
}
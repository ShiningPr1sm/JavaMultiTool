package ui;

import db.DatabaseProvider;
import service.AchievementService;
import service.AuthService;
import service.LevelService;
import util.AppLogger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Objects;
import java.util.TimerTask;

public class HeaderPanel extends JPanel {
    private static final int HEADER_HEIGHT = 70;
    private static final int SIDEBAR_WIDTH = 230;
    private static final String AVATAR_DIR = util.AppPaths.AVATAR_DIR;

    private final JLabel avatarLabel = new JLabel();
    private final JLabel loginLabel = new JLabel("");
    private final JLabel levelLabel = new JLabel();
    private final JLabel xpPopupLabel = new JLabel();
    private final String login;
    private final LevelService levelService;
    private final AchievementService achievementService;
    private final AuthService authService;
    private NotificationPanel notificationPanel;

    public HeaderPanel(String login, LevelService levelService, AchievementService achievementService, AuthService authService, Runnable onOpenAchievements, Runnable onOpenNotifications, Runnable onOpenSettings) {
        this.login = login;
        this.levelService = levelService;
        this.achievementService = achievementService;
        this.authService = authService;
        setLayout(new BorderLayout());
        setBackground(UIStyle.HEADER_COLOR);

        JPanel profileBox = new JPanel();
        profileBox.setPreferredSize(new Dimension(SIDEBAR_WIDTH, HEADER_HEIGHT));
        profileBox.setBackground(UIStyle.SIDE_BOX);
        profileBox.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));

        avatarLabel.setPreferredSize(new Dimension(55, 55));
        avatarLabel.setOpaque(true);
        avatarLabel.setBackground(Color.LIGHT_GRAY);

        File avatarFile = new File(AVATAR_DIR, login + ".png");
        if (avatarFile.exists()) {
            ImageIcon icon = new ImageIcon(avatarFile.getAbsolutePath());
            Image scaledImage = icon.getImage().getScaledInstance(55, 55, Image.SCALE_SMOOTH);
            avatarLabel.setIcon(new ImageIcon(scaledImage));
        }

        JPanel textBox = new JPanel();
        textBox.setLayout(new BoxLayout(textBox, BoxLayout.Y_AXIS));
        textBox.setBackground(UIStyle.SIDE_BOX);

        updateLoginLabel(DatabaseProvider.getUserRepository().getNickname(login));

        loginLabel.setForeground(Color.WHITE);
        loginLabel.setFont(loginLabel.getFont().deriveFont(14f));

        xpPopupLabel.setForeground(new Color(0, 255, 255));
        xpPopupLabel.setVisible(false);
        xpPopupLabel.setFont(xpPopupLabel.getFont().deriveFont(11f));

        levelLabel.setText(buildLevelText());
        levelLabel.setFont(levelLabel.getFont().deriveFont(11f));
        levelLabel.setForeground(Color.LIGHT_GRAY);

        JLabel achievementsLabel = new JLabel(buildAchievementsText());
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
            AppLogger.error("HeaderPanel: failed to load achievements icon: " + e.getMessage());
            achievementsBtn.setText("\u2699");
        }
        achievementsBtn.setPreferredSize(new Dimension(40, 40));
        achievementsBtn.setFocusPainted(false);
        achievementsBtn.setBorderPainted(false);
        achievementsBtn.setContentAreaFilled(false);
        achievementsBtn.addActionListener(e -> onOpenAchievements.run());

        JButton settingsBtn = new JButton();
        try {
            ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/icons/menu/settings_icon.png")));
            Image scaled = icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
            settingsBtn.setIcon(new ImageIcon(scaled));
        } catch (Exception e) {
            AppLogger.error("HeaderPanel: failed to load settings icon: " + e.getMessage());
            settingsBtn.setText("\u2699");
        }
        settingsBtn.setPreferredSize(new Dimension(40, 40));
        settingsBtn.setFocusPainted(false);
        settingsBtn.setBorderPainted(false);
        settingsBtn.setContentAreaFilled(false);
        settingsBtn.addActionListener(e -> onOpenSettings.run());

        notificationPanel = new NotificationPanel(onOpenNotifications);

        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        rightBox.setBackground(UIStyle.HEADER_COLOR);
        rightBox.add(createPlate(achievementsBtn));
        rightBox.add(createPlate(notificationPanel));
        rightBox.add(createPlate(settingsBtn));

        add(profileBox, BorderLayout.WEST);
        add(rightBox, BorderLayout.EAST);
    }

    private JPanel createPlate(JComponent comp) {
        JPanel plate = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIStyle.BUTTON_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
            }
        };
        plate.setLayout(new GridBagLayout());
        plate.setPreferredSize(new Dimension(44, 44));
        plate.setOpaque(false);
        plate.add(comp);
        return plate;
    }

    public void updateAvatarImage(ImageIcon newIcon) {
        avatarLabel.setIcon(newIcon);
    }

    public void updateNickName(String nickname) {
        updateLoginLabel(nickname);
    }

    public void setNotificationBadge(int count) {
        notificationPanel.setBadgeCount(count);
    }

    public void showXpGain(int amount) {
        levelLabel.setText(buildLevelText());
        xpPopupLabel.setText("+" + amount + " XP");
        xpPopupLabel.setVisible(true);

        new java.util.Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> xpPopupLabel.setVisible(false));
            }
        }, 3000);
    }

    private String buildLevelText() {
        int lvl = levelService.getLevel(login);
        int xp = levelService.getXP(login);
        return String.format("Level: %d (%d XP)", lvl, xp);
    }

    private String buildAchievementsText() {
        int total = achievementService.getTotalLevels();
        int user = achievementService.getUserLevels(login);
        int pct = total == 0 ? 0 : (user * 100) / total;
        return String.format("Achievements: %d/%d (%d%%)", user, total, pct);
    }

    private void updateLoginLabel(String nickname) {
        String roleColor = "#ffffff";
        String prefix = "";
        if (authService.isAdmin()) {
            roleColor = "#c200ff";
            prefix = "Admin";
        } else if (authService.isTester()) {
            roleColor = "#64c864";
            prefix = "Tester";
        }

        if (prefix.isEmpty()) {
            loginLabel.setText(
                    "<html><span style='color:" + roleColor + "; font-weight:bold;'>" + nickname + "</span></html>"
            );
        } else {
            loginLabel.setText(
                    "<html><span style='color:" + roleColor + "; font-weight:bold;'>" + prefix + "</span> | " + nickname + "</html>"
            );
        }
    }
}

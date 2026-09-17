package ui.settings;

import service.AchievementService;
import service.Services;
import service.SystemInfoService;
import db.DatabaseProvider;
import ui.AuthFrame;
import ui.MainFrame;
import ui.StyledDialog;
import ui.UIStyle;
import util.AppLogger;
import util.JavaFxFileChooser;

import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

public class SettingsPanel extends JPanel {

    private final db.UserRepository userRepo = DatabaseProvider.getUserRepository();
    private final String login;
    private final AchievementService achievementService;
    private final Services services;

    private static final String LOADING_LABEL = "Loading...";
    private static final int ROW_INSET = 4;
    private final JLabel appUptimeLabel = new JLabel("App uptime: " + LOADING_LABEL);
    private final JLabel sysUptimeLabel = new JLabel("System uptime: " + LOADING_LABEL);
    private final Timer uptimeTimer;

    public SettingsPanel(MainFrame mainFrame, String login, AchievementService achievementService, SystemInfoService systemInfoService, Services services) {
        this.login = login;
        this.achievementService = achievementService;
        this.services = services;
        systemInfoService.prepare();

        setLayout(new BorderLayout(0, 10));
        setBackground(UIStyle.BG_COLOR);

        UIStyle.makeFocusable(this);

        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JPanel userInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 30, 0));
        userInfoPanel.setBackground(UIStyle.BG_COLOR);
        userInfoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel avatarLabel = new JLabel();
        avatarLabel.setPreferredSize(new Dimension(121, 121));
        avatarLabel.setMaximumSize(new Dimension(121, 121));
        avatarLabel.setOpaque(true);
        avatarLabel.setBackground(Color.GRAY);
        updateAvatarImage(avatarLabel);

        JButton changeAvatarBtn = new JButton("Change Avatar");
        changeAvatarBtn.addActionListener(e -> chooseAvatar(mainFrame, avatarLabel));
        UIStyle.styleButton(changeAvatarBtn);
        changeAvatarBtn.setPreferredSize(new Dimension(avatarLabel.getPreferredSize().width, changeAvatarBtn.getPreferredSize().height));
        changeAvatarBtn.setMaximumSize(new Dimension(avatarLabel.getPreferredSize().width, changeAvatarBtn.getPreferredSize().height));

        JButton deleteAvatarBtn = new JButton("Delete Avatar");
        deleteAvatarBtn.addActionListener(e -> deleteAvatar(mainFrame, avatarLabel));
        UIStyle.styleButton(deleteAvatarBtn);
        deleteAvatarBtn.setPreferredSize(new Dimension(avatarLabel.getPreferredSize().width, deleteAvatarBtn.getPreferredSize().height));
        deleteAvatarBtn.setMaximumSize(new Dimension(avatarLabel.getPreferredSize().width, deleteAvatarBtn.getPreferredSize().height));

        JPanel avatarBox = new JPanel();
        avatarBox.setLayout(new BoxLayout(avatarBox, BoxLayout.Y_AXIS));
        avatarBox.setBackground(UIStyle.BG_COLOR);
        avatarBox.add(avatarLabel);
        avatarBox.add(Box.createVerticalStrut(10));
        avatarBox.add(changeAvatarBtn);
        avatarBox.add(Box.createVerticalStrut(5));
        avatarBox.add(deleteAvatarBtn);

        JLabel nicknameLabel = new JLabel("Nickname:");
        nicknameLabel.setForeground(Color.WHITE);

        JTextField nicknameField = new JTextField(userRepo.getNickname(login));
        UIStyle.styleTextField(nicknameField);

        JButton saveNicknameBtn = new JButton("Save Changes");
        saveNicknameBtn.addActionListener(e -> updateNickname(mainFrame, login, nicknameField.getText()));
        UIStyle.styleButton(saveNicknameBtn);

        JButton changePasswordBtn = new JButton("Change Password");
        changePasswordBtn.addActionListener(e -> openChangePasswordDialog(login));
        UIStyle.styleButton(changePasswordBtn);

        String[] themes = {"Original Dark", "Midnight Blue", "Deep Forest", "Crimson Ember", "Dracula", "Calm Tech", "Night Energy", "Blush Pink"};
        JComboBox<String> themeBox = new JComboBox<>(themes);
        UIStyle.styleComboBox(themeBox);
        themeBox.setSelectedItem(userRepo.getTheme(login));
        themeBox.addActionListener(e -> {
            String selected = (String) themeBox.getSelectedItem();
            userRepo.setTheme(login, selected);
            assert selected != null;
            AppLogger.admin("Settings: theme changed to " + selected + " (restart)");
            UIStyle.applyTheme(selected);
            StyledDialog.show(SwingUtilities.getWindowAncestor(this), "Theme applied! Program would restart to apply changes!");
            System.exit(0);
        });

        nicknameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        nicknameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nicknameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveNicknameBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        changePasswordBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        themeBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel nicknameBox = new JPanel();
        nicknameBox.setLayout(new BoxLayout(nicknameBox, BoxLayout.Y_AXIS));
        nicknameBox.setBackground(UIStyle.BG_COLOR);
        nicknameBox.add(Box.createVerticalStrut(14));
        nicknameBox.add(nicknameLabel);
        nicknameBox.add(Box.createVerticalStrut(6));
        nicknameBox.add(nicknameField);
        nicknameBox.add(Box.createVerticalStrut(6));

        JPanel btnRow = new JPanel(new GridLayout(2, 1, 0, 15));
        btnRow.setBackground(UIStyle.BG_COLOR);
        int commonWidth = Math.max(nicknameField.getPreferredSize().width,
                Math.max(saveNicknameBtn.getPreferredSize().width, changePasswordBtn.getPreferredSize().width));

        nicknameField.setPreferredSize(new Dimension(commonWidth, nicknameField.getPreferredSize().height));
        nicknameField.setMaximumSize(new Dimension(commonWidth, nicknameField.getPreferredSize().height));

        btnRow.setMaximumSize(new Dimension(commonWidth, 80));
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        saveNicknameBtn.setPreferredSize(new Dimension(commonWidth, saveNicknameBtn.getPreferredSize().height));
        changePasswordBtn.setPreferredSize(new Dimension(commonWidth, changePasswordBtn.getPreferredSize().height));
        btnRow.add(saveNicknameBtn);
        btnRow.add(changePasswordBtn);

        nicknameBox.add(btnRow);
        nicknameBox.add(Box.createVerticalStrut(15));
        themeBox.setPreferredSize(new Dimension(commonWidth, themeBox.getPreferredSize().height));
        themeBox.setMaximumSize(new Dimension(commonWidth, themeBox.getPreferredSize().height));
        nicknameBox.add(themeBox);

        userInfoPanel.add(avatarBox);
        userInfoPanel.add(nicknameBox);

        JLabel publicIpLabel = toggleLabel("Public IP: ***.***.***.***", "Public IP: ",
                systemInfoService::getCachedPublicIP);

        JLabel localIpLabel = dataLabel("Local IP: " + valueOrLoading(systemInfoService.getCachedLocalIP()));

        JLabel macLabel = dataLabel("MAC Address: " + valueOrLoading(systemInfoService.getCachedMac()));

        JLabel gatewayLabel = toggleLabel("Gateway IP: ***.***.***.***", "Gateway IP: ",
                systemInfoService::getCachedGatewayIp);

        JLabel dnsLabel = toggleLabel("DNS Servers: ***.***.***.***", "DNS Servers: ",
                systemInfoService::getCachedDnsServers);

        JLabel archLabel = dataLabel("Architecture: " + systemInfoService.getArchitecture());

        JLabel regDateLabel = dataLabel("Registered: " + reformatDate(userRepo.getRegistrationDate(login)));

        JLabel lastLoginLabel = dataLabel("Last Login: " + reformatDate(userRepo.getLastLoginDate(login)));

        styleDataLabel(appUptimeLabel);

        styleDataLabel(sysUptimeLabel);

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoutBtn.addActionListener(e -> logout());
        UIStyle.styleButton(logoutBtn);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(UIStyle.BG_COLOR);
        infoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(publicIpLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(localIpLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(macLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(gatewayLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(dnsLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(archLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(regDateLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(lastLoginLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(appUptimeLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(sysUptimeLabel);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(logoutBtn);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UIStyle.BG_COLOR);
        content.add(userInfoPanel);
        content.add(infoPanel);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(UIStyle.BG_COLOR);
        UIStyle.styleScrollBar(scrollPane);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottomBar.setBackground(UIStyle.BG_COLOR);
        JButton advancedBtn = new JButton("Advanced Settings");
        UIStyle.styleButton(advancedBtn);
        advancedBtn.addActionListener(e ->
                new AdvancedSettingsDialog((Frame) SwingUtilities.getWindowAncestor(this), login).setVisible(true));
        bottomBar.add(advancedBtn);

        add(scrollPane, BorderLayout.CENTER);
        add(bottomBar, BorderLayout.SOUTH);

        uptimeTimer = new Timer(1000, e -> {
            appUptimeLabel.setText("App uptime: " + systemInfoService.getAppUptime());
            sysUptimeLabel.setText("System uptime: " + systemInfoService.getSystemUptime());
        });
        uptimeTimer.start();
    }

    private static String valueOrLoading(String value) {
        return value != null ? value : LOADING_LABEL;
    }

    private static JLabel dataLabel(String text) {
        JLabel label = new JLabel(text);
        styleDataLabel(label);
        return label;
    }

    private static void styleDataLabel(JLabel label) {
        label.setForeground(Color.LIGHT_GRAY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, ROW_INSET, 0, 0));
    }

    private JLabel toggleLabel(String maskedText, String prefix, Supplier<String> valueSupplier) {
        JLabel label = dataLabel(maskedText);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (label.getText().contains("***")) {
                    label.setText(prefix + valueOrLoading(valueSupplier.get()));
                } else {
                    label.setText(maskedText);
                }
            }
        });
        return label;
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (uptimeTimer != null) {
            uptimeTimer.stop();
        }
    }

    private void chooseAvatar(MainFrame mainFrame, JLabel avatarLabel) {
        File selected = JavaFxFileChooser.openFile("Choose an image",
                new FileChooser.ExtensionFilter(
                        "Images", "*.jpg", "*.jpeg", "*.png", "*.bmp", "*.gif"
                )
        );

        if (selected == null) return;

        try {
            BufferedImage img = ImageIO.read(selected);
            if (img != null) {
                BufferedImage cropped = AvatarCropperDialog.showCropDialog(this, img);
                if (cropped != null) {
                    BufferedImage resized = new BufferedImage(121, 121, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = resized.createGraphics();
                    g2d.drawImage(cropped.getScaledInstance(121, 121, Image.SCALE_SMOOTH), 0, 0, null);
                    g2d.dispose();
                    String avatarPath = util.AppPaths.avatarFile(login);
                    if (avatarPath == null) return;
                    File avatarFile = new File(avatarPath);
                    File avatarDir = avatarFile.getParentFile();
                    if (avatarDir != null && !avatarDir.exists()) avatarDir.mkdirs();
                    ImageIO.write(resized, "png", avatarFile);
                    updateAvatarImage(avatarLabel);

                    if (avatarFile.exists()) {
                        ImageIcon icon = new ImageIcon(avatarFile.getAbsolutePath());
                        Image scaledImageAvatar = icon.getImage().getScaledInstance(55, 55, Image.SCALE_SMOOTH);
                        ImageIcon scaledIcon = new ImageIcon(scaledImageAvatar);
                        mainFrame.updateAvatarImage(scaledIcon);
                    }
                    AppLogger.info("Settings: avatar updated for " + login);
                }
            }
        } catch (IOException ex) {
            AppLogger.error("Settings: failed to load image - " + ex.getMessage());
            StyledDialog.show(SwingUtilities.getWindowAncestor(this), "Failed to load image.");
        }
    }

    private void updateNickname(MainFrame mainFrame, String login, String nickname) {
        if (!nickname.isEmpty()) {
            try (Connection conn = userRepo.getConnection();
                PreparedStatement stmt = conn.prepareStatement("UPDATE users SET nickname = ? WHERE login = ?")) {
                stmt.setString(1, nickname);
                stmt.setString(2, login);
                stmt.executeUpdate();
                StyledDialog.show(SwingUtilities.getWindowAncestor(this), "Nickname updated successfully!", "Success");
                AppLogger.admin("Settings: nickname changed to '" + nickname + "' for " + login);
                achievementService.complete(login, "change_nickname");
                mainFrame.updateNickName(userRepo.getNickname(login));
            } catch (Exception ex) {
                AppLogger.error("Settings: failed to update nickname - " + ex.getMessage());
                StyledDialog.show(SwingUtilities.getWindowAncestor(this), "Failed to update nickname.");
            }
        }
    }

    private void openChangePasswordDialog(String login) {
        JPasswordField currentPassword = new JPasswordField();
        JPasswordField newPassword = new JPasswordField();
        JPasswordField confirmPassword = new JPasswordField();

        UIStyle.styleTextField(currentPassword);
        UIStyle.styleTextField(newPassword);
        UIStyle.styleTextField(confirmPassword);

        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 6));
        panel.setBackground(UIStyle.SIDE_BOX);
        JLabel curLabel = new JLabel("Current Password:");
        curLabel.setForeground(UIStyle.TEXT_COLOR);
        JLabel newLabel = new JLabel("New Password:");
        newLabel.setForeground(UIStyle.TEXT_COLOR);
        JLabel confLabel = new JLabel("Confirm New Password:");
        confLabel.setForeground(UIStyle.TEXT_COLOR);

        panel.add(curLabel);
        panel.add(currentPassword);
        panel.add(newLabel);
        panel.add(newPassword);
        panel.add(confLabel);
        panel.add(confirmPassword);

        boolean confirmed = StyledDialog.confirm(SwingUtilities.getWindowAncestor(this), panel, "Change Password");
        if (confirmed) {
            String current = new String(currentPassword.getPassword());
            String newPass = new String(newPassword.getPassword());
            String confirmPass = new String(confirmPassword.getPassword());

            if (!newPass.equals(confirmPass)) {
                AppLogger.info("Settings: password change rejected - passwords do not match");
                StyledDialog.show(SwingUtilities.getWindowAncestor(this), "New passwords do not match.");
                return;
            }

            if (!userRepo.checkPassword(login, current)) {
                AppLogger.info("Settings: password change rejected - incorrect current password");
                StyledDialog.show(SwingUtilities.getWindowAncestor(this), "Current password is incorrect.");
                return;
            }

            userRepo.updatePassword(login, newPass);
            StyledDialog.show(SwingUtilities.getWindowAncestor(this), "Password changed successfully!", "Success");
            AppLogger.admin("Settings: password changed for " + login);
        }
    }

    private void logout() {
        AppLogger.admin("Settings: logout clicked for " + login);
        SwingUtilities.invokeLater(() -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            frame.dispose();
            new AuthFrame(services);
        });
    }

    private static String reformatDate(String dateStr) {
        if (dateStr == null || dateStr.equals("Unknown")) return dateStr;
        try {
            LocalDateTime dt = LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return dt.format(DateTimeFormatter.ofPattern("HH:mm:ss / dd.MM.yyyy"));
        } catch (Exception e) {
            AppLogger.info("Settings: could not reformat date '" + dateStr + "' - " + e.getMessage());
            return dateStr;
        }
    }

    private void updateAvatarImage(JLabel label) {
        String avatarPath = util.AppPaths.avatarFile(login);
        if (avatarPath != null) {
            File avatarFile = new File(avatarPath);
            if (avatarFile.exists()) {
                try {
                    BufferedImage img = ImageIO.read(avatarFile);
                    ImageIcon icon = new ImageIcon(img.getScaledInstance(121, 121, Image.SCALE_SMOOTH));
                    label.setIcon(icon);
                    label.setText("");
                } catch (IOException e) {
                    AppLogger.error("SettingsPanel: failed to update avatar image: " + e.getMessage());
                }
                return;
            }
        }
        java.net.URL defaultUrl = getClass().getResource("/icons/settings/default_avatar.png");
        if (defaultUrl != null) {
            ImageIcon defaultIcon = new ImageIcon(defaultUrl);
            Image scaled = defaultIcon.getImage().getScaledInstance(121, 121, Image.SCALE_SMOOTH);
            label.setIcon(new ImageIcon(scaled));
            label.setText("");
        }
    }

    private void deleteAvatar(MainFrame mainFrame, JLabel avatarLabel) {
        String avatarPath = util.AppPaths.avatarFile(login);
        if (avatarPath != null) {
            File avatarFile = new File(avatarPath);
            if (avatarFile.exists() && avatarFile.delete()) {
                AppLogger.info("SettingsPanel: avatar deleted for " + login);
            }
        }
        updateAvatarImage(avatarLabel);
        java.net.URL defaultUrl = getClass().getResource("/icons/settings/default_avatar.png");
        if (defaultUrl != null) {
            ImageIcon defaultIcon = new ImageIcon(defaultUrl);
            Image scaled = defaultIcon.getImage().getScaledInstance(55, 55, Image.SCALE_SMOOTH);
            mainFrame.updateAvatarImage(new ImageIcon(scaled));
        }
    }
}
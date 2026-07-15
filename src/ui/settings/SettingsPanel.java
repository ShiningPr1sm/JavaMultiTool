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

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SettingsPanel extends JPanel {

    private static final String AVATAR_DIR = util.AppPaths.AVATAR_DIR;
    private final db.UserRepository userRepo = DatabaseProvider.getUserRepository();
    private final String login;
    private final AchievementService achievementService;
    private final SystemInfoService systemInfoService;
    private final Services services;
    private final JLabel appUptimeLabel = new JLabel(" App uptime: Loading...");
    private final JLabel sysUptimeLabel = new JLabel(" System uptime: Loading...");
    private final Timer uptimeTimer;

    public SettingsPanel(MainFrame mainFrame, String login, AchievementService achievementService, SystemInfoService systemInfoService, Services services) {
        this.login = login;
        this.achievementService = achievementService;
        this.systemInfoService = systemInfoService;
        this.services = services;
        systemInfoService.prepare();

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
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

        JPanel avatarBox = new JPanel();
        avatarBox.setLayout(new BoxLayout(avatarBox, BoxLayout.Y_AXIS));
        avatarBox.setBackground(UIStyle.BG_COLOR);
        avatarBox.add(avatarLabel);
        avatarBox.add(Box.createVerticalStrut(10));
        avatarBox.add(changeAvatarBtn);

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
        nicknameBox.add(nicknameLabel);
        nicknameBox.add(Box.createVerticalStrut(6));
        nicknameBox.add(nicknameField);
        nicknameBox.add(Box.createVerticalStrut(6));
        JPanel btnRow = new JPanel(new GridLayout(2, 1, 0, 6));
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
        nicknameBox.add(Box.createVerticalStrut(7));
        themeBox.setPreferredSize(new Dimension(commonWidth, themeBox.getPreferredSize().height));
        themeBox.setMaximumSize(new Dimension(commonWidth, themeBox.getPreferredSize().height));
        nicknameBox.add(themeBox);

        userInfoPanel.add(avatarBox);
        userInfoPanel.add(nicknameBox);

        JButton publicIpBtn = new JButton("Public IP: ***.***.***.***");
        publicIpBtn.setForeground(Color.WHITE);
        publicIpBtn.setFont(UIManager.getFont("Label.font"));
        publicIpBtn.setContentAreaFilled(false);
        publicIpBtn.setBorderPainted(false);
        publicIpBtn.setFocusPainted(false);
        publicIpBtn.setOpaque(false);
        publicIpBtn.setMargin(new Insets(0, 0, 0, 0));
        publicIpBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        publicIpBtn.addActionListener(e -> {
            if (publicIpBtn.getText().contains("***")) {
                String ip = systemInfoService.getCachedPublicIP();
                publicIpBtn.setText("Public IP: " + (ip != null ? ip : "Loading..."));
            } else {
                publicIpBtn.setText("Public IP: ***.***.***.***");
            }
        });

        JLabel localIpLabel = new JLabel(" Local IP: " + (systemInfoService.getCachedLocalIP() != null ? systemInfoService.getCachedLocalIP() : "Loading..."));
        localIpLabel.setForeground(Color.WHITE);
        localIpLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel macLabel = new JLabel(" MAC Address: " + (systemInfoService.getCachedMac() != null ? systemInfoService.getCachedMac() : "Loading..."));
        macLabel.setForeground(Color.LIGHT_GRAY);
        macLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton gatewayBtn = new JButton("Gateway IP: ***.***.***.***");
        gatewayBtn.setForeground(Color.WHITE);
        gatewayBtn.setFont(UIManager.getFont("Label.font"));
        gatewayBtn.setContentAreaFilled(false);
        gatewayBtn.setBorderPainted(false);
        gatewayBtn.setFocusPainted(false);
        gatewayBtn.setOpaque(false);
        gatewayBtn.setMargin(new Insets(0, 0, 0, 0));
        gatewayBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        gatewayBtn.addActionListener(e -> {
            if (gatewayBtn.getText().contains("***")) {
                String gw = systemInfoService.getCachedGatewayIp();
                gatewayBtn.setText("Gateway IP: " + (gw != null ? gw : "Loading..."));
            } else {
                gatewayBtn.setText("Gateway IP: ***.***.***.***");
            }
        });

        JButton dnsBtn = new JButton("DNS Servers: ***.***.***.***");
        dnsBtn.setForeground(Color.WHITE);
        dnsBtn.setFont(UIManager.getFont("Label.font"));
        dnsBtn.setContentAreaFilled(false);
        dnsBtn.setBorderPainted(false);
        dnsBtn.setFocusPainted(false);
        dnsBtn.setOpaque(false);
        dnsBtn.setMargin(new Insets(0, 0, 0, 0));
        dnsBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        dnsBtn.addActionListener(e -> {
            if (dnsBtn.getText().contains("***")) {
                String dns = systemInfoService.getCachedDnsServers();
                dnsBtn.setText("DNS Servers: " + (dns != null ? dns : "Loading..."));
            } else {
                dnsBtn.setText("DNS Servers: ***.***.***.***");
            }
        });

        JLabel archLabel = new JLabel(" Architecture: " + systemInfoService.getArchitecture());
        archLabel.setForeground(Color.LIGHT_GRAY);
        archLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel regDateLabel = new JLabel(" Registered: " + reformatDate(userRepo.getRegistrationDate(login)));
        regDateLabel.setForeground(Color.LIGHT_GRAY);
        regDateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lastLoginLabel = new JLabel(" Last Login: " + reformatDate(userRepo.getLastLoginDate(login)));
        lastLoginLabel.setForeground(Color.LIGHT_GRAY);
        lastLoginLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        appUptimeLabel.setForeground(Color.LIGHT_GRAY);
        appUptimeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        sysUptimeLabel.setForeground(Color.LIGHT_GRAY);
        sysUptimeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JCheckBox saveLoginBox = new JCheckBox("Save data after first login");
        UIStyle.styleCheckbox(saveLoginBox);
        saveLoginBox.setSelected(userRepo.isSaveLoginEnabled(login));
        saveLoginBox.addActionListener(e -> userRepo.setSaveLogin(login, saveLoginBox.isSelected()));
        saveLoginBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JCheckBox trayBox = new JCheckBox("Minimize to tray on close");
        UIStyle.styleCheckbox(trayBox);
        trayBox.setSelected(userRepo.isCloseToTrayEnabled(login));
        trayBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        trayBox.addActionListener(e -> {
            userRepo.setCloseToTray(login, trayBox.isSelected());
            AppLogger.info("Settings: Close to tray set to " + trayBox.isSelected());
        });

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoutBtn.addActionListener(e -> logout());
        UIStyle.styleButton(logoutBtn);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(UIStyle.BG_COLOR);
        infoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(publicIpBtn);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(localIpLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(macLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(gatewayBtn);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(dnsBtn);
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
        infoPanel.add(saveLoginBox);
        infoPanel.add(Box.createVerticalStrut(2));
        infoPanel.add(trayBox);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(logoutBtn);
        add(userInfoPanel);
        add(infoPanel);

        uptimeTimer = new Timer(1000, e -> {
            appUptimeLabel.setText(" App uptime: " + systemInfoService.getAppUptime());
            sysUptimeLabel.setText(" System uptime: " + systemInfoService.getSystemUptime());
        });
        uptimeTimer.start();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (uptimeTimer != null) {
            uptimeTimer.stop();
        }
    }

    private void chooseAvatar(MainFrame mainFrame, JLabel avatarLabel) {
        FileDialog fd = new FileDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Choose an image", FileDialog.LOAD);
        fd.setFilenameFilter((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                    || lower.endsWith(".png") || lower.endsWith(".bmp")
                    || lower.endsWith(".gif");
        });
        fd.setVisible(true);

        if (fd.getFile() == null) return;
        File selected = new File(fd.getDirectory(), fd.getFile());

        try {
            BufferedImage img = ImageIO.read(selected);
            if (img != null) {
                BufferedImage cropped = AvatarCropperDialog.showCropDialog(this, img);
                if (cropped != null) {
                    BufferedImage resized = new BufferedImage(121, 121, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = resized.createGraphics();
                    g2d.drawImage(cropped.getScaledInstance(121, 121, Image.SCALE_SMOOTH), 0, 0, null);
                    g2d.dispose();
                    File avatarDir = new File(AVATAR_DIR);
                    if (!avatarDir.exists()) avatarDir.mkdirs();
                    File avatarFile = new File(avatarDir, login + ".png");
                    ImageIO.write(resized, "png", avatarFile);
                    updateAvatarImage(avatarLabel);

                    if (avatarFile.exists()) {
                        ImageIcon icon = new ImageIcon(avatarFile.getAbsolutePath());
                        Image scaledImageAvatar = icon.getImage().getScaledInstance(55, 55, Image.SCALE_SMOOTH);
                        ImageIcon scaledIcon = new ImageIcon(scaledImageAvatar);
                        mainFrame.updateAvatarImage(scaledIcon);
                    }
                }
            }
        } catch (IOException ex) {
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
                achievementService.complete(login, "change_nickname");
                mainFrame.updateNickName(userRepo.getNickname(login));
            } catch (Exception ex) {
                StyledDialog.show(SwingUtilities.getWindowAncestor(this), "Failed to update nickname.");
            }
        }
    }

    private void openChangePasswordDialog(String login) {
        JPasswordField currentPassword = new JPasswordField();
        JPasswordField newPassword = new JPasswordField();
        JPasswordField confirmPassword = new JPasswordField();

        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 6));
        panel.setBackground(UIStyle.SIDE_BOX);
        JLabel curLabel = new JLabel("Current Password:");
        curLabel.setForeground(Color.WHITE);
        JLabel newLabel = new JLabel("New Password:");
        newLabel.setForeground(Color.WHITE);
        JLabel confLabel = new JLabel("Confirm New Password:");
        confLabel.setForeground(Color.WHITE);
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
                StyledDialog.show(SwingUtilities.getWindowAncestor(this), "New passwords do not match.");
                return;
            }

            if (!userRepo.checkPassword(login, current)) {
                StyledDialog.show(SwingUtilities.getWindowAncestor(this), "Current password is incorrect.");
                return;
            }

            userRepo.updatePassword(login, newPass);
            StyledDialog.show(SwingUtilities.getWindowAncestor(this), "Password changed successfully!", "Success");
        }
    }

    private void logout() {
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
            return dateStr;
        }
    }

    private void updateAvatarImage(JLabel label) {
        File avatarFile = new File(AVATAR_DIR, login + ".png");
        if (avatarFile.exists()) {
            try {
                BufferedImage img = ImageIO.read(avatarFile);
                ImageIcon icon = new ImageIcon(img.getScaledInstance(121, 121, Image.SCALE_SMOOTH));
                label.setIcon(icon);
                label.setText("");
            } catch (IOException e) {
                AppLogger.error("SettingsPanel: failed to update avatar image: " + e.getMessage());
            }
        }
    }

}

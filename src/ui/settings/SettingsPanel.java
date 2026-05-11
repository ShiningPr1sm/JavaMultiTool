package ui.settings;

import db.AchievementDB;
import db.DB;
import ui.AuthFrame;
import ui.MainFrame;
import ui.UIStyle;
import ui.utils.AppLogger;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Scanner;

public class SettingsPanel extends JPanel {

    private static String cachedPublicIP = null;
    private static String cachedLocalIP = null;
    private static String cachedMac = null;

    private static boolean isInfoLoaded = false;

    private static final File AVATAR_FILE = new File(System.getProperty("user.home") + "/Documents/MultiTool-Java-data/avatar.png");

    public static void prepareSystemInfo() {
        if (isInfoLoaded) return;
        isInfoLoaded = true;

        new Thread(() -> {
            try {
                cachedPublicIP = fetchPublicIP();
                cachedLocalIP = fetchLocalIp();
                cachedMac = fetchMac();

                AppLogger.info("System info cached successfully.");
            } catch (Exception e) {
                AppLogger.error("Failed to cache system info: " + e.getMessage());
            }
        }).start();
    }

    public SettingsPanel(MainFrame mainFrame, String login) {
        prepareSystemInfo();

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(UIStyle.BG_COLOR);
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
        changeAvatarBtn.addActionListener(_ -> chooseAvatar(mainFrame, avatarLabel));
        UIStyle.styleButton(changeAvatarBtn);

        JPanel avatarBox = new JPanel();
        avatarBox.setLayout(new BoxLayout(avatarBox, BoxLayout.Y_AXIS));
        avatarBox.setBackground(UIStyle.BG_COLOR);
        avatarBox.add(avatarLabel);
        avatarBox.add(Box.createVerticalStrut(10));
        avatarBox.add(changeAvatarBtn);

        JLabel nicknameLabel = new JLabel("Nickname:");
        nicknameLabel.setForeground(Color.WHITE);

        JTextField nicknameField = new JTextField(DB.getNickname(login));
        nicknameField.setMaximumSize(new Dimension(300, 30));

        JButton saveNicknameBtn = new JButton("Save Changes");
        saveNicknameBtn.addActionListener(_ -> updateNickname(mainFrame, login, nicknameField.getText()));
        UIStyle.styleButton(saveNicknameBtn);

        JButton changePasswordBtn = new JButton("Change Password");
        changePasswordBtn.addActionListener(_ -> openChangePasswordDialog(login));
        UIStyle.styleButton(changePasswordBtn);

        String[] themes = {"Original Dark", "Midnight Blue", "Deep Forest", "Crimson Ember", "Dracula"};
        JComboBox<String> themeBox = new JComboBox<>(themes);
        UIStyle.styleComboBox(themeBox);
        themeBox.setSelectedItem(DB.getTheme(login));
        themeBox.addActionListener(_ -> {
            String selected = (String) themeBox.getSelectedItem();
            DB.setTheme(login, selected);
            assert selected != null;
            UIStyle.applyTheme(selected);
            JOptionPane.showMessageDialog(this, "Theme applied! Please switch tabs to see changes.");
        });

        nicknameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        nicknameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveNicknameBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        changePasswordBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        themeBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel nicknameBox = new JPanel();
        nicknameBox.setLayout(new BoxLayout(nicknameBox, BoxLayout.Y_AXIS));
        nicknameBox.setBackground(UIStyle.BG_COLOR);
        nicknameBox.add(nicknameLabel);
        nicknameBox.add(Box.createVerticalStrut(10));
        nicknameBox.add(nicknameField);
        nicknameBox.add(Box.createVerticalStrut(10));
        nicknameBox.add(saveNicknameBtn);
        nicknameBox.add(Box.createVerticalStrut(10));
        nicknameBox.add(changePasswordBtn);
        nicknameBox.add(Box.createVerticalStrut(15));
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
        publicIpBtn.addActionListener(_ -> {
            if (publicIpBtn.getText().contains("***")) {
                String ip = (cachedPublicIP != null) ? cachedPublicIP : "Loading...";
                publicIpBtn.setText("Public IP: " + ip);
            } else {
                publicIpBtn.setText("Public IP: ***.***.***.***");
            }
        });

        JLabel localIpLabel = new JLabel(" Local IP: " );
        localIpLabel.setForeground(Color.WHITE);
        localIpLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel macLabel = new JLabel(" MAC Address: " + (cachedMac != null ? cachedMac : "Loading..."));
        macLabel.setForeground(Color.LIGHT_GRAY);
        macLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel regDateLabel = new JLabel(" Registered: " + DB.getRegistrationDate(login));
        regDateLabel.setForeground(Color.LIGHT_GRAY);
        regDateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lastLoginLabel = new JLabel(" Last Login: " + DB.getLastLoginDate(login));
        lastLoginLabel.setForeground(Color.LIGHT_GRAY);
        lastLoginLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JCheckBox saveLoginBox = new JCheckBox("Save data after first login");
        saveLoginBox.setBackground(UIStyle.BG_COLOR);
        saveLoginBox.setForeground(Color.WHITE);
        saveLoginBox.setSelected(DB.isSaveLoginEnabled(login));
        saveLoginBox.addActionListener(_ -> DB.setSaveLogin(login, saveLoginBox.isSelected()));
        saveLoginBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JCheckBox trayBox = new JCheckBox("Minimize to tray on close");
        trayBox.setBackground(UIStyle.BG_COLOR);
        trayBox.setForeground(Color.WHITE);
        trayBox.setSelected(db.DB.isCloseToTrayEnabled(login));
        trayBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        trayBox.addActionListener(_ -> {
            db.DB.setCloseToTray(login, trayBox.isSelected());
            AppLogger.info("Settings: Close to tray set to " + trayBox.isSelected());
        });

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoutBtn.addActionListener(_ -> logout());
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
        infoPanel.add(regDateLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(lastLoginLabel);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(saveLoginBox);
        infoPanel.add(Box.createVerticalStrut(2));
        infoPanel.add(trayBox);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(logoutBtn);
        add(userInfoPanel);
        add(infoPanel);
    }

    private void chooseAvatar(MainFrame mainFrame, JLabel avatarLabel) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose an image");
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Image files", "jpg", "jpeg", "png", "bmp", "gif"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = fileChooser.getSelectedFile();
            try {
                BufferedImage img = ImageIO.read(selected);
                if (img != null) {
                    BufferedImage cropped = AvatarCropperDialog.showCropDialog(this, img);
                    if (cropped != null) {
                        BufferedImage resized = new BufferedImage(121, 121, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g2d = resized.createGraphics();
                        g2d.drawImage(cropped.getScaledInstance(121, 121, Image.SCALE_SMOOTH), 0, 0, null);
                        g2d.dispose();
                        ImageIO.write(resized, "png", AVATAR_FILE);
                        updateAvatarImage(avatarLabel);

                        if (AVATAR_FILE.exists()) {
                            ImageIcon icon = new ImageIcon(AVATAR_FILE.getAbsolutePath());
                            Image scaledImageAvatar = icon.getImage().getScaledInstance(55, 55, Image.SCALE_SMOOTH);
                            ImageIcon scaledIcon = new ImageIcon(scaledImageAvatar);
                            mainFrame.updateAvatarImage(scaledIcon);
                        }
                    }
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to load image.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateNickname(MainFrame mainFrame, String login, String nickname) {
        if (!nickname.isEmpty()) {
            try (Connection conn = DB.getConnection();
                PreparedStatement stmt = conn.prepareStatement("UPDATE users SET nickname = ? WHERE login = ?")) {
                stmt.setString(1, nickname);
                stmt.setString(2, login);
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Nickname updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                AchievementDB.completeAchievement(login, "change_nickname");
                mainFrame.updateNickName(DB.getNickname(login));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to update nickname.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openChangePasswordDialog(String login) {
        JPasswordField currentPassword = new JPasswordField();
        JPasswordField newPassword = new JPasswordField();
        JPasswordField confirmPassword = new JPasswordField();

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Current Password:"));
        panel.add(currentPassword);
        panel.add(new JLabel("New Password:"));
        panel.add(newPassword);
        panel.add(new JLabel("Confirm New Password:"));
        panel.add(confirmPassword);

        int result = JOptionPane.showConfirmDialog(this, panel, "Change Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String current = new String(currentPassword.getPassword());
            String newPass = new String(newPassword.getPassword());
            String confirmPass = new String(confirmPassword.getPassword());

            if (!newPass.equals(confirmPass)) {
                JOptionPane.showMessageDialog(this, "New passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!DB.checkPassword(login, current)) {
                JOptionPane.showMessageDialog(this, "Current password is incorrect.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            DB.updatePassword(login, newPass);
            JOptionPane.showMessageDialog(this, "Password changed successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void logout() {
        logoutUser();
        SwingUtilities.invokeLater(() -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            frame.dispose();
            new AuthFrame();
        });
    }

    private void updateAvatarImage(JLabel label) {
        if (AVATAR_FILE.exists()) {
            try {
                BufferedImage img = ImageIO.read(AVATAR_FILE);
                ImageIcon icon = new ImageIcon(img.getScaledInstance(121, 121, Image.SCALE_SMOOTH));
                label.setIcon(icon);
                label.setText("");
            } catch (IOException ignored) {}
        }
    }

    private static String fetchLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "Unavailable";
        }
    }

    public static String fetchPublicIP() {
        try (Scanner s = new Scanner(new URL("https://api.ipify.org").openStream(), StandardCharsets.UTF_8)) {
            return s.useDelimiter("\\A").next();
        } catch (Exception e) {
            return "Unavailable";
        }
    }

    public static void logoutUser() {
        System.out.println("User logged out (placeholder).");
    }

    private static String fetchMac() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface ni = NetworkInterface.getByInetAddress(ip);
            if (ni != null) {
                byte[] mac = ni.getHardwareAddress();
                if (mac != null) {
                    StringBuilder sb = new StringBuilder();
                    for (byte b : mac) {
                        sb.append(String.format("%02X:", b));
                    }
                    return sb.substring(0, sb.length() - 1);
                }
            }
        } catch (Exception e) {
            return "Unavailable";
        }
        return "Unavailable";
    }
}
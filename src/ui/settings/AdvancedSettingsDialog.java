package ui.settings;

import db.DatabaseProvider;
import db.UserRepository;
import ui.UIStyle;
import util.AppLogger;
import util.ConfigManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AdvancedSettingsDialog extends JDialog {

    private record SettingDef(String label, String key, boolean defaultValue) {}

    private static final List<SettingDef> SETTINGS = List.of(
            new SettingDef("Enable Auto-update", "autoUpdate", true),
            new SettingDef("Show Daily Quote", "dailyQuote", true),
            new SettingDef("Birthday Reminders", "birthdayReminders", true),
            new SettingDef("Use internet to download tools or get data", "useInternet", true),
            new SettingDef("Send notification messages", "showNotifications", true),
            new SettingDef("Save data after first login", "saveData", true),
            new SettingDef("Minimize to tray on close", "minToTray", true),
            new SettingDef("Launch at startup (beta)", "launchAtStartup", true)
    );

    public AdvancedSettingsDialog(Frame owner, String login) {
        super(owner, "Advanced Settings", true);
        UIStyle.setAppIcon(this);
        setBackground(UIStyle.BG_COLOR);
        UserRepository userRepo = DatabaseProvider.getUserRepository();

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(UIStyle.BG_COLOR);
        content.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 10, 4, 10);

        JLabel nameHeader = new JLabel("Preference Name", SwingConstants.LEFT);
        nameHeader.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameHeader.setForeground(UIStyle.ACCENT_COLOR);
        content.add(nameHeader, c);

        c.gridx = 1;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.NONE;
        JLabel valueHeader = new JLabel("Value");
        valueHeader.setFont(new Font("Segoe UI", Font.BOLD, 13));
        valueHeader.setForeground(UIStyle.ACCENT_COLOR);
        content.add(valueHeader, c);

        c.insets = new Insets(4, 10, 4, 10);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        for (SettingDef def : SETTINGS) {
            c.gridy++;
            c.gridx = 0;
            JLabel label = new JLabel(def.label());
            label.setForeground(UIStyle.TEXT_COLOR);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            content.add(label, c);

            c.gridx = 1;
            c.anchor = GridBagConstraints.CENTER;
            c.fill = GridBagConstraints.NONE;
            boolean value = "minToTray".equals(def.key())
                    ? userRepo.isCloseToTrayEnabled(login)
                    : Boolean.parseBoolean(ConfigManager.loadProperty(def.key(), String.valueOf(def.defaultValue())));
            JCheckBox cb = new JCheckBox();
            UIStyle.styleCheckbox(cb);
            cb.setSelected(value);
            cb.addActionListener(e -> {
                if ("minToTray".equals(def.key())) {
                    userRepo.setCloseToTray(login, cb.isSelected());
                } else {
                    ConfigManager.saveProperty(def.key(), String.valueOf(cb.isSelected()));
                }
                AppLogger.info("Settings: '" + def.label() + "' set to " + cb.isSelected());
            });
            content.add(cb, c);

            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
        }

//        c.gridy++;
//        c.gridx = 0;
//        c.gridwidth = 1;
//        c.anchor = GridBagConstraints.WEST;
//        c.fill = GridBagConstraints.HORIZONTAL;
//        JLabel saveLoginLabel = new JLabel("Save data after first login");
//        saveLoginLabel.setForeground(UIStyle.TEXT_COLOR);
//        saveLoginLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
//        content.add(saveLoginLabel, c);
//
//        c.gridx = 1;
//        c.anchor = GridBagConstraints.CENTER;
//        c.fill = GridBagConstraints.NONE;
//        JCheckBox saveLoginBox = new JCheckBox();
//        UIStyle.styleCheckbox(saveLoginBox);
//        saveLoginBox.setSelected(userRepo.isSaveLoginEnabled(login));
//        saveLoginBox.addActionListener(e -> {
//            userRepo.setSaveLogin(login, saveLoginBox.isSelected());
//            AppLogger.info("Settings: save login set to " + saveLoginBox.isSelected());
//        });
//        content.add(saveLoginBox, c);
//
//        c.gridy++;
//        c.gridx = 0;
//        c.anchor = GridBagConstraints.WEST;
//        c.fill = GridBagConstraints.HORIZONTAL;
//        JLabel trayLabel = new JLabel("Minimize to tray on close");
//        trayLabel.setForeground(UIStyle.TEXT_COLOR);
//        trayLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
//        content.add(trayLabel, c);
//
//        c.gridx = 1;
//        c.anchor = GridBagConstraints.CENTER;
//        c.fill = GridBagConstraints.NONE;
//        JCheckBox trayBox = new JCheckBox();
//        UIStyle.styleCheckbox(trayBox);
//        trayBox.setSelected(userRepo.isCloseToTrayEnabled(login));
//        trayBox.addActionListener(e -> {
//            userRepo.setCloseToTray(login, trayBox.isSelected());
//            AppLogger.info("Settings: Close to tray set to " + trayBox.isSelected());
//        });
//        content.add(trayBox, c);
//
//        c.gridy++;
//        c.gridx = 0;
//        c.anchor = GridBagConstraints.WEST;
//        c.fill = GridBagConstraints.HORIZONTAL;
//        JLabel startupLabel = new JLabel("Launch at startup (beta)");
//        startupLabel.setForeground(UIStyle.TEXT_COLOR);
//        startupLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
//        content.add(startupLabel, c);
//
//        c.gridx = 1;
//        c.anchor = GridBagConstraints.CENTER;
//        c.fill = GridBagConstraints.NONE;
//        JCheckBox startupBox = new JCheckBox();
//        UIStyle.styleCheckbox(startupBox);
//        startupBox.setSelected(ConfigManager.isStartupEnabled());
//        startupBox.addActionListener(e -> {
//            ConfigManager.setStartup(startupBox.isSelected());
//            AppLogger.info("Settings: Launch at startup set to " + startupBox.isSelected());
//        });
//        content.add(startupBox, c);

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(9, 10, 4, 10);
        JButton closeBtn = new JButton("Close");
        UIStyle.styleButton(closeBtn);
        closeBtn.addActionListener(e -> dispose());
        content.add(closeBtn, c);

        setContentPane(content);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }
}

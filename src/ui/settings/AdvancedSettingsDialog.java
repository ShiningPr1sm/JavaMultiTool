package ui.settings;

import db.DatabaseProvider;
import db.UserRepository;
import ui.UIStyle;
import util.AppLogger;
import util.ConfigManager;
import util.I18n;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AdvancedSettingsDialog extends JDialog {

    private record SettingDef(String label, String key, boolean defaultValue) {}

    private static final List<SettingDef> SETTINGS = List.of(
            new SettingDef("settings.adv.autoUpdate", "autoUpdate", true),
            new SettingDef("settings.adv.dailyQuote", "dailyQuote", true),
            new SettingDef("settings.adv.bdayReminders", "birthdayReminders", true),
            new SettingDef("settings.adv.useInternet", "useInternet", true),
            new SettingDef("settings.adv.notifications", "showNotifications", true),
            new SettingDef("settings.adv.saveData", "saveData", true),
            new SettingDef("settings.adv.minToTray", "minToTray", true),
            new SettingDef("settings.adv.launchStartup", "launchAtStartup", true)
    );

    public AdvancedSettingsDialog(Frame owner, String login) {
        super(owner, I18n.t("settings.advanced"), true);
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

        JLabel nameHeader = new JLabel(I18n.t("settings.adv.name"), SwingConstants.LEFT);
        nameHeader.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameHeader.setForeground(UIStyle.ACCENT_COLOR);
        content.add(nameHeader, c);

        c.gridx = 1;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.NONE;
        JLabel valueHeader = new JLabel(I18n.t("settings.adv.value"));
        valueHeader.setFont(new Font("Segoe UI", Font.BOLD, 13));
        valueHeader.setForeground(UIStyle.ACCENT_COLOR);
        content.add(valueHeader, c);

        c.insets = new Insets(4, 10, 4, 10);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        for (SettingDef def : SETTINGS) {
            c.gridy++;
            c.gridx = 0;
            JLabel label = new JLabel(I18n.t(def.label()));
            label.setForeground(UIStyle.TEXT_COLOR);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            content.add(label, c);

            c.gridx = 1;
            c.anchor = GridBagConstraints.CENTER;
            c.fill = GridBagConstraints.NONE;
            boolean value = switch (def.key()) {
                // Real state comes from the Windows Run key, not the saved preference,
                // so the checkbox always reflects what actually happens on boot.
                case "launchAtStartup" -> ConfigManager.isStartupEnabled();
                case "minToTray" -> userRepo.isCloseToTrayEnabled(login);
                default -> Boolean.parseBoolean(ConfigManager.loadProperty(
                        def.key(), String.valueOf(def.defaultValue())));
            };
            JCheckBox cb = new JCheckBox();
            UIStyle.styleCheckbox(cb);
            cb.setSelected(value);
            cb.addActionListener(e -> {
                switch (def.key()) {
                    case "minToTray" -> userRepo.setCloseToTray(login, cb.isSelected());
                    case "launchAtStartup" -> ConfigManager.setStartup(cb.isSelected());
                    default -> ConfigManager.saveProperty(def.key(), String.valueOf(cb.isSelected()));
                }
                AppLogger.info("Settings: '" + def.key() + "' set to " + cb.isSelected());
            });
            content.add(cb, c);

            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
        }
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(9, 10, 4, 10);
        JButton closeBtn = new JButton(I18n.t("common.close"));
        UIStyle.styleButton(closeBtn);
        closeBtn.addActionListener(e -> dispose());
        content.add(closeBtn, c);

        setContentPane(content);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }
}

package ui.utilstab;

import service.Services;
import ui.StyledDialog;
import ui.UIStyle;
import util.AppLogger;
import util.I18n;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.security.SecureRandom;

public class PasswordGeneratorPanel extends JPanel {
    private final JSpinner lengthSpinner;
    private final JCheckBox upperCb, lowerCb, digitCb, symbolCb;
    private final JTextField passwordField;
    private final JLabel strengthLabel;
    private static final String SYMBOLS = "!@#$%^&*()_+-=[]{}|;:',.<>?/~`";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static Services services;
    private static String login;

    public PasswordGeneratorPanel(Services services, String login) {
        this.services = services;
        this.login = login;
        setLayout(new BorderLayout(10, 10));
        setBackground(UIStyle.BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = GridBagConstraints.RELATIVE;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 10, 6, 10);

        JLabel title = new JLabel(I18n.t("pwgen.title"), SwingConstants.CENTER);
        title.setForeground(UIStyle.ACCENT_COLOR);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        center.add(title, c);

        c.insets = new Insets(15, 10, 6, 10);
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topRow.setOpaque(false);
        JLabel lenLbl = new JLabel(I18n.t("pwgen.length"));
        lenLbl.setForeground(UIStyle.TEXT_COLOR);
        lengthSpinner = new JSpinner(new SpinnerNumberModel(16, 4, 128, 1));
        UIStyle.styleSpinner(lengthSpinner);
        ((JSpinner.DefaultEditor) lengthSpinner.getEditor()).getTextField().setPreferredSize(new Dimension(60, 25));
        topRow.add(lenLbl);
        topRow.add(lengthSpinner);

        upperCb = new JCheckBox(I18n.t("pwgen.upper"), true);
        lowerCb = new JCheckBox(I18n.t("pwgen.lower"), true);
        digitCb = new JCheckBox(I18n.t("pwgen.digits"), true);
        symbolCb = new JCheckBox(I18n.t("pwgen.symbols"), true);
        for (var cb : new JCheckBox[]{upperCb, lowerCb, digitCb, symbolCb}) {
            UIStyle.styleCheckbox(cb);
            topRow.add(cb);
        }
        center.add(topRow, c);

        passwordField = new JTextField(25);
        passwordField.setFont(new Font("Consolas", Font.PLAIN, 14));
        passwordField.setHorizontalAlignment(JTextField.CENTER);
        UIStyle.styleTextField(passwordField);
        passwordField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateStrength(passwordField.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateStrength(passwordField.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateStrength(passwordField.getText());
            }
        });
        center.add(passwordField, c);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnRow.setOpaque(false);
        JButton genBtn = new JButton(I18n.t("common.generate"));
        UIStyle.styleButton(genBtn);
        genBtn.addActionListener(e -> generatePassword());
        JButton copyBtn = new JButton(I18n.t("common.copy"));
        UIStyle.styleButton(copyBtn);
        copyBtn.addActionListener(e -> {
            String pwd = passwordField.getText();
            if (!pwd.isEmpty()) {
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(pwd), null);
                AppLogger.info("Password copied to clipboard");
                services.achievementService().complete(login, "security");
            } else {
                StyledDialog.show(SwingUtilities.windowForComponent(this), I18n.t("pwgen.errEmpty"));
            }
        });
        btnRow.add(genBtn);
        btnRow.add(copyBtn);
        center.add(btnRow, c);

        strengthLabel = new JLabel("", SwingConstants.CENTER);
        strengthLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        center.add(strengthLabel, c);

        add(center, BorderLayout.NORTH);

        generatePassword();
    }

    private void generatePassword() {
        StringBuilder pool = new StringBuilder();
        if (upperCb.isSelected()) pool.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        if (lowerCb.isSelected()) pool.append("abcdefghijklmnopqrstuvwxyz");
        if (digitCb.isSelected()) pool.append("0123456789");
        if (symbolCb.isSelected()) pool.append(SYMBOLS);

        if (pool.isEmpty()) {
            passwordField.setText("");
            strengthLabel.setText(I18n.t("pwgen.errNoType"));
            AppLogger.info("Password generation skipped: no character types selected");
            return;
        }

        int len = (int) lengthSpinner.getValue();
        StringBuilder pwd = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            pwd.append(pool.charAt(RANDOM.nextInt(pool.length())));
        }
        passwordField.setText(pwd.toString());
        updateStrength(pwd.toString());
        AppLogger.info("Password generated (length " + len + ")");
    }

    private void updateStrength(String pwd) {
        if (pwd == null || pwd.isEmpty()) {
            strengthLabel.setText("");
            return;
        }

        int types = 0;
        if (pwd.chars().anyMatch(Character::isUpperCase)) types++;
        if (pwd.chars().anyMatch(Character::isLowerCase)) types++;
        if (pwd.chars().anyMatch(Character::isDigit)) types++;
        if (pwd.chars().anyMatch(ch -> SYMBOLS.indexOf(ch) >= 0)) types++;

        int score = pwd.length() * types;
        if (score < 20) strengthLabel.setText(I18n.t("pwgen.weak"));
        else if (score < 40) strengthLabel.setText(I18n.t("pwgen.medium"));
        else strengthLabel.setText(I18n.t("pwgen.strong"));

        Color strColor;
        if (score < 20) strColor = new Color(255, 80, 80);
        else if (score < 40) strColor = new Color(255, 200, 80);
        else strColor = new Color(80, 255, 80);
        strengthLabel.setForeground(strColor);
    }
}

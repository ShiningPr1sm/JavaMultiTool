package ui.utilstab;

import ui.UIStyle;

import javax.swing.*;
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

    public PasswordGeneratorPanel() {
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

        JLabel title = new JLabel("Password Generator");
        title.setForeground(UIStyle.ACCENT_COLOR);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        center.add(title, c);

        c.insets = new Insets(15, 10, 6, 10);
        JPanel lenRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        lenRow.setOpaque(false);
        JLabel lenLbl = new JLabel("Length:");
        lenLbl.setForeground(UIStyle.TEXT_COLOR);
        lengthSpinner = new JSpinner(new SpinnerNumberModel(16, 4, 128, 1));
        UIStyle.styleSpinner(lengthSpinner);
        ((JSpinner.DefaultEditor) lengthSpinner.getEditor()).getTextField().setPreferredSize(new Dimension(60, 25));
        lenRow.add(lenLbl);
        lenRow.add(lengthSpinner);
        center.add(lenRow, c);

        c.insets = new Insets(6, 10, 6, 10);
        upperCb = new JCheckBox("Uppercase (A-Z)", true);
        lowerCb = new JCheckBox("Lowercase (a-z)", true);
        digitCb = new JCheckBox("Digits (0-9)", true);
        symbolCb = new JCheckBox("Symbols (!@#...)", true);
        for (var cb : new JCheckBox[]{upperCb, lowerCb, digitCb, symbolCb}) {
            cb.setOpaque(false);
            cb.setForeground(UIStyle.TEXT_COLOR);
            center.add(cb, c);
        }

        passwordField = new JTextField(25);
        passwordField.setEditable(false);
        passwordField.setFont(new Font("Consolas", Font.PLAIN, 14));
        passwordField.setHorizontalAlignment(JTextField.CENTER);
        UIStyle.styleTextField(passwordField);
        center.add(passwordField, c);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnRow.setOpaque(false);
        JButton genBtn = new JButton("Generate");
        UIStyle.styleButton(genBtn);
        genBtn.addActionListener(e -> generatePassword());
        JButton copyBtn = new JButton("Copy");
        UIStyle.styleButton(copyBtn);
        copyBtn.addActionListener(e -> {
            String pwd = passwordField.getText();
            if (!pwd.isEmpty()) {
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(pwd), null);
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
            strengthLabel.setText("Select at least one character type");
            return;
        }

        int len = (int) lengthSpinner.getValue();
        StringBuilder pwd = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            pwd.append(pool.charAt(RANDOM.nextInt(pool.length())));
        }
        passwordField.setText(pwd.toString());

        int types = (upperCb.isSelected() ? 1 : 0) + (lowerCb.isSelected() ? 1 : 0)
                + (digitCb.isSelected() ? 1 : 0) + (symbolCb.isSelected() ? 1 : 0);
        int score = len * types;
        if (score < 20) strengthLabel.setText("Strength: Weak");
        else if (score < 40) strengthLabel.setText("Strength: Medium");
        else strengthLabel.setText("Strength: Strong");

        Color strColor;
        if (score < 20) strColor = new Color(255, 80, 80);
        else if (score < 40) strColor = new Color(255, 200, 80);
        else strColor = new Color(80, 255, 80);
        strengthLabel.setForeground(strColor);
    }
}

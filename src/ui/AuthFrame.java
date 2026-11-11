package ui;

import db.DatabaseProvider;
import service.Services;
import util.I18n;

import javax.swing.*;
import java.awt.*;

public class AuthFrame extends JFrame {
    private static final int FRAME_SIZE_WIDTH = 700;
    private static final int FRAME_SIZE_HEIGHT = 450;

    private final Services services;

    public AuthFrame(Services services) {
        this.services = services;
        setTitle(I18n.t("auth.frame.title"));
        UIStyle.setAppIcon(this);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(
                (int) ((screenSize.getWidth() - FRAME_SIZE_WIDTH) / 2),
                (int) ((screenSize.getHeight() - FRAME_SIZE_HEIGHT) / 2),
                FRAME_SIZE_WIDTH,
                FRAME_SIZE_HEIGHT
        );

        JPanel outerPanel = new JPanel(new GridBagLayout());
        outerPanel.setBackground(UIStyle.HEADER_COLOR);

        JLabel titleLabel = new JLabel("MultiTool");
        titleLabel.setForeground(UIStyle.ACCENT_COLOR);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));

        JLabel subtitleLabel = new JLabel(I18n.t("auth.subtitle"));
        subtitleLabel.setForeground(UIStyle.TEXT_COLOR);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(UIStyle.SIDE_BOX);
        formPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JLabel loginLabel = new JLabel(I18n.t("auth.login"));
        loginLabel.setForeground(UIStyle.TEXT_COLOR);
        loginLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JTextField loginField = new JTextField(20);
        UIStyle.styleTextField(loginField);

        JLabel passwordLabel = new JLabel(I18n.t("auth.password"));
        passwordLabel.setForeground(UIStyle.TEXT_COLOR);
        passwordLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JPasswordField passwordField = new JPasswordField(20);
        UIStyle.styleTextField(passwordField);

        JButton loginButton = new JButton(I18n.t("auth.loginBtn"));
        loginButton.setPreferredSize(new Dimension(300, 38));
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        UIStyle.styleButton(loginButton);

        JButton toggleButton = new JButton(I18n.t("auth.noAccount"));
        toggleButton.setPreferredSize(new Dimension(300, 38));
        toggleButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        UIStyle.styleButton(toggleButton);

        Runnable doLogin = () -> {
            String login = loginField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            if (login.isEmpty() || password.isEmpty()) {
                StyledDialog.show(this, I18n.t("common.fieldsEmpty"));
                return;
            }
            var userRepo = DatabaseProvider.getUserRepository();
            if (userRepo.checkLogin(login, password)) {
                String theme = userRepo.getTheme(login);
                UIStyle.applyTheme(theme);
                new MainFrame(login, services);
                userRepo.updateLastLoginDate(login);
                this.dispose();
            } else {
                StyledDialog.show(this, I18n.t("auth.invalidCredentials"));
            }
        };

        Runnable doRegister = () -> {
            String login = loginField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            if (login.isEmpty() || password.isEmpty()) {
                StyledDialog.show(this, I18n.t("common.fieldsEmpty"));
                return;
            }
            if (DatabaseProvider.getUserRepository().register(login, password)) {
                StyledDialog.show(this, I18n.t("auth.registerSuccess"));
            } else {
                StyledDialog.show(this, I18n.t("auth.loginTaken"));
            }
        };

        boolean[] registerMode = {false};

        Runnable submit = () -> {
            if (registerMode[0]) doRegister.run();
            else doLogin.run();
        };

        loginButton.addActionListener(e -> submit.run());
        passwordField.addActionListener(e -> submit.run());

        toggleButton.addActionListener(e -> {
            registerMode[0] = !registerMode[0];
            loginButton.setText(registerMode[0] ? I18n.t("auth.register") : I18n.t("auth.loginBtn"));
            toggleButton.setText(registerMode[0] ? I18n.t("auth.haveAccount") : I18n.t("auth.noAccount"));
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;

        gbc.insets = new Insets(0, 0, 0, 0);
        formPanel.add(titleLabel, gbc);
        gbc.insets = new Insets(5, 0, 0, 0);
        formPanel.add(subtitleLabel, gbc);
        gbc.insets = new Insets(20, 0, 0, 0);
        formPanel.add(loginLabel, gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(5, 0, 0, 0);
        formPanel.add(loginField, gbc);
        gbc.insets = new Insets(10, 0, 0, 0);
        formPanel.add(passwordLabel, gbc);
        gbc.insets = new Insets(5, 0, 0, 0);
        formPanel.add(passwordField, gbc);
        gbc.insets = new Insets(15, 0, 0, 0);
        formPanel.add(loginButton, gbc);
        gbc.insets = new Insets(8, 0, 0, 0);
        formPanel.add(toggleButton, gbc);

        outerPanel.add(formPanel);
        setContentPane(outerPanel);
        setVisible(true);
    }
}

package ui;

import db.DatabaseProvider;
import service.Services;

import javax.swing.*;
import java.awt.*;

public class AuthFrame extends JFrame {
    private static final int FRAME_SIZE_WIDTH = 700;
    private static final int FRAME_SIZE_HEIGHT = 450;

    private final Services services;

    public AuthFrame(Services services) {
        this.services = services;
        setTitle("MultiTool - Authentication");
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

        JLabel subtitleLabel = new JLabel("Sign in or create an account");
        subtitleLabel.setForeground(UIStyle.TEXT_COLOR);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JPanel formPanel = new JPanel();
        formPanel.setBackground(UIStyle.SIDE_BOX);
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setPreferredSize(new Dimension(320, 340));
        formPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JLabel loginLabel = new JLabel("Login:");
        loginLabel.setForeground(UIStyle.TEXT_COLOR);
        loginLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        loginLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField loginField = new JTextField(20);
        loginField.setMaximumSize(new Dimension(300, 35));
        UIStyle.styleTextField(loginField);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setForeground(UIStyle.TEXT_COLOR);
        passwordLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        passwordLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPasswordField passwordField = new JPasswordField(20);
        passwordField.setMaximumSize(new Dimension(300, 35));
        UIStyle.styleTextField(passwordField);

        JButton loginButton = new JButton("Login");
        loginButton.setMaximumSize(new Dimension(300, 38));
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        UIStyle.styleButton(loginButton);

        JButton registerButton = new JButton("Register");
        registerButton.setMaximumSize(new Dimension(300, 38));
        registerButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        UIStyle.styleButton(registerButton);
        registerButton.setBackground(UIStyle.SECONDARY_BG);

        for (JComponent comp : new JComponent[]{loginField, passwordField, loginButton, registerButton}) {
            comp.setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        Runnable doLogin = () -> {
            String login = loginField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            if (login.isEmpty() || password.isEmpty()) {
                StyledDialog.show(this, "Fields cannot be empty");
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
                StyledDialog.show(this, "Invalid login or password");
            }
        };

        loginButton.addActionListener(e -> doLogin.run());

        passwordField.addActionListener(e -> doLogin.run());

        registerButton.addActionListener(e -> {
            String login = loginField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            if (login.isEmpty() || password.isEmpty()) {
                StyledDialog.show(this, "Fields cannot be empty");
                return;
            }
            if (DatabaseProvider.getUserRepository().register(login, password)) {
                StyledDialog.show(this, "Registration successful! You can now log in.");
            } else {
                StyledDialog.show(this, "Login already taken");
            }
        });

        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        formPanel.add(titleLabel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        formPanel.add(subtitleLabel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        formPanel.add(loginLabel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        formPanel.add(loginField);
        formPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        formPanel.add(passwordLabel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        formPanel.add(passwordField);
        formPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        formPanel.add(loginButton);
        formPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        formPanel.add(registerButton);

        outerPanel.add(formPanel);
        setContentPane(outerPanel);
        setVisible(true);
    }
}

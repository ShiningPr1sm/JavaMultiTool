package ui;

import javax.swing.*;
import java.awt.*;

public class StyledDialog {

    public static void show(Window parent, String message) {
        show(parent, message, null);
    }

    public static void show(Window parent, String message, String title) {
        JDialog dialog = new JDialog(parent, title != null ? title : "", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setBackground(UIStyle.BG_COLOR);

        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(UIStyle.SIDE_BOX);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(25, 30, 20, 30)));

        JLabel msgLabel = new JLabel(message, SwingConstants.CENTER);
        msgLabel.setForeground(UIStyle.TEXT_COLOR);
        msgLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        msgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton okButton = new JButton("OK");
        okButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        okButton.setPreferredSize(new Dimension(100, 35));
        okButton.setMaximumSize(new Dimension(100, 35));
        UIStyle.styleButton(okButton);
        okButton.addActionListener(e -> dialog.dispose());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnPanel.setBackground(UIStyle.SIDE_BOX);
        btnPanel.add(okButton);

        dialog.getRootPane().setDefaultButton(okButton);

        panel.add(msgLabel, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.pack();

        Dimension dialogSize = dialog.getSize();
        dialogSize.width = Math.max(dialogSize.width, 320);
        dialog.setSize(dialogSize);

        if (parent != null) {
            dialog.setLocationRelativeTo(parent);
        } else {
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            dialog.setLocation(
                    (screen.width - dialog.getWidth()) / 2,
                    (screen.height - dialog.getHeight()) / 2);
        }

        dialog.setVisible(true);
    }

    public static boolean confirm(Window parent, JPanel content, String title) {
        JDialog dialog = new JDialog(parent, title != null ? title : "", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setBackground(UIStyle.BG_COLOR);

        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(UIStyle.SIDE_BOX);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(25, 30, 20, 30)));

        final boolean[] result = {false};

        JButton okButton = new JButton("OK");
        okButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        okButton.setPreferredSize(new Dimension(100, 35));
        UIStyle.styleButton(okButton);
        okButton.addActionListener(e -> {
            result[0] = true;
            dialog.dispose();
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cancelButton.setPreferredSize(new Dimension(100, 35));
        UIStyle.styleButton(cancelButton);
        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.getRootPane().setDefaultButton(okButton);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.setBackground(UIStyle.SIDE_BOX);
        btnPanel.add(okButton);
        btnPanel.add(cancelButton);

        panel.add(content, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.pack();

        Dimension dialogSize = dialog.getSize();
        dialogSize.width = Math.max(dialogSize.width, 360);
        dialog.setSize(dialogSize);

        if (parent != null) {
            dialog.setLocationRelativeTo(parent);
        } else {
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            dialog.setLocation(
                    (screen.width - dialog.getWidth()) / 2,
                    (screen.height - dialog.getHeight()) / 2);
        }

        dialog.setVisible(true);
        return result[0];
    }
}

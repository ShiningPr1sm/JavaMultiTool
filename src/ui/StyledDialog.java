package ui;

import javax.swing.*;
import java.awt.*;

import util.I18n;

/**
 * Factory for undecorated modal dialogs styled with the app palette
 * (a plain message box, a confirm-with-custom-content box, a yes/no box).
 */
public class StyledDialog {

    /** Shows a modal message box with a single OK button. */
    public static void show(Window parent, String message) {
        show(parent, message, null);
    }

    /** Shows a modal message box with a single OK button and an optional title. */
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

        JButton okButton = new JButton(I18n.t("common.ok"));
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

    /** Shows a modal OK/Cancel dialog around caller-supplied content; returns true when OK was pressed. */
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

        JButton okButton = new JButton(I18n.t("common.ok"));
        okButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        okButton.setPreferredSize(new Dimension(100, 35));
        UIStyle.styleButton(okButton);
        okButton.addActionListener(e -> {
            result[0] = true;
            dialog.dispose();
        });

        JButton cancelButton = new JButton(I18n.t("common.cancel"));
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

    /**
     * Shows a modal yes/no dialog; returns true when Yes was pressed.
     * The message is rendered as centered HTML rather than a wrapping
     * JTextArea, because pack() mis-measures wrapping text areas (cut-off
     * text at the fixed dialog width) and a focused text area paints a
     * stray caret bar before the first letter.
     */
    public static boolean confirmYesNo(Window parent, String message, String title) {
        JDialog dialog = new JDialog(parent, title != null ? title : "", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setBackground(UIStyle.BG_COLOR);

        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(UIStyle.SIDE_BOX);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(25, 30, 20, 30)));

        final boolean[] result = {false};

        // HTML label instead of a wrapping JTextArea (see confirmYesNo javadoc)
        String escaped = message
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");
        JLabel msgLabel = new JLabel(
                "<html><div style='text-align: center; width: 300px;'>" + escaped + "</div></html>",
                SwingConstants.CENTER);
        msgLabel.setForeground(UIStyle.TEXT_COLOR);
        msgLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JButton yesButton = new JButton(I18n.t("common.yes"));
        yesButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        yesButton.setPreferredSize(new Dimension(100, 35));
        UIStyle.styleButton(yesButton);
        yesButton.addActionListener(e -> {
            result[0] = true;
            dialog.dispose();
        });

        JButton noButton = new JButton(I18n.t("common.no"));
        noButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        noButton.setPreferredSize(new Dimension(100, 35));
        UIStyle.styleButton(noButton);
        noButton.addActionListener(e -> dialog.dispose());

        dialog.getRootPane().setDefaultButton(yesButton);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.setBackground(UIStyle.SIDE_BOX);
        btnPanel.add(yesButton);
        btnPanel.add(noButton);

        panel.add(msgLabel, BorderLayout.CENTER);
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

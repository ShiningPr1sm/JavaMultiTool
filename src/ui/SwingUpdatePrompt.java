package ui;

import util.MarkdownUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class SwingUpdatePrompt {

    public enum Choice { UPDATE, KEEP_OLD }

    public static Choice show(String currentVersion, String newVersion, String notesMarkdown) {
        Choice[] result = { Choice.KEEP_OLD };

        JDialog dialog = new JDialog();
        dialog.setUndecorated(false);
        dialog.setTitle("New version is here!");
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);

        UIStyle.setAppIcon(dialog);
        dialog.getContentPane().setBackground(UIStyle.BG_COLOR);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(UIStyle.BG_COLOR);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel versionLabel = new JLabel(currentVersion + "   →   " + newVersion, SwingConstants.CENTER);
        versionLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        versionLabel.setForeground(UIStyle.ACCENT_COLOR);
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        versionLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, versionLabel.getPreferredSize().height));

        JLabel whatsNewLabel = new JLabel("What's new?");
        whatsNewLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        whatsNewLabel.setForeground(UIStyle.TEXT_COLOR);
        whatsNewLabel.setBorder(new EmptyBorder(12, 0, 6, 0));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(UIStyle.BG_COLOR);
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        whatsNewLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        topPanel.add(versionLabel);
        topPanel.add(whatsNewLabel);

        String notesHtml = "<html><body style='font-family: Segoe UI, Segoe UI Emoji; font-size: 12px; "
                + "color: rgb(230,230,230); background-color: rgb(40,40,45); margin: 4px;'>"
                + MarkdownUtil.toPlainText(notesMarkdown).replace("\n", "<br/>")
                + "</body></html>";

        JEditorPane editorPane = new JEditorPane("text/html", notesHtml);
        editorPane.setEditable(false);
        editorPane.setOpaque(true);
        editorPane.setBackground(new Color(40, 40, 45));
        editorPane.setCaret(new javax.swing.text.DefaultCaret() {
            @Override
            public void paint(Graphics g) {}

            @Override
            public boolean isVisible() { return false; }
        });
        editorPane.setHighlighter(null);

        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setPreferredSize(new Dimension(440, 260));
        scrollPane.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR, 1));
        scrollPane.getViewport().setBackground(new Color(40, 40, 45));
        UIStyle.styleScrollBar(scrollPane);

        JButton updateButton = new JButton("Update");
        JButton keepButton = new JButton("Keep old version");
        UIStyle.styleButton(updateButton);
        UIStyle.styleButton(keepButton);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttonPanel.setBackground(UIStyle.BG_COLOR);
        buttonPanel.setBorder(new EmptyBorder(16, 0, 0, 0));
        buttonPanel.add(keepButton);
        buttonPanel.add(updateButton);

        updateButton.addActionListener(e -> {
            result[0] = Choice.UPDATE;
            dialog.dispose();
        });
        keepButton.addActionListener(e -> {
            result[0] = Choice.KEEP_OLD;
            dialog.dispose();
        });

        root.add(topPanel, BorderLayout.NORTH);
        root.add(scrollPane, BorderLayout.CENTER);
        root.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        return result[0];
    }
}

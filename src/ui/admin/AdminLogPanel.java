package ui.admin;

import ui.UIStyle;
import util.AppLogger;
import util.I18n;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;

public class AdminLogPanel extends JPanel {

    private static final Color INF_COLOR = new Color(0, 255, 0);
    private static final Color ADM_COLOR = new Color(255, 165, 0);
    private static final Color ERR_COLOR = new Color(255, 110, 110);

    public AdminLogPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);

        JTextPane textPane = new JTextPane();
        textPane.setBackground(Color.BLACK);
        textPane.setFont(new Font("Consolas", Font.PLAIN, 12));
        textPane.setEditable(false);

        StyledDocument doc = textPane.getStyledDocument();
        Style infoStyle = doc.addStyle("info", null);
        StyleConstants.setForeground(infoStyle, INF_COLOR);
        Style admStyle = doc.addStyle("admin", null);
        StyleConstants.setForeground(admStyle, ADM_COLOR);
        Style errStyle = doc.addStyle("error", null);
        StyleConstants.setForeground(errStyle, ERR_COLOR);

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setBorder(null);
        UIStyle.styleScrollBar(scrollPane);
        AppLogger.setConsoleOutput(msg -> SwingUtilities.invokeLater(() -> appendStyled(textPane, msg)));

        add(scrollPane, BorderLayout.CENTER);

        JButton clearBtn = new JButton(I18n.t("admin.clearConsole"));
        UIStyle.styleButton(clearBtn);
        clearBtn.addActionListener(e -> textPane.setText(""));
        add(clearBtn, BorderLayout.SOUTH);
    }

    private static void appendStyled(JTextPane textPane, String msg) {
        StyledDocument doc = textPane.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), msg, styleFor(doc, msg));
        } catch (BadLocationException e) {
            AppLogger.error("AdminLogPanel: failed to append log line: " + e.getMessage());
            textPane.setText(textPane.getText() + msg);
        }
    }

    private static Style styleFor(StyledDocument doc, String msg) {
        if (msg.contains("[ERR]")) {
            return doc.getStyle("error");
        } else if (msg.contains("[ADM]")) {
            return doc.getStyle("admin");
        }
        return doc.getStyle("info");
    }
}

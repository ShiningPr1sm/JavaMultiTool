package ui.admin;

import ui.UIStyle;
import util.AppLogger;
import javax.swing.*;
import java.awt.*;

public class AdminLogPanel extends JPanel {
    public AdminLogPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);

        JTextArea textArea = new JTextArea();
        textArea.setBackground(Color.BLACK);
        textArea.setForeground(new Color(0, 255, 0));
        textArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        textArea.setEditable(false);
        textArea.setLineWrap(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(null);
        UIStyle.styleScrollBar(scrollPane);
        AppLogger.setConsoleOutput(msg -> SwingUtilities.invokeLater(() -> textArea.append(msg)));

        add(scrollPane, BorderLayout.CENTER);

        JButton clearBtn = new JButton("Clear Console");
        UIStyle.styleButton(clearBtn);
        clearBtn.addActionListener(e -> textArea.setText(""));
        add(clearBtn, BorderLayout.SOUTH);
    }
}
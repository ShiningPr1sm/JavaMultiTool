package ui.utilstab;

import ui.UIStyle;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class UtilsPanel extends JPanel {
    public UtilsPanel() {
        setLayout(new BorderLayout());
        setBackground(UIStyle.BG_COLOR);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UIStyle.BG_COLOR);
        content.setBorder(new EmptyBorder(10, 10, 10, 10));

        content.add(wrapInSection("Color Picker / Converter", new ColorPickerPanel()));
        content.add(Box.createRigidArea(new Dimension(0, 10)));
        content.add(wrapInSection("Password Generator", new PasswordGeneratorPanel()));
        content.add(Box.createRigidArea(new Dimension(0, 10)));
        content.add(wrapInSection("QR Generator & Decoder", new QRToolsPanel()));
        content.add(Box.createRigidArea(new Dimension(0, 10)));
        content.add(wrapInSection("Network Tools (ping / port check / whois / my IP)", new NetworkToolsPanel()));

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBackground(UIStyle.BG_COLOR);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel wrapInSection(String title, JPanel panel) {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(UIStyle.BG_COLOR);
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIStyle.ACCENT_COLOR),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.PLAIN, 13),
                UIStyle.TEXT_COLOR
        );
        section.setBorder(border);
        section.add(panel, BorderLayout.CENTER);
        return section;
    }
}

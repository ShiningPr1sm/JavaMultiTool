package ui;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

public class UIStyle {
    public static Color BG_COLOR = new Color(25, 25, 25);
    public static Color HEADER_COLOR = new Color(30, 30, 30);
    public static Color SECONDARY_BG = new Color(35, 35, 35);
    public static Color BUTTON_BG = new Color(40, 40, 40);
    public static Color SIDE_BOX = new Color(45, 45, 45);
    public static Color BUTTON_HOVER = new Color(55, 55, 55);
    public static Color BORDER_COLOR = new Color(60, 60, 60);
    public static Color BUTTON_PRESSED = new Color(65, 65, 65);
    public static Color PROGRESS_BAR = new Color(114, 99, 100);
    public static Color BG_PROGRESS_BAR = new Color(130, 150, 100);
    public static Color COMPLETED_ACH = new Color(60, 120, 60);
    public static Color ACCENT_COLOR = new Color(100, 200, 100);
    public static Color XP_LABEL_COLOR = new Color(180, 255, 180);
    public static Color TEXT_COLOR = Color.WHITE;

    public static void applyTheme(String themeName) {
        switch (themeName) {
            case "Original Dark" -> {
                BG_COLOR       = new Color(25, 25, 25);
                HEADER_COLOR   = new Color(30, 30, 30);
                SECONDARY_BG   = new Color(35, 35, 35);
                BUTTON_BG      = new Color(40, 40, 40);
                SIDE_BOX       = new Color(45, 45, 45);
                BUTTON_HOVER   = new Color(55, 55, 55);
                BORDER_COLOR   = new Color(60, 60, 60);
                BUTTON_PRESSED = new Color(65, 65, 65);
                BG_PROGRESS_BAR= new Color(45, 45, 45);
                PROGRESS_BAR   = new Color(180, 100, 100);
                ACCENT_COLOR   = new Color(100, 200, 100);
            }
            case "Midnight Blue" -> {
                BG_COLOR       = new Color(12, 14, 20);
                HEADER_COLOR   = new Color(18, 22, 30);
                SECONDARY_BG   = new Color(24, 28, 40);
                BUTTON_BG      = new Color(30, 38, 55);
                SIDE_BOX       = new Color(35, 45, 65);
                BORDER_COLOR   = new Color(50, 65, 90);
                BUTTON_HOVER   = new Color(60, 80, 115);
                BUTTON_PRESSED = new Color(80, 105, 145);
                BG_PROGRESS_BAR= new Color(20, 30, 50);
                PROGRESS_BAR   = new Color(0, 180, 255);
                ACCENT_COLOR   = new Color(0, 162, 255);
            }
            case "Deep Forest" -> {
                BG_COLOR       = new Color(15, 18, 15);
                HEADER_COLOR   = new Color(22, 28, 22);
                SECONDARY_BG   = new Color(28, 35, 28);
                BUTTON_BG      = new Color(35, 45, 35);
                SIDE_BOX       = new Color(40, 52, 40);
                BORDER_COLOR   = new Color(55, 70, 55);
                BUTTON_HOVER   = new Color(70, 90, 70);
                BUTTON_PRESSED = new Color(85, 110, 85);
                BG_PROGRESS_BAR= new Color(30, 40, 30);
                PROGRESS_BAR   = new Color(76, 182, 76);
                ACCENT_COLOR   = new Color(140, 255, 100);
            }
            case "Dracula" -> {
                BG_COLOR       = new Color(40, 42, 54);
                HEADER_COLOR   = new Color(33, 34, 44);
                SECONDARY_BG   = new Color(55, 55, 65);
                BUTTON_BG      = new Color(56, 58, 73);
                SIDE_BOX       = new Color(68, 71, 90);
                BORDER_COLOR   = new Color(98, 114, 164);
                BUTTON_HOVER   = new Color(80, 85, 110);
                BUTTON_PRESSED = new Color(100, 105, 130);
                BG_PROGRESS_BAR= new Color(45, 47, 65);
                PROGRESS_BAR   = new Color(255, 121, 198);
                ACCENT_COLOR   = new Color(189, 147, 249);
            }
            case "Crimson Ember" -> {
                BG_COLOR       = new Color(20, 15, 15);
                HEADER_COLOR   = new Color(28, 20, 20);
                SECONDARY_BG   = new Color(35, 25, 25);
                BUTTON_BG      = new Color(45, 30, 30);
                SIDE_BOX       = new Color(55, 35, 35);
                BORDER_COLOR   = new Color(75, 45, 45);
                BUTTON_HOVER   = new Color(95, 55, 55);
                BUTTON_PRESSED = new Color(120, 65, 65);
                BG_PROGRESS_BAR= new Color(40, 20, 20);
                PROGRESS_BAR   = new Color(255, 87, 34);
                ACCENT_COLOR   = new Color(255, 75, 75);
            } case "default" -> {
                BG_COLOR       = new Color(25, 25, 25);
                HEADER_COLOR   = new Color(30, 30, 30);
                SECONDARY_BG   = new Color(35, 35, 35);
                BUTTON_BG      = new Color(40, 40, 40);
                SIDE_BOX       = new Color(45, 45, 45);
                BUTTON_HOVER   = new Color(55, 55, 55);
                BORDER_COLOR   = new Color(60, 60, 60);
                BUTTON_PRESSED = new Color(65, 65, 65);
                PROGRESS_BAR   = new Color(114, 99, 100);
                BG_PROGRESS_BAR= new Color(130, 150, 100);
                COMPLETED_ACH  = new Color(60, 120, 60);
                ACCENT_COLOR   = new Color(100, 200, 100);
                XP_LABEL_COLOR = new Color(180, 255, 180);
            }
        }
        BORDER_COLOR = SECONDARY_BG.brighter();
        BUTTON_HOVER = BUTTON_BG.brighter();
    }


    public static void styleButton(AbstractButton btn) {
        btn.setOpaque(true);
        btn.setBackground(BUTTON_BG);
        btn.setForeground(TEXT_COLOR);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.getModel().addChangeListener(_ -> {
            ButtonModel m = btn.getModel();
            if (m.isPressed())
                btn.setBackground(BUTTON_PRESSED);
            else if (m.isRollover())
                btn.setBackground(BUTTON_HOVER);
            else btn.setBackground(BUTTON_BG);
        });
    }

    public static void styleScrollBar(JScrollPane sp) {
        sp.getVerticalScrollBar().setUnitIncrement(20);
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        sp.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = BUTTON_BG;
                this.trackColor = BG_COLOR;
            }
            @Override protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }
            @Override protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }
            private JButton createZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(thumbColor);
                g2.fillRoundRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height, 8, 8);
                g2.dispose();
            }
        });
        sp.setBorder(null);
        sp.getViewport().setBackground(BG_COLOR);
    }

    public static void styleComboBox(JComboBox<?> cb) {
        cb.setBackground(BUTTON_BG);
        cb.setForeground(TEXT_COLOR);
        cb.setFocusable(false);
        cb.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        cb.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton btn = new JButton("▼");
                btn.setBorder(BorderFactory.createEmptyBorder());
                btn.setContentAreaFilled(false);
                btn.setForeground(TEXT_COLOR);
                return btn;
            }
            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(BUTTON_BG);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setOpaque(true);
                lbl.setBackground(isSelected ? BUTTON_HOVER : BUTTON_BG);
                lbl.setForeground(TEXT_COLOR);
                lbl.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                return lbl;
            }
        });
    }

    public static void styleTabbedPane(JTabbedPane tabs) {
        tabs.setBackground(BG_COLOR);
        tabs.setForeground(Color.WHITE);
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.setFocusable(false);
        tabs.setBorder(BorderFactory.createEmptyBorder());

        UIManager.put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));
        UIManager.put("TabbedPane.tabsOverlapBorder", true);

        tabs.updateUI();
        tabs.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                highlight = BG_COLOR;
                lightHighlight = BG_COLOR;
                shadow = BG_COLOR;
                darkShadow = BG_COLOR;
                focus = BG_COLOR;
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {

            }
        });
    }

    /**
     * Стилизация основных кнопок категорий сайдбара (Photo & Video и т.д.)
     */
    public static void styleSidebarMainButton(JButton button, int sidebarWidth) {
        button.setMaximumSize(new Dimension(sidebarWidth - 20, 40));
        button.setPreferredSize(new Dimension(sidebarWidth - 20, 40));
        button.setFocusPainted(false);
        button.setForeground(TEXT_COLOR);
        button.setBackground(BUTTON_BG);
        button.setBorderPainted(false);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.getModel().addChangeListener(_ -> {
            ButtonModel model = button.getModel();
            if (model.isPressed()) {
                button.setBackground(ACCENT_COLOR.darker());
            } else if (model.isRollover()) {
                button.setBackground(BUTTON_HOVER);
            } else {
                button.setBackground(BUTTON_BG);
            }
        });
    }

    /**
     * Стилизация под-кнопок функций (Media Downloader, Timer и т.д.)
     */
    public static void styleSidebarSubButton(JButton button, int sidebarWidth) {
        button.setMaximumSize(new Dimension(sidebarWidth - 40, 30));
        button.setMinimumSize(new Dimension(sidebarWidth - 40, 30));
        button.setPreferredSize(new Dimension(sidebarWidth - 40, 30));
        button.setFocusPainted(false);
        button.setForeground(TEXT_COLOR);
        button.setBackground(BUTTON_BG);
        button.setBorderPainted(false);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.getModel().addChangeListener(_ -> {
            ButtonModel model = button.getModel();
            if (model.isPressed()) {
                button.setBackground(ACCENT_COLOR.darker());
            } else if (model.isRollover()) {
                button.setBackground(BUTTON_HOVER);
            } else {
                button.setBackground(BUTTON_BG);
            }
        });
    }
}
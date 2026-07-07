package ui.theme;

import ui.UIStyle;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

public class ComponentStyler {

    public static void styleButton(AbstractButton btn) {
        btn.setOpaque(true);
        btn.setBackground(UIStyle.BUTTON_BG);
        btn.setForeground(UIStyle.TEXT_COLOR);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.getModel().addChangeListener(e -> {
            ButtonModel m = btn.getModel();
            if (m.isPressed())
                btn.setBackground(UIStyle.BUTTON_PRESSED);
            else if (m.isRollover())
                btn.setBackground(UIStyle.BUTTON_HOVER);
            else btn.setBackground(UIStyle.BUTTON_BG);
        });
    }

    public static void styleTextField(JTextField field) {
        field.setBackground(UIStyle.SECONDARY_BG);
        field.setForeground(UIStyle.TEXT_COLOR);
        field.setCaretColor(UIStyle.TEXT_COLOR);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(5, 7, 5, 5)
        ));

        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIStyle.ACCENT_COLOR, 1),
                        BorderFactory.createEmptyBorder(5, 7, 5, 5)
                ));
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIStyle.BORDER_COLOR, 1),
                        BorderFactory.createEmptyBorder(5, 7, 5, 5)
                ));
            }
        });
    }

    public static void makeFocusable(JPanel panel) {
        panel.setFocusable(true);
        panel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                panel.requestFocusInWindow();
            }
        });
    }

    public static void styleScrollBar(JScrollPane sp) {
        sp.getVerticalScrollBar().setUnitIncrement(20);
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        sp.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = UIStyle.BUTTON_BG;
                this.trackColor = UIStyle.BG_COLOR;
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
        sp.getViewport().setBackground(UIStyle.BG_COLOR);
    }

    public static void styleComboBox(JComboBox<?> cb) {
        cb.setBackground(UIStyle.BUTTON_BG);
        cb.setForeground(UIStyle.TEXT_COLOR);
        cb.setFocusable(false);
        cb.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));

        cb.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected javax.swing.plaf.basic.ComboPopup createPopup() {
                return new javax.swing.plaf.basic.BasicComboPopup(comboBox) {
                    @Override
                    protected JScrollPane createScroller() {
                        JScrollPane sp = super.createScroller();
                        ComponentStyler.styleScrollBar(sp);
                        sp.setBorder(null);
                        return sp;
                    }
                };
            }

            @Override
            protected JButton createArrowButton() {
                JButton btn = new JButton("\u25BC");
                btn.setBorder(BorderFactory.createEmptyBorder());
                btn.setContentAreaFilled(false);
                btn.setFocusPainted(false);
                btn.setForeground(UIStyle.TEXT_COLOR);
                return btn;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(UIStyle.BUTTON_BG);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });

        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setOpaque(true);

                if (isSelected) {
                    lbl.setBackground(UIStyle.BUTTON_HOVER);
                    lbl.setForeground(UIStyle.ACCENT_COLOR);
                } else {
                    lbl.setBackground(UIStyle.BUTTON_BG);
                    lbl.setForeground(UIStyle.TEXT_COLOR);
                }

                lbl.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                return lbl;
            }
        });

        Object child = cb.getAccessibleContext().getAccessibleChild(0);
        if (child instanceof javax.swing.plaf.basic.BasicComboPopup popup) {
            popup.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));
        }
    }

    public static void styleTabbedPane(JTabbedPane tabs) {
        tabs.setBackground(UIStyle.BG_COLOR);
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
                highlight = UIStyle.BG_COLOR;
                lightHighlight = UIStyle.BG_COLOR;
                shadow = UIStyle.BG_COLOR;
                darkShadow = UIStyle.BG_COLOR;
                focus = UIStyle.BG_COLOR;
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {

            }
        });
    }

    public static void styleSidebarMainButton(JButton button, int sidebarWidth) {
        button.setMaximumSize(new Dimension(sidebarWidth - 20, 40));
        button.setPreferredSize(new Dimension(sidebarWidth - 20, 40));
        button.setFocusPainted(false);
        button.setForeground(UIStyle.TEXT_COLOR);
        button.setBackground(UIStyle.BUTTON_BG);
        button.setBorderPainted(false);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.getModel().addChangeListener(e -> {
            ButtonModel model = button.getModel();
            if (model.isPressed()) {
                button.setBackground(UIStyle.ACCENT_COLOR.darker());
            } else if (model.isRollover()) {
                button.setBackground(UIStyle.BUTTON_HOVER);
            } else {
                button.setBackground(UIStyle.BUTTON_BG);
            }
        });
    }

    public static void styleSidebarSubButton(JButton button, int sidebarWidth) {
        button.setMaximumSize(new Dimension(sidebarWidth - 40, 30));
        button.setMinimumSize(new Dimension(sidebarWidth - 40, 30));
        button.setPreferredSize(new Dimension(sidebarWidth - 40, 30));
        button.setFocusPainted(false);
        button.setForeground(UIStyle.TEXT_COLOR);
        button.setBackground(UIStyle.BUTTON_BG);
        button.setBorderPainted(false);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.getModel().addChangeListener(e -> {
            ButtonModel model = button.getModel();
            if (model.isPressed()) {
                button.setBackground(UIStyle.ACCENT_COLOR.darker());
            } else if (model.isRollover()) {
                button.setBackground(UIStyle.BUTTON_HOVER);
            } else {
                button.setBackground(UIStyle.BUTTON_BG);
            }
        });
    }
}

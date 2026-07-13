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
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
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
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
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
                lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
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

    public static void styleProgressBar(JProgressBar pb) {
        pb.setForeground(UIStyle.PROGRESS_BAR);
        pb.setBackground(UIStyle.BG_PROGRESS_BAR);
        pb.setBorder(BorderFactory.createEmptyBorder());
        pb.setOpaque(false);
        pb.setAlignmentX(Component.LEFT_ALIGNMENT);
        pb.setPreferredSize(new Dimension(pb.getPreferredSize().width, 20));
        pb.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        pb.setUI(new javax.swing.plaf.basic.BasicProgressBarUI() {
            @Override
            protected void paintDeterminate(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = c.getWidth();
                int height = c.getHeight();

                g2d.setColor(pb.getBackground());
                g2d.fillRect(0, 0, width, height);

                double percent = Math.min(1.0, (double) pb.getValue() / Math.max(1, pb.getMaximum()));
                int progressWidth = (int) (width * percent);
                g2d.setColor(pb.getForeground());
                g2d.fillRect(0, 0, progressWidth, height);

                g2d.setColor(UIStyle.TEXT_COLOR);
                FontMetrics fm = g2d.getFontMetrics();
                int percentDisplay = (int) Math.round(percent * 100);
                String text = percentDisplay + "%";
                int textX = (width - fm.stringWidth(text)) / 2;
                int textY = (height - fm.getHeight()) / 2 + fm.getAscent();
                g2d.drawString(text, textX, textY);

                g2d.dispose();
            }
        });
    }

    public static void styleSpinner(JSpinner s) {
        s.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));
        s.setBackground(UIStyle.SIDE_BOX);
        s.setForeground(UIStyle.TEXT_COLOR);
        JComponent editor = s.getEditor();
        if (editor instanceof JSpinner.NumberEditor numEditor) {
            JFormattedTextField tf = numEditor.getTextField();
            tf.setBackground(UIStyle.SIDE_BOX);
            tf.setForeground(UIStyle.TEXT_COLOR);
            tf.setCaretColor(UIStyle.TEXT_COLOR);
        }
        s.setUI(new javax.swing.plaf.basic.BasicSpinnerUI() {
            @Override
            protected Component createPreviousButton() {
                return createArrowButton(false);
            }
            @Override
            protected Component createNextButton() {
                return createArrowButton(true);
            }
            private JButton createArrowButton(boolean up) {
                JButton btn = new JButton() {
                    @Override
                    public Dimension getPreferredSize() {
                        return new Dimension(24, 14);
                    }
                    @Override
                    public Dimension getMinimumSize() {
                        return getPreferredSize();
                    }
                    @Override
                    public Dimension getMaximumSize() {
                        return getPreferredSize();
                    }
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(getBackground());
                        g2.fillRect(0, 0, getWidth(), getHeight());

                        g2.setColor(UIStyle.TEXT_COLOR);

                        int w = getWidth();
                        int h = getHeight();
                        int size = Math.max(5, Math.min(w, h) / 2);
                        int cx = w / 2;
                        int cy = h / 2;

                        int[] xPoints;
                        int[] yPoints;
                        if (up) {
                            xPoints = new int[]{cx, cx - size, cx + size};
                            yPoints = new int[]{cy - size / 2, cy + size / 2, cy + size / 2};
                        } else {
                            xPoints = new int[]{cx, cx - size, cx + size};
                            yPoints = new int[]{cy + size / 2, cy - size / 2, cy - size / 2};
                        }
                        g2.fillPolygon(xPoints, yPoints, 3);
                        g2.dispose();
                    }
                };
                btn.setBackground(UIStyle.BUTTON_BG);
                btn.setOpaque(true);
                btn.setBorderPainted(false);
                btn.setFocusPainted(false);
                btn.getModel().addChangeListener(e -> {
                    ButtonModel m = btn.getModel();
                    if (m.isPressed())
                        btn.setBackground(UIStyle.BUTTON_PRESSED);
                    else if (m.isRollover())
                        btn.setBackground(UIStyle.BUTTON_HOVER);
                    else
                        btn.setBackground(UIStyle.BUTTON_BG);
                });
                btn.addActionListener(e -> {
                    try {
                        Object val = up ? s.getNextValue() : s.getPreviousValue();
                        if (val != null) s.setValue(val);
                    } catch (IllegalArgumentException ignored) {}
                });
                return btn;
            }
        });
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

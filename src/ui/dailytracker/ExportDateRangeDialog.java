package ui.dailytracker;

import ui.StyledDialog;
import ui.UIStyle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Modal date-range picker with two month calendars ("From" / "To").
 * Dates that have recorded entries are outlined with an accent circle.
 */
public class ExportDateRangeDialog extends JDialog {

    private static final Font MONTH_FONT = new Font("Segoe UI", Font.BOLD, 13);
    private static final String[] WEEKDAYS = {"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"};

    public static LocalDate[] show(Window parent, Set<LocalDate> recordedDates,
                                   LocalDate initialFrom, LocalDate initialTo) {
        final LocalDate[] result = new LocalDate[2];
        ExportDateRangeDialog dialog =
                new ExportDateRangeDialog(parent, recordedDates, initialFrom, initialTo, result);
        dialog.setVisible(true);
        return result[0] != null && result[1] != null ? result : null;
    }

    private ExportDateRangeDialog(Window parent, Set<LocalDate> recordedDates,
                                  LocalDate initialFrom, LocalDate initialTo,
                                  LocalDate[] result) {
        super(parent, "", Dialog.ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setBackground(UIStyle.BG_COLOR);

        CalendarPanel fromCal = new CalendarPanel(YearMonth.from(initialFrom), recordedDates);
        CalendarPanel toCal = new CalendarPanel(YearMonth.from(initialTo), recordedDates);
        fromCal.setSelected(initialFrom);
        toCal.setSelected(initialTo);

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UIStyle.SIDE_BOX);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(18, 22, 18, 22)));

        JLabel title = new JLabel("Select export range", SwingConstants.CENTER);
        title.setForeground(UIStyle.TEXT_COLOR);
        title.setFont(MONTH_FONT);

        JPanel calendars = new JPanel(new GridLayout(1, 2, 25, 0));
        calendars.setOpaque(false);
        calendars.add(buildCalendarColumn("From:", fromCal));
        calendars.add(buildCalendarColumn("To:", toCal));

        JButton okBtn = new JButton("OK");
        UIStyle.styleButton(okBtn);
        okBtn.setPreferredSize(new Dimension(100, 32));
        JButton cancelBtn = new JButton("Cancel");
        UIStyle.styleButton(cancelBtn);
        cancelBtn.setPreferredSize(new Dimension(100, 32));

        okBtn.addActionListener(e -> {
            LocalDate from = fromCal.getSelected();
            LocalDate to = toCal.getSelected();
            if (from == null || to == null) {
                StyledDialog.show(this, "Select both dates.");
                return;
            }
            if (from.isAfter(to)) {
                StyledDialog.show(this, "'From' date must be before or equal to 'To' date.");
                return;
            }
            result[0] = from;
            result[1] = to;
            dispose();
        });
        cancelBtn.addActionListener(e -> dispose());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(okBtn);
        btnPanel.add(cancelBtn);

        panel.add(title, BorderLayout.NORTH);
        panel.add(calendars, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(panel);
        getRootPane().setDefaultButton(okBtn);
        pack();
        if (parent != null) setLocationRelativeTo(parent);
        else {
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation((screen.width - getWidth()) / 2, (screen.height - getHeight()) / 2);
        }
    }

    private JPanel buildCalendarColumn(String label, CalendarPanel cal) {
        JLabel lbl = new JLabel(label);
        lbl.setForeground(UIStyle.TEXT_COLOR);
        lbl.setFont(MONTH_FONT);

        JButton prevBtn = navArrow(SwingConstants.WEST);
        prevBtn.addActionListener(e -> cal.setMonth(cal.getMonth().minusMonths(1)));

        JButton nextBtn = navArrow(SwingConstants.EAST);
        nextBtn.addActionListener(e -> cal.setMonth(cal.getMonth().plusMonths(1)));

        JLabel monthLabel = new JLabel(monthText(cal.getMonth()), SwingConstants.CENTER);
        monthLabel.setForeground(UIStyle.ACCENT_COLOR);
        monthLabel.setFont(MONTH_FONT);
        cal.setOnMonthChange(m -> monthLabel.setText(monthText(m)));

        JPanel nav = new JPanel(new BorderLayout(0, 0));
        nav.setOpaque(false);
        nav.add(prevBtn, BorderLayout.WEST);
        nav.add(monthLabel, BorderLayout.CENTER);
        nav.add(nextBtn, BorderLayout.EAST);

        JPanel col = new JPanel(new BorderLayout(0, 6));
        col.setOpaque(false);
        col.add(lbl, BorderLayout.NORTH);
        col.add(nav, BorderLayout.CENTER);
        col.add(cal, BorderLayout.SOUTH);
        return col;
    }

    private static String monthText(YearMonth month) {
        return month.format(DateTimeFormatter.ofPattern("MMMM yyyy")
                .withLocale(java.util.Locale.ENGLISH));
    }

    /** Flat arrow button in the app style: themed background + hand-painted triangle. */
    private static JButton navArrow(int direction) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRect(0, 0, getWidth(), getHeight());

                boolean hover = getModel().isRollover() || getModel().isPressed();
                g2.setColor(hover ? UIStyle.ACCENT_COLOR : new Color(200, 200, 200));
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                int dx = direction == SwingConstants.WEST ? -1 : 1;
                int r = 5;
                Polygon arrow = new Polygon(
                        new int[]{cx + 3 * dx, cx - 3 * dx, cx - 3 * dx},
                        new int[]{cy, cy - r, cy + r},
                        3);
                g2.fillPolygon(arrow);
            }
        };
        UIStyle.styleButton(btn);
        btn.setFocusable(false);
        btn.setPreferredSize(new Dimension(36, 26));
        return btn;
    }

    /** One month grid: navigation-driven, dates with entries get an accent circle. */
    private static class CalendarPanel extends JPanel {
        private static final int HEADER_HEIGHT = 20;
        private static final int ROWS = 6;

        private YearMonth month;
        private LocalDate selected;
        private final Set<LocalDate> recorded;
        private Consumer<YearMonth> onMonthChange;

        CalendarPanel(YearMonth month, Set<LocalDate> recorded) {
            this.month = month;
            this.recorded = recorded;
            setOpaque(true);
            setBackground(UIStyle.SECONDARY_BG);
            setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));
            setPreferredSize(new Dimension(252, 208));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    pickDay(e.getX(), e.getY());
                }
            });
        }

        void setOnMonthChange(Consumer<YearMonth> listener) {
            this.onMonthChange = listener;
        }

        YearMonth getMonth() {
            return month;
        }

        void setMonth(YearMonth newMonth) {
            this.month = newMonth;
            if (onMonthChange != null) onMonthChange.accept(newMonth);
            repaint();
        }

        void setSelected(LocalDate date) {
            this.selected = date;
            repaint();
        }

        LocalDate getSelected() {
            return selected;
        }

        private void pickDay(int x, int y) {
            float cellW = getWidth() / 7f;
            float cellH = (getHeight() - HEADER_HEIGHT) / (float) ROWS;
            int col = Math.min(6, Math.max(0, (int) (x / cellW)));
            int row = Math.min(ROWS - 1, Math.max(0, (int) ((y - HEADER_HEIGHT) / cellH)));
            int lead = month.atDay(1).getDayOfWeek().getValue() - 1;
            int dayNum = row * 7 + col - lead + 1;
            if (dayNum >= 1 && dayNum <= month.lengthOfMonth()) {
                selected = month.atDay(dayNum);
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            float cellW = getWidth() / 7f;
            float cellH = (getHeight() - HEADER_HEIGHT) / (float) ROWS;
            Insets insets = getInsets();
            float x0 = insets.left;

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.setColor(UIStyle.ACCENT_COLOR);
            for (int i = 0; i < WEEKDAYS.length; i++) {
                String wd = WEEKDAYS[i];
                FontMetrics fm = g2.getFontMetrics();
                int tx = (int) (x0 + i * cellW + (cellW - fm.stringWidth(wd)) / 2);
                g2.drawString(wd, tx, 14);
            }

            int lead = month.atDay(1).getDayOfWeek().getValue() - 1;
            int len = month.lengthOfMonth();
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < 7; col++) {
                    int dayNum = row * 7 + col - lead + 1;
                    if (dayNum < 1 || dayNum > len) continue;
                    LocalDate day = month.atDay(dayNum);
                    float cx = x0 + col * cellW + cellW / 2;
                    float cy = HEADER_HEIGHT + row * cellH + cellH / 2;
                    boolean hasData = recorded.contains(day);
                    boolean isSel = day.equals(selected);

                    if (isSel) {
                        g2.setColor(UIStyle.ACCENT_COLOR);
                        g2.fillOval((int) Math.round(cx - 11) + insets.left, (int) Math.round(cy - 11), 22, 22);
                        g2.setColor(UIStyle.BG_COLOR);
                    } else if (hasData) {
                        g2.setColor(UIStyle.ACCENT_COLOR);
                        g2.drawOval((int) Math.round(cx - 11) + insets.left, (int) Math.round(cy - 11), 22, 22);
                        g2.setColor(new Color(200, 200, 200));
                    } else {
                        g2.setColor(new Color(140, 140, 140));
                    }
                    String label = String.valueOf(dayNum);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(label, (int) (cx - fm.stringWidth(label) / 2.0), (int) (cy + fm.getAscent() / 2.0 - 2));
                }
            }
        }
    }
}

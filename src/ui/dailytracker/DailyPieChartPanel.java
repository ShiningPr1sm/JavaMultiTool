package ui.dailytracker;

import ui.UIStyle;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.function.Function;

public class DailyPieChartPanel extends JPanel {
    private final String title;
    private final Function<Double, String> formatter;
    private final JPanel legendBox;
    private final JScrollPane legendScroll;
    private final PieCanvas canvas;
    private Map<String, Double> data;

    public DailyPieChartPanel(String title, Function<Double, String> formatter) {
        this.title = title;
        this.formatter = formatter;

        setLayout(new BorderLayout());
        setBackground(UIStyle.SECONDARY_BG);

        canvas = new PieCanvas();
        add(canvas, BorderLayout.CENTER);

        legendBox = new JPanel();
        legendBox.setLayout(new BoxLayout(legendBox, BoxLayout.Y_AXIS));
        legendBox.setBackground(UIStyle.SECONDARY_BG);
        legendBox.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));

        JScrollPane sp = new JScrollPane(legendBox);
        sp.setPreferredSize(new Dimension(230, 0));
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        UIStyle.styleScrollBar(sp);
        legendScroll = sp;

        add(sp, BorderLayout.EAST);
    }

    public void setData(Map<String, Double> data) {
        this.data = data;
        rebuildLegend();
        legendScroll.setVisible(data != null && !data.isEmpty());
        canvas.repaint();
    }

    private void rebuildLegend() {
        legendBox.removeAll();
        if (data != null && !data.isEmpty()) {
            double total = data.values().stream().mapToDouble(Double::doubleValue).sum();
            int idx = 0;
            for (Map.Entry<String, Double> entry : data.entrySet()) {
                Color color = colorFor(idx++, entry.getKey());
                double pct = total > 0 ? entry.getValue() * 100.0 / total : 0.0;

                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 3));
                row.setOpaque(false);
                row.setMaximumSize(new Dimension(230, 30));

                JLabel marker = new JLabel(" ■ ");
                marker.setForeground(color);
                marker.setFont(new Font("Arial", Font.BOLD, 15));

                JLabel lbl = new JLabel(String.format("%s (%.1f%% / %s)",
                        entry.getKey(), pct, formatter.apply(entry.getValue())));
                lbl.setForeground(new Color(220, 220, 220));
                lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));

                row.add(marker);
                row.add(lbl);
                legendBox.add(row);
            }
        }
        legendBox.revalidate();
        legendBox.repaint();
    }

    private static Color colorFor(int index, String name) {
        if (name != null) {
            float hue = ((name.hashCode() & Integer.MAX_VALUE) % 1000000) / 1000000.0f;
            return Color.getHSBColor(hue, 0.65f, 0.85f);
        }
        return Color.getHSBColor((index * 0.61803398875f) % 1.0f, 0.65f, 0.85f);
    }

    private class PieCanvas extends JComponent {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
            g2.drawString(title, 20, 30);

            if (data == null || data.isEmpty()) {
                g2.setColor(Color.GRAY);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                String msg = "No data recorded for this date";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
                return;
            }

            double total = data.values().stream().mapToDouble(Double::doubleValue).sum();
            if (total <= 0) return;

            int margin = 30;
            int titleSpace = 40;
            int size = Math.min(getWidth() - margin * 2, getHeight() - titleSpace - margin);
            if (size <= 0) return;

            int x = (getWidth() - size) / 2;
            int y = titleSpace + (getHeight() - titleSpace - size) / 2;

            double cur = 0;
            int idx = 0;
            for (Map.Entry<String, Double> entry : data.entrySet()) {
                double angle = entry.getValue() * 360.0 / total;
                g2.setColor(colorFor(idx++, entry.getKey()));
                g2.fillArc(x, y, size, size, (int) Math.round(cur), (int) Math.round(angle) + 1);
                cur += angle;
            }
        }
    }
}
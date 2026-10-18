package ui.components;

import ui.UIStyle;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.function.Function;

public class PieChartPanel extends JPanel {

    public static String formatFullTime(double totalSec) {
        int ss = (int) Math.round(totalSec);
        int h = ss / 3600;
        int m = (ss % 3600) / 60;
        int s = ss % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private final String title;
    private final String emptyMessage;
    private final Function<Double, String> formatter;
    private final JPanel legendContainer;
    private final JScrollPane legendScroll;
    private final PieCanvas chartCanvas;
    private Map<String, ? extends Number> data;

    public PieChartPanel(String title, String emptyMessage, Function<Double, String> formatter) {
        this(title, emptyMessage, formatter, 230, 10);
    }

    public PieChartPanel(String title, String emptyMessage, Function<Double, String> formatter,
                         int legendWidth, int legendTopInset) {
        this.title = title;
        this.emptyMessage = emptyMessage;
        this.formatter = formatter;

        setLayout(new BorderLayout());
        setBackground(UIStyle.SECONDARY_BG);

        chartCanvas = new PieCanvas();
        add(chartCanvas, BorderLayout.CENTER);

        legendContainer = new JPanel();
        legendContainer.setLayout(new BoxLayout(legendContainer, BoxLayout.Y_AXIS));
        legendContainer.setBackground(UIStyle.SECONDARY_BG);
        legendContainer.setBorder(BorderFactory.createEmptyBorder(legendTopInset, 10, 10, 10));

        legendScroll = new JScrollPane(legendContainer);
        legendScroll.setPreferredSize(new Dimension(legendWidth, 0));
        legendScroll.setBorder(null);
        legendScroll.setOpaque(false);
        legendScroll.getViewport().setOpaque(false);
        legendScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        legendScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        UIStyle.styleScrollBar(legendScroll);

        add(legendScroll, BorderLayout.EAST);
    }

    public void setData(Map<String, ? extends Number> data) {
        this.data = data;
        rebuildLegend();
        legendScroll.setVisible(data != null && !data.isEmpty());
        chartCanvas.repaint();
    }

    private void rebuildLegend() {
        legendContainer.removeAll();
        double total = data == null
                ? 0.0
                : data.values().stream().mapToDouble(Number::doubleValue).sum();
        if (data != null && !data.isEmpty() && total > 0) {
            int idx = 0;
            for (Map.Entry<String, ? extends Number> entry : data.entrySet()) {
                double value = entry.getValue().doubleValue();
                Color color = colorFor(idx++, entry.getKey());
                double pct = value * 100.0 / total;

                JPanel row = new JPanel();
                row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
                row.setOpaque(false);
                row.setMaximumSize(new Dimension(legendScroll.getPreferredSize().width, 24));
                row.setAlignmentX(Component.LEFT_ALIGNMENT);

                JLabel marker = new JLabel(" ■ ");
                marker.setForeground(color);
                marker.setFont(new Font("Arial", Font.BOLD, 16));
                marker.setAlignmentY(Component.CENTER_ALIGNMENT);

                JLabel label = new JLabel(String.format("%s (%.1f%% / %s)", entry.getKey(), pct, formatter.apply(value)));
                label.setForeground(new Color(220, 220, 220));
                label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                label.setAlignmentY(Component.CENTER_ALIGNMENT);

                row.add(marker);
                row.add(Box.createHorizontalStrut(6));
                row.add(label);
                row.add(Box.createHorizontalGlue());
                legendContainer.add(row);
            }
        }
        legendContainer.revalidate();
        legendContainer.repaint();
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
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(emptyMessage, (getWidth() - fm.stringWidth(emptyMessage)) / 2, getHeight() / 2);
                return;
            }

            double total = data.values().stream().mapToDouble(Number::doubleValue).sum();
            if (total <= 0) return;

            int margin = 30;
            int titleSpace = 40;
            int size = Math.min(getWidth() - margin * 2, getHeight() - titleSpace - margin);
            if (size <= 0) return;

            int x = (getWidth() - size) / 2;
            int y = titleSpace + (getHeight() - titleSpace - size) / 2;

            double cur = 0;
            int idx = 0;
            for (Map.Entry<String, ? extends Number> entry : data.entrySet()) {
                double angle = entry.getValue().doubleValue() * 360.0 / total;
                g2.setColor(colorFor(idx++, entry.getKey()));
                g2.fillArc(x, y, size, size, (int) Math.round(cur), (int) Math.round(angle) + 1);
                cur += angle;
            }
        }
    }
}
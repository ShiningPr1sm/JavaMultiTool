package ui.components;

import ui.UIStyle;
import util.ColorGenerator;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Reusable pie chart with a scrollable legend to the right. Each legend entry
 * shows the wrapped name plus percentage and formatted value; colors are
 * deterministic per name via {@link ColorGenerator}.
 */
public class PieChartPanel extends JPanel {

    private static final int WRAP_CHARS = 35;

    /** Formats seconds as {@code HH:MM:SS} (rounding to the nearest second). */
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
                row.setMaximumSize(new Dimension(legendScroll.getPreferredSize().width, Integer.MAX_VALUE));
                row.setAlignmentX(Component.LEFT_ALIGNMENT);

                JLabel marker = new JLabel(" ■ ");
                marker.setForeground(color);
                marker.setFont(new Font("Arial", Font.BOLD, 16));
                marker.setAlignmentY(Component.CENTER_ALIGNMENT);

                // stats always on their own line so "kcal" never wraps off
                String nameHtml = wrapByChars(entry.getKey(), WRAP_CHARS);
                JLabel label = new JLabel(String.format(
                        "<html><div style='text-align:left'>%s<br>%.1f%% / %s</div></html>",
                        nameHtml, pct, formatter.apply(value)));
                label.setForeground(new Color(220, 220, 220));
                label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                label.setAlignmentY(Component.CENTER_ALIGNMENT);
                label.setToolTipText(String.format("%s (%.1f%% / %s)",
                        entry.getKey(), pct, formatter.apply(value)));

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

    private static String wrapByChars(String text, int n) {
        if (n <= 0 || text.length() <= n) return text;
        StringBuilder sb = new StringBuilder();
        int pos = 0;
        while (pos < text.length()) {
            if (pos > 0) sb.append("<br>");
            sb.append(text, pos, Math.min(text.length(), pos + n));
            pos += n;
        }
        return sb.toString();
    }

    private static Color colorFor(int index, String name) {
        if (name != null) {
            // Deterministic golden-angle color per name (fixed saturation/lightness).
            return ColorGenerator.colorFor(name);
        }
        return Color.getHSBColor((index * 0.61803398875f) % 1.0f, 0.65f, 0.85f);
    }

    private static List<String> wrapTitle(FontMetrics fm, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) return lines;
        if (fm.stringWidth(text) <= maxWidth) {
            lines.add(text);
            return lines;
        }
        StringBuilder current = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && fm.stringWidth(candidate) > maxWidth) {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            } else {
                current.setLength(0);
                current.append(candidate);
            }
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
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
            FontMetrics fm = g2.getFontMetrics();
            List<String> titleLines = wrapTitle(fm, title, Math.max(60, getWidth() - 40));
            int titleY = 22;
            for (String line : titleLines) {
                g2.drawString(line, 20, titleY);
                titleY += 18;
            }
            int titleSpace = titleLines.size() * 18 + 20;

            if (data == null || data.isEmpty()) {
                g2.setColor(Color.GRAY);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                FontMetrics emptyFm = g2.getFontMetrics();
                g2.drawString(emptyMessage, (getWidth() - emptyFm.stringWidth(emptyMessage)) / 2, getHeight() / 2);
                return;
            }

            double total = data.values().stream().mapToDouble(Number::doubleValue).sum();
            if (total <= 0) return;

            int margin = 30;
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
package ui.components;

import ui.UIStyle;
import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class PieChartPanel extends JPanel {
    private Map<String, Integer> data;
    private final JPanel legendContainer;
    private final PieCanvas chartCanvas;

    private final Color[] PALETTE = {
            new Color(100, 200, 100), new Color(194, 0, 255),
            new Color(0, 200, 255),   new Color(255, 140, 0),
            new Color(255, 75, 75),   new Color(255, 215, 0),
            new Color(64, 224, 208),  new Color(250, 128, 114),
            new Color(123, 104, 238), new Color(173, 255, 47),
            new Color(255, 105, 180), new Color(0, 255, 127)
    };

    public PieChartPanel() {
        setLayout(new BorderLayout());
        setBackground(UIStyle.SECONDARY_BG);

        chartCanvas = new PieCanvas();
        add(chartCanvas, BorderLayout.CENTER);

        legendContainer = new JPanel();
        legendContainer.setLayout(new BoxLayout(legendContainer, BoxLayout.Y_AXIS));
        legendContainer.setBackground(UIStyle.SECONDARY_BG);
        legendContainer.setBorder(BorderFactory.createEmptyBorder(40, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(legendContainer);
        scrollPane.setPreferredSize(new Dimension(280, 0));
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        UIStyle.styleScrollBar(scrollPane);

        add(scrollPane, BorderLayout.EAST);
    }

    public void setData(Map<String, Integer> data) {
        this.data = data;
        updateLegend();
        chartCanvas.repaint();
    }

    private void updateLegend() {
        legendContainer.removeAll();
        if (data == null || data.isEmpty()) return;

        int totalSeconds = data.values().stream().mapToInt(Integer::intValue).sum();
        int i = 0;

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            Color color = (i < PALETTE.length) ? PALETTE[i] : Color.getHSBColor((float) i / data.size(), 0.7f, 0.9f);

            double percent = (entry.getValue() * 100.0) / totalSeconds;
            String timeFormatted = formatFullTime(entry.getValue());
            String text = String.format("%s (%.1f%% / %s)", entry.getKey(), percent, timeFormatted);

            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(280, 30));

            JLabel marker = new JLabel(" ■ ");
            marker.setForeground(color);
            marker.setFont(new Font("Arial", Font.BOLD, 16));

            JLabel label = new JLabel(text);
            label.setForeground(new Color(220, 220, 220));
            label.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            row.add(marker);
            row.add(label);
            legendContainer.add(row);

            i++;
        }
        legendContainer.revalidate();
        legendContainer.repaint();
    }

    private String formatFullTime(int totalSec) {
        int h = totalSec / 3600;
        int m = (totalSec % 3600) / 60;
        int s = totalSec % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
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
            g2.drawString("Application Usage Distribution", 20, 30);

            if (data == null || data.isEmpty()) {
                g2.setColor(Color.GRAY);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                String msg = "No data recorded for this day!";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
                return;
            }

            int totalSeconds = data.values().stream().mapToInt(Integer::intValue).sum();
            int margin = 30;
            int titleSpace = 40;

            int size = Math.min(getWidth() - margin * 2, getHeight() - titleSpace - margin);

            int x = (getWidth() - size) / 2;
            int y = titleSpace + (getHeight() - titleSpace - size) / 2;

            double curAngle = 0;
            int i = 0;
            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                double angle = (entry.getValue() * 360.0) / totalSeconds;
                Color color = (i < PALETTE.length) ? PALETTE[i] : Color.getHSBColor((float) i / data.size(), 0.7f, 0.9f);

                g2.setColor(color);
                g2.fillArc(x, y, size, size, (int) Math.round(curAngle), (int) Math.round(angle) + 1);

                curAngle += angle;
                i++;
            }
        }
    }
}
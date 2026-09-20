package ui.dailytracker;

import ui.UIStyle;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

public class DailyBarChartPanel extends JPanel {
    private String title = "";
    private double[] values;
    private String[] labels;
    private Function<Double, String> formatter = v -> String.format("%.0f", v);

    private static final int PAD_LEFT = 10;
    private static final int PAD_RIGHT = 25;
    private static final int PAD_TOP = 45;
    private static final int PAD_BOTTOM = 45;

    public DailyBarChartPanel() {
        setBackground(UIStyle.SECONDARY_BG);
    }

    public void setData(String title, double[] values, String[] labels, Function<Double, String> formatter) {
        this.title = title == null ? "" : title;
        this.values = values;
        this.labels = labels;
        if (formatter != null) this.formatter = formatter;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2.drawString(title, 15, 25);

        if (values == null || values.length == 0) {
            drawEmpty(g2);
            return;
        }

        int w = getWidth() - PAD_LEFT - PAD_RIGHT;
        int h = getHeight() - PAD_TOP - PAD_BOTTOM;
        if (w <= 0 || h <= 0) return;

        double max = 1;
        for (double v : values) if (v > max) max = v;

        g2.setColor(new Color(70, 70, 70));
        g2.drawLine(PAD_LEFT, PAD_TOP + h, PAD_LEFT + w, PAD_TOP + h);

        if (max > 0) {
            g2.setColor(Color.GRAY);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.drawString(formatter.apply(max), PAD_LEFT, PAD_TOP + 10);
        }

        double barW = (double) w / values.length;
        FontMetrics fm = g2.getFontMetrics(new Font("Segoe UI", Font.PLAIN, 10));

        for (int i = 0; i < values.length; i++) {
            double v = values[i];
            int bh = (int) (v / max * h);
            int x = PAD_LEFT + (int) (i * barW);
            int y = PAD_TOP + h - bh;

            g2.setColor(new Color(100, 200, 100, 130));
            g2.fillRect(x + 2, y, (int) barW - 4, bh);
            g2.setColor(new Color(100, 200, 100, 200));
            g2.drawRect(x + 2, y, (int) barW - 4, bh);

            if (bh > 8) {
                g2.setColor(new Color(230, 230, 230));
                String valStr = formatter.apply(v);
                int vx = x + (int) (barW / 2) - fm.stringWidth(valStr) / 2;
                g2.drawString(valStr, Math.max(0, vx), y - 5);
            }

            if (labels != null && labels.length > 0 && i < labels.length) {
                g2.setColor(new Color(180, 180, 180));
                String lab = labels[i];
                int lx = x + (int) (barW / 2) - fm.stringWidth(lab) / 2;
                g2.drawString(lab, Math.max(0, lx), PAD_TOP + h + 20);
            }
        }
    }

    private void drawEmpty(Graphics2D g2) {
        g2.setColor(Color.GRAY);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        String msg = "No data recorded for this date";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
    }
}
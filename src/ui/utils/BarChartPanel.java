package ui.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

public class BarChartPanel extends JPanel {
    private int[] values;
    private String[] labels;
    private String title = "";

    private final int PADDING_LEFT = 60;
    private final int PADDING_RIGHT = 30;

    public BarChartPanel() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                showTooltip(e);
            }
        });
    }

    public void setData(String title, int[] values, String[] labels) {
        this.title = title;
        this.values = values;
        this.labels = labels;
        repaint();
    }

    private void showTooltip(MouseEvent e) {
        if (values == null || values.length == 0) return;
        int width = getWidth() - PADDING_LEFT - PADDING_RIGHT;
        double barWidth = (double) width / values.length;
        int col = (int) ((e.getX() - PADDING_LEFT) / barWidth);
        if (col >= 0 && col < values.length) {
            String dateLabel = (labels != null && col < labels.length) ? labels[col] : "Index: " + col;
            setToolTipText("<html><b>" + dateLabel + "</b><br>Time: " + formatShortTime(values[col]) + "</html>");
        } else {
            setToolTipText(null);
        }
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
            drawEmptyMessage(g2);
            return;
        }

        int width = getWidth() - PADDING_LEFT - PADDING_RIGHT;
        int PADDING_BOTTOM = 45;
        int PADDING_TOP = 45;
        int height = getHeight() - PADDING_TOP - PADDING_BOTTOM;
        int max = 1;
        for (int v : values) if (v > max) max = v;

        // Рисуем оси
        g2.setColor(new Color(70, 70, 70));
        g2.drawLine(PADDING_LEFT, PADDING_TOP, PADDING_LEFT, PADDING_TOP + height);
        g2.drawLine(PADDING_LEFT, PADDING_TOP + height, PADDING_LEFT + width, PADDING_TOP + height);

        g2.setColor(Color.GRAY);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g2.drawString(formatShortTime(max), 5, PADDING_TOP + 10);
        g2.drawString(formatShortTime(max / 2), 5, PADDING_TOP + (height / 2));

        double barWidth = (double) width / values.length;
        FontMetrics fm = g2.getFontMetrics(new Font("Segoe UI", Font.PLAIN, 10));

        // Список точек для линии тренда
        List<Point> trendPoints = new ArrayList<>();

        for (int i = 0; i < values.length; i++) {
            int barHeight = (int) ((double) values[i] / max * height);
            int x = PADDING_LEFT + (int) (i * barWidth);
            int y = PADDING_TOP + height - barHeight;

            if (values[i] > 0) {
                // Запоминаем точку центра верхушки столбика
                trendPoints.add(new Point(x + (int)(barWidth / 2), y));

                // Рисуем столбик (сделал чуть прозрачнее - 130 вместо 180, чтобы линия выделялась)
                g2.setColor(new Color(100, 200, 100, 130));
                g2.fillRect(x + 2, y, (int) barWidth - 4, barHeight);
                g2.setColor(new Color(100, 200, 100, 200));
                g2.drawRect(x + 2, y, (int) barWidth - 4, barHeight);
            }

            // Логика подписей
            g2.setColor(new Color(180, 180, 180));
            String label = "";
            if (values.length >= 28) {
                int dayNum = i + 1;
                if (dayNum == 1 || dayNum % 5 == 0) label = String.valueOf(dayNum);
            } else if (values.length == 7) {
                if (labels != null && i < labels.length) label = labels[i];
            }

            // Рисуем подпись (центрированно)
            if (values.length != 24 && !label.isEmpty()) {
                int textX = x + (int) (barWidth / 2) - (fm.stringWidth(label) / 2);
                g2.drawString(label, textX, PADDING_TOP + height + 18);
            }
        }

        // --- НОВОЕ: РИСУЕМ ЛИНИЮ ТРЕНДА ---
        if (trendPoints.size() > 1) {
            g2.setColor(new Color(73, 180, 204)); // Красный/Коралловый цвет
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            for (int i = 0; i < trendPoints.size() - 1; i++) {
                Point p1 = trendPoints.get(i);
                Point p2 = trendPoints.get(i + 1);

                // Линия между пиками
                g2.drawLine(p1.x, p1.y, p2.x, p2.y);

                // Точка на пике
                g2.fillOval(p1.x - 3, p1.y - 3, 6, 6);
            }
            // Последняя точка
            Point lastP = trendPoints.get(trendPoints.size() - 1);
            g2.fillOval(lastP.x - 3, lastP.y - 3, 6, 6);
        }

        // Шкала 24ч
        if (values.length == 24) {
            g2.setColor(new Color(180, 180, 180));
            int[] hoursToShow = {0, 6, 12, 18, 24};
            for (int h : hoursToShow) {
                String l = h + "h";
                int lx = PADDING_LEFT + (int) (h * (width / 24.0)) - (fm.stringWidth(l) / 2);
                g2.drawString(l, lx, PADDING_TOP + height + 18);
            }
        }
    }

    private void drawEmptyMessage(Graphics2D g2) {
        g2.setColor(Color.GRAY);
        String msg = "No data recorded";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
    }

    private String formatShortTime(int sec) {
        if (sec < 60) return sec + "s";
        if (sec < 3600) return (sec / 60) + "m";
        return (sec / 3600) + "h " + ((sec % 3600) / 60) + "m";
    }
}
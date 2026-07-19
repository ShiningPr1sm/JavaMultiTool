package ui.components;

import javax.swing.*;
import java.awt.*;

public class ProgressBarFooter extends JPanel {
    private int current;
    private int total = 100;
    private String stage;
    private boolean active;
    private boolean showTextOnly;

    private static final int BAR_HEIGHT = 18;
    private static final int TEXT_WIDTH = 90;
    private static final Color BAR_FILL = new Color(180, 0, 255);

    public ProgressBarFooter() {
        setOpaque(false);
        setPreferredSize(new Dimension(0, 0));
        setMaximumSize(new Dimension(0, 0));
        setMinimumSize(new Dimension(0, 0));
    }

    public void setProgress(int current, int total, String stage) {
        this.current = current;
        this.total = total > 0 ? total : 1;
        this.stage = stage;
        this.showTextOnly = total < 0;
        repaint();
    }

    public void setActive(boolean active) {
        this.active = active;
        if (active) {
            setPreferredSize(new Dimension(230, 26));
            setMaximumSize(new Dimension(230, 26));
            setMinimumSize(new Dimension(230, 26));
        } else {
            setPreferredSize(new Dimension(0, 0));
            setMaximumSize(new Dimension(0, 0));
            setMinimumSize(new Dimension(0, 0));
            current = 0;
            total = 100;
            stage = null;
            showTextOnly = false;
        }
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!active) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        int textX = 4;
        int textW = TEXT_WIDTH;
        int barX = textX + textW;
        int barW = w - barX - 4;
        int barY = (h - BAR_HEIGHT) / 2;

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        if (stage != null && !stage.isEmpty()) {
            g2.setColor(Color.LIGHT_GRAY);
            FontMetrics fm = g2.getFontMetrics();
            String label = fm.stringWidth(stage) > textW - 6
                    ? stage.substring(0, Math.min(stage.length(), 10)) + "..."
                    : stage;
            int labelY = barY + (BAR_HEIGHT + fm.getAscent()) / 2 - 2;
            g2.drawString(label, textX, labelY);
        }

        g2.setColor(new Color(50, 50, 50));
        g2.fillRoundRect(barX, barY, barW, BAR_HEIGHT, 8, 8);

        if (!showTextOnly) {
            int pct = Math.min(current * 100 / total, 100);
            int filledW = barW * pct / 100;
            if (filledW > 0) {
                g2.setColor(BAR_FILL);
                g2.fillRoundRect(barX, barY, filledW, BAR_HEIGHT, 8, 8);
            }

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            FontMetrics fm = g2.getFontMetrics();
            String pctText = pct + "%";
            int textXc = barX + barW / 2 - fm.stringWidth(pctText) / 2;
            int textYc = barY + (BAR_HEIGHT + fm.getAscent()) / 2 - 2;
            g2.drawString(pctText, textXc, textYc);
        }

        g2.dispose();
    }
}

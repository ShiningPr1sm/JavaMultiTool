package ui;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class NotificationPanel extends JPanel {
    private final JButton bellButton;
    private int badgeCount;

    public NotificationPanel(Runnable onOpenNotifications) {
        setLayout(new OverlayLayout(this));
        setOpaque(false);
        setPreferredSize(new Dimension(40, 40));
        setMaximumSize(new Dimension(40, 40));
        setMinimumSize(new Dimension(40, 40));

        bellButton = new JButton();
        try {
            ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/icons/menu/notification-13-svgrepo-com.png")));
            Image scaled = icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
            bellButton.setIcon(new ImageIcon(scaled));
        } catch (Exception e) {
            bellButton.setText("\uD83D\uDD14");
        }
        bellButton.setPreferredSize(new Dimension(40, 40));
        bellButton.setFocusPainted(false);
        bellButton.setBorderPainted(false);
        bellButton.setBackground(UIStyle.HEADER_COLOR);
        bellButton.addActionListener(e -> onOpenNotifications.run());

        setLayout(new GridBagLayout());
        add(bellButton);
    }

    public void setBadgeCount(int count) {
        this.badgeCount = count;
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (badgeCount <= 0) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int diameter = 18;
        int x = getWidth() - diameter - 2;
        int y = 2;

        g2.setColor(new Color(220, 50, 50));
        g2.fillOval(x, y, diameter, diameter);

        g2.setColor(Color.WHITE);
        String text = badgeCount > 9 ? "9+" : String.valueOf(badgeCount);
        Font font = new Font("Segoe UI", Font.BOLD, 10);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int textX = x + (diameter - fm.stringWidth(text)) / 2;
        int textY = y + (diameter + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(text, textX, textY);

        g2.dispose();
    }

    public int getBadgeCount() {
        return badgeCount;
    }
}

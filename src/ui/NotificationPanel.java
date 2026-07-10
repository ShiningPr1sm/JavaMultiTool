package ui;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class NotificationPanel extends JPanel {
    private final JButton bellButton;
    private final JLabel badgeLabel;
    private int badgeCount;

    public NotificationPanel(Runnable onOpenNotifications) {
        setLayout(null);
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
        bellButton.setBounds(0, 0, 40, 40);
        bellButton.setFocusPainted(false);
        bellButton.setBorderPainted(false);
        bellButton.setContentAreaFilled(false);
        bellButton.addActionListener(e -> onOpenNotifications.run());
        add(bellButton);

        badgeLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                String text = getText();
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(text, x, y);
                g2.dispose();
            }
        };
        badgeLabel.setBounds(20, 2, 18, 18);
        badgeLabel.setBackground(new Color(220, 50, 50));
        badgeLabel.setForeground(Color.WHITE);
        badgeLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        badgeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        badgeLabel.setOpaque(false);
        badgeLabel.setVisible(false);
        setComponentZOrder(badgeLabel, 0);
        add(badgeLabel);
    }

    public void setBadgeCount(int count) {
        this.badgeCount = count;
        if (count > 0) {
            badgeLabel.setText(count > 9 ? "9+" : String.valueOf(count));
            badgeLabel.setVisible(true);
        } else {
            badgeLabel.setVisible(false);
        }
        repaint();
    }

    public int getBadgeCount() {
        return badgeCount;
    }
}

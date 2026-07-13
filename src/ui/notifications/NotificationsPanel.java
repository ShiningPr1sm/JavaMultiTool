package ui.notifications;

import db.NotificationRecord;
import db.BDaysRepository;
import db.DatabaseProvider;
import service.NotificationService;
import ui.HeaderPanel;
import ui.UIStyle;
import util.AppLogger;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class NotificationsPanel extends JPanel {
    private final NotificationService notificationService;
    private final HeaderPanel headerPanel;
    private JPanel listPanel;

    public NotificationsPanel(NotificationService notificationService, HeaderPanel headerPanel) {
        this.notificationService = notificationService;
        this.headerPanel = headerPanel;
        setLayout(new BorderLayout());
        setBackground(UIStyle.BG_COLOR);

        JLabel title = new JLabel("Notifications", SwingConstants.CENTER);
        title.setForeground(UIStyle.TEXT_COLOR);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setBorder(BorderFactory.createEmptyBorder(30, 0, 10, 0));
        add(title, BorderLayout.NORTH);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(UIStyle.BG_COLOR);
        listPanel.setBorder(BorderFactory.createEmptyBorder(10, 40, 10, 40));

        refreshList();

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.setBackground(UIStyle.BG_COLOR);
        scroll.getViewport().setBackground(UIStyle.BG_COLOR);
        add(scroll, BorderLayout.CENTER);
    }

    private void refreshList() {
        listPanel.removeAll();
        List<NotificationRecord> notifications = notificationService.getActiveNotifications();

        if (notifications.isEmpty()) {
            JLabel empty = new JLabel("No new notifications", SwingConstants.CENTER);
            empty.setForeground(Color.GRAY);
            empty.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            listPanel.add(empty);
        } else {
            for (NotificationRecord rec : notifications) {
                listPanel.add(createNotificationCard(rec));
                listPanel.add(Box.createRigidArea(new Dimension(0, 8)));
            }
        }

        int active = notificationService.countActive();
        headerPanel.setNotificationBadge(active);

        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel createNotificationCard(NotificationRecord rec) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(UIStyle.SECONDARY_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));
        card.setMaximumSize(new Dimension(Short.MAX_VALUE, 80));

        String name = resolveName(rec);
        String text = switch (rec.getType()) {
            case "bday_reminder" ->
                    "<html><b>" + name + "</b> — birthday in <b>" + rec.getDaysBefore() + "</b> day" + (rec.getDaysBefore() > 1 ? "s" : "") + "</html>";
            default ->
                    "<html>Notification #" + rec.getId() + "</html>";
        };

        JLabel textLabel = new JLabel(text);
        textLabel.setForeground(UIStyle.TEXT_COLOR);
        textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JButton dismissBtn = new JButton("Dismiss");
        UIStyle.styleButton(dismissBtn);
        dismissBtn.addActionListener(e -> {
            notificationService.dismissNotification(rec.getId());
            refreshList();
        });

        card.add(textLabel, BorderLayout.CENTER);
        card.add(dismissBtn, BorderLayout.EAST);
        return card;
    }

    private String resolveName(NotificationRecord rec) {
        if (!"bday_reminder".equals(rec.getType())) return "Unknown";
        try {
            BDaysRepository repo = DatabaseProvider.getBDaysRepository();
            var all = repo.getAllBirthdays();
            return all.stream()
                    .filter(b -> b.getId() == rec.getReferenceId())
                    .map(b -> b.getName())
                    .findFirst()
                    .orElse("Unknown");
        } catch (Exception e) {
            AppLogger.error("NotificationsPanel: failed to resolve name: " + e.getMessage());
            return "Unknown";
        }
    }
}

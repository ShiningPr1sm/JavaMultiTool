package ui.achievements;

import db.AchievementDB;
import ui.UIStyle;
import ui.utils.AppLogger;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AchievementsPanel extends JPanel {
    public AchievementsPanel(String login) {
        setLayout(new BorderLayout());
        setBackground(UIStyle.BG_COLOR);

        JPanel grid = new JPanel(new GridLayout(0, 2, 20, 20));
        grid.setBackground(UIStyle.BG_COLOR);
        grid.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        String sql = """
            SELECT a.code,
                   a.title,
                   a.description,
                   al.xp_reward AS xp_reward,
                   ua.level,
                   ua.progress,
                   al.required_progress
              FROM achievements a
              JOIN user_achievements ua
                ON a.code = ua.achievement_code
              LEFT JOIN achievement_levels al
                ON a.code = al.achievement_code
               AND ua.level = al.level
             WHERE ua.user_login = ?
        """;

        try (Connection conn = AchievementDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, login);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("code");
                    String title = rs.getString("title");
                    String description = rs.getString("description");
                    int xpReward = rs.getInt("xp_reward");
                    int level = rs.getInt("level");
                    Integer reqObj = (Integer) rs.getObject("required_progress");

                    int required;
                    int progressVal;
                    if (reqObj == null) {
                        required    = 1;
                        progressVal = 1;
                    } else {
                        required    = reqObj;
                        progressVal = rs.getInt("progress");
                    }

                    String iconPath = "/icons/achievements/" + code + ".jpg";
                    String displayTitle = title;
                    if (AchievementDB.getMaxAchievementLevel(code) > 1) {
                        displayTitle += " (Level " + level + ")";
                    }

                    grid.add(createAchievementCard(
                            displayTitle,
                            description,
                            progressVal,
                            required,
                            iconPath,
                            xpReward
                    ));
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Error:" + e.getMessage());
        }

        JPanel container = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        container.setBackground(UIStyle.BG_COLOR);
        container.add(grid);

        JScrollPane scrollPane = new JScrollPane(container);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));

        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createAchievementCard(String title, String description, int progress, int max, String iconPath, int xpReward) {
        boolean completed = progress >= max;
        Color bg = completed ? UIStyle.COMPLETED_ACH : UIStyle.BUTTON_BG;

        JPanel card = new JPanel(new BorderLayout(15, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 1, 1);
                g2.dispose();
            }
        };

        card.setPreferredSize(new Dimension(380, 110));
        card.setMaximumSize(new Dimension(380, 110));
        card.setOpaque(false);
        card.setBackground(bg);

        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        JLabel iconLabel = new JLabel();
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setVerticalAlignment(SwingConstants.CENTER);

        try {
            java.net.URL url = getClass().getResource(iconPath);
            if (url == null)
                url = getClass().getResource("/icons/achievements/no_achievement.png");
            if (url != null) {
                Image scaled = new ImageIcon(url).getImage().getScaledInstance(70, 70, Image.SCALE_SMOOTH);
                iconLabel.setIcon(new ImageIcon(scaled));
            }
        } catch (Exception e) {
            AppLogger.error("Error loading icon: " + e.getMessage() +"\nIcon path:" + iconPath);
        }

        JPanel iconBox = new JPanel(new BorderLayout());
        iconBox.setPreferredSize(new Dimension(80, 80));
        iconBox.setMaximumSize(new Dimension(80, 80));
        iconBox.setOpaque(true);
        iconBox.setBackground(new Color(50, 50, 50));
        iconBox.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR, 1));
        iconBox.add(iconLabel, BorderLayout.CENTER);

        JPanel iconWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        iconWrapper.setOpaque(false);
        iconWrapper.add(iconBox);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(bg);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));

        JLabel xpLabel = new JLabel();
        xpLabel.setForeground(UIStyle.XP_LABEL_COLOR);
        xpLabel.setFont(xpLabel.getFont().deriveFont(12f));

        if (completed) {
            xpLabel.setText("COMPLETED");
            xpLabel.setForeground(UIStyle.XP_LABEL_COLOR.darker());
        } else {
            xpLabel.setText("+" + xpReward + " XP");
        }

        JLabel descLabel = new JLabel(
                "<html><body style='width:200px'>" + description + "</body></html>");
        descLabel.setForeground(Color.LIGHT_GRAY);
        descLabel.setFont(descLabel.getFont().deriveFont(11f));

        JProgressBar progressBar = new JProgressBar(0, max) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(d.width, 20);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();
                g2d.setColor(getBackground());
                g2d.fillRect(0, 0, width, height);
                double percent = (double) getValue() / getMaximum();
                int progressWidth = (int) (width * percent);
                g2d.setColor(getForeground());
                g2d.fillRect(0, 0, progressWidth, height);
                if (isStringPainted()) {
                    g2d.setColor(UIStyle.TEXT_COLOR);
                    Font font = getFont();
                    g2d.setFont(font);

                    int percentDisplay = (int) Math.round((double) getValue() / getMaximum() * 100);
                    String text = percentDisplay + "%";

                    FontMetrics fm = g2d.getFontMetrics();
                    int textX = (width - fm.stringWidth(text)) / 2;
                    int textY = (height - fm.getHeight()) / 2 + fm.getAscent();
                    g2d.drawString(text, textX, textY);
                }
                g2d.dispose();
            }
        };

        progressBar.setValue(progress);
        progressBar.setStringPainted(true);
        progressBar.setForeground(UIStyle.PROGRESS_BAR);
        progressBar.setBackground(UIStyle.BG_PROGRESS_BAR);
        progressBar.setOpaque(false);
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        info.add(titleLabel);
        info.add(xpLabel);
        info.add(Box.createVerticalStrut(5));
        info.add(descLabel);
        info.add(Box.createVerticalStrut(10));
        info.add(progressBar);

        card.add(iconWrapper, BorderLayout.WEST);
        card.add(info, BorderLayout.CENTER);

        return card;
    }
}
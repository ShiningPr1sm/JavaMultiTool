package ui.components;

import db.DatabaseProvider;
import service.WorkflowService;
import ui.UIStyle;

import javax.swing.*;
import java.awt.*;

public class TaskCard extends JPanel {
    private final int taskId;
    private final JLabel timeLabel;
    private final db.WorkflowRepository workflowRepo = DatabaseProvider.getWorkflowRepository();
    private final WorkflowService workflowService;

    public TaskCard(int id, String name, String desc, Runnable onToggle, Runnable onDelete, Runnable onRefreshList, WorkflowService workflowService) {
        this.taskId = id;
        this.workflowService = workflowService;
        setLayout(new BorderLayout(10, 5));
        setBackground(UIStyle.SECONDARY_BG);

        int currentActiveId = workflowService.getActiveTaskId();
        Color accent = (currentActiveId == id) ? Color.RED : new Color(194, 0, 255);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent, 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        setMaximumSize(new Dimension(Short.MAX_VALUE, 110));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        JLabel nameLbl = new JLabel(name.toUpperCase());
        nameLbl.setForeground(new Color(194, 0, 255));
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JLabel descLbl = new JLabel("<html><body style='width: 150px'>" + (desc.isEmpty() ? "No description" : desc) + "</body></html>");
        descLbl.setForeground(Color.LIGHT_GRAY);
        descLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        textPanel.add(nameLbl);
        textPanel.add(Box.createVerticalStrut(3));
        textPanel.add(descLbl);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        timeLabel = new JLabel("Time: " + formatSeconds(workflowRepo.getSecondsToday(id, 1)));
        timeLabel.setForeground(Color.WHITE);
        timeLabel.setFont(new Font("Monospaced", Font.BOLD, 12));

        JButton startBtn = new JButton(currentActiveId == id ? "STOP" : "START");
        UIStyle.styleButton(startBtn);
        if (currentActiveId == id)
            startBtn.setForeground(Color.RED);
        startBtn.addActionListener(e -> onToggle.run());

        JButton delBtn = new JButton("DEL");
        UIStyle.styleButton(delBtn);
        delBtn.setForeground(Color.GRAY);
        delBtn.addActionListener(e -> {
            if (currentActiveId == id) return;
            onDelete.run();
            onRefreshList.run();
        });

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        btns.setOpaque(false);
        btns.add(startBtn);
        btns.add(delBtn);
        bottomPanel.add(timeLabel, BorderLayout.WEST);
        bottomPanel.add(btns, BorderLayout.EAST);
        add(textPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void updateTime() {
        int freshSeconds = workflowRepo.getSecondsToday(this.taskId, 1);
        timeLabel.setText("Time: " + formatSeconds(freshSeconds));
        int activeId = workflowService.getActiveTaskId();
        Color accent = (activeId == taskId) ? Color.RED : new Color(194, 0, 255);
        this.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent, 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
    }

    public static String formatSeconds(int totalSeconds) {
        return String.format("%02d:%02d:%02d", totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60);
    }
}

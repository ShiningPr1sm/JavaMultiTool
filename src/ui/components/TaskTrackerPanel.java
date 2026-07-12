package ui.components;

import db.WorkflowRepository;
import service.WorkflowService;
import ui.UIStyle;
import util.AppLogger;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class TaskTrackerPanel extends JPanel {
    private final JPanel taskContainer = new JPanel();
    private final WorkflowRepository workflowRepo;
    private final WorkflowService workflowService;

    public TaskTrackerPanel(WorkflowRepository workflowRepo, WorkflowService workflowService) {
        this.workflowRepo = workflowRepo;
        this.workflowService = workflowService;
        setLayout(new BorderLayout(0, 10));
        setBackground(UIStyle.BG_COLOR);

        JLabel title = new JLabel("Task Worklog | Manual");
        title.setForeground(UIStyle.ACCENT_COLOR);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));

        taskContainer.setLayout(new BoxLayout(taskContainer, BoxLayout.Y_AXIS));
        taskContainer.setBackground(UIStyle.BG_COLOR);

        JScrollPane sp = new JScrollPane(taskContainer);
        sp.setBorder(null);
        UIStyle.styleScrollBar(sp);

        JPanel addForm = new JPanel(new GridBagLayout());
        addForm.setBackground(UIStyle.HEADER_COLOR);
        addForm.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 2, 2, 2);

        JTextField nameInp = new JTextField();
        nameInp.setBackground(UIStyle.SIDE_BOX);
        nameInp.setForeground(Color.WHITE);
        nameInp.setCaretColor(Color.WHITE);
        nameInp.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));

        JTextField descInp = new JTextField();
        descInp.setBackground(UIStyle.SIDE_BOX);
        descInp.setForeground(Color.WHITE);
        descInp.setCaretColor(Color.WHITE);
        descInp.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));

        JButton addBtn = new JButton("ADD NEW TASK");
        UIStyle.styleButton(addBtn);
        addBtn.addActionListener(e -> {
            if (!nameInp.getText().trim().isEmpty()) {
                workflowRepo.addTask(nameInp.getText().trim(), descInp.getText().trim());
                nameInp.setText("");
                descInp.setText("");
                loadTasksFromDB();
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        addForm.add(new JLabel("<html><b style='color:gray'>Task Name:</b></html>"), gbc);
        gbc.gridy = 1;
        addForm.add(nameInp, gbc);
        gbc.gridy = 2;
        addForm.add(new JLabel("<html><b style='color:gray'>Description:</b></html>"), gbc);
        gbc.gridy = 3;
        addForm.add(descInp, gbc);
        gbc.gridy = 4;
        gbc.insets = new Insets(10, 2, 2, 2);
        addForm.add(addBtn, gbc);

        add(title, BorderLayout.NORTH);
        add(sp, BorderLayout.CENTER);
        add(addForm, BorderLayout.SOUTH);

        loadTasksFromDB();
    }

    public void refreshTimers() {
        for (Component c : taskContainer.getComponents()) {
            if (c instanceof TaskCard card) {
                card.updateTime();
            }
        }
    }

    public void loadTasksFromDB() {
        taskContainer.removeAll();
        List<Object[]> tasks = workflowRepo.getTasksFull();
        for (Object[] t : tasks) {
            int id = (int) t[0];
            String name = (String) t[1];
            String desc = (String) t[2];
            taskContainer.add(new TaskCard(id, name, desc,
                    () -> toggleTask(id, name),
                    () -> workflowRepo.deleteTask(id),
                    this::loadTasksFromDB,
                    workflowService));
            taskContainer.add(Box.createVerticalStrut(10));
        }
        taskContainer.revalidate();
        taskContainer.repaint();
    }

    private void toggleTask(int id, String name) {
        if (workflowService.getActiveTaskId() == id) {
            workflowService.setActiveTaskId(-1);
            AppLogger.info("Task stopped: " + name);
        } else {
            workflowService.setActiveTaskId(id);
            AppLogger.info("Task started: " + name);
        }
        loadTasksFromDB();
    }
}

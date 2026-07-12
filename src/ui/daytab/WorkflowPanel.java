package ui.daytab;

import db.WorkflowRepository;
import service.RunningProcessService;
import service.WorkflowService;
import ui.UIStyle;
import ui.components.AppTrackerPanel;
import ui.components.AppsEditPanel;
import ui.components.OverviewChartsPanel;
import ui.components.PomodoroPanel;
import ui.components.TaskTrackerPanel;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;

public class WorkflowPanel extends JPanel {
    private final WorkflowRepository workflowRepo = db.DatabaseProvider.getWorkflowRepository();
    private AppTrackerPanel appTrackerPanel;
    private TaskTrackerPanel taskTrackerPanel;
    private OverviewChartsPanel overviewCharts;
    private final WorkflowService workflowService;
    private final RunningProcessService runningProcessService;
    private Timer uiRefreshTimer;

    public WorkflowPanel(WorkflowService workflowService, RunningProcessService runningProcessService) {
        this.workflowService = workflowService;
        this.runningProcessService = runningProcessService;
        setLayout(new BorderLayout());
        setBackground(UIStyle.BG_COLOR);

        PomodoroPanel pomodoroPanel = new PomodoroPanel();

        taskTrackerPanel = new TaskTrackerPanel(workflowRepo, workflowService);
        appTrackerPanel = new AppTrackerPanel(workflowRepo, () -> appTrackerPanel.refresh(), this, runningProcessService);

        JTabbedPane tabs = new JTabbedPane();
        UIStyle.styleTabbedPane(tabs);

        tabs.addTab(" Worklog ", createWorklogUI(taskTrackerPanel, pomodoroPanel));
        tabs.addTab(" AppTracker ", createAppTrackerUI(appTrackerPanel));
        tabs.addTab(" Overview ", createOverviewUI());

        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 2 && overviewCharts != null) {
                overviewCharts.refresh();
            }
        });

        add(tabs, BorderLayout.CENTER);

        uiRefreshTimer = new Timer(1000, e -> {
            if (taskTrackerPanel != null) taskTrackerPanel.refreshTimers();
            if (appTrackerPanel != null) appTrackerPanel.refresh();
        });
        uiRefreshTimer.start();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (uiRefreshTimer != null && !uiRefreshTimer.isRunning()) {
            uiRefreshTimer.start();
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (uiRefreshTimer != null) {
            uiRefreshTimer.stop();
        }
    }

    private JPanel createWorklogUI(TaskTrackerPanel tasks, PomodoroPanel pomodoro) {
        JPanel main = new JPanel(new GridLayout(1, 2, 20, 0));
        main.setBackground(UIStyle.BG_COLOR);
        main.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        main.add(tasks);
        main.add(pomodoro);
        return main;
    }

    private JPanel createAppTrackerUI(AppTrackerPanel appTracker) {
        AppsEditPanel editPanel = new AppsEditPanel(workflowRepo, () -> appTrackerPanel.refresh());
        JPanel main = new JPanel(new GridLayout(1, 2, 20, 0));
        main.setBackground(UIStyle.BG_COLOR);
        main.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        main.add(appTracker);
        main.add(editPanel);
        return main;
    }

    private JPanel createOverviewUI() {
        overviewCharts = new OverviewChartsPanel(workflowRepo);
        return overviewCharts;
    }
}

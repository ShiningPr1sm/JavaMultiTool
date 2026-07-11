package ui.daytab;

import db.WorkflowRepository;
import service.RunningProcessService;
import service.WorkflowService;
import ui.UIStyle;
import ui.components.AppTrackerPanel;
import ui.components.AppsEditPanel;
import ui.components.OverviewChartsPanel;
import ui.components.TaskTrackerPanel;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;

public class WorkflowPanel extends JPanel {
    private AppTrackerPanel appTrackerPanel;
    private TaskTrackerPanel taskTrackerPanel;
    private OverviewChartsPanel overviewCharts;
    private final WorkflowRepository workflowRepo = db.DatabaseProvider.getWorkflowRepository();
    private final WorkflowService workflowService;
    private final RunningProcessService runningProcessService;
    private final Timer uiRefreshTimer;

    public WorkflowPanel(WorkflowService workflowService, RunningProcessService runningProcessService) {
        this.workflowService = workflowService;
        this.runningProcessService = runningProcessService;
        setLayout(new BorderLayout());
        setBackground(UIStyle.BG_COLOR);

        JTabbedPane tabs = new JTabbedPane();
        UIStyle.styleTabbedPane(tabs);

        tabs.addTab(" Tracker ", createTrackerUI());
        tabs.addTab(" Overview ", createOverviewUI());
        tabs.addTab(" Edit ", createEditUI());

        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 1 && overviewCharts != null) {
                overviewCharts.refresh();
            }
        });

        add(tabs, BorderLayout.CENTER);

        uiRefreshTimer = new Timer(1000, e -> {
            if (appTrackerPanel != null) appTrackerPanel.refresh();
            if (taskTrackerPanel != null) taskTrackerPanel.refreshTimers();
        });
        uiRefreshTimer.start();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (uiRefreshTimer != null) {
            uiRefreshTimer.stop();
        }
    }

    private JPanel createTrackerUI() {
        taskTrackerPanel = new TaskTrackerPanel(workflowRepo, workflowService);
        appTrackerPanel = new AppTrackerPanel(workflowRepo, taskTrackerPanel::loadTasksFromDB, this, runningProcessService);
        JPanel main = new JPanel(new GridLayout(1, 2, 20, 0));
        main.setBackground(UIStyle.BG_COLOR);
        main.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        main.add(taskTrackerPanel);
        main.add(appTrackerPanel);

        return main;
    }

    private JPanel createOverviewUI() {
        overviewCharts = new OverviewChartsPanel(workflowRepo);
        return overviewCharts;
    }

    private JPanel createEditUI() {
        return new AppsEditPanel(workflowRepo, () -> {
            if (appTrackerPanel != null) appTrackerPanel.refresh();
        });
    }

}

package ui.components;

import db.WorkflowRepository;
import service.RunningProcessService;
import ui.UIStyle;
import util.AppLogger;
import util.I18n;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AppTrackerPanel extends JPanel {

    private final DefaultListModel<String> appListModel = new DefaultListModel<>();
    private final JList<String> appList = new JList<>(appListModel);
    private final WorkflowRepository workflowRepo;
    private final Runnable onDataChanged;
    private final RunningProcessService runningProcessService;

    public AppTrackerPanel(WorkflowRepository workflowRepo, Runnable onDataChanged, Component parent, RunningProcessService runningProcessService) {
        this.workflowRepo = workflowRepo;
        this.onDataChanged = onDataChanged;
        this.runningProcessService = runningProcessService;

        setLayout(new BorderLayout(0, 10));
        setBackground(UIStyle.BG_COLOR);

        JLabel title = new JLabel(I18n.t("apps.trackerTitle",
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))));
        title.setForeground(UIStyle.ACCENT_COLOR);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));

        appList.setBackground(UIStyle.SECONDARY_BG);
        appList.setForeground(Color.WHITE);
        appList.setFixedCellHeight(30);

        JScrollPane sp = new JScrollPane(appList);
        sp.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));
        UIStyle.styleScrollBar(sp);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        btnPanel.setBackground(UIStyle.BG_COLOR);

        JButton addManualBtn = new JButton(I18n.t("apps.browseExe"));
        UIStyle.styleButton(addManualBtn);
        addManualBtn.addActionListener(e -> {
            FileDialog fd = new FileDialog(
                    (Frame) SwingUtilities.getWindowAncestor(parent),
                    I18n.t("apps.selectExe"), FileDialog.LOAD);
            fd.setFile("*.exe");
            fd.setVisible(true);
            if (fd.getFile() != null) {
                String name = fd.getFile();
                workflowRepo.addTrackedApp(runningProcessService.prettifyExeName(name), name);
                AppLogger.info("Tracked app added manually: " + name);
                onDataChanged.run();
            }
        });

        JButton addRunningBtn = new JButton(I18n.t("apps.addRunning"));
        UIStyle.styleButton(addRunningBtn);
        addRunningBtn.addActionListener(e -> RunningAppsSelector.show(this, workflowRepo, onDataChanged, runningProcessService));

        btnPanel.add(addManualBtn);
        btnPanel.add(addRunningBtn);

        add(title, BorderLayout.NORTH);
        add(sp, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    public void refresh() {
        int lastSelected = appList.getSelectedIndex();
        appListModel.clear();
        List<Object[]> apps = workflowRepo.getTrackedAppsFull();
        for (Object[] app : apps) {
            int id = (int) app[0];
            String prettyName = (String) app[1];
            int sec = workflowRepo.getSecondsToday(id, 0);
            appListModel.addElement(prettyName + "  [" + TaskCard.formatSeconds(sec) + "]");
        }
        if (lastSelected != -1 && lastSelected < appListModel.size()) {
            appList.setSelectedIndex(lastSelected);
        }
    }
}

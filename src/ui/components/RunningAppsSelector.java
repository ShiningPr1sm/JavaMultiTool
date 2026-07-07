package ui.components;

import db.WorkflowRepository;
import service.RunningProcessService;
import ui.UIStyle;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class RunningAppsSelector {

    public static void show(Component parent, WorkflowRepository workflowRepo, Runnable onAppAdded, RunningProcessService runningProcessService) {
        JDialog dialog = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(parent),
                "Select Running App", true);
        dialog.getContentPane().setBackground(UIStyle.BG_COLOR);
        dialog.setLayout(new BorderLayout());

        DefaultListModel<String> listModel = new DefaultListModel<>();
        Map<String, String> apps = runningProcessService.getRunningExes();
        apps.keySet().forEach(listModel::addElement);

        JList<String> list = new JList<>(listModel);
        list.setBackground(UIStyle.SECONDARY_BG);
        list.setForeground(Color.WHITE);
        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));
        UIStyle.styleScrollBar(sp);

        JButton addBtn = new JButton("Add to Tracking");
        UIStyle.styleButton(addBtn);
        addBtn.addActionListener(e -> {
            String sel = list.getSelectedValue();
            if (sel != null) {
                workflowRepo.addTrackedApp(runningProcessService.prettifyExeName(sel), sel);
                onAppAdded.run();
                dialog.dispose();
            }
        });

        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(UIStyle.BG_COLOR);
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.add(new JLabel("<html><b style='color:gray'>Select an application:</b></html>"), BorderLayout.NORTH);
        p.add(sp, BorderLayout.CENTER);
        p.add(addBtn, BorderLayout.SOUTH);

        dialog.add(p);
        dialog.setSize(400, 500);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
}

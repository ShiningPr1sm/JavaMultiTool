package ui.components;

import db.WorkflowRepository;
import ui.UIStyle;
import util.AppLogger;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;

public class AppsEditPanel extends JPanel {

    private final WorkflowRepository workflowRepo;
    private final Runnable onDataChanged;
    private final DefaultTableModel model;

    public AppsEditPanel(WorkflowRepository workflowRepo, Runnable onDataChanged) {
        this.workflowRepo = workflowRepo;
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(0, 10));
        setBackground(UIStyle.BG_COLOR);

        JLabel title = new JLabel("Edit Apps");
        title.setForeground(UIStyle.ACCENT_COLOR);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));

        String[] columns = {"EXE Name", "Display Name", "ID"};
        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 1; }
        };

        JTable editTable = new JTable(model);
        editTable.setBackground(UIStyle.SECONDARY_BG);
        editTable.setForeground(Color.WHITE);
        editTable.setGridColor(UIStyle.BORDER_COLOR);
        editTable.setRowHeight(30);
        editTable.setToolTipText("Double-click on 'Display Name' to rename an application.");

        JTableHeader header = editTable.getTableHeader();
        header.setBackground(UIStyle.BG_COLOR);
        header.setForeground(Color.GRAY);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
                lbl.setBackground(UIStyle.BG_COLOR);
                lbl.setForeground(Color.GRAY);
                lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, UIStyle.BORDER_COLOR));
                return lbl;
            }
        });

        editTable.removeColumn(editTable.getColumnModel().getColumn(2));

        model.addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                workflowRepo.updateAppName((int) model.getValueAt(row, 2), (String) model.getValueAt(row, 1));
                onDataChanged.run();
            }
        });

        JButton deleteBtn = new JButton("Delete Selected");
        UIStyle.styleButton(deleteBtn);
        deleteBtn.setForeground(new Color(255, 100, 100));
        deleteBtn.addActionListener(e -> {
            int row = editTable.getSelectedRow();
            if (row != -1) {
                workflowRepo.deleteTrackedAppFromDB((int) model.getValueAt(row, 2));
                model.removeRow(row);
                onDataChanged.run();
            }
        });

        JButton refreshBtn = new JButton("Refresh List");
        UIStyle.styleButton(refreshBtn);
        refreshBtn.addActionListener(e -> refreshTable());

        JScrollPane sp = new JScrollPane(editTable);
        UIStyle.styleScrollBar(sp);
        sp.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));
        JPanel corner = new JPanel();
        corner.setBackground(UIStyle.BG_COLOR);
        sp.setCorner(JScrollPane.UPPER_RIGHT_CORNER, corner);
        JPanel corner2 = new JPanel();
        corner2.setBackground(UIStyle.BG_COLOR);
        sp.setCorner(JScrollPane.LOWER_RIGHT_CORNER, corner2);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        btnPanel.setBackground(UIStyle.BG_COLOR);
        btnPanel.add(deleteBtn);
        btnPanel.add(refreshBtn);

        add(title, BorderLayout.NORTH);
        add(sp, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        refreshTable();
    }

    private void refreshTable() {
        model.setRowCount(0);
        List<Object[]> apps = workflowRepo.getTrackedAppsFull();
        for (Object[] a : apps)
            model.addRow(new Object[]{a[2], a[1], a[0]});
    }
}

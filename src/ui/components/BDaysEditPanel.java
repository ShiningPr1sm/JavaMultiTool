package ui.components;

import db.BDaysRepository;
import service.AchievementService;
import service.BDaysService;
import ui.UIStyle;
import util.AppLogger;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;

public class BDaysEditPanel extends JPanel {
    private final DefaultTableModel model;
    private final JTable table;
    private final BDaysRepository repo;
    private final BDaysService bdaysService;
    private final AchievementService achievementService;
    private final String login;

    public BDaysEditPanel(BDaysRepository repo, BDaysService bdaysService, AchievementService achievementService, String login) {
        this.repo = repo;
        this.bdaysService = bdaysService;
        this.achievementService = achievementService;
        this.login = login;
        setLayout(new BorderLayout());
        setBackground(UIStyle.BG_COLOR);

        model = new DefaultTableModel(new String[]{"ID", "Name", "Birthday"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 0;
            }
        };
        model.addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int id = (int) model.getValueAt(row, 0);
                String name = model.getValueAt(row, 1).toString();
                String dateStr = model.getValueAt(row, 2).toString();
                try {
                    repo.updateBirthday(id, name, bdaysService.uiToDb(dateStr));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Invalid format. Use DD.MM.YYYY or DD.MM.xxxx");
                    refreshTable();
                }
            }
        });

        table = new JTable(model);
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(tbl, row + 1, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                return label;
            }
        });
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(0).setMaxWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(250);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setFillsViewportHeight(true);
        table.setBackground(UIStyle.BG_COLOR);
        table.setForeground(Color.WHITE);
        table.setGridColor(UIStyle.BORDER_COLOR);
        table.setFont(table.getFont().deriveFont(15f));

        JTableHeader header = table.getTableHeader();
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val,
                                                           boolean isSel, boolean hasFocus,
                                                           int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, val, isSel, hasFocus, row, col);
                lbl.setBackground(UIStyle.BUTTON_BG);
                lbl.setForeground(Color.WHITE);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                return lbl;
            }
        });
        header.setOpaque(true);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));
        scroll.getViewport().setBackground(UIStyle.BG_COLOR);
        add(scroll, BorderLayout.CENTER);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        form.setBackground(UIStyle.HEADER_COLOR);
        JTextField nameField = new JTextField(10);
        JTextField dateField = new JTextField(8);
        dateField.setToolTipText("Format: dd.MM.yyyy or dd.MM.xxxx");
        dateField.setColumns(8);
        JButton addBtn = new JButton("Add");
        JButton removeBtn = new JButton("Remove");

        UIStyle.styleButton(addBtn);
        UIStyle.styleButton(removeBtn);

        addBtn.setForeground(new Color(150, 255, 150));
        removeBtn.setForeground(new Color(255, 150, 150));

        addBtn.addActionListener(e -> {
            try {
                String inputDate = dateField.getText().trim();
                String name = nameField.getText().trim();

                if (name.isEmpty() || inputDate.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please fill all fields");
                    return;
                }
                repo.addBirthday(name, bdaysService.uiToDb(inputDate));

                achievementService.complete(login, "real_friend");
                refreshTable();
                dateField.setText("");
                nameField.setText("");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid format. Use dd.MM.yyyy or dd.MM.xxxx", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        removeBtn.addActionListener(e -> {
            int sel = table.getSelectedRow();
            if (sel >= 0) {
                int id = (int) model.getValueAt(sel, 0);
                repo.removeBirthday(id);
                refreshTable();
            }
        });
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setForeground(Color.WHITE);
        form.add(nameLabel);
        form.add(nameField);
        JLabel dateLabel = new JLabel("Date (dd.MM.yyyy):");
        dateLabel.setForeground(Color.WHITE);
        form.add(dateLabel);
        form.add(dateField);
        form.add(addBtn);
        form.add(removeBtn);
        add(form, BorderLayout.SOUTH);
    }

    public void refreshTable() {
        model.setRowCount(0);
        try {
            for (var rec : repo.getAllBirthdays()) {
                model.addRow(new Object[]{
                        rec.getId(),
                        rec.getName(),
                        bdaysService.dbToUi(rec.getBdayDate())
                });
            }
        } catch (Exception e) {
            AppLogger.error("Error: " + e.getMessage());
        }
    }
}

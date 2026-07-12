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
    private Runnable onDataChanged;

    public BDaysEditPanel(BDaysRepository repo, BDaysService bdaysService, AchievementService achievementService, String login) {
        this(repo, bdaysService, achievementService, login, null);
    }

    public BDaysEditPanel(BDaysRepository repo, BDaysService bdaysService, AchievementService achievementService, String login, Runnable onDataChanged) {
        this.repo = repo;
        this.bdaysService = bdaysService;
        this.achievementService = achievementService;
        this.login = login;
        this.onDataChanged = onDataChanged;
        setLayout(new BorderLayout(0, 10));
        setBackground(UIStyle.BG_COLOR);

        JLabel title = new JLabel(" Edit Birthdays");
        title.setForeground(UIStyle.ACCENT_COLOR);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));

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
                    notifyDataChanged();
                } catch (Exception ex) {
                    AppLogger.error("BDaysEditPanel: invalid birthday format: " + ex.getMessage());
                    JOptionPane.showMessageDialog(this, "Invalid format. Use DD.MM.YYYY or DD.MM.xxxx");
                    refreshTable();
                }
            }
        });

        table = new JTable(model);
        table.setBackground(UIStyle.SECONDARY_BG);
        table.setForeground(Color.WHITE);
        table.setGridColor(UIStyle.BORDER_COLOR);
        table.setRowHeight(30);
        table.setFillsViewportHeight(true);

        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(0).setMaxWidth(80);
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(tbl, row + 1, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                return label;
            }
        });
        table.getColumnModel().getColumn(1).setPreferredWidth(250);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);

        JTableHeader header = table.getTableHeader();
        header.setBackground(UIStyle.BG_COLOR);
        header.setForeground(Color.GRAY);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
                lbl.setBackground(UIStyle.BG_COLOR);
                lbl.setForeground(Color.GRAY);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, UIStyle.BORDER_COLOR));
                return lbl;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        UIStyle.styleScrollBar(scroll);
        scroll.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));
        scroll.getViewport().setBackground(UIStyle.BG_COLOR);
        JPanel corner = new JPanel();
        corner.setBackground(UIStyle.BG_COLOR);
        scroll.setCorner(JScrollPane.UPPER_RIGHT_CORNER, corner);
        JPanel corner2 = new JPanel();
        corner2.setBackground(UIStyle.BG_COLOR);
        scroll.setCorner(JScrollPane.LOWER_RIGHT_CORNER, corner2);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        form.setBackground(UIStyle.BG_COLOR);

        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setForeground(UIStyle.TEXT_COLOR);
        JTextField nameField = new JTextField(12);
        UIStyle.styleTextField(nameField);

        JLabel dateLabel = new JLabel("Date (dd.MM.yyyy):");
        dateLabel.setForeground(UIStyle.TEXT_COLOR);
        JTextField dateField = new JTextField(10);
        dateField.setToolTipText("Format: dd.MM.yyyy or dd.MM.xxxx");
        UIStyle.styleTextField(dateField);

        JButton addBtn = new JButton("Add");
        addBtn.setForeground(new Color(150, 255, 150));
        JButton removeBtn = new JButton("Remove");
        removeBtn.setForeground(new Color(255, 150, 150));
        UIStyle.styleButton(addBtn);
        UIStyle.styleButton(removeBtn);

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
                notifyDataChanged();
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
                notifyDataChanged();
            }
        });

        form.add(nameLabel);
        form.add(nameField);
        form.add(dateLabel);
        form.add(dateField);
        form.add(addBtn);
        form.add(removeBtn);

        add(title, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(form, BorderLayout.SOUTH);

        refreshTable();
    }

    private void notifyDataChanged() {
        if (onDataChanged != null) {
            onDataChanged.run();
        }
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

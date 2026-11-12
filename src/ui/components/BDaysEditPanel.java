package ui.components;

import db.BDaysRepository;
import service.AchievementService;
import service.BDaysService;
import ui.UIStyle;
import util.AppLogger;
import util.I18n;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
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

        JLabel title = new JLabel(" " + I18n.t("bday.edit.title"));
        title.setForeground(UIStyle.ACCENT_COLOR);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));

        model = new DefaultTableModel(new String[]{"ID", I18n.t("bday.col.name"), I18n.t("bday.col.birthday")}, 0) {
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
                    AppLogger.info("Birthday updated: " + name + " (" + dateStr + ")");
                    notifyDataChanged();
                } catch (Exception ex) {
                    AppLogger.error("BDaysEditPanel: invalid birthday format: " + ex.getMessage());
                    JOptionPane.showMessageDialog(this, I18n.t("bday.err.format"));
                    refreshTable();
                }
            }
        });

        table = new JTable(model);
        table.setBackground(UIStyle.SECONDARY_BG);
        table.setForeground(Color.WHITE);
        table.setRowHeight(30);
        table.setFillsViewportHeight(true);
        UIStyle.styleTable(table);

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

        JScrollPane scroll = new JScrollPane(table);
        UIStyle.styleScrollBar(scroll);
        scroll.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));
        scroll.getViewport().setBackground(UIStyle.BG_COLOR);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        form.setBackground(UIStyle.BG_COLOR);

        JLabel nameLabel = new JLabel(I18n.t("bday.col.name") + ":");
        nameLabel.setForeground(UIStyle.TEXT_COLOR);
        JTextField nameField = new JTextField(12);
        UIStyle.styleTextField(nameField);

        JLabel dateLabel = new JLabel(I18n.t("daily.date"));
        dateLabel.setForeground(UIStyle.TEXT_COLOR);
        JTextField dateField = new JTextField(10);
        dateField.setToolTipText(I18n.t("bday.tip.format"));
        UIStyle.styleTextField(dateField);

        JButton addBtn = new JButton(I18n.t("common.add"));
        addBtn.setForeground(new Color(150, 255, 150));
        JButton removeBtn = new JButton(I18n.t("bday.remove"));
        removeBtn.setForeground(new Color(255, 150, 150));
        UIStyle.styleButton(addBtn);
        UIStyle.styleButton(removeBtn);

        addBtn.addActionListener(e -> {
            try {
                String inputDate = dateField.getText().trim();
                String name = nameField.getText().trim();

                if (name.isEmpty() || inputDate.isEmpty()) {
                    JOptionPane.showMessageDialog(this, I18n.t("bday.err.fill"));
                    return;
                }
                repo.addBirthday(name, bdaysService.uiToDb(inputDate));
                AppLogger.info("Birthday added: " + name + " (" + inputDate + ")");

                achievementService.complete(login, "real_friend");
                refreshTable();
                notifyDataChanged();
                dateField.setText("");
                nameField.setText("");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, I18n.t("bday.err.format"), I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
            }
        });
        removeBtn.addActionListener(e -> {
            int sel = table.getSelectedRow();
            if (sel >= 0) {
                int id = (int) model.getValueAt(sel, 0);
                String name = (String) model.getValueAt(sel, 1);
                repo.removeBirthday(id);
                AppLogger.info("Birthday removed: " + name);
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

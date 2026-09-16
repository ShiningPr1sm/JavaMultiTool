package ui.dailytracker;

import db.NutritionRepository;
import ui.UIStyle;
import util.AppLogger;
import util.DateUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.LocalDate;

public class NutritionPanel extends JPanel {
    private final NutritionRepository repo;
    private final JTextField dateField, mealField, caloriesField;
    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel totalLabel;
    private final Runnable onDataChanged;

    public NutritionPanel(NutritionRepository repo, Runnable onDataChanged) {
        this.repo = repo;
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(0, 10));
        setBackground(UIStyle.BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 5, 4, 5);

        form.add(label("Date (dd.MM.yyyy):"), c);
        c.gridx = 1;
        dateField = new JTextField(10);
        UIStyle.styleTextField(dateField);
        dateField.setText(DateUtils.todayDisplay());
        form.add(dateField, c);

        c.gridx = 2;
        form.add(label("Meal:"), c);
        c.gridx = 3;
        c.gridwidth = 2;
        mealField = new JTextField(15);
        UIStyle.styleTextField(mealField);
        form.add(mealField, c);

        c.gridx = 5;
        c.gridwidth = 1;
        form.add(label("Calories:"), c);
        c.gridx = 6;
        c.gridwidth = 2;
        caloriesField = new JTextField(8);
        UIStyle.styleTextField(caloriesField);
        form.add(caloriesField, c);

        c.gridx = 8;
        JButton addBtn = new JButton("Add");
        UIStyle.styleButton(addBtn);
        addBtn.addActionListener(e -> addFood());
        form.add(addBtn, c);

        c.gridx = 9;
        JButton todayBtn = new JButton("Today");
        UIStyle.styleButton(todayBtn);
        todayBtn.addActionListener(e -> {
            dateField.setText(DateUtils.todayDisplay());
            refreshTable();
        });
        form.add(todayBtn, c);

        model = new DefaultTableModel(new String[]{"ID", "Meal", "Calories"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setBackground(UIStyle.SECONDARY_BG);
        table.setForeground(Color.WHITE);
        table.setGridColor(UIStyle.BORDER_COLOR);
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(280);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        styleTable(table);

        JScrollPane scroll = new JScrollPane(table);
        UIStyle.styleScrollBar(scroll);
        scroll.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));
        scroll.getViewport().setBackground(UIStyle.SECONDARY_BG);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        JButton deleteBtn = new JButton("Delete Selected");
        UIStyle.styleButton(deleteBtn);
        deleteBtn.addActionListener(e -> deleteSelected());
        totalLabel = new JLabel("Total: 0 kcal");
        totalLabel.setForeground(UIStyle.ACCENT_COLOR);
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        bottom.add(deleteBtn, BorderLayout.WEST);
        bottom.add(totalLabel, BorderLayout.EAST);

        add(form, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        refreshTable();
    }

    private void addFood() {
        LocalDate date = parseDateField();
        if (date == null) return;
        String meal = mealField.getText().trim();
        if (meal.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a date and a meal name.");
            return;
        }
        int calories;
        try {
            calories = Integer.parseInt(caloriesField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Calories must be a number.");
            return;
        }
        if (calories < 0) {
            JOptionPane.showMessageDialog(this, "Calories cannot be negative.");
            return;
        }
        repo.addFood(DateUtils.toISO(date), meal, calories);
        AppLogger.info("Nutrition: added '" + meal + "' " + calories + " kcal for " + date);
        mealField.setText("");
        caloriesField.setText("");
        refreshTable();
        notifyChanged();
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a row to delete.");
            return;
        }
        int id = (int) model.getValueAt(row, 0);
        repo.deleteFood(id);
        AppLogger.info("Nutrition: deleted food entry id=" + id);
        refreshTable();
        notifyChanged();
    }

    private void refreshTable() {
        model.setRowCount(0);
        String date = validDateISO();
        if (date == null) {
            totalLabel.setText("Total: 0 kcal");
            return;
        }
        try {
            for (db.FoodEntry entry : repo.listByDate(date)) {
                model.addRow(new Object[]{entry.id(), entry.mealName(), entry.calories()});
            }
        } catch (Exception e) {
            AppLogger.error("NutritionPanel: failed to load entries - " + e.getMessage());
        }
        totalLabel.setText("Total: " + repo.totalCalories(date) + " kcal");
    }

    private LocalDate parseDateField() {
        String text = dateField.getText().trim();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a date (dd.MM.yyyy).");
            return null;
        }
        try {
            return DateUtils.parseDisplay(text);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid date. Use format dd.MM.yyyy.");
            return null;
        }
    }

    private String validDateISO() {
        String text = dateField.getText().trim();
        if (text.isEmpty()) return null;
        try {
            return DateUtils.toISO(DateUtils.parseDisplay(text));
        } catch (Exception e) {
            return null;
        }
    }

    private void notifyChanged() {
        if (onDataChanged != null) onDataChanged.run();
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(UIStyle.TEXT_COLOR);
        return l;
    }

    static void styleTable(JTable table) {
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
                return lbl;
            }
        });
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
                if (s) {
                    lbl.setBackground(UIStyle.BUTTON_HOVER);
                    lbl.setForeground(UIStyle.ACCENT_COLOR);
                } else {
                    lbl.setBackground(UIStyle.SECONDARY_BG);
                    lbl.setForeground(Color.WHITE);
                }
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
                return lbl;
            }
        });
    }
}
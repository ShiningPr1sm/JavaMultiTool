package ui.dailytracker;

import db.StepsEntry;
import db.StepsRepository;
import ui.UIStyle;
import util.AppLogger;
import util.DateUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class StepsPanel extends JPanel {
    private final StepsRepository repo;
    private final JTextField dateField, stepsField, weightField, caloriesField;
    private final JCheckBox weightBox;
    private final JRadioButton autoBtn, manualBtn;
    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel totalLabel;
    private final Runnable onDataChanged;

    public StepsPanel(StepsRepository repo, Runnable onDataChanged) {
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
        form.add(label("Steps:"), c);
        c.gridx = 3;
        c.gridwidth = 2;
        stepsField = new JTextField(10);
        UIStyle.styleTextField(stepsField);
        form.add(stepsField, c);

        c.gridx = 5;
        c.gridwidth = 1;
        autoBtn = new JRadioButton("Auto");
        autoBtn.setFocusPainted(false);
        autoBtn.setOpaque(false);
        autoBtn.setForeground(UIStyle.TEXT_COLOR);
        autoBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        manualBtn = new JRadioButton("Manual");
        manualBtn.setFocusPainted(false);
        manualBtn.setOpaque(false);
        manualBtn.setForeground(UIStyle.TEXT_COLOR);
        manualBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        autoBtn.setSelected(true);
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(autoBtn);
        modeGroup.add(manualBtn);
        form.add(autoBtn, c);
        c.gridx = 6;
        form.add(manualBtn, c);

        c.gridx = 7;
        form.add(label("Calories:"), c);
        c.gridx = 8;
        caloriesField = new JTextField(8);
        UIStyle.styleTextField(caloriesField);
        form.add(caloriesField, c);

        c.gridy = 1;
        c.gridx = 0;
        form.add(label("Weight (kg):"), c);
        c.gridx = 1;
        weightField = new JTextField(8);
        UIStyle.styleTextField(weightField);
        form.add(weightField, c);
        c.gridx = 2;
        weightBox = new JCheckBox("Use my weight");
        UIStyle.styleCheckbox(weightBox);
        form.add(weightBox, c);

        c.gridx = 4;
        JButton addBtn = new JButton("Add");
        UIStyle.styleButton(addBtn);
        addBtn.addActionListener(e -> addSteps());
        form.add(addBtn, c);

        c.gridx = 5;
        JButton todayBtn = new JButton("Today");
        UIStyle.styleButton(todayBtn);
        todayBtn.addActionListener(e -> {
            dateField.setText(DateUtils.todayDisplay());
            refreshTable();
        });
        form.add(todayBtn, c);

        Runnable updateMode = this::updateEnabledState;
        autoBtn.addActionListener(e -> updateMode.run());
        manualBtn.addActionListener(e -> updateMode.run());
        weightBox.addActionListener(e -> updateMode.run());

        model = new DefaultTableModel(new String[]{"ID", "Steps", "Calories", "Mode"}, 0) {
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
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(140);
        NutritionPanel.styleTable(table);

        JScrollPane scroll = new JScrollPane(table);
        UIStyle.styleScrollBar(scroll);
        scroll.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));
        scroll.getViewport().setBackground(UIStyle.SECONDARY_BG);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        JButton deleteBtn = new JButton("Delete Selected");
        UIStyle.styleButton(deleteBtn);
        deleteBtn.addActionListener(e -> deleteSelected());
        totalLabel = new JLabel("Steps: 0 | Calories: 0 kcal");
        totalLabel.setForeground(UIStyle.ACCENT_COLOR);
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        bottom.add(deleteBtn, BorderLayout.WEST);
        bottom.add(totalLabel, BorderLayout.EAST);

        add(form, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        loadWeight();
        updateEnabledState();
        refreshTable();
    }

    private void loadWeight() {
        Double w = repo.getWeight();
        if (w != null) {
            weightField.setText(String.valueOf(w));
            weightBox.setSelected(true);
        }
    }

    private void updateEnabledState() {
        boolean auto = autoBtn.isSelected();
        caloriesField.setEnabled(!auto);
        weightField.setEnabled(auto && weightBox.isSelected());
        weightBox.setEnabled(auto);
    }

    private void addSteps() {
        String dateISO = validDateISO();
        if (dateISO == null) {
            JOptionPane.showMessageDialog(this, "Invalid date. Use format dd.MM.yyyy.");
            return;
        }
        int steps;
        try {
            steps = Integer.parseInt(stepsField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Steps must be a number.");
            return;
        }
        if (steps < 0) {
            JOptionPane.showMessageDialog(this, "Steps cannot be negative.");
            return;
        }

        String calcMode;
        Double weightUsed = null;
        int calories;
        if (autoBtn.isSelected()) {
            if (weightBox.isSelected()) {
                double weight;
                try {
                    weight = Double.parseDouble(weightField.getText().trim());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Enter your weight to use the weight-based calculation.");
                    return;
                }
                if (weight <= 0) {
                    JOptionPane.showMessageDialog(this, "Weight must be positive.");
                    return;
                }
                weightUsed = weight;
                calcMode = "weight";
                calories = (int) Math.round(steps * weight * 0.0005);
                repo.setWeight(weight);
            } else {
                calcMode = "fixed";
                calories = (int) Math.round(steps * 0.04);
            }
        } else {
            calcMode = "manual";
            try {
                calories = Integer.parseInt(caloriesField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter calories manually.");
                return;
            }
            if (calories < 0) {
                JOptionPane.showMessageDialog(this, "Calories cannot be negative.");
                return;
            }
        }

        repo.addSteps(dateISO, steps, calories, calcMode, weightUsed);
        AppLogger.info("Steps: " + steps + " steps, " + calories + " kcal (" + calcMode + ") for " + dateISO);
        stepsField.setText("");
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
        repo.deleteSteps(id);
        AppLogger.info("Steps: deleted entry id=" + id);
        refreshTable();
        notifyChanged();
    }

    private void refreshTable() {
        model.setRowCount(0);
        String date = validDateISO();
        if (date == null) {
            totalLabel.setText("Steps: 0 | Calories: 0 kcal");
            return;
        }
        try {
            for (StepsEntry entry : repo.listByDate(date)) {
                model.addRow(new Object[]{entry.id(), entry.steps(), entry.calories(), modeLabel(entry.calcMode())});
            }
        } catch (Exception e) {
            AppLogger.error("StepsPanel: failed to load entries - " + e.getMessage());
        }
        totalLabel.setText("Steps: " + repo.totalSteps(date) + " | Calories: " + repo.totalCalories(date) + " kcal");
    }

    private static String modeLabel(String mode) {
        return switch (mode) {
            case "weight" -> "By weight";
            case "manual" -> "Manual";
            default -> "Fixed coef.";
        };
    }

    private void notifyChanged() {
        if (onDataChanged != null) onDataChanged.run();
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

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(UIStyle.TEXT_COLOR);
        return l;
    }
}
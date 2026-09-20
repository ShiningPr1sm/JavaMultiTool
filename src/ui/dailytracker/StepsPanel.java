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
    private final JTextField dateField, stepsField;
    private JTextField weightField, caloriesField;
    private JCheckBox weightBox;
    private JRadioButton autoBtn, manualBtn;
    private JLabel hintLabel;
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
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 5, 5, 5);

        // Row 0: Date, Steps, Add (one row)
        c.gridx = 0;
        c.gridy = 0;
        form.add(label("Date (dd.MM.yyyy):"), c);
        c.gridx = 1;
        dateField = new JTextField(11);
        UIStyle.styleTextField(dateField);
        dateField.setText(DateUtils.todayDisplay());
        form.add(dateField, c);

        c.gridx = 3;
        c.insets = new Insets(5, 20, 5, 5);
        form.add(label("Steps:"), c);
        c.gridx = 4;
        c.insets = new Insets(5, 5, 5, 5);
        stepsField = new JTextField(9);
        UIStyle.styleTextField(stepsField);
        form.add(stepsField, c);

        c.gridx = 5;
        JButton addBtn = new JButton("Add Entry");
        UIStyle.styleButton(addBtn);
        addBtn.addActionListener(e -> addSteps());
        form.add(addBtn, c);

        // Row 1: Calories calculation group
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        form.add(label("Calories:"), c);
        c.gridx = 1;
        c.gridwidth = 5;
        c.anchor = GridBagConstraints.WEST;
        form.add(buildCaloriesGroup(), c);
        c.gridwidth = 1;

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

    private JPanel buildCaloriesGroup() {
        autoBtn = makeRadio("Calculate automatically from steps");
        manualBtn = makeRadio("Enter calories manually");
        autoBtn.setSelected(true);
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(autoBtn);
        modeGroup.add(manualBtn);

        weightBox = new JCheckBox("Use my weight");
        UIStyle.styleCheckbox(weightBox);
        weightField = new JTextField(8);
        UIStyle.styleTextField(weightField);
        JLabel weightUnit = label("kg");
        weightUnit.setForeground(new Color(180, 180, 180));

        JPanel weightRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        weightRow.setOpaque(false);
        weightRow.add(weightBox);
        weightRow.add(weightField);
        weightRow.add(weightUnit);

        caloriesField = new JTextField(8);
        UIStyle.styleTextField(caloriesField);
        JPanel manualRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        manualRow.setOpaque(false);
        JLabel manualLbl = label("Calories:");
        manualLbl.setForeground(new Color(200, 200, 200));
        manualRow.add(manualLbl);
        manualRow.add(caloriesField);

        hintLabel = new JLabel("Calories = steps × 0.04 (fixed coefficient)");
        hintLabel.setForeground(new Color(150, 150, 150));
        hintLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.setBackground(new Color(30, 30, 30));
        group.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.BORDER_COLOR),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        group.add(autoBtn);
        group.add(indent(weightRow));
        group.add(indent(hintLabel));
        group.add(Box.createVerticalStrut(6));
        group.add(manualBtn);
        group.add(indent(manualRow));

        Runnable updateMode = this::updateEnabledState;
        autoBtn.addActionListener(e -> updateMode.run());
        manualBtn.addActionListener(e -> updateMode.run());
        weightBox.addActionListener(e -> updateMode.run());
        return group;
    }

    private JRadioButton makeRadio(String text) {
        JRadioButton b = new JRadioButton(text);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setForeground(UIStyle.TEXT_COLOR);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        return b;
    }

    private JPanel indent(JComponent comp) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 1));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBorder(BorderFactory.createEmptyBorder(0, 26, 0, 0));
        p.add(comp);
        return p;
    }

    private void updateEnabledState() {
        boolean auto = autoBtn.isSelected();
        boolean byWeight = weightBox.isSelected();
        weightBox.setEnabled(auto);
        weightField.setEnabled(auto && byWeight);
        caloriesField.setEnabled(!auto);
        if (auto) {
            hintLabel.setText(byWeight
                    ? "Calories = steps × weight × 0.0005"
                    : "Calories = steps × 0.04 (fixed coefficient)");
        }
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
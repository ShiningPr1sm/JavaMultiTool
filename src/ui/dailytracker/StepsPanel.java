package ui.dailytracker;

import db.StepsEntry;
import db.StepsRepository;
import ui.StyledDialog;
import ui.UIStyle;
import util.AppLogger;
import util.DateUtils;
import util.I18n;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class StepsPanel extends JPanel {
    private final StepsRepository repo;
    private final JTextField dateField, stepsField;
    private JTextField weightField, caloriesField;
    private JCheckBox weightBox;
    private JRadioButton autoBtn, manualBtn;
    private JLabel hintLabel;
    private final DefaultTableModel model;
    private final JTable table;
    private final List<Integer> rowIds = new ArrayList<>();
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
        form.add(label(I18n.t("daily.date")), c);
        c.gridx = 1;
        dateField = new JTextField(11);
        UIStyle.styleTextField(dateField);
        dateField.setText(DateUtils.todayDisplay());
        form.add(dateField, c);

        c.gridx = 3;
        c.insets = new Insets(5, 20, 5, 5);
        form.add(label(I18n.t("daily.steps")), c);
        c.gridx = 4;
        c.insets = new Insets(5, 5, 5, 5);
        stepsField = new JTextField(9);
        UIStyle.styleTextField(stepsField);
        form.add(stepsField, c);

        c.gridx = 5;
        JButton addBtn = new JButton(I18n.t("steps.addEntry"));
        UIStyle.styleButton(addBtn);
        addBtn.addActionListener(e -> addSteps());
        form.add(addBtn, c);

        // Row 1: Calories calculation group
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        form.add(label(I18n.t("nutrition.calories")), c);
        c.gridx = 1;
        c.gridwidth = 5;
        c.anchor = GridBagConstraints.WEST;
        form.add(buildCaloriesGroup(), c);
        c.gridwidth = 1;

        model = new DefaultTableModel(new String[]{I18n.t("daily.steps"), I18n.t("nutrition.col.calories"), I18n.t("steps.col.mode")}, 0) {
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
        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        NutritionPanel.styleTable(table);

        JScrollPane scroll = new JScrollPane(table);
        UIStyle.styleScrollBar(scroll);
        scroll.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));
        scroll.getViewport().setBackground(UIStyle.SECONDARY_BG);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        JButton deleteBtn = new JButton(I18n.t("common.deleteSelected"));
        UIStyle.styleButton(deleteBtn);
        deleteBtn.addActionListener(e -> deleteSelected());
        totalLabel = new JLabel(I18n.t("steps.total", "0", "0"));
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
        autoBtn = makeRadio(I18n.t("steps.auto"));
        manualBtn = makeRadio(I18n.t("steps.manual"));
        autoBtn.setSelected(true);
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(autoBtn);
        modeGroup.add(manualBtn);

        weightBox = new JCheckBox(I18n.t("steps.useWeight"));
        UIStyle.styleCheckbox(weightBox);
        weightField = new JTextField(8);
        UIStyle.styleTextField(weightField);
        JLabel weightUnit = label(I18n.t("unit.kg"));
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

        hintLabel = new JLabel(I18n.t("steps.hintFixed"));
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
                    ? I18n.t("steps.hintWeight")
                    : I18n.t("steps.hintFixed"));
        }
    }

    private void addSteps() {
        String dateISO = validDateISO();
        if (dateISO == null) {
            StyledDialog.show(SwingUtilities.windowForComponent(this), I18n.t("daily.error.dateFormat"));
            return;
        }
        int steps;
        try {
            steps = Integer.parseInt(stepsField.getText().trim());
        } catch (NumberFormatException ex) {
            StyledDialog.show(SwingUtilities.windowForComponent(this), I18n.t("daily.error.stepsNumber"));
            return;
        }
        if (steps < 0) {
            StyledDialog.show(SwingUtilities.windowForComponent(this), I18n.t("daily.error.stepsNegative"));
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
                    StyledDialog.show(SwingUtilities.windowForComponent(this), I18n.t("steps.err.enterWeight"));
                    return;
                }
                if (weight <= 0) {
                    StyledDialog.show(SwingUtilities.windowForComponent(this), I18n.t("daily.error.weightPositive"));
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
                StyledDialog.show(SwingUtilities.windowForComponent(this), I18n.t("steps.err.enterCalories"));
                return;
            }
            if (calories < 0) {
                StyledDialog.show(SwingUtilities.windowForComponent(this), I18n.t("daily.error.caloriesNegative"));
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
            StyledDialog.show(SwingUtilities.windowForComponent(this), I18n.t("steps.err.selectDelete"));
            return;
        }
        int id = rowIds.get(row);
        repo.deleteSteps(id);
        AppLogger.info("Steps: deleted entry id=" + id);
        refreshTable();
        notifyChanged();
    }

    private void refreshTable() {
        model.setRowCount(0);
        rowIds.clear();
        String date = validDateISO();
        if (date == null) {
            totalLabel.setText(I18n.t("steps.total", "0", "0"));
            return;
        }
        try {
            for (StepsEntry entry : repo.listByDate(date)) {
                rowIds.add(entry.id());
                model.addRow(new Object[]{entry.steps(), entry.calories(), modeLabel(entry.calcMode())});
            }
        } catch (Exception e) {
            AppLogger.error("StepsPanel: failed to load entries - " + e.getMessage());
        }
        totalLabel.setText(I18n.t("steps.total",
                String.valueOf(repo.totalSteps(date)), String.valueOf(repo.totalCalories(date))));
    }

    private static String modeLabel(String mode) {
        return switch (mode) {
            case "weight" -> I18n.t("steps.mode.weight");
            case "manual" -> I18n.t("steps.mode.manual");
            default -> I18n.t("steps.mode.fixed");
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
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
    private final JTextField dateField, mealField, caloriesField, weightField;
    private final JCheckBox manualModeBox;
    private final JLabel caloriesLabel, weightLabel;
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

        // ---------- Form components (all created up front) ----------
        dateField = field(10);
        dateField.setText(DateUtils.todayDisplay());
        // Column widths: Date = Calories (10), Meal = Weight (12).
        // Fields stacked in the same grid column now have identical widths.
        mealField = field(12);
        caloriesField = field(10);
        weightField = field(12);

        caloriesLabel = label("Calories:");
        // Fixed width so the column doesn't jump when the text changes
        // between "Calories:" and "Calories per 100g:"
        Dimension calLabelSize = new Dimension(140, caloriesLabel.getPreferredSize().height);
        caloriesLabel.setPreferredSize(calLabelSize);
        caloriesLabel.setMinimumSize(calLabelSize);

        weightLabel = label("Weight (g):");

        // "Meal:" and "Weight (g):" share column 2. Give both labels the same
        // fixed width so the column doesn't grow when Weight becomes visible
        // (that growth was pushing the Meal field and Add button to the right).
        JLabel mealLabel = label("Meal:");
        int sharedLabelWidth = Math.max(mealLabel.getPreferredSize().width, weightLabel.getPreferredSize().width);
        Dimension sharedLabelSize = new Dimension(sharedLabelWidth, mealLabel.getPreferredSize().height);
        mealLabel.setPreferredSize(sharedLabelSize);
        mealLabel.setMinimumSize(sharedLabelSize);
        weightLabel.setPreferredSize(sharedLabelSize);
        weightLabel.setMinimumSize(sharedLabelSize);

        JButton addBtn = new JButton("Add");
        UIStyle.styleButton(addBtn);
        addBtn.addActionListener(e -> addFood());

        manualModeBox = new JCheckBox("Manual mode");
        UIStyle.styleCheckbox(manualModeBox);
        manualModeBox.setSelected(true);
        manualModeBox.setToolTipText("Checked: enter total calories. Unchecked: enter total weight and calories per 100 g.");
        manualModeBox.addActionListener(e -> updateMode());

        // ---------- Form layout ----------
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 5, 4, 5);

        // Row 0: Date, Meal, Add
        c.gridy = 0;
        c.gridx = 0; form.add(label("Date (dd.MM.yyyy):"), c);
        c.gridx = 1; form.add(dateField, c);
        c.gridx = 2; form.add(mealLabel, c);
        c.gridx = 3; form.add(mealField, c);
        c.gridx = 4; form.add(addBtn, c);

        // Row 1: Calories, Weight (Weight is hidden in manual mode).
        // Columns 2-3 keep their width because "Meal:" and the meal field
        // in row 0 occupy them, so hiding Weight doesn't shrink the form.
        c.gridy = 1;
        c.gridx = 0; form.add(caloriesLabel, c);
        c.gridx = 1; form.add(caloriesField, c);
        c.gridx = 2; form.add(weightLabel, c);
        c.gridx = 3; form.add(weightField, c);

        // Row 2: mode toggle
        c.gridy = 2;
        c.gridx = 0;
        c.gridwidth = 2;
        form.add(manualModeBox, c);
        c.gridwidth = 1;

        // Filler column: absorbs extra width so the form stays left-aligned
        // and doesn't get re-centered when the window is resized
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        c.gridy = 0;
        c.gridx = 5;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(filler, c);

        updateMode();

        // ---------- Table ----------
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

        // ---------- Bottom bar ----------
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

    /** Creates a styled text field that GridBagLayout can't shrink below its preferred size. */
    private JTextField field(int columns) {
        JTextField f = new JTextField(columns);
        UIStyle.styleTextField(f);
        f.setMinimumSize(f.getPreferredSize());
        return f;
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
        if (manualModeBox.isSelected()) {
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
        } else {
            double weight;
            double per100;
            try {
                weight = Double.parseDouble(weightField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter the total weight in grams.");
                return;
            }
            try {
                per100 = Double.parseDouble(caloriesField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter calories per 100 g.");
                return;
            }
            if (weight <= 0) {
                JOptionPane.showMessageDialog(this, "Weight must be positive.");
                return;
            }
            if (per100 < 0) {
                JOptionPane.showMessageDialog(this, "Calories per 100 g cannot be negative.");
                return;
            }
            calories = (int) Math.round(weight / 100.0 * per100);
        }
        repo.addFood(DateUtils.toISO(date), meal, calories);
        AppLogger.info("Nutrition: added '" + meal + "' " + calories + " kcal for " + date);
        mealField.setText("");
        caloriesField.setText("");
        weightField.setText("");
        refreshTable();
        notifyChanged();
    }

    private void updateMode() {
        boolean manual = manualModeBox.isSelected();
        caloriesLabel.setText(manual ? "Calories:" : "Calories per 100g:");
        // Hiding is safe here: the columns holding Weight are still sized
        // by the Meal label/field in the row above
        weightLabel.setVisible(!manual);
        weightField.setVisible(!manual);
        if (manual) weightField.setText("");
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
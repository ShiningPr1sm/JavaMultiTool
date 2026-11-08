package ui.dailytracker;

import db.NutritionRepository;
import db.ProductEntry;
import db.ProductRepository;
import ui.StyledDialog;
import ui.UIStyle;
import util.AppLogger;
import util.DateUtils;
import util.I18n;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.LocalDate;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

/**
 * Calories tab of the daily tracker: an input form (manual entry or product
 * selected from the catalog), the day's food table and a total. Notifies
 * {@code onDataChanged} after every data mutation so the overview and other
 * tabs refresh.
 */
public class NutritionPanel extends JPanel {
    private final NutritionRepository repo;
    private final ProductRepository productsRepo;
    private final JTextField dateField, mealField, caloriesField, weightField;
    private final JCheckBox manualModeBox;
    private final JComboBox<ProductEntry> catalogBox;
    private final JLabel caloriesLabel, weightLabel;
    private final DefaultTableModel model;
    private final JTable table;
    private final List<Integer> rowIds = new ArrayList<>();
    private final JLabel totalLabel;
    private final Runnable onDataChanged;
    private boolean reloadingCatalog;

    public NutritionPanel(NutritionRepository repo, ProductRepository productsRepo, Runnable onDataChanged) {
        this.repo = repo;
        this.productsRepo = productsRepo;
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(0, 10));
        setBackground(UIStyle.BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // ---------- Form components ----------
        dateField = field(10);
        dateField.setText(DateUtils.todayDisplay());
        // column widths pair up where fields share a grid column (Date=Calories, Meal=Weight)
        mealField = field(12);
        caloriesField = field(10);
        weightField = field(12);

        caloriesLabel = label(I18n.t("nutrition.calories"));
        // fixed width so the column doesn't jump when the label text switches
        Dimension calLabelSize = new Dimension(140, caloriesLabel.getPreferredSize().height);
        caloriesLabel.setPreferredSize(calLabelSize);
        caloriesLabel.setMinimumSize(calLabelSize);

        weightLabel = label(I18n.t("nutrition.weight"));

        // both labels share one fixed width so the shared column doesn't grow when Weight becomes visible
        JLabel mealLabel = label(I18n.t("nutrition.meal"));
        int sharedLabelWidth = Math.max(mealLabel.getPreferredSize().width, weightLabel.getPreferredSize().width);
        Dimension sharedLabelSize = new Dimension(sharedLabelWidth, mealLabel.getPreferredSize().height);
        mealLabel.setPreferredSize(sharedLabelSize);
        mealLabel.setMinimumSize(sharedLabelSize);
        weightLabel.setPreferredSize(sharedLabelSize);
        weightLabel.setMinimumSize(sharedLabelSize);

        JButton addBtn = new JButton(I18n.t("common.add"));
        UIStyle.styleButton(addBtn);
        addBtn.addActionListener(e -> addFood());

        manualModeBox = new JCheckBox(I18n.t("nutrition.manualMode"));
        UIStyle.styleCheckbox(manualModeBox);
        manualModeBox.setSelected(true);
        manualModeBox.setToolTipText(I18n.t("nutrition.manualModeTip"));
        manualModeBox.addActionListener(e -> updateMode());

        catalogBox = new JComboBox<>();
        UIStyle.styleComboBox(catalogBox);
        catalogBox.setPreferredSize(new Dimension(260, 30));
        catalogBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Object display = (value instanceof ProductEntry p) ? p.name() : I18n.t("nutrition.selectCatalog");
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, display, index, isSelected, cellHasFocus);
                lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                lbl.setOpaque(true);
                if (isSelected) {
                    lbl.setBackground(UIStyle.BUTTON_HOVER);
                    lbl.setForeground(UIStyle.ACCENT_COLOR);
                } else {
                    lbl.setBackground(UIStyle.BUTTON_BG);
                    lbl.setForeground(UIStyle.TEXT_COLOR);
                }
                lbl.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                return lbl;
            }
        });
        catalogBox.addActionListener(e -> {
            if (reloadingCatalog) return;
            ProductEntry p = (ProductEntry) catalogBox.getSelectedItem();
            if (p == null) return;
            mealField.setText(p.name());
            if (p.calories() != null) {
                manualModeBox.setSelected(true);
                caloriesField.setText(String.valueOf(p.calories()));
            }
        });

        // ---------- Form layout ----------
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 5, 4, 5);

        c.gridy = 0;
        c.gridx = 0; form.add(label(I18n.t("daily.date")), c);
        c.gridx = 1; form.add(dateField, c);
        c.gridx = 2; form.add(mealLabel, c);
        c.gridx = 3; form.add(mealField, c);
        c.gridx = 4; form.add(addBtn, c);

        // Weight is hidden in manual mode; columns 2-3 keep their width
        // because row 0's Meal widgets occupy them
        c.gridy = 1;
        c.gridx = 0; form.add(caloriesLabel, c);
        c.gridx = 1; form.add(caloriesField, c);
        c.gridx = 2; form.add(weightLabel, c);
        c.gridx = 3; form.add(weightField, c);

        c.gridy = 2;
        c.gridx = 0;
        c.gridwidth = 2;
        form.add(manualModeBox, c);
        c.gridwidth = 1;

        c.gridy = 3;
        c.gridx = 0;
        form.add(label(I18n.t("nutrition.fromCatalog")), c);
        c.gridx = 1;
        c.gridwidth = 4;
        form.add(catalogBox, c);
        c.gridwidth = 1;

        // filler absorbs extra width so the form stays left-aligned on resize
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        c.gridy = 0;
        c.gridx = 5;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(filler, c);

        updateMode();
        reloadCatalog();

        // ---------- Table ----------
        model = new DefaultTableModel(new String[]{I18n.t("nutrition.col.meal"), I18n.t("nutrition.col.calories")}, 0) {
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
        table.getColumnModel().getColumn(0).setPreferredWidth(280);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        styleTable(table);

        JScrollPane scroll = new JScrollPane(table);
        UIStyle.styleScrollBar(scroll);
        scroll.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));
        scroll.getViewport().setBackground(UIStyle.SECONDARY_BG);

        // ---------- Bottom bar ----------
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        JButton deleteBtn = new JButton(I18n.t("common.deleteSelected"));
        UIStyle.styleButton(deleteBtn);
        deleteBtn.addActionListener(e -> deleteSelected());
        totalLabel = new JLabel(I18n.t("nutrition.total", "0"));
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
            StyledDialog.show(SwingUtilities.windowForComponent(this), I18n.t("nutrition.err.dateMeal"));
            return;
        }
        int calories;
        if (manualModeBox.isSelected()) {
            try {
                calories = Integer.parseInt(caloriesField.getText().trim());
            } catch (NumberFormatException ex) {
                StyledDialog.show(SwingUtilities.windowForComponent(this), I18n.t("daily.error.caloriesNumber"));
                return;
            }
            if (calories < 0) {
                StyledDialog.show(SwingUtilities.windowForComponent(this), I18n.t("daily.error.caloriesNegative"));
                return;
            }
        } else {
            double weight;
            double per100;
            try {
                weight = Double.parseDouble(weightField.getText().trim());
            } catch (NumberFormatException ex) {
                StyledDialog.show(SwingUtilities.windowForComponent(this), I18n.t("daily.error.enterWeight"));
                return;
            }
            try {
                per100 = Double.parseDouble(caloriesField.getText().trim());
            } catch (NumberFormatException ex) {
                StyledDialog.show(SwingUtilities.windowForComponent(this), I18n.t("daily.error.enterCalories100"));
                return;
            }
            if (weight <= 0) {
                StyledDialog.show(SwingUtilities.windowForComponent(this), I18n.t("daily.error.weightPositive"));
                return;
            }
            if (per100 < 0) {
                StyledDialog.show(SwingUtilities.windowForComponent(this), I18n.t("daily.error.calories100Negative"));
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
        caloriesLabel.setText(manual ? I18n.t("nutrition.calories") : I18n.t("nutrition.per100"));
        // hiding is safe here: the --weight: columns are sized by row 0's Meal widgets
        weightLabel.setVisible(!manual);
        weightField.setVisible(!manual);
        if (manual) weightField.setText("");
    }

    /** Reloads the product catalog sorted by localized name, keeping the previous selection. */
    public void reloadCatalog() {
        ProductEntry selected = (ProductEntry) catalogBox.getSelectedItem();
        reloadingCatalog = true;
        catalogBox.removeAllItems();
        List<ProductEntry> all = new ArrayList<>(productsRepo.listAll());
        Collator col = Collator.getInstance(I18n.locale());
        col.setStrength(Collator.SECONDARY);
        all.sort((a, b) -> col.compare(a.name(), b.name()));
        for (ProductEntry p : all) catalogBox.addItem(p);
        if (selected != null) {
            for (ProductEntry p : all) {
                if (p.id() == selected.id()) {
                    catalogBox.setSelectedItem(p);
                    break;
                }
            }
        }
        reloadingCatalog = false;
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            StyledDialog.show(SwingUtilities.windowForComponent(this), "Select a row to delete.");
            return;
        }
        int id = rowIds.get(row);
        repo.deleteFood(id);
        AppLogger.info("Nutrition: deleted food entry id=" + id);
        refreshTable();
        notifyChanged();
    }

    /** Reloads the food table for the date shown in the date field. */
    public void refresh() {
        refreshTable();
    }

    private void refreshTable() {
        model.setRowCount(0);
        rowIds.clear();
        String date = validDateISO();
        if (date == null) {
            totalLabel.setText(I18n.t("nutrition.total", "0"));
            return;
        }
        try {
            for (db.FoodEntry entry : repo.listByDate(date)) {
                rowIds.add(entry.id());
                model.addRow(new Object[]{entry.mealName(), entry.calories()});
            }
        } catch (Exception e) {
            AppLogger.error("NutritionPanel: failed to load entries - " + e.getMessage());
        }
        totalLabel.setText(I18n.t("nutrition.total", String.valueOf(repo.totalCalories(date))));
    }

    private LocalDate parseDateField() {
        String text = dateField.getText().trim();
        if (text.isEmpty()) {
            StyledDialog.show(SwingUtilities.windowForComponent(this), I18n.t("daily.error.enterDate"));
            return null;
        }
        try {
            return DateUtils.parseDisplay(text);
        } catch (Exception ex) {
            StyledDialog.show(SwingUtilities.windowForComponent(this), I18n.t("daily.error.dateFormat"));
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
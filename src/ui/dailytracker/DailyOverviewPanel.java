package ui.dailytracker;

import db.FoodEntry;
import db.NutritionRepository;
import db.PurchasesRepository;
import db.StepsEntry;
import db.StepsRepository;
import service.DailyExportService;
import ui.StyledDialog;
import ui.UIStyle;
import ui.components.PieChartPanel;
import util.AppLogger;
import util.DateUtils;
import util.JavaFxFileChooser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class DailyOverviewPanel extends JPanel {
    private final NutritionRepository nutritionRepo;
    private final StepsRepository stepsRepo;
    private final PurchasesRepository purchasesRepo;
    private final DailyExportService exportService;
    private final JComboBox<LocalDate> dateSelector;
    private final ActionListener dateListener;
    private final PieChartPanel mealPie =
            new PieChartPanel("Calories by Meal", "No data recorded for this date",
                    v -> String.format("%.0f kcal", v), 280, 40);
    private final DailyBarChartPanel stepsBar = new DailyBarChartPanel();
    private final DailyBarChartPanel currencyBar = new DailyBarChartPanel();
    private final JLabel consumedVal = new JLabel("—");
    private final JLabel burnedVal = new JLabel("—");
    private final JLabel balanceVal = new JLabel("—");
    private final JLabel stepsVal = new JLabel("—");
    private final JLabel purchasesVal = new JLabel("—");
    private final JLabel weightVal = new JLabel("—");

    public DailyOverviewPanel(NutritionRepository nutritionRepo, StepsRepository stepsRepo, PurchasesRepository purchasesRepo) {
        this.nutritionRepo = nutritionRepo;
        this.stepsRepo = stepsRepo;
        this.purchasesRepo = purchasesRepo;
        this.exportService = new DailyExportService(nutritionRepo, stepsRepo, purchasesRepo);

        setLayout(new BorderLayout(0, 10));
        setBackground(UIStyle.BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setBackground(UIStyle.BG_COLOR);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        filterPanel.setBackground(UIStyle.BG_COLOR);

        JLabel dateLabel = new JLabel("<html><b style='color:white'>Date:</b></html>");
        filterPanel.add(dateLabel);

        dateSelector = new JComboBox<>();
        UIStyle.styleComboBox(dateSelector);
        dateSelector.setPreferredSize(new Dimension(140, 30));
        dateSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Object display = (value instanceof LocalDate d) ? DateUtils.toDisplay(d) : value;
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
        dateListener = e -> {
            LocalDate date = (LocalDate) dateSelector.getSelectedItem();
            if (date != null) updateCharts(date);
        };
        dateSelector.addActionListener(dateListener);
        filterPanel.add(dateSelector);

        JButton exportBtn = new JButton("Export JSON");
        UIStyle.styleButton(exportBtn);
        exportBtn.addActionListener(e -> exportJson());

        JButton refreshBtn = new JButton("Refresh");
        UIStyle.styleButton(refreshBtn);
        refreshBtn.addActionListener(e -> refresh());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnPanel.setBackground(UIStyle.BG_COLOR);
        btnPanel.add(exportBtn);
        btnPanel.add(refreshBtn);

        top.add(filterPanel, BorderLayout.WEST);
        top.add(btnPanel, BorderLayout.EAST);

        JPanel chartsPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        chartsPanel.setBackground(UIStyle.BG_COLOR);
        chartsPanel.add(mealPie);
        chartsPanel.add(stepsBar);
        chartsPanel.add(currencyBar);
        chartsPanel.add(buildStatsPanel());

        add(top, BorderLayout.NORTH);
        add(chartsPanel, BorderLayout.CENTER);

        refresh();
    }

    private JPanel buildStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIStyle.SECONDARY_BG);

        JLabel title = new JLabel("Day Summary");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setBorder(BorderFactory.createEmptyBorder(15, 15, 5, 15));
        title.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        JPanel rows = new JPanel(new GridLayout(6, 2, 10, 5));
        rows.setOpaque(false);
        rows.setBorder(BorderFactory.createEmptyBorder(10, 20, 15, 20));

        List<String> names = List.of("Consumed:", "Burned (steps):", "Net balance:",
                "Steps:", "Purchases:", "Weight:");
        List<JLabel> values = List.of(consumedVal, burnedVal, balanceVal, stepsVal, purchasesVal, weightVal);

        for (int i = 0; i < names.size(); i++) {
            JLabel n = new JLabel(names.get(i));
            n.setForeground(new Color(200, 200, 200));
            n.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            JLabel v = values.get(i);
            v.setForeground(UIStyle.ACCENT_COLOR);
            v.setFont(new Font("Segoe UI", Font.BOLD, 13));
            rows.add(n);
            rows.add(v);
        }

        Box box = Box.createVerticalBox();
        box.setBackground(UIStyle.SECONDARY_BG);
        box.setOpaque(true);
        box.add(title);
        box.add(rows);
        box.add(Box.createVerticalGlue());

        panel.add(box, BorderLayout.CENTER);
        return panel;
    }

    public void refresh() {
        LocalDate selected = (LocalDate) dateSelector.getSelectedItem();
        TreeSet<LocalDate> dates = new TreeSet<>();
        dates.add(LocalDate.now());
        for (String iso : queryDates("SELECT DISTINCT date FROM food_entries")) {
            try {
                dates.add(LocalDate.parse(iso));
            } catch (Exception ignored) {
            }
        }
        for (String iso : queryDates("SELECT DISTINCT date FROM steps_entries")) {
            try {
                dates.add(LocalDate.parse(iso));
            } catch (Exception ignored) {
            }
        }
        for (String iso : queryDates("SELECT DISTINCT date FROM purchases")) {
            try {
                dates.add(LocalDate.parse(iso));
            } catch (Exception ignored) {
            }
        }

        LocalDate rebuiltSelection = null;
        if (selected == null || !dates.contains(selected)) {
            rebuiltSelection = LocalDate.now();
        }

        if (rebuiltSelection != null || dateSelector.getItemCount() != dates.size()) {
            dateSelector.removeActionListener(dateListener);
            dateSelector.removeAllItems();
            for (LocalDate d : dates) dateSelector.addItem(d);
            dateSelector.setSelectedItem(rebuiltSelection != null ? rebuiltSelection : selected);
            dateSelector.addActionListener(dateListener);
        }

        LocalDate current = (LocalDate) dateSelector.getSelectedItem();
        if (current != null) updateCharts(current);
    }

    private void exportJson() {
        LocalDate[] range = promptRange();
        if (range == null) return;

        File target = JavaFxFileChooser.saveFile(
                "Export Daily Tracker to JSON",
                "dailytracker_" + DateUtils.toISO(range[0]) + "_" + DateUtils.toISO(range[1]) + ".json",
                new javafx.stage.FileChooser.ExtensionFilter("JSON file (*.json)", "*.json"));
        if (target == null) return;

        String selectedPath = target.getAbsolutePath();
        final String path = target.getName().toLowerCase().endsWith(".json") ? selectedPath : selectedPath + ".json";

        Window parent = SwingUtilities.getWindowAncestor(this);
        new Thread(() -> {
            try {
                String json = exportService.buildJson(range[0], range[1]);
                Files.writeString(new File(path).toPath(), json);
                AppLogger.info("DailyOverviewPanel: exported data to " + path);
                SwingUtilities.invokeLater(() ->
                        StyledDialog.show(parent, "Export completed:\n" + path, "Export JSON"));
            } catch (Exception ex) {
                AppLogger.error("DailyOverviewPanel: export failed - " + ex.getMessage());
                SwingUtilities.invokeLater(() ->
                        StyledDialog.show(parent, "Export failed: " + ex.getMessage(), "Export JSON"));
            }
        }, "daily-export").start();
    }

    private LocalDate[] promptRange() {
        LocalDate defaultFrom = dateSelector.getItemCount() > 0 ? dateSelector.getItemAt(0) : LocalDate.now();

        JTextField fromField = new JTextField(DateUtils.toDisplay(defaultFrom));
        JTextField toField = new JTextField(DateUtils.todayDisplay());
        UIStyle.styleTextField(fromField);
        UIStyle.styleTextField(toField);

        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 6));
        panel.setBackground(UIStyle.SIDE_BOX);

        JLabel fromLabel = new JLabel("From (dd.MM.yyyy):");
        fromLabel.setForeground(UIStyle.TEXT_COLOR);
        JLabel toLabel = new JLabel("To (dd.MM.yyyy):");
        toLabel.setForeground(UIStyle.TEXT_COLOR);

        panel.add(fromLabel);
        panel.add(fromField);
        panel.add(toLabel);
        panel.add(toField);

        boolean confirmed = StyledDialog.confirm(SwingUtilities.getWindowAncestor(this), panel, "Export date range");
        if (!confirmed) return null;

        LocalDate from;
        LocalDate to;
        try {
            from = DateUtils.parseDisplay(fromField.getText().trim());
            to = DateUtils.parseDisplay(toField.getText().trim());
        } catch (Exception e) {
            StyledDialog.show(SwingUtilities.getWindowAncestor(this), "Invalid date. Use format dd.MM.yyyy.");
            return null;
        }
        if (from.isAfter(to)) {
            StyledDialog.show(SwingUtilities.getWindowAncestor(this),
                    "'From' date must be before or equal to 'To' date.");
            return null;
        }
        return new LocalDate[]{from, to};
    }

    private List<String> queryDates(String sql) {
        List<String> dates = new ArrayList<>();
        try (Connection conn = nutritionRepo.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) dates.add(rs.getString(1));
        } catch (Exception e) {
            AppLogger.error("DailyOverviewPanel: failed to query dates - " + e.getMessage());
        }
        return dates;
    }

    private void updateCharts(LocalDate date) {
        new Thread(() -> {
            String iso = DateUtils.toISO(date);
            try {
                Map<String, Double> mealMap = new LinkedHashMap<>();
                int consumed = 0;
                for (FoodEntry f : nutritionRepo.listByDate(iso)) {
                    consumed += f.calories();
                    mealMap.merge(f.mealName(), (double) f.calories(), Double::sum);
                }
                Map<String, Double> sortedMeals = sortByValueDesc(mealMap);

                List<StepsEntry> stepEntries = stepsRepo.listByDate(iso);
                double[] stepVals = new double[stepEntries.size()];
                String[] stepLabels = new String[stepEntries.size()];
                int stepsTotal = 0;
                for (int i = 0; i < stepEntries.size(); i++) {
                    stepVals[i] = stepEntries.get(i).steps();
                    stepLabels[i] = String.valueOf(i + 1);
                    stepsTotal += stepEntries.get(i).steps();
                }
                int burned = stepsRepo.totalCalories(iso);

                Map<String, Double> curMap = new LinkedHashMap<>();
                List<String> currencies = purchasesRepo.getCurrenciesForDate(iso);
                for (String cur : currencies) {
                    curMap.merge(cur, purchasesRepo.totalForCurrency(iso, cur), Double::sum);
                }
                Map<String, Double> sortedCurs = sortByValueDesc(curMap);
                double[] curVals = new double[sortedCurs.size()];
                String[] curLabels = new String[sortedCurs.size()];
                int i = 0;
                for (Map.Entry<String, Double> e : sortedCurs.entrySet()) {
                    curVals[i] = e.getValue();
                    curLabels[i] = e.getKey();
                    i++;
                }

                int balance = consumed - burned;
                Double weight = stepsRepo.getWeight();

                StringBuilder purchaseTotals = new StringBuilder();
                for (Map.Entry<String, Double> e : sortedCurs.entrySet()) {
                    if (purchaseTotals.length() > 0) purchaseTotals.append("  ");
                    purchaseTotals.append(String.format("%.2f %s", e.getValue(), e.getKey()));
                }

                final int fConsumed = consumed;
                final int fBurned = burned;
                final int fBalance = balance;
                final int fSteps = stepsTotal;
                final String fPurchases = purchaseTotals.length() == 0 ? "0.00" : purchaseTotals.toString();
                final Double fWeight = weight;

                SwingUtilities.invokeLater(() -> {
                    mealPie.setData(sortedMeals);
                    stepsBar.setData("Steps per Record (" + DateUtils.toDisplay(date) + ")",
                            stepVals, stepLabels, v -> String.format("%.0f", v));
                    currencyBar.setData("Spending by Currency", curVals, curLabels,
                            v -> String.format("%.2f", v));
                    consumedVal.setText(fConsumed + " kcal");
                    burnedVal.setText(fBurned + " kcal");
                    balanceVal.setText((fBalance >= 0 ? "+" : "-") + Math.abs(fBalance) + " kcal");
                    stepsVal.setText(fSteps + " steps");
                    purchasesVal.setText(fPurchases);
                    weightVal.setText(fWeight != null ? String.format("%.1f kg", fWeight) : "—");
                });
            } catch (Exception e) {
                AppLogger.error("DailyOverviewPanel: failed to update charts - " + e.getMessage());
            }
        }).start();
    }

    private static Map<String, Double> sortByValueDesc(Map<String, Double> map) {
        Map<String, Double> sorted = new LinkedHashMap<>();
        map.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted;
    }
}
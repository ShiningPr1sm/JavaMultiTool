package ui.dailytracker;

import db.NutritionRepository;
import db.PurchasesRepository;
import db.StepsRepository;
import ui.UIStyle;
import util.AppLogger;
import util.DateUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class DailyOverviewPanel extends JPanel {
    private final NutritionRepository nutritionRepo;
    private final StepsRepository stepsRepo;
    private final PurchasesRepository purchasesRepo;
    private final JComboBox<LocalDate> dateSelector;
    private final JTextArea summaryArea;
    private final ActionListener dateListener;

    public DailyOverviewPanel(NutritionRepository nutritionRepo, StepsRepository stepsRepo, PurchasesRepository purchasesRepo) {
        this.nutritionRepo = nutritionRepo;
        this.stepsRepo = stepsRepo;
        this.purchasesRepo = purchasesRepo;

        setLayout(new BorderLayout(0, 10));
        setBackground(UIStyle.BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        top.setOpaque(false);
        JLabel dateLabel = new JLabel("Date:");
        dateLabel.setForeground(UIStyle.TEXT_COLOR);
        dateSelector = new JComboBox<>();
        UIStyle.styleComboBox(dateSelector);
        dateSelector.setPreferredSize(new Dimension(140, 28));
        dateSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof LocalDate d) {
                    lbl.setText(DateUtils.toDisplay(d));
                }
                return lbl;
            }
        });
        dateListener = e -> {
            LocalDate date = (LocalDate) dateSelector.getSelectedItem();
            if (date != null) updateSummary(date);
        };
        dateSelector.addActionListener(dateListener);
        JButton refreshBtn = new JButton("Refresh");
        UIStyle.styleButton(refreshBtn);
        refreshBtn.addActionListener(e -> refresh());
        top.add(dateLabel);
        top.add(dateSelector);
        top.add(refreshBtn);

        summaryArea = new JTextArea();
        summaryArea.setEditable(false);
        summaryArea.setBackground(UIStyle.SECONDARY_BG);
        summaryArea.setForeground(Color.WHITE);
        summaryArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        summaryArea.setMargin(new Insets(12, 12, 12, 12));
        JScrollPane scroll = new JScrollPane(summaryArea);
        scroll.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));
        UIStyle.styleScrollBar(scroll);

        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        refresh();
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
        if (current != null) updateSummary(current);
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

    private void updateSummary(LocalDate date) {
        String iso = DateUtils.toISO(date);
        int foodKcal = nutritionRepo.totalCalories(iso);
        int stepsCount = stepsRepo.totalSteps(iso);
        int stepsKcal = stepsRepo.totalCalories(iso);
        int balance = foodKcal - stepsKcal;

        StringBuilder sb = new StringBuilder();
        sb.append("Date: ").append(DateUtils.toDisplay(date)).append("\n\n");
        sb.append("=== Food / Calories ===\n");
        sb.append("  Consumed: ").append(foodKcal).append(" kcal\n\n");
        sb.append("=== Steps / Activity ===\n");
        sb.append("  Steps: ").append(stepsCount).append("\n");
        sb.append("  Burned: ").append(stepsKcal).append(" kcal\n\n");
        sb.append("=== Net balance ===\n");
        sb.append("  ").append(balance >= 0 ? "+" : "-").append(Math.abs(balance))
                .append(" kcal (food - activity)\n\n");
        sb.append("=== Purchases ===\n");

        try {
            List<String> currencies = purchasesRepo.getCurrenciesForDate(iso);
            if (currencies.isEmpty()) {
                sb.append("  No purchases for this date.\n");
            } else {
                for (String cur : currencies) {
                    sb.append("  ").append(String.format("%.2f", purchasesRepo.totalForCurrency(iso, cur)))
                            .append(" ").append(cur).append("\n");
                }
                sb.append("  (").append(purchasesRepo.listByDate(iso).size()).append(" item(s))\n");
            }
        } catch (Exception e) {
            AppLogger.error("DailyOverviewPanel: failed to compute purchases - " + e.getMessage());
            sb.append("  No purchases for this date.\n");
        }

        summaryArea.setText(sb.toString());
        summaryArea.setCaretPosition(0);
    }
}
package service;

import com.fasterxml.jackson.databind.ObjectMapper;

import db.FoodEntry;
import db.NutritionRepository;
import db.PurchaseEntry;
import db.PurchasesRepository;
import db.StepsEntry;
import db.StepsRepository;
import util.DateUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Builds a JSON export of the Daily Tracker data (calories, steps, purchases)
 * for a given inclusive date range, including per-day and overall summaries.
 */
public class DailyExportService {

    /** Refuse absurdly wide ranges so the per-day loop cannot hang the app. */
    private static final long MAX_RANGE_DAYS = 3660;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final NutritionRepository nutritionRepo;
    private final StepsRepository stepsRepo;
    private final PurchasesRepository purchasesRepo;

    public DailyExportService(NutritionRepository nutritionRepo,
                              StepsRepository stepsRepo,
                              PurchasesRepository purchasesRepo) {
        this.nutritionRepo = nutritionRepo;
        this.stepsRepo = stepsRepo;
        this.purchasesRepo = purchasesRepo;
    }

    public String buildJson(LocalDate from, LocalDate to) throws Exception {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'From' date must be before or equal to 'To' date.");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("Date range is too large (max " + MAX_RANGE_DAYS + " days).");
        }

        List<Map<String, Object>> days = new ArrayList<>();
        int totalConsumed = 0;
        int totalBurned = 0;
        int totalSteps = 0;
        double totalSpent = 0;
        Map<String, Double> spentByCurrency = new TreeMap<>();

        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            String iso = DateUtils.toISO(d);
            List<FoodEntry> food = nutritionRepo.listByDate(iso);
            List<StepsEntry> steps = stepsRepo.listByDate(iso);
            List<PurchaseEntry> purchases = purchasesRepo.listByDate(iso);
            if (food.isEmpty() && steps.isEmpty() && purchases.isEmpty()) continue;

            int consumed = 0;
            List<Map<String, Object>> foodEntries = new ArrayList<>();
            for (FoodEntry f : food) {
                consumed += f.calories();
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", f.id());
                entry.put("meal", f.mealName());
                entry.put("calories", f.calories());
                foodEntries.add(entry);
            }

            int stepsSum = 0;
            int burned = 0;
            List<Map<String, Object>> stepEntries = new ArrayList<>();
            for (StepsEntry s : steps) {
                stepsSum += s.steps();
                burned += s.calories();
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", s.id());
                entry.put("steps", s.steps());
                entry.put("calories", s.calories());
                entry.put("calcMode", s.calcMode());
                entry.put("weightUsed", s.weightUsed());
                stepEntries.add(entry);
            }

            double spent = 0;
            Map<String, Double> daySpentByCurrency = new TreeMap<>();
            List<Map<String, Object>> purchaseEntries = new ArrayList<>();
            for (PurchaseEntry p : purchases) {
                spent += p.price();
                daySpentByCurrency.merge(p.currency(), p.price(), Double::sum);
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", p.id());
                entry.put("item", p.itemName());
                entry.put("price", round2(p.price()));
                entry.put("currency", p.currency());
                purchaseEntries.add(entry);
            }

            Map<String, Object> calories = new LinkedHashMap<>();
            calories.put("entries", foodEntries);
            calories.put("totalConsumed", consumed);

            Map<String, Object> stepsBlock = new LinkedHashMap<>();
            stepsBlock.put("entries", stepEntries);
            stepsBlock.put("totalSteps", stepsSum);
            stepsBlock.put("totalBurnedCalories", burned);

            Map<String, Object> purchasesBlock = new LinkedHashMap<>();
            purchasesBlock.put("entries", purchaseEntries);
            purchasesBlock.put("totalSpent", round2(spent));
            purchasesBlock.put("spentByCurrency", rounded(daySpentByCurrency));

            Map<String, Object> daySummary = new LinkedHashMap<>();
            daySummary.put("consumedCalories", consumed);
            daySummary.put("burnedCalories", burned);
            daySummary.put("netBalance", consumed - burned);
            daySummary.put("steps", stepsSum);
            daySummary.put("spent", round2(spent));
            daySummary.put("spentByCurrency", rounded(daySpentByCurrency));

            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", iso);
            day.put("calories", calories);
            day.put("steps", stepsBlock);
            day.put("purchases", purchasesBlock);
            day.put("summary", daySummary);
            days.add(day);

            totalConsumed += consumed;
            totalBurned += burned;
            totalSteps += stepsSum;
            totalSpent += spent;
            daySpentByCurrency.forEach((cur, v) -> spentByCurrency.merge(cur, v, Double::sum));
        }

        Map<String, Object> range = new LinkedHashMap<>();
        range.put("from", DateUtils.toISO(from));
        range.put("to", DateUtils.toISO(to));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("days", days.size());
        summary.put("consumedCalories", totalConsumed);
        summary.put("burnedCalories", totalBurned);
        summary.put("netBalance", totalConsumed - totalBurned);
        summary.put("steps", totalSteps);
        summary.put("spent", round2(totalSpent));
        summary.put("spentByCurrency", rounded(spentByCurrency));

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("exportedAt", LocalDateTime.now().withNano(0).toString());
        root.put("range", range);
        root.put("days", days);
        root.put("summary", summary);

        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    private static Map<String, Double> rounded(Map<String, Double> values) {
        Map<String, Double> result = new LinkedHashMap<>();
        values.forEach((key, value) -> result.put(key, round2(value)));
        return result;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

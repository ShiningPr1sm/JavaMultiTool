package service;

import db.BirthdayRecord;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BDaysService {

    private static final DateTimeFormatter STORAGE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public static class BDayEntry {
        public final String name;
        public final LocalDate originalDate;
        public final LocalDate candidate;
        public final boolean expired;
        public final boolean yearUnknown;
        public final int ageThisYear;

        public BDayEntry(String name, LocalDate originalDate, LocalDate candidate,
                         boolean expired, boolean yearUnknown, int ageThisYear) {
            this.name = name;
            this.originalDate = originalDate;
            this.candidate = candidate;
            this.expired = expired;
            this.yearUnknown = yearUnknown;
            this.ageThisYear = ageThisYear;
        }
    }

    public String uiToDb(String uiDate) throws Exception {
        if (uiDate.toLowerCase().endsWith(".xxxx")) {
            String[] parts = uiDate.split("\\.");
            if (parts.length < 2) throw new Exception("Invalid format");
            String day = parts[0];
            String month = parts[1];
            return String.format("0000-%s-%s", month, day);
        }
        LocalDate date = LocalDate.parse(uiDate, DISPLAY_FORMAT);
        return date.toString();
    }

    public String dbToUi(String dbDate) {
        if (dbDate.startsWith("0000-")) {
            String[] parts = dbDate.split("-");
            return parts[2] + "." + parts[1] + ".xxxx";
        }
        return LocalDate.parse(dbDate, STORAGE_FORMAT).format(DISPLAY_FORMAT);
    }

    public List<BDayEntry> loadEntries(List<BirthdayRecord> records, String mode, LocalDate today) {
        List<BDayEntry> entries = new ArrayList<>();
        boolean upcoming = "Upcoming".equals(mode);

        for (BirthdayRecord rec : records) {
            String name = rec.getName();
            String dbDateStr = rec.getBdayDate();

            int year = Integer.parseInt(dbDateStr.substring(0, 4));
            int month = Integer.parseInt(dbDateStr.substring(5, 7));
            int day = Integer.parseInt(dbDateStr.substring(8, 10));

            boolean yearUnknown = (year == 0);
            LocalDate candidate = LocalDate.of(today.getYear(), month, day);
            boolean expired = upcoming && candidate.isBefore(today);

            LocalDate originalDate = yearUnknown ? null : LocalDate.of(year, month, day);
            int ageThisYear = yearUnknown ? -1 : candidate.getYear() - originalDate.getYear();

            entries.add(new BDayEntry(name, originalDate, candidate, expired, yearUnknown, ageThisYear));
        }

        return entries;
    }

    public Map<Integer, List<BDayEntry>> groupByMonth(List<BDayEntry> entries) {
        Map<Integer, List<BDayEntry>> monthMap = new TreeMap<>();
        for (BDayEntry entry : entries) {
            monthMap.computeIfAbsent(entry.candidate.getMonthValue(), e -> new ArrayList<>()).add(entry);
        }
        return monthMap;
    }

    public List<Integer> getMonthOrder(String mode, int startMonth) {
        List<Integer> order = new ArrayList<>();
        if ("Upcoming".equals(mode)) {
            for (int i = 0; i < 12; i++) {
                order.add((startMonth + i - 1) % 12 + 1);
            }
        } else if ("Reverse List".equals(mode)) {
            for (int i = 12; i >= 1; i--) order.add(i);
        } else {
            for (int i = 1; i <= 12; i++) order.add(i);
        }
        return order;
    }

    public void sortEntries(List<BDayEntry> entries, String mode) {
        entries.sort(Comparator.comparing(o -> o.candidate.getDayOfMonth()));
        if ("Reverse List".equals(mode)) Collections.reverse(entries);
    }

    public String getSeasonName(int month) {
        if (month >= 3 && month <= 5) return "Spring";
        if (month >= 6 && month <= 8) return "Summer";
        if (month >= 9 && month <= 11) return "Autumn";
        return "Winter";
    }
}

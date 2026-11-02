package ui.components;

import db.BDaysRepository;
import service.BDaysService;
import ui.UIStyle;
import util.AppLogger;
import util.I18n;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class BDaysOverviewPanel extends JPanel {
    private final JPanel overviewContainer = new JPanel();
    private final BDaysRepository repo;
    private final BDaysService bdaysService;
    private final JComboBox<String> modeSelector;
    private String currentMode;

    public BDaysOverviewPanel(BDaysRepository repo, BDaysService bdaysService) {
        this.repo = repo;
        this.bdaysService = bdaysService;
        setLayout(new BorderLayout());
        setBackground(UIStyle.BG_COLOR);

        modeSelector = new JComboBox<>(new String[]{"Upcoming", "List", "Reverse List"});
        // identity values stay EN (compared with equals); only display is translated
        modeSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, modeOrNull((String) value), index, isSelected, cellHasFocus);
                return this;
            }
        });
        UIStyle.styleComboBox(modeSelector);
        modeSelector.addActionListener(e -> refresh((String) modeSelector.getSelectedItem()));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.setBackground(UIStyle.BG_COLOR);

        JLabel modeLabel = new JLabel(I18n.t("bday.mode.label"));
        modeLabel.setForeground(UIStyle.TEXT_COLOR);
        modeLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        top.add(modeLabel);
        top.add(modeSelector);

        overviewContainer.setLayout(new BoxLayout(overviewContainer, BoxLayout.Y_AXIS));
        overviewContainer.setBackground(UIStyle.BG_COLOR);
        overviewContainer.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JScrollPane sp = new JScrollPane(overviewContainer);
        sp.setBackground(UIStyle.BG_COLOR);
        sp.setBorder(null);
        sp.setViewportBorder(null);
        sp.getViewport().setBackground(UIStyle.BG_COLOR);
        UIStyle.styleScrollBar(sp);

        add(top, BorderLayout.NORTH);
        add(sp, BorderLayout.CENTER);

        refresh("Upcoming");
    }

    public void refresh() {
        refresh(currentMode);
    }

    public void refresh(String mode) {
        currentMode = mode;
        overviewContainer.removeAll();
        boolean upcoming = "Upcoming".equals(mode);
        LocalDate today = LocalDate.now();

        List<BDaysService.BDayEntry> entries;
        try {
            entries = bdaysService.loadEntries(repo.getAllBirthdays(), mode, today);
        } catch (Exception e) {
            AppLogger.error("BDaysOverviewPanel: failed to load entries: " + e.getMessage());
            overviewContainer.revalidate();
            overviewContainer.repaint();
            return;
        }

        var monthMap = bdaysService.groupByMonth(entries);
        List<Integer> monthOrder = bdaysService.getMonthOrder(mode, today.getMonthValue());

        String lastSeason = "";

        for (int monthNum : monthOrder) {
            List<BDaysService.BDayEntry> monthEntries = monthMap.get(monthNum);
            if (monthEntries == null || monthEntries.isEmpty()) continue;

            String currentSeason = bdaysService.getSeasonName(monthNum);
            if (!currentSeason.equals(lastSeason)) {
                addSeasonHeader(currentSeason);
                lastSeason = currentSeason;
            }

            String mn = Month.of(monthNum).getDisplayName(TextStyle.FULL, I18n.locale())
                    .toUpperCase(I18n.locale());
            JLabel ml = new JLabel("      " + mn);
            ml.setForeground(getSeasonColor(currentSeason));
            ml.setFont(ml.getFont().deriveFont(Font.BOLD, 14f));
            overviewContainer.add(ml);

            bdaysService.sortEntries(monthEntries, mode);

            for (BDaysService.BDayEntry entry : monthEntries) {
                String ageText;
                if (!entry.yearUnknown) {
                    ageText = entry.expired
                            ? I18n.t("bday.age.already", String.valueOf(entry.ageThisYear))
                            : I18n.t("bday.age.turns", String.valueOf(entry.ageThisYear));
                } else {
                    ageText = I18n.t("bday.age.unknown");
                }

                String dateShow = entry.candidate.format(DateTimeFormatter.ofPattern("dd.MM"))
                        + (entry.yearUnknown ? ".xxxx" : "." + entry.originalDate.getYear());
                JLabel lbl = new JLabel("     \u2014 " + entry.name + " \u2014 " + dateShow + " " + ageText);

                boolean isToday = entry.candidate.equals(today);

                if (isToday) {
                    lbl.setForeground(UIStyle.ACCENT_COLOR);
                    lbl.setText(lbl.getText() + I18n.t("bday.today"));
                    lbl.setFont(lbl.getFont().deriveFont(15f));
                } else {
                    lbl.setForeground(upcoming && entry.expired ? Color.GRAY : Color.WHITE);
                    lbl.setFont(lbl.getFont().deriveFont(15f));
                }
                overviewContainer.add(lbl);
            }
        }

        overviewContainer.revalidate();
        overviewContainer.repaint();
    }

    private void addSeasonHeader(String season) {
        String seasonKey = seasonKey(season);
        JLabel sl = new JLabel("   " + (seasonKey.equals(season) ? season : I18n.t(seasonKey))
                + "  ----------------------------------------------");
        sl.setForeground(getSeasonColor(season));
        sl.setFont(sl.getFont().deriveFont(Font.BOLD, 16f));
        overviewContainer.add(sl);
    }

    /** Identity season name (EN, from BDaysService) -> i18n key. Also used for combobox modes below. */
    private static String seasonKey(String season) {
        return switch (season) {
            case "Winter" -> "season.winter";
            case "Spring" -> "season.spring";
            case "Summer" -> "season.summer";
            case "Autumn" -> "season.autumn";
            default -> season;
        };
    }

    /** Translates mapped keys; unmapped values render as-is, never passed to {@link I18n#t}. */
    private static String modeOrNull(String mode) {
        String key = modeKey(mode);
        return key.equals(mode) ? mode : I18n.t(key);
    }

    /** Mode identity ("Upcoming"/"List"/"Reverse List") -> i18n key. */
    private static String modeKey(String mode) {
        return switch (mode) {
            case "Upcoming" -> "bday.mode.upcoming";
            case "List" -> "bday.mode.list";
            case "Reverse List" -> "bday.mode.reverse";
            default -> mode;
        };
    }

    private Color getSeasonColor(String season) {
        return switch (season) {
            case "Winter" -> new Color(64, 224, 208);
            case "Spring" -> new Color(100, 200, 100);
            case "Summer" -> new Color(255, 215, 0);
            case "Autumn" -> new Color(255, 140, 0);
            default -> Color.WHITE;
        };
    }
}

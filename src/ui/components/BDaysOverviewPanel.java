package ui.components;

import db.BDaysRepository;
import service.BDaysService;
import ui.UIStyle;

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

    public BDaysOverviewPanel(BDaysRepository repo, BDaysService bdaysService) {
        this.repo = repo;
        this.bdaysService = bdaysService;
        setLayout(new BorderLayout());
        setBackground(UIStyle.BG_COLOR);

        overviewContainer.setLayout(new BoxLayout(overviewContainer, BoxLayout.Y_AXIS));
        overviewContainer.setBackground(UIStyle.BG_COLOR);
        overviewContainer.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JScrollPane sp = new JScrollPane(overviewContainer);
        sp.setBackground(UIStyle.BG_COLOR);
        sp.setBorder(null);
        sp.setViewportBorder(null);
        sp.getViewport().setBackground(UIStyle.BG_COLOR);
        sp.getVerticalScrollBar().setUnitIncrement(20);
        styleScrollBar(sp);
        add(sp, BorderLayout.CENTER);
    }

    public void refresh(String mode) {
        overviewContainer.removeAll();
        boolean upcoming = "Upcoming".equals(mode);
        LocalDate today = LocalDate.now();

        List<BDaysService.BDayEntry> entries;
        try {
            entries = bdaysService.loadEntries(repo.getAllBirthdays(), mode, today);
        } catch (Exception e) {
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

            String mn = Month.of(monthNum).getDisplayName(TextStyle.FULL, Locale.getDefault()).toUpperCase();
            JLabel ml = new JLabel("      " + mn);
            ml.setForeground(getSeasonColor(currentSeason));
            ml.setFont(ml.getFont().deriveFont(Font.BOLD, 14f));
            overviewContainer.add(ml);

            bdaysService.sortEntries(monthEntries, mode);

            for (BDaysService.BDayEntry entry : monthEntries) {
                String ageText;
                if (!entry.yearUnknown) {
                    ageText = entry.expired
                            ? "(already " + entry.ageThisYear + " y.o.)"
                            : "(will be " + entry.ageThisYear + " y.o.)";
                } else {
                    ageText = "(age unknown)";
                }

                String dateShow = entry.candidate.format(DateTimeFormatter.ofPattern("dd.MM"))
                        + (entry.yearUnknown ? ".xxxx" : "." + entry.originalDate.getYear());
                JLabel lbl = new JLabel("     \u2014 " + entry.name + " \u2014 " + dateShow + " " + ageText);

                boolean isToday = entry.candidate.equals(today);

                if (isToday) {
                    lbl.setForeground(UIStyle.ACCENT_COLOR);
                    lbl.setText(lbl.getText() + "  [\uD83C\uDF89 TODAY!]");
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
        JLabel sl = new JLabel("   " + season + "  ----------------------------------------------");
        sl.setForeground(getSeasonColor(season));
        sl.setFont(sl.getFont().deriveFont(Font.BOLD, 16f));
        overviewContainer.add(sl);
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

    private void styleScrollBar(JScrollPane sp) {
        JScrollBar vBar = sp.getVerticalScrollBar();
        vBar.setPreferredSize(new Dimension(8, 0));
        vBar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = UIStyle.BORDER_COLOR;
                trackColor = UIStyle.BG_COLOR;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(thumbColor);
                g2.fillRoundRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height, 8, 8);
                g2.dispose();
            }
        });
    }
}

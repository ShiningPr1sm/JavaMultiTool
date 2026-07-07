package ui.daytab;

import ui.utils.AppLogger;

import db.AchievementDB;
import db.BDaysDB;
import ui.UIStyle;
import ui.utils.UserSession;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
import java.util.Locale;

public class BDaysNotifierPanel extends JPanel {
    private static final String EDIT_CARD = "EDIT";
    private static final String OVERVIEW_CARD = "OVERVIEW";
    private static final DateTimeFormatter STORAGE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private DefaultTableModel model;
    private JTable table;
    private final JPanel cards;
    private final JComboBox<String> modeSelector;
    private JPanel overviewContainer;

    public BDaysNotifierPanel() {
        setLayout(new BorderLayout());
        setBackground(UIStyle.BG_COLOR);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.setBackground(UIStyle.BG_COLOR);

        modeSelector = new JComboBox<>(new String[]{"Upcoming", "List", "Reverse List"});
        UIStyle.styleComboBox(modeSelector);
        top.add(modeSelector);

        JButton overviewBtn = new JButton("Overview");
        JButton editBtn     = new JButton("Edit");
        UIStyle.styleButton(overviewBtn);
        UIStyle.styleButton(editBtn);
        top.add(overviewBtn);
        top.add(editBtn);
        add(top, BorderLayout.NORTH);

        cards = new JPanel(new CardLayout());
        cards.setBackground(UIStyle.BG_COLOR);
        cards.add(createEditPanel(), EDIT_CARD);
        cards.add(createOverviewPanel(), OVERVIEW_CARD);
        add(cards, BorderLayout.CENTER);

        overviewBtn.addActionListener(_ -> switchCard(OVERVIEW_CARD));
        editBtn.addActionListener(_ -> switchCard(EDIT_CARD));
        modeSelector.addActionListener(_ -> {
            if (OVERVIEW_CARD.equals(getCurrentCard())) {
                refreshOverview();
            }
        });
        switchCard(OVERVIEW_CARD);
    }

    private void switchCard(String card) {
        CardLayout cl = (CardLayout) cards.getLayout();
        if (OVERVIEW_CARD.equals(card)) {
            refreshOverview();
        } else {
            refreshTable();
        }
        cl.show(cards, card);
    }

    private String getCurrentCard() {
        for (Component comp : cards.getComponents()) {
            if (comp.isVisible()) {
                return comp.getName();
            }
        }
        return EDIT_CARD;
    }

    private JPanel createEditPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setName(EDIT_CARD);
        panel.setBackground(UIStyle.BG_COLOR);

        model = new DefaultTableModel(new String[]{"ID", "Name", "Birthday"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 0;
            }
        };
        model.addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int id = (int) model.getValueAt(row, 0);
                String name = model.getValueAt(row, 1).toString();
                String dateStr = model.getValueAt(row, 2).toString();
                try {
                    String dbDate = uiToDb(dateStr);
                    BDaysDB.updateBirthday(id, name, dbDate);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Invalid format. Use DD.MM.YYYY or DD.MM.xxxx");
                    refreshTable();
                }
            }
        });

        table = new JTable(model);
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(tbl, row + 1, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                return label;
            }
        });
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(0).setMaxWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(250);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setFillsViewportHeight(true);
        table.setBackground(UIStyle.BG_COLOR);
        table.setForeground(Color.WHITE);
        table.setGridColor(UIStyle.BORDER_COLOR);
        table.setFont(table.getFont().deriveFont(15f));

        JTableHeader header = table.getTableHeader();
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val,
                                                           boolean isSel, boolean hasFocus,
                                                           int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, val, isSel, hasFocus, row, col);
                lbl.setBackground(UIStyle.BUTTON_BG);
                lbl.setForeground(Color.WHITE);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                return lbl;
            }
        });
        header.setOpaque(true);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));
        scroll.getViewport().setBackground(UIStyle.BG_COLOR);
        panel.add(scroll, BorderLayout.CENTER);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        form.setBackground(UIStyle.HEADER_COLOR);
        JTextField nameField = new JTextField(10);
        JTextField dateField = new JTextField(8);
        dateField.setToolTipText("Format: dd.MM.yyyy or dd.MM.xxxx");
        dateField.setColumns(8);
        JButton addBtn = new JButton("Add");
        JButton removeBtn = new JButton("Remove");

        UIStyle.styleButton(addBtn);
        UIStyle.styleButton(removeBtn);

        addBtn.setForeground(new Color(150, 255, 150));
        removeBtn.setForeground(new Color(255, 150, 150));

        addBtn.addActionListener(_ -> {
            try {
                String inputDate = dateField.getText().trim();
                String name = nameField.getText().trim();

                if (name.isEmpty() || inputDate.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please fill all fields");
                    return;
                }
                String dbDate = uiToDb(inputDate);
                BDaysDB.addBirthday(name, dbDate);

                AchievementDB.completeAchievement(UserSession.getLogin(), "real_friend");
                refreshTable();
                dateField.setText("");
                nameField.setText("");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid format. Use dd.MM.yyyy or dd.MM.xxxx", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        removeBtn.addActionListener(_ -> {
            int sel = table.getSelectedRow();
            if (sel >= 0) {
                int id = (int) model.getValueAt(sel, 0);
                BDaysDB.removeBirthday(id);
                refreshTable();
            }
        });
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setForeground(Color.WHITE);
        form.add(nameLabel);
        form.add(nameField);
        JLabel dateLabel = new JLabel("Date (dd.MM.yyyy):");
        dateLabel.setForeground(Color.WHITE);
        form.add(dateLabel);
        form.add(dateField);
        form.add(addBtn); form.add(removeBtn);
        panel.add(form, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setName(OVERVIEW_CARD);
        panel.setBackground(UIStyle.BG_COLOR);

        overviewContainer = new JPanel();
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
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }

    private void styleScrollBar(JScrollPane sp) {
        JScrollBar vBar = sp.getVerticalScrollBar();
        vBar.setPreferredSize(new Dimension(8, 0));
        vBar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = UIStyle.BORDER_COLOR;
                this.trackColor = UIStyle.BG_COLOR;
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

    private void refreshTable() {
        model.setRowCount(0);
        try (var rs = BDaysDB.getAllBirthdays()) {
            while (rs.next()) {
                int realDbId = rs.getInt("id");
                String name = rs.getString("name");
                String dbDate = rs.getString("bday_date");

                model.addRow(new Object[]{
                        realDbId,
                        name,
                        dbToUi(dbDate)
                });
            }
        } catch (Exception e) {
            AppLogger.error("Error: " + e.getMessage());
        }
    }

    private void refreshOverview() {
        overviewContainer.removeAll();
        LocalDate today = LocalDate.now();
        String mode = (String) modeSelector.getSelectedItem();
        boolean upcoming = "Upcoming".equals(mode);

        Map<Integer, List<Object[]>> monthMap = new TreeMap<>();

        try (var rs = BDaysDB.getAllBirthdays()) {
            while (rs.next()) {
                String name = rs.getString("name");
                String dbDateStr = rs.getString("bday_date");

                int year = Integer.parseInt(dbDateStr.substring(0, 4));
                int month = Integer.parseInt(dbDateStr.substring(5, 7));
                int day = Integer.parseInt(dbDateStr.substring(8, 10));

                boolean yearUnknown = (year == 0);
                LocalDate candidate = LocalDate.of(today.getYear(), month, day);
                boolean expired = upcoming && candidate.isBefore(today);

                monthMap.computeIfAbsent(month, _ -> new ArrayList<>())
                        .add(new Object[]{name, yearUnknown ? null : LocalDate.of(year, month, day), candidate, expired, yearUnknown});
            }
        } catch (Exception e) {
            AppLogger.error("Error" + e.getMessage());
        }

        List<Integer> monthOrder = new ArrayList<>();
        if ("Upcoming".equals(mode)) {
            int startMonth = today.getMonthValue();
            for (int i = 0; i < 12; i++) {
                monthOrder.add((startMonth + i - 1) % 12 + 1);
            }
        } else if ("Reverse List".equals(mode)) {
            for (int i = 12; i >= 1; i--) monthOrder.add(i);
        } else {
            for (int i = 1; i <= 12; i++) monthOrder.add(i);
        }

        String lastSeason = "";

        for (int monthNum : monthOrder) {
            List<Object[]> entries = monthMap.get(monthNum);
            if (entries == null || entries.isEmpty()) continue;

            String currentSeason = getSeasonName(monthNum);
            if (!currentSeason.equals(lastSeason)) {
                addSeasonHeader(currentSeason);
                lastSeason = currentSeason;
            }

            String mn = Month.of(monthNum).getDisplayName(TextStyle.FULL, Locale.getDefault()).toUpperCase();
            JLabel ml = new JLabel("      " + mn);
            ml.setForeground(getSeasonColor(currentSeason));
            ml.setFont(ml.getFont().deriveFont(Font.BOLD, 14f));
            overviewContainer.add(ml);

            entries.sort(Comparator.comparing(o -> ((LocalDate) o[2]).getDayOfMonth()));
            if ("Reverse List".equals(mode)) Collections.reverse(entries);

            for (Object[] entry : entries) {
                String name = (String) entry[0];
                LocalDate originalBD = (LocalDate) entry[1];
                LocalDate candidate = (LocalDate) entry[2];
                boolean expired = (boolean) entry[3];
                boolean yearUnknown = (boolean) entry[4];

                String ageText;
                if (!yearUnknown) {
                    int ageThisYear = candidate.getYear() - originalBD.getYear();

                    if (expired) {
                        ageText = "(already " + ageThisYear + " y.o.)";
                    } else {
                        ageText = "(will be " + ageThisYear + " y.o.)";
                    }
                } else {
                    ageText = "(age unknown)";
                }
                String dateShow = candidate.format(DateTimeFormatter.ofPattern("dd.MM")) + (yearUnknown ? ".xxxx" : "." + originalBD.getYear());
                JLabel lbl = new JLabel("     — " + name + " — " + dateShow + " " + ageText);

                boolean isToday = candidate.equals(today);

                if (isToday) {
                    lbl.setForeground(UIStyle.ACCENT_COLOR);
                    lbl.setText(lbl.getText() + "  [🎉 TODAY!]");
                    lbl.setFont(lbl.getFont().deriveFont(15f));
                } else {
                    lbl.setForeground(upcoming && expired ? Color.GRAY : Color.WHITE);
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

    private String getSeasonName(int month) {
        if (month >= 3 && month <= 5) return "Spring";
        if (month >= 6 && month <= 8) return "Summer";
        if (month >= 9 && month <= 11) return "Autumn";
        return "Winter";
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

    private String uiToDb(String uiDate) throws Exception {
        if (uiDate.toLowerCase().endsWith(".xxxx")) {
            String[] parts = uiDate.split("\\.");
            if (parts.length < 2) throw new Exception("Invalid format");
            String day = parts[0];
            String month = parts[1];
            // ISO: 0000-MM-DD
            return String.format("0000-%s-%s", month, day);
        }
        LocalDate date = LocalDate.parse(uiDate, DISPLAY_FORMAT);
        return date.toString(); // YYYY-MM-DD
    }

    private String dbToUi(String dbDate) {
        if (dbDate.startsWith("0000-")) {
            // 0000-05-20 -> 20.05.xxxx
            String[] parts = dbDate.split("-");
            return parts[2] + "." + parts[1] + ".xxxx";
        }
        return LocalDate.parse(dbDate, STORAGE_FORMAT).format(DISPLAY_FORMAT);
    }
}

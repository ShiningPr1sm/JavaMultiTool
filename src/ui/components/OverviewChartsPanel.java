package ui.components;

import db.StatResult;
import db.WorkflowRepository;
import ui.UIStyle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OverviewChartsPanel extends JPanel {

    private final WorkflowRepository workflowRepo;
    private final JComboBox<String> dateSelector = new JComboBox<>();
    private final JComboBox<String> appFilter = new JComboBox<>();
    private final PieChartPanel pieChart = new PieChartPanel();
    private final BarChartPanel hourChart = new BarChartPanel();
    private final BarChartPanel weekChart = new BarChartPanel();
    private final BarChartPanel monthChart = new BarChartPanel();

    public OverviewChartsPanel(WorkflowRepository workflowRepo) {
        this.workflowRepo = workflowRepo;

        setLayout(new BorderLayout());
        setBackground(UIStyle.BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        top.setBackground(UIStyle.BG_COLOR);

        UIStyle.styleComboBox(dateSelector);
        refreshDateSelector();
        dateSelector.setPreferredSize(new Dimension(120, 30));
        dateSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Object display = value;
                if (value instanceof String raw) {
                    try {
                        display = LocalDate.parse(raw).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                    } catch (Exception ignored) {}
                }
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

        UIStyle.styleComboBox(appFilter);
        appFilter.setPreferredSize(new Dimension(150, 30));
        refreshAppFilter();

        ActionListener autoUpdateListener = e -> {
            String selectedDate = (String) dateSelector.getSelectedItem();
            String selectedApp = (String) appFilter.getSelectedItem();
            if (selectedDate != null && selectedApp != null) {
                updateCharts(selectedDate, selectedApp);
            }
        };

        dateSelector.addActionListener(autoUpdateListener);
        appFilter.addActionListener(autoUpdateListener);

        top.add(new JLabel("<html><b style='color:white'>Date:</b></html>"));
        top.add(dateSelector);
        top.add(new JLabel("<html><b style='color:white'>App:</b></html>"));
        top.add(appFilter);

        JPanel chartsPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        chartsPanel.setBackground(UIStyle.BG_COLOR);

        pieChart.setBackground(UIStyle.SECONDARY_BG);
        hourChart.setBackground(UIStyle.SECONDARY_BG);
        weekChart.setBackground(UIStyle.SECONDARY_BG);
        monthChart.setBackground(UIStyle.SECONDARY_BG);

        chartsPanel.add(pieChart);
        chartsPanel.add(hourChart);
        chartsPanel.add(weekChart);
        chartsPanel.add(monthChart);

        add(top, BorderLayout.NORTH);
        add(chartsPanel, BorderLayout.CENTER);
    }

    public void refresh() {
        refreshDateSelector();
        refreshAppFilter();
        String selectedDate = (String) dateSelector.getSelectedItem();
        String selectedApp = (String) appFilter.getSelectedItem();
        if (selectedDate != null) {
            updateCharts(selectedDate, selectedApp != null ? selectedApp : "ALL");
        }
    }

    private void refreshDateSelector() {
        dateSelector.removeAllItems();
        for (String d : workflowRepo.getAvailableDates())
            dateSelector.addItem(d);
    }

    private void refreshAppFilter() {
        appFilter.removeAllItems();
        appFilter.addItem("ALL");
        List<Object[]> apps = workflowRepo.getTrackedAppsFull();
        for (Object[] app : apps) {
            appFilter.addItem((String) app[1]);
        }
        appFilter.setSelectedItem("ALL");
    }

    private void updateCharts(String date, String filter) {
        new Thread(() -> {
            int[] hVal = workflowRepo.getHourlyStats(date, filter);
            String[] hLab = new String[24];
            for (int i = 0; i < 24; i++)
                hLab[i] = "Time: " + i + ":00";
            StatResult wData = workflowRepo.getPeriodStats(date, filter, 7);
            String[] dayNames = new String[7];
            for (int i = 0; i < 7; i++) {
                LocalDate d = LocalDate.parse(wData.labels[i]);
                dayNames[i] = d.getDayOfWeek()
                        .getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)
                        .toUpperCase();
            }
            StatResult mData = workflowRepo.getCalendarMonthStats(date, filter);
            SwingUtilities.invokeLater(() -> {
                hourChart.setData("Hourly Distribution (" + date + ")", hVal, hLab);
                weekChart.setData("Weekly Activity", wData.values, dayNames);
                monthChart.setData("Monthly Activity", mData.values, mData.labels);
                pieChart.setData(workflowRepo.getDaySummary(date));
            });
        }).start();
    }
}

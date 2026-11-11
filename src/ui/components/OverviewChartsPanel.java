package ui.components;

import db.StatResult;
import db.WorkflowRepository;
import ui.UIStyle;
import util.AppLogger;
import util.I18n;
import util.JavaFxFileChooser;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OverviewChartsPanel extends JPanel {

    private final WorkflowRepository workflowRepo;
    private final JComboBox<String> dateSelector = new JComboBox<>();
    private final JComboBox<String> appFilter = new JComboBox<>();
    private final PieChartPanel pieChart =
            new PieChartPanel(I18n.t("charts.appUsage"), I18n.t("charts.noDataDay"),
                    PieChartPanel::formatFullTime, 280, 40);
    private final BarChartPanel hourChart = new BarChartPanel();
    private final BarChartPanel weekChart = new BarChartPanel();
    private final BarChartPanel monthChart = new BarChartPanel();

    public OverviewChartsPanel(WorkflowRepository workflowRepo) {
        this.workflowRepo = workflowRepo;

        setLayout(new BorderLayout());
        setBackground(UIStyle.BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setBackground(UIStyle.BG_COLOR);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        filterPanel.setBackground(UIStyle.BG_COLOR);

        UIStyle.styleComboBox(dateSelector);
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

        filterPanel.add(new JLabel("<html><b style='color:white'>" + I18n.t("charts.date") + "</b></html>"));
        filterPanel.add(dateSelector);
        filterPanel.add(new JLabel("<html><b style='color:white'>" + I18n.t("charts.app") + "</b></html>"));
        filterPanel.add(appFilter);

        JButton refreshBtn = new JButton(I18n.t("charts.refreshData"));
        UIStyle.styleButton(refreshBtn);
        refreshBtn.addActionListener(e -> refresh());

        JButton savePngBtn = new JButton(I18n.t("charts.saveDataAs"));
        UIStyle.styleButton(savePngBtn);
        savePngBtn.addActionListener(e -> saveAsPng());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnPanel.setBackground(UIStyle.BG_COLOR);
        btnPanel.add(refreshBtn);
        btnPanel.add(savePngBtn);

        top.add(filterPanel, BorderLayout.WEST);
        top.add(btnPanel, BorderLayout.EAST);

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

        refresh();
    }

    private static final String[][] IMG_FORMATS = {
            {"PNG", "png"},
            {"JPEG", "jpg"},
            {"BMP", "bmp"},
            {"GIF", "gif"},
            {"TIFF", "tiff"},
    };

    public void saveAsPng() {
        javafx.stage.FileChooser.ExtensionFilter[] filters =
                new javafx.stage.FileChooser.ExtensionFilter[IMG_FORMATS.length];
        for (int i = 0; i < IMG_FORMATS.length; i++) {
            filters[i] = new javafx.stage.FileChooser.ExtensionFilter(
                    I18n.t("charts.imgFilter", IMG_FORMATS[i][0], IMG_FORMATS[i][1]), "*." + IMG_FORMATS[i][1]);
        }

        File file = JavaFxFileChooser.saveFile(I18n.t("charts.saveTitle"), "overview.png", filters);
        if (file == null) return;

        String name = file.getName().toLowerCase();
        String ext = "png";
        for (String[] fmt : IMG_FORMATS) {
            if (name.endsWith("." + fmt[1])) {
                ext = fmt[1];
                break;
            }
        }
        String filePath = file.getAbsolutePath();
        if (!name.contains(".")) filePath += "." + ext;

        BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        paintAll(img.getGraphics());
        try {
            ImageIO.write(img, ext, new File(filePath));
            AppLogger.info("Overview saved to " + filePath);
        } catch (Exception ex) {
            AppLogger.error("Failed to save overview image: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, I18n.t("charts.saveFailed", ex.getMessage()),
                    I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refresh() {
        refreshDateSelector();
        refreshAppFilter();
        String selectedDate = (String) dateSelector.getSelectedItem();
        String selectedApp = (String) appFilter.getSelectedItem();
        if (selectedDate != null) {
            updateCharts(selectedDate, selectedApp != null ? selectedApp : db.WorkflowRepository.MULTITOOL_NAME);
        }
    }

    private void refreshDateSelector() {
        dateSelector.removeAllItems();
        for (String d : workflowRepo.getAvailableDates())
            dateSelector.addItem(d);
    }

    private void refreshAppFilter() {
        appFilter.removeAllItems();
        appFilter.addItem(db.WorkflowRepository.MULTITOOL_NAME);
        List<Object[]> apps = workflowRepo.getTrackedAppsFull();
        for (Object[] app : apps) {
            appFilter.addItem((String) app[1]);
        }
        appFilter.setSelectedItem(db.WorkflowRepository.MULTITOOL_NAME);
    }

    private void updateCharts(String date, String filter) {
        new Thread(() -> {
            int[] hVal = workflowRepo.getHourlyStats(date, filter);
            String[] hLab = new String[24];
            for (int i = 0; i < 24; i++)
                hLab[i] = I18n.t("chart.time") + " " + i + ":00";
            StatResult wData = workflowRepo.getPeriodStats(date, filter, 7);
            String[] dayNames = new String[7];
            for (int i = 0; i < 7; i++) {
                LocalDate d = LocalDate.parse(wData.labels[i]);
                dayNames[i] = d.getDayOfWeek()
                        .getDisplayName(java.time.format.TextStyle.SHORT, I18n.locale())
                        .toUpperCase(I18n.locale());
            }
            StatResult mData = workflowRepo.getCalendarMonthStats(date, filter);
            String displayDate = date;
            try {
                displayDate = util.DateUtils.toDisplay(LocalDate.parse(date));
            } catch (Exception ignored) {}
            final String hourTitleDate = displayDate;
            SwingUtilities.invokeLater(() -> {
                hourChart.setData(I18n.t("charts.hourly", hourTitleDate), hVal, hLab);
                weekChart.setData(I18n.t("charts.weekly"), wData.values, dayNames);
                monthChart.setData(I18n.t("charts.monthly"), mData.values, mData.labels);
                pieChart.setData(workflowRepo.getDaySummary(date));
            });
        }).start();
    }
}

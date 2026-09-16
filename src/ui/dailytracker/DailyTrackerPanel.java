package ui.dailytracker;

import db.DatabaseProvider;
import ui.UIStyle;

import javax.swing.*;
import java.awt.*;

public class DailyTrackerPanel extends JPanel {
    private final DailyOverviewPanel overviewPanel;

    public DailyTrackerPanel() {
        setLayout(new BorderLayout());
        setBackground(UIStyle.BG_COLOR);

        overviewPanel = new DailyOverviewPanel(
                DatabaseProvider.getNutritionRepository(),
                DatabaseProvider.getStepsRepository(),
                DatabaseProvider.getPurchasesRepository()
        );

        Runnable refreshOverview = () ->
                SwingUtilities.invokeLater(overviewPanel::refresh);

        NutritionPanel nutritionPanel = new NutritionPanel(
                DatabaseProvider.getNutritionRepository(), refreshOverview);
        StepsPanel stepsPanel = new StepsPanel(
                DatabaseProvider.getStepsRepository(), refreshOverview);
        PurchasesPanel purchasesPanel = new PurchasesPanel(
                DatabaseProvider.getPurchasesRepository(), refreshOverview);

        JTabbedPane tabs = new JTabbedPane();
        UIStyle.styleTabbedPane(tabs);
        tabs.addTab(" Overview ", overviewPanel);
        tabs.addTab(" Calories ", nutritionPanel);
        tabs.addTab(" Steps ", stepsPanel);
        tabs.addTab(" Purchases ", purchasesPanel);

        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 0) {
                overviewPanel.refresh();
            }
        });

        add(tabs, BorderLayout.CENTER);
    }
}
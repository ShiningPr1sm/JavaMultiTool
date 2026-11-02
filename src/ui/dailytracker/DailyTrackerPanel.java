package ui.dailytracker;

import db.DatabaseProvider;
import ui.UIStyle;
import util.I18n;

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
                DatabaseProvider.getNutritionRepository(),
                DatabaseProvider.getProductRepository(),
                refreshOverview);
        StepsPanel stepsPanel = new StepsPanel(
                DatabaseProvider.getStepsRepository(), refreshOverview);
        PurchasesPanel purchasesPanel = new PurchasesPanel(
                DatabaseProvider.getPurchasesRepository(),
                DatabaseProvider.getProductRepository(),
                DatabaseProvider.getNutritionRepository(),
                refreshOverview);
        ProductCatalogPanel catalogPanel = new ProductCatalogPanel(
                DatabaseProvider.getProductRepository(),
                () -> {
                    nutritionPanel.reloadCatalog();
                    purchasesPanel.reloadCatalog();
                });

        JTabbedPane tabs = new JTabbedPane();
        UIStyle.styleTabbedPane(tabs);
        tabs.addTab(" " + I18n.t("daily.tab.overview") + " ", overviewPanel);
        tabs.addTab(" " + I18n.t("daily.tab.calories") + " ", nutritionPanel);
        tabs.addTab(" " + I18n.t("daily.tab.steps") + " ", stepsPanel);
        tabs.addTab(" " + I18n.t("daily.tab.purchases") + " ", purchasesPanel);
        tabs.addTab(" " + I18n.t("daily.tab.catalog") + " ", catalogPanel);

        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 0) {
                overviewPanel.refresh();
            }
        });

        add(tabs, BorderLayout.CENTER);
    }
}
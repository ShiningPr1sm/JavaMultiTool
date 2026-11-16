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

        // shared data-changed callback refreshes both the overview and the nutrition table;
        // nutritionRef is a holder because the panel is created after this callback
        NutritionPanel[] nutritionRef = new NutritionPanel[1];
        PurchasesPanel[] purchasesRef = new PurchasesPanel[1];
        Runnable refreshData = () -> SwingUtilities.invokeLater(() -> {
            overviewPanel.refresh();
            if (nutritionRef[0] != null) nutritionRef[0].refresh();
            if (purchasesRef[0] != null) purchasesRef[0].refresh();
        });

        NutritionPanel nutritionPanel = new NutritionPanel(
                DatabaseProvider.getNutritionRepository(),
                DatabaseProvider.getProductRepository(),
                DatabaseProvider.getPurchasesRepository(),
                refreshData);
        nutritionRef[0] = nutritionPanel;
        StepsPanel stepsPanel = new StepsPanel(
                DatabaseProvider.getStepsRepository(), refreshData);
        PurchasesPanel purchasesPanel = new PurchasesPanel(
                DatabaseProvider.getPurchasesRepository(),
                DatabaseProvider.getProductRepository(),
                DatabaseProvider.getNutritionRepository(),
                refreshData);
        purchasesRef[0] = purchasesPanel;
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
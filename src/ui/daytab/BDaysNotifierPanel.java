package ui.daytab;

import db.BDaysRepository;
import db.DatabaseProvider;
import service.AchievementService;
import service.BDaysService;
import service.UserSession;
import ui.UIStyle;
import ui.components.BDaysEditPanel;
import ui.components.BDaysOverviewPanel;

import javax.swing.*;
import java.awt.*;

public class BDaysNotifierPanel extends JPanel {
    private static final String EDIT_CARD = "EDIT";
    private static final String OVERVIEW_CARD = "OVERVIEW";

    private final JPanel cards;
    private final JComboBox<String> modeSelector;
    private final BDaysEditPanel editPanel;
    private final BDaysOverviewPanel overviewPanel;

    public BDaysNotifierPanel(String login, BDaysService bdaysService, AchievementService achievementService, UserSession userSession) {
        BDaysRepository repo = DatabaseProvider.getBDaysRepository();

        setLayout(new BorderLayout());
        setBackground(UIStyle.BG_COLOR);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.setBackground(UIStyle.BG_COLOR);

        modeSelector = new JComboBox<>(new String[]{"Upcoming", "List", "Reverse List"});
        UIStyle.styleComboBox(modeSelector);
        top.add(modeSelector);

        JButton overviewBtn = new JButton("Overview");
        JButton editBtn = new JButton("Edit");
        UIStyle.styleButton(overviewBtn);
        UIStyle.styleButton(editBtn);
        top.add(overviewBtn);
        top.add(editBtn);
        add(top, BorderLayout.NORTH);

        editPanel = new BDaysEditPanel(repo, bdaysService, achievementService, login);
        editPanel.setName(EDIT_CARD);
        overviewPanel = new BDaysOverviewPanel(repo, bdaysService);
        overviewPanel.setName(OVERVIEW_CARD);

        cards = new JPanel(new CardLayout());
        cards.setBackground(UIStyle.BG_COLOR);
        cards.add(editPanel, EDIT_CARD);
        cards.add(overviewPanel, OVERVIEW_CARD);
        add(cards, BorderLayout.CENTER);

        overviewBtn.addActionListener(e -> switchCard(OVERVIEW_CARD));
        editBtn.addActionListener(e -> switchCard(EDIT_CARD));
        modeSelector.addActionListener(e -> {
            if (OVERVIEW_CARD.equals(getCurrentCard())) {
                overviewPanel.refresh((String) modeSelector.getSelectedItem());
            }
        });
        switchCard(OVERVIEW_CARD);
    }

    private void switchCard(String card) {
        CardLayout cl = (CardLayout) cards.getLayout();
        if (OVERVIEW_CARD.equals(card)) {
            overviewPanel.refresh((String) modeSelector.getSelectedItem());
        } else {
            editPanel.refreshTable();
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
}

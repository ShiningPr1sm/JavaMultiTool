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

public class BirthdayTrackerPanel extends JPanel {

    public BirthdayTrackerPanel(String login, BDaysService bdaysService, AchievementService achievementService, UserSession userSession) {
        BDaysRepository repo = DatabaseProvider.getBDaysRepository();

        setLayout(new BorderLayout());
        setBackground(UIStyle.BG_COLOR);

        JTabbedPane tabs = new JTabbedPane();
        UIStyle.styleTabbedPane(tabs);

        BDaysOverviewPanel overview = new BDaysOverviewPanel(repo, bdaysService);
        BDaysEditPanel edit = new BDaysEditPanel(repo, bdaysService, achievementService, login, overview::refresh);

        tabs.addTab(" Overview ", overview);
        tabs.addTab(" Edit ", edit);

        add(tabs, BorderLayout.CENTER);
    }
}

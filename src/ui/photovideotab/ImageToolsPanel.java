package ui.photovideotab;

import service.Services;
import ui.UIStyle;
import util.I18n;

import javax.swing.*;
import java.awt.*;

public class ImageToolsPanel extends JPanel {

    public ImageToolsPanel(Services services, String login) {

        setLayout(new BorderLayout());
        setBackground(UIStyle.BG_COLOR);

        JTabbedPane tabs = new JTabbedPane();
        UIStyle.styleTabbedPane(tabs);

        tabs.addTab(" " + I18n.t("pv.tab.metadata") + " ", new MetadataPanel(services, login));
        tabs.addTab(" " + I18n.t("pv.tab.resizer") + " ", new ResizerPanel());
        tabs.addTab(" " + I18n.t("pv.tab.converter") + " ", new ImageConverterPanel());
        tabs.addTab(" " + I18n.t("pv.tab.uniquify") + " ", new ImageUniquifyPanel());

        add(tabs, BorderLayout.CENTER);
    }
}

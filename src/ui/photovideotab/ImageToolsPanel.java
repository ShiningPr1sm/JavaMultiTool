package ui.photovideotab;

import service.Services;
import ui.UIStyle;

import javax.swing.*;
import java.awt.*;

public class ImageToolsPanel extends JPanel {

    public ImageToolsPanel(Services services, String login) {

        setLayout(new BorderLayout());
        setBackground(UIStyle.BG_COLOR);

        JTabbedPane tabs = new JTabbedPane();
        UIStyle.styleTabbedPane(tabs);

        tabs.addTab("  Metadata  ", new MetadataPanel(services, login));
        tabs.addTab("  Resizer  ", new ResizerPanel());
        tabs.addTab("  Converter  ", new ImageConverterPanel());

        add(tabs, BorderLayout.CENTER);
    }
}

package ui.components;

import ui.UIStyle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ExpandableSection extends JPanel {

    private final AtomicBoolean expanded = new AtomicBoolean(false);
    private final JPanel subPanel;
    private final JButton mainButton;
    private final int sidebarWidth;
    private final String sectionTitle;

    public ExpandableSection(String title, String[] items, int sidebarWidth, Consumer<String> onItemClick) {
        this.sidebarWidth = sidebarWidth;
        this.sectionTitle = title;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(UIStyle.SIDE_BOX);

        mainButton = new JButton(title + "  \u25BC");
        UIStyle.styleSidebarMainButton(mainButton, sidebarWidth);
        mainButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainButton.setMaximumSize(new Dimension(sidebarWidth - 20, 40));
        mainButton.setPreferredSize(new Dimension(sidebarWidth - 20, 40));
        mainButton.setMinimumSize(new Dimension(sidebarWidth - 20, 40));

        subPanel = new JPanel();
        subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.Y_AXIS));
        subPanel.setBackground(UIStyle.SIDE_BOX);
        subPanel.setMaximumSize(new Dimension(sidebarWidth, 0));
        subPanel.setPreferredSize(new Dimension(sidebarWidth, 0));
        subPanel.setVisible(false);

        for (String item : items) {
            JButton subBtn = new JButton(item);
            UIStyle.styleSidebarSubButton(subBtn, sidebarWidth);
            subBtn.addActionListener(e -> onItemClick.accept(item));
            subPanel.add(Box.createVerticalStrut(2));
            subPanel.add(subBtn);
        }

        mainButton.addActionListener(e -> toggle(items.length));

        add(mainButton);
        add(Box.createVerticalStrut(5));
        add(subPanel);
    }

    private void toggle(int itemCount) {
        boolean isExpanding = !expanded.get();
        expanded.set(isExpanding);
        mainButton.setText(sectionTitle + "  " + (isExpanding ? "\u25B2" : "\u25BC"));

        subPanel.setVisible(true);
        int targetHeight = isExpanding ? (itemCount * 35) : 0;

        Timer timer = new Timer(10, new ActionListener() {
            int height = subPanel.getHeight();

            @Override
            public void actionPerformed(ActionEvent evt) {
                if (isExpanding && height < targetHeight) {
                    height += 10;
                    if (height > targetHeight) height = targetHeight;
                } else if (!isExpanding && height > 0) {
                    height -= 10;
                    if (height < 0) height = 0;
                }

                subPanel.setPreferredSize(new Dimension(sidebarWidth, height));
                subPanel.setMaximumSize(new Dimension(sidebarWidth, height));
                subPanel.revalidate();

                if ((!isExpanding && height == 0) || (isExpanding && height == targetHeight)) {
                    if (!isExpanding) subPanel.setVisible(false);
                    ((Timer) evt.getSource()).stop();
                }
            }
        });

        timer.start();
    }

}

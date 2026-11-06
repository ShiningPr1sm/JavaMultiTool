package ui.utils;

import ui.MainFrame;
import ui.UIStyle;
import util.AppLogger;
import util.I18n;
import util.VersionInfo;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class TrayManager {
    private final MainFrame frame;
    private TrayIcon trayIcon;
    private JPopupMenu swingMenu;

    public TrayManager(MainFrame frame) {
        this.frame = frame;
        createSwingMenu();
        setupTray();
    }

    private void createSwingMenu() {
        swingMenu = new JPopupMenu();
        swingMenu.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR, 1));
        swingMenu.setBackground(UIStyle.BG_COLOR);

        addMenuItem(I18n.t("tray.open"), "/icons/menu/home_icon.png", e -> showFrame());
        swingMenu.add(createSeparator());
        addMenuItem(I18n.t("sidebar.mediaDownloader"), "/icons/menu/download_icon.png", e -> { showFrame(); frame.openTab("sidebar.mediaDownloader"); });
        addMenuItem(I18n.t("sidebar.birthdayTracker"), "/icons/menu/bdays_icon.png", e -> { showFrame(); frame.openTab("sidebar.birthdayTracker"); });
        addMenuItem(I18n.t("main.settings"), "/icons/menu/settings_icon.png", e -> { showFrame(); frame.openSettings(); });
        swingMenu.add(createSeparator());
        addMenuItem(I18n.t("tray.exit"), "/icons/menu/exit_icon.png", e -> {
            AppLogger.info("Tray: exit clicked, closing application.");
            System.exit(0);
        });
    }

    /**
     * Adds a styled entry to the tray popup. Built as an icon label + a text
     * label inside a plain panel instead of a JMenuItem: Windows look-and-feel
     * always reserves a fixed ~24px checkmark/arrow column in front of menu item
     * icons (cannot be reduced programmatically), which pushed the icon far away
     * from the menu's left edge. A plain panel gives exact pixel control and
     * keeps the app's own colors, hover included.
     */
    private void addMenuItem(String text, String iconPath, ActionListener action) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(true);
        row.setBackground(UIStyle.BG_COLOR);
        row.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 20));

        JLabel iconLabel = new JLabel(loadIcon(iconPath));
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));

        JLabel textLabel = new JLabel(text);
        textLabel.setForeground(Color.WHITE);
        textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        row.add(iconLabel, BorderLayout.WEST);
        row.add(textLabel, BorderLayout.CENTER);

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                row.setBackground(UIStyle.BUTTON_HOVER);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                row.setBackground(UIStyle.BG_COLOR);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    swingMenu.setVisible(false);
                    action.actionPerformed(new ActionEvent(row, ActionEvent.ACTION_PERFORMED, text));
                }
            }
        };
        row.addMouseListener(mouse);
        iconLabel.addMouseListener(mouse);
        textLabel.addMouseListener(mouse);

        // The popup uses a vertical BoxLayout: rows with unbounded maximum width
        // are stretched to the full menu width, content aligned to the left edge.
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        swingMenu.add(row);
    }

    /** Loads a 16px tray-menu icon, or null when the resource is missing. */
    private Icon loadIcon(String iconPath) {
        try {
            java.net.URL url = getClass().getResource(iconPath);
            if (url != null) {
                return new ImageIcon(UIStyle.scaleSmoothly(ImageIO.read(url), 16));
            }
        } catch (Exception e) {
            AppLogger.error("TrayManager: failed to load icon: " + e.getMessage());
        }
        return null;
    }

    private JSeparator createSeparator() {
        JSeparator sep = new JSeparator();
        sep.setBackground(UIStyle.BG_COLOR);
        sep.setForeground(UIStyle.BORDER_COLOR);
        // stretch separators across the whole popup width like the rows
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, sep.getPreferredSize().height));
        return sep;
    }

    private void setupTray() {
        if (!SystemTray.isSupported()) return;
        try {
            SystemTray tray = SystemTray.getSystemTray();
            BufferedImage trayImg = ImageIO.read(getClass().getResource("/project_icon_16.png"));
            trayIcon = new TrayIcon(trayImg, buildTooltip());
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                        showJPopupMenu(e);
                    }
                }
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) showFrame();
                }
            });
            tray.add(trayIcon);
            AppLogger.info("System tray initialized.");
        } catch (Exception e) { AppLogger.error("TrayManager: failed to setup tray: " + e.getMessage()); }
    }

    private void showJPopupMenu(MouseEvent e) {
        JDialog hiddenDialog = new JDialog();
        hiddenDialog.setUndecorated(true);
        hiddenDialog.setSize(1, 1);
        hiddenDialog.setLocation(e.getX() - 5, e.getY() - swingMenu.getPreferredSize().height - 5);
        hiddenDialog.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {}
            @Override
            public void windowLostFocus(WindowEvent e) {
                swingMenu.setVisible(false);
                hiddenDialog.dispose();
            }
        });
        hiddenDialog.setVisible(true);
        swingMenu.show(hiddenDialog.getContentPane(), 0, 0);
        hiddenDialog.requestFocus();
    }

    /** "JavaMultiTool v0.1.7" from the jar manifest; "(dev)" when run from the IDE. */
    private static String buildTooltip() {
        String version = VersionInfo.getVersion();
        return "dev".equals(version) ? "JavaMultiTool (dev)" : "JavaMultiTool v" + version;
    }

    private void showFrame() {
        swingMenu.setVisible(false);
        AppLogger.info("Tray: showing main window.");
        frame.setVisible(true);
        frame.setExtendedState(JFrame.NORMAL);
        frame.toFront();
    }
}
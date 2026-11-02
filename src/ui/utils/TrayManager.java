package ui.utils;

import ui.MainFrame;
import ui.UIStyle;
import util.AppLogger;
import util.I18n;
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

        swingMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                // TODO document why this method is empty
            }
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                for (Component c : swingMenu.getComponents()) {
                    if (c instanceof JMenuItem) {
                        c.setBackground(UIStyle.BG_COLOR);
                    }
                }
            }
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });

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

    private void addMenuItem(String text, String iconPath, ActionListener action) {
        JMenuItem item = new JMenuItem(text);
        item.setOpaque(true);
        item.setBackground(UIStyle.BG_COLOR);
        item.setForeground(Color.WHITE);
        item.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        try {
            java.net.URL imgUrl = getClass().getResource(iconPath);
            if (imgUrl != null) {
                BufferedImage menuImg = ImageIO.read(imgUrl);
                ImageIcon icon = new ImageIcon(UIStyle.scaleSmoothly(menuImg, 16));
                item.setIcon(icon);
            }
        } catch (Exception e) {
            AppLogger.error("TrayManager: failed to load icon: " + e.getMessage());
        }

        item.setIconTextGap(1);
        item.setHorizontalAlignment(SwingConstants.LEFT);
        item.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 20));
        item.addActionListener(action);
        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                item.setBackground(UIStyle.BUTTON_HOVER);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                item.setBackground(UIStyle.BG_COLOR);
            }
        });
        swingMenu.add(item);
    }

    private JSeparator createSeparator() {
        JSeparator sep = new JSeparator();
        sep.setBackground(UIStyle.BG_COLOR);
        sep.setForeground(UIStyle.BORDER_COLOR);
        return sep;
    }

    private void setupTray() {
        if (!SystemTray.isSupported()) return;
        try {
            SystemTray tray = SystemTray.getSystemTray();
            BufferedImage trayImg = ImageIO.read(getClass().getResource("/project_icon_16.png"));
            trayIcon = new TrayIcon(trayImg, "MultiTool");
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

    private void showFrame() {
        swingMenu.setVisible(false);
        AppLogger.info("Tray: showing main window.");
        frame.setVisible(true);
        frame.setExtendedState(JFrame.NORMAL);
        frame.toFront();
    }
}
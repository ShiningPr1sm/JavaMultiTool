package ui;

import ui.theme.ComponentStyler;
import util.AppLogger;

import javax.swing.*;
import java.awt.*;

public class UIStyle {
    public static Color BG_COLOR = new Color(25, 25, 25);
    public static Color HEADER_COLOR = new Color(30, 30, 30);
    public static Color SECONDARY_BG = new Color(35, 35, 35);
    public static Color BUTTON_BG = new Color(40, 40, 40);
    public static Color SIDE_BOX = new Color(45, 45, 45);
    public static Color BUTTON_HOVER = new Color(55, 55, 55);
    public static Color BORDER_COLOR = new Color(60, 60, 60);
    public static Color BUTTON_PRESSED = new Color(65, 65, 65);
    public static Color PROGRESS_BAR = new Color(114, 99, 100);
    public static Color BG_PROGRESS_BAR = new Color(130, 150, 100);
    public static Color COMPLETED_ACH = new Color(60, 120, 60);
    public static Color ACCENT_COLOR = new Color(100, 200, 100);
    public static Color XP_LABEL_COLOR = new Color(180, 255, 180);
    public static Color TEXT_COLOR = Color.WHITE;

    public static void applyTheme(String themeName) {
        switch (themeName) {
            case "Original Dark" -> {
                BG_COLOR       = new Color(25, 25, 25);
                HEADER_COLOR   = new Color(30, 30, 30);
                SECONDARY_BG   = new Color(35, 35, 35);
                BUTTON_BG      = new Color(40, 40, 40);
                SIDE_BOX       = new Color(45, 45, 45);
                BUTTON_HOVER   = new Color(55, 55, 55);
                BORDER_COLOR   = new Color(60, 60, 60);
                BUTTON_PRESSED = new Color(65, 65, 65);
                BG_PROGRESS_BAR= new Color(45, 45, 45);
                PROGRESS_BAR   = new Color(176, 65, 65);
                ACCENT_COLOR   = new Color(100, 200, 100);
            }
            case "Midnight Blue" -> {
                BG_COLOR       = new Color(12, 14, 20);
                HEADER_COLOR   = new Color(18, 22, 30);
                SECONDARY_BG   = new Color(24, 28, 40);
                BUTTON_BG      = new Color(30, 38, 55);
                SIDE_BOX       = new Color(35, 45, 65);
                BORDER_COLOR   = new Color(50, 65, 90);
                BUTTON_HOVER   = new Color(60, 80, 115);
                BUTTON_PRESSED = new Color(80, 105, 145);
                BG_PROGRESS_BAR= new Color(20, 30, 50);
                PROGRESS_BAR   = new Color(0, 180, 255);
                ACCENT_COLOR   = new Color(0, 162, 255);
            }
            case "Deep Forest" -> {
                BG_COLOR       = new Color(15, 18, 15);
                HEADER_COLOR   = new Color(22, 28, 22);
                SECONDARY_BG   = new Color(28, 35, 28);
                BUTTON_BG      = new Color(35, 45, 35);
                SIDE_BOX       = new Color(40, 52, 40);
                BORDER_COLOR   = new Color(55, 70, 55);
                BUTTON_HOVER   = new Color(70, 90, 70);
                BUTTON_PRESSED = new Color(85, 110, 85);
                BG_PROGRESS_BAR= new Color(30, 40, 30);
                PROGRESS_BAR   = new Color(76, 182, 76);
                ACCENT_COLOR   = new Color(140, 255, 100);
            }
            case "Dracula" -> {
                BG_COLOR       = new Color(40, 42, 54);
                HEADER_COLOR   = new Color(33, 34, 44);
                SECONDARY_BG   = new Color(55, 55, 65);
                BUTTON_BG      = new Color(56, 58, 73);
                SIDE_BOX       = new Color(68, 71, 90);
                BORDER_COLOR   = new Color(98, 114, 164);
                BUTTON_HOVER   = new Color(80, 85, 110);
                BUTTON_PRESSED = new Color(100, 105, 130);
                BG_PROGRESS_BAR= new Color(45, 47, 65);
                PROGRESS_BAR   = new Color(255, 121, 198);
                ACCENT_COLOR   = new Color(189, 147, 249);
            }
            case "Calm Tech" -> {
                BG_COLOR       = new Color(30, 30, 38);
                HEADER_COLOR   = new Color(37, 37, 51);
                SECONDARY_BG   = new Color(46, 46, 62);
                BUTTON_BG      = new Color(58, 58, 78);
                SIDE_BOX       = new Color(42, 42, 58);
                BORDER_COLOR   = new Color(65, 65, 85);
                BUTTON_HOVER   = new Color(75, 75, 100);
                BUTTON_PRESSED = new Color(90, 90, 115);
                BG_PROGRESS_BAR= new Color(46, 46, 62);
                PROGRESS_BAR   = new Color(184, 167, 240);
                ACCENT_COLOR   = new Color(184, 167, 240);
            }
            case "Blush Pink" -> {
                BG_COLOR       = new Color(40, 32, 37);
                HEADER_COLOR   = new Color(53, 40, 48);
                SECONDARY_BG   = new Color(61, 46, 56);
                BUTTON_BG      = new Color(74, 56, 69);
                SIDE_BOX       = new Color(56, 42, 52);
                BORDER_COLOR   = new Color(80, 62, 75);
                BUTTON_HOVER   = new Color(95, 75, 90);
                BUTTON_PRESSED = new Color(115, 90, 105);
                BG_PROGRESS_BAR= new Color(61, 46, 56);
                PROGRESS_BAR   = new Color(255, 176, 184);
                ACCENT_COLOR   = new Color(255, 176, 184);
            }
            case "Night Energy" -> {
                BG_COLOR       = new Color(24, 24, 42);
                HEADER_COLOR   = new Color(32, 32, 58);
                SECONDARY_BG   = new Color(42, 42, 72);
                BUTTON_BG      = new Color(50, 48, 80);
                SIDE_BOX       = new Color(37, 37, 64);
                BORDER_COLOR   = new Color(60, 60, 95);
                BUTTON_HOVER   = new Color(72, 70, 110);
                BUTTON_PRESSED = new Color(90, 85, 130);
                BG_PROGRESS_BAR= new Color(42, 42, 72);
                PROGRESS_BAR   = new Color(255, 196, 155);
                ACCENT_COLOR   = new Color(255, 196, 155);
            }
            case "Crimson Ember" -> {
                BG_COLOR       = new Color(20, 15, 15);
                HEADER_COLOR   = new Color(28, 20, 20);
                SECONDARY_BG   = new Color(35, 25, 25);
                BUTTON_BG      = new Color(45, 30, 30);
                SIDE_BOX       = new Color(55, 35, 35);
                BORDER_COLOR   = new Color(75, 45, 45);
                BUTTON_HOVER   = new Color(95, 55, 55);
                BUTTON_PRESSED = new Color(120, 65, 65);
                BG_PROGRESS_BAR= new Color(40, 20, 20);
                PROGRESS_BAR   = new Color(255, 87, 34);
                ACCENT_COLOR   = new Color(255, 75, 75);
            } case "default" -> {
                BG_COLOR       = new Color(25, 25, 25);
                HEADER_COLOR   = new Color(30, 30, 30);
                SECONDARY_BG   = new Color(35, 35, 35);
                BUTTON_BG      = new Color(40, 40, 40);
                SIDE_BOX       = new Color(45, 45, 45);
                BUTTON_HOVER   = new Color(55, 55, 55);
                BORDER_COLOR   = new Color(60, 60, 60);
                BUTTON_PRESSED = new Color(65, 65, 65);
                PROGRESS_BAR   = new Color(114, 99, 100);
                BG_PROGRESS_BAR= new Color(130, 150, 100);
                COMPLETED_ACH  = new Color(60, 120, 60);
                ACCENT_COLOR   = new Color(100, 200, 100);
                XP_LABEL_COLOR = new Color(180, 255, 180);
            }
        }
        BORDER_COLOR = SECONDARY_BG.brighter();
        BUTTON_HOVER = BUTTON_BG.brighter();
    }


    public static void styleButton(AbstractButton btn) {
        ComponentStyler.styleButton(btn);
    }

    public static void styleTextField(JTextField field) {
        ComponentStyler.styleTextField(field);
    }

    public static void makeFocusable(JPanel panel) {
        ComponentStyler.makeFocusable(panel);
    }

    public static void styleScrollBar(JScrollPane sp) {
        ComponentStyler.styleScrollBar(sp);
    }

    public static void styleComboBox(JComboBox<?> cb) {
        ComponentStyler.styleComboBox(cb);
    }

    public static void styleTabbedPane(JTabbedPane tabs) {
        ComponentStyler.styleTabbedPane(tabs);
    }

    public static void styleProgressBar(JProgressBar pb) {
        ComponentStyler.styleProgressBar(pb);
    }

    public static void styleSpinner(JSpinner s) {
        ComponentStyler.styleSpinner(s);
    }

    public static void styleSidebarMainButton(JButton button, int sidebarWidth) {
        ComponentStyler.styleSidebarMainButton(button, sidebarWidth);
    }

    public static void styleSidebarSubButton(JButton button, int sidebarWidth) {
        ComponentStyler.styleSidebarSubButton(button, sidebarWidth);
    }

    public static void setAppIcon(Window window) {
        try {
            java.net.URL iconUrl = UIStyle.class.getResource("/project_icon.png");
            if (iconUrl == null) return;
            Image sourceImage = new ImageIcon(iconUrl).getImage();
            java.util.List<Image> images = new java.util.ArrayList<>();
            for (int size : new int[]{16, 32, 64, 128}) {
                images.add(sourceImage.getScaledInstance(size, size, Image.SCALE_SMOOTH));
            }
            if (window instanceof JFrame frame) {
                frame.setIconImages(images);
            } else if (window instanceof JDialog dialog) {
                dialog.setIconImages(images);
            }
        } catch (Exception e) {
            AppLogger.error("UIStyle: failed to set app icon: " + e.getMessage());
        }
    }
}
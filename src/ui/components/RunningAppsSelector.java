package ui.components;

import db.WorkflowRepository;
import service.RunningProcessService;
import ui.UIStyle;
import util.AppLogger;
import util.I18n;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RunningAppsSelector {

    private static final int ICON_SIZE = 20;
    private static final Map<String, Icon> ICON_CACHE = new ConcurrentHashMap<>();
    private static Icon genericFileIcon;
    private static Icon standardAppIcon;

    public static void show(Component parent, WorkflowRepository workflowRepo, Runnable onAppAdded,
                            RunningProcessService runningProcessService) {
        JDialog dialog = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(parent),
                I18n.t("apps.selectRunning"), true);
        dialog.getContentPane().setBackground(UIStyle.BG_COLOR);
        dialog.setLayout(new BorderLayout());

        DefaultListModel<Object> listModel = new DefaultListModel<>();
        Map<String, String> apps = runningProcessService.getRunningExePaths();
        apps.keySet().forEach(listModel::addElement);

        JList<Object> list = new JList<>(listModel);
        list.setBackground(UIStyle.SECONDARY_BG);
        list.setForeground(Color.WHITE);
        list.setCellRenderer(new RunningAppListRenderer(apps));
        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));
        UIStyle.styleScrollBar(sp);

        JButton addBtn = new JButton(I18n.t("apps.addToTracking"));
        UIStyle.styleButton(addBtn);
        addBtn.addActionListener(e -> {
            Object selObj = list.getSelectedValue();
            if (selObj != null) {
                String sel = selObj.toString();
                workflowRepo.addTrackedApp(runningProcessService.prettifyExeName(sel), sel);
                AppLogger.info("Tracked app added from running processes: " + sel);
                onAppAdded.run();
                dialog.dispose();
            }
        });

        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(UIStyle.BG_COLOR);
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.add(new JLabel("<html><b style='color:gray'>" + I18n.t("apps.selectPrompt") + "</b></html>"),
                BorderLayout.NORTH);
        p.add(sp, BorderLayout.CENTER);
        p.add(addBtn, BorderLayout.SOUTH);

        dialog.add(p);
        dialog.setSize(400, 500);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    /** Renders each running process with its own icon and enlarged text. */
    private static final class RunningAppListRenderer extends DefaultListCellRenderer {
        private final Map<String, String> paths;

        RunningAppListRenderer(Map<String, String> paths) {
            this.paths = paths;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            String exe = (value instanceof String s) ? s : "";
            JLabel lbl = (JLabel) super.getListCellRendererComponent(list, exe, index, isSelected, cellHasFocus);
            lbl.setIcon(appIcon(paths.get(exe)));
            lbl.setIconTextGap(8);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            lbl.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            return lbl;
        }
    }

    /** Returns a cached icon for the executable path (never null). */
    private static Icon appIcon(String path) {
        return ICON_CACHE.computeIfAbsent(path == null ? "" : path, RunningAppsSelector::loadIcon);
    }

    private static boolean sameImage(Icon a, Icon b) {
        if (a == null || b == null) return false;
        BufferedImage x = toImage(a, ICON_SIZE);
        BufferedImage y = toImage(b, ICON_SIZE);
        for (int i = 0; i < ICON_SIZE; i++)
            for (int j = 0; j < ICON_SIZE; j++)
                if (x.getRGB(i, j) != y.getRGB(i, j)) return false;
        return true;
    }

    private static BufferedImage toImage(Icon src, int size) {
        return (BufferedImage) ((ImageIcon) scaled(src, size)).getImage();
    }

    private static boolean isGeneric(Icon sys) {
        if (genericFileIcon == null) {
            genericFileIcon = UIManager.getIcon("FileView.fileIcon");
        }
        return sameImage(sys, genericFileIcon);
    }

    /** Resolves the OS icon for an executable path, falling back to a standard app glyph. */
    private static Icon loadIcon(String path) {
        if (!path.isBlank()) {
            try {
                File f = new File(path);
                if (f.exists()) {
                    Icon sys = FileSystemView.getFileSystemView().getSystemIcon(f, 32, 32);
                    if (sys != null && sys.getIconWidth() > 0 && sys.getIconHeight() > 0
                            && hasVisibleContent(sys) && !isGeneric(sys)) {
                        return scaled(sys, ICON_SIZE);
                    }
                }
            } catch (Exception e) {
                AppLogger.error("Icon load failed for " + path + ": " + e);
            }
        }
        return fallbackIcon();
    }

    /** Rejects blank shell icons returned for executables without a registered icon. */
    private static boolean hasVisibleContent(Icon icon) {
        BufferedImage img = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        icon.paintIcon(null, g, 0, 0);
        g.dispose();
        int visible = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if ((img.getRGB(x, y) >>> 24) > 32) visible++;
            }
        }
        return visible >= Math.max(1, img.getWidth() * img.getHeight() / 100);
    }

    private static Icon scaled(Icon src, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        double k = Math.min((double) size / src.getIconWidth(), (double) size / src.getIconHeight());
        g.scale(k, k);
        src.paintIcon(null, g, 0, 0);
        g.dispose();
        return new ImageIcon(img);
    }

    /** Draws the standard application placeholder used when an executable has no usable icon. */
    private static Icon fallbackIcon() {
        if (standardAppIcon != null) return standardAppIcon;
        BufferedImage img = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(UIStyle.BUTTON_HOVER);
        g.fillRoundRect(1, 1, ICON_SIZE - 2, ICON_SIZE - 2, 6, 6);
        g.setColor(UIStyle.BORDER_COLOR);
        g.drawRoundRect(1, 1, ICON_SIZE - 2, ICON_SIZE - 2, 6, 6);
        g.setColor(UIStyle.TEXT_COLOR);
        g.drawRoundRect(5, 6, 10, 9, 2, 2);
        g.drawLine(5, 9, 15, 9);
        g.dispose();
        standardAppIcon = new ImageIcon(img);
        return standardAppIcon;
    }
}
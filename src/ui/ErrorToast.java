package ui;

import util.AppLogger;
import util.ConfigManager;
import util.I18n;

import javax.swing.border.LineBorder;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;

public class ErrorToast {
    private static final int TOAST_WIDTH = 380;
    private static final int TOAST_HEIGHT = 96;
    private static final int MARGIN = 16;
    private static final int HIDE_DELAY_MS = 6000;
    private static final Color ERR_COLOR = new Color(255, 110, 110);

    private static ErrorToast instance;
    private static final Deque<String> pendingBeforeInstall = new ArrayDeque<>();

    private final JWindow window;
    private final JLabel titleLabel;
    private final JLabel counterLabel;
    private final JLabel bodyLabel;
    private final JButton closeButton;
    private final Deque<String> queue = new ArrayDeque<>();
    private final Timer hideTimer;
    private JFrame frame;
    private String currentMessage;
    private boolean openFailedReported;
    private static final String FONT_LABEL = "Segoe UI";

    private ErrorToast() {
        window = new JWindow();
        window.setAlwaysOnTop(true);
        window.setSize(TOAST_WIDTH, TOAST_HEIGHT);

        JPanel content = new JPanel(new BorderLayout(8, 4));
        content.setBackground(UIStyle.BG_COLOR);
        content.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UIStyle.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 8)));
        window.setContentPane(content);

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        topRow.setOpaque(false);

        titleLabel = new JLabel(I18n.t("toast.occurred"));
        titleLabel.setForeground(ERR_COLOR);
        titleLabel.setFont(new Font(FONT_LABEL, Font.BOLD, 13));
        topRow.add(titleLabel);

        counterLabel = new JLabel();
        counterLabel.setForeground(Color.LIGHT_GRAY);
        counterLabel.setFont(new Font(FONT_LABEL, Font.PLAIN, 11));
        topRow.add(counterLabel);

        closeButton = new JButton("\u00D7");
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setFocusPainted(false);
        closeButton.setForeground(Color.LIGHT_GRAY);
        closeButton.setFont(new Font(FONT_LABEL, Font.BOLD, 13));
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> clearAll());
        closeButton.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { closeButton.setForeground(Color.WHITE); }
            @Override public void mouseExited(MouseEvent e) { closeButton.setForeground(Color.LIGHT_GRAY); }
        });

        bodyLabel = new JLabel();
        bodyLabel.setForeground(Color.LIGHT_GRAY);
        bodyLabel.setFont(new Font(FONT_LABEL, Font.PLAIN, 12));
        bodyLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        content.add(topRow, BorderLayout.NORTH);
        content.add(bodyLabel, BorderLayout.CENTER);
        content.add(closeButton, BorderLayout.EAST);

        MouseAdapter openClick = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { openLogFile(); }
        };
        titleLabel.addMouseListener(openClick);
        bodyLabel.addMouseListener(openClick);

        hideTimer = new Timer(HIDE_DELAY_MS, e -> {
            hideToast();
            showNext();
        });
        hideTimer.setRepeats(false);
    }

    public static void install(JFrame frame) {
        if (instance == null) {
            instance = new ErrorToast();
        }
        instance.frame = frame;
        AppLogger.setOnError(ErrorToast::push);

        synchronized (pendingBeforeInstall) {
            while (!pendingBeforeInstall.isEmpty()) {
                instance.queue.addLast(pendingBeforeInstall.pollFirst());
            }
        }

        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) { instance.updatePosition(); instance.showNext(); }
            @Override public void windowIconified(WindowEvent e) { instance.hideToast(); }
            @Override public void windowDeiconified(WindowEvent e) { instance.updatePosition(); instance.showNext(); }
            @Override public void windowClosing(WindowEvent e) { instance.hideToast(); }
        });
        frame.addComponentListener(new ComponentAdapter() {
            @Override public void componentMoved(ComponentEvent e) {
                if (instance.window.isVisible()) instance.updatePosition();
            }
            @Override public void componentResized(ComponentEvent e) {
                if (instance.window.isVisible()) instance.updatePosition();
            }
        });
    }

    public static void push(String logEntry) {
        SwingUtilities.invokeLater(() -> {
            if (!Boolean.parseBoolean(ConfigManager.loadProperty("showNotifications", "true"))) return;
            if (instance == null) {
                synchronized (pendingBeforeInstall) {
                    pendingBeforeInstall.addLast(logEntry);
                }
                return;
            }
            instance.queue.addLast(logEntry);
            if (instance.window.isVisible()) {
                instance.updateCounter();
                instance.hideTimer.restart();
            } else {
                instance.showNext();
            }
        });
    }

    private void showNext() {
        if (queue.isEmpty()) {
            hideToast();
            return;
        }
        if (frame == null || !frame.isShowing()) {
            return;
        }
        currentMessage = queue.pollFirst();
        updateCounter();
        bodyLabel.setText(htmlBody(currentMessage));
        updatePosition();
        window.setVisible(true);
        hideTimer.restart();
    }

    private void hideToast() {
        hideTimer.stop();
        window.setVisible(false);
    }

    private void updateCounter() {
        if (queue.isEmpty()) {
            counterLabel.setText("");
        } else {
            counterLabel.setText("+" + queue.size());
        }
    }

    private void clearAll() {
        queue.clear();
        hideToast();
    }

    private void openLogFile() {
        File log = AppLogger.getCurrentLogFile();
        if (log == null || !log.exists()) {
            if (!openFailedReported) {
                openFailedReported = true;
                System.err.println("[ErrorToast] Log file not found.");
            }
            hideToast();
            return;
        }
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(log);
            } catch (Exception e) {
                if (!openFailedReported) {
                    openFailedReported = true;
                    System.err.println("[ErrorToast] Could not open log file: " + e.getMessage());
                }
            }
        }
        hideToast();
        showNext();
    }

    private void updatePosition() {
        if (frame == null) return;
        int x = frame.getX() + frame.getWidth() - TOAST_WIDTH - MARGIN;
        int y = frame.getY() + frame.getHeight() - TOAST_HEIGHT - MARGIN;
        window.setLocation(x, y);
    }

    private static String htmlBody(String message) {
        String shortMsg = message.length() > 120 ? message.substring(0, 120) + "..." : message;
        return "<html><body style='width:" + (TOAST_WIDTH - 80) + "px'>" + escapeHtml(shortMsg) +
                "<br><span style='color:#8a8a8a'> Click to view the log</span></body></html>";
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

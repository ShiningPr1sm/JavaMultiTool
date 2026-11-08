package ui.photovideotab;

import service.MediaDownloadService;
import ui.StyledDialog;
import ui.UIStyle;
import util.AppLogger;
import util.I18n;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MediaDownloaderPanel extends JPanel {

    /** Rendered placeholder (translated once; identity only for "empty?" checks). */
    private final String placeholderText = I18n.t("media.placeholder");

    /** Translates mapped keys; values without a mapping (browser names, yt-dlp formats)
     *  render as-is and are never passed to {@link I18n#t} (which logs missing keys). */
    private static String identityOrT(String value) {
        String key = comboKey(value);
        return key.equals(value) ? value : I18n.t(key);
    }

    /** Identity combo values -> i18n key (modes and "None"; browser names stay as-is). */
    private static String comboKey(String value) {
        return switch (value) {
            case "Video + Audio" -> "media.mode.both";
            case "Video only (muted)" -> "media.mode.videoOnly";
            case "Audio only (mp3)" -> "media.mode.audioOnly";
            case "None" -> "media.none";
            default -> value;
        };
    }

    private JTextArea textArea;
    private JProgressBar progressBar;
    private JComboBox<String> formatBox;
    private JComboBox<String> browserComboBox;
    private JButton downloadButton;
    private JButton thumbnailButton;
    private JButton cancelButton;
    private JCheckBox emergencyBox;
    /** Guards against two yt-dlp runs at once (double-click on stale buttons). */
    private volatile boolean taskRunning;
    private final File downloadFolder;
    private final MediaDownloadService downloadService;

    public MediaDownloaderPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UIStyle.BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        downloadFolder = new File(System.getProperty("user.home"), "Downloads");
        downloadService = new MediaDownloadService();
        initUI();
    }

    private void initUI() {
        JPanel centralPanel = new JPanel(new GridBagLayout());
        centralPanel.setBackground(UIStyle.BG_COLOR);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 50, 10, 50);

        ImageIcon thumbIcon = new ImageIcon(
                Objects.requireNonNull(MediaDownloaderPanel.class.getResource("/icons/photovideotab/thumbnail_icon.png"))
        );
        Image scaled = thumbIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);

        textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(UIStyle.SECONDARY_BG);
        textArea.setForeground(Color.GRAY);
        textArea.setCaretColor(Color.WHITE);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textArea.setText(placeholderText);
        textArea.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (textArea.getText().equals(placeholderText)) {
                    textArea.setText("");
                    textArea.setForeground(Color.WHITE);
                }
            }

            public void focusLost(FocusEvent e) {
                if (textArea.getText().isEmpty()) {
                    textArea.setForeground(Color.GRAY);
                    textArea.setText(placeholderText);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(650, 350));
        scrollPane.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));
        UIStyle.styleScrollBar(scrollPane);
        centralPanel.add(scrollPane, gbc);

        progressBar = new JProgressBar(0, 100);
        UIStyle.styleProgressBar(progressBar);
        centralPanel.add(progressBar, gbc);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        controls.setBackground(UIStyle.BG_COLOR);

        formatBox = new JComboBox<>(new String[]{
                "Video + Audio",
                "Video only (muted)",
                "Audio only (mp3)"
        });
        UIStyle.styleComboBox(formatBox);
        formatBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, identityOrT((String) value), index, isSelected, cellHasFocus);
                return this;
            }
        });
        formatBox.setPreferredSize(new Dimension(160, 30));

        downloadButton = new JButton(I18n.t("common.download"));
        UIStyle.styleButton(downloadButton);
        downloadButton.setPreferredSize(new Dimension(120, 30));
        downloadButton.addActionListener(e -> startDownloadTask());

        thumbnailButton = new JButton(new ImageIcon(scaled));
        UIStyle.styleButton(thumbnailButton);
        thumbnailButton.setPreferredSize(new Dimension(30, 30));
        thumbnailButton.setToolTipText(I18n.t("media.thumbTip"));
        thumbnailButton.addActionListener(e -> startThumbnailTask());

        cancelButton = new JButton(I18n.t("common.cancel"));
        UIStyle.styleButton(cancelButton);
        cancelButton.setPreferredSize(new Dimension(90, 30));
        cancelButton.setEnabled(false);
        cancelButton.addActionListener(e -> {
            cancelButton.setEnabled(false);
            downloadService.cancelAll();
            progressBar.setString(I18n.t("media.cancelled"));
            AppLogger.info("Media download cancelled by user");
        });

        browserComboBox = new JComboBox<>(new String[]{"None", "Firefox", "Chrome", "Edge", "Opera", "Brave"});
        UIStyle.styleComboBox(browserComboBox);
        browserComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, identityOrT((String) value), index, isSelected, cellHasFocus);
                return this;
            }
        });
        browserComboBox.setPreferredSize(new Dimension(100, 30));

        emergencyBox = new JCheckBox(I18n.t("media.emergency"));
        UIStyle.styleCheckbox(emergencyBox);
        emergencyBox.setToolTipText(I18n.t("media.emergencyTip"));

        controls.add(formatBox);
        controls.add(downloadButton);
        controls.add(cancelButton);
        controls.add(thumbnailButton);
        controls.add(browserComboBox);
        controls.add(emergencyBox);

        centralPanel.add(controls, gbc);
        add(centralPanel, BorderLayout.CENTER);
    }

    /** Disables every input while a download runs - one operation at a time. */
    private void setBusy(boolean busy) {
        taskRunning = busy;
        downloadButton.setEnabled(!busy);
        cancelButton.setEnabled(busy);
        thumbnailButton.setEnabled(!busy);
        formatBox.setEnabled(!busy);
        browserComboBox.setEnabled(!busy);
        emergencyBox.setEnabled(!busy);
    }

    /** Same validity rule as the standalone Media Downloader: http/https URIs only. */
    private static boolean isValidUrl(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            String scheme = uri.getScheme();
            return scheme != null && (scheme.equals("http") || scheme.equals("https"));
        } catch (java.net.URISyntaxException e) {
            return false;
        }
    }

    /** Parses the textarea (one URL per line); shows a dialog and returns null on emptiness/invalid input. */
    private List<String> parseVideoUrls() {
        String input = textArea.getText().trim();
        if (input.isEmpty() || input.equals(placeholderText)) {
            StyledDialog.show(SwingUtilities.windowForComponent(this), I18n.t("media.errUrl"));
            AppLogger.info("No URL entered.");
            return null;
        }

        List<String> videoUrls = new ArrayList<>();
        for (String url : input.split("\\r?\\n")) {
            String trimmed = url.trim();
            if (trimmed.isEmpty()) continue;
            if (!isValidUrl(trimmed)) {
                StyledDialog.show(SwingUtilities.windowForComponent(this),
                        I18n.t("media.errInvalidUrl", trimmed));
                AppLogger.info("Invalid URL entered: " + trimmed);
                return null;
            }
            videoUrls.add(trimmed);
        }

        if (videoUrls.isEmpty()) {
            StyledDialog.show(SwingUtilities.windowForComponent(this), I18n.t("media.errNoValid"));
            AppLogger.info("Aborted: no valid URLs found.");
            return null;
        }
        return videoUrls;
    }

    /** Cookie profiles may be locked by the browser; the user must expect the extraction. */
    private boolean confirmCookieAccess(String browser) {
        if (browser.equals("none")) return true;
        return StyledDialog.confirmYesNo(SwingUtilities.windowForComponent(this),
                I18n.t("media.cookieBody", browser), I18n.t("media.cookieTitle"));
    }

    private void startDownloadTask() {
        if (taskRunning) return;

        List<String> videoUrls = parseVideoUrls();
        if (videoUrls == null) return;

        String browser = Objects.requireNonNull(browserComboBox.getSelectedItem()).toString().toLowerCase();
        if (!confirmCookieAccess(browser)) return;

        setBusy(true);
        progressBar.setValue(0);
        progressBar.setString(I18n.t("media.starting"));
        boolean emergency = emergencyBox.isSelected();

        new Thread(() -> {
            int failures = 0;
            try {
                downloadService.ensureToolsAvailable();
                String format = (String) formatBox.getSelectedItem();
                AppLogger.info("Selected format: " + format + ", browser: " + browser + ", emergency referer: " + emergency);

                for (int i = 0; i < videoUrls.size(); i++) {
                    if (downloadService.isCancelled()) break;
                    int videoIndex = i + 1;
                    String videoUrl = videoUrls.get(i);
                    AppLogger.info("Processing URL " + videoIndex + "/" + videoUrls.size() + ": " + videoUrl);

                    int exitCode = downloadService.downloadVideo(videoUrl, format, browser, emergency, downloadFolder,
                            (message, percent) -> SwingUtilities.invokeLater(() -> {
                                if (percent != null) {
                                    int totalProgress = (int) (((videoIndex - 1 + percent / 100.0) / videoUrls.size()) * 100);
                                    progressBar.setValue(totalProgress);
                                    progressBar.setString(I18n.t("media.videoProgress",
                                            String.valueOf(videoIndex), String.valueOf(videoUrls.size()),
                                            String.valueOf(percent)));
                                } else {
                                    progressBar.setString(I18n.t("media.videoMsg",
                                            String.valueOf(videoIndex), String.valueOf(videoUrls.size()), message));
                                }
                            }));

                    if (exitCode != 0 && !downloadService.isCancelled()) {
                        failures++;
                        int index = videoIndex;
                        SwingUtilities.invokeLater(() ->
                                progressBar.setString(I18n.t("media.videoFailed",
                                        String.valueOf(index), String.valueOf(exitCode))));
                        AppLogger.info("Video " + videoIndex + " failed (exit " + exitCode + "), continuing: " + videoUrl);
                    }
                }

                boolean cancelled = downloadService.isCancelled();
                int failedCount = failures;
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(cancelled ? 0 : 100);
                    progressBar.setString(cancelled
                            ? I18n.t("media.cancelled")
                            : (failedCount > 0 ? I18n.t("media.doneWithFailures", String.valueOf(failedCount))
                                               : I18n.t("media.allDone")));
                    if (!cancelled && failedCount > 0) {
                        StyledDialog.show(SwingUtilities.windowForComponent(this),
                                I18n.t("media.doneWithFailures", String.valueOf(failedCount)));
                    }
                    setBusy(false);
                    AppLogger.info(cancelled ? "Download cancelled." : "All downloads completed.");
                });
            } catch (Exception ex) {
                AppLogger.error("Download error: " + ex.getMessage());
                SwingUtilities.invokeLater(() -> {
                    progressBar.setString(I18n.t("common.error") + ": " + ex.getMessage());
                    setBusy(false);
                    StyledDialog.show(SwingUtilities.windowForComponent(this), I18n.t("media.errOccurred", ex.getMessage()));
                });
            }
        }).start();
    }

    private void startThumbnailTask() {
        if (taskRunning) return;

        List<String> videoUrls = parseVideoUrls();
        if (videoUrls == null) return;

        String browser = Objects.requireNonNull(browserComboBox.getSelectedItem()).toString().toLowerCase();
        if (!confirmCookieAccess(browser)) return;

        setBusy(true);
        progressBar.setValue(0);
        progressBar.setString(I18n.t("media.thumbStart"));

        new Thread(() -> {
            int failures = 0;
            try {
                downloadService.ensureYTDLPAvailable();

                for (int i = 0; i < videoUrls.size(); i++) {
                    if (downloadService.isCancelled()) break;
                    int videoIndex = i + 1;
                    String videoUrl = videoUrls.get(i);
                    AppLogger.info("Downloading thumbnail " + videoIndex + "/" + videoUrls.size() + ": " + videoUrl);

                    int exitCode = downloadService.downloadThumbnail(videoUrl, browser, downloadFolder,
                            (message, percent) -> SwingUtilities.invokeLater(() ->
                                    progressBar.setString(I18n.t("media.thumbMsg",
                                            String.valueOf(videoIndex), String.valueOf(videoUrls.size()), message))));

                    if (exitCode != 0 && !downloadService.isCancelled()) {
                        failures++;
                        AppLogger.info("Thumbnail " + videoIndex + " failed (exit " + exitCode + "): " + videoUrl);
                    }

                    int totalProgress = (int) (((double) (i + 1) / videoUrls.size()) * 100);
                    SwingUtilities.invokeLater(() -> progressBar.setValue(totalProgress));
                }

                boolean cancelled = downloadService.isCancelled();
                int failedCount = failures;
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(cancelled ? 0 : 100);
                    progressBar.setString(cancelled
                            ? I18n.t("media.cancelled")
                            : (failedCount > 0 ? I18n.t("media.doneWithFailures", String.valueOf(failedCount))
                                               : I18n.t("media.thumbDone")));
                    if (!cancelled && failedCount > 0) {
                        StyledDialog.show(SwingUtilities.windowForComponent(this),
                                I18n.t("media.doneWithFailures", String.valueOf(failedCount)));
                    }
                    setBusy(false);
                    AppLogger.info(cancelled ? "Thumbnail download cancelled." : "All thumbnails downloaded.");
                });
            } catch (Exception ex) {
                AppLogger.error("Thumbnail download error: " + ex.getMessage());
                SwingUtilities.invokeLater(() -> {
                    progressBar.setString(I18n.t("common.error") + ": " + ex.getMessage());
                    setBusy(false);
                    StyledDialog.show(SwingUtilities.windowForComponent(this), I18n.t("media.errOccurred", ex.getMessage()));
                });
            }
        }).start();
    }
}

package ui.photovideotab;

import service.MediaDownloadService;
import ui.UIStyle;
import util.AppLogger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MediaDownloaderPanel extends JPanel {
    private static final String PLACEHOLDER = "Paste or Enter links to social media here...";

    private JTextArea textArea;
    private JProgressBar progressBar;
    private JComboBox<String> formatBox;
    private JComboBox<String> browserComboBox;
    private JButton downloadButton;
    private JButton thumbnailButton;
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
        textArea.setText(PLACEHOLDER);
        textArea.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (textArea.getText().equals(PLACEHOLDER)) {
                    textArea.setText("");
                    textArea.setForeground(Color.WHITE);
                }
            }

            public void focusLost(FocusEvent e) {
                if (textArea.getText().isEmpty()) {
                    textArea.setForeground(Color.GRAY);
                    textArea.setText(PLACEHOLDER);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(650, 350));
        scrollPane.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));
        UIStyle.styleScrollBar(scrollPane);
        centralPanel.add(scrollPane, gbc);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(450, 25));
        progressBar.setBackground(UIStyle.SECONDARY_BG);
        progressBar.setForeground(UIStyle.ACCENT_COLOR);
        progressBar.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));
        centralPanel.add(progressBar, gbc);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        controls.setBackground(UIStyle.BG_COLOR);

        formatBox = new JComboBox<>(new String[]{
                "Video + Audio",
                "Video only (muted)",
                "Audio only (mp3)"
        });
        UIStyle.styleComboBox(formatBox);
        formatBox.setPreferredSize(new Dimension(160, 30));

        downloadButton = new JButton("Download");
        UIStyle.styleButton(downloadButton);
        downloadButton.setPreferredSize(new Dimension(120, 30));
        downloadButton.addActionListener(e -> startDownloadTask());

        thumbnailButton = new JButton(new ImageIcon(scaled));
        UIStyle.styleButton(thumbnailButton);
        thumbnailButton.setPreferredSize(new Dimension(30, 30));
        thumbnailButton.setToolTipText("Download thumbnail");
        thumbnailButton.addActionListener(e -> startThumbnailTask());

        browserComboBox = new JComboBox<>(new String[]{"None", "Firefox", "Chrome", "Edge", "Opera", "Brave"});
        UIStyle.styleComboBox(browserComboBox);
        browserComboBox.setPreferredSize(new Dimension(100, 30));

        controls.add(formatBox);
        controls.add(downloadButton);
        controls.add(thumbnailButton);
        controls.add(browserComboBox);

        centralPanel.add(controls, gbc);
        add(centralPanel, BorderLayout.CENTER);
    }

    private void startDownloadTask() {
        String input = textArea.getText().trim();
        if (input.isEmpty() || input.equals(PLACEHOLDER)) {
            JOptionPane.showMessageDialog(this, "Please enter at least one video URL!");
            AppLogger.error("No URL entered.");
            return;
        }

        String[] urls = input.split("\\r?\\n");
        List<String> videoUrls = new ArrayList<>();
        for (String url : urls) {
            if (!url.trim().isEmpty()) videoUrls.add(url.trim());
        }
        if (videoUrls.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No valid URLs found!");
            return;
        }

        downloadButton.setEnabled(false);
        progressBar.setValue(0);
        progressBar.setString("Starting download...");

        String format = (String) formatBox.getSelectedItem();
        String browser = Objects.requireNonNull(browserComboBox.getSelectedItem()).toString().toLowerCase();

        new Thread(() -> {
            try {
                downloadService.ensureToolsAvailable();
                AppLogger.info("Selected format: " + format + ", browser: " + browser);

                for (int i = 0; i < videoUrls.size(); i++) {
                    int videoIndex = i + 1;
                    String videoUrl = videoUrls.get(i);
                    AppLogger.info("Processing URL " + videoIndex + "/" + videoUrls.size() + ": " + videoUrl);

                    int exitCode = downloadService.downloadVideo(videoUrl, format, browser, downloadFolder,
                            (message, percent) -> SwingUtilities.invokeLater(() -> {
                                if (percent != null) {
                                    int totalProgress = (int) (((videoIndex - 1 + percent / 100.0) / videoUrls.size()) * 100);
                                    progressBar.setValue(totalProgress);
                                    progressBar.setString("Video " + videoIndex + "/" + videoUrls.size() + " - " + percent + "%");
                                } else {
                                    progressBar.setString("Video " + videoIndex + "/" + videoUrls.size() + " - " + message);
                                }
                            }));

                    if (exitCode != 0) {
                        int finalI = i;
                        SwingUtilities.invokeLater(() ->
                                JOptionPane.showMessageDialog(this,
                                        "Error downloading " + videoUrls.get(finalI) + ". Check logs for details."));
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(100);
                    progressBar.setString("All downloads completed!");
                    downloadButton.setEnabled(true);
                    AppLogger.info("All downloads completed.");
                });
            } catch (Exception ex) {
                AppLogger.error("Download error: " + ex.getMessage());
                SwingUtilities.invokeLater(() -> {
                    progressBar.setString("Error: " + ex.getMessage());
                    downloadButton.setEnabled(true);
                    JOptionPane.showMessageDialog(this, "An error occurred: " + ex.getMessage());
                });
            }
        }).start();
    }

    private void startThumbnailTask() {
        String input = textArea.getText().trim();
        if (input.isEmpty() || input.equals(PLACEHOLDER)) {
            JOptionPane.showMessageDialog(this, "Please enter at least one video URL!");
            return;
        }

        String[] urls = input.split("\\r?\\n");
        List<String> videoUrls = new ArrayList<>();
        for (String url : urls) {
            if (!url.trim().isEmpty()) videoUrls.add(url.trim());
        }
        if (videoUrls.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No valid URLs found!");
            return;
        }

        thumbnailButton.setEnabled(false);
        progressBar.setValue(0);
        progressBar.setString("Starting thumbnail download...");

        String browser = Objects.requireNonNull(browserComboBox.getSelectedItem()).toString().toLowerCase();

        new Thread(() -> {
            try {
                downloadService.ensureYTDLPAvailable();

                for (int i = 0; i < videoUrls.size(); i++) {
                    int videoIndex = i + 1;
                    String videoUrl = videoUrls.get(i);
                    AppLogger.info("Downloading thumbnail " + videoIndex + "/" + videoUrls.size() + ": " + videoUrl);

                    downloadService.downloadThumbnail(videoUrl, browser, downloadFolder,
                            (message, percent) -> SwingUtilities.invokeLater(() ->
                                    progressBar.setString("Thumbnail " + videoIndex + "/" + videoUrls.size() + " - " + message)));

                    int totalProgress = (int) (((double) (i + 1) / videoUrls.size()) * 100);
                    SwingUtilities.invokeLater(() -> progressBar.setValue(totalProgress));
                }

                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(100);
                    progressBar.setString("All thumbnails downloaded!");
                    thumbnailButton.setEnabled(true);
                    AppLogger.info("All thumbnails downloaded.");
                });
            } catch (Exception ex) {
                AppLogger.error("Thumbnail download error: " + ex.getMessage());
                SwingUtilities.invokeLater(() -> {
                    progressBar.setString("Error: " + ex.getMessage());
                    thumbnailButton.setEnabled(true);
                    JOptionPane.showMessageDialog(this, "An error occurred: " + ex.getMessage());
                });
            }
        }).start();
    }
}

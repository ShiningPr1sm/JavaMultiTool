package ui.photovideotab;

import ui.UIStyle;
import ui.utils.AppLogger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MediaDownloaderPanel extends JPanel {
    private static final String APPDATA = System.getenv("APPDATA");
    private static final File YTDLP_DIR = new File(APPDATA, "yt-dlp-app");
    private static final File YTDLP_EXE = new File(YTDLP_DIR, "yt-dlp.exe");
    private static final File YTDLP_VERSION_FILE = new File(YTDLP_DIR, "version.txt");
    private static final String YTDLP_URL = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe";
    private static final File FFMPEG_DIR = new File(YTDLP_DIR, "ffmpeg");
    private static final File FFMPEG_EXE = new File(FFMPEG_DIR, "ffmpeg.exe");
    private static final String FFMPEG_ZIP_URL = "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip";

    private static final String PLACEHOLDER = "Paste or Enter links to social media here...";

    private JTextArea textArea;
    private JProgressBar progressBar;
    private JComboBox<String> formatBox;
    private JComboBox<String> browserComboBox;
    private JButton downloadButton;
    private JButton thumbnailButton;
    private final File downloadFolder;

    public MediaDownloaderPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UIStyle.BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        downloadFolder = new File(System.getProperty("user.home"), "Downloads");
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
        downloadButton.addActionListener(_ -> startDownloadTask());

        thumbnailButton = new JButton(new ImageIcon(scaled));
        UIStyle.styleButton(thumbnailButton);
        thumbnailButton.setPreferredSize(new Dimension(30, 30));
        thumbnailButton.setToolTipText("Download thumbnail");
        thumbnailButton.addActionListener(_ -> startThumbnailTask());

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

        new Thread(() -> {
            try {
                checkAndDownloadYTDLP();
                checkAndDownloadFFMPEG();

                String format = (String) formatBox.getSelectedItem();
                String browser = Objects.requireNonNull(browserComboBox.getSelectedItem()).toString().toLowerCase();
                AppLogger.info("Selected format: " + format + ", browser: " + browser);

                for (int i = 0; i < videoUrls.size(); i++) {
                    assert format != null;
                    executeYtDlp(videoUrls.get(i), format, browser, i + 1, videoUrls.size());
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

    private void executeYtDlp(String videoUrl, String selectedFormat, String browser,
                              int current, int total) throws IOException, InterruptedException {
        AppLogger.info("Processing URL " + current + "/" + total + ": " + videoUrl);

        List<String> command = new ArrayList<>();
        command.add(YTDLP_EXE.getAbsolutePath());
        command.add("--remote-components");
        command.add("ejs:github");

        switch (selectedFormat) {
            case "Video + Audio" -> {
                command.add("-f");
                command.add("bestvideo[ext=mp4]+bestaudio[ext=m4a]/bestvideo+bestaudio/best[ext=mp4]/best");
                command.add("--merge-output-format");
                command.add("mp4");
                command.add("--ffmpeg-location");
                command.add(FFMPEG_EXE.getAbsolutePath());
            }
            case "Video only (muted)" -> {
                command.add("-f");
                command.add("bestvideo[ext=mp4]/bestvideo/best[ext=mp4]/best");
            }
            case "Audio only (mp3)" -> {
                command.add("-f");
                command.add("bestaudio/best[ext=mp4]/best");
                command.add("--extract-audio");
                command.add("--audio-format");
                command.add("mp3");
                command.add("--ffmpeg-location");
                command.add(FFMPEG_EXE.getAbsolutePath());
            }
        }

        if (!browser.equals("none")) {
            command.add("--cookies-from-browser");
            command.add(browser);
        }

        command.add("--impersonate");
        command.add("chrome");
        command.add("-o");
        command.add(downloadFolder.getAbsolutePath() + "/%(title)s.%(ext)s");
        command.add(videoUrl);

        AppLogger.info("Executing: " + String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        Pattern pattern = Pattern.compile("(\\d{1,3}\\.\\d)%");

        while ((line = reader.readLine()) != null) {
            AppLogger.info("yt-dlp: " + line);
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                int progress = (int) Float.parseFloat(matcher.group(1));
                int totalProgress = (int) (((current - 1 + progress / 100.0) / total) * 100);
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(totalProgress);
                    progressBar.setString("Video " + current + "/" + total + " - " + progress + "%");
                });
            } else {
                String finalLine = line;
                SwingUtilities.invokeLater(() ->
                        progressBar.setString("Video " + current + "/" + total + " - " + finalLine.trim()));
            }
        }

        int exitCode = process.waitFor();
        AppLogger.info("yt-dlp exited with code " + exitCode + " for: " + videoUrl);

        if (exitCode != 0) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this,
                            "Error downloading " + videoUrl + ". Check logs for details."));
        }
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

        new Thread(() -> {
            try {
                checkAndDownloadYTDLP();
                String browser = Objects.requireNonNull(browserComboBox.getSelectedItem()).toString().toLowerCase();

                for (int i = 0; i < videoUrls.size(); i++) {
                    String videoUrl = videoUrls.get(i);
                    int videoIndex = i + 1;
                    AppLogger.info("Downloading thumbnail " + videoIndex + "/" + videoUrls.size() + ": " + videoUrl);

                    List<String> command = new ArrayList<>();
                    command.add(YTDLP_EXE.getAbsolutePath());
                    command.add("--write-thumbnail");
                    command.add("--skip-download");
                    command.add("--convert-thumbnails");
                    command.add("jpg");
                    command.add("--impersonate");
                    command.add("chrome");
                    if (!browser.equals("none")) {
                        command.add("--cookies-from-browser");
                        command.add(browser);
                    }
                    command.add("-o");
                    command.add(downloadFolder.getAbsolutePath() + "/%(title)s.%(ext)s");
                    command.add(videoUrl);

                    ProcessBuilder pb = new ProcessBuilder(command);
                    pb.redirectErrorStream(true);
                    Process process = pb.start();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        AppLogger.info("yt-dlp thumbnail: " + line);
                        String finalLine = line;
                        SwingUtilities.invokeLater(() -> progressBar.setString(
                                "Thumbnail " + videoIndex + "/" + videoUrls.size() + " - " + finalLine.trim()));
                    }

                    process.waitFor();
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

    private static void checkAndDownloadYTDLP() throws IOException {
        AppLogger.info("Checking yt-dlp...");
        if (!YTDLP_DIR.exists()) {
            YTDLP_DIR.mkdirs();
        }
        String storedVersion = readVersionFile();
        String latestVersion = getLatestYtDlpVersion();
        String currentInstalledVersion = null;
        boolean needsDownload = false;

        if (YTDLP_EXE.exists()) {
            try {
                Process p = new ProcessBuilder(YTDLP_EXE.getAbsolutePath(), "--version").start();
                currentInstalledVersion = new BufferedReader(new InputStreamReader(p.getInputStream())).readLine();
                p.waitFor();
                AppLogger.info("Installed yt-dlp version: " + currentInstalledVersion);
            } catch (Exception e) {
                AppLogger.error("Could not read installed yt-dlp version: " + e.getMessage());
                needsDownload = true;
            }
        } else {
            AppLogger.info("yt-dlp.exe not found, will download.");
            needsDownload = true;
        }

        if (latestVersion == null) {
            AppLogger.error("Could not fetch latest yt-dlp version from GitHub.");
            if (!YTDLP_EXE.exists() && storedVersion == null) needsDownload = true;
        } else {
            AppLogger.info("Latest yt-dlp version: " + latestVersion);
            if (latestVersion.equals(currentInstalledVersion) && latestVersion.equals(storedVersion)) {
                AppLogger.info("yt-dlp is up to date.");
                needsDownload = false;
            } else {
                AppLogger.info("yt-dlp update needed.");
                needsDownload = true;
            }
        }

        if (needsDownload) {
            AppLogger.info("Downloading yt-dlp...");
            try (InputStream in = new URL(YTDLP_URL).openStream();
                 FileOutputStream out = new FileOutputStream(YTDLP_EXE)) {
                in.transferTo(out);
            }
            YTDLP_EXE.setExecutable(true);

            String versionToWrite = latestVersion;
            if (versionToWrite == null) {
                try {
                    Process p = new ProcessBuilder(YTDLP_EXE.getAbsolutePath(), "--version").start();
                    versionToWrite = new BufferedReader(new InputStreamReader(p.getInputStream())).readLine();
                    p.waitFor();
                } catch (Exception e) {
                    versionToWrite = "unknown";
                }
            }
            writeVersionFile(versionToWrite != null ? versionToWrite : "unknown");
            AppLogger.info("yt-dlp updated to: " + versionToWrite);
        }
    }

    private static String getLatestYtDlpVersion() {
        try (InputStream in = new URL("https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest").openStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            StringBuilder res = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) res.append(line);
            Matcher m = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").matcher(res.toString());
            return m.find() ? m.group(1) : null;
        } catch (Exception e) {
            AppLogger.error("Could not fetch yt-dlp version: " + e.getMessage());
            return null;
        }
    }

    private static String readVersionFile() {
        if (!YTDLP_VERSION_FILE.exists()) return null;
        try {
            return Files.readString(YTDLP_VERSION_FILE.toPath()).trim();
        } catch (IOException e) {
            AppLogger.error("Error reading version file: " + e.getMessage());
            return null;
        }
    }

    private static void writeVersionFile(String version) {
        try {
            Files.writeString(YTDLP_VERSION_FILE.toPath(), version);
        } catch (IOException e) {
            AppLogger.error("Error writing version file: " + e.getMessage());
        }
    }

    private static void checkAndDownloadFFMPEG() throws IOException {
        AppLogger.info("Checking ffmpeg...");
        if (!FFMPEG_DIR.exists()) {
            FFMPEG_DIR.mkdirs();
        }
        cleanupOldFfmpegExtracts();

        if (FFMPEG_EXE.exists()) {
            AppLogger.info("ffmpeg already installed.");
            return;
        }

        AppLogger.info("Downloading ffmpeg...");
        File zip = new File(FFMPEG_DIR, "ffmpeg.zip");
        try (InputStream in = new URL(FFMPEG_ZIP_URL).openStream();
             FileOutputStream out = new FileOutputStream(zip)) {
            in.transferTo(out);
        }

        extractFfmpegFromZip(zip);
        zip.delete();

        if (!FFMPEG_EXE.exists())
            throw new IOException("ffmpeg.exe not found in archive.");

        FFMPEG_EXE.setExecutable(true, false);
        AppLogger.info("ffmpeg installed to: " + FFMPEG_EXE.getAbsolutePath());
    }

    private static void extractFfmpegFromZip(File zipFile) throws IOException {
        try (ZipFile zf = new ZipFile(zipFile)) {
            ZipEntry candidate = null;
            Enumeration<? extends ZipEntry> entries = zf.entries();

            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                String name = e.getName().replace('\\', '/').toLowerCase();
                if (!e.isDirectory() && name.endsWith("/bin/ffmpeg.exe")) {
                    candidate = e;
                    break;
                }
            }

            if (candidate == null) {
                entries = zf.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry e = entries.nextElement();
                    if (!e.isDirectory() && e.getName().toLowerCase().endsWith("ffmpeg.exe")) {
                        candidate = e;
                        break;
                    }
                }
            }

            if (candidate == null)
                throw new IOException("ffmpeg.exe not found in archive.");

            try (InputStream is = zf.getInputStream(candidate);
                 FileOutputStream fos = new FileOutputStream(MediaDownloaderPanel.FFMPEG_EXE)) {
                is.transferTo(fos);
            }
            AppLogger.info("ffmpeg.exe extracted from: " + candidate.getName());
        }
    }

    private static void cleanupOldFfmpegExtracts() {
        File[] files = FFMPEG_DIR.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.getName().equalsIgnoreCase("ffmpeg.exe") || f.getName().equalsIgnoreCase("ffmpeg.zip"))
                continue;
            deleteRecursivelyQuiet(f);
        }
    }

    private static void deleteRecursivelyQuiet(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null)
                for (File c : children) deleteRecursivelyQuiet(c);
        }
        try {
            f.delete();
        } catch (Exception ignored) {

        }
    }
}
package service;

import util.AppLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MediaDownloadService {

    private static final File YTDLP_DIR = new File(util.AppPaths.COMMON_DIR, "yt-dlp");
    public static final File YTDLP_EXE = new File(YTDLP_DIR, "yt-dlp.exe");
    private static final File YTDLP_VERSION_FILE = new File(YTDLP_DIR, "version.txt");
    private static final String YTDLP_URL = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe";
    private static final File FFMPEG_DIR = new File(util.AppPaths.COMMON_DIR, "ffmpeg");
    public static final File FFMPEG_EXE = new File(FFMPEG_DIR, "ffmpeg.exe");
    private static final String FFMPEG_ZIP_URL = "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip";

    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(String message, Integer percent);
    }

    public void ensureToolsAvailable() throws IOException {
        checkAndDownloadYTDLP();
        checkAndDownloadFFMPEG();
    }

    public void ensureYTDLPAvailable() throws IOException {
        checkAndDownloadYTDLP();
    }

    public int downloadVideo(String videoUrl, String selectedFormat, String browser, File downloadFolder, ProgressCallback callback) throws IOException, InterruptedException {
        List<String> command = buildDownloadCommand(videoUrl, selectedFormat, browser, downloadFolder);
        return executeProcess(command, callback);
    }

    public int downloadThumbnail(String videoUrl, String browser, File downloadFolder, ProgressCallback callback) throws IOException, InterruptedException {
        List<String> command = buildThumbnailCommand(videoUrl, browser, downloadFolder);
        return executeProcess(command, callback);
    }

    private List<String> buildDownloadCommand(String videoUrl, String selectedFormat, String browser, File downloadFolder) {
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

        return command;
    }

    private List<String> buildThumbnailCommand(String videoUrl, String browser, File downloadFolder) {
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
        return command;
    }

    private int executeProcess(List<String> command, ProgressCallback callback) throws IOException, InterruptedException {
        AppLogger.info("Executing: " + String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            Pattern pattern = Pattern.compile("(\\d{1,3}\\.\\d)%");
            while ((line = reader.readLine()) != null) {
                AppLogger.info("yt-dlp: " + line);
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    int progress = (int) Float.parseFloat(matcher.group(1));
                    callback.onProgress(line.trim(), progress);
                } else {
                    callback.onProgress(line.trim(), null);
                }
            }
        }

        return process.waitFor();
    }

    private void checkAndDownloadYTDLP() throws IOException {
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
                    AppLogger.error("MediaDownloadService: failed to get yt-dlp version: " + e.getMessage());
                    versionToWrite = "unknown";
                }
            }
            writeVersionFile(versionToWrite != null ? versionToWrite : "unknown");
            AppLogger.info("yt-dlp updated to: " + versionToWrite);
        }
    }

    private String getLatestYtDlpVersion() {
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

    private String readVersionFile() {
        if (!YTDLP_VERSION_FILE.exists()) return null;
        try {
            return Files.readString(YTDLP_VERSION_FILE.toPath()).trim();
        } catch (IOException e) {
            AppLogger.error("Error reading version file: " + e.getMessage());
            return null;
        }
    }

    private void writeVersionFile(String version) {
        try {
            Files.writeString(YTDLP_VERSION_FILE.toPath(), version);
        } catch (IOException e) {
            AppLogger.error("Error writing version file: " + e.getMessage());
        }
    }

    public void checkAndDownloadFFMPEG() throws IOException {
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

    private void extractFfmpegFromZip(File zipFile) throws IOException {
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
                 FileOutputStream fos = new FileOutputStream(FFMPEG_EXE)) {
                is.transferTo(fos);
            }
            AppLogger.info("ffmpeg.exe extracted from: " + candidate.getName());
        }
    }

    private void cleanupOldFfmpegExtracts() {
        File[] files = FFMPEG_DIR.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.getName().equalsIgnoreCase("ffmpeg.exe") || f.getName().equalsIgnoreCase("ffmpeg.zip"))
                continue;
            deleteRecursivelyQuiet(f);
        }
    }

    private void deleteRecursivelyQuiet(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null)
                for (File c : children) deleteRecursivelyQuiet(c);
        }
        try {
            f.delete();
        } catch (Exception e) {
            AppLogger.error("MediaDownloadService: failed to delete " + f + ": " + e.getMessage());
        }
    }
}

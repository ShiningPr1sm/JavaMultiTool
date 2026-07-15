package util;

import service.MediaDownloadService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FFmpegUtils {
    private static Boolean available;
    private static String ffmpegPath;

    public static boolean isAvailable() {
        if (available != null) return available;

        try {
            Process p = new ProcessBuilder("ffmpeg", "-version")
                    .redirectErrorStream(true).start();
            if (p.waitFor() == 0) {
                ffmpegPath = "ffmpeg";
                available = true;
                return true;
            }
        } catch (Exception ignored) {}

        if (MediaDownloadService.FFMPEG_EXE.exists()) {
            ffmpegPath = MediaDownloadService.FFMPEG_EXE.getAbsolutePath();
            available = true;
            return true;
        }

        try {
            new MediaDownloadService().checkAndDownloadFFMPEG();
            if (MediaDownloadService.FFMPEG_EXE.exists()) {
                ffmpegPath = MediaDownloadService.FFMPEG_EXE.getAbsolutePath();
                available = true;
                return true;
            }
        } catch (Exception ignored) {}

        available = false;
        return false;
    }

    public static boolean convert(File input, File output, String format, int quality) {
        if (!isAvailable()) return false;
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(ffmpegPath);
            cmd.add("-y");
            cmd.add("-i");
            cmd.add(input.getAbsolutePath());

            switch (format) {
                case "png" -> {
                    cmd.add("-compression_level");
                    cmd.add(String.valueOf(Math.max(0, Math.min(9, 100 - quality) / 11)));
                }
                case "webp", "avif", "jpg", "jpeg" -> {
                    cmd.add("-q:v");
                    cmd.add(String.valueOf(Math.max(0, Math.min(100, quality))));
                }
            }

            cmd.add(output.getAbsolutePath());

            Process p = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();
            p.waitFor();
            return output.exists() && output.length() > 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
}

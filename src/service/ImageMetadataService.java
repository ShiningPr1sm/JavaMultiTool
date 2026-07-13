package service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.icc.IccDirectory;
import com.drew.metadata.file.FileTypeDirectory;

import javax.imageio.*;
import javax.imageio.stream.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

public class ImageMetadataService {

    public ImageMetadataResult analyzeImage(File file) throws Exception {
        Metadata metadata = ImageMetadataReader.readMetadata(file);
        BufferedImage img = ImageIO.read(file);
        if (img == null) throw new Exception("Invalid image format");

        int orientation = 1;
        ExifIFD0Directory exifIFD0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (exifIFD0 != null && exifIFD0.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
            orientation = exifIFD0.getInt(ExifIFD0Directory.TAG_ORIENTATION);
        }
        BufferedImage processedImage = rotateImage(img, orientation);
        Color avgColor = getAverageColor(processedImage);

        StringBuilder sb = new StringBuilder();

        sb.append("[ FILE INFO ]\n");
        sb.append("Name: ").append(file.getName()).append("\n");
        FileTypeDirectory ftDir = metadata.getFirstDirectoryOfType(FileTypeDirectory.class);
        if (ftDir != null)
            sb.append("Type: ").append(ftDir.getDescription(FileTypeDirectory.TAG_DETECTED_FILE_TYPE_LONG_NAME)).append("\n");
        sb.append("Size: ").append(String.format("%.2f MB", file.length() / (1024.0 * 1024.0))).append("\n");
        sb.append("Resolution: ").append(processedImage.getWidth()).append(" x ").append(processedImage.getHeight()).append("\n\n");

        if (exifIFD0 != null) {
            sb.append("[ DEVICE ]\n");
            sb.append("Maker: ").append(exifIFD0.getDescription(ExifIFD0Directory.TAG_MAKE)).append("\n");
            sb.append("Model: ").append(exifIFD0.getDescription(ExifIFD0Directory.TAG_MODEL)).append("\n");
            sb.append("Software: ").append(exifIFD0.getDescription(ExifIFD0Directory.TAG_SOFTWARE)).append("\n\n");
        }

        ExifSubIFDDirectory subIFD = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (subIFD != null) {
            sb.append("[ SHOOTING SETTINGS ]\n");
            sb.append("Exposure: ").append(subIFD.getDescription(ExifSubIFDDirectory.TAG_EXPOSURE_TIME)).append("\n");
            sb.append("Aperture: ").append(subIFD.getDescription(ExifSubIFDDirectory.TAG_FNUMBER)).append("\n");
            sb.append("ISO: ").append(subIFD.getDescription(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT)).append("\n");
            sb.append("White Balance: ").append(subIFD.getDescription(ExifSubIFDDirectory.TAG_WHITE_BALANCE)).append("\n");
            sb.append("Flash: ").append(subIFD.getDescription(ExifSubIFDDirectory.TAG_FLASH)).append("\n");
            sb.append("Lens: ").append(subIFD.getDescription(ExifSubIFDDirectory.TAG_LENS_MODEL)).append("\n");
            sb.append("Date: ").append(subIFD.getDescription(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)).append("\n\n");
        }

        String mapUrl = null;
        GpsDirectory gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        if (gpsDir != null && gpsDir.getGeoLocation() != null) {
            double lat = gpsDir.getGeoLocation().getLatitude();
            double lon = gpsDir.getGeoLocation().getLongitude();
            mapUrl = "https://www.google.com/maps?q=" + lat + "," + lon;
            sb.append("[ GEOLOCATION ]\n");
            sb.append("Coordinates: ").append(String.format("%.6f, %.6f", lat, lon)).append("\n");
            sb.append("Altitude: ").append(gpsDir.getDescription(GpsDirectory.TAG_ALTITUDE)).append("\n\n");
        }

        IccDirectory iccDir = metadata.getFirstDirectoryOfType(IccDirectory.class);
        if (iccDir != null) {
            sb.append("[ COLOR PROFILE ]\n");
            String profileDesc = iccDir.getDescription(0x64657363);
            sb.append("Profile: ").append(profileDesc != null ? profileDesc : "Unknown").append("\n");
            String colorSpace = iccDir.getDescription(0x636c7220);
            sb.append("Space: ").append(colorSpace != null ? colorSpace : "Unknown").append("\n\n");
        }

        List<Color> dominantColors = extractDominantColors(processedImage, 10);

        return new ImageMetadataResult(sb.toString(), processedImage, avgColor, mapUrl, dominantColors);
    }

    public void saveWithoutExif(BufferedImage img, File source, File target) throws IOException {
        String name = source.getName().toLowerCase();
        String format;
        if (name.endsWith(".png")) format = "png";
        else if (name.endsWith(".webp")) format = "webp";
        else if (name.endsWith(".bmp")) format = "bmp";
        else if (name.endsWith(".gif")) format = "gif";
        else format = "jpg";
        ImageIO.write(img, format, target);
    }

    public List<Color> extractDominantColors(BufferedImage img, int count) {
        int step = Math.max(1, Math.min(img.getWidth(), img.getHeight()) / 100);
        int bins = 8;
        int shift = 8 / 3;
        Map<Integer, long[]> colorMap = new HashMap<>();

        for (int x = 0; x < img.getWidth(); x += step) {
            for (int y = 0; y < img.getHeight(); y += step) {
                Color c = new Color(img.getRGB(x, y));
                int ri = c.getRed() >> (8 - shift);
                int gi = c.getGreen() >> (8 - shift);
                int bi = c.getBlue() >> (8 - shift);
                int key = (ri << (shift * 2)) | (gi << shift) | bi;
                colorMap.computeIfAbsent(key, k -> new long[]{0, 0, 0, 0});
                long[] sums = colorMap.get(key);
                sums[0] += c.getRed();
                sums[1] += c.getGreen();
                sums[2] += c.getBlue();
                sums[3]++;
            }
        }

        return colorMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[3], a.getValue()[3]))
                .limit(count)
                .map(e -> {
                    long[] sums = e.getValue();
                    int r = (int) (sums[0] / sums[3]);
                    int g = (int) (sums[1] / sums[3]);
                    int b = (int) (sums[2] / sums[3]);
                    return new Color(r, g, b);
                })
                .toList();
    }

    public Image getScaledImage(BufferedImage src, int w, int h) {
        double ratio = Math.min((double) w / src.getWidth(), (double) h / src.getHeight());
        return src.getScaledInstance((int) (src.getWidth() * ratio), (int) (src.getHeight() * ratio), Image.SCALE_SMOOTH);
    }

    private BufferedImage rotateImage(BufferedImage img, int orientation) {
        if (orientation <= 1)
            return img;
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage rotated;
        if (orientation == 6 || orientation == 8)
            rotated = new BufferedImage(h, w, img.getType());
        else
            rotated = new BufferedImage(w, h, img.getType());

        Graphics2D g2 = rotated.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        if (orientation == 6) {
            g2.translate(h, 0);
            g2.rotate(Math.toRadians(90));
        } else if (orientation == 3) {
            g2.translate(w, h);
            g2.rotate(Math.toRadians(180));
        } else if (orientation == 8) {
            g2.translate(0, w);
            g2.rotate(Math.toRadians(270));
        }

        g2.drawImage(img, 0, 0, null);
        g2.dispose();
        return rotated;
    }

    private Color getAverageColor(BufferedImage img) {
        long r = 0, g = 0, b = 0, count = 0;
        for (int x = 0; x < img.getWidth(); x += 20) {
            for (int y = 0; y < img.getHeight(); y += 20) {
                Color c = new Color(img.getRGB(x, y));
                r += c.getRed();
                g += c.getGreen();
                b += c.getBlue();
                count++;
            }
        }
        return new Color((int) (r / count), (int) (g / count), (int) (b / count));
    }

    public ElaResult elaAnalysis(BufferedImage img) throws IOException {
        int w = img.getWidth(), h = img.getHeight();

        if (w * h > 2000 * 2000) {
            double scale = Math.sqrt((2000.0 * 2000.0) / (w * h));
            w = (int) (w * scale);
            h = (int) (h * scale);
            BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            g.drawImage(img, 0, 0, w, h, null);
            g.dispose();
            img = scaled;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.95f);
        writer.setOutput(ImageIO.createImageOutputStream(baos));
        writer.write(null, new IIOImage(img, null, null), param);
        writer.dispose();

        BufferedImage recompressed = ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));

        BufferedImage elaMap = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        double totalDiff = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Color orig = new Color(img.getRGB(x, y));
                Color recomp = new Color(recompressed.getRGB(x, y));

                int dr = Math.abs(orig.getRed() - recomp.getRed());
                int dg = Math.abs(orig.getGreen() - recomp.getGreen());
                int db = Math.abs(orig.getBlue() - recomp.getBlue());
                int maxDiff = Math.max(dr, Math.max(dg, db));

                totalDiff += maxDiff;

                int ela = Math.min(maxDiff * 15, 255);
                elaMap.setRGB(x, y, 0xFF000000 | (ela << 16) | (ela << 8) | ela);
            }
        }

        double avgDiff = totalDiff / (w * h);
        return new ElaResult(elaMap, avgDiff);
    }

    public record ElaResult(BufferedImage elaImage, double score) {}

    public static class ImageMetadataResult {
        private final String metadataText;
        private final BufferedImage processedImage;
        private final Color averageColor;
        private final String mapUrl;
        private final List<Color> dominantColors;

        public ImageMetadataResult(String metadataText, BufferedImage processedImage, Color averageColor, String mapUrl, List<Color> dominantColors) {
            this.metadataText = metadataText;
            this.processedImage = processedImage;
            this.averageColor = averageColor;
            this.mapUrl = mapUrl;
            this.dominantColors = dominantColors;
        }

        public String getMetadataText() {
            return metadataText;
        }

        public BufferedImage getProcessedImage() {
            return processedImage;
        }

        public Color getAverageColor() {
            return averageColor;
        }

        public String getMapUrl() {
            return mapUrl;
        }

        public List<Color> getDominantColors() {
            return dominantColors;
        }

        public String getMetadataJson() {
            StringBuilder j = new StringBuilder();
            j.append("{\n");
            String[] lines = metadataText.split("\n");
            String section = "";
            for (String line : lines) {
                if (line.startsWith("[") && line.endsWith("]")) {
                    section = line.substring(1, line.length() - 1).trim().toLowerCase().replace(" ", "_");
                    continue;
                }
                if (line.isBlank()) continue;
                int colon = line.indexOf(": ");
                if (colon > 0) {
                    String key = line.substring(0, colon).trim().toLowerCase().replace(" ", "_");
                    String val = line.substring(colon + 2).trim();
                    j.append("  \"").append(section).append(".").append(key).append("\": \"").append(escapeJson(val)).append("\",\n");
                }
            }
            if (j.length() > 1) j.setLength(j.length() - 2);
            j.append("\n}");
            return j.toString();
        }

        private String escapeJson(String s) {
            return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
}

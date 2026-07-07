package service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.icc.IccDirectory;
import com.drew.metadata.file.FileTypeDirectory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

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

        return new ImageMetadataResult(sb.toString(), processedImage, avgColor, mapUrl);
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

    public static class ImageMetadataResult {
        private final String metadataText;
        private final BufferedImage processedImage;
        private final Color averageColor;
        private final String mapUrl;

        public ImageMetadataResult(String metadataText, BufferedImage processedImage, Color averageColor, String mapUrl) {
            this.metadataText = metadataText;
            this.processedImage = processedImage;
            this.averageColor = averageColor;
            this.mapUrl = mapUrl;
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
    }
}

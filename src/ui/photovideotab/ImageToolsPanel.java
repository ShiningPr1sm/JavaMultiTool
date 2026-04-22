package ui.photovideotab;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.icc.IccDirectory;
import com.drew.metadata.file.FileTypeDirectory;
import ui.UIStyle;
import ui.utils.AppLogger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class ImageToolsPanel extends JPanel {
    private final JLabel imagePreview;
    private final JTextArea infoArea;
    private JButton openMapBtn;

    public ImageToolsPanel() {
        setLayout(new BorderLayout(15, 10));
        setBackground(UIStyle.BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        imagePreview = new JLabel("Drag & Drop Image Here", SwingConstants.CENTER);
        imagePreview.setPreferredSize(new Dimension(500, 0));
        imagePreview.setBorder(BorderFactory.createDashedBorder(UIStyle.BORDER_COLOR, 5, 5));
        imagePreview.setForeground(Color.GRAY);
        imagePreview.setFont(new Font("Segoe UI", Font.BOLD, 20));

        setupDragAndDrop();

        infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setBackground(UIStyle.SECONDARY_BG);
        infoArea.setForeground(Color.WHITE);
        infoArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollInfo = new JScrollPane(infoArea);
        scrollInfo.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));
        UIStyle.styleScrollBar(scrollInfo);

        JPanel rightHeader = new JPanel(new BorderLayout());
        rightHeader.setOpaque(false);

        rightHeader.setPreferredSize(new Dimension(420, 35));

        rightHeader.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        JLabel metaTitle = new JLabel("IMAGE METADATA");
        metaTitle.setForeground(Color.GRAY);
        metaTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));

        metaTitle.setVerticalAlignment(SwingConstants.CENTER);

        openMapBtn = new JButton("Open in Maps");
        UIStyle.styleButton(openMapBtn);
        openMapBtn.setVisible(false);

        rightHeader.add(metaTitle, BorderLayout.WEST);
        rightHeader.add(openMapBtn, BorderLayout.EAST);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(420, 0));
        rightPanel.add(rightHeader, BorderLayout.NORTH);
        rightPanel.add(scrollInfo, BorderLayout.CENTER);

        add(imagePreview, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
    }


    private void setupDragAndDrop() {
        new DropTarget(imagePreview, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    java.awt.datatransfer.Transferable t = dtde.getTransferable();
                    if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                        if (!files.isEmpty()) processImage(files.get(0));
                    }
                } catch (Exception ex) {
                    AppLogger.error("DND Error: " + ex.getMessage());
                }
            }
        });
    }

    private void processImage(File file) {
        AppLogger.info("Analyzing image: " + file.getName());
        infoArea.setText("Extracting metadata...");
        openMapBtn.setVisible(false);

        new Thread(() -> {
            try {
                Metadata metadata = ImageMetadataReader.readMetadata(file);
                BufferedImage img = ImageIO.read(file);
                if (img == null) throw new Exception("Invalid image format");

                int orientation = 1;
                ExifIFD0Directory exifIFD0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
                if (exifIFD0 != null && exifIFD0.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                    orientation = exifIFD0.getInt(ExifIFD0Directory.TAG_ORIENTATION);
                }
                BufferedImage finalImg = rotateImage(img, orientation);
                Color avgColor = getAverageColor(finalImg);
                StringBuilder sb = new StringBuilder();

                sb.append("[ FILE INFO ]\n");
                sb.append("Name: ").append(file.getName()).append("\n");
                FileTypeDirectory ftDir = metadata.getFirstDirectoryOfType(FileTypeDirectory.class);
                if (ftDir != null) sb.append("Type: ").append(ftDir.getDescription(FileTypeDirectory.TAG_DETECTED_FILE_TYPE_LONG_NAME)).append("\n");
                sb.append("Size: ").append(String.format("%.2f MB", file.length() / (1024.0 * 1024.0))).append("\n");
                sb.append("Resolution: ").append(finalImg.getWidth()).append(" x ").append(finalImg.getHeight()).append("\n\n");

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

                GpsDirectory gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory.class);
                final String mapUrl;
                if (gpsDir != null && gpsDir.getGeoLocation() != null) {
                    double lat = gpsDir.getGeoLocation().getLatitude();
                    double lon = gpsDir.getGeoLocation().getLongitude();
                    mapUrl = "https://www.google.com/maps?q=" + lat + "," + lon;
                    sb.append("[ GEOLOCATION ]\n");
                    sb.append("Coordinates: ").append(String.format("%.6f, %.6f", lat, lon)).append("\n");
                    sb.append("Altitude: ").append(gpsDir.getDescription(GpsDirectory.TAG_ALTITUDE)).append("\n\n");
                } else { mapUrl = null; }

                IccDirectory iccDir = metadata.getFirstDirectoryOfType(IccDirectory.class);
                if (iccDir != null) {
                    sb.append("[ COLOR PROFILE ]\n");

                    // 0x64657363 - код для TAG_PROFILE_DESCRIPTION
                    String profileDesc = iccDir.getDescription(0x64657363);
                    sb.append("Profile: ").append(profileDesc != null ? profileDesc : "Unknown").append("\n");

                    // 0x636c7220 - код для TAG_COLOR_SPACE
                    String colorSpace = iccDir.getDescription(0x636c7220);
                    sb.append("Space: ").append(colorSpace != null ? colorSpace : "Unknown").append("\n\n");
                }

                SwingUtilities.invokeLater(() -> {
                    infoArea.setText(sb.toString());
                    infoArea.setCaretPosition(0);
                    infoArea.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 5, 0, 0, avgColor),
                            BorderFactory.createEmptyBorder(10, 10, 10, 10)
                    ));

                    if (mapUrl != null) {
                        openMapBtn.setVisible(true);
                        for (var al : openMapBtn.getActionListeners()) openMapBtn.removeActionListener(al);
                        openMapBtn.addActionListener(_ -> {
                            try { Desktop.getDesktop().browse(new java.net.URI(mapUrl)); } catch (Exception ignored) {}
                        });
                    }

                    Image scaled = getScaledImage(finalImg, imagePreview.getWidth(), imagePreview.getHeight());
                    imagePreview.setIcon(new ImageIcon(scaled));
                    imagePreview.setText("");
                });

            } catch (Exception e) {
                AppLogger.error("Image Processing Error: " + e.getMessage());
                SwingUtilities.invokeLater(() -> infoArea.setText("Failed to process image: " + e.getMessage()));
            }
        }).start();
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
        }
        else if (orientation == 3) {
            g2.translate(w, h);
            g2.rotate(Math.toRadians(180));
        }
        else if (orientation == 8) {
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
                r += c.getRed(); g += c.getGreen(); b += c.getBlue();
                count++;
            }
        }
        return new Color((int)(r/count), (int)(g/count), (int)(b/count));
    }

    private Image getScaledImage(BufferedImage src, int w, int h) {
        double ratio = Math.min((double) w / src.getWidth(), (double) h / src.getHeight());
        return src.getScaledInstance((int)(src.getWidth()*ratio), (int)(src.getHeight()*ratio), Image.SCALE_SMOOTH);
    }
}
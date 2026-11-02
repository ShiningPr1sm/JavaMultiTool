package ui.settings;

import util.AppLogger;
import util.I18n;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;

public class AvatarCropperDialog extends JDialog {
    private static final double MIN_ZOOM = 0.5;
    private static final double MAX_ZOOM = 8.0;
    private static final double ZOOM_STEP = 1.1;

    private final BufferedImage originalImage;
    private BufferedImage croppedImage;
    private final Rectangle selection;
    private final Point dragOffset = new Point();
    private boolean dragging = false;
    private double scale = 1.0;
    private double zoom = 1.0;
    private final int displayWidth;
    private final int displayHeight;

    public AvatarCropperDialog(Frame owner, BufferedImage image) {
        super(owner, I18n.t("settings.cropTitle"), true);
        this.originalImage = image;

        int maxSize = 500;
        int tempWidth = image.getWidth();
        int tempHeight = image.getHeight();
        if (tempWidth > maxSize || tempHeight > maxSize) {
            double widthScale = maxSize / (double) tempWidth;
            double heightScale = maxSize / (double) tempHeight;
            scale = Math.min(widthScale, heightScale);
            tempWidth = (int) (tempWidth * scale);
            tempHeight = (int) (tempHeight * scale);
        }

        displayWidth = tempWidth;
        displayHeight = tempHeight;

        int squareSize = Math.min(displayWidth, displayHeight) / 2;
        this.selection = new Rectangle((displayWidth - squareSize) / 2, (displayHeight - squareSize) / 2, squareSize, squareSize);

        JLabel imageLabel = getJLabel();

        JButton cropButton = new JButton(I18n.t("settings.cropSave"));
        cropButton.addActionListener(e -> {
            try {
                int sx = (int) ((selection.x - offsetX()) / effectiveScale());
                int sy = (int) ((selection.y - offsetY()) / effectiveScale());
                int sw = (int) (selection.width / effectiveScale());
                int sh = (int) (selection.height / effectiveScale());

                int imgW = originalImage.getWidth();
                int imgH = originalImage.getHeight();
                sx = Math.max(0, Math.min(sx, imgW - 1));
                sy = Math.max(0, Math.min(sy, imgH - 1));
                sw = Math.max(1, Math.min(sw, imgW - sx));
                sh = Math.max(1, Math.min(sh, imgH - sy));

                croppedImage = originalImage.getSubimage(sx, sy, sw, sh);
                AppLogger.info("Avatar cropped (" + sw + "x" + sh + ").");
                dispose();
            } catch (RasterFormatException ex) {
                AppLogger.error("Avatar cropper: invalid crop area - " + ex.getMessage());
                JOptionPane.showMessageDialog(this, I18n.t("settings.cropBadArea"), I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
            }
        });

        JScrollPane scrollPane = new JScrollPane(imageLabel);
        scrollPane.getViewport().setBackground(Color.DARK_GRAY);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(cropButton, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    private JLabel getJLabel() {
        JLabel imageLabel = new JLabel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(displayWidth, displayHeight);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                int w = drawWidth();
                int h = drawHeight();
                int ox = offsetX();
                int oy = offsetY();
                g2.drawImage(originalImage, ox, oy, ox + w, oy + h,
                        0, 0, originalImage.getWidth(), originalImage.getHeight(), null);

                g2.setColor(new Color(0, 0, 0, 100));
                Shape clip = g2.getClip();
                Rectangle outer = new Rectangle(0, 0, displayWidth, displayHeight);
                Area area = new Area(outer);
                area.subtract(new Area(selection));
                g2.fill(area);
                g2.setClip(clip);

                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2));
                g2.drawRect(selection.x, selection.y, selection.width, selection.height);
            }
        };

        imageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (selection.contains(e.getPoint())) {
                    dragging = true;
                    dragOffset.x = e.getX() - selection.x;
                    dragOffset.y = e.getY() - selection.y;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragging = false;
            }
        });

        imageLabel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragging) {
                    int newX = e.getX() - dragOffset.x;
                    int newY = e.getY() - dragOffset.y;
                    newX = Math.max(offsetX(), Math.min(newX, offsetX() + drawWidth() - selection.width));
                    newY = Math.max(offsetY(), Math.min(newY, offsetY() + drawHeight() - selection.height));
                    selection.setLocation(newX, newY);
                    imageLabel.repaint();
                }
            }
        });

        imageLabel.addMouseWheelListener(e -> {
            double factor = (e.getWheelRotation() < 0) ? ZOOM_STEP : 1.0 / ZOOM_STEP;
            zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * factor));
            clampSelection();
            imageLabel.repaint();
        });

        return imageLabel;
    }

    private int drawWidth() {
        return (int) (displayWidth * zoom);
    }

    private int drawHeight() {
        return (int) (displayHeight * zoom);
    }

    private int offsetX() {
        return (displayWidth - drawWidth()) / 2;
    }

    private int offsetY() {
        return (displayHeight - drawHeight()) / 2;
    }

    private double effectiveScale() {
        return scale * zoom;
    }

    private void clampSelection() {
        int maxX = offsetX() + drawWidth() - selection.width;
        int maxY = offsetY() + drawHeight() - selection.height;
        selection.x = Math.max(offsetX(), Math.min(selection.x, maxX));
        selection.y = Math.max(offsetY(), Math.min(selection.y, maxY));
    }

    public static BufferedImage showCropDialog(Component parent, BufferedImage img) {
        AvatarCropperDialog dialog = new AvatarCropperDialog((Frame) SwingUtilities.getWindowAncestor(parent), img);
        dialog.setVisible(true);
        return dialog.croppedImage;
    }
}
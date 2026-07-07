package ui.settings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;

public class AvatarCropperDialog extends JDialog {
    private final BufferedImage originalImage;
    private BufferedImage croppedImage;
    private final Rectangle selection;
    private final Point dragOffset = new Point();
    private boolean dragging = false;
    private double scale = 1.0;
    private final int displayWidth;
    private final int displayHeight;

    public AvatarCropperDialog(Frame owner, BufferedImage image) {
        super(owner, "Crop Avatar", true);
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

        JButton cropButton = new JButton("Crop and Save");
        cropButton.addActionListener(_ -> {
            try {
                int sx = (int) (selection.x / scale);
                int sy = (int) (selection.y / scale);
                int sw = (int) (selection.width / scale);
                int sh = (int) (selection.height / scale);

                croppedImage = originalImage.getSubimage(sx, sy, sw, sh);
                dispose();
            } catch (RasterFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid crop area.", "Error", JOptionPane.ERROR_MESSAGE);
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
                g2.drawImage(originalImage, 0, 0, displayWidth, displayHeight, null);

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
                    newX = Math.max(0, Math.min(newX, displayWidth - selection.width));
                    newY = Math.max(0, Math.min(newY, displayHeight - selection.height));
                    selection.setLocation(newX, newY);
                    imageLabel.repaint();
                }
            }
        });
        return imageLabel;
    }

    public static BufferedImage showCropDialog(Component parent, BufferedImage img) {
        AvatarCropperDialog dialog = new AvatarCropperDialog((Frame) SwingUtilities.getWindowAncestor(parent), img);
        dialog.setVisible(true);
        return dialog.croppedImage;
    }
}
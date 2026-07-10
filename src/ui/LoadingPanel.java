package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Objects;
import javax.imageio.ImageIO;
import util.AppLogger;

public class LoadingPanel extends JPanel {
    private float alpha = 1f;
    private BufferedImage loadingImage;
    private double rotationAngle = 0.0;
    private final Timer rotationTimer;

    public LoadingPanel() {
        setLayout(new BorderLayout());
        setBackground(UIStyle.BG_COLOR);

        try {
            try {
                BufferedImage original = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/icons/menu/loading.png")));
                int targetWidth = 100;
                int targetHeight = 100;

                Image scaledImage = original.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
                loadingImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);

                Graphics2D g2d = loadingImage.createGraphics();
                g2d.drawImage(scaledImage, 0, 0, null);
                g2d.dispose();
            } catch (Exception e) {
                AppLogger.error("LoadingPanel: failed to load loading image: " + e.getMessage());
            }

        } catch (Exception e) {
            AppLogger.error("LoadingPanel: error: " + e.getMessage());
        }

        // Таймер для анимации вращения картинки
        rotationTimer = new Timer(15, e -> {
            rotationAngle += Math.toRadians(3);
            repaint();
        });
        rotationTimer.start();
    }

    public void fadeOut(Runnable onFinished) {
        Timer fadeTimer = new Timer(15, null);
        fadeTimer.addActionListener(e -> {
            alpha -= 0.05f;
            if (alpha <= 0f) {
                alpha = 0f;
                fadeTimer.stop();
                rotationTimer.stop();
                if (onFinished != null) onFinished.run();
            }
            repaint();
        });
        fadeTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (loadingImage != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            int imgWidth = loadingImage.getWidth();
            int imgHeight = loadingImage.getHeight();
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;

            AffineTransform transform = new AffineTransform();
            transform.translate(centerX, centerY);
            transform.rotate(rotationAngle);
            transform.translate((double) -imgWidth / 2, (double) -imgHeight / 2);

            g2.drawImage(loadingImage, transform, null);
            g2.dispose();
        }
    }
}

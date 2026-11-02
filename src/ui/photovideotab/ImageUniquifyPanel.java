package ui.photovideotab;

import ui.UIStyle;
import util.AppLogger;
import util.I18n;
import util.JavaFxFileChooser;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class ImageUniquifyPanel extends JPanel {
    private final JSlider noiseSlider;
    private final JLabel noiseLabel;
    private final JLabel outDirLabel;
    private final JButton processBtn, selectBtn;
    private final JTextArea logArea;
    private File selectedFile;
    private File outputDir;

    public ImageUniquifyPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UIStyle.BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel top = new JPanel(new GridBagLayout());
        top.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = GridBagConstraints.RELATIVE;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 5, 4, 5);

        selectBtn = new JButton(I18n.t("imgs.selectImage"));
        UIStyle.styleButton(selectBtn);
        selectBtn.addActionListener(e -> selectFile());
        top.add(selectBtn, c);

        noiseSlider = new JSlider(0, 100, 15);
        UIStyle.styleSlider(noiseSlider);
        noiseSlider.setPreferredSize(new Dimension(200, 22));
        noiseLabel = new JLabel(I18n.t("uniq.noise", "15"));
        noiseLabel.setForeground(UIStyle.TEXT_COLOR);
        noiseLabel.setPreferredSize(new Dimension(160, 22));
        noiseSlider.addChangeListener(e -> noiseLabel.setText(
                I18n.t("uniq.noise", String.valueOf(noiseSlider.getValue()))));
        JPanel noiseRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        noiseRow.setOpaque(false);
        noiseRow.add(noiseLabel);
        noiseRow.add(noiseSlider);
        top.add(noiseRow, c);

        JPanel outRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        outRow.setOpaque(false);
        JButton outDirBtn = new JButton(I18n.t("imgs.outputFolder"));
        UIStyle.styleButton(outDirBtn);
        outDirBtn.addActionListener(e -> chooseOutputDir());
        outDirLabel = new JLabel(I18n.t("imgs.notSelected"));
        outDirLabel.setForeground(UIStyle.TEXT_COLOR);
        outRow.add(outDirBtn);
        outRow.add(outDirLabel);
        top.add(outRow, c);

        processBtn = new JButton(I18n.t("uniq.process"));
        UIStyle.styleButton(processBtn);
        processBtn.setEnabled(false);
        processBtn.addActionListener(e -> process());
        top.add(processBtn, c);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(UIStyle.SECONDARY_BG);
        logArea.setForeground(Color.WHITE);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setMargin(new Insets(8, 8, 8, 8));
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));
        UIStyle.styleScrollBar(scroll);

        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    private void selectFile() {
        File file = JavaFxFileChooser.openFile(I18n.t("imgs.selectImage"),
                new javafx.stage.FileChooser.ExtensionFilter(
                        I18n.t("imgs.filter"),
                        "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.webp"));
        if (file == null) return;
        selectedFile = file;
        AppLogger.info("ImageUniquify: selected - " + selectedFile.getAbsolutePath());
        log(I18n.t("imgs.selectedOne", selectedFile.getName()));
        selectBtn.setText(selectedFile.getName());
        processBtn.setEnabled(outputDir != null);
    }

    private void chooseOutputDir() {
        File dir = JavaFxFileChooser.chooseDirectory(I18n.t("imgs.chooseDir"));
        if (dir == null) return;
        outputDir = dir;
        outDirLabel.setText(outputDir.getName());
        AppLogger.info("ImageUniquify: output dir - " + outputDir);
        log(I18n.t("imgs.outputLog", outputDir.getAbsolutePath()));
        processBtn.setEnabled(selectedFile != null);
    }

    private void process() {
        if (selectedFile == null || outputDir == null) return;
        int intensity = noiseSlider.getValue();

        processBtn.setEnabled(false);
        selectBtn.setEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    BufferedImage original = ImageIO.read(selectedFile);
                    if (original == null) {
                        AppLogger.error("ImageUniquify: unsupported format - " + selectedFile.getName());
                        return null;
                    }
                    BufferedImage processed = uniquify(original, intensity);

                    String name = selectedFile.getName();
                    int dot = name.lastIndexOf('.');
                    String base = dot > 0 ? name.substring(0, dot) : name;
                    File outFile = new File(outputDir, base + "_unique.jpg");
                    ImageIO.write(processed, "jpg", outFile);
                    AppLogger.info("ImageUniquify: processed - " + outFile.getAbsolutePath());
                } catch (IOException ex) {
                    AppLogger.error("ImageUniquify failed: " + selectedFile.getName() + " - " + ex.getMessage());
                }
                return null;
            }

            @Override
            protected void done() {
                processBtn.setEnabled(true);
                selectBtn.setEnabled(true);
            }
        }.execute();
    }

    public static BufferedImage uniquify(BufferedImage src, int intensity) {
        int width = src.getWidth();
        int height = src.getHeight();

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = result.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double angleRad = Math.toRadians(3.5);
        double scale = 1.08;

        AffineTransform transform = new AffineTransform();
        transform.translate(width / 2.0, height / 2.0);
        transform.rotate(angleRad);
        transform.scale(scale, scale);
        transform.translate(-width / 2.0, -height / 2.0);

        g2d.drawImage(src, transform, null);
        g2d.setColor(new Color(255, 200, 150, 12));
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();

        applyPixelNoise(result, intensity);
        return result;
    }

    private static void applyPixelNoise(BufferedImage img, int intensity) {
        int width = img.getWidth();
        int height = img.getHeight();
        Random random = new Random();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                int noise = random.nextInt(intensity * 2 + 1) - intensity;
                r = Math.min(255, Math.max(0, r + noise));
                g = Math.min(255, Math.max(0, g + noise));
                b = Math.min(255, Math.max(0, b + noise));

                img.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
    }

    private void log(String s) {
        logArea.append(s + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}

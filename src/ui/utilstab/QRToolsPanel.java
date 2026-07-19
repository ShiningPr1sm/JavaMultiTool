package ui.utilstab;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import service.Services;
import ui.UIStyle;
import util.AppLogger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import static com.google.zxing.client.j2se.MatrixToImageWriter.toBufferedImage;

public class QRToolsPanel extends JPanel {
    private final JTextArea inputArea;
    private final JLabel qrPreview;
    private final JButton generateBtn;
    private final JButton decodeBtn;
    private final JTextField decodeResult;
    private final JButton downloadBtn;
    private BufferedImage lastQrImage;

    private final String login;
    private final Services services;

    public QRToolsPanel(Services services, String login) {
        this.login = login;
        this.services = services;
        setLayout(new BorderLayout(10, 10));
        setBackground(UIStyle.BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel left = new JPanel(new BorderLayout(5, 5));
        left.setOpaque(false);

        inputArea = new JTextArea(5, 30);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        inputArea.setBackground(UIStyle.SECONDARY_BG);
        inputArea.setForeground(UIStyle.TEXT_COLOR);
        inputArea.setCaretColor(Color.WHITE);
        inputArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.BORDER_COLOR),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        inputArea.setText("https://github.com/ShiningPr1sm/JavaMultiTool");

        generateBtn = new JButton("Generate QR");
        UIStyle.styleButton(generateBtn);
        generateBtn.addActionListener(e -> generateQR());

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setOpaque(false);
        JLabel textAndUrl = new JLabel("Text / URL: ");
        textAndUrl.setForeground(Color.white);
        inputPanel.add(textAndUrl, BorderLayout.NORTH);
        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setBorder(null);
        inputPanel.add(inputScroll, BorderLayout.CENTER);
        inputPanel.add(generateBtn, BorderLayout.SOUTH);

        left.add(inputPanel, BorderLayout.NORTH);

        JPanel decodePanel = new JPanel(new BorderLayout(5, 5));
        decodePanel.setOpaque(false);
        decodePanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        decodeBtn = new JButton("Decode QR from Image");
        UIStyle.styleButton(decodeBtn);
        decodeBtn.addActionListener(e -> decodeQR());

        decodeResult = new JTextField();
        decodeResult.setEditable(false);
        UIStyle.styleTextField(decodeResult);

        decodePanel.add(decodeBtn, BorderLayout.NORTH);
        decodePanel.add(decodeResult, BorderLayout.SOUTH);

        left.add(decodePanel, BorderLayout.SOUTH);

        add(left, BorderLayout.CENTER);

        qrPreview = new JLabel("", SwingConstants.CENTER);
        qrPreview.setPreferredSize(new Dimension(250, 250));
        qrPreview.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));

        downloadBtn = new JButton("Download");
        UIStyle.styleButton(downloadBtn);
        downloadBtn.setEnabled(false);
        downloadBtn.addActionListener(e -> downloadQR());

        JPanel right = new JPanel(new BorderLayout(5, 5));
        right.setOpaque(false);
        right.add(qrPreview, BorderLayout.CENTER);
        right.add(downloadBtn, BorderLayout.SOUTH);
        add(right, BorderLayout.EAST);
    }

    private void generateQR() {
        String text = inputArea.getText().trim();
        if (text.isEmpty()) return;

        new Thread(() -> {
            try {
                QRCodeWriter writer = new QRCodeWriter();
                BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 240, 240);
                BufferedImage img = toBufferedImage(matrix);
                lastQrImage = img;
                SwingUtilities.invokeLater(() -> {
                    qrPreview.setIcon(new ImageIcon(img));
                    qrPreview.setText("");
                    downloadBtn.setEnabled(true);
                });
            } catch (Exception ex) {
                AppLogger.error("QR generation failed: " + ex.getMessage());
            }
        }).start();
    }

    private void decodeQR() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        new Thread(() -> {
            try {
                BufferedImage original = ImageIO.read(fc.getSelectedFile());
                if (original == null) {
                    SwingUtilities.invokeLater(() -> decodeResult.setText("Invalid image"));
                    return;
                }

                String text = tryDecode(original);

                if (text == null) text = tryDecode(blur(original, 3));
                if (text == null) text = tryDecode(blur(original, 5));
                if (text == null) text = tryDecode(toGrayscale(original));
                if (text == null) text = tryDecode(blur(toGrayscale(original), 3));
                if (text == null) text = tryDecode(scale(original, 0.5));
                if (text == null) text = tryDecode(scale(original, 0.75));

                final String finalText = text;
                SwingUtilities.invokeLater(() -> decodeResult.setText(
                        finalText != null ? finalText : "Failed to decode QR"));
            } catch (Exception ex) {
                AppLogger.error("QR decoding failed: " + ex.getMessage());
                SwingUtilities.invokeLater(() -> decodeResult.setText("Failed to decode QR"));
            }
        }).start();
    }

    private String tryDecode(BufferedImage img) {
        if (img == null) return null;

        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

        LuminanceSource source = new BufferedImageLuminanceSource(img);

        try {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = new QRCodeReader().decode(bitmap, hints);
            return result.getText();
        } catch (NotFoundException | ChecksumException | FormatException ignored) {
        }
        try {
            BinaryBitmap bitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
            Result result = new QRCodeReader().decode(bitmap, hints);
            return result.getText();
        } catch (NotFoundException | ChecksumException | FormatException ignored) {
            return null;
        }
    }

    private BufferedImage blur(BufferedImage src, int radius) {
        int size = radius * 2 + 1;
        float weight = 1.0f / (size * size);
        float[] data = new float[size * size];
        Arrays.fill(data, weight);
        Kernel kernel = new Kernel(size, size, data);
        BufferedImageOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        return op.filter(src, null);
    }

    private BufferedImage toGrayscale(BufferedImage src) {
        BufferedImage gray = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return gray;
    }

    private void downloadQR() {
        if (lastQrImage == null) return;
        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss"));
        String filename = "QR_" + timestamp + ".png";
        String downloads = System.getProperty("user.home") + "/Downloads";
        File file = new File(downloads, filename);
        try {
            ImageIO.write(lastQrImage, "png", file);
            services.achievementService().complete(login, "qrcode");
        } catch (Exception ex) {
            AppLogger.error("QR download failed: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Failed to save QR", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private BufferedImage scale(BufferedImage src, double factor) {
        int w = (int) (src.getWidth() * factor);
        int h = (int) (src.getHeight() * factor);
        BufferedImage scaled = new BufferedImage(w, h, src.getType() == 0 ? BufferedImage.TYPE_INT_RGB : src.getType());
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return scaled;
    }
}
package ui.utilstab;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import ui.UIStyle;
import util.AppLogger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class QRToolsPanel extends JPanel {
    private final JTextArea inputArea;
    private final JLabel qrPreview;
    private final JButton generateBtn;
    private final JButton decodeBtn;
    private final JTextField decodeResult;

    public QRToolsPanel() {
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
        inputPanel.add(new JLabel("Text / URL:"), BorderLayout.NORTH);
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
        add(qrPreview, BorderLayout.EAST);
    }

    private void generateQR() {
        String text = inputArea.getText().trim();
        if (text.isEmpty()) return;

        new Thread(() -> {
            try {
                QRCodeWriter writer = new QRCodeWriter();
                BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 240, 240);
                BufferedImage img = toBufferedImage(matrix);
                SwingUtilities.invokeLater(() -> {
                    qrPreview.setIcon(new ImageIcon(img));
                    qrPreview.setText("");
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
                BufferedImage img = ImageIO.read(fc.getSelectedFile());
                if (img == null) {
                    SwingUtilities.invokeLater(() -> decodeResult.setText("Invalid image"));
                    return;
                }
                LuminanceSource source = new BufferedImageLuminanceSource(img);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                Result result = new QRCodeReader().decode(bitmap);
                SwingUtilities.invokeLater(() -> decodeResult.setText(result.getText()));
            } catch (Exception ex) {
                AppLogger.error("QR decoding failed: " + ex.getMessage());
                SwingUtilities.invokeLater(() -> decodeResult.setText("Failed to decode QR"));
            }
        }).start();
    }

    private BufferedImage toBufferedImage(BitMatrix matrix) {
        int w = matrix.getWidth(), h = matrix.getHeight();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                img.setRGB(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return img;
    }
}

package ui.photovideotab;

import ui.UIStyle;
import util.AppLogger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ResizerPanel extends JPanel {
    private final JTextField widthField, heightField;
    private final JCheckBox keepAspectBox;
    private final JSlider qualitySlider;
    private final JLabel qualityLabel;
    private final JButton selectBtn, processBtn;
    private final JTextArea logArea;
    private final JLabel outDirLabel;
    private final JCheckBox jpgOutputBox;
    private List<File> selectedFiles;
    private File outputDir;

    public ResizerPanel() {
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

        selectBtn = new JButton("Select Images");
        UIStyle.styleButton(selectBtn);
        selectBtn.addActionListener(e -> selectFiles());
        top.add(selectBtn, c);

        JPanel dims = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        dims.setOpaque(false);
        dims.add(new JLabel("W:") { { setForeground(UIStyle.TEXT_COLOR); } });
        widthField = new JTextField(5);
        UIStyle.styleTextField(widthField);
        widthField.setText("800");
        dims.add(widthField);
        dims.add(new JLabel("H:") { { setForeground(UIStyle.TEXT_COLOR); } });
        heightField = new JTextField(5);
        UIStyle.styleTextField(heightField);
        heightField.setText("600");
        dims.add(heightField);
        keepAspectBox = new JCheckBox("Keep Aspect Ratio");
        UIStyle.styleCheckbox(keepAspectBox);
        keepAspectBox.setSelected(true);
        dims.add(keepAspectBox);
        top.add(dims, c);

        qualitySlider = new JSlider(1, 100, 85);
        UIStyle.styleSlider(qualitySlider);
        qualitySlider.setPreferredSize(new Dimension(200, 22));
        qualityLabel = new JLabel("Quality: 85%");
        qualityLabel.setForeground(UIStyle.TEXT_COLOR);
        qualityLabel.setPreferredSize(new Dimension(110, 22));
        qualitySlider.addChangeListener(e -> qualityLabel.setText("Quality: " + qualitySlider.getValue() + "%"));
        JPanel qualRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        qualRow.setOpaque(false);
        qualRow.add(qualityLabel);
        qualRow.add(qualitySlider);
        top.add(qualRow, c);

        JPanel outRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        outRow.setOpaque(false);
        JButton outDirBtn = new JButton("Output Folder");
        UIStyle.styleButton(outDirBtn);
        outDirBtn.addActionListener(e -> chooseOutputDir());
        outDirLabel = new JLabel("Not selected");
        outDirLabel.setForeground(UIStyle.TEXT_COLOR);
        outRow.add(outDirBtn);
        outRow.add(outDirLabel);
        top.add(outRow, c);

        jpgOutputBox = new JCheckBox("Save as JPEG");
        UIStyle.styleCheckbox(jpgOutputBox);
        top.add(jpgOutputBox, c);

        processBtn = new JButton("Process All");
        UIStyle.styleButton(processBtn);
        processBtn.setEnabled(false);
        processBtn.addActionListener(e -> processAll());
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

    private void selectFiles() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        javax.swing.filechooser.FileNameExtensionFilter filter =
                new javax.swing.filechooser.FileNameExtensionFilter(
                        "Images (PNG, JPG, JPEG, BMP, WEBP)", "png", "jpg", "jpeg", "bmp", "webp");
        fc.setFileFilter(filter);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedFiles = List.of(fc.getSelectedFiles());
            AppLogger.info("Resizer: selected " + selectedFiles.size() + " file(s)");
            log("Selected " + selectedFiles.size() + " file(s)");
            processBtn.setEnabled(outputDir != null && !selectedFiles.isEmpty());
            selectBtn.setText("Select Images (" + selectedFiles.size() + ")");
        }
    }

    private void chooseOutputDir() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputDir = fc.getSelectedFile();
            outDirLabel.setText(outputDir.getName());
            AppLogger.info("Resizer: output dir - " + outputDir);
            log("Output: " + outputDir);
            processBtn.setEnabled(selectedFiles != null && !selectedFiles.isEmpty());
        }
    }

    private void processAll() {
        if (selectedFiles == null || selectedFiles.isEmpty() || outputDir == null) return;

        int targetW = parseInt(widthField.getText(), 800);
        int targetH = parseInt(heightField.getText(), 600);
        int quality = qualitySlider.getValue();
        boolean keepAspect = keepAspectBox.isSelected();
        boolean toJpg = jpgOutputBox.isSelected();

        processBtn.setEnabled(false);
        selectBtn.setEnabled(false);

        AtomicInteger processed = new AtomicInteger(0);
        int total = selectedFiles.size();

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() {
                for (File f : selectedFiles) {
                    try {
                        BufferedImage img = ImageIO.read(f);
                        if (img == null) {
                            AppLogger.error("Resizer: unsupported format - " + f.getName());
                            publish("Skipped (unsupported): " + f.getName());
                            continue;
                        }

                        int w = targetW, h = targetH;
                        if (keepAspect) {
                            double ar = (double) img.getWidth() / img.getHeight();
                            if (targetW > 0 && targetH > 0) {
                                if (img.getWidth() > img.getHeight()) h = (int) (targetW / ar);
                                else w = (int) (targetH * ar);
                            } else if (targetW > 0) h = (int) (targetW / ar);
                            else w = (int) (targetH * ar);
                        }

                        BufferedImage resized = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g2d = resized.createGraphics();
                        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        g2d.drawImage(img, 0, 0, w, h, null);
                        g2d.dispose();

                        String outName = f.getName();
                        int dot = outName.lastIndexOf('.');
                        String base = dot > 0 ? outName.substring(0, dot) : outName;
                        String ext = toJpg ? "jpg" : (dot > 0 ? outName.substring(dot + 1) : "png");
                        File outFile = new File(outputDir, base + "_resized." + ext);

                        ImageIO.write(resized, ext, outFile);
                        int n = processed.incrementAndGet();
                        AppLogger.info("Resizer: [" + n + "/" + total + "] " + outFile.getName());
                        publish("[" + n + "/" + total + "] " + outFile.getName());
                    } catch (IOException ex) {
                        AppLogger.error("Resizer failed: " + f.getName() + " - " + ex.getMessage());
                        publish("Error: " + f.getName() + " - " + ex.getMessage());
                    }
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String s : chunks) log(s);
            }

            @Override
            protected void done() {
                AppLogger.info("Resizer: done! " + processed.get() + "/" + total + " processed.");
                log("Done! " + processed.get() + "/" + total + " processed.");
                processBtn.setEnabled(true);
                selectBtn.setEnabled(true);
            }
        }.execute();
    }

    private void log(String s) {
        logArea.append(s + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }
}

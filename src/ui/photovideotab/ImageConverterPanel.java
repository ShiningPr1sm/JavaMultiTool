package ui.photovideotab;

import service.MediaDownloadService;
import ui.StyledDialog;
import ui.UIStyle;
import util.AppLogger;
import util.FFmpegUtils;
import util.I18n;
import util.JavaFxFileChooser;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageConverterPanel extends JPanel {
    private final JComboBox<String> formatBox;
    private final JSlider qualitySlider;
    private final JLabel qualityLabel;
    private final JButton selectBtn, convertBtn;
    private final JTextArea logArea;
    private final JLabel outDirLabel;
    private List<File> selectedFiles;
    private File outputDir;

    public ImageConverterPanel() {
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

        selectBtn = new JButton(I18n.t("imgs.selectImages"));
        UIStyle.styleButton(selectBtn);
        selectBtn.addActionListener(e -> selectFiles());
        top.add(selectBtn, c);

        formatBox = new JComboBox<>(new String[]{"PNG", "JPG", "WEBP", "AVIF"});
        UIStyle.styleComboBox(formatBox);

        qualitySlider = new JSlider(1, 100, 85);
        UIStyle.styleSlider(qualitySlider);
        qualitySlider.setPreferredSize(new Dimension(200, 22));
        qualityLabel = new JLabel(I18n.t("conv.quality", "85"));
        qualityLabel.setForeground(UIStyle.TEXT_COLOR);
        qualityLabel.setPreferredSize(new Dimension(110, 22));
        qualitySlider.addChangeListener(e -> qualityLabel.setText(
                I18n.t("conv.quality", String.valueOf(qualitySlider.getValue()))));
        formatBox.addActionListener(e -> {
            boolean lossy = !"PNG".equals(formatBox.getSelectedItem());
            qualitySlider.setEnabled(lossy);
            qualityLabel.setEnabled(lossy);
        });

        JPanel formatRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        formatRow.setOpaque(false);
        JLabel fmtLbl = new JLabel(I18n.t("conv.targetFormat"));
        fmtLbl.setForeground(UIStyle.TEXT_COLOR);
        formatRow.add(fmtLbl);
        formatRow.add(formatBox);
        top.add(formatRow, c);

        JPanel qualRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        qualRow.setOpaque(false);
        qualRow.add(qualityLabel);
        qualRow.add(qualitySlider);
        top.add(qualRow, c);

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

        convertBtn = new JButton(I18n.t("conv.convertAll"));
        UIStyle.styleButton(convertBtn);
        convertBtn.setEnabled(false);
        convertBtn.addActionListener(e -> convertAll());
        top.add(convertBtn, c);

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
        List<File> files = JavaFxFileChooser.openFiles(I18n.t("imgs.selectImages"),
                new javafx.stage.FileChooser.ExtensionFilter(
                        I18n.t("imgs.filterExt"),
                        "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.webp", "*.avif"));
        if (files == null || files.isEmpty()) return;
        selectedFiles = files;
        AppLogger.info("Converter: selected " + selectedFiles.size() + " file(s)");
        log(I18n.t("imgs.selected", String.valueOf(selectedFiles.size())));
        convertBtn.setEnabled(outputDir != null);
        selectBtn.setText(I18n.t("imgs.selectImages") + " (" + selectedFiles.size() + ")");
    }

    private void chooseOutputDir() {
        File dir = JavaFxFileChooser.chooseDirectory(I18n.t("imgs.chooseDir"));
        if (dir == null) return;
        outputDir = dir;
        outDirLabel.setText(outputDir.getName());
        AppLogger.info("Converter: output dir - " + outputDir);
        log(I18n.t("imgs.outputLog", outputDir.getAbsolutePath()));
        convertBtn.setEnabled(selectedFiles != null && !selectedFiles.isEmpty());
    }

    private boolean ensureFfmpeg() {
        if (FFmpegUtils.isAvailable()) return true;

        Window parent = SwingUtilities.getWindowAncestor(this);
        boolean download = StyledDialog.confirmYesNo(parent,
                I18n.t("conv.ffmpegMissing", MediaDownloadService.FFMPEG_EXE.getAbsolutePath()),
                I18n.t("conv.ffmpegTitle"));
        if (!download) return false;

        try {
            new MediaDownloadService().checkAndDownloadFFMPEG();
        } catch (IOException ex) {
            AppLogger.error("Converter: failed to download ffmpeg - " + ex.getMessage());
            StyledDialog.show(parent, I18n.t("conv.ffmpegFail", ex.getMessage()));
            return false;
        }
        return FFmpegUtils.isAvailable();
    }

    private void convertAll() {
        if (selectedFiles == null || selectedFiles.isEmpty() || outputDir == null) return;

        if (!ensureFfmpeg()) {
            log(I18n.t("conv.cancelled"));
            return;
        }

        String targetFormat = formatBox.getSelectedItem().toString().toLowerCase();
        int quality = qualitySlider.getValue();

        convertBtn.setEnabled(false);
        selectBtn.setEnabled(false);

        AtomicInteger processed = new AtomicInteger(0);
        int total = selectedFiles.size();

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() {
                for (File f : selectedFiles) {
                    try {
                        String base = f.getName();
                        int dot = base.lastIndexOf('.');
                        if (dot > 0) base = base.substring(0, dot);
                        File outFile = new File(outputDir, base + "." + targetFormat);

                        boolean ok = FFmpegUtils.convert(f, outFile, targetFormat, quality);

                        int n = processed.incrementAndGet();
                        if (ok) {
                            AppLogger.info("Converter: [" + n + "/" + total + "] " + outFile.getName());
                            publish("[" + n + "/" + total + "] " + outFile.getName());
                        } else {
                            AppLogger.error("Converter: FAILED - " + outFile.getName());
                            publish(I18n.t("conv.failed", String.valueOf(n), String.valueOf(total), outFile.getName()));
                        }
                    } catch (Exception ex) {
                        AppLogger.error("Converter failed: " + f.getName() + " - " + ex.getMessage());
                        publish(I18n.t("common.error") + ": " + f.getName() + " - " + ex.getMessage());
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
                AppLogger.info("Converter: done! " + processed.get() + "/" + total + " converted.");
                log(I18n.t("conv.done", String.valueOf(processed.get()), String.valueOf(total)));
                convertBtn.setEnabled(true);
                selectBtn.setEnabled(true);
            }
        }.execute();
    }

    private void log(String s) {
        logArea.append(s + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}

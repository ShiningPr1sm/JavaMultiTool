package ui.photovideotab;

import service.ImageMetadataService;
import ui.UIStyle;
import util.AppLogger;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class MetadataPanel extends JPanel {
    private final JLabel imagePreview;
    private final JTextArea infoArea;
    private final JButton openMapBtn;
    private final JButton stripExifBtn;
    private final JButton elaBtn;
    private final JPanel palettePanel;
    private final JPanel elaPanel;
    private final JLabel elaPreview;
    private final JLabel elaScoreLabel;
    private final JPanel centerPanel;
    private final ImageMetadataService metadataService;
    private ImageMetadataService.ImageMetadataResult currentResult;
    private File currentFile;

    public MetadataPanel() {
        setLayout(new BorderLayout(15, 10));
        setBackground(UIStyle.BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        metadataService = new ImageMetadataService();

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

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actionBtns.setOpaque(false);

        JButton copyBtn = new JButton("Copy");
        UIStyle.styleButton(copyBtn);
        copyBtn.setPreferredSize(new Dimension(80, 24));
        copyBtn.addActionListener(e -> copyMetadata());

        JButton exportBtn = new JButton("Export");
        UIStyle.styleButton(exportBtn);
        exportBtn.setPreferredSize(new Dimension(85, 24));
        exportBtn.addActionListener(e -> exportMetadata());

        actionBtns.add(copyBtn);
        actionBtns.add(exportBtn);

        rightHeader.add(metaTitle, BorderLayout.WEST);
        rightHeader.add(actionBtns, BorderLayout.EAST);

        openMapBtn = new JButton("Open in Maps");
        UIStyle.styleButton(openMapBtn);
        openMapBtn.setVisible(false);

        stripExifBtn = new JButton("Strip EXIF & Save");
        UIStyle.styleButton(stripExifBtn);
        stripExifBtn.setVisible(false);
        stripExifBtn.addActionListener(e -> stripExif());

        elaBtn = new JButton("ELA");
        UIStyle.styleButton(elaBtn);
        elaBtn.setVisible(false);
        elaBtn.addActionListener(e -> runELA());

        palettePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 4));
        palettePanel.setBackground(UIStyle.BG_COLOR);
        palettePanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        palettePanel.setVisible(false);

        JPanel southPanel = new JPanel(new BorderLayout(0, 6));
        southPanel.setOpaque(false);
        southPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        btnRow.setOpaque(false);
        btnRow.add(openMapBtn);
        btnRow.add(stripExifBtn);
        btnRow.add(elaBtn);
        southPanel.add(btnRow, BorderLayout.NORTH);
        southPanel.add(palettePanel, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(420, 0));
        rightPanel.add(rightHeader, BorderLayout.NORTH);
        rightPanel.add(scrollInfo, BorderLayout.CENTER);
        rightPanel.add(southPanel, BorderLayout.SOUTH);

        centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(imagePreview, BorderLayout.CENTER);

        elaPreview = new JLabel("", SwingConstants.CENTER);
        elaPreview.setVerticalAlignment(SwingConstants.CENTER);

        elaScoreLabel = new JLabel("", SwingConstants.CENTER);
        elaScoreLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        elaScoreLabel.setForeground(UIStyle.TEXT_COLOR);

        elaPanel = new JPanel(new BorderLayout(0, 5));
        elaPanel.setOpaque(false);
        elaPanel.add(elaPreview, BorderLayout.CENTER);
        elaPanel.add(elaScoreLabel, BorderLayout.SOUTH);
        elaPanel.setVisible(false);

        centerPanel.add(elaPanel, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);
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
        stripExifBtn.setVisible(false);
        elaBtn.setVisible(false);
        elaPanel.setVisible(false);
        palettePanel.setVisible(false);

        new Thread(() -> {
            try {
                ImageMetadataService.ImageMetadataResult result = metadataService.analyzeImage(file);
                currentResult = result;
                currentFile = file;

                SwingUtilities.invokeLater(() -> {
                    infoArea.setText(result.getMetadataText());
                    infoArea.setCaretPosition(0);
                    infoArea.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 5, 0, 0, result.getAverageColor()),
                            BorderFactory.createEmptyBorder(10, 10, 10, 10)
                    ));

                    if (result.getMapUrl() != null) {
                        openMapBtn.setVisible(true);
                        for (var al : openMapBtn.getActionListeners()) openMapBtn.removeActionListener(al);
                        String mapUrl = result.getMapUrl();
                        openMapBtn.addActionListener(e -> {
                            try { Desktop.getDesktop().browse(new java.net.URI(mapUrl)); }
                            catch (Exception exception) {
                                AppLogger.error("MetadataPanel: failed to open map: " + exception.getMessage()); }
                        });
                    }

                    Image scaled = metadataService.getScaledImage(result.getProcessedImage(),
                            imagePreview.getWidth(), imagePreview.getHeight());
                    imagePreview.setIcon(new ImageIcon(scaled));
                    imagePreview.setText("");

                    stripExifBtn.setVisible(true);
                    elaBtn.setVisible(true);
                    elaPanel.setVisible(false);
                    buildPalette(result.getDominantColors());
                });

            } catch (Exception e) {
                AppLogger.error("Image Processing Error: " + e.getMessage());
                SwingUtilities.invokeLater(() -> infoArea.setText("Failed to process image: " + e.getMessage()));
            }
        }).start();
    }

    private void runELA() {
        if (currentResult == null) return;
        elaBtn.setEnabled(false);
        new Thread(() -> {
            try {
                var elaResult = metadataService.elaAnalysis(currentResult.getProcessedImage());
                SwingUtilities.invokeLater(() -> {
                    int maxH = Math.min(elaResult.elaImage().getHeight(), 200);
                    Image scaled = metadataService.getScaledImage(elaResult.elaImage(),
                            imagePreview.getWidth(), maxH);
                    elaPreview.setIcon(new ImageIcon(scaled));
                    elaPreview.setText(null);

                    double score = elaResult.score();
                    String verdict;
                    if (score < 1.0) verdict = "Low chance of editing";
                    else if (score < 3.0) verdict = "Moderate chance of editing";
                    else verdict = "High chance of editing";

                    elaScoreLabel.setText(String.format("ELA Score: %.2f — %s", score, verdict));
                    elaPanel.setVisible(true);
                    centerPanel.revalidate();
                });
            } catch (Exception ex) {
                AppLogger.error("ELA failed: " + ex.getMessage());
                SwingUtilities.invokeLater(() -> elaScoreLabel.setText("ELA analysis failed."));
            } finally {
                SwingUtilities.invokeLater(() -> elaBtn.setEnabled(true));
            }
        }).start();
    }

    private void copyMetadata() {
        String text = infoArea.getText();
        if (text == null || text.isBlank()) return;
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(text), null);
    }

    private void exportMetadata() {
        String text = infoArea.getText();
        if (text == null || text.isBlank() || currentResult == null) return;

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(currentFile != null ? currentFile.getName() + ".txt" : "metadata.txt"));
        javax.swing.filechooser.FileNameExtensionFilter txtFilter =
                new javax.swing.filechooser.FileNameExtensionFilter("Text file (*.txt)", "txt");
        javax.swing.filechooser.FileNameExtensionFilter jsonFilter =
                new javax.swing.filechooser.FileNameExtensionFilter("JSON file (*.json)", "json");
        fc.addChoosableFileFilter(txtFilter);
        fc.addChoosableFileFilter(jsonFilter);
        fc.setFileFilter(txtFilter);

        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File target = fc.getSelectedFile();
        String ext = target.getName().contains(".") ? target.getName().substring(target.getName().lastIndexOf('.') + 1).toLowerCase() : "";

        try {
            if (ext.equals("json") || fc.getFileFilter() == jsonFilter) {
                if (!ext.equals("json")) target = new File(target.getAbsolutePath() + ".json");
                Files.writeString(target.toPath(), currentResult.getMetadataJson());
            } else {
                if (!ext.equals("txt")) target = new File(target.getAbsolutePath() + ".txt");
                Files.writeString(target.toPath(), text);
            }
        } catch (IOException ex) {
            AppLogger.error("Export failed: " + ex.getMessage());
        }
    }

    private void stripExif() {
        if (currentResult == null || currentFile == null) return;

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("cleaned_" + currentFile.getName()));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File target = fc.getSelectedFile();
        try {
            metadataService.saveWithoutExif(currentResult.getProcessedImage(), currentFile, target);
            currentFile = target;
            processImage(target);
        } catch (IOException ex) {
            AppLogger.error("Strip EXIF failed: " + ex.getMessage());
        }
    }

    private void buildPalette(List<Color> colors) {
        palettePanel.removeAll();
        for (Color c : colors) {
            String hexStr = String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
            JPanel swatch = new JPanel(new BorderLayout());
            swatch.setPreferredSize(new Dimension(40, 40));
            swatch.setBackground(c);
            swatch.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));
            swatch.setToolTipText(hexStr);

            JLabel hex = new JLabel(hexStr, SwingConstants.CENTER);
            hex.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            hex.setForeground(getContrastColor(c));
            swatch.add(hex, BorderLayout.CENTER);

            swatch.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    Toolkit.getDefaultToolkit().getSystemClipboard()
                            .setContents(new StringSelection(hexStr), null);
                }
            });

            palettePanel.add(swatch);
        }
        palettePanel.setVisible(true);
        palettePanel.revalidate();
        palettePanel.repaint();
    }

    private Color getContrastColor(Color bg) {
        double lum = (0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue()) / 255;
        return lum > 0.5 ? Color.BLACK : Color.WHITE;
    }
}

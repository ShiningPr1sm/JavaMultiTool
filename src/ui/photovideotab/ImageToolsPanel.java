package ui.photovideotab;

import service.ImageMetadataService;
import ui.UIStyle;
import util.AppLogger;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.File;
import java.util.List;

public class ImageToolsPanel extends JPanel {
    private final JLabel imagePreview;
    private final JTextArea infoArea;
    private final JButton openMapBtn;
    private final ImageMetadataService metadataService;

    public ImageToolsPanel() {
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
                ImageMetadataService.ImageMetadataResult result = metadataService.analyzeImage(file);

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
                            try { Desktop.getDesktop().browse(new java.net.URI(mapUrl)); } catch (Exception exception) { AppLogger.error("ImageToolsPanel: failed to open map: " + exception.getMessage()); }
                        });
                    }

                    Image scaled = metadataService.getScaledImage(result.getProcessedImage(),
                            imagePreview.getWidth(), imagePreview.getHeight());
                    imagePreview.setIcon(new ImageIcon(scaled));
                    imagePreview.setText("");
                });

            } catch (Exception e) {
                AppLogger.error("Image Processing Error: " + e.getMessage());
                SwingUtilities.invokeLater(() -> infoArea.setText("Failed to process image: " + e.getMessage()));
            }
        }).start();
    }
}

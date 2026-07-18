package ui.utilstab;

import ui.UIStyle;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class ColorPickerPanel extends JPanel {
    private final JSlider rSlider, gSlider, bSlider;
    private final JTextField hexField;
    private final JLabel previewLabel;
    private final JTextField rgbField, hslField;

    public ColorPickerPanel() {
        setLayout(new BorderLayout());
        setBackground(UIStyle.BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        previewLabel = new JLabel();
        previewLabel.setPreferredSize(new Dimension(180, 180));
        previewLabel.setMinimumSize(new Dimension(180, 180));
        previewLabel.setMaximumSize(new Dimension(180, 180));
        previewLabel.setOpaque(true);
        previewLabel.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));

        rSlider = createSlider();
        gSlider = createSlider();
        bSlider = createSlider();
        rSlider.setValue(128);
        gSlider.setValue(128);
        bSlider.setValue(128);

        ChangeListener cl = e -> updateColor();
        rSlider.addChangeListener(cl);
        gSlider.addChangeListener(cl);
        bSlider.addChangeListener(cl);

        hexField = new JTextField(14);
        UIStyle.styleTextField(hexField);
        hexField.addActionListener(e -> applyHex());

        rgbField = new JTextField(14);
        UIStyle.styleTextField(rgbField);
        rgbField.setEditable(false);

        hslField = new JTextField(14);
        UIStyle.styleTextField(hslField);
        hslField.setEditable(false);

        JPanel controls = new JPanel(new GridBagLayout());
        controls.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = GridBagConstraints.RELATIVE;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(2, 5, 2, 5);

        controls.add(createSliderRow("R", rSlider), c);
        controls.add(createSliderRow("G", gSlider), c);
        controls.add(createSliderRow("B", bSlider), c);

        c.insets = new Insets(10, 5, 10, 5);
        JButton pipetteBtn = new JButton("Pick from Screen");
        UIStyle.styleButton(pipetteBtn);
        pipetteBtn.addActionListener(e -> startPipette());
        JPanel pipetteRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pipetteRow.setOpaque(false);
        pipetteRow.add(pipetteBtn);
        controls.add(pipetteRow, c);

        c.insets = new Insets(2, 5, 2, 5);

        JLabel hexLbl = new JLabel("HEX:");
        hexLbl.setForeground(UIStyle.TEXT_COLOR);
        hexLbl.setPreferredSize(new Dimension(28, 26));
        JButton hexCopy = new JButton("Copy");
        UIStyle.styleButton(hexCopy);
        hexCopy.addActionListener(e -> copy(hexField.getText()));
        controls.add(labeledRow(hexLbl, hexField, hexCopy), c);

        c.insets = new Insets(2, 5, 2, 5);

        JLabel rgbLbl = new JLabel("RGB:");
        rgbLbl.setForeground(UIStyle.TEXT_COLOR);
        rgbLbl.setPreferredSize(new Dimension(28, 26));
        JButton rgbCopy = new JButton("Copy");
        UIStyle.styleButton(rgbCopy);
        rgbCopy.addActionListener(e -> copy(rgbField.getText()));
        controls.add(labeledRow(rgbLbl, rgbField, rgbCopy), c);

        JLabel hslLbl = new JLabel("HSL:");
        hslLbl.setForeground(UIStyle.TEXT_COLOR);
        hslLbl.setPreferredSize(new Dimension(28, 26));
        JButton hslCopy = new JButton("Copy");
        UIStyle.styleButton(hslCopy);
        hslCopy.addActionListener(e -> copy(hslField.getText()));
        controls.add(labeledRow(hslLbl, hslField, hslCopy), c);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.X_AXIS));
        top.setOpaque(false);
        top.add(previewLabel);
        top.add(Box.createRigidArea(new Dimension(15, 0)));
        top.add(controls);
        top.add(Box.createHorizontalGlue());

        add(top, BorderLayout.NORTH);
        updateColor();
    }

    private static JPanel labeledRow(JLabel label, JTextField field, JButton copyBtn) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
        row.add(label);
        row.add(field);
        row.add(copyBtn);
        return row;
    }

    private JSlider createSlider() {
        JSlider s = new JSlider(0, 255);
        UIStyle.styleSlider(s);
        s.setPreferredSize(new Dimension(180, 22));
        return s;
    }

    private JPanel createSliderRow(String label, JSlider slider) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
        JLabel lbl = new JLabel(label + ":");
        lbl.setForeground(UIStyle.TEXT_COLOR);
        lbl.setPreferredSize(new Dimension(25, 24));
        JTextField tf = new JTextField(4);
        UIStyle.styleTextField(tf);
        tf.setPreferredSize(new Dimension(50, 24));
        tf.setHorizontalAlignment(JTextField.CENTER);
        slider.putClientProperty("textField", tf);
        slider.addChangeListener(e -> tf.setText(String.valueOf(slider.getValue())));
        tf.addActionListener(e -> {
            try {
                slider.setValue(Integer.parseInt(tf.getText()));
            } catch (NumberFormatException ignored) {
            }
        });
        tf.setText(String.valueOf(slider.getValue()));
        row.add(lbl);
        row.add(slider);
        row.add(tf);
        return row;
    }

    private void updateColor() {
        int r = rSlider.getValue(), g = gSlider.getValue(), b = bSlider.getValue();
        Color c = new Color(r, g, b);
        previewLabel.setBackground(c);
        hexField.setText(String.format("#%02X%02X%02X", r, g, b));
        rgbField.setText(String.format("(%d, %d, %d)", r, g, b));

        double rn = r / 255.0, gn = g / 255.0, bn = b / 255.0;
        double max = Math.max(rn, Math.max(gn, bn));
        double min = Math.min(rn, Math.min(gn, bn));
        double h = 0, s = 0, l = (max + min) / 2;

        if (max != min) {
            double d = max - min;
            s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
            if (max == rn) h = (gn - bn) / d + (gn < bn ? 6 : 0);
            else if (max == gn) h = (bn - rn) / d + 2;
            else h = (rn - gn) / d + 4;
            h /= 6;
        }

        hslField.setText(String.format("(%.0f\u00B0, %.0f%%, %.0f%%)", h * 360, s * 100, l * 100));
    }

    private void applyHex() {
        try {
            String h = hexField.getText().trim().replace("#", "");
            if (h.length() != 6) return;
            Color c = new Color(
                    Integer.parseInt(h.substring(0, 2), 16),
                    Integer.parseInt(h.substring(2, 4), 16),
                    Integer.parseInt(h.substring(4, 6), 16)
            );
            rSlider.setValue(c.getRed());
            gSlider.setValue(c.getGreen());
            bSlider.setValue(c.getBlue());
        } catch (Exception ignored) {
        }
    }

    private void setColor(Color c) {
        rSlider.setValue(c.getRed());
        gSlider.setValue(c.getGreen());
        bSlider.setValue(c.getBlue());
    }

    private void copy(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(text), null);
    }

    private void startPipette() {
        Window parent = SwingUtilities.getWindowAncestor(this);
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        DisplayMode dm = gd.getDisplayMode();
        int w = dm.getWidth(), h = dm.getHeight();

        new Thread(() -> {
            if (parent instanceof Frame f) f.setState(Frame.ICONIFIED);
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            try {
                Robot robot = new Robot();
                BufferedImage screenshot = robot.createScreenCapture(new Rectangle(w, h));
                SwingUtilities.invokeLater(() -> showOverlay(parent, screenshot, w, h));
            } catch (AWTException ex) {
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "Failed to initialize screen capture."));
            }
        }).start();
    }

    private void showOverlay(Window parent, BufferedImage screenshot, int w, int h) {
        int sampleSize = 9;
        int scaleFactor = 9;
        int magSize = sampleSize * scaleFactor;

        JWindow overlay = new JWindow();
        overlay.setAlwaysOnTop(true);

        JLabel bg = new JLabel(new ImageIcon(screenshot));
        bg.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        JWindow magnifier = new JWindow();
        magnifier.setAlwaysOnTop(true);
        JLabel magLabel = new JLabel();
        magLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        magnifier.getContentPane().add(magLabel);
        magnifier.setSize(magSize + 4, magSize + 4);

        JPanel infoBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 2));
        infoBar.setOpaque(true);
        infoBar.setBackground(new Color(40, 40, 40));

        JPanel colorPreview = new JPanel();
        colorPreview.setPreferredSize(new Dimension(30, 20));
        colorPreview.setOpaque(true);
        colorPreview.setBackground(Color.BLACK);
        colorPreview.setBorder(BorderFactory.createLineBorder(Color.WHITE));

        JLabel infoText = new JLabel("Click anywhere to pick a color", SwingConstants.CENTER);
        infoText.setFont(new Font("Segoe UI", Font.BOLD, 14));
        infoText.setForeground(Color.WHITE);
        infoText.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        infoBar.add(colorPreview);
        infoBar.add(infoText);

        bg.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int cx = Math.min(Math.max(e.getX(), sampleSize / 2), w - sampleSize / 2 - 1);
                int cy = Math.min(Math.max(e.getY(), sampleSize / 2), h - sampleSize / 2 - 1);

                Color c = new Color(screenshot.getRGB(cx, cy));
                colorPreview.setBackground(c);
                String hex = String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
                infoText.setText(hex + "  RGB(" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + ")");
                infoText.setForeground(new Color(255 - c.getRed(), 255 - c.getGreen(), 255 - c.getBlue()));

                BufferedImage magImg = new BufferedImage(magSize, magSize, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = magImg.createGraphics();
                int startX = cx - sampleSize / 2;
                int startY = cy - sampleSize / 2;
                for (int dy = 0; dy < sampleSize; dy++) {
                    for (int dx = 0; dx < sampleSize; dx++) {
                        g2.setColor(new Color(screenshot.getRGB(startX + dx, startY + dy)));
                        g2.fillRect(dx * scaleFactor, dy * scaleFactor, scaleFactor, scaleFactor);
                    }
                }

                int vLine = (sampleSize / 2) * scaleFactor;
                int hLine = (sampleSize / 2) * scaleFactor;
                g2.setStroke(new BasicStroke(1.5f));
                g2.setColor(Color.BLACK);
                g2.drawLine(vLine - 1, 0, vLine - 1, magSize - 1);
                g2.drawLine(vLine + 1, 0, vLine + 1, magSize - 1);
                g2.drawLine(0, hLine - 1, magSize - 1, hLine - 1);
                g2.drawLine(0, hLine + 1, magSize - 1, hLine + 1);
                g2.setColor(Color.WHITE);
                g2.drawLine(vLine, 0, vLine, magSize - 1);
                g2.drawLine(0, hLine, magSize - 1, hLine);
                g2.dispose();

                magLabel.setIcon(new ImageIcon(magImg));

                int magX = e.getXOnScreen() + 15;
                int magY = e.getYOnScreen() - magSize - 15;
                magX = Math.max(0, Math.min(magX, w - magSize - 4));
                magY = Math.max(0, Math.min(magY, h - magSize - 4));
                magnifier.setLocation(magX, magY);
                magnifier.setVisible(true);
            }
        });

        bg.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int x = Math.min(Math.max(e.getX(), 0), w - 1);
                int y = Math.min(Math.max(e.getY(), 0), h - 1);
                Color c = new Color(screenshot.getRGB(x, y));
                setColor(c);
                String hex = String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
                copy(hex);
                magnifier.dispose();
                overlay.dispose();
                if (parent instanceof Frame f) { f.setState(Frame.NORMAL); f.toFront(); }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                magnifier.setVisible(false);
            }
        });

        overlay.getContentPane().setLayout(new BorderLayout());
        overlay.getContentPane().add(bg, BorderLayout.CENTER);
        overlay.getContentPane().add(infoBar, BorderLayout.SOUTH);
        overlay.setBounds(0, 0, w, h);
        overlay.setVisible(true);
    }
}

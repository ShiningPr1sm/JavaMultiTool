package ui.dailytracker;

import db.ProductEntry;
import ui.StyledDialog;
import ui.UIStyle;
import util.I18n;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Compact modal dialog for weighed goods: the weight in grams, calories
 * computed from the catalog value (per 100 g or per 1 kg basis) and an
 * auto-calculated price (catalog price per kg) that stays editable, so a
 * receipt rounding can be typed over the suggested amount.
 */
public class WeightedEntryDialog extends JDialog {

    /** Dialog outcome: entered weight, computed calories and the purchase price (null = not added to purchases). */
    public record Result(double weight, int calories, Double price) {}

    public static Result show(Window parent, ProductEntry product) {
        Result[] result = new Result[1];
        WeightedEntryDialog dialog = new WeightedEntryDialog(parent, product, result);
        dialog.setVisible(true);
        return result[0];
    }

    private WeightedEntryDialog(Window parent, ProductEntry product, Result[] result) {
        super(parent, I18n.t("nutrition.weightedTitle"), Dialog.ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setBackground(UIStyle.BG_COLOR);

        JLabel productLabel = new JLabel(I18n.t("nutrition.weighted.product", product.name()));
        productLabel.setForeground(UIStyle.TEXT_COLOR);
        productLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JTextField weightField = field(7);
        JTextField priceField = field(7);

        JRadioButton per100g = new JRadioButton(I18n.t("nutrition.weighted.per100g"));
        JRadioButton perKg = new JRadioButton(I18n.t("nutrition.weighted.perKg"));
        UIStyle.styleRadioButton(per100g);
        UIStyle.styleRadioButton(perKg);
        per100g.setFocusable(false);
        perKg.setFocusable(false);
        ButtonGroup basis = new ButtonGroup();
        basis.add(per100g);
        basis.add(perKg);
        per100g.setSelected(true);

        JLabel kcalLabel = new JLabel();
        kcalLabel.setForeground(UIStyle.ACCENT_COLOR);
        kcalLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JCheckBox addPurchase = new JCheckBox(I18n.t("nutrition.weighted.addPurchase"));
        UIStyle.styleCheckbox(addPurchase);
        addPurchase.setFocusable(false);

        // price follows the weight until the user types a receipt-corrected value by hand
        boolean[] priceManuallyEdited = {false};
        priceField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                priceManuallyEdited[0] = true;
            }
        });

        Runnable recalc = () -> {
            double weight = parsed(weightField.getText());
            int kcal = (int) Math.round(product.calories() * weight / (per100g.isSelected() ? 100.0 : 1000.0));
            kcalLabel.setText(I18n.t("nutrition.weighted.kcal", String.valueOf(kcal)));
            if (!priceManuallyEdited[0]) {
                priceField.setText(product.price() != null
                        ? String.format("%.2f", product.price() * weight / 1000.0)
                        : "");
            }
        };
        weightField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { recalc.run(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { recalc.run(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { recalc.run(); }
        });
        per100g.addActionListener(e -> recalc.run());
        perKg.addActionListener(e -> recalc.run());

        JButton okBtn = new JButton(I18n.t("common.ok"));
        UIStyle.styleButton(okBtn);
        okBtn.setPreferredSize(new Dimension(100, 32));
        JButton cancelBtn = new JButton(I18n.t("common.cancel"));
        UIStyle.styleButton(cancelBtn);
        cancelBtn.setPreferredSize(new Dimension(100, 32));

        okBtn.addActionListener(e -> {
            String weightText = weightField.getText().trim().replace(',', '.');
            double weight;
            try {
                weight = Double.parseDouble(weightText);
            } catch (NumberFormatException ex) {
                StyledDialog.show(this, I18n.t("nutrition.weighted.errWeight"));
                return;
            }
            if (weight <= 0) {
                StyledDialog.show(this, I18n.t("nutrition.weighted.errWeightPositive"));
                return;
            }
            int kcal = (int) Math.round(product.calories() * weight / (per100g.isSelected() ? 100.0 : 1000.0));
            Double price = null;
            if (addPurchase.isSelected()) {
                String priceText = priceField.getText().trim().replace(',', '.');
                try {
                    price = Double.parseDouble(priceText);
                } catch (NumberFormatException ex) {
                    StyledDialog.show(this, I18n.t("nutrition.weighted.errPrice"));
                    return;
                }
                if (price < 0) {
                    StyledDialog.show(this, I18n.t("nutrition.weighted.errPrice"));
                    return;
                }
            }
            result[0] = new Result(weight, kcal, price);
            dispose();
        });
        cancelBtn.addActionListener(e -> dispose());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(okBtn);
        btnPanel.add(cancelBtn);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UIStyle.SIDE_BOX);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(18, 22, 18, 22)));
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 5, 5, 5);

        c.gridy = 0; c.gridx = 0; c.gridwidth = 2;
        panel.add(productLabel, c);
        c.gridwidth = 1;

        c.gridy = 1; c.gridx = 0;
        panel.add(styledLabel(I18n.t("nutrition.weight")), c);
        c.gridx = 1;
        panel.add(weightField, c);

        c.gridy = 2; c.gridx = 0; c.gridwidth = 2;
        JPanel basisRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        basisRow.setOpaque(false);
        basisRow.add(per100g);
        basisRow.add(perKg);
        basisRow.add(kcalLabel);
        panel.add(basisRow, c);
        c.gridwidth = 1;

        c.gridy = 3; c.gridx = 0;
        panel.add(styledLabel(I18n.t("catalog.price")), c);
        c.gridx = 1;
        panel.add(priceField, c);

        c.gridy = 4; c.gridx = 0; c.gridwidth = 2;
        panel.add(addPurchase, c);
        c.gridwidth = 1;

        c.gridy = 5; c.gridx = 0; c.gridwidth = 2;
        panel.add(btnPanel, c);
        c.gridwidth = 1;

        setContentPane(panel);
        getRootPane().setDefaultButton(okBtn);
        recalc.run();
        pack();
        Dimension size = getSize();
        size.width = Math.max(size.width, 380);
        setSize(size);
        if (parent != null) setLocationRelativeTo(parent);
        else {
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation((screen.width - getWidth()) / 2, (screen.height - getHeight()) / 2);
        }
        weightField.requestFocusInWindow();
    }

    private double parsed(String text) {
        try {
            return Double.parseDouble(text.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private JTextField field(int columns) {
        JTextField f = new JTextField(columns);
        UIStyle.styleTextField(f);
        f.setMinimumSize(f.getPreferredSize());
        return f;
    }

    private JLabel styledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(UIStyle.TEXT_COLOR);
        return l;
    }
}

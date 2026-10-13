package ui.dailytracker;

import db.ProductEntry;
import db.ProductRepository;
import ui.StyledDialog;
import ui.UIStyle;
import util.AppLogger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ProductCatalogPanel extends JPanel {
    private static final String[] CURRENCIES = {"", "RUB", "USD", "EUR", "GBP", "KZT", "UAH", "CNY", "JPY"};

    private final ProductRepository repo;
    private final Runnable onCatalogChanged;
    private final JTextField nameField, priceField, caloriesField, weightField;
    private final JComboBox<String> currencyBox;
    private final JCheckBox manualModeBox;
    private final JLabel caloriesLabel, weightLabel;
    private final JButton addBtn, cancelEditBtn;
    private final DefaultTableModel model;
    private final JTable table;
    private Integer editingId;

    public ProductCatalogPanel(ProductRepository repo, Runnable onCatalogChanged) {
        this.repo = repo;
        this.onCatalogChanged = onCatalogChanged;

        setLayout(new BorderLayout(0, 10));
        setBackground(UIStyle.BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 5, 4, 5);

        c.gridx = 0; form.add(label("Name:"), c);
        c.gridx = 1; nameField = field(15); form.add(nameField, c);
        c.gridx = 2; form.add(label("Price:"), c);
        c.gridx = 3; priceField = field(8); form.add(priceField, c);
        c.gridx = 4; currencyBox = new JComboBox<>(CURRENCIES);
        UIStyle.styleComboBox(currencyBox);
        currencyBox.setSelectedItem("EUR");
        form.add(currencyBox, c);

        caloriesLabel = label("Calories:");
        FontMetrics calMetrics = caloriesLabel.getFontMetrics(caloriesLabel.getFont());
        int calWidth = Math.max(calMetrics.stringWidth("Calories:"), calMetrics.stringWidth("Calories per 100g:"));
        Dimension calLabelSize = new Dimension(calWidth, caloriesLabel.getPreferredSize().height);
        caloriesLabel.setPreferredSize(calLabelSize);
        caloriesLabel.setMinimumSize(calLabelSize);
        caloriesLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        c.gridx = 5; form.add(caloriesLabel, c);
        c.gridx = 6; caloriesField = field(6); form.add(caloriesField, c);

        weightLabel = label("Weight (g):");
        Dimension weightLabelSize = new Dimension(weightLabel.getPreferredSize().width, weightLabel.getPreferredSize().height);
        weightLabel.setPreferredSize(weightLabelSize);
        weightLabel.setMinimumSize(weightLabelSize);
        c.gridx = 7; form.add(weightLabel, c);
        c.gridx = 8; weightField = field(8); form.add(weightField, c);

        addBtn = new JButton("Add");
        UIStyle.styleButton(addBtn);
        addBtn.addActionListener(e -> saveProduct());
        c.gridx = 9; form.add(addBtn, c);

        cancelEditBtn = new JButton("Cancel");
        UIStyle.styleButton(cancelEditBtn);
        cancelEditBtn.setVisible(false);
        cancelEditBtn.addActionListener(e -> clearForm());
        c.gridx = 10; form.add(cancelEditBtn, c);

        JPanel filler = new JPanel();
        filler.setOpaque(false);
        c.gridx = 11;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(filler, c);

        manualModeBox = new JCheckBox("Manual mode");
        UIStyle.styleCheckbox(manualModeBox);
        manualModeBox.setSelected(true);
        manualModeBox.setToolTipText("Checked: enter total calories. Unchecked: enter total weight and calories per 100 g.");
        manualModeBox.addItemListener(e -> updateMode());
        c.gridy = 1;
        c.gridx = 0;
        c.gridwidth = 2;
        form.add(manualModeBox, c);
        c.gridwidth = 1;

        updateMode();

        add(form, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[]{"ID", "Name", "Price", "Currency", "Calories"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setBackground(UIStyle.SECONDARY_BG);
        table.setForeground(Color.WHITE);
        table.setGridColor(UIStyle.BORDER_COLOR);
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(220);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);
        NutritionPanel.styleTable(table);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRowSelected();
        });

        JScrollPane scroll = new JScrollPane(table);
        UIStyle.styleScrollBar(scroll);
        scroll.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));
        scroll.getViewport().setBackground(UIStyle.SECONDARY_BG);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        JButton deleteBtn = new JButton("Delete Selected");
        UIStyle.styleButton(deleteBtn);
        deleteBtn.addActionListener(e -> deleteSelected());
        JLabel hint = new JLabel("Price and calories are optional.");
        hint.setForeground(UIStyle.TEXT_COLOR);
        bottom.add(deleteBtn, BorderLayout.WEST);
        bottom.add(hint, BorderLayout.EAST);

        add(scroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        refreshTable();
    }

    private void saveProduct() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            StyledDialog.show(SwingUtilities.windowForComponent(this), "Enter a product name.");
            return;
        }
        Double price = null;
        String priceText = priceField.getText().trim();
        if (!priceText.isEmpty()) {
            try {
                price = Double.parseDouble(priceText);
                if (price < 0) {
                    StyledDialog.show(SwingUtilities.windowForComponent(this), "Price cannot be negative.");
                    return;
                }
            } catch (NumberFormatException ex) {
                StyledDialog.show(SwingUtilities.windowForComponent(this), "Price must be a number.");
                return;
            }
        }
        Integer calories = null;
        if (manualModeBox.isSelected()) {
            String calText = caloriesField.getText().trim();
            if (!calText.isEmpty()) {
                try {
                    calories = Integer.parseInt(calText);
                    if (calories < 0) {
                        StyledDialog.show(SwingUtilities.windowForComponent(this), "Calories cannot be negative.");
                        return;
                    }
                } catch (NumberFormatException ex) {
                    StyledDialog.show(SwingUtilities.windowForComponent(this), "Calories must be a whole number.");
                    return;
                }
            }
        } else {
            double weight;
            try {
                weight = Double.parseDouble(weightField.getText().trim());
            } catch (NumberFormatException ex) {
                StyledDialog.show(SwingUtilities.windowForComponent(this), "Enter the total weight in grams.");
                return;
            }
            if (weight <= 0) {
                StyledDialog.show(SwingUtilities.windowForComponent(this), "Weight must be positive.");
                return;
            }
            double per100;
            try {
                per100 = Double.parseDouble(caloriesField.getText().trim());
            } catch (NumberFormatException ex) {
                StyledDialog.show(SwingUtilities.windowForComponent(this), "Enter calories per 100 g.");
                return;
            }
            if (per100 < 0) {
                StyledDialog.show(SwingUtilities.windowForComponent(this), "Calories per 100 g cannot be negative.");
                return;
            }
            calories = (int) Math.round(weight / 100.0 * per100);
        }
        String currency = (String) currencyBox.getSelectedItem();
        if (currency == null || currency.isEmpty()) currency = null;

        for (ProductEntry p : repo.listAll()) {
            if (editingId == null || p.id() != editingId) {
                if (p.name().equalsIgnoreCase(name)) {
                    StyledDialog.show(SwingUtilities.windowForComponent(this),
                            "Product with this name already exists.");
                    return;
                }
            }
        }

        if (editingId == null) {
            if (repo.addProduct(name, price, calories, currency) >= 0) notifyChanged();
        } else {
            repo.updateProduct(editingId, name, price, calories, currency);
            notifyChanged();
        }
        clearForm();
        refreshTable();
    }

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        if (nameField.hasFocus()) return;
        editingId = (int) model.getValueAt(row, 0);
        fill(nameField, (String) model.getValueAt(row, 1));
        fill(priceField, (String) model.getValueAt(row, 2));
        if (!currencyBox.isPopupVisible()) currencyBox.setSelectedItem(model.getValueAt(row, 3));
        fill(caloriesField, (String) model.getValueAt(row, 4));
        manualModeBox.setSelected(true);
        weightField.setText("");
        addBtn.setText("Save");
        cancelEditBtn.setVisible(true);
        updateMode();
    }

    private void clearForm() {
        editingId = null;
        fill(nameField, "");
        fill(priceField, "");
        fill(caloriesField, "");
        fill(weightField, "");
        if (!currencyBox.isPopupVisible()) currencyBox.setSelectedItem("EUR");
        manualModeBox.setSelected(true);
        addBtn.setText("Add");
        cancelEditBtn.setVisible(false);
        table.clearSelection();
        updateMode();
    }

    /** Fills a field only when the user is not actively editing it, so typing is never hijacked. */
    private void fill(JTextField field, String text) {
        if (field.hasFocus()) return;
        if (field.getText().equals(text)) return;
        field.setText(text);
        field.setCaretPosition(field.getText().length());
    }

    private void updateMode() {
        boolean manual = manualModeBox.isSelected();
        caloriesLabel.setText(manual ? "Calories:" : "Calories per 100g:");
        weightLabel.setVisible(!manual);
        weightField.setVisible(!manual);
        if (manual) weightField.setText("");
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            StyledDialog.show(SwingUtilities.windowForComponent(this), "Select a product to delete.");
            return;
        }
        int id = (int) model.getValueAt(row, 0);
        repo.deleteProduct(id);
        AppLogger.info("Catalog: deleted product id=" + id);
        refreshTable();
        notifyChanged();
    }

    private void refreshTable() {
        model.setRowCount(0);
        for (ProductEntry p : repo.listAll()) {
            model.addRow(new Object[]{
                    p.id(),
                    p.name(),
                    p.price() != null ? String.format("%.2f", p.price()) : "",
                    p.currency() != null ? p.currency() : "",
                    p.calories() != null ? String.valueOf(p.calories()) : ""
            });
        }
    }

    private void notifyChanged() {
        if (onCatalogChanged != null) onCatalogChanged.run();
    }

    private JTextField field(int columns) {
        JTextField f = new JTextField(columns);
        UIStyle.styleTextField(f);
        f.setMinimumSize(f.getPreferredSize());
        return f;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(UIStyle.TEXT_COLOR);
        return l;
    }
}
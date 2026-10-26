package ui.dailytracker;

import db.NutritionRepository;
import db.ProductEntry;
import db.ProductRepository;
import db.PurchaseEntry;
import db.PurchasesRepository;
import ui.StyledDialog;
import ui.UIStyle;
import util.AppLogger;
import util.DateUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

public class PurchasesPanel extends JPanel {
    private static final String[] CURRENCIES = {"CNY", "EUR", "GBP", "JPY", "KZT", "RUB", "UAH", "USD"};

    private final PurchasesRepository repo;
    private final ProductRepository productsRepo;
    private final NutritionRepository nutritionRepo;
    private final JTextField dateField, itemField, priceField;
    private final JComboBox<String> currencyBox;
    private final JComboBox<ProductEntry> catalogBox;
    private final JCheckBox includeCalBox;
    private final DefaultTableModel model;
    private final JTable table;
    private final List<Integer> rowIds = new ArrayList<>();
    private final JLabel totalLabel;
    private final Runnable onDataChanged;
    private boolean reloadingCatalog;

    public PurchasesPanel(PurchasesRepository repo, ProductRepository productsRepo,
                          NutritionRepository nutritionRepo, Runnable onDataChanged) {
        this.repo = repo;
        this.productsRepo = productsRepo;
        this.nutritionRepo = nutritionRepo;
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(0, 10));
        setBackground(UIStyle.BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 5, 4, 5);

        form.add(label("Date (dd.MM.yyyy):"), c);
        c.gridx = 1;
        dateField = new JTextField(10);
        UIStyle.styleTextField(dateField);
        dateField.setText(DateUtils.todayDisplay());
        form.add(dateField, c);

        c.gridx = 2;
        form.add(label("Item:"), c);
        c.gridx = 3;
        c.gridwidth = 2;
        itemField = new JTextField(15);
        UIStyle.styleTextField(itemField);
        form.add(itemField, c);

        c.gridx = 5;
        c.gridwidth = 1;
        form.add(label("Price:"), c);
        c.gridx = 6;
        priceField = new JTextField(9);
        UIStyle.styleTextField(priceField);
        form.add(priceField, c);

        c.gridx = 7;
        currencyBox = new JComboBox<>(CURRENCIES);
        UIStyle.styleComboBox(currencyBox);
        currencyBox.setSelectedItem("EUR");
        form.add(currencyBox, c);

        c.gridx = 8;
        JButton addBtn = new JButton("Add");
        UIStyle.styleButton(addBtn);
        addBtn.addActionListener(e -> addPurchase());
        form.add(addBtn, c);

        c.gridy = 1;
        c.gridx = 0;
        form.add(label("From catalog:"), c);
        catalogBox = new JComboBox<>();
        UIStyle.styleComboBox(catalogBox);
        catalogBox.setPreferredSize(new Dimension(240, 30));
        catalogBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Object display = (value instanceof ProductEntry p) ? p.name() : "Select from catalog...";
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, display, index, isSelected, cellHasFocus);
                lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                lbl.setOpaque(true);
                if (isSelected) {
                    lbl.setBackground(UIStyle.BUTTON_HOVER);
                    lbl.setForeground(UIStyle.ACCENT_COLOR);
                } else {
                    lbl.setBackground(UIStyle.BUTTON_BG);
                    lbl.setForeground(UIStyle.TEXT_COLOR);
                }
                lbl.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                return lbl;
            }
        });
        catalogBox.addActionListener(e -> onCatalogSelected());
        c.gridx = 1;
        c.gridwidth = 8;
        form.add(catalogBox, c);

        includeCalBox = new JCheckBox("Include calories in today's intake");
        UIStyle.styleCheckbox(includeCalBox);
        includeCalBox.setEnabled(false);
        c.gridy = 2;
        c.gridx = 1;
        form.add(includeCalBox, c);
        c.gridwidth = 1;

        model = new DefaultTableModel(new String[]{"Item", "Price", "Currency"}, 0) {
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
        table.setShowVerticalLines(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(260);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        NutritionPanel.styleTable(table);

        JScrollPane scroll = new JScrollPane(table);
        UIStyle.styleScrollBar(scroll);
        scroll.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));
        scroll.getViewport().setBackground(UIStyle.SECONDARY_BG);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        JButton deleteBtn = new JButton("Delete Selected");
        UIStyle.styleButton(deleteBtn);
        deleteBtn.addActionListener(e -> deleteSelected());
        totalLabel = new JLabel("Total: -");
        totalLabel.setForeground(UIStyle.ACCENT_COLOR);
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        bottom.add(deleteBtn, BorderLayout.WEST);
        bottom.add(totalLabel, BorderLayout.EAST);

        add(form, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        reloadCatalog();
        refreshTable();
    }

    private void addPurchase() {
        String dateISO = validDateISO();
        if (dateISO == null) {
            StyledDialog.show(SwingUtilities.windowForComponent(this), "Invalid date. Use format dd.MM.yyyy.");
            return;
        }
        String item = itemField.getText().trim();
        if (item.isEmpty()) {
            StyledDialog.show(SwingUtilities.windowForComponent(this), "Enter an item name.");
            return;
        }
        double price;
        try {
            price = Double.parseDouble(priceField.getText().trim());
        } catch (NumberFormatException ex) {
            StyledDialog.show(SwingUtilities.windowForComponent(this), "Price must be a number.");
            return;
        }
        if (price < 0) {
            StyledDialog.show(SwingUtilities.windowForComponent(this), "Price cannot be negative.");
            return;
        }
        String currency = (String) currencyBox.getSelectedItem();
        int purchaseId = repo.addPurchase(dateISO, item, price, currency);
        if (includeCalBox.isSelected() && purchaseId > 0) {
            ProductEntry p = (ProductEntry) catalogBox.getSelectedItem();
            if (p != null && p.calories() != null && p.calories() > 0) {
                nutritionRepo.addFoodLinked(dateISO, item, p.calories(), purchaseId);
                AppLogger.info("Purchases: included " + p.calories() + " kcal from '" + item + "' for " + dateISO);
            }
        }
        AppLogger.info("Purchases: added '" + item + "' " + price + " " + currency + " for " + dateISO);
        itemField.setText("");
        priceField.setText("");
        refreshTable();
        notifyChanged();
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            StyledDialog.show(SwingUtilities.windowForComponent(this), "Select a row to delete.");
            return;
        }
        int id = rowIds.get(row);
        repo.deletePurchase(id);
        nutritionRepo.deleteByPurchaseId(id);
        AppLogger.info("Purchases: deleted entry id=" + id);
        refreshTable();
        notifyChanged();
    }

    private void refreshTable() {
        model.setRowCount(0);
        rowIds.clear();
        String date = validDateISO();
        if (date == null) {
            totalLabel.setText("Total: -");
            return;
        }
        try {
            for (PurchaseEntry entry : repo.listByDate(date)) {
                rowIds.add(entry.id());
                model.addRow(new Object[]{entry.itemName(), String.format("%.2f", entry.price()), entry.currency()});
            }
        } catch (Exception e) {
            AppLogger.error("PurchasesPanel: failed to load entries - " + e.getMessage());
        }
        StringBuilder sb = new StringBuilder("Total:");
        try {
            List<String> currencies = repo.getCurrenciesForDate(date);
            if (currencies.isEmpty()) {
                sb.append(" -");
            } else {
                for (String cur : currencies) {
                    sb.append(" ").append(String.format("%.2f", repo.totalForCurrency(date, cur))).append(" ").append(cur).append("  ");
                }
            }
        } catch (Exception e) {
            AppLogger.error("PurchasesPanel: failed to compute totals - " + e.getMessage());
        }
        totalLabel.setText(sb.toString().trim());
    }

    private void notifyChanged() {
        if (onDataChanged != null) onDataChanged.run();
    }

    public void reloadCatalog() {
        ProductEntry selected = (ProductEntry) catalogBox.getSelectedItem();
        reloadingCatalog = true;
        catalogBox.removeAllItems();
        List<ProductEntry> all = new ArrayList<>(productsRepo.listAll());
        Collator col = Collator.getInstance();
        col.setStrength(Collator.SECONDARY);
        all.sort((a, b) -> col.compare(a.name(), b.name()));
        for (ProductEntry p : all) catalogBox.addItem(p);
        ProductEntry match = null;
        if (selected != null) {
            for (ProductEntry p : all) {
                if (p.id() == selected.id()) {
                    match = p;
                    break;
                }
            }
        }
        if (match != null) catalogBox.setSelectedItem(match);
        else includeCalBox.setEnabled(false);
        reloadingCatalog = false;
    }

    private void onCatalogSelected() {
        if (reloadingCatalog) return;
        ProductEntry p = (ProductEntry) catalogBox.getSelectedItem();
        if (p == null) {
            includeCalBox.setEnabled(false);
            return;
        }
        itemField.setText(p.name());
        if (p.price() != null) priceField.setText(String.format("%.2f", p.price()));
        if (p.currency() != null && !p.currency().isBlank()) currencyBox.setSelectedItem(p.currency());
        boolean hasCal = p.calories() != null && p.calories() > 0;
        includeCalBox.setEnabled(hasCal);
        if (!hasCal) includeCalBox.setSelected(false);
    }

    private String validDateISO() {
        String text = dateField.getText().trim();
        if (text.isEmpty()) return null;
        try {
            return DateUtils.toISO(DateUtils.parseDisplay(text));
        } catch (Exception e) {
            return null;
        }
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(UIStyle.TEXT_COLOR);
        return l;
    }
}
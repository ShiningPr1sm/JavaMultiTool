package ui.dailytracker;

import db.PurchaseEntry;
import db.PurchasesRepository;
import ui.UIStyle;
import util.AppLogger;
import util.DateUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class PurchasesPanel extends JPanel {
    private static final String[] CURRENCIES = {"RUB", "USD", "EUR", "GBP", "KZT", "UAH", "CNY", "JPY"};

    private final PurchasesRepository repo;
    private final JTextField dateField, itemField, priceField;
    private final JComboBox<String> currencyBox;
    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel totalLabel;
    private final Runnable onDataChanged;

    public PurchasesPanel(PurchasesRepository repo, Runnable onDataChanged) {
        this.repo = repo;
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

        model = new DefaultTableModel(new String[]{"ID", "Item", "Price", "Currency"}, 0) {
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
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(260);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(90);
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

        refreshTable();
    }

    private void addPurchase() {
        String dateISO = validDateISO();
        if (dateISO == null) {
            JOptionPane.showMessageDialog(this, "Invalid date. Use format dd.MM.yyyy.");
            return;
        }
        String item = itemField.getText().trim();
        if (item.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter an item name.");
            return;
        }
        double price;
        try {
            price = Double.parseDouble(priceField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Price must be a number.");
            return;
        }
        if (price < 0) {
            JOptionPane.showMessageDialog(this, "Price cannot be negative.");
            return;
        }
        String currency = (String) currencyBox.getSelectedItem();
        repo.addPurchase(dateISO, item, price, currency);
        AppLogger.info("Purchases: added '" + item + "' " + price + " " + currency + " for " + dateISO);
        itemField.setText("");
        priceField.setText("");
        refreshTable();
        notifyChanged();
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a row to delete.");
            return;
        }
        int id = (int) model.getValueAt(row, 0);
        repo.deletePurchase(id);
        AppLogger.info("Purchases: deleted entry id=" + id);
        refreshTable();
        notifyChanged();
    }

    private void refreshTable() {
        model.setRowCount(0);
        String date = validDateISO();
        if (date == null) {
            totalLabel.setText("Total: -");
            return;
        }
        try {
            for (PurchaseEntry entry : repo.listByDate(date)) {
                model.addRow(new Object[]{entry.id(), entry.itemName(), String.format("%.2f", entry.price()), entry.currency()});
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
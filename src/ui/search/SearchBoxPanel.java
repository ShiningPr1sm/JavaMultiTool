package ui.search;

import ui.UIStyle;
import util.I18n;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Search box for the header pane. Typing filters {@link SearchIndex} entries:
 * direct matches (title or visible tag) are listed at the top, entries matched
 * only through hidden keywords appear under a "you may be looking for" header.
 * The popup supports full keyboard navigation (footer shows the hint) and opens
 * the selected target through the router consumer.
 */
public class SearchBoxPanel extends JPanel {

    private static final int MAX_DIRECT = 5;
    private static final int MAX_MAYBE = 3;
    private static final int SEARCH_FIELD_WIDTH = 400;
    private static final int SEARCH_FIELD_HEIGHT = 40;

    private final JTextField field = new JTextField() {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(UIStyle.BORDER_COLOR.brighter());
                g2.drawString(I18n.t("search.placeholder"), 8, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(SEARCH_FIELD_WIDTH - 100, SEARCH_FIELD_HEIGHT);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }
    };
    private final Consumer<String> targetRouter;
    private final List<SearchEntry> entries;
    private boolean fieldFocused;
    /** Border-color blend: 0 = inactive (dark), 1 = focused (accent). Animated. */
    private float borderFade;
    private final Timer borderFadeTimer = new Timer(16, e -> stepBorderFade());
    private final JPanel fieldWrap;

    private JWindow popup;
    private final JPanel popupContent = new JPanel();
    private final List<RowPanel> rows = new ArrayList<>();
    private int selectedRow = -1;

    public SearchBoxPanel(boolean tester, Consumer<String> targetRouter) {
        this.targetRouter = targetRouter;
        this.entries = SearchIndex.entries(tester);

        setLayout(new BorderLayout());
        setOpaque(false);

        // magnifier glyph inside an outlined field: the outline smoothly fades
        // between dark (inactive) and accent (focused) in borderFadeTimer
        fieldWrap = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIStyle.SECONDARY_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(mix(UIStyle.BORDER_COLOR.darker(), UIStyle.ACCENT_COLOR, borderFade));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        fieldWrap.setOpaque(false);

        JLabel magnifier = new JLabel("\uD83D\uDD0D"); // magnifying glass emoji
        magnifier.setForeground(UIStyle.TEXT_COLOR);
        magnifier.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        magnifier.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 8));

        field.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
        field.setOpaque(false);
        field.setForeground(UIStyle.TEXT_COLOR);
        field.setCaretColor(UIStyle.TEXT_COLOR);
        field.setFont(field.getFont().deriveFont(15f));
        // the field must not grab focus at startup (Swing auto-focuses the first
        // focusable component); it is activated explicitly by click or Ctrl+K
        field.setFocusable(false);

        JLabel hintLabel = new JLabel("Ctrl+K");
        hintLabel.setForeground(Color.GRAY);
        hintLabel.setFont(hintLabel.getFont().deriveFont(12f));
        hintLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 10));

        fieldWrap.add(magnifier, BorderLayout.WEST);
        fieldWrap.add(field, BorderLayout.CENTER);
        fieldWrap.add(hintLabel, BorderLayout.EAST);
        // fixed width, about half of the header bar - the box must not stretch
        fieldWrap.setPreferredSize(new Dimension(SEARCH_FIELD_WIDTH, SEARCH_FIELD_HEIGHT));

        JPanel outer = new JPanel(new GridBagLayout());
        outer.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(14, 24, 14, 24);
        // anchor west without grow: the box keeps its ~half-header width
        gc.anchor = GridBagConstraints.WEST;
        outer.add(fieldWrap, gc);

        add(outer, BorderLayout.CENTER);
        setPreferredSize(new Dimension(0, 70));

        // clicks anywhere in the box (magnifier, empty space, the field itself)
        // activate the caret: the field was made non-focusable at startup
        MouseAdapter activate = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                field.setFocusable(true);
                field.requestFocusInWindow();
            }
        };
        fieldWrap.addMouseListener(activate);
        field.addMouseListener(activate);
        magnifier.addMouseListener(activate);

        field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { SwingUtilities.invokeLater(SearchBoxPanel.this::runSearch); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { SwingUtilities.invokeLater(SearchBoxPanel.this::runSearch); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { SwingUtilities.invokeLater(SearchBoxPanel.this::runSearch); }
        });
        installKeyBindings();
        installFocusHide();
        installOutsideClickBlur();

        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                fieldFocused = true;
                borderFadeTimer.start();
            }
            @Override public void focusLost(FocusEvent e) {
                fieldFocused = false;
                borderFadeTimer.start();
            }
        });
    }

    /** Smoothly drives the border color toward the current focus state. */
    private void stepBorderFade() {
        float target = fieldFocused ? 1f : 0f;
        float step = 0.07f; // ~300 ms for the full fade at 60 fps
        if (Math.abs(target - borderFade) <= step) {
            borderFade = target;
            borderFadeTimer.stop();
        } else {
            borderFade += Math.signum(target - borderFade) * step;
        }
        fieldWrap.repaint();
    }

    private static Color mix(Color from, Color to, float t) {
        return new Color(
                Math.round(from.getRed() + (to.getRed() - from.getRed()) * t),
                Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * t),
                Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * t));
    }

    /**
     * Clicking anywhere outside the search box (and its popup) must drop the
     * caret: clickable panels around it are usually not focusable, so focus
     * would otherwise stay in the field with the caret blinking forever.
     * The popup hides on the same click unless the click is a row selection.
     */
    private void installOutsideClickBlur() {
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (!(event instanceof MouseEvent me) || me.getID() != MouseEvent.MOUSE_PRESSED) return;
            Component clicked = SwingUtilities.getDeepestComponentAt(me.getComponent(), me.getX(), me.getY());
            if (clicked == null) return;
            boolean insideSearch = SwingUtilities.isDescendingFrom(clicked, this);
            boolean insidePopup = popup != null && popup.isVisible() && SwingUtilities.isDescendingFrom(clicked, popup);
            if (insideSearch || insidePopup) return;
            SwingUtilities.invokeLater(() -> {
                if (field.isFocusOwner()) {
                    // dropping focusability removes the caret AND deactivates
                    // the field; re-activation happens on the next click/Ctrl+K
                    field.setFocusable(false);
                }
                if (popup != null && popup.isVisible()) hidePopup();
            });
        }, AWTEvent.MOUSE_EVENT_MASK);
    }

    /** Ctrl+K handler: activates the field so typing starts the search. */
    public void focusSearch() {
        field.setFocusable(true);
        field.requestFocusInWindow();
        field.selectAll();
        if (!field.getText().isBlank()) {
            SwingUtilities.invokeLater(this::runSearch);
        }
    }

    private void installKeyBindings() {
        InputMap im = field.getInputMap(JComponent.WHEN_FOCUSED);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "search.down");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "search.up");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "search.enter");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "search.esc");
        field.getActionMap().put("search.down", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { moveSelection(1); }
        });
        field.getActionMap().put("search.up", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { moveSelection(-1); }
        });
        field.getActionMap().put("search.enter", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { openSelected(); }
        });
        field.getActionMap().put("search.esc", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { resetSearch(); }
        });
    }

    /**
     * The popup is unfocusable, so focus never moves into it; hide results when
     * the field loses focus (clicking a row is processed only after a focus
     * event, so invokeLater lets the row's mousePressed win first).
     */
    private void installFocusHide() {
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) {
                SwingUtilities.invokeLater(SearchBoxPanel.this::hidePopup);
            }
        });
    }

    private void moveSelection(int delta) {
        if (rows.isEmpty()) return;
        if (selectedRow >= 0 && selectedRow < rows.size()) rows.get(selectedRow).setSelected(false);
        selectedRow = Math.max(0, Math.min(rows.size() - 1, selectedRow + delta));
        rows.get(selectedRow).setSelected(true);
    }

    private void openSelected() {
        if (selectedRow < 0 || selectedRow >= rows.size()) return;
        runTarget(rows.get(selectedRow).entry);
    }

    private void runTarget(SearchEntry entry) {
        hidePopup();
        targetRouter.accept(entry.targetKey());
    }

    private void resetSearch() {
        field.setText("");
        selectedRow = -1;
        hidePopup();
    }

    private void runSearch() {
        String query = field.getText().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            hidePopup();
            return;
        }
        List<SearchEntry> direct = new ArrayList<>();
        List<SearchEntry> maybe = new ArrayList<>();
        for (SearchEntry e : entries) {
            if (matchesDirect(e, query)) {
                if (direct.size() < MAX_DIRECT) direct.add(e);
            } else if (matchesHidden(e, query)) {
                if (maybe.size() < MAX_MAYBE) maybe.add(e);
            }
        }
        if (direct.isEmpty() && maybe.isEmpty()) {
            hidePopup();
            return;
        }
        buildPopupContent(direct, maybe);
        showPopup();
    }

    private boolean matchesDirect(SearchEntry e, String q) {
        if (e.title().toLowerCase(Locale.ROOT).contains(q)) return true;
        for (String tag : e.tags()) {
            if (tag.toLowerCase(Locale.ROOT).contains(q)) return true;
        }
        return false;
    }

    /**
     * Hidden keywords match bidirectionally in the current language: typing an
     * action-word like "replace"/«замена» finds features whose titles/tags never
     * mention it.
     */
    private boolean matchesHidden(SearchEntry e, String q) {
        for (String w : e.hidden()) {
            if (w.length() < 3) continue;
            String wl = w.toLowerCase(Locale.ROOT);
            if (wl.contains(q) || q.contains(wl)) return true;
        }
        return false;
    }

    private void buildPopupContent(List<SearchEntry> direct, List<SearchEntry> maybe) {
        popupContent.removeAll();
        rows.clear();
        selectedRow = -1;
        popupContent.setLayout(new BoxLayout(popupContent, BoxLayout.Y_AXIS));
        popupContent.setBackground(UIStyle.SECONDARY_BG);

        for (SearchEntry e : direct) popupContent.add(createRow(e));
        if (!maybe.isEmpty()) {
            popupContent.add(createSectionHeader(I18n.t("search.maybe")));
            for (SearchEntry e : maybe) popupContent.add(createRow(e));
        }
        popupContent.add(createFooter());
    }

    private RowPanel createRow(SearchEntry entry) {
        RowPanel row = new RowPanel(entry);
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                row.setSelected(true);
                selectedRow = rows.indexOf(row);
            }
            @Override public void mouseExited(MouseEvent e) {
                row.setSelected(false);
            }
            @Override public void mousePressed(MouseEvent e) {
                runTarget(entry);
            }
        };
        applyTo(row, mouse);
        for (Component c : row.getComponents()) {
            applyTo(c, mouse);
            if (c instanceof JPanel badgePanel) {
                for (Component badge : badgePanel.getComponents()) applyTo(badge, mouse);
            }
        }
        rows.add(row);
        return row;
    }

    private void applyTo(Component c, MouseAdapter mouse) {
        c.addMouseListener(mouse);
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private JLabel createSectionHeader(String text) {
        JLabel header = new JLabel(text);
        header.setForeground(Color.GRAY);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 11f));
        header.setBorder(BorderFactory.createEmptyBorder(10, 14, 4, 14));
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        return header;
    }

    private JComponent createFooter() {
        JLabel footer = new JLabel(I18n.t("search.tip"));
        footer.setForeground(Color.GRAY);
        footer.setFont(footer.getFont().deriveFont(11f));
        footer.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        footer.setOpaque(false);
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        return footer;
    }

    private void showPopup() {
        if (popup == null) {
            Window owner = SwingUtilities.getWindowAncestor(field);
            popup = new JWindow(owner);
            popup.setFocusableWindowState(false);
            popupContent.setOpaque(true);
            popup.add(popupContent);
        }
        popupContent.revalidate();
        popupContent.repaint();

        Point fieldLoc = field.getLocationOnScreen();
        int x = fieldLoc.x - 30;
        int y = fieldLoc.y + field.getHeight() + 8;
        int width = field.getWidth() + 60;

        // never let the popup fall off the bottom of the screen
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int prefH = popupContent.getPreferredSize().height;
        int maxH = Math.min(prefH, screen.height - y - 20);
        popup.setBounds(x, y, Math.max(width, 320), Math.max(maxH, 60));
        popup.setVisible(true);
    }

    private void hidePopup() {
        if (popup != null) popup.setVisible(false);
    }

    /** One result row: two lines (title + tags) on the left, path on the right. */
    private class RowPanel extends JPanel {
        final SearchEntry entry;

        RowPanel(SearchEntry entry) {
            this.entry = entry;
            setLayout(new BorderLayout(10, 0));
            setOpaque(true);
            setBackground(UIStyle.SECONDARY_BG);
            setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
            setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel left = new JPanel();
            left.setOpaque(false);
            left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

            JLabel title = new JLabel(entry.title());
            title.setForeground(UIStyle.TEXT_COLOR);
            title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
            title.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel badges = new JPanel();
            badges.setOpaque(false);
            badges.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 4));
            badges.setAlignmentX(Component.LEFT_ALIGNMENT);
            int color = 0;
            for (String tag : entry.tags()) {
                JLabel badge = new JLabel("#" + tag);
                badge.setForeground(Color.WHITE);
                badge.setOpaque(true);
                badge.setBackground(UIStyle.SEARCH_TAG_COLORS[color++ % UIStyle.SEARCH_TAG_COLORS.length]);
                badge.setFont(badge.getFont().deriveFont(11f));
                badge.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
                badges.add(badge);
            }

            left.add(title);
            left.add(badges);
            add(left, BorderLayout.CENTER);

            if (entry.pathKey() != null) {
                JLabel path = new JLabel(I18n.t(entry.pathKey()) + " > " + entry.title());
                path.setForeground(Color.GRAY);
                path.setFont(path.getFont().deriveFont(11f));
                add(path, BorderLayout.EAST);
            }
        }

        void setSelected(boolean selected) {
            setBackground(selected ? UIStyle.BUTTON_HOVER : UIStyle.SECONDARY_BG);
        }
    }
}

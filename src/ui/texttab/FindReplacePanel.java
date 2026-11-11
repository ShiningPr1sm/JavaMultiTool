package ui.texttab;

import service.TextReplaceService;
import ui.StyledDialog;
import ui.UIStyle;
import util.AppLogger;
import util.I18n;
import util.JavaFxFileChooser;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class FindReplacePanel extends JPanel {

    /** Ready-made cleanup regexes offered in the preset combo. */
    private record Preset(String key, String find, String replacement) {}

    private static final Preset[] PRESETS = {
            new Preset("frp.preset.emptyLines", "^\\s*[\\r\\n]", ""),
            new Preset("frp.preset.htmlTags", "<[^>]*>", ""),
            new Preset("frp.preset.trailingWs", "[ \\t]+$", ""),
            new Preset("frp.preset.emails", "\\b[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,}\\b", "[EMAIL]"),
            new Preset("frp.preset.ipv4", "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b", "[IP]"),
            new Preset("frp.preset.urls", "https?://\\S+", "[URL]")
    };

    private static final TextReplaceService.CaseStyle[] CASE_STYLES = {
            TextReplaceService.CaseStyle.CAMEL,
            TextReplaceService.CaseStyle.SNAKE,
            TextReplaceService.CaseStyle.KEBAB,
            TextReplaceService.CaseStyle.UPPER,
            TextReplaceService.CaseStyle.LOWER
    };

    private final TextReplaceService service = new TextReplaceService();

    private JTextArea textInput;
    private JTextArea resultArea;
    private JTextField findField;
    private JTextField replaceField;
    private JCheckBox useRegexBox;
    private JCheckBox matchCaseBox;
    private JCheckBox extractBox;
    private JComboBox<String> presetBox;
    private JTextField folderField;
    private JTextField extField;
    private JCheckBox bakBox;
    private JTable rulesTable;
    private DefaultTableModel rulesModel;
    private JButton runBtn;
    private JProgressBar progressBar;
    private volatile boolean taskRunning;
    private JScrollPane outerScroll;
    private final List<JScrollPane> innerScrolls = new ArrayList<>();

    public FindReplacePanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UIStyle.BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel(I18n.t("frp.title"), SwingConstants.CENTER);
        title.setForeground(UIStyle.ACCENT_COLOR);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        add(title, BorderLayout.NORTH);

        JPanel columns = new JPanel(new GridBagLayout());
        columns.setBackground(UIStyle.BG_COLOR);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 0.30;
        gc.weighty = 1.0;
        gc.insets = new Insets(0, 0, 0, 38);
        JPanel workspace = buildWorkspaceColumn();
        workspace.setPreferredSize(new Dimension(360, 660));
        columns.add(workspace, gc);
        gc.gridx = 1;
        gc.weightx = 0.70;
        gc.insets = new Insets(0, 0, 0, 0);
        JPanel toolsBox = new JPanel(new BorderLayout());
        toolsBox.setBackground(UIStyle.BG_COLOR);
        toolsBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        toolsBox.add(buildToolsColumn(), BorderLayout.CENTER);
        toolsBox.setPreferredSize(new Dimension(470, 660));
        columns.add(toolsBox, gc);

        JScrollPane scroll = new JScrollPane(columns,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        UIStyle.styleScrollBar(scroll);
        outerScroll = scroll;
        for (JScrollPane inner : innerScrolls) delegateWheelToParent(inner, scroll);
        add(scroll, BorderLayout.CENTER);
    }

    private JLabel sectionLabel(String key) {
        JLabel label = new JLabel(I18n.t(key));
        label.setForeground(UIStyle.TEXT_COLOR);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JScrollPane textScroll(JTextArea area, int height) {
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(100, height));
        scroll.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR, 1));
        scroll.getViewport().setBackground(UIStyle.BUTTON_BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        UIStyle.styleScrollBar(scroll);
        innerScrolls.add(scroll);
        return scroll;
    }

    private void delegateWheelToParent(JScrollPane inner, JScrollPane outer) {
        inner.addMouseWheelListener(e -> {
            JScrollBar bar = inner.getVerticalScrollBar();
            boolean up = e.getWheelRotation() < 0;
            boolean atEdge = up ? bar.getValue() == 0
                    : bar.getValue() >= bar.getMaximum() - bar.getVisibleAmount();
            if (atEdge) {
                outer.dispatchEvent(SwingUtilities.convertMouseEvent(inner, e, outer));
            }
        });
    }

    private JTextArea monoArea(boolean editable) {
        JTextArea area = new JTextArea();
        area.setEditable(editable);
        area.setFont(new Font("Consolas", Font.PLAIN, 13));
        area.setForeground(UIStyle.TEXT_COLOR);
        area.setBackground(UIStyle.BUTTON_BG);
        if (editable) {
            area.setCaretColor(UIStyle.TEXT_COLOR);
        } else {
            area.setCaretColor(new Color(0, 0, 0, 0));
        }
        area.setSelectionColor(UIStyle.BUTTON_HOVER);
        area.setSelectedTextColor(UIStyle.TEXT_COLOR);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    private JPanel buildWorkspaceColumn() {
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(UIStyle.BG_COLOR);

        JPanel inputHead = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        inputHead.setBackground(UIStyle.BG_COLOR);
        inputHead.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputHead.add(sectionLabel("frp.input"));
        JButton insertTestBtn = new JButton(I18n.t("frp.insertTest"));
        UIStyle.styleButton(insertTestBtn);
        insertTestBtn.addActionListener(e -> insertTestText());
        inputHead.add(insertTestBtn);
        left.add(inputHead);
        left.add(Box.createVerticalStrut(4));

        textInput = monoArea(true);
        left.add(textScroll(textInput, 160));

        left.add(Box.createVerticalStrut(8));
        JPanel caseBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        caseBar.setBackground(UIStyle.BG_COLOR);
        caseBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        String[] names = {"camelCase", "snake_case", "kebab-case", "UPPERCASE", "lowercase"};
        String[] tipKeys = {"frp.tipCamel", "frp.tipSnake", "frp.tipKebab", "frp.tipUpper", "frp.tipLower"};
        for (int i = 0; i < CASE_STYLES.length; i++) {
            TextReplaceService.CaseStyle style = CASE_STYLES[i];
            JButton btn = new JButton(names[i]);
            UIStyle.styleButton(btn);
            btn.setToolTipText(I18n.t(tipKeys[i]));
            btn.addActionListener(e -> transformInPlace(style));
            caseBar.add(btn);
        }
        left.add(caseBar);

        left.add(Box.createVerticalStrut(10));
        left.add(sectionLabel("frp.singleTitle"));
        left.add(Box.createVerticalStrut(4));

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setBackground(UIStyle.BG_COLOR);
        fields.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints fc = new GridBagConstraints();
        fc.gridx = 0;
        fc.gridy = 0;
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.insets = new Insets(2, 0, 2, 8);

        JLabel findLabel = new JLabel(I18n.t("frp.find"));
        findLabel.setForeground(UIStyle.TEXT_COLOR);
        fields.add(findLabel, fc);
        fc.gridx = 1;
        fc.weightx = 1.0;
        findField = new JTextField();
        UIStyle.styleTextField(findField);
        fields.add(findField, fc);
        fc.gridx = 0;
        fc.weightx = 0.0;
        fc.gridy = 1;
        JLabel replaceLabel = new JLabel(I18n.t("frp.replaceLbl"));
        replaceLabel.setForeground(UIStyle.TEXT_COLOR);
        fields.add(replaceLabel, fc);
        fc.gridx = 1;
        fc.weightx = 1.0;
        replaceField = new JTextField();
        UIStyle.styleTextField(replaceField);
        fields.add(replaceField, fc);
        left.add(fields);

        left.add(Box.createVerticalStrut(4));
        JPanel flagsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        flagsRow.setBackground(UIStyle.BG_COLOR);
        flagsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        useRegexBox = new JCheckBox(I18n.t("frp.useRegex"));
        matchCaseBox = new JCheckBox(I18n.t("frp.matchCase"));
        extractBox = new JCheckBox(I18n.t("frp.extract"));
        for (JCheckBox cb : new JCheckBox[]{useRegexBox, matchCaseBox, extractBox}) {
            UIStyle.styleCheckbox(cb);
            flagsRow.add(cb);
        }
        left.add(flagsRow);

        left.add(Box.createVerticalStrut(6));
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        actionRow.setBackground(UIStyle.BG_COLOR);
        actionRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        runBtn = new JButton(I18n.t("frp.runBtn"));
        UIStyle.styleButton(runBtn);
        runBtn.addActionListener(e -> runSingle());
        actionRow.add(runBtn);
        left.add(actionRow);

        left.add(Box.createVerticalStrut(10));
        left.add(sectionLabel("frp.result"));
        left.add(Box.createVerticalStrut(4));
        resultArea = monoArea(false);
        left.add(textScroll(resultArea, 180));
        left.add(Box.createVerticalGlue());
        return left;
    }

    private JPanel buildToolsColumn() {
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBackground(UIStyle.BG_COLOR);

        right.add(sectionLabel("frp.presetsTitle"));
        right.add(Box.createVerticalStrut(4));

        presetBox = new JComboBox<>();
        presetBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        presetBox.addItem(I18n.t("frp.presetNone"));
        for (Preset preset : PRESETS) presetBox.addItem(I18n.t(preset.key));
        UIStyle.styleComboBox(presetBox);
        presetBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        presetBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                int index = presetBox.getSelectedIndex() - 1;
                if (index >= 0 && index < PRESETS.length) applyPreset(PRESETS[index]);
            }
        });
        right.add(presetBox);

        right.add(Box.createVerticalStrut(6));
        JPanel quickRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        quickRow.setBackground(UIStyle.BG_COLOR);
        quickRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton trimBtn = new JButton(I18n.t("frp.trimWs"));
        UIStyle.styleButton(trimBtn);
        trimBtn.addActionListener(e -> quickClean("[ \\t]+$", ""));
        JButton emptyBtn = new JButton(I18n.t("frp.preset.emptyLines"));
        UIStyle.styleButton(emptyBtn);
        emptyBtn.addActionListener(e -> quickClean("^\\s*[\\r\\n]", ""));
        quickRow.add(trimBtn);
        quickRow.add(emptyBtn);
        right.add(quickRow);

        right.add(Box.createVerticalStrut(10));
        right.add(sectionLabel("frp.rulesTitle"));
        right.add(Box.createVerticalStrut(4));
        right.add(buildRulesBlock());

        right.add(Box.createVerticalStrut(10));
        right.add(sectionLabel("frp.batchTitle"));
        right.add(Box.createVerticalStrut(4));
        right.add(buildBatchBlock());
        right.add(Box.createVerticalGlue());
        return right;
    }

    private JPanel buildRulesBlock() {
        rulesModel = new DefaultTableModel(
                new Object[][]{}, new Object[]{I18n.t("frp.colFind"), I18n.t("frp.colReplace")}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };
        rulesTable = new JTable(rulesModel);
        rulesTable.setBackground(UIStyle.SECONDARY_BG);
        rulesTable.setForeground(Color.WHITE);
        rulesTable.setGridColor(UIStyle.BORDER_COLOR);
        rulesTable.setRowHeight(28);
        rulesTable.setFillsViewportHeight(true);
        rulesTable.setShowVerticalLines(true);
        rulesTable.getColumnModel().getColumn(0).setPreferredWidth(160);
        rulesTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        UIStyle.styleTable(rulesTable);

        DefaultCellEditor cellEditor = new DefaultCellEditor(new JTextField());
        JTextField editorField = (JTextField) cellEditor.getComponent();
        editorField.setBackground(UIStyle.BUTTON_BG);
        editorField.setForeground(UIStyle.TEXT_COLOR);
        editorField.setCaretColor(UIStyle.TEXT_COLOR);
        editorField.setBorder(BorderFactory.createLineBorder(UIStyle.ACCENT_COLOR, 1));
        rulesTable.setDefaultEditor(Object.class, cellEditor);

        JPanel wrap = new JPanel(new BorderLayout(0, 6));
        wrap.setBackground(UIStyle.BG_COLOR);
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        JScrollPane tableScroll = new JScrollPane(rulesTable);
        tableScroll.setPreferredSize(new Dimension(100, 150));
        tableScroll.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));
        tableScroll.getViewport().setBackground(UIStyle.SECONDARY_BG);
        tableScroll.getVerticalScrollBar().setUnitIncrement(16);
        UIStyle.styleScrollBar(tableScroll);
        innerScrolls.add(tableScroll);
        wrap.add(tableScroll, BorderLayout.CENTER);

        JPanel btnGrid = new JPanel(new GridLayout(0, 2, 6, 6));
        btnGrid.setBackground(UIStyle.BG_COLOR);
        JButton addBtn = new JButton(I18n.t("frp.addRule"));
        UIStyle.styleButton(addBtn);
        addBtn.addActionListener(e -> rulesModel.addRow(new Object[]{"", ""}));

        JButton delBtn = new JButton(I18n.t("frp.delRule"));
        UIStyle.styleButton(delBtn);
        delBtn.addActionListener(e -> {
            int sel = rulesTable.getSelectedRow();
            if (sel >= 0) rulesModel.removeRow(rulesTable.convertRowIndexToModel(sel));
        });

        JButton clearBtn = new JButton(I18n.t("frp.clearRules"));
        UIStyle.styleButton(clearBtn);
        clearBtn.addActionListener(e -> rulesModel.setRowCount(0));

        JButton rulesRun = new JButton(I18n.t("frp.rulesRun"));
        UIStyle.styleButton(rulesRun);
        rulesRun.addActionListener(e -> runAllRules());

        JButton exportBtn = new JButton(I18n.t("frp.exportJava"));
        UIStyle.styleButton(exportBtn);
        exportBtn.addActionListener(e -> exportToJava());

        btnGrid.add(addBtn);
        btnGrid.add(delBtn);
        btnGrid.add(clearBtn);
        btnGrid.add(rulesRun);
        btnGrid.add(exportBtn);
        wrap.add(btnGrid, BorderLayout.SOUTH);
        return wrap;
    }

    private JPanel buildBatchBlock() {
        JPanel batch = new JPanel(new GridBagLayout());
        batch.setBackground(UIStyle.BG_COLOR);
        batch.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints bc = new GridBagConstraints();
        bc.gridx = 0;
        bc.gridy = 0;
        bc.fill = GridBagConstraints.HORIZONTAL;
        bc.weightx = 1.0;
        bc.insets = new Insets(2, 0, 2, 0);

        JPanel folderRow = new JPanel(new BorderLayout(8, 0));
        folderRow.setBackground(UIStyle.BG_COLOR);
        JLabel folderLabel = new JLabel(I18n.t("frp.folder"));
        folderLabel.setForeground(UIStyle.TEXT_COLOR);
        folderField = new JTextField();
        UIStyle.styleTextField(folderField);
        JButton browseBtn = new JButton(I18n.t("frp.browse"));
        UIStyle.styleButton(browseBtn);
        browseBtn.addActionListener(e -> {
            File dir = JavaFxFileChooser.chooseDirectory(I18n.t("frp.browseTitle"));
            if (dir != null) folderField.setText(dir.getAbsolutePath());
        });
        folderRow.add(folderLabel, BorderLayout.WEST);
        folderRow.add(folderField, BorderLayout.CENTER);
        folderRow.add(browseBtn, BorderLayout.EAST);
        batch.add(folderRow, bc);
        bc.gridy = 1;

        JPanel extRow = new JPanel(new BorderLayout(8, 0));
        extRow.setBackground(UIStyle.BG_COLOR);
        JLabel extLabel = new JLabel(I18n.t("frp.extensions"));
        extLabel.setForeground(UIStyle.TEXT_COLOR);
        extField = new JTextField(".txt, .md, .json, .csv, .log");
        UIStyle.styleTextField(extField);
        extRow.add(extLabel, BorderLayout.WEST);
        extRow.add(extField, BorderLayout.CENTER);
        batch.add(extRow, bc);
        bc.gridy = 2;

        JPanel runRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        runRow.setBackground(UIStyle.BG_COLOR);
        bakBox = new JCheckBox(I18n.t("frp.bak"), true);
        UIStyle.styleCheckbox(bakBox);
        JButton runBatchBtn = new JButton(I18n.t("frp.run"));
        UIStyle.styleButton(runBatchBtn);
        runBatchBtn.addActionListener(e -> runBatch());
        runRow.add(bakBox);
        runRow.add(runBatchBtn);
        batch.add(runRow, bc);
        bc.gridy = 3;

        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        UIStyle.styleProgressBar(progressBar);
        batch.add(progressBar, bc);
        return batch;
    }

    private void applyPreset(Preset preset) {
        findField.setText(preset.find);
        replaceField.setText(preset.replacement);
        useRegexBox.setSelected(true);
        boolean alreadyInTable = false;
        for (int i = 0; i < rulesModel.getRowCount() && !alreadyInTable; i++) {
            alreadyInTable = preset.find.equals(String.valueOf(rulesModel.getValueAt(i, 0)))
                    && preset.replacement.equals(String.valueOf(rulesModel.getValueAt(i, 1)));
        }
        if (!alreadyInTable) rulesModel.addRow(new Object[]{preset.find, preset.replacement});
        AppLogger.info("Preset rule added: " + I18n.t(preset.key));
    }

    private void insertTestText() {
        try (InputStream in = getClass().getResourceAsStream("/examples/findreplace/find_replace_example.txt")) {
            if (in == null) {
                AppLogger.error("Test file not found: examples/findreplace/find_replace_example.txt");
                return;
            }
            textInput.setText(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            AppLogger.info("Test text inserted into Find & Replace");
        } catch (IOException e) {
            AppLogger.error("Failed to read test text: " + e.getMessage());
        }
    }

    private void transformInPlace(TextReplaceService.CaseStyle style) {
        int start = textInput.getSelectionStart();
        int end = textInput.getSelectionEnd();
        if (start != end) {
            textInput.replaceRange(service.transform(textInput.getSelectedText(), style), start, end);
            AppLogger.info("Case transform applied to selection: " + style);
            return;
        }
        String src = textInput.getText();
        if (src.isEmpty()) {
            StyledDialog.show(window(), I18n.t("frp.errEmptyText"));
            return;
        }
        textInput.setText(service.transform(src, style));
        AppLogger.info("Case transform applied to whole text: " + style);
    }

    private void quickClean(String find, String replace) {
        String src = textInput.getText();
        if (src.isEmpty()) {
            StyledDialog.show(window(), I18n.t("frp.errEmptyText"));
            return;
        }
        textInput.setText(service.applyRules(src, List.of(new TextReplaceService.Rule(find, replace))));
    }

    private List<TextReplaceService.Rule> collectRules() {
        List<TextReplaceService.Rule> rules = new ArrayList<>();
        for (int i = 0; i < rulesModel.getRowCount(); i++) {
            String find = String.valueOf(rulesModel.getValueAt(i, 0)).trim();
            if (!find.isEmpty()) {
                rules.add(new TextReplaceService.Rule(
                        find, String.valueOf(rulesModel.getValueAt(i, 1))));
            }
        }
        return rules;
    }

    private void runSingle() {
        if (taskRunning) return;
        String src = textInput.getText();
        if (src.isEmpty()) {
            StyledDialog.show(window(), I18n.t("frp.errEmptyText"));
            return;
        }
        String find = findField.getText();
        if (find.isEmpty()) {
            StyledDialog.show(window(), I18n.t("frp.errEmptyFind"));
            return;
        }
        boolean useRegex = useRegexBox.isSelected();
        if (useRegex && !validateRegex(find)) return;

        String result;
        if (extractBox.isSelected()) {
            result = service.collectMatches(
                    src, find, useRegex, matchCaseBox.isSelected());
            if (result.isEmpty()) {
                StyledDialog.show(window(), I18n.t("frp.noMatches"));
                return;
            }
        } else {
            result = service.applySingle(
                    src, find, replaceField.getText(), useRegex, matchCaseBox.isSelected());
        }
        resultArea.setText(result);
        AppLogger.info("Find & Replace run finished ("
                + (useRegex ? "regex" : "literal") + ", match case " + matchCaseBox.isSelected() + ")");
    }

    private boolean validateRegex(String pattern) {
        try {
            Pattern.compile(pattern);
            return true;
        } catch (PatternSyntaxException e) {
            StyledDialog.show(window(), I18n.t("frp.errBadRegex") + e.getDescription());
            return false;
        }
    }

    private void runAllRules() {
        if (taskRunning) return;
        String src = textInput.getText();
        if (src.isEmpty()) {
            StyledDialog.show(window(), I18n.t("frp.errEmptyText"));
            return;
        }
        List<TextReplaceService.Rule> rules = collectRules();
        if (rules.isEmpty()) {
            StyledDialog.show(window(), I18n.t("frp.errNoRules"));
            return;
        }
        resultArea.setText(service.applyRules(src, rules));
        AppLogger.info("All " + rules.size() + " replacement rule(s) applied");
    }

    private List<String> parseExtensions() {
        List<String> extensions = new ArrayList<>();
        for (String token : extField.getText().split("[,;\\s]+")) {
            token = token.trim().toLowerCase();
            if (token.startsWith(".")) token = token.substring(1);
            if (!token.isEmpty()) extensions.add(token);
        }
        return extensions;
    }

    private void runBatch() {
        if (taskRunning) return;
        File root = new File(folderField.getText().trim());
        if (!root.isDirectory()) {
            StyledDialog.show(window(), I18n.t("frp.errNoSuchFolder"));
            return;
        }
        List<String> extensions = parseExtensions();
        if (extensions.isEmpty()) {
            StyledDialog.show(window(), I18n.t("frp.errNoExtensions"));
            return;
        }
        List<TextReplaceService.Rule> rules = collectRules();
        if (rules.isEmpty()) {
            StyledDialog.show(window(), I18n.t("frp.errNoRules"));
            return;
        }
        List<TextReplaceService.Rule> snapshot = rules;
        File directory = root;
        boolean backup = bakBox.isSelected();
        if (!backup && !StyledDialog.confirmYesNo(window(),
                I18n.t("frp.warnNoBackup"), I18n.t("frp.warnNoBackupTitle"))) {
            return;
        }

        setBusy(true);
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(true);
        progressBar.setString(I18n.t("frp.progressRunning"));
        progressBar.setVisible(true);

        new SwingWorker<TextReplaceService.BatchResult, Void>() {
            @Override
            protected TextReplaceService.BatchResult doInBackground() throws Exception {
                return service.processDirectory(directory, extensions, snapshot, backup);
            }

            @Override
            protected void done() {
                setBusy(false);
                progressBar.setIndeterminate(false);
                try {
                    TextReplaceService.BatchResult result = get();
                    String summary = I18n.t("frp.done",
                            String.valueOf(result.scanned()),
                            String.valueOf(result.changed()),
                            String.valueOf(result.failed()));
                    progressBar.setString(summary);
                    resultArea.setText(summary);
                    AppLogger.info("Folder replacement done on '" + directory + "': "
                            + result.scanned() + " scanned, " + result.changed()
                            + " changed, " + result.failed() + " failed");
                } catch (Exception e) {
                    progressBar.setString(I18n.t("common.error"));
                    AppLogger.error("Folder replacement failed: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        taskRunning = busy;
        runBtn.setEnabled(!busy);
    }

    private void exportToJava() {
        List<TextReplaceService.Rule> rules = collectRules();
        if (rules.isEmpty()) {
            StyledDialog.show(window(), I18n.t("frp.errNoRules"));
            return;
        }

        StringBuilder sb = new StringBuilder("String result = text");
        for (TextReplaceService.Rule rule : rules) {
            sb.append("\n        .replaceAll(")
                    .append(literal("(?m)" + rule.find()))
                    .append(", ");
            String repl = rule.replacement();
            if (repl.indexOf('\\') >= 0 || repl.indexOf('$') >= 0) {
                sb.append("Matcher.quoteReplacement(").append(literal(repl)).append(')');
            } else {
                sb.append(literal(repl));
            }
            sb.append(')');
        }
        sb.append(';');

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBackground(UIStyle.BG_COLOR);

        JTextArea code = new JTextArea(sb.toString(), 10, 60);
        code.setEditable(false);
        code.setFont(new Font("Consolas", Font.PLAIN, 13));
        code.setForeground(UIStyle.TEXT_COLOR);
        code.setBackground(UIStyle.BUTTON_BG);
        code.setCaretColor(new Color(0, 0, 0, 0));
        code.setSelectionColor(UIStyle.BUTTON_HOVER);
        code.setSelectedTextColor(UIStyle.TEXT_COLOR);
        JScrollPane codeScroll = new JScrollPane(code);
        UIStyle.styleScrollBar(codeScroll);
        content.add(codeScroll, BorderLayout.CENTER);

        JButton copyBtn = new JButton(I18n.t("common.copy"));
        UIStyle.styleButton(copyBtn);
        copyBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(code.getText()), null);
            AppLogger.info("Java replacement snippet copied to clipboard");
        });
        content.add(copyBtn, BorderLayout.SOUTH);

        StyledDialog.confirm(window(), content, I18n.t("frp.exportTitle"));
    }

    private static String literal(String value) {
        return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private Window window() {
        return SwingUtilities.windowForComponent(this);
    }
}

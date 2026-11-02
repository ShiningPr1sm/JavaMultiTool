package ui.components;

import ui.StyledDialog;
import ui.UIStyle;
import util.AppLogger;
import util.I18n;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;

public class PomodoroPanel extends JPanel {
    private static final int WORK = 0;
    private static final int SHORT_BREAK = 1;
    private static final int LONG_BREAK = 2;

    private int phase = WORK;
    private int workMinutes = 25;
    private int shortBreakMinutes = 5;
    private int longBreakMinutes = 15;
    private int remainingSeconds;
    private int sessionCount = 0;
    private boolean running = false;
    /** true after the very first Start since last reset (Reset clears it). Used to fire onTimerStart once. */
    private boolean everStarted = false;
    private Runnable onTimerStart;

    private final JLabel phaseLabel = new JLabel(I18n.t("pomo.work"), SwingConstants.CENTER);
    private final JLabel timerLabel = new JLabel("25:00", SwingConstants.CENTER);
    private final JProgressBar progressBar = new JProgressBar();
    private final JLabel sessionLabel = new JLabel(I18n.t("pomo.sessions", "0"));

    private final JSpinner workSpinner = new JSpinner(new SpinnerNumberModel(25, 1, 120, 1));
    private final JSpinner shortBreakSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 60, 1));
    private final JSpinner longBreakSpinner = new JSpinner(new SpinnerNumberModel(15, 1, 120, 1));

    private final JButton startBtn = new JButton(I18n.t("pomo.start"));
    private final JButton resetBtn = new JButton(I18n.t("pomo.reset"));

    private final Timer countdownTimer = new Timer(1000, e -> tick());

    public PomodoroPanel() {
        setLayout(new BorderLayout());
        setBackground(UIStyle.BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        phaseLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        phaseLabel.setForeground(UIStyle.ACCENT_COLOR);

        timerLabel.setFont(new Font("Consolas", Font.BOLD, 64));
        timerLabel.setForeground(Color.WHITE);

        UIStyle.styleProgressBar(progressBar);

        sessionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sessionLabel.setForeground(Color.LIGHT_GRAY);
        sessionLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(UIStyle.BG_COLOR);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 10, 0);
        center.add(phaseLabel, c);

        c.gridy = 1;
        c.insets = new Insets(0, 0, 10, 0);
        center.add(timerLabel, c);

        c.gridy = 2;
        c.insets = new Insets(0, 0, 10, 0);
        progressBar.setPreferredSize(new Dimension(400, 16));
        center.add(progressBar, c);

        c.gridy = 3;
        c.insets = new Insets(0, 0, 10, 0);
        center.add(sessionLabel, c);

        c.gridy = 4;
        c.insets = new Insets(0, 0, 20, 0);
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnRow.setBackground(UIStyle.BG_COLOR);
        UIStyle.styleButton(startBtn);
        UIStyle.styleButton(resetBtn);
        btnRow.add(startBtn);
        btnRow.add(resetBtn);
        center.add(btnRow, c);

        c.gridy = 5;
        c.insets = new Insets(0, 0, 0, 0);
        JPanel config = new JPanel(new GridLayout(3, 2, 10, 5));
        config.setBackground(UIStyle.BG_COLOR);

        UIStyle.styleSpinner(workSpinner);
        UIStyle.styleSpinner(shortBreakSpinner);
        UIStyle.styleSpinner(longBreakSpinner);

        config.add(label(I18n.t("pomo.work.min")));
        config.add(workSpinner);
        config.add(label(I18n.t("pomo.short.min")));
        config.add(shortBreakSpinner);
        config.add(label(I18n.t("pomo.long.min")));
        config.add(longBreakSpinner);
        center.add(config, c);

        add(center, BorderLayout.CENTER);

        resetTimer();

        startBtn.addActionListener(e -> toggleStartPause());
        resetBtn.addActionListener(e -> resetTimer());

        workSpinner.addChangeListener(e -> {
            workMinutes = (int) workSpinner.getValue();
            if (phase == WORK && !running) {
                remainingSeconds = workMinutes * 60;
                updateDisplay();
            }
        });
        shortBreakSpinner.addChangeListener(e -> {
            shortBreakMinutes = (int) shortBreakSpinner.getValue();
            if (phase == SHORT_BREAK && !running) {
                remainingSeconds = shortBreakMinutes * 60;
                updateDisplay();
            }
        });
        longBreakSpinner.addChangeListener(e -> {
            longBreakMinutes = (int) longBreakSpinner.getValue();
            if (phase == LONG_BREAK && !running) {
                remainingSeconds = longBreakMinutes * 60;
                updateDisplay();
            }
        });
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.LIGHT_GRAY);
        return l;
    }

    public void setOnTimerStart(Runnable r) {
        this.onTimerStart = r;
    }

    private void toggleStartPause() {
        if (running) {
            countdownTimer.stop();
            running = false;
            startBtn.setText(I18n.t("pomo.resume"));
            AppLogger.info("Pomodoro paused");
        } else {
            boolean isFirstStart = !everStarted;
            everStarted = true;
            countdownTimer.start();
            running = true;
            startBtn.setText(I18n.t("pomo.pause"));
            AppLogger.info("Pomodoro " + (isFirstStart ? "started" : "resumed"));
            if (isFirstStart && onTimerStart != null) {
                onTimerStart.run();
            }
        }
    }

    private void resetTimer() {
        countdownTimer.stop();
        running = false;
        everStarted = false;
        startBtn.setText(I18n.t("pomo.start"));
        switch (phase) {
            case WORK -> remainingSeconds = workMinutes * 60;
            case SHORT_BREAK -> remainingSeconds = shortBreakMinutes * 60;
            case LONG_BREAK -> remainingSeconds = longBreakMinutes * 60;
        }
        updateDisplay();
    }

    private void tick() {
        if (remainingSeconds > 0) {
            remainingSeconds--;
            updateDisplay();
        }
        if (remainingSeconds == 0) {
            countdownTimer.stop();
            running = false;
            startBtn.setText(I18n.t("pomo.start"));
            Toolkit.getDefaultToolkit().beep();
            onPhaseComplete();
        }
    }

    private void onPhaseComplete() {
        switch (phase) {
            case WORK -> {
                sessionCount++;
                sessionLabel.setText(I18n.t("pomo.sessions", String.valueOf(sessionCount)));
                if (sessionCount % 4 == 0) {
                    phase = LONG_BREAK;
                } else {
                    phase = SHORT_BREAK;
                }
            }
            case SHORT_BREAK, LONG_BREAK -> phase = WORK;
        }

        String phaseName = switch (phase) {
            case WORK -> I18n.t("pomo.work");
            case SHORT_BREAK -> I18n.t("pomo.short");
            default -> I18n.t("pomo.long");
        };
        phaseLabel.setText(phaseName);
        phaseLabel.setForeground(phase == WORK ? UIStyle.ACCENT_COLOR : new Color(0x2ECC71));

        AppLogger.info("Pomodoro phase completed, next: " + phaseName);
        resetTimer();
        StyledDialog.show(SwingUtilities.getWindowAncestor(this), I18n.t("pomo.title"), I18n.t("pomo.started", phaseName));
    }

    private void updateDisplay() {
        int mins = remainingSeconds / 60;
        int secs = remainingSeconds % 60;
        timerLabel.setText(String.format("%02d:%02d", mins, secs));

        int total = switch (phase) {
            case WORK -> workMinutes * 60;
            case SHORT_BREAK -> shortBreakMinutes * 60;
            default -> longBreakMinutes * 60;
        };
        int progress = total > 0 ? (int) ((double) (total - remainingSeconds) / total * 100) : 0;
        progressBar.setValue(progress);
    }
}

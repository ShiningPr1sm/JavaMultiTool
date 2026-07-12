package ui.components;

import ui.StyledDialog;
import ui.UIStyle;

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

    private final JLabel phaseLabel = new JLabel("Work", SwingConstants.CENTER);
    private final JLabel timerLabel = new JLabel("25:00", SwingConstants.CENTER);
    private final JProgressBar progressBar = new JProgressBar();
    private final JLabel sessionLabel = new JLabel("Sessions: 0");

    private final JSpinner workSpinner = new JSpinner(new SpinnerNumberModel(25, 1, 120, 1));
    private final JSpinner shortBreakSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 60, 1));
    private final JSpinner longBreakSpinner = new JSpinner(new SpinnerNumberModel(15, 1, 120, 1));

    private final JButton startBtn = new JButton("Start");
    private final JButton resetBtn = new JButton("Reset");

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

        config.add(label("Work (min):"));
        config.add(workSpinner);
        config.add(label("Short Break (min):"));
        config.add(shortBreakSpinner);
        config.add(label("Long Break (min):"));
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

    private void toggleStartPause() {
        if (running) {
            countdownTimer.stop();
            running = false;
            startBtn.setText("Resume");
        } else {
            countdownTimer.start();
            running = true;
            startBtn.setText("Pause");
        }
    }

    private void resetTimer() {
        countdownTimer.stop();
        running = false;
        startBtn.setText("Start");
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
            startBtn.setText("Start");
            Toolkit.getDefaultToolkit().beep();
            onPhaseComplete();
        }
    }

    private void onPhaseComplete() {
        switch (phase) {
            case WORK -> {
                sessionCount++;
                sessionLabel.setText("Sessions: " + sessionCount);
                if (sessionCount % 4 == 0) {
                    phase = LONG_BREAK;
                } else {
                    phase = SHORT_BREAK;
                }
            }
            case SHORT_BREAK, LONG_BREAK -> phase = WORK;
        }

        String phaseName = switch (phase) {
            case WORK -> "Work";
            case SHORT_BREAK -> "Short Break";
            default -> "Long Break";
        };
        phaseLabel.setText(phaseName);
        phaseLabel.setForeground(phase == WORK ? UIStyle.ACCENT_COLOR : new Color(0x2ECC71));

        resetTimer();
        StyledDialog.show(SwingUtilities.getWindowAncestor(this), "Pomodoro", phaseName + " phase started!");
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

package ui;

import db.DatabaseProvider;
import service.GreetingService;
import ui.components.ParticleField;

import javax.swing.*;
import java.awt.*;

public class WelcomePanel extends JPanel {
    private String greeting;
    private final String suffix;
    private int pos;
    private int phase;
    private int delayCounter;

    private int lastWidth = -1;
    private int lastHeight = -1;

    private final int circleRadius = 140;
    private int circleCenterX;
    private int circleCenterY;

    private Font[] fonts;
    private Font greetingFont;
    private final Font suffixFont = new Font("SansSerif", Font.PLAIN, 24);

    private int typingDelay;

    private double caretAlpha;
    private double caretDir = 1.0;
    private final double fadeSpeed;

    private final Timer timer;
    private final ParticleField particles;
    private final GreetingService greetingService;

    public WelcomePanel(String login, GreetingService greetingService) {
        this(login, greetingService, 0.5);
    }

    public WelcomePanel(String login, GreetingService greetingService, double initialFadeSpeed) {
        this.greetingService = greetingService;
        suffix = ", " + DatabaseProvider.getUserRepository().getNickname(login) + "!";
        setBackground(UIStyle.BG_COLOR);
        fadeSpeed = Math.max(0.0, Math.min(1.0, initialFadeSpeed));

        fonts = greetingService.loadGreetingFonts();
        particles = new ParticleField();
        resetAnimation();

        timer = new Timer(40, e -> {
            stepAnimation();
            updateCaretAlpha();
            particles.update();
            repaint();
        });

        SwingUtilities.invokeLater(() -> {
            if (!particles.isInitialized() && getWidth() > 0 && getHeight() > 0) {
                circleCenterX = getWidth() / 2;
                circleCenterY = getHeight() - 200;
                particles.setCircle(circleCenterX, circleCenterY, circleRadius);
                particles.init();
            }
        });

        timer.start();
    }

    private void resetAnimation() {
        greeting = greetingService.randomGreeting();
        greetingFont = greetingService.selectBestFontForGreeting(fonts, greeting);
        pos = 0;
        phase = 0;
        delayCounter = 0;
        typingDelay = 0;
        caretAlpha = 0.0;
        caretDir = 1.0;
    }

    private void stepAnimation() {
        switch (phase) {
            case 0:
                if (typingDelay > 0) typingDelay--;
                else if (pos < greeting.length()) {
                    pos++;
                    typingDelay = 2;
                } else {
                    phase = 1;
                    delayCounter = 25;
                }
                break;
            case 1:
                if (--delayCounter <= 0) phase = 2;
                break;
            case 2:
                if (pos > 0) pos--;
                else {
                    phase = 3;
                    delayCounter = 15;
                }
                break;
            case 3:
                if (--delayCounter <= 0) resetAnimation();
                break;
        }
    }

    private void updateCaretAlpha() {
        double currentAlphaStep = (fadeSpeed * 0.05) + 0.02;
        caretAlpha += caretDir * currentAlphaStep;
        if (caretAlpha >= 1.0) {
            caretAlpha = 1.0;
            caretDir = -1.0;
        } else if (caretAlpha <= 0.0) {
            caretAlpha = 0.0;
            caretDir = 1.0;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        int currentWidth = getWidth();
        int currentHeight = getHeight();

        if (currentWidth > 0 && currentHeight > 0 &&
                (circleCenterX == 0 || circleCenterY == 0 ||
                        currentWidth != lastWidth || currentHeight != lastHeight)) {
            lastWidth = currentWidth;
            lastHeight = currentHeight;
            circleCenterX = currentWidth / 2;
            circleCenterY = currentHeight - 200;
            particles.setCircle(circleCenterX, circleCenterY, circleRadius);
        }

        if (!particles.isInitialized() && currentWidth > 0 && currentHeight > 0) {
            particles.init();
        }

        String shown = greeting.substring(0, pos);
        FontMetrics fmG = g2.getFontMetrics(greetingFont);
        FontMetrics fmS = g2.getFontMetrics(suffixFont);

        int caretWidth = fmS.stringWidth("|");
        int textWidth = fmG.stringWidth(shown) + caretWidth + fmS.stringWidth(suffix);

        int x = (getWidth() - textWidth) / 2;
        int y = 50 + Math.max(fmG.getAscent(), fmS.getAscent());

        g2.setFont(greetingFont);
        g2.setColor(Color.WHITE);
        g2.drawString(shown, x, y);
        x += fmG.stringWidth(shown);

        g2.setFont(suffixFont);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) caretAlpha));
        g2.drawString("|", x, y);
        x += caretWidth;

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        g2.drawString(suffix, x, y);

        particles.paint(g2);
        g2.dispose();
    }
}

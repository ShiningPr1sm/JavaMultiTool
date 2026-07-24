package ui.components;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParticleField {

    private final int dotCount;
    private final int dotRadius;
    private final int connectDistance;
    private final Random rnd;
    private final List<Point> positions = new ArrayList<>();
    private final List<Point> velocities = new ArrayList<>();
    private final double[] pulsePhase;

    private int centerX;
    private int centerY;
    private int circleRadius;
    private boolean initialized;

    public ParticleField(int dotCount, int dotRadius, int connectDistance) {
        this.dotCount = dotCount;
        this.dotRadius = dotRadius;
        this.connectDistance = connectDistance;
        this.rnd = new Random();
        this.pulsePhase = new double[dotCount];
    }

    public ParticleField() {
        this(20, 4, 100);
    }

    public void setCircle(int centerX, int centerY, int radius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.circleRadius = radius;
        this.initialized = false;
    }

    public void init() {
        positions.clear();
        velocities.clear();

        for (int i = 0; i < dotCount; i++) {
            double angle = rnd.nextDouble() * 2 * Math.PI;
            double dist = rnd.nextDouble() * circleRadius;
            int x = (int) (centerX + dist * Math.cos(angle));
            int y = (int) (centerY + dist * Math.sin(angle));
            positions.add(new Point(x, y));

            int dx, dy;
            do {
                dx = rnd.nextInt(3) - 1;
                dy = rnd.nextInt(3) - 1;
            } while (dx == 0 && dy == 0);
            velocities.add(new Point(dx, dy));

            pulsePhase[i] = rnd.nextDouble() * Math.PI * 2;
        }

        initialized = true;
    }

    public void update() {
        if (positions.size() != dotCount) return;

        for (int i = 0; i < dotCount; i++) {
            Point p = positions.get(i);
            Point v = velocities.get(i);
            p.translate(v.x, v.y);

            double dx = p.x - centerX;
            double dy = p.y - centerY;
            double radiusSq = (double) circleRadius * circleRadius;
            double distSq = dx * dx + dy * dy;

            if (distSq > radiusSq) {
                double dist = Math.sqrt(distSq);
                double nx = dx / dist;
                double ny = dy / dist;

                p.x = (int) (centerX + nx * (circleRadius - 1));
                p.y = (int) (centerY + ny * (circleRadius - 1));

                int dxNew, dyNew;
                do {
                    dxNew = rnd.nextInt(3) - 1;
                    dyNew = rnd.nextInt(3) - 1;
                } while (dxNew == 0 && dyNew == 0);

                velocities.get(i).setLocation(dxNew, dyNew);
            }
        }

        for (int i = 0; i < dotCount; i++) {
            pulsePhase[i] += 0.15;
            if (pulsePhase[i] > Math.PI * 2) {
                pulsePhase[i] -= Math.PI * 2;
            }
        }
    }

    public void paint(Graphics2D g2) {
        for (int i = 0; i < dotCount; i++) {
            Point pi = positions.get(i);
            for (int j = i + 1; j < dotCount; j++) {
                Point pj = positions.get(j);
                double dist = pi.distance(pj);
                if (dist < connectDistance) {
                    float alpha = (float) (1.0 - dist / connectDistance);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    g2.setColor(Color.GRAY);
                    g2.drawLine(pi.x, pi.y, pj.x, pj.y);
                }
            }
        }

        for (int i = 0; i < dotCount; i++) {
            Point pi = positions.get(i);
            double pulse = 1.5 + Math.sin(pulsePhase[i]) * 1.5;
            int size = (int) (dotRadius + pulse);
            int drawX = pi.x - size / 2;
            int drawY = pi.y - size / 2;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            g2.setColor(Color.WHITE);
            g2.fillOval(drawX, drawY, size, size);
        }

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    public boolean isInitialized() {
        return initialized;
    }
}

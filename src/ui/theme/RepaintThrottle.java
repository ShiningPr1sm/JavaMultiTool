package ui.theme;

import javax.swing.JComponent;
import javax.swing.RepaintManager;
import javax.swing.Timer;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.HashMap;
import java.util.Map;

/**
 * Global repaint limiter. Replaces the default {@link RepaintManager} so that
 * dirty regions from any window are coalesced and flushed at a fixed cadence,
 * capping the paint rate of the whole application regardless of how fast
 * individual components request repaints (e.g. log and progress floods).
 */
public final class RepaintThrottle extends RepaintManager {

    private static final int FRAME_MS = 10;
    private static final long FRAME_NANOS = FRAME_MS * 1_000_000L;

    private final Map<JComponent, Rectangle> pendingComponents = new HashMap<>();
    private final Map<Window, Rectangle> pendingWindows = new HashMap<>();
    private final Object lock = new Object();
    private final Timer flushTimer = new Timer(FRAME_MS, e -> flush());
    private boolean flushScheduled;
    private long lastFlushNanos;

    private RepaintThrottle() {
        flushTimer.setRepeats(false);
    }

    /** Installs the throttling repaint manager for the entire application. */
    public static void install() {
        setCurrentManager(new RepaintThrottle());
    }

    @Override
    public void addDirtyRegion(JComponent c, int x, int y, int w, int h) {
        synchronized (lock) {
            merge(pendingComponents, c, x, y, w, h);
            scheduleFlush();
        }
    }

    @Override
    public void addDirtyRegion(Window window, int x, int y, int w, int h) {
        synchronized (lock) {
            merge(pendingWindows, window, x, y, w, h);
            scheduleFlush();
        }
    }

    private <K> void merge(Map<K, Rectangle> map, K key, int x, int y, int w, int h) {
        Rectangle r = map.get(key);
        if (r == null) {
            map.put(key, new Rectangle(x, y, w, h));
        } else {
            r.add(new Rectangle(x, y, w, h));
        }
    }

    private void scheduleFlush() {
        if (flushScheduled) return;
        flushScheduled = true;
        flushTimer.setInitialDelay(FRAME_MS);
        flushTimer.start();
    }

    private void flush() {
        Map<JComponent, Rectangle> components;
        Map<Window, Rectangle> windows;
        synchronized (lock) {
            long remain = FRAME_NANOS - (System.nanoTime() - lastFlushNanos);
            if (remain > 1_000_000L) {
                flushTimer.setInitialDelay((int) (remain / 1_000_000L));
                flushTimer.start();
                return;
            }
            lastFlushNanos = System.nanoTime();
            components = new HashMap<>(pendingComponents);
            pendingComponents.clear();
            windows = new HashMap<>(pendingWindows);
            pendingWindows.clear();
            flushScheduled = false;
        }

        for (Map.Entry<JComponent, Rectangle> e : components.entrySet()) {
            Rectangle r = e.getValue();
            super.addDirtyRegion(e.getKey(), r.x, r.y, r.width, r.height);
        }
        for (Map.Entry<Window, Rectangle> e : windows.entrySet()) {
            Rectangle r = e.getValue();
            super.addDirtyRegion(e.getKey(), r.x, r.y, r.width, r.height);
        }
    }
}

package util;

import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;

/**
 * Ensures only one instance of the program runs at a time, regardless of how it
 * was started (bundled jar or IDE) - both processes share the same app data dir,
 * so an OS-level file lock there is reliable for both.
 *
 * <p>When a second process launches, it fails to take the lock, writes a zero
 * byte signal file and exits <b>before</b> constructing any Service or opening
 * any database - so there is never concurrent DB access. The running instance
 * polls the signal file on one small daemon thread and shows the error dialog.</p>
 */
public final class SingleInstanceGuard {

    /** Text shown in the running instance when a duplicate launch was rejected. */
    public static final String DUPLICATE_MESSAGE = "Only one instance of the program may run at the same time.";
    /** Dialog title for the duplicate-launch warning. */
    public static final String DUPLICATE_TITLE = "Already Running";

    private static final String LOCK_FILE = "app.lock";
    private static final String SIGNAL_FILE = "app.lock.signal";

    /** How long a launch waits for a releasing instance (update-restart case) before deciding it is a duplicate. */
    private static final int GRACE_ATTEMPTS = 6;      // 6 x 500 ms = 3 s
    private static final long GRACE_DELAY_MS = 500;

    /** How often the running instance checks the signal file. */
    private static final long SIGNAL_POLL_MS = 500;

    private static RandomAccessFile lockRaf;
    private static FileChannel lockChannel;
    private static FileLock lock;
    private static final long startedAt = System.currentTimeMillis();

    private SingleInstanceGuard() {}

    /**
     * Tries to become the single running instance.
     *
     * @param onDuplicateAttempt executed inside the RUNNING instance (via EDT)
     *                           when another process tried to start while this one holds the lock
     * @return {@code true} when this process holds the lock and may continue
     */
    public static boolean acquire(Runnable onDuplicateAttempt) {
        File lockFile = new File(AppPaths.appDir(), LOCK_FILE);

        for (int attempt = 0; attempt < GRACE_ATTEMPTS; attempt++) {
            try {
                lockRaf = new RandomAccessFile(lockFile, "rw");
                lockChannel = lockRaf.getChannel();
                FileLock taken = lockChannel.tryLock();
                if (taken != null) {
                    lock = taken;
                    // Stale signal from an earlier session - clean it up.
                    File stale = new File(AppPaths.appDir(), SIGNAL_FILE);
                    if (stale.isFile() && stale.lastModified() <= startedAt) stale.delete();

                    AppLogger.info("SingleInstanceGuard: lock acquired in " + AppPaths.appDir());
                    startSignalWatcher(onDuplicateAttempt);
                    return true;
                }
            } catch (Exception e) {
                AppLogger.error("SingleInstanceGuard: lock check failed - " + e.getMessage());
                releaseQuietly();
                return false;
            }
            try {
                Thread.sleep(GRACE_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                releaseQuietly();
                return false;
            }
        }

        // still locked after the grace period -> a real duplicate launch: poke and exit
        File signal = new File(AppPaths.appDir(), SIGNAL_FILE);
        try {
            if (signal.exists()) Files.delete(signal.toPath());
            Files.write(signal.toPath(), new byte[0]);
        } catch (IOException e) {
            AppLogger.error("SingleInstanceGuard: could not write signal file - " + e.getMessage());
        }
        AppLogger.info("SingleInstanceGuard: another instance is running - this launch exits");
        return false;
    }

    /** Releases the lock when the JVM exits (also done implicitly by the OS on any exit path). */
    public static void release() {
        try {
            if (lock != null) lock.release();
        } catch (IOException ignored) {
            // nothing meaningful to do - the OS frees it on process exit anyway
        }
        releaseQuietly();
        lock = null;
    }

    private static void releaseQuietly() {
        try { if (lockRaf != null) lockRaf.close(); } catch (IOException ignored) {}
        lockRaf = null;
        lockChannel = null;
    }

    private static void startSignalWatcher(Runnable onDuplicateAttempt) {
        Thread watcher = new Thread(() -> {
            File signal = new File(AppPaths.appDir(), SIGNAL_FILE);
            while (true) {
                try {
                    Thread.sleep(SIGNAL_POLL_MS);
                } catch (InterruptedException e) {
                    return;
                }
                if (signal.isFile() && signal.lastModified() > startedAt - GRACE_DELAY_MS) {
                    signal.delete();
                    AppLogger.info("SingleInstanceGuard: duplicate launch detected");
                    if (onDuplicateAttempt != null) {
                        SwingUtilities.invokeLater(onDuplicateAttempt);
                    }
                }
            }
        }, "single-instance-guard");
        watcher.setDaemon(true);
        watcher.start();
    }
}

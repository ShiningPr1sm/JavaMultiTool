package util;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AppLogger {
    private static final String LOG_PATH = util.AppPaths.LOG_FILE;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> sessionLogs = new ArrayList<>();
    private static Consumer<String> consoleOutput;

    public static void log(String level, String message) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StackTraceElement caller = stackTrace[3];
        String fileName = caller.getFileName();
        int lineNumber = caller.getLineNumber();

        String timestamp = LocalDateTime.now().format(formatter);
        String logEntry = String.format("[%s] [%s] [%s:%d] %s",
                timestamp, level.toUpperCase(), fileName, lineNumber, message);

        System.out.println(logEntry);
        sessionLogs.add(logEntry);
        saveToFile(logEntry);
        if (consoleOutput != null) {
            consoleOutput.accept(logEntry + "\n");
        }
    }

    private static void saveToFile(String entry) {
        try (FileWriter fw = new FileWriter(LOG_PATH, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(entry);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setConsoleOutput(Consumer<String> output) {
        consoleOutput = output;
        for (String oldLog : sessionLogs) {
            output.accept(oldLog + "\n");
        }
    }

    public static void info(String msg) {
        log("INFO", msg);
    }
    public static void error(String msg) {
        log("ERROR", msg);
    }
    public static void admin(String msg) {
        log("ADMIN", msg);
    }
}

package service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UpdateApplier {
    public void restartWithNewJar(Path newJar) {
        try {
            Path currentJarPath = Paths.get(
                    UpdateApplier.class.getProtectionDomain()
                            .getCodeSource().getLocation().toURI()
            ).toAbsolutePath();
            Path newJarPath = newJar.toAbsolutePath();
            Path scriptPath = currentJarPath.getParent().resolve("javamultitool_update.bat");

            String script = "@echo off\r\n" +
                    "timeout /t 2 /nobreak > nul\r\n" +
                    ":loop\r\n" +
                    "del /f \"" + currentJarPath + "\"\r\n" +
                    "if exist \"" + currentJarPath + "\" (\r\n" +
                    "  timeout /t 1 > nul\r\n" +
                    "  goto loop\r\n" +
                    ")\r\n" +
                    "move /y \"" + newJarPath + "\" \"" + currentJarPath + "\"\r\n" +
                    "start javaw -jar \"" + currentJarPath + "\"\r\n" +
                    "del \"%~f0\"\r\n";

            Files.writeString(scriptPath, script);
            new ProcessBuilder("cmd", "/c", "start", "", scriptPath.toString()).start();
            System.exit(0);
        } catch (Exception e) {
            util.AppLogger.error("UpdateApplier: " + e.getMessage());
        }
    }
}

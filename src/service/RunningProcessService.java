package service;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

public class RunningProcessService {

    public Map<String, String> getRunningExes() {
        Map<String, String> apps = new TreeMap<>();
        ProcessHandle.allProcesses().forEach(ph -> ph.info().command().ifPresent(cmd -> {
            String exe = new File(cmd).getName();
            if (exe.toLowerCase().endsWith(".exe") && !exe.equalsIgnoreCase("java.exe"))
                apps.put(exe, exe);
        }));
        return apps;
    }

    public String prettifyExeName(String exeName) {
        String pretty = exeName.replace(".exe", "");
        pretty = pretty.substring(0, 1).toUpperCase() + pretty.substring(1).toLowerCase();
        return pretty;
    }
}

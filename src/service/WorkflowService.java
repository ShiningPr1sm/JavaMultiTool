package service;

import db.DatabaseProvider;
import util.AppLogger;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class WorkflowService {
    private int activeTaskId = -1;
    private long lastTickTime = System.currentTimeMillis();
    private boolean isRunning = false;

    public void startTracking() {
        if (isRunning) return;
        isRunning = true;
        new Thread(() -> {
            AppLogger.info("Background Workflow Service started.");
            while (isRunning) {
                try {
                    runTrackingCycle();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    private void runTrackingCycle() {
        long currentTime = System.currentTimeMillis();
        int secondsSinceLastTick = (int) ((currentTime - lastTickTime) / 1000);
        lastTickTime = currentTime;

        if (secondsSinceLastTick <= 0) return;

        var workflowRepo = DatabaseProvider.getWorkflowRepository();
        List<Object[]> appsToTrack = workflowRepo.getTrackedAppsFull();
        java.util.Set<String> runningExes = ProcessHandle.allProcesses()
                .map(ph -> ph.info().command().orElse(""))
                .filter(cmd -> !cmd.isEmpty())
                .map(cmd -> new File(cmd).getName().toLowerCase())
                .collect(Collectors.toSet());
        for (Object[] app : appsToTrack) {
            String exeName = (String) app[2];
            if (runningExes.contains(exeName.toLowerCase().trim())) {
                workflowRepo.addTime((int) app[0], 0, secondsSinceLastTick);
            }
        }

        if (activeTaskId != -1) {
            workflowRepo.addTime(activeTaskId, 1, secondsSinceLastTick);
        }
    }

    public void setActiveTaskId(int id) {
        activeTaskId = id;
    }

    public int getActiveTaskId() {
        return activeTaskId;
    }
}

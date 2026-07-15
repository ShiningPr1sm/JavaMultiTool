package db;

import db.impl.*;

public class DatabaseProvider {
    private static UserRepository userRepo;
    private static AchievementRepository achievementRepo;
    private static WorkflowRepository workflowRepo;
    private static BDaysRepository bdaysRepo;
    private static LevelRepository levelRepo;
    private static NotificationRepository notificationRepo;

    public static void reset() {
        userRepo = null;
        achievementRepo = null;
        workflowRepo = null;
        bdaysRepo = null;
        levelRepo = null;
        notificationRepo = null;
    }

    public static UserRepository getUserRepository() {
        if (userRepo == null) {
            userRepo = new UserRepositoryImpl();
            userRepo.initializeDatabase();
        }
        return userRepo;
    }

    public static AchievementRepository getAchievementRepository() {
        if (achievementRepo == null) {
            achievementRepo = new AchievementRepositoryImpl();
            achievementRepo.initializeDatabase();
        }
        return achievementRepo;
    }

    public static WorkflowRepository getWorkflowRepository() {
        if (workflowRepo == null) {
            workflowRepo = new WorkflowRepositoryImpl();
            workflowRepo.initializeDatabase();
        }
        return workflowRepo;
    }

    public static BDaysRepository getBDaysRepository() {
        if (bdaysRepo == null) {
            bdaysRepo = new BDaysRepositoryImpl();
            bdaysRepo.initializeDatabase();
        }
        return bdaysRepo;
    }

    public static LevelRepository getLevelRepository() {
        if (levelRepo == null) {
            levelRepo = new LevelRepositoryImpl();
            levelRepo.initializeDatabase();
        }
        return levelRepo;
    }

    public static NotificationRepository getNotificationRepository() {
        if (notificationRepo == null) {
            notificationRepo = new NotificationRepositoryImpl();
            notificationRepo.initializeDatabase();
        }
        return notificationRepo;
    }
}

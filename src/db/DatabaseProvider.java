package db;

import db.impl.*;

public class DatabaseProvider {
    private static UserRepository userRepo;
    private static AchievementRepository achievementRepo;
    private static WorkflowRepository workflowRepo;
    private static BDaysRepository bdaysRepo;
    private static LevelRepository levelRepo;

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
}

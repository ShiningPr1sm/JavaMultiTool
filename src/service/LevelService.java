package service;

import db.DatabaseProvider;

public class LevelService {

    public void initialize(String login) {
        var repo = DatabaseProvider.getLevelRepository();
        repo.ensureUserEntry(login);
    }

    public int getLevel(String login) {
        return DatabaseProvider.getLevelRepository().getLevel(login);
    }

    public int getXP(String login) {
        return DatabaseProvider.getLevelRepository().getXP(login);
    }

    public void addXP(String login, int amount) {
        DatabaseProvider.getLevelRepository().addXP(login, amount);
    }

    public String buildLevelText(String login) {
        var repo = DatabaseProvider.getLevelRepository();
        int lvl = repo.getLevel(login);
        int xp = repo.getXP(login);
        return String.format("Level: %d (%d XP)", lvl, xp);
    }

    public int calculateLevel(int xp) {
        return DatabaseProvider.getLevelRepository().calculateLevel(xp);
    }
}

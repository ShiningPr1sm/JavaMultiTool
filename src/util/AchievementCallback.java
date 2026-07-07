package util;

public interface AchievementCallback {
    void onAchievementLevelUp(String login, int xpReward);
    void onAchievementNotification(int xpReward);
}

package ui.utils;

public class UserSession {
    private static String currentUser;

    public static void setLogin(String login) {
        currentUser = login;
    }

    public static String getLogin() {
        return currentUser;
    }
}
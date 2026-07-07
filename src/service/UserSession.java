package service;

public class UserSession {
    private String currentUser;

    public void setLogin(String login) {
        currentUser = login;
    }

    public String getLogin() {
        return currentUser;
    }
}

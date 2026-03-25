package com.example.cab302project;

public class UserSession {
    private static UserSession instance;

    //private field holding current user object
    private User currentUser;

    private UserSession(User user) {
        this.currentUser = user;
    }

    //call when login is successful
    public static void login(User user) {
        instance = new UserSession(user);
    }

    //call to return session instance
    public static UserSession getInstance() {
        return instance;
    }

    //call to return current user object
    public User getUser() {
        return currentUser;
    }

    //call to logout
    public static void logout() {
        instance = null;
    }
}
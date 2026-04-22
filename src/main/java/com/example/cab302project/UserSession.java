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

    //helper to easily check darkmode for UI elements
    public static boolean isDarkMode() {
        // Get current session
        UserSession session = getInstance();
        // If session is not null, and user is not null
        if (session != null && session.getUser() != null) {
            // Return current user darkmode preference
            return session.getUser().isDarkMode();
        }
        return false; // Default if no one is logged in
    }
}
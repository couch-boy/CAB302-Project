package com.example.cab302project;

public class UserSession {
    private static UserSession instance;

    // Private field holding current user object
    private User currentUser;

    private UserSession(User user) {
        this.currentUser = user;
    }

    // Call when login is successful
    public static void login(User user) {
        instance = new UserSession(user);
    }

    // Call to return session instance
    public static UserSession getInstance() {
        return instance;
    }

    // Call to return current user object
    public User getUser() {
        return currentUser;
    }

    // Call to logout
    public static void logout() {
        instance = null;
    }

    // Helper to easily check darkmode for UI elements
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

    // Helper method to easily check if current user is a police officer
    // Can be used directly on UserSession instead of UserSession.currentUser
    // UserSession.isPolice() instead of UserSession.currentUser.isPolice()
    public static boolean isPolice() {
        UserSession session = getInstance();
        // If session is not null, and user is not null
        if (session != null && session.getUser() != null) {
            // Return true if user is a police officer
            return session.getUser().getUserType() == UserType.POLICE;
        }
        return false;
    }
}
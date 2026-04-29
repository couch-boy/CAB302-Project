package com.example.cab302project;

/**
 * Manages the global authentication state for the application.
 * This class uses a Singleton-like pattern to store the currently logged-in {@link User}.
 * It provides centralized access to user preferences (like Dark Mode) and
 * permission levels (like Police status) across all UI controllers.
 */
public class UserSession {

    /** The single active session instance. */
    private static UserSession instance;

    /** The {@link User} object associated with the current session. */
    private User currentUser;

    /**
     * Private constructor to initialize a session with a specific user.
     * @param user The authenticated user.
     */
    private UserSession(User user) {
        this.currentUser = user;
    }

    /**
     * Initializes a new global session upon successful authentication.
     *
     * @param user The {@link User} object to associate with the new session.
     */
    public static void login(User user) {
        instance = new UserSession(user);
    }

    /**
     * Retrieves the current active session.
     *
     * @return The current {@link UserSession} instance, or null if no user is logged in.
     */
    public static UserSession getInstance() {
        return instance;
    }

    /**
     * Gets the user data associated with the current session.
     *
     * @return The current {@link User} object.
     */
    public User getUser() {
        return currentUser;
    }

    /**
     * Terminates the current session by clearing the global instance.
     * This effectively logs the user out of the system.
     */
    public static void logout() {
        instance = null;
    }

    /**
     * Helper method to determine if the logged-in user prefers Dark Mode.
     * Used by UI controllers to apply CSS styling.
     *
     * @return true if a user is logged in and has Dark Mode enabled; false otherwise.
     */
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

    /**
     * Helper method to determine if the currently logged-in user has police permissions.
     * This provides a shorthand way to check roles without accessing the User object directly.
     *
     * @return true if the current user is a {@link UserType#POLICE} officer; false otherwise.
     */
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
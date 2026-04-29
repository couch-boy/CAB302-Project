package com.example.cab302project;

/**
 * Defines the access levels and roles available within the application.
 * This enum is used to distinguish between standard citizens and law enforcement
 * personnel, controlling UI visibility and functional permissions.
 */
public enum UserType {

    /** A standard citizen user with reporting and personal tracking capabilities. */
    REGULAR("Regular User"),

    /** A law enforcement user with elevated privileges for managing and actioning crime reports. */
    POLICE("Police Officer");

    /** The human-readable label used for UI components and display. */
    private final String displayName;

    /**
     * Constructs a UserType with a specific display label.
     *
     * @param displayName The string representation of the user role.
     */
    UserType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the display name of the user type.
     *
     * @return A string representing the user's role (e.g., "Police Officer").
     */
    @Override
    public String toString() { return displayName; }

    // Getter

    /**
     * Gets the human-readable display name of the user type.
     *
     * @return The display name string.
     */
    public String getDisplayName() {
        return displayName;
    }
}
package com.example.cab302project;

/**
 * Represents a user within the RADIUS system.
 * This class stores profile information, account credentials, and user preferences.
 * It distinguishes between regular citizens and police officers via the {@link UserType} enum.
 */
public class User {

    // Private backing fields
    private final String username;
    private String password;
    private String email;
    private String phone;
    private double homeLatitude;
    private double homeLongitude;
    private boolean darkMode;
    private UserType userType;

    /**
     * Constructs a new User with full profile details.
     *
     * @param username The unique identifier for the user (cannot be changed after creation).
     * @param password The account's authentication password.
     * @param email The user's contact email address.
     * @param phone The user's 10-digit contact phone number.
     * @param homeLatitude The latitude of the user's default home location (-90 to 90).
     * @param homeLongitude The longitude of the user's default home location (-180 to 180).
     * @param darkMode The user's UI theme preference (true for dark mode).
     * @param userType The {@link UserType} defining the user's permissions (REGULAR or POLICE).
     */
    public User(String username, String password, String email, String phone,
                double homeLatitude, double homeLongitude, boolean darkMode, UserType userType) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.phone = phone;

        // Basic validation using helper method
        if (UIUtils.isValidCoordinate(homeLatitude, homeLongitude)) {
            this.homeLatitude = homeLatitude;
            this.homeLongitude = homeLongitude;
        } else {
            // Default to 0, 0 if invalid
            this.homeLatitude = 0;
            this.homeLongitude = 0;
        }

        this.darkMode = darkMode;
        this.userType = userType;
    }

    // --- GETTERS ---

    /**
     * Gets the unique username identifier for the account.
     * @return The unique username associated with this account.
     */
    public String getUsername() { return username; }

    /**
     * Gets the account password.
     * @return The account's current password.
     */
    public String getPassword() { return password; }

    /**
     * Gets the registered email address.
     * @return The user's registered email address.
     */
    public String getEmail() { return email; }

    /**
     * Gets the registered phone number.
     * @return The user's registered phone number.
     */
    public String getPhone() { return phone; }

    /**
     * Gets the latitude of the home location.
     * @return The latitude coordinate of the user's home location.
     */
    public double getHomeLatitude() { return homeLatitude; }

    /**
     * Gets the longitude of the home location.
     * @return The longitude coordinate of the user's home location.
     */
    public double getHomeLongitude() { return homeLongitude; }

    /**
     * Checks if the dark mode preference is enabled.
     * @return true if the user has enabled dark mode; false otherwise.
     */
    public boolean isDarkMode() { return darkMode; }

    /**
     * Gets the user type role for permission handling.
     * @return The {@link UserType} of this account.
     */
    public UserType getUserType() { return userType; }

    // --- SETTERS ---

    // No setter for username, as this is the database primary key

    /**
     * Updates the account password.
     * @param password The new password to set for the account.
     */
    public void setPassword(String password) { this.password = password; }

    /**
     * Updates the registered email address.
     * @param email The new email address to set for the account.
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * Updates the registered phone number.
     * @param phone The new phone number to set for the account.
     */
    public void setPhone(String phone) { this.phone = phone; }

    // No setters for lat/lon, use setHomeLocation to set both

    /**
     * Updates the user's preference for the dark mode UI theme.
     * @param darkMode The new UI theme preference.
     */
    public void setDarkMode(boolean darkMode) { this.darkMode = darkMode; }

    // No setter for userType, as this should not change

    /**
     * Updates the user's home geographic coordinates using validation.
     *
     * @param lat The new latitude coordinate.
     * @param lon The new longitude coordinate.
     */
    public void setHomeLocation(double lat, double lon) {
        if (UIUtils.isValidCoordinate(lat, lon)) {
            this.homeLatitude = lat;
            this.homeLongitude = lon;
        }
    }

    /**
     * Checks if the user has police-level permissions.
     * Used primarily by {@link LoginController} to determine which dashboard to load.
     *
     * @return true if the user is a police officer; false otherwise.
     */
    public boolean isPolice() {
        return this.userType == UserType.POLICE;
    }
}
package com.example.cab302project;

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

    public User(String username, String password, String email, String phone,
                double homeLatitude, double homeLongitude, boolean darkMode, UserType userType) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.phone = phone;
        this.homeLatitude = homeLatitude;
        this.homeLongitude = homeLongitude;
        this.darkMode = darkMode;
        this.userType = userType;
    }

    // --- GETTERS ---
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public double getHomeLatitude() { return homeLatitude; }
    public double getHomeLongitude() { return homeLongitude; }
    public boolean isDarkMode() { return darkMode; }
    public UserType getUserType() { return userType; }

    // --- SETTERS ---
    // No setter for username, as this is the database primary key
    public void setPassword(String password) { this.password = password; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    // No setters for lat/lon, use setHomeLocation to set both
    public void setDarkMode(boolean darkMode) { this.darkMode = darkMode; }
    // No setter for userType, as this should not change

    public void setHomeLocation(double lat, double lon) {
        // Basic coordinate range validation (-90 to 90 for lat, -180 to 180 for lon)
        if (lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180) {
            this.homeLatitude = lat;
            this.homeLongitude = lon;
        }
    }

    // Method to check if user is a police officer
    public boolean isPolice() {
        return this.userType == UserType.POLICE;
    }
}
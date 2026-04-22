package com.example.cab302project;

import java.time.LocalDateTime;

public class CrimeRecord {
    // Private backing fields
    private int id;
    private CrimeCategory category;
    private LocalDateTime timestamp;
    private double latitude;
    private double longitude;
    private String description;
    private String reporter;
    private boolean actioned;

    // Constructor
    // When creating a CrimeRecord, whatever you put as the ID is up to the following:
    // If you are adding a new crime, any ID will be overwritten as the CrimeRecord is added to the database
    // If you are updating a crime, make sure you capture the previous CrimeRecord object's ID value,
    //      since otherwise the incorrect record will be updated
    public CrimeRecord(int id, CrimeCategory category, LocalDateTime timestamp, double latitude, double longitude,
                       String description, String reporter, boolean actioned) {
        this.id = id;
        this.category = category;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
        this.reporter = reporter;
        this.actioned = actioned;
    }

    // --- GETTERS ---
    public int getId() { return id; }
    public CrimeCategory getCategory() { return category; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getDescription() { return description; }
    public String getReporter() { return reporter; }
    public boolean isActioned() { return actioned; }

    // --- SETTERS ---
    // No setter for id, as this is the primary key, and is managed by the database
    public void setCategory(CrimeCategory category) { this.category = category; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    // No setters for lat/lon, use setLocation to set both
    public void setDescription(String description) { this.description = description; }
    public void setReporter(String reporter) { this.reporter = reporter; }
    public void setActioned(boolean actioned) { this.actioned = actioned; }

    public void setLocation(double lat, double lon) {
        // Basic coordinate range validation (-90 to 90 for lat, -180 to 180 for lon)
        if (lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180) {
            this.latitude = lat;
            this.longitude = lon;
        }
    }

    /**
     * Helper method for UI: Returns "Anonymous" if reporter is null.
     * Use when displaying the user who reported a crime.
     */
    public String getReporterDisplayName() {
        return (reporter == null || reporter.isEmpty()) ? "Anonymous" : reporter;
    }

    /**
     * Helper method for DAO: Returns the timestamp as a String for SQLite
     */
    public String getTimestampForDb() {
        return UIUtils.formatForDb(this.timestamp);
    }

}

package com.example.cab302project;

import java.time.LocalDateTime;

/**
 * Represents a single crime report within the system.
 * This class serves as a Data Transfer Object (DTO) containing information about
 * the type of crime, its location, the reporting user, and its current status.
 */
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

    /**
     * Constructs a new CrimeRecord with full details.
     *
     * <p><b>Note on ID Management:</b></p>
     * <ul>
     *   <li><b>New Records:</b> If you are creating a record to be added to the database,
     *   the ID provided here will be overwritten by the database's auto-incrementing primary key.</li>
     *   <li><b>Existing Records:</b> If updating an existing record, ensure you provide the
     *   original ID value to ensure the correct database entry is modified.</li>
     * </ul>
     *
     * @param id The unique identifier for the record.
     * @param category The {@link CrimeCategory} defining the type and severity of the crime.
     * @param timestamp The date and time the incident occurred or was reported.
     * @param latitude The GPS latitude coordinate (-90 to 90).
     * @param longitude The GPS longitude coordinate (-180 to 180).
     * @param description A detailed text description of the incident.
     * @param reporter The name or ID of the user reporting the crime.
     * @param actioned Whether the report has been addressed by authorities.
     */
    public CrimeRecord(int id, CrimeCategory category, LocalDateTime timestamp, double latitude, double longitude,
                       String description, String reporter, boolean actioned) {
        this.id = id;
        this.category = category;
        this.timestamp = timestamp;

        // Basic validation using helper method
        if (UIUtils.isValidCoordinate(latitude, longitude)) {
            this.latitude = latitude;
            this.longitude = longitude;
        } else {
            // Default to 0, 0 if invalid
            this.latitude = 0;
            this.longitude = 0;
        }

        this.description = description;
        this.reporter = reporter;
        this.actioned = actioned;
    }

    // --- GETTERS ---

    /**
     * Retrieves the database primary key ID.
     * @return The unique database identifier for this record.
     */
    public int getId() { return id; }

    /**
     * Retrieves the category classification of the crime.
     * @return The {@link CrimeCategory} of this record.
     */
    public CrimeCategory getCategory() { return category; }

    /**
     * Retrieves the date and time associated with the report.
     * @return The {@link LocalDateTime} associated with the report.
     */
    public LocalDateTime getTimestamp() { return timestamp; }

    /**
     * Retrieves the latitude of the reported incident.
     * @return The latitude coordinate of the incident.
     */
    public double getLatitude() { return latitude; }

    /**
     * Retrieves the longitude of the reported incident.
     * @return The longitude coordinate of the incident.
     */
    public double getLongitude() { return longitude; }

    /**
     * Retrieves the full text description of the report.
     * @return The detailed description of the incident.
     */
    public String getDescription() { return description; }

    /**
     * Retrieves the identifier of the reporting user.
     * @return The name or identifier of the reporter.
     */
    public String getReporter() { return reporter; }

    /**
     * Checks if the report has been marked as actioned by authorities.
     * @return true if the incident has been actioned, false otherwise.
     */
    public boolean isActioned() { return actioned; }

    // --- SETTERS ---

    // No setter for id, as this is the primary key, and is managed by the database

    /**
     * Sets the crime category classification.
     * @param category The new {@link CrimeCategory} to assign.
     */
    public void setCategory(CrimeCategory category) { this.category = category; }

    /**
     * Sets the timestamp for the incident.
     * @param timestamp The new {@link LocalDateTime} to assign.
     */
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    // No setters for lat/lon, use setLocation to set both

    /**
     * Sets the descriptive text for the incident.
     * @param description The new text description for the report.
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Sets the identifier for the reporting user.
     * @param reporter The name of the user reporting the incident.
     */
    public void setReporter(String reporter) { this.reporter = reporter; }

    /**
     * Sets the current actioned status of the report.
     * @param actioned The new status indicating if the report is addressed.
     */
    public void setActioned(boolean actioned) { this.actioned = actioned; }

    /**
     * Updates the geographic coordinates of the incident using validation.
     *
     * @param lat Latitude between -90 and 90.
     * @param lon Longitude between -180 and 180.
     */
    public void setLocation(double lat, double lon) {
        if (UIUtils.isValidCoordinate(lat, lon)) {
            this.latitude = lat;
            this.longitude = lon;
        }
    }

    /**
     * Helper method for UI components.
     * @return The reporter's name, or "Anonymous" if null or empty.
     */
    public String getReporterDisplayName() {
        return (reporter == null || reporter.isEmpty()) ? "Anonymous" : reporter;
    }

    /**
     * Helper method for DAO: Returns the timestamp as a String for SQLite
     * @return A formatted string of the timestamp.
     */
    public String getTimestampForDb() {
        return UIUtils.formatForDb(this.timestamp);
    }

}

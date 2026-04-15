package com.example.cab302project;

public class CrimeRecord {
    private final int id;
    private final CrimeCategory category;
    private final CrimeSeverity severity;
    private final String reporter;
    private final String description;
    private final double latitude;
    private final double longitude;

    public CrimeRecord(int id, CrimeCategory category, CrimeSeverity severity,
                       String reporter, String description, double latitude, double longitude) {
        this.id = id;
        this.category = category;
        this.severity = severity;
        this.reporter = reporter;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    //getters
    public int getId() { return id; }
    public CrimeCategory getCategory() { return category; }
    public CrimeSeverity getSeverity() { return severity; }
    public String getReporter() { return reporter; }
    public String getDescription() { return description; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}

package com.example.cab302project;

public class Hotspot {
    private final double latitude;
    private final double longitude;
    private final int count;

    /**
     * Constructs a hotspot using latitude, longitude, and number of crimes.
     */
    public Hotspot(double latitude, double longitude, int count) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.count = count;
    }

    /**
     * Returns the latitude of the hotspot.
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Returns the longitude of the hotspot.
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Returns the number of crimes in the hotspot.
     */
    public int getCount() {
        return count;
    }
}

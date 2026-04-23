package com.example.cab302project;

public class Hotspot {
    private final double latitude;
    private final double longitude;
    private final int count;

    public Hotspot(double latitude, double longitude, int count) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.count = count;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getCount() {
        return count;
    }
}

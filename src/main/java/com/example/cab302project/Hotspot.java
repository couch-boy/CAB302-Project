package com.example.cab302project;

/**
 * Represents a geographic crime hotspot, defined by a central coordinate
 * and the number of crimes clustered within that area.
 *
 * Used by the dashboard and police dashboard to render hotspot markers
 * on the map with intensity based on the crime count.
 */
public class Hotspot {
    private final double latitude;
    private final double longitude;
    private final int count;

    /**
     * Constructs a new Hotspot with the given coordinates and crime count.
     * @param latitude the latitude of the hotspot centre
     * @param longitude the longitude of the hotspot centre
     * @param count the number of crimes clustered at this hotspot
     */
    public Hotspot(double latitude, double longitude, int count) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.count = count;
    }

    /**
     * Returns the latitude of the hotspot centre.
     * @return latitude as a double
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Returns the longitude of the hotspot centre.
     * @return longitude as a double
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Returns the number of crimes clustered at this hotspot.
     * @return crime count as an integer
     */
    public int getCount() {
        return count;
    }
}

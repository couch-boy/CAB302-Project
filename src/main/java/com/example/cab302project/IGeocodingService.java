package com.example.cab302project;
import java.util.List;

/**
 * Interface defining the geocoding service used within the application.
 * It provides methods to convert addresses into geographic coordinates,
 * generate address suggestions for user input, and perform reverse geocoding
 * to obtain readable addresses from latitude and longitude values.
 */
public interface IGeocodingService {
    /**
     * Converts an address into latitude and longitude coordinates.
     */
    double[] geocodeAddress(String address) throws Exception;
    /**
     * Returns address suggestions based on a search query.
     */
    List<String> getAddressSuggestions(String query) throws Exception;
    /**
     * Converts coordinates into a readable address.
     */
    String reverseGeocode(double lat, double lon) throws Exception;
}

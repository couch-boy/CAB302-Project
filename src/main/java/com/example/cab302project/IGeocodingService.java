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
     * Converts an address string into geographic coordinates.
     * @param address the address string to geocode
     * @return a double array of length 2 where index 0 is latitude and index 1 is longitude
     * @throws Exception if the address cannot be found or the request fails
     */
    double[] geocodeAddress(String address) throws Exception;
    /**
     * Returns address suggestions based on a partial search query.
     * @param query the partial address string typed by the user
     * @return a list of matching address strings, or an empty list if none found
     * @throws Exception if the request fails
     */
    List<String> getAddressSuggestions(String query) throws Exception;
    /**
     * Converts latitude and longitude coordinates into a human-readable address.
     * @param lat the latitude coordinate to reverse geocode
     * @param lon the longitude coordinate to reverse geocode
     * @return a readable address string, or a formatted coordinate string if lookup fails
     * @throws Exception if the request fails
     */
    String reverseGeocode(double lat, double lon) throws Exception;
}

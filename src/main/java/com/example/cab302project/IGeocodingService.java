package com.example.cab302project;
import java.util.List;

public interface IGeocodingService {
    double[] geocodeAddress(String address) throws Exception;
    List<String> getAddressSuggestions(String query) throws Exception;
    String reverseGeocode(double lat, double lon) throws Exception;
}

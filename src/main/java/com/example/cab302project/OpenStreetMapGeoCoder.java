package com.example.cab302project;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;

/**
 * Implementation of the geocoding service using OpenStreetMap Nominatim API.
 * Provides functionality to convert addresses to coordinates, generate
 * address suggestions, and perform reverse geocoding.
 */

public class OpenStreetMapGeoCoder implements IGeocodingService {

    /**
     * Converts an address into latitude and longitude using the API.
     */
    @Override
    public  double[] geocodeAddress(String address) throws Exception {
        String encoded = URLEncoder.encode(address, StandardCharsets.UTF_8);

        String url = "https://nominatim.openstreetmap.org/search?q="
                + encoded + "&format=json&limit=1";

        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("GET");

        // REQUIRED for Nominatim (otherwise it may block you)
        conn.setRequestProperty("User-Agent", "CAB302-Project");

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
        );

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        JSONArray results = new JSONArray(response.toString());

        if (results.isEmpty()) {
            throw new IllegalArgumentException("Address not found");
        }

        JSONObject obj = results.getJSONObject(0);

        double lat = Double.parseDouble(obj.getString("lat"));
        double lon = Double.parseDouble(obj.getString("lon"));

        return new double[]{lat, lon};
    }

    /**
     * Retrieves address suggestions based on user input.
     */
    @Override
    public List<String> getAddressSuggestions(String query) throws Exception {
        List<String> suggestions = new ArrayList<>();

        if (query == null || query.trim().length() < 3) {
            return suggestions;
        }

        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);

        String url = "https://nominatim.openstreetmap.org/search?q="
                + encoded + "&format=json&limit=5";

        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "CAB302-Project");

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
        );

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        JSONArray results = new JSONArray(response.toString());

        for (int i = 0; i < results.length(); i++) {
            JSONObject obj = results.getJSONObject(i);
            suggestions.add(obj.getString("display_name"));
        }

        return suggestions;
    }

    /**
     * Converts latitude and longitude coordinates into a human-readable address
     * using the OpenStreetMap Nominatim reverse geocoding service.
     */

    @Override
    public String reverseGeocode(double lat, double lon) throws Exception {
        String url = "https://nominatim.openstreetmap.org/reverse?format=json&lat="
                + lat + "&lon=" + lon;

        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "CAB302-Project");

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
        );

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        JSONObject obj = new JSONObject(response.toString());

        if (obj.has("display_name")) {
            return obj.getString("display_name");
        }

        return String.format("%.4f, %.4f", lat, lon);
    }
}
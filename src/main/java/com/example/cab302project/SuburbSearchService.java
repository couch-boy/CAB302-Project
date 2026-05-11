package com.example.cab302project;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Service for searching suburbs and retrieving their boundary polygons.
 *
 * Uses the Nominatim API to search for a suburb by name and retrieve its
 * bounding box and GeoJSON polygon boundary. The boundary is used to
 * highlight the suburb on the map and filter crime records to only those
 * within the area.
 *
 * All calls are made from a background thread via the caller; this class
 * does not manage threading itself.
 */
public class SuburbSearchService {

    private static final Logger LOG = Logger.getLogger(SuburbSearchService.class.getName());

    private static final String NOMINATIM_BASE = "https://nominatim.openstreetmap.org";
    private static final String USER_AGENT = "RADIUS-App/1.0 (CAB302 University Project)";
    private static final int TIMEOUT_MS = 8000;

    /**
     * Result object returned from a suburb search.
     * Contains the display name, bounding box, centroid, and GeoJSON polygon string.
     */
    public static class SuburbResult {
        /** Human-readable name of the found location. */
        public final String displayName;
        /** Centre latitude of the suburb. */
        public final double lat;
        /** Centre longitude of the suburb. */
        public final double lon;
        /** Bounding box: [minLat, maxLat, minLon, maxLon] */
        public final double[] boundingBox;
        /** GeoJSON geometry string for the suburb boundary polygon, or null if unavailable. */
        public final String geoJson;

        /**
         * Constructs a suburb search result.
         * @param displayName the full display name from Nominatim
         * @param lat the centre latitude
         * @param lon the centre longitude
         * @param boundingBox [minLat, maxLat, minLon, maxLon]
         * @param geoJson GeoJSON geometry string, or null if not available
         */
        public SuburbResult(String displayName, double lat, double lon,
                            double[] boundingBox, String geoJson) {
            this.displayName = displayName;
            this.lat = lat;
            this.lon = lon;
            this.boundingBox = boundingBox;
            this.geoJson = geoJson;
        }
    }

    /**
     * Searches for a suburb by name and returns its boundary data.
     *
     * Calls Nominatim with polygon_geojson=1 so the full boundary polygon is
     * returned in the same request, avoiding a separate Overpass API call.
     * Results are biased toward Queensland, Australia.
     *
     * @param query the suburb or area name entered by the user
     * @return a SuburbResult with location and boundary data, or null if not found
     */
    public SuburbResult search(String query) {
        try {
            // Append Queensland Australia to bias results toward the right region
            String biasedQuery = query.trim() + ", Queensland, Australia";
            String encoded = URLEncoder.encode(biasedQuery, StandardCharsets.UTF_8);

            // polygon_geojson=1 returns the full boundary polygon in the response
            String url = NOMINATIM_BASE + "/search?q=" + encoded
                    + "&format=json&limit=1&polygon_geojson=1&addressdetails=1";

            String response = httpGet(url);
            if (response == null) return null;

            JSONArray results = new JSONArray(response);
            if (results.isEmpty()) return null;

            JSONObject obj = results.getJSONObject(0);

            double lat = Double.parseDouble(obj.getString("lat"));
            double lon = Double.parseDouble(obj.getString("lon"));
            String displayName = obj.getString("display_name");

            // Nominatim bounding box is [south, north, west, east]
            JSONArray bb = obj.getJSONArray("boundingbox");
            double[] boundingBox = new double[]{
                    Double.parseDouble(bb.getString(0)), // minLat (south)
                    Double.parseDouble(bb.getString(1)), // maxLat (north)
                    Double.parseDouble(bb.getString(2)), // minLon (west)
                    Double.parseDouble(bb.getString(3))  // maxLon (east)
            };

            // Extract GeoJSON polygon if present
            String geoJson = null;
            if (obj.has("geojson")) {
                geoJson = obj.getJSONObject("geojson").toString();
            }

            return new SuburbResult(displayName, lat, lon, boundingBox, geoJson);

        } catch (Exception e) {
            LOG.warning("Suburb search failed for '" + query + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Checks whether a lat/lon coordinate falls within the given bounding box.
     * Used to filter crime records to only those inside the searched suburb.
     *
     * @param lat the latitude to test
     * @param lon the longitude to test
     * @param boundingBox [minLat, maxLat, minLon, maxLon]
     * @return true if the coordinate is within the bounding box
     */
    public static boolean isInBoundingBox(double lat, double lon, double[] boundingBox) {
        return lat >= boundingBox[0] && lat <= boundingBox[1]
                && lon >= boundingBox[2] && lon <= boundingBox[3];
    }

    /**
     * Performs a GET request and returns the response body as a string.
     *
     * @param url the full URL to fetch
     * @return the response body string, or null if the request fails
     */
    private String httpGet(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);

            if (conn.getResponseCode() != 200) return null;

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
            );
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            return sb.toString();

        } catch (Exception e) {
            LOG.warning("HTTP GET failed for " + url + ": " + e.getMessage());
            return null;
        }
    }
}
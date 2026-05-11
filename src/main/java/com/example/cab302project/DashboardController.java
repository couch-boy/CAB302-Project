package com.example.cab302project;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the public dashboard screen (dashboard-view.fxml).
 *
 * Displays the hotspot map as the home screen for public users.
 * Manages the hamburger menu, hotspot clustering, suburb search,
 * category filtering, and map loading.
 */
public class DashboardController {

    // FXML UI elements
    @FXML private WebView mapView;
    @FXML private Button hamburgerBtn;
    @FXML private StackPane dashboardRoot;
    @FXML private NavBarController navBarController;

    // Search bar and category filter injected from FXML
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private Label searchStatusLabel;

    private IAppDAO dao;
    private HamburgerMenu hamburgerMenu;
    private WebEngine engine;

    // Tracks the active suburb bounding box — null means show all crimes
    private double[] activeBoundingBox = null;

    // Constructor
    /**
     * Initialises the controller and retrieves the shared DAO instance.
     */
    public DashboardController() {
        //get main application dao instance
        this.dao = HelloApplication.DATABASE;
    }

    /**
     * Clusters nearby crimes into hotspots based on a radius, averaging their coordinates.
     * @param crimes list of all crime records to cluster
     * @param radiusKm the radius in kilometres to group crimes together
     * @return a list of Hotspot objects representing each cluster
     */
    private List<Hotspot> buildHotspots(List<CrimeRecord> crimes, double radiusKm) {
        List<Hotspot> hotspots = new ArrayList<>();
        boolean[] used = new boolean[crimes.size()];

        for (int i = 0; i < crimes.size(); i++) {
            if (used[i]) continue;

            CrimeRecord base = crimes.get(i);

            double sumLat = base.getLatitude();
            double sumLon = base.getLongitude();
            int count = 1;
            used[i] = true;

            for (int j = i + 1; j < crimes.size(); j++) {
                if (used[j]) continue;

                CrimeRecord other = crimes.get(j);
                double distance = distanceKm(
                        base.getLatitude(), base.getLongitude(),
                        other.getLatitude(), other.getLongitude()
                );

                if (distance <= radiusKm) {
                    sumLat += other.getLatitude();
                    sumLon += other.getLongitude();
                    count++;
                    used[j] = true;
                }
            }

            hotspots.add(new Hotspot(sumLat / count, sumLon / count, count));
        }

        return hotspots;
    }

    /**
     * Calculates the distance in kilometres between two lat/lon coordinates using the Haversine formula.
     * @return distance in kilometres between the two points
     */
    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    /**
     * Converts a list of Hotspot objects into a JSON string for passing to the hotspots map.
     * @param hotspots list of hotspots to serialise
     * @return a JSON array string of hotspot objects with lat, lon and count fields
     */
    private String buildHotspotJson(List<Hotspot> hotspots) {
        StringBuilder sb = new StringBuilder("[");

        for (int i = 0; i < hotspots.size(); i++) {
            Hotspot h = hotspots.get(i);

            sb.append("{")
                    .append("\"lat\":").append(h.getLatitude()).append(",")
                    .append("\"lon\":").append(h.getLongitude()).append(",")
                    .append("\"count\":").append(h.getCount())
                    .append("}");

            if (i < hotspots.size() - 1) {
                sb.append(",");
            }
        }

        sb.append("]");
        return sb.toString();
    }

    /**
     * Filters the crime list by the currently selected category and the active
     * bounding box (suburb), then rebuilds and pushes hotspots to the map.
     * Called on initial load and whenever the search or category filter changes.
     */
    private void applyFiltersAndRefreshMap() {
        List<CrimeRecord> allCrimes = dao.getAllCrimes();

        // Apply category filter if one is selected
        String selectedCategory = categoryFilter != null ? categoryFilter.getValue() : "All Categories";
        List<CrimeRecord> filtered = new ArrayList<>();

        for (CrimeRecord crime : allCrimes) {
            // Category filter: skip if a specific category is selected and this crime doesn't match
            if (selectedCategory != null && !selectedCategory.equals("All Categories")) {
                if (!crime.getCategory().getName().equals(selectedCategory)) continue;
            }

            // Bounding box filter: skip if a suburb is active and this crime is outside it
            if (activeBoundingBox != null) {
                if (!SuburbSearchService.isInBoundingBox(
                        crime.getLatitude(), crime.getLongitude(), activeBoundingBox)) {
                    continue;
                }
            }

            filtered.add(crime);
        }

        List<Hotspot> hotspots = buildHotspots(filtered, 2.0);
        String json = buildHotspotJson(hotspots);
        final String safeJson = json.replace("\\", "\\\\").replace("'", "\\'");

        // Update status label to show how many crimes are showing
        if (searchStatusLabel != null) {
            String status = filtered.isEmpty()
                    ? "No crimes found in this area"
                    : filtered.size() + " crime" + (filtered.size() == 1 ? "" : "s") + " in view";
            Platform.runLater(() -> searchStatusLabel.setText(status));
        }

        Platform.runLater(() -> {
            try {
                engine.executeScript("loadHotspots('" + safeJson + "')");
            } catch (Exception e) {
                System.out.println("JS execution failed: " + e.getMessage());
            }
        });
    }

    /**
     * Handles the suburb search when the user presses enter in the search field.
     * Runs the Nominatim request on a background thread to avoid blocking the UI,
     * then draws the suburb boundary on the map and filters crimes to that area.
     */
    @FXML
    public void onSearch() {
        String query = searchField.getText().trim();

        if (query.isEmpty()) {
            // Clear search — remove boundary and show all crimes
            activeBoundingBox = null;
            if (searchStatusLabel != null) searchStatusLabel.setText("");
            try {
                engine.executeScript("clearSuburbBoundary()");
            } catch (Exception ignored) {}
            applyFiltersAndRefreshMap();
            return;
        }

        if (searchStatusLabel != null) searchStatusLabel.setText("Searching...");

        // Run Nominatim on a background thread so the UI stays responsive
        Thread searchThread = new Thread(() -> {
            SuburbSearchService service = new SuburbSearchService();
            SuburbSearchService.SuburbResult result = service.search(query);

            Platform.runLater(() -> {
                if (result == null) {
                    if (searchStatusLabel != null) {
                        searchStatusLabel.setText("Suburb not found. Try a different name.");
                    }
                    return;
                }

                // Store bounding box for crime filtering
                activeBoundingBox = result.boundingBox;

                // Fly the map to the suburb and draw its boundary polygon
                try {
                    String name = result.displayName
                            .replace("\\", "\\\\").replace("'", "\\'");

                    if (result.geoJson != null) {
                        // Pass the GeoJSON polygon to Leaflet to draw the boundary
                        String safeGeoJson = result.geoJson
                                .replace("\\", "\\\\").replace("'", "\\'");
                        engine.executeScript(
                                "showSuburbBoundary('" + safeGeoJson + "','" + name + "',"
                                        + result.lat + "," + result.lon + ")"
                        );
                    } else {
                        // No polygon — just fly to the bounding box centre
                        engine.executeScript(
                                "flyToSuburb(" + result.lat + "," + result.lon + ",13)"
                        );
                    }
                } catch (Exception e) {
                    System.out.println("Map update failed: " + e.getMessage());
                }

                // Refilter crimes using new bounding box
                applyFiltersAndRefreshMap();
            });
        });

        searchThread.setDaemon(true);
        searchThread.start();
    }

    /**
     * Handles category filter change. Immediately refilters the map without
     * doing another suburb search — the existing bounding box is preserved.
     */
    @FXML
    public void onCategoryChanged() {
        if (engine != null) {
            applyFiltersAndRefreshMap();
        }
    }

    /**
     * Clears the active suburb search, removes the boundary polygon from the map,
     * and resets the map to show all crimes across Brisbane.
     */
    @FXML
    public void onClearSearch() {
        activeBoundingBox = null;
        if (searchField != null) searchField.clear();
        if (searchStatusLabel != null) searchStatusLabel.setText("");
        try {
            engine.executeScript("clearSuburbBoundary()");
        } catch (Exception ignored) {}
        applyFiltersAndRefreshMap();
    }

    /**
     * Loads the map view and, once loaded, retrieves crime data,
     * generates hotspots, and injects them into the map for display.
     * Uses LeafletLoader to inject Leaflet from a bundled classpath resource
     * and routes tile requests through TileProxyServer for cross-platform compatibility.
     */
    private void loadMap() {
        if (mapView == null) {
            System.out.println("mapView is null");
            return;
        }

        engine = mapView.getEngine();

        LeafletLoader.loadMap(mapView, "hotspots-map.html", () -> {
            applyFiltersAndRefreshMap();
        });
    }

    /**
     * Populates the category filter ComboBox with all available crime category names,
     * plus an "All Categories" default option at the top.
     */
    private void setupCategoryFilter() {
        if (categoryFilter == null) return;

        List<String> options = new ArrayList<>();
        options.add("All Categories");
        for (CrimeCategory cat : CrimeCategory.values()) {
            options.add(cat.getName());
        }
        categoryFilter.setItems(FXCollections.observableArrayList(options));
        categoryFilter.setValue("All Categories");
    }

    /**
     * This method runs automatically after the FXML has loaded
     */
    @FXML
    public void initialize() {
        setupCategoryFilter();

        Platform.runLater(this::loadMap);

        // Mark Map tab as active in bottom nav
        if (navBarController != null) {
            navBarController.setActiveTab("map");
        }

        // Wire hamburger menu after scene is attached
        // Platform.runLater ensures getScene().getWindow() is not null
        Platform.runLater(() -> {
            Stage stage = (Stage) hamburgerBtn.getScene().getWindow();
            hamburgerMenu = new HamburgerMenu(stage);
            hamburgerMenu.setMaxWidth(Double.MAX_VALUE);
            hamburgerMenu.setMaxHeight(Double.MAX_VALUE);
            dashboardRoot.getChildren().add(hamburgerMenu);
            hamburgerBtn.setOnAction(e -> hamburgerMenu.toggle());
        });
    }

    /**
     * Return to login screen and logout current UserSession
     */
    @FXML
    public void onLogout() {
        UserSession.logout();

        //get the current stage (window) by referencing a ui element
        Stage stage = (Stage) hamburgerBtn.getScene().getWindow();
        //load login view
        UIUtils.switchScene(stage, "login-view.fxml");
    }

    /**
     * Go to crimes view
     */
    @FXML
    public void viewCrimes() {
        //get the current stage (window) by referencing a ui element
        Stage stage = (Stage) hamburgerBtn.getScene().getWindow();
        //load crimes view
        UIUtils.switchScene(stage, "crimes-view.fxml");
    }

    /**
     * Go to profile view
     */
    @FXML
    public void viewProfile() {
        Stage stage = (Stage) hamburgerBtn.getScene().getWindow();
        UIUtils.switchScene(stage, "profile-view.fxml");
    }

    /**
     * Navigates to the hotspots view scene.
     */
    @FXML
    public void viewHotspots() {
        Stage stage = (Stage) hamburgerBtn.getScene().getWindow();
        UIUtils.switchScene(stage, "hotspots-view.fxml");
    }

    /**
     * Navigates to the crimes screen so the user can submit a new report.
     */
    @FXML
    public void onSubmitReport() {
        Stage stage = (Stage) hamburgerBtn.getScene().getWindow();
        UIUtils.switchScene(stage, "crimes-view.fxml");
    }
}
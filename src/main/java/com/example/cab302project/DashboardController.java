package com.example.cab302project;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import javafx.scene.input.MouseEvent;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the public dashboard screen (dashboard-view.fxml).
 *
 * Displays the hotspot map as the home screen for public users.
 * Manages the hamburger menu, hotspot clustering, suburb search,
 * and the filter drawer (crime type, time range, actioned status).
 */
public class DashboardController {

    // FXML UI elements
    @FXML private WebView mapView;
    @FXML private Button hamburgerBtn;
    @FXML private StackPane dashboardRoot;
    @FXML private NavBarController navBarController;

    // Floating search bar elements
    @FXML private TextField searchField;
    @FXML private Label searchStatusLabel;
    @FXML private VBox filterDrawer;

    // Filter drawer dropdowns
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ComboBox<String> daysFilter;
    @FXML private ComboBox<String> actionedFilter;
    @FXML private StackPane filterBackdrop;


    private IAppDAO dao;
    private HamburgerMenu hamburgerMenu;
    private WebEngine engine;

    // Whether the filter drawer is currently visible
    private boolean filterDrawerOpen = false;

    // Active suburb bounding box — null means show all crimes
    private double[] activeBoundingBox = null;

    // Active suburb GeoJSON polygon string — used for precise point-in-polygon filtering in JS
    private String activeGeoJson = null;

    // Constructor
    /**
     * Initialises the controller and retrieves the shared DAO instance.
     */
    public DashboardController() {
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
            if (i < hotspots.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Applies all active filters (suburb bounding box, crime category, time range,
     * actioned status) to the full crime list, clusters the results into hotspots,
     * and pushes the updated JSON to the map.
     */
    private void applyFiltersAndRefreshMap() {
        List<CrimeRecord> allCrimes = dao.getAllCrimes();

        String selectedCategory = categoryFilter != null ? categoryFilter.getValue() : "All Types";
        String selectedDays     = daysFilter     != null ? daysFilter.getValue()     : "All time";
        String selectedActioned = actionedFilter != null ? actionedFilter.getValue() : "All";

        // Resolve the days filter to a cutoff date — null means no cutoff
        LocalDateTime cutoff = resolveDaysCutoff(selectedDays);

        List<CrimeRecord> filtered = new ArrayList<>();

        for (CrimeRecord crime : allCrimes) {

            // Suburb bounding box filter
            if (activeBoundingBox != null) {
                if (!SuburbSearchService.isInBoundingBox(
                        crime.getLatitude(), crime.getLongitude(), activeBoundingBox)) continue;
            }

            // Crime category filter
            if (selectedCategory != null && !selectedCategory.equals("All Types")) {
                if (!crime.getCategory().getName().equals(selectedCategory)) continue;
            }

            // Time range filter — compare crime timestamp against the cutoff
            if (cutoff != null && crime.getTimestamp() != null) {
                if (crime.getTimestamp().isBefore(cutoff)) continue;
            }

            // Actioned status filter
            if (selectedActioned != null) {
                if (selectedActioned.equals("Actioned") && !crime.isActioned()) continue;
                if (selectedActioned.equals("Pending")  &&  crime.isActioned()) continue;
            }

            filtered.add(crime);
        }

        List<Hotspot> hotspots = buildHotspots(filtered, 0.5);
        String json = buildHotspotJson(hotspots);
        final String safeJson = json.replace("\\", "\\\\").replace("'", "\\'");

        // Update status label — only show it when filters or suburb are active
        if (searchStatusLabel != null) {
            String status = activeBoundingBox == null && filtered.size() == allCrimes.size()
                    ? ""
                    : filtered.isEmpty()
                      ? "No crimes match these filters"
                      : filtered.size() + " crime" + (filtered.size() == 1 ? "" : "s") + " in view";
            Platform.runLater(() -> searchStatusLabel.setText(status));
        }

        Platform.runLater(() -> {
            try {
                String safeGeoJsonFilter = (activeGeoJson != null)
                        ? activeGeoJson.replace("\\", "\\\\").replace("'", "\\'") : "";
                engine.executeScript("loadHotspots('" + safeJson + "','" + safeGeoJsonFilter + "')");
            } catch (Exception e) {
                System.out.println("JS execution failed: " + e.getMessage());
            }
        });
    }

    /**
     * Converts the days-filter label into a LocalDateTime cutoff.
     * Returns null when "All time" is selected (no cutoff applied).
     * @param label the display string from the days filter dropdown
     * @return the earliest allowed crime timestamp, or null for no restriction
     */
    private LocalDateTime resolveDaysCutoff(String label) {
        if (label == null || label.equals("All time")) return null;
        LocalDateTime now = LocalDateTime.now();
        return switch (label) {
            case "Last 24 hours" -> now.minusDays(1);
            case "Last 7 days"   -> now.minusDays(7);
            case "Last 30 days"  -> now.minusDays(30);
            case "Last 90 days"  -> now.minusDays(90);
            case "Last year"     -> now.minusYears(1);
            default              -> null;
        };
    }

    /**
     * Handles the suburb search when the user presses enter or the Search button.
     * Runs the Nominatim request on a background thread to keep the UI responsive,
     * then draws the suburb boundary on the map and filters crimes to that area.
     */
    @FXML
    public void onSearch() {
        String query = searchField.getText().trim();

        if (query.isEmpty()) {
            onClearSearch();
            return;
        }

        if (searchStatusLabel != null) searchStatusLabel.setText("Searching...");

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

                activeBoundingBox = result.boundingBox;
                activeGeoJson = result.geoJson;

                try {
                    String name = result.displayName
                            .replace("\\", "\\\\").replace("'", "\\'");

                    if (result.geoJson != null) {
                        String safeGeoJson = result.geoJson
                                .replace("\\", "\\\\").replace("'", "\\'");
                        engine.executeScript(
                                "showSuburbBoundary('" + safeGeoJson + "','" + name + "',"
                                        + result.lat + "," + result.lon + ")"
                        );
                    } else {
                        engine.executeScript(
                                "flyToSuburb(" + result.lat + "," + result.lon + ",13)"
                        );
                    }
                } catch (Exception e) {
                    System.out.println("Map update failed: " + e.getMessage());
                }

                applyFiltersAndRefreshMap();
            });
        });

        searchThread.setDaemon(true);
        searchThread.start();
    }

    /**
     * Clears the suburb search, removes the boundary from the map, and resets to all crimes.
     */
    @FXML
    public void onClearSearch() {
        activeBoundingBox = null;
        activeGeoJson = null;
        if (searchField != null) searchField.clear();
        if (searchStatusLabel != null) searchStatusLabel.setText("");
        try {
            engine.executeScript("clearSuburbBoundary()");
        } catch (Exception ignored) {}
        applyFiltersAndRefreshMap();
    }

    /**
     * Toggles the filter drawer open and closed.
     * Also shows or hides the transparent backdrop that catches outside clicks.
     */
    @FXML
    public void onToggleFilter() {
        filterDrawerOpen = !filterDrawerOpen;
        if (filterBackdrop != null) {
            filterBackdrop.setVisible(filterDrawerOpen);
            filterBackdrop.setManaged(filterDrawerOpen);
        }
    }

    /**
     * Called when the user clicks anywhere on the transparent backdrop behind the filter drawer.
     * Closes the filter drawer.
     */
    @FXML
    public void onBackdropClicked() {
        if (filterDrawerOpen) onToggleFilter();
    }

    /**
     * Consumes mouse clicks on the filter drawer itself so they do not
     * propagate to the backdrop and accidentally close the panel.
     */
    @FXML
    public void onFilterDrawerClicked(MouseEvent event) {
        event.consume();
    }

    /**
     * Called when the crime category dropdown changes.
     */
    @FXML
    public void onCategoryChanged() {
        if (engine != null) applyFiltersAndRefreshMap();
    }

    /**
     * Called when the days filter dropdown changes.
     */
    @FXML
    public void onDaysChanged() {
        if (engine != null) applyFiltersAndRefreshMap();
    }

    /**
     * Called when the actioned status dropdown changes.
     */
    @FXML
    public void onActionedChanged() {
        if (engine != null) applyFiltersAndRefreshMap();
    }

    /**
     * Resets all filter dropdowns to their defaults and refreshes the map.
     */
    @FXML
    public void onResetFilters() {
        if (categoryFilter != null) categoryFilter.setValue("All Types");
        if (daysFilter     != null) daysFilter.setValue("All time");
        if (actionedFilter != null) actionedFilter.setValue("All");
        if (engine != null) applyFiltersAndRefreshMap();
    }

    /**
     * Loads the map via LeafletLoader and pushes the initial hotspot data.
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
     * Populates all filter ComboBoxes with their option lists.
     */
    private void setupFilters() {
        // Crime category
        if (categoryFilter != null) {
            List<String> categories = new ArrayList<>();
            categories.add("All Types");
            for (CrimeCategory cat : CrimeCategory.values()) {
                categories.add(cat.getName());
            }
            categoryFilter.setItems(FXCollections.observableArrayList(categories));
            categoryFilter.setValue("All Types");
        }

        // Time range
        if (daysFilter != null) {
            daysFilter.setItems(FXCollections.observableArrayList(
                    "All time", "Last 24 hours", "Last 7 days",
                    "Last 30 days", "Last 90 days", "Last year"
            ));
            daysFilter.setValue("All time");
        }

        // Actioned status
        if (actionedFilter != null) {
            actionedFilter.setItems(FXCollections.observableArrayList(
                    "All", "Actioned", "Pending"
            ));
            actionedFilter.setValue("All");
        }
    }

    /**
     * Runs automatically after the FXML has loaded.
     */
    @FXML
    public void initialize() {
        setupFilters();

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
        Stage stage = (Stage) hamburgerBtn.getScene().getWindow();
        UIUtils.switchScene(stage, "login-view.fxml");
    }

    /**
     * Go to crimes view
     */
    @FXML
    public void viewCrimes() {
        Stage stage = (Stage) hamburgerBtn.getScene().getWindow();
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
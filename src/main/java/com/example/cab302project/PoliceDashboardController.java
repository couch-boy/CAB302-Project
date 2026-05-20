package com.example.cab302project;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseEvent;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the police dashboard screen (police-dashboard-view.fxml).
 *
 * Displays the hotspot map as the home screen for police users.
 * Manages the police hamburger menu, hotspot clustering, suburb search,
 * and the filter drawer (crime type, time range, actioned status).
 */
public class PoliceDashboardController {

    // FXML UI elements
    @FXML private WebView mapView;
    @FXML private Button hamburgerBtn;
    @FXML private StackPane dashboardRoot;
    @FXML private NavBarController navBarController;

    // Floating search bar elements
    @FXML private TextField searchField;
    @FXML private VBox filterDrawer;
    @FXML private VBox aiSummaryPopup;
    @FXML private Label aiSummaryLabel;
    @FXML private ScrollPane aiSummaryScrollPane;

    // Filter drawer dropdowns
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ComboBox<String> daysFilter;
    @FXML private ComboBox<String> actionedFilter;
    @FXML private StackPane filterBackdrop;

    private IAppDAO dao;
    private final OllamaService ollamaService = new OllamaService();
    private PoliceHamburgerMenu hamburgerMenu;
    private WebEngine engine;

    // Whether the filter drawer is currently visible
    private boolean filterDrawerOpen = false;

    // Active suburb bounding box — null means show all crimes
    private double[] activeBoundingBox = null;

    // Active suburb GeoJSON polygon string — used for precise point-in-polygon filtering in JS
    private String activeGeoJson = null;

    /**
     * Initialises the controller and retrieves the shared DAO instance.
     */
    public PoliceDashboardController() {
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
     * @param lat1 latitude of the first point
     * @param lon1 longitude of the first point
     * @param lat2 latitude of the second point
     * @param lon2 longitude of the second point
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
     * Applies all active filters (suburb bounding box, crime category, time range,
     * actioned status) to the full crime list, clusters the results into hotspots,
     * and pushes the updated JSON to the map.
     */
    private List<CrimeRecord> applyFiltersAndRefreshMap() {
        List<CrimeRecord> filtered = getFilteredCrimeRecords();

        List<Hotspot> hotspots = buildHotspots(filtered, 0.5);
        String json = buildHotspotJson(hotspots);
        final String safeJson = json.replace("\\", "\\\\").replace("'", "\\'");

        Platform.runLater(() -> {
            try {
                String safeGeoJsonFilter = (activeGeoJson != null)
                        ? activeGeoJson.replace("\\", "\\\\").replace("'", "\\'") : "";
                engine.executeScript("loadHotspots('" + safeJson + "','" + safeGeoJsonFilter + "')");
            } catch (Exception e) {
                System.out.println("JS execution failed: " + e.getMessage());
            }
        });

        return filtered;
    }

    /**
     * Returns the same filtered crime records that are displayed on the map.
     */
    private List<CrimeRecord> getFilteredCrimeRecords() {
        List<CrimeRecord> allCrimes = dao.getAllCrimes();

        String selectedCategory = categoryFilter != null ? categoryFilter.getValue() : "All Types";
        String selectedDays     = daysFilter     != null ? daysFilter.getValue()     : "All time";
        String selectedActioned = actionedFilter != null ? actionedFilter.getValue() : "All";

        // Resolve the days filter to a cutoff date; null means no cutoff.
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

            // Time range filter; compare crime timestamp against the cutoff.
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

        return filtered;
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

        Thread searchThread = new Thread(() -> {
            SuburbSearchService service = new SuburbSearchService();
            SuburbSearchService.SuburbResult result = service.search(query);

            Platform.runLater(() -> {
                if (result == null) {
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

                List<CrimeRecord> displayedCrimes = applyFiltersAndRefreshMap();
                // Trigger the AI summary only after the suburb search has updated the visible map results.
                requestAiCrimeSummary(result.displayName, displayedCrimes);
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
        hideAiSummaryPopup();
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
     * @param event the mouse event to consume
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
     * Starts a background Ollama request after a suburb search has produced visible results.
     * The no-data and Ollama-failure messages are handled here so the popup always gives a clear outcome.
     * Running this on a separate thread keeps the JavaFX map and controls responsive.
     */
    private void requestAiCrimeSummary(String suburbName, List<CrimeRecord> displayedCrimes) {
        if (displayedCrimes == null || displayedCrimes.isEmpty()) {
            showAiSummaryPopup("No crime data available to summarise for this suburb.");
            return;
        }

        // Give the officer immediate feedback while the local Ollama request is running.
        showAiSummaryPopup("Generating AI summary...");

        Thread summaryThread = new Thread(() -> {
            try {
                String prompt = buildCrimeSummaryPrompt(suburbName, displayedCrimes);
                // OllamaService sends the prompt to http://localhost:11434 using the llama3.2 model.
                String summary = ollamaService.generateSummary(prompt);

                if (summary == null || summary.isBlank()) {
                    summary = "AI summary unavailable. Please make sure Ollama is running.";
                }

                String finalSummary = summary;
                Platform.runLater(() -> showAiSummaryPopup(finalSummary));
            } catch (Exception e) {
                Platform.runLater(() -> showAiSummaryPopup(
                        "AI summary unavailable. Please make sure Ollama is running."));
            }
        });

        summaryThread.setDaemon(true);
        summaryThread.start();
    }

    /**
     * Converts the visible crime records into a compact Ollama prompt.
     * CrimeRecord does not currently store a readable address field, so coordinates are not included;
     * the prompt uses the searched suburb plus a general spread description instead.
     */
    private String buildCrimeSummaryPrompt(String suburbName, List<CrimeRecord> crimes) {
        String displaySuburb = getSimpleSuburbName(suburbName);
        StringBuilder records = new StringBuilder();
        int maxRecords = Math.min(crimes.size(), 30);

        for (int i = 0; i < maxRecords; i++) {
            CrimeRecord crime = crimes.get(i);
            records.append("- Type: ").append(crime.getCategory().getName())
                    .append("; severity: ").append(crime.getCategory().getSeverity())
                    .append("; date/time: ").append(UIUtils.formatLocalDateTime(crime.getTimestamp()))
                    .append("; status: ").append(crime.isActioned() ? "Actioned" : "Pending")
                    .append("; location: ").append(formatCrimeLocation(crime, displaySuburb));

            if (crime.getDescription() != null && !crime.getDescription().isBlank()) {
                records.append("; description: ").append(limitText(crime.getDescription(), 120));
            }

            records.append("\n");
        }

        if (crimes.size() > maxRecords) {
            records.append("- ").append(crimes.size() - maxRecords)
                    .append(" additional visible records were omitted to keep the prompt short.\n");
        }

        return "You are assisting a police dashboard. Summarise the displayed records for "
                + displaySuburb + ". Base your answer only on the records below. Start with the wording "
                + "'The displayed records show'. Write exactly 3 short bullet points and keep the total under "
                + "90 words. The bullets must cover: most common crime types shown; any repeated status or "
                + "type pattern if visible; and the general location spread using the suburb/readable location. "
                + "Do not say the suburb has experienced crime in general. Do not call the suburb dangerous. "
                + "Do not make predictions or stereotype the area. Do not mention raw latitude/longitude ranges.\n\n"
                + "Location context: " + buildLocationSpreadDescription(crimes, displaySuburb) + "\n\n"
                + "Visible crime records:\n" + records;
    }

    /**
     * Prefer a readable location if the model gains one later. At present CrimeRecord only has
     * coordinates, so this returns suburb-level wording and keeps raw lat/lon out of the prompt.
     */
    private String formatCrimeLocation(CrimeRecord crime, String suburbName) {
        if (suburbName != null && !suburbName.isBlank()) {
            return "within " + suburbName;
        }
        return "within the searched suburb area";
    }

    private String getSimpleSuburbName(String suburbName) {
        if (suburbName == null || suburbName.isBlank()) {
            return "the searched suburb";
        }
        String[] parts = suburbName.split(",");
        String simpleName = parts.length > 0 ? parts[0].trim() : suburbName.trim();
        return toTitleCase(simpleName);
    }

    private String toTitleCase(String text) {
        String[] words = text.toLowerCase().split("\\s+");
        StringBuilder formatted = new StringBuilder();

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!formatted.isEmpty()) {
                formatted.append(" ");
            }
            formatted.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                formatted.append(word.substring(1));
            }
        }

        return formatted.isEmpty() ? text.trim() : formatted.toString();
    }

    private String buildLocationSpreadDescription(List<CrimeRecord> crimes, String suburbName) {
        if (crimes.size() <= 1) {
            return "the visible incident is within " + suburbName + "; raw coordinates are not provided to the AI.";
        }

        double maxDistanceKm = getMaxDistanceFromFirstCrime(crimes);
        if (maxDistanceKm <= 0.25) {
            return "incidents appear clustered within the searched suburb area.";
        }
        return "incidents are spread across multiple nearby points in " + suburbName + ".";
    }

    private double getMaxDistanceFromFirstCrime(List<CrimeRecord> crimes) {
        CrimeRecord first = crimes.get(0);
        double maxDistance = 0;
        for (CrimeRecord crime : crimes) {
            maxDistance = Math.max(maxDistance, distanceKm(
                    first.getLatitude(), first.getLongitude(),
                    crime.getLatitude(), crime.getLongitude()));
        }
        return maxDistance;
    }

    private String limitText(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Shows the compact bottom popup and scrolls its content back to the top for each new summary.
     */
    private void showAiSummaryPopup(String message) {
        if (aiSummaryPopup == null || aiSummaryLabel == null) {
            return;
        }
        aiSummaryLabel.setText(message);
        if (aiSummaryScrollPane != null) {
            aiSummaryScrollPane.setVvalue(0);
        }
        aiSummaryPopup.setVisible(true);
        aiSummaryPopup.setManaged(true);
    }

    /**
     * Hides the AI summary card when the user closes it or clears the suburb search.
     */
    @FXML
    public void hideAiSummaryPopup() {
        if (aiSummaryPopup == null) {
            return;
        }
        aiSummaryPopup.setVisible(false);
        aiSummaryPopup.setManaged(false);
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
     * Keeps the AI summary as a compact bottom card instead of a full-screen modal.
     */
    private void configureAiSummaryPopupSizing() {
        if (dashboardRoot == null || aiSummaryPopup == null) {
            return;
        }

        dashboardRoot.heightProperty().addListener((obs, oldHeight, newHeight) -> {
            double height = newHeight.doubleValue();
            aiSummaryPopup.setMaxHeight(Math.max(180, height * 0.42));
        });

        dashboardRoot.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            double width = newWidth.doubleValue();
            aiSummaryPopup.setMaxWidth(Math.min(760, Math.max(320, width * 0.88)));
        });
    }
    /**
     * Runs automatically after the FXML has loaded.
     */
    @FXML
    public void initialize() {
        setupFilters();

        configureAiSummaryPopupSizing();

        Platform.runLater(this::loadMap);

        // Mark Map tab as active in bottom nav
        if (navBarController != null) {
            navBarController.setActiveTab("map");
        }

        // Wire police hamburger menu after scene is attached
        // Platform.runLater ensures getScene().getWindow() is not null
        Platform.runLater(() -> {
            Stage stage = (Stage) hamburgerBtn.getScene().getWindow();
            hamburgerMenu = new PoliceHamburgerMenu(stage);
            hamburgerMenu.setMaxWidth(Double.MAX_VALUE);
            hamburgerMenu.setMaxHeight(Double.MAX_VALUE);
            dashboardRoot.getChildren().add(hamburgerMenu);
            hamburgerBtn.setOnAction(e -> hamburgerMenu.toggle());
        });
    }

    /**
     * Returns to the login screen and logs out the current UserSession.
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
     * Navigates to the police crimes view.
     */
    @FXML
    public void viewCrimes() {
        //get the current stage (window) by referencing a ui element
        Stage stage = (Stage) hamburgerBtn.getScene().getWindow();
        //load police crimes view
        UIUtils.switchScene(stage, "Police-crimes-view.fxml");
    }

    /**
     * Navigates to the profile view.
     */
    @FXML
    public void viewProfile() {
        Stage stage = (Stage) hamburgerBtn.getScene().getWindow();
        UIUtils.switchScene(stage, "profile-view.fxml");
    }

    /**
     * Navigates to the hotspots view screen.
     */
    @FXML
    public void viewHotspots() {
        Stage stage = (Stage) hamburgerBtn.getScene().getWindow();
        UIUtils.switchScene(stage, "hotspots-view.fxml");
    }
}

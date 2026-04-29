package com.example.cab302project;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the public dashboard screen (dashboard-view.fxml).
 *
 * Displays the hotspot map as the home screen for public users.
 * Manages the hamburger menu, hotspot clustering and map loading.
 */
public class DashboardController {

    // FXML UI elements
    @FXML
    private WebView mapView;
    @FXML
    private Button hamburgerBtn;
    @FXML
    private StackPane dashboardRoot;
    @FXML
    private NavBarController navBarController;

    private IAppDAO dao;
    private HamburgerMenu hamburgerMenu;

    // Constructor
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
     * Loads the map view and, once loaded, retrieves crime data,
     * generates hotspots, and injects them into the map for display.
     */
    private void loadMap() {
        if (mapView == null) {
            System.out.println("mapView is null");
            return;
        }

        WebEngine engine = mapView.getEngine();

        var resource = getClass().getResource("/com/example/cab302project/hotspots-map.html");
        if (resource == null) {
            System.out.println("hotspots-map.html not found");
            return;
        }

        String url = resource.toExternalForm();

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                List<CrimeRecord> crimes = dao.getAllCrimes();
                List<Hotspot> hotspots = buildHotspots(crimes, 2.0);

                String json = buildHotspotJson(hotspots);
                final String safeJson = json.replace("\\", "\\\\").replace("'", "\\'");

                Platform.runLater(() -> {
                    try {
                        engine.executeScript("loadHotspots('" + safeJson + "')");
                    } catch (Exception e) {
                        System.out.println("JS execution failed: " + e.getMessage());
                    }
                });
            }
        });

        engine.load(url);
    }

    /**
     * This method runs automatically after the FXML has loaded
     */
    @FXML
    public void initialize() {
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
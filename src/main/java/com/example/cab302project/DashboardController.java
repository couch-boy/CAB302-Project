package com.example.cab302project;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import java.util.List;

public class DashboardController {

    // FXML UI elements
    @FXML
    private Label welcomeLabel;
    @FXML
    private Label emailLabel;

    @FXML
    private WebView mapView;

    private IAppDAO dao;

    // Constructor
    public DashboardController() {
        //get main application dao instance
        this.dao = HelloApplication.DATABASE;

    }
    private String buildCrimeJson(List<CrimeRecord> crimes) {
        StringBuilder sb = new StringBuilder("[");

        for (int i = 0; i < crimes.size(); i++) {
            CrimeRecord c = crimes.get(i);

            sb.append("{")
                    .append("\"lat\":").append(c.getLatitude()).append(",")
                    .append("\"lon\":").append(c.getLongitude()).append(",")
                    .append("\"severity\":\"").append(c.getCategory().getSeverity().toString()).append("\"")
                    .append("}");

            if (i < crimes.size() - 1) {
                sb.append(",");
            }
        }

        sb.append("]");
        return sb.toString();
    }
    private void loadMap() {
        if (mapView == null) {
            System.out.println("mapView is null");
            return;
        }

        WebEngine engine = mapView.getEngine();

        var resource = getClass().getResource("/com/example/cab302project/crime-map.html");
        if (resource == null) {
            System.out.println("crime-map.html not found");
            return;
        }

        String url = resource.toExternalForm();

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                List<CrimeRecord> crimes = dao.getAllCrimes();
                final String json = buildCrimeJson(crimes)
                        .replace("\\", "\\\\")
                        .replace("'", "\\'");
                Platform.runLater(() -> {
                    try {
                        engine.executeScript("loadCrimeMarkers('" + json + "')");
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
        //this method auto-runs when dashboard-view.fxml loads
        UserSession session = UserSession.getInstance();

        if (session != null) {
            welcomeLabel.setText("Welcome back, " + session.getUser().getUsername() + "!");
            emailLabel.setText("Your email is: " + session.getUser().getEmail());
        }
        Platform.runLater(this::loadMap);
    }

    /**
     * Return to login screen and logout current UserSession
     */
    @FXML
    public void onLogout() {

        UserSession.logout();

        //get the current stage (window) by referencing a ui element
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        //load login view
        UIUtils.switchScene(stage, "login-view.fxml");

    }

    /**
     * Go to crimes view
     */
    @FXML
    public void viewCrimes() {

        //get the current stage (window) by referencing a ui element
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        //load crimes view
        UIUtils.switchScene(stage, "crimes-view.fxml");

    }

    /**
     * Go to profile view
     */
    @FXML
    public void viewProfile() {
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        UIUtils.switchScene(stage, "profile-view.fxml");
    }
    @FXML
    public void viewHotspots() {
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        UIUtils.switchScene(stage, "hotspots-view.fxml");
    }


}

package com.example.cab302project;

import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class HotspotsController {

    @FXML
    private WebView mapView;

    @FXML
    private NavBarController navBarController;

    private IAppDAO dao;

    public HotspotsController() {
        this.dao = HelloApplication.DATABASE;
    }

    @FXML
    public void initialize() {
        loadHotspotMap();
        if (navBarController != null) {
            navBarController.setActiveTab("map");
        }
    }

    @FXML
    public void onBackButtonClick() {
        Stage stage = (Stage) mapView.getScene().getWindow();
        UIUtils.switchScene(stage, "dashboard-view.fxml");
    }

    private void loadHotspotMap() {
        WebEngine engine = mapView.getEngine();

        var resource = getClass().getResource("/com/example/cab302project/hotspots-map.html");
        if (resource == null) {
            System.out.println("hotspots-map.html not found");
            return;
        }

        String url = resource.toExternalForm();

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                List<CrimeRecord> crimes = dao.getAllCrimes();
                List<Hotspot> hotspots = buildHotspots(crimes, 2.0);

                String json = buildHotspotJson(hotspots);
                json = json.replace("\\", "\\\\").replace("'", "\\'");

                engine.executeScript("loadHotspots('" + json + "')");
            }
        });

        engine.load(url);
    }

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
}
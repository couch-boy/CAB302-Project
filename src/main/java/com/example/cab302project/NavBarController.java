package com.example.cab302project;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.stage.Stage;

/**
 * Controller for the shared bottom navigation bar (nav-bar.fxml).
 *
 * Usage: call setActiveTab("map" | "crimes" | "profile") from the
 * host screen's initialize() to highlight the current tab.
 */
public class NavBarController {

    @FXML private Button btnMap;
    @FXML private Button btnCrimes;
    @FXML private Button btnProfile;

    @FXML private Label iconMap;
    @FXML private Label labelMap;
    @FXML private Label iconCrimes;
    @FXML private Label labelCrimes;
    @FXML private Label iconProfile;
    @FXML private Label labelProfile;

    // ── Navigation handlers ──────────────────────────────────────────

    @FXML
    public void onMap() {
        Stage stage = (Stage) btnMap.getScene().getWindow();
        if (UserSession.isPolice()) {
            UIUtils.switchScene(stage, "police-dashboard-view.fxml");
        } else {
            UIUtils.switchScene(stage, "dashboard-view.fxml");
        }
    }

    @FXML
    public void onCrimes() {
        Stage stage = (Stage) btnCrimes.getScene().getWindow();
        if (UserSession.isPolice()) {
            UIUtils.switchScene(stage, "Police-crimes-view.fxml");
        } else {
            UIUtils.switchScene(stage, "crimes-view.fxml");
        }
    }

    @FXML
    public void onProfile() {
        Stage stage = (Stage) btnProfile.getScene().getWindow();
        UIUtils.switchScene(stage, "profile-view.fxml");
    }

    // ── Active tab highlighting ───────────────────────────────────────

    /**
     * Call this from the host screen's initialize() to mark the active tab.
     * @param tab one of "map", "crimes", or "profile"
     */
    public void setActiveTab(String tab) {
        switch (tab) {
            case "map" -> {
                setActive(iconMap, labelMap);
            }
            case "crimes" -> {
                setActive(iconCrimes, labelCrimes);
            }
            case "profile" -> {
                setActive(iconProfile, labelProfile);
            }
        }
    }

    private void setActive(Label icon, Label label) {
        icon.getStyleClass().setAll("nav-btn-icon-active");
        label.getStyleClass().setAll("nav-btn-label-active");
    }
}
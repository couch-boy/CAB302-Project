package com.example.cab302project;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

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

    @FXML private Label labelMap;
    @FXML private Label labelCrimes;
    @FXML private Label labelProfile;

    // VBox wrappers used to look up FontIcon children for active colouring
    @FXML private VBox navMap;
    @FXML private VBox navCrimes;
    @FXML private VBox navProfile;

    // ── Navigation handlers ──────────────────────────────────────────
    /**
     * Navigates to the map/dashboard view based on user role.
     */
    @FXML
    public void onMap() {
        Stage stage = (Stage) btnMap.getScene().getWindow();
        if (UserSession.isPolice()) {
            UIUtils.switchScene(stage, "police-dashboard-view.fxml");
        } else {
            UIUtils.switchScene(stage, "dashboard-view.fxml");
        }
    }

    /**
     * Navigates to the crimes view based on user role.
     */

    @FXML
    public void onCrimes() {
        Stage stage = (Stage) btnCrimes.getScene().getWindow();
        if (UserSession.isPolice()) {
            UIUtils.switchScene(stage, "Police-crimes-view.fxml");
        } else {
            UIUtils.switchScene(stage, "crimes-view.fxml");
        }
    }
    /**
     * Navigates to the profile view.
     */

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
            case "map"     -> setActive(navMap, labelMap);
            case "crimes"  -> setActive(navCrimes, labelCrimes);
            case "profile" -> setActive(navProfile, labelProfile);
        }
    }

    /**
     * Applies active styling to the label and recolours the FontIcon in the given nav VBox.
     * @param navBox the VBox containing the FontIcon and Label for this tab
     * @param label the Label to apply the active style to
     */
    private void setActive(VBox navBox, Label label) {
        label.getStyleClass().setAll("nav-btn-label-active");

        // Find the FontIcon child and apply the active colour
        navBox.getChildren().stream()
                .filter(node -> node instanceof FontIcon)
                .map(node -> (FontIcon) node)
                .findFirst()
                .ifPresent(icon -> icon.setIconColor(
                        javafx.scene.paint.Color.web("#2A364E")));
    }
}
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
 * This controller is embedded in all main screens via {@code <fx:include>}.
 * It provides three navigation buttons — Map, Reports, and Profile — each
 * routing to the appropriate screen based on whether the logged-in user is
 * a regular user or a police officer. Call {@link #setActiveTab(String)} from
 * the host screen's {@code initialize()} method to highlight the current tab.
 */
public class NavBarController {

    @FXML private Button btnMap;
    @FXML private Button btnCrimes;
    @FXML private Button btnProfile;
    @FXML private Label labelMap;
    @FXML private Label labelCrimes;
    @FXML private Label labelProfile;
    @FXML private VBox navMap;
    @FXML private VBox navCrimes;
    @FXML private VBox navProfile;

    // Navigation handlers

    /**
     * Navigates to the map screen.
     * Routes to {@code police-dashboard-view.fxml} for police users,
     * or {@code dashboard-view.fxml} for regular users.
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
     * Navigates to the crimes / reports screen.
     * Routes to {@code Police-crimes-view.fxml} for police users,
     * or {@code crimes-view.fxml} for regular users.
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
     * Navigates to the user profile screen.
     * Both user types share the same profile view.
     */
    @FXML
    public void onProfile() {
        Stage stage = (Stage) btnProfile.getScene().getWindow();
        UIUtils.switchScene(stage, "profile-view.fxml");
    }

    // Active tab highlighting

    /**
     * Marks the specified tab as active by applying the active label style
     * and recolouring its {@link FontIcon} to the primary navy colour.
     *
     * Should be called from the host screen's {@code initialize()} method
     * so the correct tab is highlighted when the screen loads.
     *
     * @param tab the tab to activate; must be one of {@code "map"},
     *            {@code "crimes"}, or {@code "profile"}
     */
    public void setActiveTab(String tab) {
        switch (tab) {
            case "map"     -> setActive(navMap, labelMap);
            case "crimes"  -> setActive(navCrimes, labelCrimes);
            case "profile" -> setActive(navProfile, labelProfile);
        }
    }

    /**
     * Applies active styling to a single navigation tab.
     *
     * Updates the label's CSS style class to the active variant and
     * finds the first {@link FontIcon} child of the given VBox, setting
     * its colour to the primary navy ({@code #2A364E}).
     *
     * @param navBox the VBox containing the {@link FontIcon} and {@link Label} for this tab
     * @param label  the label to apply the active style class to
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
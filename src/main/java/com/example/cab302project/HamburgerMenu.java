package com.example.cab302project;

import javafx.animation.TranslateTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;


/**
 * Side drawer navigation menu for public users (hamburger-menu.fxml).
 *
 * Loaded programmatically and added to the root StackPane of each public screen.
 * Call toggle() from the hamburger button's onAction handler to open or close it.
 */
public class HamburgerMenu extends StackPane {

    private Pane     drawerOverlay;
    private HBox     drawerContainer;
    private Label    drawerUsername;
    private Label    drawerEmail;
    private CheckBox darkModeToggle;

    private boolean  isOpen = false;
    private Stage    stage;
    private IAppDAO  dao;
    private StackPane loaded;

    /**
     * Loads the hamburger menu FXML and attaches it to the scene graph.
     * @param stage the current application stage used for scene navigation
     */
    public HamburgerMenu(Stage stage) {
        this.stage = stage;
        this.dao   = HelloApplication.DATABASE;

        FXMLLoader loader = new FXMLLoader(
                HelloApplication.class.getResource("hamburger-menu.fxml"));

        try {
            loaded = loader.load();
            this.getChildren().add(loaded);
        } catch (IOException e) {
            System.err.println("Failed to load hamburger-menu.fxml: " + e.getMessage());
            e.printStackTrace();
        }

        this.setPickOnBounds(false);

        // Wire nodes once this component is attached to the live scene graph
        this.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && loaded != null && drawerOverlay == null) {
                wireNodes();
            }
        });
    }

    /**
     * Looks up all drawer nodes by fx:id and wires click handlers to each menu button.
     */
    private void wireNodes() {
        drawerOverlay   = (Pane)     loaded.lookup("#drawerOverlay");
        drawerContainer = (HBox)     loaded.lookup("#drawerContainer");
        drawerUsername  = (Label)    loaded.lookup("#drawerUsername");
        drawerEmail     = (Label)    loaded.lookup("#drawerEmail");
        darkModeToggle  = (CheckBox) loaded.lookup("#darkModeToggle");

        if (drawerOverlay == null || drawerContainer == null) {
            System.err.println("HamburgerMenu: drawer nodes not found in FXML.");
            return;
        }

        drawerOverlay.setOnMouseClicked(e -> close());

        Pane catcher = (Pane) loaded.lookup("#transparentCatcher");
        if (catcher != null) catcher.setOnMouseClicked(e -> close());

        loaded.lookup("#btnMap").setOnMouseClicked(e -> onMap());
        loaded.lookup("#btnMyReports").setOnMouseClicked(e -> onMyReports());
        loaded.lookup("#btnNewReport").setOnMouseClicked(e -> onNewReport());
        loaded.lookup("#btnProfile").setOnMouseClicked(e -> onProfile());
        loaded.lookup("#btnSettings").setOnMouseClicked(e -> onSettings());
        loaded.lookup("#btnLogout").setOnMouseClicked(e -> onLogout());
        if (darkModeToggle != null) darkModeToggle.setOnAction(e -> onDarkModeToggle());
    }

    /**
     * Toggles the drawer open or closed depending on its current state.
     */
    public void toggle() {
        if (isOpen) close();
        else        open();
    }

    /**
     * Opens the drawer by sliding it in from the left and showing the dim backdrop.
     * Also populates the username, email and dark mode toggle from the current session.
     */
    public void open() {
        if (drawerOverlay == null || drawerContainer == null) return;

        UserSession session = UserSession.getInstance();
        if (session != null && session.getUser() != null) {
            drawerUsername.setText(session.getUser().getUsername());
            drawerEmail.setText(session.getUser().getEmail());
            if (darkModeToggle != null) darkModeToggle.setSelected(session.getUser().isDarkMode());
        }

        drawerOverlay.setVisible(true);
        drawerOverlay.setManaged(true);
        drawerContainer.setVisible(true);
        drawerContainer.setManaged(true);

        drawerContainer.setTranslateX(-280);
        TranslateTransition slide = new TranslateTransition(Duration.millis(260), drawerContainer);
        slide.setToX(0);
        slide.play();

        isOpen = true;
    }

    /**
     * Closes the drawer by sliding it back off screen and hiding the backdrop.
     */
    public void close() {
        if (drawerContainer == null) return;

        TranslateTransition slide = new TranslateTransition(Duration.millis(220), drawerContainer);
        slide.setToX(-280);
        slide.setOnFinished(e -> {
            drawerContainer.setVisible(false);
            drawerContainer.setManaged(false);
            drawerOverlay.setVisible(false);
            drawerOverlay.setManaged(false);
        });
        slide.play();
        isOpen = false;
    }

    /**
     * Closes the drawer and navigates to the public hotspot map.
     */
    private void onMap() {
        close();
        UIUtils.switchScene(stage, "dashboard-view.fxml");
    }

    /**
     * Closes the drawer with no further action. Placeholder for settings screen.
     */
    private void onSettings() { close(); }

    /**
     * Closes the drawer and navigates to the public crime reports screen.
     */
    private void onMyReports() {
        close();
        UIUtils.switchScene(stage, "my-reports-view.fxml");
    }

    /**
     * Closes the drawer and navigates to the crime reports screen to submit a new report.
     */
    private void onNewReport() {
        close();
        UIUtils.switchScene(stage, "crimes-view.fxml");
    }

    /**
     * Closes the drawer and navigates to the profile screen.
     */
    private void onProfile() {
        close();
        UIUtils.switchScene(stage, "profile-view.fxml");
    }

    /**
     * Saves the user's dark mode preference to the database when the toggle is changed.
     */
    private void onDarkModeToggle() {
        UserSession session = UserSession.getInstance();
        if (session != null && session.getUser() != null) {
            session.getUser().setDarkMode(darkModeToggle.isSelected());
            dao.updateUser(session.getUser());
        }
    }

    /**
     * Logs the user out, clears the session and navigates back to the login screen.
     */
    private void onLogout() {
        close();
        UserSession.logout();
        UIUtils.switchScene(stage, "login-view.fxml");
    }
}
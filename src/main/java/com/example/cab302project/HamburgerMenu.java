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
        if (darkModeToggle != null) darkModeToggle.setOnAction(e -> onDarkModeToggle());
    }

    /** Toggle the drawer open or closed */
    public void toggle() {
        if (isOpen) close();
        else        open();
    }

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

    private void onMap() {
        close();
        UIUtils.switchScene(stage, "dashboard-view.fxml");
    }

    private void onSettings() { close(); }

    private void onMyReports() {
        close();
        UIUtils.switchScene(stage, "crimes-view.fxml");
    }

    private void onNewReport() {
        close();
        UIUtils.switchScene(stage, "crimes-view.fxml");
    }

    private void onProfile() {
        close();
        UIUtils.switchScene(stage, "profile-view.fxml");
    }

    private void onDarkModeToggle() {
        UserSession session = UserSession.getInstance();
        if (session != null && session.getUser() != null) {
            session.getUser().setDarkMode(darkModeToggle.isSelected());
            dao.updateUser(session.getUser());
        }
    }

    private void onLogout() {
        close();
        UserSession.logout();
        UIUtils.switchScene(stage, "login-view.fxml");
    }
}
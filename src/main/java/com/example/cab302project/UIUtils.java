package com.example.cab302project;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

import java.io.IOException;

public class UIUtils {

    /**
     * Shows a standard JavaFX alert dialog.
     */
    public static void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Swaps the current scene to a new FXML file.
     * @param stage The stage to update (usually retrieved from a node)
     * @param fxmlFile The name of the FXML file (e.g., "dashboard-view.fxml")
     */
    public static void switchScene(Stage stage, String fxmlFile) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource(fxmlFile));
            Scene scene = new Scene(fxmlLoader.load(), HelloApplication.WIDTH, HelloApplication.HEIGHT);
            stage.setScene(scene);
            //stage.sizeToScene();
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to load FXML file: " + fxmlFile);
            e.printStackTrace();
            showAlert(AlertType.ERROR, "System Error", "Could not load the next screen.");
        }
    }
}
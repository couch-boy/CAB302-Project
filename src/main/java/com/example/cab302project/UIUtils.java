package com.example.cab302project;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UIUtils {
    // For DB storage
    public static final DateTimeFormatter DB_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // For User Interface display
    public static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * @param dateTime A LocalDateTime object to be formatted
     * @return A String containing timestamp information, formatted for DB insertion
     */
    public static String formatForDb(LocalDateTime dateTime) {
        return dateTime.format(DB_FORMATTER);
    }

    /**
     * @param dateString A timestamp String directly from the DB
     * @return A LocalDateTime object for use by the program/UI
     */
    public static LocalDateTime parseFromDb(String dateString) {
        return LocalDateTime.parse(dateString, DB_FORMATTER);
    }

    /**
     * @param dateTime LocalDateTime object to be formatted
     * @return A formatted String for displaying a date and time in the UI
     */
    public static String formatLocalDateTime(LocalDateTime dateTime) {
        return (dateTime == null) ? "" : dateTime.format(DISPLAY_FORMATTER);
    }

    /**
     * @param value A boolean value
     * @return A Yes/No representation of the input value
     */
    public static String formatBoolean(boolean value) {
        return value ? "Yes" : "No";
    }

    /**
     * Basic regex email validation.
     */
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.trim().matches(emailRegex);
    }

    /**
     * Checks if phone is exactly 10 digits.
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        String phoneRegex = "^\\d{10}$";
        return phone.trim().matches(phoneRegex);
    }

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
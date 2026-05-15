package com.example.cab302project;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * A collection of static utility methods for UI management, data validation,
 * and formatting. This class provides centralized logic for common tasks
 * such as scene switching, alert display, and date-time parsing.
 */
public class UIUtils {

    /**
     * Formatter for database-compatible timestamp strings.
     * Format: {@code yyyy-MM-dd HH:mm:ss}
     */
    public static final DateTimeFormatter DB_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Formatter for user-friendly display of timestamps.
     * Format: {@code dd/MM/yyyy HH:mm}
     */
    public static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Formats a {@link LocalDateTime} object into a string suitable for SQLite storage.
     *
     * @param dateTime The date and time to be formatted.
     * @return A string representation of the timestamp in DB format.
     */
    public static String formatForDb(LocalDateTime dateTime) {
        return dateTime.format(DB_FORMATTER);
    }

    /**
     * Parses a timestamp string retrieved from the database into a {@link LocalDateTime} object.
     *
     * @param dateString The raw timestamp string from the database.
     * @return A {@link LocalDateTime} representation of the input string.
     */
    public static LocalDateTime parseFromDb(String dateString) {
        return LocalDateTime.parse(dateString, DB_FORMATTER);
    }

    /**
     * Formats a {@link LocalDateTime} for display within the application UI.
     *
     * @param dateTime The date and time to format.
     * @return A formatted string (dd/MM/yyyy HH:mm), or an empty string if input is null.
     */
    public static String formatLocalDateTime(LocalDateTime dateTime) {
        return (dateTime == null) ? "" : dateTime.format(DISPLAY_FORMATTER);
    }

    /**
     * Converts a boolean value into a "Yes" or "No" string for UI display.
     *
     * @param value The boolean state to represent.
     * @return "Yes" if true, "No" if false.
     */
    public static String formatBoolean(boolean value) {
        return value ? "Yes" : "No";
    }

    /**
     * Validates an email address using a standard regular expression.
     *
     * @param email The string to validate.
     * @return true if the email format is valid; false if null or invalid.
     */
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.trim().matches(emailRegex);
    }

    /**
     * Validates that a phone number consists of exactly 10 numeric digits.
     *
     * @param phone The string to validate.
     * @return true if the string contains exactly 10 digits; false otherwise.
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        String phoneRegex = "^\\d{10}$";
        return phone.trim().matches(phoneRegex);
    }

    /**
     * Validates if a set of coordinates is within global bounds.
     *
     * @param lat Latitude to check.
     * @param lon Longitude to check.
     * @return true if coordinates are valid, false otherwise.
     */
    public static boolean isValidCoordinate(double lat, double lon) {
        return lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
    }

    /**
     * Displays a standardized JavaFX Alert dialog styled to match the RADIUS design system.
     * Injects the application stylesheet and applies the alert-dialog-pane CSS class so
     * the dialog uses the correct fonts, colours, and button styles.
     *
     * @param alertType The {@link AlertType} (e.g., INFORMATION, ERROR, WARNING).
     * @param title     The text to display in the window title bar.
     * @param message   The main content text to display in the alert body.
     */
    public static void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Inject the RADIUS stylesheet and mark the dialog pane with our CSS class
        DialogPane dialogPane = alert.getDialogPane();
        URL stylesheet = HelloApplication.class.getResource("styles.css");
        if (stylesheet != null) {
            dialogPane.getStylesheets().add(stylesheet.toExternalForm());
        }
        dialogPane.getStyleClass().add("alert-dialog-pane");

        alert.showAndWait();
    }

    /**
     * Transitions the application to a different scene by loading an FXML file.
     * This method uses the global width and height constants defined in {@link HelloApplication}.
     *
     * @param stage    The current {@link Stage} to update.
     * @param fxmlFile The filename of the FXML resource (e.g., "dashboard-view.fxml").
     */
    public static void switchScene(Stage stage, String fxmlFile) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource(fxmlFile));
            Scene scene = new Scene(fxmlLoader.load(), HelloApplication.WIDTH, HelloApplication.HEIGHT);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to load FXML file: " + fxmlFile);
            e.printStackTrace();
            showAlert(AlertType.ERROR, "System Error", "Could not load the next screen.");
        }
    }
}
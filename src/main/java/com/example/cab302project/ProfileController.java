package com.example.cab302project;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ProfileController {

    // FXML UI elements
    @FXML
    private Label usernameLabel;
    @FXML
    private TextField emailField, phoneField, locationField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private CheckBox darkModeCheckBox;

    private IAppDAO dao;
    private User currentUser;

    // Constructor
    public ProfileController() {
        this.dao = HelloApplication.DATABASE;
    }

    /**
     * This method runs automatically after the FXML has loaded
     */
    @FXML
    public void initialize() {
        // Get User object from UserSession
        currentUser = UserSession.getInstance().getUser();

        // If User is not null
        if (currentUser != null) {
            // Populate UI fields with user data
            populateFields();
        }
    }

    /**
     * Attempt to update stored user information using entered details
     */
    @FXML
    public void onSave() {
        try {
            // Use the helper to process the form
            updateUserFromForm();
            // Form updates are now applied to the User object in the UserSession

            // Push updated User from UserSession into the database
            if (dao.updateUser(currentUser)) {
                UIUtils.showAlert(Alert.AlertType.INFORMATION, "Success", "Profile updated successfully.");
                populateFields(); // Refresh UI to confirm state
            } else {
                UIUtils.showAlert(Alert.AlertType.ERROR, "Database Error", "Could not save changes.");
            }
        } catch (IllegalArgumentException e) {
            UIUtils.showAlert(Alert.AlertType.WARNING, "Validation Error", e.getMessage());
        } catch (Exception e) {
            UIUtils.showAlert(Alert.AlertType.ERROR, "Input Error", "Please check your coordinate format.");
        }
    }

    /**
     * Return to the dashboard view
     */
    @FXML
    public void onBack() {
        Stage stage = (Stage) usernameLabel.getScene().getWindow();
        UIUtils.switchScene(stage, "dashboard-view.fxml");
    }

    // Helper method to fill UI elements with the User object data
    private void populateFields() {
        usernameLabel.setText(currentUser.getUsername());
        emailField.setText(currentUser.getEmail());
        phoneField.setText(currentUser.getPhone());
        darkModeCheckBox.setSelected(currentUser.isDarkMode());
        locationField.setText(String.format("%.4f, %.4f",
                currentUser.getHomeLatitude(), currentUser.getHomeLongitude()));
        passwordField.clear(); // Clear for security
    }

    // Helper method to capture data from UI and update the User object
    // Throws an exception if data is incorrectly formatted
    // Error message is passed to parent function (onSave)
    private void updateUserFromForm() throws Exception {
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String newPassword = passwordField.getText();

        // Split into lat/lon by a comma followed by zero or more whitespace characters
        String[] coords = locationField.getText().split(",\\s*");

        // Validate entered data and throw exception if invalid
        if (!UIUtils.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format.");
        }
        if (!UIUtils.isValidPhone(phone)) {
            throw new IllegalArgumentException("Phone number must be 10 digits.");
        }
        if (coords.length != 2) {
            throw new IllegalArgumentException("Location must be 'lat, lon'.");
        }

        // Convert coords to lat/lon (no trim required due to regex)
        double lat = Double.parseDouble(coords[0]);
        double lon = Double.parseDouble(coords[1]);

        // Update the User Object
        // Only update password if not null, and if a new password was actually entered
        // Avoids updating password to empty string
        if (newPassword != null && !newPassword.isEmpty()) {
            currentUser.setPassword(newPassword);
        }
        currentUser.setEmail(email);
        currentUser.setPhone(phone);
        currentUser.setHomeLocation(lat, lon);
        currentUser.setDarkMode(darkModeCheckBox.isSelected());
    }
}
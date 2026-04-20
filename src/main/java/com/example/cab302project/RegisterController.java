package com.example.cab302project;

import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegisterController {

    // FXML UI elements
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneField;


    private IAppDAO dao;

    // Constructor
    public RegisterController() {
        //get main application dao instance
        this.dao = HelloApplication.DATABASE;
    }

    /**
     * Attempt to register a new user to the database using entered details
     */
    @FXML
    public void onRegisterAccount() {
        // Trim whitespace from end of non-password fields
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();


        //check for empty fields
        if (username.isEmpty() || password.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            UIUtils.showAlert(AlertType.WARNING, "Registration Error", "All fields are required.");
            return;
        }

        //validate email
        if (!UIUtils.isValidEmail(email)) {
            UIUtils.showAlert(AlertType.ERROR, "Invalid Email", "Please enter a valid email address.");
            return;
        }

        //validate phone
        if (!UIUtils.isValidPhone(phone)) {
            UIUtils.showAlert(AlertType.ERROR, "Invalid Phone", "Phone number must be 10 digits.");
            return;
        }

        // Create User object using Brisbane CBD lat/lon and default darkmode to off
        User newUser = new User(username, password, email, phone, -27.4709, 153.0235, false);

        // Attempt to add to db
        boolean success = dao.addUser(newUser);

        if (success) {
            UIUtils.showAlert(AlertType.INFORMATION, "Success", "Account created! You can now login.");
            //return to login screen
            onReturnToLogin();
        } else {
            UIUtils.showAlert(AlertType.ERROR, "Database Error", "Username already exists.");
        }

    }

    /**
     * Return to user login view
     */
    @FXML
    public void onReturnToLogin() {

        //get the current stage (window) by referencing a ui element
        Stage stage = (Stage) usernameField.getScene().getWindow();
        //load login view
        UIUtils.switchScene(stage, "login-view.fxml");

    }

}

package com.example.cab302project;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

/**
 * Controller class for the Login screen (login-view.fxml).
 *
 * Handles user authentication by interfacing with the {@link IAppDAO} and
 * manages navigation between the login, registration, and dashboard screens.
 */
public class LoginController {

    /** Input field for the user's account username. */
    @FXML
    private TextField usernameField;

    /** Input field for the user's account password. */
    @FXML
    private PasswordField passwordField;

    /** Data Access Object used for user validation. */
    private IAppDAO dao;

    /**
     * Initializes a new LoginController.
     * Connects to the global database instance defined in {@link HelloApplication}.
     */
    public LoginController() {
        //get main application dao instance
        this.dao = HelloApplication.DATABASE;
    }

    /**
     * Handles the login button action.
     * Validates that input fields are not empty, verifies credentials via the DAO,
     * and switches to the appropriate dashboard based on the user's {@link User#isPolice()} status.
     *
     * <p>If authentication is successful, the user details are stored in {@link UserSession}.</p>
     */
    @FXML
    public void onLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // Check if fields are empty before hitting the database
        if (username.isEmpty() || password.isEmpty()) {
            UIUtils.showAlert(Alert.AlertType.WARNING, "Form Error!", "Please enter both username and password.");
            return;
        }

        // Get user object using dao function
        User authenticatedUser = dao.validateUser(username, password);

        // If user is successfully validated
        if (authenticatedUser != null) {

            // Log user in to UserSession
            UserSession.login(authenticatedUser);

            // Get the current stage (window) by referencing a UI element
            Stage stage = (Stage) usernameField.getScene().getWindow();

            // Directs user to the correct dashboard based on role
            if (authenticatedUser.isPolice()) {
                UIUtils.switchScene(stage, "police-dashboard-view.fxml");
            } else {
                UIUtils.switchScene(stage, "dashboard-view.fxml");
            }

        } else {
            UIUtils.showAlert(AlertType.ERROR, "Login Failed!", "Invalid username or password.");
        }
    }

    /**
     * Handles the register button action.
     * Navigates the user to the registration screen.
     */
    @FXML
    public void onRegister() {

        // Get the current stage (window) by referencing a UI element
        Stage stage = (Stage) usernameField.getScene().getWindow();
        // Load register view
        UIUtils.switchScene(stage, "register-view.fxml");
    }

    /**
     * Action handler for the Login tab button.
     * Currently a no-op as the user is already on the login screen.
     */
    @FXML
    public void onTabLogin() {}

    /**
     * Action handler for the Sign Up tab button.
     * Redirects the user to the registration screen.
     */
    @FXML
    public void onTabSignup() {
        onRegister();
    }
}

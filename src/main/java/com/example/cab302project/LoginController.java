package com.example.cab302project;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

public class LoginController {

    // FXML UI elements
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;

    private IAppDAO dao;

    // Constructor
    public LoginController() {
        //get main application dao instance
        this.dao = HelloApplication.DATABASE;
    }

    /**
     * Validate user information and attempt to move to the dashboard
     */
    @FXML
    public void onLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        //check if fields are empty before hitting the database
        if (username.isEmpty() || password.isEmpty()) {
            UIUtils.showAlert(Alert.AlertType.WARNING, "Form Error!", "Please enter both username and password.");
            return;
        }

        //get user object using dao function
        User authenticatedUser = dao.validateUser(username, password);

        if (authenticatedUser != null) {
            //showAlert(AlertType.INFORMATION, "Login Successful!", "Welcome, " + username + "!");
            UserSession.login(authenticatedUser);

            //get the current stage (window) by referencing a ui element
            Stage stage = (Stage) usernameField.getScene().getWindow();
            //load dashboard view
            UIUtils.switchScene(stage, "dashboard-view.fxml");

        } else {
            UIUtils.showAlert(AlertType.ERROR, "Login Failed!", "Invalid username or password.");
        }

    }

    /**
     * Move to the new user registration screen
     */
    @FXML
    public void onRegister() {

        //get the current stage (window) by referencing a ui element
        Stage stage = (Stage) usernameField.getScene().getWindow();
        //load register view
        UIUtils.switchScene(stage, "register-view.fxml");

    }
}

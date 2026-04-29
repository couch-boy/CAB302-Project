package com.example.cab302project;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * Controller for the welcome/splash screen (hello-view.fxml).
 *
 * Shown on first launch, providing buttons to navigate to the
 * login screen or the registration screen.
 */
public class HelloController {

    // FXML UI elements
    @FXML
    private Label welcomeText;
    @FXML
    private Button loginButton;

    /**
     * "Create an Account" button - navigates to register screen.
     * Previously showed a test label, now routes to registration.
     */
    @FXML
    protected void onHelloButtonClick() {
        Stage stage = (Stage) loginButton.getScene().getWindow();
        UIUtils.switchScene(stage, "register-view.fxml");
    }

    /**
     * "Sign In" button - navigates to login screen.
     */
    @FXML
    protected void onLoginButtonClick() {
        Stage stage = (Stage) loginButton.getScene().getWindow();
        UIUtils.switchScene(stage, "login-view.fxml");
    }
}
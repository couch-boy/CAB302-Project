package com.example.cab302project;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloController {

    // FXML UI elements
    @FXML
    private Label welcomeText;
    @FXML
    private Button loginButton;

    /**
     * Display welcome text (NEEDS TO BE REMOVED LATER)
     */
    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }

    /**
     * Go to user login screen
     */
    @FXML
    protected void onLoginButtonClick() {
        Stage stage = (Stage) loginButton.getScene().getWindow();
        UIUtils.switchScene(stage, "login-view.fxml");
    }

}

package com.example.cab302project;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

public class DashboardController {
    @FXML
    private Label welcomeLabel;
    @FXML
    private Label emailLabel;

    private SqliteDAO dao;

    public DashboardController() {
        //get main application dao instance
        this.dao = HelloApplication.DATABASE;
    }

    @FXML
    public void initialize() {
        //this method auto-runs when dashboard-view.fxml loads
        UserSession session = UserSession.getInstance();

        if (session != null) {
            welcomeLabel.setText("Welcome back, " + session.getUser().getUsername() + "!");
            emailLabel.setText("Your email is: " + session.getUser().getEmail());
        }
    }

    @FXML
    public void onLogout() {

        UserSession.logout();

        //get the current stage (window) by referencing a ui element
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        //load login view
        UIUtils.switchScene(stage, "login-view.fxml");

    }

    @FXML
    public void viewCrimes() {

        //get the current stage (window) by referencing a ui element
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        //load crimes view
        UIUtils.switchScene(stage, "crimes-view.fxml");

    }


}

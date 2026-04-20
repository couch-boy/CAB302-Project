package com.example.cab302project;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class DashboardController {

    // FXML UI elements
    @FXML
    private Label welcomeLabel;
    @FXML
    private Label emailLabel;

    private IAppDAO dao;

    // Constructor
    public DashboardController() {
        //get main application dao instance
        this.dao = HelloApplication.DATABASE;
    }

    /**
     * This method runs automatically after the FXML has loaded
     */
    @FXML
    public void initialize() {
        //this method auto-runs when dashboard-view.fxml loads
        UserSession session = UserSession.getInstance();

        if (session != null) {
            welcomeLabel.setText("Welcome back, " + session.getUser().getUsername() + "!");
            emailLabel.setText("Your email is: " + session.getUser().getEmail());
        }
    }

    /**
     * Return to login screen and logout current UserSession
     */
    @FXML
    public void onLogout() {

        UserSession.logout();

        //get the current stage (window) by referencing a ui element
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        //load login view
        UIUtils.switchScene(stage, "login-view.fxml");

    }

    /**
     * Go to crimes view
     */
    @FXML
    public void viewCrimes() {

        //get the current stage (window) by referencing a ui element
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        //load crimes view
        UIUtils.switchScene(stage, "crimes-view.fxml");

    }

    /**
     * Go to profile view
     */
    @FXML
    public void viewProfile() {
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        UIUtils.switchScene(stage, "profile-view.fxml");
    }


}


package com.example.cab302project;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;

    private SqliteDAO dao;

    public LoginController() {
        //get main application dao instance
        this.dao = HelloApplication.DATABASE;
    }

    @FXML
    public void onLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // Check if fields are empty before hitting the database
        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Form Error!", "Please enter both username and password.");
            return;
        }

        //get user object using dao function
        User authenticatedUser = dao.validateUser(username, password);

        if (authenticatedUser != null) {
            //showAlert(AlertType.INFORMATION, "Login Successful!", "Welcome, " + username + "!");
            UserSession.login(authenticatedUser);

            try {
                //get the current stage (window) by referencing  ui element
                Stage stage = (Stage) usernameField.getScene().getWindow();
                //load main application view
                FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("dashboard-view.fxml"));
                Scene scene = new Scene(fxmlLoader.load(), HelloApplication.WIDTH, HelloApplication.HEIGHT);
                String css = HelloApplication.class.getResource("style.css").toExternalForm();
                scene.getStylesheets().add(css);
                stage.setScene(scene);

            } catch (IOException e) {
                e.printStackTrace();
                showAlert(AlertType.ERROR, "System Error", "Could not load the user dashboard screen.");
            }

        } else {
            showAlert(AlertType.ERROR, "Login Failed!", "Invalid username or password.");
        }

    }

    @FXML
    public void onRegister() {
        try {
            //get the current stage (window) by referencing  ui element
            Stage stage = (Stage) usernameField.getScene().getWindow();
            //load register view
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("register-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), HelloApplication.WIDTH, HelloApplication.HEIGHT);
            stage.setScene(scene);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Helper method to show pop-up messages to the user
    private void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

package com.example.cab302project;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Button;

import java.io.IOException;

public class PoliceDashboardController
{
    @FXML
    private Button logoutButton;

    @FXML
    public void onLogout() {
        UserSession.logout();

        try
        {
            //get the current stage
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            //load login view
            FXMLLoader fxmlLoader = new FXMLLoader
                    (HelloApplication.class.getResource("login-view.fxml")
                    );

            Scene scene = new Scene(
                    fxmlLoader.load(),
                    HelloApplication.WIDTH,
                    HelloApplication.HEIGHT);

            stage.setScene(scene);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

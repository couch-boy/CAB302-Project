package com.example.cab302project;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import javafx.scene.control.Button;

import java.io.IOException;

public class PoliceDashboardController
{
    @FXML
    private ListView<CrimeReport> reportListView;

    @FXML
    private Button logoutButton;

    // Temp
    @FXML
    public void initialize()
    {
        ObservableList<CrimeReport> reports = FXCollections.observableArrayList
                (
                        new CrimeReport("Robbery at gas station", "HIGH", "7:35PM"),
                        new CrimeReport("Noise complaint", "LOW", "11:00PM"),
                        new CrimeReport("Assult and battery", "HIGH", "11:45AM"),
                        new CrimeReport("Suspicious Activity", "MEDIUM", "8:00AM")
                );

        reportListView.setItems(reports);
    }


    @FXML
    public void onLogout()
    {
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

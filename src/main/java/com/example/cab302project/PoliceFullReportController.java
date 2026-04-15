package com.example.cab302project;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import java.io.IOException;

public class PoliceFullReportController {
    @FXML
    private Label descriptionLabel;
    @FXML
    private Label priorityLabel;
    @FXML
    private Label timeLabel;

    private CrimeReport report;

    // get data from police dashboard
    public void setReport(CrimeReport report)
    {
        this.report = report;

        descriptionLabel.setText("Description: " + report.getDescription());
        priorityLabel.setText("Priority: " + report.getPriority());
        timeLabel.setText("Time: " + report.getTime());
    }

    @FXML
    public void onBack()
    {
        try {
            // Load the police dashboard screen
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("police-dashboard-view.fxml")
            );

            Scene scene = new Scene(
                    loader.load(),
                    HelloApplication.WIDTH,
                    HelloApplication.HEIGHT
            );

            Stage stage = (Stage) descriptionLabel.getScene().getWindow();
            stage.setScene(scene);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

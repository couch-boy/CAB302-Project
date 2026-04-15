package com.example.cab302project;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;

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
        // temporary list of crime reports
        ObservableList<CrimeReport> reports = FXCollections.observableArrayList
                (
                        new CrimeReport("Robbery at gas station", "HIGH", "7:35PM"),
                        new CrimeReport("Noise complaint", "LOW", "11:00PM"),
                        new CrimeReport("Assult and battery", "HIGH", "11:45AM"),
                        new CrimeReport("Suspicious Activity", "MEDIUM", "8:00AM")
                );

        // Link the data so it displays on the dashboard
        reportListView.setItems(reports);

        // cell to add full report
        reportListView.setCellFactory(listView -> new ListCell<>()
        {
            private final Button viewButton = new Button("Full Report");

            @Override
            protected void updateItem(CrimeReport report, boolean empty) {
                super.updateItem(report, empty);

                if (empty || report == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(report.toString());

                    viewButton.setOnAction(e -> openFullReport(report));

                    setGraphic(viewButton);
                }
            }
        }
        );
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

    // Opens the full report screen
    private void openFullReport(CrimeReport report) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("police-full-report-view.fxml")
            );

            Scene scene = new Scene(
                    loader.load(),
                    HelloApplication.WIDTH,
                    HelloApplication.HEIGHT
            );

            // pass selected report to next script
            PoliceFullReportController controller = loader.getController();
             controller.setReport(report);

             // switch scene
            Stage stage = (Stage) reportListView.getScene().getWindow();
            stage.setScene(scene);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

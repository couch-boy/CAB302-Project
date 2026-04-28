package com.example.cab302project;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.ListCell;
import java.util.List;

public class MyReportsController
{
    @FXML private ListView<CrimeRecord> crimeListView;

    @FXML private TableView<CrimeRecord> crimeTable;

    private IAppDAO dao;

    public MyReportsController() {
        this.dao = HelloApplication.DATABASE;
    }

    @FXML
    public void initialize() {
        List<CrimeRecord> allCrimes = dao.getAllCrimes();

        String currentUser =
                UserSession.getInstance().getUser().getUsername();

        List<CrimeRecord> myReports = allCrimes.stream()
                .filter(c -> currentUser.equals(c.getReporter()))
                .toList();

        crimeTable.getItems().setAll(myReports);
        crimeListView.getItems().setAll(myReports);

        setupListView();
    }

    private void setupListView() {
        crimeListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(CrimeRecord crime, boolean empty) {
                super.updateItem(crime, empty);

                if (empty || crime == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                setText(
                        crime.getCategory() + "\n" +
                                crime.getDescription() + "\n" +
                                crime.getTimestamp()
                );
            }
        });
    }
}

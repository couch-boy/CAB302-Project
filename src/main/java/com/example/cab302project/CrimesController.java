package com.example.cab302project;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class CrimesController {

    @FXML
    private TableView<CrimeRecord> crimeTable;
    @FXML
    private TableColumn<CrimeRecord, Integer> idColumn;
    @FXML
    private TableColumn<CrimeRecord, CrimeCategory> categoryColumn;
    @FXML
    private TableColumn<CrimeRecord, CrimeSeverity> severityColumn;

    @FXML
    private ComboBox<CrimeCategory> categoryComboBox;
    @FXML
    private ComboBox<CrimeSeverity> severityComboBox;
    @FXML
    private TextField reporterField, locationField;
    @FXML
    private TextArea descriptionArea;

    private SqliteDAO dao;

    public CrimesController() {
        //get main application dao instance
        this.dao = HelloApplication.DATABASE;
    }

    /**
     * This method runs automatically after the FXML has loaded.
     */
    @FXML
    public void initialize() {
        //link columns to table
        //these must match the names of your getters (getCategory -> "category")
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        severityColumn.setCellValueFactory(new PropertyValueFactory<>("severity"));
        //populate dropdowns with enum values
        categoryComboBox.getItems().setAll(CrimeCategory.values());
        severityComboBox.getItems().setAll(CrimeSeverity.values());

        //load data from dao
        crimeTable.getItems().setAll(dao.getAllCrimes());

        //selection listener
        crimeTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                categoryComboBox.setValue(newVal.getCategory());
                severityComboBox.setValue(newVal.getSeverity());
                reporterField.setText(newVal.getReporter());
                locationField.setText(String.format("%.4f, %.4f", newVal.getLatitude(), newVal.getLongitude()));
                descriptionArea.setText(newVal.getDescription());
            }
        });

        //autoselect the first item
        if (!crimeTable.getItems().isEmpty()) {
            crimeTable.getSelectionModel().selectFirst();
        }

    }

    @FXML
    public void onUpdateCrime() {
        CrimeRecord selectedCrime = crimeTable.getSelectionModel().getSelectedItem();

        if (selectedCrime != null) {
            try {
                // Get values from ComboBoxes and Fields
                CrimeCategory updatedCategory = categoryComboBox.getValue();
                CrimeSeverity updatedSeverity = severityComboBox.getValue();
                String updatedDescription = descriptionArea.getText();

                // Parse the coordinates back from the TextField
                String[] coords = locationField.getText().split(",");
                double lat = Double.parseDouble(coords[0].trim());
                double lon = Double.parseDouble(coords[1].trim());

                // Create updated object
                CrimeRecord updatedRecord = new CrimeRecord(
                        selectedCrime.getId(),
                        updatedCategory,
                        updatedSeverity,
                        selectedCrime.getReporter(),
                        updatedDescription,
                        lat,
                        lon
                );

                // Save to Database
                if (dao.updateCrime(updatedRecord)) {
                    UIUtils.showAlert(Alert.AlertType.INFORMATION, "Success", "Crime updated successfully!");
                    refreshList();
                }
            } catch (Exception e) {
                UIUtils.showAlert(Alert.AlertType.ERROR, "Input Error", "Check your data format.");
            }
        }
    }

    @FXML
    public void onBackButtonClick() {

        //get the current stage (window) by referencing a ui element
        Stage stage = (Stage) crimeTable.getScene().getWindow();
        //load crimes view
        UIUtils.switchScene(stage, "dashboard-view.fxml");

    }

    //helper to reload table data
    private void refreshList() {
        int selectedIndex = crimeTable.getSelectionModel().getSelectedIndex();
        crimeTable.getItems().setAll(dao.getAllCrimes());
        crimeTable.getSelectionModel().select(selectedIndex);
    }
}

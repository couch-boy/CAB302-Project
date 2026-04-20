package com.example.cab302project;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class CrimesController {

    // FXML UI elements
    @FXML
    private TableView<CrimeRecord> crimeTable;
    @FXML
    private TableColumn<CrimeRecord, Integer> idColumn;
    @FXML
    private TableColumn<CrimeRecord, CrimeCategory> categoryColumn;
    @FXML
    private TableColumn<CrimeRecord, String> severityColumn;
    @FXML
    private TableColumn<CrimeRecord, String> timestampColumn; // String for formatted display of timestamp
    @FXML
    private TableColumn<CrimeRecord, String> actionedColumn; // Displays Yes/No

    @FXML
    private ComboBox<CrimeCategory> categoryComboBox;
    @FXML
    private Label idLabel, severityLabel;
    @FXML
    private DatePicker datePicker;
    @FXML
    private ComboBox<String> hourBox, minuteBox, ampmBox;
    @FXML
    private TextField reporterField, locationField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private CheckBox actionedCheckBox;

    private IAppDAO dao;

    // Constructor
    public CrimesController() {
        //get main application dao instance
        this.dao = HelloApplication.DATABASE;
    }

    /**
     * This method runs automatically after the FXML has loaded
     */
    @FXML
    public void initialize() {
        // Initialize table columns
        setupTableColumns();

        // Initialize date and time UI elements
        setupDateTimeControls();

        // Set dropdown values
        categoryComboBox.getItems().setAll(CrimeCategory.values());

        // Initialize Listener -> Auto-update Severity Label when Category changes
        categoryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) severityLabel.setText(newVal.getSeverity().toString());
        });

        // Initialize Listener -> Update displayed CrimeRecord data when there are changes
        crimeTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                populateForm(newVal);
            } else {
                clearForm(); // This ensures the form empties when nothing is selected
            }
        });

        // Load and Display Data
        refreshList();
        // Update selected list element after populating
        updateSelectionAfterChange();
    }

    /**
     * This method captures the data from various UI elements to update stored crime details
     */
    @FXML
    public void onSave() {
        // Capture current CrimeRecord information to pass information for update
        CrimeRecord selected = crimeTable.getSelectionModel().getSelectedItem();

        // Avoid writing null to database
        if (selected == null) return;

        try {
            // Create new CrimeRecord object using helper method to capture form data
            CrimeRecord recordFromForm = createRecordFromForm(selected);

            if (selected.getId() == 0) {
                // CASE: This is a brand new record (id == 0)
                if (dao.addCrime(recordFromForm)) {
                    UIUtils.showAlert(Alert.AlertType.INFORMATION, "Success", "New crime reported successfully.");
                    refreshList(); // This refreshes from DB, giving us the real ID
                }
            } else {
                // CASE: This is an edit of an existing record (id != 0)
                if (dao.updateCrime(recordFromForm)) {
                    UIUtils.showAlert(Alert.AlertType.INFORMATION, "Success", "Record updated.");
                    refreshList();
                }
            }
        } catch (Exception e) {
            UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Could not save: " + e.getMessage());
        }
    }

    /**
     * Add a new crime to the database after entering details
     */
    @FXML
    public void onAddNewCrime() {
        // Check if there is a pending new CrimeRecord waiting to be saved
        if (crimeTable.getItems().stream().anyMatch(c -> c.getId() == 0)) {
            UIUtils.showAlert(Alert.AlertType.WARNING, "Pending Report",
                    "Please save or refresh before creating another new report.");
            return;
        }

        // Create blank CrimeRecord object template
        // Set id to 0
        // Default lat/lon for Brisbane CBD
        // UserSession.getInstance().getUser().getUsername() provides the reporter username
        CrimeRecord newRecord = new CrimeRecord(
                0,
                CrimeCategory.OTHER,
                LocalDateTime.now(),
                -27.4709,
                153.0235,
                "",
                UserSession.getInstance().getUser().getUsername(),
                false
        );

        // Add template to the table temporarily
        crimeTable.getItems().add(newRecord);

        // Select template so the form populates with default values from the template
        crimeTable.getSelectionModel().select(newRecord);

        // Scroll to the template in the crime table so the user sees the new entry
        crimeTable.scrollTo(newRecord);

        // Display popup prompting user to fill in template details and save to store in database
        UIUtils.showAlert(Alert.AlertType.INFORMATION, "New Report",
                "A new blank report has been created. Fill in the details and click 'Save Changes'.");
    }

    /**
     * Delete an existing crime
     */
    @FXML
    public void onDeleteCrime() {
        // Get currently selected CrimeRecord object
        CrimeRecord selected = crimeTable.getSelectionModel().getSelectedItem();

        // If selection is null, show error message
        if (selected == null) {
            UIUtils.showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a report to delete.");
            return;
        }

        // Confirmation dialog
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Are you sure you want to delete this report?");
        confirm.setContentText("This action cannot be undone.");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            // Flag for successful entry deletion
            boolean success = false;

            if (selected.getId() == 0) {
                // It's a temporary draft (id == 0), so just remove it from the UI
                crimeTable.getItems().remove(selected);
                success = true; // Set to true after draft entry deletion
            } else {
                // It's a record in the database (id != 0), so delete via DAO
                if (dao.deleteCrime(selected.getId())) {
                    UIUtils.showAlert(Alert.AlertType.INFORMATION, "Deleted", "Report successfully removed.");
                    refreshList();
                    success = true; // Set to true after db entry deletion
                } else {
                    UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Could not delete from database.");
                }
            }

            // If something was removed, update selected list element
            if (success) {
                updateSelectionAfterChange();
            }
        }
    }

    /**
     * Return to the previous menu (dashboard)
     */
    @FXML
    public void onBackButtonClick() {
        Stage stage = (Stage) crimeTable.getScene().getWindow();
        UIUtils.switchScene(stage, "dashboard-view.fxml");
    }

    // Helper function to refresh crime table with updated crime data
    private void refreshList() {
        int selectedIndex = crimeTable.getSelectionModel().getSelectedIndex();
        crimeTable.getItems().setAll(dao.getAllCrimes());
        if (selectedIndex >= 0) {
            crimeTable.getSelectionModel().select(selectedIndex);
        }
    }

    // Helper method to initialize table columns
    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));

        // Check enum value for severity
        severityColumn.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getCategory().getSeverity().toString()));

        // Get formatted string from LocalDateTime timestamp
        timestampColumn.setCellValueFactory(cd ->
                new SimpleStringProperty(UIUtils.formatLocalDateTime(cd.getValue().getTimestamp())));

        // Convert boolean to Yes/No
        actionedColumn.setCellValueFactory(cd ->
                new SimpleStringProperty(UIUtils.formatBoolean(cd.getValue().isActioned())));
    }

    // Helper method to initialize date and time UI elements
    private void setupDateTimeControls() {
        // Hours 1-12
        for (int i = 1; i <= 12; i++) hourBox.getItems().add(String.format("%02d", i));

        // Minutes in 15m increments
        minuteBox.getItems().addAll("00", "15", "30", "45");

        // AM/PM
        ampmBox.getItems().addAll("AM", "PM");

        // Set comboboxes to default values during initialization
        hourBox.setValue("12");
        minuteBox.setValue("00");
        ampmBox.setValue("PM");
    }

    // Helper method to populate UI elements with data
    private void populateForm(CrimeRecord crime) {
        // Set ID label
        idLabel.setText(String.valueOf(crime.getId()));

        // Get LocalDateTime object from CrimeRecord
        LocalDateTime dt = crime.getTimestamp();

        categoryComboBox.setValue(crime.getCategory());
        datePicker.setValue(dt.toLocalDate());

        // Set AM/PM box based on 24hr time input value
        int hour = dt.getHour();
        ampmBox.setValue(hour >= 12 ? "PM" : "AM");

        // Display hours in 12hr format instead of 24hr (modulo 12)
        int displayHour = (hour % 12 == 0) ? 12 : hour % 12;
        hourBox.setValue(String.format("%02d", displayHour));

        // Display minutes by closest 15min interval (via integer division)
        int mins = dt.getMinute();
        minuteBox.setValue(String.format("%02d", (mins / 15) * 15));

        locationField.setText(String.format("%.4f, %.4f", crime.getLatitude(), crime.getLongitude()));
        descriptionArea.setText(crime.getDescription());
        reporterField.setText(crime.getReporterDisplayName());
        actionedCheckBox.setSelected(crime.isActioned());
    }

    // Helper method to create CrimeRecord object from form data
    private CrimeRecord createRecordFromForm(CrimeRecord original) {
        // Capture hour, minute, ap/pm values from UI
        int hour = Integer.parseInt(hourBox.getValue());
        int min = Integer.parseInt(minuteBox.getValue());
        String ampm = ampmBox.getValue();

        // Convert back to 24h format for LocalDateTime
        if (ampm.equals("PM") && hour < 12) hour += 12;
        if (ampm.equals("AM") && hour == 12) hour = 0;

        LocalDateTime newTimestamp = LocalDateTime.of(datePicker.getValue(), LocalTime.of(hour, min));

        // Parse coordinates using regex to handle spaces automatically
        String[] coords = locationField.getText().split(",\\s*");

        if (coords.length != 2) {
            throw new IllegalArgumentException("Location must be in 'latitude, longitude' format.");
        }

        double lat = Double.parseDouble(coords[0]);
        double lon = Double.parseDouble(coords[1]);

        // Bundle everything into the updated object
        return new CrimeRecord(
                original.getId(),
                categoryComboBox.getValue(),
                newTimestamp,
                lat,
                lon,
                descriptionArea.getText(),
                original.getReporter(), // Preserve original raw reporter data (username/null)
                actionedCheckBox.isSelected()
        );
    }

    // Helper method to clear form data
    private void clearForm() {
        idLabel.setText("-");
        severityLabel.setText("-");
        categoryComboBox.setValue(null);
        datePicker.setValue(null);
        hourBox.setValue(null);
        minuteBox.setValue(null);
        ampmBox.setValue(null);
        locationField.clear();
        descriptionArea.clear();
        reporterField.clear();
        actionedCheckBox.setSelected(false);
    }

    // Ensures the UI state is consistent after a list refresh or deletion
    // Selects the first item if available, otherwise clears the form
    private void updateSelectionAfterChange() {
        if (!crimeTable.getItems().isEmpty()) {
            crimeTable.getSelectionModel().selectFirst();
        } else {
            clearForm();
        }
    }
}

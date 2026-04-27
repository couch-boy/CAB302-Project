package com.example.cab302project;

import javafx.animation.TranslateTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ContextMenu;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;

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
    private TableColumn<CrimeRecord, String> actionedColumn;
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

    // New bindings for the redesigned list and detail panel
    @FXML
    private ListView<CrimeRecord> crimeListView;
    @FXML
    private VBox detailPanel;
    @FXML
    private Pane detailBackdrop;
    @FXML
    private Button saveBtn;

    @FXML
    private NavBarController navBarController;

    private IAppDAO dao;

    private IGeocodingService geocoder = new OpenStreetMapGeoCoder();
    @FXML

    private final ContextMenu suggestionsPopup = new ContextMenu();
    private final PauseTransition suggestionDelay = new PauseTransition(Duration.millis(400));

    private boolean isCreatingNew = false;

    // Cache of geocoded addresses keyed by crime ID.
    // Populated on list load and on row selection.
    // Static so the cache persists across screen navigations and avoids re-geocoding
    private static final Map<Integer, String> addressCache = new HashMap<>();

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
        // Initialize table columns (still required for all existing controller logic)
        setupTableColumns();

        // Mark Crimes tab as active in bottom nav
        if (navBarController != null) {
            navBarController.setActiveTab("crimes");
        }
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
        // Initialize address autocomplete suggestions for location input
        setupAddressAutocomplete();

        // Wire the styled ListView to back the hidden TableView
        setupListView();

        // Preload addresses for all crimes so list cells show locations immediately
        preloadAddresses();
    }

    /**
     * Handles saving a crime report based on form input.
     * If the selected record is new (id == 0), it is added to the database.
     * Existing records are treated as read-only and cannot be modified.
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
                // CASE: This is a brand new record
                if (dao.addCrime(recordFromForm)) {
                    UIUtils.showAlert(Alert.AlertType.INFORMATION, "Success", "New crime reported successfully.");
                    refreshList();
                    updateSelectionAfterChange();
                    onCloseDetail();
                }
            } else {
                // Existing crimes are view-only
                UIUtils.showAlert(Alert.AlertType.WARNING, "View Only", "Existing crime reports cannot be edited.");
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
        isCreatingNew = true;
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

        // Sync list view and open detail panel with save button visible
        crimeListView.getItems().setAll(crimeTable.getItems());
        crimeListView.getSelectionModel().selectLast();
        showDetailPanel(true);

        // Display popup prompting user to fill in template details and save to store in database
        UIUtils.showAlert(Alert.AlertType.INFORMATION, "New Report",
                "A new blank report has been created. Fill in the details and click 'Save Changes'.");
    }

    /**
     * Return to the previous menu (dashboard)
     */
    @FXML
    public void onBackButtonClick() {
        Stage stage = (Stage) categoryComboBox.getScene().getWindow();
        UIUtils.switchScene(stage, "dashboard-view.fxml");
    }

    /**
     * Close the detail panel - slides back down off screen
     */
    @FXML
    public void onCloseDetail() {
        TranslateTransition slide = new TranslateTransition(Duration.millis(280), detailPanel);
        slide.setToY(detailPanel.getHeight() + 40);
        slide.setOnFinished(e -> {
            detailPanel.setVisible(false);
            detailPanel.setManaged(false);
            detailBackdrop.setVisible(false);
            detailBackdrop.setManaged(false);
            crimeListView.getSelectionModel().clearSelection();
            crimeTable.getSelectionModel().clearSelection();
        });
        slide.play();
    }

    // Helper function to refresh crime table with updated crime data
    private void refreshList() {
        int selectedIndex = crimeTable.getSelectionModel().getSelectedIndex();
        crimeTable.getItems().setAll(dao.getAllCrimes().stream().filter(c -> !c.isActioned()).toList());
        if (selectedIndex >= 0) {
            crimeTable.getSelectionModel().select(selectedIndex);
        }
        // Keep list view in sync with table
        if (crimeListView != null) {
            crimeListView.getItems().setAll(crimeTable.getItems());
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

        actionedColumn.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().isActioned() ? "Police Dispatched" : "Pending"));
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

    /**
     * Returns a human-readable relative time string from a LocalDateTime.
     * e.g. "Today", "1 day ago", "3 days ago"
     */
    private String getRelativeTime(LocalDateTime timestamp) {
        long daysAgo = ChronoUnit.DAYS.between(timestamp.toLocalDate(), LocalDate.now());
        if (daysAgo == 0) return "Today";
        if (daysAgo == 1) return "1 day ago";
        return daysAgo + " days ago";
    }

    /**
     * Geocodes all crimes in the background when the list loads.
     * Results are stored in addressCache so cells display addresses immediately.
     * Each crime is geocoded one at a time to avoid Nominatim rate limiting.
     */
    private void preloadAddresses() {
        List<CrimeRecord> crimes = new ArrayList<>(crimeTable.getItems());
        new Thread(() -> {
            for (CrimeRecord crime : crimes) {
                if (!addressCache.containsKey(crime.getId())) {
                    try {
                        String address = geocoder.reverseGeocode(
                                crime.getLatitude(), crime.getLongitude());
                        String[] parts = address.split(",");
                        String shortAddress = parts.length >= 2
                                ? parts[0].trim() + ", " + parts[1].trim()
                                : address;
                        addressCache.put(crime.getId(), shortAddress);
                        Platform.runLater(() -> crimeListView.refresh());
                    } catch (Exception e) {
                        // Leave as coordinates if geocoding fails for this entry
                    }
                }
            }
        }).start();
    }

    /**
     * Sets up the styled ListView with custom cells.
     * Selection on the ListView mirrors to the hidden TableView so all
     * existing controller logic continues to work unchanged.
     */
    private void setupListView() {
        crimeListView.getItems().setAll(crimeTable.getItems());

        crimeListView.setCellFactory(lv -> new ListCell<CrimeRecord>() {

            {
                // Style the cell via CSS class so JavaFX handles hover state natively
                // This avoids glitches from manual mouse event handlers during fast scrolling
                getStyleClass().add("crime-list-cell");
            }

            @Override
            protected void updateItem(CrimeRecord crime, boolean empty) {
                super.updateItem(crime, empty);

                if (empty || crime == null) {
                    setGraphic(null);
                    setText(null);
                    getStyleClass().remove("crime-list-cell-populated");
                    return;
                }

                getStyleClass().add("crime-list-cell-populated");

                String dotColor = switch (crime.getCategory().getSeverity()) {
                    case CRITICAL -> "#DC143C";
                    case MEDIUM   -> "#FF8C00";
                    default       -> "#FFD700";
                };

                Label dot = new Label("●");
                dot.setStyle("-fx-text-fill: " + dotColor + "; -fx-font-size: 14px;");

                Label category = new Label(crime.getCategory().toString());
                category.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1A1A2E;");

                String statusText = crime.isActioned() ? "Police Dispatched" : "Pending";
                Label status = new Label(statusText);
                status.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");

                String locationText = addressCache.containsKey(crime.getId())
                        ? addressCache.get(crime.getId())
                        : String.format("%.4f, %.4f", crime.getLatitude(), crime.getLongitude());

                Label location = new Label(locationText);
                location.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");

                VBox textBlock = new VBox(2, category, status, location);

                Label time = new Label(getRelativeTime(crime.getTimestamp()));
                time.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox row = new HBox(10, dot, textBlock, spacer, time);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 12 20 12 20; -fx-cursor: hand;");

                setGraphic(row);
            }
        });

        // Selecting a list row mirrors to the table and opens the detail panel.
        // Save button is hidden for public users viewing existing records.
        crimeListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                crimeTable.getSelectionModel().select(newVal);
                showDetailPanel(false);
            }
        });

        crimeListView.setStyle("-fx-background-color: transparent; " +
                "-fx-background: transparent; -fx-border-width: 0;");
    }

    /**
     * Shows the detail panel as a bottom sheet that slides up.
     * showSave determines whether the Save Changes button is visible:
     *   true when creating a new report (any user type)
     *   false when a public user is viewing an existing report
     *   always true for police users regardless of showSave
     */
    private void showDetailPanel(boolean showSave) {
        boolean canSave = UserSession.isPolice() || showSave;
        saveBtn.setVisible(canSave);
        saveBtn.setManaged(canSave);

        // Show backdrop
        detailBackdrop.setVisible(true);
        detailBackdrop.setManaged(true);

        // Start panel offscreen below and slide up
        detailPanel.setVisible(true);
        detailPanel.setManaged(true);
        detailPanel.setTranslateY(600);

        TranslateTransition slide = new TranslateTransition(Duration.millis(320), detailPanel);
        slide.setToY(0);
        slide.play();
    }

    /**
     * Populates the form fields with data from the selected crime record.
     * Also performs reverse geocoding to display a readable address instead of coordinates.
     */
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

        // Use cached address if already geocoded, otherwise geocode and cache.
        // After caching, refresh the list view so the cell picks up the address.
        if (addressCache.containsKey(crime.getId())) {
            locationField.setText(addressCache.get(crime.getId()));
        } else {
            new Thread(() -> {
                try {
                    String address = geocoder.reverseGeocode(crime.getLatitude(), crime.getLongitude());
                    String[] parts = address.split(",");
                    String shortAddress = parts.length >= 2
                            ? parts[0].trim() + ", " + parts[1].trim()
                            : address;
                    addressCache.put(crime.getId(), shortAddress);
                    Platform.runLater(() -> {
                        locationField.setText(address); // full address in detail panel
                        crimeListView.refresh();        // update list cell with short address
                    });
                } catch (Exception e) {
                    Platform.runLater(() ->
                            locationField.setText(String.format("%.4f, %.4f",
                                    crime.getLatitude(), crime.getLongitude()))
                    );
                }
            }).start();
        }

        descriptionArea.setText(crime.getDescription());
        reporterField.setText(crime.getReporterDisplayName());

        setFormEditable(crime.getId() == 0);
        isCreatingNew = (crime.getId() == 0);
    }

    // Helper method to create CrimeRecord object from form data
    private CrimeRecord createRecordFromForm(CrimeRecord original) throws Exception {
        // Capture hour, minute, ap/pm values from UI
        int hour = Integer.parseInt(hourBox.getValue());
        int min = Integer.parseInt(minuteBox.getValue());
        String ampm = ampmBox.getValue();

        // Convert back to 24h format for LocalDateTime
        if (ampm.equals("PM") && hour < 12) hour += 12;
        if (ampm.equals("AM") && hour == 12) hour = 0;

        LocalDateTime newTimestamp = LocalDateTime.of(datePicker.getValue(), LocalTime.of(hour, min));

        // Parse coordinates using regex to handle spaces automatically
        String address = locationField.getText().trim();

        if (address.isEmpty()) {
            throw new IllegalArgumentException("Please enter an address.");
        }

        double[] coords = geocoder.geocodeAddress(address);
        double lat = coords[0];
        double lon = coords[1];

        System.out.println("Address entered: " + address);
        System.out.println("Resolved coordinates: " + lat + ", " + lon);

        // Bundle everything into the updated object
        return new CrimeRecord(
                original.getId(),
                categoryComboBox.getValue(),
                newTimestamp,
                lat,
                lon,
                descriptionArea.getText(),
                original.getReporter(),
                original.isActioned() // Preserve original raw reporter data (username/null)
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
        setFormEditable(true);
    }

    private void setFormEditable(boolean editable) {
        categoryComboBox.setDisable(!editable);
        datePicker.setDisable(!editable);
        hourBox.setDisable(!editable);
        minuteBox.setDisable(!editable);
        ampmBox.setDisable(!editable);

        locationField.setEditable(editable);
        descriptionArea.setEditable(editable);

        if (!editable) {
            locationField.setStyle("-fx-opacity: 1; -fx-background-color: #f4f4f4; -fx-text-fill: black;");
            descriptionArea.setStyle("-fx-opacity: 1; -fx-background-color: #f4f4f4; -fx-text-fill: black;");
        } else {
            locationField.setStyle("");
            descriptionArea.setStyle("");
        }
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

    /**
     * Initializes address autocomplete for the location input, dynamically retrieving
     * suggestions as the user types. Functionality is restricted to report creation mode
     * to improve usability and prevent unnecessary interactions during viewing.
     */
    private void setupAddressAutocomplete() {
        locationField.textProperty().addListener((obs, oldText, newText) -> {

            if (!isCreatingNew) {
                suggestionsPopup.hide();
                return;
            }

            suggestionDelay.stop();

            if (newText == null || newText.trim().length() < 3) {
                suggestionsPopup.hide();
                return;
            }

            suggestionDelay.setOnFinished(event -> fetchSuggestions(newText.trim()));
            suggestionDelay.playFromStart();
        });

        locationField.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (!focused) {
                suggestionsPopup.hide();
            }
        });
    }

    private void fetchSuggestions(String query) {
        new Thread(() -> {
            try {
                List<String> suggestions = geocoder.getAddressSuggestions(query);
                Platform.runLater(() -> showSuggestions(suggestions));
            } catch (Exception e) {
                Platform.runLater(suggestionsPopup::hide);
            }
        }).start();
    }

    private void showSuggestions(List<String> suggestions) {
        suggestionsPopup.getItems().clear();

        if (suggestions == null || suggestions.isEmpty()) {
            suggestionsPopup.hide();
            return;
        }

        for (String suggestion : suggestions) {
            Label entryLabel = new Label(suggestion);
            entryLabel.setWrapText(true);
            entryLabel.setMaxWidth(350);

            CustomMenuItem item = new CustomMenuItem(entryLabel, true);
            item.setOnAction(e -> {
                locationField.setText(suggestion);
                suggestionsPopup.hide();
            });

            suggestionsPopup.getItems().add(item);
        }

        if (!suggestionsPopup.isShowing()) {
            suggestionsPopup.show(locationField, Side.BOTTOM, 0, 0);
        }
    }
}
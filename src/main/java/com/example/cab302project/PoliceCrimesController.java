package com.example.cab302project;

import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Controller for the Police Crime Reports screen (Police-crimes-view.fxml).
 *
 * Provides police officers with a full crime management interface. A styled
 * {@link ListView} lists all crime records with severity indicators. Selecting
 * a record slides up a detail panel where the officer can edit the record,
 * save changes, or mark it as dealt with. A hidden {@link TableView} is retained
 * for internal data management; all visible display is driven by the ListView
 *
 * Also supports creating new crime reports and includes address autocomplete
 * via {@link IGeocodingService} when the officer is entering a location.
 */
public class PoliceCrimesController {

    @FXML private TableView<CrimeRecord> crimeTable;
    @FXML private TableColumn<CrimeRecord, Integer> idColumn;
    @FXML private TableColumn<CrimeRecord, CrimeCategory> categoryColumn;
    @FXML private TableColumn<CrimeRecord, String> severityColumn;
    @FXML private TableColumn<CrimeRecord, String> timestampColumn;
    @FXML private TableColumn<CrimeRecord, String> actionedColumn;
    @FXML private ListView<CrimeRecord> crimeListView;
    @FXML private VBox detailPanel;
    @FXML private Pane detailBackdrop;
    @FXML private Button saveBtn;
    @FXML private Button markDealtBtn;
    @FXML private ComboBox<CrimeCategory> categoryComboBox;
    @FXML private Label idLabel;
    @FXML private Label severityLabel;
    @FXML private Label actionedStatusLabel;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> hourBox;
    @FXML private ComboBox<String> minuteBox;
    @FXML private ComboBox<String> ampmBox;
    @FXML private TextField reporterField;
    @FXML private TextField locationField;
    @FXML private TextArea descriptionArea;
    @FXML private NavBarController navBarController;
    @FXML private Button hamburgerBtn;
    @FXML private StackPane policeCrimesRoot;

    private PoliceHamburgerMenu hamburgerMenu;

    /**
     * Cache of reverse-geocoded addresses keyed by crime record ID.
     * Avoids redundant network calls when the same record is viewed multiple times.
     */
    private final java.util.Map<Integer, String> addressCache = new java.util.HashMap<>();

    private IAppDAO dao;
    private IGeocodingService geocoder = new OpenStreetMapGeoCoder();
    @FXML
    private final ContextMenu suggestionsPopup = new ContextMenu();

    /**
     * Debounce timer that delays geocoding requests until the user pauses typing,
     * reducing unnecessary API calls during fast input.
     */
    private final PauseTransition suggestionDelay = new PauseTransition(Duration.millis(400));

    private boolean isCreatingNew = false;

    /**
     * Constructs a new PoliceCrimesController and initialises
     * the DAO from the main application database instance.
     */
    public PoliceCrimesController() {
        this.dao = HelloApplication.DATABASE;
    }

    /**
     * Initialises the screen after the FXML has loaded.
     *
     * Sets up table columns, date/time controls, category dropdown,
     * change listeners, loads data, wires up the styled ListView, configures
     * address autocomplete, and attaches the police hamburger menu overlay.
     */
    @FXML
    public void initialize() {
        // Initialize table columns
        setupTableColumns();

        // Mark Crimes tab as active in bottom nav
        if (navBarController != null) {
            navBarController.setActiveTab("crimes");
        }

        // Initialize date and time UI elements
        setupDateTimeControls();

        // Set dropdown values
        categoryComboBox.getItems().setAll(CrimeCategory.values());

        // Auto-update severity label when the category selection changes
        categoryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) severityLabel.setText(newVal.getSeverity().toString());
        });

        // Populate form fields whenever a different row is selected in the table
        crimeTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                populateForm(newVal);
            } else {
                clearForm();
            }
        });

        // Load and Display Data
        refreshList();
        // Update selected list element after populating
        updateSelectionAfterChange();
        // Wire up styled ListView
        setupListView();
        // Initialize address autocomplete suggestions for location input
        setupAddressAutocomplete();

        // Wire police hamburger menu after scene is attached
        // Platform.runLater ensures getScene().getWindow() is not null
        Platform.runLater(() -> {
            Stage stage = (Stage) hamburgerBtn.getScene().getWindow();
            hamburgerMenu = new PoliceHamburgerMenu(stage);
            hamburgerMenu.setMaxWidth(Double.MAX_VALUE);
            hamburgerMenu.setMaxHeight(Double.MAX_VALUE);
            policeCrimesRoot.getChildren().add(hamburgerMenu);
            hamburgerBtn.setOnAction(e -> hamburgerMenu.toggle());
        });
    }

    /**
     * Handles saving a crime report based on current form input.
     *
     * If the selected record has an ID of 0 it is treated as a new record
     * and added to the database. Existing records with a valid ID are updated.
     * Displays a success or error alert to confirm the outcome.
     */
    @FXML
    public void onSave() {
        CrimeRecord selected = crimeTable.getSelectionModel().getSelectedItem();

        // Avoid writing null to database
        if (selected == null) return;

        try {
            CrimeRecord recordFromForm = createRecordFromForm(selected);

            if (selected.getId() == 0) {
                // CASE: This is a brand new record
                if (dao.addCrime(recordFromForm)) {
                    UIUtils.showAlert(Alert.AlertType.INFORMATION, "Success", "New crime reported successfully.");
                    refreshList();
                    updateSelectionAfterChange();
                }
            } else {
                // Police can update existing crimes
                if (dao.updateCrime(recordFromForm)) {
                    UIUtils.showAlert(Alert.AlertType.INFORMATION, "Updated", "Crime report updated successfully.");
                    refreshList();
                    updateSelectionAfterChange();
                } else {
                    UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Could not update crime.");
                }
            }
        } catch (Exception e) {
            UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Could not save: " + e.getMessage());
        }
    }

    /**
     * Creates a blank crime record template and adds it to the list,
     * prompting the officer to fill in the details before saving.
     *
     * Defaults to the Brisbane CBD coordinates and the current timestamp.
     * Warns the officer if an unsaved new record already exists.
     */
    @FXML
    public void onAddNewCrime() {
        // Check if there is a pending new CrimeRecord waiting to be saved
        if (crimeTable.getItems().stream().anyMatch(c -> c.getId() == 0)) {
            UIUtils.showAlert(Alert.AlertType.WARNING, "Pending Report",
                    "Please save or refresh before creating another new report.");
            return;
        }

        isCreatingNew = true;
        CrimeRecord newRecord = new CrimeRecord(
                0,
                CrimeCategory.OTHER,
                LocalDateTime.now(),
                -27.4709,  // Default latitude: Brisbane CBD
                153.0235,  // Default longitude: Brisbane CBD
                "",
                UserSession.getInstance().getUser().getUsername(),
                false
        );

        // Add template to the table and select it so the form populates
        crimeTable.getItems().add(newRecord);
        crimeTable.getSelectionModel().select(newRecord);
        crimeTable.scrollTo(newRecord);

        UIUtils.showAlert(Alert.AlertType.INFORMATION, "New Report",
                "A new blank report has been created. Fill in the details and click 'Save Changes'.");
    }

    /**
     * Navigates back to the dashboard screen.
     * Used if the back button is still present in the FXML.
     */
    @FXML
    public void onBackButtonClick() {
        Stage stage = (Stage) crimeTable.getScene().getWindow();
        UIUtils.switchScene(stage, "dashboard-view.fxml");
    }

    /**
     * Refreshes both the hidden table view and the visible list view with
     * the latest data from the database, preserving the current selection index.
     */
    private void refreshList() {
        int selectedIndex = crimeTable.getSelectionModel().getSelectedIndex();
        crimeTable.getItems().setAll(dao.getAllCrimes());
        if (selectedIndex >= 0) {
            crimeTable.getSelectionModel().select(selectedIndex);
        }
        if (crimeListView != null) {
            crimeListView.getItems().setAll(crimeTable.getItems());
        }
    }

    /**
     * Configures the cell factory for the crime list view.
     *
     * Each cell renders a severity colour dot, crime category, dispatch status,
     * geocoded location, and relative timestamp. Selecting a cell syncs the
     * hidden table selection and slides up the detail panel.
     */
    private void setupListView() {
        crimeListView.getItems().setAll(crimeTable.getItems());

        crimeListView.setCellFactory(lv -> new ListCell<CrimeRecord>() {
            {
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

                // Choose dot colour based on severity tier
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

                // Show cached address if available, otherwise show raw coordinates
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

        crimeListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                crimeTable.getSelectionModel().select(newVal);
                showDetailPanel();
            }
        });

        crimeListView.setStyle("-fx-background-color: transparent; " +
                "-fx-background: transparent; -fx-border-width: 0;");
    }

    /**
     * Makes the detail panel visible and animates it sliding up from the bottom of the screen.
     * The semi-transparent backdrop is also shown behind it.
     */
    private void showDetailPanel() {
        detailBackdrop.setVisible(true);
        detailBackdrop.setManaged(true);
        detailPanel.setVisible(true);
        detailPanel.setManaged(true);
        detailPanel.setTranslateY(600);
        TranslateTransition slide = new TranslateTransition(Duration.millis(320), detailPanel);
        slide.setToY(0);
        slide.play();
    }

    /**
     * Closes the detail panel by animating it sliding back down off-screen.
     * Clears the list selection and hides the backdrop once the animation completes.
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
        });
        slide.play();
    }

    /**
     * Returns a human-readable relative time string for the given timestamp.
     *
     * @param dt the timestamp to describe
     * @return a string such as {@code "5m ago"}, {@code "2h ago"}, or {@code "3d ago"}
     */
    private String getRelativeTime(LocalDateTime dt) {
        long mins = java.time.Duration.between(dt, LocalDateTime.now()).toMinutes();
        if (mins < 60) return mins + "m ago";
        long hrs = mins / 60;
        if (hrs < 24) return hrs + "h ago";
        return (hrs / 24) + "d ago";
    }

    /**
     * Configures the hidden table view's column cell value factories.
     * Severity and timestamp columns use custom string converters for display formatting.
     */
    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));

        // Derive severity string from the category's severity enum
        severityColumn.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getCategory().getSeverity().toString()));

        // Format LocalDateTime as a readable display string
        timestampColumn.setCellValueFactory(cd ->
                new SimpleStringProperty(UIUtils.formatLocalDateTime(cd.getValue().getTimestamp())));

        actionedColumn.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().isActioned() ? "Police Dispatched" : "Pending")
        );
    }

    /**
     * Populates the hour, minute, and AM/PM dropdowns with their valid options
     * and sets default values of 12:00 PM.
     */
    private void setupDateTimeControls() {
        // Hours 1-12
        for (int i = 1; i <= 12; i++) hourBox.getItems().add(String.format("%02d", i));

        // Minutes in 15-minute increments
        minuteBox.getItems().addAll("00", "15", "30", "45");

        // AM/PM
        ampmBox.getItems().addAll("AM", "PM");

        // Default values
        hourBox.setValue("12");
        minuteBox.setValue("00");
        ampmBox.setValue("PM");
    }

    /**
     * Populates the detail form fields with data from the selected crime record.
     *
     * The timestamp is converted from 24-hour to 12-hour format for the
     * hour and AM/PM dropdowns. Location is reverse-geocoded on a background
     * thread to avoid blocking the UI.
     *
     * @param crime the {@link CrimeRecord} whose data should populate the form
     */
    private void populateForm(CrimeRecord crime) {
        idLabel.setText(String.valueOf(crime.getId()));

        LocalDateTime dt = crime.getTimestamp();
        categoryComboBox.setValue(crime.getCategory());
        datePicker.setValue(dt.toLocalDate());

        // Set AM/PM based on 24-hour value
        int hour = dt.getHour();
        ampmBox.setValue(hour >= 12 ? "PM" : "AM");

        // Convert to 12-hour display format
        int displayHour = (hour % 12 == 0) ? 12 : hour % 12;
        hourBox.setValue(String.format("%02d", displayHour));

        // Round minutes down to nearest 15-minute interval
        int mins = dt.getMinute();
        minuteBox.setValue(String.format("%02d", (mins / 15) * 15));

        // Reverse geocode coordinates to address on a background thread
        new Thread(() -> {
            try {
                String address = geocoder.reverseGeocode(crime.getLatitude(), crime.getLongitude());
                Platform.runLater(() -> locationField.setText(address));
            } catch (Exception e) {
                Platform.runLater(() ->
                        locationField.setText(String.format("%.4f, %.4f",
                                crime.getLatitude(), crime.getLongitude()))
                );
            }
        }).start();

        descriptionArea.setText(crime.getDescription());
        reporterField.setText(crime.getReporterDisplayName());
        actionedStatusLabel.setText(crime.isActioned() ? "Police Dispatched" : "Pending");

        setFormEditable(true);
        isCreatingNew = (crime.getId() == 0);
    }

    /**
     * Builds a {@link CrimeRecord} from the current form field values.
     *
     * Converts the 12-hour time selection back to 24-hour format, geocodes
     * the entered address string to coordinates, and assembles a new record
     * preserving the original ID and reporter from the existing record.
     *
     * @param original the existing record being edited, used to preserve immutable fields
     * @return a new {@link CrimeRecord} populated with the form data
     * @throws Exception if the address field is empty or geocoding fails
     */
    private CrimeRecord createRecordFromForm(CrimeRecord original) throws Exception {
        int hour = Integer.parseInt(hourBox.getValue());
        int min = Integer.parseInt(minuteBox.getValue());
        String ampm = ampmBox.getValue();

        // Convert back to 24-hour format
        if (ampm.equals("PM") && hour < 12) hour += 12;
        if (ampm.equals("AM") && hour == 12) hour = 0;

        LocalDateTime newTimestamp = LocalDateTime.of(datePicker.getValue(), LocalTime.of(hour, min));

        String address = locationField.getText().trim();
        if (address.isEmpty()) {
            throw new IllegalArgumentException("Please enter an address.");
        }

        double[] coords = geocoder.geocodeAddress(address);
        double lat = coords[0];
        double lon = coords[1];

        System.out.println("Address entered: " + address);
        System.out.println("Resolved coordinates: " + lat + ", " + lon);

        return new CrimeRecord(
                original.getId(),
                categoryComboBox.getValue(),
                newTimestamp,
                lat,
                lon,
                descriptionArea.getText(),
                original.getReporter(),
                original.isActioned()
        );
    }

    /**
     * Clears all form fields and resets labels to their default placeholder values.
     * Called when no record is selected in the table.
     */
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
        actionedStatusLabel.setText("-");
        setFormEditable(true);
    }

    /**
     * Enables or disables all editable form fields.
     *
     * When {@code editable} is {@code false}, text fields are styled
     * with a grey background to visually indicate they are read-only.
     *
     * @param editable {@code true} to make fields editable, {@code false} to lock them
     */
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

    /**
     * Selects the first item in the table after a refresh or deletion,
     * or clears the form if no items remain.
     */
    private void updateSelectionAfterChange() {
        if (!crimeTable.getItems().isEmpty()) {
            crimeTable.getSelectionModel().selectFirst();
        } else {
            clearForm();
        }
    }

    /**
     * Sets up real-time address autocomplete for the location text field.
     *
     * A {@link PauseTransition} debounces input so suggestions are only
     * fetched after the user stops typing for 400ms. Autocomplete is suppressed
     * when not in new-record creation mode to avoid unnecessary API calls
     * while browsing existing records.
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

    /**
     * Fetches address suggestions for the given query string on a background thread
     * and updates the suggestions popup on the JavaFX thread when results arrive.
     *
     * @param query the partial address string to search for
     */
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

    /**
     * Populates and shows the suggestions popup with the provided list of addresses.
     * Each item, when clicked, fills the location field and hides the popup.
     * Hides the popup if the suggestions list is null or empty.
     *
     * @param suggestions the list of address strings to display as menu items
     */
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

    /**
     * Marks the currently selected crime record as dealt with by police.
     *
     * Sets the record's actioned flag to {@code true}, persists the change
     * to the database, and refreshes the list. Displays a warning if no
     * record is selected, or an error if the database update fails.
     */
    @FXML
    public void onMarkAsDealt() {
        CrimeRecord selected = crimeTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            UIUtils.showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a crime.");
            return;
        }

        selected.setActioned(true);

        if (dao.updateCrime(selected)) {
            UIUtils.showAlert(Alert.AlertType.INFORMATION, "Updated", "Crime marked as dealt with.");
            refreshList();
            updateSelectionAfterChange();
        } else {
            UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Could not update crime.");
        }
    }
}
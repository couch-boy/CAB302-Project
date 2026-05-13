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
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Controller for the police crime management screen.
 * Allows police users to view, edit, and mark crime reports as dealt with.
 */
public class PoliceCrimesController {

    // Table used as the main data source for crime records
    @FXML private TableView<CrimeRecord> crimeTable;

    // Table columns for displaying crime attributes
    @FXML private TableColumn<CrimeRecord, Integer> idColumn;
    @FXML private TableColumn<CrimeRecord, CrimeCategory> categoryColumn;
    @FXML private TableColumn<CrimeRecord, String> severityColumn;
    @FXML private TableColumn<CrimeRecord, String> timestampColumn;
    @FXML private TableColumn<CrimeRecord, String> actionedColumn;

    // Styled list view used for UI display (linked to table)
    @FXML private ListView<CrimeRecord> crimeListView;
    @FXML private MenuButton filterMenuButton;
    @FXML private RadioMenuItem severityAllItem, severitySevereItem, severityModerateItem, severityLowItem;
    @FXML private RadioMenuItem crimeTypeAllItem, crimeTypeAssaultItem, crimeTypeTrespassingItem,
            crimeTypeDomesticAbuseItem, crimeTypeHomicideItem;
    @FXML private RadioMenuItem statusAllItem, statusPendingItem, statusActionedItem;
    @FXML private RadioMenuItem dateAllItem, dateTodayItem, dateLast7DaysItem, dateLast30DaysItem;

    // Detail panel and backdrop for viewing/editing a crime
    @FXML private VBox detailPanel;
    @FXML private Pane detailBackdrop;

    // Buttons for saving and marking crimes as dealt with
    @FXML private Button saveBtn, markDealtBtn;

    // Form fields for editing crime details
    @FXML private ComboBox<CrimeCategory> categoryComboBox;
    @FXML private Label idLabel, severityLabel, actionedStatusLabel;
    @FXML private Label severityDot;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> hourBox, minuteBox, ampmBox;
    @FXML private TextField reporterField, locationField;
    @FXML private TextArea descriptionArea;

    // Navigation and layout elements
    @FXML private NavBarController navBarController;
    @FXML private Button hamburgerBtn;
    @FXML private StackPane policeCrimesRoot;

    // Hamburger menu component for navigation
    private PoliceHamburgerMenu hamburgerMenu;

    // Cache to store resolved addresses for faster UI display
    private final java.util.Map<Integer, String> addressCache = new java.util.HashMap<>();

    // Data access object for database operations
    private IAppDAO dao;
    private List<CrimeRecord> allCrimeRecords = new ArrayList<>();

    // Service used to convert between coordinates and addresses
    private IGeocodingService geocoder = new OpenStreetMapGeoCoder();

    // Popup for address suggestions
    @FXML
    private final ContextMenu suggestionsPopup = new ContextMenu();

    // Delay before triggering autocomplete requests
    private final PauseTransition suggestionDelay = new PauseTransition(Duration.millis(400));

    // Tracks whether the user is creating a new report
    private boolean isCreatingNew = false;

    // Constructor initializes DAO reference
    public PoliceCrimesController() {

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

        // Mark Crimes tab as active in bottom nav
        if (navBarController != null) {
            navBarController.setActiveTab("crimes");
        }

        // Initialize date and time UI elements
        setupDateTimeControls();

        // Set dropdown values
        categoryComboBox.getItems().setAll(CrimeCategory.values());
        setupFilters();

        // Initialize Listener -> Auto-update severity dot colour and label when category changes.
        // This gives the officer immediate visual feedback on the severity tier as they pick a crime type.
        categoryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateSeverityDisplay(newVal);
        });

        // Initialize Listener -> Update displayed CrimeRecord data when there are changes
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
        //  Initialize address autocomplete suggestions for location input
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
                }
            }
            else
            {
                // Police can update existing crimes
                if (dao.updateCrime(recordFromForm)) {
                    UIUtils.showAlert(Alert.AlertType.INFORMATION, "Updated", "Crime report updated successfully.");
                    refreshList();
                    updateSelectionAfterChange();
                }
                else
                {
                    UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Could not update crime.");
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

        // Display popup prompting user to fill in template details and save to store in database
        UIUtils.showAlert(Alert.AlertType.INFORMATION, "New Report",
                "A new blank report has been created. Fill in the details and click 'Save Changes'.");

    }



    /**
     * Return to the previous menu (dashboard)
     */
    @FXML
    public void onBackButtonClick() {
        Stage stage = (Stage) crimeTable.getScene().getWindow();
        UIUtils.switchScene(stage, "dashboard-view.fxml");
    }

    /**
     * Refreshes crime data from database and updates both table and list view.
     */
    private void refreshList() {
        int selectedIndex = crimeTable.getSelectionModel().getSelectedIndex();
        allCrimeRecords = dao.getAllCrimes();
        applyFilters();
        if (selectedIndex >= 0) {
            crimeTable.getSelectionModel().select(selectedIndex);
        }
    }

    /**
     * Groups filter menu options so one item can be active in each section.
     */
    private void setupFilters() {
        ToggleGroup severityGroup = new ToggleGroup();
        severityAllItem.setToggleGroup(severityGroup);
        severitySevereItem.setToggleGroup(severityGroup);
        severityModerateItem.setToggleGroup(severityGroup);
        severityLowItem.setToggleGroup(severityGroup);

        ToggleGroup crimeTypeGroup = new ToggleGroup();
        crimeTypeAllItem.setToggleGroup(crimeTypeGroup);
        crimeTypeAssaultItem.setToggleGroup(crimeTypeGroup);
        crimeTypeTrespassingItem.setToggleGroup(crimeTypeGroup);
        crimeTypeDomesticAbuseItem.setToggleGroup(crimeTypeGroup);
        crimeTypeHomicideItem.setToggleGroup(crimeTypeGroup);

        ToggleGroup statusGroup = new ToggleGroup();
        statusAllItem.setToggleGroup(statusGroup);
        statusPendingItem.setToggleGroup(statusGroup);
        statusActionedItem.setToggleGroup(statusGroup);

        ToggleGroup dateGroup = new ToggleGroup();
        dateAllItem.setToggleGroup(dateGroup);
        dateTodayItem.setToggleGroup(dateGroup);
        dateLast7DaysItem.setToggleGroup(dateGroup);
        dateLast30DaysItem.setToggleGroup(dateGroup);
    }

    /**
     * Handles changes from any filter menu option.
     */
    @FXML
    public void onFilterChanged() {
        applyFilters();
        updateFilterButtonText();
    }

    /**
     * Applies the selected filter values to the table and styled list view.
     */
    private void applyFilters() {
        List<CrimeRecord> filteredCrimes = allCrimeRecords.stream()
                .filter(this::matchesSeverityFilter)
                .filter(this::matchesCrimeTypeFilter)
                .filter(this::matchesStatusFilter)
                .filter(this::matchesDateFilter)
                .toList();

        crimeTable.getItems().setAll(filteredCrimes);
        if (crimeListView != null) {
            crimeListView.getItems().setAll(filteredCrimes);
        }
    }

    private boolean matchesSeverityFilter(CrimeRecord crime) {
        if (severitySevereItem.isSelected()) {
            return crime.getCategory().getSeverity() == CrimeCategory.Severity.CRITICAL;
        }
        if (severityModerateItem.isSelected()) {
            return crime.getCategory().getSeverity() == CrimeCategory.Severity.MEDIUM;
        }
        if (severityLowItem.isSelected()) {
            return crime.getCategory().getSeverity() == CrimeCategory.Severity.LOW;
        }
        return true;
    }

    private boolean matchesCrimeTypeFilter(CrimeRecord crime) {
        if (crimeTypeAssaultItem.isSelected()) {
            return crime.getCategory() == CrimeCategory.ASSAULT;
        }
        if (crimeTypeTrespassingItem.isSelected()) {
            return crime.getCategory() == CrimeCategory.TRESPASSING;
        }
        if (crimeTypeDomesticAbuseItem.isSelected()) {
            return crime.getCategory() == CrimeCategory.DOMESTICABUSE;
        }
        if (crimeTypeHomicideItem.isSelected()) {
            return crime.getCategory() == CrimeCategory.HOMICIDE;
        }
        return true;
    }

    private boolean matchesStatusFilter(CrimeRecord crime) {
        if (statusPendingItem.isSelected()) {
            return !crime.isActioned();
        }
        if (statusActionedItem.isSelected()) {
            return crime.isActioned();
        }
        return true;
    }

    private boolean matchesDateFilter(CrimeRecord crime) {
        LocalDate crimeDate = crime.getTimestamp().toLocalDate();
        LocalDate today = LocalDate.now();

        if (dateTodayItem.isSelected()) {
            return crimeDate.isEqual(today);
        }
        if (dateLast7DaysItem.isSelected()) {
            return !crimeDate.isBefore(today.minusDays(6)) && !crimeDate.isAfter(today);
        }
        if (dateLast30DaysItem.isSelected()) {
            return !crimeDate.isBefore(today.minusDays(29)) && !crimeDate.isAfter(today);
        }
        return true;
    }

    private void updateFilterButtonText() {
        int activeFilters = 0;
        if (!severityAllItem.isSelected()) activeFilters++;
        if (!crimeTypeAllItem.isSelected()) activeFilters++;
        if (!statusAllItem.isSelected()) activeFilters++;
        if (!dateAllItem.isSelected()) activeFilters++;

        filterMenuButton.setText(activeFilters == 0 ? "Filter" : "Filter (" + activeFilters + ")");
    }

    /**
     * Updates the severity dot colour and label text in the detail panel
     * based on the given crime category. Called whenever the category selection
     * changes so the officer sees immediate visual feedback on the severity tier.
     * @param category the selected CrimeCategory, or null to reset to a blank state
     */
    private void updateSeverityDisplay(CrimeCategory category) {
        if (category == null) {
            if (severityDot   != null) severityDot.setStyle("-fx-text-fill: #D1D5DB; -fx-font-size: 13px;");
            if (severityLabel != null) { severityLabel.setText("-"); severityLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #6B7280;"); }
            return;
        }

        String dotColor = switch (category.getSeverity()) {
            case CRITICAL -> "#DC143C";
            case MEDIUM   -> "#FF8C00";
            default       -> "#FFD700";
        };

        if (severityDot   != null) severityDot.setStyle("-fx-text-fill: " + dotColor + "; -fx-font-size: 13px;");
        if (severityLabel != null) { severityLabel.setText(category.getSeverity().toString()); severityLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + dotColor + ";"); }
    }

    /**
     * Sets up the styled list view and links it to the table data.
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
     * Displays the sliding detail panel for viewing/editing a crime.
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
     * Closes the detail panel with a slide-down animation and clears selection.
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
     * Converts a timestamp into a short relative time string.
     */
    private String getRelativeTime(LocalDateTime dt) {
        long mins = java.time.Duration.between(dt, LocalDateTime.now()).toMinutes();
        if (mins < 60) return mins + "m ago";
        long hrs = mins / 60;
        if (hrs < 24) return hrs + "h ago";
        return (hrs / 24) + "d ago";
    }


    /**
     * Configures table columns to map to CrimeRecord properties.
     */
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
                new SimpleStringProperty(cd.getValue().isActioned() ? "Police Dispatched" : "Pending")
        );
    }


    /**
     * Initializes dropdown values for time selection inputs.
     */
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
     * Populates the form fields with data from the selected crime record.
     * Also performs reverse geocoding to display a readable address instead of coordinates.
     */
    private void populateForm(CrimeRecord crime) {
        // Set ID label
        idLabel.setText(String.valueOf(crime.getId()));

        // Get LocalDateTime object from CrimeRecord
        LocalDateTime dt = crime.getTimestamp();

        categoryComboBox.setValue(crime.getCategory());
        updateSeverityDisplay(crime.getCategory());
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
     * Builds a CrimeRecord object using the current values entered in the form.
     * Converts UI input (time, address) into valid data for storage.
     */
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
                original.getReporter() ,
                original.isActioned()// Preserve original raw reporter data (username/null)

        );
    }

    /**
     * Resets all form fields back to their default empty state.
     */
    private void clearForm() {
        idLabel.setText("-");
        updateSeverityDisplay(null);
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
     * Enables or disables editing of form fields based on user interaction state.
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
     * Updates the current selection after data changes.
     * Selects the first record if available, otherwise clears the form.
     */
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

    /**
     * Fetches address suggestions from the geocoding service in a background thread.
     * Results are passed back to the UI thread to display in the suggestions popup.
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
     * Displays autocomplete suggestions in a dropdown below the location field.
     * Each suggestion can be clicked to populate the input field.
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
     * Marks the selected crime as dealt with and updates the database.
     */
    @FXML
    public void onMarkAsDealt() {
        CrimeRecord selected = crimeTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            UIUtils.showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a crime.");
            return;
        }

        // mark as actioned
        selected.setActioned(true);

        // update database
        if (dao.updateCrime(selected)) {
            UIUtils.showAlert(Alert.AlertType.INFORMATION, "Updated", "Crime marked as dealt with.");

            refreshList(); // reload table
            updateSelectionAfterChange();
        } else {
            UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Could not update crime.");
        }
    }
}

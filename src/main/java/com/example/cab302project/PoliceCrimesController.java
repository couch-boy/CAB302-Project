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

    /**
     * Hidden MenuButton kept in FXML so updateFilterButtonText() does not
     * throw NullPointerException. The visible filter UI is the inline filter bar.
     */
    @FXML private MenuButton filterMenuButton;

    // Hidden RadioMenuItems backing the filter bar chips for severity, status and date.
    // Crime type filtering is handled directly via filterCrimeTypeCombo - see matchesCrimeTypeFilter().
    @FXML private RadioMenuItem severityAllItem, severitySevereItem, severityModerateItem, severityLowItem;
    @FXML private RadioMenuItem crimeTypeAllItem, crimeTypeAssaultItem, crimeTypeTrespassingItem,
            crimeTypeDomesticAbuseItem, crimeTypeHomicideItem;
    @FXML private RadioMenuItem statusAllItem, statusPendingItem, statusActionedItem;
    @FXML private RadioMenuItem dateAllItem, dateTodayItem, dateLast7DaysItem, dateLast30DaysItem;

    // Inline-styled strips - needed for programmatic dark mode override
    @FXML private HBox severityLegendStrip;
    @FXML private HBox sectionHeader;
    @FXML private Label allReportsLabel;

    // Filter bar outer container - needed for dark mode restyle
    @FXML private VBox filterBar;

    // Filter bar always-visible controls
    @FXML private ToggleButton chipSevAll;
    @FXML private ToggleButton chipSevLow;
    @FXML private ToggleButton chipSevModerate;
    @FXML private ToggleButton chipSevSevere;
    @FXML private ComboBox<String> filterCrimeTypeCombo;

    // Expandable advanced filter section and its toggle button
    @FXML private VBox filterBarAdvanced;
    @FXML private Button advancedToggleBtn;

    // Advanced filter controls (inside the collapsible section)
    @FXML private ToggleButton chipStatusAll;
    @FXML private ToggleButton chipStatusPending;
    @FXML private ToggleButton chipStatusActioned;
    @FXML private ComboBox<String> filterDateCombo;

    // Tracks whether the advanced filter section is currently expanded
    private boolean advancedOpen = false;

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
        initFilterBar();

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
            hamburgerMenu.setOnDarkModeChanged(this::refreshDarkMode);
            // Apply dark mode to all inline-styled nodes now that the scene is attached
            applyDarkStrips();
            // Refresh list cells so dark mode text colours apply on first load
            if (crimeListView != null) crimeListView.refresh();
        });
    }

    /**
     * Initialises the compact filter bar above the police crime list.
     *
     * The always-visible row contains severity chips and the crime type dropdown.
     * Status and date range live in the collapsible advanced section.
     *
     * Crime type filtering is performed directly against the category name so that
     * all 21 crime types work, not just the four covered by the legacy RadioMenuItems.
     */
    private void initFilterBar() {
        // Severity chip group
        ToggleGroup sevGroup = new ToggleGroup();
        chipSevAll.setToggleGroup(sevGroup);
        chipSevLow.setToggleGroup(sevGroup);
        chipSevModerate.setToggleGroup(sevGroup);
        chipSevSevere.setToggleGroup(sevGroup);
        chipSevAll.setSelected(true);

        sevGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null) { chipSevAll.setSelected(true); return; }
            styleChips(sevGroup);
            if      (newT == chipSevAll)      severityAllItem.setSelected(true);
            else if (newT == chipSevLow)      severityLowItem.setSelected(true);
            else if (newT == chipSevModerate) severityModerateItem.setSelected(true);
            else if (newT == chipSevSevere)   severitySevereItem.setSelected(true);
            onFilterChanged();
        });

        // Status chip group - police view includes Actioned (inside advanced section)
        ToggleGroup statGroup = new ToggleGroup();
        chipStatusAll.setToggleGroup(statGroup);
        chipStatusPending.setToggleGroup(statGroup);
        chipStatusActioned.setToggleGroup(statGroup);
        chipStatusAll.setSelected(true);

        statGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null) { chipStatusAll.setSelected(true); return; }
            styleChips(statGroup);
            if      (newT == chipStatusAll)      statusAllItem.setSelected(true);
            else if (newT == chipStatusPending)  statusPendingItem.setSelected(true);
            else if (newT == chipStatusActioned) statusActionedItem.setSelected(true);
            onFilterChanged();
        });

        // Crime type ComboBox - populated with every CrimeCategory value.
        // Filtering is done directly in matchesCrimeTypeFilter() against the
        // selected string, bypassing the legacy RadioMenuItem bridge entirely.
        filterCrimeTypeCombo.getItems().add("All");
        for (CrimeCategory cat : CrimeCategory.values()) {
            filterCrimeTypeCombo.getItems().add(cat.getName());
        }
        filterCrimeTypeCombo.setValue("All");
        filterCrimeTypeCombo.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) onFilterChanged();
        });

        // Date range ComboBox (inside the advanced section)
        filterDateCombo.getItems().addAll("All", "Today", "Last 7 Days", "Last 30 Days");
        filterDateCombo.setValue("All");
        filterDateCombo.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;
            switch (newV) {
                case "Today"        -> dateTodayItem.setSelected(true);
                case "Last 7 Days"  -> dateLast7DaysItem.setSelected(true);
                case "Last 30 Days" -> dateLast30DaysItem.setSelected(true);
                default             -> dateAllItem.setSelected(true);
            }
            onFilterChanged();
        });

        // Advanced section starts collapsed
        if (filterBarAdvanced != null) {
            filterBarAdvanced.setVisible(false);
            filterBarAdvanced.setManaged(false);
        }

        // Apply initial chip styling
        styleChips(sevGroup);
        styleChips(statGroup);
    }

    /**
     * Toggles the advanced filter section open or closed and updates the button label.
     * Called by the "More" / "Less" button in the filter bar.
     */
    @FXML
    public void onAdvancedToggle() {
        advancedOpen = !advancedOpen;
        if (filterBarAdvanced != null) {
            filterBarAdvanced.setVisible(advancedOpen);
            filterBarAdvanced.setManaged(advancedOpen);
        }
        if (advancedToggleBtn != null) {
            advancedToggleBtn.setText(advancedOpen ? "Less \u25B4" : "More \u25BE");
        }
    }

    /**
     * Updates the visual style of every {@link ToggleButton} in the given group.
     * The selected chip is filled dark; unselected chips use the muted pill style.
     *
     * @param group the {@link ToggleGroup} whose members should be restyled
     */
    /**
     * Restyles all chips in the given group to reflect their selected state,
     * respecting the current dark mode setting.
     *
     * @param group the {@link ToggleGroup} whose chip buttons should be restyled
     */
    private void styleChips(ToggleGroup group) {
        boolean dark = UserSession.isDarkMode();
        String selected = dark
                ? "-fx-background-color: #4B5563; -fx-text-fill: #F9FAFB; -fx-background-radius: 20; -fx-border-radius: 20; -fx-font-size: 11px; -fx-padding: 5 0 5 0; -fx-cursor: hand; -fx-border-width: 0;"
                : "-fx-background-color: #2A364E; -fx-text-fill: #FFFFFF; -fx-background-radius: 20; -fx-border-radius: 20; -fx-font-size: 11px; -fx-padding: 5 0 5 0; -fx-cursor: hand; -fx-border-width: 0;";
        String unselected = dark
                ? "-fx-background-color: #374151; -fx-text-fill: #D1D5DB; -fx-background-radius: 20; -fx-border-radius: 20; -fx-font-size: 11px; -fx-padding: 5 0 5 0; -fx-cursor: hand; -fx-border-width: 0;"
                : "-fx-background-color: #F3F4F6; -fx-text-fill: #374151; -fx-background-radius: 20; -fx-border-radius: 20; -fx-font-size: 11px; -fx-padding: 5 0 5 0; -fx-cursor: hand; -fx-border-width: 0;";
        for (Toggle t : group.getToggles()) {
            if (t instanceof ToggleButton btn) {
                btn.setStyle(btn.isSelected() ? selected : unselected);
            }
        }
    }

    /**
     * Applies dark mode overrides to all inline-styled nodes on the police crimes screen.
     * Called on first load and whenever dark mode is toggled.
     */
    private void applyDarkStrips() {
        boolean dark = UserSession.isDarkMode();
        String darkStrip   = "-fx-background-color: #1F2937; -fx-border-color: #374151;";
        String darkSection = "-fx-padding: 12 20 8 20; -fx-background-color: #1F2937;";
        String listViewDark = "-fx-background-color: #111827; -fx-background: #111827; -fx-border-width: 0;";
        String lightStrip   = "-fx-background-color: #FFFFFF; -fx-border-color: #E5E7EB;";
        String lightSection = "-fx-padding: 12 20 8 20; -fx-background-color: #F8F9FA;";

        if (dark) {
            if (severityLegendStrip != null) severityLegendStrip.setStyle(darkStrip + " -fx-padding: 8 20 8 20; -fx-border-width: 0 0 1 0;");
            if (sectionHeader       != null) sectionHeader.setStyle(darkSection);
            if (allReportsLabel     != null) allReportsLabel.setStyle("-fx-text-fill: #F9FAFB; -fx-font-size: 13px; -fx-font-weight: bold;");
            if (crimeListView       != null) crimeListView.setStyle(listViewDark);
        } else {
            if (severityLegendStrip != null) severityLegendStrip.setStyle(lightStrip + " -fx-padding: 8 20 8 20; -fx-border-width: 0 0 1 0;");
            if (sectionHeader       != null) sectionHeader.setStyle(lightSection);
            if (allReportsLabel     != null) allReportsLabel.setStyle("-fx-text-fill: #1A1A2E; -fx-font-size: 13px; -fx-font-weight: bold;");
            if (crimeListView       != null) crimeListView.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-width: 0;");
        }
        applyDarkFilterBar(dark);
    }

    /**
     * Called by the hamburger menu when the user toggles dark mode at runtime.
     * Re-applies all dark or light overrides and refreshes the list cells.
     */
    private void refreshDarkMode() {
        applyDarkStrips();
        if (crimeListView != null) crimeListView.refresh();
    }

    /**
     * Applies or removes dark mode styling on the inline-styled filter bar controls.
     * Called from initialize() after initFilterBar(), so the bar is correctly themed
     * when the screen first opens in dark mode.
     *
     * @param dark true to apply dark styles, false to restore light styles
     */
    private void applyDarkFilterBar(boolean dark) {
        String filterBarBg = dark
                ? "-fx-background-color: #1F2937; -fx-border-color: #374151; -fx-border-width: 0 0 1 0; -fx-padding: 8 16 8 16;"
                : "-fx-background-color: #FFFFFF; -fx-border-color: #E5E7EB; -fx-border-width: 0 0 1 0; -fx-padding: 8 16 8 16;";
        String chipSel = dark
                ? "-fx-background-color: #4B5563; -fx-text-fill: #F9FAFB; -fx-background-radius: 20; -fx-border-radius: 20; -fx-font-size: 11px; -fx-padding: 5 0 5 0; -fx-cursor: hand; -fx-border-width: 0;"
                : "-fx-background-color: #2A364E; -fx-text-fill: #FFFFFF; -fx-background-radius: 20; -fx-border-radius: 20; -fx-font-size: 11px; -fx-padding: 5 0 5 0; -fx-cursor: hand; -fx-border-width: 0;";
        String chipUnsel = dark
                ? "-fx-background-color: #374151; -fx-text-fill: #D1D5DB; -fx-background-radius: 20; -fx-border-radius: 20; -fx-font-size: 11px; -fx-padding: 5 0 5 0; -fx-cursor: hand; -fx-border-width: 0;"
                : "-fx-background-color: #F3F4F6; -fx-text-fill: #374151; -fx-background-radius: 20; -fx-border-radius: 20; -fx-font-size: 11px; -fx-padding: 5 0 5 0; -fx-cursor: hand; -fx-border-width: 0;";
        String comboDark  = "-fx-background-color: #374151; -fx-control-inner-background: #374151; -fx-text-fill: #F9FAFB; -fx-border-color: #4B5563; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 11px;";
        String comboLight = "-fx-background-color: #F3F4F6; -fx-border-color: #E5E7EB; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 11px;";
        String btnDark  = "-fx-background-color: #374151; -fx-text-fill: #D1D5DB; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-width: 0; -fx-background-radius: 20; -fx-padding: 5 12 5 12;";
        String btnLight = "-fx-background-color: #F3F4F6; -fx-text-fill: #6B7280; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-width: 0; -fx-background-radius: 20; -fx-padding: 5 12 5 12;";

        if (filterBar            != null) filterBar.setStyle(filterBarBg);
        if (advancedToggleBtn    != null) advancedToggleBtn.setStyle(dark ? btnDark : btnLight);
        if (filterCrimeTypeCombo != null) filterCrimeTypeCombo.setStyle(dark ? comboDark : comboLight);
        if (filterDateCombo      != null) filterDateCombo.setStyle(dark ? comboDark : comboLight);

        for (ToggleButton btn : new ToggleButton[]{chipSevAll, chipSevLow, chipSevModerate, chipSevSevere,
                chipStatusAll, chipStatusPending, chipStatusActioned}) {
            if (btn != null) btn.setStyle(btn.isSelected() ? chipSel : chipUnsel);
        }
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

        allCrimeRecords = dao.getAllCrimes().stream()
                .filter(c -> !c.isActioned())
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp())) // NEWEST FIRST
                .toList();

        applyFilters();
        if (selectedIndex >= 0) {
            crimeTable.getSelectionModel().select(selectedIndex);
        }

        // Preload addresses for list view display
        preloadAddresses(allCrimeRecords);
    }

    /**
     * Preloads human-readable addresses for all provided crime records in a background thread.
     *
     * Addresses are stored in {@link #addressCache} and the list view is
     * refreshed on the JavaFX thread each time a new address is resolved, so
     * cells update progressively without blocking the UI.
     *
     * @param crimes the list of crime records whose coordinates should be geocoded
     */
    private void preloadAddresses(List<CrimeRecord> crimes) {
        new Thread(() -> {
            for (CrimeRecord crime : crimes) {
                if (!addressCache.containsKey(crime.getId())) {
                    try {
                        String address = geocoder.reverseGeocode(
                                crime.getLatitude(), crime.getLongitude());
                        // Shorten to first two comma-separated parts for display
                        String[] parts = address.split(",");
                        String shortAddress = parts.length >= 2
                                ? parts[0].trim() + ", " + parts[1].trim()
                                : address;
                        addressCache.put(crime.getId(), shortAddress);
                        Platform.runLater(() -> {
                            if (crimeListView != null) crimeListView.refresh();
                        });
                    } catch (Exception ignored) {}
                }
            }
        }).start();
    }

    /**
     * Groups the hidden filter RadioMenuItem options so one item can be active in each section.
     * These are still needed because the severity and date filter logic reads from them.
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
     * Handles changes from any filter control.
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

    /**
     * Checks whether a crime record matches the selected crime type in filterCrimeTypeCombo.
     * Filters directly against the category name so all 21 crime types work correctly.
     * Returns true when "All" is selected or the category is null.
     */
    private boolean matchesCrimeTypeFilter(CrimeRecord crime) {
        if (filterCrimeTypeCombo == null) return true;
        String selected = filterCrimeTypeCombo.getValue();
        if (selected == null || selected.equals("All")) return true;
        if (crime.getCategory() == null) return false;
        return crime.getCategory().getName().equals(selected);
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
        String crimeType = filterCrimeTypeCombo != null ? filterCrimeTypeCombo.getValue() : "All";
        if (crimeType != null && !crimeType.equals("All")) activeFilters++;
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

                boolean dark = UserSession.isDarkMode();

                Label category = new Label(crime.getCategory().toString());
                category.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: "
                        + (dark ? "#F9FAFB" : "#1A1A2E") + ";");

                String statusText = crime.isActioned() ? "Police Dispatched" : "Pending";
                Label status = new Label(statusText);
                status.setStyle("-fx-font-size: 11px; -fx-text-fill: "
                        + (dark ? "#9CA3AF" : "#6B7280") + ";");

                String locationText = addressCache.containsKey(crime.getId())
                        ? addressCache.get(crime.getId())
                        : String.format("%.4f, %.4f", crime.getLatitude(), crime.getLongitude());
                Label location = new Label(locationText);
                location.setStyle("-fx-font-size: 11px; -fx-text-fill: "
                        + (dark ? "#6B7280" : "#9CA3AF") + ";");

                VBox textBlock = new VBox(2, category, status, location);

                Label time = new Label(getRelativeTime(crime.getTimestamp()));
                time.setStyle("-fx-font-size: 11px; -fx-text-fill: "
                        + (dark ? "#6B7280" : "#9CA3AF") + ";");

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

        // Inline style must match CSS .dark-mode .list-view background
        crimeListView.setStyle(UserSession.isDarkMode()
                ? "-fx-background-color: #111827; -fx-background: #111827; -fx-border-width: 0;"
                : "-fx-background-color: transparent; -fx-background: transparent; -fx-border-width: 0;");
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
                original.getReporter(),
                original.isActioned() // Preserve original raw reporter data (username/null)
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
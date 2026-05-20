package com.example.cab302project;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller responsible for displaying and managing the user's submitted crime reports.
 * It loads reports specific to the logged-in user, displays them in both list and table views,
 * and provides a detailed panel for viewing individual report information. It also integrates
 * geocoding services to resolve and display readable addresses.
 *
 * Reports with id == 0 are treated as opened (not yet submitted) and are shown
 * in a separate Opened Reports section above the main submitted list.
 */
public class MyReportsController {

    @FXML private ListView<CrimeRecord> crimeListView;
    @FXML private ListView<CrimeRecord> openedListView;
    @FXML private VBox openedSection;
    @FXML private TableView<CrimeRecord> crimeTable;
    @FXML private Label reportCountLabel;
    @FXML private VBox detailPanel;
    @FXML private Pane detailBackdrop;
    @FXML private Label detailCategoryLabel;
    @FXML private Label detailSeverityLabel;
    @FXML private Label detailDateLabel;
    @FXML private TextField detailLocationField;
    @FXML private TextArea detailDescriptionArea;
    @FXML private Label detailStatusLabel;
    @FXML private VBox saveChangesBtn;
    @FXML private Button hamburgerBtn;
    @FXML private StackPane myReportsRoot;
    @FXML private NavBarController navBarController;

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

    // Inline-styled strips - needed for programmatic dark mode override
    @FXML private HBox severityLegendStrip;
    @FXML private HBox sectionHeader;
    @FXML private Label yourReportsLabel;
    @FXML private VBox submitStrip;

    private HamburgerMenu hamburgerMenu;
    private IAppDAO dao;
    private IGeocodingService geocoder = new OpenStreetMapGeoCoder();
    private List<CrimeRecord> allMyReports = new ArrayList<>();

    // The crime record currently open in the detail panel
    private CrimeRecord selectedRecord = null;

    /**
     * Shared cache of reverse-geocoded addresses keyed by crime record ID.
     * Declared static so addresses persist across navigations without re-fetching.
     */
    private static final Map<Integer, String> addressCache = new HashMap<>();

    /**
     * Constructs a new MyReportsController and initialises
     * the DAO from the main application database instance.
     */
    public MyReportsController() {
        this.dao = HelloApplication.DATABASE;
    }

    /**
     * Initialises the screen after the FXML has loaded.
     *
     * Filters all crime records to only those submitted by the current user,
     * populates the list view, sets the report count label, activates the
     * correct nav bar tab, begins background address preloading, and wires
     * up the hamburger menu overlay.
     */
    @FXML
    public void initialize() {
        // Load only this user's reports
        String currentUsername = UserSession.getInstance().getUser().getUsername();
        allMyReports = dao.getAllCrimes().stream()
                .filter(c -> currentUsername.equals(c.getReporter()))
                .toList();

        setupFilters();
        initFilterBar();
        applyFilters();

        // Mark Reports tab active in nav bar
        if (navBarController != null) {
            navBarController.setActiveTab("crimes");
        }

        // Build styled list cells for both the submitted and opened list views
        setupListView();

        // Preload addresses in background
        preloadAddresses(allMyReports);

        // Apply dark mode to inline-styled nodes
        applyDarkStrips();

        // Wire hamburger menu after scene is attached
        Platform.runLater(() -> {
            Stage stage = (Stage) hamburgerBtn.getScene().getWindow();
            hamburgerMenu = new HamburgerMenu(stage);
            hamburgerMenu.setMaxWidth(Double.MAX_VALUE);
            hamburgerMenu.setMaxHeight(Double.MAX_VALUE);
            myReportsRoot.getChildren().add(hamburgerMenu);
            hamburgerBtn.setOnAction(e -> hamburgerMenu.toggle());
            hamburgerMenu.setOnDarkModeChanged(this::refreshDarkMode);
            // Apply dark mode to filter bar now that all nodes are attached to the scene
            applyDarkFilterBar(UserSession.isDarkMode());
            if (crimeListView != null) crimeListView.refresh();
        });
    }

    /**
     * Initialises the compact filter bar above the reports list.
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

        // Status chip group (inside the advanced section)
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
     * Applies or removes dark mode styling on the inline-styled filter bar controls.
     * Called from applyDarkStrips() (dark on) and refreshDarkMode() (dark off) so the
     * filter bar always matches the current theme.
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
     * Applies dark mode overrides to nodes that use hardcoded inline style= in FXML.
     * CSS cannot override inline styles, so we patch them programmatically here.
     */
    /**
     * Applies dark mode overrides to all inline-styled nodes on this screen.
     * CSS cannot override inline styles so we patch them here.
     * Also handles the filter bar, list view background, and all strip backgrounds.
     */
    private void applyDarkStrips() {
        if (!UserSession.isDarkMode()) return;
        String darkStrip    = "-fx-background-color: #1F2937; -fx-border-color: #374151;";
        String darkSection  = "-fx-padding: 12 20 8 20; -fx-background-color: #1F2937;";
        String darkPanel    = "-fx-background-color: #1F2937; -fx-background-radius: 20 20 0 0; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 20, 0.3, 0, -4);";
        String darkReadOnly = "-fx-background-color: #2D3748; -fx-control-inner-background: #2D3748; "
                + "-fx-text-fill: #9CA3AF; -fx-border-color: #4B5563; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-opacity: 1;";
        String listViewDark = "-fx-background-color: #111827; -fx-background: #111827; -fx-border-width: 0;";

        if (severityLegendStrip   != null) severityLegendStrip.setStyle(darkStrip + " -fx-padding: 8 20 8 20; -fx-border-width: 0 0 1 0;");
        if (sectionHeader         != null) sectionHeader.setStyle(darkSection);
        if (yourReportsLabel      != null) yourReportsLabel.setStyle("-fx-text-fill: #F9FAFB; -fx-font-size: 13px; -fx-font-weight: bold;");
        if (submitStrip           != null) submitStrip.setStyle(darkStrip + " -fx-border-width: 1 0 0 0; -fx-padding: 12 20 12 20;");
        if (detailPanel           != null) detailPanel.setStyle(darkPanel);
        if (detailLocationField   != null) detailLocationField.setStyle(darkReadOnly);
        if (detailDescriptionArea != null) detailDescriptionArea.setStyle(darkReadOnly);
        if (crimeListView         != null) crimeListView.setStyle(listViewDark);
        if (openedListView        != null) openedListView.setStyle(darkStrip + " -fx-border-width: 0 0 1 0;");
        // Brighten inline-styled value labels
        if (detailCategoryLabel != null) detailCategoryLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #E5E7EB;");
        if (detailDateLabel     != null) detailDateLabel.setStyle("-fx-text-fill: #E5E7EB;");
        // Filter bar
        applyDarkFilterBar(true);
    }

    /**
     * Called by the hamburger menu when the user toggles dark mode at runtime.
     * Re-applies or removes all dark mode overrides and refreshes the list cells.
     */
    private void refreshDarkMode() {
        if (UserSession.isDarkMode()) {
            applyDarkStrips();
            crimeListView.refresh();
            if (openedListView != null) openedListView.refresh();
        } else {
            String lightStrip   = "-fx-background-color: #FFFFFF; -fx-border-color: #E5E7EB;";
            String lightSection = "-fx-padding: 12 20 8 20; -fx-background-color: #F8F9FA;";
            String lightPanel   = "-fx-background-color: #FFFFFF; -fx-background-radius: 20 20 0 0; "
                    + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 20, 0.3, 0, -4);";
            if (severityLegendStrip   != null) severityLegendStrip.setStyle(lightStrip + " -fx-padding: 8 20 8 20; -fx-border-width: 0 0 1 0;");
            if (sectionHeader         != null) sectionHeader.setStyle(lightSection);
            if (yourReportsLabel      != null) yourReportsLabel.setStyle("-fx-text-fill: #1A1A2E; -fx-font-size: 13px; -fx-font-weight: bold;");
            if (submitStrip           != null) submitStrip.setStyle(lightStrip + " -fx-border-width: 1 0 0 0; -fx-padding: 12 20 12 20;");
            if (detailPanel           != null) detailPanel.setStyle(lightPanel);
            if (detailLocationField   != null) detailLocationField.setStyle("-fx-opacity: 1;");
            if (detailDescriptionArea != null) detailDescriptionArea.setStyle("-fx-opacity: 1;");
            if (detailCategoryLabel   != null) detailCategoryLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2A364E;");
            if (detailDateLabel       != null) detailDateLabel.setStyle("-fx-text-fill: #2A364E;");
            if (crimeListView         != null) crimeListView.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-width: 0;");
            if (openedListView        != null) openedListView.setStyle("-fx-background-color: #FFFDF7; -fx-background: #FFFDF7; -fx-border-width: 0;");
            applyDarkFilterBar(false);
            crimeListView.refresh();
            if (openedListView != null) openedListView.refresh();
        }
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
     * Opened reports (id == 0) are always shown unfiltered in the Opened Reports
     * section. Submitted reports (id != 0) pass through all active filters.
     */
    private void applyFilters() {
        // Separate opened (not yet submitted) reports from submitted ones
        List<CrimeRecord> openedReports = allMyReports.stream()
                .filter(c -> c.getId() == 0)
                .toList();

        List<CrimeRecord> submittedReports = allMyReports.stream()
                .filter(c -> c.getId() != 0)
                .filter(this::matchesSeverityFilter)
                .filter(this::matchesCrimeTypeFilter)
                .filter(this::matchesStatusFilter)
                .filter(this::matchesDateFilter)
                .toList();

        // Show or hide the opened section depending on whether any opened reports exist
        if (openedSection != null) {
            openedSection.setVisible(!openedReports.isEmpty());
            openedSection.setManaged(!openedReports.isEmpty());
        }
        if (openedListView != null) {
            openedListView.getItems().setAll(openedReports);
        }

        crimeTable.getItems().setAll(submittedReports);
        crimeListView.getItems().setAll(submittedReports);
        reportCountLabel.setText(submittedReports.size() + " report" + (submittedReports.size() == 1 ? "" : "s"));
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
     * Configures cell factories for both the submitted and opened crime list views.
     *
     * Submitted list cells display severity colour dot, crime category, dispatch
     * status, geocoded location, and a relative timestamp. Selecting a cell
     * populates and slides up the read-only detail panel.
     *
     * Opened list cells use the same styling but show "Not Submitted" as the status.
     * Selecting an opened cell slides up the detail panel with Save Changes visible.
     */
    private void setupListView() {
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

                Label dot = new Label("\u25CF");
                dot.setStyle("-fx-text-fill: " + dotColor + "; -fx-font-size: 14px;");

                boolean dark = UserSession.isDarkMode();

                Label category = new Label(crime.getCategory().toString());
                category.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: "
                        + (dark ? "#F9FAFB" : "#1A1A2E") + ";");

                String statusText = crime.isActioned() ? "Police Dispatched" : "Pending";
                Label status = new Label(statusText);
                status.setStyle("-fx-font-size: 11px; -fx-text-fill: "
                        + (dark ? "#9CA3AF" : "#6B7280") + ";");

                // Show cached address if available, otherwise show raw coordinates
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

        // Set list view background respecting dark mode - inline style must be set here
        // because FXML transparent style would override the CSS .dark-mode .list-view rule
        crimeListView.setStyle(UserSession.isDarkMode()
                ? "-fx-background-color: #111827; -fx-background: #111827; -fx-border-width: 0;"
                : "-fx-background-color: transparent; -fx-background: transparent; -fx-border-width: 0;");

        // Submitted list selection - opens read-only detail panel
        crimeListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if (openedListView != null) openedListView.getSelectionModel().clearSelection();
                selectedRecord = newVal;
                populateDetailPanel(newVal);
                showDetailPanel(false);
            }
        });

        // Set up the opened reports list view with matching cell style
        if (openedListView != null) {
            openedListView.setCellFactory(lv -> new ListCell<CrimeRecord>() {
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

                    Label dot = new Label("\u25CF");
                    dot.setStyle("-fx-text-fill: " + dotColor + "; -fx-font-size: 14px;");

                    boolean dark = UserSession.isDarkMode();

                    Label category = new Label(crime.getCategory().toString());
                    category.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: "
                            + (dark ? "#F9FAFB" : "#1A1A2E") + ";");

                    // Opened reports always show as "Not Submitted"
                    Label status = new Label("Not Submitted");
                    status.setStyle("-fx-font-size: 11px; -fx-text-fill: "
                            + (dark ? "#9CA3AF" : "#6B7280") + ";");

                    Label location = new Label(String.format("%.4f, %.4f",
                            crime.getLatitude(), crime.getLongitude()));
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

            // Opened list selection - opens editable detail panel with Save Changes visible
            openedListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    crimeListView.getSelectionModel().clearSelection();
                    selectedRecord = newVal;
                    populateDetailPanel(newVal);
                    showDetailPanel(true);
                }
            });

            openedListView.setStyle(UserSession.isDarkMode()
                    ? "-fx-background-color: #1F2937; -fx-background: #1F2937; -fx-border-width: 0;"
                    : "-fx-background-color: #FFFDF7; -fx-background: #FFFDF7; -fx-border-width: 0;");
        }
    }

    /**
     * Populates all fields in the detail panel with data from the selected crime record.
     *
     * If a cached address exists for the record it is shown immediately.
     * Otherwise, the raw coordinates are displayed while a background thread
     * performs reverse geocoding and updates the field when complete.
     * Dark mode field styles are re-applied after population since setText()
     * can reset the skin and clear inline styles.
     *
     * @param crime the selected {@link CrimeRecord} whose details are to be displayed
     */
    private void populateDetailPanel(CrimeRecord crime) {
        detailCategoryLabel.setText(crime.getCategory().toString());
        detailSeverityLabel.setText(crime.getCategory().getSeverity().toString());
        detailDateLabel.setText(UIUtils.formatLocalDateTime(crime.getTimestamp()));
        detailDescriptionArea.setText(crime.getDescription());
        detailStatusLabel.setText(crime.isActioned() ? "Police Dispatched" : "Pending");

        // Show cached address or geocode on demand
        if (addressCache.containsKey(crime.getId())) {
            detailLocationField.setText(addressCache.get(crime.getId()));
        } else {
            detailLocationField.setText(String.format("%.4f, %.4f",
                    crime.getLatitude(), crime.getLongitude()));
            new Thread(() -> {
                try {
                    String address = geocoder.reverseGeocode(
                            crime.getLatitude(), crime.getLongitude());
                    addressCache.put(crime.getId(), address);
                    Platform.runLater(() -> {
                        detailLocationField.setText(address);
                        crimeListView.refresh();
                    });
                } catch (Exception ignored) {}
            }).start();
        }

        // Colour the severity label based on severity tier
        String colour = switch (crime.getCategory().getSeverity()) {
            case CRITICAL -> "#DC143C";
            case MEDIUM   -> "#FF8C00";
            default       -> "#B8860B";
        };
        detailSeverityLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + colour + ";");

        // Re-apply dark mode field styles after setText() calls which can reset the skin
        if (UserSession.isDarkMode()) {
            String darkReadOnly = "-fx-background-color: #2D3748; -fx-control-inner-background: #2D3748; "
                    + "-fx-text-fill: #9CA3AF; -fx-border-color: #4B5563; "
                    + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-opacity: 1;";
            if (detailLocationField   != null) detailLocationField.setStyle(darkReadOnly);
            if (detailDescriptionArea != null) detailDescriptionArea.setStyle(darkReadOnly);
            if (detailCategoryLabel   != null) detailCategoryLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #E5E7EB;");
            if (detailDateLabel       != null) detailDateLabel.setStyle("-fx-text-fill: #E5E7EB;");
            if (detailStatusLabel     != null) detailStatusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #E5E7EB;");
        }
    }

    /**
     * Makes the detail panel visible and animates it sliding up from the bottom of the screen.
     * The semi-transparent backdrop is also shown to focus attention on the panel.
     * @param showSave true shows the Save Changes button for opened (unsubmitted) reports
     */
    private void showDetailPanel(boolean showSave) {
        // Show or hide the Save Changes button depending on report type
        if (saveChangesBtn != null) {
            saveChangesBtn.setVisible(showSave);
            saveChangesBtn.setManaged(showSave);
        }

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
     * Clears both list selections and hides the backdrop once the animation completes.
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
            if (openedListView != null) openedListView.getSelectionModel().clearSelection();
            selectedRecord = null;
        });
        slide.play();
    }

    /**
     * Saves the currently opened (unsubmitted) report to the database.
     *
     * Only callable when an opened report (id == 0) is selected. On success
     * the report moves from the Opened Reports section into the submitted list
     * and the detail panel closes.
     */
    @FXML
    public void onSaveChanges() {
        if (selectedRecord == null || selectedRecord.getId() != 0) return;

        // Update the description from the panel field before saving
        selectedRecord.setDescription(detailDescriptionArea.getText());

        try {
            if (dao.addCrime(selectedRecord)) {
                UIUtils.showAlert(Alert.AlertType.INFORMATION, "Success", "Report submitted successfully.");

                // Reload this user's reports and refresh both list sections
                String currentUsername = UserSession.getInstance().getUser().getUsername();
                allMyReports = dao.getAllCrimes().stream()
                        .filter(c -> currentUsername.equals(c.getReporter()))
                        .toList();

                applyFilters();
                preloadAddresses(allMyReports);
                onCloseDetail();
            }
        } catch (Exception e) {
            UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Could not save: " + e.getMessage());
        }
    }

    /**
     * Navigates to the report submission view.
     */
    @FXML
    public void onSubmitNewReport() {
        Stage stage = (Stage) hamburgerBtn.getScene().getWindow();
        UIUtils.switchScene(stage, "crimes-view.fxml");
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
                        Platform.runLater(() -> crimeListView.refresh());
                    } catch (Exception ignored) {}
                }
            }
        }).start();
    }

    /**
     * Returns a human-readable relative time string for the given timestamp.
     *
     * @param dt the timestamp to describe
     * @return {@code "Today"}, {@code "1 day ago"}, or {@code "N days ago"}
     */
    private String getRelativeTime(java.time.LocalDateTime dt) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(
                dt.toLocalDate(), java.time.LocalDate.now());
        if (days == 0) return "Today";
        if (days == 1) return "1 day ago";
        return days + " days ago";
    }
}
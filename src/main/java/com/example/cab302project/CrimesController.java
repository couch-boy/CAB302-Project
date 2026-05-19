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
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ContextMenu;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;

import java.util.List;
import javafx.collections.FXCollections;
import javafx.scene.input.MouseEvent;

/**
 * Controller for the public crime reports screen (crimes-view.fxml).
 *
 * Manages the List and Map tab views, the sliding detail panel,
 * crime report submission, address autocomplete and the hamburger menu.
 */
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
    private Label severityLabel;
    @FXML
    private Label severityDot;
    @FXML
    private DatePicker datePicker;
    @FXML
    private ComboBox<String> hourBox, minuteBox, ampmBox;
    @FXML
    private TextField reporterField, locationField;
    @FXML
    private CheckBox anonymousCheckBox;
    @FXML
    private TextArea descriptionArea;

    // Read-only detail view bindings (view panel shown for existing crimes)
    @FXML private javafx.scene.control.ScrollPane viewScrollPane;
    @FXML private javafx.scene.control.ScrollPane editScrollPane;
    @FXML private Label detailCategoryLabel;
    @FXML private Label detailDateLabel;
    @FXML private Label detailStatusLabel;
    @FXML private Label severityDotEdit;
    @FXML private Label severityLabelEdit;
    @FXML private TextField locationFieldEdit;
    @FXML private TextArea descriptionAreaEdit;

    // New bindings for the redesigned list and detail panel
    @FXML
    private ListView<CrimeRecord> crimeListView;
    @FXML
    private MenuButton filterMenuButton;
    @FXML
    private RadioMenuItem severityAllItem, severitySevereItem, severityModerateItem, severityLowItem;
    @FXML
    private RadioMenuItem crimeTypeAllItem, crimeTypeAssaultItem, crimeTypeTrespassingItem,
            crimeTypeDomesticAbuseItem, crimeTypeHomicideItem;
    @FXML
    private RadioMenuItem statusAllItem, statusPendingItem;
    @FXML
    private RadioMenuItem dateAllItem, dateTodayItem, dateLast7DaysItem, dateLast30DaysItem;
    @FXML
    private VBox detailPanel;
    @FXML
    private Pane detailBackdrop;
    @FXML
    private Button saveBtn;

    // Opened report banner - shown above the list when an unsaved report exists
    @FXML private VBox openedSection;
    @FXML private Label openedBannerSubtitle;
    @FXML private VBox saveChangesStrip;

    @FXML private ListView<CrimeRecord> openedListView;

    // Tab bar buttons and content panes
    @FXML
    private Button listTabBtn;
    @FXML
    private Button mapTabBtn;
    @FXML
    private VBox listPane;
    @FXML
    private VBox mapPane;
    @FXML
    private WebView crimeMapView;
    // Crime map search overlay fields - mirror the dashboard floating search bar
    @FXML private VBox crimeSearchOverlay;
    @FXML private TextField crimeSearchField;
    @FXML private Label crimeSearchStatusLabel;
    @FXML private StackPane crimeFilterBackdrop;
    @FXML private VBox crimeFilterDrawer;
    @FXML private ComboBox<String> crimeCategoryFilter;
    @FXML private ComboBox<String> crimeDaysFilter;
    @FXML private ComboBox<String> crimeSeverityFilter;
    // Inline-styled strips - needed for programmatic dark mode override
    @FXML private HBox severityLegendStrip;
    @FXML private HBox sectionHeader;
    @FXML private Label recentLabel;
    @FXML private VBox submitStripList;
    @FXML private HBox mapSeverityLegend;
    @FXML private VBox submitStripMap;

    // Crime map search state - mirrors DashboardController pattern
    private double[] crimeActiveBoundingBox = null;
    private String crimeActiveGeoJson = null;
    private boolean crimeFilterDrawerOpen = false;
    private WebEngine crimeEngine = null;
    // Set to true only after LeafletLoader has finished injecting Leaflet and called initMap.
    // Guards against filter/search methods calling loadCrimeMarkers before the JS is ready.
    private boolean crimeMapReady = false;

    // Hamburger menu
    @FXML
    private StackPane crimesRoot;
    @FXML
    private Button hamburgerBtn;
    @FXML
    private Button crimeFilterToggleBtn;

    private HamburgerMenu hamburgerMenu;

    @FXML
    private NavBarController navBarController;

    private IAppDAO dao;

    private IGeocodingService geocoder = new OpenStreetMapGeoCoder();
    @FXML

    private final ContextMenu suggestionsPopup = new ContextMenu();
    private final PauseTransition suggestionDelay = new PauseTransition(Duration.millis(400));

    private boolean isCreatingNew = false;

    // Holds the unsaved report (id == 0) across filter and refresh cycles.
    // Kept separately from allCrimeRecords which only contains database records.
    private CrimeRecord pendingRecord = null;

    // Caches the raw address text the user typed so it survives panel close/reopen
    private String pendingLocationText = null;
    private List<CrimeRecord> allCrimeRecords = new ArrayList<>();

    // Tracks whether the crime map has been loaded yet (loaded lazily on first Map tab open)
    private boolean crimeMapLoaded = false;

    // Cache of geocoded addresses keyed by crime ID.
    // Populated on list load and on row selection.
    // Static so the cache persists across screen navigations and avoids re-geocoding
    private static final Map<Integer, String> addressCache = new HashMap<>();

    /**
     * Constructs the CrimesController and initialises the database access object (DAO).
     * This allows the controller to interact with the shared application database
     * for managing and retrieving crime records.
     */
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
        setupFilters();

        // Initialize Listener -> Auto-update severity dot colour and label when category changes.
        // This gives the user immediate visual feedback on the severity tier as they pick a crime type.
        categoryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateSeverityDisplay(newVal);
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

        // Set List tab as active by default
        setActiveTab(true);

        // Apply dark mode to inline-styled nodes
        applyDarkStrips();

        // Wire hamburger menu after scene is attached
        // Platform.runLater ensures getScene().getWindow() is not null
        Platform.runLater(() -> {
            Stage stage = (Stage) hamburgerBtn.getScene().getWindow();
            hamburgerMenu = new HamburgerMenu(stage);
            hamburgerMenu.setMaxWidth(Double.MAX_VALUE);
            hamburgerMenu.setMaxHeight(Double.MAX_VALUE);
            crimesRoot.getChildren().add(hamburgerMenu);
            hamburgerBtn.setOnAction(e -> hamburgerMenu.toggle());
            hamburgerMenu.setOnDarkModeChanged(this::refreshDarkMode);
        });

        anonymousCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {

            if (newVal) {
                reporterField.setText("Anonymous");
            } else {
                if (UserSession.getInstance().getUser() != null) {
                    reporterField.setText(
                            UserSession.getInstance().getUser().getUsername()
                    );
                }
            }
        });
    }

    /**
     * Applies dark mode overrides to nodes that use hardcoded inline style= in FXML.
     * CSS cannot override inline styles, so we patch them programmatically here.
     */
    private void applyDarkStrips() {
        if (!UserSession.isDarkMode()) return;
        String darkStrip   = "-fx-background-color: #1F2937; -fx-border-color: #374151;";
        String darkSection = "-fx-padding: 12 20 8 20; -fx-background-color: #1F2937;";
        String darkPanel   = "-fx-background-color: #1F2937; -fx-background-radius: 20 20 0 0; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 20, 0.3, 0, -4);";
        String darkInput   = "-fx-background-color: #374151; -fx-control-inner-background: #374151; "
                + "-fx-text-fill: #F9FAFB; -fx-border-color: #4B5563; "
                + "-fx-border-radius: 8; -fx-background-radius: 8;";
        String darkReadOnly = "-fx-background-color: #2D3748; -fx-control-inner-background: #2D3748; "
                + "-fx-text-fill: #9CA3AF; -fx-border-color: #4B5563; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-opacity: 1;";
        if (severityLegendStrip != null) severityLegendStrip.setStyle(darkStrip + " -fx-padding: 8 20 8 20; -fx-border-width: 0 0 1 0;");
        if (sectionHeader       != null) sectionHeader.setStyle(darkSection);
        if (recentLabel         != null) recentLabel.setStyle("-fx-text-fill: #F9FAFB; -fx-font-size: 13px; -fx-font-weight: bold;");
        if (submitStripList     != null) submitStripList.setStyle(darkStrip + " -fx-border-width: 1 0 0 0; -fx-padding: 12 20 12 20;");
        if (mapSeverityLegend   != null) mapSeverityLegend.setStyle(darkStrip + " -fx-padding: 8 20 8 20; -fx-border-width: 1 0 0 0;");
        if (submitStripMap      != null) submitStripMap.setStyle(darkStrip + " -fx-border-width: 1 0 0 0; -fx-padding: 12 20 12 20;");
        if (detailPanel         != null) detailPanel.setStyle(darkPanel);
        if (saveChangesStrip    != null) saveChangesStrip.setStyle(darkStrip + " -fx-border-width: 1 0 0 0; -fx-padding: 12 20 16 20;");
        // Read-only view fields
        if (locationField       != null) locationField.setStyle(darkReadOnly);
        if (descriptionArea     != null) descriptionArea.setStyle(darkReadOnly);
        // Editable form fields (shown only when creating a new report)
        if (locationFieldEdit   != null) locationFieldEdit.setStyle(darkInput);
        if (descriptionAreaEdit != null) descriptionAreaEdit.setStyle(darkInput);
        if (reporterField       != null) reporterField.setStyle(darkReadOnly);
        // Brighten inline-styled value labels in the read-only view
        String brightLabel = "-fx-font-weight: bold; -fx-text-fill: #E5E7EB;";
        String brightLabelNormal = "-fx-text-fill: #E5E7EB;";
        if (detailCategoryLabel != null) detailCategoryLabel.setStyle(brightLabel);
        if (detailDateLabel     != null) detailDateLabel.setStyle(brightLabelNormal);
    }

    private void refreshDarkMode() {
        if (UserSession.isDarkMode()) {
            applyDarkStrips();
            crimeListView.refresh();
        } else {
            String lightStrip   = "-fx-background-color: #FFFFFF; -fx-border-color: #E5E7EB;";
            String lightSection = "-fx-padding: 12 20 8 20; -fx-background-color: #F8F9FA;";
            String lightPanel   = "-fx-background-color: #FFFFFF; -fx-background-radius: 20 20 0 0; "
                    + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 20, 0.3, 0, -4);";
            if (severityLegendStrip != null) severityLegendStrip.setStyle(lightStrip + " -fx-padding: 8 20 8 20; -fx-border-width: 0 0 1 0;");
            if (sectionHeader       != null) sectionHeader.setStyle(lightSection);
            if (recentLabel         != null) recentLabel.setStyle("-fx-text-fill: #1A1A2E; -fx-font-size: 13px; -fx-font-weight: bold;");
            if (submitStripList     != null) submitStripList.setStyle(lightStrip + " -fx-border-width: 1 0 0 0; -fx-padding: 12 20 12 20;");
            if (mapSeverityLegend   != null) mapSeverityLegend.setStyle(lightStrip + " -fx-padding: 8 20 8 20; -fx-border-width: 1 0 0 0;");
            if (submitStripMap      != null) submitStripMap.setStyle(lightStrip + " -fx-border-width: 1 0 0 0; -fx-padding: 12 20 12 20;");
            if (detailPanel         != null) detailPanel.setStyle(lightPanel);
            if (saveChangesStrip    != null) saveChangesStrip.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E5E7EB; -fx-border-width: 1 0 0 0; -fx-padding: 12 20 16 20;");
            // Read-only view fields
            if (locationField       != null) locationField.setStyle("-fx-opacity: 1;");
            if (descriptionArea     != null) descriptionArea.setStyle("-fx-opacity: 1;");
            // Editable form fields
            if (locationFieldEdit   != null) locationFieldEdit.setStyle(null);
            if (descriptionAreaEdit != null) descriptionAreaEdit.setStyle(null);
            if (reporterField       != null) reporterField.setStyle("-fx-opacity: 1;");
            // Restore value label colours for light mode
            if (detailCategoryLabel != null) detailCategoryLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #E5E7EB;");
            if (detailDateLabel     != null) detailDateLabel.setStyle("-fx-text-fill: #E5E7EB;");
            crimeListView.refresh();
        }
    }

    /**
     * Shows the list pane and marks the List tab as active.
     */
    @FXML
    public void onListTabClick() {
        setActiveTab(true);
    }

    /**
     * Shows the map pane and marks the Map tab as active.
     * Loads the crime map on first open only.
     */
    @FXML
    public void onMapTabClick() {
        setActiveTab(false);
        if (!crimeMapLoaded) {
            loadCrimeMap();
            crimeMapLoaded = true;
        }
    }

    /**
     * Toggles visibility between the list and map panes and updates tab button styling.
     * When switching to the map tab, the floating search overlay is made visible.
     * When switching back to the list tab, the overlay is hidden so it does not
     * appear over the list content.
     * @param listActive true shows the list pane, false shows the map pane
     */
    private void setActiveTab(boolean listActive) {
        if (listActive) {
            listTabBtn.getStyleClass().add("tab-btn-active");
            mapTabBtn.getStyleClass().remove("tab-btn-active");
            listPane.setVisible(true);
            listPane.setManaged(true);
            mapPane.setVisible(false);
            mapPane.setManaged(false);
            // Hide the floating search overlay when returning to the list pane
            if (crimeSearchOverlay != null) {
                crimeSearchOverlay.setVisible(false);
                crimeSearchOverlay.setManaged(false);
            }
        } else {
            mapTabBtn.getStyleClass().add("tab-btn-active");
            listTabBtn.getStyleClass().remove("tab-btn-active");
            mapPane.setVisible(true);
            mapPane.setManaged(true);
            listPane.setVisible(false);
            listPane.setManaged(false);
            // Show the floating search overlay when the map pane is active
            if (crimeSearchOverlay != null) {
                crimeSearchOverlay.setVisible(true);
                crimeSearchOverlay.setManaged(true);
            }
        }
    }

    /**
     * Loads crime-map.html into the WebView and injects crime marker data via JavaScript
     * once the page has finished loading. Uses LeafletLoader to inject Leaflet from a
     * bundled classpath resource and routes tile requests through TileProxyServer
     * for cross-platform compatibility.
     * Stores the WebEngine reference and sets up the map filter dropdowns so the
     * floating search bar and filter drawer work after the map is first opened.
     */
    private void loadCrimeMap() {
        if (crimeMapView == null) return;

        crimeEngine = crimeMapView.getEngine();
        setupCrimeMapFilters();

        LeafletLoader.loadMap(crimeMapView, "crime-map.html", () -> {
            crimeMapReady = true;
            // Brief delay on first load so Leaflet finishes initialising
            // before markers are pushed, ensuring fitBounds works correctly
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(
                    javafx.util.Duration.millis(300));
            delay.setOnFinished(e -> applyCrimeMapFilters());
            delay.play();
        });
    }

    /**
     * Populates the crime map filter drawer dropdowns with their option lists.
     * Called once when the map is first loaded.
     */
    private void setupCrimeMapFilters() {
        if (crimeCategoryFilter != null) {
            List<String> cats = new ArrayList<>();
            cats.add("All Types");
            for (CrimeCategory cat : CrimeCategory.values()) cats.add(cat.getName());
            crimeCategoryFilter.setItems(FXCollections.observableArrayList(cats));
            crimeCategoryFilter.setValue("All Types");
        }
        if (crimeSeverityFilter != null) {
            crimeSeverityFilter.setItems(FXCollections.observableArrayList(
                    "All Severities", "Severe", "Moderate", "Low"));
            crimeSeverityFilter.setValue("All Severities");
        }
        if (crimeDaysFilter != null) {
            crimeDaysFilter.setItems(FXCollections.observableArrayList(
                    "All time", "Last 24 hours", "Last 7 days",
                    "Last 30 days", "Last 90 days", "Last year"));
            crimeDaysFilter.setValue("All time");
        }
    }

    /**
     * Applies all active crime map filters - suburb bounding box, crime type,
     * severity and time range - and pushes the filtered JSON to the map.
     * Also passes the active GeoJSON polygon so JavaScript can perform a precise
     * point-in-polygon check inside the suburb boundary.
     */
    private void applyCrimeMapFilters() {
        if (crimeEngine == null || !crimeMapReady) return;

        List<CrimeRecord> allCrimes = dao.getAllCrimes();

        String selCat  = crimeCategoryFilter != null ? crimeCategoryFilter.getValue() : "All Types";
        String selSev  = crimeSeverityFilter  != null ? crimeSeverityFilter.getValue()  : "All Severities";
        String selDays = crimeDaysFilter      != null ? crimeDaysFilter.getValue()      : "All time";

        LocalDateTime cutoff = resolveCrimeDaysCutoff(selDays);

        List<CrimeRecord> filtered = new ArrayList<>();
        for (CrimeRecord c : allCrimes) {

            // Suburb bounding box pre-filter - fast rectangle check before polygon test
            if (crimeActiveBoundingBox != null &&
                    !SuburbSearchService.isInBoundingBox(
                            c.getLatitude(), c.getLongitude(), crimeActiveBoundingBox)) continue;

            // Crime type filter
            if (selCat != null && !selCat.equals("All Types") &&
                    !c.getCategory().getName().equals(selCat)) continue;

            // Severity filter - map display labels to the internal enum values
            if (selSev != null && !selSev.equals("All Severities")) {
                String severityLabel = switch (selSev) {
                    case "Severe"   -> "Critical";
                    case "Moderate" -> "Medium";
                    case "Low"      -> "Low";
                    default         -> selSev;
                };
                if (!c.getCategory().getSeverity().toString().equals(severityLabel)) continue;
            }

            // Time range filter
            if (cutoff != null && c.getTimestamp() != null &&
                    c.getTimestamp().isBefore(cutoff)) continue;

            filtered.add(c);
        }

        final String json = buildCrimeJson(filtered)
                .replace("\\", "\\\\")
                .replace("'", "\\'");

        // Pass the active GeoJSON polygon so JavaScript performs a precise
        // point-in-polygon test inside the suburb boundary, not just the bounding box
        final String geoJsonFilter = (crimeActiveGeoJson != null)
                ? crimeActiveGeoJson.replace("\\", "\\\\").replace("'", "\\'")
                : "";

        // Update the status label below the search bar
        String status = crimeActiveBoundingBox == null && filtered.size() == allCrimes.size()
                ? ""
                : filtered.isEmpty()
                  ? "No crimes match these filters"
                  : filtered.size() + " crime" + (filtered.size() == 1 ? "" : "s") + " in view";
        if (crimeSearchStatusLabel != null) {
            Platform.runLater(() -> crimeSearchStatusLabel.setText(status));
        }

        try {
            crimeEngine.executeScript("loadCrimeMarkers('" + json + "','" + geoJsonFilter + "')");
        } catch (Exception e) {
            System.out.println("JS execution failed: " + e.getMessage());
        }
    }

    /**
     * Converts a time range label from the days filter dropdown into a LocalDateTime
     * cutoff. Returns null when All time is selected meaning no restriction is applied.
     * @param label the display string from the days filter dropdown
     * @return the earliest allowed crime timestamp, or null for no restriction
     */
    private LocalDateTime resolveCrimeDaysCutoff(String label) {
        if (label == null || label.equals("All time")) return null;
        LocalDateTime now = LocalDateTime.now();
        return switch (label) {
            case "Last 24 hours" -> now.minusDays(1);
            case "Last 7 days"   -> now.minusDays(7);
            case "Last 30 days"  -> now.minusDays(30);
            case "Last 90 days"  -> now.minusDays(90);
            case "Last year"     -> now.minusYears(1);
            default              -> null;
        };
    }

    /**
     * Handles suburb search on the crime map when the user presses enter or Search.
     * Runs Nominatim on a background thread to keep the UI responsive, then draws
     * the suburb boundary polygon on the map and refilters markers to that area.
     */
    @FXML
    public void onCrimeSearch() {
        if (crimeSearchField == null) return;
        String query = crimeSearchField.getText().trim();
        if (query.isEmpty()) {
            onCrimeClearSearch();
            return;
        }
        if (crimeSearchStatusLabel != null) crimeSearchStatusLabel.setText("Searching...");

        Thread t = new Thread(() -> {
            SuburbSearchService.SuburbResult result = new SuburbSearchService().search(query);
            Platform.runLater(() -> {
                if (result == null) {
                    if (crimeSearchStatusLabel != null)
                        crimeSearchStatusLabel.setText("Suburb not found. Try a different name.");
                    return;
                }
                crimeActiveBoundingBox = result.boundingBox;
                crimeActiveGeoJson = result.geoJson;

                try {
                    String name = result.displayName
                            .replace("\\", "\\\\").replace("'", "\\'");
                    if (result.geoJson != null) {
                        String safeGj = result.geoJson
                                .replace("\\", "\\\\").replace("'", "\\'");
                        crimeEngine.executeScript(
                                "showSuburbBoundary('" + safeGj + "','" + name + "',"
                                        + result.lat + "," + result.lon + ")");
                    } else {
                        crimeEngine.executeScript(
                                "flyToSuburb(" + result.lat + "," + result.lon + ",13)");
                    }
                } catch (Exception e) {
                    System.out.println("Crime map update failed: " + e.getMessage());
                }
                applyCrimeMapFilters();
            });
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * Clears the suburb search on the crime map, removes the boundary polygon,
     * and resets the map to show all crimes.
     */
    @FXML
    public void onCrimeClearSearch() {
        crimeActiveBoundingBox = null;
        crimeActiveGeoJson = null;
        if (crimeSearchField != null) crimeSearchField.clear();
        if (crimeSearchStatusLabel != null) crimeSearchStatusLabel.setText("");
        try { crimeEngine.executeScript("clearSuburbBoundary()"); } catch (Exception ignored) {}
        applyCrimeMapFilters();
    }

    /**
     * Toggles the crime map filter drawer open and closed.
     * Also shows or hides the transparent backdrop that catches outside clicks.
     */
    @FXML
    public void onCrimeToggleFilter() {
        crimeFilterDrawerOpen = !crimeFilterDrawerOpen;
        if (crimeFilterBackdrop != null) {
            crimeFilterBackdrop.setVisible(crimeFilterDrawerOpen);
            crimeFilterBackdrop.setManaged(crimeFilterDrawerOpen);
        }
    }

    /**
     * Closes the crime map filter drawer when the user clicks outside it.
     */
    @FXML
    public void onCrimeBackdropClicked() {
        if (crimeFilterDrawerOpen) onCrimeToggleFilter();
    }

    /**
     * Consumes mouse clicks on the filter drawer itself so they do not
     * propagate to the backdrop and accidentally close the panel.
     * @param event the mouse event to consume
     */
    @FXML
    public void onCrimeFilterDrawerClicked(MouseEvent event) {
        event.consume();
    }

    /**
     * Called when any crime map filter dropdown changes value.
     * Immediately reapplies all filters and refreshes the map markers.
     */
    @FXML
    public void onCrimeFilterChanged() {
        if (crimeEngine != null) applyCrimeMapFilters();
    }

    /**
     * Resets all crime map filter dropdowns to their defaults and refreshes the map.
     */
    @FXML
    public void onCrimeResetFilters() {
        if (crimeCategoryFilter != null) crimeCategoryFilter.setValue("All Types");
        if (crimeSeverityFilter  != null) crimeSeverityFilter.setValue("All Severities");
        if (crimeDaysFilter      != null) crimeDaysFilter.setValue("All time");
        if (crimeEngine != null) applyCrimeMapFilters();
    }

    /**
     * Converts a list of CrimeRecord objects into a JSON string for the crime map markers.
     * @param crimes list of crimes to serialise
     * @return a JSON array string with lat, lon and severity fields for each crime
     */
    private String buildCrimeJson(List<CrimeRecord> crimes) {
        StringBuilder sb = new StringBuilder("[");

        for (int i = 0; i < crimes.size(); i++) {
            CrimeRecord c = crimes.get(i);
            sb.append("{")
                    .append("\"lat\":").append(c.getLatitude()).append(",")
                    .append("\"lon\":").append(c.getLongitude()).append(",")
                    .append("\"severity\":\"").append(c.getCategory().getSeverity().toString().toUpperCase()).append("\"")
                    .append("}");
            if (i < crimes.size() - 1) sb.append(",");
        }

        sb.append("]");
        return sb.toString();
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
                    // Clear the pending record now that it has been saved
                    pendingRecord = null;
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
     * Add a new crime to the database after entering details.
     * The new blank record is stored in pendingRecord and shown in the
     * Opened Report banner above the list. The list scrolls to the top
     * so the user sees the banner immediately.
     */
    @FXML
    public void onAddNewCrime() {
        // Check if there is already a pending unsaved report
        if (pendingRecord != null) {
            UIUtils.showAlert(Alert.AlertType.WARNING, "Pending Report",
                    "Please save or discard your opened report before creating another.");
            openDetailForPending();
            return;
        }

        // Create blank CrimeRecord object template
        // Set id to 0
        // Default lat/lon for Brisbane CBD
        // UserSession.getInstance().getUser().getUsername() provides the reporter username
        isCreatingNew = true;
        pendingRecord = new CrimeRecord(
                0,
                CrimeCategory.OTHER,
                LocalDateTime.now(),
                -27.4709,
                153.0235,
                "",
                UserSession.getInstance().getUser().getUsername(),
                false
        );

        // Add to table so existing form population logic still works
        crimeTable.getItems().add(0, pendingRecord);
        crimeTable.getSelectionModel().select(pendingRecord);
        anonymousCheckBox.setSelected(false);

        // Show the banner and scroll the list to the top
        refreshOpenedSection();
        crimeListView.scrollTo(0);

        // Open detail panel with the save button visible
        showDetailPanel(true);

        // Open detail panel with the save button visible
        showDetailPanel(true);

        // Open detail panel with the save button visible
        showDetailPanel(true);

        // Show the alert over the panel - passing the owner stage ensures it
        // renders on top of the JavaFX window rather than behind the detail panel
        Stage stage = (Stage) hamburgerBtn.getScene().getWindow();
        UIUtils.showAlert(Alert.AlertType.INFORMATION, "New Report",
                "A new blank report has been created. Fill in the details and click 'Save Changes'.",
                stage);
    }

    /**
     * Re-opens the detail panel for the current pending report.
     * Called when the user taps the Opened Report banner.
     * The table is re-synced with the latest pendingRecord reference so
     * any edits saved on close are restored into the form fields.
     */
    private void openDetailForPending() {
        if (pendingRecord == null) return;

        // Re-sync the table with the current pendingRecord so populateForm
        // reads the values saved when the panel was last closed
        if (!crimeTable.getItems().contains(pendingRecord)) {
            crimeTable.getItems().add(0, pendingRecord);
        } else {
            // Replace the old reference in the table with the updated one
            int idx = crimeTable.getItems().indexOf(pendingRecord);
            if (idx >= 0) crimeTable.getItems().set(idx, pendingRecord);
        }

        crimeTable.getSelectionModel().select(pendingRecord);
        anonymousCheckBox.setSelected(pendingRecord.getReporter() == null
                || pendingRecord.getReporter().isEmpty());
        showDetailPanel(true);
    }

    /**
     * Handles a tap on the Opened Report banner above the list.
     * Re-opens the detail panel so the user can continue editing
     * and submit the pending report.
     */
    @FXML
    public void onOpenedBannerClicked() {
        openDetailForPending();
    }

    /**
     * Refreshes the Opened Report banner visibility and subtitle.
     * Shows the banner when a pending record exists, hides it when there is none.
     */
    private void refreshOpenedSection() {
        boolean hasPending = pendingRecord != null;
        if (openedSection != null) {
            openedSection.setVisible(hasPending);
            openedSection.setManaged(hasPending);
        }
        if (openedBannerSubtitle != null && hasPending) {
            openedBannerSubtitle.setText(
                    pendingRecord.getCategory().toString() + " - Tap to continue editing and submit"
            );
        }
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
     * Close the detail panel - slides back down off screen.
     * If a pending report is open, its current form values are saved back into
     * pendingRecord before closing so they are restored when the user reopens it.
     */
    @FXML
    public void onCloseDetail() {
        // Persist any edits the user has made back into the pending record
        // so reopening the banner restores what they typed rather than the defaults
        if (pendingRecord != null && isCreatingNew) {
            if (categoryComboBox.getValue() != null) {
                pendingRecord = new CrimeRecord(
                        0,
                        categoryComboBox.getValue(),
                        pendingRecord.getTimestamp(),
                        pendingRecord.getLatitude(),
                        pendingRecord.getLongitude(),
                        descriptionAreaEdit != null ? descriptionAreaEdit.getText() : descriptionArea.getText(),
                        anonymousCheckBox.isSelected() ? "" : pendingRecord.getReporter(),
                        false
                );
            }
            // Cache the raw address text since CrimeRecord only stores coordinates
            if (locationFieldEdit != null) pendingLocationText = locationFieldEdit.getText();
            // Refresh the banner subtitle to reflect any category change made in the form
            refreshOpenedSection();
        }

        // Cache the raw address text since CrimeRecord only stores coordinates
        if (locationFieldEdit != null) pendingLocationText = locationFieldEdit.getText();

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

    /**
     * Refreshes the crime list by loading non-actioned records
     * and keeping the current selection in sync.
     * Pending records (id == 0) are preserved across refreshes since
     * they only exist in memory and are not returned by the database.
     */
// Helper function to refresh crime table with updated crime data
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

        // Restore the banner - pending record lives only in memory so it
        // would be lost if refreshOpenedSection is not called after each refresh
        refreshOpenedSection();
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
     * Opened reports (id == 0) are excluded from the filtered list and
     * shown only in the Opened Report banner above the list.
     */
    private void applyFilters() {
        List<CrimeRecord> filteredCrimes = allCrimeRecords.stream()
                .filter(c -> c.getId() != 0) // opened reports are handled separately
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

    /**
     * Checks whether a crime record matches the currently selected severity filter.
     * Returns true if no specific severity filter is selected.
     */
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
     * Checks whether a crime record matches the currently selected crime type filter.
     * Returns true if no specific crime type filter is selected.
     */
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

    /**
     * Checks whether a crime record matches the currently selected status filter.
     * Pending crimes are identified as records that have not yet been actioned.
     */
    private boolean matchesStatusFilter(CrimeRecord crime) {
        if (statusPendingItem.isSelected()) {
            return !crime.isActioned();
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

    /**
     * Updates the filter button text to display the number
     * of currently active filter categories.
     */
    private void updateFilterButtonText() {
        int activeFilters = 0;
        if (!severityAllItem.isSelected()) activeFilters++;
        if (!crimeTypeAllItem.isSelected()) activeFilters++;
        if (!statusAllItem.isSelected()) activeFilters++;
        if (!dateAllItem.isSelected()) activeFilters++;

        filterMenuButton.setText(activeFilters == 0 ? "Filter" : "Filter (" + activeFilters + ")");
    }

    /**
     * Sets up table columns and binds them to crime data fields.
     */
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

    /**
     * Initialises date and time input controls with values and defaults.
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
     * Returns a human-readable relative time string from a LocalDateTime.
     * @param timestamp the timestamp to describe
     * @return a string such as "Today", "1 day ago", or "3 days ago"
     */
    public static String getRelativeTime(LocalDateTime timestamp) {
        long daysAgo = ChronoUnit.DAYS.between(timestamp.toLocalDate(), LocalDate.now());

        if (daysAgo == 0) return "Today";
        if (daysAgo == 1) return "1 day ago";
        return daysAgo + " days ago";
    }

    /**
     * Updates the severity dot colour and label text in the detail panel
     * based on the given crime category. Called whenever the category selection
     * changes so the user sees immediate visual feedback on the severity tier.
     * @param category the selected CrimeCategory, or null to reset to a blank state
     */
    private void updateSeverityDisplay(CrimeCategory category) {
        if (category == null) {
            if (severityDot  != null) severityDot.setStyle("-fx-text-fill: #D1D5DB; -fx-font-size: 13px;");
            if (severityLabel != null) { severityLabel.setText("-"); severityLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #6B7280;"); }
            return;
        }

        String dotColor = switch (category.getSeverity()) {
            case CRITICAL -> "#DC143C";
            case MEDIUM   -> "#FF8C00";
            default       -> "#FFD700";
        };

        String severityText = category.getSeverity().toString();

        if (severityDot  != null) severityDot.setStyle("-fx-text-fill: " + dotColor + "; -fx-font-size: 13px;");
        if (severityLabel != null) { severityLabel.setText(severityText); severityLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + dotColor + ";"); }
        // Also update edit panel severity indicators
        if (severityDotEdit  != null) severityDotEdit.setStyle("-fx-text-fill: " + dotColor + "; -fx-font-size: 13px;");
        if (severityLabelEdit != null) { severityLabelEdit.setText(severityText); severityLabelEdit.setStyle("-fx-font-weight: bold; -fx-text-fill: " + dotColor + ";"); }
    }

    /**
     * Builds the styled ListView and the opened reports ListView with custom cells.
     * The opened list shows unsaved reports (id == 0) above the main list.
     * Selection on either ListView mirrors to the hidden TableView so all existing logic stays intact.
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

        // Selecting a submitted list row mirrors to the table and opens the detail panel.
        // Save button is hidden for submitted records viewed by public users.
        crimeListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if (openedListView != null) openedListView.getSelectionModel().clearSelection();
                crimeTable.getSelectionModel().select(newVal);
                showDetailPanel(false);
            }
        });

        crimeListView.setStyle("-fx-background-color: transparent; " +
                "-fx-background: transparent; -fx-border-width: 0;");

    }

    /**
     * Slides the detail panel up from the bottom of the screen and dims the background.
     * @param showSave true shows the Save Changes strip, used when creating a new report
     */
    private void showDetailPanel(boolean showSave) {
        boolean canSave = UserSession.isPolice() || showSave;

        // Toggle the save strip that wraps the save button
        if (saveChangesStrip != null) {
            saveChangesStrip.setVisible(canSave);
            saveChangesStrip.setManaged(canSave);
        }

        // Re-apply dark mode field styles each time the panel opens,
        // since setText() calls reset the skin and can clear inline styles.
        if (UserSession.isDarkMode()) {
            String darkReadOnly = "-fx-background-color: #2D3748; -fx-control-inner-background: #2D3748; "
                    + "-fx-text-fill: #9CA3AF; -fx-border-color: #4B5563; "
                    + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-opacity: 1;";
            String darkInput = "-fx-background-color: #374151; -fx-control-inner-background: #374151; "
                    + "-fx-text-fill: #F9FAFB; -fx-border-color: #4B5563; "
                    + "-fx-border-radius: 8; -fx-background-radius: 8;";
            if (locationField       != null) locationField.setStyle(darkReadOnly);
            if (descriptionArea     != null) descriptionArea.setStyle(darkReadOnly);
            if (locationFieldEdit   != null) locationFieldEdit.setStyle(darkInput);
            if (descriptionAreaEdit != null) descriptionAreaEdit.setStyle(darkInput);
            // Re-brighten value labels after setText() resets skins
            if (detailCategoryLabel != null) detailCategoryLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #E5E7EB;");
            if (detailDateLabel     != null) detailDateLabel.setStyle("-fx-text-fill: #E5E7EB;");
        }

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
     * @param crime the CrimeRecord whose data should populate the form
     */
    private void populateForm(CrimeRecord crime) {
        boolean isNew = (crime.getId() == 0);

        // Always populate read-only view labels
        if (detailCategoryLabel != null) detailCategoryLabel.setText(crime.getCategory().toString());
        if (detailDateLabel != null) detailDateLabel.setText(UIUtils.formatLocalDateTime(crime.getTimestamp()));
        if (detailStatusLabel != null) detailStatusLabel.setText(crime.isActioned() ? "Police Dispatched" : "Pending");

        // Populate editable form fields (used when creating a new report)
        LocalDateTime dt = crime.getTimestamp();
        categoryComboBox.setValue(crime.getCategory());
        updateSeverityDisplay(crime.getCategory());
        datePicker.setValue(dt.toLocalDate());
        int hour = dt.getHour();
        ampmBox.setValue(hour >= 12 ? "PM" : "AM");
        int displayHour = (hour % 12 == 0) ? 12 : hour % 12;
        hourBox.setValue(String.format("%02d", displayHour));
        int mins = dt.getMinute();
        minuteBox.setValue(String.format("%02d", (mins / 15) * 15));
        if (descriptionAreaEdit != null) descriptionAreaEdit.setText(crime.getDescription());
        reporterField.setText(crime.getReporterDisplayName());

        // Geocode address and populate both location fields
        if (addressCache.containsKey(crime.getId())) {
            String addr = addressCache.get(crime.getId());
            locationField.setText(addr);
            if (locationFieldEdit != null) locationFieldEdit.setText(addr);
        } else {
            String coords = String.format("%.4f, %.4f", crime.getLatitude(), crime.getLongitude());
            locationField.setText(coords);
            if (locationFieldEdit != null) locationFieldEdit.setText(coords);
            new Thread(() -> {
                try {
                    String address = geocoder.reverseGeocode(crime.getLatitude(), crime.getLongitude());
                    String[] parts = address.split(",");
                    String shortAddress = parts.length >= 2
                            ? parts[0].trim() + ", " + parts[1].trim()
                            : address;
                    addressCache.put(crime.getId(), shortAddress);
                    Platform.runLater(() -> {
                        locationField.setText(address);
                        if (locationFieldEdit != null) locationFieldEdit.setText(address);
                        crimeListView.refresh();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        String c = String.format("%.4f, %.4f", crime.getLatitude(), crime.getLongitude());
                        locationField.setText(c);
                        if (locationFieldEdit != null) locationFieldEdit.setText(c);
                    });
                }
            }).start();
        }

        // For the pending record, restore the raw address text the user had typed
        if (isNew && pendingLocationText != null && !pendingLocationText.isEmpty()) {
            locationField.setText(pendingLocationText);
            if (locationFieldEdit != null) locationFieldEdit.setText(pendingLocationText);
        }

        // Apply dark mode styles to read-only fields after text is set
        if (UserSession.isDarkMode()) {
            String darkReadOnly = "-fx-background-color: #2D3748; -fx-control-inner-background: #2D3748; "
                    + "-fx-text-fill: #9CA3AF; -fx-border-color: #4B5563; "
                    + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-opacity: 1;";
            if (locationField   != null) locationField.setStyle(darkReadOnly);
            if (descriptionArea != null) descriptionArea.setStyle(darkReadOnly);
            if (detailCategoryLabel != null) detailCategoryLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #E5E7EB;");
            if (detailDateLabel     != null) detailDateLabel.setStyle("-fx-text-fill: #E5E7EB;");
            if (detailStatusLabel   != null) detailStatusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #E5E7EB;");
        }

        descriptionArea.setText(crime.getDescription());

        setFormEditable(isNew);
        isCreatingNew = isNew;
    }

    /**
     * Builds a CrimeRecord from the current form field values.
     * Converts the 12-hour time selection back to 24-hour format, geocodes
     * the entered address string to coordinates, and assembles a new record
     * preserving the original ID and reporter from the existing record.
     *
     * @param original the existing record being edited, used to preserve immutable fields
     * @return a new CrimeRecord populated with the form data
     * @throws IllegalArgumentException if the address field is empty
     * @throws Exception if geocoding fails
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
        String address = locationFieldEdit != null ? locationFieldEdit.getText().trim() : locationField.getText().trim();

        if (address.isEmpty()) {
            throw new IllegalArgumentException("Please enter an address.");
        }

        double[] coords = geocoder.geocodeAddress(address);
        double lat = coords[0];
        double lon = coords[1];

        System.out.println("Address entered: " + address);
        System.out.println("Resolved coordinates: " + lat + ", " + lon);

        String description = descriptionAreaEdit != null ? descriptionAreaEdit.getText() : descriptionArea.getText();

        // Bundle everything into the updated object
        return new CrimeRecord(
                original.getId(),
                categoryComboBox.getValue(),
                newTimestamp,
                lat,
                lon,
                description,
                anonymousCheckBox.isSelected() ? "" : original.getReporter(),
                original.isActioned() // Preserve original raw reporter data (username/null)
        );
    }

    /**
     * Clears all form fields and resets them to default values.
     */
    private void clearForm() {
        if (detailCategoryLabel != null) detailCategoryLabel.setText("-");
        if (detailDateLabel     != null) detailDateLabel.setText("-");
        if (detailStatusLabel   != null) detailStatusLabel.setText("-");
        updateSeverityDisplay(null);
        categoryComboBox.setValue(null);
        datePicker.setValue(null);
        hourBox.setValue(null);
        minuteBox.setValue(null);
        ampmBox.setValue(null);
        locationField.clear();
        descriptionArea.clear();
        if (locationFieldEdit   != null) locationFieldEdit.clear();
        if (descriptionAreaEdit != null) descriptionAreaEdit.clear();
        reporterField.clear();
        setFormEditable(true);
    }

    /**
     * Enables or disables form fields based on edit mode.
     * @param editable true to make fields editable, false to lock them as read-only
     */
    private void setFormEditable(boolean editable) {
        // Swap between read-only view panel and editable form panel
        if (viewScrollPane != null) {
            viewScrollPane.setVisible(!editable);
            viewScrollPane.setManaged(!editable);
        }
        if (editScrollPane != null) {
            editScrollPane.setVisible(editable);
            editScrollPane.setManaged(editable);
        }
    }

    /**
     * Updates selection after changes, selecting the first item or clearing the form.
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
        // Autocomplete attaches to the editable location field in the new-report form
        TextField autoField = locationFieldEdit != null ? locationFieldEdit : locationField;

        autoField.textProperty().addListener((obs, oldText, newText) -> {

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

        autoField.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (!focused) {
                suggestionsPopup.hide();
            }
        });
    }

    /**
     * Runs an address search in the background and passes results to showSuggestions on the UI thread.
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
     * Populates and displays the address autocomplete dropdown below the location field.
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
                if (locationFieldEdit != null) locationFieldEdit.setText(suggestion);
                locationField.setText(suggestion);
                suggestionsPopup.hide();
            });

            suggestionsPopup.getItems().add(item);
        }

        TextField anchorField = locationFieldEdit != null && locationFieldEdit.isVisible()
                ? locationFieldEdit : locationField;
        if (!suggestionsPopup.isShowing()) {
            suggestionsPopup.show(anchorField, Side.BOTTOM, 0, 0);
        }
    }
}
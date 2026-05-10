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
 */
public class MyReportsController {

    @FXML private ListView<CrimeRecord> crimeListView;
    @FXML private TableView<CrimeRecord> crimeTable;
    @FXML private Label reportCountLabel;
    @FXML private VBox detailPanel;
    @FXML private Pane detailBackdrop;
    @FXML private Label detailIdLabel;
    @FXML private Label detailCategoryLabel;
    @FXML private Label detailSeverityLabel;
    @FXML private Label detailDateLabel;
    @FXML private TextField detailLocationField;
    @FXML private TextArea detailDescriptionArea;
    @FXML private Label detailStatusLabel;
    @FXML private Button hamburgerBtn;
    @FXML private StackPane myReportsRoot;
    @FXML private NavBarController navBarController;
    @FXML private MenuButton filterMenuButton;
    @FXML private RadioMenuItem severityAllItem, severitySevereItem, severityModerateItem, severityLowItem;
    @FXML private RadioMenuItem crimeTypeAllItem, crimeTypeAssaultItem, crimeTypeTrespassingItem,
            crimeTypeDomesticAbuseItem, crimeTypeHomicideItem;
    @FXML private RadioMenuItem statusAllItem, statusPendingItem, statusActionedItem;
    @FXML private RadioMenuItem dateAllItem, dateTodayItem, dateLast7DaysItem, dateLast30DaysItem;

    private HamburgerMenu hamburgerMenu;
    private IAppDAO dao;
    private IGeocodingService geocoder = new OpenStreetMapGeoCoder();
    private List<CrimeRecord> allMyReports = new ArrayList<>();

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
        applyFilters();

        // Mark Reports tab active in nav bar
        if (navBarController != null) {
            navBarController.setActiveTab("crimes");
        }

        // Build styled list cells
        setupListView();

        // Preload addresses in background
        preloadAddresses(allMyReports);

        // Wire hamburger menu after scene is attached
        Platform.runLater(() -> {
            Stage stage = (Stage) hamburgerBtn.getScene().getWindow();
            hamburgerMenu = new HamburgerMenu(stage);
            hamburgerMenu.setMaxWidth(Double.MAX_VALUE);
            hamburgerMenu.setMaxHeight(Double.MAX_VALUE);
            myReportsRoot.getChildren().add(hamburgerMenu);
            hamburgerBtn.setOnAction(e -> hamburgerMenu.toggle());
        });
    }

    // ── List setup ────────────────────────────────────────────────────

    /**
     * Configures the cell factory for the crime list view.
     *
     * Each cell displays a severity colour dot, crime category, dispatch
     * status, geocoded location, and a relative timestamp. Selecting a cell
     * populates and slides up the detail panel.
     */
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
        List<CrimeRecord> filteredReports = allMyReports.stream()
                .filter(this::matchesSeverityFilter)
                .filter(this::matchesCrimeTypeFilter)
                .filter(this::matchesStatusFilter)
                .filter(this::matchesDateFilter)
                .toList();

        crimeTable.getItems().setAll(filteredReports);
        crimeListView.getItems().setAll(filteredReports);
        reportCountLabel.setText(filteredReports.size() + " report" + (filteredReports.size() == 1 ? "" : "s"));
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
                populateDetailPanel(newVal);
                showDetailPanel();
            }
        });
    }

    // ── Detail panel ─────────────────────────────────────────────────

    /**
     * Populates all fields in the detail panel with data from the selected crime record.
     *
     * If a cached address exists for the record it is shown immediately.
     * Otherwise, the raw coordinates are displayed while a background thread
     * performs reverse geocoding and updates the field when complete.
     *
     * @param crime the selected {@link CrimeRecord} whose details are to be displayed
     */
    private void populateDetailPanel(CrimeRecord crime) {
        detailIdLabel.setText(String.valueOf(crime.getId()));
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
    }

    /**
     * Makes the detail panel visible and animates it sliding up from the bottom of the screen.
     * The semi-transparent backdrop is also shown to focus attention on the panel.
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

    // ── Actions ───────────────────────────────────────────────────────

    /**
     * Navigates to the report submission view.
     */
    @FXML
    public void onSubmitNewReport() {
        Stage stage = (Stage) hamburgerBtn.getScene().getWindow();
        UIUtils.switchScene(stage, "crimes-view.fxml");
    }

    // ── Helpers ───────────────────────────────────────────────────────

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

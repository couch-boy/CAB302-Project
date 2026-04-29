package com.example.cab302project;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

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

    // FXML bindings
    @FXML private ListView<CrimeRecord> crimeListView;
    @FXML private TableView<CrimeRecord> crimeTable;
    @FXML private Label reportCountLabel;

    // Detail panel
    @FXML private VBox detailPanel;
    @FXML private Pane detailBackdrop;
    @FXML private Label detailIdLabel;
    @FXML private Label detailCategoryLabel;
    @FXML private Label detailSeverityLabel;
    @FXML private Label detailDateLabel;
    @FXML private TextField detailLocationField;
    @FXML private TextArea detailDescriptionArea;
    @FXML private Label detailStatusLabel;

    // App bar / nav
    @FXML private Button hamburgerBtn;
    @FXML private StackPane myReportsRoot;
    @FXML private NavBarController navBarController;

    private HamburgerMenu hamburgerMenu;
    private IAppDAO dao;
    private IGeocodingService geocoder = new OpenStreetMapGeoCoder();
    private static final Map<Integer, String> addressCache = new HashMap<>();

    /**
     * Initialises the controller with the shared database instance.
     */
    public MyReportsController() {

        this.dao = HelloApplication.DATABASE;
    }
    /**
     * Loads the current user's reports, sets up UI components,
     * and prepares the list view and navigation state.
     */
    @FXML
    public void initialize() {
        // Load only this user's reports
        String currentUsername = UserSession.getInstance().getUser().getUsername();
        List<CrimeRecord> myReports = dao.getAllCrimes().stream()
                .filter(c -> currentUsername.equals(c.getReporter()))
                .toList();

        crimeTable.getItems().setAll(myReports);
        crimeListView.getItems().setAll(myReports);

        // Update count label
        reportCountLabel.setText(myReports.size() + " report" + (myReports.size() == 1 ? "" : "s"));

        // Mark Reports tab active in nav bar
        if (navBarController != null) {
            navBarController.setActiveTab("crimes");
        }

        // Build styled list cells
        setupListView();

        // Preload addresses in background
        preloadAddresses(myReports);

        // Wire hamburger menu
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
     * Configures the list view to display styled crime records.
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
                populateDetailPanel(newVal);
                showDetailPanel();
            }
        });
    }

    // ── Detail panel ─────────────────────────────────────────────────
    /**
     * Populates the detail panel with selected crime information.
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

        // Colour severity label
        String colour = switch (crime.getCategory().getSeverity()) {
            case CRITICAL -> "#DC143C";
            case MEDIUM   -> "#FF8C00";
            default       -> "#B8860B";
        };
        detailSeverityLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + colour + ";");
    }

    /**
     * Displays the detail panel with an animation.
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
     * Closes the detail panel and clears the current selection.
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
     * Loads and caches addresses for crimes in the background.
     */
    private void preloadAddresses(List<CrimeRecord> crimes) {
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
                    } catch (Exception ignored) {}
                }
            }
        }).start();
    }
    /**
     * Returns a human-readable relative time for a given date.
     */
    private String getRelativeTime(java.time.LocalDateTime dt) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(
                dt.toLocalDate(), java.time.LocalDate.now());
        if (days == 0) return "Today";
        if (days == 1) return "1 day ago";
        return days + " days ago";
    }
}
package com.example.cab302project;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

/**
 * Controller for the User Profile screen (profile-view.fxml).
 *
 * Allows the logged-in user to view and update their personal details
 * including email, phone number, home location, password, and dark mode
 * preference. Changes are validated before being persisted to the database.
 *
 * The screen is accessible to both regular users and police officers.
 * The appropriate hamburger menu variant ({@link HamburgerMenu} or
 * {@link PoliceHamburgerMenu}) is selected at runtime based on the
 * current user's role from {@link UserSession}.
 */
public class ProfileController {

    @FXML private Label usernameLabel;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField locationField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox darkModeCheckBox;
    @FXML private NavBarController navBarController;
    @FXML private Button hamburgerBtn;
    @FXML private StackPane profileRoot;

    /**
     * Holds either a {@link HamburgerMenu} or {@link PoliceHamburgerMenu} depending
     * on the logged-in user's role. Typed as {@link StackPane} (the shared superclass)
     * to avoid casting at the field level.
     */
    private StackPane hamburgerMenu;
    private IAppDAO dao;
    private User currentUser;
    private IGeocodingService geocoder = new OpenStreetMapGeoCoder();
    private final ContextMenu suggestionsPopup = new ContextMenu();
    private final PauseTransition suggestionDelay = new PauseTransition(Duration.millis(400));

    /**
     * Constructs a new ProfileController and initialises
     * the DAO from the main application database instance.
     */
    public ProfileController() {
        this.dao = HelloApplication.DATABASE;
    }

    /**
     * Initialises the screen after the FXML has loaded.
     *
     * Loads the current user from the session, populates the form fields,
     * marks the Profile tab active in the nav bar, and wires up the correct
     * hamburger menu variant based on the user's role.
     */
    @FXML
    public void initialize() {
        // Load the current user from the active session
        currentUser = UserSession.getInstance().getUser();

        if (currentUser != null) {
            populateFields();
        }

        setupAddressAutocomplete();

        // Mark Profile tab as active in bottom nav
        if (navBarController != null) {
            navBarController.setActiveTab("profile");
        }

        // Wire hamburger menu after scene is attached
        // Platform.runLater ensures getScene().getWindow() is not null at the time of access
        Platform.runLater(() -> {
            Stage stage = (Stage) hamburgerBtn.getScene().getWindow();

            // Use the police drawer for police officers, standard drawer for regular users
            if (UserSession.isPolice()) {
                hamburgerMenu = new PoliceHamburgerMenu(stage);
            } else {
                hamburgerMenu = new HamburgerMenu(stage);
            }

            hamburgerMenu.setMaxWidth(Double.MAX_VALUE);
            hamburgerMenu.setMaxHeight(Double.MAX_VALUE);
            profileRoot.getChildren().add(hamburgerMenu);

            // Toggle the correct menu type at runtime using pattern matching
            hamburgerBtn.setOnAction(e -> {
                if (hamburgerMenu instanceof HamburgerMenu hm) {
                    hm.toggle();
                } else if (hamburgerMenu instanceof PoliceHamburgerMenu phm) {
                    phm.toggle();
                }
            });
        });
    }

    /**
     * Validates and saves the user's updated profile information to the database.
     *
     * Reads form values, validates email and phone format and coordinate structure,
     * then updates the {@link User} object and persists it via the DAO. Displays
     * an appropriate alert for success, validation errors, or database failures.
     */
    @FXML
    public void onSave() {
        try {
            updateUserFromForm();

            if (dao.updateUser(currentUser)) {
                UIUtils.showAlert(Alert.AlertType.INFORMATION, "Success", "Profile updated successfully.");
                populateFields(); // Refresh UI to confirm saved state
            } else {
                UIUtils.showAlert(Alert.AlertType.ERROR, "Database Error", "Could not save changes.");
            }
        } catch (IllegalArgumentException e) {
            UIUtils.showAlert(Alert.AlertType.WARNING, "Validation Error", e.getMessage());
        } catch (Exception e) {
            UIUtils.showAlert(Alert.AlertType.ERROR, "Input Error", "Please check your coordinate format.");
        }
    }

    /**
     * Populates all form fields with the current user's stored data.
     * The location field is reverse-geocoded in the background so a readable
     * address is shown rather than raw coordinates. The password field is
     * intentionally cleared for security.
     */
    private void populateFields() {
        usernameLabel.setText("Logged in as " + currentUser.getUsername());
        emailField.setText(currentUser.getEmail());
        phoneField.setText(currentUser.getPhone());
        darkModeCheckBox.setSelected(currentUser.isDarkMode());
        passwordField.clear();

        // Show coordinates immediately, then replace with address in background
        locationField.setText(String.format("%.4f, %.4f",
                currentUser.getHomeLatitude(), currentUser.getHomeLongitude()));

        new Thread(() -> {
            try {
                String address = geocoder.reverseGeocode(
                        currentUser.getHomeLatitude(), currentUser.getHomeLongitude());
                Platform.runLater(() -> locationField.setText(address));
            } catch (Exception ignored) {}
        }).start();
    }

    /**
     * Reads and validates the form field values, then applies them to the
     * current {@link User} object in the active session.
     *
     * The location field accepts either a plain-text address (which is
     * forward-geocoded via Nominatim) or a raw "lat, lon" coordinate pair.
     * Password is only updated if a non-empty value was entered.
     *
     * @throws IllegalArgumentException if email format, phone format, or address is invalid
     * @throws Exception if geocoding fails
     */
    private void updateUserFromForm() throws Exception {
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String newPassword = passwordField.getText();
        String locationText = locationField.getText().trim();

        if (!UIUtils.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format.");
        }
        if (!UIUtils.isValidPhone(phone)) {
            throw new IllegalArgumentException("Phone number must be 10 digits.");
        }
        if (locationText.isEmpty()) {
            throw new IllegalArgumentException("Please enter a home location.");
        }

        double lat, lon;

        // If it looks like "lat, lon" coordinates, parse directly; otherwise geocode
        String[] parts = locationText.split(",\\s*");
        if (parts.length == 2 && isNumeric(parts[0]) && isNumeric(parts[1])) {
            lat = Double.parseDouble(parts[0]);
            lon = Double.parseDouble(parts[1]);
        } else {
            double[] coords = geocoder.geocodeAddress(locationText);
            lat = coords[0];
            lon = coords[1];
        }

        if (newPassword != null && !newPassword.isEmpty()) {
            String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            currentUser.setPassword(hashedPassword);
        }

        currentUser.setEmail(email);
        currentUser.setPhone(phone);
        currentUser.setHomeLocation(lat, lon);
        currentUser.setDarkMode(darkModeCheckBox.isSelected());
    }

    /** Returns true if the string can be parsed as a double. */
    private boolean isNumeric(String s) {
        try { Double.parseDouble(s); return true; }
        catch (NumberFormatException e) { return false; }
    }

    /**
     * Wires up address autocomplete for the location field.
     * Suggestions are fetched from Nominatim after a short debounce delay.
     */
    private void setupAddressAutocomplete() {
        locationField.textProperty().addListener((obs, oldText, newText) -> {
            suggestionDelay.stop();
            if (newText == null || newText.trim().length() < 3) {
                suggestionsPopup.hide();
                return;
            }
            // Don't trigger suggestions if the text looks like raw coordinates
            String[] parts = newText.split(",\\s*");
            if (parts.length == 2 && isNumeric(parts[0])) {
                suggestionsPopup.hide();
                return;
            }
            suggestionDelay.setOnFinished(e -> fetchSuggestions(newText.trim()));
            suggestionDelay.playFromStart();
        });

        locationField.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (!focused) suggestionsPopup.hide();
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
            javafx.scene.control.Label entryLabel = new javafx.scene.control.Label(suggestion);
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
    }}
package com.example.cab302project;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

/**
 * Controller class for the User Registration screen (register-view.fxml).
 *
 * Handles the creation of new user accounts, input validation (email and phone),
 * and navigation back to the login screen. Includes address autocomplete for
 * the home location field, which is forward-geocoded to coordinates on submit.
 */
public class RegisterController {

    /** Input field for the desired username. */
    @FXML private TextField usernameField;

    /** Input field for the account password. */
    @FXML private PasswordField passwordField;

    /** Input field for the user's email address. */
    @FXML private TextField emailField;

    /** Input field for the user's phone number. */
    @FXML private TextField phoneField;

    /** Input field for the user's home address with autocomplete. */
    @FXML private TextField locationField;

    /** Data Access Object used for persisting new user records. */
    private IAppDAO dao;

    private IGeocodingService geocoder = new OpenStreetMapGeoCoder();
    private final ContextMenu suggestionsPopup = new ContextMenu();
    private final PauseTransition suggestionDelay = new PauseTransition(Duration.millis(400));

    /**
     * Initializes a new RegisterController.
     * Links the controller to the global {@link IAppDAO} instance defined in {@link HelloApplication}.
     */
    public RegisterController() {
        this.dao = HelloApplication.DATABASE;
    }

    /**
     * Wires up address autocomplete after FXML has loaded.
     */
    @FXML
    public void initialize() {
        setupAddressAutocomplete();
    }

    /**
     * Handles the registration logic when the user submits the form.
     * The home location field accepts a plain-text address which is
     * forward-geocoded to coordinates. If left blank, Brisbane CBD is used as default.
     */
    @FXML
    public void onRegisterAccount() {
        String username = usernameField.getText().trim();
        String plaintextPassword = passwordField.getText();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String locationText = locationField.getText().trim();

        if (username.isEmpty() || plaintextPassword.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            UIUtils.showAlert(AlertType.WARNING, "Registration Error", "All fields are required.");
            return;
        }

        if (!UIUtils.isValidEmail(email)) {
            UIUtils.showAlert(AlertType.ERROR, "Invalid Email", "Please enter a valid email address.");
            return;
        }

        if (!UIUtils.isValidPhone(phone)) {
            UIUtils.showAlert(AlertType.ERROR, "Invalid Phone", "Phone number must be 10 digits.");
            return;
        }

        // Geocode address in background, then register
        String hashedPassword = BCrypt.hashpw(plaintextPassword, BCrypt.gensalt());

        new Thread(() -> {
            double lat = -27.4709;
            double lon = 153.0235;

            if (!locationText.isEmpty()) {
                try {
                    double[] coords = geocoder.geocodeAddress(locationText);
                    lat = coords[0];
                    lon = coords[1];
                } catch (Exception e) {
                    Platform.runLater(() ->
                            UIUtils.showAlert(AlertType.WARNING, "Location Not Found",
                                    "Could not find that address. Using Brisbane CBD as your home location.")
                    );
                }
            }

            final double finalLat = lat;
            final double finalLon = lon;

            Platform.runLater(() -> {
                User newUser = new User(username, hashedPassword, email, phone,
                        finalLat, finalLon, false, UserType.REGULAR);

                boolean success = dao.addUser(newUser);

                if (success) {
                    UIUtils.showAlert(AlertType.INFORMATION, "Success", "Account created! You can now login.");
                    onReturnToLogin();
                } else {
                    UIUtils.showAlert(AlertType.ERROR, "Database Error", "Username already exists.");
                }
            });
        }).start();
    }

    /**
     * Navigates the application back to the login screen.
     */
    @FXML
    public void onReturnToLogin() {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        UIUtils.switchScene(stage, "login-view.fxml");
    }

    /** Action handler for the Login tab button. */
    @FXML
    public void onTabLogin() {
        onReturnToLogin();
    }

    /** Action handler for the Sign Up tab button. */
    @FXML
    public void onTabSignup() {}

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
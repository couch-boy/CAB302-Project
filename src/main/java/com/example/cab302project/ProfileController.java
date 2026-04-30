package com.example.cab302project;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

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
     * The password field is intentionally cleared for security — it never
     * displays the existing password.
     */
    private void populateFields() {
        usernameLabel.setText("Logged in as " + currentUser.getUsername());
        emailField.setText(currentUser.getEmail());
        phoneField.setText(currentUser.getPhone());
        darkModeCheckBox.setSelected(currentUser.isDarkMode());
        locationField.setText(String.format("%.4f, %.4f",
                currentUser.getHomeLatitude(), currentUser.getHomeLongitude()));
        passwordField.clear();
    }

    /**
     * Reads and validates the form field values, then applies them to the
     * current {@link User} object in the active session.
     *
     * Password is only updated if a non-empty value was entered, avoiding
     * accidental overwrites with an empty string. Coordinates are split on a
     * comma with optional surrounding whitespace.
     *
     * @throws IllegalArgumentException if email format, phone format, or coordinate structure is invalid
     * @throws NumberFormatException    if latitude or longitude cannot be parsed as a double
     */
    private void updateUserFromForm() throws Exception {
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String newPassword = passwordField.getText();

        // Split into lat/lon on a comma followed by zero or more whitespace characters
        String[] coords = locationField.getText().split(",\\s*");

        if (!UIUtils.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format.");
        }
        if (!UIUtils.isValidPhone(phone)) {
            throw new IllegalArgumentException("Phone number must be 10 digits.");
        }
        if (coords.length != 2) {
            throw new IllegalArgumentException("Location must be 'lat, lon'.");
        }

        double lat = Double.parseDouble(coords[0]);
        double lon = Double.parseDouble(coords[1]);

        // Only update password if a new one was actually entered
        if (newPassword != null && !newPassword.isEmpty()) {

            // Hash the plaintext password - salt is generated and embedded automatically
            String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());

            currentUser.setPassword(hashedPassword);
        }

        currentUser.setEmail(email);
        currentUser.setPhone(phone);
        currentUser.setHomeLocation(lat, lon);
        currentUser.setDarkMode(darkModeCheckBox.isSelected());
    }
}
package com.example.cab302project;

import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Controller class for the User Registration screen (register-view.fxml).
 *
 * Handles the creation of new user accounts, input validation (email and phone),
 * and navigation back to the login screen.
 */
public class RegisterController {

    /** Input field for the desired username. */
    @FXML
    private TextField usernameField;

    /** Input field for the account password. */
    @FXML
    private PasswordField passwordField;

    /** Input field for the user's email address. */
    @FXML
    private TextField emailField;

    /** Input field for the user's phone number. */
    @FXML
    private TextField phoneField;

    /** Data Access Object used for persisting new user records. */
    private IAppDAO dao;

    /**
     * Initializes a new RegisterController.
     * Links the controller to the global {@link IAppDAO} instance defined in {@link HelloApplication}.
     */
    public RegisterController() {
        //get main application dao instance
        this.dao = HelloApplication.DATABASE;
    }

    /**
     * Handles the registration logic when the user submits the form.
     * <p>
     * This method performs the following steps:
     * </p>
     * <ul>
     *     <li>Trims and extracts text from all input fields.</li>
     *     <li>Checks for empty fields.</li>
     *     <li>Validates the format of the email and phone number via {@link UIUtils}.</li>
     *     <li>Creates a new {@link User} object with default coordinates (Brisbane CBD).</li>
     *     <li>Attempts to persist the user via the DAO, checking for username uniqueness.</li>
     * </ul>
     */
    @FXML
    public void onRegisterAccount() {
        // Trim whitespace from end of non-password fields
        String username = usernameField.getText().trim();
        String plaintextPassword = passwordField.getText();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();

        //check for empty fields
        if (username.isEmpty() || plaintextPassword.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            UIUtils.showAlert(AlertType.WARNING, "Registration Error", "All fields are required.");
            return;
        }

        //validate email
        if (!UIUtils.isValidEmail(email)) {
            UIUtils.showAlert(AlertType.ERROR, "Invalid Email", "Please enter a valid email address.");
            return;
        }

        //validate phone
        if (!UIUtils.isValidPhone(phone)) {
            UIUtils.showAlert(AlertType.ERROR, "Invalid Phone", "Phone number must be 10 digits.");
            return;
        }

        // Hash the plaintext password - salt is generated and embedded automatically
        String hashedPassword = BCrypt.hashpw(plaintextPassword, BCrypt.gensalt());

        // Create new regular user User object using Brisbane CBD lat/lon and default darkmode to off with hashed password
        User newUser = new User(username, hashedPassword, email, phone, -27.4709, 153.0235, false, UserType.REGULAR);

        // Attempt to add to db
        boolean success = dao.addUser(newUser);

        if (success) {
            UIUtils.showAlert(AlertType.INFORMATION, "Success", "Account created! You can now login.");
            //return to login screen
            onReturnToLogin();
        } else {
            UIUtils.showAlert(AlertType.ERROR, "Database Error", "Username already exists.");
        }
    }

    /**
     * Navigates the application back to the login screen.
     * Retrieves the current {@link Stage} from the UI context to perform the scene switch.
     */
    @FXML
    public void onReturnToLogin() {
        //get the current stage (window) by referencing a ui element
        Stage stage = (Stage) usernameField.getScene().getWindow();
        //load login view
        UIUtils.switchScene(stage, "login-view.fxml");
    }

    /**
     * Action handler for the Login tab button.
     * Triggers the navigation back to the login view.
     */
    @FXML
    public void onTabLogin() {
        onReturnToLogin();
    }

    /**
     * Action handler for the Sign Up tab button.
     * Currently a no-op as the user is already on the registration screen.
     */
    @FXML
    public void onTabSignup() {}
}
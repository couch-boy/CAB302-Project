package com.example.cab302project;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;

public class RegisterController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneField;

    private SqliteDAO dao;

    public RegisterController() {
        //get main application dao instance
        this.dao = HelloApplication.DATABASE;
    }

    public void onRegisterAccount() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();

        //check for empty fields
        if (username.isEmpty() || password.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            showAlert(AlertType.WARNING, "Registration Error", "All fields are required.");
            return;
        }

        //validate email
        if (!isValidEmail(email)) {
            showAlert(AlertType.ERROR, "Invalid Email", "Please enter a valid email address.");
            return;
        }

        //validate phone
        if (!isValidPhone(phone)) {
            showAlert(AlertType.ERROR, "Invalid Phone", "Phone number must be 10 digits.");
            return;
        }

        //attempt to add user to database
        boolean success = dao.addUser(username, password, email, phone);

        if (success) {
            showAlert(AlertType.INFORMATION, "Success", "Account created! You can now login.");
            //return to login screen
            onReturnToLogin();
        } else {
            showAlert(AlertType.ERROR, "Database Error", "Username already exists.");
        }


    }

    @FXML
    public void onReturnToLogin() {
        try {
            //get the current stage (window) by referencing a ui element
            Stage stage = (Stage) usernameField.getScene().getWindow();
            //load login view
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("login-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), HelloApplication.WIDTH, HelloApplication.HEIGHT);
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isValidEmail(String email) {
        //basic regex email validation. checks for characters, an @ symbol, and a domain
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }

    private boolean isValidPhone(String phone) {
        //checks if phone is exactly 10 digits
        return phone.matches("\\d{10}");
    }

    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

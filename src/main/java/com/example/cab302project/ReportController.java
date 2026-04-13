package com.example.cab302project;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

import java.io.IOException;
public class ReportController {
    @FXML
    private ComboBox<String> typeField;

    @FXML
    private TextArea descriptionField;

    @FXML
    private TextField locationField;

    private SqliteDAO dao;

    public ReportController() {
        this.dao = HelloApplication.DATABASE;
    }
    @FXML
    public void initialize() {
        typeField.getItems().addAll("Theft",
                "Vandalism",
                "Assualt",
                "Suspicious Activity");
    }
    @FXML
    public void onSubmitReport() {
        String type = typeField.getValue();
        String description = descriptionField.getText();
        String location = locationField.getText();

        if (type == null || description.isEmpty() || location.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Error", "All fields are required.");
            return;
        }
        User user = UserSession.getInstance().getUser();
        boolean success = dao.addReport(
                user.getUsername(),type ,description,location
        );
        
        if (success) {
            showAlert(Alert.AlertType.INFORMATION,"Sucsess","Report Submiited!");
            descriptionField.clear();
            locationField.clear();
            typeField.setValue(null);

        }else{
            showAlert(Alert.AlertType.ERROR,"Error","Failed to submit report!");

        }
    }
    @FXML
    public void onBack() throws IOException {
        Stage stage =(Stage) locationField.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("dashboard-view.fxml"));
        Scene scene = new Scene(loader.load(), HelloApplication.WIDTH, HelloApplication.HEIGHT);
        stage.setScene(scene);
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

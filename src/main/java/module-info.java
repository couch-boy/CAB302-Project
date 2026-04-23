module com.example.cab302project {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    requires org.json;



    opens com.example.cab302project to javafx.fxml;
    exports com.example.cab302project;
}
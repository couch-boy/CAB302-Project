module com.example.cab302project {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    requires org.json;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires jbcrypt;

    opens com.example.cab302project to javafx.fxml;
    exports com.example.cab302project;
}
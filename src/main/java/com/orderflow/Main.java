package com.orderflow;

import com.orderflow.db.DatabaseConnection;
import com.orderflow.util.AppExecutor;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Application entry point.
 *
 * Note on "C and Java Compilation": unlike a C program, which a compiler
 * (e.g. gcc) turns directly into machine code for one specific operating
 * system, this Java program is compiled by javac into platform-independent
 * .class bytecode. That bytecode is then executed by the Java Virtual
 * Machine (JVM), which is why the same OrderFlow.jar can run on Windows,
 * Linux or macOS without being recompiled. See README.md for the full
 * compile/run commands used for this project.
 */
public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        primaryStage.getIcons().add(new Image(Main.class.getResourceAsStream("/com/orderflow/images/logo.png")));

        // Touch the database once at startup so the schema/seed data
        // is created before the login screen even appears.
        DatabaseConnection.getConnection();

        switchScene("/com/orderflow/fxml/Login.fxml", "OrderFlow - Login");
    }

    /** Lets controllers navigate between full-screen views (Login -> Main app). */
    public static void switchScene(String fxmlPath, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1000, 650);
        scene.getStylesheets().add(Main.class.getResource("/com/orderflow/css/style.css").toExternalForm());

        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    @Override
    public void stop() {
        AppExecutor.shutdown();
        DatabaseConnection.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

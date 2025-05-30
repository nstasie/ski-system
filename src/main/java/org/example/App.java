package org.example;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;

import java.io.IOException;

import static org.example.Services.initDB;

import org.example.controller.*;
import org.example.model.User;

public class App extends Application {
    private Stage primaryStage;
    private Scene loginScene;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;

        try {
            Logger.logSystemEvent("APPLICATION_START", "Ski Service Application starting");
            initDB();
            showLogin();
            primaryStage.setTitle("Ski Service");
            primaryStage.show();
            Logger.logSystemEvent("APPLICATION_READY", "Application UI loaded successfully");

        } catch (Exception e) {
            Logger.logError("APPLICATION_START", "SYSTEM", e.getMessage(), "Failed to start application");
            throw e;
        }
    }

    @Override
    public void stop() throws Exception {
        Logger.logSystemEvent("APPLICATION_STOP", "Ski Service Application shutting down");
        super.stop();
    }

    private void showLogin() throws IOException {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            loginScene = new Scene(root, 300, 180);
            loginScene.getStylesheets().add(
                    getClass().getResource("/css/styles.css").toExternalForm());
            LoginController loginCtrl = loader.getController();
            loginCtrl.setApp(this);
            primaryStage.setScene(loginScene);
            Logger.logSystemEvent("UI_LOAD", "Login screen loaded");
        } catch (IOException e) {
            Logger.logError("UI_LOAD", "SYSTEM", e.getMessage(), "Failed to load login screen");
            throw e;
        }
    }

    public void showMain(User currentUser) throws IOException {
        try {
            Logger.logUserAction("UI_TRANSITION", currentUser.getUsername(), "Transitioning to main application");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Main.fxml"));
            Parent root = loader.load();
            Scene mainScene = new Scene(root, 800, 600);
            mainScene.getStylesheets().add(
                    getClass().getResource("/css/styles.css").toExternalForm());
            MainController mainCtrl = loader.getController();
            mainCtrl.init(primaryStage, loginScene, currentUser);
            primaryStage.setScene(mainScene);
            Logger.logUserAction("UI_LOAD", currentUser.getUsername(), "Main application screen loaded");
        } catch (IOException e) {
            Logger.logError("UI_LOAD", currentUser != null ? currentUser.getUsername() : "UNKNOWN",
                    e.getMessage(), "Failed to load main application");
            throw e;
        }
    }
}
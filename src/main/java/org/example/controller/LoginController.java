package org.example.controller;

import org.example.App;
import org.example.Logger;
import org.example.Services;
import org.example.service.*;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.model.User;

public class LoginController {
    @FXML
    private TextField tfUser;
    @FXML
    private PasswordField tfPass;
    @FXML
    private Button btnLogin, btnReg;
    @FXML
    private Label lblMsg;

    private App app;
    private final AuthService authService = new AuthService();

    public void setApp(App app) {
        this.app = app;
        Logger.logSystemEvent("CONTROLLER_INIT", "LoginController linked to App");
    }

    @FXML
    public void initialize() {
        try {
            Logger.logSystemEvent("CONTROLLER_INIT", "LoginController initialize() started");

            setupEventHandlers();

            Logger.logSystemEvent("CONTROLLER_INIT", "LoginController initialized successfully");
        } catch (Exception e) {
            Logger.logError("CONTROLLER_INIT", "SYSTEM", e.getMessage(), "LoginController initialization failed");
            e.printStackTrace();
        }
    }

    private void setupEventHandlers() {
        btnLogin.setOnAction(e -> handleLogin());
        btnReg.setOnAction(e -> handleRegistration());

        // Видалення повідомлення про помилку, коли користувач починає вводити текст
        tfUser.textProperty().addListener((obs, oldText, newText) -> clearMessage());
        tfPass.textProperty().addListener((obs, oldText, newText) -> clearMessage());
    }

    private void handleLogin() {
        String username = tfUser.getText();
        String password = tfPass.getText();
        String params = String.format("username=%s", username);

        Logger.logUserAction("LOGIN_ATTEMPT", username, params);

        try {
            var validationResult = authService.validateLoginCredentials(username, password);
            if (!validationResult.isValid()) {
                Logger.logError("LOGIN_ATTEMPT", username, validationResult.getMessage(), params);
                showMessage(validationResult.getMessage(), MessageType.ERROR);
                return;
            }

            if (Services.AuthService.login(username, password)) {
                User user = Services.AuthService.getCurrentUser();
                Logger.logUserAction("LOGIN_SUCCESS_UI", user.getUsername(),
                        String.format("role=%s", user.getRole()));

                clearForm();
                app.showMain(user);
            } else {
                Logger.logUserAction("LOGIN_FAILED_UI", username, params);
                showMessage("Invalid username or password", MessageType.ERROR);
            }

        } catch (Exception ex) {
            Logger.logError("LOGIN_ATTEMPT", username, ex.getMessage(), params);
            showMessage("Login error: " + ex.getMessage(), MessageType.ERROR);
        }
    }

    private void handleRegistration() {
        String username = tfUser.getText();
        String password = tfPass.getText();
        String params = String.format("username=%s", username);

        Logger.logUserAction("REGISTER_ATTEMPT", username, params);

        try {
            var validationResult = authService.validateRegistrationCredentials(username, password);
            if (!validationResult.isValid()) {
                Logger.logError("REGISTER_ATTEMPT", username, validationResult.getMessage(), params);
                showMessage(validationResult.getMessage(), MessageType.ERROR);
                return;
            }

            // перевірка, чи пароль збігається з іменем користувача
            if (authService.isPasswordSameAsUsername(username, password)) {
                Logger.logError("REGISTER_ATTEMPT", username, "Password same as username", params);
                showMessage("Password cannot be the same as username", MessageType.ERROR);
                return;
            }

            var securityAssessment = authService.assessCredentialSecurity(username, password);
            if (securityAssessment.getLevel() == AuthService.SecurityLevel.VERY_WEAK) {
                showMessage("Warning: " + securityAssessment.getMessage(), MessageType.WARNING);
            }

            String sanitizedUsername = authService.sanitizeUsername(username);

            boolean success = Services.AuthService.register(sanitizedUsername, password);
            if (success) {
                Logger.logUserAction("REGISTER_SUCCESS", sanitizedUsername, params);
                showMessage("Registration successful! You can now log in.", MessageType.SUCCESS);
                clearForm();
            } else {
                Logger.logError("REGISTER_ATTEMPT", username, "Registration failed", params);
                showMessage("Registration failed. Please try again.", MessageType.ERROR);
            }

        } catch (Exception ex) {
            String errorMessage = ex.getMessage();
            if (errorMessage != null && errorMessage.contains("UNIQUE constraint failed")) {
                Logger.logError("REGISTER_ATTEMPT", username, "Username already exists", params);
                showMessage("Username already exists. Please choose a different one.", MessageType.ERROR);
            } else {
                Logger.logError("REGISTER_ATTEMPT", username, errorMessage, params);
                showMessage("Registration error: " + errorMessage, MessageType.ERROR);
            }
        }
    }

    private void showMessage(String message, MessageType type) {
        lblMsg.setText(message);

        switch (type) {
            case SUCCESS:
                lblMsg.setStyle("-fx-text-fill: green;");
                break;
            case WARNING:
                lblMsg.setStyle("-fx-text-fill: orange;");
                break;
            case ERROR:
            default:
                lblMsg.setStyle("-fx-text-fill: red;");
                break;
        }
    }

    private void clearForm() {
        tfUser.clear();
        tfPass.clear();
        clearMessage();
    }

    private void clearMessage() {
        lblMsg.setText("");
        lblMsg.setStyle("");
    }

    private enum MessageType {
        SUCCESS, WARNING, ERROR
    }
}
package org.example.controller;

import org.example.Logger;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.scene.Scene;
import org.example.model.User;

import java.io.IOException;

public class MainController {
    @FXML
    private TabPane tabPane;
    @FXML
    private Tab tabDashboard, tabBooking, tabEquipment, tabInstructors, tabFinance, tabAnalytics;
    @FXML
    private Label lblWelcome;
    @FXML
    private Button btnLogout;

    private Stage primaryStage;
    private Scene loginScene;
    private User currentUser;
    private DashboardController dashboardController;
    private BookingController bookingController;
    private EquipmentController equipmentController;
    private InstructorController instructorController;

    public void init(Stage primaryStage, Scene loginScene, User currentUser) throws IOException {
        this.primaryStage = primaryStage;
        this.loginScene = loginScene;
        this.currentUser = currentUser;

        try {
            Logger.logUserAction("MAIN_INIT", currentUser.getUsername(),
                    String.format("role=%s", currentUser.getRole()));

            lblWelcome.setText("Hello, " + currentUser.getUsername()
                    + " (" + currentUser.getRole() + ")");

            loadUserTabs(currentUser);
            loadAdminTabs(currentUser);

            if (currentUser.getRole().equals("ADMIN")) {
                tabDashboard.setDisable(true);
                tabBooking.setDisable(true);
                tabEquipment.setDisable(true);
                tabInstructors.setDisable(true);

                tabPane.getSelectionModel().select(tabFinance);
                Logger.logUserAction("UI_CONFIG", currentUser.getUsername(), "Admin interface configured");
            } else {
                tabFinance.setDisable(true);
                tabAnalytics.setDisable(true);

                tabPane.getSelectionModel().select(tabDashboard);
                Logger.logUserAction("UI_CONFIG", currentUser.getUsername(), "User interface configured");
            }

            btnLogout.setOnAction(e -> {
                Logger.logUserAction("LOGOUT", currentUser.getUsername(), "User logged out");
                primaryStage.setScene(loginScene);
            });

        } catch (Exception e) {
            Logger.logError("MAIN_INIT", currentUser.getUsername(), e.getMessage(),
                    "Failed to initialize main controller");
            throw e;
        }
    }

    private void loadUserTabs(User currentUser) throws IOException {
        try {
            FXMLLoader dashLoader = new FXMLLoader(getClass().getResource("/fxml/DashboardView.fxml"));
            Parent dashRoot = dashLoader.load();
            dashboardController = dashLoader.getController();
            dashboardController.setCurrentUser(currentUser);
            tabDashboard.setContent(dashRoot);

            FXMLLoader bookingLoader = new FXMLLoader(getClass().getResource("/fxml/BookingView.fxml"));
            Parent bookingRoot = bookingLoader.load();
            bookingController = bookingLoader.getController();
            bookingController.setMainController(this);
            tabBooking.setContent(bookingRoot);

            FXMLLoader equipLoader = new FXMLLoader(getClass().getResource("/fxml/EquipmentView.fxml"));
            Parent equipRoot = equipLoader.load();
            equipmentController = equipLoader.getController();
            equipmentController.setMainController(this);
            tabEquipment.setContent(equipRoot);

            FXMLLoader instrLoader = new FXMLLoader(getClass().getResource("/fxml/InstructorView.fxml"));
            Parent instrRoot = instrLoader.load();
            instructorController = instrLoader.getController();
            instructorController.setMainController(this);
            tabInstructors.setContent(instrRoot);

            Logger.logUserAction("UI_LOAD", currentUser.getUsername(), "User tabs loaded successfully");
        } catch (IOException e) {
            Logger.logError("UI_LOAD", currentUser.getUsername(), e.getMessage(), "Failed to load user tabs");
            throw e;
        }
    }

    private void loadAdminTabs(User currentUser) throws IOException {
        try {
            FXMLLoader finLoader = new FXMLLoader(getClass().getResource("/fxml/FinanceView.fxml"));
            Parent finRoot = finLoader.load();
            FinanceController finCtrl = finLoader.getController();
            finCtrl.setCurrentUser(currentUser);
            tabFinance.setContent(finRoot);

            FXMLLoader anLoader = new FXMLLoader(getClass().getResource("/fxml/AnalyticsView.fxml"));
            Parent anRoot = anLoader.load();
            AnalyticsController anCtrl = anLoader.getController();
            anCtrl.setCurrentUser(currentUser);
            tabAnalytics.setContent(anRoot);

            Logger.logUserAction("UI_LOAD", currentUser.getUsername(), "Admin tabs loaded successfully");
        } catch (IOException e) {
            Logger.logError("UI_LOAD", currentUser.getUsername(), e.getMessage(), "Failed to load admin tabs");
            throw e;
        }
    }

    public void refreshDashboard() {
        try {
            if (dashboardController != null) {
                dashboardController.refresh();
                Logger.logUserAction("DASHBOARD_REFRESH", currentUser.getUsername(), "Dashboard refreshed");
            }
        } catch (Exception e) {
            Logger.logError("DASHBOARD_REFRESH", currentUser.getUsername(), e.getMessage(),
                    "Failed to refresh dashboard");
            e.printStackTrace();
        }
    }
}
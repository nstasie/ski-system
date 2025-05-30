package org.example.controller;

import org.example.Logger;
import org.example.Services;
import org.example.service.*;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.example.model.Equipment;
import org.example.model.UserRental;

public class EquipmentController {
    @FXML
    private TableView<Equipment> tvEquip;
    @FXML
    private TableColumn<Equipment, Integer> colId;
    @FXML
    private TableColumn<Equipment, String> colType;
    @FXML
    private TableColumn<Equipment, String> colSize;
    @FXML
    private TableColumn<Equipment, Integer> colAvail;

    @FXML
    private TableView<UserRental> tvMyRentals;
    @FXML
    private TableColumn<UserRental, String> colMyType;
    @FXML
    private TableColumn<UserRental, String> colMySize;
    @FXML
    private TableColumn<UserRental, String> colMyStatus;

    @FXML
    private ComboBox<String> cbType;
    @FXML
    private ComboBox<String> cbSize;
    @FXML
    private ComboBox<UserRental> cbMyRentals;
    @FXML
    private Button btnRent, btnReturn;

    private MainController mainController;
    private final EquipmentService equipmentService = new EquipmentService();

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        Logger.logSystemEvent("CONTROLLER_INIT", "EquipmentController linked to MainController");
    }

    @FXML
    public void initialize() {
        try {
            Logger.logSystemEvent("CONTROLLER_INIT", "EquipmentController initialize() started");

            setupTables();
            setupControls();
            setupEventHandlers();
            refresh();

            Logger.logSystemEvent("CONTROLLER_INIT", "EquipmentController initialized successfully");
        } catch (Exception e) {
            Logger.logError("CONTROLLER_INIT", "SYSTEM", e.getMessage(), "EquipmentController initialization failed");
            e.printStackTrace();
        }
    }

    private void setupTables() {
        // Таблиця спорядження
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        colAvail.setCellValueFactory(new PropertyValueFactory<>("available"));

        // Таблиця бронювань
        colMyType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colMySize.setCellValueFactory(new PropertyValueFactory<>("size"));
        colMyStatus.setCellValueFactory(new PropertyValueFactory<>("rentedSince"));
    }

    private void setupControls() {
        cbType.setItems(FXCollections.observableArrayList(equipmentService.getAvailableEquipmentTypes()));
        cbType.setPromptText("Select type...");

        cbSize.setItems(FXCollections.observableArrayList(equipmentService.getAvailableSizes()));
        cbSize.setPromptText("Select size...");

        cbMyRentals.setCellFactory(param -> new ListCell<UserRental>() {
            @Override
            protected void updateItem(UserRental item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(equipmentService.createRentalDisplayString(item));
                }
            }
        });

        cbMyRentals.setButtonCell(new ListCell<UserRental>() {
            @Override
            protected void updateItem(UserRental item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Select equipment to return...");
                } else {
                    setText(equipmentService.createRentalDisplayString(item));
                }
            }
        });
    }

    private void setupEventHandlers() {
        cbType.setOnAction(e -> updateButtonStates());
        cbSize.setOnAction(e -> updateButtonStates());
        cbMyRentals.setOnAction(e -> updateButtonStates());

        btnRent.setOnAction(e -> handleRental());
        btnReturn.setOnAction(e -> handleReturn());
    }

    private void handleRental() {
        String username = Services.AuthService.getCurrentUser().getUsername();
        String selectedType = cbType.getValue();
        String selectedSize = cbSize.getValue();
        String params = String.format("type=%s, size=%s", selectedType, selectedSize);

        Logger.logUserAction("EQUIPMENT_RENT_ATTEMPT", username, params);

        try {
            var validationResult = equipmentService.validateRentalRequest(selectedType, selectedSize, username);
            if (!validationResult.isValid()) {
                Logger.logError("EQUIPMENT_RENT_ATTEMPT", username, validationResult.getMessage(), params);
                showAlert(validationResult.getMessage());
                return;
            }

            ObservableList<Equipment> allEquipment = Services.EquipmentService.listAll();
            Equipment availableEquipment = equipmentService.findAvailableEquipment(
                    allEquipment, selectedType, selectedSize);

            if (availableEquipment == null) {
                Logger.logError("EQUIPMENT_RENT_ATTEMPT", username, "No equipment available", params);
                showAlert("No " + selectedType + " equipment available in size " + selectedSize);
                return;
            }

            var availabilityResult = equipmentService.checkAvailability(availableEquipment);
            if (!availabilityResult.isAvailable()) {
                showAlert(availabilityResult.getMessage());
                return;
            }

            // відображення вікна підтвердження
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setHeaderText("Rent Equipment");
            confirm.setContentText("Rent " + selectedType + " (Size: " + selectedSize + ") for $20?");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        String finalParams = params + String.format(", equipment_id=%d", availableEquipment.getId());
                        Services.EquipmentService.rent(availableEquipment.getId(), username);
                        refresh();
                        refreshMainDashboard();
                        showInfo("Equipment rented successfully!");
                        clearRentalForm();
                        Logger.logUserAction("EQUIPMENT_RENT_SUCCESS", username, finalParams);
                    } catch (Exception ex) {
                        Logger.logError("EQUIPMENT_RENT_ATTEMPT", username, ex.getMessage(), params);
                        showAlert("Rental failed: " + ex.getMessage());
                    }
                } else {
                    Logger.logUserAction("EQUIPMENT_RENT_CANCELLED", username, params);
                }
            });

        } catch (Exception ex) {
            Logger.logError("EQUIPMENT_RENT_ATTEMPT", username, ex.getMessage(), params);
            showAlert("Rental operation failed: " + ex.getMessage());
        }
    }

    private void handleReturn() {
        String username = Services.AuthService.getCurrentUser().getUsername();
        UserRental selectedRental = cbMyRentals.getValue();
        String params = selectedRental != null
                ? String.format("equipment_id=%d, type=%s, size=%s", selectedRental.getEquipmentId(),
                        selectedRental.getType(), selectedRental.getSize())
                : "no_selection";

        Logger.logUserAction("EQUIPMENT_RETURN_ATTEMPT", username, params);

        try {
            var validationResult = equipmentService.validateReturnRequest(selectedRental, username);
            if (!validationResult.isValid()) {
                Logger.logError("EQUIPMENT_RETURN_ATTEMPT", username, validationResult.getMessage(), params);
                showAlert(validationResult.getMessage());
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setHeaderText("Return Equipment");
            confirm.setContentText("Return " + equipmentService.createRentalDisplayString(selectedRental) + "?");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        Services.EquipmentService.ret(selectedRental.getEquipmentId(), username);
                        refresh();
                        refreshMainDashboard();
                        showInfo("Equipment returned successfully!");
                        Logger.logUserAction("EQUIPMENT_RETURN_SUCCESS", username, params);
                    } catch (Exception ex) {
                        Logger.logError("EQUIPMENT_RETURN_ATTEMPT", username, ex.getMessage(), params);
                        showAlert("Return failed: " + ex.getMessage());
                    }
                } else {
                    Logger.logUserAction("EQUIPMENT_RETURN_CANCELLED", username, params);
                }
            });

        } catch (Exception ex) {
            Logger.logError("EQUIPMENT_RETURN_ATTEMPT", username, ex.getMessage(), params);
            showAlert("Return operation failed: " + ex.getMessage());
        }
    }

    private void refresh() {
        try {
            String username = Services.AuthService.getCurrentUser().getUsername();
            Logger.logUserAction("EQUIPMENT_REFRESH", username, "Refreshing equipment lists");

            // завантаження всього спорядження
            ObservableList<Equipment> allEquipment = Services.EquipmentService.listAll();
            tvEquip.setItems(allEquipment);

            // завантаження поточних бронювань користувача
            ObservableList<UserRental> currentRentals = Services.EquipmentService.getCurrentRentals(username);
            tvMyRentals.setItems(currentRentals);
            cbMyRentals.setItems(currentRentals);

            updateButtonStates();
        } catch (Exception e) {
            Logger.logError("EQUIPMENT_REFRESH", "SYSTEM", e.getMessage(), "Failed to refresh equipment lists");
            e.printStackTrace();
        }
    }

    private void updateButtonStates() {
        String username = Services.AuthService.getCurrentUser().getUsername();
        ObservableList<UserRental> currentRentals = Services.EquipmentService.getCurrentRentals(username);

        //перевірка, чи досягнуто ліміту оренди
        boolean hasReachedLimit = equipmentService.hasReachedRentalLimit(currentRentals, 5); // Max 5 rentals

        btnRent.setDisable(cbType.getValue() == null || cbSize.getValue() == null || hasReachedLimit);
        btnReturn.setDisable(currentRentals.isEmpty() || cbMyRentals.getValue() == null);

        if (hasReachedLimit) {
            btnRent.setTooltip(new Tooltip("You have reached the maximum rental limit"));
        } else {
            btnRent.setTooltip(null);
        }
    }

    private void clearRentalForm() {
        cbType.setValue(null);
        cbSize.setValue(null);
    }

    private void refreshMainDashboard() {
        try {
            if (mainController != null) {
                mainController.refreshDashboard();
            }
        } catch (Exception e) {
            Logger.logError("DASHBOARD_REFRESH", "SYSTEM", e.getMessage(), "Failed to refresh main dashboard");
            e.printStackTrace();
        }
    }

    private void showAlert(String message) {
        try {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Error");
            alert.setContentText(message);
            alert.showAndWait();
        } catch (Exception e) {
            Logger.logError("UI_ALERT", "SYSTEM", e.getMessage(), "Failed to show alert: " + message);
            e.printStackTrace();
        }
    }

    private void showInfo(String message) {
        try {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("Success");
            alert.setContentText(message);
            alert.showAndWait();
        } catch (Exception e) {
            Logger.logError("UI_INFO", "SYSTEM", e.getMessage(), "Failed to show info: " + message);
            e.printStackTrace();
        }
    }
}
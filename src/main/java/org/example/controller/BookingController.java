package org.example.controller;

import org.example.service.*;
import org.example.Logger;
import org.example.Services;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.example.model.Booking;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class BookingController {
    @FXML
    private TableView<Booking> tvBooking;
    @FXML
    private TableColumn<Booking, Integer> colId;
    @FXML
    private TableColumn<Booking, String> colUser;
    @FXML
    private TableColumn<Booking, String> colSlot;
    @FXML
    private TableColumn<Booking, LocalDateTime> colTime;
    @FXML
    private ComboBox<String> cbSlot;
    @FXML
    private DatePicker dpDate;
    @FXML
    private Button btnBook, btnCancel, btnTransfer;

    private MainController mainController;
    private final BookingService bookingService = new BookingService();

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        Logger.logSystemEvent("CONTROLLER_INIT", "BookingController linked to MainController");
    }

    @FXML
    public void initialize() {
        try {
            Logger.logSystemEvent("CONTROLLER_INIT", "BookingController initialize() started");

            setupTableColumns();
            setupFormControls();
            setupEventHandlers();
            refresh();

            Logger.logSystemEvent("CONTROLLER_INIT", "BookingController initialized successfully");
        } catch (Exception e) {
            Logger.logError("CONTROLLER_INIT", "SYSTEM", e.getMessage(), "BookingController initialization failed");
            e.printStackTrace();
        }
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        colSlot.setCellValueFactory(new PropertyValueFactory<>("slot"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
    }

    private void setupFormControls() {
        cbSlot.setItems(FXCollections.observableArrayList(bookingService.getAvailableTimeSlots()));
        dpDate.setValue(LocalDate.now());
    }

    private void setupEventHandlers() {
        btnBook.setOnAction(e -> handleBooking());
        btnCancel.setOnAction(e -> handleCancellation());
        btnTransfer.setOnAction(e -> handleTransfer());
    }

    private void handleBooking() {
        String currentUsername = Services.AuthService.getCurrentUser().getUsername();
        String selectedSlot = cbSlot.getValue();
        LocalDate selectedDate = dpDate.getValue();
        String params = String.format("slot=%s, date=%s", selectedSlot, selectedDate);

        Logger.logUserAction("BOOKING_ATTEMPT", currentUsername, params);

        try {
            // Підтвердження спроб бронювань
            var validationResult = bookingService.validateBookingRequest(selectedSlot, selectedDate, currentUsername);
            if (!validationResult.isValid()) {
                Logger.logError("BOOKING_ATTEMPT", currentUsername, validationResult.getMessage(), params);
                showAlert(validationResult.getMessage());
                return;
            }

            LocalDateTime bookingTime = bookingService.getTimeFromSlot(selectedSlot, selectedDate);

            Services.BookingService.book(currentUsername, selectedSlot, bookingTime);

            refresh();
            refreshMainDashboard();
            showInfo("Booking created successfully!");
            clearForm();

        } catch (Exception ex) {
            Logger.logError("BOOKING_ATTEMPT", currentUsername, ex.getMessage(), params);
            showAlert("Booking failed: " + ex.getMessage());
        }
    }

    private void handleCancellation() {
        String currentUsername = Services.AuthService.getCurrentUser().getUsername();
        Booking selectedBooking = tvBooking.getSelectionModel().getSelectedItem();
        String params = selectedBooking != null ? String.format("booking_id=%d", selectedBooking.getId())
                : "no_selection";

        Logger.logUserAction("BOOKING_CANCEL_ATTEMPT", currentUsername, params);

        try {
            // Підтвердження спроб скасування бронювань
            var validationResult = bookingService.validateCancellationRequest(selectedBooking, currentUsername);
            if (!validationResult.isValid()) {
                Logger.logError("BOOKING_CANCEL_ATTEMPT", currentUsername, validationResult.getMessage(), params);
                showAlert(validationResult.getMessage());
                return;
            }

            // відображення вікна остаточного підтвердження для скасування
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setHeaderText("Cancel Booking");
            confirm.setContentText("Are you sure you want to cancel this booking?");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        Services.BookingService.cancel(selectedBooking.getId());
                        refresh();
                        refreshMainDashboard();
                        showInfo("Booking cancelled successfully!");
                    } catch (Exception ex) {
                        Logger.logError("BOOKING_CANCEL_ATTEMPT", currentUsername, ex.getMessage(), params);
                        showAlert("Cancellation failed: " + ex.getMessage());
                    }
                } else {
                    Logger.logUserAction("BOOKING_CANCEL_CANCELLED", currentUsername, params);
                }
            });

        } catch (Exception ex) {
            Logger.logError("BOOKING_CANCEL_ATTEMPT", currentUsername, ex.getMessage(), params);
            showAlert("Cancel operation failed: " + ex.getMessage());
        }
    }

    private void handleTransfer() {
        String currentUsername = Services.AuthService.getCurrentUser().getUsername();
        Booking selectedBooking = tvBooking.getSelectionModel().getSelectedItem();
        String params = selectedBooking != null ? String.format("booking_id=%d", selectedBooking.getId())
                : "no_selection";

        Logger.logUserAction("BOOKING_TRANSFER_ATTEMPT", currentUsername, params);

        try {
            if (selectedBooking == null) {
                showAlert("Please select a booking to transfer");
                return;
            }

            // відображення вікна вибору інших проміжків часу у скі-пасі
            ChoiceDialog<String> slotDialog = new ChoiceDialog<>(selectedBooking.getSlot(),
                    bookingService.getAvailableTimeSlots());
            slotDialog.setTitle("Transfer Booking");
            slotDialog.setHeaderText("Select new slot");
            slotDialog.showAndWait().ifPresent(newSlot -> {

                // відображення вікна вибору іншої дати
                DatePicker datePicker = new DatePicker(selectedBooking.getTime().toLocalDate());
                Dialog<LocalDate> dateDialog = new Dialog<>();
                dateDialog.setTitle("Transfer Booking");
                dateDialog.setHeaderText("Select new date");
                dateDialog.getDialogPane().setContent(datePicker);
                dateDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
                dateDialog.setResultConverter(bt -> bt == ButtonType.OK ? datePicker.getValue() : null);

                dateDialog.showAndWait().ifPresent(newDate -> {
                    try {
                        var validationResult = bookingService.validateTransferRequest(
                                selectedBooking, newSlot, newDate, currentUsername);
                        if (!validationResult.isValid()) {
                            showAlert(validationResult.getMessage());
                            return;
                        }

                        LocalDateTime newBookingTime = bookingService.getTimeFromSlot(newSlot, newDate);
                        String transferParams = String.format("booking_id=%d, new_slot=%s, new_date=%s",
                                selectedBooking.getId(), newSlot, newDate);

                        Services.BookingService.transfer(selectedBooking.getId(), newSlot, newBookingTime);
                        refresh();
                        refreshMainDashboard();
                        showInfo("Booking transferred successfully!");
                        Logger.logUserAction("BOOKING_TRANSFER_SUCCESS", currentUsername, transferParams);

                    } catch (Exception ex) {
                        Logger.logError("BOOKING_TRANSFER_ATTEMPT", currentUsername, ex.getMessage(), params);
                        showAlert("Transfer failed: " + ex.getMessage());
                    }
                });
            });

        } catch (Exception ex) {
            Logger.logError("BOOKING_TRANSFER_ATTEMPT", currentUsername, ex.getMessage(), params);
            showAlert("Transfer operation failed: " + ex.getMessage());
        }
    }

    private void refresh() {
        try {
            String currentUsername = Services.AuthService.getCurrentUser().getUsername();
            String currentUserRole = Services.AuthService.getCurrentUser().getRole();
            Logger.logUserAction("BOOKING_REFRESH", currentUsername, "Refreshing booking list");

            ObservableList<Booking> allBookings = Services.BookingService.listAll();

            var filteredBookings = bookingService.filterBookingsForUser(
                    allBookings, currentUsername, currentUserRole);

            tvBooking.setItems(FXCollections.observableArrayList(filteredBookings));

        } catch (Exception e) {
            Logger.logError("BOOKING_REFRESH", "SYSTEM", e.getMessage(), "Failed to refresh booking list");
            e.printStackTrace();
        }
    }

    private void clearForm() {
        cbSlot.setValue(null);
        dpDate.setValue(LocalDate.now());
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
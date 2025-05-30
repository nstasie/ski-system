package org.example.controller;

import org.example.Logger;
import org.example.Services;
import org.example.model.*;
import org.example.service.*;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

import org.example.model.Lesson;
import org.example.service.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class InstructorController {
    @FXML
    private TableView<InstructorWorkload> tvInstructorWorkload;
    @FXML
    private TableColumn<InstructorWorkload, String> colInstructorName;
    @FXML
    private TableColumn<InstructorWorkload, Integer> colTotalLessons;
    @FXML
    private TableColumn<InstructorWorkload, Integer> colTodayLessons;
    @FXML
    private TableColumn<InstructorWorkload, Integer> colWeekLessons;
    @FXML
    private TableColumn<InstructorWorkload, String> colStatus;

    @FXML
    private TableView<Lesson> tvLessons;
    @FXML
    private TableColumn<Lesson, Integer> colId;
    @FXML
    private TableColumn<Lesson, String> colUser;
    @FXML
    private TableColumn<Lesson, String> colInstr;
    @FXML
    private TableColumn<Lesson, LocalDateTime> colTime;

    @FXML
    private ComboBox<String> cbInstructor;
    @FXML
    private DatePicker dpInsDate;
    @FXML
    private Spinner<Integer> spHour;
    @FXML
    private Button btnInsBook;

    private MainController mainController;
    private final InstructorService instructorService = new InstructorService();

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        Logger.logSystemEvent("CONTROLLER_INIT", "InstructorController linked to MainController");
    }

    @FXML
    public void initialize() {
        try {
            Logger.logSystemEvent("CONTROLLER_INIT", "InstructorController initialize() started");

            setupWorkloadTable();
            setupLessonsTable();
            setupBookingControls();
            setupEventHandlers();
            refresh();

            Logger.logSystemEvent("CONTROLLER_INIT", "InstructorController initialized successfully");
        } catch (Exception e) {
            Logger.logError("CONTROLLER_INIT", "SYSTEM", e.getMessage(), "InstructorController initialization failed");
            e.printStackTrace();
        }
    }

    private void setupWorkloadTable() {
        colInstructorName.setCellValueFactory(new PropertyValueFactory<>("instructorName"));
        colTotalLessons.setCellValueFactory(new PropertyValueFactory<>("totalLessons"));
        colTodayLessons.setCellValueFactory(new PropertyValueFactory<>("todayLessons"));
        colWeekLessons.setCellValueFactory(new PropertyValueFactory<>("weekLessons"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void setupLessonsTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        colInstr.setCellValueFactory(new PropertyValueFactory<>("instructor"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
    }

    private void setupBookingControls() {
        cbInstructor.setItems(FXCollections.observableArrayList(
                Services.InstructorService.listNames()));
        dpInsDate.setValue(LocalDate.now());

        List<Integer> availableHours = instructorService.getAvailableLessonHours();
        spHour.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                availableHours.get(0),
                availableHours.get(availableHours.size() - 1),
                9));

        cbInstructor.setCellFactory(param -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);

                    InstructorWorkload workload = getWorkloadForInstructor(item);
                    if (workload != null) {
                        String tooltipText = instructorService.createWorkloadTooltip(
                                new InstructorService.InstructorWorkloadStats(
                                        workload.getInstructorName(),
                                        workload.getTotalLessons(),
                                        workload.getTodayLessons(),
                                        workload.getWeekLessons(),
                                        workload.getStatus()));
                        setTooltip(new Tooltip(tooltipText));
                    }
                }
            }
        });
    }

    private void setupEventHandlers() {
        btnInsBook.setOnAction(e -> handleLessonBooking());
    }

    private void handleLessonBooking() {
        String username = Services.AuthService.getCurrentUser().getUsername();
        String instructor = cbInstructor.getValue();
        LocalDate selectedDate = dpInsDate.getValue();
        Integer selectedHour = spHour.getValue();
        String params = String.format("instructor=%s, date=%s, hour=%d", instructor, selectedDate, selectedHour);

        Logger.logUserAction("LESSON_BOOK_ATTEMPT", username, params);

        try {
            ObservableList<Lesson> existingLessons = Services.InstructorService.listAll();

            var validationResult = instructorService.validateLessonBooking(
                    instructor, selectedDate, selectedHour, username, existingLessons);
            if (!validationResult.isValid()) {
                Logger.logError("LESSON_BOOK_ATTEMPT", username, validationResult.getMessage(), params);
                showAlert(validationResult.getMessage());
                return;
            }

            LocalDateTime lessonTime = selectedDate.atTime(selectedHour, 0);

            var availabilityResult = instructorService.checkInstructorAvailability(
                    instructor, selectedDate, selectedHour, existingLessons);
            if (!availabilityResult.isAvailable()) {
                showAlert(availabilityResult.getMessage());
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setHeaderText("Book Lesson");
            confirm.setContentText("Book lesson with " + instructor + " on " +
                    selectedDate + " at " + selectedHour + ":00 for $30?");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        Services.InstructorService.book(instructor, username, lessonTime);
                        refresh();
                        refreshMainDashboard();
                        showInfo("Lesson booked successfully!");
                        clearBookingForm();
                        Logger.logUserAction("LESSON_BOOK_SUCCESS", username, params);
                    } catch (Exception ex) {
                        Logger.logError("LESSON_BOOK_ATTEMPT", username, ex.getMessage(), params);
                        showAlert("Booking failed: " + ex.getMessage());
                    }
                } else {
                    Logger.logUserAction("LESSON_BOOK_CANCELLED", username, params);
                }
            });

        } catch (Exception ex) {
            Logger.logError("LESSON_BOOK_ATTEMPT", username, ex.getMessage(), params);
            showAlert("Booking operation failed: " + ex.getMessage());
        }
    }

    private void refresh() {
        try {
            String username = Services.AuthService.getCurrentUser().getUsername();
            String userRole = Services.AuthService.getCurrentUser().getRole();
            Logger.logUserAction("INSTRUCTOR_REFRESH", username, "Refreshing instructor data");

            refreshLessonsTable(username, userRole);
            refreshWorkloadTable();
        } catch (Exception e) {
            Logger.logError("INSTRUCTOR_REFRESH", "SYSTEM", e.getMessage(), "Failed to refresh instructor data");
            e.printStackTrace();
        }
    }

    private void refreshLessonsTable(String username, String userRole) {
        try {
            ObservableList<Lesson> allLessons = Services.InstructorService.listAll();

            var filteredLessons = instructorService.filterLessonsForUser(allLessons, username, userRole);
            tvLessons.setItems(FXCollections.observableArrayList(filteredLessons));

        } catch (Exception e) {
            Logger.logError("LESSON_REFRESH", "SYSTEM", e.getMessage(), "Failed to refresh lessons table");
            e.printStackTrace();
        }
    }

    private void refreshWorkloadTable() {
        try {
            List<String> instructorNames = Services.InstructorService.listNames();
            ObservableList<Lesson> allLessons = Services.InstructorService.listAll();

            var workloadStats = instructorService.calculateInstructorWorkloads(instructorNames, allLessons);

            ObservableList<InstructorWorkload> workloadData = FXCollections.observableArrayList();
            for (var stats : workloadStats) {
                workloadData.add(new InstructorWorkload(
                        stats.getInstructorName(),
                        stats.getTotalLessons(),
                        stats.getTodayLessons(),
                        stats.getWeekLessons(),
                        stats.getStatus()));
            }

            tvInstructorWorkload.setItems(workloadData);

        } catch (Exception e) {
            Logger.logError("WORKLOAD_REFRESH", "SYSTEM", e.getMessage(), "Failed to refresh workload table");
            e.printStackTrace();
        }
    }

    private InstructorWorkload getWorkloadForInstructor(String instructorName) {
        return tvInstructorWorkload.getItems().stream()
                .filter(workload -> workload.getInstructorName().equals(instructorName))
                .findFirst()
                .orElse(null);
    }

    private void clearBookingForm() {
        dpInsDate.setValue(LocalDate.now());
        spHour.getValueFactory().setValue(9);
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

    public static class InstructorWorkload {
        private final SimpleStringProperty instructorName;
        private final SimpleIntegerProperty totalLessons;
        private final SimpleIntegerProperty todayLessons;
        private final SimpleIntegerProperty weekLessons;
        private final SimpleStringProperty status;

        public InstructorWorkload(String instructorName, int totalLessons,
                int todayLessons, int weekLessons, String status) {
            this.instructorName = new SimpleStringProperty(instructorName);
            this.totalLessons = new SimpleIntegerProperty(totalLessons);
            this.todayLessons = new SimpleIntegerProperty(todayLessons);
            this.weekLessons = new SimpleIntegerProperty(weekLessons);
            this.status = new SimpleStringProperty(status);
        }

        public String getInstructorName() {
            return instructorName.get();
        }

        public int getTotalLessons() {
            return totalLessons.get();
        }

        public int getTodayLessons() {
            return todayLessons.get();
        }

        public int getWeekLessons() {
            return weekLessons.get();
        }

        public String getStatus() {
            return status.get();
        }

        public SimpleStringProperty instructorNameProperty() {
            return instructorName;
        }

        public SimpleIntegerProperty totalLessonsProperty() {
            return totalLessons;
        }

        public SimpleIntegerProperty todayLessonsProperty() {
            return todayLessons;
        }

        public SimpleIntegerProperty weekLessonsProperty() {
            return weekLessons;
        }

        public SimpleStringProperty statusProperty() {
            return status;
        }
    }
}
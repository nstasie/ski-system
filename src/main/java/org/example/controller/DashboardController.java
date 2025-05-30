package org.example.controller;

import org.example.Services;
import org.example.model.*;
import org.example.service.*;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class DashboardController {
    @FXML
    private Label lblWelcome;
    @FXML
    private Label lblActiveBookings, lblCurrentRentals, lblUpcomingLessons;

    // Таблиця активних бронювань
    @FXML
    private TableView<BookingDisplay> tvActiveBookings;
    @FXML
    private TableColumn<BookingDisplay, String> colBookingSlot;
    @FXML
    private TableColumn<BookingDisplay, String> colBookingDate;
    @FXML
    private TableColumn<BookingDisplay, String> colBookingStatus;

    // Таблиця поточних бронювань
    @FXML
    private TableView<RentalDisplay> tvCurrentRentals;
    @FXML
    private TableColumn<RentalDisplay, String> colRentalType;
    @FXML
    private TableColumn<RentalDisplay, String> colRentalSize;
    @FXML
    private TableColumn<RentalDisplay, String> colRentalSince;

    // Таблиця власних уроків
    @FXML
    private TableView<LessonDisplay> tvMyLessons;
    @FXML
    private TableColumn<LessonDisplay, String> colLessonInstructor;
    @FXML
    private TableColumn<LessonDisplay, String> colLessonTime;
    @FXML
    private TableColumn<LessonDisplay, String> colLessonStatus;

    // Таблиця останніх дій
    @FXML
    private TableView<ActivityDisplay> tvRecentActivity;
    @FXML
    private TableColumn<ActivityDisplay, String> colActivityType;
    @FXML
    private TableColumn<ActivityDisplay, String> colActivityDetails;
    @FXML
    private TableColumn<ActivityDisplay, String> colActivityTime;
    @FXML
    private TableColumn<ActivityDisplay, String> colActivityAmount;

    private User currentUser;
    private final DashboardService dashboardService = new DashboardService();

    public void setCurrentUser(User user) {
        this.currentUser = user;
        try {
            initializeUI();
            loadUserData();
        } catch (Exception e) {
            System.err.println("Error setting current user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        try {
            setupTableColumns();
        } catch (Exception e) {
            System.err.println("Error initializing dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupTableColumns() {
        try {
            // Активні бронювання
            colBookingSlot.setCellValueFactory(new PropertyValueFactory<>("slot"));
            colBookingDate.setCellValueFactory(new PropertyValueFactory<>("formattedDate"));
            colBookingStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

            // Поточне бронювання
            colRentalType.setCellValueFactory(new PropertyValueFactory<>("type"));
            colRentalSize.setCellValueFactory(new PropertyValueFactory<>("size"));
            colRentalSince.setCellValueFactory(new PropertyValueFactory<>("rentedSince"));

            // Уроки
            colLessonInstructor.setCellValueFactory(new PropertyValueFactory<>("instructor"));
            colLessonTime.setCellValueFactory(new PropertyValueFactory<>("formattedDate"));
            colLessonStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

            // Нещодавна активність
            colActivityType.setCellValueFactory(new PropertyValueFactory<>("activityType"));
            colActivityDetails.setCellValueFactory(new PropertyValueFactory<>("details"));
            colActivityTime.setCellValueFactory(new PropertyValueFactory<>("formattedDate"));
            colActivityAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));

        } catch (Exception e) {
            System.err.println("Error setting up table columns: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeUI() {
        try {
            if (currentUser != null && lblWelcome != null) {
                lblWelcome.setText("Welcome back, " + currentUser.getUsername() + "!");
            }
        } catch (Exception e) {
            System.err.println("Error initializing UI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadUserData() {
        try {
            if (currentUser == null) {
                System.err.println("Current user is null, cannot load dashboard data");
                return;
            }

            String username = currentUser.getUsername();

            ObservableList<Booking> allBookings = Services.BookingService.listAll();
            ObservableList<UserRental> allRentals = Services.EquipmentService.getCurrentRentals(username);
            ObservableList<Lesson> allLessons = Services.InstructorService.listAll();
            ObservableList<Transaction> allTransactions = Services.TransactionService.listAll();

            var stats = dashboardService.calculateUserStats(username, allBookings, allRentals, allLessons);
            updateQuickStats(stats);

            loadBookingsDisplay(username, allBookings);
            loadRentalsDisplay(username, allRentals);
            loadLessonsDisplay(username, allLessons);
            loadActivityDisplay(username, allTransactions);

        } catch (Exception e) {
            System.err.println("Error loading user data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadBookingsDisplay(String username, ObservableList<Booking> allBookings) {
        try {
            var bookingDisplayInfo = dashboardService.prepareBookingsForDisplay(username, allBookings);

            // Перетворення на відображувані об’єкти JavaFX
            ObservableList<BookingDisplay> bookingDisplays = FXCollections.observableArrayList();
            for (var info : bookingDisplayInfo) {
                bookingDisplays.add(new BookingDisplay(
                        info.getSlot(),
                        info.getFormattedDate(),
                        info.getStatus()));
            }

            if (tvActiveBookings != null) {
                tvActiveBookings.setItems(bookingDisplays);
            }
        } catch (Exception e) {
            System.err.println("Error loading bookings display: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadRentalsDisplay(String username, ObservableList<UserRental> allRentals) {
        try {
            var rentalDisplayInfo = dashboardService.prepareRentalsForDisplay(username, allRentals);

            // Перетворення на відображувані об’єкти JavaFX
            ObservableList<RentalDisplay> rentalDisplays = FXCollections.observableArrayList();
            for (var info : rentalDisplayInfo) {
                rentalDisplays.add(new RentalDisplay(
                        info.getType(),
                        info.getSize(),
                        info.getRentedSince()));
            }

            if (tvCurrentRentals != null) {
                tvCurrentRentals.setItems(rentalDisplays);
            }
        } catch (Exception e) {
            System.err.println("Error loading rentals display: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadLessonsDisplay(String username, ObservableList<Lesson> allLessons) {
        try {
            var lessonDisplayInfo = dashboardService.prepareLessonsForDisplay(username, allLessons);

            // Перетворення на відображувані об’єкти JavaFX
            ObservableList<LessonDisplay> lessonDisplays = FXCollections.observableArrayList();
            for (var info : lessonDisplayInfo) {
                lessonDisplays.add(new LessonDisplay(
                        info.getInstructor(),
                        info.getFormattedDate(),
                        info.getStatus()));
            }

            if (tvMyLessons != null) {
                tvMyLessons.setItems(lessonDisplays);
            }
        } catch (Exception e) {
            System.err.println("Error loading lessons display: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadActivityDisplay(String username, ObservableList<Transaction> allTransactions) {
        try {
            var activityDisplayInfo = dashboardService.prepareRecentActivityForDisplay(username, allTransactions, 10);

            // Перетворення на відображувані об’єкти JavaFX
            ObservableList<ActivityDisplay> activityDisplays = FXCollections.observableArrayList();
            for (var info : activityDisplayInfo) {
                activityDisplays.add(new ActivityDisplay(
                        info.getActivityType(),
                        info.getDetails(),
                        info.getFormattedDate(),
                        info.getAmount()));
            }

            if (tvRecentActivity != null) {
                tvRecentActivity.setItems(activityDisplays);
            }
        } catch (Exception e) {
            System.err.println("Error loading activity display: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateQuickStats(DashboardService.DashboardStats stats) {
        try {
            if (lblActiveBookings == null || lblCurrentRentals == null || lblUpcomingLessons == null) {
                System.err.println("Quick stats labels are null");
                return;
            }

            // Оновлення міток за допомогою Platform.runLater, щоб забезпечити потік інтерфейсу користувача
            javafx.application.Platform.runLater(() -> {
                try {
                    lblActiveBookings.setText(String.valueOf(stats.getActiveBookings()));
                    lblCurrentRentals.setText(String.valueOf(stats.getCurrentRentals()));
                    lblUpcomingLessons.setText(String.valueOf(stats.getUpcomingLessons()));
                } catch (Exception e) {
                    System.err.println("Error updating labels on UI thread: " + e.getMessage());
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            System.err.println("Error updating quick stats: " + e.getMessage());
            e.printStackTrace();

            // Встановлення значення за замовчуванням, щоб запобігти нульовому відображенню
            javafx.application.Platform.runLater(() -> {
                try {
                    if (lblActiveBookings != null)
                        lblActiveBookings.setText("0");
                    if (lblCurrentRentals != null)
                        lblCurrentRentals.setText("0");
                    if (lblUpcomingLessons != null)
                        lblUpcomingLessons.setText("0");
                } catch (Exception ex) {
                    System.err.println("Error setting default stats: " + ex.getMessage());
                }
            });
        }
    }

    public void refresh() {
        try {
            if (currentUser != null) {
                loadUserData();
            } else {
                System.err.println("Cannot refresh dashboard: current user is null");
            }
        } catch (Exception e) {
            System.err.println("Error refreshing dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static class BookingDisplay {
        private final String slot;
        private final String formattedDate;
        private final String status;

        public BookingDisplay(String slot, String formattedDate, String status) {
            this.slot = slot;
            this.formattedDate = formattedDate;
            this.status = status;
        }

        public String getSlot() {
            return slot;
        }

        public String getFormattedDate() {
            return formattedDate;
        }

        public String getStatus() {
            return status;
        }
    }

    public static class RentalDisplay {
        private final String type;
        private final String size;
        private final String rentedSince;

        public RentalDisplay(String type, String size, String rentedSince) {
            this.type = type;
            this.size = size;
            this.rentedSince = rentedSince;
        }

        public String getType() {
            return type;
        }

        public String getSize() {
            return size;
        }

        public String getRentedSince() {
            return rentedSince;
        }
    }

    public static class LessonDisplay {
        private final String instructor;
        private final String formattedDate;
        private final String status;

        public LessonDisplay(String instructor, String formattedDate, String status) {
            this.instructor = instructor;
            this.formattedDate = formattedDate;
            this.status = status;
        }

        public String getInstructor() {
            return instructor;
        }

        public String getFormattedDate() {
            return formattedDate;
        }

        public String getStatus() {
            return status;
        }
    }

    public static class ActivityDisplay {
        private final String activityType;
        private final String details;
        private final String formattedDate;
        private final String amount;

        public ActivityDisplay(String activityType, String details, String formattedDate, String amount) {
            this.activityType = activityType;
            this.details = details;
            this.formattedDate = formattedDate;
            this.amount = amount;
        }

        public String getActivityType() {
            return activityType;
        }

        public String getDetails() {
            return details;
        }

        public String getFormattedDate() {
            return formattedDate;
        }

        public String getAmount() {
            return amount;
        }
    }
}
package org.example.controller;

import org.example.Services;
import org.example.model.*;
import org.example.service.*;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;

import java.util.*;

public class AnalyticsController {
    @FXML
    private VBox mainContainer;

    // діаграми
    private LineChart<String, Number> attendanceChart;
    private BarChart<String, Number> hourlyActivityChart;

    // таблиці про найбільш зайнятих інструкторів та популярне спорядження
    private TableView<AnalyticsService.InstructorStats> tvPopularInstructors;
    private TableView<AnalyticsService.EquipmentStats> tvPopularSki;
    private TableView<AnalyticsService.EquipmentStats> tvPopularSnowboard;

    private User currentUser;
    private AnalyticsService analyticsService = new AnalyticsService();

    public void setCurrentUser(User user) {
        this.currentUser = user;
        setupUI();
        loadAnalyticsData();
    }

    @FXML
    public void initialize() {
        setupUI();
    }

    private void setupUI() {
        if (mainContainer == null)
            return;

        // Очищення наявного вмісту
        mainContainer.getChildren().clear();

        // Створення діаграм і таблиць
        createAttendanceChart();
        createHourlyActivityChart();

        createPopularInstructorsTable();
        createPopularEquipmentTables();

        // Додавання всіх компонентів до основного контейнера
        mainContainer.getChildren().addAll(
                createSectionTitle("Daily User Attendance (Last 30 Days)"),
                attendanceChart,
                createSectionTitle("Hourly Booking Activity (Current Month)"),
                hourlyActivityChart,
                createSectionTitle("Most Popular Instructors"),
                tvPopularInstructors,
                createSectionTitle("Equipment Popularity"),
                createEquipmentTablesContainer());
    }

    private Label createSectionTitle(String title) {
        Label label = new Label(title);
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 20 0 10 0;");
        return label;
    }

    private void createAttendanceChart() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Date");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Unique Users");
        yAxis.setForceZeroInRange(true);

        attendanceChart = new LineChart<>(xAxis, yAxis);
        attendanceChart.setTitle("Daily User Attendance");
        attendanceChart.setPrefHeight(300);
        attendanceChart.setCreateSymbols(true);
        attendanceChart.setLegendVisible(false);
    }

    private void createHourlyActivityChart() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Hour of Day");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Number of Bookings");
        yAxis.setForceZeroInRange(true);

        hourlyActivityChart = new BarChart<>(xAxis, yAxis);
        hourlyActivityChart.setTitle("Booking Activity by Hour (Current Month)");
        hourlyActivityChart.setPrefHeight(300);
        hourlyActivityChart.setLegendVisible(false);
        hourlyActivityChart.setCategoryGap(2);
    }

    private void createPopularInstructorsTable() {
        tvPopularInstructors = new TableView<>();
        tvPopularInstructors.setPrefHeight(200);

        TableColumn<AnalyticsService.InstructorStats, String> colName = new TableColumn<>("Instructor Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(200);

        TableColumn<AnalyticsService.InstructorStats, String> colLessons = new TableColumn<>("Total Lessons");
        colLessons.setCellValueFactory(new PropertyValueFactory<>("lessonsCount"));
        colLessons.setPrefWidth(150);

        tvPopularInstructors.getColumns().addAll(colName, colLessons);
    }

    public void createPopularEquipmentTables() {
        tvPopularSki = new TableView<>();
        tvPopularSki.setPrefHeight(150);

        TableColumn<AnalyticsService.EquipmentStats, String> colSkiSize = new TableColumn<>("Size");
        colSkiSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        colSkiSize.setPrefWidth(100);

        TableColumn<AnalyticsService.EquipmentStats, String> colSkiRentals = new TableColumn<>("Current Rentals");
        colSkiRentals.setCellValueFactory(new PropertyValueFactory<>("rentalsCount"));
        colSkiRentals.setPrefWidth(150);

        tvPopularSki.getColumns().addAll(colSkiSize, colSkiRentals);

        tvPopularSnowboard = new TableView<>();
        tvPopularSnowboard.setPrefHeight(150);

        TableColumn<AnalyticsService.EquipmentStats, String> colSnowSize = new TableColumn<>("Size");
        colSnowSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        colSnowSize.setPrefWidth(100);

        TableColumn<AnalyticsService.EquipmentStats, String> colSnowRentals = new TableColumn<>("Current Rentals");
        colSnowRentals.setCellValueFactory(new PropertyValueFactory<>("rentalsCount"));
        colSnowRentals.setPrefWidth(150);

        tvPopularSnowboard.getColumns().addAll(colSnowSize, colSnowRentals);
    }

    private VBox createEquipmentTablesContainer() {
        VBox container = new VBox(10);

        // Лижна секція
        Label skiLabel = new Label("Ski Equipment (by Size)");
        skiLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // Сноубордична секція
        Label snowLabel = new Label("Snowboard Equipment (by Size)");
        snowLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        container.getChildren().addAll(
                skiLabel,
                tvPopularSki,
                snowLabel,
                tvPopularSnowboard);

        return container;
    }

    private void loadAnalyticsData() {
        try {
            loadAttendanceData();
            loadHourlyActivityData();
            loadPopularInstructorsData();
            loadPopularEquipmentData();
        } catch (Exception e) {
            System.err.println("Error loading analytics data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadAttendanceData() {
        if (attendanceChart == null)
            return;

        try {
            // отримання даних із сервісу по транзакціям
            ObservableList<Transaction> transactions = Services.TransactionService.listAll();

            Map<String, Integer> dailyAttendance = analyticsService.calculateDailyAttendance(
                    new ArrayList<>(transactions));

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Unique Users");

            // додавання точок даних
            for (Map.Entry<String, Integer> entry : dailyAttendance.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }

            attendanceChart.getData().clear();
            attendanceChart.getData().add(series);

        } catch (Exception e) {
            System.err.println("Error loading attendance data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadHourlyActivityData() {
        if (hourlyActivityChart == null)
            return;

        try {
            ObservableList<Booking> bookings = Services.BookingService.listAll();

            Map<Integer, Integer> hourlyActivity = analyticsService.calculateHourlyActivity(
                    new ArrayList<>(bookings));

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Bookings");

            // додавання точок даних для кожної години (показуюються лише години активності)
            boolean hasData = false;
            for (int hour = 8; hour <= 20; hour++) { // години роботи гірськолижного курорту/підйомників
                Integer count = hourlyActivity.get(hour);
                if (count == null)
                    count = 0;

                String hourLabel = String.format("%02d:00", hour);
                series.getData().add(new XYChart.Data<>(hourLabel, count));

                if (count > 0)
                    hasData = true;
            }

            hourlyActivityChart.getData().clear();
            hourlyActivityChart.getData().add(series);

        } catch (Exception e) {
            System.err.println("Error loading hourly activity data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadPopularInstructorsData() {
        if (tvPopularInstructors == null)
            return;

        try {
            ObservableList<Lesson> lessons = Services.InstructorService.listAll();

            List<AnalyticsService.InstructorStats> stats = analyticsService.calculateInstructorStats(
                    new ArrayList<>(lessons));

            tvPopularInstructors.setItems(FXCollections.observableArrayList(stats));
        } catch (Exception e) {
            System.err.println("Error loading instructor data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadPopularEquipmentData() {
        if (tvPopularSki == null || tvPopularSnowboard == null)
            return;

        try {
            ObservableList<UserRental> allRentals = Services.EquipmentService.getAllCurrentRentals();
            ObservableList<Equipment> equipment = Services.EquipmentService.listAll();

            AnalyticsService.EquipmentPopularity popularity = analyticsService.calculateEquipmentPopularity(
                    new ArrayList<>(allRentals),
                    new ArrayList<>(equipment));

            tvPopularSki.setItems(FXCollections.observableArrayList(popularity.getSkiStats()));
            tvPopularSnowboard.setItems(FXCollections.observableArrayList(popularity.getSnowboardStats()));

        } catch (Exception e) {
            System.err.println("Error loading equipment popularity data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
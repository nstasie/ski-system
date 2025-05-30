package org.example.controller;

import org.example.Services;
import org.example.model.*;
import org.example.service.*;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;

import org.example.model.Transaction;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class FinanceController {
    @FXML
    private VBox mainContainer;
    @FXML
    private TableView<Transaction> tvTrans;
    @FXML
    private TableColumn<Transaction, Integer> colId;
    @FXML
    private TableColumn<Transaction, String> colUser;
    @FXML
    private TableColumn<Transaction, String> colType;
    @FXML
    private TableColumn<Transaction, Double> colAmount;
    @FXML
    private TableColumn<Transaction, LocalDateTime> colTime;

    //компоненти тижневої діаграми
    private BarChart<String, Number> weeklyChart;
    private CategoryAxis xAxis;
    private NumberAxis yAxis;

    // таблиця місячних звітів
    private TableView<MonthlyReport> tvMonthlyReports;

    private org.example.model.User currentUser;

    public void setCurrentUser(org.example.model.User user) {
        this.currentUser = user;
        setupUI();
        load();
    }

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));

        setupUI();
    }

    private void setupUI() {
        if (mainContainer == null)
            return;

        mainContainer.getChildren().clear();

        // Створення тижневої діаграми прибутків
        createWeeklyChart();

        // Створення таблиці місячних звітів
        createMonthlyReportsTable();

        Label transHistoryLabel = new Label("Transaction History");
        transHistoryLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10 0 5 0;");

        // Додавання всіх компонентів до основного контейнера
        mainContainer.getChildren().addAll(
                createSectionTitle("Weekly Profit/Loss Chart"),
                weeklyChart,
                createSectionTitle("Monthly Financial Reports"),
                tvMonthlyReports,
                transHistoryLabel,
                tvTrans);
    }

    private Label createSectionTitle(String title) {
        Label label = new Label(title);
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 20 0 10 0;");
        return label;
    }

    private void createWeeklyChart() {
        xAxis = new CategoryAxis();
        xAxis.setLabel("Day");

        yAxis = new NumberAxis(-2000, 2000, 500);
        yAxis.setLabel("Profit/Loss ($)");
        yAxis.setTickUnit(500);

        weeklyChart = new BarChart<>(xAxis, yAxis);
        weeklyChart.setTitle("Weekly Financial Performance");
        weeklyChart.setPrefHeight(300);
        weeklyChart.setLegendVisible(true);

        // Set custom CSS for positive/negative bars
        weeklyChart.setStyle("-fx-bar-fill: #4CAF50;"); // Default green
    }

    private void createMonthlyReportsTable() {
        tvMonthlyReports = new TableView<>();
        tvMonthlyReports.setPrefHeight(150);

        TableColumn<MonthlyReport, String> colCategory = new TableColumn<>("Category");
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colCategory.setPrefWidth(150);

        TableColumn<MonthlyReport, String> colAmount = new TableColumn<>("Amount");
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colAmount.setPrefWidth(100);

        TableColumn<MonthlyReport, String> colCount = new TableColumn<>("Count");
        colCount.setCellValueFactory(new PropertyValueFactory<>("count"));
        colCount.setPrefWidth(80);

        tvMonthlyReports.getColumns().addAll(colCategory, colAmount, colCount);
    }

    private void load() {
        ObservableList<Transaction> all = Services.TransactionService.listAll();
        if (currentUser.getRole().equals("ADMIN")) {
            tvTrans.setItems(all);
        } else {
            tvTrans.setItems(
                    all.filtered(t -> t.getUsername().equals(currentUser.getUsername())));
        }

        loadWeeklyChartData(all);
        loadMonthlyReportsData(all);
    }

    private void loadWeeklyChartData(ObservableList<Transaction> transactions) {
        if (weeklyChart == null)
            return;

        // Отримуємо останні 7 днів, включаючи сьогодні
        LocalDate today = LocalDate.now();
        Map<String, Double> bookingData = new HashMap<>();
        Map<String, Double> equipmentData = new HashMap<>();
        Map<String, Double> lessonData = new HashMap<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dayLabel = date.format(DateTimeFormatter.ofPattern("MMM dd"));
            bookingData.put(dayLabel, 0.0);
            equipmentData.put(dayLabel, 0.0);
            lessonData.put(dayLabel, 0.0);
        }

        // Оброблення транзакцій
        for (Transaction t : transactions) {
            LocalDate transDate = t.getTime().toLocalDate();

            // лише за останні 7 днів
            if (transDate.isAfter(today.minusDays(7)) && !transDate.isAfter(today)) {
                String dayLabel = transDate.format(DateTimeFormatter.ofPattern("MMM dd"));
                double amount = calculateDisplayAmount(t.getType(), t.getAmount());

                switch (t.getType()) {
                    case "booking":
                        bookingData.merge(dayLabel, amount, Double::sum);
                        break;
                    case "cancel_booking":
                        bookingData.merge(dayLabel, amount, Double::sum);
                        break;
                    case "rent_eq":
                        equipmentData.merge(dayLabel, amount, Double::sum);
                        break;
                    case "return_eq":
                        //спорядження для повернення відображається як $0
                        equipmentData.merge(dayLabel, 0.0, Double::sum);
                        break;
                    case "lesson":
                        lessonData.merge(dayLabel, amount, Double::sum);
                        break;
                }
            }
        }

        XYChart.Series<String, Number> bookingSeries = new XYChart.Series<>();
        bookingSeries.setName("Bookings");

        XYChart.Series<String, Number> equipmentSeries = new XYChart.Series<>();
        equipmentSeries.setName("Equipment");

        XYChart.Series<String, Number> lessonSeries = new XYChart.Series<>();
        lessonSeries.setName("Lessons");

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dayLabel = date.format(DateTimeFormatter.ofPattern("MMM dd"));

            bookingSeries.getData().add(new XYChart.Data<>(dayLabel, bookingData.get(dayLabel)));
            equipmentSeries.getData().add(new XYChart.Data<>(dayLabel, equipmentData.get(dayLabel)));
            lessonSeries.getData().add(new XYChart.Data<>(dayLabel, lessonData.get(dayLabel)));
        }

        weeklyChart.getData().clear();
        weeklyChart.getData().addAll(bookingSeries, equipmentSeries, lessonSeries);

        styleChartBars();
    }

    private void styleChartBars() {
        weeklyChart.applyCss();
        weeklyChart.layout();

        weeklyChart.lookupAll(".chart-bar").forEach(node -> {
            if (node.getUserData() instanceof Number) {
                double value = ((Number) node.getUserData()).doubleValue();
                if (value >= 0) {
                    node.setStyle("-fx-bar-fill: #4CAF50;"); // Green for positive
                } else {
                    node.setStyle("-fx-bar-fill: #F44336;"); // Red for negative
                }
            }
        });
    }

    private void loadMonthlyReportsData(ObservableList<Transaction> transactions) {
        if (tvMonthlyReports == null)
            return;

        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);

        Map<String, Double> totals = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();

        totals.put("Bookings", 0.0);
        totals.put("Equipment Rentals", 0.0);
        totals.put("Lessons", 0.0);
        totals.put("Cancellations", 0.0);
        totals.put("TOTAL", 0.0);

        counts.put("Bookings", 0);
        counts.put("Equipment Rentals", 0);
        counts.put("Lessons", 0);
        counts.put("Cancellations", 0);
        counts.put("TOTAL", 0);

        for (Transaction t : transactions) {
            LocalDate transDate = t.getTime().toLocalDate();

            if (!transDate.isBefore(startOfMonth) && !transDate.isAfter(today)) {
                double amount = calculateDisplayAmount(t.getType(), t.getAmount());

                switch (t.getType()) {
                    case "booking":
                        totals.merge("Bookings", amount, Double::sum);
                        counts.merge("Bookings", 1, Integer::sum);
                        break;
                    case "cancel_booking":
                        totals.merge("Cancellations", amount, Double::sum);
                        counts.merge("Cancellations", 1, Integer::sum);
                        break;
                    case "rent_eq":
                        totals.merge("Equipment Rentals", amount, Double::sum);
                        counts.merge("Equipment Rentals", 1, Integer::sum);
                        break;
                    case "return_eq":
                        // Повернення не впливають на місячну суму (вже оплачено)
                        break;
                    case "lesson":
                        totals.merge("Lessons", amount, Double::sum);
                        counts.merge("Lessons", 1, Integer::sum);
                        break;
                }

                if (!t.getType().equals("return_eq")) {
                    totals.merge("TOTAL", amount, Double::sum);
                    counts.merge("TOTAL", 1, Integer::sum);
                }
            }
        }

        // Створення даних звіту
        ObservableList<MonthlyReport> reports = FXCollections.observableArrayList();

        String[] categories = { "Bookings", "Equipment Rentals", "Lessons", "Cancellations", "TOTAL" };
        for (String category : categories) {
            double amount = totals.get(category);
            int count = counts.get(category);
            String amountStr = String.format("$%.0f", amount);
            if (amount < 0) {
                amountStr = "-$" + String.format("%.0f", Math.abs(amount));
            }

            MonthlyReport report = new MonthlyReport(category, amountStr, String.valueOf(count));
            reports.add(report);
        }

        tvMonthlyReports.setItems(reports);
    }

    private double calculateDisplayAmount(String type, double originalAmount) {
        switch (type) {
            case "booking":
                return originalAmount; // +50
            case "cancel_booking":
                return -Math.abs(originalAmount); // -50
            case "rent_eq":
                return originalAmount; // +20
            case "return_eq":
                return 0.0; // 0 (user doesn't get money back)
            case "lesson":
                return originalAmount; // +30
            default:
                return originalAmount;
        }
    }

    public static class MonthlyReport {
        private final String category;
        private final String amount;
        private final String count;

        public MonthlyReport(String category, String amount, String count) {
            this.category = category;
            this.amount = amount;
            this.count = count;
        }

        public String getCategory() {
            return category;
        }

        public String getAmount() {
            return amount;
        }

        public String getCount() {
            return count;
        }
    }
}
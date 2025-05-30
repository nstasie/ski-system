package org.example.service;

import org.example.model.*;

import org.example.model.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


public class FinanceService {


    public double calculateDisplayAmount(String type, double originalAmount) {
        if (type == null) {
            return originalAmount;
        }

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

   //Обчислення щотижневих даних прибутку/збитку для діаграми
    public Map<String, WeeklyFinancialData> calculateWeeklyFinancialData(List<Transaction> transactions) {
        if (transactions == null) {
            return new LinkedHashMap<>();
        }

        LocalDate today = LocalDate.now();
        Map<String, WeeklyFinancialData> weeklyData = new LinkedHashMap<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dayLabel = date.format(DateTimeFormatter.ofPattern("MMM dd"));
            weeklyData.put(dayLabel, new WeeklyFinancialData(dayLabel));
        }

        for (Transaction t : transactions) {
            LocalDate transDate = t.getTime().toLocalDate();

            if (transDate.isAfter(today.minusDays(7)) && !transDate.isAfter(today)) {
                String dayLabel = transDate.format(DateTimeFormatter.ofPattern("MMM dd"));
                WeeklyFinancialData dayData = weeklyData.get(dayLabel);

                if (dayData != null) {
                    double amount = calculateDisplayAmount(t.getType(), t.getAmount());
                    dayData.addTransactionAmount(t.getType(), amount);
                }
            }
        }

        return weeklyData;
    }

    //Розрахунок щомісячних фінансових звітів
    public List<MonthlyFinancialReport> calculateMonthlyReports(List<Transaction> transactions) {
        if (transactions == null) {
            return new ArrayList<>();
        }

        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);

        Map<String, Double> totals = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();

        String[] categories = { "Bookings", "Equipment Rentals", "Lessons", "Cancellations" };
        for (String category : categories) {
            totals.put(category, 0.0);
            counts.put(category, 0);
        }

        // Оброблення операцій за поточний місяць
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
                    case "lesson":
                        totals.merge("Lessons", amount, Double::sum);
                        counts.merge("Lessons", 1, Integer::sum);
                        break;
                }
            }
        }

        // загальний підрахунок
        double totalAmount = totals.values().stream().mapToDouble(Double::doubleValue).sum();
        int totalCount = counts.values().stream().mapToInt(Integer::intValue).sum();

        // створення звітів
        List<MonthlyFinancialReport> reports = new ArrayList<>();
        for (String category : categories) {
            double amount = totals.get(category);
            int count = counts.get(category);
            reports.add(new MonthlyFinancialReport(category, amount, count));
        }

        reports.add(new MonthlyFinancialReport("TOTAL", totalAmount, totalCount));

        return reports;
    }


    public List<Transaction> filterTransactionsForUser(List<Transaction> allTransactions, String username,
            String userRole) {
        if (allTransactions == null) {
            return new ArrayList<>();
        }

        if ("ADMIN".equals(userRole)) {
            return new ArrayList<>(allTransactions);
        }

        return allTransactions.stream()
                .filter(t -> username != null && username.equals(t.getUsername()))
                .collect(Collectors.toList());
    }


    public FinancialSummary calculatePeriodSummary(List<Transaction> transactions, LocalDate startDate,
            LocalDate endDate) {
        if (transactions == null || startDate == null || endDate == null) {
            return new FinancialSummary(0.0, 0.0, 0.0, 0);
        }

        double totalRevenue = 0.0;
        double totalRefunds = 0.0;
        int transactionCount = 0;

        for (Transaction t : transactions) {
            LocalDate transDate = t.getTime().toLocalDate();

            if (!transDate.isBefore(startDate) && !transDate.isAfter(endDate)) {
                double amount = calculateDisplayAmount(t.getType(), t.getAmount());

                if (amount > 0) {
                    totalRevenue += amount;
                } else {
                    totalRefunds += Math.abs(amount);
                }

                transactionCount++;
            }
        }

        double netProfit = totalRevenue - totalRefunds;

        return new FinancialSummary(totalRevenue, totalRefunds, netProfit, transactionCount);
    }

    //визначення діяльность, яка приносить дохід
    public List<RevenueByActivity> calculateRevenueByActivity(List<Transaction> transactions, LocalDate startDate,
            LocalDate endDate) {
        if (transactions == null || startDate == null || endDate == null) {
            return new ArrayList<>();
        }

        Map<String, Double> revenueByType = new HashMap<>();
        Map<String, Integer> countByType = new HashMap<>();

        for (Transaction t : transactions) {
            LocalDate transDate = t.getTime().toLocalDate();

            if (!transDate.isBefore(startDate) && !transDate.isAfter(endDate)) {
                double amount = calculateDisplayAmount(t.getType(), t.getAmount());
                String displayType = getDisplayTransactionType(t.getType());

                revenueByType.merge(displayType, amount, Double::sum);
                countByType.merge(displayType, 1, Integer::sum);
            }
        }

        return revenueByType.entrySet().stream()
                .map(entry -> new RevenueByActivity(
                        entry.getKey(),
                        entry.getValue(),
                        countByType.get(entry.getKey())))
                .sorted((a, b) -> Double.compare(b.getRevenue(), a.getRevenue()))
                .collect(Collectors.toList());
    }

    public String formatCurrency(double amount) {
        if (amount >= 0) {
            return String.format("$%.0f", amount);
        } else {
            return String.format("-$%.0f", Math.abs(amount));
        }
    }


    public String getDisplayTransactionType(String type) {
        if (type == null)
            return "Unknown";

        switch (type) {
            case "booking":
                return "Bookings";
            case "cancel_booking":
                return "Cancellations";
            case "rent_eq":
                return "Equipment Rentals";
            case "return_eq":
                return "Equipment Returns";
            case "lesson":
                return "Lessons";
            default:
                return type;
        }
    }


    public double calculateProfitMargin(List<Transaction> transactions, LocalDate startDate, LocalDate endDate) {
        FinancialSummary summary = calculatePeriodSummary(transactions, startDate, endDate);

        if (summary.getTotalRevenue() == 0) {
            return 0.0;
        }

        return (summary.getNetProfit() / summary.getTotalRevenue()) * 100;
    }

    // Inner classes for structured data
    public static class WeeklyFinancialData {
        private final String day;
        private double bookings = 0.0;
        private double equipment = 0.0;
        private double lessons = 0.0;
        private double cancellations = 0.0;

        public WeeklyFinancialData(String day) {
            this.day = day;
        }

        public void addTransactionAmount(String type, double amount) {
            switch (type) {
                case "booking":
                    bookings += amount;
                    break;
                case "cancel_booking":
                    cancellations += amount;
                    break;
                case "rent_eq":
                    equipment += amount;
                    break;
                case "lesson":
                    lessons += amount;
                    break;
            }
        }

        public String getDay() {
            return day;
        }

        public double getBookings() {
            return bookings;
        }

        public double getEquipment() {
            return equipment;
        }

        public double getLessons() {
            return lessons;
        }

        public double getCancellations() {
            return cancellations;
        }

        public double getTotal() {
            return bookings + equipment + lessons + cancellations;
        }
    }

    public static class MonthlyFinancialReport {
        private final String category;
        private final double amount;
        private final int count;

        public MonthlyFinancialReport(String category, double amount, int count) {
            this.category = category;
            this.amount = amount;
            this.count = count;
        }

        public String getCategory() {
            return category;
        }

        public String getAmount() {
            return amount >= 0 ? String.format("$%.0f", amount) : String.format("-$%.0f", Math.abs(amount));
        }

        public String getCount() {
            return String.valueOf(count);
        }

        public double getRawAmount() {
            return amount;
        }

        public int getRawCount() {
            return count;
        }
    }

    public static class FinancialSummary {
        private final double totalRevenue;
        private final double totalRefunds;
        private final double netProfit;
        private final int transactionCount;

        public FinancialSummary(double totalRevenue, double totalRefunds, double netProfit, int transactionCount) {
            this.totalRevenue = totalRevenue;
            this.totalRefunds = totalRefunds;
            this.netProfit = netProfit;
            this.transactionCount = transactionCount;
        }

        public double getTotalRevenue() {
            return totalRevenue;
        }

        public double getTotalRefunds() {
            return totalRefunds;
        }

        public double getNetProfit() {
            return netProfit;
        }

        public int getTransactionCount() {
            return transactionCount;
        }
    }

    public static class RevenueByActivity {
        private final String activityType;
        private final double revenue;
        private final int count;

        public RevenueByActivity(String activityType, double revenue, int count) {
            this.activityType = activityType;
            this.revenue = revenue;
            this.count = count;
        }

        public String getActivityType() {
            return activityType;
        }

        public double getRevenue() {
            return revenue;
        }

        public int getCount() {
            return count;
        }
    }
}
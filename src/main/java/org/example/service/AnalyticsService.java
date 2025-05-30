package org.example.service;

import org.example.model.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class AnalyticsService {

    //Щоденна відвідуваність користувачів за останні 30 днів
    public Map<String, Integer> calculateDailyAttendance(List<Transaction> transactions) {
        LocalDate today = LocalDate.now();
        Map<String, Set<String>> dailyUsers = new HashMap<>();

        // Ініціалізація останніх 30 днів
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateLabel = date.format(DateTimeFormatter.ofPattern("MMM dd"));
            dailyUsers.put(dateLabel, new HashSet<>());
        }

        // Process transactions
        for (Transaction t : transactions) {
            LocalDate transDate = t.getTime().toLocalDate();

            if (transDate.isAfter(today.minusDays(30)) && !transDate.isAfter(today)) {
                String dateLabel = transDate.format(DateTimeFormatter.ofPattern("MMM dd"));
                if (dailyUsers.containsKey(dateLabel)) {
                    dailyUsers.get(dateLabel).add(t.getUsername());
                }
            }
        }

        // Convert to counts
        Map<String, Integer> result = new LinkedHashMap<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateLabel = date.format(DateTimeFormatter.ofPattern("MMM dd"));
            result.put(dateLabel, dailyUsers.get(dateLabel).size());
        }

        return result;
    }

    /**
     * FIXED: Calculate hourly activity for bookings in current month
     */
    public Map<Integer, Integer> calculateHourlyActivity(List<Booking> bookings) {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);

        // Ініціалізація лічильників годин (0-23)
        Map<Integer, Integer> hourCounts = new HashMap<>();
        for (int i = 0; i < 24; i++) {
            hourCounts.put(i, 0);
        }

        // Оброблення замовлень на поточний місяць
        for (Booking booking : bookings) {
            LocalDate bookingDate = booking.getTime().toLocalDate();

            // Перевірка фіксованого діапазону дат
            if (!bookingDate.isBefore(startOfMonth) && !bookingDate.isAfter(today)) {
                String slot = booking.getSlot();

                try {
                    String[] timeParts = slot.split("-");
                    if (timeParts.length == 2) {
                        int startHour = Integer.parseInt(timeParts[0].trim());
                        int endHour = Integer.parseInt(timeParts[1].trim());

                        if (startHour >= 0 && startHour < 24 && endHour > startHour && endHour <= 24) {
                            // Розраховувати кожну годину в слоті як активну
                            for (int hour = startHour; hour < endHour; hour++) {
                                hourCounts.merge(hour, 1, Integer::sum);
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Invalid slot format: " + slot);
                }
            }
        }
        return hourCounts;
    }

    //Розрахунок статистики популярності інструктора (за к-стю уроків)
    public List<InstructorStats> calculateInstructorStats(List<Lesson> lessons) {
        Map<String, Integer> instructorCounts = new HashMap<>();

        // Підрахунок уроків на викладача
        for (Lesson lesson : lessons) {
            instructorCounts.merge(lesson.getInstructor(), 1, Integer::sum);
        }

        // Перетворення на об’єкти статистики та сортування за кількістю (за спаданням)
        return instructorCounts.entrySet().stream()
                .map(entry -> new InstructorStats(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> Integer.compare(b.getLessonsCount(), a.getLessonsCount()))
                .collect(Collectors.toList());
    }

    //розрахунок популярності спорядження за типом і розміром
    public EquipmentPopularity calculateEquipmentPopularity(
            List<UserRental> currentRentals,
            List<Equipment> allEquipment) {

        Map<String, Integer> skiCounts = new HashMap<>();
        Map<String, Integer> snowboardCounts = new HashMap<>();

        if (allEquipment != null) {
            for (Equipment eq : allEquipment) {
                String type = eq.getType().toLowerCase();
                String size = eq.getSize();

                if ("ski".equals(type)) {
                    skiCounts.put(size, 0);
                } else if ("snowboard".equals(type)) {
                    snowboardCounts.put(size, 0);
                }
            }
        }

        // підрахунок поточних прокатів
        if (currentRentals != null) {
            for (UserRental rental : currentRentals) {
                String type = rental.getType().toLowerCase();
                String size = rental.getSize();

                if ("ski".equals(type)) {
                    skiCounts.merge(size, 1, Integer::sum);
                } else if ("snowboard".equals(type)) {
                    snowboardCounts.merge(size, 1, Integer::sum);
                }
            }
        }

        // Перетворення в статистику та сортування за популярністю (кількість прокатів)
        List<EquipmentStats> skiStats = skiCounts.entrySet().stream()
                .map(entry -> new EquipmentStats(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> Integer.compare(b.getRentalsCount(), a.getRentalsCount()))
                .collect(Collectors.toList());

        List<EquipmentStats> snowboardStats = snowboardCounts.entrySet().stream()
                .map(entry -> new EquipmentStats(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> Integer.compare(b.getRentalsCount(), a.getRentalsCount()))
                .collect(Collectors.toList());

        return new EquipmentPopularity(skiStats, snowboardStats);
    }

    public EquipmentPopularity calculateEquipmentPopularityWithHistory(
            List<UserRental> currentRentals,
            List<Equipment> allEquipment,
            List<Transaction> transactions) {

        Map<String, Integer> skiHistoricalCounts = new HashMap<>();
        Map<String, Integer> snowboardHistoricalCounts = new HashMap<>();

        if (allEquipment != null) {
            for (Equipment eq : allEquipment) {
                String type = eq.getType().toLowerCase();
                String size = eq.getSize();

                if ("ski".equals(type)) {
                    skiHistoricalCounts.put(size, 0);
                } else if ("snowboard".equals(type)) {
                    snowboardHistoricalCounts.put(size, 0);
                }
            }
        }

        // Розрахунок з попередніх транзакцій бронювань
        if (transactions != null) {
            for (Transaction transaction : transactions) {
                if ("rent_eq".equals(transaction.getType())) {
                    // Примітка. Для цього потрібні дані про спорядження в транзакції або додатковий пошук
                }
            }
        }

        // Підрахунок поточних бронювань
        if (currentRentals != null) {
            for (UserRental rental : currentRentals) {
                String type = rental.getType().toLowerCase();
                String size = rental.getSize();

                if ("ski".equals(type)) {
                    skiHistoricalCounts.merge(size, 1, Integer::sum);
                } else if ("snowboard".equals(type)) {
                    snowboardHistoricalCounts.merge(size, 1, Integer::sum);
                }
            }
        }

        // Перетворення в статистику та сортування
        List<EquipmentStats> skiStats = skiHistoricalCounts.entrySet().stream()
                .map(entry -> new EquipmentStats(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> Integer.compare(b.getRentalsCount(), a.getRentalsCount()))
                .collect(Collectors.toList());

        List<EquipmentStats> snowboardStats = snowboardHistoricalCounts.entrySet().stream()
                .map(entry -> new EquipmentStats(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> Integer.compare(b.getRentalsCount(), a.getRentalsCount()))
                .collect(Collectors.toList());

        return new EquipmentPopularity(skiStats, snowboardStats);
    }

    public static class InstructorStats {
        private final String name;
        private final int lessonsCount;

        public InstructorStats(String name, int lessonsCount) {
            this.name = name;
            this.lessonsCount = lessonsCount;
        }

        public String getName() {
            return name;
        }

        public int getLessonsCount() {
            return lessonsCount;
        }
    }

    public static class EquipmentStats {
        private final String size;
        private final int rentalsCount;

        public EquipmentStats(String size, int rentalsCount) {
            this.size = size;
            this.rentalsCount = rentalsCount;
        }

        public String getSize() {
            return size;
        }

        public int getRentalsCount() {
            return rentalsCount;
        }
    }

    public static class EquipmentPopularity {
        private final List<EquipmentStats> skiStats;
        private final List<EquipmentStats> snowboardStats;

        public EquipmentPopularity(List<EquipmentStats> skiStats, List<EquipmentStats> snowboardStats) {
            this.skiStats = skiStats;
            this.snowboardStats = snowboardStats;
        }

        public List<EquipmentStats> getSkiStats() {
            return skiStats;
        }

        public List<EquipmentStats> getSnowboardStats() {
            return snowboardStats;
        }
    }
}
package org.example.service;

import org.example.model.*;

import org.example.model.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


public class DashboardService {

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final DateTimeFormatter dateOnlyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    public DashboardStats calculateUserStats(String username,
            List<Booking> allBookings,
            List<UserRental> allRentals,
            List<Lesson> allLessons) {

        if (username == null) {
            return new DashboardStats(0, 0, 0);
        }

        int activeBookings = 0;
        int currentRentals = 0;
        int upcomingLessons = 0;

        if (allBookings != null) {
            activeBookings = (int) allBookings.stream()
                    .filter(booking -> username.equals(booking.getUsername()))
                    .filter(this::isUpcoming)
                    .count();
        }

        if (allRentals != null) {
            currentRentals = (int) allRentals.stream()
                    .filter(rental -> username.equals(rental.getUsername()))
                    .count();
        }

        if (allLessons != null) {
            upcomingLessons = (int) allLessons.stream()
                    .filter(lesson -> username.equals(lesson.getUsername()))
                    .filter(this::isUpcoming)
                    .count();
        }

        return new DashboardStats(activeBookings, currentRentals, upcomingLessons);
    }


    public List<BookingDisplayInfo> prepareBookingsForDisplay(String username, List<Booking> allBookings) {
        if (allBookings == null || username == null) {
            return new ArrayList<>();
        }

        return allBookings.stream()
                .filter(booking -> username.equals(booking.getUsername()))
                .map(this::createBookingDisplayInfo)
                .sorted((a, b) -> a.getDateTime().compareTo(b.getDateTime()))
                .collect(Collectors.toList());
    }


    public List<RentalDisplayInfo> prepareRentalsForDisplay(String username, List<UserRental> allRentals) {
        if (allRentals == null || username == null) {
            return new ArrayList<>();
        }

        return allRentals.stream()
                .filter(rental -> username.equals(rental.getUsername()))
                .map(this::createRentalDisplayInfo)
                .collect(Collectors.toList());
    }

    //Фільтрування уроків для відображення інформаційної панелі користувача
    public List<LessonDisplayInfo> prepareLessonsForDisplay(String username, List<Lesson> allLessons) {
        if (allLessons == null || username == null) {
            return new ArrayList<>();
        }

        return allLessons.stream()
                .filter(lesson -> username.equals(lesson.getUsername()))
                .map(this::createLessonDisplayInfo)
                .sorted((a, b) -> a.getDateTime().compareTo(b.getDateTime()))
                .collect(Collectors.toList());
    }


    public List<ActivityDisplayInfo> prepareRecentActivityForDisplay(String username, List<Transaction> allTransactions,
            int limit) {
        if (allTransactions == null || username == null) {
            return new ArrayList<>();
        }

        return allTransactions.stream()
                .filter(transaction -> username.equals(transaction.getUsername()))
                .sorted((t1, t2) -> t2.getTime().compareTo(t1.getTime())) // Most recent first
                .limit(limit)
                .map(this::createActivityDisplayInfo)
                .collect(Collectors.toList());
    }

   //Перевірка, чи відбудеться бронювання/урок (у майбутньому)
    private boolean isUpcoming(Booking booking) {
        return booking.getTime().isAfter(LocalDateTime.now());
    }

    private boolean isUpcoming(Lesson lesson) {
        return lesson.getTime().isAfter(LocalDateTime.now());
    }


    private BookingDisplayInfo createBookingDisplayInfo(Booking booking) {
        String formattedDate = booking.getTime().format(dateFormatter);
        String status = isUpcoming(booking) ? "Upcoming" : "Expired";

        return new BookingDisplayInfo(
                booking.getSlot(),
                formattedDate,
                status,
                booking.getTime());
    }


    private RentalDisplayInfo createRentalDisplayInfo(UserRental rental) {
        return new RentalDisplayInfo(
                rental.getType(),
                rental.getSize(),
                rental.getRentedSince());
    }


    private LessonDisplayInfo createLessonDisplayInfo(Lesson lesson) {
        String formattedDate = lesson.getTime().format(dateFormatter);
        String status = isUpcoming(lesson) ? "Scheduled" : "Completed";

        return new LessonDisplayInfo(
                lesson.getInstructor(),
                formattedDate,
                status,
                lesson.getTime());
    }


    private ActivityDisplayInfo createActivityDisplayInfo(Transaction transaction) {
        String activityType = getActivityDisplayName(transaction.getType());
        String details = getActivityDetails(transaction.getType());
        String formattedDate = transaction.getTime().format(dateOnlyFormatter);
        String amount = String.format("$%.0f", transaction.getAmount());

        return new ActivityDisplayInfo(
                activityType,
                details,
                formattedDate,
                amount);
    }


    private String getActivityDisplayName(String type) {
        if (type == null)
            return "Unknown";

        switch (type) {
            case "booking":
                return "Booking";
            case "rent_eq":
                return "Equipment Rental";
            case "return_eq":
                return "Equipment Return";
            case "lesson":
                return "Lesson";
            case "cancel_booking":
                return "Booking Cancelled";
            default:
                return type;
        }
    }


    private String getActivityDetails(String type) {
        if (type == null)
            return "Activity";

        switch (type) {
            case "booking":
                return "Time slot reservation";
            case "rent_eq":
                return "Equipment rented";
            case "return_eq":
                return "Equipment returned";
            case "lesson":
                return "Lesson with instructor";
            case "cancel_booking":
                return "Booking cancelled";
            default:
                return "Activity";
        }
    }


    public ActivitySummary calculateActivitySummary(String username,
            List<Transaction> transactions,
            int days) {
        if (transactions == null || username == null) {
            return new ActivitySummary(0, 0.0, new HashMap<>());
        }

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);

        List<Transaction> recentTransactions = transactions.stream()
                .filter(t -> username.equals(t.getUsername()))
                .filter(t -> t.getTime().isAfter(cutoffDate))
                .collect(Collectors.toList());

        int totalActivities = recentTransactions.size();
        double totalSpent = recentTransactions.stream()
                .mapToDouble(Transaction::getAmount)
                .filter(amount -> amount > 0) // Only count positive amounts as spending
                .sum();

        Map<String, Integer> activityCounts = recentTransactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getType,
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)));

        return new ActivitySummary(totalActivities, totalSpent, activityCounts);
    }


    public NextActivity getNextUpcomingActivity(String username,
            List<Booking> bookings,
            List<Lesson> lessons) {
        if (username == null) {
            return new NextActivity("None", "No upcoming activities", null);
        }

        LocalDateTime now = LocalDateTime.now();

        // пошук наступного бронювання
        Optional<Booking> nextBooking = Optional.empty();
        if (bookings != null) {
            nextBooking = bookings.stream()
                    .filter(b -> username.equals(b.getUsername()))
                    .filter(b -> b.getTime().isAfter(now))
                    .min(Comparator.comparing(Booking::getTime));
        }

        // пошук наступного уроку
        Optional<Lesson> nextLesson = Optional.empty();
        if (lessons != null) {
            nextLesson = lessons.stream()
                    .filter(l -> username.equals(l.getUsername()))
                    .filter(l -> l.getTime().isAfter(now))
                    .min(Comparator.comparing(Lesson::getTime));
        }

        // Повернення найранішої майбутньої дії
        if (nextBooking.isPresent() && nextLesson.isPresent()) {
            if (nextBooking.get().getTime().isBefore(nextLesson.get().getTime())) {
                Booking booking = nextBooking.get();
                return new NextActivity("Booking",
                        "Time slot " + booking.getSlot(),
                        booking.getTime());
            } else {
                Lesson lesson = nextLesson.get();
                return new NextActivity("Lesson",
                        "With " + lesson.getInstructor(),
                        lesson.getTime());
            }
        } else if (nextBooking.isPresent()) {
            Booking booking = nextBooking.get();
            return new NextActivity("Booking",
                    "Time slot " + booking.getSlot(),
                    booking.getTime());
        } else if (nextLesson.isPresent()) {
            Lesson lesson = nextLesson.get();
            return new NextActivity("Lesson",
                    "With " + lesson.getInstructor(),
                    lesson.getTime());
        }

        return new NextActivity("None", "No upcoming activities", null);
    }

    public static class DashboardStats {
        private final int activeBookings;
        private final int currentRentals;
        private final int upcomingLessons;

        public DashboardStats(int activeBookings, int currentRentals, int upcomingLessons) {
            this.activeBookings = activeBookings;
            this.currentRentals = currentRentals;
            this.upcomingLessons = upcomingLessons;
        }

        public int getActiveBookings() {
            return activeBookings;
        }

        public int getCurrentRentals() {
            return currentRentals;
        }

        public int getUpcomingLessons() {
            return upcomingLessons;
        }
    }

    public static class BookingDisplayInfo {
        private final String slot;
        private final String formattedDate;
        private final String status;
        private final LocalDateTime dateTime;

        public BookingDisplayInfo(String slot, String formattedDate, String status, LocalDateTime dateTime) {
            this.slot = slot;
            this.formattedDate = formattedDate;
            this.status = status;
            this.dateTime = dateTime;
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

        public LocalDateTime getDateTime() {
            return dateTime;
        }
    }

    public static class RentalDisplayInfo {
        private final String type;
        private final String size;
        private final String rentedSince;

        public RentalDisplayInfo(String type, String size, String rentedSince) {
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

    public static class LessonDisplayInfo {
        private final String instructor;
        private final String formattedDate;
        private final String status;
        private final LocalDateTime dateTime;

        public LessonDisplayInfo(String instructor, String formattedDate, String status, LocalDateTime dateTime) {
            this.instructor = instructor;
            this.formattedDate = formattedDate;
            this.status = status;
            this.dateTime = dateTime;
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

        public LocalDateTime getDateTime() {
            return dateTime;
        }
    }

    public static class ActivityDisplayInfo {
        private final String activityType;
        private final String details;
        private final String formattedDate;
        private final String amount;

        public ActivityDisplayInfo(String activityType, String details, String formattedDate, String amount) {
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

    public static class ActivitySummary {
        private final int totalActivities;
        private final double totalSpent;
        private final Map<String, Integer> activityCounts;

        public ActivitySummary(int totalActivities, double totalSpent, Map<String, Integer> activityCounts) {
            this.totalActivities = totalActivities;
            this.totalSpent = totalSpent;
            this.activityCounts = activityCounts;
        }

        public int getTotalActivities() {
            return totalActivities;
        }

        public double getTotalSpent() {
            return totalSpent;
        }

        public Map<String, Integer> getActivityCounts() {
            return activityCounts;
        }
    }

    public static class NextActivity {
        private final String type;
        private final String description;
        private final LocalDateTime dateTime;

        public NextActivity(String type, String description, LocalDateTime dateTime) {
            this.type = type;
            this.description = description;
            this.dateTime = dateTime;
        }

        public String getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }

        public LocalDateTime getDateTime() {
            return dateTime;
        }
    }
}
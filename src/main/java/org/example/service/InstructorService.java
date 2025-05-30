package org.example.service;

import org.example.model.*;

import org.example.model.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;


public class InstructorService {

    private static final int VERY_BUSY_TODAY_THRESHOLD = 6;
    private static final int BUSY_TODAY_THRESHOLD = 4;
    private static final int BUSY_WEEK_THRESHOLD = 20;
    private static final int MODERATE_WEEK_THRESHOLD = 10;


    public List<InstructorWorkloadStats> calculateInstructorWorkloads(
            List<String> instructorNames,
            List<Lesson> allLessons) {

        if (instructorNames == null || allLessons == null) {
            return new ArrayList<>();
        }

        LocalDate today = LocalDate.now();
        int currentWeek = today.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());

        List<InstructorWorkloadStats> workloads = new ArrayList<>();

        for (String instructorName : instructorNames) {
            List<Lesson> instructorLessons = allLessons.stream()
                    .filter(lesson -> lesson.getInstructor().equals(instructorName))
                    .collect(Collectors.toList());

            int totalLessons = instructorLessons.size();

            int todayLessons = (int) instructorLessons.stream()
                    .filter(lesson -> lesson.getTime().toLocalDate().equals(today))
                    .count();

            int weekLessons = (int) instructorLessons.stream()
                    .filter(lesson -> {
                        LocalDate lessonDate = lesson.getTime().toLocalDate();
                        int lessonWeek = lessonDate.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
                        return lessonWeek == currentWeek;
                    })
                    .count();

            String status = determineInstructorStatus(todayLessons, weekLessons);

            workloads.add(new InstructorWorkloadStats(
                    instructorName, totalLessons, todayLessons, weekLessons, status));
        }

        return workloads;
    }

    //визначення статуса інструктора залежно від навантаження
    public String determineInstructorStatus(int todayLessons, int weekLessons) {
        if (todayLessons >= VERY_BUSY_TODAY_THRESHOLD) {
            return "Very Busy";
        }
        if (todayLessons >= BUSY_TODAY_THRESHOLD) {
            return "Busy";
        }
        if (weekLessons >= BUSY_WEEK_THRESHOLD) {
            return "Busy";
        }
        if (weekLessons >= MODERATE_WEEK_THRESHOLD) {
            return "Moderate";
        }
        return "Available";
    }


    public LessonValidationResult validateLessonBooking(
            String instructor, LocalDate date, Integer hour, String username, List<Lesson> existingLessons) {

        if (instructor == null || instructor.trim().isEmpty()) {
            return new LessonValidationResult(false, "Please select an instructor");
        }

        if (date == null) {
            return new LessonValidationResult(false, "Please select a date");
        }

        if (hour == null) {
            return new LessonValidationResult(false, "Please select an hour");
        }

        if (username == null || username.trim().isEmpty()) {
            return new LessonValidationResult(false, "User must be logged in");
        }

        LocalDateTime lessonTime = date.atTime(hour, 0);

        if (lessonTime.isBefore(LocalDateTime.now())) {
            return new LessonValidationResult(false, "Cannot book lessons in the past");
        }

        // Перевірка існуючих уроків користувача
        if (hasUserConflict(username, lessonTime, existingLessons)) {
            return new LessonValidationResult(false, "You already have a lesson at this time");
        }

        return new LessonValidationResult(true, "Valid lesson booking request");
    }


    public boolean hasUserConflict(String username, LocalDateTime lessonTime, List<Lesson> existingLessons) {
        if (existingLessons == null || username == null || lessonTime == null) {
            return false;
        }

        return existingLessons.stream()
                .anyMatch(lesson -> lesson.getUsername().equals(username) &&
                        lesson.getTime().equals(lessonTime));
    }


    public List<Integer> getAvailableLessonHours() {
        List<Integer> hours = new ArrayList<>();
        for (int hour = 8; hour <= 18; hour++) {
            hours.add(hour);
        }
        return hours;
    }


    public List<Lesson> filterLessonsForUser(List<Lesson> allLessons, String username, String userRole) {
        if (allLessons == null) {
            return new ArrayList<>();
        }

        if ("ADMIN".equals(userRole)) {
            return new ArrayList<>(allLessons);
        }

        return allLessons.stream()
                .filter(lesson -> username != null && username.equals(lesson.getUsername()))
                .collect(Collectors.toList());
    }


    public LessonStats calculateLessonStats(List<Lesson> lessons, LocalDate startDate, LocalDate endDate) {
        if (lessons == null || startDate == null || endDate == null) {
            return new LessonStats(0, 0, 0, new HashMap<>());
        }

        List<Lesson> filteredLessons = lessons.stream()
                .filter(lesson -> {
                    LocalDate lessonDate = lesson.getTime().toLocalDate();
                    return !lessonDate.isBefore(startDate) && !lessonDate.isAfter(endDate);
                })
                .collect(Collectors.toList());

        int totalLessons = filteredLessons.size();

        Set<String> uniqueUsers = filteredLessons.stream()
                .map(Lesson::getUsername)
                .collect(Collectors.toSet());
        int uniqueStudents = uniqueUsers.size();

        Set<String> uniqueInstructors = filteredLessons.stream()
                .map(Lesson::getInstructor)
                .collect(Collectors.toSet());
        int activeInstructors = uniqueInstructors.size();

        Map<String, Integer> instructorCounts = filteredLessons.stream()
                .collect(Collectors.groupingBy(
                        Lesson::getInstructor,
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)));

        return new LessonStats(totalLessons, uniqueStudents, activeInstructors, instructorCounts);
    }

   //наявність інструктора на певну дату та годину
    public InstructorAvailability checkInstructorAvailability(
            String instructor, LocalDate date, Integer hour, List<Lesson> existingLessons) {

        if (instructor == null || date == null || hour == null || existingLessons == null) {
            return new InstructorAvailability(false, "Invalid parameters");
        }

        LocalDateTime requestedTime = date.atTime(hour, 0);

        boolean hasConflict = existingLessons.stream()
                .anyMatch(lesson -> lesson.getInstructor().equals(instructor) &&
                        lesson.getTime().equals(requestedTime));

        if (hasConflict) {
            return new InstructorAvailability(false,
                    String.format("%s is not available at %s on %s", instructor, hour + ":00", date));
        }

        return new InstructorAvailability(true,
                String.format("%s is available at %s on %s", instructor, hour + ":00", date));
    }


    public String createLessonDescription(Lesson lesson) {
        if (lesson == null) {
            return "No lesson selected";
        }

        return String.format("Lesson with %s on %s at %s",
                lesson.getInstructor(),
                lesson.getTime().toLocalDate(),
                lesson.getTime().getHour() + ":00");
    }


    public String createWorkloadTooltip(InstructorWorkloadStats workload) {
        if (workload == null) {
            return "";
        }

        return String.format("%s - Total: %d, Today: %d, This Week: %d (%s)",
                workload.getInstructorName(),
                workload.getTotalLessons(),
                workload.getTodayLessons(),
                workload.getWeekLessons(),
                workload.getStatus());
    }


    public static class InstructorWorkloadStats {
        private final String instructorName;
        private final int totalLessons;
        private final int todayLessons;
        private final int weekLessons;
        private final String status;

        public InstructorWorkloadStats(String instructorName, int totalLessons,
                int todayLessons, int weekLessons, String status) {
            this.instructorName = instructorName;
            this.totalLessons = totalLessons;
            this.todayLessons = todayLessons;
            this.weekLessons = weekLessons;
            this.status = status;
        }

        public String getInstructorName() {
            return instructorName;
        }

        public int getTotalLessons() {
            return totalLessons;
        }

        public int getTodayLessons() {
            return todayLessons;
        }

        public int getWeekLessons() {
            return weekLessons;
        }

        public String getStatus() {
            return status;
        }
    }

    public static class LessonValidationResult {
        private final boolean valid;
        private final String message;

        public LessonValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class LessonStats {
        private final int totalLessons;
        private final int uniqueStudents;
        private final int activeInstructors;
        private final Map<String, Integer> instructorLessonCounts;

        public LessonStats(int totalLessons, int uniqueStudents,
                int activeInstructors, Map<String, Integer> instructorLessonCounts) {
            this.totalLessons = totalLessons;
            this.uniqueStudents = uniqueStudents;
            this.activeInstructors = activeInstructors;
            this.instructorLessonCounts = instructorLessonCounts;
        }

        public int getTotalLessons() {
            return totalLessons;
        }

        public int getUniqueStudents() {
            return uniqueStudents;
        }

        public int getActiveInstructors() {
            return activeInstructors;
        }

        public Map<String, Integer> getInstructorLessonCounts() {
            return instructorLessonCounts;
        }
    }

    public static class InstructorAvailability {
        private final boolean available;
        private final String message;

        public InstructorAvailability(boolean available, String message) {
            this.available = available;
            this.message = message;
        }

        public boolean isAvailable() {
            return available;
        }

        public String getMessage() {
            return message;
        }
    }
}
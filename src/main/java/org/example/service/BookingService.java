package org.example.service;

import org.example.model.*;

import org.example.model.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;

public class BookingService {


    public LocalDateTime getTimeFromSlot(String slot, LocalDate date) {
        if (slot == null || date == null) {
            return date != null ? date.atTime(9, 0) : LocalDateTime.now().withHour(9).withMinute(0);
        }

        LocalTime startTime;
        switch (slot) {
            case "9-13":
                startTime = LocalTime.of(9, 0);
                break;
            case "13-17":
                startTime = LocalTime.of(13, 0);
                break;
            case "17-20":
                startTime = LocalTime.of(17, 0);
                break;
            default:
                startTime = LocalTime.of(9, 0);
        }
        return date.atTime(startTime);
    }


    public BookingValidationResult validateBookingRequest(String slot, LocalDate date, String username) {
        if (slot == null || slot.trim().isEmpty()) {
            return new BookingValidationResult(false, "Please select a time slot");
        }

        if (date == null) {
            return new BookingValidationResult(false, "Please select a date");
        }

        if (username == null || username.trim().isEmpty()) {
            return new BookingValidationResult(false, "User must be logged in");
        }

        LocalDateTime bookingTime = getTimeFromSlot(slot, date);
        if (bookingTime.isBefore(LocalDateTime.now())) {
            return new BookingValidationResult(false, "Cannot book time slots in the past");
        }

        return new BookingValidationResult(true, "Valid booking request");
    }


    public BookingValidationResult validateTransferRequest(Booking booking, String newSlot, LocalDate newDate,
                                                           String currentUsername) {
        if (booking == null) {
            return new BookingValidationResult(false, "Please select a booking to transfer");
        }

        if (!booking.getUsername().equals(currentUsername)) {
            return new BookingValidationResult(false, "You can only transfer your own bookings");
        }

        if (newSlot == null || newSlot.trim().isEmpty()) {
            return new BookingValidationResult(false, "Please select a new time slot");
        }

        if (newDate == null) {
            return new BookingValidationResult(false, "Please select a new date");
        }

        LocalDateTime newBookingTime = getTimeFromSlot(newSlot, newDate);
        if (newBookingTime.isBefore(LocalDateTime.now())) {
            return new BookingValidationResult(false, "Cannot transfer to time slots in the past");
        }

        // Check if it's the same slot and time (no change needed)
        if (booking.getSlot().equals(newSlot) && booking.getTime().toLocalDate().equals(newDate)) {
            return new BookingValidationResult(false, "New slot and date are the same as current booking");
        }

        return new BookingValidationResult(true, "Valid transfer request");
    }


    public BookingValidationResult validateCancellationRequest(Booking booking, String currentUsername) {
        if (booking == null) {
            return new BookingValidationResult(false, "Please select a booking to cancel");
        }

        if (!booking.getUsername().equals(currentUsername)) {
            return new BookingValidationResult(false, "You can only cancel your own bookings");
        }

        return new BookingValidationResult(true, "Valid cancellation request");
    }


    public List<Booking> filterBookingsForUser(List<Booking> allBookings, String username, String userRole) {
        if (allBookings == null) {
            return new ArrayList<>();
        }

        if ("ADMIN".equals(userRole)) {
            return new ArrayList<>(allBookings);
        }

        List<Booking> userBookings = new ArrayList<>();
        for (Booking booking : allBookings) {
            if (username != null && username.equals(booking.getUsername())) {
                userBookings.add(booking);
            }
        }
        return userBookings;
    }


    public List<String> getAvailableTimeSlots() {
        List<String> slots = new ArrayList<>();
        slots.add("9-13");
        slots.add("13-17");
        slots.add("17-20");
        return slots;
    }


    public TimeSlotInfo parseTimeSlot(String slot) {
        if (slot == null || slot.trim().isEmpty()) {
            return new TimeSlotInfo(9, 13, false);
        }

        String[] parts = slot.split("-");
        if (parts.length != 2) {
            return new TimeSlotInfo(9, 13, false);
        }

        try {
            int startHour = Integer.parseInt(parts[0].trim());
            int endHour = Integer.parseInt(parts[1].trim());

            if (startHour >= 0 && startHour < 24 && endHour > startHour && endHour <= 24) {
                return new TimeSlotInfo(startHour, endHour, true);
            }
        } catch (NumberFormatException e) {
            // Invalid format, return default
        }

        return new TimeSlotInfo(9, 13, false);
    }

    public static class BookingValidationResult {
        private final boolean valid;
        private final String message;

        public BookingValidationResult(boolean valid, String message) {
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

    public static class TimeSlotInfo {
        private final int startHour;
        private final int endHour;
        private final boolean valid;

        public TimeSlotInfo(int startHour, int endHour, boolean valid) {
            this.startHour = startHour;
            this.endHour = endHour;
            this.valid = valid;
        }

        public int getStartHour() {
            return startHour;
        }

        public int getEndHour() {
            return endHour;
        }

        public boolean isValid() {
            return valid;
        }

        public int getDuration() {
            return endHour - startHour;
        }
    }
}
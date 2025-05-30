package org.example.service;

import org.example.model.*;

import org.example.model.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class EquipmentService {


    public List<String> getAvailableEquipmentTypes() {
        return Arrays.asList("ski", "snowboard");
    }


    public List<String> getAvailableSizes() {
        return Arrays.asList("S", "M", "L", "XL", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45");
    }


    public EquipmentValidationResult validateRentalRequest(String type, String size, String username) {
        if (username == null || username.trim().isEmpty()) {
            return new EquipmentValidationResult(false, "User must be logged in");
        }

        if (type == null || type.trim().isEmpty()) {
            return new EquipmentValidationResult(false, "Please select equipment type");
        }

        if (size == null || size.trim().isEmpty()) {
            return new EquipmentValidationResult(false, "Please select equipment size");
        }

        if (!getAvailableEquipmentTypes().contains(type.toLowerCase())) {
            return new EquipmentValidationResult(false, "Invalid equipment type selected");
        }

        if (!getAvailableSizes().contains(size)) {
            return new EquipmentValidationResult(false, "Invalid size selected");
        }

        return new EquipmentValidationResult(true, "Valid rental request");
    }


    public EquipmentValidationResult validateReturnRequest(UserRental rental, String username) {
        if (username == null || username.trim().isEmpty()) {
            return new EquipmentValidationResult(false, "User must be logged in");
        }

        if (rental == null) {
            return new EquipmentValidationResult(false, "Please select equipment to return");
        }

        if (!rental.getUsername().equals(username)) {
            return new EquipmentValidationResult(false, "You can only return equipment you have rented");
        }

        return new EquipmentValidationResult(true, "Valid return request");
    }


    public Equipment findAvailableEquipment(List<Equipment> allEquipment, String type, String size) {
        if (allEquipment == null || type == null || size == null) {
            return null;
        }

        for (Equipment equipment : allEquipment) {
            if (equipment.getType().equalsIgnoreCase(type) &&
                    equipment.getSize().equals(size) &&
                    equipment.getAvailable() > 0) {
                return equipment;
            }
        }

        return null;
    }


    public List<Equipment> filterAvailableEquipment(List<Equipment> allEquipment) {
        if (allEquipment == null) {
            return new ArrayList<>();
        }

        List<Equipment> available = new ArrayList<>();
        for (Equipment equipment : allEquipment) {
            if (equipment.getAvailable() > 0) {
                available.add(equipment);
            }
        }
        return available;
    }


    public EquipmentTypeStats calculateEquipmentStats(List<Equipment> allEquipment, String type) {
        if (allEquipment == null || type == null) {
            return new EquipmentTypeStats(type, 0, 0, 0);
        }

        int totalItems = 0;
        int availableItems = 0;
        int rentedItems = 0;

        for (Equipment equipment : allEquipment) {
            if (equipment.getType().equalsIgnoreCase(type)) {
                totalItems += equipment.getAvailable(); // This represents current available count
                availableItems += equipment.getAvailable();
                // Note: We'd need rental history to calculate actual total and rented counts
            }
        }

        return new EquipmentTypeStats(type, totalItems, availableItems, rentedItems);
    }


    public String createRentalDisplayString(UserRental rental) {
        if (rental == null) {
            return "No rental selected";
        }

        return String.format("%s (Size: %s)",
                capitalize(rental.getType()),
                rental.getSize());
    }


    public boolean hasReachedRentalLimit(List<UserRental> userRentals, int maxRentalsPerUser) {
        if (userRentals == null) {
            return false;
        }

        return userRentals.size() >= maxRentalsPerUser;
    }


    public RentalSummary getUserRentalSummary(List<UserRental> userRentals) {
        if (userRentals == null || userRentals.isEmpty()) {
            return new RentalSummary(0, 0, 0);
        }

        int totalRentals = userRentals.size();
        int skiRentals = 0;
        int snowboardRentals = 0;

        for (UserRental rental : userRentals) {
            if ("ski".equalsIgnoreCase(rental.getType())) {
                skiRentals++;
            } else if ("snowboard".equalsIgnoreCase(rental.getType())) {
                snowboardRentals++;
            }
        }

        return new RentalSummary(totalRentals, skiRentals, snowboardRentals);
    }


    public AvailabilityResult checkAvailability(Equipment equipment) {
        if (equipment == null) {
            return new AvailabilityResult(false, "Equipment not found");
        }

        if (equipment.getAvailable() <= 0) {
            return new AvailabilityResult(false,
                    String.format("No %s equipment available in size %s",
                            equipment.getType(), equipment.getSize()));
        }

        return new AvailabilityResult(true,
                String.format("%d %s(s) available in size %s",
                        equipment.getAvailable(), equipment.getType(), equipment.getSize()));
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }


    public static class EquipmentValidationResult {
        private final boolean valid;
        private final String message;

        public EquipmentValidationResult(boolean valid, String message) {
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

    public static class EquipmentTypeStats {
        private final String type;
        private final int totalItems;
        private final int availableItems;
        private final int rentedItems;

        public EquipmentTypeStats(String type, int totalItems, int availableItems, int rentedItems) {
            this.type = type;
            this.totalItems = totalItems;
            this.availableItems = availableItems;
            this.rentedItems = rentedItems;
        }

        public String getType() {
            return type;
        }

        public int getTotalItems() {
            return totalItems;
        }

        public int getAvailableItems() {
            return availableItems;
        }

        public int getRentedItems() {
            return rentedItems;
        }

        public double getUtilizationRate() {
            return totalItems > 0 ? (double) rentedItems / totalItems : 0.0;
        }
    }

    public static class RentalSummary {
        private final int totalRentals;
        private final int skiRentals;
        private final int snowboardRentals;

        public RentalSummary(int totalRentals, int skiRentals, int snowboardRentals) {
            this.totalRentals = totalRentals;
            this.skiRentals = skiRentals;
            this.snowboardRentals = snowboardRentals;
        }

        public int getTotalRentals() {
            return totalRentals;
        }

        public int getSkiRentals() {
            return skiRentals;
        }

        public int getSnowboardRentals() {
            return snowboardRentals;
        }
    }

    public static class AvailabilityResult {
        private final boolean available;
        private final String message;

        public AvailabilityResult(boolean available, String message) {
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
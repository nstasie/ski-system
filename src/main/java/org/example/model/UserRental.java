package org.example.model;

import javafx.beans.property.*;

//Заброньоване спорядження користувачем
public class UserRental {
    private final IntegerProperty equipmentId = new SimpleIntegerProperty();
    private final StringProperty type = new SimpleStringProperty();
    private final StringProperty size = new SimpleStringProperty();
    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty rentedSince = new SimpleStringProperty();

    public UserRental(int equipmentId, String type, String size, String username, String rentedSince) {
        this.equipmentId.set(equipmentId);
        this.type.set(type);
        this.size.set(size);
        this.username.set(username);
        this.rentedSince.set(rentedSince);
    }

    public int getEquipmentId() {
        return equipmentId.get();
    }

    public String getType() {
        return type.get();
    }

    public String getSize() {
        return size.get();
    }

    public String getUsername() {
        return username.get();
    }

    public String getRentedSince() {
        return rentedSince.get();
    }
}

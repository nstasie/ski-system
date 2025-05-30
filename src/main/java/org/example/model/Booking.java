package org.example.model;

import java.time.LocalDateTime;
import javafx.beans.property.*;

//Бронювання скі-пасів
public class Booking {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty slot = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> time = new SimpleObjectProperty<>();

    public Booking(int id, String user, String slot, LocalDateTime time) {
        this.id.set(id);
        this.username.set(user);
        this.slot.set(slot);
        this.time.set(time);
    }

    public int getId() {
        return id.get();
    }

    public String getUsername() {
        return username.get();
    }

    public String getSlot() {
        return slot.get();
    }

    public LocalDateTime getTime() {
        return time.get();
    }
}

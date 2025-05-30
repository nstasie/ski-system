package org.example.model;

import java.time.LocalDateTime;
import javafx.beans.property.*;

//Транзакції
public class Transaction {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty type = new SimpleStringProperty();
    private final DoubleProperty amount = new SimpleDoubleProperty();
    private final ObjectProperty<LocalDateTime> time = new SimpleObjectProperty<>();

    public Transaction(int id, String user, String type, double amount, LocalDateTime time) {
        this.id.set(id);
        this.username.set(user);
        this.type.set(type);
        this.amount.set(amount);
        this.time.set(time);
    }

    public int getId() {
        return id.get();
    }

    public String getUsername() {
        return username.get();
    }

    public String getType() {
        return type.get();
    }

    public double getAmount() {
        return amount.get();
    }

    public LocalDateTime getTime() {
        return time.get();
    }
}

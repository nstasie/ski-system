package org.example.model;

import javafx.beans.property.*;

//Спорядження
public class Equipment {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty type = new SimpleStringProperty();
    private final StringProperty size = new SimpleStringProperty();
    private final IntegerProperty available = new SimpleIntegerProperty();

    public Equipment(int id, String type, String size, int available) {
        this.id.set(id);
        this.type.set(type);
        this.size.set(size);
        this.available.set(available);
    }

    public int getId() {
        return id.get();
    }

    public String getType() {
        return type.get();
    }

    public String getSize() {
        return size.get();
    }

    public int getAvailable() {
        return available.get();
    }
}

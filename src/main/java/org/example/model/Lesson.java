package org.example.model;

import java.time.LocalDateTime;
import javafx.beans.property.*;

//Уроки з інструктором
public class Lesson {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty instructor = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> time = new SimpleObjectProperty<>();

    public Lesson(int id, String user, String instr, LocalDateTime time) {
        this.id.set(id);
        this.username.set(user);
        this.instructor.set(instr);
        this.time.set(time);
    }

    public int getId() {
        return id.get();
    }

    public String getUsername() {
        return username.get();
    }

    public String getInstructor() {
        return instructor.get();
    }

    public LocalDateTime getTime() {
        return time.get();
    }
}

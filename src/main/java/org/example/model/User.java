package org.example.model;

//Користувачі даного сервісу
public class User {
    private final String username, role;

    public User(String username, String role) {
        this.username = username;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}

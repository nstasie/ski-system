package org.example;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.example.model.Booking;
import org.example.model.Equipment;
import org.example.model.Lesson;
import org.example.model.Transaction;
import org.example.model.User;
import org.example.model.UserRental;


public class Services {
    private static Connection conn;

    public static void initDB() throws SQLException {
        try {
            Logger.logSystemEvent("DATABASE_INIT", "Starting database initialization");

            conn = DriverManager.getConnection("jdbc:sqlite:skiservice.db");
            try (Statement s = conn.createStatement()) {
                s.execute("PRAGMA foreign_keys = ON;");

                s.execute("""
                            CREATE TABLE IF NOT EXISTS users(
                              id INTEGER PRIMARY KEY,
                              username TEXT UNIQUE NOT NULL,
                              password TEXT NOT NULL,
                              role TEXT NOT NULL
                            );
                        """);
                s.execute("""
                            CREATE TABLE IF NOT EXISTS bookings(
                              id INTEGER PRIMARY KEY,
                              username TEXT NOT NULL,
                              slot TEXT NOT NULL,
                              time TEXT NOT NULL
                            );
                        """);
                s.execute("""
                            CREATE TABLE IF NOT EXISTS equipment(
                              id INTEGER PRIMARY KEY,
                              type TEXT NOT NULL,
                              size TEXT NOT NULL,
                              total INTEGER NOT NULL DEFAULT 0,
                              available INTEGER NOT NULL DEFAULT 0
                            );
                        """);
                s.execute("""
                            CREATE TABLE IF NOT EXISTS equipment_rent(
                              id INTEGER PRIMARY KEY,
                              eq_id INTEGER NOT NULL,
                              username TEXT NOT NULL,
                              FOREIGN KEY(eq_id) REFERENCES equipment(id)
                            );
                        """);
                s.execute("""
                            CREATE TABLE IF NOT EXISTS instructors(
                              id INTEGER PRIMARY KEY,
                              name TEXT NOT NULL
                            );
                        """);
                s.execute("""
                            CREATE TABLE IF NOT EXISTS lessons(
                              id INTEGER PRIMARY KEY,
                              username TEXT NOT NULL,
                              instructor TEXT NOT NULL,
                              time TEXT NOT NULL
                            );
                        """);
                s.execute("""
                            CREATE TABLE IF NOT EXISTS trans(
                              id INTEGER PRIMARY KEY,
                              username TEXT NOT NULL,
                              type TEXT NOT NULL,
                              amount REAL NOT NULL DEFAULT 0.0,
                              time TEXT NOT NULL
                            );
                        """);
            }

            try (PreparedStatement p = conn.prepareStatement("SELECT COUNT(*) FROM users")) {
                ResultSet rs = p.executeQuery();
                rs.next();
                if (rs.getInt(1) == 0) {
                    try (PreparedStatement userStmt = conn.prepareStatement(
                            "INSERT INTO users(username,password,role) VALUES (?,?,?)")) {
                        userStmt.setString(1, "admin");
                        userStmt.setString(2, "admin");
                        userStmt.setString(3, "ADMIN");
                        userStmt.executeUpdate();

                        userStmt.setString(1, "user");
                        userStmt.setString(2, "user");
                        userStmt.setString(3, "USER");
                        userStmt.executeUpdate();
                    }

                    try (PreparedStatement equipStmt = conn.prepareStatement(
                            "INSERT INTO equipment(type,size,total,available) VALUES (?,?,?,?)")) {
                        equipStmt.setString(1, "Ski");
                        equipStmt.setString(2, "42");
                        equipStmt.setInt(3, 5);
                        equipStmt.setInt(4, 5);
                        equipStmt.executeUpdate();

                        equipStmt.setString(1, "Ski");
                        equipStmt.setString(2, "43");
                        equipStmt.setInt(3, 5);
                        equipStmt.setInt(4, 5);
                        equipStmt.executeUpdate();

                        equipStmt.setString(1, "Snowboard");
                        equipStmt.setString(2, "M");
                        equipStmt.setInt(3, 3);
                        equipStmt.setInt(4, 3);
                        equipStmt.executeUpdate();
                    }

                    try (PreparedStatement instrStmt = conn.prepareStatement(
                            "INSERT INTO instructors(name) VALUES (?)")) {
                        instrStmt.setString(1, "Ivan");
                        instrStmt.executeUpdate();

                        instrStmt.setString(1, "Olena");
                        instrStmt.executeUpdate();
                    }

                    Logger.logSystemEvent("DATABASE_SEEDED", "Initial data created");
                }
            }
            Logger.logSystemEvent("DATABASE_INIT", "Database initialization completed successfully");
        } catch (SQLException e) {
            Logger.logError("DATABASE_INIT", "SYSTEM", e.getMessage(), "Database initialization failed");
            throw e;
        }
    }

    public static class AuthService {
        private static User currentUser;

        public static boolean login(String username, String password) throws SQLException {
            String params = String.format("username=%s", username);

            try {
                if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
                    Logger.logError("LOGIN", username, "Empty credentials", params);
                    return false;
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT role FROM users WHERE username=? AND password=?")) {
                    ps.setString(1, username.trim());
                    ps.setString(2, password.trim());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        currentUser = new User(username.trim(), rs.getString("role"));
                        Logger.logUserAction("LOGIN_SUCCESS", username.trim(), params);
                        return true;
                    }
                }

                Logger.logUserAction("LOGIN_FAILED", username, params);
                return false;
            } catch (SQLException e) {
                Logger.logError("LOGIN", username, e.getMessage(), params);
                throw e;
            }
        }

        public static boolean register(String username, String password) throws SQLException {
            String params = String.format("username=%s", username);

            try {
                if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
                    Logger.logError("REGISTER", username, "Empty credentials", params);
                    throw new SQLException("Username and password cannot be empty");
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO users(username,password,role) VALUES(?,?,?)")) {
                    ps.setString(1, username.trim());
                    ps.setString(2, password.trim());
                    ps.setString(3, "USER");
                    boolean success = ps.executeUpdate() == 1;

                    if (success) {
                        Logger.logUserAction("REGISTER_SUCCESS", username.trim(), params);
                    } else {
                        Logger.logError("REGISTER", username, "Failed to insert user", params);
                    }

                    return success;
                }
            } catch (SQLException e) {
                Logger.logError("REGISTER", username, e.getMessage(), params);
                throw e;
            }
        }
        public static User getCurrentUser() {
            return currentUser;
        }
    }

    public static class BookingService {
        private static final DateTimeFormatter F = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        public static void book(String user, String slot, LocalDateTime when) throws SQLException {
            String params = String.format("slot=%s, time=%s", slot, when);

            try {
                if (user == null || slot == null || when == null) {
                    Logger.logError("BOOKING_CREATE", user, "Null parameters", params);
                    throw new SQLException("User, slot, and time cannot be null");
                }

                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO bookings(username,slot,time) VALUES(?,?,?)")) {
                    ins.setString(1, user);
                    ins.setString(2, slot);
                    ins.setString(3, when.format(F));
                    ins.executeUpdate();
                }

                TransactionService.log(user, "booking", 50.0, when);
                Logger.logUserAction("BOOKING_CREATE", user, params);

            } catch (SQLException e) {
                Logger.logError("BOOKING_CREATE", user, e.getMessage(), params);
                throw e;
            }
        }

        public static void cancel(int id) throws SQLException {
            String params = String.format("booking_id=%d", id);
            String user = "UNKNOWN";

            try {
                try (PreparedStatement getUser = conn.prepareStatement("SELECT username FROM bookings WHERE id=?")) {
                    getUser.setInt(1, id);
                    ResultSet rs = getUser.executeQuery();
                    if (rs.next()) {
                        user = rs.getString("username");
                    }
                }

                try (PreparedStatement d = conn.prepareStatement("DELETE FROM bookings WHERE id=?")) {
                    d.setInt(1, id);
                    int deleted = d.executeUpdate();

                    if (deleted > 0) {
                        TransactionService.log("system", "cancel_booking", -50.0, LocalDateTime.now());
                        Logger.logUserAction("BOOKING_CANCEL", user, params);
                    } else {
                        Logger.logError("BOOKING_CANCEL", user, "Booking not found", params);
                    }
                }
            } catch (SQLException e) {
                Logger.logError("BOOKING_CANCEL", user, e.getMessage(), params);
                throw e;
            }
        }

        public static void transfer(int id, String slot, LocalDateTime when) throws SQLException {
            String params = String.format("booking_id=%d, new_slot=%s, new_time=%s", id, slot, when);
            String user = "UNKNOWN";

            try {
                if (slot == null || when == null) {
                    Logger.logError("BOOKING_TRANSFER", user, "Null parameters", params);
                    throw new SQLException("Slot and time cannot be null");
                }

                try (PreparedStatement getUser = conn.prepareStatement("SELECT username FROM bookings WHERE id=?")) {
                    getUser.setInt(1, id);
                    ResultSet rs = getUser.executeQuery();
                    if (rs.next()) {
                        user = rs.getString("username");
                    }
                }

                try (PreparedStatement u = conn.prepareStatement(
                        "UPDATE bookings SET slot=?, time=? WHERE id=?")) {
                    u.setString(1, slot);
                    u.setString(2, when.format(F));
                    u.setInt(3, id);
                    int updated = u.executeUpdate();

                    if (updated > 0) {
                        Logger.logUserAction("BOOKING_TRANSFER", user, params);
                    } else {
                        Logger.logError("BOOKING_TRANSFER", user, "Booking not found", params);
                    }
                }
            } catch (SQLException e) {
                Logger.logError("BOOKING_TRANSFER", user, e.getMessage(), params);
                throw e;
            }
        }

        public static ObservableList<Booking> listAll() {
            ObservableList<Booking> L = FXCollections.observableArrayList();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM bookings")) {
                while (rs.next()) {
                    L.add(new Booking(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("slot"),
                            LocalDateTime.parse(rs.getString("time"), F)));
                }
            } catch (Exception e) {
                Logger.logError("BOOKING_LIST", "SYSTEM", e.getMessage(), "Loading all bookings");
                e.printStackTrace();
            }
            return L;
        }

        public static int countAll() {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM bookings")) {
                rs.next();
                return rs.getInt(1);
            } catch (Exception e) {
                Logger.logError("BOOKING_COUNT", "SYSTEM", e.getMessage(), "Counting all bookings");
                return 0;
            }
        }

        public static int countByUser(String user) {
            if (user == null)
                return 0;

            try (PreparedStatement p = conn.prepareStatement(
                    "SELECT COUNT(*) FROM bookings WHERE username=?")) {
                p.setString(1, user);
                ResultSet rs = p.executeQuery();
                if (rs.next())
                    return rs.getInt(1);
            } catch (Exception e) {
                Logger.logError("BOOKING_COUNT_USER", user, e.getMessage(), String.format("user=%s", user));
                e.printStackTrace();
            }
            return 0;
        }
    }

    public static class EquipmentService {
        public static ObservableList<Equipment> listAll() {
            ObservableList<Equipment> L = FXCollections.observableArrayList();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM equipment")) {
                while (rs.next()) {
                    L.add(new Equipment(
                            rs.getInt("id"),
                            rs.getString("type"),
                            rs.getString("size"),
                            rs.getInt("available")));
                }
            } catch (Exception e) {
                Logger.logError("EQUIPMENT_LIST", "SYSTEM", e.getMessage(), "Loading all equipment");
                e.printStackTrace();
            }
            return L;
        }

        public static void rent(int eqId, String user) throws SQLException {
            String params = String.format("equipment_id=%d, user=%s", eqId, user);

            try {
                if (user == null || user.trim().isEmpty()) {
                    Logger.logError("EQUIPMENT_RENT", user, "Empty user", params);
                    throw new SQLException("User cannot be null or empty");
                }

                try (PreparedStatement p = conn.prepareStatement(
                        "SELECT available, type, size FROM equipment WHERE id=?")) {
                    p.setInt(1, eqId);
                    ResultSet rs = p.executeQuery();
                    if (!rs.next()) {
                        Logger.logError("EQUIPMENT_RENT", user, "Equipment not found", params);
                        throw new SQLException("Equipment not found");
                    }
                    if (rs.getInt("available") < 1) {
                        String equipmentInfo = String.format("type=%s, size=%s", rs.getString("type"),
                                rs.getString("size"));
                        Logger.logError("EQUIPMENT_RENT", user, "None available", params + ", " + equipmentInfo);
                        throw new SQLException("None available");
                    }
                }

                try (PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE equipment SET available=available-1 WHERE id=?")) {
                    updateStmt.setInt(1, eqId);
                    updateStmt.executeUpdate();
                }

                try (PreparedStatement insertStmt = conn.prepareStatement(
                        "INSERT INTO equipment_rent(eq_id,username) VALUES(?,?)")) {
                    insertStmt.setInt(1, eqId);
                    insertStmt.setString(2, user);
                    insertStmt.executeUpdate();
                }

                TransactionService.log(user, "rent_eq", 20.0, LocalDateTime.now());
                Logger.logUserAction("EQUIPMENT_RENT", user, params);

            } catch (SQLException e) {
                Logger.logError("EQUIPMENT_RENT", user, e.getMessage(), params);
                throw e;
            }
        }

        public static void ret(int eqId, String user) throws SQLException {
            String params = String.format("equipment_id=%d, user=%s", eqId, user);

            try {
                if (user == null || user.trim().isEmpty()) {
                    Logger.logError("EQUIPMENT_RETURN", user, "Empty user", params);
                    throw new SQLException("User cannot be null or empty");
                }

                try (PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE equipment SET available=available+1 WHERE id=?")) {
                    updateStmt.setInt(1, eqId);
                    updateStmt.executeUpdate();
                }

                try (PreparedStatement deleteStmt = conn.prepareStatement(
                        "DELETE FROM equipment_rent WHERE eq_id=? AND username=?")) {
                    deleteStmt.setInt(1, eqId);
                    deleteStmt.setString(2, user);
                    int deleted = deleteStmt.executeUpdate();

                    if (deleted > 0) {
                        TransactionService.log(user, "return_eq", -20.0, LocalDateTime.now());
                        Logger.logUserAction("EQUIPMENT_RETURN", user, params);
                    } else {
                        Logger.logError("EQUIPMENT_RETURN", user, "Rental record not found", params);
                    }
                }
            } catch (SQLException e) {
                Logger.logError("EQUIPMENT_RETURN", user, e.getMessage(), params);
                throw e;
            }
        }

        public static int countAll() {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM equipment_rent")) {
                rs.next();
                return rs.getInt(1);
            } catch (Exception e) {
                Logger.logError("EQUIPMENT_COUNT", "SYSTEM", e.getMessage(), "Counting all rentals");
                return 0;
            }
        }

        public static int countByUser(String user) {
            if (user == null)
                return 0;

            try (PreparedStatement p = conn.prepareStatement(
                    "SELECT COUNT(*) FROM equipment_rent WHERE username=?")) {
                p.setString(1, user);
                ResultSet rs = p.executeQuery();
                if (rs.next())
                    return rs.getInt(1);
            } catch (Exception e) {
                Logger.logError("EQUIPMENT_COUNT_USER", user, e.getMessage(), String.format("user=%s", user));
                e.printStackTrace();
            }
            return 0;
        }

        public static ObservableList<UserRental> getCurrentRentals(String username) {
            ObservableList<UserRental> rentals = FXCollections.observableArrayList();
            if (username == null)
                return rentals;

            try (PreparedStatement p = conn.prepareStatement(
                    "SELECT er.eq_id, e.type, e.size, er.username " +
                            "FROM equipment_rent er " +
                            "JOIN equipment e ON er.eq_id = e.id " +
                            "WHERE er.username = ?")) {
                p.setString(1, username);
                ResultSet rs = p.executeQuery();

                while (rs.next()) {
                    rentals.add(new UserRental(
                            rs.getInt("eq_id"),
                            rs.getString("type"),
                            rs.getString("size"),
                            rs.getString("username"),
                            "Active"));
                }
            } catch (Exception e) {
                Logger.logError("EQUIPMENT_GET_RENTALS", username, e.getMessage(), String.format("user=%s", username));
                e.printStackTrace();
            }
            return rentals;
        }

        public static ObservableList<UserRental> getAllCurrentRentals() {
            ObservableList<UserRental> rentals = FXCollections.observableArrayList();
            try (PreparedStatement p = conn.prepareStatement(
                    "SELECT er.eq_id, e.type, e.size, er.username " +
                            "FROM equipment_rent er " +
                            "JOIN equipment e ON er.eq_id = e.id")) {
                ResultSet rs = p.executeQuery();

                while (rs.next()) {
                    rentals.add(new UserRental(
                            rs.getInt("eq_id"),
                            rs.getString("type"),
                            rs.getString("size"),
                            rs.getString("username"),
                            "Active"));
                }
            } catch (Exception e) {
                Logger.logError("EQUIPMENT_GET_ALL_RENTALS", "SYSTEM", e.getMessage(), "Loading all current rentals");
                e.printStackTrace();
            }
            return rentals;
        }
    }

    public static class InstructorService {
        private static final DateTimeFormatter F = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        public static List<String> listNames() {
            List<String> L = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT name FROM instructors")) {
                while (rs.next())
                    L.add(rs.getString(1));
            } catch (Exception e) {
                Logger.logError("INSTRUCTOR_LIST_NAMES", "SYSTEM", e.getMessage(), "Loading instructor names");
                e.printStackTrace();
            }
            return L;
        }

        public static void book(String instr, String user, LocalDateTime when) throws SQLException {
            String params = String.format("instructor=%s, user=%s, time=%s", instr, user, when);

            try {
                if (instr == null || user == null || when == null) {
                    Logger.logError("LESSON_BOOK", user, "Null parameters", params);
                    throw new SQLException("Instructor, user, and time cannot be null");
                }

                // перевірка для інструкторів - вони можуть викладати лише один урок за раз!!!!!!!
                try (PreparedStatement p = conn.prepareStatement(
                        "SELECT COUNT(*) FROM lessons WHERE instructor=? AND time=?")) {
                    p.setString(1, instr);
                    p.setString(2, when.format(F));
                    ResultSet rs = p.executeQuery();
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        Logger.logError("LESSON_BOOK", user, "Slot already taken", params);
                        throw new SQLException("Instructor is not available at this time");
                    }
                }

                try (PreparedStatement insertStmt = conn.prepareStatement(
                        "INSERT INTO lessons(username,instructor,time) VALUES(?,?,?)")) {
                    insertStmt.setString(1, user);
                    insertStmt.setString(2, instr);
                    insertStmt.setString(3, when.format(F));
                    insertStmt.executeUpdate();
                }

                TransactionService.log(user, "lesson", 30.0, when);
                Logger.logUserAction("LESSON_BOOK", user, params);

            } catch (SQLException e) {
                Logger.logError("LESSON_BOOK", user, e.getMessage(), params);
                throw e;
            }
        }

        public static ObservableList<Lesson> listAll() {
            ObservableList<Lesson> L = FXCollections.observableArrayList();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM lessons")) {
                while (rs.next()) {
                    L.add(new Lesson(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("instructor"),
                            LocalDateTime.parse(rs.getString("time"), F)));
                }
            } catch (Exception e) {
                Logger.logError("LESSON_LIST", "SYSTEM", e.getMessage(), "Loading all lessons");
                e.printStackTrace();
            }
            return L;
        }

        public static int countAll() {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM lessons")) {
                rs.next();
                return rs.getInt(1);
            } catch (Exception e) {
                Logger.logError("LESSON_COUNT", "SYSTEM", e.getMessage(), "Counting all lessons");
                return 0;
            }
        }

        public static int countByUser(String user) {
            if (user == null)
                return 0;

            try (PreparedStatement p = conn.prepareStatement(
                    "SELECT COUNT(*) FROM lessons WHERE username=?")) {
                p.setString(1, user);
                ResultSet rs = p.executeQuery();
                if (rs.next())
                    return rs.getInt(1);
            } catch (Exception e) {
                Logger.logError("LESSON_COUNT_USER", user, e.getMessage(), String.format("user=%s", user));
                e.printStackTrace();
            }
            return 0;
        }
    }

    public static class TransactionService {
        private static final DateTimeFormatter F = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        public static void log(String user, String type, double amount, LocalDateTime when) {
            String params = String.format("type=%s, amount=%.2f, time=%s", type, amount, when);

            if (user == null || type == null || when == null) {
                Logger.logError("TRANSACTION_LOG", user, "Null parameters", params);
                System.err.println("Cannot log transaction: null parameters");
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO trans(username,type,amount,time) VALUES(?,?,?,?)")) {
                stmt.setString(1, user);
                stmt.setString(2, type);
                stmt.setDouble(3, amount);
                stmt.setString(4, when.format(F));
                stmt.executeUpdate();

                Logger.logUserAction("TRANSACTION_LOG", user, params);
            } catch (Exception e) {
                Logger.logError("TRANSACTION_LOG", user, e.getMessage(), params);
                e.printStackTrace();
            }
        }

        public static ObservableList<Transaction> listAll() {
            ObservableList<Transaction> L = FXCollections.observableArrayList();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM trans")) {
                while (rs.next()) {
                    L.add(new Transaction(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("type"),
                            rs.getDouble("amount"),
                            LocalDateTime.parse(rs.getString("time"), F)));
                }
            } catch (Exception e) {
                Logger.logError("TRANSACTION_LIST", "SYSTEM", e.getMessage(), "Loading all transactions");
                e.printStackTrace();
            }
            return L;
        }
    }
}
package org.example;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

//Логгери з позначенням основних дій
public class Logger {
    private static final String LOG_FILE = "ski-service-logs.txt";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void logUserAction(String action, String user, String parameters) {
        String logMessage = String.format("[%s] ACTION: %s | USER: %s | PARAMS: %s",
                getCurrentTimestamp(), action, user != null ? user : "SYSTEM", parameters);
        writeToFile(logMessage);
    }

    public static void logError(String action, String user, String error, String parameters) {
        String logMessage = String.format("[%s] ERROR: %s | USER: %s | ERROR: %s | PARAMS: %s",
                getCurrentTimestamp(), action, user != null ? user : "SYSTEM", error, parameters);
        writeToFile(logMessage);
    }

    public static void logSystemEvent(String event, String details) {
        String logMessage = String.format("[%s] SYSTEM: %s | DETAILS: %s",
                getCurrentTimestamp(), event, details);
        writeToFile(logMessage);
    }

    private static String getCurrentTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }

    private static synchronized void writeToFile(String message) {
        try (FileWriter writer = new FileWriter(LOG_FILE, true)) {
            writer.write(message + System.lineSeparator());
            writer.flush();
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
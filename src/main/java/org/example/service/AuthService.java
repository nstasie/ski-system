package org.example.service;

import org.example.model.*;

import java.util.regex.Pattern;

public class AuthService {

    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 20;
    private static final int MIN_PASSWORD_LENGTH = 3;
    private static final int MAX_PASSWORD_LENGTH = 50;

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");


    public AuthValidationResult validateLoginCredentials(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return new AuthValidationResult(false, "Username cannot be empty");
        }

        if (password == null || password.trim().isEmpty()) {
            return new AuthValidationResult(false, "Password cannot be empty");
        }

        username = username.trim();
        password = password.trim();

        if (username.length() < MIN_USERNAME_LENGTH) {
            return new AuthValidationResult(false,
                    String.format("Username must be at least %d characters long", MIN_USERNAME_LENGTH));
        }

        if (username.length() > MAX_USERNAME_LENGTH) {
            return new AuthValidationResult(false,
                    String.format("Username cannot be longer than %d characters", MAX_USERNAME_LENGTH));
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            return new AuthValidationResult(false,
                    String.format("Password must be at least %d characters long", MIN_PASSWORD_LENGTH));
        }

        return new AuthValidationResult(true, "Valid login credentials");
    }

    public AuthValidationResult validateRegistrationCredentials(String username, String password) {
        AuthValidationResult basicValidation = validateLoginCredentials(username, password);
        if (!basicValidation.isValid()) {
            return basicValidation;
        }

        username = username.trim();
        password = password.trim();

        // Додаткова перевірка імені користувача
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return new AuthValidationResult(false,
                    "Username can only contain letters, numbers, hyphens, and underscores");
        }

        // Перевірка зарезервованих імен користувачів
        if (isReservedUsername(username)) {
            return new AuthValidationResult(false, "Username is reserved and cannot be used");
        }

        // Додаткова перевірка пароля
        if (password.length() > MAX_PASSWORD_LENGTH) {
            return new AuthValidationResult(false,
                    String.format("Password cannot be longer than %d characters", MAX_PASSWORD_LENGTH));
        }

        if (isWeakPassword(password)) {
            return new AuthValidationResult(false,
                    "Password is too weak. Avoid common passwords and consider using a mix of characters");
        }

        return new AuthValidationResult(true, "Valid registration credentials");
    }

    private boolean isReservedUsername(String username) {
        if (username == null)
            return false;

        String[] reserved = {
                "admin", "root", "system", "null", "undefined",
                "test", "guest", "public", "private", "api",
                "www", "mail", "ftp", "localhost", "server"
        };

        String lowerUsername = username.toLowerCase();
        for (String reservedName : reserved) {
            if (lowerUsername.equals(reservedName)) {
                return true;
            }
        }

        return false;
    }

    private boolean isWeakPassword(String password) {
        if (password == null)
            return true;

        String[] commonPasswords = {
                "password", "123456", "qwerty", "abc123",
                "password123", "admin", "user", "guest",
                "12345678", "111111", "000000"
        };

        String lowerPassword = password.toLowerCase();
        for (String commonPass : commonPasswords) {
            if (lowerPassword.equals(commonPass)) {
                return true;
            }
        }

        return false;
    }

    public boolean isPasswordSameAsUsername(String username, String password) {
        if (username == null || password == null)
            return false;
        return username.trim().equalsIgnoreCase(password.trim());
    }


    public AuthValidationResult validateUserRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return new AuthValidationResult(false, "Role cannot be empty");
        }

        String[] validRoles = { "USER", "ADMIN" };
        String upperRole = role.trim().toUpperCase();

        for (String validRole : validRoles) {
            if (validRole.equals(upperRole)) {
                return new AuthValidationResult(true, "Valid role");
            }
        }

        return new AuthValidationResult(false, "Invalid role. Must be USER or ADMIN");
    }


    public String sanitizeUsername(String username) {
        if (username == null)
            return null;

        // Trim whitespace and convert to lowercase for consistency
        String sanitized = username.trim();

        // Remove any potentially dangerous characters not caught by regex
        sanitized = sanitized.replaceAll("[<>\"'&]", "");

        return sanitized;
    }


    public boolean hasPermission(String userRole, String requiredPermission) {
        if (userRole == null || requiredPermission == null)
            return false;

        switch (requiredPermission.toUpperCase()) {
            case "VIEW_OWN_DATA":
                return "USER".equals(userRole) || "ADMIN".equals(userRole);
            case "VIEW_ALL_DATA":
                return "ADMIN".equals(userRole);
            case "MANAGE_BOOKINGS":
                return "USER".equals(userRole) || "ADMIN".equals(userRole);
            case "MANAGE_EQUIPMENT":
                return "USER".equals(userRole) || "ADMIN".equals(userRole);
            case "MANAGE_LESSONS":
                return "USER".equals(userRole) || "ADMIN".equals(userRole);
            case "VIEW_FINANCES":
                return "ADMIN".equals(userRole);
            case "VIEW_ANALYTICS":
                return "ADMIN".equals(userRole);
            case "ADMIN_FUNCTIONS":
                return "ADMIN".equals(userRole);
            default:
                return false;
        }
    }


    public UserCapabilities getUserCapabilities(String userRole) {
        if (userRole == null) {
            return new UserCapabilities(false, false, false, false, false, false);
        }

        switch (userRole.toUpperCase()) {
            case "ADMIN":
                return new UserCapabilities(true, true, true, true, true, true);
            case "USER":
                return new UserCapabilities(true, true, true, false, false, false);
            default:
                return new UserCapabilities(false, false, false, false, false, false);
        }
    }


    public String createDisplayName(String username, String role) {
        if (username == null)
            return "Unknown User";

        String sanitized = sanitizeUsername(username);
        if (sanitized == null || sanitized.isEmpty())
            return "Unknown User";

        // Capitalize first letter for display
        String displayName = sanitized.substring(0, 1).toUpperCase() +
                (sanitized.length() > 1 ? sanitized.substring(1) : "");

        if ("ADMIN".equals(role)) {
            displayName += " (Admin)";
        }

        return displayName;
    }


    public SessionInfo createSessionInfo(String username, String role) {
        if (username == null || role == null) {
            return new SessionInfo("", "", false, null);
        }

        String displayName = createDisplayName(username, role);
        UserCapabilities capabilities = getUserCapabilities(role);
        boolean isValid = validateUserRole(role).isValid();

        return new SessionInfo(username, displayName, isValid, capabilities);
    }


    public SecurityAssessment assessCredentialSecurity(String username, String password) {
        if (username == null || password == null) {
            return new SecurityAssessment(SecurityLevel.VERY_WEAK,
                    "Missing credentials", new String[] { "Provide both username and password" });
        }

        AuthValidationResult validation = validateRegistrationCredentials(username, password);
        if (!validation.isValid()) {
            return new SecurityAssessment(SecurityLevel.VERY_WEAK,
                    validation.getMessage(), new String[] { "Fix validation errors" });
        }

        if (isPasswordSameAsUsername(username, password)) {
            return new SecurityAssessment(SecurityLevel.VERY_WEAK,
                    "Password same as username", new String[] { "Use a different password" });
        }

        if (isWeakPassword(password)) {
            return new SecurityAssessment(SecurityLevel.WEAK,
                    "Password is common", new String[] { "Use a more unique password" });
        }

        if (password.length() < 6) {
            return new SecurityAssessment(SecurityLevel.WEAK,
                    "Short password", new String[] { "Consider using a longer password" });
        }

        if (password.length() >= 8 && hasVariedCharacters(password)) {
            return new SecurityAssessment(SecurityLevel.STRONG,
                    "Good credential security", new String[] {});
        }

        return new SecurityAssessment(SecurityLevel.MODERATE,
                "Acceptable security", new String[] { "Consider adding varied characters" });
    }

    private boolean hasVariedCharacters(String password) {
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(c -> !Character.isLetterOrDigit(c));

        int varietyCount = 0;
        if (hasLower)
            varietyCount++;
        if (hasUpper)
            varietyCount++;
        if (hasDigit)
            varietyCount++;
        if (hasSpecial)
            varietyCount++;

        return varietyCount >= 3;
    }


    public static class AuthValidationResult {
        private final boolean valid;
        private final String message;

        public AuthValidationResult(boolean valid, String message) {
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

    public static class UserCapabilities {
        private final boolean canManageOwnData;
        private final boolean canBookServices;
        private final boolean canRentEquipment;
        private final boolean canViewAllData;
        private final boolean canViewFinances;
        private final boolean canViewAnalytics;

        public UserCapabilities(boolean canManageOwnData, boolean canBookServices,
                boolean canRentEquipment, boolean canViewAllData,
                boolean canViewFinances, boolean canViewAnalytics) {
            this.canManageOwnData = canManageOwnData;
            this.canBookServices = canBookServices;
            this.canRentEquipment = canRentEquipment;
            this.canViewAllData = canViewAllData;
            this.canViewFinances = canViewFinances;
            this.canViewAnalytics = canViewAnalytics;
        }

        public boolean canManageOwnData() {
            return canManageOwnData;
        }

        public boolean canBookServices() {
            return canBookServices;
        }

        public boolean canRentEquipment() {
            return canRentEquipment;
        }

        public boolean canViewAllData() {
            return canViewAllData;
        }

        public boolean canViewFinances() {
            return canViewFinances;
        }

        public boolean canViewAnalytics() {
            return canViewAnalytics;
        }
    }

    public static class SessionInfo {
        private final String username;
        private final String displayName;
        private final boolean valid;
        private final UserCapabilities capabilities;

        public SessionInfo(String username, String displayName, boolean valid, UserCapabilities capabilities) {
            this.username = username;
            this.displayName = displayName;
            this.valid = valid;
            this.capabilities = capabilities;
        }

        public String getUsername() {
            return username;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isValid() {
            return valid;
        }

        public UserCapabilities getCapabilities() {
            return capabilities;
        }
    }

    public enum SecurityLevel {
        VERY_WEAK, WEAK, MODERATE, STRONG
    }

    public static class SecurityAssessment {
        private final SecurityLevel level;
        private final String message;
        private final String[] recommendations;

        public SecurityAssessment(SecurityLevel level, String message, String[] recommendations) {
            this.level = level;
            this.message = message;
            this.recommendations = recommendations;
        }

        public SecurityLevel getLevel() {
            return level;
        }

        public String getMessage() {
            return message;
        }

        public String[] getRecommendations() {
            return recommendations;
        }
    }
}
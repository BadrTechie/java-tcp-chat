package io.github.badrtechie.tcpchat.server.business;

final class Inputs {
    static final int MAX_USERNAME_LENGTH = 40;
    static final int MAX_MESSAGE_LENGTH = 4_000;

    private Inputs() {
    }

    static String requireHost(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Address is required.");
        }
        String result = value.trim();
        if (result.length() > 255 || result.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Address must be a host name or IP address without spaces.");
        }
        return result;
    }

    static int parsePort(String value) {
        try {
            return requirePort(Integer.parseInt(value == null ? "" : value.trim()));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Port must be a number from 1 to 65535.", ex);
        }
    }

    static int parseBindPort(String value) {
        try {
            return requireBindPort(Integer.parseInt(value == null ? "" : value.trim()));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Bind port must be a number from 0 to 65535.", ex);
        }
    }

    static int requirePort(int value) {
        if (value < 1 || value > 65_535) {
            throw new IllegalArgumentException("Port must be from 1 to 65535.");
        }
        return value;
    }

    static int requireBindPort(int value) {
        if (value < 0 || value > 65_535) {
            throw new IllegalArgumentException("Bind port must be from 0 to 65535.");
        }
        return value;
    }

    static String username(String value) {
        String result = value == null ? "" : value.trim();
        if (result.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException("Username must be at most " + MAX_USERNAME_LENGTH + " characters.");
        }
        if (result.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Username cannot contain control characters.");
        }
        return result;
    }

    static String message(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Message cannot be blank.");
        }
        if (value.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Message must be at most " + MAX_MESSAGE_LENGTH + " characters.");
        }
        return value;
    }
}

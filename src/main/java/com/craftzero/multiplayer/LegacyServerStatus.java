package com.craftzero.multiplayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Release-era server-list status text carried inside the old 0xFF kick packet.
 */
public record LegacyServerStatus(
        String motd,
        int onlinePlayers,
        int maxPlayers,
        int protocolVersion,
        String versionName
) {
    public static final int RELEASE_ONE_PROTOCOL_VERSION = 22;
    public static final String RELEASE_ONE_VERSION_NAME = "1.0.0";
    private static final char SECTION = '\u00A7';
    private static final char NULL = '\0';
    private static final int MAX_TEXT_CHARS = 256;

    public LegacyServerStatus {
        motd = sanitizeStatusText(motd, "CraftZero", false);
        onlinePlayers = Math.max(0, onlinePlayers);
        maxPlayers = Math.max(1, maxPlayers);
        protocolVersion = Math.max(0, protocolVersion);
        versionName = sanitizeStatusText(versionName, RELEASE_ONE_VERSION_NAME, false);
    }

    public static LegacyServerStatus of(String motd, int onlinePlayers, int maxPlayers) {
        return new LegacyServerStatus(motd, onlinePlayers, maxPlayers,
                RELEASE_ONE_PROTOCOL_VERSION, RELEASE_ONE_VERSION_NAME);
    }

    public static LegacyServerStatus parse(String response) {
        String text = response == null ? "" : response.trim();
        if (text.isEmpty()) {
            return of("CraftZero", 0, MultiplayerProtocol.DEFAULT_MAX_PLAYERS);
        }
        if (isExtendedStatus(text)) {
            return parseExtended(text);
        }
        return parseReleaseOne(text);
    }

    public String toReleaseOneStatusText() {
        return sanitizeStatusText(motd, "CraftZero", false)
                + SECTION + Math.max(0, onlinePlayers)
                + SECTION + Math.max(1, maxPlayers);
    }

    public String toExtendedStatusText() {
        return new StringBuilder()
                .append(SECTION).append('1').append(NULL)
                .append(Math.max(0, protocolVersion)).append(NULL)
                .append(sanitizeStatusText(versionName, RELEASE_ONE_VERSION_NAME, false)).append(NULL)
                .append(sanitizeStatusText(motd, "CraftZero", false)).append(NULL)
                .append(Math.max(0, onlinePlayers)).append(NULL)
                .append(Math.max(1, maxPlayers))
                .toString();
    }

    public static String sanitizeKickText(String value, String fallback) {
        return sanitizeStatusText(value, fallback == null || fallback.isBlank() ? "CraftZero" : fallback, true);
    }

    private static boolean isExtendedStatus(String text) {
        return text.length() >= 2 && text.charAt(0) == SECTION && text.charAt(1) == '1'
                && text.indexOf(NULL) >= 0;
    }

    private static LegacyServerStatus parseExtended(String text) {
        List<String> parts = split(text, NULL);
        if (parts.size() >= 6) {
            return new LegacyServerStatus(
                    parts.get(3),
                    parseNumber(parts.get(4), 0),
                    parseNumber(parts.get(5), MultiplayerProtocol.DEFAULT_MAX_PLAYERS),
                    parseNumber(parts.get(1), RELEASE_ONE_PROTOCOL_VERSION),
                    parts.get(2));
        }
        return parseReleaseOne(text.replace(NULL, SECTION));
    }

    private static LegacyServerStatus parseReleaseOne(String text) {
        List<String> parts = split(text, SECTION);
        if (parts.size() >= 3) {
            int last = parts.size() - 1;
            String motd = join(parts.subList(0, last - 1), SECTION);
            return of(motd,
                    parseNumber(parts.get(last - 1), 0),
                    parseNumber(parts.get(last), MultiplayerProtocol.DEFAULT_MAX_PLAYERS));
        }
        return of(text, 0, MultiplayerProtocol.DEFAULT_MAX_PLAYERS);
    }

    private static List<String> split(String text, char delimiter) {
        ArrayList<String> parts = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == delimiter) {
                parts.add(text.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(text.substring(start));
        return parts;
    }

    private static String join(List<String> parts, char delimiter) {
        if (parts == null || parts.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (builder.length() > 0) {
                builder.append(delimiter);
            }
            builder.append(part == null ? "" : part);
        }
        return builder.toString();
    }

    private static int parseNumber(String value, int fallback) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static String sanitizeStatusText(String value, String fallback, boolean allowSection) {
        String raw = value == null || value.isBlank() ? fallback : value;
        if (raw == null || raw.isBlank()) {
            raw = "CraftZero";
        }
        StringBuilder builder = new StringBuilder(Math.min(raw.length(), MAX_TEXT_CHARS));
        for (int i = 0; i < raw.length() && builder.length() < MAX_TEXT_CHARS; i++) {
            char c = raw.charAt(i);
            if (c == '\r' || c == '\n' || c == NULL || (!allowSection && c == SECTION)) {
                builder.append(' ');
            } else if (c >= 0x20 || c == SECTION) {
                builder.append(c);
            }
        }
        String text = builder.toString().trim();
        return text.isEmpty() ? "CraftZero" : text;
    }
}

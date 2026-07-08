package com.craftzero.multiplayer;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Map;

public record NetworkMessage(String type, JsonObject data) {
    private static final Gson GSON = new Gson();

    public NetworkMessage {
        type = normalizeType(type);
        data = sanitizeData(data);
    }

    public static NetworkMessage of(String type) {
        return new NetworkMessage(type, new JsonObject());
    }

    public static NetworkMessage of(String type, JsonObject data) {
        return new NetworkMessage(type, data == null ? new JsonObject() : data);
    }

    public String toLine() {
        return GSON.toJson(this);
    }

    public static NetworkMessage fromLine(String line) {
        if (line == null || line.isBlank() || line.length() > MultiplayerProtocol.MAX_PROTOCOL_LINE_LENGTH) {
            return null;
        }
        try {
            JsonElement root = JsonParser.parseString(line);
            if (root == null || !root.isJsonObject()) {
                return null;
            }
            JsonObject object = root.getAsJsonObject();
            JsonElement typeElement = object.get("type");
            if (typeElement == null || !typeElement.isJsonPrimitive()) {
                return null;
            }
            JsonElement dataElement = object.get("data");
            JsonObject payload = dataElement != null && dataElement.isJsonObject()
                    ? dataElement.getAsJsonObject()
                    : new JsonObject();
            NetworkMessage message = new NetworkMessage(typeElement.getAsString(), payload);
            return message.type().isEmpty() ? null : message;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static JsonObject object() {
        return new JsonObject();
    }

    private static String normalizeType(String type) {
        String normalized = type == null ? "" : type.trim();
        return MultiplayerProtocol.isValidLegacyMessageType(normalized) ? normalized : "";
    }

    private static JsonObject sanitizeData(JsonObject source) {
        if (source == null || source.isJsonNull()) {
            return new JsonObject();
        }
        JsonObject result = new JsonObject();
        int entries = 0;
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            if (entries >= MultiplayerProtocol.MAX_LEGACY_MESSAGE_DATA_ENTRIES) {
                break;
            }
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (!MultiplayerProtocol.isValidLegacyDataKey(key) || value == null || value.isJsonNull()) {
                continue;
            }
            String encoded = value.toString();
            if (!MultiplayerProtocol.isValidLegacyDataValue(encoded)) {
                continue;
            }
            result.add(key, value.deepCopy());
            entries++;
        }
        return result;
    }
}

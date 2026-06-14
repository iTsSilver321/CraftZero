package com.craftzero.multiplayer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public record NetworkMessage(String type, JsonObject data) {
    private static final Gson GSON = new Gson();

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
        return GSON.fromJson(line, NetworkMessage.class);
    }

    public static JsonObject object() {
        return new JsonObject();
    }
}

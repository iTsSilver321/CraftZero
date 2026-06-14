package com.craftzero.multiplayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProtocolCodec {
    private static final String TYPE = "type";

    private ProtocolCodec() {
    }

    public static String encode(ProtocolMessage message) {
        return Json.stringify(toMap(message));
    }

    public static ProtocolMessage decode(String line) {
        Map<String, Object> object = asObject(Json.parse(line));
        String type = string(object, TYPE, "");
        return switch (type) {
            case ProtocolMessage.Type.HELLO -> new ProtocolMessage.Hello(
                    integer(object, "protocolVersion", 0),
                    string(object, "playerId", ""),
                    string(object, "serverName", "CraftZero")
            );
            case ProtocolMessage.Type.WORLD_STATE -> new ProtocolMessage.WorldState(
                    longNumber(object, "seed", 0L),
                    decimal(object, "timeOfDay", 0.0),
                    playerStates(object.get("players"))
            );
            case ProtocolMessage.Type.JOIN -> new ProtocolMessage.Join(
                    string(object, "username", "Player")
            );
            case ProtocolMessage.Type.CLIENT_INPUT -> new ProtocolMessage.ClientInput(
                    string(object, "playerId", ""),
                    pose(object.get("pose")),
                    bool(object, "forward", false),
                    bool(object, "backward", false),
                    bool(object, "left", false),
                    bool(object, "right", false),
                    bool(object, "jumping", false),
                    bool(object, "sneaking", false)
            );
            case ProtocolMessage.Type.CLIENT_ACTION -> new ProtocolMessage.ClientAction(
                    string(object, "playerId", ""),
                    string(object, "action", "action"),
                    blockUpdateOrNull(object.get("blockUpdate")),
                    stringMap(object.get("data"))
            );
            case ProtocolMessage.Type.PLAYER_STATE -> playerState(object);
            case ProtocolMessage.Type.BLOCK_UPDATE -> blockUpdate(object);
            case ProtocolMessage.Type.ENTITY_UPDATE -> new ProtocolMessage.EntityUpdate(
                    string(object, "entityId", ""),
                    string(object, "entityType", "entity"),
                    pose(object.get("pose")),
                    stringMap(object.get("data"))
            );
            case ProtocolMessage.Type.INVENTORY_UPDATE -> new ProtocolMessage.InventoryUpdate(
                    string(object, "playerId", ""),
                    integer(object, "slot", 0),
                    string(object, "itemId", "air"),
                    integer(object, "count", 0),
                    integer(object, "damage", 0)
            );
            case ProtocolMessage.Type.CHAT -> new ProtocolMessage.Chat(
                    string(object, "playerId", ""),
                    string(object, "sender", "Player"),
                    string(object, "text", "")
            );
            case ProtocolMessage.Type.DISCONNECT -> new ProtocolMessage.Disconnect(
                    string(object, "playerId", ""),
                    string(object, "reason", "disconnect")
            );
            default -> throw new IllegalArgumentException("Unknown protocol message type: " + type);
        };
    }

    static Map<String, Object> toMap(ProtocolMessage message) {
        if (message instanceof ProtocolMessage.Hello hello) {
            Map<String, Object> object = typed(hello);
            object.put("protocolVersion", hello.protocolVersion());
            object.put("playerId", hello.playerId());
            object.put("serverName", hello.serverName());
            return object;
        }
        if (message instanceof ProtocolMessage.WorldState worldState) {
            Map<String, Object> object = typed(worldState);
            object.put("seed", worldState.seed());
            object.put("timeOfDay", worldState.timeOfDay());
            object.put("players", worldState.players().stream().map(ProtocolCodec::playerStateToMap).toList());
            return object;
        }
        if (message instanceof ProtocolMessage.Join join) {
            Map<String, Object> object = typed(join);
            object.put("username", join.username());
            return object;
        }
        if (message instanceof ProtocolMessage.ClientInput input) {
            Map<String, Object> object = typed(input);
            object.put("playerId", input.playerId());
            object.put("pose", poseToMap(input.pose()));
            object.put("forward", input.forward());
            object.put("backward", input.backward());
            object.put("left", input.left());
            object.put("right", input.right());
            object.put("jumping", input.jumping());
            object.put("sneaking", input.sneaking());
            return object;
        }
        if (message instanceof ProtocolMessage.ClientAction action) {
            Map<String, Object> object = typed(action);
            object.put("playerId", action.playerId());
            object.put("action", action.action());
            object.put("blockUpdate", action.blockUpdate() == null ? null : blockUpdateToMap(action.blockUpdate()));
            object.put("data", new LinkedHashMap<>(action.data()));
            return object;
        }
        if (message instanceof ProtocolMessage.PlayerState playerState) {
            return playerStateToMap(playerState);
        }
        if (message instanceof ProtocolMessage.BlockUpdate blockUpdate) {
            return blockUpdateToMap(blockUpdate);
        }
        if (message instanceof ProtocolMessage.EntityUpdate entityUpdate) {
            Map<String, Object> object = typed(entityUpdate);
            object.put("entityId", entityUpdate.entityId());
            object.put("entityType", entityUpdate.entityType());
            object.put("pose", poseToMap(entityUpdate.pose()));
            object.put("data", new LinkedHashMap<>(entityUpdate.data()));
            return object;
        }
        if (message instanceof ProtocolMessage.InventoryUpdate inventoryUpdate) {
            Map<String, Object> object = typed(inventoryUpdate);
            object.put("playerId", inventoryUpdate.playerId());
            object.put("slot", inventoryUpdate.slot());
            object.put("itemId", inventoryUpdate.itemId());
            object.put("count", inventoryUpdate.count());
            object.put("damage", inventoryUpdate.damage());
            return object;
        }
        if (message instanceof ProtocolMessage.Chat chat) {
            Map<String, Object> object = typed(chat);
            object.put("playerId", chat.playerId());
            object.put("sender", chat.sender());
            object.put("text", chat.text());
            return object;
        }
        if (message instanceof ProtocolMessage.Disconnect disconnect) {
            Map<String, Object> object = typed(disconnect);
            object.put("playerId", disconnect.playerId());
            object.put("reason", disconnect.reason());
            return object;
        }
        throw new IllegalArgumentException("Unsupported protocol message: " + message);
    }

    private static Map<String, Object> playerStateToMap(ProtocolMessage.PlayerState playerState) {
        Map<String, Object> object = typed(playerState);
        object.put("playerId", playerState.playerId());
        object.put("username", playerState.username());
        object.put("pose", poseToMap(playerState.pose()));
        object.put("onGround", playerState.onGround());
        return object;
    }

    private static Map<String, Object> blockUpdateToMap(ProtocolMessage.BlockUpdate blockUpdate) {
        Map<String, Object> object = typed(blockUpdate);
        object.put("x", blockUpdate.x());
        object.put("y", blockUpdate.y());
        object.put("z", blockUpdate.z());
        object.put("blockId", blockUpdate.blockId());
        object.put("metadata", blockUpdate.metadata());
        object.put("sourcePlayerId", blockUpdate.sourcePlayerId());
        return object;
    }

    private static Map<String, Object> poseToMap(ProtocolMessage.PlayerPose pose) {
        Map<String, Object> object = new LinkedHashMap<>();
        object.put("x", pose.x());
        object.put("y", pose.y());
        object.put("z", pose.z());
        object.put("yaw", pose.yaw());
        object.put("pitch", pose.pitch());
        return object;
    }

    private static Map<String, Object> typed(ProtocolMessage message) {
        Map<String, Object> object = new LinkedHashMap<>();
        object.put(TYPE, message.type());
        return object;
    }

    private static List<ProtocolMessage.PlayerState> playerStates(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        ArrayList<ProtocolMessage.PlayerState> players = new ArrayList<>();
        for (Object item : list) {
            players.add(playerState(asObject(item)));
        }
        return players;
    }

    private static ProtocolMessage.PlayerState playerState(Map<String, Object> object) {
        return new ProtocolMessage.PlayerState(
                string(object, "playerId", ""),
                string(object, "username", "Player"),
                pose(object.get("pose")),
                bool(object, "onGround", false)
        );
    }

    private static ProtocolMessage.BlockUpdate blockUpdateOrNull(Object value) {
        if (value == null) {
            return null;
        }
        return blockUpdate(asObject(value));
    }

    private static ProtocolMessage.BlockUpdate blockUpdate(Map<String, Object> object) {
        return new ProtocolMessage.BlockUpdate(
                integer(object, "x", 0),
                integer(object, "y", 0),
                integer(object, "z", 0),
                string(object, "blockId", "air"),
                integer(object, "metadata", 0),
                string(object, "sourcePlayerId", "")
        );
    }

    private static ProtocolMessage.PlayerPose pose(Object value) {
        if (!(value instanceof Map<?, ?>)) {
            return ProtocolMessage.PlayerPose.origin();
        }
        Map<String, Object> object = asObject(value);
        return new ProtocolMessage.PlayerPose(
                decimal(object, "x", 0.0),
                decimal(object, "y", 0.0),
                decimal(object, "z", 0.0),
                (float) decimal(object, "yaw", 0.0),
                (float) decimal(object, "pitch", 0.0)
        );
    }

    private static Map<String, String> stringMap(Object value) {
        if (!(value instanceof Map<?, ?> raw)) {
            return Map.of();
        }
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return result;
    }

    private static Map<String, Object> asObject(Object value) {
        if (!(value instanceof Map<?, ?> raw)) {
            throw new IllegalArgumentException("Expected JSON object");
        }
        LinkedHashMap<String, Object> object = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            object.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return object;
    }

    private static String string(Map<String, Object> object, String key, String fallback) {
        Object value = object.get(key);
        if (value == null) {
            return fallback;
        }
        return String.valueOf(value);
    }

    private static boolean bool(Map<String, Object> object, String key, boolean fallback) {
        Object value = object.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String string) {
            return Boolean.parseBoolean(string);
        }
        return fallback;
    }

    private static int integer(Map<String, Object> object, String key, int fallback) {
        return (int) longNumber(object, key, fallback);
    }

    private static long longNumber(Map<String, Object> object, String key, long fallback) {
        Object value = object.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static double decimal(Map<String, Object> object, String key, double fallback) {
        Object value = object.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String string) {
            try {
                return Double.parseDouble(string);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}

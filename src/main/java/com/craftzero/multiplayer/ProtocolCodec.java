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
        if (line == null || line.length() > MultiplayerProtocol.MAX_PROTOCOL_LINE_LENGTH) {
            throw new IllegalArgumentException("Protocol line too long");
        }
        Map<String, Object> object = asObject(Json.parse(line));
        String type = string(object, TYPE, "");
        return switch (type) {
            case ProtocolMessage.Type.HELLO -> new ProtocolMessage.Hello(
                    integer(object, "protocolVersion", 0),
                    string(object, "playerId", ""),
                    string(object, "serverName", "CraftZero")
            );
            case ProtocolMessage.Type.KEEP_ALIVE -> new ProtocolMessage.KeepAlive(
                    integer(object, "id", 0)
            );
            case ProtocolMessage.Type.WORLD_STATE -> new ProtocolMessage.WorldState(
                    longNumber(object, "seed", 0L),
                    decimal(object, "timeOfDay", 0.0),
                    string(object, "weatherState", "clear"),
                    integer(object, "spawnX", 0),
                    integer(object, "spawnY", 80),
                    integer(object, "spawnZ", 0),
                    string(object, "gameMode", "SURVIVAL"),
                    string(object, "difficulty", "EASY"),
                    bool(object, "hardcore", false),
                    bool(object, "allowCheats", false),
                    bool(object, "pvp", true),
                    bool(object, "spawnAnimals", true),
                    bool(object, "spawnMonsters", true),
                    bool(object, "spawnNpcs", true),
                    bool(object, "allowNether", true),
                    bool(object, "allowFlight", false),
                    string(object, "dimension", "overworld"),
                    integer(object, "maxPlayers", MultiplayerProtocol.DEFAULT_MAX_PLAYERS),
                    integer(object, "viewDistance", MultiplayerProtocol.DEFAULT_VIEW_DISTANCE),
                    integer(object, "maxBuildHeight", MultiplayerProtocol.DEFAULT_MAX_BUILD_HEIGHT),
                    bool(object, "generateStructures", MultiplayerProtocol.DEFAULT_GENERATE_STRUCTURES),
                    playerStates(object.get("players")),
                    blockUpdates(object.get("blockUpdates")),
                    entityUpdates(object.get("entityUpdates")),
                    inventoryUpdates(object.get("inventoryUpdates"))
            );
            case ProtocolMessage.Type.JOIN -> new ProtocolMessage.Join(
                    string(object, "username", "Player"),
                    integer(object, "protocolVersion", 0)
            );
            case ProtocolMessage.Type.PLAYER_LIST -> new ProtocolMessage.PlayerList(
                    playerListEntries(object.get("players"))
            );
            case ProtocolMessage.Type.CLIENT_INPUT -> new ProtocolMessage.ClientInput(
                    string(object, "playerId", ""),
                    pose(object.get("pose")),
                    bool(object, "forward", false),
                    bool(object, "backward", false),
                    bool(object, "left", false),
                    bool(object, "right", false),
                    bool(object, "jumping", false),
                    bool(object, "sneaking", false),
                    bool(object, "onGround", !bool(object, "jumping", false)),
                    (float) decimal(object, "health", 20.0),
                    string(object, "heldItemId", "air"),
                    integer(object, "heldItemCount", 0),
                    integer(object, "heldItemDamage", 0),
                    integer(object, "selectedSlot", 0),
                    string(object, "gameMode", "SURVIVAL"),
                    stringMap(object.get("data"))
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
                    integer(object, "damage", 0),
                    stringMap(object.get("data"))
            );
            case ProtocolMessage.Type.WORLD_EVENT -> new ProtocolMessage.WorldEvent(
                    string(object, "eventType", "event"),
                    stringMap(object.get("data"))
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
        if (message instanceof ProtocolMessage.KeepAlive keepAlive) {
            Map<String, Object> object = typed(keepAlive);
            object.put("id", keepAlive.id());
            return object;
        }
        if (message instanceof ProtocolMessage.WorldState worldState) {
            Map<String, Object> object = typed(worldState);
            object.put("seed", worldState.seed());
            object.put("timeOfDay", worldState.timeOfDay());
            object.put("weatherState", worldState.weatherState());
            object.put("spawnX", worldState.spawnX());
            object.put("spawnY", worldState.spawnY());
            object.put("spawnZ", worldState.spawnZ());
            object.put("gameMode", worldState.gameMode());
            object.put("difficulty", worldState.difficulty());
            object.put("hardcore", worldState.hardcore());
            object.put("allowCheats", worldState.allowCheats());
            object.put("pvp", worldState.pvp());
            object.put("spawnAnimals", worldState.spawnAnimals());
            object.put("spawnMonsters", worldState.spawnMonsters());
            object.put("spawnNpcs", worldState.spawnNpcs());
            object.put("allowNether", worldState.allowNether());
            object.put("allowFlight", worldState.allowFlight());
            object.put("dimension", worldState.dimension());
            object.put("maxPlayers", worldState.maxPlayers());
            object.put("viewDistance", worldState.viewDistance());
            object.put("maxBuildHeight", worldState.maxBuildHeight());
            object.put("generateStructures", worldState.generateStructures());
            object.put("players", worldState.players().stream().map(ProtocolCodec::playerStateToMap).toList());
            object.put("blockUpdates", worldState.blockUpdates().stream().map(ProtocolCodec::blockUpdateToMap).toList());
            object.put("entityUpdates", worldState.entityUpdates().stream().map(ProtocolCodec::entityUpdateToMap).toList());
            object.put("inventoryUpdates", worldState.inventoryUpdates().stream()
                    .map(ProtocolCodec::inventoryUpdateToMap).toList());
            return object;
        }
        if (message instanceof ProtocolMessage.Join join) {
            Map<String, Object> object = typed(join);
            object.put("username", join.username());
            object.put("protocolVersion", join.protocolVersion());
            return object;
        }
        if (message instanceof ProtocolMessage.PlayerList playerList) {
            Map<String, Object> object = typed(playerList);
            object.put("players", playerList.players().stream().map(ProtocolCodec::playerListEntryToMap).toList());
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
            object.put("onGround", input.onGround());
            object.put("health", input.health());
            object.put("heldItemId", input.heldItemId());
            object.put("heldItemCount", input.heldItemCount());
            object.put("heldItemDamage", input.heldItemDamage());
            object.put("selectedSlot", input.selectedSlot());
            object.put("gameMode", input.gameMode());
            object.put("data", new LinkedHashMap<>(input.data()));
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
            return entityUpdateToMap(entityUpdate);
        }
        if (message instanceof ProtocolMessage.InventoryUpdate inventoryUpdate) {
            return inventoryUpdateToMap(inventoryUpdate);
        }
        if (message instanceof ProtocolMessage.WorldEvent worldEvent) {
            Map<String, Object> object = typed(worldEvent);
            object.put("eventType", worldEvent.eventType());
            object.put("data", new LinkedHashMap<>(worldEvent.data()));
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
        object.put("sneaking", playerState.sneaking());
        object.put("health", playerState.health());
        object.put("heldItemId", playerState.heldItemId());
        object.put("heldItemCount", playerState.heldItemCount());
        object.put("heldItemDamage", playerState.heldItemDamage());
        object.put("selectedSlot", playerState.selectedSlot());
        object.put("gameMode", playerState.gameMode());
        object.put("data", new LinkedHashMap<>(playerState.data()));
        return object;
    }

    private static Map<String, Object> playerListEntryToMap(ProtocolMessage.PlayerListEntry player) {
        Map<String, Object> object = new LinkedHashMap<>();
        object.put("playerId", player.playerId());
        object.put("username", player.username());
        object.put("latencyMillis", player.latencyMillis());
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
        object.put("data", new LinkedHashMap<>(blockUpdate.data()));
        return object;
    }

    private static Map<String, Object> entityUpdateToMap(ProtocolMessage.EntityUpdate entityUpdate) {
        Map<String, Object> object = typed(entityUpdate);
        object.put("entityId", entityUpdate.entityId());
        object.put("entityType", entityUpdate.entityType());
        object.put("pose", poseToMap(entityUpdate.pose()));
        object.put("data", new LinkedHashMap<>(entityUpdate.data()));
        return object;
    }

    private static Map<String, Object> inventoryUpdateToMap(ProtocolMessage.InventoryUpdate inventoryUpdate) {
        Map<String, Object> object = typed(inventoryUpdate);
        object.put("playerId", inventoryUpdate.playerId());
        object.put("slot", inventoryUpdate.slot());
        object.put("itemId", inventoryUpdate.itemId());
        object.put("count", inventoryUpdate.count());
        object.put("damage", inventoryUpdate.damage());
        object.put("data", new LinkedHashMap<>(inventoryUpdate.data()));
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

    private static List<ProtocolMessage.PlayerListEntry> playerListEntries(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        ArrayList<ProtocolMessage.PlayerListEntry> players = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> object = asObject(item);
            players.add(new ProtocolMessage.PlayerListEntry(
                    string(object, "playerId", ""),
                    string(object, "username", "Player"),
                    integer(object, "latencyMillis", -1)
            ));
        }
        return players;
    }

    private static List<ProtocolMessage.BlockUpdate> blockUpdates(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        ArrayList<ProtocolMessage.BlockUpdate> updates = new ArrayList<>();
        for (Object item : list) {
            updates.add(blockUpdate(asObject(item)));
        }
        return updates;
    }

    private static List<ProtocolMessage.EntityUpdate> entityUpdates(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        ArrayList<ProtocolMessage.EntityUpdate> updates = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> object = asObject(item);
            updates.add(new ProtocolMessage.EntityUpdate(
                    string(object, "entityId", ""),
                    string(object, "entityType", "entity"),
                    pose(object.get("pose")),
                    stringMap(object.get("data"))
            ));
        }
        return updates;
    }

    private static List<ProtocolMessage.InventoryUpdate> inventoryUpdates(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        ArrayList<ProtocolMessage.InventoryUpdate> updates = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> object = asObject(item);
            updates.add(new ProtocolMessage.InventoryUpdate(
                    string(object, "playerId", ""),
                    integer(object, "slot", 0),
                    string(object, "itemId", "air"),
                    integer(object, "count", 0),
                    integer(object, "damage", 0),
                    stringMap(object.get("data"))
            ));
        }
        return updates;
    }

    private static ProtocolMessage.PlayerState playerState(Map<String, Object> object) {
        return new ProtocolMessage.PlayerState(
                string(object, "playerId", ""),
                string(object, "username", "Player"),
                pose(object.get("pose")),
                bool(object, "onGround", false),
                bool(object, "sneaking", false),
                (float) decimal(object, "health", 20.0),
                string(object, "heldItemId", "air"),
                integer(object, "heldItemCount", 0),
                integer(object, "heldItemDamage", 0),
                integer(object, "selectedSlot", 0),
                string(object, "gameMode", "SURVIVAL"),
                stringMap(object.get("data"))
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
                string(object, "sourcePlayerId", ""),
                stringMap(object.get("data"))
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
            if (result.size() >= MultiplayerProtocol.MAX_LEGACY_MESSAGE_DATA_ENTRIES) {
                break;
            }
            String key = String.valueOf(entry.getKey());
            String text = stringMapText(entry.getValue());
            if (MultiplayerProtocol.isValidLegacyDataKey(key)
                    && text != null
                    && text.length() <= MultiplayerProtocol.MAX_ITEM_STACK_DATA_VALUE_LENGTH) {
                result.put(key, text);
            }
        }
        return result;
    }

    private static String stringMapText(Object value) {
        if (value == null || value instanceof Map<?, ?> || value instanceof List<?>) {
            return null;
        }
        return String.valueOf(value);
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
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String string) {
            if ("true".equalsIgnoreCase(string)) {
                return true;
            }
            if ("false".equalsIgnoreCase(string)) {
                return false;
            }
        }
        throw new IllegalArgumentException("Invalid boolean field: " + key);
    }

    private static int integer(Map<String, Object> object, String key, int fallback) {
        long value = longNumber(object, key, fallback);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Integer field out of range: " + key);
        }
        return (int) value;
    }

    private static long longNumber(Map<String, Object> object, String key, long fallback) {
        Object value = object.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            if (number instanceof Double || number instanceof Float) {
                double decimal = number.doubleValue();
                if (!Double.isFinite(decimal) || Math.rint(decimal) != decimal
                        || decimal < Long.MIN_VALUE || decimal > Long.MAX_VALUE) {
                    throw new IllegalArgumentException("Invalid integer field: " + key);
                }
            }
            return number.longValue();
        }
        if (value instanceof String string) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {
            }
        }
        throw new IllegalArgumentException("Invalid integer field: " + key);
    }

    private static double decimal(Map<String, Object> object, String key, double fallback) {
        Object value = object.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            double decimal = number.doubleValue();
            if (!Double.isFinite(decimal)) {
                throw new IllegalArgumentException("Non-finite decimal field: " + key);
            }
            return decimal;
        }
        if (value instanceof String string) {
            try {
                double decimal = Double.parseDouble(string);
                if (!Double.isFinite(decimal)) {
                    throw new IllegalArgumentException("Non-finite decimal field: " + key);
                }
                return decimal;
            } catch (NumberFormatException ignored) {
            }
        }
        throw new IllegalArgumentException("Invalid decimal field: " + key);
    }
}

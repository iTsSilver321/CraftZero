package com.craftzero.multiplayer;

import java.util.Map;

public final class MultiplayerProtocol {
    public static final int DEFAULT_PORT = 25565;
    public static final int DEFAULT_QUERY_PORT = DEFAULT_PORT;
    public static final int DEFAULT_MAX_PLAYERS = 20;
    public static final int MIN_VIEW_DISTANCE = 3;
    public static final int MAX_VIEW_DISTANCE = 15;
    public static final int DEFAULT_VIEW_DISTANCE = 10;
    public static final int VERSION = 1;
    public static final long JOIN_TIMEOUT_MILLIS = 15_000L;
    public static final long KEEP_ALIVE_INTERVAL_MILLIS = 15_000L;
    public static final long KEEP_ALIVE_TIMEOUT_MILLIS = 30_000L;
    public static final int WORLD_MIN_Y = 0;
    public static final int WORLD_HEIGHT = 128;
    public static final int MIN_MAX_BUILD_HEIGHT = 1;
    public static final int DEFAULT_MAX_BUILD_HEIGHT = WORLD_HEIGHT;
    public static final boolean DEFAULT_GENERATE_STRUCTURES = true;
    public static final int BLOCK_METADATA_MIN = 0;
    public static final int BLOCK_METADATA_MAX = 15;
    public static final int INVENTORY_HOTBAR_SLOTS = 9;
    public static final int INVENTORY_MAIN_SLOTS = 27;
    public static final int INVENTORY_CRAFTING_SLOTS = 4;
    public static final int INVENTORY_ARMOR_SLOTS = 4;
    public static final int INVENTORY_CURSOR_SLOT = INVENTORY_HOTBAR_SLOTS + INVENTORY_MAIN_SLOTS
            + INVENTORY_CRAFTING_SLOTS + INVENTORY_ARMOR_SLOTS;
    public static final int INVENTORY_SLOT_COUNT = INVENTORY_CURSOR_SLOT + 1;
    public static final int MAX_STACK_COUNT = 64;
    public static final int MIN_ITEM_DAMAGE = -1;
    public static final int MAX_ITEM_DAMAGE = Short.MAX_VALUE;
    public static final int MAX_NETWORK_ID_LENGTH = 64;
    public static final int MAX_ITEM_STACK_DATA_ENTRIES = 128;
    public static final int MAX_ITEM_STACK_DATA_KEY_LENGTH = 96;
    public static final int MAX_ITEM_STACK_DATA_VALUE_LENGTH = 512;
    public static final int MAX_PROTOCOL_LINE_LENGTH = 32_768;
    public static final int MAX_LEGACY_MESSAGE_TYPE_LENGTH = 64;
    public static final int MAX_LEGACY_MESSAGE_DATA_ENTRIES = 4_096;
    public static final int MAX_LEGACY_MESSAGE_DATA_VALUE_LENGTH = 8_192;
    public static final double PLAYER_EYE_HEIGHT = 1.62d;
    public static final double MAX_CLIENT_BLOCK_EDIT_DISTANCE = 8.0d;
    public static final double MAX_CLIENT_BLOCK_EDIT_DISTANCE_SQ =
            MAX_CLIENT_BLOCK_EDIT_DISTANCE * MAX_CLIENT_BLOCK_EDIT_DISTANCE;
    public static final double MAX_CLIENT_ENTITY_ACTION_DISTANCE = 6.0d;
    public static final double MAX_CLIENT_ENTITY_ACTION_DISTANCE_SQ =
            MAX_CLIENT_ENTITY_ACTION_DISTANCE * MAX_CLIENT_ENTITY_ACTION_DISTANCE;
    public static final float MAX_PLAYER_HEALTH = 20.0f;
    public static final double MAX_WORLD_COORDINATE = 30_000_000.0d;
    public static final double MIN_PROTOCOL_PLAYER_Y = -1024.0d;
    public static final double MAX_PROTOCOL_PLAYER_Y = WORLD_HEIGHT + 1024.0d;
    public static final int MAX_CHAT_TEXT_LENGTH = 256;
    public static final int SIGN_LINE_COUNT = 4;
    public static final int MAX_SIGN_LINE_LENGTH = 15;
    public static final int MAX_CONTAINER_UPDATE_DATA_ENTRIES = 4096;
    public static final int MAX_CLIENT_STATUS_EFFECTS = 32;
    public static final int MAX_CLIENT_STATUS_EFFECT_DURATION = 24_000;
    public static final int MAX_CLIENT_STATUS_EFFECT_AMPLIFIER = 255;
    public static final int MAX_CLIENT_FIRE_TICKS = 24_000;
    public static final float MAX_CLIENT_FOOD_STAT = 20.0f;
    public static final float MAX_CLIENT_AIR_STAT = 15.0f;
    public static final float MAX_CLIENT_EXHAUSTION_STAT = 40.0f;
    public static final String ACTION_BED_SLEEP_START = "bed_sleep_start";
    public static final String ACTION_BED_SLEEP_STOP = "bed_sleep_stop";
    public static final String ACTION_BED_SLEEP_COMPLETE = "bed_sleep_complete";
    public static final String ACTION_SIGN_UPDATE = "sign_update";
    public static final String ACTION_CONTAINER_UPDATE = "container_update";
    public static final String ACTION_ENCHANT_ITEM = "enchant_item";
    public static final String ACTION_CRAFT_ITEM = "craft_item";
    public static final String ACTION_INVENTORY_SYNC = "inventory_sync";
    public static final String ACTION_COMMAND_PRIVATE_MESSAGE = "command_private_message";
    public static final String ACTION_COMMAND_GIVE = "command_give";
    public static final String ACTION_COMMAND_TELEPORT = "command_teleport";
    public static final String ACTION_COMMAND_KILL = "command_kill";
    public static final String ACTION_COMMAND_CLEAR = "command_clear";
    public static final String ACTION_COMMAND_SPAWNPOINT = "command_spawnpoint";
    public static final String ACTION_COMMAND_GAMEMODE = "command_gamemode";
    public static final String ACTION_COMMAND_EXPERIENCE = "command_experience";
    public static final String ACTION_COMMAND_DAMAGE = "command_damage";
    public static final String ACTION_COMMAND_VELOCITY = "command_velocity";
    public static final String ACTION_COMMAND_POTION_EFFECT = "command_potion_effect";
    public static final String ACTION_ENTITY_ATTACK = "entity_attack";
    public static final String ACTION_ENTITY_USE = "entity_use";
    public static final String ACTION_PLAYER_ATTACK = "player_attack";
    public static final String ACTION_ITEM_USE = "item_use";
    public static final String ACTION_PLAYER_RESPAWN = "player_respawn";
    public static final String EVENT_INITIAL_SYNC_COMPLETE = "initial_sync_complete";

    private MultiplayerProtocol() {
    }

    public static boolean isValidBlockUpdate(String blockId, int y, int metadata) {
        return isValidNetworkId(blockId)
                && y >= WORLD_MIN_Y
                && y < WORLD_HEIGHT
                && metadata >= BLOCK_METADATA_MIN
                && metadata <= BLOCK_METADATA_MAX;
    }

    public static boolean isValidInventoryUpdate(String itemId, int slot, int count, int damage) {
        return isValidInventorySlot(slot)
                && isValidNetworkId(itemId)
                && count >= 0
                && count <= MAX_STACK_COUNT
                && damage >= MIN_ITEM_DAMAGE
                && damage <= MAX_ITEM_DAMAGE;
    }

    public static boolean isValidItemStackData(Map<String, String> data) {
        if (data == null) {
            return true;
        }
        if (data.size() > MAX_ITEM_STACK_DATA_ENTRIES) {
            return false;
        }
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || key.isBlank() || key.length() > MAX_ITEM_STACK_DATA_KEY_LENGTH) {
                return false;
            }
            if (value == null || value.length() > MAX_ITEM_STACK_DATA_VALUE_LENGTH) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValidClientPlayerStateData(Map<String, String> data) {
        if (data == null) {
            return true;
        }
        if (data.size() > MAX_ITEM_STACK_DATA_ENTRIES) {
            return false;
        }
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (!isValidClientPlayerStateData(entry.getKey(), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValidClientPlayerStateData(String key, String value) {
        if (key == null || key.isBlank() || key.length() > MAX_ITEM_STACK_DATA_KEY_LENGTH
                || value == null || value.length() > MAX_ITEM_STACK_DATA_VALUE_LENGTH) {
            return false;
        }
        if (key.startsWith("status.")) {
            return isValidClientStatusStateData(key, value);
        }
        if (key.startsWith("stats.")) {
            return isValidClientStatsStateData(key, value);
        }
        if (key.startsWith("progression.")) {
            return isValidClientProgressionStateData(key, value);
        }
        if (key.startsWith("respawn.") || key.startsWith("bedSpawn.")) {
            return isValidClientRespawnStateData(key, value);
        }
        if (key.startsWith("remote.")) {
            return isValidClientRemotePlayerStateData(key, value);
        }
        if (!key.startsWith("vehicle.")) {
            return false;
        }
        return switch (key) {
            case "vehicle.entityId" -> value.isBlank() || isValidNetworkId(value);
            case "vehicle.type" -> value.isBlank()
                    || "boat".equals(value)
                    || "minecart".equals(value)
                    || "pig".equals(value);
            case "vehicle.mounted", "vehicle.dismount" -> isBooleanText(value);
            case "vehicle.forward", "vehicle.strafe" -> isFiniteFloatText(value)
                    && Math.abs(Float.parseFloat(value)) <= 1.01f;
            case "vehicle.yaw" -> isFiniteFloatText(value);
            default -> false;
        };
    }

    private static boolean isValidClientStatusStateData(String key, String value) {
        if ("status.count".equals(key)) {
            return isClampedIntegerText(value, 0, MAX_CLIENT_STATUS_EFFECTS);
        }
        String rest = key.substring("status.".length());
        int separator = rest.indexOf('.');
        if (separator <= 0 || separator >= rest.length() - 1) {
            return false;
        }
        if (!isClampedIntegerText(rest.substring(0, separator), 0, MAX_CLIENT_STATUS_EFFECTS - 1)) {
            return false;
        }
        return switch (rest.substring(separator + 1)) {
            case "type" -> isValidNetworkId(value);
            case "duration" -> isClampedIntegerText(value, 0, MAX_CLIENT_STATUS_EFFECT_DURATION);
            case "amplifier" -> isClampedIntegerText(value, 0, MAX_CLIENT_STATUS_EFFECT_AMPLIFIER);
            default -> false;
        };
    }

    private static boolean isValidClientStatsStateData(String key, String value) {
        return switch (key) {
            case "stats.health" -> isClampedFiniteFloatText(value, 0.0f, MAX_PLAYER_HEALTH);
            case "stats.hunger", "stats.saturation" -> isClampedFiniteFloatText(value, 0.0f, MAX_CLIENT_FOOD_STAT);
            case "stats.air" -> isClampedFiniteFloatText(value, 0.0f, MAX_CLIENT_AIR_STAT);
            case "stats.exhaustion" -> isClampedFiniteFloatText(value, 0.0f, MAX_CLIENT_EXHAUSTION_STAT);
            case "stats.onFire" -> isBooleanText(value);
            case "stats.fireTicks" -> isClampedIntegerText(value, 0, MAX_CLIENT_FIRE_TICKS);
            default -> false;
        };
    }

    private static boolean isValidClientProgressionStateData(String key, String value) {
        return switch (key) {
            case "progression.totalExperience",
                    "progression.score",
                    "progression.level",
                    "progression.experienceIntoLevel",
                    "progression.experienceToNextLevel" -> isClampedIntegerText(value, 0, Integer.MAX_VALUE);
            default -> false;
        };
    }

    private static boolean isValidClientRemotePlayerStateData(String key, String value) {
        return switch (key) {
            case "remote.sprinting", "remote.usingItem", "remote.blocking", "remote.drawingBow" -> isBooleanText(value);
            case "remote.useProgress" -> isFiniteFloatText(value)
                    && Float.parseFloat(value) >= 0.0f
                    && Float.parseFloat(value) <= 1.0f;
            default -> false;
        };
    }

    private static boolean isValidClientRespawnStateData(String key, String value) {
        return switch (key) {
            case "respawn.x", "respawn.z" -> isFiniteFloatText(value)
                    && Math.abs(Float.parseFloat(value)) <= MAX_WORLD_COORDINATE;
            case "respawn.y" -> isFiniteFloatText(value)
                    && Float.parseFloat(value) >= MIN_PROTOCOL_PLAYER_Y
                    && Float.parseFloat(value) <= MAX_PROTOCOL_PLAYER_Y;
            case "bedSpawn.set" -> isBooleanText(value);
            case "bedSpawn.x", "bedSpawn.z" -> isIntegerText(value)
                    && Math.abs(Integer.parseInt(value)) <= MAX_WORLD_COORDINATE;
            case "bedSpawn.y" -> isIntegerText(value)
                    && Integer.parseInt(value) >= WORLD_MIN_Y
                    && Integer.parseInt(value) < WORLD_HEIGHT;
            default -> false;
        };
    }

    public static boolean isValidInventorySlot(int slot) {
        return slot >= 0 && slot < INVENTORY_SLOT_COUNT;
    }

    public static boolean isValidNetworkId(String value) {
        return value != null && !value.isBlank() && value.length() <= MAX_NETWORK_ID_LENGTH;
    }

    public static boolean isValidLegacyMessageType(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_LEGACY_MESSAGE_TYPE_LENGTH) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!(c == '_' || c == '-' || c == '.'
                    || Character.isDigit(c)
                    || (c >= 'A' && c <= 'Z')
                    || (c >= 'a' && c <= 'z'))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValidLegacyDataKey(String value) {
        return value != null && !value.isBlank() && value.length() <= MAX_ITEM_STACK_DATA_KEY_LENGTH;
    }

    public static boolean isValidLegacyDataValue(String value) {
        return value != null && value.length() <= MAX_LEGACY_MESSAGE_DATA_VALUE_LENGTH;
    }

    private static boolean isBooleanText(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    private static boolean isFiniteFloatText(String value) {
        try {
            float parsed = Float.parseFloat(value);
            return !Float.isNaN(parsed) && !Float.isInfinite(parsed);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isClampedFiniteFloatText(String value, float min, float max) {
        try {
            float parsed = Float.parseFloat(value);
            return !Float.isNaN(parsed) && !Float.isInfinite(parsed)
                    && parsed >= min
                    && parsed <= max;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isIntegerText(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isClampedIntegerText(String value, int min, int max) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed >= min && parsed <= max;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}

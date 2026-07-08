package com.craftzero.multiplayer;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface ProtocolMessage {
    String type();

    final class Type {
        public static final String HELLO = "hello";
        public static final String WORLD_STATE = "world_state";
        public static final String KEEP_ALIVE = "keep_alive";
        public static final String JOIN = "join";
        public static final String PLAYER_LIST = "player_list";
        public static final String CLIENT_INPUT = "client_input";
        public static final String CLIENT_ACTION = "client_action";
        public static final String PLAYER_STATE = "player_state";
        public static final String BLOCK_UPDATE = "block_update";
        public static final String ENTITY_UPDATE = "entity_update";
        public static final String INVENTORY_UPDATE = "inventory_update";
        public static final String WORLD_EVENT = "world_event";
        public static final String CHAT = "chat";
        public static final String DISCONNECT = "disconnect";

        private Type() {
        }
    }

    record Hello(int protocolVersion, String playerId, String serverName) implements ProtocolMessage {
        public Hello {
            playerId = Objects.requireNonNull(playerId, "playerId");
            serverName = Objects.requireNonNullElse(serverName, "CraftZero");
        }

        @Override
        public String type() {
            return Type.HELLO;
        }
    }

    record KeepAlive(int id) implements ProtocolMessage {
        @Override
        public String type() {
            return Type.KEEP_ALIVE;
        }
    }

    record WorldState(
            long seed,
            double timeOfDay,
            String weatherState,
            int spawnX,
            int spawnY,
            int spawnZ,
            String gameMode,
            String difficulty,
            boolean hardcore,
            boolean allowCheats,
            boolean pvp,
            boolean spawnAnimals,
            boolean spawnMonsters,
            boolean spawnNpcs,
            boolean allowNether,
            boolean allowFlight,
            String dimension,
            int maxPlayers,
            int viewDistance,
            int maxBuildHeight,
            boolean generateStructures,
            List<PlayerState> players,
            List<BlockUpdate> blockUpdates,
            List<EntityUpdate> entityUpdates,
            List<InventoryUpdate> inventoryUpdates
    ) implements ProtocolMessage {
        public WorldState(long seed, double timeOfDay, List<PlayerState> players) {
            this(seed, timeOfDay, "clear", players, List.of(), List.of(), List.of());
        }

        public WorldState(long seed, double timeOfDay, String weatherState, List<PlayerState> players) {
            this(seed, timeOfDay, weatherState, players, List.of(), List.of(), List.of());
        }

        public WorldState(long seed, double timeOfDay, List<PlayerState> players,
                List<BlockUpdate> blockUpdates, List<EntityUpdate> entityUpdates,
                List<InventoryUpdate> inventoryUpdates) {
            this(seed, timeOfDay, "clear", players, blockUpdates, entityUpdates, inventoryUpdates);
        }

        public WorldState(long seed, double timeOfDay, String weatherState, List<PlayerState> players,
                List<BlockUpdate> blockUpdates, List<EntityUpdate> entityUpdates,
                List<InventoryUpdate> inventoryUpdates) {
            this(seed, timeOfDay, weatherState, 0, 80, 0, "SURVIVAL", "EASY", false, false,
                    true, true, true, true, true, false, "overworld", MultiplayerProtocol.DEFAULT_MAX_PLAYERS,
                    MultiplayerProtocol.DEFAULT_VIEW_DISTANCE, MultiplayerProtocol.DEFAULT_MAX_BUILD_HEIGHT,
                    MultiplayerProtocol.DEFAULT_GENERATE_STRUCTURES,
                    players, blockUpdates, entityUpdates, inventoryUpdates);
        }

        public WorldState {
            weatherState = normalize(weatherState, "clear");
            gameMode = normalize(gameMode, "SURVIVAL");
            difficulty = normalize(difficulty, "EASY");
            dimension = normalize(dimension, "overworld");
            maxPlayers = Math.max(1, maxPlayers);
            viewDistance = Math.max(MultiplayerProtocol.MIN_VIEW_DISTANCE,
                    Math.min(MultiplayerProtocol.MAX_VIEW_DISTANCE, viewDistance));
            maxBuildHeight = Math.max(MultiplayerProtocol.MIN_MAX_BUILD_HEIGHT,
                    Math.min(MultiplayerProtocol.WORLD_HEIGHT, maxBuildHeight));
            players = players == null ? List.of() : List.copyOf(players);
            blockUpdates = blockUpdates == null ? List.of() : List.copyOf(blockUpdates);
            entityUpdates = entityUpdates == null ? List.of() : List.copyOf(entityUpdates);
            inventoryUpdates = inventoryUpdates == null ? List.of() : List.copyOf(inventoryUpdates);
        }

        @Override
        public String type() {
            return Type.WORLD_STATE;
        }
    }

    record PlayerList(List<PlayerListEntry> players) implements ProtocolMessage {
        public PlayerList {
            players = players == null ? List.of() : List.copyOf(players);
        }

        @Override
        public String type() {
            return Type.PLAYER_LIST;
        }
    }

    record PlayerListEntry(String playerId, String username, int latencyMillis) {
        public PlayerListEntry {
            playerId = normalize(playerId, "");
            username = normalize(username, "Player");
            latencyMillis = Math.max(-1, latencyMillis);
        }
    }

    record Join(String username, int protocolVersion) implements ProtocolMessage {
        public Join(String username) {
            this(username, MultiplayerProtocol.VERSION);
        }

        public Join {
            username = normalize(username, "Player");
        }

        @Override
        public String type() {
            return Type.JOIN;
        }
    }

    record ClientInput(
            String playerId,
            PlayerPose pose,
            boolean forward,
            boolean backward,
            boolean left,
            boolean right,
            boolean jumping,
            boolean sneaking,
            boolean onGround,
            float health,
            String heldItemId,
            int heldItemCount,
            int heldItemDamage,
            int selectedSlot,
            String gameMode,
            Map<String, String> data
    ) implements ProtocolMessage {
        public ClientInput(String playerId, PlayerPose pose, boolean forward, boolean backward, boolean left,
                boolean right, boolean jumping, boolean sneaking) {
            this(playerId, pose, forward, backward, left, right, jumping, sneaking, !jumping,
                    20.0f, "air", 0, 0, 0, "SURVIVAL", Map.of());
        }

        public ClientInput(String playerId, PlayerPose pose, boolean forward, boolean backward, boolean left,
                boolean right, boolean jumping, boolean sneaking, boolean onGround, float health,
                String heldItemId, int heldItemCount, int heldItemDamage, int selectedSlot, String gameMode) {
            this(playerId, pose, forward, backward, left, right, jumping, sneaking, onGround, health,
                    heldItemId, heldItemCount, heldItemDamage, selectedSlot, gameMode, Map.of());
        }

        public ClientInput {
            playerId = normalize(playerId, "");
            pose = pose == null ? PlayerPose.origin() : pose;
            health = Math.max(0.0f, health);
            heldItemId = normalize(heldItemId, "air");
            heldItemCount = Math.max(0, heldItemCount);
            selectedSlot = Math.max(0, selectedSlot);
            gameMode = normalize(gameMode, "SURVIVAL");
            data = data == null ? Map.of() : Map.copyOf(data);
        }

        @Override
        public String type() {
            return Type.CLIENT_INPUT;
        }
    }

    record ClientAction(
            String playerId,
            String action,
            BlockUpdate blockUpdate,
            Map<String, String> data
    ) implements ProtocolMessage {
        public ClientAction {
            playerId = normalize(playerId, "");
            action = normalize(action, "action");
            data = data == null ? Map.of() : Map.copyOf(data);
        }

        public static ClientAction blockUpdate(String playerId, BlockUpdate blockUpdate) {
            return new ClientAction(playerId, Type.BLOCK_UPDATE, blockUpdate, Map.of());
        }

        @Override
        public String type() {
            return Type.CLIENT_ACTION;
        }
    }

    record PlayerState(
            String playerId,
            String username,
            PlayerPose pose,
            boolean onGround,
            boolean sneaking,
            float health,
            String heldItemId,
            int heldItemCount,
            int heldItemDamage,
            int selectedSlot,
            String gameMode,
            Map<String, String> data
    ) implements ProtocolMessage {
        public PlayerState(String playerId, String username, PlayerPose pose, boolean onGround) {
            this(playerId, username, pose, onGround, false, 20.0f, "air", 0, 0, 0, "SURVIVAL", Map.of());
        }

        public PlayerState(String playerId, String username, PlayerPose pose, boolean onGround, boolean sneaking,
                float health, String heldItemId, int heldItemCount, int heldItemDamage, int selectedSlot,
                String gameMode) {
            this(playerId, username, pose, onGround, sneaking, health, heldItemId, heldItemCount, heldItemDamage,
                    selectedSlot, gameMode, Map.of());
        }

        public PlayerState {
            playerId = normalize(playerId, "");
            username = normalize(username, "Player");
            pose = pose == null ? PlayerPose.origin() : pose;
            health = Math.max(0.0f, health);
            heldItemId = normalize(heldItemId, "air");
            heldItemCount = Math.max(0, heldItemCount);
            selectedSlot = Math.max(0, selectedSlot);
            gameMode = normalize(gameMode, "SURVIVAL");
            data = data == null ? Map.of() : Map.copyOf(data);
        }

        public PlayerState withPlayerId(String playerId) {
            return new PlayerState(playerId, username, pose, onGround, sneaking, health,
                    heldItemId, heldItemCount, heldItemDamage, selectedSlot, gameMode, data);
        }

        @Override
        public String type() {
            return Type.PLAYER_STATE;
        }
    }

    record BlockUpdate(
            int x,
            int y,
            int z,
            String blockId,
            int metadata,
            String sourcePlayerId,
            Map<String, String> data
    ) implements ProtocolMessage {
        public BlockUpdate(int x, int y, int z, String blockId, int metadata, String sourcePlayerId) {
            this(x, y, z, blockId, metadata, sourcePlayerId, Map.of());
        }

        public BlockUpdate {
            blockId = normalize(blockId, "air");
            sourcePlayerId = normalize(sourcePlayerId, "");
            data = data == null ? Map.of() : Map.copyOf(data);
        }

        public BlockUpdate withSourcePlayerId(String playerId) {
            return new BlockUpdate(x, y, z, blockId, metadata, playerId, data);
        }

        @Override
        public String type() {
            return Type.BLOCK_UPDATE;
        }
    }

    record EntityUpdate(
            String entityId,
            String entityType,
            PlayerPose pose,
            Map<String, String> data
    ) implements ProtocolMessage {
        public EntityUpdate {
            entityId = normalize(entityId, "");
            entityType = normalize(entityType, "entity");
            pose = pose == null ? PlayerPose.origin() : pose;
            data = data == null ? Map.of() : Map.copyOf(data);
        }

        @Override
        public String type() {
            return Type.ENTITY_UPDATE;
        }
    }

    record InventoryUpdate(
            String playerId,
            int slot,
            String itemId,
            int count,
            int damage,
            Map<String, String> data
    ) implements ProtocolMessage {
        public InventoryUpdate(String playerId, int slot, String itemId, int count, int damage) {
            this(playerId, slot, itemId, count, damage, Map.of());
        }

        public InventoryUpdate {
            playerId = normalize(playerId, "");
            itemId = normalize(itemId, "air");
            data = data == null ? Map.of() : Map.copyOf(data);
        }

        @Override
        public String type() {
            return Type.INVENTORY_UPDATE;
        }
    }

    record WorldEvent(String eventType, Map<String, String> data) implements ProtocolMessage {
        public WorldEvent {
            eventType = normalize(eventType, "event");
            data = data == null ? Map.of() : Map.copyOf(data);
        }

        @Override
        public String type() {
            return Type.WORLD_EVENT;
        }
    }

    record Chat(String playerId, String sender, String text) implements ProtocolMessage {
        public Chat {
            playerId = normalize(playerId, "");
            sender = normalize(sender, "Player");
            text = Objects.requireNonNullElse(text, "");
        }

        public Chat withPlayerId(String playerId) {
            return new Chat(playerId, sender, text);
        }

        public Chat withSender(String sender) {
            return new Chat(playerId, sender, text);
        }

        @Override
        public String type() {
            return Type.CHAT;
        }
    }

    record Disconnect(String playerId, String reason) implements ProtocolMessage {
        public Disconnect {
            playerId = normalize(playerId, "");
            reason = normalize(reason, "disconnect");
        }

        @Override
        public String type() {
            return Type.DISCONNECT;
        }
    }

    record PlayerPose(
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {
        public static PlayerPose origin() {
            return new PlayerPose(0.0, 0.0, 0.0, 0.0f, 0.0f);
        }
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}

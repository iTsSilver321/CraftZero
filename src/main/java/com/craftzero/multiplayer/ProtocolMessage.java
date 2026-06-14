package com.craftzero.multiplayer;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface ProtocolMessage {
    String type();

    final class Type {
        public static final String HELLO = "hello";
        public static final String WORLD_STATE = "world_state";
        public static final String JOIN = "join";
        public static final String CLIENT_INPUT = "client_input";
        public static final String CLIENT_ACTION = "client_action";
        public static final String PLAYER_STATE = "player_state";
        public static final String BLOCK_UPDATE = "block_update";
        public static final String ENTITY_UPDATE = "entity_update";
        public static final String INVENTORY_UPDATE = "inventory_update";
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

    record WorldState(long seed, double timeOfDay, List<PlayerState> players) implements ProtocolMessage {
        public WorldState {
            players = players == null ? List.of() : List.copyOf(players);
        }

        @Override
        public String type() {
            return Type.WORLD_STATE;
        }
    }

    record Join(String username) implements ProtocolMessage {
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
            boolean sneaking
    ) implements ProtocolMessage {
        public ClientInput {
            playerId = normalize(playerId, "");
            pose = pose == null ? PlayerPose.origin() : pose;
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
            boolean onGround
    ) implements ProtocolMessage {
        public PlayerState {
            playerId = normalize(playerId, "");
            username = normalize(username, "Player");
            pose = pose == null ? PlayerPose.origin() : pose;
        }

        public PlayerState withPlayerId(String playerId) {
            return new PlayerState(playerId, username, pose, onGround);
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
            String sourcePlayerId
    ) implements ProtocolMessage {
        public BlockUpdate {
            blockId = normalize(blockId, "air");
            sourcePlayerId = normalize(sourcePlayerId, "");
        }

        public BlockUpdate withSourcePlayerId(String playerId) {
            return new BlockUpdate(x, y, z, blockId, metadata, playerId);
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
            int damage
    ) implements ProtocolMessage {
        public InventoryUpdate {
            playerId = normalize(playerId, "");
            itemId = normalize(itemId, "air");
        }

        @Override
        public String type() {
            return Type.INVENTORY_UPDATE;
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

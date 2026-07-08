package com.craftzero.command;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Difficulty;
import com.craftzero.main.GameMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandDispatcherTest {

    @Test
    @DisplayName("Command suggestions should include command names and item arguments")
    void commandSuggestions() {
        CommandDispatcher dispatcher = new CommandDispatcher();
        FakeContext context = new FakeContext();

        assertTrue(dispatcher.suggestions("/g", context).contains("/gamemode"));
        assertTrue(dispatcher.suggestions("/give dia", context).contains("diamond"));
        assertTrue(dispatcher.suggestions("/difficulty h", context).contains("hard"));
    }

    @Test
    @DisplayName("Give should accept namespaced names and old numeric id:data values")
    void giveCommandParsesItems() {
        CommandDispatcher dispatcher = new CommandDispatcher();
        FakeContext context = new FakeContext();

        dispatcher.execute("/give Steve minecraft:diamond 3", context);
        assertEquals(ItemType.DIAMOND, context.given.get(0).getType());
        assertEquals(3, context.given.get(0).getCount());

        dispatcher.execute("/give 263:1 2", context);
        assertEquals(ItemType.CHARCOAL, context.given.get(1).getType());
        assertEquals(2, context.given.get(1).getCount());

        dispatcher.execute("/give 51 1", context);
        assertEquals(ItemType.FIRE, context.given.get(2).getType());
        assertEquals(1, context.given.get(2).getCount());

        dispatcher.execute("/give minecraft:mob_spawner 1", context);
        assertEquals(ItemType.MOB_SPAWNER, context.given.get(3).getType());
        assertEquals(1, context.given.get(3).getCount());

        dispatcher.execute("/give 97:2 1", context);
        assertEquals(ItemType.INFESTED_STONE_BRICK, context.given.get(4).getType());
        assertEquals(1, context.given.get(4).getCount());

        dispatcher.execute("/give 43:5 2", context);
        assertEquals(ItemType.DOUBLE_STONE_BRICK_SLAB, context.given.get(5).getType());
        assertEquals(2, context.given.get(5).getCount());
    }

    @Test
    @DisplayName("Time, teleport, and gamemode commands should update context")
    void gameplayCommandsUpdateContext() {
        CommandDispatcher dispatcher = new CommandDispatcher();
        FakeContext context = new FakeContext();

        dispatcher.execute("/time set night", context);
        dispatcher.execute("/tp ~ 70 -4", context);
        dispatcher.execute("/gamemode creative", context);
        dispatcher.execute("/weather thunder", context);

        assertEquals(13000.0f, context.time, 0.0001f);
        assertEquals(10.0f, context.x, 0.0001f);
        assertEquals(70.0f, context.y, 0.0001f);
        assertEquals(-4.0f, context.z, 0.0001f);
        assertEquals(GameMode.CREATIVE, context.gameMode);
        assertEquals("thunder", context.weather);
    }

    @Test
    @DisplayName("Tell should route private messages to known remote players")
    void tellRoutesPrivateMessagesToRemotePlayers() {
        CommandDispatcher dispatcher = new CommandDispatcher();
        FakeContext context = new FakeContext();

        assertTrue(dispatcher.execute("/tell Alex meet at spawn", context));

        assertEquals("Steve->Alex:meet at spawn", context.privateMessages.get(0));
        assertEquals("[Steve -> Alex] meet at spawn", context.feedback.get(0));
    }

    @Test
    @DisplayName("Targeted player commands should route known remote players through context hooks")
    void targetedCommandsRouteRemotePlayersThroughContextHooks() {
        CommandDispatcher dispatcher = new CommandDispatcher();
        FakeContext context = new FakeContext();

        assertTrue(dispatcher.execute("/tp Alex ~1 70 ~-2", context));
        assertTrue(dispatcher.execute("/give Alex minecraft:diamond 2", context));
        assertTrue(dispatcher.execute("/clear Alex diamond", context));
        assertTrue(dispatcher.execute("/clear Alex", context));
        assertTrue(dispatcher.execute("/kill Alex", context));
        assertTrue(dispatcher.execute("/spawnpoint Alex", context));
        assertTrue(dispatcher.execute("/spawnpoint Alex 2 64 2", context));

        assertEquals("tp:Alex:21.0:70.0:28.0", context.remoteActions.get(0));
        assertEquals(ItemType.DIAMOND, context.remoteGiven.get(0).getType());
        assertEquals(2, context.remoteGiven.get(0).getCount());
        assertEquals("clear:Alex:DIAMOND", context.remoteActions.get(1));
        assertEquals("clear:Alex:null", context.remoteActions.get(2));
        assertEquals("kill:Alex", context.remoteActions.get(3));
        assertEquals("spawn-current:Alex", context.remoteActions.get(4));
        assertEquals("spawn:Alex:2.0:64.0:2.0", context.remoteActions.get(5));
        assertEquals(10.0f, context.x, 0.0001f);
        assertTrue(context.given.isEmpty());
    }

    @Test
    @DisplayName("Release 1.0 server-admin commands should dispatch through admin context")
    void adminCommandsDispatch() {
        CommandDispatcher dispatcher = new CommandDispatcher();
        FakeContext context = new FakeContext();

        dispatcher.execute("/ban Alex being rude", context);

        assertEquals("ban ok", context.feedback.get(0));
    }

    @Test
    @DisplayName("Commands should reject execution and autocomplete when context lacks permission")
    void commandPermissionsGateExecutionAndSuggestions() {
        CommandDispatcher dispatcher = new CommandDispatcher();
        FakeContext context = new FakeContext();
        context.allowOnly("help", "list", "me", "msg", "seed");

        assertFalse(dispatcher.execute("/give Steve diamond 1", context));
        assertEquals("You do not have permission to use this command.", context.feedback.get(0));
        assertFalse(dispatcher.suggestions("/g", context).contains("/give"));
        assertTrue(dispatcher.suggestions("/l", context).contains("/list"));
        assertEquals(List.of(), dispatcher.suggestions("/give d", context));
    }

    @Test
    @DisplayName("Help output should only include commands visible to the current context")
    void helpRespectsPermissions() {
        CommandDispatcher dispatcher = new CommandDispatcher();
        FakeContext context = new FakeContext();
        context.allowOnly("help", "list");

        dispatcher.execute("/help", context);
        assertTrue(context.feedback.get(0).contains("/help"));
        assertTrue(context.feedback.get(0).contains("/list"));
        assertFalse(context.feedback.get(0).contains("/give"));

        dispatcher.execute("/help give", context);
        assertEquals("You do not have permission to use this command.", context.feedback.get(1));
    }

    private static final class FakeContext implements CommandDispatcher.Context {
        private final List<String> feedback = new ArrayList<>();
        private final List<ItemStack> given = new ArrayList<>();
        private final List<ItemStack> remoteGiven = new ArrayList<>();
        private final List<String> privateMessages = new ArrayList<>();
        private final List<String> remoteActions = new ArrayList<>();
        private Set<String> allowedCommands;
        private float time = 6000.0f;
        private float x = 10.0f;
        private float y = 64.0f;
        private float z = 10.0f;
        private float alexX = 20.0f;
        private float alexY = 65.0f;
        private float alexZ = 30.0f;
        private GameMode gameMode = GameMode.SURVIVAL;
        private Difficulty difficulty = Difficulty.EASY;
        private String weather = "clear";

        private void allowOnly(String... commands) {
            allowedCommands = new HashSet<>();
            Arrays.stream(commands)
                    .map(command -> command.toLowerCase(Locale.ROOT))
                    .forEach(allowedCommands::add);
        }

        @Override
        public String senderName() {
            return "Steve";
        }

        @Override
        public boolean hasPermission(String command) {
            return allowedCommands == null || allowedCommands.contains(command.toLowerCase(Locale.ROOT));
        }

        @Override
        public List<String> playerNames() {
            return List.of("Steve", "Alex");
        }

        @Override
        public long seed() {
            return 12345L;
        }

        @Override
        public float timeOfDay() {
            return time;
        }

        @Override
        public void setTimeOfDay(float time) {
            this.time = time;
        }

        @Override
        public GameMode gameMode() {
            return gameMode;
        }

        @Override
        public void setGameMode(GameMode mode) {
            this.gameMode = mode;
        }

        @Override
        public Difficulty difficulty() {
            return difficulty;
        }

        @Override
        public void setDifficulty(Difficulty difficulty) {
            this.difficulty = difficulty;
        }

        @Override
        public float playerX() {
            return x;
        }

        @Override
        public float playerY() {
            return y;
        }

        @Override
        public float playerZ() {
            return z;
        }

        @Override
        public float playerX(String target) {
            return "Alex".equalsIgnoreCase(target) ? alexX : x;
        }

        @Override
        public float playerY(String target) {
            return "Alex".equalsIgnoreCase(target) ? alexY : y;
        }

        @Override
        public float playerZ(String target) {
            return "Alex".equalsIgnoreCase(target) ? alexZ : z;
        }

        @Override
        public void teleport(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean teleportPlayer(String target, float x, float y, float z) {
            remoteActions.add("tp:" + target + ":" + x + ":" + y + ":" + z);
            if ("Alex".equalsIgnoreCase(target)) {
                alexX = x;
                alexY = y;
                alexZ = z;
            }
            return true;
        }

        @Override
        public boolean addItem(ItemStack stack) {
            given.add(stack);
            return true;
        }

        @Override
        public boolean addItemToPlayer(String target, ItemStack stack) {
            remoteGiven.add(stack);
            return true;
        }

        @Override
        public void clearInventory(ItemType filter) {
        }

        @Override
        public boolean clearPlayerInventory(String target, ItemType filter) {
            remoteActions.add("clear:" + target + ":" + (filter == null ? "null" : filter.name()));
            return true;
        }

        @Override
        public void kill() {
        }

        @Override
        public boolean killPlayer(String target) {
            remoteActions.add("kill:" + target);
            return true;
        }

        @Override
        public void setSpawn(float x, float y, float z) {
        }

        @Override
        public boolean setPlayerSpawn(String target, float x, float y, float z) {
            remoteActions.add("spawn:" + target + ":" + x + ":" + y + ":" + z);
            return true;
        }

        @Override
        public boolean setPlayerSpawnToCurrentPosition(String target) {
            remoteActions.add("spawn-current:" + target);
            return true;
        }

        @Override
        public void setWorldSpawn(int x, int y, int z) {
        }

        @Override
        public void addExperience(int amount) {
        }

        @Override
        public void setWeather(String weather) {
            this.weather = weather;
        }

        @Override
        public void saveAll() {
        }

        @Override
        public void setSavingEnabled(boolean enabled) {
        }

        @Override
        public boolean savingEnabled() {
            return true;
        }

        @Override
        public void requestStop() {
        }

        @Override
        public void feedback(String message) {
            feedback.add(message);
        }

        @Override
        public void broadcast(String message) {
            feedback.add(message);
        }

        @Override
        public boolean sendPrivateMessage(String sender, String target, String message) {
            privateMessages.add(sender + "->" + target + ":" + message);
            return true;
        }

        @Override
        public String runServerAdminCommand(String command, List<String> args) {
            return command + " ok";
        }
    }
}

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
    }

    @Test
    @DisplayName("Time, teleport, and gamemode commands should update context")
    void gameplayCommandsUpdateContext() {
        CommandDispatcher dispatcher = new CommandDispatcher();
        FakeContext context = new FakeContext();

        dispatcher.execute("/time set night", context);
        dispatcher.execute("/tp ~ 70 -4", context);
        dispatcher.execute("/gamemode creative", context);

        assertEquals(13000.0f, context.time, 0.0001f);
        assertEquals(10.0f, context.x, 0.0001f);
        assertEquals(70.0f, context.y, 0.0001f);
        assertEquals(-4.0f, context.z, 0.0001f);
        assertEquals(GameMode.CREATIVE, context.gameMode);
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
        private Set<String> allowedCommands;
        private float time = 6000.0f;
        private float x = 10.0f;
        private float y = 64.0f;
        private float z = 10.0f;
        private GameMode gameMode = GameMode.SURVIVAL;
        private Difficulty difficulty = Difficulty.EASY;

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
        public void teleport(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean addItem(ItemStack stack) {
            given.add(stack);
            return true;
        }

        @Override
        public void clearInventory(ItemType filter) {
        }

        @Override
        public void kill() {
        }

        @Override
        public void setSpawn(float x, float y, float z) {
        }

        @Override
        public void setWorldSpawn(int x, int y, int z) {
        }

        @Override
        public void addExperience(int amount) {
        }

        @Override
        public void setWeather(String weather) {
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
        public String runServerAdminCommand(String command, List<String> args) {
            return command + " ok";
        }
    }
}

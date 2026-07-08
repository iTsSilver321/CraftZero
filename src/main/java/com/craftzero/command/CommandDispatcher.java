package com.craftzero.command;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Difficulty;
import com.craftzero.main.GameMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Release 1.0-style command dispatcher with modern chat suggestions.
 */
public final class CommandDispatcher {
    private static final int HELP_PAGE_SIZE = 8;
    private static final String PERMISSION_DENIED = "You do not have permission to use this command.";
    private static final Set<String> ADMIN_PLACEHOLDERS = Set.of(
            "ban", "ban-ip", "banlist", "deop", "kick", "op", "pardon", "pardon-ip", "whitelist");

    private final Map<String, Command> commands = new LinkedHashMap<>();

    public CommandDispatcher() {
        register("help", "Shows command help.", this::help, "?");
        register("list", "Lists connected players.", this::list);
        register("me", "Sends an action message.", this::me);
        register("msg", "Sends a private message.", this::tell, "tell", "w");
        register("say", "Broadcasts a server-style message.", this::say);
        register("seed", "Prints the world seed.", this::seed);
        register("locate", "Finds the nearest generated structure.", this::locate);
        register("time", "Changes or queries world time.", this::time);
        register("gamemode", "Changes the current game mode.", this::gamemode);
        register("difficulty", "Changes the current difficulty.", this::difficulty);
        register("give", "Gives an item stack.", this::give);
        register("tp", "Teleports the player.", this::teleport, "teleport");
        register("kill", "Kills the player.", this::kill);
        register("clear", "Clears inventory contents.", this::clear);
        register("spawnpoint", "Sets the player spawn point.", this::spawnpoint);
        register("setworldspawn", "Sets the world spawn point.", this::setworldspawn);
        register("xp", "Adds experience.", this::xp, "experience");
        register("weather", "Sets the weather state.", this::weather);
        register("save-all", "Saves the world.", this::saveAll, "save");
        register("save-on", "Enables autosaving.", this::saveOn);
        register("save-off", "Disables autosaving.", this::saveOff);
        register("regenchunks", "Regenerates unmodified chunks around you.", this::regenChunks);
        register("stop", "Stops the local host.", this::stop);
        for (String name : ADMIN_PLACEHOLDERS) {
            register(name, "Manages server administration state.",
                    (args, context) -> adminCommand(name, args, context));
        }
    }

    public boolean execute(String rawInput, Context context) {
        if (context == null) {
            return false;
        }
        String raw = rawInput == null ? "" : rawInput.trim();
        if (raw.startsWith("/")) {
            raw = raw.substring(1).trim();
        }
        if (raw.isEmpty()) {
            return false;
        }

        List<String> tokens = split(raw);
        String name = normalizeCommand(tokens.get(0));
        Command command = commands.get(name);
        if (command == null) {
            context.feedback("Unknown command. Type /help for help.");
            return false;
        }
        if (!canUse(context, command)) {
            context.feedback(PERMISSION_DENIED);
            return false;
        }

        try {
            command.executor.accept(tokens.subList(1, tokens.size()), context);
            return true;
        } catch (CommandException exception) {
            context.feedback(exception.getMessage());
            return false;
        } catch (RuntimeException exception) {
            context.feedback("Command failed: " + exception.getMessage());
            return false;
        }
    }

    public List<String> suggestions(String rawInput, Context context) {
        String raw = rawInput == null ? "" : rawInput;
        if (!raw.startsWith("/")) {
            return context == null ? List.of() : matching(context.playerNames(), raw);
        }

        String body = raw.substring(1);
        boolean endsWithSpace = body.endsWith(" ");
        List<String> tokens = splitForSuggestions(body);
        if (tokens.isEmpty() || (tokens.size() == 1 && !endsWithSpace)) {
            String prefix = tokens.isEmpty() ? "" : normalizeCommand(tokens.get(0));
            return commands.keySet().stream()
                    .filter(name -> name.startsWith(prefix) && canUse(context, commands.get(name)))
                    .distinct()
                    .sorted()
                    .map(name -> "/" + name)
                    .limit(12)
                    .toList();
        }

        String commandName = normalizeCommand(tokens.get(0));
        Command command = commands.get(commandName);
        if (!canUse(context, command)) {
            return List.of();
        }
        String current = endsWithSpace ? "" : tokens.get(tokens.size() - 1);
        int argIndex = endsWithSpace ? tokens.size() - 1 : tokens.size() - 2;

        return switch (canonical(commandName)) {
            case "gamemode" -> matching(optionNames(GameMode.values()), current);
            case "difficulty" -> matching(optionNames(Difficulty.values()), current);
            case "time" -> timeSuggestions(tokens, argIndex, current);
            case "weather" -> matching(List.of("clear", "rain", "thunder"), current);
            case "locate" -> matching(List.of("stronghold", "fortress", "nether_fortress"), current);
            case "give" -> giveSuggestions(context, argIndex, current);
            case "tp", "kill", "clear", "spawnpoint", "msg" -> context == null ? List.of()
                    : matching(context.playerNames(), current);
            case "xp" -> matching(List.of("1", "5", "10", "30", "100", "1000"), current);
            case "regenchunks" -> matching(List.of("0", "1", "2", "3", "4"), current);
            case "whitelist" -> matching(List.of("on", "off", "list", "add", "remove", "reload"), current);
            case "banlist" -> matching(List.of("players", "ips"), current);
            default -> List.of();
        };
    }

    public List<String> commandNames() {
        return commands.keySet().stream().distinct().sorted().toList();
    }

    public List<String> commandNames(Context context) {
        return commands.keySet().stream()
                .filter(name -> canUse(context, commands.get(name)))
                .distinct()
                .sorted()
                .toList();
    }

    private void register(String name, String description, BiConsumer<List<String>, Context> executor,
            String... aliases) {
        Command command = new Command(name, description, executor);
        commands.put(name, command);
        for (String alias : aliases) {
            commands.put(alias, command);
        }
    }

    private void help(List<String> args, Context context) {
        if (!args.isEmpty() && !args.get(0).matches("\\d+")) {
            Command command = commands.get(normalizeCommand(args.get(0)));
            if (command == null) {
                throw usage("No help for command: " + args.get(0));
            }
            if (!canUse(context, command)) {
                throw usage(PERMISSION_DENIED);
            }
            context.feedback("/" + command.name + " - " + command.description);
            return;
        }

        List<String> names = commandNames(context);
        int pages = Math.max(1, (int) Math.ceil(names.size() / (double) HELP_PAGE_SIZE));
        int page = args.isEmpty() ? 1 : clamp(parseInt(args.get(0), 1), 1, pages);
        int start = (page - 1) * HELP_PAGE_SIZE;
        int end = Math.min(names.size(), start + HELP_PAGE_SIZE);
        context.feedback("Commands page " + page + "/" + pages + ": /" + String.join(", /", names.subList(start, end)));
    }

    private void list(List<String> args, Context context) {
        context.feedback("Connected players: " + String.join(", ", context.playerNames()));
    }

    private void me(List<String> args, Context context) {
        require(args, 1, "Usage: /me <action>");
        context.broadcast("* " + context.senderName() + " " + join(args, 0));
    }

    private void tell(List<String> args, Context context) {
        require(args, 2, "Usage: /tell <player> <message>");
        String target = args.get(0);
        String message = join(args, 1);
        String formatted = "[" + context.senderName() + " -> " + target + "] " + message;
        if (!isLocalPlayer(context, target)) {
            requireKnownPlayer(context, target);
            requireRemoteAction(context.sendPrivateMessage(context.senderName(), target, message), target);
        }
        context.feedback(formatted);
    }

    private void say(List<String> args, Context context) {
        require(args, 1, "Usage: /say <message>");
        context.broadcast("[Server] " + join(args, 0));
    }

    private void seed(List<String> args, Context context) {
        context.feedback("Seed: " + context.seed());
    }

    private void locate(List<String> args, Context context) {
        require(args, 1, "Usage: /locate <stronghold|fortress>");
        String type = args.get(0).toLowerCase(Locale.ROOT);
        String canonical = switch (type) {
            case "stronghold" -> "stronghold";
            case "fortress", "nether_fortress" -> "nether_fortress";
            default -> throw usage("Unknown structure: " + args.get(0));
        };
        StructureResult result = context.locateStructure(canonical);
        if (result == null) {
            context.feedback("Could not locate " + canonical + " in this dimension.");
            return;
        }
        context.feedback("Nearest " + canonical + " is at " + result.x() + ", " + result.y() + ", " + result.z());
    }

    private void time(List<String> args, Context context) {
        require(args, 1, "Usage: /time <set|add|query> <value>");
        String mode = args.get(0).toLowerCase(Locale.ROOT);
        if ("set".equals(mode)) {
            require(args, 2, "Usage: /time set <day|night|noon|midnight|ticks>");
            float value = switch (args.get(1).toLowerCase(Locale.ROOT)) {
                case "day" -> 1000.0f;
                case "noon" -> 6000.0f;
                case "night" -> 13000.0f;
                case "midnight" -> 18000.0f;
                default -> parseFloat(args.get(1), "Invalid time: " + args.get(1));
            };
            context.setTimeOfDay(value);
            context.feedback("Set the time to " + Math.round(context.timeOfDay()));
        } else if ("add".equals(mode)) {
            require(args, 2, "Usage: /time add <ticks>");
            context.setTimeOfDay(context.timeOfDay() + parseFloat(args.get(1), "Invalid time delta."));
            context.feedback("Added time. Current time is " + Math.round(context.timeOfDay()));
        } else if ("query".equals(mode)) {
            context.feedback("The time is " + Math.round(context.timeOfDay()));
        } else {
            throw usage("Usage: /time <set|add|query> <value>");
        }
    }

    private void gamemode(List<String> args, Context context) {
        require(args, 1, "Usage: /gamemode <survival|creative|hardcore|0|1|2>");
        GameMode mode = GameMode.fromName(args.get(0));
        context.setGameMode(mode);
        context.feedback("Set game mode to " + mode.displayName());
    }

    private void difficulty(List<String> args, Context context) {
        require(args, 1, "Usage: /difficulty <peaceful|easy|normal|hard>");
        Difficulty difficulty = Difficulty.fromName(args.get(0));
        context.setDifficulty(difficulty);
        context.feedback("Set difficulty to " + difficulty.displayName());
    }

    private void give(List<String> args, Context context) {
        require(args, 1, "Usage: /give [player] <item|id[:data]> [amount]");
        int itemIndex = 0;
        String target = null;
        if (args.size() >= 2 && isKnownPlayer(context, args.get(0))) {
            target = args.get(0);
            itemIndex = 1;
        }
        String itemName = args.get(itemIndex);
        ItemType item = parseItem(itemName)
                .orElseThrow(() -> usage("Unknown item: " + itemName));
        int count = args.size() > itemIndex + 1 ? parseInt(args.get(itemIndex + 1), item.getMaxStackSize())
                : item.getMaxStackSize();
        count = clamp(count, 1, Math.max(1, item.getMaxStackSize() * 64));
        ItemStack stack = new ItemStack(item, count);
        if (target != null && !isLocalPlayer(context, target)) {
            requireRemoteAction(context.addItemToPlayer(target, stack), target);
            context.feedback("Gave " + count + " " + item.getDisplayName() + " to " + target);
            return;
        }
        boolean fullyAdded = context.addItem(stack);
        context.feedback("Gave " + count + " " + item.getDisplayName() + (fullyAdded ? "" : " (inventory full)"));
    }

    private void teleport(List<String> args, Context context) {
        require(args, 3, "Usage: /tp [player] <x> <y> <z>");
        int coord = args.size() >= 4 ? 1 : 0;
        String target = coord == 1 ? args.get(0) : context.senderName();
        if (coord == 1) {
            requireKnownPlayer(context, target);
        }
        float x = parseCoordinate(args.get(coord), context.playerX(target));
        float y = parseCoordinate(args.get(coord + 1), context.playerY(target));
        float z = parseCoordinate(args.get(coord + 2), context.playerZ(target));
        if (!isLocalPlayer(context, target)) {
            requireRemoteAction(context.teleportPlayer(target, x, y, z), target);
            context.feedback("Teleported " + target + " to " + oneDecimal(x) + ", " + oneDecimal(y) + ", "
                    + oneDecimal(z));
            return;
        }
        context.teleport(x, y, z);
        context.feedback("Teleported to " + oneDecimal(x) + ", " + oneDecimal(y) + ", " + oneDecimal(z));
    }

    private void kill(List<String> args, Context context) {
        if (!args.isEmpty()) {
            requireKnownPlayer(context, args.get(0));
            if (!isLocalPlayer(context, args.get(0))) {
                requireRemoteAction(context.killPlayer(args.get(0)), args.get(0));
                context.feedback("Killed " + args.get(0) + ".");
                return;
            }
        }
        context.kill();
        context.feedback("Ouch. That looked like it hurt.");
    }

    private void clear(List<String> args, Context context) {
        ItemType filter = null;
        String target = context.senderName();
        if (!args.isEmpty()) {
            int itemIndex = 0;
            if (isKnownPlayer(context, args.get(0))) {
                target = args.get(0);
                itemIndex = 1;
            }
            if (args.size() > itemIndex) {
                String itemName = args.get(itemIndex);
                filter = parseItem(itemName)
                        .orElseThrow(() -> usage("Unknown item: " + itemName));
            }
        }
        if (!isLocalPlayer(context, target)) {
            requireRemoteAction(context.clearPlayerInventory(target, filter), target);
            context.feedback(filter == null ? "Cleared " + target + "'s inventory."
                    : "Cleared " + filter.getDisplayName() + " from " + target + ".");
            return;
        }
        context.clearInventory(filter);
        context.feedback(filter == null ? "Cleared inventory." : "Cleared " + filter.getDisplayName() + ".");
    }

    private void spawnpoint(List<String> args, Context context) {
        boolean hasExplicitTarget = !args.isEmpty() && isKnownPlayer(context, args.get(0));
        String target = hasExplicitTarget ? args.get(0) : context.senderName();
        int coord = hasExplicitTarget ? 1 : 0;
        if (hasExplicitTarget) {
            requireKnownPlayer(context, target);
        }
        boolean hasCoords = args.size() >= coord + 3;
        float x = hasCoords ? parseCoordinate(args.get(coord), context.playerX(target)) : context.playerX(target);
        float y = hasCoords ? parseCoordinate(args.get(coord + 1), context.playerY(target)) : context.playerY(target);
        float z = hasCoords ? parseCoordinate(args.get(coord + 2), context.playerZ(target)) : context.playerZ(target);
        if (!isLocalPlayer(context, target)) {
            requireRemoteAction(hasCoords
                    ? context.setPlayerSpawn(target, x, y, z)
                    : context.setPlayerSpawnToCurrentPosition(target), target);
            context.feedback("Set " + target + "'s spawn point to " + oneDecimal(x) + ", " + oneDecimal(y)
                    + ", " + oneDecimal(z));
            return;
        }
        context.setSpawn(x, y, z);
        context.feedback("Set spawn point to " + oneDecimal(x) + ", " + oneDecimal(y) + ", " + oneDecimal(z));
    }

    private void setworldspawn(List<String> args, Context context) {
        float x = args.size() >= 3 ? parseCoordinate(args.get(0), context.playerX()) : context.playerX();
        float y = args.size() >= 3 ? parseCoordinate(args.get(1), context.playerY()) : context.playerY();
        float z = args.size() >= 3 ? parseCoordinate(args.get(2), context.playerZ()) : context.playerZ();
        context.setWorldSpawn(Math.round(x), Math.round(y), Math.round(z));
        context.feedback("Set world spawn to " + Math.round(x) + ", " + Math.round(y) + ", " + Math.round(z));
    }

    private void xp(List<String> args, Context context) {
        require(args, 1, "Usage: /xp <amount>");
        int amount = parseInt(args.get(0).replace("L", ""), 0);
        if (amount <= 0) {
            throw usage("Experience amount must be positive.");
        }
        context.addExperience(amount);
        context.feedback("Added " + amount + " experience.");
    }

    private void weather(List<String> args, Context context) {
        require(args, 1, "Usage: /weather <clear|rain|thunder>");
        String weather = args.get(0).toLowerCase(Locale.ROOT);
        if (!List.of("clear", "rain", "thunder").contains(weather)) {
            throw usage("Usage: /weather <clear|rain|thunder>");
        }
        context.setWeather(weather);
        context.feedback("Set weather to " + weather + ".");
    }

    private void saveAll(List<String> args, Context context) {
        context.saveAll();
        context.feedback("Saved the world.");
    }

    private void saveOn(List<String> args, Context context) {
        context.setSavingEnabled(true);
        context.feedback("Enabled autosaving.");
    }

    private void saveOff(List<String> args, Context context) {
        context.setSavingEnabled(false);
        context.feedback("Disabled autosaving.");
    }

    private void regenChunks(List<String> args, Context context) {
        int radius = args.isEmpty() ? 1 : parseInt(args.get(0), 1);
        int regenerated = context.regenerateUnmodifiedChunks(Math.max(0, Math.min(4, radius)));
        context.feedback("Regenerated " + regenerated + " unmodified chunk"
                + (regenerated == 1 ? "" : "s") + " within radius " + Math.max(0, Math.min(4, radius)) + ".");
    }

    private void stop(List<String> args, Context context) {
        context.feedback("Stopping local host.");
        context.requestStop();
    }

    private void adminCommand(String name, List<String> args, Context context) {
        context.feedback(context.runServerAdminCommand(name, args));
    }

    private List<String> timeSuggestions(List<String> tokens, int argIndex, String current) {
        if (argIndex <= 0) {
            return matching(List.of("set", "add", "query"), current);
        }
        if (tokens.size() > 1 && "set".equalsIgnoreCase(tokens.get(1))) {
            return matching(List.of("day", "noon", "night", "midnight", "0", "1000", "6000", "13000", "18000"),
                    current);
        }
        return matching(List.of("0", "1000", "6000", "12000", "18000"), current);
    }

    private List<String> giveSuggestions(Context context, int argIndex, String current) {
        if (argIndex == 0 && context != null) {
            List<String> players = matching(context.playerNames(), current);
            if (!players.isEmpty()) {
                return players;
            }
        }
        return matching(itemNames(), current);
    }

    private static List<String> itemNames() {
        return Arrays.stream(ItemType.values())
                .map(item -> normalizeItemName(item.name()))
                .sorted()
                .toList();
    }

    private static List<String> optionNames(GameMode[] modes) {
        return Arrays.stream(modes).map(GameMode::optionName).toList();
    }

    private static List<String> optionNames(Difficulty[] difficulties) {
        return Arrays.stream(difficulties).map(Difficulty::optionName).toList();
    }

    private static List<String> matching(List<String> values, String prefix) {
        String normalized = normalizeItemName(prefix == null ? "" : prefix);
        return values.stream()
                .filter(value -> normalizeItemName(value).startsWith(normalized))
                .sorted(Comparator.naturalOrder())
                .limit(12)
                .toList();
    }

    private String canonical(String commandName) {
        Command command = commands.get(normalizeCommand(commandName));
        return command == null ? normalizeCommand(commandName) : command.name;
    }

    private static boolean canUse(Context context, Command command) {
        return command != null && context != null && context.hasPermission(command.name);
    }

    private static Optional<ItemType> parseItem(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("minecraft:")) {
            value = value.substring("minecraft:".length());
        }
        if (value.matches("\\d+(:\\d+)?")) {
            String[] parts = value.split(":");
            int id = parseInt(parts[0], -1);
            int data = parts.length > 1 ? parseInt(parts[1], 0) : 0;
            return Optional.ofNullable(ItemType.fromId(id, data));
        }

        String normalized = normalizeItemName(value);
        for (ItemType item : ItemType.values()) {
            if (normalizeItemName(item.name()).equals(normalized)
                    || normalizeItemName(item.getDisplayName()).equals(normalized)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    private static List<String> split(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.trim().split("\\s+")).filter(part -> !part.isEmpty()).toList();
    }

    private static List<String> splitForSuggestions(String raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(Arrays.asList(raw.split("\\s+")));
    }

    private static String normalizeCommand(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeItemName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace("minecraft:", "")
                .replace(' ', '_')
                .replace('-', '_');
    }

    private static void require(List<String> args, int count, String message) {
        if (args.size() < count) {
            throw usage(message);
        }
    }

    private static CommandException usage(String message) {
        return new CommandException(message);
    }

    private static String join(List<String> args, int start) {
        return String.join(" ", args.subList(start, args.size()));
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static float parseFloat(String raw, String message) {
        try {
            return Float.parseFloat(raw);
        } catch (RuntimeException ignored) {
            throw usage(message);
        }
    }

    private static float parseCoordinate(String raw, float base) {
        if (raw == null || raw.isBlank()) {
            throw usage("Invalid coordinate.");
        }
        if (raw.equals("~")) {
            return base;
        }
        if (raw.startsWith("~")) {
            return base + parseFloat(raw.substring(1), "Invalid coordinate: " + raw);
        }
        return parseFloat(raw, "Invalid coordinate: " + raw);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String oneDecimal(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static void requireRemoteAction(boolean completed, String target) {
        if (!completed) {
            throw usage("Could not reach player: " + target);
        }
    }

    private static void requireKnownPlayer(Context context, String target) {
        if (!isKnownPlayer(context, target)) {
            throw usage("No such player: " + target);
        }
    }

    private static boolean isKnownPlayer(Context context, String target) {
        return context != null && target != null && context.playerNames().stream()
                .anyMatch(name -> name.equalsIgnoreCase(target));
    }

    private static boolean isLocalPlayer(Context context, String target) {
        return context != null && target != null && context.senderName().equalsIgnoreCase(target);
    }

    private record Command(String name, String description, BiConsumer<List<String>, Context> executor) {
    }

    private static final class CommandException extends RuntimeException {
        private CommandException(String message) {
            super(message);
        }
    }

    public interface Context {
        String senderName();

        default boolean hasPermission(String command) {
            return true;
        }

        List<String> playerNames();

        long seed();

        float timeOfDay();

        void setTimeOfDay(float time);

        GameMode gameMode();

        void setGameMode(GameMode mode);

        Difficulty difficulty();

        void setDifficulty(Difficulty difficulty);

        float playerX();

        float playerY();

        float playerZ();

        default float playerX(String target) {
            return playerX();
        }

        default float playerY(String target) {
            return playerY();
        }

        default float playerZ(String target) {
            return playerZ();
        }

        void teleport(float x, float y, float z);

        default boolean teleportPlayer(String target, float x, float y, float z) {
            return false;
        }

        boolean addItem(ItemStack stack);

        default boolean addItemToPlayer(String target, ItemStack stack) {
            return false;
        }

        void clearInventory(ItemType filter);

        default boolean clearPlayerInventory(String target, ItemType filter) {
            return false;
        }

        void kill();

        default boolean killPlayer(String target) {
            return false;
        }

        void setSpawn(float x, float y, float z);

        default boolean setPlayerSpawn(String target, float x, float y, float z) {
            return false;
        }

        default boolean setPlayerSpawnToCurrentPosition(String target) {
            return false;
        }

        void setWorldSpawn(int x, int y, int z);

        void addExperience(int amount);

        void setWeather(String weather);

        void saveAll();

        void setSavingEnabled(boolean enabled);

        boolean savingEnabled();

        void requestStop();

        void feedback(String message);

        void broadcast(String message);

        default boolean sendPrivateMessage(String sender, String target, String message) {
            return false;
        }

        String runServerAdminCommand(String command, List<String> args);

        default int regenerateUnmodifiedChunks(int radiusChunks) {
            return 0;
        }

        default StructureResult locateStructure(String type) {
            return null;
        }
    }

    public record StructureResult(int x, int y, int z) {
    }
}

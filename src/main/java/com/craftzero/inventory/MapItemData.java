package com.craftzero.inventory;

import com.craftzero.world.BlockType;
import com.craftzero.world.Chunk;
import com.craftzero.world.World;
import com.craftzero.world.tile.BlockPos;

import java.util.Arrays;
import java.util.Base64;

/**
 * Release-era filled map state stored on CraftZero's item metadata.
 */
public final class MapItemData {
    public static final int MAP_SIZE = 128;
    public static final int DEFAULT_SCALE = 3;

    private static final int HELD_UPDATE_RADIUS = 8;
    private static final int USE_UPDATE_RADIUS = 16;
    private static final int UNKNOWN_COLOR = 0;
    private static final int SHADE_DARK = 0;
    private static final int SHADE_NORMAL = 1;
    private static final int SHADE_LIGHT = 2;
    private static final int SHADE_WATER_DARK = 3;

    private static final String KEY_INITIALIZED = "map.initialized";
    private static final String KEY_ID = "map.id";
    private static final String KEY_SCALE = "map.scale";
    private static final String KEY_CENTER_X = "map.centerX";
    private static final String KEY_CENTER_Z = "map.centerZ";
    private static final String KEY_DIMENSION = "map.dimension";
    private static final String KEY_COLORS = "map.colors";
    private static final String KEY_COLOR_FORMAT = "map.colorFormat";
    private static final String KEY_PLAYER_X = "map.playerX";
    private static final String KEY_PLAYER_Z = "map.playerZ";
    private static final String KEY_PLAYER_ROTATION = "map.playerRotation";
    private static final String COLOR_FORMAT_SHADED = "release-shaded";
    private static final String MAP_ID_PREFIX = "map_";

    private static final int[] PALETTE = {
            0xD7C9A2, // unknown parchment
            0x5F8A36, // grass
            0x8B6A3F, // dirt
            0xD8C887, // sand
            0x3F67B5, // water
            0x818181, // stone
            0x6B6B6B, // cobble/ore
            0x8A6B3F, // wood
            0x3D7F32, // leaves
            0xF2F2F2, // snow
            0x9CA3A7, // clay/ice
            0xD85D20, // lava/fire
            0x6B2B2B, // netherrack
            0x161616, // obsidian/bedrock/end portal
            0xA08A47, // farmland/wheat
            0x4A7A7C, // mycelium/end stone family
            0xB64A4A // redstone/brick
    };

    private MapItemData() {
    }

    public static boolean isMap(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getType() == ItemType.MAP;
    }

    public static boolean isInitializedMap(ItemStack stack) {
        if (!isMap(stack) || !"true".equals(stack.getMetadata().get(KEY_INITIALIZED))) {
            return false;
        }
        ensureMapIdentity(stack);
        return true;
    }

    public static ItemStack copyInitializedMap(ItemStack source, int count) {
        if (!isInitializedMap(source) || count <= 0) {
            return null;
        }
        ItemStack copy = source.copy();
        copy.setCount(count);
        return copy;
    }

    public static boolean useMap(World world, ItemStack stack, float playerX, float playerZ) {
        return useMap(world, stack, playerX, playerZ, 0.0f);
    }

    public static boolean useMap(World world, ItemStack stack, float playerX, float playerZ, float playerYaw) {
        if (!isMap(stack) || world == null) {
            return false;
        }
        update(world, stack, playerX, playerZ, playerYaw, USE_UPDATE_RADIUS);
        return true;
    }

    public static boolean updateHeldMap(World world, ItemStack stack, float playerX, float playerZ) {
        return updateHeldMap(world, stack, playerX, playerZ, 0.0f);
    }

    public static boolean updateHeldMap(World world, ItemStack stack, float playerX, float playerZ, float playerYaw) {
        if (!isMap(stack) || world == null) {
            return false;
        }
        return update(world, stack, playerX, playerZ, playerYaw, HELD_UPDATE_RADIUS);
    }

    public static View view(ItemStack stack) {
        return view(null, stack);
    }

    public static View view(World world, ItemStack stack) {
        if (!isMap(stack)) {
            return null;
        }
        if ("true".equals(stack.getMetadata().get(KEY_INITIALIZED))) {
            ensureMapIdentity(stack);
        }
        boolean initialized = "true".equals(stack.getMetadata().get(KEY_INITIALIZED));
        int scale = readInt(stack, KEY_SCALE, DEFAULT_SCALE);
        int centerX = readInt(stack, KEY_CENTER_X, 0);
        int centerZ = readInt(stack, KEY_CENTER_Z, 0);
        int dimension = readInt(stack, KEY_DIMENSION, 0);
        int playerPixelX = readInt(stack, KEY_PLAYER_X, -1);
        int playerPixelZ = readInt(stack, KEY_PLAYER_Z, -1);
        int playerRotation = readInt(stack, KEY_PLAYER_ROTATION, -1);
        return new View(initialized, centerX, centerZ, scale, dimension, playerPixelX, playerPixelZ, playerRotation,
                readSharedColorData(world, stack));
    }

    public static int rgbForPaletteIndex(int paletteIndex) {
        if (paletteIndex > 3) {
            int baseIndex = basePaletteIndex(paletteIndex);
            int shade = shadeIndex(paletteIndex);
            int rgb = PALETTE[baseIndex];
            int multiplier = switch (shade) {
                case SHADE_DARK -> 180;
                case SHADE_NORMAL -> 220;
                case SHADE_WATER_DARK -> 135;
                default -> 255;
            };
            int r = ((rgb >> 16) & 0xFF) * multiplier / 255;
            int g = ((rgb >> 8) & 0xFF) * multiplier / 255;
            int b = (rgb & 0xFF) * multiplier / 255;
            return (r << 16) | (g << 8) | b;
        }
        int index = Math.max(0, Math.min(PALETTE.length - 1, paletteIndex));
        return PALETTE[index];
    }

    public static int basePaletteIndex(int color) {
        int index = color > 3 ? color / 4 : color;
        return Math.max(0, Math.min(PALETTE.length - 1, index));
    }

    public static int shadeIndex(int color) {
        return color > 3 ? color & 3 : SHADE_LIGHT;
    }

    public static int mapRotation(float yawDegrees) {
        float normalized = yawDegrees % 360.0f;
        if (normalized < 0.0f) {
            normalized += 360.0f;
        }
        return (int) Math.floor(normalized * 16.0f / 360.0f + 0.5f) & 15;
    }

    private static boolean update(World world, ItemStack stack, float playerX, float playerZ, float playerYaw,
            int radius) {
        boolean changed = initializeIfNeeded(world, stack, playerX, playerZ);
        int scale = clampScale(readInt(stack, KEY_SCALE, DEFAULT_SCALE));
        int dimension = readInt(stack, KEY_DIMENSION, world.getDimension().getId());
        int centerX = readInt(stack, KEY_CENTER_X, (int) Math.floor(playerX));
        int centerZ = readInt(stack, KEY_CENTER_Z, (int) Math.floor(playerZ));
        byte[] colors = readSharedColorData(world, stack);

        if (dimension != world.getDimension().getId()) {
            changed |= putIfChanged(stack, KEY_PLAYER_X, "-1");
            changed |= putIfChanged(stack, KEY_PLAYER_Z, "-1");
            changed |= putIfChanged(stack, KEY_PLAYER_ROTATION, "-1");
            return changed;
        }

        int playerPixelX = mapPixel(playerX, centerX, scale);
        int playerPixelZ = mapPixel(playerZ, centerZ, scale);
        changed |= putIfChanged(stack, KEY_PLAYER_X, Integer.toString(playerPixelX));
        changed |= putIfChanged(stack, KEY_PLAYER_Z, Integer.toString(playerPixelZ));
        changed |= putIfChanged(stack, KEY_PLAYER_ROTATION, Integer.toString(mapRotation(playerYaw)));

        if (playerPixelX < 0 || playerPixelZ < 0 || playerPixelX >= MAP_SIZE || playerPixelZ >= MAP_SIZE) {
            return changed;
        }

        boolean colorsChanged = false;
        int minX = Math.max(0, playerPixelX - radius);
        int maxX = Math.min(MAP_SIZE - 1, playerPixelX + radius);
        int minZ = Math.max(0, playerPixelZ - radius);
        int maxZ = Math.min(MAP_SIZE - 1, playerPixelZ + radius);

        for (int x = minX; x <= maxX; x++) {
            float previousAverageY = previousPixelAverageY(world, centerX, centerZ, scale, x, minZ);
            for (int z = minZ; z <= maxZ; z++) {
                PixelSample sample = samplePixelArea(world, centerX, centerZ, scale, x, z);
                if (sample == null) {
                    continue;
                }
                int color = encodeColor(sample.baseColor(),
                        shadeForSample(sample, x, z, 1 << clampScale(scale), previousAverageY));
                if (color == UNKNOWN_COLOR) {
                    continue;
                }
                int index = x + z * MAP_SIZE;
                if ((colors[index] & 0xFF) != color) {
                    colors[index] = (byte) color;
                    colorsChanged = true;
                }
                previousAverageY = sample.averageY();
            }
        }

        if (colorsChanged) {
            writeColorData(world, stack, colors);
            changed = true;
        }
        return changed;
    }

    private static boolean initializeIfNeeded(World world, ItemStack stack, float playerX, float playerZ) {
        if ("true".equals(stack.getMetadata().get(KEY_INITIALIZED))) {
            return false;
        }
        int scale = DEFAULT_SCALE;
        BlockPos spawn = world.getWorldSpawn();
        int centerX = releaseMapCenter(spawn.x(), scale);
        int centerZ = releaseMapCenter(spawn.z(), scale);
        int dimension = world.getDimension().getId();
        boolean changed = false;
        changed |= putIfChanged(stack, KEY_INITIALIZED, "true");
        String id = world.allocateFilledMapId();
        changed |= putIfChanged(stack, KEY_ID, id);
        changed |= syncDamageFromMapId(stack, id);
        changed |= putIfChanged(stack, KEY_SCALE, Integer.toString(scale));
        changed |= putIfChanged(stack, KEY_CENTER_X, Integer.toString(centerX));
        changed |= putIfChanged(stack, KEY_CENTER_Z, Integer.toString(centerZ));
        changed |= putIfChanged(stack, KEY_DIMENSION, Integer.toString(dimension));
        if (!stack.getMetadata().containsKey(KEY_COLORS)) {
            writeColorData(world, stack, new byte[MAP_SIZE * MAP_SIZE]);
            changed = true;
        }
        return changed;
    }

    private static int releaseMapCenter(int coordinate, int scale) {
        int span = MAP_SIZE * (1 << clampScale(scale));
        return Math.round(coordinate / (float) span) * span;
    }

    private static PixelSample samplePixelArea(World world, int centerX, int centerZ, int scale, int pixelX,
            int pixelZ) {
        int blocksPerPixel = 1 << clampScale(scale);
        int startX = centerX + (pixelX - MAP_SIZE / 2) * blocksPerPixel;
        int startZ = centerZ + (pixelZ - MAP_SIZE / 2) * blocksPerPixel;
        return sampleArea(world, startX, startZ, blocksPerPixel);
    }

    private static float previousPixelAverageY(World world, int centerX, int centerZ, int scale, int pixelX,
            int firstPixelZ) {
        if (firstPixelZ <= 0) {
            return 0.0f;
        }
        PixelSample previous = samplePixelArea(world, centerX, centerZ, scale, pixelX, firstPixelZ - 1);
        return previous == null ? 0.0f : previous.averageY();
    }

    private static Surface surfaceAt(World world, int worldX, int worldZ) {
        if (!world.isChunkGeneratedForBlock(worldX, worldZ)) {
            return null;
        }
        for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
            BlockType type = world.getBlockIfLoaded(worldX, y, worldZ, BlockType.AIR);
            if (isIgnoredSurface(type)) {
                continue;
            }
            return new Surface(type, y);
        }
        return null;
    }

    private static PixelSample sampleArea(World world, int startX, int startZ, int blocksPerPixel) {
        int[] colorCounts = new int[PALETTE.length];
        int[] waterDepthSums = new int[PALETTE.length];
        int samples = 0;
        int heightSum = 0;

        for (int dz = 0; dz < blocksPerPixel; dz++) {
            for (int dx = 0; dx < blocksPerPixel; dx++) {
                Surface surface = surfaceAt(world, startX + dx, startZ + dz);
                if (surface == null) {
                    continue;
                }
                int color = colorForBlock(surface.type());
                if (color == UNKNOWN_COLOR) {
                    continue;
                }
                colorCounts[color]++;
                if (surface.type().isWater()) {
                    waterDepthSums[color] += waterDepthAt(world, startX + dx, startZ + dz, surface.y());
                }
                samples++;
                heightSum += surface.y();
            }
        }

        if (samples == 0) {
            return null;
        }

        int baseColor = UNKNOWN_COLOR;
        int bestCount = 0;
        for (int color = 1; color < colorCounts.length; color++) {
            if (colorCounts[color] > bestCount) {
                baseColor = color;
                bestCount = colorCounts[color];
            }
        }
        if (baseColor == UNKNOWN_COLOR) {
            return null;
        }

        float averageWaterDepth = bestCount == 0 ? 0.0f : waterDepthSums[baseColor] / (float) bestCount;
        return new PixelSample(baseColor, heightSum / (float) samples, averageWaterDepth);
    }

    private static int shadeForSample(PixelSample sample, int pixelX, int pixelZ, int blocksPerPixel,
            float previousAverageY) {
        if (sample.baseColor() == 4) {
            return waterShade(sample.averageWaterDepth(), pixelX, pixelZ);
        }
        float checker = (((pixelX + pixelZ) & 1) - 0.5f) * 0.4f;
        float relief = (sample.averageY() - previousAverageY) * 4.0f / (blocksPerPixel + 4.0f) + checker;
        if (relief > 0.6f) {
            return SHADE_LIGHT;
        }
        if (relief < -0.6f) {
            return SHADE_DARK;
        }
        return SHADE_NORMAL;
    }

    private static int waterDepthAt(World world, int worldX, int worldZ, int surfaceY) {
        int depth = 0;
        for (int y = surfaceY; y >= 0; y--) {
            BlockType type = world.getBlockIfLoaded(worldX, y, worldZ, BlockType.AIR);
            if (!type.isWater()) {
                break;
            }
            depth++;
        }
        return depth;
    }

    private static int waterShade(float averageDepth, int pixelX, int pixelZ) {
        float value = averageDepth * 0.1f + (((pixelX + pixelZ) & 1) == 0 ? 0.0f : 0.2f);
        if (value < 0.5f) {
            return SHADE_LIGHT;
        }
        if (value > 0.9f) {
            return SHADE_DARK;
        }
        return SHADE_NORMAL;
    }

    private static int encodeColor(int baseColor, int shade) {
        if (baseColor <= 0) {
            return UNKNOWN_COLOR;
        }
        return (baseColor << 2) | Math.max(0, Math.min(3, shade));
    }

    private static boolean isIgnoredSurface(BlockType type) {
        return type == null
                || type == BlockType.AIR
                || type == BlockType.FIRE
                || type == BlockType.TORCH
                || type == BlockType.REDSTONE_WIRE
                || type == BlockType.REDSTONE_TORCH_ON
                || type == BlockType.REDSTONE_TORCH_OFF
                || type == BlockType.STONE_BUTTON
                || type == BlockType.LEVER
                || type == BlockType.WOODEN_PRESSURE_PLATE
                || type == BlockType.STONE_PRESSURE_PLATE
                || type == BlockType.SAPLING
                || type == BlockType.TALL_GRASS
                || type == BlockType.DEAD_BUSH
                || type == BlockType.YELLOW_FLOWER
                || type == BlockType.RED_ROSE
                || type == BlockType.BROWN_MUSHROOM
                || type == BlockType.RED_MUSHROOM
                || type == BlockType.CROPS
                || type == BlockType.PUMPKIN_STEM
                || type == BlockType.MELON_STEM
                || type == BlockType.NETHER_WART
                || type == BlockType.SUGAR_CANE
                || type == BlockType.VINES
                || type == BlockType.LADDER
                || type == BlockType.STANDING_SIGN
                || type == BlockType.WALL_SIGN;
    }

    private static int colorForBlock(BlockType type) {
        return switch (type) {
            case GRASS -> 1;
            case DIRT -> 2;
            case SAND, SANDSTONE -> 3;
            case WATER, FLOWING_WATER, ICE -> 4;
            case STONE, STONE_BRICK, INFESTED_STONE, END_STONE -> 5;
            case COBBLESTONE, MOSSY_COBBLESTONE, GRAVEL, COAL_ORE, IRON_ORE, GOLD_ORE, DIAMOND_ORE, LAPIS_ORE,
                    REDSTONE_ORE, GLOWING_REDSTONE_ORE, FURNACE, LIT_FURNACE, DISPENSER, MOB_SPAWNER, CAULDRON,
                    BREWING_STAND, STONE_SLAB, DOUBLE_STONE_SLAB, COBBLESTONE_STAIRS,
                    STONE_BRICK_STAIRS, BRICK_STAIRS, NETHER_BRICK_STAIRS -> 6;
            case OAK_LOG, OAK_PLANKS, OAK_STAIRS, CHEST, CRAFTING_TABLE, BOOKSHELF, FENCE, FENCE_GATE, TRAPDOOR,
                    NOTE_BLOCK, JUKEBOX, BROWN_MUSHROOM_BLOCK, RED_MUSHROOM_BLOCK -> 7;
            case LEAVES, LILY_PAD, CACTUS -> 8;
            case SNOW, SNOW_LAYER -> 9;
            case CLAY, GLASS, GLASS_PANE, IRON_BARS -> 10;
            case LAVA, FLOWING_LAVA -> 11;
            case NETHERRACK, SOUL_SAND, NETHER_BRICK, NETHER_BRICK_FENCE -> 12;
            case BEDROCK, OBSIDIAN, PORTAL, END_PORTAL, END_PORTAL_FRAME, DRAGON_EGG -> 13;
            case FARMLAND -> 14;
            case MYCELIUM -> 15;
            case BRICK, TNT, REDSTONE_REPEATER_OFF, REDSTONE_REPEATER_ON -> 16;
            default -> type.isWater() ? 4 : type.isSolid() ? 5 : UNKNOWN_COLOR;
        };
    }

    private static int mapPixel(float coordinate, int center, int scale) {
        int blocksPerPixel = 1 << clampScale(scale);
        return (int) Math.floor((coordinate - center) / blocksPerPixel + MAP_SIZE / 2.0f);
    }

    private static byte[] readColorData(ItemStack stack) {
        String encoded = stack.getMetadata().get(KEY_COLORS);
        if (encoded == null || encoded.isBlank()) {
            return new byte[MAP_SIZE * MAP_SIZE];
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encoded);
            if (decoded.length == MAP_SIZE * MAP_SIZE) {
                return normalizeColorData(stack, decoded);
            }
        } catch (IllegalArgumentException ignored) {
            // Malformed metadata should not break rendering or held-item updates.
        }
        return new byte[MAP_SIZE * MAP_SIZE];
    }

    private static byte[] normalizeColorData(ItemStack stack, byte[] colors) {
        if (COLOR_FORMAT_SHADED.equals(stack.getMetadata().get(KEY_COLOR_FORMAT))) {
            return colors;
        }
        if (looksLikeShadedColorData(colors)) {
            stack.putMetadata(KEY_COLOR_FORMAT, COLOR_FORMAT_SHADED);
            return colors;
        }

        byte[] converted = new byte[colors.length];
        for (int i = 0; i < colors.length; i++) {
            int rawColor = colors[i] & 0xFF;
            if (rawColor <= 0) {
                converted[i] = 0;
                continue;
            }
            converted[i] = (byte) encodeColor(rawColor, SHADE_LIGHT);
        }
        stack.putMetadata(KEY_COLORS, Base64.getEncoder().encodeToString(converted));
        stack.putMetadata(KEY_COLOR_FORMAT, COLOR_FORMAT_SHADED);
        return converted;
    }

    private static boolean looksLikeShadedColorData(byte[] colors) {
        for (byte color : colors) {
            if ((color & 0xFF) > PALETTE.length - 1) {
                return true;
            }
        }
        return false;
    }

    private static byte[] readSharedColorData(World world, ItemStack stack) {
        byte[] colors = readColorData(stack);
        if (world == null || !isInitializedMap(stack)) {
            return colors;
        }
        String id = ensureMapIdentity(stack);
        byte[] shared = world.getOrCreateFilledMapColors(id, colors);
        if (shared == null || shared.length != MAP_SIZE * MAP_SIZE) {
            return colors;
        }
        if (!Arrays.equals(shared, colors)) {
            stack.putMetadata(KEY_COLORS, Base64.getEncoder().encodeToString(shared));
        }
        return shared;
    }

    private static void writeColorData(World world, ItemStack stack, byte[] colors) {
        if (colors == null || colors.length != MAP_SIZE * MAP_SIZE) {
            colors = new byte[MAP_SIZE * MAP_SIZE];
        }
        stack.putMetadata(KEY_COLORS, Base64.getEncoder().encodeToString(colors));
        stack.putMetadata(KEY_COLOR_FORMAT, COLOR_FORMAT_SHADED);
        if (world != null && isInitializedMap(stack)) {
            world.putFilledMapColors(ensureMapIdentity(stack), colors);
        }
    }

    private static String ensureMapIdentity(ItemStack stack) {
        String id = stack.getMetadata().get(KEY_ID);
        if (id != null && !id.isBlank()) {
            syncDamageFromMapId(stack, id);
            return id;
        }
        int damage = stack.getDurability();
        if (damage >= 0) {
            id = MAP_ID_PREFIX + damage;
            stack.putMetadata(KEY_ID, id);
            return id;
        }
        return null;
    }

    private static boolean syncDamageFromMapId(ItemStack stack, String id) {
        Integer numericId = numericMapId(id);
        if (numericId == null || stack.getDurability() == numericId) {
            return false;
        }
        stack.setDurability(numericId);
        return true;
    }

    private static Integer numericMapId(String id) {
        if (id == null || !id.startsWith(MAP_ID_PREFIX)) {
            return null;
        }
        try {
            int value = Integer.parseInt(id.substring(MAP_ID_PREFIX.length()));
            return value < 0 ? null : value;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static int readInt(ItemStack stack, String key, int fallback) {
        String value = stack.getMetadata().get(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static boolean putIfChanged(ItemStack stack, String key, String value) {
        if (value.equals(stack.getMetadata().get(key))) {
            return false;
        }
        stack.putMetadata(key, value);
        return true;
    }

    private static int clampScale(int scale) {
        return Math.max(0, Math.min(4, scale));
    }

    public record View(boolean initialized, int centerX, int centerZ, int scale, int dimension,
            int playerPixelX, int playerPixelZ, int playerRotation, byte[] colors) {
    }

    private record Surface(BlockType type, int y) {
    }

    private record PixelSample(int baseColor, float averageY, float averageWaterDepth) {
    }
}

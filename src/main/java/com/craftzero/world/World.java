package com.craftzero.world;

import com.craftzero.combat.DamageSource;
import com.craftzero.combat.ExplosionExposure;
import com.craftzero.graphics.Camera;
import com.craftzero.graphics.Frustum;
import com.craftzero.graphics.Mesh;
import com.craftzero.graphics.Renderer;
import com.craftzero.graphics.Texture;
import com.craftzero.entity.ArrowEntity;
import com.craftzero.entity.BoatEntity;
import com.craftzero.entity.ChestMinecartEntity;
import com.craftzero.entity.DroppedItem;
import com.craftzero.entity.EnderPearlEntity;
import com.craftzero.entity.EndCrystalEntity;
import com.craftzero.entity.Entity;
import com.craftzero.entity.ExperienceOrbEntity;
import com.craftzero.entity.FireballEntity;
import com.craftzero.entity.FallingBlockEntity;
import com.craftzero.entity.FurnaceMinecartEntity;
import com.craftzero.entity.LivingEntity;
import com.craftzero.entity.MinecartEntity;
import com.craftzero.entity.PaintingEntity;
import com.craftzero.entity.PrimedTntEntity;
import com.craftzero.entity.SplashPotionEntity;
import com.craftzero.entity.ThrownItemEntity;
import com.craftzero.entity.mob.Creeper;
import com.craftzero.entity.mob.EnderDragon;
import com.craftzero.entity.mob.Mob;
import com.craftzero.entity.mob.MobDefinition;
import com.craftzero.entity.mob.MobFactory;
import com.craftzero.entity.mob.Pig;
import com.craftzero.entity.mob.Silverfish;
import com.craftzero.entity.mob.ZombiePigman;
import com.craftzero.inventory.ItemType;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemStackOps;
import com.craftzero.main.CombatRules;
import com.craftzero.math.Noise;
import com.craftzero.physics.AABB;
import com.craftzero.progression.PotionData;
import com.craftzero.progression.PotionEffectResolver;
import com.craftzero.progression.StatusEffectInstance;
import com.craftzero.progression.StatusEffectVisuals;
import com.craftzero.save.SaveManager;
import com.craftzero.world.tile.BlockPos;
import com.craftzero.world.tile.ChestTileEntity;
import com.craftzero.world.tile.BrewingStandTileEntity;
import com.craftzero.world.tile.DispenserTileEntity;
import com.craftzero.world.tile.EnchantingTableTileEntity;
import com.craftzero.world.tile.FurnaceTileEntity;
import com.craftzero.world.tile.JukeboxTileEntity;
import com.craftzero.world.tile.MonsterSpawnerTileEntity;
import com.craftzero.world.tile.NoteBlockTileEntity;
import com.craftzero.world.tile.SignTileEntity;
import com.craftzero.world.tile.TileEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.joml.Vector3f;

/**
 * World manager that handles chunk loading, generation, and rendering.
 * Implements procedural terrain generation with biomes.
 */
public class World implements GeneratedStructureSink {

    private static final int MIN_RENDER_DISTANCE = 2;
    private static final int DEFAULT_RENDER_DISTANCE = 8;
    private static final int MAX_RENDER_DISTANCE = 12;
    private static final int MAX_MESH_UPLOADS_PER_FRAME = 1; // GPU uploads are the biggest main-thread spike.
    private static final int MAX_GENERATES_PER_FRAME = 1; // Smooth out expensive Release-era chunk generation.
    private static final int MAX_LIGHTINGS_PER_FRAME = 1; // Limit async lighting submits.
    private static final int MAX_MESHES_PER_FRAME = 1; // Limit async mesh building submits.
    private static final int MAX_PENDING_CHUNK_WORK = 2;
    private static final int MAX_CHUNK_SHELL_STEPS_PER_FRAME = 32;
    private static final int MAX_BLOCK_UPDATES_PER_TICK = 250;
    private static final int MAX_PARTICLES = 384;
    private static final int SPLASH_POTION_SPELL_PARTICLES = 100;
    private static final float TILE_DROP_OFFSET_RANGE = 0.8f;
    private static final float TILE_DROP_OFFSET_MIN = 0.1f;
    private static final int TILE_DROP_MIN_SPLIT = 10;
    private static final int TILE_DROP_RANDOM_SPLIT = 21;
    private static final float TILE_DROP_HORIZONTAL_VELOCITY = 0.05f;
    private static final float TILE_DROP_VERTICAL_VELOCITY = 0.2f;
    private static final int SNOWBALL_POOF_PARTICLES = 8;
    private static final int SNOW_GOLEM_CREATION_PARTICLES = 120;
    private static final int ENTITY_CRIT_EMITTER_TICKS = 3;
    private static final int ENTITY_CRIT_PARTICLES_PER_TICK = 8;
    private static final int ENTITY_CRIT_PARTICLE_ATTEMPT_LIMIT = 24;
    private static final float ENTITY_CRIT_POSITION_WIDTH_SCALE = 0.25f;
    private static final float ENTITY_CRIT_POSITION_HEIGHT_SCALE = 0.25f;
    private static final float ENTITY_CRIT_VERTICAL_MOTION_BONUS = 0.2f;
    private static final float ENTITY_CRIT_PARTICLE_BASE_SCALE = 0.14f;
    private static final float ENTITY_CRIT_PARTICLE_RANDOM_SCALE = 0.06f;
    private static final float ENTITY_PARTICLE_BURST_MIN_WIDTH = 0.2f;
    private static final float ENTITY_PARTICLE_BURST_MIN_HEIGHT = 0.2f;
    private static final float ENTITY_PARTICLE_BURST_MOTION_SCALE = 0.18f;
    private static final float ENTITY_PARTICLE_BURST_VERTICAL_BONUS = 0.10f;
    private static final int ENTITY_WATER_ENTRY_MIN_PARTICLES = 4;
    private static final int ENTITY_WATER_ENTRY_MAX_PARTICLES = 48;
    private static final float ENTITY_WATER_ENTRY_WIDTH_PARTICLES = 20.0f;
    private static final float ENTITY_WATER_ENTRY_IMPACT_PARTICLES = 10.0f;
    private static final float SPRINT_BLOCK_PARTICLE_MIN_SPEED_SQ = 0.0025f;
    private static final int SOURCE_DIGGING_TEXTURE_FACE = Block.FACE_BOTTOM;
    private static final float DROPPED_ITEM_MERGE_RADIUS_SQ = 2.25f;
    private static final float DROPPED_ITEM_PICKUP_SCAN_RADIUS_SQ = 9.0f;
    private static final float DROPPED_ITEM_HALF_SIZE = 0.125f;
    private static final int WATER_TICK_DELAY = 5;
    private static final int LAVA_TICK_DELAY = 30;
    private static final int FIRE_TICK_DELAY = 30;
    private static final int FIRE_HORIZONTAL_CATCH_CHANCE = 300;
    private static final int FIRE_VERTICAL_CATCH_CHANCE = 250;
    private static final int FIRE_AIR_SPREAD_BASE_CHANCE = 100;
    private static final int FIRE_AIR_SPREAD_VERTICAL_CHANCE_STEP = 100;
    private static final int FIRE_AIR_SPREAD_ENCOURAGEMENT_BONUS = 40;
    private static final int ENTITY_FIRE_CONTACT_TICKS = 160;
    private static final int ENTITY_LAVA_CONTACT_TICKS = 300;
    private static final float PLAYER_FIRE_CONTACT_DAMAGE = 1.0f;
    private static final float PLAYER_LAVA_CONTACT_DAMAGE = 4.0f;
    private static final float CACTUS_CONTACT_DAMAGE = 1.0f;
    private static final float SUFFOCATION_DAMAGE = 1.0f;
    private static final float DROPPED_ITEM_FIRE_DAMAGE = 1.0f;
    private static final float DROPPED_ITEM_LAVA_DAMAGE = 4.0f;
    private static final float VEHICLE_FIRE_CONTACT_DAMAGE = 1.0f;
    private static final float VEHICLE_LAVA_CONTACT_DAMAGE = 4.0f;
    private static final float DROPPED_ITEM_LAVA_BOUNCE_Y = 4.0f;
    private static final float DROPPED_ITEM_LAVA_BOUNCE_HORIZONTAL = 4.0f;
    private static final int FALLING_BLOCK_TICK_DELAY = 3;
    private static final int FARMLAND_TICK_DELAY = 20;
    private static final int CROP_TICK_DELAY = 20;
    private static final float PISTON_BLOCKED_ENTITY_DAMAGE = 1.0f;
    private static final int PLANT_GROWTH_TICK_DELAY = 20;
    private static final int GRASS_LIKE_TICK_DELAY = 200;
    private static final int GRASS_LIKE_SPREAD_ATTEMPTS = 4;
    private static final int GRASS_LIKE_DECAY_LIGHT = 4;
    private static final int GRASS_LIKE_SPREAD_SOURCE_LIGHT = 9;
    private static final int GRASS_LIKE_SPREAD_TARGET_LIGHT = 4;
    private static final int GRASS_LIKE_COVER_OPACITY_LIMIT = 2;
    private static final int GRASS_BONE_MEAL_ATTEMPTS = 128;
    private static final int VINE_TICK_DELAY = 200;
    private static final int VINE_DENSITY_RADIUS = 4;
    private static final int VINE_DENSITY_VERTICAL_RADIUS = 1;
    private static final int VINE_DENSITY_LIMIT = 5;
    private static final int SNOW_LAYER_TICK_DELAY = 20;
    private static final int ICE_TICK_DELAY = 20;
    private static final int ICE_LIGHT_OPACITY = 3;
    private static final int ICE_MELT_BLOCK_LIGHT_THRESHOLD = 11 - ICE_LIGHT_OPACITY;
    private static final int LEAF_DECAY_TICK_DELAY = 20;
    private static final int LEAF_DECAY_RADIUS = 4;
    private static final int LOCKED_CHEST_DECAY_TICK_DELAY = 1;
    private static final int REDSTONE_ORE_GLOW_TICK_DELAY = 30;
    private static final float REDSTONE_ORE_SPARKLE_FACE_OFFSET = 0.0625f;
    private static final float FARMLAND_TRAMPLE_MIN_FALL = 0.5f;
    private static final int DRAGON_EGG_TELEPORT_ATTEMPTS = 1000;
    private static final int DRAGON_EGG_HORIZONTAL_TELEPORT_BOUND = 16;
    private static final int DRAGON_EGG_VERTICAL_TELEPORT_BOUND = 8;
    private static final int DRAGON_EGG_PORTAL_PARTICLES = 128;
    private static final int NETHER_PORTAL_AMBIENT_SOUND_CHANCE = 100;
    private static final int NETHER_PORTAL_AMBIENT_PARTICLES = 4;
    private static final int AMBIENT_BLOCK_RANDOM_SAMPLES_PER_TICK = 128;
    private static final int AMBIENT_BLOCK_SAMPLE_BOUND = 16;
    private static final int AMBIENT_LAVA_PARTICLE_CHANCE = 100;
    private static final int AMBIENT_WATER_SUSPENDED_CHANCE = 10;
    private static final int AMBIENT_DEPTH_SUSPEND_CHANCE = 8;
    private static final int AMBIENT_MYCELIUM_TOWN_AURA_CHANCE = 10;
    private static final int CAVE_AMBIENT_SOUND_CHANCE = 5000;
    private static final int CAVE_AMBIENT_MIN_COOLDOWN_TICKS = 4000;
    private static final int CAVE_AMBIENT_RANDOM_COOLDOWN_TICKS = 6000;
    private static final int CAVE_AMBIENT_MIN_DISTANCE_SQ = 16;
    private static final int CAVE_AMBIENT_MAX_SKY_LIGHT = 0;
    private static final int CAVE_AMBIENT_MAX_BLOCK_LIGHT = 1;
    private static final int FIRE_TOP_SMOKE_PARTICLES = 3;
    private static final int FIRE_SIDE_SMOKE_PARTICLES = 2;
    private static final float FIRE_SIDE_SMOKE_INSET = 0.1f;
    private static final float TORCH_DISPLAY_Y_OFFSET = 0.7f;
    private static final float TORCH_DISPLAY_WALL_Y_OFFSET = 0.22f;
    private static final float TORCH_DISPLAY_WALL_OFFSET = 0.2700000107f;
    private static final float REDSTONE_TORCH_DISPLAY_JITTER = 0.2f;
    private static final float REDSTONE_REPEATER_DISPLAY_JITTER = 0.2f;
    private static final float REDSTONE_REPEATER_DISPLAY_Y_OFFSET = 0.4f;
    private static final float REDSTONE_REPEATER_OUTPUT_TORCH_OFFSET = 0.3125f;
    private static final float[] REDSTONE_REPEATER_REAR_TORCH_OFFSETS = {
            -0.0625f, 0.0625f, 0.1875f, 0.3125f
    };
    private static final int DEPTH_SUSPEND_START_Y = 17;
    private static final float PORTAL_PARTICLE_SCALE = 0.25f;
    private static final int PORTAL_PARTICLE_LIFETIME_TICKS = 40;
    private static final float PISTON_ENTITY_CLEARANCE = 0.001f;
    private static final int EXPLOSION_RAY_GRID = 16;
    private static final float EXPLOSION_RAY_STEP = 0.3f;
    private static final float EXPLOSION_RAY_AIR_ATTENUATION = EXPLOSION_RAY_STEP * 0.75f;
    private static final float EXPLOSION_MIN_RAY_POWER = 0.7f;
    private static final float EXPLOSION_RANDOM_RAY_POWER = 0.6f;
    private static final float EXPLOSION_ENTITY_RADIUS_MULTIPLIER = 2.0f;
    private static final float EXPLOSION_ENTITY_DAMAGE_SCALE = 8.0f;
    private static final float EXPLOSION_PARTICLE_MIN_SCALE = 1.0f;
    private static final float EXPLOSION_PARTICLE_SCALE_PER_POWER = 0.5f;
    private static final int EXPLOSION_PARTICLE_LIFETIME_TICKS = 16;
    private static final int EXPLOSION_DEBRIS_PARTICLE_LIMIT = 64;
    private static final float EXPLOSION_DEBRIS_VELOCITY_NUMERATOR = 0.5f;
    private static final float EXPLOSION_DEBRIS_DISTANCE_BIAS = 0.1f;
    private static final float EXPLOSION_DEBRIS_RANDOM_BIAS = 0.3f;
    private static final float EXPLOSION_FLASH_PARTICLE_BASE_SCALE = 0.20f;
    private static final float EXPLOSION_FLASH_PARTICLE_RANDOM_SCALE = 0.08f;
    private static final int EXPLOSION_FLASH_PARTICLE_BASE_LIFETIME_TICKS = 14;
    private static final int EXPLOSION_FLASH_PARTICLE_RANDOM_LIFETIME_TICKS = 8;
    private static final float EXPLOSION_SMOKE_PARTICLE_BASE_SCALE = 0.24f;
    private static final float EXPLOSION_SMOKE_PARTICLE_RANDOM_SCALE = 0.12f;
    private static final int EXPLOSION_SMOKE_PARTICLE_BASE_LIFETIME_TICKS = 18;
    private static final int EXPLOSION_SMOKE_PARTICLE_RANDOM_LIFETIME_TICKS = 10;
    private static final int WEATHER_CLEAR_MIN_TICKS = 12000;
    private static final int WEATHER_CLEAR_RANDOM_TICKS = 168000;
    private static final int RAIN_ACTIVE_MIN_TICKS = 12000;
    private static final int RAIN_ACTIVE_RANDOM_TICKS = 12000;
    private static final int THUNDER_ACTIVE_MIN_TICKS = 3600;
    private static final int THUNDER_ACTIVE_RANDOM_TICKS = 12000;
    private static final int LIGHTNING_STRIKE_CHANCE = 100000;
    private static final int LIGHTNING_CHUNK_LCG_MULTIPLIER = 3;
    private static final int LIGHTNING_CHUNK_LCG_INCREMENT = 1013904223;
    private static final int LIGHTNING_FIRE_EXTRA_ATTEMPTS = 4;
    private static final int LIGHTNING_ENTITY_FIRE_TICKS = 160;
    private static final float LIGHTNING_DAMAGE = 5.0f;
    private static final float LIGHTNING_ENTITY_RADIUS = 3.0f;
    private static final float LIGHTNING_FLASH_DECAY_PER_TICK = 0.25f;
    private static final int PRECIPITATION_EFFECT_RADIUS = 4;
    private static final int PRECIPITATION_PARTICLES_PER_TICK = 3;
    private static final int SNOW_ACCUMULATION_CHANCE = 32;
    private static final int WEATHER_WATER_FREEZE_CHANCE = 32;
    private static final int WEATHER_RAIN_SOUND_INTERVAL_TICKS = 20;
    private static final int NETHER_PORTAL_SEARCH_RADIUS = 128;
    private static final int NETHER_PORTAL_CREATE_RADIUS = 16;
    public static final int CAULDRON_MAX_LEVEL = 3;
    public static final int CAKE_MAX_BITES = 6;
    public static final int CAKE_LAST_BITE_METADATA = CAKE_MAX_BITES - 1;
    public static final int FARMLAND_MAX_MOISTURE = 7;
    public static final int MAX_CROP_AGE = 7;
    public static final int COLUMN_PLANT_MAX_AGE = 15;
    public static final int COLUMN_PLANT_MAX_HEIGHT = 3;
    public static final int NETHER_WART_MAX_AGE = 3;
    public static final int BED_OCCUPIED_BIT = 4;
    public static final int LEAF_PERSISTENT_BIT = 4;
    public static final int LEAF_CHECK_DECAY_BIT = 8;
    public static final float BED_EXPLOSION_POWER = 5.0f;
    public static final int END_PORTAL_FRAME_EYE_BIT = 4;
    private static final int END_PORTAL_FRAME_EYE_SMOKE_PARTICLES = 16;
    private static final float END_PORTAL_FRAME_EYE_SMOKE_MIN_OFFSET = 5.0f / 16.0f;
    private static final float END_PORTAL_FRAME_EYE_SMOKE_RANDOM_OFFSET = 6.0f / 16.0f;
    private static final float END_PORTAL_FRAME_EYE_SMOKE_Y_OFFSET = 0.8125f;
    private static final float BED_MONSTER_CHECK_HORIZONTAL_RANGE = 8.0f;
    private static final float BED_MONSTER_CHECK_VERTICAL_RANGE = 5.0f;
    private static final float BED_RESPAWN_HALF_WIDTH = 0.3f;
    private static final float BED_RESPAWN_HEIGHT = 1.8f;
    private static final int REDSTONE_TORCH_BURNOUT_SMOKE_PARTICLES = 5;
    private static final int NETHER_WATER_EVAPORATION_SMOKE_PARTICLES = 8;
    private static final int LAVA_MIX_SMOKE_PARTICLES = 8;
    private static final float LAVA_MIX_SMOKE_Y_OFFSET = 1.2f;
    private static final int PROJECTILE_WATER_TRAIL_BUBBLES = 4;
    private static final float PROJECTILE_WATER_TRAIL_BACKSTEP = 0.25f;
    private static final float PROJECTILE_WATER_TRAIL_BUBBLE_SCALE = 0.055f;
    private static final int PROJECTILE_WATER_TRAIL_BUBBLE_LIFETIME_TICKS = 8;
    private static final int SEA_LEVEL = 62;
    private static final int BASE_HEIGHT = 64;
    public static final float FLUID_CURRENT_PUSH_PER_TICK = 0.014f;
    private static final int[][] HORIZONTAL_DIRS = {
            { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 }
    };

    private final ConcurrentHashMap<Long, Chunk> chunks;
    private final Noise terrainNoise;
    private final Noise biomeNoise;
    private final Noise treeNoise;
    private final CaveGenerator caveGenerator;
    private final RavineGenerator ravineGenerator;
    private final OreGenerator oreGenerator;
    private final WorldGenerator worldGenerator;
    private final String generatorId;
    private final Dimension dimension;
    private final long seed;
    private final boolean generateStructures;
    private final Random random;
    private final Random displayRandom;
    private boolean releaseOneWorldGenCreatureRandomPrimed;
    private final PriorityQueue<ScheduledBlockTick> scheduledBlockTicks;
    private final CopyOnWriteArrayList<BlockChangeListener> blockChangeListeners;
    private final CopyOnWriteArrayList<DamageEventListener> damageEventListeners;
    private ProjectilePlayerInteractionHandler projectilePlayerInteractionHandler;
    private RemotePlayerInteractionHandler remotePlayerInteractionHandler;
    private final Set<ScheduledTickKey> scheduledTickKeys;
    private final ConcurrentHashMap<BlockPos, MovingPistonState> movingPistons;
    private final Set<Long> dynamicTickScannedChunks;
    private long blockTickClock;
    private long nextBlockTickSequence;
    private float blockTickAccumulator;
    private float ambientBlockEffectAccumulator;
    private float critParticleEmitterAccumulator;
    private boolean chunkShellReady;
    private int chunkShellCenterX;
    private int chunkShellCenterZ;
    private int chunkShellCursor;
    private int renderDistance = DEFAULT_RENDER_DISTANCE;
    private int unloadDistance = DEFAULT_RENDER_DISTANCE + 2;
    private boolean smoothLighting = true;
    private boolean fancyGraphics = true;
    private boolean advancedOpenGl;
    private boolean spawnNpcs = true;

    private Texture atlas;
    private SaveManager saveManager;
    private boolean suppressNeighborSupportUpdates;
    private int worldSpawnX;
    private int worldSpawnY = 80;
    private int worldSpawnZ;

    // Dropped items in the world
    private final List<DroppedItem> droppedItems;
    private final List<WorldParticle> particles;
    private final List<WorldParticle> particleEvents;
    private final List<CritParticleEmitter> critParticleEmitters;
    private final List<WorldLightningBolt> lightningBolts;
    private final List<WorldLightningBolt> lightningEvents;
    private final List<WorldSoundEvent> soundEvents;
    private final ConcurrentHashMap<BlockPos, TileEntity> tileEntities;
    private final ConcurrentHashMap<BlockPos, TileEntity> generatedTileEntities;
    private final ConcurrentHashMap<String, byte[]> filledMapColors;
    private int nextFilledMapId;

    // Living entities (mobs)
    private final List<Entity> entities;
    private final List<Entity> entitiesToAdd;
    private final List<Entity> entitiesToRemove;
    private final ConcurrentLinkedQueue<Entity> generatedEntities;

    // Reference to player (for AI targeting)
    private com.craftzero.main.Player player;

    // Day/night cycle manager reference
    private DayCycleManager dayCycleManager;
    private String weatherState = "clear";
    private boolean raining;
    private boolean thundering;
    private int rainTime;
    private int thunderTime;
    private float prevRainStrength;
    private float rainStrength;
    private float prevThunderStrength;
    private float thunderStrength;
    private float prevLightningFlashStrength;
    private float lightningFlashStrength;
    private float weatherTickAccumulator;
    private int weatherAmbientCooldownTicks;
    private int caveAmbientCooldownTicks;
    private int lightningUpdateLcg;

    // Async mesh building infrastructure
    private final ExecutorService meshBuildPool;
    private final ExecutorService chunkSavePool;
    private final Set<Long> chunksBeingBuilt; // Chunks currently being processed
    private final ConcurrentLinkedQueue<ChunkMeshTask> completedMeshTasks;

    // Frustum culling
    private final Frustum frustum;
    private final org.joml.Matrix4f viewProjection;
    private final List<Chunk> visibleRenderChunks;

    // Pre-calculated spiral loading order (center-out, sorted by distance)
    private static final int[][] SPIRAL_OFFSETS;
    static {
        // Generate offsets for render distance
        int maxDist = MAX_RENDER_DISTANCE + 2;
        java.util.List<int[]> offsets = new java.util.ArrayList<>();
        for (int dx = -maxDist; dx <= maxDist; dx++) {
            for (int dz = -maxDist; dz <= maxDist; dz++) {
                offsets.add(new int[] { dx, dz, dx * dx + dz * dz }); // Store distance squared
            }
        }
        // Sort by distance (closest first)
        offsets.sort((a, b) -> Integer.compare(a[2], b[2]));
        SPIRAL_OFFSETS = offsets.toArray(new int[0][]);
    }

    // Task class to hold chunk and its built mesh data
    private static class ChunkMeshTask {
        final Chunk chunk;
        final ChunkMeshData meshData;
        final long expectedVersion;
        final Chunk.ChunkState fallbackState;

        ChunkMeshTask(Chunk chunk, ChunkMeshData meshData, long expectedVersion, Chunk.ChunkState fallbackState) {
            this.chunk = chunk;
            this.meshData = meshData;
            this.expectedVersion = expectedVersion;
            this.fallbackState = fallbackState;
        }
    }

    private record ScheduledTickKey(int x, int y, int z, BlockType type) {
    }

    private record ScheduledBlockTick(long dueTick, long sequence, ScheduledTickKey key)
            implements Comparable<ScheduledBlockTick> {
        @Override
        public int compareTo(ScheduledBlockTick other) {
            int dueCompare = Long.compare(dueTick, other.dueTick);
            return dueCompare != 0 ? dueCompare : Long.compare(sequence, other.sequence);
        }
    }

    public record MovingPistonState(int x, int y, int z, int facing,
            BlockType carriedType, int carriedMetadata,
            BlockType finalType, int finalMetadata,
            float fromX, float fromY, float fromZ,
            float toX, float toY, float toZ,
            long startTick, boolean restoredFromSave) {
        public float progress(long tick) {
            return Math.min(1.0f, Math.max(0.0f,
                    (tick - startTick) / (float) RedstoneEngine.PISTON_MOVEMENT_TICKS));
        }
    }

    public record ScheduledBlockTickState(int x, int y, int z, BlockType type, int delayTicks) {
    }

    public record ChunkAreaProgress(int total, int generated, int lighted, int readyChunks) {
        public float progress() {
            if (total <= 0) {
                return 1.0f;
            }
            int weighted = generated + lighted + readyChunks;
            return Math.min(1.0f, weighted / (float) (total * 3));
        }

        public boolean isReady() {
            return total > 0 && readyChunks >= total;
        }
    }

    private record CritParticleEmitter(WorldParticle.Type type, LivingEntity target, int ticksRemaining) {
    }

    public World(long seed) {
        this(seed, WorldGenerator.RELEASE_ONE);
    }

    public World(long seed, String generatorId) {
        this(seed, generatorId, null);
    }

    public World(long seed, String generatorId, Dimension dimension) {
        this(seed, generatorId, dimension, true);
    }

    public World(long seed, String generatorId, Dimension dimension, boolean generateStructures) {
        this.seed = seed;
        this.generateStructures = generateStructures;
        String requestedGeneratorId = generatorId == null || generatorId.isBlank()
                ? WorldGenerator.RELEASE_ONE
                : generatorId;
        this.worldGenerator = WorldGenerators.create(requestedGeneratorId, seed, dimension, generateStructures);
        this.dimension = worldGenerator != null ? worldGenerator.getDimension() : Dimension.OVERWORLD;
        this.generatorId = worldGenerator != null ? worldGenerator.getId() : requestedGeneratorId;
        this.chunks = new ConcurrentHashMap<>();
        this.terrainNoise = new Noise(seed);
        this.biomeNoise = new Noise(seed + 1);
        this.treeNoise = new Noise(seed + 3);
        this.caveGenerator = new CaveGenerator();
        this.ravineGenerator = new RavineGenerator();
        this.oreGenerator = new OreGenerator();
        this.random = new Random(seed);
        this.displayRandom = new Random(seed ^ 0x5DEECE66DL);
        this.lightningUpdateLcg = (int) seed;
        applyWeatherState(weatherState);
        resetWeatherTimersForState(weatherState);
        this.scheduledBlockTicks = new PriorityQueue<>();
        this.blockChangeListeners = new CopyOnWriteArrayList<>();
        this.damageEventListeners = new CopyOnWriteArrayList<>();
        this.scheduledTickKeys = ConcurrentHashMap.newKeySet();
        this.movingPistons = new ConcurrentHashMap<>();
        this.dynamicTickScannedChunks = ConcurrentHashMap.newKeySet();
        this.blockTickClock = 0;
        this.nextBlockTickSequence = 0;
        this.blockTickAccumulator = 0.0f;
        this.ambientBlockEffectAccumulator = 0.0f;
        this.caveAmbientCooldownTicks = 0;
        this.critParticleEmitterAccumulator = 0.0f;
        this.chunkShellReady = false;
        this.chunkShellCenterX = Integer.MIN_VALUE;
        this.chunkShellCenterZ = Integer.MIN_VALUE;
        this.chunkShellCursor = 0;
        this.droppedItems = new ArrayList<>();
        this.particles = new ArrayList<>();
        this.particleEvents = new ArrayList<>();
        this.critParticleEmitters = new ArrayList<>();
        this.lightningBolts = new ArrayList<>();
        this.lightningEvents = new ArrayList<>();
        this.soundEvents = new ArrayList<>();
        this.tileEntities = new ConcurrentHashMap<>();
        this.generatedTileEntities = new ConcurrentHashMap<>();
        this.filledMapColors = new ConcurrentHashMap<>();
        this.nextFilledMapId = 0;
        this.entities = new ArrayList<>();
        this.entitiesToAdd = new ArrayList<>();
        this.entitiesToRemove = new ArrayList<>();
        this.generatedEntities = new ConcurrentLinkedQueue<>();

        int workerCount = Math.max(1, Math.min(2, Runtime.getRuntime().availableProcessors() - 1));
        this.meshBuildPool = Executors.newFixedThreadPool(workerCount, r -> {
            Thread t = new Thread(r, "ChunkMeshBuilder");
            t.setDaemon(true);
            return t;
        });
        this.chunkSavePool = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ChunkSaveWorker");
            t.setDaemon(true);
            return t;
        });
        this.chunksBeingBuilt = ConcurrentHashMap.newKeySet();
        this.completedMeshTasks = new ConcurrentLinkedQueue<>();

        // Frustum culling
        this.frustum = new Frustum();
        this.viewProjection = new org.joml.Matrix4f();
        this.visibleRenderChunks = new ArrayList<>();
    }

    public void init() throws Exception {
        atlas = new Texture("/textures/terrain/Terrain.png");
        System.out.println("World initialized with seed: " + seed);
    }

    public void setRenderDistanceChunks(int chunks) {
        int clamped = Math.max(MIN_RENDER_DISTANCE, Math.min(MAX_RENDER_DISTANCE, chunks));
        if (clamped == renderDistance) {
            return;
        }
        renderDistance = clamped;
        unloadDistance = renderDistance + 2;
        chunkShellReady = false;
        chunkShellCursor = 0;
    }

    public int getRenderDistanceChunks() {
        return renderDistance;
    }

    public void setSmoothLighting(boolean smoothLighting) {
        if (this.smoothLighting == smoothLighting) {
            return;
        }
        this.smoothLighting = smoothLighting;
        ChunkMeshBuilder.setSmoothLightingEnabled(smoothLighting);
        for (Chunk chunk : chunks.values()) {
            chunk.setDirty(true);
        }
    }

    public void setFancyGraphics(boolean fancyGraphics) {
        if (this.fancyGraphics == fancyGraphics) {
            return;
        }
        this.fancyGraphics = fancyGraphics;
        BlockType.setFancyGraphics(fancyGraphics);
        for (Chunk chunk : chunks.values()) {
            chunk.setDirty(true);
        }
    }

    public void setAdvancedOpenGl(boolean advancedOpenGl) {
        this.advancedOpenGl = advancedOpenGl;
    }

    public int regenerateUnmodifiedChunksAround(float worldX, float worldZ, int radiusChunks) {
        int radius = Math.max(0, Math.min(4, radiusChunks));
        int centerChunkX = (int) Math.floor(worldX / Chunk.WIDTH);
        int centerChunkZ = (int) Math.floor(worldZ / Chunk.DEPTH);
        int regenerated = 0;
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int chunkX = centerChunkX + dx;
                int chunkZ = centerChunkZ + dz;
                long key = chunkKey(chunkX, chunkZ);
                Chunk existing = chunks.get(key);
                if (existing != null && existing.isModified()) {
                    continue;
                }
                Chunk chunk = existing == null ? createChunk(chunkX, chunkZ) : existing;
                if (existing == null) {
                    chunks.put(key, chunk);
                }
                chunksBeingBuilt.remove(key);
                chunk.cleanup();
                chunk.setState(Chunk.ChunkState.GENERATING);
                generateChunkTerrain(chunk, chunkX, chunkZ);
                chunk.setState(Chunk.ChunkState.GENERATED);
                reconcileTileEntitiesInChunk(chunk);
                scheduleTickableBlocksInChunk(chunk);
                if (chunk.hasAllNeighborsAtLeast(Chunk.ChunkState.GENERATED)) {
                    chunk.calculateSkyLight();
                    chunk.setState(Chunk.ChunkState.LIGHTED);
                }
                chunk.setDirty(true);
                regenerated++;
                refreshChunkAndNeighbors(chunkX, chunkZ);
            }
        }
        return regenerated;
    }

    public ReleaseOneWorldGenerator.SpawnPoint findSafeSpawn() {
        if (worldGenerator instanceof ReleaseOneWorldGenerator releaseOne) {
            return releaseOne.findSafeSpawn();
        }
        return new ReleaseOneWorldGenerator.SpawnPoint(0, 80, 0);
    }

    public void setWorldSpawn(int x, int y, int z) {
        this.worldSpawnX = x;
        this.worldSpawnY = y;
        this.worldSpawnZ = z;
    }

    public BlockPos getWorldSpawn() {
        return new BlockPos(worldSpawnX, worldSpawnY, worldSpawnZ);
    }

    public void setSaveManager(SaveManager saveManager) {
        this.saveManager = saveManager;
    }

    public synchronized String allocateFilledMapId() {
        String id;
        do {
            id = "map_" + nextFilledMapId++;
        } while (filledMapColors.containsKey(id));
        return id;
    }

    public synchronized int getNextFilledMapId() {
        return nextFilledMapId;
    }

    public synchronized void reserveFilledMapIdsUpTo(int nextId) {
        nextFilledMapId = Math.max(nextFilledMapId, Math.max(0, nextId));
    }

    public byte[] getFilledMapColors(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        byte[] colors = filledMapColors.get(id);
        return colors == null ? null : colors.clone();
    }

    public byte[] getOrCreateFilledMapColors(String id, byte[] fallbackColors) {
        if (id == null || id.isBlank() || fallbackColors == null) {
            return fallbackColors == null ? null : fallbackColors.clone();
        }
        byte[] initial = fallbackColors.clone();
        byte[] stored = filledMapColors.putIfAbsent(id, initial);
        reserveFilledMapId(id);
        return (stored == null ? initial : stored).clone();
    }

    public void putFilledMapColors(String id, byte[] colors) {
        if (id == null || id.isBlank() || colors == null) {
            return;
        }
        filledMapColors.put(id, colors.clone());
        reserveFilledMapId(id);
    }

    public Map<String, byte[]> getFilledMapColorsSnapshot() {
        Map<String, byte[]> snapshot = new HashMap<>();
        for (Map.Entry<String, byte[]> entry : filledMapColors.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                snapshot.put(entry.getKey(), entry.getValue().clone());
            }
        }
        return snapshot;
    }

    public void replaceFilledMapColors(Map<String, byte[]> maps) {
        filledMapColors.clear();
        nextFilledMapId = 0;
        if (maps == null) {
            return;
        }
        for (Map.Entry<String, byte[]> entry : maps.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                filledMapColors.put(entry.getKey(), entry.getValue().clone());
                reserveFilledMapId(entry.getKey());
            }
        }
    }

    private synchronized void reserveFilledMapId(String id) {
        if (id == null || !id.startsWith("map_")) {
            return;
        }
        try {
            int numericId = Integer.parseInt(id.substring(4));
            reserveFilledMapIdsUpTo(numericId + 1);
        } catch (NumberFormatException ignored) {
            // Legacy metadata used descriptive ids; only numeric map ids advance the allocator.
        }
    }

    /**
     * Generate a unique key for chunk coordinates.
     */
    public static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    /**
     * Get chunk at the specified coordinates, creating if needed.
     * Does NOT generate terrain immediately - that happens async in update().
     */
    public Chunk getChunk(int chunkX, int chunkZ) {
        long key = chunkKey(chunkX, chunkZ);
        Chunk chunk = chunks.get(key);

        if (chunk == null) {
            chunk = createChunk(chunkX, chunkZ);
            chunks.put(key, chunk);
            scanCreatedChunkForDynamicTicks(key, chunk);
        }

        return chunk;
    }

    /**
     * Get chunk with immediate terrain generation.
     * Used for block queries/edits that need data NOW.
     */
    public Chunk getChunkNow(int chunkX, int chunkZ) {
        long key = chunkKey(chunkX, chunkZ);
        Chunk chunk = chunks.get(key);

        if (chunk == null) {
            chunk = createChunk(chunkX, chunkZ);
            chunks.put(key, chunk);
            scanCreatedChunkForDynamicTicks(key, chunk);
        }

        // Force synchronous generation if needed
        if (chunk.getState() == Chunk.ChunkState.EMPTY) {
            chunk.setState(Chunk.ChunkState.GENERATING);
            generateChunkTerrain(chunk, chunkX, chunkZ);
            chunk.setState(Chunk.ChunkState.GENERATED);
            reconcileTileEntitiesInChunk(chunk);
        }

        return chunk;
    }

    private void scanCreatedChunkForDynamicTicks(long key, Chunk chunk) {
        if (chunk.getState().ordinal() >= Chunk.ChunkState.GENERATED.ordinal() && dynamicTickScannedChunks.add(key)) {
            scheduleTickableBlocksInChunk(chunk);
        }
    }

    public ChunkAreaProgress getChunkAreaProgress(float worldX, float worldZ, int radius) {
        int centerChunkX = (int) Math.floor(worldX / Chunk.WIDTH);
        int centerChunkZ = (int) Math.floor(worldZ / Chunk.DEPTH);
        int total = 0;
        int generated = 0;
        int lighted = 0;
        int ready = 0;
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                total++;
                Chunk chunk = chunks.get(chunkKey(centerChunkX + dx, centerChunkZ + dz));
                if (chunk == null) {
                    continue;
                }
                Chunk.ChunkState state = chunk.getState();
                if (state.ordinal() >= Chunk.ChunkState.GENERATED.ordinal()) {
                    generated++;
                }
                if (state.ordinal() >= Chunk.ChunkState.LIGHTED.ordinal()) {
                    lighted++;
                }
                if (state == Chunk.ChunkState.READY && !chunk.isEmpty()) {
                    ready++;
                }
            }
        }
        return new ChunkAreaProgress(total, generated, lighted, ready);
    }

    private Chunk createChunk(int chunkX, int chunkZ) {
        Chunk chunk = new Chunk(chunkX, chunkZ);
        if (saveManager != null && saveManager.loadChunkIfExists(chunk, dimension)) {
            chunk.setState(Chunk.ChunkState.GENERATED);
            reconcileTileEntitiesInChunk(chunk);
        }
        return chunk;
    }

    private void generateChunkTerrain(Chunk chunk, int chunkX, int chunkZ) {
        if (worldGenerator != null) {
            worldGenerator.generateChunk(this, chunk, chunkX, chunkZ);
            return;
        }

        // Renamed from generateChunk to generateChunkTerrain

        int worldX = chunkX * Chunk.WIDTH;
        int worldZ = chunkZ * Chunk.DEPTH;

        // Pass 1: Local Terrain (No dependency on neighbors)
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                int globalX = worldX + x;
                int globalZ = worldZ + z;

                // Get biome value
                double biomeValue = biomeNoise.octaveNoise2D(globalX * 0.005, globalZ * 0.005, 4, 0.5);
                int height = calculateHeight(globalX, globalZ, biomeValue);
                BiomeType biome = getBiome(biomeValue);

                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    BlockType blockType = getBlockType(y, height, biome, globalX, globalZ);
                    // Cave generation

                    chunk.setBlock(x, y, z, blockType);
                }
            }
        }

        // Pass 1.5: Caves (Worm/Runner based)
        caveGenerator.generate(chunk, seed);

        // Pass 1.6: Ravines (Vertical Cracks)
        ravineGenerator.generate(chunk, seed);

        // Pass 1.7: Ores (Replace stone with ore clusters)
        oreGenerator.generate(chunk, seed);

        // Pass 2: Trees within this chunk
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                int globalX = worldX + x;
                int globalZ = worldZ + z;
                generateTreeIfPresent(chunk, globalX, globalZ);
            }
        }

        // Pass 3: Check for trees in neighboring chunks (within 2 blocks of border)
        // that would have leaves extending into this chunk
        int leafRadius = 2; // Max leaf radius
        for (int x = -leafRadius; x < Chunk.WIDTH + leafRadius; x++) {
            for (int z = -leafRadius; z < Chunk.DEPTH + leafRadius; z++) {
                // Skip positions inside this chunk (already handled)
                if (x >= 0 && x < Chunk.WIDTH && z >= 0 && z < Chunk.DEPTH)
                    continue;

                int globalX = worldX + x;
                int globalZ = worldZ + z;
                generateTreeIfPresent(chunk, globalX, globalZ);
            }
        }

        chunk.clearModified();
    }

    /**
     * Check if a tree exists at this global position and generate its parts in the
     * given chunk.
     */
    private void generateTreeIfPresent(Chunk chunk, int globalX, int globalZ) {
        double biomeValue = biomeNoise.octaveNoise2D(globalX * 0.005, globalZ * 0.005, 4, 0.5);
        int height = calculateHeight(globalX, globalZ, biomeValue);
        BiomeType biome = getBiome(biomeValue);

        // Trees only spawn in forest biome, above sea level, and on grass (not beach
        // sand)
        // Beach is height <= SEA_LEVEL + 2, so we need height > SEA_LEVEL + 2 for grass
        if (biome == BiomeType.FOREST && height > SEA_LEVEL + 2) {
            double treeValue = treeNoise.noise2D(globalX * 0.5, globalZ * 0.5);
            if (treeValue > 0.7 && isLocalMaximum(globalX, globalZ, 8)) {
                // Deterministic height based on position
                int trunkHeight = 5 + getPositionHash(globalX, globalZ) % 3;
                generateTreeAtPosition(chunk, globalX, height + 1, globalZ, trunkHeight);
            }
        }
    }

    /**
     * Generate a deterministic hash for a position (for random-like but
     * reproducible values).
     */
    private int getPositionHash(int x, int z) {
        long hash = seed ^ (x * 73856093L) ^ (z * 19349663L);
        hash = hash ^ (hash >>> 16);
        return Math.abs((int) hash);
    }

    /**
     * Checks if the noise value at (x,z) is higher than all neighbors in radius.
     */
    private boolean isLocalMaximum(int x, int z, int radius) {
        double centerValue = treeNoise.noise2D(x * 0.5, z * 0.5);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx == 0 && dz == 0)
                    continue;
                double neighborValue = treeNoise.noise2D((x + dx) * 0.5, (z + dz) * 0.5);
                if (neighborValue >= centerValue) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Generate tree parts at position (only places blocks within the target chunk).
     */
    private void generateTreeAtPosition(Chunk chunk, int x, int y, int z, int trunkHeight) {
        // x, z are GLOBAL coordinates

        // Trunk - only in this chunk's bounds
        for (int i = 0; i < trunkHeight; i++) {
            setBlockInChunk(chunk, x, y + i, z, BlockType.OAK_LOG);
        }

        // Leaves - Fuller & Rounded pattern (only within chunk bounds)
        int h = trunkHeight;

        // Loop: Bottom (h-2), Middle (h-1), Top (h), Peak (h+1)
        for (int ly = y + h - 2; ly <= y + h + 1; ly++) {
            int dy = ly - (y + h);
            int radius;

            // Shape logic - MC Oak style
            if (dy == 1)
                radius = 1; // Peak (Cross)
            else if (dy == 0)
                radius = 1; // Top body (3x3)
            else
                radius = 2; // Bottom body (5x5)

            for (int lx = x - radius; lx <= x + radius; lx++) {
                for (int lz = z - radius; lz <= z + radius; lz++) {
                    int dx = Math.abs(lx - x);
                    int dz = Math.abs(lz - z);
                    boolean place = false;

                    if (dy == 1) { // Peak: Cross
                        if (dx + dz <= 1)
                            place = true;
                    } else if (dy == 0) { // Top: 3x3
                        if (dx <= 1 && dz <= 1)
                            place = true;
                    } else { // Bottom: 5x5 minus corners
                        if (!(dx == 2 && dz == 2))
                            place = true;
                    }

                    if (!place)
                        continue;

                    // Only place if within chunk AND not overwriting trunk
                    int chunkWorldX = chunk.getChunkX() * Chunk.WIDTH;
                    int chunkWorldZ = chunk.getChunkZ() * Chunk.DEPTH;
                    int localLx = lx - chunkWorldX;
                    int localLz = lz - chunkWorldZ;

                    if (localLx >= 0 && localLx < Chunk.WIDTH && localLz >= 0 && localLz < Chunk.DEPTH) {
                        if (chunk.getBlock(localLx, ly, localLz) != BlockType.OAK_LOG) {
                            chunk.setBlock(localLx, ly, localLz, BlockType.LEAVES);
                        }
                    }
                }
            }
        }
    }

    /**
     * Calculate terrain height using noise.
     */
    private int calculateHeight(int x, int z, double biomeValue) {
        // Ensure safe spawn point (small platform)
        if (Math.abs(x) < 4 && Math.abs(z) < 4) {
            return 64;
        }

        // Base terrain
        double baseNoise = terrainNoise.octaveNoise2D(x * 0.01, z * 0.01, 4, 0.5);

        // Map biome value [-1, 1] to amplitude factor [8, 40]
        // Smooth interpolation to prevent terrain tears
        double t = (biomeValue + 1.0) * 0.5; // [0, 1]
        t = Math.max(0, Math.min(1, t)); // Clamp

        // Smoothstep for nicer transitions
        t = t * t * (3 - 2 * t);

        double lowAmp = 8.0; // Plains
        double highAmp = 40.0; // Mountains

        double amplitude = lowAmp + t * (highAmp - lowAmp);

        if (t > 0.7) {
            // Add extra roughness for mountains
            baseNoise += terrainNoise.octaveNoise2D(x * 0.02, z * 0.02, 2, 0.5) * 0.5;
        }

        int height = (int) (BASE_HEIGHT + baseNoise * amplitude);

        // Clamp height
        return Math.max(1, Math.min(height, Chunk.HEIGHT - 1));
    }

    /**
     * Determine biome from noise value.
     */
    private BiomeType getBiome(double biomeValue) {
        if (biomeValue < -0.3) {
            return BiomeType.PLAINS;
        } else if (biomeValue < 0.15) {
            return BiomeType.FOREST;
        } else if (biomeValue < 0.45) {
            return BiomeType.HILLS;
        } else {
            return BiomeType.MOUNTAINS;
        }
    }

    /**
     * Get block type based on height and biome.
     */
    private BlockType getBlockType(int y, int height, BiomeType biome, int x, int z) {
        // Bedrock at bottom
        if (y == 0) {
            return BlockType.BEDROCK;
        }

        // Water below sea level
        if (y <= SEA_LEVEL && y > height) {
            return BlockType.WATER;
        }

        // Above ground
        if (y > height) {
            return BlockType.AIR;
        }

        // Surface blocks based on biome
        if (y == height) {
            if (height <= SEA_LEVEL + 2) {
                return BlockType.SAND; // Beach
            }
            switch (biome) {
                case MOUNTAINS:
                    return height > 90 ? BlockType.SNOW : BlockType.STONE;
                case PLAINS:
                case FOREST:
                case HILLS:
                default:
                    return BlockType.GRASS;
            }
        }

        // Near surface
        if (y > height - 4) {
            if (height <= SEA_LEVEL + 2) {
                return BlockType.SAND;
            }
            return BlockType.DIRT;
        }

        if (y < height - 4) {
            // Ores are generated exclusively by OreGenerator's vein pass.
            return BlockType.STONE;
        }

        return BlockType.STONE;
    }

    /**
     * Update chunks around the player.
     * Uses staged loading: EMPTY → GENERATED → LIGHTED → READY
     * Chunks only mesh when all neighbors are LIGHTED to prevent border artifacts.
     */
    public void update(Camera camera) {
        int playerChunkX = (int) Math.floor(camera.getPosition().x / Chunk.WIDTH);
        int playerChunkZ = (int) Math.floor(camera.getPosition().z / Chunk.DEPTH);

        // Step 1: Process completed mesh build tasks (rate limited)
        int uploadsThisFrame = 0;
        while (uploadsThisFrame < MAX_MESH_UPLOADS_PER_FRAME) {
            ChunkMeshTask task = completedMeshTasks.poll();
            if (task == null)
                break;

            long key = chunkKey(task.chunk.getChunkX(), task.chunk.getChunkZ());
            if (chunks.get(key) != task.chunk || task.chunk.getState() != Chunk.ChunkState.MESHING) {
                chunksBeingBuilt.remove(key);
                continue;
            }
            if (task.chunk.getModificationVersion() != task.expectedVersion) {
                task.chunk.setState(task.fallbackState);
                task.chunk.setDirty(true);
                chunksBeingBuilt.remove(key);
                continue;
            }

            // Apply mesh data on main thread (GPU upload)
            task.chunk.applyMeshData(task.meshData);
            task.chunk.setState(Chunk.ChunkState.READY);
            chunksBeingBuilt.remove(key);
            uploadsThisFrame++;
        }

        ensureChunkShell(playerChunkX, playerChunkZ);

        // Push already-generated nearby chunks toward visibility before spending
        // worker capacity on farther empty terrain.
        progressPriorityMeshableChunks(playerChunkX, playerChunkZ);
        progressPriorityLightingChunks(playerChunkX, playerChunkZ);

        // Step 3: Progress chunks through states (with per-frame rate limiting)
        // Uses spiral order: closest chunks to player are processed first
        int generationsThisFrame = 0;
        int lightingsThisFrame = 0;
        int meshesThisFrame = 0;

        for (int[] offset : SPIRAL_OFFSETS) {
            if (generationsThisFrame >= MAX_GENERATES_PER_FRAME
                    && lightingsThisFrame >= MAX_LIGHTINGS_PER_FRAME
                    && meshesThisFrame >= MAX_MESHES_PER_FRAME) {
                break;
            }

            int dx = offset[0];
            int dz = offset[1];

            // Skip if outside render distance
            if (Math.abs(dx) > renderDistance || Math.abs(dz) > renderDistance)
                continue;

            int chunkX = playerChunkX + dx;
            int chunkZ = playerChunkZ + dz;
            long key = chunkKey(chunkX, chunkZ);
            Chunk chunk = chunks.get(key);

            if (chunk == null)
                continue;

            Chunk.ChunkState state = chunk.getState();
            // EMPTY → Submit for terrain generation (rate limited)
            if (state == Chunk.ChunkState.EMPTY && !chunksBeingBuilt.contains(key)) {
                if (generationsThisFrame >= MAX_GENERATES_PER_FRAME)
                    continue;
                if (chunksBeingBuilt.size() >= MAX_PENDING_CHUNK_WORK)
                    continue;
                generationsThisFrame++;

                chunk.setState(Chunk.ChunkState.GENERATING);
                chunksBeingBuilt.add(key);
                final Chunk chunkRef = chunk;
                final int cx = chunkX, cz = chunkZ;
                meshBuildPool.submit(() -> {
                    try {
                        generateChunkTerrain(chunkRef, cx, cz);
                        if (isLiveChunk(chunkRef, cx, cz)) {
                            chunkRef.setState(Chunk.ChunkState.GENERATED);
                        }
                    } catch (Exception e) {
                        System.err.println("Error generating chunk: " + e.getMessage());
                        if (isLiveChunk(chunkRef, cx, cz)) {
                            chunkRef.setState(Chunk.ChunkState.EMPTY);
                        }
                    } finally {
                        chunksBeingBuilt.remove(chunkKey(cx, cz));
                    }
                });
            }

            // GENERATED → Submit for lighting (rate limited, ASYNC!)
            else if (state == Chunk.ChunkState.GENERATED && !chunksBeingBuilt.contains(key)) {
                if (dynamicTickScannedChunks.add(key)) {
                    reconcileTileEntitiesInChunk(chunk);
                    scheduleTickableBlocksInChunk(chunk);
                }
                if (chunk.hasAllNeighborsAtLeast(Chunk.ChunkState.GENERATED)) {
                    if (lightingsThisFrame >= MAX_LIGHTINGS_PER_FRAME)
                        continue;
                    if (chunksBeingBuilt.size() >= MAX_PENDING_CHUNK_WORK)
                        continue;
                    lightingsThisFrame++;

                    chunk.setState(Chunk.ChunkState.LIGHTING);
                    chunksBeingBuilt.add(key);
                    final Chunk chunkRef = chunk;
                    final long chunkKey = key;
                    meshBuildPool.submit(() -> {
                        try {
                            if (!isLiveChunk(chunkRef)) {
                                return;
                            }
                            chunkRef.calculateSkyLight();
                            if (isLiveChunk(chunkRef)) {
                                chunkRef.setState(Chunk.ChunkState.LIGHTED);
                            }
                        } catch (Exception e) {
                            System.err.println("Error lighting chunk: " + e.getMessage());
                            if (isLiveChunk(chunkRef)) {
                                chunkRef.setState(Chunk.ChunkState.GENERATED);
                            }
                        } finally {
                            chunksBeingBuilt.remove(chunkKey);
                        }
                    });
                }
            }

            // LIGHTED → Submit for mesh building (rate limited)
            else if (state == Chunk.ChunkState.LIGHTED && !chunksBeingBuilt.contains(key)) {
                if (chunk.hasAllNeighborsAtLeast(Chunk.ChunkState.LIGHTED)) {
                    if (meshesThisFrame >= MAX_MESHES_PER_FRAME)
                        continue;
                    if (chunksBeingBuilt.size() >= MAX_PENDING_CHUNK_WORK)
                        continue;
                    meshesThisFrame++;

                    chunk.setState(Chunk.ChunkState.MESHING);
                    chunksBeingBuilt.add(key);
                    final Chunk chunkRef = chunk;
                    final long expectedVersion = chunkRef.getModificationVersion();
                    meshBuildPool.submit(() -> {
                        boolean queued = false;
                        try {
                            if (!isLiveChunk(chunkRef)) {
                                return;
                            }
                            ChunkMeshData meshData = ChunkMeshBuilder.buildMeshData(chunkRef);
                            if (isLiveChunk(chunkRef)) {
                                completedMeshTasks.offer(new ChunkMeshTask(chunkRef, meshData, expectedVersion,
                                        Chunk.ChunkState.LIGHTED));
                                queued = true;
                            }
                        } catch (Exception e) {
                            System.err.println("Error building chunk mesh: " + e.getMessage());
                            if (isLiveChunk(chunkRef)) {
                                chunkRef.setState(Chunk.ChunkState.LIGHTED);
                            }
                            chunksBeingBuilt.remove(chunkKey(chunkRef.getChunkX(), chunkRef.getChunkZ()));
                        } finally {
                            if (!queued) {
                                chunksBeingBuilt.remove(chunkKey(chunkRef.getChunkX(), chunkRef.getChunkZ()));
                            }
                        }
                    });
                }
            }

            // READY but dirty (block placed/removed) → Rebuild mesh
            else if (state == Chunk.ChunkState.READY && chunk.isDirty() && !chunksBeingBuilt.contains(key)) {
                if (chunk.hasAllNeighborsAtLeast(Chunk.ChunkState.LIGHTED)) {
                    if (meshesThisFrame >= MAX_MESHES_PER_FRAME)
                        continue;
                    if (chunksBeingBuilt.size() >= MAX_PENDING_CHUNK_WORK)
                        continue;
                    meshesThisFrame++;

                    chunk.setState(Chunk.ChunkState.MESHING);
                    chunksBeingBuilt.add(key);
                    final Chunk chunkRef = chunk;
                    final long expectedVersion = chunkRef.getModificationVersion();
                    meshBuildPool.submit(() -> {
                        boolean queued = false;
                        try {
                            if (!isLiveChunk(chunkRef)) {
                                return;
                            }
                            chunkRef.calculateSkyLight();
                            if (!isLiveChunk(chunkRef)) {
                                return;
                            }
                            ChunkMeshData meshData = ChunkMeshBuilder.buildMeshData(chunkRef);
                            if (isLiveChunk(chunkRef)) {
                                completedMeshTasks.offer(new ChunkMeshTask(chunkRef, meshData, expectedVersion,
                                        Chunk.ChunkState.READY));
                                queued = true;
                            }
                        } catch (Exception e) {
                            System.err.println("Error rebuilding chunk mesh: " + e.getMessage());
                            if (isLiveChunk(chunkRef)) {
                                chunkRef.setState(Chunk.ChunkState.READY);
                            }
                            chunksBeingBuilt.remove(chunkKey(chunkRef.getChunkX(), chunkRef.getChunkZ()));
                        } finally {
                            if (!queued) {
                                chunksBeingBuilt.remove(chunkKey(chunkRef.getChunkX(), chunkRef.getChunkZ()));
                            }
                        }
                    });
                }
            }
        }

        // Step 4: Unload distant chunks
        List<Long> toUnload = new ArrayList<>();
        for (Map.Entry<Long, Chunk> entry : chunks.entrySet()) {
            Chunk chunk = entry.getValue();
            int dx = chunk.getChunkX() - playerChunkX;
            int dz = chunk.getChunkZ() - playerChunkZ;

            if (Math.abs(dx) > unloadDistance || Math.abs(dz) > unloadDistance) {
                toUnload.add(entry.getKey());
            }
        }

        for (Long key : toUnload) {
            Chunk chunk = chunks.get(key);
            if (chunk != null && chunk.isModified() && saveManager != null) {
                queueModifiedChunkSave(chunk);
            }
            chunk = chunks.remove(key);
            if (chunk != null) {
                removeScheduledTicksForChunk(chunk);
                removeTileEntitiesForChunk(chunk);
                chunk.cleanup();
            }
            chunksBeingBuilt.remove(key);
            dynamicTickScannedChunks.remove(key);
        }
    }

    private boolean isLiveChunk(Chunk chunk) {
        return chunk != null && isLiveChunk(chunk, chunk.getChunkX(), chunk.getChunkZ());
    }

    private boolean isLiveChunk(Chunk chunk, int chunkX, int chunkZ) {
        return chunk != null && chunks.get(chunkKey(chunkX, chunkZ)) == chunk;
    }

    private void progressPriorityLightingChunks(int playerChunkX, int playerChunkZ) {
        int lightingsThisFrame = 0;
        for (int[] offset : SPIRAL_OFFSETS) {
            if (lightingsThisFrame >= MAX_LIGHTINGS_PER_FRAME
                    || chunksBeingBuilt.size() >= MAX_PENDING_CHUNK_WORK) {
                return;
            }
            int dx = offset[0];
            int dz = offset[1];
            if (Math.abs(dx) > renderDistance || Math.abs(dz) > renderDistance) {
                continue;
            }
            int chunkX = playerChunkX + dx;
            int chunkZ = playerChunkZ + dz;
            long key = chunkKey(chunkX, chunkZ);
            Chunk chunk = chunks.get(key);
            if (chunk == null || chunk.getState() != Chunk.ChunkState.GENERATED
                    || chunksBeingBuilt.contains(key)) {
                continue;
            }
            if (dynamicTickScannedChunks.add(key)) {
                reconcileTileEntitiesInChunk(chunk);
                scheduleTickableBlocksInChunk(chunk);
            }
            if (!chunk.hasAllNeighborsAtLeast(Chunk.ChunkState.GENERATED)) {
                continue;
            }

            lightingsThisFrame++;
            chunk.setState(Chunk.ChunkState.LIGHTING);
            chunksBeingBuilt.add(key);
            final Chunk chunkRef = chunk;
            final long chunkKey = key;
            meshBuildPool.submit(() -> {
                try {
                    if (!isLiveChunk(chunkRef)) {
                        return;
                    }
                    chunkRef.calculateSkyLight();
                    if (isLiveChunk(chunkRef)) {
                        chunkRef.setState(Chunk.ChunkState.LIGHTED);
                    }
                } catch (Exception e) {
                    System.err.println("Error lighting chunk: " + e.getMessage());
                    if (isLiveChunk(chunkRef)) {
                        chunkRef.setState(Chunk.ChunkState.GENERATED);
                    }
                } finally {
                    chunksBeingBuilt.remove(chunkKey);
                }
            });
        }
    }

    private void progressPriorityMeshableChunks(int playerChunkX, int playerChunkZ) {
        int meshesThisFrame = 0;
        for (int[] offset : SPIRAL_OFFSETS) {
            if (meshesThisFrame >= MAX_MESHES_PER_FRAME
                    || chunksBeingBuilt.size() >= MAX_PENDING_CHUNK_WORK) {
                return;
            }
            int dx = offset[0];
            int dz = offset[1];
            if (Math.abs(dx) > renderDistance || Math.abs(dz) > renderDistance) {
                continue;
            }
            int chunkX = playerChunkX + dx;
            int chunkZ = playerChunkZ + dz;
            long key = chunkKey(chunkX, chunkZ);
            Chunk chunk = chunks.get(key);
            if (chunk == null || chunksBeingBuilt.contains(key)) {
                continue;
            }

            Chunk.ChunkState state = chunk.getState();
            boolean dirtyReady = state == Chunk.ChunkState.READY && chunk.isDirty();
            boolean initialMesh = state == Chunk.ChunkState.LIGHTED;
            if ((!dirtyReady && !initialMesh) || !chunk.hasAllNeighborsAtLeast(Chunk.ChunkState.LIGHTED)) {
                continue;
            }

            meshesThisFrame++;
            chunk.setState(Chunk.ChunkState.MESHING);
            chunksBeingBuilt.add(key);
            final Chunk chunkRef = chunk;
            final Chunk.ChunkState fallbackState = dirtyReady ? Chunk.ChunkState.READY : Chunk.ChunkState.LIGHTED;
            final long expectedVersion = chunkRef.getModificationVersion();
            meshBuildPool.submit(() -> {
                boolean queued = false;
                try {
                    if (!isLiveChunk(chunkRef)) {
                        return;
                    }
                    if (dirtyReady) {
                        chunkRef.calculateSkyLight();
                        if (!isLiveChunk(chunkRef)) {
                            return;
                        }
                    }
                    ChunkMeshData meshData = ChunkMeshBuilder.buildMeshData(chunkRef);
                    if (isLiveChunk(chunkRef)) {
                        completedMeshTasks.offer(new ChunkMeshTask(chunkRef, meshData, expectedVersion, fallbackState));
                        queued = true;
                    }
                } catch (Exception e) {
                    System.err.println("Error building chunk mesh: " + e.getMessage());
                    if (isLiveChunk(chunkRef)) {
                        chunkRef.setState(fallbackState);
                    }
                    chunksBeingBuilt.remove(chunkKey(chunkRef.getChunkX(), chunkRef.getChunkZ()));
                } finally {
                    if (!queued) {
                        chunksBeingBuilt.remove(chunkKey(chunkRef.getChunkX(), chunkRef.getChunkZ()));
                    }
                }
            });
        }
    }

    private void ensureChunkShell(int playerChunkX, int playerChunkZ) {
        if (playerChunkX != chunkShellCenterX || playerChunkZ != chunkShellCenterZ) {
            chunkShellReady = false;
            chunkShellCenterX = playerChunkX;
            chunkShellCenterZ = playerChunkZ;
            chunkShellCursor = 0;
        }
        if (chunkShellReady) {
            return;
        }

        int steps = 0;
        int shellRadius = renderDistance + 1;
        while (chunkShellCursor < SPIRAL_OFFSETS.length && steps < MAX_CHUNK_SHELL_STEPS_PER_FRAME) {
            int[] offset = SPIRAL_OFFSETS[chunkShellCursor++];
            int dx = offset[0];
            int dz = offset[1];
            if (Math.abs(dx) > shellRadius || Math.abs(dz) > shellRadius) {
                continue;
            }
            steps++;

            int chunkX = playerChunkX + dx;
            int chunkZ = playerChunkZ + dz;
            long key = chunkKey(chunkX, chunkZ);
            if (!chunks.containsKey(key)) {
                chunks.put(key, createChunk(chunkX, chunkZ));
            }
            refreshChunkAndNeighbors(chunkX, chunkZ);
        }

        if (chunkShellCursor >= SPIRAL_OFFSETS.length) {
            chunkShellReady = true;
            chunkShellCursor = 0;
        }
    }

    private void refreshChunkAndNeighbors(int chunkX, int chunkZ) {
        refreshChunkNeighbors(chunkX, chunkZ);
        refreshChunkNeighbors(chunkX, chunkZ - 1);
        refreshChunkNeighbors(chunkX, chunkZ + 1);
        refreshChunkNeighbors(chunkX + 1, chunkZ);
        refreshChunkNeighbors(chunkX - 1, chunkZ);
    }

    private void refreshChunkNeighbors(int chunkX, int chunkZ) {
        Chunk chunk = chunks.get(chunkKey(chunkX, chunkZ));
        if (chunk == null) {
            return;
        }
        chunk.setNeighbors(
                chunks.get(chunkKey(chunkX, chunkZ - 1)),
                chunks.get(chunkKey(chunkX, chunkZ + 1)),
                chunks.get(chunkKey(chunkX + 1, chunkZ)),
                chunks.get(chunkKey(chunkX - 1, chunkZ)));
    }

    /**
     * Render all visible chunks with frustum culling.
     */
    /**
     * Render world chunks.
     *
     * @param midPassAction Optional action to run between Opaque and Transparent
     *                      passes (e.g. Block Highlight).
     */
    public void render(Renderer renderer, Camera camera, Runnable midPassAction) {
        int playerChunkX = (int) Math.floor(camera.getPosition().x / Chunk.WIDTH);
        int playerChunkZ = (int) Math.floor(camera.getPosition().z / Chunk.DEPTH);

        // Update frustum for culling
        viewProjection.set(camera.getProjectionMatrix()).mul(camera.getViewMatrix());
        frustum.update(viewProjection);

        atlas.bind(0);

        visibleRenderChunks.clear();
        boolean hasCutout = false;
        boolean hasTranslucent = false;
        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                if (advancedOpenGl && dx * dx + dz * dz > renderDistance * renderDistance) {
                    continue;
                }
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;
                int worldX = chunkX * Chunk.WIDTH;
                int worldZ = chunkZ * Chunk.DEPTH;
                if (!frustum.isChunkVisible(worldX, worldZ)) {
                    continue;
                }

                Chunk chunk = chunks.get(chunkKey(chunkX, chunkZ));
                if (chunk == null || chunk.isEmpty()) {
                    continue;
                }

                visibleRenderChunks.add(chunk);
                hasCutout |= chunk.getCutoutMesh() != null;
                hasTranslucent |= chunk.getTransparentMesh() != null;
            }
        }

        // --- PASS 1: OPAQUE ---
        renderer.beginRender(camera);
        renderer.useIdentityModelMatrix();

        // Explicitly ensure solid rendering state for opaque pass
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        org.lwjgl.opengl.GL11.glDepthMask(true);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_BLEND);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_CULL_FACE);
        renderer.setAlphaCutoff(0.0f);

        for (Chunk chunk : visibleRenderChunks) {
            Mesh mesh = chunk.getMesh();
            if (mesh != null) {
                renderer.renderPreparedMesh(mesh);
            }
        }

        // --- PASS 2: CUTOUT ---
        // Alpha-tested blocks such as leaves must write depth so clouds/water do not
        // show through their solid pixels.
        if (hasCutout) {
            renderer.setAlphaCutoff(0.1f);

            for (Chunk chunk : visibleRenderChunks) {
                Mesh mesh = chunk.getCutoutMesh();
                if (mesh != null) {
                    renderer.renderPreparedMesh(mesh);
                }
            }
            renderer.setAlphaCutoff(0.0f);
        }
        renderer.endPreparedMeshBatch();
        renderer.endRender(); // End Opaque/Cutout Pass

        // --- MID-PASS ACTION (Highlight) ---
        if (midPassAction != null) {
            midPassAction.run();
        }

        // --- PASS 3: TRANSLUCENT ---
        if (hasTranslucent) {
            // Re-bind atlas in case midPassAction unbound it (fixes black water bug)
            atlas.bind(0);

            renderer.beginRender(camera);
            renderer.useIdentityModelMatrix();

            // CRITICAL: Re-enable blending for water/glass (BlockHighlight might have
            // disabled it)
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
            org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA,
                    org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);

            renderer.setAlphaCutoff(0.0f);
            renderer.setDepthMask(false); // Disable depth writing
            org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_CULL_FACE);

            visibleRenderChunks.sort((a, b) -> Float.compare(
                    chunkDistanceSquared(b, camera),
                    chunkDistanceSquared(a, camera)));
            for (Chunk chunk : visibleRenderChunks) {
                Mesh mesh = chunk.getTransparentMesh();
                if (mesh != null) {
                    renderer.renderPreparedMesh(mesh);
                }
            }

            renderer.endPreparedMeshBatch();
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_CULL_FACE);
            renderer.setDepthMask(true); // Re-enable depth writing
            org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_BLEND);
            renderer.endRender();
        }

        atlas.unbind();
    }

    private static float chunkDistanceSquared(Chunk chunk, Camera camera) {
        float cx = chunk.getWorldX() + Chunk.WIDTH * 0.5f;
        float cz = chunk.getWorldZ() + Chunk.DEPTH * 0.5f;
        float dx = cx - camera.getPosition().x;
        float dz = cz - camera.getPosition().z;
        return dx * dx + dz * dz;
    }

    /**
     * Get block at world coordinates.
     */
    public BlockType getBlock(int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return BlockType.AIR;
        }

        int chunkX = Math.floorDiv(x, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(z, Chunk.DEPTH);

        Chunk chunk = getChunkNow(chunkX, chunkZ);

        int localX = Math.floorMod(x, Chunk.WIDTH);
        int localZ = Math.floorMod(z, Chunk.DEPTH);

        return chunk.getBlock(localX, y, localZ);
    }

    public BlockType getBlockIfLoaded(int x, int y, int z, BlockType fallback) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return fallback;
        }

        int chunkX = Math.floorDiv(x, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(z, Chunk.DEPTH);
        Chunk chunk = chunks.get(chunkKey(chunkX, chunkZ));
        if (chunk == null || chunk.getState().ordinal() < Chunk.ChunkState.GENERATED.ordinal()) {
            return fallback;
        }

        int localX = Math.floorMod(x, Chunk.WIDTH);
        int localZ = Math.floorMod(z, Chunk.DEPTH);
        return chunk.getBlock(localX, y, localZ);
    }

    public Chunk getLoadedChunk(int chunkX, int chunkZ) {
        return chunks.get(chunkKey(chunkX, chunkZ));
    }

    public int getBlockMetadataIfLoaded(int x, int y, int z, int fallback) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return fallback;
        }

        int chunkX = Math.floorDiv(x, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(z, Chunk.DEPTH);
        Chunk chunk = chunks.get(chunkKey(chunkX, chunkZ));
        if (chunk == null || chunk.getState().ordinal() < Chunk.ChunkState.GENERATED.ordinal()) {
            return fallback;
        }

        int localX = Math.floorMod(x, Chunk.WIDTH);
        int localZ = Math.floorMod(z, Chunk.DEPTH);
        return chunk.getBlockMetadata(localX, y, localZ);
    }

    public boolean isChunkGeneratedForBlock(int x, int z) {
        int chunkX = Math.floorDiv(x, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(z, Chunk.DEPTH);
        Chunk chunk = chunks.get(chunkKey(chunkX, chunkZ));
        return chunk != null && chunk.getState().ordinal() >= Chunk.ChunkState.GENERATED.ordinal();
    }

    public boolean canSeeSky(int x, int y, int z) {
        if (y >= Chunk.HEIGHT) {
            return true;
        }
        if (!isChunkGeneratedForBlock(x, z)) {
            return false;
        }
        int startY = Math.max(0, y + 1);
        for (int checkY = startY; checkY < Chunk.HEIGHT; checkY++) {
            BlockType block = getBlockIfLoaded(x, checkY, z, BlockType.BEDROCK);
            if (!block.isAir() && !block.isTransparent()) {
                return false;
            }
        }
        return true;
    }

    public int getBlockMetadata(int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return 0;
        }

        int chunkX = Math.floorDiv(x, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(z, Chunk.DEPTH);
        Chunk chunk = getChunkNow(chunkX, chunkZ);
        int localX = Math.floorMod(x, Chunk.WIDTH);
        int localZ = Math.floorMod(z, Chunk.DEPTH);
        return chunk.getBlockMetadata(localX, y, localZ);
    }

    public int getBlockLight(int x, int y, int z) {
        return getBlockLightIfLoaded(x, y, z, 0);
    }

    public int getBlockLightIfLoaded(int x, int y, int z, int fallback) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return fallback;
        }

        int chunkX = Math.floorDiv(x, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(z, Chunk.DEPTH);
        Chunk chunk = chunks.get(chunkKey(chunkX, chunkZ));
        if (chunk == null || chunk.getState().ordinal() < Chunk.ChunkState.LIGHTED.ordinal()) {
            return fallback;
        }

        int localX = Math.floorMod(x, Chunk.WIDTH);
        int localZ = Math.floorMod(z, Chunk.DEPTH);
        return chunk.getBlockLight(localX, y, localZ);
    }

    public List<AABB> getCollisionBoxes(int x, int y, int z) {
        BlockType type = getBlock(x, y, z);
        int metadata = getBlockMetadata(x, y, z);
        if (type == BlockType.MOVING_PISTON) {
            MovingPistonState state = getMovingPistonState(x, y, z);
            if (state != null) {
                return movingPistonBoxes(state, true);
            }
        }
        return BlockShape.collisionShape(new BlockState(type, metadata), contextAt(x, y, z)).toAabbs(x, y, z);
    }

    public List<AABB> getCollisionBoxesIfLoaded(int x, int y, int z) {
        BlockType type = getBlockIfLoaded(x, y, z, BlockType.AIR);
        if (type == BlockType.AIR) {
            return List.of();
        }
        int metadata = getBlockMetadataIfLoaded(x, y, z, 0);
        if (type == BlockType.MOVING_PISTON) {
            MovingPistonState state = getMovingPistonState(x, y, z);
            if (state != null) {
                return movingPistonBoxes(state, true);
            }
        }
        return BlockShape.collisionShape(new BlockState(type, metadata), contextAtIfLoaded(x, y, z))
                .toAabbs(x, y, z);
    }

    public List<AABB> getSelectionBoxes(int x, int y, int z) {
        BlockType type = getBlock(x, y, z);
        int metadata = getBlockMetadata(x, y, z);
        if (type == BlockType.MOVING_PISTON) {
            MovingPistonState state = getMovingPistonState(x, y, z);
            if (state != null) {
                return movingPistonBoxes(state, false);
            }
        }
        return BlockShape.selectionShape(new BlockState(type, metadata), contextAt(x, y, z)).toAabbs(x, y, z);
    }

    public List<AABB> getSelectionBoxesIfLoaded(int x, int y, int z) {
        BlockType type = getBlockIfLoaded(x, y, z, BlockType.AIR);
        if (type == BlockType.AIR) {
            return List.of();
        }
        int metadata = getBlockMetadataIfLoaded(x, y, z, 0);
        if (type == BlockType.MOVING_PISTON) {
            MovingPistonState state = getMovingPistonState(x, y, z);
            if (state != null) {
                return movingPistonBoxes(state, false);
            }
        }
        return BlockShape.selectionShape(new BlockState(type, metadata), contextAtIfLoaded(x, y, z))
                .toAabbs(x, y, z);
    }

    public List<AABB> getMovingPistonCollisionBoxes(AABB searchBox) {
        if (searchBox == null || movingPistons.isEmpty()) {
            return List.of();
        }
        ArrayList<AABB> boxes = new ArrayList<>();
        for (MovingPistonState state : movingPistons.values()) {
            for (AABB box : movingPistonBoxes(state, true)) {
                if (box.intersects(searchBox)) {
                    boxes.add(box);
                }
            }
        }
        return boxes;
    }

    private List<AABB> movingPistonBoxes(MovingPistonState state, boolean collision) {
        return movingPistonBoxesAtProgress(state, state.progress(blockTickClock), collision);
    }

    private List<AABB> movingPistonBoxesAtProgress(MovingPistonState state, float progress, boolean collision) {
        BlockType carriedType = state.carriedType();
        if (carriedType == null || carriedType.isAir()) {
            return List.of();
        }
        int metadata = carriedType == BlockType.PISTON_HEAD ? state.facing() : state.carriedMetadata();
        VoxelShape shape = collision
                ? BlockShape.collisionShape(new BlockState(carriedType, metadata), emptyBlockContext())
                : BlockShape.selectionShape(new BlockState(carriedType, metadata), emptyBlockContext());
        if (shape.isEmpty()) {
            return List.of();
        }
        float x = state.fromX() + (state.toX() - state.fromX()) * progress;
        float y = state.fromY() + (state.toY() - state.fromY()) * progress;
        float z = state.fromZ() + (state.toZ() - state.fromZ()) * progress;
        ArrayList<AABB> boxes = new ArrayList<>(shape.boxes().size());
        for (BlockShape.Cuboid box : shape.boxes()) {
            boxes.add(new AABB(
                    x + box.minX(), y + box.minY(), z + box.minZ(),
                    x + box.maxX(), y + box.maxY(), z + box.maxZ()));
        }
        return boxes;
    }

    private static BlockShape.BlockContext emptyBlockContext() {
        return new BlockShape.BlockContext() {
            @Override
            public BlockType getBlock(int dx, int dy, int dz) {
                return BlockType.AIR;
            }

            @Override
            public int getMetadata(int dx, int dy, int dz) {
                return 0;
            }
        };
    }

    public List<AABB> getPlacementCollisionBoxes(int x, int y, int z, BlockType type, int metadata) {
        return BlockShape.collisionShape(new BlockState(type, metadata), contextAt(x, y, z)).toAabbs(x, y, z);
    }

    private List<AABB> getPlacementCollisionBoxesIfLoaded(int x, int y, int z, BlockType type, int metadata) {
        return BlockShape.collisionShape(new BlockState(type, metadata), contextAtIfLoaded(x, y, z)).toAabbs(x, y, z);
    }

    public boolean canPlaceBlockAt(int x, int y, int z, BlockType type, int metadata, AABB playerBox) {
        if (y < 0 || y >= Chunk.HEIGHT || !BlockShape.isReplaceable(getBlock(x, y, z))) {
            return false;
        }
        if (type == BlockType.CHEST && !canPlaceChestAt(x, y, z)) {
            return false;
        }
        if (type == BlockType.FENCE_GATE && !canPlaceFenceGateAt(x, y, z)) {
            return false;
        }
        if (!BlockShape.canPlaceAt(type, metadata, contextAt(x, y, z))) {
            return false;
        }
        if (isSmallMushroom(type) && !canSmallMushroomStayAt(x, y, z, false)) {
            return false;
        }
        if (playerBox != null) {
            for (AABB box : getPlacementCollisionBoxes(x, y, z, type, metadata)) {
                if (box.intersects(playerBox)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean canPlaceBlockAtIfLoaded(int x, int y, int z, BlockType type, int metadata) {
        if (y < 0 || y >= Chunk.HEIGHT || !isChunkGeneratedForBlock(x, z)
                || !BlockShape.isReplaceable(getBlockIfLoaded(x, y, z, BlockType.AIR))) {
            return false;
        }
        if (!BlockShape.canPlaceAt(type, metadata, contextAtIfLoaded(x, y, z))) {
            return false;
        }
        return !isSmallMushroom(type) || canSmallMushroomStayAt(x, y, z, true);
    }

    private boolean canPlaceFenceGateAt(int x, int y, int z) {
        return getBlock(x, y - 1, z).isSolid();
    }

    BlockShape.BlockContext contextAt(int x, int y, int z) {
        return new BlockShape.BlockContext() {
            @Override
            public BlockType getBlock(int dx, int dy, int dz) {
                return World.this.getBlock(x + dx, y + dy, z + dz);
            }

            @Override
            public int getMetadata(int dx, int dy, int dz) {
                return World.this.getBlockMetadata(x + dx, y + dy, z + dz);
            }
        };
    }

    public BlockShape.BlockContext contextAtIfLoaded(int x, int y, int z) {
        return new BlockShape.BlockContext() {
            @Override
            public BlockType getBlock(int dx, int dy, int dz) {
                return World.this.getBlockIfLoaded(x + dx, y + dy, z + dz, BlockType.AIR);
            }

            @Override
            public int getMetadata(int dx, int dy, int dz) {
                return World.this.getBlockMetadataIfLoaded(x + dx, y + dy, z + dz, 0);
            }
        };
    }

    /**
     * Get sky light level at world coordinates (0-15).
     * Returns 15 (full light) if chunk not yet LIGHTED, preventing hostile mob
     * spawning.
     */
    public int getSkyLight(int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return y >= Chunk.HEIGHT ? 15 : 0;
        }

        int chunkX = Math.floorDiv(x, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(z, Chunk.DEPTH);

        Chunk chunk = chunks.get(chunkKey(chunkX, chunkZ));
        if (chunk == null) {
            return 15; // Unloaded chunks default to full light
        }

        // Don't return lighting data until chunk has been properly lit
        // This prevents hostile mobs from spawning before lighting is calculated
        if (chunk.getState().ordinal() < Chunk.ChunkState.LIGHTED.ordinal()) {
            return 15; // Not ready yet, return full light to prevent hostile spawns
        }

        int localX = Math.floorMod(x, Chunk.WIDTH);
        int localZ = Math.floorMod(z, Chunk.DEPTH);

        return chunk.getSkyLight(localX, y, localZ);
    }

    /**
     * Set block at world coordinates.
     */
    public void setBlock(int x, int y, int z, BlockType type) {
        setBlock(x, y, z, type, 0);
    }

    public void setBlock(int x, int y, int z, BlockType type, int metadata) {
        setBlockInternal(x, y, z, type, metadata, false);
    }

    public void setBlockPreservingTile(int x, int y, int z, BlockType type, int metadata) {
        setBlockInternal(x, y, z, type, metadata, true);
    }

    public void addBlockChangeListener(BlockChangeListener listener) {
        if (listener != null) {
            blockChangeListeners.add(listener);
        }
    }

    public void removeBlockChangeListener(BlockChangeListener listener) {
        blockChangeListeners.remove(listener);
    }

    public void addDamageEventListener(DamageEventListener listener) {
        if (listener != null) {
            damageEventListeners.add(listener);
        }
    }

    public void removeDamageEventListener(DamageEventListener listener) {
        damageEventListeners.remove(listener);
    }

    public void setProjectilePlayerInteractionHandler(ProjectilePlayerInteractionHandler handler) {
        this.projectilePlayerInteractionHandler = handler;
    }

    public void setRemotePlayerInteractionHandler(RemotePlayerInteractionHandler handler) {
        this.remotePlayerInteractionHandler = handler;
    }

    public RemotePlayerTarget remotePlayerTargetById(String playerId) {
        if (remotePlayerInteractionHandler == null) {
            return null;
        }
        return remotePlayerInteractionHandler.targetById(playerId);
    }

    public RemotePlayerTarget remotePlayerViewById(String playerId) {
        if (remotePlayerInteractionHandler == null) {
            return null;
        }
        return remotePlayerInteractionHandler.viewById(playerId);
    }

    public RemotePlayerTarget nearestRemotePlayerTarget(float sourceX, float sourceY, float sourceZ,
            float range, boolean requireSight) {
        if (remotePlayerInteractionHandler == null) {
            return null;
        }
        return remotePlayerInteractionHandler.nearestTarget(sourceX, sourceY, sourceZ, range, requireSight);
    }

    public List<RemotePlayerTarget> remotePlayerTargets(float sourceX, float sourceY, float sourceZ,
            float range, boolean requireSight) {
        if (remotePlayerInteractionHandler == null) {
            return List.of();
        }
        List<RemotePlayerTarget> targets = remotePlayerInteractionHandler.targets(
                sourceX, sourceY, sourceZ, range, requireSight);
        return targets == null ? List.of() : targets;
    }

    public List<RemotePlayerTarget> remotePlayerViews(float sourceX, float sourceY, float sourceZ,
            float range, boolean requireSight) {
        if (remotePlayerInteractionHandler == null) {
            return List.of();
        }
        List<RemotePlayerTarget> views = remotePlayerInteractionHandler.views(
                sourceX, sourceY, sourceZ, range, requireSight);
        return views == null ? List.of() : views;
    }

    public boolean damageRemotePlayerTarget(String playerId, RemotePlayerDamage damage) {
        return remotePlayerInteractionHandler != null
                && remotePlayerInteractionHandler.damageTarget(playerId, damage);
    }

    public boolean applyRemotePlayerStatusEffect(String playerId, StatusEffectInstance effect) {
        return remotePlayerInteractionHandler != null
                && remotePlayerInteractionHandler.applyStatusEffect(playerId, effect);
    }

    public boolean pullRemotePlayerTarget(String playerId, float motionX, float motionY, float motionZ) {
        return remotePlayerInteractionHandler != null
                && remotePlayerInteractionHandler.pullTarget(playerId, motionX, motionY, motionZ);
    }

    public ProjectilePlayerHit findRemoteProjectilePlayerHit(Vector3f origin, Vector3f direction, float maxDistance) {
        return findRemoteProjectilePlayerHit(origin, direction, maxDistance, "");
    }

    public ProjectilePlayerHit findRemoteProjectilePlayerHit(Vector3f origin, Vector3f direction, float maxDistance,
            String ignoredPlayerId) {
        if (projectilePlayerInteractionHandler == null) {
            return ProjectilePlayerHit.miss();
        }
        ProjectilePlayerHit hit = projectilePlayerInteractionHandler.findProjectilePlayerHit(origin, direction,
                maxDistance, ignoredPlayerId);
        return hit == null ? ProjectilePlayerHit.miss() : hit;
    }

    public boolean damageRemoteProjectilePlayer(ProjectilePlayerHit hit, ProjectilePlayerDamage damage) {
        return projectilePlayerInteractionHandler != null
                && projectilePlayerInteractionHandler.damageProjectilePlayer(hit, damage);
    }

    public void splashRemoteProjectilePlayers(float x, float y, float z, PotionData potion,
            String directHitPlayerId) {
        if (projectilePlayerInteractionHandler != null) {
            projectilePlayerInteractionHandler.splashPotionPlayers(x, y, z, potion, directHitPlayerId);
        }
    }

    public void rebuildBlockMeshesNow(int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return;
        }
        int chunkX = Math.floorDiv(x, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(z, Chunk.DEPTH);
        int localX = Math.floorMod(x, Chunk.WIDTH);
        int localZ = Math.floorMod(z, Chunk.DEPTH);

        markChunkForAsyncRebuild(chunkX, chunkZ);
        if (localX == 0) {
            markChunkForAsyncRebuild(chunkX - 1, chunkZ);
        } else if (localX == Chunk.WIDTH - 1) {
            markChunkForAsyncRebuild(chunkX + 1, chunkZ);
        }
        if (localZ == 0) {
            markChunkForAsyncRebuild(chunkX, chunkZ - 1);
        } else if (localZ == Chunk.DEPTH - 1) {
            markChunkForAsyncRebuild(chunkX, chunkZ + 1);
        }
    }

    private void markChunkForAsyncRebuild(int chunkX, int chunkZ) {
        Chunk chunk = chunks.get(chunkKey(chunkX, chunkZ));
        if (chunk == null) {
            return;
        }
        chunk.setDirty(true);
        chunk.markLightDirty();
    }

    private void setBlockInternal(int x, int y, int z, BlockType type, int metadata, boolean preserveTile) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return;
        }

        int chunkX = Math.floorDiv(x, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(z, Chunk.DEPTH);

        Chunk chunk = getChunkNow(chunkX, chunkZ);

        int localX = Math.floorMod(x, Chunk.WIDTH);
        int localZ = Math.floorMod(z, Chunk.DEPTH);

        BlockType previous = chunk.getBlock(localX, y, localZ);
        int previousMetadata = chunk.getBlockMetadata(localX, y, localZ);
        if (previous == type && previousMetadata == metadata) {
            return;
        }
        BlockPos pos = new BlockPos(x, y, z);
        if (previous == BlockType.MOVING_PISTON && type != BlockType.MOVING_PISTON) {
            movingPistons.remove(pos);
        }
        if (previous == BlockType.JUKEBOX && type != BlockType.JUKEBOX) {
            ejectJukeboxRecordOnRemoval(tileEntities.get(pos));
        }
        chunk.setBlock(localX, y, localZ, type, metadata);
        clearScheduledBlockTick(x, y, z, previous);

        boolean keepTile = canReuseTileEntity(previous, type);
        if (previous.hasTileEntity() && !keepTile) {
            tileEntities.remove(pos);
        }
        if (type.hasTileEntity()) {
            if (keepTile) {
                tileEntities.computeIfAbsent(pos, key -> createTileEntityForBlock(type, x, y, z));
            } else {
                TileEntity tile = createTileEntityForBlock(type, x, y, z);
                if (tile != null) {
                    tileEntities.put(pos, tile);
                }
            }
        } else {
            tileEntities.remove(pos);
        }

        // Mark neighboring chunks as dirty if on border
        // Also recalculate their light since block changes affect cross-chunk lighting
        if (localX == 0) {
            Chunk neighbor = chunks.get(chunkKey(chunkX - 1, chunkZ));
            if (neighbor != null) {
                neighbor.setDirty(true);
                neighbor.markLightDirty();
            }
        }
        if (localX == Chunk.WIDTH - 1) {
            Chunk neighbor = chunks.get(chunkKey(chunkX + 1, chunkZ));
            if (neighbor != null) {
                neighbor.setDirty(true);
                neighbor.markLightDirty();
            }
        }
        if (localZ == 0) {
            Chunk neighbor = chunks.get(chunkKey(chunkX, chunkZ - 1));
            if (neighbor != null) {
                neighbor.setDirty(true);
                neighbor.markLightDirty();
            }
        }
        if (localZ == Chunk.DEPTH - 1) {
            Chunk neighbor = chunks.get(chunkKey(chunkX, chunkZ + 1));
            if (neighbor != null) {
                neighbor.setDirty(true);
                neighbor.markLightDirty();
            }
        }

        if (!suppressNeighborSupportUpdates) {
            notifyBlockChanged(x, y, z, previous, previousMetadata, type, metadata);
        } else {
            notifyBlockChangeListeners(x, y, z, previous, previousMetadata, type, metadata);
        }
    }

    public boolean setBlockIfLoaded(int x, int y, int z, BlockType type, int metadata) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return false;
        }
        int chunkX = Math.floorDiv(x, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(z, Chunk.DEPTH);
        Chunk chunk = chunks.get(chunkKey(chunkX, chunkZ));
        if (chunk == null || chunk.getState().ordinal() < Chunk.ChunkState.GENERATED.ordinal()) {
            return false;
        }

        int localX = Math.floorMod(x, Chunk.WIDTH);
        int localZ = Math.floorMod(z, Chunk.DEPTH);
        BlockType previous = chunk.getBlock(localX, y, localZ);
        int previousMetadata = chunk.getBlockMetadata(localX, y, localZ);
        if (previous == type && previousMetadata == metadata) {
            return true;
        }
        BlockPos pos = new BlockPos(x, y, z);
        if (previous == BlockType.MOVING_PISTON && type != BlockType.MOVING_PISTON) {
            movingPistons.remove(pos);
        }
        if (previous == BlockType.JUKEBOX && type != BlockType.JUKEBOX) {
            ejectJukeboxRecordOnRemoval(tileEntities.get(pos));
        }

        chunk.setBlock(localX, y, localZ, type, metadata);
        clearScheduledBlockTick(x, y, z, previous);

        boolean keepTile = canReuseTileEntity(previous, type);
        if (previous.hasTileEntity() && !keepTile) {
            tileEntities.remove(pos);
        }
        if (type.hasTileEntity()) {
            if (keepTile) {
                tileEntities.computeIfAbsent(pos, key -> createTileEntityForBlock(type, x, y, z));
            } else {
                TileEntity tile = createTileEntityForBlock(type, x, y, z);
                if (tile != null) {
                    tileEntities.put(pos, tile);
                }
            }
        } else {
            tileEntities.remove(pos);
        }

        chunk.setDirty(true);
        chunk.markLightDirty();
        markNeighborChunkDirtyForBorder(chunkX, chunkZ, localX, localZ);
        if (!suppressNeighborSupportUpdates) {
            notifyBlockChanged(x, y, z, previous, previousMetadata, type, metadata);
        } else {
            notifyBlockChangeListeners(x, y, z, previous, previousMetadata, type, metadata);
        }
        return true;
    }

    private void markNeighborChunkDirtyForBorder(int chunkX, int chunkZ, int localX, int localZ) {
        if (localX == 0) {
            markChunkDirtyIfLoaded(chunkX - 1, chunkZ);
        }
        if (localX == Chunk.WIDTH - 1) {
            markChunkDirtyIfLoaded(chunkX + 1, chunkZ);
        }
        if (localZ == 0) {
            markChunkDirtyIfLoaded(chunkX, chunkZ - 1);
        }
        if (localZ == Chunk.DEPTH - 1) {
            markChunkDirtyIfLoaded(chunkX, chunkZ + 1);
        }
    }

    private void markChunkDirtyIfLoaded(int chunkX, int chunkZ) {
        Chunk neighbor = chunks.get(chunkKey(chunkX, chunkZ));
        if (neighbor != null) {
            neighbor.setDirty(true);
            neighbor.markLightDirty();
        }
    }

    public void tickBlockUpdates(float deltaTime) {
        blockTickAccumulator += deltaTime * 20.0f;
        while (blockTickAccumulator >= 1.0f) {
            blockTickAccumulator -= 1.0f;
            advanceBlockTicks(1);
        }
    }

    public void advanceBlockTicks(int ticks) {
        for (int i = 0; i < ticks; i++) {
            blockTickClock++;
            pushEntitiesWithMovingPistons();
            completeDueMovingPistonStates();
            processDueMovingPistonTicks();
            int processed = 0;
            while (!scheduledBlockTicks.isEmpty()
                    && scheduledBlockTicks.peek().dueTick() <= blockTickClock
                    && processed < MAX_BLOCK_UPDATES_PER_TICK) {
                ScheduledBlockTick tick = scheduledBlockTicks.poll();
                if (processScheduledBlockTick(tick)) {
                    processed++;
                }
            }
        }
    }

    private void pushEntitiesWithMovingPistons() {
        if (movingPistons.isEmpty()) {
            return;
        }
        List<MovingPistonState> states = new ArrayList<>(movingPistons.values());
        states.sort((left, right) -> {
            int xCompare = Integer.compare(left.x(), right.x());
            if (xCompare != 0) {
                return xCompare;
            }
            int yCompare = Integer.compare(left.y(), right.y());
            return yCompare != 0 ? yCompare : Integer.compare(left.z(), right.z());
        });
        for (MovingPistonState state : states) {
            pushEntitiesWithMovingPiston(state);
        }
    }

    private void pushEntitiesWithMovingPiston(MovingPistonState state) {
        if (state == null || state.carriedType() == null || state.carriedType().isAir()) {
            return;
        }
        float previousProgress = movingPistonProgressAt(state, blockTickClock - 1);
        float currentProgress = movingPistonProgressAt(state, blockTickClock);
        float progressDelta = currentProgress - previousProgress;
        if (progressDelta <= 0.0f) {
            return;
        }
        float moveX = (state.toX() - state.fromX()) * progressDelta;
        float moveY = (state.toY() - state.fromY()) * progressDelta;
        float moveZ = (state.toZ() - state.fromZ()) * progressDelta;
        if (Math.abs(moveX) < 0.0001f && Math.abs(moveY) < 0.0001f && Math.abs(moveZ) < 0.0001f) {
            return;
        }
        for (AABB box : movingPistonBoxesAtProgress(state, currentProgress, true)) {
            pushEntitiesOutOfMovingPistonArea(entities, box, moveX, moveY, moveZ);
            pushEntitiesOutOfMovingPistonArea(entitiesToAdd, box, moveX, moveY, moveZ);
            pushDroppedItemsOutOfMovingPistonArea(box, moveX, moveY, moveZ);
            pushPlayerOutOfMovingPistonArea(box, moveX, moveY, moveZ);
        }
    }

    private static float movingPistonProgressAt(MovingPistonState state, long tick) {
        if (state == null) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f,
                (tick - state.startTick()) / (float) RedstoneEngine.PISTON_MOVEMENT_TICKS));
    }

    private void completeDueMovingPistonStates() {
        if (movingPistons.isEmpty()) {
            return;
        }
        List<MovingPistonState> due = new ArrayList<>();
        for (MovingPistonState state : movingPistons.values()) {
            if (state != null && blockTickClock - state.startTick() >= RedstoneEngine.PISTON_MOVEMENT_TICKS) {
                due.add(state);
            }
        }
        due.sort((left, right) -> {
            int xCompare = Integer.compare(left.x(), right.x());
            if (xCompare != 0) {
                return xCompare;
            }
            int yCompare = Integer.compare(left.y(), right.y());
            return yCompare != 0 ? yCompare : Integer.compare(left.z(), right.z());
        });
        for (MovingPistonState state : due) {
            completeMovingPiston(state.x(), state.y(), state.z());
        }
    }

    private void processDueMovingPistonTicks() {
        if (scheduledBlockTicks.isEmpty()) {
            return;
        }
        List<ScheduledBlockTick> dueMovingPistons = new ArrayList<>();
        Iterator<ScheduledBlockTick> iterator = scheduledBlockTicks.iterator();
        while (iterator.hasNext()) {
            ScheduledBlockTick tick = iterator.next();
            if (tick.dueTick() <= blockTickClock && tick.key().type() == BlockType.MOVING_PISTON) {
                iterator.remove();
                dueMovingPistons.add(tick);
            }
        }
        dueMovingPistons.sort(null);
        for (ScheduledBlockTick tick : dueMovingPistons) {
            processScheduledBlockTick(tick);
        }
    }

    private boolean processScheduledBlockTick(ScheduledBlockTick tick) {
        if (!scheduledTickKeys.remove(tick.key())) {
            return false;
        }
        ScheduledTickKey key = tick.key();
        if (getBlockIfLoaded(key.x(), key.y(), key.z(), null) != key.type()) {
            return false;
        }
        if (requiresHorizontalTickNeighborhood(key.type())
                && !hasLoadedHorizontalTickNeighborhood(key.x(), key.z())) {
            scheduleBlockTick(key.x(), key.y(), key.z(), key.type(),
                    getTickDelay(key.type(), getBlockMetadataIfLoaded(key.x(), key.y(), key.z(), 0)));
            return false;
        }
        tickScheduledBlock(key.x(), key.y(), key.z(), key.type());
        return true;
    }

    private boolean hasLoadedHorizontalTickNeighborhood(int x, int z) {
        return isChunkGeneratedForBlock(x, z)
                && isChunkGeneratedForBlock(x + 1, z)
                && isChunkGeneratedForBlock(x - 1, z)
                && isChunkGeneratedForBlock(x, z + 1)
                && isChunkGeneratedForBlock(x, z - 1);
    }

    private boolean requiresHorizontalTickNeighborhood(BlockType type) {
        return type.isFluid() || type == BlockType.FIRE;
    }

    public int getScheduledBlockTickCount() {
        return scheduledTickKeys.size();
    }

    public boolean hasScheduledBlockTick(int x, int y, int z, BlockType type) {
        return scheduledTickKeys.contains(new ScheduledTickKey(x, y, z, type));
    }

    public long getBlockTickClock() {
        return blockTickClock;
    }

    public List<ScheduledBlockTickState> getScheduledBlockTickStates() {
        List<ScheduledBlockTick> ticks = new ArrayList<>(scheduledBlockTicks);
        ticks.sort(null);

        List<ScheduledBlockTickState> states = new ArrayList<>();
        for (ScheduledBlockTick tick : ticks) {
            if (!scheduledTickKeys.contains(tick.key())) {
                continue;
            }
            ScheduledTickKey key = tick.key();
            long remaining = Math.max(0L, tick.dueTick() - blockTickClock);
            int delay = remaining > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) remaining;
            states.add(new ScheduledBlockTickState(key.x(), key.y(), key.z(), key.type(), delay));
        }
        return states;
    }

    public void replaceScheduledBlockTicks(Collection<ScheduledBlockTickState> states) {
        scheduledBlockTicks.clear();
        scheduledTickKeys.clear();
        if (states != null) {
            for (ScheduledBlockTickState state : states) {
                if (state != null) {
                    scheduleBlockTick(state.x(), state.y(), state.z(), state.type(), state.delayTicks());
                }
            }
        }
        rescheduleMovingPistonCompletionTicks();
    }

    public void scheduleBlockTick(int x, int y, int z, BlockType type, int delayTicks) {
        if (type == null || !isTickableBlock(type) || y < 0 || y >= Chunk.HEIGHT) {
            return;
        }
        ScheduledTickKey key = new ScheduledTickKey(x, y, z, type);
        long dueTick = blockTickClock + Math.max(0, delayTicks);
        if (!scheduledTickKeys.add(key)) {
            if (shouldPreemptScheduledTick(key, dueTick)) {
                scheduledBlockTicks.removeIf(tick -> tick.key().equals(key));
                scheduledBlockTicks.add(new ScheduledBlockTick(dueTick, nextBlockTickSequence++, key));
            }
            return;
        }
        scheduledBlockTicks.add(new ScheduledBlockTick(dueTick, nextBlockTickSequence++, key));
    }

    private boolean shouldPreemptScheduledTick(ScheduledTickKey key, long dueTick) {
        long existingDueTick = Long.MAX_VALUE;
        for (ScheduledBlockTick tick : scheduledBlockTicks) {
            if (tick.key().equals(key)) {
                existingDueTick = Math.min(existingDueTick, tick.dueTick());
            }
        }
        return dueTick < existingDueTick;
    }

    private void clearScheduledBlockTick(int x, int y, int z, BlockType type) {
        ScheduledTickKey key = new ScheduledTickKey(x, y, z, type);
        scheduledTickKeys.remove(key);
        scheduledBlockTicks.removeIf(tick -> tick.key().equals(key));
    }

    private void removeScheduledTicksForChunk(Chunk chunk) {
        int minX = chunk.getWorldX();
        int maxX = minX + Chunk.WIDTH;
        int minZ = chunk.getWorldZ();
        int maxZ = minZ + Chunk.DEPTH;
        scheduledTickKeys.removeIf(key -> key.x() >= minX && key.x() < maxX
                && key.z() >= minZ && key.z() < maxZ);
        scheduledBlockTicks.removeIf(tick -> tick.key().x() >= minX && tick.key().x() < maxX
                && tick.key().z() >= minZ && tick.key().z() < maxZ);
    }

    public void scheduleMechanismUpdatesAround(int x, int y, int z) {
        RedstoneEngine.scheduleAround(this, x, y, z);
    }

    public void schedulePressurePlateUpdatesForAabb(AABB box, boolean includeStonePlates) {
        if (box == null) {
            return;
        }
        int minX = (int) Math.floor(box.getMin().x);
        int maxX = (int) Math.floor(box.getMax().x);
        int minZ = (int) Math.floor(box.getMin().z);
        int maxZ = (int) Math.floor(box.getMax().z);
        int minY = (int) Math.floor(box.getMin().y - 0.125f);
        int maxY = (int) Math.floor(box.getMin().y + 0.125f);
        for (int y = minY; y <= maxY; y++) {
            if (y < 0 || y >= Chunk.HEIGHT) {
                continue;
            }
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    BlockType type = getBlockIfLoaded(x, y, z, BlockType.AIR);
                    if (type == BlockType.WOODEN_PRESSURE_PLATE
                            || (includeStonePlates && type == BlockType.STONE_PRESSURE_PLATE)) {
                        scheduleBlockTick(x, y, z, type, 0);
                    }
                }
            }
        }
    }

    public enum BedUseOutcome {
        NOT_BED,
        NOT_NIGHT,
        OCCUPIED,
        MONSTERS_NEARBY,
        EXPLODED,
        SLEEP_ALLOWED
    }

    public record BedUseResult(BedUseOutcome outcome, BlockPos footPos, BlockPos headPos) {
        public boolean sleepAllowed() {
            return outcome == BedUseOutcome.SLEEP_ALLOWED;
        }
    }

    private record DeferredBlockChange(int x, int y, int z,
            BlockType previous, int previousMetadata,
            BlockType current, int currentMetadata) {
    }

    private record BedParts(BlockPos foot, BlockPos head, int footMetadata, int headMetadata) {
    }

    private record NetherPortalInterior(int minX, int minY, int minZ, int axis) {
    }

    public void schedulePressurePlateUpdatesAt(float x, float y, float z, boolean includeStonePlates) {
        schedulePressurePlateUpdatesForAabb(new AABB(x - 0.05f, y - 0.05f, z - 0.05f,
                x + 0.05f, y + 0.05f, z + 0.05f), includeStonePlates);
    }

    public boolean isBlockPowered(int x, int y, int z) {
        return RedstoneEngine.isBlockPowered(this, x, y, z);
    }

    public int getWeakPower(int x, int y, int z, int towardFace) {
        return RedstoneEngine.getWeakPower(this, x, y, z, towardFace);
    }

    public int getStrongPower(int x, int y, int z, int towardFace) {
        return RedstoneEngine.getStrongPower(this, x, y, z, towardFace);
    }

    private void scheduleTickableBlocksInChunk(Chunk chunk) {
        int worldX = chunk.getChunkX() * Chunk.WIDTH;
        int worldZ = chunk.getChunkZ() * Chunk.DEPTH;
        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int x = 0; x < Chunk.WIDTH; x++) {
                    BlockType type = chunk.getBlock(x, y, z);
                    int bx = worldX + x;
                    int bz = worldZ + z;
                    if (type.isFlowingFluid() || type == BlockType.FIRE
                            || type == BlockType.FARMLAND || type.isCrop()
                            || isGrassLikeBlock(type)
                            || isGrowingPlantBlock(type)
                            || type == BlockType.VINES
                            || type == BlockType.SNOW_LAYER
                            || type == BlockType.ICE
                            || type == BlockType.LEAVES
                            || type == BlockType.LOCKED_CHEST
                            || type == BlockType.GLOWING_REDSTONE_ORE
                            || RedstoneEngine.isRedstoneTickable(type)) {
                        RedstoneEngine.rememberPoweredOpenableState(this, bx, y, bz);
                        scheduleBlockTick(bx, y, bz, type, getTickDelay(type, chunk.getBlockMetadata(x, y, z)));
                    }
                }
            }
        }
    }

    private boolean isTickableBlock(BlockType type) {
        return type.isFluid() || type == BlockType.FIRE || type.isFallingBlock()
                || type == BlockType.MOVING_PISTON
                || type == BlockType.FARMLAND || type.isCrop()
                || isGrassLikeBlock(type)
                || isGrowingPlantBlock(type)
                || type == BlockType.VINES
                || type == BlockType.SNOW_LAYER
                || type == BlockType.ICE
                || type == BlockType.LEAVES
                || type == BlockType.LOCKED_CHEST
                || type == BlockType.GLOWING_REDSTONE_ORE
                || RedstoneEngine.isRedstoneTickable(type);
    }

    private boolean isGrowingPlantBlock(BlockType type) {
        return type == BlockType.CACTUS || type == BlockType.SUGAR_CANE || type == BlockType.NETHER_WART;
    }

    private boolean isGrassLikeBlock(BlockType type) {
        return type == BlockType.GRASS || type == BlockType.MYCELIUM;
    }

    private int getTickDelay(BlockType type) {
        return getTickDelay(type, 0);
    }

    private int getTickDelay(BlockType type, int metadata) {
        if (type.isWater()) {
            return WATER_TICK_DELAY;
        }
        if (type.isLava()) {
            return LAVA_TICK_DELAY;
        }
        if (type == BlockType.FIRE) {
            return FIRE_TICK_DELAY;
        }
        if (type.isFallingBlock()) {
            return FALLING_BLOCK_TICK_DELAY;
        }
        if (type == BlockType.MOVING_PISTON) {
            return RedstoneEngine.PISTON_MOVEMENT_TICKS;
        }
        if (type == BlockType.FARMLAND) {
            return FARMLAND_TICK_DELAY;
        }
        if (type.isCrop()) {
            return CROP_TICK_DELAY;
        }
        if (isGrassLikeBlock(type)) {
            return GRASS_LIKE_TICK_DELAY + random.nextInt(GRASS_LIKE_TICK_DELAY);
        }
        if (isGrowingPlantBlock(type)) {
            return PLANT_GROWTH_TICK_DELAY;
        }
        if (type == BlockType.VINES) {
            return VINE_TICK_DELAY + random.nextInt(VINE_TICK_DELAY);
        }
        if (type == BlockType.SNOW_LAYER) {
            return SNOW_LAYER_TICK_DELAY;
        }
        if (type == BlockType.ICE) {
            return ICE_TICK_DELAY;
        }
        if (type == BlockType.LEAVES) {
            return LEAF_DECAY_TICK_DELAY;
        }
        if (type == BlockType.LOCKED_CHEST) {
            return LOCKED_CHEST_DECAY_TICK_DELAY;
        }
        if (type == BlockType.GLOWING_REDSTONE_ORE) {
            return REDSTONE_ORE_GLOW_TICK_DELAY;
        }
        if (RedstoneEngine.isRedstoneTickable(type)) {
            return RedstoneEngine.getTickDelay(type, metadata);
        }
        return 1;
    }

    private void tickScheduledBlock(int x, int y, int z, BlockType type) {
        if (type.isFluid()) {
            updateFluidBlock(x, y, z, type);
        } else if (type == BlockType.FIRE) {
            updateFireBlock(x, y, z);
        } else if (type.isFallingBlock()) {
            updateFallingBlock(x, y, z, type);
        } else if (type == BlockType.MOVING_PISTON) {
            completeMovingPiston(x, y, z);
        } else if (type == BlockType.FARMLAND) {
            updateFarmlandBlock(x, y, z);
        } else if (type.isCrop()) {
            updateCropBlock(x, y, z, type);
        } else if (isGrassLikeBlock(type)) {
            updateGrassLikeBlock(x, y, z, type);
        } else if (type == BlockType.CACTUS || type == BlockType.SUGAR_CANE) {
            updateColumnPlantBlock(x, y, z, type);
        } else if (type == BlockType.NETHER_WART) {
            updateNetherWartBlock(x, y, z);
        } else if (type == BlockType.VINES) {
            updateVineBlock(x, y, z);
        } else if (type == BlockType.SNOW_LAYER) {
            updateSnowLayerBlock(x, y, z);
        } else if (type == BlockType.ICE) {
            updateIceBlock(x, y, z);
        } else if (type == BlockType.LEAVES) {
            updateLeafBlock(x, y, z);
        } else if (type == BlockType.LOCKED_CHEST) {
            updateLockedChestBlock(x, y, z);
        } else if (type == BlockType.GLOWING_REDSTONE_ORE) {
            updateGlowingRedstoneOre(x, y, z);
        } else if (RedstoneEngine.isRedstoneTickable(type)) {
            RedstoneEngine.tick(this, x, y, z, type);
        }
    }

    private void notifyBlockChanged(int x, int y, int z, BlockType previous, int previousMetadata,
            BlockType current, int currentMetadata) {
        applyBlockChangeSideEffects(x, y, z, previous, previousMetadata, current, currentMetadata);
        notifyBlockChangeListeners(x, y, z, previous, previousMetadata, current, currentMetadata);
    }

    private void applyBlockChangeSideEffects(int x, int y, int z, BlockType previous, int previousMetadata,
            BlockType current, int currentMetadata) {
        if (previous != current) {
            RedstoneEngine.clearBlockRuntimeState(this, x, y, z, previous, previousMetadata,
                    current, currentMetadata);
        }
        scheduleBlockTick(x, y, z, current, getTickDelay(current, currentMetadata));
        updateNeighborSupport(x, y, z);
        scheduleNeighborBlockUpdates(x, y, z);
        scheduleImmediateNeighborFluidUpdates(x, y, z);
        scheduleMechanismUpdatesAround(x, y, z);
        tryMixFluidsAround(x, y, z);
        if (current.isLava()) {
            igniteAroundLava(x, y, z);
        }
        if (current == BlockType.FIRE) {
            tryActivateNetherPortalFromFire(x, y, z);
        }
        if (previous == BlockType.OAK_LOG && current != BlockType.OAK_LOG) {
            markLeavesForDecayAroundLog(x, y, z);
        }
        tryCreateSnowGolemFromChangedBlock(x, y, z, current);
    }

    private void deferBlockChange(List<DeferredBlockChange> changes,
            int x, int y, int z, BlockType type, int metadata) {
        DeferredBlockChange change = setBlockWithDeferredEffects(x, y, z, type, metadata);
        if (change != null) {
            changes.add(change);
        }
    }

    private DeferredBlockChange setBlockWithDeferredEffects(int x, int y, int z, BlockType type, int metadata) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return null;
        }
        BlockType previous = getBlock(x, y, z);
        int previousMetadata = getBlockMetadata(x, y, z);
        if (previous == type && previousMetadata == metadata) {
            return null;
        }
        setBlock(x, y, z, type, metadata);
        return new DeferredBlockChange(x, y, z, previous, previousMetadata, type, metadata);
    }

    private void flushDeferredBlockEffects(List<DeferredBlockChange> changes) {
        for (DeferredBlockChange change : changes) {
            applyBlockChangeSideEffects(change.x(), change.y(), change.z(),
                    change.previous(), change.previousMetadata(),
                    change.current(), change.currentMetadata());
        }
    }

    private void notifyBlockChangeListeners(int x, int y, int z, BlockType previous, int previousMetadata,
            BlockType current, int currentMetadata) {
        for (BlockChangeListener listener : blockChangeListeners) {
            listener.onBlockChanged(x, y, z, previous, previousMetadata, current, currentMetadata);
        }
    }

    private void notifyExplosionDamageListeners(float x, float y, float z, float power) {
        for (DamageEventListener listener : damageEventListeners) {
            listener.onExplosion(x, y, z, power);
        }
    }

    private void notifyLightningDamageListeners(float x, float y, float z) {
        for (DamageEventListener listener : damageEventListeners) {
            listener.onLightning(x, y, z);
        }
    }

    private void tryCreateSnowGolemFromChangedBlock(int x, int y, int z, BlockType current) {
        if (current == BlockType.PUMPKIN || current == BlockType.JACK_O_LANTERN) {
            tryCreateSnowGolem(x, y, z);
        } else if (current == BlockType.SNOW) {
            tryCreateSnowGolem(x, y + 1, z);
            tryCreateSnowGolem(x, y + 2, z);
        }
    }

    private boolean tryCreateSnowGolem(int x, int pumpkinY, int z) {
        BlockType pumpkin = getBlockIfLoaded(x, pumpkinY, z, BlockType.AIR);
        if (pumpkin != BlockType.PUMPKIN && pumpkin != BlockType.JACK_O_LANTERN) {
            return false;
        }
        int upperSnowY = pumpkinY - 1;
        int lowerSnowY = pumpkinY - 2;
        if (lowerSnowY < 0
                || getBlockIfLoaded(x, upperSnowY, z, BlockType.AIR) != BlockType.SNOW
                || getBlockIfLoaded(x, lowerSnowY, z, BlockType.AIR) != BlockType.SNOW) {
            return false;
        }
        Mob golem = MobFactory.create(MobDefinition.SNOW_GOLEM);
        if (golem == null) {
            return false;
        }
        setBlockIfLoaded(x, pumpkinY, z, BlockType.AIR, 0);
        setBlockIfLoaded(x, upperSnowY, z, BlockType.AIR, 0);
        setBlockIfLoaded(x, lowerSnowY, z, BlockType.AIR, 0);
        golem.setPosition(x + 0.5f, lowerSnowY, z + 0.5f);
        spawnEntity(golem);
        spawnSnowGolemCreationParticles(x, lowerSnowY, z);
        return true;
    }

    private void scheduleNeighborBlockUpdates(int x, int y, int z) {
        int[][] dirs = {
                { 0, 0, 0 },
                { 1, 0, 0 }, { -1, 0, 0 },
                { 0, 1, 0 }, { 0, -1, 0 },
                { 0, 0, 1 }, { 0, 0, -1 }
        };
        for (int[] dir : dirs) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            int nz = z + dir[2];
            BlockType neighbor = getBlockIfLoaded(nx, ny, nz, BlockType.AIR);
            if (neighbor == BlockType.LEAVES) {
                markLeafForDecay(nx, ny, nz);
            }
            if (neighbor.isFluid() || neighbor == BlockType.FIRE
                    || RedstoneEngine.isRedstoneTickable(neighbor)
                    || neighbor == BlockType.FARMLAND || neighbor.isCrop()
                    || isGrowingPlantBlock(neighbor)
                    || neighbor == BlockType.VINES
                    || neighbor == BlockType.SNOW_LAYER
                    || neighbor == BlockType.ICE
                    || neighbor == BlockType.LOCKED_CHEST
                    || neighbor == BlockType.GLOWING_REDSTONE_ORE
                    || (neighbor.isFallingBlock() && ny > 0
                            && BlockShape.canFallingBlockFallThrough(getBlockIfLoaded(nx, ny - 1, nz, BlockType.AIR)))) {
                scheduleBlockTick(nx, ny, nz, neighbor,
                        getTickDelay(neighbor, getBlockMetadataIfLoaded(nx, ny, nz, 0)));
            }
            if (neighbor.isLava()) {
                igniteAroundLava(nx, ny, nz);
            }
        }
    }

    private void scheduleImmediateNeighborFluidUpdates(int x, int y, int z) {
        int[][] dirs = {
                { 1, 0, 0 }, { -1, 0, 0 },
                { 0, 1, 0 }, { 0, -1, 0 },
                { 0, 0, 1 }, { 0, 0, -1 }
        };
        for (int[] dir : dirs) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            int nz = z + dir[2];
            BlockType neighbor = getBlockIfLoaded(nx, ny, nz, BlockType.AIR);
            if (neighbor.isFluid()) {
                scheduleBlockTick(nx, ny, nz, neighbor, 0);
            }
        }
    }

    void startMovingPiston(int x, int y, int z, int metadata,
            BlockType carriedType, int carriedMetadata,
            BlockType finalType, int finalMetadata,
            float fromX, float fromY, float fromZ,
            float toX, float toY, float toZ) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return;
        }
        BlockPos pos = new BlockPos(x, y, z);
        MovingPistonState state = new MovingPistonState(x, y, z, metadata & 7,
                carriedType == null ? BlockType.AIR : carriedType,
                carriedType == null ? 0 : carriedMetadata,
                finalType == null ? BlockType.AIR : finalType,
                finalType == null ? 0 : finalMetadata,
                fromX, fromY, fromZ, toX, toY, toZ, blockTickClock, false);
        movingPistons.put(pos, state);
        if (!setBlockIfLoaded(x, y, z, BlockType.MOVING_PISTON, metadata)) {
            movingPistons.remove(pos);
            return;
        }
        rescheduleBlockTick(x, y, z, BlockType.MOVING_PISTON, RedstoneEngine.PISTON_MOVEMENT_TICKS);
    }

    public MovingPistonState getMovingPistonState(int x, int y, int z) {
        return movingPistons.get(new BlockPos(x, y, z));
    }

    public List<MovingPistonState> getMovingPistonStates() {
        List<MovingPistonState> states = new ArrayList<>(movingPistons.values());
        states.sort((left, right) -> {
            int xCompare = Integer.compare(left.x(), right.x());
            if (xCompare != 0) {
                return xCompare;
            }
            int yCompare = Integer.compare(left.y(), right.y());
            return yCompare != 0 ? yCompare : Integer.compare(left.z(), right.z());
        });
        return states;
    }

    public void replaceMovingPistonStates(Collection<MovingPistonState> states) {
        movingPistons.clear();
        if (states == null) {
            return;
        }
        for (MovingPistonState state : states) {
            if (state != null) {
                movingPistons.put(new BlockPos(state.x(), state.y(), state.z()), state);
            }
        }
        rescheduleMovingPistonCompletionTicks();
    }

    private void rescheduleMovingPistonCompletionTicks() {
        for (MovingPistonState state : movingPistons.values()) {
            if (state == null || state.y() < 0 || state.y() >= Chunk.HEIGHT) {
                continue;
            }
            long elapsed = Math.max(0L, blockTickClock - state.startTick());
            int remaining = (int) Math.max(0L, RedstoneEngine.PISTON_MOVEMENT_TICKS - elapsed);
            rescheduleBlockTick(state.x(), state.y(), state.z(), BlockType.MOVING_PISTON, remaining);
        }
    }

    private void completeMovingPiston(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        MovingPistonState state = movingPistons.remove(pos);
        if (getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.MOVING_PISTON) {
            return;
        }
        if (state == null) {
            setBlockIfLoaded(x, y, z, BlockType.AIR, 0);
            return;
        }
        if (setBlockIfLoaded(x, y, z, state.finalType(), state.finalMetadata())
                && state.finalType() != BlockType.AIR
                && getBlockIfLoaded(x, y, z, BlockType.AIR) == state.finalType()
                && !isBlockSupportedIfLoaded(x, y, z)) {
            breakBlock(x, y, z, true);
        }
    }

    void rescheduleBlockTick(int x, int y, int z, BlockType type, int delayTicks) {
        ScheduledTickKey key = new ScheduledTickKey(x, y, z, type);
        scheduledTickKeys.remove(key);
        scheduledBlockTicks.removeIf(tick -> tick.key().equals(key));
        scheduleBlockTick(x, y, z, type, delayTicks);
    }

    public boolean isFluidSource(int x, int y, int z) {
        BlockType type = getBlockIfLoaded(x, y, z, BlockType.AIR);
        return type.isFluid() && FluidState.isSource(getBlockMetadataIfLoaded(x, y, z, 0));
    }

    public ItemType pickupFluidSource(int x, int y, int z) {
        BlockType type = getBlockIfLoaded(x, y, z, BlockType.AIR);
        if (!type.isFluid() || !FluidState.isSource(getBlockMetadataIfLoaded(x, y, z, 0))) {
            return null;
        }
        ItemType filled = type.isWater() ? ItemType.WATER_BUCKET : ItemType.LAVA_BUCKET;
        return setBlockIfLoaded(x, y, z, BlockType.AIR, 0) ? filled : null;
    }

    public boolean placeFluidSource(int x, int y, int z, boolean water, AABB playerBox) {
        BlockType source = water ? BlockType.WATER : BlockType.LAVA;
        if (y < 0 || y >= Chunk.HEIGHT) {
            return false;
        }
        BlockType target = getBlockIfLoaded(x, y, z, BlockType.BEDROCK);
        if (!canFluidDisplace(x, y, z, water)) {
            return false;
        }
        if (water && dimension == Dimension.NETHER) {
            playNetherWaterEvaporationFeedback(x, y, z);
            return true;
        }
        if (isFluidSourceMixTarget(target, water)) {
            return placeFluidSourceMix(x, y, z, target, water, playerBox);
        }
        if (playerBox != null) {
            for (AABB box : getPlacementCollisionBoxesIfLoaded(x, y, z, source, 0)) {
                if (box.intersects(playerBox)) {
                    return false;
                }
            }
        }
        displaceBlockForFluid(x, y, z, target, water);
        if (!setBlockIfLoaded(x, y, z, source, 0)) {
            return false;
        }
        tryMixFluidsAround(x, y, z);
        return true;
    }

    private boolean isFluidSourceMixTarget(BlockType target, boolean water) {
        return water ? target.isLava() : target.isWater();
    }

    private boolean placeFluidSourceMix(int x, int y, int z, BlockType target, boolean water, AABB playerBox) {
        BlockType mixed = null;
        if (water && target.isLava()) {
            mixed = lavaHardenedByWater(getBlockMetadataIfLoaded(x, y, z, 0));
        } else if (!water && target.isWater()) {
            mixed = BlockType.COBBLESTONE;
        }
        if (mixed == null) {
            return false;
        }
        if (playerBox != null) {
            for (AABB box : getPlacementCollisionBoxesIfLoaded(x, y, z, mixed, 0)) {
                if (box.intersects(playerBox)) {
                    return false;
                }
            }
        }
        if (setBlockIfLoaded(x, y, z, mixed, 0)) {
            playLavaMixEffects(x, y, z);
            return true;
        }
        return false;
    }

    private void playNetherWaterEvaporationFeedback(int x, int y, int z) {
        playSound(WorldSoundEvent.FIZZ, x + 0.5f, y + 0.5f, z + 0.5f,
                0.5f, redstoneTorchBurnoutPitch());
        for (int i = 0; i < NETHER_WATER_EVAPORATION_SMOKE_PARTICLES; i++) {
            spawnParticle(WorldParticle.Type.LARGE_SMOKE,
                    x + random.nextFloat(),
                    y + random.nextFloat(),
                    z + random.nextFloat(),
                    0.0f, 0.0f, 0.0f,
                    0.30f, 22);
        }
    }

    public int getCauldronLevel(int x, int y, int z) {
        if (getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.CAULDRON) {
            return 0;
        }
        return Math.max(0, Math.min(CAULDRON_MAX_LEVEL, getBlockMetadataIfLoaded(x, y, z, 0)));
    }

    public boolean fillCauldronFromWaterBucket(int x, int y, int z) {
        if (getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.CAULDRON) {
            return false;
        }
        if (getCauldronLevel(x, y, z) >= CAULDRON_MAX_LEVEL) {
            return false;
        }
        return setBlockIfLoaded(x, y, z, BlockType.CAULDRON, CAULDRON_MAX_LEVEL);
    }

    public boolean drainCauldronIntoBottle(int x, int y, int z) {
        if (getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.CAULDRON) {
            return false;
        }
        int level = getCauldronLevel(x, y, z);
        if (level <= 0) {
            return false;
        }
        return setBlockIfLoaded(x, y, z, BlockType.CAULDRON, level - 1);
    }

    public boolean placeLilyPadOnWater(int waterX, int waterY, int waterZ, AABB playerBox) {
        BlockType water = getBlockIfLoaded(waterX, waterY, waterZ, BlockType.AIR);
        if (!water.isWater() || getBlockMetadataIfLoaded(waterX, waterY, waterZ, -1) != 0) {
            return false;
        }
        int padY = waterY + 1;
        if (!canPlaceBlockAtIfLoaded(waterX, padY, waterZ, BlockType.LILY_PAD, 0)) {
            return false;
        }
        if (playerBox != null) {
            for (AABB box : getPlacementCollisionBoxesIfLoaded(waterX, padY, waterZ, BlockType.LILY_PAD, 0)) {
                if (box.intersects(playerBox)) {
                    return false;
                }
            }
        }
        if (!setBlockIfLoaded(waterX, padY, waterZ, BlockType.LILY_PAD, 0)) {
            return false;
        }
        return true;
    }

    public boolean tryFillCauldronFromRainAt(int x, int y, int z) {
        return false;
    }

    public int getCakeBites(int x, int y, int z) {
        if (getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.CAKE) {
            return 0;
        }
        return Math.max(0, Math.min(CAKE_LAST_BITE_METADATA, getBlockMetadataIfLoaded(x, y, z, 0)));
    }

    public boolean eatCakeSlice(int x, int y, int z) {
        if (getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.CAKE) {
            return false;
        }
        int bites = getCakeBites(x, y, z);
        if (bites >= CAKE_LAST_BITE_METADATA) {
            setBlock(x, y, z, BlockType.AIR, 0);
        } else {
            setBlock(x, y, z, BlockType.CAKE, bites + 1);
        }
        return true;
    }

    public BlockPos teleportDragonEgg(int x, int y, int z) {
        if (getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.DRAGON_EGG) {
            return null;
        }
        int metadata = getBlockMetadataIfLoaded(x, y, z, 0);
        for (int attempt = 0; attempt < DRAGON_EGG_TELEPORT_ATTEMPTS; attempt++) {
            int nx = x + random.nextInt(DRAGON_EGG_HORIZONTAL_TELEPORT_BOUND)
                    - random.nextInt(DRAGON_EGG_HORIZONTAL_TELEPORT_BOUND);
            int ny = y + random.nextInt(DRAGON_EGG_VERTICAL_TELEPORT_BOUND)
                    - random.nextInt(DRAGON_EGG_VERTICAL_TELEPORT_BOUND);
            int nz = z + random.nextInt(DRAGON_EGG_HORIZONTAL_TELEPORT_BOUND)
                    - random.nextInt(DRAGON_EGG_HORIZONTAL_TELEPORT_BOUND);
            if (ny < 0 || ny >= Chunk.HEIGHT) {
                continue;
            }
            if (getBlockIfLoaded(nx, ny, nz, null) != BlockType.AIR) {
                continue;
            }
            setBlock(x, y, z, BlockType.AIR, 0);
            setBlock(nx, ny, nz, BlockType.DRAGON_EGG, metadata);
            spawnDragonEggTeleportParticles(x, y, z, nx, ny, nz);
            return new BlockPos(nx, ny, nz);
        }
        return null;
    }

    private void spawnDragonEggTeleportParticles(int oldX, int oldY, int oldZ, int newX, int newY, int newZ) {
        for (int i = 0; i < DRAGON_EGG_PORTAL_PARTICLES; i++) {
            double progress = random.nextDouble();
            float motionX = (random.nextFloat() - 0.5f) * 0.2f;
            float motionY = (random.nextFloat() - 0.5f) * 0.2f;
            float motionZ = (random.nextFloat() - 0.5f) * 0.2f;
            float px = (float) (newX + (oldX - newX) * progress
                    + (random.nextDouble() - 0.5d) + 0.5d);
            float py = (float) (newY + (oldY - newY) * progress
                    + random.nextDouble() - 0.5d);
            float pz = (float) (newZ + (oldZ - newZ) * progress
                    + (random.nextDouble() - 0.5d) + 0.5d);
            spawnParticle(WorldParticle.Type.PORTAL, px, py, pz, motionX, motionY, motionZ, 0.25f, 40);
        }
    }

    public int getCropAge(int x, int y, int z) {
        if (getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.CROPS) {
            return 0;
        }
        return Math.max(0, Math.min(MAX_CROP_AGE, getBlockMetadataIfLoaded(x, y, z, 0)));
    }

    public boolean applyBoneMealToCrop(int x, int y, int z) {
        if (getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.CROPS) {
            return false;
        }
        int age = getCropAge(x, y, z);
        if (age >= MAX_CROP_AGE) {
            return false;
        }
        setBlock(x, y, z, BlockType.CROPS, MAX_CROP_AGE);
        return true;
    }

    public boolean applyBoneMealToStem(int x, int y, int z) {
        BlockType type = getBlockIfLoaded(x, y, z, BlockType.AIR);
        if (!isStem(type)) {
            return false;
        }
        int age = getCropAgeValue(x, y, z);
        if (age >= MAX_CROP_AGE) {
            return false;
        }
        setBlock(x, y, z, type, Math.min(MAX_CROP_AGE, age + 2 + random.nextInt(4)));
        return true;
    }

    public boolean applyBoneMealToPlant(int x, int y, int z) {
        BlockType type = getBlockIfLoaded(x, y, z, BlockType.AIR);
        boolean applied;
        if (type == BlockType.CROPS) {
            applied = applyBoneMealToCrop(x, y, z);
        } else if (isStem(type)) {
            applied = applyBoneMealToStem(x, y, z);
        } else if (type == BlockType.SAPLING) {
            applied = applyBoneMealToSapling(x, y, z);
        } else if (type == BlockType.GRASS) {
            applied = applyBoneMealToGrass(x, y, z);
        } else if (type == BlockType.BROWN_MUSHROOM || type == BlockType.RED_MUSHROOM) {
            applied = applyBoneMealToMushroom(x, y, z);
        } else {
            applied = false;
        }
        return applied;
    }

    public boolean applyBoneMealToGrass(int x, int y, int z) {
        if (getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.GRASS) {
            return false;
        }
        for (int attempt = 0; attempt < GRASS_BONE_MEAL_ATTEMPTS; attempt++) {
            int plantX = x;
            int plantY = y + 1;
            int plantZ = z;
            boolean valid = true;
            for (int step = 0; step < attempt / 16; step++) {
                plantX += random.nextInt(3) - 1;
                plantY += (random.nextInt(3) - 1) * random.nextInt(3) / 2;
                plantZ += random.nextInt(3) - 1;
                if (getBlockIfLoaded(plantX, plantY - 1, plantZ, BlockType.AIR) != BlockType.GRASS
                        || getBlockIfLoaded(plantX, plantY, plantZ, BlockType.AIR).isSolid()) {
                    valid = false;
                    break;
                }
            }
            if (!valid || plantY < 0 || plantY >= Chunk.HEIGHT
                    || getBlockIfLoaded(plantX, plantY, plantZ, BlockType.AIR) != BlockType.AIR) {
                continue;
            }

            BlockType plant;
            int metadata = 0;
            if (random.nextInt(10) != 0) {
                plant = BlockType.TALL_GRASS;
                metadata = 1;
            } else {
                plant = random.nextInt(3) != 0 ? BlockType.YELLOW_FLOWER : BlockType.RED_ROSE;
            }
            if (BlockShape.canPlaceAt(plant, metadata, contextAtIfLoaded(plantX, plantY, plantZ))) {
                setBlock(plantX, plantY, plantZ, plant, metadata);
            }
        }
        return true;
    }

    public boolean applyBoneMealToMushroom(int x, int y, int z) {
        BlockType type = getBlockIfLoaded(x, y, z, BlockType.AIR);
        if (type != BlockType.BROWN_MUSHROOM && type != BlockType.RED_MUSHROOM) {
            return false;
        }
        int metadata = getBlockMetadataIfLoaded(x, y, z, 0);
        setBlock(x, y, z, BlockType.AIR, 0);
        BlockType hugeType = type == BlockType.BROWN_MUSHROOM
                ? BlockType.BROWN_MUSHROOM_BLOCK
                : BlockType.RED_MUSHROOM_BLOCK;
        if (tryPlaceHugeMushroom(x, y, z, hugeType)) {
            return true;
        }
        setBlock(x, y, z, type, metadata);
        return false;
    }

    private boolean tryPlaceHugeMushroom(int x, int y, int z, BlockType hugeType) {
        boolean red = hugeType == BlockType.RED_MUSHROOM_BLOCK;
        int height = random.nextInt(3) + 4;
        if (y < 1 || y + height + 1 > Chunk.HEIGHT || !canHugeMushroomStay(x, y, z)) {
            return false;
        }

        for (int checkY = y; checkY <= y + height + 1; checkY++) {
            int radius = checkY == y ? 0 : 3;
            for (int checkX = x - radius; checkX <= x + radius; checkX++) {
                for (int checkZ = z - radius; checkZ <= z + radius; checkZ++) {
                    BlockType block = getBlockIfLoaded(checkX, checkY, checkZ, BlockType.AIR);
                    if (checkY < 0 || checkY >= Chunk.HEIGHT
                            || (block != BlockType.AIR && block != BlockType.LEAVES)) {
                        return false;
                    }
                }
            }
        }

        setBlock(x, y - 1, z, BlockType.DIRT, 0);
        int capStartY = red ? y + height - 3 : y + height;
        for (int capY = capStartY; capY <= y + height; capY++) {
            int radius = capY < y + height ? 2 : 1;
            if (!red) {
                radius = 3;
            }
            for (int capX = x - radius; capX <= x + radius; capX++) {
                for (int capZ = z - radius; capZ <= z + radius; capZ++) {
                    int capMetadata = hugeMushroomCapMetadata(x, z, capX, capZ, radius);
                    if (!red || capY < y + height) {
                        if ((capX == x - radius || capX == x + radius)
                                && (capZ == z - radius || capZ == z + radius)) {
                            continue;
                        }
                        capMetadata = hugeMushroomEdgeMetadata(x, z, capX, capZ, radius, capMetadata);
                    }
                    if (capMetadata == 5 && capY < y + height) {
                        capMetadata = 0;
                    }
                    if (capMetadata == 0 && y < y + height - 1) {
                        continue;
                    }
                    if (!getBlockIfLoaded(capX, capY, capZ, BlockType.AIR).isSolid()) {
                        setBlock(capX, capY, capZ, hugeType, capMetadata);
                    }
                }
            }
        }

        for (int dy = 0; dy < height; dy++) {
            int trunkY = y + dy;
            if (!getBlockIfLoaded(x, trunkY, z, BlockType.AIR).isSolid()) {
                setBlock(x, trunkY, z, hugeType, 10);
            }
        }
        return true;
    }

    private boolean canHugeMushroomStay(int x, int y, int z) {
        BlockType below = getBlockIfLoaded(x, y - 1, z, BlockType.AIR);
        if (below == BlockType.MYCELIUM) {
            return true;
        }
        if (below != BlockType.GRASS && below != BlockType.DIRT) {
            return false;
        }
        return Math.max(getSkyLight(x, y, z), getBlockLight(x, y, z)) < 13;
    }

    private static int hugeMushroomCapMetadata(int centerX, int centerZ, int x, int z, int radius) {
        int metadata = 5;
        if (x == centerX - radius) {
            metadata--;
        }
        if (x == centerX + radius) {
            metadata++;
        }
        if (z == centerZ - radius) {
            metadata -= 3;
        }
        if (z == centerZ + radius) {
            metadata += 3;
        }
        return metadata;
    }

    private static int hugeMushroomEdgeMetadata(int centerX, int centerZ, int x, int z, int radius, int metadata) {
        if (x == centerX - (radius - 1) && z == centerZ - radius) {
            metadata = 1;
        }
        if (x == centerX - radius && z == centerZ - (radius - 1)) {
            metadata = 1;
        }
        if (x == centerX + (radius - 1) && z == centerZ - radius) {
            metadata = 3;
        }
        if (x == centerX + radius && z == centerZ - (radius - 1)) {
            metadata = 3;
        }
        if (x == centerX - (radius - 1) && z == centerZ + radius) {
            metadata = 7;
        }
        if (x == centerX - radius && z == centerZ + (radius - 1)) {
            metadata = 7;
        }
        if (x == centerX + (radius - 1) && z == centerZ + radius) {
            metadata = 9;
        }
        if (x == centerX + radius && z == centerZ + (radius - 1)) {
            metadata = 9;
        }
        return metadata;
    }

    public boolean applyBoneMealToSapling(int x, int y, int z) {
        if (getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.SAPLING) {
            return false;
        }
        int treeMetadata = getBlockMetadataIfLoaded(x, y, z, 0) & 3;
        setBlock(x, y, z, BlockType.AIR, 0);
        boolean grown = false;
        if (treeMetadata == 1) {
            TreeFeature.Candidate tree = spruceSaplingTree(x, y, z);
            if (tree.canPlace((qx, qy, qz) -> getBlockIfLoaded(qx, qy, qz, BlockType.BEDROCK))) {
                tree.placeInto(this);
                grown = true;
            }
            if (!grown) {
                setBlock(x, y, z, BlockType.SAPLING, treeMetadata);
            }
            return true;
        }

        int height = (treeMetadata == 2 ? 5 : 4) + random.nextInt(3);
        TreeFeature.Candidate probe = new TreeFeature.Candidate(x, y, z, height, 0, treeMetadata);
        if (probe.canPlace((qx, qy, qz) -> getBlockIfLoaded(qx, qy, qz, BlockType.BEDROCK))) {
            TreeFeature.Candidate tree = new TreeFeature.Candidate(x, y, z, height, 0, treeMetadata,
                    TreeFeature.Kind.NORMAL, normalTreeCornerMask(random), 0, 0, 0);
            tree.placeInto(this);
            grown = true;
        }
        if (!grown) {
            setBlock(x, y, z, BlockType.SAPLING, treeMetadata);
        }
        return true;
    }

    private TreeFeature.Candidate spruceSaplingTree(int x, int y, int z) {
        int height = random.nextInt(4) + 6;
        int topOffset = 1 + random.nextInt(2);
        int maxRadius = 2 + random.nextInt(2);
        int initialRadius = random.nextInt(2);
        int trunkShorten = random.nextInt(3);
        return new TreeFeature.Candidate(x, y, z, height, 0, 1,
                TreeFeature.Kind.TAIGA2, topOffset, maxRadius, initialRadius, trunkShorten);
    }

    private int normalTreeCornerMask(Random random) {
        int mask = 0;
        for (int i = 0; i < 16; i++) {
            if (random.nextInt(2) == 0) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    public boolean activateRedstoneOre(int x, int y, int z) {
        BlockType type = getBlockIfLoaded(x, y, z, BlockType.AIR);
        if (type == BlockType.REDSTONE_ORE) {
            spawnRedstoneOreSparkleParticles(x, y, z);
            setBlock(x, y, z, BlockType.GLOWING_REDSTONE_ORE, getBlockMetadataIfLoaded(x, y, z, 0));
            scheduleBlockTick(x, y, z, BlockType.GLOWING_REDSTONE_ORE, REDSTONE_ORE_GLOW_TICK_DELAY);
            return true;
        }
        if (type == BlockType.GLOWING_REDSTONE_ORE) {
            spawnRedstoneOreSparkleParticles(x, y, z);
            scheduleBlockTick(x, y, z, BlockType.GLOWING_REDSTONE_ORE, REDSTONE_ORE_GLOW_TICK_DELAY);
            return true;
        }
        return false;
    }

    private void spawnRedstoneOreSparkleParticles(int x, int y, int z) {
        spawnRedstoneOreSparkleParticles(x, y, z, random);
    }

    private void spawnRedstoneOreSparkleParticles(int x, int y, int z, Random source) {
        for (int side = 0; side < 6; side++) {
            float particleX = x + source.nextFloat();
            float particleY = y + source.nextFloat();
            float particleZ = z + source.nextFloat();

            if (side == 0 && !isOpaqueCubeAt(x, y + 1, z)) {
                particleY = y + 1.0f + REDSTONE_ORE_SPARKLE_FACE_OFFSET;
            } else if (side == 1 && !isOpaqueCubeAt(x, y - 1, z)) {
                particleY = y - REDSTONE_ORE_SPARKLE_FACE_OFFSET;
            } else if (side == 2 && !isOpaqueCubeAt(x, y, z + 1)) {
                particleZ = z + 1.0f + REDSTONE_ORE_SPARKLE_FACE_OFFSET;
            } else if (side == 3 && !isOpaqueCubeAt(x, y, z - 1)) {
                particleZ = z - REDSTONE_ORE_SPARKLE_FACE_OFFSET;
            } else if (side == 4 && !isOpaqueCubeAt(x + 1, y, z)) {
                particleX = x + 1.0f + REDSTONE_ORE_SPARKLE_FACE_OFFSET;
            } else if (side == 5 && !isOpaqueCubeAt(x - 1, y, z)) {
                particleX = x - REDSTONE_ORE_SPARKLE_FACE_OFFSET;
            }

            if (particleX < x || particleX > x + 1.0f
                    || particleY < y || particleY > y + 1.0f
                    || particleZ < z || particleZ > z + 1.0f) {
                spawnParticle(WorldParticle.Type.RED_DUST,
                        particleX, particleY, particleZ,
                        0.0f, 0.0f, 0.0f,
                        0.08f, 18 + source.nextInt(8),
                        WorldParticle.RED_DUST_DEFAULT_COLOR_DATA);
            }
        }
    }

    private boolean isOpaqueCubeAt(int x, int y, int z) {
        return BlockShape.isOpaqueCube(getBlockIfLoaded(x, y, z, BlockType.AIR));
    }

    public void activateRedstoneOreBelow(AABB box) {
        if (box == null) {
            return;
        }
        int minX = (int) Math.floor(box.getMin().x);
        int maxX = (int) Math.floor(box.getMax().x - 0.0001f);
        int y = (int) Math.floor(box.getMin().y - 0.0001f);
        int minZ = (int) Math.floor(box.getMin().z);
        int maxZ = (int) Math.floor(box.getMax().z - 0.0001f);
        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                activateRedstoneOre(x, y, z);
            }
        }
    }

    public boolean canPlaceFallingBlockAt(int x, int y, int z, BlockType type, int metadata) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return false;
        }
        BlockType target = getBlock(x, y, z);
        if (!BlockShape.canFallingBlockReplace(target)) {
            return false;
        }
        return !BlockShape.canFallingBlockFallThrough(getBlock(x, y - 1, z));
    }

    private void updateFarmlandBlock(int x, int y, int z) {
        int moisture = Math.max(0, Math.min(FARMLAND_MAX_MOISTURE, getBlockMetadata(x, y, z)));
        if (hasFarmlandMoistureSource(x, y, z)) {
            if (moisture < FARMLAND_MAX_MOISTURE) {
                setBlock(x, y, z, BlockType.FARMLAND, FARMLAND_MAX_MOISTURE);
            }
        } else if (moisture > 0) {
            setBlock(x, y, z, BlockType.FARMLAND, moisture - 1);
        } else if (!hasCropAboveFarmland(x, y, z)) {
            setBlock(x, y, z, BlockType.DIRT, 0);
            return;
        }

        if (getBlockIfLoaded(x, y, z, BlockType.AIR) == BlockType.FARMLAND) {
            scheduleBlockTick(x, y, z, BlockType.FARMLAND, FARMLAND_TICK_DELAY);
        }
    }

    public boolean trampleFarmlandBelow(AABB box, float fallDistance) {
        if (box == null || fallDistance <= FARMLAND_TRAMPLE_MIN_FALL) {
            return false;
        }
        float chance = fallDistance - FARMLAND_TRAMPLE_MIN_FALL;
        if (chance < 1.0f && random.nextFloat() >= chance) {
            return false;
        }

        int y = (int) Math.floor(box.getMin().y - 0.0001f);
        int minX = (int) Math.floor(box.getMin().x);
        int maxX = (int) Math.floor(box.getMax().x - 0.0001f);
        int minZ = (int) Math.floor(box.getMin().z);
        int maxZ = (int) Math.floor(box.getMax().z - 0.0001f);
        boolean trampled = false;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (getBlockIfLoaded(x, y, z, BlockType.AIR) == BlockType.FARMLAND) {
                    setBlock(x, y, z, BlockType.DIRT, 0);
                    trampled = true;
                }
            }
        }
        return trampled;
    }

    private boolean hasFarmlandMoistureSource(int x, int y, int z) {
        return hasFarmlandWaterNearby(x, y, z) || isRainingAt(x, y + 1, z);
    }

    private boolean hasFarmlandWaterNearby(int x, int y, int z) {
        for (int nx = x - 4; nx <= x + 4; nx++) {
            for (int ny = y; ny <= y + 1; ny++) {
                for (int nz = z - 4; nz <= z + 4; nz++) {
                    if (getBlockIfLoaded(nx, ny, nz, BlockType.AIR).isWater()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasCropAboveFarmland(int x, int y, int z) {
        return getBlockIfLoaded(x, y + 1, z, BlockType.AIR).isCrop();
    }

    private void updateCropBlock(int x, int y, int z, BlockType type) {
        if (!BlockShape.canPlaceAt(type, getBlockMetadata(x, y, z), contextAt(x, y, z))) {
            breakBlock(x, y, z, true);
            return;
        }

        int age = getCropAgeValue(x, y, z);
        if (getCropLightLevel(x, y, z) >= 9) {
            float growthChance = getCropGrowthChance(x, y, z);
            int bound = Math.max(2, (int) (25.0f / growthChance) + 1);
            if (random.nextInt(bound) == 0) {
                if (age < MAX_CROP_AGE) {
                    setBlock(x, y, z, type, age + 1);
                } else if (isStem(type)) {
                    tryGrowStemFruit(x, y, z, type);
                }
            }
        }

        if (getBlockIfLoaded(x, y, z, BlockType.AIR) == type
                && (isStem(type) || getCropAgeValue(x, y, z) < MAX_CROP_AGE)) {
            scheduleBlockTick(x, y, z, type, CROP_TICK_DELAY);
        }
    }

    private int getCropAgeValue(int x, int y, int z) {
        return Math.max(0, Math.min(MAX_CROP_AGE, getBlockMetadataIfLoaded(x, y, z, 0)));
    }

    private static boolean isStem(BlockType type) {
        return type == BlockType.PUMPKIN_STEM || type == BlockType.MELON_STEM;
    }

    private void tryGrowStemFruit(int x, int y, int z, BlockType stem) {
        BlockType fruit = stem == BlockType.PUMPKIN_STEM ? BlockType.PUMPKIN : BlockType.MELON;
        for (int[] dir : HORIZONTAL_DIRS) {
            if (getBlockIfLoaded(x + dir[0], y, z + dir[1], BlockType.AIR) == fruit) {
                return;
            }
        }

        int[] dir = HORIZONTAL_DIRS[random.nextInt(HORIZONTAL_DIRS.length)];
        int fruitX = x + dir[0];
        int fruitZ = z + dir[1];
        if (getBlockIfLoaded(fruitX, y, fruitZ, BlockType.AIR) != BlockType.AIR) {
            return;
        }
        BlockType below = getBlockIfLoaded(fruitX, y - 1, fruitZ, BlockType.AIR);
        if (below == BlockType.GRASS || below == BlockType.DIRT || below == BlockType.FARMLAND) {
            setBlock(fruitX, y, fruitZ, fruit, 0);
        }
    }

    private void updateColumnPlantBlock(int x, int y, int z, BlockType type) {
        if (!BlockShape.canPlaceAt(type, getBlockMetadata(x, y, z), contextAt(x, y, z))) {
            breakBlock(x, y, z, true);
            return;
        }

        if (getBlockIfLoaded(x, y + 1, z, BlockType.AIR) == BlockType.AIR
                && getColumnPlantHeightBelow(x, y, z, type) < COLUMN_PLANT_MAX_HEIGHT) {
            int age = Math.max(0, Math.min(COLUMN_PLANT_MAX_AGE, getBlockMetadata(x, y, z)));
            if (age >= COLUMN_PLANT_MAX_AGE) {
                setBlock(x, y, z, type, 0);
                setBlock(x, y + 1, z, type, 0);
            } else if (random.nextInt(3) == 0) {
                setBlock(x, y, z, type, age + 1);
            }
        }

        if (getBlockIfLoaded(x, y, z, BlockType.AIR) == type) {
            scheduleBlockTick(x, y, z, type, PLANT_GROWTH_TICK_DELAY);
        }
    }

    private int getColumnPlantHeightBelow(int x, int y, int z, BlockType type) {
        int height = 1;
        while (height < COLUMN_PLANT_MAX_HEIGHT
                && getBlockIfLoaded(x, y - height, z, BlockType.AIR) == type) {
            height++;
        }
        return height;
    }

    private void updateNetherWartBlock(int x, int y, int z) {
        if (!BlockShape.canPlaceAt(BlockType.NETHER_WART, getBlockMetadata(x, y, z), contextAt(x, y, z))) {
            breakBlock(x, y, z, true);
            return;
        }

        int age = Math.max(0, Math.min(NETHER_WART_MAX_AGE, getBlockMetadata(x, y, z)));
        if (dimension == Dimension.NETHER && age < NETHER_WART_MAX_AGE && random.nextInt(10) == 0) {
            setBlock(x, y, z, BlockType.NETHER_WART, age + 1);
        }

        if (getBlockIfLoaded(x, y, z, BlockType.AIR) == BlockType.NETHER_WART
                && getBlockMetadataIfLoaded(x, y, z, 0) < NETHER_WART_MAX_AGE) {
            scheduleBlockTick(x, y, z, BlockType.NETHER_WART, PLANT_GROWTH_TICK_DELAY);
        }
    }

    private void queueModifiedChunkSave(Chunk chunk) {
        SaveManager manager = saveManager;
        Dimension targetDimension = dimension;
        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();
        chunk.calculateSkyLight();
        short[] blockIds = chunk.copyBlockIds();
        byte[] metadata = chunk.copyBlockMetadata();
        byte[] skyLight = chunk.copySkyLight();
        byte[] blockLight = chunk.copyBlockLight();
        int[] heightMap = chunk.copyHeightMap();
        SaveManager.DimensionRuntimeData runtimeData = manager == null
                ? null
                : manager.createChunkRuntimeDataSnapshot(this, chunkX, chunkZ);
        chunkSavePool.submit(() -> {
            try {
                synchronized (manager) {
                    manager.saveModifiedChunkData(targetDimension, chunkX, chunkZ, blockIds, metadata,
                            skyLight, blockLight, heightMap, runtimeData);
                }
            } catch (Exception e) {
                System.err.println("Failed to flush modified chunk before unload "
                        + chunkX + "," + chunkZ + ": " + e.getMessage());
            }
        });
    }

    private void updateGrassLikeBlock(int x, int y, int z, BlockType type) {
        if (getBlockIfLoaded(x, y, z, BlockType.AIR) != type) {
            return;
        }

        int aboveLight = getCombinedLightLevel(x, y + 1, z);
        BlockType above = getBlockIfLoaded(x, y + 1, z, BlockType.BEDROCK);
        if (aboveLight < GRASS_LIKE_DECAY_LIGHT
                && getGrassLikeCoverOpacity(above) > GRASS_LIKE_COVER_OPACITY_LIMIT) {
            setBlock(x, y, z, BlockType.DIRT, 0);
            return;
        }

        if (aboveLight >= GRASS_LIKE_SPREAD_SOURCE_LIGHT) {
            for (int attempt = 0; attempt < GRASS_LIKE_SPREAD_ATTEMPTS; attempt++) {
                int targetX = x + random.nextInt(3) - 1;
                int targetY = y + random.nextInt(5) - 3;
                int targetZ = z + random.nextInt(3) - 1;
                if (targetY < 0 || targetY + 1 >= Chunk.HEIGHT) {
                    continue;
                }
                if (getBlockIfLoaded(targetX, targetY, targetZ, BlockType.AIR) != BlockType.DIRT) {
                    continue;
                }
                BlockType targetAbove = getBlockIfLoaded(targetX, targetY + 1, targetZ, BlockType.BEDROCK);
                if (getCombinedLightLevel(targetX, targetY + 1, targetZ) >= GRASS_LIKE_SPREAD_TARGET_LIGHT
                        && getGrassLikeCoverOpacity(targetAbove) <= GRASS_LIKE_COVER_OPACITY_LIMIT) {
                    setBlock(targetX, targetY, targetZ, type, 0);
                }
            }
        }

        if (getBlockIfLoaded(x, y, z, BlockType.AIR) == type) {
            scheduleBlockTick(x, y, z, type, getTickDelay(type));
        }
    }

    private int getGrassLikeCoverOpacity(BlockType type) {
        if (type == null || type.isAir()) {
            return 0;
        }
        if (type.isFluid()) {
            return 3;
        }
        if (!type.isSolid()) {
            return 0;
        }
        if (type == BlockType.LEAVES || type == BlockType.GLASS || type == BlockType.GLASS_PANE
                || type == BlockType.ICE) {
            return 1;
        }
        if (type.isTransparent()) {
            return 2;
        }
        return BlockShape.isOpaqueCube(type) ? 3 : 2;
    }

    private void updateVineBlock(int x, int y, int z) {
        if (getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.VINES) {
            return;
        }
        int metadata = getBlockMetadataIfLoaded(x, y, z, 0) & 15;
        int supportedMetadata = supportedVineMetadata(x, y, z, metadata);
        if (supportedMetadata != metadata) {
            if (supportedMetadata == 0 && !canVineHangFromAbove(x, y, z)) {
                breakBlock(x, y, z, true);
                return;
            }
            setBlockMetadataIfLoadedSilently(x, y, z, BlockType.VINES, supportedMetadata);
            metadata = supportedMetadata;
        } else if (metadata == 0 && !canVineHangFromAbove(x, y, z)) {
            breakBlock(x, y, z, true);
            return;
        }

        if (random.nextInt(4) == 0 && hasVineGrowthRoom(x, y, z)) {
            int direction = random.nextInt(6);
            if (direction == Block.FACE_TOP) {
                growVineUp(x, y, z, metadata);
            } else if (direction == Block.FACE_BOTTOM) {
                growVineDown(x, y, z, metadata);
            } else {
                growVineSideways(x, y, z, metadata, direction);
            }
        }

        if (getBlockIfLoaded(x, y, z, BlockType.AIR) == BlockType.VINES) {
            scheduleBlockTick(x, y, z, BlockType.VINES, getTickDelay(BlockType.VINES));
        }
    }

    private int supportedVineMetadata(int x, int y, int z, int metadata) {
        int supported = 0;
        for (int face : VINE_HORIZONTAL_FACES) {
            int bit = BlockShape.vineMetadataFromFace(face);
            if ((metadata & bit) != 0 && canVineBitStay(x, y, z, bit, face)) {
                supported |= bit;
            }
        }
        return supported;
    }

    private boolean canVineBitStay(int x, int y, int z, int bit, int face) {
        int supportX = x + vineSupportDx(face);
        int supportZ = z + vineSupportDz(face);
        if (BlockShape.canSupportAttached(getBlockIfLoaded(supportX, y, supportZ, BlockType.AIR))) {
            return true;
        }
        return getBlockIfLoaded(x, y + 1, z, BlockType.AIR) == BlockType.VINES
                && (getBlockMetadataIfLoaded(x, y + 1, z, 0) & bit) != 0;
    }

    private boolean canVineHangFromAbove(int x, int y, int z) {
        return y + 1 < Chunk.HEIGHT
                && BlockShape.canSupportAttached(getBlockIfLoaded(x, y + 1, z, BlockType.AIR));
    }

    private void growVineUp(int x, int y, int z, int metadata) {
        if (y + 1 >= Chunk.HEIGHT || getBlockIfLoaded(x, y + 1, z, BlockType.BEDROCK) != BlockType.AIR) {
            return;
        }
        int newMetadata = randomVineBitsSupportedAt(x, y + 1, z, metadata);
        if (newMetadata > 0) {
            setBlock(x, y + 1, z, BlockType.VINES, newMetadata);
        }
    }

    private int randomVineBitsSupportedAt(int x, int y, int z, int metadata) {
        int newMetadata = 0;
        for (int face : VINE_HORIZONTAL_FACES) {
            int bit = BlockShape.vineMetadataFromFace(face);
            if ((metadata & bit) != 0
                    && random.nextBoolean()
                    && canAttachVineFaceAt(x, y, z, face)) {
                newMetadata |= bit;
            }
        }
        return newMetadata;
    }

    private void growVineDown(int x, int y, int z, int metadata) {
        if (y <= 0) {
            return;
        }
        BlockType below = getBlockIfLoaded(x, y - 1, z, BlockType.BEDROCK);
        if (below != BlockType.AIR && below != BlockType.VINES) {
            return;
        }
        int copiedMetadata = randomVineBitSubset(metadata);
        if (copiedMetadata == 0) {
            return;
        }
        if (below == BlockType.VINES) {
            int belowMetadata = getBlockMetadataIfLoaded(x, y - 1, z, 0);
            setBlockMetadataIfLoadedSilently(x, y - 1, z, BlockType.VINES, belowMetadata | copiedMetadata);
        } else {
            setBlock(x, y - 1, z, BlockType.VINES, copiedMetadata);
        }
    }

    private int randomVineBitSubset(int metadata) {
        int copied = 0;
        for (int face : VINE_HORIZONTAL_FACES) {
            int bit = BlockShape.vineMetadataFromFace(face);
            if ((metadata & bit) != 0 && random.nextBoolean()) {
                copied |= bit;
            }
        }
        return copied;
    }

    private void growVineSideways(int x, int y, int z, int metadata, int face) {
        int bit = BlockShape.vineMetadataFromFace(face);
        if (bit <= 0 || (metadata & bit) != 0) {
            return;
        }

        int dx = horizontalFaceDx(face);
        int dz = horizontalFaceDz(face);
        BlockType target = getBlockIfLoaded(x + dx, y, z + dz, BlockType.BEDROCK);
        if (BlockShape.canSupportAttached(target)) {
            setBlockMetadataIfLoadedSilently(x, y, z, BlockType.VINES, metadata | bit);
            return;
        }
        if (target != BlockType.AIR) {
            return;
        }

        int sideMetadata = 0;
        int leftFace = rotateVineFaceLeft(face);
        int rightFace = rotateVineFaceRight(face);
        int leftBit = BlockShape.vineMetadataFromFace(leftFace);
        int rightBit = BlockShape.vineMetadataFromFace(rightFace);
        if ((metadata & leftBit) != 0 && canAttachVineFaceAt(x + dx, y, z + dz, leftFace)) {
            sideMetadata |= leftBit;
        }
        if ((metadata & rightBit) != 0 && canAttachVineFaceAt(x + dx, y, z + dz, rightFace)) {
            sideMetadata |= rightBit;
        }
        if (sideMetadata == 0 && y + 1 < Chunk.HEIGHT
                && BlockShape.canSupportAttached(getBlockIfLoaded(x + dx, y + 1, z + dz, BlockType.AIR))) {
            setBlock(x + dx, y, z + dz, BlockType.VINES, 0);
            return;
        }
        if (sideMetadata > 0) {
            setBlock(x + dx, y, z + dz, BlockType.VINES, sideMetadata);
        }
    }

    private boolean canAttachVineFaceAt(int x, int y, int z, int face) {
        return BlockShape.canSupportAttached(getBlockIfLoaded(
                x + vineSupportDx(face), y, z + vineSupportDz(face), BlockType.AIR));
    }

    private boolean hasVineGrowthRoom(int x, int y, int z) {
        int vines = 0;
        for (int dx = -VINE_DENSITY_RADIUS; dx <= VINE_DENSITY_RADIUS; dx++) {
            for (int dz = -VINE_DENSITY_RADIUS; dz <= VINE_DENSITY_RADIUS; dz++) {
                for (int dy = -VINE_DENSITY_VERTICAL_RADIUS; dy <= VINE_DENSITY_VERTICAL_RADIUS; dy++) {
                    if (getBlockIfLoaded(x + dx, y + dy, z + dz, BlockType.AIR) == BlockType.VINES
                            && ++vines > VINE_DENSITY_LIMIT) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static final int[] VINE_HORIZONTAL_FACES = {
            Block.FACE_NORTH, Block.FACE_EAST, Block.FACE_SOUTH, Block.FACE_WEST
    };

    private static int horizontalFaceDx(int face) {
        return switch (face) {
            case Block.FACE_EAST -> 1;
            case Block.FACE_WEST -> -1;
            default -> 0;
        };
    }

    private static int horizontalFaceDz(int face) {
        return switch (face) {
            case Block.FACE_SOUTH -> 1;
            case Block.FACE_NORTH -> -1;
            default -> 0;
        };
    }

    private static int vineSupportDx(int face) {
        return switch (face) {
            case Block.FACE_EAST -> -1;
            case Block.FACE_WEST -> 1;
            default -> 0;
        };
    }

    private static int vineSupportDz(int face) {
        return switch (face) {
            case Block.FACE_NORTH -> 1;
            case Block.FACE_SOUTH -> -1;
            default -> 0;
        };
    }

    private static int rotateVineFaceLeft(int face) {
        return switch (face) {
            case Block.FACE_NORTH -> Block.FACE_WEST;
            case Block.FACE_WEST -> Block.FACE_SOUTH;
            case Block.FACE_SOUTH -> Block.FACE_EAST;
            case Block.FACE_EAST -> Block.FACE_NORTH;
            default -> face;
        };
    }

    private static int rotateVineFaceRight(int face) {
        return switch (face) {
            case Block.FACE_NORTH -> Block.FACE_EAST;
            case Block.FACE_EAST -> Block.FACE_SOUTH;
            case Block.FACE_SOUTH -> Block.FACE_WEST;
            case Block.FACE_WEST -> Block.FACE_NORTH;
            default -> face;
        };
    }

    private void updateSnowLayerBlock(int x, int y, int z) {
        if (!BlockShape.canPlaceAt(BlockType.SNOW_LAYER, getBlockMetadata(x, y, z), contextAt(x, y, z))
                || getBlockLight(x, y, z) > 11) {
            setBlock(x, y, z, BlockType.AIR, 0);
            return;
        }

        if (getBlockIfLoaded(x, y, z, BlockType.AIR) == BlockType.SNOW_LAYER) {
            scheduleBlockTick(x, y, z, BlockType.SNOW_LAYER, SNOW_LAYER_TICK_DELAY);
        }
    }

    private void updateIceBlock(int x, int y, int z) {
        if (getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.ICE) {
            return;
        }
        if (getBlockLight(x, y, z) > ICE_MELT_BLOCK_LIGHT_THRESHOLD) {
            meltIceBlock(x, y, z);
            return;
        }
        scheduleBlockTick(x, y, z, BlockType.ICE, ICE_TICK_DELAY);
    }

    private void meltIceBlock(int x, int y, int z) {
        if (dimension == Dimension.NETHER) {
            setBlock(x, y, z, BlockType.AIR, 0);
            return;
        }
        setBlock(x, y, z, BlockType.WATER, 0);
    }

    private void updateLeafBlock(int x, int y, int z) {
        int metadata = getBlockMetadataIfLoaded(x, y, z, 0);
        if ((metadata & LEAF_PERSISTENT_BIT) != 0) {
            return;
        }
        if ((metadata & LEAF_CHECK_DECAY_BIT) == 0) {
            return;
        }
        if (!hasLoadedLeafDecayNeighborhood(x, z)) {
            scheduleBlockTick(x, y, z, BlockType.LEAVES, LEAF_DECAY_TICK_DELAY);
            return;
        }
        if (!hasConnectedLogWithinLeafDecayRadius(x, y, z)) {
            breakBlock(x, y, z, true);
            return;
        }
        setBlockMetadataIfLoadedSilently(x, y, z, BlockType.LEAVES, metadata & ~LEAF_CHECK_DECAY_BIT);
    }

    private void markLeavesForDecayAroundLog(int x, int y, int z) {
        for (int dx = -LEAF_DECAY_RADIUS; dx <= LEAF_DECAY_RADIUS; dx++) {
            for (int dy = -LEAF_DECAY_RADIUS; dy <= LEAF_DECAY_RADIUS; dy++) {
                int leafY = y + dy;
                if (leafY < 0 || leafY >= Chunk.HEIGHT) {
                    continue;
                }
                for (int dz = -LEAF_DECAY_RADIUS; dz <= LEAF_DECAY_RADIUS; dz++) {
                    markLeafForDecay(x + dx, leafY, z + dz);
                }
            }
        }
    }

    private boolean markLeafForDecay(int x, int y, int z) {
        if (getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.LEAVES) {
            return false;
        }
        int metadata = getBlockMetadataIfLoaded(x, y, z, 0);
        if ((metadata & LEAF_PERSISTENT_BIT) != 0) {
            return false;
        }
        if ((metadata & LEAF_CHECK_DECAY_BIT) == 0) {
            setBlockMetadataIfLoadedSilently(x, y, z, BlockType.LEAVES, metadata | LEAF_CHECK_DECAY_BIT);
        }
        scheduleBlockTick(x, y, z, BlockType.LEAVES, LEAF_DECAY_TICK_DELAY);
        return true;
    }

    private boolean setBlockMetadataIfLoadedSilently(int x, int y, int z, BlockType expectedType, int metadata) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return false;
        }
        int chunkX = Math.floorDiv(x, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(z, Chunk.DEPTH);
        Chunk chunk = chunks.get(chunkKey(chunkX, chunkZ));
        if (chunk == null || chunk.getState().ordinal() < Chunk.ChunkState.GENERATED.ordinal()) {
            return false;
        }
        int localX = Math.floorMod(x, Chunk.WIDTH);
        int localZ = Math.floorMod(z, Chunk.DEPTH);
        if (chunk.getBlock(localX, y, localZ) != expectedType) {
            return false;
        }
        if (chunk.getBlockMetadata(localX, y, localZ) == metadata) {
            return true;
        }
        chunk.setBlock(localX, y, localZ, expectedType, metadata);
        chunk.setDirty(true);
        chunk.markLightDirty();
        markNeighborChunkDirtyForBorder(chunkX, chunkZ, localX, localZ);
        return true;
    }

    private void updateLockedChestBlock(int x, int y, int z) {
        setBlockIfLoaded(x, y, z, BlockType.AIR, 0);
    }

    private boolean hasLoadedLeafDecayNeighborhood(int x, int z) {
        return isChunkGeneratedForBlock(x - LEAF_DECAY_RADIUS, z - LEAF_DECAY_RADIUS)
                && isChunkGeneratedForBlock(x - LEAF_DECAY_RADIUS, z + LEAF_DECAY_RADIUS)
                && isChunkGeneratedForBlock(x + LEAF_DECAY_RADIUS, z - LEAF_DECAY_RADIUS)
                && isChunkGeneratedForBlock(x + LEAF_DECAY_RADIUS, z + LEAF_DECAY_RADIUS);
    }

    private boolean hasConnectedLogWithinLeafDecayRadius(int x, int y, int z) {
        int size = LEAF_DECAY_RADIUS * 2 + 1;
        boolean[][][] visited = new boolean[size][size][size];
        int capacity = size * size * size;
        int[] queueX = new int[capacity];
        int[] queueY = new int[capacity];
        int[] queueZ = new int[capacity];
        int[] queueDistance = new int[capacity];
        int head = 0;
        int tail = 0;
        int center = LEAF_DECAY_RADIUS;
        visited[center][center][center] = true;
        queueX[tail] = x;
        queueY[tail] = y;
        queueZ[tail] = z;
        queueDistance[tail] = 0;
        tail++;

        int[][] dirs = {
                { 1, 0, 0 }, { -1, 0, 0 },
                { 0, 1, 0 }, { 0, -1, 0 },
                { 0, 0, 1 }, { 0, 0, -1 }
        };
        while (head < tail) {
            int cx = queueX[head];
            int cy = queueY[head];
            int cz = queueZ[head];
            int distance = queueDistance[head];
            head++;
            for (int[] dir : dirs) {
                int nextDistance = distance + 1;
                if (nextDistance > LEAF_DECAY_RADIUS) {
                    continue;
                }
                int nx = cx + dir[0];
                int ny = cy + dir[1];
                int nz = cz + dir[2];
                int localX = nx - x + LEAF_DECAY_RADIUS;
                int localY = ny - y + LEAF_DECAY_RADIUS;
                int localZ = nz - z + LEAF_DECAY_RADIUS;
                if (localX < 0 || localX >= size || localY < 0 || localY >= size || localZ < 0 || localZ >= size
                        || visited[localX][localY][localZ]) {
                    continue;
                }
                BlockType type = getBlockIfLoaded(nx, ny, nz, BlockType.AIR);
                if (type == BlockType.OAK_LOG) {
                    return true;
                }
                if (type == BlockType.LEAVES && nextDistance < LEAF_DECAY_RADIUS) {
                    visited[localX][localY][localZ] = true;
                    queueX[tail] = nx;
                    queueY[tail] = ny;
                    queueZ[tail] = nz;
                    queueDistance[tail] = nextDistance;
                    tail++;
                }
            }
        }
        return false;
    }

    private int getCropLightLevel(int x, int y, int z) {
        return getCombinedLightLevel(x, y, z);
    }

    private int getCombinedLightLevel(int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return y >= Chunk.HEIGHT ? 15 : 0;
        }

        int chunkX = Math.floorDiv(x, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(z, Chunk.DEPTH);
        Chunk chunk = chunks.get(chunkKey(chunkX, chunkZ));
        if (chunk == null || chunk.getState().ordinal() < Chunk.ChunkState.LIGHTED.ordinal()) {
            return 0;
        }

        int localX = Math.floorMod(x, Chunk.WIDTH);
        int localZ = Math.floorMod(z, Chunk.DEPTH);
        return Math.max(chunk.getSkyLight(localX, y, localZ), chunk.getBlockLight(localX, y, localZ));
    }

    private float getCropGrowthChance(int x, int y, int z) {
        float chance = 1.0f;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                float contribution = 0.0f;
                if (getBlockIfLoaded(x + dx, y - 1, z + dz, BlockType.AIR) == BlockType.FARMLAND) {
                    contribution = getBlockMetadataIfLoaded(x + dx, y - 1, z + dz, 0) > 0 ? 3.0f : 1.0f;
                }
                if (dx != 0 || dz != 0) {
                    contribution /= 4.0f;
                }
                chance += contribution;
            }
        }

        boolean xNeighbor = getBlockIfLoaded(x - 1, y, z, BlockType.AIR) == BlockType.CROPS
                || getBlockIfLoaded(x + 1, y, z, BlockType.AIR) == BlockType.CROPS;
        boolean zNeighbor = getBlockIfLoaded(x, y, z - 1, BlockType.AIR) == BlockType.CROPS
                || getBlockIfLoaded(x, y, z + 1, BlockType.AIR) == BlockType.CROPS;
        boolean diagonalNeighbor = getBlockIfLoaded(x - 1, y, z - 1, BlockType.AIR) == BlockType.CROPS
                || getBlockIfLoaded(x + 1, y, z - 1, BlockType.AIR) == BlockType.CROPS
                || getBlockIfLoaded(x - 1, y, z + 1, BlockType.AIR) == BlockType.CROPS
                || getBlockIfLoaded(x + 1, y, z + 1, BlockType.AIR) == BlockType.CROPS;
        if (diagonalNeighbor || (xNeighbor && zNeighbor)) {
            chance /= 2.0f;
        }
        return Math.max(1.0f, chance);
    }

    private void updateGlowingRedstoneOre(int x, int y, int z) {
        if (getBlockIfLoaded(x, y, z, BlockType.AIR) == BlockType.GLOWING_REDSTONE_ORE) {
            setBlock(x, y, z, BlockType.REDSTONE_ORE, getBlockMetadataIfLoaded(x, y, z, 0));
        }
    }

    private void updateFluidBlock(int x, int y, int z, BlockType type) {
        boolean water = type.isWater();
        if (tryMixFluidAt(x, y, z)) {
            return;
        }

        int metadata = getBlockMetadataIfLoaded(x, y, z, 0) & 15;
        int decayStep = water || dimension == Dimension.NETHER ? 1 : 2;

        if (metadata > 0) {
            int[] sourceCount = { 0 };
            int smallestFlowDecay = -100;
            for (int[] dir : HORIZONTAL_DIRS) {
                smallestFlowDecay = getSmallestFlowDecay(x + dir[0], y, z + dir[1],
                        smallestFlowDecay, water, sourceCount);
            }

            int newDecay = smallestFlowDecay + decayStep;
            if (newDecay >= 8 || smallestFlowDecay < 0) {
                newDecay = -1;
            }

            int aboveDecay = getFlowDecay(x, y + 1, z, water);
            if (aboveDecay >= 0) {
                newDecay = aboveDecay >= 8 ? aboveDecay : aboveDecay + 8;
            }

            if (water && sourceCount[0] >= 2) {
                BlockType below = getBlockIfLoaded(x, y - 1, z, BlockType.STONE);
                if (below.isSolid()
                        || (below.isWater() && (getBlockMetadataIfLoaded(x, y - 1, z, 0) & 7) == 0)) {
                    newDecay = 0;
                }
            }

            if (newDecay != metadata) {
                if (newDecay < 0) {
                    setBlockIfLoaded(x, y, z, BlockType.AIR, 0);
                    return;
                }
                setBlockIfLoaded(x, y, z,
                        newDecay == 0 ? BlockType.stillVariant(water) : BlockType.flowingVariant(water),
                        newDecay);
                metadata = newDecay;
            }
        }

        if (canFluidDisplace(x, y - 1, z, water)) {
            int downDecay = metadata >= 8 ? metadata : metadata + 8;
            flowIntoBlock(x, y - 1, z, water, downDecay, true);
        } else if (metadata >= 0 && (metadata == 0 || blocksFluidFlow(x, y - 1, z))) {
            int spreadDecay = metadata >= 8 ? 1 : metadata + decayStep;
            if (spreadDecay < 8) {
                boolean[] directions = getOptimalFlowDirections(x, y, z, water);
                for (int i = 0; i < HORIZONTAL_DIRS.length; i++) {
                    if (directions[i]) {
                        flowIntoBlock(x + HORIZONTAL_DIRS[i][0], y, z + HORIZONTAL_DIRS[i][1],
                                water, spreadDecay, false);
                    }
                }
            }
        }

        BlockType current = getBlockIfLoaded(x, y, z, BlockType.AIR);
        if (current.isFluid()) {
            scheduleBlockTick(x, y, z, current, getTickDelay(current));
        }
    }

    private int getSmallestFlowDecay(int x, int y, int z, int currentSmallest, boolean water, int[] sourceCount) {
        int decay = getFlowDecay(x, y, z, water);
        if (decay < 0) {
            return currentSmallest;
        }
        if (decay == 0) {
            sourceCount[0]++;
        }
        if (decay >= 8) {
            decay = 0;
        }
        return currentSmallest >= 0 && decay >= currentSmallest ? currentSmallest : decay;
    }

    private int getFlowDecay(int x, int y, int z, boolean water) {
        BlockType type = getBlockIfLoaded(x, y, z, BlockType.AIR);
        if (water ? !type.isWater() : !type.isLava()) {
            return -1;
        }
        return getBlockMetadataIfLoaded(x, y, z, 0) & 15;
    }

    public Vector3f getFluidFlowVector(AABB area, boolean water) {
        Vector3f total = new Vector3f();
        if (area == null) {
            return total;
        }

        int minX = (int) Math.floor(area.getMin().x);
        int minY = (int) Math.floor(area.getMin().y);
        int minZ = (int) Math.floor(area.getMin().z);
        int maxX = (int) Math.floor(area.getMax().x - 0.0001f);
        int maxY = (int) Math.floor(area.getMax().y - 0.0001f);
        int maxZ = (int) Math.floor(area.getMax().z - 0.0001f);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockType type = getBlockIfLoaded(x, y, z, BlockType.AIR);
                    if (water ? type.isWater() : type.isLava()) {
                        total.add(getFluidFlowVectorAt(x, y, z, water));
                    }
                }
            }
        }

        if (total.lengthSquared() > 0.000001f) {
            total.normalize();
        }
        return total;
    }

    private Vector3f getFluidFlowVectorAt(int x, int y, int z, boolean water) {
        Vector3f flow = new Vector3f();
        int metadata = getFlowDecayIfLoaded(x, y, z, water);
        if (metadata < 0) {
            return flow;
        }

        int decay = FluidState.flowDecay(metadata);
        for (int[] dir : HORIZONTAL_DIRS) {
            int nx = x + dir[0];
            int nz = z + dir[1];
            int neighborDecay = getFlowDecayIfLoaded(nx, y, nz, water);
            if (neighborDecay >= 0) {
                int delta = FluidState.flowDecay(neighborDecay) - decay;
                flow.add(dir[0] * delta, 0.0f, dir[1] * delta);
            } else if (!blocksFluidFlowIfLoaded(nx, y, nz)) {
                int belowDecay = getFlowDecayIfLoaded(nx, y - 1, nz, water);
                if (belowDecay >= 0) {
                    int delta = FluidState.flowDecay(belowDecay) - (decay - 8);
                    flow.add(dir[0] * delta, 0.0f, dir[1] * delta);
                }
            }
        }

        if (FluidState.isFalling(metadata) && hasSolidFluidCurrentSide(x, y, z)) {
            if (flow.lengthSquared() > 0.000001f) {
                flow.normalize();
            }
            flow.add(0.0f, -6.0f, 0.0f);
        }
        if (flow.lengthSquared() > 0.000001f) {
            flow.normalize();
        }
        return flow;
    }

    private int getFlowDecayIfLoaded(int x, int y, int z, boolean water) {
        BlockType type = getBlockIfLoaded(x, y, z, BlockType.AIR);
        if (water ? !type.isWater() : !type.isLava()) {
            return -1;
        }
        return getBlockMetadataIfLoaded(x, y, z, 0) & 15;
    }

    private boolean hasSolidFluidCurrentSide(int x, int y, int z) {
        for (int[] dir : HORIZONTAL_DIRS) {
            if (blocksFluidFlowIfLoaded(x + dir[0], y, z + dir[1])) {
                return true;
            }
        }
        return false;
    }

    private boolean blocksFluidFlowIfLoaded(int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return true;
        }
        BlockType type = getBlockIfLoaded(x, y, z, BlockType.STONE);
        if (type == BlockType.AIR || type.isFluid() || type == BlockType.FIRE
                || type == BlockType.TORCH || type.isPlant()) {
            return false;
        }
        if (type.isDoor() || type.isSign() || type == BlockType.LADDER) {
            return true;
        }
        return type.isSolid();
    }

    private boolean[] getOptimalFlowDirections(int x, int y, int z, boolean water) {
        boolean[] optimal = new boolean[4];
        int bestCost = 1000;
        for (int i = 0; i < HORIZONTAL_DIRS.length; i++) {
            int nx = x + HORIZONTAL_DIRS[i][0];
            int nz = z + HORIZONTAL_DIRS[i][1];
            if (blocksFluidFlow(nx, y, nz) || isSameFluid(nx, y, nz, water)) {
                continue;
            }
            int cost = !blocksFluidFlow(nx, y - 1, nz)
                    ? 0
                    : calculateFlowCost(nx, y, nz, 1, i, water);
            if (cost < bestCost) {
                bestCost = cost;
            }
        }
        for (int i = 0; i < HORIZONTAL_DIRS.length; i++) {
            int nx = x + HORIZONTAL_DIRS[i][0];
            int nz = z + HORIZONTAL_DIRS[i][1];
            if (blocksFluidFlow(nx, y, nz) || isSameFluid(nx, y, nz, water)) {
                continue;
            }
            int cost = !blocksFluidFlow(nx, y - 1, nz)
                    ? 0
                    : calculateFlowCost(nx, y, nz, 1, i, water);
            optimal[i] = cost == bestCost;
        }
        return optimal;
    }

    private int calculateFlowCost(int x, int y, int z, int depth, int previousDirection, boolean water) {
        int bestCost = 1000;
        for (int i = 0; i < HORIZONTAL_DIRS.length; i++) {
            if (i == oppositeHorizontalDirection(previousDirection)) {
                continue;
            }
            int nx = x + HORIZONTAL_DIRS[i][0];
            int nz = z + HORIZONTAL_DIRS[i][1];
            if (blocksFluidFlow(nx, y, nz) || isSameFluid(nx, y, nz, water)) {
                continue;
            }
            if (!blocksFluidFlow(nx, y - 1, nz)) {
                return depth;
            }
            if (depth >= 4) {
                continue;
            }
            int cost = calculateFlowCost(nx, y, nz, depth + 1, i, water);
            if (cost < bestCost) {
                bestCost = cost;
            }
        }
        return bestCost;
    }

    private int oppositeHorizontalDirection(int direction) {
        return switch (direction) {
            case 0 -> 1;
            case 1 -> 0;
            case 2 -> 3;
            default -> 2;
        };
    }

    private boolean isSameFluid(int x, int y, int z, boolean water) {
        BlockType type = getBlockIfLoaded(x, y, z, BlockType.AIR);
        return water ? type.isWater() : type.isLava();
    }

    private boolean blocksFluidFlow(int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return true;
        }
        BlockType type = getBlockIfLoaded(x, y, z, BlockType.STONE);
        if (type == BlockType.AIR || type.isFluid() || type == BlockType.FIRE
                || type == BlockType.TORCH || type.isPlant()) {
            return false;
        }
        if (type.isDoor() || type.isSign() || type == BlockType.LADDER) {
            return true;
        }
        return type.isSolid();
    }

    private boolean canFluidDisplace(int x, int y, int z, boolean water) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return false;
        }
        BlockType target = getBlockIfLoaded(x, y, z, BlockType.BEDROCK);
        if (water && target.isLava()) {
            return true;
        }
        if (!water && target.isWater()) {
            return true;
        }
        if (target.isFluid()) {
            return water ? target.isWater() : target.isLava();
        }
        if (target.isDoor() || target.isSign() || target == BlockType.LADDER) {
            return false;
        }
        return target == BlockType.AIR || target == BlockType.FIRE
                || target == BlockType.TORCH || target.isPlant() || !target.isSolid();
    }

    private void flowIntoBlock(int x, int y, int z, boolean water, int metadata, boolean downward) {
        BlockType target = getBlockIfLoaded(x, y, z, BlockType.BEDROCK);
        if (water && target.isLava()) {
            BlockType hardened = lavaHardenedByWater(getBlockMetadataIfLoaded(x, y, z, 0));
            if (hardened != null) {
                if (setBlockIfLoaded(x, y, z, hardened, 0)) {
                    playLavaMixEffects(x, y, z);
                }
            }
            return;
        }
        if (!water && target.isWater()) {
            if (setBlockIfLoaded(x, y, z, downward ? BlockType.STONE : BlockType.COBBLESTONE, 0)) {
                playLavaMixEffects(x, y, z);
            }
            return;
        }
        if (!canFluidDisplace(x, y, z, water)) {
            return;
        }
        if (target.isFluid() && (water ? target.isWater() : target.isLava())
                && FluidState.isStrongerOrEqual(getBlockMetadataIfLoaded(x, y, z, 0), metadata)) {
            return;
        }
        displaceBlockForFluid(x, y, z, target, water);
        setBlockIfLoaded(x, y, z, BlockType.flowingVariant(water), metadata);
    }

    private void displaceBlockForFluid(int x, int y, int z, BlockType target, boolean water) {
        if (target == BlockType.AIR || target.isFluid()) {
            return;
        }
        if (water) {
            breakBlock(x, y, z, true);
        } else {
            playLavaMixEffects(x, y, z);
            breakBlock(x, y, z, false);
        }
    }

    private void tryMixFluidsAround(int x, int y, int z) {
        tryMixFluidAt(x, y, z);
        int[][] dirs = {
                { 1, 0, 0 }, { -1, 0, 0 },
                { 0, 1, 0 }, { 0, -1, 0 },
                { 0, 0, 1 }, { 0, 0, -1 }
        };
        for (int[] dir : dirs) {
            tryMixFluidAt(x + dir[0], y + dir[1], z + dir[2]);
        }
    }

    private boolean tryMixFluidAt(int x, int y, int z) {
        BlockType type = getBlockIfLoaded(x, y, z, BlockType.AIR);
        if (!type.isLava()) {
            return false;
        }
        int[][] dirs = {
                { 1, 0, 0 }, { -1, 0, 0 },
                { 0, 1, 0 }, { 0, 0, 1 }, { 0, 0, -1 }
        };
        for (int[] dir : dirs) {
            if (getBlockIfLoaded(x + dir[0], y + dir[1], z + dir[2], BlockType.AIR).isWater()) {
                int metadata = getBlockMetadataIfLoaded(x, y, z, 0);
                BlockType hardened = lavaHardenedByWater(metadata);
                if (hardened != null) {
                    if (setBlockIfLoaded(x, y, z, hardened, 0)) {
                        playLavaMixEffects(x, y, z);
                        return true;
                    }
                    return false;
                }
                return false;
            }
        }
        return false;
    }

    private static BlockType lavaHardenedByWater(int metadata) {
        int level = metadata & 15;
        if (level == 0) {
            return BlockType.OBSIDIAN;
        }
        if (level <= 4) {
            return BlockType.COBBLESTONE;
        }
        return null;
    }

    private void playLavaMixEffects(int x, int y, int z) {
        playSound(WorldSoundEvent.FIZZ,
                x + 0.5f, y + 0.5f, z + 0.5f,
                0.5f, redstoneTorchBurnoutPitch());
        for (int i = 0; i < LAVA_MIX_SMOKE_PARTICLES; i++) {
            spawnParticle(WorldParticle.Type.LARGE_SMOKE,
                    x + random.nextFloat(),
                    y + LAVA_MIX_SMOKE_Y_OFFSET,
                    z + random.nextFloat(),
                    0.0f, 0.0f, 0.0f,
                    0.30f, 22);
        }
    }

    private void updateFallingBlock(int x, int y, int z, BlockType type) {
        if (y <= 0 || !BlockShape.canFallingBlockFallThrough(getBlock(x, y - 1, z))) {
            return;
        }
        int metadata = getBlockMetadata(x, y, z);
        setBlock(x, y, z, BlockType.AIR, 0);
        FallingBlockEntity falling = new FallingBlockEntity(type, metadata);
        falling.setPosition(x + 0.5f, y, z + 0.5f);
        spawnEntity(falling);
    }

    private void updateFireBlock(int x, int y, int z) {
        Random fireRandom = getRandom();
        boolean infiniteSupport = hasInfiniteFireSupport(x, y, z);
        int oldAge = getBlockMetadata(x, y, z);
        if (!canFireStay(x, y, z)) {
            setBlock(x, y, z, BlockType.AIR, 0);
            return;
        }
        if (!infiniteSupport && isRainDousingFire(x, y, z)) {
            setBlock(x, y, z, BlockType.AIR, 0);
            return;
        }
        int age = Math.min(15, oldAge + (fireRandom.nextInt(3) == 0 ? 1 : 0));
        if (age != oldAge) {
            setBlock(x, y, z, BlockType.FIRE, age);
        }
        boolean flammableNeighbor = hasFlammableNeighbor(x, y, z);
        BlockType below = getBlock(x, y - 1, z);
        if (!infiniteSupport && !flammableNeighbor) {
            if (!BlockShape.isOpaqueCube(below) || oldAge > 3) {
                setBlock(x, y, z, BlockType.AIR, 0);
                return;
            }
        } else if (!infiniteSupport && !canCatchFire(below)
                && oldAge == 15 && fireRandom.nextInt(4) == 0) {
            setBlock(x, y, z, BlockType.AIR, 0);
            return;
        }
        spreadFireFrom(x, y, z, age, fireRandom);
        if (getBlock(x, y, z) == BlockType.FIRE) {
            scheduleBlockTick(x, y, z, BlockType.FIRE, FIRE_TICK_DELAY);
        }
    }

    private boolean isRainDousingFire(int x, int y, int z) {
        return isRainingAt(x, y, z)
                || isRainingAt(x - 1, y, z)
                || isRainingAt(x + 1, y, z)
                || isRainingAt(x, y, z - 1)
                || isRainingAt(x, y, z + 1);
    }

    private boolean canFireStay(int x, int y, int z) {
        return BlockShape.isOpaqueCube(getBlock(x, y - 1, z)) || hasFlammableNeighbor(x, y, z);
    }

    private boolean hasInfiniteFireSupport(int x, int y, int z) {
        BlockType below = getBlock(x, y - 1, z);
        return below == BlockType.NETHERRACK
                || (dimension == Dimension.THE_END && below == BlockType.BEDROCK);
    }

    private boolean hasFlammableNeighbor(int x, int y, int z) {
        int[][] dirs = {
                { 1, 0, 0 }, { -1, 0, 0 },
                { 0, 1, 0 }, { 0, -1, 0 },
                { 0, 0, 1 }, { 0, 0, -1 }
        };
        for (int[] dir : dirs) {
            if (canCatchFire(getBlock(x + dir[0], y + dir[1], z + dir[2]))) {
                return true;
            }
        }
        return false;
    }

    private void spreadFireFrom(int x, int y, int z, int age, Random fireRandom) {
        int[][] dirs = {
                { 1, 0, 0 }, { -1, 0, 0 },
                { 0, 1, 0 }, { 0, -1, 0 },
                { 0, 0, 1 }, { 0, 0, -1 }
        };
        for (int[] dir : dirs) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            int nz = z + dir[2];
            BlockType target = getBlock(nx, ny, nz);
            int chance = dir[1] == 0 ? FIRE_HORIZONTAL_CATCH_CHANCE : FIRE_VERTICAL_CATCH_CHANCE;
            tryCatchBlockOnFire(nx, ny, nz, target, chance, fireRandom, age);
        }
        spreadFireThroughAirAround(x, y, z, age, fireRandom);
    }

    private void spreadFireThroughAirAround(int x, int y, int z, int age, Random fireRandom) {
        for (int nx = x - 1; nx <= x + 1; nx++) {
            for (int nz = z - 1; nz <= z + 1; nz++) {
                for (int ny = y - 1; ny <= y + 4; ny++) {
                    if (nx == x && ny == y && nz == z) {
                        continue;
                    }
                    int encouragement = neighborFireEncouragement(nx, ny, nz);
                    if (encouragement <= 0) {
                        continue;
                    }
                    int chance = FIRE_AIR_SPREAD_BASE_CHANCE;
                    if (ny > y + 1) {
                        chance += (ny - (y + 1)) * FIRE_AIR_SPREAD_VERTICAL_CHANCE_STEP;
                    }
                    int threshold = (encouragement + FIRE_AIR_SPREAD_ENCOURAGEMENT_BONUS) / (age + 30);
                    if (threshold > 0 && fireRandom.nextInt(chance) <= threshold
                            && !isRainDousingFire(nx, ny, nz)) {
                        int newAge = Math.min(15, age + fireRandom.nextInt(5) / 4);
                        setBlock(nx, ny, nz, BlockType.FIRE, newAge);
                    }
                }
            }
        }
    }

    private int neighborFireEncouragement(int x, int y, int z) {
        if (getBlock(x, y, z) != BlockType.AIR) {
            return 0;
        }
        int encouragement = 0;
        encouragement = Math.max(encouragement, getBlock(x + 1, y, z).getFireEncouragement());
        encouragement = Math.max(encouragement, getBlock(x - 1, y, z).getFireEncouragement());
        encouragement = Math.max(encouragement, getBlock(x, y + 1, z).getFireEncouragement());
        encouragement = Math.max(encouragement, getBlock(x, y - 1, z).getFireEncouragement());
        encouragement = Math.max(encouragement, getBlock(x, y, z + 1).getFireEncouragement());
        encouragement = Math.max(encouragement, getBlock(x, y, z - 1).getFireEncouragement());
        return encouragement;
    }

    private boolean canCatchFire(BlockType target) {
        return target.getFireEncouragement() > 0;
    }

    private void tryCatchBlockOnFire(int x, int y, int z, BlockType target, int chance, Random fireRandom, int age) {
        int flammability = target.getFireFlammability();
        if (flammability <= 0 || fireRandom.nextInt(chance) >= flammability) {
            return;
        }
        if (target == BlockType.TNT) {
            primeTnt(x, y, z, RedstoneEngine.TNT_FUSE_TICKS);
            return;
        }
        if (fireRandom.nextInt(age + 10) < 5) {
            setBlock(x, y, z, BlockType.FIRE, Math.min(15, age + fireRandom.nextInt(5) / 4));
        } else {
            setBlock(x, y, z, BlockType.AIR, 0);
        }
    }

    private void igniteAroundLava(int x, int y, int z) {
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int nx = x + dx;
                    int ny = y + dy;
                    int nz = z + dz;
                    if (getBlock(nx, ny, nz) == BlockType.AIR && hasFlammableNeighbor(nx, ny, nz)) {
                        setBlock(nx, ny, nz, BlockType.FIRE, 0);
                    }
                }
            }
        }
    }

    public boolean breakBlock(int x, int y, int z, boolean dropBlock) {
        return breakBlock(x, y, z, dropBlock, null);
    }

    public boolean breakBlock(int x, int y, int z, boolean dropBlock, ItemType tool) {
        return breakBlockInternal(x, y, z, dropBlock, tool, null);
    }

    public boolean breakBlockWithToolStack(int x, int y, int z, boolean dropBlock, ItemStack toolStack) {
        ItemType tool = toolStack == null || toolStack.isEmpty() ? null : toolStack.getType();
        return breakBlockInternal(x, y, z, dropBlock, tool, toolStack);
    }

    private boolean breakBlockInternal(int x, int y, int z, boolean dropBlock, ItemType tool, ItemStack toolStack) {
        BlockType type = getBlock(x, y, z);
        if (type == BlockType.AIR || type == BlockType.BEDROCK) {
            return false;
        }

        int metadata = getBlockMetadata(x, y, z);
        if (type.isDoor()) {
            return breakDoor(x, y, z, type, metadata, dropBlock);
        }
        if (type.isBed()) {
            return breakBed(x, y, z, metadata, dropBlock);
        }
        if (type == BlockType.PORTAL) {
            return breakNetherPortal(x, y, z);
        }
        if (type == BlockType.PISTON_HEAD) {
            return breakPistonHead(x, y, z, metadata, dropBlock);
        }
        if ((type == BlockType.PISTON || type == BlockType.STICKY_PISTON)
                && (metadata & RedstoneEngine.PISTON_EXTENDED_BIT) != 0) {
            return breakExtendedPistonBase(x, y, z, type, metadata, dropBlock, tool, toolStack);
        }
        if (type == BlockType.ICE) {
            breakIceBlock(x, y, z, dropBlock, tool, toolStack);
            return true;
        }

        TileEntity tile = getTileEntity(x, y, z);
        ejectJukeboxRecordOnRemoval(tile);
        tile = removeTileEntity(x, y, z);
        if (dropBlock) {
            dropBlockStacks(x, y, z, type, metadata, tool, toolStack);
        }
        if (tile != null) {
            dropTileEntityContents(tile, x, y, z);
        }
        if (type == BlockType.INFESTED_STONE) {
            spawnSilverfishFromInfestedBlock(x, y, z);
        }

        setBlock(x, y, z, BlockType.AIR, 0);
        return true;
    }

    private void breakIceBlock(int x, int y, int z, boolean dropBlock, ItemType tool, ItemStack toolStack) {
        if (dropBlock) {
            dropBlockStacks(x, y, z, BlockType.ICE, getBlockMetadata(x, y, z), tool, toolStack);
        }
        if (dimension == Dimension.NETHER || !brokenIceShouldBecomeWater(x, y, z)) {
            setBlock(x, y, z, BlockType.AIR, 0);
            return;
        }
        setBlock(x, y, z, BlockType.FLOWING_WATER, 0);
    }

    private boolean brokenIceShouldBecomeWater(int x, int y, int z) {
        BlockType below = getBlockIfLoaded(x, y - 1, z, BlockType.AIR);
        return below.isSolid() || below.isFluid();
    }

    private boolean breakPistonHead(int x, int y, int z, int metadata, boolean dropBlock) {
        int facing = pistonFacing(metadata);
        int baseX = x + RedstoneEngine.faceToDx(RedstoneEngine.opposite(facing));
        int baseY = y + RedstoneEngine.faceToDy(RedstoneEngine.opposite(facing));
        int baseZ = z + RedstoneEngine.faceToDz(RedstoneEngine.opposite(facing));
        BlockType base = getBlock(baseX, baseY, baseZ);
        int baseMetadata = getBlockMetadata(baseX, baseY, baseZ);

        if ((base == BlockType.PISTON || base == BlockType.STICKY_PISTON)
                && (baseMetadata & RedstoneEngine.PISTON_EXTENDED_BIT) != 0
                && pistonFacing(baseMetadata) == facing) {
            if (dropBlock) {
                dropBlockStacks(baseX, baseY, baseZ, base, baseMetadata, null);
            }
            setBlock(baseX, baseY, baseZ, BlockType.AIR, 0);
        }
        setBlock(x, y, z, BlockType.AIR, 0);
        return true;
    }

    private boolean breakExtendedPistonBase(int x, int y, int z, BlockType type, int metadata,
            boolean dropBlock, ItemType tool, ItemStack toolStack) {
        int facing = pistonFacing(metadata);
        int headX = x + RedstoneEngine.faceToDx(facing);
        int headY = y + RedstoneEngine.faceToDy(facing);
        int headZ = z + RedstoneEngine.faceToDz(facing);
        if (getBlock(headX, headY, headZ) == BlockType.PISTON_HEAD
                && pistonFacing(getBlockMetadata(headX, headY, headZ)) == facing) {
            setBlock(headX, headY, headZ, BlockType.AIR, 0);
        }
        if (dropBlock) {
            dropBlockStacks(x, y, z, type, metadata, tool, toolStack);
        }
        setBlock(x, y, z, BlockType.AIR, 0);
        return true;
    }

    private static int pistonFacing(int metadata) {
        int facing = metadata & 7;
        return switch (facing) {
            case Block.FACE_TOP, Block.FACE_BOTTOM, Block.FACE_NORTH,
                    Block.FACE_SOUTH, Block.FACE_EAST, Block.FACE_WEST -> facing;
            default -> Block.FACE_NORTH;
        };
    }

    private void dropBlockStacks(int x, int y, int z, BlockType type, int metadata, ItemType tool) {
        dropBlockStacks(x, y, z, type, metadata, tool, null);
    }

    private void dropBlockStacks(int x, int y, int z, BlockType type, int metadata, ItemType tool,
            ItemStack toolStack) {
        List<ItemStack> drops = toolStack == null
                ? BlockDropResolver.getDrops(type, metadata, random, tool)
                : BlockDropResolver.getDropsWithToolStack(type, metadata, random, toolStack);
        for (ItemStack droppedStack : drops) {
            if (droppedStack == null || droppedStack.isEmpty()) {
                continue;
            }
            spawnThrownStack(x + 0.5f, y + 0.5f, z + 0.5f, droppedStack,
                    (random.nextFloat() - 0.5f) * 0.12f,
                    0.18f,
                    (random.nextFloat() - 0.5f) * 0.12f);
        }
    }

    private void dropTileEntityContents(TileEntity tile, int x, int y, int z) {
        if (tile == null) {
            return;
        }
        for (ItemStack stack : tile.getDrops()) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ItemStack remaining = stack.copy();
            while (!remaining.isEmpty()) {
                int amount = Math.min(remaining.getCount(),
                        random.nextInt(TILE_DROP_RANDOM_SPLIT) + TILE_DROP_MIN_SPLIT);
                ItemStack dropped = ItemStackOps.split(remaining, amount);
                if (dropped == null || dropped.isEmpty()) {
                    break;
                }
                float dropX = x + random.nextFloat() * TILE_DROP_OFFSET_RANGE + TILE_DROP_OFFSET_MIN;
                float dropY = y + random.nextFloat() * TILE_DROP_OFFSET_RANGE + TILE_DROP_OFFSET_MIN;
                float dropZ = z + random.nextFloat() * TILE_DROP_OFFSET_RANGE + TILE_DROP_OFFSET_MIN;
                spawnThrownStack(dropX, dropY, dropZ, dropped,
                        (float) random.nextGaussian() * TILE_DROP_HORIZONTAL_VELOCITY,
                        (float) random.nextGaussian() * TILE_DROP_HORIZONTAL_VELOCITY
                                + TILE_DROP_VERTICAL_VELOCITY,
                        (float) random.nextGaussian() * TILE_DROP_HORIZONTAL_VELOCITY);
            }
        }
    }

    private void spawnSilverfishFromInfestedBlock(int x, int y, int z) {
        Silverfish silverfish = new Silverfish();
        silverfish.setPosition(x + 0.5f, y, z + 0.5f);
        spawnEntity(silverfish);
    }

    private TileEntity createTileEntityForBlock(BlockType type, int x, int y, int z) {
        if (type == BlockType.CHEST) {
            return new ChestTileEntity(x, y, z);
        }
        if (type.isFurnace()) {
            return new FurnaceTileEntity(x, y, z);
        }
        if (type == BlockType.MOB_SPAWNER) {
            return new MonsterSpawnerTileEntity(x, y, z);
        }
        if (type == BlockType.BREWING_STAND) {
            return new BrewingStandTileEntity(x, y, z);
        }
        if (type == BlockType.DISPENSER) {
            return new DispenserTileEntity(x, y, z);
        }
        if (type == BlockType.NOTE_BLOCK) {
            return new NoteBlockTileEntity(x, y, z);
        }
        if (type == BlockType.JUKEBOX) {
            return new JukeboxTileEntity(x, y, z);
        }
        if (type == BlockType.ENCHANTING_TABLE) {
            return new EnchantingTableTileEntity(x, y, z);
        }
        if (type.isSign()) {
            return new SignTileEntity(x, y, z);
        }
        return null;
    }

    private boolean canReuseTileEntity(BlockType previous, BlockType current) {
        if (!previous.hasTileEntity() || !current.hasTileEntity()) {
            return false;
        }
        if (previous == current) {
            return true;
        }
        return previous.isFurnace() && current.isFurnace();
    }

    public TileEntity getTileEntity(int x, int y, int z) {
        return tileEntities.get(new BlockPos(x, y, z));
    }

    public TileEntity removeTileEntity(int x, int y, int z) {
        return tileEntities.remove(new BlockPos(x, y, z));
    }

    public void putTileEntity(TileEntity tileEntity) {
        if (tileEntity != null) {
            tileEntities.put(tileEntity.getPos(), tileEntity);
        }
    }

    @Override
    public void stageGeneratedTileEntity(TileEntity tileEntity) {
        if (tileEntity != null) {
            generatedTileEntities.put(tileEntity.getPos(), tileEntity);
        }
    }

    @Override
    public void stageGeneratedEntity(Entity entity) {
        if (entity != null) {
            generatedEntities.add(entity);
        }
    }

    public void setSpawnNpcs(boolean spawnNpcs) {
        this.spawnNpcs = spawnNpcs;
    }

    public boolean shouldSpawnNpcs() {
        return spawnNpcs;
    }

    synchronized int nextReleaseOneWorldRandomInt(int bound) {
        return random.nextInt(bound);
    }

    synchronized int nextReleaseOneWorldGenCreatureRandomInt(int bound) {
        if (!releaseOneWorldGenCreatureRandomPrimed) {
            random.nextInt();
            random.nextInt();
            releaseOneWorldGenCreatureRandomPrimed = true;
        }
        return random.nextInt(bound);
    }

    synchronized int nextReleaseOneWorldGenSheepColor() {
        int roll = random.nextInt(100);
        if (roll < 5) {
            return 15;
        }
        if (roll < 10) {
            return 7;
        }
        if (roll < 15) {
            return 8;
        }
        if (roll < 18) {
            return 12;
        }
        return random.nextInt(500) == 0 ? 6 : 0;
    }

    public Random getRandom() {
        return random;
    }

    public Collection<TileEntity> getTileEntities() {
        return tileEntitySnapshot();
    }

    public void replaceTileEntities(Collection<TileEntity> restored) {
        tileEntities.clear();
        if (restored == null) {
            reconcileLoadedTileEntities();
            return;
        }
        for (TileEntity tile : restored) {
            putTileEntity(tile);
        }
        reconcileLoadedTileEntities();
    }

    public void reconcileLoadedTileEntities() {
        removeStaleTileEntitiesInLoadedChunks();
        for (Chunk chunk : chunks.values()) {
            if (chunk.getState().ordinal() >= Chunk.ChunkState.GENERATED.ordinal()) {
                reconcileTileEntitiesInChunk(chunk);
            }
        }
    }

    private void removeStaleTileEntitiesInLoadedChunks() {
        Iterator<Map.Entry<BlockPos, TileEntity>> iterator = tileEntities.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, TileEntity> entry = iterator.next();
            BlockPos pos = entry.getKey();
            if (pos == null || pos.y() < 0 || pos.y() >= Chunk.HEIGHT) {
                iterator.remove();
                continue;
            }
            if (!isChunkGeneratedForBlock(pos.x(), pos.z())) {
                continue;
            }
            BlockType type = getBlockIfLoaded(pos.x(), pos.y(), pos.z(), BlockType.AIR);
            if (!tileMatchesBlock(entry.getValue(), type)) {
                iterator.remove();
            }
        }
    }

    private void removeTileEntitiesForChunk(Chunk chunk) {
        if (chunk == null) {
            return;
        }
        int minX = chunk.getWorldX();
        int maxX = minX + Chunk.WIDTH;
        int minZ = chunk.getWorldZ();
        int maxZ = minZ + Chunk.DEPTH;
        tileEntities.keySet().removeIf(pos -> pos != null
                && pos.x() >= minX && pos.x() < maxX
                && pos.z() >= minZ && pos.z() < maxZ);
        generatedTileEntities.keySet().removeIf(pos -> pos != null
                && pos.x() >= minX && pos.x() < maxX
                && pos.z() >= minZ && pos.z() < maxZ);
    }

    private void reconcileTileEntitiesInChunk(Chunk chunk) {
        int worldX = chunk.getChunkX() * Chunk.WIDTH;
        int worldZ = chunk.getChunkZ() * Chunk.DEPTH;
        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int x = 0; x < Chunk.WIDTH; x++) {
                    BlockType type = chunk.getBlock(x, y, z);
                    if (type.hasTileEntity()) {
                        final int bx = worldX + x;
                        final int by = y;
                        final int bz = worldZ + z;
                        BlockPos pos = new BlockPos(bx, by, bz);
                        TileEntity staged = generatedTileEntities.remove(pos);
                        TileEntity current = tileEntities.get(pos);
                        if (!tileMatchesBlock(current, type)) {
                            TileEntity replacement = tileMatchesBlock(staged, type)
                                    ? staged
                                    : createTileEntityForBlock(type, bx, by, bz);
                            if (replacement != null) {
                                tileEntities.put(pos, replacement);
                            } else {
                                tileEntities.remove(pos);
                            }
                        }
                        reconcileTileBlockState(chunk, x, y, z, type, tileEntities.get(pos));
                    } else {
                        generatedTileEntities.remove(new BlockPos(worldX + x, y, worldZ + z));
                    }
                }
            }
        }
    }

    private boolean tileMatchesBlock(TileEntity tile, BlockType type) {
        if (tile == null || type == null || !type.hasTileEntity()) {
            return false;
        }
        if (type == BlockType.CHEST) {
            return tile instanceof ChestTileEntity;
        }
        if (type.isFurnace()) {
            return tile instanceof FurnaceTileEntity;
        }
        if (type == BlockType.MOB_SPAWNER) {
            return tile instanceof MonsterSpawnerTileEntity;
        }
        if (type == BlockType.BREWING_STAND) {
            return tile instanceof BrewingStandTileEntity;
        }
        if (type == BlockType.DISPENSER) {
            return tile instanceof DispenserTileEntity;
        }
        if (type == BlockType.NOTE_BLOCK) {
            return tile instanceof NoteBlockTileEntity;
        }
        if (type == BlockType.JUKEBOX) {
            return tile instanceof JukeboxTileEntity;
        }
        if (type == BlockType.ENCHANTING_TABLE) {
            return tile instanceof EnchantingTableTileEntity;
        }
        if (type.isSign()) {
            return tile instanceof SignTileEntity;
        }
        return false;
    }

    private void reconcileTileBlockState(Chunk chunk, int localX, int y, int localZ, BlockType type,
            TileEntity tile) {
        if (type == BlockType.JUKEBOX && tile instanceof JukeboxTileEntity jukebox) {
            int expectedMetadata = jukebox.hasRecord() ? 1 : 0;
            setChunkBlockStateIfNeeded(chunk, localX, y, localZ, type, expectedMetadata);
        } else if (type == BlockType.BREWING_STAND && tile instanceof BrewingStandTileEntity brewing) {
            setChunkBlockStateIfNeeded(chunk, localX, y, localZ, type, brewing.getFilledSlots());
        } else if (type.isFurnace() && tile instanceof FurnaceTileEntity furnace) {
            BlockType expectedType = furnace.isBurning() ? BlockType.LIT_FURNACE : BlockType.FURNACE;
            setChunkBlockStateIfNeeded(chunk, localX, y, localZ,
                    expectedType, chunk.getBlockMetadata(localX, y, localZ));
        }
    }

    private void setChunkBlockStateIfNeeded(Chunk chunk, int localX, int y, int localZ,
            BlockType type, int metadata) {
        if (chunk.getBlock(localX, y, localZ) != type || chunk.getBlockMetadata(localX, y, localZ) != metadata) {
            chunk.setBlock(localX, y, localZ, type, metadata);
        }
    }

    public void tickTileEntities(float deltaTime) {
        removeStaleTileEntitiesInLoadedChunks();
        for (TileEntity tile : tileEntitySnapshot()) {
            if (!canTickTileEntity(tile)) {
                continue;
            }
            tile.tick(this, deltaTime);
        }
    }

    private List<TileEntity> tileEntitySnapshot() {
        List<TileEntity> snapshot = new ArrayList<>(tileEntities.values());
        snapshot.removeIf(tile -> tile == null || tile.getPos() == null);
        snapshot.sort(World::compareTileEntities);
        return snapshot;
    }

    private static int compareTileEntities(TileEntity left, TileEntity right) {
        BlockPos leftPos = left.getPos();
        BlockPos rightPos = right.getPos();
        int xCompare = Integer.compare(leftPos.x(), rightPos.x());
        if (xCompare != 0) {
            return xCompare;
        }
        int yCompare = Integer.compare(leftPos.y(), rightPos.y());
        return yCompare != 0 ? yCompare : Integer.compare(leftPos.z(), rightPos.z());
    }

    private boolean canTickTileEntity(TileEntity tile) {
        if (tile == null) {
            return false;
        }
        BlockPos pos = tile.getPos();
        if (pos == null || pos.y() < 0 || pos.y() >= Chunk.HEIGHT
                || !isChunkGeneratedForBlock(pos.x(), pos.z())) {
            return false;
        }
        BlockType type = getBlockIfLoaded(pos.x(), pos.y(), pos.z(), BlockType.AIR);
        return tileMatchesBlock(tile, type);
    }

    public boolean canPlaceChestAt(int x, int y, int z) {
        int adjacent = 0;
        for (int[] dir : HORIZONTAL_DIRS) {
            int nx = x + dir[0];
            int nz = z + dir[1];
            if (getBlock(nx, y, nz) == BlockType.CHEST) {
                adjacent++;
                if (adjacent > 1 || countAdjacentChests(nx, y, nz, x, z) > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    public ChestTileEntity getAdjacentChest(ChestTileEntity chest) {
        BlockPos pos = chest.getPos();
        for (int[] dir : HORIZONTAL_DIRS) {
            TileEntity tile = getTileEntity(pos.x() + dir[0], pos.y(), pos.z() + dir[1]);
            if (tile instanceof ChestTileEntity adjacent) {
                return adjacent;
            }
        }
        return null;
    }

    public boolean canOpenChest(ChestTileEntity chest) {
        if (chest == null) {
            return false;
        }
        BlockPos pos = chest.getPos();
        if (isChestBlockedAbove(pos.x(), pos.y(), pos.z())) {
            return false;
        }
        ChestTileEntity adjacent = getAdjacentChest(chest);
        return adjacent == null || !isChestBlockedAbove(adjacent.getPos().x(), adjacent.getPos().y(), adjacent.getPos().z());
    }

    private boolean isChestBlockedAbove(int x, int y, int z) {
        return BlockShape.isOpaqueCube(getBlockIfLoaded(x, y + 1, z, BlockType.AIR));
    }

    private int countAdjacentChests(int x, int y, int z, int ignoreX, int ignoreZ) {
        int count = 0;
        for (int[] dir : HORIZONTAL_DIRS) {
            int nx = x + dir[0];
            int nz = z + dir[1];
            if (nx == ignoreX && nz == ignoreZ) {
                continue;
            }
            if (getBlock(nx, y, nz) == BlockType.CHEST) {
                count++;
            }
        }
        return count;
    }

    public boolean toggleBlock(int x, int y, int z) {
        return toggleBlock(x, y, z, -1);
    }

    public boolean toggleBlock(int x, int y, int z, int playerFacing) {
        if (RedstoneEngine.toggleInteractiveBlock(this, x, y, z)) {
            return true;
        }
        BlockType type = getBlock(x, y, z);
        int metadata = getBlockMetadata(x, y, z);
        if (type == BlockType.WOODEN_DOOR) {
            int lowerY = BlockShape.isDoorUpper(metadata) ? y - 1 : y;
            if (BlockShape.isDoorUpper(metadata)) {
                BlockType lowerType = getBlockIfLoaded(x, lowerY, z, BlockType.AIR);
                int lowerExistingMetadata = getBlockMetadataIfLoaded(x, lowerY, z, 0);
                if (lowerType != type || BlockShape.isDoorUpper(lowerExistingMetadata)) {
                    return true;
                }
            }
            int lowerMetadata = getBlockMetadata(x, lowerY, z);
            int newMetadata = lowerMetadata ^ 4;
            setBlock(x, lowerY, z, type, newMetadata);
            playOpenableSound(type, x, lowerY, z, (newMetadata & RedstoneEngine.DOOR_OPEN_BIT) != 0);
            RedstoneEngine.rememberPoweredOpenableState(this, x, lowerY, z);
            scheduleMechanismUpdatesAround(x, lowerY, z);
            return true;
        }
        if (type == BlockType.IRON_DOOR) {
            return false;
        }
        if (type == BlockType.NOTE_BLOCK) {
            TileEntity tile = getTileEntity(x, y, z);
            if (tile instanceof NoteBlockTileEntity note) {
                note.cyclePitch();
                note.play(this);
                return true;
            }
            return false;
        }
        if (type == BlockType.JUKEBOX) {
            if (metadata == 0) {
                return false;
            }
            TileEntity tile = getTileEntity(x, y, z);
            return tile instanceof JukeboxTileEntity jukebox && jukebox.ejectRecord(this);
        }
        if (type == BlockType.TRAPDOOR) {
            int newMetadata = metadata ^ 4;
            setBlock(x, y, z, type, newMetadata);
            playOpenableSound(type, x, y, z, (newMetadata & RedstoneEngine.DOOR_OPEN_BIT) != 0);
            RedstoneEngine.rememberPoweredOpenableState(this, x, y, z);
            scheduleMechanismUpdatesAround(x, y, z);
            return true;
        }
        if (type == BlockType.FENCE_GATE) {
            int newMetadata = manuallyToggledFenceGateMetadata(metadata, playerFacing);
            setBlock(x, y, z, type, newMetadata);
            playOpenableSound(type, x, y, z, (newMetadata & RedstoneEngine.DOOR_OPEN_BIT) != 0);
            RedstoneEngine.rememberPoweredOpenableState(this, x, y, z);
            scheduleMechanismUpdatesAround(x, y, z);
            return true;
        }
        return false;
    }

    public boolean setWoodenDoorOpen(int x, int y, int z, boolean open) {
        BlockType type = getBlockIfLoaded(x, y, z, BlockType.AIR);
        if (type != BlockType.WOODEN_DOOR) {
            return false;
        }

        int metadata = getBlockMetadataIfLoaded(x, y, z, 0);
        int lowerY = BlockShape.isDoorUpper(metadata) ? y - 1 : y;
        if (BlockShape.isDoorUpper(metadata)) {
            BlockType lowerType = getBlockIfLoaded(x, lowerY, z, BlockType.AIR);
            int lowerMetadata = getBlockMetadataIfLoaded(x, lowerY, z, 0);
            if (lowerType != type || BlockShape.isDoorUpper(lowerMetadata)) {
                return false;
            }
            metadata = lowerMetadata;
        }

        boolean wasOpen = (metadata & RedstoneEngine.DOOR_OPEN_BIT) != 0;
        if (wasOpen == open) {
            return true;
        }

        int newMetadata = open
                ? metadata | RedstoneEngine.DOOR_OPEN_BIT
                : metadata & ~RedstoneEngine.DOOR_OPEN_BIT;
        if (!setBlockIfLoaded(x, lowerY, z, type, newMetadata)) {
            return false;
        }

        rebuildBlockMeshesNow(x, lowerY, z);
        if (getBlockIfLoaded(x, lowerY + 1, z, BlockType.AIR) == type
                && BlockShape.isDoorUpper(getBlockMetadataIfLoaded(x, lowerY + 1, z, 0))) {
            rebuildBlockMeshesNow(x, lowerY + 1, z);
        }
        playOpenableSound(type, x, lowerY, z, open);
        RedstoneEngine.rememberPoweredOpenableState(this, x, lowerY, z);
        scheduleMechanismUpdatesAround(x, lowerY, z);
        return true;
    }

    private static int manuallyToggledFenceGateMetadata(int metadata, int playerFacing) {
        boolean wasOpen = (metadata & RedstoneEngine.DOOR_OPEN_BIT) != 0;
        int newMetadata = metadata & ~RedstoneEngine.DOOR_OPEN_BIT;
        if (wasOpen) {
            return newMetadata;
        }
        if (playerFacing >= 0) {
            int facing = newMetadata & 3;
            int playerFacingIndex = playerFacing & 3;
            if (facing == ((playerFacingIndex + 2) & 3)) {
                newMetadata = (newMetadata & ~3) | playerFacingIndex;
            }
        }
        return newMetadata | RedstoneEngine.DOOR_OPEN_BIT;
    }

    public boolean playNoteBlock(int x, int y, int z) {
        if (getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.NOTE_BLOCK) {
            return false;
        }
        TileEntity tile = getTileEntity(x, y, z);
        return tile instanceof NoteBlockTileEntity note && note.play(this);
    }

    public boolean tryActivateNetherPortalFromFire(int x, int y, int z) {
        if (dimension == Dimension.THE_END || getBlock(x, y, z) != BlockType.FIRE) {
            return false;
        }
        NetherPortalInterior portal = findNetherPortalInteriorFromBottomFire(x, y, z);
        if (portal == null) {
            return false;
        }
        fillNetherPortal(portal);
        return true;
    }

    private NetherPortalInterior findNetherPortalInteriorFromBottomFire(int x, int y, int z) {
        for (int offset = 0; offset <= 1; offset++) {
            NetherPortalInterior portal = new NetherPortalInterior(x - offset, y, z, BlockShape.PORTAL_AXIS_X);
            if (netherPortalFrameMatches(portal, false)) {
                return portal;
            }
        }
        for (int offset = 0; offset <= 1; offset++) {
            NetherPortalInterior portal = new NetherPortalInterior(x, y, z - offset, BlockShape.PORTAL_AXIS_Z);
            if (netherPortalFrameMatches(portal, false)) {
                return portal;
            }
        }
        return null;
    }

    private NetherPortalInterior findNetherPortalInteriorContaining(int x, int y, int z, boolean loadedOnly) {
        return findNetherPortalInteriorContaining(x, y, z, loadedOnly, false);
    }

    private NetherPortalInterior findActiveNetherPortalInteriorContaining(int x, int y, int z, boolean loadedOnly) {
        return findNetherPortalInteriorContaining(x, y, z, loadedOnly, true);
    }

    private NetherPortalInterior findNetherPortalInteriorContaining(int x, int y, int z, boolean loadedOnly,
            boolean activeOnly) {
        for (int minY = y - 2; minY <= y; minY++) {
            for (int offset = 0; offset <= 1; offset++) {
                NetherPortalInterior portal = new NetherPortalInterior(x - offset, minY, z, BlockShape.PORTAL_AXIS_X);
                if (netherPortalFrameMatches(portal, loadedOnly, activeOnly)) {
                    return portal;
                }
            }
            for (int offset = 0; offset <= 1; offset++) {
                NetherPortalInterior portal = new NetherPortalInterior(x, minY, z - offset, BlockShape.PORTAL_AXIS_Z);
                if (netherPortalFrameMatches(portal, loadedOnly, activeOnly)) {
                    return portal;
                }
            }
        }
        return null;
    }

    private boolean netherPortalFrameMatches(NetherPortalInterior portal, boolean loadedOnly) {
        return netherPortalFrameMatches(portal, loadedOnly, false);
    }

    private boolean netherPortalFrameMatches(NetherPortalInterior portal, boolean loadedOnly, boolean activeOnly) {
        if (portal.minY() <= 0 || portal.minY() + 2 >= Chunk.HEIGHT - 1) {
            return false;
        }
        return portal.axis() == BlockShape.PORTAL_AXIS_X
                ? netherPortalFrameMatchesX(portal, loadedOnly, activeOnly)
                : netherPortalFrameMatchesZ(portal, loadedOnly, activeOnly);
    }

    private boolean netherPortalFrameMatchesX(NetherPortalInterior portal, boolean loadedOnly, boolean activeOnly) {
        int minX = portal.minX();
        int minY = portal.minY();
        int z = portal.minZ();
        int bottomY = minY - 1;
        int topY = minY + 3;
        for (int y = bottomY; y <= topY; y++) {
            for (int x = minX - 1; x <= minX + 2; x++) {
                boolean frame = y == bottomY || y == topY || x == minX - 1 || x == minX + 2;
                if (frame && !portalBlockIs(x, y, z, BlockType.OBSIDIAN, loadedOnly)) {
                    return false;
                }
                if (!frame && !isNetherPortalInteriorBlock(x, y, z, loadedOnly, activeOnly)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean netherPortalFrameMatchesZ(NetherPortalInterior portal, boolean loadedOnly, boolean activeOnly) {
        int x = portal.minX();
        int minY = portal.minY();
        int minZ = portal.minZ();
        int bottomY = minY - 1;
        int topY = minY + 3;
        for (int y = bottomY; y <= topY; y++) {
            for (int z = minZ - 1; z <= minZ + 2; z++) {
                boolean frame = y == bottomY || y == topY || z == minZ - 1 || z == minZ + 2;
                if (frame && !portalBlockIs(x, y, z, BlockType.OBSIDIAN, loadedOnly)) {
                    return false;
                }
                if (!frame && !isNetherPortalInteriorBlock(x, y, z, loadedOnly, activeOnly)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean portalBlockIs(int x, int y, int z, BlockType expected, boolean loadedOnly) {
        BlockType type = loadedOnly ? getBlockIfLoaded(x, y, z, null) : getBlock(x, y, z);
        return type == null ? loadedOnly : type == expected;
    }

    private boolean isNetherPortalInteriorBlock(int x, int y, int z, boolean loadedOnly, boolean activeOnly) {
        BlockType type = loadedOnly ? getBlockIfLoaded(x, y, z, null) : getBlock(x, y, z);
        if (type == null) {
            return loadedOnly;
        }
        if (activeOnly) {
            return type == BlockType.PORTAL;
        }
        return type == BlockType.AIR || type == BlockType.FIRE || type == BlockType.PORTAL;
    }

    private void fillNetherPortal(NetherPortalInterior portal) {
        List<DeferredBlockChange> changes = new ArrayList<>();
        suppressNeighborSupportUpdates = true;
        try {
            if (portal.axis() == BlockShape.PORTAL_AXIS_X) {
                for (int y = portal.minY(); y <= portal.minY() + 2; y++) {
                    for (int x = portal.minX(); x <= portal.minX() + 1; x++) {
                        deferBlockChange(changes, x, y, portal.minZ(), BlockType.PORTAL, portal.axis());
                    }
                }
            } else {
                for (int y = portal.minY(); y <= portal.minY() + 2; y++) {
                    for (int z = portal.minZ(); z <= portal.minZ() + 1; z++) {
                        deferBlockChange(changes, portal.minX(), y, z, BlockType.PORTAL, portal.axis());
                    }
                }
            }
        } finally {
            suppressNeighborSupportUpdates = false;
        }
        flushDeferredBlockEffects(changes);
    }

    private boolean breakNetherPortal(int x, int y, int z) {
        NetherPortalInterior portal = findNetherPortalInteriorContaining(x, y, z, false);
        if (portal == null) {
            setBlock(x, y, z, BlockType.AIR, 0);
            return true;
        }
        List<DeferredBlockChange> changes = new ArrayList<>();
        suppressNeighborSupportUpdates = true;
        try {
            if (portal.axis() == BlockShape.PORTAL_AXIS_X) {
                for (int py = portal.minY(); py <= portal.minY() + 2; py++) {
                    for (int px = portal.minX(); px <= portal.minX() + 1; px++) {
                        if (getBlock(px, py, portal.minZ()) == BlockType.PORTAL) {
                            deferBlockChange(changes, px, py, portal.minZ(), BlockType.AIR, 0);
                        }
                    }
                }
            } else {
                for (int py = portal.minY(); py <= portal.minY() + 2; py++) {
                    for (int pz = portal.minZ(); pz <= portal.minZ() + 1; pz++) {
                        if (getBlock(portal.minX(), py, pz) == BlockType.PORTAL) {
                            deferBlockChange(changes, portal.minX(), py, pz, BlockType.AIR, 0);
                        }
                    }
                }
            }
        } finally {
            suppressNeighborSupportUpdates = false;
        }
        flushDeferredBlockEffects(changes);
        return true;
    }

    private void updateNetherPortalNeighbors(NetherPortalInterior portal) {
        if (portal.axis() == BlockShape.PORTAL_AXIS_X) {
            for (int y = portal.minY(); y <= portal.minY() + 2; y++) {
                for (int x = portal.minX(); x <= portal.minX() + 1; x++) {
                    updateNeighborSupport(x, y, portal.minZ());
                }
            }
        } else {
            for (int y = portal.minY(); y <= portal.minY() + 2; y++) {
                for (int z = portal.minZ(); z <= portal.minZ() + 1; z++) {
                    updateNeighborSupport(portal.minX(), y, z);
                }
            }
        }
    }

    public BlockPos ensureNetherPortalAt(float x, float y, float z) {
        int targetX = (int) Math.floor(x);
        int targetY = Math.max(4, Math.min(Chunk.HEIGHT - 6, (int) Math.floor(y)));
        int targetZ = (int) Math.floor(z);

        NetherPortalInterior existing = findNearestNetherPortalInteriorAround(targetX, targetY, targetZ,
                NETHER_PORTAL_SEARCH_RADIUS);
        if (existing != null) {
            fillNetherPortal(existing);
            return netherPortalSpawnPos(existing);
        }

        NetherPortalInterior portal = findBuildableNetherPortalInteriorAround(targetX, targetY, targetZ,
                NETHER_PORTAL_CREATE_RADIUS);
        if (portal == null) {
            portal = new NetherPortalInterior(targetX, targetY, targetZ, BlockShape.PORTAL_AXIS_X);
        }
        List<DeferredBlockChange> changes = new ArrayList<>();
        suppressNeighborSupportUpdates = true;
        try {
            buildNetherPortalFrame(portal, changes);
        } finally {
            suppressNeighborSupportUpdates = false;
        }
        flushDeferredBlockEffects(changes);
        fillNetherPortal(portal);
        return netherPortalSpawnPos(portal);
    }

    private NetherPortalInterior findNearestNetherPortalInteriorAround(int targetX, int targetY, int targetZ,
            int radius) {
        NetherPortalInterior best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int x = targetX - radius; x <= targetX + radius; x++) {
            for (int z = targetZ - radius; z <= targetZ + radius; z++) {
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    if (getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.PORTAL) {
                        continue;
                    }
                    NetherPortalInterior portal = findActiveNetherPortalInteriorContaining(x, y, z, true);
                    if (portal == null) {
                        continue;
                    }
                    BlockPos spawn = netherPortalSpawnPos(portal);
                    double dx = spawn.x() + 0.5 - targetX;
                    double dy = spawn.y() - targetY;
                    double dz = spawn.z() + 0.5 - targetZ;
                    double distance = dx * dx + dy * dy + dz * dz;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = portal;
                    }
                }
            }
        }
        return best;
    }

    private NetherPortalInterior findBuildableNetherPortalInteriorAround(int targetX, int targetY, int targetZ,
            int radius) {
        NetherPortalInterior best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int x = targetX - radius; x <= targetX + radius; x++) {
            for (int z = targetZ - radius; z <= targetZ + radius; z++) {
                for (int y = 4; y <= Chunk.HEIGHT - 6; y++) {
                    NetherPortalInterior xAxis = new NetherPortalInterior(x, y, z, BlockShape.PORTAL_AXIS_X);
                    double xDistance = portalDistanceSquared(xAxis, targetX, targetY, targetZ);
                    if (xDistance < bestDistance && isBuildableNetherPortalInterior(xAxis)) {
                        bestDistance = xDistance;
                        best = xAxis;
                    }
                    NetherPortalInterior zAxis = new NetherPortalInterior(x, y, z, BlockShape.PORTAL_AXIS_Z);
                    double zDistance = portalDistanceSquared(zAxis, targetX, targetY, targetZ);
                    if (zDistance < bestDistance && isBuildableNetherPortalInterior(zAxis)) {
                        bestDistance = zDistance;
                        best = zAxis;
                    }
                }
            }
        }
        return best;
    }

    private double portalDistanceSquared(NetherPortalInterior portal, int targetX, int targetY, int targetZ) {
        BlockPos spawn = netherPortalSpawnPos(portal);
        double dx = spawn.x() + 0.5 - targetX;
        double dy = spawn.y() - targetY;
        double dz = spawn.z() + 0.5 - targetZ;
        return dx * dx + dy * dy + dz * dz;
    }

    private boolean isBuildableNetherPortalInterior(NetherPortalInterior portal) {
        if (portal.minY() <= 1 || portal.minY() + 3 >= Chunk.HEIGHT) {
            return false;
        }
        return portal.axis() == BlockShape.PORTAL_AXIS_X
                ? isBuildableNetherPortalInteriorX(portal)
                : isBuildableNetherPortalInteriorZ(portal);
    }

    private boolean isBuildableNetherPortalInteriorX(NetherPortalInterior portal) {
        int minX = portal.minX();
        int minY = portal.minY();
        int z = portal.minZ();
        for (int x = minX - 1; x <= minX + 2; x++) {
            if (!BlockShape.isOpaqueCube(getBlockIfLoaded(x, minY - 2, z, BlockType.AIR))) {
                return false;
            }
        }
        for (int y = minY - 1; y <= minY + 3; y++) {
            for (int x = minX - 1; x <= minX + 2; x++) {
                if (!isReplaceableForNetherPortalBuild(x, y, z)) {
                    return false;
                }
            }
        }
        for (int y = minY; y <= minY + 2; y++) {
            for (int x = minX; x <= minX + 1; x++) {
                if (!isReplaceableForNetherPortalBuild(x, y, z - 1)
                        || !isReplaceableForNetherPortalBuild(x, y, z + 1)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isBuildableNetherPortalInteriorZ(NetherPortalInterior portal) {
        int x = portal.minX();
        int minY = portal.minY();
        int minZ = portal.minZ();
        for (int z = minZ - 1; z <= minZ + 2; z++) {
            if (!BlockShape.isOpaqueCube(getBlockIfLoaded(x, minY - 2, z, BlockType.AIR))) {
                return false;
            }
        }
        for (int y = minY - 1; y <= minY + 3; y++) {
            for (int z = minZ - 1; z <= minZ + 2; z++) {
                if (!isReplaceableForNetherPortalBuild(x, y, z)) {
                    return false;
                }
            }
        }
        for (int y = minY; y <= minY + 2; y++) {
            for (int z = minZ; z <= minZ + 1; z++) {
                if (!isReplaceableForNetherPortalBuild(x - 1, y, z)
                        || !isReplaceableForNetherPortalBuild(x + 1, y, z)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isReplaceableForNetherPortalBuild(int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return false;
        }
        return BlockShape.isReplaceable(getBlockIfLoaded(x, y, z, BlockType.BEDROCK));
    }

    private BlockPos netherPortalSpawnPos(NetherPortalInterior portal) {
        return new BlockPos(portal.minX(), portal.minY(), portal.minZ());
    }

    private void buildNetherPortalFrame(NetherPortalInterior portal, List<DeferredBlockChange> changes) {
        int minY = portal.minY();
        if (portal.axis() == BlockShape.PORTAL_AXIS_X) {
            int minX = portal.minX();
            int z = portal.minZ();
            for (int y = minY - 1; y <= minY + 3; y++) {
                for (int x = minX - 1; x <= minX + 2; x++) {
                    boolean frame = y == minY - 1 || y == minY + 3 || x == minX - 1 || x == minX + 2;
                    deferBlockChange(changes, x, y, z, frame ? BlockType.OBSIDIAN : BlockType.AIR, 0);
                }
            }
            for (int y = minY; y <= minY + 2; y++) {
                for (int x = minX; x <= minX + 1; x++) {
                    deferBlockChange(changes, x, y, z - 1, BlockType.AIR, 0);
                    deferBlockChange(changes, x, y, z + 1, BlockType.AIR, 0);
                }
            }
        } else {
            int x = portal.minX();
            int minZ = portal.minZ();
            for (int y = minY - 1; y <= minY + 3; y++) {
                for (int z = minZ - 1; z <= minZ + 2; z++) {
                    boolean frame = y == minY - 1 || y == minY + 3 || z == minZ - 1 || z == minZ + 2;
                    deferBlockChange(changes, x, y, z, frame ? BlockType.OBSIDIAN : BlockType.AIR, 0);
                }
            }
            for (int y = minY; y <= minY + 2; y++) {
                for (int z = minZ; z <= minZ + 1; z++) {
                    deferBlockChange(changes, x - 1, y, z, BlockType.AIR, 0);
                    deferBlockChange(changes, x + 1, y, z, BlockType.AIR, 0);
                }
            }
        }
    }

    public boolean addEyeToEndPortalFrame(int x, int y, int z) {
        if (getBlock(x, y, z) != BlockType.END_PORTAL_FRAME) {
            return false;
        }
        int metadata = getBlockMetadata(x, y, z);
        if ((metadata & END_PORTAL_FRAME_EYE_BIT) != 0) {
            return false;
        }
        setBlock(x, y, z, BlockType.END_PORTAL_FRAME, metadata | END_PORTAL_FRAME_EYE_BIT);
        spawnEndPortalFrameEyeSmokeParticles(x, y, z);
        tryActivateEndPortalAround(x, y, z);
        return true;
    }

    private void spawnEndPortalFrameEyeSmokeParticles(int x, int y, int z) {
        int amount = Math.min(END_PORTAL_FRAME_EYE_SMOKE_PARTICLES, Math.max(0, MAX_PARTICLES - particles.size()));
        for (int i = 0; i < amount; i++) {
            float particleX = x + END_PORTAL_FRAME_EYE_SMOKE_MIN_OFFSET
                    + displayRandom.nextFloat() * END_PORTAL_FRAME_EYE_SMOKE_RANDOM_OFFSET;
            float particleY = y + END_PORTAL_FRAME_EYE_SMOKE_Y_OFFSET;
            float particleZ = z + END_PORTAL_FRAME_EYE_SMOKE_MIN_OFFSET
                    + displayRandom.nextFloat() * END_PORTAL_FRAME_EYE_SMOKE_RANDOM_OFFSET;
            spawnParticle(WorldParticle.Type.SMOKE, particleX, particleY, particleZ,
                    0.0f, 0.0f, 0.0f, 0.20f, 16);
        }
    }

    public boolean tryActivateEndPortalAround(int frameX, int frameY, int frameZ) {
        for (int centerX = frameX - 2; centerX <= frameX + 2; centerX++) {
            for (int centerZ = frameZ - 2; centerZ <= frameZ + 2; centerZ++) {
                if (isCompleteEndPortalFrame(centerX, frameY, centerZ)) {
                    for (int x = centerX - 1; x <= centerX + 1; x++) {
                        for (int z = centerZ - 1; z <= centerZ + 1; z++) {
                            setBlock(x, frameY, z, BlockType.END_PORTAL, 0);
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isCompleteEndPortalFrame(int centerX, int y, int centerZ) {
        for (int x = centerX - 1; x <= centerX + 1; x++) {
            if (!hasEndPortalEyeFacing(x, y, centerZ - 2, 0)
                    || !hasEndPortalEyeFacing(x, y, centerZ + 2, 2)) {
                return false;
            }
        }
        for (int z = centerZ - 1; z <= centerZ + 1; z++) {
            if (!hasEndPortalEyeFacing(centerX - 2, y, z, 3)
                    || !hasEndPortalEyeFacing(centerX + 2, y, z, 1)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasEndPortalEyeFacing(int x, int y, int z, int facing) {
        int metadata = getBlockMetadataIfLoaded(x, y, z, 0);
        return getBlockIfLoaded(x, y, z, BlockType.AIR) == BlockType.END_PORTAL_FRAME
                && (metadata & 3) == facing
                && (metadata & END_PORTAL_FRAME_EYE_BIT) != 0;
    }

    public boolean isEndPortalAt(int x, int y, int z) {
        return getBlockIfLoaded(x, y, z, BlockType.AIR) == BlockType.END_PORTAL;
    }

    public boolean isNetherPortalAt(int x, int y, int z) {
        return getBlockIfLoaded(x, y, z, BlockType.AIR) == BlockType.PORTAL;
    }

    public void updateAmbientBlockEffects(float deltaTime) {
        ambientBlockEffectAccumulator += Math.max(0.0f, deltaTime) * 20.0f;
        while (ambientBlockEffectAccumulator >= 1.0f) {
            ambientBlockEffectAccumulator -= 1.0f;
            tickAmbientBlockEffects();
        }
    }

    private void tickAmbientBlockEffects() {
        if (player == null) {
            return;
        }
        int baseX = (int) Math.floor(player.getPosition().x);
        int baseY = (int) Math.floor(player.getPosition().y);
        int baseZ = (int) Math.floor(player.getPosition().z);
        if (caveAmbientCooldownTicks > 0) {
            caveAmbientCooldownTicks--;
        }
        for (int i = 0; i < AMBIENT_BLOCK_RANDOM_SAMPLES_PER_TICK; i++) {
            int x = baseX + displayRandom.nextInt(AMBIENT_BLOCK_SAMPLE_BOUND)
                    - displayRandom.nextInt(AMBIENT_BLOCK_SAMPLE_BOUND);
            int y = baseY + displayRandom.nextInt(AMBIENT_BLOCK_SAMPLE_BOUND)
                    - displayRandom.nextInt(AMBIENT_BLOCK_SAMPLE_BOUND);
            int z = baseZ + displayRandom.nextInt(AMBIENT_BLOCK_SAMPLE_BOUND)
                    - displayRandom.nextInt(AMBIENT_BLOCK_SAMPLE_BOUND);
            tickCaveAmbientAt(baseX, baseY, baseZ, x, y, z, displayRandom);
            tickDepthSuspendAmbientAt(x, y, z, displayRandom);
            if (y >= 0 && y < Chunk.HEIGHT) {
                tickNetherPortalAmbientAt(x, y, z, displayRandom);
                tickAmbientBlockParticleAt(x, y, z, displayRandom);
            }
        }
    }

    public boolean tickCaveAmbientAt(int playerX, int playerY, int playerZ,
            int x, int y, int z, Random randomSource) {
        if (dimension != Dimension.OVERWORLD || caveAmbientCooldownTicks > 0
                || y < 0 || y >= Chunk.HEIGHT) {
            return false;
        }
        Random source = randomSource == null ? displayRandom : randomSource;
        if (source.nextInt(CAVE_AMBIENT_SOUND_CHANCE) != 0
                || !isDarkCaveAmbientCandidate(x, y, z)) {
            return false;
        }
        int dx = x - playerX;
        int dy = y - playerY;
        int dz = z - playerZ;
        if (dx * dx + dy * dy + dz * dz < CAVE_AMBIENT_MIN_DISTANCE_SQ) {
            return false;
        }
        playSound(WorldSoundEvent.AMBIENT_CAVE,
                x + 0.5f, y + 0.5f, z + 0.5f,
                0.7f, 0.8f + source.nextFloat() * 0.2f);
        caveAmbientCooldownTicks = CAVE_AMBIENT_MIN_COOLDOWN_TICKS
                + source.nextInt(CAVE_AMBIENT_RANDOM_COOLDOWN_TICKS);
        return true;
    }

    private boolean isDarkCaveAmbientCandidate(int x, int y, int z) {
        return isChunkGeneratedForBlock(x, z)
                && getBlockIfLoaded(x, y, z, BlockType.BEDROCK) == BlockType.AIR
                && getSkyLight(x, y, z) <= CAVE_AMBIENT_MAX_SKY_LIGHT
                && getBlockLightIfLoaded(x, y, z, 15) <= CAVE_AMBIENT_MAX_BLOCK_LIGHT
                && !canSeeSky(x, y, z);
    }

    public boolean tickAmbientBlockParticleAt(int x, int y, int z, Random randomSource) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return false;
        }
        Random source = randomSource == null ? displayRandom : randomSource;
        BlockType type = getBlockIfLoaded(x, y, z, BlockType.AIR);
        boolean emitted = false;
        if (type.isLava() && getBlockIfLoaded(x, y + 1, z, BlockType.AIR) == BlockType.AIR
                && source.nextInt(AMBIENT_LAVA_PARTICLE_CHANCE) == 0) {
            spawnLavaParticle(x, y, z, source);
            emitted = true;
        }
        if (type.isWater() && canEmitSuspendedWaterParticleAt(x, y, z)
                && source.nextInt(AMBIENT_WATER_SUSPENDED_CHANCE) == 0) {
            spawnSuspendedWaterParticle(x, y, z, source);
            emitted = true;
        }
        if (type == BlockType.MYCELIUM && source.nextInt(AMBIENT_MYCELIUM_TOWN_AURA_CHANCE) == 0) {
            spawnTownAuraParticle(x, y, z, source);
            emitted = true;
        }
        if (type == BlockType.FIRE) {
            spawnFireLargeSmokeParticle(x, y, z, source);
            emitted = true;
        }
        if (type == BlockType.TORCH) {
            spawnTorchSmokeAndFlameParticles(x, y, z, getBlockMetadataIfLoaded(x, y, z, 0));
            emitted = true;
        } else if (type == BlockType.REDSTONE_TORCH_ON) {
            spawnActiveRedstoneTorchDustParticle(x, y, z, getBlockMetadataIfLoaded(x, y, z, 0), source);
            emitted = true;
        } else if (type == BlockType.REDSTONE_REPEATER_ON) {
            spawnActiveRedstoneRepeaterDustParticle(x, y, z, getBlockMetadataIfLoaded(x, y, z, 0), source);
            emitted = true;
        } else if (type == BlockType.BREWING_STAND) {
            spawnBrewingStandSmokeParticle(x, y, z, source);
            emitted = true;
        } else if (type == BlockType.END_PORTAL) {
            spawnEndPortalSmokeParticle(x, y, z, source);
            emitted = true;
        }
        if (type == BlockType.REDSTONE_WIRE && getBlockMetadataIfLoaded(x, y, z, 0) > 0) {
            spawnRedstoneDustParticle(x, y, z, getBlockMetadataIfLoaded(x, y, z, 0), source);
            emitted = true;
        } else if (type == BlockType.GLOWING_REDSTONE_ORE) {
            spawnRedstoneOreSparkleParticles(x, y, z, source);
            emitted = true;
        }
        BlockType above = getBlockIfLoaded(x, y + 1, z, BlockType.AIR);
        if (canEmitLiquidDripThrough(type) && getBlockIfLoaded(x, y - 1, z, BlockType.AIR) == BlockType.AIR) {
            if (above.isWater()) {
                spawnLiquidDripParticle(WorldParticle.Type.DRIP_WATER, x, y, z, source);
                emitted = true;
            } else if (above.isLava()) {
                spawnLiquidDripParticle(WorldParticle.Type.DRIP_LAVA, x, y, z, source);
                emitted = true;
            }
        }
        return emitted;
    }

    private void spawnTorchSmokeAndFlameParticles(int x, int y, int z, int metadata) {
        float[] point = torchDisplayPoint(x, y, z, metadata);
        spawnParticle(WorldParticle.Type.SMOKE, point[0], point[1], point[2],
                0.0f, 0.0f, 0.0f, 0.20f, 16);
        spawnParticle(WorldParticle.Type.FLAME, point[0], point[1], point[2],
                0.0f, 0.0f, 0.0f, 0.16f, 8);
    }

    private void spawnActiveRedstoneTorchDustParticle(int x, int y, int z, int metadata, Random source) {
        float particleX = x + 0.5f + (source.nextFloat() - 0.5f) * REDSTONE_TORCH_DISPLAY_JITTER;
        float particleY = y + TORCH_DISPLAY_Y_OFFSET
                + (source.nextFloat() - 0.5f) * REDSTONE_TORCH_DISPLAY_JITTER;
        float particleZ = z + 0.5f + (source.nextFloat() - 0.5f) * REDSTONE_TORCH_DISPLAY_JITTER;
        float[] point = offsetTorchDisplayPoint(particleX, particleY, particleZ, metadata);
        spawnParticle(WorldParticle.Type.RED_DUST, point[0], point[1], point[2],
                0.0f, 0.0f, 0.0f, 0.08f, 18 + source.nextInt(8),
                WorldParticle.RED_DUST_DEFAULT_COLOR_DATA);
    }

    private void spawnActiveRedstoneRepeaterDustParticle(int x, int y, int z, int metadata, Random source) {
        int direction = metadata & 3;
        float particleX = x + 0.5f + (source.nextFloat() - 0.5f) * REDSTONE_REPEATER_DISPLAY_JITTER;
        float particleY = y + REDSTONE_REPEATER_DISPLAY_Y_OFFSET
                + (source.nextFloat() - 0.5f) * REDSTONE_REPEATER_DISPLAY_JITTER;
        float particleZ = z + 0.5f + (source.nextFloat() - 0.5f) * REDSTONE_REPEATER_DISPLAY_JITTER;
        float offsetX = 0.0f;
        float offsetZ = 0.0f;
        if (source.nextInt(2) == 0) {
            switch (direction) {
                case 0 -> offsetZ = -REDSTONE_REPEATER_OUTPUT_TORCH_OFFSET;
                case 2 -> offsetZ = REDSTONE_REPEATER_OUTPUT_TORCH_OFFSET;
                case 3 -> offsetX = -REDSTONE_REPEATER_OUTPUT_TORCH_OFFSET;
                case 1 -> offsetX = REDSTONE_REPEATER_OUTPUT_TORCH_OFFSET;
                default -> {
                }
            }
        } else {
            int delay = (metadata & RedstoneEngine.REPEATER_DELAY_MASK) >> RedstoneEngine.REPEATER_DELAY_SHIFT;
            float rearOffset = REDSTONE_REPEATER_REAR_TORCH_OFFSETS[delay];
            switch (direction) {
                case 0 -> offsetZ = rearOffset;
                case 2 -> offsetZ = -rearOffset;
                case 3 -> offsetX = rearOffset;
                case 1 -> offsetX = -rearOffset;
                default -> {
                }
            }
        }
        spawnParticle(WorldParticle.Type.RED_DUST,
                particleX + offsetX,
                particleY,
                particleZ + offsetZ,
                0.0f, 0.0f, 0.0f,
                0.08f, 18 + source.nextInt(8),
                WorldParticle.RED_DUST_DEFAULT_COLOR_DATA);
    }

    private void spawnBrewingStandSmokeParticle(int x, int y, int z, Random source) {
        spawnParticle(WorldParticle.Type.SMOKE,
                x + 0.4f + source.nextFloat() * 0.2f,
                y + 0.7f + source.nextFloat() * 0.3f,
                z + 0.4f + source.nextFloat() * 0.2f,
                0.0f, 0.0f, 0.0f,
                0.20f, 16);
    }

    private void spawnEndPortalSmokeParticle(int x, int y, int z, Random source) {
        spawnParticle(WorldParticle.Type.SMOKE,
                x + source.nextFloat(),
                y + 0.8f,
                z + source.nextFloat(),
                0.0f, 0.0f, 0.0f,
                0.20f, 16);
    }

    private static float[] torchDisplayPoint(int x, int y, int z, int metadata) {
        return offsetTorchDisplayPoint(x + 0.5f, y + TORCH_DISPLAY_Y_OFFSET, z + 0.5f, metadata);
    }

    private static float[] offsetTorchDisplayPoint(float particleX, float particleY, float particleZ, int metadata) {
        return switch (metadata & 7) {
            case 1 -> new float[] { particleX - TORCH_DISPLAY_WALL_OFFSET,
                    particleY + TORCH_DISPLAY_WALL_Y_OFFSET, particleZ };
            case 2 -> new float[] { particleX + TORCH_DISPLAY_WALL_OFFSET,
                    particleY + TORCH_DISPLAY_WALL_Y_OFFSET, particleZ };
            case 3 -> new float[] { particleX, particleY + TORCH_DISPLAY_WALL_Y_OFFSET,
                    particleZ - TORCH_DISPLAY_WALL_OFFSET };
            case 4 -> new float[] { particleX, particleY + TORCH_DISPLAY_WALL_Y_OFFSET,
                    particleZ + TORCH_DISPLAY_WALL_OFFSET };
            default -> new float[] { particleX, particleY, particleZ };
        };
    }

    public boolean tickDepthSuspendAmbientAt(int x, int y, int z, Random randomSource) {
        if (dimension != Dimension.OVERWORLD || y >= DEPTH_SUSPEND_START_Y) {
            return false;
        }
        if (y >= 0 && y < Chunk.HEIGHT
                && (!isChunkGeneratedForBlock(x, z) || getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.AIR)) {
            return false;
        }
        Random source = randomSource == null ? displayRandom : randomSource;
        if (source.nextInt(AMBIENT_DEPTH_SUSPEND_CHANCE) != 0) {
            return false;
        }
        spawnDepthSuspendParticle(x, y, z, source);
        return true;
    }

    private void spawnLiquidDripParticle(WorldParticle.Type type, int x, int y, int z, Random source) {
        spawnParticle(type,
                x + source.nextFloat(),
                y - 0.05f,
                z + source.nextFloat(),
                0.0f, 0.0f, 0.0f,
                0.045f, dripParticleLifetime(source));
    }

    private static int dripParticleLifetime(Random source) {
        return (int) (64.0f / (source.nextFloat() * 0.8f + 0.2f));
    }

    private void spawnSuspendedWaterParticle(int x, int y, int z, Random source) {
        spawnParticle(WorldParticle.Type.SUSPENDED,
                x + source.nextFloat(),
                y + source.nextFloat(),
                z + source.nextFloat(),
                0.0f, 0.0f, 0.0f,
                0.02f + source.nextFloat() * 0.06f,
                suspendedParticleLifetime(source));
    }

    private boolean canEmitSuspendedWaterParticleAt(int x, int y, int z) {
        int metadata = getBlockMetadataIfLoaded(x, y, z, 0);
        return metadata <= 0 || metadata >= 8;
    }

    private void spawnDepthSuspendParticle(int x, int y, int z, Random source) {
        float depthFactor = Math.max(0.0f, Math.min(1.0f,
                (DEPTH_SUSPEND_START_Y - y) / (float) DEPTH_SUSPEND_START_Y));
        spawnParticle(WorldParticle.Type.DEPTH_SUSPEND,
                x + source.nextFloat(),
                y + source.nextFloat(),
                z + source.nextFloat(),
                0.0f, 0.0f, 0.0f,
                0.025f + depthFactor * 0.035f,
                auraParticleLifetime(source));
    }

    private void spawnTownAuraParticle(int x, int y, int z, Random source) {
        spawnParticle(WorldParticle.Type.TOWN_AURA,
                x + source.nextFloat(),
                y + 1.1f,
                z + source.nextFloat(),
                0.0f, 0.0f, 0.0f,
                0.035f * (source.nextFloat() * 0.6f + 0.5f),
                auraParticleLifetime(source));
    }

    private static int suspendedParticleLifetime(Random source) {
        return (int) (16.0f / (source.nextFloat() * 0.8f + 0.2f));
    }

    private static int auraParticleLifetime(Random source) {
        return (int) (20.0f / (source.nextFloat() * 0.8f + 0.2f));
    }

    private void spawnFireLargeSmokeParticle(int x, int y, int z, Random source) {
        if (isFireTopSupported(x, y, z)) {
            for (int i = 0; i < FIRE_TOP_SMOKE_PARTICLES; i++) {
                spawnFireLargeSmokeParticleAt(
                        x + source.nextFloat(),
                        y + source.nextFloat() * 0.5f + 0.5f,
                        z + source.nextFloat());
            }
            return;
        }
        if (canCatchFire(getBlockIfLoaded(x - 1, y, z, BlockType.AIR))) {
            for (int i = 0; i < FIRE_SIDE_SMOKE_PARTICLES; i++) {
                spawnFireLargeSmokeParticleAt(
                        x + source.nextFloat() * FIRE_SIDE_SMOKE_INSET,
                        y + source.nextFloat(),
                        z + source.nextFloat());
            }
        }
        if (canCatchFire(getBlockIfLoaded(x + 1, y, z, BlockType.AIR))) {
            for (int i = 0; i < FIRE_SIDE_SMOKE_PARTICLES; i++) {
                spawnFireLargeSmokeParticleAt(
                        x + 1.0f - source.nextFloat() * FIRE_SIDE_SMOKE_INSET,
                        y + source.nextFloat(),
                        z + source.nextFloat());
            }
        }
        if (canCatchFire(getBlockIfLoaded(x, y, z - 1, BlockType.AIR))) {
            for (int i = 0; i < FIRE_SIDE_SMOKE_PARTICLES; i++) {
                spawnFireLargeSmokeParticleAt(
                        x + source.nextFloat(),
                        y + source.nextFloat(),
                        z + source.nextFloat() * FIRE_SIDE_SMOKE_INSET);
            }
        }
        if (canCatchFire(getBlockIfLoaded(x, y, z + 1, BlockType.AIR))) {
            for (int i = 0; i < FIRE_SIDE_SMOKE_PARTICLES; i++) {
                spawnFireLargeSmokeParticleAt(
                        x + source.nextFloat(),
                        y + source.nextFloat(),
                        z + 1.0f - source.nextFloat() * FIRE_SIDE_SMOKE_INSET);
            }
        }
        if (canCatchFire(getBlockIfLoaded(x, y + 1, z, BlockType.AIR))) {
            for (int i = 0; i < FIRE_SIDE_SMOKE_PARTICLES; i++) {
                spawnFireLargeSmokeParticleAt(
                        x + source.nextFloat(),
                        y + 1.0f - source.nextFloat() * FIRE_SIDE_SMOKE_INSET,
                        z + source.nextFloat());
            }
        }
    }

    private boolean isFireTopSupported(int x, int y, int z) {
        BlockType below = getBlockIfLoaded(x, y - 1, z, BlockType.AIR);
        return BlockShape.isOpaqueCube(below) || canCatchFire(below);
    }

    private void spawnFireLargeSmokeParticleAt(float x, float y, float z) {
        spawnParticle(WorldParticle.Type.LARGE_SMOKE,
                x, y, z,
                0.0f, 0.0f, 0.0f,
                0.30f, 22);
    }

    private void spawnLavaParticle(int x, int y, int z, Random source) {
        spawnParticle(WorldParticle.Type.LAVA,
                x + source.nextFloat(),
                y + 1.0f,
                z + source.nextFloat(),
                (source.nextFloat() - 0.5f) * 0.04f,
                0.08f + source.nextFloat() * 0.06f,
                (source.nextFloat() - 0.5f) * 0.04f,
                0.10f + source.nextFloat() * 0.04f,
                12 + source.nextInt(8));
    }

    private void spawnSnowGolemCreationParticles(int x, int lowerSnowY, int z) {
        int amount = Math.min(SNOW_GOLEM_CREATION_PARTICLES, Math.max(0, MAX_PARTICLES - particles.size()));
        for (int i = 0; i < amount; i++) {
            spawnParticle(WorldParticle.Type.SNOW_SHOVEL,
                    x + random.nextFloat(),
                    lowerSnowY + random.nextFloat() * 2.5f,
                    z + random.nextFloat(),
                    0.0f, 0.0f, 0.0f,
                    0.13f + random.nextFloat() * 0.035f,
                    18 + random.nextInt(10));
        }
    }

    private void spawnRedstoneDustParticle(int x, int y, int z, int power, Random source) {
        spawnParticle(WorldParticle.Type.RED_DUST,
                x + 0.5f + (source.nextFloat() - 0.5f) * 0.2f,
                y + 0.0625f,
                z + 0.5f + (source.nextFloat() - 0.5f) * 0.2f,
                0.0f, 0.0f, 0.0f,
                0.08f, 18 + source.nextInt(8),
                Math.max(1, Math.min(15, power)));
    }

    private static boolean canEmitLiquidDripThrough(BlockType type) {
        return type != null && type != BlockType.AIR && !type.isFluid()
                && type.isSolid() && !type.isTransparent();
    }

    public boolean tickNetherPortalAmbientAt(int x, int y, int z, Random randomSource) {
        if (getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.PORTAL) {
            return false;
        }
        Random source = randomSource == null ? displayRandom : randomSource;
        if (source.nextInt(NETHER_PORTAL_AMBIENT_SOUND_CHANCE) == 0) {
            playSound(WorldSoundEvent.PORTAL_AMBIENT,
                    x + 0.5f, y + 0.5f, z + 0.5f,
                    0.5f, WorldSoundEvent.portalAmbientPitch(source));
        }
        for (int i = 0; i < NETHER_PORTAL_AMBIENT_PARTICLES; i++) {
            spawnNetherPortalAmbientParticle(x, y, z, source);
        }
        return true;
    }

    private void spawnNetherPortalAmbientParticle(int x, int y, int z, Random source) {
        float particleX = x + source.nextFloat();
        float particleY = y + source.nextFloat();
        float particleZ = z + source.nextFloat();
        int side = source.nextInt(2) * 2 - 1;
        float motionX = (source.nextFloat() - 0.5f) * 0.5f;
        float motionY = (source.nextFloat() - 0.5f) * 0.5f;
        float motionZ = (source.nextFloat() - 0.5f) * 0.5f;
        if (getBlockIfLoaded(x - 1, y, z, BlockType.AIR) != BlockType.PORTAL
                && getBlockIfLoaded(x + 1, y, z, BlockType.AIR) != BlockType.PORTAL) {
            particleX = x + 0.5f + 0.25f * side;
            motionX = source.nextFloat() * 2.0f * side;
        } else {
            particleZ = z + 0.5f + 0.25f * side;
            motionZ = source.nextFloat() * 2.0f * side;
        }
        spawnParticle(WorldParticle.Type.PORTAL, particleX, particleY, particleZ,
                motionX, motionY, motionZ, PORTAL_PARTICLE_SCALE, PORTAL_PARTICLE_LIFETIME_TICKS);
    }

    public void ensureEndSpawnPlatform() {
        int cx = DimensionTransferService.END_PLATFORM_CENTER_X;
        int cy = DimensionTransferService.END_PLATFORM_Y;
        int cz = DimensionTransferService.END_PLATFORM_CENTER_Z;
        int radius = DimensionTransferService.END_PLATFORM_RADIUS;
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                setBlock(x, cy, z, BlockType.OBSIDIAN, 0);
                for (int y = cy + 1; y <= cy + DimensionTransferService.END_PLATFORM_CLEARANCE; y++) {
                    setBlock(x, y, z, BlockType.AIR, 0);
                }
            }
        }
    }

    public boolean placeDoor(int x, int y, int z, BlockType type, int facing, AABB playerBox) {
        if (!type.isDoor() || y + 1 >= Chunk.HEIGHT) {
            return false;
        }
        if (!canPlaceBlockAt(x, y, z, type, facing, playerBox)
                || !BlockShape.isReplaceable(getBlock(x, y + 1, z))) {
            return false;
        }
        if (playerBox != null) {
            for (AABB box : getPlacementCollisionBoxes(x, y + 1, z, type, 8)) {
                if (box.intersects(playerBox)) {
                    return false;
                }
            }
        }
        int upperMetadata = doorUpperMetadata(x, y, z, type, facing);
        List<DeferredBlockChange> changes = new ArrayList<>(2);
        suppressNeighborSupportUpdates = true;
        try {
            deferBlockChange(changes, x, y, z, type, facing);
            deferBlockChange(changes, x, y + 1, z, type, upperMetadata);
        } finally {
            suppressNeighborSupportUpdates = false;
        }
        flushDeferredBlockEffects(changes);
        return true;
    }

    private int doorUpperMetadata(int x, int y, int z, BlockType type, int facing) {
        int sideX = 0;
        int sideZ = 0;
        switch (facing & 3) {
            case 0 -> sideZ = 1;
            case 1 -> sideX = -1;
            case 2 -> sideZ = -1;
            default -> sideX = 1;
        }

        int leftCount = doorHingeCount(x - sideX, y, z - sideZ);
        int rightCount = doorHingeCount(x + sideX, y, z + sideZ);
        boolean leftDoor = isSameDoorColumn(x - sideX, y, z - sideZ, type);
        boolean rightDoor = isSameDoorColumn(x + sideX, y, z + sideZ, type);
        boolean rightHinge = leftDoor && !rightDoor;
        if (!rightHinge && rightCount > leftCount) {
            rightHinge = true;
        }
        return 8 | (rightHinge ? 1 : 0);
    }

    private int doorHingeCount(int x, int y, int z) {
        int count = BlockShape.isOpaqueCube(getBlock(x, y, z)) ? 1 : 0;
        return count + (BlockShape.isOpaqueCube(getBlock(x, y + 1, z)) ? 1 : 0);
    }

    private boolean isSameDoorColumn(int x, int y, int z, BlockType type) {
        return getBlock(x, y, z) == type || getBlock(x, y + 1, z) == type;
    }

    public boolean placeTrapdoor(int x, int y, int z, int attachedFace, AABB playerBox) {
        if (attachedFace == Block.FACE_TOP || attachedFace == Block.FACE_BOTTOM) {
            return false;
        }
        int metadata = horizontalIndexFromFace(attachedFace);
        if (!canPlaceBlockAt(x, y, z, BlockType.TRAPDOOR, metadata, playerBox)) {
            return false;
        }
        setBlock(x, y, z, BlockType.TRAPDOOR, metadata);
        return true;
    }

    public boolean placeStoneButton(int x, int y, int z, int attachedFace, AABB playerBox) {
        int metadata = BlockShape.buttonMetadataFromFace(attachedFace);
        if (metadata < 0) {
            return false;
        }
        if (!canPlaceBlockAt(x, y, z, BlockType.STONE_BUTTON, metadata, playerBox)) {
            return false;
        }
        setBlock(x, y, z, BlockType.STONE_BUTTON, metadata);
        return true;
    }

    public boolean placeLever(int x, int y, int z, int attachedFace, AABB playerBox) {
        boolean verticalAttachment = attachedFace == Block.FACE_TOP || attachedFace == Block.FACE_BOTTOM;
        int metadata = BlockShape.leverMetadataFromFace(attachedFace, verticalAttachment && random.nextBoolean());
        if (metadata < 0) {
            return false;
        }
        if (!canPlaceBlockAt(x, y, z, BlockType.LEVER, metadata, playerBox)) {
            return false;
        }
        setBlock(x, y, z, BlockType.LEVER, metadata);
        return true;
    }

    public BlockPos placeBed(int x, int y, int z, int facing, AABB playerBox) {
        int[] dir = horizontalDirection(facing);
        int headX = x + dir[0];
        int headZ = z + dir[1];
        if (!canPlaceBlockAt(x, y, z, BlockType.BED, facing, playerBox)
                || !canPlaceBlockAt(headX, y, headZ, BlockType.BED, facing | 8, playerBox)) {
            return null;
        }
        List<DeferredBlockChange> changes = new ArrayList<>(2);
        suppressNeighborSupportUpdates = true;
        try {
            deferBlockChange(changes, x, y, z, BlockType.BED, facing);
            deferBlockChange(changes, headX, y, headZ, BlockType.BED, facing | 8);
        } finally {
            suppressNeighborSupportUpdates = false;
        }
        flushDeferredBlockEffects(changes);
        return new BlockPos(x, y, z);
    }

    public BedUseResult useBed(int x, int y, int z) {
        BedParts parts = bedPartsAt(x, y, z);
        if (parts == null) {
            return new BedUseResult(BedUseOutcome.NOT_BED, null, null);
        }
        if (dimension != Dimension.OVERWORLD) {
            explodeBed(parts);
            return new BedUseResult(BedUseOutcome.EXPLODED, parts.foot(), parts.head());
        }
        if (isBedOccupied(parts)) {
            return new BedUseResult(BedUseOutcome.OCCUPIED, parts.foot(), parts.head());
        }
        if (dayCycleManager == null || !dayCycleManager.isNight()) {
            return new BedUseResult(BedUseOutcome.NOT_NIGHT, parts.foot(), parts.head());
        }
        if (hasSleepPreventingMonsterNear(parts)) {
            return new BedUseResult(BedUseOutcome.MONSTERS_NEARBY, parts.foot(), parts.head());
        }
        setBedOccupied(parts, true);
        return new BedUseResult(BedUseOutcome.SLEEP_ALLOWED, parts.foot(), parts.head());
    }

    public boolean setBedOccupied(int x, int y, int z, boolean occupied) {
        BedParts parts = bedPartsAt(x, y, z);
        if (parts == null) {
            return false;
        }
        setBedOccupied(parts, occupied);
        return true;
    }

    public boolean completeBedSleep(BedUseResult result) {
        if (result == null || !result.sleepAllowed() || result.footPos() == null) {
            return false;
        }
        try {
            if (dayCycleManager != null) {
                dayCycleManager.skipToMorning();
            }
            setWeatherState("clear");
            return true;
        } finally {
            BlockPos foot = result.footPos();
            setBedOccupied(foot.x(), foot.y(), foot.z(), false);
        }
    }

    public BlockPos getBedFootPos(int x, int y, int z) {
        BedParts parts = bedPartsAt(x, y, z);
        return parts == null ? null : parts.foot();
    }

    public BlockPos findBedRespawnPosition(int x, int y, int z) {
        BedParts parts = bedPartsAt(x, y, z);
        if (parts == null) {
            return null;
        }
        int facing = parts.footMetadata() & 3;
        int[] dir = horizontalDirection(facing);
        for (int bedPart = 0; bedPart <= 1; bedPart++) {
            int baseX = parts.foot().x() + dir[0] * bedPart;
            int baseZ = parts.foot().z() + dir[1] * bedPart;
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int candidateX = baseX + dx;
                    int candidateZ = baseZ + dz;
                    if (isValidBedRespawnPosition(candidateX, y, candidateZ)) {
                        return new BlockPos(candidateX, y, candidateZ);
                    }
                }
            }
        }
        return null;
    }

    private boolean isValidBedRespawnPosition(int x, int y, int z) {
        if (y <= 0 || y + 2 >= Chunk.HEIGHT) {
            return false;
        }
        if (!BlockShape.canSupportBed(getBlockIfLoaded(x, y - 1, z, BlockType.AIR))) {
            return false;
        }
        AABB playerBox = new AABB(
                x + 0.5f - BED_RESPAWN_HALF_WIDTH, y, z + 0.5f - BED_RESPAWN_HALF_WIDTH,
                x + 0.5f + BED_RESPAWN_HALF_WIDTH, y + BED_RESPAWN_HEIGHT, z + 0.5f + BED_RESPAWN_HALF_WIDTH);
        return !isBlockedByLoadedCollision(playerBox);
    }

    private BedParts bedPartsAt(int x, int y, int z) {
        if (getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.BED) {
            return null;
        }
        int metadata = getBlockMetadataIfLoaded(x, y, z, 0);
        int facing = metadata & 3;
        int[] dir = horizontalDirection(facing);
        int footX = BlockShape.isBedHead(metadata) ? x - dir[0] : x;
        int footZ = BlockShape.isBedHead(metadata) ? z - dir[1] : z;
        int headX = footX + dir[0];
        int headZ = footZ + dir[1];
        if (getBlockIfLoaded(footX, y, footZ, BlockType.AIR) != BlockType.BED
                || getBlockIfLoaded(headX, y, headZ, BlockType.AIR) != BlockType.BED) {
            return null;
        }
        int footMetadata = getBlockMetadataIfLoaded(footX, y, footZ, 0);
        int headMetadata = getBlockMetadataIfLoaded(headX, y, headZ, 0);
        if (BlockShape.isBedHead(footMetadata) || !BlockShape.isBedHead(headMetadata)
                || (footMetadata & 3) != (headMetadata & 3)) {
            return null;
        }
        return new BedParts(new BlockPos(footX, y, footZ), new BlockPos(headX, y, headZ),
                footMetadata, headMetadata);
    }

    private boolean isBedOccupied(BedParts parts) {
        return (parts.headMetadata() & BED_OCCUPIED_BIT) != 0;
    }

    private void setBedOccupied(BedParts parts, boolean occupied) {
        int footMetadata = bedMetadataWithOccupied(parts.footMetadata(), false);
        int headMetadata = bedMetadataWithOccupied(parts.headMetadata(), occupied);
        boolean changed = parts.footMetadata() != footMetadata || parts.headMetadata() != headMetadata;
        setBlock(parts.foot().x(), parts.foot().y(), parts.foot().z(),
                BlockType.BED, footMetadata);
        setBlock(parts.head().x(), parts.head().y(), parts.head().z(),
                BlockType.BED, headMetadata);
        if (changed) {
            rebuildBlockMeshesNow(parts.foot().x(), parts.foot().y(), parts.foot().z());
            rebuildBlockMeshesNow(parts.head().x(), parts.head().y(), parts.head().z());
        }
    }

    private static int bedMetadataWithOccupied(int metadata, boolean occupied) {
        return occupied ? metadata | BED_OCCUPIED_BIT : metadata & ~BED_OCCUPIED_BIT;
    }

    private boolean hasSleepPreventingMonsterNear(BedParts parts) {
        BlockPos head = parts.head();
        AABB area = new AABB(
                head.x() - BED_MONSTER_CHECK_HORIZONTAL_RANGE,
                head.y() - BED_MONSTER_CHECK_VERTICAL_RANGE,
                head.z() - BED_MONSTER_CHECK_HORIZONTAL_RANGE,
                head.x() + BED_MONSTER_CHECK_HORIZONTAL_RANGE,
                head.y() + BED_MONSTER_CHECK_VERTICAL_RANGE,
                head.z() + BED_MONSTER_CHECK_HORIZONTAL_RANGE);
        return hasSleepPreventingMonsterNear(area, entities)
                || hasSleepPreventingMonsterNear(area, entitiesToAdd);
    }

    private boolean hasSleepPreventingMonsterNear(AABB area, List<Entity> candidates) {
        for (Entity entity : candidates) {
            if (entity instanceof Mob mob && isSleepPreventingMonster(mob)
                    && mob.getBoundingBox().intersects(area)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSleepPreventingMonster(Mob mob) {
        if (mob == null || mob.isRemoved() || mob.isDead() || !mob.isHostile()) {
            return false;
        }
        MobDefinition definition = mob.getDefinition();
        return definition != MobDefinition.GHAST
                && definition != MobDefinition.SLIME
                && definition != MobDefinition.MAGMA_CUBE
                && definition != MobDefinition.ENDER_DRAGON;
    }

    private void explodeBed(BedParts parts) {
        removeBedWithoutDrops(parts);
        float x = parts.head().x() + 0.5f;
        float y = parts.head().y() + 0.5f;
        float z = parts.head().z() + 0.5f;
        explode(x, y, z, BED_EXPLOSION_POWER, true);
    }

    private void removeBedWithoutDrops(BedParts parts) {
        List<DeferredBlockChange> changes = new ArrayList<>(2);
        suppressNeighborSupportUpdates = true;
        try {
            deferBlockChange(changes, parts.foot().x(), parts.foot().y(), parts.foot().z(), BlockType.AIR, 0);
            deferBlockChange(changes, parts.head().x(), parts.head().y(), parts.head().z(), BlockType.AIR, 0);
        } finally {
            suppressNeighborSupportUpdates = false;
        }
        flushDeferredBlockEffects(changes);
    }

    public boolean tryMergeSlab(int x, int y, int z) {
        if (getBlock(x, y, z) != BlockType.STONE_SLAB) {
            return false;
        }
        setBlock(x, y, z, BlockType.DOUBLE_STONE_SLAB, getBlockMetadata(x, y, z));
        return true;
    }

    public boolean isBlockSupported(int x, int y, int z) {
        BlockType type = getBlock(x, y, z);
        int metadata = getBlockMetadata(x, y, z);
        if (type == BlockType.AIR) {
            return true;
        }
        if (type.isDoor()) {
            if (BlockShape.isDoorUpper(metadata)) {
                return getBlock(x, y - 1, z) == type && !BlockShape.isDoorUpper(getBlockMetadata(x, y - 1, z));
            }
            return BlockShape.canSupportDoor(getBlock(x, y - 1, z))
                    && getBlock(x, y + 1, z) == type
                    && BlockShape.isDoorUpper(getBlockMetadata(x, y + 1, z));
        }
        if (type.isBed()) {
            if (!BlockShape.canSupportBed(getBlock(x, y - 1, z))) {
                return false;
            }
            return hasMatchingBedHalf(x, y, z, metadata, false);
        }
        if (type == BlockType.PORTAL) {
            return findActiveNetherPortalInteriorContaining(x, y, z, false) != null;
        }
        if (isSmallMushroom(type)) {
            return BlockShape.canPlaceAt(type, metadata, contextAt(x, y, z))
                    && canSmallMushroomStayAt(x, y, z, false);
        }
        return BlockShape.canPlaceAt(type, metadata, contextAt(x, y, z));
    }

    private boolean isBlockSupportedIfLoaded(int x, int y, int z) {
        if (!isChunkGeneratedForBlock(x, z)) {
            return true;
        }
        BlockType type = getBlockIfLoaded(x, y, z, BlockType.AIR);
        int metadata = getBlockMetadataIfLoaded(x, y, z, 0);
        if (type == BlockType.AIR) {
            return true;
        }
        if (type.isDoor()) {
            if (BlockShape.isDoorUpper(metadata)) {
                return getBlockIfLoaded(x, y - 1, z, BlockType.AIR) == type
                        && !BlockShape.isDoorUpper(getBlockMetadataIfLoaded(x, y - 1, z, 0));
            }
            return BlockShape.canSupportDoor(getBlockIfLoaded(x, y - 1, z, BlockType.BEDROCK))
                    && getBlockIfLoaded(x, y + 1, z, BlockType.AIR) == type
                    && BlockShape.isDoorUpper(getBlockMetadataIfLoaded(x, y + 1, z, 0));
        }
        if (type.isBed()) {
            if (!BlockShape.canSupportBed(getBlockIfLoaded(x, y - 1, z, BlockType.BEDROCK))) {
                return false;
            }
            return hasMatchingBedHalf(x, y, z, metadata, true);
        }
        if (type == BlockType.PORTAL) {
            return findActiveNetherPortalInteriorContaining(x, y, z, true) != null;
        }
        if (isSmallMushroom(type)) {
            return BlockShape.canPlaceAt(type, metadata, contextAtIfLoaded(x, y, z))
                    && canSmallMushroomStayAt(x, y, z, true);
        }
        return BlockShape.canPlaceAt(type, metadata, contextAtIfLoaded(x, y, z));
    }

    private boolean canSmallMushroomStayAt(int x, int y, int z, boolean loadedOnly) {
        BlockType support = loadedOnly
                ? getBlockIfLoaded(x, y - 1, z, BlockType.AIR)
                : getBlock(x, y - 1, z);
        if (support == BlockType.MYCELIUM) {
            return true;
        }
        if (!BlockShape.isOpaqueCube(support) || canSeeSky(x, y, z)) {
            return false;
        }
        int blockLight = loadedOnly
                ? getBlockLightIfLoaded(x, y, z, 15)
                : getBlockLight(x, y, z);
        return blockLight < 13;
    }

    private static boolean isSmallMushroom(BlockType type) {
        return type == BlockType.BROWN_MUSHROOM || type == BlockType.RED_MUSHROOM;
    }

    private boolean hasMatchingBedHalf(int x, int y, int z, int metadata, boolean loadedOnly) {
        int facing = metadata & 3;
        int[] dir = horizontalDirection(facing);
        boolean head = BlockShape.isBedHead(metadata);
        int otherX = head ? x - dir[0] : x + dir[0];
        int otherZ = head ? z - dir[1] : z + dir[1];
        if (loadedOnly && !isChunkGeneratedForBlock(otherX, otherZ)) {
            return true;
        }
        BlockType otherType = loadedOnly
                ? getBlockIfLoaded(otherX, y, otherZ, BlockType.BEDROCK)
                : getBlock(otherX, y, otherZ);
        if (otherType != BlockType.BED) {
            return false;
        }
        int otherMetadata = loadedOnly
                ? getBlockMetadataIfLoaded(otherX, y, otherZ, 0)
                : getBlockMetadata(otherX, y, otherZ);
        return BlockShape.isBedHead(otherMetadata) != head
                && (otherMetadata & 3) == facing;
    }

    private void updateNeighborSupport(int x, int y, int z) {
        int[][] dirs = {
                { 1, 0, 0 }, { -1, 0, 0 },
                { 0, 1, 0 }, { 0, -1, 0 },
                { 0, 0, 1 }, { 0, 0, -1 }
        };
        for (int[] dir : dirs) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            int nz = z + dir[2];
            BlockType neighbor = getBlockIfLoaded(nx, ny, nz, BlockType.AIR);
            if (neighbor != BlockType.AIR && neighbor != BlockType.BEDROCK && !isBlockSupportedIfLoaded(nx, ny, nz)) {
                breakBlock(nx, ny, nz, neighbor != BlockType.SNOW_LAYER);
            }
        }
    }

    private boolean breakDoor(int x, int y, int z, BlockType type, int metadata, boolean dropBlock) {
        boolean breakingUpper = BlockShape.isDoorUpper(metadata);
        int lowerY = breakingUpper ? y - 1 : y;
        int upperY = lowerY + 1;
        int lowerMetadata = getBlockMetadataIfLoaded(x, lowerY, z, 0);
        int upperMetadata = getBlockMetadataIfLoaded(x, upperY, z, 0);
        boolean hasValidLower = getBlockIfLoaded(x, lowerY, z, BlockType.AIR) == type
                && !BlockShape.isDoorUpper(lowerMetadata);
        boolean hasValidUpper = getBlockIfLoaded(x, upperY, z, BlockType.AIR) == type
                && BlockShape.isDoorUpper(upperMetadata);
        if (dropBlock && (!breakingUpper || hasValidLower)) {
            ItemType item = type == BlockType.WOODEN_DOOR ? ItemType.WOODEN_DOOR : ItemType.IRON_DOOR;
            spawnDroppedItem(x + 0.5f, lowerY + 0.5f, z + 0.5f, item, 1);
        }
        List<DeferredBlockChange> changes = new ArrayList<>(2);
        suppressNeighborSupportUpdates = true;
        try {
            if (!breakingUpper || hasValidLower) {
                deferBlockChange(changes, x, lowerY, z, BlockType.AIR, 0);
            }
            if (breakingUpper || hasValidUpper) {
                deferBlockChange(changes, x, upperY, z, BlockType.AIR, 0);
            }
        } finally {
            suppressNeighborSupportUpdates = false;
        }
        flushDeferredBlockEffects(changes);
        return true;
    }

    private boolean breakBed(int x, int y, int z, int metadata, boolean dropBlock) {
        boolean breakingHead = BlockShape.isBedHead(metadata);
        int facing = metadata & 3;
        int[] dir = horizontalDirection(facing);
        int footX = breakingHead ? x - dir[0] : x;
        int footZ = breakingHead ? z - dir[1] : z;
        int headX = footX + dir[0];
        int headZ = footZ + dir[1];
        int footMetadata = getBlockMetadataIfLoaded(footX, y, footZ, 0);
        int headMetadata = getBlockMetadataIfLoaded(headX, y, headZ, 0);
        boolean hasValidFoot = getBlockIfLoaded(footX, y, footZ, BlockType.AIR) == BlockType.BED
                && !BlockShape.isBedHead(footMetadata)
                && (footMetadata & 3) == facing;
        boolean hasValidHead = getBlockIfLoaded(headX, y, headZ, BlockType.AIR) == BlockType.BED
                && BlockShape.isBedHead(headMetadata)
                && (headMetadata & 3) == facing;
        if (dropBlock && (!breakingHead || hasValidFoot)) {
            spawnDroppedItem(footX + 0.5f, y + 0.5f, footZ + 0.5f, ItemType.BED, 1);
        }
        List<DeferredBlockChange> changes = new ArrayList<>(2);
        suppressNeighborSupportUpdates = true;
        try {
            if (!breakingHead || hasValidFoot) {
                deferBlockChange(changes, footX, y, footZ, BlockType.AIR, 0);
            }
            if (breakingHead || hasValidHead) {
                deferBlockChange(changes, headX, y, headZ, BlockType.AIR, 0);
            }
        } finally {
            suppressNeighborSupportUpdates = false;
        }
        flushDeferredBlockEffects(changes);
        return true;
    }

    public static int[] horizontalDirection(int facing) {
        return switch (facing & 3) {
            case 0 -> new int[] { 0, -1 };
            case 1 -> new int[] { 1, 0 };
            case 2 -> new int[] { 0, 1 };
            default -> new int[] { -1, 0 };
        };
    }

    public static int horizontalIndexFromFace(int face) {
        return switch (face) {
            case Block.FACE_NORTH -> 0;
            case Block.FACE_EAST -> 1;
            case Block.FACE_SOUTH -> 2;
            case Block.FACE_WEST -> 3;
            default -> 0;
        };
    }

    /**
     * Set block only in the given chunk's bounds (for decoration during
     * generation).
     * Used for trees to avoid placing leaves in chunks that aren't fully generated
     * yet.
     */
    private void setBlockInChunk(Chunk chunk, int worldX, int y, int worldZ, BlockType type) {
        if (y < 0 || y >= Chunk.HEIGHT)
            return;

        int chunkWorldX = chunk.getChunkX() * Chunk.WIDTH;
        int chunkWorldZ = chunk.getChunkZ() * Chunk.DEPTH;

        // Only place block if within this chunk's bounds
        int localX = worldX - chunkWorldX;
        int localZ = worldZ - chunkWorldZ;

        if (localX >= 0 && localX < Chunk.WIDTH && localZ >= 0 && localZ < Chunk.DEPTH) {
            chunk.setBlock(localX, y, localZ, type);
        }
        // Ignore blocks outside this chunk - they'll be generated when that chunk loads
    }

    public void cleanup() {
        // Shutdown async mesh building
        meshBuildPool.shutdownNow();
        chunkSavePool.shutdown();
        chunksBeingBuilt.clear();
        completedMeshTasks.clear();
        movingPistons.clear();
        RedstoneEngine.clearRuntimeState(this);
        particles.clear();

        for (Chunk chunk : chunks.values()) {
            chunk.cleanup();
        }
        chunks.clear();

        if (atlas != null) {
            atlas.cleanup();
        }
    }

    public long getSeed() {
        return seed;
    }

    public String getGeneratorId() {
        return generatorId;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public boolean shouldGenerateStructures() {
        return generateStructures;
    }

    public StructureGenerator.StructureLocation locateStructure(StructureType type, int originX, int originZ) {
        ReleaseOneWorldGenerator generator = worldGenerator instanceof ReleaseOneWorldGenerator releaseOne
                ? releaseOne
                : new ReleaseOneWorldGenerator(seed, dimension, generateStructures);
        return new StructureGenerator().locate(seed, dimension, type, originX, originZ, generator);
    }

    boolean isInsideStructure(StructureType type, int x, int y, int z) {
        ReleaseOneWorldGenerator generator = worldGenerator instanceof ReleaseOneWorldGenerator releaseOne
                ? releaseOne
                : new ReleaseOneWorldGenerator(seed, dimension, generateStructures);
        return new StructureGenerator().contains(seed, dimension, type, x, y, z, generator);
    }

    public com.craftzero.world.BiomeType getReleaseBiome(int x, int z) {
        return worldGenerator != null
                ? worldGenerator.getBiome(x, z)
                : com.craftzero.world.BiomeType.PLAINS;
    }

    // ===== DROPPED ITEMS =====

    /**
     * Spawn a dropped item at the given position.
     */
    public void spawnDroppedItem(float x, float y, float z, ItemType type, int count) {
        if (type == null || count <= 0) {
            return;
        }
        addDroppedItem(new DroppedItem(x, y, z, type, count, random));
    }

    /**
     * Spawn a thrown item with initial velocity (for Q drop and inventory throw).
     */
    public void spawnThrownItem(float x, float y, float z, ItemType type, int count,
            float velX, float velY, float velZ) {
        spawnThrownItem(x, y, z, type, count, velX, velY, velZ, DroppedItem.DEFAULT_PICKUP_DELAY_TICKS);
    }

    public void spawnThrownItem(float x, float y, float z, ItemType type, int count,
            float velX, float velY, float velZ, int pickupDelayTicks) {
        if (type == null || count <= 0) {
            return;
        }
        DroppedItem item = new DroppedItem(x, y, z, type, count, velX, velY, velZ, random);
        item.setPickupDelayTicks(pickupDelayTicks);
        addDroppedItem(item);
    }

    public void spawnThrownStack(float x, float y, float z, ItemStack stack,
            float velX, float velY, float velZ) {
        spawnThrownStack(x, y, z, stack, velX, velY, velZ, DroppedItem.DEFAULT_PICKUP_DELAY_TICKS);
    }

    public void spawnThrownStack(float x, float y, float z, ItemStack stack,
            float velX, float velY, float velZ, int pickupDelayTicks) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        DroppedItem item = new DroppedItem(x, y, z, stack, velX, velY, velZ, random);
        item.setPickupDelayTicks(pickupDelayTicks);
        addDroppedItem(item);
    }

    private void addDroppedItem(DroppedItem item) {
        while (item != null && item.getCount() > 0) {
            item.attachToWorld(this);
            int maxStack = Math.max(1, item.getMaxStackSize());
            DroppedItem stack = item;
            if (item.getCount() > maxStack) {
                stack = item.splitOff(maxStack);
            } else {
                item = null;
            }
            mergeOrAddDroppedItem(stack);
        }
    }

    private void mergeOrAddDroppedItem(DroppedItem item) {
        if (item == null || item.getCount() <= 0) {
            return;
        }
        for (DroppedItem existing : droppedItems) {
            float dx = existing.getX() - item.getX();
            float dy = existing.getY() - item.getY();
            float dz = existing.getZ() - item.getZ();
            if (dx * dx + dy * dy + dz * dz > DROPPED_ITEM_MERGE_RADIUS_SQ) {
                continue;
            }
            if (existing.canMergeWith(item)) {
                existing.mergeWith(item);
                if (item.getCount() <= 0) {
                    return;
                }
            }
        }
        droppedItems.add(item);
    }

    private boolean mergeDroppedItemDuringUpdate(DroppedItem item) {
        if (item == null || item.getCount() <= 0 || !item.shouldRunItemCellWork()) {
            return false;
        }
        for (DroppedItem existing : droppedItems) {
            if (existing == item) {
                continue;
            }
            float dx = existing.getX() - item.getX();
            float dy = existing.getY() - item.getY();
            float dz = existing.getZ() - item.getZ();
            if (dx * dx + dy * dy + dz * dz > DROPPED_ITEM_MERGE_RADIUS_SQ) {
                continue;
            }
            if (existing.canMergeWith(item)) {
                existing.mergeWith(item);
                if (item.getCount() <= 0) {
                    return true;
                }
            }
        }
        return item.getCount() <= 0;
    }

    /**
     * Update all dropped items (physics, animation, despawn).
     */
    public void updateDroppedItems(float deltaTime) {
        Iterator<DroppedItem> iterator = droppedItems.iterator();
        while (iterator.hasNext()) {
            DroppedItem item = iterator.next();
            if (isCactusContact(item)) {
                iterator.remove();
                continue;
            }
            if (item.update(deltaTime, this)) {
                iterator.remove(); // Despawned
                continue;
            }
            if (applyDroppedItemHazardContact(item)) {
                iterator.remove();
                continue;
            }
            if (mergeDroppedItemDuringUpdate(item)) {
                iterator.remove();
                continue;
            }
            schedulePressurePlateUpdatesAt(item.getX(), item.getY(), item.getZ(), false);
            activateRedstoneOre((int) Math.floor(item.getX()), (int) Math.floor(item.getY() - 0.2f),
                    (int) Math.floor(item.getZ()));
        }
    }

    public void spawnParticle(WorldParticle.Type type, float x, float y, float z,
            float motionX, float motionY, float motionZ, float scale, int lifetimeTicks) {
        if (particles.size() >= MAX_PARTICLES || !allFinite(x, y, z, motionX, motionY, motionZ, scale)
                || lifetimeTicks <= 0) {
            return;
        }
        if (type == WorldParticle.Type.PORTAL) {
            addParticle(new WorldParticle(type, x, y, z, motionX, motionY, motionZ, scale,
                    portalParticleLifetime(), portalParticleFrame()));
            return;
        }
        if (type == WorldParticle.Type.MOB_SPELL) {
            addParticle(new WorldParticle(type, x, y, z, motionX, motionY, motionZ,
                    mobSpellParticleScale(scale), auraParticleLifetime(displayRandom)));
            return;
        }
        addParticle(new WorldParticle(type, x, y, z, motionX, motionY, motionZ, scale, lifetimeTicks));
    }

    public void spawnParticle(WorldParticle.Type type, float x, float y, float z,
            float motionX, float motionY, float motionZ, float scale, int lifetimeTicks, float data) {
        if (particles.size() >= MAX_PARTICLES || !allFinite(x, y, z, motionX, motionY, motionZ, scale, data)
                || lifetimeTicks <= 0) {
            return;
        }
        if (type == WorldParticle.Type.MOB_SPELL) {
            addParticle(new WorldParticle(type, x, y, z, motionX, motionY, motionZ,
                    mobSpellParticleScale(scale), auraParticleLifetime(displayRandom), data));
            return;
        }
        addParticle(new WorldParticle(type, x, y, z, motionX, motionY, motionZ, scale, lifetimeTicks, data));
    }

    public void spawnNetworkParticle(WorldParticle.Type type, float x, float y, float z,
            float motionX, float motionY, float motionZ, float scale, int lifetimeTicks, float data,
            boolean hasTarget, float targetX, float targetY, float targetZ) {
        if (!allFinite(x, y, z, motionX, motionY, motionZ, scale, data)
                || (hasTarget && !allFinite(targetX, targetY, targetZ))
                || lifetimeTicks <= 0) {
            return;
        }
        addParticle(WorldParticle.fromNetwork(type, x, y, z, motionX, motionY, motionZ, scale,
                lifetimeTicks, data, hasTarget, targetX, targetY, targetZ), false);
    }

    private void addParticle(WorldParticle particle) {
        addParticle(particle, true);
    }

    private void addParticle(WorldParticle particle, boolean recordEvent) {
        if (particle == null || !particle.isValid() || particles.size() >= MAX_PARTICLES) {
            return;
        }
        particles.add(particle);
        if (recordEvent) {
            particleEvents.add(particle);
        }
    }

    private float mobSpellParticleScale(float scale) {
        return Math.max(0.01f, scale) * (displayRandom.nextFloat() * 0.6f + 0.5f);
    }

    private int portalParticleLifetime() {
        return 40 + random.nextInt(10);
    }

    private float portalParticleFrame() {
        return random.nextInt(8);
    }

    public void spawnDispenserSmokeParticles(int x, int y, int z, int metadata) {
        int face = RedstoneEngine.metadataToOutputFace(metadata);
        int dx = RedstoneEngine.faceToDx(face);
        int dz = RedstoneEngine.faceToDz(face);
        float baseX = x + dx * 0.6f + 0.5f;
        float baseY = y + 0.5f;
        float baseZ = z + dz * 0.6f + 0.5f;
        for (int i = 0; i < 10; i++) {
            float speed = displayRandom.nextFloat() * 0.2f + 0.01f;
            float particleX = baseX + dx * 0.01f + (displayRandom.nextFloat() - 0.5f) * dz * 0.5f;
            float particleY = baseY + (displayRandom.nextFloat() - 0.5f) * 0.5f;
            float particleZ = baseZ + dz * 0.01f + (displayRandom.nextFloat() - 0.5f) * dx * 0.5f;
            float motionX = dx * speed + (float) displayRandom.nextGaussian() * 0.01f;
            float motionY = -0.03f + (float) displayRandom.nextGaussian() * 0.01f;
            float motionZ = dz * speed + (float) displayRandom.nextGaussian() * 0.01f;
            spawnParticle(WorldParticle.Type.SMOKE,
                    particleX, particleY, particleZ,
                    motionX, motionY, motionZ,
                    0.22f, 16);
        }
    }

    public void spawnParticleBurst(WorldParticle.Type type, float x, float y, float z, int count) {
        int amount = Math.min(Math.max(0, count), Math.max(0, MAX_PARTICLES - particles.size()));
        for (int i = 0; i < amount; i++) {
            float spreadX = (random.nextFloat() - 0.5f) * 0.55f;
            float spreadY = random.nextFloat() * 0.35f;
            float spreadZ = (random.nextFloat() - 0.5f) * 0.55f;
            float motionX = (random.nextFloat() - 0.5f) * 0.25f;
            float motionY = 0.20f + random.nextFloat() * 0.28f;
            float motionZ = (random.nextFloat() - 0.5f) * 0.25f;
            spawnBurstParticle(type, x + spreadX, y + spreadY, z + spreadZ,
                    motionX, motionY, motionZ);
        }
    }

    public void spawnEntityParticleBurst(WorldParticle.Type type, float entityX, float entityY, float entityZ,
            float entityWidth, float entityHeight, int count) {
        if (type == null) {
            return;
        }
        float width = Math.max(ENTITY_PARTICLE_BURST_MIN_WIDTH, entityWidth);
        float height = Math.max(ENTITY_PARTICLE_BURST_MIN_HEIGHT, entityHeight);
        int amount = Math.min(Math.max(0, count), Math.max(0, MAX_PARTICLES - particles.size()));
        for (int i = 0; i < amount; i++) {
            float offsetX = (random.nextFloat() - 0.5f) * width;
            float offsetY = random.nextFloat() * height;
            float offsetZ = (random.nextFloat() - 0.5f) * width;
            float motionX = offsetX * ENTITY_PARTICLE_BURST_MOTION_SCALE
                    + (random.nextFloat() - 0.5f) * 0.10f;
            float motionY = ENTITY_PARTICLE_BURST_VERTICAL_BONUS + random.nextFloat() * 0.16f
                    + offsetY / height * 0.08f;
            float motionZ = offsetZ * ENTITY_PARTICLE_BURST_MOTION_SCALE
                    + (random.nextFloat() - 0.5f) * 0.10f;
            spawnBurstParticle(type,
                    entityX + offsetX,
                    entityY + offsetY,
                    entityZ + offsetZ,
                    motionX, motionY, motionZ);
        }
    }

    private void spawnBurstParticle(WorldParticle.Type type, float x, float y, float z,
            float motionX, float motionY, float motionZ) {
        float scale = switch (type) {
            case HEART -> 0.28f;
            case FLAME -> 0.20f + random.nextFloat() * 0.06f;
            case SPLASH -> 0.16f + random.nextFloat() * 0.08f;
            case CRIT, MAGIC_CRIT -> 0.14f + random.nextFloat() * 0.06f;
            default -> 0.22f + random.nextFloat() * 0.08f;
        };
        int lifetime = switch (type) {
            case HEART -> 20;
            case FLAME -> 10 + random.nextInt(5);
            case SPLASH -> 8 + random.nextInt(5);
            case CRIT, MAGIC_CRIT -> critParticleLifetime(random);
            default -> 16 + random.nextInt(8);
        };
        if (type == WorldParticle.Type.CRIT) {
            spawnParticle(type, x, y, z, motionX, motionY, motionZ,
                    scale, lifetime, critParticleBaseGray(random));
        } else {
            spawnParticle(type, x, y, z, motionX, motionY, motionZ, scale, lifetime);
        }
    }

    public void spawnEntityCritEmitter(WorldParticle.Type type, LivingEntity target) {
        if (!isEntityCritParticleType(type) || target == null || target.isRemoved()) {
            return;
        }
        spawnEntityCritBurst(type, target);
        if (ENTITY_CRIT_EMITTER_TICKS > 1) {
            critParticleEmitters.add(new CritParticleEmitter(type, target, ENTITY_CRIT_EMITTER_TICKS - 1));
        }
    }

    private void spawnEntityCritBurst(WorldParticle.Type type, LivingEntity target) {
        if (particles.size() >= MAX_PARTICLES || target == null || target.isRemoved()) {
            return;
        }
        float width = Math.max(0.1f, target.getWidth());
        float height = Math.max(0.1f, target.getHeight());
        int amount = Math.min(ENTITY_CRIT_PARTICLES_PER_TICK, Math.max(0, MAX_PARTICLES - particles.size()));
        for (int spawned = 0, attempts = 0;
                spawned < amount && attempts < ENTITY_CRIT_PARTICLE_ATTEMPT_LIMIT && particles.size() < MAX_PARTICLES;
                attempts++) {
            float offsetX = random.nextFloat() * 2.0f - 1.0f;
            float offsetY = random.nextFloat() * 2.0f - 1.0f;
            float offsetZ = random.nextFloat() * 2.0f - 1.0f;
            if (offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ > 1.0f) {
                continue;
            }
            float particleX = target.getX() + offsetX * width * ENTITY_CRIT_POSITION_WIDTH_SCALE;
            float particleY = target.getY() + height * 0.5f
                    + offsetY * height * ENTITY_CRIT_POSITION_HEIGHT_SCALE;
            float particleZ = target.getZ() + offsetZ * width * ENTITY_CRIT_POSITION_WIDTH_SCALE;
            float scale = ENTITY_CRIT_PARTICLE_BASE_SCALE + random.nextFloat() * ENTITY_CRIT_PARTICLE_RANDOM_SCALE;
            int lifetime = critParticleLifetime(random);
            if (type == WorldParticle.Type.CRIT) {
                spawnParticle(type,
                        particleX, particleY, particleZ,
                        offsetX, offsetY + ENTITY_CRIT_VERTICAL_MOTION_BONUS, offsetZ,
                        scale, lifetime,
                        critParticleBaseGray(random));
            } else {
                spawnParticle(type,
                        particleX, particleY, particleZ,
                        offsetX, offsetY + ENTITY_CRIT_VERTICAL_MOTION_BONUS, offsetZ,
                        scale, lifetime);
            }
            spawned++;
        }
    }

    private static boolean isEntityCritParticleType(WorldParticle.Type type) {
        return type == WorldParticle.Type.CRIT || type == WorldParticle.Type.MAGIC_CRIT;
    }

    private static int critParticleLifetime(Random source) {
        return Math.max(1, (int) (6.0f / (source.nextFloat() * 0.8f + 0.6f)));
    }

    private static float critParticleBaseGray(Random source) {
        return source.nextFloat() * 0.3f + 0.6f;
    }

    public void spawnItemBreakParticles(ItemType type, float x, float y, float z) {
        spawnItemBreakParticles(type, x, y, z, 8);
    }

    public void spawnItemBreakParticles(ItemType type, float x, float y, float z, int count) {
        if (type == null) {
            return;
        }
        int amount = Math.min(Math.max(0, count), Math.max(0, MAX_PARTICLES - particles.size()));
        for (int i = 0; i < amount; i++) {
            float spreadX = (random.nextFloat() - 0.5f) * 0.28f;
            float spreadY = (random.nextFloat() - 0.5f) * 0.28f;
            float spreadZ = (random.nextFloat() - 0.5f) * 0.28f;
            float motionX = (random.nextFloat() - 0.5f) * 0.18f;
            float motionY = 0.10f + random.nextFloat() * 0.16f;
            float motionZ = (random.nextFloat() - 0.5f) * 0.18f;
            spawnParticle(WorldParticle.Type.ITEM_CRACK,
                    x + spreadX, y + spreadY, z + spreadZ,
                    motionX, motionY, motionZ,
                    0.10f + random.nextFloat() * 0.035f,
                    12 + random.nextInt(8),
                    WorldParticle.itemParticleData(type));
        }
    }

    public void spawnSnowballPoofParticles(float x, float y, float z) {
        int amount = Math.min(SNOWBALL_POOF_PARTICLES, Math.max(0, MAX_PARTICLES - particles.size()));
        for (int i = 0; i < amount; i++) {
            spawnParticle(WorldParticle.Type.SNOWBALL_POOF,
                    x + (random.nextFloat() - 0.5f) * 0.25f,
                    y + (random.nextFloat() - 0.5f) * 0.25f,
                    z + (random.nextFloat() - 0.5f) * 0.25f,
                    (random.nextFloat() - 0.5f) * 0.04f,
                    (random.nextFloat() - 0.5f) * 0.04f,
                    (random.nextFloat() - 0.5f) * 0.04f,
                    0.12f + random.nextFloat() * 0.025f,
                    10 + random.nextInt(6));
        }
    }

    public void spawnSplashPotionParticles(float x, float y, float z, PotionData potionData) {
        spawnSplashPotionItemCrackParticles(x, y, z);
        spawnSplashPotionSpellParticles(x, y, z, potionData);
    }

    private void spawnSplashPotionItemCrackParticles(float x, float y, float z) {
        int amount = Math.min(8, Math.max(0, MAX_PARTICLES - particles.size()));
        for (int i = 0; i < amount; i++) {
            spawnParticle(WorldParticle.Type.ITEM_CRACK,
                    x, y, z,
                    (float) random.nextGaussian() * 0.15f,
                    random.nextFloat() * 0.20f,
                    (float) random.nextGaussian() * 0.15f,
                    0.10f,
                    16,
                    WorldParticle.itemParticleData(ItemType.POTION));
        }
    }

    private void spawnSplashPotionSpellParticles(float x, float y, float z, PotionData potionData) {
        int amount = Math.min(SPLASH_POTION_SPELL_PARTICLES, Math.max(0, MAX_PARTICLES - particles.size()));
        if (amount <= 0) {
            return;
        }
        WorldParticle.Type type = PotionEffectResolver.isInstant(potionData)
                ? WorldParticle.Type.INSTANT_SPELL
                : WorldParticle.Type.SPELL;
        int color = StatusEffectVisuals.potionColor(potionData);
        for (int i = 0; i < amount; i++) {
            float radialDistance = random.nextFloat() * 4.0f;
            float angle = random.nextFloat() * (float) (Math.PI * 2.0);
            float radialX = (float) Math.cos(angle) * radialDistance;
            float radialZ = (float) Math.sin(angle) * radialDistance;
            float motionX = radialX * 0.4f;
            float motionY = (0.01f + random.nextFloat() * 0.5f) * 0.2f;
            float motionZ = radialZ * 0.4f;
            float brightness = 0.75f + random.nextFloat() * 0.25f;
            spawnParticle(type,
                    x + radialX * 0.10f,
                    y + 0.30f,
                    z + radialZ * 0.10f,
                    motionX, motionY, motionZ,
                    0.13f + random.nextFloat() * 0.04f,
                    18 + random.nextInt(10),
                    scaledParticleColor(color, brightness));
        }
    }

    private static int scaledParticleColor(int color, float brightness) {
        float scale = Math.max(0.0f, Math.min(1.0f, brightness));
        int red = Math.max(0, Math.min(255, Math.round(((color >> 16) & 0xff) * scale)));
        int green = Math.max(0, Math.min(255, Math.round(((color >> 8) & 0xff) * scale)));
        int blue = Math.max(0, Math.min(255, Math.round((color & 0xff) * scale)));
        return (red << 16) | (green << 8) | blue;
    }

    public void spawnItemPickupParticle(ItemType type, float itemX, float itemY, float itemZ,
            float targetX, float targetY, float targetZ) {
        if (type == null) {
            return;
        }
        if (particles.size() >= MAX_PARTICLES) {
            return;
        }
        addParticle(new WorldParticle(WorldParticle.Type.ITEM_PICKUP,
                itemX, itemY, itemZ,
                0.0f, 0.0f, 0.0f,
                0.22f, 3,
                WorldParticle.itemParticleData(type),
                targetX, targetY, targetZ));
    }

    public void spawnSlimeLandingParticles(float entityX, float entityY, float entityZ,
            float entityWidth, int slimeSize) {
        int size = Math.max(1, slimeSize);
        int count = Math.min(size * 8, Math.max(0, MAX_PARTICLES - particles.size()));
        float width = Math.max(0.2f, entityWidth);
        for (int i = 0; i < count; i++) {
            float px = entityX + (random.nextFloat() - 0.5f) * width;
            float py = entityY + 0.05f + random.nextFloat() * 0.1f;
            float pz = entityZ + (random.nextFloat() - 0.5f) * width;
            spawnParticle(WorldParticle.Type.SLIME,
                    px, py, pz,
                    (random.nextFloat() - 0.5f) * 0.16f,
                    0.04f + random.nextFloat() * 0.10f,
                    (random.nextFloat() - 0.5f) * 0.16f,
                    0.08f + random.nextFloat() * 0.025f,
                    10 + random.nextInt(6));
        }
    }

    public void spawnEnchantmentTableParticle(int tableX, int tableY, int tableZ,
            int shelfX, int shelfY, int shelfZ) {
        if (particles.size() >= MAX_PARTICLES) {
            return;
        }
        float startX = tableX + 0.5f;
        float startY = tableY + 2.0f;
        float startZ = tableZ + 0.5f;
        float targetX = startX + (shelfX - tableX) + random.nextFloat() - 0.5f;
        float targetY = startY + (shelfY - tableY) - random.nextFloat() - 1.0f;
        float targetZ = startZ + (shelfZ - tableZ) + random.nextFloat() - 0.5f;
        int lifetime = 30 + random.nextInt(10);
        addParticle(new WorldParticle(WorldParticle.Type.ENCHANTMENT_TABLE,
                startX, startY, startZ,
                0.0f, 0.0f, 0.0f,
                0.20f + random.nextFloat() * 0.50f,
                lifetime,
                1.0f + random.nextInt(26),
                targetX, targetY, targetZ));
    }

    public void spawnMobDeathParticles(float entityX, float entityY, float entityZ,
            float entityWidth, float entityHeight) {
        float width = Math.max(0.1f, entityWidth);
        float height = Math.max(0.1f, entityHeight);
        int count = Math.max(8, Math.min(24, 8 + Math.round(width * height * 8.0f)));
        int amount = Math.min(count, Math.max(0, MAX_PARTICLES - particles.size()));
        for (int i = 0; i < amount; i++) {
            float offsetX = (random.nextFloat() - 0.5f) * width;
            float offsetY = random.nextFloat() * height;
            float offsetZ = (random.nextFloat() - 0.5f) * width;
            spawnParticle(WorldParticle.Type.EXPLODE,
                    entityX + offsetX,
                    entityY + offsetY,
                    entityZ + offsetZ,
                    offsetX * 0.12f + (random.nextFloat() - 0.5f) * 0.04f,
                    0.05f + random.nextFloat() * 0.08f + offsetY / height * 0.04f,
                    offsetZ * 0.12f + (random.nextFloat() - 0.5f) * 0.04f,
                    0.20f + random.nextFloat() * 0.08f,
                    14 + random.nextInt(8));
        }
    }

    public void spawnBlockDestroyParticles(int x, int y, int z, BlockType type, int metadata) {
        if (!canSpawnBlockParticle(type)) {
            return;
        }
        int subdivisions = 4;
        for (int px = 0; px < subdivisions; px++) {
            for (int py = 0; py < subdivisions; py++) {
                for (int pz = 0; pz < subdivisions; pz++) {
                    float localX = (px + 0.5f) / subdivisions;
                    float localY = (py + 0.5f) / subdivisions;
                    float localZ = (pz + 0.5f) / subdivisions;
                    float[] motion = sourceDiggingMotion(localX - 0.5f, localY - 0.5f,
                            localZ - 0.5f, 1.0f);
                    spawnParticle(WorldParticle.Type.BLOCK_CRACK,
                            x + localX, y + localY, z + localZ,
                            motion[0], motion[1], motion[2],
                            sourceDiggingParticleScale(1.0f), sourceParticleLifetimeTicks(),
                            WorldParticle.blockParticleData(type, metadata, SOURCE_DIGGING_TEXTURE_FACE));
                }
            }
        }
    }

    public void spawnBlockHitParticle(int x, int y, int z, int face, BlockType type, int metadata) {
        if (!canSpawnBlockParticle(type)) {
            return;
        }
        BlockShape.Cuboid bounds = particleRenderBounds(x, y, z, type, metadata);
        float inset = 0.1f;
        float px = x + bounds.minX() + inset
                + random.nextFloat() * (bounds.maxX() - bounds.minX() - inset * 2.0f);
        float py = y + bounds.minY() + inset
                + random.nextFloat() * (bounds.maxY() - bounds.minY() - inset * 2.0f);
        float pz = z + bounds.minZ() + inset
                + random.nextFloat() * (bounds.maxZ() - bounds.minZ() - inset * 2.0f);
        switch (face) {
            case Block.FACE_BOTTOM -> py = y + bounds.minY() - inset;
            case Block.FACE_TOP -> py = y + bounds.maxY() + inset;
            case Block.FACE_NORTH -> pz = z + bounds.minZ() - inset;
            case Block.FACE_SOUTH -> pz = z + bounds.maxZ() + inset;
            case Block.FACE_EAST -> px = x + bounds.maxX() + inset;
            case Block.FACE_WEST -> px = x + bounds.minX() - inset;
            default -> {
            }
        }
        float[] motion = sourceDiggingMotion(0.0f, 0.0f, 0.0f, 0.2f);
        spawnParticle(WorldParticle.Type.BLOCK_CRACK,
                px, py, pz,
                motion[0], motion[1], motion[2],
                sourceDiggingParticleScale(0.6f), sourceParticleLifetimeTicks(),
                WorldParticle.blockParticleData(type, metadata, SOURCE_DIGGING_TEXTURE_FACE));
    }

    private BlockShape.Cuboid particleRenderBounds(int x, int y, int z, BlockType type, int metadata) {
        List<BlockShape.Cuboid> boxes = BlockShape.getRenderBoxes(type, metadata, contextAtIfLoaded(x, y, z));
        if (boxes.isEmpty()) {
            return BlockShape.FULL;
        }
        float minX = 1.0f;
        float minY = 1.0f;
        float minZ = 1.0f;
        float maxX = 0.0f;
        float maxY = 0.0f;
        float maxZ = 0.0f;
        for (BlockShape.Cuboid box : boxes) {
            minX = Math.min(minX, box.minX());
            minY = Math.min(minY, box.minY());
            minZ = Math.min(minZ, box.minZ());
            maxX = Math.max(maxX, box.maxX());
            maxY = Math.max(maxY, box.maxY());
            maxZ = Math.max(maxZ, box.maxZ());
        }
        return new BlockShape.Cuboid(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private float[] sourceDiggingMotion(float inputX, float inputY, float inputZ, float velocityMultiplier) {
        float motionX = inputX + (random.nextFloat() * 2.0f - 1.0f) * 0.4f;
        float motionY = inputY + (random.nextFloat() * 2.0f - 1.0f) * 0.4f;
        float motionZ = inputZ + (random.nextFloat() * 2.0f - 1.0f) * 0.4f;
        float speed = (random.nextFloat() + random.nextFloat() + 1.0f) * 0.15f;
        float length = (float) Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        if (length <= 0.000001f) {
            motionY = 1.0f;
            length = 1.0f;
        }
        motionX = motionX / length * speed * 0.4f;
        motionY = motionY / length * speed * 0.4f + 0.1f;
        motionZ = motionZ / length * speed * 0.4f;
        if (velocityMultiplier != 1.0f) {
            motionX *= velocityMultiplier;
            motionY = (motionY - 0.1f) * velocityMultiplier + 0.1f;
            motionZ *= velocityMultiplier;
        }
        return new float[] { motionX, motionY, motionZ };
    }

    private float sourceDiggingParticleScale(float scaleMultiplier) {
        float oldParticleScale = random.nextFloat() * 0.5f + 0.5f;
        return 0.2f * oldParticleScale * scaleMultiplier;
    }

    private int sourceParticleLifetimeTicks() {
        return Math.max(1, (int) (4.0f / (random.nextFloat() * 0.9f + 0.1f)));
    }

    public void spawnSprintBlockParticle(float entityX, float feetY, float entityZ, float entityWidth,
            float motionX, float motionZ, BlockType type, int metadata) {
        if (!canSpawnBlockParticle(type)) {
            return;
        }
        float speedSq = motionX * motionX + motionZ * motionZ;
        if (speedSq < SPRINT_BLOCK_PARTICLE_MIN_SPEED_SQ) {
            return;
        }
        float width = Math.max(0.1f, entityWidth);
        float speed = (float) Math.sqrt(speedSq);
        spawnParticle(WorldParticle.Type.BLOCK_CRACK,
                entityX + (random.nextFloat() - 0.5f) * width,
                feetY + 0.1f,
                entityZ + (random.nextFloat() - 0.5f) * width,
                -motionX * 3.0f,
                0.10f + Math.min(0.35f, speed * 0.5f),
                -motionZ * 3.0f,
                0.08f + Math.min(0.05f, speed * 0.03f),
                10 + random.nextInt(6),
                WorldParticle.blockParticleData(type, metadata, SOURCE_DIGGING_TEXTURE_FACE));
    }

    public void spawnEntityWaterEntryParticles(float entityX, float entityY, float entityZ, float entityWidth,
            float motionX, float motionY, float motionZ) {
        float width = Math.max(0.1f, entityWidth);
        float horizontalSpeed = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
        float impact = Math.min(3.0f, Math.abs(motionY) + horizontalSpeed * 0.5f);
        int count = Math.max(ENTITY_WATER_ENTRY_MIN_PARTICLES,
                Math.min(ENTITY_WATER_ENTRY_MAX_PARTICLES,
                        Math.round(1.0f + width * ENTITY_WATER_ENTRY_WIDTH_PARTICLES
                                + impact * ENTITY_WATER_ENTRY_IMPACT_PARTICLES)));
        float particleY = (float) Math.floor(entityY + 0.1f) + 1.0f;
        for (int i = 0; i < count; i++) {
            float offsetX = (random.nextFloat() * 2.0f - 1.0f) * width;
            float offsetZ = (random.nextFloat() * 2.0f - 1.0f) * width;
            spawnParticle(WorldParticle.Type.BUBBLE,
                    entityX + offsetX,
                    particleY,
                    entityZ + offsetZ,
                    motionX * 0.25f + offsetX * 0.05f,
                    motionY * 0.25f - random.nextFloat() * 0.2f,
                    motionZ * 0.25f + offsetZ * 0.05f,
                    0.055f,
                    8 + random.nextInt(8));
        }
        for (int i = 0; i < count; i++) {
            float offsetX = (random.nextFloat() * 2.0f - 1.0f) * width;
            float offsetZ = (random.nextFloat() * 2.0f - 1.0f) * width;
            spawnParticle(WorldParticle.Type.SPLASH,
                    entityX + offsetX,
                    particleY,
                    entityZ + offsetZ,
                    motionX * 0.35f + offsetX * 0.06f,
                    Math.max(0.02f, motionY * 0.20f + random.nextFloat() * (0.10f + impact * 0.05f)),
                    motionZ * 0.35f + offsetZ * 0.06f,
                    0.12f + Math.min(0.08f, impact * 0.03f),
                    8 + random.nextInt(6));
        }
    }

    public void spawnProjectileWaterBubbleTrail(float projectileX, float projectileY, float projectileZ,
            float motionX, float motionY, float motionZ) {
        for (int i = 0; i < PROJECTILE_WATER_TRAIL_BUBBLES; i++) {
            spawnParticle(WorldParticle.Type.BUBBLE,
                    projectileX - motionX * PROJECTILE_WATER_TRAIL_BACKSTEP,
                    projectileY - motionY * PROJECTILE_WATER_TRAIL_BACKSTEP,
                    projectileZ - motionZ * PROJECTILE_WATER_TRAIL_BACKSTEP,
                    motionX, motionY, motionZ,
                    PROJECTILE_WATER_TRAIL_BUBBLE_SCALE,
                    PROJECTILE_WATER_TRAIL_BUBBLE_LIFETIME_TICKS);
        }
    }

    private static boolean canSpawnBlockParticle(BlockType type) {
        return type != null && type != BlockType.AIR && !type.isFluid();
    }

    public void updateParticles(float deltaTime) {
        updateCritParticleEmitters(deltaTime);
        List<float[]> waterDripSplashes = null;
        List<float[]> lavaSmokePuffs = null;
        Iterator<WorldParticle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            WorldParticle particle = iterator.next();
            boolean lavaParticle = particle.getType() == WorldParticle.Type.LAVA;
            float lavaX = 0.0f;
            float lavaY = 0.0f;
            float lavaZ = 0.0f;
            float lavaMotionX = 0.0f;
            float lavaMotionY = 0.0f;
            float lavaMotionZ = 0.0f;
            if (lavaParticle) {
                lavaX = particle.getRenderX(1.0f);
                lavaY = particle.getRenderY(1.0f);
                lavaZ = particle.getRenderZ(1.0f);
                lavaMotionX = particle.getMotionX();
                lavaMotionY = particle.getMotionY();
                lavaMotionZ = particle.getMotionZ();
            }
            boolean expired = particle.update(this, deltaTime);
            if (lavaParticle && !expired
                    && displayRandom.nextFloat() > particle.getAgeTicks() / particle.getLifetimeTicks()) {
                if (lavaSmokePuffs == null) {
                    lavaSmokePuffs = new ArrayList<>();
                }
                lavaSmokePuffs.add(new float[] {
                        lavaX, lavaY, lavaZ,
                        lavaMotionX, lavaMotionY, lavaMotionZ
                });
            }
            if (particle.consumeWaterDripSplashPending()) {
                if (waterDripSplashes == null) {
                    waterDripSplashes = new ArrayList<>();
                }
                waterDripSplashes.add(new float[] {
                        particle.getRenderX(1.0f),
                        particle.getRenderY(1.0f),
                        particle.getRenderZ(1.0f)
                });
            }
            if (expired) {
                iterator.remove();
            }
        }
        if (waterDripSplashes != null) {
            for (float[] splash : waterDripSplashes) {
                spawnParticle(WorldParticle.Type.SPLASH,
                        splash[0], splash[1], splash[2],
                        0.0f, 0.0f, 0.0f,
                        0.12f, 8);
            }
        }
        if (lavaSmokePuffs != null) {
            for (float[] puff : lavaSmokePuffs) {
                spawnParticle(WorldParticle.Type.SMOKE,
                        puff[0], puff[1], puff[2],
                        puff[3], puff[4], puff[5],
                        0.22f, 18);
            }
        }
        Iterator<WorldLightningBolt> lightningIterator = lightningBolts.iterator();
        while (lightningIterator.hasNext()) {
            if (lightningIterator.next().update(deltaTime)) {
                lightningIterator.remove();
            }
        }
        tickLightningFlash(deltaTime);
    }

    private void updateCritParticleEmitters(float deltaTime) {
        if (critParticleEmitters.isEmpty()) {
            return;
        }
        critParticleEmitterAccumulator += Math.max(0.0f, deltaTime) * 20.0f;
        int ticks = (int) critParticleEmitterAccumulator;
        if (ticks <= 0) {
            return;
        }
        critParticleEmitterAccumulator -= ticks;
        for (int tick = 0; tick < ticks && !critParticleEmitters.isEmpty(); tick++) {
            tickCritParticleEmitters();
        }
    }

    private void tickCritParticleEmitters() {
        for (int i = critParticleEmitters.size() - 1; i >= 0; i--) {
            CritParticleEmitter emitter = critParticleEmitters.get(i);
            LivingEntity target = emitter.target();
            if (target == null || target.isRemoved()) {
                critParticleEmitters.remove(i);
                continue;
            }
            spawnEntityCritBurst(emitter.type(), target);
            int ticksRemaining = emitter.ticksRemaining() - 1;
            if (ticksRemaining <= 0) {
                critParticleEmitters.remove(i);
            } else {
                critParticleEmitters.set(i,
                        new CritParticleEmitter(emitter.type(), target, ticksRemaining));
            }
        }
    }

    public List<WorldParticle> getParticles() {
        return particles;
    }

    public List<WorldParticle> drainParticleEvents() {
        List<WorldParticle> drained = new ArrayList<>(particleEvents);
        particleEvents.clear();
        return drained;
    }

    public List<WorldLightningBolt> getLightningBolts() {
        return lightningBolts;
    }

    public List<WorldLightningBolt> drainLightningEvents() {
        List<WorldLightningBolt> drained = new ArrayList<>(lightningEvents);
        lightningEvents.clear();
        return drained;
    }

    public void playSound(String soundId, float x, float y, float z, float volume, float pitch) {
        WorldSoundEvent event = new WorldSoundEvent(soundId, x, y, z, volume, pitch);
        if (!event.isPlayable()) {
            return;
        }
        soundEvents.add(event);
    }

    public void stopRecordSound(float x, float y, float z) {
        WorldSoundEvent event = WorldSoundEvent.stopRecord(x, y, z);
        if (event.isPlayable()) {
            soundEvents.add(event);
        }
    }

    public void playBowSound(float x, float y, float z) {
        playSound(WorldSoundEvent.BOW, x, y, z, 1.0f, bowPitch());
    }

    private float bowPitch() {
        return 1.0f / (random.nextFloat() * 0.4f + 0.8f);
    }

    public void playThrowSound(float x, float y, float z) {
        playSound(WorldSoundEvent.BOW, x, y, z, 0.5f, throwPitch());
    }

    private float throwPitch() {
        return 0.4f / (random.nextFloat() * 0.4f + 0.8f);
    }

    public void playFireIgniteSound(float x, float y, float z) {
        playSound(WorldSoundEvent.FIRE_IGNITE, x, y, z, 1.0f, fireIgnitePitch());
    }

    private float fireIgnitePitch() {
        return random.nextFloat() * 0.4f + 0.8f;
    }

    public void playRedstoneTorchBurnoutFeedback(int x, int y, int z) {
        playSound(WorldSoundEvent.REDSTONE_TORCH_BURNOUT,
                x + 0.5f, y + 0.5f, z + 0.5f,
                0.5f, redstoneTorchBurnoutPitch());
        for (int i = 0; i < REDSTONE_TORCH_BURNOUT_SMOKE_PARTICLES; i++) {
            float px = x + random.nextFloat() * 0.6f + 0.2f;
            float py = y + random.nextFloat() * 0.6f + 0.2f;
            float pz = z + random.nextFloat() * 0.6f + 0.2f;
            spawnParticle(WorldParticle.Type.SMOKE, px, py, pz,
                    0.0f, 0.02f, 0.0f, 0.18f + random.nextFloat() * 0.04f, 14);
        }
    }

    private float redstoneTorchBurnoutPitch() {
        return 2.6f + (random.nextFloat() - random.nextFloat()) * 0.8f;
    }

    public void playItemPickupSound(float x, float y, float z) {
        float pitch = ((random.nextFloat() - random.nextFloat()) * 0.7f + 1.0f) * 2.0f;
        playSound(WorldSoundEvent.ITEM_PICKUP, x, y, z, 0.2f, pitch);
    }

    public void playExperiencePickupSound(float x, float y, float z) {
        float pitch = 0.5f * ((random.nextFloat() - random.nextFloat()) * 0.7f + 1.8f);
        playSound(WorldSoundEvent.XP_PICKUP, x, y, z, 0.1f, pitch);
    }

    public void playExperienceLevelUpSound(float x, float y, float z) {
        playSound(WorldSoundEvent.XP_LEVEL_UP, x, y, z, 0.75f, 1.0f);
    }

    public void playEatSound(float x, float y, float z) {
        playSound(WorldSoundEvent.EAT, x, y, z, 0.5f, consumePitch());
    }

    public void playDrinkSound(float x, float y, float z) {
        playSound(WorldSoundEvent.DRINK, x, y, z, 0.5f, consumePitch());
    }

    public void playBurpSound(float x, float y, float z) {
        playSound(WorldSoundEvent.BURP, x, y, z, 0.5f, consumePitch());
    }

    private float consumePitch() {
        return random.nextFloat() * 0.1f + 0.9f;
    }

    public void playFallSound(float x, float y, float z, float fallDistance) {
        String soundId = WorldSoundEvent.fallSoundId(fallDistance);
        if (soundId != null) {
            playSound(soundId, x, y, z, 1.0f, 1.0f);
        }
    }

    public void playChestSound(String soundId, float x, float y, float z) {
        playSound(soundId, x, y, z, 0.5f, random.nextFloat() * 0.1f + 0.9f);
    }

    public void playBlockBreakSound(BlockType type, int x, int y, int z) {
        String soundId = WorldSoundEvent.blockBreakSoundId(type);
        if (soundId != null) {
            playSound(soundId, x + 0.5f, y + 0.5f, z + 0.5f,
                    WorldSoundEvent.blockInteractionVolume(type),
                    WorldSoundEvent.blockBreakPitch(type));
        }
    }

    public void playBlockPlaceSound(BlockType type, int x, int y, int z) {
        String soundId = WorldSoundEvent.blockPlaceSoundId(type);
        if (soundId != null) {
            playSound(soundId, x + 0.5f, y + 0.5f, z + 0.5f,
                    WorldSoundEvent.blockInteractionVolume(type),
                    WorldSoundEvent.blockPlacePitch(type));
        }
    }

    public void playBlockStepSound(BlockType type, float x, float y, float z) {
        String soundId = WorldSoundEvent.blockStepSoundId(type);
        if (soundId != null) {
            playSound(soundId, x, y, z,
                    WorldSoundEvent.blockStepVolume(type),
                    WorldSoundEvent.blockStepPitch(type));
        }
    }

    void playOpenableSound(BlockType type, int x, int y, int z, boolean open) {
        String soundId = WorldSoundEvent.openableSoundId(type, open);
        if (soundId != null) {
            playSound(soundId, x + 0.5f, y + 0.5f, z + 0.5f, 1.0f,
                    WorldSoundEvent.openablePitch(random));
        }
    }

    public List<WorldSoundEvent> getSoundEvents() {
        return soundEvents;
    }

    public List<WorldSoundEvent> drainSoundEvents() {
        List<WorldSoundEvent> drained = new ArrayList<>(soundEvents);
        soundEvents.clear();
        return drained;
    }

    /**
     * Try to collect nearby items into the player's inventory.
     * Collects the amount that fits and leaves any remainder in the world.
     *
     * @return List of collected items (for adding to inventory)
     */
    public List<DroppedItem> collectNearbyItems(float playerX, float playerY, float playerZ,
            float deltaTime, com.craftzero.main.Player player) {
        List<DroppedItem> collected = new ArrayList<>();
        Iterator<DroppedItem> iterator = droppedItems.iterator();

        while (iterator.hasNext()) {
            DroppedItem item = iterator.next();
            if (!item.canPickup()) {
                continue;
            }
            float dx = item.getX() - playerX;
            float dy = item.getY() - playerY;
            float dz = item.getZ() - playerZ;
            if (dx * dx + dy * dy + dz * dz > DROPPED_ITEM_PICKUP_SCAN_RADIUS_SQ) {
                continue;
            }
            ItemStack available = item.toItemStack();
            int addable = player.countAddableToInventory(available);
            if (addable <= 0 || !item.tryCollect(playerX, playerY, playerZ, deltaTime)) {
                continue;
            }
            ItemStack transfer = item.toItemStack();
            transfer.setCount(Math.min(addable, item.getCount()));
            int beforeTransfer = transfer.getCount();
            player.addStackToInventory(transfer);
            int moved = beforeTransfer - transfer.getCount();
            if (moved <= 0) {
                continue;
            }
            DroppedItem collectedItem = item.splitOff(moved);
            playItemPickupSound(item.getX(), item.getY(), item.getZ());
            spawnItemPickupParticle(item.getItemType(),
                    item.getX(), item.getY() + 0.2f, item.getZ(),
                    playerX, playerY, playerZ);
            if (collectedItem != null) {
                collected.add(collectedItem);
            }
            if (item.getCount() <= 0) {
                iterator.remove();
            }
        }

        return collected;
    }

    /**
     * Get all dropped items (for rendering).
     */
    public List<DroppedItem> getDroppedItems() {
        return droppedItems;
    }

    public void replaceDroppedItems(List<DroppedItem> items) {
        droppedItems.clear();
        if (items == null) {
            return;
        }
        for (DroppedItem item : items) {
            if (item == null) {
                continue;
            }
            item.attachToWorld(this);
            droppedItems.add(item);
        }
    }

    public java.util.Collection<Chunk> getLoadedChunks() {
        return chunks.values();
    }

    /**
     * Get the texture atlas (for dropped item rendering).
     */
    public Texture getAtlas() {
        return atlas;
    }

    // ===== ENTITY MANAGEMENT =====

    /**
     * Spawn an entity into the world.
     */
    public void spawnEntity(Entity entity) {
        entity.setWorld(this);
        entitiesToAdd.add(entity);
    }

    public void addEntityNow(Entity entity) {
        if (entity == null) {
            return;
        }
        entity.setWorld(this);
        entitiesToAdd.remove(entity);
        entitiesToRemove.remove(entity);
        if (!entities.contains(entity)) {
            entities.add(entity);
        }
    }

    public ArrowEntity spawnArrow(float x, float y, float z, float motionX, float motionY, float motionZ,
            Entity shooter, boolean playerOwned, float damage) {
        ArrowEntity arrow = new ArrowEntity(x, y, z, motionX, motionY, motionZ, shooter, playerOwned, damage);
        spawnEntity(arrow);
        return arrow;
    }

    public FireballEntity spawnFireball(float x, float y, float z, float motionX, float motionY, float motionZ,
            Entity shooter, boolean explosive) {
        FireballEntity fireball = new FireballEntity(x, y, z, motionX, motionY, motionZ, shooter, explosive);
        spawnEntity(fireball);
        return fireball;
    }

    public SplashPotionEntity spawnSplashPotion(float x, float y, float z, float motionX, float motionY, float motionZ,
            Entity shooter, PotionData potionData) {
        SplashPotionEntity potion = new SplashPotionEntity(x, y, z, motionX, motionY, motionZ, shooter, potionData);
        spawnEntity(potion);
        return potion;
    }

    public ThrownItemEntity spawnThrownItemProjectile(float x, float y, float z,
            float motionX, float motionY, float motionZ, ItemType itemType, Entity shooter) {
        return spawnThrownItemProjectile(x, y, z, motionX, motionY, motionZ, itemType, shooter, false);
    }

    public ThrownItemEntity spawnThrownItemProjectile(float x, float y, float z,
            float motionX, float motionY, float motionZ, ItemType itemType, Entity shooter, boolean playerOwned) {
        ThrownItemEntity projectile = new ThrownItemEntity(x, y, z, motionX, motionY, motionZ, itemType, shooter,
                playerOwned);
        spawnEntity(projectile);
        return projectile;
    }

    public EnderPearlEntity spawnEnderPearl(float x, float y, float z,
            float motionX, float motionY, float motionZ, com.craftzero.main.Player owner) {
        EnderPearlEntity pearl = new EnderPearlEntity(x, y, z, motionX, motionY, motionZ, owner);
        spawnEntity(pearl);
        return pearl;
    }

    public MinecartEntity spawnMinecart(float x, float y, float z, MinecartEntity.CartKind kind) {
        MinecartEntity cart = switch (kind) {
            case CHEST -> new ChestMinecartEntity(x, y, z);
            case FURNACE -> new FurnaceMinecartEntity(x, y, z);
            default -> new MinecartEntity(x, y, z, MinecartEntity.CartKind.RIDEABLE);
        };
        spawnEntity(cart);
        return cart;
    }

    public boolean placeMinecartOnRail(int x, int y, int z, ItemType itemType) {
        BlockType rail = getBlockIfLoaded(x, y, z, BlockType.AIR);
        if (!RailShapeResolver.isRail(rail)) {
            return false;
        }
        if (itemType == null) {
            return false;
        }
        MinecartEntity.CartKind kind = switch (itemType) {
            case MINECART -> MinecartEntity.CartKind.RIDEABLE;
            case CHEST_MINECART -> MinecartEntity.CartKind.CHEST;
            case FURNACE_MINECART -> MinecartEntity.CartKind.FURNACE;
            default -> null;
        };
        if (kind == null) {
            return false;
        }
        int metadata = getBlockMetadataIfLoaded(x, y, z, 0);
        int shape = RailShapeResolver.shapeFromMetadata(rail, metadata);
        float slopeOffset = RailShapeResolver.isAscending(shape) ? 0.5f : 0.0f;
        spawnMinecart(x + 0.5f, y + 0.1f + slopeOffset, z + 0.5f, kind);
        return true;
    }

    public BoatEntity spawnBoat(float x, float y, float z) {
        BoatEntity boat = new BoatEntity(x, y, z);
        spawnEntity(boat);
        return boat;
    }

    public boolean placeBoatOnWater(int x, int y, int z, float yaw) {
        BlockType block = getBlockIfLoaded(x, y, z, BlockType.AIR);
        if (!block.isWater()) {
            return false;
        }
        BoatEntity boat = new BoatEntity(x + 0.5f, y + 0.25f, z + 0.5f);
        boat.setYaw(yaw);
        AABB spawnBox = boat.getBoundingBox().expand(-0.1f);
        if (isBlockedByLoadedCollision(spawnBox)
                || hasEntityIntersecting(spawnBox.getMin().x, spawnBox.getMin().y, spawnBox.getMin().z,
                        spawnBox.getMax().x, spawnBox.getMax().y, spawnBox.getMax().z, true)) {
            return false;
        }
        spawnEntity(boat);
        return true;
    }

    public PaintingEntity placePainting(int supportX, int supportY, int supportZ, int face) {
        PaintingEntity painting = PaintingEntity.create(this, supportX, supportY, supportZ, face, random);
        if (painting == null) {
            return null;
        }
        spawnEntity(painting);
        return painting;
    }

    public PrimedTntEntity spawnPrimedTnt(float x, float y, float z, int fuseTicks,
            float motionX, float motionY, float motionZ) {
        PrimedTntEntity tnt = new PrimedTntEntity(x, y, z, fuseTicks);
        tnt.setMotion(motionX, motionY, motionZ);
        spawnEntity(tnt);
        return tnt;
    }

    public PrimedTntEntity primeTnt(int x, int y, int z, int fuseTicks) {
        return primeTnt(x, y, z, fuseTicks, true);
    }

    public PrimedTntEntity primeTntByPlayer(int x, int y, int z, int fuseTicks) {
        PrimedTntEntity tnt = primeTnt(x, y, z, fuseTicks, true);
        if (tnt != null) {
            tnt.setPlayerOwned(true);
        }
        return tnt;
    }

    public PrimedTntEntity primeTntByRemotePlayer(int x, int y, int z, int fuseTicks,
            String remoteOwnerPlayerId) {
        PrimedTntEntity tnt = primeTnt(x, y, z, fuseTicks, true);
        if (tnt != null) {
            tnt.setRemoteOwnerPlayerId(remoteOwnerPlayerId);
        }
        return tnt;
    }

    private PrimedTntEntity primeTnt(int x, int y, int z, int fuseTicks, boolean playFuseSound) {
        if (getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.TNT) {
            return null;
        }
        setBlockIfLoaded(x, y, z, BlockType.AIR, 0);
        if (playFuseSound) {
            playSound(WorldSoundEvent.TNT_FUSE, x + 0.5f, y + 0.5f, z + 0.5f, 1.0f, 1.0f);
        }
        float angle = random.nextFloat() * (float) (Math.PI * 2.0);
        return spawnPrimedTnt(x + 0.5f, y, z + 0.5f, fuseTicks,
                -(float) Math.sin(angle) * 0.02f, 0.2f, -(float) Math.cos(angle) * 0.02f);
    }

    public void spawnExperience(float x, float y, float z, int amount) {
        int remaining = Math.max(0, amount);
        while (remaining > 0) {
            int value = ExperienceOrbEntity.getOrbValue(remaining);
            remaining -= value;
            spawnEntity(new ExperienceOrbEntity(x, y, z, value, random));
        }
    }

    /**
     * Remove an entity from the world.
     */
    public void removeEntity(Entity entity) {
        entitiesToRemove.add(entity);
    }

    public void removeEntityNow(Entity entity) {
        if (entity == null) {
            return;
        }
        entity.remove();
        entities.remove(entity);
        entitiesToAdd.remove(entity);
        entitiesToRemove.remove(entity);
    }

    /**
     * Update all entities in the world.
     */
    public void updateEntities(float deltaTime) {
        Entity generated;
        while ((generated = generatedEntities.poll()) != null) {
            generated.setWorld(this);
            entitiesToAdd.add(generated);
        }

        // Add pending entities
        entities.addAll(entitiesToAdd);
        entitiesToAdd.clear();

        if (player != null) {
            player.updateExperiencePickupCooldown(deltaTime);
        }

        // Update all entities
        Iterator<Entity> iterator = entities.iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next();

            entity.tick();
            entity.updatePhysics(deltaTime);
            if (!entity.isRemoved()) {
                schedulePressurePlateUpdatesForAabb(entity.getBoundingBox(), true);
                activateRedstoneOreBelow(entity.getBoundingBox());
                applyHazardContact(entity);
            }

            // Remove dead entities
            if (entity.isRemoved()) {
                entitiesToRemove.add(entity);
            }
        }

        resolveMinecartCollisions();
        resolveBoatCollisions();

        // Remove pending entities
        entities.removeAll(entitiesToRemove);
        entitiesToRemove.clear();
    }

    private void resolveMinecartCollisions() {
        for (int i = 0; i < entities.size(); i++) {
            Entity first = entities.get(i);
            if (!(first instanceof MinecartEntity firstCart) || firstCart.isRemoved()) {
                continue;
            }
            AABB firstBox = minecartCollisionBox(firstCart);
            for (int j = 0; j < entities.size(); j++) {
                if (j == i) {
                    continue;
                }
                Entity second = entities.get(j);
                if (second == null || second.isRemoved()) {
                    continue;
                }
                if (second instanceof MinecartEntity secondCart) {
                    if (j <= i || !firstBox.intersects(secondCart.getBoundingBox())) {
                        continue;
                    }
                    firstCart.collideWithMinecart(secondCart);
                } else if (second instanceof LivingEntity living && firstBox.intersects(living.getBoundingBox())) {
                    firstCart.collideWithLivingEntity(living);
                }
            }
            if (player != null && player.getBoundingBox() != null
                    && !player.isRidingMinecart()
                    && firstBox.intersects(player.getBoundingBox())) {
                player.collideWithMinecart(firstCart);
            }
        }
        for (Entity entity : entities) {
            if (entity instanceof MinecartEntity cart && !cart.isRemoved()) {
                cart.syncPassengerPosition();
            }
        }
    }

    private static AABB minecartCollisionBox(MinecartEntity cart) {
        AABB box = cart.getBoundingBox();
        return new AABB(
                box.getMin().x - 0.2f, box.getMin().y, box.getMin().z - 0.2f,
                box.getMax().x + 0.2f, box.getMax().y, box.getMax().z + 0.2f);
    }

    private void resolveBoatCollisions() {
        for (int i = 0; i < entities.size(); i++) {
            Entity first = entities.get(i);
            if (!(first instanceof BoatEntity firstBoat) || firstBoat.isRemoved()) {
                continue;
            }
            AABB firstBox = firstBoat.getBoundingBox().expand(0.2f);
            for (int j = 0; j < entities.size(); j++) {
                if (j == i) {
                    continue;
                }
                Entity second = entities.get(j);
                if (second == null || second.isRemoved()) {
                    continue;
                }
                if (second instanceof BoatEntity secondBoat) {
                    if (j <= i || !firstBox.intersects(secondBoat.getBoundingBox())) {
                        continue;
                    }
                    firstBoat.collideWithBoat(secondBoat);
                } else if (second instanceof LivingEntity living && firstBox.intersects(living.getBoundingBox())) {
                    firstBoat.collideWithEntity(living);
                }
            }
            if (player != null && player.getBoundingBox() != null
                    && !player.isRidingBoat()
                    && firstBox.intersects(player.getBoundingBox())) {
                player.collideWithBoat(firstBoat);
            }
        }
    }

    private void applyHazardContact(Entity entity) {
        if (entity == null || entity.getBoundingBox() == null) {
            return;
        }
        HazardContact contact = hazardContact(entity.getBoundingBox());
        if (entity instanceof LivingEntity living) {
            if (contact.lava()) {
                living.setOnFire(ENTITY_LAVA_CONTACT_TICKS);
                living.damage(PLAYER_LAVA_CONTACT_DAMAGE, DamageSource.point(DamageSource.Type.FIRE,
                        entity.getX(), entity.getY(), entity.getZ(), 0.0f, 0.0f));
            } else if (contact.fire()) {
                living.setOnFire(ENTITY_FIRE_CONTACT_TICKS);
            }
            if (contact.cactus()) {
                living.damage(CACTUS_CONTACT_DAMAGE, DamageSource.generic());
            }
            if (contact.suffocation()) {
                living.damage(SUFFOCATION_DAMAGE, DamageSource.suffocation(
                        entity.getX(), entity.getY(), entity.getZ()));
            }
        } else {
            applyVehicleHazardContact(entity, contact);
        }
    }

    private void applyVehicleHazardContact(Entity entity, HazardContact contact) {
        if (!(entity instanceof BoatEntity) && !(entity instanceof MinecartEntity)) {
            return;
        }
        if (contact.lava()) {
            damageVehicle(entity, VEHICLE_LAVA_CONTACT_DAMAGE);
        } else if (contact.fire()) {
            damageVehicle(entity, VEHICLE_FIRE_CONTACT_DAMAGE);
        }
        if (!entity.isRemoved() && contact.cactus()) {
            damageVehicle(entity, CACTUS_CONTACT_DAMAGE);
        }
    }

    private void damageVehicle(Entity entity, float amount) {
        if (entity instanceof BoatEntity boat) {
            boat.attack(amount, false);
        } else if (entity instanceof MinecartEntity cart) {
            cart.attack(amount, false);
        }
    }

    public void applyPlayerHazardContact(com.craftzero.main.Player player) {
        if (player == null || player.getBoundingBox() == null) {
            return;
        }
        HazardContact contact = hazardContact(player.getBoundingBox());
        if (contact.lava()) {
            player.setOnFire(ENTITY_LAVA_CONTACT_TICKS);
            player.hurt(PLAYER_LAVA_CONTACT_DAMAGE, DamageSource.point(DamageSource.Type.FIRE,
                    player.getPosition().x, player.getPosition().y, player.getPosition().z, 0.0f, 0.0f));
        } else if (contact.fire()) {
            player.setOnFire(ENTITY_FIRE_CONTACT_TICKS);
            player.hurt(PLAYER_FIRE_CONTACT_DAMAGE, DamageSource.point(DamageSource.Type.FIRE,
                    player.getPosition().x, player.getPosition().y, player.getPosition().z, 0.0f, 0.0f));
        }
        if (contact.cactus()) {
            player.hurt(CACTUS_CONTACT_DAMAGE, DamageSource.generic());
        }
        if (contact.suffocation()) {
            player.hurt(SUFFOCATION_DAMAGE, DamageSource.suffocation(
                    player.getPosition().x, player.getPosition().y, player.getPosition().z));
        }
    }

    private HazardContact hazardContact(AABB box) {
        int minX = (int) Math.floor(box.getMin().x);
        int maxX = (int) Math.floor(box.getMax().x - 0.0001f);
        int minY = (int) Math.floor(box.getMin().y);
        int maxY = (int) Math.floor(box.getMax().y - 0.0001f);
        int minZ = (int) Math.floor(box.getMin().z);
        int maxZ = (int) Math.floor(box.getMax().z - 0.0001f);
        boolean fire = false;
        boolean cactus = false;
        boolean suffocation = false;
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    BlockType type = getBlockIfLoaded(x, y, z, BlockType.AIR);
                    if (type.isLava()) {
                        return HazardContact.LAVA;
                    }
                    if (type == BlockType.FIRE) {
                        fire = true;
                    } else if (type == BlockType.CACTUS && intersectsBlockCollision(box, x, y, z)) {
                        cactus = true;
                    } else if (BlockShape.isOpaqueCube(type) && intersectsBlockCollision(box, x, y, z)) {
                        suffocation = true;
                    }
                }
            }
        }
        return new HazardContact(fire, false, cactus, suffocation);
    }

    private boolean intersectsBlockCollision(AABB box, int x, int y, int z) {
        for (AABB collision : getCollisionBoxesIfLoaded(x, y, z)) {
            if (box.intersects(collision)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCactusContact(DroppedItem item) {
        return hazardContact(droppedItemBounds(item)).cactus();
    }

    private boolean applyDroppedItemHazardContact(DroppedItem item) {
        HazardContact contact = hazardContact(droppedItemBounds(item));
        if (contact.cactus()) {
            return true;
        }
        if (contact.lava()) {
            item.damage(DROPPED_ITEM_LAVA_DAMAGE, DamageSource.point(DamageSource.Type.FIRE,
                    item.getX(), item.getY(), item.getZ(), 0.0f, 0.0f));
            if (item.isDestroyed()) {
                return true;
            }
            if (item.shouldApplyLavaFeedback()) {
                item.setVelocity((random.nextFloat() - random.nextFloat()) * DROPPED_ITEM_LAVA_BOUNCE_HORIZONTAL,
                        DROPPED_ITEM_LAVA_BOUNCE_Y,
                        (random.nextFloat() - random.nextFloat()) * DROPPED_ITEM_LAVA_BOUNCE_HORIZONTAL);
                playSound(WorldSoundEvent.FIZZ, item.getX(), item.getY(), item.getZ(),
                        0.4f, 2.0f + random.nextFloat() * 0.4f);
            }
            return false;
        }
        if (contact.fire()) {
            item.damage(DROPPED_ITEM_FIRE_DAMAGE, DamageSource.point(DamageSource.Type.FIRE,
                    item.getX(), item.getY(), item.getZ(), 0.0f, 0.0f));
            return item.isDestroyed();
        }
        return false;
    }

    private record HazardContact(boolean fire, boolean lava, boolean cactus, boolean suffocation) {
        private static final HazardContact NONE = new HazardContact(false, false, false, false);
        private static final HazardContact LAVA = new HazardContact(true, true, false, false);
    }

    /**
     * Get all entities in the world.
     */
    public List<Entity> getEntities() {
        return entities;
    }

    public List<Entity> getEntitiesIncludingPending() {
        List<Entity> result = new ArrayList<>(entities.size() + entitiesToAdd.size() + generatedEntities.size());
        addLiveEntities(result, entities);
        addLiveEntities(result, entitiesToAdd);
        addLiveEntities(result, generatedEntities);
        return result;
    }

    private static void addLiveEntities(List<Entity> target, Collection<? extends Entity> source) {
        for (Entity entity : source) {
            if (entity != null && !entity.isRemoved()) {
                target.add(entity);
            }
        }
    }

    public boolean hasEntityIntersecting(float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ, boolean includeDroppedItems) {
        AABB area = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        return hasEntityIntersecting(area, null, includeDroppedItems);
    }

    public boolean hasEntityIntersecting(AABB area, Entity excluded, boolean includeDroppedItems) {
        if (area == null) {
            return false;
        }
        if (hasEntityIntersecting(entities, area, excluded)
                || hasEntityIntersecting(entitiesToAdd, area, excluded)) {
            return true;
        }
        if (includeDroppedItems) {
            for (DroppedItem item : droppedItems) {
                if (droppedItemBounds(item).intersects(area)) {
                    return true;
                }
            }
        }
        return player != null && player.getBoundingBox().intersects(area);
    }

    private static boolean hasEntityIntersecting(Collection<? extends Entity> candidates,
            AABB area, Entity excluded) {
        for (Entity entity : candidates) {
            if (entity == excluded || entity.isRemoved()) {
                continue;
            }
            if (entity.getBoundingBox().intersects(area)) {
                return true;
            }
        }
        return false;
    }

    private static AABB droppedItemBounds(DroppedItem item) {
        return new AABB(
                item.getX() - DROPPED_ITEM_HALF_SIZE,
                item.getY() - DROPPED_ITEM_HALF_SIZE,
                item.getZ() - DROPPED_ITEM_HALF_SIZE,
                item.getX() + DROPPED_ITEM_HALF_SIZE,
                item.getY() + DROPPED_ITEM_HALF_SIZE,
                item.getZ() + DROPPED_ITEM_HALF_SIZE);
    }

    public boolean hasLivingEntityIntersecting(float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ) {
        AABB area = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity && !entity.isRemoved() && entity.getBoundingBox().intersects(area)) {
                return true;
            }
        }
        for (Entity entity : entitiesToAdd) {
            if (entity instanceof LivingEntity && !entity.isRemoved() && entity.getBoundingBox().intersects(area)) {
                return true;
            }
        }
        return player != null && player.getBoundingBox().intersects(area);
    }

    public void pushEntitiesIntersectingBlock(int x, int y, int z, int dx, int dy, int dz) {
        if (dx == 0 && dy == 0 && dz == 0) {
            return;
        }
        AABB area = AABB.forBlock(x, y, z);
        pushEntitiesIntersectingArea(entities, area, dx, dy, dz);
        pushEntitiesIntersectingArea(entitiesToAdd, area, dx, dy, dz);
        pushDroppedItemsIntersectingArea(area, dx, dy, dz);
        pushPlayerIntersectingArea(area, dx, dy, dz);
    }

    private void pushEntitiesIntersectingArea(Collection<? extends Entity> source, AABB area,
            float dx, float dy, float dz) {
        for (Entity entity : source) {
            if (!entity.isRemoved() && entity.getBoundingBox().intersects(area)) {
                AABB destination = entity.getBoundingBox().offset(dx, dy, dz);
                if (isBlockedByLoadedCollision(destination)) {
                    if (entity instanceof LivingEntity living) {
                        living.damage(PISTON_BLOCKED_ENTITY_DAMAGE, DamageSource.generic());
                    }
                } else {
                    entity.setPosition(entity.getX() + dx, entity.getY() + dy, entity.getZ() + dz);
                    entity.addMotion(dx * 0.2f, dy * 0.2f, dz * 0.2f);
                    syncPistonMovedEntity(entity);
                }
            }
        }
    }

    private void pushEntitiesOutOfMovingPistonArea(Collection<? extends Entity> source, AABB area,
            float moveX, float moveY, float moveZ) {
        for (Entity entity : source) {
            if (entity.isRemoved()) {
                continue;
            }
            AABB entityBox = entity.getBoundingBox();
            if (!entityBox.intersects(area)) {
                continue;
            }
            float[] push = movingPistonEntityPush(area, entityBox, moveX, moveY, moveZ);
            if (push == null) {
                continue;
            }
            AABB destination = entityBox.offset(push[0], push[1], push[2]);
            if (isBlockedByLoadedCollision(destination)) {
                if (entity instanceof LivingEntity living) {
                    living.damage(PISTON_BLOCKED_ENTITY_DAMAGE, DamageSource.generic());
                }
            } else {
                entity.setPosition(entity.getX() + push[0], entity.getY() + push[1], entity.getZ() + push[2]);
                entity.addMotion(push[0] * 0.2f, push[1] * 0.2f, push[2] * 0.2f);
                syncPistonMovedEntity(entity);
            }
        }
    }

    private static void syncPistonMovedEntity(Entity entity) {
        if (entity instanceof MinecartEntity cart) {
            cart.syncPassengerPosition();
        }
    }

    private void pushDroppedItemsIntersectingArea(AABB area, float dx, float dy, float dz) {
        for (DroppedItem item : droppedItems) {
            if (area.contains(new org.joml.Vector3f(item.getX(), item.getY(), item.getZ()))) {
                item.moveBy(dx, dy, dz);
            }
        }
    }

    private void pushDroppedItemsOutOfMovingPistonArea(AABB area, float moveX, float moveY, float moveZ) {
        for (DroppedItem item : droppedItems) {
            if (!area.contains(new org.joml.Vector3f(item.getX(), item.getY(), item.getZ()))) {
                continue;
            }
            float[] push = movingPistonPointPush(area, item.getX(), item.getY(), item.getZ(),
                    moveX, moveY, moveZ);
            if (push != null) {
                item.moveBy(push[0], push[1], push[2]);
            }
        }
    }

    private void pushPlayerIntersectingArea(AABB area, float dx, float dy, float dz) {
        if (player == null || !player.getBoundingBox().intersects(area)) {
            return;
        }
        AABB destination = player.getBoundingBox().offset(dx, dy, dz);
        if (isBlockedByLoadedCollision(destination)) {
            player.hurt(PISTON_BLOCKED_ENTITY_DAMAGE, DamageSource.generic());
        } else {
            movePlayerByPiston(dx, dy, dz);
        }
    }

    private void pushPlayerOutOfMovingPistonArea(AABB area, float moveX, float moveY, float moveZ) {
        if (player == null) {
            return;
        }
        AABB playerBox = player.getBoundingBox();
        if (!playerBox.intersects(area)) {
            return;
        }
        float[] push = movingPistonEntityPush(area, playerBox, moveX, moveY, moveZ);
        if (push == null) {
            return;
        }
        AABB destination = playerBox.offset(push[0], push[1], push[2]);
        if (isBlockedByLoadedCollision(destination)) {
            player.hurt(PISTON_BLOCKED_ENTITY_DAMAGE, DamageSource.generic());
        } else {
            movePlayerByPiston(push[0], push[1], push[2]);
        }
    }

    private void movePlayerByPiston(float dx, float dy, float dz) {
        org.joml.Vector3f position = player.getPosition();
        player.setPosition(position.x + dx, position.y + dy, position.z + dz);
        org.joml.Vector3f velocity = player.getVelocity();
        velocity.x += dx * 0.2f;
        velocity.y += dy * 0.2f;
        velocity.z += dz * 0.2f;
    }

    private float[] movingPistonEntityPush(AABB pistonBox, AABB entityBox,
            float moveX, float moveY, float moveZ) {
        if (moveX > 0.0f) {
            return new float[] {
                    pistonBox.getMax().x - entityBox.getMin().x + PISTON_ENTITY_CLEARANCE, 0.0f, 0.0f
            };
        }
        if (moveX < 0.0f) {
            return new float[] {
                    pistonBox.getMin().x - entityBox.getMax().x - PISTON_ENTITY_CLEARANCE, 0.0f, 0.0f
            };
        }
        if (moveY > 0.0f) {
            return new float[] {
                    0.0f, pistonBox.getMax().y - entityBox.getMin().y + PISTON_ENTITY_CLEARANCE, 0.0f
            };
        }
        if (moveY < 0.0f) {
            return new float[] {
                    0.0f, pistonBox.getMin().y - entityBox.getMax().y - PISTON_ENTITY_CLEARANCE, 0.0f
            };
        }
        if (moveZ > 0.0f) {
            return new float[] {
                    0.0f, 0.0f, pistonBox.getMax().z - entityBox.getMin().z + PISTON_ENTITY_CLEARANCE
            };
        }
        if (moveZ < 0.0f) {
            return new float[] {
                    0.0f, 0.0f, pistonBox.getMin().z - entityBox.getMax().z - PISTON_ENTITY_CLEARANCE
            };
        }
        return null;
    }

    private float[] movingPistonPointPush(AABB pistonBox, float x, float y, float z,
            float moveX, float moveY, float moveZ) {
        if (moveX > 0.0f) {
            return new float[] { pistonBox.getMax().x - x + PISTON_ENTITY_CLEARANCE, 0.0f, 0.0f };
        }
        if (moveX < 0.0f) {
            return new float[] { pistonBox.getMin().x - x - PISTON_ENTITY_CLEARANCE, 0.0f, 0.0f };
        }
        if (moveY > 0.0f) {
            return new float[] { 0.0f, pistonBox.getMax().y - y + PISTON_ENTITY_CLEARANCE, 0.0f };
        }
        if (moveY < 0.0f) {
            return new float[] { 0.0f, pistonBox.getMin().y - y - PISTON_ENTITY_CLEARANCE, 0.0f };
        }
        if (moveZ > 0.0f) {
            return new float[] { 0.0f, 0.0f, pistonBox.getMax().z - z + PISTON_ENTITY_CLEARANCE };
        }
        if (moveZ < 0.0f) {
            return new float[] { 0.0f, 0.0f, pistonBox.getMin().z - z - PISTON_ENTITY_CLEARANCE };
        }
        return null;
    }

    private boolean isBlockedByLoadedCollision(AABB box) {
        int minX = (int) Math.floor(box.getMin().x);
        int minY = (int) Math.floor(box.getMin().y);
        int minZ = (int) Math.floor(box.getMin().z);
        int maxX = (int) Math.floor(box.getMax().x - 0.0001f);
        int maxY = (int) Math.floor(box.getMax().y - 0.0001f);
        int maxZ = (int) Math.floor(box.getMax().z - 0.0001f);
        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    for (AABB collision : getCollisionBoxesIfLoaded(bx, by, bz)) {
                        if (box.intersects(collision)) {
                            return true;
                        }
                    }
                }
            }
        }
        for (AABB collision : getMovingPistonCollisionBoxes(box)) {
            if (box.intersects(collision)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasMinecartAt(int x, int y, int z) {
        AABB area = new AABB(x, y, z, x + 1, y + 1, z + 1);
        return hasMinecartIntersecting(area);
    }

    public boolean hasMinecartIntersecting(float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ) {
        return hasMinecartIntersecting(new AABB(minX, minY, minZ, maxX, maxY, maxZ));
    }

    private boolean hasMinecartIntersecting(AABB area) {
        for (Entity entity : entities) {
            if (entity instanceof MinecartEntity && !entity.isRemoved() && entity.getBoundingBox().intersects(area)) {
                return true;
            }
        }
        for (Entity entity : entitiesToAdd) {
            if (entity instanceof MinecartEntity && !entity.isRemoved() && entity.getBoundingBox().intersects(area)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasEntityOfType(Class<? extends Entity> type) {
        if (type == null) {
            return false;
        }
        for (Entity entity : entities) {
            if (type.isInstance(entity) && !entity.isRemoved()) {
                return true;
            }
        }
        for (Entity entity : entitiesToAdd) {
            if (type.isInstance(entity) && !entity.isRemoved()) {
                return true;
            }
        }
        for (Entity entity : generatedEntities) {
            if (type.isInstance(entity) && !entity.isRemoved()) {
                return true;
            }
        }
        return false;
    }

    public void onEndCrystalDestroyed(EndCrystalEntity crystal) {
        if (crystal == null) {
            return;
        }
        notifyDragonsOfCrystalDestruction(entities, crystal);
        notifyDragonsOfCrystalDestruction(entitiesToAdd, crystal);
        notifyDragonsOfCrystalDestruction(generatedEntities, crystal);
    }

    private static void notifyDragonsOfCrystalDestruction(Collection<? extends Entity> source,
            EndCrystalEntity crystal) {
        for (Entity entity : source) {
            if (entity instanceof EnderDragon dragon && !dragon.isRemoved()) {
                dragon.onHealingCrystalDestroyed(crystal);
            }
        }
    }

    public void replaceEntities(Collection<? extends Entity> restoredEntities) {
        entities.clear();
        entitiesToAdd.clear();
        entitiesToRemove.clear();
        if (restoredEntities == null) {
            return;
        }
        for (Entity entity : restoredEntities) {
            if (entity == null || entity.isRemoved()) {
                continue;
            }
            entity.setWorld(this);
            entities.add(entity);
        }
    }

    /**
     * Set the player reference (for AI targeting).
     */
    public void setPlayer(com.craftzero.main.Player player) {
        this.player = player;
    }

    /**
     * Get the player reference.
     */
    public com.craftzero.main.Player getPlayer() {
        return player;
    }

    /**
     * Set the day/night cycle manager reference.
     */
    public void setDayCycleManager(DayCycleManager manager) {
        this.dayCycleManager = manager;
    }

    /**
     * Get the day/night cycle manager.
     */
    public DayCycleManager getDayCycleManager() {
        return dayCycleManager;
    }

    public void updateWeather(float deltaTime) {
        if (dimension != Dimension.OVERWORLD) {
            return;
        }
        weatherTickAccumulator += Math.max(0.0f, deltaTime) * 20.0f;
        int ticks = (int) weatherTickAccumulator;
        if (ticks <= 0) {
            return;
        }
        weatherTickAccumulator -= ticks;
        for (int i = 0; i < ticks; i++) {
            tickWeather();
            tickWeatherStrengths();
            tickPrecipitationEffects();
        }
    }

    private void tickWeather() {
        if (thunderTime > 0) {
            thunderTime--;
        }
        if (thunderTime == 0) {
            if (thundering) {
                thundering = false;
                thunderTime = nextThunderActiveTime();
            } else {
                thundering = raining;
                thunderTime = thundering ? nextThunderActiveTime() : nextClearWeatherTime();
            }
            syncWeatherState();
        }

        if (rainTime > 0) {
            rainTime--;
        }
        if (rainTime == 0) {
            if (raining || thundering) {
                raining = false;
                thundering = false;
                rainTime = nextRainActiveTime();
                thunderTime = nextClearWeatherTime();
            } else {
                raining = true;
                rainTime = nextRainActiveTime();
            }
            syncWeatherState();
        }

        if (thundering) {
            tryNaturalLightningStrike();
        }
    }

    public static String normalizeWeatherState(String weather) {
        String normalized = weather == null ? "clear" : weather.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case "rain", "thunder" -> normalized;
            default -> "clear";
        };
    }

    public void setWeatherState(String weather) {
        applyWeatherState(weather);
        resetWeatherTimersForState(weatherState);
    }

    public void setWeatherState(String weather, Integer rainTicks, Integer thunderTicks) {
        applyWeatherState(weather);
        rainTime = rainTicks == null || rainTicks <= 0
                ? defaultRainTimeForState(weatherState)
                : rainTicks;
        thunderTime = thunderTicks == null || thunderTicks <= 0
                ? defaultThunderTimeForState(weatherState)
                : thunderTicks;
        weatherTickAccumulator = 0.0f;
    }

    public String getWeatherState() {
        return weatherState;
    }

    public int getRainTime() {
        return rainTime;
    }

    public int getThunderTime() {
        return thunderTime;
    }

    public float getRainStrength(float partialTick) {
        float t = clamp01(partialTick);
        return prevRainStrength + (rainStrength - prevRainStrength) * t;
    }

    public float getThunderStrength(float partialTick) {
        float t = clamp01(partialTick);
        return prevThunderStrength + (thunderStrength - prevThunderStrength) * t;
    }

    public float getLightningFlashStrength(float partialTick) {
        float t = clamp01(partialTick);
        return prevLightningFlashStrength + (lightningFlashStrength - prevLightningFlashStrength) * t;
    }

    public boolean isRaining() {
        return dimension == Dimension.OVERWORLD && (raining || thundering);
    }

    public boolean isThundering() {
        return dimension == Dimension.OVERWORLD && thundering;
    }

    public boolean isRainingAt(int x, int y, int z) {
        com.craftzero.world.BiomeType biome = getReleaseBiome(x, z);
        return isRaining()
                && biome.hasPrecipitation()
                && !biome.canFreezeWater()
                && canSeeSky(x, y, z);
    }

    public boolean isSnowingAt(int x, int y, int z) {
        com.craftzero.world.BiomeType biome = getReleaseBiome(x, z);
        return isRaining()
                && biome.hasPrecipitation()
                && biome.canFreezeWater()
                && canSeeSky(x, y, z);
    }

    public boolean tryAccumulateSnowAtColumn(int x, int z) {
        int y = findPrecipitationSurfaceY(x, z);
        return y >= 0 && tryAccumulateSnowAt(x, y, z);
    }

    public boolean tryAccumulateSnowAt(int x, int y, int z) {
        if (y <= 0 || y >= Chunk.HEIGHT || !isChunkGeneratedForBlock(x, z)) {
            return false;
        }
        if (!isSnowingAt(x, y, z)) {
            return false;
        }
        if (getBlockIfLoaded(x, y, z, BlockType.BEDROCK) != BlockType.AIR) {
            return false;
        }
        if (getBlockLight(x, y, z) >= 10) {
            return false;
        }
        BlockType support = getBlockIfLoaded(x, y - 1, z, BlockType.AIR);
        if (support == BlockType.ICE
                || !BlockShape.canPlaceAt(BlockType.SNOW_LAYER, 0, contextAtIfLoaded(x, y, z))) {
            return false;
        }
        return setBlockIfLoaded(x, y, z, BlockType.SNOW_LAYER, 0);
    }

    public boolean tryFreezeWaterAtColumn(int x, int z) {
        int y = findWeatherFreezeSurfaceY(x, z);
        return y >= 0 && tryFreezeWaterAt(x, y, z);
    }

    public boolean tryFreezeWaterAt(int x, int y, int z) {
        if (!canFreezeWaterAt(x, y, z)) {
            return false;
        }
        return freezeWaterAt(x, y, z);
    }

    public boolean strikeLightningAt(int x, int y, int z) {
        if (!canLightningStrikeAt(x, y, z)) {
            return false;
        }

        playLightningSounds(x + 0.5f, y + 0.5f, z + 0.5f);
        flashLightningSky();
        addLightningBolt(new WorldLightningBolt(x + 0.5f, y, z + 0.5f, random));
        placeLightningFire(x, y, z);
        for (int i = 0; i < LIGHTNING_FIRE_EXTRA_ATTEMPTS; i++) {
            int fx = x + random.nextInt(3) - 1;
            int fy = y + random.nextInt(3) - 1;
            int fz = z + random.nextInt(3) - 1;
            if (canLightningStrikeAt(fx, fy, fz)) {
                placeLightningFire(fx, fy, fz);
            }
        }
        strikeLightningEntities(x + 0.5f, y + 0.5f, z + 0.5f);
        return true;
    }

    public void spawnNetworkLightning(WorldLightningBolt bolt) {
        addLightningBolt(bolt, false);
        flashLightningSky();
    }

    private void addLightningBolt(WorldLightningBolt bolt) {
        addLightningBolt(bolt, true);
    }

    private void addLightningBolt(WorldLightningBolt bolt, boolean recordEvent) {
        if (bolt == null) {
            return;
        }
        lightningBolts.add(bolt);
        if (recordEvent) {
            lightningEvents.add(bolt);
        }
    }

    public boolean strikeLightningAtColumn(int x, int z) {
        int y = findLightningStrikeY(x, z);
        return y >= 0 && strikeLightningAt(x, y, z);
    }

    private void applyWeatherState(String weather) {
        weatherState = normalizeWeatherState(weather);
        raining = "rain".equals(weatherState) || "thunder".equals(weatherState);
        thundering = "thunder".equals(weatherState);
    }

    private void resetWeatherTimersForState(String weather) {
        weatherTickAccumulator = 0.0f;
        weatherAmbientCooldownTicks = 0;
        rainTime = defaultRainTimeForState(weather);
        thunderTime = defaultThunderTimeForState(weather);
    }

    private void tickWeatherStrengths() {
        prevRainStrength = rainStrength;
        rainStrength = clamp01(rainStrength + (raining ? 0.01f : -0.01f));
        prevThunderStrength = thunderStrength;
        thunderStrength = clamp01(thunderStrength + (thundering ? 0.01f : -0.01f));
    }

    private void flashLightningSky() {
        prevLightningFlashStrength = 1.0f;
        lightningFlashStrength = 1.0f;
    }

    private void tickLightningFlash(float deltaTime) {
        prevLightningFlashStrength = lightningFlashStrength;
        lightningFlashStrength = Math.max(0.0f,
                lightningFlashStrength - Math.max(0.0f, deltaTime) * 20.0f * LIGHTNING_FLASH_DECAY_PER_TICK);
        if (lightningFlashStrength == 0.0f) {
            prevLightningFlashStrength = 0.0f;
        }
    }

    private int defaultRainTimeForState(String weather) {
        return "clear".equals(weather) ? nextClearWeatherTime() : nextRainActiveTime();
    }

    private int defaultThunderTimeForState(String weather) {
        return "thunder".equals(weather) ? nextThunderActiveTime() : nextClearWeatherTime();
    }

    private int nextClearWeatherTime() {
        return random.nextInt(WEATHER_CLEAR_RANDOM_TICKS) + WEATHER_CLEAR_MIN_TICKS;
    }

    private int nextRainActiveTime() {
        return random.nextInt(RAIN_ACTIVE_RANDOM_TICKS) + RAIN_ACTIVE_MIN_TICKS;
    }

    private int nextThunderActiveTime() {
        return random.nextInt(THUNDER_ACTIVE_RANDOM_TICKS) + THUNDER_ACTIVE_MIN_TICKS;
    }

    private void syncWeatherState() {
        weatherState = thundering ? "thunder" : raining ? "rain" : "clear";
    }

    private void tryNaturalLightningStrike() {
        if (player == null) {
            return;
        }
        int centerChunkX = (int) Math.floor(player.getPosition().x / Chunk.WIDTH);
        int centerChunkZ = (int) Math.floor(player.getPosition().z / Chunk.DEPTH);
        for (int dz = -renderDistance; dz <= renderDistance; dz++) {
            for (int dx = -renderDistance; dx <= renderDistance; dx++) {
                Chunk chunk = chunks.get(chunkKey(centerChunkX + dx, centerChunkZ + dz));
                if (!isGeneratedChunk(chunk) || random.nextInt(LIGHTNING_STRIKE_CHANCE) != 0) {
                    continue;
                }
                strikeNaturalLightningInChunk(chunk.getChunkX(), chunk.getChunkZ());
            }
        }
    }

    boolean strikeNaturalLightningInChunk(int chunkX, int chunkZ) {
        Chunk chunk = chunks.get(chunkKey(chunkX, chunkZ));
        if (!isGeneratedChunk(chunk)) {
            return false;
        }
        lightningUpdateLcg = lightningUpdateLcg * LIGHTNING_CHUNK_LCG_MULTIPLIER
                + LIGHTNING_CHUNK_LCG_INCREMENT;
        int sample = lightningUpdateLcg >> 2;
        int x = chunkX * Chunk.WIDTH + (sample & 15);
        int z = chunkZ * Chunk.DEPTH + ((sample >> 8) & 15);
        return strikeLightningAtColumn(x, z);
    }

    private static boolean isGeneratedChunk(Chunk chunk) {
        return chunk != null
                && chunk.getState().ordinal() >= Chunk.ChunkState.GENERATED.ordinal();
    }

    private void tickPrecipitationEffects() {
        if (!isRaining() || player == null) {
            weatherAmbientCooldownTicks = 0;
            return;
        }

        spawnPrecipitationParticlesNearPlayer();
        if (weatherAmbientCooldownTicks > 0) {
            weatherAmbientCooldownTicks--;
        }
        int playerX = (int) Math.floor(player.getPosition().x);
        int playerY = (int) Math.floor(player.getPosition().y);
        int playerZ = (int) Math.floor(player.getPosition().z);
        if (weatherAmbientCooldownTicks <= 0
                && (isRainingAt(playerX, playerY, playerZ) || isSnowingAt(playerX, playerY, playerZ))) {
            playSound(WorldSoundEvent.WEATHER_RAIN,
                    player.getPosition().x, player.getPosition().y, player.getPosition().z,
                    0.1f + getRainStrength(1.0f) * 0.2f, 1.0f);
            weatherAmbientCooldownTicks = WEATHER_RAIN_SOUND_INTERVAL_TICKS;
        }
    }

    private void spawnPrecipitationParticlesNearPlayer() {
        int baseX = (int) Math.floor(player.getPosition().x);
        int baseZ = (int) Math.floor(player.getPosition().z);
        for (int i = 0; i < PRECIPITATION_PARTICLES_PER_TICK; i++) {
            int x = baseX + random.nextInt(PRECIPITATION_EFFECT_RADIUS * 2 + 1) - PRECIPITATION_EFFECT_RADIUS;
            int z = baseZ + random.nextInt(PRECIPITATION_EFFECT_RADIUS * 2 + 1) - PRECIPITATION_EFFECT_RADIUS;
            spawnPrecipitationParticleAtColumn(x, z);
        }
    }

    private boolean spawnPrecipitationParticleAtColumn(int x, int z) {
        int y = findPrecipitationSurfaceY(x, z);
        if (y < 0 || y >= Chunk.HEIGHT) {
            return false;
        }
        boolean snowing = isSnowingAt(x, y, z);
        boolean rainingHere = !snowing && isRainingAt(x, y, z);
        if (!snowing && !rainingHere) {
            return false;
        }
        float px = x + random.nextFloat();
        float pz = z + random.nextFloat();
        if (snowing) {
            float py = Math.min(Chunk.HEIGHT - 0.25f, y + 4.0f + random.nextFloat() * 3.0f);
            spawnParticle(WorldParticle.Type.SNOW, px, py, pz,
                    (random.nextFloat() - 0.5f) * 0.08f,
                    -0.45f,
                    (random.nextFloat() - 0.5f) * 0.08f,
                    0.18f + random.nextFloat() * 0.04f, 40);
            if (random.nextInt(SNOW_ACCUMULATION_CHANCE) == 0) {
                tryAccumulateSnowAt(x, y, z);
            }
            if (random.nextInt(WEATHER_WATER_FREEZE_CHANCE) == 0) {
                tryFreezeWaterAtColumn(x, z);
            }
        } else {
            float py = Math.min(Chunk.HEIGHT - 0.05f, y + 0.10f);
            spawnParticle(WorldParticle.Type.RAIN, px, py, pz,
                    (random.nextFloat() - 0.5f) * 0.06f,
                    0.10f + random.nextFloat() * 0.20f,
                    (random.nextFloat() - 0.5f) * 0.06f,
                    0.16f + random.nextFloat() * 0.03f,
                    rainParticleLifetime(random));
        }
        return true;
    }

    private static int rainParticleLifetime(Random source) {
        return (int) (8.0f / (source.nextFloat() * 0.8f + 0.2f));
    }

    boolean tickCauldronRainFillAtColumn(int x, int z, Random randomSource) {
        return false;
    }

    private int findPrecipitationSurfaceY(int x, int z) {
        if (!isChunkGeneratedForBlock(x, z)) {
            return -1;
        }
        for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
            BlockType block = getBlockIfLoaded(x, y, z, BlockType.BEDROCK);
            if (block != BlockType.AIR && !block.isFluid()) {
                return Math.min(Chunk.HEIGHT - 1, y + 1);
            }
        }
        return 0;
    }

    private int findWeatherFreezeSurfaceY(int x, int z) {
        if (!isChunkGeneratedForBlock(x, z)) {
            return -1;
        }
        for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
            BlockType block = getBlockIfLoaded(x, y, z, BlockType.BEDROCK);
            if (block == BlockType.AIR || block == BlockType.SNOW_LAYER || block == BlockType.LILY_PAD) {
                continue;
            }
            return block.isWater() ? y : -1;
        }
        return -1;
    }

    private boolean canFreezeWaterAt(int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT || !isChunkGeneratedForBlock(x, z)) {
            return false;
        }
        if (!isSnowingAt(x, y, z)) {
            return false;
        }
        BlockType block = getBlockIfLoaded(x, y, z, BlockType.BEDROCK);
        if (!block.isWater() || getBlockMetadataIfLoaded(x, y, z, -1) != 0) {
            return false;
        }
        return getBlockLight(x, y, z) < 10;
    }

    private boolean freezeWaterAt(int x, int y, int z) {
        BlockType previous = getBlockIfLoaded(x, y, z, BlockType.AIR);
        int previousMetadata = getBlockMetadataIfLoaded(x, y, z, 0);
        BlockType above = getBlockIfLoaded(x, y + 1, z, BlockType.AIR);
        int aboveMetadata = getBlockMetadataIfLoaded(x, y + 1, z, 0);
        boolean clearLilyPad = above == BlockType.LILY_PAD;
        boolean changed;

        suppressNeighborSupportUpdates = true;
        try {
            changed = setBlockIfLoaded(x, y, z, BlockType.ICE, 0);
            if (changed && clearLilyPad) {
                setBlockIfLoaded(x, y + 1, z, BlockType.AIR, 0);
            }
        } finally {
            suppressNeighborSupportUpdates = false;
        }

        if (!changed) {
            return false;
        }
        if (clearLilyPad) {
            notifyBlockChanged(x, y + 1, z, above, aboveMetadata, BlockType.AIR, 0);
        }
        notifyBlockChanged(x, y, z, previous, previousMetadata, BlockType.ICE, 0);
        return true;
    }

    private boolean canLightningStrikeAt(int x, int y, int z) {
        return y >= 0 && y < Chunk.HEIGHT
                && isChunkGeneratedForBlock(x, z)
                && isThundering()
                && isRainingAt(x, y, z);
    }

    private int findLightningStrikeY(int x, int z) {
        if (!isChunkGeneratedForBlock(x, z)) {
            return -1;
        }
        for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
            BlockType block = getBlockIfLoaded(x, y, z, BlockType.BEDROCK);
            if (block != BlockType.AIR && !block.isFluid()) {
                return Math.min(Chunk.HEIGHT - 1, y + 1);
            }
        }
        return 0;
    }

    private void playLightningSounds(float x, float y, float z) {
        playSound(WorldSoundEvent.WEATHER_THUNDER, x, y, z,
                10000.0f, 0.8f + random.nextFloat() * 0.2f);
        playSound(WorldSoundEvent.EXPLOSION, x, y, z,
                2.0f, 0.5f + random.nextFloat() * 0.2f);
    }

    private boolean placeLightningFire(int x, int y, int z) {
        BlockType target = getBlockIfLoaded(x, y, z, BlockType.BEDROCK);
        if (!BlockShape.isReplaceable(target)
                || !BlockShape.canPlaceAt(BlockType.FIRE, 0, contextAtIfLoaded(x, y, z))
                || !canLightningFireStay(x, y, z)) {
            return false;
        }
        return setBlockIfLoaded(x, y, z, BlockType.FIRE, 0);
    }

    private boolean canLightningFireStay(int x, int y, int z) {
        if (BlockShape.isOpaqueCube(getBlockIfLoaded(x, y - 1, z, BlockType.BEDROCK))) {
            return true;
        }
        int[][] dirs = {
                { 1, 0, 0 }, { -1, 0, 0 },
                { 0, 1, 0 }, { 0, -1, 0 },
                { 0, 0, 1 }, { 0, 0, -1 }
        };
        for (int[] dir : dirs) {
            if (canCatchFire(getBlockIfLoaded(x + dir[0], y + dir[1], z + dir[2], BlockType.AIR))) {
                return true;
            }
        }
        return false;
    }

    private void strikeLightningEntities(float x, float y, float z) {
        AABB strikeBox = new AABB(
                x - LIGHTNING_ENTITY_RADIUS, y - LIGHTNING_ENTITY_RADIUS, z - LIGHTNING_ENTITY_RADIUS,
                x + LIGHTNING_ENTITY_RADIUS, y + LIGHTNING_ENTITY_RADIUS * 2.0f, z + LIGHTNING_ENTITY_RADIUS);
        DamageSource source = DamageSource.point(DamageSource.Type.LIGHTNING, x, y, z, 0.0f, 0.0f);
        List<Pig> pigsToTransform = new ArrayList<>();
        strikeLightningEntities(entities, strikeBox, source, pigsToTransform);
        strikeLightningEntities(entitiesToAdd, strikeBox, source, pigsToTransform);
        for (Pig pig : pigsToTransform) {
            transformPigByLightning(pig);
        }
        if (player != null && player.getBoundingBox().intersects(strikeBox)) {
            player.hurt(LIGHTNING_DAMAGE, source);
        }
        notifyLightningDamageListeners(x, y, z);
    }

    private void strikeLightningEntities(Collection<? extends Entity> sourceEntities, AABB strikeBox,
            DamageSource source, List<Pig> pigsToTransform) {
        for (Entity entity : sourceEntities) {
            if (entity.isRemoved() || !entity.getBoundingBox().intersects(strikeBox)) {
                continue;
            }
            if (entity instanceof Pig pig) {
                pigsToTransform.add(pig);
                continue;
            }
            if (entity instanceof Creeper creeper) {
                creeper.setPowered(true);
            }
            if (entity instanceof LivingEntity living) {
                living.setOnFire(LIGHTNING_ENTITY_FIRE_TICKS);
                living.damage(LIGHTNING_DAMAGE, source);
            }
        }
    }

    private void transformPigByLightning(Pig pig) {
        if (pig == null || pig.isRemoved()) {
            return;
        }
        ZombiePigman pigman = new ZombiePigman();
        pigman.setPosition(pig.getX(), pig.getY(), pig.getZ());
        pigman.setMotion(pig.getMotionX(), pig.getMotionY(), pig.getMotionZ());
        pigman.setYaw(pig.getYaw());
        pigman.setPitch(pig.getPitch());
        pigman.setTicksExisted(pig.getTicksExisted());
        pigman.setWorld(this);

        detachPigLightningPassenger(pig);
        pig.remove();
        entitiesToRemove.add(pig);
        entities.remove(pig);
        entitiesToAdd.remove(pig);
        entities.add(pigman);
    }

    private void detachPigLightningPassenger(Pig pig) {
        if (player != null && player.getRidingPig() == pig) {
            player.dismountPig();
            return;
        }
        if (pig.hasPlayerPassenger()) {
            pig.dismountPlayer();
        }
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    /**
     * Create an explosion at the specified location.
     * Destroys blocks within radius and damages entities.
     *
     * @param x     Center X
     * @param y     Center Y
     * @param z     Center Z
     * @param power Explosion power (radius = power * 1.5)
     */
    public void explode(float x, float y, float z, float power) {
        explode(x, y, z, power, false);
    }

    public void explode(float x, float y, float z, float power, boolean flaming) {
        explode(x, y, z, power, flaming, null);
    }

    public void explode(float x, float y, float z, float power, boolean flaming, DamageSource source) {
        if (!allFinite(x, y, z, power) || power <= 0.0f) {
            return;
        }
        List<DroppedItem> preExistingDroppedItems = new ArrayList<>(droppedItems);
        ExplosionImpact impact = collectExplosionImpact(x, y, z, power);
        playSound(WorldSoundEvent.EXPLOSION, x, y, z, 4.0f, explosionPitch());
        spawnExplosionParticles(x, y, z, power, impact);
        for (BlockPos pos : impact.destroyedBlocks()) {
            destroyBlockByExplosion(pos, power);
        }
        if (flaming) {
            igniteExplosionFires(impact.affectedBlocks());
        }

        damagePaintingsByExplosion(x, y, z, power);
        damageEntitiesByExplosion(x, y, z, power, source);
        damageDroppedItemsByExplosion(x, y, z, power, preExistingDroppedItems);
        damagePlayerByExplosion(x, y, z, power, source);
        notifyExplosionDamageListeners(x, y, z, power);
    }

    private float explosionPitch() {
        return (1.0f + (random.nextFloat() - random.nextFloat()) * 0.2f) * 0.7f;
    }

    private void spawnExplosionParticles(float x, float y, float z, float power, ExplosionImpact impact) {
        if (!allFinite(x, y, z, power) || power <= 0.0f || impact == null) {
            return;
        }
        spawnParticle(explosionCenterParticleType(power), x, y, z,
                0.0f, 0.0f, 0.0f,
                explosionParticleScale(power),
                EXPLOSION_PARTICLE_LIFETIME_TICKS);

        int emitted = 0;
        for (BlockPos pos : impact.destroyedBlocks()) {
            if (emitted >= EXPLOSION_DEBRIS_PARTICLE_LIMIT) {
                break;
            }
            spawnExplosionDebrisParticles(pos, x, y, z, power);
            emitted++;
        }
    }

    private static float explosionParticleScale(float power) {
        if (!Float.isFinite(power)) {
            return EXPLOSION_PARTICLE_MIN_SCALE;
        }
        return Math.max(EXPLOSION_PARTICLE_MIN_SCALE, power * EXPLOSION_PARTICLE_SCALE_PER_POWER);
    }

    private static WorldParticle.Type explosionCenterParticleType(float power) {
        return power >= 2.0f ? WorldParticle.Type.HUGE_EXPLOSION : WorldParticle.Type.LARGE_EXPLOSION;
    }

    private void spawnExplosionDebrisParticles(BlockPos pos, float centerX, float centerY, float centerZ,
            float power) {
        float px = pos.x() + displayRandom.nextFloat();
        float py = pos.y() + displayRandom.nextFloat();
        float pz = pos.z() + displayRandom.nextFloat();
        Vector3f motion = explosionDebrisMotion(px, py, pz, centerX, centerY, centerZ, power);

        float flashScale = EXPLOSION_FLASH_PARTICLE_BASE_SCALE
                + displayRandom.nextFloat() * EXPLOSION_FLASH_PARTICLE_RANDOM_SCALE;
        int flashLifetime = EXPLOSION_FLASH_PARTICLE_BASE_LIFETIME_TICKS
                + displayRandom.nextInt(EXPLOSION_FLASH_PARTICLE_RANDOM_LIFETIME_TICKS);
        spawnParticle(WorldParticle.Type.EXPLODE,
                (px + centerX) * 0.5f, (py + centerY) * 0.5f, (pz + centerZ) * 0.5f,
                motion.x, motion.y, motion.z,
                flashScale, flashLifetime);

        float smokeScale = EXPLOSION_SMOKE_PARTICLE_BASE_SCALE
                + displayRandom.nextFloat() * EXPLOSION_SMOKE_PARTICLE_RANDOM_SCALE;
        int smokeLifetime = EXPLOSION_SMOKE_PARTICLE_BASE_LIFETIME_TICKS
                + displayRandom.nextInt(EXPLOSION_SMOKE_PARTICLE_RANDOM_LIFETIME_TICKS);
        spawnParticle(WorldParticle.Type.SMOKE, px, py, pz,
                motion.x, motion.y, motion.z,
                smokeScale, smokeLifetime);
    }

    private Vector3f explosionDebrisMotion(float px, float py, float pz,
            float centerX, float centerY, float centerZ, float power) {
        if (!allFinite(px, py, pz, centerX, centerY, centerZ, power) || power <= 0.0f) {
            return new Vector3f(0.0f, 0.0f, 0.0f);
        }
        Vector3f motion = new Vector3f(px - centerX, py - centerY, pz - centerZ);
        float distance = motion.length();
        if (!Float.isFinite(distance) || distance <= 0.0001f) {
            return new Vector3f(0.0f, 0.0f, 0.0f);
        }
        float speed = EXPLOSION_DEBRIS_VELOCITY_NUMERATOR
                / (distance / power + EXPLOSION_DEBRIS_DISTANCE_BIAS);
        speed *= displayRandom.nextFloat() * displayRandom.nextFloat()
                + EXPLOSION_DEBRIS_RANDOM_BIAS;
        return motion.div(distance).mul(speed);
    }

    static float explosionBlockDropChance(float power) {
        if (!Float.isFinite(power) || power <= 0.0f) {
            return 1.0f;
        }
        return Math.min(1.0f, 1.0f / power);
    }

    static boolean shouldDropBlockFromExplosion(Random random, float power) {
        if (random == null) {
            return false;
        }
        return random.nextFloat() <= explosionBlockDropChance(power);
    }

    private void damageEntitiesByExplosion(float x, float y, float z, float power, DamageSource source) {
        if (!allFinite(x, y, z, power) || power <= 0.0f) {
            return;
        }
        float entityRadius = power * EXPLOSION_ENTITY_RADIUS_MULTIPLIER;
        damageEntitiesByExplosion(entities, x, y, z, entityRadius, source);
        damageEntitiesByExplosion(entitiesToAdd, x, y, z, entityRadius, source);
    }

    private void damageEntitiesByExplosion(List<Entity> candidates, float x, float y, float z, float entityRadius,
            DamageSource source) {
        if (candidates == null || !allFinite(x, y, z, entityRadius) || entityRadius <= 0.0f) {
            return;
        }
        for (Entity entity : candidates) {
            if (entity == null) {
                continue;
            }
            if (entity.isRemoved()) {
                continue;
            }
            if (entity instanceof PaintingEntity painting) {
                damagePaintingByExplosion(painting, x, y, z, entityRadius);
                continue;
            }
            if (entity instanceof ExperienceOrbEntity orb) {
                damageExperienceOrbByExplosion(orb, x, y, z, entityRadius);
                continue;
            }
            if (entity instanceof BoatEntity boat) {
                damageBoatByExplosion(boat, x, y, z, entityRadius);
                continue;
            }
            if (entity instanceof MinecartEntity cart) {
                damageMinecartByExplosion(cart, x, y, z, entityRadius);
                continue;
            }
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            float distance = distanceFromExplosion(entity.getX(), entity.getY(), entity.getZ(), x, y, z);
            if (distance > entityRadius) {
                continue;
            }
            float exposure = ExplosionExposure.sample(this, x, y, z, living.getBoundingBox());
            float impact = explosionImpact(distance, entityRadius, exposure);
            float damage = explosionDamage(entityRadius, impact);
            if (damage <= 0.0f || !living.damage(damage, explosionDamageSource(source, x, y, z, 0.0f, 0.0f))) {
                continue;
            }
            ExplosionPush push = explosionPush(living.getX(), living.getY(), living.getZ(), x, y, z, impact);
            if (push.length() > 0.0f) {
                entity.addMotion(push.x(), push.y(), push.z());
            }
        }
    }

    private void damageBoatByExplosion(BoatEntity boat, float x, float y, float z, float entityRadius) {
        if (boat.isRemoved()) {
            return;
        }
        float distance = distanceFromExplosion(boat.getX(), boat.getY(), boat.getZ(), x, y, z);
        if (distance > entityRadius) {
            return;
        }
        AABB bounds = boat.getBoundingBox();
        float exposure = ExplosionExposure.sample(this, x, y, z, bounds);
        float impact = explosionImpact(distance, entityRadius, exposure);
        float damage = explosionDamage(entityRadius, impact);
        if (damage <= 0.0f || !boat.attack(damage, false) || boat.isRemoved()) {
            return;
        }
        ExplosionPush push = explosionPush(boat.getX(), boat.getY(), boat.getZ(), x, y, z, impact);
        if (push.length() > 0.0f) {
            boat.addMotion(push.x(), push.y(), push.z());
        }
    }

    private void damageMinecartByExplosion(MinecartEntity cart, float x, float y, float z, float entityRadius) {
        if (cart.isRemoved()) {
            return;
        }
        float distance = distanceFromExplosion(cart.getX(), cart.getY(), cart.getZ(), x, y, z);
        if (distance > entityRadius) {
            return;
        }
        AABB bounds = cart.getBoundingBox();
        float exposure = ExplosionExposure.sample(this, x, y, z, bounds);
        float impact = explosionImpact(distance, entityRadius, exposure);
        float damage = explosionDamage(entityRadius, impact);
        if (damage <= 0.0f || !cart.attack(damage, false) || cart.isRemoved()) {
            return;
        }
        ExplosionPush push = explosionPush(cart.getX(), cart.getY(), cart.getZ(), x, y, z, impact);
        if (push.length() > 0.0f) {
            cart.addMotion(push.x(), push.y(), push.z());
        }
    }

    private void damageExperienceOrbByExplosion(ExperienceOrbEntity orb, float x, float y, float z,
            float entityRadius) {
        if (orb.isRemoved()) {
            return;
        }
        float distance = distanceFromExplosion(orb.getX(), orb.getY(), orb.getZ(), x, y, z);
        if (distance > entityRadius) {
            return;
        }
        float exposure = ExplosionExposure.sample(this, x, y, z, orb.getBoundingBox());
        float impact = explosionImpact(distance, entityRadius, exposure);
        float damage = explosionDamage(entityRadius, impact);
        if (damage <= 0.0f) {
            return;
        }
        orb.damage(damage, DamageSource.point(DamageSource.Type.EXPLOSION, x, y, z, 0.0f, 0.0f));
        if (orb.isRemoved()) {
            return;
        }
        ExplosionPush push = explosionPush(orb.getX(), orb.getY(), orb.getZ(), x, y, z, impact);
        if (push.length() > 0.0f) {
            orb.addMotion(push.x(), push.y(), push.z());
        }
    }

    private void damageDroppedItemsByExplosion(float x, float y, float z, float power,
            List<DroppedItem> candidates) {
        if (candidates == null || !allFinite(x, y, z, power) || power <= 0.0f) {
            return;
        }
        float entityRadius = power * EXPLOSION_ENTITY_RADIUS_MULTIPLIER;
        for (DroppedItem item : candidates) {
            if (item == null || !droppedItems.contains(item)) {
                continue;
            }
            float distance = distanceFromExplosion(item.getX(), item.getY(), item.getZ(), x, y, z);
            if (distance > entityRadius) {
                continue;
            }
            AABB bounds = droppedItemBounds(item);
            float exposure = ExplosionExposure.sample(this, x, y, z, bounds);
            float impact = explosionImpact(distance, entityRadius, exposure);
            float damage = explosionDamage(entityRadius, impact);
            if (damage <= 0.0f || !item.damage(damage,
                    DamageSource.point(DamageSource.Type.EXPLOSION, x, y, z, 0.0f, 0.0f))) {
                continue;
            }
            if (item.isDestroyed()) {
                droppedItems.remove(item);
                continue;
            }
            ExplosionPush push = explosionPush(item.getX(), item.getY(), item.getZ(), x, y, z, impact);
            if (push.length() > 0.0f) {
                item.addVelocity(push.x(), push.y(), push.z());
            }
        }
    }

    private void damagePaintingsByExplosion(float x, float y, float z, float power) {
        if (!allFinite(x, y, z, power) || power <= 0.0f) {
            return;
        }
        float entityRadius = power * EXPLOSION_ENTITY_RADIUS_MULTIPLIER;
        damagePaintingsByExplosion(entities, x, y, z, entityRadius);
        damagePaintingsByExplosion(entitiesToAdd, x, y, z, entityRadius);
    }

    private void damagePaintingsByExplosion(List<Entity> candidates, float x, float y, float z, float entityRadius) {
        if (candidates == null || !allFinite(x, y, z, entityRadius) || entityRadius <= 0.0f) {
            return;
        }
        for (Entity entity : candidates) {
            if (entity instanceof PaintingEntity painting) {
                damagePaintingByExplosion(painting, x, y, z, entityRadius);
            }
        }
    }

    private void damagePaintingByExplosion(PaintingEntity painting, float x, float y, float z, float entityRadius) {
        if (painting.isRemoved()) {
            return;
        }
        float distance = distanceFromExplosion(painting.getX(), painting.getY(), painting.getZ(), x, y, z);
        if (distance > entityRadius) {
            return;
        }
        painting.breakAsItem(false);
    }

    private void damagePlayerByExplosion(float x, float y, float z, float power, DamageSource source) {
        if (player == null || !allFinite(x, y, z, power) || power <= 0.0f) {
            return;
        }
        float entityRadius = power * EXPLOSION_ENTITY_RADIUS_MULTIPLIER;
        float distance = distanceFromExplosion(player.getPosition().x, player.getPosition().y,
                player.getPosition().z, x, y, z);
        if (distance > entityRadius) {
            return;
        }
        float exposure = ExplosionExposure.sample(this, x, y, z, player.getBoundingBox());
        float impact = explosionImpact(distance, entityRadius, exposure);
        float damage = CombatRules.easyExplosionDamage(explosionDamage(entityRadius, impact));
        if (damage <= 0.0f) {
            return;
        }
        Vector3f playerPosition = player.getPosition();
        ExplosionPush push = explosionPush(playerPosition.x, playerPosition.y, playerPosition.z, x, y, z, impact);
        player.hurt(damage, explosionDamageSource(source, x, y, z,
                push.horizontalLength(), Math.max(0.0f, push.y())));
    }

    private static DamageSource explosionDamageSource(DamageSource source, float x, float y, float z,
            float horizontalKnockback, float verticalKnockback) {
        if (source == null) {
            return DamageSource.point(DamageSource.Type.EXPLOSION, x, y, z,
                    horizontalKnockback, verticalKnockback);
        }
        return new DamageSource(DamageSource.Type.EXPLOSION, source.entity(), true, x, y, z,
                horizontalKnockback, verticalKnockback, source.lootingLevel(), source.playerCredit(),
                source.playerId());
    }

    private static float distanceFromExplosion(float entityX, float entityY, float entityZ,
            float explosionX, float explosionY, float explosionZ) {
        if (!allFinite(entityX, entityY, entityZ, explosionX, explosionY, explosionZ)) {
            return Float.MAX_VALUE;
        }
        float dx = entityX - explosionX;
        float dy = entityY - explosionY;
        float dz = entityZ - explosionZ;
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        return Float.isFinite(distance) ? distance : Float.MAX_VALUE;
    }

    private static float explosionImpact(float distance, float entityRadius, float exposure) {
        if (!allFinite(distance, entityRadius, exposure) || entityRadius <= 0.0f) {
            return 0.0f;
        }
        return Math.max(0.0f, 1.0f - distance / entityRadius) * clamp01(exposure);
    }

    private static float explosionDamage(float entityRadius, float impact) {
        if (!allFinite(entityRadius, impact) || entityRadius <= 0.0f || impact <= 0.0f) {
            return 0.0f;
        }
        return ((impact * impact + impact) * 0.5f * EXPLOSION_ENTITY_DAMAGE_SCALE * entityRadius) + 1.0f;
    }

    private static ExplosionPush explosionPush(float entityX, float entityY, float entityZ,
            float explosionX, float explosionY, float explosionZ, float impact) {
        if (!allFinite(entityX, entityY, entityZ, explosionX, explosionY, explosionZ, impact) || impact <= 0.0f) {
            return ExplosionPush.NONE;
        }
        float dx = entityX - explosionX;
        float dy = entityY - explosionY;
        float dz = entityZ - explosionZ;
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (!Float.isFinite(length) || length <= 0.0001f) {
            return ExplosionPush.NONE;
        }
        return new ExplosionPush(dx / length * impact, dy / length * impact, dz / length * impact);
    }

    private record ExplosionPush(float x, float y, float z) {
        private static final ExplosionPush NONE = new ExplosionPush(0.0f, 0.0f, 0.0f);

        private ExplosionPush {
            x = finiteOrZero(x);
            y = finiteOrZero(y);
            z = finiteOrZero(z);
        }

        float length() {
            float length = (float) Math.sqrt(x * x + y * y + z * z);
            return Float.isFinite(length) ? length : 0.0f;
        }

        float horizontalLength() {
            float length = (float) Math.sqrt(x * x + z * z);
            return Float.isFinite(length) ? length : 0.0f;
        }
    }

    private record ExplosionImpact(Set<BlockPos> affectedBlocks, Set<BlockPos> destroyedBlocks) {
    }

    private ExplosionImpact collectExplosionImpact(float x, float y, float z, float power) {
        Set<BlockPos> affectedBlocks = new HashSet<>();
        Set<BlockPos> destroyedBlocks = new HashSet<>();
        if (!allFinite(x, y, z, power) || power <= 0.0f) {
            return new ExplosionImpact(affectedBlocks, destroyedBlocks);
        }
        for (int rayX = 0; rayX < EXPLOSION_RAY_GRID; rayX++) {
            for (int rayY = 0; rayY < EXPLOSION_RAY_GRID; rayY++) {
                for (int rayZ = 0; rayZ < EXPLOSION_RAY_GRID; rayZ++) {
                    if (!isExplosionBoundaryRay(rayX, rayY, rayZ)) {
                        continue;
                    }
                    traceExplosionRay(x, y, z, power, rayX, rayY, rayZ, affectedBlocks, destroyedBlocks);
                }
            }
        }
        return new ExplosionImpact(affectedBlocks, destroyedBlocks);
    }

    private static boolean isExplosionBoundaryRay(int rayX, int rayY, int rayZ) {
        int edge = EXPLOSION_RAY_GRID - 1;
        return rayX == 0 || rayX == edge || rayY == 0 || rayY == edge || rayZ == 0 || rayZ == edge;
    }

    private void traceExplosionRay(float x, float y, float z, float power,
            int rayX, int rayY, int rayZ, Set<BlockPos> affectedBlocks, Set<BlockPos> destroyedBlocks) {
        if (!allFinite(x, y, z, power) || power <= 0.0f
                || affectedBlocks == null || destroyedBlocks == null) {
            return;
        }
        double dx = rayX / (double) (EXPLOSION_RAY_GRID - 1) * 2.0 - 1.0;
        double dy = rayY / (double) (EXPLOSION_RAY_GRID - 1) * 2.0 - 1.0;
        double dz = rayZ / (double) (EXPLOSION_RAY_GRID - 1) * 2.0 - 1.0;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx /= length;
        dy /= length;
        dz /= length;

        float energy = power * (EXPLOSION_MIN_RAY_POWER + random.nextFloat() * EXPLOSION_RANDOM_RAY_POWER);
        double px = x;
        double py = y;
        double pz = z;
        while (energy > 0.0f) {
            int bx = (int) Math.floor(px);
            int by = (int) Math.floor(py);
            int bz = (int) Math.floor(pz);
            BlockType block = by < 0 || by >= Chunk.HEIGHT
                    ? BlockType.BEDROCK
                    : getBlockIfLoaded(bx, by, bz, BlockType.BEDROCK);

            if (block != BlockType.AIR) {
                energy -= (explosionResistance(block) + 0.3f) * EXPLOSION_RAY_STEP;
            }
            if (energy > 0.0f) {
                BlockPos pos = new BlockPos(bx, by, bz);
                affectedBlocks.add(pos);
                if (canExplosionDestroy(block)) {
                    destroyedBlocks.add(pos);
                }
            }

            px += dx * EXPLOSION_RAY_STEP;
            py += dy * EXPLOSION_RAY_STEP;
            pz += dz * EXPLOSION_RAY_STEP;
            energy -= EXPLOSION_RAY_AIR_ATTENUATION;
        }
    }

    void igniteExplosionFires(Set<BlockPos> affectedBlocks) {
        if (affectedBlocks == null) {
            return;
        }
        Random fireRandom = getRandom();
        for (BlockPos pos : affectedBlocks) {
            int x = pos.x();
            int y = pos.y();
            int z = pos.z();
            if (y < 1 || y >= Chunk.HEIGHT
                    || fireRandom.nextInt(3) != 0
                    || getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.AIR
                    || !canPlaceExplosionFireOn(getBlockIfLoaded(x, y - 1, z, BlockType.AIR))) {
                continue;
            }
            setBlock(x, y, z, BlockType.FIRE, 0);
        }
    }

    static boolean canPlaceExplosionFireOn(BlockType below) {
        return BlockShape.isOpaqueCube(below);
    }

    private void destroyBlockByExplosion(BlockPos pos, float explosionPower) {
        if (pos == null || !Float.isFinite(explosionPower)) {
            return;
        }
        BlockType block = getBlockIfLoaded(pos.x(), pos.y(), pos.z(), BlockType.AIR);
        if (!canExplosionDestroy(block)) {
            return;
        }
        if (block == BlockType.TNT) {
            primeTntFromExplosion(pos.x(), pos.y(), pos.z());
        } else if (shouldDropBlockFromExplosion(random, explosionPower)) {
            breakBlock(pos.x(), pos.y(), pos.z(), true);
        } else {
            TileEntity tile = getTileEntity(pos.x(), pos.y(), pos.z());
            ejectJukeboxRecordOnRemoval(tile);
            tile = removeTileEntity(pos.x(), pos.y(), pos.z());
            if (tile != null) {
                dropTileEntityContents(tile, pos.x(), pos.y(), pos.z());
            }
            setBlock(pos.x(), pos.y(), pos.z(), BlockType.AIR);
        }
    }

    private void ejectJukeboxRecordOnRemoval(TileEntity tile) {
        if (tile instanceof JukeboxTileEntity jukebox) {
            jukebox.ejectRecordOnRemoval(this);
        }
    }

    private static boolean canExplosionDestroy(BlockType block) {
        return block != BlockType.AIR
                && block.getHardness() >= 0.0f
                && block != BlockType.OBSIDIAN
                && !block.isFluid()
                && block != BlockType.PORTAL
                && block != BlockType.END_PORTAL;
    }

    private static float explosionResistance(BlockType block) {
        if (block == null) {
            return 0.0f;
        }
        return Math.max(0.0f, finiteOrZero(block.getExplosionResistance()));
    }

    private static boolean allFinite(float... values) {
        for (float value : values) {
            if (!Float.isFinite(value)) {
                return false;
            }
        }
        return true;
    }

    private static float finiteOrZero(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }

    private void primeTntFromExplosion(int x, int y, int z) {
        int baseFuse = Math.max(1, RedstoneEngine.TNT_FUSE_TICKS / 8);
        int randomFuse = Math.max(1, RedstoneEngine.TNT_FUSE_TICKS / 4);
        primeTnt(x, y, z, baseFuse + random.nextInt(randomFuse), false);
    }

    @FunctionalInterface
    public interface BlockChangeListener {
        void onBlockChanged(int x, int y, int z, BlockType previous, int previousMetadata,
                BlockType current, int currentMetadata);
    }

    public interface DamageEventListener {
        default void onExplosion(float x, float y, float z, float power) {
        }

        default void onLightning(float x, float y, float z) {
        }
    }

    public interface ProjectilePlayerInteractionHandler {
        ProjectilePlayerHit findProjectilePlayerHit(Vector3f origin, Vector3f direction, float maxDistance);

        default ProjectilePlayerHit findProjectilePlayerHit(Vector3f origin, Vector3f direction, float maxDistance,
                String ignoredPlayerId) {
            return findProjectilePlayerHit(origin, direction, maxDistance);
        }

        boolean damageProjectilePlayer(ProjectilePlayerHit hit, ProjectilePlayerDamage damage);

        void splashPotionPlayers(float x, float y, float z, PotionData potion, String directHitPlayerId);
    }

    public interface RemotePlayerInteractionHandler {
        RemotePlayerTarget targetById(String playerId);

        default RemotePlayerTarget viewById(String playerId) {
            return targetById(playerId);
        }

        RemotePlayerTarget nearestTarget(float sourceX, float sourceY, float sourceZ,
                float range, boolean requireSight);

        default List<RemotePlayerTarget> targets(float sourceX, float sourceY, float sourceZ,
                float range, boolean requireSight) {
            RemotePlayerTarget target = nearestTarget(sourceX, sourceY, sourceZ, range, requireSight);
            return target == null ? List.of() : List.of(target);
        }

        default List<RemotePlayerTarget> views(float sourceX, float sourceY, float sourceZ,
                float range, boolean requireSight) {
            return targets(sourceX, sourceY, sourceZ, range, requireSight);
        }

        boolean damageTarget(String playerId, RemotePlayerDamage damage);

        default boolean applyStatusEffect(String playerId, StatusEffectInstance effect) {
            return false;
        }

        default boolean pullTarget(String playerId, float motionX, float motionY, float motionZ) {
            return false;
        }
    }

    public record RemotePlayerTarget(String playerId, float x, float y, float z, float eyeY,
            float height, float distance, float yaw, float pitch, boolean wearingPumpkinHelmet,
            ItemType heldItem, String username) {
        public RemotePlayerTarget {
            playerId = playerId == null ? "" : playerId;
            username = username == null ? "" : username.trim();
            height = height <= 0.0f ? 1.8f : height;
            distance = Math.max(0.0f, distance);
            yaw = Float.isFinite(yaw) ? yaw : 0.0f;
            pitch = Float.isFinite(pitch) ? Math.max(-90.0f, Math.min(90.0f, pitch)) : 0.0f;
        }

        public RemotePlayerTarget(String playerId, float x, float y, float z, float eyeY,
                float height, float distance) {
            this(playerId, x, y, z, eyeY, height, distance, 0.0f, 0.0f, false, null, "");
        }

        public boolean valid() {
            return !playerId.isBlank();
        }
    }

    public record RemotePlayerDamage(float amount, String damageType,
            float sourceX, float sourceY, float sourceZ,
            float horizontalKnockback, float verticalKnockback, int fireTicks) {
        public RemotePlayerDamage {
            amount = Math.max(0.0f, amount);
            damageType = damageType == null || damageType.isBlank() ? "generic" : damageType;
            horizontalKnockback = Math.max(0.0f, horizontalKnockback);
            verticalKnockback = Math.max(0.0f, verticalKnockback);
            fireTicks = Math.max(0, fireTicks);
        }
    }

    public record ProjectilePlayerHit(String playerId, Vector3f hitPoint, float distance) {
        public ProjectilePlayerHit {
            playerId = playerId == null ? "" : playerId;
            hitPoint = hitPoint == null ? null : new Vector3f(hitPoint);
        }

        public static ProjectilePlayerHit miss() {
            return new ProjectilePlayerHit("", null, Float.MAX_VALUE);
        }

        public boolean hit() {
            return !playerId.isBlank() && hitPoint != null && distance >= 0.0f && distance < Float.MAX_VALUE;
        }
    }

    public record ProjectilePlayerDamage(float amount, String damageType,
            float sourceX, float sourceY, float sourceZ,
            float horizontalKnockback, float verticalKnockback, int fireTicks, String sourcePlayerId) {
        public ProjectilePlayerDamage {
            damageType = damageType == null || damageType.isBlank() ? "generic" : damageType;
            amount = Math.max(0.0f, amount);
            horizontalKnockback = Math.max(0.0f, horizontalKnockback);
            verticalKnockback = Math.max(0.0f, verticalKnockback);
            fireTicks = Math.max(0, fireTicks);
            sourcePlayerId = sourcePlayerId == null ? "" : sourcePlayerId.trim();
        }

        public ProjectilePlayerDamage(float amount, String damageType,
                float sourceX, float sourceY, float sourceZ,
                float horizontalKnockback, float verticalKnockback, int fireTicks) {
            this(amount, damageType, sourceX, sourceY, sourceZ,
                    horizontalKnockback, verticalKnockback, fireTicks, "");
        }
    }

    /**
     * Biome types for terrain generation.
     */
    private enum BiomeType {
        PLAINS,
        FOREST,
        HILLS,
        MOUNTAINS
    }
}

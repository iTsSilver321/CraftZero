package com.craftzero.world;

import com.craftzero.entity.mob.*;
import com.craftzero.main.Player;

import java.util.List;
import java.util.Random;

/**
 * Handles mob spawning logic.
 * Called from World.update() every few ticks.
 */
public class MobSpawner {

    private final World world;
    private final Random random;

    // Spawn timing
    private int spawnTick = 0;
    private static final int SPAWN_INTERVAL = 20; // Every second

    // Mob caps
    private static final int MAX_HOSTILE = 70;
    private static final int MAX_PASSIVE = 10;
    private static final int MAX_WATER = 15;

    private static final List<SpawnRule> OVERWORLD_HOSTILES = List.of(
            new SpawnRule(MobDefinition.ZOMBIE, 10, 1, Chunk.HEIGHT - 3, false),
            new SpawnRule(MobDefinition.SKELETON, 10, 1, Chunk.HEIGHT - 3, false),
            new SpawnRule(MobDefinition.CREEPER, 10, 1, Chunk.HEIGHT - 3, false),
            new SpawnRule(MobDefinition.SPIDER, 10, 1, Chunk.HEIGHT - 3, false),
            new SpawnRule(MobDefinition.ENDERMAN, 2, 1, Chunk.HEIGHT - 3, false),
            new SpawnRule(MobDefinition.SLIME, 4, 1, 39, false));
    private static final List<SpawnRule> NETHER_HOSTILES = List.of(
            new SpawnRule(MobDefinition.ZOMBIE_PIGMAN, 20, 1, Chunk.HEIGHT - 3, false),
            new SpawnRule(MobDefinition.GHAST, 4, 32, Chunk.HEIGHT - 10, false),
            new SpawnRule(MobDefinition.BLAZE, 3, 1, Chunk.HEIGHT - 3, false),
            new SpawnRule(MobDefinition.MAGMA_CUBE, 4, 1, Chunk.HEIGHT - 3, false));

    // Spawn distances
    private static final float MIN_SPAWN_DISTANCE = 24.0f;
    private static final float MAX_SPAWN_DISTANCE = 128.0f;

    public MobSpawner(World world) {
        this.world = world;
        this.random = new Random();
    }

    /**
     * Get effective light level for mob spawning (considers time of day).
     * Sky light is scaled by sun brightness - at night, exterior areas become dark
     * enough for spawns.
     */
    private int getEffectiveLightForSpawning(int x, int y, int z) {
        int skyLight = world.getSkyLight(x, y, z);

        // Adjust sky light based on time of day
        DayCycleManager cycle = world.getDayCycleManager();
        if (cycle != null) {
            float brightness = cycle.getSunBrightness();
            // Scale sky light by sun brightness (1.0 = day, 0.3 = night)
            skyLight = (int) (skyLight * brightness);
        }

        return skyLight;
    }

    /**
     * Attempt to spawn mobs. Called every tick.
     */
    public void tick() {
        spawnTick++;
        if (spawnTick < SPAWN_INTERVAL) {
            return;
        }
        spawnTick = 0;

        Player player = world.getPlayer();
        if (player == null)
            return;

        float playerX = player.getPosition().x;
        float playerY = player.getPosition().y;
        float playerZ = player.getPosition().z;

        // Count current mobs
        int hostileCount = 0;
        int passiveCount = 0;
        int waterCount = 0;
        for (var entity : world.getEntities()) {
            if (entity instanceof Mob mob) {
                if (mob.getDefinition() != null && mob.getDefinition().category() == MobDefinition.MobCategory.WATER_CREATURE) {
                    waterCount++;
                } else if (mob.isHostile()) {
                    hostileCount++;
                } else {
                    passiveCount++;
                }
            }
        }

        // Try to spawn hostile mobs
        if (!player.isCreative() && player.getDifficulty().allowsHostileSpawns() && hostileCount < MAX_HOSTILE) {
            trySpawnHostile(playerX, playerY, playerZ);
        }

        // Try to spawn passive mobs (less frequently)
        if (passiveCount < MAX_PASSIVE && random.nextFloat() < 0.2f) {
            trySpawnPassive(playerX, playerY, playerZ);
        }

        if (world.getDimension() == Dimension.OVERWORLD && waterCount < MAX_WATER && random.nextFloat() < 0.15f) {
            trySpawnWater(playerX, playerY, playerZ);
        }
    }

    private void trySpawnHostile(float playerX, float playerY, float playerZ) {
        // Pick random position
        float angle = random.nextFloat() * (float) Math.PI * 2;
        float distance = MIN_SPAWN_DISTANCE + random.nextFloat() * (MAX_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE);

        float spawnX = playerX + (float) Math.cos(angle) * distance;
        float spawnZ = playerZ + (float) Math.sin(angle) * distance;
        int blockX = (int) Math.floor(spawnX);
        int blockZ = (int) Math.floor(spawnZ);
        if (!world.isChunkGeneratedForBlock(blockX, blockZ)) {
            return;
        }

        // Find valid Y (solid block with air above)
        int spawnY = findSpawnY(blockX, (int) playerY, blockZ);
        if (spawnY < 0)
            return;

        SpawnRule rule = chooseHostileRule(blockX, spawnY, blockZ);
        if (rule == null || !canSpawnHostileAt(blockX, spawnY, blockZ))
            return;

        Mob mob = createMobForRule(rule, blockX, spawnY, blockZ);
        if (mob == null) {
            return;
        }
        mob.setPosition(spawnX + 0.5f, spawnY + 1, spawnZ + 0.5f);

        world.spawnEntity(mob);
    }

    private void trySpawnWater(float playerX, float playerY, float playerZ) {
        float angle = random.nextFloat() * (float) Math.PI * 2;
        float distance = MIN_SPAWN_DISTANCE + random.nextFloat() * 40.0f;
        int blockX = (int) Math.floor(playerX + (float) Math.cos(angle) * distance);
        int blockZ = (int) Math.floor(playerZ + (float) Math.sin(angle) * distance);
        if (!world.isChunkGeneratedForBlock(blockX, blockZ)) {
            return;
        }
        for (int y = Math.min(62, (int) playerY + 16); y >= 45; y--) {
            if (world.getBlockIfLoaded(blockX, y, blockZ, BlockType.AIR).isWater()
                    && world.getBlockIfLoaded(blockX, y + 1, blockZ, BlockType.AIR).isWater()) {
                Mob squid = MobFactory.create(MobDefinition.SQUID);
                if (squid != null) {
                    squid.setPosition(blockX + 0.5f, y, blockZ + 0.5f);
                    world.spawnEntity(squid);
                }
                return;
            }
        }
    }

    private void trySpawnPassive(float playerX, float playerY, float playerZ) {
        // Pick random position (closer to player for passive mobs)
        float angle = random.nextFloat() * (float) Math.PI * 2;
        float distance = MIN_SPAWN_DISTANCE + random.nextFloat() * 40.0f;

        float spawnX = playerX + (float) Math.cos(angle) * distance;
        float spawnZ = playerZ + (float) Math.sin(angle) * distance;
        int blockX = (int) Math.floor(spawnX);
        int blockZ = (int) Math.floor(spawnZ);
        if (!world.isChunkGeneratedForBlock(blockX, blockZ)) {
            return;
        }

        // Find valid Y
        int spawnY = findSpawnY(blockX, (int) playerY, blockZ);
        if (spawnY < 0)
            return;

        if (!canSpawnPassiveAt(blockX, spawnY, blockZ))
            return;

        // Pick mob type
        Mob mob = createRandomPassiveMob();
        mob.setPosition(spawnX + 0.5f, spawnY + 1, spawnZ + 0.5f);

        world.spawnEntity(mob);
    }

    private int findSpawnY(int x, int startY, int z) {
        // Search up and down from player Y level
        for (int dy = 0; dy < 30; dy++) {
            int y = startY + dy;
            if (isValidSpawnY(x, y, z))
                return y;

            y = startY - dy;
            if (y > 0 && isValidSpawnY(x, y, z))
                return y;
        }
        return -1;
    }

    private boolean isValidSpawnY(int x, int y, int z) {
        if (!world.isChunkGeneratedForBlock(x, z)) {
            return false;
        }
        BlockType below = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
        BlockType atFeet = world.getBlockIfLoaded(x, y + 1, z, BlockType.STONE);
        BlockType atHead = world.getBlockIfLoaded(x, y + 2, z, BlockType.STONE);

        return below.isSolid() && !atFeet.isSolid() && !atHead.isSolid();
    }

    boolean canSpawnHostileAt(int x, int groundY, int z) {
        if (!world.isChunkGeneratedForBlock(x, z) || !hasClearSpawnSpace(x, groundY, z)) {
            return false;
        }
        int light = getEffectiveLightForSpawning(x, groundY + 1, z);
        return light <= 7;
    }

    boolean canSpawnPassiveAt(int x, int groundY, int z) {
        if (!world.isChunkGeneratedForBlock(x, z) || !hasClearSpawnSpace(x, groundY, z)) {
            return false;
        }
        int light = world.getSkyLight(x, groundY + 1, z);
        if (light < 9) {
            return false;
        }
        return world.getBlockIfLoaded(x, groundY, z, BlockType.AIR) == BlockType.GRASS;
    }

    private boolean hasClearSpawnSpace(int x, int groundY, int z) {
        BlockType below = world.getBlockIfLoaded(x, groundY, z, BlockType.AIR);
        if (!below.isSolid()) {
            return false;
        }

        BlockType atFeet = world.getBlockIfLoaded(x, groundY + 1, z, BlockType.STONE);
        BlockType atHead = world.getBlockIfLoaded(x, groundY + 2, z, BlockType.STONE);
        return !atFeet.isSolid() && !atHead.isSolid();
    }

    private Mob createRandomHostileMob() {
        SpawnRule rule = chooseWeighted(world.getDimension() == Dimension.NETHER ? NETHER_HOSTILES : OVERWORLD_HOSTILES);
        return rule == null ? new Zombie() : MobFactory.create(rule.definition());
    }

    private Mob createRandomPassiveMob() {
        int type = random.nextInt(4);
        return switch (type) {
            case 0 -> new Pig();
            case 1 -> new Cow();
            case 2 -> new Sheep();
            case 3 -> new Chicken();
            default -> new Pig();
        };
    }

    private SpawnRule chooseHostileRule(int x, int y, int z) {
        List<SpawnRule> rules = world.getDimension() == Dimension.NETHER ? NETHER_HOSTILES : OVERWORLD_HOSTILES;
        for (int attempts = 0; attempts < 8; attempts++) {
            SpawnRule rule = chooseWeighted(rules);
            if (rule != null && rule.matches(world, x, y, z)) {
                if (rule.definition() == MobDefinition.SLIME && !isSlimeChunk(x, z)) {
                    continue;
                }
                return rule;
            }
        }
        return null;
    }

    private Mob createMobForRule(SpawnRule rule, int x, int y, int z) {
        Mob mob = MobFactory.create(rule.definition());
        if (mob instanceof Slime && rule.definition() == MobDefinition.SLIME) {
            int size = random.nextInt(3) == 0 ? 4 : random.nextBoolean() ? 2 : 1;
            mob = new Slime(size);
        } else if (mob instanceof MagmaCube) {
            int size = random.nextInt(3) == 0 ? 4 : random.nextBoolean() ? 2 : 1;
            mob = new MagmaCube(size);
        }
        return mob;
    }

    private SpawnRule chooseWeighted(List<SpawnRule> rules) {
        int total = 0;
        for (SpawnRule rule : rules) {
            total += Math.max(0, rule.weight());
        }
        if (total <= 0) {
            return null;
        }
        int roll = random.nextInt(total);
        for (SpawnRule rule : rules) {
            roll -= Math.max(0, rule.weight());
            if (roll < 0) {
                return rule;
            }
        }
        return rules.get(0);
    }

    boolean isSlimeChunk(int blockX, int blockZ) {
        int chunkX = Math.floorDiv(blockX, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(blockZ, Chunk.DEPTH);
        Random chunkRandom = new Random(world.getSeed()
                + (long) (chunkX * chunkX * 0x4c1906)
                + (long) (chunkX * 0x5ac0db)
                + (long) (chunkZ * chunkZ) * 0x4307a7L
                + (long) (chunkZ * 0x5f24f)
                ^ 0x3ad8025fL);
        return chunkRandom.nextInt(10) == 0;
    }
}

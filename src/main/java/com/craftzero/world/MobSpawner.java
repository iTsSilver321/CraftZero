package com.craftzero.world;

import com.craftzero.entity.mob.*;
import com.craftzero.main.Player;
import com.craftzero.physics.AABB;
import com.craftzero.world.tile.BlockPos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Handles mob spawning logic.
 * Called from the fixed world tick.
 */
public class MobSpawner {

    private final World world;
    private final Random random;
    private boolean spawnAnimals = true;
    private boolean spawnMonsters = true;

    // Mob caps
    private static final int MAX_HOSTILE = 70;
    private static final int MAX_PASSIVE = 10;
    private static final int MAX_WATER = 15;
    private static final int SLIME_MAX_GROUND_Y = 38;
    private static final int GHAST_SPAWN_CHANCE = 20;

    private static final List<SpawnRule> OVERWORLD_HOSTILES = List.of(
            new SpawnRule(MobDefinition.ZOMBIE, 10, 1, Chunk.HEIGHT - 3, false),
            new SpawnRule(MobDefinition.SKELETON, 10, 1, Chunk.HEIGHT - 3, false),
            new SpawnRule(MobDefinition.CREEPER, 10, 1, Chunk.HEIGHT - 3, false),
            new SpawnRule(MobDefinition.SPIDER, 10, 1, Chunk.HEIGHT - 3, false),
            new SpawnRule(MobDefinition.ENDERMAN, 2, 1, Chunk.HEIGHT - 3, false),
            new SpawnRule(MobDefinition.SLIME, 4, 1, SLIME_MAX_GROUND_Y, false));
    private static final List<SpawnRule> NETHER_HOSTILES = List.of(
            new SpawnRule(MobDefinition.ZOMBIE_PIGMAN, 20, 1, Chunk.HEIGHT - 3, false),
            new SpawnRule(MobDefinition.GHAST, 4, 32, Chunk.HEIGHT - 10, false),
            new SpawnRule(MobDefinition.MAGMA_CUBE, 4, 1, Chunk.HEIGHT - 3, false));
    private static final List<SpawnRule> NETHER_FORTRESS_HOSTILES = List.of(
            new SpawnRule(MobDefinition.BLAZE, 10, 1, Chunk.HEIGHT - 3, false),
            new SpawnRule(MobDefinition.ZOMBIE_PIGMAN, 10, 1, Chunk.HEIGHT - 3, false),
            new SpawnRule(MobDefinition.MAGMA_CUBE, 3, 1, Chunk.HEIGHT - 3, false));
    private static final List<SpawnRule> END_HOSTILES = List.of(
            new SpawnRule(MobDefinition.ENDERMAN, 10, 1, Chunk.HEIGHT - 3, false));
    private static final List<SpawnRule> NO_SPAWNS = List.of();
    private static final List<SpawnRule> STANDARD_PASSIVES = List.of(
            new SpawnRule(MobDefinition.SHEEP, 12, 1, Chunk.HEIGHT - 3, false),
            new SpawnRule(MobDefinition.PIG, 10, 1, Chunk.HEIGHT - 3, false),
            new SpawnRule(MobDefinition.CHICKEN, 10, 1, Chunk.HEIGHT - 3, false),
            new SpawnRule(MobDefinition.COW, 8, 1, Chunk.HEIGHT - 3, false));
    private static final List<SpawnRule> FOREST_PASSIVES = List.of(
            new SpawnRule(MobDefinition.SHEEP, 12, 1, Chunk.HEIGHT - 3, false),
            new SpawnRule(MobDefinition.PIG, 10, 1, Chunk.HEIGHT - 3, false),
            new SpawnRule(MobDefinition.CHICKEN, 10, 1, Chunk.HEIGHT - 3, false),
            new SpawnRule(MobDefinition.COW, 8, 1, Chunk.HEIGHT - 3, false),
            new SpawnRule(MobDefinition.WOLF, 5, 1, Chunk.HEIGHT - 3, false));
    private static final List<SpawnRule> TAIGA_PASSIVES = List.of(
            new SpawnRule(MobDefinition.SHEEP, 12, 1, Chunk.HEIGHT - 3, false),
            new SpawnRule(MobDefinition.PIG, 10, 1, Chunk.HEIGHT - 3, false),
            new SpawnRule(MobDefinition.CHICKEN, 10, 1, Chunk.HEIGHT - 3, false),
            new SpawnRule(MobDefinition.COW, 8, 1, Chunk.HEIGHT - 3, false),
            new SpawnRule(MobDefinition.WOLF, 8, 1, Chunk.HEIGHT - 3, false));
    private static final List<SpawnRule> MUSHROOM_PASSIVES = List.of(
            new SpawnRule(MobDefinition.MOOSHROOM, 8, 1, Chunk.HEIGHT - 3, false));
    private static final List<SpawnRule> OVERWORLD_WATER_CREATURES = List.of(
            new SpawnRule(MobDefinition.SQUID, 10, 45, 62, true));

    // Spawn distances
    private static final float MIN_SPAWN_DISTANCE = 24.0f;
    private static final int PLAYER_CHUNK_SPAWN_RADIUS = 8;
    private static final int PLAYER_CHUNK_SPAWN_DIAMETER = PLAYER_CHUNK_SPAWN_RADIUS * 2 + 1;
    private static final int PLAYER_CHUNK_SPAWN_AREA =
            PLAYER_CHUNK_SPAWN_DIAMETER * PLAYER_CHUNK_SPAWN_DIAMETER;
    private static final int MOB_CAP_CHUNK_DIVISOR = 256;
    private static final int NATURAL_GROUP_ATTEMPTS_PER_CHUNK = 3;
    private static final int NATURAL_GROUP_MEMBER_ATTEMPTS = 4;
    private static final int NATURAL_GROUP_JITTER = 6;
    private static final int HOSTILE_SKY_LIGHT_RANDOM_BOUND = 32;
    private static final int HOSTILE_BLOCK_LIGHT_RANDOM_BOUND = 8;
    private static final int SPIDER_JOCKEY_CHANCE = 100;

    public MobSpawner(World world) {
        this(world, world.getRandom());
    }

    MobSpawner(World world, Random random) {
        this.world = world;
        this.random = random == null ? world.getRandom() : random;
    }

    public void configureServerSpawnRules(boolean spawnAnimals, boolean spawnMonsters) {
        this.spawnAnimals = spawnAnimals;
        this.spawnMonsters = spawnMonsters;
    }

    /**
     * Get effective light level for mob spawning (considers time of day).
     * Sky light is scaled by sun brightness - at night, exterior areas become dark
     * enough for spawns. Block light still applies at full strength.
     */
    private int getEffectiveLightForSpawning(int x, int y, int z) {
        if (world.getDimension() != Dimension.OVERWORLD) {
            return world.getBlockLight(x, y, z);
        }
        int skyLight = world.getSkyLight(x, y, z);

        // Adjust sky light based on time of day
        DayCycleManager cycle = world.getDayCycleManager();
        if (cycle != null) {
            float brightness = cycle.getSunBrightness();
            // Scale sky light by sun brightness (1.0 = day, 0.3 = night)
            skyLight = (int) (skyLight * brightness);
        }

        int blockLight = world.getBlockLight(x, y, z);
        return Math.max(skyLight, blockLight);
    }

    /**
     * Attempt to spawn mobs. Called from the fixed world tick.
     */
    public void tick() {
        Player player = world.getPlayer();
        if (player == null) {
            return;
        }

        float playerX = player.getPosition().x;
        float playerY = player.getPosition().y;
        float playerZ = player.getPosition().z;
        List<SpawnAnchor> passiveAnchors = livePlayerSpawnAnchors(playerX, playerY, playerZ, false);
        List<SpawnAnchor> hostileAnchors = livePlayerSpawnAnchors(playerX, playerY, playerZ, true);
        List<SpawnChunkCandidate> passiveChunks = eligibleSpawnChunks(passiveAnchors);
        List<SpawnChunkCandidate> hostileChunks = eligibleSpawnChunks(hostileAnchors);

        // Count active plus queued mobs so same-tick additions still reserve cap space.
        int hostileCount = 0;
        int passiveCount = 0;
        int waterCount = 0;
        for (var entity : world.getEntitiesIncludingPending()) {
            if (entity instanceof Mob mob) {
                MobDefinition definition = mob.getDefinition();
                if (definition == null) {
                    if (mob.isHostile()) {
                        hostileCount++;
                    } else {
                        passiveCount++;
                    }
                    continue;
                }
                switch (definition.category()) {
                    case MONSTER -> hostileCount++;
                    case CREATURE -> passiveCount++;
                    case WATER_CREATURE -> waterCount++;
                    default -> {
                        // Ambient, utility, and boss mobs are not part of the
                        // Release-era natural spawning cap categories.
                    }
                }
            }
        }

        int hostileCap = releaseOneMobCap(MAX_HOSTILE, hostileChunks.size());
        int passiveCap = releaseOneMobCap(MAX_PASSIVE, passiveChunks.size());
        int waterCap = releaseOneMobCap(MAX_WATER, passiveChunks.size());

        // Try to spawn hostile mobs
        if (!hostileAnchors.isEmpty()
                && spawnMonsters
                && hostileCount < hostileCap) {
            trySpawnHostile(hostileAnchors, hostileChunks, hostileCap - hostileCount);
        }

        if (!passiveAnchors.isEmpty() && spawnAnimals && passiveCount < passiveCap) {
            trySpawnPassive(passiveAnchors, passiveChunks, passiveCap - passiveCount);
        }

        if (!passiveAnchors.isEmpty()
                && spawnAnimals
                && world.getDimension() == Dimension.OVERWORLD
                && waterCount < waterCap) {
            trySpawnWater(passiveAnchors, passiveChunks, waterCap - waterCount);
        }
    }

    static int releaseOneMobCap(int baseCap) {
        return releaseOneMobCap(baseCap, PLAYER_CHUNK_SPAWN_AREA);
    }

    static int releaseOneMobCap(int baseCap, int eligibleChunkCount) {
        return Math.max(0, baseCap) * Math.max(0, eligibleChunkCount) / MOB_CAP_CHUNK_DIVISOR;
    }

    private List<SpawnAnchor> livePlayerSpawnAnchors(float playerX, float playerY, float playerZ,
            boolean hostileOnly) {
        ArrayList<SpawnAnchor> anchors = new ArrayList<>();
        Player player = world.getPlayer();
        if (player != null && (!hostileOnly
                || (!player.isCreative() && player.getDifficulty().allowsHostileSpawns()))) {
            anchors.add(new SpawnAnchor(playerX, playerY, playerZ));
        }
        List<World.RemotePlayerTarget> remotes = hostileOnly
                ? world.remotePlayerTargets(playerX, playerY, playerZ, Float.MAX_VALUE, false)
                : world.remotePlayerViews(playerX, playerY, playerZ, Float.MAX_VALUE, false);
        for (World.RemotePlayerTarget remote : remotes) {
            if (remote != null && remote.valid() && allFinite(remote.x(), remote.y(), remote.z())) {
                anchors.add(new SpawnAnchor(remote.x(), remote.y(), remote.z()));
            }
        }
        return anchors;
    }

    private List<SpawnChunkCandidate> eligibleSpawnChunks(List<SpawnAnchor> anchors) {
        if (anchors == null || anchors.isEmpty()) {
            return List.of();
        }
        Map<Long, SpawnChunkCandidate> chunks = new LinkedHashMap<>();
        for (SpawnAnchor anchor : anchors) {
            int playerChunkX = Math.floorDiv((int) Math.floor(anchor.x()), Chunk.WIDTH);
            int playerChunkZ = Math.floorDiv((int) Math.floor(anchor.z()), Chunk.DEPTH);
            for (int localZ = -PLAYER_CHUNK_SPAWN_RADIUS; localZ <= PLAYER_CHUNK_SPAWN_RADIUS; localZ++) {
                for (int localX = -PLAYER_CHUNK_SPAWN_RADIUS; localX <= PLAYER_CHUNK_SPAWN_RADIUS; localX++) {
                    int chunkX = playerChunkX + localX;
                    int chunkZ = playerChunkZ + localZ;
                    boolean border = Math.abs(localX) == PLAYER_CHUNK_SPAWN_RADIUS
                            || Math.abs(localZ) == PLAYER_CHUNK_SPAWN_RADIUS;
                    long key = chunkKey(chunkX, chunkZ);
                    SpawnChunkCandidate existing = chunks.get(key);
                    if (existing == null) {
                        chunks.put(key, new SpawnChunkCandidate(chunkX, chunkZ, border, anchor.y()));
                    } else if (existing.border() && !border) {
                        chunks.put(key, new SpawnChunkCandidate(chunkX, chunkZ, false, anchor.y()));
                    }
                }
            }
        }
        return new ArrayList<>(chunks.values());
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
    }

    private int trySpawnHostile(List<SpawnAnchor> anchors, List<SpawnChunkCandidate> chunks, int capRemaining) {
        return trySpawnGroundAroundPlayers(anchors, chunks, capRemaining, SpawnKind.HOSTILE);
    }

    private int trySpawnWater(List<SpawnAnchor> anchors, List<SpawnChunkCandidate> chunks, int capRemaining) {
        if (capRemaining <= 0) {
            return 0;
        }
        int spawned = 0;
        if (anchors == null || anchors.isEmpty() || chunks == null || chunks.isEmpty()) {
            return 0;
        }
        int start = random.nextInt(chunks.size());
        for (int attempt = 0; attempt < chunks.size() && spawned < capRemaining; attempt++) {
            SpawnChunkCandidate chunk = chunks.get((start + attempt) % chunks.size());
            if (chunk.border()) {
                continue;
            }
            int blockX = chunk.chunkX() * Chunk.WIDTH + random.nextInt(Chunk.WIDTH);
            int blockZ = chunk.chunkZ() * Chunk.DEPTH + random.nextInt(Chunk.DEPTH);
            if (!world.isChunkGeneratedForBlock(blockX, blockZ)) {
                continue;
            }
            int y = findWaterSpawnY(blockX, Math.min(62, (int) chunk.referenceY() + 16), blockZ);
            if (y < 0) {
                continue;
            }
            for (int group = 0; group < NATURAL_GROUP_ATTEMPTS_PER_CHUNK && spawned < capRemaining; group++) {
                spawned += trySpawnWaterGroup(blockX, y, blockZ, anchors,
                        capRemaining - spawned);
            }
        }
        return spawned;
    }

    private int trySpawnPassive(List<SpawnAnchor> anchors, List<SpawnChunkCandidate> chunks, int capRemaining) {
        return trySpawnGroundAroundPlayers(anchors, chunks, capRemaining, SpawnKind.PASSIVE);
    }

    private int trySpawnGroundAroundPlayers(List<SpawnAnchor> anchors, List<SpawnChunkCandidate> chunks,
            int capRemaining, SpawnKind kind) {
        if (capRemaining <= 0) {
            return 0;
        }
        int spawned = 0;
        if (anchors == null || anchors.isEmpty() || chunks == null || chunks.isEmpty()) {
            return 0;
        }
        int start = random.nextInt(chunks.size());
        for (int attempt = 0; attempt < chunks.size() && spawned < capRemaining; attempt++) {
            SpawnChunkCandidate chunk = chunks.get((start + attempt) % chunks.size());
            if (chunk.border()) {
                continue;
            }
            int blockX = chunk.chunkX() * Chunk.WIDTH + random.nextInt(Chunk.WIDTH);
            int blockZ = chunk.chunkZ() * Chunk.DEPTH + random.nextInt(Chunk.DEPTH);
            if (!world.isChunkGeneratedForBlock(blockX, blockZ)) {
                continue;
            }
            int spawnY = randomNaturalGroundY(blockX, blockZ);
            if (spawnY < 0) {
                continue;
            }
            for (int group = 0; group < NATURAL_GROUP_ATTEMPTS_PER_CHUNK && spawned < capRemaining; group++) {
                spawned += trySpawnGroundGroup(kind, blockX, spawnY, blockZ, anchors,
                        capRemaining - spawned);
            }
        }
        return spawned;
    }

    private static boolean isOuterEligibleChunkBorder(int chunkIndex) {
        int localX = chunkIndex % PLAYER_CHUNK_SPAWN_DIAMETER;
        int localZ = chunkIndex / PLAYER_CHUNK_SPAWN_DIAMETER;
        return localX == 0 || localX == PLAYER_CHUNK_SPAWN_DIAMETER - 1
                || localZ == 0 || localZ == PLAYER_CHUNK_SPAWN_DIAMETER - 1;
    }

    private int randomNaturalGroundY(int x, int z) {
        int topY = highestNaturalSpawnSearchY(x, z);
        return topY < 1 ? -1 : random.nextInt(topY + 1);
    }

    private int highestNaturalSpawnSearchY(int x, int z) {
        if (!world.isChunkGeneratedForBlock(x, z)) {
            return -1;
        }
        for (int y = Chunk.HEIGHT - 3; y >= 0; y--) {
            BlockType block = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
            if (!block.isAir()) {
                return y;
            }
        }
        return -1;
    }

    private int trySpawnGroundGroup(SpawnKind kind, int baseX, int selectedY, int baseZ,
            List<SpawnAnchor> anchors, int maxToSpawn) {
        if (maxToSpawn <= 0) {
            return 0;
        }
        SpawnRule rule = null;
        int targetCount = 0;
        int spawned = 0;
        int x = baseX;
        int z = baseZ;
        for (int attempt = 0; attempt < NATURAL_GROUP_MEMBER_ATTEMPTS && spawned < maxToSpawn; attempt++) {
            x += random.nextInt(NATURAL_GROUP_JITTER) - random.nextInt(NATURAL_GROUP_JITTER);
            z += random.nextInt(NATURAL_GROUP_JITTER) - random.nextInt(NATURAL_GROUP_JITTER);
            if (rule == null) {
                rule = kind == SpawnKind.HOSTILE
                        ? chooseHostileRule(x, selectedY, z)
                        : choosePassiveRule(x, selectedY, z);
                if (rule == null) {
                    continue;
                }
                targetCount = Math.min(maxToSpawn, nextPackSize(rule.definition()));
            }
            if (spawned >= targetCount) {
                break;
            }
            int y = selectedNaturalSpawnBaseY(kind, rule.definition(), x, selectedY, z);
            if (y < 0) {
                continue;
            }
            float spawnX = x + 0.5f;
            float spawnY = y + 1.0f;
            float spawnZ = z + 0.5f;
            if (!isFarEnoughFromPlayers(spawnX, spawnY, spawnZ, anchors)
                    || !isFarEnoughFromWorldSpawn(spawnX, spawnY, spawnZ)) {
                continue;
            }
            if (!rule.matches(world, x, y, z)) {
                continue;
            }
            boolean validPosition = kind == SpawnKind.HOSTILE
                    ? canSpawnHostileAt(x, y, z, rule.definition())
                    : canSpawnPassiveAt(x, y, z, rule.definition());
            if (!validPosition) {
                continue;
            }

            Mob mob = createMobForRule(rule, x, y, z);
            if (mob == null || hasGroundSpawnBlockCollision(mob, spawnX, spawnY, spawnZ)
                    || hasLivingSpawnCollision(mob, spawnX, spawnY, spawnZ)) {
                continue;
            }
            if (!passesMobSpecificSpawnGate(mob.getDefinition())) {
                continue;
            }
            placeNaturalSpawn(mob, spawnX, spawnY, spawnZ);
            initializeNaturalSpawn(mob);
            spawnMobWithNaturalRiders(mob);
            spawned++;
            if (spawned >= targetCount) {
                break;
            }
        }
        return spawned;
    }

    private int trySpawnWaterGroup(int baseX, int y, int baseZ, List<SpawnAnchor> anchors, int maxToSpawn) {
        if (maxToSpawn <= 0) {
            return 0;
        }
        SpawnRule rule = null;
        int targetCount = 0;
        int spawned = 0;
        int x = baseX;
        int z = baseZ;
        for (int attempt = 0; attempt < NATURAL_GROUP_MEMBER_ATTEMPTS && spawned < maxToSpawn; attempt++) {
            x += random.nextInt(NATURAL_GROUP_JITTER) - random.nextInt(NATURAL_GROUP_JITTER);
            z += random.nextInt(NATURAL_GROUP_JITTER) - random.nextInt(NATURAL_GROUP_JITTER);
            int candidateY = findWaterSpawnY(x, y, z);
            if (candidateY < 0) {
                continue;
            }
            float spawnX = x + 0.5f;
            float spawnY = candidateY;
            float spawnZ = z + 0.5f;
            if (!isFarEnoughFromPlayers(spawnX, spawnY, spawnZ, anchors)
                    || !isFarEnoughFromWorldSpawn(spawnX, spawnY, spawnZ)) {
                continue;
            }
            if (rule == null) {
                rule = chooseWeighted(OVERWORLD_WATER_CREATURES);
                if (rule == null) {
                    continue;
                }
                targetCount = Math.min(maxToSpawn, nextPackSize(rule.definition()));
            }
            if (spawned >= targetCount) {
                break;
            }
            if (!rule.matches(world, x, candidateY, z)) {
                continue;
            }

            Mob mob = createMobForRule(rule, x, candidateY, z);
            if (mob == null || hasLivingSpawnCollision(mob, spawnX, spawnY, spawnZ)) {
                continue;
            }
            placeNaturalSpawn(mob, spawnX, spawnY, spawnZ);
            initializeNaturalSpawn(mob);
            spawnMobWithNaturalRiders(mob);
            spawned++;
            if (spawned >= targetCount) {
                break;
            }
        }
        return spawned;
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

        return isReleaseNormalSpawnSupport(below) && !atFeet.isSolid() && !atHead.isSolid();
    }

    boolean canSpawnHostileAt(int x, int groundY, int z) {
        return canSpawnHostileAt(x, groundY, z, null);
    }

    boolean canSpawnHostileAt(int x, int groundY, int z, MobDefinition definition) {
        if (!world.isChunkGeneratedForBlock(x, z)) {
            return false;
        }
        if (definition != null && !definition.canSpawnIn(world.getDimension())) {
            return false;
        }
        if (requiresSolidSpawnSupport(definition) && !hasClearSpawnSpace(x, groundY, z)) {
            return false;
        }
        if (definition != null && hasSpawnVolumeObstruction(definition, x, groundY, z)) {
            return false;
        }
        if (definition == MobDefinition.SLIME) {
            return groundY <= SLIME_MAX_GROUND_Y && isSlimeChunk(x, z);
        }
        if (definition == MobDefinition.GHAST) {
            return true;
        }
        return passesReleaseHostileLightGate(x, groundY + 1, z);
    }

    boolean canSpawnPassiveAt(int x, int groundY, int z) {
        return canSpawnPassiveAt(x, groundY, z, null);
    }

    private boolean canSpawnPassiveAt(int x, int groundY, int z, MobDefinition definition) {
        if (!world.isChunkGeneratedForBlock(x, z) || !hasClearSpawnSpace(x, groundY, z)) {
            return false;
        }
        if (definition != null && (!definition.canSpawnIn(world.getDimension())
                || hasSpawnVolumeObstruction(definition, x, groundY, z))) {
            return false;
        }
        int light = getEffectiveLightForSpawning(x, groundY + 1, z);
        if (light < 9) {
            return false;
        }
        BlockType support = world.getBlockIfLoaded(x, groundY, z, BlockType.AIR);
        if (definition == MobDefinition.MOOSHROOM) {
            return support == BlockType.MYCELIUM;
        }
        return support == BlockType.GRASS;
    }

    int spawnHostilePack(SpawnRule rule, int baseX, int groundY, int baseZ, int maxToSpawn) {
        return spawnGroundPack(rule, baseX, groundY, baseZ, Float.NaN, Float.NaN, Float.NaN,
                maxToSpawn, SpawnKind.HOSTILE);
    }

    int spawnPassivePack(SpawnRule rule, int baseX, int groundY, int baseZ, int maxToSpawn) {
        return spawnGroundPack(rule, baseX, groundY, baseZ, Float.NaN, Float.NaN, Float.NaN,
                maxToSpawn, SpawnKind.PASSIVE);
    }

    int spawnWaterPack(SpawnRule rule, int baseX, int y, int baseZ, int maxToSpawn) {
        return spawnWaterPack(rule, baseX, y, baseZ, Float.NaN, Float.NaN, Float.NaN, maxToSpawn);
    }

    private int spawnHostilePack(SpawnRule rule, int baseX, int groundY, int baseZ,
            float playerX, float playerY, float playerZ, int maxToSpawn) {
        return spawnGroundPack(rule, baseX, groundY, baseZ, playerX, playerY, playerZ,
                maxToSpawn, SpawnKind.HOSTILE);
    }

    private int spawnPassivePack(SpawnRule rule, int baseX, int groundY, int baseZ,
            float playerX, float playerY, float playerZ, int maxToSpawn) {
        return spawnGroundPack(rule, baseX, groundY, baseZ, playerX, playerY, playerZ,
                maxToSpawn, SpawnKind.PASSIVE);
    }

    private int spawnWaterPack(SpawnRule rule, int baseX, int y, int baseZ,
            float playerX, float playerY, float playerZ, int maxToSpawn) {
        if (rule == null || rule.definition() == null || maxToSpawn <= 0) {
            return 0;
        }

        int targetCount = Math.min(maxToSpawn, nextPackSize(rule.definition()));
        int attempts = Math.max(targetCount * 6, 8);
        int spawned = 0;
        for (int attempt = 0; attempt < attempts && spawned < targetCount; attempt++) {
            int x = attempt == 0 ? baseX : baseX + random.nextInt(9) - 4;
            int z = attempt == 0 ? baseZ : baseZ + random.nextInt(9) - 4;
            int candidateY = attempt == 0 ? y : findWaterSpawnY(x, y, z);
            if (candidateY < 0 || !rule.matches(world, x, candidateY, z)) {
                continue;
            }
            if (!isFarEnoughFromPlayer(x + 0.5f, candidateY, z + 0.5f, playerX, playerY, playerZ)) {
                continue;
            }
            if (isNaturalSpawnAttempt(playerX, playerY, playerZ)
                    && !isFarEnoughFromWorldSpawn(x + 0.5f, candidateY, z + 0.5f)) {
                continue;
            }

            Mob mob = createMobForRule(rule, x, candidateY, z);
            if (mob == null || hasLivingSpawnCollision(mob, x + 0.5f, candidateY, z + 0.5f)) {
                continue;
            }
            placeNaturalSpawn(mob, x + 0.5f, candidateY, z + 0.5f);
            initializeNaturalSpawn(mob);
            spawnMobWithNaturalRiders(mob);
            spawned++;
        }
        return spawned;
    }

    private void spawnMobWithNaturalRiders(Mob mob) {
        world.spawnEntity(mob);
        if (mob instanceof Spider spider
                && mob.getDefinition() == MobDefinition.SPIDER
                && random.nextInt(SPIDER_JOCKEY_CHANCE) == 0) {
            Skeleton skeleton = new Skeleton();
            spider.mountJockey(skeleton);
            world.spawnEntity(skeleton);
        }
    }

    private int spawnGroundPack(SpawnRule rule, int baseX, int groundY, int baseZ,
            float playerX, float playerY, float playerZ, int maxToSpawn, SpawnKind kind) {
        if (rule == null || rule.definition() == null || maxToSpawn <= 0) {
            return 0;
        }

        int targetCount = Math.min(maxToSpawn, nextPackSize(rule.definition()));
        int attempts = Math.max(targetCount * 6, 8);
        int spawned = 0;
        for (int attempt = 0; attempt < attempts && spawned < targetCount; attempt++) {
            int x = attempt == 0 ? baseX : baseX + random.nextInt(9) - 4;
            int z = attempt == 0 ? baseZ : baseZ + random.nextInt(9) - 4;
            int y = attempt == 0 ? groundY : findSpawnY(x, groundY, z);
            if (y < 0 || !rule.matches(world, x, y, z)) {
                continue;
            }
            if (!isFarEnoughFromPlayer(x + 0.5f, y + 1.0f, z + 0.5f, playerX, playerY, playerZ)) {
                continue;
            }
            if (isNaturalSpawnAttempt(playerX, playerY, playerZ)
                    && !isFarEnoughFromWorldSpawn(x + 0.5f, y + 1.0f, z + 0.5f)) {
                continue;
            }
            boolean validPosition = kind == SpawnKind.HOSTILE
                    ? canSpawnHostileAt(x, y, z, rule.definition())
                    : canSpawnPassiveAt(x, y, z, rule.definition());
            if (!validPosition) {
                continue;
            }

            Mob mob = createMobForRule(rule, x, y, z);
            float spawnX = x + 0.5f;
            float spawnY = y + 1.0f;
            float spawnZ = z + 0.5f;
            if (mob == null || hasGroundSpawnBlockCollision(mob, spawnX, spawnY, spawnZ)
                    || hasLivingSpawnCollision(mob, spawnX, spawnY, spawnZ)) {
                continue;
            }
            if (!passesMobSpecificSpawnGate(mob.getDefinition())) {
                continue;
            }
            placeNaturalSpawn(mob, spawnX, spawnY, spawnZ);
            initializeNaturalSpawn(mob);
            spawnMobWithNaturalRiders(mob);
            spawned++;
        }
        return spawned;
    }

    private void placeNaturalSpawn(Mob mob, float x, float y, float z) {
        mob.setPosition(x, y, z);
        mob.setYaw(random.nextFloat() * 360.0f);
        mob.setPitch(0.0f);
    }

    private void initializeNaturalSpawn(Mob mob) {
        if (mob instanceof Sheep sheep) {
            sheep.applyNaturalSpawnColor(random);
        }
    }

    private int nextPackSize(MobDefinition definition) {
        int min = Math.max(1, definition.minPackSize());
        int max = Math.max(min, definition.maxPackSize());
        return min + random.nextInt(max - min + 1);
    }

    private boolean isFarEnoughFromPlayer(float spawnX, float spawnY, float spawnZ,
            float playerX, float playerY, float playerZ) {
        if (Float.isNaN(playerX) || Float.isNaN(playerY) || Float.isNaN(playerZ)) {
            return true;
        }
        float dx = spawnX - playerX;
        float dy = spawnY - playerY;
        float dz = spawnZ - playerZ;
        float minDistanceSq = MIN_SPAWN_DISTANCE * MIN_SPAWN_DISTANCE;
        return dx * dx + dy * dy + dz * dz >= minDistanceSq;
    }

    private boolean isFarEnoughFromPlayers(float spawnX, float spawnY, float spawnZ, List<SpawnAnchor> anchors) {
        if (anchors == null || anchors.isEmpty()) {
            return true;
        }
        for (SpawnAnchor anchor : anchors) {
            if (!isFarEnoughFromPlayer(spawnX, spawnY, spawnZ, anchor.x(), anchor.y(), anchor.z())) {
                return false;
            }
        }
        return true;
    }

    private boolean isNaturalSpawnAttempt(float playerX, float playerY, float playerZ) {
        return !Float.isNaN(playerX) && !Float.isNaN(playerY) && !Float.isNaN(playerZ);
    }

    private boolean isFarEnoughFromWorldSpawn(float spawnX, float spawnY, float spawnZ) {
        BlockPos spawn = world.getWorldSpawn();
        if (spawn == null) {
            return true;
        }
        float dx = spawnX - spawn.x();
        float dy = spawnY - spawn.y();
        float dz = spawnZ - spawn.z();
        float minDistanceSq = MIN_SPAWN_DISTANCE * MIN_SPAWN_DISTANCE;
        return dx * dx + dy * dy + dz * dz >= minDistanceSq;
    }

    private boolean hasLivingSpawnCollision(Mob mob, float x, float y, float z) {
        float halfWidth = mob.getWidth() * 0.5f;
        return world.hasLivingEntityIntersecting(x - halfWidth, y, z - halfWidth,
                x + halfWidth, y + mob.getHeight(), z + halfWidth);
    }

    private boolean hasGroundSpawnBlockCollision(Mob mob, float x, float y, float z) {
        if (mob == null) {
            return true;
        }
        float halfWidth = mob.getWidth() * 0.5f;
        AABB bounds = new AABB(x - halfWidth, y, z - halfWidth,
                x + halfWidth, y + mob.getHeight(), z + halfWidth);
        int minX = (int) Math.floor(bounds.getMin().x);
        int minY = (int) Math.floor(bounds.getMin().y);
        int minZ = (int) Math.floor(bounds.getMin().z);
        int maxX = (int) Math.floor(bounds.getMax().x - 0.0001f);
        int maxY = (int) Math.floor(bounds.getMax().y - 0.0001f);
        int maxZ = (int) Math.floor(bounds.getMax().z - 0.0001f);
        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    if (by < 0 || by >= Chunk.HEIGHT || !world.isChunkGeneratedForBlock(bx, bz)) {
                        return true;
                    }
                    BlockType type = world.getBlockIfLoaded(bx, by, bz, BlockType.BEDROCK);
                    if (type.isFluid()) {
                        return true;
                    }
                    for (AABB collision : world.getCollisionBoxesIfLoaded(bx, by, bz)) {
                        if (bounds.intersects(collision)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private int findWaterSpawnY(int x, int startY, int z) {
        for (int y = Math.min(62, startY); y >= 45; y--) {
            if (SpawnRule.isWaterSpawnCell(world, x, y, z)) {
                return y;
            }
        }
        return -1;
    }

    private boolean hasClearSpawnSpace(int x, int groundY, int z) {
        BlockType below = world.getBlockIfLoaded(x, groundY, z, BlockType.AIR);
        if (!isReleaseNormalSpawnSupport(below)) {
            return false;
        }

        BlockType atFeet = world.getBlockIfLoaded(x, groundY + 1, z, BlockType.STONE);
        BlockType atHead = world.getBlockIfLoaded(x, groundY + 2, z, BlockType.STONE);
        return !atFeet.isSolid() && !atHead.isSolid();
    }

    private int selectedNaturalSpawnBaseY(SpawnKind kind, MobDefinition definition, int x, int selectedY, int z) {
        if (definition == MobDefinition.GHAST) {
            return selectedY > 0 && selectedY < Chunk.HEIGHT - 2
                    && world.isChunkGeneratedForBlock(x, z) ? selectedY : -1;
        }
        if (isValidSpawnY(x, selectedY, z)) {
            return selectedY;
        }
        return findSpawnY(x, selectedY, z);
    }

    private static boolean isReleaseNormalSpawnSupport(BlockType type) {
        return BlockShape.isOpaqueCube(type)
                && type != BlockType.LEAVES
                && type != BlockType.CACTUS
                && type != BlockType.TNT;
    }

    private boolean requiresSolidSpawnSupport(MobDefinition definition) {
        return definition != MobDefinition.GHAST;
    }

    private boolean passesMobSpecificSpawnGate(MobDefinition definition) {
        if (definition == MobDefinition.GHAST) {
            return random.nextInt(GHAST_SPAWN_CHANCE) == 0;
        }
        return true;
    }

    private boolean passesReleaseHostileLightGate(int x, int y, int z) {
        if (world.getDimension() == Dimension.OVERWORLD
                && world.getSkyLight(x, y, z) > random.nextInt(HOSTILE_SKY_LIGHT_RANDOM_BOUND)) {
            return false;
        }
        int light = getEffectiveLightForSpawning(x, y, z);
        return light <= random.nextInt(HOSTILE_BLOCK_LIGHT_RANDOM_BOUND);
    }

    private boolean hasSpawnVolumeObstruction(MobDefinition definition, int x, int groundY, int z) {
        float spawnX = x + 0.5f;
        float spawnY = groundY + 1.0f;
        float spawnZ = z + 0.5f;
        float halfWidth = definition.width() * 0.5f;
        AABB bounds = new AABB(spawnX - halfWidth, spawnY, spawnZ - halfWidth,
                spawnX + halfWidth, spawnY + definition.height(), spawnZ + halfWidth);
        int minX = (int) Math.floor(bounds.getMin().x);
        int minY = (int) Math.floor(bounds.getMin().y);
        int minZ = (int) Math.floor(bounds.getMin().z);
        int maxX = (int) Math.floor(bounds.getMax().x - 0.0001f);
        int maxY = (int) Math.floor(bounds.getMax().y - 0.0001f);
        int maxZ = (int) Math.floor(bounds.getMax().z - 0.0001f);
        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    if (by < 0 || by >= Chunk.HEIGHT || !world.isChunkGeneratedForBlock(bx, bz)) {
                        return true;
                    }
                    BlockType type = world.getBlockIfLoaded(bx, by, bz, BlockType.BEDROCK);
                    if (type.isFluid()) {
                        return true;
                    }
                    for (AABB collision : world.getCollisionBoxesIfLoaded(bx, by, bz)) {
                        if (bounds.intersects(collision)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
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
        List<SpawnRule> rules = hostileRulesAt(x, y, z);
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

    private SpawnRule choosePassiveRule(int x, int y, int z) {
        List<SpawnRule> rules = passiveRulesAt(x, y, z);
        for (int attempts = 0; attempts < 8; attempts++) {
            SpawnRule rule = chooseWeighted(rules);
            if (rule != null && rule.matches(world, x, y, z)) {
                return rule;
            }
        }
        return null;
    }

    List<SpawnRule> hostileRulesAt(int x, int y, int z) {
        if (world.getDimension() == Dimension.NETHER) {
            return world.isInsideStructure(StructureType.NETHER_FORTRESS, x, y, z)
                    ? NETHER_FORTRESS_HOSTILES
                    : NETHER_HOSTILES;
        }
        if (world.getDimension() == Dimension.THE_END) {
            return END_HOSTILES;
        }
        BiomeType biome = world.getReleaseBiome(x, z);
        if (biome == BiomeType.MUSHROOM_ISLAND || biome == BiomeType.MUSHROOM_ISLAND_SHORE) {
            return NO_SPAWNS;
        }
        return OVERWORLD_HOSTILES;
    }

    List<SpawnRule> passiveRulesAt(int x, int y, int z) {
        if (world.getDimension() != Dimension.OVERWORLD) {
            return NO_SPAWNS;
        }
        return switch (world.getReleaseBiome(x, z)) {
            case OCEAN, FROZEN_OCEAN, RIVER, FROZEN_RIVER, DESERT, DESERT_HILLS,
                    BEACH, HELL, SKY -> NO_SPAWNS;
            case MUSHROOM_ISLAND, MUSHROOM_ISLAND_SHORE -> MUSHROOM_PASSIVES;
            case FOREST, FOREST_HILLS -> FOREST_PASSIVES;
            case TAIGA, TAIGA_HILLS -> TAIGA_PASSIVES;
            default -> STANDARD_PASSIVES;
        };
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

    private enum SpawnKind {
        HOSTILE,
        PASSIVE
    }

    private record SpawnAnchor(float x, float y, float z) {
    }

    private record SpawnChunkCandidate(int chunkX, int chunkZ, boolean border, float referenceY) {
    }

    private static boolean allFinite(float... values) {
        for (float value : values) {
            if (!Float.isFinite(value)) {
                return false;
            }
        }
        return true;
    }
}

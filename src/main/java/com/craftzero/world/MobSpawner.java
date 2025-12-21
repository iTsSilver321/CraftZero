package com.craftzero.world;

import com.craftzero.entity.mob.*;
import com.craftzero.main.Player;

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
        for (var entity : world.getEntities()) {
            if (entity instanceof Mob mob) {
                if (mob.isHostile()) {
                    hostileCount++;
                } else {
                    passiveCount++;
                }
            }
        }

        // Try to spawn hostile mobs
        if (hostileCount < MAX_HOSTILE) {
            trySpawnHostile(playerX, playerY, playerZ);
        }

        // Try to spawn passive mobs (less frequently)
        if (passiveCount < MAX_PASSIVE && random.nextFloat() < 0.2f) {
            trySpawnPassive(playerX, playerY, playerZ);
        }
    }

    private void trySpawnHostile(float playerX, float playerY, float playerZ) {
        // Pick random position
        float angle = random.nextFloat() * (float) Math.PI * 2;
        float distance = MIN_SPAWN_DISTANCE + random.nextFloat() * (MAX_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE);

        float spawnX = playerX + (float) Math.cos(angle) * distance;
        float spawnZ = playerZ + (float) Math.sin(angle) * distance;

        // Find valid Y (solid block with air above)
        int spawnY = findSpawnY((int) spawnX, (int) playerY, (int) spawnZ);
        if (spawnY < 0)
            return;

        // Check spawn conditions for hostile mobs
        // Use effective light (considers time of day - sky light is reduced at night)
        int light = getEffectiveLightForSpawning((int) spawnX, spawnY + 1, (int) spawnZ);
        if (light > 7)
            return; // Too bright for hostile mobs

        // Check block below is solid
        BlockType below = world.getBlock((int) spawnX, spawnY, (int) spawnZ);
        if (!below.isSolid())
            return;

        // Check space is clear
        BlockType atFeet = world.getBlock((int) spawnX, spawnY + 1, (int) spawnZ);
        BlockType atHead = world.getBlock((int) spawnX, spawnY + 2, (int) spawnZ);
        if (atFeet.isSolid() || atHead.isSolid())
            return;

        // Pick mob type
        Mob mob = createRandomHostileMob();
        mob.setPosition(spawnX + 0.5f, spawnY + 1, spawnZ + 0.5f);

        world.spawnEntity(mob);
    }

    private void trySpawnPassive(float playerX, float playerY, float playerZ) {
        // Pick random position (closer to player for passive mobs)
        float angle = random.nextFloat() * (float) Math.PI * 2;
        float distance = MIN_SPAWN_DISTANCE + random.nextFloat() * 40.0f;

        float spawnX = playerX + (float) Math.cos(angle) * distance;
        float spawnZ = playerZ + (float) Math.sin(angle) * distance;

        // Find valid Y
        int spawnY = findSpawnY((int) spawnX, (int) playerY, (int) spawnZ);
        if (spawnY < 0)
            return;

        // Check spawn conditions for passive mobs
        int light = world.getSkyLight((int) spawnX, spawnY + 1, (int) spawnZ);
        if (light < 9)
            return; // Too dark for passive mobs

        // Must spawn on grass
        BlockType below = world.getBlock((int) spawnX, spawnY, (int) spawnZ);
        if (below != BlockType.GRASS)
            return;

        // Check space is clear
        BlockType atFeet = world.getBlock((int) spawnX, spawnY + 1, (int) spawnZ);
        BlockType atHead = world.getBlock((int) spawnX, spawnY + 2, (int) spawnZ);
        if (atFeet.isSolid() || atHead.isSolid())
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
        BlockType below = world.getBlock(x, y, z);
        BlockType atFeet = world.getBlock(x, y + 1, z);
        BlockType atHead = world.getBlock(x, y + 2, z);

        return below.isSolid() && !atFeet.isSolid() && !atHead.isSolid();
    }

    private Mob createRandomHostileMob() {
        int type = random.nextInt(4);
        return switch (type) {
            case 0 -> new Zombie();
            case 1 -> new Skeleton();
            case 2 -> new Creeper();
            case 3 -> new Spider();
            default -> new Zombie();
        };
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
}

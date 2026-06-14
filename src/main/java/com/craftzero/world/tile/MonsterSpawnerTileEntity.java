package com.craftzero.world.tile;

import com.craftzero.entity.Entity;
import com.craftzero.entity.mob.Mob;
import com.craftzero.entity.mob.MobDefinition;
import com.craftzero.entity.mob.MobFactory;
import com.craftzero.main.Player;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;

import java.util.Random;

public class MonsterSpawnerTileEntity extends TileEntity {
    private static final int ACTIVATION_RANGE = 16;
    private static final int SPAWN_RANGE = 4;
    private static final int REQUIRED_PLAYER_RANGE_SQ = ACTIVATION_RANGE * ACTIVATION_RANGE;
    private static final int NEARBY_CAP_RANGE = 8;
    private static final int NEARBY_CAP_RANGE_SQ = NEARBY_CAP_RANGE * NEARBY_CAP_RANGE;

    private final Random random = new Random();
    private MobDefinition mobDefinition = MobDefinition.ZOMBIE;
    private int delay = 20;
    private int minDelay = 200;
    private int maxDelay = 800;
    private int spawnCount = 4;
    private int maxNearbyEntities = 6;

    public MonsterSpawnerTileEntity(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public String getTypeId() {
        return "mob_spawner";
    }

    @Override
    public void tick(World world, float deltaTime) {
        BlockPos pos = getPos();
        if (!world.isChunkGeneratedForBlock(pos.x(), pos.z())) {
            return;
        }
        if (!isPlayerNearby(world)) {
            return;
        }

        if (delay > 0) {
            delay--;
            markDirty();
            return;
        }

        boolean spawned = false;
        for (int i = 0; i < spawnCount; i++) {
            if (countNearby(world) >= maxNearbyEntities) {
                break;
            }
            if (trySpawn(world)) {
                spawned = true;
            }
        }

        resetDelay();
        if (spawned) {
            markDirty();
        }
    }

    private boolean isPlayerNearby(World world) {
        Player player = world.getPlayer();
        if (player == null || player.isDead()) {
            return false;
        }
        BlockPos pos = getPos();
        float dx = player.getPosition().x - (pos.x() + 0.5f);
        float dy = player.getPosition().y - (pos.y() + 0.5f);
        float dz = player.getPosition().z - (pos.z() + 0.5f);
        return dx * dx + dy * dy + dz * dz <= REQUIRED_PLAYER_RANGE_SQ;
    }

    private int countNearby(World world) {
        int count = 0;
        BlockPos pos = getPos();
        float cx = pos.x() + 0.5f;
        float cy = pos.y() + 0.5f;
        float cz = pos.z() + 0.5f;
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Mob mob
                    && mob.getDefinition() == mobDefinition
                    && distanceSquared(entity, cx, cy, cz) <= NEARBY_CAP_RANGE_SQ) {
                count++;
            }
        }
        return count;
    }

    private boolean trySpawn(World world) {
        Mob mob = MobFactory.create(mobDefinition);
        if (mob == null) {
            return false;
        }

        BlockPos pos = getPos();
        double angle = random.nextDouble() * Math.PI * 2.0;
        float radius = random.nextFloat() * SPAWN_RANGE;
        int x = (int) Math.floor(pos.x() + 0.5f + Math.cos(angle) * radius);
        int z = (int) Math.floor(pos.z() + 0.5f + Math.sin(angle) * radius);
        int y = pos.y() - 1 + random.nextInt(3);

        if (!world.isChunkGeneratedForBlock(x, z)) {
            return false;
        }

        BlockType feet = world.getBlockIfLoaded(x, y, z, BlockType.STONE);
        BlockType head = world.getBlockIfLoaded(x, y + 1, z, BlockType.STONE);
        BlockType below = world.getBlockIfLoaded(x, y - 1, z, BlockType.AIR);
        if (mobDefinition.category() == MobDefinition.MobCategory.WATER_CREATURE) {
            if (!feet.isWater() || !head.isWater()) {
                return false;
            }
        } else if (feet.isSolid() || head.isSolid() || !below.isSolid()) {
            return false;
        }

        mob.setPosition(x + 0.5f, y, z + 0.5f);
        world.spawnEntity(mob);
        return true;
    }

    private static float distanceSquared(Entity entity, float x, float y, float z) {
        float dx = entity.getX() - x;
        float dy = entity.getY() - y;
        float dz = entity.getZ() - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public MobDefinition getMobDefinition() {
        return mobDefinition;
    }

    public void setMobDefinition(MobDefinition mobDefinition) {
        this.mobDefinition = mobDefinition == null ? MobDefinition.ZOMBIE : mobDefinition;
        markDirty();
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = Math.max(0, delay);
        markDirty();
    }

    public int getMinDelay() {
        return minDelay;
    }

    public int getMaxDelay() {
        return maxDelay;
    }

    public void setDelayRange(int minDelay, int maxDelay) {
        this.minDelay = Math.max(1, minDelay);
        this.maxDelay = Math.max(this.minDelay, maxDelay);
        markDirty();
    }

    public int getSpawnCount() {
        return spawnCount;
    }

    public void setSpawnCount(int spawnCount) {
        this.spawnCount = Math.max(1, spawnCount);
        markDirty();
    }

    public int getMaxNearbyEntities() {
        return maxNearbyEntities;
    }

    public void setMaxNearbyEntities(int maxNearbyEntities) {
        this.maxNearbyEntities = Math.max(1, maxNearbyEntities);
        markDirty();
    }

    private void resetDelay() {
        delay = minDelay + random.nextInt(Math.max(1, maxDelay - minDelay + 1));
    }
}

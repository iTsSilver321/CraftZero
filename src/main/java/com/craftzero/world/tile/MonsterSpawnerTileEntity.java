package com.craftzero.world.tile;

import com.craftzero.entity.Entity;
import com.craftzero.entity.mob.Mob;
import com.craftzero.entity.mob.MobDefinition;
import com.craftzero.entity.mob.MobFactory;
import com.craftzero.entity.mob.MagmaCube;
import com.craftzero.entity.mob.Slime;
import com.craftzero.main.Player;
import com.craftzero.physics.AABB;
import com.craftzero.world.BlockType;
import com.craftzero.world.Chunk;
import com.craftzero.world.DayCycleManager;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;

import java.util.Random;

public class MonsterSpawnerTileEntity extends TileEntity {
    private static final int ACTIVATION_RANGE = 16;
    private static final int SPAWN_RANGE = 4;
    private static final int REQUIRED_PLAYER_RANGE_SQ = ACTIVATION_RANGE * ACTIVATION_RANGE;
    private static final int NEARBY_CAP_HORIZONTAL_RANGE = 8;
    private static final int NEARBY_CAP_VERTICAL_RANGE = 4;
    private static final int WATER_CREATURE_MIN_Y = 46;
    private static final int WATER_CREATURE_MAX_Y = 62;
    private static final int SPAWN_BURST_PARTICLE_PAIRS = 20;
    private static final float SPAWN_BURST_SPREAD = 2.0f;
    private static final float SPAWN_BURST_SMOKE_SCALE = 0.22f;
    private static final float SPAWN_BURST_FLAME_SCALE = 0.20f;
    private static final int SPAWN_BURST_SMOKE_LIFETIME_TICKS = 16;
    private static final int SPAWN_BURST_FLAME_LIFETIME_TICKS = 10;

    private static final MobDefinition DEFAULT_MOB = MobDefinition.PIG;

    private final Random injectedRandom;
    private MobDefinition mobDefinition = DEFAULT_MOB;
    private int delay = 20;
    private int minDelay = 200;
    private int maxDelay = 800;
    private int spawnCount = 4;
    private int maxNearbyEntities = 6;
    private float tickAccumulator;
    private float prevRenderRotation;
    private float renderRotation;

    public MonsterSpawnerTileEntity(int x, int y, int z) {
        this(x, y, z, null);
    }

    MonsterSpawnerTileEntity(int x, int y, int z, Random random) {
        super(x, y, z);
        this.injectedRandom = random;
    }

    @Override
    public String getTypeId() {
        return "mob_spawner";
    }

    @Override
    public void tick(World world, float deltaTime) {
        tickAccumulator += deltaTime * 20.0f;
        while (tickAccumulator >= 1.0f) {
            tickAccumulator -= 1.0f;
            tickOne(world);
        }
    }

    private void tickOne(World world) {
        BlockPos pos = getPos();
        if (!world.isChunkGeneratedForBlock(pos.x(), pos.z())) {
            syncInactiveRenderRotation();
            return;
        }
        if (!isPlayerNearby(world)) {
            syncInactiveRenderRotation();
            return;
        }
        advanceRenderRotation();
        Random random = randomSource(world);
        emitActiveParticles(world, random);

        if (delay > 0) {
            delay--;
            markDirty();
            return;
        }

        int nearbyCount = countNearby(world);
        boolean spawnedAny = false;
        for (int i = 0; i < spawnCount; i++) {
            if (nearbyCount >= maxNearbyEntities) {
                resetDelay(random);
                markDirty();
                return;
            }
            if (trySpawn(world, random)) {
                nearbyCount++;
                spawnedAny = true;
            }
        }

        if (spawnedAny) {
            resetDelay(random);
            markDirty();
        }
    }

    private Random randomSource(World world) {
        return injectedRandom != null ? injectedRandom : world.getRandom();
    }

    private void syncInactiveRenderRotation() {
        prevRenderRotation = renderRotation;
    }

    private void advanceRenderRotation() {
        prevRenderRotation = renderRotation;
        renderRotation += 1000.0f / (delay + 200.0f);
        while (renderRotation >= 360.0f) {
            renderRotation -= 360.0f;
            prevRenderRotation -= 360.0f;
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

    private void emitActiveParticles(World world, Random random) {
        BlockPos pos = getPos();
        float x = pos.x() + random.nextFloat();
        float y = pos.y() + random.nextFloat();
        float z = pos.z() + random.nextFloat();
        world.spawnParticle(WorldParticle.Type.SMOKE, x, y, z,
                0.0f, 0.0f, 0.0f, 0.18f + random.nextFloat() * 0.06f, 14);
        world.spawnParticle(WorldParticle.Type.FLAME, x, y, z,
                0.0f, 0.0f, 0.0f, 0.16f + random.nextFloat() * 0.04f, 10);
    }

    private int countNearby(World world) {
        int count = 0;
        BlockPos pos = getPos();
        float minX = pos.x() - NEARBY_CAP_HORIZONTAL_RANGE;
        float minY = pos.y() - NEARBY_CAP_VERTICAL_RANGE;
        float minZ = pos.z() - NEARBY_CAP_HORIZONTAL_RANGE;
        float maxX = pos.x() + 1.0f + NEARBY_CAP_HORIZONTAL_RANGE;
        float maxY = pos.y() + 1.0f + NEARBY_CAP_VERTICAL_RANGE;
        float maxZ = pos.z() + 1.0f + NEARBY_CAP_HORIZONTAL_RANGE;
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Mob mob
                    && mob.getDefinition() == mobDefinition
                    && intersectsExpandedBox(entity, minX, minY, minZ, maxX, maxY, maxZ)) {
                count++;
            }
        }
        return count;
    }

    private boolean trySpawn(World world, Random random) {
        BlockPos pos = getPos();
        float spawnX = pos.x() + 0.5f + (float) ((random.nextDouble() - random.nextDouble()) * SPAWN_RANGE);
        float spawnZ = pos.z() + 0.5f + (float) ((random.nextDouble() - random.nextDouble()) * SPAWN_RANGE);
        int x = (int) Math.floor(spawnX);
        int z = (int) Math.floor(spawnZ);
        int y = pos.y() - 1 + random.nextInt(3);

        if (!world.isChunkGeneratedForBlock(x, z)) {
            return false;
        }

        BlockType feet = world.getBlockIfLoaded(x, y, z, BlockType.STONE);
        BlockType head = world.getBlockIfLoaded(x, y + 1, z, BlockType.STONE);
        if (mobDefinition.category() == MobDefinition.MobCategory.WATER_CREATURE) {
            if (!canSpawnWaterCreature(y, feet, head)) {
                return false;
            }
        } else {
            if (feet.isSolid() || head.isSolid()) {
                return false;
            }
        }
        if (mobDefinition.category() == MobDefinition.MobCategory.CREATURE
                && !canSpawnCreature(world, x, y, z)) {
            return false;
        }
        if (mobDefinition.category() == MobDefinition.MobCategory.MONSTER
                && effectiveSpawnLight(world, x, y, z) > 7) {
            return false;
        }

        Mob mob = createSpawnerMob(random);
        if (mob == null) {
            return false;
        }
        boolean rejectFluids = mobDefinition.category() != MobDefinition.MobCategory.WATER_CREATURE;
        if (!hasClearSpawnVolume(world, mob, spawnX, y, spawnZ, rejectFluids)) {
            return false;
        }
        mob.setPosition(spawnX, y, spawnZ);
        mob.setYaw(random.nextFloat() * 360.0f);
        mob.setPitch(0.0f);
        world.spawnEntity(mob);
        emitSpawnBurst(world, random);
        return true;
    }

    private Mob createSpawnerMob(Random random) {
        return switch (mobDefinition) {
            case SLIME -> new Slime(randomSlimeSize(random));
            case MAGMA_CUBE -> new MagmaCube(randomSlimeSize(random));
            default -> MobFactory.create(mobDefinition);
        };
    }

    private static int randomSlimeSize(Random random) {
        Random source = random == null ? new Random() : random;
        return 1 << source.nextInt(3);
    }

    private void emitSpawnBurst(World world, Random random) {
        BlockPos pos = getPos();
        for (int i = 0; i < SPAWN_BURST_PARTICLE_PAIRS; i++) {
            float particleX = pos.x() + 0.5f + (random.nextFloat() - 0.5f) * SPAWN_BURST_SPREAD;
            float particleY = pos.y() + 0.5f + (random.nextFloat() - 0.5f) * SPAWN_BURST_SPREAD;
            float particleZ = pos.z() + 0.5f + (random.nextFloat() - 0.5f) * SPAWN_BURST_SPREAD;
            world.spawnParticle(WorldParticle.Type.SMOKE, particleX, particleY, particleZ,
                    0.0f, 0.0f, 0.0f, SPAWN_BURST_SMOKE_SCALE, SPAWN_BURST_SMOKE_LIFETIME_TICKS);
            world.spawnParticle(WorldParticle.Type.FLAME, particleX, particleY, particleZ,
                    0.0f, 0.0f, 0.0f, SPAWN_BURST_FLAME_SCALE, SPAWN_BURST_FLAME_LIFETIME_TICKS);
        }
    }

    private static boolean canSpawnWaterCreature(int y, BlockType feet, BlockType head) {
        return y >= WATER_CREATURE_MIN_Y
                && y <= WATER_CREATURE_MAX_Y
                && feet.isWater()
                && head.isWater();
    }

    private static boolean canSpawnCreature(World world, int x, int y, int z) {
        if (world.getBlockIfLoaded(x, y - 1, z, BlockType.AIR) != BlockType.GRASS) {
            return false;
        }
        int light = Math.max(world.getSkyLight(x, y, z), world.getBlockLight(x, y, z));
        return light >= 9;
    }

    private static boolean hasClearSpawnVolume(World world, Mob mob, float x, int y, float z,
            boolean rejectFluids) {
        float halfWidth = mob.getWidth() * 0.5f;
        AABB bounds = new AABB(x - halfWidth, y, z - halfWidth,
                x + halfWidth, y + mob.getHeight(), z + halfWidth);
        if (bounds.getMin().y < 0.0f || bounds.getMax().y > Chunk.HEIGHT) {
            return false;
        }
        if (world.hasEntityIntersecting(bounds.getMin().x, bounds.getMin().y, bounds.getMin().z,
                bounds.getMax().x, bounds.getMax().y, bounds.getMax().z, false)) {
            return false;
        }

        int minX = (int) Math.floor(bounds.getMin().x);
        int minY = (int) Math.floor(bounds.getMin().y);
        int minZ = (int) Math.floor(bounds.getMin().z);
        int maxX = (int) Math.floor(bounds.getMax().x - 0.0001f);
        int maxY = (int) Math.floor(bounds.getMax().y - 0.0001f);
        int maxZ = (int) Math.floor(bounds.getMax().z - 0.0001f);
        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    if (rejectFluids && world.getBlockIfLoaded(bx, by, bz, BlockType.BEDROCK).isFluid()) {
                        return false;
                    }
                    for (AABB blockBox : world.getCollisionBoxesIfLoaded(bx, by, bz)) {
                        if (bounds.intersects(blockBox)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private static int effectiveSpawnLight(World world, int x, int y, int z) {
        int skyLight = world.getSkyLight(x, y, z);
        DayCycleManager dayCycle = world.getDayCycleManager();
        if (dayCycle != null) {
            skyLight = (int) (skyLight * dayCycle.getSunBrightness());
        }
        return Math.max(skyLight, world.getBlockLight(x, y, z));
    }

    private static boolean intersectsExpandedBox(Entity entity, float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ) {
        float halfWidth = entity.getWidth() * 0.5f;
        return entity.getX() + halfWidth > minX
                && entity.getX() - halfWidth < maxX
                && entity.getY() + entity.getHeight() > minY
                && entity.getY() < maxY
                && entity.getZ() + halfWidth > minZ
                && entity.getZ() - halfWidth < maxZ;
    }

    public MobDefinition getMobDefinition() {
        return mobDefinition;
    }

    public void setMobDefinition(MobDefinition mobDefinition) {
        this.mobDefinition = mobDefinition == null ? DEFAULT_MOB : mobDefinition;
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

    public float getTickAccumulator() {
        return tickAccumulator;
    }

    public void setTickAccumulator(float tickAccumulator) {
        this.tickAccumulator = Math.max(0.0f, Math.min(1.0f, tickAccumulator));
        markDirty();
    }

    public float getRenderRotation(float partialTick) {
        float t = Math.max(0.0f, Math.min(1.0f, partialTick));
        return prevRenderRotation + (renderRotation - prevRenderRotation) * t;
    }

    private void resetDelay(Random random) {
        delay = minDelay + random.nextInt(Math.max(1, maxDelay - minDelay));
    }
}

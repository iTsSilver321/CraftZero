package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.Entity;
import com.craftzero.entity.ai.LineOfSightUtil;
import com.craftzero.entity.ai.MeleeAttackGoal;
import com.craftzero.entity.ai.TargetNearestGoal;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.CombatRules;
import com.craftzero.main.Player;
import com.craftzero.world.BlockType;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

public class Enderman extends Mob {
    private static final Set<BlockType> CARRYABLE = EnumSet.of(
            BlockType.GRASS, BlockType.DIRT, BlockType.SAND, BlockType.GRAVEL,
            BlockType.YELLOW_FLOWER, BlockType.RED_ROSE, BlockType.BROWN_MUSHROOM,
            BlockType.RED_MUSHROOM, BlockType.TNT, BlockType.CACTUS, BlockType.CLAY,
            BlockType.PUMPKIN, BlockType.MELON, BlockType.MYCELIUM);

    private BlockType carriedBlock = BlockType.AIR;
    private int carriedMetadata;
    private boolean angry;
    private int stareTicks;
    private int teleportCooldown;

    public Enderman() {
        super(MobDefinition.ENDERMAN.width(), MobDefinition.ENDERMAN.height(), MobDefinition.ENDERMAN.maxHealth());
        this.definition = MobDefinition.ENDERMAN;
        this.hostile = true;
        this.burnsInSunlight = false;
        this.moveSpeed = MobDefinition.ENDERMAN.moveSpeed();
        this.experienceValue = MobDefinition.ENDERMAN.experienceValue();
        ai.addGoal(2, new TargetNearestGoal(this, ai, 32.0f, true, this::canTargetPlayer));
        ai.addGoal(3, new MeleeAttackGoal(this, ai, CombatRules.EASY_ZOMBIE_DAMAGE, 1.8f, 1.15f));
    }

    private boolean canTargetPlayer() {
        if (angry) {
            return true;
        }
        Player player = world != null ? world.getPlayer() : null;
        if (player == null || player.isCreative() || !player.getDifficulty().allowsHostileSpawns()) {
            stareTicks = 0;
            return false;
        }
        if (isPlayerStaring(player)) {
            stareTicks++;
            if (stareTicks >= 5) {
                angry = true;
                return true;
            }
        } else {
            stareTicks = 0;
        }
        return false;
    }

    private boolean isPlayerStaring(Player player) {
        Vector3f eye = player.getCamera().getPosition();
        Vector3f forward = player.getCamera().getForward();
        Vector3f toMob = new Vector3f(x - eye.x, y + height * 0.5f - eye.y, z - eye.z);
        float distance = toMob.length();
        if (distance < 0.1f || distance > 64.0f) {
            return false;
        }
        toMob.normalize();
        float dot = forward.dot(toMob);
        return dot > 1.0f - 0.025f / distance
                && LineOfSightUtil.hasLineOfSight(world, eye.x, eye.y, eye.z, x, y + height * 0.5f, z);
    }

    @Override
    public void tick() {
        if (world != null) {
            if (isTouchingWaterOrRainLight()) {
                damage(1.0f, DamageSource.point(DamageSource.Type.GENERIC, x, y, z, 0.0f, 0.0f));
                teleportRandomly();
            } else if (teleportCooldown-- <= 0 && angry && random.nextInt(80) == 0) {
                Player player = world.getPlayer();
                if (player != null && distanceToSquaredPlayer(player) > 16.0f * 16.0f) {
                    teleportNearPlayer(player);
                }
                teleportCooldown = 30;
            }
            handleCarriedBlock();
        }
        super.tick();
    }

    private boolean isTouchingWaterOrRainLight() {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        for (int by = (int) Math.floor(y); by <= (int) Math.floor(y + height); by++) {
            if (world.getBlockIfLoaded(bx, by, bz, BlockType.AIR).isWater()) {
                return true;
            }
        }
        return world.canSeeSky(bx, (int) Math.floor(y + height), bz)
                && world.getDayCycleManager() != null
                && world.getDayCycleManager().getSunBrightness() > 0.7f
                && random.nextInt(30) == 0;
    }

    private float distanceToSquaredPlayer(Player player) {
        float dx = player.getPosition().x - x;
        float dy = player.getPosition().y - y;
        float dz = player.getPosition().z - z;
        return dx * dx + dy * dy + dz * dz;
    }

    private void handleCarriedBlock() {
        if (world == null) {
            return;
        }
        if (carriedBlock == BlockType.AIR && random.nextInt(200) == 0) {
            int bx = (int) Math.floor(x - 2 + random.nextFloat() * 4);
            int by = (int) Math.floor(y + random.nextFloat() * 3);
            int bz = (int) Math.floor(z - 2 + random.nextFloat() * 4);
            BlockType type = world.getBlockIfLoaded(bx, by, bz, BlockType.AIR);
            if (CARRYABLE.contains(type)) {
                carriedBlock = type;
                carriedMetadata = world.getBlockMetadata(bx, by, bz);
                world.setBlock(bx, by, bz, BlockType.AIR);
            }
        } else if (carriedBlock != BlockType.AIR && random.nextInt(2000) == 0) {
            int bx = (int) Math.floor(x - 1 + random.nextFloat() * 2);
            int by = (int) Math.floor(y + random.nextFloat() * 2);
            int bz = (int) Math.floor(z - 1 + random.nextFloat() * 2);
            if (world.getBlockIfLoaded(bx, by, bz, BlockType.STONE) == BlockType.AIR
                    && world.getBlockIfLoaded(bx, by - 1, bz, BlockType.AIR).isSolid()) {
                world.setBlock(bx, by, bz, carriedBlock, carriedMetadata);
                carriedBlock = BlockType.AIR;
                carriedMetadata = 0;
            }
        }
    }

    @Override
    public boolean damage(float amount, DamageSource source) {
        angry = true;
        if (source != null && source.type() == DamageSource.Type.ARROW) {
            for (int i = 0; i < 16; i++) {
                if (teleportRandomly()) {
                    return false;
                }
            }
        }
        return super.damage(amount, source);
    }

    @Override
    protected void onHurt(float amount, Entity source) {
        angry = true;
    }

    private boolean teleportRandomly() {
        if (world == null) {
            return false;
        }
        for (int i = 0; i < 16; i++) {
            float nx = x + (random.nextFloat() - 0.5f) * 64.0f;
            float ny = y + random.nextInt(64) - 32;
            float nz = z + (random.nextFloat() - 0.5f) * 64.0f;
            if (teleportTo(nx, ny, nz)) {
                return true;
            }
        }
        return false;
    }

    private boolean teleportNearPlayer(Player player) {
        Vector3f direction = new Vector3f(x - player.getPosition().x, y - player.getPosition().y,
                z - player.getPosition().z);
        if (direction.lengthSquared() < 0.01f) {
            return teleportRandomly();
        }
        direction.normalize();
        return teleportTo(player.getPosition().x + direction.x * 16.0f + (random.nextFloat() - 0.5f) * 8.0f,
                player.getPosition().y + random.nextInt(16) - 8,
                player.getPosition().z + direction.z * 16.0f + (random.nextFloat() - 0.5f) * 8.0f);
    }

    private boolean teleportTo(float nx, float ny, float nz) {
        int bx = (int) Math.floor(nx);
        int by = (int) Math.floor(ny);
        int bz = (int) Math.floor(nz);
        if (!world.isChunkGeneratedForBlock(bx, bz)) {
            return false;
        }
        while (by > 1 && !world.getBlockIfLoaded(bx, by - 1, bz, BlockType.AIR).isSolid()) {
            by--;
        }
        if (world.getBlockIfLoaded(bx, by, bz, BlockType.STONE) != BlockType.AIR
                || world.getBlockIfLoaded(bx, by + 1, bz, BlockType.STONE) != BlockType.AIR
                || !world.getBlockIfLoaded(bx, by - 1, bz, BlockType.AIR).isSolid()) {
            return false;
        }
        setPosition(bx + 0.5f, by, bz + 0.5f);
        setMotion(0, 0, 0);
        return true;
    }

    @Override
    public void dropLoot() {
        dropItems(ItemType.ENDER_PEARL, 0, 1);
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/enderman.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.ENDERMAN;
    }

    public BlockType getCarriedBlock() {
        return carriedBlock;
    }

    public int getCarriedMetadata() {
        return carriedMetadata;
    }

    public void setCarriedBlock(BlockType carriedBlock, int carriedMetadata) {
        this.carriedBlock = carriedBlock == null ? BlockType.AIR : carriedBlock;
        this.carriedMetadata = carriedMetadata;
    }

    public boolean isAngry() {
        return angry;
    }

    public void setAngry(boolean angry) {
        this.angry = angry;
    }
}

package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.EnderPearlEntity;
import com.craftzero.entity.Entity;
import com.craftzero.entity.ThrownItemEntity;
import com.craftzero.entity.ai.LineOfSightUtil;
import com.craftzero.entity.ai.MeleeAttackGoal;
import com.craftzero.entity.ai.TargetNearestGoal;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.CombatRules;
import com.craftzero.main.Player;
import com.craftzero.progression.ArmorSlot;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import com.craftzero.world.WorldSoundEvent;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

public class Enderman extends Mob {
    private static final int TELEPORT_PARTICLES = 128;
    private static final int AMBIENT_PORTAL_PARTICLES = 2;
    private static final float TELEPORT_PARTICLE_SCALE = 0.25f;
    private static final int TELEPORT_PARTICLE_LIFETIME_TICKS = 40;
    private static final float DAYLIGHT_ESCAPE_MIN_BRIGHTNESS = 0.5f;
    private static final int CARRY_PICKUP_ROLL_TICKS = 20;
    private static final int CARRY_PLACE_ROLL_TICKS = 2000;
    private static final float STARED_TELEPORT_DISTANCE_SQ = 16.0f;

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
        ai.addGoal(3, new MeleeAttackGoal(this, ai, CombatRules.EASY_ENDERMAN_DAMAGE, 1.8f, 1.15f));
    }

    private boolean canTargetPlayer() {
        if (angry) {
            return true;
        }
        Player player = world != null ? world.getPlayer() : null;
        boolean localStaring = player != null
                && !player.isCreative()
                && player.getDifficulty().allowsHostileSpawns()
                && isPlayerStaring(player);
        World.RemotePlayerTarget remoteStaring = nearestStaringRemotePlayer();
        if (!localStaring && remoteStaring == null) {
            stareTicks = 0;
            return false;
        }
        stareTicks++;
        if (stareTicks < 5) {
            return false;
        }
        angry = true;
        playEndermanStareSound();
        if (shouldPreferRemoteStareTarget(player, localStaring, remoteStaring)) {
            ai.setRemotePlayerTarget(remoteStaring);
            ai.setMoveTarget(remoteStaring.x(), remoteStaring.y(), remoteStaring.z());
        } else {
            ai.clearRemotePlayerTarget();
        }
        return true;
    }

    private boolean isPlayerStaring(Player player) {
        if (isWearingPumpkinHelmet(player)) {
            return false;
        }
        Vector3f eye = player.getCamera().getPosition();
        Vector3f forward = player.getCamera().getForward();
        Vector3f toMob = new Vector3f(x - eye.x, y + getHeight() * 0.5f - eye.y, z - eye.z);
        float distance = toMob.length();
        if (distance < 0.1f || distance > 64.0f) {
            return false;
        }
        toMob.normalize();
        float dot = forward.dot(toMob);
        return dot > 1.0f - 0.025f / distance
                && LineOfSightUtil.hasLineOfSight(world, eye.x, eye.y, eye.z, x, y + getHeight() * 0.5f, z);
    }

    private boolean isWearingPumpkinHelmet(Player player) {
        if (player == null || player.getInventory() == null) {
            return false;
        }
        ItemStack[] armor = player.getInventory().getArmor();
        if (armor == null || ArmorSlot.HELMET.getIndex() >= armor.length) {
            return false;
        }
        ItemStack helmet = armor[ArmorSlot.HELMET.getIndex()];
        return helmet != null && !helmet.isEmpty() && helmet.getType() == ItemType.PUMPKIN;
    }

    private World.RemotePlayerTarget nearestStaringRemotePlayer() {
        if (world == null) {
            return null;
        }
        for (World.RemotePlayerTarget target : world.remotePlayerTargets(
                x, y + getHeight() * 0.5f, z, 64.0f, true)) {
            if (isRemotePlayerStaring(target)) {
                return target;
            }
        }
        return null;
    }

    private boolean isRemotePlayerStaring(World.RemotePlayerTarget target) {
        if (target == null || !target.valid() || target.wearingPumpkinHelmet()) {
            return false;
        }
        Vector3f eye = new Vector3f(target.x(), target.eyeY(), target.z());
        Vector3f forward = remoteForward(target);
        Vector3f toMob = new Vector3f(x - eye.x, y + getHeight() * 0.5f - eye.y, z - eye.z);
        float distance = toMob.length();
        if (distance < 0.1f || distance > 64.0f) {
            return false;
        }
        toMob.normalize();
        float dot = forward.dot(toMob);
        return dot > 1.0f - 0.025f / distance
                && LineOfSightUtil.hasLineOfSight(world, eye.x, eye.y, eye.z, x, y + getHeight() * 0.5f, z);
    }

    private Vector3f remoteForward(World.RemotePlayerTarget target) {
        float yawRad = (float) Math.toRadians(target == null ? 0.0f : target.yaw());
        float pitchRad = (float) Math.toRadians(target == null ? 0.0f : target.pitch());
        return new Vector3f(
                (float) (Math.sin(yawRad) * Math.cos(pitchRad)),
                (float) (-Math.sin(pitchRad)),
                (float) (-Math.cos(yawRad) * Math.cos(pitchRad)));
    }

    private boolean shouldPreferRemoteStareTarget(Player localPlayer, boolean localStaring,
            World.RemotePlayerTarget remoteStaring) {
        if (remoteStaring == null || !remoteStaring.valid()) {
            return false;
        }
        if (!localStaring || localPlayer == null) {
            return true;
        }
        return remoteStaring.distance() <= distanceToPlayer(localPlayer);
    }

    @Override
    public void tick() {
        if (world != null) {
            if (isWet()) {
                damage(1.0f, DamageSource.point(DamageSource.Type.DROWN, x, y, z, 0.0f, 0.0f));
                teleportRandomly();
            } else if (shouldEscapeDaylight() && teleportRandomly()) {
                angry = false;
                stareTicks = 0;
                ai.clearMoveTarget();
                ai.clearTarget();
            } else if (teleportCooldown-- <= 0 && angry && random.nextInt(80) == 0) {
                World.RemotePlayerTarget remoteTarget = ai.getRemotePlayerTarget();
                if (remoteTarget != null && distanceToSquaredRemotePlayer(remoteTarget) > 16.0f * 16.0f) {
                    teleportNearRemotePlayer(remoteTarget);
                } else {
                    Player player = world.getPlayer();
                    if (player != null && distanceToSquaredPlayer(player) > 16.0f * 16.0f) {
                        teleportNearPlayer(player);
                    }
                }
                teleportCooldown = 30;
            }
            handleCarriedBlock();
            emitAmbientPortalParticles();
        }
        super.tick();
    }

    private boolean isWet() {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        for (int by = (int) Math.floor(y); by <= (int) Math.floor(y + getHeight()); by++) {
            if (world.getBlockIfLoaded(bx, by, bz, BlockType.AIR).isWater()) {
                return true;
            }
        }
        return world.isRainingAt(bx, (int) Math.floor(y + getHeight()), bz);
    }

    private boolean shouldEscapeDaylight() {
        if (world.getDayCycleManager() == null || !world.getDayCycleManager().isDaylightBurnTime()) {
            return false;
        }
        int blockX = (int) Math.floor(x);
        int blockY = (int) Math.floor(y);
        int blockZ = (int) Math.floor(z);
        if (!world.canSeeSky(blockX, blockY, blockZ)) {
            return false;
        }
        float brightness = daylightBrightness(blockX, blockY, blockZ);
        return brightness > DAYLIGHT_ESCAPE_MIN_BRIGHTNESS
                && random.nextFloat() * 30.0f < (brightness - 0.4f) * 2.0f;
    }

    private float daylightBrightness(int blockX, int blockY, int blockZ) {
        int skyLight = world.getSkyLight(blockX, blockY, blockZ);
        skyLight = (int) (skyLight * world.getDayCycleManager().getSunBrightness());
        int blockLight = world.getBlockLightIfLoaded(blockX, blockY, blockZ, 0);
        return Math.max(skyLight, blockLight) / 15.0f;
    }

    private float distanceToSquaredPlayer(Player player) {
        float dx = player.getPosition().x - x;
        float dy = player.getPosition().y - y;
        float dz = player.getPosition().z - z;
        return dx * dx + dy * dy + dz * dz;
    }

    private float distanceToPlayer(Player player) {
        return (float) Math.sqrt(distanceToSquaredPlayer(player));
    }

    private float distanceToSquaredRemotePlayer(World.RemotePlayerTarget target) {
        if (target == null) {
            return Float.MAX_VALUE;
        }
        float dx = target.x() - x;
        float dy = target.y() - y;
        float dz = target.z() - z;
        return dx * dx + dy * dy + dz * dz;
    }

    private void handleCarriedBlock() {
        if (world == null) {
            return;
        }
        if (carriedBlock == BlockType.AIR && random.nextInt(CARRY_PICKUP_ROLL_TICKS) == 0) {
            int bx = (int) Math.floor(x - 2 + random.nextFloat() * 4);
            int by = (int) Math.floor(y + random.nextFloat() * 3);
            int bz = (int) Math.floor(z - 2 + random.nextFloat() * 4);
            BlockType type = world.getBlockIfLoaded(bx, by, bz, BlockType.AIR);
            if (CARRYABLE.contains(type)) {
                carriedBlock = type;
                carriedMetadata = world.getBlockMetadataIfLoaded(bx, by, bz, 0);
                world.setBlock(bx, by, bz, BlockType.AIR);
            }
        } else if (carriedBlock != BlockType.AIR && random.nextInt(CARRY_PLACE_ROLL_TICKS) == 0) {
            int bx = (int) Math.floor(x - 1 + random.nextFloat() * 2);
            int by = (int) Math.floor(y + random.nextFloat() * 2);
            int bz = (int) Math.floor(z - 1 + random.nextFloat() * 2);
            if (world.getBlockIfLoaded(bx, by, bz, BlockType.STONE) == BlockType.AIR
                    && world.canPlaceBlockAtIfLoaded(bx, by, bz, carriedBlock, carriedMetadata)) {
                world.setBlock(bx, by, bz, carriedBlock, carriedMetadata);
                carriedBlock = BlockType.AIR;
                carriedMetadata = 0;
            }
        }
    }

    @Override
    public boolean damage(float amount, DamageSource source) {
        if (shouldDodgeProjectile(source)) {
            for (int i = 0; i < 16; i++) {
                if (teleportRandomly()) {
                    return false;
                }
            }
            return false;
        }
        if (shouldAngerFrom(source)) {
            angry = true;
        }
        return super.damage(amount, source);
    }

    private boolean shouldDodgeProjectile(DamageSource source) {
        if (source == null) {
            return false;
        }
        return source.type() == DamageSource.Type.ARROW
                || source.entity() instanceof ThrownItemEntity
                || source.entity() instanceof EnderPearlEntity;
    }

    private boolean shouldAngerFrom(DamageSource source) {
        if (source == null) {
            return false;
        }
        return source.type() == DamageSource.Type.PLAYER_ATTACK
                || source.type() == DamageSource.Type.MOB_MELEE
                || source.entity() != null;
    }

    @Override
    protected void onHurt(float amount, Entity source) {
        super.onHurt(amount, source);
        playMobHurtSound(WorldSoundEvent.ENDERMAN_HURT);
        if (source != null) {
            angry = true;
        }
    }

    @Override
    protected void onDeath() {
        playMobDeathSound(WorldSoundEvent.ENDERMAN_DEATH);
        super.onDeath();
    }

    @Override
    protected String getAmbientSoundId() {
        return angry ? WorldSoundEvent.ENDERMAN_SCREAM : WorldSoundEvent.ENDERMAN_IDLE;
    }

    private void playEndermanStareSound() {
        if (world != null) {
            world.playSound(WorldSoundEvent.ENDERMAN_STARE, x, y + getHeight() * 0.5f, z, 1.0f, 1.0f);
        }
    }

    private void emitAmbientPortalParticles() {
        for (int i = 0; i < AMBIENT_PORTAL_PARTICLES; i++) {
            float px = x + (random.nextFloat() - 0.5f) * getWidth();
            float py = y + random.nextFloat() * getHeight() - 0.25f;
            float pz = z + (random.nextFloat() - 0.5f) * getWidth();
            float motionX = (random.nextFloat() - 0.5f) * 2.0f;
            float motionY = -random.nextFloat();
            float motionZ = (random.nextFloat() - 0.5f) * 2.0f;
            world.spawnParticle(WorldParticle.Type.PORTAL, px, py, pz,
                    motionX, motionY, motionZ,
                    TELEPORT_PARTICLE_SCALE, TELEPORT_PARTICLE_LIFETIME_TICKS);
        }
    }

    @Override
    public boolean onMeleePursuit(Player player, float distance) {
        if (player == null || !isPlayerStaring(player)) {
            return false;
        }
        if (distance * distance < STARED_TELEPORT_DISTANCE_SQ) {
            teleportRandomly();
        }
        teleportCooldown = 0;
        return true;
    }

    @Override
    public boolean onRemoteMeleePursuit(World.RemotePlayerTarget target, float distance) {
        if (!isRemotePlayerStaring(target)) {
            return false;
        }
        if (distance * distance < STARED_TELEPORT_DISTANCE_SQ) {
            teleportRandomly();
        }
        teleportCooldown = 0;
        return true;
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

    private boolean teleportNearRemotePlayer(World.RemotePlayerTarget target) {
        if (target == null || !target.valid()) {
            return false;
        }
        Vector3f direction = new Vector3f(x - target.x(), y - target.y(), z - target.z());
        if (direction.lengthSquared() < 0.01f) {
            return teleportRandomly();
        }
        direction.normalize();
        return teleportTo(target.x() + direction.x * 16.0f + (random.nextFloat() - 0.5f) * 8.0f,
                target.y() + random.nextInt(16) - 8,
                target.z() + direction.z * 16.0f + (random.nextFloat() - 0.5f) * 8.0f);
    }

    private boolean teleportTo(float nx, float ny, float nz) {
        float oldX = x;
        float oldY = y;
        float oldZ = z;
        int bx = (int) Math.floor(nx);
        int by = (int) Math.floor(ny);
        int bz = (int) Math.floor(nz);
        if (!world.isChunkGeneratedForBlock(bx, bz)) {
            return false;
        }
        while (by > 1 && !world.getBlockIfLoaded(bx, by - 1, bz, BlockType.AIR).isSolid()) {
            by--;
        }
        BlockType support = world.getBlockIfLoaded(bx, by - 1, bz, BlockType.AIR);
        if (!hasTeleportClearance(bx, by, bz)
                || !isTeleportSupportSafe(support)
                || isTeleportDestinationWet(bx, by, bz)) {
            return false;
        }
        setPosition(bx + 0.5f, by, bz + 0.5f);
        setMotion(0, 0, 0);
        emitTeleportFeedback(oldX, oldY, oldZ);
        return true;
    }

    private boolean hasTeleportClearance(int bx, int by, int bz) {
        int topY = (int) Math.floor(by + getHeight());
        for (int checkY = by; checkY <= topY; checkY++) {
            if (world.getBlockIfLoaded(bx, checkY, bz, BlockType.STONE) != BlockType.AIR) {
                return false;
            }
        }
        return true;
    }

    private boolean isTeleportSupportSafe(BlockType support) {
        return support != null && support.isSolid() && support != BlockType.CACTUS;
    }

    private boolean isTeleportDestinationWet(int bx, int by, int bz) {
        int topY = (int) Math.floor(by + getHeight());
        for (int checkY = by; checkY <= topY; checkY++) {
            if (world.getBlockIfLoaded(bx, checkY, bz, BlockType.AIR).isWater()) {
                return true;
            }
        }
        return world.isRainingAt(bx, topY, bz);
    }

    private void emitTeleportFeedback(float oldX, float oldY, float oldZ) {
        if (world == null) {
            return;
        }
        for (int i = 0; i < TELEPORT_PARTICLES; i++) {
            float t = TELEPORT_PARTICLES == 1 ? 0.0f : i / (float) (TELEPORT_PARTICLES - 1);
            float spreadX = (random.nextFloat() - 0.5f) * getWidth() * 2.0f;
            float spreadY = random.nextFloat() * getHeight();
            float spreadZ = (random.nextFloat() - 0.5f) * getWidth() * 2.0f;
            float motionX = (random.nextFloat() - 0.5f) * 0.2f;
            float motionY = (random.nextFloat() - 0.5f) * 0.2f;
            float motionZ = (random.nextFloat() - 0.5f) * 0.2f;
            float px = oldX + (x - oldX) * t + spreadX;
            float py = oldY + (y - oldY) * t + spreadY;
            float pz = oldZ + (z - oldZ) * t + spreadZ;
            world.spawnParticle(WorldParticle.Type.PORTAL, px, py, pz,
                    motionX, motionY, motionZ, TELEPORT_PARTICLE_SCALE,
                    TELEPORT_PARTICLE_LIFETIME_TICKS);
        }
        world.playSound(WorldSoundEvent.ENDERMAN_TELEPORT,
                oldX, oldY, oldZ, 1.0f, 1.0f);
        world.playSound(WorldSoundEvent.ENDERMAN_TELEPORT,
                x, y, z, 1.0f, 1.0f);
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

    public int getStareTicks() {
        return stareTicks;
    }

    public int getTeleportCooldown() {
        return teleportCooldown;
    }

    public void setAttentionState(int stareTicks, int teleportCooldown) {
        this.stareTicks = Math.max(0, stareTicks);
        this.teleportCooldown = Math.max(0, teleportCooldown);
    }
}

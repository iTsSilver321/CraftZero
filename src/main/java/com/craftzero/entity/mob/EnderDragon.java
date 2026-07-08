package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.EndCrystalEntity;
import com.craftzero.entity.Entity;
import com.craftzero.main.CombatRules;
import com.craftzero.main.Player;
import com.craftzero.physics.AABB;
import com.craftzero.world.Block;
import com.craftzero.world.BlockShape;
import com.craftzero.world.BlockType;
import com.craftzero.world.Chunk;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import com.craftzero.world.WorldSoundEvent;
import org.joml.Vector3f;

public class EnderDragon extends Mob {
    private static final int MOVEMENT_HISTORY_SIZE = 64;
    private static final float HEALING_CRYSTAL_RANGE = 32.0f;
    private static final float HEALING_CRYSTAL_DESTROY_DAMAGE = 10.0f;
    private static final float RETARGET_CLOSE_DISTANCE = 10.0f;
    private static final float RETARGET_FAR_DISTANCE = 150.0f;
    private static final float YAW_TURN_LIMIT_DEGREES = 50.0f;
    private static final float VERTICAL_ACCELERATION = 0.1f;
    private static final float FORWARD_ACCELERATION = 0.06f;
    private static final float YAW_VELOCITY_DAMPING = 0.8f;
    private static final float YAW_ALIGNMENT_STRENGTH = 0.7f;
    private static final float HORIZONTAL_BASE_DAMPING = 0.8f;
    private static final float HORIZONTAL_ALIGNMENT_DAMPING = 0.15f;
    private static final float VERTICAL_DAMPING = 0.91f;
    private static final float PITCH_MOVEMENT_SCALE = 5.0f;
    private static final int EXIT_PORTAL_Y = 64;
    private static final int DEATH_XP_PULSE_START_TICK = 150;
    private static final int DEATH_XP_PULSE_INTERVAL_TICKS = 5;
    private static final int DEATH_XP_PULSE_AMOUNT = 1000;
    private static final int DEATH_XP_FINAL_AMOUNT = 2000;
    private static final int DEATH_PARTICLE_START_TICK = 180;
    private static final float DEATH_PARTICLE_HORIZONTAL_RANGE = 8.0f;
    private static final float DEATH_PARTICLE_VERTICAL_RANGE = 4.0f;
    private static final float DEATH_PARTICLE_Y_OFFSET = 2.0f;
    private static final float DEATH_PARTICLE_SCALE = 4.0f;
    private static final int DEATH_PARTICLE_LIFETIME_TICKS = 16;
    private static final float DEATH_SOUND_VOLUME = 5.0f;
    private static final float DEATH_SOUND_PITCH = 1.0f;
    private static final float DEATH_ASCENT_PER_TICK = 0.1f;
    private static final float DEATH_ROTATION_PER_TICK = 20.0f;
    private static final float BLOCK_DESTRUCTION_PARTICLE_SCALE = 2.0f;
    private static final int BLOCK_DESTRUCTION_PARTICLE_LIFETIME_TICKS = 12;
    private static final float PROTECTED_BLOCK_MOVEMENT_SCALE = 0.8f;
    private static final float BODY_PART_WIDTH = 5.0f;
    private static final float BODY_PART_HEIGHT = 3.0f;
    private static final float BODY_PART_FORWARD_OFFSET = 0.5f;
    private static final float HEAD_PART_WIDTH = 3.0f;
    private static final float HEAD_PART_HEIGHT = 3.0f;
    private static final float HEAD_PART_FORWARD_OFFSET = 5.5f;
    private static final float CONTACT_DAMAGE = 5.0f;
    private static final int CONTACT_DAMAGE_INTERVAL_TICKS = 10;
    private static final float PLAYER_HALF_WIDTH = 0.3f;
    private static final float REMOTE_PLAYER_CONTACT_RANGE = 32.0f;
    public static final int DEATH_SEQUENCE_TICKS = 200;

    private float targetX;
    private float targetY;
    private float targetZ;
    private int targetCooldown;
    private int deathTicks;
    private EndCrystalEntity healingCrystal;
    private boolean slowedByProtectedBlock;
    private final double[][] movementHistory = new double[MOVEMENT_HISTORY_SIZE][3];
    private int movementHistoryIndex = -1;
    private float yawVelocity;

    public EnderDragon() {
        super(MobDefinition.ENDER_DRAGON.width(), MobDefinition.ENDER_DRAGON.height(),
                MobDefinition.ENDER_DRAGON.maxHealth());
        this.definition = MobDefinition.ENDER_DRAGON;
        this.hostile = true;
        this.moveSpeed = MobDefinition.ENDER_DRAGON.moveSpeed();
        this.experienceValue = MobDefinition.ENDER_DRAGON.experienceValue();
        chooseTarget();
    }

    @Override
    public void tick() {
        prevX = x;
        prevY = y;
        prevZ = z;
        prevYaw = yaw;
        prevPitch = pitch;
        ticksExisted++;
        prevBodyYaw = bodyYaw;
        prevLimbSwingAmount = limbSwingAmount;
        if (hurtTime > 0) {
            hurtTime--;
        }
        if (invulnerableTime > 0) {
            invulnerableTime--;
        }
        if (health <= 0.0f) {
            if (!dead) {
                dead = true;
                onDeath();
            }
            deathTicks++;
            motionX *= 0.92f;
            motionY *= 0.92f;
            motionZ *= 0.92f;
            emitDeathEffects();
            releaseDeathExperience();
            moveWithCollision(0.0f, DEATH_ASCENT_PER_TICK, 0.0f);
            yaw += DEATH_ROTATION_PER_TICK;
            bodyYaw = yaw;
            if (deathTicks >= DEATH_SEQUENCE_TICKS) {
                createDeathPortal();
                remove();
            }
            return;
        }
        healFromCrystals();
        fly();
        destroyBlocksInFlightPath();
        damageNearbyPlayer();
    }

    private void fly() {
        float distanceToTarget = distanceToTarget();
        if (targetCooldown-- <= 0 || distanceToTarget < RETARGET_CLOSE_DISTANCE
                || distanceToTarget > RETARGET_FAR_DISTANCE) {
            chooseTarget();
            distanceToTarget = distanceToTarget();
        }
        float dx = targetX - x;
        float dy = targetY - y;
        float dz = targetZ - z;
        float distance = Math.max(0.1f, (float) Math.sqrt(dx * dx + dy * dy + dz * dz));
        motionY += dy / distance * VERTICAL_ACCELERATION;

        float desiredYaw = (float) (180.0 - Math.toDegrees(Math.atan2(dx, dz)));
        yaw = updateRotation(yaw, desiredYaw, YAW_TURN_LIMIT_DEGREES);

        float yawRadians = (float) Math.toRadians(yaw);
        float headingX = (float) Math.sin(yawRadians);
        float headingY = motionY;
        float headingZ = -(float) Math.cos(yawRadians);
        float headingLength = Math.max(0.0001f,
                (float) Math.sqrt(headingX * headingX + headingY * headingY + headingZ * headingZ));
        headingX /= headingLength;
        headingY /= headingLength;
        headingZ /= headingLength;

        float targetXNorm = dx / distance;
        float targetYNorm = dy / distance;
        float targetZNorm = dz / distance;
        float alignment = (headingX * targetXNorm + headingY * targetYNorm + headingZ * targetZNorm + 0.5f) / 1.5f;
        alignment = Math.max(0.0f, alignment);

        float horizontalSpeed = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
        float speedFactor = Math.min(40.0f, horizontalSpeed + 1.0f);
        yawVelocity *= YAW_VELOCITY_DAMPING;
        yawVelocity += alignment * (YAW_ALIGNMENT_STRENGTH / speedFactor / (horizontalSpeed + 1.0f));
        yaw = wrapDegreesLocal(yaw + yawVelocity * 0.1f);

        yawRadians = (float) Math.toRadians(yaw);
        float forwardScale = 2.0f / (speedFactor + 1.0f);
        float movementScale = slowedByProtectedBlock ? PROTECTED_BLOCK_MOVEMENT_SCALE : 1.0f;
        motionX += (float) Math.sin(yawRadians) * FORWARD_ACCELERATION * forwardScale * movementScale;
        motionZ -= (float) Math.cos(yawRadians) * FORWARD_ACCELERATION * forwardScale * movementScale;
        x += motionX;
        y += motionY;
        z += motionZ;

        float movementLength = Math.max(0.0001f,
                (float) Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ));
        float movementAlignment = ((motionX / movementLength) * headingX
                + (motionY / movementLength) * headingY
                + (motionZ / movementLength) * headingZ
                + 1.0f) * 0.5f;
        float horizontalDamping = HORIZONTAL_BASE_DAMPING + HORIZONTAL_ALIGNMENT_DAMPING * movementAlignment;
        motionX *= horizontalDamping;
        motionY *= VERTICAL_DAMPING;
        motionZ *= horizontalDamping;
        horizontalSpeed = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
        pitch = -((float) Math.toDegrees(Math.atan2(motionY, horizontalSpeed))) * PITCH_MOVEMENT_SCALE;
        bodyYaw = yaw;
        limbSwing += 0.18f;
        limbSwingAmount = 0.35f;
        recordMovementHistory();
    }

    private void destroyBlocksInFlightPath() {
        if (world == null) {
            return;
        }
        DragonBlockDestruction body = destroyDragonBlocksInBox(dragonBodyBlockDestructionBox());
        DragonBlockDestruction head = destroyDragonBlocksInBox(dragonHeadBlockDestructionBox());
        slowedByProtectedBlock = body.blockedByProtectedBlock() || head.blockedByProtectedBlock();
    }

    private AABB dragonBodyBlockDestructionBox() {
        return dragonPartBox(BODY_PART_FORWARD_OFFSET, BODY_PART_WIDTH, BODY_PART_HEIGHT);
    }

    private AABB dragonHeadBlockDestructionBox() {
        return dragonPartBox(HEAD_PART_FORWARD_OFFSET, HEAD_PART_WIDTH, HEAD_PART_HEIGHT);
    }

    private AABB dragonPartBox(float forwardOffset, float width, float height) {
        float yawRadians = (float) Math.toRadians(yaw);
        float partX = x + (float) Math.sin(yawRadians) * forwardOffset;
        float partZ = z - (float) Math.cos(yawRadians) * forwardOffset;
        float halfWidth = width * 0.5f;
        return new AABB(partX - halfWidth, y, partZ - halfWidth,
                partX + halfWidth, y + height, partZ + halfWidth);
    }

    private DragonBlockDestruction destroyDragonBlocksInBox(AABB bounds) {
        int minX = (int) Math.floor(bounds.getMin().x);
        int minY = Math.max(0, (int) Math.floor(bounds.getMin().y));
        int minZ = (int) Math.floor(bounds.getMin().z);
        int maxX = (int) Math.floor(bounds.getMax().x);
        int maxY = Math.min(Chunk.HEIGHT - 1, (int) Math.floor(bounds.getMax().y));
        int maxZ = (int) Math.floor(bounds.getMax().z);
        boolean destroyed = false;
        boolean blockedByProtectedBlock = false;
        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    BlockType block = world.getBlockIfLoaded(bx, by, bz, BlockType.AIR);
                    if (block == null || block == BlockType.AIR) {
                        continue;
                    }
                    if (isDragonProtectedBlock(block)) {
                        blockedByProtectedBlock = true;
                        continue;
                    }
                    destroyed |= world.setBlockIfLoaded(bx, by, bz, BlockType.AIR, 0);
                }
            }
        }
        if (destroyed) {
            spawnBlockDestructionParticle(bounds);
        }
        return new DragonBlockDestruction(destroyed, blockedByProtectedBlock);
    }

    private void spawnBlockDestructionParticle(AABB bounds) {
        float px = bounds.getMin().x + (bounds.getMax().x - bounds.getMin().x) * random.nextFloat();
        float py = bounds.getMin().y + (bounds.getMax().y - bounds.getMin().y) * random.nextFloat();
        float pz = bounds.getMin().z + (bounds.getMax().z - bounds.getMin().z) * random.nextFloat();
        world.spawnParticle(WorldParticle.Type.HUGE_EXPLOSION,
                px, py, pz,
                0.0f, 0.0f, 0.0f,
                BLOCK_DESTRUCTION_PARTICLE_SCALE,
                BLOCK_DESTRUCTION_PARTICLE_LIFETIME_TICKS);
    }

    private static boolean isDragonProtectedBlock(BlockType block) {
        return block == BlockType.BEDROCK
                || block == BlockType.OBSIDIAN
                || block == BlockType.END_STONE;
    }

    private record DragonBlockDestruction(boolean destroyed, boolean blockedByProtectedBlock) {
    }

    private void chooseTarget() {
        if (choosePlayerTarget()) {
            return;
        }
        double angle = random.nextDouble() * Math.PI * 2.0;
        float radius = 48.0f + random.nextFloat() * 42.0f;
        targetX = (float) Math.cos(angle) * radius;
        targetZ = (float) Math.sin(angle) * radius;
        targetY = 68.0f + random.nextFloat() * 34.0f;
        targetCooldown = 80 + random.nextInt(80);
    }

    private boolean choosePlayerTarget() {
        if (world == null || random.nextInt(2) != 0) {
            return false;
        }

        Player localPlayer = world.getPlayer();
        boolean localValid = localPlayer != null && !localPlayer.isDead();
        float localDistanceSq = localValid ? distanceSquaredTo(localPlayer.getPosition().x,
                localPlayer.getPosition().y, localPlayer.getPosition().z) : Float.MAX_VALUE;
        World.RemotePlayerTarget remoteTarget = world.nearestRemotePlayerTarget(x, y, z, RETARGET_FAR_DISTANCE, false);
        boolean remoteValid = remoteTarget != null && remoteTarget.valid();
        float remoteDistanceSq = remoteValid ? remoteTarget.distance() * remoteTarget.distance() : Float.MAX_VALUE;

        if (remoteValid && (!localValid || remoteDistanceSq <= localDistanceSq)) {
            targetX = remoteTarget.x();
            targetY = remoteTarget.y();
            targetZ = remoteTarget.z();
        } else if (localValid) {
            Vector3f playerPosition = localPlayer.getPosition();
            targetX = playerPosition.x;
            targetY = playerPosition.y;
            targetZ = playerPosition.z;
        } else {
            return false;
        }
        targetCooldown = 80 + random.nextInt(80);
        return true;
    }

    private float distanceSquaredTo(float targetX, float targetY, float targetZ) {
        float dx = targetX - x;
        float dy = targetY - y;
        float dz = targetZ - z;
        return dx * dx + dy * dy + dz * dz;
    }

    private float distanceToTarget() {
        float dx = targetX - x;
        float dy = targetY - y;
        float dz = targetZ - z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private void healFromCrystals() {
        if (world == null) {
            return;
        }
        if (healingCrystal == null || healingCrystal.isRemoved()) {
            healingCrystal = nearestHealingCrystal();
        }
        if (healingCrystal != null && ticksExisted % 10 == 0 && health < maxHealth) {
            heal(1.0f);
        }
    }

    private EndCrystalEntity nearestHealingCrystal() {
        EndCrystalEntity nearest = null;
        float nearestDistanceSquared = Float.MAX_VALUE;
        for (Entity entity : world.getEntities()) {
            if (entity instanceof EndCrystalEntity && !entity.isRemoved()) {
                float dx = entity.getX() - x;
                float dy = entity.getY() - y;
                float dz = entity.getZ() - z;
                if (Math.abs(dx) <= HEALING_CRYSTAL_RANGE
                        && Math.abs(dy) <= HEALING_CRYSTAL_RANGE
                        && Math.abs(dz) <= HEALING_CRYSTAL_RANGE) {
                    float distanceSquared = dx * dx + dy * dy + dz * dz;
                    if (distanceSquared < nearestDistanceSquared) {
                        nearestDistanceSquared = distanceSquared;
                        nearest = (EndCrystalEntity) entity;
                    }
                }
            }
        }
        return nearest;
    }

    public boolean isChargingFrom(EndCrystalEntity crystal) {
        return healingCrystal == crystal && crystal != null && !crystal.isRemoved();
    }

    public EndCrystalEntity getHealingCrystal() {
        return healingCrystal != null && !healingCrystal.isRemoved() ? healingCrystal : null;
    }

    public void onHealingCrystalDestroyed(EndCrystalEntity crystal) {
        if (!isChargingFrom(crystal)) {
            return;
        }
        healingCrystal = null;
        damage(HEALING_CRYSTAL_DESTROY_DAMAGE, DamageSource.point(DamageSource.Type.EXPLOSION,
                crystal.getX(), crystal.getY(), crystal.getZ(), 0.0f, 0.0f));
    }

    private void damageNearbyPlayer() {
        if (world == null || ticksExisted % CONTACT_DAMAGE_INTERVAL_TICKS != 0) {
            return;
        }
        Player player = world.getPlayer();
        if (player != null) {
            AABB playerBox = player.getBoundingBox();
            if (playerBox != null) {
                if (damagePlayerFromPart(player, playerBox, dragonHeadBlockDestructionBox())) {
                    return;
                }
                if (damagePlayerFromPart(player, playerBox, dragonBodyBlockDestructionBox())) {
                    return;
                }
            }
        }

        World.RemotePlayerTarget remoteTarget = world.nearestRemotePlayerTarget(
                x, y, z, REMOTE_PLAYER_CONTACT_RANGE, false);
        if (remoteTarget == null || !remoteTarget.valid()) {
            return;
        }
        AABB remoteBox = remotePlayerBox(remoteTarget);
        if (damageRemotePlayerFromPart(remoteTarget, remoteBox, dragonHeadBlockDestructionBox())) {
            return;
        }
        damageRemotePlayerFromPart(remoteTarget, remoteBox, dragonBodyBlockDestructionBox());
    }

    private boolean damagePlayerFromPart(Player player, AABB playerBox, AABB partBox) {
        if (!partBox.intersects(playerBox)) {
            return false;
        }
        float sourceX = (partBox.getMin().x + partBox.getMax().x) * 0.5f;
        float sourceY = (partBox.getMin().y + partBox.getMax().y) * 0.5f;
        float sourceZ = (partBox.getMin().z + partBox.getMax().z) * 0.5f;
        return player.hurt(CONTACT_DAMAGE, DamageSource.point(DamageSource.Type.MOB_MELEE,
                sourceX, sourceY, sourceZ,
                CombatRules.MOB_MELEE_HORIZONTAL_KNOCKBACK * 2.0f,
                CombatRules.MOB_MELEE_VERTICAL_KNOCKBACK + 0.2f));
    }

    private AABB remotePlayerBox(World.RemotePlayerTarget target) {
        return new AABB(target.x() - PLAYER_HALF_WIDTH, target.y(), target.z() - PLAYER_HALF_WIDTH,
                target.x() + PLAYER_HALF_WIDTH, target.y() + target.height(), target.z() + PLAYER_HALF_WIDTH);
    }

    private boolean damageRemotePlayerFromPart(World.RemotePlayerTarget target, AABB playerBox, AABB partBox) {
        if (!partBox.intersects(playerBox)) {
            return false;
        }
        float sourceX = (partBox.getMin().x + partBox.getMax().x) * 0.5f;
        float sourceY = (partBox.getMin().y + partBox.getMax().y) * 0.5f;
        float sourceZ = (partBox.getMin().z + partBox.getMax().z) * 0.5f;
        return world.damageRemotePlayerTarget(target.playerId(),
                new World.RemotePlayerDamage(CONTACT_DAMAGE, "mob_melee",
                        sourceX, sourceY, sourceZ,
                        CombatRules.MOB_MELEE_HORIZONTAL_KNOCKBACK * 2.0f,
                        CombatRules.MOB_MELEE_VERTICAL_KNOCKBACK + 0.2f,
                        0));
    }

    @Override
    public void updatePhysics(float deltaTime) {
        // The dragon's flight integration happens in tick().
    }

    @Override
    protected void onDeath() {
        // The dragon exit portal is created at the end of the old death sequence.
    }

    private void createDeathPortal() {
        if (world == null) {
            return;
        }
        int cx = (int) Math.floor(x);
        int cz = (int) Math.floor(z);
        createExitPortal(cx, cz);
    }

    private void releaseDeathExperience() {
        if (world == null) {
            return;
        }
        if (deathTicks > DEATH_XP_PULSE_START_TICK
                && deathTicks % DEATH_XP_PULSE_INTERVAL_TICKS == 0) {
            world.spawnExperience(x, y, z, DEATH_XP_PULSE_AMOUNT);
        }
        if (deathTicks == DEATH_SEQUENCE_TICKS) {
            world.spawnExperience(x, y, z, DEATH_XP_FINAL_AMOUNT);
        }
    }

    private void emitDeathEffects() {
        if (world == null) {
            return;
        }
        if (deathTicks == 1) {
            world.playSound(WorldSoundEvent.ENDER_DRAGON_DEATH, x, y, z,
                    DEATH_SOUND_VOLUME, DEATH_SOUND_PITCH);
        }
        if (deathTicks >= DEATH_PARTICLE_START_TICK && deathTicks <= DEATH_SEQUENCE_TICKS) {
            float px = x + (random.nextFloat() - 0.5f) * DEATH_PARTICLE_HORIZONTAL_RANGE;
            float py = y + DEATH_PARTICLE_Y_OFFSET
                    + (random.nextFloat() - 0.5f) * DEATH_PARTICLE_VERTICAL_RANGE;
            float pz = z + (random.nextFloat() - 0.5f) * DEATH_PARTICLE_HORIZONTAL_RANGE;
            world.spawnParticle(WorldParticle.Type.HUGE_EXPLOSION, px, py, pz,
                    0.0f, 0.0f, 0.0f, DEATH_PARTICLE_SCALE, DEATH_PARTICLE_LIFETIME_TICKS);
        }
    }

    private void createExitPortal(int cx, int cz) {
        for (int y = EXIT_PORTAL_Y - 1; y <= EXIT_PORTAL_Y + 32; y++) {
            for (int x = cx - 4; x <= cx + 4; x++) {
                for (int z = cz - 4; z <= cz + 4; z++) {
                    double dx = x - cx;
                    double dz = z - cz;
                    double distanceSquared = dx * dx + dz * dz;
                    if (distanceSquared > 12.25D) {
                        continue;
                    }
                    if (y < EXIT_PORTAL_Y) {
                        if (distanceSquared <= 6.25D) {
                            world.setBlock(x, y, z, BlockType.BEDROCK);
                        }
                    } else if (y > EXIT_PORTAL_Y) {
                        world.setBlock(x, y, z, BlockType.AIR);
                    } else if (distanceSquared > 6.25D) {
                        world.setBlock(x, y, z, BlockType.BEDROCK);
                    } else {
                        world.setBlock(x, y, z, BlockType.END_PORTAL);
                    }
                }
            }
        }

        world.setBlock(cx, EXIT_PORTAL_Y, cz, BlockType.BEDROCK);
        world.setBlock(cx, EXIT_PORTAL_Y + 1, cz, BlockType.BEDROCK);
        world.setBlock(cx, EXIT_PORTAL_Y + 2, cz, BlockType.BEDROCK);
        world.setBlock(cx - 1, EXIT_PORTAL_Y + 2, cz, BlockType.TORCH,
                BlockShape.torchMetadataFromFace(Block.FACE_WEST));
        world.setBlock(cx + 1, EXIT_PORTAL_Y + 2, cz, BlockType.TORCH,
                BlockShape.torchMetadataFromFace(Block.FACE_EAST));
        world.setBlock(cx, EXIT_PORTAL_Y + 2, cz - 1, BlockType.TORCH,
                BlockShape.torchMetadataFromFace(Block.FACE_NORTH));
        world.setBlock(cx, EXIT_PORTAL_Y + 2, cz + 1, BlockType.TORCH,
                BlockShape.torchMetadataFromFace(Block.FACE_SOUTH));
        world.setBlock(cx, EXIT_PORTAL_Y + 3, cz, BlockType.BEDROCK);
        world.setBlock(cx, EXIT_PORTAL_Y + 4, cz, BlockType.DRAGON_EGG);
    }

    @Override
    public void dropLoot() {
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/enderdragon/dragon.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.DRAGON;
    }

    public int getDeathTicks() {
        return deathTicks;
    }

    public float getTargetX() {
        return targetX;
    }

    public float getTargetY() {
        return targetY;
    }

    public float getTargetZ() {
        return targetZ;
    }

    public int getTargetCooldown() {
        return targetCooldown;
    }

    public double[] getMovementOffset(int ticksBack, float partialTick) {
        if (movementHistoryIndex < 0) {
            initializeMovementHistory();
        }
        int clampedTicksBack = Math.max(0, ticksBack);
        float interpolation = health <= 0.0f ? 1.0f : 1.0f - clamp(partialTick, 0.0f, 1.0f);
        int currentIndex = (movementHistoryIndex - clampedTicksBack) & (MOVEMENT_HISTORY_SIZE - 1);
        int previousIndex = (movementHistoryIndex - clampedTicksBack - 1) & (MOVEMENT_HISTORY_SIZE - 1);
        double currentYaw = movementHistory[currentIndex][0];
        double previousYawDelta = wrapDegreesLocal(movementHistory[previousIndex][0] - currentYaw);
        double currentY = movementHistory[currentIndex][1];
        double previousYDelta = movementHistory[previousIndex][1] - currentY;
        double currentPitch = movementHistory[currentIndex][2];
        double previousPitchDelta = movementHistory[previousIndex][2] - currentPitch;
        return new double[] {
                currentYaw + previousYawDelta * interpolation,
                currentY + previousYDelta * interpolation,
                currentPitch + previousPitchDelta * interpolation
        };
    }

    public void setFlightState(float targetX, float targetY, float targetZ, int targetCooldown) {
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.targetCooldown = Math.max(0, targetCooldown);
    }

    public void setDeathState(int deathTicks, boolean deathStarted) {
        this.deathTicks = Math.max(0, deathTicks);
        if (deathStarted) {
            this.health = 0.0f;
            this.dead = true;
        }
    }

    @Override
    public void setPosition(float x, float y, float z) {
        super.setPosition(x, y, z);
        movementHistoryIndex = -1;
    }

    @Override
    public void setYaw(float yaw) {
        super.setYaw(yaw);
        this.bodyYaw = this.yaw;
        this.prevYaw = this.yaw;
        this.prevBodyYaw = this.yaw;
        movementHistoryIndex = -1;
    }

    @Override
    public void setRenderBodyYaw(float bodyYaw) {
        super.setRenderBodyYaw(bodyYaw);
        movementHistoryIndex = -1;
    }

    private void initializeMovementHistory() {
        for (int i = 0; i < movementHistory.length; i++) {
            movementHistory[i][0] = yaw;
            movementHistory[i][1] = y;
            movementHistory[i][2] = pitch;
        }
        movementHistoryIndex = 0;
    }

    private void recordMovementHistory() {
        if (movementHistoryIndex < 0) {
            initializeMovementHistory();
        }
        movementHistoryIndex = (movementHistoryIndex + 1) & (MOVEMENT_HISTORY_SIZE - 1);
        movementHistory[movementHistoryIndex][0] = yaw;
        movementHistory[movementHistoryIndex][1] = y;
        movementHistory[movementHistoryIndex][2] = pitch;
    }

    private static float updateRotation(float current, float target, float maxTurn) {
        float delta = wrapDegreesLocal(target - current);
        if (delta > maxTurn) {
            delta = maxTurn;
        }
        if (delta < -maxTurn) {
            delta = -maxTurn;
        }
        return wrapDegreesLocal(current + delta);
    }

    private static float wrapDegreesLocal(double degrees) {
        double wrapped = degrees % 360.0;
        if (wrapped >= 180.0) {
            wrapped -= 360.0;
        }
        if (wrapped < -180.0) {
            wrapped += 360.0;
        }
        return (float) wrapped;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}

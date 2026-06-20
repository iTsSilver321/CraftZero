package com.craftzero.entity;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.BlockType;
import com.craftzero.world.RailShapeResolver;
import com.craftzero.world.RedstoneEngine;

public class MinecartEntity extends Entity {
    public enum CartKind {
        RIDEABLE,
        CHEST,
        FURNACE
    }

    private static final float MAX_RAIL_SPEED = 0.40f;
    private static final float RAIL_DRAG = 0.996f;

    private final CartKind kind;
    private int rollingAmplitude;
    private float damage;

    public MinecartEntity(CartKind kind) {
        super(0.98f, 0.70f);
        this.kind = kind == null ? CartKind.RIDEABLE : kind;
    }

    public MinecartEntity(float x, float y, float z, CartKind kind) {
        this(kind);
        setPosition(x, y, z);
    }

    @Override
    public void tick() {
        super.tick();
        if (rollingAmplitude > 0) {
            rollingAmplitude--;
        }
    }

    @Override
    public void updatePhysics(float deltaTime) {
        if (world == null || removed) {
            return;
        }
        int railX = (int) Math.floor(x);
        int railZ = (int) Math.floor(z);
        int railY = RailShapeResolver.findRailY(world, railX, (int) Math.floor(y), railZ);
        if (railY == Integer.MIN_VALUE) {
            super.updatePhysics(deltaTime);
            return;
        }

        BlockType rail = world.getBlockIfLoaded(railX, railY, railZ, BlockType.AIR);
        int metadata = world.getBlockMetadataIfLoaded(railX, railY, railZ, 0);
        int shape = metadata & 7;
        if (rail == BlockType.RAIL && metadata >= RailShapeResolver.CURVE_SOUTH_EAST) {
            shape = metadata;
        }

        y = railY + 0.0625f;
        motionY = 0.0f;
        applyRailShape(shape);

        if (rail == BlockType.POWERED_RAIL) {
            if ((metadata & RedstoneEngine.RAIL_POWERED_BIT) != 0) {
                acceleratePoweredRail(shape);
            } else {
                motionX *= 0.5f;
                motionZ *= 0.5f;
            }
        }

        applySpecialCartForces();
        clampHorizontalSpeed();

        x += motionX;
        z += motionZ;
        yaw = (float) Math.toDegrees(Math.atan2(motionX, -motionZ));

        motionX *= RAIL_DRAG;
        motionZ *= RAIL_DRAG;
        RedstoneEngine.updateRailDetectorForCart(world, this);
    }

    private void applyRailShape(int shape) {
        if (RailShapeResolver.isNorthSouth(shape)) {
            motionX = 0.0f;
            x = (float) Math.floor(x) + 0.5f;
        } else if (RailShapeResolver.isEastWest(shape)) {
            motionZ = 0.0f;
            z = (float) Math.floor(z) + 0.5f;
        }

        switch (shape) {
            case RailShapeResolver.ASCENDING_EAST -> {
                motionX += 0.0075f;
                y += (float) ((x - Math.floor(x)) * 0.5f);
            }
            case RailShapeResolver.ASCENDING_WEST -> {
                motionX -= 0.0075f;
                y += (float) ((1.0f - (x - Math.floor(x))) * 0.5f);
            }
            case RailShapeResolver.ASCENDING_NORTH -> {
                motionZ -= 0.0075f;
                y += (float) ((1.0f - (z - Math.floor(z))) * 0.5f);
            }
            case RailShapeResolver.ASCENDING_SOUTH -> {
                motionZ += 0.0075f;
                y += (float) ((z - Math.floor(z)) * 0.5f);
            }
            case RailShapeResolver.CURVE_SOUTH_EAST -> turnCurve(1, 1);
            case RailShapeResolver.CURVE_SOUTH_WEST -> turnCurve(-1, 1);
            case RailShapeResolver.CURVE_NORTH_WEST -> turnCurve(-1, -1);
            case RailShapeResolver.CURVE_NORTH_EAST -> turnCurve(1, -1);
            default -> {
            }
        }
    }

    private void turnCurve(int sx, int sz) {
        float speed = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (speed < 0.01f) {
            return;
        }
        motionX = sx * speed * 0.7071f;
        motionZ = sz * speed * 0.7071f;
    }

    private void acceleratePoweredRail(int shape) {
        float speed = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (speed < 0.01f) {
            if (RailShapeResolver.isEastWest(shape)) {
                motionX = 0.04f;
            } else {
                motionZ = 0.04f;
            }
            return;
        }
        motionX += motionX / speed * 0.06f;
        motionZ += motionZ / speed * 0.06f;
    }

    protected void applySpecialCartForces() {
    }

    private void clampHorizontalSpeed() {
        float speed = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (speed > MAX_RAIL_SPEED) {
            motionX = motionX / speed * MAX_RAIL_SPEED;
            motionZ = motionZ / speed * MAX_RAIL_SPEED;
        }
    }

    public void bump(float amount) {
        damage += amount;
        rollingAmplitude = 10;
    }

    public void dropAsItem() {
        if (world != null) {
            world.spawnThrownStack(x, y + 0.25f, z, new ItemStack(getItemType(), 1), 0.0f, 0.15f, 0.0f);
        }
        remove();
    }

    public CartKind getKind() {
        return kind;
    }

    public ItemType getItemType() {
        return switch (kind) {
            case CHEST -> ItemType.CHEST_MINECART;
            case FURNACE -> ItemType.FURNACE_MINECART;
            default -> ItemType.MINECART;
        };
    }

    public int getRollingAmplitude() {
        return rollingAmplitude;
    }

    public float getDamage() {
        return damage;
    }

    public void setDamage(float damage) {
        this.damage = Math.max(0.0f, damage);
    }
}

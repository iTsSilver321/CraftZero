package com.craftzero.entity;

import com.craftzero.world.BlockType;
import com.craftzero.world.RailShapeResolver;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FurnaceMinecartEntityTest {
    @Test
    @DisplayName("Coal fuel adds Java 1.0 furnace minecart duration and caps at short max")
    void addFuelAddsLegacyDurationAndCaps() {
        FurnaceMinecartEntity cart = new FurnaceMinecartEntity(2.0f, 0.0f, 0.0f);

        cart.addFuel(0.0f, 0.0f);
        assertEquals(FurnaceMinecartEntity.FUEL_TICKS_PER_COAL, cart.getFuelTicks());

        cart.setFuelTicks(FurnaceMinecartEntity.MAX_FUEL_TICKS - 10);
        cart.addFuel(0.0f, 0.0f);

        assertEquals(FurnaceMinecartEntity.MAX_FUEL_TICKS, cart.getFuelTicks());
    }

    @Test
    @DisplayName("Using a furnace minecart stores the raw Release-style push delta")
    void setPushDirectionStoresRawDeltaFromPlayer() {
        FurnaceMinecartEntity cart = new FurnaceMinecartEntity(2.0f, 0.0f, 0.0f);

        cart.setPushDirectionFrom(0.0f, 0.0f);

        assertEquals(2.0f, cart.getPushX(), 0.0001f);
        assertEquals(0.0f, cart.getPushZ(), 0.0001f);
        assertEquals(0, cart.getFuelTicks());
    }

    @Test
    @DisplayName("Using a furnace minecart from its center should clear stale push direction")
    void setPushDirectionFromSamePositionClearsStaleDirection() {
        FurnaceMinecartEntity cart = new FurnaceMinecartEntity(2.0f, 0.0f, 0.0f);
        cart.setPush(1.0f, 0.0f);

        cart.setPushDirectionFrom(2.0f, 0.0f);

        assertEquals(0.0f, cart.getPushX(), 0.0001f);
        assertEquals(0.0f, cart.getPushZ(), 0.0001f);
    }

    @Test
    @DisplayName("Raw furnace minecart push vectors should apply post-move engine force")
    void rawPushVectorAppliesPostMoveEngineForce() {
        World world = new World(6108L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);
            FurnaceMinecartEntity cart = new FurnaceMinecartEntity(0.5f, 70.1f, 0.5f);
            cart.setFuelTicks(20);
            cart.setPush(2.0f, 0.0f);
            world.replaceEntities(java.util.List.of(cart));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(0.5f, cart.getX(), 0.0001f);
            assertEquals(0.05f, cart.getMotionX(), 0.0001f);
            assertEquals(0.05f, cart.getPushX(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Unfueled furnace minecarts should use the source furnace-cart drag")
    void unfueledFurnaceMinecartUsesSourceDrag() {
        World world = new World(6109L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);
            FurnaceMinecartEntity cart = new FurnaceMinecartEntity(0.5f, 70.1f, 0.5f);
            cart.setMotion(0.2f, 0.0f, 0.0f);
            world.replaceEntities(java.util.List.of(cart));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(0.7f, cart.getX(), 0.0001f);
            assertEquals(0.196f, cart.getMotionX(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Loaded furnace minecart fuel is clamped to Java 1.0 max")
    void setFuelTicksClampsToLegacyRange() {
        FurnaceMinecartEntity cart = new FurnaceMinecartEntity();

        cart.setFuelTicks(-20);
        assertEquals(0, cart.getFuelTicks());

        cart.setFuelTicks(FurnaceMinecartEntity.MAX_FUEL_TICKS + 1);
        assertEquals(FurnaceMinecartEntity.MAX_FUEL_TICKS, cart.getFuelTicks());
    }

    @Test
    @DisplayName("Expired fuel clears furnace minecart push vector")
    void expiredFuelClearsPushVector() {
        FurnaceMinecartEntity cart = new FurnaceMinecartEntity();
        cart.setFuelTicks(1);
        cart.setPush(1.0f, 0.0f);

        cart.tick();

        assertEquals(0, cart.getFuelTicks());
        assertEquals(0.0f, cart.getPushX(), 0.0001f);
        assertEquals(0.0f, cart.getPushZ(), 0.0001f);
    }

    @Test
    @DisplayName("Powered furnace minecarts emit Release-style exhaust smoke")
    void poweredFurnaceMinecartEmitsExhaustSmoke() {
        World world = new RandomOverrideWorld(6110L, fixedNextInt(0));
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);
            FurnaceMinecartEntity cart = new FurnaceMinecartEntity(0.5f, 70.1f, 0.5f);
            cart.setFuelTicks(2);
            cart.setPush(2.0f, 0.0f);
            world.replaceEntities(java.util.List.of(cart));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(1, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.LARGE_SMOKE)
                    .count());
            WorldParticle smoke = world.getParticles().get(0);
            assertEquals(WorldParticle.Type.LARGE_SMOKE, smoke.getType());
            assertEquals(cart.getX(), smoke.getRenderX(1.0f), 0.0001f);
            assertEquals(cart.getY() + 0.8f, smoke.getRenderY(1.0f), 0.0001f);
            assertEquals(cart.getZ(), smoke.getRenderZ(1.0f), 0.0001f);
            assertEquals(0.30f, smoke.getScale(1.0f), 0.0001f);
            assertEquals(22.0f, smoke.getLifetimeTicks(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Furnace minecart smoke stops when the last fuel tick is consumed")
    void furnaceMinecartDoesNotSmokeAfterFuelExpires() {
        World world = new RandomOverrideWorld(6111L, fixedNextInt(0));
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);
            FurnaceMinecartEntity cart = new FurnaceMinecartEntity(0.5f, 70.1f, 0.5f);
            cart.setFuelTicks(1);
            cart.setPush(2.0f, 0.0f);
            world.replaceEntities(java.util.List.of(cart));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(0, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.LARGE_SMOKE)
                    .count());
        } finally {
            world.cleanup();
        }
    }

    private static Random fixedNextInt(int value) {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                return value;
            }
        };
    }

    private static final class RandomOverrideWorld extends World {
        private final Random random;

        private RandomOverrideWorld(long seed, Random random) {
            super(seed);
            this.random = random;
        }

        @Override
        public Random getRandom() {
            return random;
        }
    }
}

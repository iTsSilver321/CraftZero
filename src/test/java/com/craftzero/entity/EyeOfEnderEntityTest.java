package com.craftzero.entity;

import com.craftzero.inventory.ItemType;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EyeOfEnderEntityTest {
    @Test
    @DisplayName("Eye of Ender far targets should become a short rising Release 1.0 waypoint")
    void farTargetsBecomeShortRisingWaypoint() {
        EyeOfEnderEntity eye = new EyeOfEnderEntity(0.0f, 64.0f, 0.0f,
                120.0f, 32.0f, 0.0f, true);

        eye.moveTowards(120.0f, 32.0f, 0.0f);

        assertEquals(12.0f, eye.getTargetX(), 0.001f);
        assertEquals(72.0f, eye.getTargetY(), 0.001f);
        assertEquals(0.0f, eye.getTargetZ(), 0.001f);
    }

    @Test
    @DisplayName("Eye of Ender nearby targets should keep the final stronghold point")
    void nearbyTargetsKeepFinalPoint() {
        EyeOfEnderEntity eye = new EyeOfEnderEntity(10.0f, 64.0f, -4.0f,
                14.5f, 41.0f, -7.5f, true);

        eye.moveTowards(14.5f, 41.0f, -7.5f);

        assertEquals(14.5f, eye.getTargetX(), 0.001f);
        assertEquals(41.0f, eye.getTargetY(), 0.001f);
        assertEquals(-7.5f, eye.getTargetZ(), 0.001f);
    }

    @Test
    @DisplayName("Eye of Ender should emit Release-style portal trail particles while flying")
    void emitsPortalTrailParticlesWhileFlying() {
        World world = new World(6239L);
        try {
            EyeOfEnderEntity eye = new EyeOfEnderEntity(8.0f, 82.0f, 1.0f,
                    12.0f, 86.0f, 4.0f, true);
            world.replaceEntities(List.of(eye));

            world.updateEntities(1.0f / 20.0f);

            long portalParticles = portalParticleCount(world);
            assertEquals(1, portalParticles);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Eye of Ender should emit bubble trail particles while flying through water")
    void emitsBubbleTrailParticlesWhileInWater() {
        World world = new World(6243L);
        try {
            world.setBlock(8, 82, 1, BlockType.WATER, 0);
            EyeOfEnderEntity eye = new EyeOfEnderEntity(8.0f, 82.0f, 1.0f,
                    12.0f, 86.0f, 4.0f, true);
            world.replaceEntities(List.of(eye));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(4, particleCount(world, WorldParticle.Type.BUBBLE));
            assertEquals(0, portalParticleCount(world));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Eye of Ender should use the Release source steering curve")
    void sourceSteeringCurveControlsMotionBeforePositionAdvances() {
        World world = new World(6240L);
        try {
            EyeOfEnderEntity eye = new EyeOfEnderEntity(0.0f, 64.0f, 0.0f,
                    12.0f, 72.0f, 0.0f, true);
            world.replaceEntities(List.of(eye));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(0.0f, eye.getX(), 0.0001f);
            assertEquals(64.0f, eye.getY(), 0.0001f);
            assertEquals(0.0f, eye.getZ(), 0.0001f);
            assertEquals(0.03f, eye.getMotionX(), 0.0001f);
            assertEquals(0.015f, eye.getMotionY(), 0.0001f);
            assertEquals(0.0f, eye.getMotionZ(), 0.0001f);

            world.updateEntities(1.0f / 20.0f);

            assertEquals(0.03f, eye.getX(), 0.0001f);
            assertEquals(64.015f, eye.getY(), 0.0001f);
            assertEquals(0.0f, eye.getZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Eye of Ender should drop itself after the Release 1.0 lifetime when the drop roll succeeds")
    void dropsAfterLifetimeWhenDropRollSucceeds() {
        World world = new World(6237L);
        try {
            EyeOfEnderEntity eye = new EyeOfEnderEntity(8.0f, 82.0f, 1.0f,
                    12.0f, 86.0f, 4.0f, true);
            world.replaceEntities(List.of(eye));

            for (int i = 0; i < 80; i++) {
                world.updateEntities(1.0f / 20.0f);
            }

            assertFalse(eye.isRemoved());
            assertTrue(world.getDroppedItems().stream()
                    .noneMatch(item -> item.getItemType() == ItemType.EYE_OF_ENDER));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(eye.isRemoved());
            assertFalse(world.getEntities().stream().anyMatch(EyeOfEnderEntity.class::isInstance));
            assertTrue(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == ItemType.EYE_OF_ENDER && item.getCount() == 1));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Eye of Ender should shatter without dropping when the Release 1.0 drop roll fails")
    void shattersWithoutDropWhenDropRollFails() {
        World world = new World(6238L);
        try {
            EyeOfEnderEntity eye = new EyeOfEnderEntity(8.0f, 82.0f, 1.0f,
                    12.0f, 86.0f, 4.0f, false);
            world.replaceEntities(List.of(eye));

            eye.setTicksExisted(EyeOfEnderEntity.LIFE_TICKS);
            world.updateEntities(1.0f / 20.0f);

            assertTrue(eye.isRemoved());
            assertTrue(world.getDroppedItems().stream()
                    .noneMatch(item -> item.getItemType() == ItemType.EYE_OF_ENDER));
            assertEquals(81, portalParticleCount(world));
            assertEquals(8, particleCount(world, WorldParticle.Type.ITEM_CRACK));
            assertEyeOfEnderShatterShape(world.getParticles(), 8.5f, 82.0f, 1.5f);
        } finally {
            world.cleanup();
        }
    }

    private static void assertEyeOfEnderShatterShape(List<WorldParticle> particles,
            float centerX, float centerY, float centerZ) {
        assertEquals(89, particles.size());
        for (int i = 1; i <= 8; i++) {
            WorldParticle chip = particles.get(i);
            assertEquals(WorldParticle.Type.ITEM_CRACK, chip.getType());
            assertEquals(ItemType.EYE_OF_ENDER, chip.getItemParticleType());
            assertEquals(centerX, chip.getRenderX(0.0f), 0.0001f);
            assertEquals(centerY, chip.getRenderY(0.0f), 0.0001f);
            assertEquals(centerZ, chip.getRenderZ(0.0f), 0.0001f);
        }
        for (int i = 9; i < particles.size(); i += 2) {
            WorldParticle inner = particles.get(i);
            WorldParticle outer = particles.get(i + 1);
            assertEquals(WorldParticle.Type.PORTAL, inner.getType());
            assertEquals(WorldParticle.Type.PORTAL, outer.getType());
            assertEquals(inner.getRenderX(0.0f), outer.getRenderX(0.0f), 0.0001f);
            assertEquals(centerY - 0.4f, inner.getRenderY(0.0f), 0.0001f);
            assertEquals(centerY - 0.4f, outer.getRenderY(0.0f), 0.0001f);
            assertEquals(inner.getRenderZ(0.0f), outer.getRenderZ(0.0f), 0.0001f);

            float dx = inner.getRenderX(0.0f) - centerX;
            float dz = inner.getRenderZ(0.0f) - centerZ;
            assertEquals(25.0f, dx * dx + dz * dz, 0.0001f);
            assertEquals(-dx, inner.getMotionX(), 0.0001f);
            assertEquals(-dz, inner.getMotionZ(), 0.0001f);
            assertEquals(-dx * 1.4f, outer.getMotionX(), 0.0001f);
            assertEquals(-dz * 1.4f, outer.getMotionZ(), 0.0001f);
        }
    }

    private static long portalParticleCount(World world) {
        return particleCount(world, WorldParticle.Type.PORTAL);
    }

    private static long particleCount(World world, WorldParticle.Type type) {
        return world.getParticles().stream()
                .filter(particle -> particle.getType() == type)
                .count();
    }
}

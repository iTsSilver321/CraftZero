package com.craftzero.graphics.model;

import com.craftzero.entity.mob.EnderDragon;
import com.craftzero.world.Dimension;
import com.craftzero.world.World;
import com.craftzero.world.WorldGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DragonModelTest {
    @Test
    @DisplayName("Dragon model should expose articulated neck and tail chains")
    void dragonModelExposesSegmentChains() {
        DragonModel model = new DragonModel();

        assertEquals(3, model.neckSegments().length);
        assertEquals(5, model.tailSegments().length);
    }

    @Test
    @DisplayName("Dragon articulation should follow Release-style movement history")
    void dragonArticulationFollowsMovementHistory() {
        World world = new World(8030L, WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        try {
            EnderDragon dragon = eastboundDragon();
            world.replaceEntities(List.of(dragon));

            advanceEntityTicks(world, 12);

            DragonModel.DragonArticulation articulation =
                    DragonModel.articulationFor(dragon, dragon.getTicksExisted(), 1.0f, 0.0f);

            assertTrue(hasPoseBend(articulation.neckSegments()));
            assertTrue(hasYawBend(articulation.tailSegments()));
            assertTrue(articulation.leftWingRoll() < -0.25f);
            assertTrue(articulation.rightWingRoll() > 0.25f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dragon model animation should apply history articulation to visible parts")
    void dragonAnimationAppliesArticulationToModelParts() {
        World world = new World(8031L, WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        try {
            EnderDragon dragon = eastboundDragon();
            world.replaceEntities(List.of(dragon));
            advanceEntityTicks(world, 12);

            DragonModel model = new DragonModel();
            DragonModel.DragonArticulation articulation =
                    DragonModel.articulationFor(dragon, dragon.getTicksExisted(), 1.0f, 0.0f);

            model.animate(dragon, dragon.getTicksExisted(), 1.0f, 0.0f);

            assertEquals(articulation.neckSegments()[0].yaw(),
                    model.neckSegments()[0].getRotationY(), 0.0001f);
            assertEquals(articulation.tailSegments()[2].yaw(),
                    model.tailSegments()[2].getRotationY(), 0.0001f);
            assertEquals(articulation.leftWingRoll(), model.leftWing().getRotationZ(), 0.0001f);
            assertEquals(articulation.rightWingRoll(), model.rightWing().getRotationZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    private static EnderDragon eastboundDragon() {
        EnderDragon dragon = new EnderDragon();
        dragon.setPosition(0.5f, 80.0f, 0.5f);
        dragon.setRenderBodyYaw(0.0f);
        dragon.setFlightState(64.0f, 82.0f, 0.5f, 40);
        return dragon;
    }

    private static void advanceEntityTicks(World world, int ticks) {
        for (int i = 0; i < ticks; i++) {
            world.updateEntities(0.0f);
        }
    }

    private static boolean hasYawBend(DragonModel.DragonSegmentPose[] poses) {
        for (DragonModel.DragonSegmentPose pose : poses) {
            if (Math.abs(pose.yaw()) > 0.001f) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasPoseBend(DragonModel.DragonSegmentPose[] poses) {
        for (DragonModel.DragonSegmentPose pose : poses) {
            if (Math.abs(pose.yaw()) > 0.001f
                    || Math.abs(pose.pitch()) > 0.001f
                    || Math.abs(pose.roll()) > 0.001f) {
                return true;
            }
        }
        return false;
    }
}

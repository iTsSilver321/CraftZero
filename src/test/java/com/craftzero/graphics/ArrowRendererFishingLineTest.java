package com.craftzero.graphics;

import com.craftzero.entity.FishingHookEntity;
import com.craftzero.entity.EndCrystalEntity;
import com.craftzero.entity.mob.EnderDragon;
import com.craftzero.main.Player;
import com.craftzero.world.Block;
import com.craftzero.world.BlockType;
import com.craftzero.world.Dimension;
import com.craftzero.world.World;
import com.craftzero.world.WorldGenerator;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArrowRendererFishingLineTest {
    @Test
    @DisplayName("Fishing line segment starts near the rod hand and ends at the bobber")
    void fishingLineSegmentUsesOwnerHandAnchorAndBobberEndpoint() {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        FishingHookEntity hook = new FishingHookEntity(2.0f, 71.0f, -3.0f,
                0.0f, 0.0f, 0.0f, player);

        ArrowRenderer.FishingLineSegment segment = ArrowRenderer.fishingLineSegment(hook, 0.0f);

        assertNotNull(segment);
        assertEquals(0.35f, segment.start().x, 0.001f);
        assertEquals(71.45f, segment.start().y, 0.001f);
        assertEquals(-0.25f, segment.start().z, 0.001f);
        assertEquals(2.0f, segment.end().x, 0.001f);
        assertEquals(71.0f, segment.end().y, 0.001f);
        assertEquals(-3.0f, segment.end().z, 0.001f);
        assertTrue(segment.start().distanceSquared(segment.end()) > 0.0f);
    }

    @Test
    @DisplayName("Fishing line segment is skipped for removed hooks")
    void fishingLineSegmentSkipsRemovedHook() {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        FishingHookEntity hook = new FishingHookEntity(2.0f, 71.0f, -3.0f,
                0.0f, 0.0f, 0.0f, player);

        hook.remove();

        assertNull(ArrowRenderer.fishingLineSegment(hook, 0.0f));
    }

    @Test
    @DisplayName("Fishing line start lowers for sneaking players")
    void fishingLineStartLowersForSneakingPlayer() throws Exception {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        Vector3f standing = ArrowRenderer.fishingLineStart(player, 0.0f);
        setSneaking(player, true);

        Vector3f sneaking = ArrowRenderer.fishingLineStart(player, 0.0f);

        assertTrue(sneaking.y < standing.y);
    }

    @Test
    @DisplayName("Boat entity mesh should use a real five-box hull instead of the flat item quad")
    void boatEntityMeshUsesFiveBoxHullGeometry() {
        ArrowRenderer.MeshData mesh = ArrowRenderer.boatMeshData();

        assertEquals(5 * 6 * 4 * 11, mesh.vertices().length);
        assertEquals(5 * 6 * 6, mesh.indices().length);
        assertEquals(-0.68f, minCoordinate(mesh.vertices(), 0), 0.0001f);
        assertEquals(0.68f, maxCoordinate(mesh.vertices(), 0), 0.0001f);
        assertEquals(0.0f, minCoordinate(mesh.vertices(), 1), 0.0001f);
        assertEquals(0.50f, maxCoordinate(mesh.vertices(), 1), 0.0001f);
        assertEquals(-1.02f, minCoordinate(mesh.vertices(), 2), 0.0001f);
        assertEquals(1.02f, maxCoordinate(mesh.vertices(), 2), 0.0001f);
    }

    @Test
    @DisplayName("Minecart entity mesh should use an open tub instead of the flat item quad")
    void minecartEntityMeshUsesOpenTubGeometry() {
        ArrowRenderer.MeshData mesh = ArrowRenderer.minecartMeshData();

        assertEquals(5 * 6 * 4 * 11, mesh.vertices().length);
        assertEquals(5 * 6 * 6, mesh.indices().length);
        assertEquals(-0.58f, minCoordinate(mesh.vertices(), 0), 0.0001f);
        assertEquals(0.58f, maxCoordinate(mesh.vertices(), 0), 0.0001f);
        assertEquals(0.0f, minCoordinate(mesh.vertices(), 1), 0.0001f);
        assertEquals(0.62f, maxCoordinate(mesh.vertices(), 1), 0.0001f);
        assertEquals(-0.58f, minCoordinate(mesh.vertices(), 2), 0.0001f);
        assertEquals(0.58f, maxCoordinate(mesh.vertices(), 2), 0.0001f);
    }

    @Test
    @DisplayName("Chest minecart payload should sit inside the cart and rise above the rails")
    void chestMinecartPayloadUsesChestBoxGeometry() {
        ArrowRenderer.MeshData mesh = ArrowRenderer.chestMinecartPayloadMeshData();

        assertEquals(2 * 6 * 4 * 11, mesh.vertices().length);
        assertEquals(2 * 6 * 6, mesh.indices().length);
        assertEquals(-0.40f, minCoordinate(mesh.vertices(), 0), 0.0001f);
        assertEquals(0.40f, maxCoordinate(mesh.vertices(), 0), 0.0001f);
        assertEquals(0.20f, minCoordinate(mesh.vertices(), 1), 0.0001f);
        assertEquals(0.72f, maxCoordinate(mesh.vertices(), 1), 0.0001f);
        assertEquals(-0.40f, minCoordinate(mesh.vertices(), 2), 0.0001f);
        assertEquals(0.40f, maxCoordinate(mesh.vertices(), 2), 0.0001f);
    }

    @Test
    @DisplayName("Fueled furnace minecarts should use the lit furnace face")
    void furnaceMinecartPayloadSwitchesToLitFrontUv() {
        ArrowRenderer.MeshData cold = ArrowRenderer.furnaceMinecartPayloadMeshData(false);
        ArrowRenderer.MeshData lit = ArrowRenderer.furnaceMinecartPayloadMeshData(true);

        assertEquals(6 * 4 * 11, cold.vertices().length);
        assertEquals(6 * 6, cold.indices().length);
        assertEquals(cold.vertices().length, lit.vertices().length);
        assertEquals(cold.indices().length, lit.indices().length);
        assertTrue(anyUvDifference(cold.vertices(), lit.vertices()));
        assertEquals(-0.38f, minCoordinate(cold.vertices(), 0), 0.0001f);
        assertEquals(0.74f, maxCoordinate(cold.vertices(), 1), 0.0001f);
        assertEquals(0.38f, maxCoordinate(cold.vertices(), 2), 0.0001f);
    }

    @Test
    @DisplayName("End crystal render mesh should use 3D boxes instead of a flat sprite")
    void endCrystalMeshUsesThreeDimensionalBoxes() {
        ArrowRenderer.MeshData mesh = ArrowRenderer.crystalMeshData();

        assertEquals(3 * 6 * 4 * 11, mesh.vertices().length);
        assertEquals(3 * 6 * 6, mesh.indices().length);
        assertTrue(minCoordinate(mesh.vertices(), 0) < -0.5f);
        assertTrue(maxCoordinate(mesh.vertices(), 0) > 0.5f);
        assertEquals(0.0f, minCoordinate(mesh.vertices(), 1), 0.0001f);
        assertEquals(1.5f, maxCoordinate(mesh.vertices(), 1), 0.0001f);
        assertTrue(minCoordinate(mesh.vertices(), 2) < -0.5f);
        assertTrue(maxCoordinate(mesh.vertices(), 2) > 0.5f);
    }

    @Test
    @DisplayName("Fireball entity sprite should use the terrain fire tile")
    void fireballEntitySpriteUsesTerrainFireTile() {
        assertArrayEquals(BlockType.FIRE.getTextureCoords(Block.FACE_NORTH),
                ArrowRenderer.fireballSpriteUv(), 0.0001f);
    }

    @Test
    @DisplayName("End crystal animation should tick and interpolate like the Release inner rotation")
    void endCrystalAnimationTicksAndInterpolates() {
        EndCrystalEntity crystal = new EndCrystalEntity(2.0f, 80.0f, -3.0f);

        assertEquals(0.0f, crystal.getRenderInnerRotation(0.5f), 0.0001f);

        crystal.tick();

        assertEquals(1, crystal.getInnerRotation());
        assertEquals(0.0f, crystal.getRenderInnerRotation(0.0f), 0.0001f);
        assertEquals(0.5f, crystal.getRenderInnerRotation(0.5f), 0.0001f);
        assertEquals(1.0f, crystal.getRenderInnerRotation(1.0f), 0.0001f);

        crystal.setTicksExisted(44);

        assertEquals(44, crystal.getInnerRotation());
        assertEquals(44.0f, crystal.getRenderInnerRotation(0.5f), 0.0001f);
    }

    @Test
    @DisplayName("End crystal renderer should bob and spin from the inner rotation")
    void endCrystalModelMatrixUsesReleaseBobAndSpin() {
        EndCrystalEntity crystal = new EndCrystalEntity(2.0f, 80.0f, -3.0f);
        for (int i = 0; i < 10; i++) {
            crystal.tick();
        }
        float animation = crystal.getRenderInnerRotation(0.5f);

        Matrix4f matrix = ArrowRenderer.endCrystalModelMatrix(crystal, 0.5f);
        Vector3f origin = transformedOrigin(matrix);

        assertEquals(2.0f, origin.x, 0.0001f);
        assertEquals(80.0f + ArrowRenderer.endCrystalBobOffset(animation), origin.y, 0.0001f);
        assertEquals(-3.0f, origin.z, 0.0001f);
        assertEquals(animation * 3.0f, ArrowRenderer.endCrystalRotationDegrees(animation), 0.0001f);
        assertTrue(ArrowRenderer.endCrystalBobOffset(animation) > 0.0f);
    }

    @Test
    @DisplayName("Dragon crystal beam segment connects the active crystal to the dragon body")
    void dragonCrystalBeamSegmentUsesActiveHealingCrystal() {
        World world = new World(18110L, WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        try {
            EnderDragon dragon = new EnderDragon();
            dragon.setPosition(0.0f, 80.0f, 0.0f);
            dragon.setHealth(100.0f);
            EndCrystalEntity near = new EndCrystalEntity(12.0f, 82.0f, 0.0f);
            EndCrystalEntity far = new EndCrystalEntity(24.0f, 82.0f, 0.0f);
            world.replaceEntities(List.of(dragon, far, near));

            advanceEntityTicks(world, 10);

            ArrowRenderer.DragonCrystalBeamSegment segment =
                    ArrowRenderer.dragonCrystalBeamSegment(dragon, 1.0f);

            assertNotNull(segment);
            assertTrue(dragon.isChargingFrom(near));
            assertEquals(near.getRenderX(1.0f), segment.start().x, 0.001f);
            assertEquals(near.getRenderY(1.0f) + near.getHeight() * 0.5f, segment.start().y, 0.001f);
            assertEquals(near.getRenderZ(1.0f), segment.start().z, 0.001f);
            assertEquals(dragon.getRenderX(1.0f), segment.end().x, 0.001f);
            assertEquals(dragon.getRenderY(1.0f) + dragon.getHeight() * 0.5f, segment.end().y, 0.001f);
            assertEquals(dragon.getRenderZ(1.0f), segment.end().z, 0.001f);
            assertTrue(segment.start().distanceSquared(segment.end()) > 0.0f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dragon crystal healing beam should render animated twisted strands")
    void dragonCrystalBeamSegmentsUseAnimatedTwistedStrands() {
        World world = new World(18111L, WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        try {
            EnderDragon dragon = new EnderDragon();
            dragon.setPosition(0.0f, 80.0f, 0.0f);
            dragon.setHealth(100.0f);
            EndCrystalEntity crystal = new EndCrystalEntity(12.0f, 82.0f, 0.0f);
            world.replaceEntities(List.of(dragon, crystal));

            advanceEntityTicks(world, 10);

            List<ArrowRenderer.DragonCrystalBeamSegment> early =
                    ArrowRenderer.dragonCrystalBeamSegments(dragon, 0.0f);
            List<ArrowRenderer.DragonCrystalBeamSegment> late =
                    ArrowRenderer.dragonCrystalBeamSegments(dragon, 1.0f);

            int expectedSegments = 1 + ArrowRenderer.DRAGON_BEAM_RING_COUNT * 2
                    + (ArrowRenderer.DRAGON_BEAM_RING_COUNT / 2 - 1);
            assertEquals(expectedSegments, early.size());
            assertEquals(expectedSegments, late.size());
            ArrowRenderer.DragonCrystalBeamSegment center = early.get(0);
            assertEquals(crystal.getRenderX(0.0f), center.start().x, 0.001f);
            assertEquals(crystal.getRenderY(0.0f) + crystal.getHeight() * 0.5f, center.start().y, 0.001f);
            assertEquals(crystal.getRenderZ(0.0f), center.start().z, 0.001f);
            assertEquals(dragon.getRenderX(0.0f), center.end().x, 0.001f);
            assertEquals(dragon.getRenderY(0.0f) + dragon.getHeight() * 0.5f, center.end().y, 0.001f);
            assertEquals(dragon.getRenderZ(0.0f), center.end().z, 0.001f);

            Vector3f firstRingCenter = new Vector3f(center.start())
                    .lerp(center.end(), 1.0f / ArrowRenderer.DRAGON_BEAM_RING_COUNT);
            assertTrue(early.get(1).end().distanceSquared(firstRingCenter) > 0.001f);
            assertTrue(early.get(1).end().distanceSquared(late.get(1).end()) > 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dragon crystal beam segment is skipped without an active crystal")
    void dragonCrystalBeamSegmentSkipsInactiveDragon() {
        EnderDragon dragon = new EnderDragon();
        dragon.setPosition(0.0f, 80.0f, 0.0f);

        assertNull(ArrowRenderer.dragonCrystalBeamSegment(dragon, 0.0f));
    }

    private static void setSneaking(Player player, boolean sneaking) throws Exception {
        Field field = Player.class.getDeclaredField("sneaking");
        field.setAccessible(true);
        field.setBoolean(player, sneaking);
    }

    private static void advanceEntityTicks(World world, int ticks) {
        for (int i = 0; i < ticks; i++) {
            world.updateEntities(1.0f / 20.0f);
        }
    }

    private static float minCoordinate(float[] vertices, int axis) {
        float min = Float.POSITIVE_INFINITY;
        for (int i = axis; i < vertices.length; i += 11) {
            min = Math.min(min, vertices[i]);
        }
        return min;
    }

    private static float maxCoordinate(float[] vertices, int axis) {
        float max = Float.NEGATIVE_INFINITY;
        for (int i = axis; i < vertices.length; i += 11) {
            max = Math.max(max, vertices[i]);
        }
        return max;
    }

    private static Vector3f transformedOrigin(Matrix4f matrix) {
        Vector3f point = new Vector3f();
        matrix.transformPosition(point);
        return point;
    }

    private static boolean anyUvDifference(float[] a, float[] b) {
        for (int i = 3; i < a.length; i += 11) {
            if (Math.abs(a[i] - b[i]) > 0.0001f || Math.abs(a[i + 1] - b[i + 1]) > 0.0001f) {
                return true;
            }
        }
        return false;
    }
}

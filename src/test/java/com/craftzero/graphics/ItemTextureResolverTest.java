package com.craftzero.graphics;

import com.craftzero.inventory.ItemRenderProfile;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.world.DayCycleManager;
import com.craftzero.world.Dimension;
import com.craftzero.world.World;
import com.craftzero.world.WorldGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemTextureResolverTest {
    @Test
    @DisplayName("Item sprite UVs should cover one 16x16 cell of items.png")
    void itemSpriteUvCoversSingleCell() {
        float[] uv = ItemTextureResolver.getUv(ItemType.BOW);

        assertEquals(1.0f / 16.0f, uv[2] - uv[0], 0.0001f);
        assertEquals(1.0f / 16.0f, uv[3] - uv[1], 0.0001f);
        assertTrue(uv[2] < 1.0f);
        assertTrue(uv[3] < 1.0f);

        float[] bucketUv = ItemTextureResolver.getUv(ItemType.WATER_BUCKET);
        assertEquals(1.0f / 16.0f, bucketUv[2] - bucketUv[0], 0.0001f);
        assertEquals(1.0f / 16.0f, bucketUv[3] - bucketUv[1], 0.0001f);
    }

    @Test
    @DisplayName("Drawing bows should use the Release-era pull sprite frames")
    void bowDrawUsesReleasePullSprites() {
        assertEquals(0, ItemTextureResolver.bowDrawFrame(ItemType.BOW, false, 1.0f));
        assertEquals(0, ItemTextureResolver.bowDrawFrame(ItemType.BOW, true, 0.0f));
        assertEquals(1, ItemTextureResolver.bowDrawFrame(ItemType.BOW, true, 0.01f));
        assertEquals(1, ItemTextureResolver.bowDrawFrame(ItemType.BOW, true, 13.0f / 20.0f));
        assertEquals(2, ItemTextureResolver.bowDrawFrame(ItemType.BOW, true, 0.66f));
        assertEquals(3, ItemTextureResolver.bowDrawFrame(ItemType.BOW, true, 18.0f / 20.0f));
        assertEquals(0, ItemTextureResolver.bowDrawFrame(ItemType.ARROW, true, 1.0f));

        assertArrayEquals(ItemTextureResolver.getItemsUv(5, 1),
                ItemTextureResolver.getUv(ItemType.BOW, false, 1.0f), 0.0001f);
        assertArrayEquals(ItemTextureResolver.getItemsUv(6, 1),
                ItemTextureResolver.getUv(ItemType.BOW, true, 0.01f), 0.0001f);
        assertArrayEquals(ItemTextureResolver.getItemsUv(7, 1),
                ItemTextureResolver.getUv(ItemType.BOW, true, 0.66f), 0.0001f);
        assertArrayEquals(ItemTextureResolver.getItemsUv(8, 1),
                ItemTextureResolver.getUv(ItemType.BOW, true, 0.90f), 0.0001f);
        assertArrayEquals(ItemTextureResolver.getUv(ItemType.ARROW),
                ItemTextureResolver.getUv(ItemType.ARROW, true, 1.0f), 0.0001f);
    }

    @Test
    @DisplayName("Clock dynamic state should follow Overworld day time")
    void clockDynamicStateFollowsOverworldTime() {
        World world = new World(9220L);
        DayCycleManager dayCycle = new DayCycleManager();
        world.setDayCycleManager(dayCycle);

        dayCycle.setTime(0.0f);
        assertEquals(0, ItemTextureResolver.dynamicItemState(ItemType.CLOCK, world, null).frame());
        assertEquals(0.0f, ItemTextureResolver.clockAngle(world), 0.0001f);

        dayCycle.setTime(6000.0f);
        assertEquals(16, ItemTextureResolver.dynamicItemState(ItemType.CLOCK, world, null).frame());

        dayCycle.setTime(18000.0f);
        assertEquals(48, ItemTextureResolver.dynamicItemState(ItemType.CLOCK, world, null).frame());
    }

    @Test
    @DisplayName("Compass dynamic state should point at world spawn relative to player yaw")
    void compassDynamicStateTracksSpawnRelativeToPlayerYaw() {
        World world = new World(9221L);
        world.setWorldSpawn(0, 70, -10);
        Player player = new Player(0.5f, 70.0f, 0.5f);
        player.setWorld(world);

        player.getCamera().setYaw(0.0f);
        assertEquals(0, ItemTextureResolver.dynamicItemState(ItemType.COMPASS, world, player).frame(),
                "Facing north toward spawn should point straight up");

        player.getCamera().setYaw(90.0f);
        assertEquals(48, ItemTextureResolver.dynamicItemState(ItemType.COMPASS, world, player).frame(),
                "Facing east with spawn to the left should rotate the needle counterclockwise");

        world.setWorldSpawn(10, 70, 0);
        player.getCamera().setYaw(0.0f);
        assertEquals(16, ItemTextureResolver.dynamicItemState(ItemType.COMPASS, world, player).frame(),
                "Spawn east of a north-facing player should point right");
    }

    @Test
    @DisplayName("Clock and compass should use deterministic spin in invalid dimensions")
    void dynamicUtilityItemsSpinInInvalidDimensions() {
        World nether = new World(9222L, WorldGenerator.RELEASE_ONE, Dimension.NETHER);
        DayCycleManager dayCycle = new DayCycleManager();
        dayCycle.setTime(6000.0f);
        nether.setDayCycleManager(dayCycle);
        Player player = new Player(0.0f, 70.0f, 0.0f);
        player.setWorld(nether);

        ItemTextureResolver.DynamicItemState clock = ItemTextureResolver.dynamicItemState(ItemType.CLOCK, nether, player);
        ItemTextureResolver.DynamicItemState compass = ItemTextureResolver.dynamicItemState(ItemType.COMPASS, nether, player);

        assertTrue(clock.active());
        assertTrue(compass.active());
        assertEquals(clock.frame(), compass.frame(),
                "Invalid-dimension utility items should share the same deterministic wobble source");
        assertNotEquals(16, clock.frame(),
                "Nether clock should not report the valid Overworld noon frame");
    }

    @Test
    @DisplayName("Item render profiles should distinguish blocks, tools, skinny items, and large sprites")
    void itemRenderProfilesClassifyHeldItems() {
        assertSame(ItemRenderProfile.ModelKind.BLOCK, ItemType.STONE.getRenderProfile().modelKind());
        assertSame(ItemRenderProfile.ModelKind.SPRITE, ItemType.BOW.getRenderProfile().modelKind());
        assertEquals(0.48f, ItemType.BOW.getRenderProfile().firstPersonScale(), 0.0001f);
        assertTrue(ItemType.BOW.getRenderProfile().firstPersonOffsetX() > 0.55f);
        assertEquals(-0.50f, ItemType.BOW.getRenderProfile().firstPersonOffsetY(), 0.0001f);
        assertTrue(ItemType.BOW.getRenderProfile().firstPersonOffsetZ() < -0.75f);
        assertTrue(ItemType.BOW.getRenderProfile().thirdPersonScale() >= 0.4f);
        assertEquals(0.42f, ItemType.ARROW.getRenderProfile().firstPersonScale(), 0.0001f);
        assertEquals(0.50f, ItemType.SIGN.getRenderProfile().firstPersonScale(), 0.0001f);
        assertSame(ItemRenderProfile.ModelKind.SPRITE, ItemType.WATER_BUCKET.getRenderProfile().modelKind());
        assertSame(ItemRenderProfile.ModelKind.SPRITE, ItemType.RAIL.getRenderProfile().modelKind());
        assertSame(ItemRenderProfile.ModelKind.SPRITE, ItemType.TRAPDOOR.getRenderProfile().modelKind());
        assertSame(ItemRenderProfile.ModelKind.SPRITE, ItemType.LEVER.getRenderProfile().modelKind());
        assertSame(ItemRenderProfile.ModelKind.SPRITE, ItemType.YELLOW_FLOWER.getRenderProfile().modelKind());
        assertTrue(ItemType.DIAMOND_SWORD.getRenderProfile().firstPersonRotX() > -20.0f,
                "Held tools should stay upright instead of pitching forward into the screen");
        assertEquals(-80.0f, ItemType.DIAMOND_SWORD.getRenderProfile().firstPersonRotY(), 0.0001f,
                "Held tools should yaw strongly so the screen-right side is clearly closer");
        assertEquals(30.0f, ItemType.DIAMOND_SWORD.getRenderProfile().firstPersonRotZ(), 0.0001f,
                "Mirrored held tools need a positive roll so the blade points upward instead of sideways");
    }
}

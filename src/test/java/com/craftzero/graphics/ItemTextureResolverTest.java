package com.craftzero.graphics;

import com.craftzero.inventory.ItemRenderProfile;
import com.craftzero.inventory.ItemType;
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
    @DisplayName("Item render profiles should distinguish blocks, tools, skinny items, and large sprites")
    void itemRenderProfilesClassifyHeldItems() {
        assertSame(ItemRenderProfile.ModelKind.BLOCK, ItemType.STONE.getRenderProfile().modelKind());
        assertSame(ItemRenderProfile.ModelKind.SPRITE, ItemType.BOW.getRenderProfile().modelKind());
        assertEquals(0.54f, ItemType.BOW.getRenderProfile().firstPersonScale(), 0.0001f);
        assertTrue(ItemType.BOW.getRenderProfile().firstPersonOffsetX() > 0.5f);
        assertTrue(ItemType.BOW.getRenderProfile().firstPersonOffsetY() < -0.4f);
        assertTrue(ItemType.BOW.getRenderProfile().thirdPersonScale() >= 0.4f);
        assertEquals(0.42f, ItemType.ARROW.getRenderProfile().firstPersonScale(), 0.0001f);
        assertEquals(0.45f, ItemType.SIGN.getRenderProfile().firstPersonScale(), 0.0001f);
        assertSame(ItemRenderProfile.ModelKind.SPRITE, ItemType.WATER_BUCKET.getRenderProfile().modelKind());
        assertSame(ItemRenderProfile.ModelKind.SPRITE, ItemType.RAIL.getRenderProfile().modelKind());
        assertSame(ItemRenderProfile.ModelKind.SPRITE, ItemType.TRAPDOOR.getRenderProfile().modelKind());
        assertSame(ItemRenderProfile.ModelKind.SPRITE, ItemType.LEVER.getRenderProfile().modelKind());
        assertSame(ItemRenderProfile.ModelKind.SPRITE, ItemType.YELLOW_FLOWER.getRenderProfile().modelKind());
        assertTrue(ItemType.DIAMOND_SWORD.getRenderProfile().firstPersonRotX() > -20.0f,
                "Held tools should stay upright instead of pitching forward into the screen");
        assertTrue(Math.abs(ItemType.DIAMOND_SWORD.getRenderProfile().firstPersonRotY()) <= 15.0f,
                "Held tools should only cant slightly inward, not yaw forward into the world");
        assertTrue(ItemType.DIAMOND_SWORD.getRenderProfile().firstPersonRotZ() < -25.0f,
                "Held tools should roll upward from the lower-right like classic Minecraft");
    }
}

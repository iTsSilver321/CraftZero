package com.craftzero.physics;

import com.craftzero.entity.FurnaceMinecartEntity;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaycastTest {
    @Test
    @DisplayName("Generic entity raycast can target minecarts while combat raycast ignores them")
    void genericRaycastTargetsMinecartWithoutMakingItCombatTarget() {
        FurnaceMinecartEntity cart = new FurnaceMinecartEntity(2.0f, 0.0f, 0.0f);
        Vector3f origin = new Vector3f(0.0f, 0.35f, 0.0f);
        Vector3f direction = new Vector3f(1.0f, 0.0f, 0.0f);

        Raycast.EntityRaycastResult useHit = Raycast.castAnyEntity(List.of(cart), origin, direction, 4.0f, null);
        Raycast.EntityRaycastResult combatHit = Raycast.castEntities(List.of(cart), origin, direction, 4.0f, null);

        assertTrue(useHit.hit);
        assertSame(cart, useHit.entity);
        assertFalse(combatHit.hit);
    }

    @Test
    @DisplayName("Bucket raycast should target source liquids without changing normal block selection")
    void fluidSourceRaycastTargetsSourceLiquidBeforeSolidBehindIt() {
        World world = new World(6280L);
        try {
            world.setBlock(0, 120, 2, BlockType.WATER, 0);
            world.setBlock(0, 120, 4, BlockType.STONE, 0);
            Vector3f origin = new Vector3f(0.5f, 120.5f, 0.5f);
            Vector3f direction = new Vector3f(0.0f, 0.0f, 1.0f);

            Raycast.RaycastResult normalHit = Raycast.cast(world, origin, direction, 6.0f);
            Raycast.RaycastResult fluidHit = Raycast.castFluidSource(world, origin, direction, 6.0f);

            assertTrue(normalHit.hit);
            assertEquals(4, normalHit.blockPos.z);
            assertTrue(fluidHit.hit);
            assertEquals(2, fluidHit.blockPos.z);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Bucket raycast should not see source liquids through selectable blocks")
    void fluidSourceRaycastStopsAtSelectableBlockBeforeSourceLiquid() {
        World world = new World(6281L);
        try {
            world.setBlock(0, 120, 1, BlockType.STONE, 0);
            world.setBlock(0, 120, 3, BlockType.WATER, 0);
            Vector3f origin = new Vector3f(0.5f, 120.5f, 0.5f);
            Vector3f direction = new Vector3f(0.0f, 0.0f, 1.0f);

            Raycast.RaycastResult fluidHit = Raycast.castFluidSource(world, origin, direction, 6.0f);

            assertFalse(fluidHit.hit);
        } finally {
            world.cleanup();
        }
    }
}

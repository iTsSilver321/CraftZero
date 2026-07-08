package com.craftzero.world.tile;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class FurnaceTileEntityTest {

    @Test
    @DisplayName("Furnace should consume fuel, smelt input, and toggle lit block")
    void furnaceSmeltsAndTogglesLitBlock() {
        World world = new World(4L);
        try {
            world.setBlock(0, 70, 0, BlockType.FURNACE, 3);
            FurnaceTileEntity furnace = (FurnaceTileEntity) world.getTileEntity(0, 70, 0);
            furnace.getInventory()[FurnaceTileEntity.SLOT_INPUT] = new ItemStack(ItemType.IRON_ORE, 1);
            furnace.getInventory()[FurnaceTileEntity.SLOT_FUEL] = new ItemStack(ItemType.COAL, 1);

            furnace.tick(world, 10.0f);

            assertSame(BlockType.LIT_FURNACE, world.getBlock(0, 70, 0));
            assertEquals(3, world.getBlockMetadata(0, 70, 0));
            assertNull(furnace.getInventory()[FurnaceTileEntity.SLOT_INPUT]);
            assertNull(furnace.getInventory()[FurnaceTileEntity.SLOT_FUEL]);
            assertSame(ItemType.IRON_INGOT, furnace.getInventory()[FurnaceTileEntity.SLOT_OUTPUT].getType());
            assertEquals(1, furnace.getInventory()[FurnaceTileEntity.SLOT_OUTPUT].getCount());
            assertTrue(furnace.getBurnTime() > 0);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Furnace tick should reconcile restored burn time with lit block state")
    void furnaceTickReconcilesRestoredBurnStateWithBlockState() {
        RecordingWorld world = new RecordingWorld(140L);
        try {
            world.setBlock(0, 70, 0, BlockType.FURNACE, 4);
            FurnaceTileEntity furnace = (FurnaceTileEntity) world.getTileEntity(0, 70, 0);
            furnace.getInventory()[FurnaceTileEntity.SLOT_INPUT] = new ItemStack(ItemType.IRON_ORE, 1);
            furnace.setBurnTime(20);
            furnace.setCurrentFuelBurnTime(1600);

            furnace.tick(world, 0.05f);

            assertSame(BlockType.LIT_FURNACE, world.getBlock(0, 70, 0));
            assertEquals(4, world.getBlockMetadata(0, 70, 0));
            assertSame(furnace, world.getTileEntity(0, 70, 0));
            assertEquals(19, furnace.getBurnTime());
            assertEquals(1, furnace.getCookTime());
            assertEquals(1, world.rebuildCount);
            assertEquals(0, world.lastRebuildX);
            assertEquals(70, world.lastRebuildY);
            assertEquals(0, world.lastRebuildZ);

            furnace.tick(world, 0.05f);

            assertEquals(1, world.rebuildCount);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Furnace tick should clear stale lit block state after burn time ends")
    void furnaceTickClearsStaleLitBlockState() {
        RecordingWorld world = new RecordingWorld(141L);
        try {
            world.setBlock(0, 70, 0, BlockType.LIT_FURNACE, 5);
            FurnaceTileEntity furnace = (FurnaceTileEntity) world.getTileEntity(0, 70, 0);

            furnace.tick(world, 0.05f);

            assertSame(BlockType.FURNACE, world.getBlock(0, 70, 0));
            assertEquals(5, world.getBlockMetadata(0, 70, 0));
            assertSame(furnace, world.getTileEntity(0, 70, 0));
            assertEquals(0, furnace.getBurnTime());
            assertEquals(0, furnace.getCookTime());
            assertEquals(1, world.rebuildCount);
            assertEquals(0, world.lastRebuildX);
            assertEquals(70, world.lastRebuildY);
            assertEquals(0, world.lastRebuildZ);

            furnace.tick(world, 0.05f);

            assertEquals(1, world.rebuildCount);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Lit furnace should emit Release-style smoke and flame from its facing side")
    void litFurnaceEmitsFacingSmokeAndFlame() {
        World world = new World(144L);
        try {
            world.setBlock(0, 70, 0, BlockType.LIT_FURNACE, 2);
            FurnaceTileEntity furnace = (FurnaceTileEntity) world.getTileEntity(0, 70, 0);
            furnace.setBurnTime(2);

            furnace.tick(world, 0.05f);

            List<WorldParticle> particles = world.getParticles();
            assertEquals(2, particles.size());
            assertTrue(particles.stream().anyMatch(particle -> particle.getType() == WorldParticle.Type.SMOKE));
            assertTrue(particles.stream().anyMatch(particle -> particle.getType() == WorldParticle.Type.FLAME));
            for (WorldParticle particle : particles) {
                assertTrue(particle.getRenderZ(0.0f) < 0.0f,
                        "Metadata 2 should emit from the north/front face of the furnace");
                assertTrue(particle.getRenderX(0.0f) >= 0.2f && particle.getRenderX(0.0f) <= 0.8f);
                assertTrue(particle.getRenderY(0.0f) >= 70.0f && particle.getRenderY(0.0f) <= 70.375f);
            }
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Lit furnace ambience should stay visual-only in Release 1.0")
    void litFurnaceAmbienceIsVisualOnly() {
        FurnaceAmbienceWorld world = new FurnaceAmbienceWorld(145L);
        try {
            world.setBlock(4, 70, 5, BlockType.LIT_FURNACE, 5);
            FurnaceTileEntity furnace = (FurnaceTileEntity) world.getTileEntity(4, 70, 5);
            furnace.setBurnTime(2);

            furnace.tick(world, 0.05f);

            assertTrue(world.drainSoundEvents().isEmpty(),
                    "Release 1.0 furnace display ticks should not play a furnace-specific sound");

            WorldParticle smoke = world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.SMOKE)
                    .findFirst()
                    .orElseThrow();
            assertEquals(5.02f, smoke.getRenderX(0.0f), 0.0001f,
                    "Metadata 5 should emit from the east/front face of the furnace");
            assertEquals(70.1875f, smoke.getRenderY(0.0f), 0.0001f);
            assertEquals(5.5f, smoke.getRenderZ(0.0f), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 furnace should leave an empty bucket after lava fuel starts")
    void lavaBucketFuelLeavesEmptyBucketInReleaseOne() {
        World world = new World(14L);
        try {
            world.setBlock(0, 70, 0, BlockType.FURNACE);
            FurnaceTileEntity furnace = (FurnaceTileEntity) world.getTileEntity(0, 70, 0);
            furnace.getInventory()[FurnaceTileEntity.SLOT_INPUT] = new ItemStack(ItemType.IRON_ORE, 1);
            furnace.getInventory()[FurnaceTileEntity.SLOT_FUEL] = new ItemStack(ItemType.LAVA_BUCKET, 1);

            furnace.tick(world, 0.05f);

            assertSame(BlockType.LIT_FURNACE, world.getBlock(0, 70, 0));
            assertSame(ItemType.BUCKET, furnace.getInventory()[FurnaceTileEntity.SLOT_FUEL].getType());
            assertEquals(1, furnace.getInventory()[FurnaceTileEntity.SLOT_FUEL].getCount());
            assertEquals(20000, furnace.getCurrentFuelBurnTime());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Furnace should reset partial cook progress when fuel runs out")
    void furnaceResetsCookProgressWhenFuelRunsOut() {
        World world = new World(142L);
        try {
            world.setBlock(0, 70, 0, BlockType.LIT_FURNACE, 2);
            FurnaceTileEntity furnace = (FurnaceTileEntity) world.getTileEntity(0, 70, 0);
            furnace.getInventory()[FurnaceTileEntity.SLOT_INPUT] = new ItemStack(ItemType.IRON_ORE, 1);
            furnace.setBurnTime(1);
            furnace.setCurrentFuelBurnTime(1600);
            furnace.setCookTime(80);

            furnace.tick(world, 0.05f);

            assertSame(BlockType.FURNACE, world.getBlock(0, 70, 0));
            assertEquals(0, furnace.getBurnTime());
            assertEquals(0, furnace.getCookTime());
            assertSame(ItemType.IRON_ORE, furnace.getInventory()[FurnaceTileEntity.SLOT_INPUT].getType());
            assertNull(furnace.getInventory()[FurnaceTileEntity.SLOT_OUTPUT]);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Furnace should reset partial cook progress when invalid fuel is present")
    void furnaceResetsCookProgressWithInvalidFuelPresent() {
        World world = new World(143L);
        try {
            world.setBlock(0, 70, 0, BlockType.FURNACE, 2);
            FurnaceTileEntity furnace = (FurnaceTileEntity) world.getTileEntity(0, 70, 0);
            furnace.getInventory()[FurnaceTileEntity.SLOT_INPUT] = new ItemStack(ItemType.IRON_ORE, 1);
            furnace.getInventory()[FurnaceTileEntity.SLOT_FUEL] = new ItemStack(ItemType.DIRT, 1);
            furnace.setCookTime(80);

            furnace.tick(world, 0.05f);

            assertEquals(0, furnace.getBurnTime());
            assertEquals(0, furnace.getCookTime());
            assertSame(ItemType.DIRT, furnace.getInventory()[FurnaceTileEntity.SLOT_FUEL].getType());
            assertNull(furnace.getInventory()[FurnaceTileEntity.SLOT_OUTPUT]);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Furnace should reset stale cook progress when fuel slot is empty")
    void furnaceClearsStaleCookProgressWhenFuelSlotIsEmpty() {
        World world = new World(147L);
        try {
            world.setBlock(0, 70, 0, BlockType.FURNACE, 2);
            FurnaceTileEntity furnace = (FurnaceTileEntity) world.getTileEntity(0, 70, 0);
            furnace.getInventory()[FurnaceTileEntity.SLOT_INPUT] = new ItemStack(ItemType.IRON_ORE, 1);
            furnace.setCookTime(80);

            assertDoesNotThrow(() -> furnace.tick(world, 0.05f));

            assertEquals(0, furnace.getBurnTime());
            assertEquals(0, furnace.getCookTime());
            assertSame(ItemType.IRON_ORE, furnace.getInventory()[FurnaceTileEntity.SLOT_INPUT].getType());
            assertNull(furnace.getInventory()[FurnaceTileEntity.SLOT_OUTPUT]);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Furnace should clear stale cook progress when the input can no longer smelt")
    void furnaceClearsStaleCookProgressForInvalidInputStates() {
        World world = new World(146L);
        try {
            world.setBlock(0, 70, 0, BlockType.FURNACE, 2);
            FurnaceTileEntity missingInput = (FurnaceTileEntity) world.getTileEntity(0, 70, 0);
            missingInput.setCookTime(80);

            missingInput.tick(world, 0.05f);

            assertEquals(0, missingInput.getCookTime());

            world.setBlock(1, 70, 0, BlockType.FURNACE, 2);
            FurnaceTileEntity unsmeltableInput = (FurnaceTileEntity) world.getTileEntity(1, 70, 0);
            unsmeltableInput.getInventory()[FurnaceTileEntity.SLOT_INPUT] = new ItemStack(ItemType.DIRT, 1);
            unsmeltableInput.setCookTime(80);

            unsmeltableInput.tick(world, 0.05f);

            assertEquals(0, unsmeltableInput.getCookTime());
            assertSame(ItemType.DIRT, unsmeltableInput.getInventory()[FurnaceTileEntity.SLOT_INPUT].getType());

            world.setBlock(2, 70, 0, BlockType.FURNACE, 2);
            FurnaceTileEntity blockedOutput = (FurnaceTileEntity) world.getTileEntity(2, 70, 0);
            blockedOutput.getInventory()[FurnaceTileEntity.SLOT_INPUT] = new ItemStack(ItemType.IRON_ORE, 1);
            blockedOutput.getInventory()[FurnaceTileEntity.SLOT_OUTPUT] = new ItemStack(ItemType.DIRT, 1);
            blockedOutput.setCookTime(80);

            blockedOutput.tick(world, 0.05f);

            assertEquals(0, blockedOutput.getCookTime());
            assertSame(ItemType.IRON_ORE, blockedOutput.getInventory()[FurnaceTileEntity.SLOT_INPUT].getType());
            assertSame(ItemType.DIRT, blockedOutput.getInventory()[FurnaceTileEntity.SLOT_OUTPUT].getType());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Furnace should not smelt when output is blocked")
    void furnaceDoesNotSmeltWithBlockedOutput() {
        World world = new World(5L);
        try {
            world.setBlock(0, 70, 0, BlockType.FURNACE);
            FurnaceTileEntity furnace = (FurnaceTileEntity) world.getTileEntity(0, 70, 0);
            furnace.getInventory()[FurnaceTileEntity.SLOT_INPUT] = new ItemStack(ItemType.GOLD_ORE, 1);
            furnace.getInventory()[FurnaceTileEntity.SLOT_FUEL] = new ItemStack(ItemType.STICK, 1);
            furnace.getInventory()[FurnaceTileEntity.SLOT_OUTPUT] = new ItemStack(ItemType.DIRT, 1);

            furnace.tick(world, 10.0f);

            assertSame(ItemType.GOLD_ORE, furnace.getInventory()[FurnaceTileEntity.SLOT_INPUT].getType());
            assertSame(ItemType.STICK, furnace.getInventory()[FurnaceTileEntity.SLOT_FUEL].getType());
            assertSame(ItemType.DIRT, furnace.getInventory()[FurnaceTileEntity.SLOT_OUTPUT].getType());
            assertEquals(0, furnace.getBurnTime());
            assertEquals(0, furnace.getCookTime());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Smelting registry should match the Java Release 1.0 recipe table")
    void smeltingRegistryMatchesReleaseOneRecipeTable() throws ReflectiveOperationException {
        Map<ItemType, ItemType> expected = Map.ofEntries(
                Map.entry(ItemType.IRON_ORE, ItemType.IRON_INGOT),
                Map.entry(ItemType.GOLD_ORE, ItemType.GOLD_INGOT),
                Map.entry(ItemType.DIAMOND_ORE, ItemType.DIAMOND),
                Map.entry(ItemType.COAL_ORE, ItemType.COAL),
                Map.entry(ItemType.REDSTONE_ORE, ItemType.REDSTONE),
                Map.entry(ItemType.LAPIS_ORE, ItemType.LAPIS_LAZULI),
                Map.entry(ItemType.OAK_LOG, ItemType.CHARCOAL),
                Map.entry(ItemType.SPRUCE_LOG, ItemType.CHARCOAL),
                Map.entry(ItemType.BIRCH_LOG, ItemType.CHARCOAL),
                Map.entry(ItemType.RAW_PORKCHOP, ItemType.COOKED_PORKCHOP),
                Map.entry(ItemType.RAW_BEEF, ItemType.STEAK),
                Map.entry(ItemType.RAW_CHICKEN, ItemType.COOKED_CHICKEN),
                Map.entry(ItemType.RAW_FISH, ItemType.COOKED_FISH),
                Map.entry(ItemType.SAND, ItemType.GLASS),
                Map.entry(ItemType.COBBLESTONE, ItemType.STONE),
                Map.entry(ItemType.CLAY_BALL, ItemType.BRICK_ITEM),
                Map.entry(ItemType.CACTUS, ItemType.CACTUS_GREEN));

        assertEquals(expected.size(), smeltingRecipes().size(),
                "CraftZero should not expose later-era furnace recipes in the Release 1.0 target");
        expected.forEach(FurnaceTileEntityTest::assertSmeltsTo);
        assertNull(SmeltingRegistry.getResult(new ItemStack(ItemType.CLAY, 1)));
        assertNull(SmeltingRegistry.getResult(new ItemStack(ItemType.NETHERRACK, 1)));
        assertNull(SmeltingRegistry.getResult(new ItemStack(ItemType.STONE, 1)));

        assertEquals(1600, FuelRegistry.getBurnTime(new ItemStack(ItemType.CHARCOAL, 1)));
        assertEquals(20000, FuelRegistry.getBurnTime(new ItemStack(ItemType.LAVA_BUCKET, 1)));
        assertEquals(2400, FuelRegistry.getBurnTime(new ItemStack(ItemType.BLAZE_ROD, 1)));
        assertEquals(300, FuelRegistry.getBurnTime(new ItemStack(ItemType.CHEST, 1)));
        assertEquals(300, FuelRegistry.getBurnTime(new ItemStack(ItemType.LOCKED_CHEST, 1)));
        assertEquals(300, FuelRegistry.getBurnTime(new ItemStack(ItemType.OAK_PLANKS, 1)));
        assertEquals(300, FuelRegistry.getBurnTime(new ItemStack(ItemType.TRAPDOOR, 1)));
        assertEquals(100, FuelRegistry.getBurnTime(new ItemStack(ItemType.SAPLING, 1)));
        assertEquals(100, FuelRegistry.getBurnTime(new ItemStack(ItemType.SPRUCE_SAPLING, 1)));
        assertEquals(100, FuelRegistry.getBurnTime(new ItemStack(ItemType.BIRCH_SAPLING, 1)));
        assertEquals(100, FuelRegistry.getBurnTime(new ItemStack(ItemType.STICK, 1)));
        assertEquals(0, FuelRegistry.getBurnTime(new ItemStack(ItemType.WOODEN_SLAB, 1)));
        assertEquals(0, FuelRegistry.getBurnTime(new ItemStack(ItemType.WOODEN_PICKAXE, 1)));
        assertEquals(0, FuelRegistry.getBurnTime(new ItemStack(ItemType.WOODEN_DOOR, 1)));
    }

    private static void assertSmeltsTo(ItemType input, ItemType output) {
        ItemStack result = SmeltingRegistry.getResult(new ItemStack(input, 1));
        assertNotNull(result, input + " should be smeltable");
        assertSame(output, result.getType());
        assertEquals(1, result.getCount());
    }

    @SuppressWarnings("unchecked")
    private static Map<ItemType, ItemStack> smeltingRecipes() throws ReflectiveOperationException {
        Field recipes = SmeltingRegistry.class.getDeclaredField("RECIPES");
        recipes.setAccessible(true);
        return (Map<ItemType, ItemStack>) recipes.get(null);
    }

    private static final class RecordingWorld extends World {
        private int rebuildCount;
        private int lastRebuildX;
        private int lastRebuildY;
        private int lastRebuildZ;

        private RecordingWorld(long seed) {
            super(seed);
        }

        @Override
        public void rebuildBlockMeshesNow(int x, int y, int z) {
            rebuildCount++;
            lastRebuildX = x;
            lastRebuildY = y;
            lastRebuildZ = z;
        }
    }

    private static final class FurnaceAmbienceWorld extends World {
        private final Random ambienceRandom = new Random() {
            @Override
            public float nextFloat() {
                return 0.5f;
            }
        };

        private FurnaceAmbienceWorld(long seed) {
            super(seed);
        }

        @Override
        public Random getRandom() {
            return ambienceRandom;
        }
    }
}

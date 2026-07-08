package com.craftzero.entity;

import com.craftzero.entity.mob.Zombie;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.progression.PotionData;
import com.craftzero.world.Block;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaintingEntityTest {
    @Test
    @DisplayName("Paintings choose only artwork that fits the available support")
    void paintingChoosesOnlyFittingArtOnOneBlockWall() {
        World world = new World(6240L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE, 0);

            PaintingEntity painting = world.placePainting(0, 70, 0, Block.FACE_NORTH);
            world.updateEntities(1.0f / 20.0f);

            assertTrue(world.getEntities().contains(painting));
            assertEquals(1, painting.getArt().blocksWide());
            assertEquals(1, painting.getArt().blocksHigh());
            assertEquals(Block.FACE_NORTH, painting.getFacing());
            assertEquals(0.5f, painting.getX(), 0.001f);
            assertEquals(70.5f, painting.getY(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Painting creation should fall back to the world RNG when no random is injected")
    void paintingCreateUsesWorldRandomFallback() {
        CountingIntRandom random = new CountingIntRandom(2);
        World world = new RandomOverrideWorld(6248L, random);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE, 0);

            PaintingEntity painting = PaintingEntity.create(world, 0, 70, 0, Block.FACE_NORTH, null);

            assertSame(PaintingEntity.Art.ALBAN, painting.getArt());
            assertEquals(1, random.nextIntCalls());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Paintings can only be placed on horizontal block faces")
    void paintingRequiresHorizontalFace() {
        World world = new World(6241L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE, 0);

            assertNull(world.placePainting(0, 70, 0, Block.FACE_TOP));
            assertNull(world.placePainting(0, 70, 0, Block.FACE_BOTTOM));
            assertTrue(world.getEntities().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Painting artwork catalog should stay within Java Release 1.0")
    void paintingCatalogExcludesPostReleaseMotives() {
        for (PaintingEntity.Art art : PaintingEntity.Art.values()) {
            assertFalse("Wither".equals(art.motive()));
        }

        assertSame(PaintingEntity.Art.KEBAB, PaintingEntity.Art.fromMotive("Wither"));
    }

    @Test
    @DisplayName("Paintings cannot occupy the same space as solid blocks")
    void paintingRequiresEmptyHangingSpace() {
        World world = new World(6246L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, -1, BlockType.STONE, 0);

            assertNull(world.placePainting(0, 70, 0, Block.FACE_NORTH));
            assertTrue(world.getEntities().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Painting placement should reject overlaps with pending paintings")
    void paintingPlacementRejectsPendingPaintingOverlap() {
        World world = new World(6249L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE, 0);

            PaintingEntity first = world.placePainting(0, 70, 0, Block.FACE_NORTH);
            PaintingEntity overlapping = world.placePainting(0, 70, 0, Block.FACE_NORTH);

            assertNotNull(first);
            assertNull(overlapping);

            world.updateEntities(1.0f / 20.0f);

            assertEquals(1L, world.getEntities().stream().filter(PaintingEntity.class::isInstance).count());
            assertSame(first, world.getEntities().stream()
                    .filter(PaintingEntity.class::isInstance)
                    .findFirst()
                    .orElseThrow());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Painting placement should reject mobs occupying the hanging space")
    void paintingPlacementRejectsMobOverlap() {
        World world = new World(6250L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE, 0);
            Zombie zombie = new Zombie();
            zombie.setPosition(0.5f, 70.0f, -0.03125f);
            world.replaceEntities(List.of(zombie));

            assertNull(world.placePainting(0, 70, 0, Block.FACE_NORTH));
            assertEquals(1, world.getEntities().size());
            assertSame(zombie, world.getEntities().get(0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Painting placement should reject dropped items occupying the hanging space")
    void paintingPlacementRejectsDroppedItemOverlap() {
        World world = new World(6251L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE, 0);
            world.spawnThrownStack(0.5f, 70.5f, -0.03125f,
                    new ItemStack(ItemType.DIRT, 1), 0.0f, 0.0f, 0.0f);

            assertNull(world.placePainting(0, 70, 0, Block.FACE_NORTH));
            assertTrue(world.getEntities().isEmpty());
            assertEquals(1, world.getDroppedItems().size());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Paintings break and drop when an entity enters their hanging space")
    void paintingBreaksWhenEntityEntersHangingSpace() {
        World world = new World(6252L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE, 0);
            PaintingEntity painting = world.placePainting(0, 70, 0, Block.FACE_NORTH);
            world.updateEntities(1.0f / 20.0f);

            Zombie zombie = new Zombie();
            zombie.setPosition(0.5f, 70.0f, -0.03125f);
            world.spawnEntity(zombie);
            world.updateEntities(1.0f / 20.0f);

            assertTrue(painting.isRemoved());
            assertFalse(world.getEntities().stream().anyMatch(entity -> entity instanceof PaintingEntity));
            assertTrue(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == ItemType.PAINTING && item.getCount() == 1));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Breaking a painting drops the painting item")
    void paintingDropsItemWhenBroken() {
        World world = new World(6242L);
        try {
            PaintingEntity painting = PaintingEntity.fromSupport(0, 70, 0,
                    Block.FACE_NORTH, PaintingEntity.Art.KEBAB);
            world.spawnEntity(painting);

            assertTrue(painting.breakAsItem(false));

            assertTrue(painting.isRemoved());
            assertTrue(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == ItemType.PAINTING && item.getCount() == 1));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Creative painting breaks do not drop an item")
    void creativeBreakDoesNotDrop() {
        World world = new World(6243L);
        try {
            PaintingEntity painting = PaintingEntity.fromSupport(0, 70, 0,
                    Block.FACE_NORTH, PaintingEntity.Art.KEBAB);
            world.spawnEntity(painting);

            assertTrue(painting.breakAsItem(true));

            assertTrue(painting.isRemoved());
            assertFalse(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == ItemType.PAINTING));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Paintings break and drop when their supporting block is removed")
    void paintingBreaksWhenSupportRemoved() {
        World world = new World(6244L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE, 0);
            PaintingEntity painting = world.placePainting(0, 70, 0, Block.FACE_NORTH);
            world.updateEntities(1.0f / 20.0f);

            world.setBlock(0, 70, 0, BlockType.AIR, 0);
            world.updateEntities(1.0f / 20.0f);

            assertTrue(painting.isRemoved());
            assertFalse(world.getEntities().stream().anyMatch(entity -> entity instanceof PaintingEntity));
            assertTrue(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == ItemType.PAINTING && item.getCount() == 1));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Explosion damage breaks paintings immediately")
    void explosionBreaksPaintingImmediately() {
        World world = new World(6247L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE, 0);
            PaintingEntity painting = world.placePainting(0, 70, 0, Block.FACE_NORTH);
            world.updateEntities(1.0f / 20.0f);

            assertTrue(world.getEntities().contains(painting));
            assertEquals(1, painting.getArt().blocksWide());
            assertEquals(1, painting.getArt().blocksHigh());
            assertEquals(0.5f, painting.getX(), 0.001f);
            assertEquals(70.5f, painting.getY(), 0.001f);
            assertEquals(-0.03125f, painting.getZ(), 0.001f);
            world.explode(0.5f, 70.5f, -1.0f, 4.0f);

            assertTrue(painting.isRemoved());
            assertTrue(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == ItemType.PAINTING && item.getCount() == 1));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Arrows should pop paintings before sticking in their support block")
    void arrowsBreakPaintingsBeforeSupportBlockImpact() {
        World world = new World(6253L);
        try {
            PaintingEntity painting = supportedPainting(world);
            ArrowEntity arrow = new ArrowEntity(0.5f, 70.5f, -2.0f,
                    0.0f, 0.0f, 3.0f, null, false, 2.0f);
            world.replaceEntities(List.of(painting, arrow));

            world.updateEntities(1.0f / 20.0f);

            assertProjectilePoppedPainting(world, painting, arrow);
            assertSame(BlockType.STONE, world.getBlock(0, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Thrown snowballs should pop paintings on direct contact")
    void thrownSnowballsBreakPaintingsOnImpact() {
        World world = new World(6254L);
        try {
            PaintingEntity painting = supportedPainting(world);
            ThrownItemEntity snowball = new ThrownItemEntity(0.5f, 70.5f, -2.0f,
                    0.0f, 0.0f, 3.0f, ItemType.SNOWBALL, null);
            world.replaceEntities(List.of(painting, snowball));

            world.updateEntities(1.0f / 20.0f);

            assertProjectilePoppedPainting(world, painting, snowball);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Splash potions should pop paintings and splash at the impact point")
    void splashPotionsBreakPaintingsOnImpact() {
        World world = new World(6255L);
        try {
            PaintingEntity painting = supportedPainting(world);
            SplashPotionEntity potion = new SplashPotionEntity(0.5f, 70.5f, -2.0f,
                    0.0f, 0.0f, 3.0f, null, PotionData.water());
            world.replaceEntities(List.of(painting, potion));

            world.updateEntities(1.0f / 20.0f);

            assertProjectilePoppedPainting(world, painting, potion);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Fireballs should pop paintings on entity impact")
    void fireballsBreakPaintingsOnImpact() {
        World world = new World(6256L);
        try {
            PaintingEntity painting = supportedPainting(world);
            FireballEntity fireball = new FireballEntity(0.5f, 70.5f, -2.0f,
                    0.0f, 0.0f, 3.0f, null, false);
            world.replaceEntities(List.of(painting, fireball));

            world.updateEntities(1.0f / 20.0f);

            assertProjectilePoppedPainting(world, painting, fireball);
        } finally {
            world.cleanup();
        }
    }

    private static PaintingEntity supportedPainting(World world) {
        world.setBlock(0, 70, 0, BlockType.STONE, 0);
        return PaintingEntity.fromSupport(0, 70, 0, Block.FACE_NORTH, PaintingEntity.Art.KEBAB);
    }

    private static void assertProjectilePoppedPainting(World world, PaintingEntity painting, Entity projectile) {
        assertTrue(painting.isRemoved());
        assertTrue(projectile.isRemoved());
        assertFalse(world.getEntities().stream()
                .anyMatch(entity -> entity instanceof PaintingEntity && !entity.isRemoved()));
        assertTrue(world.getDroppedItems().stream()
                .anyMatch(item -> item.getItemType() == ItemType.PAINTING && item.getCount() == 1));
    }

    private static final class CountingIntRandom extends Random {
        private final int value;
        private int calls;

        private CountingIntRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            calls++;
            if (value < 0 || value >= bound) {
                throw new IllegalArgumentException("Fixed random value " + value + " outside bound " + bound);
            }
            return value;
        }

        private int nextIntCalls() {
            return calls;
        }
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

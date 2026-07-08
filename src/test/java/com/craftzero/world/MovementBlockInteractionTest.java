package com.craftzero.world;

import com.craftzero.engine.Input;
import com.craftzero.entity.Entity;
import com.craftzero.entity.LivingEntity;
import com.craftzero.entity.mob.Chicken;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.main.PlayerStats;
import com.craftzero.progression.ArmorSlot;
import com.craftzero.progression.EnchantmentInstance;
import com.craftzero.progression.EnchantmentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.junit.jupiter.api.Assertions.*;

class MovementBlockInteractionTest {
    @Test
    @DisplayName("Cobweb should be selectable but not block movement")
    void cobwebIsPassThroughCollision() {
        BlockShape.BlockContext empty = emptyContext();

        assertTrue(BlockShape.collisionShape(BlockState.of(BlockType.COBWEB), empty).isEmpty());
        assertFalse(BlockShape.selectionShape(BlockState.of(BlockType.COBWEB), empty).isEmpty());
    }

    @Test
    @DisplayName("Soul sand should lower its collision top by one eighth block")
    void soulSandUsesLowerCollisionTop() {
        VoxelShape shape = BlockShape.collisionShape(BlockState.of(BlockType.SOUL_SAND), emptyContext());

        assertFalse(shape.isEmpty());
        assertFalse(shape.isFullCube());
        assertEquals(1, shape.boxes().size());
        assertEquals(14.0f / 16.0f, shape.boxes().get(0).maxY(), 0.0001f);
        assertTrue(BlockShape.selectionShape(BlockState.of(BlockType.SOUL_SAND), emptyContext()).isFullCube());
    }

    @Test
    @DisplayName("Entities inside cobweb should move with damped attempts and clear stored motion")
    void entityInCobwebIsSlowed() {
        World world = new World(9201L);
        try {
            world.setBlock(0, 100, 0, BlockType.COBWEB, 0);
            TestPhysicsEntity entity = new TestPhysicsEntity(0.5f, 100.0f, 0.5f);
            entity.setMotion(1.0f, -1.0f, 0.0f);
            world.replaceEntities(List.of(entity));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(entity.getX() > 0.7f, () -> "x=" + entity.getX());
            assertEquals(0.0f, entity.getMotionX(), 0.0001f);
            assertEquals(0.0f, entity.getMotionY(), 0.0001f);
            assertEquals(0.0f, entity.getMotionZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Players inside cobweb should move with damped attempts and clear velocity")
    void playerInCobwebIsSlowed() {
        World world = new World(9202L);
        try {
            world.setBlock(0, 100, 0, BlockType.COBWEB, 0);
            Player player = new Player(0.5f, 100.0f, 0.5f);
            player.getVelocity().set(4.0f, -4.0f, 0.0f);

            player.update(1.0f / 20.0f, world);

            assertTrue(player.getPosition().x > 0.53f, () -> "x=" + player.getPosition().x);
            assertEquals(0.0f, player.getVelocity().x, 0.0001f);
            assertEquals(0.0f, player.getVelocity().y, 0.0001f);
            assertEquals(0.0f, player.getVelocity().z, 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Soul sand should slow horizontal entity motion on contact")
    void entityTouchingSoulSandIsSlowed() {
        World world = new World(9203L);
        try {
            world.setBlock(0, 100, 0, BlockType.SOUL_SAND, 0);
            TestPhysicsEntity entity = new TestPhysicsEntity(0.5f, 100.875f, 0.5f);
            entity.setMotion(1.0f, 0.0f, 0.0f);
            world.replaceEntities(List.of(entity));
            assertTrue(entity.isTouching(BlockType.SOUL_SAND), "fixture should overlap soul sand before ticking");

            world.updateEntities(1.0f / 20.0f);

            assertTrue(entity.getMotionX() < 0.25f, () -> "motionX=" + entity.getMotionX());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Soul sand should slow horizontal player velocity on contact")
    void playerTouchingSoulSandIsSlowed() {
        World world = new World(9204L);
        try {
            world.setBlock(0, 100, 0, BlockType.SOUL_SAND, 0);
            Player player = new Player(0.5f, 100.875f, 0.5f);
            player.getVelocity().set(4.0f, 0.0f, 0.0f);

            player.update(1.0f / 20.0f, world);

            assertTrue(player.getVelocity().x < 1.60f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ice should preserve more horizontal entity motion than normal ground")
    void icePreservesEntityMotion() {
        float stoneMotion = entityMotionAfterGroundTick(BlockType.STONE);
        float iceMotion = entityMotionAfterGroundTick(BlockType.ICE);

        assertTrue(stoneMotion < 0.13f);
        assertTrue(iceMotion > 0.18f);
        assertTrue(iceMotion > stoneMotion + 0.05f);
    }

    @Test
    @DisplayName("Ice should preserve more horizontal player velocity than normal ground")
    void icePreservesPlayerVelocity() {
        float stoneVelocity = playerVelocityAfterGroundTick(BlockType.STONE);
        float iceVelocity = playerVelocityAfterGroundTick(BlockType.ICE);

        assertTrue(stoneVelocity < 3.70f);
        assertTrue(iceVelocity > 3.85f);
        assertTrue(iceVelocity > stoneVelocity + 0.20f);
    }

    @Test
    @DisplayName("Vines should be selectable climbable pass-through side attachments")
    void vinesArePassThroughSideAttachments() {
        BlockShape.BlockContext eastSupport = contextWithBlockAt(1, 0, 0, BlockType.STONE);
        int westVineMetadata = BlockShape.vineMetadataFromFace(Block.FACE_WEST);

        assertTrue(BlockShape.collisionShape(new BlockState(BlockType.VINES, westVineMetadata), eastSupport).isEmpty());
        VoxelShape selection = BlockShape.selectionShape(new BlockState(BlockType.VINES, westVineMetadata), eastSupport);
        assertFalse(selection.isEmpty());
        assertFalse(selection.isFullCube());
        assertTrue(BlockShape.canFallThrough(BlockType.VINES));
        assertEquals(8, BlockShape.vineMetadataFromFace(Block.FACE_WEST));
        assertEquals(2, BlockShape.vineMetadataFromFace(Block.FACE_EAST));
        assertEquals(1, BlockShape.vineMetadataFromFace(Block.FACE_NORTH));
        assertEquals(4, BlockShape.vineMetadataFromFace(Block.FACE_SOUTH));
        assertEquals(-1, BlockShape.vineMetadataFromFace(Block.FACE_TOP));
    }

    @Test
    @DisplayName("Vines should require horizontal side support")
    void vinesRequireSideSupport() {
        assertFalse(BlockShape.canPlaceAt(BlockType.VINES, 8, emptyContext()));
        assertTrue(BlockShape.canPlaceAt(BlockType.VINES, 8,
                contextWithBlockAt(1, 0, 0, BlockType.STONE)));
        assertFalse(BlockShape.canPlaceAt(BlockType.VINES, 8,
                contextWithBlockAt(0, -1, 0, BlockType.STONE)));
        assertTrue(BlockShape.canPlaceAt(BlockType.VINES, 8,
                contextWithBlockAt(0, 1, 0, BlockType.VINES, 8)));
        assertFalse(BlockShape.canPlaceAt(BlockType.VINES, 8,
                contextWithBlockAt(0, 1, 0, BlockType.VINES, 2)));
    }

    @Test
    @DisplayName("Vine ticks should prune unsupported side bits and drop fully unsupported vines")
    void vineTicksPruneUnsupportedSidesAndDropWhenDetached() {
        World world = new World(9413L);
        int westBit = BlockShape.vineMetadataFromFace(Block.FACE_WEST);
        int eastBit = BlockShape.vineMetadataFromFace(Block.FACE_EAST);
        try {
            world.setBlock(1, 100, 0, BlockType.STONE, 0);
            world.setBlock(-1, 100, 0, BlockType.STONE, 0);
            world.setBlock(0, 100, 0, BlockType.VINES, westBit | eastBit);

            world.breakBlock(-1, 100, 0, false);
            world.scheduleBlockTick(0, 100, 0, BlockType.VINES, 0);
            world.advanceBlockTicks(1);

            assertSame(BlockType.VINES, world.getBlock(0, 100, 0));
            assertEquals(westBit, world.getBlockMetadata(0, 100, 0) & 15);

            world.breakBlock(1, 100, 0, false);
            world.scheduleBlockTick(0, 100, 0, BlockType.VINES, 0);
            world.advanceBlockTicks(1);

            assertSame(BlockType.AIR, world.getBlock(0, 100, 0));
            assertTrue(world.getDroppedItems().stream().noneMatch(item -> item.getItemType() == ItemType.VINES));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Vine random ticks should grow hanging columns downward")
    void vineTicksGrowHangingColumnsDownward() {
        World world = new World(9414L);
        int westBit = BlockShape.vineMetadataFromFace(Block.FACE_WEST);
        try {
            world.setBlock(1, 100, 0, BlockType.STONE, 0);
            world.setBlock(-1, 100, 0, BlockType.STONE, 0);
            world.setBlock(0, 100, 1, BlockType.STONE, 0);
            world.setBlock(0, 100, -1, BlockType.STONE, 0);
            world.setBlock(0, 101, 0, BlockType.STONE, 0);
            world.setBlock(0, 99, 0, BlockType.AIR, 0);
            world.setBlock(0, 100, 0, BlockType.VINES, westBit);

            for (int i = 0; i < 500 && world.getBlock(0, 99, 0) != BlockType.VINES; i++) {
                world.scheduleBlockTick(0, 100, 0, BlockType.VINES, 0);
                world.advanceBlockTicks(1);
            }

            assertSame(BlockType.VINES, world.getBlock(0, 99, 0));
            assertTrue((world.getBlockMetadata(0, 99, 0) & westBit) != 0);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Players touching vines should use climbable falling control")
    void playerTouchingVinesIsClimbable() {
        World world = new World(9401L);
        try {
            world.setBlock(0, 100, 0, BlockType.VINES, BlockShape.vineMetadataFromFace(Block.FACE_WEST));
            Player player = new Player(0.5f, 100.0f, 0.5f);
            player.getVelocity().set(10.0f, -10.0f, -10.0f);

            player.update(1.0f / 20.0f, world);

            assertEquals(3.0f, player.getVelocity().x, 0.001f);
            assertEquals(-3.0f, player.getVelocity().y, 0.001f);
            assertEquals(-3.0f, player.getVelocity().z, 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Players colliding horizontally on vines should get the source climb bump")
    void playerCollidingHorizontallyOnVinesClimbs() {
        World world = new World(9402L);
        try {
            world.setBlock(0, 100, 0, BlockType.VINES, BlockShape.vineMetadataFromFace(Block.FACE_WEST));
            world.setBlock(1, 100, 0, BlockType.STONE, 0);
            Player player = new Player(0.65f, 100.0f, 0.5f);
            player.getVelocity().set(10.0f, -10.0f, 0.0f);

            player.update(1.0f / 20.0f, world);

            assertEquals(0.0f, player.getVelocity().x, 0.001f);
            assertEquals(4.0f, player.getVelocity().y, 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player jump key on vines should not climb without horizontal collision")
    void playerJumpKeyOnVinesDoesNotBypassSourceCollisionClimb() throws Exception {
        World world = new World(9403L);
        try {
            setKeyDown(GLFW_KEY_SPACE, true);
            world.setBlock(0, 100, 0, BlockType.VINES, BlockShape.vineMetadataFromFace(Block.FACE_WEST));
            Player player = new Player(0.5f, 100.0f, 0.5f);
            player.getVelocity().set(0.0f, -10.0f, 0.0f);

            player.update(1.0f / 20.0f, world);

            assertEquals(-3.0f, player.getVelocity().y, 0.001f);
        } finally {
            setKeyDown(GLFW_KEY_SPACE, false);
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player forward key on vines should not climb without horizontal collision")
    void playerForwardKeyOnVinesDoesNotBypassSourceCollisionClimb() throws Exception {
        World world = new World(9404L);
        try {
            setKeyDown(GLFW_KEY_W, true);
            world.setBlock(0, 100, 0, BlockType.VINES, BlockShape.vineMetadataFromFace(Block.FACE_WEST));
            Player player = new Player(0.5f, 100.0f, 0.5f);
            player.getVelocity().set(0.0f, -10.0f, 0.0f);

            player.update(1.0f / 20.0f, world);

            assertEquals(-3.0f, player.getVelocity().y, 0.001f);
        } finally {
            setKeyDown(GLFW_KEY_W, false);
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player back key on vines should not force downward motion")
    void playerBackKeyOnVinesDoesNotForceDescent() throws Exception {
        World world = new World(9405L);
        try {
            setKeyDown(GLFW_KEY_S, true);
            world.setBlock(0, 100, 0, BlockType.VINES, BlockShape.vineMetadataFromFace(Block.FACE_WEST));
            Player player = new Player(0.5f, 100.0f, 0.5f);
            player.getVelocity().set(0.0f, 1.0f, 0.0f);

            player.update(1.0f / 20.0f, world);

            assertEquals(1.0f, player.getVelocity().y, 0.001f);
        } finally {
            setKeyDown(GLFW_KEY_S, false);
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player sneak key on vines should stop downward motion")
    void playerSneakKeyOnVinesStopsDownwardMotion() throws Exception {
        World world = new World(9406L);
        try {
            setKeyDown(GLFW_KEY_LEFT_SHIFT, true);
            world.setBlock(0, 100, 0, BlockType.VINES, BlockShape.vineMetadataFromFace(Block.FACE_WEST));
            Player player = new Player(0.5f, 100.0f, 0.5f);
            player.getVelocity().set(0.0f, -10.0f, 0.0f);

            player.update(1.0f / 20.0f, world);

            assertEquals(0.0f, player.getVelocity().y, 0.001f);
        } finally {
            setKeyDown(GLFW_KEY_LEFT_SHIFT, false);
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Living entities touching vines should use source climbable motion clamps")
    void livingEntitiesTouchingVinesUseClimbableMotionClamp() {
        World world = new World(9407L);
        try {
            world.setBlock(0, 100, 0, BlockType.VINES, BlockShape.vineMetadataFromFace(Block.FACE_WEST));
            TestLivingEntity entity = new TestLivingEntity(0.5f, 100.0f, 0.5f);
            entity.setMotion(1.0f, -1.0f, -1.0f);
            world.replaceEntities(List.of(entity));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(0.15f, entity.getMotionX(), 0.0001f);
            assertEquals(-0.15f, entity.getMotionY(), 0.0001f);
            assertEquals(-0.15f, entity.getMotionZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Living entities colliding horizontally on vines should get the source climb bump")
    void livingEntitiesCollidingHorizontallyOnVinesClimb() {
        World world = new World(9408L);
        try {
            world.setBlock(0, 100, 0, BlockType.VINES, BlockShape.vineMetadataFromFace(Block.FACE_WEST));
            world.setBlock(1, 100, 0, BlockType.STONE, 0);
            TestLivingEntity entity = new TestLivingEntity(0.65f, 100.0f, 0.5f);
            entity.setMotion(1.0f, -1.0f, 0.0f);
            world.replaceEntities(List.of(entity));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(0.0f, entity.getMotionX(), 0.0001f);
            assertEquals(0.2f, entity.getMotionY(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Landing fall damage should use the fall damage source path")
    void landingFallDamageUsesFallDamageSource() {
        World world = new World(9409L);
        try {
            world.setBlock(0, 100, 0, BlockType.STONE, 0);
            Player player = new Player(0.5f, 106.0f, 0.5f);
            player.getStats().restore(20.0f, 0.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);
            ItemStack boots = new ItemStack(ItemType.IRON_BOOTS, 1);
            boots.addEnchantment(new EnchantmentInstance(EnchantmentType.FEATHER_FALLING, 4));
            player.getInventory().getArmor()[ArmorSlot.BOOTS.getIndex()] = boots;
            player.getVelocity().set(0.0f, -80.0f, 0.0f);

            player.update(0.1f, world);

            assertTrue(player.isOnGround());
            assertTrue(player.getStats().getHealth() > 18.5f,
                    () -> "health=" + player.getStats().getHealth());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Landing fall damage should round up like Release 1.0")
    void landingFallDamageRoundsUp() {
        World world = new World(9410L);
        try {
            world.setBlock(0, 100, 0, BlockType.STONE, 0);
            Player player = new Player(0.5f, 104.1f, 0.5f);
            player.getStats().restore(20.0f, 0.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);
            player.getVelocity().set(0.0f, -80.0f, 0.0f);

            player.update(0.1f, world);

            assertTrue(player.isOnGround());
            assertEquals(19.0f, player.getStats().getHealth(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Living entities should take rounded Release 1.0 fall damage on landing")
    void livingEntityLandingFallDamageRoundsUp() {
        World world = new World(9404L);
        try {
            world.setBlock(0, 100, 0, BlockType.STONE, 0);
            TestLivingEntity entity = new TestLivingEntity(0.5f, 104.1f, 0.5f);
            entity.setMotion(0.0f, -4.0f, 0.0f);
            world.replaceEntities(List.of(entity));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(entity.isOnGround());
            assertEquals(9.0f, entity.getHealth(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Chickens should keep their Release-style fall damage immunity")
    void chickensIgnoreLandingFallDamage() {
        World world = new World(9405L);
        try {
            world.setBlock(0, 100, 0, BlockType.STONE, 0);
            Chicken chicken = new Chicken();
            chicken.setPosition(0.5f, 104.1f, 0.5f);
            chicken.setMotion(0.0f, -4.0f, 0.0f);
            world.replaceEntities(List.of(chicken));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(chicken.isOnGround());
            assertEquals(chicken.getMaxHealth(), chicken.getHealth(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Living entities inside opaque blocks should suffocate at Release cadence")
    void livingEntityInsideOpaqueBlockSuffocates() {
        World world = new World(9411L);
        try {
            fillVerticalColumn(world, 0, 98, 102, 0, BlockType.STONE);
            TestLivingEntity entity = new TestLivingEntity(0.5f, 100.0f, 0.5f);
            world.replaceEntities(List.of(entity));

            world.updateEntities(1.0f / 20.0f);
            assertEquals(9.0f, entity.getHealth(), 0.001f);

            for (int i = 0; i < 9; i++) {
                world.updateEntities(1.0f / 20.0f);
            }
            assertEquals(9.0f, entity.getHealth(), 0.001f);

            world.updateEntities(1.0f / 20.0f);
            assertEquals(8.0f, entity.getHealth(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Players inside opaque blocks should suffocate without armor or difficulty scaling")
    void playerInsideOpaqueBlockSuffocates() {
        World world = new World(9412L);
        try {
            fillVerticalColumn(world, 0, 98, 102, 0, BlockType.STONE);
            Player player = new Player(0.5f, 100.0f, 0.5f);
            player.getStats().restore(20.0f, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);
            ItemStack helmet = new ItemStack(ItemType.IRON_HELMET, 1);
            player.getInventory().getArmor()[ArmorSlot.HELMET.getIndex()] = helmet;

            player.update(1.0f / 20.0f, world);
            assertEquals(19.0f, player.getStats().getHealth(), 0.001f);
            assertSame(helmet, player.getInventory().getArmor()[ArmorSlot.HELMET.getIndex()]);

            for (int i = 0; i < 9; i++) {
                player.update(1.0f / 20.0f, world);
            }
            assertEquals(19.0f, player.getStats().getHealth(), 0.001f);

            player.update(1.0f / 20.0f, world);
            assertEquals(18.0f, player.getStats().getHealth(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    private static float entityMotionAfterGroundTick(BlockType ground) {
        World world = new World(9205L + ground.getId());
        try {
            world.setBlock(0, 100, 0, ground, 0);
            TestPhysicsEntity entity = new TestPhysicsEntity(0.5f, 101.0f, 0.5f);
            entity.setMotion(0.2f, 0.0f, 0.0f);
            world.replaceEntities(List.of(entity));

            world.updateEntities(1.0f / 20.0f);

            return entity.getMotionX();
        } finally {
            world.cleanup();
        }
    }

    private static void fillVerticalColumn(World world, int x, int minY, int maxY, int z, BlockType block) {
        for (int y = minY; y <= maxY; y++) {
            world.setBlock(x, y, z, block, 0);
        }
    }

    private static float playerVelocityAfterGroundTick(BlockType ground) {
        World world = new World(9305L + ground.getId());
        try {
            world.setBlock(0, 100, 0, ground, 0);
            Player player = new Player(0.5f, 101.0f, 0.5f);
            player.update(1.0f / 20.0f, world);
            player.getVelocity().set(4.0f, 0.0f, 0.0f);

            player.update(1.0f / 20.0f, world);

            return player.getVelocity().x;
        } finally {
            world.cleanup();
        }
    }

    private static BlockShape.BlockContext emptyContext() {
        return new BlockShape.BlockContext() {
            @Override
            public BlockType getBlock(int dx, int dy, int dz) {
                return BlockType.AIR;
            }

            @Override
            public int getMetadata(int dx, int dy, int dz) {
                return 0;
            }
        };
    }

    private static BlockShape.BlockContext contextWithBlockAt(int supportDx, int supportDy, int supportDz,
            BlockType support) {
        return contextWithBlockAt(supportDx, supportDy, supportDz, support, 0);
    }

    private static BlockShape.BlockContext contextWithBlockAt(int supportDx, int supportDy, int supportDz,
            BlockType support, int metadata) {
        return new BlockShape.BlockContext() {
            @Override
            public BlockType getBlock(int dx, int dy, int dz) {
                return dx == supportDx && dy == supportDy && dz == supportDz ? support : BlockType.AIR;
            }

            @Override
            public int getMetadata(int dx, int dy, int dz) {
                return dx == supportDx && dy == supportDy && dz == supportDz ? metadata : 0;
            }
        };
    }

    private static void setKeyDown(int key, boolean down) throws Exception {
        Field field = Input.class.getDeclaredField("keys");
        field.setAccessible(true);
        ((boolean[]) field.get(null))[key] = down;
    }

    private static final class TestPhysicsEntity extends Entity {
        private TestPhysicsEntity(float x, float y, float z) {
            super(0.6f, 0.98f);
            setPosition(x, y, z);
        }

        private boolean isTouching(BlockType type) {
            return isTouchingBlock(type);
        }
    }

    private static final class TestLivingEntity extends LivingEntity {
        private TestLivingEntity(float x, float y, float z) {
            super(0.6f, 1.8f, 10.0f);
            setPosition(x, y, z);
        }
    }
}

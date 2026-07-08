package com.craftzero.main;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import com.craftzero.engine.Input;
import com.craftzero.entity.LivingEntity;
import com.craftzero.progression.StatusEffectInstance;
import com.craftzero.progression.StatusEffectType;
import com.craftzero.world.Block;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import com.craftzero.world.WorldSoundEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;

class PlayerMovementConstantsTest {
    @Test
    @DisplayName("Player movement constants should use the Release-style values")
    void playerMovementConstantsUseReleaseValues() throws ReflectiveOperationException {
        assertEquals(-32.0f, playerFloatConstant("GRAVITY"), 0.0001f);
        assertEquals(9.5f, playerFloatConstant("JUMP_VELOCITY"), 0.0001f);
        assertEquals(2.0f, playerFloatConstant("JUMP_BOOST_VELOCITY_BONUS"), 0.0001f);
        assertEquals(4.317f, playerFloatConstant("WALK_SPEED"), 0.0001f);
        assertEquals(5.612f, playerFloatConstant("SPRINT_SPEED"), 0.0001f);
        assertEquals(4.317f * 0.3f, playerFloatConstant("SNEAK_SPEED"), 0.0001f);
        assertEquals(0.8f, playerFloatConstant("WATER_DRAG"), 0.0001f);
        assertEquals(-8.0f, playerFloatConstant("WATER_GRAVITY_ACCELERATION"), 0.0001f);
        assertEquals(16.0f, playerFloatConstant("WATER_SWIM_UP_ACCELERATION"), 0.0001f);
        assertEquals(0.5f, playerFloatConstant("LAVA_DRAG"), 0.0001f);
        assertEquals(-8.0f, playerFloatConstant("LAVA_GRAVITY_ACCELERATION"), 0.0001f);
        assertEquals(16.0f, playerFloatConstant("LAVA_SWIM_UP_ACCELERATION"), 0.0001f);
    }

    @Test
    @DisplayName("Jump Boost should add the old per-level player jump velocity")
    void jumpBoostAddsReleaseOnePlayerJumpVelocity() throws ReflectiveOperationException {
        try {
            setKeyDown(GLFW_KEY_SPACE, true);
            Player player = new Player(0.5f, 90.0f, 0.5f);
            setPlayerBoolean(player, "onGround", true);
            player.getStats().addEffect(new StatusEffectInstance(StatusEffectType.JUMP_BOOST, 200, 1));

            player.handleInput(0.0f);

            assertEquals(13.5f, player.getVelocity().y, 0.0001f);
        } finally {
            setKeyDown(GLFW_KEY_SPACE, false);
        }
    }

    @Test
    @DisplayName("Sneaking should clamp horizontal movement to the old 30 percent walk speed")
    void sneakingUsesReleaseOneHorizontalSpeedCap() throws ReflectiveOperationException {
        World world = new World(501L);
        try {
            Player player = new Player(0.5f, 90.0f, 0.5f);
            setPlayerBoolean(player, "sneaking", true);
            setPlayerBoolean(player, "sprinting", true);
            player.getVelocity().set(10.0f, 0.0f, 0.0f);

            player.update(0.05f, world);

            float horizontalSpeed = (float) Math.sqrt(player.getVelocity().x * player.getVelocity().x
                    + player.getVelocity().z * player.getVelocity().z);
            assertEquals(4.317f * 0.3f, horizontalSpeed, 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player mob collision push should use the Release max-axis impulse")
    void playerMobCollisionPushUsesReleaseMaxAxisImpulse() {
        assertEquals(0.03535534f, pushedMobMotionX(0.5f, 2.0f), 0.0001f);
        assertEquals(0.05f, pushedMobMotionX(2.0f, 4.0f), 0.0001f);
    }

    @Test
    @DisplayName("Player mob collision should also push the player back")
    void playerMobCollisionPushesPlayerOppositeMob() {
        CollisionPush push = playerMobCollisionPush(0.5f, 2.0f);

        assertEquals(0.03535534f, push.mobMotionX(), 0.0001f);
        assertEquals(-0.03535534f, push.playerVelocityX(), 0.0001f);
    }

    @Test
    @DisplayName("Exact player mob overlap should still separate")
    void exactPlayerMobOverlapUsesFallbackPush() {
        CollisionPush push = playerMobCollisionPush(0.0f, 2.0f);

        assertEquals(0.05f, push.mobMotionX(), 0.0001f);
        assertEquals(-0.05f, push.playerVelocityX(), 0.0001f);
    }

    @Test
    @DisplayName("Water physics should use the old per-tick drag and gravity")
    void waterPhysicsUsesReleaseDragAndGravity() throws ReflectiveOperationException {
        assertEquals(-0.4f, updatedWaterVelocityY(false, false), 0.0001f);
        assertEquals(0.24f, updatedWaterVelocityY(true, false), 0.0001f);
        assertEquals(-0.4f, updatedWaterVelocityY(false, true), 0.0001f);
    }

    @Test
    @DisplayName("Water jump input should not be hidden behind a surface bobbing cooldown")
    void waterJumpInputDoesNotUseSurfaceBobbingCooldown() throws ReflectiveOperationException {
        assertEquals(0.24f, updatedWaterVelocityYWithLegacySurfaceCooldown(), 0.0001f);
    }

    @Test
    @DisplayName("Flowing water should carry players along decay gradients")
    void flowingWaterCurrentCarriesPlayers() {
        World world = new World(509L);
        try {
            Player player = new Player(1.5f, 90.0f, 0.5f);
            clearPlayerWaterFixture(world);
            world.setBlock(0, 90, 0, BlockType.WATER, 0);
            world.setBlock(0, 91, 0, BlockType.WATER, 0);
            world.setBlock(1, 90, 0, BlockType.FLOWING_WATER, 1);
            world.setBlock(1, 91, 0, BlockType.FLOWING_WATER, 1);

            player.update(1.0f / 20.0f, world);

            assertTrue(player.getVelocity().x > 0.25f, () -> "velocityX=" + player.getVelocity().x);
            assertTrue(player.getPosition().x > 1.51f, () -> "x=" + player.getPosition().x);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Lava physics should use the old heavy drag and small downward gravity")
    void lavaPhysicsUsesReleaseHeavyDragAndGravity() {
        World world = new World(510L);
        try {
            Player player = new Player(0.5f, 90.0f, 0.5f);
            clearPlayerWaterFixture(world);
            world.setBlock(0, 90, 0, BlockType.LAVA, 0);
            world.setBlock(0, 91, 0, BlockType.LAVA, 0);
            player.getVelocity().set(4.0f, 0.0f, 0.0f);

            player.update(1.0f / 20.0f, world);

            assertEquals(2.0f, player.getVelocity().x, 0.0001f);
            assertEquals(-0.4f, player.getVelocity().y, 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Flowing lava should carry players along decay gradients")
    void flowingLavaCurrentCarriesPlayers() {
        World world = new World(511L);
        try {
            Player player = new Player(1.5f, 90.0f, 0.5f);
            clearPlayerWaterFixture(world);
            world.setBlock(0, 90, 0, BlockType.LAVA, 0);
            world.setBlock(0, 91, 0, BlockType.LAVA, 0);
            world.setBlock(1, 90, 0, BlockType.FLOWING_LAVA, 2);
            world.setBlock(1, 91, 0, BlockType.FLOWING_LAVA, 2);

            player.update(1.0f / 20.0f, world);

            assertTrue(player.getVelocity().x > 0.25f, () -> "velocityX=" + player.getVelocity().x);
            assertTrue(player.getPosition().x > 1.51f, () -> "x=" + player.getPosition().x);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Walking players should play material footsteps without invented particles")
    void walkingPlayersPlayMaterialFootstepsWithoutInventedParticles() throws ReflectiveOperationException {
        World world = new World(512L);
        try {
            prepareWalkway(world, BlockType.STONE);
            Player player = new Player(0.5f, 100.0f, 0.5f);

            for (int tick = 0; tick < 40 && world.getSoundEvents().isEmpty(); tick++) {
                setPlayerBoolean(player, "onGround", true);
                setPlayerBoolean(player, "movementInputActive", true);
                player.getVelocity().set(4.317f, 0.0f, 0.0f);
                player.update(1.0f / 20.0f, world);
            }

            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            WorldSoundEvent step = sounds.get(0);
            assertEquals(WorldSoundEvent.STEP_STONE, step.soundId());
            assertEquals(0.15f, step.volume(), 0.0001f);
            assertEquals(1.0f, step.pitch(), 0.0001f);
            assertTrue(step.x() > 0.5f);
            assertEquals(100.0f, step.y(), 0.0001f);
            assertEquals(0, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.BLOCK_DUST
                            || particle.getType() == WorldParticle.Type.FOOTSTEP)
                    .count());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Sprinting players should kick up old tile-crack particles")
    void sprintingPlayersEmitTileCrackParticles() throws ReflectiveOperationException {
        World world = new World(514L);
        try {
            prepareWalkway(world, BlockType.GRASS);
            Player player = new Player(0.5f, 100.0f, 0.5f);
            setPlayerBoolean(player, "onGround", true);
            setPlayerBoolean(player, "sprinting", true);
            setPlayerBoolean(player, "movementInputActive", true);
            player.getVelocity().set(5.612f, 0.0f, 0.0f);

            player.update(1.0f / 20.0f, world);

            WorldParticle sprintParticle = world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.BLOCK_CRACK)
                    .findFirst()
                    .orElseThrow();
            assertSame(BlockType.GRASS, sprintParticle.getBlockParticleType());
            assertEquals(Block.FACE_BOTTOM, sprintParticle.getBlockParticleFace());
            assertTrue(sprintParticle.getRenderY(0.0f) > 100.0f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Sprinting state should not emit terrain chips while underwater")
    void sprintingDoesNotEmitTileCrackParticlesUnderwater() throws ReflectiveOperationException {
        World world = new World(515L);
        try {
            clearPlayerWaterFixture(world);
            world.setBlock(0, 90, 0, BlockType.WATER, 0);
            world.setBlock(0, 91, 0, BlockType.WATER, 0);
            Player player = new Player(0.5f, 90.0f, 0.5f);
            setPlayerBoolean(player, "sprinting", true);
            setPlayerBoolean(player, "movementInputActive", true);
            player.getVelocity().set(5.612f, 0.0f, 0.0f);

            player.update(1.0f / 20.0f, world);

            assertEquals(0, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.BLOCK_CRACK)
                    .count());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player entering water should emit Release-style splash and bubble particles")
    void playerEnteringWaterEmitsSplashAndBubbleParticles() {
        World world = new World(513L);
        try {
            Player player = new Player(0.5f, 90.0f, 0.5f);
            clearPlayerWaterFixture(world);
            world.setBlock(0, 90, 0, BlockType.WATER, 0);
            world.setBlock(0, 91, 0, BlockType.WATER, 0);

            player.update(1.0f / 20.0f, world);

            assertTrue(world.getParticles().stream()
                    .anyMatch(particle -> particle.getType() == WorldParticle.Type.BUBBLE));
            assertTrue(world.getParticles().stream()
                    .anyMatch(particle -> particle.getType() == WorldParticle.Type.SPLASH));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Steady swimming should not emit invented movement bubbles")
    void steadySwimmingDoesNotEmitInventedMovementBubbles() throws ReflectiveOperationException {
        World world = new World(516L);
        try {
            Player player = new Player(0.5f, 90.0f, 0.5f);
            clearPlayerWaterFixture(world);
            world.setBlock(0, 90, 0, BlockType.WATER, 0);
            world.setBlock(0, 91, 0, BlockType.WATER, 0);
            player.update(1.0f / 20.0f, world);
            world.getParticles().clear();

            setPlayerBoolean(player, "movementInputActive", true);
            player.getVelocity().set(4.0f, 0.0f, 0.0f);
            player.update(1.0f / 20.0f, world);

            assertEquals(0, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.BUBBLE
                            || particle.getType() == WorldParticle.Type.SPLASH)
                    .count());
        } finally {
            world.cleanup();
        }
    }

    private static float updatedWaterVelocityY(boolean jumpHeld, boolean sneakHeld) throws ReflectiveOperationException {
        World world = new World(504L);
        try {
            setKeyDown(GLFW_KEY_SPACE, jumpHeld);
            setKeyDown(GLFW_KEY_LEFT_SHIFT, sneakHeld);
            Player player = new Player(0.5f, 90.0f, 0.5f);
            clearPlayerWaterFixture(world);
            world.setBlock(0, 90, 0, BlockType.WATER, 0);
            world.setBlock(0, 91, 0, BlockType.WATER, 0);
            assertEquals(BlockType.WATER, world.getBlockIfLoaded(0, 90, 0, BlockType.AIR));
            assertEquals(BlockType.WATER, world.getBlockIfLoaded(0, 91, 0, BlockType.AIR));
            assertEquals(0, world.getCollisionBoxesIfLoaded(0, 90, 0).size());

            player.update(0.05f, world);

            return player.getVelocity().y;
        } finally {
            setKeyDown(GLFW_KEY_SPACE, false);
            setKeyDown(GLFW_KEY_LEFT_SHIFT, false);
            world.cleanup();
        }
    }

    private static float updatedWaterVelocityYWithLegacySurfaceCooldown() throws ReflectiveOperationException {
        World world = new World(505L);
        try {
            setKeyDown(GLFW_KEY_SPACE, true);
            Player player = new Player(0.5f, 90.0f, 0.5f);
            setLegacySurfaceBobbingTimerIfPresent(player, 1.0f);
            clearPlayerWaterFixture(world);
            world.setBlock(0, 90, 0, BlockType.WATER, 0);
            world.setBlock(0, 91, 0, BlockType.WATER, 0);

            player.update(0.05f, world);

            return player.getVelocity().y;
        } finally {
            setKeyDown(GLFW_KEY_SPACE, false);
            world.cleanup();
        }
    }

    private static void clearPlayerWaterFixture(World world) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 89; y <= 93; y++) {
                    world.setBlock(x, y, z, BlockType.AIR, 0);
                }
            }
        }
    }

    private static void prepareWalkway(World world, BlockType floor) {
        for (int x = -1; x <= 8; x++) {
            for (int z = -1; z <= 1; z++) {
                world.setBlock(x, 99, z, floor, 0);
                for (int y = 100; y <= 103; y++) {
                    world.setBlock(x, y, z, BlockType.AIR, 0);
                }
            }
        }
    }

    private static float pushedMobMotionX(float mobX, float mobWidth) {
        return playerMobCollisionPush(mobX, mobWidth).mobMotionX();
    }

    private static CollisionPush playerMobCollisionPush(float mobX, float mobWidth) {
        World world = new World(502L);
        try {
            Player player = new Player(0.0f, 90.0f, 0.0f);
            WideLivingEntity mob = new WideLivingEntity(mobWidth);
            mob.setPosition(mobX, 90.0f, 0.0f);
            world.replaceEntities(List.of(mob));

            player.update(0.05f, world);

            return new CollisionPush(mob.getMotionX(), player.getVelocity().x);
        } finally {
            world.cleanup();
        }
    }

    private record CollisionPush(float mobMotionX, float playerVelocityX) {
    }

    private static float playerFloatConstant(String name) throws ReflectiveOperationException {
        Field field = Player.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getFloat(null);
    }

    private static void setLegacySurfaceBobbingTimerIfPresent(Player player, float value)
            throws ReflectiveOperationException {
        try {
            Field field = Player.class.getDeclaredField("surfaceBobbingTimer");
            field.setAccessible(true);
            field.setFloat(player, value);
        } catch (NoSuchFieldException ignored) {
            // Expected after the Release-style water movement cleanup.
        }
    }

    private static void setPlayerBoolean(Player player, String name, boolean value) throws ReflectiveOperationException {
        Field field = Player.class.getDeclaredField(name);
        field.setAccessible(true);
        field.setBoolean(player, value);
    }

    private static void setKeyDown(int key, boolean down) throws ReflectiveOperationException {
        Field field = Input.class.getDeclaredField("keys");
        field.setAccessible(true);
        ((boolean[]) field.get(null))[key] = down;
    }

    private static final class WideLivingEntity extends LivingEntity {
        private WideLivingEntity(float width) {
            super(width, 1.8f, 20.0f);
        }
    }
}

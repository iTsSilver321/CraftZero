package com.craftzero.entity;

import com.craftzero.entity.mob.Zombie;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Difficulty;
import com.craftzero.main.Player;
import com.craftzero.world.Block;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnderPearlEntityTest {
    @Test
    @DisplayName("Ender pearls teleport their owner to block impact and deal fall damage")
    void enderPearlTeleportsOwnerOnBlockImpact() {
        World world = new World(6230L);
        try {
            Player player = new Player(0.5f, 100.0f, 0.5f);
            player.getStats().restore(20.0f, 20.0f, 5.0f, 15.0f);
            world.setPlayer(player);
            world.setBlock(2, 100, 0, BlockType.STONE, 0);

            EnderPearlEntity pearl = world.spawnEnderPearl(0.5f, 100.5f, 0.5f,
                    3.0f, 0.0f, 0.0f, player);

            world.updateEntities(1.0f / 20.0f);

            assertTrue(pearl.isRemoved());
            assertEquals(2.0f, player.getPosition().x, 0.001f);
            assertEquals(100.5f, player.getPosition().y, 0.001f);
            assertEquals(0.5f, player.getPosition().z, 0.001f);
            assertEquals(15.0f, player.getStats().getHealth(), 0.001f);
            List<WorldParticle> portalParticles = portalParticles(world);
            assertEquals(32, portalParticles.size());
            portalParticles.forEach(particle -> {
                assertEquals(2.0f, particle.getRenderX(0.0f), 0.0001f);
                assertTrue(particle.getRenderY(0.0f) >= 100.5f
                        && particle.getRenderY(0.0f) < 102.5f);
                assertEquals(0.5f, particle.getRenderZ(0.0f), 0.0001f);
                assertEquals(0.0f, particle.getScale(0.0f), 0.0001f);
                assertTrue(particle.getLifetimeTicks() >= 40.0f
                        && particle.getLifetimeTicks() <= 49.0f);
                assertTrue(particle.getData() >= 0.0f && particle.getData() <= 7.0f);
            });
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ender pearl fall damage should ignore difficulty scaling")
    void enderPearlFallDamageIgnoresDifficulty() {
        assertEnderPearlDamageOnDifficulty(Difficulty.PEACEFUL);
        assertEnderPearlDamageOnDifficulty(Difficulty.HARD);
    }

    @Test
    @DisplayName("Ender pearls hit living entities for zero damage before teleporting")
    void enderPearlHitsLivingEntityForZeroDamage() {
        World world = new World(6231L);
        try {
            Player player = new Player(0.5f, 100.0f, 0.5f);
            player.getStats().restore(20.0f, 20.0f, 5.0f, 15.0f);
            world.setPlayer(player);
            Zombie zombie = new Zombie();
            zombie.setPosition(2.0f, 100.0f, 0.5f);
            float beforeHealth = zombie.getHealth();
            world.spawnEntity(zombie);

            EnderPearlEntity pearl = world.spawnEnderPearl(0.5f, 100.5f, 0.5f,
                    3.0f, 0.0f, 0.0f, player);

            world.updateEntities(1.0f / 20.0f);

            assertTrue(pearl.isRemoved());
            assertEquals(beforeHealth, zombie.getHealth(), 0.001f);
            assertEquals(1.6f, player.getPosition().x, 0.001f);
            assertEquals(15.0f, player.getStats().getHealth(), 0.001f);
            assertEquals(32, portalParticles(world).size());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ender pearls should pop paintings before hitting their support block")
    void enderPearlsBreakPaintingsBeforeSupportBlockImpact() {
        World world = new World(6233L);
        try {
            Player player = new Player(0.5f, 70.0f, -3.0f);
            player.getStats().restore(20.0f, 20.0f, 5.0f, 15.0f);
            world.setPlayer(player);
            world.setBlock(0, 70, 0, BlockType.STONE, 0);
            PaintingEntity painting = PaintingEntity.fromSupport(0, 70, 0,
                    Block.FACE_NORTH, PaintingEntity.Art.KEBAB);
            EnderPearlEntity pearl = new EnderPearlEntity(0.5f, 70.5f, -2.0f,
                    0.0f, 0.0f, 3.0f, player);
            world.replaceEntities(List.of(painting, pearl));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(pearl.isRemoved());
            assertTrue(painting.isRemoved());
            assertFalse(world.getEntities().stream()
                    .anyMatch(entity -> entity instanceof PaintingEntity && !entity.isRemoved()));
            assertTrue(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == ItemType.PAINTING && item.getCount() == 1));
            assertEquals(0.5f, player.getPosition().x, 0.001f);
            assertEquals(70.5f, player.getPosition().y, 0.001f);
            assertEquals(-0.1625f, player.getPosition().z, 0.001f);
            assertEquals(15.0f, player.getStats().getHealth(), 0.001f);
            assertEquals(32, portalParticles(world).size());
            assertEquals(BlockType.STONE, world.getBlock(0, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ender pearls should impact boats before backing blocks")
    void enderPearlsImpactBoatsBeforeBlocks() {
        World world = new World(6234L);
        try {
            Player player = new Player(0.5f, 100.0f, 0.5f);
            player.getStats().restore(20.0f, 20.0f, 5.0f, 15.0f);
            world.setPlayer(player);
            world.setBlock(4, 100, 0, BlockType.STONE, 0);
            BoatEntity boat = new BoatEntity(3.0f, 100.0f, 0.5f);
            EnderPearlEntity pearl = new EnderPearlEntity(0.5f, 100.3f, 0.5f,
                    5.0f, 0.0f, 0.0f, player);
            world.replaceEntities(List.of(pearl, boat));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(pearl.isRemoved());
            assertFalse(boat.isRemoved());
            assertEquals(0.0f, boat.getDamage(), 0.001f);
            assertEquals(2.15f, player.getPosition().x, 0.001f);
            assertEquals(100.3f, player.getPosition().y, 0.001f);
            assertEquals(0.5f, player.getPosition().z, 0.001f);
            assertEquals(15.0f, player.getStats().getHealth(), 0.001f);
            assertEquals(32, portalParticles(world).size());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ender pearls should impact minecarts before backing blocks")
    void enderPearlsImpactMinecartsBeforeBlocks() {
        World world = new World(6235L);
        try {
            Player player = new Player(0.5f, 100.0f, 0.5f);
            player.getStats().restore(20.0f, 20.0f, 5.0f, 15.0f);
            world.setPlayer(player);
            world.setBlock(4, 100, 0, BlockType.STONE, 0);
            MinecartEntity minecart = new MinecartEntity(3.0f, 100.0f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            EnderPearlEntity pearl = new EnderPearlEntity(0.5f, 100.3f, 0.5f,
                    5.0f, 0.0f, 0.0f, player);
            world.replaceEntities(List.of(pearl, minecart));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(pearl.isRemoved());
            assertFalse(minecart.isRemoved());
            assertEquals(0.0f, minecart.getDamage(), 0.001f);
            assertEquals(2.41f, player.getPosition().x, 0.001f);
            assertEquals(100.3f, player.getPosition().y, 0.001f);
            assertEquals(0.5f, player.getPosition().z, 0.001f);
            assertEquals(15.0f, player.getStats().getHealth(), 0.001f);
            assertEquals(32, portalParticles(world).size());
        } finally {
            world.cleanup();
        }
    }

    private static void assertEnderPearlDamageOnDifficulty(Difficulty difficulty) {
        World world = new World(6232L + difficulty.id());
        try {
            Player player = new Player(0.5f, 100.0f, 0.5f);
            player.setDifficulty(difficulty);
            player.getStats().restore(20.0f, 20.0f, 5.0f, 15.0f);
            world.setPlayer(player);
            world.setBlock(2, 100, 0, BlockType.STONE, 0);

            EnderPearlEntity pearl = world.spawnEnderPearl(0.5f, 100.5f, 0.5f,
                    3.0f, 0.0f, 0.0f, player);

            world.updateEntities(1.0f / 20.0f);

            assertTrue(pearl.isRemoved());
            assertEquals(15.0f, player.getStats().getHealth(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    private static List<WorldParticle> portalParticles(World world) {
        return world.getParticles().stream()
                .filter(particle -> particle.getType() == WorldParticle.Type.PORTAL)
                .toList();
    }
}

package com.craftzero.entity.mob;

import com.craftzero.entity.ai.MeleeAttackGoal;
import com.craftzero.main.Player;
import com.craftzero.progression.PotionData;
import com.craftzero.progression.PotionEffectResolver;
import com.craftzero.progression.PotionType;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GiantTest {
    @Test
    @DisplayName("Giants should use Release-era scale, health, visuals, and melee strength")
    void giantsUseReleaseScaleHealthAndAttackStrength() {
        World world = new World(9401L);
        try {
            world.getChunkNow(0, 0);
            prepareArena(world, 8, 69, 8);
            Player player = new Player(8.5f, 70.0f, 8.5f);
            player.getStats().update(5.1f, false, false, player.getDifficulty());
            world.setPlayer(player);

            Giant giant = new Giant();
            giant.setPosition(8.5f, 70.0f, 6.6f);
            world.replaceEntities(List.of(giant));

            assertSame(MobDefinition.GIANT, giant.getDefinition());
            assertEquals(100.0f, giant.getMaxHealth(), 0.001f);
            assertEquals(3.6f, giant.getWidth(), 0.001f);
            assertEquals(10.8f, giant.getHeight(), 0.001f);
            assertEquals(6.0f, giant.getRenderScale(), 0.001f);
            assertFalse(giant.burnsInSunlight());
            assertEquals("/textures/mob/zombie.png", giant.getTexturePath());
            assertSame(Mob.MobModelType.HUMANOID, giant.getModelType());

            MeleeAttackGoal attack = giant.getAI().getGoal(MeleeAttackGoal.class);
            assertNotNull(attack);
            giant.getAI().setMoveTarget(player.getPosition().x, player.getPosition().z);
            attack.start();
            attack.tick();

            assertEquals(0.0f, player.getStats().getHealth(), 0.001f);
            assertTrue(player.getStats().isDead());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Giants should count as undead for instant potion inversion")
    void giantsAreUndeadForInstantPotions() {
        Giant giant = new Giant();

        PotionEffectResolver.applyToLiving(giant, new PotionData(PotionType.HEALING, false, false, false), 1.0f);

        assertTrue(PotionEffectResolver.isUndead(giant));
        assertEquals(94.0f, giant.getHealth(), 0.001f);
    }

    private static void prepareArena(World world, int centerX, int groundY, int centerZ) {
        for (int x = centerX - 5; x <= centerX + 5; x++) {
            for (int z = centerZ - 5; z <= centerZ + 5; z++) {
                assertTrue(world.isChunkGeneratedForBlock(x, z));
                world.setBlock(x, groundY, z, BlockType.STONE);
                for (int y = groundY + 1; y <= groundY + 14; y++) {
                    world.setBlock(x, y, z, BlockType.AIR);
                }
            }
        }
    }
}

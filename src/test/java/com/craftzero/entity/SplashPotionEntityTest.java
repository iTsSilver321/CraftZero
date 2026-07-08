package com.craftzero.entity;

import com.craftzero.entity.mob.Cow;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.BlockType;
import com.craftzero.progression.PotionData;
import com.craftzero.progression.PotionType;
import com.craftzero.progression.StatusEffectVisuals;
import com.craftzero.progression.StatusEffectType;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SplashPotionEntityTest {

    @Test
    @DisplayName("Direct splash potion hits should apply full Release 1.0 effect strength")
    void directHitAppliesFullStrength() {
        World world = new World(5160L);
        try {
            Cow cow = new Cow();
            cow.setPosition(1.5f, 80.0f, 0.0f);
            SplashPotionEntity potion = new SplashPotionEntity(0.0f, 80.7f, 0.0f,
                    2.0f, 0.0f, 0.0f, null,
                    new PotionData(PotionType.POISON, true, false, false));
            world.replaceEntities(List.of(cow, potion));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(potion.isRemoved());
            assertEquals(1, cow.getActiveEffects().size());
            assertSame(StatusEffectType.POISON, cow.getActiveEffects().get(0).type());
            assertEquals(900, cow.getActiveEffects().get(0).durationTicks());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Splash potion radius should be centered on the actual block impact point")
    void blockImpactUsesImpactPointForSplashRadius() {
        World world = new World(5161L);
        try {
            for (int x = 0; x <= 7; x++) {
                world.setBlock(x, 80, 0, com.craftzero.world.BlockType.AIR, 0);
            }
            world.setBlock(4, 80, 0, com.craftzero.world.BlockType.STONE, 0);
            Cow cow = new Cow();
            cow.setPosition(6.5f, 80.0f, 0.5f);
            SplashPotionEntity potion = new SplashPotionEntity(0.5f, 80.7f, 0.5f,
                    5.0f, 0.0f, 0.0f, null,
                    new PotionData(PotionType.POISON, true, false, false));
            world.replaceEntities(List.of(cow, potion));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(potion.isRemoved());
            assertEquals(1, cow.getActiveEffects().size());
            assertSame(StatusEffectType.POISON, cow.getActiveEffects().get(0).type());
            assertEquals(337, cow.getActiveEffects().get(0).durationTicks());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Splash potions should shatter and emit colored spell particles on impact")
    void splashPotionsEmitItemCrackParticlesOnImpact() {
        World world = new World(5163L);
        PotionData poisonSplash = new PotionData(PotionType.POISON, true, false, false);
        try {
            for (int x = 0; x <= 5; x++) {
                world.setBlock(x, 80, 0, BlockType.AIR, 0);
            }
            world.setBlock(4, 80, 0, BlockType.STONE, 0);
            SplashPotionEntity potion = new SplashPotionEntity(0.5f, 80.7f, 0.5f,
                    5.0f, 0.0f, 0.0f, null, poisonSplash);
            world.replaceEntities(List.of(potion));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(potion.isRemoved());
            assertEquals(8, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.ITEM_CRACK)
                    .filter(particle -> particle.getItemParticleType() == ItemType.POTION)
                    .count());
            int baseColor = StatusEffectVisuals.potionColor(poisonSplash);
            assertEquals(100, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.SPELL)
                    .filter(particle -> particleColorIsSourceBrightnessBand(particle, baseColor))
                    .count());
            assertEquals(0, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.INSTANT_SPELL)
                    .count());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Instant splash potions should use the instant spell particle type")
    void instantSplashPotionsEmitInstantSpellParticlesOnImpact() {
        World world = new World(5164L);
        PotionData healingSplash = new PotionData(PotionType.HEALING, true, false, false);
        try {
            for (int x = 0; x <= 5; x++) {
                world.setBlock(x, 80, 0, BlockType.AIR, 0);
            }
            world.setBlock(4, 80, 0, BlockType.STONE, 0);
            SplashPotionEntity potion = new SplashPotionEntity(0.5f, 80.7f, 0.5f,
                    5.0f, 0.0f, 0.0f, null, healingSplash);
            world.replaceEntities(List.of(potion));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(potion.isRemoved());
            int baseColor = StatusEffectVisuals.potionColor(healingSplash);
            assertEquals(100, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.INSTANT_SPELL)
                    .filter(particle -> particleColorIsSourceBrightnessBand(particle, baseColor))
                    .count());
            assertEquals(0, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.SPELL)
                    .count());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Splash potions should impact boats before backing blocks")
    void splashPotionsImpactBoatsBeforeBlocks() {
        World world = new World(5162L);
        try {
            world.setBlock(4, 100, 0, BlockType.STONE, 0);
            SplashPotionEntity potion = new SplashPotionEntity(0.5f, 100.3f, 0.5f,
                    5.0f, 0.0f, 0.0f, null,
                    new PotionData(PotionType.POISON, true, false, false));
            BoatEntity boat = new BoatEntity(3.0f, 100.0f, 0.5f);
            Cow cow = new Cow();
            cow.setPosition(2.5f, 100.0f, 2.0f);
            world.replaceEntities(List.of(potion, boat, cow));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(potion.isRemoved());
            assertFalse(boat.isRemoved());
            assertEquals(0.0f, boat.getDamage(), 0.001f);
            assertEquals(2.15f, potion.getX(), 0.001f);
            assertEquals(100.3f, potion.getY(), 0.001f);
            assertEquals(0.5f, potion.getZ(), 0.001f);
            assertEquals(1, cow.getActiveEffects().size());
            assertSame(StatusEffectType.POISON, cow.getActiveEffects().get(0).type());
            assertTrue(cow.getActiveEffects().get(0).durationTicks() > 500,
                    "Vehicle impact should splash from the boat, not the backing block");
        } finally {
            world.cleanup();
        }
    }

    private static boolean particleColorIsSourceBrightnessBand(WorldParticle particle, int baseColor) {
        int color = Math.round(particle.getData());
        return channelInBrightnessBand(color, baseColor, 16)
                && channelInBrightnessBand(color, baseColor, 8)
                && channelInBrightnessBand(color, baseColor, 0);
    }

    private static boolean channelInBrightnessBand(int color, int baseColor, int shift) {
        int base = (baseColor >> shift) & 0xff;
        int channel = (color >> shift) & 0xff;
        if (base == 0) {
            return channel == 0;
        }
        return channel >= Math.floor(base * 0.74f) && channel <= base;
    }
}

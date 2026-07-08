package com.craftzero.graphics;

import com.craftzero.inventory.ItemType;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleRendererTest {
    @Test
    @DisplayName("Portal particles should render with the old purple shimmer tint")
    void portalParticlesUsePurpleTint() {
        Vector3f tint = ParticleRenderer.tintFor(new WorldParticle(WorldParticle.Type.PORTAL,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.25f, 40));

        assertEquals(ParticleRenderer.PORTAL_TINT.x, tint.x, 0.0001f);
        assertEquals(ParticleRenderer.PORTAL_TINT.y, tint.y, 0.0001f);
        assertEquals(ParticleRenderer.PORTAL_TINT.z, tint.z, 0.0001f);
    }

    @Test
    @DisplayName("Mob spell particles should render from packed potion RGB data")
    void mobSpellParticlesUsePackedRgbTint() {
        Vector3f tint = ParticleRenderer.tintFor(new WorldParticle(WorldParticle.Type.MOB_SPELL,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.25f, 40, 0x336699));
        Vector3f spellTint = ParticleRenderer.tintFor(new WorldParticle(WorldParticle.Type.SPELL,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.25f, 40, 0x336699));
        Vector3f instantSpellTint = ParticleRenderer.tintFor(new WorldParticle(WorldParticle.Type.INSTANT_SPELL,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.25f, 40, 0x336699));

        assertEquals(0x33 / 255.0f, tint.x, 0.0001f);
        assertEquals(0x66 / 255.0f, tint.y, 0.0001f);
        assertEquals(0x99 / 255.0f, tint.z, 0.0001f);
        assertEquals(tint.x, spellTint.x, 0.0001f);
        assertEquals(tint.y, spellTint.y, 0.0001f);
        assertEquals(tint.z, spellTint.z, 0.0001f);
        assertEquals(tint.x, instantSpellTint.x, 0.0001f);
        assertEquals(tint.y, instantSpellTint.y, 0.0001f);
        assertEquals(tint.z, instantSpellTint.z, 0.0001f);
    }

    @Test
    @DisplayName("Snowball poof particles should render from the snowball item icon")
    void snowballPoofParticlesUseSnowballItemIcon() {
        assertEquals(ItemType.SNOWBALL, ParticleRenderer.itemParticleTypeForRender(
                new WorldParticle(WorldParticle.Type.SNOWBALL_POOF,
                        0.0f, 0.0f, 0.0f,
                        0.0f, 0.0f, 0.0f,
                        0.12f, 12)));
        assertEquals(ItemType.SLIMEBALL, ParticleRenderer.itemParticleTypeForRender(
                new WorldParticle(WorldParticle.Type.SLIME,
                        0.0f, 0.0f, 0.0f,
                        0.0f, 0.0f, 0.0f,
                        0.12f, 12)));
        assertEquals(ItemType.EGG, ParticleRenderer.itemParticleTypeForRender(
                new WorldParticle(WorldParticle.Type.ITEM_CRACK,
                        0.0f, 0.0f, 0.0f,
                        0.0f, 0.0f, 0.0f,
                        0.12f, 12,
                        WorldParticle.itemParticleData(ItemType.EGG))));
    }

    @Test
    @DisplayName("Ambient fluid and redstone particles should use distinct tints")
    void ambientParticlesUseDistinctTints() {
        Vector3f water = ParticleRenderer.tintFor(new WorldParticle(WorldParticle.Type.DRIP_WATER,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.05f, 20));
        Vector3f lava = ParticleRenderer.tintFor(new WorldParticle(WorldParticle.Type.LAVA,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.10f, 20));
        Vector3f redstone = ParticleRenderer.tintFor(new WorldParticle(WorldParticle.Type.RED_DUST,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.08f, 20, 15.0f));

        assertEquals(ParticleRenderer.WATER_DRIP_TINT.x, water.x, 0.0001f);
        assertEquals(ParticleRenderer.WATER_DRIP_TINT.y, water.y, 0.0001f);
        assertEquals(ParticleRenderer.WATER_DRIP_TINT.z, water.z, 0.0001f);
        assertEquals(ParticleRenderer.LAVA_TINT.x, lava.x, 0.0001f);
        assertEquals(ParticleRenderer.LAVA_TINT.y, lava.y, 0.0001f);
        assertEquals(ParticleRenderer.LAVA_TINT.z, lava.z, 0.0001f);
        assertEquals(1.0f, redstone.x, 0.0001f);
        assertEquals(0.20f, redstone.y, 0.0001f);
        assertEquals(0.0f, redstone.z, 0.0001f);

        Vector3f defaultRedDust = ParticleRenderer.tintFor(new WorldParticle(WorldParticle.Type.RED_DUST,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.08f, 20, WorldParticle.RED_DUST_DEFAULT_COLOR_DATA));
        assertEquals(1.0f, defaultRedDust.x, 0.0001f);
        assertEquals(0.0f, defaultRedDust.y, 0.0001f);
        assertEquals(0.0f, defaultRedDust.z, 0.0001f);
    }

    @Test
    @DisplayName("Red dust particles should animate through the old first-row atlas frames")
    void redDustParticlesUseOldAnimatedTextureFrames() {
        WorldParticle particle = new WorldParticle(WorldParticle.Type.RED_DUST,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.08f, 32, 15.0f);

        assertEquals(7, ParticleRenderer.redDustTextureFrame(particle, 0.0f));
        particle.update(1.0f / 20.0f);
        assertEquals(6, ParticleRenderer.redDustTextureFrame(particle, 3.0f));
        particle.update(30.0f / 20.0f);
        assertEquals(0, ParticleRenderer.redDustTextureFrame(particle, 0.0f));
    }

    @Test
    @DisplayName("Portal particles should keep their old random first-row atlas frame")
    void portalParticlesUseOldStaticTextureFrame() {
        WorldParticle particle = new WorldParticle(WorldParticle.Type.PORTAL,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.22f, 32, 5.0f);

        assertEquals(5, ParticleRenderer.portalTextureFrame(particle, 0.0f));
        particle.update(1.0f / 20.0f);
        assertEquals(5, ParticleRenderer.portalTextureFrame(particle, 3.0f));
        particle.update(30.0f / 20.0f);
        assertEquals(5, ParticleRenderer.portalTextureFrame(particle, 0.0f));
    }

    @Test
    @DisplayName("Smoke-family particles should animate through the old first-row atlas frames")
    void smokeFamilyParticlesUseOldAnimatedTextureFrames() {
        WorldParticle smoke = new WorldParticle(WorldParticle.Type.SMOKE,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.22f, 32);
        WorldParticle snowShovel = new WorldParticle(WorldParticle.Type.SNOW_SHOVEL,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.22f, 32);

        assertEquals(7, ParticleRenderer.smokeTextureFrame(smoke, 0.0f));
        smoke.update(1.0f / 20.0f);
        assertEquals(6, ParticleRenderer.smokeTextureFrame(smoke, 3.0f));
        snowShovel.update(31.0f / 20.0f);
        assertEquals(0, ParticleRenderer.smokeTextureFrame(snowShovel, 0.0f));
    }

    @Test
    @DisplayName("Lava particles should use the old lava atlas cell")
    void lavaParticlesUseOldAtlasCell() {
        assertArrayEquals(new float[] {
                8.0f / 256.0f,
                24.0f / 256.0f,
                16.0f / 256.0f,
                32.0f / 256.0f
        }, ParticleRenderer.particleCellUv(49), 0.0001f);
    }

    @Test
    @DisplayName("Splash particles should render from the old rain/splash atlas cell")
    void splashParticlesUseOldRainAtlasCell() {
        assertArrayEquals(new float[] {
                32.0f / 256.0f,
                8.0f / 256.0f,
                40.0f / 256.0f,
                16.0f / 256.0f
        }, ParticleRenderer.particleCellUv(ParticleRenderer.SPLASH_PARTICLE_ATLAS_INDEX), 0.0001f);
    }

    @Test
    @DisplayName("Flame particles should render from the old particle atlas cell")
    void flameParticlesUseOldParticleAtlasCell() {
        assertArrayEquals(new float[] {
                0.0f,
                24.0f / 256.0f,
                8.0f / 256.0f,
                32.0f / 256.0f
        }, ParticleRenderer.particleCellUv(ParticleRenderer.FLAME_PARTICLE_ATLAS_INDEX), 0.0001f);
    }

    @Test
    @DisplayName("Crit particles should render from old cell 65 and fade green/blue")
    void critParticlesUseOldAtlasCellAndTintFade() {
        WorldParticle crit = new WorldParticle(WorldParticle.Type.CRIT,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.16f, 8, 0.8f);

        assertArrayEquals(new float[] {
                8.0f / 256.0f,
                32.0f / 256.0f,
                16.0f / 256.0f,
                40.0f / 256.0f
        }, ParticleRenderer.particleCellUv(ParticleRenderer.CRIT_PARTICLE_ATLAS_INDEX), 0.0001f);

        Vector3f initial = ParticleRenderer.tintFor(crit);
        crit.update(1.0f / 20.0f);
        Vector3f later = ParticleRenderer.tintFor(crit);

        assertEquals(0.8f, initial.x, 0.0001f);
        assertEquals(0.8f, initial.y, 0.0001f);
        assertEquals(0.8f, initial.z, 0.0001f);
        assertEquals(initial.x, later.x, 0.0001f);
        assertTrue(later.y < later.x);
        assertTrue(later.z < later.y);
    }

    @Test
    @DisplayName("Water and lava drips should use the old hanging, falling, and lava-ground cells")
    void dripParticlesUseOldAtlasCells() {
        WorldParticle water = new WorldParticle(WorldParticle.Type.DRIP_WATER,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.045f, 80);

        assertEquals(ParticleRenderer.DRIP_HANG_PARTICLE_ATLAS_INDEX,
                ParticleRenderer.dripTextureAtlasIndex(water, 0.0f));
        water.update(41.0f / 20.0f);
        assertEquals(ParticleRenderer.DRIP_FALL_PARTICLE_ATLAS_INDEX,
                ParticleRenderer.dripTextureAtlasIndex(water, 0.0f));
        assertArrayEquals(new float[] {
                0.0f,
                56.0f / 256.0f,
                8.0f / 256.0f,
                64.0f / 256.0f
        }, ParticleRenderer.particleCellUv(ParticleRenderer.DRIP_FALL_PARTICLE_ATLAS_INDEX), 0.0001f);
        assertArrayEquals(new float[] {
                8.0f / 256.0f,
                56.0f / 256.0f,
                16.0f / 256.0f,
                64.0f / 256.0f
        }, ParticleRenderer.particleCellUv(ParticleRenderer.DRIP_HANG_PARTICLE_ATLAS_INDEX), 0.0001f);

        World world = new World(12001L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            WorldParticle lava = new WorldParticle(WorldParticle.Type.DRIP_LAVA,
                    0.5f, 70.0f, 0.5f,
                    0.0f, 0.0f, 0.0f,
                    0.045f, 80);
            world.getParticles().add(lava);
            for (int i = 0; i < 60 && !lava.isOnGround(); i++) {
                world.updateParticles(1.0f / 20.0f);
            }

            assertEquals(ParticleRenderer.DRIP_LAVA_GROUND_PARTICLE_ATLAS_INDEX,
                    ParticleRenderer.dripTextureAtlasIndex(lava, 0.0f));
            assertArrayEquals(new float[] {
                    16.0f / 256.0f,
                    56.0f / 256.0f,
                    24.0f / 256.0f,
                    64.0f / 256.0f
            }, ParticleRenderer.particleCellUv(ParticleRenderer.DRIP_LAVA_GROUND_PARTICLE_ATLAS_INDEX), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Lava drips should warm-shift from yellow orange toward red")
    void lavaDripParticlesUseOldColorShift() {
        WorldParticle lava = new WorldParticle(WorldParticle.Type.DRIP_LAVA,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.045f, 80);

        Vector3f initial = ParticleRenderer.tintFor(lava);
        lava.update(40.0f / 20.0f);
        Vector3f later = ParticleRenderer.tintFor(lava);

        assertEquals(1.0f, initial.x, 0.0001f);
        assertEquals(1.0f, initial.y, 0.0001f);
        assertEquals(0.5f, initial.z, 0.0001f);
        assertEquals(1.0f, later.x, 0.0001f);
        assertTrue(later.y < initial.y);
        assertTrue(later.z < initial.z);
    }

    @Test
    @DisplayName("Splash potion spell particles should use old spell atlas frames")
    void splashPotionSpellParticlesUseOldAtlasFrames() {
        WorldParticle spell = new WorldParticle(WorldParticle.Type.SPELL,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.13f, 32, 0x336699);
        WorldParticle instant = new WorldParticle(WorldParticle.Type.INSTANT_SPELL,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.13f, 32, 0xCC3333);

        assertEquals(135, ParticleRenderer.spellTextureAtlasIndex(spell, 0.0f));
        assertEquals(151, ParticleRenderer.spellTextureAtlasIndex(instant, 0.0f));
        spell.update(31.0f / 20.0f);
        instant.update(31.0f / 20.0f);

        assertEquals(128, ParticleRenderer.spellTextureAtlasIndex(spell, 0.0f));
        assertEquals(144, ParticleRenderer.spellTextureAtlasIndex(instant, 0.0f));
        assertArrayEquals(new float[] {
                0.0f,
                64.0f / 256.0f,
                8.0f / 256.0f,
                72.0f / 256.0f
        }, ParticleRenderer.particleCellUv(128), 0.0001f);
        assertArrayEquals(new float[] {
                0.0f,
                72.0f / 256.0f,
                8.0f / 256.0f,
                80.0f / 256.0f
        }, ParticleRenderer.particleCellUv(144), 0.0001f);
    }

    @Test
    @DisplayName("Enchantment-table particles should render with pale source glyph tint")
    void enchantmentTableParticlesUseSourceGlyphTint() {
        Vector3f tint = ParticleRenderer.tintFor(new WorldParticle(WorldParticle.Type.ENCHANTMENT_TABLE,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.14f, 20, 3.0f));

        assertEquals(0.90f, tint.x, 0.0001f);
        assertEquals(0.90f, tint.y, 0.0001f);
        assertEquals(1.0f, tint.z, 0.0001f);
    }

    @Test
    @DisplayName("Suspended water particles should render with subtle underwater tint")
    void suspendedWaterParticlesUseUnderwaterTint() {
        Vector3f tint = ParticleRenderer.tintFor(new WorldParticle(WorldParticle.Type.SUSPENDED,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.04f, 40));

        assertEquals(ParticleRenderer.SUSPENDED_TINT.x, tint.x, 0.0001f);
        assertEquals(ParticleRenderer.SUSPENDED_TINT.y, tint.y, 0.0001f);
        assertEquals(ParticleRenderer.SUSPENDED_TINT.z, tint.z, 0.0001f);
    }

    @Test
    @DisplayName("Depth-suspended particles should render with void-fog tint")
    void depthSuspendedParticlesUseVoidFogTint() {
        Vector3f tint = ParticleRenderer.tintFor(new WorldParticle(WorldParticle.Type.DEPTH_SUSPEND,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.05f, 50));

        assertEquals(ParticleRenderer.DEPTH_SUSPEND_TINT.x, tint.x, 0.0001f);
        assertEquals(ParticleRenderer.DEPTH_SUSPEND_TINT.y, tint.y, 0.0001f);
        assertEquals(ParticleRenderer.DEPTH_SUSPEND_TINT.z, tint.z, 0.0001f);
    }

    @Test
    @DisplayName("Magic crit particles should render with enchanted hit tint")
    void magicCritParticlesUseEnchantedHitTint() {
        WorldParticle particle = new WorldParticle(WorldParticle.Type.MAGIC_CRIT,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.18f, 10);
        Vector3f tint = ParticleRenderer.tintFor(particle);

        assertEquals(ParticleRenderer.MAGIC_CRIT_TINT.x, tint.x, 0.0001f);
        assertEquals(ParticleRenderer.MAGIC_CRIT_TINT.y, tint.y, 0.0001f);
        assertEquals(ParticleRenderer.MAGIC_CRIT_TINT.z, tint.z, 0.0001f);

        particle.update(1.0f / 20.0f);
        Vector3f later = ParticleRenderer.tintFor(particle);

        assertEquals(ParticleRenderer.MAGIC_CRIT_TINT.x, later.x, 0.0001f);
        assertTrue(later.y < ParticleRenderer.MAGIC_CRIT_TINT.y);
        assertTrue(later.z < ParticleRenderer.MAGIC_CRIT_TINT.z);
    }

    @Test
    @DisplayName("Mycelium feedback particles should use the old town-aura tint")
    void myceliumFeedbackParticlesUseTownAuraTint() {
        Vector3f townAura = ParticleRenderer.tintFor(new WorldParticle(WorldParticle.Type.TOWN_AURA,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.04f, 30));

        assertEquals(ParticleRenderer.TOWN_AURA_TINT.x, townAura.x, 0.0001f);
        assertEquals(ParticleRenderer.TOWN_AURA_TINT.y, townAura.y, 0.0001f);
        assertEquals(ParticleRenderer.TOWN_AURA_TINT.z, townAura.z, 0.0001f);
    }
}

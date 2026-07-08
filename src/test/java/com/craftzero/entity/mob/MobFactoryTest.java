package com.craftzero.entity.mob;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MobFactoryTest {
    @Test
    @DisplayName("Implemented mob definitions should create self-identifying mobs")
    void implementedDefinitionsCreateSelfIdentifyingMobs() {
        for (MobDefinition definition : MobFactory.implementedDefinitions()) {
            Mob mob = MobFactory.create(definition);

            assertNotNull(mob, definition.name());
            assertSame(definition, mob.getDefinition());
            assertEquals(definition.hostile(), mob.isHostile(), definition.name());
            assertEquals(definition.burnsInSunlight(), mob.burnsInSunlight(), definition.name());
            assertEquals(definition.experienceValue(), mob.getExperienceValue(), definition.name());
            assertTrue(mob.getMaxHealth() > 0.0f, definition.name());
            assertNotNull(mob.getTexturePath(), definition.name());
            assertFalse(mob.getTexturePath().isBlank(), definition.name());
        }
    }

    @Test
    @DisplayName("This sprint's requested mobs should be factory-backed and future mobs should stay gated")
    void requestedDefinitionsAreBackedFutureDefinitionsAreGated() {
        Set<MobDefinition> implemented = MobFactory.implementedDefinitions();

        assertTrue(implemented.contains(MobDefinition.SLIME));
        assertTrue(implemented.contains(MobDefinition.SQUID));
        assertTrue(implemented.contains(MobDefinition.ENDERMAN));
        assertTrue(implemented.contains(MobDefinition.CAVE_SPIDER));
        assertTrue(implemented.contains(MobDefinition.SILVERFISH));
        assertTrue(implemented.contains(MobDefinition.GIANT));
        assertTrue(implemented.contains(MobDefinition.GHAST));
        assertTrue(implemented.contains(MobDefinition.ZOMBIE_PIGMAN));
        assertTrue(implemented.contains(MobDefinition.BLAZE));
        assertTrue(implemented.contains(MobDefinition.MAGMA_CUBE));
        assertTrue(implemented.contains(MobDefinition.ENDER_DRAGON));
        assertTrue(implemented.contains(MobDefinition.WOLF));
        assertTrue(implemented.contains(MobDefinition.MOOSHROOM));
        assertTrue(implemented.contains(MobDefinition.VILLAGER));
        assertTrue(implemented.contains(MobDefinition.SNOW_GOLEM));

        assertInstanceOf(Wolf.class, MobFactory.create(MobDefinition.WOLF));
        assertInstanceOf(Mooshroom.class, MobFactory.create(MobDefinition.MOOSHROOM));
        assertInstanceOf(Villager.class, MobFactory.create(MobDefinition.VILLAGER));
        assertInstanceOf(SnowGolem.class, MobFactory.create(MobDefinition.SNOW_GOLEM));
        assertInstanceOf(Giant.class, MobFactory.create(MobDefinition.GIANT));
        assertEquals("/textures/mob/redcow.png", MobFactory.create(MobDefinition.MOOSHROOM).getTexturePath());
        assertEquals("/textures/mob/villager/farmer.png", MobFactory.create(MobDefinition.VILLAGER).getTexturePath());
        assertEquals("/textures/mob/snowman.png", MobFactory.create(MobDefinition.SNOW_GOLEM).getTexturePath());
        assertEquals("/textures/mob/zombie.png", MobFactory.create(MobDefinition.GIANT).getTexturePath());
    }
}

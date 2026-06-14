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
        assertTrue(implemented.contains(MobDefinition.GHAST));
        assertTrue(implemented.contains(MobDefinition.ZOMBIE_PIGMAN));
        assertTrue(implemented.contains(MobDefinition.BLAZE));
        assertTrue(implemented.contains(MobDefinition.MAGMA_CUBE));

        assertNull(MobFactory.create(MobDefinition.WOLF));
        assertNull(MobFactory.create(MobDefinition.MOOSHROOM));
        assertNull(MobFactory.create(MobDefinition.VILLAGER));
        assertNull(MobFactory.create(MobDefinition.SNOW_GOLEM));
        assertNull(MobFactory.create(MobDefinition.ENDER_DRAGON));
    }
}

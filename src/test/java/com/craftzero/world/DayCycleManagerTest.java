package com.craftzero.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DayCycleManagerTest {
    @Test
    @DisplayName("Day cycle should initialize lighting before the first update")
    void lightingInitializedOnConstruction() {
        DayCycleManager manager = new DayCycleManager();

        assertTrue(manager.getSkyColor().lengthSquared() > 0.0f);
        assertTrue(manager.getSunDirection().lengthSquared() > 0.9f);
        assertTrue(manager.isDay());
        assertTrue(manager.isDaylightBurnTime());
    }

    @Test
    @DisplayName("Day cycle should advance only when update is called")
    void timeAdvancesOnlyOnUpdate() {
        DayCycleManager manager = new DayCycleManager();
        float initialTime = manager.getTime();

        assertEquals(initialTime, manager.getTime(), 0.001f);
        manager.update(1.0f);

        assertEquals(initialTime + 20.0f, manager.getTime(), 0.001f);
    }
}

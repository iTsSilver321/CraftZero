package com.craftzero.save;

import com.craftzero.entity.Entity;
import com.craftzero.entity.mob.Mob;
import com.craftzero.entity.mob.Pig;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.world.DayCycleManager;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnimalBreedingSaveTest {
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Animal love-mode ticks should round-trip through save/load")
    void animalLoveModeRoundTrips() throws Exception {
        SaveManager manager = new SaveManager(tempDir.resolve("breeding-world"));
        World world = new World(6285L);
        World restored = null;
        try {
            Pig pig = new Pig();
            pig.setPosition(1.0f, 70.0f, 0.0f);
            assertTrue(pig.feedBreedingItem(ItemType.WHEAT));
            world.setSaveManager(manager);
            world.replaceEntities(List.of(pig));

            manager.save(world, new Player(0.0f, 70.0f, 0.0f), new DayCycleManager());

            SaveManager.LevelData loaded = manager.loadLevelIfExists();
            assertNotNull(loaded);
            restored = new World(loaded.seed);
            restored.setSaveManager(manager);
            manager.applyLevel(loaded, new Player(0.0f, 70.0f, 0.0f), new DayCycleManager(), restored);

            Entity restoredEntity = restored.getEntities().stream()
                    .filter(Pig.class::isInstance)
                    .findFirst()
                    .orElseThrow();
            Pig restoredPig = (Pig) restoredEntity;
            assertEquals(Mob.LOVE_MODE_TICKS, restoredPig.getLoveTicks());
        } finally {
            world.cleanup();
            if (restored != null) {
                restored.cleanup();
            }
        }
    }
}

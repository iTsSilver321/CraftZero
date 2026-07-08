package com.craftzero.save;

import com.craftzero.entity.Entity;
import com.craftzero.entity.mob.Pig;
import com.craftzero.main.Player;
import com.craftzero.world.DayCycleManager;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PigSaveTest {
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Saddled pig state should round-trip through level save data")
    void saddledPigRoundTrips() throws Exception {
        SaveManager manager = new SaveManager(tempDir.resolve("pig-world"));
        World world = new World(6271L);
        World restoredWorld = null;
        try {
            Pig pig = new Pig();
            pig.setPosition(1.5f, 70.0f, 2.5f);
            pig.setSaddled(true);
            world.setSaveManager(manager);
            world.replaceEntities(List.of(pig));

            manager.save(world, new Player(0.0f, 70.0f, 0.0f), new DayCycleManager());

            SaveManager.LevelData loaded = manager.loadLevelIfExists();
            assertNotNull(loaded);
            restoredWorld = new World(loaded.seed);
            restoredWorld.setSaveManager(manager);
            manager.applyLevel(loaded, new Player(0.0f, 70.0f, 0.0f), new DayCycleManager(), restoredWorld);

            assertEquals(1, restoredWorld.getEntities().size());
            Entity restored = restoredWorld.getEntities().get(0);
            assertInstanceOf(Pig.class, restored);
            Pig restoredPig = (Pig) restored;
            assertTrue(restoredPig.isSaddled());
            assertEquals(1.5f, restoredPig.getX(), 0.001f);
            assertEquals(2.5f, restoredPig.getZ(), 0.001f);
        } finally {
            world.cleanup();
            if (restoredWorld != null) {
                restoredWorld.cleanup();
            }
        }
    }
}

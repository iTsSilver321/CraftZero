package com.craftzero.save;

import com.craftzero.entity.BoatEntity;
import com.craftzero.entity.Entity;
import com.craftzero.main.Player;
import com.craftzero.world.DayCycleManager;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BoatSaveTest {
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Boat entities round-trip through level save data")
    void boatEntityRoundTrips() throws Exception {
        SaveManager manager = new SaveManager(tempDir.resolve("boat-world"));
        World world = new World(6220L);
        World restoredWorld = null;
        try {
            BoatEntity boat = new BoatEntity(1.5f, 70.25f, 2.5f);
            boat.setMotion(0.12f, 0.01f, -0.05f);
            boat.setYaw(35.0f);
            boat.setDamage(18.0f);
            boat.setTicksExisted(42);
            world.setSaveManager(manager);
            world.replaceEntities(List.of(boat));

            manager.save(world, new Player(0.0f, 70.0f, 0.0f), new DayCycleManager());

            SaveManager.LevelData loaded = manager.loadLevelIfExists();
            assertNotNull(loaded);
            restoredWorld = new World(loaded.seed);
            restoredWorld.setSaveManager(manager);
            manager.applyLevel(loaded, new Player(0.0f, 70.0f, 0.0f), new DayCycleManager(), restoredWorld);

            assertEquals(1, restoredWorld.getEntities().size());
            Entity restored = restoredWorld.getEntities().get(0);
            assertInstanceOf(BoatEntity.class, restored);
            BoatEntity restoredBoat = (BoatEntity) restored;
            assertEquals(1.5f, restoredBoat.getX(), 0.001f);
            assertEquals(70.25f, restoredBoat.getY(), 0.001f);
            assertEquals(2.5f, restoredBoat.getZ(), 0.001f);
            assertEquals(0.12f, restoredBoat.getMotionX(), 0.001f);
            assertEquals(-0.05f, restoredBoat.getMotionZ(), 0.001f);
            assertEquals(35.0f, restoredBoat.getYaw(), 0.001f);
            assertEquals(18.0f, restoredBoat.getDamage(), 0.001f);
            assertEquals(42, restoredBoat.getTicksExisted());
        } finally {
            world.cleanup();
            if (restoredWorld != null) {
                restoredWorld.cleanup();
            }
        }
    }
}

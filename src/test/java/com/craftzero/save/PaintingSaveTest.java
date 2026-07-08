package com.craftzero.save;

import com.craftzero.entity.Entity;
import com.craftzero.entity.PaintingEntity;
import com.craftzero.main.Player;
import com.craftzero.world.Block;
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

class PaintingSaveTest {
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Painting entities round-trip through level save data")
    void paintingEntityRoundTrips() throws Exception {
        SaveManager manager = new SaveManager(tempDir.resolve("painting-world"));
        World world = new World(6245L);
        World restoredWorld = null;
        try {
            PaintingEntity painting = PaintingEntity.fromSupport(4, 75, -2,
                    Block.FACE_EAST, PaintingEntity.Art.MATCH);
            painting.setMotion(0.01f, 0.02f, 0.03f);
            painting.setTicksExisted(37);
            world.setSaveManager(manager);
            world.replaceEntities(List.of(painting));

            manager.save(world, new Player(0.0f, 70.0f, 0.0f), new DayCycleManager());

            SaveManager.LevelData loaded = manager.loadLevelIfExists();
            assertNotNull(loaded);
            restoredWorld = new World(loaded.seed);
            restoredWorld.setSaveManager(manager);
            manager.applyLevel(loaded, new Player(0.0f, 70.0f, 0.0f), new DayCycleManager(), restoredWorld);

            assertEquals(1, restoredWorld.getEntities().size());
            Entity restored = restoredWorld.getEntities().get(0);
            assertInstanceOf(PaintingEntity.class, restored);
            PaintingEntity restoredPainting = (PaintingEntity) restored;
            assertEquals(PaintingEntity.Art.MATCH, restoredPainting.getArt());
            assertEquals(Block.FACE_EAST, restoredPainting.getFacing());
            assertEquals(painting.getX(), restoredPainting.getX(), 0.001f);
            assertEquals(painting.getY(), restoredPainting.getY(), 0.001f);
            assertEquals(painting.getZ(), restoredPainting.getZ(), 0.001f);
            assertEquals(0.01f, restoredPainting.getMotionX(), 0.001f);
            assertEquals(0.02f, restoredPainting.getMotionY(), 0.001f);
            assertEquals(0.03f, restoredPainting.getMotionZ(), 0.001f);
            assertEquals(37, restoredPainting.getTicksExisted());
        } finally {
            world.cleanup();
            if (restoredWorld != null) {
                restoredWorld.cleanup();
            }
        }
    }
}

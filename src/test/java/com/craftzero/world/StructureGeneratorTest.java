package com.craftzero.world;

import com.craftzero.entity.mob.Villager;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.tile.ChestTileEntity;
import com.craftzero.world.tile.MonsterSpawnerTileEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class StructureGeneratorTest {
    @Test
    @DisplayName("Stronghold generation should be deterministic and dimension-gated")
    void strongholdGenerationIsDeterministicAndDimensionGated() {
        long seed = 8128L;
        StructureGenerator structures = new StructureGenerator();
        ReleaseOneWorldGenerator overworldGenerator = new ReleaseOneWorldGenerator(seed, Dimension.OVERWORLD);
        World world = new World(seed);
        try {
            Chunk first = null;
            Chunk second = null;
            int foundX = 0;
            int foundZ = 0;
            for (int cx = -80; cx <= 80 && first == null; cx++) {
                for (int cz = -80; cz <= 80; cz++) {
                    Chunk candidate = new Chunk(cx, cz);
                    structures.generate(world, candidate, seed, cx, cz, Dimension.OVERWORLD, overworldGenerator);
                    if (contains(candidate, BlockType.END_PORTAL_FRAME)) {
                        first = candidate;
                        foundX = cx;
                        foundZ = cz;
                        break;
                    }
                }
            }
            assertNotNull(first, "Expected one first-pass stronghold candidate near spawn");

            second = new Chunk(foundX, foundZ);
            structures.generate(world, second, seed, foundX, foundZ, Dimension.OVERWORLD, overworldGenerator);
            assertEquals(count(first, BlockType.END_PORTAL_FRAME), count(second, BlockType.END_PORTAL_FRAME));
            assertEquals(count(first, BlockType.STONE_BRICK), count(second, BlockType.STONE_BRICK));
            assertTrue(generatedStrongholdNeighborhoodContains(structures, world, seed, foundX, foundZ,
                    overworldGenerator, BlockType.MOB_SPAWNER));

            Chunk netherChunk = new Chunk(foundX, foundZ);
            structures.generate(world, netherChunk, seed, foundX, foundZ, Dimension.NETHER,
                    new ReleaseOneWorldGenerator(seed, Dimension.NETHER));
            assertFalse(contains(netherChunk, BlockType.END_PORTAL_FRAME));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Structure locator should point at a generated stronghold chunk")
    void locateStrongholdMatchesGeneratedPortalRoom() {
        long seed = 24681357L;
        StructureGenerator structures = new StructureGenerator();
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(seed, Dimension.OVERWORLD);
        StructureGenerator.StructureLocation location = structures.locate(seed, Dimension.OVERWORLD,
                StructureType.STRONGHOLD, 0, 0, generator);
        assertNotNull(location);

        StructureStart start = new StructurePlanner()
                .startsForChunk(seed, Dimension.OVERWORLD, location.chunkX(), location.chunkZ(), generator).stream()
                .filter(candidate -> candidate.type() == StructureType.STRONGHOLD)
                .filter(candidate -> candidate.chunkX() == location.chunkX() && candidate.chunkZ() == location.chunkZ())
                .findFirst()
                .orElseThrow();
        StructurePiece portal = start.pieces().stream()
                .filter(piece -> "PORTAL".equals(strongholdRoomName(piece)))
                .findFirst()
                .orElseThrow();
        assertEquals(portal.bounds().centerX(), location.blockX());
        assertEquals(portal.bounds().centerY(), location.blockY());
        assertEquals(portal.bounds().centerZ(), location.blockZ());

        World world = new World(seed);
        try {
            boolean found = false;
            for (int cx = location.chunkX() - 5; cx <= location.chunkX() + 5 && !found; cx++) {
                for (int cz = location.chunkZ() - 5; cz <= location.chunkZ() + 5; cz++) {
                    Chunk chunk = new Chunk(cx, cz);
                    structures.generate(world, chunk, seed, cx, cz, Dimension.OVERWORLD, generator);
                    if (contains(chunk, BlockType.END_PORTAL_FRAME)) {
                        found = true;
                        break;
                    }
                }
            }
            assertTrue(found, "Locator should resolve to a stronghold with a portal room nearby");
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Structure locator should point at generated village starts")
    void locateVillageMatchesGeneratedStart() {
        long seed = 38L;
        StructureGenerator structures = new StructureGenerator();
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(seed, Dimension.OVERWORLD);
        StructureStart expected = villageStart(seed, -79, 9, generator);
        StructureBoundingBox expectedBounds = expected.bounds();

        StructureGenerator.StructureLocation location = structures.locate(seed, Dimension.OVERWORLD,
                StructureType.VILLAGE, expectedBounds.centerX(), expectedBounds.centerZ(), generator);

        assertLocatedStructureMatchesStart(location, expected);
        Chunk chunk = new Chunk(location.chunkX(), location.chunkZ());
        structures.generate(null, chunk, seed, location.chunkX(), location.chunkZ(), Dimension.OVERWORLD,
                generator);
        assertVillageWell(chunk, villageWellGroundY(generator, location.chunkX(), location.chunkZ()));
    }

    @Test
    @DisplayName("Structure locator should point at generated mineshaft starts")
    void locateMineshaftMatchesGeneratedStart() {
        long seed = 4L;
        StructureGenerator structures = new StructureGenerator();
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(seed, Dimension.OVERWORLD);
        StructureStart expected = mineshaftStart(seed, -16, 2, generator);
        StructureBoundingBox expectedBounds = expected.bounds();

        StructureGenerator.StructureLocation location = structures.locate(seed, Dimension.OVERWORLD,
                StructureType.MINESHAFT, expectedBounds.centerX(), expectedBounds.centerZ(), generator);

        assertLocatedStructureMatchesStart(location, expected);
        Chunk chunk = new Chunk(location.chunkX(), location.chunkZ());
        chunk.setBlock(2, 42, 2, BlockType.STONE);
        chunk.setBlock(9, 42, 9, BlockType.STONE);
        structures.generate(null, chunk, seed, location.chunkX(), location.chunkZ(), Dimension.OVERWORLD,
                generator);
        assertSame(BlockType.DIRT, chunk.getBlock(2, 42, 2));
        assertSame(BlockType.DIRT, chunk.getBlock(9, 42, 9));
    }

    @Test
    @DisplayName("Generated strongholds should use source-sized oriented library pieces")
    void generatedStrongholdLibraryUsesSourceSizedBounds() throws Exception {
        StructurePiece library = findGeneratedStrongholdRoomAcrossSeeds("LIBRARY", 32768);
        StructureBoundingBox box = library.bounds();

        assertTrue((width(box) == 14 && depth(box) == 15)
                        || (width(box) == 15 && depth(box) == 14),
                "Source libraries keep a 14x15 footprint in either orientation");
        assertTrue(height(box) == 6 || height(box) == 11,
                "Source libraries are either the small six-high room or the large eleven-high room");
        assertTrue(strongholdCoordBaseMode(library) >= 0);
        assertTrue(strongholdCountsForWeight(library),
                "Recursive source libraries should consume the weighted library quota");
    }

    @Test
    @DisplayName("Generated strongholds should use source-sized room-crossing pieces")
    void generatedStrongholdCrossingUsesSourceSizedBounds() throws Exception {
        StructurePiece crossing = findGeneratedStrongholdRoomAcrossSeeds("CROSSING", 32768);
        StructureBoundingBox box = crossing.bounds();

        assertEquals(11, width(box));
        assertEquals(7, height(box));
        assertEquals(11, depth(box));
        assertTrue(strongholdCoordBaseMode(crossing) >= 0);
        assertTrue(strongholdCountsForWeight(crossing),
                "Recursive source room crossings should consume the weighted room quota");
    }

    @Test
    @DisplayName("Generated stronghold corridors should use source tube dimensions")
    void generatedStrongholdCorridorsUseSourceTubeDimensions() {
        long seed = 24681357L;
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(seed, Dimension.OVERWORLD);
        StructureGenerator.StructureLocation location = new StructureGenerator().locate(seed, Dimension.OVERWORLD,
                StructureType.STRONGHOLD, 0, 0, generator);
        assertNotNull(location);

        StructureStart start = new StructurePlanner()
                .startsForChunk(seed, Dimension.OVERWORLD, location.chunkX(), location.chunkZ(), generator).stream()
                .filter(candidate -> candidate.type() == StructureType.STRONGHOLD)
                .filter(candidate -> candidate.chunkX() == location.chunkX() && candidate.chunkZ() == location.chunkZ())
                .findFirst()
                .orElseThrow();
        long corridorCount = start.pieces().stream()
                .filter(piece -> "CORRIDOR".equals(strongholdRoomName(piece)))
                .peek(piece -> {
                    StructureBoundingBox box = piece.bounds();
                    assertEquals(5, height(box));
                    assertTrue(width(box) == 5 || depth(box) == 5,
                            "Source stronghold corridors should keep a 5-block cross-section");
                })
                .count();
        assertTrue(corridorCount >= 1, "Expected source stronghold recursion to include tube corridors");
    }

    @Test
    @DisplayName("Generated strongholds should use source-sized start stair pieces")
    void generatedStrongholdStartUsesSourceSizedStairs() {
        long seed = 24681357L;
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(seed, Dimension.OVERWORLD);
        StructureGenerator.StructureLocation location = new StructureGenerator().locate(seed, Dimension.OVERWORLD,
                StructureType.STRONGHOLD, 0, 0, generator);
        assertNotNull(location);

        StructureStart start = new StructurePlanner()
                .startsForChunk(seed, Dimension.OVERWORLD, location.chunkX(), location.chunkZ(), generator).stream()
                .filter(candidate -> candidate.type() == StructureType.STRONGHOLD)
                .filter(candidate -> candidate.chunkX() == location.chunkX() && candidate.chunkZ() == location.chunkZ())
                .findFirst()
                .orElseThrow();
        StructurePiece startPiece = start.pieces().stream()
                .filter(piece -> "START".equals(strongholdRoomName(piece)))
                .findFirst()
                .orElseThrow();
        StructureBoundingBox box = startPiece.bounds();
        StructureBoundingBox expected = expectedGeneratedStrongholdBox(seed, location, 0, 0, 4, 4);
        int rootMode = expectedGeneratedStrongholdRootMode(seed, location.chunkX(), location.chunkZ());

        assertEquals(5, width(box));
        assertEquals(11, height(box));
        assertEquals(5, depth(box));
        assertEquals(expected.minX(), box.minX());
        assertEquals(expected.maxX(), box.maxX());
        assertEquals(expected.minZ(), box.minZ());
        assertEquals(expected.maxZ(), box.maxZ());
        assertEquals(rootMode, strongholdCoordBaseMode(startPiece));
    }

    @Test
    @DisplayName("Generated strongholds should force the source first crossing child")
    void generatedStrongholdUsesSourceFirstCrossingChild() {
        long seed = 24681357L;
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(seed, Dimension.OVERWORLD);
        StructureGenerator.StructureLocation location = new StructureGenerator().locate(seed, Dimension.OVERWORLD,
                StructureType.STRONGHOLD, 0, 0, generator);
        assertNotNull(location);

        StructureStart start = new StructurePlanner()
                .startsForChunk(seed, Dimension.OVERWORLD, location.chunkX(), location.chunkZ(), generator).stream()
                .filter(candidate -> candidate.type() == StructureType.STRONGHOLD)
                .filter(candidate -> candidate.chunkX() == location.chunkX() && candidate.chunkZ() == location.chunkZ())
                .findFirst()
                .orElseThrow();
        StructurePiece crossing = start.pieces().stream()
                .filter(piece -> "CROSSING_HALL".equals(strongholdRoomName(piece)))
                .findFirst()
                .orElseThrow();
        StructureBoundingBox box = crossing.bounds();
        int rootMode = expectedGeneratedStrongholdRootMode(seed, location.chunkX(), location.chunkZ());
        StructureBoundingBox expected = expectedGeneratedStrongholdFirstCrossingBox(location.chunkX(),
                location.chunkZ(), rootMode);
        Random expectedRandom = expectedGeneratedStrongholdRandomAfterRoot(seed, location.chunkX(), location.chunkZ());
        String expectedDoor = expectedSourceStrongholdDoor(expectedRandom);
        boolean expectedLowerLeft = expectedRandom.nextBoolean();
        boolean expectedUpperLeft = expectedRandom.nextBoolean();
        boolean expectedLowerRight = expectedRandom.nextBoolean();
        boolean expectedUpperRight = expectedRandom.nextInt(3) > 0;

        assertEquals(expected.minX(), box.minX());
        assertEquals(expected.maxX(), box.maxX());
        assertEquals(expected.minZ(), box.minZ());
        assertEquals(expected.maxZ(), box.maxZ());
        assertEquals(9, height(box));
        assertEquals(rootMode, strongholdCoordBaseMode(crossing));
        assertEquals(expectedDoor, strongholdDoorName(crossing));
        assertEquals(expectedLowerLeft, strongholdBoolean(crossing, "crossingLowerLeft"));
        assertEquals(expectedUpperLeft, strongholdBoolean(crossing, "crossingUpperLeft"));
        assertEquals(expectedLowerRight, strongholdBoolean(crossing, "crossingLowerRight"));
        assertEquals(expectedUpperRight, strongholdBoolean(crossing, "crossingUpperRight"));
    }

    @Test
    @DisplayName("Generated first stronghold crossing should not consume weighted crossing quota")
    void generatedStrongholdFirstCrossingDoesNotConsumeWeightedCrossingQuota() throws Exception {
        StructureStart start = generatedStrongholdStart(new StructurePlanner(), 24681357L, 0, 0);
        StructurePiece crossing = start.pieces().stream()
                .filter(piece -> "CROSSING_HALL".equals(strongholdRoomName(piece)))
                .findFirst()
                .orElseThrow();

        assertFalse(strongholdCountsForWeight(crossing),
                "Source forced ComponentStrongholdCrossing bypasses weighted selection accounting");
    }

    @Test
    @DisplayName("Generated recursive stronghold room crossings should consume weighted quota")
    void generatedStrongholdRecursiveRoomCrossingsConsumeWeightedQuota() throws Exception {
        StructurePiece roomCrossing = findGeneratedStrongholdRoomAcrossSeeds("CROSSING", 32768);

        assertTrue(strongholdCountsForWeight(roomCrossing),
                "Room crossings after the forced first crossing should come from the source weighted table");
    }

    @Test
    @DisplayName("Generated recursive stronghold chest corridors should consume weighted quota")
    void generatedStrongholdRecursiveChestCorridorsConsumeWeightedQuota() throws Exception {
        StructurePiece chestCorridor = findGeneratedStrongholdRoomAcrossSeeds("CHEST_CORRIDOR", 32768);

        assertTrue(strongholdCountsForWeight(chestCorridor),
                "Chest corridors should no longer be injected as quota-neutral proxy rooms");
    }

    @Test
    @DisplayName("Generated recursive stronghold prison rooms should consume weighted quota")
    void generatedStrongholdRecursivePrisonsConsumeWeightedQuota() throws Exception {
        StructurePiece prison = findGeneratedStrongholdRoomAcrossSeeds("PRISON", 32768);

        assertTrue(strongholdCountsForWeight(prison),
                "Prisons should no longer be injected as quota-neutral proxy rooms");
    }

    @Test
    @DisplayName("Generated first stronghold crossing should use source bounds for every root orientation")
    void generatedStrongholdFirstCrossingUsesSourceBoundsForEveryRootOrientation() throws Exception {
        boolean[] covered = new boolean[4];
        StructurePlanner planner = new StructurePlanner();

        for (long seed = 0L; seed < 8192L && !(covered[0] && covered[1] && covered[2] && covered[3]); seed++) {
            StructureStart start = generatedStrongholdStart(planner, seed, 0, 0);
            StructurePiece startPiece = start.pieces().stream()
                    .filter(piece -> "START".equals(strongholdRoomName(piece)))
                    .findFirst()
                    .orElseThrow();
            int rootMode = strongholdCoordBaseMode(startPiece);
            if (covered[rootMode]) {
                continue;
            }
            StructurePiece crossing = start.pieces().stream()
                    .filter(piece -> "CROSSING_HALL".equals(strongholdRoomName(piece)))
                    .findFirst()
                    .orElseThrow();
            StructureBoundingBox box = crossing.bounds();
            ExpectedStrongholdPiece root = new ExpectedStrongholdPiece(startPiece.bounds(), rootMode);
            StructureBoundingBox expected = expectedStrongholdPiece(expectedStrongholdNormalAccess(root, 1, 1),
                    -4, -3, 0, 10, 9, 11).box();

            assertEquals(expected.minX(), box.minX());
            assertEquals(expected.maxX(), box.maxX());
            assertEquals(expected.minZ(), box.minZ());
            assertEquals(expected.maxZ(), box.maxZ());
            assertEquals(rootMode, strongholdCoordBaseMode(crossing));
            covered[rootMode] = true;
        }

        for (int mode = 0; mode < covered.length; mode++) {
            assertTrue(covered[mode], "Expected test seeds to cover stronghold root mode " + mode);
        }
    }

    @Test
    @DisplayName("Generated stronghold starts should retry source recursion until a portal room exists")
    void generatedStrongholdStartsRetrySourceRecursionUntilPortalRoomExists() throws Exception {
        StructurePlanner planner = new StructurePlanner();

        for (long seed = 0L; seed < 128L; seed++) {
            StructureStart start = generatedStrongholdStart(planner, seed, 0, 0);

            assertTrue(start.pieces().stream()
                            .anyMatch(piece -> "PORTAL".equals(strongholdRoomName(piece))),
                    "Source stronghold generation should retry until a portal room is present for seed " + seed);
        }
    }

    @Test
    @DisplayName("Generated strongholds should use source available-height shifting")
    void generatedStrongholdUsesSourceAvailableHeightShift() {
        long seed = 24681357L;
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(seed, Dimension.OVERWORLD);
        StructureGenerator.StructureLocation location = new StructureGenerator().locate(seed, Dimension.OVERWORLD,
                StructureType.STRONGHOLD, 0, 0, generator);
        assertNotNull(location);

        StructureStart start = new StructurePlanner()
                .startsForChunk(seed, Dimension.OVERWORLD, location.chunkX(), location.chunkZ(), generator).stream()
                .filter(candidate -> candidate.type() == StructureType.STRONGHOLD)
                .filter(candidate -> candidate.chunkX() == location.chunkX() && candidate.chunkZ() == location.chunkZ())
                .findFirst()
                .orElseThrow();
        StructureBoundingBox bounds = start.bounds();

        assertTrue(height(bounds) >= 13, "Expanded source-style strongholds may exceed the old proxy height");
        assertTrue(bounds.minY() >= 0,
                "markAvailableHeight(..., 10) should keep generated stronghold pieces above the world floor");
        assertTrue(bounds.maxY() <= 52, "markAvailableHeight(..., 10) should keep the top below sea level - 10");
    }

    @Test
    @DisplayName("Generated strongholds should use source-sized straight connector pieces")
    void generatedStrongholdStraightUsesSourceSizedBounds() throws Exception {
        StructurePiece straight = findGeneratedStrongholdRoomAcrossSeeds("STRAIGHT", 128);
        StructureBoundingBox box = straight.bounds();

        assertTrue((width(box) == 5 && depth(box) == 7)
                        || (width(box) == 7 && depth(box) == 5),
                "Source straight connectors keep a 5x7 footprint in either orientation");
        assertEquals(5, height(box));
        assertTrue(strongholdCoordBaseMode(straight) >= 0);
        assertTrue(strongholdCountsForWeight(straight));
    }

    @Test
    @DisplayName("Generated stronghold side branches should expand into weighted rooms")
    void generatedStrongholdSideBranchesExpandIntoWeightedRooms() throws Exception {
        StructurePlanner planner = new StructurePlanner();

        for (long seed = 0L; seed < 4096L; seed++) {
            StructureStart start = generatedStrongholdStart(planner, seed, 0, 0);
            boolean expanded = start.pieces().stream()
                    .map(StructureGeneratorTest::strongholdRoomName)
                    .anyMatch(room -> room.equals("LEFT_TURN")
                            || room.equals("RIGHT_TURN")
                            || room.equals("STAIRS_STRAIGHT"));
            if (expanded) {
                assertTrue(start.pieces().size() > 12,
                        "Source recursive branches should create explorable pieces beyond the root crossing");
                return;
            }
        }

        fail("Expected at least one fixture seed to expand stronghold side branches into weighted rooms");
    }

    @Test
    @DisplayName("Generated stronghold side branches should roll source stairwell pieces")
    void generatedStrongholdSideBranchesCanRollStairwellPieces() throws Exception {
        StructurePlanner planner = new StructurePlanner();

        for (long seed = 0L; seed < 8192L; seed++) {
            StructureStart start = generatedStrongholdStart(planner, seed, 0, 0);
            StructurePiece stairs = start.pieces().stream()
                    .filter(piece -> "STAIRS".equals(strongholdRoomName(piece)))
                    .findFirst()
                    .orElse(null);
            if (stairs == null) {
                continue;
            }

            StructureBoundingBox box = stairs.bounds();
            assertEquals(5, width(box));
            assertEquals(11, height(box));
            assertEquals(5, depth(box));
            assertNotNull(strongholdDoorName(stairs));
            return;
        }

        fail("Expected at least one fixture seed to roll a weighted stronghold stairwell");
    }

    @Test
    @DisplayName("Generated stronghold side branches should roll source crossing halls")
    void generatedStrongholdSideBranchesCanRollCrossingHalls() throws Exception {
        StructurePlanner planner = new StructurePlanner();

        for (long seed = 0L; seed < 8192L; seed++) {
            StructureStart start = generatedStrongholdStart(planner, seed, 0, 0);
            long crossingHallCount = start.pieces().stream()
                    .filter(piece -> "CROSSING_HALL".equals(strongholdRoomName(piece)))
                    .count();
            if (crossingHallCount < 2) {
                continue;
            }

            StructurePiece crossing = start.pieces().stream()
                    .filter(piece -> "CROSSING_HALL".equals(strongholdRoomName(piece)))
                    .skip(1)
                    .findFirst()
                    .orElseThrow();
            StructureBoundingBox box = crossing.bounds();
            assertTrue(width(box) == 10 || width(box) == 11);
            assertEquals(9, height(box));
            assertTrue(depth(box) == 10 || depth(box) == 11);
            assertEquals(21, width(box) + depth(box));
            assertTrue(strongholdCoordBaseMode(crossing) >= 0);
            assertNotNull(strongholdDoorName(crossing));
            return;
        }

        fail("Expected at least one fixture seed to roll a weighted stronghold crossing hall");
    }

    @Test
    @DisplayName("Generated stronghold branch failures should create source fallback corridors")
    void generatedStrongholdBranchFailuresCreateSourceFallbackCorridors() throws Exception {
        java.util.ArrayList<StructurePiece> pieces = new java.util.ArrayList<>();
        pieces.add(strongholdPiece("CORRIDOR", 1, 39, 3, 5, 43, 7, 0));

        StructurePiece fallback = fallbackStrongholdCorridor(pieces, 2, 40, 0, 0);

        assertNotNull(fallback);
        assertEquals("CORRIDOR", strongholdRoomName(fallback));
        assertEquals(0, strongholdCoordBaseMode(fallback));
        StructureBoundingBox box = fallback.bounds();
        assertEquals(1, box.minX());
        assertEquals(5, box.maxX());
        assertEquals(39, box.minY());
        assertEquals(43, box.maxY());
        assertEquals(0, box.minZ());
        assertEquals(2, box.maxZ());
        assertEquals(5, width(box));
        assertEquals(5, height(box));
        assertEquals(3, depth(box));
    }

    @Test
    @DisplayName("Generated stronghold libraries should obey the source depth gate")
    void generatedStrongholdLibrariesObeySourceDepthGate() throws Exception {
        java.util.ArrayList<StructurePiece> pieces = new java.util.ArrayList<>();

        assertNull(generatedStrongholdComponent("LIBRARY", pieces, 2, 40, 0, 0, 4),
                "Source stronghold libraries are not eligible until depth five");

        StructurePiece library = generatedStrongholdComponent("LIBRARY", pieces, 2, 40, 0, 0, 5);

        assertNotNull(library);
        assertEquals("LIBRARY", strongholdRoomName(library));
        assertEquals(0, strongholdCoordBaseMode(library));
        assertEquals(14, width(library.bounds()));
        assertEquals(15, depth(library.bounds()));
    }

    @Test
    @DisplayName("Generated stronghold library placement should fall back from source large to small box")
    void generatedStrongholdLibraryFallsBackToSourceSmallBox() throws Exception {
        java.util.ArrayList<StructurePiece> pieces = new java.util.ArrayList<>();
        pieces.add(strongholdPiece("CORRIDOR", 4, 45, 0, 17, 49, 14, 0));
        CountingRandom random = new CountingRandom();

        StructurePiece library = generatedStrongholdComponent("LIBRARY", pieces, random, 8, 40, 0, 0, 5);

        assertNotNull(library);
        assertEquals("LIBRARY", strongholdRoomName(library));
        assertEquals(6, height(library.bounds()),
                "Source retries a six-block-tall library when the large box collides");
        assertEquals(1, random.nextIntCalls(),
                "Valid small fallback should consume only the source door draw");
        assertEquals(0, random.nextBooleanCalls(),
                "Source library height is chosen by box validity, not a random boolean");
    }

    @Test
    @DisplayName("Generated stronghold portal rooms should obey the source depth gate")
    void generatedStrongholdPortalRoomsObeySourceDepthGate() throws Exception {
        java.util.ArrayList<StructurePiece> pieces = new java.util.ArrayList<>();

        assertNull(generatedStrongholdComponent("PORTAL", pieces, 2, 40, 0, 0, 5),
                "Source stronghold portal rooms are not eligible until depth six");

        StructurePiece portal = generatedStrongholdComponent("PORTAL", pieces, 2, 40, 0, 0, 6);

        assertNotNull(portal);
        assertEquals("PORTAL", strongholdRoomName(portal));
        assertEquals(0, strongholdCoordBaseMode(portal));
        assertEquals(11, width(portal.bounds()));
        assertEquals(8, height(portal.bounds()));
        assertEquals(16, depth(portal.bounds()));
    }

    @Test
    @DisplayName("Generated stronghold side branches should roll source portal rooms")
    void generatedStrongholdSideBranchesCanRollPortalRooms() throws Exception {
        StructurePiece portal = findGeneratedStrongholdRoomAcrossSeeds("PORTAL", 128);

        assertEquals(8, height(portal.bounds()));
        assertTrue((width(portal.bounds()) == 11 && depth(portal.bounds()) == 16)
                        || (width(portal.bounds()) == 16 && depth(portal.bounds()) == 11),
                "Weighted portal rooms should keep the source 11x16 footprint in any orientation");
        assertTrue(strongholdCoordBaseMode(portal) >= 0);
        assertTrue(strongholdCountsForWeight(portal),
                "Portal rooms should come from the source weighted table after restart removes the proxy portal");
    }

    @Test
    @DisplayName("Generated stronghold weighted expansion should stop after capped rooms are exhausted")
    void generatedStrongholdWeightedExpansionStopsAfterLimitedRoomsExhausted() throws Exception {
        java.util.ArrayList<StructurePiece> pieces = new java.util.ArrayList<>();

        assertTrue(strongholdWeightedTotal(pieces) > 0,
                "Fresh stronghold weights should allow weighted expansion");

        addStrongholdRoomCopies(pieces, "PRISON", 5);
        addStrongholdRoomCopies(pieces, "CROSSING", 6);
        addStrongholdRoomCopies(pieces, "STAIRS_STRAIGHT", 5);
        addStrongholdRoomCopies(pieces, "STAIRS", 5);
        addStrongholdRoomCopies(pieces, "CROSSING_HALL", 4);
        addStrongholdRoomCopies(pieces, "CHEST_CORRIDOR", 4);
        addStrongholdRoomCopies(pieces, "LIBRARY", 2);
        addStrongholdRoomCopies(pieces, "PORTAL", 1);

        assertEquals(-1, strongholdWeightedTotal(pieces),
                "Release stronghold generation stops once only uncapped filler pieces remain");
    }

    @Test
    @DisplayName("Generated stronghold weighted selection should reject immediate straight repeats")
    void generatedStrongholdWeightedSelectionRejectsImmediateStraightRepeats() throws Exception {
        StructurePiece repeatedStraight = chooseStrongholdWeightedComponentAfterPrevious("STRAIGHT",
                new FixedNextIntRandom(0), 2, 40, 0, 0, 1);

        assertNull(repeatedStraight,
                "Source weighted selection rejects the previously selected weight, including STRAIGHT");
    }

    @Test
    @DisplayName("Generated stronghold invalid weighted placement should not consume constructor RNG")
    void generatedStrongholdInvalidWeightedPlacementDoesNotConsumeConstructorRng() throws Exception {
        java.util.ArrayList<StructurePiece> pieces = new java.util.ArrayList<>();
        pieces.add(strongholdPiece("CORRIDOR", 1, 39, 0, 5, 43, 6, 0));
        CountingRandom random = new CountingRandom();

        StructurePiece straight = generatedStrongholdComponent("STRAIGHT", pieces, random, 2, 40, 0, 0, 1);

        assertNull(straight);
        assertEquals(0, random.nextIntCalls(),
                "Source findValidPlacement returns before door/side-opening constructor RNG on collisions");
        assertEquals(0, random.nextBooleanCalls());
    }

    @Test
    @DisplayName("Generated stronghold weighted selection should continue after invalid placement")
    void generatedStrongholdWeightedSelectionContinuesAfterInvalidPlacement() throws Exception {
        java.util.ArrayList<StructurePiece> pieces = new java.util.ArrayList<>();
        pieces.add(strongholdPiece("CORRIDOR", 0, 44, 0, 0, 44, 0, 0));

        StructurePiece piece = chooseStrongholdWeightedComponent(pieces, new FixedNextIntRandom(85),
                2, 40, 0, 0, 6);

        assertNotNull(piece);
        assertEquals("STAIRS_STRAIGHT", strongholdRoomName(piece),
                "Source keeps scanning later weights after a selected component's placement fails");
        assertEquals(5, width(piece.bounds()));
        assertEquals(11, height(piece.bounds()));
        assertEquals(8, depth(piece.bounds()));
    }

    @Test
    @DisplayName("Generated stronghold branch recursion should use the source depth limit")
    void generatedStrongholdBranchRecursionUsesSourceDepthLimit() throws Exception {
        assertEquals(1, generatedStrongholdChildCountFromParentDepth(49),
                "Source getNextValidComponent allows a child whose component type reaches 50");
        assertEquals(0, generatedStrongholdChildCountFromParentDepth(50),
                "Source getNextValidComponent rejects children only after component type 50");
    }

    @Test
    @DisplayName("Stronghold placement should use Release 1.0 generation-layer biome search")
    void strongholdPlacementUsesReleaseOneBiomeReservoirSearch() {
        long seed = 38L;
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(seed, Dimension.OVERWORLD);
        StructurePlanner planner = new StructurePlanner();

        StructureStart nearSpawn = strongholdStart(planner, seed, -5, -44, generator);
        assertEquals(-5, nearSpawn.chunkX());
        assertEquals(-44, nearSpawn.chunkZ());

        StructureStart eastern = strongholdStart(planner, seed, 64, 19, generator);
        assertEquals(64, eastern.chunkX());
        assertEquals(19, eastern.chunkZ());

        StructureStart western = strongholdStart(planner, seed, -49, 36, generator);
        assertEquals(-49, western.chunkX());
        assertEquals(36, western.chunkZ());
        assertSame(BiomeType.SWAMPLAND, generator.getBiome((western.chunkX() << 4) + 8,
                (western.chunkZ() << 4) + 8));
        assertTrue(hasReleaseOneStrongholdBiomeInGenerationChunk(generator, western.chunkX(), western.chunkZ()));
    }

    @Test
    @DisplayName("Stronghold stones should use Release 1.0 variant metadata")
    void strongholdStonesUseReleaseOneVariantMetadata() {
        long seed = 24681357L;
        StructureGenerator structures = new StructureGenerator();
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(seed, Dimension.OVERWORLD);
        StructureGenerator.StructureLocation location = structures.locate(seed, Dimension.OVERWORLD,
                StructureType.STRONGHOLD, 0, 0, generator);
        assertNotNull(location);

        int mossy = 0;
        int cracked = 0;
        int stoneBrickEggs = 0;
        int plainStoneEggs = 0;
        World world = new World(seed);
        try {
            for (int cx = location.chunkX() - 5; cx <= location.chunkX() + 5; cx++) {
                for (int cz = location.chunkZ() - 5; cz <= location.chunkZ() + 5; cz++) {
                    Chunk chunk = new Chunk(cx, cz);
                    structures.generate(world, chunk, seed, cx, cz, Dimension.OVERWORLD, generator);
                    mossy += count(chunk, BlockType.STONE_BRICK, 1);
                    cracked += count(chunk, BlockType.STONE_BRICK, 2);
                    stoneBrickEggs += count(chunk, BlockType.INFESTED_STONE, 2);
                    plainStoneEggs += count(chunk, BlockType.INFESTED_STONE, 0);
                }
            }
        } finally {
            world.cleanup();
        }

        assertTrue(mossy > 0, "Stronghold shell should include mossy stone bricks");
        assertTrue(cracked > 0, "Stronghold shell should include cracked stone bricks");
        assertTrue(stoneBrickEggs > 0, "Stronghold shell should include stone-brick monster eggs");
        assertEquals(0, plainStoneEggs, "Stronghold monster eggs should use stone-brick metadata");
    }

    @Test
    @DisplayName("Stronghold portal room should use source chamber blocks")
    void strongholdPortalRoomUsesSourceChamberBlocks() throws Exception {
        StructurePiece portalPiece = strongholdPiece("PORTAL", 0, 40, 0, 15, 47, 10, 3);
        Chunk chunk = new Chunk(0, 0);

        portalPiece.place(null, chunk, 24681357L, 0, 0);

        assertEquals(12, count(chunk, BlockType.END_PORTAL_FRAME));
        assertEquals(15, count(chunk, BlockType.FLOWING_LAVA));
        assertEquals(0, count(chunk, BlockType.LAVA),
                "Source portal rooms use moving lava, not still lava, during generation");
        assertEquals(8, count(chunk, BlockType.STONE_BRICK_STAIRS),
                "The silverfish spawner replaces the middle top stair after the stair run is placed");
        assertEquals(39, count(chunk, BlockType.IRON_BARS));
        assertSame(BlockType.MOB_SPAWNER, worldBlock(chunk, 0, 0, 6, 43, 5));

        assertSame(BlockType.IRON_BARS, worldBlock(chunk, 0, 0, 0, 41, 4));
        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 0, 41, 5));
        assertSame(BlockType.FLOWING_LAVA, worldBlock(chunk, 0, 0, 9, 41, 4));
        assertSame(BlockType.STONE_BRICK_STAIRS, worldBlock(chunk, 0, 0, 4, 41, 4));
        assertEquals(0, worldMetadata(chunk, 0, 0, 4, 41, 4));

        assertEquals(3, worldMetadata(chunk, 0, 0, 8, 43, 4) & 3);
        assertEquals(1, worldMetadata(chunk, 0, 0, 12, 43, 4) & 3);
        assertEquals(0, worldMetadata(chunk, 0, 0, 9, 43, 3) & 3);
        assertEquals(2, worldMetadata(chunk, 0, 0, 9, 43, 7) & 3);
    }

    @Test
    @DisplayName("Stronghold pieces should abort when liquid touches the source envelope")
    void strongholdPiecesAbortWhenLiquidTouchesSourceEnvelope() throws Exception {
        StructurePiece straight = strongholdPiece("STRAIGHT", 0, 40, 0, 4, 44, 6, 0);
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(2, 39, 2, BlockType.FLOWING_WATER);
        chunk.setBlock(2, 41, 2, BlockType.STONE);

        straight.place(null, chunk, 24681357L, 0, 0);

        assertSame(BlockType.FLOWING_WATER, chunk.getBlock(2, 39, 2));
        assertSame(BlockType.STONE, chunk.getBlock(2, 41, 2),
                "Source stronghold rooms return false before carving when their expanded box touches liquid");
        assertEquals(0, count(chunk, BlockType.STONE_BRICK));
    }

    @Test
    @DisplayName("Stronghold portal rooms should ignore liquid envelope aborts")
    void strongholdPortalRoomIgnoresLiquidEnvelopeAbort() throws Exception {
        StructurePiece portalPiece = strongholdPiece("PORTAL", 0, 40, 0, 15, 47, 10, 3);
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(2, 39, 2, BlockType.FLOWING_WATER);

        portalPiece.place(null, chunk, 24681357L, 0, 0);

        assertSame(BlockType.FLOWING_WATER, chunk.getBlock(2, 39, 2));
        assertEquals(12, count(chunk, BlockType.END_PORTAL_FRAME),
                "Source portal rooms do not run the liquid-envelope abort used by other stronghold pieces");
        assertSame(BlockType.MOB_SPAWNER, worldBlock(chunk, 0, 0, 6, 43, 5));
    }

    @Test
    @DisplayName("Stronghold chest corridors should use the source shelf and chest layout")
    void strongholdChestCorridorUsesSourceShelfLayout() throws Exception {
        StructurePiece chestCorridor = strongholdPiece("CHEST_CORRIDOR", 0, 40, 0, 4, 44, 6, -1);
        Chunk chunk = new Chunk(0, 0);

        chestCorridor.place(null, chunk, 1357911L, 0, 0);

        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 2, 41, 0));
        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 2, 43, 6));
        assertNotSame(BlockType.AIR, worldBlock(chunk, 0, 0, 0, 41, 3),
                "Source chest corridors open only their front and rear doorways");
        assertNotSame(BlockType.AIR, worldBlock(chunk, 0, 0, 4, 41, 3));

        assertSame(BlockType.STONE_BRICK, worldBlock(chunk, 0, 0, 3, 41, 3));
        assertSame(BlockType.STONE_SLAB, worldBlock(chunk, 0, 0, 3, 41, 1));
        assertEquals(5, worldMetadata(chunk, 0, 0, 3, 41, 1),
                "Block.stairSingle metadata 5 is the Release-era stone-brick slab");
        assertSame(BlockType.STONE_SLAB, worldBlock(chunk, 0, 0, 2, 41, 3));
        assertEquals(5, worldMetadata(chunk, 0, 0, 2, 41, 3));
        assertSame(BlockType.STONE_SLAB, worldBlock(chunk, 0, 0, 3, 42, 2));
        assertEquals(5, worldMetadata(chunk, 0, 0, 3, 42, 2));
        assertSame(BlockType.CHEST, worldBlock(chunk, 0, 0, 3, 42, 3));

        World exactWorld = new World(1357911L);
        try {
            Chunk exactChunk = exactWorld.getChunkNow(0, 0);
            ItemStack[] expected = expectedStrongholdChestCorridorInventory(1357911L, chestCorridor.bounds());

            chestCorridor.place(exactWorld, exactChunk, 1357911L, 0, 0);
            exactWorld.reconcileLoadedTileEntities();

            ChestTileEntity chest = (ChestTileEntity) exactWorld.getTileEntity(3, 42, 3);
            assertNotNull(chest);
            assertInventoryEquals(expected, chest.getInventory());
        } finally {
            exactWorld.cleanup();
        }
    }

    @Test
    @DisplayName("Stronghold randomized shells should iterate local X before Z")
    void strongholdRandomizedShellsUseSourceLocalXBeforeZ() throws Exception {
        StructurePiece chestCorridor = strongholdPiece("CHEST_CORRIDOR", 0, 40, 0, 4, 44, 6, -1);
        Chunk chunk = new Chunk(0, 0);

        chestCorridor.place(null, chunk, 0L, 0, 0, new StrongholdShellOrderRandom(8));

        assertSame(BlockType.STONE_BRICK, worldBlock(chunk, 0, 0, 1, 40, 0));
        assertEquals(2, worldMetadata(chunk, 0, 0, 1, 40, 0),
                "Source fillWithRandomizedBlocks iterates local y, then x, then z");
        assertSame(BlockType.STONE_BRICK, worldBlock(chunk, 0, 0, 2, 40, 1));
        assertEquals(0, worldMetadata(chunk, 0, 0, 2, 40, 1),
                "The old z-before-x order spent the eighth shell draw here instead");
    }

    @Test
    @DisplayName("Stronghold prisons should use the source cell layout")
    void strongholdPrisonUsesSourceCellLayout() throws Exception {
        StructurePiece prison = strongholdPiece("PRISON", 0, 40, 0, 8, 44, 10, -1);
        Chunk chunk = new Chunk(0, 0);

        prison.place(null, chunk, 246802L, 0, 0);

        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 2, 41, 0));
        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 2, 43, 10));
        assertNotSame(BlockType.AIR, worldBlock(chunk, 0, 0, 0, 41, 5));
        assertNotSame(BlockType.AIR, worldBlock(chunk, 0, 0, 8, 41, 5));

        assertNotSame(BlockType.AIR, worldBlock(chunk, 0, 0, 4, 41, 1));
        assertNotSame(BlockType.AIR, worldBlock(chunk, 0, 0, 4, 42, 3));
        assertNotSame(BlockType.AIR, worldBlock(chunk, 0, 0, 4, 42, 7));
        assertNotSame(BlockType.AIR, worldBlock(chunk, 0, 0, 4, 41, 9));

        assertSame(BlockType.IRON_BARS, worldBlock(chunk, 0, 0, 4, 41, 5));
        assertSame(BlockType.IRON_BARS, worldBlock(chunk, 0, 0, 7, 43, 5));
        assertSame(BlockType.IRON_BARS, worldBlock(chunk, 0, 0, 4, 43, 2));
        assertSame(BlockType.IRON_BARS, worldBlock(chunk, 0, 0, 4, 43, 8));

        assertSame(BlockType.IRON_DOOR, worldBlock(chunk, 0, 0, 4, 41, 2));
        assertEquals(3, worldMetadata(chunk, 0, 0, 4, 41, 2));
        assertSame(BlockType.IRON_DOOR, worldBlock(chunk, 0, 0, 4, 42, 2));
        assertEquals(11, worldMetadata(chunk, 0, 0, 4, 42, 2));
        assertSame(BlockType.IRON_DOOR, worldBlock(chunk, 0, 0, 4, 41, 8));
        assertEquals(3, worldMetadata(chunk, 0, 0, 4, 41, 8));
        assertSame(BlockType.IRON_DOOR, worldBlock(chunk, 0, 0, 4, 42, 8));
        assertEquals(11, worldMetadata(chunk, 0, 0, 4, 42, 8));
    }

    @Test
    @DisplayName("Stronghold libraries should use the source large-room layout")
    void strongholdLibraryUsesSourceLargeRoomLayout() throws Exception {
        StructurePiece library = strongholdPiece("LIBRARY", 0, 40, 0, 13, 50, 14, -1);
        Chunk chunk = new Chunk(0, 0);

        library.place(null, chunk, 11235813L, 0, 0);

        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 5, 41, 0));
        assertSame(BlockType.OAK_PLANKS, worldBlock(chunk, 0, 0, 1, 41, 1));
        assertSame(BlockType.TORCH, worldBlock(chunk, 0, 0, 2, 43, 1));
        assertSame(BlockType.BOOKSHELF, worldBlock(chunk, 0, 0, 1, 41, 2));
        assertSame(BlockType.BOOKSHELF, worldBlock(chunk, 0, 0, 4, 43, 3));
        assertSame(BlockType.OAK_PLANKS, worldBlock(chunk, 0, 0, 1, 45, 13));
        assertSame(BlockType.FENCE, worldBlock(chunk, 0, 0, 3, 46, 12));
        assertSame(BlockType.LADDER, worldBlock(chunk, 0, 0, 10, 47, 13));
        assertEquals(3, worldMetadata(chunk, 0, 0, 10, 47, 13));
        assertSame(BlockType.FENCE, worldBlock(chunk, 0, 0, 6, 49, 7));
        assertSame(BlockType.TORCH, worldBlock(chunk, 0, 0, 5, 48, 7));
        assertSame(BlockType.CHEST, worldBlock(chunk, 0, 0, 3, 43, 5));
        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 12, 49, 1));
        assertSame(BlockType.CHEST, worldBlock(chunk, 0, 0, 12, 48, 1));

        World exactWorld = new World(11235813L);
        try {
            Chunk exactChunk = exactWorld.getChunkNow(0, 0);
            ItemStack[][] expected = expectedStrongholdLibraryInventories(11235813L, library.bounds());

            clearStructureLiquidEnvelope(exactChunk, 0, 0, library.bounds());
            library.place(exactWorld, exactChunk, 11235813L, 0, 0);
            exactWorld.reconcileLoadedTileEntities();

            ChestTileEntity lowerChest = (ChestTileEntity) exactWorld.getTileEntity(3, 43, 5);
            ChestTileEntity upperChest = (ChestTileEntity) exactWorld.getTileEntity(12, 48, 1);
            assertNotNull(lowerChest);
            assertNotNull(upperChest);
            assertInventoryEquals(expected[0], lowerChest.getInventory());
            assertInventoryEquals(expected[1], upperChest.getInventory());
        } finally {
            exactWorld.cleanup();
        }
    }

    @Test
    @DisplayName("Stronghold room crossings should use the source balcony layout")
    void strongholdRoomCrossingUsesSourceBalconyLayout() throws Exception {
        StructurePiece crossing = strongholdPiece("CROSSING", 0, 40, 0, 10, 46, 10, -1);
        Chunk chunk = new Chunk(0, 0);

        crossing.place(null, chunk, 3L, 0, 0);

        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 5, 41, 0));
        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 5, 42, 10));
        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 0, 42, 5));
        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 10, 42, 5));

        assertSame(BlockType.COBBLESTONE, worldBlock(chunk, 0, 0, 1, 43, 5));
        assertSame(BlockType.COBBLESTONE, worldBlock(chunk, 0, 0, 5, 41, 4));
        assertSame(BlockType.COBBLESTONE, worldBlock(chunk, 0, 0, 6, 43, 6));
        assertSame(BlockType.OAK_PLANKS, worldBlock(chunk, 0, 0, 2, 43, 5));
        assertSame(BlockType.OAK_PLANKS, worldBlock(chunk, 0, 0, 5, 43, 8));
        assertSame(BlockType.TORCH, worldBlock(chunk, 0, 0, 5, 43, 5));
        assertSame(BlockType.LADDER, worldBlock(chunk, 0, 0, 9, 42, 3));
        assertEquals(4, worldMetadata(chunk, 0, 0, 9, 42, 3));
        assertSame(BlockType.CHEST, worldBlock(chunk, 0, 0, 3, 44, 8));

        World exactWorld = new World(3L);
        try {
            Chunk exactChunk = exactWorld.getChunkNow(0, 0);
            ItemStack[] expected = expectedStrongholdRoomCrossingInventory(3L, crossing.bounds());

            crossing.place(exactWorld, exactChunk, 3L, 0, 0);
            exactWorld.reconcileLoadedTileEntities();

            ChestTileEntity chest = (ChestTileEntity) exactWorld.getTileEntity(3, 44, 8);
            assertNotNull(chest);
            assertInventoryEquals(expected, chest.getInventory());
        } finally {
            exactWorld.cleanup();
        }
    }

    @Test
    @DisplayName("Stronghold corridors should use the source open-ended randomized tube layout")
    void strongholdCorridorUsesSourceOpenEndedTubeLayout() throws Exception {
        StructurePiece corridor = strongholdPiece("CORRIDOR", 0, 40, 0, 9, 44, 4, -1);
        Chunk chunk = new Chunk(0, 0);

        corridor.place(null, chunk, 424242L, 0, 0);

        assertStrongholdStoneShell(chunk, 0, 40, 0);
        assertStrongholdStoneShell(chunk, 5, 44, 2);
        assertStrongholdStoneShell(chunk, 5, 42, 0);
        assertStrongholdStoneShell(chunk, 5, 42, 4);
        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 5, 42, 2));
        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 0, 42, 2),
                "Source corridor tubes do not cap their lengthwise ends");
        assertTrue(count(chunk, BlockType.STONE_BRICK, 1) > 0,
                "Corridor shells should include mossy stone-brick variants");
        assertTrue(count(chunk, BlockType.STONE_BRICK, 2) > 0,
                "Corridor shells should include cracked stone-brick variants");
        assertEquals(0, count(chunk, BlockType.INFESTED_STONE),
                "Corridor shells use the randomized stronghold-stone palette without monster eggs");
    }

    @Test
    @DisplayName("Stronghold starts should use the source stairwell layout")
    void strongholdStartUsesSourceStairwellLayout() throws Exception {
        StructurePiece start = strongholdPiece("START", 0, 40, 0, 4, 50, 4, -1);
        Chunk chunk = new Chunk(0, 0);

        start.place(null, chunk, 123456L, 0, 0);

        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 2, 47, 0));
        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 2, 41, 4));
        assertSame(BlockType.STONE_BRICK, worldBlock(chunk, 0, 0, 2, 46, 1));
        assertSame(BlockType.STONE_BRICK, worldBlock(chunk, 0, 0, 1, 45, 1));
        assertSame(BlockType.STONE_SLAB, worldBlock(chunk, 0, 0, 1, 46, 1));
        assertEquals(0, worldMetadata(chunk, 0, 0, 1, 46, 1));
        assertSame(BlockType.STONE_BRICK, worldBlock(chunk, 0, 0, 3, 43, 3));
        assertSame(BlockType.STONE_SLAB, worldBlock(chunk, 0, 0, 3, 44, 3));
        assertEquals(0, worldMetadata(chunk, 0, 0, 3, 44, 3));
        assertSame(BlockType.STONE_BRICK, worldBlock(chunk, 0, 0, 1, 41, 2));
        assertSame(BlockType.STONE_SLAB, worldBlock(chunk, 0, 0, 1, 41, 3));
        assertEquals(0, worldMetadata(chunk, 0, 0, 1, 41, 3));
    }

    @Test
    @DisplayName("Generated stronghold stairwell pieces should use the source doorway layout")
    void strongholdWeightedStairsUseSourceDoorwayLayout() throws Exception {
        StructurePiece stairs = strongholdPieceWithDoor("STAIRS", 0, 40, 0, 4, 50, 4, -1, "WOOD_DOOR");
        Chunk chunk = new Chunk(0, 0);

        stairs.place(null, chunk, 123456L, 0, 0);

        assertSame(BlockType.WOODEN_DOOR, worldBlock(chunk, 0, 0, 2, 47, 0));
        assertEquals(0, worldMetadata(chunk, 0, 0, 2, 47, 0));
        assertSame(BlockType.WOODEN_DOOR, worldBlock(chunk, 0, 0, 2, 48, 0));
        assertEquals(8, worldMetadata(chunk, 0, 0, 2, 48, 0));
        assertSame(BlockType.STONE_BRICK, worldBlock(chunk, 0, 0, 1, 47, 0));
        assertSame(BlockType.STONE_BRICK, worldBlock(chunk, 0, 0, 3, 49, 0));
        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 2, 41, 4));
        assertSame(BlockType.STONE_SLAB, worldBlock(chunk, 0, 0, 1, 41, 3));
    }

    @Test
    @DisplayName("Stronghold straights should use the source connector layout")
    void strongholdStraightUsesSourceConnectorLayout() throws Exception {
        StructurePiece straight = strongholdStraightPiece(0, 40, 0, 4, 44, 6, -1, true, true);
        Chunk chunk = new Chunk(0, 0);

        straight.place(null, chunk, 86420L, 0, 0);

        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 2, 41, 0));
        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 2, 43, 6));
        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 0, 42, 3));
        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 4, 42, 3));
        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 2, 42, 3));
        assertNotSame(BlockType.AIR, worldBlock(chunk, 0, 0, 0, 40, 3));
        assertNotSame(BlockType.AIR, worldBlock(chunk, 0, 0, 4, 44, 3));
    }

    @Test
    @DisplayName("Stronghold pieces should consume the shared structure placement random")
    void strongholdPiecesUseSharedPlacementRandom() throws Exception {
        StructurePiece straight = strongholdStraightPiece(0, 40, 0, 4, 44, 6, -1, false, false);
        Chunk chunk = new Chunk(0, 0);

        straight.place(null, chunk, 86420L, 0, 0, new Random(2048L));

        assertSame(BlockType.STONE_BRICK, worldBlock(chunk, 0, 0, 0, 40, 0));
        assertEquals(0, worldMetadata(chunk, 0, 0, 0, 40, 0),
                "Source structure generation passes one placement Random through stronghold pieces");
    }

    @Test
    @DisplayName("Stronghold doors should use the source opening variants")
    void strongholdDoorsUseSourceOpeningVariants() throws Exception {
        StructurePiece woodDoor = strongholdPieceWithDoor("CHEST_CORRIDOR", 0, 40, 1, 4, 44, 7, -1,
                "WOOD_DOOR");
        StructurePiece grates = strongholdPieceWithDoor("CHEST_CORRIDOR", 0, 40, 1, 4, 44, 7, -1,
                "GRATES");
        StructurePiece ironDoor = strongholdPieceWithDoor("CHEST_CORRIDOR", 0, 40, 1, 4, 44, 7, -1,
                "IRON_DOOR");
        Chunk woodChunk = new Chunk(0, 0);
        Chunk gratesChunk = new Chunk(0, 0);
        Chunk ironChunk = new Chunk(0, 0);

        woodDoor.place(null, woodChunk, 11L, 0, 0);
        grates.place(null, gratesChunk, 11L, 0, 0);
        ironDoor.place(null, ironChunk, 11L, 0, 0);

        assertSame(BlockType.WOODEN_DOOR, worldBlock(woodChunk, 0, 0, 2, 41, 1));
        assertEquals(0, worldMetadata(woodChunk, 0, 0, 2, 41, 1));
        assertSame(BlockType.WOODEN_DOOR, worldBlock(woodChunk, 0, 0, 2, 42, 1));
        assertEquals(8, worldMetadata(woodChunk, 0, 0, 2, 42, 1));
        assertSame(BlockType.STONE_BRICK, worldBlock(woodChunk, 0, 0, 1, 41, 1));
        assertSame(BlockType.STONE_BRICK, worldBlock(woodChunk, 0, 0, 3, 43, 1));

        assertSame(BlockType.AIR, worldBlock(gratesChunk, 0, 0, 2, 41, 1));
        assertSame(BlockType.AIR, worldBlock(gratesChunk, 0, 0, 2, 42, 1));
        assertSame(BlockType.IRON_BARS, worldBlock(gratesChunk, 0, 0, 1, 41, 1));
        assertSame(BlockType.IRON_BARS, worldBlock(gratesChunk, 0, 0, 3, 43, 1));

        assertSame(BlockType.IRON_DOOR, worldBlock(ironChunk, 0, 0, 2, 41, 1));
        assertEquals(0, worldMetadata(ironChunk, 0, 0, 2, 41, 1));
        assertSame(BlockType.IRON_DOOR, worldBlock(ironChunk, 0, 0, 2, 42, 1));
        assertEquals(8, worldMetadata(ironChunk, 0, 0, 2, 42, 1));
        assertSame(BlockType.STONE_BUTTON, worldBlock(ironChunk, 0, 0, 3, 42, 2));
        assertSame(BlockType.STONE_BUTTON, worldBlock(ironChunk, 0, 0, 3, 42, 0));
    }

    @Test
    @DisplayName("Stronghold turns should use source side openings")
    void strongholdTurnsUseSourceSideOpenings() throws Exception {
        StructurePiece leftTurn = strongholdPiece("LEFT_TURN", 0, 40, 0, 4, 44, 4, 3);
        StructurePiece rightTurn = strongholdPiece("RIGHT_TURN", 0, 40, 0, 4, 44, 4, 3);
        Chunk leftChunk = new Chunk(0, 0);
        Chunk rightChunk = new Chunk(0, 0);

        leftTurn.place(null, leftChunk, 13579L, 0, 0);
        rightTurn.place(null, rightChunk, 13579L, 0, 0);

        assertSame(BlockType.AIR, worldBlock(leftChunk, 0, 0, 0, 42, 2));
        assertSame(BlockType.AIR, worldBlock(leftChunk, 0, 0, 2, 42, 0));
        assertNotSame(BlockType.AIR, worldBlock(leftChunk, 0, 0, 2, 42, 4));

        assertSame(BlockType.AIR, worldBlock(rightChunk, 0, 0, 0, 42, 2));
        assertSame(BlockType.AIR, worldBlock(rightChunk, 0, 0, 2, 42, 4));
        assertNotSame(BlockType.AIR, worldBlock(rightChunk, 0, 0, 2, 42, 0));
    }

    @Test
    @DisplayName("Stronghold stair straights should use the source descending stair run")
    void strongholdStairsStraightUsesSourceDescendingRun() throws Exception {
        StructurePiece stairs = strongholdPiece("STAIRS_STRAIGHT", 0, 40, 0, 4, 50, 7, -1);
        Chunk chunk = new Chunk(0, 0);

        stairs.place(null, chunk, 97531L, 0, 0);

        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 2, 47, 0));
        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 2, 41, 7));
        assertSame(BlockType.COBBLESTONE_STAIRS, worldBlock(chunk, 0, 0, 1, 46, 1));
        assertEquals(2, worldMetadata(chunk, 0, 0, 1, 46, 1));
        assertSame(BlockType.COBBLESTONE_STAIRS, worldBlock(chunk, 0, 0, 2, 43, 4));
        assertEquals(2, worldMetadata(chunk, 0, 0, 2, 43, 4));
        assertSame(BlockType.COBBLESTONE_STAIRS, worldBlock(chunk, 0, 0, 3, 41, 6));
        assertEquals(2, worldMetadata(chunk, 0, 0, 3, 41, 6));
        assertSame(BlockType.STONE_BRICK, worldBlock(chunk, 0, 0, 2, 45, 1));
        assertSame(BlockType.STONE_BRICK, worldBlock(chunk, 0, 0, 2, 42, 4));
        assertEquals(18, count(chunk, BlockType.COBBLESTONE_STAIRS));
    }

    @Test
    @DisplayName("Stronghold crossing halls should use the source multi-level layout")
    void strongholdCrossingHallUsesSourceMultiLevelLayout() throws Exception {
        StructurePiece crossing = strongholdCrossingHallPiece(0, 40, 0, 9, 48, 10, -1,
                true, true, true, true);
        Chunk chunk = new Chunk(0, 0);

        crossing.place(null, chunk, 24680L, 0, 0);

        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 5, 43, 0));
        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 6, 42, 10));
        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 0, 44, 2));
        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 0, 46, 8));
        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 9, 44, 2));
        assertSame(BlockType.AIR, worldBlock(chunk, 0, 0, 9, 46, 8));

        assertSame(BlockType.STONE_SLAB, worldBlock(chunk, 0, 0, 2, 43, 4));
        assertSame(BlockType.STONE_SLAB, worldBlock(chunk, 0, 0, 2, 44, 6));
        assertSame(BlockType.STONE_SLAB, worldBlock(chunk, 0, 0, 6, 41, 9));
        assertSame(BlockType.STONE_SLAB, worldBlock(chunk, 0, 0, 6, 42, 7));
        assertSame(BlockType.STONE_SLAB, worldBlock(chunk, 0, 0, 4, 45, 8));
        assertSame(BlockType.STONE_SLAB, worldBlock(chunk, 0, 0, 8, 45, 8));
        assertSame(BlockType.DOUBLE_STONE_SLAB, worldBlock(chunk, 0, 0, 6, 45, 8));
        assertSame(BlockType.TORCH, worldBlock(chunk, 0, 0, 6, 45, 6));
        assertNotSame(BlockType.AIR, worldBlock(chunk, 0, 0, 4, 42, 5));
    }

    @Test
    @DisplayName("Nether fortress locator should generate blaze spawner and wart pieces")
    void locateNetherFortressMatchesGeneratedPieces() {
        long seed = 1L;
        StructureGenerator structures = new StructureGenerator();
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(seed, Dimension.NETHER);
        StructureGenerator.StructureLocation location = structures.locate(seed, Dimension.NETHER,
                StructureType.NETHER_FORTRESS, 0, 0, generator);
        assertNotNull(location);

        StructureStart start = netherFortressStart(seed, location, generator);
        StructureBoundingBox blazePlatform = findFortressRoom(start, "BLAZE_PLATFORM").bounds();
        StructureBoundingBox wartRoom = findFortressRoom(start, "WART_ROOM").bounds();

        assertTrue(generatedRoomContains(structures, seed, generator, blazePlatform, BlockType.MOB_SPAWNER));
        assertTrue(generatedRoomContains(structures, seed, generator, wartRoom, BlockType.NETHER_WART));
    }

    @Test
    @DisplayName("Nether fortress starts should use source-sized crossing and bridge pieces")
    void netherFortressStartUsesSourceSizedPieces() {
        long seed = 97531L;
        StructurePlanner planner = new StructurePlanner();
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(seed, Dimension.NETHER);
        StructureGenerator.StructureLocation location = new StructureGenerator().locate(seed, Dimension.NETHER,
                StructureType.NETHER_FORTRESS, 0, 0, generator);
        assertNotNull(location);

        StructureStart start = planner.startsForChunk(seed, Dimension.NETHER, location.chunkX(), location.chunkZ(),
                generator).stream()
                .filter(candidate -> candidate.type() == StructureType.NETHER_FORTRESS)
                .filter(candidate -> candidate.chunkX() == location.chunkX() && candidate.chunkZ() == location.chunkZ())
                .findFirst()
                .orElseThrow();

        StructurePiece crossing = start.pieces().get(0);
        StructureBoundingBox crossingBox = crossing.bounds();
        assertEquals((location.chunkX() << 4) + 2, crossingBox.minX());
        assertEquals((location.chunkZ() << 4) + 2, crossingBox.minZ());
        assertEquals(19, width(crossingBox));
        assertEquals(10, height(crossingBox));
        assertEquals(19, depth(crossingBox));
        StructureBoundingBox startBounds = start.bounds();
        assertTrue(startBounds.minY() >= 48,
                "StructureNetherBridgeStart.setRandomHeight should keep the full fortress above the source floor");
        if (height(startBounds) <= 23) {
            assertTrue(startBounds.maxY() <= 70,
                    "Fortresses short enough for the 48..70 range should fit below the source ceiling");
        } else {
            assertEquals(48, startBounds.minY(),
                    "Fortresses taller than the 48..70 range should be anchored at the source minimum height");
        }

        java.util.List<String> initialRooms = start.pieces().subList(1, 4).stream()
                .map(StructureGeneratorTest::fortressRoomName)
                .toList();
        assertEquals(java.util.List.of("STAIRS", "BRIDGE", "STAIRS"), initialRooms,
                "Source weighted selection should choose the first three branches from the primary table");
        assertTrue(start.pieces().size() > 20, "Source fortress starts should drain a recursive component queue");
        assertTrue(start.pieces().stream().mapToInt(StructureGeneratorTest::fortressComponentType).max().orElse(0) <= 31,
                "Weighted fortress generation stops choosing new components after source depth 30 and caps with ends");

        long bridgeCount = start.pieces().stream()
                .map(StructurePiece::bounds)
                .filter(box -> (width(box) == 5 && depth(box) == 19) || (width(box) == 19 && depth(box) == 5))
                .count();
        assertTrue(bridgeCount >= 3, "Recursive source graph should include the initial bridge-size branch pieces");
        assertFalse(start.pieces().subList(1, 4).stream()
                .map(StructurePiece::bounds)
                .anyMatch(box -> box.minX() == crossingBox.minX() - 19 && box.maxX() == crossingBox.minX() - 1
                        && box.minZ() == crossingBox.minZ() + 7 && box.maxZ() == crossingBox.minZ() + 11),
                "Seed 97531 has source start mode 3, so the west branch should not be generated");
    }

    @Test
    @DisplayName("Nether fortress weighted selection should continue after invalid placement")
    void netherFortressWeightedSelectionContinuesAfterInvalidPlacement() throws Exception {
        java.util.ArrayList<StructurePiece> pieces = new java.util.ArrayList<>();
        pieces.add(fortressPiece("END", 10, 62, 10, 10, 62, 10, 0));

        StructurePiece piece = chooseFortressWeightedComponent(pieces, false, new FixedNextIntRandom(30),
                2, 64, 0, 0, 1);

        assertNotNull(piece);
        assertEquals("SMALL_CROSSING", fortressRoomName(piece),
                "Source keeps scanning later fortress weights after a selected component's placement fails");
        assertEquals(7, width(piece.bounds()));
        assertEquals(9, height(piece.bounds()));
        assertEquals(7, depth(piece.bounds()));
    }

    @Test
    @DisplayName("Nether fortress distance caps should keep the source parent component depth")
    void netherFortressDistanceCapUsesParentDepthForEndCaps() throws Exception {
        java.util.ArrayList<StructurePiece> pieces = new java.util.ArrayList<>();
        StructurePiece parent = fortressPiece("BRIDGE", 0, 64, 0, 4, 73, 18, 0, 7);
        pieces.add(parent);

        addNextFortressComponent(parent, pieces, new Random(1234L), 113, 64, 0, 0, false);

        assertEquals(2, pieces.size());
        StructurePiece end = pieces.get(1);
        assertEquals("END", fortressRoomName(end));
        assertEquals(7, fortressComponentType(end),
                "Source out-of-range fortress caps call ComponentNetherBridgeEnd with the parent component type");
    }

    @Test
    @DisplayName("Nether fortress support fills should extend through air and stop at terrain")
    void netherFortressSupportFillsStopAtSolidTerrain() {
        long seed = 97531L;
        StructureGenerator structures = new StructureGenerator();
        StructurePlanner planner = new StructurePlanner();
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(seed, Dimension.NETHER);
        StructureGenerator.StructureLocation location = structures.locate(seed, Dimension.NETHER,
                StructureType.NETHER_FORTRESS, 0, 0, generator);
        assertNotNull(location);

        StructureStart start = planner.startsForChunk(seed, Dimension.NETHER, location.chunkX(), location.chunkZ(),
                generator).stream()
                .filter(candidate -> candidate.type() == StructureType.NETHER_FORTRESS)
                .filter(candidate -> candidate.chunkX() == location.chunkX() && candidate.chunkZ() == location.chunkZ())
                .findFirst()
                .orElseThrow();
        StructureBoundingBox crossing = start.pieces().get(0).bounds();
        int supportX = crossing.minX() + 8;
        int supportZ = crossing.minZ() + 1;
        int chunkX = Math.floorDiv(supportX, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(supportZ, Chunk.DEPTH);

        Chunk emptyChunk = new Chunk(chunkX, chunkZ);
        emptyChunk.setBlock(Math.floorMod(supportX, Chunk.WIDTH), 2, Math.floorMod(supportZ, Chunk.DEPTH),
                BlockType.FLOWING_LAVA, 8);
        structures.generate(null, emptyChunk, seed, chunkX, chunkZ, Dimension.NETHER, generator);
        assertSame(BlockType.NETHER_BRICK, worldBlock(emptyChunk, chunkX, chunkZ,
                supportX, crossing.minY() - 1, supportZ));
        assertSame(BlockType.NETHER_BRICK, worldBlock(emptyChunk, chunkX, chunkZ, supportX, 2, supportZ),
                "Source support fill treats flowing fluids as replaceable liquid");

        Chunk terrainChunk = new Chunk(chunkX, chunkZ);
        terrainChunk.setBlock(Math.floorMod(supportX, Chunk.WIDTH), 10, Math.floorMod(supportZ, Chunk.DEPTH),
                BlockType.NETHERRACK);
        structures.generate(null, terrainChunk, seed, chunkX, chunkZ, Dimension.NETHER, generator);
        assertSame(BlockType.NETHER_BRICK, worldBlock(terrainChunk, chunkX, chunkZ, supportX, 11, supportZ));
        assertSame(BlockType.NETHERRACK, worldBlock(terrainChunk, chunkX, chunkZ, supportX, 10, supportZ),
                "Source support fill stops when it reaches non-air, non-liquid terrain");
        assertSame(BlockType.AIR, worldBlock(terrainChunk, chunkX, chunkZ, supportX, 9, supportZ));
    }

    @Test
    @DisplayName("Nether fortress blaze throne should use the source open room shape")
    void netherFortressBlazeThroneUsesSourceOpenShape() throws Exception {
        long seed = 97531L;
        StructurePiece thronePiece = fortressPiece("BLAZE_PLATFORM", 0, 40, 0, 6, 47, 8, -1);
        StructureBoundingBox throne = thronePiece.bounds();
        int chunkX = 0;
        int chunkZ = 0;
        Chunk chunk = new Chunk(chunkX, chunkZ);
        thronePiece.place(null, chunk, seed, chunkX, chunkZ);

        assertSame(BlockType.NETHER_BRICK, worldBlock(chunk, chunkX, chunkZ,
                throne.minX() + 3, throne.minY(), throne.minZ() + 4));
        assertSame(BlockType.AIR, worldBlock(chunk, chunkX, chunkZ,
                throne.minX() + 3, throne.minY() + 7, throne.minZ() + 4),
                "Source throne clears the upper interior instead of sealing it with a brick ceiling");
        assertSame(BlockType.NETHER_BRICK_FENCE, worldBlock(chunk, chunkX, chunkZ,
                throne.minX() + 3, throne.minY() + 8, throne.minZ() + 8));
        assertSame(BlockType.MOB_SPAWNER, worldBlock(chunk, chunkX, chunkZ,
                throne.minX() + 3, throne.minY() + 5, throne.minZ() + 5));
    }

    @Test
    @DisplayName("Nether fortress spawners should preserve the shared structure placement random")
    void netherFortressPiecesUseSharedPlacementRandom() throws Exception {
        StructurePiece thronePiece = fortressPiece("BLAZE_PLATFORM", 0, 40, 0, 6, 47, 8, -1);
        Random expectedRandom = new Random(2048L);
        Random placementRandom = new Random(2048L);
        World world = new World(97531L);
        try {
            Chunk chunk = world.getChunkNow(0, 0);

            thronePiece.place(world, chunk, 97531L, 0, 0, placementRandom);
            world.reconcileLoadedTileEntities();

            MonsterSpawnerTileEntity spawner = (MonsterSpawnerTileEntity) world.getTileEntity(3, 45, 5);
            assertNotNull(spawner);
            assertEquals(20, spawner.getDelay(),
                    "Source structure spawners keep the tile entity's default generated delay");
            assertEquals(expectedRandom.nextInt(), placementRandom.nextInt(),
                    "Source Nether fortress spawner placement does not consume the structure placement random");
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Nether fortress wart room should use source stairs and wart beds")
    void netherFortressWartRoomUsesSourceShape() throws Exception {
        StructurePiece wartPiece = fortressPiece("WART_ROOM", 0, 40, 0, 12, 53, 12, -1);
        StructureBoundingBox wartRoom = wartPiece.bounds();
        int chunkX = 0;
        int chunkZ = 0;
        Chunk chunk = new Chunk(chunkX, chunkZ);
        wartPiece.place(null, chunk, 97531L, chunkX, chunkZ);

        assertSame(BlockType.SOUL_SAND, worldBlock(chunk, chunkX, chunkZ,
                wartRoom.minX() + 3, wartRoom.minY() + 4, wartRoom.minZ() + 4));
        assertSame(BlockType.NETHER_WART, worldBlock(chunk, chunkX, chunkZ,
                wartRoom.minX() + 3, wartRoom.minY() + 5, wartRoom.minZ() + 4));
        assertSame(BlockType.NETHER_BRICK_STAIRS, worldBlock(chunk, chunkX, chunkZ,
                wartRoom.minX() + 6, wartRoom.minY() + 5, wartRoom.minZ() + 4));
        assertEquals(3, worldMetadata(chunk, chunkX, chunkZ,
                wartRoom.minX() + 6, wartRoom.minY() + 5, wartRoom.minZ() + 4));

        assertSame(BlockType.AIR, worldBlock(chunk, chunkX, chunkZ,
                wartRoom.minX() + 6, wartRoom.minY() + 13, wartRoom.minZ() + 12),
                "Source nether-stalk room cuts an opening through the upper rear fence run");
    }

    @Test
    @DisplayName("Nether fortress entrance lava well should apply source immediate flow")
    void netherFortressEntranceLavaWellAppliesSourceImmediateFlow() throws Exception {
        StructurePiece entrancePiece = fortressPiece("ENTRANCE", 0, 40, 0, 12, 53, 12, -1);
        StructureBoundingBox entrance = entrancePiece.bounds();
        Chunk chunk = new Chunk(0, 0);

        entrancePiece.place(null, chunk, 13579L, 0, 0);

        int lavaX = entrance.minX() + 6;
        int lavaZ = entrance.minZ() + 6;
        assertSame(BlockType.LAVA, worldBlock(chunk, 0, 0, lavaX, entrance.minY() + 5, lavaZ),
                "The immediately ticked source cap settles to still lava");
        assertEquals(0, worldMetadata(chunk, 0, 0, lavaX, entrance.minY() + 5, lavaZ));
        for (int y = entrance.minY() + 1; y <= entrance.minY() + 4; y++) {
            assertSame(BlockType.FLOWING_LAVA, worldBlock(chunk, 0, 0, lavaX, y, lavaZ),
                    "The generated well should contain falling lava through the open shaft");
            assertEquals(8, worldMetadata(chunk, 0, 0, lavaX, y, lavaZ));
        }
        assertSame(BlockType.NETHER_BRICK, worldBlock(chunk, 0, 0, lavaX, entrance.minY(), lavaZ));
    }

    @Test
    @DisplayName("Nether fortress oriented rooms should use source local offsets and stair metadata")
    void netherFortressOrientedRoomsUseSourceOffsetsAndMetadata() throws Exception {
        StructurePiece wartRoom = fortressPiece("WART_ROOM", 0, 40, 0, 12, 53, 12, 1);
        Chunk chunk = new Chunk(0, 0);

        wartRoom.place(null, chunk, 24680L, 0, 0);

        assertSame(BlockType.NETHER_BRICK_STAIRS, worldBlock(chunk, 0, 0, 8, 45, 5));
        assertEquals(1, worldMetadata(chunk, 0, 0, 8, 45, 5),
                "Mode 1 should map local stair metadata 3 through StructureComponent.getMetadataWithOffset");
        assertNotEquals(BlockType.NETHER_BRICK_STAIRS, worldBlock(chunk, 0, 0, 5, 45, 4),
                "Mode 1 should rotate local x/z instead of placing blocks with raw min-offset coordinates");
        assertSame(BlockType.NETHER_BRICK_STAIRS, worldBlock(chunk, 0, 0, 10, 45, 8));
        assertEquals(2, worldMetadata(chunk, 0, 0, 10, 45, 8),
                "Mode 1 should map local stair metadata 0 through StructureComponent.getMetadataWithOffset");
    }

    @Test
    @DisplayName("Null-world structure generation should keep Release 1.0 fortress special blocks")
    void nullWorldStructureGenerationKeepsReleaseOneFortressSpecialBlocks() {
        long seed = 1L;
        StructureGenerator structures = new StructureGenerator();
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(seed, Dimension.NETHER);
        StructureGenerator.StructureLocation location = structures.locate(seed, Dimension.NETHER,
                StructureType.NETHER_FORTRESS, 0, 0, generator);
        assertNotNull(location);
        StructureStart start = netherFortressStart(seed, location, generator);
        StructureBoundingBox bounds = start.bounds();

        boolean hasSpawner = false;
        boolean hasWart = false;
        boolean hasChest = false;
        boolean hasBrick = false;
        for (int cx = Math.floorDiv(bounds.minX(), Chunk.WIDTH); cx <= Math.floorDiv(bounds.maxX(), Chunk.WIDTH);
                cx++) {
            for (int cz = Math.floorDiv(bounds.minZ(), Chunk.DEPTH); cz <= Math.floorDiv(bounds.maxZ(), Chunk.DEPTH);
                    cz++) {
                int chunkX = cx;
                int chunkZ = cz;
                Chunk chunk = new Chunk(chunkX, chunkZ);
                assertDoesNotThrow(() -> structures.generate(null, chunk, seed, chunkX, chunkZ,
                        Dimension.NETHER, generator));
                hasSpawner |= contains(chunk, BlockType.MOB_SPAWNER);
                hasWart |= contains(chunk, BlockType.NETHER_WART);
                hasChest |= contains(chunk, BlockType.CHEST);
                hasBrick |= contains(chunk, BlockType.NETHER_BRICK);
            }
        }

        assertTrue(hasBrick);
        assertTrue(hasSpawner, "Blaze spawner blocks should still be visible without a live World");
        assertTrue(hasWart, "Fortress wart rooms should still be visible without a live World");
        assertFalse(hasChest, "Java Release 1.0 fortresses should not generate loot chests");
    }

    @Test
    @DisplayName("Mineshaft starts should use Release 1.0 placement gate and corridor shape")
    void mineshaftPlacementUsesReleaseOneSpawnRule() {
        long seed = 4L;
        StructureGenerator structures = new StructureGenerator();
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(seed, Dimension.OVERWORLD);
        Chunk origin = new Chunk(-16, 2);
        origin.setBlock(2, 42, 2, BlockType.STONE);
        origin.setBlock(9, 42, 9, BlockType.STONE);
        structures.generate(null, origin, seed, -16, 2, Dimension.OVERWORLD, generator);

        assertSame(BlockType.DIRT, origin.getBlock(2, 42, 2),
                "Source mineshaft starts replace non-air room-floor terrain with dirt");
        assertSame(BlockType.DIRT, origin.getBlock(9, 42, 9));
        assertSame(BlockType.AIR, origin.getBlock(5, 43, 5));
        assertSame(BlockType.AIR, origin.getBlock(3, 45, 10),
                "Source corridors do not backfill unsupported floor cells when a recursive branch spills in");

        StructureStart start = mineshaftStart(seed, -16, 2, generator);
        assertTrue(start.pieces().size() > 2,
                "Release-era mineshafts should grow a bounded recursive branch graph, not one freestanding corridor");
        assertTrue(start.pieces().stream()
                .anyMatch(piece -> piece.getClass().getSimpleName().equals("MineshaftCrossPiece")
                        || piece.getClass().getSimpleName().equals("MineshaftStairsPiece")),
                "The source component table should be able to add crossings or stairs beyond corridors");
    }

    @Test
    @DisplayName("Mineshaft corridor should use source section supports")
    void mineshaftCorridorUsesSourceSectionSupports() throws Exception {
        StructurePiece corridor = mineshaftPiece(0, 40, 0, 2, 42, 9, false, 2, false, false);
        Chunk chunk = new Chunk(0, 0);

        corridor.place(null, chunk, 424242L, 0, 0);

        assertSame(BlockType.AIR, chunk.getBlock(1, 40, 0));
        assertSame(BlockType.AIR, chunk.getBlock(1, 41, 0));
        assertSame(BlockType.AIR, chunk.getBlock(1, 39, 0),
                "Source corridors do not add a plank floor under unsupported cells");
        assertSame(BlockType.OAK_PLANKS, chunk.getBlock(0, 40, 2),
                "Release mineshaft supports use plank foot blocks with fence posts above");
        assertSame(BlockType.FENCE, chunk.getBlock(0, 41, 2));
        assertSame(BlockType.FENCE, chunk.getBlock(2, 41, 7));
        assertSame(BlockType.OAK_PLANKS, chunk.getBlock(2, 40, 7));
        assertSame(BlockType.OAK_PLANKS, chunk.getBlock(0, 42, 2));
        assertSame(BlockType.OAK_PLANKS, chunk.getBlock(2, 42, 7));
    }

    @Test
    @DisplayName("Mineshaft special support arch should span side roof planks")
    void mineshaftCorridorSupportArchUsesSourceSideRoofSpan() throws Exception {
        StructurePiece corridor = mineshaftPiece(0, 40, 0, 2, 42, 4, false, 1, false, false);
        Chunk chunk = new Chunk(0, 0);

        corridor.place(null, chunk, 0L, 0, 0, new MineshaftSupportArchRandom());

        assertSame(BlockType.OAK_PLANKS, chunk.getBlock(0, 42, 1));
        assertSame(BlockType.OAK_PLANKS, chunk.getBlock(0, 42, 2));
        assertSame(BlockType.OAK_PLANKS, chunk.getBlock(0, 42, 3));
        assertSame(BlockType.OAK_PLANKS, chunk.getBlock(2, 42, 1));
        assertSame(BlockType.OAK_PLANKS, chunk.getBlock(2, 42, 2));
        assertSame(BlockType.OAK_PLANKS, chunk.getBlock(2, 42, 3));
        assertSame(BlockType.AIR, chunk.getBlock(1, 42, 2),
                "Source special support arches leave the center ceiling lane open");
    }

    @Test
    @DisplayName("Mineshaft corridor torches should attach to generated side supports")
    void mineshaftCorridorTorchesAttachToGeneratedSideSupports() throws Exception {
        StructurePiece corridor = mineshaftPiece(0, 40, 0, 2, 42, 4, false, 1, false, false);
        Chunk chunk = new Chunk(0, 0);

        corridor.place(null, chunk, 0L, 0, 0, new MineshaftTorchRandom());

        assertSame(BlockType.TORCH, chunk.getBlock(1, 42, 1));
        assertEquals(BlockShape.torchMetadataFromFace(Block.FACE_EAST), chunk.getBlockMetadata(1, 42, 1),
                "Generated torches should emulate source BlockTorch auto-orientation");
        assertSame(BlockType.OAK_PLANKS, chunk.getBlock(0, 42, 1));
        assertSame(BlockType.TORCH, chunk.getBlock(1, 42, 3));
        assertEquals(BlockShape.torchMetadataFromFace(Block.FACE_EAST), chunk.getBlockMetadata(1, 42, 3));
        assertSame(BlockType.OAK_PLANKS, chunk.getBlock(0, 42, 3));
    }

    @Test
    @DisplayName("Mineshaft corridors should consume the shared structure placement random")
    void mineshaftCorridorUsesSharedPlacementRandom() throws Exception {
        StructurePiece corridor = mineshaftPiece(0, 40, 0, 2, 42, 4, false, 1, false, false);
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(0, 42, 0, BlockType.STONE);

        corridor.place(null, chunk, 24680L, 0, 0, new Random(2048L));

        assertSame(BlockType.STONE, chunk.getBlock(0, 42, 0),
                "Source structure generation passes one placement Random through intersecting pieces");
    }

    @Test
    @DisplayName("Mineshaft corridor random ceiling should use source local order")
    void mineshaftCorridorRandomCeilingUsesSourceLocalOrder() throws Exception {
        StructurePiece corridor = mineshaftPiece(0, 40, 0, 2, 42, 4, false, 1, false, false);
        Chunk chunk = new Chunk(0, 0);
        for (int z = 0; z <= 4; z++) {
            for (int x = 0; x <= 2; x++) {
                chunk.setBlock(x, 42, z, BlockType.STONE);
            }
        }

        corridor.place(null, chunk, 0L, 0, 0, new Random(0L));

        assertSame(BlockType.STONE, chunk.getBlock(0, 42, 1),
                "Source randomFill iterates local x before z for corridor ceiling clearing");
        assertSame(BlockType.AIR, chunk.getBlock(1, 42, 0));
    }

    @Test
    @DisplayName("Mineshaft spider webs should use source local order")
    void mineshaftSpiderWebsUseSourceLocalOrder() throws Exception {
        StructurePiece corridor = mineshaftPiece(0, 40, 0, 2, 42, 4, false, 1, false, true);
        Chunk chunk = new Chunk(0, 0);

        corridor.place(null, chunk, 0L, 0, 0, new Random(0L));

        assertSame(BlockType.AIR, chunk.getBlock(0, 40, 1),
                "Source randomFill iterates local y, then x, then z for spider webs");
        assertSame(BlockType.COBWEB, chunk.getBlock(0, 40, 4));
    }

    @Test
    @DisplayName("Mineshaft corridors should abort when liquid touches the source envelope")
    void mineshaftCorridorAbortsWhenLiquidTouchesSourceEnvelope() throws Exception {
        StructurePiece corridor = mineshaftPiece(0, 40, 0, 2, 42, 4, false, 1, false, false);
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(1, 39, 1, BlockType.FLOWING_WATER);
        chunk.setBlock(1, 40, 1, BlockType.STONE);

        corridor.place(null, chunk, 424242L, 0, 0);

        assertSame(BlockType.FLOWING_WATER, chunk.getBlock(1, 39, 1));
        assertSame(BlockType.STONE, chunk.getBlock(1, 40, 1),
                "Source corridors return false before carving when their expanded box touches liquid");
        assertSame(BlockType.AIR, chunk.getBlock(0, 40, 2));
    }

    @Test
    @DisplayName("Mineshaft rooms should abort when liquid touches the source envelope")
    void mineshaftRoomAbortsWhenLiquidTouchesSourceEnvelope() throws Exception {
        StructurePiece room = mineshaftRoomPiece(new StructureBoundingBox(0, 40, 0, 9, 45, 9));
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(1, 39, 1, BlockType.WATER);
        chunk.setBlock(1, 40, 1, BlockType.STONE);

        room.place(null, chunk, 0L, 0, 0);

        assertSame(BlockType.WATER, chunk.getBlock(1, 39, 1));
        assertSame(BlockType.STONE, chunk.getBlock(1, 40, 1));
    }

    @Test
    @DisplayName("Mineshaft room floor should only replace non-air blocks")
    void mineshaftRoomFloorOnlyReplacesNonAirBlocks() throws Exception {
        StructurePiece room = mineshaftRoomPiece(new StructureBoundingBox(0, 40, 0, 3, 43, 3));
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(1, 40, 1, BlockType.STONE);
        chunk.setBlock(2, 40, 1, BlockType.AIR);

        room.place(null, chunk, 0L, 0, 0);

        assertSame(BlockType.DIRT, chunk.getBlock(1, 40, 1),
                "Source fillWithBlocks(..., true) writes the floor only over existing non-air terrain");
        assertSame(BlockType.AIR, chunk.getBlock(2, 40, 1));
    }

    @Test
    @DisplayName("Mineshaft rooms should clear source child-opening volumes")
    void mineshaftRoomClearsSourceChildOpeningVolumes() throws Exception {
        StructurePiece room = mineshaftRoomPiece(new StructureBoundingBox(0, 40, 0, 9, 45, 9),
                java.util.List.of(new StructureBoundingBox(0, 42, 2, 1, 44, 4)));
        Chunk chunk = new Chunk(0, 0);
        for (int y = 40; y <= 45; y++) {
            for (int z = 0; z <= 9; z++) {
                for (int x = 0; x <= 9; x++) {
                    chunk.setBlock(x, y, z, BlockType.STONE);
                }
            }
        }

        room.place(null, chunk, 0L, 0, 0);

        assertSame(BlockType.AIR, chunk.getBlock(0, 44, 2),
                "Source room openings clear the child connector's top three local Y slices");
        assertSame(BlockType.AIR, chunk.getBlock(1, 44, 4));
        assertSame(BlockType.STONE, chunk.getBlock(0, 44, 1),
                "The opening clear should stay inside the stored child-connector bounds");
    }

    @Test
    @DisplayName("Mineshaft crossings should abort when liquid touches the source envelope")
    void mineshaftCrossAbortsWhenLiquidTouchesSourceEnvelope() throws Exception {
        StructurePiece cross = mineshaftCrossPiece(new StructureBoundingBox(0, 40, 0, 4, 42, 4), false);
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(2, 39, 2, BlockType.FLOWING_LAVA);
        chunk.setBlock(2, 40, 2, BlockType.STONE);

        cross.place(null, chunk, 0L, 0, 0);

        assertSame(BlockType.FLOWING_LAVA, chunk.getBlock(2, 39, 2));
        assertSame(BlockType.STONE, chunk.getBlock(2, 40, 2));
    }

    @Test
    @DisplayName("Mineshaft crossings should add the source air-only plank support floor")
    void mineshaftCrossAddsSourceAirOnlySupportFloor() throws Exception {
        StructurePiece cross = mineshaftCrossPiece(new StructureBoundingBox(0, 40, 0, 4, 42, 4), false);
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(0, 39, 0, BlockType.STONE);

        cross.place(null, chunk, 0L, 0, 0);

        assertSame(BlockType.OAK_PLANKS, chunk.getBlock(2, 39, 2),
                "Source crossings fill air one block below the intersection with planks");
        assertSame(BlockType.STONE, chunk.getBlock(0, 39, 0),
                "The crossing support floor should only fill air and leave existing terrain intact");
        assertSame(BlockType.OAK_PLANKS, chunk.getBlock(1, 40, 1));
        assertSame(BlockType.OAK_PLANKS, chunk.getBlock(3, 42, 3));
    }

    @Test
    @DisplayName("Mineshaft stairs should abort when liquid touches the source envelope")
    void mineshaftStairsAbortsWhenLiquidTouchesSourceEnvelope() throws Exception {
        StructurePiece stairs = mineshaftStairsPiece(new StructureBoundingBox(0, 40, 0, 2, 47, 8), 0);
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(1, 39, 1, BlockType.LAVA);
        chunk.setBlock(1, 45, 0, BlockType.STONE);

        stairs.place(null, chunk, 0L, 0, 0);

        assertSame(BlockType.LAVA, chunk.getBlock(1, 39, 1));
        assertSame(BlockType.STONE, chunk.getBlock(1, 45, 0));
    }

    @Test
    @DisplayName("Mineshaft stairs should use source descending carve slices")
    void mineshaftStairsUseSourceDescendingCarveSlices() throws Exception {
        StructurePiece stairs = mineshaftStairsPiece(new StructureBoundingBox(0, 40, 0, 2, 47, 8), 0);
        Chunk chunk = new Chunk(0, 0);
        for (int y = 40; y <= 47; y++) {
            for (int z = 0; z <= 8; z++) {
                for (int x = 0; x <= 2; x++) {
                    chunk.setBlock(x, y, z, BlockType.STONE);
                }
            }
        }

        stairs.place(null, chunk, 0L, 0, 0);

        assertSame(BlockType.AIR, chunk.getBlock(1, 45, 2));
        assertSame(BlockType.AIR, chunk.getBlock(1, 44, 3));
        assertSame(BlockType.AIR, chunk.getBlock(1, 43, 4));
        assertSame(BlockType.AIR, chunk.getBlock(1, 42, 5));
        assertSame(BlockType.AIR, chunk.getBlock(1, 41, 6));
        assertSame(BlockType.AIR, chunk.getBlock(1, 44, 2),
                "Source mineshaft stair slices start one local Y lower for the first four steps");
        assertSame(BlockType.AIR, chunk.getBlock(1, 43, 3));
        assertSame(BlockType.AIR, chunk.getBlock(1, 42, 4));
        assertSame(BlockType.AIR, chunk.getBlock(1, 41, 5));
        assertSame(BlockType.STONE, chunk.getBlock(1, 43, 2));
        assertSame(BlockType.STONE, chunk.getBlock(1, 42, 3));
        assertSame(BlockType.STONE, chunk.getBlock(1, 41, 4));
        assertSame(BlockType.STONE, chunk.getBlock(1, 40, 5));
        assertSame(BlockType.STONE, chunk.getBlock(1, 40, 6));
    }

    @Test
    @DisplayName("Mineshaft rooms should use deterministic source rare upper carve")
    void mineshaftRoomUsesDeterministicSourceRareUpperCarve() throws Exception {
        StructurePiece room = mineshaftRoomPiece(new StructureBoundingBox(0, 40, 0, 9, 45, 9));
        Chunk chunk = new Chunk(0, 0);
        for (int y = 40; y <= 45; y++) {
            for (int z = 0; z <= 9; z++) {
                for (int x = 0; x <= 9; x++) {
                    chunk.setBlock(x, y, z, BlockType.STONE);
                }
            }
        }

        room.place(null, chunk, 0L, 0, 0);

        assertSame(BlockType.AIR, chunk.getBlock(2, 44, 4),
                "Source randomlyRareFillWithBlocks has no extra seeded chance gate");
        assertSame(BlockType.STONE, chunk.getBlock(0, 45, 0),
                "The rare upper carve should still respect the source ellipsoid boundary");
    }

    @Test
    @DisplayName("Mineshaft room child branches should use inclusive source Y range")
    void mineshaftRoomBranchesUseInclusiveSourceYRange() throws Exception {
        StructureBoundingBox roomBox = new StructureBoundingBox(0, 50, 0, 7, 55, 7);
        java.util.ArrayList<Object> descriptors = new java.util.ArrayList<>();
        java.util.ArrayList<StructureBoundingBox> openings = new java.util.ArrayList<>();
        java.lang.reflect.Method build = StructurePlanner.class.getDeclaredMethod("buildMineshaftRoomBranches",
                StructureBoundingBox.class, java.util.List.class, java.util.List.class, Random.class);
        build.setAccessible(true);

        build.invoke(null, roomBox, descriptors, openings, new Random(0L));

        assertFalse(descriptors.isEmpty());
        assertEquals(new StructureBoundingBox(5, 52, -20, 7, 54, -1),
                mineshaftDescriptorBounds(descriptors.get(0)));
        assertEquals(new StructureBoundingBox(5, 52, 0, 7, 54, 1), openings.get(0));
    }

    @Test
    @DisplayName("Mineshaft spider corridors should not lose spawners after off-chunk candidates")
    void mineshaftSpiderCorridorSkipsOffChunkSpawnerCandidate() throws Exception {
        StructurePiece corridor = mineshaftPiece(0, 40, -4, 2, 42, 5, false, 2, false, true);
        Chunk chunk = new Chunk(0, 0);

        corridor.place(null, chunk, 0L, 0, 0);

        assertEquals(1, count(chunk, BlockType.MOB_SPAWNER),
                "Source corridors continue after an off-chunk spider-spawner candidate");
    }

    @Test
    @DisplayName("Mineshaft spider spawners should not advance placement random")
    void mineshaftSpiderSpawnerDoesNotConsumePlacementRandom() throws Exception {
        StructurePiece corridor = mineshaftPiece(0, 40, 0, 2, 42, 4, false, 1, false, true);
        Chunk chunk = new Chunk(0, 0);
        Random placementRandom = new Random(0L);
        Random expectedRandom = new Random(0L);

        corridor.place(null, chunk, 0L, 0, 0, placementRandom);
        for (int axis = 0; axis <= 4; axis++) {
            for (int cross = 0; cross <= 2; cross++) {
                expectedRandom.nextFloat();
            }
        }
        for (int axis = 0; axis <= 4; axis++) {
            for (int y = 0; y <= 1; y++) {
                for (int cross = 0; cross <= 2; cross++) {
                    expectedRandom.nextFloat();
                }
            }
        }
        expectedRandom.nextInt(4);
        for (int i = 0; i < 10; i++) {
            expectedRandom.nextFloat();
        }
        assertNotEquals(0, expectedRandom.nextInt(100), "Fixture should not open the first chest branch");
        assertNotEquals(0, expectedRandom.nextInt(100), "Fixture should not open the second chest branch");
        expectedRandom.nextInt(3);

        assertEquals(1, count(chunk, BlockType.MOB_SPAWNER));
        assertEquals(expectedRandom.nextInt(), placementRandom.nextInt(),
                "Source cave-spider spawner placement sets the tile type without drawing a delay");
    }

    @Test
    @DisplayName("Mineshaft corridor recursion should draw branch before Y offset")
    void mineshaftCorridorRecursionUsesSourceRandomOrder() throws Exception {
        StructureBoundingBox root = new StructureBoundingBox(0, 40, -20, 7, 45, -13);
        StructureBoundingBox corridorBox = new StructureBoundingBox(0, 40, 0, 2, 42, 9);
        Object corridor = mineshaftCorridorDescriptor(corridorBox, 0, 8, 2, false, false);
        java.util.ArrayList<Object> descriptors = new java.util.ArrayList<>();
        descriptors.add(corridor);

        java.lang.reflect.Method build = StructurePlanner.class.getDeclaredMethod("buildMineshaftCorridor",
                StructureBoundingBox.class, java.util.List.class, Random.class, corridor.getClass());
        build.setAccessible(true);
        build.invoke(null, root, descriptors, new Random(0L), corridor);

        assertEquals(2, descriptors.size(), "Depth-capped fixture should add only the source-order continuation");
        assertEquals(new StructureBoundingBox(-20, 40, 6, -1, 42, 8),
                mineshaftDescriptorBounds(descriptors.get(1)));
        assertEquals(1, mineshaftDescriptorMode(descriptors.get(1)));
    }

    @Test
    @DisplayName("Mineshaft corridor chests should use the source weighted loot table")
    void mineshaftCorridorChestUsesSourceWeightedLootTable() throws Exception {
        StructurePiece corridor = mineshaftPiece(0, 40, 0, 2, 42, 4, false, 1, false, false);
        java.lang.reflect.Method chestMethod = corridor.getClass().getDeclaredMethod("placeMineshaftChest",
                World.class, Chunk.class, int.class, int.class, Random.class, int.class, int.class, int.class);
        chestMethod.setAccessible(true);

        World world = new World(24682468L);
        try {
            Chunk chunk = world.getChunkNow(0, 0);
            Random placementRandom = new Random(123456789L);
            ItemStack[] expected = expectedMineshaftInventory(new Random(123456789L));

            chestMethod.invoke(corridor, world, chunk, 0, 0, placementRandom, 2, 0, 1);
            world.reconcileLoadedTileEntities();

            assertSame(BlockType.CHEST, chunk.getBlock(2, 40, 1));
            ChestTileEntity chest = (ChestTileEntity) world.getTileEntity(2, 40, 1);
            assertNotNull(chest);
            assertInventoryEquals(expected, chest.getInventory());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Mineshaft chests should not refill existing generated chests")
    void mineshaftCorridorChestSkipsExistingChestWithoutLootRng() throws Exception {
        StructurePiece corridor = mineshaftPiece(0, 40, 0, 2, 42, 4, false, 1, false, false);
        java.lang.reflect.Method chestMethod = corridor.getClass().getDeclaredMethod("placeMineshaftChest",
                World.class, Chunk.class, int.class, int.class, Random.class, int.class, int.class, int.class);
        chestMethod.setAccessible(true);
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(2, 40, 1, BlockType.CHEST);
        Random placementRandom = new Random(123456789L);
        Random expectedRandom = new Random(123456789L);

        chestMethod.invoke(corridor, null, chunk, 0, 0, placementRandom, 2, 0, 1);
        expectedRandom.nextInt(4);

        assertSame(BlockType.CHEST, chunk.getBlock(2, 40, 1));
        assertEquals(expectedRandom.nextInt(), placementRandom.nextInt(),
                "Source structure chests skip existing chest blocks without consuming weighted loot draws");
    }

    @Test
    @DisplayName("Village wood huts should use the source block layout")
    void villageWoodHutUsesSourceBlockLayout() throws Exception {
        StructurePiece hut = villageWoodHutPiece(new StructureBoundingBox(0, 70, 0, 3, 75, 4), 0, false, 2);
        Chunk chunk = new Chunk(0, 0);

        hut.place(null, chunk, 13579L, 0, 0);

        assertSame(BlockType.COBBLESTONE, chunk.getBlock(0, 70, 0));
        assertSame(BlockType.DIRT, chunk.getBlock(1, 70, 2));
        assertSame(BlockType.AIR, chunk.getBlock(1, 71, 1));
        assertSame(BlockType.AIR, chunk.getBlock(1, 74, 2),
                "The short hut variant leaves the y=4 center open and roofs it at y=5");
        assertSame(BlockType.OAK_LOG, chunk.getBlock(1, 75, 2));
        assertSame(BlockType.OAK_LOG, chunk.getBlock(0, 74, 2));
        assertSame(BlockType.OAK_PLANKS, chunk.getBlock(0, 72, 1));
        assertSame(BlockType.GLASS_PANE, chunk.getBlock(0, 72, 2));
        assertSame(BlockType.FENCE, chunk.getBlock(2, 71, 3));
        assertSame(BlockType.WOODEN_PRESSURE_PLATE, chunk.getBlock(2, 72, 3));
        assertSame(BlockType.WOODEN_DOOR, chunk.getBlock(1, 71, 0));
        assertEquals(1, chunk.getBlockMetadata(1, 71, 0));
        assertSame(BlockType.WOODEN_DOOR, chunk.getBlock(1, 72, 0));
        assertEquals(8, chunk.getBlockMetadata(1, 72, 0));
    }

    @Test
    @DisplayName("Village garden houses should use the source block layout")
    void villageGardenHouseUsesSourceBlockLayout() throws Exception {
        StructurePiece house = villageHouse4GardenPiece(new StructureBoundingBox(0, 70, 0, 4, 75, 4), 0, true);
        Chunk chunk = new Chunk(0, 0);

        house.place(null, chunk, 24601L, 0, 0);

        assertSame(BlockType.COBBLESTONE, chunk.getBlock(0, 70, 0));
        assertSame(BlockType.COBBLESTONE, chunk.getBlock(4, 73, 4));
        assertSame(BlockType.OAK_LOG, chunk.getBlock(0, 74, 0));
        assertSame(BlockType.OAK_PLANKS, chunk.getBlock(2, 74, 2));
        assertSame(BlockType.OAK_PLANKS, chunk.getBlock(0, 72, 1));
        assertSame(BlockType.GLASS_PANE, chunk.getBlock(0, 72, 2));
        assertSame(BlockType.GLASS_PANE, chunk.getBlock(2, 72, 4));
        assertSame(BlockType.AIR, chunk.getBlock(2, 71, 0),
                "The source garden house leaves the front center open instead of placing a door");
        assertSame(BlockType.AIR, chunk.getBlock(2, 71, 2));
        assertSame(BlockType.FENCE, chunk.getBlock(0, 75, 0));
        assertSame(BlockType.FENCE, chunk.getBlock(4, 75, 3));
        assertSame(BlockType.LADDER, chunk.getBlock(3, 71, 3));
        assertEquals(2, chunk.getBlockMetadata(3, 71, 3));
        assertSame(BlockType.TORCH, chunk.getBlock(2, 73, 1));
        assertSame(BlockType.COBBLESTONE, chunk.getBlock(2, 69, 2));
    }

    @Test
    @DisplayName("Village churches should use the source block layout")
    void villageChurchUsesSourceBlockLayout() throws Exception {
        StructurePiece church = villageChurchPiece(new StructureBoundingBox(0, 70, 0, 4, 81, 8), 0);
        Chunk chunk = new Chunk(0, 0);

        church.place(null, chunk, 86420L, 0, 0);

        assertSame(BlockType.AIR, chunk.getBlock(2, 71, 1));
        assertSame(BlockType.AIR, chunk.getBlock(2, 75, 2));
        assertSame(BlockType.COBBLESTONE, chunk.getBlock(1, 70, 0));
        assertSame(BlockType.COBBLESTONE, chunk.getBlock(0, 80, 2));
        assertSame(BlockType.COBBLESTONE, chunk.getBlock(2, 81, 0));
        assertSame(BlockType.COBBLESTONE_STAIRS, chunk.getBlock(1, 71, 5));
        assertEquals(2, chunk.getBlockMetadata(1, 71, 5));
        assertSame(BlockType.COBBLESTONE_STAIRS, chunk.getBlock(1, 72, 7));
        assertEquals(1, chunk.getBlockMetadata(1, 72, 7));
        assertSame(BlockType.GLASS_PANE, chunk.getBlock(0, 72, 2));
        assertSame(BlockType.GLASS_PANE, chunk.getBlock(2, 76, 0));
        assertSame(BlockType.GLASS_PANE, chunk.getBlock(2, 73, 8));
        assertSame(BlockType.TORCH, chunk.getBlock(2, 74, 7));
        assertSame(BlockType.LADDER, chunk.getBlock(3, 71, 3));
        assertEquals(4, chunk.getBlockMetadata(3, 71, 3));
        assertSame(BlockType.LADDER, chunk.getBlock(3, 79, 3));
        assertSame(BlockType.WOODEN_DOOR, chunk.getBlock(2, 71, 0));
        assertEquals(1, chunk.getBlockMetadata(2, 71, 0));
        assertSame(BlockType.WOODEN_DOOR, chunk.getBlock(2, 72, 0));
        assertEquals(8, chunk.getBlockMetadata(2, 72, 0));
        assertSame(BlockType.COBBLESTONE, chunk.getBlock(2, 69, 4));
    }

    @Test
    @DisplayName("Village house 1 should use the source block layout")
    void villageHouse1UsesSourceBlockLayout() throws Exception {
        StructurePiece house = villageHouse1Piece(new StructureBoundingBox(0, 70, 0, 8, 78, 5), 0);
        Chunk chunk = new Chunk(0, 0);

        house.place(null, chunk, 112233L, 0, 0);

        assertSame(BlockType.AIR, chunk.getBlock(1, 71, 1));
        assertSame(BlockType.COBBLESTONE, chunk.getBlock(0, 70, 0));
        assertSame(BlockType.COBBLESTONE, chunk.getBlock(4, 77, 2));
        assertSame(BlockType.OAK_STAIRS, chunk.getBlock(4, 76, 0));
        assertEquals(2, chunk.getBlockMetadata(4, 76, 0));
        assertSame(BlockType.OAK_STAIRS, chunk.getBlock(4, 76, 5));
        assertEquals(3, chunk.getBlockMetadata(4, 76, 5));
        assertSame(BlockType.OAK_PLANKS, chunk.getBlock(8, 72, 1));
        assertSame(BlockType.GLASS_PANE, chunk.getBlock(5, 72, 0));
        assertSame(BlockType.GLASS_PANE, chunk.getBlock(0, 73, 3));
        assertSame(BlockType.GLASS_PANE, chunk.getBlock(8, 72, 2));
        assertSame(BlockType.GLASS_PANE, chunk.getBlock(2, 72, 5));
        assertSame(BlockType.BOOKSHELF, chunk.getBlock(4, 73, 4));
        assertSame(BlockType.OAK_STAIRS, chunk.getBlock(7, 71, 3));
        assertEquals(0, chunk.getBlockMetadata(7, 71, 3));
        assertSame(BlockType.OAK_STAIRS, chunk.getBlock(3, 71, 4));
        assertEquals(2, chunk.getBlockMetadata(3, 71, 4));
        assertSame(BlockType.FENCE, chunk.getBlock(4, 71, 3));
        assertSame(BlockType.WOODEN_PRESSURE_PLATE, chunk.getBlock(4, 72, 3));
        assertSame(BlockType.CRAFTING_TABLE, chunk.getBlock(7, 71, 1));
        assertSame(BlockType.WOODEN_DOOR, chunk.getBlock(1, 71, 0));
        assertEquals(1, chunk.getBlockMetadata(1, 71, 0));
        assertSame(BlockType.WOODEN_DOOR, chunk.getBlock(1, 72, 0));
        assertEquals(8, chunk.getBlockMetadata(1, 72, 0));
        assertSame(BlockType.COBBLESTONE, chunk.getBlock(4, 69, 2));
    }

    @Test
    @DisplayName("Village house 3 should use the source block layout")
    void villageHouse3UsesSourceBlockLayout() throws Exception {
        StructurePiece house = villageHouse3Piece(new StructureBoundingBox(0, 70, 0, 8, 76, 11), 0);
        Chunk chunk = new Chunk(0, 0);

        house.place(null, chunk, 445566L, 0, 0);

        assertSame(BlockType.AIR, chunk.getBlock(1, 71, 1));
        assertSame(BlockType.AIR, chunk.getBlock(3, 71, 6));
        assertSame(BlockType.OAK_PLANKS, chunk.getBlock(1, 70, 1));
        assertSame(BlockType.OAK_PLANKS, chunk.getBlock(3, 70, 6));
        assertSame(BlockType.COBBLESTONE, chunk.getBlock(0, 70, 0));
        assertSame(BlockType.COBBLESTONE, chunk.getBlock(8, 73, 10));
        assertSame(BlockType.OAK_STAIRS, chunk.getBlock(4, 74, 0));
        assertEquals(2, chunk.getBlockMetadata(4, 74, 0));
        assertSame(BlockType.OAK_STAIRS, chunk.getBlock(1, 74, 5));
        assertEquals(3, chunk.getBlockMetadata(1, 74, 5));
        assertSame(BlockType.OAK_PLANKS, chunk.getBlock(5, 76, 10));
        assertSame(BlockType.OAK_STAIRS, chunk.getBlock(4, 76, 4));
        assertEquals(0, chunk.getBlockMetadata(4, 76, 4));
        assertSame(BlockType.OAK_STAIRS, chunk.getBlock(6, 76, 4));
        assertEquals(1, chunk.getBlockMetadata(6, 76, 4));
        assertSame(BlockType.OAK_LOG, chunk.getBlock(0, 72, 1));
        assertSame(BlockType.GLASS_PANE, chunk.getBlock(0, 72, 2));
        assertSame(BlockType.GLASS_PANE, chunk.getBlock(8, 72, 7));
        assertSame(BlockType.GLASS_PANE, chunk.getBlock(2, 72, 8));
        assertSame(BlockType.GLASS_PANE, chunk.getBlock(5, 74, 10));
        assertSame(BlockType.TORCH, chunk.getBlock(2, 73, 1));
        assertSame(BlockType.WOODEN_DOOR, chunk.getBlock(2, 71, 0));
        assertEquals(1, chunk.getBlockMetadata(2, 71, 0));
        assertSame(BlockType.WOODEN_DOOR, chunk.getBlock(2, 72, 0));
        assertEquals(8, chunk.getBlockMetadata(2, 72, 0));
        assertSame(BlockType.COBBLESTONE, chunk.getBlock(4, 69, 2));
        assertSame(BlockType.COBBLESTONE, chunk.getBlock(5, 69, 8));
    }

    @Test
    @DisplayName("Village blacksmiths should use the source block layout and loot chest")
    void villageBlacksmithUsesSourceBlockLayoutAndLootChest() throws Exception {
        StructurePiece blacksmith = villageBlacksmithPiece(new StructureBoundingBox(0, 70, 0, 9, 75, 6), 0);
        Chunk chunk = new Chunk(0, 0);

        blacksmith.place(null, chunk, 778899L, 0, 0);

        assertSame(BlockType.COBBLESTONE, chunk.getBlock(0, 70, 0));
        assertSame(BlockType.STONE_SLAB, chunk.getBlock(0, 75, 0));
        assertSame(BlockType.AIR, chunk.getBlock(1, 75, 1));
        assertSame(BlockType.OAK_LOG, chunk.getBlock(0, 71, 0));
        assertSame(BlockType.OAK_PLANKS, chunk.getBlock(3, 73, 1));
        assertSame(BlockType.FENCE, chunk.getBlock(5, 71, 0));
        assertSame(BlockType.FLOWING_LAVA, chunk.getBlock(7, 71, 5));
        assertSame(BlockType.IRON_BARS, chunk.getBlock(9, 72, 5));
        assertSame(BlockType.FURNACE, chunk.getBlock(6, 72, 3));
        assertSame(BlockType.DOUBLE_STONE_SLAB, chunk.getBlock(8, 71, 1));
        assertSame(BlockType.GLASS_PANE, chunk.getBlock(0, 72, 2));
        assertSame(BlockType.WOODEN_PRESSURE_PLATE, chunk.getBlock(2, 72, 4));
        assertSame(BlockType.OAK_STAIRS, chunk.getBlock(2, 71, 5));
        assertEquals(2, chunk.getBlockMetadata(2, 71, 5));
        assertSame(BlockType.CHEST, chunk.getBlock(5, 71, 5),
                "Release-era ComponentVillageHouse2 places the blacksmith loot chest");
        assertSame(BlockType.COBBLESTONE, chunk.getBlock(5, 69, 3));

        World world = new World(778899L);
        try {
            Chunk worldChunk = world.getChunkNow(0, 0);
            Random placementRandom = new Random(123456789L);
            Random expectedRandom = new Random(123456789L);
            ItemStack[] expectedInventory = expectedStrongholdInventory(expectedRandom, VILLAGE_BLACKSMITH_LOOT,
                    3 + expectedRandom.nextInt(6));

            blacksmith.place(world, worldChunk, 778899L, 0, 0, placementRandom);
            world.reconcileLoadedTileEntities();
            world.updateEntities(0.0f);

            assertSame(BlockType.CHEST, worldChunk.getBlock(5, 71, 5));
            ChestTileEntity chest = (ChestTileEntity) world.getTileEntity(5, 71, 5);
            assertNotNull(chest);
            assertInventoryEquals(expectedInventory, chest.getInventory());
            java.util.List<Villager> villagers = world.getEntities().stream()
                    .filter(Villager.class::isInstance)
                    .map(Villager.class::cast)
                    .toList();
            assertEquals(1, villagers.size(), "Source blacksmiths spawn one smith villager");
            Villager smith = villagers.get(0);
            assertEquals(Villager.PROFESSION_SMITH, smith.getProfession());
            assertEquals("/textures/mob/villager/smith.png", smith.getTexturePath());
            assertTrue(smith.getX() >= 7.0f && smith.getX() < 8.0f);
            assertTrue(smith.getZ() >= 1.0f && smith.getZ() < 2.0f);
        } finally {
            world.cleanup();
        }

        Chunk replayChunk = new Chunk(0, 0);
        Random placementRandom = new Random(123456789L);
        Random expectedRandom = new Random(123456789L);

        blacksmith.place(null, replayChunk, 778899L, 0, 0, placementRandom);
        expectedStrongholdInventory(expectedRandom, VILLAGE_BLACKSMITH_LOOT, 3 + expectedRandom.nextInt(6));

        assertEquals(expectedRandom.nextInt(), placementRandom.nextInt(),
                "Release-era blacksmith placement should consume the source chest loot stream");
    }

    @Test
    @DisplayName("Implemented village pieces should spawn source villagers")
    void implementedVillagePiecesSpawnSourceVillagers() throws Exception {
        assertVillageVillagers("wood hut",
                villageWoodHutPiece(new StructureBoundingBox(0, 70, 0, 3, 75, 4), 0, false, 2),
                new ExpectedVillager(Villager.PROFESSION_FARMER, 1.5f, 71.0f, 2.5f));
        assertVillageVillagers("garden house",
                villageHouse4GardenPiece(new StructureBoundingBox(0, 70, 0, 4, 75, 4), 0, true),
                new ExpectedVillager(Villager.PROFESSION_FARMER, 1.5f, 71.0f, 2.5f));
        assertVillageVillagers("church",
                villageChurchPiece(new StructureBoundingBox(0, 70, 0, 4, 81, 8), 0),
                new ExpectedVillager(Villager.PROFESSION_PRIEST, 2.5f, 71.0f, 2.5f));
        assertVillageVillagers("house 1",
                villageHouse1Piece(new StructureBoundingBox(0, 70, 0, 8, 78, 5), 0),
                new ExpectedVillager(Villager.PROFESSION_LIBRARIAN, 2.5f, 71.0f, 2.5f));
        assertVillageVillagers("house 3",
                villageHouse3Piece(new StructureBoundingBox(0, 70, 0, 8, 76, 11), 0),
                new ExpectedVillager(Villager.PROFESSION_FARMER, 4.5f, 71.0f, 2.5f),
                new ExpectedVillager(Villager.PROFESSION_FARMER, 5.5f, 71.0f, 2.5f));
        assertVillageVillagers("blacksmith",
                villageBlacksmithPiece(new StructureBoundingBox(0, 70, 0, 9, 75, 6), 0),
                new ExpectedVillager(Villager.PROFESSION_SMITH, 7.5f, 71.0f, 1.5f));
        assertVillageVillagers("hall",
                villageHallPiece(new StructureBoundingBox(0, 70, 0, 8, 76, 10), 0),
                new ExpectedVillager(Villager.PROFESSION_BUTCHER, 4.5f, 71.0f, 2.5f),
                new ExpectedVillager(Villager.PROFESSION_FARMER, 5.5f, 71.0f, 2.5f));
    }

    @Test
    @DisplayName("Village path queue should attach implemented village pieces with source fallback torches")
    void villagePathQueueAttachesImplementedPiecesWithoutTorchSpam() {
        long seed = 38L;
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(seed, Dimension.OVERWORLD);
        StructureStart start = new StructurePlanner()
                .startsForChunk(seed, Dimension.OVERWORLD, -79, 9, generator).stream()
                .filter(candidate -> candidate.type() == StructureType.VILLAGE)
                .findFirst()
                .orElseThrow();

        assertEquals(1, countPieces(start, "VillageWellPiece"));
        assertEquals(1, countPieces(start, "VillageChurchPiece"));
        assertEquals(2, countPieces(start, "VillageHouse1Piece"));
        assertEquals(1, countPieces(start, "VillageHouse3Piece"));
        assertEquals(3, countPieces(start, "VillageHouse4GardenPiece"));
        assertEquals(2, countPieces(start, "VillageWoodHutPiece"));
        assertEquals(2, countPieces(start, "VillageHallPiece"));
        assertEquals(7, countPieces(start, "VillageFarmPiece"));
        assertTrue(countPieces(start, "VillagePathPiece") > 4,
                "A successful side attachment should allow source-style path branching to continue");
        assertTrue(start.pieces().stream()
                .filter(piece -> piece.getClass().getSimpleName().equals("VillagePathPiece"))
                .allMatch(piece -> villagePathComponentType(piece) == 0),
                "Release village side-road branches reuse the parent path depth");
        assertTrue(start.pieces().stream()
                .filter(piece -> piece.getClass().getSimpleName().equals("VillagePathPiece"))
                .allMatch(piece -> villagePathComponentType(piece) <= 3),
                "Village path recursion should still respect the source depth cap");
        assertEquals(2, countPieces(start, "VillageTorchPiece"),
                "Source torch fallback should follow the distance-guarded village RNG stream");
    }

    @Test
    @DisplayName("Village recursive queue should drain paths before buildings")
    void villageRecursiveQueueProcessesPathsBeforeBuildings() throws Exception {
        Object context = villageGenerationContext(new StructureBoundingBox(0, 70, 0, 5, 85, 5),
                java.util.List.of());
        java.util.List<Object> paths = villagePendingList(context, "pendingPaths");
        java.util.List<Object> buildings = villagePendingList(context, "pendingBuildings");
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(38L, Dimension.OVERWORLD);
        StructureStart start = new StructureStart(StructureType.VILLAGE, 0, 0);
        paths.add(villagePathPiece(new StructureBoundingBox(20, 70, 20, 22, 72, 26), generator, 0, 0));
        paths.add(villagePathPiece(new StructureBoundingBox(30, 70, 20, 32, 72, 26), generator, 0, 0));
        buildings.add(villageHouse1Piece(new StructureBoundingBox(40, 70, 20, 48, 78, 25), 0));
        BoundRecordingRandom random = new BoundRecordingRandom();

        processVillageQueues(context, start, random, generator);

        assertFalse(random.bounds().isEmpty());
        assertEquals(2, random.bounds().get(0),
                "StructureVillageStart chooses from the pending path queue before pending buildings");
    }

    @Test
    @DisplayName("Village path distance guard should run before length search")
    void villagePathDistanceGuardRunsBeforeLengthSearch() throws Exception {
        Object context = villageGenerationContext(new StructureBoundingBox(0, 70, 0, 5, 85, 5),
                java.util.List.of());
        StructureStart start = new StructureStart(StructureType.VILLAGE, 0, 0);
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(38L, Dimension.OVERWORLD);
        CountingRandom random = new CountingRandom();

        StructurePiece path = addVillagePath(context, start, random, generator, 113, 70, 0, 1, 0);

        assertNull(path, "Release 1.0 rejects a too-distant path input before testing shorter road lengths");
        assertEquals(0, random.nextIntCalls(),
                "The path length draw should not be consumed after the source distance guard rejects the input");
        assertTrue(start.pieces().isEmpty());
    }

    @Test
    @DisplayName("Village paths should use the source top solid or liquid column")
    void villagePathsUseSourceTopSolidOrLiquidColumn() throws Exception {
        long seed = 38L;
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(seed, Dimension.OVERWORLD);
        StructurePiece path = villagePathPiece(new StructureBoundingBox(0, 70, 0, 2, 72, 4),
                generator, 0, 0);
        Chunk chunk = new Chunk(0, 0);
        int roadX = 1;
        int roadZ = 2;
        int roadY = Math.max(ReleaseOneWorldGenerator.SEA_LEVEL, generator.terrainTopY(roadX, roadZ));
        chunk.setBlock(roadX, roadY, roadZ, BlockType.DIRT, 0);
        chunk.setBlock(roadX, roadY + 1, roadZ, BlockType.TALL_GRASS, 1);
        chunk.setBlock(roadX, roadY + 2, roadZ, BlockType.LEAVES, 0);
        chunk.setBlock(roadX, roadY + 3, roadZ, BlockType.OAK_LOG, 0);
        chunk.setBlock(roadX, roadY + 4, roadZ, BlockType.LEAVES, 0);
        int waterX = 2;
        int waterZ = 3;
        int waterY = roadY + 5;
        chunk.setBlock(waterX, waterY, waterZ, BlockType.WATER, 0);
        chunk.setBlock(waterX, waterY + 1, waterZ, BlockType.LEAVES, 0);

        path.place(null, chunk, seed, 0, 0);

        assertSame(BlockType.DIRT, chunk.getBlock(roadX, roadY, roadZ));
        assertSame(BlockType.TALL_GRASS, chunk.getBlock(roadX, roadY + 1, roadZ));
        assertSame(BlockType.LEAVES, chunk.getBlock(roadX, roadY + 2, roadZ));
        assertSame(BlockType.GRAVEL, chunk.getBlock(roadX, roadY + 3, roadZ),
                "The road should replace the highest solid, non-leaf block in the live column");
        assertSame(BlockType.LEAVES, chunk.getBlock(roadX, roadY + 4, roadZ),
                "Leaves are ignored by the source top-column scan and should not be cleared by the path");
        assertSame(BlockType.GRAVEL, chunk.getBlock(waterX, waterY, waterZ),
                "Village paths should treat fluids as top solid/liquid height candidates");
        assertSame(BlockType.LEAVES, chunk.getBlock(waterX, waterY + 1, waterZ));
    }

    @Test
    @DisplayName("Village weighted selector should stop when source weights are exhausted")
    void villageWeightedSelectorStopsWhenWeightsExhausted() throws Exception {
        Object context = villageGenerationContext(new StructureBoundingBox(0, 70, 0, 5, 85, 5),
                java.util.List.of());
        StructureStart start = new StructureStart(StructureType.VILLAGE, 0, 0);
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(38L, Dimension.OVERWORLD);

        StructurePiece piece = chooseVillageComponent(context, start, new Random(1L), generator,
                10, 70, 10, 0, 1);

        assertNull(piece,
                "Release 1.0 returns null before the torch fallback path when no village weights can spawn");
    }

    @Test
    @DisplayName("Village torch fallback should place supported wall torches")
    void villageTorchFallbackUsesSupportedWallTorchMetadata() throws Exception {
        for (int mode = 0; mode < 4; mode++) {
            StructurePiece torch = villageTorchPiece(new StructureBoundingBox(2, 70, 2, 4, 73, 3), mode);
            Chunk chunk = new Chunk(0, 0);

            torch.place(null, chunk, 97531L, 0, 0);

            int torches = 0;
            for (int x = 0; x < Chunk.WIDTH; x++) {
                for (int z = 0; z < Chunk.DEPTH; z++) {
                    if (chunk.getBlock(x, 73, z) != BlockType.TORCH) {
                        continue;
                    }
                    torches++;
                    assertVillageLampTorchSupport(chunk, x, 73, z, mode);
                }
            }
            assertEquals(4, torches, "Village lamp should keep all four fallback torches in mode " + mode);
        }
    }

    @Test
    @DisplayName("Village halls should use the source block layout")
    void villageHallUsesSourceBlockLayout() throws Exception {
        StructurePiece hall = villageHallPiece(new StructureBoundingBox(0, 70, 0, 8, 76, 10), 0);
        Chunk chunk = new Chunk(0, 0);

        hall.place(null, chunk, 97531L, 0, 0);

        assertSame(BlockType.AIR, chunk.getBlock(1, 71, 1));
        assertSame(BlockType.DIRT, chunk.getBlock(2, 70, 6));
        assertSame(BlockType.FENCE, chunk.getBlock(2, 71, 6));
        assertSame(BlockType.OAK_PLANKS, chunk.getBlock(1, 70, 1));
        assertSame(BlockType.COBBLESTONE, chunk.getBlock(0, 72, 0));
        assertSame(BlockType.OAK_STAIRS, chunk.getBlock(4, 74, 0));
        assertEquals(2, chunk.getBlockMetadata(4, 74, 0));
        assertSame(BlockType.OAK_STAIRS, chunk.getBlock(4, 76, 2));
        assertEquals(2, chunk.getBlockMetadata(4, 76, 2));
        assertSame(BlockType.OAK_LOG, chunk.getBlock(8, 72, 1));
        assertSame(BlockType.GLASS_PANE, chunk.getBlock(0, 72, 2));
        assertSame(BlockType.DOUBLE_STONE_SLAB, chunk.getBlock(6, 71, 1));
        assertSame(BlockType.WOODEN_DOOR, chunk.getBlock(2, 71, 0));
        assertEquals(1, chunk.getBlockMetadata(2, 71, 0));
        assertSame(BlockType.WOODEN_DOOR, chunk.getBlock(6, 72, 5));
        assertEquals(8, chunk.getBlockMetadata(6, 72, 5));
        assertSame(BlockType.TORCH, chunk.getBlock(2, 73, 1));
        assertSame(BlockType.COBBLESTONE, chunk.getBlock(4, 69, 2));
    }

    @Test
    @DisplayName("Village farm variants should use source block layouts")
    void villageFarmVariantsUseSourceBlockLayouts() throws Exception {
        StructurePiece wideFarm = villageFarmPiece(new StructureBoundingBox(0, 70, 0, 12, 73, 8), 0, true);
        Chunk wide = new Chunk(0, 0);

        wideFarm.place(null, wide, 24680L, 0, 0);

        assertSame(BlockType.OAK_LOG, wide.getBlock(0, 70, 0));
        assertSame(BlockType.OAK_LOG, wide.getBlock(6, 70, 8));
        assertSame(BlockType.OAK_LOG, wide.getBlock(12, 70, 4));
        assertSame(BlockType.FARMLAND, wide.getBlock(1, 70, 1));
        assertSame(BlockType.FARMLAND, wide.getBlock(11, 70, 7));
        assertSame(BlockType.FLOWING_WATER, wide.getBlock(3, 70, 1));
        assertSame(BlockType.FLOWING_WATER, wide.getBlock(9, 70, 7));
        assertSame(BlockType.CROPS, wide.getBlock(1, 71, 1));
        assertCropAge(wide, 1, 71, 1);
        assertSame(BlockType.CROPS, wide.getBlock(11, 71, 7));
        assertCropAge(wide, 11, 71, 7);
        assertSame(BlockType.AIR, wide.getBlock(6, 72, 4));
        assertSame(BlockType.DIRT, wide.getBlock(6, 69, 4));

        StructurePiece narrowFarm = villageFarmPiece(new StructureBoundingBox(0, 70, 0, 6, 73, 8), 0, false);
        Chunk narrow = new Chunk(0, 0);

        narrowFarm.place(null, narrow, 13579L, 0, 0);

        assertSame(BlockType.OAK_LOG, narrow.getBlock(0, 70, 0));
        assertSame(BlockType.OAK_LOG, narrow.getBlock(6, 70, 8));
        assertSame(BlockType.FARMLAND, narrow.getBlock(1, 70, 1));
        assertSame(BlockType.FARMLAND, narrow.getBlock(5, 70, 7));
        assertSame(BlockType.FLOWING_WATER, narrow.getBlock(3, 70, 7));
        assertSame(BlockType.CROPS, narrow.getBlock(5, 71, 7));
        assertCropAge(narrow, 5, 71, 7);
        assertSame(BlockType.AIR, narrow.getBlock(3, 72, 4));
        assertSame(BlockType.DIRT, narrow.getBlock(3, 69, 4));
    }

    @Test
    @DisplayName("Village farm crop ages should consume the shared structure placement random")
    void villageFarmCropAgesUsePlacementRandomStream() throws Exception {
        StructurePiece wideFarm = villageFarmPiece(new StructureBoundingBox(0, 70, 0, 12, 73, 8), 0, true);
        Chunk wide = new Chunk(0, 0);
        Random wideRandom = new Random(2468L);

        wideFarm.place(null, wide, 0L, 0, 0, wideRandom);

        assertFarmCropAgesMatchRandom(wide, new Random(2468L), new int[] {1, 2, 4, 5, 7, 8, 10, 11});

        StructurePiece narrowFarm = villageFarmPiece(new StructureBoundingBox(0, 70, 0, 6, 73, 8), 0, false);
        Chunk narrow = new Chunk(0, 0);
        Random narrowRandom = new Random(97531L);

        narrowFarm.place(null, narrow, 0L, 0, 0, narrowRandom);

        assertFarmCropAgesMatchRandom(narrow, new Random(97531L), new int[] {1, 2, 4, 5});
    }

    @Test
    @DisplayName("Village starts should use Release 1.0 spacing, separation, salt, and biome gate")
    void villagePlacementUsesReleaseOneGridAndBiomeGate() {
        long seed = 38L;
        StructureGenerator structures = new StructureGenerator();
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(seed, Dimension.OVERWORLD);
        int chunkX = -79;
        int chunkZ = 9;
        int centerX = chunkX * Chunk.WIDTH + 8;
        int centerZ = chunkZ * Chunk.DEPTH + 8;
        assertSame(BiomeType.DESERT, generator.getBiome(centerX, centerZ));
        assertTrue(structures.suppressesOverworldLakes(seed, chunkX, chunkZ, generator),
                "Release 1.0 skips lake population when village generation claims the chunk");
        assertFalse(structures.suppressesOverworldLakes(seed, chunkX + 1, chunkZ, generator));

        World world = new World(seed);
        try {
            Chunk chunk = new Chunk(chunkX, chunkZ);
            structures.generate(world, chunk, seed, chunkX, chunkZ, Dimension.OVERWORLD, generator);

            assertVillageWell(chunk, villageWellGroundY(generator, chunkX, chunkZ));
            assertVillageRoads(chunk, seed, generator, chunkX, chunkZ);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Village biome gate should use the source generation layer")
    void villageBiomeGateUsesGenerationLayer() {
        long seed = 0L;
        int chunkX = 116;
        int chunkZ = 176;
        int centerX = chunkX * Chunk.WIDTH + 8;
        int centerZ = chunkZ * Chunk.DEPTH + 8;
        StructureGenerator structures = new StructureGenerator();
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(seed, Dimension.OVERWORLD);

        assertSame(BiomeType.OCEAN, generator.getBiome(centerX, centerZ),
                "Final Voronoi biome at this source village origin is not a village biome");
        assertSame(BiomeType.DESERT, generator.getBiomeForGenerationLayer(centerX >> 2, centerZ >> 2),
                "Release 1.0 MapGenVillage checks WorldChunkManager's generation layer");
        assertTrue(structures.suppressesOverworldLakes(seed, chunkX, chunkZ, generator));
    }

    @Test
    @DisplayName("Non-sizeable village starts should not generate or suppress lakes")
    void nonSizeableVillageStartsDoNotGenerateOrSuppressLakes() {
        long seed = 0L;
        int chunkX = 107;
        int chunkZ = -59;
        int centerX = chunkX * Chunk.WIDTH + 8;
        int centerZ = chunkZ * Chunk.DEPTH + 8;
        StructureGenerator structures = new StructureGenerator();
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(seed, Dimension.OVERWORLD);

        assertSame(BiomeType.DESERT, generator.getBiomeForGenerationLayer(centerX >> 2, centerZ >> 2));
        assertSame(BiomeType.DESERT, villageBiome(seed, chunkX, chunkZ, generator),
                "The old grid/biome gate should accept this origin before the size check");

        StructurePlanner planner = new StructurePlanner();
        assertTrue(planner.startsForChunk(seed, Dimension.OVERWORLD, chunkX, chunkZ, generator).stream()
                .noneMatch(candidate -> candidate.type() == StructureType.VILLAGE
                        && candidate.chunkX() == chunkX && candidate.chunkZ() == chunkZ),
                "A well-only or road-only attempt should fail the source village size gate");
        assertFalse(structures.suppressesOverworldLakes(seed, chunkX, chunkZ, generator),
                "Failed village starts should not suppress lake population");
        int wellX = chunkX * Chunk.WIDTH + 5;
        int wellZ = chunkZ * Chunk.DEPTH + 5;
        assertFalse(structures.contains(seed, Dimension.OVERWORLD, StructureType.VILLAGE, wellX,
                villageWellGroundY(generator, chunkX, chunkZ), wellZ, generator));
    }

    @Test
    @DisplayName("World-backed Overworld population should place structures before lakes and decorators")
    void worldBackedOverworldPopulationPlacesStructuresInSourceOrder() {
        long seed = 38L;
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(seed, Dimension.OVERWORLD);
        int chunkX = -79;
        int chunkZ = 9;
        World world = new World(seed);
        try {
            Chunk chunk = world.getChunkNow(chunkX, chunkZ);

            assertVillageWell(chunk, villageWellGroundY(generator, chunkX, chunkZ));
            assertVillageRoads(chunk, seed, generator, chunkX, chunkZ);
            assertTrue(count(chunk, BlockType.GRAVEL) > 16);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Null-world ReleaseOne chunks should include structure blocks")
    void nullWorldReleaseOneChunksIncludeStructureBlocks() {
        ReleaseOneWorldGenerator overworld = new ReleaseOneWorldGenerator(38L, Dimension.OVERWORLD);
        Chunk village = new Chunk(-79, 9);
        overworld.generateChunk(null, village, -79, 9);

        assertTrue(count(village, BlockType.GRAVEL) > 16,
                "Village roads should be visible in null-world Overworld chunks");
        assertVillageWell(village, villageWellGroundY(overworld, -79, 9));
        assertVillageRoads(village, 38L, overworld, -79, 9);

        ReleaseOneWorldGenerator nether = new ReleaseOneWorldGenerator(97531L, Dimension.NETHER);
        Chunk fortress = new Chunk(-23, 9);
        nether.generateChunk(null, fortress, -23, 9);

        assertTrue(count(fortress, BlockType.NETHER_BRICK) > 16,
                "Fortresses should be visible in null-world Nether chunks");
    }

    private static boolean contains(Chunk chunk, BlockType type) {
        return count(chunk, type) > 0;
    }

    private static long countPieces(StructureStart start, String simpleName) {
        return start.pieces().stream()
                .filter(piece -> piece.getClass().getSimpleName().equals(simpleName))
                .count();
    }

    private static int villagePathComponentType(StructurePiece piece) {
        try {
            java.lang.reflect.Field field = piece.getClass().getDeclaredField("componentType");
            field.setAccessible(true);
            return field.getInt(piece);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static void assertVillageWell(Chunk chunk, int groundY) {
        assertSame(BlockType.GRAVEL, chunk.getBlock(2, groundY, 2));
        assertSame(BlockType.COBBLESTONE, chunk.getBlock(3, groundY, 3));
        assertSame(BlockType.FLOWING_WATER, chunk.getBlock(4, groundY, 4));
        assertSame(BlockType.AIR, chunk.getBlock(4, groundY + 1, 4));
        assertSame(BlockType.FENCE, chunk.getBlock(3, groundY + 2, 3));
        assertSame(BlockType.FENCE, chunk.getBlock(3, groundY + 3, 3));
        assertSame(BlockType.COBBLESTONE, chunk.getBlock(4, groundY + 4, 4));
    }

    private static int villageWellGroundY(ReleaseOneWorldGenerator generator, int chunkX, int chunkZ) {
        int minX = chunkX * Chunk.WIDTH + 2;
        int minZ = chunkZ * Chunk.DEPTH + 2;
        int total = 0;
        int count = 0;
        for (int z = minZ; z <= minZ + 5; z++) {
            for (int x = minX; x <= minX + 5; x++) {
                total += Math.max(generator.terrainTopY(x, z), ReleaseOneWorldGenerator.SEA_LEVEL);
                count++;
            }
        }
        return total / count;
    }

    private static void assertVillageRoads(Chunk chunk, long seed, ReleaseOneWorldGenerator generator,
            int chunkX, int chunkZ) {
        int wellMinX = chunkX * Chunk.WIDTH + 2;
        int wellMinZ = chunkZ * Chunk.DEPTH + 2;
        int wellGroundY = villageWellGroundY(generator, chunkX, chunkZ);
        StructureBoundingBox wellBox = new StructureBoundingBox(wellMinX, wellGroundY - 11, wellMinZ,
                wellMinX + 5, wellGroundY + 4, wellMinZ + 5);
        java.util.Random random = villageStartRandom(seed, chunkX, chunkZ);
        consumeVillagePieceWeightRandoms(random);
        random.nextInt(4);
        int roadY = wellBox.maxY() - 4;
        assertVillagePath(chunk, generator, chunkX, chunkZ,
                villagePathBox(random, wellBox.minX() - 1, roadY, wellBox.minZ() + 1, 1));
        assertVillagePath(chunk, generator, chunkX, chunkZ,
                villagePathBox(random, wellBox.maxX() + 1, roadY, wellBox.minZ() + 1, 3));
        assertVillagePath(chunk, generator, chunkX, chunkZ,
                villagePathBox(random, wellBox.minX() + 1, roadY, wellBox.minZ() - 1, 2));
        assertVillagePath(chunk, generator, chunkX, chunkZ,
                villagePathBox(random, wellBox.minX() + 1, roadY, wellBox.maxZ() + 1, 0));
    }

    private static void assertVillagePath(Chunk chunk, ReleaseOneWorldGenerator generator, int chunkX, int chunkZ,
            StructureBoundingBox box) {
        int minX = chunkX * Chunk.WIDTH;
        int minZ = chunkZ * Chunk.DEPTH;
        int maxX = minX + Chunk.WIDTH - 1;
        int maxZ = minZ + Chunk.DEPTH - 1;
        int checked = 0;
        for (int x = Math.max(box.minX(), minX); x <= Math.min(box.maxX(), maxX); x++) {
            for (int z = Math.max(box.minZ(), minZ); z <= Math.min(box.maxZ(), maxZ); z++) {
                int y = Math.max(ReleaseOneWorldGenerator.SEA_LEVEL, generator.terrainTopY(x, z));
                assertSame(BlockType.GRAVEL, chunk.getBlock(x - minX, y, z - minZ),
                        "Expected village path gravel at " + x + "," + y + "," + z);
                checked++;
            }
        }
        assertTrue(checked > 0, "Expected source village path box to intersect the target chunk");
    }

    private static void assertCropAge(Chunk chunk, int x, int y, int z) {
        int metadata = chunk.getBlockMetadata(x, y, z);
        assertTrue(metadata >= 2 && metadata <= 7, "Village crop age should be in the source 2..7 range");
    }

    private static void assertFarmCropAgesMatchRandom(Chunk chunk, Random expectedRandom, int[] cropXs) {
        for (int z = 1; z <= 7; z++) {
            for (int x : cropXs) {
                assertEquals(randomIntegerInRange(expectedRandom, 2, 7), chunk.getBlockMetadata(x, 71, z),
                        "Village crop age should follow the shared placement random at " + x + ",71," + z);
            }
        }
    }

    private static ItemStack[] expectedMineshaftInventory(Random random) {
        ItemStack[] inventory = new ItemStack[ChestTileEntity.SIZE];
        int rolls = 3 + random.nextInt(4);
        for (int i = 0; i < rolls; i++) {
            MineshaftLootSpec loot = weightedMineshaftLoot(random);
            int count = loot.minCount() >= loot.maxCount()
                    ? loot.minCount()
                    : random.nextInt(loot.maxCount() - loot.minCount() + 1) + loot.minCount();
            inventory[random.nextInt(ChestTileEntity.SIZE)] = new ItemStack(loot.type(), count);
        }
        return inventory;
    }

    private static ItemStack[] expectedStrongholdChestCorridorInventory(long seed, StructureBoundingBox box) {
        Random random = new Random(seed ^ (long) box.minX() * 341873128712L
                ^ (long) box.minY() * 42317861L ^ (long) box.minZ() * 132897987541L);
        consumeStrongholdShellRandom(random, 0, 0, 0, 4, 4, 6);

        ItemStack[] inventory = new ItemStack[ChestTileEntity.SIZE];
        int rolls = 2 + random.nextInt(2);
        for (int i = 0; i < rolls; i++) {
            StrongholdLootSpec loot = weightedStrongholdChestLoot(random);
            int count = loot.minCount() >= loot.maxCount()
                    ? loot.minCount()
                    : random.nextInt(loot.maxCount() - loot.minCount() + 1) + loot.minCount();
            inventory[random.nextInt(ChestTileEntity.SIZE)] = new ItemStack(loot.type(), count);
        }
        return inventory;
    }

    private static ItemStack[][] expectedStrongholdLibraryInventories(long seed, StructureBoundingBox box) {
        Random random = new Random(seed ^ (long) box.minX() * 341873128712L
                ^ (long) box.minY() * 42317861L ^ (long) box.minZ() * 132897987541L);
        boolean largeRoom = height(box) > 6;
        consumeStrongholdShellRandom(random, 0, 0, 0, 13, largeRoom ? 10 : 5, 14);
        consumeRandomFill(random, 2, 1, 1, 11, 4, 13);

        ItemStack[] lower = expectedStrongholdInventory(random, STRONGHOLD_LIBRARY_LOOT, 1 + random.nextInt(4));
        ItemStack[] upper = largeRoom
                ? expectedStrongholdInventory(random, STRONGHOLD_LIBRARY_LOOT, 1 + random.nextInt(4))
                : new ItemStack[ChestTileEntity.SIZE];
        return new ItemStack[][] { lower, upper };
    }

    private static ItemStack[] expectedStrongholdRoomCrossingInventory(long seed, StructureBoundingBox box) {
        Random random = new Random(seed ^ (long) box.minX() * 341873128712L
                ^ (long) box.minY() * 42317861L ^ (long) box.minZ() * 132897987541L);
        assertEquals(2, random.nextInt(5), "Fixture seed should force the balcony room-crossing variant");
        consumeStrongholdShellRandom(random, 0, 0, 0, 10, 6, 10);
        return expectedStrongholdInventory(random, STRONGHOLD_ROOM_CROSSING_LOOT, 1 + random.nextInt(4));
    }

    private static void consumeStrongholdShellRandom(Random random, int minX, int minY, int minZ, int maxX,
            int maxY, int maxZ) {
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean edge = x == minX || x == maxX || y == minY || y == maxY
                            || z == minZ || z == maxZ;
                    if (edge) {
                        random.nextFloat();
                    }
                }
            }
        }
    }

    private static void consumeRandomFill(Random random, int minX, int minY, int minZ, int maxX, int maxY,
            int maxZ) {
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    random.nextFloat();
                }
            }
        }
    }

    private static ItemStack[] expectedStrongholdInventory(Random random, StrongholdLootSpec[] loot, int rolls) {
        ItemStack[] inventory = new ItemStack[ChestTileEntity.SIZE];
        for (int i = 0; i < rolls; i++) {
            StrongholdLootSpec entry = weightedStrongholdLoot(random, loot);
            int count = entry.minCount() >= entry.maxCount()
                    ? entry.minCount()
                    : random.nextInt(entry.maxCount() - entry.minCount() + 1) + entry.minCount();
            inventory[random.nextInt(ChestTileEntity.SIZE)] = new ItemStack(entry.type(), count);
        }
        return inventory;
    }

    private static StrongholdLootSpec weightedStrongholdChestLoot(Random random) {
        return weightedStrongholdLoot(random, STRONGHOLD_CHEST_CORRIDOR_LOOT);
    }

    private static StrongholdLootSpec weightedStrongholdLoot(Random random, StrongholdLootSpec[] lootTable) {
        int totalWeight = 0;
        for (StrongholdLootSpec loot : lootTable) {
            totalWeight += loot.weight();
        }
        int choice = random.nextInt(totalWeight);
        for (StrongholdLootSpec loot : lootTable) {
            choice -= loot.weight();
            if (choice < 0) {
                return loot;
            }
        }
        return lootTable[lootTable.length - 1];
    }

    private static MineshaftLootSpec weightedMineshaftLoot(Random random) {
        int totalWeight = 0;
        for (MineshaftLootSpec loot : MINESHAFT_LOOT) {
            totalWeight += loot.weight();
        }
        int choice = random.nextInt(totalWeight);
        for (MineshaftLootSpec loot : MINESHAFT_LOOT) {
            choice -= loot.weight();
            if (choice < 0) {
                return loot;
            }
        }
        return MINESHAFT_LOOT[MINESHAFT_LOOT.length - 1];
    }

    private static void assertInventoryEquals(ItemStack[] expected, ItemStack[] actual) {
        assertEquals(expected.length, actual.length);
        for (int slot = 0; slot < expected.length; slot++) {
            ItemStack expectedStack = expected[slot];
            ItemStack actualStack = actual[slot];
            if (expectedStack == null) {
                assertNull(actualStack, "Expected empty slot " + slot);
            } else {
                assertNotNull(actualStack, "Expected filled slot " + slot);
                assertSame(expectedStack.getType(), actualStack.getType(), "Wrong item in slot " + slot);
                assertEquals(expectedStack.getCount(), actualStack.getCount(), "Wrong count in slot " + slot);
            }
        }
    }

    private static void assertStrongholdStoneShell(Chunk chunk, int x, int y, int z) {
        BlockType type = worldBlock(chunk, 0, 0, x, y, z);
        int metadata = worldMetadata(chunk, 0, 0, x, y, z);
        assertSame(BlockType.STONE_BRICK, type);
        assertTrue(metadata >= 0 && metadata <= 2,
                "Stronghold stone shell should use plain, mossy, or cracked stone-brick metadata");
    }

    private static void assertVillageLampTorchSupport(Chunk chunk, int x, int y, int z, int mode) {
        int metadata = chunk.getBlockMetadata(x, y, z);
        assertTrue(metadata >= 1 && metadata <= 4,
                "Village lamp torch should use wall metadata in mode " + mode);
        int supportX = x + switch (metadata) {
            case 1 -> -1;
            case 2 -> 1;
            default -> 0;
        };
        int supportZ = z + switch (metadata) {
            case 3 -> -1;
            case 4 -> 1;
            default -> 0;
        };
        assertSame(BlockType.WHITE_WOOL, chunk.getBlock(supportX, y, supportZ),
                "Village lamp torch should attach to the wool lamp block in mode " + mode);
    }

    private static final MineshaftLootSpec[] MINESHAFT_LOOT = {
            new MineshaftLootSpec(ItemType.IRON_INGOT, 1, 5, 10),
            new MineshaftLootSpec(ItemType.GOLD_INGOT, 1, 3, 5),
            new MineshaftLootSpec(ItemType.REDSTONE, 4, 9, 5),
            new MineshaftLootSpec(ItemType.INK_SAC, 4, 9, 5),
            new MineshaftLootSpec(ItemType.DIAMOND, 1, 2, 3),
            new MineshaftLootSpec(ItemType.COAL, 3, 8, 10),
            new MineshaftLootSpec(ItemType.BREAD, 1, 3, 15),
            new MineshaftLootSpec(ItemType.IRON_PICKAXE, 1, 1, 1),
            new MineshaftLootSpec(ItemType.RAIL, 4, 8, 1),
            new MineshaftLootSpec(ItemType.MELON_SEEDS, 2, 4, 10),
            new MineshaftLootSpec(ItemType.PUMPKIN_SEEDS, 2, 4, 10)
    };

    private record MineshaftLootSpec(ItemType type, int minCount, int maxCount, int weight) {
    }

    private static final StrongholdLootSpec[] STRONGHOLD_CHEST_CORRIDOR_LOOT = {
            new StrongholdLootSpec(ItemType.ENDER_PEARL, 1, 1, 10),
            new StrongholdLootSpec(ItemType.DIAMOND, 1, 3, 3),
            new StrongholdLootSpec(ItemType.IRON_INGOT, 1, 5, 10),
            new StrongholdLootSpec(ItemType.GOLD_INGOT, 1, 3, 5),
            new StrongholdLootSpec(ItemType.REDSTONE, 4, 9, 5),
            new StrongholdLootSpec(ItemType.BREAD, 1, 3, 15),
            new StrongholdLootSpec(ItemType.APPLE, 1, 3, 15),
            new StrongholdLootSpec(ItemType.IRON_PICKAXE, 1, 1, 5),
            new StrongholdLootSpec(ItemType.IRON_SWORD, 1, 1, 5),
            new StrongholdLootSpec(ItemType.IRON_CHESTPLATE, 1, 1, 5),
            new StrongholdLootSpec(ItemType.IRON_HELMET, 1, 1, 5),
            new StrongholdLootSpec(ItemType.IRON_LEGGINGS, 1, 1, 5),
            new StrongholdLootSpec(ItemType.IRON_BOOTS, 1, 1, 5),
            new StrongholdLootSpec(ItemType.GOLDEN_APPLE, 1, 1, 1)
    };

    private static final StrongholdLootSpec[] STRONGHOLD_LIBRARY_LOOT = {
            new StrongholdLootSpec(ItemType.BOOK, 1, 3, 20),
            new StrongholdLootSpec(ItemType.PAPER, 2, 7, 20),
            new StrongholdLootSpec(ItemType.MAP, 1, 1, 1),
            new StrongholdLootSpec(ItemType.COMPASS, 1, 1, 1)
    };

    private static final StrongholdLootSpec[] STRONGHOLD_ROOM_CROSSING_LOOT = {
            new StrongholdLootSpec(ItemType.IRON_INGOT, 1, 5, 10),
            new StrongholdLootSpec(ItemType.GOLD_INGOT, 1, 3, 5),
            new StrongholdLootSpec(ItemType.REDSTONE, 4, 9, 5),
            new StrongholdLootSpec(ItemType.COAL, 3, 8, 10),
            new StrongholdLootSpec(ItemType.BREAD, 1, 3, 15),
            new StrongholdLootSpec(ItemType.APPLE, 1, 3, 15),
            new StrongholdLootSpec(ItemType.IRON_PICKAXE, 1, 1, 1)
    };

    private static final StrongholdLootSpec[] VILLAGE_BLACKSMITH_LOOT = {
            new StrongholdLootSpec(ItemType.DIAMOND, 1, 3, 3),
            new StrongholdLootSpec(ItemType.IRON_INGOT, 1, 5, 10),
            new StrongholdLootSpec(ItemType.GOLD_INGOT, 1, 3, 5),
            new StrongholdLootSpec(ItemType.BREAD, 1, 3, 15),
            new StrongholdLootSpec(ItemType.APPLE, 1, 3, 15),
            new StrongholdLootSpec(ItemType.IRON_PICKAXE, 1, 1, 5),
            new StrongholdLootSpec(ItemType.IRON_SWORD, 1, 1, 5),
            new StrongholdLootSpec(ItemType.IRON_CHESTPLATE, 1, 1, 5),
            new StrongholdLootSpec(ItemType.IRON_HELMET, 1, 1, 5),
            new StrongholdLootSpec(ItemType.IRON_LEGGINGS, 1, 1, 5),
            new StrongholdLootSpec(ItemType.IRON_BOOTS, 1, 1, 5),
            new StrongholdLootSpec(ItemType.OBSIDIAN, 3, 7, 5),
            new StrongholdLootSpec(ItemType.SAPLING, 3, 7, 5)
    };

    private record StrongholdLootSpec(ItemType type, int minCount, int maxCount, int weight) {
    }

    private static StructureBoundingBox villagePathBox(java.util.Random random, int x, int y, int z, int mode) {
        int length = 7 * randomIntegerInRange(random, 3, 5);
        return componentToAddBoundingBox(x, y, z, 0, 0, 0, 3, 3, length, mode);
    }

    private static java.util.Random villageStartRandom(long seed, int chunkX, int chunkZ) {
        java.util.Random random = new java.util.Random(seed);
        long xSeed = random.nextLong();
        long zSeed = random.nextLong();
        random.setSeed((long) chunkX * xSeed ^ (long) chunkZ * zSeed ^ seed);
        random.nextInt();
        return random;
    }

    private static void consumeVillagePieceWeightRandoms(java.util.Random random) {
        randomIntegerInRange(random, 2, 4);
        randomIntegerInRange(random, 0, 1);
        randomIntegerInRange(random, 0, 2);
        randomIntegerInRange(random, 2, 5);
        randomIntegerInRange(random, 0, 2);
        randomIntegerInRange(random, 1, 4);
        randomIntegerInRange(random, 2, 4);
        randomIntegerInRange(random, 0, 1);
        randomIntegerInRange(random, 0, 3);
    }

    private static int randomIntegerInRange(java.util.Random random, int min, int max) {
        return min >= max ? min : random.nextInt(max - min + 1) + min;
    }

    private static StructureBoundingBox componentToAddBoundingBox(int x, int y, int z, int offX, int offY, int offZ,
            int sizeX, int sizeY, int sizeZ, int mode) {
        return switch (mode) {
            case 2 -> new StructureBoundingBox(x + offX, y + offY, z - sizeZ + 1 + offZ,
                    x + sizeX - 1 + offX, y + sizeY - 1 + offY, z + offZ);
            case 1 -> new StructureBoundingBox(x - sizeZ + 1 + offZ, y + offY, z + offX,
                    x + offZ, y + sizeY - 1 + offY, z + sizeX - 1 + offX);
            case 3 -> new StructureBoundingBox(x + offZ, y + offY, z + offX,
                    x + sizeZ - 1 + offZ, y + sizeY - 1 + offY, z + sizeX - 1 + offX);
            default -> new StructureBoundingBox(x + offX, y + offY, z + offZ,
                    x + sizeX - 1 + offX, y + sizeY - 1 + offY, z + sizeZ - 1 + offZ);
        };
    }

    private static boolean hasReleaseOneStrongholdBiomeInGenerationChunk(ReleaseOneWorldGenerator generator,
            int chunkX, int chunkZ) {
        for (int layerX = chunkX << 2; layerX < (chunkX << 2) + 4; layerX++) {
            for (int layerZ = chunkZ << 2; layerZ < (chunkZ << 2) + 4; layerZ++) {
                if (isReleaseOneStrongholdBiome(generator.getBiomeForGenerationLayer(layerX, layerZ))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isReleaseOneStrongholdBiome(BiomeType biome) {
        if (biome == null) {
            return false;
        }
        return switch (biome) {
            case DESERT, FOREST, EXTREME_HILLS, SWAMPLAND, TAIGA,
                    ICE_PLAINS, ICE_MOUNTAINS -> true;
            default -> false;
        };
    }

    private static int count(Chunk chunk, BlockType type) {
        int count = 0;
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    if (chunk.getBlock(x, y, z) == type) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static int count(Chunk chunk, BlockType type, int metadata) {
        int count = 0;
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    if (chunk.getBlock(x, y, z) == type && chunk.getBlockMetadata(x, y, z) == metadata) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static BlockType worldBlock(Chunk chunk, int chunkX, int chunkZ, int worldX, int y, int worldZ) {
        assertEquals(chunkX, Math.floorDiv(worldX, Chunk.WIDTH), "Test coordinate should be inside generated chunk");
        assertEquals(chunkZ, Math.floorDiv(worldZ, Chunk.DEPTH), "Test coordinate should be inside generated chunk");
        return chunk.getBlock(Math.floorMod(worldX, Chunk.WIDTH), y, Math.floorMod(worldZ, Chunk.DEPTH));
    }

    private static int worldMetadata(Chunk chunk, int chunkX, int chunkZ, int worldX, int y, int worldZ) {
        assertEquals(chunkX, Math.floorDiv(worldX, Chunk.WIDTH), "Test coordinate should be inside generated chunk");
        assertEquals(chunkZ, Math.floorDiv(worldZ, Chunk.DEPTH), "Test coordinate should be inside generated chunk");
        return chunk.getBlockMetadata(Math.floorMod(worldX, Chunk.WIDTH), y, Math.floorMod(worldZ, Chunk.DEPTH));
    }

    private static void clearStructureLiquidEnvelope(Chunk chunk, int chunkX, int chunkZ,
            StructureBoundingBox bounds) {
        int chunkMinX = chunkX * Chunk.WIDTH;
        int chunkMinZ = chunkZ * Chunk.DEPTH;
        int minX = Math.max(bounds.minX() - 1, chunkMinX);
        int minY = Math.max(bounds.minY() - 1, 0);
        int minZ = Math.max(bounds.minZ() - 1, chunkMinZ);
        int maxX = Math.min(bounds.maxX() + 1, chunkMinX + Chunk.WIDTH - 1);
        int maxY = Math.min(bounds.maxY() + 1, Chunk.HEIGHT - 1);
        int maxZ = Math.min(bounds.maxZ() + 1, chunkMinZ + Chunk.DEPTH - 1);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    int localX = x - chunkMinX;
                    int localZ = z - chunkMinZ;
                    if (isStructureLiquid(chunk.getBlock(localX, y, localZ))) {
                        chunk.setBlock(localX, y, localZ, BlockType.STONE, 0);
                    }
                }
            }
        }
    }

    private static boolean isStructureLiquid(BlockType type) {
        return type == BlockType.WATER || type == BlockType.FLOWING_WATER
                || type == BlockType.LAVA || type == BlockType.FLOWING_LAVA;
    }

    private static int width(StructureBoundingBox box) {
        return box.maxX() - box.minX() + 1;
    }

    private static int height(StructureBoundingBox box) {
        return box.maxY() - box.minY() + 1;
    }

    private static int depth(StructureBoundingBox box) {
        return box.maxZ() - box.minZ() + 1;
    }

    private static int expectedGeneratedStrongholdRootMode(long seed, int chunkX, int chunkZ) {
        Random random = expectedGeneratedStrongholdRandom(seed, chunkX, chunkZ);
        return random.nextInt(4);
    }

    private static Random expectedGeneratedStrongholdRandomAfterRoot(long seed, int chunkX, int chunkZ) {
        Random random = expectedGeneratedStrongholdRandom(seed, chunkX, chunkZ);
        random.nextInt(4);
        return random;
    }

    private static Random expectedGeneratedStrongholdRandom(long seed, int chunkX, int chunkZ) {
        Random random = new Random(seed);
        long xSeed = random.nextLong();
        long zSeed = random.nextLong();
        random.setSeed((long) chunkX * xSeed ^ (long) chunkZ * zSeed ^ seed);
        random.nextInt();
        return random;
    }

    private static String expectedSourceStrongholdDoor(Random random) {
        return switch (random.nextInt(5)) {
            case 2 -> "WOOD_DOOR";
            case 3 -> "GRATES";
            case 4 -> "IRON_DOOR";
            default -> "OPENING";
        };
    }

    private record ExpectedStrongholdPiece(StructureBoundingBox box, int mode) {
    }

    private record ExpectedStrongholdAccess(int x, int y, int z, int mode) {
    }

    private static StructureBoundingBox expectedGeneratedStrongholdFirstCrossingBox(int chunkX, int chunkZ,
            int rootMode) {
        return expectedGeneratedStrongholdFirstCrossingPiece(chunkX, chunkZ, rootMode).box();
    }

    private static ExpectedStrongholdPiece expectedGeneratedStrongholdFirstCrossingPiece(int chunkX, int chunkZ,
            int rootMode) {
        StructureBoundingBox root = new StructureBoundingBox((chunkX << 4) + 2, 64, (chunkZ << 4) + 2,
                (chunkX << 4) + 6, 74, (chunkZ << 4) + 6);
        ExpectedStrongholdAccess access = expectedStrongholdNormalAccess(root, rootMode, 1, 1);
        return expectedStrongholdPiece(access, -4, -3, 0, 10, 9, 11);
    }

    private static ExpectedStrongholdPiece expectedStrongholdPiece(ExpectedStrongholdAccess access, int offX,
            int offY, int offZ, int sizeX, int sizeY, int sizeZ) {
        StructureBoundingBox box = expectedComponentToAddBoundingBox(access.x(), access.y(), access.z(),
                offX, offY, offZ, sizeX, sizeY, sizeZ, access.mode());
        return new ExpectedStrongholdPiece(box, access.mode());
    }

    private static int[] expectedStrongholdNormalAccess(int minX, int minY, int minZ, int maxX, int maxZ,
            int mode, int xOffset, int yOffset) {
        int y = minY + yOffset;
        return switch (mode) {
            case 2 -> new int[] { minX + xOffset, y, minZ - 1 };
            case 1 -> new int[] { minX - 1, y, minZ + xOffset };
            case 3 -> new int[] { maxX + 1, y, minZ + xOffset };
            default -> new int[] { minX + xOffset, y, maxZ + 1 };
        };
    }

    private static ExpectedStrongholdAccess expectedStrongholdNormalAccess(ExpectedStrongholdPiece piece,
            int xOffset, int yOffset) {
        return expectedStrongholdNormalAccess(piece.box(), piece.mode(), xOffset, yOffset);
    }

    private static ExpectedStrongholdAccess expectedStrongholdNormalAccess(StructureBoundingBox box, int mode,
            int xOffset, int yOffset) {
        int y = box.minY() + yOffset;
        return switch (mode) {
            case 2 -> new ExpectedStrongholdAccess(box.minX() + xOffset, y, box.minZ() - 1, mode);
            case 1 -> new ExpectedStrongholdAccess(box.minX() - 1, y, box.minZ() + xOffset, mode);
            case 3 -> new ExpectedStrongholdAccess(box.maxX() + 1, y, box.minZ() + xOffset, mode);
            default -> new ExpectedStrongholdAccess(box.minX() + xOffset, y, box.maxZ() + 1, mode);
        };
    }

    private static ExpectedStrongholdAccess expectedStrongholdXAccess(ExpectedStrongholdPiece piece, int yOffset,
            int zOffset) {
        StructureBoundingBox box = piece.box();
        int y = box.minY() + yOffset;
        return switch (piece.mode()) {
            case 1, 3 -> new ExpectedStrongholdAccess(box.minX() + zOffset, y, box.minZ() - 1, 2);
            default -> new ExpectedStrongholdAccess(box.minX() - 1, y, box.minZ() + zOffset, 1);
        };
    }

    private static ExpectedStrongholdAccess expectedStrongholdZAccess(ExpectedStrongholdPiece piece, int yOffset,
            int xOffset) {
        StructureBoundingBox box = piece.box();
        int y = box.minY() + yOffset;
        return switch (piece.mode()) {
            case 1, 3 -> new ExpectedStrongholdAccess(box.minX() + xOffset, y, box.maxZ() + 1, 0);
            default -> new ExpectedStrongholdAccess(box.maxX() + 1, y, box.minZ() + xOffset, 3);
        };
    }

    private static StructureBoundingBox expectedComponentToAddBoundingBox(int x, int y, int z, int offX, int offY,
            int offZ, int sizeX, int sizeY, int sizeZ, int mode) {
        return switch (mode) {
            case 2 -> new StructureBoundingBox(x + offX, y + offY, z - sizeZ + 1 + offZ,
                    x + sizeX - 1 + offX, y + sizeY - 1 + offY, z + offZ);
            case 1 -> new StructureBoundingBox(x - sizeZ + 1 + offZ, y + offY, z + offX,
                    x + offZ, y + sizeY - 1 + offY, z + sizeX - 1 + offX);
            case 3 -> new StructureBoundingBox(x + offZ, y + offY, z + offX,
                    x + sizeZ - 1 + offZ, y + sizeY - 1 + offY, z + sizeX - 1 + offX);
            default -> new StructureBoundingBox(x + offX, y + offY, z + offZ,
                    x + sizeX - 1 + offX, y + sizeY - 1 + offY, z + sizeZ - 1 + offZ);
        };
    }

    private static StructureBoundingBox expectedGeneratedStrongholdBox(long seed,
            StructureGenerator.StructureLocation location, int minX, int minZ, int maxX, int maxZ) {
        int rootX = (location.chunkX() << 4) + 2;
        int rootZ = (location.chunkZ() << 4) + 2;
        int rootMode = expectedGeneratedStrongholdRootMode(seed, location.chunkX(), location.chunkZ());
        int[][] corners = {
                rotateStrongholdRelative(minX, minZ, rootMode),
                rotateStrongholdRelative(minX, maxZ, rootMode),
                rotateStrongholdRelative(maxX, minZ, rootMode),
                rotateStrongholdRelative(maxX, maxZ, rootMode)
        };
        int rotatedMinX = Integer.MAX_VALUE;
        int rotatedMinZ = Integer.MAX_VALUE;
        int rotatedMaxX = Integer.MIN_VALUE;
        int rotatedMaxZ = Integer.MIN_VALUE;
        for (int[] corner : corners) {
            rotatedMinX = Math.min(rotatedMinX, corner[0]);
            rotatedMinZ = Math.min(rotatedMinZ, corner[1]);
            rotatedMaxX = Math.max(rotatedMaxX, corner[0]);
            rotatedMaxZ = Math.max(rotatedMaxZ, corner[1]);
        }
        return new StructureBoundingBox(rootX + rotatedMinX, 0, rootZ + rotatedMinZ,
                rootX + rotatedMaxX, 0, rootZ + rotatedMaxZ);
    }

    private static int[] rotateStrongholdRelative(int x, int z, int rootMode) {
        return switch (rootMode) {
            case 0 -> new int[] { 4 - z, x };
            case 1 -> new int[] { 4 - x, 4 - z };
            case 2 -> new int[] { z, 4 - x };
            default -> new int[] { x, z };
        };
    }

    private static int rotateStrongholdMode(int baseMode, int rootMode) {
        int[] vector = switch (baseMode) {
            case 0 -> new int[] { 0, 1 };
            case 1 -> new int[] { -1, 0 };
            case 2 -> new int[] { 0, -1 };
            default -> new int[] { 1, 0 };
        };
        int[] rotated = switch (rootMode) {
            case 0 -> new int[] { -vector[1], vector[0] };
            case 1 -> new int[] { -vector[0], -vector[1] };
            case 2 -> new int[] { vector[1], -vector[0] };
            default -> vector;
        };
        if (rotated[0] > 0) {
            return 3;
        }
        if (rotated[0] < 0) {
            return 1;
        }
        return rotated[1] > 0 ? 0 : 2;
    }

    private static StructureStart netherFortressStart(long seed, StructureGenerator.StructureLocation location,
            ReleaseOneWorldGenerator generator) {
        StructurePlanner planner = new StructurePlanner();
        return planner.startsForChunk(seed, Dimension.NETHER, location.chunkX(), location.chunkZ(), generator).stream()
                .filter(candidate -> candidate.type() == StructureType.NETHER_FORTRESS)
                .filter(candidate -> candidate.chunkX() == location.chunkX() && candidate.chunkZ() == location.chunkZ())
                .findFirst()
                .orElseThrow();
    }

    private static StructureStart strongholdStart(StructurePlanner planner, long seed, int chunkX, int chunkZ,
            ReleaseOneWorldGenerator generator) {
        return planner.startsForChunk(seed, Dimension.OVERWORLD, chunkX, chunkZ, generator).stream()
                .filter(candidate -> candidate.type() == StructureType.STRONGHOLD)
                .filter(candidate -> candidate.chunkX() == chunkX && candidate.chunkZ() == chunkZ)
                .findFirst()
                .orElseThrow();
    }

    private static StructureStart generatedStrongholdStart(StructurePlanner planner, long seed, int chunkX,
            int chunkZ) throws Exception {
        java.lang.reflect.Method method = StructurePlanner.class.getDeclaredMethod("buildStronghold",
                long.class, int.class, int.class);
        method.setAccessible(true);
        return (StructureStart) method.invoke(planner, seed, chunkX, chunkZ);
    }

    private static StructurePiece findGeneratedStrongholdRoomAcrossSeeds(String room, int maxSeeds) throws Exception {
        StructurePlanner planner = new StructurePlanner();
        for (long seed = 0L; seed < maxSeeds; seed++) {
            StructureStart start = generatedStrongholdStart(planner, seed, 0, 0);
            StructurePiece piece = start.pieces().stream()
                    .filter(candidate -> room.equals(strongholdRoomName(candidate)))
                    .findFirst()
                    .orElse(null);
            if (piece != null) {
                return piece;
            }
        }
        fail("Expected at least one generated stronghold to contain room " + room);
        return null;
    }

    private static StructureStart mineshaftStart(long seed, int chunkX, int chunkZ,
            ReleaseOneWorldGenerator generator) {
        StructurePlanner planner = new StructurePlanner();
        return planner.startsForChunk(seed, Dimension.OVERWORLD, chunkX, chunkZ, generator).stream()
                .filter(candidate -> candidate.type() == StructureType.MINESHAFT)
                .filter(candidate -> candidate.chunkX() == chunkX && candidate.chunkZ() == chunkZ)
                .findFirst()
                .orElseThrow();
    }

    private static StructureStart villageStart(long seed, int chunkX, int chunkZ,
            ReleaseOneWorldGenerator generator) {
        StructurePlanner planner = new StructurePlanner();
        return planner.startsForChunk(seed, Dimension.OVERWORLD, chunkX, chunkZ, generator).stream()
                .filter(candidate -> candidate.type() == StructureType.VILLAGE)
                .filter(candidate -> candidate.chunkX() == chunkX && candidate.chunkZ() == chunkZ)
                .findFirst()
                .orElseThrow();
    }

    private static BiomeType villageBiome(long seed, int chunkX, int chunkZ,
            ReleaseOneWorldGenerator generator) {
        try {
            java.lang.reflect.Method method = StructurePlanner.class.getDeclaredMethod("villageBiome",
                    long.class, int.class, int.class, ReleaseOneWorldGenerator.class);
            method.setAccessible(true);
            return (BiomeType) method.invoke(new StructurePlanner(), seed, chunkX, chunkZ, generator);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static void assertLocatedStructureMatchesStart(StructureGenerator.StructureLocation location,
            StructureStart expected) {
        assertNotNull(location);
        assertEquals(expected.type(), location.type());
        assertEquals(expected.chunkX(), location.chunkX());
        assertEquals(expected.chunkZ(), location.chunkZ());
        StructureBoundingBox bounds = expected.bounds();
        assertEquals(bounds.centerX(), location.blockX());
        assertEquals(bounds.centerY(), location.blockY());
        assertEquals(bounds.centerZ(), location.blockZ());
    }

    private static StructurePiece fallbackStrongholdCorridor(java.util.List<StructurePiece> pieces, int x, int y,
            int z, int mode) throws Exception {
        Class<?> accessClass = Class.forName("com.craftzero.world.StructurePlanner$StrongholdAccessPoint");
        Object access = strongholdAccessPoint(accessClass, x, y, z, mode);
        java.lang.reflect.Method method = StructurePlanner.class.getDeclaredMethod("createFallbackStrongholdCorridor",
                java.util.List.class, accessClass);
        method.setAccessible(true);
        return (StructurePiece) method.invoke(null, pieces, access);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static StructurePiece generatedStrongholdComponent(String room, java.util.List<StructurePiece> pieces,
            int x, int y, int z, int mode, int depth) throws Exception {
        return generatedStrongholdComponent(room, pieces, new Random(13579L), x, y, z, mode, depth);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static StructurePiece generatedStrongholdComponent(String room, java.util.List<StructurePiece> pieces,
            Random random, int x, int y, int z, int mode, int depth) throws Exception {
        Class<?> roomClass = Class.forName("com.craftzero.world.StructurePlanner$StrongholdRoom");
        Object roomValue = Enum.valueOf((Class<Enum>) roomClass.asSubclass(Enum.class), room);
        Class<?> accessClass = Class.forName("com.craftzero.world.StructurePlanner$StrongholdAccessPoint");
        Object access = strongholdAccessPoint(accessClass, x, y, z, mode);
        java.lang.reflect.Method method = StructurePlanner.class.getDeclaredMethod("createGeneratedStrongholdComponent",
                roomClass, java.util.List.class, Random.class, accessClass, int.class);
        method.setAccessible(true);
        return (StructurePiece) method.invoke(null, roomValue, pieces, random, access, depth);
    }

    private static void addStrongholdRoomCopies(java.util.List<StructurePiece> pieces, String room, int count)
            throws Exception {
        for (int i = 0; i < count; i++) {
            pieces.add(strongholdPiece(room, i * 16, 40, 0, i * 16 + 4, 44, 4, 0));
        }
    }

    private static int strongholdWeightedTotal(java.util.List<StructurePiece> pieces) throws Exception {
        Class<?> contextClass = Class.forName("com.craftzero.world.StructurePlanner$StrongholdGenerationContext");
        java.lang.reflect.Constructor<?> constructor = contextClass.getDeclaredConstructor(
                StructureBoundingBox.class, java.util.List.class);
        constructor.setAccessible(true);
        Object context = constructor.newInstance(new StructureBoundingBox(0, 40, 0, 4, 50, 4), pieces);

        java.lang.reflect.Field weightsField = contextClass.getDeclaredField("weights");
        weightsField.setAccessible(true);
        Object weights = weightsField.get(context);

        java.lang.reflect.Method method = StructurePlanner.class.getDeclaredMethod("totalStrongholdWeight",
                java.util.List.class);
        method.setAccessible(true);
        return (int) method.invoke(null, weights);
    }

    private static StructurePiece chooseStrongholdWeightedComponentAfterPrevious(String previousRoom, Random random,
            int x, int y, int z, int mode, int depth) throws Exception {
        java.util.ArrayList<StructurePiece> pieces = new java.util.ArrayList<>();
        Class<?> contextClass = Class.forName("com.craftzero.world.StructurePlanner$StrongholdGenerationContext");
        java.lang.reflect.Constructor<?> constructor = contextClass.getDeclaredConstructor(
                StructureBoundingBox.class, java.util.List.class);
        constructor.setAccessible(true);
        Object context = constructor.newInstance(new StructureBoundingBox(0, 40, 0, 4, 50, 4), pieces);

        java.lang.reflect.Field weightsField = contextClass.getDeclaredField("weights");
        weightsField.setAccessible(true);
        java.util.List<?> weights = (java.util.List<?>) weightsField.get(context);
        Object previousWeight = strongholdWeightForRoom(weights, previousRoom);

        java.lang.reflect.Field previousWeightField = contextClass.getDeclaredField("previousWeight");
        previousWeightField.setAccessible(true);
        previousWeightField.set(context, previousWeight);

        Class<?> accessClass = Class.forName("com.craftzero.world.StructurePlanner$StrongholdAccessPoint");
        Object access = strongholdAccessPoint(accessClass, x, y, z, mode);
        java.lang.reflect.Method method = StructurePlanner.class.getDeclaredMethod("chooseGeneratedStrongholdComponent",
                contextClass, java.util.List.class, Random.class, accessClass, int.class);
        method.setAccessible(true);
        return (StructurePiece) method.invoke(null, context, pieces, random, access, depth);
    }

    private static StructurePiece chooseStrongholdWeightedComponent(java.util.List<StructurePiece> pieces,
            Random random, int x, int y, int z, int mode, int depth) throws Exception {
        Class<?> contextClass = Class.forName("com.craftzero.world.StructurePlanner$StrongholdGenerationContext");
        java.lang.reflect.Constructor<?> constructor = contextClass.getDeclaredConstructor(
                StructureBoundingBox.class, java.util.List.class);
        constructor.setAccessible(true);
        Object context = constructor.newInstance(new StructureBoundingBox(0, 40, 0, 4, 50, 4), pieces);

        Class<?> accessClass = Class.forName("com.craftzero.world.StructurePlanner$StrongholdAccessPoint");
        Object access = strongholdAccessPoint(accessClass, x, y, z, mode);
        java.lang.reflect.Method method = StructurePlanner.class.getDeclaredMethod("chooseGeneratedStrongholdComponent",
                contextClass, java.util.List.class, Random.class, accessClass, int.class);
        method.setAccessible(true);
        return (StructurePiece) method.invoke(null, context, pieces, random, access, depth);
    }

    private static StructurePiece chooseFortressWeightedComponent(java.util.List<StructurePiece> pieces,
            boolean secondary, Random random, int x, int y, int z, int mode, int depth) throws Exception {
        Class<?> contextClass = Class.forName("com.craftzero.world.StructurePlanner$FortressGenerationContext");
        java.lang.reflect.Constructor<?> constructor = contextClass.getDeclaredConstructor(StructureBoundingBox.class);
        constructor.setAccessible(true);
        Object context = constructor.newInstance(new StructureBoundingBox(0, 64, 0, 18, 73, 18));

        java.lang.reflect.Field weightsField = contextClass.getDeclaredField(
                secondary ? "secondaryWeights" : "primaryWeights");
        weightsField.setAccessible(true);
        java.util.List<?> weights = (java.util.List<?>) weightsField.get(context);

        java.lang.reflect.Method method = StructurePlanner.class.getDeclaredMethod("chooseFortressComponent",
                contextClass, java.util.List.class, java.util.List.class, Random.class,
                int.class, int.class, int.class, int.class, int.class);
        method.setAccessible(true);
        return (StructurePiece) method.invoke(null, context, weights, pieces, random, x, y, z, mode, depth);
    }

    private static void addNextFortressComponent(StructurePiece parent, java.util.List<StructurePiece> pieces,
            Random random, int x, int y, int z, int mode, boolean secondary) throws Exception {
        Class<?> contextClass = Class.forName("com.craftzero.world.StructurePlanner$FortressGenerationContext");
        java.lang.reflect.Constructor<?> constructor = contextClass.getDeclaredConstructor(StructureBoundingBox.class);
        constructor.setAccessible(true);
        Object context = constructor.newInstance(new StructureBoundingBox(0, 64, 0, 18, 73, 18));

        java.lang.reflect.Method method = StructurePlanner.class.getDeclaredMethod("getNextFortressComponent",
                parent.getClass(), contextClass, java.util.List.class, Random.class,
                int.class, int.class, int.class, int.class, boolean.class);
        method.setAccessible(true);
        method.invoke(null, parent, context, pieces, random, x, y, z, mode, secondary);
    }

    private static int generatedStrongholdChildCountFromParentDepth(int parentDepth) throws Exception {
        Class<?> contextClass = Class.forName("com.craftzero.world.StructurePlanner$StrongholdGenerationContext");
        java.lang.reflect.Constructor<?> constructor = contextClass.getDeclaredConstructor(
                StructureBoundingBox.class, java.util.List.class);
        constructor.setAccessible(true);
        java.util.ArrayList<StructurePiece> pieces = new java.util.ArrayList<>();
        java.util.ArrayList<Object> pending = new java.util.ArrayList<>();
        Object context = constructor.newInstance(new StructureBoundingBox(0, 40, 0, 4, 50, 4), pieces);

        Class<?> accessClass = Class.forName("com.craftzero.world.StructurePlanner$StrongholdAccessPoint");
        Object access = strongholdAccessPoint(accessClass, 2, 40, 0, 0);
        java.lang.reflect.Method method = StructurePlanner.class.getDeclaredMethod("getNextGeneratedStrongholdComponent",
                contextClass, java.util.List.class, java.util.List.class, Random.class, accessClass, int.class);
        method.setAccessible(true);
        method.invoke(null, context, pieces, pending, new FixedNextIntRandom(0), access, parentDepth);
        return pieces.size();
    }

    private static Object strongholdWeightForRoom(java.util.List<?> weights, String roomName) throws Exception {
        for (Object weight : weights) {
            java.lang.reflect.Field roomField = weight.getClass().getDeclaredField("room");
            roomField.setAccessible(true);
            if (roomName.equals(roomField.get(weight).toString())) {
                return weight;
            }
        }
        throw new AssertionError("Missing stronghold weight for " + roomName);
    }

    private static Object strongholdAccessPoint(Class<?> accessClass, int x, int y, int z, int mode)
            throws Exception {
        java.lang.reflect.Constructor<?> constructor = accessClass.getDeclaredConstructor(int.class, int.class,
                int.class, int.class);
        constructor.setAccessible(true);
        return constructor.newInstance(x, y, z, mode);
    }

    private static StructurePiece findFortressRoom(StructureStart start, String room) {
        return start.pieces().stream()
                .filter(piece -> room.equals(fortressRoomName(piece)))
                .findFirst()
                .orElseThrow();
    }

    private static boolean generatedRoomContains(StructureGenerator structures, long seed,
            ReleaseOneWorldGenerator generator, StructureBoundingBox room, BlockType type) {
        for (int cx = Math.floorDiv(room.minX(), Chunk.WIDTH); cx <= Math.floorDiv(room.maxX(), Chunk.WIDTH); cx++) {
            for (int cz = Math.floorDiv(room.minZ(), Chunk.DEPTH); cz <= Math.floorDiv(room.maxZ(), Chunk.DEPTH);
                    cz++) {
                Chunk chunk = new Chunk(cx, cz);
                structures.generate(null, chunk, seed, cx, cz, Dimension.NETHER, generator);
                if (contains(chunk, type)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean generatedStrongholdNeighborhoodContains(StructureGenerator structures, World world,
            long seed, int centerChunkX, int centerChunkZ, ReleaseOneWorldGenerator generator, BlockType type) {
        for (int cx = centerChunkX - 5; cx <= centerChunkX + 5; cx++) {
            for (int cz = centerChunkZ - 5; cz <= centerChunkZ + 5; cz++) {
                Chunk chunk = new Chunk(cx, cz);
                structures.generate(world, chunk, seed, cx, cz, Dimension.OVERWORLD, generator);
                if (contains(chunk, type)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String fortressRoomName(StructurePiece piece) {
        try {
            java.lang.reflect.Field field = piece.getClass().getDeclaredField("room");
            field.setAccessible(true);
            return field.get(piece).toString();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static int fortressComponentType(StructurePiece piece) {
        try {
            java.lang.reflect.Field field = piece.getClass().getDeclaredField("componentType");
            field.setAccessible(true);
            return field.getInt(piece);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static String strongholdRoomName(StructurePiece piece) {
        try {
            java.lang.reflect.Field field = piece.getClass().getDeclaredField("room");
            field.setAccessible(true);
            return field.get(piece).toString();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static int strongholdCoordBaseMode(StructurePiece piece) {
        try {
            java.lang.reflect.Field field = piece.getClass().getDeclaredField("coordBaseMode");
            field.setAccessible(true);
            return field.getInt(piece);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static String strongholdDoorName(StructurePiece piece) {
        try {
            java.lang.reflect.Field field = piece.getClass().getDeclaredField("doorType");
            field.setAccessible(true);
            return field.get(piece).toString();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static boolean strongholdCountsForWeight(StructurePiece piece) {
        try {
            java.lang.reflect.Field field = piece.getClass().getDeclaredField("countsForWeight");
            field.setAccessible(true);
            return field.getBoolean(piece);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static boolean strongholdBoolean(StructurePiece piece, String fieldName) {
        try {
            java.lang.reflect.Field field = piece.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.getBoolean(piece);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static StructurePiece fortressPiece(String room, int minX, int minY, int minZ, int maxX, int maxY,
            int maxZ, int coordBaseMode) throws Exception {
        return fortressPiece(room, minX, minY, minZ, maxX, maxY, maxZ, coordBaseMode, 0);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static StructurePiece fortressPiece(String room, int minX, int minY, int minZ, int maxX, int maxY,
            int maxZ, int coordBaseMode, int componentType) throws Exception {
        Class<?> roomClass = Class.forName("com.craftzero.world.StructurePlanner$FortressRoom");
        Object roomValue = Enum.valueOf((Class<Enum>) roomClass.asSubclass(Enum.class), room);
        Class<?> pieceClass = Class.forName("com.craftzero.world.StructurePlanner$NetherFortressPiece");
        java.lang.reflect.Constructor<?> constructor = pieceClass.getDeclaredConstructor(int.class, int.class,
                int.class, int.class, int.class, int.class, roomClass, int.class, int.class);
        constructor.setAccessible(true);
        return (StructurePiece) constructor.newInstance(minX, minY, minZ, maxX, maxY, maxZ, roomValue,
                coordBaseMode, componentType);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static StructurePiece strongholdPiece(String room, int minX, int minY, int minZ, int maxX, int maxY,
            int maxZ, int coordBaseMode) throws Exception {
        Class<?> roomClass = Class.forName("com.craftzero.world.StructurePlanner$StrongholdRoom");
        Object roomValue = Enum.valueOf((Class<Enum>) roomClass.asSubclass(Enum.class), room);
        Class<?> pieceClass = Class.forName("com.craftzero.world.StructurePlanner$StrongholdBoxPiece");
        java.lang.reflect.Constructor<?> constructor = pieceClass.getDeclaredConstructor(int.class, int.class,
                int.class, int.class, int.class, int.class, roomClass, int.class);
        constructor.setAccessible(true);
        return (StructurePiece) constructor.newInstance(minX, minY, minZ, maxX, maxY, maxZ, roomValue,
                coordBaseMode);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static StructurePiece strongholdPieceWithDoor(String room, int minX, int minY, int minZ, int maxX,
            int maxY, int maxZ, int coordBaseMode, String door) throws Exception {
        Class<?> roomClass = Class.forName("com.craftzero.world.StructurePlanner$StrongholdRoom");
        Class<?> doorClass = Class.forName("com.craftzero.world.StructurePlanner$StrongholdDoor");
        Object roomValue = Enum.valueOf((Class<Enum>) roomClass.asSubclass(Enum.class), room);
        Object doorValue = Enum.valueOf((Class<Enum>) doorClass.asSubclass(Enum.class), door);
        Class<?> pieceClass = Class.forName("com.craftzero.world.StructurePlanner$StrongholdBoxPiece");
        java.lang.reflect.Constructor<?> constructor = pieceClass.getDeclaredConstructor(int.class, int.class,
                int.class, int.class, int.class, int.class, roomClass, int.class, doorClass);
        constructor.setAccessible(true);
        return (StructurePiece) constructor.newInstance(minX, minY, minZ, maxX, maxY, maxZ, roomValue,
                coordBaseMode, doorValue);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static StructurePiece strongholdStraightPiece(int minX, int minY, int minZ, int maxX, int maxY,
            int maxZ, int coordBaseMode, boolean expandsX, boolean expandsZ) throws Exception {
        Class<?> roomClass = Class.forName("com.craftzero.world.StructurePlanner$StrongholdRoom");
        Object roomValue = Enum.valueOf((Class<Enum>) roomClass.asSubclass(Enum.class), "STRAIGHT");
        Class<?> pieceClass = Class.forName("com.craftzero.world.StructurePlanner$StrongholdBoxPiece");
        java.lang.reflect.Constructor<?> constructor = pieceClass.getDeclaredConstructor(int.class, int.class,
                int.class, int.class, int.class, int.class, roomClass, int.class, boolean.class, boolean.class);
        constructor.setAccessible(true);
        return (StructurePiece) constructor.newInstance(minX, minY, minZ, maxX, maxY, maxZ, roomValue,
                coordBaseMode, expandsX, expandsZ);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static StructurePiece strongholdCrossingHallPiece(int minX, int minY, int minZ, int maxX, int maxY,
            int maxZ, int coordBaseMode, boolean lowerLeft, boolean upperLeft, boolean lowerRight,
            boolean upperRight) throws Exception {
        Class<?> roomClass = Class.forName("com.craftzero.world.StructurePlanner$StrongholdRoom");
        Object roomValue = Enum.valueOf((Class<Enum>) roomClass.asSubclass(Enum.class), "CROSSING_HALL");
        Class<?> pieceClass = Class.forName("com.craftzero.world.StructurePlanner$StrongholdBoxPiece");
        java.lang.reflect.Constructor<?> constructor = pieceClass.getDeclaredConstructor(int.class, int.class,
                int.class, int.class, int.class, int.class, roomClass, int.class, boolean.class, boolean.class,
                boolean.class, boolean.class, boolean.class, boolean.class);
        constructor.setAccessible(true);
        return (StructurePiece) constructor.newInstance(minX, minY, minZ, maxX, maxY, maxZ, roomValue,
                coordBaseMode, false, false, lowerLeft, upperLeft, lowerRight, upperRight);
    }

    private static StructurePiece mineshaftPiece(int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
            boolean eastWest, int sections, boolean hasRails, boolean hasSpiders) throws Exception {
        Class<?> pieceClass = Class.forName("com.craftzero.world.StructurePlanner$MineshaftPiece");
        java.lang.reflect.Constructor<?> constructor = pieceClass.getDeclaredConstructor(int.class, int.class,
                int.class, int.class, int.class, int.class, boolean.class, int.class, boolean.class, boolean.class);
        constructor.setAccessible(true);
        return (StructurePiece) constructor.newInstance(minX, minY, minZ, maxX, maxY, maxZ,
                eastWest, sections, hasRails, hasSpiders);
    }

    private static StructurePiece mineshaftRoomPiece(StructureBoundingBox box) throws Exception {
        return mineshaftRoomPiece(box, java.util.List.of());
    }

    private static StructurePiece mineshaftRoomPiece(StructureBoundingBox box,
            java.util.List<StructureBoundingBox> openings) throws Exception {
        Class<?> pieceClass = Class.forName("com.craftzero.world.StructurePlanner$MineshaftRoomPiece");
        java.lang.reflect.Constructor<?> constructor = pieceClass.getDeclaredConstructor(StructureBoundingBox.class,
                java.util.List.class);
        constructor.setAccessible(true);
        return (StructurePiece) constructor.newInstance(box, openings);
    }

    private static StructurePiece mineshaftCrossPiece(StructureBoundingBox box, boolean multipleFloors)
            throws Exception {
        Class<?> pieceClass = Class.forName("com.craftzero.world.StructurePlanner$MineshaftCrossPiece");
        java.lang.reflect.Constructor<?> constructor = pieceClass.getDeclaredConstructor(StructureBoundingBox.class,
                boolean.class);
        constructor.setAccessible(true);
        return (StructurePiece) constructor.newInstance(box, multipleFloors);
    }

    private static StructurePiece mineshaftStairsPiece(StructureBoundingBox box, int mode) throws Exception {
        Class<?> pieceClass = Class.forName("com.craftzero.world.StructurePlanner$MineshaftStairsPiece");
        java.lang.reflect.Constructor<?> constructor = pieceClass.getDeclaredConstructor(StructureBoundingBox.class,
                int.class);
        constructor.setAccessible(true);
        return (StructurePiece) constructor.newInstance(box, mode);
    }

    private static Object mineshaftCorridorDescriptor(StructureBoundingBox box, int mode, int depth, int sections,
            boolean hasRails, boolean hasSpiders) throws Exception {
        Class<?> descriptorClass = Class.forName("com.craftzero.world.StructurePlanner$MineshaftDescriptor");
        java.lang.reflect.Method factory = descriptorClass.getDeclaredMethod("corridor",
                StructureBoundingBox.class, int.class, int.class, int.class, boolean.class, boolean.class);
        factory.setAccessible(true);
        return factory.invoke(null, box, mode, depth, sections, hasRails, hasSpiders);
    }

    private static StructureBoundingBox mineshaftDescriptorBounds(Object descriptor) throws Exception {
        java.lang.reflect.Method accessor = descriptor.getClass().getDeclaredMethod("bounds");
        accessor.setAccessible(true);
        return (StructureBoundingBox) accessor.invoke(descriptor);
    }

    private static int mineshaftDescriptorMode(Object descriptor) throws Exception {
        java.lang.reflect.Method accessor = descriptor.getClass().getDeclaredMethod("mode");
        accessor.setAccessible(true);
        return (int) accessor.invoke(descriptor);
    }

    private static Object villageGenerationContext(StructureBoundingBox startBox,
            java.util.List<?> weights) throws Exception {
        Class<?> contextClass = Class.forName("com.craftzero.world.StructurePlanner$VillageGenerationContext");
        java.lang.reflect.Constructor<?> constructor = contextClass.getDeclaredConstructor(
                StructureBoundingBox.class, java.util.List.class);
        constructor.setAccessible(true);
        return constructor.newInstance(startBox, weights);
    }

    @SuppressWarnings("unchecked")
    private static java.util.List<Object> villagePendingList(Object context, String fieldName) throws Exception {
        java.lang.reflect.Field field = context.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (java.util.List<Object>) field.get(context);
    }

    private static void processVillageQueues(Object context, StructureStart start, Random random,
            ReleaseOneWorldGenerator generator) throws Exception {
        java.lang.reflect.Method method = StructurePlanner.class.getDeclaredMethod("processVillageQueues",
                context.getClass(), StructureStart.class, Random.class, ReleaseOneWorldGenerator.class);
        method.setAccessible(true);
        method.invoke(null, context, start, random, generator);
    }

    private static StructurePiece addVillagePath(Object context, StructureStart start, Random random,
            ReleaseOneWorldGenerator generator, int x, int y, int z, int mode, int componentType)
            throws Exception {
        java.lang.reflect.Method method = StructurePlanner.class.getDeclaredMethod("addVillagePath",
                context.getClass(), StructureStart.class, Random.class, ReleaseOneWorldGenerator.class,
                int.class, int.class, int.class, int.class, int.class);
        method.setAccessible(true);
        return (StructurePiece) method.invoke(null, context, start, random, generator,
                x, y, z, mode, componentType);
    }

    private static StructurePiece chooseVillageComponent(Object context, StructureStart start, Random random,
            ReleaseOneWorldGenerator generator, int x, int y, int z, int mode, int componentType)
            throws Exception {
        java.lang.reflect.Method method = StructurePlanner.class.getDeclaredMethod("chooseVillageComponent",
                context.getClass(), StructureStart.class, Random.class, ReleaseOneWorldGenerator.class,
                int.class, int.class, int.class, int.class, int.class);
        method.setAccessible(true);
        return (StructurePiece) method.invoke(null, context, start, random, generator,
                x, y, z, mode, componentType);
    }

    private record ExpectedVillager(int profession, float x, float y, float z) {
    }

    private static final class FixedNextIntRandom extends Random {
        private final int value;

        private FixedNextIntRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            return Math.min(value, bound - 1);
        }
    }

    private static final class BoundRecordingRandom extends Random {
        private final java.util.ArrayList<Integer> bounds = new java.util.ArrayList<>();

        @Override
        public int nextInt(int bound) {
            bounds.add(bound);
            return 0;
        }

        private java.util.List<Integer> bounds() {
            return bounds;
        }
    }

    private static final class CountingRandom extends Random {
        private int nextIntCalls;
        private int nextBooleanCalls;

        @Override
        public int nextInt(int bound) {
            nextIntCalls++;
            return 0;
        }

        @Override
        public boolean nextBoolean() {
            nextBooleanCalls++;
            return false;
        }

        private int nextIntCalls() {
            return nextIntCalls;
        }

        private int nextBooleanCalls() {
            return nextBooleanCalls;
        }
    }

    private static final class StrongholdShellOrderRandom extends Random {
        private final int markedFloatCall;
        private int floatCalls;

        private StrongholdShellOrderRandom(int markedFloatCall) {
            this.markedFloatCall = markedFloatCall;
        }

        @Override
        public float nextFloat() {
            floatCalls++;
            return floatCalls == markedFloatCall ? 0.0f : 1.0f;
        }

        @Override
        public int nextInt(int bound) {
            return Math.max(0, bound - 1);
        }
    }

    private static final class MineshaftSupportArchRandom extends Random {
        @Override
        public float nextFloat() {
            return 1.0f;
        }

        @Override
        public int nextInt(int bound) {
            if (bound == 4) {
                return 0;
            }
            if (bound == 100) {
                return 1;
            }
            return 0;
        }
    }

    private static final class MineshaftTorchRandom extends Random {
        private int floatCalls;

        @Override
        public float nextFloat() {
            floatCalls++;
            return floatCalls == 24 || floatCalls == 25 ? 0.0f : 1.0f;
        }

        @Override
        public int nextInt(int bound) {
            if (bound == 4) {
                return 0;
            }
            if (bound == 100) {
                return 1;
            }
            return 0;
        }
    }

    private static void assertVillageVillagers(String description, StructurePiece piece,
            ExpectedVillager... expected) {
        World world = new World(1234567L);
        try {
            Chunk chunk = new Chunk(0, 0);
            piece.place(world, chunk, 1234567L, 0, 0);
            world.updateEntities(0.0f);

            java.util.List<Villager> villagers = world.getEntities().stream()
                    .filter(Villager.class::isInstance)
                    .map(Villager.class::cast)
                    .sorted(java.util.Comparator.comparingDouble(Villager::getX)
                            .thenComparingDouble(Villager::getZ))
                    .toList();
            assertEquals(expected.length, villagers.size(), description + " villager count");
            for (int i = 0; i < expected.length; i++) {
                ExpectedVillager expectedVillager = expected[i];
                Villager villager = villagers.get(i);
                assertEquals(expectedVillager.profession(), villager.getProfession(),
                        description + " villager " + i + " profession");
                assertEquals(expectedVillager.x(), villager.getX(), 0.001f,
                        description + " villager " + i + " x");
                assertTrue(villager.getY() <= expectedVillager.y()
                                && villager.getY() > expectedVillager.y() - 0.25f,
                        description + " villager " + i + " y");
                assertEquals(expectedVillager.z(), villager.getZ(), 0.001f,
                        description + " villager " + i + " z");
            }
        } finally {
            world.cleanup();
        }
    }

    private static StructurePiece villageWoodHutPiece(StructureBoundingBox box, int coordBaseMode,
            boolean tallHouse, int tablePosition) throws Exception {
        Class<?> pieceClass = Class.forName("com.craftzero.world.StructurePlanner$VillageWoodHutPiece");
        java.lang.reflect.Constructor<?> constructor = pieceClass.getDeclaredConstructor(StructureBoundingBox.class,
                int.class, boolean.class, int.class);
        constructor.setAccessible(true);
        return (StructurePiece) constructor.newInstance(box, coordBaseMode, tallHouse, tablePosition);
    }

    private static StructurePiece villageHouse4GardenPiece(StructureBoundingBox box, int coordBaseMode,
            boolean roofAccessible) throws Exception {
        Class<?> pieceClass = Class.forName("com.craftzero.world.StructurePlanner$VillageHouse4GardenPiece");
        java.lang.reflect.Constructor<?> constructor = pieceClass.getDeclaredConstructor(StructureBoundingBox.class,
                int.class, boolean.class);
        constructor.setAccessible(true);
        return (StructurePiece) constructor.newInstance(box, coordBaseMode, roofAccessible);
    }

    private static StructurePiece villageChurchPiece(StructureBoundingBox box, int coordBaseMode) throws Exception {
        Class<?> pieceClass = Class.forName("com.craftzero.world.StructurePlanner$VillageChurchPiece");
        java.lang.reflect.Constructor<?> constructor = pieceClass.getDeclaredConstructor(StructureBoundingBox.class,
                int.class);
        constructor.setAccessible(true);
        return (StructurePiece) constructor.newInstance(box, coordBaseMode);
    }

    private static StructurePiece villageHouse1Piece(StructureBoundingBox box, int coordBaseMode) throws Exception {
        Class<?> pieceClass = Class.forName("com.craftzero.world.StructurePlanner$VillageHouse1Piece");
        java.lang.reflect.Constructor<?> constructor = pieceClass.getDeclaredConstructor(StructureBoundingBox.class,
                int.class);
        constructor.setAccessible(true);
        return (StructurePiece) constructor.newInstance(box, coordBaseMode);
    }

    private static StructurePiece villageHouse3Piece(StructureBoundingBox box, int coordBaseMode) throws Exception {
        Class<?> pieceClass = Class.forName("com.craftzero.world.StructurePlanner$VillageHouse3Piece");
        java.lang.reflect.Constructor<?> constructor = pieceClass.getDeclaredConstructor(StructureBoundingBox.class,
                int.class);
        constructor.setAccessible(true);
        return (StructurePiece) constructor.newInstance(box, coordBaseMode);
    }

    private static StructurePiece villageBlacksmithPiece(StructureBoundingBox box, int coordBaseMode) throws Exception {
        Class<?> pieceClass = Class.forName("com.craftzero.world.StructurePlanner$VillageBlacksmithPiece");
        java.lang.reflect.Constructor<?> constructor = pieceClass.getDeclaredConstructor(StructureBoundingBox.class,
                int.class);
        constructor.setAccessible(true);
        return (StructurePiece) constructor.newInstance(box, coordBaseMode);
    }

    private static StructurePiece villageFarmPiece(StructureBoundingBox box, int coordBaseMode,
            boolean wide) throws Exception {
        Class<?> pieceClass = Class.forName("com.craftzero.world.StructurePlanner$VillageFarmPiece");
        java.lang.reflect.Constructor<?> constructor = pieceClass.getDeclaredConstructor(StructureBoundingBox.class,
                int.class, boolean.class);
        constructor.setAccessible(true);
        return (StructurePiece) constructor.newInstance(box, coordBaseMode, wide);
    }

    private static StructurePiece villageHallPiece(StructureBoundingBox box, int coordBaseMode) throws Exception {
        Class<?> pieceClass = Class.forName("com.craftzero.world.StructurePlanner$VillageHallPiece");
        java.lang.reflect.Constructor<?> constructor = pieceClass.getDeclaredConstructor(StructureBoundingBox.class,
                int.class);
        constructor.setAccessible(true);
        return (StructurePiece) constructor.newInstance(box, coordBaseMode);
    }

    private static StructurePiece villagePathPiece(StructureBoundingBox box, ReleaseOneWorldGenerator generator,
            int coordBaseMode, int componentType) throws Exception {
        Class<?> pieceClass = Class.forName("com.craftzero.world.StructurePlanner$VillagePathPiece");
        java.lang.reflect.Constructor<?> constructor = pieceClass.getDeclaredConstructor(StructureBoundingBox.class,
                ReleaseOneWorldGenerator.class, int.class, int.class);
        constructor.setAccessible(true);
        return (StructurePiece) constructor.newInstance(box, generator, coordBaseMode, componentType);
    }

    private static StructurePiece villageTorchPiece(StructureBoundingBox box, int coordBaseMode) throws Exception {
        Class<?> pieceClass = Class.forName("com.craftzero.world.StructurePlanner$VillageTorchPiece");
        java.lang.reflect.Constructor<?> constructor = pieceClass.getDeclaredConstructor(StructureBoundingBox.class,
                int.class);
        constructor.setAccessible(true);
        return (StructurePiece) constructor.newInstance(box, coordBaseMode);
    }

}

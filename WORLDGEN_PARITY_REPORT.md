# CraftZero World Generation Parity Report

Target: Minecraft Java Release 1.0 Overworld, Nether, and The End generation.

Current status: partial parity. The biome layer graph and several structure
placement rules now match Release 1.0 sources more closely, but full terrain,
population, structure-piece, and render/performance parity is still unproven.

## Verified In This Pass

- Overworld tall-grass decoration now uses the source `grassPerChunk` counts
  during biome decoration instead of the earlier reduced visual pass. Plains
  now run the Release-era 10 tall-grass scatter attempts, forests run 2, and
  mushroom biomes keep their negative count suppressed by the existing positive
  decorator gate.
- Mushroom-island huge mushroom attempts now follow the source origin-biome
  gate without re-checking the target biome. Attempts selected by a mushroom
  origin can spill into the population margin, with the generator's support and
  space checks deciding whether the target location accepts the mushroom.
- Huge mushroom starts now also use the Release-era support whitelist before
  growth: only dirt, grass, and mycelium under the stem can pass the source
  `WorldGenBigMushroom` preflight, instead of allowing every dark opaque
  support accepted by the small-mushroom stay rule.
- Biome decorator height-map consumers now share a source-style height helper:
  trees, huge mushrooms, and the brown-mushroom cluster path start one block
  above the top solid/liquid column, matching `getHeightValue` instead of
  feeding the top block itself. Huge mushroom cap and stem output now also
  uses the source opaque-cube replacement rule, so leaves accepted by the
  preflight no longer block the generated mushroom body.
- The decorator height helper now matches the Release chunk height-map
  predicate more closely by using source `Block.lightOpacity` semantics rather
  than a broad solid/fluid scan. Farmland, half slabs, stairs, soul sand,
  water, ice, leaves, and cobwebs retain their source height behavior, while
  glass, fences, doors, panes, cactus, chests, and other zero-opacity blocks no
  longer lift tree, huge-mushroom, mushroom-cluster, flower, or tall-grass sky
  checks. Generated decorator block-light replay now also attenuates through
  that same source opacity table instead of treating every opaque render face
  as a hard stop.
- Mineshaft corridor torches now use the source structure placement metadata
  path, writing metadata `0` instead of pre-resolving a support face, and
  mineshaft crossings no longer synthesize the old oak-plank underfloor across
  unsupported air cells.
- `WorldGenMinable` ore, dirt, gravel, and lapis vein geometry now uses the
  Release-era `+8` X/Z center offset after the raw chunk-local start draw,
  removing the previous eight-block northwest shift in generated vein bodies;
  the endpoint math now also follows the source float casts around
  `(start + 8)` and `numberOfBlocks / 8F`.
- Overworld ore population and decorator-scratch replay now only include the
  source-reachable west, north, northwest, and current origins for a target
  chunk after that `+8` center offset, avoiding future east/south ore side
  effects in the current chunk's decorator scratch.
- Nether surface replacement now uses the exact Release-era gravel-noise Y
  sample coordinate `109` instead of the previous fractional local value,
  aligning the gravel patch mask with `ChunkProviderHell`.
- Overworld temperature-dependent output now uses a Release-style temperature
  GenLayer path instead of a hard frozen-biome predicate. Surface replacement,
  the base water-surface freeze pass, water-lake surface freeze, scratch lake
  replay, and the final shifted ice/snow pass all gate on source temperature
  `<= 0.15F`, matching the old `WorldChunkManager`/`World` freeze and snow
  checks more closely.
- Release biome temperature constants now keep taiga and taiga hills at the
  source `0.3F` value, and final snow placement now mirrors
  `BlockSnow.canPlaceBlockAt` by requiring opaque solid support instead of
  allowing leaf support.
- Initial Overworld spawn-biome selection now uses the Release-era
  `WorldChunkManager` spawn reservoir of forest, swampland, and taiga instead
  of accidentally allowing plains while omitting swampland.
- Overworld cave, Overworld ravine, and Nether cave carvers now use the
  Release-era `3.141593F` and `1.570796F` angle literals in their sine-table
  paths instead of Java's `Math.PI`, tightening source-coordinate carving
  parity for branch yaw and radius interpolation.
- Overworld `WorldGenLiquids` springs and Nether `WorldGenHellLava` springs
  now replay the source immediate flowing-block tick after a successful
  neighbor gate. The placed spring converts to the still variant with metadata
  `0`, then writes the first source-flow side effect into the one air neighbor
  selected by the generator gate.
- Swamp tree vine generation now follows the Release `WorldGenSwamp` downward
  run length: the side vine block can extend four more air blocks below its
  leaf attachment, instead of stopping after four total vine blocks.
- Overworld population replay now uses a shared chunk-window policy. The target
  chunk still writes the four source-reachable visible origins, while the
  mutable scratch population world also replays the adjacent non-visible
  origins needed for cross-origin structure, lake, dungeon, ore, disk, tree,
  huge-mushroom, detail, spring, creature, and snow/ice reads.
- Final shifted Overworld ice/snow finishing now follows that same population
  window policy: visible origins still write target intersections, while the
  wider scratch-only origins can record freeze/snow side effects for later
  final-pass reads without leaking blocks outside the populated chunk.
- Carried Overworld population scratch now backfills scratch-only ore mutations
  from the wider non-visible origin window before disks, trees,
  huge-mushrooms, detail decorators, creature checks, and final snow/ice read
  it. Visible source-reachable ore origins still write the real target chunk,
  while non-visible origins only contribute mutable source-state side effects.
- Worldgen passive creature placement now uses the Release-era
  `findTopSolidBlock` height scan and `isBlockNormalCube`/liquid predicate
  from `SpawnerAnimals.func_35573_a` instead of a grass/mycelium-only support
  gate. Animals can now evaluate the same post-decoration scratch column that
  source population sees, including leaf-ignored spawn heights and normal-cube
  obstruction checks. Natural sheep colors generated during this pass now also
  consume the world's Release-style random stream, matching the source
  `creatureSpecificInit` path instead of advancing the chunk population random.
  The normal-cube check also keeps source material translucency exceptions for
  leaves, cactus, and TNT out of the spawn-support/obstruction predicate.
- Village path gravel placement now mirrors
  `ComponentVillagePathGen.addComponentParts`: the road Y is
  `findTopSolidBlock(x,z) - 1`, so leaves and fluids are ignored instead of
  letting water/lava columns become the road surface.
- Village blacksmith generation now mirrors the Release 1.0
  `ComponentVillageHouse2` block output by omitting the later-version
  generated loot chest. The blacksmith piece no longer consumes the shared
  structure-placement random stream for chest rolls that do not exist in the
  target source.
- Generated village wooden doors now use a local `ItemDoor.placeDoorBlock`
  metadata pass: the lower half is first rotated through the village
  component transform, then the source adjacent-normal-cube/same-door hinge
  rule decides whether to set the open/hinge bit and mirrors that value into
  the upper half.
- Village building and well grounding now uses the source `ComponentVillage`
  foundation contract: average `max(world.findTopSolidBlock(x,z),
  worldOceanHeight)`, where `findTopSolidBlock` returns the first air block
  above the top non-leaf solid. This removes the previous one-block-low
  placement drift from averaging CraftZero's top-solid terrain Y directly.
- Nether decorator replay now uses the same wider scratch-only window for
  lava, fire, glowstone, and mushroom passes, so non-visible neighboring
  decorator side effects can affect later Nether validation without writing
  impossible spillover blocks into the target chunk.
- The End entry platform now uses a single shared Release-era definition for
  portal transfer restoration and generated chunks: 5x5 obsidian centered at
  `(100, 48, 0)` with the three-block air clearance above it. End chunks that
  include the entry footprint now contain the platform even before a portal
  transfer mutates the world.
- Village start generation now keeps the Release-era source graph height for
  the well and roads: the well is planned at Y=64..78 and roads/buildings are
  selected from that source-space graph, while each grounded village piece
  carries a separate placement Y for terrain output. This avoids terrain
  pre-grounding from changing village path/building collision decisions.
  Grounded output now uses the source `getFoundationLevel` semantics rather
  than top-solid terrain Y, so buildings and wells sit at the same vertical
  offset as Release 1.0 once terrain grounding is applied.
- Stronghold and mineshaft vertical offsets now use the source
  `markAvailableHeight(world, random, 10)` ocean-height ceiling. Release
  `worldOceanHeight` is Y=63, matching CraftZero's water surface constant, so
  generated stronghold and mineshaft shifts no longer borrow the separate
  village foundation minimum of `SEA_LEVEL + 1`.
- Generated stronghold weighted branch selection now keeps exhausted piece
  weights inside the source draw range and burns the selected attempt when that
  weight can no longer spawn, rather than filtering exhausted weights before
  `nextInt(totalWeight)`. This preserves the Release-era random stream for
  later stronghold branch attempts.
- Nether glowstone cluster growth now permits source-valid Y=0 candidates
  instead of rejecting them with the previous `py <= 0` guard; only negative
  and above-world candidates are skipped.
- Mineshaft corridor split support caps now match the Release-era
  `ComponentMineshaftCorridor` block layout: the rare split-cap branch places
  only the two side roof planks at the support axis instead of extending those
  side planks into the neighboring corridor cells.
- Mineshaft corridor support posts now use the source fence columns for both
  lower support cells at each section, instead of leaving oak-plank bases under
  the fence uprights.
- Nether fortress entrance lava wells now place the source top block as
  flowing lava instead of a still lava source, while preserving the generated
  downward flow column used during chunk-safe structure placement.
- Nether Hell cave carver block-output coverage now locks multiple
  `MapGenCavesHell` source-audited vectors directly against the standalone
  carver. Seeds `515151`, `1234`, and `987654321` now cover origin, shifted
  negative, and shifted positive chunks with full block-id hashes, air and
  netherrack counts, zero-lava invariants, and representative carved/solid
  samples. Focused verification:
  `.\gradlew.bat test --tests "com.craftzero.world.NetherCaveGeneratorTest"`
  plus the item-1 worldgen bundle.
- Mineshaft room block-output coverage now locks the source child-opening clear
  volume. Stored room child connector boxes clear their top three Y slices into
  the room wall, while adjacent upper wall cells outside that connector remain
  untouched by the opening pass. Focused verification:
  `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.mineshaftRoomClearsSourceChildOpeningVolumes"`
  plus `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest"`.
- Mineshaft stair block-output now follows the Release-era
  `ComponentMineshaftStairs` descending-slice lower bound
  `5 - i - (i >= 4 ? 0 : 1)`, carving one extra lower row for the first four
  steps while preserving the solid row below each slice. Focused verification:
  `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.mineshaftStairsUseSourceDescendingCarveSlices"` plus
  `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest"` and
  the item-1 worldgen bundle.
- Nether fortress out-of-range branch caps now keep the Release-era
  `ComponentNetherBridgePiece` distance-guard depth semantics. When a branch
  moves more than 112 blocks from the start piece, the terminal
  `ComponentNetherBridgeEnd` is created with the parent component type instead
  of the normal child-depth increment. This tightens fortress graph
  source-vector parity for distance-capped branches. Focused verification:
  `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.netherFortressDistanceCapUsesParentDepthForEndCaps"` plus
  `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest"`.
- Village side-road branches now reuse the parent path component depth, matching
  the Release-era `ComponentVillagePathGen` calls into
  `StructureVillagePieces.getNextStructureComponentVillagePath`. Building
  attachments still advance through the separate village-structure recursion
  path. Focused verification:
  `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.villagePathQueueAttachesImplementedPiecesWithoutTorchSpam"` plus
  `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest"` and
  the item-1 worldgen bundle.
- Generated stronghold starts now use the same Release-era `MapGenStructure`
  chunk RNG path as mineshafts and villages: seed the map generator from the
  world seed, draw the per-world X/Z multipliers, reseed by structure chunk,
  consume the `recursiveGenerate` guard `nextInt()`, then run
  `ComponentStrongholdStairs2` attempts on that shared stream until a portal
  room is present. Focused verification:
  `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdStartUsesSourceSizedStairs" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdUsesSourceFirstCrossingChild" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdStartsRetrySourceRecursionUntilPortalRoomExists" --tests "com.craftzero.world.StructureGeneratorTest.strongholdPlacementUsesReleaseOneBiomeReservoirSearch"` plus
  `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest"`.
- Stronghold portal room End portal frames now use the Release 1.0
  high-roll eye predicate: each frame adds the eye metadata only when
  `random.nextFloat() > 0.9F`. The old implementation used the low 10% of the
  same random stream, preserving probability but producing different
  source-vector block output.
- Village path generation now matches the Release-era `ComponentVillagePathGen`
  candidate ordering: component-depth and start-distance guards run before the
  road-length draw, the length loop shrinks only for piece intersections, and
  min-Y/biome rejection no longer probes shorter road candidates. This keeps
  the village random stream coupled to source behavior for later buildings and
  torch fallbacks. Focused verification:
  `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.villagePathDistanceGuardRunsBeforeLengthSearch"` plus
  `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest"`.
- Village starts now apply the Release-era `StructureVillageStart` size gate
  after the well, road, and component graph is built. Only starts with more
  than two non-road components are exposed for chunk placement, structure
  locating, and lake suppression; biome/grid hits that collapse to a well plus
  roads are discarded. Focused verification:
  `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.villagePlacementUsesReleaseOneGridAndBiomeGate" --tests "com.craftzero.world.StructureGeneratorTest.villageBiomeGateUsesGenerationLayer" --tests "com.craftzero.world.StructureGeneratorTest.locateVillageMatchesGeneratedStart" --tests "com.craftzero.world.StructureGeneratorTest.villagePathQueueAttachesImplementedPiecesWithoutTorchSpam" --tests "com.craftzero.world.StructureGeneratorTest.nonSizeableVillageStartsDoNotGenerateOrSuppressLakes"`.
- Overworld taiga tall-grass decoration now keeps the Release 1.0
  `WorldGenGrass` metadata/RNG path. The generator no longer consumes a
  taiga-only random draw or produces fern metadata during grass decoration, so
  taiga ground cover stays on long-grass metadata `1` and later decorators keep
  their expected source-order random stream. Focused verification:
  `.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.biomeDecoratorsUseSourceShapedDetailScatter" --tests "com.craftzero.world.OverworldGenerationSprintTest.biomeGrassMetadataDoesNotConsumeTaigaSpecificRandomness"`.
- Stronghold side-branch generation now includes the Release-era weighted
  portal-room component. Recursive branches can roll depth-gated, source-sized
  11x8x16 portal chambers, and generated starts now retire the guaranteed
  proxy library/portal spine. The start staircase forces only the source first
  crossing, then drains the shared recursive pending queue by random index and
  retries whole start attempts until a weighted portal room exists. Recursive
  room crossings, chest corridors, prisons, libraries, and portal rooms now
  consume their source weighted quotas. Focused verification:
  `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.generatedStronghold*" --tests "com.craftzero.world.StructureGeneratorTest.locateStrongholdMatchesGeneratedPortalRoom"`,
  `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest"`, and
  `.\gradlew.bat test --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.StructureGeneratorTest"`.
- Red huge mushroom caps now keep the Release-era layer-relative interior
  block rule. Metadata-0 cap cells below the second-highest red cap layer are
  still skipped and filled by the stem pass, but the upper interior cells are
  emitted as cap blocks instead of being accidentally suppressed by a base-Y
  comparison. Focused verification:
  `.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.redHugeMushroomCapsKeepSourceUpperInteriorCells"` and
  `.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest"`.
  Broader item-1 verification:
  `.\gradlew.bat test --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.StructureGeneratorTest"`.
- Stronghold corridor connectors now use the Release-era stronghold-stone
  selector for shell blocks. Open-ended corridor tubes still avoid monster
  eggs, but their edge blocks now include the source plain/mossy/cracked
  stone-brick mix instead of uniform plain stone brick. Focused verification:
  `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.strongholdCorridorUsesSourceOpenEndedTubeLayout"` and
  `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.strongholdStonesUseReleaseOneVariantMetadata" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdCorridorsUseSourceTubeDimensions"`.
- Stronghold randomized shell and interior scatter fills now follow the source
  `fillWithRandomizedBlocks`/`randomlyFillWithBlocks` local traversal order:
  `y`, then `x`, then `z`. Stronghold chest-corridor, library, and
  room-crossing loot RNG expectations now advance through that source-order
  shell/cobweb stream before chest rolls. Focused verification:
  `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.strongholdRandomizedShellsUseSourceLocalXBeforeZ" --tests "com.craftzero.world.StructureGeneratorTest.strongholdChestCorridorUsesSourceShelfLayout" --tests "com.craftzero.world.StructureGeneratorTest.strongholdLibraryUsesSourceLargeRoomLayout" --tests "com.craftzero.world.StructureGeneratorTest.strongholdRoomCrossingUsesSourceBalconyLayout"`.
- Nether Hell cave carving now mutates the same Y cell used by the source
  ellipsoid membership check instead of carving one block above the tested
  coordinate. This corrects vertical cave placement after Nether surface
  replacement while preserving the existing lava-contact abort and
  soul-sand/gravel surface-patch preservation. Focused verification:
  `.\gradlew.bat test --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.WorldGenerationParityTest.generatedNetherBaseChunkAppliesHellCavesAfterSurfaceReplacement" --tests "com.craftzero.world.WorldGenerationParityTest.releaseOneNetherDecoratesTerrain"`.
- Abandoned mineshaft corridor torches now emulate Release-era generated torch
  auto-orientation when a generated support is present. Corridor torch chance
  rolls still consume the same placement random stream, but successful torches
  now resolve valid wall/standing metadata from adjacent planks or support
  blocks instead of remaining raw metadata `0` after direct chunk writes.
  Focused verification:
  `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.mineshaft*"`.
- Abandoned mineshaft crossings now add the Release-era air-only plank support
  floor one block below the intersection footprint. Corridors still avoid
  synthetic floors, but crossings now match the source component's final pass:
  unsupported air below the crossing is filled with oak planks while existing
  terrain remains untouched. Focused verification:
  `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.mineshaftCrossAddsSourceAirOnlySupportFloor"`.
- Overworld population replay now seeds the mutable lake/dungeon/ore/decorator
  scratch with source-order structure block side effects. Temporary replay
  chunks are built from carved terrain plus generated structures before lake
  and dungeon replay, and neighboring structure-chunk differences are overlaid
  into the scratch without overwriting the target chunk. Late tree, plant,
  mushroom, spring, dungeon, ore, and worldgen-creature checks can therefore
  see nearby stronghold/village/mineshaft blocks and carved air instead of
  treating those columns as untouched terrain. Focused verification:
  `.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.decoratorScratchOverlaysOffChunkStructuresBeforeLateFeatures" --tests "com.craftzero.world.OverworldGenerationSprintTest.sourceTreeScratchOverlaysOffChunkLakesBeforeTrees" --tests "com.craftzero.world.OverworldGenerationSprintTest.dungeonValidationReadsOffChunkLakeScratchState" --tests "com.craftzero.world.OverworldGenerationSprintTest.decoratorScratchOverlaysOffChunkDungeonsBeforeLateFeatures" --tests "com.craftzero.world.OverworldGenerationSprintTest.decoratorScratchOverlaysOffChunkOresBeforeLateFeatures" --tests "com.craftzero.world.WorldGenerationParityTest.overworldDungeonRandomResumesAfterLakeBranches" --tests "com.craftzero.world.WorldGenerationParityTest.overworldOreRandomResumesAfterLakeAndDungeonPhases" --tests "com.craftzero.world.WorldGenerationParityTest.overworldDecoratorRandomResumesAfterOreHelper"`.
- Live Overworld chunk population now carries the same mutable population
  scratch through lake placement, dungeon placement, ore placement, and biome
  decoration for the target chunk. Target writes still land in the chunk, but
  off-chunk mutations from visible population origins stay in the scratch for
  later source-order support/obstruction checks; target overlays are cleared
  after falling-block stabilization so late decorators read the stabilized
  chunk state. Focused verification:
  `.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.sourceTreeScratchOverlaysOffChunkLakesBeforeTrees" --tests "com.craftzero.world.OverworldGenerationSprintTest.dungeonValidationReadsOffChunkLakeScratchState" --tests "com.craftzero.world.OverworldGenerationSprintTest.worldGenCreatureSpawningReadsDecoratorScratchState" --tests "com.craftzero.world.OverworldGenerationSprintTest.lakeValidationReadsMutableScratchState" --tests "com.craftzero.world.OverworldGenerationSprintTest.decoratorScratchOverlaysOffChunkOresBeforeLateFeatures" --tests "com.craftzero.world.OverworldGenerationSprintTest.overworldOrePopulationCarriesOffChunkScratchMutations" --tests "com.craftzero.world.OverworldGenerationSprintTest.decoratorScratchOverlaysOffChunkDungeonsBeforeLateFeatures" --tests "com.craftzero.world.OverworldGenerationSprintTest.decoratorScratchOverlaysOffChunkStructuresBeforeLateFeatures" --tests "com.craftzero.world.OverworldGenerationSprintTest.lateDecoratorsReadOffChunkSourceScratchState" --tests "com.craftzero.world.OverworldGenerationSprintTest.underwaterDisksUseOffChunkSourceScratchState" --tests "com.craftzero.world.WorldGenerationParityTest.overworldPopulationRandomAdvancesThroughStructurePlacement" --tests "com.craftzero.world.WorldGenerationParityTest.overworldStructureReplayUsesCarvedTerrainLiquidChecks" --tests "com.craftzero.world.WorldGenerationParityTest.overworldDungeonRandomResumesAfterLakeBranches" --tests "com.craftzero.world.WorldGenerationParityTest.overworldOreRandomResumesAfterLakeAndDungeonPhases" --tests "com.craftzero.world.WorldGenerationParityTest.overworldDecoratorRandomResumesAfterOreHelper"`.
- The final shifted Overworld ice/snow pass now builds a world-coordinate
  block-light snapshot from the mutable population scratch as well as the
  target chunk, using the widened population light margin shared by Overworld
  and Nether decorator scratch replay. Off-chunk generated torches and other
  light emitters from neighboring source-population origins can now suppress
  `canBlockFreeze` and `canSnowAt` decisions at chunk borders and in
  scratch-only final-pass origins, matching the Release provider's world-light
  checks during the shifted freeze/snow loop. Focused verification:
  `.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.finalIceAndSnowPassHonorsSourceFluidAndLightGates" --tests "com.craftzero.world.OverworldGenerationSprintTest.finalIceAndSnowPassReadsOffChunkScratchBlockLight"`.
- Late Overworld detail decorator light gates now use a block-light snapshot
  built from the mutable population scratch instead of the target chunk alone.
  The snapshot can now answer by world coordinate for scratch-backed off-target
  placements as well as target intersections, so covered mushrooms, flowers,
  tall grass, and dead bushes now see neighboring generated torches and other
  off-chunk emitters during their source `BlockFlower`-style stay checks.
  Focused verification:
  `.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.lateDecoratorsReadOffChunkSourceScratchState" --tests "com.craftzero.world.OverworldGenerationSprintTest.lateDecoratorLightGatesReadOffChunkScratchEmitters" --tests "com.craftzero.world.OverworldGenerationSprintTest.finalIceAndSnowPassReadsOffChunkScratchBlockLight"`.
- Nether mushroom decoration now also uses a world-coordinate generated
  block-light snapshot built from the mutable Nether decorator scratch. Bright
  off-target glowstone produced earlier in the source decorator order can now
  reject later off-target scratch-backed mushroom candidates before later
  shifted origins read those scratch side effects. Focused verification:
  `.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest.netherMushroomsUseWorldGenFlowersScatterAndRngCost" --tests "com.craftzero.world.WorldGenerationParityTest.netherMushroomScratchLightReadsOffTargetGlowstone" --tests "com.craftzero.world.WorldGenerationParityTest.netherDecoratorScratchPreservesOffTargetGlowstoneGrowth"`.
- Worldgen parity fixtures were refreshed against the current source-shaped
  structure/decorator stream. The mixed mineshaft/village replay vector for
  seed `1`, chunk `(-45,38)` now locks the post-structure population stream to
  `nextInt(16)=9`, `nextInt(128)+4=50`, `nextInt(16)=14`; the surface detail
  decorator fixture now checks deterministic sugar cane in chunk `(-47,-7)`;
  and the taiga metadata fixture now asserts long-grass metadata `1` with no
  fern metadata. Verification:
  `.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest.releaseOneOverworldDecoratesSurfaceDetails" --tests "com.craftzero.world.WorldGenerationParityTest.releaseOneTaigaDecorationKeepsLongGrassMetadata"`,
  `.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest"`, and
  `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.mineshaftCorridorUsesSharedPlacementRandom" --tests "com.craftzero.world.StructureGeneratorTest.villageFarmCropAgesUsePlacementRandomStream" --tests "com.craftzero.world.StructureGeneratorTest.netherFortressPiecesUseSharedPlacementRandom"`.
- Full structure regression coverage is green after refreshing the isolated
  stronghold library inventory fixture to prepare a liquid-free generated chunk
  envelope before world-backed piece placement. That keeps the source
  liquid-envelope abort rule intact while still exercising staged stronghold
  library chest inventories. Verification:
  `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.strongholdLibraryUsesSourceLargeRoomLayout"` and
  `.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest"`.
- Abandoned mineshaft corridor support arches now match the Release-era special
  support branch: the random side-arch variant places oak plank spans along the
  left and right roof rails around the support post and keeps the center ceiling
  lane open. Focused verification:
  `.\gradlew test --tests "com.craftzero.world.StructureGeneratorTest.mineshaftCorridorSupportArchUsesSourceSideRoofSpan" --tests "com.craftzero.world.StructureGeneratorTest.mineshaftCorridorUsesSourceSectionSupports"`.

## Source References

- OneHundredGenerator Java 1.0 port: https://github.com/DjDCH/OneHundredGenerator
- DI9 Minecraft 1.2.5 source mirror used where Release 1.0-adjacent code is stable:
  - `GenLayer`
  - `GenLayerAddIsland`
  - `GenLayerAddMushroomIsland`
  - `GenLayerMushroomShore`
  - `GenLayerBiome`
  - `GenLayerAddSnow`
  - `GenLayerRiverInit`
  - `GenLayerRiver`
  - `GenLayerRiverMix`
  - `GenLayerVoronoiZoom`
  - `MapGenStronghold`
  - `WorldGenSpikes`
  - `BiomeEndDecorator`
  - `ChunkProviderEnd`
  - `ChunkProviderHell`
  - `MapGenCavesHell`
  - `NoiseGeneratorOctaves`
  - `NoiseGeneratorPerlin`
  - `ChunkProviderGenerate`
  - `Chunk`
  - `BiomeDecorator`
  - `SpawnerAnimals`
  - `BlockFlower`
  - `WorldGenTallGrass`
  - `WorldGenDeadBush`
  - `WorldGenTrees`
  - `WorldGenBigTree`
  - `WorldGenTaiga1`
  - `WorldGenTaiga2`
  - `WorldGenSwampTree`
  - `BlockMushroom`
  - `BlockMushroomCap`
  - `BlockGlowStone`
  - `WorldGenGlowStone1`
  - `WorldGenGlowStone2`
  - `WorldGenSand`
  - `WorldGenClay`
  - `WorldGenReed`
  - `WorldGenCactus`
  - `WorldGenPumpkin`
  - `WorldGenLiquids`
  - `WorldGenDungeons`
  - `MapGenStructure`
  - `StructureStart`
  - `StructureStrongholdStart`
  - `ComponentStronghold`
  - `MapGenVillage`
  - `StructureVillageStart`
  - `ComponentVillageWell`
  - `ComponentVillagePathGen`
  - `ComponentVillageWoodHut`
  - `ComponentVillageChurch`
  - `ComponentVillageHouse1`
  - `ComponentVillageHouse2`
  - `ComponentVillageHouse3`
  - `ComponentVillageHall`
  - `ComponentVillageHouse4_Garden`
  - `ComponentVillageField`
  - `ComponentVillageField2`
  - `ComponentVillageTorch`
  - `ComponentStrongholdStairs2`
  - `ComponentStrongholdStairs`
  - `ComponentStrongholdStraight`
  - `ComponentStrongholdStairsStraight`
  - `ComponentStrongholdLeftTurn`
  - `ComponentStrongholdRightTurn`
  - `ComponentStrongholdCrossing`
  - `ComponentStrongholdPortalRoom`
  - `ComponentStrongholdChestCorridor`
  - `ComponentStrongholdPrison`
  - `ComponentStrongholdLibrary`
  - `ComponentStrongholdRoomCrossing`
  - `ComponentStrongholdCorridor`
  - `StructureNetherBridgePieces`
  - `ComponentNetherBridgePiece`
  - `ComponentNetherBridgeEnd`
  - `EnumDoor`
  - `WorldServer.createSpawnPosition`
  - `WorldChunkManager.findBiomePosition`
  - `WorldProvider.canCoordinateBeSpawn`

## Verified In This Pass

- Generated stronghold side-branch assembly now follows more of the
  Release-era weighted component path. When weighted piece selection cannot add
  a room but the source same-floor collision probe can connect to a nearby
  piece, the branch now emits the short open-ended
  `ComponentStrongholdCorridor` tube instead of stopping at the doorway.
  If a selected weighted room's source bounding box is invalid, the selector
  now keeps scanning later weights within the same draw attempt instead of
  consuming a fresh branch-selection RNG draw.
  Weighted libraries are also gated to source depth `> 4`, weighted chest
  corridors use the old four-instance cap, and the branch selector now stops
  expansion once every capped stronghold room class is exhausted instead of
  continuing with unlimited filler-only corridors.
- Nether fortress recursive weighted selection now keeps scanning later source
  weights within the same draw attempt when the selected bridge piece's
  bounding box is invalid. The fortress height fixture now asserts the source
  `setRandomHeight(48, 70)` rule against the full final structure bounds
  instead of assuming the root crossing alone remains in that range after tall
  recursive graphs are assembled.
- Village recursive start expansion now drains pending road/path pieces before
  pending building pieces, matching `StructureVillageStart` queue priority.
  This keeps the path-branch RNG stream coupled to the source order instead of
  spending queue-selection draws on buildings with no child expansion first.
- Stronghold placement now applies the source liquid-envelope abort to
  non-portal pieces. Start stairs, corridors, turns, libraries, prisons, chest
  corridors, crossing rooms, and weighted branch rooms skip carving for a
  target chunk when their expanded piece box touches water or lava, while the
  portal room remains exempt and still overwrites terrain/liquids like the old
  source room.
- World-backed Overworld one-time creature population now validates against
  the post-decoration scratch state. The shifted `chunk*16+8` animal spawn
  area reads decorated top solid/liquid height, grass/mycelium support,
  fluids, and collision boxes, so generated passive packs can use off-chunk
  decorated grass/ground cover and reject late tree, dungeon, fluid, or solid
  obstructions before staged mobs enter the world.
- Overworld decorator scratch replay now includes off-target dungeon block
  side effects between the lake and ore phases. Later ore, tree, huge
  mushroom, plant, reed/cactus, and spring checks can now see neighboring
  dungeon shell blocks, carved room air, chests, and spawners before they
  decide support, obstruction, and stone-replacement outcomes.
- New-world spawn selection now follows the Release-era source shape instead
  of CraftZero's previous custom expanding-edge scan. Overworld spawns first
  run the seeded generation-layer biome reservoir search around `(0,0)` for
  the old forest/swampland/taiga spawn biome set, then apply paired
  `nextInt(64)-nextInt(64)` X/Z jitter attempts until the provider-style grass
  surface gate accepts the coordinate, capped at the old 1000 tries. CraftZero
  still resolves the returned player Y to the generated surface so new worlds
  place the player on top of the selected block.
- `ReleaseOneBiomeSource` now follows the Java 1.0 `mc100` layer topology:
  - The post-biome stack matches the OneHundredGenerator source shape:
    `GenLayerBiome`, two normal zooms, then four final zooms with only
    `GenLayerIsland(3)` and `GenLayerMushroomShore(1000)` inserted during the
    first final zoom.
  - Later `GenLayerHills`, regular `GenLayerShore`, and
    `GenLayerSwampRivers` behavior has been removed from the Release 1.0
    biome source. Natural Overworld sampling no longer emits the later
    beach/hills/edge biome ids.
  - Warm biome selection includes taiga.
  - Snow layer converts land into plains or ice plains with the Release 1.0
    1-in-5 ice plains rule.
  - River zoom count and final biome zoom sequence were corrected.
  - River initialization/mixing now follows the old source shape: every
    positive parent cell becomes `nextInt(2) + 2`, river carving compares the
    raw 0/2/3 neighbor values, only normal ocean bypasses river mixing, and
    only ice plains convert mixed rivers to frozen rivers. This refreshes the
    locked river/mushroom-shore, ocean-density, structure-RNG, lily, and cactus
    fixtures under the corrected river stream.
  - Pre-biome island, mushroom-island, snow, and biome layers now use the
    source's raw ocean sentinel (`0`) instead of treating `FROZEN_OCEAN` as
    ocean. Frozen-ocean ids emitted by island erosion now flow through
    `GenLayerBiome` like the source, which converts non-`0`/non-`1`/
    non-mushroom climate ids into ice plains and shifts the locked frozen-river
    and mushroom-island fixtures accordingly.
  - Final Voronoi biome zoom now seeds each 4x4-cell corner jitter from the
    source block-space cell origin (`cell << 2`) instead of raw cell
    coordinates. Focused boundary vectors now lock the corrected
    mushroom-shore/ocean split at `(-9656,3664)` and `(-9656,3666)`.
  - Zoom tie-breaking now follows the old GenLayerZoom behavior, including its
    unusual third-argument returns in the final tie branches.
- Stronghold placement uses the Release 1.0-style three-stronghold ring and
  generation-layer biome reservoir search.
- Stronghold and village fixtures were refreshed under the corrected Java 1.0
  biome map. Seed `38` now locks stronghold starts at chunks `(-5,-44)`,
  `(64,19)`, and `(-49,36)`, and the source-shaped village path queue at
  `(-79,9)` now deterministically produces seven farm pieces and two torch
  fallbacks.
- Stronghold allowed biomes are limited to the Release 1.0 set:
  `DESERT`, `FOREST`, `EXTREME_HILLS`, `SWAMPLAND`, `TAIGA`, `ICE_PLAINS`,
  and `ICE_MOUNTAINS`.
- Village placement uses Release 1.0 spacing, separation, salt, and the reduced
  plains/desert biome gate against the source generation-layer biome map rather
  than final Voronoi biome samples. Seed `0`, chunk `(116,176)` locks a
  sizeable village case where the final center biome is ocean but the
  generation-layer village gate is desert.
- Village starts now include the source-shaped well root at `(chunk << 4) + 2`
  and its initial four source-style path components. The well uses the 6x6
  gravel rim, cobblestone shaft, flowing-water center, fence posts, and
  cobblestone roof from the Release-era component layout. Initial roads now use
  the village start random stream after the weighted-piece setup draws and lay
  gravel at each intersecting terrain-surface column instead of keeping the old
  fake sandstone/planks shell.
  After the graph expands, village starts must now contain more than two
  non-road pieces before they count as generated villages. Seed `0`, chunk
  `(107,-59)` locks a desert-origin attempt that raw expansion reduces to a
  well plus two roads and is therefore rejected.
- Village path components now process the source-style side-attachment queues:
  pending paths are selected with the village random stream, side offsets use
  the `ComponentVillage` NN/PP rules, weighted piece limits are initialized in
  the source order, and implemented side components can enqueue buildings before
  later paths continue. The first implemented building is the source-shaped
  wood hut, including short/tall roof variant state, table-position state,
  oriented plank/log/cobblestone/glass/door placement, downward cobblestone
  support fills, and the source torch fallback piece. Fallback lamp posts now
  rotate their four wall-torch metadata values against the wool lamp block
  instead of writing invalid torch metadata `15`. The weighted selector now
  matches the source exhaustion path: if no weighted village piece can spawn,
  it returns no component instead of placing a synthetic fallback torch.
  Building and path viability checks now use the same generation-layer
  `WorldChunkManager.areBiomesViable`-style radius gate as Release 1.0.
  Side roads now carry the parent path component depth into the next
  `ComponentVillagePathGen`, while side buildings still use the incremented
  village-structure depth.
  Path block placement now resolves its gravel Y from the live generated column
  using source-style `findTopSolidBlock(x,z) - 1` semantics, ignoring leaves
  and fluids instead of relying only on density-field terrain height.
  If a later village table expansion introduces an unavailable weighted piece,
  the selector still skips it instead of visually inventing fallback
  structures.
- Village field variants now use the source-shaped farm layouts: the 13x9 and
  7x9 farm footprints place log borders, farmland rows, moving-water channels,
  crop rows with Release-era age range `2..7`, air clearing, and downward dirt
  support fills. Crop age draws now consume the threaded structure placement
  random when village structures are populated, with a deterministic direct
  fallback retained for isolated piece fixtures.
- Village halls now use the source-shaped two-section layout: front room,
  rear fenced area, plank/cobblestone shell, oriented stair roof, glass panes,
  table detail, double-slab seating, two wooden doors, torches, and downward
  cobblestone support fills.
- Village garden houses now use the source-shaped 5x5 layout: cobblestone
  floor and corner posts, plank walls, glass panes, log/plank roof, open front
  arch, optional roof fence and oriented ladder, the interior torch, and
  downward cobblestone support fills. The weighted village selector now lets
  `HOUSE_4_GARDEN` consume its source weight and limit instead of skipping it
  as an unimplemented component.
- Village churches now use the source-shaped 5x9, 12-high layout: cobblestone
  nave and tower shell, tower air shaft, stair details, glass panes, interior
  torches, oriented ladder, wooden door, and downward cobblestone support
  fills. The weighted village selector now lets `CHURCH` consume its source
  weight and limit instead of skipping it as an unimplemented component.
- Village house 1 now uses the source-shaped 9x6 layout: cobblestone base and
  gabled shell, oriented plank-stair roof, plank/cobblestone walls, glass pane
  clusters, bookshelves, table details, crafting table, wooden door, and
  downward cobblestone support fills. The weighted village selector now lets
  `HOUSE_1` consume its source weight and limit instead of skipping it as an
  unimplemented component.
- Village house 3 now uses the source-shaped 9x12 L-shaped layout: two-room
  plank floors, cobblestone and plank walls, asymmetric oriented stair roofs,
  log/glass pane window groups, torch and wooden door, cleared front apron, and
  footprint-specific downward cobblestone support fills. The weighted village
  selector now lets `HOUSE_3` consume its source weight and limit.
- Village blacksmiths now use the source-shaped 10x7 `ComponentVillageHouse2`
  layout: slab roof, log/plank/cobblestone shell, fenced front, forge lava,
  iron bars, furnaces, glass panes, table detail, and downward cobblestone
  support fills. Release 1.0 `ComponentVillageHouse2` has no generated loot
  chest, so the blacksmith now avoids later-version chest block output and
  placement-random loot rolls while still staging the source smith villager at
  local `(7,1,1)`.
- World-backed village pieces now stage the Release 1.0 source villagers for
  implemented building components: wood huts and garden houses spawn one
  farmer at local `(1,1,2)`, churches spawn one priest at `(2,1,2)`, house 1
  spawns one librarian at `(2,1,2)`, house 3 spawns two farmers at `(4,1,2)`
  and `(5,1,2)`, halls spawn one butcher then one farmer at `(4,1,2)` and
  `(5,1,2)`, and blacksmiths spawn one smith. The shared helper keeps the
  source-style per-piece spawned-villager counter and chunk gate; the villager
  shell currently covers generated placement, professions, profession-specific
  textures, passive flee/wander/door use, idle player awareness, and save/load
  preservation.
  `StructureStart`/`StructurePiece` placement can now receive the source
  population random, and Overworld structure placement passes that random into
  components.
- Lake population now asks the village planner's sizeable-start decision before
  placing water/lava lakes from a population origin, matching the Release 1.0
  village suppression rule without suppressing lakes for failed village
  attempts.
- Water-lake population no longer skips desert origins. Release 1.0 still
  consumes and attempts the 1-in-4 water-lake branch in deserts; a deterministic
  all-desert-origin fixture now locks that behavior.
- Rejected low lake attempts now preserve `WorldGenLakes` RNG consumption:
  once the branch is selected, the ellipsoid count and shape doubles are drawn
  before boundary validation can reject the lake. This keeps the following
  population draws, especially the lava-lake branch, closer to the source stream.
- Lake candidate validation and lake RNG replay now read cave/ravine-carved
  terrain for the origin and neighboring columns instead of falling back to
  undecorated base terrain outside the generated chunk.
- Lake population and lake RNG replay now validate later water/lava lake
  attempts against a mutable source scratch containing earlier accepted lake
  water, lava, air cavities, restored surfaces, frozen surfaces, and lava stone
  shells. This makes the source water-before-lava ordering visible to later
  lake validation instead of validating every lake against a fresh carved chunk.
- Dungeon population now resumes from the same population RNG after replaying
  the source water/lava lake branches for the origin. A source-derived fixture
  locks the first dungeon center after a water-lake attempt for seed `424242`,
  origin `(-20,-18)`.
- Overworld dungeon validation now reads carved neighboring chunk terrain
  instead of treating every off-target coordinate as solid stone. World-backed
  dungeon placement still reads the target chunk after structures/lakes have
  already mutated it, while neighboring room-envelope checks see cave/ravine
  carved terrain plus replayed off-target lake side effects. The pre-ore
  dungeon RNG replay now applies source lakes to its carved origin scratch and
  uses the same lake-aware origin/neighbor reader, so dungeon success/failure
  consumes source-shaped RNG without the old fake-stone/off-chunk-lake shortcut.
  Dungeon generation no longer skips source-valid rooms whose bounding boxes
  land wholly outside the current target chunk. Those off-target rooms now
  replay into the mutable dungeon scratch, so later dungeon chest/spawner RNG
  and later room validation can see source-order off-target shell, air, chest,
  and spawner mutations while still only staging target-chunk tile entities.
  The later decorator scratch also replays bounded off-target dungeon blocks
  before ore and biome decorators run, so neighboring dungeon rooms are no
  longer invisible to late tree/detail/spring support and obstruction checks.
- Null-world Overworld chunk generation now still runs dungeon and structure
  block population. Dungeon spawners/chests, village roads/wells, and Nether
  fortress bricks, blaze spawners, and wart-room blocks are no longer tied to
  tile-entity staging through a non-null `World`; only generated tile entities
  are skipped when no world object is available. Nether fortress loot chests
  remain absent because they are post-Release-1.0 content.
- Village lake suppression now uses the structure planner for both world-backed
  and null-world population paths, so null-world village chunks no longer allow
  source-suppressed lakes to carve through the generated structure blocks.
- World-backed Overworld chunk population now follows the source high-level
  ordering more closely: structures are generated before lake attempts, dungeon
  attempts run before ore/biome decoration, Release-style creature population
  runs after decoration, and snow/ice finishing remains last.
- Overworld structure generation/replay now follows the source structure pass
  order inside `ChunkProviderGenerate.populate`: mineshafts first, villages
  second, strongholds third. A mixed-structure fixture locks the downstream
  population RNG before lake checks.
- Overworld population RNG replay now advances through source-order structure
  placement before lake, dungeon, ore, and decorator helpers resume their origin
  streams. A focused stronghold fixture locks the difference between a raw
  population seed and the post-structure stream used by Release 1.0 lake checks.
- Overworld structure RNG replay now runs against copied cave/ravine-carved
  terrain, so mineshaft room/corridor/cross/stairs liquid-envelope aborts can
  preserve the same population stream that source chunk population would see
  instead of consuming random from an empty scratch chunk.
- Mineshaft corridor block placement no longer synthesizes an oak-plank floor
  under unsupported cells, so rails are attempted only when the preexisting
  block below the corridor center is solid. Crossings keep their source corner
  posts and now run the source air-only plank support-floor pass one block
  below the intersection instead of floating unsupported over caves.
- Mineshaft corridor support arches now use the Release-era fence-column
  supports: both lower support cells are fences, and the existing random
  roof-plank span still consumes the shared structure placement random.
- Shared structure loot-chest placement now mirrors the Release 1.0
  `StructureComponent` helper guard: generated mineshaft and stronghold chests
  are written only when the target is in the current chunk and is not already a
  chest. Existing chest blocks are left untouched and no weighted-loot draws are
  consumed after the caller's source-order roll-count draw.
- World-backed Overworld generation now runs the Release-era one-time creature
  population pass from the shifted `chunk*16+8` area. It uses the old passive
  animal weights for implemented mobs and stages generated mobs through the
  world entity queue. Its weighted creature-type choice now follows
  `SpawnerCreature` by consuming the world's random stream, while group size,
  start position, group drift, and yaw remain on the chunk population random.
  Its top solid/liquid scan, support predicate, fluid checks, and collision
  volume checks now read the post-decoration scratch, matching the source
  order where `SpawnerAnimals.performWorldGenSpawning` runs after decoration
  and may sample the shifted spawn area outside the target chunk.
  The deterministic CraftZero proxy now lazily primes that world stream with
  the two startup draws needed before the first one-time creature pick, keeping
  the locked source-style spawn fixtures on the intended stream position.
  Forest/taiga wolf selections and mushroom-island
  mooshroom selections now create visible mobs through `MobFactory`; wolves use
  the wolf texture/model path and mooshrooms use the red cow texture on the cow
  model path. The forest and taiga wolf spawn entries are now split to match the
  Release 1.0 source weights: forest wolf weight 5, taiga wolf weight 8, both
  with 4..4 group sizes. The general seed `38`, chunk `(8,0)` live-spawn
  fixture now asserts the corrected source-weight-selected cows, and the taiga
  live-spawn fixture now uses seed `5`, chunk `(-29,28)`, where the source world
  RNG selects wolves. The mushroom-island fixture now uses seed `5`, chunk
  `(-1877,-349)` and expects seven mooshrooms,
  matching the corrected separation between the world RNG type pick and the
  population RNG group/position stream.
  One-time creature population now also initializes generated sheep with the
  Release-era weighted fleece-color roll instead of leaving all chunk-populated
  sheep white.
  The worldgen spawn support predicate is now mob-aware:
  mooshrooms validate against mycelium instead of inheriting the ordinary
  grass-only passive support rule, so mushroom-island population no longer
  succeeds only after group drift reaches neighboring grass.
  The spawn-space predicate now uses each generated mob's actual collision box
  plus a fluid-volume scan instead of requiring literal air at the center feet
  and head cells. Worldgen cows, wolves, and mooshrooms can therefore appear in
  collision-free tall grass/flower ground cover like Release-era animals while
  still rejecting water and real body obstructions.
- Snow finishing now follows the shifted Release 1.0 `chunk*16+8` population
  origin loop and applies only the columns that intersect the generated chunk.
  It now uses the source top-solid-or-liquid height, freezes exposed
  frozen-biome water in the late finishing pass, and preserves the Release 1.0
  rule that snow is not placed directly on ice. The final freeze/snow checks
  now also use a generation-local block-light snapshot: `canBlockFreeze` only
  freezes level-0 water below block light 10, and `canSnowAt` rejects snow
  placement at block light 10 or higher. The snapshot now uses a no-emitter
  fast path, so ordinary chunks avoid allocating and flood-filling a block-light
  array during final snow/ice finishing. The freeze gate now matches the source
  `isBlockHydratedDirectly` shape by not requiring air above the water; level-0
  water can freeze even under a non-blocking cover such as a lily pad, and the
  generator now mirrors the source `setBlockWithNotify` side effect that removes
  a lily pad once its water support has been replaced by ice.
- Overworld biome decoration now runs the sand/clay/sand underwater disk passes
  before tree placement, matching the first surface passes in
  `BiomeDecorator`. Sand and clay disks now use the source `World.f(x,z)`
  floor-water height: the first water block directly above the solid lake/ocean
  floor, not the top water surface. Disk placement now reads the same source
  scratch that contains replayed lake side effects, writes valid off-target
  sand/clay replacements back into that scratch, and hands the mutated scratch
  to tree/huge-mushroom/detail decorators so later source-ordered features can
  see earlier disk terrain changes across chunk boundaries. This pass
  re-audited the local `WorldGenSand` and `WorldGenClay` sources: both return
  before consuming the radius RNG unless the chosen center block is water, sand
  replaces only dirt/grass through vertical radius 2, and clay replaces only
  dirt/clay through vertical radius 1, matching the current chunk-safe disk
  implementation.
- Mushroom-island huge mushroom attempts now run in the source phase after
  tree placement and before small flowers/grass/reeds/pumpkins/cactus/springs.
  They are gated to mushroom island and mushroom shore population origins,
  reuse the same per-origin decorator random after disk placement, and can
  spill into neighboring chunks without re-running the disk pass. Generated
  huge mushroom bodies now follow `WorldGenHugeMushroom` type/height random
  order, support-to-dirt replacement, brown radius-3 cap silhouette, red
  tiered cap silhouette, the layer-relative red-cap metadata-0 interior gate,
  and stem/cap metadata values. Mushroom block rendering
  now uses `BlockMushroomCap` metadata texture rules for brown/red cap faces,
  pore/interior faces, and stem faces.
- Overworld detail decorators now use source-shaped passes and biome-specific
  counts for yellow/red flowers, tall grass, dead bushes, water lilies,
  mushrooms, reeds, pumpkins, cactus, and water/lava springs. These passes use
  the Release-era random-y starts and local scatter attempt counts from
  `WorldGenFlowers`, `WorldGenGrass`, `WorldGenDeadBush`, `WorldGenWaterlily`,
  `WorldGenReed`, `WorldGenPumpkin`, and `WorldGenCactus`, while still applying
  only the blocks that intersect the generated chunk. Generated tall grass now
  also uses the Release 1.0 `WorldGenGrass` metadata value `1` in every biome
  instead of producing taiga ferns, and the metadata path no longer consumes a
  biome-specific taiga random draw. Reed and cactus scatters now preserve the
  source RNG height draws after their initial empty/water gates, even when
  later per-block support checks reject placement. Generated cactus support now
  uses the source `BlockCactus` material-adjacency predicate, so non-colliding
  but material-blocking neighbors such as redstone wire reject cactus growth.
  Red mushroom attempts inside the per-biome mushroom loop now draw their start
  coordinates in the source order, x then z then y, instead of x/y/z.
  Biome-counted reed attempts now also use the Release 1.0 decorator's x/z/y
  origin draw order while the always-ten reed pass keeps its separate x/y/z
  source order. Generated lily pads now use the source `BlockWaterlily` support
  rule, requiring water material with metadata `0` below the pad instead of
  accepting flowing water levels. Generated flowers, tall grass, and dead bushes
  now also use the Release-era `BlockFlower` stay light gate, requiring sky
  access or block light level 8+; ordinary flowers/tall grass accept farmland
  support as the source `BlockFlower` support predicate does. This pass
  re-audited the local `WorldGenReed`, `WorldGenCactus`, `WorldGenPumpkin`, and
  `WorldGenLiquids` sources against CraftZero's detail scatter/spring helpers;
  no new RNG-order or placement-gate mismatch was found.
- The later-era desert-well decorator path was removed. The Release 1.0
  `BiomeDecorator` source has no desert-well pass, and a deterministic desert
  fixture now verifies the previous sandstone/slab/water well footprint is not
  generated.
- Overworld ore starts now use the Release-era raw population origin
  (`origin + nextInt(16)`) from `BiomeDecorator`'s ore helper instead of the
  later decorator feature start. The `WorldGenMinable` vein body then applies
  the source `+8` X/Z center offset before interpolating blobs, so ore, dirt,
  gravel, and lapis veins no longer generate eight blocks northwest of their
  Release-era positions. Chunk-safe ore replay now includes the west,
  north, northwest, and current population origins that can spill shifted
  `WorldGenMinable` veins into the target chunk, without adding future
  east/south origin side effects to the decorator scratch. Ore start RNG now
  also follows the source helper draw order: x, y, z for normal ores and x,
  y1, y2, z for lapis, and the vein endpoint/radius math uses the old
  65,536-entry `MathHelper` sine table with the Release `3.141593F` constant.
  World-backed Overworld ore generation now starts from the population RNG
  after replaying the source lake branches and the eight dungeon attempts for
  each origin, rather than restarting at the beginning of the per-chunk
  population stream.
- Off-target ore side effects are now replayed into the decorator source
  scratch after lake/dungeon replay and before sand/clay disks, trees, huge
  mushrooms, detail scatters, and liquid springs. Later source-ordered
  decorators can now see neighboring `WorldGenMinable` stone-to-ore mutations
  instead of treating every off-target cave wall as pre-ore stone.
- Overworld sand/clay disk decorators now start from that same post-ore
  population RNG stream instead of a separate salted decorator stream. This
  matches the `BiomeDecorator` phase boundary where the ore helper runs before
  sand, clay, sand, tree, huge mushroom, detail, and spring decorators.
- Overworld decorator streams now replay the source tree phase after the
  sand/clay/sand disk passes and before huge mushrooms, detail scatters, cactus,
  and liquid springs. The replay consumes the Release 1.0 bonus-tree roll,
  biome-specific tree counts and tree-kind selection, plus source-shaped
  generator RNG costs for normal, birch, big oak, swamp, and taiga tree
  attempts. Successful replayed normal, birch, big oak, swamp, and taiga
  attempts now also mutate a lightweight source-tree scratch overlay with dirt
  supports, leaves, logs, branches, and swamp vines, so later replayed tree
  attempts can see earlier tree obstructions before the huge mushroom/detail
  streams continue. Big-oak replay now includes source-style leaf-node
  collision filtering before writing its final leaf disks, trunk, and branch
  lines into the scratch overlay. The replay overlay is now applied to the
  target chunk before huge mushrooms and detail decorators, so generated visible
  normal, birch, big oak, swamp, and taiga trees come from the source-shaped
  decorator attempts instead of the old salted `FeaturePlanner` render pass.
- The tree replay scratch overlay now also replays source-shaped water/lava lake
  side effects around the target chunk before tree attempts. Off-target lake
  water/lava, air cavities, restored grass/mycelium, frozen lake surfaces, and
  lava stone shells can now affect cross-border tree support and obstruction
  checks, while the target chunk's already-generated ore/dungeon state remains
  authoritative.
- The same source scratch overlay is now threaded through the later huge
  mushroom, flower/grass/bush/lily, mushroom, reed, pumpkin, cactus, and spring
  decorator reads. Those late passes can now see off-target replayed trees and
  lakes for obstruction, support, top-height, water-adjacency, and spring
  side-count checks instead of falling back to undecorated base terrain outside
  the target chunk.
- Scratch-backed late decorators can now accept and record valid off-target
  placements in the source overlay while still writing only intersecting blocks
  into the generated chunk. This restores source-style spill behavior and RNG
  side effects for cases such as a huge mushroom stem outside the target chunk
  whose cap intersects the target.
- Generated swamp tree vines now store the Release 1.0 vine side bitmask
  metadata used by `WorldGenSwampTree`. `BlockShape` now decodes those source
  bitmasks when support is present, while preserving the existing face-constant
  fallback used by player-placed vines.
- The old salted `FeaturePlanner` small-feature path was removed; it no longer
  places early generic flowers, mushrooms, or cactus outside the source
  decorator order.
- Normal tree candidates now use the Release `WorldGenTrees` leaf layer range,
  source-style random corner skips, no extra top leaf layer, and source-style
  support-to-dirt replacement. The chunk-safe candidate stores the corner-skip
  mask so neighboring chunks render the same tree crown consistently.
- Big oak candidates now use the Release `WorldGenBigTree` seed handoff,
  internal 5..16 height selection, height attenuation, leaf-node list, circular
  leaf disks, trunk line, and branch log lines instead of the previous tall
  normal-tree stand-in.
- Taiga tree candidates now use source-derived `WorldGenTaiga1` and
  `WorldGenTaiga2` conifer shapes instead of broad normal-tree crowns with
  spruce metadata. Generated spruce trunks/leaves use metadata 1, conifer
  crowns leave the broad normal-tree corners empty, and generated roots turn
  the support block to dirt like the source tree generators.
- Swamp tree candidates now use the Release `WorldGenSwampTree` height range,
  water-root lowering, source leaf-layer radii, random lower-corner leaves,
  top-corner suppression, support-to-dirt replacement, and four-block hanging
  vine runs. Vine attempts now replay a per-tree Java `Random` stream in the
  source leaf-order and west/east/north/south side-order, so chunk neighbors
  agree on crossing crowns while preserving the Release 1-in-4 side-attempt
  shape.
- Small Overworld mushroom decorators now use the Release-era stay rule shape:
  mycelium always permits placement, while ordinary opaque support requires
  overhead cover and generated full-block light below 13 instead of accepting
  open-sky or torch-lit grass, dirt, stone, or planks.
- Source-height checks used by Overworld decorators now ignore soft plant blocks
  and only stop on solid/fluid terrain. Generated flowers and grass can still
  see sky through soft plants above them, while generated mushrooms no longer
  treat tall grass or other non-height decoration as valid shade.
- Overworld density blending now samples the low-resolution generation biome
  layer and uses the Release 1.0 provider's linear stepping through 4x8x4 cells
  instead of final Voronoi biomes or eased/smoothstep interpolation.
- Biome terrain height constants are now pinned against the Release 1.0 source
  values for every generated Overworld biome in the layer stack: ocean, plains,
  desert, extreme hills, forest, taiga, swampland, rivers, frozen variants, ice
  mountains, mushroom island, and mushroom shore. Extreme Hills and Ice
  Mountains both use max height `1.8F`, which raises the locked Extreme Hills
  sample from y=100 to y=109.
- Overworld min-limit, max-limit, and selector density noises now use an
  explicit single-point 3D octave sampler so they keep Y-dependent Perlin noise
  instead of falling through the old `ySize == 1` 2D fast path.
- Exact source-derived sample vectors now lock representative final Voronoi
  biomes, terrain-generation biomes, terrain top heights, top blocks, and
  sea-level blocks across forest, plains, ocean, ice plains, extreme hills,
  mushroom island, and mushroom shore samples.
- Raw Overworld density fixtures now lock representative Release 1.0 4x8x4
  provider grid nodes and an interpolated in-cell sample across forest/plains,
  ocean, ice plains, extreme hills, mushroom island, and mushroom shore terrain.
  These values exercise the source vertical density math, depth noise,
  min/max/selector noise blending, generation-biome weighted height blending,
  and linear cell interpolation below the visible terrain-height fixtures.
  Whole-grid hashes now also lock complete 5x17x5 low-resolution density arrays
  across multiple chunks and two seeds, including mushroom island regions.
- Overworld density caching now keys the full low-resolution source grid
  coordinates instead of packing truncated X/Z fields. Far terrain samples no
  longer alias every 2,097,152 density cells and reuse unrelated source noise.
- The End spike placement was checked against source-adjacent logic. The current
  1-in-5 chunk-origin spike attempt is consistent with the old decorator shape.
  End decorator random streams now advance through the inherited
  `BiomeDecorator.generateOres()` pass before the spike branch, matching the
  Release 1.0 `BiomeEndDecorator` call order even though those ore attempts do
  not replace End Stone.
  Generated End crystals now consume and store the `WorldGenSpikes` random yaw
  only after the source height/radius draws and footprint validation succeed,
  spawn at the same Y coordinate as the generated cap, and then maintain fire at
  that floored cap coordinate during End ticks, matching the old source entity
  update path. The bedrock cap is applied after staging the crystal to match the
  source call order. The origin End dragon now also receives the
  `BiomeEndDecorator` population-random yaw after reproducing any preceding
  spike RNG consumption in chunk `(0, 0)`. Null-world End chunks now still
  generate obsidian spike columns and bedrock caps while skipping only the live
  crystal entity staging.
  Active dragon healing from those crystals now has renderer feedback: the
  current healing crystal exposes a crystal-center to dragon-body beam that is
  drawn as an animated multi-segment world-line tether driven by crystal
  rotation.
- The End density field was checked against `ChunkProviderEnd`. Its dead
  intermediate island-depth values are intentionally preserved, and deterministic
  fixture coverage now locks the origin island, radial falloff, far-void chunk
  shape, raw source-vector density samples from those same regions, and
whole-grid density hashes including an alternate-seed origin. Base-chunk
fixtures now also cover negative falloff, diagonal rim, and alternate-seed rim
chunks, with matching raw density vectors/hashes for those regions.
- End portal transfers now target the Release 1.0 fixed entry point at
  `(100.5, 49.0, 0.5)`, and entry platform restoration rebuilds the 5x5
  obsidian layer at y=48 while clearing exactly three blocks of headroom above
  it. The fixture pre-fills the footprint to prove re-entry replaces the
  platform, clears the source-shaped air volume, and stays inside the 5x5
  footprint.
- Raw Nether density fixtures now lock representative Release 1.0 5x17x5
  provider grid samples and whole-grid hashes for origin, negative-coordinate,
  far positive-coordinate, and alternate-seed chunks. These vectors exercise
  bottom/top density tapering, mid-height min/max/selector blending, and
  coordinate-dependent Hell terrain variation below the visible
  block-count/decorator fixtures.
- Nether population order now follows the source `ChunkProviderHell` decorator
  sequence more closely:
  - Nether decorator random streams now start from the source fixed chunk seed
    (`chunkX * 341873128712 + chunkZ * 132897987541`) after replaying the
    surface-replacement RNG consumed during `provideChunk`, rather than using
    the Overworld population seed mixer.
  - Nether surface replacement now uses the source gravel-noise call site with
    Y coordinate `109`. The underlying octave grid still preserves the
    Release-era `ySize == 1` two-dimensional fast path, so the call is
    source-shaped while single-layer noise remains Y-insensitive.
  - Eight lava spring attempts.
  - Lava springs now use the `WorldGenHellLava` five-neighbor gate: target
    air-or-netherrack, netherrack above, exactly four netherrack neighbors and
    one air neighbor across the four horizontal sides plus the block below.
    Successful springs now apply the source immediate tick: the spring itself
    becomes still lava metadata `0`, then the single open neighbor receives
    flowing lava metadata `8` when below or `1` when horizontal.
  - Fire count uses `nextInt(nextInt(10) + 1) + 1`.
  - Each fire attempt now replays `WorldGenFire`'s 64 local scatter candidates
    with x/y/z coordinate draws before the glowstone RNG stream continues.
  - First glowstone pass uses `nextInt(nextInt(10) + 1)` attempts at y 4..123.
  - Second glowstone pass uses ten attempts at y 0..127.
  - `WorldGenGlowStone1` scatter behavior is now pinned: accepted seeds require
    netherrack above an air target, each cluster consumes 1500 local candidates
    with x/y/z draw order, and growth only accepts candidates touching exactly
    one existing glowstone block. This pass re-audited the local/DI9
    `WorldGenGlowStone1` and `WorldGenGlowStone2` bodies and confirmed that the
    two generator classes share the same scatter/growth algorithm in this
    Release-era code; the decorator-level attempt count and y-range are the
    meaningful differences.
  - Nether decorator replay now uses a mutable scratch overlay for source-order
    writes outside the target chunk. Later lava, fire, glowstone, and mushroom
    attempts can see earlier off-target decorator blocks while only intersecting
    blocks are written into the generated chunk.
  - Population no longer runs extra 16-attempt gravel and soul-sand patch loops;
    those materials come from the Nether surface replacement noise pass.
  - Brown and red mushroom placement keeps the old `nextInt(1)` RNG consumption
    before each always-true placement branch, then replays `WorldGenFlowers`'
    64 local x/y/z scatter candidates for mushroom placement. Nether mushrooms
    now also use the source support rule shape for generation: mycelium or
    opaque support blocks at generated block light below 13, with soul sand
    preserved as a Release-era support despite its shortened collision shape.
    Glowstone now emits level-15 light, so generated mushroom stay checks can
    reject bright current-chunk Nether placements after glowstone decoration;
    the same stay checks now read world-coordinate light from the mutable
    Nether decorator scratch, so off-target glowstone can reject off-target
    scratch-backed mushroom candidates too.
- Nether base chunk generation now runs a dedicated Release-era Hell cave carver
  after surface replacement and before decorators. It uses the Hell cave node
  frequency, y 0..127 starts, 0.5 vertical scale, lava-contact aborts, and
  carve-to-air behavior instead of the Overworld cave generator's water abort
  and low-y lava fill. Direct carver coverage now also locks source-audited
  full block-output hashes for seeds `515151`, `1234`, and `987654321` across
  origin and shifted chunks.
- Generated Nether base chunk coverage now compares terrain plus source surface
  replacement before caves against full base generation after Hell cave carving.
  Seed 515151 chunk (0,-4) locks 290 netherrack-to-air cave cuts, preserves 48
  soul-sand/gravel surface-patch blocks, and proves the pass is subtractive.
  Seed 1234 chunk (7,-8) adds an independent trace with 352 cave cuts, 152
  preserved soul-sand/gravel patch blocks, 1298 retained lava blocks, and
  source-shaped randomized bedrock counts.
- Overworld cave nodes, Nether cave nodes, and ravine nodes now use the
  Release-era `MathHelper` sine table for radius curves, yaw/pitch movement,
  and source-shaped boundary carving instead of Java's direct `Math.sin/cos`.
- Nether fortress pieces are now placed before Nether decorators in the chunk
  pipeline, matching the source order more closely. Fortress starts now use the
  old bridge origin anchor, the source-style `StructureNetherBridgeStart`
  vertical band, a 19x10x19 start crossing, and source orientation-specific
  three-branch start anchors instead of the old fixed four-arm proxy layout.
  Fortress generation now drains a recursive source-style component queue:
  primary/secondary weighted lists, max counts, last-piece repeat suppression,
  source depth cutoff, 112-block branch-distance cap, end-cap fallback pieces,
  and post-graph `setRandomHeight(48, 70)` vertical offset are represented. The
  implemented bridge, crossing, stair, corridor, entrance, blaze-throne, end-cap,
  and nether-stalk-room block placements are source-shaped, including the open
  upper throne room, fences, stairs, soul-sand beds, wart rows, immediate
  lava-well source cap/falling shaft, and downward support fills through air,
  still fluids, and flowing fluids until they hit solid terrain.
  Fortress local block placement also carries the source `coordBaseMode`
  transform, including `StructureComponent.getMetadataWithOffset`-style stair
  metadata rotation. Random-consuming fortress piece placement now consumes the
  shared Nether population random stream. Current-chunk Nether decorators
  continue from the real fortress placement stream, and shifted neighbor-origin
  decorator replay now advances its scratch population stream through fortress
  placement before lava/fire/glowstone/mushroom attempts.
- Nether lava fluid ticks now use the Release-era Hell-world horizontal decay
  step. Lava still spreads two metadata steps outside the Nether, but spreads
  one metadata step inside the Nether as in the source `isHellWorld` branch.
- Nether fortress proxy pieces no longer generate loot chests. Fortress chests
  were added after Java Release 1.0, so the Release 1.0 target keeps blaze
  spawners, nether wart gardens, soul sand, fences, and nether brick structure
  blocks but omits fortress loot chests.
- The general Nether hostile mob table no longer includes blazes. Release 1.0
  blaze availability remains tied to fortress behavior and generated blaze
  spawner blocks, rather than allowing ordinary Nether cave/floor spawns
  everywhere.
- Nether hostile spawning now switches to a fortress-specific mob list when the
  spawn coordinate is inside a generated fortress piece. The list follows the
  old `MapGenNetherBridge` shape: blazes, zombie pigmen, and magma cubes, while
  ordinary Nether terrain still uses the general Hell biome table.
- Stronghold proxy shells now use the Release-era stone selector shape for
  shell blocks: cracked stone brick, mossy stone brick, stone-brick monster
  eggs, and regular stone brick metadata are generated deterministically, while
  corridor pieces avoid monster eggs. Random-consuming stronghold piece
  placement now consumes the shared structure placement random stream when one
  is supplied by chunk population.
- Generated stronghold proxy starts now follow the source root placement more
  closely: the root layout is anchored at `(chunk << 4) + 2` on X/Z, built from
  Y=64, uses the source root-orientation draw to rotate the current proxy piece
  graph and each oriented piece's `coordBaseMode`, and shifts the final piece
  set with the `StructureStart.markAvailableHeight(..., 10)` formula. The root
  now also forces the first `ComponentStrongholdCrossing` child from the source
  normal exit, consumes that crossing's source door/side-opening constructor
  RNG without spending a weighted crossing-hall quota slot, and routes the
  simplified main path after that crossing through the source
  `getNextComponentNormal/X/Z` access-point transforms instead of old
  root-relative offsets. The fixed room-crossing proxy likewise no longer
  consumes a weighted room-crossing quota slot, and the optional fixed chest
  corridor and prison proxies no longer spend their limited weighted-room
  quotas. Optional side openings from that first crossing now
  get source-anchored straight branch roots when their constructor booleans are
  set and the root does not collide with the existing proxy pieces, and those
  roots consume/store the source straight-piece door and side-opening
  constructor RNG after the source normal child is created, matching
  `ComponentStrongholdCrossing.buildComponent` child order and source child
  component depth. The first generated straight connector also creates
  source-anchored optional X/Z branch roots from its own
  `ComponentStrongholdStraight` constructor booleans after its source normal
  child is created, with the source child component depth. The main
  room-crossing side exit now participates in
  the same branch queue as well: if the optional chest branch occupies that exit
  its chest corridor can continue recursively, otherwise the exit creates a
  source-anchored straight branch root. Those optional and room-crossing roots
  feed a source-depth weighted side-branch queue that can add additional
  source-shaped straights, left/right turns, standalone stairwells,
  straight-stairs, crossing halls, room crossings, prisons, chest corridors,
  depth-gated libraries, and depth-gated portal rooms while honoring
  collision, depth, branch-distance guards, and the source no-immediate-repeat
  rule for the previously selected weight, including straight connectors.
  Weighted stronghold components now validate their source bounding boxes
  before consuming constructor door/variant RNG, and libraries try the source
  large room before falling back to the small room instead of randomly choosing
  a height. A later pass removed the guaranteed portal/library route and now
  retries the source-recursive stronghold assembly until a weighted portal room
  exists.
- Non-portal stronghold pieces now honor the same source liquid-envelope abort
  already used by mineshaft pieces, so flooded/ocean intersections can leave
  Release-style skipped rooms or corridors in the target chunk. The portal room
  intentionally bypasses that guard and still places its chamber, frames, lava,
  and silverfish spawner.
- Stronghold locating now follows `ComponentStrongholdStairs2#getCenter` for
  generated proxy starts by returning the portal-room center when a portal room
  exists, instead of returning the center of the whole stronghold bounding box.
- Overworld village and mineshaft locating now scans nearby generated start
  origins and returns the center of the same start graph used by chunk
  population. Locate-style callers no longer see villages/mineshafts as
  unavailable while the terrain generator can still create them.
- Stronghold source-door variants are now implemented for generated proxy
  pieces that carry source door state and for direct piece fixtures that
  request it: open passages, framed wooden doors, iron-bar grates, and framed
  iron doors with source-positioned stone buttons. The generated proxy now also
  consumes and stores the source constructor room-crossing variant draw instead
  of choosing the room-crossing variant during block placement.
- Stronghold start pieces now use the source 5x11x5 `ComponentStrongholdStairs`
  layout: upper and lower openings, randomized stronghold shell, and the exact
  stone-brick/slab stair run. The simplified generated proxy is oriented so its
  lower exit feeds the east corridor. Weighted generated side branches can now
  also roll the separate source stairwell piece with the same 5x11x5 bounds,
  randomized door state, door-aware upper entrance, and lower branch exit.
- Stronghold straight connector pieces now use the source 5x5x7
  `ComponentStrongholdStraight` layout: front/rear openings, randomized shell,
  randomized torch attempts, and optional side openings stored as component
  state. The simplified generated proxy now uses a source-sized straight piece
  between the start stairwell and the longer corridor tube.
- Stronghold straight-stairs pieces now use the source 5x11x8
  `ComponentStrongholdStairsStraight` layout: upper/lower openings, randomized
  stronghold shell, six rows of old cobblestone stairs, and the stone-brick
  support run beneath the first five steps.
- Stronghold left/right turn pieces now use the source 5x5x5
  `ComponentStrongholdLeftTurn`/`ComponentStrongholdRightTurn` layouts:
  randomized stronghold shell, front opening, and orientation-dependent side
  opening on the correct turn wall.
- Stronghold crossing-hall pieces now use the source 10x9x11
  `ComponentStrongholdCrossing` layout: upper front opening, lower rear
  opening, four optional side openings stored as component state, randomized
  internal stronghold-stone platforms/walls, stone slab tiers, double-slab upper
  platform, and central torch.
  Generated side branches can now also roll those crossing halls through the
  weighted stronghold component table, preserve their randomized door/opening
  constructor state, and recurse from their rear and optional side exits.
- The stronghold portal-room proxy now uses the source chamber block layout:
  11x8x16 oriented bounds, grate doorway, raised stone-brick stair approach,
  12 correctly facing End portal frames, moving-lava side pools and center pool,
  side/back iron bars, and the silverfish spawner placed after the stair run at
  the source local coordinate.
- Stronghold chest-corridor pieces now use the source 5x5x7 local block layout:
  front/rear openings, the side stone-brick bench, stone-brick slab shelves
  with metadata 5, and the loot chest at local `(3,2,3)` instead of the old
  generic center-floor chest. The chest contents now also use the source
  weighted stronghold corridor loot table and source `2..3` roll count instead
  of the generic equal-choice chest helper.
- Stronghold prison pieces now use the source 9x5x11 cell layout: front/rear
  openings, source-positioned stone partitions, crossing iron-bar dividers,
  cap bars, and the two inner iron doors with direction-adjusted lower/upper
  metadata.
- Stronghold library pieces now use the source large-room block layout:
  14x11x15 oriented bounds, the front opening, randomized cobweb pass, side
  plank/bookshelf runs, interior shelf rows, balcony planks/fences,
  direction-adjusted ladder metadata, the central fence/torch fixture, and the
  two source-positioned library loot chests. Library chest contents now use the
  source weighted book/paper/map/compass table and source `1..4` roll count.
- Stronghold room-crossing pieces now use the source 11x7x11 local block
  layout: front/rear/side openings, source random room variants, the plain
  stone-brick pillar variant, the moving-water fountain variant, and the
  cobblestone/plank balcony variant with direction-adjusted ladder metadata and
  source-positioned loot chest. Balcony chest contents now use the source
  weighted room-crossing table and source `1..4` roll count.
- Stronghold corridor connector pieces now use the source open-ended tube
  layout from `ComponentStrongholdCorridor`: 5-block cross-section, 5-block
  height, plain stone-brick floor/ceiling/walls, air interior, and no end caps
  along the corridor length.
- The simplified mineshaft start now begins with a source-shaped root room and
  grows a bounded source-style corridor/cross/stairs graph. The room uses the
  Release-era random size, non-air-only dirt floor replacement, lower air
  clearing, child-opening clearing, rare upper air pocket, and
  `markAvailableHeight(..., 10)` vertical shift. The rare upper pocket now
  follows the deterministic source
  `randomlyRareFillWithBlocks` ellipsoid without the old extra seeded chance
  gate, and room child branch Y selection now uses the source inclusive
  `getYSize() - 4` range. The graph uses the source 70/10/20
  corridor/stairs/cross component
  weighting, depth cap, 80-block branch-distance cap, overlap rejection, and
  child-coordinate rules. Corridor continuation recursion now preserves the
  source RNG order by drawing the `nextInt(4)` branch choice before the
  `nextInt(3)` Y offset, and corridor block placement now consumes the shared
  structure placement random stream when one is supplied by chunk population.
  Room, corridor, crossing, and stairs pieces now abort before carving when the
  source expanded component envelope touches liquid in the target chunk.
  Corridors keep the 3-wide/3-high carved passage, section
  supports every five blocks, no synthetic plank floor below unsupported air,
  source-order randomized ceiling clearing, split support caps that place only
  the two source side roof planks at the support axis, optional rails only on
  supported center floor, source-style web/torch/chest rolls, source-order
  spider-web random-fill placement, source weighted chest loot with the Java
  `3..6` roll count, and chunk-bounds-gated cave-spider spawner placement for spider
  corridors. Structure spawner placement now preserves the shared placement
  random and keeps the source tile-entity default delay instead of drawing a
  generated delay during chunk population. Crossings and stairs now emit
  source-shaped air spaces and supports, and stairwell pieces now use the
  Release-era five-slice descent with local lower bound
  `5 - i - (i >= 4 ? 0 : 1)`, without over-carving the block below each step.
- Generated Release 1.0 Overworld, Nether, and End chunks now pass mesh-buffer
  sanity checks with linked neighbors: buffers are non-empty, finite,
  index-consistent, UVs stay inside the terrain atlas, and vertex positions stay
  within the target chunk bounds. Structure-heavy generated fixtures now also
  cover Overworld seed `1` chunk `(-45, 38)`, Nether seed `1` chunk `(7, -8)`,
  and an End edge chunk.
- Chunk-side mesh lookups now resolve one-ring diagonal neighbors for block
  type, metadata, sky light, and block light. Fluid meshes now have a regression
  for diagonal chunk-corner height sampling, direct solid border faces are
  checked for cross-chunk culling, and generated Overworld/Nether/End chunks are
  compared against open-neighbor meshes to catch added seam faces.
- Alpha-tested utility blocks such as rails, ladders, torches, redstone wire,
  repeaters, doors, trapdoors, bars, panes, signs, levers, buttons, and brewing
  stands now route to the cutout mesh layer instead of the opaque layer.
- Crossed plant sprites now use the Release-style `drawCrossedSquares` 0.45
  half-width, so flowers, saplings, mushrooms, crops, sugar cane, and nether
  wart render from local `0.05..0.95` instead of the narrower prior shape.
- Tall grass now uses the source coordinate-based render jitter constants before
  crossed-sprite bounds are emitted, and cobweb now routes through the same
  Release-style crossed-sprite mesh path instead of cube fallback geometry.
- Tall grass now also uses soft plant shape semantics instead of solid-block
  fallback: empty collision, partial selection, grass/dirt support, falling-block
  pass-through, replacement, and tree-trunk/leaf replacement all match the
  Release-era ground-cover behavior more closely.

## Tests Run

Latest verification after the stronghold MapGenStructure start-RNG update:

```powershell
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdStartUsesSourceSizedStairs" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdUsesSourceFirstCrossingChild" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdStartsRetrySourceRecursionUntilPortalRoomExists" --tests "com.craftzero.world.StructureGeneratorTest.strongholdPlacementUsesReleaseOneBiomeReservoirSearch"
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest"
.\gradlew.bat test --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.StructureGeneratorTest"
```

Latest verification after the village path candidate-order update:

```powershell
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.villagePathDistanceGuardRunsBeforeLengthSearch"
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest"
.\gradlew.bat test --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.StructureGeneratorTest"
```

Latest verification after the village sizeable-start gate update:

```powershell
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.villagePlacementUsesReleaseOneGridAndBiomeGate" --tests "com.craftzero.world.StructureGeneratorTest.villageBiomeGateUsesGenerationLayer" --tests "com.craftzero.world.StructureGeneratorTest.locateVillageMatchesGeneratedStart" --tests "com.craftzero.world.StructureGeneratorTest.villagePathQueueAttachesImplementedPiecesWithoutTorchSpam" --tests "com.craftzero.world.StructureGeneratorTest.nonSizeableVillageStartsDoNotGenerateOrSuppressLakes"
```

Latest verification after the stronghold corridor stone-palette update:

```powershell
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.strongholdCorridorUsesSourceOpenEndedTubeLayout"
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.strongholdStonesUseReleaseOneVariantMetadata" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdCorridorsUseSourceTubeDimensions"
```

Latest verification after the stronghold randomized-fill source-order update:

```powershell
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.strongholdRandomizedShellsUseSourceLocalXBeforeZ" --tests "com.craftzero.world.StructureGeneratorTest.strongholdChestCorridorUsesSourceShelfLayout" --tests "com.craftzero.world.StructureGeneratorTest.strongholdLibraryUsesSourceLargeRoomLayout" --tests "com.craftzero.world.StructureGeneratorTest.strongholdRoomCrossingUsesSourceBalconyLayout"
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest"
.\gradlew.bat test --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.StructureGeneratorTest"
```

Latest verification after the red huge-mushroom cap interior update:

```powershell
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.redHugeMushroomCapsKeepSourceUpperInteriorCells"
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest"
.\gradlew.bat test --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.StructureGeneratorTest"
```

Latest verification after the generated stronghold source-recursive restart update:

```powershell
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.generatedStronghold*" --tests "com.craftzero.world.StructureGeneratorTest.locateStrongholdMatchesGeneratedPortalRoom"
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest"
.\gradlew.bat test --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.StructureGeneratorTest"
```

Latest verification after the generated stronghold weighted portal-room update:

```powershell
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdPortalRoomsObeySourceDepthGate" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdSideBranchesCanRollPortalRooms" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdWeightedExpansionStopsAfterLimitedRoomsExhausted"
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.locateStrongholdMatchesGeneratedPortalRoom" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdMainPathUsesSourceChildAccessesForEveryRootOrientation" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdSideBranchesExpandIntoWeightedRooms" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdSideBranchesCanRollCrossingHalls" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdSideBranchesCanRollStairwellPieces"
```

Latest verification after the stronghold liquid-envelope placement update:

```powershell
.\gradlew.bat test --rerun-tasks --tests "com.craftzero.world.StructureGeneratorTest.strongholdPiecesAbortWhenLiquidTouchesSourceEnvelope" --tests "com.craftzero.world.StructureGeneratorTest.strongholdPortalRoomIgnoresLiquidEnvelopeAbort" --tests "com.craftzero.world.StructureGeneratorTest.strongholdPortalRoomUsesSourceChamberBlocks"
```

Latest verification after the generated stronghold fallback-corridor/depth-gate update:

```powershell
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdBranchFailuresCreateSourceFallbackCorridors" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdLibrariesObeySourceDepthGate" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdSideBranchesExpandIntoWeightedRooms" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdSideBranchesCanRollCrossingHalls" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdSideBranchesCanRollStairwellPieces"
```

Latest verification after the off-target dungeon decorator-scratch replay update:

```powershell
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.decoratorScratchOverlaysOffChunkDungeonsBeforeLateFeatures" --tests "com.craftzero.world.OverworldGenerationSprintTest.decoratorScratchOverlaysOffChunkOresBeforeLateFeatures" --tests "com.craftzero.world.OverworldGenerationSprintTest.nullWorldOverworldGenerationPlacesDungeonBlocks" --tests "com.craftzero.world.WorldGenerationParityTest.overworldDungeonRandomResumesAfterLakeBranches" --tests "com.craftzero.world.WorldGenerationParityTest.overworldOreRandomResumesAfterLakeAndDungeonPhases" --tests "com.craftzero.world.WorldGenerationParityTest.overworldDecoratorRandomResumesAfterOreHelper" --tests "com.craftzero.world.DungeonFeatureTest"
```

Latest verification after the worldgen creature post-decoration scratch update:

```powershell
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.worldGenCreatureSpawningReadsDecoratorScratchState" --tests "com.craftzero.world.OverworldGenerationSprintTest.worldGenCreatureSpawningUsesCollisionVolume" --tests "com.craftzero.world.OverworldGenerationSprintTest.worldBackedOverworldPopulationRunsCreatureSpawning" --tests "com.craftzero.world.OverworldGenerationSprintTest.worldBackedOverworldPopulationSpawnsWolvesInTaiga" --tests "com.craftzero.world.OverworldGenerationSprintTest.worldBackedOverworldPopulationSpawnsMooshroomsOnMushroomIslands" --tests "com.craftzero.world.OverworldGenerationSprintTest.decoratorScratchOverlaysOffChunkDungeonsBeforeLateFeatures" --tests "com.craftzero.world.OverworldGenerationSprintTest.decoratorScratchOverlaysOffChunkOresBeforeLateFeatures"
```

Latest verification after the natural sheep fleece-color spawn update:

```powershell
.\gradlew.bat test --rerun-tasks --tests "com.craftzero.world.MobSpawnerTest.naturalSheepSpawnsUseReleaseOneWeightedFleeceColors" --tests "com.craftzero.world.MobSpawnerTest.naturalSheepSpawnsUseReleaseOneRarePinkFleeceRoll" --tests "com.craftzero.entity.mob.SheepTest" --tests "com.craftzero.world.MobSpawnerTest.naturalPassiveSpawnsUseReleaseOnePackSizes"
```

Latest verification after the Nether fortress distance-cap depth update:

```powershell
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.netherFortressDistanceCapUsesParentDepthForEndCaps"
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest"
.\gradlew.bat test --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.StructureGeneratorTest"
```

Latest verification after the Nether Hell cave block-output vector update:

```powershell
.\gradlew.bat test --tests "com.craftzero.world.NetherCaveGeneratorTest"
.\gradlew.bat test --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.StructureGeneratorTest"
```

Latest verification after the mineshaft room child-opening block-output update:

```powershell
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.mineshaftRoomClearsSourceChildOpeningVolumes"
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest"
.\gradlew.bat test --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.StructureGeneratorTest"
```

Latest verification after the generated stronghold weighted crossing-hall branch update:

```powershell
.\gradlew.bat test --rerun-tasks --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdSideBranchesCanRollCrossingHalls" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdSideBranchesExpandIntoWeightedRooms" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdSideBranchesCanRollStairwellPieces" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdFirstCrossingUsesSourceBoundsForEveryRootOrientation" --tests "com.craftzero.world.StructureGeneratorTest.strongholdCrossingHallUsesSourceMultiLevelLayout"
```

Latest verification after the generated stronghold invalid-placement selector update:

```powershell
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdWeightedSelectionRejectsImmediateStraightRepeats" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdInvalidWeightedPlacementDoesNotConsumeConstructorRng" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdLibraryFallsBackToSourceSmallBox" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdWeightedSelectionContinuesAfterInvalidPlacement"
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest"
.\gradlew.bat test --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.StructureGeneratorTest"
```

Latest verification after the Nether fortress invalid-placement selector update:

```powershell
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.netherFortressWeightedSelectionContinuesAfterInvalidPlacement" --tests "com.craftzero.world.StructureGeneratorTest.netherFortressStartUsesSourceSizedPieces" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdWeightedSelectionContinuesAfterInvalidPlacement" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdInvalidWeightedPlacementDoesNotConsumeConstructorRng"
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest"
.\gradlew.bat test --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.StructureGeneratorTest"
```

Latest verification after the village queue-order update:

```powershell
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.villageRecursiveQueueProcessesPathsBeforeBuildings" --tests "com.craftzero.world.StructureGeneratorTest.villagePathQueueAttachesImplementedPiecesWithoutTorchSpam"
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest"
.\gradlew.bat test --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.StructureGeneratorTest"
```

Latest verification after the worldgen creature spawn-volume update:

```powershell
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest"
```

Latest verification after the village blacksmith/HOUSE_2 no-chest and village
door metadata fix:

```powershell
.\gradlew.bat compileJava
```

Latest verification after the village side-road same-depth fix:

```powershell
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.villagePathQueueAttachesImplementedPiecesWithoutTorchSpam"
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest"
.\gradlew.bat test --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.StructureGeneratorTest"
```

Latest verification after the Java 1.0 `mc100` biome topology fix that removes
later hills, regular shore/beach, extreme-hills-edge, and swamp-river layers,
source-shaped raw-ocean GenLayer predicate fix, source-shaped GenLayer river
initializer/carver/mixer fix, GenLayerVoronoiZoom cell-origin seeding fix,
Release 1.0 Extreme
Hills/Ice Mountains terrain constant update, desert water-lake fix,
tall-grass metadata fix,
source-shaped huge mushroom body update, huge mushroom texture fix, and
later-era desert-well removal, reed/cactus scatter RNG fix, rejected-lake RNG
fix, red-mushroom and biome reed draw-order fixes, and WorldGenMinable
endpoint/sine-table fix, cave/ravine sine-table fix, and End spike
crystal/dragon-yaw fix, Nether lava spring five-neighbor gate fix, and Nether
fire/mushroom scatter RNG and mushroom support-rule fix, and Nether decorator
RNG stream fix, shifted snow-finishing loop, late finishing-water freeze,
source block-light and source-water gates for final ice/snow, and
dungeon RNG after lake-branch replay, raw ore origin fix, and underwater disk
floor-water height fix, lily-pad source-water support fix, post-dungeon
Overworld ore RNG fix, post-ore Overworld decorator RNG fix, source-shaped
tree-attempt decorator RNG replay before huge mushrooms/details, replay-local
source tree scratch mutation for later tree obstruction checks, and big-oak
scratch leaf-node/trunk/branch mutation, applying source-replayed visible trees
to chunks, source vine bitmask metadata decoding, raw Overworld density
grid-vector fixtures, raw End density grid-vector fixtures, raw Nether density
grid-vector fixtures, Overworld/Nether/End whole-grid density hashes, diagonal
chunk-corner fluid mesh lookup, generated neighbor seam-face regressions,
structure-heavy generated chunk mesh regressions, and stronghold stone-brick
variant selector metadata, Overworld mushroom
light/mycelium stay-rule fix, Nether mushroom generated-light stay gate, and
glowstone light-emission fix, source-height soft-plant sky/shade gate fix,
generated mesh UV bounds, and cutout routing for
alpha-tested utility blocks, and off-chunk lake side effects in the source tree
scratch overlay, and late-decorator reads through that same source scratch
overlay, and scratch-backed off-target decorator placement/spill, and carved
neighbor terrain reads for dungeon room validation/replay, carved lake
  candidate validation, off-chunk lake reads during dungeon validation, and
  mutable source-lake scratch validation/replay, and source scratch reads/writes
  for underwater disk decorators, and off-target ore side effects in the
  decorator source scratch, visible null-world dungeon/structure block
  population, null-world village lake suppression, null-world End spike block
  generation, Overworld worldgen creature population, generated Nether Hell cave
  fixture coverage, full generated-biome height constant coverage, source wolf
  spawn weights, source world-RNG creature selection, the End entry platform
  fix, Release-style crossed plant sprite width, source tall-grass render
  jitter, cobweb crossed-sprite routing, and tall-grass soft plant shape
  semantics, source-shaped Nether fortress start/piece bounds,
source-shaped fortress blaze-throne/nether-stalk-room block placement,
fortress downward support fills, source three-branch fortress start topology,
oriented fortress local-coordinate/stair-metadata transforms, and recursive
weighted Nether fortress component generation, source shared-random Nether
fortress piece placement, source-order Nether decorator RNG after fortress
placement, source-order Overworld population RNG after structure placement,
source mineshaft-village-stronghold Overworld structure pass ordering,
carved-terrain Overworld structure RNG replay for mineshaft liquid aborts,
fortress entrance immediate lava-well flow, flowing-fluid support
replacement, and Nether lava one-step Hell-world decay,
source-shaped stronghold portal-room chamber blocks,
source-shaped stronghold chest-corridor shelves/chest placement,
source-shaped stronghold prison cell layout, and
bounded source-style mineshaft corridor/cross/stairs graph generation,
source-order mineshaft corridor continuation RNG,
source shared-random mineshaft corridor block placement,
source-weighted mineshaft corridor chest loot, and
chunk-gated mineshaft cave-spider spawner placement, and
deterministic source-shaped mineshaft room upper pockets, and
source-inclusive mineshaft room child branch Y range, and
source non-air-only mineshaft room floor replacement, and
source liquid-envelope aborts for mineshaft pieces, and
generated stronghold source root anchor/available-height shifting, and
source-recursive stronghold pending-queue/restart-until-portal assembly,
portal-room-centered stronghold locating, stronghold source-door variants,
generated stronghold source first crossing child/access chain, source
weighted-room quota accounting, source shared-random stronghold piece
placement, source-order stronghold randomized shell/cobweb fill traversal,
stronghold corridor randomized stone-brick shell selection, and same-chunk
solid mesh face culling, structure-heavy generated chunk mesh
  validation, final ice/snow block-light/source-water gates, BlockFlower-style
  decorator light/support gates, BlockMushroom generated block-light gates, and
  refreshed source-order Overworld decorator/creature fixtures:

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest.releaseOneBiomeSourceIsDeterministicAndVaried" --tests "com.craftzero.world.WorldGenerationParityTest.releaseOneBiomeSourceMatchesSourceVectors" --tests "com.craftzero.world.WorldGenerationParityTest.releaseOneOverworldTerrainMatchesHeightVectors" --tests "com.craftzero.world.WorldGenerationParityTest.releaseOneOverworldDensityMatchesRawGridVectors"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.worldBackedOverworldPopulationSpawnsMooshroomsOnMushroomIslands" --tests "com.craftzero.world.OverworldGenerationSprintTest.hugeMushroomsSpillFromOffChunkSourceScratchCenters" --tests "com.craftzero.world.OverworldGenerationSprintTest.mushroomIslandDecoratorPlacesHugeMushrooms" --tests "com.craftzero.world.OverworldGenerationSprintTest.biomeDecoratorsUseSourceShapedDetailScatter" --tests "com.craftzero.world.OverworldGenerationSprintTest.biomeDecoratorsUseReleaseTreeAndCactusVariants"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.OverworldGenerationSprintTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest.generatedNetherBaseChunkAppliesHellCavesAfterSurfaceReplacement" --tests "com.craftzero.world.NetherCaveGeneratorTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.EndProgressionTest.endTerrainMatchesReleaseOneDensityFixtures" --tests "com.craftzero.world.EndProgressionTest.endRawDensityMatchesReleaseOneSourceVectors"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.EndProgressionTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.finalIceAndSnowPassFreezesExposedWater" --tests "com.craftzero.world.OverworldGenerationSprintTest.finalIceAndSnowPassHonorsSourceFluidAndLightGates"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.LilyPadInteractionTest" --tests "com.craftzero.world.OverworldGenerationSprintTest.finalIceAndSnowPassHonorsSourceFluidAndLightGates"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest.releaseOneOverworldDensityMatchesRawGridVectors" --tests "com.craftzero.world.WorldGenerationParityTest.overworldDensityCacheKeepsFullGridCoordinates"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.OverworldGenerationSprintTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.biomeDecoratorsUseSourceShapedDetailScatter" --tests "com.craftzero.world.OverworldGenerationSprintTest.overworldDensityUsesBiomeBlendedHeightField"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.OverworldGenerationSprintTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest.releaseOneBiomeSourceMatchesSourceVectors" --tests "com.craftzero.world.WorldGenerationParityTest.releaseOneOverworldTerrainMatchesHeightVectors" --tests "com.craftzero.world.WorldGenerationParityTest.releaseOneOverworldDensityMatchesRawGridVectors" --tests "com.craftzero.world.OverworldGenerationSprintTest.worldBackedOverworldPopulationSpawnsMooshroomsOnMushroomIslands" --tests "com.craftzero.world.OverworldGenerationSprintTest.hugeMushroomsSpillFromOffChunkSourceScratchCenters" --tests "com.craftzero.world.OverworldGenerationSprintTest.mushroomIslandDecoratorPlacesHugeMushrooms"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.biomeDecoratorsUseReleaseTreeAndCactusVariants" --tests "com.craftzero.world.OverworldGenerationSprintTest.biomeDecoratorsUseSourceShapedDetailScatter" --tests "com.craftzero.world.WorldGenerationParityTest.overworldPopulationRandomAdvancesThroughStructurePlacement" --tests "com.craftzero.world.WorldGenerationParityTest.overworldStructureReplayUsesSourceStructureOrder" --tests "com.craftzero.world.WorldGenerationParityTest.releaseOneOverworldDensityMatchesRawGridVectors"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.OverworldGenerationSprintTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.finalIceAndSnowPassFreezesExposedWater" --tests "com.craftzero.world.OverworldGenerationSprintTest.finalIceAndSnowPassHonorsSourceFluidAndLightGates"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.generatedFlowersAndGrassRequireSourceLightGate" --tests "com.craftzero.world.OverworldGenerationSprintTest.biomeDecoratorsUseSourceShapedDetailScatter"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.generatedMushroomsRequireMyceliumOrLowLightSupport" --tests "com.craftzero.world.OverworldGenerationSprintTest.biomeDecoratorsUseSourceShapedDetailScatter" --tests "com.craftzero.world.OverworldGenerationSprintTest.biomeMushroomLoopDrawsRedStartAsSourceOrder"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.generatedMushroomsRequireMyceliumOrLowLightSupport" --tests "com.craftzero.world.OverworldGenerationSprintTest.generatedFlowersAndGrassRequireSourceLightGate" --tests "com.craftzero.world.OverworldGenerationSprintTest.finalIceAndSnowPassHonorsSourceFluidAndLightGates" --tests "com.craftzero.world.OverworldGenerationSprintTest.biomeDecoratorsUseSourceShapedDetailScatter"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.ChunkMeshBuilderTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest.netherMushroomsUseWorldGenFlowersScatterAndRngCost" --tests "com.craftzero.world.WorldGenerationParityTest.releaseOneNetherDecoratesTerrain" --tests "com.craftzero.world.WorldGenerationParityTest.netherGlowstoneUsesSourceScatterAndSingleNeighborGrowth" --tests "com.craftzero.world.WorldGenerationParityTest.netherDecoratorScratchPreservesOffTargetGlowstoneGrowth"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.ChunkMeshBuilderTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.WorldGenerationParityTest.generatedNetherBaseChunkAppliesHellCavesAfterSurfaceReplacement" --tests "com.craftzero.world.WorldGenerationParityTest.releaseOneNetherDecoratesTerrain" --tests "com.craftzero.world.NetherCaveGeneratorTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.OverworldGenerationSprintTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.ChunkMeshBuilderTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest" --tests "com.craftzero.world.FluidSimulationTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.StructureGeneratorTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.DimensionTransferServiceTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.FluidMeshTest" --tests "com.craftzero.world.FluidSimulationTest" --tests "com.craftzero.world.MobSpawnerTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.MobSpawnerTest" --tests "com.craftzero.world.StructureGeneratorTest.locateNetherFortressMatchesGeneratedPieces"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest" --tests "com.craftzero.world.WorldGenerationParityTest.releaseOneNetherDecoratesTerrain"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.ChunkMeshBuilderTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.worldBackedOverworldPopulationRunsCreatureSpawning"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.entity.mob.MobFactoryTest" --tests "com.craftzero.world.OverworldGenerationSprintTest.worldBackedOverworldPopulationRunsCreatureSpawning" --tests "com.craftzero.world.OverworldGenerationSprintTest.worldBackedOverworldPopulationSpawnsWolvesInTaiga" --tests "com.craftzero.world.OverworldGenerationSprintTest.worldBackedOverworldPopulationSpawnsMooshroomsOnMushroomIslands"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.worldBackedOverworldPopulationRunsCreatureSpawning" --tests "com.craftzero.world.OverworldGenerationSprintTest.worldGenCreatureListsUseReleaseOneWolfWeights" --tests "com.craftzero.world.OverworldGenerationSprintTest.worldBackedOverworldPopulationSpawnsWolvesInTaiga"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.worldBackedOverworldPopulationRunsCreatureSpawning" --tests "com.craftzero.world.OverworldGenerationSprintTest.worldBackedOverworldPopulationSpawnsWolvesInTaiga" --tests "com.craftzero.world.OverworldGenerationSprintTest.worldBackedOverworldPopulationSpawnsMooshroomsOnMushroomIslands" --tests "com.craftzero.world.OverworldGenerationSprintTest.worldGenCreatureListsUseReleaseOneWolfWeights"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest.releaseOneBiomeTerrainConstantsMatchSource"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest.netherGlowstoneUsesSourceScatterAndSingleNeighborGrowth"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest.generatedNetherBaseChunkAppliesHellCavesAfterSurfaceReplacement"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.DimensionTransferServiceTest" --tests "com.craftzero.world.EndProgressionTest.endEntryRebuildsReleaseOneObsidianPlatform"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OreGeneratorTest" --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.StructureGeneratorTest" --tests "com.craftzero.world.DungeonFeatureTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.DimensionTransferServiceTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.FluidMeshTest" --tests "com.craftzero.world.FoliageMetadataInteractionTest" --tests "com.craftzero.world.MovementBlockInteractionTest" --tests "com.craftzero.world.BlockStateShapeTest" --tests "com.craftzero.world.MechanismSprintTest" --tests "com.craftzero.entity.mob.MobFactoryTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.StructureGeneratorTest" --tests "com.craftzero.world.EndProgressionTest.nullWorldEndGenerationIncludesSpikeBlocks"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest" --tests "com.craftzero.world.EndProgressionTest.nullWorldEndGenerationIncludesSpikeBlocks"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OreGeneratorTest" --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.StructureGeneratorTest" --tests "com.craftzero.world.DungeonFeatureTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.FluidMeshTest" --tests "com.craftzero.world.FoliageMetadataInteractionTest" --tests "com.craftzero.world.MovementBlockInteractionTest" --tests "com.craftzero.world.BlockStateShapeTest" --tests "com.craftzero.world.MechanismSprintTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.nullWorldOverworldGenerationPlacesDungeonBlocks" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.DungeonFeatureTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OreGeneratorTest" --tests "com.craftzero.world.OverworldGenerationSprintTest.decoratorScratchOverlaysOffChunkOresBeforeLateFeatures" --tests "com.craftzero.world.OverworldGenerationSprintTest.underwaterDisksUseOffChunkSourceScratchState" --tests "com.craftzero.world.WorldGenerationParityTest.overworldDecoratorRandomResumesAfterOreHelper"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.OreGeneratorTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest.overworldDecoratorRandomResumesAfterOreHelper" --tests "com.craftzero.world.WorldGenerationParityTest.overworldOreRandomResumesAfterLakeAndDungeonPhases"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest.overworldDungeonRandomResumesAfterLakeBranches" --tests "com.craftzero.world.WorldGenerationParityTest.overworldOreRandomResumesAfterLakeAndDungeonPhases" --tests "com.craftzero.world.DungeonFeatureTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OreGeneratorTest" --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.StructureGeneratorTest" --tests "com.craftzero.world.DungeonFeatureTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.FluidMeshTest" --tests "com.craftzero.world.FoliageMetadataInteractionTest" --tests "com.craftzero.world.MovementBlockInteractionTest" --tests "com.craftzero.world.BlockStateShapeTest" --tests "com.craftzero.world.MechanismSprintTest"
```

Earlier broad parity verification:

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OreGeneratorTest" --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.StructureGeneratorTest" --tests "com.craftzero.world.DungeonFeatureTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.FoliageMetadataInteractionTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OreGeneratorTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.OverworldGenerationSprintTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.generatedLilyPadsRequireSourceLevelWater"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.generatedCactusUsesSourceMaterialAdjacency" --tests "com.craftzero.world.PlacementSupportTest.cactusUsesSourceMaterialBlocking"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.PlacementSupportTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OreGeneratorTest" --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.StructureGeneratorTest" --tests "com.craftzero.world.DungeonFeatureTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.FoliageMetadataInteractionTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.underwaterDisksUseFloorWaterHeight" --tests "com.craftzero.world.OverworldGenerationSprintTest.overworldDecorationPlacesUnderwaterDisks"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OreGeneratorTest" --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.StructureGeneratorTest" --tests "com.craftzero.world.DungeonFeatureTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.FoliageMetadataInteractionTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OreGeneratorTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OreGeneratorTest" --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.StructureGeneratorTest" --tests "com.craftzero.world.DungeonFeatureTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.FoliageMetadataInteractionTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.finalIceAndSnowPassFreezesExposedWater"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest.overworldDungeonRandomResumesAfterLakeBranches"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.DungeonFeatureTest" --tests "com.craftzero.world.OverworldGenerationSprintTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.EndProgressionTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.OverworldGenerationSprintTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OreGeneratorTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.FoliageMetadataInteractionTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.ChunkMeshBuilderTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.OverworldGenerationSprintTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OreGeneratorTest" --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.StructureGeneratorTest" --tests "com.craftzero.world.DungeonFeatureTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.FoliageMetadataInteractionTest"
```

Latest verification after the mineshaft stairwell descending-slice correction:

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.mineshaftStairsUseSourceDescendingCarveSlices" --tests "com.craftzero.world.StructureGeneratorTest.mineshaftStairsAbortsWhenLiquidTouchesSourceEnvelope"
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest"
.\gradlew.bat test --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.StructureGeneratorTest"
```

Previous verification after the mineshaft corridor/crossing unsupported-floor,
source-order corridor random-fill, and structure-spawner RNG parity update:

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.netherFortressPiecesUseSharedPlacementRandom" --tests "com.craftzero.world.StructureGeneratorTest.mineshaftSpiderSpawnerDoesNotConsumePlacementRandom"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest.netherDecoratorRandomAdvancesThroughFortressPlacement"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.mineshaftCrossDoesNotBackfillUnsupportedFloor" --tests "com.craftzero.world.StructureGeneratorTest.mineshaftCorridorUsesSourceSectionSupports"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.mineshaftCorridorUsesSharedPlacementRandom" --tests "com.craftzero.world.StructureGeneratorTest.mineshaftCorridorRandomCeilingUsesSourceLocalOrder" --tests "com.craftzero.world.StructureGeneratorTest.mineshaftSpiderWebsUseSourceLocalOrder"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.OverworldGenerationSprintTest"
```

Previous verification after the Nether/Overworld primitive source-audit report
update:

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest.netherGlowstoneUsesSourceScatterAndSingleNeighborGrowth" --tests "com.craftzero.world.WorldGenerationParityTest.netherDecoratorScratchPreservesOffTargetGlowstoneGrowth" --tests "com.craftzero.world.WorldGenerationParityTest.netherLavaSpringsUseSourceFiveNeighborGate" --tests "com.craftzero.world.WorldGenerationParityTest.netherFireUsesSourceScatterAndRngCost" --tests "com.craftzero.world.WorldGenerationParityTest.netherMushroomsUseWorldGenFlowersScatterAndRngCost" --tests "com.craftzero.world.OverworldGenerationSprintTest.underwaterDisksUseFloorWaterHeight" --tests "com.craftzero.world.OverworldGenerationSprintTest.underwaterDisksUseOffChunkSourceScratchState" --tests "com.craftzero.world.OverworldGenerationSprintTest.overworldDecorationPlacesLiquidSprings" --tests "com.craftzero.world.OverworldGenerationSprintTest.reedAndCactusScatterConsumeSourceHeightDraws" --tests "com.craftzero.world.OverworldGenerationSprintTest.biomeDecoratorsUseSourceShapedDetailScatter"
```

Previous verification after the off-target dungeon scratch replay update:

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.DungeonFeatureTest" --tests "com.craftzero.world.WorldGenerationParityTest.overworldOreRandomResumesAfterLakeAndDungeonPhases" --tests "com.craftzero.world.WorldGenerationParityTest.overworldDecoratorRandomResumesAfterOreHelper" --tests "com.craftzero.world.OverworldGenerationSprintTest.nullWorldOverworldGenerationPlacesDungeonBlocks" --tests "com.craftzero.world.OverworldGenerationSprintTest.dungeonValidationReadsOffChunkLakeScratchState"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.DungeonFeatureTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.OverworldGenerationSprintTest"
```

Previous verification after the End decorator inherited ore-RNG phase update:

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.EndProgressionTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.WorldGenerationParityTest"
```

Previous verification after the shifted Overworld ore-origin and draw-order
update:

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OreGeneratorTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OreGeneratorTest" --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.StructureGeneratorTest" --tests "com.craftzero.world.DungeonFeatureTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest"
```

Previous verification after adding biome and terrain source-vector coverage:

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest"
```

Previous verification after the source-shaped swamp tree/vine and big-tree
updates:

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.StructureGeneratorTest" --tests "com.craftzero.world.DungeonFeatureTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest"
```

Earlier verification from this worldgen parity pass:

After the mushroom-island decorator update:

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.StructureGeneratorTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.StructureGeneratorTest" --tests "com.craftzero.world.DungeonFeatureTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest"
```

Passed after the final ice/snow scratch-light update:

```text
.\gradlew.bat test --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.StructureGeneratorTest"
```

Passed after the late decorator scratch-light update:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.lateDecoratorsReadOffChunkSourceScratchState" --tests "com.craftzero.world.OverworldGenerationSprintTest.lateDecoratorLightGatesReadOffChunkScratchEmitters" --tests "com.craftzero.world.OverworldGenerationSprintTest.finalIceAndSnowPassReadsOffChunkScratchBlockLight"
```

Passed after the off-target detail scratch-light update:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest.lateDecoratorLightGatesReadOffChunkScratchEmitters"
```

Passed after the Nether mushroom scratch-light update:

```text
.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest.netherMushroomsUseWorldGenFlowersScatterAndRngCost" --tests "com.craftzero.world.WorldGenerationParityTest.netherMushroomScratchLightReadsOffTargetGlowstone" --tests "com.craftzero.world.WorldGenerationParityTest.netherDecoratorScratchPreservesOffTargetGlowstoneGrowth"
```

Passed after the forced/proxy stronghold weight-accounting update:

```text
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdFirstCrossingDoesNotConsumeWeightedCrossingQuota" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdFixedRoomCrossingDoesNotConsumeWeightedRoomQuota" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdFixedChestCorridorDoesNotConsumeWeightedRoomQuota" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdFixedPrisonDoesNotConsumeWeightedRoomQuota"
```

Passed after the stronghold weighted-repeat guard update:

```text
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdWeightedSelectionRejectsImmediateStraightRepeats"
```

Passed after the stronghold source-depth recursion guard update:

```text
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdBranchRecursionUsesSourceDepthLimit"
```

Passed after the stronghold side-root component-depth update:

```text
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdFirstCrossingSideRootsUseSourceComponentDepth" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdFirstStraightSideRootsUseSourceComponentDepth"
```

Passed after the stronghold constructor-RNG validity ordering update:

```text
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdInvalidWeightedPlacementDoesNotConsumeConstructorRng" --tests "com.craftzero.world.StructureGeneratorTest.generatedStrongholdLibraryFallsBackToSourceSmallBox"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.StructureGeneratorTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.StructureGeneratorTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.StructureGeneratorTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.StructureGeneratorTest"
```

Passed:

```text
git diff --check -- WORLDGEN_PARITY_REPORT.md src\main\java\com\craftzero\world\ReleaseOneBiomeSource.java src\main\java\com\craftzero\world\ReleaseOneOctaveNoise.java src\main\java\com\craftzero\world\OverworldDensityField.java src\main\java\com\craftzero\world\ReleaseOneWorldGenerator.java src\main\java\com\craftzero\world\NetherCaveGenerator.java src\main\java\com\craftzero\world\StructureGenerator.java src\main\java\com\craftzero\world\StructurePlanner.java src\test\java\com\craftzero\world\ReleaseOneOctaveNoiseTest.java src\test\java\com\craftzero\world\NetherCaveGeneratorTest.java src\test\java\com\craftzero\world\WorldGenerationParityTest.java src\test\java\com\craftzero\world\OverworldGenerationSprintTest.java src\test\java\com\craftzero\world\StructureGeneratorTest.java src\test\java\com\craftzero\world\EndProgressionTest.java src\test\java\com\craftzero\world\ChunkMeshBuilderTest.java
```

`git diff --check` only emitted Git line-ending warnings for tracked files.

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.StructureGeneratorTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.StructureGeneratorTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.ChunkMeshBuilderTest" --tests "com.craftzero.world.EndProgressionTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.StructureGeneratorTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.DungeonFeatureTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.StructureGeneratorTest"
```

Passed:

```text
.\gradlew.bat test --tests "com.craftzero.world.ReleaseOneOctaveNoiseTest" --tests "com.craftzero.world.NetherCaveGeneratorTest" --tests "com.craftzero.world.WorldGenerationParityTest" --tests "com.craftzero.world.OverworldGenerationSprintTest" --tests "com.craftzero.world.StructureGeneratorTest" --tests "com.craftzero.world.DungeonFeatureTest" --tests "com.craftzero.world.EndProgressionTest" --tests "com.craftzero.world.ChunkMeshBuilderTest"
```

Passed:

```text
git diff --check -- WORLDGEN_PARITY_REPORT.md src\main\java\com\craftzero\world\ReleaseOneBiomeSource.java src\main\java\com\craftzero\world\ReleaseOneOctaveNoise.java src\main\java\com\craftzero\world\OverworldDensityField.java src\main\java\com\craftzero\world\ReleaseOneWorldGenerator.java src\main\java\com\craftzero\world\NetherCaveGenerator.java src\main\java\com\craftzero\world\StructureGenerator.java src\main\java\com\craftzero\world\StructurePlanner.java src\test\java\com\craftzero\world\ReleaseOneOctaveNoiseTest.java src\test\java\com\craftzero\world\NetherCaveGeneratorTest.java src\test\java\com\craftzero\world\WorldGenerationParityTest.java src\test\java\com\craftzero\world\OverworldGenerationSprintTest.java src\test\java\com\craftzero\world\StructureGeneratorTest.java src\test\java\com\craftzero\world\EndProgressionTest.java src\test\java\com\craftzero\world\ChunkMeshBuilderTest.java
```

`git diff --check` only emitted Git line-ending warnings for tracked files.

## Remaining Parity Gaps

1. Overworld population RNG is not fully source-coupled.

   Release 1.0 uses one population random stream per chunk after structures.
   CraftZero now follows the high-level structure -> lakes -> dungeons ->
   ore/decorator -> snow/ice ordering for world-backed Overworld chunks.
   Dungeon attempts now resume after replaying the source lake branches for the
   origin, including source-valid rooms that generate wholly outside the current
   target chunk, and ore placement now starts after a scratch replay of the
   eight dungeon attempts before using the source raw `origin + nextInt(16)` x/z
   starts for every neighboring origin that can spill into the target chunk. It
   now validates lake attempts against a mutable lake scratch, overlays nearby
   off-target structure, lake, dungeon, and ore side effects into the decorator
   scratch before disks and tree replay, and lets later decorator
   support/obstruction reads use that scratch state. The temporary replay chunks
   used for lake and dungeon advancement now also start from carved terrain plus
   current-chunk structure placement rather than carved terrain alone. The
   one-time creature population pass now consumes
   `world.random` for weighted creature-type selection, matching the
   `SpawnerCreature` split between world RNG and chunk population RNG, and uses
   post-decoration scratch-backed collision/fluid volume checks so
   non-colliding plants do not suppress animal births while late fluids or
   real obstructions still do. Dungeon
   validation/replay now reads carved neighboring terrain and nearby off-target
   lake side effects instead of assuming fake stone outside the target/origin
   chunk, the decorator scratch now carries bounded off-target dungeon block
   mutations forward before ores and late features, dungeon/structure blocks
   now populate even when chunk generation is run without a world object, and
   origin RNG replay now advances through structure placement on copied carved
   terrain before lake/dungeon/ore/decorator helpers resume. The live target
   chunk now carries one mutable scratch through lakes, dungeons, ores, and
   decorators, with target overlays cleared after falling-block stabilization so
   late features read stabilized chunk state. The final shifted ice/snow pass
   now also reads block light from the population scratch, so neighboring
   generated torches can suppress target-chunk freezing/snow where source
   world-light checks would see them. Late detail-decorator stay checks now
   also read scratch-derived block light for covered mushrooms, flowers, tall
   grass, and dead bushes. The mixed mineshaft/village
   structure replay fixture for seed `1`, chunk `(-45,38)` now locks the
   post-structure random stream to `9,50,14` for the next lake-position draws.
   It still uses bounded
   cross-origin population-world validation, visible tree placement, and helper
   streams for some replayed origins instead of one exact vanilla world object.
   The off-origin replay used for ore and decorator state is still an
   origin-carved, structure/lake/dungeon-applied, off-target-mutating scratch
   simulation, not a full world-state simulation of every cross-chunk
   structure, lake, and dungeon side effect, so exact post-dungeon coupling is
   still not proven. This preserves deterministic chunk safety, but it is not
   exact source RNG order.

2. Biome decorator random coupling and full mutable population side effects are
   still not fully source-equivalent.

   The source decorator places sand, clay, sand again, trees, huge mushrooms,
   flowers, grass, dead bushes, lilies, mushrooms, reeds, pumpkins, cactus, then
   liquid springs. CraftZero now places disk passes, replays source-shaped tree
   attempt RNG with a mutable scratch tree overlay, then places mushroom-island
   huge mushrooms, biome-counted detail features, cactus, and springs in that
   source sequence. Small Overworld mushroom placement now rejects open-sky
   ordinary support and still allows mycelium or covered opaque support.
   Normal tree geometry now follows `WorldGenTrees` more
   closely, and big oak geometry now follows `WorldGenBigTree` more closely,
   taiga conifer geometry now follows `WorldGenTaiga1` and `WorldGenTaiga2`
   more closely, and swamp tree geometry now follows `WorldGenSwampTree` more
   closely. Tall-grass decoration now uses the source biome counts, including
   10 scatter attempts in plains, 2 in forests, and suppressed attempts in
   mushroom biomes. Mushroom-island huge mushroom placement is now gated by the
   source origin biome and the generator support/space checks, so target
   locations in the population margin are no longer rejected just because their
   final biome is adjacent non-mushroom terrain; huge mushroom starts now also
   require the Release-era dirt, grass, or mycelium support before the small
   mushroom placement/light gate is considered. Current surface-detail coverage
   checks deterministic sugar cane in chunk `(-47,-7)` and taiga long-grass
   metadata `1` with no fern metadata.
   The tree replay overlay now includes off-target structure, lake,
   dungeon, and ore side effects plus source-ordered underwater disk mutations
   before trees run, the later huge-mushroom/detail/spring decorators read
   through that overlay, detail light gates use a world-coordinate
   scratch-aware generated block-light snapshot, and scratch-backed late
   decorators can record valid off-target placements for later checks while
   writing only target intersections. Final shifted ice/snow finishing now uses
   that same wider scratch-only population window and world-coordinate
   block-light replay, so nearby non-visible origins can contribute final
   freeze/snow side effects for later final-pass reads without writing outside
   the target chunk. The mutable population window now
   separates visible source-reachable origins from the wider scratch-only
   non-visible origins that can affect cross-origin validation. It is still not
   a full unbounded mutable source population world with every distant side
   effect, but the local chunk-safe population window now carries every
   lake/dungeon/ore/disk/tree/huge-mushroom/detail/spring side effect used by
   late reads in this pass. The audited primitive generators for
   sand/clay disks, reeds, cactus, pumpkins, liquid springs, and Nether
   glowstone now match their source RNG gates closely; final ice/snow
  block-light checks now read the post-decoration scratch, decorator
  `getHeightValue`/sky checks use the source light-opacity height-map
  predicate, generated decorator block light attenuates through the same
  source opacity table, and worldgen creature placement now uses the source
  `findTopSolidBlock` height scan plus normal-cube/liquid gate against that
  scratch. Sheep color initialization also consumes the world's source-shaped
  random stream instead of the chunk decorator random. Exact coupling with the
  full mutable population world used
  by structures, lake validation, dungeons, ores, decorators, tree vines, every
  worldgen creature side effect, and distant final ice/snow side effects remain
  incomplete.

3. Overworld terrain is closer to source-vector proven, but not exhaustive.

   `OverworldDensityField` uses Release-era octave noise, generation-layer
   biome blending, vertical density math, Y-aware 3D min/max/selector noise, and
   linear 4x8x4 cell interpolation. Final biome, generation biome, top-block,
   sea-level, terrain-height, raw density grid-node, and interpolated density
   vectors are now locked, along with whole-grid density hashes for multiple
   chunks/seeds. It still needs broader independent source-vector coverage from
   external source traces across more chunk arrays and seeds before claiming
   exact Perlin/noise parity.

4. Structure interiors remain simplified.

   Stronghold, village, and mineshaft placement/interiors remain simplified.
   Fortress starts now use source-shaped bounds, visible block placement,
   source-style support fills, source three-branch start topology, source
   local-coordinate/stair-metadata transforms for oriented pieces, and recursive
   weighted component generation; generated strongholds now use the source
   `(chunk << 4) + 2` root X/Z anchor, root-Y, and
   `markAvailableHeight(..., 10)` vertical shift with the source
   `worldOceanHeight` ceiling instead of the village foundation minimum.
   Generated stronghold starts
   now rotate from the source root orientation draw, force only the source first
   crossing child without consuming the weighted crossing-hall quota, consume
   that child's source constructor door/side-opening RNG, drain the same
   recursive pending queue by random index, and retry whole source-shaped start
   attempts until a weighted portal room exists. Recursive exits now feed the
   source-depth weighted side-branch queue that adds source-shaped straights,
   turns, standalone stairwells, stair-straights, room crossings, prisons, chest
   corridors, depth-gated libraries, and depth-gated portal rooms when
   collision/distance/depth guards allow, while rejecting immediate repeats of
   the previously selected source weight; weighted branch selection keeps
   exhausted weights in the Release-era draw range and lets those selected
   exhausted entries burn an attempt, matching source random-stream behavior
   once limited stronghold rooms are used up; weighted
   stronghold component factories now avoid consuming constructor RNG until
   after source bounding-box validity succeeds, and library branches use the
   source large-then-small fallback rather than a random height choice; when
   weighted room placement fails
   against a same-floor nearby stronghold piece, branches now use the source
   short `ComponentStrongholdCorridor` fallback connector instead of ending
  abruptly, weighted chest corridors use the source four-instance cap, and
  branch expansion now stops after every capped weighted room is exhausted
  instead of chaining through unlimited filler-only pieces;
   stronghold locate calls now return the generated portal-room center when one
   exists, and village/mineshaft locate calls now return the nearest generated
   start center from the same start builders used by chunk population;
   generated/randomized stronghold doors now support the source opening,
   wooden-door, grates, and iron-door variants, and generated room crossings
   now store their constructor room-variant draw; stronghold shell blocks use
   Release-era stone-brick
   variant/monster-egg metadata, the portal room now uses the source chamber
   block layout, the chest corridor now uses the source shelf/chest layout and
   weighted loot table, prisons now use the source cell/interior-door layout,
   libraries now use the source large-room shelf/balcony/chest layout and
   weighted loot table, and room crossings now
   use the source variant layouts and balcony weighted loot table;
   start stairs and generated side-branch stairwells now use the source
   stairwell layout; straight connectors now use the source 5x5x7 connector
   layout;
   straight-stairs pieces now use the source 5x11x8 descending stair layout;
   left/right turns now use the source 5x5x5 turn layouts;
   crossing-hall pieces now use the source 10x9x11 multi-level crossing layout;
   corridor connectors now use the source open-ended tube layout plus
   randomized plain/mossy/cracked stronghold-stone shell selection, including
   the short collision fallback connectors from generated weighted branch
   failures;
   simplified mineshaft starts now include a source-shaped root room and bounded
   corridor/cross/stairs graph, source-order corridor continuation RNG,
   source `markAvailableHeight(..., 10)` vertical shifts,
   shared-random corridor block placement, and source weighted corridor chest
   loot, with no synthetic corridor/crossing plank floor under unsupported
   cells, source-shaped lower fence-column corridor supports,
   generated corridor torches written through the source metadata-`0`
   structure path, and
   spider/fortress/stronghold spawners preserving the shared placement random
   while keeping the source default generated delay; spider
   spawners are gated to the current chunk before the corridor marks one placed;
   room upper pockets use
   the deterministic source rare-fill ellipsoid, and room branch heights use
   the source inclusive Y range; room floors replace only existing non-air
   terrain with dirt; stair pieces use the source descending-slice lower bound;
   and room, corridor, crossing, and stairs pieces now honor the source
   liquid-envelope abort in the target chunk; non-portal stronghold pieces now
   apply the same source liquid-envelope abort while portal rooms remain exempt;
   mineshaft room child-opening clear volumes now have direct block-output
   coverage for the stored source connector boxes;
   simplified village starts now include the
   source-shaped well root, initial path pieces, path side-attachment queues
   whose side roads now preserve the parent Release-style road recursion depth,
   wood huts, churches, house 1, house 3, blacksmiths, halls,
   garden houses, farm variants, torch fallback pieces with valid rotated
   wall-torch metadata, and source villager spawn count/profession/position
   coverage for implemented village buildings. Release 1.0 blacksmiths now
   also keep the source no-chest block output and avoid later-version loot RNG
   consumption, and generated village doors now calculate their lower and
   upper metadata with the source `ItemDoor.placeDoorBlock` hinge rule. Village
   foundation placement now also uses source `findTopSolidBlock`/ocean-height
   semantics, so terrain-grounded village pieces no longer sit one block below
   the Release 1.0 foundation calculation.
   Full structure parity still needs stronger
   source-vector coverage for fortress graphs and block output, plus broader
   stronghold `ComponentStrongholdStairs2` retry/queue-order and weighted-branch
   source-vector coverage,
   village/mineshaft piece generators, broader source-vector coverage for
   village road graph shapes, exact village and mineshaft source-vector
   graph/block-output coverage, shared
   component placement RNG for any remaining random-consuming piece paths
   outside the covered fortress, stronghold, village farm, and mineshaft
   corridor cases,
   bounding-box behavior, and structure-specific random sequencing.

5. Structure/decorator ordering is still not fully source-equivalent.

   Village lake suppression is implemented for world-backed and null-world
   generation, Overworld structure placement now runs before lakes/decorators,
   Overworld lake/dungeon/ore/decorator RNG replay advances through structure
   placement for each population origin, and Overworld structures now replay in
   the source mineshaft -> village -> stronghold order,
   the world-backed Overworld creature population pass now runs after
   decoration, forest/taiga wolf worldgen weights now match the source lists,
   Nether fortress placement runs before Nether decorators, Nether decorator
   streams now replay the source post-surface RNG state, blazes have been
   removed from the general Nether hostile spawn table, and Nether hostile
   selection now uses a separate fortress piece list when inside generated
   fortress-piece bounds. Current-origin Nether decorators now continue after
   real fortress placement, and shifted neighbor-origin decorator replay
   advances through scratch fortress placement before decorators; nearby
   off-target Nether fortress block deltas are now cached and overlaid into the
   mutable Nether decorator scratch so cross-chunk lava, fire, glowstone, and
   mushroom placement reads see already-placed fortress blocks instead of raw
   base Nether terrain; Nether mushroom light checks now read the mutable
   decorator scratch for off-target glowstone as well as current-chunk
   glowstone, and final Overworld ice/snow finishing now uses the wider
   scratch-only population window plus the world-coordinate scratch light
   replay used by late decorators. Full
   cross-chunk structure/decorator block side-effect replay is still
   chunk-safe rather than a complete mutable source world. Full parity still
   requires exact source RNG coupling for remaining Overworld population phases,
   stronger natural fortress graph source-vector coverage, final wolf
   taming/anger/sheep-hunting AI edge-case source vectors, and final villager
   AI/source-vector coverage beyond the implemented professions, passive
   persistence, door use, panic, and zombie-avoidance behavior (trading is
   intentionally absent for Release 1.0), plus a chunk-safe
   population plan that preserves every structure, lake, dungeon,
   biome-decorator, creature-spawn, ice, and snow side effect.

6. Nether terrain still needs an independently executed provider trace.

   Nether density, bedrock, lava seas, glowstone, fire, soul sand, gravel,
   mushrooms, decorator order, and Hell cave carving now have focused coverage,
   including raw density grid-vector fixtures, whole-grid hashes across multiple
   chunks/seeds, off-target Nether mushroom scratch-light coverage, and
   direct Hell cave block-output hashes across multiple chunks/seeds, plus
   generated base-chunk cave fixtures for seed 515151 chunk (0,-4) and seed
   1234 chunk (7,-8). A separately executed Release-era provider trace and
   final fortress piece parity are still missing.

7. End island terrain needs broader source-vector coverage.

   End terrain, spike RNG phase ordering, and the entry obsidian platform are
   implemented, source-audited, and covered by deterministic
   island/falloff/void chunk fixtures, null-world spike block fixtures, raw
   density grid-vector
   validation, whole-grid density hashes for origin, radial falloff, far-void,
   negative falloff, diagonal rim, alternate-seed origin/rim chunks, and an
   entry-platform rebuild fixture. It still needs an independently executed
   Release-era provider oracle across more chunks/seeds before it can be called
   exact.

8. Visual/render parity is still not fully proven.

   `ChunkMeshBuilderTest` now validates generated Overworld, Nether, and End
   mesh buffers at a data level, neighbor-aware generated meshes are checked
   against added seam faces, direct solid chunk-border culling is pinned, fluid
   corner geometry samples diagonal chunk neighbors, same-chunk adjacent solid
   blocks are pinned against internal hidden-face emission, structure-heavy
   Overworld/Nether generated chunks and an End edge chunk are validated,
   generated UVs are
   bounded to the terrain atlas, alpha-tested utility blocks route to cutout
   buffers, crossed plant sprites use the Release-style 0.45 half-width, tall
   grass uses source render jitter and soft plant shape/replacement semantics,
   cobweb uses crossed-sprite geometry, and mushroom block metadata now selects
   the Release-era cap, pore, and stem atlas textures. Active Ender Dragon
   crystal healing now renders an animated multi-segment beam from the selected
   crystal to the dragon body. This still does not
   prove there are no visual bugs. A render harness should inspect generated
   scenes for missing faces, transparent block ordering, lighting artifacts,
   chunk-boundary seams, and performance.

## Recommended Next Work

1. Refactor Overworld population to use a source-ordered population plan while
   keeping cross-chunk placement deterministic.
2. Expand the source tree replay overlay into a fuller mutable population plan
   that includes all lake, dungeon, tree, decorator, ice, and snow side effects
   across chunk boundaries.
3. Replace simplified structure layouts with source-derived piece assemblers.
4. Add rendered chunk regression checks for Overworld, Nether, and End scenes.

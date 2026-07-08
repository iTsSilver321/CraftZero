package com.craftzero.main;

import com.craftzero.audio.AmbientMusicScheduler;
import com.craftzero.audio.WorldSoundDispatcher;
import com.craftzero.audio.OpenAlSoundSink;
import com.craftzero.combat.DamageSource;
import com.craftzero.combat.ExplosionExposure;
import com.craftzero.command.CommandDispatcher;
import com.craftzero.crafting.CraftingRecipe;
import com.craftzero.crafting.CraftingRegistry;
import com.craftzero.engine.Input;
import com.craftzero.engine.Timer;
import com.craftzero.engine.Window;
import com.craftzero.entity.ArrowEntity;
import com.craftzero.entity.BoatEntity;
import com.craftzero.entity.ChestMinecartEntity;
import com.craftzero.entity.DroppedItem;
import com.craftzero.entity.EndCrystalEntity;
import com.craftzero.entity.EnderPearlEntity;
import com.craftzero.entity.Entity;
import com.craftzero.entity.ExperienceOrbEntity;
import com.craftzero.entity.EyeOfEnderEntity;
import com.craftzero.entity.FallingBlockEntity;
import com.craftzero.entity.FishingHookEntity;
import com.craftzero.entity.FireballEntity;
import com.craftzero.entity.FurnaceMinecartEntity;
import com.craftzero.entity.LivingEntity;
import com.craftzero.entity.MinecartEntity;
import com.craftzero.entity.PaintingEntity;
import com.craftzero.entity.PrimedTntEntity;
import com.craftzero.entity.SplashPotionEntity;
import com.craftzero.entity.ThrownItemEntity;
import com.craftzero.graphics.ArrowRenderer;
import com.craftzero.graphics.BlockBreakingRenderer;
import com.craftzero.graphics.BlockHighlightRenderer;
import com.craftzero.graphics.ChestRenderer;
import com.craftzero.graphics.CloudRenderer;
import com.craftzero.graphics.DimensionRenderEnvironment;
import com.craftzero.graphics.DimensionRenderEnvironment.CameraFluid;
import com.craftzero.graphics.DroppedItemRenderer;
import com.craftzero.graphics.EnchantingTableRenderer;
import com.craftzero.graphics.FallingBlockRenderer;
import com.craftzero.graphics.HudRenderer;
import com.craftzero.graphics.InventoryPlayerRenderer;
import com.craftzero.graphics.InventoryRenderer;
import com.craftzero.graphics.LightningRenderer;
import com.craftzero.graphics.MobRenderer;
import com.craftzero.graphics.MobSpawnerRenderer;
import com.craftzero.graphics.MovingPistonRenderer;
import com.craftzero.graphics.ParticleRenderer;
import com.craftzero.graphics.PrecipitationRenderer;
import com.craftzero.graphics.Renderer;
import com.craftzero.graphics.SignTextRenderer;
import com.craftzero.graphics.SkyRenderer;
import com.craftzero.graphics.SurvivalHudRenderer;
import com.craftzero.graphics.TextRenderer;
import com.craftzero.inventory.CraftingGridOps;
import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.inventory.MapItemData;
import com.craftzero.inventory.ToolType;
import com.craftzero.multiplayer.LegacyServerStatus;
import com.craftzero.multiplayer.MultiplayerClient;
import com.craftzero.multiplayer.MultiplayerProtocol;
import com.craftzero.multiplayer.NetworkMessage;
import com.craftzero.multiplayer.MultiplayerServer;
import com.craftzero.multiplayer.SavedServer;
import com.craftzero.multiplayer.SavedServerList;
import com.craftzero.physics.AABB;
import com.craftzero.physics.Raycast;
import com.craftzero.progression.ArmorMaterial;
import com.craftzero.progression.ArmorSlot;
import com.craftzero.progression.BookshelfPower;
import com.craftzero.progression.EnchantmentInstance;
import com.craftzero.progression.EnchantmentResolver;
import com.craftzero.progression.EnchantmentType;
import com.craftzero.progression.PlayerProgression;
import com.craftzero.progression.PotionData;
import com.craftzero.progression.PotionEffectResolver;
import com.craftzero.progression.PotionType;
import com.craftzero.progression.StatusEffectInstance;
import com.craftzero.progression.StatusEffectType;
import com.craftzero.resources.ResourcePackManager;
import com.craftzero.save.SaveManager;
import com.craftzero.save.WorldManager;
import com.craftzero.save.WorldManager.WorldInfo;
import com.craftzero.ui.ChestScreen;
import com.craftzero.ui.BrewingStandScreen;
import com.craftzero.ui.ChatOverlay;
import com.craftzero.ui.CraftAction;
import com.craftzero.ui.CraftingTableScreen;
import com.craftzero.ui.DispenserScreen;
import com.craftzero.ui.EnchantingTableScreen;
import com.craftzero.ui.FurnaceScreen;
import com.craftzero.ui.InventoryScreen;
import com.craftzero.ui.SignEditScreen;
import com.craftzero.ui.menu.BaseMenuScreen;
import com.craftzero.ui.menu.CreativeInventoryScreen;
import com.craftzero.ui.menu.GuiScale;
import com.craftzero.ui.menu.MenuButton;
import com.craftzero.ui.menu.MenuInput;
import com.craftzero.ui.menu.MenuLabel;
import com.craftzero.ui.menu.MenuList;
import com.craftzero.ui.menu.MenuRenderer;
import com.craftzero.ui.menu.MenuScreens;
import com.craftzero.ui.menu.Rect;
import com.craftzero.ui.menu.Screen;
import com.craftzero.ui.menu.ScreenManager;
import com.craftzero.ui.menu.TextField;
import com.craftzero.world.DayCycleManager;
import com.craftzero.world.Dimension;
import com.craftzero.world.DimensionTransferService;
import com.craftzero.world.MobSpawner;
import com.craftzero.world.Block;
import com.craftzero.world.BlockHarvestRules;
import com.craftzero.world.BlockShape;
import com.craftzero.world.BlockType;
import com.craftzero.world.Chunk;
import com.craftzero.world.World;
import com.craftzero.world.WorldGenerator;
import com.craftzero.world.WorldGenerators;
import com.craftzero.world.WorldLightningBolt;
import com.craftzero.world.WorldParticle;
import com.craftzero.world.WorldSoundEvent;
import com.craftzero.entity.mob.Blaze;
import com.craftzero.entity.mob.Chicken;
import com.craftzero.entity.mob.Creeper;
import com.craftzero.entity.mob.EnderDragon;
import com.craftzero.entity.mob.Enderman;
import com.craftzero.entity.mob.Cow;
import com.craftzero.entity.mob.Ghast;
import com.craftzero.entity.mob.MagmaCube;
import com.craftzero.entity.mob.Mob;
import com.craftzero.entity.mob.MobDefinition;
import com.craftzero.entity.mob.MobFactory;
import com.craftzero.entity.mob.Mooshroom;
import com.craftzero.entity.mob.Pig;
import com.craftzero.entity.mob.Sheep;
import com.craftzero.entity.mob.Skeleton;
import com.craftzero.entity.mob.Slime;
import com.craftzero.entity.mob.SnowGolem;
import com.craftzero.entity.mob.Spider;
import com.craftzero.entity.mob.Squid;
import com.craftzero.entity.mob.Villager;
import com.craftzero.entity.mob.Wolf;
import com.craftzero.entity.mob.ZombiePigman;
import com.craftzero.entity.ai.RangedAttackGoal;
import com.craftzero.entity.ai.LineOfSightUtil;
import com.craftzero.world.StructureGenerator;
import com.craftzero.world.StructureType;
import com.craftzero.world.tile.BlockPos;
import com.craftzero.world.tile.ChestTileEntity;
import com.craftzero.world.tile.BrewingRecipeRegistry;
import com.craftzero.world.tile.BrewingStandTileEntity;
import com.craftzero.world.tile.DispenserTileEntity;
import com.craftzero.world.tile.EnchantingTableTileEntity;
import com.craftzero.world.tile.FuelRegistry;
import com.craftzero.world.tile.FurnaceTileEntity;
import com.craftzero.world.tile.JukeboxTileEntity;
import com.craftzero.world.tile.MonsterSpawnerTileEntity;
import com.craftzero.world.tile.NoteBlockTileEntity;
import com.craftzero.world.tile.SignTileEntity;
import com.craftzero.world.tile.TileEntity;

import org.lwjgl.BufferUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Main entry point for CraftZero.
 */
public class Main implements Runnable {

    private static final int TARGET_UPS = 20;
    private static final float FIXED_DELTA = 1.0f / TARGET_UPS;
    private static final int MAX_FIXED_UPDATES_PER_FRAME = 3;
    private static final float AUTOSAVE_INTERVAL = 60.0f;
    private static final float NETHER_PORTAL_TRANSFER_TIME = 4.0f;
    private static final float BED_SLEEP_TRANSITION_SECONDS = 1.0f;
    private static final float BED_SLEEP_OVERLAY_MAX_ALPHA = 0.86f;
    private static final int INITIAL_LOAD_READY_RADIUS = 2;
    private static final int TERRAIN_LOADING_BAR_WIDTH = 182;
    private static final int TERRAIN_LOADING_BAR_HEIGHT = 6;
    private static final int TERRAIN_LOADING_SHIMMER_SPACING = 8;
    private static final float TERRAIN_LOADING_SHIMMER_SPEED = 18.0f;
    private static final int PLAYER_LIST_MAX_ROWS = 20;
    private static final int PLAYER_LIST_ROW_HEIGHT = 10;
    private static final int PLAYER_LIST_HEADER_HEIGHT = 18;
    private static final boolean VSYNC = false;
    private static final float ANAGLYPH_HALF_EYE_OFFSET_BLOCKS = 0.035f;
    private static final int MULTIPLAYER_ARMOR_SLOTS = MultiplayerProtocol.INVENTORY_ARMOR_SLOTS;
    private static final int MULTIPLAYER_CURSOR_SLOT = MultiplayerProtocol.INVENTORY_CURSOR_SLOT;
    private static final int MULTIPLAYER_INVENTORY_SLOTS = MultiplayerProtocol.INVENTORY_SLOT_COUNT;
    private static final int MAX_MULTIPLAYER_PARTICLE_EVENTS_PER_TICK = 256;
    private static final int MAX_DEFERRED_NETWORK_BLOCK_UPDATES = 8192;
    private static final int MAX_DEFERRED_NETWORK_BLOCK_UPDATES_PER_TICK = 512;
    private static final float MULTIPLAYER_ENTITY_SYNC_INTERVAL = 0.15f;
    private static final float MULTIPLAYER_TILE_SYNC_INTERVAL = 0.25f;
    private static final float MULTIPLAYER_REMOTE_PLAYER_WIDTH = 0.6f;
    private static final float MULTIPLAYER_REMOTE_PLAYER_HEIGHT = 1.8f;
    private static final float MULTIPLAYER_REMOTE_DAMAGE_COOLDOWN_SECONDS = 0.5f;
    private static final float MULTIPLAYER_REMOTE_FIRE_CONTACT_DAMAGE = 1.0f;
    private static final float MULTIPLAYER_REMOTE_LAVA_CONTACT_DAMAGE = 4.0f;
    private static final float MULTIPLAYER_REMOTE_CACTUS_CONTACT_DAMAGE = 1.0f;
    private static final float MULTIPLAYER_REMOTE_SUFFOCATION_DAMAGE = 1.0f;
    private static final int MULTIPLAYER_REMOTE_FIRE_CONTACT_TICKS = 160;
    private static final int MULTIPLAYER_REMOTE_LAVA_CONTACT_TICKS = 300;
    private static final float MULTIPLAYER_EXPLOSION_ENTITY_RADIUS_MULTIPLIER = 2.0f;
    private static final float MULTIPLAYER_EXPLOSION_ENTITY_DAMAGE_SCALE = 8.0f;
    private static final float MULTIPLAYER_LIGHTNING_DAMAGE = 5.0f;
    private static final float MULTIPLAYER_LIGHTNING_ENTITY_RADIUS = 3.0f;
    private static final int MULTIPLAYER_LIGHTNING_FIRE_TICKS = 160;
    private static final int DEFAULT_SERVER_SPAWN_PROTECTION = 16;
    private static final int DEFAULT_SERVER_VIEW_DISTANCE = MultiplayerProtocol.DEFAULT_VIEW_DISTANCE;
    private static final List<LanguageOption> RELEASE_LANGUAGE_OPTIONS = List.of(
            new LanguageOption("en_US", "English (US)"),
            new LanguageOption("en_GB", "English (UK)"),
            new LanguageOption("de_DE", "Deutsch (DE)"),
            new LanguageOption("es_ES", "Espanol (ES)"),
            new LanguageOption("fr_FR", "Francais (FR)"),
            new LanguageOption("it_IT", "Italiano (IT)"),
            new LanguageOption("nl_NL", "Nederlands (NL)"),
            new LanguageOption("pt_BR", "Portugues (BR)"),
            new LanguageOption("pl_PL", "Polski (PL)"),
            new LanguageOption("sv_SE", "Svenska (SE)"));
    private static final DateTimeFormatter SCREENSHOT_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

    private record LanguageOption(String code, String displayName) {
    }

    private Window window;
    private Timer timer;
    private Renderer renderer;
    private SkyRenderer skyRenderer;
    private CloudRenderer cloudRenderer;
    private ChestRenderer chestRenderer;
    private EnchantingTableRenderer enchantingTableRenderer;
    private DayCycleManager dayCycleManager;
    private HudRenderer hudRenderer;
    private SurvivalHudRenderer survivalHudRenderer;
    private BlockHighlightRenderer blockHighlightRenderer;
    private BlockBreakingRenderer blockBreakingRenderer;
    private InventoryScreen inventoryScreen;
    private ChestScreen chestScreen;
    private FurnaceScreen furnaceScreen;
    private DispenserScreen dispenserScreen;
    private BrewingStandScreen brewingStandScreen;
    private EnchantingTableScreen enchantingTableScreen;
    private SignEditScreen signEditScreen;
    private InventoryRenderer inventoryRenderer;
    private InventoryPlayerRenderer inventoryPlayerRenderer;
    private DroppedItemRenderer droppedItemRenderer;
    private MobRenderer mobRenderer;
    private MobSpawnerRenderer mobSpawnerRenderer;
    private ArrowRenderer arrowRenderer;
    private FallingBlockRenderer fallingBlockRenderer;
    private MovingPistonRenderer movingPistonRenderer;
    private LightningRenderer lightningRenderer;
    private ParticleRenderer particleRenderer;
    private PrecipitationRenderer precipitationRenderer;
    private SignTextRenderer signTextRenderer;
    private com.craftzero.graphics.PlayerRenderer playerRenderer;
    private CraftingTableScreen craftingTableScreen;
    private com.craftzero.graphics.DeathScreen deathScreen;
    private TextRenderer textRenderer;
    private TextRenderer enchantmentTextRenderer;
    private MenuRenderer menuRenderer;
    private ScreenManager screenManager;
    private ChatOverlay chatOverlay;
    private CommandDispatcher commandDispatcher;
    private final ConcurrentLinkedQueue<String> pendingChatMessages = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<NetworkMessage> pendingNetworkMessages = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<NetworkMessage> deferredNetworkBlockUpdates = new ConcurrentLinkedQueue<>();

    private World world;
    private MobSpawner mobSpawner;
    private Player player;
    private SaveManager saveManager;
    private SaveManager.LevelData loadedLevel;
    private WorldInfo currentWorldInfo;
    private GameMode currentGameMode = GameMode.SURVIVAL;
    private Difficulty currentDifficulty = Difficulty.EASY;
    private boolean currentHardcore;
    private boolean deathMenuOpen;

    private GameSettings settings;
    private WorldSoundDispatcher soundDispatcher;
    private OpenAlSoundSink soundSink;
    private AmbientMusicScheduler ambientMusicScheduler;
    private WorldManager worldManager;
    private boolean screenshotRequested;
    private boolean debugOverlayVisible;
    private ResourcePackManager resourcePackManager;
    private MultiplayerServer multiplayerServer;
    private MultiplayerClient multiplayerClient;
    private boolean clientMultiplayerWorld;
    private float multiplayerStateTimer;
    private float multiplayerEntityTimer;
    private float multiplayerTileTimer;
    private boolean applyingNetworkBlockUpdate;
    private final String[] multiplayerInventorySnapshot = new String[MULTIPLAYER_INVENTORY_SLOTS];
    private final Map<String, RemotePlayerView> remotePlayers = new LinkedHashMap<>();
    private final Map<String, MultiplayerRosterEntry> multiplayerRoster = new LinkedHashMap<>();
    private final Map<String, Entity> remoteEntities = new HashMap<>();
    private final Map<String, DroppedItem> remoteDroppedItems = new HashMap<>();
    private final Map<String, FishingHookEntity> multiplayerFishingHooks = new HashMap<>();
    private final Map<String, String> multiplayerVehicleRidersByEntityId = new HashMap<>();
    private final Map<String, String> multiplayerVehicleEntityByPlayerId = new HashMap<>();
    private final Map<String, org.joml.Vector3f> multiplayerRespawnOverrides = new HashMap<>();
    private final Map<String, Float> multiplayerExperiencePickupCooldowns = new HashMap<>();
    private final Map<String, Float> multiplayerRemoteDamageCooldowns = new HashMap<>();
    private final Set<String> multiplayerRemoteSprintKnockbackUsed = new HashSet<>();
    private final IdentityHashMap<Entity, String> multiplayerEntityIds = new IdentityHashMap<>();
    private final IdentityHashMap<DroppedItem, String> multiplayerDroppedItemIds = new IdentityHashMap<>();
    private final Set<String> lastMultiplayerEntityIds = new HashSet<>();
    private final Set<String> lastMultiplayerDroppedItemIds = new HashSet<>();
    private final Player.PlayerActionHandler multiplayerPlayerActionHandler = new Player.PlayerActionHandler() {
        @Override
        public Player.PlayerHit findAttackTarget(org.joml.Vector3f origin, org.joml.Vector3f direction,
                float maxDistance) {
            return findMultiplayerPlayerAttackTarget(origin, direction, maxDistance);
        }

        @Override
        public boolean attackPlayer(Player.PlayerHit hit, Player.PlayerAttack attack) {
            return applyMultiplayerPlayerAttack(hit, attack);
        }
    };
    private final Player.DeathDropHandler multiplayerDeathDropHandler = new Player.DeathDropHandler() {
        @Override
        public void dropExperience(float x, float y, float z, int amount) {
            syncMultiplayerDeathDropSourceState();
            sendMultiplayerDeathExperienceDropAction(amount);
        }

        @Override
        public void dropStack(int sourceSlot, float x, float y, float z, ItemStack stack,
                float velocityX, float velocityY, float velocityZ, int pickupDelayTicks) {
            syncMultiplayerDeathDropSourceState();
            sendMultiplayerDeathDropStackAction(sourceSlot, stack, velocityX, velocityY, velocityZ, pickupDelayTicks);
        }
    };
    private int nextMultiplayerEntityId = 1;
    private int nextMultiplayerDroppedItemId = 1;
    private boolean multiplayerDeathDropStateSynced;
    private boolean multiplayerRespawnRequestPending;

    private float autosaveTimer;
    private ExecutorService saveExecutor;
    private Future<?> autosaveFuture;
    private boolean savingEnabled = true;
    private boolean currentAllowCheats;
    private String weatherState = "clear";
    private int worldSpawnX;
    private int worldSpawnY = 80;
    private int worldSpawnZ;
    private World.BedUseResult activeBedSleep;
    private float activeBedSleepTimer;
    private boolean multiplayerSleepCompletePending;
    private final java.util.Set<String> operators = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final java.util.Set<String> bannedPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final java.util.Set<String> bannedIps = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final java.util.Set<String> whitelist = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private boolean whitelistEnabled;
    private String multiplayerServerIp = "";
    private int multiplayerServerPort = MultiplayerProtocol.DEFAULT_PORT;
    private int multiplayerMaxPlayers = MultiplayerProtocol.DEFAULT_MAX_PLAYERS;
    private boolean multiplayerPvp = true;
    private boolean multiplayerSpawnAnimals = true;
    private boolean multiplayerSpawnMonsters = true;
    private boolean multiplayerSpawnNpcs = true;
    private boolean multiplayerAllowNether = true;
    private boolean multiplayerOnlineMode;
    private boolean multiplayerAllowFlight;
    private boolean multiplayerEnableQuery;
    private int multiplayerQueryPort = MultiplayerProtocol.DEFAULT_QUERY_PORT;
    private int multiplayerSpawnProtection = DEFAULT_SERVER_SPAWN_PROTECTION;
    private int multiplayerViewDistance = DEFAULT_SERVER_VIEW_DISTANCE;
    private int multiplayerMaxBuildHeight = MultiplayerProtocol.DEFAULT_MAX_BUILD_HEIGHT;
    private boolean multiplayerGenerateStructures = MultiplayerProtocol.DEFAULT_GENERATE_STRUCTURES;
    private float dimensionTransferCooldown;
    private float netherPortalTime;
    private boolean hostAfterTerrainLoad;
    private float terrainLoadingTime;
    private World.ChunkAreaProgress terrainLoadProgress = new World.ChunkAreaProgress(1, 0, 0, 0);

    private boolean running;
    private boolean paused;
    private GameState gameState = GameState.TITLE;

    private void init() throws Exception {
        window = new Window("CraftZero", 1280, 720, VSYNC);
        window.init();
        Input.init(window);

        timer = new Timer();
        timer.init();

        settings = GameSettings.loadOrCreate(GameSettings.DEFAULT_OPTIONS_PATH);
        applyWindowSettings();
        resourcePackManager = new ResourcePackManager();
        resourcePackManager.setSelectedPackId(settings.getSelectedTexturePack());
        ResourcePackManager.setActive(resourcePackManager);
        initSoundDispatcher();
        worldManager = new WorldManager(WorldManager.DEFAULT_SAVES_ROOT);

        renderer = new Renderer();
        renderer.init();

        textRenderer = new TextRenderer();
        textRenderer.init(window.getWidth(), window.getHeight());
        enchantmentTextRenderer = new TextRenderer("/textures/font/alternate.png");
        enchantmentTextRenderer.init(window.getWidth(), window.getHeight());

        com.craftzero.graphics.GuiTexture.init();

        skyRenderer = new SkyRenderer();
        skyRenderer.init();
        cloudRenderer = new CloudRenderer();
        cloudRenderer.init();
        chestRenderer = new ChestRenderer();
        chestRenderer.init(
                com.craftzero.graphics.GuiTexture.getChestTexture(),
                com.craftzero.graphics.GuiTexture.getLargeChestTexture());
        enchantingTableRenderer = new EnchantingTableRenderer();
        enchantingTableRenderer.init(com.craftzero.graphics.GuiTexture.getBookTexture());

        hudRenderer = new HudRenderer();
        hudRenderer.init(window);
        survivalHudRenderer = new SurvivalHudRenderer();
        survivalHudRenderer.init(window);
        survivalHudRenderer.setTextRenderer(textRenderer);
        survivalHudRenderer.setGuiTextures(
                com.craftzero.graphics.GuiTexture.getIconsTexture(),
                com.craftzero.graphics.GuiTexture.getGuiTexture());
        survivalHudRenderer.setItemsTexture(com.craftzero.graphics.GuiTexture.getItemsTexture());

        blockHighlightRenderer = new BlockHighlightRenderer();
        blockHighlightRenderer.init();
        blockBreakingRenderer = new BlockBreakingRenderer();
        blockBreakingRenderer.init();

        inventoryRenderer = new InventoryRenderer();
        inventoryRenderer.init(window.getWidth(), window.getHeight());
        inventoryRenderer.setTextRenderer(textRenderer);
        inventoryRenderer.setEnchantmentTextRenderer(enchantmentTextRenderer);
        inventoryRenderer.setGuiTextures(
                com.craftzero.graphics.GuiTexture.getInventoryTexture(),
                com.craftzero.graphics.GuiTexture.getCraftingTexture());
        inventoryRenderer.setCreativeTexture(com.craftzero.graphics.GuiTexture.getAllItemsTexture());
        inventoryRenderer.setContainerTextures(
                com.craftzero.graphics.GuiTexture.getContainerTexture(),
                com.craftzero.graphics.GuiTexture.getFurnaceTexture(),
                com.craftzero.graphics.GuiTexture.getTrapTexture());
        inventoryRenderer.setProgressionTextures(
                com.craftzero.graphics.GuiTexture.getEnchantTexture(),
                com.craftzero.graphics.GuiTexture.getAlchemyTexture());
        inventoryRenderer.setItemsTexture(com.craftzero.graphics.GuiTexture.getItemsTexture());

        inventoryPlayerRenderer = new InventoryPlayerRenderer();
        inventoryPlayerRenderer.init();
        inventoryPlayerRenderer.updateScreenSize(window.getWidth(), window.getHeight());
        inventoryRenderer.setPlayerRenderer(inventoryPlayerRenderer);

        droppedItemRenderer = new DroppedItemRenderer();
        droppedItemRenderer.init();
        mobRenderer = new MobRenderer(renderer);
        mobRenderer.init();
        mobSpawnerRenderer = new MobSpawnerRenderer(mobRenderer);
        arrowRenderer = new ArrowRenderer(renderer);
        arrowRenderer.init();
        fallingBlockRenderer = new FallingBlockRenderer(renderer);
        fallingBlockRenderer.init();
        movingPistonRenderer = new MovingPistonRenderer(renderer);
        movingPistonRenderer.init();
        lightningRenderer = new LightningRenderer(renderer);
        lightningRenderer.init();
        particleRenderer = new ParticleRenderer(renderer);
        particleRenderer.init();
        precipitationRenderer = new PrecipitationRenderer(renderer);
        precipitationRenderer.init();
        signTextRenderer = new SignTextRenderer(renderer);
        signTextRenderer.init();
        playerRenderer = new com.craftzero.graphics.PlayerRenderer(renderer);
        playerRenderer.init();

        deathScreen = new com.craftzero.graphics.DeathScreen();
        deathScreen.init(window);
        deathScreen.setTextRenderer(textRenderer);

        menuRenderer = new MenuRenderer();
        menuRenderer.init(window.getWidth(), window.getHeight(), textRenderer);
        updateMenuScale();
        screenManager = new ScreenManager();
        screenManager.setButtonClickSound(this::playMenuClickSound);
        chatOverlay = new ChatOverlay();
        commandDispatcher = new CommandDispatcher();

        saveExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "CraftZeroSaveWorker");
            thread.setDaemon(true);
            return thread;
        });

        openTitleScreen();
        System.out.println("CraftZero menu shell initialized. Select Singleplayer to load or create a world.");
    }

    private void gameLoop() {
        float accumulator = 0.0f;
        float lastFpsUpdate = 0;
        int frameCount = 0;

        while (running && !window.shouldClose()) {
            long frameStartNanos = System.nanoTime();
            Input.update();
            window.update();

            timer.update();
            float deltaTime = timer.getDeltaTime();
            accumulator += deltaTime;

            handleInput();

            float partialTick;
            if (isWorldSimulationPaused()) {
                accumulator = 0.0f;
                partialTick = 1.0f;
            } else {
                int fixedUpdates = 0;
                while (accumulator >= FIXED_DELTA && fixedUpdates < MAX_FIXED_UPDATES_PER_FRAME) {
                    update(FIXED_DELTA);
                    accumulator -= FIXED_DELTA;
                    fixedUpdates++;
                }
                if (accumulator >= FIXED_DELTA) {
                    accumulator = 0.0f;
                }
                partialTick = accumulator / FIXED_DELTA;
            }

            render(deltaTime, partialTick);

            frameCount++;
            lastFpsUpdate += deltaTime;
            if (lastFpsUpdate >= 1.0f) {
                updateWindowTitle(frameCount);
                frameCount = 0;
                lastFpsUpdate = 0;
            }

            sleepForFrameLimit(frameStartNanos);
        }
    }

    private void updateWindowTitle(int frameCount) {
        if (player == null) {
            glfwSetWindowTitle(window.getHandle(), "CraftZero - FPS: " + frameCount + " | " + gameState);
            return;
        }
        String flyMode = player.isFlying() ? " [FLYING]" : "";
        String blockName = "Empty";
        if (player.getInventory().getItemInHand() != null) {
            blockName = player.getInventory().getItemInHand().getType().name();
        }
        String title = String.format(
                "CraftZero - FPS: %d | %s | Pos: %.1f, %.1f, %.1f | Block: %s%s",
                frameCount,
                currentGameMode,
                player.getPosition().x,
                player.getPosition().y,
                player.getPosition().z,
                blockName,
                flyMode);
        glfwSetWindowTitle(window.getHandle(), title);
    }

    private void sleepForFrameLimit(long frameStartNanos) {
        if (settings == null || settings.isVsync()) {
            return;
        }
        int limit = settings.getFramerateLimit();
        if (limit <= 0) {
            return;
        }
        long targetNanos = 1_000_000_000L / Math.max(1, limit);
        long elapsed = System.nanoTime() - frameStartNanos;
        long remaining = targetNanos - elapsed;
        if (remaining <= 0) {
            return;
        }
        try {
            long millis = remaining / 1_000_000L;
            int nanos = (int) (remaining % 1_000_000L);
            Thread.sleep(millis, nanos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private MenuInput menuInput() {
        int scale = currentGuiScale();
        return new MenuInput(
                guiWidth(),
                guiHeight(),
                Input.getMouseX() / scale,
                Input.getMouseY() / scale,
                Input.isButtonDown(GLFW_MOUSE_BUTTON_LEFT),
                Input.getScrollY(),
                Input.getPressedKeys(),
                Input.getDownKeys(),
                Input.getTypedCharacters());
    }

    private int currentGuiScale() {
        if (window == null || settings == null) {
            return 1;
        }
        return GuiScale.compute(settings.getGuiScale(), window.getWidth(), window.getHeight());
    }

    private int guiWidth() {
        return Math.max(1, window.getWidth() / currentGuiScale());
    }

    private int guiHeight() {
        return Math.max(1, window.getHeight() / currentGuiScale());
    }

    private void applyRuntimeSettings(boolean rebuildMenu) {
        applyWindowSettings();
        updateMenuScale();
        if (resourcePackManager != null) {
            resourcePackManager.setSelectedPackId(settings.getSelectedTexturePack());
            ResourcePackManager.setActive(resourcePackManager);
        }
        if (player != null) {
            player.applySettings(settings);
            player.getCamera().setFov(settingsFovDegrees());
            currentDifficulty = currentHardcore ? Difficulty.HARD : settings.getDifficulty();
            player.setDifficulty(currentDifficulty);
        }
        if (world != null) {
            world.setRenderDistanceChunks(effectiveWorldRenderDistanceChunks());
            world.setFancyGraphics(settings.isFancyGraphics());
            world.setSmoothLighting(settings.isSmoothLighting());
            world.setAdvancedOpenGl(settings.isAdvancedOpenGl());
        } else {
            com.craftzero.world.BlockType.setFancyGraphics(settings.isFancyGraphics());
            com.craftzero.world.ChunkMeshBuilder.setSmoothLightingEnabled(settings.isSmoothLighting());
        }
        if (rebuildMenu && screenManager != null && screenManager.hasScreen()) {
            rebuildCurrentMenuForResize();
        }
    }

    private void applyWindowSettings() {
        if (window == null || settings == null) {
            return;
        }
        window.setVSync(settings.isVsync());
        window.setFullscreen(settings.isFullscreen());
    }

    private void updateMenuScale() {
        if (menuRenderer != null && settings != null) {
            menuRenderer.setGuiScale(settings.getGuiScale());
        }
    }

    private float settingsFovDegrees() {
        return 70.0f + settings.getFov() * 40.0f;
    }

    private int renderDistanceChunks(int chunks) {
        return GameSettings.effectiveRenderDistanceChunks(chunks);
    }

    private int effectiveWorldRenderDistanceChunks() {
        int localDistance = settings == null ? GameSettings.DEFAULT_RENDER_DISTANCE_CHUNKS
                : renderDistanceChunks(settings.getRenderDistance());
        if (!clientMultiplayerWorld) {
            return localDistance;
        }
        return Math.max(GameSettings.EFFECTIVE_MIN_RENDER_DISTANCE_CHUNKS,
                Math.min(localDistance, multiplayerViewDistance));
    }

    private boolean isBindingPressed(GameSettings.KeyBinding binding) {
        return GameInput.isBindingPressed(settings, binding);
    }

    private boolean isInventoryBindingPressed() {
        return isBindingPressed(GameSettings.KeyBinding.INVENTORY);
    }

    private boolean isDropBindingPressed() {
        return isBindingPressed(GameSettings.KeyBinding.DROP);
    }

    private void handleInput() {
        MenuInput menuInput = menuInput();

        if (Input.isKeyPressed(GLFW_KEY_F11)) {
            window.toggleFullscreen();
            settings.setFullscreen(window.isFullscreen());
            applyRuntimeSettings(false);
            saveSettings();
        }
        if (isBindingPressed(GameSettings.KeyBinding.SCREENSHOT)) {
            screenshotRequested = true;
        }
        if (Input.isKeyPressed(GLFW_KEY_F3)) {
            debugOverlayVisible = !debugOverlayVisible;
        }

        Screen activeScreen = screenManager.currentScreen();
        if (activeScreen != null) {
            if (activeScreen instanceof CreativeInventoryScreen
                    && isBindingPressed(GameSettings.KeyBinding.INVENTORY)) {
                screenManager.handleBack();
                return;
            }
            screenManager.update(menuInput);
            if (activeScreen.consumesInput()) {
                if (player != null) {
                    player.clearMovementInputState();
                }
                return;
            }
        }

        if (player == null || world == null) {
            return;
        }

        if (chatOverlay != null && chatOverlay.isOpen()) {
            player.clearMovementInputState();
            chatOverlay.update(menuInput, input -> commandDispatcher.suggestions(input, commandContext()))
                    .ifPresent(this::handleChatSubmit);
            if (!chatOverlay.isOpen() && gameState == GameState.PLAYING) {
                Input.setCursorLocked(true);
            }
            return;
        }

        if (player.isDead()) {
            player.clearMovementInputState();
            openDeathMenuIfNeeded();
            Input.setCursorLocked(false);
            return;
        }
        if (player.isSleeping()) {
            if (isLeaveBedPressed()) {
                leaveActiveBedSleep();
                return;
            }
            if (!paused && gameState == GameState.PLAYING && noGameplayScreenOpen()) {
                if (isBindingPressed(GameSettings.KeyBinding.CHAT)) {
                    player.clearMovementInputState();
                    chatOverlay.open(false);
                    Input.setCursorLocked(false);
                    return;
                }
                if (isBindingPressed(GameSettings.KeyBinding.COMMAND)) {
                    player.clearMovementInputState();
                    chatOverlay.open(true);
                    Input.setCursorLocked(false);
                    return;
                }
            }
            updateGameplayScreens();
            player.clearMovementInputState();
            return;
        }

        if (isBindingPressed(GameSettings.KeyBinding.TOGGLE_PERSPECTIVE)) {
            player.cycleCameraMode();
        }
        if (isBindingPressed(GameSettings.KeyBinding.SMOOTH_CAMERA)) {
            player.toggleSmoothCamera();
            addChatMessage("Smooth camera: " + (player.isSmoothCamera() ? "ON" : "OFF"));
        }

        if (isBindingPressed(GameSettings.KeyBinding.INVENTORY) && !craftingTableScreen.isOpen()
                && !chestScreen.isOpen() && !furnaceScreen.isOpen() && !dispenserScreen.isOpen()
                && !brewingStandScreen.isOpen() && !enchantingTableScreen.isOpen() && !signEditScreen.isOpen()) {
            if (player.isCreative()) {
                recordInventoryOpenedAchievement();
                openCreativeInventory();
            } else {
                boolean wasOpen = inventoryScreen.isOpen();
                inventoryScreen.toggle(window.getWidth(), window.getHeight());
                if (!wasOpen && inventoryScreen.isOpen()) {
                    recordInventoryOpenedAchievement();
                }
            }
        }

        if (Input.isKeyPressed(GLFW_KEY_ESCAPE)) {
            if (closeGameplayScreen()) {
                return;
            }
            openPauseScreen();
            return;
        }

        if (!paused && gameState == GameState.PLAYING && noGameplayScreenOpen()) {
            if (isBindingPressed(GameSettings.KeyBinding.CHAT)) {
                player.clearMovementInputState();
                chatOverlay.open(false);
                Input.setCursorLocked(false);
                return;
            }
            if (isBindingPressed(GameSettings.KeyBinding.COMMAND)) {
                player.clearMovementInputState();
                chatOverlay.open(true);
                Input.setCursorLocked(false);
                return;
            }
        }

        updateGameplayScreens();

        if (!paused && gameState == GameState.PLAYING && noGameplayScreenOpen()) {
            player.handleInput(timer.getDeltaTime());
            player.handleBlockInteraction(world, timer.getDeltaTime());
            flushMultiplayerEntityActions();
            handlePlayerOpenRequests();
        } else if (player != null) {
            player.clearMovementInputState();
        }
    }

    private boolean isWorldSimulationPaused() {
        if (world == null || player == null || gameState == GameState.LOADING_WORLD) {
            return false;
        }
        Screen screen = screenManager.currentScreen();
        return paused || gameState != GameState.PLAYING || !window.isFocused()
                || (screen != null && screen.pausesGame());
    }

    private boolean isLeaveBedPressed() {
        return Input.isKeyPressed(GLFW_KEY_ESCAPE)
                || isBindingPressed(GameSettings.KeyBinding.JUMP)
                || isBindingPressed(GameSettings.KeyBinding.SNEAK)
                || isBindingPressed(GameSettings.KeyBinding.ATTACK)
                || isBindingPressed(GameSettings.KeyBinding.USE);
    }

    private boolean closeGameplayScreen() {
        if (craftingTableScreen != null && craftingTableScreen.isOpen()) {
            craftingTableScreen.close();
            throwScreenItems(craftingTableScreen.getAndClearItemsToThrow(), 4.0f, 2.0f);
            return true;
        }
        if (chestScreen != null && chestScreen.isOpen()) {
            chestScreen.close();
            throwScreenItems(chestScreen.getAndClearItemsToThrow(), 4.0f, 2.0f);
            return true;
        }
        if (furnaceScreen != null && furnaceScreen.isOpen()) {
            furnaceScreen.close();
            throwScreenItems(furnaceScreen.getAndClearItemsToThrow(), 4.0f, 2.0f);
            return true;
        }
        if (dispenserScreen != null && dispenserScreen.isOpen()) {
            dispenserScreen.close();
            throwScreenItems(dispenserScreen.getAndClearItemsToThrow(), 4.0f, 2.0f);
            return true;
        }
        if (brewingStandScreen != null && brewingStandScreen.isOpen()) {
            brewingStandScreen.close();
            throwScreenItems(brewingStandScreen.getAndClearItemsToThrow(), 4.0f, 2.0f);
            return true;
        }
        if (enchantingTableScreen != null && enchantingTableScreen.isOpen()) {
            enchantingTableScreen.close();
            throwScreenItems(enchantingTableScreen.getAndClearItemsToThrow(), 4.0f, 2.0f);
            return true;
        }
        if (signEditScreen != null && signEditScreen.isOpen()) {
            closeSignEditScreen();
            return true;
        }
        if (inventoryScreen != null && inventoryScreen.isOpen()) {
            inventoryScreen.close();
            throwInventoryScreenItems(inventoryScreen.getAndClearItemsToThrow());
            return true;
        }
        return false;
    }

    private boolean noGameplayScreenOpen() {
        return !inventoryScreen.isOpen() && !craftingTableScreen.isOpen()
                && !chestScreen.isOpen() && !furnaceScreen.isOpen() && !dispenserScreen.isOpen()
                && !brewingStandScreen.isOpen() && !enchantingTableScreen.isOpen() && !signEditScreen.isOpen();
    }

    private void updateGameplayScreens() {
        if (craftingTableScreen.isOpen()) {
            updateCraftingTableScreen();
        }
        if (chestScreen.isOpen()) {
            updateChestScreen();
        }
        if (furnaceScreen.isOpen()) {
            updateFurnaceScreen();
        }
        if (dispenserScreen.isOpen()) {
            updateDispenserScreen();
        }
        if (brewingStandScreen.isOpen()) {
            updateBrewingStandScreen();
        }
        if (enchantingTableScreen.isOpen()) {
            updateEnchantingTableScreen();
        }
        if (signEditScreen.isOpen()) {
            signEditScreen.update(window.getWidth(), window.getHeight());
            if (signEditScreen.consumeCloseRequest()) {
                closeSignEditScreen();
            }
        }
        if (!signEditScreen.isOpen()) {
            inventoryScreen.update();
        }
        throwInventoryScreenItems(inventoryScreen.getAndClearItemsToThrow());
        syncClientMultiplayerCraftActions(inventoryScreen.drainCraftActions());
    }

    private void closeSignEditScreen() {
        if (signEditScreen == null || !signEditScreen.isOpen()) {
            return;
        }
        SignTileEntity sign = signEditScreen.getSign();
        if (sign != null) {
            if (clientMultiplayerWorld && multiplayerClient != null && multiplayerClient.isConnected()) {
                sendMultiplayerSignUpdate(sign);
            } else if (multiplayerServer != null) {
                broadcastMultiplayerTileEntity(sign);
            }
        }
        signEditScreen.close();
        Input.setCursorLocked(true);
    }

    private void sendMultiplayerSignUpdate(SignTileEntity sign) {
        if (sign == null || multiplayerClient == null || !multiplayerClient.isConnected()) {
            return;
        }
        try {
            BlockPos pos = sign.getPos();
            com.google.gson.JsonObject data = NetworkMessage.object();
            data.addProperty("action", MultiplayerProtocol.ACTION_SIGN_UPDATE);
            data.addProperty("x", pos.x());
            data.addProperty("y", pos.y());
            data.addProperty("z", pos.z());
            String[] lines = sign.getLines();
            for (int i = 0; i < MultiplayerProtocol.SIGN_LINE_COUNT; i++) {
                data.addProperty("signLine" + i, i < lines.length && lines[i] != null ? lines[i] : "");
            }
            multiplayerClient.send(NetworkMessage.of("clientAction", data));
        } catch (Exception e) {
            System.err.println("Could not sync multiplayer sign text: " + e.getMessage());
        }
    }

    private void syncClientMultiplayerOpenContainer(TileEntity tile) {
        if (!clientMultiplayerWorld || multiplayerClient == null || !multiplayerClient.isConnected()
                || tile == null || !tile.isDirty() || networkEditableContainerInventory(tile) == null) {
            return;
        }
        if (sendMultiplayerContainerUpdate(tile)) {
            tile.clearDirty();
        }
    }

    private boolean sendMultiplayerContainerUpdate(TileEntity tile) {
        if (tile == null || multiplayerClient == null || !multiplayerClient.isConnected()) {
            return false;
        }
        ItemStack[] inventory = networkEditableContainerInventory(tile);
        if (inventory == null) {
            return false;
        }
        try {
            HashMap<String, String> snapshot = new HashMap<>();
            BlockPos pos = tile.getPos();
            snapshot.put("action", MultiplayerProtocol.ACTION_CONTAINER_UPDATE);
            snapshot.put("x", Integer.toString(pos.x()));
            snapshot.put("y", Integer.toString(pos.y()));
            snapshot.put("z", Integer.toString(pos.z()));
            snapshot.put("tileType", tile.getTypeId());
            putTileInventoryData(snapshot, "tile.inventory", inventory);

            com.google.gson.JsonObject data = NetworkMessage.object();
            for (Map.Entry<String, String> entry : snapshot.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    data.addProperty(entry.getKey(), entry.getValue());
                }
            }
            multiplayerClient.send(NetworkMessage.of("clientAction", data));
            return true;
        } catch (Exception e) {
            System.err.println("Could not sync multiplayer container update: " + e.getMessage());
            return false;
        }
    }

    private void syncClientMultiplayerOpenChestMinecart(ChestScreen screen) {
        if (!clientMultiplayerWorld || multiplayerClient == null || !multiplayerClient.isConnected()
                || screen == null || !screen.isMinecartDirty()) {
            return;
        }
        ChestMinecartEntity minecart = screen.getMinecart();
        if (minecart == null || minecart.isRemoved()) {
            screen.clearMinecartDirty();
            return;
        }
        String entityId = remoteEntityNetworkId(minecart);
        if (entityId == null || entityId.isBlank()) {
            return;
        }
        if (sendMultiplayerChestMinecartContainerUpdate(entityId, minecart)) {
            screen.clearMinecartDirty();
        }
    }

    private boolean sendMultiplayerChestMinecartContainerUpdate(String entityId, ChestMinecartEntity minecart) {
        if (entityId == null || entityId.isBlank() || minecart == null
                || multiplayerClient == null || !multiplayerClient.isConnected()) {
            return false;
        }
        try {
            HashMap<String, String> snapshot = new HashMap<>();
            snapshot.put("action", MultiplayerProtocol.ACTION_CONTAINER_UPDATE);
            snapshot.put("entityId", entityId);
            snapshot.put("tileType", "chest_minecart");
            putTileInventoryData(snapshot, "tile.inventory", minecart.getInventory());

            com.google.gson.JsonObject data = NetworkMessage.object();
            for (Map.Entry<String, String> entry : snapshot.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    data.addProperty(entry.getKey(), entry.getValue());
                }
            }
            multiplayerClient.send(NetworkMessage.of("clientAction", data));
            return true;
        } catch (Exception e) {
            System.err.println("Could not sync multiplayer chest minecart inventory: " + e.getMessage());
            return false;
        }
    }

    private void syncClientMultiplayerEnchantingTable(EnchantingTableScreen screen) {
        if (screen == null) {
            return;
        }
        EnchantingTableScreen.EnchantAction action = screen.drainEnchantAction();
        if (action == null || !clientMultiplayerWorld || multiplayerClient == null
                || !multiplayerClient.isConnected()) {
            return;
        }
        sendMultiplayerEnchantItemAction(action);
    }

    private boolean sendMultiplayerEnchantItemAction(EnchantingTableScreen.EnchantAction action) {
        if (action == null || action.tablePos() == null || action.inputItem() == null
                || action.inputItem().isEmpty() || multiplayerClient == null || !multiplayerClient.isConnected()) {
            return false;
        }
        try {
            HashMap<String, String> snapshot = new HashMap<>();
            snapshot.put("action", MultiplayerProtocol.ACTION_ENCHANT_ITEM);
            snapshot.put("x", Integer.toString(action.tablePos().x));
            snapshot.put("y", Integer.toString(action.tablePos().y));
            snapshot.put("z", Integer.toString(action.tablePos().z));
            snapshot.put("offerSlot", Integer.toString(action.offerSlot()));
            snapshot.put("offerCost", Integer.toString(action.cost()));
            snapshot.put("offerSeed", Long.toString(action.offerSeed()));
            putItemStackData(snapshot, "table.item", action.inputItem());

            com.google.gson.JsonObject data = NetworkMessage.object();
            for (Map.Entry<String, String> entry : snapshot.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    data.addProperty(entry.getKey(), entry.getValue());
                }
            }
            multiplayerClient.send(NetworkMessage.of("clientAction", data));
            return true;
        } catch (Exception e) {
            System.err.println("Could not sync multiplayer enchanting action: " + e.getMessage());
            return false;
        }
    }

    private void syncClientMultiplayerCraftActions(List<CraftAction> actions) {
        if (actions == null || actions.isEmpty() || !clientMultiplayerWorld
                || multiplayerClient == null || !multiplayerClient.isConnected()) {
            return;
        }
        for (CraftAction action : actions) {
            sendMultiplayerCraftAction(action);
        }
    }

    private boolean sendMultiplayerCraftAction(CraftAction action) {
        if (action == null || action.crafts() <= 0 || multiplayerClient == null
                || !multiplayerClient.isConnected()) {
            return false;
        }
        if (action.gridSize() == 3 && action.tablePos() == null) {
            return false;
        }
        try {
            HashMap<String, String> snapshot = new HashMap<>();
            snapshot.put("action", MultiplayerProtocol.ACTION_CRAFT_ITEM);
            snapshot.put("gridSize", Integer.toString(action.gridSize()));
            snapshot.put("quickMove", Boolean.toString(action.quickMove()));
            snapshot.put("crafts", Integer.toString(Math.max(1, Math.min(action.crafts(), 64))));
            if (action.tablePos() != null) {
                snapshot.put("x", Integer.toString(action.tablePos().x));
                snapshot.put("y", Integer.toString(action.tablePos().y));
                snapshot.put("z", Integer.toString(action.tablePos().z));
            }
            putTileInventoryData(snapshot, "craft.grid", action.grid());
            putItemStackData(snapshot, "cursor", action.cursorBefore());

            com.google.gson.JsonObject data = NetworkMessage.object();
            for (Map.Entry<String, String> entry : snapshot.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    data.addProperty(entry.getKey(), entry.getValue());
                }
            }
            multiplayerClient.send(NetworkMessage.of("clientAction", data));
            return true;
        } catch (Exception e) {
            System.err.println("Could not sync multiplayer craft action: " + e.getMessage());
            return false;
        }
    }

    private void flushMultiplayerEntityActions() {
        if (player == null) {
            return;
        }
        List<Player.EntityActionRecord> actions = player.drainEntityActionRecords();
        List<Player.ItemActionRecord> itemActions = player.drainItemActionRecords();
        if ((actions.isEmpty() && itemActions.isEmpty())
                || !clientMultiplayerWorld || multiplayerClient == null || !multiplayerClient.isConnected()) {
            return;
        }
        for (Player.EntityActionRecord action : actions) {
            sendMultiplayerEntityAction(action);
        }
        for (Player.ItemActionRecord action : itemActions) {
            sendMultiplayerItemAction(action);
        }
    }

    private void sendMultiplayerEntityAction(Player.EntityActionRecord action) {
        if (action == null || action.entity() == null || action.actionType() == null
                || multiplayerClient == null || !multiplayerClient.isConnected()) {
            return;
        }
        String entityId = remoteEntityNetworkId(action.entity());
        if (entityId == null || entityId.isBlank()) {
            return;
        }
        try {
            com.google.gson.JsonObject data = NetworkMessage.object();
            data.addProperty("action", action.actionType() == Player.EntityActionType.ATTACK
                    ? MultiplayerProtocol.ACTION_ENTITY_ATTACK
                    : MultiplayerProtocol.ACTION_ENTITY_USE);
            data.addProperty("entityId", entityId);
            multiplayerClient.send(NetworkMessage.of("clientAction", data));
        } catch (Exception e) {
            System.err.println("Could not sync multiplayer entity action: " + e.getMessage());
        }
    }

    private String remoteEntityNetworkId(Entity entity) {
        if (entity == null) {
            return null;
        }
        for (Map.Entry<String, Entity> entry : remoteEntities.entrySet()) {
            if (entry.getValue() == entity) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void sendMultiplayerItemAction(Player.ItemActionRecord action) {
        if (action == null || action.actionType() == null
                || multiplayerClient == null || !multiplayerClient.isConnected()) {
            return;
        }
        try {
            com.google.gson.JsonObject data = NetworkMessage.object();
            data.addProperty("action", MultiplayerProtocol.ACTION_ITEM_USE);
            data.addProperty("useAction", multiplayerItemUseActionName(action.actionType()));
            data.addProperty("itemId", itemTypeNetworkId(action.itemType()));
            data.addProperty("dirX", action.directionX());
            data.addProperty("dirY", action.directionY());
            data.addProperty("dirZ", action.directionZ());
            data.addProperty("power", action.power());
            if (action.blockY() != Integer.MIN_VALUE) {
                data.addProperty("blockX", action.blockX());
                data.addProperty("blockY", action.blockY());
                data.addProperty("blockZ", action.blockZ());
            }
            if (action.blockFace() != Integer.MIN_VALUE) {
                data.addProperty("blockFace", action.blockFace());
            }
            multiplayerClient.send(NetworkMessage.of("clientAction", data));
        } catch (Exception e) {
            System.err.println("Could not sync multiplayer item action: " + e.getMessage());
        }
    }

    private String multiplayerItemUseActionName(Player.ItemActionType actionType) {
        return switch (actionType) {
            case BOW -> "bow";
            case THROW_ITEM -> "throw_item";
            case ENDER_PEARL -> "ender_pearl";
            case EYE_OF_ENDER -> "eye_of_ender";
            case SPLASH_POTION -> "splash_potion";
            case CONSUME_FOOD -> "consume_food";
            case DRINK_MILK -> "drink_milk";
            case DRINK_POTION -> "drink_potion";
            case USE_MAP -> "use_map";
            case EQUIP_ARMOR -> "equip_armor";
            case PLAY_NOTE_BLOCK -> "play_note_block";
            case TUNE_NOTE_BLOCK -> "tune_note_block";
            case INSERT_RECORD -> "insert_record";
            case EJECT_RECORD -> "eject_record";
            case PLACE_BOAT -> "place_boat";
            case PLACE_PAINTING -> "place_painting";
            case PLACE_MINECART -> "place_minecart";
            case FISHING_CAST -> "fishing_cast";
            case FISHING_REEL -> "fishing_reel";
        };
    }

    private void sendMultiplayerDropItemAction(ItemStack dropped, org.joml.Vector3f direction) {
        if (dropped == null || dropped.isEmpty() || multiplayerClient == null || !multiplayerClient.isConnected()) {
            return;
        }
        org.joml.Vector3f normalized = new org.joml.Vector3f(direction == null ? new org.joml.Vector3f(0, 0, 1) : direction);
        if (normalized.lengthSquared() <= 0.0001f) {
            normalized.set(0, 0, 1);
        } else {
            normalized.normalize();
        }
        try {
            com.google.gson.JsonObject data = NetworkMessage.object();
            data.addProperty("action", MultiplayerProtocol.ACTION_ITEM_USE);
            data.addProperty("useAction", "drop_item");
            data.addProperty("itemId", itemTypeNetworkId(dropped.getType()));
            data.addProperty("dirX", normalized.x);
            data.addProperty("dirY", normalized.y);
            data.addProperty("dirZ", normalized.z);
            data.addProperty("power", 1.0f);
            multiplayerClient.send(NetworkMessage.of("clientAction", data));
        } catch (Exception e) {
            System.err.println("Could not sync multiplayer dropped item action: " + e.getMessage());
        }
    }

    private void sendMultiplayerDropStackAction(ItemStack dropped, org.joml.Vector3f direction,
            float throwSpeed, float yVelocity) {
        if (dropped == null || dropped.isEmpty() || multiplayerClient == null || !multiplayerClient.isConnected()) {
            return;
        }
        org.joml.Vector3f normalized = new org.joml.Vector3f(direction == null ? new org.joml.Vector3f(0, 0, 1) : direction);
        if (normalized.lengthSquared() <= 0.0001f) {
            normalized.set(0, 0, 1);
        } else {
            normalized.normalize();
        }
        try {
            com.google.gson.JsonObject data = NetworkMessage.object();
            data.addProperty("action", MultiplayerProtocol.ACTION_ITEM_USE);
            data.addProperty("useAction", "drop_stack");
            data.addProperty("dirX", normalized.x);
            data.addProperty("dirY", normalized.y);
            data.addProperty("dirZ", normalized.z);
            data.addProperty("power", Math.max(0.0f, Math.min(throwSpeed, 8.0f)));
            data.addProperty("velocityY", Math.max(-4.0f, Math.min(yVelocity, 8.0f)));
            HashMap<String, String> stackData = new HashMap<>();
            putItemStackData(stackData, "stack", dropped);
            for (Map.Entry<String, String> entry : stackData.entrySet()) {
                data.addProperty(entry.getKey(), entry.getValue());
            }
            multiplayerClient.send(NetworkMessage.of("clientAction", data));
        } catch (Exception e) {
            System.err.println("Could not sync multiplayer dropped stack action: " + e.getMessage());
        }
    }

    private void sendMultiplayerDeathDropStackAction(int sourceSlot, ItemStack dropped,
            float velocityX, float velocityY, float velocityZ, int pickupDelayTicks) {
        if (sourceSlot < 0 || sourceSlot >= MULTIPLAYER_INVENTORY_SLOTS
                || dropped == null || dropped.isEmpty()
                || multiplayerClient == null || !multiplayerClient.isConnected()) {
            return;
        }
        try {
            com.google.gson.JsonObject data = NetworkMessage.object();
            data.addProperty("action", MultiplayerProtocol.ACTION_ITEM_USE);
            data.addProperty("useAction", "death_drop_stack");
            data.addProperty("sourceSlot", sourceSlot);
            data.addProperty("motionX", Math.max(-8.0f, Math.min(velocityX, 8.0f)));
            data.addProperty("motionY", Math.max(-8.0f, Math.min(velocityY, 8.0f)));
            data.addProperty("motionZ", Math.max(-8.0f, Math.min(velocityZ, 8.0f)));
            data.addProperty("pickupDelay", Math.max(0, Math.min(pickupDelayTicks, 200)));
            HashMap<String, String> stackData = new HashMap<>();
            putItemStackData(stackData, "stack", dropped);
            for (Map.Entry<String, String> entry : stackData.entrySet()) {
                data.addProperty(entry.getKey(), entry.getValue());
            }
            multiplayerClient.send(NetworkMessage.of("clientAction", data));
        } catch (Exception e) {
            System.err.println("Could not sync multiplayer death dropped stack: " + e.getMessage());
        }
    }

    private void sendMultiplayerDeathExperienceDropAction(int amount) {
        if (amount <= 0 || multiplayerClient == null || !multiplayerClient.isConnected()) {
            return;
        }
        try {
            com.google.gson.JsonObject data = NetworkMessage.object();
            data.addProperty("action", MultiplayerProtocol.ACTION_ITEM_USE);
            data.addProperty("useAction", "death_drop_xp");
            data.addProperty("amount", Math.max(0, Math.min(amount, 100_000)));
            data.addProperty("power", 0.0f);
            multiplayerClient.send(NetworkMessage.of("clientAction", data));
        } catch (Exception e) {
            System.err.println("Could not sync multiplayer death experience drop: " + e.getMessage());
        }
    }

    private void syncMultiplayerDeathDropSourceState() {
        if (multiplayerDeathDropStateSynced) {
            return;
        }
        sendMultiplayerPlayerStateNow();
        multiplayerDeathDropStateSynced = true;
    }

    private boolean shouldSendClientMultiplayerDrops() {
        return clientMultiplayerWorld && multiplayerClient != null && multiplayerClient.isConnected();
    }

    private void updateCraftingTableScreen() {
        if (!craftingTableScreen.isOpen()) {
            return;
        }
        boolean usable = craftingTableScreen.isStillUsable(player);
        if (usable) {
            craftingTableScreen.update();
        } else {
            craftingTableScreen.close();
        }
        throwScreenItems(craftingTableScreen.getAndClearItemsToThrow(), 4.0f, 2.0f);
        syncClientMultiplayerCraftActions(craftingTableScreen.drainCraftActions());
        if (usable && craftingTableScreen.shouldOpenInventoryAfterClose()) {
            inventoryScreen.open(window.getWidth(), window.getHeight());
            recordInventoryOpenedAchievement();
        }
    }

    private void updateChestScreen() {
        if (!chestScreen.isOpen()) {
            return;
        }
        if (chestScreen.isStillUsable(player)) {
            chestScreen.update();
        } else {
            chestScreen.close();
        }
        throwScreenItems(chestScreen.getAndClearItemsToThrow(), 4.0f, 2.0f);
        if (chestScreen.isOpen()) {
            syncClientMultiplayerOpenContainer(chestScreen.getFirstChest());
            syncClientMultiplayerOpenContainer(chestScreen.getSecondChest());
            syncClientMultiplayerOpenChestMinecart(chestScreen);
        }
    }

    private void updateFurnaceScreen() {
        if (!furnaceScreen.isOpen()) {
            return;
        }
        if (furnaceScreen.isStillUsable(world, player)) {
            furnaceScreen.update();
        } else {
            furnaceScreen.close();
        }
        throwScreenItems(furnaceScreen.getAndClearItemsToThrow(), 4.0f, 2.0f);
        if (furnaceScreen.isOpen()) {
            syncClientMultiplayerOpenContainer(furnaceScreen.getFurnace());
        }
    }

    private void updateDispenserScreen() {
        if (!dispenserScreen.isOpen()) {
            return;
        }
        if (dispenserScreen.isStillUsable(world, player)) {
            dispenserScreen.update();
        } else {
            dispenserScreen.close();
        }
        throwScreenItems(dispenserScreen.getAndClearItemsToThrow(), 4.0f, 2.0f);
        if (dispenserScreen.isOpen()) {
            syncClientMultiplayerOpenContainer(dispenserScreen.getDispenser());
        }
    }

    private void updateBrewingStandScreen() {
        if (!brewingStandScreen.isOpen()) {
            return;
        }
        if (brewingStandScreen.isStillUsable(world, player)) {
            brewingStandScreen.update();
        } else {
            brewingStandScreen.close();
        }
        throwScreenItems(brewingStandScreen.getAndClearItemsToThrow(), 4.0f, 2.0f);
        if (brewingStandScreen.isOpen()) {
            syncClientMultiplayerOpenContainer(brewingStandScreen.getBrewingStand());
        }
    }

    private void updateEnchantingTableScreen() {
        if (!enchantingTableScreen.isOpen()) {
            return;
        }
        if (enchantingTableScreen.isStillUsable(player)) {
            enchantingTableScreen.update();
        } else {
            enchantingTableScreen.close();
        }
        throwScreenItems(enchantingTableScreen.getAndClearItemsToThrow(), 4.0f, 2.0f);
        if (enchantingTableScreen.isOpen()) {
            syncClientMultiplayerEnchantingTable(enchantingTableScreen);
        }
    }

    private void throwInventoryScreenItems(List<com.craftzero.inventory.ItemStack> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        boolean sentMultiplayerDrop = false;
        for (com.craftzero.inventory.ItemStack thrown : items) {
            if (thrown == null || thrown.isEmpty()) {
                continue;
            }
            org.joml.Vector3f forward = player.getCamera().getForward();
            org.joml.Vector3f playerVel = player.getVelocity();
            if (shouldSendClientMultiplayerDrops()) {
                sendMultiplayerDropStackAction(thrown, forward, 6.0f, 3.0f);
                sentMultiplayerDrop = true;
            } else {
                world.spawnThrownStack(player.getPosition().x + forward.x * 0.5f,
                        player.getPosition().y + 1.5f,
                        player.getPosition().z + forward.z * 0.5f,
                        thrown,
                        forward.x * 6.0f + playerVel.x,
                        3.0f,
                        forward.z * 6.0f + playerVel.z,
                        DroppedItem.THROWN_PICKUP_DELAY_TICKS);
            }
        }
        if (sentMultiplayerDrop) {
            syncMultiplayerInventoryStateNow();
        }
    }

    private void handlePlayerOpenRequests() {
        org.joml.Vector3i craftingTablePos = player.getAndClearCraftingTableOpenRequest();
        if (craftingTablePos != null
                && world.getBlockIfLoaded(craftingTablePos.x, craftingTablePos.y, craftingTablePos.z,
                        BlockType.AIR) == BlockType.CRAFTING_TABLE) {
            craftingTableScreen.open(world, craftingTablePos, window.getWidth(), window.getHeight());
        }
        org.joml.Vector3i chestPos = player.getAndClearChestOpenRequest();
        if (chestPos != null
                && world.getTileEntity(chestPos.x, chestPos.y, chestPos.z) instanceof ChestTileEntity chest
                && world.canOpenChest(chest)) {
            chestScreen.open(world, chest, window.getWidth(), window.getHeight());
        }
        ChestMinecartEntity chestMinecart = player.getAndClearChestMinecartOpenRequest();
        if (chestMinecart != null && !chestMinecart.isRemoved()) {
            chestScreen.openMinecart(chestMinecart, window.getWidth(), window.getHeight());
        }
        org.joml.Vector3i furnacePos = player.getAndClearFurnaceOpenRequest();
        if (furnacePos != null
                && world.getTileEntity(furnacePos.x, furnacePos.y, furnacePos.z) instanceof FurnaceTileEntity furnace) {
            furnaceScreen.open(furnace, window.getWidth(), window.getHeight());
        }
        org.joml.Vector3i dispenserPos = player.getAndClearDispenserOpenRequest();
        if (dispenserPos != null
                && world.getTileEntity(dispenserPos.x, dispenserPos.y, dispenserPos.z) instanceof DispenserTileEntity dispenser) {
            dispenserScreen.open(dispenser, window.getWidth(), window.getHeight());
        }
        org.joml.Vector3i brewingPos = player.getAndClearBrewingStandOpenRequest();
        if (brewingPos != null
                && world.getTileEntity(brewingPos.x, brewingPos.y, brewingPos.z) instanceof BrewingStandTileEntity brewingStand) {
            brewingStandScreen.open(brewingStand, window.getWidth(), window.getHeight(),
                    player.getStats().getAchievements());
        }
        org.joml.Vector3i enchantingPos = player.getAndClearEnchantingTableOpenRequest();
        if (enchantingPos != null) {
            enchantingTableScreen.open(world, enchantingPos, player.getStats().getProgression(),
                    window.getWidth(), window.getHeight());
        }
        org.joml.Vector3i signPos = player.getAndClearSignEditRequest();
        if (signPos != null
                && world.getTileEntity(signPos.x, signPos.y, signPos.z) instanceof SignTileEntity sign) {
            signEditScreen.open(sign);
        }
        org.joml.Vector3i bedPos = player.getAndClearBedUseRequest();
        if (bedPos != null) {
            World.BedUseResult bedUse = world.useBed(bedPos.x, bedPos.y, bedPos.z);
            if (bedUse.sleepAllowed()) {
                if (beginAcceptedBedSleep(player, world, bedUse)) {
                    activeBedSleep = bedUse;
                    activeBedSleepTimer = 0.0f;
                    multiplayerSleepCompletePending = false;
                    notifyMultiplayerSleepStarted();
                }
            } else {
                switch (bedUse.outcome()) {
                    case NOT_NIGHT -> addChatMessage("You can only sleep at night");
                    case OCCUPIED -> addChatMessage("This bed is occupied");
                    case MONSTERS_NEARBY -> addChatMessage("You may not rest now, there are monsters nearby");
                    default -> {
                    }
                }
            }
        }
    }

    static boolean beginAcceptedBedSleep(Player player, World world, World.BedUseResult bedUse) {
        if (player == null || world == null || bedUse == null || !bedUse.sleepAllowed()
                || bedUse.footPos() == null || bedUse.headPos() == null) {
            return false;
        }
        player.startSleepingInBed(bedUse.footPos(), bedUse.headPos());
        applyBedSpawnIfSafe(player, world, bedUse);
        return true;
    }

    static boolean finishAcceptedBedSleep(Player player, World world, World.BedUseResult bedUse) {
        if (player == null || world == null || bedUse == null || !bedUse.sleepAllowed()
                || bedUse.footPos() == null) {
            if (player != null) {
                player.wakeFromBed(null);
            }
            return false;
        }
        BlockPos foot = bedUse.footPos();
        BlockPos wakePos = world.findBedRespawnPosition(foot.x(), foot.y(), foot.z());
        try {
            return world.completeBedSleep(bedUse);
        } finally {
            world.setBedOccupied(foot.x(), foot.y(), foot.z(), false);
            player.wakeFromBed(wakePos);
        }
    }

    static boolean cancelAcceptedBedSleep(Player player, World world, World.BedUseResult bedUse) {
        if (player == null || !player.isSleeping()) {
            return false;
        }
        BlockPos foot = bedUse != null && bedUse.footPos() != null
                ? bedUse.footPos()
                : player.getSleepingBedFootPos();
        BlockPos wakePos = null;
        if (world != null && foot != null) {
            wakePos = world.findBedRespawnPosition(foot.x(), foot.y(), foot.z());
            world.setBedOccupied(foot.x(), foot.y(), foot.z(), false);
        }
        player.wakeFromBed(wakePos);
        return true;
    }

    static boolean applyBedSpawnIfSafe(Player player, World world, World.BedUseResult bedUse) {
        if (player == null || world == null || bedUse == null || !bedUse.sleepAllowed() || bedUse.footPos() == null) {
            return false;
        }
        BlockPos foot = bedUse.footPos();
        BlockPos respawn = world.findBedRespawnPosition(foot.x(), foot.y(), foot.z());
        if (respawn == null) {
            return false;
        }
        player.setBedSpawnPosition(foot, respawn.x() + 0.5f, respawn.y(), respawn.z() + 0.5f);
        return true;
    }

    enum RespawnTarget {
        SAVED_SPAWN,
        BED,
        WORLD_SPAWN
    }

    private record MultiplayerRespawnResult(float x, float y, float z, RespawnTarget target,
            BlockPos bedFoot, boolean bedMissing) {
    }

    static RespawnTarget preparePlayerRespawn(Player player, World world, int worldSpawnX, int worldSpawnY,
            int worldSpawnZ) {
        if (player == null || !player.hasBedSpawn()) {
            return RespawnTarget.SAVED_SPAWN;
        }
        BlockPos bedFoot = player.getBedSpawnPos();
        BlockPos respawn = world != null && bedFoot != null
                ? world.findBedRespawnPosition(bedFoot.x(), bedFoot.y(), bedFoot.z())
                : null;
        if (respawn != null) {
            player.setBedSpawnPosition(bedFoot, respawn.x() + 0.5f, respawn.y(), respawn.z() + 0.5f);
            return RespawnTarget.BED;
        }
        player.clearBedSpawn();
        player.setSpawnPosition(worldSpawnX + 0.5f, worldSpawnY, worldSpawnZ + 0.5f);
        return RespawnTarget.WORLD_SPAWN;
    }

    private void throwScreenItems(List<com.craftzero.inventory.ItemStack> items, float throwSpeed, float yVelocity) {
        if (items == null || items.isEmpty()) {
            return;
        }
        boolean sentMultiplayerDrop = false;
        for (com.craftzero.inventory.ItemStack dropped : items) {
            if (dropped != null && !dropped.isEmpty()) {
                org.joml.Vector3f forward = player.getCamera().getForward();
                if (shouldSendClientMultiplayerDrops()) {
                    sendMultiplayerDropStackAction(dropped, forward, throwSpeed, yVelocity);
                    sentMultiplayerDrop = true;
                } else {
                    world.spawnThrownStack(
                            player.getPosition().x + forward.x * 0.5f,
                            player.getPosition().y + 1.5f,
                            player.getPosition().z + forward.z * 0.5f,
                            dropped,
                            forward.x * throwSpeed,
                            yVelocity,
                            forward.z * throwSpeed,
                            DroppedItem.THROWN_PICKUP_DELAY_TICKS);
                }
            }
        }
        if (sentMultiplayerDrop) {
            syncMultiplayerInventoryStateNow();
        }
    }

    private void update(float deltaTime) {
        if (world == null || player == null) {
            return;
        }
        drainPendingNetworkMessages();
        if (gameState == GameState.LOADING_WORLD) {
            updateTerrainLoading(deltaTime);
            drainDeferredNetworkBlockUpdates();
            return;
        }
        Screen screen = screenManager.currentScreen();
        dispatchWorldSoundEvents();
        if (paused || gameState != GameState.PLAYING || !window.isFocused()
                || (screen != null && screen.pausesGame())) {
            return;
        }

        tickAmbientMusic(deltaTime);
        configurePlayerActionHandler();
        configurePlayerDeathDropHandler();
        player.update(deltaTime, world);
        if (player.isDead()) {
            sendMultiplayerPlayerStateNow();
            syncMultiplayerInventoryStateNow();
            openDeathMenuIfNeeded();
            return;
        }
        boolean sleepingThisTick = player.isSleeping();
        updateActiveBedSleep(deltaTime);
        if (!sleepingThisTick && !player.isSleeping()) {
            if (dimensionTransferCooldown > 0.0f) {
                dimensionTransferCooldown -= deltaTime;
                netherPortalTime = 0.0f;
            } else {
                handleDimensionTransfers(deltaTime);
            }

            handleDroppedHotbarItem();
        } else {
            netherPortalTime = 0.0f;
        }

        updateFog();

        world.update(player.getCamera());
        if (!clientMultiplayerWorld) {
            world.tickBlockUpdates(deltaTime);
        }
        dayCycleManager.update(deltaTime);
        if (!clientMultiplayerWorld) {
            world.updateWeather(deltaTime);
            weatherState = world.getWeatherState();
        }
        world.tickTileEntities(deltaTime);
        syncMultiplayerTileEntityState(deltaTime);
        if (!clientMultiplayerWorld) {
            world.updateDroppedItems(deltaTime);
        }
        world.updateAmbientBlockEffects(deltaTime);
        world.updateParticles(deltaTime);
        if (!clientMultiplayerWorld) {
            world.updateEntities(deltaTime);
            player.syncRidingPosition();
            configureMobSpawnerRules();
            mobSpawner.tick();
            updateMultiplayerRemotePickups(deltaTime);
            updateMultiplayerRemoteDamage(deltaTime);
        }
        broadcastMultiplayerParticleEvents(world.drainParticleEvents());
        broadcastMultiplayerLightningEvents(world.drainLightningEvents());

        sendMultiplayerPlayerState(deltaTime);
        syncMultiplayerEntityState(deltaTime);
        syncMultiplayerInventoryState();

        if (multiplayerServer != null) {
            configureMultiplayerWorldMetadata();
            multiplayerServer.broadcastWorldState(dayCycleManager.getTime(), weatherState);
        }
        dispatchWorldSoundEvents();

        autosaveTimer += deltaTime;
        if (savingEnabled && autosaveTimer >= AUTOSAVE_INTERVAL) {
            autosaveTimer = 0.0f;
            saveGameAsync("autosave");
        }
    }

    private void updateActiveBedSleep(float deltaTime) {
        if (activeBedSleep == null) {
            return;
        }
        activeBedSleepTimer += deltaTime;
        if (activeBedSleepTimer < BED_SLEEP_TRANSITION_SECONDS) {
            return;
        }
        if (!isMultiplayerSleepReadyToComplete()) {
            return;
        }
        World.BedUseResult sleep = activeBedSleep;
        activeBedSleep = null;
        activeBedSleepTimer = 0.0f;
        multiplayerSleepCompletePending = false;
        if (finishAcceptedBedSleep(player, world, sleep)) {
            weatherState = world.getWeatherState();
        }
        notifyMultiplayerSleepStopped();
    }

    private boolean isMultiplayerSleepReadyToComplete() {
        if (multiplayerServer != null) {
            if (multiplayerServer.consumePendingSleepCompletion()) {
                return true;
            }
            if (multiplayerServer.beginHostSleep()) {
                multiplayerServer.consumePendingSleepCompletion();
                return true;
            }
            return false;
        }
        if (clientMultiplayerWorld && multiplayerClient != null && multiplayerClient.isConnected()) {
            return multiplayerSleepCompletePending;
        }
        return true;
    }

    private void leaveActiveBedSleep() {
        World.BedUseResult sleep = activeBedSleep;
        activeBedSleep = null;
        activeBedSleepTimer = 0.0f;
        multiplayerSleepCompletePending = false;
        if (cancelAcceptedBedSleep(player, world, sleep)) {
            notifyMultiplayerSleepStopped();
        }
    }

    private void notifyMultiplayerSleepStarted() {
        if (clientMultiplayerWorld && multiplayerClient != null && multiplayerClient.isConnected()) {
            try {
                multiplayerClient.sendBedSleepStart();
            } catch (Exception e) {
                addChatMessage("Could not sync bed sleep: " + e.getMessage());
            }
        }
    }

    private void notifyMultiplayerSleepStopped() {
        if (multiplayerServer != null) {
            multiplayerServer.stopHostSleep();
        }
        if (clientMultiplayerWorld && multiplayerClient != null && multiplayerClient.isConnected()) {
            try {
                multiplayerClient.sendBedSleepStop();
            } catch (Exception e) {
                addChatMessage("Could not sync bed wake: " + e.getMessage());
            }
        }
    }

    private void dispatchWorldSoundEvents() {
        if (world == null) {
            return;
        }
        List<WorldSoundEvent> events = world.drainSoundEvents();
        if (events.isEmpty()) {
            return;
        }
        broadcastMultiplayerSoundEvents(events);
        if (soundDispatcher != null && settings != null) {
            if (player != null) {
                player.getCamera().updateViewMatrix();
                org.joml.Vector3f position = player.getCamera().getPosition();
                org.joml.Vector3f forward = player.getCamera().getForward();
                org.joml.Vector3f up = player.getCamera().getUp();
                soundDispatcher.dispatchEvents(events, settings.getSoundVolume(),
                        position.x, position.y, position.z,
                        forward.x, forward.y, forward.z,
                        up.x, up.y, up.z);
            } else {
                soundDispatcher.dispatchEvents(events, settings.getSoundVolume());
            }
        }
    }

    private void broadcastMultiplayerSoundEvents(List<WorldSoundEvent> events) {
        if (multiplayerServer == null || events == null || events.isEmpty()) {
            return;
        }
        for (WorldSoundEvent event : events) {
            if (event == null || !event.isPlayable()) {
                continue;
            }
            multiplayerServer.broadcastWorldSound(event);
        }
    }

    private void broadcastMultiplayerParticleEvents(List<WorldParticle> particles) {
        if (multiplayerServer == null || particles == null || particles.isEmpty()) {
            return;
        }
        int sent = 0;
        for (WorldParticle particle : particles) {
            if (!shouldBroadcastMultiplayerParticle(particle)) {
                continue;
            }
            multiplayerServer.broadcastWorldParticle(particle);
            sent++;
            if (sent >= MAX_MULTIPLAYER_PARTICLE_EVENTS_PER_TICK) {
                return;
            }
        }
    }

    private boolean shouldBroadcastMultiplayerParticle(WorldParticle particle) {
        if (particle == null || particle.getType() == null) {
            return false;
        }
        return particle.getType() != WorldParticle.Type.RAIN
                && particle.getType() != WorldParticle.Type.SNOW;
    }

    private void broadcastMultiplayerLightningEvents(List<WorldLightningBolt> bolts) {
        if (multiplayerServer == null || bolts == null || bolts.isEmpty()) {
            return;
        }
        for (WorldLightningBolt bolt : bolts) {
            multiplayerServer.broadcastWorldLightning(bolt);
        }
    }

    private void playMenuClickSound() {
        if (soundDispatcher != null && settings != null) {
            soundDispatcher.play(WorldSoundEvent.uiButtonClick(), settings.getSoundVolume());
        }
    }

    private void tickAmbientMusic(float deltaTime) {
        if (ambientMusicScheduler == null || settings == null || player == null) {
            return;
        }
        player.getCamera().updateViewMatrix();
        org.joml.Vector3f position = player.getCamera().getPosition();
        ambientMusicScheduler.tick(deltaTime, settings.getMusicVolume(),
                position.x, position.y, position.z);
    }

    private void initSoundDispatcher() {
        try {
            soundSink = OpenAlSoundSink.create();
            soundDispatcher = new WorldSoundDispatcher(soundSink);
            ambientMusicScheduler = new AmbientMusicScheduler(soundSink);
        } catch (RuntimeException ex) {
            soundSink = null;
            soundDispatcher = WorldSoundDispatcher.noop();
            ambientMusicScheduler = new AmbientMusicScheduler((event, effectiveVolume) -> {
            });
            System.err.println("Audio playback disabled: " + ex.getMessage());
        }
    }

    private void updateTerrainLoading(float deltaTime) {
        terrainLoadingTime += deltaTime;
        player.setInterpolatedCameraPosition(1.0f);
        world.update(player.getCamera());
        org.joml.Vector3f position = player.getPosition();
        terrainLoadProgress = world.getChunkAreaProgress(position.x, position.z, INITIAL_LOAD_READY_RADIUS);
        if (terrainLoadProgress.isReady()) {
            finishTerrainLoading();
        }
    }

    private void beginTerrainLoading(boolean hostAfterLoad) {
        hostAfterTerrainLoad = hostAfterLoad;
        terrainLoadingTime = 0.0f;
        terrainLoadProgress = new World.ChunkAreaProgress(1, 0, 0, 0);
        gameState = GameState.LOADING_WORLD;
        paused = false;
        Input.setCursorLocked(false);
        if (player != null) {
            player.getCamera().setAspectRatio(window.getWidth(), window.getHeight());
            player.setInterpolatedCameraPosition(1.0f);
        }
    }

    private void finishTerrainLoading() {
        gameState = GameState.PLAYING;
        Input.setCursorLocked(true);
        player.getCamera().setAspectRatio(window.getWidth(), window.getHeight());
        player.prepareForWorldJoin();
        if (ambientMusicScheduler != null) {
            ambientMusicScheduler.reset();
        }
        if (hostAfterTerrainLoad) {
            hostAfterTerrainLoad = false;
            startMultiplayerHost();
        }
    }

    private void updateFog() {
        if (player.isHeadInWater()) {
            int depth = 0;
            org.joml.Vector3f pos = player.getPosition();
            for (int i = 1; i <= 20; i++) {
                if (world.getBlockIfLoaded((int) pos.x, (int) pos.y + 1 + i, (int) pos.z, BlockType.AIR).isWater()) {
                    depth++;
                } else {
                    break;
                }
            }
            float t = Math.min(depth, 10) / 10.0f;
            float density = 0.15f + t * 0.20f;
            renderer.setFogDensity(density);
            float r = 0.1f * (1 - t) + 0.02f * t;
            float g = 0.4f * (1 - t) + 0.1f * t;
            float b = 0.8f * (1 - t) + 0.4f * t;
            renderer.setFogColor(new org.joml.Vector3f(r, g, b));
            renderer.setClearColor(r, g, b, 1.0f);
        } else {
            applyNormalDistanceFog();
            renderer.setFogColor(new org.joml.Vector3f(0.6f, 0.6f, 0.6f));
            renderer.setClearColor(0.529f, 0.808f, 0.922f, 1.0f);
        }
    }

    private CameraFluid cameraFluid() {
        if (world == null || player == null) {
            return CameraFluid.NONE;
        }

        org.joml.Vector3f pos = player.getCamera().getPosition();
        BlockType block = world.getBlockIfLoaded(
                floorBlock(pos.x),
                floorBlock(pos.y),
                floorBlock(pos.z),
                BlockType.AIR);
        if (block.isWater()) {
            return CameraFluid.WATER;
        }
        if (block.isLava()) {
            return CameraFluid.LAVA;
        }
        return CameraFluid.NONE;
    }

    private float cameraWaterDepthFactor() {
        if (world == null || player == null) {
            return 0.0f;
        }

        org.joml.Vector3f pos = player.getCamera().getPosition();
        int x = floorBlock(pos.x);
        int y = floorBlock(pos.y);
        int z = floorBlock(pos.z);
        int depth = 0;
        for (int i = 0; i <= 20; i++) {
            if (world.getBlockIfLoaded(x, y + i, z, BlockType.AIR).isWater()) {
                depth++;
            } else {
                break;
            }
        }
        return Math.min(depth, 10) / 10.0f;
    }

    static float[] normalFogRangeForRenderDistance(int renderDistanceChunks) {
        float end = Math.max(32.0f, renderDistanceChunks * (float) Chunk.WIDTH);
        return new float[] { end * 0.70f, end };
    }

    static float normalClipFarForRenderDistance(int renderDistanceChunks) {
        return Math.max(32.0f, renderDistanceChunks * (float) Chunk.WIDTH);
    }

    private void applyNormalDistanceFog() {
        int chunks = effectiveWorldRenderDistanceChunks();
        float[] range = normalFogRangeForRenderDistance(chunks);
        if (player != null && player.getCamera() != null) {
            player.getCamera().setFarPlane(normalClipFarForRenderDistance(chunks));
        }
        renderer.setFogRange(range[0], range[1]);
    }

    private static int floorBlock(float value) {
        return (int) Math.floor(value);
    }

    private void handleDroppedHotbarItem() {
        if (!inventoryScreen.isOpen() && player.wantsToDropItem()) {
            com.craftzero.inventory.ItemStack dropped = player.dropOneFromHand();
            if (dropped != null) {
                org.joml.Vector3f forward = player.getCamera().getForward();
                org.joml.Vector3f playerVel = player.getVelocity();
                if (clientMultiplayerWorld && multiplayerClient != null && multiplayerClient.isConnected()) {
                    sendMultiplayerDropItemAction(dropped, forward);
                    syncMultiplayerInventoryStateNow();
                } else {
                    world.spawnThrownStack(
                            player.getPosition().x + forward.x,
                            player.getPosition().y + 1.5f,
                            player.getPosition().z + forward.z,
                            dropped,
                            forward.x * 8.0f + playerVel.x,
                            3.0f,
                            forward.z * 8.0f + playerVel.z,
                            DroppedItem.THROWN_PICKUP_DELAY_TICKS);
                }
            }
            player.clearDropFlag();
        }
    }

    private void handleChatSubmit(String submitted) {
        String message = submitted == null ? "" : submitted.trim();
        if (message.isEmpty()) {
            return;
        }
        if (message.startsWith("/")) {
            if (clientMultiplayerWorld && multiplayerClient != null && multiplayerClient.isConnected()) {
                try {
                    multiplayerClient.sendChat(message);
                } catch (Exception e) {
                    addChatMessage("Could not send command: " + e.getMessage());
                }
                return;
            }
            commandDispatcher.execute(message, commandContext());
        } else {
            sendPlayerChat(message);
        }
    }

    private void sendPlayerChat(String text) {
        String sender = localPlayerName();
        if (multiplayerClient != null && multiplayerClient.isConnected()) {
            try {
                multiplayerClient.sendChat(text);
                return;
            } catch (Exception e) {
                addChatMessage("Could not send chat: " + e.getMessage());
            }
        }
        addChatMessage("<" + sender + "> " + text);
        if (multiplayerServer != null) {
            multiplayerServer.broadcastChat(sender, text);
        }
    }

    private String localPlayerName() {
        return settings == null ? GameSettings.DEFAULT_PLAYER_NAME : settings.getPlayerName();
    }

    private void broadcastSystemChat(String text) {
        addChatMessage(text);
        if (multiplayerServer != null) {
            multiplayerServer.broadcastChat("Server", text);
        }
    }

    private void addChatMessage(String message) {
        if (chatOverlay != null) {
            chatOverlay.addMessage(message);
        }
    }

    private void drainPendingChatMessages() {
        if (chatOverlay == null) {
            pendingChatMessages.clear();
            return;
        }
        String message;
        while ((message = pendingChatMessages.poll()) != null) {
            chatOverlay.addMessage(message);
        }
    }

    private void handleNetworkMessage(NetworkMessage message) {
        if (message == null || message.data() == null) {
            return;
        }
        if (!"chat".equals(message.type())) {
            pendingNetworkMessages.offer(message);
            return;
        }
        String sender = message.data().has("sender") ? message.data().get("sender").getAsString() : "Player";
        String text = message.data().has("text") ? message.data().get("text").getAsString() : "";
        if ("Server".equalsIgnoreCase(sender)) {
            pendingChatMessages.offer(text);
        } else {
            pendingChatMessages.offer("<" + sender + "> " + text);
        }
    }

    private void installWorldNetworkHooks(World targetWorld) {
        resetMultiplayerInventorySnapshot();
        if (targetWorld != null) {
            targetWorld.addBlockChangeListener(this::syncMultiplayerBlockChange);
            targetWorld.addDamageEventListener(new World.DamageEventListener() {
                @Override
                public void onExplosion(float x, float y, float z, float power) {
                    applyMultiplayerRemoteExplosionDamage(x, y, z, power);
                }

                @Override
                public void onLightning(float x, float y, float z) {
                    applyMultiplayerRemoteLightningDamage(x, y, z);
                }
            });
            targetWorld.setProjectilePlayerInteractionHandler(new World.ProjectilePlayerInteractionHandler() {
                @Override
                public World.ProjectilePlayerHit findProjectilePlayerHit(org.joml.Vector3f origin,
                        org.joml.Vector3f direction, float maxDistance) {
                    return findMultiplayerRemoteProjectilePlayerHit(origin, direction, maxDistance);
                }

                @Override
                public World.ProjectilePlayerHit findProjectilePlayerHit(org.joml.Vector3f origin,
                        org.joml.Vector3f direction, float maxDistance, String ignoredPlayerId) {
                    return findMultiplayerRemoteProjectilePlayerHit(origin, direction, maxDistance, ignoredPlayerId);
                }

                @Override
                public boolean damageProjectilePlayer(World.ProjectilePlayerHit hit,
                        World.ProjectilePlayerDamage damage) {
                    return damageMultiplayerRemoteProjectilePlayer(hit, damage);
                }

                @Override
                public void splashPotionPlayers(float x, float y, float z, PotionData potion,
                        String directHitPlayerId) {
                    splashMultiplayerRemotePotionPlayers(x, y, z, potion, directHitPlayerId);
                }
            });
            targetWorld.setRemotePlayerInteractionHandler(new World.RemotePlayerInteractionHandler() {
                @Override
                public World.RemotePlayerTarget targetById(String playerId) {
                    return multiplayerRemoteTargetById(playerId, 0.0f, 0.0f, 0.0f);
                }

                @Override
                public World.RemotePlayerTarget viewById(String playerId) {
                    return multiplayerRemotePlayerViewById(playerId, 0.0f, 0.0f, 0.0f);
                }

                @Override
                public World.RemotePlayerTarget nearestTarget(float sourceX, float sourceY, float sourceZ,
                        float range, boolean requireSight) {
                    return nearestMultiplayerRemoteTarget(sourceX, sourceY, sourceZ, range, requireSight);
                }

                @Override
                public List<World.RemotePlayerTarget> targets(float sourceX, float sourceY, float sourceZ,
                        float range, boolean requireSight) {
                    return multiplayerRemoteTargets(sourceX, sourceY, sourceZ, range, requireSight);
                }

                @Override
                public List<World.RemotePlayerTarget> views(float sourceX, float sourceY, float sourceZ,
                        float range, boolean requireSight) {
                    return multiplayerRemotePlayerViews(sourceX, sourceY, sourceZ, range, requireSight);
                }

                @Override
                public boolean damageTarget(String playerId, World.RemotePlayerDamage damage) {
                    return damageMultiplayerRemoteTarget(playerId, damage);
                }

                @Override
                public boolean applyStatusEffect(String playerId, StatusEffectInstance effect) {
                    return applyStatusEffectMultiplayerRemoteTarget(playerId, effect);
                }

                @Override
                public boolean pullTarget(String playerId, float motionX, float motionY, float motionZ) {
                    return pullMultiplayerRemoteTarget(playerId, motionX, motionY, motionZ);
                }
            });
        }
    }

    private void syncMultiplayerBlockChange(int x, int y, int z, BlockType previous, int previousMetadata,
            BlockType current, int currentMetadata) {
        if (applyingNetworkBlockUpdate || current == null) {
            return;
        }
        String blockId = Integer.toString(current.getId());
        if (!MultiplayerProtocol.isValidBlockUpdate(blockId, y, currentMetadata)) {
            return;
        }
        Map<String, String> tileData = multiplayerServer == null ? Map.of() : multiplayerBlockData(x, y, z, current);
        if (multiplayerServer != null) {
            multiplayerServer.broadcastBlockUpdate(x, y, z, current.getId(), currentMetadata, tileData);
        }
        if (clientMultiplayerWorld && multiplayerClient != null && multiplayerClient.isConnected()) {
            try {
                com.google.gson.JsonObject data = NetworkMessage.object();
                data.addProperty("x", x);
                data.addProperty("y", y);
                data.addProperty("z", z);
                data.addProperty("blockId", blockId);
                data.addProperty("metadata", currentMetadata);
                multiplayerClient.send(NetworkMessage.of("blockUpdate", data));
            } catch (Exception e) {
                System.err.println("Could not sync multiplayer block update: " + e.getMessage());
            }
        }
    }

    private void drainPendingNetworkMessages() {
        drainDeferredNetworkBlockUpdates();
        NetworkMessage message;
        while ((message = pendingNetworkMessages.poll()) != null) {
            if (message.data() == null) {
                continue;
            }
            switch (message.type()) {
                case "hello", "worldState" -> {
                    if (dayCycleManager != null && message.data().has("time")) {
                        dayCycleManager.setTime(message.data().get("time").getAsFloat());
                    }
                    if (world != null && message.data().has("weatherState")) {
                        weatherState = World.normalizeWeatherState(message.data().get("weatherState").getAsString());
                        world.setWeatherState(weatherState);
                    }
                    if ("worldState".equals(message.type())) {
                        applyNetworkWorldMetadata(message);
                    }
                }
                case "clientAction" -> applyNetworkClientAction(message);
                case "serverCommand" -> applyRemoteServerCommand(message);
                case "playerList" -> applyNetworkPlayerList(message);
                case "playerState" -> applyNetworkPlayerState(message);
                case "entityUpdate" -> applyNetworkEntityUpdate(message);
                case "blockUpdate" -> applyNetworkBlockUpdate(message);
                case "inventoryUpdate" -> applyNetworkInventoryUpdate(message);
                case "worldSound" -> applyNetworkWorldSound(message);
                case "worldParticle" -> applyNetworkWorldParticle(message);
                case "worldLightning" -> applyNetworkWorldLightning(message);
                case "disconnect" -> {
                    boolean localDisconnect = disconnectTargetsLocalMultiplayerClient(message);
                    removeRemotePlayer(message);
                    String reason = message.data().has("reason") ? message.data().get("reason").getAsString()
                            : "Disconnected from server.";
                    if (localDisconnect) {
                        handleLocalMultiplayerDisconnect(reason);
                        return;
                    }
                }
                default -> {
                }
            }
        }
        drainDeferredNetworkBlockUpdates();
    }

    private void handleLocalMultiplayerDisconnect(String reason) {
        String message = reason == null || reason.isBlank() ? "Disconnected from server." : reason;
        unloadWorld(false);
        gameState = GameState.TITLE;
        openMessageScreen("Disconnected", message);
    }

    private void applyNetworkClientAction(NetworkMessage message) {
        if (message.data() == null || !message.data().has("action")) {
            return;
        }
        String action = message.data().get("action").getAsString();
        if (!networkMessageTargetsCurrentDimension(message)) {
            return;
        }
        applyRemotePlayerActionAnimation(message, action);
        if (MultiplayerProtocol.ACTION_BED_SLEEP_COMPLETE.equals(action)) {
            multiplayerSleepCompletePending = true;
            if (dayCycleManager != null) {
                float time = message.data().has("time") ? message.data().get("time").getAsFloat() : 0.0f;
                dayCycleManager.setTime(time);
            }
            if (world != null) {
                world.setWeatherState("clear");
                weatherState = world.getWeatherState();
            }
            return;
        }
        if (MultiplayerProtocol.ACTION_SIGN_UPDATE.equals(action)) {
            applyClientSignUpdate(message);
            return;
        }
        if (MultiplayerProtocol.ACTION_ENCHANT_ITEM.equals(action)) {
            if (messageBoolean(message, "accepted", false)) {
                applyAcceptedMultiplayerEnchantResult(message);
            } else {
                applyClientEnchantItem(message);
            }
            return;
        }
        if (MultiplayerProtocol.ACTION_CRAFT_ITEM.equals(action)) {
            if (messageBoolean(message, "accepted", false)) {
                applyAcceptedMultiplayerCraftResult(message);
            } else {
                applyClientCraftItem(message);
            }
            return;
        }
        if (MultiplayerProtocol.ACTION_CONTAINER_UPDATE.equals(action)) {
            applyClientContainerUpdate(message);
            return;
        }
        if (MultiplayerProtocol.ACTION_PLAYER_ATTACK.equals(action)) {
            applyClientPlayerAttack(message);
            return;
        }
        if (MultiplayerProtocol.ACTION_PLAYER_RESPAWN.equals(action)) {
            if (messageBoolean(message, "accepted", false)) {
                applyAcceptedMultiplayerRespawn(message);
            } else {
                applyClientRespawnAction(message);
            }
            return;
        }
        if (isMultiplayerEntityAction(action)) {
            applyClientEntityAction(message);
            return;
        }
        if (MultiplayerProtocol.ACTION_ITEM_USE.equals(action)) {
            applyClientItemUseAction(message);
            return;
        }

        if (!actionTargetsLocalMultiplayerClient(message)) {
            return;
        }

        switch (action) {
            case MultiplayerProtocol.ACTION_COMMAND_PRIVATE_MESSAGE -> applyCommandPrivateMessage(message);
            case MultiplayerProtocol.ACTION_COMMAND_GIVE -> applyCommandGive(message);
            case MultiplayerProtocol.ACTION_COMMAND_TELEPORT -> applyCommandTeleport(message);
            case MultiplayerProtocol.ACTION_COMMAND_KILL -> applyCommandKill();
            case MultiplayerProtocol.ACTION_COMMAND_CLEAR -> applyCommandClear(message);
            case MultiplayerProtocol.ACTION_COMMAND_SPAWNPOINT -> applyCommandSpawnpoint(message);
            case MultiplayerProtocol.ACTION_COMMAND_GAMEMODE -> applyCommandGameMode(message);
            case MultiplayerProtocol.ACTION_COMMAND_EXPERIENCE -> applyCommandExperience(message);
            case MultiplayerProtocol.ACTION_COMMAND_DAMAGE -> applyCommandDamage(message);
            case MultiplayerProtocol.ACTION_COMMAND_VELOCITY -> applyCommandVelocity(message);
            case MultiplayerProtocol.ACTION_COMMAND_POTION_EFFECT -> applyCommandPotionEffect(message);
            default -> {
            }
        }
    }

    private boolean isMultiplayerEntityAction(String action) {
        return MultiplayerProtocol.ACTION_ENTITY_ATTACK.equals(action)
                || MultiplayerProtocol.ACTION_ENTITY_USE.equals(action);
    }

    private void applyRemotePlayerActionAnimation(NetworkMessage message, String action) {
        if (message == null || message.data() == null || action == null || action.isBlank()) {
            return;
        }
        String playerId = messageString(message, "playerId", "");
        RemotePlayerView view = remotePlayerViewForAction(playerId);
        if (view == null) {
            return;
        }
        if (MultiplayerProtocol.ACTION_ENTITY_ATTACK.equals(action)
                || MultiplayerProtocol.ACTION_PLAYER_ATTACK.equals(action)) {
            view.player().playRemoteSwingAnimation();
            return;
        }
        if (MultiplayerProtocol.ACTION_ENTITY_USE.equals(action)) {
            view.player().playRemoteUseAnimation();
            return;
        }
        if (MultiplayerProtocol.ACTION_ITEM_USE.equals(action)) {
            applyRemotePlayerItemActionAnimation(view.player(), messageString(message, "useAction", ""));
        }
    }

    private void applyRemotePlayerItemActionAnimation(Player remote, String useAction) {
        if (remote == null || useAction == null || useAction.isBlank()) {
            return;
        }
        switch (useAction) {
            case "bow", "throw_item", "ender_pearl", "eye_of_ender", "splash_potion",
                    "play_note_block", "fishing_cast", "fishing_reel" -> remote.playRemoteSwingAnimation();
            case "consume_food", "drink_milk", "drink_potion", "use_map", "equip_armor",
                    "tune_note_block", "insert_record", "eject_record", "place_boat", "place_painting",
                    "place_minecart", "drop_item", "drop_stack" -> remote.playRemoteUseAnimation();
            default -> {
            }
        }
    }

    private RemotePlayerView remotePlayerViewForAction(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return null;
        }
        String localPlayerId = localMultiplayerPlayerId();
        if (!localPlayerId.isBlank() && playerId.equals(localPlayerId)) {
            return null;
        }
        String username = playerId;
        MultiplayerRosterEntry rosterEntry = multiplayerRoster.get(playerId);
        if (rosterEntry != null && rosterEntry.username() != null && !rosterEntry.username().isBlank()) {
            username = rosterEntry.username();
        }
        com.google.gson.JsonObject serverState = null;
        if (multiplayerServer != null) {
            serverState = currentDimensionMultiplayerPlayerStateById(playerId);
            if (serverState == null) {
                return null;
            }
            username = jsonString(serverState, "username", username);
        }
        String displayName = username;
        RemotePlayerView view = remotePlayers.computeIfAbsent(playerId, key -> new RemotePlayerView(displayName));
        view.player().setPlayerName(username);
        return view;
    }

    private void applyClientEntityAction(NetworkMessage message) {
        if (multiplayerServer == null || world == null || message == null || message.data() == null) {
            return;
        }
        String playerId = messageString(message, "playerId", "");
        String entityId = messageString(message, "entityId", "");
        Entity target = multiplayerEntityById(entityId);
        com.google.gson.JsonObject actorState = liveMultiplayerPlayerStateById(playerId);
        if (target == null || actorState == null || !multiplayerEntityActionInReach(actorState, target)) {
            return;
        }
        String action = messageString(message, "action", "");
        ItemStack heldStack = multiplayerSelectedStack(playerId, actorState);
        ItemType heldItem = heldStack == null || heldStack.isEmpty() ? null : heldStack.getType();
        boolean changed = false;
        if (MultiplayerProtocol.ACTION_ENTITY_ATTACK.equals(action)) {
            changed = applyClientEntityAttack(playerId, actorState, target, heldStack);
        } else if (MultiplayerProtocol.ACTION_ENTITY_USE.equals(action)) {
            changed = applyClientEntityUse(playerId, entityId, actorState, target, heldItem);
        }
        if (changed) {
            broadcastMultiplayerEntityStateNow(target);
        }
    }

    private void applyClientPlayerAttack(NetworkMessage message) {
        if (multiplayerServer == null || message == null || message.data() == null) {
            return;
        }
        String actorPlayerId = messageString(message, "playerId", "");
        String targetPlayerId = messageString(message, "targetPlayerId", "");
        if (actorPlayerId.isBlank() || targetPlayerId.isBlank() || actorPlayerId.equals(targetPlayerId)) {
            return;
        }
        com.google.gson.JsonObject actorState = liveMultiplayerPlayerStateById(actorPlayerId);
        if (actorState == null) {
            return;
        }
        ItemStack heldStack = multiplayerSelectedStack(actorPlayerId, actorState);
        boolean sprintKnockback = hostedSprintKnockbackReady(actorPlayerId, actorState);
        Player.PlayerAttack attack = multiplayerPlayerAttackFromActor(actorState, heldStack, sprintKnockback);
        boolean applied;
        if ("host".equalsIgnoreCase(targetPlayerId)) {
            applied = applyClientPlayerAttackToHost(actorState, attack);
        } else {
            applied = applyClientPlayerAttackToRemote(actorState, targetPlayerId, attack);
        }
        if (applied && attack.sprintKnockback()) {
            markHostedSprintKnockbackUsed(actorPlayerId, actorState);
        }
        if (applied && heldStack != null && heldStack.isTool()) {
            damageMultiplayerSelectedDurable(actorPlayerId, actorState, 1);
        }
    }

    private boolean applyClientPlayerAttackToHost(com.google.gson.JsonObject actorState, Player.PlayerAttack attack) {
        if (!multiplayerPvp
                || player == null
                || player.isDead()
                || player.isCreative()
                || attack == null
                || attack.damage() <= 0.0f) {
            return false;
        }
        if (!multiplayerPlayerAttackInReach(actorState,
                player.getPosition().x, player.getPosition().y, player.getPosition().z)) {
            return false;
        }
        boolean changed = player.hurt(attack.damage(),
                DamageSource.point(DamageSource.Type.PLAYER_ATTACK,
                        attack.sourceX(), attack.sourceY(), attack.sourceZ(),
                        attack.horizontalKnockback(), attack.verticalKnockback()));
        if (changed && attack.fireTicks() > 0 && !player.isDead()) {
            player.setOnFire(attack.fireTicks());
        }
        if (changed) {
            sendMultiplayerPlayerStateNow();
            syncMultiplayerInventoryStateNow();
        }
        return changed;
    }

    private boolean applyClientPlayerAttackToRemote(com.google.gson.JsonObject actorState,
            String targetPlayerId, Player.PlayerAttack attack) {
        if (!multiplayerPvp
                || targetPlayerId == null
                || targetPlayerId.isBlank()
                || attack == null
                || attack.damage() <= 0.0f) {
            return false;
        }
        com.google.gson.JsonObject targetState = currentDimensionMultiplayerPlayerStateById(targetPlayerId);
        if (!isAttackableMultiplayerPlayerState(targetState)
                || !multiplayerPlayerAttackInReach(actorState, targetState)) {
            return false;
        }
        int clientId = parseProtocolClientId(targetPlayerId);
        if (clientId <= 0) {
            return false;
        }
        return sendMultiplayerRemoteDamage(clientId, attack.damage(), "player_attack",
                attack.sourceX(), attack.sourceY(), attack.sourceZ(),
                attack.horizontalKnockback(), attack.verticalKnockback(), attack.fireTicks());
    }

    private boolean multiplayerPlayerAttackInReach(com.google.gson.JsonObject actorState,
            com.google.gson.JsonObject targetState) {
        if (targetState == null) {
            return false;
        }
        return multiplayerPlayerAttackInReach(actorState,
                jsonFloat(targetState, "x", 0.0f),
                jsonFloat(targetState, "y", 80.0f),
                jsonFloat(targetState, "z", 0.0f));
    }

    private boolean multiplayerPlayerAttackInReach(com.google.gson.JsonObject actorState,
            float targetX, float targetY, float targetZ) {
        if (actorState == null) {
            return false;
        }
        float actorX = jsonFloat(actorState, "x", Float.NaN);
        float actorY = jsonFloat(actorState, "y", Float.NaN);
        float actorZ = jsonFloat(actorState, "z", Float.NaN);
        if (!Float.isFinite(actorX) || !Float.isFinite(actorY) || !Float.isFinite(actorZ)) {
            return false;
        }
        double dx = targetX - actorX;
        double dy = targetY + MultiplayerProtocol.PLAYER_EYE_HEIGHT
                - (actorY + MultiplayerProtocol.PLAYER_EYE_HEIGHT);
        double dz = targetZ - actorZ;
        double distanceSq = dx * dx + dy * dy + dz * dz;
        return distanceSq <= MultiplayerProtocol.MAX_CLIENT_ENTITY_ACTION_DISTANCE_SQ;
    }

    private Player.PlayerAttack multiplayerPlayerAttackFromActor(com.google.gson.JsonObject actorState,
            ItemStack heldStack, boolean sprintKnockback) {
        float damage = multiplayerPlayerAttackDamage(actorState, heldStack);
        float knockback = CombatRules.PLAYER_ATTACK_KNOCKBACK
                + EnchantmentResolver.getLevel(heldStack, EnchantmentType.KNOCKBACK) * 0.4f;
        if (sprintKnockback) {
            knockback += CombatRules.PLAYER_ATTACK_SPRINT_BONUS;
        }
        int fireTicks = EnchantmentResolver.getLevel(heldStack, EnchantmentType.FIRE_ASPECT) * 80;
        return new Player.PlayerAttack(damage,
                jsonFloat(actorState, "x", 0.0f),
                jsonFloat(actorState, "y", 80.0f) + (float) MultiplayerProtocol.PLAYER_EYE_HEIGHT,
                jsonFloat(actorState, "z", 0.0f),
                knockback,
                CombatRules.PLAYER_ATTACK_VERTICAL_KNOCKBACK,
                fireTicks,
                sprintKnockback);
    }

    private float multiplayerPlayerAttackDamage(com.google.gson.JsonObject actorState, ItemStack heldStack) {
        float damage = 1.0f;
        if (heldStack != null && !heldStack.isEmpty() && heldStack.isTool()) {
            damage = heldStack.getType().getToolType().getAttackDamage();
        }
        damage += EnchantmentResolver.attackDamageBonus(heldStack);
        damage += hostedClientAttackDamageBonus(actorState);
        return Math.max(0.0f, damage);
    }

    private void applyClientItemUseAction(NetworkMessage message) {
        if (multiplayerServer == null || world == null || message == null || message.data() == null) {
            return;
        }
        String playerId = messageString(message, "playerId", "");
        com.google.gson.JsonObject actorState = currentDimensionMultiplayerPlayerStateById(playerId);
        if (actorState == null) {
            return;
        }
        String useAction = messageString(message, "useAction", "");
        org.joml.Vector3f direction = multiplayerActionDirection(message, actorState);
        ItemStack heldStack = multiplayerSelectedStack(playerId, actorState);
        if (!itemUseActionCanReplayWithoutHeldStack(useAction)
                && (heldStack == null || heldStack.isEmpty() || heldStack.getType() == null)) {
            return;
        }
        boolean changed = switch (useAction) {
            case "bow" -> applyClientBowUse(playerId, actorState, heldStack, direction,
                    Math.max(0.0f, Math.min(1.0f, messageFloat(message, "power", 0.0f))));
            case "throw_item" -> applyClientThrownItemUse(playerId, actorState, heldStack, direction);
            case "ender_pearl" -> applyClientEnderPearlUse(playerId, actorState, heldStack, direction);
            case "eye_of_ender" -> applyClientEyeOfEnderUse(actorState, heldStack, direction);
            case "splash_potion" -> applyClientSplashPotionUse(playerId, actorState, heldStack, direction);
            case "consume_food" -> applyClientFoodUse(playerId, actorState, heldStack);
            case "drink_milk" -> applyClientMilkUse(playerId, actorState, heldStack);
            case "drink_potion" -> applyClientDrinkablePotionUse(playerId, actorState, heldStack);
            case "use_map" -> applyClientMapUse(playerId, actorState, heldStack);
            case "equip_armor" -> applyClientArmorEquip(playerId, actorState, heldStack);
            case "play_note_block" -> applyClientNoteBlockPlay(playerId, message);
            case "tune_note_block" -> applyClientNoteBlockTune(playerId, message);
            case "insert_record" -> applyClientJukeboxRecordInsert(playerId, message, heldStack);
            case "eject_record" -> applyClientJukeboxRecordEject(playerId, message);
            case "place_boat" -> applyClientBoatPlacement(playerId, actorState, heldStack, direction);
            case "place_painting" -> applyClientPaintingPlacement(playerId, message, heldStack);
            case "place_minecart" -> applyClientMinecartPlacement(playerId, message, heldStack);
            case "fishing_cast" -> applyClientFishingCast(playerId, actorState, heldStack, direction);
            case "fishing_reel" -> applyClientFishingReel(playerId, actorState, heldStack);
            case "drop_item" -> applyClientDropItemUse(actorState, heldStack, direction);
            case "drop_stack" -> applyClientDropStackUse(message, actorState, direction);
            case "death_drop_stack" -> applyClientDeathDropStackUse(message, actorState);
            case "death_drop_xp" -> applyClientDeathDropExperienceUse(message, actorState);
            default -> false;
        };
        if (changed) {
            applyClientItemUseCost(playerId, actorState, useAction, heldStack);
            dispatchWorldSoundEvents();
        }
    }

    private void applyClientItemUseCost(String playerId, com.google.gson.JsonObject actorState,
            String useAction, ItemStack heldStack) {
        if (isCreativeMultiplayerPlayerState(actorState) || useAction == null || useAction.isBlank()) {
            return;
        }
        ItemType heldType = heldStack == null || heldStack.isEmpty() ? null : heldStack.getType();
        switch (useAction) {
            case "bow" -> {
                consumeMultiplayerInventoryItem(playerId, ItemType.ARROW, 1);
                damageMultiplayerSelectedDurable(playerId, actorState, 1);
            }
            case "throw_item" -> {
                if (heldType == ItemType.EGG || heldType == ItemType.SNOWBALL) {
                    consumeMultiplayerSelectedItem(playerId, actorState, heldType, 1);
                }
            }
            case "ender_pearl", "eye_of_ender", "splash_potion", "insert_record", "place_boat",
                    "place_painting", "place_minecart" -> {
                if (heldType != null) {
                    consumeMultiplayerSelectedItem(playerId, actorState, heldType, 1);
                }
            }
            case "drop_item" -> {
                if (heldType != null) {
                    consumeMultiplayerSelectedItem(playerId, actorState, heldType, 1);
                }
            }
            case "drop_stack" -> {
            }
            case "death_drop_stack", "death_drop_xp" -> {
            }
            default -> {
            }
        }
    }

    private boolean itemUseActionCanReplayWithoutHeldStack(String useAction) {
        return "play_note_block".equals(useAction)
                || "tune_note_block".equals(useAction)
                || "eject_record".equals(useAction)
                || "drop_stack".equals(useAction)
                || "death_drop_stack".equals(useAction)
                || "death_drop_xp".equals(useAction);
    }

    private ItemStack multiplayerSelectedStack(String playerId, com.google.gson.JsonObject actorState) {
        int selectedSlot = Math.max(0, Math.min(Inventory.HOTBAR_SIZE - 1,
                jsonInt(actorState, "selectedSlot", 0)));
        NetworkMessage inventoryState = multiplayerServer == null ? null
                : multiplayerServer.inventoryState(playerId, selectedSlot);
        if (inventoryState != null && inventoryState.data() != null) {
            return inventoryStackFromNetworkMessage(
                    inventoryState,
                    messageString(inventoryState, "itemId", "air"),
                    messageInt(inventoryState, "count", 0),
                    messageInt(inventoryState, "damage", -1));
        }
        return networkItemStack(
                jsonString(actorState, "heldItemId", "air"),
                jsonInt(actorState, "heldItemCount", 0),
                jsonInt(actorState, "heldItemDamage", -1));
    }

    private boolean consumeMultiplayerSelectedItem(String playerId, com.google.gson.JsonObject actorState,
            ItemType expectedType, int amount) {
        if (isCreativeMultiplayerPlayerState(actorState)) {
            return false;
        }
        int selectedSlot = Math.max(0, Math.min(Inventory.HOTBAR_SIZE - 1,
                jsonInt(actorState, "selectedSlot", 0)));
        return consumeMultiplayerInventorySlot(playerId, selectedSlot, expectedType, amount);
    }

    private boolean consumeMultiplayerInventoryItem(String playerId, ItemType expectedType, int amount) {
        if (playerId == null || playerId.isBlank() || expectedType == null || amount <= 0) {
            return false;
        }
        Inventory inventory = multiplayerInventorySnapshotFor(playerId);
        ItemStack[] before = snapshotPlayerInventoryStacks(inventory);
        int remaining = amount;
        for (int slot = 0; slot < Inventory.HOTBAR_SIZE + Inventory.MAIN_SIZE && remaining > 0; slot++) {
            ItemStack stack = multiplayerInventorySlot(inventory, slot);
            if (stack == null || stack.isEmpty() || stack.getType() != expectedType) {
                continue;
            }
            int removed = Math.min(remaining, stack.getCount());
            stack.remove(removed);
            remaining -= removed;
            if (stack.isEmpty()) {
                applyLocalInventorySlot(inventory, slot, null);
            }
        }
        if (remaining == amount) {
            return false;
        }
        broadcastChangedMultiplayerInventorySlots(playerId, inventory, before);
        return true;
    }

    private boolean consumeMultiplayerInventorySlot(String playerId, int slot, ItemType expectedType, int amount) {
        if (playerId == null || playerId.isBlank() || expectedType == null || amount <= 0
                || slot < 0 || slot >= Inventory.HOTBAR_SIZE + Inventory.MAIN_SIZE) {
            return false;
        }
        Inventory inventory = multiplayerInventorySnapshotFor(playerId);
        ItemStack stack = multiplayerInventorySlot(inventory, slot);
        if (stack == null || stack.isEmpty() || stack.getType() != expectedType) {
            return false;
        }
        ItemStack[] before = snapshotPlayerInventoryStacks(inventory);
        stack.remove(amount);
        if (stack.isEmpty()) {
            applyLocalInventorySlot(inventory, slot, null);
        }
        broadcastChangedMultiplayerInventorySlots(playerId, inventory, before);
        return true;
    }

    private boolean damageMultiplayerSelectedDurable(String playerId, com.google.gson.JsonObject actorState,
            int amount) {
        if (isCreativeMultiplayerPlayerState(actorState) || playerId == null || playerId.isBlank() || amount <= 0) {
            return false;
        }
        int selectedSlot = Math.max(0, Math.min(Inventory.HOTBAR_SIZE - 1,
                jsonInt(actorState, "selectedSlot", 0)));
        Inventory inventory = multiplayerInventorySnapshotFor(playerId);
        ItemStack stack = multiplayerInventorySlot(inventory, selectedSlot);
        if (stack == null || stack.isEmpty() || !stack.isDamageable()) {
            return false;
        }
        ItemStack[] before = snapshotPlayerInventoryStacks(inventory);
        boolean changed = false;
        java.util.Random random = world == null ? new java.util.Random() : world.getRandom();
        for (int i = 0; i < amount; i++) {
            if (EnchantmentResolver.shouldPreventDurabilityLoss(stack, random)) {
                continue;
            }
            changed = true;
            if (stack.useDurability()) {
                applyLocalInventorySlot(inventory, selectedSlot, null);
                break;
            }
        }
        if (changed) {
            broadcastChangedMultiplayerInventorySlots(playerId, inventory, before);
        }
        return changed;
    }

    private boolean isCreativeMultiplayerPlayerState(com.google.gson.JsonObject state) {
        return parseNetworkGameMode(jsonString(state, "gameMode", "SURVIVAL")).isCreative();
    }

    private org.joml.Vector3f multiplayerActionDirection(NetworkMessage message, com.google.gson.JsonObject actorState) {
        org.joml.Vector3f direction = new org.joml.Vector3f(
                messageFloat(message, "dirX", 0.0f),
                messageFloat(message, "dirY", 0.0f),
                messageFloat(message, "dirZ", 0.0f));
        if (direction.lengthSquared() > 0.0001f) {
            return direction.normalize();
        }
        float yawRad = (float) Math.toRadians(jsonFloat(actorState, "yaw", 0.0f));
        float pitchRad = (float) Math.toRadians(jsonFloat(actorState, "pitch", 0.0f));
        direction.set(
                (float) (Math.sin(yawRad) * Math.cos(pitchRad)),
                (float) (-Math.sin(pitchRad)),
                (float) (-Math.cos(yawRad) * Math.cos(pitchRad)));
        if (direction.lengthSquared() <= 0.0001f) {
            direction.set(0.0f, 0.0f, -1.0f);
        }
        return direction.normalize();
    }

    private org.joml.Vector3f multiplayerActionSpawn(com.google.gson.JsonObject actorState,
            org.joml.Vector3f direction, float forwardOffset, float eyeYOffset) {
        return new org.joml.Vector3f(
                jsonFloat(actorState, "x", 0.0f),
                jsonFloat(actorState, "y", 80.0f) + (float) MultiplayerProtocol.PLAYER_EYE_HEIGHT + eyeYOffset,
                jsonFloat(actorState, "z", 0.0f))
                .add(new org.joml.Vector3f(direction).mul(forwardOffset));
    }

    private boolean applyClientBowUse(String playerId, com.google.gson.JsonObject actorState, ItemStack heldStack,
            org.joml.Vector3f direction, float power) {
        if (heldStack.getType() != ItemType.BOW || power < 0.1f) {
            return false;
        }
        org.joml.Vector3f spawn = multiplayerActionSpawn(actorState, direction, 0.6f, -0.1f);
        float speed = 3.0f * power;
        float damage = 2.0f + 4.0f * power;
        ArrowEntity arrow = world.spawnArrow(spawn.x, spawn.y, spawn.z,
                direction.x * speed, direction.y * speed, direction.z * speed,
                null, true, damage);
        arrow.setCritical(power >= 1.0f);
        arrow.setRemoteShooterPlayerId(playerId);
        world.playBowSound(spawn.x, spawn.y, spawn.z);
        return true;
    }

    private boolean applyClientThrownItemUse(String playerId, com.google.gson.JsonObject actorState,
            ItemStack heldStack,
            org.joml.Vector3f direction) {
        ItemType type = heldStack.getType();
        if (type != ItemType.EGG && type != ItemType.SNOWBALL) {
            return false;
        }
        org.joml.Vector3f spawn = multiplayerActionSpawn(actorState, direction, 0.35f, -0.1f);
        ThrownItemEntity projectile = world.spawnThrownItemProjectile(spawn.x, spawn.y, spawn.z,
                direction.x * 1.5f, direction.y * 1.5f, direction.z * 1.5f,
                type, null, true);
        projectile.setRemoteShooterPlayerId(playerId);
        projectile.setYaw(jsonFloat(actorState, "yaw", projectile.getYaw()));
        projectile.setPitch(jsonFloat(actorState, "pitch", projectile.getPitch()));
        world.playThrowSound(spawn.x, spawn.y, spawn.z);
        return true;
    }

    private boolean applyClientEnderPearlUse(String playerId, com.google.gson.JsonObject actorState, ItemStack heldStack,
            org.joml.Vector3f direction) {
        if (heldStack.getType() != ItemType.ENDER_PEARL) {
            return false;
        }
        org.joml.Vector3f spawn = multiplayerActionSpawn(actorState, direction, 0.35f, -0.1f);
        EnderPearlEntity pearl = world.spawnEnderPearl(spawn.x, spawn.y, spawn.z,
                direction.x * 1.5f, direction.y * 1.5f, direction.z * 1.5f, null);
        pearl.setRemoteOwnerPlayerId(playerId);
        int clientId = parseProtocolClientId(playerId);
        if (clientId > 0) {
            pearl.setImpactCallback(impactedPearl -> {
                sendMultiplayerClientAction(clientId,
                        MultiplayerProtocol.ACTION_COMMAND_TELEPORT,
                        Map.of(
                                "x", Float.toString(impactedPearl.getX()),
                                "y", Float.toString(impactedPearl.getY()),
                                "z", Float.toString(impactedPearl.getZ())));
                sendMultiplayerRemoteDamage(clientId, 5.0f, "fall",
                        impactedPearl.getX(), impactedPearl.getY(), impactedPearl.getZ(),
                        0.0f, 0.0f, 0);
            });
        }
        pearl.setYaw(jsonFloat(actorState, "yaw", pearl.getYaw()));
        pearl.setPitch(jsonFloat(actorState, "pitch", pearl.getPitch()));
        world.playThrowSound(spawn.x, spawn.y, spawn.z);
        return true;
    }

    private boolean applyClientEyeOfEnderUse(com.google.gson.JsonObject actorState, ItemStack heldStack,
            org.joml.Vector3f direction) {
        if (heldStack.getType() != ItemType.EYE_OF_ENDER) {
            return false;
        }
        StructureGenerator.StructureLocation target = world.locateStructure(StructureType.STRONGHOLD,
                (int) Math.floor(jsonFloat(actorState, "x", 0.0f)),
                (int) Math.floor(jsonFloat(actorState, "z", 0.0f)));
        if (target == null) {
            return false;
        }
        boolean drops = world.getRandom().nextFloat() >= 0.20f;
        float actorX = jsonFloat(actorState, "x", 0.0f);
        float actorY = jsonFloat(actorState, "y", 80.0f);
        float actorZ = jsonFloat(actorState, "z", 0.0f);
        EyeOfEnderEntity eye = new EyeOfEnderEntity(actorX, actorY + (float) MultiplayerProtocol.PLAYER_EYE_HEIGHT - 0.2f,
                actorZ, target.blockX() + 0.5f, target.blockY() + 1.0f, target.blockZ() + 0.5f, drops);
        eye.moveTowards(target.blockX() + 0.5f, target.blockY() + 1.0f, target.blockZ() + 0.5f);
        eye.setMotion(direction.x * 0.5f, direction.y * 0.5f + 0.15f, direction.z * 0.5f);
        world.spawnEntity(eye);
        world.playThrowSound(actorX, actorY + (float) MultiplayerProtocol.PLAYER_EYE_HEIGHT, actorZ);
        return true;
    }

    private boolean applyClientSplashPotionUse(String playerId, com.google.gson.JsonObject actorState,
            ItemStack heldStack,
            org.joml.Vector3f direction) {
        if (heldStack.getType() != ItemType.POTION) {
            return false;
        }
        PotionData potion = heldStack.getPotionData() == null ? PotionData.water() : heldStack.getPotionData();
        if (!potion.splash()) {
            return false;
        }
        org.joml.Vector3f spawn = multiplayerActionSpawn(actorState, direction, 0.35f, -0.1f);
        SplashPotionEntity potionEntity = world.spawnSplashPotion(spawn.x, spawn.y, spawn.z,
                direction.x * 0.5f, direction.y * 0.5f + 0.1f, direction.z * 0.5f,
                null, potion);
        potionEntity.setRemoteShooterPlayerId(playerId);
        world.playThrowSound(spawn.x, spawn.y, spawn.z);
        return true;
    }

    private boolean applyClientNoteBlockPlay(String playerId, NetworkMessage message) {
        BlockActionPos pos = blockActionPos(playerId, message);
        return pos != null
                && world.getBlockIfLoaded(pos.x(), pos.y(), pos.z(), BlockType.AIR) == BlockType.NOTE_BLOCK
                && world.playNoteBlock(pos.x(), pos.y(), pos.z());
    }

    private boolean applyClientNoteBlockTune(String playerId, NetworkMessage message) {
        BlockActionPos pos = blockActionPos(playerId, message);
        return pos != null
                && world.getBlockIfLoaded(pos.x(), pos.y(), pos.z(), BlockType.AIR) == BlockType.NOTE_BLOCK
                && world.toggleBlock(pos.x(), pos.y(), pos.z());
    }

    private boolean applyClientJukeboxRecordInsert(String playerId, NetworkMessage message, ItemStack heldStack) {
        if (heldStack == null || heldStack.isEmpty() || !heldStack.getType().isRecord()) {
            return false;
        }
        BlockActionPos pos = blockActionPos(playerId, message);
        if (pos == null
                || world.getBlockIfLoaded(pos.x(), pos.y(), pos.z(), BlockType.AIR) != BlockType.JUKEBOX
                || world.getBlockMetadataIfLoaded(pos.x(), pos.y(), pos.z(), -1) != 0) {
            return false;
        }
        TileEntity tile = world.getTileEntity(pos.x(), pos.y(), pos.z());
        if (!(tile instanceof JukeboxTileEntity jukebox) || jukebox.hasRecord()) {
            return false;
        }
        if (!jukebox.insertRecord(world, heldStack)) {
            return false;
        }
        jukebox.play(world);
        return true;
    }

    private boolean applyClientJukeboxRecordEject(String playerId, NetworkMessage message) {
        BlockActionPos pos = blockActionPos(playerId, message);
        return pos != null
                && world.getBlockIfLoaded(pos.x(), pos.y(), pos.z(), BlockType.AIR) == BlockType.JUKEBOX
                && world.getBlockMetadataIfLoaded(pos.x(), pos.y(), pos.z(), -1) == 1
                && world.toggleBlock(pos.x(), pos.y(), pos.z());
    }

    private BlockActionPos blockActionPos(String playerId, NetworkMessage message) {
        int x = messageInt(message, "blockX", Integer.MIN_VALUE);
        int y = messageInt(message, "blockY", Integer.MIN_VALUE);
        int z = messageInt(message, "blockZ", Integer.MIN_VALUE);
        if (y < 0 || y >= Chunk.HEIGHT || !canRemotePlayerModifyBlock(playerId, x, y, z)) {
            return null;
        }
        return new BlockActionPos(x, y, z);
    }

    private record BlockActionPos(int x, int y, int z) {
    }

    private boolean applyClientBoatPlacement(String playerId, com.google.gson.JsonObject actorState,
            ItemStack heldStack, org.joml.Vector3f direction) {
        if (heldStack.getType() != ItemType.BOAT) {
            return false;
        }
        org.joml.Vector3f origin = multiplayerActionSpawn(actorState, direction, 0.0f, 0.0f);
        for (float distance = 0.0f; distance <= 5.0f; distance += 0.1f) {
            int x = (int) Math.floor(origin.x + direction.x * distance);
            int y = (int) Math.floor(origin.y + direction.y * distance);
            int z = (int) Math.floor(origin.z + direction.z * distance);
            BlockType type = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
            if (type.isWater()) {
                if (!canRemotePlayerModifyBlock(playerId, x, y, z)) {
                    return false;
                }
                return world.placeBoatOnWater(x, y, z, jsonFloat(actorState, "yaw", 0.0f));
            }
            if (type.isSolid()) {
                return false;
            }
        }
        return false;
    }

    private boolean applyClientPaintingPlacement(String playerId, NetworkMessage message, ItemStack heldStack) {
        if (heldStack.getType() != ItemType.PAINTING) {
            return false;
        }
        int x = messageInt(message, "blockX", Integer.MIN_VALUE);
        int y = messageInt(message, "blockY", Integer.MIN_VALUE);
        int z = messageInt(message, "blockZ", Integer.MIN_VALUE);
        int face = messageInt(message, "blockFace", -1);
        if (y < 0 || y >= Chunk.HEIGHT || !isHorizontalBlockFace(face)) {
            return false;
        }
        if (!canRemotePlayerModifyBlock(playerId, x, y, z)) {
            return false;
        }
        return world.placePainting(x, y, z, face) != null;
    }

    private boolean applyClientMinecartPlacement(String playerId, NetworkMessage message, ItemStack heldStack) {
        ItemType type = heldStack.getType();
        if (type != ItemType.MINECART && type != ItemType.CHEST_MINECART && type != ItemType.FURNACE_MINECART) {
            return false;
        }
        int x = messageInt(message, "blockX", Integer.MIN_VALUE);
        int y = messageInt(message, "blockY", Integer.MIN_VALUE);
        int z = messageInt(message, "blockZ", Integer.MIN_VALUE);
        if (y < 0 || y >= Chunk.HEIGHT) {
            return false;
        }
        if (!canRemotePlayerModifyBlock(playerId, x, y, z)) {
            return false;
        }
        BlockType rail = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
        if (rail != BlockType.RAIL && rail != BlockType.POWERED_RAIL && rail != BlockType.DETECTOR_RAIL) {
            return false;
        }
        return world.placeMinecartOnRail(x, y, z, type);
    }

    private static boolean isHorizontalBlockFace(int face) {
        return face == Block.FACE_NORTH || face == Block.FACE_SOUTH
                || face == Block.FACE_EAST || face == Block.FACE_WEST;
    }

    private boolean applyClientDropItemUse(com.google.gson.JsonObject actorState, ItemStack heldStack,
            org.joml.Vector3f direction) {
        if (world == null || actorState == null || heldStack == null || heldStack.isEmpty()) {
            return false;
        }
        ItemStack dropped = heldStack.copy();
        dropped.setCount(1);
        org.joml.Vector3f throwDirection = new org.joml.Vector3f(direction);
        if (throwDirection.lengthSquared() <= 0.0001f) {
            throwDirection.set(0.0f, 0.0f, -1.0f);
        } else {
            throwDirection.normalize();
        }
        float x = jsonFloat(actorState, "x", 0.0f);
        float y = jsonFloat(actorState, "y", 80.0f);
        float z = jsonFloat(actorState, "z", 0.0f);
        world.spawnThrownStack(
                x + throwDirection.x,
                y + 1.5f,
                z + throwDirection.z,
                dropped,
                throwDirection.x * 8.0f,
                3.0f,
                throwDirection.z * 8.0f,
                DroppedItem.THROWN_PICKUP_DELAY_TICKS);
        return true;
    }

    private boolean applyClientDropStackUse(NetworkMessage message, com.google.gson.JsonObject actorState,
            org.joml.Vector3f direction) {
        if (world == null || message == null || actorState == null) {
            return false;
        }
        ItemStack dropped = itemStackFromBlockData(message, "stack");
        if (dropped == null || dropped.isEmpty()) {
            return false;
        }
        org.joml.Vector3f throwDirection = new org.joml.Vector3f(direction);
        if (throwDirection.lengthSquared() <= 0.0001f) {
            throwDirection.set(0.0f, 0.0f, -1.0f);
        } else {
            throwDirection.normalize();
        }
        float speed = Math.max(0.0f, Math.min(messageFloat(message, "power", 4.0f), 8.0f));
        float velocityY = Math.max(-4.0f, Math.min(messageFloat(message, "velocityY", 2.0f), 8.0f));
        float x = jsonFloat(actorState, "x", 0.0f);
        float y = jsonFloat(actorState, "y", 80.0f);
        float z = jsonFloat(actorState, "z", 0.0f);
        world.spawnThrownStack(
                x + throwDirection.x * 0.5f,
                y + 1.5f,
                z + throwDirection.z * 0.5f,
                dropped,
                throwDirection.x * speed,
                velocityY,
                throwDirection.z * speed,
                DroppedItem.THROWN_PICKUP_DELAY_TICKS);
        return true;
    }

    private boolean applyClientDeathDropStackUse(NetworkMessage message, com.google.gson.JsonObject actorState) {
        if (world == null || message == null || actorState == null
                || jsonFloat(actorState, "health", 20.0f) > 0.0f) {
            return false;
        }
        String playerId = messageString(message, "playerId", "");
        int sourceSlot = messageInt(message, "sourceSlot", -1);
        if (playerId.isBlank() || sourceSlot < 0 || sourceSlot >= MULTIPLAYER_INVENTORY_SLOTS) {
            return false;
        }
        Inventory inventory = multiplayerInventorySnapshotFor(playerId);
        ItemStack authoritative = multiplayerInventorySlot(inventory, sourceSlot);
        if (authoritative == null || authoritative.isEmpty()) {
            return false;
        }
        ItemStack requested = itemStackFromBlockData(message, "stack");
        if (requested != null && !requested.isEmpty()
                && (requested.getType() != authoritative.getType()
                        || requested.getCount() != authoritative.getCount()
                        || requested.getDurability() != authoritative.getDurability())) {
            return false;
        }
        ItemStack[] before = snapshotPlayerInventoryStacks(inventory);
        ItemStack dropped = authoritative.copy();
        applyLocalInventorySlot(inventory, sourceSlot, null);
        float x = jsonFloat(actorState, "x", 0.0f);
        float y = jsonFloat(actorState, "y", 80.0f) + 1.0f;
        float z = jsonFloat(actorState, "z", 0.0f);
        float motionX = Math.max(-8.0f, Math.min(messageFloat(message, "motionX", 0.0f), 8.0f));
        float motionY = Math.max(-8.0f, Math.min(messageFloat(message, "motionY", 0.0f), 8.0f));
        float motionZ = Math.max(-8.0f, Math.min(messageFloat(message, "motionZ", 0.0f), 8.0f));
        int pickupDelay = Math.max(0, Math.min(messageInt(message, "pickupDelay",
                DroppedItem.THROWN_PICKUP_DELAY_TICKS), 200));
        world.spawnThrownStack(x, y, z, dropped, motionX, motionY, motionZ, pickupDelay);
        broadcastChangedMultiplayerInventorySlots(playerId, inventory, before);
        return true;
    }

    private boolean applyClientDeathDropExperienceUse(NetworkMessage message, com.google.gson.JsonObject actorState) {
        if (world == null || message == null || actorState == null
                || jsonFloat(actorState, "health", 20.0f) > 0.0f) {
            return false;
        }
        int amount = multiplayerDeathDropExperience(actorState);
        if (amount <= 0) {
            return false;
        }
        float x = jsonFloat(actorState, "x", 0.0f);
        float y = jsonFloat(actorState, "y", 80.0f) + 1.0f;
        float z = jsonFloat(actorState, "z", 0.0f);
        world.spawnExperience(x, y, z, amount);
        clearMultiplayerDeathDropExperience(actorState);
        return true;
    }

    private void applyClientRespawnAction(NetworkMessage message) {
        if (multiplayerServer == null || world == null || message == null || message.data() == null) {
            return;
        }
        String playerId = messageString(message, "playerId", "");
        com.google.gson.JsonObject actorState = currentDimensionMultiplayerPlayerStateById(playerId);
        if (actorState == null || jsonFloat(actorState, "health", 20.0f) > 0.0f) {
            return;
        }
        int clientId = parseProtocolClientId(playerId);
        if (clientId <= 0) {
            return;
        }
        MultiplayerRespawnResult result = multiplayerRespawnResult(playerId, actorState);
        HashMap<String, String> data = new HashMap<>();
        data.put("accepted", "true");
        data.put("x", Float.toString(result.x()));
        data.put("y", Float.toString(result.y()));
        data.put("z", Float.toString(result.z()));
        data.put("target", result.target().name());
        data.put("bedMissing", Boolean.toString(result.bedMissing()));
        if (result.bedFoot() != null) {
            data.put("bedSpawn.set", "true");
            data.put("bedSpawn.x", Integer.toString(result.bedFoot().x()));
            data.put("bedSpawn.y", Integer.toString(result.bedFoot().y()));
            data.put("bedSpawn.z", Integer.toString(result.bedFoot().z()));
        } else {
            data.put("bedSpawn.set", "false");
        }
        actorState.addProperty("x", result.x());
        actorState.addProperty("y", result.y());
        actorState.addProperty("z", result.z());
        actorState.addProperty("health", MultiplayerProtocol.MAX_PLAYER_HEALTH);
        actorState.addProperty("stats.health", MultiplayerProtocol.MAX_PLAYER_HEALTH);
        sendMultiplayerClientAction(clientId, MultiplayerProtocol.ACTION_PLAYER_RESPAWN, data);
    }

    private MultiplayerRespawnResult multiplayerRespawnResult(String playerId, com.google.gson.JsonObject actorState) {
        BlockPos bedFoot = multiplayerBedSpawnFoot(actorState);
        if (bedFoot != null && world != null) {
            BlockPos bedRespawn = world.findBedRespawnPosition(bedFoot.x(), bedFoot.y(), bedFoot.z());
            if (bedRespawn != null) {
                return new MultiplayerRespawnResult(
                        bedRespawn.x() + 0.5f,
                        bedRespawn.y(),
                        bedRespawn.z() + 0.5f,
                        RespawnTarget.BED,
                        bedFoot,
                        false);
            }
        }
        org.joml.Vector3f override = multiplayerRespawnOverrides.get(playerId);
        if (override != null) {
            return new MultiplayerRespawnResult(override.x, override.y, override.z,
                    RespawnTarget.SAVED_SPAWN, null, bedFoot != null);
        }
        return new MultiplayerRespawnResult(worldSpawnX + 0.5f, worldSpawnY, worldSpawnZ + 0.5f,
                RespawnTarget.WORLD_SPAWN, null, bedFoot != null);
    }

    private BlockPos multiplayerBedSpawnFoot(com.google.gson.JsonObject actorState) {
        if (actorState == null || !jsonBoolean(actorState, "bedSpawn.set", false)) {
            return null;
        }
        int x = jsonInt(actorState, "bedSpawn.x", Integer.MIN_VALUE);
        int y = jsonInt(actorState, "bedSpawn.y", Integer.MIN_VALUE);
        int z = jsonInt(actorState, "bedSpawn.z", Integer.MIN_VALUE);
        if (y < 0 || y >= Chunk.HEIGHT || x == Integer.MIN_VALUE || z == Integer.MIN_VALUE) {
            return null;
        }
        return new BlockPos(x, y, z);
    }

    private void applyAcceptedMultiplayerRespawn(NetworkMessage message) {
        if (player == null || message == null || message.data() == null
                || !actionTargetsLocalMultiplayerClient(message)) {
            return;
        }
        float x = messageFloat(message, "x", worldSpawnX + 0.5f);
        float y = messageFloat(message, "y", worldSpawnY);
        float z = messageFloat(message, "z", worldSpawnZ + 0.5f);
        boolean bedSpawnSet = messageBoolean(message, "bedSpawn.set", false);
        if (bedSpawnSet) {
            player.setBedSpawnPosition(new BlockPos(
                    messageInt(message, "bedSpawn.x", (int) Math.floor(x)),
                    messageInt(message, "bedSpawn.y", (int) Math.floor(y)),
                    messageInt(message, "bedSpawn.z", (int) Math.floor(z))),
                    x, y, z);
        } else {
            player.setSpawnPosition(x, y, z);
        }
        player.respawn();
        multiplayerRespawnRequestPending = false;
        sendMultiplayerPlayerStateNow();
        syncMultiplayerInventoryStateNow();
        if (messageBoolean(message, "bedMissing", false)) {
            addChatMessage("Your home bed was missing or obstructed");
        }
        deathMenuOpen = false;
        resumeGame();
    }

    private int multiplayerDeathDropExperience(com.google.gson.JsonObject actorState) {
        if (actorState == null) {
            return 0;
        }
        int level = jsonInt(actorState, "progression.level", -1);
        if (level < 0) {
            PlayerProgression progression = new PlayerProgression();
            progression.restore(Math.max(0, jsonInt(actorState, "progression.totalExperience", 0)),
                    Math.max(0, jsonInt(actorState, "progression.score", 0)));
            level = progression.getLevel();
        }
        return Math.min(100, Math.max(0, level) * 7);
    }

    private void clearMultiplayerDeathDropExperience(com.google.gson.JsonObject actorState) {
        if (actorState == null) {
            return;
        }
        actorState.addProperty("progression.totalExperience", 0);
        actorState.addProperty("progression.experienceIntoLevel", 0);
        actorState.addProperty("progression.experienceToNextLevel", PlayerProgression.experienceForLevel(1));
        actorState.addProperty("progression.level", 0);
    }

    private boolean applyClientFishingCast(String playerId, com.google.gson.JsonObject actorState, ItemStack heldStack,
            org.joml.Vector3f direction) {
        if (heldStack.getType() != ItemType.FISHING_ROD || playerId == null || playerId.isBlank()) {
            return false;
        }
        FishingHookEntity existing = multiplayerFishingHooks.get(playerId);
        if (existing != null && !existing.isRemoved()) {
            return false;
        }
        org.joml.Vector3f spawn = multiplayerActionSpawn(actorState, direction, 0.45f, -0.15f);
        FishingHookEntity hook = new FishingHookEntity(spawn.x, spawn.y, spawn.z,
                direction.x * 1.5f, direction.y * 1.5f + 0.1f, direction.z * 1.5f, null);
        hook.setYaw(jsonFloat(actorState, "yaw", hook.getYaw()));
        hook.setPitch(jsonFloat(actorState, "pitch", hook.getPitch()));
        hook.setRemoteOwnerSupplier(() -> multiplayerFishingOwnerSnapshot(playerId));
        hook.setRemoteOwnerPlayerId(playerId);
        multiplayerFishingHooks.put(playerId, hook);
        world.spawnEntity(hook);
        world.playThrowSound(spawn.x, spawn.y, spawn.z);
        return true;
    }

    private boolean applyClientFishingReel(String playerId, com.google.gson.JsonObject actorState,
            ItemStack heldStack) {
        if (heldStack.getType() != ItemType.FISHING_ROD || playerId == null || playerId.isBlank()) {
            return false;
        }
        FishingHookEntity hook = multiplayerFishingHooks.remove(playerId);
        if (hook == null || hook.isRemoved()) {
            return false;
        }
        FishingHookEntity.ReelResult reelResult = hook.reelInWithResult();
        if (reelResult.durabilityCost() > 0) {
            damageMultiplayerSelectedDurable(playerId, actorState, reelResult.durabilityCost());
        }
        broadcastMultiplayerEntityStateNow(hook);
        return true;
    }

    private FishingHookEntity.OwnerSnapshot multiplayerFishingOwnerSnapshot(String playerId) {
        com.google.gson.JsonObject state = currentDimensionMultiplayerPlayerStateById(playerId);
        if (state == null) {
            return new FishingHookEntity.OwnerSnapshot(0.0f, 0.0f, 0.0f, false, false, 0.0f, false);
        }
        ItemStack selected = multiplayerSelectedStack(playerId, state);
        boolean holdingRod = selected != null && !selected.isEmpty() && selected.getType() == ItemType.FISHING_ROD;
        return new FishingHookEntity.OwnerSnapshot(
                jsonFloat(state, "x", 0.0f),
                jsonFloat(state, "y", 80.0f),
                jsonFloat(state, "z", 0.0f),
                jsonFloat(state, "health", 20.0f) > 0.0f,
                holdingRod,
                jsonFloat(state, "yaw", 0.0f),
                jsonBoolean(state, "sneaking", false));
    }

    private void applyClientSignUpdate(NetworkMessage message) {
        if (multiplayerServer == null || world == null || message == null || message.data() == null) {
            return;
        }
        String playerId = messageString(message, "playerId", "");
        int x = messageInt(message, "x", Integer.MIN_VALUE);
        int y = messageInt(message, "y", Integer.MIN_VALUE);
        int z = messageInt(message, "z", Integer.MIN_VALUE);
        if (y < 0 || y >= com.craftzero.world.Chunk.HEIGHT) {
            return;
        }
        BlockType type = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
        if (!type.isSign() || !(world.getTileEntity(x, y, z) instanceof SignTileEntity sign)) {
            return;
        }
        if (!canRemotePlayerModifyBlock(playerId, x, y, z)) {
            broadcastMultiplayerTileEntity(sign);
            return;
        }
        for (int i = 0; i < MultiplayerProtocol.SIGN_LINE_COUNT; i++) {
            sign.setLine(i, messageString(message, "signLine" + i, ""));
        }
        broadcastMultiplayerTileEntity(sign);
    }

    private void applyClientEnchantItem(NetworkMessage message) {
        if (multiplayerServer == null || world == null || message == null || message.data() == null) {
            return;
        }
        String playerId = messageString(message, "playerId", "");
        int clientId = parseProtocolClientId(playerId);
        com.google.gson.JsonObject actorState = liveMultiplayerPlayerStateById(playerId);
        if (clientId <= 0 || actorState == null) {
            return;
        }
        int x = messageInt(message, "x", Integer.MIN_VALUE);
        int y = messageInt(message, "y", Integer.MIN_VALUE);
        int z = messageInt(message, "z", Integer.MIN_VALUE);
        if (!canRemotePlayerModifyBlock(playerId, x, y, z)) {
            return;
        }
        if (y < 0 || y >= Chunk.HEIGHT
                || world.getBlockIfLoaded(x, y, z, BlockType.AIR) != BlockType.ENCHANTING_TABLE
                || !multiplayerBlockActionInReach(actorState, x, y, z)) {
            return;
        }
        int offerSlot = messageInt(message, "offerSlot", -1);
        if (offerSlot < 0 || offerSlot > 2) {
            return;
        }
        ItemStack input = itemStackFromBlockData(message, "table.item");
        if (!EnchantmentResolver.isEnchantable(input)) {
            return;
        }
        long offerSeed = messageLong(message, "offerSeed", 0L);
        int bookshelfPower = BookshelfPower.count(world, x, y, z);
        int cost = EnchantmentResolver.offerCost(
                multiplayerEnchantOfferRandom(offerSeed, x, y, z, input, offerSlot),
                offerSlot, bookshelfPower, input);
        if (cost <= 0 || cost != messageInt(message, "offerCost", -1)) {
            return;
        }
        PlayerProgression progression = new PlayerProgression();
        progression.restore(
                Math.max(0, jsonInt(actorState, "progression.totalExperience", 0)),
                Math.max(0, jsonInt(actorState, "progression.score", 0)));
        if (progression.getLevel() < cost) {
            return;
        }
        List<EnchantmentInstance> enchantments = EnchantmentResolver.generate(
                multiplayerEnchantOfferRandom(offerSeed, x, y, z, input, offerSlot),
                input, cost);
        if (enchantments.isEmpty() || !progression.consumeLevels(cost)) {
            return;
        }
        ItemStack result = input.copy();
        result.setEnchantments(enchantments);
        updateMultiplayerActorProgressionState(actorState, progression);
        sendAcceptedMultiplayerEnchantResult(clientId, x, y, z, offerSlot, cost, result, progression);
    }

    private void applyAcceptedMultiplayerEnchantResult(NetworkMessage message) {
        if (!actionTargetsLocalMultiplayerClient(message) || player == null) {
            return;
        }
        int totalExperience = Math.max(0, messageInt(message, "totalExperience",
                player.getStats().getProgression().getTotalExperience()));
        int score = Math.max(0, messageInt(message, "score", player.getStats().getProgression().getScore()));
        player.getStats().getProgression().restore(totalExperience, score);
        int x = messageInt(message, "x", Integer.MIN_VALUE);
        int y = messageInt(message, "y", Integer.MIN_VALUE);
        int z = messageInt(message, "z", Integer.MIN_VALUE);
        ItemStack result = itemStackFromBlockData(message, "table.item");
        if (enchantingTableScreen != null && enchantingTableScreen.isOpen() && result != null) {
            enchantingTableScreen.applyRemoteEnchantResult(x, y, z, result);
        }
        sendMultiplayerPlayerStateNow();
    }

    private void sendAcceptedMultiplayerEnchantResult(int clientId, int x, int y, int z,
            int offerSlot, int cost, ItemStack result, PlayerProgression progression) {
        if (clientId <= 0 || result == null || result.isEmpty() || progression == null) {
            return;
        }
        HashMap<String, String> data = new HashMap<>();
        data.put("accepted", "true");
        data.put("x", Integer.toString(x));
        data.put("y", Integer.toString(y));
        data.put("z", Integer.toString(z));
        data.put("offerSlot", Integer.toString(offerSlot));
        data.put("offerCost", Integer.toString(cost));
        data.put("totalExperience", Integer.toString(Math.max(0, progression.getTotalExperience())));
        data.put("score", Integer.toString(Math.max(0, progression.getScore())));
        data.put("level", Integer.toString(Math.max(0, progression.getLevel())));
        putItemStackData(data, "table.item", result);
        sendMultiplayerClientAction(clientId, MultiplayerProtocol.ACTION_ENCHANT_ITEM, data);
    }

    private void updateMultiplayerActorProgressionState(com.google.gson.JsonObject actorState,
            PlayerProgression progression) {
        if (actorState == null || progression == null) {
            return;
        }
        actorState.addProperty("progression.totalExperience", Math.max(0, progression.getTotalExperience()));
        actorState.addProperty("progression.score", Math.max(0, progression.getScore()));
        actorState.addProperty("progression.level", Math.max(0, progression.getLevel()));
        actorState.addProperty("progression.experienceIntoLevel",
                Math.max(0, progression.getExperienceIntoLevel()));
        actorState.addProperty("progression.experienceToNextLevel",
                Math.max(0, progression.getExperienceToNextLevel()));
    }

    private java.util.Random multiplayerEnchantOfferRandom(long offerSeed, int tableX, int tableY, int tableZ,
            ItemStack stack, int slot) {
        long seed = 0x5DEECE66DL ^ offerSeed;
        seed ^= tableX * 341873128712L;
        seed ^= tableY * 132897987541L;
        seed ^= tableZ * 42317861L;
        seed ^= (long) multiplayerEnchantStackOfferKey(stack) * 31L;
        seed ^= slot * 0x9E3779B97F4A7C15L;
        return new java.util.Random(seed);
    }

    private int multiplayerEnchantStackOfferKey(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        return Objects.hash(stack.getType(), stack.getDurability(), stack.getEnchantments(),
                stack.getPotionData(), stack.getMetadata());
    }

    private void applyClientCraftItem(NetworkMessage message) {
        if (multiplayerServer == null || message == null || message.data() == null) {
            return;
        }
        String playerId = messageString(message, "playerId", "");
        int clientId = parseProtocolClientId(playerId);
        com.google.gson.JsonObject actorState = liveMultiplayerPlayerStateById(playerId);
        if (clientId <= 0 || actorState == null) {
            return;
        }
        int gridSize = messageInt(message, "gridSize", 0);
        if (gridSize != 2 && gridSize != 3) {
            return;
        }
        int x = messageInt(message, "x", Integer.MIN_VALUE);
        int y = messageInt(message, "y", Integer.MIN_VALUE);
        int z = messageInt(message, "z", Integer.MIN_VALUE);
        if (gridSize == 3 && !canReplayRemoteCraftingTable(playerId, actorState, x, y, z)) {
            return;
        }
        int expectedSlots = gridSize * gridSize;
        if (messageInt(message, "craft.grid.size", -1) != expectedSlots) {
            return;
        }
        ItemStack[] grid = new ItemStack[expectedSlots];
        applyTileInventory(message, "craft.grid", grid);
        if (findMultiplayerCraftingRecipe(gridSize, grid) == null) {
            return;
        }
        Inventory inventory = multiplayerInventorySnapshotFor(playerId);
        ItemStack[] before = snapshotPlayerInventoryStacks(inventory);
        applyLocalInventorySlot(inventory, MULTIPLAYER_CURSOR_SLOT, itemStackFromBlockData(message, "cursor"));
        if (gridSize == 2) {
            copyCraftingGrid(grid, inventory.getCraftingGrid());
            grid = inventory.getCraftingGrid();
        }
        int requestedCrafts = Math.max(1, Math.min(messageInt(message, "crafts", 1), 64));
        int crafted = replayMultiplayerCraft(inventory, grid, gridSize,
                messageBoolean(message, "quickMove", false), requestedCrafts);
        if (crafted <= 0) {
            return;
        }
        broadcastChangedMultiplayerInventorySlots(playerId, inventory, before);
        if (gridSize == 3) {
            sendAcceptedMultiplayerCraftResult(clientId, x, y, z, gridSize, crafted, grid);
        }
    }

    private boolean canReplayRemoteCraftingTable(String playerId, com.google.gson.JsonObject actorState,
            int x, int y, int z) {
        return world != null
                && y >= 0 && y < Chunk.HEIGHT
                && canRemotePlayerModifyBlock(playerId, x, y, z)
                && world.getBlockIfLoaded(x, y, z, BlockType.AIR) == BlockType.CRAFTING_TABLE
                && multiplayerBlockActionInReach(actorState, x, y, z);
    }

    private int replayMultiplayerCraft(Inventory inventory, ItemStack[] grid, int gridSize,
            boolean quickMove, int requestedCrafts) {
        if (inventory == null || grid == null || requestedCrafts <= 0) {
            return 0;
        }
        if (!quickMove) {
            CraftingRecipe recipe = findMultiplayerCraftingRecipe(gridSize, grid);
            return CraftingGridOps.takeOutputToCursor(inventory, grid, recipe, null) ? 1 : 0;
        }
        int crafted = 0;
        for (int i = 0; i < requestedCrafts; i++) {
            CraftingRecipe recipe = findMultiplayerCraftingRecipe(gridSize, grid);
            if (recipe == null) {
                break;
            }
            ItemStack output = recipe.getOutput();
            if (!inventory.canAddItem(output) || !inventory.addItem(output)) {
                break;
            }
            CraftingGridOps.consumeIngredients(inventory, grid, recipe, null);
            crafted++;
        }
        return crafted;
    }

    private CraftingRecipe findMultiplayerCraftingRecipe(int gridSize, ItemStack[] grid) {
        return gridSize == 3 ? CraftingRegistry.findRecipe3x3(grid) : CraftingRegistry.findRecipe(grid);
    }

    private void copyCraftingGrid(ItemStack[] source, ItemStack[] target) {
        if (target == null) {
            return;
        }
        for (int i = 0; i < target.length; i++) {
            target[i] = source != null && i < source.length && source[i] != null ? source[i].copy() : null;
        }
    }

    private void sendAcceptedMultiplayerCraftResult(int clientId, int x, int y, int z,
            int gridSize, int crafted, ItemStack[] grid) {
        if (clientId <= 0 || gridSize != 3 || grid == null) {
            return;
        }
        HashMap<String, String> data = new HashMap<>();
        data.put("accepted", "true");
        data.put("gridSize", Integer.toString(gridSize));
        data.put("crafts", Integer.toString(Math.max(0, crafted)));
        data.put("x", Integer.toString(x));
        data.put("y", Integer.toString(y));
        data.put("z", Integer.toString(z));
        putTileInventoryData(data, "craft.grid", grid);
        sendMultiplayerClientAction(clientId, MultiplayerProtocol.ACTION_CRAFT_ITEM, data);
    }

    private void applyAcceptedMultiplayerCraftResult(NetworkMessage message) {
        if (!actionTargetsLocalMultiplayerClient(message) || craftingTableScreen == null) {
            return;
        }
        int gridSize = messageInt(message, "gridSize", 0);
        if (gridSize != 3 || !craftingTableScreen.isOpen()) {
            return;
        }
        ItemStack[] grid = new ItemStack[9];
        applyTileInventory(message, "craft.grid", grid);
        craftingTableScreen.applyRemoteCraftingGrid(
                messageInt(message, "x", Integer.MIN_VALUE),
                messageInt(message, "y", Integer.MIN_VALUE),
                messageInt(message, "z", Integer.MIN_VALUE),
                grid);
    }

    private void applyClientContainerUpdate(NetworkMessage message) {
        if (multiplayerServer == null || world == null || message == null || message.data() == null) {
            return;
        }
        String playerId = messageString(message, "playerId", "");
        com.google.gson.JsonObject actorState = liveMultiplayerPlayerStateById(playerId);
        if (parseProtocolClientId(playerId) <= 0 || actorState == null) {
            return;
        }
        String entityId = messageString(message, "entityId", "");
        if (!entityId.isBlank()) {
            applyClientEntityContainerUpdate(message, actorState, entityId);
            return;
        }
        int x = messageInt(message, "x", Integer.MIN_VALUE);
        int y = messageInt(message, "y", Integer.MIN_VALUE);
        int z = messageInt(message, "z", Integer.MIN_VALUE);
        if (y < 0 || y >= Chunk.HEIGHT) {
            return;
        }
        if (!canRemotePlayerModifyBlock(playerId, x, y, z)) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile != null) {
                broadcastMultiplayerTileEntity(tile);
            }
            return;
        }
        String tileType = messageString(message, "tileType", "");
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!networkTileMatches(tile, tileType)) {
            return;
        }
        ItemStack[] inventory = networkEditableContainerInventory(tile);
        if (inventory == null || messageInt(message, "tile.inventory.size", -1) != inventory.length) {
            return;
        }
        ItemStack[] submittedInventory = networkContainerInventorySnapshot(
                message, "tile.inventory", inventory.length);
        if (submittedInventory == null || !networkContainerInventoryAllowed(tile, submittedInventory)) {
            broadcastMultiplayerTileEntity(tile);
            return;
        }
        copyTileInventory(submittedInventory, inventory);
        tile.markDirty();
        broadcastMultiplayerTileEntity(tile);
    }

    private void applyClientEntityContainerUpdate(NetworkMessage message,
            com.google.gson.JsonObject actorState, String entityId) {
        if (!"chest_minecart".equals(messageString(message, "tileType", ""))) {
            return;
        }
        Entity entity = multiplayerEntityById(entityId);
        if (!(entity instanceof ChestMinecartEntity chestMinecart)
                || !multiplayerEntityActionInReach(actorState, chestMinecart)
                || messageInt(message, "tile.inventory.size", -1) != ChestMinecartEntity.SIZE) {
            return;
        }
        ItemStack[] submittedInventory = networkContainerInventorySnapshot(
                message, "tile.inventory", ChestMinecartEntity.SIZE);
        if (submittedInventory == null) {
            return;
        }
        copyTileInventory(submittedInventory, chestMinecart.getInventory());
        broadcastMultiplayerEntityStateNow(chestMinecart);
    }

    private ItemStack[] networkContainerInventorySnapshot(NetworkMessage message, String prefix, int expectedSize) {
        if (message == null || message.data() == null || expectedSize < 0
                || messageInt(message, prefix + ".size", -1) != expectedSize) {
            return null;
        }
        ItemStack[] inventory = new ItemStack[expectedSize];
        for (int slot = 0; slot < expectedSize; slot++) {
            inventory[slot] = itemStackFromBlockData(message, prefix + "." + slot);
        }
        return inventory;
    }

    private void copyTileInventory(ItemStack[] source, ItemStack[] target) {
        if (source == null || target == null) {
            return;
        }
        int slots = Math.min(source.length, target.length);
        for (int slot = 0; slot < slots; slot++) {
            target[slot] = source[slot] == null ? null : source[slot].copy();
        }
        for (int slot = slots; slot < target.length; slot++) {
            target[slot] = null;
        }
    }

    private boolean networkContainerInventoryAllowed(TileEntity tile, ItemStack[] submittedInventory) {
        if (tile == null || submittedInventory == null) {
            return false;
        }
        if (tile instanceof FurnaceTileEntity furnace) {
            return networkFurnaceInventoryAllowed(furnace, submittedInventory);
        }
        if (tile instanceof BrewingStandTileEntity) {
            return networkBrewingInventoryAllowed(submittedInventory);
        }
        return tile instanceof ChestTileEntity || tile instanceof DispenserTileEntity;
    }

    private boolean networkFurnaceInventoryAllowed(FurnaceTileEntity furnace, ItemStack[] submittedInventory) {
        if (furnace == null || submittedInventory.length != FurnaceTileEntity.SIZE) {
            return false;
        }
        ItemStack fuel = submittedInventory[FurnaceTileEntity.SLOT_FUEL];
        if (fuel != null && !fuel.isEmpty() && !FuelRegistry.isFuel(fuel)) {
            return false;
        }
        return networkOutputSlotNotIncreased(
                furnace.getInventory()[FurnaceTileEntity.SLOT_OUTPUT],
                submittedInventory[FurnaceTileEntity.SLOT_OUTPUT]);
    }

    private boolean networkBrewingInventoryAllowed(ItemStack[] submittedInventory) {
        if (submittedInventory == null || submittedInventory.length != BrewingStandTileEntity.SIZE) {
            return false;
        }
        for (int slot = BrewingStandTileEntity.SLOT_BOTTLE_0; slot <= BrewingStandTileEntity.SLOT_BOTTLE_2; slot++) {
            ItemStack bottle = submittedInventory[slot];
            if (bottle != null && !bottle.isEmpty() && !BrewingRecipeRegistry.isBottleSlotItem(bottle)) {
                return false;
            }
        }
        ItemStack ingredient = submittedInventory[BrewingStandTileEntity.SLOT_INGREDIENT];
        return ingredient == null || ingredient.isEmpty() || BrewingRecipeRegistry.isIngredient(ingredient);
    }

    private boolean networkOutputSlotNotIncreased(ItemStack current, ItemStack submitted) {
        if (submitted == null || submitted.isEmpty()) {
            return true;
        }
        if (current == null || current.isEmpty()) {
            return false;
        }
        return sameNetworkStackKind(current, submitted) && submitted.getCount() <= current.getCount();
    }

    private boolean sameNetworkStackKind(ItemStack left, ItemStack right) {
        if (left == null || right == null || left.getType() != right.getType()
                || left.getDurability() != right.getDurability()
                || !Objects.equals(left.getCustomName(), right.getCustomName())
                || !Objects.equals(left.getPotionData(), right.getPotionData())
                || !Objects.equals(left.getEnchantments(), right.getEnchantments())) {
            return false;
        }
        return Objects.equals(left.getMetadata(), right.getMetadata());
    }

    private ItemStack[] networkEditableContainerInventory(TileEntity tile) {
        if (tile instanceof ChestTileEntity chest) {
            return chest.getInventory();
        }
        if (tile instanceof FurnaceTileEntity furnace) {
            return furnace.getInventory();
        }
        if (tile instanceof DispenserTileEntity dispenser) {
            return dispenser.getInventory();
        }
        if (tile instanceof BrewingStandTileEntity brewingStand) {
            return brewingStand.getInventory();
        }
        return null;
    }

    private Entity multiplayerEntityById(String entityId) {
        if (entityId == null || entityId.isBlank()) {
            return null;
        }
        for (Map.Entry<Entity, String> entry : multiplayerEntityIds.entrySet()) {
            if (entityId.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private com.google.gson.JsonObject multiplayerPlayerStateById(String playerId) {
        if (playerId == null || playerId.isBlank() || multiplayerServer == null) {
            return null;
        }
        for (com.google.gson.JsonObject state : multiplayerServer.playerStates().values()) {
            if (state != null && state.has("playerId") && playerId.equals(state.get("playerId").getAsString())) {
                return state;
            }
        }
        return null;
    }

    private com.google.gson.JsonObject currentDimensionMultiplayerPlayerStateById(String playerId) {
        com.google.gson.JsonObject state = multiplayerPlayerStateById(playerId);
        return remotePlayerStateTargetsCurrentDimension(state) ? state : null;
    }

    private com.google.gson.JsonObject liveMultiplayerPlayerStateById(String playerId) {
        com.google.gson.JsonObject state = multiplayerPlayerStateById(playerId);
        return isLiveRemotePlayerState(state) ? state : null;
    }

    private boolean multiplayerEntityActionInReach(com.google.gson.JsonObject actorState, Entity target) {
        if (actorState == null || target == null) {
            return false;
        }
        float actorX = jsonFloat(actorState, "x", Float.NaN);
        float actorY = jsonFloat(actorState, "y", Float.NaN);
        float actorZ = jsonFloat(actorState, "z", Float.NaN);
        if (!Float.isFinite(actorX) || !Float.isFinite(actorY) || !Float.isFinite(actorZ)) {
            return false;
        }
        double dx = target.getX() - actorX;
        double dy = target.getY() - (actorY + MultiplayerProtocol.PLAYER_EYE_HEIGHT);
        double dz = target.getZ() - actorZ;
        double distanceSq = dx * dx + dy * dy + dz * dz;
        return distanceSq <= MultiplayerProtocol.MAX_CLIENT_ENTITY_ACTION_DISTANCE_SQ;
    }

    private boolean multiplayerBlockActionInReach(com.google.gson.JsonObject actorState, int x, int y, int z) {
        if (actorState == null) {
            return false;
        }
        float actorX = jsonFloat(actorState, "x", Float.NaN);
        float actorY = jsonFloat(actorState, "y", Float.NaN);
        float actorZ = jsonFloat(actorState, "z", Float.NaN);
        if (!Float.isFinite(actorX) || !Float.isFinite(actorY) || !Float.isFinite(actorZ)) {
            return false;
        }
        double dx = x + 0.5d - actorX;
        double dy = y + 0.5d - (actorY + MultiplayerProtocol.PLAYER_EYE_HEIGHT);
        double dz = z + 0.5d - actorZ;
        double distanceSq = dx * dx + dy * dy + dz * dz;
        return distanceSq <= MultiplayerProtocol.MAX_CLIENT_BLOCK_EDIT_DISTANCE_SQ;
    }

    private boolean canRemotePlayerModifyBlock(String playerId, int x, int y, int z) {
        if (!isWithinServerBuildHeight(y)) {
            return false;
        }
        return canRemotePlayerModifyBlock(playerId, x, z);
    }

    private boolean canRemotePlayerModifyBlock(String playerId, int x, int z) {
        if (!isProtectedSpawnBlock(x, z)) {
            return true;
        }
        return isMultiplayerOperator(playerId);
    }

    private boolean isWithinServerBuildHeight(int y) {
        return y >= MultiplayerProtocol.WORLD_MIN_Y && y < multiplayerMaxBuildHeight;
    }

    private boolean isProtectedSpawnBlock(int x, int z) {
        if (multiplayerSpawnProtection <= 0 || world == null || world.getDimension() != Dimension.OVERWORLD) {
            return false;
        }
        return Math.abs((long) x - worldSpawnX) <= multiplayerSpawnProtection
                && Math.abs((long) z - worldSpawnZ) <= multiplayerSpawnProtection;
    }

    private boolean isMultiplayerOperator(String playerId) {
        if (playerId == null || playerId.isBlank() || "host".equalsIgnoreCase(playerId)) {
            return true;
        }
        HashSet<String> candidates = new HashSet<>();
        candidates.add(playerId.trim().toLowerCase(java.util.Locale.ROOT));
        int clientId = parseProtocolClientId(playerId);
        if (clientId > 0) {
            candidates.add(("player" + clientId).toLowerCase(java.util.Locale.ROOT));
            if (multiplayerServer != null) {
                String username = multiplayerServer.usernameForClient(clientId);
                if (username != null && !username.isBlank()) {
                    candidates.add(username.trim().toLowerCase(java.util.Locale.ROOT));
                }
            }
        }
        com.google.gson.JsonObject state = multiplayerPlayerStateById(playerId);
        if (state != null && state.has("username")) {
            candidates.add(state.get("username").getAsString().trim().toLowerCase(java.util.Locale.ROOT));
        }
        MultiplayerRosterEntry rosterEntry = multiplayerRoster.get(playerId);
        if (rosterEntry != null && rosterEntry.username() != null && !rosterEntry.username().isBlank()) {
            candidates.add(rosterEntry.username().trim().toLowerCase(java.util.Locale.ROOT));
        }
        for (String candidate : candidates) {
            if (operators.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private ItemType multiplayerHeldItemType(com.google.gson.JsonObject actorState) {
        if (actorState == null) {
            return null;
        }
        return parseNetworkItemType(
                jsonString(actorState, "heldItemId", "air"),
                jsonInt(actorState, "heldItemDamage", -1));
    }

    private boolean applyClientEntityAttack(String playerId, com.google.gson.JsonObject actorState,
            Entity target, ItemStack heldStack) {
        if (target == null || target.isRemoved()) {
            return false;
        }
        boolean creative = isCreativeMultiplayerPlayerState(actorState);
        float damage = multiplayerAttackDamage(actorState, heldStack,
                target instanceof LivingEntity living ? living : null);
        boolean sprintKnockback = hostedSprintKnockbackReady(playerId, actorState);
        boolean applied = false;
        boolean damageHeldTool = false;
        if (target instanceof EndCrystalEntity crystal) {
            applied = attackEndCrystalFromRemotePlayer(playerId, actorState, crystal);
            damageHeldTool = applied;
        } else if (target instanceof LivingEntity living) {
            if (living.isDead()) {
                return false;
            }
            applied = living.damage(damage, DamageSource.remotePlayerAttack(playerId,
                    jsonFloat(actorState, "x", living.getX()),
                    jsonFloat(actorState, "y", living.getY()) + (float) MultiplayerProtocol.PLAYER_EYE_HEIGHT,
                    jsonFloat(actorState, "z", living.getZ()),
                    EnchantmentResolver.getLevel(heldStack, EnchantmentType.LOOTING)));
            if (applied && living.getHealth() > 0.0f) {
                applyClientEntityKnockback(actorState, living, heldStack, sprintKnockback);
                applyClientEntityFireAspect(living, heldStack);
                notifyRemoteOwnedWolvesOfCombatTarget(playerId, actorState, living);
                if (sprintKnockback) {
                    markHostedSprintKnockbackUsed(playerId, actorState);
                }
            }
            damageHeldTool = applied;
        } else if (target instanceof MinecartEntity minecart) {
            applied = minecart.attack(damage, creative);
            damageHeldTool = applied;
        } else if (target instanceof BoatEntity boat) {
            applied = boat.attack(damage, creative);
            damageHeldTool = applied;
        } else if (target instanceof PaintingEntity painting) {
            painting.breakAsItem(creative);
            applied = true;
        } else if (target instanceof FireballEntity fireball) {
            org.joml.Vector3f direction = new org.joml.Vector3f(
                    fireball.getX() - jsonFloat(actorState, "x", fireball.getX()),
                    fireball.getY() - jsonFloat(actorState, "y", fireball.getY()),
                    fireball.getZ() - jsonFloat(actorState, "z", fireball.getZ()));
            if (direction.lengthSquared() <= 0.0001f) {
                direction.set(0.0f, 0.0f, -1.0f);
            } else {
                direction.normalize();
            }
            if (fireball.deflectFromPlayer(direction)) {
                fireball.setRemoteDeflectorPlayerId(playerId);
                applied = true;
            }
        }
        if (damageHeldTool && !creative && heldStack != null && heldStack.isTool()) {
            damageMultiplayerSelectedDurable(playerId, actorState, 1);
        }
        return applied;
    }

    private boolean attackEndCrystalFromRemotePlayer(String playerId, com.google.gson.JsonObject actorState,
            EndCrystalEntity crystal) {
        if (crystal == null || crystal.isDead() || crystal.isRemoved()) {
            return false;
        }
        return crystal.damage(0.0f, DamageSource.remotePlayerAttack(playerId,
                jsonFloat(actorState, "x", crystal.getX()),
                jsonFloat(actorState, "y", crystal.getY()) + (float) MultiplayerProtocol.PLAYER_EYE_HEIGHT,
                jsonFloat(actorState, "z", crystal.getZ()),
                0));
    }

    private void notifyRemoteOwnedWolvesOfCombatTarget(String playerId, com.google.gson.JsonObject actorState,
            LivingEntity target) {
        if (world == null || target == null || target.isDead() || target.isRemoved()) {
            return;
        }
        String username = multiplayerActorUsername(playerId, actorState);
        if (username == null || username.isBlank()) {
            return;
        }
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Wolf wolf
                    && wolf.isOwnedByName(username)
                    && wolf.distanceToSquared(target) <= Wolf.ASSIST_RANGE * Wolf.ASSIST_RANGE) {
                wolf.setAssistTarget(target);
            }
        }
    }

    private float multiplayerAttackDamage(com.google.gson.JsonObject actorState, ItemStack heldStack,
            LivingEntity target) {
        float damage = 1.0f;
        if (heldStack != null && !heldStack.isEmpty() && heldStack.isTool()) {
            damage = heldStack.getType().getToolType().getAttackDamage();
        }
        damage += target == null
                ? EnchantmentResolver.attackDamageBonus(heldStack)
                : EnchantmentResolver.attackDamageBonus(heldStack, target);
        damage += hostedClientAttackDamageBonus(actorState);
        return Math.max(0.0f, damage);
    }

    private float hostedClientAttackDamageBonus(com.google.gson.JsonObject actorState) {
        float bonus = 0.0f;
        int strength = hostedClientEffectAmplifier(actorState, StatusEffectType.STRENGTH);
        if (strength >= 0) {
            bonus += 3 << strength;
        }
        int weakness = hostedClientEffectAmplifier(actorState, StatusEffectType.WEAKNESS);
        if (weakness >= 0) {
            bonus -= 2 << weakness;
        }
        return bonus;
    }

    private int hostedClientEffectAmplifier(com.google.gson.JsonObject actorState, StatusEffectType type) {
        if (type == null) {
            return -1;
        }
        int best = -1;
        for (StatusEffectInstance effect : hostedClientStatusEffects(actorState)) {
            if (effect.type() == type && !effect.expired()) {
                best = Math.max(best, effect.amplifier());
            }
        }
        return best;
    }

    private boolean hostedSprintKnockbackReady(String playerId, com.google.gson.JsonObject actorState) {
        if (playerId == null || playerId.isBlank() || actorState == null) {
            return false;
        }
        if (!jsonBoolean(actorState, "input.forward", false)) {
            multiplayerRemoteSprintKnockbackUsed.remove(playerId);
            return false;
        }
        return jsonBoolean(actorState, "remote.sprinting", false)
                && !multiplayerRemoteSprintKnockbackUsed.contains(playerId);
    }

    private void markHostedSprintKnockbackUsed(String playerId, com.google.gson.JsonObject actorState) {
        if (playerId == null || playerId.isBlank()) {
            return;
        }
        multiplayerRemoteSprintKnockbackUsed.add(playerId);
        if (actorState != null) {
            actorState.addProperty("remote.sprinting", false);
        }
    }

    private void applyClientEntityKnockback(com.google.gson.JsonObject actorState, LivingEntity target,
            ItemStack heldStack, boolean sprintKnockback) {
        float actorX = jsonFloat(actorState, "x", target.getX());
        float actorZ = jsonFloat(actorState, "z", target.getZ());
        float dx = target.getX() - actorX;
        float dz = target.getZ() - actorZ;
        float distance = (float) Math.sqrt(dx * dx + dz * dz);
        if (distance <= 0.001f) {
            return;
        }
        float knockback = CombatRules.PLAYER_ATTACK_KNOCKBACK
                + EnchantmentResolver.getLevel(heldStack, EnchantmentType.KNOCKBACK) * 0.4f;
        if (sprintKnockback) {
            knockback += CombatRules.PLAYER_ATTACK_SPRINT_BONUS;
        }
        target.addMotion(
                dx / distance * knockback,
                CombatRules.PLAYER_ATTACK_VERTICAL_KNOCKBACK,
                dz / distance * knockback);
    }

    private void applyClientEntityFireAspect(LivingEntity target, ItemStack heldStack) {
        int fireTicks = EnchantmentResolver.getLevel(heldStack, EnchantmentType.FIRE_ASPECT) * 80;
        if (fireTicks > 0) {
            target.setOnFire(fireTicks);
        }
    }

    private boolean applyClientEntityUse(String playerId, String entityId, com.google.gson.JsonObject actorState,
            Entity target, ItemType heldItem) {
        if (target == null || target.isRemoved()) {
            return false;
        }
        if (target instanceof Sheep sheep) {
            if (heldItem == ItemType.SHEARS && sheep.shear()) {
                damageMultiplayerSelectedDurable(playerId, actorState, 1);
                return true;
            }
            int woolColor = Sheep.woolColorForDye(heldItem);
            if (woolColor >= 0 && sheep.dye(woolColor)) {
                consumeMultiplayerSelectedItem(playerId, actorState, heldItem, 1);
                return true;
            }
        }
        if (target instanceof Mooshroom mooshroom) {
            if (heldItem == ItemType.BOWL && applyClientMooshroomBowlUse(playerId, actorState, mooshroom)) {
                return true;
            }
            if (heldItem == ItemType.SHEARS && applyClientMooshroomShearUse(playerId, actorState, mooshroom)) {
                return true;
            }
        }
        if (target instanceof Cow cow && heldItem == ItemType.BUCKET
                && applyClientCowBucketUse(playerId, actorState, cow)) {
            return true;
        }
        if (target instanceof Mob mob && !(target instanceof Wolf) && heldItem != null
                && mob.feedBreedingItem(heldItem)) {
            consumeMultiplayerSelectedItem(playerId, actorState, heldItem, 1);
            return true;
        }
        if (target instanceof Pig pig) {
            if (heldItem == ItemType.SADDLE && !pig.isSaddled()) {
                if (pig.saddle()) {
                    consumeMultiplayerSelectedItem(playerId, actorState, ItemType.SADDLE, 1);
                    return true;
                }
                return false;
            }
            if (pig.isSaddled()) {
                return mountMultiplayerVehicle(playerId, entityId, pig);
            }
        }
        if (target instanceof Wolf wolf && applyClientWolfUse(playerId, actorState, wolf, heldItem)) {
            return true;
        }
        if (target instanceof BoatEntity boat) {
            return mountMultiplayerVehicle(playerId, entityId, boat);
        }
        if (target instanceof ChestMinecartEntity) {
            return true;
        }
        if (target instanceof MinecartEntity minecart
                && minecart.getKind() == MinecartEntity.CartKind.RIDEABLE) {
            return mountMultiplayerVehicle(playerId, entityId, minecart);
        }
        if (target instanceof FurnaceMinecartEntity furnaceCart) {
            float actorX = jsonFloat(actorState, "x", furnaceCart.getX());
            float actorZ = jsonFloat(actorState, "z", furnaceCart.getZ());
            if (isFurnaceMinecartFuel(heldItem)) {
                furnaceCart.addFuel(actorX, actorZ);
                consumeMultiplayerSelectedItem(playerId, actorState, heldItem, 1);
            } else {
                furnaceCart.setPushDirectionFrom(actorX, actorZ);
            }
            return true;
        }
        return false;
    }

    private boolean applyClientCowBucketUse(String playerId, com.google.gson.JsonObject actorState, Cow cow) {
        if (cow == null || cow.isRemoved() || cow.isBaby()) {
            return false;
        }
        return replaceMultiplayerHeldContainerItem(playerId, actorState, ItemType.BUCKET,
                new ItemStack(ItemType.MILK_BUCKET, 1));
    }

    private boolean applyClientMooshroomBowlUse(String playerId, com.google.gson.JsonObject actorState,
            Mooshroom mooshroom) {
        if (mooshroom == null || mooshroom.isRemoved() || mooshroom.isBaby()) {
            return false;
        }
        return replaceMultiplayerHeldContainerItem(playerId, actorState, ItemType.BOWL,
                new ItemStack(ItemType.MUSHROOM_STEW, 1));
    }

    private boolean applyClientMooshroomShearUse(String playerId, com.google.gson.JsonObject actorState,
            Mooshroom mooshroom) {
        if (world == null || mooshroom == null || mooshroom.isRemoved() || mooshroom.isBaby()) {
            return false;
        }
        Cow cow = new Cow();
        cow.setPosition(mooshroom.getX(), mooshroom.getY(), mooshroom.getZ());
        cow.setYaw(mooshroom.getYaw());
        cow.setPitch(mooshroom.getPitch());
        cow.setMotion(mooshroom.getMotionX(), mooshroom.getMotionY(), mooshroom.getMotionZ());
        cow.setHealth(mooshroom.getHealth());
        world.spawnEntity(cow);
        mooshroom.remove();
        world.spawnParticle(WorldParticle.Type.LARGE_EXPLOSION,
                mooshroom.getX(), mooshroom.getY() + mooshroom.getHeight() * 0.5f, mooshroom.getZ(),
                0.0f, 0.0f, 0.0f,
                Player.MOOSHROOM_SHEAR_PARTICLE_SCALE, Player.MOOSHROOM_SHEAR_PARTICLE_LIFETIME_TICKS);
        for (int i = 0; i < 5; i++) {
            world.spawnDroppedItem(mooshroom.getX(), mooshroom.getY() + mooshroom.getHeight() * 0.5f,
                    mooshroom.getZ(), ItemType.RED_MUSHROOM, 1);
        }
        damageMultiplayerSelectedDurable(playerId, actorState, 1);
        return true;
    }

    private boolean replaceMultiplayerHeldContainerItem(String playerId, com.google.gson.JsonObject actorState,
            ItemType expectedType, ItemStack replacement) {
        if (isCreativeMultiplayerPlayerState(actorState)) {
            return true;
        }
        return replaceOneMultiplayerSelectedItemWith(playerId, actorState, expectedType, replacement);
    }

    private void applyMultiplayerVehiclePlayerState(String playerId, NetworkMessage message) {
        if (playerId == null || playerId.isBlank() || message == null || message.data() == null) {
            return;
        }
        String entityId = messageString(message, "vehicle.entityId", "");
        boolean dismount = messageBoolean(message, "vehicle.dismount", false)
                || (message.data().has("vehicle.mounted")
                        && !messageBoolean(message, "vehicle.mounted", true));
        if (dismount) {
            dismountMultiplayerVehicle(playerId, entityId);
            return;
        }
        if (entityId.isBlank() || !messageBoolean(message, "vehicle.mounted", false)) {
            return;
        }
        Entity vehicle = multiplayerEntityById(entityId);
        if (!mountMultiplayerVehicle(playerId, entityId, vehicle)) {
            return;
        }
        applyMultiplayerVehicleInput(playerId, vehicle,
                messageFloat(message, "vehicle.yaw", messageFloat(message, "yaw", vehicle.getYaw())),
                clampUnit(messageFloat(message, "vehicle.forward", 0.0f)),
                clampUnit(messageFloat(message, "vehicle.strafe", 0.0f)));
        snapMultiplayerPlayerStateToVehicle(playerId, vehicle, message.data());
    }

    private boolean mountMultiplayerVehicle(String playerId, String entityId, Entity vehicle) {
        if (playerId == null || playerId.isBlank() || entityId == null || entityId.isBlank()
                || vehicle == null || vehicle.isRemoved()) {
            return false;
        }
        String currentEntityId = multiplayerVehicleEntityByPlayerId.get(playerId);
        if (entityId.equals(currentEntityId) && playerId.equals(multiplayerVehicleRidersByEntityId.get(entityId))) {
            return true;
        }
        if (multiplayerVehicleRidersByEntityId.containsKey(entityId)) {
            return false;
        }
        dismountMultiplayerVehicle(playerId, currentEntityId);
        if (vehicleHasPlayerPassenger(vehicle)) {
            return false;
        }
        boolean mounted = mountVehiclePassenger(vehicle);
        if (!mounted) {
            return false;
        }
        multiplayerVehicleRidersByEntityId.put(entityId, playerId);
        multiplayerVehicleEntityByPlayerId.put(playerId, entityId);
        return true;
    }

    private boolean mountVehiclePassenger(Entity vehicle) {
        if (vehicle instanceof BoatEntity boat) {
            return boat.mountPlayer();
        }
        if (vehicle instanceof MinecartEntity minecart && minecart.getKind() == MinecartEntity.CartKind.RIDEABLE) {
            return minecart.mountPlayer();
        }
        if (vehicle instanceof Pig pig && pig.isSaddled()) {
            return pig.mountPlayer();
        }
        return false;
    }

    private boolean vehicleHasPlayerPassenger(Entity vehicle) {
        if (vehicle instanceof BoatEntity boat) {
            return boat.hasPlayerPassenger();
        }
        if (vehicle instanceof MinecartEntity minecart) {
            return minecart.hasPlayerPassenger();
        }
        if (vehicle instanceof Pig pig) {
            return pig.hasPlayerPassenger();
        }
        return false;
    }

    private boolean isRideableVehicle(Entity vehicle) {
        return vehicle instanceof BoatEntity
                || vehicle instanceof Pig
                || (vehicle instanceof MinecartEntity minecart
                        && minecart.getKind() == MinecartEntity.CartKind.RIDEABLE);
    }

    private void dismountMultiplayerVehicle(String playerId, String entityId) {
        String mountedEntityId = entityId == null || entityId.isBlank()
                ? multiplayerVehicleEntityByPlayerId.get(playerId)
                : entityId;
        if (mountedEntityId == null || mountedEntityId.isBlank()) {
            return;
        }
        String rider = multiplayerVehicleRidersByEntityId.get(mountedEntityId);
        if (playerId != null && !playerId.isBlank() && rider != null && !playerId.equals(rider)) {
            return;
        }
        multiplayerVehicleRidersByEntityId.remove(mountedEntityId);
        if (rider != null) {
            multiplayerVehicleEntityByPlayerId.remove(rider);
        } else if (playerId != null) {
            multiplayerVehicleEntityByPlayerId.remove(playerId);
        }
        Entity vehicle = multiplayerEntityById(mountedEntityId);
        dismountVehiclePassenger(vehicle);
    }

    private void clearMultiplayerVehicleEntity(String entityId) {
        if (entityId == null || entityId.isBlank()) {
            return;
        }
        String rider = multiplayerVehicleRidersByEntityId.remove(entityId);
        if (rider != null) {
            multiplayerVehicleEntityByPlayerId.remove(rider);
        }
    }

    private void dismountVehiclePassenger(Entity vehicle) {
        if (vehicle instanceof BoatEntity boat) {
            boat.dismountPlayer();
        } else if (vehicle instanceof MinecartEntity minecart) {
            minecart.dismountPlayer();
        } else if (vehicle instanceof Pig pig) {
            pig.dismountPlayer();
        }
    }

    private void applyMultiplayerVehicleInput(String playerId, Entity vehicle, float yaw, float forward, float strafe) {
        if (vehicle == null || vehicle.isRemoved()) {
            return;
        }
        if (vehicle instanceof BoatEntity boat) {
            boat.applyRiderInput(yaw, forward, strafe);
        } else if (vehicle instanceof MinecartEntity minecart
                && minecart.getKind() == MinecartEntity.CartKind.RIDEABLE && forward > 0.0f) {
            minecart.applyRiderInput(yaw);
        }
    }

    private void snapMultiplayerPlayerStateToVehicle(String playerId, Entity vehicle, com.google.gson.JsonObject state) {
        if (vehicle == null || state == null) {
            return;
        }
        state.addProperty("x", vehicle.getX());
        state.addProperty("y", vehicle.getY() + 0.1f);
        state.addProperty("z", vehicle.getZ());
        state.addProperty("onGround", true);
        if (multiplayerServer == null || playerId == null || playerId.isBlank()) {
            return;
        }
        for (com.google.gson.JsonObject serverState : multiplayerServer.playerStates().values()) {
            if (serverState != null && playerId.equals(jsonString(serverState, "playerId", ""))) {
                serverState.addProperty("x", vehicle.getX());
                serverState.addProperty("y", vehicle.getY() + 0.1f);
                serverState.addProperty("z", vehicle.getZ());
                serverState.addProperty("onGround", true);
                break;
            }
        }
    }

    private float clampUnit(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 0.0f;
        }
        return Math.max(-1.0f, Math.min(1.0f, value));
    }

    private boolean applyClientWolfUse(String playerId, com.google.gson.JsonObject actorState,
            Wolf wolf, ItemType heldItem) {
        if (wolf == null || wolf.isRemoved()) {
            return false;
        }
        String username = multiplayerActorUsername(playerId, actorState);
        if (heldItem == ItemType.BONE && wolf.canAcceptBone()) {
            boolean tamed = wolf.tryTameWithBone(java.util.concurrent.ThreadLocalRandom.current());
            if (tamed) {
                wolf.setOwnerName(username);
            }
            consumeMultiplayerSelectedItem(playerId, actorState, ItemType.BONE, 1);
            return true;
        }
        if (heldItem != null && isHostedWolfOwnedBy(wolf, username) && wolf.feedMeat(heldItem)) {
            consumeMultiplayerSelectedItem(playerId, actorState, heldItem, 1);
            return true;
        }
        if (isHostedWolfOwnedBy(wolf, username)) {
            return wolf.toggleSitting();
        }
        return false;
    }

    private String multiplayerActorUsername(String playerId, com.google.gson.JsonObject actorState) {
        String username = jsonString(actorState, "username", "");
        if (username == null || username.isBlank()) {
            username = playerId == null || playerId.isBlank() ? "Player" : playerId;
        }
        return username.trim();
    }

    private boolean isHostedWolfOwnedBy(Wolf wolf, String username) {
        return wolf != null
                && wolf.isTamed()
                && wolf.hasOwner()
                && username != null
                && !username.isBlank()
                && username.equalsIgnoreCase(wolf.getOwnerName());
    }

    private boolean isFurnaceMinecartFuel(ItemType type) {
        return type == ItemType.COAL || type == ItemType.CHARCOAL;
    }

    private void broadcastMultiplayerEntityStateNow(Entity entity) {
        if (multiplayerServer == null || entity == null) {
            return;
        }
        String entityId = multiplayerEntityId(entity);
        if (!shouldSyncMultiplayerEntity(entity)) {
            multiplayerServer.broadcastEntityUpdate(entityId, "removed",
                    entity.getX(), entity.getY(), entity.getZ(), entity.getYaw(), entity.getPitch(),
                    Map.of("removed", "true"));
            lastMultiplayerEntityIds.remove(entityId);
            clearMultiplayerVehicleEntity(entityId);
            return;
        }
        multiplayerServer.broadcastEntityUpdate(
                entityId,
                multiplayerEntityType(entity),
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                entity.getYaw(),
                entity.getPitch(),
                multiplayerEntityData(entity, false));
    }

    private float jsonFloat(com.google.gson.JsonObject object, String key, float fallback) {
        try {
            float value = object != null && object.has(key) ? object.get(key).getAsFloat() : fallback;
            return Float.isFinite(value) ? value : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private int jsonInt(com.google.gson.JsonObject object, String key, int fallback) {
        try {
            return object != null && object.has(key) ? object.get(key).getAsInt() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private boolean jsonBoolean(com.google.gson.JsonObject object, String key, boolean fallback) {
        try {
            return object != null && object.has(key) ? object.get(key).getAsBoolean() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private String jsonString(com.google.gson.JsonObject object, String key, String fallback) {
        try {
            return object != null && object.has(key) ? object.get(key).getAsString() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private boolean actionTargetsLocalMultiplayerClient(NetworkMessage message) {
        if (multiplayerClient == null || !multiplayerClient.isConnected() || message.data() == null) {
            return false;
        }
        int targetClientId = message.data().has("clientId")
                ? message.data().get("clientId").getAsInt()
                : parseProtocolClientId(message.data().has("playerId") ? message.data().get("playerId").getAsString() : "");
        return targetClientId > 0 && targetClientId == multiplayerClient.clientId();
    }

    private void applyCommandPrivateMessage(NetworkMessage message) {
        String text = message.data().has("text") ? message.data().get("text").getAsString() : "";
        if (!text.isBlank()) {
            addChatMessage(text);
        }
    }

    private void applyRemoteServerCommand(NetworkMessage message) {
        if (message == null || message.data() == null) {
            return;
        }
        String text = messageString(message, "text", "");
        if (text.isBlank()) {
            return;
        }
        String sender = messageString(message, "sender", "Player");
        int clientId = messageInt(message, "clientId", parseProtocolClientId(messageString(message, "playerId", "")));
        commandDispatcher.execute(text, remoteCommandContext(sender, clientId));
    }

    private void applyCommandGive(NetworkMessage message) {
        if (player == null) {
            return;
        }
        ItemType item = commandActionItem(message);
        if (item == null) {
            return;
        }
        int count = Math.max(1, messageInt(message, "count", item.getMaxStackSize()));
        boolean fullyAdded = player.addStackToInventory(new ItemStack(item, count));
        addChatMessage("Gave " + count + " " + item.getDisplayName() + (fullyAdded ? "" : " (inventory full)"));
    }

    private void applyCommandTeleport(NetworkMessage message) {
        if (player == null) {
            return;
        }
        float x = messageFloat(message, "x", player.getPosition().x);
        float y = messageFloat(message, "y", player.getPosition().y);
        float z = messageFloat(message, "z", player.getPosition().z);
        player.setPosition(x, y, z);
        addChatMessage("Teleported to " + String.format(java.util.Locale.ROOT, "%.1f, %.1f, %.1f", x, y, z));
    }

    private void applyCommandKill() {
        killLocalPlayer();
        addChatMessage("Ouch. That looked like it hurt.");
    }

    private void applyCommandGameMode(NetworkMessage message) {
        GameMode mode = parseNetworkGameMode(messageString(message, "gameMode", currentGameMode.name()));
        currentGameMode = mode;
        currentHardcore = currentGameMode == GameMode.HARDCORE;
        if (player != null) {
            player.setGameMode(mode);
            player.setDifficulty(currentHardcore ? Difficulty.HARD : currentDifficulty);
        }
    }

    private void applyCommandExperience(NetworkMessage message) {
        if (player != null) {
            int amount = messageInt(message, "amount", 0);
            int previousLevel = player.getStats().getProgression().getLevel();
            player.getStats().getProgression().addExperience(amount);
            if (amount > 0 && messageBoolean(message, "pickup", false)) {
                player.onExperiencePickedUp();
                if (world != null) {
                    world.playExperiencePickupSound(player.getPosition().x, player.getEyeY(), player.getPosition().z);
                    if (player.getStats().getProgression().getLevel() > previousLevel) {
                        world.playExperienceLevelUpSound(player.getPosition().x, player.getEyeY(),
                                player.getPosition().z);
                    }
                }
            }
        }
    }

    private void applyCommandDamage(NetworkMessage message) {
        if (player == null) {
            return;
        }
        int fireTicks = Math.max(0, messageInt(message, "fireTicks", 0));
        float amount = Math.max(0.0f, messageFloat(message, "amount", 0.0f));
        boolean playerAttack = isCommandDamageType(message, "player", "player_attack");
        boolean changed = false;
        if (!playerAttack && fireTicks > 0) {
            player.setOnFire(fireTicks);
            changed = true;
        }
        if (amount > 0.0f) {
            boolean hurt = player.hurt(amount, commandDamageSource(message));
            if (hurt && playerAttack && fireTicks > 0 && !player.isDead()) {
                player.setOnFire(fireTicks);
            }
            changed = hurt || changed;
        }
        if (changed) {
            sendMultiplayerPlayerStateNow();
            syncMultiplayerInventoryStateNow();
        }
    }

    private void applyCommandVelocity(NetworkMessage message) {
        if (player == null) {
            return;
        }
        float motionX = clampedFiniteMessageFloat(message, "motionX", 0.0f, -16.0f, 16.0f);
        float motionY = clampedFiniteMessageFloat(message, "motionY", 0.0f, -16.0f, 16.0f);
        float motionZ = clampedFiniteMessageFloat(message, "motionZ", 0.0f, -16.0f, 16.0f);
        if (motionX == 0.0f && motionY == 0.0f && motionZ == 0.0f) {
            return;
        }
        player.addVelocity(motionX, motionY, motionZ);
        sendMultiplayerPlayerStateNow();
    }

    private boolean isCommandDamageType(NetworkMessage message, String... expectedTypes) {
        String rawType = messageString(message, "damageType", "generic")
                .trim()
                .toLowerCase(java.util.Locale.ROOT);
        for (String expected : expectedTypes) {
            if (rawType.equals(expected)) {
                return true;
            }
        }
        return false;
    }

    private void applyCommandPotionEffect(NetworkMessage message) {
        if (player == null) {
            return;
        }
        if (message.data() != null && message.data().has("effectType")) {
            StatusEffectInstance effect = statusEffectFromCommandMessage(message);
            if (effect != null) {
                player.getStats().addEffect(effect);
                sendMultiplayerPlayerStateNow();
                syncMultiplayerInventoryStateNow();
            }
            return;
        }
        float strength = Math.max(0.0f, Math.min(1.0f, messageFloat(message, "strength", 1.0f)));
        if (strength <= 0.0f) {
            return;
        }
        PotionEffectResolver.applyToPlayer(player, potionDataFromMessage(message), strength);
        sendMultiplayerPlayerStateNow();
        syncMultiplayerInventoryStateNow();
    }

    private StatusEffectInstance statusEffectFromCommandMessage(NetworkMessage message) {
        String rawType = messageString(message, "effectType", "");
        StatusEffectType type;
        try {
            type = StatusEffectType.valueOf(rawType.trim()
                    .replace('-', '_')
                    .replace(' ', '_')
                    .toUpperCase(java.util.Locale.ROOT));
        } catch (RuntimeException ignored) {
            return null;
        }
        int duration = Math.max(0, Math.min(messageInt(message, "duration", 0),
                MultiplayerProtocol.MAX_CLIENT_STATUS_EFFECT_DURATION));
        int amplifier = Math.max(0, Math.min(messageInt(message, "amplifier", 0),
                MultiplayerProtocol.MAX_CLIENT_STATUS_EFFECT_AMPLIFIER));
        return duration <= 0 ? null : new StatusEffectInstance(type, duration, amplifier);
    }

    private DamageSource commandDamageSource(NetworkMessage message) {
        String rawType = messageString(message, "damageType", "generic")
                .trim()
                .toLowerCase(java.util.Locale.ROOT);
        float sourceX = messageFloat(message, "sourceX", player == null ? 0.0f : player.getPosition().x);
        float sourceY = messageFloat(message, "sourceY", player == null ? 0.0f : player.getPosition().y);
        float sourceZ = messageFloat(message, "sourceZ", player == null ? 0.0f : player.getPosition().z);
        float horizontalKnockback = Math.max(0.0f, messageFloat(message, "horizontalKnockback", 0.0f));
        float verticalKnockback = Math.max(0.0f, messageFloat(message, "verticalKnockback", 0.0f));
        DamageSource source = switch (rawType) {
            case "arrow" -> DamageSource.point(DamageSource.Type.ARROW, sourceX, sourceY, sourceZ,
                    horizontalKnockback, verticalKnockback);
            case "drown" -> DamageSource.point(DamageSource.Type.DROWN, sourceX, sourceY, sourceZ,
                    horizontalKnockback, verticalKnockback);
            case "explosion" -> DamageSource.point(DamageSource.Type.EXPLOSION, sourceX, sourceY, sourceZ,
                    horizontalKnockback, verticalKnockback);
            case "fall" -> DamageSource.point(DamageSource.Type.FALL, sourceX, sourceY, sourceZ,
                    horizontalKnockback, verticalKnockback);
            case "fire", "lava" -> DamageSource.point(DamageSource.Type.FIRE, sourceX, sourceY, sourceZ,
                    horizontalKnockback, verticalKnockback);
            case "lightning" -> DamageSource.point(DamageSource.Type.LIGHTNING, sourceX, sourceY, sourceZ,
                    horizontalKnockback, verticalKnockback);
            case "magic" -> DamageSource.magic();
            case "mob", "mob_melee" -> DamageSource.point(DamageSource.Type.MOB_MELEE, sourceX, sourceY, sourceZ,
                    horizontalKnockback, verticalKnockback);
            case "player", "player_attack" -> DamageSource.point(DamageSource.Type.PLAYER_ATTACK,
                    sourceX, sourceY, sourceZ, horizontalKnockback, verticalKnockback);
            case "suffocation" -> DamageSource.suffocation(sourceX, sourceY, sourceZ);
            default -> DamageSource.point(DamageSource.Type.GENERIC, sourceX, sourceY, sourceZ,
                    horizontalKnockback, verticalKnockback);
        };
        String sourcePlayerId = sanitizeNetworkPlayerId(messageString(message, "sourcePlayerId", ""));
        return sourcePlayerId.isBlank() ? source : source.withPlayerCredit(true).withPlayerId(sourcePlayerId);
    }

    private void applyCommandClear(NetworkMessage message) {
        ItemType filter = message.data().has("itemId") ? commandActionItem(message) : null;
        clearLocalPlayerInventory(filter);
        addChatMessage(filter == null ? "Cleared inventory." : "Cleared " + filter.getDisplayName() + ".");
    }

    private void applyCommandSpawnpoint(NetworkMessage message) {
        if (player == null) {
            return;
        }
        float x = message.data().has("x") ? messageFloat(message, "x", player.getPosition().x) : player.getPosition().x;
        float y = message.data().has("y") ? messageFloat(message, "y", player.getPosition().y) : player.getPosition().y;
        float z = message.data().has("z") ? messageFloat(message, "z", player.getPosition().z) : player.getPosition().z;
        player.setSpawnPosition(x, y, z);
        addChatMessage("Set spawn point to " + String.format(java.util.Locale.ROOT, "%.1f, %.1f, %.1f", x, y, z));
    }

    private void applyNetworkPlayerList(NetworkMessage message) {
        if (message == null || message.data() == null || !message.data().has("players")
                || !message.data().get("players").isJsonArray()) {
            return;
        }
        multiplayerRoster.clear();
        com.google.gson.JsonArray players = message.data().getAsJsonArray("players");
        for (com.google.gson.JsonElement element : players) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            com.google.gson.JsonObject entry = element.getAsJsonObject();
            String playerId = entry.has("playerId") ? entry.get("playerId").getAsString() : "";
            if (playerId.isBlank() && entry.has("clientId")) {
                playerId = "player-" + entry.get("clientId").getAsInt();
            }
            String username = entry.has("username") ? entry.get("username").getAsString() : playerId;
            if (!playerId.isBlank() && username != null && !username.isBlank()) {
                int latencyMillis = entry.has("latencyMillis") ? entry.get("latencyMillis").getAsInt() : -1;
                multiplayerRoster.put(playerId, new MultiplayerRosterEntry(username, latencyMillis));
            }
        }
        reconcileRemotePlayersWithRoster();
    }

    private void reconcileRemotePlayersWithRoster() {
        for (Map.Entry<String, MultiplayerRosterEntry> entry : multiplayerRoster.entrySet()) {
            RemotePlayerView view = remotePlayers.get(entry.getKey());
            if (view != null) {
                view.player().setPlayerName(entry.getValue().username());
            }
        }
        remotePlayers.entrySet().removeIf(entry -> staleRosterRemotePlayer(entry.getKey()));
    }

    private boolean staleRosterRemotePlayer(String playerId) {
        int clientId = parseProtocolClientId(playerId);
        if (clientId <= 0) {
            return false;
        }
        if (multiplayerClient != null && clientId == multiplayerClient.clientId()) {
            return false;
        }
        return !multiplayerRoster.containsKey(playerId);
    }

    private void applyNetworkPlayerState(NetworkMessage message) {
        if (message.data() == null) {
            return;
        }
        if (!networkMessageTargetsCurrentDimension(message)) {
            return;
        }
        String playerId = message.data().has("playerId") ? message.data().get("playerId").getAsString() : "";
        if (playerId.isBlank()) {
            return;
        }
        int clientId = messageInt(message, "clientId", parseProtocolClientId(playerId));
        if (multiplayerServer != null) {
            applyMultiplayerVehiclePlayerState(playerId, message);
        }
        if (multiplayerClient != null && multiplayerClient.isConnected() && clientId == multiplayerClient.clientId()) {
            return;
        }
        String username = message.data().has("username") ? message.data().get("username").getAsString() : playerId;
        RemotePlayerView view = remotePlayers.computeIfAbsent(playerId, key -> new RemotePlayerView(username));
        view.player().setPlayerName(username);
        applyRemotePlayerMotionMetadata(view, message);
        view.player().applyRemotePose(
                messageFloat(message, "x", view.player().getPosition().x),
                messageFloat(message, "y", view.player().getPosition().y),
                messageFloat(message, "z", view.player().getPosition().z),
                messageFloat(message, "yaw", view.player().getCamera().getYaw()),
                messageFloat(message, "pitch", view.player().getCamera().getPitch()),
                message.data().has("onGround") && message.data().get("onGround").getAsBoolean());
        applyRemotePlayerMetadata(view, message);
    }

    private boolean disconnectTargetsLocalMultiplayerClient(NetworkMessage message) {
        if (!clientMultiplayerWorld || multiplayerClient == null || message.data() == null) {
            return false;
        }
        int targetClientId = message.data().has("clientId")
                ? message.data().get("clientId").getAsInt()
                : parseProtocolClientId(message.data().has("playerId") ? message.data().get("playerId").getAsString() : "");
        return targetClientId > 0 && targetClientId == multiplayerClient.clientId();
    }

    private void applyNetworkWorldMetadata(NetworkMessage message) {
        if (message == null || message.data() == null) {
            return;
        }
        Dimension previousDimension = world == null ? null : world.getDimension();
        Dimension networkDimension = previousDimension;
        if (message.data().has("dimension")) {
            networkDimension = Dimension.fromSaveName(messageString(message, "dimension",
                    previousDimension == null ? Dimension.OVERWORLD.getSaveName() : previousDimension.getSaveName()));
        }
        if (message.data().has("gameMode")) {
            currentGameMode = parseNetworkGameMode(messageString(message, "gameMode", currentGameMode.name()));
            currentHardcore = currentGameMode == GameMode.HARDCORE || messageBoolean(message, "hardcore", currentHardcore);
        }
        if (message.data().has("difficulty")) {
            currentDifficulty = currentHardcore ? Difficulty.HARD
                    : parseNetworkDifficulty(messageString(message, "difficulty", currentDifficulty.name()));
        }
        if (message.data().has("allowCheats")) {
            currentAllowCheats = messageBoolean(message, "allowCheats", currentAllowCheats);
        }
        if (message.data().has("spawnX") || message.data().has("spawnY") || message.data().has("spawnZ")) {
            worldSpawnX = messageInt(message, "spawnX", worldSpawnX);
            worldSpawnY = messageInt(message, "spawnY", worldSpawnY);
            worldSpawnZ = messageInt(message, "spawnZ", worldSpawnZ);
        }
        if (message.data().has("spawnNpcs")) {
            multiplayerSpawnNpcs = messageBoolean(message, "spawnNpcs", multiplayerSpawnNpcs);
            if (world != null) {
                world.setSpawnNpcs(multiplayerSpawnNpcs);
            }
        }
        if (message.data().has("viewDistance")) {
            multiplayerViewDistance = Math.max(MultiplayerProtocol.MIN_VIEW_DISTANCE,
                    Math.min(MultiplayerProtocol.MAX_VIEW_DISTANCE,
                            messageInt(message, "viewDistance", multiplayerViewDistance)));
            if (clientMultiplayerWorld && world != null) {
                world.setRenderDistanceChunks(effectiveWorldRenderDistanceChunks());
                applyNormalDistanceFog();
            }
        }
        if (message.data().has("maxBuildHeight")) {
            multiplayerMaxBuildHeight = clampServerMaxBuildHeight(
                    messageInt(message, "maxBuildHeight", multiplayerMaxBuildHeight));
        }
        if (message.data().has("generateStructures")) {
            multiplayerGenerateStructures = messageBoolean(message, "generateStructures",
                    multiplayerGenerateStructures);
        }
        if (clientMultiplayerWorld && previousDimension != null && networkDimension != null
                && networkDimension != previousDimension) {
            switchClientMultiplayerDimension(networkDimension);
        }
        if (message.data().has("spawnX") || message.data().has("spawnY") || message.data().has("spawnZ")) {
            if (world != null) {
                world.setWorldSpawn(worldSpawnX, worldSpawnY, worldSpawnZ);
            }
        }
        if (player != null) {
            player.setGameMode(currentGameMode);
            player.setDifficulty(currentDifficulty);
        }
    }

    private void switchClientMultiplayerDimension(Dimension dimension) {
        if (!clientMultiplayerWorld || multiplayerClient == null || player == null || dimension == null) {
            return;
        }
        if (world != null && world.getDimension() == dimension) {
            return;
        }

        World previousWorld = world;
        World nextWorld = new World(multiplayerClient.seed(), WorldGenerators.generatorIdFor(dimension), dimension,
                multiplayerGenerateStructures);
        nextWorld.setSpawnNpcs(multiplayerSpawnNpcs);
        installWorldNetworkHooks(nextWorld);
        try {
            nextWorld.init();
        } catch (Exception exception) {
            addChatMessage("Could not switch multiplayer dimension: " + exception.getMessage());
            return;
        }

        if (previousWorld != null) {
            for (Entity entity : remoteEntities.values()) {
                previousWorld.removeEntityNow(entity);
            }
            for (DroppedItem item : remoteDroppedItems.values()) {
                previousWorld.getDroppedItems().remove(item);
            }
            previousWorld.cleanup();
        }
        remotePlayers.clear();
        remoteEntities.clear();
        remoteDroppedItems.clear();
        deferredNetworkBlockUpdates.clear();
        multiplayerFishingHooks.clear();
        multiplayerVehicleRidersByEntityId.clear();
        multiplayerVehicleEntityByPlayerId.clear();
        multiplayerExperiencePickupCooldowns.clear();
        multiplayerRemoteDamageCooldowns.clear();
        multiplayerRemoteSprintKnockbackUsed.clear();
        multiplayerEntityIds.clear();
        multiplayerDroppedItemIds.clear();
        lastMultiplayerEntityIds.clear();
        lastMultiplayerDroppedItemIds.clear();

        world = nextWorld;
        world.setWorldSpawn(worldSpawnX, worldSpawnY, worldSpawnZ);
        world.setWeatherState(weatherState);
        world.setPlayer(player);
        player.setWorld(world);
        player.placeAfterDimensionTransfer(worldSpawnX + 0.5f, worldSpawnY, worldSpawnZ + 0.5f);
        player.setSpawnPosition(worldSpawnX + 0.5f, worldSpawnY, worldSpawnZ + 0.5f);
        player.setGameMode(currentGameMode);
        player.setDifficulty(currentDifficulty);
        world.setDayCycleManager(dayCycleManager);
        world.setRenderDistanceChunks(effectiveWorldRenderDistanceChunks());
        world.setFancyGraphics(settings.isFancyGraphics());
        world.setSmoothLighting(settings.isSmoothLighting());
        world.setAdvancedOpenGl(settings.isAdvancedOpenGl());
        survivalHudRenderer.setAtlas(world.getAtlas());
        inventoryRenderer.setAtlas(world.getAtlas());
        playerRenderer.setTextures(world.getAtlas(), com.craftzero.graphics.GuiTexture.getItemsTexture());
        mobSpawner = new MobSpawner(world);
        beginTerrainLoading(false);
    }

    private void applyRemotePlayerMetadata(RemotePlayerView view, NetworkMessage message) {
        if (view == null || message == null || message.data() == null) {
            return;
        }
        Player remote = view.player();
        applyRemotePlayerMotionMetadata(view, message);
        if (message.data().has("gameMode")) {
            remote.setGameMode(parseNetworkGameMode(messageString(message, "gameMode", "SURVIVAL")));
        }
        applyRemotePlayerUseMetadata(remote, message);
        applyRemotePlayerSurvivalState(remote, message);
        applyRemotePlayerProgression(remote, message);
        if (message.data().has("selectedSlot")) {
            remote.getInventory().setSelectedSlot(messageInt(message, "selectedSlot", remote.getInventory().getSelectedSlot()));
        }
        if (message.data().has("heldItemId") || message.data().has("heldItemCount")) {
            int selectedSlot = Math.max(0, Math.min(Inventory.HOTBAR_SIZE - 1, remote.getInventory().getSelectedSlot()));
            String itemId = messageString(message, "heldItemId", "air");
            int count = messageInt(message, "heldItemCount", 0);
            int damage = messageInt(message, "heldItemDamage", -1);
            remote.getInventory().getHotbar()[selectedSlot] = networkItemStack(itemId, count, damage);
        }
        if (message.data().has("armor.size")) {
            applyTileInventory(message, "armor", remote.getInventory().getArmor());
        }
        if (message.data().has("status.count")) {
            remote.getStats().setActiveEffects(statusEffectsFromMessage(message, "status"));
        }
    }

    private void applyRemotePlayerMotionMetadata(RemotePlayerView view, NetworkMessage message) {
        if (view == null || message == null || message.data() == null) {
            return;
        }
        Player remote = view.player();
        boolean hasInput = messageHasAny(message,
                "input.forward", "input.backward", "input.left", "input.right", "input.jumping",
                "input.sneaking", "remote.sprinting");
        if (hasInput) {
            remote.applyRemoteInputState(
                    messageBoolean(message, "input.forward", false),
                    messageBoolean(message, "input.backward", false),
                    messageBoolean(message, "input.left", false),
                    messageBoolean(message, "input.right", false),
                    messageBoolean(message, "input.jumping", false),
                    messageBoolean(message, "input.sneaking", messageBoolean(message, "sneaking", false)),
                    messageBoolean(message, "remote.sprinting", false));
        } else if (message.data().has("sneaking")) {
            remote.setRemoteSneaking(messageBoolean(message, "sneaking", false));
        }
        if (message.data().has("remote.sprinting") && !hasInput) {
            remote.setRemoteSprinting(messageBoolean(message, "remote.sprinting", false));
        }
    }

    private void applyRemotePlayerUseMetadata(Player remote, NetworkMessage message) {
        if (remote == null || message == null || message.data() == null
                || !messageHasAny(message, "remote.usingItem", "remote.blocking", "remote.drawingBow",
                        "remote.useProgress")) {
            return;
        }
        remote.applyRemoteUseState(
                messageBoolean(message, "remote.usingItem", false),
                messageBoolean(message, "remote.blocking", false),
                messageBoolean(message, "remote.drawingBow", false),
                messageFloat(message, "remote.useProgress", 0.0f));
    }

    private void applyRemotePlayerSurvivalState(Player remote, NetworkMessage message) {
        if (remote == null || message == null || message.data() == null || remote.getStats() == null) {
            return;
        }
        PlayerStats stats = remote.getStats();
        if (messageHasAny(message,
                "health",
                "stats.health",
                "stats.hunger",
                "stats.saturation",
                "stats.air",
                "stats.exhaustion")) {
            stats.restore(
                    messageFloat(message, "stats.health", messageFloat(message, "health", stats.getHealth())),
                    messageFloat(message, "stats.hunger", stats.getHunger()),
                    messageFloat(message, "stats.saturation", stats.getSaturation()),
                    messageFloat(message, "stats.air", stats.getCurrentAir()),
                    messageFloat(message, "stats.exhaustion", stats.getExhaustion()));
        }
        if (messageHasAny(message, "stats.onFire", "stats.fireTicks")) {
            int fireTicks = clampedMessageInt(message, "stats.fireTicks", remote.getFireTicks(), 0, 24_000);
            boolean onFire = messageBoolean(message, "stats.onFire", fireTicks > 0);
            if (onFire && fireTicks <= 0) {
                fireTicks = 80;
            }
            if (fireTicks > 0) {
                remote.setFireTicks(fireTicks);
            } else {
                remote.extinguish();
            }
        }
    }

    private void applyRemotePlayerProgression(Player remote, NetworkMessage message) {
        if (remote == null || message == null || message.data() == null || remote.getStats() == null) {
            return;
        }
        if (!messageHasAny(message, "progression.totalExperience", "progression.score")) {
            return;
        }
        PlayerProgression progression = remote.getStats().getProgression();
        progression.restore(
                clampedMessageInt(message, "progression.totalExperience", progression.getTotalExperience(),
                        0, 10_000_000),
                clampedMessageInt(message, "progression.score", progression.getScore(),
                        0, 10_000_000));
    }

    private GameMode parseNetworkGameMode(String value) {
        if (value == null || value.isBlank()) {
            return GameMode.SURVIVAL;
        }
        try {
            return GameMode.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return GameMode.SURVIVAL;
        }
    }

    private Difficulty parseNetworkDifficulty(String value) {
        if (value == null || value.isBlank()) {
            return Difficulty.EASY;
        }
        try {
            return Difficulty.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return Difficulty.EASY;
        }
    }

    private void removeRemotePlayer(NetworkMessage message) {
        if (message.data() == null) {
            return;
        }
        String playerId = message.data().has("playerId") ? message.data().get("playerId").getAsString() : "";
        if (!playerId.isBlank()) {
            dismountMultiplayerVehicle(playerId, "");
            multiplayerExperiencePickupCooldowns.remove(playerId);
            multiplayerRemoteDamageCooldowns.remove(playerId);
            remotePlayers.remove(playerId);
            multiplayerRoster.remove(playerId);
        }
    }

    private void applyNetworkEntityUpdate(NetworkMessage message) {
        if (world == null || message.data() == null) {
            return;
        }
        if (!networkMessageTargetsCurrentDimension(message)) {
            return;
        }
        String entityId = message.data().has("entityId") ? message.data().get("entityId").getAsString() : "";
        if (entityId.isBlank()) {
            return;
        }
        if (message.data().has("removed") && message.data().get("removed").getAsBoolean()) {
            DroppedItem removedItem = remoteDroppedItems.remove(entityId);
            if (removedItem != null) {
                world.getDroppedItems().remove(removedItem);
            }
            Entity removed = remoteEntities.remove(entityId);
            if (removed != null) {
                world.removeEntityNow(removed);
            }
            return;
        }

        String entityType = message.data().has("entityType")
                ? message.data().get("entityType").getAsString()
                : "";
        if (isDroppedItemEntityType(entityType)) {
            applyNetworkDroppedItemUpdate(entityId, message);
            return;
        }

        Entity entity = remoteEntities.get(entityId);
        if (entity != null && !remoteEntityMatchesType(entity, entityType)) {
            world.removeEntityNow(entity);
            remoteEntities.remove(entityId);
            entity = null;
        }
        if (entity == null) {
            entity = createRemoteEntity(message);
            if (entity == null) {
                return;
            }
            remoteEntities.put(entityId, entity);
            world.addEntityNow(entity);
        }

        float x = messageFloat(message, "x", entity.getX());
        float y = messageFloat(message, "y", entity.getY());
        float z = messageFloat(message, "z", entity.getZ());
        float yaw = messageFloat(message, "yaw", entity.getYaw());
        float pitch = messageFloat(message, "pitch", entity.getPitch());
        float motionX = messageFloat(message, "motionX", entity.getMotionX());
        float motionY = messageFloat(message, "motionY", entity.getMotionY());
        float motionZ = messageFloat(message, "motionZ", entity.getMotionZ());
        boolean onGround = !message.data().has("onGround") || message.data().get("onGround").getAsBoolean();
        entity.applyRemotePose(x, y, z, yaw, pitch, motionX, motionY, motionZ, onGround);
        if (message.data().has("age")) {
            entity.setTicksExisted(clampedMessageInt(message, "age", entity.getTicksExisted(), 0,
                    Integer.MAX_VALUE));
        }
        if (entity instanceof LivingEntity living) {
            living.setRenderBodyYaw(yaw);
            if (message.data().has("health")) {
                living.setHealth(messageFloat(message, "health", living.getHealth()));
            }
        }
        applyRemoteEntityExtraState(entity, message);
    }

    private void applyNetworkDroppedItemUpdate(String entityId, NetworkMessage message) {
        String itemId = message.data().has("itemId") ? message.data().get("itemId").getAsString() : "air";
        int count = messageInt(message, "count", 0);
        int damage = messageInt(message, "damage", -1);
        ItemStack stack = networkItemStack(itemId, count, damage);
        if (stack == null) {
            DroppedItem removed = remoteDroppedItems.remove(entityId);
            if (removed != null) {
                world.getDroppedItems().remove(removed);
            }
            return;
        }

        DroppedItem item = remoteDroppedItems.get(entityId);
        if (item == null || item.getItemType() != stack.getType() || item.getDurability() != stack.getDurability()) {
            if (item != null) {
                world.getDroppedItems().remove(item);
            }
            item = new DroppedItem(
                    messageFloat(message, "x", 0.0f),
                    messageFloat(message, "y", 0.0f),
                    messageFloat(message, "z", 0.0f),
                    stack,
                    messageFloat(message, "motionX", 0.0f),
                    messageFloat(message, "motionY", 0.0f),
                    messageFloat(message, "motionZ", 0.0f));
            item.attachToWorld(world);
            remoteDroppedItems.put(entityId, item);
            world.getDroppedItems().add(item);
        }

        float x = messageFloat(message, "x", item.getX());
        float y = messageFloat(message, "y", item.getY());
        float z = messageFloat(message, "z", item.getZ());
        item.moveBy(x - item.getX(), y - item.getY(), z - item.getZ());
        item.setVelocity(
                messageFloat(message, "motionX", item.getVelocityX()),
                messageFloat(message, "motionY", item.getVelocityY()),
                messageFloat(message, "motionZ", item.getVelocityZ()));
        item.setOnGround(!message.data().has("onGround") || message.data().get("onGround").getAsBoolean());
        item.setCount(count);
        item.setHealth(messageInt(message, "health", item.getHealth()));
        item.setAge(messageFloat(message, "age", item.getAge()));
        item.setAnimationState(
                messageFloat(message, "rotation", item.getRotation()),
                messageFloat(message, "bobPhase", item.getBobPhase()));
        item.setPickupDelayTicks(Integer.MAX_VALUE / 4);
    }

    private Entity createRemoteEntity(NetworkMessage message) {
        String entityType = message.data().has("entityType") ? message.data().get("entityType").getAsString() : "";
        String normalized = normalizeNetworkEntityType(entityType);
        float x = messageFloat(message, "x", 0.0f);
        float y = messageFloat(message, "y", 80.0f);
        float z = messageFloat(message, "z", 0.0f);
        float motionX = messageFloat(message, "motionX", 0.0f);
        float motionY = messageFloat(message, "motionY", 0.0f);
        float motionZ = messageFloat(message, "motionZ", 0.0f);

        MobDefinition definition = parseMobDefinition(entityType);
        if (definition != null) {
            if (definition == MobDefinition.SLIME) {
                return new Slime(normalizeNetworkSlimeSize(messageInt(message, "slimeSize", 4)));
            }
            if (definition == MobDefinition.MAGMA_CUBE) {
                return new MagmaCube(normalizeNetworkSlimeSize(messageInt(message, "slimeSize", 4)));
            }
            return MobFactory.create(definition);
        }
        return switch (normalized) {
            case "ARROW", "ARROWENTITY" -> new ArrowEntity(x, y, z, motionX, motionY, motionZ, null,
                    messageBoolean(message, "playerOwned", false), messageFloat(message, "damage", 2.0f));
            case "FIREBALL", "FIREBALLENTITY" -> new FireballEntity(x, y, z, motionX, motionY, motionZ, null,
                    messageBoolean(message, "explosive", true));
            case "FISHING_HOOK", "FISHINGHOOKENTITY" -> {
                FishingHookEntity hook = new FishingHookEntity(x, y, z, motionX, motionY, motionZ, null);
                hook.restoreFishingState(
                        messageInt(message, "waitTicks", 0),
                        messageInt(message, "catchableTicks", 0),
                        messageBoolean(message, "stuckInGround", false));
                hook.setRemoteOwnerPlayerId(sanitizeNetworkPlayerId(messageString(message,
                        "remoteOwnerPlayerId", messageString(message, "ownerPlayerId", ""))));
                hook.restoreHookedRemotePlayer(sanitizeNetworkPlayerId(messageString(message,
                        "hookedRemotePlayerId", "")));
                yield hook;
            }
            case "EYE_OF_ENDER", "EYEOFENDERENTITY" -> {
                EyeOfEnderEntity eye = new EyeOfEnderEntity(x, y, z,
                        messageFloat(message, "targetX", x + motionX * 20.0f),
                        messageFloat(message, "targetY", y + motionY * 20.0f),
                        messageFloat(message, "targetZ", z + motionZ * 20.0f),
                        messageBoolean(message, "dropsItem", true));
                eye.setMotion(motionX, motionY, motionZ);
                yield eye;
            }
            case "ENDER_PEARL", "ENDERPEARLENTITY" -> {
                EnderPearlEntity pearl = new EnderPearlEntity(x, y, z, motionX, motionY, motionZ, null);
                pearl.setRemoteOwnerPlayerId(sanitizeNetworkPlayerId(messageString(message,
                        "remoteOwnerPlayerId", "")));
                yield pearl;
            }
            case "SPLASH_POTION", "SPLASHPOTIONENTITY" -> {
                SplashPotionEntity potion = new SplashPotionEntity(x, y, z, motionX, motionY, motionZ,
                        null, potionDataFromMessage(message));
                potion.setPlayerOwned(messageBoolean(message, "playerOwned", false));
                yield potion;
            }
            case "EGG", "SNOWBALL", "THROWN_ITEM", "THROWNITEMENTITY" -> new ThrownItemEntity(x, y, z, motionX,
                    motionY, motionZ, parseNetworkItemType(messageString(message, "itemId",
                            "EGG".equals(normalized) ? "egg" : "snowball"), -1), null,
                    messageBoolean(message, "playerOwned", false));
            case "EXPERIENCE_ORB", "EXPERIENCEORBENTITY", "XP_ORB" -> {
                ExperienceOrbEntity orb = new ExperienceOrbEntity(x, y, z, messageInt(message, "value", 1));
                orb.setMotion(motionX, motionY, motionZ);
                yield orb;
            }
            case "END_CRYSTAL", "ENDCRYSTALENTITY", "END_CRYSTAL_ENTITY" -> new EndCrystalEntity(x, y, z);
            case "FALLING_BLOCK", "FALLINGBLOCKENTITY" -> {
                BlockType block = parseNetworkBlockType(messageString(message, "blockId", "12"));
                FallingBlockEntity falling = new FallingBlockEntity(block == null ? BlockType.SAND : block,
                        messageInt(message, "metadata", 0));
                falling.setPosition(x, y, z);
                falling.setMotion(motionX, motionY, motionZ);
                yield falling;
            }
            case "CHEST_MINECART", "CHESTMINECARTENTITY" -> new ChestMinecartEntity(x, y, z);
            case "FURNACE_MINECART", "FURNACEMINECARTENTITY" -> new FurnaceMinecartEntity(x, y, z);
            case "MINECART", "MINECARTENTITY" -> new MinecartEntity(x, y, z, MinecartEntity.CartKind.RIDEABLE);
            case "BOAT", "BOATENTITY" -> new BoatEntity(x, y, z);
            case "PAINTING", "PAINTINGENTITY" -> new PaintingEntity(x, y, z,
                    messageInt(message, "facing", com.craftzero.world.Block.FACE_NORTH),
                    PaintingEntity.Art.fromMotive(messageString(message, "art", "Kebab")));
            case "PRIMED_TNT", "PRIMEDTNTENTITY", "TNT" -> {
                PrimedTntEntity tnt = new PrimedTntEntity(x, y, z, messageInt(message, "fuseTicks", 80));
                tnt.setMotion(motionX, motionY, motionZ);
                tnt.setPlayerOwned(messageBoolean(message, "playerOwned", false));
                tnt.setRemoteOwnerPlayerId(sanitizeNetworkPlayerId(
                        messageString(message, "remoteOwnerPlayerId", tnt.getRemoteOwnerPlayerId())));
                yield tnt;
            }
            default -> null;
        };
    }

    private boolean remoteEntityMatchesType(Entity entity, String entityType) {
        String normalized = normalizeNetworkEntityType(entityType);
        if (entity instanceof Mob) {
            return parseMobDefinition(entityType) != null;
        }
        return switch (normalized) {
            case "ARROW", "ARROWENTITY" -> entity instanceof ArrowEntity;
            case "FIREBALL", "FIREBALLENTITY" -> entity instanceof FireballEntity;
            case "FISHING_HOOK", "FISHINGHOOKENTITY" -> entity instanceof FishingHookEntity;
            case "EYE_OF_ENDER", "EYEOFENDERENTITY" -> entity instanceof EyeOfEnderEntity;
            case "ENDER_PEARL", "ENDERPEARLENTITY" -> entity instanceof EnderPearlEntity;
            case "SPLASH_POTION", "SPLASHPOTIONENTITY" -> entity instanceof SplashPotionEntity;
            case "EGG", "SNOWBALL", "THROWN_ITEM", "THROWNITEMENTITY" -> entity instanceof ThrownItemEntity;
            case "EXPERIENCE_ORB", "EXPERIENCEORBENTITY", "XP_ORB" -> entity instanceof ExperienceOrbEntity;
            case "END_CRYSTAL", "ENDCRYSTALENTITY", "END_CRYSTAL_ENTITY" -> entity instanceof EndCrystalEntity;
            case "FALLING_BLOCK", "FALLINGBLOCKENTITY" -> entity instanceof FallingBlockEntity;
            case "CHEST_MINECART", "CHESTMINECARTENTITY" -> entity instanceof ChestMinecartEntity;
            case "FURNACE_MINECART", "FURNACEMINECARTENTITY" -> entity instanceof FurnaceMinecartEntity;
            case "MINECART", "MINECARTENTITY" -> entity instanceof MinecartEntity
                    && !(entity instanceof ChestMinecartEntity) && !(entity instanceof FurnaceMinecartEntity);
            case "BOAT", "BOATENTITY" -> entity instanceof BoatEntity;
            case "PAINTING", "PAINTINGENTITY" -> entity instanceof PaintingEntity;
            case "PRIMED_TNT", "PRIMEDTNTENTITY", "TNT" -> entity instanceof PrimedTntEntity;
            default -> false;
        };
    }

    private void applyRemoteEntityExtraState(Entity entity, NetworkMessage message) {
        if (entity instanceof LivingEntity living) {
            applyRemoteLivingEntityRuntimeState(living, message);
        }
        if (entity instanceof Mob mob) {
            applyRemoteMobAgeState(mob, message);
            applyRemoteSpecialMobState(mob, message);
        }
        if (entity instanceof ArrowEntity arrow) {
            arrow.setCritical(messageBoolean(message, "critical", arrow.isCritical()));
            applyRemoteProjectileShooterState(arrow, message);
            if (message.data().has("remoteShooterPlayerId")) {
                arrow.setRemoteShooterPlayerId(sanitizeNetworkPlayerId(
                        messageString(message, "remoteShooterPlayerId", arrow.getRemoteShooterPlayerId())));
            }
            arrow.setKnockback(
                    clampedFiniteMessageFloat(message, "knockbackHorizontal", arrow.getKnockbackHorizontal(),
                            0.0f, 16.0f),
                    clampedFiniteMessageFloat(message, "knockbackVertical", arrow.getKnockbackVertical(),
                            0.0f, 16.0f));
            arrow.setFireTicksOnHit(clampedMessageInt(message, "fireTicksOnHit", arrow.getFireTicksOnHit(),
                    0, 24_000));
            if (messageHasAny(message, "inGround", "stuckTicks", "blockX", "blockY", "blockZ")) {
                arrow.restoreStuckState(
                        messageBoolean(message, "inGround", arrow.isInGround()),
                        clampedMessageInt(message, "blockX", arrow.getBlockX(), -30_000_000, 30_000_000),
                        clampedMessageInt(message, "blockY", arrow.getBlockY(), 0, Chunk.HEIGHT - 1),
                        clampedMessageInt(message, "blockZ", arrow.getBlockZ(), -30_000_000, 30_000_000),
                        clampedMessageInt(message, "stuckTicks", arrow.getStuckTicks(),
                                0, ArrowEntity.STUCK_DESPAWN_TICKS - 1));
            }
        }
        if (entity instanceof FishingHookEntity hook) {
            hook.restoreFishingState(
                    messageInt(message, "waitTicks", hook.getWaitTicks()),
                    messageInt(message, "catchableTicks", hook.getCatchableTicks()),
                    messageBoolean(message, "stuckInGround", hook.isStuckInGround()));
            if (message.data().has("remoteOwnerPlayerId")) {
                hook.setRemoteOwnerPlayerId(sanitizeNetworkPlayerId(
                        messageString(message, "remoteOwnerPlayerId", hook.getRemoteOwnerPlayerId())));
            }
            applyRemoteFishingHookedEntityState(hook, message);
            applyRemoteFishingHookOwnerState(hook, message);
        }
        if (entity instanceof FireballEntity fireball) {
            applyRemoteProjectileShooterState(fireball, message);
            if (message.data().has("deflectedByPlayer")) {
                fireball.setDeflectedByPlayer(messageBoolean(message, "deflectedByPlayer",
                        fireball.isDeflectedByPlayer()));
            }
            if (message.data().has("remoteDeflectorPlayerId")) {
                fireball.setRemoteDeflectorPlayerId(sanitizeNetworkPlayerId(
                        messageString(message, "remoteDeflectorPlayerId", fireball.getRemoteDeflectorPlayerId())));
            }
        }
        if (entity instanceof ThrownItemEntity thrown) {
            applyRemoteProjectileShooterState(thrown, message);
            if (message.data().has("remoteShooterPlayerId")) {
                thrown.setRemoteShooterPlayerId(sanitizeNetworkPlayerId(
                        messageString(message, "remoteShooterPlayerId", thrown.getRemoteShooterPlayerId())));
            }
        }
        if (entity instanceof SplashPotionEntity potion) {
            applyRemoteProjectileShooterState(potion, message);
            potion.setPlayerOwned(messageBoolean(message, "playerOwned", potion.isPlayerOwned()));
            if (message.data().has("remoteShooterPlayerId")) {
                potion.setRemoteShooterPlayerId(sanitizeNetworkPlayerId(
                        messageString(message, "remoteShooterPlayerId", potion.getRemoteShooterPlayerId())));
            }
        }
        if (entity instanceof EnderPearlEntity pearl && message.data().has("remoteOwnerPlayerId")) {
            pearl.setRemoteOwnerPlayerId(sanitizeNetworkPlayerId(
                    messageString(message, "remoteOwnerPlayerId", pearl.getRemoteOwnerPlayerId())));
        }
        if (entity instanceof Sheep sheep) {
            sheep.setSheared(messageBoolean(message, "sheared", sheep.isSheared()));
            sheep.setWoolColor(messageInt(message, "woolColor", sheep.getWoolColor()));
            sheep.setEatingGrassTimer(messageInt(message, "eatingGrassTimer", sheep.getEatingGrassTimer()));
        }
        if (entity instanceof Pig pig) {
            pig.setSaddled(messageBoolean(message, "saddled", pig.isSaddled()));
        }
        if (entity instanceof Wolf wolf) {
            if (message.data().has("tamed")) {
                wolf.setTamed(messageBoolean(message, "tamed", wolf.isTamed()));
            }
            if (message.data().has("ownerName")) {
                wolf.setOwnerName(messageString(message, "ownerName", wolf.getOwnerName()));
            }
            wolf.setSitting(messageBoolean(message, "sitting", wolf.isSitting()));
            wolf.setAngry(messageBoolean(message, "angry", wolf.isAngry()));
        }
        applyRemoteVehiclePassengerState(entity, message);
        if (entity instanceof FurnaceMinecartEntity furnaceCart) {
            furnaceCart.setFuelTicks(messageInt(message, "fuelTicks", furnaceCart.getFuelTicks()));
            furnaceCart.setPush(
                    messageFloat(message, "pushX", furnaceCart.getPushX()),
                    messageFloat(message, "pushZ", furnaceCart.getPushZ()));
        }
        if (entity instanceof ChestMinecartEntity chestMinecart) {
            applyTileInventory(message, "entity.inventory", chestMinecart.getInventory());
        }
        if (entity instanceof MinecartEntity minecart && message.data().has("damage")) {
            applyRemoteMinecartRuntimeState(minecart, message);
        }
        if (entity instanceof BoatEntity boat && messageHasAny(message, "damage", "rollingAmplitude",
                "rollingDirection")) {
            boat.setDamage(clampedFiniteMessageFloat(message, "damage", boat.getDamage(), 0.0f, 1024.0f));
            boat.restoreRollingState(
                    clampedMessageInt(message, "rollingAmplitude", boat.getRollingAmplitude(),
                            0, BoatEntity.HIT_ROLLING_TICKS),
                    messageInt(message, "rollingDirection", boat.getRollingDirection()));
        }
        if (entity instanceof ExperienceOrbEntity orb) {
            orb.setPickupDelayTicks(clampedMessageInt(message, "pickupDelayTicks", orb.getPickupDelayTicks(),
                    0, 32_767));
            orb.setHealth(clampedMessageInt(message, "orbHealth", orb.getHealth(),
                    1, ExperienceOrbEntity.MAX_HEALTH));
        }
        if (entity instanceof PrimedTntEntity tnt) {
            tnt.setFuseTicks(messageInt(message, "fuseTicks", tnt.getFuseTicks()));
            tnt.setPlayerOwned(messageBoolean(message, "playerOwned", tnt.isPlayerOwned()));
            if (message.data().has("remoteOwnerPlayerId")) {
                tnt.setRemoteOwnerPlayerId(sanitizeNetworkPlayerId(
                        messageString(message, "remoteOwnerPlayerId", tnt.getRemoteOwnerPlayerId())));
            }
        }
    }

    private void applyRemoteMinecartRuntimeState(MinecartEntity minecart, NetworkMessage message) {
        if (minecart == null || message == null || message.data() == null) {
            return;
        }
        minecart.setDamage(clampedFiniteMessageFloat(message, "damage", minecart.getDamage(), 0.0f, 1024.0f));
        minecart.restoreRollingState(
                clampedMessageInt(message, "rollingAmplitude", minecart.getRollingAmplitude(),
                        0, MinecartEntity.HIT_ROLLING_TICKS),
                messageInt(message, "rollingDirection", minecart.getRollingDirection()));
    }

    private void applyRemoteLivingEntityRuntimeState(LivingEntity living, NetworkMessage message) {
        if (living == null || message == null || message.data() == null) {
            return;
        }
        if (message.data().has("effects.count")) {
            living.setActiveEffects(statusEffectsFromMessage(message, "effects"));
        }
        if (message.data().has("fireTicks")) {
            living.setFireTicks(clampedMessageInt(message, "fireTicks", living.getFireTicks(), 0, 24_000));
        }
        if (messageHasAny(message, "hurtTime", "invulnerableTime", "lastDamageAmount",
                "recentPlayerHitTicks", "recentPlayerLootingLevel")) {
            living.restoreDamageState(
                    clampedMessageInt(message, "hurtTime", living.getHurtTime(), 0, LivingEntity.MAX_HURT_TIME),
                    clampedMessageInt(message, "invulnerableTime", living.getInvulnerableTime(),
                            0, LivingEntity.MAX_INVULNERABLE_TIME),
                    clampedFiniteMessageFloat(message, "lastDamageAmount", living.getLastDamageAmount(),
                            0.0f, 1024.0f),
                    clampedMessageInt(message, "recentPlayerHitTicks", living.getRecentPlayerHitTicks(),
                            0, LivingEntity.RECENT_PLAYER_HIT_TICKS),
                    clampedMessageInt(message, "recentPlayerLootingLevel", living.getRecentPlayerLootingLevel(),
                            0, 255));
        }
        if (messageHasAny(message, "dead", "deathTime")) {
            living.restoreDeathAnimationState(
                    messageBoolean(message, "dead", living.isDead()),
                    clampedMessageInt(message, "deathTime", living.getDeathTime(), 0, 20));
        }
    }

    private void applyRemoteMobAgeState(Mob mob, NetworkMessage message) {
        if (mob == null || message == null || message.data() == null) {
            return;
        }
        if (message.data().has("growingAge")) {
            mob.setGrowingAge(clampNetworkMobGrowingAge(messageInt(message, "growingAge", mob.getGrowingAge())));
        }
        if (message.data().has("loveTicks")) {
            int loveTicks = Math.max(0, Math.min(Mob.LOVE_MODE_TICKS,
                    messageInt(message, "loveTicks", mob.getLoveTicks())));
            if (mob.getGrowingAge() != 0 || mob.isBaby()) {
                loveTicks = 0;
            }
            mob.setLoveTicks(loveTicks);
        }
    }

    private int clampNetworkMobGrowingAge(int growingAge) {
        return Math.max(Mob.BABY_GROWING_AGE, Math.min(Mob.BREEDING_COOLDOWN_AGE, growingAge));
    }

    private void applyRemoteSpecialMobState(Mob mob, NetworkMessage message) {
        if (mob == null || message == null || message.data() == null) {
            return;
        }
        if (mob instanceof Slime slime) {
            slime.setJumpDelay(clampedMessageInt(message, "jumpDelay", slime.getJumpDelay(), 0, 2400));
        }
        if (mob instanceof Chicken chicken) {
            chicken.setEggTimer(clampedMessageInt(message, "eggTimer", chicken.getEggTimer(), 0, 12_000));
        }
        if (mob instanceof Skeleton skeleton && messageHasAny(message, "rangedAttackActive",
                "rangedAttackCooldown", "rangedStrafeTime", "rangedStrafingClockwise", "rangedStrafeSpeed")) {
            RangedAttackGoal.State current = skeleton.getRangedAttackState();
            skeleton.restoreRangedAttackState(new RangedAttackGoal.State(
                    clampedMessageInt(message, "rangedAttackCooldown", current.attackCooldown(), 0, 1200),
                    clampedMessageInt(message, "rangedStrafeTime", current.strafeTime(), 0, 1200),
                    messageBoolean(message, "rangedStrafingClockwise", current.strafingClockwise()),
                    clampedFiniteMessageFloat(message, "rangedStrafeSpeed", current.strafeSpeed(), 0.0f, 4.0f)),
                    messageBoolean(message, "rangedAttackActive", skeleton.isRangedAttackActive()));
        }
        if (mob instanceof SnowGolem snowGolem) {
            snowGolem.setSnowballAttackCooldown(clampedMessageInt(message, "snowGolemAttackCooldown",
                    snowGolem.getSnowballAttackCooldown(), 0, 1200));
        }
        if (mob instanceof Blaze blaze && messageHasAny(message, "blazeAttackCooldown", "burstShots",
                "burstCooldown")) {
            blaze.setAttackState(
                    clampedMessageInt(message, "blazeAttackCooldown", blaze.getAttackCooldown(), 0, 2400),
                    clampedMessageInt(message, "burstShots", blaze.getBurstShots(), 0, 8),
                    clampedMessageInt(message, "burstCooldown", blaze.getBurstCooldown(), 0, 2400));
        }
        if (mob instanceof Ghast ghast && messageHasAny(message, "fireCooldown", "ghastAttackCharge",
                "wanderCooldown", "targetX", "targetY", "targetZ")) {
            ghast.setFlightState(
                    clampedMessageInt(message, "fireCooldown", ghast.getFireCooldown(), 0, 2400),
                    clampedMessageInt(message, "ghastAttackCharge", ghast.getAttackCharge(), 0, 2400),
                    clampedMessageInt(message, "wanderCooldown", ghast.getWanderCooldown(), 0, 2400),
                    clampedFiniteMessageFloat(message, "targetX", ghast.getTargetX(), -30_000_000.0f,
                            30_000_000.0f),
                    clampedFiniteMessageFloat(message, "targetY", ghast.getTargetY(), -1024.0f, 1024.0f),
                    clampedFiniteMessageFloat(message, "targetZ", ghast.getTargetZ(), -30_000_000.0f,
                            30_000_000.0f));
        }
        if (mob instanceof Squid squid && messageHasAny(message, "swimTimer", "airTicks", "swimX", "swimY",
                "swimZ", "squidPitch", "prevSquidPitch", "squidYaw", "prevSquidYaw", "squidRotation",
                "prevSquidRotation", "tentacleAngle", "prevTentacleAngle")) {
            squid.setSwimState(
                    clampedMessageInt(message, "swimTimer", squid.getSwimTimer(), 0, 2400),
                    clampedMessageInt(message, "airTicks", squid.getAirTicks(), LivingEntity.DROWN_DAMAGE_AIR_TICKS,
                            LivingEntity.MAX_AIR_TICKS),
                    clampedFiniteMessageFloat(message, "swimX", squid.getSwimX(), -4.0f, 4.0f),
                    clampedFiniteMessageFloat(message, "swimY", squid.getSwimY(), -4.0f, 4.0f),
                    clampedFiniteMessageFloat(message, "swimZ", squid.getSwimZ(), -4.0f, 4.0f),
                    clampedFiniteMessageFloat(message, "squidPitch", squid.getSquidPitch(), -720.0f, 720.0f),
                    clampedFiniteMessageFloat(message, "prevSquidPitch", squid.getPrevSquidPitch(), -720.0f,
                            720.0f),
                    clampedFiniteMessageFloat(message, "squidYaw", squid.getSquidYaw(), -720.0f, 720.0f),
                    clampedFiniteMessageFloat(message, "prevSquidYaw", squid.getPrevSquidYaw(), -720.0f, 720.0f),
                    clampedFiniteMessageFloat(message, "squidRotation", squid.getSquidRotation(), -720.0f,
                            720.0f),
                    clampedFiniteMessageFloat(message, "prevSquidRotation", squid.getPrevSquidRotation(), -720.0f,
                            720.0f),
                    clampedFiniteMessageFloat(message, "tentacleAngle", squid.getTentacleAngle(), -720.0f, 720.0f),
                    clampedFiniteMessageFloat(message, "prevTentacleAngle", squid.getPrevTentacleAngle(), -720.0f,
                            720.0f));
        }
        if (mob instanceof EnderDragon dragon && messageHasAny(message, "targetX", "targetY", "targetZ",
                "targetCooldown", "dragonDeathTicks", "dragonDeathStarted")) {
            dragon.setFlightState(
                    clampedFiniteMessageFloat(message, "targetX", dragon.getTargetX(), -30_000_000.0f,
                            30_000_000.0f),
                    clampedFiniteMessageFloat(message, "targetY", dragon.getTargetY(), -1024.0f, 1024.0f),
                    clampedFiniteMessageFloat(message, "targetZ", dragon.getTargetZ(), -30_000_000.0f,
                            30_000_000.0f),
                    clampedMessageInt(message, "targetCooldown", dragon.getTargetCooldown(), 0, 2400));
            dragon.setDeathState(
                    clampedMessageInt(message, "dragonDeathTicks", dragon.getDeathTicks(), 0, 400),
                    messageBoolean(message, "dragonDeathStarted", dragon.isDead()));
        }
        if (mob instanceof Creeper creeper) {
            creeper.setFuseState(
                    clampedMessageInt(message, "creeperFuseTicks", creeper.getFuseTime(), 0,
                            creeper.getMaxFuseTime()),
                    messageBoolean(message, "ignited", creeper.isIgnited()));
            creeper.setPowered(messageBoolean(message, "creeperPowered", creeper.isPowered()));
        }
        if (mob instanceof Wolf wolf && messageHasAny(message, "wolfWet", "wolfShaking", "wolfShakeTime",
                "wolfPrevShakeTime")) {
            wolf.setWetShakeState(
                    messageBoolean(message, "wolfWet", wolf.isWet()),
                    messageBoolean(message, "wolfShaking", wolf.isShaking()),
                    clampedFiniteMessageFloat(message, "wolfShakeTime", wolf.getShakeTime(), 0.0f, 2.05f),
                    clampedFiniteMessageFloat(message, "wolfPrevShakeTime", wolf.getPrevShakeTime(), 0.0f, 2.05f));
        }
        if (mob instanceof Enderman enderman && messageHasAny(message, "carriedBlockId", "carriedMetadata",
                "endermanAngry", "stareTicks", "teleportCooldown")) {
            BlockType carried = parseNetworkBlockType(messageString(message, "carriedBlockId",
                    Integer.toString(enderman.getCarriedBlock().getId())));
            enderman.setCarriedBlock(carried == null ? BlockType.AIR : carried,
                    clampedMessageInt(message, "carriedMetadata", enderman.getCarriedMetadata(), 0, 15));
            enderman.setAngry(messageBoolean(message, "endermanAngry", enderman.isAngry()));
            enderman.setAttentionState(
                    clampedMessageInt(message, "stareTicks", enderman.getStareTicks(), 0, 2400),
                    clampedMessageInt(message, "teleportCooldown", enderman.getTeleportCooldown(), 0, 2400));
        }
        if (mob instanceof ZombiePigman pigman) {
            pigman.setAngerTicks(clampedMessageInt(message, "angerTicks", pigman.getAngerTicks(), 0, 2400));
        }
        if (mob instanceof Spider spider) {
            spider.setProvoked(messageBoolean(message, "spiderProvoked", spider.isProvoked()));
        }
        if (mob instanceof Villager villager) {
            villager.setProfession(clampedMessageInt(message, "profession", villager.getProfession(),
                    Villager.PROFESSION_FARMER, Villager.PROFESSION_BUTCHER));
        }
    }

    private void applyRemoteFishingHookOwnerState(FishingHookEntity hook, NetworkMessage message) {
        if (hook == null || message == null || message.data() == null) {
            return;
        }
        String ownerPlayerId = messageString(message, "ownerPlayerId", "");
        if (!ownerPlayerId.isBlank()) {
            hook.setRemoteOwnerPlayerId(sanitizeNetworkPlayerId(ownerPlayerId));
        }
        if (message.data().has("ownerX") || message.data().has("ownerY") || message.data().has("ownerZ")) {
            hook.restoreOwnerSnapshot(new FishingHookEntity.OwnerSnapshot(
                    messageFloat(message, "ownerX", hook.getX()),
                    messageFloat(message, "ownerY", hook.getY()),
                    messageFloat(message, "ownerZ", hook.getZ()),
                    messageBoolean(message, "ownerAlive", true),
                    messageBoolean(message, "ownerHoldingRod", true),
                    messageFloat(message, "ownerYaw", hook.getYaw()),
                    messageBoolean(message, "ownerSneaking", false)));
        }
        if (!clientMultiplayerWorld || multiplayerClient == null || player == null || world == null
                || ownerPlayerId.isBlank() || !ownerPlayerId.equals(localMultiplayerPlayerId())) {
            return;
        }
        FishingHookEntity currentHook = player.getFishingHook();
        if (currentHook != null && currentHook != hook) {
            world.removeEntityNow(currentHook);
        }
        player.attachFishingHook(hook);
    }

    private void applyRemoteFishingHookedEntityState(FishingHookEntity hook, NetworkMessage message) {
        if (hook == null || message == null || message.data() == null
                || (!message.data().has("hookedEntityId") && !message.data().has("hookedRemotePlayerId"))) {
            return;
        }
        if (message.data().has("hookedEntityId")) {
            String hookedEntityId = messageString(message, "hookedEntityId", "");
            if (hookedEntityId.isBlank()) {
                hook.restoreHookedEntity(null);
            } else {
                Entity hooked = remoteEntities.get(hookedEntityId);
                if (hooked != null && !hooked.isRemoved()) {
                    hook.restoreHookedEntity(hooked);
                }
            }
        }
        if (message.data().has("hookedRemotePlayerId")) {
            hook.restoreHookedRemotePlayer(sanitizeNetworkPlayerId(
                    messageString(message, "hookedRemotePlayerId", hook.getHookedRemotePlayerId())));
        }
    }

    private void applyRemoteProjectileShooterState(Entity projectile, NetworkMessage message) {
        if (projectile == null || message == null || message.data() == null || !message.data().has("shooterEntityId")) {
            return;
        }
        String shooterEntityId = messageString(message, "shooterEntityId", "");
        if (shooterEntityId.isBlank()) {
            restoreProjectileShooter(projectile, null);
            return;
        }
        Entity shooter = remoteEntities.get(shooterEntityId);
        if (shooter != null && !shooter.isRemoved()) {
            restoreProjectileShooter(projectile, shooter);
        }
    }

    private void restoreProjectileShooter(Entity projectile, Entity shooter) {
        if (projectile instanceof ArrowEntity arrow) {
            arrow.restoreShooter(shooter);
        } else if (projectile instanceof FireballEntity fireball) {
            fireball.restoreShooter(shooter);
        } else if (projectile instanceof ThrownItemEntity thrown) {
            thrown.restoreShooter(shooter);
        } else if (projectile instanceof SplashPotionEntity potion) {
            potion.restoreShooter(shooter);
        }
    }

    private void applyRemoteVehiclePassengerState(Entity entity, NetworkMessage message) {
        if (entity == null || message == null || message.data() == null || !isRideableVehicle(entity)) {
            return;
        }
        String riderPlayerId = messageString(message, "riderPlayerId", "");
        if (riderPlayerId.isBlank()) {
            dismountVehiclePassenger(entity);
            return;
        }
        if (clientMultiplayerWorld && player != null && riderPlayerId.equals(localMultiplayerPlayerId())) {
            if (!isPlayerRidingVehicle(entity)) {
                dismountVehiclePassenger(entity);
                player.restoreVehicleMount(entity);
            }
            return;
        }
        if (isPlayerRidingVehicle(entity)) {
            dismountLocalPlayerFromVehicle(entity);
        }
        dismountVehiclePassenger(entity);
        mountVehiclePassenger(entity);
    }

    private boolean isPlayerRidingVehicle(Entity vehicle) {
        return player != null
                && (player.getRidingBoat() == vehicle
                        || player.getRidingMinecart() == vehicle
                        || player.getRidingPig() == vehicle);
    }

    private void dismountLocalPlayerFromVehicle(Entity vehicle) {
        if (player == null || vehicle == null) {
            return;
        }
        if (player.getRidingBoat() == vehicle) {
            player.dismountBoat();
        } else if (player.getRidingMinecart() == vehicle) {
            player.dismountMinecart();
        } else if (player.getRidingPig() == vehicle) {
            player.dismountPig();
        }
    }

    private String localMultiplayerPlayerId() {
        return multiplayerClient != null && multiplayerClient.clientId() > 0
                ? "player-" + multiplayerClient.clientId()
                : "";
    }

    private String sanitizeNetworkPlayerId(String playerId) {
        if (playerId == null) {
            return "";
        }
        String trimmed = playerId.trim();
        if ("host".equalsIgnoreCase(trimmed)) {
            return "host";
        }
        int clientId = parseProtocolClientId(trimmed);
        return clientId > 0 ? "player-" + clientId : "";
    }

    private String multiplayerProjectilePlayerId(String playerId, boolean hostPlayerOwned) {
        String sanitized = sanitizeNetworkPlayerId(playerId);
        if (!sanitized.isBlank()) {
            return sanitized;
        }
        return hostPlayerOwned ? "host" : "";
    }

    private String multiplayerFireballDeflectorPlayerId(FireballEntity fireball) {
        if (fireball == null || !fireball.isDeflectedByPlayer()) {
            return "";
        }
        return multiplayerProjectilePlayerId(fireball.getRemoteDeflectorPlayerId(), true);
    }

    private void configurePlayerActionHandler() {
        if (player == null) {
            return;
        }
        player.setPlayerActionHandler(hasMultiplayerPlayerActionTargets() ? multiplayerPlayerActionHandler : null);
    }

    private void configurePlayerDeathDropHandler() {
        if (player == null) {
            multiplayerDeathDropStateSynced = false;
            multiplayerRespawnRequestPending = false;
            return;
        }
        if (!player.isDead()) {
            multiplayerDeathDropStateSynced = false;
            multiplayerRespawnRequestPending = false;
        }
        player.setDeathDropHandler(shouldSendClientMultiplayerDrops() ? multiplayerDeathDropHandler : null);
    }

    private boolean hasMultiplayerPlayerActionTargets() {
        return multiplayerServer != null
                || (clientMultiplayerWorld && multiplayerClient != null && multiplayerClient.isConnected());
    }

    private Player.PlayerHit findMultiplayerPlayerAttackTarget(org.joml.Vector3f origin,
            org.joml.Vector3f direction, float maxDistance) {
        if (origin == null || direction == null || maxDistance <= 0.0f) {
            return null;
        }
        if (multiplayerServer != null && !clientMultiplayerWorld) {
            return findHostedRemotePlayerAttackTarget(origin, direction, maxDistance);
        }
        if (clientMultiplayerWorld && multiplayerClient != null && multiplayerClient.isConnected()) {
            return findClientRemotePlayerAttackTarget(origin, direction, maxDistance);
        }
        return null;
    }

    private Player.PlayerHit findHostedRemotePlayerAttackTarget(org.joml.Vector3f origin,
            org.joml.Vector3f direction, float maxDistance) {
        if (!multiplayerPvp) {
            return null;
        }
        String closestPlayerId = "";
        float closestDistance = maxDistance;
        org.joml.Vector3f closestPoint = null;
        for (com.google.gson.JsonObject state : multiplayerServer.playerStates().values()) {
            if (!isAttackableMultiplayerPlayerState(state)) {
                continue;
            }
            String playerId = jsonString(state, "playerId", "");
            if (parseProtocolClientId(playerId) <= 0) {
                continue;
            }
            AABB box = Raycast.playerPickBox(multiplayerRemotePlayerBox(
                    jsonFloat(state, "x", 0.0f),
                    jsonFloat(state, "y", 80.0f),
                    jsonFloat(state, "z", 0.0f)));
            float distance = Raycast.intersectsAabb(origin, direction, box);
            if (distance >= 0.0f && distance < closestDistance) {
                closestDistance = distance;
                closestPlayerId = playerId;
                closestPoint = Raycast.pointAt(origin, direction, distance);
            }
        }
        return closestPoint == null ? null : new Player.PlayerHit(closestPlayerId, closestDistance, closestPoint);
    }

    private Player.PlayerHit findClientRemotePlayerAttackTarget(org.joml.Vector3f origin,
            org.joml.Vector3f direction, float maxDistance) {
        if (!multiplayerPvp) {
            return null;
        }
        String localPlayerId = localMultiplayerPlayerId();
        String closestPlayerId = "";
        float closestDistance = maxDistance;
        org.joml.Vector3f closestPoint = null;
        for (Map.Entry<String, RemotePlayerView> entry : remotePlayers.entrySet()) {
            String playerId = entry.getKey();
            if (playerId == null || playerId.isBlank() || playerId.equals(localPlayerId)) {
                continue;
            }
            RemotePlayerView view = entry.getValue();
            Player remote = view == null ? null : view.player();
            if (!isAttackableRemotePlayer(remote)) {
                continue;
            }
            org.joml.Vector3f pos = remote.getPosition();
            AABB box = Raycast.playerPickBox(multiplayerRemotePlayerBox(pos.x, pos.y, pos.z));
            float distance = Raycast.intersectsAabb(origin, direction, box);
            if (distance >= 0.0f && distance < closestDistance) {
                closestDistance = distance;
                closestPlayerId = playerId;
                closestPoint = Raycast.pointAt(origin, direction, distance);
            }
        }
        return closestPoint == null ? null : new Player.PlayerHit(closestPlayerId, closestDistance, closestPoint);
    }

    private boolean applyMultiplayerPlayerAttack(Player.PlayerHit hit, Player.PlayerAttack attack) {
        if (!multiplayerPvp || hit == null || !hit.hit() || attack == null || attack.damage() <= 0.0f) {
            return false;
        }
        if (multiplayerServer != null && !clientMultiplayerWorld) {
            return applyLocalMultiplayerPlayerAttack(hit.playerId(), attack);
        }
        if (clientMultiplayerWorld && multiplayerClient != null && multiplayerClient.isConnected()) {
            return sendMultiplayerPlayerAttack(hit.playerId());
        }
        return false;
    }

    private boolean applyLocalMultiplayerPlayerAttack(String targetPlayerId, Player.PlayerAttack attack) {
        if (!multiplayerPvp || targetPlayerId == null || targetPlayerId.isBlank() || attack == null) {
            return false;
        }
        com.google.gson.JsonObject targetState = currentDimensionMultiplayerPlayerStateById(targetPlayerId);
        if (!isAttackableMultiplayerPlayerState(targetState)) {
            return false;
        }
        int clientId = parseProtocolClientId(targetPlayerId);
        if (clientId <= 0) {
            return false;
        }
        return sendMultiplayerRemoteDamage(clientId, attack.damage(), "player_attack",
                attack.sourceX(), attack.sourceY(), attack.sourceZ(),
                attack.horizontalKnockback(), attack.verticalKnockback(), attack.fireTicks());
    }

    private boolean sendMultiplayerPlayerAttack(String targetPlayerId) {
        if (targetPlayerId == null || targetPlayerId.isBlank()
                || multiplayerClient == null || !multiplayerClient.isConnected()) {
            return false;
        }
        try {
            com.google.gson.JsonObject data = NetworkMessage.object();
            data.addProperty("action", MultiplayerProtocol.ACTION_PLAYER_ATTACK);
            data.addProperty("targetPlayerId", targetPlayerId);
            multiplayerClient.send(NetworkMessage.of("clientAction", data));
            return true;
        } catch (Exception e) {
            System.err.println("Could not sync multiplayer player attack: " + e.getMessage());
            return false;
        }
    }

    private boolean isAttackableMultiplayerPlayerState(com.google.gson.JsonObject state) {
        return isLiveRemotePlayerState(state)
                && !parseNetworkGameMode(jsonString(state, "gameMode", "SURVIVAL")).isCreative();
    }

    private boolean isAttackableRemotePlayer(Player remote) {
        return remote != null
                && !remote.isDead()
                && remote.getStats().getHealth() > 0.0f
                && !remote.getGameMode().isCreative();
    }

    private boolean isDroppedItemEntityType(String entityType) {
        return "DROPPED_ITEM".equals(normalizeNetworkEntityType(entityType));
    }

    private String normalizeNetworkEntityType(String entityType) {
        return entityType == null ? "" : entityType.trim()
                .replace("minecraft:", "")
                .replace("craftzero:", "")
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(java.util.Locale.ROOT);
    }

    private MobDefinition parseMobDefinition(String entityType) {
        if (entityType == null || entityType.isBlank()) {
            return null;
        }
        String normalized = entityType.trim()
                .replace("minecraft:", "")
                .replace("craftzero:", "")
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(java.util.Locale.ROOT);
        try {
            return MobDefinition.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private ItemType commandActionItem(NetworkMessage message) {
        if (message.data() == null || !message.data().has("itemId")) {
            return null;
        }
        int id = messageInt(message, "itemId", -1);
        int data = messageInt(message, "itemData", 0);
        return ItemType.fromId(id, data);
    }

    private PotionData potionDataFromMessage(NetworkMessage message) {
        return potionDataFromMessage(message, "potion", true);
    }

    private PotionData potionDataFromMessage(NetworkMessage message, String prefix, boolean defaultSplash) {
        PotionType type = PotionType.WATER;
        String rawType = messageString(message, prefix + "Type", type.name());
        try {
            type = PotionType.valueOf(rawType.trim()
                    .replace('-', '_')
                    .replace(' ', '_')
                    .toUpperCase(java.util.Locale.ROOT));
        } catch (RuntimeException ignored) {
            type = PotionType.WATER;
        }
        return new PotionData(
                type,
                messageBoolean(message, prefix + "Splash", defaultSplash),
                messageBoolean(message, prefix + "Extended", false),
                messageBoolean(message, prefix + "Enhanced", false));
    }

    private int messageInt(NetworkMessage message, String key, int fallback) {
        try {
            return message.data() != null && message.data().has(key) ? message.data().get(key).getAsInt() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private long messageLong(NetworkMessage message, String key, long fallback) {
        try {
            return message.data() != null && message.data().has(key) ? message.data().get(key).getAsLong() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private int clampedMessageInt(NetworkMessage message, String key, int fallback, int min, int max) {
        int value = messageInt(message, key, fallback);
        return Math.max(min, Math.min(max, value));
    }

    private float messageFloat(NetworkMessage message, String key, float fallback) {
        try {
            return message.data() != null && message.data().has(key) ? message.data().get(key).getAsFloat() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private float clampedFiniteMessageFloat(NetworkMessage message, String key, float fallback, float min, float max) {
        float value = messageFloat(message, key, fallback);
        if (!Float.isFinite(value)) {
            value = fallback;
        }
        if (!Float.isFinite(value)) {
            value = min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private boolean messageBoolean(NetworkMessage message, String key, boolean fallback) {
        try {
            return message.data() != null && message.data().has(key) ? message.data().get(key).getAsBoolean() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private String messageString(NetworkMessage message, String key, String fallback) {
        try {
            return message.data() != null && message.data().has(key) ? message.data().get(key).getAsString() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private boolean networkMessageTargetsCurrentDimension(NetworkMessage message) {
        if (message == null || message.data() == null || !message.data().has("dimension")) {
            return true;
        }
        return dimensionNameTargetsCurrentWorld(messageString(message, "dimension", ""));
    }

    private boolean remotePlayerStateTargetsCurrentDimension(com.google.gson.JsonObject state) {
        if (state == null || !state.has("dimension")) {
            return true;
        }
        return dimensionNameTargetsCurrentWorld(jsonString(state, "dimension", ""));
    }

    private boolean dimensionNameTargetsCurrentWorld(String dimensionName) {
        if (world == null) {
            return true;
        }
        if (dimensionName == null || dimensionName.isBlank() || !Dimension.isValidSaveName(dimensionName)) {
            return false;
        }
        return Dimension.fromSaveName(dimensionName) == world.getDimension();
    }

    private boolean messageHasAny(NetworkMessage message, String... keys) {
        if (message == null || message.data() == null || keys == null) {
            return false;
        }
        for (String key : keys) {
            if (key != null && message.data().has(key)) {
                return true;
            }
        }
        return false;
    }

    private int parseProtocolClientId(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return -1;
        }
        String normalized = playerId.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.startsWith("player-")) {
            normalized = normalized.substring("player-".length());
        } else if (normalized.startsWith("player")) {
            normalized = normalized.substring("player".length());
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private boolean sendMultiplayerClientAction(int clientId, String action, Map<String, String> data) {
        if (multiplayerServer == null || clientId <= 0 || action == null || action.isBlank()) {
            return false;
        }
        com.google.gson.JsonObject payload = NetworkMessage.object();
        payload.addProperty("playerId", "player-" + clientId);
        payload.addProperty("action", action);
        if (data != null) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    payload.addProperty(entry.getKey(), entry.getValue());
                }
            }
        }
        boolean sent = multiplayerServer.sendToClient(clientId, NetworkMessage.of("clientAction", payload));
        if (sent && MultiplayerProtocol.ACTION_COMMAND_SPAWNPOINT.equals(action)) {
            rememberMultiplayerRespawnOverride(clientId, data);
        }
        return sent;
    }

    private void rememberMultiplayerRespawnOverride(int clientId, Map<String, String> data) {
        if (clientId <= 0 || multiplayerServer == null) {
            return;
        }
        String playerId = "player-" + clientId;
        float x;
        float y;
        float z;
        if (data != null && data.containsKey("x") && data.containsKey("y") && data.containsKey("z")) {
            try {
                x = Float.parseFloat(data.get("x"));
                y = Float.parseFloat(data.get("y"));
                z = Float.parseFloat(data.get("z"));
            } catch (RuntimeException ignored) {
                return;
            }
        } else {
            com.google.gson.JsonObject state = currentDimensionMultiplayerPlayerStateById(playerId);
            if (state == null) {
                return;
            }
            x = jsonFloat(state, "x", Float.NaN);
            y = jsonFloat(state, "y", Float.NaN);
            z = jsonFloat(state, "z", Float.NaN);
        }
        if (Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z)) {
            multiplayerRespawnOverrides.put(playerId, new org.joml.Vector3f(x, y, z));
        }
    }

    private void clearLocalPlayerInventory(ItemType filter) {
        if (player == null) {
            return;
        }
        if (filter == null) {
            player.getInventory().clearInventory();
            return;
        }
        clearInventorySlots(player.getInventory().getHotbar(), filter);
        clearInventorySlots(player.getInventory().getMainInventory(), filter);
        clearInventorySlots(player.getInventory().getCraftingGrid(), filter);
        clearInventorySlots(player.getInventory().getArmor(), filter);
        ItemStack cursor = player.getInventory().getCursorItem();
        if (cursor != null && cursor.getType() == filter) {
            player.getInventory().setCursorItem(null);
        }
    }

    private void clearInventorySlots(ItemStack[] slots, ItemType filter) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null && slots[i].getType() == filter) {
                slots[i] = null;
            }
        }
    }

    private void killLocalPlayer() {
        if (player != null) {
            PlayerStats stats = player.getStats();
            stats.restore(0.0f, stats.getHunger(), stats.getSaturation(), stats.getCurrentAir());
        }
    }

    private void applyNetworkBlockUpdate(NetworkMessage message) {
        applyNetworkBlockUpdate(message, true);
    }

    private boolean applyNetworkBlockUpdate(NetworkMessage message, boolean deferIfChunkMissing) {
        if (world == null || message.data() == null
                || !message.data().has("x") || !message.data().has("y") || !message.data().has("z")
                || !message.data().has("blockId")) {
            return false;
        }
        if (!networkMessageTargetsCurrentDimension(message)) {
            return false;
        }
        int x;
        int y;
        int z;
        int metadata;
        String blockId;
        try {
            x = message.data().get("x").getAsInt();
            y = message.data().get("y").getAsInt();
            z = message.data().get("z").getAsInt();
            metadata = message.data().has("metadata") ? message.data().get("metadata").getAsInt() : 0;
            blockId = message.data().get("blockId").getAsString();
        } catch (RuntimeException ignored) {
            return false;
        }
        if (!MultiplayerProtocol.isValidBlockUpdate(blockId, y, metadata)) {
            return false;
        }
        BlockType type = parseNetworkBlockType(blockId);
        if (world.getLoadedChunk(Math.floorDiv(x, com.craftzero.world.Chunk.WIDTH),
                Math.floorDiv(z, com.craftzero.world.Chunk.DEPTH)) == null) {
            if (deferIfChunkMissing && clientMultiplayerWorld) {
                deferNetworkBlockUpdate(message);
            }
            return false;
        }
        if (type != null) {
            applyRemotePlayerBlockActionAnimation(message);
            if (applyHostedClientBlockEdit(message, x, y, z, type, metadata)) {
                return true;
            }
            applyingNetworkBlockUpdate = true;
            try {
                world.setBlock(x, y, z, type, metadata);
            } finally {
                applyingNetworkBlockUpdate = false;
            }
            applyNetworkTileEntityData(message, type, x, y, z);
            broadcastMultiplayerBlockState(x, y, z);
            return true;
        }
        return false;
    }

    private void applyRemotePlayerBlockActionAnimation(NetworkMessage message) {
        if (message == null || message.data() == null) {
            return;
        }
        String playerId = messageString(message, "playerId", messageString(message, "sourcePlayerId", ""));
        RemotePlayerView view = remotePlayerViewForAction(playerId);
        if (view != null) {
            view.player().playRemoteUseAnimation();
        }
    }

    private void deferNetworkBlockUpdate(NetworkMessage message) {
        if (message == null || message.data() == null) {
            return;
        }
        while (deferredNetworkBlockUpdates.size() >= MAX_DEFERRED_NETWORK_BLOCK_UPDATES) {
            if (deferredNetworkBlockUpdates.poll() == null) {
                break;
            }
        }
        deferredNetworkBlockUpdates.offer(message);
    }

    private void drainDeferredNetworkBlockUpdates() {
        if (world == null || deferredNetworkBlockUpdates.isEmpty()) {
            return;
        }
        int attempts = Math.min(MAX_DEFERRED_NETWORK_BLOCK_UPDATES_PER_TICK,
                deferredNetworkBlockUpdates.size());
        for (int i = 0; i < attempts; i++) {
            NetworkMessage deferred = deferredNetworkBlockUpdates.poll();
            if (deferred == null) {
                return;
            }
            if (!networkBlockUpdateChunkLoaded(deferred)) {
                deferredNetworkBlockUpdates.offer(deferred);
                continue;
            }
            applyNetworkBlockUpdate(deferred, false);
        }
    }

    private boolean networkBlockUpdateChunkLoaded(NetworkMessage message) {
        if (world == null) {
            return false;
        }
        if (message == null || message.data() == null
                || !message.data().has("x") || !message.data().has("z")) {
            return true;
        }
        try {
            int x = message.data().get("x").getAsInt();
            int z = message.data().get("z").getAsInt();
            return world.getLoadedChunk(Math.floorDiv(x, com.craftzero.world.Chunk.WIDTH),
                    Math.floorDiv(z, com.craftzero.world.Chunk.DEPTH)) != null;
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    private boolean applyHostedClientBlockEdit(NetworkMessage message, int x, int y, int z,
            BlockType requestedType, int requestedMetadata) {
        if (multiplayerServer == null || clientMultiplayerWorld || world == null || message == null
                || message.data() == null) {
            return false;
        }
        String playerId = messageString(message, "playerId", "");
        if (parseProtocolClientId(playerId) <= 0) {
            return false;
        }
        BlockType current = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
        int currentMetadata = world.getBlockMetadataIfLoaded(x, y, z, 0);
        com.google.gson.JsonObject actorState = liveMultiplayerPlayerStateById(playerId);
        if (actorState == null) {
            return false;
        }
        if (requestedType == current && requestedMetadata == currentMetadata) {
            broadcastMultiplayerBlockState(x, y, z);
            return true;
        }
        if (!canRemotePlayerModifyBlock(playerId, x, y, z)) {
            broadcastMultiplayerBlockState(x, y, z);
            return true;
        }
        if (applyHostedClientSpecialBlockEdit(playerId, actorState, x, y, z,
                current, currentMetadata, requestedType, requestedMetadata)) {
            return true;
        }
        if (requestedType == BlockType.AIR && current != BlockType.AIR) {
            return applyHostedClientBlockBreak(playerId, actorState, x, y, z, current);
        }
        if (current == BlockType.AIR && requestedType != BlockType.AIR) {
            return applyHostedClientBlockPlacement(playerId, actorState, x, y, z, requestedType, requestedMetadata);
        }
        return false;
    }

    private boolean applyHostedClientBlockBreak(String playerId, com.google.gson.JsonObject actorState,
            int x, int y, int z, BlockType current) {
        if (playerId == null || playerId.isBlank() || current == null || current == BlockType.AIR
                || !current.isBreakable()) {
            return false;
        }
        ItemStack heldStack = multiplayerSelectedStack(playerId, actorState);
        ItemType heldType = heldStack == null || heldStack.isEmpty() ? null : heldStack.getType();
        ToolType toolType = heldType == null ? ToolType.NONE : heldType.getToolType();
        boolean canHarvest = isCreativeMultiplayerPlayerState(actorState)
                || BlockHarvestRules.canHarvest(current, heldType, toolType);
        boolean broke = world.breakBlockWithToolStack(x, y, z, canHarvest, heldStack);
        if (!broke) {
            return false;
        }
        if (shouldDamageMultiplayerToolOnBlockBreak(heldStack, current, heldType)) {
            damageMultiplayerSelectedDurable(playerId, actorState, 1);
        }
        return true;
    }

    private boolean applyHostedClientBlockPlacement(String playerId, com.google.gson.JsonObject actorState,
            int x, int y, int z,
            BlockType requestedType, int requestedMetadata) {
        if (playerId == null || playerId.isBlank() || requestedType == null || requestedType == BlockType.AIR) {
            return false;
        }
        ItemStack heldStack = multiplayerSelectedStack(playerId, actorState);
        ItemType heldType = heldStack == null || heldStack.isEmpty() ? null : heldStack.getType();
        if (heldType == null || !heldType.isPlaceable() || heldType.getPlacedBlock() != requestedType) {
            return false;
        }
        if (!world.canPlaceBlockAt(x, y, z, requestedType, requestedMetadata, null)) {
            return false;
        }
        world.setBlock(x, y, z, requestedType, requestedMetadata);
        if (!isCreativeMultiplayerPlayerState(actorState)) {
            consumeMultiplayerSelectedItem(playerId, actorState, heldType, 1);
        }
        world.playBlockPlaceSound(requestedType, x, y, z);
        world.rebuildBlockMeshesNow(x, y, z);
        return true;
    }

    private boolean applyHostedClientSpecialBlockEdit(String playerId, com.google.gson.JsonObject actorState,
            int x, int y, int z, BlockType current, int currentMetadata,
            BlockType requestedType, int requestedMetadata) {
        ItemStack heldStack = multiplayerSelectedStack(playerId, actorState);
        ItemType heldType = heldStack == null || heldStack.isEmpty() ? null : heldStack.getType();
        if (applyHostedClientFluidBucketEdit(playerId, actorState, x, y, z,
                current, requestedType, requestedMetadata, heldType)) {
            return true;
        }
        if (applyHostedClientTntIgnite(playerId, actorState, x, y, z, current, requestedType, heldType)) {
            return true;
        }
        if (applyHostedClientFirePlacement(playerId, actorState, x, y, z, current, requestedType,
                requestedMetadata, heldType)) {
            return true;
        }
        if (applyHostedClientMultiBlockPlacement(playerId, actorState, x, y, z,
                current, requestedType, requestedMetadata, heldType)) {
            return true;
        }
        if (applyHostedClientSignPlacement(playerId, actorState, x, y, z,
                current, requestedType, requestedMetadata, heldType)) {
            return true;
        }
        if (applyHostedClientSlabMerge(playerId, actorState, x, y, z, current, requestedType, heldType)) {
            return true;
        }
        if (applyHostedClientInteractiveBlockUse(actorState, x, y, z,
                current, currentMetadata, requestedType, requestedMetadata)) {
            return true;
        }
        if (applyHostedClientRedstoneOreUse(x, y, z, current, requestedType, requestedMetadata)) {
            return true;
        }
        if (applyHostedClientCakeUse(actorState, x, y, z, current, currentMetadata,
                requestedType, requestedMetadata)) {
            return true;
        }
        if (applyHostedClientDragonEggUse(x, y, z, current, requestedType)) {
            return true;
        }
        if (applyHostedClientCauldronUse(playerId, actorState, x, y, z, current, currentMetadata,
                requestedType, requestedMetadata, heldType)) {
            return true;
        }
        if (applyHostedClientFarmingUse(playerId, actorState, x, y, z, current,
                requestedType, requestedMetadata, heldType)) {
            return true;
        }
        if (applyHostedClientEndPortalFrameUse(playerId, actorState, x, y, z, current, currentMetadata,
                requestedType, requestedMetadata, heldType)) {
            return true;
        }
        return applyHostedClientBoneMealUse(playerId, actorState, x, y, z, current, currentMetadata,
                requestedType, requestedMetadata, heldType);
    }

    private boolean applyHostedClientFluidBucketEdit(String playerId, com.google.gson.JsonObject actorState,
            int x, int y, int z, BlockType current, BlockType requestedType, int requestedMetadata,
            ItemType heldType) {
        if (heldType == ItemType.BUCKET && requestedType == BlockType.AIR && current != null && current.isFluid()) {
            ItemType filled = world.pickupFluidSource(x, y, z);
            if (filled == null) {
                return false;
            }
            replaceOneMultiplayerSelectedItemWith(playerId, actorState, ItemType.BUCKET,
                    new ItemStack(filled, 1));
            world.rebuildBlockMeshesNow(x, y, z);
            return true;
        }
        if (heldType != ItemType.WATER_BUCKET && heldType != ItemType.LAVA_BUCKET) {
            return false;
        }
        boolean water = heldType == ItemType.WATER_BUCKET;
        BlockType source = water ? BlockType.WATER : BlockType.LAVA;
        if (requestedMetadata != 0 || !isHostedClientFluidPlacementResult(requestedType, source)) {
            return false;
        }
        if (!world.placeFluidSource(x, y, z, water, null)) {
            return false;
        }
        replaceOneMultiplayerSelectedItemWith(playerId, actorState, heldType, new ItemStack(ItemType.BUCKET, 1));
        world.rebuildBlockMeshesNow(x, y, z);
        return true;
    }

    private boolean isHostedClientFluidPlacementResult(BlockType requestedType, BlockType source) {
        return requestedType == source
                || requestedType == BlockType.OBSIDIAN
                || requestedType == BlockType.COBBLESTONE;
    }

    private boolean applyHostedClientTntIgnite(String playerId, com.google.gson.JsonObject actorState,
            int x, int y, int z, BlockType current, BlockType requestedType, ItemType heldType) {
        if (heldType != ItemType.FLINT_AND_STEEL || current != BlockType.TNT || requestedType != BlockType.AIR) {
            return false;
        }
        if (world.primeTntByRemotePlayer(x, y, z, 80, playerId) == null) {
            return false;
        }
        damageMultiplayerSelectedDurable(playerId, actorState, 1);
        world.rebuildBlockMeshesNow(x, y, z);
        return true;
    }

    private boolean applyHostedClientFirePlacement(String playerId, com.google.gson.JsonObject actorState,
            int x, int y, int z, BlockType current, BlockType requestedType, int requestedMetadata,
            ItemType heldType) {
        if (heldType != ItemType.FLINT_AND_STEEL || current != BlockType.AIR
                || requestedType != BlockType.FIRE || requestedMetadata != 0) {
            return false;
        }
        if (!world.canPlaceBlockAt(x, y, z, BlockType.FIRE, 0, null)) {
            return false;
        }
        world.setBlock(x, y, z, BlockType.FIRE, 0);
        world.playFireIgniteSound(x + 0.5f, y + 0.5f, z + 0.5f);
        damageMultiplayerSelectedDurable(playerId, actorState, 1);
        world.rebuildBlockMeshesNow(x, y, z);
        return true;
    }

    private boolean applyHostedClientMultiBlockPlacement(String playerId, com.google.gson.JsonObject actorState,
            int x, int y, int z, BlockType current, BlockType requestedType, int requestedMetadata,
            ItemType heldType) {
        if (current != BlockType.AIR || heldType == null) {
            return false;
        }
        if (requestedType != null && requestedType.isDoor() && heldType.getPlacedBlock() == requestedType
                && !BlockShape.isDoorUpper(requestedMetadata)) {
            int facing = requestedMetadata & 3;
            if (!world.placeDoor(x, y, z, requestedType, facing, null)) {
                return false;
            }
            consumeMultiplayerSelectedItem(playerId, actorState, heldType, 1);
            world.playBlockPlaceSound(requestedType, x, y, z);
            world.rebuildBlockMeshesNow(x, y, z);
            world.rebuildBlockMeshesNow(x, y + 1, z);
            return true;
        }
        if (requestedType == BlockType.BED && heldType == ItemType.BED
                && (requestedMetadata & 8) == 0) {
            int facing = requestedMetadata & 3;
            if (world.placeBed(x, y, z, facing, null) == null) {
                return false;
            }
            consumeMultiplayerSelectedItem(playerId, actorState, heldType, 1);
            int[] dir = World.horizontalDirection(facing);
            world.playBlockPlaceSound(BlockType.BED, x, y, z);
            world.rebuildBlockMeshesNow(x, y, z);
            world.rebuildBlockMeshesNow(x + dir[0], y, z + dir[1]);
            return true;
        }
        return false;
    }

    private boolean applyHostedClientSignPlacement(String playerId, com.google.gson.JsonObject actorState,
            int x, int y, int z, BlockType current, BlockType requestedType, int requestedMetadata,
            ItemType heldType) {
        if (current != BlockType.AIR || heldType != ItemType.SIGN
                || (requestedType != BlockType.STANDING_SIGN && requestedType != BlockType.WALL_SIGN)) {
            return false;
        }
        if (!world.canPlaceBlockAt(x, y, z, requestedType, requestedMetadata, null)) {
            return false;
        }
        world.setBlock(x, y, z, requestedType, requestedMetadata);
        consumeMultiplayerSelectedItem(playerId, actorState, heldType, 1);
        world.playBlockPlaceSound(requestedType, x, y, z);
        world.rebuildBlockMeshesNow(x, y, z);
        return true;
    }

    private boolean applyHostedClientSlabMerge(String playerId, com.google.gson.JsonObject actorState,
            int x, int y, int z, BlockType current, BlockType requestedType, ItemType heldType) {
        if (current != BlockType.STONE_SLAB || requestedType != BlockType.DOUBLE_STONE_SLAB
                || heldType == null || heldType.getPlacedBlock() != BlockType.STONE_SLAB) {
            return false;
        }
        if (!world.tryMergeSlab(x, y, z)) {
            return false;
        }
        consumeMultiplayerSelectedItem(playerId, actorState, heldType, 1);
        world.playBlockPlaceSound(BlockType.STONE_SLAB, x, y, z);
        world.rebuildBlockMeshesNow(x, y, z);
        return true;
    }

    private boolean applyHostedClientInteractiveBlockUse(com.google.gson.JsonObject actorState,
            int x, int y, int z, BlockType current, int currentMetadata,
            BlockType requestedType, int requestedMetadata) {
        if (current == null || current == BlockType.AIR || current != requestedType
                || currentMetadata == requestedMetadata) {
            return false;
        }
        if (!world.toggleBlock(x, y, z, hostedClientHorizontalFacing(actorState))) {
            return false;
        }
        rebuildHostedToggledBlockMeshes(x, y, z, current);
        return true;
    }

    private boolean applyHostedClientRedstoneOreUse(int x, int y, int z,
            BlockType current, BlockType requestedType, int requestedMetadata) {
        if ((current != BlockType.REDSTONE_ORE && current != BlockType.GLOWING_REDSTONE_ORE)
                || requestedType != BlockType.GLOWING_REDSTONE_ORE) {
            return false;
        }
        int metadata = world.getBlockMetadataIfLoaded(x, y, z, 0);
        if (requestedMetadata != metadata) {
            return false;
        }
        boolean activated = world.activateRedstoneOre(x, y, z);
        if (activated) {
            world.rebuildBlockMeshesNow(x, y, z);
        }
        return activated;
    }

    private boolean applyHostedClientCakeUse(com.google.gson.JsonObject actorState,
            int x, int y, int z, BlockType current, int currentMetadata,
            BlockType requestedType, int requestedMetadata) {
        float hunger = hostedClientFoodValue(actorState, "stats.hunger", PlayerStats.MAX_HUNGER);
        if (current != BlockType.CAKE || isCreativeMultiplayerPlayerState(actorState)
                || hunger >= PlayerStats.MAX_HUNGER) {
            return false;
        }
        int bites = Math.max(0, Math.min(World.CAKE_LAST_BITE_METADATA, currentMetadata));
        boolean validRequest = bites >= World.CAKE_LAST_BITE_METADATA
                ? requestedType == BlockType.AIR && requestedMetadata == 0
                : requestedType == BlockType.CAKE && requestedMetadata == bites + 1;
        if (!validRequest || !world.eatCakeSlice(x, y, z)) {
            return false;
        }
        feedHostedClientPlayerState(actorState, Player.CAKE_SLICE_HUNGER, Player.CAKE_SLICE_SATURATION);
        playHostedClientConsumeCompleteSounds(actorState);
        world.rebuildBlockMeshesNow(x, y, z);
        return true;
    }

    private void feedHostedClientPlayerState(com.google.gson.JsonObject actorState,
            float hungerPoints, float saturationPoints) {
        if (actorState == null || hungerPoints <= 0.0f) {
            return;
        }
        float hunger = hostedClientFoodValue(actorState, "stats.hunger", PlayerStats.MAX_HUNGER);
        float saturation = hostedClientFoodValue(actorState, "stats.saturation", 0.0f);
        float newHunger = Math.min(PlayerStats.MAX_HUNGER, hunger + hungerPoints);
        float newSaturation = Math.min(newHunger, saturation + Math.max(0.0f, saturationPoints));
        actorState.addProperty("stats.hunger", newHunger);
        actorState.addProperty("stats.saturation", newSaturation);
    }

    private float hostedClientFoodValue(com.google.gson.JsonObject actorState, String key, float fallback) {
        float value = jsonFloat(actorState, key, fallback);
        if (!Float.isFinite(value)) {
            value = fallback;
        }
        return Math.max(0.0f, Math.min(PlayerStats.MAX_HUNGER, value));
    }

    private void playHostedClientConsumeCompleteSounds(com.google.gson.JsonObject actorState) {
        if (world == null || actorState == null) {
            return;
        }
        float x = jsonFloat(actorState, "x", 0.0f);
        float y = jsonFloat(actorState, "y", 80.0f) + (float) MultiplayerProtocol.PLAYER_EYE_HEIGHT * 0.5f;
        float z = jsonFloat(actorState, "z", 0.0f);
        world.playEatSound(x, y, z);
        world.playBurpSound(x, y, z);
    }

    private boolean applyClientFoodUse(String playerId, com.google.gson.JsonObject actorState, ItemStack heldStack) {
        if (!isLiveRemotePlayerState(actorState) || heldStack == null || heldStack.isEmpty()) {
            return false;
        }
        ItemType type = heldStack.getType();
        HostedFoodValue food = multiplayerFoodValue(type);
        if (food == null) {
            return false;
        }
        float hunger = hostedClientFoodValue(actorState, "stats.hunger", PlayerStats.MAX_HUNGER);
        if (type != ItemType.GOLDEN_APPLE && hunger >= PlayerStats.MAX_HUNGER) {
            return false;
        }
        boolean creative = isCreativeMultiplayerPlayerState(actorState);
        if (!creative && !consumeHostedClientFoodStack(playerId, actorState, type)) {
            return false;
        }
        if (!creative) {
            feedHostedClientPlayerState(actorState, food.hunger(), food.saturation());
            applyHostedClientFoodSideEffects(actorState, type);
        }
        playHostedClientConsumeCompleteSounds(actorState);
        return true;
    }

    private boolean consumeHostedClientFoodStack(String playerId, com.google.gson.JsonObject actorState, ItemType type) {
        if (type == ItemType.MUSHROOM_STEW) {
            return replaceOneMultiplayerSelectedItemWith(playerId, actorState, ItemType.MUSHROOM_STEW,
                    new ItemStack(ItemType.BOWL, 1));
        }
        return consumeMultiplayerSelectedItem(playerId, actorState, type, 1);
    }

    private boolean applyClientMilkUse(String playerId, com.google.gson.JsonObject actorState, ItemStack heldStack) {
        if (!isLiveRemotePlayerState(actorState) || heldStack == null || heldStack.isEmpty()
                || heldStack.getType() != ItemType.MILK_BUCKET) {
            return false;
        }
        if (!isCreativeMultiplayerPlayerState(actorState)
                && !replaceOneMultiplayerSelectedItemWith(playerId, actorState, ItemType.MILK_BUCKET,
                        new ItemStack(ItemType.BUCKET, 1))) {
            return false;
        }
        clearHostedClientStatusEffects(actorState);
        playHostedClientDrinkSound(actorState);
        return true;
    }

    private boolean applyClientDrinkablePotionUse(String playerId, com.google.gson.JsonObject actorState,
            ItemStack heldStack) {
        if (!isLiveRemotePlayerState(actorState) || heldStack == null || heldStack.isEmpty()
                || heldStack.getType() != ItemType.POTION) {
            return false;
        }
        PotionData potion = itemPotionDataOrWater(heldStack);
        if (potion.splash()) {
            return false;
        }
        if (!isCreativeMultiplayerPlayerState(actorState)
                && !replaceOneMultiplayerSelectedItemWith(playerId, actorState, ItemType.POTION,
                        new ItemStack(ItemType.GLASS_BOTTLE, 1))) {
            return false;
        }
        applyHostedClientPotionEffects(actorState, potion);
        playHostedClientDrinkSound(actorState);
        return true;
    }

    private boolean applyClientMapUse(String playerId, com.google.gson.JsonObject actorState, ItemStack heldStack) {
        if (!isLiveRemotePlayerState(actorState) || playerId == null || playerId.isBlank()
                || !MapItemData.isMap(heldStack)) {
            return false;
        }
        int selectedSlot = Math.max(0, Math.min(Inventory.HOTBAR_SIZE - 1,
                jsonInt(actorState, "selectedSlot", 0)));
        Inventory inventory = multiplayerInventorySnapshotFor(playerId);
        ItemStack selected = multiplayerInventorySlot(inventory, selectedSlot);
        if (!MapItemData.isMap(selected)) {
            return false;
        }
        ItemStack[] before = snapshotPlayerInventoryStacks(inventory);
        boolean changed = MapItemData.useMap(
                world,
                selected,
                jsonFloat(actorState, "x", 0.0f),
                jsonFloat(actorState, "z", 0.0f),
                jsonFloat(actorState, "yaw", 0.0f));
        if (changed) {
            actorState.addProperty("heldItemId", multiplayerItemId(selected));
            actorState.addProperty("heldItemCount", selected.getCount());
            actorState.addProperty("heldItemDamage", selected.getDurability());
            broadcastChangedMultiplayerInventorySlots(playerId, inventory, before);
        }
        return true;
    }

    private boolean applyClientArmorEquip(String playerId, com.google.gson.JsonObject actorState, ItemStack heldStack) {
        if (!isLiveRemotePlayerState(actorState) || playerId == null || playerId.isBlank()
                || heldStack == null || heldStack.isEmpty()) {
            return false;
        }
        ArmorSlot armorSlot = ArmorMaterial.slotOf(heldStack.getType());
        if (armorSlot == null) {
            return false;
        }
        int selectedSlot = Math.max(0, Math.min(Inventory.HOTBAR_SIZE - 1,
                jsonInt(actorState, "selectedSlot", 0)));
        Inventory inventory = multiplayerInventorySnapshotFor(playerId);
        ItemStack selected = multiplayerInventorySlot(inventory, selectedSlot);
        if (selected == null || selected.isEmpty() || selected.getType() != heldStack.getType()) {
            return false;
        }
        int armorInventorySlot = Inventory.HOTBAR_SIZE + Inventory.MAIN_SIZE
                + Inventory.CRAFTING_SIZE + armorSlot.getIndex();
        ItemStack[] before = snapshotPlayerInventoryStacks(inventory);
        ItemStack previousArmor = multiplayerInventorySlot(inventory, armorInventorySlot);
        ItemStack equipped = selected.copy();
        equipped.setCount(1);
        applyLocalInventorySlot(inventory, armorInventorySlot, equipped);
        applyLocalInventorySlot(inventory, selectedSlot, previousArmor == null ? null : previousArmor.copy());
        actorState.addProperty("heldItemId", previousArmor == null || previousArmor.isEmpty()
                ? "air" : multiplayerItemId(previousArmor));
        actorState.addProperty("heldItemCount", previousArmor == null || previousArmor.isEmpty()
                ? 0 : previousArmor.getCount());
        actorState.addProperty("heldItemDamage", previousArmor == null || previousArmor.isEmpty()
                ? 0 : previousArmor.getDurability());
        broadcastChangedMultiplayerInventorySlots(playerId, inventory, before);
        return true;
    }

    private PotionData itemPotionDataOrWater(ItemStack stack) {
        PotionData potion = stack == null ? null : stack.getPotionData();
        return potion == null ? PotionData.water() : potion;
    }

    private void playHostedClientDrinkSound(com.google.gson.JsonObject actorState) {
        if (world == null || actorState == null) {
            return;
        }
        float x = jsonFloat(actorState, "x", 0.0f);
        float y = jsonFloat(actorState, "y", 80.0f) + (float) MultiplayerProtocol.PLAYER_EYE_HEIGHT * 0.5f;
        float z = jsonFloat(actorState, "z", 0.0f);
        world.playDrinkSound(x, y, z);
    }

    private void applyHostedClientFoodSideEffects(com.google.gson.JsonObject actorState, ItemType type) {
        if (actorState == null || type == null) {
            return;
        }
        java.util.Random random = world == null ? new java.util.Random() : world.getRandom();
        if (type == ItemType.ROTTEN_FLESH && random.nextFloat() < 0.8f) {
            addHostedClientStatusEffect(actorState, new StatusEffectInstance(StatusEffectType.HUNGER, 30 * 20, 0));
        } else if (type == ItemType.RAW_CHICKEN && random.nextFloat() < 0.3f) {
            addHostedClientStatusEffect(actorState, new StatusEffectInstance(StatusEffectType.HUNGER, 30 * 20, 0));
        } else if (type == ItemType.SPIDER_EYE) {
            addHostedClientStatusEffect(actorState, new StatusEffectInstance(StatusEffectType.POISON, 5 * 20, 0));
        } else if (type == ItemType.GOLDEN_APPLE) {
            addHostedClientStatusEffect(actorState, new StatusEffectInstance(StatusEffectType.REGENERATION, 30 * 20, 0));
        }
    }

    private void applyHostedClientPotionEffects(com.google.gson.JsonObject actorState, PotionData potion) {
        if (actorState == null || potion == null) {
            return;
        }
        if (PotionEffectResolver.isInstant(potion)) {
            applyHostedClientInstantPotion(actorState, potion);
            return;
        }
        for (StatusEffectInstance effect : PotionEffectResolver.effects(potion)) {
            int duration = Math.round(effect.durationTicks());
            if (duration > 20) {
                addHostedClientStatusEffect(actorState,
                        new StatusEffectInstance(effect.type(), duration, effect.amplifier()));
            }
        }
    }

    private void applyHostedClientInstantPotion(com.google.gson.JsonObject actorState, PotionData potion) {
        if (potion.type() == PotionType.HEALING) {
            setHostedClientHealth(actorState,
                    hostedClientHealth(actorState) + hostedClientInstantPotionAmount(potion, true));
        } else if (potion.type() == PotionType.HARMING) {
            setHostedClientHealth(actorState,
                    hostedClientHealth(actorState) - hostedClientInstantPotionAmount(potion, false));
        }
    }

    private int hostedClientInstantPotionAmount(PotionData potion, boolean beneficial) {
        int amplifier = potion != null && potion.enhanced() ? 1 : 0;
        int base = (beneficial ? 4 : 6) << amplifier;
        return Math.max(0, (int) (base + 0.5f));
    }

    private float hostedClientHealth(com.google.gson.JsonObject actorState) {
        float fallback = jsonFloat(actorState, "health", MultiplayerProtocol.MAX_PLAYER_HEALTH);
        float health = jsonFloat(actorState, "stats.health", fallback);
        if (!Float.isFinite(health)) {
            health = fallback;
        }
        return Math.max(0.0f, Math.min(MultiplayerProtocol.MAX_PLAYER_HEALTH, health));
    }

    private void setHostedClientHealth(com.google.gson.JsonObject actorState, float health) {
        if (actorState == null || !Float.isFinite(health)) {
            return;
        }
        float clamped = Math.max(0.0f, Math.min(MultiplayerProtocol.MAX_PLAYER_HEALTH, health));
        actorState.addProperty("health", clamped);
        actorState.addProperty("stats.health", clamped);
    }

    private void clearHostedClientStatusEffects(com.google.gson.JsonObject actorState) {
        setHostedClientStatusEffects(actorState, List.of());
    }

    private void addHostedClientStatusEffect(com.google.gson.JsonObject actorState, StatusEffectInstance effect) {
        if (actorState == null || effect == null || effect.expired()) {
            return;
        }
        ArrayList<StatusEffectInstance> effects = new ArrayList<>(hostedClientStatusEffects(actorState));
        for (int i = 0; i < effects.size(); i++) {
            StatusEffectInstance existing = effects.get(i);
            if (existing.type() == effect.type()) {
                if (effect.amplifier() > existing.amplifier()
                        || (effect.amplifier() == existing.amplifier()
                                && effect.durationTicks() > existing.durationTicks())) {
                    effects.set(i, effect);
                    setHostedClientStatusEffects(actorState, effects);
                }
                return;
            }
        }
        effects.add(effect);
        setHostedClientStatusEffects(actorState, effects);
    }

    private List<StatusEffectInstance> hostedClientStatusEffects(com.google.gson.JsonObject actorState) {
        ArrayList<StatusEffectInstance> effects = new ArrayList<>();
        if (actorState == null) {
            return effects;
        }
        int count = Math.max(0, Math.min(jsonInt(actorState, "status.count", 0),
                MultiplayerProtocol.MAX_CLIENT_STATUS_EFFECTS));
        for (int i = 0; i < count; i++) {
            String rawType = jsonString(actorState, "status." + i + ".type", "");
            try {
                StatusEffectType type = StatusEffectType.valueOf(rawType);
                int duration = Math.max(0, Math.min(jsonInt(actorState, "status." + i + ".duration", 0),
                        MultiplayerProtocol.MAX_CLIENT_STATUS_EFFECT_DURATION));
                int amplifier = Math.max(0, Math.min(jsonInt(actorState, "status." + i + ".amplifier", 0),
                        MultiplayerProtocol.MAX_CLIENT_STATUS_EFFECT_AMPLIFIER));
                if (duration > 0) {
                    effects.add(new StatusEffectInstance(type, duration, amplifier));
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return effects;
    }

    private void setHostedClientStatusEffects(com.google.gson.JsonObject actorState,
            List<StatusEffectInstance> effects) {
        if (actorState == null) {
            return;
        }
        ArrayList<String> statusKeys = new ArrayList<>();
        for (Map.Entry<String, com.google.gson.JsonElement> entry : actorState.entrySet()) {
            if (entry.getKey().startsWith("status.")) {
                statusKeys.add(entry.getKey());
            }
        }
        for (String key : statusKeys) {
            actorState.remove(key);
        }
        int count = 0;
        if (effects != null) {
            for (StatusEffectInstance effect : effects) {
                if (effect == null || effect.expired()
                        || count >= MultiplayerProtocol.MAX_CLIENT_STATUS_EFFECTS) {
                    continue;
                }
                int duration = Math.max(0, Math.min(effect.durationTicks(),
                        MultiplayerProtocol.MAX_CLIENT_STATUS_EFFECT_DURATION));
                int amplifier = Math.max(0, Math.min(effect.amplifier(),
                        MultiplayerProtocol.MAX_CLIENT_STATUS_EFFECT_AMPLIFIER));
                if (duration <= 0) {
                    continue;
                }
                actorState.addProperty("status." + count + ".type", effect.type().name());
                actorState.addProperty("status." + count + ".duration", duration);
                actorState.addProperty("status." + count + ".amplifier", amplifier);
                count++;
            }
        }
        actorState.addProperty("status.count", count);
    }

    private HostedFoodValue multiplayerFoodValue(ItemType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case APPLE -> new HostedFoodValue(4, 2.4f);
            case BREAD -> new HostedFoodValue(5, 6.0f);
            case MUSHROOM_STEW -> new HostedFoodValue(6, 7.2f);
            case RAW_PORKCHOP, RAW_BEEF -> new HostedFoodValue(3, 1.8f);
            case COOKED_PORKCHOP, STEAK -> new HostedFoodValue(8, 12.8f);
            case GOLDEN_APPLE -> new HostedFoodValue(10, 24.0f);
            case COOKIE, RAW_FISH -> new HostedFoodValue(2, 0.4f);
            case MELON_SLICE, RAW_CHICKEN -> new HostedFoodValue(2, 1.2f);
            case COOKED_FISH -> new HostedFoodValue(5, 6.0f);
            case COOKED_CHICKEN -> new HostedFoodValue(6, 7.2f);
            case ROTTEN_FLESH -> new HostedFoodValue(4, 0.8f);
            case SPIDER_EYE -> new HostedFoodValue(2, 3.2f);
            default -> null;
        };
    }

    private record HostedFoodValue(float hunger, float saturation) {
    }

    private boolean applyHostedClientDragonEggUse(int x, int y, int z,
            BlockType current, BlockType requestedType) {
        if (current != BlockType.DRAGON_EGG || requestedType != BlockType.AIR) {
            return false;
        }
        BlockPos newPos = world.teleportDragonEgg(x, y, z);
        if (newPos == null) {
            return false;
        }
        world.rebuildBlockMeshesNow(x, y, z);
        world.rebuildBlockMeshesNow(newPos.x(), newPos.y(), newPos.z());
        return true;
    }

    private boolean applyHostedClientCauldronUse(String playerId, com.google.gson.JsonObject actorState,
            int x, int y, int z, BlockType current, int currentMetadata,
            BlockType requestedType, int requestedMetadata, ItemType heldType) {
        if (current != BlockType.CAULDRON || requestedType != BlockType.CAULDRON) {
            return false;
        }
        if (heldType == ItemType.WATER_BUCKET && requestedMetadata == World.CAULDRON_MAX_LEVEL) {
            if (!world.fillCauldronFromWaterBucket(x, y, z)) {
                return false;
            }
            replaceOneMultiplayerSelectedItemWith(playerId, actorState, ItemType.WATER_BUCKET,
                    new ItemStack(ItemType.BUCKET, 1));
            world.rebuildBlockMeshesNow(x, y, z);
            return true;
        }
        int level = Math.max(0, Math.min(World.CAULDRON_MAX_LEVEL, currentMetadata));
        if (heldType == ItemType.GLASS_BOTTLE && level > 0 && requestedMetadata == level - 1) {
            if (!world.drainCauldronIntoBottle(x, y, z)) {
                return false;
            }
            ItemStack waterBottle = new ItemStack(ItemType.POTION, 1);
            waterBottle.setPotionData(PotionData.water());
            replaceOneMultiplayerSelectedItemWith(playerId, actorState, ItemType.GLASS_BOTTLE, waterBottle);
            world.rebuildBlockMeshesNow(x, y, z);
            return true;
        }
        return false;
    }

    private boolean applyHostedClientFarmingUse(String playerId, com.google.gson.JsonObject actorState,
            int x, int y, int z, BlockType current, BlockType requestedType, int requestedMetadata,
            ItemType heldType) {
        if (isMultiplayerHoe(heldType) && (current == BlockType.DIRT || current == BlockType.GRASS)
                && requestedType == BlockType.FARMLAND && requestedMetadata == 0) {
            if (world.getBlockIfLoaded(x, y + 1, z, BlockType.AIR) != BlockType.AIR) {
                return false;
            }
            world.setBlock(x, y, z, BlockType.FARMLAND, 0);
            damageMultiplayerSelectedDurable(playerId, actorState, 1);
            world.rebuildBlockMeshesNow(x, y, z);
            return true;
        }
        BlockType crop = multiplayerCropBlockForSeed(heldType);
        if (crop == null || current != BlockType.AIR || requestedType != crop || requestedMetadata != 0) {
            return false;
        }
        if (world.getBlockIfLoaded(x, y - 1, z, BlockType.AIR) != BlockType.FARMLAND
                || !world.canPlaceBlockAt(x, y, z, crop, 0, null)) {
            return false;
        }
        world.setBlock(x, y, z, crop, 0);
        consumeMultiplayerSelectedItem(playerId, actorState, heldType, 1);
        world.rebuildBlockMeshesNow(x, y, z);
        return true;
    }

    private boolean applyHostedClientEndPortalFrameUse(String playerId, com.google.gson.JsonObject actorState,
            int x, int y, int z, BlockType current, int currentMetadata,
            BlockType requestedType, int requestedMetadata, ItemType heldType) {
        if (heldType != ItemType.EYE_OF_ENDER || current != BlockType.END_PORTAL_FRAME
                || requestedType != BlockType.END_PORTAL_FRAME
                || requestedMetadata != (currentMetadata | World.END_PORTAL_FRAME_EYE_BIT)) {
            return false;
        }
        if (!world.addEyeToEndPortalFrame(x, y, z)) {
            return false;
        }
        consumeMultiplayerSelectedItem(playerId, actorState, heldType, 1);
        world.rebuildBlockMeshesNow(x, y, z);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (world.isEndPortalAt(x + dx, y, z + dz)) {
                    world.rebuildBlockMeshesNow(x + dx, y, z + dz);
                }
            }
        }
        return true;
    }

    private boolean applyHostedClientBoneMealUse(String playerId, com.google.gson.JsonObject actorState,
            int x, int y, int z, BlockType current, int currentMetadata,
            BlockType requestedType, int requestedMetadata, ItemType heldType) {
        if (heldType != ItemType.BONE_MEAL || current == null || !current.isCrop()
                || requestedType != current || requestedMetadata <= currentMetadata) {
            return false;
        }
        if (!world.applyBoneMealToPlant(x, y, z)) {
            return false;
        }
        consumeMultiplayerSelectedItem(playerId, actorState, heldType, 1);
        world.rebuildBlockMeshesNow(x, y, z);
        return true;
    }

    private boolean isMultiplayerHoe(ItemType type) {
        return type != null && type.getToolType().getCategory() == ToolType.Category.HOE;
    }

    private BlockType multiplayerCropBlockForSeed(ItemType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case SEEDS -> BlockType.CROPS;
            case PUMPKIN_SEEDS -> BlockType.PUMPKIN_STEM;
            case MELON_SEEDS -> BlockType.MELON_STEM;
            default -> null;
        };
    }

    private int hostedClientHorizontalFacing(com.google.gson.JsonObject actorState) {
        float yaw = jsonFloat(actorState, "yaw", 0.0f) % 360.0f;
        if (yaw < 0.0f) {
            yaw += 360.0f;
        }
        if (yaw >= 315.0f || yaw < 45.0f) {
            return 0;
        }
        if (yaw < 135.0f) {
            return 1;
        }
        if (yaw < 225.0f) {
            return 2;
        }
        return 3;
    }

    private void rebuildHostedToggledBlockMeshes(int x, int y, int z, BlockType clickedBlock) {
        if (clickedBlock == null || clickedBlock == BlockType.JUKEBOX) {
            return;
        }
        if (clickedBlock.isDoor()) {
            int metadata = world.getBlockMetadataIfLoaded(x, y, z, 0);
            int lowerY = BlockShape.isDoorUpper(metadata) ? y - 1 : y;
            if (world.getBlockIfLoaded(x, lowerY, z, BlockType.AIR) == clickedBlock
                    && !BlockShape.isDoorUpper(world.getBlockMetadataIfLoaded(x, lowerY, z, 0))) {
                world.rebuildBlockMeshesNow(x, lowerY, z);
                int upperY = lowerY + 1;
                if (world.getBlockIfLoaded(x, upperY, z, BlockType.AIR) == clickedBlock
                        && BlockShape.isDoorUpper(world.getBlockMetadataIfLoaded(x, upperY, z, 0))) {
                    world.rebuildBlockMeshesNow(x, upperY, z);
                }
                return;
            }
        }
        world.rebuildBlockMeshesNow(x, y, z);
    }

    private boolean replaceOneMultiplayerSelectedItemWith(String playerId, com.google.gson.JsonObject actorState,
            ItemType expectedType, ItemStack replacement) {
        if (isCreativeMultiplayerPlayerState(actorState) || playerId == null || playerId.isBlank()
                || expectedType == null || replacement == null || replacement.isEmpty()) {
            return false;
        }
        int selectedSlot = Math.max(0, Math.min(Inventory.HOTBAR_SIZE - 1,
                jsonInt(actorState, "selectedSlot", 0)));
        Inventory inventory = multiplayerInventorySnapshotFor(playerId);
        ItemStack selected = multiplayerInventorySlot(inventory, selectedSlot);
        if (selected == null || selected.isEmpty() || selected.getType() != expectedType) {
            return false;
        }
        ItemStack[] before = snapshotPlayerInventoryStacks(inventory);
        ItemStack replacementCopy = replacement.copy();
        if (selected.getCount() <= 1) {
            applyLocalInventorySlot(inventory, selectedSlot, replacementCopy);
        } else {
            selected.remove(1);
            if (!inventory.addItem(replacementCopy) && !replacementCopy.isEmpty()) {
                dropMultiplayerReplacementStack(actorState, replacementCopy);
            }
        }
        broadcastChangedMultiplayerInventorySlots(playerId, inventory, before);
        return true;
    }

    private void dropMultiplayerReplacementStack(com.google.gson.JsonObject actorState, ItemStack stack) {
        if (world == null || stack == null || stack.isEmpty()) {
            return;
        }
        float x = jsonFloat(actorState, "x", 0.0f);
        float y = jsonFloat(actorState, "y", 80.0f) + (float) MultiplayerProtocol.PLAYER_EYE_HEIGHT;
        float z = jsonFloat(actorState, "z", 0.0f);
        world.spawnThrownStack(x, y, z, stack, 0.0f, 0.2f, 0.0f);
    }

    private boolean shouldDamageMultiplayerToolOnBlockBreak(ItemStack heldStack, BlockType blockType,
            ItemType heldType) {
        return heldStack != null
                && heldStack.isDamageable()
                && (heldStack.isTool()
                        || (heldType == ItemType.SHEARS && isMultiplayerShearsHarvestBlock(blockType)));
    }

    private boolean isMultiplayerShearsHarvestBlock(BlockType blockType) {
        return blockType == BlockType.COBWEB
                || blockType == BlockType.LEAVES
                || blockType == BlockType.TALL_GRASS
                || blockType == BlockType.VINES;
    }

    private void broadcastMultiplayerBlockState(int x, int y, int z) {
        if (multiplayerServer == null || world == null) {
            return;
        }
        BlockType current = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
        int metadata = world.getBlockMetadataIfLoaded(x, y, z, 0);
        String blockId = Integer.toString(current.getId());
        if (!MultiplayerProtocol.isValidBlockUpdate(blockId, y, metadata)) {
            return;
        }
        multiplayerServer.broadcastBlockUpdate(x, y, z, current.getId(), metadata,
                multiplayerBlockData(x, y, z, current));
    }

    private void applyNetworkTileEntityData(NetworkMessage message, BlockType type, int x, int y, int z) {
        if (world == null || message.data() == null || !message.data().has("tileType")) {
            return;
        }
        String tileType = messageString(message, "tileType", "");
        if (tileType.isBlank()) {
            return;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!networkTileMatches(tile, tileType)) {
            tile = createNetworkTileEntity(type, tileType, x, y, z);
            if (tile == null) {
                return;
            }
            world.putTileEntity(tile);
        }

        if (tile instanceof SignTileEntity sign) {
            for (int i = 0; i < 4; i++) {
                sign.setLine(i, messageString(message, "signLine" + i, ""));
            }
        } else if (tile instanceof ChestTileEntity chest) {
            applyTileInventory(message, "tile.inventory", chest.getInventory());
            chest.setLidAngle(messageFloat(message, "lidAngle", chest.getLidAngle()));
        } else if (tile instanceof FurnaceTileEntity furnace) {
            applyTileInventory(message, "tile.inventory", furnace.getInventory());
            furnace.setBurnTime(messageInt(message, "burnTime", furnace.getBurnTime()));
            furnace.setCurrentFuelBurnTime(messageInt(message, "currentFuelBurnTime",
                    furnace.getCurrentFuelBurnTime()));
            furnace.setCookTime(messageInt(message, "cookTime", furnace.getCookTime()));
            furnace.setTickAccumulator(messageFloat(message, "furnaceTickAccumulator",
                    furnace.getTickAccumulator()));
        } else if (tile instanceof BrewingStandTileEntity brewingStand) {
            applyTileInventory(message, "tile.inventory", brewingStand.getInventory());
            brewingStand.setBrewTime(messageInt(message, "brewTime", brewingStand.getBrewTime()));
            brewingStand.setTickAccumulator(messageFloat(message, "brewingTickAccumulator",
                    brewingStand.getTickAccumulator()));
        } else if (tile instanceof DispenserTileEntity dispenser) {
            applyTileInventory(message, "tile.inventory", dispenser.getInventory());
        } else if (tile instanceof NoteBlockTileEntity note) {
            note.setPitch(messageInt(message, "notePitch", note.getPitch()));
            note.setLastInstrument(messageInt(message, "noteInstrument", note.getLastInstrument()));
            note.setPlayTicks(messageInt(message, "notePlayTicks", note.getPlayTicks()));
        } else if (tile instanceof JukeboxTileEntity jukebox) {
            ItemStack record = itemStackFromBlockData(message, "record");
            if (jukebox.hasRecord()) {
                jukebox.removeRecord();
            }
            if (record != null && !record.isEmpty()) {
                jukebox.insertRecord(record);
            }
            jukebox.setPlayTicks(messageInt(message, "jukeboxPlayTicks", jukebox.getPlayTicks()));
        } else if (tile instanceof EnchantingTableTileEntity enchantingTable) {
            enchantingTable.setAnimationState(
                    messageInt(message, "enchantingTickCount", enchantingTable.getTickCount()),
                    messageFloat(message, "enchantingPageFlip", enchantingTable.getPageFlip()),
                    messageFloat(message, "enchantingPrevPageFlip", enchantingTable.getPrevPageFlip()),
                    messageFloat(message, "enchantingPageFlipTarget", enchantingTable.getPageFlipTarget()),
                    messageFloat(message, "enchantingPageFlipVelocity", enchantingTable.getPageFlipVelocity()),
                    messageFloat(message, "enchantingBookSpread", enchantingTable.getBookSpread()),
                    messageFloat(message, "enchantingPrevBookSpread", enchantingTable.getPrevBookSpread()),
                    messageFloat(message, "enchantingBookRotation", enchantingTable.getBookRotation()),
                    messageFloat(message, "enchantingBookRotation2", enchantingTable.getBookRotation2()),
                    messageFloat(message, "enchantingPrevBookRotation", enchantingTable.getPrevBookRotation()),
                    messageFloat(message, "enchantingTickAccumulator", enchantingTable.getTickAccumulator()));
        } else if (tile instanceof MonsterSpawnerTileEntity spawner) {
            String mobType = messageString(message, "mobType", spawner.getMobDefinition().name());
            try {
                spawner.setMobDefinition(MobDefinition.valueOf(mobType));
            } catch (IllegalArgumentException ignored) {
                spawner.setMobDefinition(MobDefinition.PIG);
            }
            spawner.setDelay(messageInt(message, "spawnDelay", spawner.getDelay()));
            spawner.setDelayRange(
                    messageInt(message, "minSpawnDelay", spawner.getMinDelay()),
                    messageInt(message, "maxSpawnDelay", spawner.getMaxDelay()));
            spawner.setSpawnCount(messageInt(message, "spawnCount", spawner.getSpawnCount()));
            spawner.setMaxNearbyEntities(messageInt(message, "maxNearbyEntities", spawner.getMaxNearbyEntities()));
            spawner.setTickAccumulator(messageFloat(message, "spawnerTickAccumulator",
                    spawner.getTickAccumulator()));
        }
        tile.clearDirty();
        world.rebuildBlockMeshesNow(x, y, z);
    }

    private boolean networkTileMatches(TileEntity tile, String tileType) {
        return tile != null && tile.getTypeId().equals(tileType);
    }

    private TileEntity createNetworkTileEntity(BlockType blockType, String tileType, int x, int y, int z) {
        if (blockType == null || tileType == null) {
            return null;
        }
        return switch (tileType) {
            case "chest" -> blockType == BlockType.CHEST ? new ChestTileEntity(x, y, z) : null;
            case "furnace" -> blockType.isFurnace() ? new FurnaceTileEntity(x, y, z) : null;
            case "brewing_stand" -> blockType == BlockType.BREWING_STAND
                    ? new BrewingStandTileEntity(x, y, z) : null;
            case "dispenser" -> blockType == BlockType.DISPENSER ? new DispenserTileEntity(x, y, z) : null;
            case "note_block" -> blockType == BlockType.NOTE_BLOCK ? new NoteBlockTileEntity(x, y, z) : null;
            case "jukebox" -> blockType == BlockType.JUKEBOX ? new JukeboxTileEntity(x, y, z) : null;
            case "enchanting_table" -> blockType == BlockType.ENCHANTING_TABLE
                    ? new EnchantingTableTileEntity(x, y, z) : null;
            case "sign" -> blockType.isSign() ? new SignTileEntity(x, y, z) : null;
            case "mob_spawner" -> blockType == BlockType.MOB_SPAWNER ? new MonsterSpawnerTileEntity(x, y, z) : null;
            default -> null;
        };
    }

    private void applyTileInventory(NetworkMessage message, String prefix, ItemStack[] inventory) {
        if (inventory == null || message.data() == null || !message.data().has(prefix + ".size")) {
            return;
        }
        int slots = Math.min(inventory.length, Math.max(0, messageInt(message, prefix + ".size", inventory.length)));
        for (int slot = 0; slot < inventory.length; slot++) {
            inventory[slot] = slot < slots ? itemStackFromBlockData(message, prefix + "." + slot) : null;
        }
    }

    private void applyNetworkInventoryUpdate(NetworkMessage message) {
        if (player == null || message.data() == null || !actionTargetsLocalMultiplayerClient(message)) {
            return;
        }
        if (!networkMessageTargetsCurrentDimension(message)) {
            return;
        }
        int slot = messageInt(message, "slot", -1);
        int count = messageInt(message, "count", 0);
        int damage = messageInt(message, "damage", -1);
        String itemId = message.data().has("itemId") ? message.data().get("itemId").getAsString() : "air";
        if (!MultiplayerProtocol.isValidInventoryUpdate(itemId, slot, count, damage)) {
            return;
        }
        ItemStack stack = inventoryStackFromNetworkMessage(message, itemId, count, damage);
        applyLocalInventorySlot(player.getInventory(), slot, stack);
        rememberMultiplayerInventorySlot(slot, stack);
    }

    private void applyNetworkWorldSound(NetworkMessage message) {
        if (world == null || message == null || message.data() == null) {
            return;
        }
        if (!networkMessageTargetsCurrentDimension(message)) {
            return;
        }
        String soundId = messageString(message, "soundId", "");
        if (soundId.isBlank()) {
            return;
        }
        world.playSound(
                soundId,
                messageFloat(message, "x", 0.0f),
                messageFloat(message, "y", 0.0f),
                messageFloat(message, "z", 0.0f),
                messageFloat(message, "volume", 1.0f),
                messageFloat(message, "pitch", 1.0f));
    }

    private void applyNetworkWorldParticle(NetworkMessage message) {
        if (world == null || message == null || message.data() == null) {
            return;
        }
        if (!networkMessageTargetsCurrentDimension(message)) {
            return;
        }
        WorldParticle.Type type = parseNetworkParticleType(messageString(message, "particleType", ""));
        if (type == null) {
            return;
        }
        boolean hasTarget = messageBoolean(message, "hasTarget", false);
        world.spawnNetworkParticle(
                type,
                messageFloat(message, "x", 0.0f),
                messageFloat(message, "y", 0.0f),
                messageFloat(message, "z", 0.0f),
                messageFloat(message, "motionX", 0.0f),
                messageFloat(message, "motionY", 0.0f),
                messageFloat(message, "motionZ", 0.0f),
                Math.max(0.01f, messageFloat(message, "scale", 0.2f)),
                Math.max(1, Math.min(messageInt(message, "lifetime", 16), 240)),
                messageFloat(message, "data", 0.0f),
                hasTarget,
                messageFloat(message, "targetX", messageFloat(message, "x", 0.0f)),
                messageFloat(message, "targetY", messageFloat(message, "y", 0.0f)),
                messageFloat(message, "targetZ", messageFloat(message, "z", 0.0f)));
    }

    private WorldParticle.Type parseNetworkParticleType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return WorldParticle.Type.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void applyNetworkWorldLightning(NetworkMessage message) {
        if (world == null || message == null || message.data() == null) {
            return;
        }
        if (!networkMessageTargetsCurrentDimension(message)) {
            return;
        }
        List<WorldLightningBolt.Segment> segments = lightningSegmentsFromMessage(message);
        if (segments.isEmpty()) {
            return;
        }
        List<WorldLightningBolt.FlashWindow> windows = lightningFlashWindowsFromMessage(message);
        int lifetime = Math.max(1, Math.min(messageInt(message, "lifetime", 8), 80));
        world.spawnNetworkLightning(WorldLightningBolt.fromNetwork(
                messageFloat(message, "x", 0.0f),
                messageFloat(message, "y", 0.0f),
                messageFloat(message, "z", 0.0f),
                segments,
                windows,
                lifetime));
    }

    private List<WorldLightningBolt.Segment> lightningSegmentsFromMessage(NetworkMessage message) {
        ArrayList<WorldLightningBolt.Segment> segments = new ArrayList<>();
        int count = Math.max(0, Math.min(messageInt(message, "segmentCount", 0), 32));
        for (int i = 0; i < count; i++) {
            String prefix = "segment." + i;
            segments.add(new WorldLightningBolt.Segment(
                    messageFloat(message, prefix + ".x1", 0.0f),
                    messageFloat(message, prefix + ".y1", 0.0f),
                    messageFloat(message, prefix + ".z1", 0.0f),
                    messageFloat(message, prefix + ".x2", 0.0f),
                    messageFloat(message, prefix + ".y2", 0.0f),
                    messageFloat(message, prefix + ".z2", 0.0f)));
        }
        return segments;
    }

    private List<WorldLightningBolt.FlashWindow> lightningFlashWindowsFromMessage(NetworkMessage message) {
        ArrayList<WorldLightningBolt.FlashWindow> windows = new ArrayList<>();
        int count = Math.max(0, Math.min(messageInt(message, "flashCount", 0), 8));
        for (int i = 0; i < count; i++) {
            String prefix = "flash." + i;
            windows.add(new WorldLightningBolt.FlashWindow(
                    messageFloat(message, prefix + ".start", 0.0f),
                    messageFloat(message, prefix + ".end", 1.0f)));
        }
        if (windows.isEmpty()) {
            windows.add(new WorldLightningBolt.FlashWindow(0.0f, 3.0f));
        }
        return windows;
    }

    private ItemStack inventoryStackFromNetworkMessage(NetworkMessage message, String itemId, int count, int damage) {
        ItemStack stack = networkItemStack(itemId, count, damage);
        applyItemStackExtraData(message, "stack", stack);
        return stack;
    }

    private ItemStack networkItemStack(String itemId, int count, int damage) {
        ItemType type = parseNetworkItemType(itemId, damage);
        if (type == null || count <= 0) {
            return null;
        }
        int clampedCount = Math.max(1,
                Math.min(count, Math.min(MultiplayerProtocol.MAX_STACK_COUNT, Math.max(1, type.getMaxStackSize()))));
        return damage >= 0 ? new ItemStack(type, clampedCount, damage) : new ItemStack(type, clampedCount);
    }

    private ItemStack itemStackFromBlockData(NetworkMessage message, String prefix) {
        if (message == null || message.data() == null) {
            return null;
        }
        String itemId = messageString(message, prefix + ".itemId", "air");
        int count = messageInt(message, prefix + ".count", 0);
        int damage = messageInt(message, prefix + ".damage", -1);
        ItemStack stack = networkItemStack(itemId, count, damage);
        if (stack == null) {
            return null;
        }
        applyItemStackExtraData(message, prefix, stack);
        return stack;
    }

    private void applyItemStackExtraData(NetworkMessage message, String prefix, ItemStack stack) {
        if (message == null || message.data() == null || stack == null) {
            return;
        }
        if (message.data().has(prefix + ".customName")) {
            stack.setCustomName(messageString(message, prefix + ".customName", ""));
        }
        if (message.data().has(prefix + ".potionType")) {
            stack.setPotionData(potionDataFromMessage(message, prefix + ".potion", false));
        }
        int enchantmentCount = messageInt(message, prefix + ".enchantmentCount", 0);
        if (enchantmentCount > 0) {
            java.util.ArrayList<EnchantmentInstance> enchantments = new java.util.ArrayList<>();
            for (int i = 0; i < enchantmentCount; i++) {
                String rawType = messageString(message, prefix + ".enchantment." + i + ".type", "");
                try {
                    EnchantmentType enchantmentType = EnchantmentType.valueOf(rawType);
                    int level = messageInt(message, prefix + ".enchantment." + i + ".level", 1);
                    enchantments.add(new EnchantmentInstance(enchantmentType, level));
                } catch (IllegalArgumentException ignored) {
                }
            }
            stack.setEnchantments(enchantments);
        }
        int metadataCount = messageInt(message, prefix + ".metadataCount", 0);
        if (metadataCount > 0) {
            HashMap<String, String> metadata = new HashMap<>();
            for (int i = 0; i < metadataCount; i++) {
                String key = messageString(message, prefix + ".metadata." + i + ".key", "");
                String value = messageString(message, prefix + ".metadata." + i + ".value", "");
                if (!key.isBlank()) {
                    metadata.put(key, value);
                }
            }
            stack.setMetadata(metadata);
        }
    }

    private void applyLocalInventorySlot(Inventory inventory, int slot, ItemStack stack) {
        if (inventory == null || slot < 0) {
            return;
        }
        if (slot < Inventory.HOTBAR_SIZE) {
            inventory.getHotbar()[slot] = stack;
            return;
        }
        int mainSlot = slot - Inventory.HOTBAR_SIZE;
        if (mainSlot >= 0 && mainSlot < Inventory.MAIN_SIZE) {
            inventory.getMainInventory()[mainSlot] = stack;
            return;
        }
        int craftingSlot = mainSlot - Inventory.MAIN_SIZE;
        if (craftingSlot >= 0 && craftingSlot < Inventory.CRAFTING_SIZE) {
            inventory.getCraftingGrid()[craftingSlot] = stack;
            return;
        }
        int armorSlot = craftingSlot - Inventory.CRAFTING_SIZE;
        if (armorSlot >= 0 && armorSlot < inventory.getArmor().length) {
            inventory.getArmor()[armorSlot] = stack;
            return;
        }
        if (armorSlot == inventory.getArmor().length) {
            inventory.setCursorItem(stack);
        }
    }

    private BlockType parseNetworkBlockType(String rawBlockId) {
        if (rawBlockId == null || rawBlockId.isBlank()) {
            return null;
        }
        String value = rawBlockId.trim();
        try {
            return BlockType.fromId(Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
        }
        String normalized = value.toUpperCase(java.util.Locale.ROOT)
                .replace("MINECRAFT:", "")
                .replace("CRAFTZERO:", "")
                .replace('-', '_')
                .replace(' ', '_');
        for (BlockType type : BlockType.values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return null;
    }

    private ItemType parseNetworkItemType(String rawItemId, int dataValue) {
        if (rawItemId == null || rawItemId.isBlank()) {
            return null;
        }
        String value = rawItemId.trim();
        if ("air".equalsIgnoreCase(value) || "minecraft:air".equalsIgnoreCase(value) || "0".equals(value)) {
            return null;
        }
        ItemType numeric = parseNetworkNumericItemType(value, dataValue);
        if (numeric != null) {
            return numeric;
        }
        String normalized = value.toUpperCase(java.util.Locale.ROOT)
                .replace("MINECRAFT:", "")
                .replace("CRAFTZERO:", "")
                .replace('-', '_')
                .replace(' ', '_');
        for (ItemType type : ItemType.values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return null;
    }

    private ItemType parseNetworkNumericItemType(String value, int dataValue) {
        String[] parts = value.split(":", 2);
        if (parts.length == 2 && !isIntegerText(parts[0])) {
            return null;
        }
        try {
            int id = Integer.parseInt(parts[0]);
            int itemData = parts.length == 2 ? Integer.parseInt(parts[1]) : Math.max(0, dataValue);
            return ItemType.fromId(id, itemData);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean isIntegerText(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        int start = value.charAt(0) == '-' ? 1 : 0;
        if (start == value.length()) {
            return false;
        }
        for (int i = start; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private void syncMultiplayerInventoryState() {
        syncMultiplayerInventoryState(false);
    }

    private void syncMultiplayerInventoryStateNow() {
        syncMultiplayerInventoryState(true);
    }

    private void syncMultiplayerInventoryState(boolean force) {
        if (player == null || player.getInventory() == null || !hasActiveMultiplayerSync()) {
            return;
        }
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < MULTIPLAYER_INVENTORY_SLOTS; slot++) {
            ItemStack stack = multiplayerInventorySlot(inventory, slot);
            String key = multiplayerInventoryKey(stack);
            if (force || !key.equals(multiplayerInventorySnapshot[slot])) {
                multiplayerInventorySnapshot[slot] = key;
                sendMultiplayerInventorySlot(slot, stack);
            }
        }
    }

    private boolean hasActiveMultiplayerSync() {
        return multiplayerServer != null
                || (clientMultiplayerWorld && multiplayerClient != null && multiplayerClient.isConnected());
    }

    private void sendMultiplayerInventorySlot(int slot, ItemStack stack) {
        String itemId = "air";
        int count = 0;
        int damage = 0;
        if (stack != null && !stack.isEmpty() && stack.getType() != null) {
            itemId = multiplayerItemId(stack);
            count = Math.max(0, Math.min(stack.getCount(), MultiplayerProtocol.MAX_STACK_COUNT));
            damage = Math.max(MultiplayerProtocol.MIN_ITEM_DAMAGE,
                    Math.min(stack.getDurability(), MultiplayerProtocol.MAX_ITEM_DAMAGE));
        }
        Map<String, String> stackData = multiplayerInventoryData(stack);
        if (multiplayerServer != null) {
            multiplayerServer.broadcastInventoryUpdate("host", slot, itemId, count, damage, stackData);
        }
        if (clientMultiplayerWorld && multiplayerClient != null && multiplayerClient.isConnected()) {
            try {
                multiplayerClient.sendInventoryUpdate(slot, itemId, count, damage, stackData);
            } catch (Exception e) {
                System.err.println("Could not sync multiplayer inventory update: " + e.getMessage());
            }
        }
    }

    private Map<String, String> multiplayerInventoryData(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getType() == null) {
            return Map.of();
        }
        HashMap<String, String> data = new HashMap<>();
        putItemStackData(data, "stack", stack);
        return data;
    }

    private String multiplayerInventoryKey(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getType() == null) {
            return "empty";
        }
        String customName = stack.getCustomName() == null ? "" : stack.getCustomName();
        int potionHash = stack.getPotionData() == null ? 0 : stack.getPotionData().hashCode();
        return multiplayerItemId(stack) + ':' + stack.getCount() + ':' + stack.getDurability()
                + ':' + customName.hashCode() + ':' + potionHash
                + ':' + stack.getMetadata().hashCode() + ':' + stack.getEnchantments().hashCode();
    }

    private String multiplayerItemId(ItemStack stack) {
        return itemTypeNetworkId(stack == null ? null : stack.getType());
    }

    private String itemTypeNetworkId(ItemType type) {
        if (type == null) {
            return "air";
        }
        int data = type.getDataValue();
        return data == 0 ? Integer.toString(type.getId()) : type.getId() + ":" + data;
    }

    private ItemStack multiplayerInventorySlot(Inventory inventory, int slot) {
        if (inventory == null || slot < 0) {
            return null;
        }
        if (slot < Inventory.HOTBAR_SIZE) {
            return inventory.getHotbar()[slot];
        }
        int mainSlot = slot - Inventory.HOTBAR_SIZE;
        if (mainSlot >= 0 && mainSlot < Inventory.MAIN_SIZE) {
            return inventory.getMainInventory()[mainSlot];
        }
        int craftingSlot = mainSlot - Inventory.MAIN_SIZE;
        if (craftingSlot >= 0 && craftingSlot < Inventory.CRAFTING_SIZE) {
            return inventory.getCraftingGrid()[craftingSlot];
        }
        int armorSlot = craftingSlot - Inventory.CRAFTING_SIZE;
        if (armorSlot >= 0 && armorSlot < MULTIPLAYER_ARMOR_SLOTS) {
            return inventory.getArmor()[armorSlot];
        }
        return slot == MULTIPLAYER_CURSOR_SLOT ? inventory.getCursorItem() : null;
    }

    private void rememberMultiplayerInventorySlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < multiplayerInventorySnapshot.length) {
            multiplayerInventorySnapshot[slot] = multiplayerInventoryKey(stack);
        }
    }

    private void resetMultiplayerInventorySnapshot() {
        Arrays.fill(multiplayerInventorySnapshot, "empty");
    }

    private void syncMultiplayerTileEntityState(float deltaTime) {
        if (multiplayerServer == null || world == null) {
            multiplayerTileTimer = 0.0f;
            return;
        }
        multiplayerTileTimer += deltaTime;
        if (multiplayerTileTimer < MULTIPLAYER_TILE_SYNC_INTERVAL) {
            return;
        }
        multiplayerTileTimer = 0.0f;

        for (TileEntity tile : List.copyOf(world.getTileEntities())) {
            if (tile == null || !tile.isDirty()) {
                continue;
            }
            broadcastMultiplayerTileEntity(tile);
        }
    }

    private void broadcastMultiplayerTileEntity(TileEntity tile) {
        if (multiplayerServer == null || world == null || tile == null) {
            return;
        }
        BlockPos pos = tile.getPos();
        BlockType type = world.getBlockIfLoaded(pos.x(), pos.y(), pos.z(), BlockType.AIR);
        if (!type.hasTileEntity()) {
            tile.clearDirty();
            return;
        }
        int metadata = world.getBlockMetadataIfLoaded(pos.x(), pos.y(), pos.z(), 0);
        multiplayerServer.broadcastBlockUpdate(
                pos.x(),
                pos.y(),
                pos.z(),
                type.getId(),
                metadata,
                multiplayerBlockData(pos.x(), pos.y(), pos.z(), type));
        tile.clearDirty();
    }

    private Map<String, String> multiplayerBlockData(int x, int y, int z, BlockType type) {
        if (world == null || type == null || !type.hasTileEntity()) {
            return Map.of();
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile == null) {
            return Map.of();
        }
        HashMap<String, String> data = new HashMap<>();
        data.put("tileType", tile.getTypeId());
        if (tile instanceof SignTileEntity sign) {
            String[] lines = sign.getLines();
            for (int i = 0; i < lines.length; i++) {
                data.put("signLine" + i, lines[i] == null ? "" : lines[i]);
            }
        } else if (tile instanceof ChestTileEntity chest) {
            putTileInventoryData(data, "tile.inventory", chest.getInventory());
            data.put("lidAngle", Float.toString(chest.getLidAngle()));
        } else if (tile instanceof FurnaceTileEntity furnace) {
            putTileInventoryData(data, "tile.inventory", furnace.getInventory());
            data.put("burnTime", Integer.toString(furnace.getBurnTime()));
            data.put("currentFuelBurnTime", Integer.toString(furnace.getCurrentFuelBurnTime()));
            data.put("cookTime", Integer.toString(furnace.getCookTime()));
            data.put("furnaceTickAccumulator", Float.toString(furnace.getTickAccumulator()));
        } else if (tile instanceof BrewingStandTileEntity brewingStand) {
            putTileInventoryData(data, "tile.inventory", brewingStand.getInventory());
            data.put("brewTime", Integer.toString(brewingStand.getBrewTime()));
            data.put("brewingTickAccumulator", Float.toString(brewingStand.getTickAccumulator()));
        } else if (tile instanceof DispenserTileEntity dispenser) {
            putTileInventoryData(data, "tile.inventory", dispenser.getInventory());
        } else if (tile instanceof NoteBlockTileEntity note) {
            data.put("notePitch", Integer.toString(note.getPitch()));
            data.put("noteInstrument", Integer.toString(note.getLastInstrument()));
            data.put("notePlayTicks", Integer.toString(note.getPlayTicks()));
        } else if (tile instanceof JukeboxTileEntity jukebox) {
            putItemStackData(data, "record", jukebox.getRecord());
            data.put("jukeboxPlayTicks", Integer.toString(jukebox.getPlayTicks()));
        } else if (tile instanceof EnchantingTableTileEntity enchantingTable) {
            data.put("enchantingTickCount", Integer.toString(enchantingTable.getTickCount()));
            data.put("enchantingPageFlip", Float.toString(enchantingTable.getPageFlip()));
            data.put("enchantingPrevPageFlip", Float.toString(enchantingTable.getPrevPageFlip()));
            data.put("enchantingPageFlipTarget", Float.toString(enchantingTable.getPageFlipTarget()));
            data.put("enchantingPageFlipVelocity", Float.toString(enchantingTable.getPageFlipVelocity()));
            data.put("enchantingBookSpread", Float.toString(enchantingTable.getBookSpread()));
            data.put("enchantingPrevBookSpread", Float.toString(enchantingTable.getPrevBookSpread()));
            data.put("enchantingBookRotation", Float.toString(enchantingTable.getBookRotation()));
            data.put("enchantingBookRotation2", Float.toString(enchantingTable.getBookRotation2()));
            data.put("enchantingPrevBookRotation", Float.toString(enchantingTable.getPrevBookRotation()));
            data.put("enchantingTickAccumulator", Float.toString(enchantingTable.getTickAccumulator()));
        } else if (tile instanceof MonsterSpawnerTileEntity spawner) {
            data.put("mobType", spawner.getMobDefinition().name());
            data.put("spawnDelay", Integer.toString(spawner.getDelay()));
            data.put("minSpawnDelay", Integer.toString(spawner.getMinDelay()));
            data.put("maxSpawnDelay", Integer.toString(spawner.getMaxDelay()));
            data.put("spawnCount", Integer.toString(spawner.getSpawnCount()));
            data.put("maxNearbyEntities", Integer.toString(spawner.getMaxNearbyEntities()));
            data.put("spawnerTickAccumulator", Float.toString(spawner.getTickAccumulator()));
        }
        return data;
    }

    private void putTileInventoryData(Map<String, String> data, String prefix, ItemStack[] inventory) {
        if (inventory == null) {
            data.put(prefix + ".size", "0");
            return;
        }
        data.put(prefix + ".size", Integer.toString(inventory.length));
        for (int slot = 0; slot < inventory.length; slot++) {
            putItemStackData(data, prefix + "." + slot, inventory[slot]);
        }
    }

    private void putItemStackData(Map<String, String> data, String prefix, ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getType() == null) {
            data.put(prefix + ".itemId", "air");
            data.put(prefix + ".count", "0");
            data.put(prefix + ".damage", "-1");
            return;
        }
        data.put(prefix + ".itemId", multiplayerItemId(stack));
        data.put(prefix + ".count", Integer.toString(Math.max(0,
                Math.min(stack.getCount(), MultiplayerProtocol.MAX_STACK_COUNT))));
        data.put(prefix + ".damage", Integer.toString(Math.max(MultiplayerProtocol.MIN_ITEM_DAMAGE,
                Math.min(stack.getDurability(), MultiplayerProtocol.MAX_ITEM_DAMAGE))));
        if (stack.getCustomName() != null && !stack.getCustomName().isBlank()) {
            data.put(prefix + ".customName", stack.getCustomName());
        }
        PotionData potion = stack.getPotionData();
        if (potion != null) {
            data.put(prefix + ".potionType", potion.type().name());
            data.put(prefix + ".potionSplash", Boolean.toString(potion.splash()));
            data.put(prefix + ".potionExtended", Boolean.toString(potion.extended()));
            data.put(prefix + ".potionEnhanced", Boolean.toString(potion.enhanced()));
        }
        data.put(prefix + ".enchantmentCount", Integer.toString(stack.getEnchantments().size()));
        for (int i = 0; i < stack.getEnchantments().size(); i++) {
            EnchantmentInstance enchantment = stack.getEnchantments().get(i);
            data.put(prefix + ".enchantment." + i + ".type", enchantment.type().name());
            data.put(prefix + ".enchantment." + i + ".level", Integer.toString(enchantment.level()));
        }
        data.put(prefix + ".metadataCount", Integer.toString(stack.getMetadata().size()));
        int metadataIndex = 0;
        for (Map.Entry<String, String> entry : stack.getMetadata().entrySet()) {
            data.put(prefix + ".metadata." + metadataIndex + ".key", entry.getKey());
            data.put(prefix + ".metadata." + metadataIndex + ".value", entry.getValue());
            metadataIndex++;
        }
    }

    private void syncMultiplayerEntityState(float deltaTime) {
        if (multiplayerServer == null || world == null) {
            multiplayerEntityTimer = 0.0f;
            return;
        }
        multiplayerEntityTimer += deltaTime;
        if (multiplayerEntityTimer < MULTIPLAYER_ENTITY_SYNC_INTERVAL) {
            return;
        }
        multiplayerEntityTimer = 0.0f;

        Set<String> currentIds = new HashSet<>();
        for (Entity entity : world.getEntities()) {
            if (!shouldSyncMultiplayerEntity(entity)) {
                continue;
            }
            String entityId = multiplayerEntityId(entity);
            currentIds.add(entityId);
            multiplayerServer.broadcastEntityUpdate(
                    entityId,
                    multiplayerEntityType(entity),
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    entity.getYaw(),
                    entity.getPitch(),
                    multiplayerEntityData(entity, false));
        }

        for (String previousId : new HashSet<>(lastMultiplayerEntityIds)) {
            if (!currentIds.contains(previousId)) {
                multiplayerServer.broadcastEntityUpdate(previousId, "removed",
                        0.0f, 0.0f, 0.0f, 0.0f, 0.0f, Map.of("removed", "true"));
                clearMultiplayerVehicleEntity(previousId);
            }
        }
        lastMultiplayerEntityIds.clear();
        lastMultiplayerEntityIds.addAll(currentIds);
        syncMultiplayerDroppedItemState();
    }

    private void updateMultiplayerRemotePickups(float deltaTime) {
        if (multiplayerServer == null || world == null) {
            multiplayerExperiencePickupCooldowns.clear();
            return;
        }
        tickMultiplayerExperienceCooldowns(deltaTime);
        for (com.google.gson.JsonObject state : multiplayerServer.playerStates().values()) {
            if (!isLiveRemotePlayerState(state)) {
                continue;
            }
            String playerId = jsonString(state, "playerId", "");
            if (playerId.isBlank()) {
                continue;
            }
            float x = jsonFloat(state, "x", 0.0f);
            float y = jsonFloat(state, "y", 80.0f);
            float z = jsonFloat(state, "z", 0.0f);
            collectMultiplayerRemoteDroppedItems(playerId, x, y, z, deltaTime);
            collectMultiplayerRemoteExperience(playerId, x, y, z, state);
        }
    }

    private void updateMultiplayerRemoteDamage(float deltaTime) {
        if (multiplayerServer == null || world == null) {
            multiplayerRemoteDamageCooldowns.clear();
            return;
        }
        tickMultiplayerRemoteDamageCooldowns(deltaTime);
        for (com.google.gson.JsonObject state : multiplayerServer.playerStates().values()) {
            if (!isLiveRemotePlayerState(state)) {
                continue;
            }
            String playerId = jsonString(state, "playerId", "");
            if (playerId.isBlank() || parseProtocolClientId(playerId) <= 0
                    || multiplayerRemoteDamageCooldowns.getOrDefault(playerId, 0.0f) > 0.0f) {
                continue;
            }
            AABB box = multiplayerRemotePlayerBox(
                    jsonFloat(state, "x", 0.0f),
                    jsonFloat(state, "y", 80.0f),
                    jsonFloat(state, "z", 0.0f));
            MultiplayerRemoteHazardContact contact = multiplayerRemoteHazardContact(box);
            if (contact == MultiplayerRemoteHazardContact.NONE) {
                continue;
            }
            sendMultiplayerRemoteHazardDamage(playerId, state, contact);
        }
    }

    private boolean isLiveRemotePlayerState(com.google.gson.JsonObject state) {
        return state != null
                && jsonFloat(state, "health", 20.0f) > 0.0f
                && !jsonString(state, "playerId", "").isBlank()
                && remotePlayerStateTargetsCurrentDimension(state);
    }

    private void tickMultiplayerExperienceCooldowns(float deltaTime) {
        if (multiplayerExperiencePickupCooldowns.isEmpty()) {
            return;
        }
        for (String playerId : new HashSet<>(multiplayerExperiencePickupCooldowns.keySet())) {
            float remaining = multiplayerExperiencePickupCooldowns.getOrDefault(playerId, 0.0f)
                    - Math.max(0.0f, deltaTime);
            if (remaining <= 0.0f) {
                multiplayerExperiencePickupCooldowns.remove(playerId);
            } else {
                multiplayerExperiencePickupCooldowns.put(playerId, remaining);
            }
        }
    }

    private void tickMultiplayerRemoteDamageCooldowns(float deltaTime) {
        if (multiplayerRemoteDamageCooldowns.isEmpty()) {
            return;
        }
        for (String playerId : new HashSet<>(multiplayerRemoteDamageCooldowns.keySet())) {
            float remaining = multiplayerRemoteDamageCooldowns.getOrDefault(playerId, 0.0f)
                    - Math.max(0.0f, deltaTime);
            if (remaining <= 0.0f) {
                multiplayerRemoteDamageCooldowns.remove(playerId);
            } else {
                multiplayerRemoteDamageCooldowns.put(playerId, remaining);
            }
        }
    }

    private AABB multiplayerRemotePlayerBox(float x, float y, float z) {
        float halfWidth = MULTIPLAYER_REMOTE_PLAYER_WIDTH * 0.5f;
        return new AABB(
                x - halfWidth, y, z - halfWidth,
                x + halfWidth, y + MULTIPLAYER_REMOTE_PLAYER_HEIGHT, z + halfWidth);
    }

    private MultiplayerRemoteHazardContact multiplayerRemoteHazardContact(AABB box) {
        if (box == null || world == null) {
            return MultiplayerRemoteHazardContact.NONE;
        }
        int minX = (int) Math.floor(box.getMin().x);
        int maxX = (int) Math.floor(box.getMax().x - 0.0001f);
        int minY = (int) Math.floor(box.getMin().y);
        int maxY = (int) Math.floor(box.getMax().y - 0.0001f);
        int minZ = (int) Math.floor(box.getMin().z);
        int maxZ = (int) Math.floor(box.getMax().z - 0.0001f);
        boolean fire = false;
        boolean cactus = false;
        boolean suffocation = false;
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    BlockType type = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
                    if (type.isLava()) {
                        return MultiplayerRemoteHazardContact.LAVA;
                    }
                    if (type == BlockType.FIRE) {
                        fire = true;
                    } else if (type == BlockType.CACTUS && multiplayerRemoteIntersectsBlockCollision(box, x, y, z)) {
                        cactus = true;
                    } else if (BlockShape.isOpaqueCube(type)
                            && multiplayerRemoteIntersectsBlockCollision(box, x, y, z)) {
                        suffocation = true;
                    }
                }
            }
        }
        if (!fire && !cactus && !suffocation) {
            return MultiplayerRemoteHazardContact.NONE;
        }
        return new MultiplayerRemoteHazardContact(fire, false, cactus, suffocation);
    }

    private boolean multiplayerRemoteIntersectsBlockCollision(AABB box, int x, int y, int z) {
        if (box == null || world == null) {
            return false;
        }
        for (AABB collision : world.getCollisionBoxesIfLoaded(x, y, z)) {
            if (box.intersects(collision)) {
                return true;
            }
        }
        return false;
    }

    private void sendMultiplayerRemoteHazardDamage(String playerId, com.google.gson.JsonObject state,
            MultiplayerRemoteHazardContact contact) {
        int clientId = parseProtocolClientId(playerId);
        if (clientId <= 0 || contact == null || contact == MultiplayerRemoteHazardContact.NONE) {
            return;
        }
        float amount;
        String damageType;
        int fireTicks = 0;
        if (contact.lava()) {
            amount = MULTIPLAYER_REMOTE_LAVA_CONTACT_DAMAGE;
            damageType = "fire";
            fireTicks = MULTIPLAYER_REMOTE_LAVA_CONTACT_TICKS;
        } else if (contact.fire()) {
            amount = MULTIPLAYER_REMOTE_FIRE_CONTACT_DAMAGE;
            damageType = "fire";
            fireTicks = MULTIPLAYER_REMOTE_FIRE_CONTACT_TICKS;
        } else if (contact.cactus()) {
            amount = MULTIPLAYER_REMOTE_CACTUS_CONTACT_DAMAGE;
            damageType = "generic";
        } else if (contact.suffocation()) {
            amount = MULTIPLAYER_REMOTE_SUFFOCATION_DAMAGE;
            damageType = "suffocation";
        } else {
            return;
        }
        float x = jsonFloat(state, "x", 0.0f);
        float y = jsonFloat(state, "y", 80.0f);
        float z = jsonFloat(state, "z", 0.0f);
        if (sendMultiplayerRemoteDamage(clientId, amount, damageType, x, y, z, 0.0f, 0.0f, fireTicks)) {
            multiplayerRemoteDamageCooldowns.put(playerId, MULTIPLAYER_REMOTE_DAMAGE_COOLDOWN_SECONDS);
        }
    }

    private boolean sendMultiplayerRemoteDamage(int clientId, float amount, String damageType,
            float sourceX, float sourceY, float sourceZ,
            float horizontalKnockback, float verticalKnockback, int fireTicks) {
        return sendMultiplayerRemoteDamage(clientId, amount, damageType, sourceX, sourceY, sourceZ,
                horizontalKnockback, verticalKnockback, fireTicks, "");
    }

    private boolean sendMultiplayerRemoteDamage(int clientId, float amount, String damageType,
            float sourceX, float sourceY, float sourceZ,
            float horizontalKnockback, float verticalKnockback, int fireTicks, String sourcePlayerId) {
        if (clientId <= 0 || amount <= 0.0f || damageType == null || damageType.isBlank()) {
            return false;
        }
        HashMap<String, String> data = new HashMap<>();
        data.put("amount", Float.toString(amount));
        data.put("damageType", damageType);
        data.put("sourceX", Float.toString(sourceX));
        data.put("sourceY", Float.toString(sourceY));
        data.put("sourceZ", Float.toString(sourceZ));
        if (horizontalKnockback > 0.0f) {
            data.put("horizontalKnockback", Float.toString(horizontalKnockback));
        }
        if (verticalKnockback > 0.0f) {
            data.put("verticalKnockback", Float.toString(verticalKnockback));
        }
        if (fireTicks > 0) {
            data.put("fireTicks", Integer.toString(fireTicks));
        }
        String sanitizedSourcePlayerId = sanitizeNetworkPlayerId(sourcePlayerId);
        if (!sanitizedSourcePlayerId.isBlank()) {
            data.put("sourcePlayerId", sanitizedSourcePlayerId);
        }
        return sendMultiplayerClientAction(clientId, MultiplayerProtocol.ACTION_COMMAND_DAMAGE, data);
    }

    private boolean sendMultiplayerRemoteVelocity(int clientId, float motionX, float motionY, float motionZ) {
        if (clientId <= 0) {
            return false;
        }
        float safeMotionX = clampMultiplayerVelocity(motionX);
        float safeMotionY = clampMultiplayerVelocity(motionY);
        float safeMotionZ = clampMultiplayerVelocity(motionZ);
        if (safeMotionX == 0.0f && safeMotionY == 0.0f && safeMotionZ == 0.0f) {
            return false;
        }
        HashMap<String, String> data = new HashMap<>();
        data.put("motionX", Float.toString(safeMotionX));
        data.put("motionY", Float.toString(safeMotionY));
        data.put("motionZ", Float.toString(safeMotionZ));
        return sendMultiplayerClientAction(clientId, MultiplayerProtocol.ACTION_COMMAND_VELOCITY, data);
    }

    private float clampMultiplayerVelocity(float motion) {
        if (!Float.isFinite(motion)) {
            return 0.0f;
        }
        return Math.max(-16.0f, Math.min(16.0f, motion));
    }

    private World.ProjectilePlayerHit findMultiplayerRemoteProjectilePlayerHit(org.joml.Vector3f origin,
            org.joml.Vector3f direction, float maxDistance) {
        return findMultiplayerRemoteProjectilePlayerHit(origin, direction, maxDistance, "");
    }

    private World.ProjectilePlayerHit findMultiplayerRemoteProjectilePlayerHit(org.joml.Vector3f origin,
            org.joml.Vector3f direction, float maxDistance, String ignoredPlayerId) {
        if (multiplayerServer == null || clientMultiplayerWorld || origin == null || direction == null
                || maxDistance <= 0.0f) {
            return World.ProjectilePlayerHit.miss();
        }
        String ignored = ignoredPlayerId == null ? "" : ignoredPlayerId;
        String closestPlayerId = "";
        float closestDistance = maxDistance;
        org.joml.Vector3f closestHitPoint = null;
        for (com.google.gson.JsonObject state : multiplayerServer.playerStates().values()) {
            if (!isLiveRemotePlayerState(state)) {
                continue;
            }
            String playerId = jsonString(state, "playerId", "");
            if (playerId.isBlank() || playerId.equals(ignored) || parseProtocolClientId(playerId) <= 0) {
                continue;
            }
            AABB box = Raycast.playerPickBox(multiplayerRemotePlayerBox(
                    jsonFloat(state, "x", 0.0f),
                    jsonFloat(state, "y", 80.0f),
                    jsonFloat(state, "z", 0.0f)));
            float distance = Raycast.intersectsAabb(origin, direction, box);
            if (distance >= 0.0f && distance < closestDistance) {
                closestDistance = distance;
                closestPlayerId = playerId;
                closestHitPoint = Raycast.pointAt(origin, direction, distance);
            }
        }
        return closestHitPoint == null
                ? World.ProjectilePlayerHit.miss()
                : new World.ProjectilePlayerHit(closestPlayerId, closestHitPoint, closestDistance);
    }

    private World.RemotePlayerTarget nearestMultiplayerRemoteTarget(float sourceX, float sourceY, float sourceZ,
            float range, boolean requireSight) {
        List<World.RemotePlayerTarget> targets = multiplayerRemoteTargets(sourceX, sourceY, sourceZ,
                range, requireSight);
        return targets.isEmpty() ? null : targets.get(0);
    }

    private List<World.RemotePlayerTarget> multiplayerRemoteTargets(float sourceX, float sourceY, float sourceZ,
            float range, boolean requireSight) {
        if (multiplayerServer == null || clientMultiplayerWorld || world == null || range <= 0.0f
                || !currentDifficulty.allowsHostileSpawns()) {
            return List.of();
        }
        ArrayList<World.RemotePlayerTarget> targets = new ArrayList<>();
        float maxDistanceSq = range * range;
        for (com.google.gson.JsonObject state : multiplayerServer.playerStates().values()) {
            if (!isTargetableRemotePlayerState(state)) {
                continue;
            }
            World.RemotePlayerTarget target = multiplayerRemoteTargetFromState(state, sourceX, sourceY, sourceZ);
            if (target == null || !target.valid()) {
                continue;
            }
            float distanceSq = target.distance() * target.distance();
            if (distanceSq > maxDistanceSq) {
                continue;
            }
            if (requireSight && !LineOfSightUtil.hasLineOfSight(world, sourceX, sourceY, sourceZ,
                    target.x(), target.eyeY(), target.z())) {
                continue;
            }
            targets.add(target);
        }
        targets.sort(java.util.Comparator.comparingDouble(World.RemotePlayerTarget::distance));
        return targets;
    }

    private List<World.RemotePlayerTarget> multiplayerRemotePlayerViews(float sourceX, float sourceY, float sourceZ,
            float range, boolean requireSight) {
        if (multiplayerServer == null || clientMultiplayerWorld || world == null || range <= 0.0f) {
            return List.of();
        }
        ArrayList<World.RemotePlayerTarget> views = new ArrayList<>();
        float maxDistanceSq = range * range;
        for (com.google.gson.JsonObject state : multiplayerServer.playerStates().values()) {
            if (!isLiveRemotePlayerState(state)) {
                continue;
            }
            World.RemotePlayerTarget view = multiplayerRemotePlayerViewFromState(state, sourceX, sourceY, sourceZ);
            if (view == null || !view.valid()) {
                continue;
            }
            float distanceSq = view.distance() * view.distance();
            if (distanceSq > maxDistanceSq) {
                continue;
            }
            if (requireSight && !LineOfSightUtil.hasLineOfSight(world, sourceX, sourceY, sourceZ,
                    view.x(), view.eyeY(), view.z())) {
                continue;
            }
            views.add(view);
        }
        views.sort(java.util.Comparator.comparingDouble(World.RemotePlayerTarget::distance));
        return views;
    }

    private World.RemotePlayerTarget multiplayerRemoteTargetById(String playerId,
            float sourceX, float sourceY, float sourceZ) {
        if (multiplayerServer == null || clientMultiplayerWorld || playerId == null || playerId.isBlank()
                || !currentDifficulty.allowsHostileSpawns()) {
            return null;
        }
        com.google.gson.JsonObject state = currentDimensionMultiplayerPlayerStateById(playerId);
        if (!isTargetableRemotePlayerState(state)) {
            return null;
        }
        return multiplayerRemoteTargetFromState(state, sourceX, sourceY, sourceZ);
    }

    private World.RemotePlayerTarget multiplayerRemotePlayerViewById(String playerId,
            float sourceX, float sourceY, float sourceZ) {
        if (multiplayerServer == null || clientMultiplayerWorld || playerId == null || playerId.isBlank()) {
            return null;
        }
        com.google.gson.JsonObject state = currentDimensionMultiplayerPlayerStateById(playerId);
        if (!isLiveRemotePlayerState(state)) {
            return null;
        }
        return multiplayerRemotePlayerViewFromState(state, sourceX, sourceY, sourceZ);
    }

    private World.RemotePlayerTarget multiplayerRemoteTargetFromState(com.google.gson.JsonObject state,
            float sourceX, float sourceY, float sourceZ) {
        if (!isTargetableRemotePlayerState(state)) {
            return null;
        }
        return multiplayerRemotePlayerViewFromState(state, sourceX, sourceY, sourceZ);
    }

    private World.RemotePlayerTarget multiplayerRemotePlayerViewFromState(com.google.gson.JsonObject state,
            float sourceX, float sourceY, float sourceZ) {
        if (!isLiveRemotePlayerState(state)) {
            return null;
        }
        String playerId = jsonString(state, "playerId", "");
        String username = jsonString(state, "username", playerId);
        float x = jsonFloat(state, "x", 0.0f);
        float y = jsonFloat(state, "y", 80.0f);
        float z = jsonFloat(state, "z", 0.0f);
        float eyeY = y + (jsonBoolean(state, "sneaking", false) ? 1.495f
                : (float) MultiplayerProtocol.PLAYER_EYE_HEIGHT);
        float yaw = jsonFloat(state, "yaw", 0.0f);
        float pitch = jsonFloat(state, "pitch", 0.0f);
        ItemType heldItem = jsonInt(state, "heldItemCount", 0) > 0
                ? parseNetworkItemType(jsonString(state, "heldItemId", "air"),
                        jsonInt(state, "heldItemDamage", -1))
                : null;
        float dx = x - sourceX;
        float dy = y - sourceY;
        float dz = z - sourceZ;
        return new World.RemotePlayerTarget(playerId, x, y, z, eyeY,
                MULTIPLAYER_REMOTE_PLAYER_HEIGHT,
                (float) Math.sqrt(dx * dx + dy * dy + dz * dz),
                yaw, pitch, isRemotePlayerWearingPumpkinHelmet(state), heldItem, username);
    }

    private boolean isRemotePlayerWearingPumpkinHelmet(com.google.gson.JsonObject state) {
        if (state == null) {
            return false;
        }
        int helmetSlot = ArmorSlot.HELMET.getIndex();
        ItemType helmet = parseNetworkItemType(
                jsonString(state, "armor." + helmetSlot + ".itemId", "air"),
                jsonInt(state, "armor." + helmetSlot + ".damage", -1));
        return helmet == ItemType.PUMPKIN;
    }

    private boolean isTargetableRemotePlayerState(com.google.gson.JsonObject state) {
        if (!isLiveRemotePlayerState(state) || !currentDifficulty.allowsHostileSpawns()) {
            return false;
        }
        return !parseNetworkGameMode(jsonString(state, "gameMode", "SURVIVAL")).isCreative();
    }

    private boolean damageMultiplayerRemoteTarget(String playerId, World.RemotePlayerDamage damage) {
        if (damage == null || playerId == null || playerId.isBlank()) {
            return false;
        }
        int clientId = parseProtocolClientId(playerId);
        boolean sent = sendMultiplayerRemoteDamage(clientId, damage.amount(), damage.damageType(),
                damage.sourceX(), damage.sourceY(), damage.sourceZ(),
                damage.horizontalKnockback(), damage.verticalKnockback(), damage.fireTicks());
        if (sent) {
            notifyRemoteOwnedWolvesOfOwnerDamage(playerId, damage);
        }
        return sent;
    }

    private boolean pullMultiplayerRemoteTarget(String playerId, float motionX, float motionY, float motionZ) {
        if (playerId == null || playerId.isBlank()) {
            return false;
        }
        int clientId = parseProtocolClientId(playerId);
        com.google.gson.JsonObject state = currentDimensionMultiplayerPlayerStateById(playerId);
        if (clientId <= 0 || !isLiveRemotePlayerState(state)) {
            return false;
        }
        return sendMultiplayerRemoteVelocity(clientId, motionX, motionY, motionZ);
    }

    private void notifyRemoteOwnedWolvesOfOwnerDamage(String playerId, World.RemotePlayerDamage damage) {
        if (world == null || damage == null || playerId == null || playerId.isBlank()
                || !"mob_melee".equalsIgnoreCase(damage.damageType())) {
            return;
        }
        String username = multiplayerUsernameForPlayerId(playerId);
        if (username == null || username.isBlank()) {
            return;
        }
        LivingEntity attacker = nearestLivingDamageSource(damage.sourceX(), damage.sourceY(), damage.sourceZ());
        if (attacker == null || attacker.isDead() || attacker.isRemoved()) {
            return;
        }
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Wolf wolf
                    && wolf.isOwnedByName(username)
                    && wolf.distanceToSquared(attacker) <= Wolf.ASSIST_RANGE * Wolf.ASSIST_RANGE) {
                wolf.setAssistTarget(attacker);
            }
        }
    }

    private LivingEntity nearestLivingDamageSource(float sourceX, float sourceY, float sourceZ) {
        if (world == null || !Float.isFinite(sourceX) || !Float.isFinite(sourceY) || !Float.isFinite(sourceZ)) {
            return null;
        }
        LivingEntity closest = null;
        float closestDistanceSq = 6.0f * 6.0f;
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || living.isDead() || living.isRemoved()) {
                continue;
            }
            float dx = living.getX() - sourceX;
            float dy = living.getY() - sourceY;
            float dz = living.getZ() - sourceZ;
            float distanceSq = dx * dx + dy * dy + dz * dz;
            if (distanceSq < closestDistanceSq) {
                closestDistanceSq = distanceSq;
                closest = living;
            }
        }
        return closest;
    }

    private String multiplayerUsernameForPlayerId(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return "";
        }
        com.google.gson.JsonObject state = currentDimensionMultiplayerPlayerStateById(playerId);
        String username = jsonString(state, "username", "");
        if ((username == null || username.isBlank()) && multiplayerServer != null) {
            username = multiplayerServer.usernameForClient(parseProtocolClientId(playerId));
        }
        return username == null || username.isBlank() ? playerId : username.trim();
    }

    private boolean applyStatusEffectMultiplayerRemoteTarget(String playerId, StatusEffectInstance effect) {
        if (effect == null || effect.expired() || playerId == null || playerId.isBlank()) {
            return false;
        }
        int clientId = parseProtocolClientId(playerId);
        com.google.gson.JsonObject state = currentDimensionMultiplayerPlayerStateById(playerId);
        if (clientId <= 0 || !isLiveRemotePlayerState(state)) {
            return false;
        }
        StatusEffectInstance bounded = boundedMultiplayerStatusEffect(effect);
        if (bounded == null || !sendMultiplayerStatusEffect(clientId, bounded)) {
            return false;
        }
        addHostedClientStatusEffect(state, bounded);
        return true;
    }

    private boolean damageMultiplayerRemoteProjectilePlayer(World.ProjectilePlayerHit hit,
            World.ProjectilePlayerDamage damage) {
        if (hit == null || !hit.hit() || damage == null) {
            return false;
        }
        int clientId = parseProtocolClientId(hit.playerId());
        return sendMultiplayerRemoteDamage(clientId, damage.amount(), damage.damageType(),
                damage.sourceX(), damage.sourceY(), damage.sourceZ(),
                damage.horizontalKnockback(), damage.verticalKnockback(), damage.fireTicks(),
                damage.sourcePlayerId());
    }

    private void splashMultiplayerRemotePotionPlayers(float x, float y, float z, PotionData potion,
            String directHitPlayerId) {
        if (multiplayerServer == null || clientMultiplayerWorld || potion == null) {
            return;
        }
        String directId = directHitPlayerId == null ? "" : directHitPlayerId;
        for (com.google.gson.JsonObject state : multiplayerServer.playerStates().values()) {
            if (!isLiveRemotePlayerState(state)) {
                continue;
            }
            String playerId = jsonString(state, "playerId", "");
            int clientId = parseProtocolClientId(playerId);
            if (clientId <= 0) {
                continue;
            }
            float strength = playerId.equals(directId)
                    ? 1.0f
                    : multiplayerPotionSplashStrength(x, y, z,
                            jsonFloat(state, "x", 0.0f),
                            jsonFloat(state, "y", 80.0f) + 1.0f,
                            jsonFloat(state, "z", 0.0f));
            if (strength <= 0.0f) {
                continue;
            }
            sendMultiplayerPotionEffect(clientId, potion, strength);
        }
    }

    private float multiplayerPotionSplashStrength(float splashX, float splashY, float splashZ,
            float targetX, float targetY, float targetZ) {
        float dx = targetX - splashX;
        float dy = targetY - splashY;
        float dz = targetZ - splashZ;
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance > 4.0f) {
            return 0.0f;
        }
        return Math.max(0.0f, 1.0f - distance / 4.0f);
    }

    private void sendMultiplayerPotionEffect(int clientId, PotionData potion, float strength) {
        if (clientId <= 0 || potion == null || strength <= 0.0f) {
            return;
        }
        HashMap<String, String> data = new HashMap<>();
        data.put("potionType", potion.type().name());
        data.put("potionSplash", Boolean.toString(true));
        data.put("potionExtended", Boolean.toString(potion.extended()));
        data.put("potionEnhanced", Boolean.toString(potion.enhanced()));
        data.put("strength", Float.toString(Math.max(0.0f, Math.min(1.0f, strength))));
        sendMultiplayerClientAction(clientId, MultiplayerProtocol.ACTION_COMMAND_POTION_EFFECT, data);
    }

    private boolean sendMultiplayerStatusEffect(int clientId, StatusEffectInstance effect) {
        StatusEffectInstance bounded = boundedMultiplayerStatusEffect(effect);
        if (clientId <= 0 || bounded == null) {
            return false;
        }
        HashMap<String, String> data = new HashMap<>();
        data.put("effectType", bounded.type().name());
        data.put("duration", Integer.toString(bounded.durationTicks()));
        data.put("amplifier", Integer.toString(bounded.amplifier()));
        return sendMultiplayerClientAction(clientId, MultiplayerProtocol.ACTION_COMMAND_POTION_EFFECT, data);
    }

    private StatusEffectInstance boundedMultiplayerStatusEffect(StatusEffectInstance effect) {
        if (effect == null || effect.expired()) {
            return null;
        }
        int duration = Math.max(0, Math.min(effect.durationTicks(),
                MultiplayerProtocol.MAX_CLIENT_STATUS_EFFECT_DURATION));
        int amplifier = Math.max(0, Math.min(effect.amplifier(),
                MultiplayerProtocol.MAX_CLIENT_STATUS_EFFECT_AMPLIFIER));
        return duration <= 0 ? null : new StatusEffectInstance(effect.type(), duration, amplifier);
    }

    private void applyMultiplayerRemoteExplosionDamage(float explosionX, float explosionY, float explosionZ,
            float power) {
        if (multiplayerServer == null || clientMultiplayerWorld || world == null
                || !allFinite(explosionX, explosionY, explosionZ, power) || power <= 0.0f) {
            return;
        }
        float entityRadius = power * MULTIPLAYER_EXPLOSION_ENTITY_RADIUS_MULTIPLIER;
        for (com.google.gson.JsonObject state : multiplayerServer.playerStates().values()) {
            if (!isLiveRemotePlayerState(state)) {
                continue;
            }
            String playerId = jsonString(state, "playerId", "");
            int clientId = parseProtocolClientId(playerId);
            if (clientId <= 0) {
                continue;
            }
            float playerX = jsonFloat(state, "x", 0.0f);
            float playerY = jsonFloat(state, "y", 80.0f);
            float playerZ = jsonFloat(state, "z", 0.0f);
            float distance = multiplayerDistanceFrom(playerX, playerY, playerZ, explosionX, explosionY, explosionZ);
            if (distance > entityRadius) {
                continue;
            }
            AABB box = multiplayerRemotePlayerBox(playerX, playerY, playerZ);
            float exposure = ExplosionExposure.sample(world, explosionX, explosionY, explosionZ, box);
            float impact = multiplayerExplosionImpact(distance, entityRadius, exposure);
            float damage = CombatRules.easyExplosionDamage(multiplayerExplosionDamage(entityRadius, impact));
            if (damage <= 0.0f) {
                continue;
            }
            MultiplayerExplosionPush push = multiplayerExplosionPush(playerX, playerY, playerZ,
                    explosionX, explosionY, explosionZ, impact);
            sendMultiplayerRemoteDamage(clientId, damage, "explosion",
                    explosionX, explosionY, explosionZ,
                    push.horizontalLength(), Math.max(0.0f, push.y()), 0);
        }
    }

    private void applyMultiplayerRemoteLightningDamage(float x, float y, float z) {
        if (multiplayerServer == null || clientMultiplayerWorld || world == null) {
            return;
        }
        AABB strikeBox = new AABB(
                x - MULTIPLAYER_LIGHTNING_ENTITY_RADIUS,
                y - MULTIPLAYER_LIGHTNING_ENTITY_RADIUS,
                z - MULTIPLAYER_LIGHTNING_ENTITY_RADIUS,
                x + MULTIPLAYER_LIGHTNING_ENTITY_RADIUS,
                y + MULTIPLAYER_LIGHTNING_ENTITY_RADIUS * 2.0f,
                z + MULTIPLAYER_LIGHTNING_ENTITY_RADIUS);
        for (com.google.gson.JsonObject state : multiplayerServer.playerStates().values()) {
            if (!isLiveRemotePlayerState(state)) {
                continue;
            }
            int clientId = parseProtocolClientId(jsonString(state, "playerId", ""));
            if (clientId <= 0) {
                continue;
            }
            AABB playerBox = multiplayerRemotePlayerBox(
                    jsonFloat(state, "x", 0.0f),
                    jsonFloat(state, "y", 80.0f),
                    jsonFloat(state, "z", 0.0f));
            if (!playerBox.intersects(strikeBox)) {
                continue;
            }
            sendMultiplayerRemoteDamage(clientId, MULTIPLAYER_LIGHTNING_DAMAGE, "lightning",
                    x, y, z, 0.0f, 0.0f, MULTIPLAYER_LIGHTNING_FIRE_TICKS);
        }
    }

    private static float multiplayerDistanceFrom(float entityX, float entityY, float entityZ,
            float sourceX, float sourceY, float sourceZ) {
        if (!allFinite(entityX, entityY, entityZ, sourceX, sourceY, sourceZ)) {
            return Float.MAX_VALUE;
        }
        float dx = entityX - sourceX;
        float dy = entityY - sourceY;
        float dz = entityZ - sourceZ;
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        return Float.isFinite(distance) ? distance : Float.MAX_VALUE;
    }

    private static float multiplayerExplosionImpact(float distance, float entityRadius, float exposure) {
        if (!allFinite(distance, entityRadius, exposure) || entityRadius <= 0.0f) {
            return 0.0f;
        }
        return Math.max(0.0f, 1.0f - distance / entityRadius) * clamp01(exposure);
    }

    private static float multiplayerExplosionDamage(float entityRadius, float impact) {
        if (!allFinite(entityRadius, impact) || entityRadius <= 0.0f || impact <= 0.0f) {
            return 0.0f;
        }
        return ((impact * impact + impact) * 0.5f * MULTIPLAYER_EXPLOSION_ENTITY_DAMAGE_SCALE * entityRadius) + 1.0f;
    }

    private static MultiplayerExplosionPush multiplayerExplosionPush(float entityX, float entityY, float entityZ,
            float sourceX, float sourceY, float sourceZ, float impact) {
        if (!allFinite(entityX, entityY, entityZ, sourceX, sourceY, sourceZ, impact) || impact <= 0.0f) {
            return MultiplayerExplosionPush.NONE;
        }
        float dx = entityX - sourceX;
        float dy = entityY - sourceY;
        float dz = entityZ - sourceZ;
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (!Float.isFinite(length) || length <= 0.0001f) {
            return MultiplayerExplosionPush.NONE;
        }
        return new MultiplayerExplosionPush(dx / length * impact, dy / length * impact, dz / length * impact);
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static boolean allFinite(float... values) {
        if (values == null) {
            return false;
        }
        for (float value : values) {
            if (!Float.isFinite(value)) {
                return false;
            }
        }
        return true;
    }

    private void collectMultiplayerRemoteDroppedItems(String playerId, float playerX, float playerY, float playerZ,
            float deltaTime) {
        if (playerId == null || playerId.isBlank() || world == null || multiplayerServer == null) {
            return;
        }
        java.util.Iterator<DroppedItem> iterator = world.getDroppedItems().iterator();
        while (iterator.hasNext()) {
            DroppedItem item = iterator.next();
            if (item == null || item.isDestroyed() || !item.canPickup()) {
                continue;
            }
            float dx = item.getX() - playerX;
            float dy = item.getY() - playerY;
            float dz = item.getZ() - playerZ;
            if (dx * dx + dy * dy + dz * dz > 9.0f) {
                continue;
            }
            ItemStack available = item.toItemStack();
            int addable = countAddableToMultiplayerInventory(playerId, available);
            if (addable <= 0 || !item.tryCollect(playerX, playerY, playerZ, deltaTime)) {
                continue;
            }
            ItemStack transfer = item.toItemStack();
            transfer.setCount(Math.min(addable, item.getCount()));
            int beforeTransfer = transfer.getCount();
            int moved = addStackToMultiplayerInventory(playerId, transfer);
            if (moved <= 0) {
                continue;
            }
            item.splitOff(Math.min(moved, beforeTransfer));
            world.playItemPickupSound(item.getX(), item.getY(), item.getZ());
            world.spawnItemPickupParticle(item.getItemType(),
                    item.getX(), item.getY() + 0.2f, item.getZ(),
                    playerX, playerY, playerZ);
            if (item.getCount() <= 0) {
                iterator.remove();
            }
        }
    }

    private void collectMultiplayerRemoteExperience(String playerId, float playerX, float playerY, float playerZ,
            com.google.gson.JsonObject playerState) {
        if (playerId == null || playerId.isBlank() || world == null || multiplayerServer == null
                || multiplayerExperiencePickupCooldowns.getOrDefault(playerId, 0.0f) > 0.0f) {
            return;
        }
        float eyeY = playerY + (jsonBoolean(playerState, "sneaking", false) ? 1.495f : (float) MultiplayerProtocol.PLAYER_EYE_HEIGHT);
        for (Entity entity : List.copyOf(world.getEntities())) {
            if (!(entity instanceof ExperienceOrbEntity orb) || orb.isRemoved() || orb.getPickupDelayTicks() > 0) {
                continue;
            }
            float dx = playerX - orb.getX();
            float dy = eyeY - orb.getY();
            float dz = playerZ - orb.getZ();
            if (dx * dx + dy * dy + dz * dz > 1.0f) {
                continue;
            }
            sendMultiplayerExperiencePickup(playerId, orb.getValue());
            world.playExperiencePickupSound(orb.getX(), orb.getY(), orb.getZ());
            orb.remove();
            multiplayerExperiencePickupCooldowns.put(playerId, 2.0f / 20.0f);
            break;
        }
    }

    private int countAddableToMultiplayerInventory(String playerId, ItemStack stack) {
        Inventory inventory = multiplayerInventorySnapshotFor(playerId);
        return inventory.countAddable(stack);
    }

    private int addStackToMultiplayerInventory(String playerId, ItemStack stack) {
        if (playerId == null || playerId.isBlank() || stack == null || stack.isEmpty()) {
            return 0;
        }
        Inventory inventory = multiplayerInventorySnapshotFor(playerId);
        ItemStack[] before = snapshotPlayerInventoryStacks(inventory);
        int requested = stack.getCount();
        inventory.addItem(stack);
        int moved = requested - stack.getCount();
        if (moved > 0) {
            broadcastChangedMultiplayerInventorySlots(playerId, inventory, before);
        }
        return moved;
    }

    private Inventory multiplayerInventorySnapshotFor(String playerId) {
        Inventory inventory = new Inventory();
        if (playerId == null || playerId.isBlank() || multiplayerServer == null) {
            return inventory;
        }
        for (int slot = 0; slot < MULTIPLAYER_INVENTORY_SLOTS; slot++) {
            NetworkMessage update = multiplayerServer.inventoryState(playerId, slot);
            if (update == null || update.data() == null) {
                continue;
            }
            ItemStack stack = inventoryStackFromNetworkMessage(
                    update,
                    messageString(update, "itemId", "air"),
                    messageInt(update, "count", 0),
                    messageInt(update, "damage", -1));
            applyLocalInventorySlot(inventory, slot, stack);
        }
        return inventory;
    }

    private ItemStack[] snapshotPlayerInventoryStacks(Inventory inventory) {
        ItemStack[] snapshot = new ItemStack[MULTIPLAYER_INVENTORY_SLOTS];
        for (int slot = 0; slot < MULTIPLAYER_INVENTORY_SLOTS; slot++) {
            ItemStack stack = multiplayerInventorySlot(inventory, slot);
            snapshot[slot] = stack == null ? null : stack.copy();
        }
        return snapshot;
    }

    private void broadcastChangedMultiplayerInventorySlots(String playerId, Inventory inventory, ItemStack[] before) {
        if (multiplayerServer == null || playerId == null || playerId.isBlank()) {
            return;
        }
        for (int slot = 0; slot < MULTIPLAYER_INVENTORY_SLOTS; slot++) {
            ItemStack current = multiplayerInventorySlot(inventory, slot);
            ItemStack previous = before != null && slot < before.length ? before[slot] : null;
            if (multiplayerInventoryKey(current).equals(multiplayerInventoryKey(previous))) {
                continue;
            }
            String itemId = current == null || current.isEmpty() ? "air" : multiplayerItemId(current);
            int count = current == null || current.isEmpty() ? 0 : current.getCount();
            int damage = current == null || current.isEmpty() ? 0 : current.getDurability();
            multiplayerServer.broadcastInventoryUpdate(playerId, slot, itemId, count, damage,
                    multiplayerInventoryData(current));
        }
    }

    private void sendMultiplayerExperiencePickup(String playerId, int amount) {
        int clientId = parseProtocolClientId(playerId);
        if (clientId <= 0 || amount <= 0) {
            return;
        }
        sendMultiplayerClientAction(clientId, MultiplayerProtocol.ACTION_COMMAND_EXPERIENCE,
                Map.of("amount", Integer.toString(amount), "pickup", "true"));
    }

    private boolean shouldSyncMultiplayerEntity(Entity entity) {
        return entity != null && !entity.isRemoved()
                && (entity instanceof Mob
                        || entity instanceof ArrowEntity
                        || entity instanceof FireballEntity
                        || entity instanceof FishingHookEntity
                        || entity instanceof EyeOfEnderEntity
                        || entity instanceof EnderPearlEntity
                        || entity instanceof EndCrystalEntity
                        || entity instanceof SplashPotionEntity
                        || entity instanceof ThrownItemEntity
                        || entity instanceof ExperienceOrbEntity
                        || entity instanceof FallingBlockEntity
                        || entity instanceof MinecartEntity
                        || entity instanceof BoatEntity
                        || entity instanceof PaintingEntity
                        || entity instanceof PrimedTntEntity);
    }

    private String multiplayerEntityId(Entity entity) {
        return multiplayerEntityIds.computeIfAbsent(entity, ignored -> "entity-" + nextMultiplayerEntityId++);
    }

    private String multiplayerFishingHookOwnerId(FishingHookEntity hook) {
        if (hook == null) {
            return "";
        }
        String remoteOwnerPlayerId = sanitizeNetworkPlayerId(hook.getRemoteOwnerPlayerId());
        if (!remoteOwnerPlayerId.isBlank()) {
            return remoteOwnerPlayerId;
        }
        for (Map.Entry<String, FishingHookEntity> entry : multiplayerFishingHooks.entrySet()) {
            if (entry.getValue() == hook) {
                return entry.getKey();
            }
        }
        return hook.getOwner() == player ? "host" : "";
    }

    private String multiplayerFishingHookedEntityId(FishingHookEntity hook) {
        if (hook == null) {
            return "";
        }
        Entity hooked = hook.getHookedEntity();
        if (hooked == null || hooked.isRemoved() || !shouldSyncMultiplayerEntity(hooked)) {
            return "";
        }
        return multiplayerEntityId(hooked);
    }

    private String multiplayerProjectileShooterEntityId(Entity projectile, Entity shooter) {
        if (projectile == null || shooter == null || projectile == shooter || shooter.isRemoved()
                || !shouldSyncMultiplayerEntity(shooter)) {
            return "";
        }
        return multiplayerEntityId(shooter);
    }

    private void putMultiplayerVehicleRiderData(Map<String, String> data, Entity entity) {
        if (data == null || entity == null || !isRideableVehicle(entity)) {
            return;
        }
        String riderId = multiplayerVehicleRiderId(entity);
        if (!riderId.isBlank()) {
            data.put("riderPlayerId", riderId);
        }
    }

    private String multiplayerVehicleRiderId(Entity entity) {
        if (entity == null || !isRideableVehicle(entity)) {
            return "";
        }
        if (currentPlayerVehicle(player) == entity) {
            return "host";
        }
        String entityId = multiplayerEntityId(entity);
        return multiplayerVehicleRidersByEntityId.getOrDefault(entityId, "");
    }

    private String multiplayerEntityType(Entity entity) {
        if (entity instanceof Mob mob && mob.getDefinition() != null) {
            return mob.getDefinition().name();
        }
        if (entity instanceof MinecartEntity minecart) {
            return switch (minecart.getKind()) {
                case CHEST -> "chest_minecart";
                case FURNACE -> "furnace_minecart";
                default -> "minecart";
            };
        }
        if (entity instanceof ThrownItemEntity thrown) {
            return thrown.getItemType() == ItemType.EGG ? "egg" : "snowball";
        }
        if (entity instanceof FishingHookEntity) {
            return "fishing_hook";
        }
        if (entity instanceof EndCrystalEntity) {
            return "end_crystal";
        }
        return entity.getClass().getSimpleName();
    }

    private Map<String, String> multiplayerEntityData(Entity entity, boolean removed) {
        HashMap<String, String> data = new HashMap<>();
        data.put("removed", Boolean.toString(removed));
        data.put("motionX", Float.toString(entity.getMotionX()));
        data.put("motionY", Float.toString(entity.getMotionY()));
        data.put("motionZ", Float.toString(entity.getMotionZ()));
        data.put("onGround", Boolean.toString(entity.isOnGround()));
        data.put("age", Integer.toString(Math.max(0, entity.getTicksExisted())));
        if (entity instanceof LivingEntity living) {
            data.put("health", Float.toString(living.getHealth()));
            data.put("fireTicks", Integer.toString(Math.max(0, Math.min(living.getFireTicks(), 24_000))));
            putStatusEffectData(data, "effects", living.getActiveEffects());
            data.put("dead", Boolean.toString(living.isDead()));
            data.put("deathTime", Integer.toString(Math.max(0, Math.min(living.getDeathTime(), 20))));
            data.put("hurtTime", Integer.toString(Math.max(0, Math.min(living.getHurtTime(),
                    LivingEntity.MAX_HURT_TIME))));
            data.put("invulnerableTime", Integer.toString(Math.max(0, Math.min(living.getInvulnerableTime(),
                    LivingEntity.MAX_INVULNERABLE_TIME))));
            data.put("lastDamageAmount", Float.toString(Math.max(0.0f, Math.min(living.getLastDamageAmount(),
                    1024.0f))));
            data.put("recentPlayerHitTicks", Integer.toString(Math.max(0, Math.min(living.getRecentPlayerHitTicks(),
                    LivingEntity.RECENT_PLAYER_HIT_TICKS))));
            data.put("recentPlayerLootingLevel", Integer.toString(Math.max(0,
                    Math.min(living.getRecentPlayerLootingLevel(), 255))));
        }
        if (entity instanceof Mob mob) {
            data.put("growingAge", Integer.toString(mob.getGrowingAge()));
            data.put("loveTicks", Integer.toString(mob.getLoveTicks()));
            putMultiplayerSpecialMobState(data, mob);
        }
        if (entity instanceof Sheep sheep) {
            data.put("sheared", Boolean.toString(sheep.isSheared()));
            data.put("woolColor", Integer.toString(sheep.getWoolColor()));
            data.put("eatingGrassTimer", Integer.toString(sheep.getEatingGrassTimer()));
        } else if (entity instanceof Pig pig) {
            data.put("saddled", Boolean.toString(pig.isSaddled()));
            putMultiplayerVehicleRiderData(data, entity);
        } else if (entity instanceof Wolf wolf) {
            data.put("tamed", Boolean.toString(wolf.isTamed()));
            data.put("sitting", Boolean.toString(wolf.isSitting()));
            data.put("angry", Boolean.toString(wolf.isAngry()));
            data.put("ownerName", wolf.getOwnerName() == null ? "" : wolf.getOwnerName());
        }
        if (entity instanceof ArrowEntity arrow) {
            data.put("damage", Float.toString(arrow.getDamage()));
            data.put("critical", Boolean.toString(arrow.isCritical()));
            data.put("playerOwned", Boolean.toString(arrow.isPlayerOwned()));
            String shooterEntityId = multiplayerProjectileShooterEntityId(arrow, arrow.getShooter());
            data.put("shooterEntityId", shooterEntityId);
            String remoteShooterPlayerId = multiplayerProjectilePlayerId(arrow.getRemoteShooterPlayerId(),
                    arrow.isPlayerOwned());
            if (!remoteShooterPlayerId.isBlank()) {
                data.put("remoteShooterPlayerId", remoteShooterPlayerId);
            }
            data.put("knockbackHorizontal", Float.toString(Math.max(0.0f, Math.min(arrow.getKnockbackHorizontal(),
                    16.0f))));
            data.put("knockbackVertical", Float.toString(Math.max(0.0f, Math.min(arrow.getKnockbackVertical(),
                    16.0f))));
            data.put("fireTicksOnHit", Integer.toString(Math.max(0, Math.min(arrow.getFireTicksOnHit(),
                    24_000))));
            data.put("inGround", Boolean.toString(arrow.isInGround()));
            data.put("stuckTicks", Integer.toString(Math.max(0, Math.min(arrow.getStuckTicks(),
                    ArrowEntity.STUCK_DESPAWN_TICKS - 1))));
            data.put("blockX", Integer.toString(arrow.getBlockX()));
            data.put("blockY", Integer.toString(Math.max(0, Math.min(arrow.getBlockY(), Chunk.HEIGHT - 1))));
            data.put("blockZ", Integer.toString(arrow.getBlockZ()));
        } else if (entity instanceof FishingHookEntity hook) {
            data.put("waitTicks", Integer.toString(hook.getWaitTicks()));
            data.put("catchableTicks", Integer.toString(hook.getCatchableTicks()));
            data.put("stuckInGround", Boolean.toString(hook.isStuckInGround()));
            data.put("hookedEntityId", multiplayerFishingHookedEntityId(hook));
            data.put("hookedRemotePlayerId", sanitizeNetworkPlayerId(hook.getHookedRemotePlayerId()));
            String ownerPlayerId = multiplayerFishingHookOwnerId(hook);
            if (!ownerPlayerId.isBlank()) {
                data.put("ownerPlayerId", ownerPlayerId);
                data.put("remoteOwnerPlayerId", ownerPlayerId);
            }
            FishingHookEntity.OwnerSnapshot ownerSnapshot = hook.getOwnerSnapshot();
            if (ownerSnapshot != null) {
                data.put("ownerX", Float.toString(ownerSnapshot.x()));
                data.put("ownerY", Float.toString(ownerSnapshot.y()));
                data.put("ownerZ", Float.toString(ownerSnapshot.z()));
                data.put("ownerAlive", Boolean.toString(ownerSnapshot.alive()));
                data.put("ownerHoldingRod", Boolean.toString(ownerSnapshot.holdingFishingRod()));
                data.put("ownerYaw", Float.toString(ownerSnapshot.yaw()));
                data.put("ownerSneaking", Boolean.toString(ownerSnapshot.sneaking()));
            }
        } else if (entity instanceof FireballEntity fireball) {
            data.put("explosive", Boolean.toString(fireball.isExplosive()));
            data.put("deflectedByPlayer", Boolean.toString(fireball.isDeflectedByPlayer()));
            String shooterEntityId = multiplayerProjectileShooterEntityId(fireball, fireball.getShooter());
            data.put("shooterEntityId", shooterEntityId);
            String remoteDeflectorPlayerId = multiplayerFireballDeflectorPlayerId(fireball);
            if (!remoteDeflectorPlayerId.isBlank()) {
                data.put("remoteDeflectorPlayerId", remoteDeflectorPlayerId);
            }
        } else if (entity instanceof EnderPearlEntity pearl) {
            String remoteOwnerPlayerId = multiplayerProjectilePlayerId(pearl.getRemoteOwnerPlayerId(),
                    pearl.getOwner() == player);
            if (!remoteOwnerPlayerId.isBlank()) {
                data.put("remoteOwnerPlayerId", remoteOwnerPlayerId);
            }
        } else if (entity instanceof EyeOfEnderEntity eye) {
            data.put("targetX", Float.toString(eye.getTargetX()));
            data.put("targetY", Float.toString(eye.getTargetY()));
            data.put("targetZ", Float.toString(eye.getTargetZ()));
            data.put("dropsItem", Boolean.toString(eye.dropsItem()));
        } else if (entity instanceof ThrownItemEntity thrown) {
            data.put("itemId", itemTypeNetworkId(thrown.getItemType()));
            data.put("playerOwned", Boolean.toString(thrown.isPlayerOwned()));
            String shooterEntityId = multiplayerProjectileShooterEntityId(thrown, thrown.getShooter());
            data.put("shooterEntityId", shooterEntityId);
            String remoteShooterPlayerId = multiplayerProjectilePlayerId(thrown.getRemoteShooterPlayerId(),
                    thrown.isPlayerOwned());
            if (!remoteShooterPlayerId.isBlank()) {
                data.put("remoteShooterPlayerId", remoteShooterPlayerId);
            }
        } else if (entity instanceof SplashPotionEntity potion) {
            data.put("potionType", potion.getPotionData().type().name());
            data.put("potionSplash", Boolean.toString(potion.getPotionData().splash()));
            data.put("potionExtended", Boolean.toString(potion.getPotionData().extended()));
            data.put("potionEnhanced", Boolean.toString(potion.getPotionData().enhanced()));
            data.put("playerOwned", Boolean.toString(potion.isPlayerOwned()));
            String shooterEntityId = multiplayerProjectileShooterEntityId(potion, potion.getShooter());
            data.put("shooterEntityId", shooterEntityId);
            String remoteShooterPlayerId = sanitizeNetworkPlayerId(potion.getRemoteShooterPlayerId());
            if (!remoteShooterPlayerId.isBlank()) {
                data.put("remoteShooterPlayerId", remoteShooterPlayerId);
            }
        } else if (entity instanceof ExperienceOrbEntity orb) {
            data.put("value", Integer.toString(orb.getValue()));
            data.put("pickupDelayTicks", Integer.toString(Math.max(0, orb.getPickupDelayTicks())));
            data.put("orbHealth", Integer.toString(Math.max(1, Math.min(orb.getHealth(),
                    ExperienceOrbEntity.MAX_HEALTH))));
        } else if (entity instanceof FallingBlockEntity falling) {
            data.put("blockId", Integer.toString(falling.getBlockType().getId()));
            data.put("metadata", Integer.toString(falling.getMetadata()));
        } else if (entity instanceof ChestMinecartEntity chestMinecart) {
            data.put("cartKind", chestMinecart.getKind().name());
            data.put("damage", Float.toString(chestMinecart.getDamage()));
            putMultiplayerMinecartRuntimeState(data, chestMinecart);
            putTileInventoryData(data, "entity.inventory", chestMinecart.getInventory());
        } else if (entity instanceof FurnaceMinecartEntity furnaceCart) {
            data.put("fuelTicks", Integer.toString(furnaceCart.getFuelTicks()));
            data.put("pushX", Float.toString(furnaceCart.getPushX()));
            data.put("pushZ", Float.toString(furnaceCart.getPushZ()));
            data.put("damage", Float.toString(furnaceCart.getDamage()));
            putMultiplayerMinecartRuntimeState(data, furnaceCart);
        } else if (entity instanceof MinecartEntity minecart) {
            data.put("cartKind", minecart.getKind().name());
            data.put("damage", Float.toString(minecart.getDamage()));
            putMultiplayerMinecartRuntimeState(data, minecart);
            putMultiplayerVehicleRiderData(data, entity);
        } else if (entity instanceof BoatEntity boat) {
            data.put("damage", Float.toString(boat.getDamage()));
            data.put("rollingAmplitude", Integer.toString(Math.max(0, Math.min(boat.getRollingAmplitude(),
                    BoatEntity.HIT_ROLLING_TICKS))));
            data.put("rollingDirection", Integer.toString(boat.getRollingDirection() < 0 ? -1 : 1));
            putMultiplayerVehicleRiderData(data, entity);
        } else if (entity instanceof PaintingEntity painting) {
            data.put("facing", Integer.toString(painting.getFacing()));
            data.put("art", painting.getArt().name());
        } else if (entity instanceof PrimedTntEntity tnt) {
            data.put("fuseTicks", Integer.toString(tnt.getFuseTicks()));
            data.put("playerOwned", Boolean.toString(tnt.isPlayerOwned()));
            String remoteOwnerPlayerId = sanitizeNetworkPlayerId(tnt.getRemoteOwnerPlayerId());
            if (!remoteOwnerPlayerId.isBlank()) {
                data.put("remoteOwnerPlayerId", remoteOwnerPlayerId);
            }
        }
        return data;
    }

    private void putMultiplayerMinecartRuntimeState(Map<String, String> data, MinecartEntity minecart) {
        if (data == null || minecart == null) {
            return;
        }
        data.put("rollingAmplitude", Integer.toString(Math.max(0, Math.min(minecart.getRollingAmplitude(),
                MinecartEntity.HIT_ROLLING_TICKS))));
        data.put("rollingDirection", Integer.toString(minecart.getRollingDirection() < 0 ? -1 : 1));
    }

    private void putMultiplayerSpecialMobState(Map<String, String> data, Mob mob) {
        if (data == null || mob == null) {
            return;
        }
        if (mob instanceof Slime slime) {
            data.put("slimeSize", Integer.toString(slime.getSize()));
            data.put("jumpDelay", Integer.toString(Math.max(0, slime.getJumpDelay())));
        }
        if (mob instanceof Chicken chicken) {
            data.put("eggTimer", Integer.toString(Math.max(0, chicken.getEggTimer())));
        }
        if (mob instanceof Skeleton skeleton) {
            RangedAttackGoal.State ranged = skeleton.getRangedAttackState();
            data.put("rangedAttackActive", Boolean.toString(skeleton.isRangedAttackActive()));
            data.put("rangedAttackCooldown", Integer.toString(Math.max(0, ranged.attackCooldown())));
            data.put("rangedStrafeTime", Integer.toString(Math.max(0, ranged.strafeTime())));
            data.put("rangedStrafingClockwise", Boolean.toString(ranged.strafingClockwise()));
            data.put("rangedStrafeSpeed", Float.toString(Math.max(0.0f, Math.min(ranged.strafeSpeed(), 4.0f))));
        }
        if (mob instanceof SnowGolem snowGolem) {
            data.put("snowGolemAttackCooldown",
                    Integer.toString(Math.max(0, snowGolem.getSnowballAttackCooldown())));
        }
        if (mob instanceof Blaze blaze) {
            data.put("blazeAttackCooldown", Integer.toString(Math.max(0, blaze.getAttackCooldown())));
            data.put("burstShots", Integer.toString(Math.max(0, Math.min(blaze.getBurstShots(), 8))));
            data.put("burstCooldown", Integer.toString(Math.max(0, blaze.getBurstCooldown())));
        }
        if (mob instanceof Ghast ghast) {
            data.put("fireCooldown", Integer.toString(Math.max(0, ghast.getFireCooldown())));
            data.put("ghastAttackCharge", Integer.toString(Math.max(0, ghast.getAttackCharge())));
            data.put("wanderCooldown", Integer.toString(Math.max(0, ghast.getWanderCooldown())));
            data.put("targetX", Float.toString(ghast.getTargetX()));
            data.put("targetY", Float.toString(ghast.getTargetY()));
            data.put("targetZ", Float.toString(ghast.getTargetZ()));
        }
        if (mob instanceof Squid squid) {
            data.put("swimTimer", Integer.toString(Math.max(0, squid.getSwimTimer())));
            data.put("airTicks", Integer.toString(Math.max(LivingEntity.DROWN_DAMAGE_AIR_TICKS,
                    Math.min(LivingEntity.MAX_AIR_TICKS, squid.getAirTicks()))));
            data.put("swimX", Float.toString(squid.getSwimX()));
            data.put("swimY", Float.toString(squid.getSwimY()));
            data.put("swimZ", Float.toString(squid.getSwimZ()));
            data.put("squidPitch", Float.toString(squid.getSquidPitch()));
            data.put("prevSquidPitch", Float.toString(squid.getPrevSquidPitch()));
            data.put("squidYaw", Float.toString(squid.getSquidYaw()));
            data.put("prevSquidYaw", Float.toString(squid.getPrevSquidYaw()));
            data.put("squidRotation", Float.toString(squid.getSquidRotation()));
            data.put("prevSquidRotation", Float.toString(squid.getPrevSquidRotation()));
            data.put("tentacleAngle", Float.toString(squid.getTentacleAngle()));
            data.put("prevTentacleAngle", Float.toString(squid.getPrevTentacleAngle()));
        }
        if (mob instanceof EnderDragon dragon) {
            data.put("targetX", Float.toString(dragon.getTargetX()));
            data.put("targetY", Float.toString(dragon.getTargetY()));
            data.put("targetZ", Float.toString(dragon.getTargetZ()));
            data.put("targetCooldown", Integer.toString(Math.max(0, dragon.getTargetCooldown())));
            data.put("dragonDeathTicks", Integer.toString(Math.max(0, dragon.getDeathTicks())));
            data.put("dragonDeathStarted", Boolean.toString(dragon.isDead()));
        }
        if (mob instanceof Creeper creeper) {
            data.put("ignited", Boolean.toString(creeper.isIgnited()));
            data.put("creeperFuseTicks", Integer.toString(Math.max(0, Math.min(creeper.getFuseTime(),
                    creeper.getMaxFuseTime()))));
            data.put("creeperPowered", Boolean.toString(creeper.isPowered()));
        }
        if (mob instanceof Wolf wolf) {
            data.put("wolfWet", Boolean.toString(wolf.isWet()));
            data.put("wolfShaking", Boolean.toString(wolf.isShaking()));
            data.put("wolfShakeTime", Float.toString(Math.max(0.0f, Math.min(wolf.getShakeTime(), 2.05f))));
            data.put("wolfPrevShakeTime", Float.toString(Math.max(0.0f, Math.min(wolf.getPrevShakeTime(),
                    2.05f))));
        }
        if (mob instanceof Enderman enderman) {
            data.put("carriedBlockId", Integer.toString(enderman.getCarriedBlock().getId()));
            data.put("carriedMetadata", Integer.toString(Math.max(0, Math.min(enderman.getCarriedMetadata(), 15))));
            data.put("endermanAngry", Boolean.toString(enderman.isAngry()));
            data.put("stareTicks", Integer.toString(Math.max(0, enderman.getStareTicks())));
            data.put("teleportCooldown", Integer.toString(Math.max(0, enderman.getTeleportCooldown())));
        }
        if (mob instanceof ZombiePigman pigman) {
            data.put("angerTicks", Integer.toString(Math.max(0, pigman.getAngerTicks())));
        }
        if (mob instanceof Spider spider) {
            data.put("spiderProvoked", Boolean.toString(spider.isProvoked()));
        }
        if (mob instanceof Villager villager) {
            data.put("profession", Integer.toString(Math.max(Villager.PROFESSION_FARMER,
                    Math.min(Villager.PROFESSION_BUTCHER, villager.getProfession()))));
        }
    }

    private int normalizeNetworkSlimeSize(int size) {
        if (size <= 1) {
            return 1;
        }
        if (size <= 2) {
            return 2;
        }
        return 4;
    }

    private void syncMultiplayerDroppedItemState() {
        if (multiplayerServer == null || world == null) {
            return;
        }
        Set<String> currentIds = new HashSet<>();
        for (DroppedItem item : world.getDroppedItems()) {
            if (item == null || item.getCount() <= 0 || item.isDestroyed()) {
                continue;
            }
            String itemId = multiplayerDroppedItemId(item);
            currentIds.add(itemId);
            multiplayerServer.broadcastEntityUpdate(
                    itemId,
                    "dropped_item",
                    item.getX(),
                    item.getY(),
                    item.getZ(),
                    item.getRotation(),
                    0.0f,
                    multiplayerDroppedItemData(item, false));
        }
        for (String previousId : new HashSet<>(lastMultiplayerDroppedItemIds)) {
            if (!currentIds.contains(previousId)) {
                multiplayerServer.broadcastEntityUpdate(previousId, "dropped_item",
                        0.0f, 0.0f, 0.0f, 0.0f, 0.0f, Map.of("removed", "true"));
            }
        }
        lastMultiplayerDroppedItemIds.clear();
        lastMultiplayerDroppedItemIds.addAll(currentIds);
    }

    private String multiplayerDroppedItemId(DroppedItem item) {
        return multiplayerDroppedItemIds.computeIfAbsent(item, ignored -> "drop-" + nextMultiplayerDroppedItemId++);
    }

    private Map<String, String> multiplayerDroppedItemData(DroppedItem item, boolean removed) {
        HashMap<String, String> data = new HashMap<>();
        data.put("removed", Boolean.toString(removed));
        data.put("motionX", Float.toString(item.getVelocityX()));
        data.put("motionY", Float.toString(item.getVelocityY()));
        data.put("motionZ", Float.toString(item.getVelocityZ()));
        data.put("onGround", Boolean.toString(item.isOnGround()));
        data.put("itemId", itemTypeNetworkId(item.getItemType()));
        data.put("count", Integer.toString(item.getCount()));
        data.put("damage", Integer.toString(item.getDurability()));
        data.put("age", Float.toString(item.getAge()));
        data.put("health", Integer.toString(item.getHealth()));
        data.put("pickupDelay", Integer.toString(item.getPickupDelayTicks()));
        data.put("rotation", Float.toString(item.getRotation()));
        data.put("bobPhase", Float.toString(item.getBobPhase()));
        return data;
    }

    private void sendMultiplayerPlayerState(float deltaTime) {
        boolean hasConnectedClient = multiplayerClient != null && multiplayerClient.isConnected();
        if (player == null || (multiplayerServer == null && !hasConnectedClient)) {
            multiplayerStateTimer = 0.0f;
            return;
        }
        multiplayerStateTimer += deltaTime;
        if (multiplayerStateTimer < 0.1f) {
            return;
        }
        multiplayerStateTimer = 0.0f;
        sendMultiplayerPlayerStateNow();
    }

    private void sendMultiplayerPlayerStateNow() {
        boolean hasConnectedClient = multiplayerClient != null && multiplayerClient.isConnected();
        if (player == null || (multiplayerServer == null && !hasConnectedClient)) {
            return;
        }
        multiplayerStateTimer = 0.0f;
        Inventory inventory = player.getInventory();
        ItemStack held = inventory.getItemInHand();
        String heldItemId = held == null || held.isEmpty() ? "air" : multiplayerItemId(held);
        int heldItemCount = held == null || held.isEmpty() ? 0 : held.getCount();
        int heldItemDamage = held == null || held.isEmpty() ? 0 : held.getDurability();
        int selectedSlot = inventory.getSelectedSlot();
        String gameMode = player.getGameMode().name();
        Map<String, String> stateData = multiplayerPlayerStateData(player);
        Map<String, String> clientStateData = hasConnectedClient
                ? multiplayerClientPlayerStateData(player)
                : Map.of();

        if (multiplayerServer != null) {
            multiplayerServer.broadcastPlayerState(
                    "host",
                    localPlayerName(),
                    player.getPosition().x,
                    player.getPosition().y,
                    player.getPosition().z,
                    player.getCamera().getYaw(),
                    player.getCamera().getPitch(),
                    player.isOnGround(),
                    player.isSneaking(),
                    player.getStats().getHealth(),
                    heldItemId,
                    heldItemCount,
                    heldItemDamage,
                    selectedSlot,
                    gameMode,
                    stateData);
        }

        if (!hasConnectedClient) {
            return;
        }
        try {
            multiplayerClient.sendPlayerState(
                    player.getPosition().x,
                    player.getPosition().y,
                    player.getPosition().z,
                    player.getCamera().getYaw(),
                    player.getCamera().getPitch(),
                    player.isInputForwardDown(),
                    player.isInputBackwardDown(),
                    player.isInputLeftDown(),
                    player.isInputRightDown(),
                    player.isInputJumpDown(),
                    player.isOnGround(),
                    player.isSneaking(),
                    player.getStats().getHealth(),
                    heldItemId,
                    heldItemCount,
                    heldItemDamage,
                    selectedSlot,
                    gameMode,
                    clientStateData);
        } catch (Exception e) {
            addChatMessage("Could not sync player state: " + e.getMessage());
        }
    }

    private Map<String, String> multiplayerPlayerStateData(Player source) {
        HashMap<String, String> data = new HashMap<>();
        Inventory inventory = source == null ? null : source.getInventory();
        putTileInventoryData(data, "armor", inventory == null ? null : inventory.getArmor());
        if (source != null && source.getStats() != null) {
            putPlayerStatsData(data, "stats", source);
            putPlayerProgressionData(data, "progression", source.getStats().getProgression());
            putStatusEffectData(data, "status", source.getStats().getActiveEffects());
            putPlayerRespawnStateData(data, source);
            putRemotePlayerAnimationData(data, source);
        } else {
            data.put("status.count", "0");
        }
        return data;
    }

    private Map<String, String> multiplayerClientPlayerStateData(Player source) {
        HashMap<String, String> data = new HashMap<>();
        if (source != null && source.getStats() != null) {
            putPlayerStatsData(data, "stats", source);
            putPlayerProgressionData(data, "progression", source.getStats().getProgression());
            putStatusEffectData(data, "status", source.getStats().getActiveEffects());
            putPlayerRespawnStateData(data, source);
            putRemotePlayerAnimationData(data, source);
        } else {
            data.put("status.count", "0");
        }
        putClientVehicleStateData(data, source);
        return data;
    }

    private void putRemotePlayerAnimationData(Map<String, String> data, Player source) {
        if (data == null || source == null) {
            return;
        }
        data.put("remote.sprinting", Boolean.toString(source.isSprinting()));
        data.put("remote.usingItem", Boolean.toString(source.isUsingItem()));
        data.put("remote.blocking", Boolean.toString(source.isBlockingItem()));
        data.put("remote.drawingBow", Boolean.toString(source.isDrawingBow()));
        data.put("remote.useProgress", Float.toString(Math.max(0.0f, Math.min(1.0f, source.getUseProgress(1.0f)))));
    }

    private void putClientVehicleStateData(Map<String, String> data, Player source) {
        if (data == null || source == null || !clientMultiplayerWorld) {
            return;
        }
        Entity ridingVehicle = currentPlayerVehicle(source);
        if (ridingVehicle != null) {
            String entityId = remoteEntityNetworkId(ridingVehicle);
            if (entityId != null && !entityId.isBlank()) {
                data.put("vehicle.entityId", entityId);
                data.put("vehicle.type", multiplayerVehicleType(ridingVehicle));
                data.put("vehicle.mounted", "true");
                data.put("vehicle.forward", Float.toString(source.getMountedForwardInput()));
                data.put("vehicle.strafe", Float.toString(source.getMountedStrafeInput()));
                data.put("vehicle.yaw", Float.toString(source.getMountedYawInput()));
            }
            return;
        }
        Entity dismounted = source.drainLastDismountedVehicle();
        String entityId = remoteEntityNetworkId(dismounted);
        if (entityId != null && !entityId.isBlank()) {
            data.put("vehicle.entityId", entityId);
            data.put("vehicle.type", multiplayerVehicleType(dismounted));
            data.put("vehicle.mounted", "false");
            data.put("vehicle.dismount", "true");
            data.put("vehicle.forward", "0.0");
            data.put("vehicle.strafe", "0.0");
            data.put("vehicle.yaw", Float.toString(source.getMountedYawInput()));
        }
    }

    private void putPlayerRespawnStateData(Map<String, String> data, Player source) {
        if (data == null || source == null) {
            return;
        }
        data.put("respawn.x", Float.toString(source.getSpawnX()));
        data.put("respawn.y", Float.toString(source.getSpawnY()));
        data.put("respawn.z", Float.toString(source.getSpawnZ()));
        BlockPos bedSpawn = source.getBedSpawnPos();
        data.put("bedSpawn.set", Boolean.toString(bedSpawn != null));
        if (bedSpawn != null) {
            data.put("bedSpawn.x", Integer.toString(bedSpawn.x()));
            data.put("bedSpawn.y", Integer.toString(bedSpawn.y()));
            data.put("bedSpawn.z", Integer.toString(bedSpawn.z()));
        }
    }

    private Entity currentPlayerVehicle(Player source) {
        if (source == null) {
            return null;
        }
        if (source.getRidingMinecart() != null) {
            return source.getRidingMinecart();
        }
        if (source.getRidingBoat() != null) {
            return source.getRidingBoat();
        }
        return source.getRidingPig();
    }

    private String multiplayerVehicleType(Entity vehicle) {
        if (vehicle instanceof BoatEntity) {
            return "boat";
        }
        if (vehicle instanceof MinecartEntity) {
            return "minecart";
        }
        if (vehicle instanceof Pig) {
            return "pig";
        }
        return "";
    }

    private void putPlayerStatsData(Map<String, String> data, String prefix, Player source) {
        if (data == null || source == null || source.getStats() == null) {
            return;
        }
        PlayerStats stats = source.getStats();
        data.put(prefix + ".health", Float.toString(stats.getHealth()));
        data.put(prefix + ".hunger", Float.toString(stats.getHunger()));
        data.put(prefix + ".saturation", Float.toString(stats.getSaturation()));
        data.put(prefix + ".air", Float.toString(stats.getCurrentAir()));
        data.put(prefix + ".exhaustion", Float.toString(stats.getExhaustion()));
        data.put(prefix + ".onFire", Boolean.toString(source.isOnFire()));
        data.put(prefix + ".fireTicks", Integer.toString(Math.max(0, Math.min(source.getFireTicks(), 24_000))));
    }

    private void putPlayerProgressionData(Map<String, String> data, String prefix, PlayerProgression progression) {
        if (data == null || progression == null) {
            return;
        }
        data.put(prefix + ".totalExperience", Integer.toString(Math.max(0, progression.getTotalExperience())));
        data.put(prefix + ".score", Integer.toString(Math.max(0, progression.getScore())));
        data.put(prefix + ".level", Integer.toString(Math.max(0, progression.getLevel())));
        data.put(prefix + ".experienceIntoLevel", Integer.toString(Math.max(0, progression.getExperienceIntoLevel())));
        data.put(prefix + ".experienceToNextLevel", Integer.toString(Math.max(0, progression.getExperienceToNextLevel())));
    }

    private void putStatusEffectData(Map<String, String> data, String prefix, List<StatusEffectInstance> effects) {
        if (effects == null || effects.isEmpty()) {
            data.put(prefix + ".count", "0");
            return;
        }
        data.put(prefix + ".count", Integer.toString(effects.size()));
        for (int i = 0; i < effects.size(); i++) {
            StatusEffectInstance effect = effects.get(i);
            data.put(prefix + "." + i + ".type", effect.type().name());
            data.put(prefix + "." + i + ".duration", Integer.toString(Math.max(0, effect.durationTicks())));
            data.put(prefix + "." + i + ".amplifier", Integer.toString(Math.max(0, effect.amplifier())));
        }
    }

    private List<StatusEffectInstance> statusEffectsFromMessage(NetworkMessage message, String prefix) {
        ArrayList<StatusEffectInstance> effects = new ArrayList<>();
        if (message == null || message.data() == null) {
            return effects;
        }
        int count = Math.max(0, Math.min(messageInt(message, prefix + ".count", 0), 32));
        for (int i = 0; i < count; i++) {
            String rawType = messageString(message, prefix + "." + i + ".type", "");
            try {
                StatusEffectType type = StatusEffectType.valueOf(rawType);
                int duration = Math.max(0, Math.min(messageInt(message, prefix + "." + i + ".duration", 0),
                        24_000));
                int amplifier = Math.max(0, Math.min(messageInt(message, prefix + "." + i + ".amplifier", 0),
                        255));
                if (duration > 0) {
                    effects.add(new StatusEffectInstance(type, duration, amplifier));
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return effects;
    }

    private record MultiplayerRemoteHazardContact(boolean fire, boolean lava, boolean cactus, boolean suffocation) {
        private static final MultiplayerRemoteHazardContact NONE =
                new MultiplayerRemoteHazardContact(false, false, false, false);
        private static final MultiplayerRemoteHazardContact LAVA =
                new MultiplayerRemoteHazardContact(true, true, false, false);
    }

    private record MultiplayerExplosionPush(float x, float y, float z) {
        private static final MultiplayerExplosionPush NONE = new MultiplayerExplosionPush(0.0f, 0.0f, 0.0f);

        float horizontalLength() {
            return (float) Math.sqrt(x * x + z * z);
        }
    }

    private static final class RemotePlayerView {
        private final Player player;

        private RemotePlayerView(String username) {
            this.player = new Player(0.0f, 80.0f, 0.0f);
            this.player.setPlayerName(username);
            this.player.setCameraMode(1);
        }

        private Player player() {
            return player;
        }

        private void tick(float deltaTime) {
            player.tickRemoteAnimations(deltaTime);
        }
    }

    private record MultiplayerRosterEntry(String username, int latencyMillis) {
    }

    private record MultiplayerPlayerListRow(String username, int latencyMillis) {
    }

    private CommandDispatcher.Context commandContext() {
        return new GameCommandContext(localPlayerName(), true, -1);
    }

    private CommandDispatcher.Context remoteCommandContext(String senderName, int clientId) {
        return new GameCommandContext(senderName, false, clientId);
    }

    private final class GameCommandContext implements CommandDispatcher.Context {
        private final String senderName;
        private final boolean localSender;
        private final int senderClientId;

        private GameCommandContext(String senderName, boolean localSender, int senderClientId) {
            String normalized = senderName == null || senderName.isBlank() ? "Player" : senderName.trim();
            this.senderName = normalized;
            this.localSender = localSender;
            this.senderClientId = senderClientId;
        }

        @Override
        public String senderName() {
            return senderName;
        }

        @Override
        public boolean hasPermission(String command) {
            String name = command == null ? "" : command.toLowerCase(java.util.Locale.ROOT);
            return switch (name) {
                case "help", "list", "me", "msg", "seed" -> true;
                case "say", "save-all", "save-on", "save-off", "stop",
                        "ban", "ban-ip", "banlist", "deop", "kick", "op",
                        "pardon", "pardon-ip", "whitelist" -> !clientMultiplayerWorld && canUsePrivilegedCommand();
                default -> !clientMultiplayerWorld && canUsePrivilegedCommand();
            };
        }

        @Override
        public List<String> playerNames() {
            List<String> names = new java.util.ArrayList<>();
            names.add(localPlayerName());
            names.add(senderName());
            if (multiplayerServer != null) {
                for (MultiplayerServer.ConnectedPlayer connectedPlayer : multiplayerServer.connectedPlayers()) {
                    names.add(connectedPlayer.username());
                }
                for (java.util.Map.Entry<Integer, com.google.gson.JsonObject> entry : multiplayerServer.playerStates().entrySet()) {
                    com.google.gson.JsonObject data = entry.getValue();
                    if (data != null && data.has("username") && !data.get("username").getAsString().isBlank()) {
                        names.add(data.get("username").getAsString());
                    } else {
                        names.add("Player" + entry.getKey());
                    }
                }
            }
            return names.stream().distinct().toList();
        }

        @Override
        public long seed() {
            return world == null ? 0L : world.getSeed();
        }

        @Override
        public float timeOfDay() {
            return dayCycleManager == null ? 0.0f : dayCycleManager.getTime();
        }

        @Override
        public void setTimeOfDay(float time) {
            if (dayCycleManager != null) {
                dayCycleManager.setTime(time);
            }
        }

        @Override
        public GameMode gameMode() {
            return currentGameMode;
        }

        @Override
        public void setGameMode(GameMode mode) {
            GameMode nextMode = mode == null ? GameMode.SURVIVAL : mode;
            if (localSender) {
                currentGameMode = nextMode;
                currentHardcore = currentGameMode == GameMode.HARDCORE;
                if (player != null) {
                    player.setGameMode(currentGameMode);
                    player.setDifficulty(currentHardcore ? Difficulty.HARD : currentDifficulty);
                }
            } else {
                sendCommandActionToClientId(senderClientId, MultiplayerProtocol.ACTION_COMMAND_GAMEMODE,
                        Map.of("gameMode", nextMode.name()));
            }
        }

        @Override
        public Difficulty difficulty() {
            return currentDifficulty;
        }

        @Override
        public void setDifficulty(Difficulty difficulty) {
            if (currentHardcore) {
                currentDifficulty = Difficulty.HARD;
            } else {
                currentDifficulty = difficulty == null ? Difficulty.EASY : difficulty;
                if (settings != null) {
                    settings.setDifficulty(currentDifficulty);
                }
            }
            if (player != null) {
                player.setDifficulty(currentDifficulty);
            }
        }

        @Override
        public float playerX() {
            return localSender ? (player == null ? 0.0f : player.getPosition().x) : playerX(senderName);
        }

        @Override
        public float playerY() {
            return localSender ? (player == null ? 0.0f : player.getPosition().y) : playerY(senderName);
        }

        @Override
        public float playerZ() {
            return localSender ? (player == null ? 0.0f : player.getPosition().z) : playerZ(senderName);
        }

        @Override
        public float playerX(String target) {
            if (isHostPlayerName(target)) {
                return player == null ? 0.0f : player.getPosition().x;
            }
            com.google.gson.JsonObject data = remotePlayerState(target);
            return data != null && data.has("x") ? data.get("x").getAsFloat() : 0.0f;
        }

        @Override
        public float playerY(String target) {
            if (isHostPlayerName(target)) {
                return player == null ? 0.0f : player.getPosition().y;
            }
            com.google.gson.JsonObject data = remotePlayerState(target);
            return data != null && data.has("y") ? data.get("y").getAsFloat() : 80.0f;
        }

        @Override
        public float playerZ(String target) {
            if (isHostPlayerName(target)) {
                return player == null ? 0.0f : player.getPosition().z;
            }
            com.google.gson.JsonObject data = remotePlayerState(target);
            return data != null && data.has("z") ? data.get("z").getAsFloat() : 0.0f;
        }

        @Override
        public void teleport(float x, float y, float z) {
            if (localSender && player != null) {
                player.setPosition(x, y, z);
            } else if (!localSender) {
                sendCommandActionToClientId(senderClientId, MultiplayerProtocol.ACTION_COMMAND_TELEPORT,
                        Map.of("x", Float.toString(x), "y", Float.toString(y), "z", Float.toString(z)));
            }
        }

        @Override
        public boolean teleportPlayer(String target, float x, float y, float z) {
            if (isHostPlayerName(target)) {
                if (player != null) {
                    player.setPosition(x, y, z);
                    return true;
                }
                return false;
            }
            return sendTargetedCommandAction(target, MultiplayerProtocol.ACTION_COMMAND_TELEPORT,
                    Map.of("x", Float.toString(x), "y", Float.toString(y), "z", Float.toString(z)));
        }

        @Override
        public boolean addItem(ItemStack stack) {
            if (localSender) {
                return player != null && player.addStackToInventory(stack);
            }
            if (stack == null || stack.isEmpty() || stack.getType() == null) {
                return false;
            }
            return sendCommandActionToClientId(senderClientId, MultiplayerProtocol.ACTION_COMMAND_GIVE,
                    Map.of("itemId", Integer.toString(stack.getType().getId()),
                            "itemData", Integer.toString(stack.getType().getDataValue()),
                            "count", Integer.toString(stack.getCount())));
        }

        @Override
        public boolean addItemToPlayer(String target, ItemStack stack) {
            if (stack == null || stack.isEmpty() || stack.getType() == null) {
                return false;
            }
            if (isHostPlayerName(target)) {
                return player != null && player.addStackToInventory(stack);
            }
            ItemType item = stack.getType();
            return sendTargetedCommandAction(target, MultiplayerProtocol.ACTION_COMMAND_GIVE,
                    Map.of("itemId", Integer.toString(item.getId()),
                            "itemData", Integer.toString(item.getDataValue()),
                            "count", Integer.toString(stack.getCount())));
        }

        @Override
        public void clearInventory(ItemType filter) {
            if (localSender) {
                clearLocalPlayerInventory(filter);
            } else if (filter == null) {
                sendCommandActionToClientId(senderClientId, MultiplayerProtocol.ACTION_COMMAND_CLEAR, Map.of());
            } else {
                sendCommandActionToClientId(senderClientId, MultiplayerProtocol.ACTION_COMMAND_CLEAR,
                        Map.of("itemId", Integer.toString(filter.getId()),
                                "itemData", Integer.toString(filter.getDataValue())));
            }
        }

        @Override
        public boolean clearPlayerInventory(String target, ItemType filter) {
            if (isHostPlayerName(target)) {
                clearLocalPlayerInventory(filter);
                return true;
            }
            if (filter == null) {
                return sendTargetedCommandAction(target, MultiplayerProtocol.ACTION_COMMAND_CLEAR, Map.of());
            }
            return sendTargetedCommandAction(target, MultiplayerProtocol.ACTION_COMMAND_CLEAR,
                    Map.of("itemId", Integer.toString(filter.getId()),
                            "itemData", Integer.toString(filter.getDataValue())));
        }

        @Override
        public void kill() {
            if (localSender) {
                killLocalPlayer();
            } else {
                sendCommandActionToClientId(senderClientId, MultiplayerProtocol.ACTION_COMMAND_KILL, Map.of());
            }
        }

        @Override
        public boolean killPlayer(String target) {
            if (isHostPlayerName(target)) {
                killLocalPlayer();
                return true;
            }
            return sendTargetedCommandAction(target, MultiplayerProtocol.ACTION_COMMAND_KILL, Map.of());
        }

        @Override
        public void setSpawn(float x, float y, float z) {
            if (localSender && player != null) {
                player.setSpawnPosition(x, y, z);
            } else if (!localSender) {
                sendCommandActionToClientId(senderClientId, MultiplayerProtocol.ACTION_COMMAND_SPAWNPOINT,
                        Map.of("x", Float.toString(x), "y", Float.toString(y), "z", Float.toString(z)));
            }
        }

        @Override
        public boolean setPlayerSpawn(String target, float x, float y, float z) {
            if (isHostPlayerName(target)) {
                if (player != null) {
                    player.setSpawnPosition(x, y, z);
                    return true;
                }
                return false;
            }
            return sendTargetedCommandAction(target, MultiplayerProtocol.ACTION_COMMAND_SPAWNPOINT,
                    Map.of("x", Float.toString(x), "y", Float.toString(y), "z", Float.toString(z)));
        }

        @Override
        public boolean setPlayerSpawnToCurrentPosition(String target) {
            if (isHostPlayerName(target)) {
                if (player != null) {
                    player.setSpawnPosition(player.getPosition().x, player.getPosition().y, player.getPosition().z);
                    return true;
                }
                return false;
            }
            return sendTargetedCommandAction(target, MultiplayerProtocol.ACTION_COMMAND_SPAWNPOINT, Map.of());
        }

        @Override
        public void setWorldSpawn(int x, int y, int z) {
            worldSpawnX = x;
            worldSpawnY = y;
            worldSpawnZ = z;
            if (world != null) {
                world.setWorldSpawn(x, y, z);
            }
            if (player != null) {
                player.setSpawnPosition(x + 0.5f, y, z + 0.5f);
            }
        }

        @Override
        public void addExperience(int amount) {
            if (localSender && player != null) {
                player.getStats().getProgression().addExperience(amount);
            } else if (!localSender) {
                sendCommandActionToClientId(senderClientId, MultiplayerProtocol.ACTION_COMMAND_EXPERIENCE,
                        Map.of("amount", Integer.toString(amount)));
            }
        }

        @Override
        public void setWeather(String weather) {
            weatherState = World.normalizeWeatherState(weather);
            if (world != null) {
                world.setWeatherState(weatherState);
            }
        }

        @Override
        public void saveAll() {
            saveGame("command");
        }

        @Override
        public void setSavingEnabled(boolean enabled) {
            savingEnabled = enabled;
        }

        @Override
        public boolean savingEnabled() {
            return savingEnabled;
        }

        @Override
        public void requestStop() {
            running = false;
        }

        @Override
        public void feedback(String message) {
            if (localSender) {
                addChatMessage(message);
            } else {
                sendCommandActionToClientId(senderClientId, MultiplayerProtocol.ACTION_COMMAND_PRIVATE_MESSAGE,
                        Map.of("text", message == null ? "" : message));
            }
        }

        @Override
        public void broadcast(String message) {
            broadcastSystemChat(message);
        }

        @Override
        public boolean sendPrivateMessage(String sender, String target, String message) {
            String formatted = "[" + sender + " -> " + target + "] " + message;
            if (isHostPlayerName(target)) {
                addChatMessage(formatted);
                return true;
            }
            return sendTargetedCommandAction(target, MultiplayerProtocol.ACTION_COMMAND_PRIVATE_MESSAGE,
                    Map.of("text", formatted));
        }

        @Override
        public String runServerAdminCommand(String command, List<String> args) {
            return switch (command) {
                case "op" -> requirePlayerArg(command, args, playerName -> {
                    operators.add(normalizeAdminName(playerName));
                    configureSaveMetadata();
                    return "Opped " + playerName + ".";
                });
                case "deop" -> requirePlayerArg(command, args, playerName -> {
                    operators.remove(normalizeAdminName(playerName));
                    configureSaveMetadata();
                    return "De-opped " + playerName + ".";
                });
                case "ban" -> requirePlayerArg(command, args, playerName -> {
                    bannedPlayers.add(normalizeAdminName(playerName));
                    String reason = args.size() > 1 ? String.join(" ", args.subList(1, args.size())) : "Banned";
                    configureMultiplayerAccessControl();
                    configureSaveMetadata();
                    int kicked = enforceMultiplayerAccessControl(reason);
                    return "Banned player " + playerName + formatKickedSuffix(kicked);
                });
                case "pardon" -> requirePlayerArg(command, args, playerName -> {
                    bannedPlayers.remove(normalizeAdminName(playerName));
                    configureMultiplayerAccessControl();
                    configureSaveMetadata();
                    return "Pardoned player " + playerName + ".";
                });
                case "ban-ip" -> requirePlayerArg(command, args, target -> {
                    String ip = resolveBanIpTarget(target);
                    if (ip.isBlank()) {
                        return "No connected client or address found for " + target + ".";
                    }
                    bannedIps.add(ip);
                    configureMultiplayerAccessControl();
                    configureSaveMetadata();
                    int kicked = enforceMultiplayerAccessControl(
                            args.size() > 1 ? String.join(" ", args.subList(1, args.size())) : "IP banned");
                    return "Banned IP " + ip + formatKickedSuffix(kicked);
                });
                case "pardon-ip" -> requirePlayerArg(command, args, ip -> {
                    bannedIps.remove(normalizeAdminIp(ip));
                    configureMultiplayerAccessControl();
                    configureSaveMetadata();
                    return "Pardoned IP " + ip + ".";
                });
                case "banlist" -> {
                    boolean ips = !args.isEmpty() && args.get(0).equalsIgnoreCase("ips");
                    java.util.Set<String> list = ips ? bannedIps : bannedPlayers;
                    yield (ips ? "Banned IPs: " : "Banned players: ")
                            + (list.isEmpty() ? "(none)" : String.join(", ", list));
                }
                case "kick" -> requirePlayerArg(command, args, playerName -> {
                    String reason = args.size() > 1 ? String.join(" ", args.subList(1, args.size())) : "Kicked by an operator";
                    return kickNamedPlayer(playerName, reason)
                            ? "Kicked " + playerName + "."
                            : "No connected client named " + playerName + ".";
                });
                case "whitelist" -> whitelistCommand(args);
                default -> "Unknown server administration command.";
            };
        }

        @Override
        public int regenerateUnmodifiedChunks(int radiusChunks) {
            if (world == null || player == null) {
                return 0;
            }
            return world.regenerateUnmodifiedChunksAround(player.getPosition().x, player.getPosition().z, radiusChunks);
        }

        @Override
        public CommandDispatcher.StructureResult locateStructure(String type) {
            if (world == null || player == null) {
                return null;
            }
            StructureType structureType = switch (type == null ? "" : type) {
                case "stronghold" -> StructureType.STRONGHOLD;
                case "nether_fortress" -> StructureType.NETHER_FORTRESS;
                default -> null;
            };
            if (structureType == null) {
                return null;
            }
            StructureGenerator.StructureLocation location = world.locateStructure(structureType,
                    (int) Math.floor(player.getPosition().x), (int) Math.floor(player.getPosition().z));
            return location == null ? null
                    : new CommandDispatcher.StructureResult(location.blockX(), location.blockY(), location.blockZ());
        }

        private String whitelistCommand(List<String> args) {
            if (args.isEmpty() || "list".equalsIgnoreCase(args.get(0))) {
                return "Whitelisted players: " + (whitelist.isEmpty() ? "(none)" : String.join(", ", whitelist));
            }
            String mode = args.get(0).toLowerCase(java.util.Locale.ROOT);
            if ("on".equals(mode)) {
                whitelistEnabled = true;
                configureMultiplayerAccessControl();
                configureSaveMetadata();
                int kicked = enforceMultiplayerAccessControl("You are not white-listed on this server");
                return "Whitelist enabled" + formatKickedSuffix(kicked);
            }
            if ("off".equals(mode)) {
                whitelistEnabled = false;
                configureMultiplayerAccessControl();
                configureSaveMetadata();
                return "Whitelist disabled.";
            }
            if ("add".equals(mode) || "remove".equals(mode)) {
                if (args.size() < 2) {
                    return "Usage: /whitelist " + mode + " <player>";
                }
                String playerName = normalizeAdminName(args.get(1));
                if ("add".equals(mode)) {
                    whitelist.add(playerName);
                    configureMultiplayerAccessControl();
                    configureSaveMetadata();
                    return "Added " + args.get(1) + " to the whitelist.";
                }
                whitelist.remove(playerName);
                configureMultiplayerAccessControl();
                configureSaveMetadata();
                int kicked = enforceMultiplayerAccessControl("You are not white-listed on this server");
                return "Removed " + args.get(1) + " from the whitelist" + formatKickedSuffix(kicked);
            }
            if ("reload".equals(mode)) {
                configureMultiplayerAccessControl();
                configureSaveMetadata();
                int kicked = enforceMultiplayerAccessControl("You are not white-listed on this server");
                return "Whitelist reloaded" + formatKickedSuffix(kicked);
            }
            return "Usage: /whitelist <on|off|list|add|remove|reload>";
        }

        private int enforceMultiplayerAccessControl(String reason) {
            if (multiplayerServer == null) {
                return 0;
            }
            return multiplayerServer.enforceAccessControl(reason);
        }

        private String resolveBanIpTarget(String target) {
            String connectedAddress = connectedRemoteAddress(target);
            if (!connectedAddress.isBlank()) {
                return connectedAddress;
            }
            return normalizeAdminIp(target);
        }

        private String connectedRemoteAddress(String target) {
            if (multiplayerServer == null || target == null || target.isBlank()) {
                return "";
            }
            String normalized = target.trim().toLowerCase(java.util.Locale.ROOT);
            for (MultiplayerServer.ConnectedPlayer connectedPlayer : multiplayerServer.connectedPlayers()) {
                String username = normalizeAdminName(connectedPlayer.username());
                String playerId = connectedPlayer.playerId() == null
                        ? ""
                        : connectedPlayer.playerId().toLowerCase(java.util.Locale.ROOT);
                String legacyName = ("player" + connectedPlayer.clientId()).toLowerCase(java.util.Locale.ROOT);
                if (normalized.equals(username) || normalized.equals(playerId) || normalized.equals(legacyName)) {
                    return normalizeAdminIp(connectedPlayer.remoteAddress());
                }
            }
            return "";
        }

        private String formatKickedSuffix(int kicked) {
            return kicked <= 0 ? "." : "; kicked " + kicked + " connected client" + (kicked == 1 ? "" : "s") + ".";
        }

        private String normalizeAdminName(String value) {
            return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
        }

        private String normalizeAdminIp(String value) {
            String normalized = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
            return normalized.startsWith("/") ? normalized.substring(1) : normalized;
        }

        private String requirePlayerArg(String command, List<String> args, java.util.function.Function<String, String> action) {
            if (args.isEmpty() || args.get(0).isBlank()) {
                return "Usage: /" + command + " <player>";
            }
            return action.apply(args.get(0));
        }

        private boolean kickNamedPlayer(String playerName, String reason) {
            if (multiplayerServer == null || playerName == null) {
                return false;
            }
            String normalized = playerName.trim().toLowerCase(java.util.Locale.ROOT);
            for (MultiplayerServer.ConnectedPlayer connectedPlayer : multiplayerServer.connectedPlayers()) {
                String connectedName = connectedPlayer.username() == null
                        ? ""
                        : connectedPlayer.username().toLowerCase(java.util.Locale.ROOT);
                if (normalized.equals(connectedName)
                        || normalized.equals(connectedPlayer.playerId().toLowerCase(java.util.Locale.ROOT))
                        || normalized.equals(("player" + connectedPlayer.clientId()).toLowerCase(java.util.Locale.ROOT))) {
                    return multiplayerServer.disconnectClient(connectedPlayer.clientId(), reason);
                }
            }
            for (java.util.Map.Entry<Integer, com.google.gson.JsonObject> entry : multiplayerServer.playerStates().entrySet()) {
                com.google.gson.JsonObject data = entry.getValue();
                if (data != null && data.has("username")
                        && normalized.equals(data.get("username").getAsString().toLowerCase(java.util.Locale.ROOT))) {
                    return multiplayerServer.disconnectClient(entry.getKey(), reason);
                }
            }
            int clientId = parseProtocolClientId(normalized);
            if (clientId > 0) {
                return multiplayerServer.disconnectClient(clientId, reason);
            }
            return false;
        }

        private boolean sendTargetedCommandAction(String target, String action, Map<String, String> data) {
            if (multiplayerServer == null || target == null || action == null || action.isBlank()) {
                return false;
            }
            int clientId = remoteClientId(target);
            return sendCommandActionToClientId(clientId, action, data);
        }

        private boolean sendCommandActionToClientId(int clientId, String action, Map<String, String> data) {
            if (multiplayerServer == null || clientId <= 0 || action == null || action.isBlank()) {
                return false;
            }
            com.google.gson.JsonObject payload = NetworkMessage.object();
            payload.addProperty("playerId", "player-" + clientId);
            payload.addProperty("action", action);
            if (data != null) {
                for (Map.Entry<String, String> entry : data.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        payload.addProperty(entry.getKey(), entry.getValue());
                    }
                }
            }
            return multiplayerServer.sendToClient(clientId, NetworkMessage.of("clientAction", payload));
        }

        private boolean isHostPlayerName(String target) {
            return target != null && target.equalsIgnoreCase(localPlayerName());
        }

        private com.google.gson.JsonObject remotePlayerState(String playerName) {
            if (multiplayerServer == null || playerName == null) {
                return null;
            }
            String normalized = playerName.trim().toLowerCase(java.util.Locale.ROOT);
            int connectedClientId = multiplayerServer.clientIdForUsername(playerName);
            if (connectedClientId > 0) {
                com.google.gson.JsonObject data = multiplayerServer.playerStates().get(connectedClientId);
                if (data != null) {
                    return data;
                }
            }
            for (java.util.Map.Entry<Integer, com.google.gson.JsonObject> entry : multiplayerServer.playerStates().entrySet()) {
                com.google.gson.JsonObject data = entry.getValue();
                if (data != null && data.has("username")
                        && normalized.equals(data.get("username").getAsString().toLowerCase(java.util.Locale.ROOT))) {
                    return data;
                }
            }
            int clientId = parseProtocolClientId(normalized);
            return clientId > 0 ? multiplayerServer.playerStates().get(clientId) : null;
        }

        private int remoteClientId(String playerName) {
            if (multiplayerServer != null) {
                int connectedClientId = multiplayerServer.clientIdForUsername(playerName);
                if (connectedClientId > 0) {
                    return connectedClientId;
                }
            }
            com.google.gson.JsonObject data = remotePlayerState(playerName);
            if (data != null && data.has("clientId")) {
                return data.get("clientId").getAsInt();
            }
            return parseProtocolClientId(playerName);
        }

        private boolean isLocalOperator() {
            return operators.contains(senderName().toLowerCase(java.util.Locale.ROOT));
        }

        private boolean canUsePrivilegedCommand() {
            return localSender
                    ? multiplayerServer != null || currentAllowCheats || isLocalOperator()
                    : isLocalOperator();
        }
    }

    private void render(float deltaTime, float partialTick) {
        handleResize();

        if (world == null || player == null) {
            renderer.setClearColor(0.08f, 0.10f, 0.12f, 1.0f);
            renderer.clear();
            screenManager.render(menuRenderer, menuInput(), deltaTime);
            restoreWorldGlState();
            capturePendingScreenshot();
            return;
        }
        drainPendingChatMessages();

        if (gameState == GameState.LOADING_WORLD) {
            renderer.setClearColor(0.08f, 0.10f, 0.12f, 1.0f);
            renderer.clear();
            renderTerrainLoadingScreen(deltaTime);
            restoreWorldGlState();
            capturePendingScreenshot();
            return;
        }

        player.setInterpolatedCameraPosition(partialTick);
        CameraFluid cameraFluid = cameraFluid();
        float waterDepth = cameraFluid == CameraFluid.WATER ? cameraWaterDepthFactor() : 0.0f;
        float rainStrength = world.getRainStrength(partialTick);
        float thunderStrength = world.getThunderStrength(partialTick);
        float lightningFlashStrength = world.getLightningFlashStrength(partialTick);
        DimensionRenderEnvironment.Snapshot renderEnvironment = DimensionRenderEnvironment.snapshot(
                world.getDimension(), dayCycleManager, cameraFluid, waterDepth,
                rainStrength, thunderStrength, lightningFlashStrength);
        org.joml.Vector3f clearColor = renderEnvironment.clearColor();
        renderer.setClearColor(clearColor.x, clearColor.y, clearColor.z, 1.0f);
        renderer.setFogColor(renderEnvironment.fogColor());
        if (renderEnvironment.distanceFog()) {
            applyNormalDistanceFog();
        } else {
            renderer.setFogDensity(renderEnvironment.fogDensity());
        }
        float gammaBoost = settings == null ? 0.0f : settings.getGamma() * 0.35f;
        renderer.setAmbientLight(Math.min(1.0f, renderEnvironment.ambientIntensity() + gammaBoost));
        renderer.setLightDirection(dayCycleManager.getSunDirection());
        renderer.setSunBrightness(renderEnvironment.sunBrightness());
        renderer.clear();

        if (settings != null && settings.isAnaglyph3d()) {
            renderAnaglyphWorldScene(deltaTime, partialTick, renderEnvironment);
        } else {
            renderWorldScene(deltaTime, partialTick, renderEnvironment);
        }

        if (!player.isDead() && gameState == GameState.PLAYING && screenManager.currentScreen() == null) {
            hudRenderer.render(window);
        }
        if (!player.isCreative()) {
            survivalHudRenderer.setDynamicItemContext(world, player);
            survivalHudRenderer.setPortalOverlayStrength(netherPortalOverlayStrength());
            survivalHudRenderer.render(player.getStats(), player.getInventory(), deltaTime);
        } else {
            survivalHudRenderer.setDynamicItemContext(world, player);
            survivalHudRenderer.setPortalOverlayStrength(netherPortalOverlayStrength());
            survivalHudRenderer.renderHotbarOnly(player.getInventory(), deltaTime);
        }
        renderBossHud();
        inventoryRenderer.setDynamicItemContext(world, player);
        inventoryRenderer.render(inventoryScreen);
        inventoryRenderer.renderCraftingTable(craftingTableScreen);
        inventoryRenderer.renderChest(chestScreen);
        inventoryRenderer.renderFurnace(furnaceScreen);
        inventoryRenderer.renderDispenser(dispenserScreen);
        inventoryRenderer.renderBrewingStand(brewingStandScreen);
        inventoryRenderer.renderEnchantingTable(enchantingTableScreen);
        inventoryRenderer.renderSignEditor(signEditScreen);

        if (player.isDead()) {
            deathScreen.render(player.getDeathTime(), (float) Input.getMouseX(), (float) Input.getMouseY(),
                    player.getStats().getProgression().getScore(), currentHardcore);
        }
        renderBedSleepOverlay();
        renderMultiplayerPlayerList();
        if (chatOverlay != null) {
            chatOverlay.render(menuRenderer, guiWidth(), guiHeight(), deltaTime);
        }
        screenManager.render(menuRenderer, menuInput(), deltaTime);
        if (screenManager.currentScreen() instanceof CreativeInventoryScreen creativeInventoryScreen) {
            inventoryRenderer.renderCreative(creativeInventoryScreen, menuRenderer.guiScale());
        }
        if (debugOverlayVisible) {
            survivalHudRenderer.renderDebugOverlay(buildDebugOverlaySnapshot());
        }
        restoreWorldGlState();
        capturePendingScreenshot();
    }

    private SurvivalHudRenderer.DebugOverlaySnapshot buildDebugOverlaySnapshot() {
        org.joml.Vector3f pos = player.getPosition();
        int blockX = (int) Math.floor(pos.x);
        int blockY = (int) Math.floor(pos.y);
        int blockZ = (int) Math.floor(pos.z);
        int chunkX = Math.floorDiv(blockX, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(blockZ, Chunk.DEPTH);
        int localX = Math.floorMod(blockX, Chunk.WIDTH);
        int localY = Math.max(0, Math.min(Chunk.HEIGHT - 1, blockY));
        int localZ = Math.floorMod(blockZ, Chunk.DEPTH);
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long usedMemory = totalMemory - runtime.freeMemory();
        long worldTime = dayCycleManager == null ? 0L : dayCycleManager.getWorldTime();
        int day = dayCycleManager == null ? 0 : dayCycleManager.getDayCount();
        int moonPhase = dayCycleManager == null ? 0 : dayCycleManager.getMoonPhase();
        return new SurvivalHudRenderer.DebugOverlaySnapshot(
                true,
                "CraftZero Release 1.0 parity",
                timer == null ? 0 : timer.getFps(),
                window == null ? 0 : window.getWidth(),
                window == null ? 0 : window.getHeight(),
                world.getDimension().getSaveName(),
                world.getGeneratorId(),
                world.getSeed(),
                prettyEnum(player.getGameMode().name()),
                prettyEnum(player.getDifficulty().name()),
                pos.x,
                pos.y,
                pos.z,
                blockX,
                blockY,
                blockZ,
                chunkX,
                chunkZ,
                localX,
                localY,
                localZ,
                player.getCamera().getYaw(),
                player.getCamera().getPitch(),
                debugFacingName(player.getCamera().getYaw()),
                prettyEnum(world.getReleaseBiome(blockX, blockZ).name()),
                world.getSkyLight(blockX, localY, blockZ),
                world.getBlockLightIfLoaded(blockX, localY, blockZ, 0),
                debugTargetBlockText(player.getTargetBlock()),
                world.getWeatherState(),
                world.getRainStrength(1.0f),
                world.getThunderStrength(1.0f),
                worldTime,
                day,
                moonPhase,
                world.getRenderDistanceChunks(),
                world.getLoadedChunks().size(),
                world.getEntitiesIncludingPending().size(),
                world.getDroppedItems().size(),
                world.getParticles().size(),
                usedMemory,
                totalMemory,
                runtime.maxMemory());
    }

    private String debugTargetBlockText(Raycast.RaycastResult target) {
        if (target == null || !target.hit || target.blockPos == null) {
            return "miss";
        }
        int x = target.blockPos.x;
        int y = target.blockPos.y;
        int z = target.blockPos.z;
        BlockType type = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
        return type + " @ " + x + " " + y + " " + z
                + " face " + debugFaceName(target.face)
                + String.format(java.util.Locale.ROOT, " %.2fm", target.distance);
    }

    private static String debugFacingName(float yaw) {
        int quadrant = Math.floorMod(Math.round(yaw / 90.0f), 4);
        return switch (quadrant) {
            case 1 -> "east";
            case 2 -> "south";
            case 3 -> "west";
            default -> "north";
        };
    }

    private static String debugFaceName(int face) {
        return switch (face) {
            case Block.FACE_TOP -> "top";
            case Block.FACE_BOTTOM -> "bottom";
            case Block.FACE_NORTH -> "north";
            case Block.FACE_SOUTH -> "south";
            case Block.FACE_EAST -> "east";
            case Block.FACE_WEST -> "west";
            default -> "none";
        };
    }

    private static String prettyEnum(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
    }

    private void capturePendingScreenshot() {
        if (!screenshotRequested || window == null) {
            return;
        }
        screenshotRequested = false;
        int width = Math.max(1, window.getWidth());
        int height = Math.max(1, window.getHeight());
        ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 4);
        ByteBuffer flipped = BufferUtils.createByteBuffer(width * height * 4);

        glReadBuffer(GL_BACK);
        glPixelStorei(GL_PACK_ALIGNMENT, 1);
        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixels);

        int stride = width * 4;
        for (int y = 0; y < height; y++) {
            int sourceRow = (height - 1 - y) * stride;
            int targetRow = y * stride;
            for (int x = 0; x < stride; x++) {
                flipped.put(targetRow + x, pixels.get(sourceRow + x));
            }
        }

        try {
            Path path = nextScreenshotPath();
            boolean written = org.lwjgl.stb.STBImageWrite.stbi_write_png(path.toString(), width, height, 4, flipped,
                    stride);
            addChatMessage(written ? "Saved screenshot as " + path : "Could not save screenshot");
        } catch (Exception e) {
            addChatMessage("Could not save screenshot: " + e.getMessage());
        }
    }

    private Path nextScreenshotPath() throws java.io.IOException {
        Path directory = Paths.get("screenshots");
        Files.createDirectories(directory);
        String timestamp = LocalDateTime.now().format(SCREENSHOT_TIMESTAMP);
        Path candidate = directory.resolve(timestamp + ".png");
        int suffix = 1;
        while (Files.exists(candidate)) {
            candidate = directory.resolve(timestamp + "_" + suffix + ".png");
            suffix++;
        }
        return candidate;
    }

    private void renderAnaglyphWorldScene(float deltaTime, float partialTick,
            DimensionRenderEnvironment.Snapshot renderEnvironment) {
        com.craftzero.graphics.Camera camera = player.getCamera();
        org.joml.Vector3f originalPosition = new org.joml.Vector3f(camera.getPosition());
        org.joml.Vector3f eyeOffset = anaglyphEyeOffset(camera);
        try {
            renderer.setAnaglyphColorCorrection(true);
            droppedItemRenderer.setAnaglyphColorCorrection(true);
            renderAnaglyphEye(camera, originalPosition, eyeOffset, -1.0f,
                    true, false, false, deltaTime, partialTick, renderEnvironment);

            glClear(GL_DEPTH_BUFFER_BIT);
            renderAnaglyphEye(camera, originalPosition, eyeOffset, 1.0f,
                    false, true, true, 0.0f, partialTick, renderEnvironment);
        } finally {
            renderer.setAnaglyphColorCorrection(false);
            droppedItemRenderer.setAnaglyphColorCorrection(false);
            glColorMask(true, true, true, true);
            camera.setPosition(originalPosition);
        }
    }

    private org.joml.Vector3f anaglyphEyeOffset(com.craftzero.graphics.Camera camera) {
        org.joml.Vector3f right = new org.joml.Vector3f(camera.getRight());
        if (!Float.isFinite(right.x) || !Float.isFinite(right.y) || !Float.isFinite(right.z)
                || right.lengthSquared() < 0.000001f) {
            right.set(1.0f, 0.0f, 0.0f);
        } else {
            right.normalize();
        }
        return right.mul(ANAGLYPH_HALF_EYE_OFFSET_BLOCKS);
    }

    private void renderAnaglyphEye(com.craftzero.graphics.Camera camera,
            org.joml.Vector3f originalPosition,
            org.joml.Vector3f eyeOffset,
            float side,
            boolean red,
            boolean green,
            boolean blue,
            float deltaTime,
            float partialTick,
            DimensionRenderEnvironment.Snapshot renderEnvironment) {
        camera.setPosition(new org.joml.Vector3f(originalPosition).add(
                eyeOffset.x * side,
                eyeOffset.y * side,
                eyeOffset.z * side));
        glColorMask(red, green, blue, false);
        renderWorldScene(deltaTime, partialTick, renderEnvironment);
    }

    private void renderWorldScene(float deltaTime, float partialTick,
            DimensionRenderEnvironment.Snapshot renderEnvironment) {
        if (renderEnvironment.renderCelestialSky()) {
            skyRenderer.render(renderer, dayCycleManager, player.getCamera());
        }
        world.render(renderer, player.getCamera(), () -> {
            blockHighlightRenderer.render(player.getCamera(), player.getTargetBlock(), world);
            if (player.isBreaking()) {
                blockBreakingRenderer.render(
                        player.getCamera(),
                        player.getBreakingBlockPos(),
                        player.getBreakProgress(),
                        world);
            }
        });

        chestRenderer.render(renderer, player.getCamera(), world, partialTick);
        enchantingTableRenderer.render(renderer, player.getCamera(), world, partialTick);
        if (settings.isClouds() && renderEnvironment.renderClouds()) {
            cloudRenderer.render(renderer, dayCycleManager, player.getCamera(), deltaTime,
                    renderEnvironment.cloudBrightnessMultiplier());
        }
        droppedItemRenderer.render(player.getCamera(), world.getDroppedItems(), world.getAtlas(),
                com.craftzero.graphics.GuiTexture.getItemsTexture(), dayCycleManager, world);

        renderer.beginRender(player.getCamera());
        mobRenderer.renderAll(world.getEntities(), player.getCamera(), partialTick, world.getAtlas());
        mobSpawnerRenderer.render(world, player.getCamera(), partialTick, world.getAtlas());
        movingPistonRenderer.render(world, player.getCamera(), world.getAtlas(), partialTick);
        fallingBlockRenderer.renderAll(world.getEntities(), player.getCamera(), world.getAtlas(), partialTick);
        arrowRenderer.renderAll(world.getEntities(), player.getCamera(),
                com.craftzero.graphics.GuiTexture.getItemsTexture(), world.getAtlas(), partialTick);
        lightningRenderer.render(world, player.getCamera(), partialTick);
        precipitationRenderer.render(world, player.getCamera(), partialTick);
        particleRenderer.render(world, player.getCamera(), partialTick, world.getAtlas(),
                settings == null ? 0 : settings.getParticles());
        signTextRenderer.render(world, player.getCamera());
        renderRemotePlayers(deltaTime, partialTick);
        playerRenderer.render(player, player.getCamera(), partialTick, player.getCameraMode());
        renderer.endRender();
    }

    private void renderRemotePlayers(float deltaTime, float partialTick) {
        if (playerRenderer == null) {
            return;
        }
        if (multiplayerServer != null) {
            for (com.google.gson.JsonObject state : multiplayerServer.playerStates().values()) {
                if (!remotePlayerStateTargetsCurrentDimension(state)) {
                    String playerId = jsonString(state, "playerId", "");
                    if (!playerId.isBlank()) {
                        remotePlayers.remove(playerId);
                    }
                    continue;
                }
                renderRemotePlayerState(state, deltaTime, partialTick);
            }
            return;
        }
        for (RemotePlayerView view : remotePlayers.values()) {
            view.tick(deltaTime);
            playerRenderer.render(view.player(), view.player().getCamera(), partialTick, 1);
        }
    }

    private void renderRemotePlayerState(com.google.gson.JsonObject state, float deltaTime, float partialTick) {
        if (state == null || !state.has("playerId") || !remotePlayerStateTargetsCurrentDimension(state)) {
            return;
        }
        String playerId = state.get("playerId").getAsString();
        String username = state.has("username") ? state.get("username").getAsString() : playerId;
        RemotePlayerView view = remotePlayers.computeIfAbsent(playerId, key -> new RemotePlayerView(username));
        view.player().setPlayerName(username);
        applyRemotePlayerMotionMetadata(view, NetworkMessage.of("playerState", state));
        view.player().applyRemotePose(
                state.has("x") ? state.get("x").getAsFloat() : view.player().getPosition().x,
                state.has("y") ? state.get("y").getAsFloat() : view.player().getPosition().y,
                state.has("z") ? state.get("z").getAsFloat() : view.player().getPosition().z,
                state.has("yaw") ? state.get("yaw").getAsFloat() : view.player().getCamera().getYaw(),
                state.has("pitch") ? state.get("pitch").getAsFloat() : view.player().getCamera().getPitch(),
                !state.has("onGround") || state.get("onGround").getAsBoolean());
        applyRemotePlayerMetadata(view, NetworkMessage.of("playerState", state));
        view.tick(deltaTime);
        playerRenderer.render(view.player(), view.player().getCamera(), partialTick, 1);
    }

    private void renderTerrainLoadingScreen(float deltaTime) {
        menuRenderer.renderDirtBackground();
        int width = guiWidth();
        int height = guiHeight();
        int centerX = width / 2;
        int centerY = height / 2;
        float progress = terrainLoadProgress == null ? 0.0f : terrainLoadProgress.progress();
        progress = Math.max(0.0f, Math.min(1.0f, progress));

        menuRenderer.drawCenteredText("Building terrain", centerX, centerY - 32, 1.0f,
                new float[] { 1.0f, 1.0f, 1.0f, 1.0f });
        int barX = centerX - TERRAIN_LOADING_BAR_WIDTH / 2;
        int barY = centerY - 12;
        drawTerrainLoadingProgressBar(barX, barY, TERRAIN_LOADING_BAR_WIDTH, TERRAIN_LOADING_BAR_HEIGHT, progress);

        String detail = Math.round(progress * 100.0f) + "%";
        menuRenderer.drawCenteredText(detail, centerX, centerY + 6, 0.8f,
                new float[] { 0.82f, 0.82f, 0.82f, 1.0f });
        if (terrainLoadProgress != null && terrainLoadProgress.total() > 0) {
            String chunks = terrainLoadProgress.readyChunks() + " / " + terrainLoadProgress.total();
            menuRenderer.drawCenteredText(chunks, centerX, centerY + 18, 0.65f,
                    new float[] { 0.42f, 0.42f, 0.42f, 1.0f });
        }
    }

    private void drawTerrainLoadingProgressBar(int x, int y, int width, int height, float progress) {
        int fillWidth = Math.max(0, Math.min(width - 2, Math.round((width - 2) * progress)));
        menuRenderer.drawRect(x - 2, y - 2, width + 4, height + 4, 0.0f, 0.0f, 0.0f, 0.78f);
        menuRenderer.drawRect(x - 1, y - 1, width + 2, 1, 0.62f, 0.62f, 0.62f, 0.65f);
        menuRenderer.drawRect(x - 1, y - 1, 1, height + 2, 0.56f, 0.56f, 0.56f, 0.58f);
        menuRenderer.drawRect(x - 1, y + height, width + 2, 1, 0.04f, 0.04f, 0.04f, 0.95f);
        menuRenderer.drawRect(x + width, y - 1, 1, height + 2, 0.04f, 0.04f, 0.04f, 0.88f);
        menuRenderer.drawRect(x, y, width, height, 0.0f, 0.0f, 0.0f, 1.0f);
        if (fillWidth <= 0) {
            return;
        }
        menuRenderer.drawRect(x + 1, y + 1, fillWidth, height - 2,
                0.54f, 0.54f, 0.54f, 1.0f);
        menuRenderer.drawRect(x + 1, y + 1, fillWidth, 1,
                0.82f, 0.82f, 0.82f, 0.45f);
        int shimmerOffset = Math.floorMod(Math.round(terrainLoadingTime * TERRAIN_LOADING_SHIMMER_SPEED),
                TERRAIN_LOADING_SHIMMER_SPACING);
        for (int sx = 1 + shimmerOffset; sx < fillWidth; sx += TERRAIN_LOADING_SHIMMER_SPACING) {
            menuRenderer.drawRect(x + 1 + sx, y + 1, 1, height - 2,
                    0.80f, 0.80f, 0.80f, 0.22f);
        }
    }

    private void renderBossHud() {
        if (world == null || survivalHudRenderer == null || world.getDimension() != Dimension.THE_END) {
            return;
        }
        for (com.craftzero.entity.Entity entity : world.getEntities()) {
            if (entity instanceof EnderDragon dragon && !dragon.isDead()) {
                survivalHudRenderer.renderBossBar("Ender Dragon", dragon.getHealth() / dragon.getMaxHealth());
                return;
            }
        }
    }

    private void renderBedSleepOverlay() {
        float alpha = bedSleepOverlayAlpha(activeBedSleep != null && player != null && player.isSleeping(),
                activeBedSleepTimer, BED_SLEEP_TRANSITION_SECONDS);
        if (alpha <= 0.0f || menuRenderer == null) {
            return;
        }
        menuRenderer.drawRect(0, 0, menuRenderer.logicalWidth(), menuRenderer.logicalHeight(),
                0.0f, 0.0f, 0.0f, alpha);
    }

    private void renderMultiplayerPlayerList() {
        if (!shouldRenderMultiplayerPlayerList()) {
            return;
        }
        List<MultiplayerPlayerListRow> rows = currentMultiplayerPlayerListRows();
        if (rows.isEmpty()) {
            return;
        }

        int width = guiWidth();
        int columns = Math.max(1, (rows.size() + PLAYER_LIST_MAX_ROWS - 1) / PLAYER_LIST_MAX_ROWS);
        int visibleRows = Math.max(1, (rows.size() + columns - 1) / columns);
        int widest = 0;
        for (MultiplayerPlayerListRow row : rows) {
            String name = row.username();
            widest = Math.max(widest, textRenderer == null ? name.length() * 6 : textRenderer.getStringWidth(name, 1.0f));
        }
        int cellWidth = Math.min(174, Math.max(104, widest + 34));
        int maxPanelWidth = Math.max(96, width - 20);
        if (columns * cellWidth + 8 > maxPanelWidth) {
            cellWidth = Math.max(64, (maxPanelWidth - 8) / columns);
        }

        String title = "Players";
        int titleWidth = textRenderer == null ? title.length() * 6 : textRenderer.getStringWidth(title, 1.0f);
        String countText = rows.size() + "/" + Math.max(rows.size(), multiplayerMaxPlayers);
        int countWidth = textRenderer == null ? countText.length() * 6 : textRenderer.getStringWidth(countText, 0.75f);
        int panelWidth = Math.min(maxPanelWidth, Math.max(titleWidth + 24, columns * cellWidth + 10));
        panelWidth = Math.max(panelWidth, titleWidth + countWidth + 28);
        int panelHeight = PLAYER_LIST_HEADER_HEIGHT + visibleRows * PLAYER_LIST_ROW_HEIGHT + 8;
        int panelX = (width - panelWidth) / 2;
        int panelY = 8;
        drawMultiplayerPlayerListFrame(panelX, panelY, panelWidth, panelHeight);
        menuRenderer.drawCenteredText(title, width / 2, panelY + 5, 1.0f,
                new float[] { 0.94f, 0.94f, 0.94f, 1.0f });
        menuRenderer.drawText(countText, panelX + panelWidth - countWidth - 8, panelY + 6, 0.75f,
                new float[] { 0.58f, 0.58f, 0.58f, 1.0f });

        String localName = localPlayerName();
        for (int i = 0; i < rows.size(); i++) {
            int column = i / visibleRows;
            int row = i % visibleRows;
            MultiplayerPlayerListRow playerRow = rows.get(i);
            int cellX = panelX + 6 + column * cellWidth;
            int cellY = panelY + PLAYER_LIST_HEADER_HEIGHT + 3 + row * PLAYER_LIST_ROW_HEIGHT;
            if (column > 0 && row == 0) {
                int separatorX = cellX - 4;
                menuRenderer.drawRect(separatorX, panelY + PLAYER_LIST_HEADER_HEIGHT,
                        1, visibleRows * PLAYER_LIST_ROW_HEIGHT + 3,
                        1.0f, 1.0f, 1.0f, 0.08f);
            }
            drawMultiplayerPlayerListRow(playerRow, localName, cellX, cellY, cellWidth, row);
        }
    }

    private void drawMultiplayerPlayerListFrame(int x, int y, int width, int height) {
        menuRenderer.drawRect(x, y, width, height, 0.0f, 0.0f, 0.0f, 0.60f);
        menuRenderer.drawRect(x + 1, y + 1, width - 2, height - 2, 0.08f, 0.08f, 0.08f, 0.30f);
        menuRenderer.drawRect(x, y, width, 1, 0.62f, 0.62f, 0.62f, 0.42f);
        menuRenderer.drawRect(x, y, 1, height, 0.50f, 0.50f, 0.50f, 0.28f);
        menuRenderer.drawRect(x, y + height - 1, width, 1, 0.02f, 0.02f, 0.02f, 0.90f);
        menuRenderer.drawRect(x + width - 1, y, 1, height, 0.02f, 0.02f, 0.02f, 0.82f);
        menuRenderer.drawRect(x + 4, y + PLAYER_LIST_HEADER_HEIGHT - 2, width - 8, 1,
                1.0f, 1.0f, 1.0f, 0.13f);
        menuRenderer.drawRect(x + 4, y + PLAYER_LIST_HEADER_HEIGHT - 1, width - 8, 1,
                0.0f, 0.0f, 0.0f, 0.42f);
    }

    private void drawMultiplayerPlayerListRow(MultiplayerPlayerListRow playerRow, String localName,
            int cellX, int cellY, int cellWidth, int row) {
        boolean localRow = playerRow.username().equalsIgnoreCase(localName);
        int rowWidth = Math.max(1, cellWidth - 8);
        float rowAlpha = localRow ? 0.40f : (row % 2 == 0 ? 0.18f : 0.10f);
        menuRenderer.drawRect(cellX - 2, cellY - 1, rowWidth, PLAYER_LIST_ROW_HEIGHT - 1,
                localRow ? 0.22f : 0.0f, localRow ? 0.18f : 0.0f, localRow ? 0.02f : 0.0f, rowAlpha);
        if (localRow) {
            menuRenderer.drawRect(cellX - 2, cellY - 1, 2, PLAYER_LIST_ROW_HEIGHT - 1,
                    1.0f, 1.0f, 0.36f, 0.78f);
        }
        String name = fitMenuRow(playerRow.username(), cellWidth - 34);
        float[] color = localRow
                ? new float[] { 1.0f, 1.0f, 0.48f, 1.0f }
                : new float[] { 0.96f, 0.96f, 0.96f, 1.0f };
        menuRenderer.drawText(name, cellX + (localRow ? 3 : 0), cellY, 1.0f, color);
        renderMultiplayerPingBars(cellX + cellWidth - 22, cellY + 1, playerRow.latencyMillis());
    }

    private void renderMultiplayerPingBars(int x, int y, int latencyMillis) {
        int activeBars = multiplayerPingBars(latencyMillis);
        boolean unknown = latencyMillis < 0;
        float[] activeColor = multiplayerPingColor(latencyMillis);
        menuRenderer.drawRect(x - 1, y + 7, 16, 1, 0.0f, 0.0f, 0.0f, 0.52f);
        for (int i = 0; i < 5; i++) {
            int barHeight = 2 + i;
            int barX = x + i * 3;
            int barY = y + 7 - barHeight;
            boolean active = !unknown && i < activeBars;
            menuRenderer.drawRect(barX, barY, 2, barHeight, 0.0f, 0.0f, 0.0f, 0.42f);
            float r = active ? activeColor[0] : 0.20f;
            float g = active ? activeColor[1] : 0.20f;
            float b = active ? activeColor[2] : 0.20f;
            float a = active ? 1.0f : (unknown && i == 0 ? 0.7f : 0.45f);
            menuRenderer.drawRect(barX, barY, 1, barHeight, r, g, b, a);
            menuRenderer.drawRect(barX + 1, barY, 1, barHeight, Math.min(1.0f, r + 0.20f),
                    Math.min(1.0f, g + 0.20f), Math.min(1.0f, b + 0.20f), a * 0.72f);
        }
    }

    private static int multiplayerPingBars(int latencyMillis) {
        if (latencyMillis < 0) {
            return 1;
        }
        if (latencyMillis < 150) {
            return 5;
        }
        if (latencyMillis < 300) {
            return 4;
        }
        if (latencyMillis < 600) {
            return 3;
        }
        if (latencyMillis < 1000) {
            return 2;
        }
        return 1;
    }

    private static float[] multiplayerPingColor(int latencyMillis) {
        if (latencyMillis < 0) {
            return new float[] { 0.45f, 0.45f, 0.45f };
        }
        if (latencyMillis < 300) {
            return new float[] { 0.34f, 0.95f, 0.28f };
        }
        if (latencyMillis < 600) {
            return new float[] { 0.95f, 0.82f, 0.24f };
        }
        return new float[] { 0.95f, 0.28f, 0.22f };
    }

    private boolean shouldRenderMultiplayerPlayerList() {
        return Input.isKeyDown(GLFW_KEY_TAB)
                && (multiplayerServer != null
                || (clientMultiplayerWorld && multiplayerClient != null && multiplayerClient.isConnected()))
                && gameState == GameState.PLAYING
                && screenManager.currentScreen() == null
                && (chatOverlay == null || !chatOverlay.isOpen())
                && player != null
                && world != null
                && !player.isDead()
                && noGameplayScreenOpen();
    }

    private List<MultiplayerPlayerListRow> currentMultiplayerPlayerListRows() {
        LinkedHashMap<String, MultiplayerPlayerListRow> names = new LinkedHashMap<>();
        addMultiplayerPlayerName(names, localPlayerName(), 0);
        if (multiplayerServer != null) {
            for (MultiplayerServer.ConnectedPlayer connectedPlayer : multiplayerServer.connectedPlayers()) {
                addMultiplayerPlayerName(names, connectedPlayer.username(), connectedPlayer.latencyMillis());
            }
        }
        for (MultiplayerRosterEntry entry : multiplayerRoster.values()) {
            addMultiplayerPlayerName(names, entry.username(), entry.latencyMillis());
        }
        for (RemotePlayerView view : remotePlayers.values()) {
            addMultiplayerPlayerName(names, view.player().getPlayerName(), -1);
        }
        return new java.util.ArrayList<>(names.values());
    }

    private static void addMultiplayerPlayerName(LinkedHashMap<String, MultiplayerPlayerListRow> names,
            String playerName, int latencyMillis) {
        if (names == null || playerName == null || playerName.isBlank()) {
            return;
        }
        String cleaned = playerName.trim();
        String key = cleaned.toLowerCase(java.util.Locale.ROOT);
        MultiplayerPlayerListRow existing = names.get(key);
        if (existing == null || (existing.latencyMillis() < 0 && latencyMillis >= 0)) {
            names.put(key, new MultiplayerPlayerListRow(cleaned, latencyMillis));
        }
    }

    static float bedSleepOverlayAlpha(boolean activeSleep, float elapsedSeconds, float transitionSeconds) {
        if (!activeSleep) {
            return 0.0f;
        }
        if (transitionSeconds <= 0.0f) {
            return BED_SLEEP_OVERLAY_MAX_ALPHA;
        }
        float progress = Math.max(0.0f, Math.min(1.0f, elapsedSeconds / transitionSeconds));
        return progress * BED_SLEEP_OVERLAY_MAX_ALPHA;
    }

    private void restoreWorldGlState() {
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
    }

    private void handleResize() {
        if (!window.isResized()) {
            return;
        }
        glViewport(0, 0, window.getWidth(), window.getHeight());
        if (player != null) {
            player.getCamera().setAspectRatio(window.getWidth(), window.getHeight());
        }
        hudRenderer.updateOrtho(window.getWidth(), window.getHeight());
        survivalHudRenderer.updateOrtho(window.getWidth(), window.getHeight());
        inventoryRenderer.updateOrtho(window.getWidth(), window.getHeight());
        deathScreen.updateOrtho(window.getWidth(), window.getHeight());
        textRenderer.updateOrtho(window.getWidth(), window.getHeight());
        enchantmentTextRenderer.updateOrtho(window.getWidth(), window.getHeight());
        menuRenderer.updateOrtho(window.getWidth(), window.getHeight());
        updateMenuScale();
        rebuildCurrentMenuForResize();
        window.setResized(false);
    }

    private void rebuildCurrentMenuForResize() {
        Screen active = screenManager == null ? null : screenManager.currentScreen();
        if (active == null) {
            return;
        }
        if (active instanceof CreativeInventoryScreen creativeInventoryScreen) {
            creativeInventoryScreen.resize(guiWidth(), guiHeight());
            return;
        }
        switch (gameState) {
            case TITLE -> openTitleScreen();
            case WORLD_SELECT -> openWorldSelectScreen(false);
            case MULTIPLAYER -> openMultiplayerScreen();
            case PAUSED -> openPauseScreen();
            case END_CREDITS -> openEndCreditsScreen();
            case OPTIONS -> openOptionsScreen(player == null ? () -> {
                saveSettings();
                openTitleScreen();
            } : this::resumePauseMenu);
            case DEATH -> openDeathMenuIfNeeded();
            default -> {
            }
        }
    }

    private void loadWorld(WorldInfo worldInfo, boolean host) {
        if (worldInfo == null) {
            return;
        }
        try {
            unloadWorld(false);
            gameState = GameState.LOADING_WORLD;
            screenManager.clear();
            currentWorldInfo = worldInfo;
            saveManager = new SaveManager(worldInfo.path());
            SaveManager.SaveLoadResult loadResult = saveManager.loadLevel();
            if (loadResult.shouldBlockLoad()) {
                String message = loadResult.error() == null ? "Could not load world." : loadResult.error().message();
                openMessageScreen("World Load Failed", message);
                saveManager = null;
                currentWorldInfo = null;
                gameState = GameState.TITLE;
                return;
            }
            loadedLevel = loadResult.levelData();
            boolean serverPropertiesBootstrap = false;
            if (loadedLevel == null) {
                loadedLevel = saveManager.createServerPropertiesBootstrap(worldInfo.displayName(), worldInfo.seed(),
                        worldInfo.gameMode(), worldInfo.difficulty());
                serverPropertiesBootstrap = loadedLevel != null;
            }
            clientMultiplayerWorld = false;
            long seed = loadedLevel != null ? loadedLevel.seed : worldInfo.seed();
            String generatorId = loadedLevel != null ? loadedLevel.generatorId : WorldGenerator.RELEASE_ONE;
            Dimension dimension = loadedLevel != null ? Dimension.fromSaveName(loadedLevel.dimension) : Dimension.OVERWORLD;
            multiplayerGenerateStructures = loadedLevel == null
                    || loadedLevel.shouldGenerateStructures();
            currentGameMode = loadedLevel != null ? loadedLevel.getGameMode() : worldInfo.gameMode();
            currentHardcore = currentGameMode == GameMode.HARDCORE || (loadedLevel != null && loadedLevel.hardcore);
            currentDifficulty = currentHardcore ? Difficulty.HARD
                    : (loadedLevel != null ? loadedLevel.getDifficulty() : worldInfo.difficulty());
            currentAllowCheats = loadedLevel != null && loadedLevel.allowCheats;
            worldSpawnX = loadedLevel != null ? loadedLevel.spawnX : 0;
            worldSpawnY = loadedLevel != null ? loadedLevel.spawnY : 80;
            worldSpawnZ = loadedLevel != null ? loadedLevel.spawnZ : 0;
            weatherState = loadedLevel != null ? World.normalizeWeatherState(loadedLevel.weatherState) : "clear";
            restoreAdminState(loadedLevel);
            restoreServerProperties(loadedLevel);
            configureSaveMetadata();

            dayCycleManager = new DayCycleManager();
            world = new World(seed, generatorId, dimension, multiplayerGenerateStructures);
            world.setSpawnNpcs(multiplayerSpawnNpcs);
            installWorldNetworkHooks(world);
            world.setSaveManager(saveManager);
            world.init();
            world.setWorldSpawn(worldSpawnX, worldSpawnY, worldSpawnZ);
            world.setWeatherState(weatherState);

            survivalHudRenderer.setAtlas(world.getAtlas());
            inventoryRenderer.setAtlas(world.getAtlas());
            playerRenderer.setTextures(world.getAtlas(), com.craftzero.graphics.GuiTexture.getItemsTexture());

            player = new Player(0, 80, 0);
            player.setGameMode(currentGameMode);
            player.setDifficulty(currentDifficulty);
            player.applySettings(settings);
            player.getCamera().setFov(settingsFovDegrees());
            player.setDifficulty(currentDifficulty);
            world.setPlayer(player);
            player.setWorld(world);
            world.setDayCycleManager(dayCycleManager);
            world.setRenderDistanceChunks(effectiveWorldRenderDistanceChunks());
            world.setFancyGraphics(settings.isFancyGraphics());
            world.setSmoothLighting(settings.isSmoothLighting());
            world.setAdvancedOpenGl(settings.isAdvancedOpenGl());
            saveManager.applyLevel(loadedLevel, player, dayCycleManager, world);
            if (loadedLevel == null || serverPropertiesBootstrap || loadedLevel.player == null) {
                com.craftzero.world.ReleaseOneWorldGenerator.SpawnPoint spawn = world.findSafeSpawn();
                worldSpawnX = spawn.x();
                worldSpawnY = spawn.y();
                worldSpawnZ = spawn.z();
                world.setWorldSpawn(worldSpawnX, worldSpawnY, worldSpawnZ);
                player.setPosition(spawn.x() + 0.5f, spawn.y(), spawn.z() + 0.5f);
                player.setSpawnPosition(spawn.x() + 0.5f, spawn.y(), spawn.z() + 0.5f);
            }
            player.getStats().getStatistics().recordWorldLoaded();

            mobSpawner = new MobSpawner(world);
            inventoryScreen = new InventoryScreen(player.getInventory(), this::isDropBindingPressed);
            chestScreen = new ChestScreen(player.getInventory(), this::isInventoryBindingPressed,
                    this::isDropBindingPressed);
            furnaceScreen = new FurnaceScreen(player.getInventory(), this::isInventoryBindingPressed,
                    this::isDropBindingPressed);
            dispenserScreen = new DispenserScreen(player.getInventory(), this::isInventoryBindingPressed,
                    this::isDropBindingPressed);
            brewingStandScreen = new BrewingStandScreen(player.getInventory(), this::isInventoryBindingPressed,
                    this::isDropBindingPressed);
            enchantingTableScreen = new EnchantingTableScreen(player.getInventory(), this::isInventoryBindingPressed,
                    this::isDropBindingPressed);
            signEditScreen = new SignEditScreen();
            craftingTableScreen = new CraftingTableScreen(player.getInventory(), this::isInventoryBindingPressed,
                    this::isDropBindingPressed);

            autosaveTimer = 0.0f;
            savingEnabled = true;
            paused = false;
            deathMenuOpen = false;
            beginTerrainLoading(host);

            System.out.println("Loaded world '" + worldInfo.displayName() + "' at " + worldInfo.path());
            System.out.println("Seed: " + seed + " | Generator: " + world.getGeneratorId()
                    + " | Dimension: " + world.getDimension().getSaveName()
                    + " | Mode: " + currentGameMode + " | Difficulty: " + currentDifficulty);
        } catch (Exception e) {
            e.printStackTrace();
            unloadWorld(false);
            openTitleScreen();
        }
    }

    private void startMultiplayerClientWorld(MultiplayerClient client) throws Exception {
        gameState = GameState.LOADING_WORLD;
        screenManager.clear();
        currentWorldInfo = null;
        saveManager = null;
        loadedLevel = null;
        currentGameMode = parseNetworkGameMode(client.worldGameMode());
        currentHardcore = client.worldHardcore() || currentGameMode == GameMode.HARDCORE;
        currentDifficulty = currentHardcore ? Difficulty.HARD : parseNetworkDifficulty(client.worldDifficulty());
        currentAllowCheats = client.worldAllowCheats();
        restoreServerProperties(null);
        clientMultiplayerWorld = true;
        multiplayerMaxPlayers = Math.max(1, client.worldMaxPlayers());
        multiplayerPvp = client.worldPvp();
        multiplayerSpawnAnimals = client.worldSpawnAnimals();
        multiplayerSpawnMonsters = client.worldSpawnMonsters();
        multiplayerSpawnNpcs = client.worldSpawnNpcs();
        multiplayerAllowNether = client.worldAllowNether();
        multiplayerAllowFlight = client.worldAllowFlight();
        multiplayerViewDistance = client.worldViewDistance();
        multiplayerMaxBuildHeight = client.worldMaxBuildHeight();
        multiplayerGenerateStructures = client.worldGenerateStructures();
        worldSpawnX = client.worldSpawnX();
        worldSpawnY = client.worldSpawnY();
        worldSpawnZ = client.worldSpawnZ();
        weatherState = World.normalizeWeatherState(client.worldWeather());
        Dimension dimension = Dimension.fromSaveName(client.worldDimension());
        restoreAdminState(null);

        dayCycleManager = new DayCycleManager();
        dayCycleManager.setTime(client.worldTime());
        world = new World(client.seed(), WorldGenerators.generatorIdFor(dimension), dimension,
                multiplayerGenerateStructures);
        world.setSpawnNpcs(multiplayerSpawnNpcs);
        installWorldNetworkHooks(world);
        world.init();
        world.setWorldSpawn(worldSpawnX, worldSpawnY, worldSpawnZ);
        world.setWeatherState(weatherState);

        survivalHudRenderer.setAtlas(world.getAtlas());
        inventoryRenderer.setAtlas(world.getAtlas());
        playerRenderer.setTextures(world.getAtlas(), com.craftzero.graphics.GuiTexture.getItemsTexture());

        player = new Player(worldSpawnX + 0.5f, worldSpawnY, worldSpawnZ + 0.5f);
        player.setGameMode(currentGameMode);
        player.setDifficulty(currentDifficulty);
        player.getStats().getStatistics().recordWorldLoaded();
        player.getStats().getStatistics().recordMultiplayerJoin();
        player.applySettings(settings);
        player.getCamera().setFov(settingsFovDegrees());
        world.setPlayer(player);
        player.setWorld(world);
        world.setDayCycleManager(dayCycleManager);
        world.setRenderDistanceChunks(effectiveWorldRenderDistanceChunks());
        world.setFancyGraphics(settings.isFancyGraphics());
        world.setSmoothLighting(settings.isSmoothLighting());
        world.setAdvancedOpenGl(settings.isAdvancedOpenGl());

        mobSpawner = new MobSpawner(world);
        inventoryScreen = new InventoryScreen(player.getInventory(), this::isDropBindingPressed);
        chestScreen = new ChestScreen(player.getInventory(), this::isInventoryBindingPressed,
                this::isDropBindingPressed);
        furnaceScreen = new FurnaceScreen(player.getInventory(), this::isInventoryBindingPressed,
                this::isDropBindingPressed);
        dispenserScreen = new DispenserScreen(player.getInventory(), this::isInventoryBindingPressed,
                this::isDropBindingPressed);
        brewingStandScreen = new BrewingStandScreen(player.getInventory(), this::isInventoryBindingPressed,
                this::isDropBindingPressed);
        enchantingTableScreen = new EnchantingTableScreen(player.getInventory(), this::isInventoryBindingPressed,
                this::isDropBindingPressed);
        signEditScreen = new SignEditScreen();
        craftingTableScreen = new CraftingTableScreen(player.getInventory(), this::isInventoryBindingPressed,
                this::isDropBindingPressed);

        autosaveTimer = 0.0f;
        savingEnabled = false;
        multiplayerStateTimer = 0.0f;
        paused = false;
        deathMenuOpen = false;
        beginTerrainLoading(false);
    }

    private void unloadWorld(boolean save) {
        waitForAutosave();
        hostAfterTerrainLoad = false;
        terrainLoadingTime = 0.0f;
        terrainLoadProgress = new World.ChunkAreaProgress(1, 0, 0, 0);
        if (save) {
            if (player != null) {
                player.getStats().getStatistics().recordGameQuit();
            }
            saveGame("leave world");
        }
        closeMultiplayer();
        if (world != null) {
            world.cleanup();
        }
        world = null;
        player = null;
        mobSpawner = null;
        saveManager = null;
        loadedLevel = null;
        currentAllowCheats = false;
        restoreServerProperties(null);
        clientMultiplayerWorld = false;
        multiplayerStateTimer = 0.0f;
        pendingNetworkMessages.clear();
        deferredNetworkBlockUpdates.clear();
        inventoryScreen = null;
        chestScreen = null;
        furnaceScreen = null;
        brewingStandScreen = null;
        enchantingTableScreen = null;
        signEditScreen = null;
        craftingTableScreen = null;
        paused = false;
        deathMenuOpen = false;
        Input.setCursorLocked(false);
    }

    private void openTitleScreen() {
        gameState = GameState.TITLE;
        paused = false;
        Input.setCursorLocked(false);
        screenManager.show(MenuScreens.title(
                guiWidth(),
                guiHeight(),
                () -> openWorldSelectScreen(false),
                this::openMultiplayerScreen,
                this::openTexturePacksScreen,
                () -> openOptionsScreen(() -> {
                    saveSettings();
                    screenManager.pop();
                    openTitleScreen();
                }),
                () -> running = false));
    }

    private void openWorldSelectScreen(boolean host) {
        gameState = GameState.WORLD_SELECT;
        Input.setCursorLocked(false);
        screenManager.show(MenuScreens.worldSelect(
                guiWidth(),
                guiHeight(),
                this::listWorldsSafe,
                info -> loadWorld(info, host),
                this::openCreateWorldScreen,
                this::openRenameWorldScreen,
                this::deleteWorldAndRefresh,
                this::openTitleScreen));
    }

    private List<WorldInfo> listWorldsSafe() {
        try {
            return worldManager.listWorlds();
        } catch (Exception e) {
            System.err.println("Failed to list worlds: " + e.getMessage());
            return List.of();
        }
    }

    private void openCreateWorldScreen() {
        screenManager.show(MenuScreens.createWorld(
                guiWidth(),
                guiHeight(),
                request -> {
                    try {
                        Difficulty difficulty = request.gameMode() == GameMode.HARDCORE ? Difficulty.HARD : Difficulty.EASY;
                        WorldInfo info = worldManager.createWorld(request.name(),
                                WorldManager.parseSeed(request.seed()), request.gameMode(), difficulty);
                        loadWorld(info, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                        openWorldSelectScreen(false);
                    }
                },
                () -> openWorldSelectScreen(false)));
    }

    private void openRenameWorldScreen(WorldInfo info) {
        if (info == null) {
            return;
        }
        TextField field = new TextField(info.displayName(), 32, guiWidth() / 2 - 100, 100, 200, 20);
        Runnable done = () -> {
            try {
                worldManager.renameWorld(info.id(), field.value());
            } catch (Exception e) {
                System.err.println("Failed to rename world: " + e.getMessage());
            }
            openWorldSelectScreen(false);
        };
        BaseMenuScreen screen = new BaseMenuScreen("Rename World", true, false, () -> openWorldSelectScreen(false));
        screen.add(field.onEnter(done));
        screen.add(new MenuButton("Done", guiWidth() / 2 - 100, 140, 200, 20, done));
        screen.add(new MenuButton("Cancel", guiWidth() / 2 - 100, 168, 200, 20, () -> openWorldSelectScreen(false)));
        screenManager.show(screen);
    }

    private void deleteWorldAndRefresh(WorldInfo info) {
        if (info == null) {
            return;
        }
        try {
            worldManager.deleteWorld(info.id());
        } catch (Exception e) {
            System.err.println("Failed to delete world: " + e.getMessage());
        }
        openWorldSelectScreen(false);
    }

    private void openPauseScreen() {
        if (player == null || world == null) {
            openTitleScreen();
            return;
        }
        paused = true;
        gameState = GameState.PAUSED;
        Input.setCursorLocked(false);
        screenManager.show(MenuScreens.pause(
                guiWidth(),
                guiHeight(),
                this::resumeGame,
                this::openAchievementsScreen,
                this::openStatisticsScreen,
                () -> openOptionsScreen(this::resumePauseMenu),
                () -> {
                    unloadWorld(true);
                    openTitleScreen();
                }));
    }

    private void openAchievementsScreen() {
        if (player == null || world == null) {
            openTitleScreen();
            return;
        }
        paused = true;
        gameState = GameState.PAUSED;
        Input.setCursorLocked(false);
        screenManager.show(MenuScreens.achievements(
                guiWidth(),
                guiHeight(),
                player.getStats().getAchievements(),
                this::openPauseScreen));
    }

    private void openStatisticsScreen() {
        if (player == null || world == null) {
            openTitleScreen();
            return;
        }
        paused = true;
        gameState = GameState.PAUSED;
        Input.setCursorLocked(false);
        screenManager.show(MenuScreens.statistics(
                guiWidth(),
                guiHeight(),
                player.getStats().getStatistics(),
                this::openPauseScreen));
    }

    private void resumePauseMenu() {
        saveSettings();
        applyRuntimeSettings(false);
        openPauseScreen();
    }

    private void resumeGame() {
        screenManager.clear();
        paused = false;
        gameState = GameState.PLAYING;
        Input.setCursorLocked(true);
    }

    private void openOptionsScreen(Runnable done) {
        gameState = GameState.OPTIONS;
        BaseMenuScreen options = MenuScreens.options(
                guiWidth(),
                guiHeight(),
                settings,
                () -> screenManager.show(MenuScreens.video(guiWidth(), guiHeight(), settings,
                        () -> openOptionsScreen(done), this::settingsChangedLive)),
                () -> openControlsScreen(done),
                () -> openLanguageScreen(done),
                done,
                this::settingsChangedLive);
        screenManager.show(options);
    }

    private void openControlsScreen(Runnable done) {
        BaseMenuScreen screen = new BaseMenuScreen("Controls", true, false, () -> openOptionsScreen(done));
        int cx = guiWidth() / 2;
        int rows = (GameSettings.KeyBinding.values().length + 1) / 2;
        int buttonWidth = 74;
        int labelWidth = 72;
        int columnGap = 10;
        int labelGap = 6;
        int columnWidth = buttonWidth + labelGap + labelWidth;
        int pairWidth = columnWidth * 2 + columnGap;
        int x0 = Math.max(4, cx - pairWidth / 2);
        int y = Math.max(42, Math.min(58, guiHeight() / 5));
        int doneY = Math.max(112, guiHeight() - 28);
        int rowSpacing = Math.min(24, Math.max(18, (doneY - y - 26) / Math.max(1, rows - 1)));
        final GameSettings.KeyBinding[] waitingAction = { null };
        Map<GameSettings.KeyBinding, MenuButton> buttons = new EnumMap<>(GameSettings.KeyBinding.class);
        final Runnable[] refreshLabels = new Runnable[1];
        int index = 0;
        for (GameSettings.KeyBinding binding : GameSettings.KeyBinding.values()) {
            int column = index % 2;
            int row = index / 2;
            int rowY = y + row * rowSpacing;
            int x = x0 + column * (columnWidth + columnGap);
            MenuButton button = new MenuButton("control-" + binding.name(), keyName(settings.getKeyBinding(binding)),
                    new com.craftzero.ui.menu.Rect(x, rowY, buttonWidth, 20), null);
            button.setAction(() -> {
                waitingAction[0] = binding;
                refreshLabels[0].run();
            });
            buttons.put(binding, button);
            screen.add(button);
            int labelX = x + buttonWidth + labelGap;
            int availableLabelWidth = Math.max(40, Math.min(labelWidth, guiWidth() - labelX - 4));
            screen.add(new MenuLabel("control-label-" + binding.name(),
                    fitControlMenuText(binding.displayName(), availableLabelWidth),
                    new com.craftzero.ui.menu.Rect(labelX, rowY + 6, availableLabelWidth, 10))
                    .color(0.82f, 0.82f, 0.82f, 1.0f));
            index++;
        }
        refreshLabels[0] = () -> refreshControlButtonLabels(buttons, waitingAction[0]);
        refreshLabels[0].run();
        screen.onTick(() -> {
            if (waitingAction[0] != null && (!Input.getPressedKeys().isEmpty() || !Input.getPressedButtons().isEmpty())) {
                int key = !Input.getPressedButtons().isEmpty()
                        ? GameInput.keyCodeFromMouseButton(Input.getPressedButtons().get(0))
                        : Input.getPressedKeys().get(0);
                if (key != GLFW_KEY_ESCAPE) {
                    settings.setKeyBinding(waitingAction[0], key);
                    saveSettings();
                }
                waitingAction[0] = null;
                refreshLabels[0].run();
            }
        });
        screen.add(new MenuButton("Reset Keys", cx - 100, doneY, 98, 20, () -> {
            waitingAction[0] = null;
            settings.resetKeyBindings();
            saveSettings();
            refreshLabels[0].run();
        }));
        screen.add(new MenuButton("Done", cx + 2, doneY, 98, 20,
                () -> openOptionsScreen(done)));
        screenManager.show(screen);
    }

    private static String fitControlMenuText(String text, int width) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        int maxChars = Math.max(4, width / 6);
        if (text.length() <= maxChars) {
            return text;
        }
        if (maxChars <= 3) {
            return text.substring(0, Math.min(text.length(), maxChars));
        }
        return text.substring(0, Math.max(0, maxChars - 3)).stripTrailing() + "...";
    }

    private void openLanguageScreen(Runnable done) {
        BaseMenuScreen screen = new BaseMenuScreen("Language", true, false, () -> openOptionsScreen(done));
        int cx = guiWidth() / 2;
        int listWidth = Math.min(260, Math.max(180, guiWidth() - 48));
        List<LanguageOption> languages = languageOptions(settings.getLanguage());
        MenuLabel current = MenuLabel.centered("language-current", languageCurrentLabel(settings.getLanguage(), languages),
                cx, 50, listWidth)
                .color(1.0f, 1.0f, 0.63f, 1.0f);
        screen.add(current);
        screen.add(MenuLabel.centered("language-note",
                "Translations may not be 100% accurate.",
                cx, 64, listWidth).color(0.72f, 0.72f, 0.72f, 1.0f));

        MenuList<LanguageOption> languageList = new MenuList<>("languages",
                new com.craftzero.ui.menu.Rect(cx - listWidth / 2, 82, listWidth,
                        18 * Math.max(4, Math.min(6, (guiHeight() - 150) / 18))),
                18,
                languages,
                option -> languageRowLabel(option, settings.getLanguage()));
        int selectedIndex = languageIndex(languages, settings.getLanguage());
        if (selectedIndex >= 0) {
            languageList.setSelectedIndex(selectedIndex);
        }
        Runnable applySelectedLanguage = () -> languageList.selectedItem().ifPresent(option -> {
            settings.setLanguage(option.code());
            settingsChangedLive();
            current.text(languageCurrentLabel(settings.getLanguage(), languages));
            languageList.setItems(languages);
        });
        languageList.setOnActivated(option -> applySelectedLanguage.run());
        screen.add(languageList);
        screen.add(new MenuButton("Select", cx - 100, guiHeight() - 56, 98, 20,
                applySelectedLanguage));
        screen.add(new MenuButton("Done", cx + 2, guiHeight() - 56, 98, 20,
                () -> openOptionsScreen(done)));
        screenManager.show(screen);
    }

    private static List<LanguageOption> languageOptions(String currentLanguage) {
        String current = normalizeLanguageCode(currentLanguage);
        for (LanguageOption option : RELEASE_LANGUAGE_OPTIONS) {
            if (option.code().equals(current)) {
                return RELEASE_LANGUAGE_OPTIONS;
            }
        }
        List<LanguageOption> languages = new ArrayList<>(RELEASE_LANGUAGE_OPTIONS);
        languages.add(new LanguageOption(current, current));
        return List.copyOf(languages);
    }

    private static int languageIndex(List<LanguageOption> languages, String currentLanguage) {
        String current = normalizeLanguageCode(currentLanguage);
        for (int i = 0; i < languages.size(); i++) {
            if (languages.get(i).code().equals(current)) {
                return i;
            }
        }
        return languages.isEmpty() ? -1 : 0;
    }

    private static String languageCurrentLabel(String language, List<LanguageOption> languages) {
        String current = normalizeLanguageCode(language);
        for (LanguageOption option : languages) {
            if (option.code().equals(current)) {
                return "Language: " + option.displayName();
            }
        }
        return "Language: " + current;
    }

    private static String languageRowLabel(LanguageOption option, String currentLanguage) {
        return option.displayName() + (option.code().equals(normalizeLanguageCode(currentLanguage)) ? " *" : "");
    }

    private static String normalizeLanguageCode(String language) {
        String cleaned = language == null ? "" : language.trim();
        return cleaned.isEmpty() ? GameSettings.DEFAULT_LANGUAGE : cleaned;
    }

    private void refreshControlButtonLabels(Map<GameSettings.KeyBinding, MenuButton> buttons,
            GameSettings.KeyBinding waitingAction) {
        for (Map.Entry<GameSettings.KeyBinding, MenuButton> entry : buttons.entrySet()) {
            GameSettings.KeyBinding binding = entry.getKey();
            MenuButton button = entry.getValue();
            if (binding == waitingAction) {
                button.setLabel("> ??? <");
                button.setTextColor(1.0f, 1.0f, 0.25f, 1.0f);
                continue;
            }
            int key = settings.getKeyBinding(binding);
            button.setLabel(keyName(key));
            if (isConflictingControlBinding(binding)) {
                button.setTextColor(1.0f, 0.25f, 0.25f, 1.0f);
            } else {
                button.clearTextColor();
            }
        }
    }

    private boolean isConflictingControlBinding(GameSettings.KeyBinding binding) {
        int key = settings.getKeyBinding(binding);
        for (GameSettings.KeyBinding other : GameSettings.KeyBinding.values()) {
            if (other != binding && settings.getKeyBinding(other) == key) {
                return true;
            }
        }
        return false;
    }

    private String keyName(int key) {
        if (key < 0) {
            return "Button " + (GameInput.mouseButtonFromKeyCode(key) + 1);
        }
        String name = glfwGetKeyName(key, 0);
        return name == null ? Integer.toString(key) : name.toUpperCase();
    }

    private void openTexturePacksScreen() {
        gameState = GameState.OPTIONS;
        screenManager.show(MenuScreens.texturePacks(
                guiWidth(),
                guiHeight(),
                resourcePackManager,
                settings,
                () -> {
                    saveSettings();
                    openTitleScreen();
                }));
    }

    private void openMultiplayerScreen() {
        gameState = GameState.MULTIPLAYER;
        SavedServerList savedServers;
        try {
            savedServers = loadSavedServerList();
        } catch (Exception exception) {
            openMessageScreen("Server List Failed", exception.getMessage());
            return;
        }

        int width = guiWidth();
        int height = guiHeight();
        int cx = width / 2;
        int listWidth = Math.min(300, Math.max(220, width - 48));
        int listX = cx - listWidth / 2;
        int rowHeight = 22;
        int listY = Math.max(48, Math.min(58, height / 4));
        int visibleRows = Math.max(4, Math.min(7, Math.max(1, (height - listY - 84) / rowHeight)));
        while (visibleRows > 3 && listY + rowHeight * visibleRows + 8 > height - 68) {
            visibleRows--;
        }
        List<SavedServer> entries = savedServers.entries();
        Map<SavedServer, ServerPingStatus> pingStatuses = new java.util.concurrent.ConcurrentHashMap<>();
        startSavedServerStatusPings(entries, pingStatuses);
        MenuList<SavedServer> serverList = new MenuList<>("saved-server-list",
                new Rect(listX, listY, listWidth, rowHeight * visibleRows),
                rowHeight,
                entries,
                server -> savedServerLabel(server, pingStatuses.get(server), listWidth - 16));

        int smallButtonWidth = Math.max(72, Math.min(98, (width - 24) / 3));
        int buttonGap = 4;
        int buttonRowWidth = smallButtonWidth * 3 + buttonGap * 2;
        int buttonX = cx - buttonRowWidth / 2;
        int buttonY = Math.min(height - 68, listY + rowHeight * visibleRows + 10);
        int secondRowY = buttonY + 24;
        int cancelY = buttonY + 48;
        int fullButtonWidth = Math.min(200, Math.max(160, width - 48));
        MenuButton join = new MenuButton("join-saved-server", "Join Server",
                new Rect(buttonX, buttonY, smallButtonWidth, 20), () -> connectToSavedServer(serverList.selected()));
        MenuButton direct = new MenuButton("direct-connect", "Direct Connect",
                new Rect(buttonX + smallButtonWidth + buttonGap, buttonY, smallButtonWidth, 20),
                this::openDirectConnectScreen);
        MenuButton add = new MenuButton("add-server", "Add Server",
                new Rect(buttonX + (smallButtonWidth + buttonGap) * 2, buttonY, smallButtonWidth, 20),
                () -> openSavedServerEditScreen(null));
        MenuButton edit = new MenuButton("edit-server", "Edit",
                new Rect(buttonX, secondRowY, smallButtonWidth, 20),
                () -> openSavedServerEditScreen(serverList.selected()));
        MenuButton delete = new MenuButton("delete-server", "Delete",
                new Rect(buttonX + smallButtonWidth + buttonGap, secondRowY, smallButtonWidth, 20),
                () -> deleteSavedServer(serverList.selected()));
        join.enabled(!entries.isEmpty());
        edit.enabled(!entries.isEmpty());
        delete.enabled(!entries.isEmpty());
        serverList.setOnSelectionChanged(server -> {
            join.setEnabled(server != null);
            edit.setEnabled(server != null);
            delete.setEnabled(server != null);
        });
        serverList.setOnActivated(this::connectToSavedServer);
        if (!entries.isEmpty()) {
            serverList.setSelectedIndex(0);
        }

        BaseMenuScreen screen = new BaseMenuScreen("Multiplayer", true, false, this::openTitleScreen);
        if (entries.isEmpty()) {
            screen.add(MenuLabel.centered("saved-server-empty", "No saved servers",
                    cx, listY + rowHeight * visibleRows / 2 - 4, listWidth)
                    .color(0.65f, 0.65f, 0.65f, 1.0f));
        }
        screen.add(serverList);
        screen.add(join);
        screen.add(direct);
        screen.add(add);
        screen.add(edit);
        screen.add(delete);
        screen.add(new MenuButton("Host World",
                buttonX + (smallButtonWidth + buttonGap) * 2, secondRowY, smallButtonWidth, 20,
                () -> openWorldSelectScreen(true)));
        screen.add(new MenuButton("Cancel", cx - fullButtonWidth / 2, cancelY, fullButtonWidth, 20,
                this::openTitleScreen));
        screenManager.show(screen);
    }

    private void openDirectConnectScreen() {
        ParsedServerAddress remembered = parseServerAddressOrDefault(settings.getLastServer());
        int width = guiWidth();
        int height = guiHeight();
        int cx = width / 2;
        int fieldWidth = Math.min(260, Math.max(180, width - 64));
        int fieldX = cx - fieldWidth / 2;
        int top = Math.max(60, height / 4);
        TextField host = new TextField("direct-connect-host", new Rect(fieldX, top + 16, fieldWidth, 20),
                remembered.host(), 128);
        TextField port = new TextField("direct-connect-port", new Rect(fieldX, top + 60, fieldWidth, 20),
                Integer.toString(MultiplayerServer.DEFAULT_PORT), 8);
        port.setText(Integer.toString(remembered.port()));
        int buttonWidth = Math.min(200, fieldWidth);
        int buttonX = cx - buttonWidth / 2;
        int buttonY = Math.min(height - 56, top + 96);
        Runnable connect = () -> {
            try {
                ParsedServerAddress address = parseDirectServerAddress(host.value(), port.value());
                connectToMultiplayerServer(address.host(), address.port(), address.host(), false);
            } catch (Exception e) {
                openMessageScreen("Connection Failed", e.getMessage());
            }
        };
        BaseMenuScreen screen = new BaseMenuScreen("Direct Connect", true, false, this::openMultiplayerScreen);
        screen.add(MenuLabel.centered("direct-connect-host-label", "Server Address", cx, top + 2, fieldWidth)
                .color(0.82f, 0.82f, 0.82f, 1.0f));
        screen.add(host.onEnter(connect));
        screen.add(MenuLabel.centered("direct-connect-port-label", "Port", cx, top + 46, fieldWidth)
                .color(0.82f, 0.82f, 0.82f, 1.0f));
        screen.add(port.onEnter(connect));
        screen.add(new MenuButton("Join Server", buttonX, buttonY, buttonWidth, 20, connect));
        screen.add(new MenuButton("Cancel", buttonX, buttonY + 28, buttonWidth, 20, this::openMultiplayerScreen));
        screenManager.show(screen);
    }

    private void openSavedServerEditScreen(SavedServer existing) {
        int width = guiWidth();
        int height = guiHeight();
        int cx = width / 2;
        int fieldWidth = Math.min(260, Math.max(180, width - 64));
        int fieldX = cx - fieldWidth / 2;
        int top = Math.max(60, height / 4);
        TextField name = new TextField(existing == null ? "Minecraft Server" : existing.name(), 32,
                fieldX, top + 16, fieldWidth, 20);
        TextField address = new TextField(existing == null
                ? formatServerAddress(parseServerAddressOrDefault(settings.getLastServer()))
                : formatServerAddress(existing),
                128,
                fieldX,
                top + 60,
                fieldWidth,
                20);
        int buttonWidth = Math.min(200, fieldWidth);
        int buttonX = cx - buttonWidth / 2;
        int buttonY = Math.min(height - 56, top + 96);
        Runnable save = () -> {
            try {
                ParsedServerAddress parsed = parseServerAddress(address.value());
                String serverName = name.value().trim();
                if (serverName.isBlank()) {
                    serverName = parsed.host();
                }
                SavedServer server = new SavedServer(serverName, parsed.host(), parsed.port(),
                        existing == null ? 0L : existing.lastConnectedEpochMillis());
                SavedServerList savedServers = loadSavedServerList();
                if (existing != null) {
                    savedServers.remove(existing.host(), existing.port());
                }
                savedServers.addOrUpdate(server);
                savedServers.save();
                openMultiplayerScreen();
            } catch (Exception exception) {
                openMessageScreen("Server Save Failed", exception.getMessage());
            }
        };
        BaseMenuScreen screen = new BaseMenuScreen(existing == null ? "Add Server" : "Edit Server",
                true, false, this::openMultiplayerScreen);
        screen.add(MenuLabel.centered("server-name-label", "Server Name", cx, top + 2, fieldWidth)
                .color(0.82f, 0.82f, 0.82f, 1.0f));
        screen.add(name.onEnter(save));
        screen.add(MenuLabel.centered("server-address-label", "Server Address", cx, top + 46, fieldWidth)
                .color(0.82f, 0.82f, 0.82f, 1.0f));
        screen.add(address.onEnter(save));
        screen.add(new MenuButton("Done", buttonX, buttonY, buttonWidth, 20, save));
        screen.add(new MenuButton("Cancel", buttonX, buttonY + 28, buttonWidth, 20, this::openMultiplayerScreen));
        screenManager.show(screen);
    }

    private void connectToSavedServer(SavedServer server) {
        if (server == null) {
            return;
        }
        connectToMultiplayerServer(server.host(), server.port(), server.name(), true);
    }

    private void deleteSavedServer(SavedServer server) {
        if (server == null) {
            return;
        }
        try {
            SavedServerList savedServers = loadSavedServerList();
            savedServers.remove(server.host(), server.port());
            savedServers.save();
            openMultiplayerScreen();
        } catch (Exception exception) {
            openMessageScreen("Server List Failed", exception.getMessage());
        }
    }

    private void connectToMultiplayerServer(String host, int port, String serverName, boolean rememberSavedEntry) {
        MultiplayerClient client = null;
        try {
            unloadWorld(true);
            client = new MultiplayerClient();
            client.addListener(this::handleNetworkMessage);
            client.connect(host, port, localPlayerName());
            if (!client.awaitHello(3000L)) {
                throw new IllegalStateException("Timed out waiting for CraftZero host handshake.");
            }
            multiplayerClient = client;
            startMultiplayerClientWorld(client);
            rememberMultiplayerServer(host, port, serverName, rememberSavedEntry);
            addChatMessage("Connected to " + (serverName == null || serverName.isBlank() ? host : serverName) + ".");
        } catch (Exception exception) {
            if (client != null) {
                client.close();
            }
            openMessageScreen("Connection Failed", exception.getMessage());
        }
    }

    private void rememberMultiplayerServer(String host, int port, String serverName, boolean rememberSavedEntry) {
        settings.setLastServer(formatServerAddress(host, port));
        saveSettings();
        if (!rememberSavedEntry) {
            return;
        }
        try {
            SavedServerList savedServers = loadSavedServerList();
            String name = serverName == null || serverName.isBlank() ? host : serverName;
            savedServers.addOrUpdate(new SavedServer(name, host, port, Instant.now().toEpochMilli()));
            savedServers.save();
        } catch (Exception exception) {
            System.err.println("Failed to update saved server list: " + exception.getMessage());
        }
    }

    private SavedServerList loadSavedServerList() throws java.io.IOException {
        return SavedServerList.load(SavedServerList.defaultPath(Paths.get(".")));
    }

    private void startSavedServerStatusPings(List<SavedServer> servers, Map<SavedServer, ServerPingStatus> statuses) {
        if (servers == null || servers.isEmpty() || statuses == null) {
            return;
        }
        for (SavedServer server : servers) {
            statuses.put(server, ServerPingStatus.pinging());
            Thread thread = new Thread(() -> statuses.put(server, pingSavedServer(server)),
                    "CraftZero-ServerListPing-" + server.host() + "-" + server.port());
            thread.setDaemon(true);
            thread.start();
        }
    }

    private static ServerPingStatus pingSavedServer(SavedServer server) {
        if (server == null) {
            return ServerPingStatus.offline();
        }
        ServerPingStatus extended = pingSavedServer(server, true);
        return extended.onlineStatus() ? extended : pingSavedServer(server, false);
    }

    private static ServerPingStatus pingSavedServer(SavedServer server, boolean extended) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(server.host(), server.port()), 750);
            socket.setSoTimeout(1000);
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            output.writeByte(0xFE);
            if (extended) {
                output.writeByte(0x01);
            }
            output.flush();

            DataInputStream input = new DataInputStream(socket.getInputStream());
            int packetId = input.readUnsignedByte();
            if (packetId != 0xFF) {
                return ServerPingStatus.offline();
            }
            int length = input.readUnsignedShort();
            if (length <= 0 || length > 1024) {
                return ServerPingStatus.offline();
            }
            byte[] payload = input.readNBytes(length * 2);
            String response = new String(payload, StandardCharsets.UTF_16BE);
            LegacyServerStatus status = LegacyServerStatus.parse(response);
            return new ServerPingStatus(status.motd(), status.onlinePlayers(),
                    Math.max(1, status.maxPlayers()), true, false);
        } catch (Exception ignored) {
            return ServerPingStatus.offline();
        }
    }

    private static String savedServerLabel(SavedServer server, ServerPingStatus status, int width) {
        if (server == null) {
            return "";
        }
        String statusText = status == null ? "pinging..." : status.label();
        return fitMenuRow(server.name() + " (" + formatServerAddress(server) + ") - " + statusText, width);
    }

    private static String fitMenuRow(String text, int width) {
        int maxChars = Math.max(4, width / 6);
        if (text == null || text.length() <= maxChars) {
            return text == null ? "" : text;
        }
        return text.substring(0, Math.max(1, maxChars - 3)) + "...";
    }

    private record ServerPingStatus(String motd, int online, int max, boolean onlineStatus, boolean pending) {
        static ServerPingStatus pinging() {
            return new ServerPingStatus("", 0, 0, false, true);
        }

        static ServerPingStatus offline() {
            return new ServerPingStatus("", 0, 0, false, false);
        }

        String label() {
            if (pending) {
                return "pinging...";
            }
            if (!onlineStatus) {
                return "offline";
            }
            String prefix = motd == null || motd.isBlank() ? "online" : motd;
            return prefix + " " + online + "/" + max;
        }
    }

    private static String formatServerAddress(SavedServer server) {
        return server == null ? "" : formatServerAddress(server.host(), server.port());
    }

    private static String formatServerAddress(ParsedServerAddress address) {
        return address == null ? formatServerAddress("127.0.0.1", MultiplayerServer.DEFAULT_PORT)
                : formatServerAddress(address.host(), address.port());
    }

    private static String formatServerAddress(String host, int port) {
        return (host == null || host.isBlank() ? "127.0.0.1" : host.trim()) + ":" + port;
    }

    private ParsedServerAddress parseServerAddressOrDefault(String value) {
        try {
            return parseServerAddress(value);
        } catch (IllegalArgumentException ignored) {
            return new ParsedServerAddress("127.0.0.1", MultiplayerServer.DEFAULT_PORT);
        }
    }

    private ParsedServerAddress parseDirectServerAddress(String hostValue, String portValue) {
        ParsedServerAddress address = parseServerAddress(hostValue);
        if (portValue == null || portValue.isBlank()) {
            return address;
        }
        return new ParsedServerAddress(address.host(), parseServerPort(portValue));
    }

    private static ParsedServerAddress parseServerAddress(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isBlank()) {
            throw new IllegalArgumentException("Server address is empty.");
        }
        String host = text;
        int port = MultiplayerServer.DEFAULT_PORT;
        int separator = text.lastIndexOf(':');
        if (separator > 0 && separator < text.length() - 1) {
            host = text.substring(0, separator).trim();
            port = parseServerPort(text.substring(separator + 1));
        }
        if (host.isBlank()) {
            throw new IllegalArgumentException("Server host is empty.");
        }
        return new ParsedServerAddress(host, port);
    }

    private static int parseServerPort(String value) {
        try {
            int port = Integer.parseInt(value == null ? "" : value.trim());
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("Server port must be between 1 and 65535.");
            }
            return port;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Server port must be a number.", exception);
        }
    }

    private record ParsedServerAddress(String host, int port) {
    }

    private void openMessageScreen(String title, String message) {
        BaseMenuScreen screen = MenuScreens.message(title, message, guiWidth(), guiHeight(), this::openTitleScreen);
        screen.add(new MenuButton(message == null ? "" : message, guiWidth() / 2 - 180,
                guiHeight() / 2 - 10, 360, 20, () -> {
                }).enabled(false));
        screenManager.show(screen);
    }

    private void openEndCreditsScreen() {
        if (player == null || world == null) {
            openTitleScreen();
            return;
        }
        paused = true;
        gameState = GameState.END_CREDITS;
        Input.setCursorLocked(false);
        screenManager.show(MenuScreens.endCredits(guiWidth(), guiHeight(), this::resumeGame));
    }

    private void openCreativeInventory() {
        screenManager.show(new CreativeInventoryScreen(player.getInventory(), guiWidth(), guiHeight(), () -> {
            screenManager.clear();
            Input.setCursorLocked(true);
        }, this::isDropBindingPressed));
        Input.setCursorLocked(false);
    }

    private void recordInventoryOpenedAchievement() {
        if (player != null) {
            player.getStats().getAchievements().recordInventoryOpened();
        }
    }

    private void openDeathMenuIfNeeded() {
        if (deathMenuOpen) {
            return;
        }
        deathMenuOpen = true;
        paused = true;
        gameState = GameState.DEATH;
        Input.setCursorLocked(false);
        BaseMenuScreen screen = new BaseMenuScreen(currentHardcore ? "Game over!" : "You Died!", false, false, null);
        screen.add(MenuLabel.centered("score",
                com.craftzero.graphics.DeathScreen.scoreText(player.getStats().getProgression().getScore()),
                guiWidth() / 2, guiHeight() / 2 - 8, 200)
                .color(0.85f, 0.85f, 0.85f, 1.0f));
        if (currentHardcore) {
            screen.add(MenuLabel.centered("hardcore-message", "You cannot respawn in hardcore mode!",
                    guiWidth() / 2, guiHeight() / 2 + 20, 240)
                    .color(1.0f, 1.0f, 1.0f, 1.0f));
            screen.add(new MenuButton("Delete World", guiWidth() / 2 - 100, guiHeight() / 2 + 48, 200, 20,
                    this::deleteHardcoreWorldAndReturnToTitle));
        } else {
            screen.add(new MenuButton("Respawn", guiWidth() / 2 - 100, guiHeight() / 2 + 20, 200, 20,
                    () -> {
                        if (clientMultiplayerWorld && multiplayerClient != null && multiplayerClient.isConnected()) {
                            requestMultiplayerRespawn();
                            return;
                        }
                        RespawnTarget target = preparePlayerRespawn(player, world, worldSpawnX, worldSpawnY, worldSpawnZ);
                        player.respawn();
                        sendMultiplayerPlayerStateNow();
                        syncMultiplayerInventoryStateNow();
                        if (target == RespawnTarget.WORLD_SPAWN) {
                            addChatMessage("Your home bed was missing or obstructed");
                        }
                        deathMenuOpen = false;
                        resumeGame();
                    }));
            screen.add(new MenuButton("Title Menu", guiWidth() / 2 - 100, guiHeight() / 2 + 48, 200, 20,
                    () -> {
                        unloadWorld(true);
                        openTitleScreen();
                    }));
        }
        screenManager.show(screen);
    }

    private void requestMultiplayerRespawn() {
        if (multiplayerRespawnRequestPending || multiplayerClient == null || !multiplayerClient.isConnected()) {
            return;
        }
        sendMultiplayerPlayerStateNow();
        try {
            com.google.gson.JsonObject data = NetworkMessage.object();
            data.addProperty("action", MultiplayerProtocol.ACTION_PLAYER_RESPAWN);
            multiplayerClient.send(NetworkMessage.of("clientAction", data));
            multiplayerRespawnRequestPending = true;
        } catch (Exception e) {
            addChatMessage("Could not request respawn: " + e.getMessage());
        }
    }

    private void deleteHardcoreWorldAndReturnToTitle() {
        WorldInfo toDelete = currentWorldInfo;
        unloadWorld(false);
        if (toDelete != null && worldManager != null) {
            try {
                worldManager.deleteWorld(toDelete.id());
            } catch (Exception e) {
                System.err.println("Failed to delete hardcore world: " + e.getMessage());
            }
        }
        openTitleScreen();
    }

    private void startMultiplayerHost() {
        closeMultiplayer();
        try {
            String serverName = hostedMultiplayerServerName();
            String serverIp = validServerIp(multiplayerServerIp);
            int serverPort = validServerPort(multiplayerServerPort);
            multiplayerServer = new MultiplayerServer(serverIp, serverPort, world.getSeed(),
                    dayCycleManager.getTime(), serverName);
            multiplayerServer.addListener(this::handleNetworkMessage);
            configureMultiplayerWorldMetadata();
            configureMultiplayerAccessControl();
            configureMultiplayerQuery();
            multiplayerServer.start();
            seedMultiplayerHostSnapshot();
            String bindText = serverIp.isBlank() ? "*" : serverIp;
            System.out.println("Hosting " + serverName + " on " + bindText + ":" + multiplayerServer.getPort());
        } catch (Exception e) {
            System.err.println("Failed to host multiplayer world: " + e.getMessage());
        }
    }

    private String hostedMultiplayerServerName() {
        if (loadedLevel != null && loadedLevel.serverMotd != null && !loadedLevel.serverMotd.isBlank()) {
            return loadedLevel.serverMotd.trim();
        }
        if (currentWorldInfo != null && currentWorldInfo.displayName() != null
                && !currentWorldInfo.displayName().isBlank()) {
            return currentWorldInfo.displayName().trim();
        }
        return "CraftZero";
    }

    private void configureMultiplayerWorldMetadata() {
        if (multiplayerServer == null || world == null) {
            return;
        }
        multiplayerServer.configureWorldMetadata(
                worldSpawnX,
                worldSpawnY,
                worldSpawnZ,
                currentGameMode.name(),
                currentDifficulty.name(),
                currentHardcore,
                currentAllowCheats,
                multiplayerPvp,
                multiplayerSpawnAnimals,
                multiplayerSpawnMonsters,
                multiplayerSpawnNpcs,
                multiplayerAllowNether,
                multiplayerAllowFlight,
                world.getDimension().getSaveName(),
                multiplayerMaxPlayers,
                multiplayerViewDistance,
                multiplayerMaxBuildHeight,
                multiplayerGenerateStructures);
    }

    private void configureMultiplayerAccessControl() {
        if (multiplayerServer == null) {
            return;
        }
        multiplayerServer.configureAccessControl(bannedPlayers, bannedIps, whitelist, whitelistEnabled,
                multiplayerOnlineMode);
    }

    private void configureMultiplayerQuery() {
        if (multiplayerServer == null) {
            return;
        }
        multiplayerServer.configureQuery(multiplayerEnableQuery, multiplayerQueryPort);
    }

    private void seedMultiplayerHostSnapshot() {
        if (multiplayerServer == null || world == null) {
            return;
        }
        seedMultiplayerHostPlayerSnapshot();
        seedMultiplayerHostBlockSnapshot();
        seedMultiplayerHostEntitySnapshot();
        seedMultiplayerHostDroppedItemSnapshot();
        seedMultiplayerHostInventorySnapshot();
    }

    private void seedMultiplayerHostPlayerSnapshot() {
        if (player == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        ItemStack held = inventory.getItemInHand();
        String heldItemId = held == null || held.isEmpty() ? "air" : multiplayerItemId(held);
        int heldItemCount = held == null || held.isEmpty() ? 0 : held.getCount();
        int heldItemDamage = held == null || held.isEmpty() ? 0 : held.getDurability();
        multiplayerServer.seedPlayerState(
                "host",
                localPlayerName(),
                player.getPosition().x,
                player.getPosition().y,
                player.getPosition().z,
                player.getCamera().getYaw(),
                player.getCamera().getPitch(),
                player.isOnGround(),
                player.isSneaking(),
                player.getStats().getHealth(),
                heldItemId,
                heldItemCount,
                heldItemDamage,
                inventory.getSelectedSlot(),
                player.getGameMode().name(),
                multiplayerPlayerStateData(player));
    }

    private void seedMultiplayerHostBlockSnapshot() {
        int seeded = 0;
        for (Chunk chunk : world.getLoadedChunks()) {
            if (!shouldSeedMultiplayerChunk(chunk)) {
                continue;
            }
            int baseX = chunk.getWorldX();
            int baseZ = chunk.getWorldZ();
            for (int y = 0; y < Chunk.HEIGHT; y++) {
                for (int z = 0; z < Chunk.DEPTH; z++) {
                    for (int x = 0; x < Chunk.WIDTH; x++) {
                        BlockType type = chunk.getBlock(x, y, z);
                        int metadata = chunk.getBlockMetadata(x, y, z);
                        if (type == BlockType.AIR && metadata == 0) {
                            continue;
                        }
                        multiplayerServer.seedBlockState(baseX + x, y, baseZ + z, type.getId(), metadata,
                                multiplayerBlockData(baseX + x, y, baseZ + z, type));
                        seeded++;
                    }
                }
            }
        }
        if (seeded > 0) {
            System.out.println("Seeded " + seeded + " multiplayer block snapshot entries.");
        }
    }

    private boolean shouldSeedMultiplayerChunk(Chunk chunk) {
        return chunk != null
                && (chunk.isModified() || chunk.isLoadedFromStorage())
                && chunk.getState().ordinal() >= Chunk.ChunkState.GENERATED.ordinal();
    }

    private void seedMultiplayerHostEntitySnapshot() {
        if (world == null) {
            return;
        }
        Set<String> currentIds = new HashSet<>();
        for (Entity entity : world.getEntities()) {
            if (!shouldSyncMultiplayerEntity(entity)) {
                continue;
            }
            String entityId = multiplayerEntityId(entity);
            currentIds.add(entityId);
            multiplayerServer.seedEntityState(
                    entityId,
                    multiplayerEntityType(entity),
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    entity.getYaw(),
                    entity.getPitch(),
                    multiplayerEntityData(entity, false));
        }
        lastMultiplayerEntityIds.clear();
        lastMultiplayerEntityIds.addAll(currentIds);
    }

    private void seedMultiplayerHostDroppedItemSnapshot() {
        if (world == null) {
            return;
        }
        Set<String> currentIds = new HashSet<>();
        for (DroppedItem item : world.getDroppedItems()) {
            if (item == null || item.getCount() <= 0 || item.isDestroyed()) {
                continue;
            }
            String entityId = multiplayerDroppedItemId(item);
            currentIds.add(entityId);
            multiplayerServer.seedEntityState(
                    entityId,
                    "dropped_item",
                    item.getX(),
                    item.getY(),
                    item.getZ(),
                    item.getRotation(),
                    0.0f,
                    multiplayerDroppedItemData(item, false));
        }
        lastMultiplayerDroppedItemIds.clear();
        lastMultiplayerDroppedItemIds.addAll(currentIds);
    }

    private void seedMultiplayerHostInventorySnapshot() {
        if (player == null || player.getInventory() == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < MULTIPLAYER_INVENTORY_SLOTS; slot++) {
            ItemStack stack = multiplayerInventorySlot(inventory, slot);
            rememberMultiplayerInventorySlot(slot, stack);
            seedMultiplayerInventorySlot("host", slot, stack);
        }
    }

    private void seedMultiplayerInventorySlot(String playerId, int slot, ItemStack stack) {
        String itemId = "air";
        int count = 0;
        int damage = 0;
        if (stack != null && !stack.isEmpty() && stack.getType() != null) {
            itemId = multiplayerItemId(stack);
            count = Math.max(0, Math.min(stack.getCount(), MultiplayerProtocol.MAX_STACK_COUNT));
            damage = Math.max(MultiplayerProtocol.MIN_ITEM_DAMAGE,
                    Math.min(stack.getDurability(), MultiplayerProtocol.MAX_ITEM_DAMAGE));
        }
        multiplayerServer.seedInventoryState(playerId, slot, itemId, count, damage, multiplayerInventoryData(stack));
    }

    private void closeMultiplayer() {
        multiplayerSleepCompletePending = false;
        if (multiplayerServer != null) {
            multiplayerServer.close();
            multiplayerServer = null;
        }
        if (multiplayerClient != null) {
            multiplayerClient.close();
            multiplayerClient = null;
        }
        if (world != null) {
            for (Entity entity : remoteEntities.values()) {
                world.removeEntityNow(entity);
            }
            for (DroppedItem item : remoteDroppedItems.values()) {
                world.getDroppedItems().remove(item);
            }
        }
        remotePlayers.clear();
        remoteEntities.clear();
        remoteDroppedItems.clear();
        multiplayerFishingHooks.clear();
        multiplayerVehicleRidersByEntityId.clear();
        multiplayerVehicleEntityByPlayerId.clear();
        multiplayerExperiencePickupCooldowns.clear();
        multiplayerRemoteDamageCooldowns.clear();
        multiplayerEntityIds.clear();
        multiplayerDroppedItemIds.clear();
        lastMultiplayerEntityIds.clear();
        lastMultiplayerDroppedItemIds.clear();
        multiplayerRoster.clear();
        multiplayerEntityTimer = 0.0f;
        multiplayerTileTimer = 0.0f;
        nextMultiplayerEntityId = 1;
        nextMultiplayerDroppedItemId = 1;
    }

    private void handleDimensionTransfers(float deltaTime) {
        if (handleEndPortalTransfer()) {
            netherPortalTime = 0.0f;
            return;
        }
        handleNetherPortalTransfer(deltaTime);
    }

    private boolean handleEndPortalTransfer() {
        if (world == null || player == null || clientMultiplayerWorld) {
            return false;
        }
        org.joml.Vector3f pos = player.getPosition();
        int x = floorBlock(pos.x);
        int y = floorBlock(pos.y + 0.1f);
        int z = floorBlock(pos.z);
        boolean inPortal = world.isEndPortalAt(x, y, z) || world.isEndPortalAt(x, y + 1, z);
        if (!inPortal) {
            return false;
        }
        Dimension previousDimension = world.getDimension();
        DimensionTransferService.TransferTarget target = DimensionTransferService.fromEndPortal(world.getDimension(),
                worldSpawnX, worldSpawnY, worldSpawnZ);
        switchDimension(target);
        if (previousDimension == Dimension.THE_END
                && world != null
                && world.getDimension() == Dimension.OVERWORLD) {
            openEndCreditsScreen();
        }
        return true;
    }

    private void handleNetherPortalTransfer(float deltaTime) {
        if (world == null || player == null || clientMultiplayerWorld || world.getDimension() == Dimension.THE_END) {
            netherPortalTime = 0.0f;
            return;
        }
        if (!multiplayerAllowNether) {
            netherPortalTime = 0.0f;
            return;
        }
        org.joml.Vector3f pos = player.getPosition();
        int x = floorBlock(pos.x);
        int y = floorBlock(pos.y + 0.1f);
        int z = floorBlock(pos.z);
        boolean inPortal = world.isNetherPortalAt(x, y, z) || world.isNetherPortalAt(x, y + 1, z);
        if (!inPortal) {
            netherPortalTime = 0.0f;
            return;
        }
        float requiredPortalTime = netherPortalTransferTimeFor(player.getGameMode());
        netherPortalTime += deltaTime;
        if (netherPortalTime < requiredPortalTime) {
            return;
        }
        netherPortalTime = 0.0f;
        DimensionTransferService.TransferTarget target = DimensionTransferService.fromNetherPortal(
                world.getDimension(), pos.x, pos.y, pos.z);
        switchDimension(target);
    }

    private float netherPortalOverlayStrength() {
        if (world == null || player == null || clientMultiplayerWorld || world.getDimension() == Dimension.THE_END
                || !multiplayerAllowNether) {
            return 0.0f;
        }
        org.joml.Vector3f pos = player.getPosition();
        int x = floorBlock(pos.x);
        int y = floorBlock(pos.y + 0.1f);
        int z = floorBlock(pos.z);
        boolean inPortal = world.isNetherPortalAt(x, y, z) || world.isNetherPortalAt(x, y + 1, z);
        if (!inPortal) {
            return 0.0f;
        }
        float requiredPortalTime = netherPortalTransferTimeFor(player.getGameMode());
        if (requiredPortalTime <= 0.0f) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, netherPortalTime / requiredPortalTime));
    }

    static float netherPortalTransferTimeFor(GameMode gameMode) {
        return gameMode == GameMode.CREATIVE ? 0.0f : NETHER_PORTAL_TRANSFER_TIME;
    }

    private void switchDimension(DimensionTransferService.TransferTarget target) {
        if (target == null || world == null || player == null || dayCycleManager == null) {
            return;
        }
        try {
            Dimension previousDimension = world.getDimension();
            waitForAutosave();
            saveGame("dimension transfer");
            long seed = world.getSeed();
            boolean generateStructures = world.shouldGenerateStructures();
            if (world != null) {
                world.cleanup();
            }
            world = new World(seed, WorldGenerators.generatorIdFor(target.dimension()), target.dimension(),
                    generateStructures);
            multiplayerGenerateStructures = generateStructures;
            world.setSpawnNpcs(multiplayerSpawnNpcs);
            installWorldNetworkHooks(world);
            world.setSaveManager(saveManager);
            world.init();
            world.setWeatherState(weatherState);
            survivalHudRenderer.setAtlas(world.getAtlas());
            inventoryRenderer.setAtlas(world.getAtlas());
            playerRenderer.setTextures(world.getAtlas(), com.craftzero.graphics.GuiTexture.getItemsTexture());
            float targetX = target.x();
            float targetY = target.y();
            float targetZ = target.z();
            if (target.prepareNetherPortal()) {
                BlockPos portalPos = world.ensureNetherPortalAt(target.x(), target.y(), target.z());
                if (portalPos != null) {
                    targetX = portalPos.x() + 0.5f;
                    targetY = portalPos.y();
                    targetZ = portalPos.z() + 0.5f;
                }
            }
            player.placeAfterDimensionTransfer(targetX, targetY, targetZ);
            player.setWorld(world);
            world.setPlayer(player);
            world.setDayCycleManager(dayCycleManager);
            world.setRenderDistanceChunks(effectiveWorldRenderDistanceChunks());
            world.setFancyGraphics(settings.isFancyGraphics());
            world.setSmoothLighting(settings.isSmoothLighting());
            world.setAdvancedOpenGl(settings.isAdvancedOpenGl());
            saveManager.applyDimensionRuntime(world, player, dayCycleManager);
            weatherState = world.getWeatherState();
            if (target.dimension() == Dimension.THE_END) {
                world.ensureEndSpawnPlatform();
            }
            player.getStats().getAchievements().recordDimensionTravel(previousDimension, target.dimension());
            mobSpawner = new MobSpawner(world);
            dimensionTransferCooldown = 2.0f;
            refreshMultiplayerHostWorldSnapshot();
        } catch (Exception e) {
            e.printStackTrace();
            openMessageScreen("Dimension Transfer Failed", e.getMessage());
        }
    }

    private void refreshMultiplayerHostWorldSnapshot() {
        if (multiplayerServer == null || world == null || dayCycleManager == null) {
            return;
        }
        configureMultiplayerWorldMetadata();
        seedMultiplayerHostSnapshot();
        multiplayerServer.broadcastWorldState(dayCycleManager.getTime(), weatherState);
    }

    private void saveSettings() {
        try {
            settings.save(GameSettings.DEFAULT_OPTIONS_PATH);
        } catch (Exception e) {
            System.err.println("Failed to save options.txt: " + e.getMessage());
        }
    }

    private void settingsChangedLive() {
        saveSettings();
        applyRuntimeSettings(false);
    }

    private void cleanup() {
        unloadWorld(true);
        if (saveExecutor != null) {
            saveExecutor.shutdown();
        }
        if (renderer != null) {
            renderer.cleanup();
        }
        if (skyRenderer != null) {
            skyRenderer.cleanup();
        }
        if (cloudRenderer != null) {
            cloudRenderer.cleanup();
        }
        if (chestRenderer != null) {
            chestRenderer.cleanup();
        }
        if (enchantingTableRenderer != null) {
            enchantingTableRenderer.cleanup();
        }
        if (hudRenderer != null) {
            hudRenderer.cleanup();
        }
        if (blockHighlightRenderer != null) {
            blockHighlightRenderer.cleanup();
        }
        if (blockBreakingRenderer != null) {
            blockBreakingRenderer.cleanup();
        }
        if (survivalHudRenderer != null) {
            survivalHudRenderer.cleanup();
        }
        if (mobRenderer != null) {
            mobRenderer.cleanup();
        }
        if (mobSpawnerRenderer != null) {
            mobSpawnerRenderer.cleanup();
        }
        if (arrowRenderer != null) {
            arrowRenderer.cleanup();
        }
        if (fallingBlockRenderer != null) {
            fallingBlockRenderer.cleanup();
        }
        if (movingPistonRenderer != null) {
            movingPistonRenderer.cleanup();
        }
        if (lightningRenderer != null) {
            lightningRenderer.cleanup();
        }
        if (particleRenderer != null) {
            particleRenderer.cleanup();
        }
        if (precipitationRenderer != null) {
            precipitationRenderer.cleanup();
        }
        if (signTextRenderer != null) {
            signTextRenderer.cleanup();
        }
        if (playerRenderer != null) {
            playerRenderer.cleanup();
        }
        if (inventoryPlayerRenderer != null) {
            inventoryPlayerRenderer.cleanup();
        }
        if (deathScreen != null) {
            deathScreen.cleanup();
        }
        if (menuRenderer != null) {
            menuRenderer.cleanup();
        }
        if (enchantmentTextRenderer != null) {
            enchantmentTextRenderer.cleanup();
        }
        if (textRenderer != null) {
            textRenderer.cleanup();
        }
        if (soundSink != null) {
            soundSink.close();
            soundSink = null;
        }
        com.craftzero.graphics.GuiTexture.cleanup();
        if (window != null) {
            window.cleanup();
        }
        System.out.println("CraftZero shut down successfully.");
    }

    private void saveGame(String reason) {
        if (saveManager == null || world == null || player == null || dayCycleManager == null) {
            return;
        }
        try {
            configureSaveMetadata();
            player.getStats().getStatistics().recordWorldSaved();
            saveManager.save(world, player, dayCycleManager);
            System.out.println("Saved world (" + reason + ").");
        } catch (Exception e) {
            System.err.println("Failed to save world (" + reason + "): " + e.getMessage());
        }
    }

    private void saveGameAsync(String reason) {
        if (saveManager == null || world == null || player == null || dayCycleManager == null || saveExecutor == null) {
            return;
        }
        if (autosaveFuture != null && !autosaveFuture.isDone()) {
            return;
        }
        configureSaveMetadata();
        player.getStats().getStatistics().recordWorldSaved();
        SaveManager.LevelData levelData = saveManager.createLevelDataSnapshot(world, player, dayCycleManager);
        List<Chunk> loadedChunks = List.copyOf(world.getLoadedChunks());
        World savedWorld = world;
        autosaveFuture = saveExecutor.submit(() -> {
            try {
                SaveManager.SaveSnapshot snapshot = saveManager.createSnapshot(levelData, loadedChunks);
                saveManager.writeSnapshot(snapshot);
                saveManager.clearSnapshotModifiedFlags(savedWorld, snapshot);
                System.out.println("Saved world (" + reason + ").");
            } catch (Exception e) {
                System.err.println("Failed to save world (" + reason + "): " + e.getMessage());
            }
        });
    }

    private void configureSaveMetadata() {
        if (saveManager == null) {
            return;
        }
        saveManager.setLevelMetadata(currentWorldInfo != null ? currentWorldInfo.displayName() : "Default World",
                currentGameMode, currentDifficulty, currentHardcore, currentAllowCheats);
        saveManager.setWorldGenerationMetadata(loadedLevel == null ? "" : loadedLevel.getServerLevelSeed(),
                world == null ? multiplayerGenerateStructures : world.shouldGenerateStructures());
        saveManager.setServerQueryProperties(multiplayerEnableQuery, multiplayerQueryPort);
        saveManager.setServerProperties(hostedMultiplayerServerName(), multiplayerServerIp, multiplayerServerPort,
                multiplayerMaxPlayers, multiplayerPvp, multiplayerSpawnAnimals, multiplayerSpawnMonsters,
                multiplayerSpawnNpcs, multiplayerAllowNether, multiplayerOnlineMode, multiplayerAllowFlight,
                multiplayerSpawnProtection, multiplayerViewDistance, multiplayerMaxBuildHeight);
        saveManager.setWorldStateMetadata(worldSpawnX, worldSpawnY, worldSpawnZ, weatherState);
        saveManager.setAdminState(operators, bannedPlayers, bannedIps, whitelist, whitelistEnabled);
    }

    private void restoreServerProperties(SaveManager.LevelData data) {
        multiplayerServerIp = "";
        multiplayerServerPort = MultiplayerProtocol.DEFAULT_PORT;
        multiplayerMaxPlayers = MultiplayerProtocol.DEFAULT_MAX_PLAYERS;
        multiplayerPvp = true;
        multiplayerSpawnAnimals = true;
        multiplayerSpawnMonsters = true;
        multiplayerSpawnNpcs = true;
        multiplayerAllowNether = true;
        multiplayerOnlineMode = false;
        multiplayerAllowFlight = false;
        multiplayerEnableQuery = false;
        multiplayerQueryPort = MultiplayerProtocol.DEFAULT_QUERY_PORT;
        multiplayerSpawnProtection = DEFAULT_SERVER_SPAWN_PROTECTION;
        multiplayerViewDistance = DEFAULT_SERVER_VIEW_DISTANCE;
        multiplayerMaxBuildHeight = MultiplayerProtocol.DEFAULT_MAX_BUILD_HEIGHT;
        multiplayerGenerateStructures = MultiplayerProtocol.DEFAULT_GENERATE_STRUCTURES;
        if (data == null) {
            return;
        }
        multiplayerServerIp = data.getServerIp();
        multiplayerServerPort = data.getServerPort();
        multiplayerMaxPlayers = Math.max(1, data.serverMaxPlayers);
        multiplayerPvp = data.isServerPvp();
        multiplayerSpawnAnimals = data.isServerSpawnAnimals();
        multiplayerSpawnMonsters = data.isServerSpawnMonsters();
        multiplayerSpawnNpcs = data.isServerSpawnNpcs();
        multiplayerAllowNether = data.isServerAllowNether();
        multiplayerOnlineMode = data.isServerOnlineMode();
        multiplayerAllowFlight = data.isServerAllowFlight();
        multiplayerEnableQuery = data.isServerEnableQuery();
        multiplayerQueryPort = data.getServerQueryPort();
        multiplayerSpawnProtection = data.getServerSpawnProtection();
        multiplayerViewDistance = data.getServerViewDistance();
        multiplayerMaxBuildHeight = data.getServerMaxBuildHeight();
        multiplayerGenerateStructures = data.shouldGenerateStructures();
    }

    private void configureMobSpawnerRules() {
        if (mobSpawner != null) {
            mobSpawner.configureServerSpawnRules(multiplayerSpawnAnimals, multiplayerSpawnMonsters);
        }
    }

    private static int validServerPort(int port) {
        return port <= 0 || port > 65535 ? MultiplayerProtocol.DEFAULT_PORT : port;
    }

    private static int clampServerMaxBuildHeight(int height) {
        return Math.max(MultiplayerProtocol.MIN_MAX_BUILD_HEIGHT,
                Math.min(MultiplayerProtocol.WORLD_HEIGHT, height));
    }

    private static String validServerIp(String value) {
        return value == null ? "" : value.replace('\r', ' ').replace('\n', ' ').trim();
    }

    private void restoreAdminState(SaveManager.LevelData data) {
        operators.clear();
        bannedPlayers.clear();
        bannedIps.clear();
        whitelist.clear();
        whitelistEnabled = false;
        if (data == null) {
            return;
        }
        addAllNormalized(operators, data.operators);
        addAllNormalized(bannedPlayers, data.bannedPlayers);
        addAllNormalized(bannedIps, data.bannedIps);
        addAllNormalized(whitelist, data.whitelist);
        whitelistEnabled = data.whitelistEnabled;
    }

    private static void addAllNormalized(java.util.Set<String> target, List<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                target.add(value.trim().toLowerCase(java.util.Locale.ROOT));
            }
        }
    }

    private void waitForAutosave() {
        if (autosaveFuture == null || autosaveFuture.isDone()) {
            return;
        }
        try {
            autosaveFuture.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Timed out waiting for autosave: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            init();
            running = true;
            gameLoop();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    public static void main(String[] args) {
        new Main().run();
    }
}

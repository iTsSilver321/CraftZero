package com.craftzero.main;

import com.craftzero.command.CommandDispatcher;
import com.craftzero.engine.Input;
import com.craftzero.engine.Timer;
import com.craftzero.engine.Window;
import com.craftzero.graphics.ArrowRenderer;
import com.craftzero.graphics.BlockBreakingRenderer;
import com.craftzero.graphics.BlockHighlightRenderer;
import com.craftzero.graphics.ChestRenderer;
import com.craftzero.graphics.CloudRenderer;
import com.craftzero.graphics.DroppedItemRenderer;
import com.craftzero.graphics.FallingBlockRenderer;
import com.craftzero.graphics.HudRenderer;
import com.craftzero.graphics.InventoryPlayerRenderer;
import com.craftzero.graphics.InventoryRenderer;
import com.craftzero.graphics.MobRenderer;
import com.craftzero.graphics.Renderer;
import com.craftzero.graphics.SkyRenderer;
import com.craftzero.graphics.SurvivalHudRenderer;
import com.craftzero.graphics.TextRenderer;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.multiplayer.MultiplayerClient;
import com.craftzero.multiplayer.NetworkMessage;
import com.craftzero.multiplayer.MultiplayerServer;
import com.craftzero.resources.ResourcePackManager;
import com.craftzero.save.SaveManager;
import com.craftzero.save.WorldManager;
import com.craftzero.save.WorldManager.WorldInfo;
import com.craftzero.ui.ChestScreen;
import com.craftzero.ui.BrewingStandScreen;
import com.craftzero.ui.ChatOverlay;
import com.craftzero.ui.CraftingTableScreen;
import com.craftzero.ui.EnchantingTableScreen;
import com.craftzero.ui.FurnaceScreen;
import com.craftzero.ui.InventoryScreen;
import com.craftzero.ui.SignEditScreen;
import com.craftzero.ui.menu.BaseMenuScreen;
import com.craftzero.ui.menu.CreativeInventoryScreen;
import com.craftzero.ui.menu.GuiScale;
import com.craftzero.ui.menu.MenuButton;
import com.craftzero.ui.menu.MenuInput;
import com.craftzero.ui.menu.MenuRenderer;
import com.craftzero.ui.menu.MenuScreens;
import com.craftzero.ui.menu.Screen;
import com.craftzero.ui.menu.ScreenManager;
import com.craftzero.ui.menu.TextField;
import com.craftzero.world.DayCycleManager;
import com.craftzero.world.Dimension;
import com.craftzero.world.DimensionTransferService;
import com.craftzero.world.MobSpawner;
import com.craftzero.world.BlockType;
import com.craftzero.world.Chunk;
import com.craftzero.world.World;
import com.craftzero.world.WorldGenerator;
import com.craftzero.entity.mob.EnderDragon;
import com.craftzero.world.StructureGenerator;
import com.craftzero.world.StructureType;
import com.craftzero.world.tile.ChestTileEntity;
import com.craftzero.world.tile.BrewingStandTileEntity;
import com.craftzero.world.tile.FurnaceTileEntity;
import com.craftzero.world.tile.SignTileEntity;

import java.nio.file.Paths;
import java.util.List;
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

    private static final int TARGET_UPS = 60;
    private static final float FIXED_DELTA = 1.0f / TARGET_UPS;
    private static final float AUTOSAVE_INTERVAL = 60.0f;
    private static final int INITIAL_LOAD_READY_RADIUS = 2;
    private static final boolean VSYNC = false;

    private Window window;
    private Timer timer;
    private Renderer renderer;
    private SkyRenderer skyRenderer;
    private CloudRenderer cloudRenderer;
    private ChestRenderer chestRenderer;
    private DayCycleManager dayCycleManager;
    private HudRenderer hudRenderer;
    private SurvivalHudRenderer survivalHudRenderer;
    private BlockHighlightRenderer blockHighlightRenderer;
    private BlockBreakingRenderer blockBreakingRenderer;
    private InventoryScreen inventoryScreen;
    private ChestScreen chestScreen;
    private FurnaceScreen furnaceScreen;
    private BrewingStandScreen brewingStandScreen;
    private EnchantingTableScreen enchantingTableScreen;
    private SignEditScreen signEditScreen;
    private InventoryRenderer inventoryRenderer;
    private InventoryPlayerRenderer inventoryPlayerRenderer;
    private DroppedItemRenderer droppedItemRenderer;
    private MobRenderer mobRenderer;
    private ArrowRenderer arrowRenderer;
    private FallingBlockRenderer fallingBlockRenderer;
    private com.craftzero.graphics.PlayerRenderer playerRenderer;
    private CraftingTableScreen craftingTableScreen;
    private com.craftzero.graphics.DeathScreen deathScreen;
    private TextRenderer textRenderer;
    private MenuRenderer menuRenderer;
    private ScreenManager screenManager;
    private ChatOverlay chatOverlay;
    private CommandDispatcher commandDispatcher;
    private final ConcurrentLinkedQueue<String> pendingChatMessages = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<NetworkMessage> pendingNetworkMessages = new ConcurrentLinkedQueue<>();

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
    private WorldManager worldManager;
    private ResourcePackManager resourcePackManager;
    private MultiplayerServer multiplayerServer;
    private MultiplayerClient multiplayerClient;
    private boolean clientMultiplayerWorld;
    private float multiplayerStateTimer;

    private float autosaveTimer;
    private ExecutorService saveExecutor;
    private Future<?> autosaveFuture;
    private boolean savingEnabled = true;
    private boolean currentAllowCheats;
    private String weatherState = "clear";
    private int worldSpawnX;
    private int worldSpawnY = 80;
    private int worldSpawnZ;
    private final java.util.Set<String> operators = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final java.util.Set<String> bannedPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final java.util.Set<String> bannedIps = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final java.util.Set<String> whitelist = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private boolean whitelistEnabled;
    private float dimensionTransferCooldown;
    private boolean hostAfterTerrainLoad;
    private float terrainLoadingTime;
    private World.ChunkAreaProgress terrainLoadProgress = new World.ChunkAreaProgress(1, 0, 0, 0);

    private boolean running;
    private boolean paused;
    private GameState gameState = GameState.TITLE;

    private enum CameraFluid {
        NONE,
        WATER,
        LAVA
    }

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
        worldManager = new WorldManager(WorldManager.DEFAULT_SAVES_ROOT);

        renderer = new Renderer();
        renderer.init();

        textRenderer = new TextRenderer();
        textRenderer.init(window.getWidth(), window.getHeight());

        com.craftzero.graphics.GuiTexture.init();

        skyRenderer = new SkyRenderer();
        skyRenderer.init();
        cloudRenderer = new CloudRenderer();
        cloudRenderer.init();
        chestRenderer = new ChestRenderer();
        chestRenderer.init(
                com.craftzero.graphics.GuiTexture.getChestTexture(),
                com.craftzero.graphics.GuiTexture.getLargeChestTexture());

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
        inventoryRenderer.setGuiTextures(
                com.craftzero.graphics.GuiTexture.getInventoryTexture(),
                com.craftzero.graphics.GuiTexture.getCraftingTexture());
        inventoryRenderer.setCreativeTexture(com.craftzero.graphics.GuiTexture.getAllItemsTexture());
        inventoryRenderer.setContainerTextures(
                com.craftzero.graphics.GuiTexture.getContainerTexture(),
                com.craftzero.graphics.GuiTexture.getFurnaceTexture());
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
        arrowRenderer = new ArrowRenderer(renderer);
        arrowRenderer.init();
        fallingBlockRenderer = new FallingBlockRenderer(renderer);
        fallingBlockRenderer.init();
        playerRenderer = new com.craftzero.graphics.PlayerRenderer(renderer);
        playerRenderer.init();

        deathScreen = new com.craftzero.graphics.DeathScreen();
        deathScreen.init(window);
        deathScreen.setTextRenderer(textRenderer);

        menuRenderer = new MenuRenderer();
        menuRenderer.init(window.getWidth(), window.getHeight(), textRenderer);
        updateMenuScale();
        screenManager = new ScreenManager();
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

            while (accumulator >= FIXED_DELTA) {
                update(FIXED_DELTA);
                accumulator -= FIXED_DELTA;
            }

            float partialTick = accumulator / FIXED_DELTA;
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
            world.setRenderDistanceChunks(renderDistanceChunks(settings.getRenderDistance()));
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
        return Math.max(GameSettings.MIN_RENDER_DISTANCE_CHUNKS,
                Math.min(GameSettings.MAX_RENDER_DISTANCE_CHUNKS, chunks));
    }

    private boolean isBindingPressed(GameSettings.KeyBinding binding) {
        int code = settings.getKeyBinding(binding);
        return code < 0 ? Input.isButtonPressed(mouseButtonFromKeyCode(code)) : Input.isKeyPressed(code);
    }

    private static int mouseButtonFromKeyCode(int keyCode) {
        return Math.max(0, keyCode + 100);
    }

    private static int keyCodeFromMouseButton(int button) {
        return button - 100;
    }

    private void handleInput() {
        MenuInput menuInput = menuInput();

        if (Input.isKeyPressed(GLFW_KEY_F11)) {
            window.toggleFullscreen();
            settings.setFullscreen(window.isFullscreen());
            applyRuntimeSettings(false);
            saveSettings();
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
                return;
            }
        }

        if (player == null || world == null) {
            return;
        }

        if (chatOverlay != null && chatOverlay.isOpen()) {
            chatOverlay.update(menuInput, input -> commandDispatcher.suggestions(input, commandContext()))
                    .ifPresent(this::handleChatSubmit);
            if (!chatOverlay.isOpen() && gameState == GameState.PLAYING) {
                Input.setCursorLocked(true);
            }
            return;
        }

        if (player.isDead()) {
            openDeathMenuIfNeeded();
            Input.setCursorLocked(false);
            return;
        }

        if (isBindingPressed(GameSettings.KeyBinding.TOGGLE_PERSPECTIVE)) {
            player.cycleCameraMode();
        }

        if (isBindingPressed(GameSettings.KeyBinding.INVENTORY) && !craftingTableScreen.isOpen()
                && !chestScreen.isOpen() && !furnaceScreen.isOpen() && !signEditScreen.isOpen()) {
            if (player.isCreative()) {
                openCreativeInventory();
            } else {
                inventoryScreen.toggle(window.getWidth(), window.getHeight());
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
                chatOverlay.open(false);
                Input.setCursorLocked(false);
                return;
            }
            if (isBindingPressed(GameSettings.KeyBinding.COMMAND)) {
                chatOverlay.open(true);
                Input.setCursorLocked(false);
                return;
            }
        }

        updateGameplayScreens();

        if (!paused && gameState == GameState.PLAYING && noGameplayScreenOpen()) {
            player.handleInput(timer.getDeltaTime());
            player.handleBlockInteraction(world, timer.getDeltaTime());
            handlePlayerOpenRequests();
        }
    }

    private boolean closeGameplayScreen() {
        if (craftingTableScreen != null && craftingTableScreen.isOpen()) {
            craftingTableScreen.close();
            return true;
        }
        if (chestScreen != null && chestScreen.isOpen()) {
            chestScreen.close();
            return true;
        }
        if (furnaceScreen != null && furnaceScreen.isOpen()) {
            furnaceScreen.close();
            return true;
        }
        if (signEditScreen != null && signEditScreen.isOpen()) {
            signEditScreen.close();
            Input.setCursorLocked(true);
            return true;
        }
        if (inventoryScreen != null && inventoryScreen.isOpen()) {
            inventoryScreen.close();
            return true;
        }
        return false;
    }

    private boolean noGameplayScreenOpen() {
        return !inventoryScreen.isOpen() && !craftingTableScreen.isOpen()
                && !chestScreen.isOpen() && !furnaceScreen.isOpen() && !signEditScreen.isOpen();
    }

    private void updateGameplayScreens() {
        if (craftingTableScreen.isOpen()) {
            craftingTableScreen.update();
            throwScreenItems(craftingTableScreen.getAndClearItemsToThrow(), 4.0f, 2.0f);
            if (craftingTableScreen.shouldOpenInventoryAfterClose()) {
                inventoryScreen.open(window.getWidth(), window.getHeight());
            }
        }
        if (chestScreen.isOpen()) {
            chestScreen.update();
            throwScreenItems(chestScreen.getAndClearItemsToThrow(), 4.0f, 2.0f);
        }
        if (furnaceScreen.isOpen()) {
            furnaceScreen.update();
            throwScreenItems(furnaceScreen.getAndClearItemsToThrow(), 4.0f, 2.0f);
        }
        if (brewingStandScreen.isOpen()) {
            brewingStandScreen.update();
            throwScreenItems(brewingStandScreen.getAndClearItemsToThrow(), 4.0f, 2.0f);
        }
        if (enchantingTableScreen.isOpen()) {
            enchantingTableScreen.update();
            throwScreenItems(enchantingTableScreen.getAndClearItemsToThrow(), 4.0f, 2.0f);
        }
        if (signEditScreen.isOpen()) {
            signEditScreen.update();
        }
        if (!signEditScreen.isOpen()) {
            inventoryScreen.update();
        }
        com.craftzero.inventory.ItemStack thrown = inventoryScreen.getAndClearItemToThrow();
        if (thrown != null && !thrown.isEmpty()) {
            org.joml.Vector3f forward = player.getCamera().getForward();
            org.joml.Vector3f playerVel = player.getVelocity();
            world.spawnThrownStack(player.getPosition().x + forward.x * 0.5f,
                    player.getPosition().y + 1.5f,
                    player.getPosition().z + forward.z * 0.5f,
                    thrown,
                    forward.x * 6.0f + playerVel.x,
                    3.0f,
                    forward.z * 6.0f + playerVel.z);
        }
    }

    private void handlePlayerOpenRequests() {
        if (player.wantsCraftingTable()) {
            craftingTableScreen.open(window.getWidth(), window.getHeight());
        }
        org.joml.Vector3i chestPos = player.getAndClearChestOpenRequest();
        if (chestPos != null && world.getTileEntity(chestPos.x, chestPos.y, chestPos.z) instanceof ChestTileEntity chest) {
            chestScreen.open(world, chest, window.getWidth(), window.getHeight());
        }
        org.joml.Vector3i furnacePos = player.getAndClearFurnaceOpenRequest();
        if (furnacePos != null
                && world.getTileEntity(furnacePos.x, furnacePos.y, furnacePos.z) instanceof FurnaceTileEntity furnace) {
            furnaceScreen.open(furnace, window.getWidth(), window.getHeight());
        }
        org.joml.Vector3i brewingPos = player.getAndClearBrewingStandOpenRequest();
        if (brewingPos != null
                && world.getTileEntity(brewingPos.x, brewingPos.y, brewingPos.z) instanceof BrewingStandTileEntity brewingStand) {
            brewingStandScreen.open(brewingStand, window.getWidth(), window.getHeight());
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
            player.setSpawnPosition(bedPos.x + 0.5f, bedPos.y + 1.0f, bedPos.z + 0.5f);
            if (dayCycleManager.isNight()) {
                dayCycleManager.skipToMorning();
            }
        }
    }

    private void throwScreenItems(List<com.craftzero.inventory.ItemStack> items, float throwSpeed, float yVelocity) {
        for (com.craftzero.inventory.ItemStack dropped : items) {
            if (dropped != null && !dropped.isEmpty()) {
                org.joml.Vector3f forward = player.getCamera().getForward();
                world.spawnThrownStack(
                        player.getPosition().x + forward.x * 0.5f,
                        player.getPosition().y + 1.5f,
                        player.getPosition().z + forward.z * 0.5f,
                        dropped,
                        forward.x * throwSpeed,
                        yVelocity,
                        forward.z * throwSpeed);
            }
        }
    }

    private void update(float deltaTime) {
        if (world == null || player == null) {
            return;
        }
        drainPendingNetworkMessages();
        if (gameState == GameState.LOADING_WORLD) {
            updateTerrainLoading(deltaTime);
            return;
        }
        Screen screen = screenManager.currentScreen();
        if (paused || gameState != GameState.PLAYING || !window.isFocused()
                || (screen != null && screen.pausesGame())) {
            return;
        }

        player.update(deltaTime, world);
        if (player.isDead()) {
            openDeathMenuIfNeeded();
            return;
        }
        if (dimensionTransferCooldown > 0.0f) {
            dimensionTransferCooldown -= deltaTime;
        } else {
            handleEndPortalTransfer();
        }

        updateFog();
        handleDroppedHotbarItem();

        world.update(player.getCamera());
        world.tickBlockUpdates(deltaTime);
        dayCycleManager.update(deltaTime);
        world.tickTileEntities(deltaTime);
        world.updateDroppedItems(deltaTime);
        if (!clientMultiplayerWorld) {
            world.updateEntities(deltaTime);
            mobSpawner.tick();
        }

        sendMultiplayerPlayerState(deltaTime);

        if (multiplayerServer != null) {
            multiplayerServer.broadcastWorldState(dayCycleManager.getTime());
        }

        autosaveTimer += deltaTime;
        if (savingEnabled && autosaveTimer >= AUTOSAVE_INTERVAL) {
            autosaveTimer = 0.0f;
            saveGameAsync("autosave");
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

    private org.joml.Vector3f renderEnvironmentClearColor(CameraFluid fluid, float waterDepth) {
        if (fluid == CameraFluid.WATER) {
            return waterFogColor(waterDepth);
        }
        if (fluid == CameraFluid.LAVA) {
            return new org.joml.Vector3f(0.55f, 0.16f, 0.02f);
        }
        return dayCycleManager.getSkyColor();
    }

    private org.joml.Vector3f renderEnvironmentFogColor(CameraFluid fluid, float waterDepth) {
        if (fluid == CameraFluid.WATER) {
            return waterFogColor(waterDepth);
        }
        if (fluid == CameraFluid.LAVA) {
            return new org.joml.Vector3f(0.70f, 0.22f, 0.03f);
        }
        return dayCycleManager.getFogColor();
    }

    private org.joml.Vector3f waterFogColor(float depthFactor) {
        float t = Math.max(0.0f, Math.min(1.0f, depthFactor));
        float r = 0.10f * (1.0f - t) + 0.02f * t;
        float g = 0.40f * (1.0f - t) + 0.12f * t;
        float b = 0.82f * (1.0f - t) + 0.42f * t;
        return new org.joml.Vector3f(r, g, b);
    }

    private float renderEnvironmentFogDensity(CameraFluid fluid, float waterDepth) {
        if (fluid == CameraFluid.WATER) {
            return 0.12f + Math.max(0.0f, Math.min(1.0f, waterDepth)) * 0.18f;
        }
        if (fluid == CameraFluid.LAVA) {
            return 0.42f;
        }
        return 0.007f;
    }

    static float[] normalFogRangeForRenderDistance(int renderDistanceChunks) {
        float end = Math.max(32.0f, renderDistanceChunks * (float) Chunk.WIDTH);
        return new float[] { end * 0.70f, end };
    }

    private void applyNormalDistanceFog() {
        int chunks = settings == null ? GameSettings.DEFAULT_RENDER_DISTANCE_CHUNKS
                : renderDistanceChunks(settings.getRenderDistance());
        float[] range = normalFogRangeForRenderDistance(chunks);
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
                world.spawnThrownStack(
                        player.getPosition().x + forward.x,
                        player.getPosition().y + 1.5f,
                        player.getPosition().z + forward.z,
                        dropped,
                        forward.x * 8.0f + playerVel.x,
                        3.0f,
                        forward.z * 8.0f + playerVel.z);
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
            commandDispatcher.execute(message, commandContext());
        } else {
            sendPlayerChat(message);
        }
    }

    private void sendPlayerChat(String text) {
        String sender = localPlayerName();
        if (multiplayerClient != null && multiplayerClient.isConnected()) {
            try {
                multiplayerClient.sendChat(sender, text);
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
        if (multiplayerClient != null && multiplayerClient.isConnected()) {
            try {
                multiplayerClient.sendChat("Server", text);
            } catch (Exception e) {
                addChatMessage("Could not send chat: " + e.getMessage());
            }
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

    private void drainPendingNetworkMessages() {
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
                }
                case "blockUpdate" -> applyNetworkBlockUpdate(message);
                case "disconnect" -> {
                    String reason = message.data().has("reason") ? message.data().get("reason").getAsString()
                            : "Disconnected from server.";
                    addChatMessage(reason);
                    if (clientMultiplayerWorld) {
                        openMessageScreen("Disconnected", reason);
                    }
                }
                default -> {
                }
            }
        }
    }

    private void applyNetworkBlockUpdate(NetworkMessage message) {
        if (world == null || message.data() == null
                || !message.data().has("x") || !message.data().has("y") || !message.data().has("z")
                || !message.data().has("blockId")) {
            return;
        }
        int x = message.data().get("x").getAsInt();
        int y = message.data().get("y").getAsInt();
        int z = message.data().get("z").getAsInt();
        int metadata = message.data().has("metadata") ? message.data().get("metadata").getAsInt() : 0;
        BlockType type = parseNetworkBlockType(message.data().get("blockId").getAsString());
        if (world.getLoadedChunk(Math.floorDiv(x, com.craftzero.world.Chunk.WIDTH),
                Math.floorDiv(z, com.craftzero.world.Chunk.DEPTH)) == null) {
            return;
        }
        if (type != null) {
            world.setBlock(x, y, z, type, metadata);
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

    private void sendMultiplayerPlayerState(float deltaTime) {
        if (multiplayerClient == null || !multiplayerClient.isConnected() || player == null) {
            multiplayerStateTimer = 0.0f;
            return;
        }
        multiplayerStateTimer += deltaTime;
        if (multiplayerStateTimer < 0.1f) {
            return;
        }
        multiplayerStateTimer = 0.0f;
        try {
            multiplayerClient.sendPlayerState(
                    player.getPosition().x,
                    player.getPosition().y,
                    player.getPosition().z,
                    player.getCamera().getYaw(),
                    player.getCamera().getPitch());
        } catch (Exception e) {
            addChatMessage("Could not sync player state: " + e.getMessage());
        }
    }

    private CommandDispatcher.Context commandContext() {
        return new GameCommandContext();
    }

    private final class GameCommandContext implements CommandDispatcher.Context {
        @Override
        public String senderName() {
            return localPlayerName();
        }

        @Override
        public boolean hasPermission(String command) {
            String name = command == null ? "" : command.toLowerCase(java.util.Locale.ROOT);
            return switch (name) {
                case "help", "list", "me", "msg", "seed" -> true;
                case "say", "save-all", "save-on", "save-off", "stop",
                        "ban", "ban-ip", "banlist", "deop", "kick", "op",
                        "pardon", "pardon-ip", "whitelist" -> !clientMultiplayerWorld
                                && (multiplayerServer != null || currentAllowCheats || isLocalOperator());
                default -> !clientMultiplayerWorld && (currentAllowCheats || isLocalOperator());
            };
        }

        @Override
        public List<String> playerNames() {
            List<String> names = new java.util.ArrayList<>();
            names.add(senderName());
            if (multiplayerServer != null) {
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
            currentGameMode = mode == null ? GameMode.SURVIVAL : mode;
            currentHardcore = currentGameMode == GameMode.HARDCORE;
            if (player != null) {
                player.setGameMode(currentGameMode);
                player.setDifficulty(currentHardcore ? Difficulty.HARD : currentDifficulty);
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
            return player == null ? 0.0f : player.getPosition().x;
        }

        @Override
        public float playerY() {
            return player == null ? 0.0f : player.getPosition().y;
        }

        @Override
        public float playerZ() {
            return player == null ? 0.0f : player.getPosition().z;
        }

        @Override
        public void teleport(float x, float y, float z) {
            if (player != null) {
                player.setPosition(x, y, z);
            }
        }

        @Override
        public boolean addItem(ItemStack stack) {
            return player != null && player.addStackToInventory(stack);
        }

        @Override
        public void clearInventory(ItemType filter) {
            if (player == null) {
                return;
            }
            if (filter == null) {
                player.getInventory().clearInventory();
                return;
            }
            clearMatching(player.getInventory().getHotbar(), filter);
            clearMatching(player.getInventory().getMainInventory(), filter);
            clearMatching(player.getInventory().getCraftingGrid(), filter);
            clearMatching(player.getInventory().getArmor(), filter);
            ItemStack cursor = player.getInventory().getCursorItem();
            if (cursor != null && cursor.getType() == filter) {
                player.getInventory().setCursorItem(null);
            }
        }

        @Override
        public void kill() {
            if (player != null) {
                PlayerStats stats = player.getStats();
                stats.restore(0.0f, stats.getHunger(), stats.getSaturation(), stats.getCurrentAir());
            }
        }

        @Override
        public void setSpawn(float x, float y, float z) {
            if (player != null) {
                player.setSpawnPosition(x, y, z);
            }
        }

        @Override
        public void setWorldSpawn(int x, int y, int z) {
            worldSpawnX = x;
            worldSpawnY = y;
            worldSpawnZ = z;
            if (player != null) {
                player.setSpawnPosition(x + 0.5f, y, z + 0.5f);
            }
        }

        @Override
        public void addExperience(int amount) {
            if (player != null) {
                player.getStats().getProgression().addExperience(amount);
            }
        }

        @Override
        public void setWeather(String weather) {
            weatherState = weather == null || weather.isBlank() ? "clear" : weather;
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
            addChatMessage(message);
        }

        @Override
        public void broadcast(String message) {
            broadcastSystemChat(message);
        }

        @Override
        public String runServerAdminCommand(String command, List<String> args) {
            return switch (command) {
                case "op" -> requirePlayerArg(command, args, playerName -> {
                    operators.add(playerName.toLowerCase(java.util.Locale.ROOT));
                    return "Opped " + playerName + ".";
                });
                case "deop" -> requirePlayerArg(command, args, playerName -> {
                    operators.remove(playerName.toLowerCase(java.util.Locale.ROOT));
                    return "De-opped " + playerName + ".";
                });
                case "ban" -> requirePlayerArg(command, args, playerName -> {
                    bannedPlayers.add(playerName.toLowerCase(java.util.Locale.ROOT));
                    kickNamedPlayer(playerName, args.size() > 1 ? String.join(" ", args.subList(1, args.size())) : "Banned");
                    return "Banned player " + playerName + ".";
                });
                case "pardon" -> requirePlayerArg(command, args, playerName -> {
                    bannedPlayers.remove(playerName.toLowerCase(java.util.Locale.ROOT));
                    return "Pardoned player " + playerName + ".";
                });
                case "ban-ip" -> requirePlayerArg(command, args, ip -> {
                    bannedIps.add(ip.toLowerCase(java.util.Locale.ROOT));
                    return "Banned IP " + ip + ".";
                });
                case "pardon-ip" -> requirePlayerArg(command, args, ip -> {
                    bannedIps.remove(ip.toLowerCase(java.util.Locale.ROOT));
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
                return "Whitelist enabled.";
            }
            if ("off".equals(mode)) {
                whitelistEnabled = false;
                return "Whitelist disabled.";
            }
            if ("add".equals(mode) || "remove".equals(mode)) {
                if (args.size() < 2) {
                    return "Usage: /whitelist " + mode + " <player>";
                }
                String playerName = args.get(1).toLowerCase(java.util.Locale.ROOT);
                if ("add".equals(mode)) {
                    whitelist.add(playerName);
                    return "Added " + args.get(1) + " to the whitelist.";
                }
                whitelist.remove(playerName);
                return "Removed " + args.get(1) + " from the whitelist.";
            }
            if ("reload".equals(mode)) {
                return "Whitelist reloaded.";
            }
            return "Usage: /whitelist <on|off|list|add|remove|reload>";
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
            for (java.util.Map.Entry<Integer, com.google.gson.JsonObject> entry : multiplayerServer.playerStates().entrySet()) {
                com.google.gson.JsonObject data = entry.getValue();
                if (data != null && data.has("username")
                        && normalized.equals(data.get("username").getAsString().toLowerCase(java.util.Locale.ROOT))) {
                    return multiplayerServer.disconnectClient(entry.getKey(), reason);
                }
            }
            if (normalized.startsWith("player")) {
                try {
                    int clientId = Integer.parseInt(normalized.substring("player".length()));
                    return multiplayerServer.disconnectClient(clientId, reason);
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
            return false;
        }

        private boolean isLocalOperator() {
            return operators.contains(senderName().toLowerCase(java.util.Locale.ROOT));
        }

        private void clearMatching(ItemStack[] slots, ItemType filter) {
            for (int i = 0; i < slots.length; i++) {
                if (slots[i] != null && slots[i].getType() == filter) {
                    slots[i] = null;
                }
            }
        }
    }

    private void render(float deltaTime, float partialTick) {
        handleResize();

        if (world == null || player == null) {
            renderer.setClearColor(0.08f, 0.10f, 0.12f, 1.0f);
            renderer.clear();
            screenManager.render(menuRenderer, menuInput(), deltaTime);
            restoreWorldGlState();
            return;
        }
        drainPendingChatMessages();

        if (gameState == GameState.LOADING_WORLD) {
            renderer.setClearColor(0.08f, 0.10f, 0.12f, 1.0f);
            renderer.clear();
            renderTerrainLoadingScreen(deltaTime);
            restoreWorldGlState();
            return;
        }

        player.setInterpolatedCameraPosition(partialTick);
        CameraFluid cameraFluid = cameraFluid();
        float waterDepth = cameraFluid == CameraFluid.WATER ? cameraWaterDepthFactor() : 0.0f;
        org.joml.Vector3f clearColor = renderEnvironmentClearColor(cameraFluid, waterDepth);
        renderer.setClearColor(clearColor.x, clearColor.y, clearColor.z, 1.0f);
        renderer.setFogColor(renderEnvironmentFogColor(cameraFluid, waterDepth));
        if (cameraFluid == CameraFluid.NONE) {
            applyNormalDistanceFog();
        } else {
            renderer.setFogDensity(renderEnvironmentFogDensity(cameraFluid, waterDepth));
        }
        float gammaBoost = settings == null ? 0.0f : settings.getGamma() * 0.35f;
        renderer.setAmbientLight(Math.min(1.0f, dayCycleManager.getAmbientIntensity() + gammaBoost));
        renderer.setLightDirection(dayCycleManager.getSunDirection());
        renderer.setSunBrightness(dayCycleManager.getSunBrightness());
        renderer.clear();

        if (cameraFluid == CameraFluid.NONE) {
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
        if (settings.isClouds() && cameraFluid == CameraFluid.NONE) {
            cloudRenderer.render(renderer, dayCycleManager, player.getCamera(), deltaTime);
        }
        droppedItemRenderer.render(player.getCamera(), world.getDroppedItems(), world.getAtlas(),
                com.craftzero.graphics.GuiTexture.getItemsTexture(), dayCycleManager, world);

        renderer.beginRender(player.getCamera());
        mobRenderer.renderAll(world.getEntities(), player.getCamera(), partialTick, world.getAtlas());
        fallingBlockRenderer.renderAll(world.getEntities(), player.getCamera(), world.getAtlas(), partialTick);
        arrowRenderer.renderAll(world.getEntities(), player.getCamera(),
                com.craftzero.graphics.GuiTexture.getItemsTexture(), partialTick);
        playerRenderer.render(player, player.getCamera(), partialTick, player.getCameraMode());
        renderer.endRender();

        if (!player.isDead() && gameState == GameState.PLAYING && screenManager.currentScreen() == null) {
            hudRenderer.render(window);
        }
        if (!player.isCreative()) {
            survivalHudRenderer.render(player.getStats(), player.getInventory(), deltaTime);
        } else {
            survivalHudRenderer.renderHotbarOnly(player.getInventory(), deltaTime);
        }
        renderBossHud();
        inventoryRenderer.render(inventoryScreen);
        inventoryRenderer.renderCraftingTable(craftingTableScreen);
        inventoryRenderer.renderChest(chestScreen);
        inventoryRenderer.renderFurnace(furnaceScreen);
        inventoryRenderer.renderBrewingStand(brewingStandScreen);
        inventoryRenderer.renderEnchantingTable(enchantingTableScreen);
        inventoryRenderer.renderSignEditor(signEditScreen);

        if (player.isDead()) {
            deathScreen.render(player.getDeathTime(), (float) Input.getMouseX(), (float) Input.getMouseY());
        }
        if (chatOverlay != null) {
            chatOverlay.render(menuRenderer, guiWidth(), guiHeight(), deltaTime);
        }
        screenManager.render(menuRenderer, menuInput(), deltaTime);
        if (screenManager.currentScreen() instanceof CreativeInventoryScreen creativeInventoryScreen) {
            inventoryRenderer.renderCreative(creativeInventoryScreen, menuRenderer.guiScale());
        }
        restoreWorldGlState();
    }

    private void renderTerrainLoadingScreen(float deltaTime) {
        menuRenderer.renderDirtBackground();
        int width = guiWidth();
        int height = guiHeight();
        int centerX = width / 2;
        int centerY = height / 2;
        float progress = terrainLoadProgress == null ? 0.0f : terrainLoadProgress.progress();
        progress = Math.max(0.0f, Math.min(1.0f, progress));

        menuRenderer.drawCenteredText("Building terrain", centerX, centerY - 34, 1.0f,
                new float[] { 1.0f, 1.0f, 1.0f, 1.0f });
        int barWidth = 182;
        int barHeight = 6;
        int barX = centerX - barWidth / 2;
        int barY = centerY - 12;
        menuRenderer.drawRect(barX - 1, barY - 1, barWidth + 2, barHeight + 2,
                0.18f, 0.18f, 0.18f, 1.0f);
        menuRenderer.drawRect(barX, barY, barWidth, barHeight,
                0.0f, 0.0f, 0.0f, 1.0f);
        menuRenderer.drawRect(barX + 1, barY + 1, Math.max(0.0f, (barWidth - 2) * progress), barHeight - 2,
                0.55f, 0.55f, 0.55f, 1.0f);

        String detail = Math.round(progress * 100.0f) + "%";
        if (terrainLoadProgress != null) {
            detail += "  " + terrainLoadProgress.readyChunks() + "/" + terrainLoadProgress.total();
        }
        menuRenderer.drawCenteredText(detail, centerX, centerY + 6, 0.8f,
                new float[] { 0.75f, 0.75f, 0.75f, 1.0f });
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
            clientMultiplayerWorld = false;
            long seed = loadedLevel != null ? loadedLevel.seed : worldInfo.seed();
            String generatorId = loadedLevel != null ? loadedLevel.generatorId : WorldGenerator.RELEASE_ONE;
            Dimension dimension = loadedLevel != null ? Dimension.fromSaveName(loadedLevel.dimension) : Dimension.OVERWORLD;
            currentGameMode = loadedLevel != null ? loadedLevel.getGameMode() : worldInfo.gameMode();
            currentHardcore = currentGameMode == GameMode.HARDCORE || (loadedLevel != null && loadedLevel.hardcore);
            currentDifficulty = currentHardcore ? Difficulty.HARD
                    : (loadedLevel != null ? loadedLevel.getDifficulty() : worldInfo.difficulty());
            currentAllowCheats = loadedLevel != null && loadedLevel.allowCheats;
            worldSpawnX = loadedLevel != null ? loadedLevel.spawnX : 0;
            worldSpawnY = loadedLevel != null ? loadedLevel.spawnY : 80;
            worldSpawnZ = loadedLevel != null ? loadedLevel.spawnZ : 0;
            weatherState = loadedLevel != null && loadedLevel.weatherState != null ? loadedLevel.weatherState : "clear";
            restoreAdminState(loadedLevel);
            configureSaveMetadata();

            dayCycleManager = new DayCycleManager();
            world = new World(seed, generatorId, dimension);
            world.setSaveManager(saveManager);
            world.init();

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
            world.setRenderDistanceChunks(renderDistanceChunks(settings.getRenderDistance()));
            world.setFancyGraphics(settings.isFancyGraphics());
            world.setSmoothLighting(settings.isSmoothLighting());
            world.setAdvancedOpenGl(settings.isAdvancedOpenGl());
            saveManager.applyLevel(loadedLevel, player, dayCycleManager, world);
            if (loadedLevel == null || loadedLevel.player == null) {
                com.craftzero.world.ReleaseOneWorldGenerator.SpawnPoint spawn = world.findSafeSpawn();
                worldSpawnX = spawn.x();
                worldSpawnY = spawn.y();
                worldSpawnZ = spawn.z();
                player.setPosition(spawn.x() + 0.5f, spawn.y(), spawn.z() + 0.5f);
                player.setSpawnPosition(spawn.x() + 0.5f, spawn.y(), spawn.z() + 0.5f);
            }

            mobSpawner = new MobSpawner(world);
            inventoryScreen = new InventoryScreen(player.getInventory());
            chestScreen = new ChestScreen(player.getInventory());
            furnaceScreen = new FurnaceScreen(player.getInventory());
            brewingStandScreen = new BrewingStandScreen(player.getInventory());
            enchantingTableScreen = new EnchantingTableScreen(player.getInventory());
            signEditScreen = new SignEditScreen();
            craftingTableScreen = new CraftingTableScreen(player.getInventory());

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
        currentGameMode = GameMode.SURVIVAL;
        currentHardcore = false;
        currentDifficulty = settings == null ? Difficulty.EASY : settings.getDifficulty();
        currentAllowCheats = false;
        worldSpawnX = 0;
        worldSpawnY = 80;
        worldSpawnZ = 0;
        weatherState = "clear";
        restoreAdminState(null);

        dayCycleManager = new DayCycleManager();
        dayCycleManager.setTime(client.worldTime());
        world = new World(client.seed(), WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        world.init();

        survivalHudRenderer.setAtlas(world.getAtlas());
        inventoryRenderer.setAtlas(world.getAtlas());
        playerRenderer.setTextures(world.getAtlas(), com.craftzero.graphics.GuiTexture.getItemsTexture());

        player = new Player(0, 80, 0);
        player.setGameMode(currentGameMode);
        player.setDifficulty(currentDifficulty);
        player.applySettings(settings);
        player.getCamera().setFov(settingsFovDegrees());
        world.setPlayer(player);
        player.setWorld(world);
        world.setDayCycleManager(dayCycleManager);
        world.setRenderDistanceChunks(renderDistanceChunks(settings.getRenderDistance()));
        world.setFancyGraphics(settings.isFancyGraphics());
        world.setSmoothLighting(settings.isSmoothLighting());
        world.setAdvancedOpenGl(settings.isAdvancedOpenGl());

        mobSpawner = new MobSpawner(world);
        inventoryScreen = new InventoryScreen(player.getInventory());
        chestScreen = new ChestScreen(player.getInventory());
        furnaceScreen = new FurnaceScreen(player.getInventory());
        brewingStandScreen = new BrewingStandScreen(player.getInventory());
        enchantingTableScreen = new EnchantingTableScreen(player.getInventory());
        signEditScreen = new SignEditScreen();
        craftingTableScreen = new CraftingTableScreen(player.getInventory());

        autosaveTimer = 0.0f;
        savingEnabled = false;
        clientMultiplayerWorld = true;
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
        clientMultiplayerWorld = false;
        multiplayerStateTimer = 0.0f;
        pendingNetworkMessages.clear();
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
                () -> openOptionsScreen(this::resumePauseMenu),
                () -> {
                    unloadWorld(true);
                    openTitleScreen();
                }));
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
                done,
                this::settingsChangedLive);
        screenManager.show(options);
    }

    private void openControlsScreen(Runnable done) {
        BaseMenuScreen screen = new BaseMenuScreen("Controls", true, false, () -> openOptionsScreen(done));
        int cx = guiWidth() / 2;
        int y = 46;
        final String[] waitingAction = { null };
        int index = 0;
        for (GameSettings.KeyBinding binding : settings.getKeyBindings().keySet()) {
            int column = index % 2;
            int row = index / 2;
            int rowY = y + row * 20;
            int x = column == 0 ? cx - 205 : cx + 5;
            screen.add(new MenuButton(binding.displayName() + ": " + keyName(settings.getKeyBinding(binding)),
                    x, rowY, 200, 20,
                    () -> waitingAction[0] = binding.name()));
            index++;
        }
        screen.onTick(() -> {
            if (waitingAction[0] != null && (!Input.getPressedKeys().isEmpty() || !Input.getPressedButtons().isEmpty())) {
                int key = !Input.getPressedButtons().isEmpty()
                        ? keyCodeFromMouseButton(Input.getPressedButtons().get(0))
                        : Input.getPressedKeys().get(0);
                if (key != GLFW_KEY_ESCAPE) {
                    settings.setKeyBinding(GameSettings.KeyBinding.valueOf(waitingAction[0]), key);
                    saveSettings();
                    waitingAction[0] = null;
                    openControlsScreen(done);
                }
            }
        });
        screen.add(new MenuButton("Done", cx - 100, guiHeight() - 24, 200, 20,
                () -> openOptionsScreen(done)));
        screenManager.show(screen);
    }

    private String keyName(int key) {
        if (key < 0) {
            return "Button " + (mouseButtonFromKeyCode(key) + 1);
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
        screenManager.show(MenuScreens.multiplayer(
                guiWidth(),
                guiHeight(),
                this::openDirectConnectScreen,
                () -> openWorldSelectScreen(true),
                this::openTitleScreen));
    }

    private void openDirectConnectScreen() {
        TextField host = new TextField("127.0.0.1", 64, guiWidth() / 2 - 100, 100, 200, 20);
        TextField port = new TextField(Integer.toString(MultiplayerServer.DEFAULT_PORT), 8,
                guiWidth() / 2 - 100, 128, 200, 20);
        Runnable connect = () -> {
            MultiplayerClient client = null;
            try {
                unloadWorld(true);
                client = new MultiplayerClient();
                client.addListener(this::handleNetworkMessage);
                client.connect(host.value(), Integer.parseInt(port.value()), localPlayerName());
                if (!client.awaitHello(3000L)) {
                    throw new IllegalStateException("Timed out waiting for CraftZero host handshake.");
                }
                multiplayerClient = client;
                startMultiplayerClientWorld(client);
                addChatMessage("Connected to CraftZero host.");
            } catch (Exception e) {
                if (client != null) {
                    client.close();
                }
                openMessageScreen("Connection Failed", e.getMessage());
            }
        };
        BaseMenuScreen screen = new BaseMenuScreen("Direct Connect", true, false, this::openMultiplayerScreen);
        screen.add(host.onEnter(connect));
        screen.add(port.onEnter(connect));
        screen.add(new MenuButton("Join Server", guiWidth() / 2 - 100, 164, 200, 20, connect));
        screen.add(new MenuButton("Cancel", guiWidth() / 2 - 100, 192, 200, 20, this::openMultiplayerScreen));
        screenManager.show(screen);
    }

    private void openMessageScreen(String title, String message) {
        BaseMenuScreen screen = MenuScreens.message(title, message, guiWidth(), guiHeight(), this::openTitleScreen);
        screen.add(new MenuButton(message == null ? "" : message, guiWidth() / 2 - 180,
                guiHeight() / 2 - 10, 360, 20, () -> {
                }).enabled(false));
        screenManager.show(screen);
    }

    private void openCreativeInventory() {
        screenManager.show(new CreativeInventoryScreen(player.getInventory(), guiWidth(), guiHeight(), () -> {
            screenManager.clear();
            Input.setCursorLocked(true);
        }));
        Input.setCursorLocked(false);
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
        if (currentHardcore) {
            screen.add(new MenuButton("Delete World", guiWidth() / 2 - 100, guiHeight() / 2 + 20, 200, 20,
                    () -> {
                        WorldInfo toDelete = currentWorldInfo;
                        unloadWorld(false);
                        try {
                            worldManager.deleteWorld(toDelete.id());
                        } catch (Exception e) {
                            System.err.println("Failed to delete hardcore world: " + e.getMessage());
                        }
                        openTitleScreen();
                    }));
        } else {
            screen.add(new MenuButton("Respawn", guiWidth() / 2 - 100, guiHeight() / 2 + 20, 200, 20,
                    () -> {
                        player.respawn();
                        deathMenuOpen = false;
                        resumeGame();
                    }));
        }
        screen.add(new MenuButton("Title Screen", guiWidth() / 2 - 100, guiHeight() / 2 + 48, 200, 20,
                () -> {
                    unloadWorld(!currentHardcore);
                    openTitleScreen();
                }));
        screenManager.show(screen);
    }

    private void startMultiplayerHost() {
        closeMultiplayer();
        try {
            multiplayerServer = new MultiplayerServer(MultiplayerServer.DEFAULT_PORT, world.getSeed(), dayCycleManager.getTime());
            multiplayerServer.addListener(this::handleNetworkMessage);
            multiplayerServer.start();
            System.out.println("Hosting CraftZero world on port " + MultiplayerServer.DEFAULT_PORT);
        } catch (Exception e) {
            System.err.println("Failed to host multiplayer world: " + e.getMessage());
        }
    }

    private void closeMultiplayer() {
        if (multiplayerServer != null) {
            multiplayerServer.close();
            multiplayerServer = null;
        }
        if (multiplayerClient != null) {
            multiplayerClient.close();
            multiplayerClient = null;
        }
    }

    private void handleEndPortalTransfer() {
        if (world == null || player == null || clientMultiplayerWorld) {
            return;
        }
        org.joml.Vector3f pos = player.getPosition();
        int x = floorBlock(pos.x);
        int y = floorBlock(pos.y + 0.1f);
        int z = floorBlock(pos.z);
        boolean inPortal = world.isEndPortalAt(x, y, z) || world.isEndPortalAt(x, y + 1, z);
        if (!inPortal) {
            return;
        }
        DimensionTransferService.TransferTarget target = DimensionTransferService.fromEndPortal(world.getDimension(),
                worldSpawnX, worldSpawnY, worldSpawnZ);
        switchDimension(target);
    }

    private void switchDimension(DimensionTransferService.TransferTarget target) {
        if (target == null || world == null || player == null || dayCycleManager == null) {
            return;
        }
        try {
            waitForAutosave();
            saveGame("dimension transfer");
            long seed = world.getSeed();
            if (world != null) {
                world.cleanup();
            }
            world = new World(seed, WorldGenerator.RELEASE_ONE, target.dimension());
            world.setSaveManager(saveManager);
            world.init();
            survivalHudRenderer.setAtlas(world.getAtlas());
            inventoryRenderer.setAtlas(world.getAtlas());
            playerRenderer.setTextures(world.getAtlas(), com.craftzero.graphics.GuiTexture.getItemsTexture());
            player.setPosition(target.x(), target.y(), target.z());
            player.setWorld(world);
            world.setPlayer(player);
            world.setDayCycleManager(dayCycleManager);
            world.setRenderDistanceChunks(renderDistanceChunks(settings.getRenderDistance()));
            world.setFancyGraphics(settings.isFancyGraphics());
            world.setSmoothLighting(settings.isSmoothLighting());
            world.setAdvancedOpenGl(settings.isAdvancedOpenGl());
            if (target.dimension() == Dimension.THE_END) {
                world.ensureEndSpawnPlatform();
            }
            mobSpawner = new MobSpawner(world);
            dimensionTransferCooldown = 2.0f;
        } catch (Exception e) {
            e.printStackTrace();
            openMessageScreen("Dimension Transfer Failed", e.getMessage());
        }
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
        if (arrowRenderer != null) {
            arrowRenderer.cleanup();
        }
        if (fallingBlockRenderer != null) {
            fallingBlockRenderer.cleanup();
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
        SaveManager.SaveSnapshot snapshot = saveManager.createSnapshot(world, player, dayCycleManager);
        autosaveFuture = saveExecutor.submit(() -> {
            try {
                saveManager.writeSnapshot(snapshot);
                saveManager.clearSnapshotModifiedFlags(world, snapshot);
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
        saveManager.setWorldStateMetadata(worldSpawnX, worldSpawnY, worldSpawnZ, weatherState);
        saveManager.setAdminState(operators, bannedPlayers, bannedIps, whitelist, whitelistEnabled);
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

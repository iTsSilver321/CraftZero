package com.craftzero.main;

import com.craftzero.engine.Input;
import com.craftzero.engine.Timer;
import com.craftzero.engine.Window;
import com.craftzero.graphics.BlockBreakingRenderer;
import com.craftzero.graphics.BlockHighlightRenderer;
import com.craftzero.graphics.DroppedItemRenderer;
import com.craftzero.graphics.HudRenderer;
import com.craftzero.graphics.InventoryRenderer;
import com.craftzero.graphics.Renderer;
import com.craftzero.graphics.SurvivalHudRenderer;
import com.craftzero.graphics.TextRenderer;
import com.craftzero.inventory.ItemStack;
import com.craftzero.ui.CraftingTableScreen;
import com.craftzero.ui.InventoryScreen;
import com.craftzero.world.World;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Main entry point for CraftZero.
 * Implements the game loop with fixed timestep for physics
 * and variable timestep for rendering.
 */
public class Main implements Runnable {

    private static final int TARGET_UPS = 60; // Updates per second
    private static final float FIXED_DELTA = 1.0f / TARGET_UPS;

    private Window window;
    private Timer timer;
    private Renderer renderer;
    private HudRenderer hudRenderer;
    private SurvivalHudRenderer survivalHudRenderer;
    private BlockHighlightRenderer blockHighlightRenderer;
    private BlockBreakingRenderer blockBreakingRenderer;
    private InventoryScreen inventoryScreen;
    private InventoryRenderer inventoryRenderer;
    private DroppedItemRenderer droppedItemRenderer;
    private CraftingTableScreen craftingTableScreen;
    private World world;
    private Player player;

    private TextRenderer textRenderer;

    private boolean running;
    private boolean paused;

    private void init() throws Exception {
        // Create window
        window = new Window("CraftZero - Minecraft Clone", 1280, 720, true);
        window.init();

        // Initialize input
        Input.init(window);

        // Initialize timer
        timer = new Timer();
        timer.init();

        // Initialize renderer
        renderer = new Renderer();
        renderer.init();

        // Initialize text renderer
        textRenderer = new TextRenderer();
        textRenderer.init(window.getWidth(), window.getHeight());

        // Initialize HUD
        hudRenderer = new HudRenderer();
        hudRenderer.init(window);

        // Initialize survival HUD (hearts/hunger)
        survivalHudRenderer = new SurvivalHudRenderer();
        survivalHudRenderer.init(window);
        survivalHudRenderer.setTextRenderer(textRenderer);

        // Initialize block highlight
        blockHighlightRenderer = new BlockHighlightRenderer();
        blockHighlightRenderer.init();

        // Initialize block breaking overlay
        blockBreakingRenderer = new BlockBreakingRenderer();
        blockBreakingRenderer.init();

        // Initialize inventory system
        inventoryRenderer = new InventoryRenderer();
        inventoryRenderer.init(window.getWidth(), window.getHeight());
        inventoryRenderer.setTextRenderer(textRenderer);

        // Initialize dropped item renderer
        droppedItemRenderer = new DroppedItemRenderer();
        droppedItemRenderer.init();

        // Create world with random seed
        long seed = System.currentTimeMillis();
        world = new World(seed);
        world.init();

        // Pass atlas to UI renderers for textured block icons
        survivalHudRenderer.setAtlas(world.getAtlas());
        inventoryRenderer.setAtlas(world.getAtlas());

        // Create player at spawn point (closer to typical terrain height)
        // Player has 5 seconds of spawn invincibility to handle any remaining fall
        player = new Player(0, 80, 0);

        // Create inventory screen (connects to player's inventory)
        inventoryScreen = new InventoryScreen(player.getInventory());

        // Create crafting table screen
        craftingTableScreen = new CraftingTableScreen(player.getInventory());

        // Lock cursor for FPS mode
        Input.setCursorLocked(true);
        paused = false;

        // Update camera aspect ratio
        player.getCamera().setAspectRatio(window.getWidth(), window.getHeight());

        System.out.println("=================================================");
        System.out.println("              CraftZero Initialized              ");
        System.out.println("=================================================");
        System.out.println("Seed: " + seed);
        System.out.println("");
        System.out.println("Controls:");
        System.out.println("  WASD         - Move");
        System.out.println("  Mouse        - Look around");
        System.out.println("  Space        - Jump");
        System.out.println("  Left Ctrl    - Sprint");
        System.out.println("  F            - Toggle fly mode");
        System.out.println("  Left Click   - Break block");
        System.out.println("  Right Click  - Place block");
        System.out.println("  1-9          - Select block type");
        System.out.println("  E            - Open inventory");
        System.out.println("  TAB          - Toggle cursor lock");
        System.out.println("  ESC          - Exit");
        System.out.println("=================================================");
    }

    private void gameLoop() {
        float accumulator = 0.0f;
        float lastFpsUpdate = 0;
        int frameCount = 0;

        while (running && !window.shouldClose()) {
            // 1. Prepare input for this frame
            Input.update(); // Clear 'pressed' states from previous frame
            window.update(); // Poll new events (populates 'pressed' / 'down')

            timer.update();
            float deltaTime = timer.getDeltaTime();
            accumulator += deltaTime;

            // Handle input
            handleInput();

            // Fixed timestep updates (physics)
            while (accumulator >= FIXED_DELTA) {
                update(FIXED_DELTA);
                accumulator -= FIXED_DELTA;
            }

            // Render
            // Render
            render(deltaTime);

            // FPS counter
            frameCount++;
            lastFpsUpdate += deltaTime;
            if (lastFpsUpdate >= 1.0f) {
                String flyMode = player.isFlying() ? " [FLYING]" : "";
                String blockName = "Empty";
                if (player.getInventory().getItemInHand() != null) {
                    blockName = player.getInventory().getItemInHand().getType().name();
                }
                String title = String.format(
                        "CraftZero - FPS: %d | Pos: %.1f, %.1f, %.1f | Block: %s%s",
                        frameCount,
                        player.getPosition().x,
                        player.getPosition().y,
                        player.getPosition().z,
                        blockName,
                        flyMode);
                glfwSetWindowTitle(window.getHandle(), title);
                frameCount = 0;
                lastFpsUpdate = 0;
            }
        }
    }

    private void handleInput() {
        // Toggle inventory with E (only when crafting table not open)
        if (Input.isKeyPressed(GLFW_KEY_E) && !craftingTableScreen.isOpen()) {
            inventoryScreen.toggle(window.getWidth(), window.getHeight());
        }

        // Close inventory/crafting table with ESC (or exit if nothing open)
        if (Input.isKeyPressed(GLFW_KEY_ESCAPE)) {
            if (craftingTableScreen.isOpen()) {
                craftingTableScreen.close();
            } else if (inventoryScreen.isOpen()) {
                inventoryScreen.close();
            } else {
                running = false;
            }
        }

        // Toggle cursor lock with TAB (only when no screens open)
        if (Input.isKeyPressed(GLFW_KEY_TAB) && !inventoryScreen.isOpen() && !craftingTableScreen.isOpen()) {
            paused = !paused;
            Input.setCursorLocked(!paused);
        }

        // Update screens
        if (craftingTableScreen.isOpen()) {
            craftingTableScreen.update();

            // Handle items dropped from crafting table
            java.util.List<com.craftzero.inventory.ItemStack> droppedItems = craftingTableScreen
                    .getAndClearItemsToThrow();
            for (com.craftzero.inventory.ItemStack dropped : droppedItems) {
                if (dropped != null && !dropped.isEmpty()) {
                    org.joml.Vector3f forward = player.getCamera().getForward();
                    float throwSpeed = 4.0f;
                    float throwX = player.getPosition().x + forward.x * 0.5f;
                    float throwY = player.getPosition().y + 1.5f;
                    float throwZ = player.getPosition().z + forward.z * 0.5f;
                    world.spawnThrownItem(throwX, throwY, throwZ, dropped.getType(), dropped.getCount(),
                            forward.x * throwSpeed, 2.0f, forward.z * throwSpeed);
                }
            }

            // Check if should open inventory after closing crafting table (E was pressed)
            if (craftingTableScreen.shouldOpenInventoryAfterClose()) {
                inventoryScreen.open(window.getWidth(), window.getHeight());
            }
        }

        inventoryScreen.update();

        // Handle items thrown from inventory (click outside)
        com.craftzero.inventory.ItemStack thrown = inventoryScreen.getAndClearItemToThrow();
        if (thrown != null && !thrown.isEmpty()) {
            org.joml.Vector3f forward = player.getCamera().getForward();
            float throwSpeed = 6.0f;
            float throwX = player.getPosition().x + forward.x * 0.5f;
            float throwY = player.getPosition().y + 1.5f;
            float throwZ = player.getPosition().z + forward.z * 0.5f;
            world.spawnThrownItem(throwX, throwY, throwZ, thrown.getType(), thrown.getCount(),
                    forward.x * throwSpeed, 3.0f, forward.z * throwSpeed);
        }

        // Player input (only when not paused and no screens open)
        if (!paused && !inventoryScreen.isOpen() && !craftingTableScreen.isOpen()) {
            player.handleInput(timer.getDeltaTime());

            // Check if player wants to open crafting table
            if (player.wantsCraftingTable()) {
                craftingTableScreen.open(window.getWidth(), window.getHeight());
            }
        }
    }

    private void update(float deltaTime) {
        if (paused) {
            return;
        }

        // Update player physics and collision
        player.update(deltaTime, world);

        // Handle block interaction
        player.handleBlockInteraction(world, deltaTime);

        // Handle Q key item drop (only when inventory closed)
        if (!inventoryScreen.isOpen() && player.wantsToDropItem()) {
            com.craftzero.world.BlockType dropped = player.dropOneFromHand();
            if (dropped != null) {
                // Throw item in front of player with forward velocity
                org.joml.Vector3f forward = player.getCamera().getForward();
                float throwSpeed = 6.0f; // Horizontal throw speed
                float throwX = player.getPosition().x + forward.x * 0.5f;
                float throwY = player.getPosition().y + 1.5f;
                float throwZ = player.getPosition().z + forward.z * 0.5f;
                // Velocity: forward * speed + slight upward arc
                world.spawnThrownItem(throwX, throwY, throwZ, dropped, 1,
                        forward.x * throwSpeed, 3.0f, forward.z * throwSpeed);
            }
            player.clearDropFlag();
        }

        // Update world (load/unload chunks around player)
        world.update(player.getCamera());

        // Update dropped items (physics, animation)
        world.updateDroppedItems(deltaTime);
    }

    private void render(float deltaTime) {
        // Handle window resize
        if (window.isResized()) {
            glViewport(0, 0, window.getWidth(), window.getHeight());
            player.getCamera().setAspectRatio(window.getWidth(), window.getHeight());
            hudRenderer.updateOrtho(window.getWidth(), window.getHeight());
            survivalHudRenderer.updateOrtho(window.getWidth(), window.getHeight());
            inventoryRenderer.updateOrtho(window.getWidth(), window.getHeight());
            window.setResized(false);
        }

        // Clear the framebuffer
        renderer.clear();

        // Render world
        world.render(renderer, player.getCamera());

        // Render dropped items
        droppedItemRenderer.render(player.getCamera(), world.getDroppedItems(), world.getAtlas());

        // Render block highlight
        blockHighlightRenderer.render(player.getCamera(), player.getTargetBlock());

        // Render block breaking crack overlay
        if (player.isBreaking()) {
            blockBreakingRenderer.render(
                    player.getCamera(),
                    player.getBreakingBlockPos(),
                    player.getBreakProgress());
        }

        // Render HUD (crosshair)
        hudRenderer.render(window);

        // Render survival HUD (hearts and hunger)
        survivalHudRenderer.render(player.getStats(), player.getInventory(), deltaTime);

        // Render inventory overlay (if open)
        inventoryRenderer.render(inventoryScreen);

        // Render crafting table overlay (if open)
        inventoryRenderer.renderCraftingTable(craftingTableScreen);
    }

    private void cleanup() {
        // Cleanup resources
        if (world != null) {
            world.cleanup();
        }
        if (renderer != null) {
            renderer.cleanup();
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
        window.cleanup();
        System.out.println("CraftZero shut down successfully.");
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

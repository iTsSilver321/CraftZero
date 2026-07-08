package com.craftzero.graphics;

import com.craftzero.engine.Input;
import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.progression.EnchantmentInstance;
import com.craftzero.progression.PotionEffectResolver;
import com.craftzero.main.Player;
import com.craftzero.ui.InventoryScreen;
import com.craftzero.ui.CraftingTableScreen;
import com.craftzero.ui.ChestScreen;
import com.craftzero.ui.BrewingStandScreen;
import com.craftzero.ui.DispenserScreen;
import com.craftzero.ui.EnchantingTableScreen;
import com.craftzero.ui.FurnaceScreen;
import com.craftzero.ui.SignEditScreen;
import com.craftzero.ui.menu.ClassicGuiTexture;
import com.craftzero.ui.menu.CreativeInventoryScreen;
import com.craftzero.ui.menu.UvRegion;
import com.craftzero.world.tile.BrewingStandTileEntity;
import com.craftzero.world.tile.FurnaceTileEntity;
import com.craftzero.world.World;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Renders the Inventory GUI overlay.
 * Uses procedural rendering (no texture atlas for GUI) matching the Hotbar
 * style.
 */
public class InventoryRenderer {

    private ShaderProgram shader;
    private int vao, vbo;
    private int windowWidth, windowHeight;

    // Textured shader for isometric block icons
    private ShaderProgram texturedShader;
    private int texturedVao, texturedVbo;
    private Texture atlas;

    // Slot styling (matches hotbar)
    private static final int SLOT_SIZE = InventoryScreen.SLOT_SIZE;
    private static final int SLOT_SPACING = InventoryScreen.SLOT_SPACING;
    private static final int BORDER = 4;

    // Colors
    private static final float[] BG_COLOR = { 0.75f, 0.75f, 0.75f, 1.0f }; // Window background
    private static final float[] SLOT_BG = { 0.55f, 0.55f, 0.55f, 1.0f }; // Slot inner
    private static final float[] SLOT_BORDER = { 0.75f, 0.75f, 0.75f, 1.0f }; // Slot border (grey)
    private static final float[] HOVER_OVERLAY = { 1.0f, 1.0f, 1.0f, 0.4f }; // White highlight
    private static final int TOOLTIP_BACKGROUND = 0xF0100010;
    private static final int TOOLTIP_BORDER_TOP = 0x505000FF;
    private static final int TOOLTIP_BORDER_BOTTOM = 0x5028007F;
    private static final float TOOLTIP_TEXT_SCALE = 1.0f;
    private static final int TOOLTIP_LINE_HEIGHT = 10;
    private static final int CREATIVE_SCROLL_THUMB_U = 0;
    private static final int CREATIVE_SCROLL_THUMB_V = 208;
    private static final int CREATIVE_SCROLL_THUMB_W = 12;
    private static final int CREATIVE_SCROLL_THUMB_H = 15;
    private static final int HOTBAR_SELECTION_SIZE = 24;
    private static final int HOTBAR_SELECTION_INSET = 3;
    private static final float ITEM_ICON_UV_INSET = 0.5f / 256.0f;
    private static final float BLOCK_ICON_HALF_WIDTH = 0.46f;
    private static final float BLOCK_ICON_TOP_HALF_HEIGHT = 0.23f;
    private static final float BLOCK_ICON_SIDE_HEIGHT = 0.52f;
    private static final float BLOCK_ICON_CENTER_X = 0.50f;
    private static final float BLOCK_ICON_CENTER_X_BIAS = -0.03f;
    private static final float BLOCK_ICON_CENTER_Y = 0.22f;
    private static final float BLOCK_ICON_TOP_BRIGHTNESS = 1.00f;
    private static final float BLOCK_ICON_LEFT_BRIGHTNESS = 0.60f;
    private static final float BLOCK_ICON_RIGHT_BRIGHTNESS = 0.45f;

    private TextRenderer textRenderer;
    private TextRenderer enchantmentTextRenderer;
    private World dynamicItemWorld;
    private Player dynamicItemPlayer;

    // Player model renderer for inventory preview
    private InventoryPlayerRenderer playerRenderer;

    public void setTextRenderer(TextRenderer textRenderer) {
        this.textRenderer = textRenderer;
    }

    public void setEnchantmentTextRenderer(TextRenderer textRenderer) {
        this.enchantmentTextRenderer = textRenderer;
    }

    public void init(int width, int height) throws Exception {
        this.windowWidth = width;
        this.windowHeight = height;

        // Create simple color shader
        shader = new ShaderProgram();
        shader.createVertexShader(
                "#version 330 core\n" +
                        "layout (location = 0) in vec2 aPos;\n" +
                        "uniform mat4 projection;\n" +
                        "void main() {\n" +
                        "    gl_Position = projection * vec4(aPos, 0.0, 1.0);\n" +
                        "}");
        shader.createFragmentShader(
                "#version 330 core\n" +
                        "out vec4 fragColor;\n" +
                        "uniform vec4 color;\n" +
                        "void main() {\n" +
                        "    fragColor = color;\n" +
                        "}");
        shader.link();
        shader.createUniform("projection");
        shader.createUniform("color");

        // Create VAO/VBO for color drawing
        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, 40 * Float.BYTES, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        // Textured shader for isometric block icons
        texturedShader = new ShaderProgram();
        texturedShader.createVertexShader(
                "#version 330 core\n" +
                        "layout (location = 0) in vec2 aPos;\n" +
                        "layout (location = 1) in vec2 aTexCoord;\n" +
                        "out vec2 texCoord;\n" +
                        "uniform mat4 projection;\n" +
                        "void main() {\n" +
                        "    gl_Position = projection * vec4(aPos, 0.0, 1.0);\n" +
                        "    texCoord = aTexCoord;\n" +
                        "}");
        texturedShader.createFragmentShader(
                "#version 330 core\n" +
                        "in vec2 texCoord;\n" +
                        "out vec4 fragColor;\n" +
                        "uniform sampler2D textureSampler;\n" +
                        "uniform float brightness;\n" +
                        "uniform float alpha;\n" +
                        "void main() {\n" +
                        "    vec4 texColor = texture(textureSampler, texCoord);\n" +
                        "    if (texColor.a < 0.1) discard;\n" +
                        "    fragColor = vec4(texColor.rgb * brightness, texColor.a * alpha);\n" +
                        "}");
        texturedShader.link();
        texturedShader.createUniform("projection");
        texturedShader.createUniform("textureSampler");
        texturedShader.createUniform("brightness");
        texturedShader.createUniform("alpha");
        texturedShader.bind();
        texturedShader.setUniform("alpha", 1.0f);
        texturedShader.unbind();

        // Create VAO/VBO for textured drawing
        texturedVao = glGenVertexArrays();
        texturedVbo = glGenBuffers();

        glBindVertexArray(texturedVao);
        glBindBuffer(GL_ARRAY_BUFFER, texturedVbo);
        glBufferData(GL_ARRAY_BUFFER, 64 * Float.BYTES, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void setAtlas(Texture atlas) {
        this.atlas = atlas;
    }

    // GUI textures for inventory and crafting backgrounds
    private Texture inventoryTexture; // inventory.png
    private Texture allItemsTexture; // allitems.png
    private Texture craftingTexture; // crafting.png
    private Texture containerTexture; // container.png
    private Texture furnaceTexture; // furnace.png
    private Texture trapTexture; // trap.png
    private Texture enchantTexture; // enchant.png
    private Texture alchemyTexture; // alchemy.png
    private Texture itemsTexture; // items.png for sticks and tools

    public void setGuiTextures(Texture inventory, Texture crafting) {
        this.inventoryTexture = inventory;
        this.craftingTexture = crafting;
    }

    public void setCreativeTexture(Texture allItems) {
        this.allItemsTexture = allItems;
    }

    public void setContainerTextures(Texture container, Texture furnace) {
        setContainerTextures(container, furnace, null);
    }

    public void setContainerTextures(Texture container, Texture furnace, Texture trap) {
        this.containerTexture = container;
        this.furnaceTexture = furnace;
        this.trapTexture = trap;
    }

    public void setProgressionTextures(Texture enchant, Texture alchemy) {
        this.enchantTexture = enchant;
        this.alchemyTexture = alchemy;
    }

    public void setItemsTexture(Texture items) {
        this.itemsTexture = items;
    }

    public void setDynamicItemContext(World world, Player player) {
        this.dynamicItemWorld = world;
        this.dynamicItemPlayer = player;
    }

    public void setPlayerRenderer(InventoryPlayerRenderer renderer) {
        this.playerRenderer = renderer;
    }

    public void updateOrtho(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        if (playerRenderer != null) {
            playerRenderer.updateScreenSize(width, height);
        }
    }

    public void render(InventoryScreen screen) {
        if (!screen.isOpen())
            return;

        // Setup GL state
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);

        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
        shader.bind();
        shader.setUniform("projection", ortho);

        int winX = screen.getWindowX();
        int winY = screen.getWindowY();
        Inventory inv = screen.getInventory();
        float scale = InventoryScreen.GUI_SCALE;

        // 1. Dim background (full screen overlay)
        drawRect(0, 0, windowWidth, windowHeight, 0.0f, 0.0f, 0.0f, 0.5f);

        // 2. Draw textured inventory background from inventory.png
        if (inventoryTexture != null) {
            shader.unbind();
            inventoryTexture.bind(0);
            texturedShader.bind();
            texturedShader.setUniform("projection", ortho);
            texturedShader.setUniform("textureSampler", 0);
            texturedShader.setUniform("brightness", 1.0f);

            // UV coordinates for inventory region (176x166 out of 256x256)
            float u1 = 0.0f, v1 = 0.0f;
            float u2 = 176.0f / 256.0f;
            float v2 = 166.0f / 256.0f;

            drawTexturedQuad(
                    winX, winY,
                    winX + InventoryScreen.WINDOW_WIDTH, winY,
                    winX + InventoryScreen.WINDOW_WIDTH, winY + InventoryScreen.WINDOW_HEIGHT,
                    winX, winY + InventoryScreen.WINDOW_HEIGHT,
                    u1, v1, u2, v2);

            texturedShader.unbind();
            inventoryTexture.unbind();
            shader.bind();
            shader.setUniform("projection", ortho);
        }

        // 3. Draw items in main inventory slots (0-26) at texture positions
        int itemSize = InventoryScreen.ITEM_SIZE;
        int itemOffset = (int) ((InventoryScreen.TEX_SLOT_SIZE - InventoryScreen.TEX_ITEM_SIZE) / 2 * scale);

        for (int row = 0; row < InventoryScreen.MAIN_ROWS; row++) {
            for (int col = 0; col < InventoryScreen.COLS; col++) {
                int slotIndex = row * InventoryScreen.COLS + col;
                int x = winX + (int) ((InventoryScreen.TEX_MAIN_INV_X + col * InventoryScreen.TEX_SLOT_SIZE) * scale)
                        + itemOffset;
                int y = winY + (int) ((InventoryScreen.TEX_MAIN_INV_Y + row * InventoryScreen.TEX_SLOT_SIZE) * scale)
                        + itemOffset;

                ItemStack item = inv.getMainInventory()[slotIndex];
                if (item != null && !item.isEmpty()) {
                    drawItemIconAt(x, y, itemSize, item);
                    if (item.getCount() > 1) {
                        drawStackCountAt(x, y, itemSize, item.getCount());
                    }
                }

                // Draw hover highlight
                if (screen.getHoveredSlot() == slotIndex) {
                    int slotX = winX
                            + (int) ((InventoryScreen.TEX_MAIN_INV_X + col * InventoryScreen.TEX_SLOT_SIZE) * scale);
                    int slotY = winY
                            + (int) ((InventoryScreen.TEX_MAIN_INV_Y + row * InventoryScreen.TEX_SLOT_SIZE) * scale);
                    drawRect(slotX + 1, slotY + 1, InventoryScreen.SLOT_SIZE - 2, InventoryScreen.SLOT_SIZE - 2,
                            HOVER_OVERLAY[0], HOVER_OVERLAY[1], HOVER_OVERLAY[2], HOVER_OVERLAY[3]);
                }
            }
        }

        // 4. Draw items in hotbar slots (27-35) at texture positions
        for (int col = 0; col < InventoryScreen.COLS; col++) {
            int slotIndex = 27 + col;
            int x = winX + (int) ((InventoryScreen.TEX_HOTBAR_X + col * InventoryScreen.TEX_SLOT_SIZE) * scale)
                    + itemOffset;
            int y = winY + (int) (InventoryScreen.TEX_HOTBAR_Y * scale) + itemOffset;

            ItemStack item = inv.getHotbar()[col];
            if (item != null && !item.isEmpty()) {
                drawItemIconAt(x, y, itemSize, item);
                if (item.getCount() > 1) {
                    drawStackCountAt(x, y, itemSize, item.getCount());
                }
            }

            // Draw hover highlight
            if (screen.getHoveredSlot() == slotIndex) {
                int slotX = winX + (int) ((InventoryScreen.TEX_HOTBAR_X + col * InventoryScreen.TEX_SLOT_SIZE) * scale);
                int slotY = winY + (int) (InventoryScreen.TEX_HOTBAR_Y * scale);
                drawRect(slotX + 1, slotY + 1, InventoryScreen.SLOT_SIZE - 2, InventoryScreen.SLOT_SIZE - 2,
                        HOVER_OVERLAY[0], HOVER_OVERLAY[1], HOVER_OVERLAY[2], HOVER_OVERLAY[3]);
            }
        }

        // 5. Draw items in crafting grid (36-39) at texture positions
        for (int row = 0; row < InventoryScreen.CRAFTING_ROWS; row++) {
            for (int col = 0; col < InventoryScreen.CRAFTING_COLS; col++) {
                int slotIndex = InventoryScreen.CRAFTING_SLOT_START + row * InventoryScreen.CRAFTING_COLS + col;
                int x = winX + (int) ((InventoryScreen.TEX_CRAFT_GRID_X + col * InventoryScreen.TEX_SLOT_SIZE) * scale)
                        + itemOffset;
                int y = winY + (int) ((InventoryScreen.TEX_CRAFT_GRID_Y + row * InventoryScreen.TEX_SLOT_SIZE) * scale)
                        + itemOffset;

                ItemStack item = inv.getCraftingGrid()[slotIndex - 36];
                if (item != null && !item.isEmpty()) {
                    drawItemIconAt(x, y, itemSize, item);
                    if (item.getCount() > 1) {
                        drawStackCountAt(x, y, itemSize, item.getCount());
                    }
                }

                // Draw hover highlight
                if (screen.getHoveredSlot() == slotIndex) {
                    int slotX = winX
                            + (int) ((InventoryScreen.TEX_CRAFT_GRID_X + col * InventoryScreen.TEX_SLOT_SIZE) * scale);
                    int slotY = winY
                            + (int) ((InventoryScreen.TEX_CRAFT_GRID_Y + row * InventoryScreen.TEX_SLOT_SIZE) * scale);
                    drawRect(slotX + 1, slotY + 1, InventoryScreen.SLOT_SIZE - 2, InventoryScreen.SLOT_SIZE - 2,
                            HOVER_OVERLAY[0], HOVER_OVERLAY[1], HOVER_OVERLAY[2], HOVER_OVERLAY[3]);
                }
            }
        }

        // 5.5. Draw equipped armor slots (41-44) at Release inventory texture positions
        for (int row = 0; row < InventoryScreen.ARMOR_SLOT_COUNT; row++) {
            int slotIndex = InventoryScreen.ARMOR_SLOT_START + row;
            int x = winX + (int) (InventoryScreen.TEX_ARMOR_X * scale) + itemOffset;
            int y = winY + (int) ((InventoryScreen.TEX_ARMOR_Y + row * InventoryScreen.TEX_SLOT_SIZE) * scale)
                    + itemOffset;

            ItemStack item = inv.getArmor()[row];
            if (item != null && !item.isEmpty()) {
                drawItemIconAt(x, y, itemSize, item);
            }

            if (screen.getHoveredSlot() == slotIndex) {
                int slotX = winX + (int) (InventoryScreen.TEX_ARMOR_X * scale);
                int slotY = winY
                        + (int) ((InventoryScreen.TEX_ARMOR_Y + row * InventoryScreen.TEX_SLOT_SIZE) * scale);
                drawRect(slotX + 1, slotY + 1, InventoryScreen.SLOT_SIZE - 2, InventoryScreen.SLOT_SIZE - 2,
                        HOVER_OVERLAY[0], HOVER_OVERLAY[1], HOVER_OVERLAY[2], HOVER_OVERLAY[3]);
            }
        }

        // 6. Draw crafting output (slot 40)
        com.craftzero.crafting.CraftingRecipe recipe = com.craftzero.crafting.CraftingRegistry
                .findRecipe(inv.getCraftingGrid());
        ItemStack outputItem = recipe != null ? recipe.getOutput() : null;

        int outputX = winX + (int) (InventoryScreen.TEX_CRAFT_OUTPUT_X * scale) + itemOffset;
        int outputY = winY + (int) (InventoryScreen.TEX_CRAFT_OUTPUT_Y * scale) + itemOffset;

        if (outputItem != null && !outputItem.isEmpty()) {
            drawItemIconAt(outputX, outputY, itemSize, outputItem);
            if (outputItem.getCount() > 1) {
                drawStackCountAt(outputX, outputY, itemSize, outputItem.getCount());
            }
        }

        // Output hover highlight
        if (screen.getHoveredSlot() == InventoryScreen.CRAFTING_OUTPUT_SLOT) {
            int slotX = winX + (int) (InventoryScreen.TEX_CRAFT_OUTPUT_X * scale);
            int slotY = winY + (int) (InventoryScreen.TEX_CRAFT_OUTPUT_Y * scale);
            drawRect(slotX + 1, slotY + 1, InventoryScreen.SLOT_SIZE - 2, InventoryScreen.SLOT_SIZE - 2,
                    HOVER_OVERLAY[0], HOVER_OVERLAY[1], HOVER_OVERLAY[2], HOVER_OVERLAY[3]);
        }

        // 6.5. Render player model (after all items, on top of background)
        if (playerRenderer != null) {
            shader.unbind();
            playerRenderer.render(screen);
            // Restore GL state after player model rendering
            glDisable(GL_DEPTH_TEST);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glDisable(GL_CULL_FACE);
            shader.bind();
            shader.setUniform("projection", ortho);
        }

        // 7. Draw cursor item (following mouse)
        ItemStack cursorItem = inv.getCursorItem();
        if (cursorItem != null && !cursorItem.isEmpty()) {
            int mx = (int) Input.getMouseX();
            int my = (int) Input.getMouseY();
            // Center the item on cursor
            drawItemIconAt(mx - itemSize / 2, my - itemSize / 2, itemSize, cursorItem);
            if (cursorItem.getCount() > 1) {
                drawStackCountAt(mx - itemSize / 2, my - itemSize / 2, itemSize, cursorItem.getCount());
            }
        }

        // 8. Draw Tooltip (LAST, on top of everything)
        int hoveredSlot = screen.getHoveredSlot();
        if (hoveredSlot != -1) {
            ItemStack item = null;
            if (hoveredSlot < 27)
                item = inv.getMainInventory()[hoveredSlot];
            else if (hoveredSlot < 36)
                item = inv.getHotbar()[hoveredSlot - 27];
            else if (hoveredSlot < InventoryScreen.CRAFTING_OUTPUT_SLOT)
                item = inv.getCraftingGrid()[hoveredSlot - InventoryScreen.CRAFTING_SLOT_START];
            else if (hoveredSlot == InventoryScreen.CRAFTING_OUTPUT_SLOT)
                item = outputItem;
            else if (hoveredSlot >= InventoryScreen.ARMOR_SLOT_START
                    && hoveredSlot < InventoryScreen.ARMOR_SLOT_START + InventoryScreen.ARMOR_SLOT_COUNT)
                item = inv.getArmor()[hoveredSlot - InventoryScreen.ARMOR_SLOT_START];

            if (item != null && !item.isEmpty()) {
                drawTooltip(item, (int) Input.getMouseX(), (int) Input.getMouseY());
            }
        }

        shader.unbind();

        // Restore GL state
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }

    public void renderCreative(CreativeInventoryScreen screen, int guiScale) {
        if (screen == null) {
            return;
        }

        int scale = Math.max(1, guiScale);
        int winX = screen.windowX() * scale;
        int winY = screen.windowY() * scale;
        int winW = CreativeInventoryScreen.TEX_WIDTH * scale;
        int winH = CreativeInventoryScreen.TEX_HEIGHT * scale;
        int itemSize = CreativeInventoryScreen.TEX_ITEM_SIZE * scale;
        int itemOffset = Math.max(1, scale);

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);

        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
        shader.bind();
        shader.setUniform("projection", ortho);

        if (allItemsTexture != null) {
            shader.unbind();
            allItemsTexture.bind(0);
            texturedShader.bind();
            texturedShader.setUniform("projection", ortho);
            texturedShader.setUniform("textureSampler", 0);
            texturedShader.setUniform("brightness", 1.0f);
            drawTexturedQuad(
                    winX, winY,
                    winX + winW, winY,
                    winX + winW, winY + winH,
                    winX, winY + winH,
                    0.0f, 0.0f,
                    CreativeInventoryScreen.TEX_WIDTH / 256.0f,
                    CreativeInventoryScreen.TEX_HEIGHT / 256.0f);
            texturedShader.unbind();
            allItemsTexture.unbind();
            shader.bind();
            shader.setUniform("projection", ortho);
        } else {
            drawRect(winX, winY, winW, winH, 0.72f, 0.72f, 0.72f, 1.0f);
            drawRectOutline(winX, winY, winW, winH, 0.15f, 0.15f, 0.15f, 1.0f);
        }

        drawCreativeScrollbar(screen, winX, winY, scale, ortho);

        for (int slot = 0; slot < CreativeInventoryScreen.GRID_SLOT_COUNT; slot++) {
            ItemStack stack = screen.stackAtVisibleSlot(slot);
            if (stack == null) {
                continue;
            }
            int col = slot % CreativeInventoryScreen.GRID_COLS;
            int row = slot / CreativeInventoryScreen.GRID_COLS;
            int slotX = winX + (CreativeInventoryScreen.TEX_GRID_X + col * CreativeInventoryScreen.TEX_SLOT_SIZE)
                    * scale;
            int slotY = winY + (CreativeInventoryScreen.TEX_GRID_Y + row * CreativeInventoryScreen.TEX_SLOT_SIZE)
                    * scale;
            drawScreenItem(stack, slotX + itemOffset, slotY + itemOffset, itemSize);
            drawHoverIfNeeded(screen.hoveredCreativeSlot(), slot, slotX, slotY,
                    CreativeInventoryScreen.TEX_SLOT_SIZE * scale);
        }

        Inventory inventory = screen.inventory();
        for (int col = 0; col < CreativeInventoryScreen.HOTBAR_COLS; col++) {
            int slotX = winX + (CreativeInventoryScreen.TEX_HOTBAR_X + col * CreativeInventoryScreen.TEX_SLOT_SIZE)
                    * scale;
            int slotY = winY + CreativeInventoryScreen.TEX_HOTBAR_Y * scale;
            if (col == inventory.getSelectedSlot()) {
                drawCreativeHotbarSelection(slotX, slotY, scale, ortho);
            }
            drawScreenItem(inventory.getHotbar()[col], slotX + itemOffset, slotY + itemOffset, itemSize);
            drawHoverIfNeeded(screen.hoveredHotbarSlot(), col, slotX, slotY,
                    CreativeInventoryScreen.TEX_SLOT_SIZE * scale);
        }

        if (textRenderer != null) {
            textRenderer.drawText("Item Selection", winX + 8 * scale, winY + 6 * scale, scale,
                    new float[] { 0.25f, 0.25f, 0.25f, 1.0f });
        }

        ItemStack cursorItem = inventory.getCursorItem();
        if (cursorItem != null && !cursorItem.isEmpty()) {
            int mx = (int) Input.getMouseX();
            int my = (int) Input.getMouseY();
            drawScreenItem(cursorItem, mx - itemSize / 2, my - itemSize / 2, itemSize);
        }

        ItemStack hovered = screen.hoveredStack();
        if (hovered != null && !hovered.isEmpty()) {
            drawTooltip(hovered, (int) Input.getMouseX(), (int) Input.getMouseY());
        }

        shader.unbind();
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }

    private void drawCreativeScrollbar(CreativeInventoryScreen screen, int winX, int winY, int scale, Matrix4f ortho) {
        int thumbX = winX + CreativeInventoryScreen.TEX_SCROLL_X * scale;
        int thumbY = winY + screen.scrollThumbTexY() * scale;
        int thumbW = CREATIVE_SCROLL_THUMB_W * scale;
        int thumbH = CreativeInventoryScreen.TEX_SCROLL_THUMB_HEIGHT * scale;
        if (allItemsTexture != null) {
            shader.unbind();
            allItemsTexture.bind(0);
            texturedShader.bind();
            texturedShader.setUniform("projection", ortho);
            texturedShader.setUniform("textureSampler", 0);
            texturedShader.setUniform("brightness", 1.0f);
            drawTexturedQuad(thumbX, thumbY, thumbX + thumbW, thumbY,
                    thumbX + thumbW, thumbY + thumbH, thumbX, thumbY + thumbH,
                    CREATIVE_SCROLL_THUMB_U / 256.0f,
                    CREATIVE_SCROLL_THUMB_V / 256.0f,
                    (CREATIVE_SCROLL_THUMB_U + CREATIVE_SCROLL_THUMB_W) / 256.0f,
                    (CREATIVE_SCROLL_THUMB_V + CREATIVE_SCROLL_THUMB_H) / 256.0f);
            texturedShader.unbind();
            allItemsTexture.unbind();
            shader.bind();
            shader.setUniform("projection", ortho);
            return;
        }
        drawRect(thumbX, thumbY, thumbW, thumbH, 0.66f, 0.66f, 0.66f, 1.0f);
        drawRect(thumbX, thumbY, thumbW, Math.max(1, scale), 0.95f, 0.95f, 0.95f, 1.0f);
        drawRect(thumbX, thumbY, Math.max(1, scale), thumbH, 0.92f, 0.92f, 0.92f, 1.0f);
        drawRect(thumbX, thumbY + thumbH - Math.max(1, scale), thumbW, Math.max(1, scale),
                0.30f, 0.30f, 0.30f, 1.0f);
        drawRect(thumbX + thumbW - Math.max(1, scale), thumbY, Math.max(1, scale), thumbH,
                0.30f, 0.30f, 0.30f, 1.0f);
    }

    private void drawCreativeHotbarSelection(int slotX, int slotY, int scale, Matrix4f ortho) {
        Texture guiTexture = GuiTexture.getGuiTexture();
        int x = slotX - HOTBAR_SELECTION_INSET * scale;
        int y = slotY - HOTBAR_SELECTION_INSET * scale;
        int size = HOTBAR_SELECTION_SIZE * scale;
        if (guiTexture == null) {
            drawRectOutline(slotX, slotY, CreativeInventoryScreen.TEX_SLOT_SIZE * scale,
                    CreativeInventoryScreen.TEX_SLOT_SIZE * scale, 1.0f, 1.0f, 0.35f, 1.0f);
            return;
        }
        float[] uv = GuiTexture.getHotbarSelectionUV();
        shader.unbind();
        guiTexture.bind(0);
        texturedShader.bind();
        texturedShader.setUniform("projection", ortho);
        texturedShader.setUniform("textureSampler", 0);
        texturedShader.setUniform("brightness", 1.0f);
        drawTexturedQuad(x, y, x + size, y, x + size, y + size, x, y + size,
                uv[0], uv[1], uv[2], uv[3]);
        texturedShader.unbind();
        guiTexture.unbind();
        shader.bind();
        shader.setUniform("projection", ortho);
    }

    /**
     * Draw an item icon at an exact position with specified size.
     */
    private void drawItemIconAt(int x, int y, int size, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        drawItemIconAt(x, y, size, stack.getType());
        drawPotionItemOverlay(x, y, size, stack);
        drawDynamicItemOverlay(x, y, size, stack.getType());
        drawEnchantedItemOverlay(x, y, size, stack);
    }

    private void drawItemIconAt(int x, int y, int size, ItemType type) {
        if (atlas != null) {
            if (type.getRenderProfile().modelKind() == com.craftzero.inventory.ItemRenderProfile.ModelKind.BLOCK) {
                // Same as hotbar - use full size
                drawIsometricBlockIcon(x, y, size, type);
            } else {
                drawItemSprite(x, y, size, type);
            }
        }
    }

    /**
     * Draw stack count at an exact position.
     */
    private void drawStackCountAt(int slotX, int slotY, int size, int count) {
        if (drawBitmapStackCount(slotX, slotY, size, size, count, 2, 2)) {
            return;
        }
        String countStr = String.valueOf(count);
        int digitWidth = 6;
        int digitHeight = 8;
        int spacing = 1;
        int totalWidth = countStr.length() * (digitWidth + spacing) - spacing;

        int baseX = slotX + size - totalWidth - 2;
        int baseY = slotY + size - digitHeight - 2;

        // Draw shadow first
        for (int i = 0; i < countStr.length(); i++) {
            int digit = countStr.charAt(i) - '0';
            int dx = baseX + i * (digitWidth + spacing) + 1;
            int dy = baseY + 1;
            drawDigit(dx, dy, digit, 0.1f, 0.1f, 0.1f);
        }

        // Draw white digits
        for (int i = 0; i < countStr.length(); i++) {
            int digit = countStr.charAt(i) - '0';
            int dx = baseX + i * (digitWidth + spacing);
            drawDigit(dx, baseY, digit, 1.0f, 1.0f, 1.0f);
        }
    }

    /**
     * Render the crafting table 3x3 grid UI with player inventory.
     */
    public void renderCraftingTable(CraftingTableScreen screen) {
        if (!screen.isOpen())
            return;

        // Setup GL state for 2D rendering
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);

        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
        shader.bind();
        shader.setUniform("projection", ortho);

        Inventory inventory = screen.getInventory();
        ItemStack[] craftingGrid = screen.getCraftingGrid();
        int hoveredSlot = screen.getHoveredSlot();
        int winX = screen.getWindowX();
        int winY = screen.getWindowY();
        float scale = CraftingTableScreen.GUI_SCALE;

        // 1. Draw semi-transparent background overlay
        drawRect(0, 0, this.windowWidth, this.windowHeight, 0.0f, 0.0f, 0.0f, 0.5f);

        // 2. Draw textured crafting table background from crafting.png
        if (craftingTexture != null) {
            shader.unbind();
            craftingTexture.bind(0);
            texturedShader.bind();
            texturedShader.setUniform("projection", ortho);
            texturedShader.setUniform("textureSampler", 0);
            texturedShader.setUniform("brightness", 1.0f);

            // UV coordinates for crafting region (176x166 out of 256x256)
            float u1 = 0.0f, v1 = 0.0f;
            float u2 = 176.0f / 256.0f;
            float v2 = 166.0f / 256.0f;

            drawTexturedQuad(
                    winX, winY,
                    winX + CraftingTableScreen.WINDOW_WIDTH, winY,
                    winX + CraftingTableScreen.WINDOW_WIDTH, winY + CraftingTableScreen.WINDOW_HEIGHT,
                    winX, winY + CraftingTableScreen.WINDOW_HEIGHT,
                    u1, v1, u2, v2);

            texturedShader.unbind();
            craftingTexture.unbind();
            shader.bind();
            shader.setUniform("projection", ortho);
        }

        int itemSize = CraftingTableScreen.ITEM_SIZE;
        int itemOffset = (int) ((CraftingTableScreen.TEX_SLOT_SIZE - CraftingTableScreen.TEX_ITEM_SIZE) / 2 * scale);

        // 3. Draw items in crafting grid (0-8)
        for (int row = 0; row < CraftingTableScreen.CRAFTING_ROWS; row++) {
            for (int col = 0; col < CraftingTableScreen.CRAFTING_COLS; col++) {
                int slotIndex = row * CraftingTableScreen.CRAFTING_COLS + col;
                int x = winX + (int) ((CraftingTableScreen.TEX_CRAFT_GRID_X + col * CraftingTableScreen.TEX_SLOT_SIZE)
                        * scale) + itemOffset;
                int y = winY + (int) ((CraftingTableScreen.TEX_CRAFT_GRID_Y + row * CraftingTableScreen.TEX_SLOT_SIZE)
                        * scale) + itemOffset;

                ItemStack item = craftingGrid[slotIndex];
                if (item != null && !item.isEmpty()) {
                    drawItemIconAt(x, y, itemSize, item);
                    if (item.getCount() > 1) {
                        drawStackCountAt(x, y, itemSize, item.getCount());
                    }
                }

                if (hoveredSlot == slotIndex) {
                    int slotX = winX
                            + (int) ((CraftingTableScreen.TEX_CRAFT_GRID_X + col * CraftingTableScreen.TEX_SLOT_SIZE)
                                    * scale);
                    int slotY = winY
                            + (int) ((CraftingTableScreen.TEX_CRAFT_GRID_Y + row * CraftingTableScreen.TEX_SLOT_SIZE)
                                    * scale);
                    drawRect(slotX + 1, slotY + 1, CraftingTableScreen.SLOT_SIZE - 2, CraftingTableScreen.SLOT_SIZE - 2,
                            HOVER_OVERLAY[0], HOVER_OVERLAY[1], HOVER_OVERLAY[2], HOVER_OVERLAY[3]);
                }
            }
        }

        // 4. Draw crafting output (slot 9)
        com.craftzero.crafting.CraftingRecipe recipe = com.craftzero.crafting.CraftingRegistry
                .findRecipe3x3(craftingGrid);
        ItemStack outputItem = (recipe != null) ? recipe.getOutput() : null;

        int outputX = winX + (int) (CraftingTableScreen.TEX_CRAFT_OUTPUT_X * scale) + itemOffset;
        int outputY = winY + (int) (CraftingTableScreen.TEX_CRAFT_OUTPUT_Y * scale) + itemOffset;

        if (outputItem != null && !outputItem.isEmpty()) {
            drawItemIconAt(outputX, outputY, itemSize, outputItem);
            if (outputItem.getCount() > 1) {
                drawStackCountAt(outputX, outputY, itemSize, outputItem.getCount());
            }
        }

        if (hoveredSlot == 9) {
            int slotX = winX + (int) (CraftingTableScreen.TEX_CRAFT_OUTPUT_X * scale);
            int slotY = winY + (int) (CraftingTableScreen.TEX_CRAFT_OUTPUT_Y * scale);
            drawRect(slotX + 1, slotY + 1, CraftingTableScreen.SLOT_SIZE - 2, CraftingTableScreen.SLOT_SIZE - 2,
                    HOVER_OVERLAY[0], HOVER_OVERLAY[1], HOVER_OVERLAY[2], HOVER_OVERLAY[3]);
        }

        // 5. Draw items in main inventory (10-36)
        for (int row = 0; row < CraftingTableScreen.INVENTORY_ROWS; row++) {
            for (int col = 0; col < CraftingTableScreen.INVENTORY_COLS; col++) {
                int slotIndex = 10 + row * CraftingTableScreen.INVENTORY_COLS + col;
                int x = winX
                        + (int) ((CraftingTableScreen.TEX_MAIN_INV_X + col * CraftingTableScreen.TEX_SLOT_SIZE) * scale)
                        + itemOffset;
                int y = winY
                        + (int) ((CraftingTableScreen.TEX_MAIN_INV_Y + row * CraftingTableScreen.TEX_SLOT_SIZE) * scale)
                        + itemOffset;

                ItemStack item = inventory.getMainInventory()[row * CraftingTableScreen.INVENTORY_COLS + col];
                if (item != null && !item.isEmpty()) {
                    drawItemIconAt(x, y, itemSize, item);
                    if (item.getCount() > 1) {
                        drawStackCountAt(x, y, itemSize, item.getCount());
                    }
                }

                if (hoveredSlot == slotIndex) {
                    int slotX = winX
                            + (int) ((CraftingTableScreen.TEX_MAIN_INV_X + col * CraftingTableScreen.TEX_SLOT_SIZE)
                                    * scale);
                    int slotY = winY
                            + (int) ((CraftingTableScreen.TEX_MAIN_INV_Y + row * CraftingTableScreen.TEX_SLOT_SIZE)
                                    * scale);
                    drawRect(slotX + 1, slotY + 1, CraftingTableScreen.SLOT_SIZE - 2, CraftingTableScreen.SLOT_SIZE - 2,
                            HOVER_OVERLAY[0], HOVER_OVERLAY[1], HOVER_OVERLAY[2], HOVER_OVERLAY[3]);
                }
            }
        }

        // 6. Draw items in hotbar (37-45)
        for (int col = 0; col < CraftingTableScreen.INVENTORY_COLS; col++) {
            int slotIndex = 37 + col;
            int x = winX + (int) ((CraftingTableScreen.TEX_HOTBAR_X + col * CraftingTableScreen.TEX_SLOT_SIZE) * scale)
                    + itemOffset;
            int y = winY + (int) (CraftingTableScreen.TEX_HOTBAR_Y * scale) + itemOffset;

            ItemStack item = inventory.getHotbar()[col];
            if (item != null && !item.isEmpty()) {
                drawItemIconAt(x, y, itemSize, item);
                if (item.getCount() > 1) {
                    drawStackCountAt(x, y, itemSize, item.getCount());
                }
            }

            if (hoveredSlot == slotIndex) {
                int slotX = winX
                        + (int) ((CraftingTableScreen.TEX_HOTBAR_X + col * CraftingTableScreen.TEX_SLOT_SIZE) * scale);
                int slotY = winY + (int) (CraftingTableScreen.TEX_HOTBAR_Y * scale);
                drawRect(slotX + 1, slotY + 1, CraftingTableScreen.SLOT_SIZE - 2, CraftingTableScreen.SLOT_SIZE - 2,
                        HOVER_OVERLAY[0], HOVER_OVERLAY[1], HOVER_OVERLAY[2], HOVER_OVERLAY[3]);
            }
        }

        // 7. Draw cursor item
        ItemStack cursorItem = inventory.getCursorItem();
        if (cursorItem != null && !cursorItem.isEmpty()) {
            int mx = (int) Input.getMouseX();
            int my = (int) Input.getMouseY();
            drawItemIconAt(mx - itemSize / 2, my - itemSize / 2, itemSize, cursorItem);
            if (cursorItem.getCount() > 1) {
                drawStackCountAt(mx - itemSize / 2, my - itemSize / 2, itemSize, cursorItem.getCount());
            }
        }

        // 8. Draw Tooltip
        if (hoveredSlot != -1) {
            ItemStack item = null;
            if (hoveredSlot < 9)
                item = craftingGrid[hoveredSlot];
            else if (hoveredSlot == 9)
                item = outputItem;
            else if (hoveredSlot < 37)
                item = inventory.getMainInventory()[hoveredSlot - 10];
            else if (hoveredSlot < 46)
                item = inventory.getHotbar()[hoveredSlot - 37];

            if (item != null && !item.isEmpty()) {
                drawTooltip(item, (int) Input.getMouseX(), (int) Input.getMouseY());
            }
        }

        shader.unbind();

        // Restore GL state
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }

    public void renderChest(ChestScreen screen) {
        if (!screen.isOpen())
            return;

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);

        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
        shader.bind();
        shader.setUniform("projection", ortho);

        int winX = screen.getWindowX();
        int winY = screen.getWindowY();
        float scale = ChestScreen.GUI_SCALE;
        int rows = screen.getContainerRows();
        int texHeight = 114 + rows * 18;

        drawRect(0, 0, windowWidth, windowHeight, 0.0f, 0.0f, 0.0f, 0.5f);
        if (containerTexture != null) {
            shader.unbind();
            containerTexture.bind(0);
            texturedShader.bind();
            texturedShader.setUniform("projection", ortho);
            texturedShader.setUniform("textureSampler", 0);
            texturedShader.setUniform("brightness", 1.0f);
            drawTexturedQuad(winX, winY, winX + ChestScreen.WINDOW_WIDTH, winY,
                    winX + ChestScreen.WINDOW_WIDTH, winY + screen.getWindowHeight(),
                    winX, winY + screen.getWindowHeight(),
                    0.0f, 0.0f, 176.0f / 256.0f, texHeight / 256.0f);
            texturedShader.unbind();
            containerTexture.unbind();
            shader.bind();
            shader.setUniform("projection", ortho);
        }

        int itemSize = ChestScreen.ITEM_SIZE;
        int itemOffset = (int) ((ChestScreen.TEX_SLOT_SIZE - ChestScreen.TEX_ITEM_SIZE) / 2 * scale);
        for (int slot = 0; slot < screen.getContainerSize(); slot++) {
            int row = slot / ChestScreen.COLS;
            int col = slot % ChestScreen.COLS;
            int x = winX + (int) ((ChestScreen.TEX_CONTAINER_X + col * ChestScreen.TEX_SLOT_SIZE) * scale)
                    + itemOffset;
            int y = winY + (int) ((ChestScreen.TEX_CONTAINER_Y + row * ChestScreen.TEX_SLOT_SIZE) * scale)
                    + itemOffset;
            drawScreenItem(screen.getItemInSlot(slot), x, y, itemSize);
            drawHoverIfNeeded(screen.getHoveredSlot(), slot,
                    winX + (int) ((ChestScreen.TEX_CONTAINER_X + col * ChestScreen.TEX_SLOT_SIZE) * scale),
                    winY + (int) ((ChestScreen.TEX_CONTAINER_Y + row * ChestScreen.TEX_SLOT_SIZE) * scale),
                    ChestScreen.SLOT_SIZE);
        }

        renderPlayerInventorySlotsForChest(screen, winX, winY, itemOffset, itemSize, scale);
        renderCursorAndTooltip(screen.getInventory(), screen.getHoveredSlot(), i -> screen.getItemInSlot(i), itemSize);

        shader.unbind();
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }

    private void renderPlayerInventorySlotsForChest(ChestScreen screen, int winX, int winY, int itemOffset,
            int itemSize, float scale) {
        int base = screen.getContainerSize();
        for (int row = 0; row < ChestScreen.MAIN_ROWS; row++) {
            for (int col = 0; col < ChestScreen.COLS; col++) {
                int slot = base + row * ChestScreen.COLS + col;
                int x = winX + (int) ((ChestScreen.TEX_MAIN_INV_X + col * ChestScreen.TEX_SLOT_SIZE) * scale)
                        + itemOffset;
                int y = winY + (int) ((screen.getTexMainInvY() + row * ChestScreen.TEX_SLOT_SIZE) * scale)
                        + itemOffset;
                drawScreenItem(screen.getItemInSlot(slot), x, y, itemSize);
                drawHoverIfNeeded(screen.getHoveredSlot(), slot,
                        winX + (int) ((ChestScreen.TEX_MAIN_INV_X + col * ChestScreen.TEX_SLOT_SIZE) * scale),
                        winY + (int) ((screen.getTexMainInvY() + row * ChestScreen.TEX_SLOT_SIZE) * scale),
                        ChestScreen.SLOT_SIZE);
            }
        }
        int hotbarBase = base + Inventory.MAIN_SIZE;
        for (int col = 0; col < ChestScreen.COLS; col++) {
            int slot = hotbarBase + col;
            int x = winX + (int) ((ChestScreen.TEX_HOTBAR_X + col * ChestScreen.TEX_SLOT_SIZE) * scale)
                    + itemOffset;
            int y = winY + (int) (screen.getTexHotbarY() * scale) + itemOffset;
            drawScreenItem(screen.getItemInSlot(slot), x, y, itemSize);
            drawHoverIfNeeded(screen.getHoveredSlot(), slot,
                    winX + (int) ((ChestScreen.TEX_HOTBAR_X + col * ChestScreen.TEX_SLOT_SIZE) * scale),
                    winY + (int) (screen.getTexHotbarY() * scale),
                    ChestScreen.SLOT_SIZE);
        }
    }

    public void renderFurnace(FurnaceScreen screen) {
        if (!screen.isOpen())
            return;

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);

        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
        shader.bind();
        shader.setUniform("projection", ortho);

        int winX = screen.getWindowX();
        int winY = screen.getWindowY();
        float scale = FurnaceScreen.GUI_SCALE;

        drawRect(0, 0, windowWidth, windowHeight, 0.0f, 0.0f, 0.0f, 0.5f);
        if (furnaceTexture != null) {
            shader.unbind();
            furnaceTexture.bind(0);
            texturedShader.bind();
            texturedShader.setUniform("projection", ortho);
            texturedShader.setUniform("textureSampler", 0);
            texturedShader.setUniform("brightness", 1.0f);
            drawTexturedQuad(winX, winY, winX + FurnaceScreen.WINDOW_WIDTH, winY,
                    winX + FurnaceScreen.WINDOW_WIDTH, winY + FurnaceScreen.WINDOW_HEIGHT,
                    winX, winY + FurnaceScreen.WINDOW_HEIGHT,
                    0.0f, 0.0f, 176.0f / 256.0f, 166.0f / 256.0f);
            texturedShader.unbind();
            furnaceTexture.unbind();
            shader.bind();
            shader.setUniform("projection", ortho);
        }

        renderFurnaceProgress(screen, winX, winY, scale, ortho);

        int itemSize = FurnaceScreen.ITEM_SIZE;
        int itemOffset = (int) ((FurnaceScreen.TEX_SLOT_SIZE - FurnaceScreen.TEX_ITEM_SIZE) / 2 * scale);
        renderFurnaceSlot(screen, FurnaceTileEntity.SLOT_INPUT, FurnaceScreen.TEX_INPUT_X, FurnaceScreen.TEX_INPUT_Y,
                winX, winY, itemOffset, itemSize, scale);
        renderFurnaceSlot(screen, FurnaceTileEntity.SLOT_FUEL, FurnaceScreen.TEX_FUEL_X, FurnaceScreen.TEX_FUEL_Y,
                winX, winY, itemOffset, itemSize, scale);
        renderFurnaceSlot(screen, FurnaceTileEntity.SLOT_OUTPUT, FurnaceScreen.TEX_OUTPUT_X, FurnaceScreen.TEX_OUTPUT_Y,
                winX, winY, itemOffset, itemSize, scale);

        for (int row = 0; row < FurnaceScreen.MAIN_ROWS; row++) {
            for (int col = 0; col < FurnaceScreen.COLS; col++) {
                int slot = FurnaceTileEntity.SIZE + row * FurnaceScreen.COLS + col;
                renderFurnaceSlot(screen, slot,
                        FurnaceScreen.TEX_MAIN_INV_X + col * FurnaceScreen.TEX_SLOT_SIZE,
                        FurnaceScreen.TEX_MAIN_INV_Y + row * FurnaceScreen.TEX_SLOT_SIZE,
                        winX, winY, itemOffset, itemSize, scale);
            }
        }
        int hotbarBase = FurnaceTileEntity.SIZE + Inventory.MAIN_SIZE;
        for (int col = 0; col < FurnaceScreen.COLS; col++) {
            renderFurnaceSlot(screen, hotbarBase + col,
                    FurnaceScreen.TEX_HOTBAR_X + col * FurnaceScreen.TEX_SLOT_SIZE,
                    FurnaceScreen.TEX_HOTBAR_Y,
                    winX, winY, itemOffset, itemSize, scale);
        }

        renderCursorAndTooltip(screen.getInventory(), screen.getHoveredSlot(), i -> screen.getItemInSlot(i), itemSize);

        shader.unbind();
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }

    public void renderDispenser(DispenserScreen screen) {
        if (!screen.isOpen())
            return;

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);

        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
        shader.bind();
        shader.setUniform("projection", ortho);

        int winX = screen.getWindowX();
        int winY = screen.getWindowY();
        float scale = DispenserScreen.GUI_SCALE;

        drawRect(0, 0, windowWidth, windowHeight, 0.0f, 0.0f, 0.0f, 0.5f);
        if (trapTexture != null) {
            shader.unbind();
            trapTexture.bind(0);
            texturedShader.bind();
            texturedShader.setUniform("projection", ortho);
            texturedShader.setUniform("textureSampler", 0);
            texturedShader.setUniform("brightness", 1.0f);
            drawTexturedQuad(winX, winY, winX + DispenserScreen.WINDOW_WIDTH, winY,
                    winX + DispenserScreen.WINDOW_WIDTH, winY + DispenserScreen.WINDOW_HEIGHT,
                    winX, winY + DispenserScreen.WINDOW_HEIGHT,
                    0.0f, 0.0f, 176.0f / 256.0f, 166.0f / 256.0f);
            texturedShader.unbind();
            trapTexture.unbind();
            shader.bind();
            shader.setUniform("projection", ortho);
        }

        int itemSize = DispenserScreen.ITEM_SIZE;
        int itemOffset = (int) ((DispenserScreen.TEX_SLOT_SIZE - DispenserScreen.TEX_ITEM_SIZE) / 2 * scale);
        for (int row = 0; row < DispenserScreen.DISPENSER_ROWS; row++) {
            for (int col = 0; col < DispenserScreen.DISPENSER_COLS; col++) {
                int slot = row * DispenserScreen.DISPENSER_COLS + col;
                renderDispenserSlot(screen, slot,
                        DispenserScreen.TEX_CONTAINER_X + col * DispenserScreen.TEX_SLOT_SIZE,
                        DispenserScreen.TEX_CONTAINER_Y + row * DispenserScreen.TEX_SLOT_SIZE,
                        winX, winY, itemOffset, itemSize, scale);
            }
        }

        for (int row = 0; row < DispenserScreen.MAIN_ROWS; row++) {
            for (int col = 0; col < DispenserScreen.COLS; col++) {
                int slot = com.craftzero.world.tile.DispenserTileEntity.SIZE + row * DispenserScreen.COLS + col;
                renderDispenserSlot(screen, slot,
                        DispenserScreen.TEX_MAIN_INV_X + col * DispenserScreen.TEX_SLOT_SIZE,
                        DispenserScreen.TEX_MAIN_INV_Y + row * DispenserScreen.TEX_SLOT_SIZE,
                        winX, winY, itemOffset, itemSize, scale);
            }
        }
        int hotbarBase = com.craftzero.world.tile.DispenserTileEntity.SIZE + Inventory.MAIN_SIZE;
        for (int col = 0; col < DispenserScreen.COLS; col++) {
            renderDispenserSlot(screen, hotbarBase + col,
                    DispenserScreen.TEX_HOTBAR_X + col * DispenserScreen.TEX_SLOT_SIZE,
                    DispenserScreen.TEX_HOTBAR_Y,
                    winX, winY, itemOffset, itemSize, scale);
        }

        renderCursorAndTooltip(screen.getInventory(), screen.getHoveredSlot(), i -> screen.getItemInSlot(i), itemSize);

        shader.unbind();
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }

    private void renderDispenserSlot(DispenserScreen screen, int slot, int texX, int texY,
            int winX, int winY, int itemOffset, int itemSize, float scale) {
        int x = winX + (int) (texX * scale) + itemOffset;
        int y = winY + (int) (texY * scale) + itemOffset;
        drawScreenItem(screen.getItemInSlot(slot), x, y, itemSize);
        drawHoverIfNeeded(screen.getHoveredSlot(), slot,
                winX + (int) (texX * scale), winY + (int) (texY * scale), DispenserScreen.SLOT_SIZE);
    }

    public void renderSignEditor(SignEditScreen screen) {
        if (screen == null || !screen.isOpen() || screen.getSign() == null || textRenderer == null) {
            return;
        }

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);

        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
        shader.bind();
        shader.setUniform("projection", ortho);

        drawRect(0, 0, windowWidth, windowHeight, 0.0f, 0.0f, 0.0f, 0.45f);

        String title = "Edit sign message:";
        float titleScale = 2.0f;
        int titleWidth = textRenderer.getStringWidth(title, titleScale);
        int titleY = Math.max(22, windowHeight / 2 - 188);
        textRenderer.drawText(title, (windowWidth - titleWidth) / 2 + 2, titleY + 2, titleScale,
                new float[] { 0.08f, 0.08f, 0.08f, 0.95f });
        textRenderer.drawText(title, (windowWidth - titleWidth) / 2, titleY, titleScale,
                new float[] { 1.0f, 1.0f, 1.0f, 0.98f });

        shader.bind();
        shader.setUniform("projection", ortho);

        int signWidth = 340;
        int signHeight = 160;
        int signX = (windowWidth - signWidth) / 2;
        int signY = Math.max(titleY + 42, windowHeight / 2 - 112);
        drawRect(signX + signWidth / 2 - 18, signY + signHeight - 2, 36, 74,
                0.35f, 0.21f, 0.10f, 1.0f);
        drawRectOutline(signX + signWidth / 2 - 18, signY + signHeight - 2, 36, 74,
                0.16f, 0.08f, 0.03f, 1.0f);
        drawRect(signX - 7, signY + 6, signWidth + 14, signHeight + 10,
                0.12f, 0.07f, 0.03f, 0.38f);
        drawRect(signX, signY, signWidth, signHeight, 0.54f, 0.34f, 0.16f, 1.0f);
        drawRect(signX, signY, signWidth, signHeight / 3, 0.62f, 0.40f, 0.20f, 1.0f);
        drawRect(signX, signY + signHeight / 3, signWidth, signHeight / 3,
                0.55f, 0.34f, 0.16f, 1.0f);
        drawRect(signX, signY + (signHeight / 3) * 2, signWidth, signHeight - (signHeight / 3) * 2,
                0.48f, 0.29f, 0.13f, 1.0f);
        drawRect(signX, signY + signHeight / 3, signWidth, 2, 0.36f, 0.20f, 0.08f, 0.60f);
        drawRect(signX, signY + (signHeight / 3) * 2, signWidth, 2, 0.36f, 0.20f, 0.08f, 0.60f);
        drawRectOutline(signX, signY, signWidth, signHeight, 0.18f, 0.09f, 0.03f, 1.0f);
        drawRectOutline(signX + 3, signY + 3, signWidth - 6, signHeight - 6,
                0.76f, 0.52f, 0.27f, 0.45f);

        String[] lines = screen.getSign().getLines();
        for (int i = 0; i < lines.length; i++) {
            int lineY = signY + 32 + i * 28;
            String text = i == screen.getSelectedLine() ? lines[i] + "_" : lines[i];
            int textWidth = textRenderer.getStringWidth(text, 2.0f);
            textRenderer.drawText(text, signX + (signWidth - textWidth) / 2, lineY, 2.0f,
                    new float[] { 0.05f, 0.03f, 0.02f, 1.0f });
        }

        shader.bind();
        shader.setUniform("projection", ortho);
        SignEditScreen.ButtonBounds done = SignEditScreen.doneButtonBounds(windowWidth, windowHeight);
        boolean doneHovered = done.contains(Input.getMouseX(), Input.getMouseY());
        drawClassicGuiButton(done.x(), done.y(), done.width(), done.height(), "Done", doneHovered);

        shader.unbind();
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }

    private void drawClassicGuiButton(int x, int y, int width, int height, String label, boolean hovered) {
        if (!drawClassicGuiButtonTexture(x, y, width, height, hovered)) {
            float shade = hovered ? 0.62f : 0.44f;
            drawRect(x, y, width, height, shade, shade, hovered ? 0.72f : shade, 1.0f);
            drawRectOutline(x, y, width, height, 0.08f, 0.08f, 0.08f, 1.0f);
        }
        float scale = 2.0f;
        int textWidth = textRenderer.getStringWidth(label, scale);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - Math.round(8 * scale)) / 2;
        float[] face = hovered
                ? new float[] { 1.0f, 1.0f, 160.0f / 255.0f, 1.0f }
                : new float[] { 224.0f / 255.0f, 224.0f / 255.0f, 224.0f / 255.0f, 1.0f };
        textRenderer.drawText(label, textX + 1, textY + 1, scale,
                new float[] { 0.15f, 0.15f, 0.15f, 1.0f });
        textRenderer.drawText(label, textX, textY, scale, face);
    }

    private boolean drawClassicGuiButtonTexture(int x, int y, int width, int height, boolean hovered) {
        Texture gui = GuiTexture.getGuiTexture();
        if (gui == null || texturedShader == null || texturedVao == 0 || width <= 0 || height <= 0) {
            return false;
        }
        ClassicGuiTexture.ButtonState state = hovered
                ? ClassicGuiTexture.ButtonState.HOVERED
                : ClassicGuiTexture.ButtonState.NORMAL;
        int leftWidth = Math.max(1, width / 2);
        int rightWidth = Math.max(1, width - leftWidth);
        UvRegion left = ClassicGuiTexture.buttonHalf(state, ClassicGuiTexture.ButtonHalf.LEFT);
        UvRegion right = ClassicGuiTexture.buttonHalf(state, ClassicGuiTexture.ButtonHalf.RIGHT);

        shader.unbind();
        gui.bind(0);
        texturedShader.bind();
        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
        texturedShader.setUniform("projection", ortho);
        texturedShader.setUniform("textureSampler", 0);
        texturedShader.setUniform("brightness", 1.0f);
        drawTexturedQuad(x, y, x + leftWidth, y, x + leftWidth, y + height, x, y + height,
                left.u1(), left.v1(), left.u2(), left.v2());
        drawTexturedQuad(x + leftWidth, y, x + leftWidth + rightWidth, y,
                x + leftWidth + rightWidth, y + height, x + leftWidth, y + height,
                right.u1(), right.v1(), right.u2(), right.v2());
        texturedShader.unbind();
        gui.unbind();
        shader.bind();
        shader.setUniform("projection", ortho);
        return true;
    }

    private void renderFurnaceSlot(FurnaceScreen screen, int slot, int texX, int texY,
            int winX, int winY, int itemOffset, int itemSize, float scale) {
        int x = winX + (int) (texX * scale) + itemOffset;
        int y = winY + (int) (texY * scale) + itemOffset;
        drawScreenItem(screen.getItemInSlot(slot), x, y, itemSize);
        drawHoverIfNeeded(screen.getHoveredSlot(), slot,
                winX + (int) (texX * scale), winY + (int) (texY * scale), FurnaceScreen.SLOT_SIZE);
    }

    private void renderFurnaceProgress(FurnaceScreen screen, int winX, int winY, float scale, Matrix4f ortho) {
        FurnaceTileEntity furnace = screen.getFurnace();
        if (furnace == null) {
            return;
        }
        FurnaceScreen.ProgressOverlay flame = FurnaceScreen.getBurnFlameOverlay(furnace);
        FurnaceScreen.ProgressOverlay arrow = FurnaceScreen.getCookArrowOverlay(furnace);
        if (furnaceTexture != null) {
            shader.unbind();
            furnaceTexture.bind(0);
            texturedShader.bind();
            texturedShader.setUniform("projection", ortho);
            texturedShader.setUniform("textureSampler", 0);
            texturedShader.setUniform("brightness", 1.0f);
            drawFurnaceOverlay(winX, winY, scale, flame);
            drawFurnaceOverlay(winX, winY, scale, arrow);
            texturedShader.unbind();
            furnaceTexture.unbind();
            shader.bind();
            shader.setUniform("projection", ortho);
            return;
        }
        if (flame != null) {
            drawRect(winX + (int) (flame.x() * scale),
                    winY + (int) (flame.y() * scale),
                    (int) (flame.width() * scale), (int) (flame.height() * scale),
                    1.0f, 0.45f, 0.05f, 1.0f);
        }
        if (arrow != null) {
            drawRect(winX + (int) (arrow.x() * scale),
                    winY + (int) (arrow.y() * scale),
                    (int) (arrow.width() * scale), (int) (arrow.height() * scale),
                    0.75f, 0.75f, 0.75f, 1.0f);
        }
    }

    private void drawFurnaceOverlay(int winX, int winY, float scale, FurnaceScreen.ProgressOverlay overlay) {
        if (overlay == null || overlay.width() <= 0 || overlay.height() <= 0) {
            return;
        }
        float x = winX + overlay.x() * scale;
        float y = winY + overlay.y() * scale;
        float width = overlay.width() * scale;
        float height = overlay.height() * scale;
        float u1 = overlay.sourceX() / 256.0f;
        float v1 = overlay.sourceY() / 256.0f;
        float u2 = (overlay.sourceX() + overlay.width()) / 256.0f;
        float v2 = (overlay.sourceY() + overlay.height()) / 256.0f;
        drawTexturedQuad(x, y, x + width, y, x + width, y + height, x, y + height,
                u1, v1, u2, v2);
    }

    public void renderBrewingStand(BrewingStandScreen screen) {
        if (screen == null || !screen.isOpen()) {
            return;
        }
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);

        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
        shader.bind();
        shader.setUniform("projection", ortho);

        int winX = screen.getWindowX();
        int winY = screen.getWindowY();
        float scale = BrewingStandScreen.GUI_SCALE;
        drawRect(0, 0, windowWidth, windowHeight, 0.0f, 0.0f, 0.0f, 0.5f);
        if (alchemyTexture != null) {
            shader.unbind();
            alchemyTexture.bind(0);
            texturedShader.bind();
            texturedShader.setUniform("projection", ortho);
            texturedShader.setUniform("textureSampler", 0);
            texturedShader.setUniform("brightness", 1.0f);
            drawTexturedQuad(winX, winY, winX + BrewingStandScreen.WINDOW_WIDTH, winY,
                    winX + BrewingStandScreen.WINDOW_WIDTH, winY + BrewingStandScreen.WINDOW_HEIGHT,
                    winX, winY + BrewingStandScreen.WINDOW_HEIGHT,
                    0.0f, 0.0f, 176.0f / 256.0f, 166.0f / 256.0f);
            texturedShader.unbind();
            alchemyTexture.unbind();
            shader.bind();
            shader.setUniform("projection", ortho);
        }

        renderBrewingProgress(screen, winX, winY, scale);
        int itemSize = BrewingStandScreen.ITEM_SIZE;
        int itemOffset = (int) ((BrewingStandScreen.TEX_SLOT_SIZE - BrewingStandScreen.TEX_ITEM_SIZE) / 2 * scale);
        renderBrewingSlot(screen, BrewingStandTileEntity.SLOT_INGREDIENT,
                BrewingStandScreen.TEX_INGREDIENT_X, BrewingStandScreen.TEX_INGREDIENT_Y,
                winX, winY, itemOffset, itemSize, scale);
        renderBrewingSlot(screen, BrewingStandTileEntity.SLOT_BOTTLE_0,
                BrewingStandScreen.TEX_BOTTLE_0_X, BrewingStandScreen.TEX_BOTTLE_0_Y,
                winX, winY, itemOffset, itemSize, scale);
        renderBrewingSlot(screen, BrewingStandTileEntity.SLOT_BOTTLE_1,
                BrewingStandScreen.TEX_BOTTLE_1_X, BrewingStandScreen.TEX_BOTTLE_1_Y,
                winX, winY, itemOffset, itemSize, scale);
        renderBrewingSlot(screen, BrewingStandTileEntity.SLOT_BOTTLE_2,
                BrewingStandScreen.TEX_BOTTLE_2_X, BrewingStandScreen.TEX_BOTTLE_2_Y,
                winX, winY, itemOffset, itemSize, scale);
        for (int row = 0; row < BrewingStandScreen.MAIN_ROWS; row++) {
            for (int col = 0; col < BrewingStandScreen.COLS; col++) {
                int slot = BrewingStandTileEntity.SIZE + row * BrewingStandScreen.COLS + col;
                renderBrewingSlot(screen, slot,
                        BrewingStandScreen.TEX_MAIN_INV_X + col * BrewingStandScreen.TEX_SLOT_SIZE,
                        BrewingStandScreen.TEX_MAIN_INV_Y + row * BrewingStandScreen.TEX_SLOT_SIZE,
                        winX, winY, itemOffset, itemSize, scale);
            }
        }
        int hotbarBase = BrewingStandTileEntity.SIZE + Inventory.MAIN_SIZE;
        for (int col = 0; col < BrewingStandScreen.COLS; col++) {
            renderBrewingSlot(screen, hotbarBase + col,
                    BrewingStandScreen.TEX_HOTBAR_X + col * BrewingStandScreen.TEX_SLOT_SIZE,
                    BrewingStandScreen.TEX_HOTBAR_Y,
                    winX, winY, itemOffset, itemSize, scale);
        }
        renderCursorAndTooltip(screen.getInventory(), screen.getHoveredSlot(), i -> screen.getItemInSlot(i), itemSize);

        shader.unbind();
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }

    private void renderBrewingSlot(BrewingStandScreen screen, int slot, int texX, int texY,
            int winX, int winY, int itemOffset, int itemSize, float scale) {
        int x = winX + (int) (texX * scale) + itemOffset;
        int y = winY + (int) (texY * scale) + itemOffset;
        drawScreenItem(screen.getItemInSlot(slot), x, y, itemSize);
        drawHoverIfNeeded(screen.getHoveredSlot(), slot,
                winX + (int) (texX * scale), winY + (int) (texY * scale), BrewingStandScreen.SLOT_SIZE);
    }

    private void renderBrewingProgress(BrewingStandScreen screen, int winX, int winY, float scale) {
        BrewingStandTileEntity brewingStand = screen.getBrewingStand();
        if (brewingStand == null || brewingStand.getBrewTime() <= 0) {
            return;
        }
        int elapsed = BrewingStandTileEntity.BREW_TIME_TOTAL - brewingStand.getBrewTime();
        int height = Math.round(28.0f * elapsed / BrewingStandTileEntity.BREW_TIME_TOTAL);
        drawRect(winX + (int) (97 * scale), winY + (int) ((16 + 28 - height) * scale),
                (int) (9 * scale), (int) (height * scale),
                0.45f, 0.28f, 0.95f, 1.0f);
    }

    public void renderEnchantingTable(EnchantingTableScreen screen) {
        if (screen == null || !screen.isOpen()) {
            return;
        }
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);

        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
        shader.bind();
        shader.setUniform("projection", ortho);

        int winX = screen.getWindowX();
        int winY = screen.getWindowY();
        float scale = EnchantingTableScreen.GUI_SCALE;
        drawRect(0, 0, windowWidth, windowHeight, 0.0f, 0.0f, 0.0f, 0.5f);
        if (enchantTexture != null) {
            shader.unbind();
            enchantTexture.bind(0);
            texturedShader.bind();
            texturedShader.setUniform("projection", ortho);
            texturedShader.setUniform("textureSampler", 0);
            texturedShader.setUniform("brightness", 1.0f);
            drawTexturedQuad(winX, winY, winX + EnchantingTableScreen.WINDOW_WIDTH, winY,
                    winX + EnchantingTableScreen.WINDOW_WIDTH, winY + EnchantingTableScreen.WINDOW_HEIGHT,
                    winX, winY + EnchantingTableScreen.WINDOW_HEIGHT,
                    0.0f, 0.0f, 176.0f / 256.0f, 166.0f / 256.0f);
            texturedShader.unbind();
            enchantTexture.unbind();
            shader.bind();
            shader.setUniform("projection", ortho);
        }

        renderEnchantOffers(screen, winX, winY, scale);
        int itemSize = EnchantingTableScreen.ITEM_SIZE;
        int itemOffset = (int) ((EnchantingTableScreen.TEX_SLOT_SIZE - EnchantingTableScreen.TEX_ITEM_SIZE) / 2 * scale);
        renderEnchantSlot(screen, 0, EnchantingTableScreen.TEX_TABLE_SLOT_X, EnchantingTableScreen.TEX_TABLE_SLOT_Y,
                winX, winY, itemOffset, itemSize, scale);
        for (int row = 0; row < EnchantingTableScreen.MAIN_ROWS; row++) {
            for (int col = 0; col < EnchantingTableScreen.COLS; col++) {
                int slot = 1 + row * EnchantingTableScreen.COLS + col;
                renderEnchantSlot(screen, slot,
                        EnchantingTableScreen.TEX_MAIN_INV_X + col * EnchantingTableScreen.TEX_SLOT_SIZE,
                        EnchantingTableScreen.TEX_MAIN_INV_Y + row * EnchantingTableScreen.TEX_SLOT_SIZE,
                        winX, winY, itemOffset, itemSize, scale);
            }
        }
        int hotbarBase = 1 + Inventory.MAIN_SIZE;
        for (int col = 0; col < EnchantingTableScreen.COLS; col++) {
            renderEnchantSlot(screen, hotbarBase + col,
                    EnchantingTableScreen.TEX_HOTBAR_X + col * EnchantingTableScreen.TEX_SLOT_SIZE,
                    EnchantingTableScreen.TEX_HOTBAR_Y,
                    winX, winY, itemOffset, itemSize, scale);
        }
        renderCursorAndTooltip(screen.getInventory(), screen.getHoveredSlot(), i -> screen.getItemInSlot(i), itemSize);

        shader.unbind();
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }

    private void renderEnchantOffers(EnchantingTableScreen screen, int winX, int winY, float scale) {
        int[] offers = screen.getOffers();
        int level = screen.getProgression() == null ? 0 : screen.getProgression().getLevel();
        drawEnchantOfferRows(screen, offers, level, winX, winY, scale);
        for (int i = 0; i < offers.length; i++) {
            int x = winX + (int) (EnchantingTableScreen.TEX_OFFER_X * scale);
            int y = winY + (int) ((EnchantingTableScreen.TEX_OFFER_Y + i * EnchantingTableScreen.TEX_OFFER_H) * scale);
            int w = (int) (EnchantingTableScreen.TEX_OFFER_W * scale);
            boolean enabled = offers[i] > 0 && level >= offers[i];
            if (textRenderer != null && offers[i] > 0) {
                TextRenderer phraseRenderer = enchantmentTextRenderer == null ? textRenderer : enchantmentTextRenderer;
                float phraseScale = 1.0f;
                int phraseColor = screen.getHoveredOffer() == i && enabled ? 0xffff80
                        : (enabled ? 0x685e4a : ((0x685e4a & 0xfefefe) >> 1));
                int phraseX = x + Math.round(3 * scale);
                int phraseY = y + Math.round(2 * scale);
                int phraseClipW = Math.max(1, w - Math.round(26 * scale));
                int phraseClipH = Math.max(1, Math.round(EnchantingTableScreen.TEX_OFFER_H * scale) - 4);
                String phrase = fitEnchantOfferPhrase(phraseRenderer, screen.getOfferPhrase(i),
                        phraseClipW, phraseScale);
                drawClippedEnchantText(phraseRenderer, phrase, phraseX + 1, phraseY + 1,
                        phraseScale, colorFromRgb(enabled ? 0x1a160e : 0x0c0a06), phraseX, y + 1,
                        phraseClipW, phraseClipH);
                drawClippedEnchantText(phraseRenderer, phrase, phraseX, phraseY,
                        phraseScale, colorFromRgb(phraseColor), phraseX, y + 1, phraseClipW, phraseClipH);

                String text = String.valueOf(offers[i]);
                float textScale = 1.0f;
                int levelColor = enabled ? 0x80ff20 : 0x407f10;
                int textWidth = textRenderer.getStringWidth(text, textScale);
                int levelX = x + w - Math.round(5 * scale) - textWidth;
                int levelY = y + Math.round(7 * scale);
                textRenderer.drawText(text, levelX + 1, levelY + 1, textScale,
                        colorFromRgb(enabled ? 0x203f08 : 0x102004));
                textRenderer.drawText(text, levelX, levelY, textScale, colorFromRgb(levelColor));
            }
        }
        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
        shader.bind();
        shader.setUniform("projection", ortho);
    }

    private String fitEnchantOfferPhrase(TextRenderer renderer, String phrase, int maxWidth, float scale) {
        if (renderer == null || phrase == null || phrase.isBlank() || maxWidth <= 0) {
            return "";
        }
        String trimmed = phrase.trim();
        if (renderer.getStringWidth(trimmed, scale) <= maxWidth) {
            return trimmed;
        }
        String[] words = trimmed.split("\\s+");
        StringBuilder fitted = new StringBuilder();
        for (String word : words) {
            String candidate = fitted.isEmpty() ? word : fitted + " " + word;
            if (renderer.getStringWidth(candidate, scale) > maxWidth) {
                break;
            }
            fitted.setLength(0);
            fitted.append(candidate);
        }
        if (!fitted.isEmpty()) {
            return fitted.toString();
        }
        String clipped = trimmed;
        while (!clipped.isEmpty() && renderer.getStringWidth(clipped, scale) > maxWidth) {
            clipped = clipped.substring(0, clipped.length() - 1);
        }
        return clipped;
    }

    private void drawClippedEnchantText(TextRenderer renderer, String text, int x, int y, float scale, float[] color,
            int clipX, int clipY, int clipW, int clipH) {
        if (renderer == null || text == null || text.isEmpty() || clipW <= 0 || clipH <= 0) {
            return;
        }
        glEnable(GL_SCISSOR_TEST);
        glScissor(Math.max(0, clipX), Math.max(0, windowHeight - clipY - clipH), clipW, clipH);
        renderer.drawText(text, x, y, scale, color);
        glDisable(GL_SCISSOR_TEST);
    }

    private void drawEnchantOfferRows(EnchantingTableScreen screen, int[] offers, int level,
            int winX, int winY, float scale) {
        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
        if (enchantTexture != null) {
            shader.unbind();
            enchantTexture.bind(0);
            texturedShader.bind();
            texturedShader.setUniform("projection", ortho);
            texturedShader.setUniform("textureSampler", 0);
            texturedShader.setUniform("brightness", 1.0f);
        }

        for (int i = 0; i < offers.length; i++) {
            int x = winX + (int) (EnchantingTableScreen.TEX_OFFER_X * scale);
            int y = winY + (int) ((EnchantingTableScreen.TEX_OFFER_Y + i * EnchantingTableScreen.TEX_OFFER_H) * scale);
            int w = (int) (EnchantingTableScreen.TEX_OFFER_W * scale);
            int h = (int) (EnchantingTableScreen.TEX_OFFER_H * scale);
            int textureV = EnchantingTableScreen.offerTextureV(offers[i], level, screen.getHoveredOffer() == i);
            if (enchantTexture == null) {
                boolean enabled = offers[i] > 0 && level >= offers[i];
                float shade = enabled ? 0.78f : 0.36f;
                drawRect(x, y, w, h, shade * 0.35f, shade * 0.28f, shade * 0.12f, 0.78f);
                continue;
            }
            float u1 = EnchantingTableScreen.TEX_OFFER_U / 256.0f;
            float v1 = textureV / 256.0f;
            float u2 = (EnchantingTableScreen.TEX_OFFER_U + EnchantingTableScreen.TEX_OFFER_W) / 256.0f;
            float v2 = (textureV + EnchantingTableScreen.TEX_OFFER_H) / 256.0f;
            drawTexturedQuad(x, y, x + w, y, x + w, y + h, x, y + h, u1, v1, u2, v2);
        }

        if (enchantTexture != null) {
            texturedShader.unbind();
            enchantTexture.unbind();
            shader.bind();
            shader.setUniform("projection", ortho);
        }
    }

    private float[] colorFromRgb(int color) {
        return new float[] {
                ((color >> 16) & 0xff) / 255.0f,
                ((color >> 8) & 0xff) / 255.0f,
                (color & 0xff) / 255.0f,
                1.0f
        };
    }

    private void renderEnchantSlot(EnchantingTableScreen screen, int slot, int texX, int texY,
            int winX, int winY, int itemOffset, int itemSize, float scale) {
        int x = winX + (int) (texX * scale) + itemOffset;
        int y = winY + (int) (texY * scale) + itemOffset;
        drawScreenItem(screen.getItemInSlot(slot), x, y, itemSize);
        drawHoverIfNeeded(screen.getHoveredSlot(), slot,
                winX + (int) (texX * scale), winY + (int) (texY * scale), EnchantingTableScreen.SLOT_SIZE);
    }

    private void drawScreenItem(ItemStack item, int x, int y, int itemSize) {
        if (item != null && !item.isEmpty()) {
            drawItemIconAt(x, y, itemSize, item);
            if (item.getCount() > 1) {
                drawStackCountAt(x, y, itemSize, item.getCount());
            }
        }
    }

    private void drawHoverIfNeeded(int hoveredSlot, int slot, int slotX, int slotY, int size) {
        if (hoveredSlot == slot) {
            drawRect(slotX + 1, slotY + 1, size - 2, size - 2,
                    HOVER_OVERLAY[0], HOVER_OVERLAY[1], HOVER_OVERLAY[2], HOVER_OVERLAY[3]);
        }
    }

    private interface SlotLookup {
        ItemStack get(int slot);
    }

    private void renderCursorAndTooltip(Inventory inventory, int hoveredSlot, SlotLookup slotLookup, int itemSize) {
        ItemStack cursorItem = inventory.getCursorItem();
        if (cursorItem != null && !cursorItem.isEmpty()) {
            int mx = (int) Input.getMouseX();
            int my = (int) Input.getMouseY();
            drawItemIconAt(mx - itemSize / 2, my - itemSize / 2, itemSize, cursorItem);
            if (cursorItem.getCount() > 1) {
                drawStackCountAt(mx - itemSize / 2, my - itemSize / 2, itemSize, cursorItem.getCount());
            }
        }

        if (hoveredSlot != -1) {
            ItemStack item = slotLookup.get(hoveredSlot);
            if (item != null && !item.isEmpty()) {
                drawTooltip(item, (int) Input.getMouseX(), (int) Input.getMouseY());
            }
        }
    }

    /**
     * Draw an arrow pointing right (for crafting output).
     */
    private void drawArrow(int x, int y) {
        // Simple arrow: ====>
        drawRect(x, y - 2, 15, 4, 0.4f, 0.4f, 0.4f, 1.0f);
        // Arrow head
        drawRect(x + 12, y - 6, 4, 4, 0.4f, 0.4f, 0.4f, 1.0f);
        drawRect(x + 12, y + 2, 4, 4, 0.4f, 0.4f, 0.4f, 1.0f);
        drawRect(x + 16, y - 4, 4, 4, 0.4f, 0.4f, 0.4f, 1.0f);
        drawRect(x + 16, y, 4, 4, 0.4f, 0.4f, 0.4f, 1.0f);
    }

    private void drawSlot(int x, int y, int slotIndex, boolean hovered, ItemStack item) {
        // Slot background
        drawRect(x, y, SLOT_SIZE, SLOT_SIZE, SLOT_BG[0], SLOT_BG[1], SLOT_BG[2], SLOT_BG[3]);

        // Borders (matching hotbar style - uniform grey)
        int innerH = SLOT_SIZE - (BORDER * 2);
        // Top
        drawRect(x, y, SLOT_SIZE, BORDER, SLOT_BORDER[0], SLOT_BORDER[1], SLOT_BORDER[2], SLOT_BORDER[3]);
        // Bottom
        drawRect(x, y + SLOT_SIZE - BORDER, SLOT_SIZE, BORDER, SLOT_BORDER[0], SLOT_BORDER[1], SLOT_BORDER[2],
                SLOT_BORDER[3]);
        // Left
        drawRect(x, y + BORDER, BORDER, innerH, SLOT_BORDER[0], SLOT_BORDER[1], SLOT_BORDER[2], SLOT_BORDER[3]);
        // Right
        drawRect(x + SLOT_SIZE - BORDER, y + BORDER, BORDER, innerH, SLOT_BORDER[0], SLOT_BORDER[1], SLOT_BORDER[2],
                SLOT_BORDER[3]);

        // Item icon
        if (item != null && !item.isEmpty()) {
            drawItemIcon(x, y, item);

            // Draw stack count in bottom-right corner (if > 1)
            if (item.getCount() > 1) {
                drawStackCount(x, y, item.getCount());
            }
        }

        // Hover overlay
        if (hovered) {
            drawRect(x + BORDER, y + BORDER, SLOT_SIZE - BORDER * 2, SLOT_SIZE - BORDER * 2,
                    HOVER_OVERLAY[0], HOVER_OVERLAY[1], HOVER_OVERLAY[2], HOVER_OVERLAY[3]);
        }
    }

    private void drawTooltip(ItemStack item, int mouseX, int mouseY) {
        if (textRenderer == null) {
            return;
        }

        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add(displayName(item));
        for (EnchantmentInstance enchantment : item.getEnchantments()) {
            lines.add(formatEnchantment(enchantment));
        }

        int textWidth = 0;
        for (String line : lines) {
            textWidth = Math.max(textWidth, textRenderer.getStringWidth(line, TOOLTIP_TEXT_SCALE));
        }

        int textHeight = 8;
        if (lines.size() > 1) {
            textHeight += 2 + (lines.size() - 1) * TOOLTIP_LINE_HEIGHT;
        }

        int textX = mouseX + 12;
        int textY = mouseY - 12;
        if (textX + textWidth + 4 > windowWidth) {
            textX = mouseX - 16 - textWidth;
        }
        if (textY + textHeight + 4 > windowHeight) {
            textY = windowHeight - textHeight - 4;
        }
        if (textX < 4) {
            textX = 4;
        }
        if (textY < 4) {
            textY = 4;
        }

        drawClassicTooltipFrame(textX, textY, textWidth, textHeight);

        int lineY = textY;
        for (int i = 0; i < lines.size(); i++) {
            float[] color = i == 0 ? new float[] { 1f, 1f, 1f, 1f }
                    : new float[] { 0.65f, 0.55f, 1.0f, 1f };
            textRenderer.drawText(lines.get(i), textX, lineY, TOOLTIP_TEXT_SCALE, color);
            lineY += TOOLTIP_LINE_HEIGHT;
            if (i == 0 && lines.size() > 1) {
                lineY += 2;
            }
        }
    }

    private void drawClassicTooltipFrame(int textX, int textY, int textWidth, int textHeight) {
        int left = textX - 3;
        int top = textY - 4;
        int width = textWidth + 6;
        int height = textHeight + 8;
        drawArgbRect(left, top, width, height, TOOLTIP_BACKGROUND);
        drawArgbGradientRect(left - 1, top + 1, 1, height - 2, TOOLTIP_BORDER_TOP, TOOLTIP_BORDER_BOTTOM);
        drawArgbGradientRect(left + width, top + 1, 1, height - 2, TOOLTIP_BORDER_TOP, TOOLTIP_BORDER_BOTTOM);
        drawArgbRect(left, top - 1, width, 1, TOOLTIP_BACKGROUND);
        drawArgbRect(left, top + height, width, 1, TOOLTIP_BACKGROUND);
        drawArgbRect(left, top, width, 1, TOOLTIP_BORDER_TOP);
        drawArgbRect(left, top + height - 1, width, 1, TOOLTIP_BORDER_BOTTOM);
    }

    private String displayName(ItemStack item) {
        if (item.getCustomName() != null && !item.getCustomName().isBlank()) {
            return item.getCustomName();
        }
        if (item.getType() == ItemType.POTION) {
            return PotionEffectResolver.displayName(item.getPotionData());
        }
        return item.getType().getDisplayName();
    }

    private String formatEnchantment(EnchantmentInstance enchantment) {
        String[] words = enchantment.type().name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(word.charAt(0)).append(word.substring(1).toLowerCase());
        }
        sb.append(' ').append(roman(enchantment.level()));
        return sb.toString();
    }

    private String roman(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(value);
        };
    }

    /**
     * Draw stack count as a number in the bottom-right of a slot.
     */
    private void drawStackCount(int slotX, int slotY, int count) {
        if (drawBitmapStackCount(slotX, slotY, SLOT_SIZE, SLOT_SIZE, count, 4, 4)) {
            return;
        }
        String countStr = String.valueOf(count);
        int digitWidth = 6;
        int digitHeight = 8;
        int spacing = 1;
        int totalWidth = countStr.length() * (digitWidth + spacing) - spacing;

        // Position in bottom-right corner with some padding
        int baseX = slotX + SLOT_SIZE - totalWidth - 4;
        int baseY = slotY + SLOT_SIZE - digitHeight - 4;

        // Draw shadow first (offset by 1)
        for (int i = 0; i < countStr.length(); i++) {
            int digit = countStr.charAt(i) - '0';
            int x = baseX + i * (digitWidth + spacing) + 1;
            int y = baseY + 1;
            drawDigit(x, y, digit, 0.1f, 0.1f, 0.1f);
        }

        // Draw white digits
        for (int i = 0; i < countStr.length(); i++) {
            int digit = countStr.charAt(i) - '0';
            int x = baseX + i * (digitWidth + spacing);
            drawDigit(x, baseY, digit, 1.0f, 1.0f, 1.0f);
        }
    }

    private boolean drawBitmapStackCount(int slotX, int slotY, int slotWidth, int slotHeight,
            int count, int rightInset, int bottomInset) {
        if (textRenderer == null) {
            return false;
        }
        String countText = String.valueOf(count);
        int textWidth = textRenderer.getStringWidth(countText, 1.0f);
        int x = slotX + slotWidth - textWidth - rightInset;
        int y = slotY + slotHeight - 8 - bottomInset;
        textRenderer.drawText(countText, x + 1, y + 1, 1.0f, new float[] { 0.15f, 0.15f, 0.15f, 1.0f });
        textRenderer.drawText(countText, x, y, 1.0f, new float[] { 1.0f, 1.0f, 1.0f, 1.0f });
        restoreColorShader();
        return true;
    }

    /**
     * Draw a single digit using simple rectangles (7-segment style display).
     */
    private void drawDigit(int x, int y, int digit, float r, float g, float b) {
        // Segment positions: 0=top, 1=topLeft, 2=topRight, 3=middle, 4=bottomLeft,
        // 5=bottomRight, 6=bottom
        // Which segments to draw for each digit
        boolean[][] segments = {
                { true, true, true, false, true, true, true }, // 0
                { false, false, true, false, false, true, false }, // 1
                { true, false, true, true, true, false, true }, // 2
                { true, false, true, true, false, true, true }, // 3
                { false, true, true, true, false, true, false }, // 4
                { true, true, false, true, false, true, true }, // 5
                { true, true, false, true, true, true, true }, // 6
                { true, false, true, false, false, true, false }, // 7
                { true, true, true, true, true, true, true }, // 8
                { true, true, true, true, false, true, true } // 9
        };

        if (digit < 0 || digit > 9)
            return;

        int w = 6, h = 8, t = 2;

        // Segment drawing
        if (segments[digit][0])
            drawRect(x, y, w, t, r, g, b, 1.0f); // Top
        if (segments[digit][1])
            drawRect(x, y, t, h / 2, r, g, b, 1.0f); // Top left
        if (segments[digit][2])
            drawRect(x + w - t, y, t, h / 2, r, g, b, 1.0f); // Top right
        if (segments[digit][3])
            drawRect(x, y + h / 2 - t / 2, w, t, r, g, b, 1.0f); // Middle
        if (segments[digit][4])
            drawRect(x, y + h / 2, t, h / 2, r, g, b, 1.0f); // Bottom left
        if (segments[digit][5])
            drawRect(x + w - t, y + h / 2, t, h / 2, r, g, b, 1.0f); // Bottom right
        if (segments[digit][6])
            drawRect(x, y + h - t, w, t, r, g, b, 1.0f); // Bottom
    }

    private void drawItemIcon(int x, int y, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        drawItemIcon(x, y, stack.getType());
        drawPotionItemOverlay(x + 4, y + 4, SLOT_SIZE - 8, stack);
        drawDynamicItemOverlay(x + 4, y + 4, SLOT_SIZE - 8, stack.getType());
        drawEnchantedItemOverlay(x + 4, y + 4, SLOT_SIZE - 8, stack);
    }

    private void drawItemIcon(int x, int y, ItemType type) {
        // If atlas is available, draw textured icon
        if (atlas != null) {
            int size = SLOT_SIZE - 8;
            if (type.getRenderProfile().modelKind() == com.craftzero.inventory.ItemRenderProfile.ModelKind.BLOCK) {
                // Blocks render as isometric 3D cubes
                drawIsometricBlockIcon(x + 4, y + 4, size, type);
            } else {
                // Items render as flat 2D sprites (like stick)
                drawItemSprite(x + 4, y + 4, size, type);
            }
            return;
        }

        // Fallback to colored squares if no atlas
        float r = 0.5f, g = 0.5f, b = 0.5f;
        switch (type) {
            case GRASS:
                r = 0.2f;
                g = 0.8f;
                b = 0.2f;
                break;
            case DIRT:
                r = 0.55f;
                g = 0.35f;
                b = 0.15f;
                break;
            case STONE:
                r = 0.5f;
                g = 0.5f;
                b = 0.5f;
                break;
            default:
                break;
        }
        int iconPadding = 4;
        int iconSize = SLOT_SIZE - iconPadding * 2;
        drawRect(x + iconPadding, y + iconPadding, iconSize, iconSize, r, g, b, 1.0f);
    }

    private void drawPotionItemOverlay(int x, int y, int size, ItemStack stack) {
        if (!PotionItemVisuals.shouldDrawOverlay(stack)) {
            return;
        }

        float[] color = PotionItemVisuals.liquidColor(stack);
        float bodyLeft = x + size * 0.30f;
        float bodyRight = x + size * 0.70f;
        float bodyTop = y + size * 0.46f;
        float bodyBottom = y + size * 0.82f;
        float shoulderInset = size * 0.05f;
        float[] body = {
                bodyLeft, bodyTop,
                bodyRight, bodyTop,
                bodyRight - shoulderInset, bodyBottom,
                bodyLeft + shoulderInset, bodyBottom
        };
        drawShape(body, 4, color[0], color[1], color[2], 0.78f);

        int neckX = Math.round(x + size * 0.42f);
        int neckY = Math.round(y + size * 0.35f);
        int neckW = Math.max(2, Math.round(size * 0.16f));
        int neckH = Math.max(2, Math.round(size * 0.15f));
        drawRect(neckX, neckY, neckW, neckH, color[0], color[1], color[2], 0.60f);

        int shineW = Math.max(1, Math.round(size * 0.06f));
        int shineH = Math.max(2, Math.round(size * 0.18f));
        drawRect(Math.round(bodyLeft + size * 0.06f), Math.round(bodyTop + size * 0.05f),
                shineW, shineH, 1.0f, 1.0f, 1.0f, 0.22f);

        if (PotionItemVisuals.isSplash(stack)) {
            drawSplashPotionMarker(x, y, size);
        }
    }

    private void drawEnchantedItemOverlay(int x, int y, int size, ItemStack stack) {
        if (!EnchantedItemVisuals.shouldDrawGlint(stack)) {
            return;
        }
        if (drawTexturedEnchantedItemOverlay(x, y, size)) {
            return;
        }

        float[] wash = EnchantedItemVisuals.glintWashColor();
        drawRect(x, y, size, size, wash[0], wash[1], wash[2], wash[3]);

        float[] color = EnchantedItemVisuals.glintColor();
        for (EnchantedItemVisuals.Band band : EnchantedItemVisuals.glintBands(x, y, size)) {
            drawShape(band.copyVertices(), band.vertexCount(), color[0], color[1], color[2], color[3]);
        }
    }

    private boolean drawTexturedEnchantedItemOverlay(int x, int y, int size) {
        Texture glint = GuiTexture.getGlintTexture();
        if (glint == null || size <= 0) {
            return false;
        }

        shader.unbind();
        glint.bind(0);
        texturedShader.bind();
        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
        texturedShader.setUniform("projection", ortho);
        texturedShader.setUniform("textureSampler", 0);
        texturedShader.setUniform("brightness", 1.0f);

        glEnable(GL_SCISSOR_TEST);
        glScissor(x, Math.max(0, windowHeight - y - size), size, size);
        glBlendFunc(GL_SRC_COLOR, GL_ONE);
        for (EnchantedItemVisuals.TexturePass pass : EnchantedItemVisuals.texturePasses(x, y, size)) {
            float[] vertices = pass.copyVertices();
            if (vertices.length >= 8) {
                drawTexturedQuad(
                        vertices[0], vertices[1],
                        vertices[2], vertices[3],
                        vertices[4], vertices[5],
                        vertices[6], vertices[7],
                        pass.u1(), pass.v1(), pass.u2(), pass.v2());
            }
        }
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_SCISSOR_TEST);

        texturedShader.unbind();
        glint.unbind();
        shader.bind();
        shader.setUniform("projection", ortho);
        return true;
    }

    private void drawDynamicItemOverlay(int x, int y, int size, ItemType type) {
        ItemTextureResolver.DynamicItemState state =
                ItemTextureResolver.dynamicItemState(type, dynamicItemWorld, dynamicItemPlayer);
        if (!state.active()) {
            return;
        }

        int centerX = Math.round(x + size * 0.5f);
        int centerY = Math.round(y + size * 0.5f);
        int radius = Math.max(4, Math.round(size * 0.33f));
        int width = Math.max(1, Math.round(size * 0.045f));
        if (type == ItemType.COMPASS) {
            drawDynamicPointer(centerX, centerY, state.angleRadians() + (float) Math.PI,
                    Math.max(2, Math.round(radius * 0.42f)), Math.max(1, width - 1),
                    0.86f, 0.86f, 0.86f, 0.92f);
            drawDynamicPointer(centerX, centerY, state.angleRadians(), radius, width,
                    0.92f, 0.08f, 0.04f, 0.96f);
        } else if (type == ItemType.CLOCK) {
            drawDynamicPointer(centerX, centerY, state.angleRadians(), radius, width,
                    1.0f, 0.83f, 0.25f, 0.95f);
        }

        int hub = Math.max(2, Math.round(size * 0.11f));
        drawRect(centerX - hub / 2, centerY - hub / 2, hub, hub,
                0.08f, 0.08f, 0.08f, 0.90f);
    }

    private void drawDynamicPointer(int centerX, int centerY, float angleRadians, int radius, int halfWidth,
            float r, float g, float b, float a) {
        float sin = (float) Math.sin(angleRadians);
        float cos = (float) Math.cos(angleRadians);
        float startX = centerX - sin * Math.max(1, halfWidth);
        float startY = centerY + cos * Math.max(1, halfWidth);
        float endX = centerX + sin * radius;
        float endY = centerY - cos * radius;
        float perpX = cos * halfWidth;
        float perpY = sin * halfWidth;
        float[] vertices = {
                startX - perpX, startY - perpY,
                startX + perpX, startY + perpY,
                endX + perpX, endY + perpY,
                endX - perpX, endY - perpY
        };
        drawShape(vertices, 4, r, g, b, a);
    }

    private void drawSplashPotionMarker(int x, int y, int size) {
        int marker = Math.max(3, Math.round(size * 0.20f));
        int startX = x + size - marker - Math.max(1, Math.round(size * 0.05f));
        int startY = y + Math.max(1, Math.round(size * 0.07f));
        drawRect(startX, startY, marker, Math.max(1, marker / 3), 0.92f, 0.92f, 1.0f, 0.90f);
        drawRect(startX + marker / 3, startY + marker / 3, marker, Math.max(1, marker / 3),
                0.45f, 0.35f, 0.95f, 0.85f);
        drawRect(startX + marker / 2, startY + marker * 2 / 3, Math.max(1, marker / 3), Math.max(1, marker / 3),
                1.0f, 1.0f, 1.0f, 0.75f);
    }

    /**
     * Draw an item as a flat 2D sprite (for sticks, tools, etc).
     * Uses items.png for items that have defined texture positions there.
     */
    private void drawItemSprite(int x, int y, int size, ItemType type) {
        float[] uv;
        Texture texToUse;

        if (ItemTextureResolver.usesItemsAtlas(type) && itemsTexture != null) {
            uv = ItemTextureResolver.getUv(type);
            texToUse = itemsTexture;
        } else {
            uv = ItemTextureResolver.getUv(type);
            texToUse = atlas;
        }

        if (texToUse == null)
            return;

        uv = insetIconUv(uv);

        shader.unbind();
        texToUse.bind(0);
        texturedShader.bind();
        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
        texturedShader.setUniform("projection", ortho);
        texturedShader.setUniform("textureSampler", 0);
        texturedShader.setUniform("brightness", 1.0f);

        drawTexturedQuad(x, y, x + size, y, x + size, y + size, x, y + size,
                uv[0], uv[1], uv[2], uv[3]);

        texturedShader.unbind();
        texToUse.unbind();
        shader.bind();
        shader.setUniform("projection", ortho);
    }

    /**
     * Draw an isometric 3D block icon using textures from the atlas.
     * Orientation: top corner pointing straight up at 45 degrees.
     */
    private void drawIsometricBlockIcon(int x, int y, int size, ItemType type) {
        float[] topUV = insetIconUv(type.getTextureCoords(0));
        float[] sideUV = insetIconUv(type.getTextureCoords(2));

        float halfW = size * BLOCK_ICON_HALF_WIDTH;
        float quarterH = size * BLOCK_ICON_TOP_HALF_HEIGHT;
        float sideH = size * BLOCK_ICON_SIDE_HEIGHT;

        float cx = x + size * (BLOCK_ICON_CENTER_X + BLOCK_ICON_CENTER_X_BIAS);
        float cy = y + size * BLOCK_ICON_CENTER_Y;

        shader.unbind();
        atlas.bind(0);
        texturedShader.bind();
        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
        texturedShader.setUniform("projection", ortho);
        texturedShader.setUniform("textureSampler", 0);

        // Draw TOP face (diamond shape, brightest) - top corner pointing UP
        texturedShader.setUniform("brightness", BLOCK_ICON_TOP_BRIGHTNESS);
        drawTexturedQuad(
                cx, cy - quarterH, // Top corner (pointing up)
                cx + halfW, cy, // Right corner
                cx, cy + quarterH, // Bottom corner
                cx - halfW, cy, // Left corner
                topUV[0], topUV[1], topUV[2], topUV[3]);

        // Draw LEFT face (parallelogram, medium brightness)
        texturedShader.setUniform("brightness", BLOCK_ICON_LEFT_BRIGHTNESS);
        drawTexturedQuad(
                cx - halfW, cy, // Top-left
                cx, cy + quarterH, // Top-right
                cx, cy + quarterH + sideH, // Bottom-right
                cx - halfW, cy + sideH, // Bottom-left
                sideUV[0], sideUV[1], sideUV[2], sideUV[3]);

        // Draw RIGHT face (parallelogram, darkest)
        texturedShader.setUniform("brightness", BLOCK_ICON_RIGHT_BRIGHTNESS);
        drawTexturedQuad(
                cx, cy + quarterH, // Top-left
                cx + halfW, cy, // Top-right
                cx + halfW, cy + sideH, // Bottom-right
                cx, cy + quarterH + sideH, // Bottom-left
                sideUV[0], sideUV[1], sideUV[2], sideUV[3]);

        texturedShader.unbind();
        atlas.unbind();
        shader.bind();
        shader.setUniform("projection", ortho);
    }

    private static float[] insetIconUv(float[] uv) {
        return new float[] {
                Math.min(uv[0] + ITEM_ICON_UV_INSET, uv[2]),
                Math.min(uv[1] + ITEM_ICON_UV_INSET, uv[3]),
                Math.max(uv[2] - ITEM_ICON_UV_INSET, uv[0]),
                Math.max(uv[3] - ITEM_ICON_UV_INSET, uv[1])
        };
    }

    private void drawTexturedQuad(float x1, float y1, float x2, float y2,
            float x3, float y3, float x4, float y4,
            float u1, float v1, float u2, float v2) {
        float[] vertices = {
                x1, y1, u1, v1,
                x2, y2, u2, v1,
                x3, y3, u2, v2,
                x4, y4, u1, v2
        };

        glBindBuffer(GL_ARRAY_BUFFER, texturedVbo);
        FloatBuffer buffer = MemoryUtil.memAllocFloat(vertices.length);
        buffer.put(vertices).flip();
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);
        MemoryUtil.memFree(buffer);

        glBindVertexArray(texturedVao);
        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
        glBindVertexArray(0);
    }

    /**
     * Draw textured inventory background from inventory.png.
     * Uses the full texture as the background.
     */
    private void drawTexturedInventoryBackground(int x, int y, int width, int height) {
        shader.unbind();
        inventoryTexture.bind(0);
        texturedShader.bind();
        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
        texturedShader.setUniform("projection", ortho);
        texturedShader.setUniform("textureSampler", 0);
        texturedShader.setUniform("brightness", 1.0f);

        // Draw full texture as background
        drawTexturedQuad(x, y, x + width, y, x + width, y + height, x, y + height,
                0.0f, 0.0f, 1.0f, 1.0f);

        texturedShader.unbind();
        inventoryTexture.unbind();
        shader.bind();
        shader.setUniform("projection", ortho);
    }

    /**
     * Draw textured crafting table background from crafting.png.
     * Uses the full texture as the background.
     */
    private void drawTexturedCraftingBackground(int x, int y, int width, int height) {
        shader.unbind();
        craftingTexture.bind(0);
        texturedShader.bind();
        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
        texturedShader.setUniform("projection", ortho);
        texturedShader.setUniform("textureSampler", 0);
        texturedShader.setUniform("brightness", 1.0f);

        // Draw full texture as background
        drawTexturedQuad(x, y, x + width, y, x + width, y + height, x, y + height,
                0.0f, 0.0f, 1.0f, 1.0f);

        texturedShader.unbind();
        craftingTexture.unbind();
        shader.bind();
        shader.setUniform("projection", ortho);
    }

    private void drawRect(int x, int y, int width, int height, float r, float g, float b, float a) {
        if (width <= 0 || height <= 0) {
            return;
        }
        float[] vertices = {
                x, y,
                x + width, y,
                x + width, y + height,
                x, y + height
        };
        drawShape(vertices, 4, r, g, b, a);
    }

    private void restoreColorShader() {
        if (shader == null) {
            return;
        }
        shader.bind();
        shader.setUniform("projection", new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1));
    }

    private void drawArgbRect(int x, int y, int width, int height, int argb) {
        drawRect(x, y, width, height,
                ((argb >> 16) & 0xff) / 255.0f,
                ((argb >> 8) & 0xff) / 255.0f,
                (argb & 0xff) / 255.0f,
                ((argb >>> 24) & 0xff) / 255.0f);
    }

    private void drawArgbGradientRect(int x, int y, int width, int height, int topArgb, int bottomArgb) {
        if (width <= 0 || height <= 0) {
            return;
        }
        for (int row = 0; row < height; row++) {
            float t = height <= 1 ? 0.0f : row / (float) (height - 1);
            drawArgbRect(x, y + row, width, 1, lerpArgb(topArgb, bottomArgb, t));
        }
    }

    private int lerpArgb(int from, int to, float t) {
        int a = Math.round(lerp((from >>> 24) & 0xff, (to >>> 24) & 0xff, t));
        int r = Math.round(lerp((from >> 16) & 0xff, (to >> 16) & 0xff, t));
        int g = Math.round(lerp((from >> 8) & 0xff, (to >> 8) & 0xff, t));
        int b = Math.round(lerp(from & 0xff, to & 0xff, t));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    private void drawRectOutline(int x, int y, int width, int height, float r, float g, float b, float a) {
        float[] vertices = {
                x, y,
                x + width, y,
                x + width, y + height,
                x, y + height
        };
        drawShapeOutline(vertices, 4, r, g, b, a);
    }

    private void drawShape(float[] vertices, int count, float r, float g, float b, float a) {
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer buffer = MemoryUtil.memAllocFloat(vertices.length);
        buffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_DYNAMIC_DRAW);
        MemoryUtil.memFree(buffer);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        shader.setUniform("color", new org.joml.Vector4f(r, g, b, a));
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLE_FAN, 0, count);
        glBindVertexArray(0);
    }

    private void drawShapeOutline(float[] vertices, int count, float r, float g, float b, float a) {
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer buffer = MemoryUtil.memAllocFloat(vertices.length);
        buffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_DYNAMIC_DRAW);
        MemoryUtil.memFree(buffer);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        shader.setUniform("color", new org.joml.Vector4f(r, g, b, a));
        glBindVertexArray(vao);
        glDrawArrays(GL_LINE_LOOP, 0, count);
        glBindVertexArray(0);
    }

    public void cleanup() {
        if (shader != null)
            shader.cleanup();
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}

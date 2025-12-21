package com.craftzero.graphics;

import com.craftzero.engine.Input;
import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.ui.InventoryScreen;
import com.craftzero.ui.CraftingTableScreen;
import com.craftzero.world.BlockType;
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

    private TextRenderer textRenderer;

    // Player model renderer for inventory preview
    private InventoryPlayerRenderer playerRenderer;

    public void setTextRenderer(TextRenderer textRenderer) {
        this.textRenderer = textRenderer;
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
                        "void main() {\n" +
                        "    vec4 texColor = texture(textureSampler, texCoord);\n" +
                        "    if (texColor.a < 0.1) discard;\n" +
                        "    fragColor = vec4(texColor.rgb * brightness, texColor.a);\n" +
                        "}");
        texturedShader.link();
        texturedShader.createUniform("projection");
        texturedShader.createUniform("textureSampler");
        texturedShader.createUniform("brightness");

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
    private Texture craftingTexture; // crafting.png
    private Texture itemsTexture; // items.png for sticks and tools

    public void setGuiTextures(Texture inventory, Texture crafting) {
        this.inventoryTexture = inventory;
        this.craftingTexture = crafting;
    }

    public void setItemsTexture(Texture items) {
        this.itemsTexture = items;
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
                    drawItemIconAt(x, y, itemSize, item.getType());
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
                drawItemIconAt(x, y, itemSize, item.getType());
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
                int slotIndex = 36 + row * InventoryScreen.CRAFTING_COLS + col;
                int x = winX + (int) ((InventoryScreen.TEX_CRAFT_GRID_X + col * InventoryScreen.TEX_SLOT_SIZE) * scale)
                        + itemOffset;
                int y = winY + (int) ((InventoryScreen.TEX_CRAFT_GRID_Y + row * InventoryScreen.TEX_SLOT_SIZE) * scale)
                        + itemOffset;

                ItemStack item = inv.getCraftingGrid()[slotIndex - 36];
                if (item != null && !item.isEmpty()) {
                    drawItemIconAt(x, y, itemSize, item.getType());
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

        // 6. Draw crafting output (slot 40)
        com.craftzero.world.BlockType[] pattern = inv.getCraftingPattern();
        com.craftzero.crafting.CraftingRecipe recipe = com.craftzero.crafting.CraftingRegistry.findRecipe(pattern);
        ItemStack outputItem = recipe != null ? recipe.getOutput() : null;

        int outputX = winX + (int) (InventoryScreen.TEX_CRAFT_OUTPUT_X * scale) + itemOffset;
        int outputY = winY + (int) (InventoryScreen.TEX_CRAFT_OUTPUT_Y * scale) + itemOffset;

        if (outputItem != null && !outputItem.isEmpty()) {
            drawItemIconAt(outputX, outputY, itemSize, outputItem.getType());
            if (outputItem.getCount() > 1) {
                drawStackCountAt(outputX, outputY, itemSize, outputItem.getCount());
            }
        }

        // Output hover highlight
        if (screen.getHoveredSlot() == 40) {
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
            drawItemIconAt(mx - itemSize / 2, my - itemSize / 2, itemSize, cursorItem.getType());
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
            else if (hoveredSlot < 40)
                item = inv.getCraftingGrid()[hoveredSlot - 36];
            else if (hoveredSlot == 40)
                item = outputItem;

            if (item != null && !item.isEmpty()) {
                drawTooltip(item, (int) Input.getMouseX(), (int) Input.getMouseY());
            }
        }

        shader.unbind();

        // Restore GL state
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }

    /**
     * Draw an item icon at an exact position with specified size.
     */
    private void drawItemIconAt(int x, int y, int size, com.craftzero.world.BlockType type) {
        if (atlas != null) {
            if (type.isItem()) {
                drawItemSprite(x, y, size, type);
            } else {
                // Same as hotbar - use full size
                drawIsometricBlockIcon(x, y, size, type);
            }
        }
    }

    /**
     * Draw stack count at an exact position.
     */
    private void drawStackCountAt(int slotX, int slotY, int size, int count) {
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
                    drawItemIconAt(x, y, itemSize, item.getType());
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
                .findRecipe3x3(getCraftingPattern(craftingGrid));
        ItemStack outputItem = (recipe != null) ? recipe.getOutput() : null;

        int outputX = winX + (int) (CraftingTableScreen.TEX_CRAFT_OUTPUT_X * scale) + itemOffset;
        int outputY = winY + (int) (CraftingTableScreen.TEX_CRAFT_OUTPUT_Y * scale) + itemOffset;

        if (outputItem != null && !outputItem.isEmpty()) {
            drawItemIconAt(outputX, outputY, itemSize, outputItem.getType());
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
                    drawItemIconAt(x, y, itemSize, item.getType());
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
                drawItemIconAt(x, y, itemSize, item.getType());
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
            drawItemIconAt(mx - itemSize / 2, my - itemSize / 2, itemSize, cursorItem.getType());
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

    /**
     * Helper to get crafting pattern from grid.
     */
    private BlockType[] getCraftingPattern(ItemStack[] grid) {
        BlockType[] pattern = new BlockType[9];
        for (int i = 0; i < 9; i++) {
            pattern[i] = (grid[i] != null && !grid[i].isEmpty()) ? grid[i].getType() : null;
        }
        return pattern;
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
            drawItemIcon(x, y, item.getType());

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

    // NEW: Tooltip Drawing
    private void drawTooltip(ItemStack item, int mouseX, int mouseY) {
        if (textRenderer == null)
            return;

        String rawName = item.getType().toString();
        // Format name
        String[] words = rawName.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (sb.length() > 0)
                sb.append(" ");
            sb.append(word.charAt(0)).append(word.substring(1).toLowerCase());
        }
        String name = sb.toString();

        float scale = 0.5f;
        int textWidth = textRenderer.getStringWidth(name, scale);
        int padding = 4;

        // Use taller box for comfort (Size 32 scaled to 16px)
        int boxHeight = 24;
        int boxWidth = textWidth + padding * 2;

        int boxX = mouseX + 10;
        int boxY = mouseY - boxHeight - 5; // Position above cursor

        // Draw Tooltip Background
        drawRect(boxX, boxY, boxWidth, boxHeight, 0.1f, 0.0f, 0.1f, 0.9f);

        // Draw Border
        drawRectOutline(boxX, boxY, boxWidth, boxHeight, 0.3f, 0.0f, 0.8f, 1.0f);

        // Draw Text - align vertically
        // Compensate for likely TextRenderer V-flip offset (visual shift)
        // Passing boxY - 2 results in visual text appearing centered.
        textRenderer.drawText(name, boxX + padding, boxY - 2, scale, new float[] { 1f, 1f, 1f, 1f });
    }

    /**
     * Draw stack count as a number in the bottom-right of a slot.
     * Uses simple digit rendering with small rectangles.
     */
    private void drawStackCount(int slotX, int slotY, int count) {
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

    private void drawItemIcon(int x, int y, BlockType type) {
        // If atlas is available, draw textured icon
        if (atlas != null) {
            int size = SLOT_SIZE - 8;
            if (type.isItem()) {
                // Items render as flat 2D sprites (like stick)
                drawItemSprite(x + 4, y + 4, size, type);
            } else {
                // Blocks render as isometric 3D cubes
                drawIsometricBlockIcon(x + 4, y + 4, size, type);
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

    /**
     * Draw an item as a flat 2D sprite (for sticks, tools, etc).
     * Uses items.png for items that have defined texture positions there.
     */
    private void drawItemSprite(int x, int y, int size, BlockType type) {
        float[] uv;
        Texture texToUse;

        // Check if this item uses items.png
        if (type.usesItemTexture() && itemsTexture != null) {
            int[] pos = type.getItemTexturePos();
            uv = GuiTexture.getItemUV(pos[0], pos[1]);
            texToUse = itemsTexture;
        } else {
            // Fallback to terrain atlas
            uv = type.getTextureCoords(2);
            texToUse = atlas;
        }

        if (texToUse == null)
            return;

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
    private void drawIsometricBlockIcon(int x, int y, int size, BlockType type) {
        float[] topUV = type.getTextureCoords(0);
        float[] sideUV = type.getTextureCoords(2);

        // Minecraft-style isometric proportions - slightly compact for inventory
        float halfW = size * 0.46f; // Tiny bit narrower to prevent clipping
        float quarterH = size * 0.23f; // Height of top diamond
        float sideH = size * 0.52f; // Slightly taller side faces

        // Center point of the icon - centered in slot
        float cx = x + size * 0.5f - 1; // Shift left by 1px to center
        float cy = y + size * 0.22f; // Shift up to prevent bottom clipping

        shader.unbind();
        atlas.bind(0);
        texturedShader.bind();
        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
        texturedShader.setUniform("projection", ortho);
        texturedShader.setUniform("textureSampler", 0);

        // Draw TOP face (diamond shape, brightest) - top corner pointing UP
        texturedShader.setUniform("brightness", 1.0f);
        drawTexturedQuad(
                cx, cy - quarterH, // Top corner (pointing up)
                cx + halfW, cy, // Right corner
                cx, cy + quarterH, // Bottom corner
                cx - halfW, cy, // Left corner
                topUV[0], topUV[1], topUV[2], topUV[3]);

        // Draw LEFT face (parallelogram, medium brightness)
        texturedShader.setUniform("brightness", 0.6f);
        drawTexturedQuad(
                cx - halfW, cy, // Top-left
                cx, cy + quarterH, // Top-right
                cx, cy + quarterH + sideH, // Bottom-right
                cx - halfW, cy + sideH, // Bottom-left
                sideUV[0], sideUV[1], sideUV[2], sideUV[3]);

        // Draw RIGHT face (parallelogram, darkest)
        texturedShader.setUniform("brightness", 0.45f);
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
        float[] vertices = {
                x, y,
                x + width, y,
                x + width, y + height,
                x, y + height
        };
        drawShape(vertices, 4, r, g, b, a);
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

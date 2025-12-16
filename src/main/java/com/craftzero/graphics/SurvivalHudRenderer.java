package com.craftzero.graphics;

import com.craftzero.engine.Window;
import com.craftzero.main.PlayerStats;
import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.world.BlockType;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Survival HUD renderer - draws colored hearts, hunger icons, and hotbar
 */
public class SurvivalHudRenderer {

    private ShaderProgram shader;
    private int vao, vbo;

    // Textured shader for isometric block icons
    private ShaderProgram texturedShader;
    private int texturedVao, texturedVbo;
    private Texture atlas;

    private int windowWidth, windowHeight;

    // Standard Minecraft Size (Hearts/Hunger)
    private static final int ICON_SIZE = 18;
    private static final int SPACING = 20;

    // Hotbar: Contiguous 9 slots matching Top Row Width (432px)
    // 432px / 9 slots = 48px per slot exactly.
    private static final int HOTBAR_SLOT_SIZE = 48;
    private static final int HOTBAR_SPACING = 48; // No gaps
    private static final int HOTBAR_WIDTH = 9 * HOTBAR_SPACING;

    private TextRenderer textRenderer;

    // Item Name Animation State
    private String currentItemName = "";
    private float animationTime = 0f;
    private int lastSelectedSlot = -1;
    private static final float FADE_IN_DURATION = 0.4f;
    private static final float STAY_DURATION = 1.5f;
    private static final float FADE_OUT_DURATION = 0.4f;
    private static final float TOTAL_DURATION = FADE_IN_DURATION + STAY_DURATION + FADE_OUT_DURATION;

    public void init(Window window) throws Exception {
        this.windowWidth = window.getWidth();
        this.windowHeight = window.getHeight();

        // Simple color shader (for hearts, hunger, borders)
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

        // Create VAO/VBO for textured drawing (pos + uv = 4 floats per vertex)
        texturedVao = glGenVertexArrays();
        texturedVbo = glGenBuffers();

        glBindVertexArray(texturedVao);
        glBindBuffer(GL_ARRAY_BUFFER, texturedVbo);
        glBufferData(GL_ARRAY_BUFFER, 64 * Float.BYTES, GL_DYNAMIC_DRAW); // 16 vertices * 4 floats
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        System.out.println("SurvivalHudRenderer initialized - Window: " + windowWidth + "x" + windowHeight);
    }

    public void setAtlas(Texture atlas) {
        this.atlas = atlas;
    }

    public void updateOrtho(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
    }

    public void setTextRenderer(TextRenderer textRenderer) {
        this.textRenderer = textRenderer;
    }

    public void render(PlayerStats stats, Inventory inventory, float deltaTime) {
        // Animation Logic for Item Name
        int currentSlot = inventory.getSelectedSlot();
        if (currentSlot != lastSelectedSlot) {
            lastSelectedSlot = currentSlot;
            ItemStack item = inventory.getHotbar()[currentSlot];
            if (item != null) {
                // Formatting name: convert ENUM_CONST to Title Case
                String rawName = item.getType().toString();
                String[] words = rawName.split("_");
                StringBuilder sb = new StringBuilder();
                for (String word : words) {
                    if (sb.length() > 0)
                        sb.append(" ");
                    sb.append(word.charAt(0)).append(word.substring(1).toLowerCase());
                }
                currentItemName = sb.toString();
                animationTime = 0f;
            } else {
                currentItemName = ""; // Reset if empty slot
            }
        }

        if (!currentItemName.isEmpty()) {
            animationTime += deltaTime;
        }

        // FORCE fresh state
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);

        // Use screen-space ortho projection
        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);

        shader.bind();
        shader.setUniform("projection", ortho);

        // Minecraft-style centered positioning
        int centerX = windowWidth / 2;

        // Repositioned above large hotbar
        // Hotbar Top is ~Y-60. Hearts (18px) + padding -> Y-90
        int bottomY = windowHeight - 90;

        // Hearts go LEFT from just left of center
        int heartStartX = centerX - 216;
        drawHearts(stats, heartStartX, bottomY);

        // Hunger goes RIGHT from just right of center
        int hungerStartX = centerX + 18;
        drawHungerRight(stats, hungerStartX, bottomY);

        // Hotbar goes BELOW hearts/hunger
        int hotbarY = windowHeight - 60; // 10px from bottom edge
        drawHotbar(inventory, centerX, hotbarY);

        shader.unbind();

        // Render Item Name with Animation
        if (!currentItemName.isEmpty() && animationTime < TOTAL_DURATION && textRenderer != null) {
            float alpha = 1.0f;
            if (animationTime < FADE_IN_DURATION) {
                alpha = animationTime / FADE_IN_DURATION;
            } else if (animationTime > FADE_IN_DURATION + STAY_DURATION) {
                float fadeOutTime = animationTime - (FADE_IN_DURATION + STAY_DURATION);
                alpha = 1.0f - (fadeOutTime / FADE_OUT_DURATION);
            }

            if (alpha > 0) {
                float textScale = 0.5f; // Adjust scale for pixel look
                int textWidth = textRenderer.getStringWidth(currentItemName, textScale);
                float textX = (windowWidth - textWidth) / 2.0f;
                float textY = windowHeight - 90;

                // Draw shadow
                textRenderer.drawText(currentItemName, textX + 2, textY + 2, textScale,
                        new float[] { 0f, 0f, 0f, alpha });
                // Draw Text
                textRenderer.drawText(currentItemName, textX, textY, textScale, new float[] { 1f, 1f, 1f, alpha });
            }
        }

        // Restore state
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }

    private void drawHotbar(Inventory inventory, int centerX, int y) {
        if (inventory == null)
            return;

        int startX = centerX - (HOTBAR_WIDTH / 2);
        ItemStack[] items = inventory.getHotbar();
        int selected = inventory.getSelectedSlot();

        // 1. Draw 9 contiguous slots
        for (int i = 0; i < 9; i++) {
            int slotX = startX + (i * HOTBAR_SPACING);

            // 1. Background fill (Match Inventory Grey)
            drawSimpleSquare(slotX, y, HOTBAR_SLOT_SIZE, 0.55f, 0.55f, 0.55f, 1.0f);

            // 2. Draw item icon if exists (DRAW BEHIND BORDERS)
            ItemStack item = items[i];
            if (item != null && !item.isEmpty()) {
                // Remove +4 offset to center correctly (internal +2 offset handles padding)
                drawItemIcon(slotX, y, item.getType());

                // Draw stack count (if > 1)
                if (item.getCount() > 1) {
                    drawStackCount(slotX, y, item.getCount());
                }
            }

            // 3. Borders (DRAW ON TOP) - Uniform Light Grey (User Request)
            int border = 4; // Thicker grid
            int innerH = HOTBAR_SLOT_SIZE - (border * 2);

            // User requested "all around 4 corners light grey"
            // Using a bright light grey (0.75f) for all sides

            // Top Bar (Full Width)
            drawRect(slotX, y, HOTBAR_SLOT_SIZE, border, 0.75f, 0.75f, 0.75f, 1.0f);

            // Bottom Bar (Full Width)
            drawRect(slotX, y + HOTBAR_SLOT_SIZE - border, HOTBAR_SLOT_SIZE, border, 0.75f, 0.75f, 0.75f, 1.0f);

            // Left Bar (Inner Height)
            drawRect(slotX, y + border, border, innerH, 0.75f, 0.75f, 0.75f, 1.0f);

            // Right Bar (Inner Height)
            drawRect(slotX + HOTBAR_SLOT_SIZE - border, y + border, border, innerH, 0.75f, 0.75f, 0.75f, 1.0f);
        }

        // 2. Draw Selection Frame (Refined: Thickness 5, Padding 0)
        // Manual 4-rect drawing to ensure thickness works
        int selX = startX + (selected * HOTBAR_SPACING);
        int thickness = 5;
        int innerSize = HOTBAR_SLOT_SIZE;

        // White Selection Frame
        // Top Bar
        drawRect(selX - thickness, y - thickness, innerSize + (thickness * 2), thickness, 1.0f, 1.0f, 1.0f, 1.0f);
        // Bottom Bar
        drawRect(selX - thickness, y + innerSize, innerSize + (thickness * 2), thickness, 1.0f, 1.0f, 1.0f, 1.0f);
        // Left Bar
        drawRect(selX - thickness, y, thickness, innerSize, 1.0f, 1.0f, 1.0f, 1.0f);
        // Right Bar
        drawRect(selX + innerSize, y, thickness, innerSize, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void drawRect(int x, int y, int w, int h, float r, float g, float b, float a) {
        float[] vertices = {
                x, y,
                x + w, y,
                x + w, y + h,
                x, y + h
        };
        drawShape(vertices, 4, r, g, b, a);
    }

    private void drawItemIcon(int x, int y, BlockType type) {
        // If atlas is available, draw textured icon
        if (atlas != null) {
            if (type.isItem()) {
                // Items render as flat 2D sprites (like stick)
                drawItemSprite(x + 4, y + 4, HOTBAR_SLOT_SIZE - 8, type);
            } else {
                // Blocks render as isometric 3D cubes
                drawIsometricBlockIcon(x + 4, y + 4, HOTBAR_SLOT_SIZE - 8, type);
            }
            return;
        }

        // Fallback to colored squares if no atlas
        float r = 0.5f, g = 0.5f, b = 0.5f;
        switch (type) {
            default:
                r = 0.5f;
                g = 0.5f;
                b = 0.5f;
                break;
        }
        drawSimpleSquare(x + 2, y + 2, HOTBAR_SLOT_SIZE - 4, r, g, b, 1.0f);
    }

    /**
     * Draw an item as a flat 2D sprite (for sticks, etc).
     */
    private void drawItemSprite(int x, int y, int size, BlockType type) {
        float[] uv = type.getTextureCoords(2); // Use side texture for items

        shader.unbind();
        atlas.bind(0);
        texturedShader.bind();
        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
        texturedShader.setUniform("projection", ortho);
        texturedShader.setUniform("textureSampler", 0);
        texturedShader.setUniform("brightness", 1.0f);

        // Simple square sprite
        drawTexturedQuad(x, y, x + size, y, x + size, y + size, x, y + size,
                uv[0], uv[1], uv[2], uv[3]);

        texturedShader.unbind();
        atlas.unbind();
        shader.bind();
        shader.setUniform("projection", ortho);
    }

    /**
     * Draw an isometric 3D block icon using textures from the atlas.
     * Shows 3 faces: top, left side, right side (like Minecraft inventory).
     */
    private void drawIsometricBlockIcon(int x, int y, int size, BlockType type) {
        // Get UVs for each face
        float[] topUV = type.getTextureCoords(0); // Top face
        float[] sideUV = type.getTextureCoords(2); // Side face

        // Standard isometric cube proportions
        float halfW = size * 0.5f;
        float topHeight = size * 0.25f; // Top diamond
        float sideHeight = size * 0.62f; // Taller side faces

        // Center point
        float cx = x + halfW;
        float topY = y + topHeight * 0.5f; // Top of the top face

        shader.unbind();
        atlas.bind(0);
        texturedShader.bind();
        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
        texturedShader.setUniform("projection", ortho);
        texturedShader.setUniform("textureSampler", 0);

        // Draw TOP face (diamond shape, brightest)
        texturedShader.setUniform("brightness", 1.0f);
        float topCenter = topY + topHeight * 0.5f;
        drawTexturedQuad(
                cx, topY, // Top
                cx + halfW, topCenter, // Right
                cx, topY + topHeight, // Bottom
                cx - halfW, topCenter, // Left
                topUV[0], topUV[1], topUV[2], topUV[3]);

        // Draw LEFT face (parallelogram, medium brightness)
        texturedShader.setUniform("brightness", 0.65f);
        float sideTop = topY + topHeight;
        float sideBottom = sideTop + sideHeight;
        drawTexturedQuad(
                cx - halfW, topCenter, // Top-left
                cx, sideTop, // Top-right
                cx, sideBottom, // Bottom-right
                cx - halfW, sideBottom - topHeight * 0.5f, // Bottom-left
                sideUV[0], sideUV[1], sideUV[2], sideUV[3]);

        // Draw RIGHT face (parallelogram, darkest)
        texturedShader.setUniform("brightness", 0.5f);
        drawTexturedQuad(
                cx, sideTop, // Top-left
                cx + halfW, topCenter, // Top-right
                cx + halfW, sideBottom - topHeight * 0.5f, // Bottom-right
                cx, sideBottom, // Bottom-left
                sideUV[0], sideUV[1], sideUV[2], sideUV[3]);

        texturedShader.unbind();
        atlas.unbind();

        // Rebind color shader for subsequent draws
        shader.bind();
        shader.setUniform("projection", ortho);
    }

    /**
     * Draw a textured quad with 4 vertices and UV coordinates.
     */
    private void drawTexturedQuad(float x1, float y1, float x2, float y2,
            float x3, float y3, float x4, float y4,
            float u1, float v1, float u2, float v2) {
        // Vertices: position (x, y) + texcoord (u, v)
        float[] vertices = {
                x1, y1, u1, v1, // Top
                x2, y2, u2, v1, // Right
                x3, y3, u2, v2, // Bottom
                x4, y4, u1, v2 // Left
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

    private void drawSimpleSquareOutline(int x, int y, int size, float r, float g, float b, float a, float thickness) {
        float[] vertices = {
                x, y,
                x + size, y,
                x + size, y + size,
                x, y + size
        };
        drawShapeOutline(vertices, 4, r, g, b, a);
    }

    private void drawSimpleSquare(int x, int y, int size, float r, float g, float b, float a) {
        float[] vertices = {
                x, y,
                x + size, y,
                x + size, y + size,
                x, y + size
        };
        drawShape(vertices, 4, r, g, b, a);
    }

    private void drawHearts(PlayerStats stats, int startX, int y) {
        int fullHearts = stats.getFullHearts();
        boolean hasHalf = stats.hasHalfHeart();

        for (int i = 0; i < 10; i++) {
            int x = startX + (i * SPACING);

            // Choose color based on health
            float r, g, b, a;
            if (i < fullHearts) {
                r = 1.0f;
                g = 0.0f;
                b = 0.0f;
                a = 1.0f; // Full red
            } else if (i == fullHearts && hasHalf) {
                r = 1.0f;
                g = 0.5f;
                b = 0.5f;
                a = 1.0f; // Pink - HALF HEART
            } else {
                r = 0.4f;
                g = 0.0f;
                b = 0.0f;
                a = 0.8f; // Dark red - EMPTY
            }

            drawSquare(x, y, ICON_SIZE, r, g, b, a);
        }
    }

    private void drawHungerRight(PlayerStats stats, int startX, int y) {
        int fullBars = stats.getFullHungerBars();
        boolean hasHalf = stats.hasHalfHungerBar();

        for (int i = 0; i < 10; i++) {
            // Render from LEFT to RIGHT
            int x = startX + (i * SPACING);

            // Choose color based on hunger
            float r, g, b, a;
            if (i < fullBars) {
                r = 1.0f;
                g = 0.7f;
                b = 0.0f;
                a = 1.0f; // Orange - FULL
            } else if (i == fullBars && hasHalf) {
                r = 1.0f;
                g = 0.85f;
                b = 0.5f;
                a = 1.0f; // Light orange - HALF
            } else {
                r = 0.4f;
                g = 0.3f;
                b = 0.0f;
                a = 0.8f; // Dark orange - EMPTY
            }

            drawSquare(x, y, ICON_SIZE, r, g, b, a);
        }
    }

    private void drawSquare(int x, int y, int size, float r, float g, float b, float a) {
        // FIXED: Detect half hearts correctly!
        boolean isFullHeart = (r > 0.9f && g < 0.1f && b < 0.1f);
        boolean isHalfHeart = (r > 0.9f && g > 0.4f && g < 0.6f && b > 0.4f);
        boolean isEmptyHeart = (r < 0.5f && g < 0.1f && b < 0.1f);

        if (isFullHeart || isHalfHeart || isEmptyHeart) {
            // HEART rendering
            if (isEmptyHeart) {
                drawStunningHeartOutline(x, y, size, 0.2f, 0.0f, 0.0f, 1.0f);
            } else if (isHalfHeart) {
                drawStunningHalfHeart(x, y, size);
            } else {
                drawStunningHeart(x, y, size, 0.94f, 0.22f, 0.22f, 1.0f); // Bright Minecraft red!
                drawStunningHeartOutline(x, y, size, 0.0f, 0.0f, 0.0f, 0.7f);
            }
        } else {
            // DRUMSTICK rendering
            boolean isEmpty = (r < 0.5f);
            boolean isHalf = (g > 0.8f);

            if (isEmpty) {
                drawTiltedDrumstickOutline(x, y, size, 0.3f, 0.2f, 0.1f, 1.0f);
            } else if (isHalf) {
                drawTiltedDrumstickSimple(x, y, size, 0.9f, 0.7f, 0.5f, 1.0f);
                drawTiltedDrumstickOutline(x, y, size, 0.3f, 0.2f, 0.1f, 0.8f);
            } else {
                drawTiltedDrumstick(x, y, size);
            }
        }
    }

    private void drawStunningHeart(int x, int y, int size, float r, float g, float b, float a) {
        // SUPER smooth Minecraft heart with MANY vertices for perfect curves
        float s = size / 2f;
        float[] vertices = {
                x, y + s * 0.92f, // 1. Bottom point
                x - s * 0.3f, y + s * 0.5f, // 2. Left bottom curve start
                x - s * 0.6f, y + s * 0.25f, // 3.
                x - s * 0.85f, y + s * 0.05f, // 4. Left bottom curve
                x - s * 0.95f, y - s * 0.15f, // 5. Left side
                x - s * 0.98f, y - s * 0.4f, // 6.
                x - s * 0.95f, y - s * 0.65f, // 7. Left side top
                x - s * 0.8f, y - s * 0.88f, // 8. Left top curve
                x - s * 0.5f, y - s * 0.98f, // 9.
                x - s * 0.2f, y - s * 0.95f, // 10. Left lobe top
                x, y - s * 0.7f, // 11. Center dip
                x + s * 0.2f, y - s * 0.95f, // 12. Right lobe top
                x + s * 0.5f, y - s * 0.98f, // 13.
                x + s * 0.8f, y - s * 0.88f, // 14. Right top curve
                x + s * 0.95f, y - s * 0.65f, // 15. Right side top
                x + s * 0.98f, y - s * 0.4f, // 16.
                x + s * 0.95f, y - s * 0.15f, // 17. Right side
                x + s * 0.85f, y + s * 0.05f, // 18. Right bottom curve
                x + s * 0.6f, y + s * 0.25f, // 19.
                x + s * 0.3f, y + s * 0.5f // 20. Right bottom curve end
        };
        drawShape(vertices, 20, r, g, b, a);
    }

    private void drawStunningHeartOutline(int x, int y, int size, float r, float g, float b, float a) {
        float s = size / 2f;
        float[] vertices = {
                x, y + s * 0.92f,
                x - s * 0.3f, y + s * 0.5f,
                x - s * 0.6f, y + s * 0.25f,
                x - s * 0.85f, y + s * 0.05f,
                x - s * 0.95f, y - s * 0.15f,
                x - s * 0.98f, y - s * 0.4f,
                x - s * 0.95f, y - s * 0.65f,
                x - s * 0.8f, y - s * 0.88f,
                x - s * 0.5f, y - s * 0.98f,
                x - s * 0.2f, y - s * 0.95f,
                x, y - s * 0.7f,
                x + s * 0.2f, y - s * 0.95f,
                x + s * 0.5f, y - s * 0.98f,
                x + s * 0.8f, y - s * 0.88f,
                x + s * 0.95f, y - s * 0.65f,
                x + s * 0.98f, y - s * 0.4f,
                x + s * 0.95f, y - s * 0.15f,
                x + s * 0.85f, y + s * 0.05f,
                x + s * 0.6f, y + s * 0.25f,
                x + s * 0.3f, y + s * 0.5f
        };
        drawShapeOutline(vertices, 20, r, g, b, a);
    }

    private void drawStunningHalfHeart(int x, int y, int size) {
        float s = size / 2f;
        // LEFT HALF filled in BRIGHT red
        float[] leftHalf = {
                x, y + s * 0.92f,
                x - s * 0.3f, y + s * 0.5f,
                x - s * 0.6f, y + s * 0.25f,
                x - s * 0.85f, y + s * 0.05f,
                x - s * 0.95f, y - s * 0.15f,
                x - s * 0.98f, y - s * 0.4f,
                x - s * 0.95f, y - s * 0.65f,
                x - s * 0.8f, y - s * 0.88f,
                x - s * 0.5f, y - s * 0.98f,
                x - s * 0.2f, y - s * 0.95f,
                x, y - s * 0.7f
        };
        drawShape(leftHalf, 11, 0.94f, 0.22f, 0.22f, 1.0f); // Bright red!

        // Draw full outline
        drawStunningHeartOutline(x, y, size, 0.0f, 0.0f, 0.0f, 0.8f);
    }

    private void drawTiltedDrumstick(int x, int y, int size) {
        // Minecraft drumstick - 45 DEGREE ROTATION (Bone Down-Right, Meat Up-Left)
        float s = size / 2f;

        // Brown meat body - Rotated 45 degrees left
        float[] meatBody = {
                x - s * 0.99f, y - s * 0.21f, // Top left corner (was -0.55, -0.85)
                x - s * 0.92f, y - s * 0.42f, // Top narrow (was -0.35, -0.95)
                x - s * 0.69f, y - s * 0.69f, // Top center (was 0, -0.98)
                x - s * 0.42f, y - s * 0.92f, // Top narrow (was 0.35, -0.95)
                x - s * 0.21f, y - s * 0.99f, // Top right corner (was 0.55, -0.85)
                x + s * 0.02f, y - s * 0.94f, // Right side (was 0.68, -0.65)
                x + s * 0.23f, y - s * 0.79f, // (0.72, -0.4) -> 0.32*0.7=0.22, (-0.4-0.72)=-1.12*0.7=-0.79
                x + s * 0.39f, y - s * 0.60f, // (0.7, -0.15) -> 0.55*0.7=0.385, (-0.15-0.7)=-0.85*0.7=-0.595
                x + s * 0.47f, y - s * 0.40f, // (0.62, 0.05) -> 0.67*0.7=0.469, (0.05-0.62)=-0.57*0.7=-0.399
                x + s * 0.49f, y - s * 0.18f, // (0.48, 0.22) -> 0.7*0.7=0.49, (0.22-0.48)=-0.26*0.7=-0.18
                x + s * 0.46f, y + s * 0.04f, // Meat narrows (0.3, 0.35) -> 0.65*0.7=0.455, (0.35-0.3)=0.05*0.7=0.035
                x + s * 0.40f, y + s * 0.19f, // (0.15, 0.42) -> 0.57*0.7=0.399, (0.42-0.15)=0.27*0.7=0.189
                x + s * 0.32f, y + s * 0.32f, // Bottom center (0, 0.45) -> 0.318, 0.318
                x + s * 0.19f, y + s * 0.40f, // (-0.15, 0.42) -> 0.27*0.7=0.189, (0.42--0.15)=0.57*0.7=0.399
                x + s * 0.04f, y + s * 0.46f, // (-0.3, 0.35) -> 0.05*0.7=0.035, (0.35--0.3)=0.65*0.7=0.455
                x - s * 0.18f, y + s * 0.49f, // (-0.48, 0.22) -> -0.26*0.7=-0.182, (0.22--0.48)=0.7*0.7=0.49
                x - s * 0.40f, y + s * 0.47f, // (-0.62, 0.05) -> -0.57*0.7=-0.399, (0.05--0.62)=0.67*0.7=0.469
                x - s * 0.60f, y + s * 0.39f, // (-0.7, -0.15) -> -0.85*0.7=-0.595, (-0.15--0.7)=0.55*0.7=0.385
                x - s * 0.79f, y + s * 0.23f, // (-0.72, -0.4) -> -1.12*0.7=-0.784, (-0.4--0.72)=0.32*0.7=0.224
                x - s * 0.94f, y + s * 0.02f // Left side (-0.68, -0.65) -> -1.33*0.7=-0.931,
                                             // (-0.65--0.68)=0.03*0.7=0.02
        };
        drawShape(meatBody, 20, 0.78f, 0.49f, 0.35f, 1.0f);

        // White bone at BOTTOM - Rotated 45 degrees left
        float[] bone = {
                x + s * 0.11f, y + s * 0.39f, // Start
                x + s * 0.23f, y + s * 0.45f,
                x + s * 0.35f, y + s * 0.71f,
                x + s * 0.35f, y + s * 0.85f, // Left knob
                x + s * 0.40f, y + s * 0.95f,
                x + s * 0.48f, y + s * 0.98f,
                x + s * 0.67f, y + s * 0.67f, // Bottom
                x + s * 0.92f, y + s * 0.54f, // Right knob bottom
                x + s * 0.95f, y + s * 0.45f,
                x + s * 0.85f, y + s * 0.35f, // Right knob top
                x + s * 0.71f, y + s * 0.35f,
                x + s * 0.45f, y + s * 0.23f,
                x + s * 0.39f, y + s * 0.11f // Connection back
        };
        drawShape(bone, 13, 0.98f, 0.98f, 0.98f, 1.0f);

        // Red/dark meat highlights - Rotated 45 degrees left
        float[] meatHighlight = {
                x - s * 0.81f, y - s * 0.25f,
                x - s * 0.60f, y - s * 0.60f,
                x - s * 0.25f, y - s * 0.81f,
                x + s * 0.04f, y - s * 0.74f,
                x + s * 0.27f, y - s * 0.55f,
                x + s * 0.34f, y - s * 0.34f,
                x + s * 0.28f, y - s * 0.07f,
                x + s * 0.16f, y + s * 0.16f,
                x - s * 0.07f, y + s * 0.28f,
                x - s * 0.34f, y + s * 0.34f,
                x - s * 0.74f, y + s * 0.04f
        };
        drawShape(meatHighlight, 11, 0.92f, 0.28f, 0.28f, 1.0f);

        // Dark outline - Rotated 45 degrees left
        drawTiltedDrumstickOutline(x, y, size, 0.22f, 0.12f, 0.06f, 0.95f);
    }

    private void drawTiltedDrumstickSimple(int x, int y, int size, float r, float g, float b, float a) {
        float s = size / 2f;
        // Simple version - 45 DEGREE ROTATION
        float[] vertices = {
                x - s * 0.92f, y - s * 0.28f, // (-0.45, -0.85) -> -1.3*0.7=-0.91, -0.4*0.7=-0.28
                x - s * 0.28f, y - s * 0.92f, // (0.45, -0.85) -> -0.4*0.7=-0.28, -1.3*0.7=-0.91
                x + s * 0.18f, y - s * 0.67f, // (0.6, -0.35) -> 0.25*0.7=0.175, -0.95*0.7=-0.665
                x + s * 0.46f, y - s * 0.25f, // (0.5, 0.15) -> 0.65*0.7=0.455, -0.35*0.7=-0.245
                x + s * 0.71f, y + s * 0.35f, // (0.25, 0.75) -> 1.0*0.7=0.7, 0.5*0.7=0.35
                x + s * 0.64f, y + s * 0.64f, // (0, 0.9) -> 0.9*0.7=0.63, 0.9*0.7=0.63
                x + s * 0.35f, y + s * 0.71f, // (-0.25, 0.75) -> 0.5*0.7=0.35, 1.0*0.7=0.7
                x - s * 0.25f, y + s * 0.46f, // (-0.5, 0.15) -> -0.35*0.7=-0.245, 0.65*0.7=0.455
                x - s * 0.67f, y + s * 0.18f // (-0.6, -0.35) -> -0.95*0.7=-0.665, 0.25*0.7=0.175
        };
        drawShape(vertices, 9, r, g, b, a);
    }

    private void drawTiltedDrumstickOutline(int x, int y, int size, float r, float g, float b, float a) {
        float s = size / 2f;
        // Outline - 45 DEGREE ROTATION
        float[] vertices = {
                x - s * 0.69f, y - s * 0.69f, // Top (0, -0.98) -> -0.98*0.7=-0.686, -0.98*0.7=-0.686
                x - s * 0.21f, y - s * 0.99f, // (0.55, -0.85) -> -0.3*0.7=-0.21, -1.4*0.7=-0.98
                x + s * 0.23f, y - s * 0.79f, // (0.72, -0.4) -> 0.22, -0.79
                x + s * 0.39f, y - s * 0.60f, // (0.7, -0.15) -> 0.385, -0.595
                x + s * 0.49f, y - s * 0.18f, // (0.48, 0.22) -> 0.49, -0.18
                x + s * 0.39f, y + s * 0.11f, // (0.2, 0.35) -> 0.385, 0.105
                x + s * 0.95f, y + s * 0.35f, // Bone R (0.42, 0.92) -> 1.34*0.7=0.938, 0.5*0.7=0.35
                x + s * 0.67f, y + s * 0.67f, // Bottom (0, 0.95) -> 0.665, 0.665
                x + s * 0.35f, y + s * 0.95f, // Bone L (-0.42, 0.92) -> 0.5*0.7=0.35, 1.34*0.7=0.938
                x + s * 0.11f, y + s * 0.39f, // (-0.2, 0.35) -> 0.105, 0.385
                x - s * 0.18f, y + s * 0.49f, // (-0.48, 0.22) -> -0.182, 0.49
                x - s * 0.60f, y + s * 0.39f, // (-0.7, -0.15) -> -0.595, 0.385
                x - s * 0.79f, y + s * 0.23f, // (-0.72, -0.4) -> -0.784, 0.224
                x - s * 0.99f, y - s * 0.21f // (-0.55, -0.85) -> -0.98, -0.21. Wait (-0.55, -0.85) -> -1.4*0.7=-0.98,
                                             // (-0.85--0.55)=-0.3*0.7=-0.21. So (x-0.99, y-0.21)
        };
        drawShapeOutline(vertices, 14, r, g, b, a);
    }

    private void drawShape(float[] vertices, int count, float r, float g, float b, float a) {
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer buffer = MemoryUtil.memAllocFloat(vertices.length);
        buffer.put(vertices).flip();
        // FORCE Buffer Orphan to avoid synchronization issues on some drivers
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
        // FORCE Buffer Orphan to avoid synchronization issues
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_DYNAMIC_DRAW);
        MemoryUtil.memFree(buffer);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        shader.setUniform("color", new org.joml.Vector4f(r, g, b, a));
        glBindVertexArray(vao);
        glDrawArrays(GL_LINE_LOOP, 0, count);
        glBindVertexArray(0);
    }

    public void cleanup() {
        if (shader != null) {
            shader.cleanup();
        }
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }

    /**
     * Draw stack count as a number in the bottom-right of a slot.
     */
    private void drawStackCount(int slotX, int slotY, int count) {
        String countStr = String.valueOf(count);
        int digitWidth = 6;
        int digitHeight = 8;
        int spacing = 1;
        int totalWidth = countStr.length() * (digitWidth + spacing) - spacing;

        // Position in bottom-right corner
        int baseX = slotX + HOTBAR_SLOT_SIZE - totalWidth - 4;
        int baseY = slotY + HOTBAR_SLOT_SIZE - digitHeight - 4;

        // Draw shadow first
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
     * Draw a single digit using 7-segment style rectangles.
     */
    private void drawDigit(int x, int y, int digit, float r, float g, float b) {
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

        if (segments[digit][0])
            drawRect(x, y, w, t, r, g, b, 1.0f);
        if (segments[digit][1])
            drawRect(x, y, t, h / 2, r, g, b, 1.0f);
        if (segments[digit][2])
            drawRect(x + w - t, y, t, h / 2, r, g, b, 1.0f);
        if (segments[digit][3])
            drawRect(x, y + h / 2 - t / 2, w, t, r, g, b, 1.0f);
        if (segments[digit][4])
            drawRect(x, y + h / 2, t, h / 2, r, g, b, 1.0f);
        if (segments[digit][5])
            drawRect(x + w - t, y + h / 2, t, h / 2, r, g, b, 1.0f);
        if (segments[digit][6])
            drawRect(x, y + h - t, w, t, r, g, b, 1.0f);
    }
}

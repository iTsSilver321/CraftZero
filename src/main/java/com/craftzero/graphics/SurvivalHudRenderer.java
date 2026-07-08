package com.craftzero.graphics;

import com.craftzero.engine.Window;
import com.craftzero.main.PlayerStats;
import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemRenderProfile;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.inventory.MapItemData;
import com.craftzero.main.Player;
import com.craftzero.progression.AchievementTracker;
import com.craftzero.progression.AchievementType;
import com.craftzero.progression.ArmorCalculator;
import com.craftzero.progression.ArmorSlot;
import com.craftzero.progression.PlayerProgression;
import com.craftzero.progression.StatusEffectInstance;
import com.craftzero.progression.StatusEffectType;
import com.craftzero.progression.StatusEffectVisuals;
import com.craftzero.world.Block;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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

    // Procedural fallback hotbar; textured rendering below derives from gui.png pixels.
    private static final int HOTBAR_SLOT_SIZE = 48;
    private static final int HOTBAR_SPACING = 48; // No gaps
    private static final int HOTBAR_WIDTH = 9 * HOTBAR_SPACING;
    private static final int HOTBAR_TEXTURE_WIDTH = 182;
    private static final int HOTBAR_TEXTURE_HEIGHT = 22;
    private static final int HOTBAR_SELECTION_TEXTURE_SIZE = 24;
    private static final int HOTBAR_SLOT_TEXTURE_PITCH = 20;
    private static final int HOTBAR_ITEM_TEXTURE_SIZE = 16;
    private static final int HOTBAR_ITEM_TEXTURE_INSET = 3;
    private static final int HOTBAR_SELECTION_TEXTURE_OFFSET = -1;
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
    private static final int BOSS_BAR_TEXTURE_WIDTH = 182;
    private static final int BOSS_BAR_TEXTURE_HEIGHT = 5;
    private static final int BOSS_BAR_SCALE = 2;

    private TextRenderer textRenderer;

    private World dynamicItemWorld;
    private Player dynamicItemPlayer;
    private float portalOverlayStrength;
    private float portalOverlayTime;
    private float waterOverlayTime;

    // Bubble Animation State
    private float[] bubblePopTimers = new float[10];
    private float lastAir = PlayerStats.MAX_AIR_SECONDS;

    private static final int HELD_MAP_TOP_MARGIN = 18;
    private static final int HELD_MAP_SIDE_MARGIN = 24;
    private static final int HELD_MAP_BOTTOM_GAP = 18;
    private static final int HELD_MAP_MAX_CELL_SIZE = 3;
    private static final int HELD_MAP_MIN_DISPLAY_PIXELS = 32;
    private static final int STATUS_EFFECT_ICON_SIZE = 32;
    private static final int STATUS_EFFECT_SPACING = 4;
    private static final int STATUS_EFFECT_RIGHT_MARGIN = 8;
    private static final int STATUS_EFFECT_TOP_MARGIN = 8;
    private static final int STATUS_EFFECT_ICON_BORDER = 2;
    private static final int STATUS_EFFECT_DURATION_BAR_HEIGHT = 3;
    private static final int STATUS_EFFECT_ITEM_INSET = 6;
    private static final float STATUS_EFFECT_AMPLIFIER_SCALE = 0.70f;
    private static final int STATUS_EFFECT_LOW_TIME_TICKS = 10 * 20;
    private static final int ACHIEVEMENT_TOAST_WIDTH = 320;
    private static final int ACHIEVEMENT_TOAST_HEIGHT = 64;
    private static final int ACHIEVEMENT_TOAST_MARGIN = 8;
    private static final int ACHIEVEMENT_TOAST_ICON_SIZE = 32;
    private static final int ACHIEVEMENT_TOAST_ICON_SLOT = 40;
    private static final int ACHIEVEMENT_TOAST_ICON_X = 12;
    private static final int ACHIEVEMENT_TOAST_ICON_Y = 14;
    private static final int ACHIEVEMENT_TEXTURE_SIZE = 256;
    private static final int ACHIEVEMENT_PANEL_U = 97;
    private static final int ACHIEVEMENT_PANEL_V = 203;
    private static final int ACHIEVEMENT_PANEL_W = 158;
    private static final int ACHIEVEMENT_PANEL_H = 23;
    private static final int ACHIEVEMENT_SLOT_U = 3;
    private static final int ACHIEVEMENT_SPECIAL_SLOT_U = 27;
    private static final int ACHIEVEMENT_SLOT_V = 203;
    private static final int ACHIEVEMENT_SLOT_SIZE = 24;
    private static final int ACHIEVEMENT_SOURCE_SCALE = 2;
    private static final float FIRE_OVERLAY_HEIGHT_RATIO = 0.58f;
    private static final float FIRE_OVERLAY_WIDTH_RATIO = 0.76f;
    private static final float FIRE_OVERLAY_BOTTOM_OVERSCAN_RATIO = 0.08f;
    private static final float FIRE_OVERLAY_MIN_HEIGHT = 145.0f;
    private static final float FIRE_OVERLAY_MAX_HEIGHT = 360.0f;
    private static final float PUMPKIN_OVERLAY_ALPHA = 1.0f;
    private static final float VIGNETTE_MIN_ALPHA = 0.18f;
    private static final float VIGNETTE_MAX_ALPHA = 0.82f;
    private static final float PORTAL_OVERLAY_MIN_ALPHA = 0.12f;
    private static final float PORTAL_OVERLAY_MAX_ALPHA = 0.74f;
    private static final float PORTAL_OVERLAY_BASE_REPEAT = 1.18f;
    private static final float PORTAL_OVERLAY_EXTRA_REPEAT = 0.42f;
    private static final float WATER_OVERLAY_ALPHA = 0.58f;
    private static final float WATER_OVERLAY_REPEAT = 1.08f;

    public record DebugOverlaySnapshot(
            boolean visible,
            String version,
            int fps,
            int windowWidth,
            int windowHeight,
            String dimension,
            String generator,
            long seed,
            String gameMode,
            String difficulty,
            float x,
            float y,
            float z,
            int blockX,
            int blockY,
            int blockZ,
            int chunkX,
            int chunkZ,
            int localX,
            int localY,
            int localZ,
            float yaw,
            float pitch,
            String facing,
            String biome,
            int skyLight,
            int blockLight,
            String targetBlock,
            String weather,
            float rainStrength,
            float thunderStrength,
            long worldTime,
            int day,
            int moonPhase,
            int renderDistance,
            int loadedChunks,
            int entities,
            int droppedItems,
            int particles,
            long usedMemoryBytes,
            long totalMemoryBytes,
            long maxMemoryBytes) {

        private List<String> leftLines() {
            return List.of(
                    version + " (" + fps + " fps)",
                    "Display: " + windowWidth + "x" + windowHeight,
                    "World: " + dimension + " / " + generator + " / seed " + seed,
                    "Mode: " + gameMode + " / " + difficulty,
                    format("XYZ: %.3f / %.3f / %.3f", x, y, z),
                    "Block: " + blockX + " " + blockY + " " + blockZ,
                    "Chunk: " + chunkX + " " + chunkZ + " in " + localX + " " + localY + " " + localZ,
                    format("Facing: %s (yaw %.1f / pitch %.1f)", facing, yaw, pitch),
                    "Biome: " + biome,
                    "Light: sky " + skyLight + " block " + blockLight,
                    "Target: " + targetBlock);
        }

        private List<String> rightLines() {
            return List.of(
                    "Memory: " + mib(usedMemoryBytes) + "MB / " + mib(totalMemoryBytes)
                            + "MB up to " + mib(maxMemoryBytes) + "MB",
                    "Java: " + System.getProperty("java.version", "unknown"),
                    "Time: " + worldTime + " day " + day + " moon " + moonPhase,
                    format("Weather: %s rain %.2f thunder %.2f", weather, rainStrength, thunderStrength),
                    "Chunks: " + loadedChunks + " loaded / render " + renderDistance,
                    "Entities: " + entities + " mobs/items " + droppedItems,
                    "Particles: " + particles);
        }

        private static String format(String pattern, Object... args) {
            return String.format(Locale.ROOT, pattern, args);
        }

        private static long mib(long bytes) {
            return Math.max(0L, bytes) / (1024L * 1024L);
        }
    }

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

        achievementTexture = new Texture("/textures/achievement/bg.png");
        pumpkinOverlayTexture = new Texture("/textures/misc/pumpkinblur.png");
        vignetteTexture = new Texture("/textures/misc/vignette.png");
        portalOverlayTexture = new Texture("/textures/misc/tunnel.png");
        portalOverlayTexture.setRepeatWrapping();
        waterOverlayTexture = new Texture("/textures/misc/water.png");
        waterOverlayTexture.setRepeatWrapping();

        System.out.println("SurvivalHudRenderer initialized - Window: " + windowWidth + "x" + windowHeight);
    }

    public void setAtlas(Texture atlas) {
        this.atlas = atlas;
    }

    // GUI textures for hearts, hunger, hotbar
    private Texture iconsTexture; // icons.png - hearts, hunger icons
    private Texture guiTexture; // gui.png - hotbar background
    private Texture itemsTexture; // items.png - sticks, tools
    private Texture achievementTexture; // achievement/bg.png - toast frame pieces
    private Texture pumpkinOverlayTexture; // misc/pumpkinblur.png - first-person helmet mask
    private Texture vignetteTexture; // misc/vignette.png - first-person light falloff
    private Texture portalOverlayTexture; // misc/tunnel.png - Nether portal screen wash
    private Texture waterOverlayTexture; // misc/water.png - underwater first-person wash

    public void setGuiTextures(Texture icons, Texture gui) {
        this.iconsTexture = icons;
        this.guiTexture = gui;
    }

    public void setItemsTexture(Texture items) {
        this.itemsTexture = items;
    }

    public void setDynamicItemContext(World world, Player player) {
        this.dynamicItemWorld = world;
        this.dynamicItemPlayer = player;
    }

    public void setPortalOverlayStrength(float strength) {
        this.portalOverlayStrength = Math.max(0.0f, Math.min(1.0f, strength));
    }

    public void updateOrtho(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
    }

    public void setTextRenderer(TextRenderer textRenderer) {
        this.textRenderer = textRenderer;
    }

    public void render(PlayerStats stats, Inventory inventory, float deltaTime) {
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

        // First-person held items render behind the normal HUD layer.
        int hotbarY = windowHeight - 60; // 10px from bottom edge
        drawHeldMapOverlay(inventory, hotbarY);
        if (drawVignetteOverlay(ortho)) {
            shader.bind();
            shader.setUniform("projection", ortho);
        }
        if (drawWaterOverlay(ortho, deltaTime)) {
            shader.bind();
            shader.setUniform("projection", ortho);
        }
        if (drawPortalOverlay(ortho, deltaTime)) {
            shader.bind();
            shader.setUniform("projection", ortho);
        }
        if (drawPumpkinHelmetOverlay(inventory, ortho)) {
            shader.bind();
            shader.setUniform("projection", ortho);
        }
        if (drawFirstPersonFireOverlay(ortho)) {
            shader.bind();
            shader.setUniform("projection", ortho);
        }

        // Hearts go LEFT from just left of center
        int heartStartX = centerX - 216;
        drawHearts(stats, heartStartX, bottomY);
        drawArmor(inventory, heartStartX, bottomY - 22);

        // Hunger goes RIGHT from just right of center
        int hungerStartX = centerX + 18;
        drawHungerRight(stats, hungerStartX, bottomY);

        // Bubbles go ABOVE hunger (if air < max or animating)
        // Check if any bubble is popping OR air < max
        boolean bubblesVisible = stats.getCurrentAir() < PlayerStats.MAX_AIR_SECONDS || hasBubbleAnimation();
        if (bubblesVisible) {
            // Move up to avoid overlap (hunger is ~18px tall + padding? bottomY is top of
            // icons?)
            // Icons draw downwards? Usually (x, y) is top-left.
            // If bottomY is 90 from bottom, bubbles at bottomY-26 puts them above.
            drawBubbles(stats, hungerStartX, bottomY - 26, deltaTime);
        }

        // Hotbar goes BELOW hearts/hunger
        drawExperience(stats.getProgression(), centerX, hotbarY - 16);
        drawHotbar(inventory, centerX, hotbarY);
        drawStatusEffects(stats.getActiveEffects());
        drawAchievementNotification(stats.getAchievements(), deltaTime);

        shader.unbind();

        // Update lastAir for next frame comparison
        lastAir = stats.getCurrentAir();

        // Restore state
        glEnable(GL_DEPTH_TEST);
        // glDisable(GL_BLEND); // Keep blending enabled for World renderer
    }

    public void renderHotbarOnly(Inventory inventory, float deltaTime) {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);

        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
        shader.bind();
        shader.setUniform("projection", ortho);
        int hotbarY = windowHeight - 60;
        drawHeldMapOverlay(inventory, hotbarY);
        if (drawVignetteOverlay(ortho)) {
            shader.bind();
            shader.setUniform("projection", ortho);
        }
        if (drawWaterOverlay(ortho, deltaTime)) {
            shader.bind();
            shader.setUniform("projection", ortho);
        }
        if (drawPortalOverlay(ortho, deltaTime)) {
            shader.bind();
            shader.setUniform("projection", ortho);
        }
        if (drawPumpkinHelmetOverlay(inventory, ortho)) {
            shader.bind();
            shader.setUniform("projection", ortho);
        }
        if (drawFirstPersonFireOverlay(ortho)) {
            shader.bind();
            shader.setUniform("projection", ortho);
        }
        drawHotbar(inventory, windowWidth / 2, hotbarY);
        shader.unbind();
        glEnable(GL_DEPTH_TEST);
    }

    public void renderDebugOverlay(DebugOverlaySnapshot snapshot) {
        if (snapshot == null || !snapshot.visible() || textRenderer == null) {
            return;
        }
        List<String> leftLines = snapshot.leftLines();
        List<String> rightLines = snapshot.rightLines();
        float scale = 1.0f;
        int lineHeight = 9;
        int margin = 2;
        int leftWidth = debugTextWidth(leftLines, scale);
        int rightWidth = debugTextWidth(rightLines, scale);
        int leftX = margin;
        int leftY = margin;
        int rightX = windowWidth - margin;
        int rightY = margin;
        if (rightX - rightWidth < leftX + leftWidth + 8) {
            rightX = margin + rightWidth;
            rightY = leftY + debugTextHeight(leftLines, lineHeight) + 4;
        }

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);

        drawDebugLines(leftLines, leftX, leftY, lineHeight, scale);
        drawDebugLinesRightAligned(rightLines, rightX, rightY, lineHeight, scale);
        glEnable(GL_DEPTH_TEST);
    }

    private int debugTextWidth(List<String> lines, float scale) {
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, textRenderer.getStringWidth(line, scale));
        }
        return width;
    }

    private static int debugTextHeight(List<String> lines, int lineHeight) {
        return lines.size() * lineHeight;
    }

    private void drawDebugLines(List<String> lines, int x, int y, int lineHeight, float scale) {
        for (int i = 0; i < lines.size(); i++) {
            drawHudTextShadowed(lines.get(i), x, y + i * lineHeight, scale,
                    new float[] { 0.92f, 0.92f, 0.92f, 1.0f });
        }
    }

    private void drawDebugLinesRightAligned(List<String> lines, int rightX, int y, int lineHeight, float scale) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int textWidth = textRenderer.getStringWidth(line, scale);
            drawHudTextShadowed(line, rightX - textWidth, y + i * lineHeight, scale,
                    new float[] { 0.92f, 0.92f, 0.92f, 1.0f });
        }
    }

    public void renderBossBar(String name, float healthFraction) {
        if (name == null || name.isBlank()) {
            return;
        }
        float clamped = Math.max(0.0f, Math.min(1.0f, healthFraction));
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);

        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
        shader.bind();
        shader.setUniform("projection", ortho);
        int width = Math.max(0, Math.min(BOSS_BAR_TEXTURE_WIDTH * BOSS_BAR_SCALE, windowWidth - 80));
        if (width <= 0) {
            shader.unbind();
            glEnable(GL_DEPTH_TEST);
            return;
        }
        int height = BOSS_BAR_TEXTURE_HEIGHT * BOSS_BAR_SCALE;
        int x = (windowWidth - width) / 2;
        int y = 20;
        if (!drawTexturedBossBar(x, y, width, height, clamped, ortho)) {
            drawRect(x - 1, y - 1, width + 2, height + 2, 0.0f, 0.0f, 0.0f, 0.85f);
            drawRect(x, y, width, height, 0.18f, 0.0f, 0.18f, 1.0f);
            drawRect(x, y, Math.round(width * clamped), height, 0.65f, 0.0f, 0.72f, 1.0f);
        }
        if (textRenderer != null) {
            float scale = 1.0f;
            int textWidth = textRenderer.getStringWidth(name, scale);
            drawHudTextShadowed(name, (windowWidth - textWidth) / 2.0f, 8.0f, scale,
                    new float[] { 1.0f, 1.0f, 1.0f, 1.0f });
            restoreColorShader();
        }
        shader.unbind();
        glEnable(GL_DEPTH_TEST);
    }

    private boolean drawTexturedBossBar(int x, int y, int width, int height, float healthFraction, Matrix4f ortho) {
        if (iconsTexture == null || width <= 0 || height <= 0) {
            return false;
        }
        shader.unbind();
        iconsTexture.bind(0);
        texturedShader.bind();
        texturedShader.setUniform("projection", ortho);
        texturedShader.setUniform("textureSampler", 0);
        texturedShader.setUniform("brightness", 1.0f);
        float[] background = GuiTexture.getBossBarBackgroundUV();
        drawTexturedQuad(x, y, x + width, y, x + width, y + height, x, y + height,
                background[0], background[1], background[2], background[3]);
        int sourceFill = Math.round(BOSS_BAR_TEXTURE_WIDTH * healthFraction);
        if (sourceFill > 0) {
            int fillWidth = Math.round(width * (sourceFill / (float) BOSS_BAR_TEXTURE_WIDTH));
            float[] fill = GuiTexture.getBossBarFillUV(sourceFill);
            drawTexturedQuad(x, y, x + fillWidth, y, x + fillWidth, y + height, x, y + height,
                    fill[0], fill[1], fill[2], fill[3]);
        }
        texturedShader.unbind();
        iconsTexture.unbind();
        shader.bind();
        shader.setUniform("projection", ortho);
        return true;
    }

    private boolean hasBubbleAnimation() {
        for (float t : bubblePopTimers) {
            if (t > 0) {
                return true;
            }
        }
        return false;
    }

    private void drawStatusEffects(List<StatusEffectInstance> effects) {
        List<StatusEffectHudEntry> entries = statusEffectHudEntries(effects, windowWidth, STATUS_EFFECT_TOP_MARGIN);
        if (entries.isEmpty()) {
            return;
        }

        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
        shader.bind();
        shader.setUniform("projection", ortho);

        for (StatusEffectHudEntry entry : entries) {
            drawStatusEffectIcon(entry, ortho);
        }

        if (textRenderer == null) {
            return;
        }

        shader.unbind();
        for (StatusEffectHudEntry entry : entries) {
            drawStatusEffectAmplifier(entry);
        }
        shader.bind();
        shader.setUniform("projection", ortho);
    }

    private void drawStatusEffectIcon(StatusEffectHudEntry entry, Matrix4f ortho) {
        float alpha = Math.max(0.35f, Math.min(1.0f, entry.warningAlpha()));
        int x = entry.x();
        int y = entry.y();
        int size = entry.iconSize();
        int inner = size - STATUS_EFFECT_ICON_BORDER * 2;

        drawRect(x - 1, y - 1, size + 2, size + 2, 0.0f, 0.0f, 0.0f, 0.52f * alpha);
        drawRect(x, y, size, size, 0.36f, 0.36f, 0.36f, 0.92f * alpha);
        drawRect(x + 1, y + 1, size - 2, 1, 0.76f, 0.76f, 0.68f, 0.50f * alpha);
        drawRect(x + 1, y + 1, 1, size - 2, 0.70f, 0.70f, 0.64f, 0.42f * alpha);
        drawRect(x + 1, y + size - 2, size - 2, 1, 0.02f, 0.02f, 0.02f, 0.62f * alpha);
        drawRect(x + size - 2, y + 1, 1, size - 2, 0.02f, 0.02f, 0.02f, 0.55f * alpha);
        drawRect(x + STATUS_EFFECT_ICON_BORDER, y + STATUS_EFFECT_ICON_BORDER, inner, inner,
                entry.red() * 0.42f, entry.green() * 0.42f, entry.blue() * 0.42f, 0.96f * alpha);
        drawRect(x + STATUS_EFFECT_ICON_BORDER + 1, y + STATUS_EFFECT_ICON_BORDER + 1,
                Math.max(1, inner - 2), Math.max(1, inner / 2),
                Math.min(1.0f, entry.red() * 0.75f + 0.16f),
                Math.min(1.0f, entry.green() * 0.75f + 0.16f),
                Math.min(1.0f, entry.blue() * 0.75f + 0.16f),
                0.26f * alpha);

        int barX = x + 4;
        int barY = y + size - STATUS_EFFECT_DURATION_BAR_HEIGHT - 4;
        int barWidth = size - 8;
        drawRect(barX, barY, barWidth, STATUS_EFFECT_DURATION_BAR_HEIGHT, 0.0f, 0.0f, 0.0f, 0.58f * alpha);
        drawRect(barX, barY,
                statusEffectDurationBarWidth(entry.durationTicks(), barWidth),
                STATUS_EFFECT_DURATION_BAR_HEIGHT,
                0.95f, 0.95f, 0.95f, 0.86f * alpha);

        if (entry.iconItem() != null) {
            int itemSize = size - STATUS_EFFECT_ITEM_INSET * 2;
            drawItemSprite(x + STATUS_EFFECT_ITEM_INSET, y + STATUS_EFFECT_ITEM_INSET - 1, itemSize, entry.iconItem());
            shader.bind();
            shader.setUniform("projection", ortho);
        }
    }

    private void drawStatusEffectAmplifier(StatusEffectHudEntry entry) {
        if (entry.amplifierText().isEmpty()) {
            return;
        }
        float alpha = Math.max(0.35f, Math.min(1.0f, entry.warningAlpha()));
        int textWidth = textRenderer.getStringWidth(entry.amplifierText(), STATUS_EFFECT_AMPLIFIER_SCALE);
        float x = entry.x() + entry.iconSize() - textWidth - 3.0f;
        float y = entry.y() + 2.0f;
        drawHudTextShadowed(entry.amplifierText(), x, y, STATUS_EFFECT_AMPLIFIER_SCALE,
                new float[] { 1.0f, 1.0f, 1.0f, alpha });
    }

    private void drawAchievementNotification(AchievementTracker tracker, float deltaTime) {
        if (tracker == null) {
            return;
        }
        tracker.updateNotifications(deltaTime);
        AchievementType achievement = tracker.activeNotification();
        if (achievement == null) {
            return;
        }
        float alpha = AchievementTracker.notificationAlpha(tracker.activeNotificationAge());
        if (alpha <= 0.0f) {
            return;
        }

        int width = Math.min(ACHIEVEMENT_TOAST_WIDTH, Math.max(160, windowWidth - ACHIEVEMENT_TOAST_MARGIN * 2));
        int height = ACHIEVEMENT_TOAST_HEIGHT;
        int x = windowWidth - width - ACHIEVEMENT_TOAST_MARGIN
                + Math.round((1.0f - alpha) * (width + ACHIEVEMENT_TOAST_MARGIN));
        int y = ACHIEVEMENT_TOAST_MARGIN;

        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
        shader.bind();
        shader.setUniform("projection", ortho);
        drawAchievementToastFrame(x, y, width, height, alpha, achievement.special(), ortho);

        int iconSlotX = x + ACHIEVEMENT_TOAST_ICON_X;
        int iconSlotY = y + ACHIEVEMENT_TOAST_ICON_Y;
        int iconX = iconSlotX + (ACHIEVEMENT_TOAST_ICON_SLOT - ACHIEVEMENT_TOAST_ICON_SIZE) / 2;
        int iconY = iconSlotY + (ACHIEVEMENT_TOAST_ICON_SLOT - ACHIEVEMENT_TOAST_ICON_SIZE) / 2;
        drawAchievementToastIconSlot(iconSlotX, iconSlotY, alpha, achievement.special(), ortho);
        drawAchievementToastIcon(achievement, iconX, iconY, ACHIEVEMENT_TOAST_ICON_SIZE, alpha, ortho);

        if (textRenderer == null) {
            shader.bind();
            shader.setUniform("projection", ortho);
            return;
        }

        float labelScale = 0.9f;
        float titleScale = 1.05f;
        float textX = x + 62.0f;
        float labelY = y + 12.0f;
        float titleY = y + 32.0f;
        int maxTitleWidth = Math.max(24, width - 72);
        String title = truncateTextToWidth(achievement.title(), maxTitleWidth, titleScale);
        drawHudTextShadowed("Achievement get!", textX, labelY, labelScale,
                new float[] { 1.0f, 1.0f, 0.45f, alpha });
        drawHudTextShadowed(title, textX, titleY, titleScale,
                new float[] { 1.0f, 1.0f, 1.0f, alpha });

        shader.bind();
        shader.setUniform("projection", ortho);
    }

    private void drawAchievementToastFrame(int x, int y, int width, int height, float alpha, boolean special,
            Matrix4f ortho) {
        if (achievementTexture != null) {
            int panelHeight = Math.min(height - 12, ACHIEVEMENT_PANEL_H * ACHIEVEMENT_SOURCE_SCALE);
            int panelY = y + (height - panelHeight) / 2;
            drawAchievementTextureRegion(x, panelY, width, panelHeight,
                    ACHIEVEMENT_PANEL_U, ACHIEVEMENT_PANEL_V, ACHIEVEMENT_PANEL_W, ACHIEVEMENT_PANEL_H,
                    alpha, ortho);
            float accentR = special ? 0.72f : 0.24f;
            float accentG = special ? 0.48f : 0.23f;
            float accentB = special ? 0.94f : 0.19f;
            drawRect(x + 2, panelY + 2, width - 4, 1, accentR, accentG, accentB, 0.30f * alpha);
            drawRect(x + 2, panelY + panelHeight - 3, width - 4, 1,
                    0.0f, 0.0f, 0.0f, 0.45f * alpha);
            return;
        }
        float accentR = special ? 0.86f : 0.54f;
        float accentG = special ? 0.56f : 0.46f;
        float accentB = special ? 0.98f : 0.34f;
        drawRect(x, y, width, height, 0.0f, 0.0f, 0.0f, 0.82f * alpha);
        drawRect(x + 1, y + 1, width - 2, height - 2, accentR * 0.50f, accentG * 0.50f, accentB * 0.50f,
                0.90f * alpha);
        drawRect(x + 2, y + 2, width - 4, height - 4, 0.19f, 0.18f, 0.16f, 0.94f * alpha);
        drawRect(x + 3, y + 3, width - 6, 2, 0.48f, 0.46f, 0.39f, 0.42f * alpha);
        drawRect(x + 3, y + height - 5, width - 6, 2, 0.02f, 0.02f, 0.02f, 0.58f * alpha);
        drawRect(x + 3, y + 5, 2, height - 10, 0.38f, 0.36f, 0.31f, 0.32f * alpha);
        drawRect(x + width - 5, y + 5, 2, height - 10, 0.02f, 0.02f, 0.02f, 0.40f * alpha);
    }

    private void drawAchievementToastIconSlot(int x, int y, float alpha, boolean special, Matrix4f ortho) {
        if (achievementTexture != null) {
            int size = ACHIEVEMENT_SLOT_SIZE * ACHIEVEMENT_SOURCE_SCALE;
            int slotX = x + (ACHIEVEMENT_TOAST_ICON_SLOT - size) / 2;
            int slotY = y + (ACHIEVEMENT_TOAST_ICON_SLOT - size) / 2;
            int u = special ? ACHIEVEMENT_SPECIAL_SLOT_U : ACHIEVEMENT_SLOT_U;
            drawAchievementTextureRegion(slotX, slotY, size, size,
                    u, ACHIEVEMENT_SLOT_V, ACHIEVEMENT_SLOT_SIZE, ACHIEVEMENT_SLOT_SIZE, alpha, ortho);
            return;
        }
        float slotR = special ? 0.28f : 0.18f;
        float slotG = special ? 0.20f : 0.18f;
        float slotB = special ? 0.36f : 0.18f;
        drawRect(x, y, ACHIEVEMENT_TOAST_ICON_SLOT, ACHIEVEMENT_TOAST_ICON_SLOT, 0.02f, 0.02f, 0.02f,
                0.70f * alpha);
        drawRect(x + 2, y + 2, ACHIEVEMENT_TOAST_ICON_SLOT - 4, ACHIEVEMENT_TOAST_ICON_SLOT - 4,
                0.40f, 0.38f, 0.33f, 0.65f * alpha);
        drawRect(x + 4, y + 4, ACHIEVEMENT_TOAST_ICON_SLOT - 8, ACHIEVEMENT_TOAST_ICON_SLOT - 8,
                slotR, slotG, slotB, 0.84f * alpha);
        drawRect(x + 5, y + 5, ACHIEVEMENT_TOAST_ICON_SLOT - 10, 1,
                0.74f, 0.70f, 0.58f, 0.35f * alpha);
        drawRect(x + 5, y + ACHIEVEMENT_TOAST_ICON_SLOT - 6, ACHIEVEMENT_TOAST_ICON_SLOT - 10, 1,
                0.0f, 0.0f, 0.0f, 0.45f * alpha);
    }

    private void drawAchievementToastIcon(AchievementType achievement, int x, int y, int size, float alpha,
            Matrix4f ortho) {
        ItemType icon = achievement == null ? null : achievement.icon();
        if (icon == null) {
            return;
        }
        boolean textured = atlas != null || itemsTexture != null;
        if (!textured) {
            drawSimpleSquare(x, y, size, 0.82f, 0.58f, 0.14f, alpha);
            return;
        }
        if (icon.getRenderProfile().modelKind() == ItemRenderProfile.ModelKind.BLOCK) {
            if (atlas != null) {
                drawIsometricBlockIcon(x, y, size, icon, alpha);
            } else {
                drawSimpleSquare(x, y, size, 0.45f, 0.45f, 0.45f, alpha);
            }
        } else {
            drawItemSprite(x, y, size, icon, alpha);
        }
        shader.bind();
        shader.setUniform("projection", ortho);
    }

    private void drawAchievementTextureRegion(int x, int y, int width, int height,
            int u, int v, int sourceWidth, int sourceHeight, float alpha, Matrix4f ortho) {
        if (achievementTexture == null || width <= 0 || height <= 0) {
            return;
        }
        shader.unbind();
        achievementTexture.bind(0);
        texturedShader.bind();
        texturedShader.setUniform("projection", ortho);
        texturedShader.setUniform("textureSampler", 0);
        texturedShader.setUniform("brightness", 1.0f);
        texturedShader.setUniform("alpha", Math.max(0.0f, Math.min(1.0f, alpha)));
        drawTexturedQuad(x, y, x + width, y, x + width, y + height, x, y + height,
                u / (float) ACHIEVEMENT_TEXTURE_SIZE,
                v / (float) ACHIEVEMENT_TEXTURE_SIZE,
                (u + sourceWidth) / (float) ACHIEVEMENT_TEXTURE_SIZE,
                (v + sourceHeight) / (float) ACHIEVEMENT_TEXTURE_SIZE);
        texturedShader.setUniform("alpha", 1.0f);
        texturedShader.unbind();
        achievementTexture.unbind();
        shader.bind();
        shader.setUniform("projection", ortho);
    }

    private String truncateTextToWidth(String text, int maxWidth, float scale) {
        if (text == null) {
            return "";
        }
        if (textRenderer == null || textRenderer.getStringWidth(text, scale) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        String candidate = text;
        while (!candidate.isEmpty()
                && textRenderer.getStringWidth(candidate + ellipsis, scale) > maxWidth) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }
        return candidate.isEmpty() ? ellipsis : candidate + ellipsis;
    }

    private void drawHudTextShadowed(String text, float x, float y, float scale, float[] color) {
        textRenderer.drawText(text, x + 1, y + 1, scale,
                new float[] { 0.0f, 0.0f, 0.0f, color[3] });
        textRenderer.drawText(text, x, y, scale, color);
    }

    private void drawHeldMapOverlay(Inventory inventory, int hotbarY) {
        if (inventory == null) {
            return;
        }
        ItemStack held = inventory.getItemInHand();
        MapItemData.View view = MapItemData.view(held);
        if (view == null || !view.initialized()) {
            return;
        }

        HeldMapLayout layout = heldMapLayout(windowWidth, windowHeight, hotbarY);
        int mapSize = layout.mapSize();
        int frame = layout.frame();
        int x = layout.mapX();
        int y = layout.mapY();
        HeldMapHandPose hands = heldMapHandPose(layout);

        drawHeldMapForearms(hands);
        drawRect(x - frame, y - frame, mapSize + frame * 2, mapSize + frame * 2,
                0.58f, 0.49f, 0.33f, 0.94f);
        drawRect(x - frame + 2, y - frame + 2, mapSize + frame * 2 - 4, mapSize + frame * 2 - 4,
                0.84f, 0.76f, 0.56f, 0.95f);
        drawRect(x, y, mapSize, mapSize, 0.78f, 0.70f, 0.50f, 1.0f);

        byte[] colors = view.colors();
        for (int py = 0; py < layout.displayPixels(); py++) {
            int sourceY = py * layout.sourceStep();
            for (int px = 0; px < layout.displayPixels(); px++) {
                int sourceX = px * layout.sourceStep();
                int palette = colors[sourceX + sourceY * MapItemData.MAP_SIZE] & 0xFF;
                int rgb = MapItemData.rgbForPaletteIndex(palette);
                float r = ((rgb >> 16) & 0xFF) / 255.0f;
                float g = ((rgb >> 8) & 0xFF) / 255.0f;
                float b = (rgb & 0xFF) / 255.0f;
                drawRect(x + px * layout.cellSize(), y + py * layout.cellSize(),
                        layout.cellSize(), layout.cellSize(), r, g, b, 1.0f);
            }
        }

        HeldMapMarker markerPosition = heldMapMarker(layout, view);
        if (markerPosition != null) {
            int markerX = markerPosition.x();
            int markerY = markerPosition.y();
            int marker = Math.max(6, layout.cellSize() * 4);
            MarkerVector vector = markerVector(view.playerRotation(), marker);
            int markerBody = markerPosition.edgeClamped() ? 4 : 3;
            drawRect(markerX - markerBody / 2, markerY - markerBody / 2, markerBody, markerBody,
                    0.05f, 0.05f, 0.05f, 1.0f);
            drawRect(markerX + vector.tipX() - 1, markerY + vector.tipY() - 1, 3, 3,
                    0.05f, 0.05f, 0.05f, 1.0f);
            drawRect(markerX - vector.sideX(), markerY - vector.sideY(), 2, 2,
                    0.05f, 0.05f, 0.05f, 1.0f);
            drawRect(markerX + 1, markerY + 1, 1, 1, 1.0f, 1.0f, 1.0f, 1.0f);
        }
        drawHeldMapGripHands(hands);
    }

    private boolean drawFirstPersonFireOverlay(Matrix4f ortho) {
        if (dynamicItemPlayer == null || !dynamicItemPlayer.isOnFire() || atlas == null || texturedShader == null) {
            return false;
        }
        float[] uv = BlockType.FIRE.getTextureCoords(Block.FACE_NORTH);
        atlas.bind(0);
        texturedShader.bind();
        texturedShader.setUniform("projection", ortho);
        texturedShader.setUniform("textureSampler", 0);
        texturedShader.setUniform("brightness", 1.0f);
        texturedShader.setUniform("alpha", 1.0f);
        for (HudQuad quad : firstPersonFireOverlayQuads(windowWidth, windowHeight)) {
            drawTexturedQuad(quad.x1(), quad.y1(), quad.x2(), quad.y2(), quad.x3(), quad.y3(), quad.x4(), quad.y4(),
                    uv[0], uv[1], uv[2], uv[3]);
        }
        texturedShader.unbind();
        atlas.unbind();
        return true;
    }

    private boolean drawPumpkinHelmetOverlay(Inventory inventory, Matrix4f ortho) {
        if (!isWearingPumpkin(inventory) || pumpkinOverlayTexture == null || texturedShader == null) {
            return false;
        }
        pumpkinOverlayTexture.bind(0);
        texturedShader.bind();
        texturedShader.setUniform("projection", ortho);
        texturedShader.setUniform("textureSampler", 0);
        texturedShader.setUniform("brightness", 1.0f);
        texturedShader.setUniform("alpha", PUMPKIN_OVERLAY_ALPHA);
        drawTexturedQuad(0.0f, 0.0f,
                windowWidth, 0.0f,
                windowWidth, windowHeight,
                0.0f, windowHeight,
                0.0f, 0.0f, 1.0f, 1.0f);
        texturedShader.unbind();
        pumpkinOverlayTexture.unbind();
        return true;
    }

    private boolean drawVignetteOverlay(Matrix4f ortho) {
        if (vignetteTexture == null || texturedShader == null || windowWidth <= 0 || windowHeight <= 0) {
            return false;
        }
        float alpha = vignetteAlpha();
        if (alpha <= 0.01f) {
            return false;
        }
        vignetteTexture.bind(0);
        texturedShader.bind();
        texturedShader.setUniform("projection", ortho);
        texturedShader.setUniform("textureSampler", 0);
        texturedShader.setUniform("brightness", 1.0f);
        texturedShader.setUniform("alpha", alpha);
        drawTexturedQuad(0.0f, 0.0f,
                windowWidth, 0.0f,
                windowWidth, windowHeight,
                0.0f, windowHeight,
                0.0f, 0.0f, 1.0f, 1.0f);
        texturedShader.setUniform("alpha", 1.0f);
        texturedShader.unbind();
        vignetteTexture.unbind();
        return true;
    }

    private float vignetteAlpha() {
        if (dynamicItemWorld == null || dynamicItemPlayer == null) {
            return VIGNETTE_MIN_ALPHA;
        }
        int x = (int) Math.floor(dynamicItemPlayer.getPosition().x);
        int y = Math.max(0, Math.min(255, (int) Math.floor(dynamicItemPlayer.getEyeY())));
        int z = (int) Math.floor(dynamicItemPlayer.getPosition().z);
        int light = Math.max(dynamicItemWorld.getSkyLight(x, y, z), dynamicItemWorld.getBlockLight(x, y, z));
        float darkness = 1.0f - Math.max(0.0f, Math.min(1.0f, light / 15.0f));
        float eased = darkness * darkness * (3.0f - 2.0f * darkness);
        return VIGNETTE_MIN_ALPHA + (VIGNETTE_MAX_ALPHA - VIGNETTE_MIN_ALPHA) * eased;
    }

    private boolean drawPortalOverlay(Matrix4f ortho, float deltaTime) {
        if (portalOverlayTexture == null || texturedShader == null || windowWidth <= 0 || windowHeight <= 0) {
            return false;
        }
        if (portalOverlayStrength <= 0.001f) {
            portalOverlayTime = 0.0f;
            return false;
        }

        portalOverlayTime += Math.max(0.0f, deltaTime);
        float eased = portalOverlayStrength * portalOverlayStrength;
        float alpha = PORTAL_OVERLAY_MIN_ALPHA + (PORTAL_OVERLAY_MAX_ALPHA - PORTAL_OVERLAY_MIN_ALPHA) * eased;
        float repeat = PORTAL_OVERLAY_BASE_REPEAT + PORTAL_OVERLAY_EXTRA_REPEAT * (1.0f - portalOverlayStrength);
        float drift = portalOverlayTime * (0.13f + 0.09f * portalOverlayStrength);
        float u1 = -repeat + drift;
        float v1 = -repeat - drift * 0.65f;
        float u2 = repeat + drift;
        float v2 = repeat - drift * 0.65f;

        portalOverlayTexture.bind(0);
        texturedShader.bind();
        texturedShader.setUniform("projection", ortho);
        texturedShader.setUniform("textureSampler", 0);
        texturedShader.setUniform("brightness", 1.0f);
        texturedShader.setUniform("alpha", alpha);
        drawTexturedQuad(0.0f, 0.0f,
                windowWidth, 0.0f,
                windowWidth, windowHeight,
                0.0f, windowHeight,
                u1, v1, u2, v2);
        texturedShader.setUniform("alpha", 1.0f);
        texturedShader.unbind();
        portalOverlayTexture.unbind();
        return true;
    }

    private boolean drawWaterOverlay(Matrix4f ortho, float deltaTime) {
        if (dynamicItemPlayer == null || !dynamicItemPlayer.isHeadInWater()
                || waterOverlayTexture == null || texturedShader == null
                || windowWidth <= 0 || windowHeight <= 0) {
            waterOverlayTime = 0.0f;
            return false;
        }

        waterOverlayTime += Math.max(0.0f, deltaTime);
        float drift = waterOverlayTime * 0.045f;
        float wobble = (float) Math.sin(waterOverlayTime * 0.8f) * 0.025f;
        float u1 = -WATER_OVERLAY_REPEAT + drift;
        float v1 = -WATER_OVERLAY_REPEAT + wobble;
        float u2 = WATER_OVERLAY_REPEAT + drift;
        float v2 = WATER_OVERLAY_REPEAT + wobble;

        waterOverlayTexture.bind(0);
        texturedShader.bind();
        texturedShader.setUniform("projection", ortho);
        texturedShader.setUniform("textureSampler", 0);
        texturedShader.setUniform("brightness", 1.0f);
        texturedShader.setUniform("alpha", WATER_OVERLAY_ALPHA);
        drawTexturedQuad(0.0f, 0.0f,
                windowWidth, 0.0f,
                windowWidth, windowHeight,
                0.0f, windowHeight,
                u1, v1, u2, v2);
        texturedShader.setUniform("alpha", 1.0f);
        texturedShader.unbind();
        waterOverlayTexture.unbind();
        return true;
    }

    private boolean isWearingPumpkin(Inventory inventory) {
        if (inventory == null || inventory.getArmor() == null) {
            return false;
        }
        int helmetSlot = ArmorSlot.HELMET.getIndex();
        if (helmetSlot < 0 || helmetSlot >= inventory.getArmor().length) {
            return false;
        }
        ItemStack helmet = inventory.getArmor()[helmetSlot];
        return helmet != null && !helmet.isEmpty() && helmet.getType() == ItemType.PUMPKIN;
    }

    private void drawHeldMapForearms(HeldMapHandPose hands) {
        drawHudQuad(hands.leftSleeve(), 0.21f, 0.16f, 0.12f, 0.98f);
        drawHudQuad(hands.rightSleeve(), 0.21f, 0.16f, 0.12f, 0.98f);
    }

    private void drawHeldMapGripHands(HeldMapHandPose hands) {
        drawHudQuad(hands.leftHand(), 0.78f, 0.58f, 0.40f, 1.0f);
        drawHudQuad(hands.rightHand(), 0.78f, 0.58f, 0.40f, 1.0f);
        drawShapeOutline(hands.leftHand().vertices(), 4, 0.24f, 0.14f, 0.08f, 0.85f);
        drawShapeOutline(hands.rightHand().vertices(), 4, 0.24f, 0.14f, 0.08f, 0.85f);
    }

    private void drawHudQuad(HudQuad quad, float r, float g, float b, float a) {
        drawShape(quad.vertices(), 4, r, g, b, a);
    }

    static HeldMapHandPose heldMapHandPose(HeldMapLayout layout) {
        int mapSize = layout.mapSize();
        float frame = layout.frame();
        float outerLeft = layout.mapX() - frame;
        float outerRight = layout.mapX() + mapSize + frame;
        float outerBottom = layout.mapY() + mapSize + frame;
        float gripY = layout.mapY() + mapSize - clamp(mapSize * 0.08f, 7.0f, 24.0f);

        float sleeveWidth = clamp(mapSize * 0.16f, 18.0f, 58.0f);
        float sleeveLength = clamp(mapSize * 0.30f, 36.0f, 112.0f);
        float bottomSpread = clamp(mapSize * 0.15f, 18.0f, 58.0f);
        float inset = clamp(mapSize * 0.045f, 6.0f, 18.0f);
        float handWidth = clamp(mapSize * 0.095f, 11.0f, 28.0f);
        float handHeight = clamp(mapSize * 0.085f, 10.0f, 24.0f);

        float leftTopX = outerLeft + inset;
        float leftBottomX = leftTopX - bottomSpread;
        HudQuad leftSleeve = new HudQuad(
                leftTopX, gripY + handHeight * 0.45f,
                leftTopX + sleeveWidth, gripY + handHeight * 0.10f,
                leftBottomX + sleeveWidth, outerBottom + sleeveLength,
                leftBottomX, outerBottom + sleeveLength);
        HudQuad leftHand = new HudQuad(
                leftTopX + sleeveWidth * 0.28f, gripY - handHeight * 0.30f,
                leftTopX + sleeveWidth * 0.28f + handWidth, gripY - handHeight * 0.05f,
                leftTopX + sleeveWidth * 0.18f + handWidth, gripY + handHeight,
                leftTopX + sleeveWidth * 0.18f, gripY + handHeight * 0.78f);

        float rightTopX = outerRight - inset - sleeveWidth;
        float rightBottomX = rightTopX + bottomSpread;
        HudQuad rightSleeve = new HudQuad(
                rightTopX, gripY + handHeight * 0.10f,
                rightTopX + sleeveWidth, gripY + handHeight * 0.45f,
                rightBottomX + sleeveWidth, outerBottom + sleeveLength,
                rightBottomX, outerBottom + sleeveLength);
        HudQuad rightHand = new HudQuad(
                rightTopX + sleeveWidth * 0.72f - handWidth, gripY - handHeight * 0.05f,
                rightTopX + sleeveWidth * 0.72f, gripY - handHeight * 0.30f,
                rightTopX + sleeveWidth * 0.82f, gripY + handHeight * 0.78f,
                rightTopX + sleeveWidth * 0.82f - handWidth, gripY + handHeight);

        return new HeldMapHandPose(leftSleeve, leftHand, rightSleeve, rightHand);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    static HeldMapLayout heldMapLayout(int windowWidth, int windowHeight, int hotbarY) {
        int bottomLimit = Math.max(HELD_MAP_TOP_MARGIN + HELD_MAP_MIN_DISPLAY_PIXELS,
                Math.min(hotbarY, windowHeight - 8));

        for (int displayPixels = MapItemData.MAP_SIZE;
                displayPixels >= HELD_MAP_MIN_DISPLAY_PIXELS;
                displayPixels /= 2) {
            int sourceStep = Math.max(1, MapItemData.MAP_SIZE / displayPixels);
            for (int cellSize = HELD_MAP_MAX_CELL_SIZE; cellSize >= 1; cellSize--) {
                int frame = heldMapFrameForCell(cellSize);
                int mapSize = displayPixels * cellSize;
                boolean fitsWidth = mapSize + frame * 2 <= windowWidth - HELD_MAP_SIDE_MARGIN;
                boolean fitsHeight = bottomLimit - (mapSize + frame * 2 + HELD_MAP_BOTTOM_GAP)
                        >= HELD_MAP_TOP_MARGIN;
                if (fitsWidth && fitsHeight) {
                    int x = (windowWidth - mapSize) / 2;
                    int y = bottomLimit - mapSize - frame * 2 - HELD_MAP_BOTTOM_GAP + frame;
                    return new HeldMapLayout(x, y, displayPixels, sourceStep, cellSize, frame);
                }
            }
        }

        int fallbackFrame = heldMapFrameForCell(1);
        int fallbackPixels = HELD_MAP_MIN_DISPLAY_PIXELS;
        int x = Math.max(fallbackFrame, (windowWidth - fallbackPixels) / 2);
        int y = Math.max(HELD_MAP_TOP_MARGIN + fallbackFrame, bottomLimit - fallbackPixels
                - fallbackFrame - HELD_MAP_BOTTOM_GAP);
        return new HeldMapLayout(x, y, fallbackPixels, MapItemData.MAP_SIZE / fallbackPixels, 1, fallbackFrame);
    }

    private static int heldMapFrameForCell(int cellSize) {
        return Math.max(8, cellSize * 5);
    }

    static HeldMapMarker heldMapMarker(HeldMapLayout layout, MapItemData.View view) {
        if (layout == null || view == null || view.playerRotation() < 0) {
            return null;
        }
        int clampedX = clamp(view.playerPixelX(), 0, MapItemData.MAP_SIZE - 1);
        int clampedZ = clamp(view.playerPixelZ(), 0, MapItemData.MAP_SIZE - 1);
        boolean edgeClamped = clampedX != view.playerPixelX() || clampedZ != view.playerPixelZ();
        int displayX = Math.min(layout.displayPixels() - 1, clampedX / layout.sourceStep());
        int displayZ = Math.min(layout.displayPixels() - 1, clampedZ / layout.sourceStep());
        return new HeldMapMarker(
                layout.mapX() + displayX * layout.cellSize(),
                layout.mapY() + displayZ * layout.cellSize(),
                edgeClamped);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    static List<HudQuad> firstPersonFireOverlayQuads(int windowWidth, int windowHeight) {
        float screenWidth = Math.max(1.0f, windowWidth);
        float screenHeight = Math.max(1.0f, windowHeight);
        float height = clamp(screenHeight * FIRE_OVERLAY_HEIGHT_RATIO,
                FIRE_OVERLAY_MIN_HEIGHT, FIRE_OVERLAY_MAX_HEIGHT);
        float width = height * FIRE_OVERLAY_WIDTH_RATIO;
        float centerX = screenWidth * 0.5f;
        float innerGap = clamp(screenWidth * 0.055f, 24.0f, 72.0f);
        float bottom = screenHeight + height * FIRE_OVERLAY_BOTTOM_OVERSCAN_RATIO;
        float top = bottom - height;

        HudQuad left = new HudQuad(
                centerX - innerGap - width * 0.95f, top,
                centerX - innerGap + width * 0.12f, top,
                centerX - innerGap - width * 0.05f, bottom,
                centerX - innerGap - width * 1.08f, bottom);
        HudQuad right = new HudQuad(
                centerX + innerGap - width * 0.12f, top,
                centerX + innerGap + width * 0.95f, top,
                centerX + innerGap + width * 1.08f, bottom,
                centerX + innerGap + width * 0.05f, bottom);
        return List.of(left, right);
    }

    static MarkerVector markerVector(int rotation, int markerSize) {
        if (rotation < 0) {
            return new MarkerVector(0, -Math.max(2, markerSize / 2), 0, 1);
        }
        int radius = Math.max(2, markerSize / 2);
        double angle = rotation * Math.PI * 2.0 / 16.0 - Math.PI / 2.0;
        int tipX = Math.round((float) Math.cos(angle) * radius);
        int tipY = Math.round((float) Math.sin(angle) * radius);
        int sideX = Math.round((float) Math.cos(angle + Math.PI / 2.0));
        int sideY = Math.round((float) Math.sin(angle + Math.PI / 2.0));
        return new MarkerVector(tipX, tipY, sideX, sideY);
    }

    record MarkerVector(int tipX, int tipY, int sideX, int sideY) {
    }

    record HeldMapHandPose(HudQuad leftSleeve, HudQuad leftHand, HudQuad rightSleeve, HudQuad rightHand) {
    }

    record HeldMapMarker(int x, int y, boolean edgeClamped) {
    }

    record HudQuad(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
        float[] vertices() {
            return new float[] { x1, y1, x2, y2, x3, y3, x4, y4 };
        }

        float minX() {
            return Math.min(Math.min(x1, x2), Math.min(x3, x4));
        }

        float maxX() {
            return Math.max(Math.max(x1, x2), Math.max(x3, x4));
        }

        float minY() {
            return Math.min(Math.min(y1, y2), Math.min(y3, y4));
        }

        float maxY() {
            return Math.max(Math.max(y1, y2), Math.max(y3, y4));
        }

        float centerX() {
            return (x1 + x2 + x3 + x4) * 0.25f;
        }
    }

    record HeldMapLayout(int mapX, int mapY, int displayPixels, int sourceStep, int cellSize, int frame) {
        int mapSize() {
            return displayPixels * cellSize;
        }

        int frameY() {
            return mapY - frame;
        }

        int bottomY() {
            return mapY + mapSize() + frame;
        }
    }

    static List<StatusEffectHudEntry> statusEffectHudEntries(List<StatusEffectInstance> effects,
            int screenWidth, int topMargin) {
        List<StatusEffectInstance> visible = new ArrayList<>();
        if (effects != null) {
            for (StatusEffectInstance effect : effects) {
                if (effect != null && !effect.expired()) {
                    visible.add(effect);
                }
            }
        }
        visible.sort(Comparator.comparingInt(effect -> effect.type().ordinal()));

        List<StatusEffectHudEntry> entries = new ArrayList<>(visible.size());
        int x = Math.max(2, screenWidth - STATUS_EFFECT_RIGHT_MARGIN - STATUS_EFFECT_ICON_SIZE);
        for (int i = 0; i < visible.size(); i++) {
            StatusEffectInstance effect = visible.get(i);
            int y = Math.max(2, topMargin) + i * (STATUS_EFFECT_ICON_SIZE + STATUS_EFFECT_SPACING);
            int color = StatusEffectVisuals.color(effect.type());
            entries.add(new StatusEffectHudEntry(
                    effect.type(),
                    x,
                    y,
                    STATUS_EFFECT_ICON_SIZE,
                    statusEffectDisplayName(effect.type()),
                    statusEffectDuration(effect.durationTicks()),
                    statusEffectAmplifier(effect.amplifier()),
                    statusEffectWarningAlpha(effect.durationTicks()),
                    effect.durationTicks(),
                    ((color >> 16) & 0xFF) / 255.0f,
                    ((color >> 8) & 0xFF) / 255.0f,
                    (color & 0xFF) / 255.0f,
                    statusEffectIconItem(effect.type())));
        }
        return entries;
    }

    static String statusEffectDuration(int durationTicks) {
        int seconds = Math.max(1, (Math.max(0, durationTicks) + 19) / 20);
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return minutes + ":" + (remainingSeconds < 10 ? "0" : "") + remainingSeconds;
    }

    static String statusEffectAmplifier(int amplifier) {
        int level = Math.max(0, amplifier) + 1;
        if (level <= 1) {
            return "";
        }
        String[] roman = { "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X" };
        if (level < roman.length) {
            return roman[level];
        }
        return String.valueOf(level);
    }

    static float statusEffectWarningAlpha(int durationTicks) {
        if (durationTicks > STATUS_EFFECT_LOW_TIME_TICKS) {
            return 1.0f;
        }
        return ((Math.max(0, durationTicks) / 5) % 2 == 0) ? 1.0f : 0.45f;
    }

    static int statusEffectDurationBarWidth(int durationTicks, int maxWidth) {
        if (maxWidth <= 0 || durationTicks <= 0) {
            return 0;
        }
        if (durationTicks > STATUS_EFFECT_LOW_TIME_TICKS) {
            return maxWidth;
        }
        float fraction = Math.max(0.0f, Math.min(1.0f, durationTicks / (float) STATUS_EFFECT_LOW_TIME_TICKS));
        return Math.max(1, Math.round(maxWidth * fraction));
    }

    private static String statusEffectDisplayName(StatusEffectType type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case SPEED -> "Speed";
            case SLOWNESS -> "Slowness";
            case HASTE -> "Haste";
            case MINING_FATIGUE -> "Mining Fatigue";
            case STRENGTH -> "Strength";
            case INSTANT_HEALTH -> "Instant Health";
            case INSTANT_DAMAGE -> "Instant Damage";
            case JUMP_BOOST -> "Jump Boost";
            case NAUSEA -> "Nausea";
            case REGENERATION -> "Regeneration";
            case RESISTANCE -> "Resistance";
            case FIRE_RESISTANCE -> "Fire Resistance";
            case WATER_BREATHING -> "Water Breathing";
            case INVISIBILITY -> "Invisibility";
            case BLINDNESS -> "Blindness";
            case NIGHT_VISION -> "Night Vision";
            case HUNGER -> "Hunger";
            case WEAKNESS -> "Weakness";
            case POISON -> "Poison";
        };
    }

    private static ItemType statusEffectIconItem(StatusEffectType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case SPEED -> ItemType.SUGAR;
            case SLOWNESS, WEAKNESS -> ItemType.FERMENTED_SPIDER_EYE;
            case STRENGTH -> ItemType.BLAZE_POWDER;
            case INSTANT_HEALTH, REGENERATION, RESISTANCE -> ItemType.GOLDEN_APPLE;
            case INSTANT_DAMAGE, POISON -> ItemType.SPIDER_EYE;
            case FIRE_RESISTANCE -> ItemType.MAGMA_CREAM;
            case HUNGER -> ItemType.ROTTEN_FLESH;
            case HASTE, MINING_FATIGUE -> ItemType.BLAZE_ROD;
            case WATER_BREATHING -> ItemType.POTION;
            case INVISIBILITY, BLINDNESS, NIGHT_VISION, NAUSEA, JUMP_BOOST -> ItemType.GHAST_TEAR;
        };
    }

    record StatusEffectHudEntry(StatusEffectType type, int x, int y, int iconSize,
            String displayName, String durationText, String amplifierText, float warningAlpha,
            int durationTicks, float red, float green, float blue, ItemType iconItem) {
    }

    private void drawHotbar(Inventory inventory, int centerX, int y) {
        if (inventory == null)
            return;

        ItemStack[] items = inventory.getHotbar();
        int selected = inventory.getSelectedSlot();

        // Use textured hotbar if gui texture is available
        if (guiTexture != null) {
            drawTexturedHotbar(inventory, centerX, y);
            return;
        }

        // Fallback to procedural hotbar
        int startX = centerX - (HOTBAR_WIDTH / 2);

        // 1. Draw 9 contiguous slots
        for (int i = 0; i < 9; i++) {
            int slotX = startX + (i * HOTBAR_SPACING);

            // 1. Background fill (Match Inventory Grey)
            drawSimpleSquare(slotX, y, HOTBAR_SLOT_SIZE, 0.55f, 0.55f, 0.55f, 1.0f);

            // 2. Draw item icon if exists (DRAW BEHIND BORDERS)
            ItemStack item = items[i];
            if (item != null && !item.isEmpty()) {
                // Remove +4 offset to center correctly (internal +2 offset handles padding)
                drawItemIcon(slotX, y, item);

                // Draw stack count (if > 1)
                if (item.getCount() > 1) {
                    drawStackCount(slotX, y, item.getCount());
                }
            }

            // 3. Borders (DRAW ON TOP) - Uniform Light Grey (User Request)
            int border = 4; // Thicker grid
            int innerH = HOTBAR_SLOT_SIZE - (border * 2);

            // Top Bar (Full Width)
            drawRect(slotX, y, HOTBAR_SLOT_SIZE, border, 0.75f, 0.75f, 0.75f, 1.0f);
            // Bottom Bar (Full Width)
            drawRect(slotX, y + HOTBAR_SLOT_SIZE - border, HOTBAR_SLOT_SIZE, border, 0.75f, 0.75f, 0.75f, 1.0f);
            // Left Bar (Inner Height)
            drawRect(slotX, y + border, border, innerH, 0.75f, 0.75f, 0.75f, 1.0f);
            // Right Bar (Inner Height)
            drawRect(slotX + HOTBAR_SLOT_SIZE - border, y + border, border, innerH, 0.75f, 0.75f, 0.75f, 1.0f);
        }

        // 2. Draw Selection Frame
        int selX = startX + (selected * HOTBAR_SPACING);
        int thickness = 5;
        int innerSize = HOTBAR_SLOT_SIZE;

        // White Selection Frame
        drawRect(selX - thickness, y - thickness, innerSize + (thickness * 2), thickness, 1.0f, 1.0f, 1.0f, 1.0f);
        drawRect(selX - thickness, y + innerSize, innerSize + (thickness * 2), thickness, 1.0f, 1.0f, 1.0f, 1.0f);
        drawRect(selX - thickness, y, thickness, innerSize, 1.0f, 1.0f, 1.0f, 1.0f);
        drawRect(selX + innerSize, y, thickness, innerSize, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Draw hotbar using textures from gui.png.
     * Hotbar is 182x22 pixels, scaled up to match our slot size.
     */
    private void drawTexturedHotbar(Inventory inventory, int centerX, int y) {
        ItemStack[] items = inventory.getHotbar();
        int selected = inventory.getSelectedSlot();

        float scale = HOTBAR_SLOT_SIZE / (float) HOTBAR_SLOT_TEXTURE_PITCH;
        int hotbarWidth = Math.round(HOTBAR_TEXTURE_WIDTH * scale);
        int hotbarHeight = Math.round(HOTBAR_TEXTURE_HEIGHT * scale);
        int startX = centerX - hotbarWidth / 2;
        int hotbarY = y;

        // Draw hotbar background from gui.png
        float[] hotbarUV = GuiTexture.getHotbarUV();

        shader.unbind();
        guiTexture.bind(0);
        texturedShader.bind();
        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
        texturedShader.setUniform("projection", ortho);
        texturedShader.setUniform("textureSampler", 0);
        texturedShader.setUniform("brightness", 1.0f);

        // Draw hotbar background
        drawTexturedQuad(
                startX, hotbarY,
                startX + hotbarWidth, hotbarY,
                startX + hotbarWidth, hotbarY + hotbarHeight,
                startX, hotbarY + hotbarHeight,
                hotbarUV[0], hotbarUV[1], hotbarUV[2], hotbarUV[3]);

        // Draw selection frame
        float[] selectionUV = GuiTexture.getHotbarSelectionUV();
        int selectionSize = Math.round(HOTBAR_SELECTION_TEXTURE_SIZE * scale);
        int slotWidth = Math.round(HOTBAR_SLOT_TEXTURE_PITCH * scale);
        int selX = startX
                + Math.round((selected * HOTBAR_SLOT_TEXTURE_PITCH + HOTBAR_SELECTION_TEXTURE_OFFSET) * scale);
        int selY = hotbarY + Math.round(HOTBAR_SELECTION_TEXTURE_OFFSET * scale);

        drawTexturedQuad(
                selX, selY,
                selX + selectionSize, selY,
                selX + selectionSize, selY + selectionSize,
                selX, selY + selectionSize,
                selectionUV[0], selectionUV[1], selectionUV[2], selectionUV[3]);

        texturedShader.unbind();
        guiTexture.unbind();
        shader.bind();
        shader.setUniform("projection", ortho);

        // Draw item icons on top of hotbar
        int itemSize = Math.round(HOTBAR_ITEM_TEXTURE_SIZE * scale);
        int itemInset = Math.round(HOTBAR_ITEM_TEXTURE_INSET * scale);

        for (int i = 0; i < 9; i++) {
            ItemStack item = items[i];
            if (item != null && !item.isEmpty()) {
                int itemX = startX + Math.round((i * HOTBAR_SLOT_TEXTURE_PITCH) * scale) + itemInset;
                int itemY = hotbarY + itemInset;

                if (item.getType().getRenderProfile().modelKind() == com.craftzero.inventory.ItemRenderProfile.ModelKind.BLOCK) {
                    drawIsometricBlockIcon(itemX, itemY, itemSize, item.getType());
                } else {
                    drawItemSprite(itemX, itemY, itemSize, item.getType());
                }
                drawDynamicItemOverlay(itemX, itemY, itemSize, item.getType());
                drawEnchantedItemOverlay(itemX, itemY, itemSize, item);

                // Draw stack count
                if (item.getCount() > 1) {
                    drawStackCountAt(startX + i * slotWidth, hotbarY, slotWidth, hotbarHeight, item.getCount());
                }
            }
        }
    }

    /**
     * Draw stack count at a specific slot position.
     */
    private void drawStackCountAt(int slotX, int slotY, int slotWidth, int slotHeight, int count) {
        if (drawBitmapStackCount(slotX, slotY, slotWidth, slotHeight, count, 3, 3)) {
            return;
        }
        String countStr = String.valueOf(count);
        int digitWidth = 6;
        int digitHeight = 8;
        int spacing = 1;
        int totalWidth = countStr.length() * (digitWidth + spacing) - spacing;

        int baseX = slotX + slotWidth - totalWidth - 3;
        int baseY = slotY + slotHeight - digitHeight - 3;

        // Draw shadow first, then digit
        for (int i = 0; i < countStr.length(); i++) {
            int digit = countStr.charAt(i) - '0';
            int dx = baseX + i * (digitWidth + spacing);
            drawDigit(dx + 1, baseY + 1, digit, 0.2f, 0.2f, 0.2f);
            drawDigit(dx, baseY, digit, 1.0f, 1.0f, 1.0f);
        }
    }

    private void drawRect(int x, int y, int w, int h, float r, float g, float b, float a) {
        if (w <= 0 || h <= 0) {
            return;
        }
        float[] vertices = {
                x, y,
                x + w, y,
                x + w, y + h,
                x, y + h
        };
        drawShape(vertices, 4, r, g, b, a);
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

    private void restoreColorShader() {
        if (shader == null) {
            return;
        }
        shader.bind();
        shader.setUniform("projection", new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1));
    }

    private void drawItemIcon(int x, int y, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        drawItemIcon(x, y, stack.getType());
        drawDynamicItemOverlay(x + 4, y + 4, HOTBAR_SLOT_SIZE - 8, stack.getType());
        drawEnchantedItemOverlay(x + 4, y + 4, HOTBAR_SLOT_SIZE - 8, stack);
    }

    private void drawItemIcon(int x, int y, ItemType type) {
        // If atlas is available, draw textured icon
        if (atlas != null) {
            if (type.getRenderProfile().modelKind() == com.craftzero.inventory.ItemRenderProfile.ModelKind.BLOCK) {
                // Blocks render as isometric 3D cubes
                drawIsometricBlockIcon(x + 4, y + 4, HOTBAR_SLOT_SIZE - 8, type);
            } else {
                // Items render as flat 2D sprites (like stick)
                drawItemSprite(x + 4, y + 4, HOTBAR_SLOT_SIZE - 8, type);
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

    /**
     * Draw an item as a flat 2D sprite (for sticks, tools, etc).
     */
    private void drawItemSprite(int x, int y, int size, ItemType type) {
        drawItemSprite(x, y, size, type, 1.0f);
    }

    private void drawItemSprite(int x, int y, int size, ItemType type, float alpha) {
        float[] uv;
        Texture texToUse;

        // Check if this item uses items.png
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
        texturedShader.setUniform("alpha", clamp(alpha, 0.0f, 1.0f));

        // Simple square sprite
        drawTexturedQuad(x, y, x + size, y, x + size, y + size, x, y + size,
                uv[0], uv[1], uv[2], uv[3]);

        texturedShader.setUniform("alpha", 1.0f);
        texturedShader.unbind();
        texToUse.unbind(); // Unbind correct texture
        shader.bind();
        shader.setUniform("projection", ortho);
    }

    /**
     * Draw an isometric 3D block icon using textures from the atlas.
     * Shows 3 faces: top, left side, right side (like Minecraft inventory).
     * Orientation: top corner pointing straight up at 45 degrees.
     */
    private void drawIsometricBlockIcon(int x, int y, int size, ItemType type) {
        drawIsometricBlockIcon(x, y, size, type, 1.0f);
    }

    private void drawIsometricBlockIcon(int x, int y, int size, ItemType type, float alpha) {
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
        texturedShader.setUniform("alpha", clamp(alpha, 0.0f, 1.0f));

        texturedShader.setUniform("brightness", BLOCK_ICON_TOP_BRIGHTNESS);
        drawTexturedQuad(
                cx, cy - quarterH, // Top corner (pointing up)
                cx + halfW, cy, // Right corner
                cx, cy + quarterH, // Bottom corner
                cx - halfW, cy, // Left corner
                topUV[0], topUV[1], topUV[2], topUV[3]);

        texturedShader.setUniform("brightness", BLOCK_ICON_LEFT_BRIGHTNESS);
        drawTexturedQuad(
                cx - halfW, cy, // Top-left
                cx, cy + quarterH, // Top-right
                cx, cy + quarterH + sideH, // Bottom-right
                cx - halfW, cy + sideH, // Bottom-left
                sideUV[0], sideUV[1], sideUV[2], sideUV[3]);

        texturedShader.setUniform("brightness", BLOCK_ICON_RIGHT_BRIGHTNESS);
        drawTexturedQuad(
                cx, cy + quarterH, // Top-left
                cx + halfW, cy, // Top-right
                cx + halfW, cy + sideH, // Bottom-right
                cx, cy + quarterH + sideH, // Bottom-left
                sideUV[0], sideUV[1], sideUV[2], sideUV[3]);

        texturedShader.setUniform("alpha", 1.0f);
        texturedShader.unbind();
        atlas.unbind();

        // Rebind color shader for subsequent draws
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

    private void drawArmor(Inventory inventory, int startX, int y) {
        int armor = inventory == null ? 0 : ArmorCalculator.armorPoints(inventory.getArmor());
        if (armor <= 0) {
            return;
        }
        if (iconsTexture == null) {
            for (int i = 0; i < 10; i++) {
                float fill = armor >= (i + 1) * 2 ? 1.0f : armor == i * 2 + 1 ? 0.55f : 0.18f;
                drawSquare(startX + i * SPACING, y, ICON_SIZE, fill, fill, fill, 1.0f);
            }
            return;
        }
        float scale = 2.0f;
        int iconSize = (int) (9 * scale);
        int spacing = iconSize + 2;
        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
        shader.unbind();
        iconsTexture.bind(0);
        texturedShader.bind();
        texturedShader.setUniform("projection", ortho);
        texturedShader.setUniform("textureSampler", 0);
        texturedShader.setUniform("brightness", 1.0f);
        float[] container = GuiTexture.getArmorContainerUV();
        float[] half = GuiTexture.getHalfArmorUV();
        float[] full = GuiTexture.getFullArmorUV();
        for (int i = 0; i < 10; i++) {
            int x = startX + i * spacing;
            drawTexturedQuad(x, y, x + iconSize, y, x + iconSize, y + iconSize, x, y + iconSize,
                    container[0], container[1], container[2], container[3]);
            if (armor >= (i + 1) * 2) {
                drawTexturedQuad(x, y, x + iconSize, y, x + iconSize, y + iconSize, x, y + iconSize,
                        full[0], full[1], full[2], full[3]);
            } else if (armor == i * 2 + 1) {
                drawTexturedQuad(x, y, x + iconSize, y, x + iconSize, y + iconSize, x, y + iconSize,
                        half[0], half[1], half[2], half[3]);
            }
        }
        texturedShader.unbind();
        iconsTexture.unbind();
        shader.bind();
        shader.setUniform("projection", ortho);
    }

    private void drawExperience(PlayerProgression progression, int centerX, int y) {
        if (progression == null || progression.getTotalExperience() <= 0) {
            return;
        }
        int width = 182 * 2;
        int height = 5 * 2;
        int x = centerX - width / 2;
        float fraction = progression.getExperienceToNextLevel() <= 0 ? 0.0f
                : progression.getExperienceIntoLevel() / (float) progression.getExperienceToNextLevel();
        int fill = Math.round(182.0f * Math.max(0.0f, Math.min(1.0f, fraction)));
        if (iconsTexture != null) {
            Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
            shader.unbind();
            iconsTexture.bind(0);
            texturedShader.bind();
            texturedShader.setUniform("projection", ortho);
            texturedShader.setUniform("textureSampler", 0);
            texturedShader.setUniform("brightness", 1.0f);
            float[] bg = GuiTexture.getXpBarBackgroundUV();
            drawTexturedQuad(x, y, x + width, y, x + width, y + height, x, y + height,
                    bg[0], bg[1], bg[2], bg[3]);
            if (fill > 0) {
                float[] fg = GuiTexture.getXpBarFillUV(fill);
                drawTexturedQuad(x, y, x + fill * 2, y, x + fill * 2, y + height, x, y + height,
                        fg[0], fg[1], fg[2], fg[3]);
            }
            texturedShader.unbind();
            iconsTexture.unbind();
            shader.bind();
            shader.setUniform("projection", ortho);
        } else {
            drawRect(x, y, width, height, 0.0f, 0.0f, 0.0f, 0.8f);
            drawRect(x, y, fill * 2, height, 0.3f, 0.85f, 0.05f, 1.0f);
        }
        if (textRenderer != null && progression.getLevel() > 0) {
            String level = String.valueOf(progression.getLevel());
            float scale = 1.5f;
            int textWidth = textRenderer.getStringWidth(level, scale);
            int tx = centerX - textWidth / 2;
            int ty = y - 18;
            float[] outline = new float[] { 0.0f, 0.25f, 0.0f, 1.0f };
            textRenderer.drawText(level, tx - 1, ty, scale, outline);
            textRenderer.drawText(level, tx + 1, ty, scale, outline);
            textRenderer.drawText(level, tx, ty - 1, scale, outline);
            textRenderer.drawText(level, tx, ty + 1, scale, outline);
            textRenderer.drawText(level, tx, ty, scale, new float[] { 0.50f, 1.0f, 0.10f, 1.0f });
            restoreColorShader();
        }
    }

    private void drawHearts(PlayerStats stats, int startX, int y) {
        int fullHearts = stats.getFullHearts();
        boolean hasHalf = stats.hasHalfHeart();

        // Use textured hearts if icons texture available
        if (iconsTexture != null) {
            drawTexturedHearts(stats, startX, y);
            return;
        }

        // Fallback to procedural hearts
        for (int i = 0; i < 10; i++) {
            int x = startX + (i * SPACING);

            float r, g, b, a;
            if (i < fullHearts) {
                r = 1.0f;
                g = 0.0f;
                b = 0.0f;
                a = 1.0f;
            } else if (i == fullHearts && hasHalf) {
                r = 1.0f;
                g = 0.5f;
                b = 0.5f;
                a = 1.0f;
            } else {
                r = 0.4f;
                g = 0.0f;
                b = 0.0f;
                a = 0.8f;
            }
            drawSquare(x, y, ICON_SIZE, r, g, b, a);
        }
    }

    /**
     * Draw hearts using textures from icons.png.
     */
    private void drawTexturedHearts(PlayerStats stats, int startX, int y) {
        int fullHearts = stats.getFullHearts();
        boolean hasHalf = stats.hasHalfHeart();

        float scale = 2.0f; // Scale up the 9x9 icons
        int iconSize = (int) (9 * scale);
        int spacing = iconSize + 2;

        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);

        shader.unbind();
        iconsTexture.bind(0);
        texturedShader.bind();
        texturedShader.setUniform("projection", ortho);
        texturedShader.setUniform("textureSampler", 0);
        texturedShader.setUniform("brightness", 1.0f);

        float[] containerUV = GuiTexture.getHeartContainerUV();
        float[] fullHeartUV = GuiTexture.getFullHeartUV();
        float[] halfHeartUV = GuiTexture.getHalfHeartUV();

        for (int i = 0; i < 10; i++) {
            int x = startX + (i * spacing);

            // Draw heart container (background)
            drawTexturedQuad(x, y, x + iconSize, y, x + iconSize, y + iconSize, x, y + iconSize,
                    containerUV[0], containerUV[1], containerUV[2], containerUV[3]);

            // Draw heart fill
            if (i < fullHearts) {
                drawTexturedQuad(x, y, x + iconSize, y, x + iconSize, y + iconSize, x, y + iconSize,
                        fullHeartUV[0], fullHeartUV[1], fullHeartUV[2], fullHeartUV[3]);
            } else if (i == fullHearts && hasHalf) {
                drawTexturedQuad(x, y, x + iconSize, y, x + iconSize, y + iconSize, x, y + iconSize,
                        halfHeartUV[0], halfHeartUV[1], halfHeartUV[2], halfHeartUV[3]);
            }
        }

        texturedShader.unbind();
        iconsTexture.unbind();
        shader.bind();
        shader.setUniform("projection", ortho);
    }

    private void drawHungerRight(PlayerStats stats, int startX, int y) {
        int fullBars = stats.getFullHungerBars();
        boolean hasHalf = stats.hasHalfHungerBar();

        // Use textured hunger if icons texture available
        if (iconsTexture != null) {
            drawTexturedHunger(stats, startX, y);
            return;
        }

        // Fallback to procedural
        for (int i = 0; i < 10; i++) {
            int x = startX + (i * SPACING);

            float r, g, b, a;
            if (i < fullBars) {
                r = 1.0f;
                g = 0.7f;
                b = 0.0f;
                a = 1.0f;
            } else if (i == fullBars && hasHalf) {
                r = 1.0f;
                g = 0.85f;
                b = 0.5f;
                a = 1.0f;
            } else {
                r = 0.4f;
                g = 0.3f;
                b = 0.0f;
                a = 0.8f;
            }
            drawSquare(x, y, ICON_SIZE, r, g, b, a);
        }
    }

    /**
     * Draw hunger icons using textures from icons.png.
     */
    private void drawTexturedHunger(PlayerStats stats, int startX, int y) {
        int fullBars = stats.getFullHungerBars();
        boolean hasHalf = stats.hasHalfHungerBar();

        float scale = 2.0f; // Scale up the 9x9 icons
        int iconSize = (int) (9 * scale);
        int spacing = iconSize + 2;

        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);

        shader.unbind();
        iconsTexture.bind(0);
        texturedShader.bind();
        texturedShader.setUniform("projection", ortho);
        texturedShader.setUniform("textureSampler", 0);
        texturedShader.setUniform("brightness", 1.0f);

        float[] containerUV = GuiTexture.getHungerContainerUV();
        float[] fullHungerUV = GuiTexture.getFullHungerUV();
        float[] halfHungerUV = GuiTexture.getHalfHungerUV();

        for (int i = 0; i < 10; i++) {
            int x = startX + (i * spacing);

            // Draw hunger container (background)
            drawTexturedQuad(x, y, x + iconSize, y, x + iconSize, y + iconSize, x, y + iconSize,
                    containerUV[0], containerUV[1], containerUV[2], containerUV[3]);

            // Draw hunger fill (drumstick)
            if (i < fullBars) {
                drawTexturedQuad(x, y, x + iconSize, y, x + iconSize, y + iconSize, x, y + iconSize,
                        fullHungerUV[0], fullHungerUV[1], fullHungerUV[2], fullHungerUV[3]);
            } else if (i == fullBars && hasHalf) {
                drawTexturedQuad(x, y, x + iconSize, y, x + iconSize, y + iconSize, x, y + iconSize,
                        halfHungerUV[0], halfHungerUV[1], halfHungerUV[2], halfHungerUV[3]);
            }
        }

        texturedShader.unbind();
        iconsTexture.unbind();
        shader.bind();
        shader.setUniform("projection", ortho);
    }

    /**
     * Draw breath bubbles.
     */
    private void drawBubbles(PlayerStats stats, int startX, int y, float deltaTime) {
        if (iconsTexture == null)
            return;

        float currentAir = stats.getCurrentAir();
        float airPerBubble = PlayerStats.MAX_AIR_SECONDS / 10.0f;

        Matrix4f ortho = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);

        shader.unbind();
        iconsTexture.bind(0);
        texturedShader.bind();
        texturedShader.setUniform("projection", ortho);
        texturedShader.setUniform("textureSampler", 0);
        texturedShader.setUniform("brightness", 1.0f);

        float[] fullBubbleUV = GuiTexture.getFullBubbleUV();
        float[] poppedBubbleUV = GuiTexture.getEmptyBubbleUV();

        float scale = 2.0f;
        int iconSize = (int) (9 * scale);
        int spacing = iconSize + 2;

        // Update logic for pops
        for (int i = 0; i < 10; i++) {
            float threshold = i * airPerBubble;
            boolean hadBubble = lastAir > threshold + 0.1f;
            boolean hasBubble = currentAir > threshold + 0.1f;

            if (hadBubble && !hasBubble) {
                bubblePopTimers[i] = 0.1f; // Very short pop animation
            }

            if (bubblePopTimers[i] > 0) {
                bubblePopTimers[i] -= deltaTime;
            }
        }

        // Draw loop
        for (int i = 0; i < 10; i++) {
            int x = startX + (i * spacing);

            float threshold = i * airPerBubble;
            boolean hasBubble = currentAir > threshold + 0.1f;

            float[] uvToUse = null;

            if (hasBubble) {
                uvToUse = fullBubbleUV;
            } else if (bubblePopTimers[i] > 0) {
                uvToUse = poppedBubbleUV;
            }

            if (uvToUse != null) {
                drawTexturedQuad(x, y, x + iconSize, y, x + iconSize, y + iconSize, x, y + iconSize,
                        uvToUse[0], uvToUse[1], uvToUse[2], uvToUse[3]);
            }
        }

        texturedShader.unbind();
        iconsTexture.unbind();
        shader.bind();
        shader.setUniform("projection", ortho);
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
        if (achievementTexture != null) {
            achievementTexture.cleanup();
            achievementTexture = null;
        }
        if (pumpkinOverlayTexture != null) {
            pumpkinOverlayTexture.cleanup();
            pumpkinOverlayTexture = null;
        }
        if (vignetteTexture != null) {
            vignetteTexture.cleanup();
            vignetteTexture = null;
        }
        if (portalOverlayTexture != null) {
            portalOverlayTexture.cleanup();
            portalOverlayTexture = null;
        }
        if (waterOverlayTexture != null) {
            waterOverlayTexture.cleanup();
            waterOverlayTexture = null;
        }
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }

    /**
     * Draw stack count as a number in the bottom-right of a slot.
     */
    private void drawStackCount(int slotX, int slotY, int count) {
        if (drawBitmapStackCount(slotX, slotY, HOTBAR_SLOT_SIZE, HOTBAR_SLOT_SIZE, count, 4, 4)) {
            return;
        }
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

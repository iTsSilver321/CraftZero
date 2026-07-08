package com.craftzero.ui.menu;

import com.craftzero.graphics.ItemTextureResolver;
import com.craftzero.graphics.ShaderProgram;
import com.craftzero.graphics.TextRenderer;
import com.craftzero.graphics.Texture;
import com.craftzero.graphics.TitlePanoramaRenderer;
import com.craftzero.inventory.ItemRenderProfile;
import com.craftzero.inventory.ItemType;
import com.craftzero.progression.AchievementType;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class MenuRenderer {
    private static final int TERRAIN_ATLAS_CELLS = 16;
    private static final float TERRAIN_CELL = 1.0f / TERRAIN_ATLAS_CELLS;
    private static final float TERRAIN_UV_INSET = 0.5f / 256.0f;
    private static final float ITEM_ICON_UV_INSET = 0.5f / 256.0f;
    private static final float BLOCK_ICON_HALF_WIDTH = 0.46f;
    private static final float BLOCK_ICON_TOP_HALF_HEIGHT = 0.23f;
    private static final float BLOCK_ICON_SIDE_HEIGHT = 0.52f;
    private static final float BLOCK_ICON_CENTER_X = 0.47f;
    private static final float BLOCK_ICON_CENTER_Y = 0.22f;
    private static final float BLOCK_ICON_TOP_BRIGHTNESS = 1.00f;
    private static final float BLOCK_ICON_LEFT_BRIGHTNESS = 0.60f;
    private static final float BLOCK_ICON_RIGHT_BRIGHTNESS = 0.45f;
    private static final int TEX_GRASS_TOP = 0;
    private static final int TEX_STONE = 1;
    private static final int TEX_DIRT = 2;
    private static final int TEX_GRASS_SIDE = 3;
    private static final int TEX_COBBLE = 16;
    private static final int TEX_SAND = 18;
    private static final int TEX_LOG_SIDE = 20;
    private static final int TEX_LEAVES = 53;
    private static final int TEX_WATER = 205;

    private ShaderProgram shader;
    private ShaderProgram texturedShader;
    private int vao;
    private int vbo;
    private int texturedVao;
    private int texturedVbo;
    private Matrix4f projection;
    private int width;
    private int height;
    private int guiScale = 1;
    private int logicalWidth;
    private int logicalHeight;
    private TextRenderer textRenderer;
    private Texture guiTexture;
    private Texture terrainTexture;
    private Texture itemsTexture;
    private Texture cloudsTexture;
    private Texture achievementTexture;
    private TitlePanoramaRenderer titlePanoramaRenderer;

    public void init(int width, int height, TextRenderer textRenderer) throws Exception {
        this.width = width;
        this.height = height;
        this.textRenderer = textRenderer;
        shader = new ShaderProgram();
        shader.createVertexShader("""
                #version 330 core
                layout (location = 0) in vec2 aPos;
                uniform mat4 projection;
                void main() {
                    gl_Position = projection * vec4(aPos, 0.0, 1.0);
                }
                """);
        shader.createFragmentShader("""
                #version 330 core
                out vec4 fragColor;
                uniform vec4 color;
                void main() {
                    fragColor = color;
                }
                """);
        shader.link();
        shader.createUniform("projection");
        shader.createUniform("color");

        texturedShader = new ShaderProgram();
        texturedShader.createVertexShader("""
                #version 330 core
                layout (location = 0) in vec2 aPos;
                layout (location = 1) in vec2 aUv;
                out vec2 texCoord;
                uniform mat4 projection;
                void main() {
                    texCoord = aUv;
                    gl_Position = projection * vec4(aPos, 0.0, 1.0);
                }
                """);
        texturedShader.createFragmentShader("""
                #version 330 core
                in vec2 texCoord;
                out vec4 fragColor;
                uniform sampler2D textureSampler;
                uniform vec4 tint;
                void main() {
                    fragColor = texture(textureSampler, texCoord) * tint;
                    if (fragColor.a < 0.01) {
                        discard;
                    }
                }
                """);
        texturedShader.link();
        texturedShader.createUniform("projection");
        texturedShader.createUniform("textureSampler");
        texturedShader.createUniform("tint");

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, Float.BYTES * 12, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        texturedVao = glGenVertexArrays();
        texturedVbo = glGenBuffers();
        glBindVertexArray(texturedVao);
        glBindBuffer(GL_ARRAY_BUFFER, texturedVbo);
        glBufferData(GL_ARRAY_BUFFER, Float.BYTES * 16, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2L * Float.BYTES);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        guiTexture = new Texture("/textures/gui/gui.png");
        terrainTexture = new Texture("/textures/terrain/Terrain.png");
        itemsTexture = new Texture("/textures/item/items.png");
        cloudsTexture = new Texture("/textures/environment/clouds.png");
        achievementTexture = new Texture("/textures/achievement/bg.png");
        titlePanoramaRenderer = new TitlePanoramaRenderer();
        titlePanoramaRenderer.init();
        updateOrtho(width, height);
    }

    public void updateOrtho(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.projection = new Matrix4f().ortho(0, this.width, this.height, 0, -1, 1);
        updateLogicalSize();
    }

    public void setGuiScale(int configuredScale) {
        this.guiScale = GuiScale.compute(configuredScale, width, height);
        updateLogicalSize();
    }

    public int guiScale() {
        return guiScale;
    }

    public int logicalWidth() {
        return logicalWidth;
    }

    public int logicalHeight() {
        return logicalHeight;
    }

    private void updateLogicalSize() {
        this.logicalWidth = Math.max(1, width / Math.max(1, guiScale));
        this.logicalHeight = Math.max(1, height / Math.max(1, guiScale));
    }

    public void renderPanoramaBackground(float time) {
        prepare2DState();
        drawPanoramaSky(time);
        drawPanoramaClouds(time);
        titlePanoramaRenderer.render(width, height, time);

        // The real Release 1.0 title panorama is blurred/dimmed behind the menu.
        drawBlurVeil(time);
        drawRect(0, 0, logicalWidth, logicalHeight, 0.0f, 0.0f, 0.0f, 0.23f);
        drawRect(0, 0, logicalWidth, 20, 1.0f, 1.0f, 1.0f, 0.08f);
    }

    private void drawBlurVeil(float time) {
        for (int i = 0; i < 12; i++) {
            float t = i / 11.0f;
            float y = i * logicalHeight / 12.0f;
            float wave = (float) Math.sin(time * 0.18f + i * 0.7f) * 0.018f;
            drawRect(0, y, logicalWidth, logicalHeight / 12.0f + 2,
                    0.54f + wave + t * 0.10f,
                    0.58f + wave + t * 0.08f,
                    0.52f + wave + t * 0.04f,
                    0.105f);
        }
    }

    private void drawPanoramaSky(float time) {
        int bands = 14;
        float daylight = 0.96f + (float) Math.sin(time * 0.08f) * 0.025f;
        for (int i = 0; i < bands; i++) {
            float t = i / (float) (bands - 1);
            float y = i * logicalHeight / (float) bands;
            drawRect(0, y, logicalWidth, logicalHeight / (float) bands + 1,
                    (0.34f + t * 0.22f) * daylight,
                    (0.56f + t * 0.23f) * daylight,
                    (0.86f + t * 0.12f) * daylight,
                    1.0f);
        }
    }

    private void drawPanoramaClouds(float time) {
        if (cloudsTexture == null) {
            return;
        }
        float drift = (time * 5.0f) % 96.0f;
        for (int row = 0; row < 2; row++) {
            float y = 20 + row * 34;
            float scale = row == 0 ? 76 : 92;
            for (float x = -scale - drift + row * 37; x < logicalWidth + scale; x += scale * 1.6f) {
                drawTexturedQuad(cloudsTexture, x, y, scale, scale * 0.5f,
                        0.0f, 0.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, row == 0 ? 0.45f : 0.34f);
            }
        }
    }

    private void drawPanoramaHills(float time) {
        float horizon = logicalHeight * 0.54f;
        float drift = (time * 3.0f) % 64.0f;
        for (int layer = 0; layer < 3; layer++) {
            float tile = 16 + layer * 4;
            float yBase = horizon + layer * 12;
            float alpha = 0.28f + layer * 0.12f;
            for (float x = -tile - drift * (0.35f + layer * 0.22f); x < logicalWidth + tile; x += tile) {
                int column = (int) Math.floor((x + drift) / tile);
                int height = 2 + Math.abs((column * 31 + layer * 17) % 4);
                for (int h = 0; h < height; h++) {
                    drawTerrainTile(TEX_STONE, x, yBase - h * tile, tile, 0.55f, 0.62f, 0.58f, alpha);
                }
                drawTerrainTile(TEX_GRASS_SIDE, x, yBase - height * tile, tile, 0.62f, 0.70f, 0.58f, alpha);
            }
        }
    }

    private void drawPanoramaTerrain(float time) {
        float tile = 16.0f;
        float scroll = (time * 8.0f) % tile;
        int startColumn = -4;
        int endColumn = (int) Math.ceil(logicalWidth / tile) + 5;
        float waterTop = logicalHeight - tile * 2.2f;

        for (int column = startColumn; column <= endColumn; column++) {
            float x = column * tile - scroll;
            int terrainWave = Math.round((float) Math.sin((column + time * 0.08f) * 0.68f) * 10.0f
                    + (float) Math.sin(column * 0.27f) * 5.0f);
            float groundY = logicalHeight * 0.68f + terrainWave;
            boolean shore = column % 9 == 0 || column % 9 == 1;
            int surface = shore ? TEX_SAND : TEX_GRASS_SIDE;
            drawTerrainTile(surface, x, groundY, tile, 1.0f, 1.0f, 1.0f, 1.0f);
            drawTerrainTile(shore ? TEX_SAND : TEX_DIRT, x, groundY + tile, tile, 0.94f, 0.94f, 0.94f, 1.0f);
            for (float y = groundY + tile * 2.0f; y < logicalHeight + tile; y += tile) {
                drawTerrainTile(y > logicalHeight - tile * 3.0f ? TEX_STONE : TEX_DIRT,
                        x, y, tile, 0.82f, 0.82f, 0.82f, 1.0f);
            }
            if (column % 11 == 4 || column % 13 == 7) {
                drawPanoramaTree(x, groundY - tile, tile);
            }
            if (column % 17 == 8) {
                drawTerrainTile(TEX_COBBLE, x, groundY - tile, tile, 0.9f, 0.9f, 0.9f, 1.0f);
            }
        }

        for (float x = -scroll; x < logicalWidth + tile; x += tile) {
            drawTerrainTile(TEX_WATER, x, waterTop, tile, 0.64f, 0.82f, 1.0f, 0.62f);
            drawTerrainTile(TEX_WATER, x, waterTop + tile, tile, 0.56f, 0.72f, 1.0f, 0.58f);
        }
    }

    private void drawPanoramaTree(float x, float baseY, float tile) {
        drawTerrainTile(TEX_LOG_SIDE, x, baseY, tile, 0.88f, 0.88f, 0.88f, 1.0f);
        drawTerrainTile(TEX_LOG_SIDE, x, baseY - tile, tile, 0.88f, 0.88f, 0.88f, 1.0f);
        drawTerrainTile(TEX_LOG_SIDE, x, baseY - tile * 2.0f, tile, 0.88f, 0.88f, 0.88f, 1.0f);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -4; dy <= -2; dy++) {
                if (Math.abs(dx) == 1 && dy == -4) {
                    continue;
                }
                drawTerrainTile(TEX_LEAVES, x + dx * tile, baseY + dy * tile, tile,
                        0.78f, 0.94f, 0.78f, 0.98f);
            }
        }
        drawTerrainTile(TEX_LEAVES, x, baseY - tile * 5.0f, tile, 0.78f, 0.94f, 0.78f, 0.98f);
    }

    public void renderDirtBackground() {
        prepare2DState();
        drawRect(0, 0, logicalWidth, logicalHeight, 0.05f, 0.04f, 0.03f, 1.0f);
        int tile = 32;
        if (terrainTexture == null) {
            drawFallbackDirtPattern(tile);
            return;
        }
        for (int y = -tile; y < logicalHeight + tile; y += tile) {
            for (int x = -tile; x < logicalWidth + tile; x += tile) {
                float shade = dirtTileShade(x / tile, y / tile);
                drawTerrainTile(TEX_DIRT, x, y, tile, shade, shade, shade, 1.0f);
            }
        }
        drawRect(0, 0, logicalWidth, logicalHeight, 0.0f, 0.0f, 0.0f, 0.42f);
        drawRect(0, 0, logicalWidth, 18, 0.0f, 0.0f, 0.0f, 0.22f);
        drawRect(0, logicalHeight - 22, logicalWidth, 22, 0.0f, 0.0f, 0.0f, 0.20f);
    }

    private void drawFallbackDirtPattern(int tile) {
        for (int y = 0; y < logicalHeight; y += tile) {
            for (int x = 0; x < logicalWidth; x += tile) {
                float shade = dirtTileShade(x / tile, y / tile);
                drawRect(x, y, tile, tile, 0.32f * shade, 0.22f * shade, 0.13f * shade, 1.0f);
            }
        }
        drawRect(0, 0, logicalWidth, logicalHeight, 0.0f, 0.0f, 0.0f, 0.36f);
    }

    private float dirtTileShade(int tileX, int tileY) {
        int hash = Math.floorMod(tileX * 37 + tileY * 19 + (tileX ^ tileY) * 11, 7);
        return 0.82f + hash * 0.025f;
    }

    public void drawButton(MenuButton button) {
        if (!button.visible()) {
            return;
        }
        drawClassicButtonTexture(button.x(), button.y(), button.width(), button.height(), button.visualState());
        float[] color = button.textColor();
        if (color == null) {
            color = defaultButtonTextColor(button);
        }
        String label = fitControlText(button.label(), button.width() - 8, 1.0f);
        drawCenteredText(label, button.x() + button.width() / 2,
                button.y() + (button.height() - 8) / 2, 1.0f, color);
    }

    private void drawClassicButtonTexture(int x, int y, int buttonWidth, int buttonHeight,
            ClassicGuiTexture.ButtonState state) {
        if (buttonWidth <= 0 || buttonHeight <= 0) {
            return;
        }
        if (guiTexture == null) {
            float shade = state == ClassicGuiTexture.ButtonState.DISABLED ? 0.28f
                    : state == ClassicGuiTexture.ButtonState.HOVERED ? 0.62f : 0.44f;
            drawRect(x, y, buttonWidth, buttonHeight, shade, shade, shade, 0.95f);
            return;
        }
        int leftWidth = Math.max(1, buttonWidth / 2);
        int rightWidth = Math.max(1, buttonWidth - leftWidth);
        UvRegion left = ClassicGuiTexture.buttonHalf(state, ClassicGuiTexture.ButtonHalf.LEFT);
        UvRegion right = ClassicGuiTexture.buttonHalf(state, ClassicGuiTexture.ButtonHalf.RIGHT);
        drawTexturedQuad(guiTexture, x, y, leftWidth, buttonHeight,
                left.u1(), left.v1(), left.u2(), left.v2(), 1.0f, 1.0f, 1.0f, 1.0f);
        drawTexturedQuad(guiTexture, x + leftWidth, y, rightWidth, buttonHeight,
                right.u1(), right.v1(), right.u2(), right.v2(), 1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static float[] defaultButtonTextColor(MenuButton button) {
        return defaultControlTextColor(button.enabled(), button.hovered());
    }

    private static float[] defaultControlTextColor(boolean enabled, boolean highlighted) {
        if (!enabled) {
            return new float[] { 160.0f / 255.0f, 160.0f / 255.0f, 160.0f / 255.0f, 1.0f };
        }
        if (highlighted) {
            return new float[] { 1.0f, 1.0f, 160.0f / 255.0f, 1.0f };
        }
        return new float[] { 224.0f / 255.0f, 224.0f / 255.0f, 224.0f / 255.0f, 1.0f };
    }

    public void drawSlider(MenuSlider slider) {
        if (!slider.visible()) {
            return;
        }
        ClassicGuiTexture.ButtonState state = sliderVisualState(slider);
        drawClassicButtonTexture(slider.x(), slider.y(), slider.width(), slider.height(), state);
        int knob = slider.x() + (int) Math.round(slider.normalizedValue() * (slider.width() - 8));
        drawClassicSliderHandle(knob, slider.y(), slider.height(), state);
        String label = fitControlText(slider.displayText(), slider.width() - 10, 1.0f);
        drawCenteredText(label, slider.x() + slider.width() / 2,
                slider.y() + (slider.height() - 8) / 2, 1.0f,
                defaultControlTextColor(slider.enabled(), slider.isHovered() || slider.isDragging()));
    }

    private String fitControlText(String text, int maxWidth, float scale) {
        if (text == null) {
            return "";
        }
        if (textRenderer == null || maxWidth <= 0 || textRenderer.getStringWidth(text, scale) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisWidth = textRenderer.getStringWidth(ellipsis, scale);
        if (ellipsisWidth > maxWidth) {
            return "";
        }
        String candidate = text;
        while (!candidate.isEmpty()
                && textRenderer.getStringWidth(candidate + ellipsis, scale) > maxWidth) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }
        return candidate.isEmpty() ? ellipsis : candidate + ellipsis;
    }

    private void drawClassicSliderHandle(int x, int y, int sliderHeight, ClassicGuiTexture.ButtonState state) {
        if (sliderHeight <= 0) {
            return;
        }
        if (guiTexture == null) {
            float shade = state == ClassicGuiTexture.ButtonState.DISABLED ? 0.45f : 0.8f;
            drawRect(x, y, 8, sliderHeight, shade, shade, shade, 1.0f);
            return;
        }
        int textureY = ClassicGuiTexture.buttonY(state);
        UvRegion left = UvRegion.fromPixels(0, textureY, 4, ClassicGuiTexture.BUTTON_HEIGHT,
                ClassicGuiTexture.ATLAS_WIDTH, ClassicGuiTexture.ATLAS_HEIGHT);
        UvRegion right = UvRegion.fromPixels(ClassicGuiTexture.BUTTON_WIDTH - 4, textureY, 4,
                ClassicGuiTexture.BUTTON_HEIGHT, ClassicGuiTexture.ATLAS_WIDTH, ClassicGuiTexture.ATLAS_HEIGHT);
        drawTexturedQuad(guiTexture, x, y, 4, sliderHeight,
                left.u1(), left.v1(), left.u2(), left.v2(), 1.0f, 1.0f, 1.0f, 1.0f);
        drawTexturedQuad(guiTexture, x + 4, y, 4, sliderHeight,
                right.u1(), right.v1(), right.u2(), right.v2(), 1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static ClassicGuiTexture.ButtonState sliderVisualState(MenuSlider slider) {
        if (!slider.enabled()) {
            return ClassicGuiTexture.ButtonState.DISABLED;
        }
        return slider.isHovered() || slider.isDragging()
                ? ClassicGuiTexture.ButtonState.HOVERED
                : ClassicGuiTexture.ButtonState.NORMAL;
    }

    public void drawTextField(TextField field) {
        if (!field.visible()) {
            return;
        }
        drawRect(field.x(), field.y(), field.width(), field.height(), 0.0f, 0.0f, 0.0f, 0.82f);
        drawRect(field.x() + 1, field.y() + 1, field.width() - 2, field.height() - 2,
                0.10f, 0.10f, 0.10f, 0.95f);
        drawRect(field.x() + 1, field.y() + 1, field.width() - 2, 1,
                field.focused() ? 0.92f : 0.48f, field.focused() ? 0.92f : 0.48f,
                field.focused() ? 0.92f : 0.48f, 1.0f);
        drawRect(field.x() + 1, field.y() + 1, 1, field.height() - 2,
                field.focused() ? 0.92f : 0.48f, field.focused() ? 0.92f : 0.48f,
                field.focused() ? 0.92f : 0.48f, 1.0f);
        drawRect(field.x() + 1, field.y() + field.height() - 2, field.width() - 2, 1,
                0.02f, 0.02f, 0.02f, 1.0f);
        drawRect(field.x() + field.width() - 2, field.y() + 1, 1, field.height() - 2,
                0.02f, 0.02f, 0.02f, 1.0f);

        VisibleFieldText visibleText = visibleFieldText(field, Math.max(1, field.width() - 10), 1.0f);
        int textX = field.x() + 5;
        int textY = field.y() + Math.max(5, (field.height() - 8) / 2);
        drawText(visibleText.text(), textX, textY, 1.0f,
                field.enabled()
                        ? new float[] { 1.0f, 1.0f, 1.0f, 1.0f }
                        : new float[] { 0.62f, 0.62f, 0.62f, 1.0f });
        if (field.focused()) {
            int cursorPrefixWidth = textRenderer == null
                    ? visibleText.cursorInText() * 6
                    : textRenderer.getStringWidth(visibleText.text().substring(0, visibleText.cursorInText()), 1.0f);
            drawRect(textX + cursorPrefixWidth, textY - 1, 1, 10, 0.90f, 0.90f, 0.90f, 1.0f);
        }
    }

    private VisibleFieldText visibleFieldText(TextField field, int maxWidth, float scale) {
        String value = field.value() == null ? "" : field.value();
        int cursor = Math.max(0, Math.min(value.length(), field.cursorIndex()));
        if (textRenderer == null || textRenderer.getStringWidth(value, scale) <= maxWidth) {
            return new VisibleFieldText(value, cursor);
        }
        int start = 0;
        int end = value.length();
        while (start < cursor && textRenderer.getStringWidth(value.substring(start, end), scale) > maxWidth) {
            start++;
        }
        while (end > cursor && textRenderer.getStringWidth(value.substring(start, end), scale) > maxWidth) {
            end--;
        }
        while (start < end && textRenderer.getStringWidth(value.substring(start, end), scale) > maxWidth) {
            start++;
        }
        String visible = value.substring(start, end);
        return new VisibleFieldText(visible, Math.max(0, Math.min(visible.length(), cursor - start)));
    }

    private record VisibleFieldText(String text, int cursorInText) {
    }

    public <T> void drawList(MenuList<T> list) {
        if (!list.visible()) {
            return;
        }
        if (list.id().startsWith("statistics-")) {
            drawStatisticsList(list);
            return;
        }
        int height = list.rowHeight() * list.visibleRows();
        drawRect(list.x(), list.y(), list.width(), height, 0.01f, 0.01f, 0.01f, 0.66f);
        drawOutline(list.x(), list.y(), list.width(), height, 0.0f, 0.0f, 0.0f, 1.0f);
        drawRect(list.x() + 1, list.y() + 1, list.width() - 2, 1, 0.58f, 0.58f, 0.58f, 0.26f);
        for (MenuList.Row<T> row : list.visibleRowEntries()) {
            Rect bounds = row.bounds();
            if ((row.index() & 1) == 0) {
                drawRect(bounds.x() + 1, bounds.y() + 1, bounds.width() - 2, bounds.height() - 1,
                        0.10f, 0.10f, 0.10f, 0.32f);
            }
            if (row.selected() || row.hovered()) {
                float shade = row.selected() ? 0.42f : 0.28f;
                drawRect(bounds.x() + 2, bounds.y() + 1, bounds.width() - 4, bounds.height() - 2,
                        shade, shade, shade, row.selected() ? 0.82f : 0.50f);
            }
            String label = fitListText(row.label(), Math.max(1, bounds.width() - 14), 1.0f);
            int labelWidth = textRenderer == null ? 0 : textRenderer.getStringWidth(label, 1.0f);
            drawText(label, bounds.centerX() - labelWidth / 2, bounds.y() + Math.max(3, (bounds.height() - 8) / 2),
                    1.0f, new float[] { 0.92f, 0.92f, 0.92f, 1.0f });
        }
        drawListScrollbar(list, height);
    }

    private <T> void drawListScrollbar(MenuList<T> list, int listHeight) {
        int total = list.items().size();
        int visible = list.visibleRows();
        if (total <= visible) {
            return;
        }
        int trackX = list.x() + list.width() - 5;
        drawRect(trackX, list.y() + 1, 3, listHeight - 2, 0.0f, 0.0f, 0.0f, 0.42f);
        int barHeight = Math.max(12, (listHeight - 2) * visible / Math.max(1, total));
        int maxOffset = Math.max(1, total - visible);
        int travel = Math.max(1, listHeight - 2 - barHeight);
        int barY = list.y() + 1 + Math.round(travel * (list.scrollOffset() / (float) maxOffset));
        drawRect(trackX, barY, 3, barHeight, 0.78f, 0.78f, 0.78f, 0.76f);
    }

    private <T> void drawStatisticsList(MenuList<T> list) {
        int x = list.x();
        int y = list.y();
        int width = list.width();
        int height = list.rowHeight() * list.visibleRows();
        drawRect(x, y, width, height, 0.01f, 0.01f, 0.01f, 0.62f);
        drawOutline(x, y, width, height, 0.0f, 0.0f, 0.0f, 1.0f);
        drawRect(x + 1, y + 1, width - 2, 1, 0.62f, 0.62f, 0.62f, 0.35f);

        for (MenuList.Row<T> row : list.visibleRowEntries()) {
            drawStatisticsRow(list, row);
        }
        drawStatisticsScrollbar(list, height);
    }

    private <T> void drawStatisticsRow(MenuList<T> list, MenuList.Row<T> row) {
        Rect bounds = row.bounds();
        String label = row.label() == null ? "" : row.label();
        boolean header = label.startsWith("#");
        boolean even = (row.index() & 1) == 0;
        int textY = statisticsTextY(bounds);
        if (header) {
            drawRect(bounds.x() + 1, bounds.y() + 1, bounds.width() - 2, bounds.height() - 1,
                    0.20f, 0.18f, 0.12f, 0.78f);
            drawRect(bounds.x() + 1, bounds.y() + bounds.height() - 1, bounds.width() - 2, 1,
                    0.64f, 0.60f, 0.42f, 0.48f);
        } else if (even) {
            drawRect(bounds.x() + 1, bounds.y() + 1, bounds.width() - 2, bounds.height() - 1,
                    0.10f, 0.10f, 0.10f, 0.42f);
        }
        if (!header && (row.selected() || row.hovered())) {
            drawRect(bounds.x() + 2, bounds.y() + 1, bounds.width() - 4, bounds.height() - 2,
                    row.selected() ? 0.40f : 0.28f,
                    row.selected() ? 0.40f : 0.28f,
                    row.selected() ? 0.40f : 0.28f,
                    row.selected() ? 0.78f : 0.52f);
        }

        if (header) {
            label = label.substring(1);
        }
        if (label.indexOf('\t') >= 0) {
            drawStatisticsTableRow(list, bounds, label, header);
            return;
        }

        int separator = label.indexOf(": ");
        if (separator < 0) {
            drawCenteredText(fitListText(label, Math.max(1, bounds.width() - 12), 1.0f),
                    bounds.centerX(), textY, 1.0f, new float[] { 0.72f, 0.72f, 0.72f, 1.0f });
            return;
        }

        int leftX = bounds.x() + 8;
        int rightPadding = 8;
        int rightEdge = bounds.right() - rightPadding;
        int splitX = bounds.x() + Math.max(120, Math.round(bounds.width() * 0.58f));
        String left = fitListText(label.substring(0, separator), Math.max(1, splitX - leftX - 6), 1.0f);
        String right = fitListText(label.substring(separator + 2),
                Math.max(1, rightEdge - splitX), 1.0f);
        int rightWidth = textRenderer == null ? 0 : textRenderer.getStringWidth(right, 1.0f);
        drawText(left, leftX, textY, 1.0f, new float[] { 0.92f, 0.92f, 0.92f, 1.0f });
        drawText(right, rightEdge - rightWidth, textY, 1.0f,
                new float[] { 0.82f, 0.82f, 0.82f, 1.0f });
    }

    private <T> void drawStatisticsTableRow(MenuList<T> list, Rect bounds, String label, boolean header) {
        String[] columns = label.split("\\t", -1);
        if (columns.length <= 1) {
            drawCenteredText(fitListText(label, Math.max(1, bounds.width() - 12), 1.0f),
                    bounds.centerX(), statisticsTextY(bounds), 1.0f,
                    header ? new float[] { 1.0f, 1.0f, 0.63f, 1.0f }
                            : new float[] { 0.72f, 0.72f, 0.72f, 1.0f });
            return;
        }

        int leftX = bounds.x() + 6;
        int rightEdge = bounds.right() - (statisticsHasScrollbar(list) ? 10 : 7);
        int valueColumns = Math.max(1, columns.length - 1);
        boolean objectCounterRow = columns.length >= 6;
        int nameWidth;
        int valueWidth;
        int valueAreaX;
        if (objectCounterRow) {
            int available = Math.max(1, rightEdge - leftX);
            valueWidth = Math.max(24, Math.min(36, (available - 72) / valueColumns));
            int valueAreaWidth = Math.max(valueColumns * 20, valueWidth * valueColumns);
            valueAreaX = Math.max(leftX + 62, rightEdge - valueAreaWidth);
            nameWidth = Math.max(56, valueAreaX - leftX - 7);
        } else {
            int minValueWidth = 72;
            int preferredNameWidth = Math.round(bounds.width() * 0.62f);
            int maxNameWidth = Math.max(56, rightEdge - leftX - valueColumns * minValueWidth - 6);
            nameWidth = Math.max(56, Math.min(preferredNameWidth, maxNameWidth));
            valueAreaX = leftX + nameWidth + 6;
            valueWidth = Math.max(1, (rightEdge - valueAreaX) / valueColumns);
        }
        int textY = statisticsTextY(bounds);
        float[] nameColor = header
                ? new float[] { 1.0f, 1.0f, 0.63f, 1.0f }
                : new float[] { 0.92f, 0.92f, 0.92f, 1.0f };
        float[] valueColor = header
                ? new float[] { 1.0f, 1.0f, 0.63f, 1.0f }
                : new float[] { 0.82f, 0.82f, 0.82f, 1.0f };

        drawText(fitListText(columns[0], Math.max(1, nameWidth - 4), 1.0f),
                leftX, textY, 1.0f, nameColor);
        drawRect(valueAreaX - 4, bounds.y() + 2, 1, bounds.height() - 4,
                0.52f, 0.52f, 0.52f, header ? 0.46f : 0.24f);

        for (int i = 1; i < columns.length; i++) {
            int cellX = valueAreaX + (i - 1) * valueWidth;
            int cellRight = i == columns.length - 1 ? rightEdge : cellX + valueWidth - 2;
            if (i > 1) {
                drawRect(cellX - 3, bounds.y() + 2, 1, bounds.height() - 4,
                        0.36f, 0.36f, 0.36f, header ? 0.36f : 0.16f);
            }
            String value = fitListText(columns[i], Math.max(1, cellRight - cellX - 2), 1.0f);
            int valueWidthPixels = textRenderer == null ? 0 : textRenderer.getStringWidth(value, 1.0f);
            drawText(value, Math.max(cellX, cellRight - valueWidthPixels), textY, 1.0f, valueColor);
        }
    }

    private int statisticsTextY(Rect bounds) {
        return bounds.y() + Math.max(2, (bounds.height() - 8) / 2);
    }

    private boolean statisticsHasScrollbar(MenuList<?> list) {
        return list.items().size() > list.visibleRows();
    }

    private <T> void drawStatisticsScrollbar(MenuList<T> list, int listHeight) {
        int total = list.items().size();
        int visible = list.visibleRows();
        if (total <= visible) {
            return;
        }
        int trackX = list.x() + list.width() - 5;
        drawRect(trackX, list.y() + 1, 3, listHeight - 2, 0.0f, 0.0f, 0.0f, 0.45f);
        int barHeight = Math.max(12, (listHeight - 2) * visible / Math.max(1, total));
        int maxOffset = Math.max(1, total - visible);
        int travel = Math.max(1, listHeight - 2 - barHeight);
        int barY = list.y() + 1 + Math.round(travel * (list.scrollOffset() / (float) maxOffset));
        drawRect(trackX, barY, 3, barHeight, 0.78f, 0.78f, 0.78f, 0.78f);
    }

    public void drawLabel(MenuLabel label) {
        if (!label.visible()) {
            return;
        }
        if (label.centered() && Math.abs(label.rotationDegrees()) > 0.001f) {
            drawRotatedCenteredText(label.text(), label.bounds().centerX(),
                    label.bounds().y() + label.bounds().height() / 2,
                    label.scale(), label.rotationDegrees(), label.color());
            return;
        }
        if (label.centered()) {
            drawCenteredText(label.text(), label.bounds().centerX(), label.bounds().y(), label.scale(), label.color());
        } else {
            drawText(label.text(), label.bounds().x(), label.bounds().y(), label.scale(), label.color());
        }
    }

    public void drawTitle(String text, int y, float scale) {
        drawCenteredText(text, logicalWidth / 2, y, scale, new float[] { 1, 1, 1, 1 });
    }

    public void drawCenteredText(String text, int centerX, int y, float scale, float[] color) {
        if (textRenderer == null) {
            return;
        }
        int textWidth = textRenderer.getStringWidth(text, scale);
        drawText(text, centerX - textWidth / 2, y, scale, color);
    }

    public void drawRotatedCenteredText(String text, int centerX, int centerY, float scale, float rotationDegrees,
            float[] color) {
        if (textRenderer == null) {
            return;
        }
        int px = Math.round(centerX * guiScale);
        int py = Math.round(centerY * guiScale);
        float scaledText = scale * guiScale;
        textRenderer.drawTextRotatedCentered(text, px + guiScale, py + guiScale, scaledText, rotationDegrees,
                new float[] { 0, 0, 0, color[3] });
        textRenderer.drawTextRotatedCentered(text, px, py, scaledText, rotationDegrees, color);
    }

    public void drawText(String text, int x, int y, float scale, float[] color) {
        if (textRenderer == null) {
            return;
        }
        int px = Math.round(x * guiScale);
        int py = Math.round(y * guiScale);
        float scaledText = scale * guiScale;
        textRenderer.drawText(text, px + guiScale, py + guiScale, scaledText, new float[] { 0, 0, 0, color[3] });
        textRenderer.drawText(text, px, py, scaledText, color);
    }

    public void drawRect(float x, float y, float rectWidth, float rectHeight, float r, float g, float b, float a) {
        prepare2DState();
        shader.bind();
        shader.setUniform("projection", projection);
        shader.setUniform("color", new Vector4f(r, g, b, a));
        float px = x * guiScale;
        float py = y * guiScale;
        float pw = rectWidth * guiScale;
        float ph = rectHeight * guiScale;
        float[] vertices = {
                px, py,
                px + pw, py,
                px + pw, py + ph,
                px, py,
                px + pw, py + ph,
                px, py + ph
        };
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        shader.unbind();
    }

    public void drawItemIcon(ItemType type, float x, float y, float size, float alpha) {
        if (type == null) {
            return;
        }
        if (type.getRenderProfile().modelKind() == ItemRenderProfile.ModelKind.BLOCK) {
            drawBlockItemIcon(type, x, y, size, alpha, 1.0f);
            return;
        }
        boolean usesItemsAtlas = ItemTextureResolver.usesItemsAtlas(type);
        Texture texture = usesItemsAtlas ? itemsTexture : terrainTexture;
        float[] uv = ItemTextureResolver.getUv(type);
        float[] insetUv = insetIconUv(uv);
        float u1 = insetUv[0];
        float v1 = insetUv[1];
        float u2 = insetUv[2];
        float v2 = insetUv[3];
        drawTexturedQuad(texture, x, y, size, size, u1, v1, u2, v2, 1.0f, 1.0f, 1.0f, alpha);
    }

    private void drawBlockItemIcon(ItemType type, float x, float y, float size, float alpha, float shade) {
        if (terrainTexture == null) {
            return;
        }
        float[] topUV = insetIconUv(type.getTextureCoords(0));
        float[] sideUV = insetIconUv(type.getTextureCoords(2));
        float halfW = size * BLOCK_ICON_HALF_WIDTH;
        float quarterH = size * BLOCK_ICON_TOP_HALF_HEIGHT;
        float sideH = size * BLOCK_ICON_SIDE_HEIGHT;
        float cx = x + size * BLOCK_ICON_CENTER_X;
        float cy = y + size * BLOCK_ICON_CENTER_Y;

        drawTexturedQuad(terrainTexture,
                cx, cy - quarterH,
                cx + halfW, cy,
                cx, cy + quarterH,
                cx - halfW, cy,
                topUV[0], topUV[1], topUV[2], topUV[3],
                shade * BLOCK_ICON_TOP_BRIGHTNESS,
                shade * BLOCK_ICON_TOP_BRIGHTNESS,
                shade * BLOCK_ICON_TOP_BRIGHTNESS,
                alpha);

        drawTexturedQuad(terrainTexture,
                cx - halfW, cy,
                cx, cy + quarterH,
                cx, cy + quarterH + sideH,
                cx - halfW, cy + sideH,
                sideUV[0], sideUV[1], sideUV[2], sideUV[3],
                shade * BLOCK_ICON_LEFT_BRIGHTNESS,
                shade * BLOCK_ICON_LEFT_BRIGHTNESS,
                shade * BLOCK_ICON_LEFT_BRIGHTNESS,
                alpha);

        drawTexturedQuad(terrainTexture,
                cx, cy + quarterH,
                cx + halfW, cy,
                cx + halfW, cy + sideH,
                cx, cy + quarterH + sideH,
                sideUV[0], sideUV[1], sideUV[2], sideUV[3],
                shade * BLOCK_ICON_RIGHT_BRIGHTNESS,
                shade * BLOCK_ICON_RIGHT_BRIGHTNESS,
                shade * BLOCK_ICON_RIGHT_BRIGHTNESS,
                alpha);
    }

    private void drawTerrainTile(int textureIndex, float x, float y, float size,
            float r, float g, float b, float a) {
        int cellX = Math.floorMod(textureIndex, TERRAIN_ATLAS_CELLS);
        int cellY = Math.floorDiv(textureIndex, TERRAIN_ATLAS_CELLS);
        float u1 = cellX * TERRAIN_CELL + TERRAIN_UV_INSET;
        float v1 = cellY * TERRAIN_CELL + TERRAIN_UV_INSET;
        float u2 = (cellX + 1) * TERRAIN_CELL - TERRAIN_UV_INSET;
        float v2 = (cellY + 1) * TERRAIN_CELL - TERRAIN_UV_INSET;
        drawTexturedQuad(terrainTexture, x, y, size, size, u1, v1, u2, v2, r, g, b, a);
    }

    private void drawTexturedQuad(Texture texture, float x, float y, float rectWidth, float rectHeight,
            float u1, float v1, float u2, float v2,
            float r, float g, float b, float a) {
        drawTexturedQuad(texture,
                x, y,
                x + rectWidth, y,
                x + rectWidth, y + rectHeight,
                x, y + rectHeight,
                u1, v1, u2, v2,
                r, g, b, a);
    }

    private void drawTexturedQuad(Texture texture,
            float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4,
            float u1, float v1, float u2, float v2,
            float r, float g, float b, float a) {
        if (texture == null || texturedShader == null || texturedVao == 0) {
            return;
        }
        prepare2DState();
        texture.bind(0);
        texturedShader.bind();
        texturedShader.setUniform("projection", projection);
        texturedShader.setUniform("textureSampler", 0);
        texturedShader.setUniform("tint", new Vector4f(r, g, b, a));

        float[] vertices = {
                x1 * guiScale, y1 * guiScale, u1, v1,
                x2 * guiScale, y2 * guiScale, u2, v1,
                x3 * guiScale, y3 * guiScale, u2, v2,
                x4 * guiScale, y4 * guiScale, u1, v2
        };
        glBindVertexArray(texturedVao);
        glBindBuffer(GL_ARRAY_BUFFER, texturedVbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        texturedShader.unbind();
        texture.unbind();
    }

    private static float[] insetIconUv(float[] uv) {
        return new float[] {
                Math.min(uv[0] + ITEM_ICON_UV_INSET, uv[2]),
                Math.min(uv[1] + ITEM_ICON_UV_INSET, uv[3]),
                Math.max(uv[2] - ITEM_ICON_UV_INSET, uv[0]),
                Math.max(uv[3] - ITEM_ICON_UV_INSET, uv[1])
        };
    }

    public void drawAchievementTree(AchievementTreeComponent tree) {
        if (!tree.visible()) {
            return;
        }
        Rect bounds = tree.bounds();
        drawAchievementFrame(bounds);
        drawAchievementBackground(tree);

        for (AchievementType type : tree.achievements()) {
            AchievementType parent = type.parent();
            if (parent != null && tree.isNodeVisible(type) && tree.isNodeVisible(parent)) {
                drawAchievementLink(tree, parent, type);
            }
        }
        for (AchievementType type : tree.achievements()) {
            if (tree.isNodeVisible(type)) {
                drawAchievementNode(tree, type);
            }
        }
        drawAchievementFrameOverlay(bounds);
        drawAchievementScrollbars(tree);
        drawAchievementTooltip(tree);
    }

    private void drawAchievementFrame(Rect bounds) {
        drawRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                0.03f, 0.03f, 0.03f, 0.92f);
        if (achievementTexture != null) {
            drawTexturedQuad(achievementTexture, bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    0.0f, 0.0f, 1.0f, 202.0f / 256.0f,
                    0.92f, 0.92f, 0.92f, 1.0f);
            return;
        }
        drawRect(bounds.x(), bounds.y(), bounds.width(), 2, 0.78f, 0.78f, 0.78f, 1.0f);
        drawRect(bounds.x(), bounds.y(), 2, bounds.height(), 0.78f, 0.78f, 0.78f, 1.0f);
        drawRect(bounds.x(), bounds.bottom() - 2, bounds.width(), 2, 0.18f, 0.18f, 0.18f, 1.0f);
        drawRect(bounds.right() - 2, bounds.y(), 2, bounds.height(), 0.18f, 0.18f, 0.18f, 1.0f);
    }

    private void drawAchievementFrameOverlay(Rect bounds) {
        Rect viewport = achievementViewport(bounds);
        drawOutline(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 0.0f, 0.0f, 0.0f, 1.0f);
        drawOutline(viewport.x() - 1, viewport.y() - 1, viewport.width() + 2, viewport.height() + 2,
                0.08f, 0.08f, 0.08f, 0.95f);
    }

    private Rect achievementViewport(Rect bounds) {
        int inset = Math.min(14, Math.max(4, Math.min(bounds.width(), bounds.height()) / 5));
        return new Rect(
                bounds.x() + inset,
                bounds.y() + inset,
                Math.max(1, bounds.width() - inset * 2),
                Math.max(1, bounds.height() - inset * 2));
    }

    private void drawAchievementBackground(AchievementTreeComponent tree) {
        Rect bounds = achievementViewport(tree.bounds());
        int tile = 16;
        int startX = bounds.x() - Math.floorMod(tree.scrollX(), tile);
        int startY = bounds.y() - Math.floorMod(tree.scrollY(), tile);
        for (int y = startY; y < bounds.bottom(); y += tile) {
            for (int x = startX; x < bounds.right(); x += tile) {
                int contentX = x - bounds.x() + tree.scrollX();
                int contentY = y - bounds.y() + tree.scrollY();
                ItemType block = achievementBackgroundBlock(tree, contentX, contentY);
                float shade = backgroundShade(contentX, contentY);
                drawItemIconClipped(block, x, y, tile, bounds, shade, 0.82f);
            }
        }
        drawRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                0.0f, 0.0f, 0.0f, 0.24f);
    }

    private ItemType achievementBackgroundBlock(AchievementTreeComponent tree, int contentX, int contentY) {
        if (contentY < 20) {
            return ItemType.GRASS;
        }
        float depth = contentY / (float) Math.max(1, tree.contentHeight());
        if (depth < 0.24f) {
            return ItemType.DIRT;
        }
        if (contentY > tree.contentHeight() - 48) {
            return ItemType.BEDROCK;
        }
        int hash = Math.floorMod(contentX * 31 + contentY * 17, 227);
        if (hash == 7) {
            return ItemType.DIAMOND_ORE;
        }
        if (hash == 23) {
            return ItemType.GOLD_ORE;
        }
        if (hash == 41 || hash == 87) {
            return ItemType.IRON_ORE;
        }
        if (hash == 61 || hash == 143) {
            return ItemType.COAL_ORE;
        }
        if (hash == 109) {
            return ItemType.LAPIS_ORE;
        }
        return ItemType.STONE;
    }

    private float backgroundShade(int contentX, int contentY) {
        int hash = Math.floorMod(contentX * 13 + contentY * 19, 5);
        return 0.54f + hash * 0.035f;
    }

    private void drawAchievementLink(AchievementTreeComponent tree, AchievementType parent, AchievementType child) {
        Rect bounds = achievementViewport(tree.bounds());
        Rect parentRect = tree.nodeScreenRect(parent);
        Rect childRect = tree.nodeScreenRect(child);
        int parentX = parentRect.centerX();
        int parentY = parentRect.centerY();
        int childX = childRect.centerX();
        int childY = childRect.centerY();
        float[] color = tree.isUnlocked(child)
                ? new float[] { 0.82f, 0.82f, 0.82f, 1.0f }
                : tree.isAvailable(child)
                        ? new float[] { 0.45f, 0.68f, 0.45f, 1.0f }
                        : new float[] { 0.20f, 0.20f, 0.20f, 1.0f };
        int left = Math.min(parentX, childX);
        int width = Math.max(2, Math.abs(parentX - childX));
        int top = Math.min(parentY, childY);
        int height = Math.max(2, Math.abs(parentY - childY));
        float[] shadow = new float[] { 0.0f, 0.0f, 0.0f, 0.75f };
        drawClippedRect(bounds, left - 1, parentY - 2, width + 2, 4, shadow);
        drawClippedRect(bounds, childX - 2, top - 1, 4, height + 2, shadow);
        drawClippedRect(bounds, left, parentY - 1, width, 2, color);
        drawClippedRect(bounds, childX - 1, top, 2, height, color);
    }

    private void drawAchievementNode(AchievementTreeComponent tree, AchievementType type) {
        Rect bounds = achievementViewport(tree.bounds());
        Rect node = tree.nodeScreenRect(type);
        if (!intersects(bounds, node)) {
            return;
        }
        AchievementTreeComponent.NodeState state = tree.nodeState(type);
        float[] color = tree.colorFor(type);
        boolean selected = type == tree.selectedAchievement();
        boolean hovered = type == tree.hoveredAchievement() && tree.hasDetails(type);
        float fill = state == AchievementTreeComponent.NodeState.UNLOCKED ? 0.30f
                : state == AchievementTreeComponent.NodeState.AVAILABLE ? 0.22f : 0.09f;
        drawClippedRect(bounds, node.x() + 2, node.y() + 2, node.width(), node.height(),
                new float[] { 0.0f, 0.0f, 0.0f, 0.62f });
        drawClippedRect(bounds, node.x(), node.y(), node.width(), node.height(),
                new float[] { 0.04f, 0.04f, 0.04f, 0.96f });
        drawClippedRect(bounds, node.x() + 1, node.y() + 1, node.width() - 2, node.height() - 2,
                new float[] { 0.70f, 0.70f, 0.70f, state == AchievementTreeComponent.NodeState.LOCKED ? 0.36f : 0.72f });
        drawClippedRect(bounds, node.x() + 2, node.y() + 2, node.width() - 4, node.height() - 4,
                new float[] { fill, fill, fill, 0.98f });
        if (type.special()) {
            drawNodeOutline(bounds, node.x() - 2, node.y() - 2, node.width() + 4, node.height() + 4,
                    0.95f, 0.72f, 0.22f, 1.0f);
            drawNodeOutline(bounds, node.x() - 1, node.y() - 1, node.width() + 2, node.height() + 2,
                    0.20f, 0.12f, 0.02f, 0.95f);
        }
        drawNodeOutline(bounds, node.x(), node.y(), node.width(), node.height(),
                color[0], color[1], color[2], 1.0f);
        drawItemIconClipped(tree.iconFor(type), node.x() + 5, node.y() + 5, 16, bounds,
                state == AchievementTreeComponent.NodeState.LOCKED ? 0.45f : 1.0f, 1.0f);
        if (state == AchievementTreeComponent.NodeState.LOCKED) {
            drawClippedRect(bounds, node.x() + 4, node.y() + 4, node.width() - 8, node.height() - 8,
                    new float[] { 0.0f, 0.0f, 0.0f, 0.56f });
            drawClippedRect(bounds, node.x() + 5, node.y() + 12, node.width() - 10, 2,
                    new float[] { 0.08f, 0.08f, 0.08f, 0.74f });
        }
        if (selected || hovered) {
            drawNodeOutline(bounds, node.x() - 2, node.y() - 2, node.width() + 4, node.height() + 4,
                    hovered ? 1.0f : 0.75f,
                    hovered ? 1.0f : 0.75f,
                    hovered ? 1.0f : 0.75f,
                    1.0f);
        }
    }

    private void drawAchievementScrollbars(AchievementTreeComponent tree) {
        Rect bounds = achievementViewport(tree.bounds());
        int maxScrollY = Math.max(0, tree.contentHeight() - bounds.height());
        if (maxScrollY > 0) {
            int barHeight = Math.max(16, bounds.height() * bounds.height() / tree.contentHeight());
            int y = bounds.y() + Math.round((bounds.height() - barHeight) * (tree.scrollY() / (float) maxScrollY));
            drawRect(bounds.right() - 5, bounds.y(), 4, bounds.height(), 0.0f, 0.0f, 0.0f, 0.38f);
            drawRect(bounds.right() - 4, y, 3, barHeight, 0.82f, 0.82f, 0.82f, 0.72f);
        }
        int maxScrollX = Math.max(0, tree.contentWidth() - bounds.width());
        if (maxScrollX > 0) {
            int barWidth = Math.max(16, bounds.width() * bounds.width() / tree.contentWidth());
            int x = bounds.x() + Math.round((bounds.width() - barWidth) * (tree.scrollX() / (float) maxScrollX));
            drawRect(bounds.x(), bounds.bottom() - 5, bounds.width(), 4, 0.0f, 0.0f, 0.0f, 0.38f);
            drawRect(x, bounds.bottom() - 4, barWidth, 3, 0.82f, 0.82f, 0.82f, 0.72f);
        }
    }

    private void drawAchievementTooltip(AchievementTreeComponent tree) {
        AchievementType hovered = tree.hoveredAchievement();
        if (hovered == null || !tree.hasDetails(hovered) || textRenderer == null) {
            return;
        }
        String title = fitTooltipText(tree.titleLine(hovered), 246, 1.0f);
        String description = fitTooltipText(tree.detailDescription(hovered), 246, 0.85f);
        int titleWidth = textRenderer.getStringWidth(title, 1.0f);
        int descWidth = textRenderer.getStringWidth(description, 0.85f);
        int tooltipWidth = Math.max(120, Math.min(260, Math.max(titleWidth, descWidth) + 14));
        int tooltipHeight = description.isEmpty() ? 22 : 36;
        int x = Math.min(Math.max(tree.mouseX() + 12, tree.bounds().x() + 4),
                tree.bounds().right() - tooltipWidth - 4);
        int y = Math.min(Math.max(tree.mouseY() + 12, tree.bounds().y() + 4),
                tree.bounds().bottom() - tooltipHeight - 4);
        float[] color = tree.colorFor(hovered);
        drawAchievementTooltipFrame(x, y, tooltipWidth, tooltipHeight, color);
        drawText(title, x + 7, y + 6, 1.0f, color);
        if (!description.isEmpty()) {
            drawText(description, x + 7, y + 21, 0.85f, new float[] { 0.78f, 0.78f, 0.78f, 1.0f });
        }
    }

    private void drawAchievementTooltipFrame(int x, int y, int tooltipWidth, int tooltipHeight, float[] color) {
        if (achievementTexture != null) {
            drawTexturedQuad(achievementTexture, x, y, tooltipWidth, tooltipHeight,
                    96.0f / 256.0f, 202.0f / 256.0f, 1.0f, 232.0f / 256.0f,
                    0.88f, 0.88f, 0.88f, 0.98f);
        } else {
            drawRect(x, y, tooltipWidth, tooltipHeight, 0.03f, 0.03f, 0.03f, 0.92f);
        }
        drawOutline(x, y, tooltipWidth, tooltipHeight, 0.0f, 0.0f, 0.0f, 1.0f);
        drawOutline(x + 1, y + 1, tooltipWidth - 2, tooltipHeight - 2, color[0], color[1], color[2], 0.82f);
    }

    private String fitTooltipText(String text, int maxWidth, float scale) {
        if (text == null || textRenderer == null || textRenderer.getStringWidth(text, scale) <= maxWidth) {
            return text == null ? "" : text;
        }
        if (maxWidth <= 0) {
            return "";
        }
        String suffix = "...";
        int end = Math.max(0, text.length());
        while (end > 0) {
            String candidate = text.substring(0, end) + suffix;
            if (textRenderer.getStringWidth(candidate, scale) <= maxWidth) {
                return candidate;
            }
            end--;
        }
        return suffix;
    }

    private String fitListText(String text, int maxWidth, float scale) {
        if (text == null || textRenderer == null || textRenderer.getStringWidth(text, scale) <= maxWidth) {
            return text == null ? "" : text;
        }
        if (maxWidth <= 0) {
            return "";
        }
        String suffix = "...";
        int end = Math.max(0, text.length());
        while (end > 0) {
            String candidate = text.substring(0, end).stripTrailing() + suffix;
            if (textRenderer.getStringWidth(candidate, scale) <= maxWidth) {
                return candidate;
            }
            end--;
        }
        return suffix;
    }

    private void drawItemIconClipped(ItemType type, int x, int y, int size, Rect clip, float alpha, float shade) {
        if (x < clip.x() || y < clip.y() || x + size > clip.right() || y + size > clip.bottom()) {
            return;
        }
        if (shade >= 0.99f) {
            drawItemIcon(type, x, y, size, alpha);
            return;
        }
        boolean usesItemsAtlas = ItemTextureResolver.usesItemsAtlas(type);
        Texture texture = usesItemsAtlas ? itemsTexture : terrainTexture;
        float[] uv = insetIconUv(ItemTextureResolver.getUv(type));
        drawTexturedQuad(texture, x, y, size, size, uv[0], uv[1], uv[2], uv[3],
                shade, shade, shade, alpha);
    }

    private void drawNodeOutline(Rect clip, int x, int y, int width, int height,
            float r, float g, float b, float a) {
        float[] color = new float[] { r, g, b, a };
        drawClippedRect(clip, x, y, width, 1, color);
        drawClippedRect(clip, x, y + height - 1, width, 1, color);
        drawClippedRect(clip, x, y, 1, height, color);
        drawClippedRect(clip, x + width - 1, y, 1, height, color);
    }

    private void drawClippedRect(Rect clip, int x, int y, int rectWidth, int rectHeight, float[] color) {
        int x1 = Math.max(clip.x(), x);
        int y1 = Math.max(clip.y(), y);
        int x2 = Math.min(clip.right(), x + rectWidth);
        int y2 = Math.min(clip.bottom(), y + rectHeight);
        if (x2 <= x1 || y2 <= y1) {
            return;
        }
        drawRect(x1, y1, x2 - x1, y2 - y1, color[0], color[1], color[2], color[3]);
    }

    private static boolean intersects(Rect first, Rect second) {
        return first.x() < second.right()
                && first.right() > second.x()
                && first.y() < second.bottom()
                && first.bottom() > second.y();
    }

    public void drawComponent(MenuComponent component) {
        if (component == null || !component.isVisible()) {
            return;
        }
        if (component instanceof AchievementTreeComponent tree) {
            drawAchievementTree(tree);
        } else if (component instanceof MenuList<?> list) {
            drawList(list);
        } else if (component instanceof TextField field) {
            drawTextField(field);
        } else if (component instanceof MenuSlider slider) {
            drawSlider(slider);
        } else if (component instanceof MenuButton button) {
            drawButton(button);
        } else if (component instanceof MenuLabel label) {
            drawLabel(label);
        }
    }

    private void prepare2DState() {
        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    private void drawOutline(int x, int y, int outlineWidth, int outlineHeight, float r, float g, float b, float a) {
        drawRect(x, y, outlineWidth, 1, r, g, b, a);
        drawRect(x, y + outlineHeight - 1, outlineWidth, 1, r, g, b, a);
        drawRect(x, y, 1, outlineHeight, r, g, b, a);
        drawRect(x + outlineWidth - 1, y, 1, outlineHeight, r, g, b, a);
    }

    public void cleanup() {
        if (shader != null) {
            shader.cleanup();
        }
        if (texturedShader != null) {
            texturedShader.cleanup();
        }
        if (vbo != 0) {
            glDeleteBuffers(vbo);
        }
        if (vao != 0) {
            glDeleteVertexArrays(vao);
        }
        if (texturedVbo != 0) {
            glDeleteBuffers(texturedVbo);
        }
        if (texturedVao != 0) {
            glDeleteVertexArrays(texturedVao);
        }
        if (terrainTexture != null) {
            terrainTexture.cleanup();
        }
        if (guiTexture != null) {
            guiTexture.cleanup();
        }
        if (itemsTexture != null) {
            itemsTexture.cleanup();
        }
        if (cloudsTexture != null) {
            cloudsTexture.cleanup();
        }
        if (achievementTexture != null) {
            achievementTexture.cleanup();
        }
        if (titlePanoramaRenderer != null) {
            titlePanoramaRenderer.cleanup();
        }
    }
}

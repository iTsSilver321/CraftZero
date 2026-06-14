package com.craftzero.ui.menu;

import com.craftzero.graphics.ShaderProgram;
import com.craftzero.graphics.TextRenderer;
import com.craftzero.graphics.Texture;
import com.craftzero.graphics.TitlePanoramaRenderer;
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
    private Texture terrainTexture;
    private Texture cloudsTexture;
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

        terrainTexture = new Texture("/textures/terrain/Terrain.png");
        cloudsTexture = new Texture("/textures/environment/clouds.png");
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
        drawRect(0, 0, logicalWidth, logicalHeight, 0.19f, 0.14f, 0.10f, 1.0f);
        int tile = 32;
        for (int y = 0; y < logicalHeight; y += tile) {
            for (int x = 0; x < logicalWidth; x += tile) {
                boolean dark = ((x / tile) + (y / tile)) % 2 == 0;
                drawRect(x, y, tile, tile, dark ? 0.14f : 0.18f, dark ? 0.10f : 0.12f, dark ? 0.07f : 0.09f, 1.0f);
            }
        }
    }

    public void drawButton(MenuButton button) {
        if (!button.visible()) {
            return;
        }
        float shade = !button.enabled() ? 0.28f : button.hovered() ? 0.62f : 0.44f;
        drawRect(button.x(), button.y(), button.width(), button.height(), shade, shade, shade, 0.95f);
        drawRect(button.x(), button.y(), button.width(), 2, shade + 0.22f, shade + 0.22f, shade + 0.22f, 1.0f);
        drawRect(button.x(), button.y(), 2, button.height(), shade + 0.20f, shade + 0.20f, shade + 0.20f, 1.0f);
        drawRect(button.x(), button.y() + button.height() - 2, button.width(), 2, 0.08f, 0.08f, 0.08f, 1.0f);
        drawRect(button.x() + button.width() - 2, button.y(), 2, button.height(), 0.08f, 0.08f, 0.08f, 1.0f);
        drawCenteredText(button.label(), button.x() + button.width() / 2, button.y() + 6,
                1.0f, button.enabled() ? new float[] { 1, 1, 1, 1 } : new float[] { 0.6f, 0.6f, 0.6f, 1 });
    }

    public void drawSlider(MenuSlider slider) {
        if (!slider.visible()) {
            return;
        }
        float shade = slider.enabled() ? 0.32f : 0.18f;
        drawRect(slider.x(), slider.y(), slider.width(), slider.height(), shade, shade, shade, 0.95f);
        int knob = slider.x() + (int) Math.round(slider.normalizedValue() * (slider.width() - 8));
        float knobShade = slider.enabled() ? 0.8f : 0.45f;
        drawRect(knob, slider.y(), 8, slider.height(), knobShade, knobShade, knobShade, 1.0f);
        drawCenteredText(slider.displayText(), slider.x() + slider.width() / 2, slider.y() + 6, 1.0f,
                slider.enabled() ? new float[] { 1, 1, 1, 1 } : new float[] { 0.6f, 0.6f, 0.6f, 1 });
    }

    public void drawTextField(TextField field) {
        if (!field.visible()) {
            return;
        }
        drawRect(field.x(), field.y(), field.width(), field.height(), 0.02f, 0.02f, 0.02f, 0.85f);
        drawOutline(field.x(), field.y(), field.width(), field.height(),
                field.focused() ? 1.0f : 0.55f, field.focused() ? 1.0f : 0.55f, field.focused() ? 1.0f : 0.55f, 1.0f);
        drawText(field.value() + (field.focused() ? "_" : ""), field.x() + 5, field.y() + 6, 1.0f,
                new float[] { 1, 1, 1, 1 });
    }

    public <T> void drawList(MenuList<T> list) {
        if (!list.visible()) {
            return;
        }
        drawRect(list.x(), list.y(), list.width(), list.rowHeight() * list.visibleRows(),
                0.02f, 0.02f, 0.02f, 0.55f);
        for (int i = 0; i < list.visibleItems().size(); i++) {
            T item = list.visibleItems().get(i);
            int absolute = list.scrollOffset() + i;
            int y = list.y() + i * list.rowHeight();
            boolean selected = absolute == list.selectedIndex();
            if (selected) {
                drawRect(list.x() + 2, y + 2, list.width() - 4, list.rowHeight() - 4, 0.42f, 0.42f, 0.42f, 0.9f);
            }
            drawText(list.labelFor(item), list.x() + 8, y + 8, 1.0f, new float[] { 1, 1, 1, 1 });
        }
        drawOutline(list.x(), list.y(), list.width(), list.rowHeight() * list.visibleRows(), 0, 0, 0, 1);
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
        if (texture == null || texturedShader == null || texturedVao == 0) {
            return;
        }
        prepare2DState();
        texture.bind(0);
        texturedShader.bind();
        texturedShader.setUniform("projection", projection);
        texturedShader.setUniform("textureSampler", 0);
        texturedShader.setUniform("tint", new Vector4f(r, g, b, a));

        float px = x * guiScale;
        float py = y * guiScale;
        float pw = rectWidth * guiScale;
        float ph = rectHeight * guiScale;
        float[] vertices = {
                px, py, u1, v1,
                px + pw, py, u2, v1,
                px + pw, py + ph, u2, v2,
                px, py + ph, u1, v2
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

    public void drawComponent(MenuComponent component) {
        if (component == null || !component.isVisible()) {
            return;
        }
        if (component instanceof MenuList<?> list) {
            drawList(list);
        } else if (component instanceof TextField field) {
            drawTextField(field);
        } else if (component instanceof MenuSlider slider) {
            drawSlider(slider);
        } else if (component instanceof MenuButton button) {
            drawButton(button);
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
        if (cloudsTexture != null) {
            cloudsTexture.cleanup();
        }
        if (titlePanoramaRenderer != null) {
            titlePanoramaRenderer.cleanup();
        }
    }
}

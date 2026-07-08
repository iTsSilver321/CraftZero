package com.craftzero.graphics;

import com.craftzero.engine.Window;
import com.craftzero.ui.menu.ClassicGuiTexture;
import com.craftzero.ui.menu.UvRegion;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Renders the Minecraft-style death screen.
 * Matches Minecraft's exact styling: dark red background, white title, and
 * gui.png button strips.
 */
public class DeathScreen {

    private ShaderProgram shader;
    private ShaderProgram texturedShader;
    private int vao, vbo;
    private int texturedVao, texturedVbo;
    private Matrix4f projection;
    private int windowWidth, windowHeight;
    private TextRenderer textRenderer;

    // Button state
    private boolean respawnHovered = false;
    private boolean titleMenuHovered = false;

    // Minecraft-style button dimensions (wide buttons)
    private static final int BUTTON_WIDTH = 400;
    private static final int BUTTON_HEIGHT = 40;
    private static final int BUTTON_GAP = 8;
    private static final String RESPAWN_LABEL = "Respawn";
    private static final String TITLE_MENU_LABEL = "Title Menu";
    private static final String DELETE_WORLD_LABEL = "Delete World";
    private static final String HARDCORE_MESSAGE = "You cannot respawn in hardcore mode!";

    public void init(Window window) throws Exception {
        this.windowWidth = window.getWidth();
        this.windowHeight = window.getHeight();

        // Create simple shader for colored quads
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
                        "out vec4 FragColor;\n" +
                        "uniform vec4 color;\n" +
                        "void main() {\n" +
                        "    FragColor = color;\n" +
                        "}");
        shader.link();
        shader.createUniform("projection");
        shader.createUniform("color");

        texturedShader = new ShaderProgram();
        texturedShader.createVertexShader(
                "#version 330 core\n" +
                        "layout (location = 0) in vec2 aPos;\n" +
                        "layout (location = 1) in vec2 aUv;\n" +
                        "out vec2 texCoord;\n" +
                        "uniform mat4 projection;\n" +
                        "void main() {\n" +
                        "    texCoord = aUv;\n" +
                        "    gl_Position = projection * vec4(aPos, 0.0, 1.0);\n" +
                        "}");
        texturedShader.createFragmentShader(
                "#version 330 core\n" +
                        "in vec2 texCoord;\n" +
                        "out vec4 FragColor;\n" +
                        "uniform sampler2D textureSampler;\n" +
                        "uniform vec4 tint;\n" +
                        "void main() {\n" +
                        "    FragColor = texture(textureSampler, texCoord) * tint;\n" +
                        "    if (FragColor.a < 0.01) discard;\n" +
                        "}");
        texturedShader.link();
        texturedShader.createUniform("projection");
        texturedShader.createUniform("textureSampler");
        texturedShader.createUniform("tint");

        // Create VAO/VBO for quads
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

        projection = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
    }

    public void setTextRenderer(TextRenderer textRenderer) {
        this.textRenderer = textRenderer;
    }

    public void updateOrtho(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        projection = new Matrix4f().ortho(0, width, height, 0, -1, 1);
    }

    /**
     * Render the death screen overlay - Minecraft style.
     * 
     * @param deathTime Ticks since death for fade-in animation
     * @param mouseX    Current mouse X position
     * @param mouseY    Current mouse Y position
     */
    public void render(int deathTime, float mouseX, float mouseY) {
        render(deathTime, mouseX, mouseY, 0);
    }

    public void render(int deathTime, float mouseX, float mouseY, int score) {
        render(deathTime, mouseX, mouseY, score, false);
    }

    public void render(int deathTime, float mouseX, float mouseY, int score, boolean hardcore) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_DEPTH_TEST);

        // Calculate fade-in alpha (0 to max over ~1 second / 20 ticks)
        float fadeProgress = Math.min(deathTime / 20.0f, 1.0f);
        // Minecraft uses a very dark, almost opaque red overlay
        float alpha = fadeProgress * 0.85f;

        // Draw dark red/maroon background overlay (Minecraft uses very dark red)
        drawQuad(0, 0, windowWidth, windowHeight, 0.15f, 0.0f, 0.0f, alpha);

        // Only show text and buttons after fade-in starts
        if (deathTime > 5 && textRenderer != null) {
            // Calculate text fade
            float textAlpha = Math.min((deathTime - 5) / 15.0f, 1.0f);

            String title = hardcore ? "Game over!" : "You Died!";
            float titleScale = 3.0f; // Larger for visibility
            int titleWidth = textRenderer.getStringWidth(title, titleScale);
            int titleX = (windowWidth - titleWidth) / 2;
            // Position title in the upper third of the screen
            int titleY = windowHeight / 4;

            // Draw black shadow (offset by 2 pixels)
            textRenderer.drawText(title, titleX + 2, titleY + 2, titleScale,
                    new float[] { 0.0f, 0.0f, 0.0f, textAlpha });
            // Draw white text
            textRenderer.drawText(title, titleX, titleY, titleScale,
                    new float[] { 1.0f, 1.0f, 1.0f, textAlpha });

            String scoreText = scoreText(score);
            float scoreScale = 2.0f;
            int scoreWidth = textRenderer.getStringWidth(scoreText, scoreScale);
            int scoreX = (windowWidth - scoreWidth) / 2;
            int scoreY = titleY + 58;
            textRenderer.drawText(scoreText, scoreX + 1, scoreY + 1, scoreScale,
                    new float[] { 0.0f, 0.0f, 0.0f, textAlpha });
            textRenderer.drawText(scoreText, scoreX, scoreY, scoreScale,
                    new float[] { 1.0f, 1.0f, 1.0f, textAlpha });

            if (hardcore) {
                int messageWidth = textRenderer.getStringWidth(HARDCORE_MESSAGE, 1.5f);
                int messageX = (windowWidth - messageWidth) / 2;
                int messageY = scoreY + 42;
                textRenderer.drawText(HARDCORE_MESSAGE, messageX + 1, messageY + 1, 1.5f,
                        new float[] { 0.0f, 0.0f, 0.0f, textAlpha });
                textRenderer.drawText(HARDCORE_MESSAGE, messageX, messageY, 1.5f,
                        new float[] { 1.0f, 1.0f, 1.0f, textAlpha });
                clearButtonHover();
                ButtonBounds deleteBounds = deleteWorldButtonBounds(windowWidth, windowHeight);
                boolean deleteHovered = deleteBounds.contains(mouseX, mouseY);
                drawMinecraftButton(deleteBounds.x(), deleteBounds.y(), deleteBounds.width(), deleteBounds.height(),
                        DELETE_WORLD_LABEL, deleteHovered, textAlpha);
            } else {
                ButtonBounds respawnBounds = respawnButtonBounds(windowWidth, windowHeight);
                ButtonBounds titleMenuBounds = titleMenuButtonBounds(windowWidth, windowHeight);
                updateButtonHover(mouseX, mouseY);

                drawMinecraftButton(respawnBounds.x(), respawnBounds.y(), respawnBounds.width(), respawnBounds.height(),
                        RESPAWN_LABEL, respawnHovered, textAlpha);
                drawMinecraftButton(titleMenuBounds.x(), titleMenuBounds.y(), titleMenuBounds.width(),
                        titleMenuBounds.height(), TITLE_MENU_LABEL, titleMenuHovered, textAlpha);
            }
        } else {
            clearButtonHover();
        }

        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }

    public static String scoreText(int score) {
        return "Score: " + Math.max(0, score);
    }

    public static ButtonBounds respawnButtonBounds(int windowWidth, int windowHeight) {
        return buttonBounds(windowWidth, windowHeight, 0);
    }

    public static ButtonBounds titleMenuButtonBounds(int windowWidth, int windowHeight) {
        return buttonBounds(windowWidth, windowHeight, 1);
    }

    public static ButtonBounds deleteWorldButtonBounds(int windowWidth, int windowHeight) {
        return buttonBounds(windowWidth, windowHeight, 1);
    }

    public static String hardcoreMessageText() {
        return HARDCORE_MESSAGE;
    }

    private static ButtonBounds buttonBounds(int windowWidth, int windowHeight, int row) {
        int buttonX = (windowWidth - BUTTON_WIDTH) / 2;
        int buttonY = windowHeight / 2 + 20 + row * (BUTTON_HEIGHT + BUTTON_GAP);
        return new ButtonBounds(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    public void updateButtonHover(float mouseX, float mouseY) {
        respawnHovered = respawnButtonBounds(windowWidth, windowHeight).contains(mouseX, mouseY);
        titleMenuHovered = titleMenuButtonBounds(windowWidth, windowHeight).contains(mouseX, mouseY);
    }

    private void clearButtonHover() {
        respawnHovered = false;
        titleMenuHovered = false;
    }

    private void drawMinecraftButton(int x, int y, int width, int height,
            String text, boolean hovered, float alpha) {
        if (!drawClassicButtonTexture(x, y, width, height, hovered, alpha)) {
            drawFallbackButton(x, y, width, height, hovered, alpha);
        }
        if (textRenderer != null) {
            float buttonScale = 2.0f;
            int textWidth = textRenderer.getStringWidth(text, buttonScale);
            int textX = x + (width - textWidth) / 2;
            int textY = y + (height - Math.round(8 * buttonScale)) / 2;
            float[] textColor = hovered
                    ? new float[] { 1.0f, 1.0f, 160.0f / 255.0f, alpha }
                    : new float[] { 224.0f / 255.0f, 224.0f / 255.0f, 224.0f / 255.0f, alpha };
            textRenderer.drawText(text, textX + 1, textY + 1, buttonScale,
                    new float[] { 0.15f, 0.15f, 0.15f, alpha });
            textRenderer.drawText(text, textX, textY, buttonScale, textColor);
        }
    }

    private boolean drawClassicButtonTexture(int x, int y, int width, int height, boolean hovered, float alpha) {
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

        gui.bind(0);
        texturedShader.bind();
        texturedShader.setUniform("projection", projection);
        texturedShader.setUniform("textureSampler", 0);
        texturedShader.setUniform("tint", new Vector4f(1.0f, 1.0f, 1.0f, alpha));
        drawTexturedQuad(x, y, leftWidth, height, left);
        drawTexturedQuad(x + leftWidth, y, rightWidth, height, right);
        texturedShader.unbind();
        gui.unbind();
        return true;
    }

    private void drawFallbackButton(int x, int y, int width, int height, boolean hovered, float alpha) {
        float shade = hovered ? 0.62f : 0.44f;
        drawQuad(x, y, width, height, shade, shade, hovered ? 0.72f : shade, alpha);
        drawQuad(x, y, width, 2, 0.7f, 0.7f, 0.7f, alpha);
        drawQuad(x, y, 2, height, 0.7f, 0.7f, 0.7f, alpha);
        drawQuad(x, y + height - 2, width, 2, 0.2f, 0.2f, 0.2f, alpha);
        drawQuad(x + width - 2, y, 2, height, 0.2f, 0.2f, 0.2f, alpha);
        drawQuad(x - 1, y - 1, width + 2, 1, 0.0f, 0.0f, 0.0f, alpha);
        drawQuad(x - 1, y + height, width + 2, 1, 0.0f, 0.0f, 0.0f, alpha);
        drawQuad(x - 1, y, 1, height, 0.0f, 0.0f, 0.0f, alpha);
        drawQuad(x + width, y, 1, height, 0.0f, 0.0f, 0.0f, alpha);
    }

    /**
     * Check if respawn button is hovered and clicked.
     */
    public boolean isRespawnClicked(boolean mousePressed) {
        return respawnHovered && mousePressed;
    }

    /**
     * Check if the Release-era title menu button is hovered and clicked.
     */
    public boolean isTitleMenuClicked(boolean mousePressed) {
        return titleMenuHovered && mousePressed;
    }

    public record ButtonBounds(int x, int y, int width, int height) {
        public boolean contains(float mouseX, float mouseY) {
            return mouseX >= x && mouseX <= x + width
                    && mouseY >= y && mouseY <= y + height;
        }
    }

    private void drawQuad(float x, float y, float width, float height,
            float r, float g, float b, float a) {
        if (width <= 0 || height <= 0) {
            return;
        }
        shader.bind();
        shader.setUniform("projection", projection);
        shader.setUniform("color", new Vector4f(r, g, b, a));

        float[] vertices = {
                x, y,
                x + width, y,
                x + width, y + height,
                x, y,
                x + width, y + height,
                x, y + height
        };

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
        shader.unbind();
    }

    private void drawTexturedQuad(float x, float y, float width, float height, UvRegion uv) {
        if (width <= 0 || height <= 0) {
            return;
        }
        float[] vertices = {
                x, y, uv.u1(), uv.v1(),
                x + width, y, uv.u2(), uv.v1(),
                x + width, y + height, uv.u2(), uv.v2(),
                x, y + height, uv.u1(), uv.v2()
        };
        glBindVertexArray(texturedVao);
        glBindBuffer(GL_ARRAY_BUFFER, texturedVbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void cleanup() {
        if (shader != null) {
            shader.cleanup();
        }
        if (texturedShader != null) {
            texturedShader.cleanup();
        }
        if (vao != 0) {
            glDeleteVertexArrays(vao);
        }
        if (vbo != 0) {
            glDeleteBuffers(vbo);
        }
        if (texturedVao != 0) {
            glDeleteVertexArrays(texturedVao);
        }
        if (texturedVbo != 0) {
            glDeleteBuffers(texturedVbo);
        }
    }
}

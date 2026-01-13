package com.craftzero.graphics;

import com.craftzero.engine.Window;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Renders the Minecraft-style death screen.
 * Matches Minecraft's exact styling: dark red background, white title, gray
 * buttons.
 */
public class DeathScreen {

    private ShaderProgram shader;
    private int vao, vbo;
    private Matrix4f projection;
    private int windowWidth, windowHeight;
    private TextRenderer textRenderer;

    // Button state
    private boolean respawnHovered = false;

    // Minecraft-style button dimensions (wide buttons)
    private static final int BUTTON_WIDTH = 400;
    private static final int BUTTON_HEIGHT = 40;

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

            // "You Died!" title - WHITE text with BLACK shadow (Minecraft style)
            String title = "You Died!";
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

            // Respawn button - positioned in center of screen
            int buttonX = (windowWidth - BUTTON_WIDTH) / 2;
            int buttonY = windowHeight / 2 + 20;

            // Check if mouse is hovering over button
            respawnHovered = mouseX >= buttonX && mouseX <= buttonX + BUTTON_WIDTH
                    && mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT;

            // Draw Minecraft-style button
            drawMinecraftButton(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT,
                    "Respawn", respawnHovered, textAlpha);
        }

        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }

    /**
     * Draw a Minecraft-style button with 3D beveled look.
     */
    private void drawMinecraftButton(int x, int y, int width, int height,
            String text, boolean hovered, float alpha) {

        // Button colors (Minecraft style)
        float bgR, bgG, bgB;
        if (hovered) {
            // Lighter gray when hovered
            bgR = 0.55f;
            bgG = 0.55f;
            bgB = 0.65f;
        } else {
            // Normal gray
            bgR = 0.45f;
            bgG = 0.45f;
            bgB = 0.45f;
        }

        // Main button background
        drawQuad(x, y, width, height, bgR, bgG, bgB, alpha);

        // Top border (light - gives 3D raised effect)
        drawQuad(x, y, width, 2, 0.7f, 0.7f, 0.7f, alpha);
        // Left border (light)
        drawQuad(x, y, 2, height, 0.7f, 0.7f, 0.7f, alpha);

        // Bottom border (dark - shadow effect)
        drawQuad(x, y + height - 2, width, 2, 0.2f, 0.2f, 0.2f, alpha);
        // Right border (dark)
        drawQuad(x + width - 2, y, 2, height, 0.2f, 0.2f, 0.2f, alpha);

        // Outer dark border
        drawQuad(x - 1, y - 1, width + 2, 1, 0.0f, 0.0f, 0.0f, alpha); // Top
        drawQuad(x - 1, y + height, width + 2, 1, 0.0f, 0.0f, 0.0f, alpha); // Bottom
        drawQuad(x - 1, y, 1, height, 0.0f, 0.0f, 0.0f, alpha); // Left
        drawQuad(x + width, y, 1, height, 0.0f, 0.0f, 0.0f, alpha); // Right

        // Button text - WHITE with black shadow
        if (textRenderer != null) {
            float buttonScale = 2.0f; // Larger button text
            int textWidth = textRenderer.getStringWidth(text, buttonScale);
            int textX = x + (width - textWidth) / 2;
            // Center text vertically - adjusted for Minecraft bitmap font
            int textY = y + (height / 2) - 6;

            // Draw shadow
            textRenderer.drawText(text, textX + 1, textY + 1, buttonScale,
                    new float[] { 0.15f, 0.15f, 0.15f, alpha });
            // Draw text
            textRenderer.drawText(text, textX, textY, buttonScale,
                    new float[] { 1.0f, 1.0f, 1.0f, alpha });
        }
    }

    /**
     * Check if respawn button is hovered and clicked.
     */
    public boolean isRespawnClicked(boolean mousePressed) {
        return respawnHovered && mousePressed;
    }

    private void drawQuad(float x, float y, float width, float height,
            float r, float g, float b, float a) {
        shader.bind();
        shader.setUniform("projection", projection);
        shader.setUniform("color", new org.joml.Vector4f(r, g, b, a));

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

    public void cleanup() {
        if (shader != null) {
            shader.cleanup();
        }
        if (vao != 0) {
            glDeleteVertexArrays(vao);
        }
        if (vbo != 0) {
            glDeleteBuffers(vbo);
        }
    }
}

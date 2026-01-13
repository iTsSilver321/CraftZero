package com.craftzero.graphics;

import org.lwjgl.BufferUtils;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Minecraft bitmap font renderer using default.png texture.
 * The font texture is a 16x16 grid of 8x8 pixel characters (128x128 or
 * 256x128).
 */
public class TextRenderer {

    private int vao, vbo;
    private ShaderProgram shader;
    private int fontTextureId;
    private int windowWidth, windowHeight;

    // Minecraft font uses 8x8 pixel characters in a 16x16 grid
    private static final int CHAR_WIDTH = 8;
    private static final int CHAR_HEIGHT = 8;
    private static final int GRID_COLS = 16;
    private static final int GRID_ROWS = 16;

    // Character widths for proportional spacing (Minecraft uses variable width)
    private int[] charWidths = new int[256];

    private int textureWidth = 128;
    private int textureHeight = 128;

    public void init(int windowWidth, int windowHeight) throws Exception {
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;

        // Load Minecraft font texture
        loadFontTexture();

        // Setup Shader
        shader = new ShaderProgram();
        shader.createVertexShader(
                "#version 330 core\n" +
                        "layout (location = 0) in vec4 vertex; // <vec2 pos, vec2 tex>\n" +
                        "out vec2 TexCoords;\n" +
                        "uniform mat4 projection;\n" +
                        "void main() {\n" +
                        "    gl_Position = projection * vec4(vertex.xy, 0.0, 1.0);\n" +
                        "    TexCoords = vertex.zw;\n" +
                        "}");
        shader.createFragmentShader(
                "#version 330 core\n" +
                        "in vec2 TexCoords;\n" +
                        "out vec4 color;\n" +
                        "uniform sampler2D text;\n" +
                        "uniform vec4 textColor;\n" +
                        "void main() {\n" +
                        "    vec4 sampled = texture(text, TexCoords);\n" +
                        "    if (sampled.a < 0.1) discard;\n" +
                        "    color = textColor * sampled;\n" +
                        "}");
        shader.link();
        shader.createUniform("projection");
        shader.createUniform("text");
        shader.createUniform("textColor");

        // Setup VAO/VBO
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, Float.BYTES * 6 * 4, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 4, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    private void loadFontTexture() throws Exception {
        // Load the Minecraft font from resources
        InputStream is = getClass().getResourceAsStream("/textures/font/default.png");
        if (is == null) {
            throw new Exception("Could not find font texture: /textures/font/default.png");
        }

        BufferedImage image = ImageIO.read(is);
        is.close();

        textureWidth = image.getWidth();
        textureHeight = image.getHeight();

        // Calculate character widths by scanning each character cell
        calculateCharWidths(image);

        // Upload texture
        fontTextureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, fontTextureId);

        int[] pixels = new int[textureWidth * textureHeight];
        image.getRGB(0, 0, textureWidth, textureHeight, pixels, 0, textureWidth);

        ByteBuffer buffer = BufferUtils.createByteBuffer(textureWidth * textureHeight * 4);
        for (int pixel : pixels) {
            buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
            buffer.put((byte) ((pixel >> 8) & 0xFF)); // G
            buffer.put((byte) (pixel & 0xFF)); // B
            buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
        }
        buffer.flip();

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, textureWidth, textureHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        // Use NEAREST filtering for pixel-perfect Minecraft look
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    }

    /**
     * Calculate the width of each character by scanning the font texture.
     * Minecraft characters have variable widths.
     */
    private void calculateCharWidths(BufferedImage image) {
        int cellWidth = textureWidth / GRID_COLS;
        int cellHeight = textureHeight / GRID_ROWS;

        for (int charIndex = 0; charIndex < 256; charIndex++) {
            int col = charIndex % GRID_COLS;
            int row = charIndex / GRID_COLS;

            int cellX = col * cellWidth;
            int cellY = row * cellHeight;

            // Find the rightmost non-transparent pixel
            int width = 0;
            for (int x = cellWidth - 1; x >= 0; x--) {
                boolean found = false;
                for (int y = 0; y < cellHeight; y++) {
                    int px = cellX + x;
                    int py = cellY + y;
                    if (px < textureWidth && py < textureHeight) {
                        int pixel = image.getRGB(px, py);
                        int alpha = (pixel >> 24) & 0xFF;
                        if (alpha > 0) {
                            width = x + 2; // +1 for the pixel, +1 for spacing
                            found = true;
                            break;
                        }
                    }
                }
                if (found)
                    break;
            }

            // Minimum width for space character
            if (width == 0) {
                width = 4;
            }

            charWidths[charIndex] = width;
        }

        // Override space width
        charWidths[32] = 4;
    }

    public void drawText(String text, float x, float y, float scale, float[] color) {
        if (text == null || text.isEmpty())
            return;

        shader.bind();
        shader.setUniform("textColor", new Vector4f(color[0], color[1], color[2], color[3]));
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, fontTextureId);
        glBindVertexArray(vao);

        // Ortho projection
        Matrix4f projection = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1);
        shader.setUniform("projection", projection);

        int cellWidth = textureWidth / GRID_COLS;
        int cellHeight = textureHeight / GRID_ROWS;

        float charW = cellWidth * scale;
        float charH = cellHeight * scale;

        for (char c : text.toCharArray()) {
            int charIndex = (int) c;
            if (charIndex < 0 || charIndex >= 256)
                continue;

            int col = charIndex % GRID_COLS;
            int row = charIndex / GRID_COLS;

            // UV coordinates for this character
            float u1 = (float) col / GRID_COLS;
            float v1 = (float) row / GRID_ROWS;
            float u2 = (float) (col + 1) / GRID_COLS;
            float v2 = (float) (row + 1) / GRID_ROWS;

            // Vertices: Pos(x,y) Tex(u,v)
            float[] vertices = {
                    x, y + charH, u1, v2,
                    x, y, u1, v1,
                    x + charW, y, u2, v1,

                    x, y + charH, u1, v2,
                    x + charW, y, u2, v1,
                    x + charW, y + charH, u2, v2
            };

            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
            glBindBuffer(GL_ARRAY_BUFFER, 0);

            glDrawArrays(GL_TRIANGLES, 0, 6);

            // Advance by character width
            x += charWidths[charIndex] * scale;
        }

        glBindVertexArray(0);
        glBindTexture(GL_TEXTURE_2D, 0);
        shader.unbind();
    }

    public int getStringWidth(String text, float scale) {
        int width = 0;
        for (char c : text.toCharArray()) {
            int charIndex = (int) c;
            if (charIndex >= 0 && charIndex < 256) {
                width += charWidths[charIndex];
            }
        }
        return (int) (width * scale);
    }

    /**
     * Update the orthographic projection for window resize/fullscreen.
     */
    public void updateOrtho(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
    }

    /**
     * Cleanup resources.
     */
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
        if (fontTextureId != 0) {
            glDeleteTextures(fontTextureId);
        }
    }
}

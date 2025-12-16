package com.craftzero.graphics;

import org.lwjgl.BufferUtils;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class TextRenderer {

    private int vao, vbo;
    private ShaderProgram shader;
    private int fontTextureId;
    private Map<Character, Glyph> glyphs = new HashMap<>();
    private int windowWidth, windowHeight;

    private static final int FONT_SIZE = 32; // Bitmap resolution
    private static final int BITMAP_WIDTH = 512;
    private static final int BITMAP_HEIGHT = 512;

    private static class Glyph {
        float x, y, width, height;
        float xOffset, yOffset, xAdvance;
    }

    public void init(int windowWidth, int windowHeight) throws Exception {
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;

        // Generate Font Texture
        generateFontTexture();

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
                        "    vec4 sampled = vec4(1.0, 1.0, 1.0, texture(text, TexCoords).r);\n" +
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

    private void generateFontTexture() {
        BufferedImage image = new BufferedImage(BITMAP_WIDTH, BITMAP_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font("Segoe UI", Font.PLAIN, FONT_SIZE));
        g.setColor(Color.WHITE);

        FontMetrics fm = g.getFontMetrics();
        int x = 0;
        int y = 0;
        int intHeight = fm.getHeight();

        int padding = 2; // Padding to prevent texture bleeding

        for (int i = 32; i < 127; i++) {
            char c = (char) i;
            int charWidth = fm.charWidth(c);
            int charHeight = fm.getHeight();

            if (x + charWidth + padding >= BITMAP_WIDTH) {
                x = 0;
                y += intHeight + padding; // Add vertical padding
            }

            g.drawString(String.valueOf(c), x, y + fm.getAscent());

            Glyph glyph = new Glyph();
            glyph.x = x / (float) BITMAP_WIDTH;
            glyph.y = y / (float) BITMAP_HEIGHT;
            glyph.width = charWidth / (float) BITMAP_WIDTH;
            glyph.height = charHeight / (float) BITMAP_HEIGHT;
            glyph.xOffset = 0;
            glyph.yOffset = 0;
            glyph.xAdvance = charWidth;

            glyphs.put(c, glyph);
            x += charWidth + padding;
        }
        g.dispose();

        // Upload texture
        fontTextureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, fontTextureId);

        int[] pixels = new int[BITMAP_WIDTH * BITMAP_HEIGHT];
        image.getRGB(0, 0, BITMAP_WIDTH, BITMAP_HEIGHT, pixels, 0, BITMAP_WIDTH);

        ByteBuffer buffer = BufferUtils.createByteBuffer(BITMAP_WIDTH * BITMAP_HEIGHT * 4);
        for (int pixel : pixels) {
            // Extract alpha from red channel since we drew white text
            int alpha = (pixel >> 24) & 0xFF;
            // Or just use the alpha itself? BufferedImage is TYPE_INT_ARGB
            // If we drew white (255,255,255) with alpha, the RGB are 255.
            // Let's use Red channel as 'intensity' for single channel texture or keep
            // generic.

            // Standard approach: use trace.
            // Let's just create RGBA texture to be safe
            buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
            buffer.put((byte) ((pixel >> 8) & 0xFF)); // G
            buffer.put((byte) (pixel & 0xFF)); // B
            buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
        }
        buffer.flip();

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, BITMAP_WIDTH, BITMAP_HEIGHT, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
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

        for (char c : text.toCharArray()) {
            Glyph glyph = glyphs.get(c);
            if (glyph == null)
                continue;

            float xPos = x + glyph.xOffset * scale;
            float yPos = y + glyph.yOffset * scale; // Assuming top-left origin?
            // Font rendering usually baseline. My generator used top-left.
            // Let's assume y is top of text.

            float w = glyph.width * BITMAP_WIDTH * scale;
            float h = glyph.height * BITMAP_HEIGHT * scale;

            float u = glyph.x;
            float v = glyph.y; // Inverted? AWT is top-left 0,0. OpenGL 0,0 is bottom-left usually depending on
                               // ortho.
            // My ortho is 0,0 top-left (ortho(0, w, h, 0)).
            // So texture coords: 0,0 is top-left of image? No, OpenGL texture orgin is
            // bottom-left.
            // AWT image (0,0) is top-left.
            // buffer.put stores rows top-to-bottom.
            // So if I upload straight, 0,0 in UV is bottom-left of image. The image is
            // flipped.
            // Standard fix: flip Y in shader or CPU.
            // Or simply: AWT Y=0 is the first byte. OpenGL Y=0 is the first byte?
            // OpenGL default: 0,0 is bottom-left.
            // Actually, let's just use standard UV:
            // Top-Left of quad: U, V

            // Just try standard and flip if upside down.

            float u2 = u + glyph.width;
            float v2 = v + glyph.height;

            // Vertices: Pos(x,y) Tex(u,v)
            float[] vertices = {
                    xPos, yPos + h, u, v2,
                    xPos, yPos, u, v,
                    xPos + w, yPos, u2, v,

                    xPos, yPos + h, u, v2,
                    xPos + w, yPos, u2, v,
                    xPos + w, yPos + h, u2, v2
            };

            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
            glBindBuffer(GL_ARRAY_BUFFER, 0);

            glDrawArrays(GL_TRIANGLES, 0, 6);

            x += glyph.xAdvance * scale;
        }

        glBindVertexArray(0);
        glBindTexture(GL_TEXTURE_2D, 0);
        shader.unbind();
    }

    public int getStringWidth(String text, float scale) {
        int width = 0;
        for (char c : text.toCharArray()) {
            Glyph glyph = glyphs.get(c);
            if (glyph != null) {
                width += glyph.xAdvance * scale;
            }
        }
        return width;
    }
}

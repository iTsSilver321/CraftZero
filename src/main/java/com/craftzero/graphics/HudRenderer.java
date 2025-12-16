package com.craftzero.graphics;

import com.craftzero.engine.Window;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * HUD renderer for crosshair and UI elements.
 * Uses orthographic projection for 2D rendering.
 */
public class HudRenderer {

    private ShaderProgram hudShader;
    private int crosshairVao;
    private int crosshairVbo;

    private Matrix4f orthoMatrix;

    public void init(Window window) throws Exception {
        // Create HUD shader
        hudShader = new ShaderProgram();
        hudShader.createVertexShader(
                "#version 330 core\n" +
                        "layout (location = 0) in vec2 aPos;\n" +
                        "uniform mat4 projection;\n" +
                        "void main() {\n" +
                        "    gl_Position = projection * vec4(aPos, 0.0, 1.0);\n" +
                        "}");
        hudShader.createFragmentShader(
                "#version 330 core\n" +
                        "out vec4 fragColor;\n" +
                        "uniform vec4 color;\n" +
                        "void main() {\n" +
                        "    fragColor = color;\n" +
                        "}");
        hudShader.link();
        hudShader.createUniform("projection");
        hudShader.createUniform("color");

        // Create crosshair mesh
        createCrosshair();

        // Initial ortho matrix
        updateOrtho(window.getWidth(), window.getHeight());
    }

    private void createCrosshair() {
        float size = 10.0f;
        float thickness = 2.0f;

        // Crosshair vertices (two rectangles)
        float[] vertices = {
                // Horizontal line
                -size, -thickness / 2,
                size, -thickness / 2,
                size, thickness / 2,
                -size, thickness / 2,
                // Vertical line
                -thickness / 2, -size,
                thickness / 2, -size,
                thickness / 2, size,
                -thickness / 2, size
        };

        crosshairVao = glGenVertexArrays();
        crosshairVbo = glGenBuffers();

        glBindVertexArray(crosshairVao);

        FloatBuffer buffer = MemoryUtil.memAllocFloat(vertices.length);
        buffer.put(vertices).flip();

        glBindBuffer(GL_ARRAY_BUFFER, crosshairVbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        MemoryUtil.memFree(buffer);
    }

    public void updateOrtho(int width, int height) {
        orthoMatrix = new Matrix4f().ortho(
                -width / 2.0f, width / 2.0f,
                -height / 2.0f, height / 2.0f,
                -1.0f, 1.0f);
    }

    public void render(Window window) {
        if (window.isResized()) {
            updateOrtho(window.getWidth(), window.getHeight());
        }

        // Disable depth testing for HUD
        glDisable(GL_DEPTH_TEST);

        // Enable blending for transparency
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        hudShader.bind();
        hudShader.setUniform("projection", orthoMatrix);

        // Render crosshair (white with slight transparency)
        hudShader.setUniform("color", new org.joml.Vector4f(1.0f, 1.0f, 1.0f, 0.8f));

        glBindVertexArray(crosshairVao);
        glDrawArrays(GL_TRIANGLE_FAN, 0, 4); // Horizontal
        glDrawArrays(GL_TRIANGLE_FAN, 4, 4); // Vertical
        glBindVertexArray(0);

        hudShader.unbind();

        // Re-enable depth testing
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }

    public void cleanup() {
        if (hudShader != null) {
            hudShader.cleanup();
        }
        glDeleteBuffers(crosshairVbo);
        glDeleteVertexArrays(crosshairVao);
    }
}

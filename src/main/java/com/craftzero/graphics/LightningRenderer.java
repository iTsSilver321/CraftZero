package com.craftzero.graphics;

import com.craftzero.world.World;
import com.craftzero.world.WorldLightningBolt;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

/**
 * Renders transient lightning bolt geometry as bright jagged line segments.
 */
public class LightningRenderer {
    private static final float LIGHTNING_RENDER_DISTANCE = 256.0f;

    private final Renderer renderer;
    private ShaderProgram lineShader;
    private int vao;
    private int vbo;

    public LightningRenderer(Renderer renderer) {
        this.renderer = renderer;
    }

    public void init() throws Exception {
        lineShader = new ShaderProgram();
        lineShader.createVertexShader(ShaderProgram.loadResource("/shaders/line.vert"));
        lineShader.createFragmentShader(ShaderProgram.loadResource("/shaders/line.frag"));
        lineShader.link();
        lineShader.createUniform("projectionMatrix");
        lineShader.createUniform("viewMatrix");
        lineShader.createUniform("lineColor");

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void render(World world, Camera camera, float partialTick) {
        if (world == null || camera == null || lineShader == null || vao == 0 || vbo == 0) {
            return;
        }
        List<WorldLightningBolt> bolts = world.getLightningBolts();
        if (bolts.isEmpty()) {
            return;
        }

        lineShader.bind();
        lineShader.setUniform("projectionMatrix", camera.getProjectionMatrix());
        lineShader.setUniform("viewMatrix", camera.getViewMatrix());
        glDisable(GL_CULL_FACE);
        glLineWidth(2.0f);
        glBindVertexArray(vao);

        for (WorldLightningBolt bolt : bolts) {
            if (isTooFar(camera, bolt)) {
                continue;
            }
            float alpha = bolt.getAlpha(partialTick);
            if (alpha <= 0.0f) {
                continue;
            }
            lineShader.setUniform("lineColor", new Vector4f(0.72f, 0.82f, 1.0f, alpha));
            float[] vertices = verticesFor(bolt);
            FloatBuffer buffer = MemoryUtil.memAllocFloat(vertices.length);
            try {
                buffer.put(vertices).flip();
                glBindBuffer(GL_ARRAY_BUFFER, vbo);
                glBufferData(GL_ARRAY_BUFFER, buffer, GL_DYNAMIC_DRAW);
                glDrawArrays(GL_LINES, 0, vertices.length / 3);
            } finally {
                MemoryUtil.memFree(buffer);
            }
        }

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        glEnable(GL_CULL_FACE);
        lineShader.unbind();
        renderer.getShaderProgram().bind();
    }

    private static float[] verticesFor(WorldLightningBolt bolt) {
        List<WorldLightningBolt.Segment> segments = bolt.getSegments();
        float[] vertices = new float[segments.size() * 6];
        int i = 0;
        for (WorldLightningBolt.Segment segment : segments) {
            vertices[i++] = segment.x1();
            vertices[i++] = segment.y1();
            vertices[i++] = segment.z1();
            vertices[i++] = segment.x2();
            vertices[i++] = segment.y2();
            vertices[i++] = segment.z2();
        }
        return vertices;
    }

    private static boolean isTooFar(Camera camera, WorldLightningBolt bolt) {
        if (bolt == null) {
            return true;
        }
        return RenderDistanceCulling.isPointTooFar(camera, bolt.getX(), bolt.getY(), bolt.getZ(),
                LIGHTNING_RENDER_DISTANCE);
    }

    public void cleanup() {
        if (vao != 0) {
            glDisableVertexAttribArray(0);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glDeleteBuffers(vbo);
            glBindVertexArray(0);
            glDeleteVertexArrays(vao);
            vao = 0;
            vbo = 0;
        }
        if (lineShader != null) {
            lineShader.cleanup();
            lineShader = null;
        }
    }
}

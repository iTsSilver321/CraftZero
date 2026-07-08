package com.craftzero.graphics;

import com.craftzero.world.DayCycleManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Renders the classic Release-era cloud sheet as a wrapped, textured layer.
 */
public class CloudRenderer {

    static final String CLOUD_TEXTURE_RESOURCE = "/textures/environment/clouds.png";
    static final float RELEASE_CLOUD_HEIGHT = 108.0f;
    static final float CLOUD_TILE_SIZE = 256.0f;
    static final float CLOUD_SPEED = 0.6f;
    static final int CLOUD_TILE_RADIUS = 3;

    private ShaderProgram cloudShader;
    private Texture cloudTexture;
    private int vao, vbo, ebo;
    private int vertexCount;
    private float cloudOffsetX = 0.0f;

    public void init() throws Exception {
        cloudShader = new ShaderProgram();
        cloudShader.createVertexShader(
                "#version 330 core\n" +
                        "layout (location = 0) in vec3 aPos;\n" +
                        "layout (location = 1) in vec2 aTexCoord;\n" +
                        "out vec2 fragTexCoord;\n" +
                        "uniform mat4 projectionMatrix;\n" +
                        "uniform mat4 viewMatrix;\n" +
                        "uniform mat4 modelMatrix;\n" +
                        "void main() {\n" +
                        "    fragTexCoord = aTexCoord;\n" +
                        "    gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(aPos, 1.0);\n" +
                        "}");
        cloudShader.createFragmentShader(
                "#version 330 core\n" +
                        "in vec2 fragTexCoord;\n" +
                        "out vec4 fragColor;\n" +
                        "uniform sampler2D cloudTexture;\n" +
                        "uniform float cloudBrightness;\n" +
                        "void main() {\n" +
                        "    vec4 tex = texture(cloudTexture, fragTexCoord);\n" +
                        "    if (tex.a < 0.05) discard;\n" +
                        "    fragColor = vec4(tex.rgb * cloudBrightness, tex.a * 0.85);\n" +
                        "}");
        cloudShader.link();
        cloudShader.createUniform("projectionMatrix");
        cloudShader.createUniform("viewMatrix");
        cloudShader.createUniform("modelMatrix");
        cloudShader.createUniform("cloudTexture");
        cloudShader.createUniform("cloudBrightness");

        cloudTexture = new Texture(CLOUD_TEXTURE_RESOURCE);
        cloudTexture.bind(0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        cloudTexture.unbind();

        float[] vertices = {
                -0.5f, 0.0f, -0.5f, 0.0f, 1.0f,
                -0.5f, 0.0f, 0.5f, 0.0f, 0.0f,
                0.5f, 0.0f, 0.5f, 1.0f, 0.0f,
                0.5f, 0.0f, -0.5f, 1.0f, 1.0f
        };
        int[] indices = { 0, 1, 3, 3, 1, 2 };
        vertexCount = indices.length;

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        int stride = 5 * Float.BYTES;
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, (long) 3 * Float.BYTES);

        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        glBindVertexArray(0);
    }

    public void render(Renderer renderer, DayCycleManager dayCycle, Camera camera, float deltaTime,
            float brightnessMultiplier) {
        cloudOffsetX = normalizedScrollOffset(cloudOffsetX - CLOUD_SPEED * deltaTime);

        Vector3f camPos = camera.getPosition();

        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);

        cloudShader.bind();
        cloudShader.setUniform("projectionMatrix", camera.getProjectionMatrix());
        cloudShader.setUniform("viewMatrix", camera.getViewMatrix());
        cloudShader.setUniform("cloudBrightness", cloudBrightness(dayCycle.getSunBrightness(), brightnessMultiplier));
        cloudShader.setUniform("cloudTexture", 0);
        cloudTexture.bind(0);

        glBindVertexArray(vao);

        Matrix4f modelMatrix = new Matrix4f();
        for (int tileX = -CLOUD_TILE_RADIUS; tileX <= CLOUD_TILE_RADIUS; tileX++) {
            float renderX = cloudTileCenter(camPos.x, cloudOffsetX, tileX);
            for (int tileZ = -CLOUD_TILE_RADIUS; tileZ <= CLOUD_TILE_RADIUS; tileZ++) {
                float renderZ = cloudTileCenter(camPos.z, 0.0f, tileZ);

                modelMatrix.identity();
                modelMatrix.translate(renderX, RELEASE_CLOUD_HEIGHT, renderZ);
                modelMatrix.scale(CLOUD_TILE_SIZE, 1.0f, CLOUD_TILE_SIZE);

                cloudShader.setUniform("modelMatrix", modelMatrix);
                glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
            }
        }

        glBindVertexArray(0);
        cloudShader.unbind();

        glDepthMask(true);
        glEnable(GL_CULL_FACE);
    }

    public void render(Renderer renderer, DayCycleManager dayCycle, Camera camera, float deltaTime) {
        render(renderer, dayCycle, camera, deltaTime, 1.0f);
    }

    static float cloudBrightness(float sunBrightness, float brightnessMultiplier) {
        return 0.95f * clamp01(sunBrightness) * clamp01(brightnessMultiplier);
    }

    static float normalizedScrollOffset(float offset) {
        float wrapped = offset % CLOUD_TILE_SIZE;
        if (wrapped > 0.0f) {
            wrapped -= CLOUD_TILE_SIZE;
        }
        return wrapped;
    }

    static float cloudTileCenter(float cameraCoord, float scrollOffset, int tileOffset) {
        float baseCenter = (float) Math.floor((cameraCoord - scrollOffset) / CLOUD_TILE_SIZE) * CLOUD_TILE_SIZE
                + scrollOffset;
        return baseCenter + tileOffset * CLOUD_TILE_SIZE;
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    public void cleanup() {
        if (cloudShader != null) {
            cloudShader.cleanup();
        }
        if (cloudTexture != null) {
            cloudTexture.cleanup();
        }
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        glDeleteVertexArrays(vao);
    }
}

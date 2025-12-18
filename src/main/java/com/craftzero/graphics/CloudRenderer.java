package com.craftzero.graphics;

import com.craftzero.world.DayCycleManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Renders Minecraft-style clouds as solid white blocky shapes.
 * Uses its own simple shader that just outputs white - no texture sampling.
 */
public class CloudRenderer {

        private ShaderProgram cloudShader;
        private int vao, vbo, ebo;
        private int vertexCount;
        private List<float[]> cloudBlocks;

        private static final float CLOUD_HEIGHT = 192.0f;
        private static final float CLOUD_AREA = 600.0f;
        private static final float CLOUD_SPEED = 0.5f;
        private static final int CLOUD_CLUSTER_COUNT = 30;

        private float cloudOffsetX = 0;

        public void init() throws Exception {
                // Create simple shader that outputs solid white
                cloudShader = new ShaderProgram();
                cloudShader.createVertexShader(
                                "#version 330 core\n" +
                                                "layout (location = 0) in vec3 aPos;\n" +
                                                "uniform mat4 projectionMatrix;\n" +
                                                "uniform mat4 viewMatrix;\n" +
                                                "uniform mat4 modelMatrix;\n" +
                                                "void main() {\n" +
                                                "    gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(aPos, 1.0);\n"
                                                +
                                                "}");
                cloudShader.createFragmentShader(
                                "#version 330 core\n" +
                                                "out vec4 fragColor;\n" +
                                                "uniform vec3 cloudColor;\n" +
                                                "void main() {\n" +
                                                "    fragColor = vec4(cloudColor, 1.0);\n" + // Solid color, fully
                                                                                             // opaque
                                                "}");
                cloudShader.link();
                cloudShader.createUniform("projectionMatrix");
                cloudShader.createUniform("viewMatrix");
                cloudShader.createUniform("modelMatrix");
                cloudShader.createUniform("cloudColor");

                // Generate cloud blocks as clusters
                cloudBlocks = new ArrayList<>();
                Random random = new Random(42);

                for (int i = 0; i < CLOUD_CLUSTER_COUNT; i++) {
                        float clusterX = (random.nextFloat() - 0.5f) * CLOUD_AREA * 2;
                        float clusterZ = (random.nextFloat() - 0.5f) * CLOUD_AREA * 2;

                        int blocksInCluster = 4 + random.nextInt(10);

                        float currentX = clusterX;
                        float currentZ = clusterZ;

                        for (int j = 0; j < blocksInCluster; j++) {
                                float blockSize = 12.0f;
                                cloudBlocks.add(new float[] { currentX, currentZ, blockSize, blockSize });

                                int dir = random.nextInt(4);
                                switch (dir) {
                                        case 0:
                                                currentX += blockSize;
                                                break;
                                        case 1:
                                                currentX -= blockSize;
                                                break;
                                        case 2:
                                                currentZ += blockSize;
                                                break;
                                        case 3:
                                                currentZ -= blockSize;
                                                break;
                                }
                        }
                }

                // Create simple quad VAO
                float[] vertices = {
                                -0.5f, 0, -0.5f,
                                -0.5f, 0, 0.5f,
                                0.5f, 0, 0.5f,
                                0.5f, 0, -0.5f
                };
                int[] indices = { 0, 1, 3, 3, 1, 2 };
                vertexCount = indices.length;

                vao = glGenVertexArrays();
                glBindVertexArray(vao);

                vbo = glGenBuffers();
                glBindBuffer(GL_ARRAY_BUFFER, vbo);
                glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
                glEnableVertexAttribArray(0);
                glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

                ebo = glGenBuffers();
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

                glBindVertexArray(0);
        }

        public void render(Renderer renderer, DayCycleManager dayCycle, Camera camera, float deltaTime) {
                cloudOffsetX -= CLOUD_SPEED * deltaTime;
                if (cloudOffsetX < -CLOUD_AREA * 2) {
                        cloudOffsetX += CLOUD_AREA * 2;
                }

                Vector3f camPos = camera.getPosition();

                glDisable(GL_CULL_FACE);

                cloudShader.bind();
                cloudShader.setUniform("projectionMatrix", camera.getProjectionMatrix());
                cloudShader.setUniform("viewMatrix", camera.getViewMatrix());

                // Light gray clouds (Minecraft style: RGB 191,191,191 = 0.75)
                cloudShader.setUniform("cloudColor", new Vector3f(0.95f, 0.95f, 0.95f));

                glBindVertexArray(vao);

                Matrix4f modelMatrix = new Matrix4f();

                for (float[] cloud : cloudBlocks) {
                        float baseX = cloud[0];
                        float baseZ = cloud[1];
                        float width = cloud[2];
                        float depth = cloud[3];

                        float worldX = baseX + cloudOffsetX;

                        float relX = worldX - camPos.x;
                        while (relX > CLOUD_AREA)
                                relX -= CLOUD_AREA * 2;
                        while (relX < -CLOUD_AREA)
                                relX += CLOUD_AREA * 2;

                        float relZ = baseZ - camPos.z;
                        while (relZ > CLOUD_AREA)
                                relZ -= CLOUD_AREA * 2;
                        while (relZ < -CLOUD_AREA)
                                relZ += CLOUD_AREA * 2;

                        float renderX = camPos.x + relX;
                        float renderZ = camPos.z + relZ;

                        modelMatrix.identity();
                        modelMatrix.translate(renderX, CLOUD_HEIGHT, renderZ);
                        modelMatrix.scale(width, 1, depth);

                        cloudShader.setUniform("modelMatrix", modelMatrix);
                        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
                }

                glBindVertexArray(0);
                cloudShader.unbind();

                glEnable(GL_CULL_FACE);
        }

        public void cleanup() {
                if (cloudShader != null)
                        cloudShader.cleanup();
                glDeleteBuffers(vbo);
                glDeleteBuffers(ebo);
                glDeleteVertexArrays(vao);
        }
}

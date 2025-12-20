package com.craftzero.graphics;

import com.craftzero.engine.Input;
import com.craftzero.graphics.model.ModelPart;
import com.craftzero.graphics.model.PlayerModel;
import com.craftzero.ui.InventoryScreen;

import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.*;

/**
 * Renders the player model in the inventory screen with cursor tracking.
 * The player model looks at the mouse cursor, just like in Minecraft.
 */
public class InventoryPlayerRenderer {

    private ShaderProgram shader;
    private PlayerModel playerModel;
    private Texture playerTexture;
    private Matrix4f modelMatrix;

    // Model scale (Minecraft model units are 1/16th of a block)
    private static final float MODEL_SCALE = 1.0f / 16.0f;

    // Screen dimensions for calculations
    private int windowWidth;
    private int windowHeight;

    public void init() throws Exception {
        // Create simple 3D shader for inventory model
        shader = new ShaderProgram();
        shader.createVertexShader(
                "#version 330 core\n" +
                        "layout (location = 0) in vec3 aPos;\n" +
                        "layout (location = 1) in vec2 aTexCoord;\n" +
                        "layout (location = 2) in vec3 aNormal;\n" +
                        "out vec2 texCoord;\n" +
                        "out vec3 normal;\n" +
                        "uniform mat4 projection;\n" +
                        "uniform mat4 view;\n" +
                        "uniform mat4 modelMatrix;\n" +
                        "void main() {\n" +
                        "    gl_Position = projection * view * modelMatrix * vec4(aPos, 1.0);\n" +
                        "    texCoord = aTexCoord;\n" +
                        "    normal = mat3(modelMatrix) * aNormal;\n" +
                        "}");
        shader.createFragmentShader(
                "#version 330 core\n" +
                        "in vec2 texCoord;\n" +
                        "in vec3 normal;\n" +
                        "out vec4 fragColor;\n" +
                        "uniform sampler2D textureSampler;\n" +
                        "void main() {\n" +
                        "    vec4 texColor = texture(textureSampler, texCoord);\n" +
                        "    if (texColor.a < 0.1) discard;\n" +
                        "    // Simple directional lighting\n" +
                        "    vec3 lightDir = normalize(vec3(0.3, 0.8, 0.5));\n" +
                        "    float diff = max(dot(normalize(normal), lightDir), 0.0);\n" +
                        "    float ambient = 0.5;\n" +
                        "    float lighting = ambient + diff * 0.5;\n" +
                        "    fragColor = vec4(texColor.rgb * lighting, texColor.a);\n" +
                        "}");
        shader.link();
        shader.createUniform("projection");
        shader.createUniform("view");
        shader.createUniform("modelMatrix");
        shader.createUniform("textureSampler");

        // Create player model
        playerModel = new PlayerModel();
        playerModel.buildMeshes();

        // Load player texture
        playerTexture = MobTexture.get("/textures/mob/char.png");

        modelMatrix = new Matrix4f();

        System.out.println("InventoryPlayerRenderer initialized");
    }

    public void updateScreenSize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
    }

    /**
     * Render the player model in the inventory screen.
     * The model looks at the mouse cursor.
     * 
     * @param screen The inventory screen
     */
    public void render(InventoryScreen screen) {
        if (!screen.isOpen() || playerTexture == null)
            return;

        // Get window position and calculate model position
        int winX = screen.getWindowX();
        int winY = screen.getWindowY();
        float scale = InventoryScreen.GUI_SCALE;

        // Player model area in the inventory (black box on left side)
        // The black box is approximately at (26, 8) to (77, 77) in texture pixels
        // Center of the box is around (51, 42) for the body, feet at ~75
        float boxCenterX = 51.0f; // Center X of the black box in texture pixels
        float boxBottomY = 75.0f; // Bottom of the black box (where feet go)

        float modelCenterX = winX + boxCenterX * scale;
        float modelBottomY = winY + boxBottomY * scale;

        // Get mouse position
        float mouseX = (float) Input.getMouseX();
        float mouseY = (float) Input.getMouseY();

        // Calculate normalized position relative to model center
        // -1 to 1 range where screen edges are the limits
        float headCenterY = modelBottomY - 45 * scale; // Approximate head height

        // Calculate normalized offsets (-1 to +1 based on distance from model to screen
        // edge)
        // For X: left edge = -1, right edge = +1
        // Negate because the model faces forward (toward camera)
        float normalizedX = -(mouseX - modelCenterX) / (windowWidth * 0.5f);
        // For Y: top edge = -1, bottom edge = +1
        float normalizedY = (mouseY - headCenterY) / (windowHeight * 0.5f);

        // Clamp to -1 to 1 range
        normalizedX = Math.max(-1.0f, Math.min(1.0f, normalizedX));
        normalizedY = Math.max(-1.0f, Math.min(1.0f, normalizedY));

        // Head rotation - full range when cursor at screen edges
        // Max 40 degrees yaw, 30 degrees pitch
        float headYaw = normalizedX * 40.0f; // Positive = look right when cursor right
        float headPitch = normalizedY * 30.0f; // Positive = look down when cursor below

        // Body rotation - subtle "peeking" effect (about 20% of head yaw)
        float bodyYaw = headYaw * 0.2f;

        // Body lean - lean back when looking up, lean forward when looking down
        // Max 10 degrees lean
        float bodyPitch = normalizedY * 10.0f;

        // Animate the model - head yaw is relative to body, so subtract body
        // contribution
        float headYawRelative = headYaw - bodyYaw;
        playerModel.animate(0, 0, 0, headYawRelative, headPitch, 0, false);

        // Don't set body rotation here - we'll apply lean to the whole model via matrix

        // Setup GL state for 3D rendering in UI
        glDisable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
        glClear(GL_DEPTH_BUFFER_BIT);

        shader.bind();

        // Create orthographic projection for UI-space 3D rendering
        Matrix4f projection = new Matrix4f().ortho(
                0, windowWidth,
                windowHeight, 0,
                -1000, 1000);

        // View matrix - identity since we're positioning via model matrix
        Matrix4f view = new Matrix4f().identity();

        shader.setUniform("projection", projection);
        shader.setUniform("view", view);
        shader.setUniform("textureSampler", 0);

        // Position and scale the model
        float displayScale = scale * 30.0f * MODEL_SCALE;

        modelMatrix.identity();
        // Position at the bottom center of the player preview area
        modelMatrix.translate(modelCenterX, modelBottomY, 100);
        // Flip Y to correct for screen coordinates (Y down) vs model coordinates (Y up)
        modelMatrix.scale(displayScale, -displayScale, displayScale);
        // First rotate 180 degrees so we see the front of the model
        modelMatrix.rotateY((float) Math.toRadians(180));
        // Apply subtle body yaw rotation (peeking)
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        // Apply body lean (whole body tilts forward/back based on cursor Y)
        modelMatrix.rotateX((float) Math.toRadians(bodyPitch));

        // Calculate transforms for all parts
        playerModel.root.calculateTransform(modelMatrix);

        // Render all parts
        playerTexture.bind(0);
        renderModelPart(playerModel.root);
        playerTexture.unbind();

        shader.unbind();

        // Restore GL state
        glDisable(GL_DEPTH_TEST);
    }

    private void renderModelPart(ModelPart part) {
        if (part.getMesh() != null) {
            shader.setUniform("modelMatrix", part.getWorldTransform());
            part.getMesh().render();
        }
        for (ModelPart child : part.getChildren()) {
            renderModelPart(child);
        }
    }

    public void cleanup() {
        if (shader != null)
            shader.cleanup();
        if (playerModel != null)
            playerModel.cleanup();
    }
}

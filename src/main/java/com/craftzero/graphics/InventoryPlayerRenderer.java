package com.craftzero.graphics;

import com.craftzero.engine.Input;
import com.craftzero.graphics.model.ModelPart;
import com.craftzero.graphics.model.PlayerModel;
import com.craftzero.inventory.ItemStack;
import com.craftzero.ui.InventoryScreen;

import org.joml.Matrix4f;

import java.util.List;

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
    private static final float PREVIEW_CENTER_TEX_X = 51.0f;
    private static final float PREVIEW_FOOT_TEX_Y = 75.0f;
    private static final float PREVIEW_HEAD_LOOK_TEX_OFFSET = 50.0f;
    private static final float PREVIEW_ENTITY_SCALE = 30.0f;
    private static final float RELEASE_LOOK_DIVISOR = 40.0f;
    private static final float RELEASE_BODY_YAW = 20.0f;
    private static final float RELEASE_HEAD_YAW = 40.0f;
    private static final float RELEASE_HEAD_PITCH = 20.0f;
    private static final float ARMOR_LAYER_SCALE = 1.025f;

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

        int winX = screen.getWindowX();
        int winY = screen.getWindowY();
        float scale = InventoryScreen.GUI_SCALE;

        float modelCenterX = winX + PREVIEW_CENTER_TEX_X * scale;
        float modelBottomY = winY + PREVIEW_FOOT_TEX_Y * scale;
        float headLookY = modelBottomY - PREVIEW_HEAD_LOOK_TEX_OFFSET * scale;
        float mouseX = (float) Input.getMouseX();
        float mouseY = (float) Input.getMouseY();

        float lookDivisor = RELEASE_LOOK_DIVISOR * scale;
        float lookX = modelCenterX - mouseX;
        float lookY = headLookY - mouseY;
        float yawCurve = (float) Math.atan(lookX / lookDivisor);
        float pitchCurve = (float) Math.atan(lookY / lookDivisor);
        float bodyYaw = yawCurve * RELEASE_BODY_YAW;
        float headYaw = yawCurve * RELEASE_HEAD_YAW;
        float headPitch = -pitchCurve * RELEASE_HEAD_PITCH;
        float headYawRelative = headYaw - bodyYaw;
        playerModel.animate(0, 0, 0, headYawRelative, headPitch, 0, false);

        glDisable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);
        glDepthFunc(GL_LEQUAL);
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

        float displayScale = scale * PREVIEW_ENTITY_SCALE * MODEL_SCALE;

        modelMatrix.identity();
        modelMatrix.translate(modelCenterX, modelBottomY, 100);
        modelMatrix.scale(displayScale, -displayScale, displayScale);
        modelMatrix.rotateY((float) Math.toRadians(180));
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.rotateX((float) Math.toRadians(headPitch));

        playerModel.root.calculateTransform(modelMatrix);
        playerTexture.bind(0);
        renderModelPart(playerModel.root);
        playerTexture.unbind();

        renderArmorLayers(screen.getInventory().getArmor());

        shader.unbind();

        glDisable(GL_DEPTH_TEST);
        glDepthMask(true);
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

    private void renderArmorLayers(ItemStack[] armor) {
        List<PlayerRenderer.ArmorRenderLayer> layers = PlayerRenderer.armorRenderLayers(armor);
        if (layers.isEmpty()) {
            return;
        }
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(-1.0f, -1.0f);
        for (PlayerRenderer.ArmorRenderLayer layer : layers) {
            Texture texture = MobTexture.get(layer.texturePath());
            if (texture == null) {
                continue;
            }
            Matrix4f armorMatrix = new Matrix4f(modelMatrix).scale(ARMOR_LAYER_SCALE);
            playerModel.root.calculateTransform(armorMatrix);
            texture.bind(0);
            renderArmorModelParts(layer);
            texture.unbind();
        }
        glDisable(GL_POLYGON_OFFSET_FILL);
    }

    private void renderArmorModelParts(PlayerRenderer.ArmorRenderLayer layer) {
        if (layer.renders(PlayerRenderer.ArmorModelPart.HEAD)) {
            renderModelPart(playerModel.head);
        }
        if (layer.renders(PlayerRenderer.ArmorModelPart.BODY)) {
            renderModelPart(playerModel.body);
        }
        if (layer.renders(PlayerRenderer.ArmorModelPart.RIGHT_ARM)) {
            renderModelPart(playerModel.rightArm);
        }
        if (layer.renders(PlayerRenderer.ArmorModelPart.LEFT_ARM)) {
            renderModelPart(playerModel.leftArm);
        }
        if (layer.renders(PlayerRenderer.ArmorModelPart.RIGHT_LEG)) {
            renderModelPart(playerModel.rightLeg);
        }
        if (layer.renders(PlayerRenderer.ArmorModelPart.LEFT_LEG)) {
            renderModelPart(playerModel.leftLeg);
        }
    }

    public void cleanup() {
        if (shader != null)
            shader.cleanup();
        if (playerModel != null)
            playerModel.cleanup();
    }
}

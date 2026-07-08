package com.craftzero.graphics.model;

/**
 * Release-era villager model: tall head, protruding nose, robe body, and
 * folded arms. Coordinates use CraftZero's Y-up model convention.
 */
public class VillagerModel {
    public final ModelPart root = new ModelPart();
    public final ModelPart head;
    public final ModelPart nose;
    public final ModelPart body;
    public final ModelPart arms;
    public final ModelPart leftArmSleeve;
    public final ModelPart rightArmSleeve;
    public final ModelPart foldedHands;
    public final ModelPart rightLeg;
    public final ModelPart leftLeg;

    private static final float TEXTURE_SIZE = 64.0f;
    private static final float FOLDED_ARM_ROTATION = 0.75f;

    public VillagerModel() {
        head = new ModelPart()
                .setTextureSize(TEXTURE_SIZE, TEXTURE_SIZE)
                .setTextureOffset(0, 0)
                .setPivot(0, 22, 0)
                .addBox(-4, 0, -4, 8, 10, 8);
        nose = new ModelPart()
                .setTextureSize(TEXTURE_SIZE, TEXTURE_SIZE)
                .setTextureOffset(24, 0)
                .setPivot(0, 0, 0)
                .addBox(-1, 1, -6, 2, 4, 2);

        body = new ModelPart()
                .setTextureSize(TEXTURE_SIZE, TEXTURE_SIZE)
                .setTextureOffset(16, 20)
                .setPivot(0, 10, 0)
                .addBox(-4, 0, -3, 8, 12, 6);

        arms = new ModelPart()
                .setPivot(0, 18, -1)
                .addBox(0, 0, 0, 0, 0, 0);
        leftArmSleeve = new ModelPart()
                .setTextureSize(TEXTURE_SIZE, TEXTURE_SIZE)
                .setTextureOffset(44, 22)
                .setPivot(0, 0, 0)
                .addBox(-8, -2, -2, 4, 8, 4);
        rightArmSleeve = new ModelPart()
                .setTextureSize(TEXTURE_SIZE, TEXTURE_SIZE)
                .setTextureOffset(44, 22)
                .setPivot(0, 0, 0)
                .addBox(4, -2, -2, 4, 8, 4);
        foldedHands = new ModelPart()
                .setTextureSize(TEXTURE_SIZE, TEXTURE_SIZE)
                .setTextureOffset(40, 38)
                .setPivot(0, 0, 0)
                .addBox(-4, 2, -2, 8, 4, 4);

        rightLeg = new ModelPart()
                .setTextureSize(TEXTURE_SIZE, TEXTURE_SIZE)
                .setTextureOffset(0, 22)
                .setPivot(-2, 12, 0)
                .addBox(-2, -12, -2, 4, 12, 4);
        leftLeg = new ModelPart()
                .setTextureSize(TEXTURE_SIZE, TEXTURE_SIZE)
                .setTextureOffset(0, 22)
                .setPivot(2, 12, 0)
                .addBox(-2, -12, -2, 4, 12, 4);

        head.addChild(nose);
        arms.addChild(leftArmSleeve);
        arms.addChild(rightArmSleeve);
        arms.addChild(foldedHands);

        root.addChild(head);
        root.addChild(body);
        root.addChild(arms);
        root.addChild(rightLeg);
        root.addChild(leftLeg);
    }

    public void buildMeshes() {
        root.buildMesh();
    }

    public void animate(float limbSwing, float limbSwingAmount, float headYaw, float headPitch) {
        headYaw = Math.max(-60, Math.min(60, headYaw));
        headPitch = Math.max(-45, Math.min(45, headPitch));
        head.setRotation((float) Math.toRadians(headPitch), (float) Math.toRadians(headYaw), 0.0f);
        body.setRotation(0.0f, 0.0f, 0.0f);
        arms.setRotation(FOLDED_ARM_ROTATION, 0.0f, 0.0f);

        float legSwing = (float) Math.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;
        rightLeg.setRotation(legSwing, 0.0f, 0.0f);
        leftLeg.setRotation(-legSwing, 0.0f, 0.0f);
    }

    public void cleanup() {
        root.cleanup();
    }
}

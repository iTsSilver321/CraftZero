package com.craftzero.graphics.model;

public class DragonModel {
    public final ModelPart root = new ModelPart();
    private final ModelPart head;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart tail;

    public DragonModel() {
        ModelPart body = new ModelPart()
                .setTextureSize(256, 256)
                .setTextureOffset(0, 0)
                .setPivot(0, 18, 0)
                .addBox(-12, -12, -32, 24, 24, 64);
        head = new ModelPart()
                .setTextureSize(256, 256)
                .setTextureOffset(0, 88)
                .setPivot(0, 14, -42)
                .addBox(-8, -8, -16, 16, 16, 16);
        leftWing = new ModelPart()
                .setTextureSize(256, 256)
                .setTextureOffset(112, 88)
                .setPivot(12, 12, -8)
                .addBox(0, -2, -8, 48, 4, 24);
        rightWing = new ModelPart()
                .setTextureSize(256, 256)
                .setTextureOffset(112, 88)
                .setPivot(-12, 12, -8)
                .addBox(-48, -2, -8, 48, 4, 24);
        tail = new ModelPart()
                .setTextureSize(256, 256)
                .setTextureOffset(0, 120)
                .setPivot(0, 18, 32)
                .addBox(-5, -5, 0, 10, 10, 56);
        root.addChild(body);
        root.addChild(head);
        root.addChild(leftWing);
        root.addChild(rightWing);
        root.addChild(tail);
    }

    public void buildMeshes() {
        root.buildMesh();
    }

    public void animate(float ageInTicks, float headYaw, float headPitch) {
        head.setRotation((float) Math.toRadians(headPitch) * 0.25f, (float) Math.toRadians(headYaw) * 0.25f, 0.0f);
        float flap = (float) Math.sin(ageInTicks * 0.18f) * 0.35f;
        leftWing.setRotation(0.0f, 0.0f, -0.25f - flap);
        rightWing.setRotation(0.0f, 0.0f, 0.25f + flap);
        tail.setRotation((float) Math.sin(ageInTicks * 0.08f) * 0.12f, 0.0f, 0.0f);
    }

    public void cleanup() {
        root.cleanup();
    }
}

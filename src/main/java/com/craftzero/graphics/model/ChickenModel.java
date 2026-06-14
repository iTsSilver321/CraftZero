package com.craftzero.graphics.model;

/**
 * Classic Minecraft chicken model with head, bill, chin, body, legs, and wings.
 */
public class ChickenModel {
    public final ModelPart head;
    public final ModelPart bill;
    public final ModelPart chin;
    public final ModelPart body;
    public final ModelPart rightLeg;
    public final ModelPart leftLeg;
    public final ModelPart rightWing;
    public final ModelPart leftWing;
    public final ModelPart root;

    public ChickenModel() {
        root = new ModelPart();

        head = new ModelPart()
                .setTextureOffset(0, 0)
                .setPivot(0, 9, -4)
                .addBox(-2, 0, -2, 4, 6, 3);

        bill = new ModelPart()
                .setTextureOffset(14, 0)
                .setPivot(0, 9, -4)
                .addBox(-2, 2, -4, 4, 2, 2);

        chin = new ModelPart()
                .setTextureOffset(14, 4)
                .setPivot(0, 9, -4)
                .addBox(-1, 0, -3, 2, 2, 2);

        body = new ModelPart()
                .setTextureOffset(0, 9)
                .setPivot(0, 8, 0)
                .addBox(-3, -4, -3, 6, 8, 6);

        rightLeg = new ModelPart()
                .setTextureOffset(26, 0)
                .setPivot(-2, 5, 1)
                .addBox(-1, -5, -3, 3, 5, 3);

        leftLeg = new ModelPart()
                .setTextureOffset(26, 0)
                .setPivot(1, 5, 1)
                .addBox(-1, -5, -3, 3, 5, 3);

        rightWing = new ModelPart()
                .setTextureOffset(24, 13)
                .setPivot(-4, 11, 0)
                .addBox(0, -4, -3, 1, 4, 6);

        leftWing = new ModelPart()
                .setTextureOffset(24, 13)
                .setPivot(4, 11, 0)
                .addBox(-1, -4, -3, 1, 4, 6);

        root.addChild(head);
        root.addChild(bill);
        root.addChild(chin);
        root.addChild(body);
        root.addChild(rightLeg);
        root.addChild(leftLeg);
        root.addChild(rightWing);
        root.addChild(leftWing);
    }

    public void buildMeshes() {
        root.buildMesh();
    }

    public void animate(float limbSwing, float limbSwingAmount, float ageInTicks,
            float headYaw, float headPitch, boolean airborne) {
        headYaw = clamp(headYaw, -60, 60);
        headPitch = clamp(headPitch, -35, 35);
        float headPitchRad = (float) Math.toRadians(headPitch);
        float headYawRad = (float) Math.toRadians(headYaw);

        head.setRotation(headPitchRad, headYawRad, 0);
        bill.setRotation(headPitchRad, headYawRad, 0);
        chin.setRotation(headPitchRad, headYawRad, 0);

        body.setRotation((float) Math.PI / 2.0f, 0, 0);

        float legSwing = (float) Math.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;
        rightLeg.setRotation(legSwing, 0, 0);
        leftLeg.setRotation(-legSwing, 0, 0);

        float flap = airborne
                ? 0.85f + (float) Math.sin(ageInTicks * 1.6f) * 0.45f
                : (float) Math.sin(ageInTicks * 0.25f) * 0.04f;
        rightWing.setRotation(0, 0, flap);
        leftWing.setRotation(0, 0, -flap);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public void cleanup() {
        root.cleanup();
    }
}

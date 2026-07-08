package com.craftzero.graphics.model;

public class SnowGolemModel {
    public final ModelPart root = new ModelPart();
    public final ModelPart head;
    public final ModelPart body;
    public final ModelPart bottom;
    public final ModelPart leftArm;
    public final ModelPart rightArm;

    private static final float SOURCE_MODEL_BOTTOM_Y = 24.0f;
    private static final float SOURCE_HEAD_PIVOT_Y = 4.0f;
    private static final float SOURCE_BODY_PIVOT_Y = 13.0f;
    private static final float SOURCE_ARM_PIVOT_Y = 6.0f;
    private static final float SOURCE_BOTTOM_PIVOT_Y = 24.0f;
    private static final float HEAD_PIVOT_Y = sourceToCraftY(SOURCE_HEAD_PIVOT_Y);
    private static final float BODY_PIVOT_Y = sourceToCraftY(SOURCE_BODY_PIVOT_Y);
    private static final float ARM_PIVOT_Y = sourceToCraftY(SOURCE_ARM_PIVOT_Y);
    private static final float BOTTOM_PIVOT_Y = sourceToCraftY(SOURCE_BOTTOM_PIVOT_Y);
    private static final float ARM_PIVOT_RADIUS = 5.0f;

    public SnowGolemModel() {
        head = new ModelPart()
                .setTextureOffset(0, 0)
                .setPivot(0, HEAD_PIVOT_Y, 0)
                .addBox(-4, 0, -4, 8, 8, 8);
        body = new ModelPart()
                .setTextureOffset(0, 16)
                .setPivot(0, BODY_PIVOT_Y, 0)
                .addBox(-5, 0, -5, 10, 10, 10);
        bottom = new ModelPart()
                .setTextureOffset(24, 16)
                .setPivot(0, BOTTOM_PIVOT_Y, 0)
                .addBox(-6, 0, -6, 12, 12, 12);
        leftArm = new ModelPart()
                .setTextureOffset(32, 0)
                .setPivot(ARM_PIVOT_RADIUS, ARM_PIVOT_Y, 0)
                .addBox(-1, -1, -1, 12, 2, 2);
        rightArm = new ModelPart()
                .setTextureOffset(32, 0)
                .setPivot(-ARM_PIVOT_RADIUS, ARM_PIVOT_Y, 0)
                .addBox(-1, -1, -1, 12, 2, 2);

        root.addChild(bottom);
        root.addChild(body);
        root.addChild(head);
        root.addChild(leftArm);
        root.addChild(rightArm);
    }

    private static float sourceToCraftY(float sourceY) {
        return SOURCE_MODEL_BOTTOM_Y - sourceY;
    }

    public void buildMeshes() {
        root.buildMesh();
    }

    public void animate(float limbSwing, float limbSwingAmount, float ageInTicks, float headYaw, float headPitch) {
        float headYawRad = (float) Math.toRadians(headYaw);
        float headPitchRad = (float) Math.toRadians(headPitch);
        float bodyYaw = headYawRad * 0.25f;
        float sin = (float) Math.sin(bodyYaw);
        float cos = (float) Math.cos(bodyYaw);

        head.setRotation(headPitchRad, headYawRad, 0.0f);
        body.setRotation(0.0f, bodyYaw, 0.0f);
        bottom.setRotation(0.0f, 0.0f, 0.0f);
        leftArm.setPivot(cos * ARM_PIVOT_RADIUS, ARM_PIVOT_Y, -sin * ARM_PIVOT_RADIUS);
        rightArm.setPivot(-cos * ARM_PIVOT_RADIUS, ARM_PIVOT_Y, sin * ARM_PIVOT_RADIUS);
        leftArm.setRotation(0.0f, bodyYaw, 1.0f);
        rightArm.setRotation(0.0f, (float) Math.PI + bodyYaw, -1.0f);
    }

    public void cleanup() {
        root.cleanup();
    }
}

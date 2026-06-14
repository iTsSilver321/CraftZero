package com.craftzero.graphics.model;

/**
 * Classic Minecraft spider model: head, neck, body, and eight angled legs.
 * Dimensions are in model pixels where 16 pixels equals one block.
 */
public class SpiderModel {
    public final ModelPart head;
    public final ModelPart neck;
    public final ModelPart body;
    public final ModelPart leg1;
    public final ModelPart leg2;
    public final ModelPart leg3;
    public final ModelPart leg4;
    public final ModelPart leg5;
    public final ModelPart leg6;
    public final ModelPart leg7;
    public final ModelPart leg8;
    public final ModelPart[] legs;
    public final ModelPart root;

    public SpiderModel() {
        root = new ModelPart();

        head = new ModelPart()
                .setTextureOffset(32, 4)
                .setPivot(0, 9, -3)
                .addBox(-4, -4, -8, 8, 8, 8);

        neck = new ModelPart()
                .setTextureOffset(0, 0)
                .setPivot(0, 9, 0)
                .addBox(-3, -3, -3, 6, 6, 6);

        body = new ModelPart()
                .setTextureOffset(0, 12)
                .setPivot(0, 9, 9)
                .addBox(-5, -4, -6, 10, 8, 12);

        leg1 = leftLeg(2);
        leg2 = rightLeg(2);
        leg3 = leftLeg(1);
        leg4 = rightLeg(1);
        leg5 = leftLeg(0);
        leg6 = rightLeg(0);
        leg7 = leftLeg(-1);
        leg8 = rightLeg(-1);
        legs = new ModelPart[] { leg1, leg2, leg3, leg4, leg5, leg6, leg7, leg8 };

        root.addChild(head);
        root.addChild(neck);
        root.addChild(body);
        for (ModelPart leg : legs) {
            root.addChild(leg);
        }
    }

    private ModelPart leftLeg(float z) {
        return new ModelPart()
                .setTextureOffset(18, 0)
                .setPivot(-4, 9, z)
                .addBox(-15, -1, -1, 16, 2, 2);
    }

    private ModelPart rightLeg(float z) {
        return new ModelPart()
                .setTextureOffset(18, 0)
                .setPivot(4, 9, z)
                .addBox(-1, -1, -1, 16, 2, 2);
    }

    public void buildMeshes() {
        root.buildMesh();
    }

    public void animate(float limbSwing, float limbSwingAmount, float ageInTicks,
            float headYaw, float headPitch) {
        headYaw = clamp(headYaw, -60, 60);
        headPitch = clamp(headPitch, -45, 45);
        head.setRotation((float) Math.toRadians(headPitch), (float) Math.toRadians(headYaw), 0);
        neck.setRotation(0, 0, 0);
        body.setRotation(0, 0, 0);

        float quarterPi = (float) Math.PI / 4.0f;
        float eighthPi = (float) Math.PI / 8.0f;

        leg1.setRotation(0, eighthPi * 2.0f, -quarterPi);
        leg2.setRotation(0, -eighthPi * 2.0f, quarterPi);
        leg3.setRotation(0, eighthPi, -quarterPi * 0.74f);
        leg4.setRotation(0, -eighthPi, quarterPi * 0.74f);
        leg5.setRotation(0, -eighthPi, -quarterPi * 0.74f);
        leg6.setRotation(0, eighthPi, quarterPi * 0.74f);
        leg7.setRotation(0, -eighthPi * 2.0f, -quarterPi);
        leg8.setRotation(0, eighthPi * 2.0f, quarterPi);

        float phase = limbSwing * 0.6662f;
        float yaw1 = -(float) Math.cos(phase * 2.0f) * 0.4f * limbSwingAmount;
        float yaw2 = -(float) Math.cos(phase * 2.0f + Math.PI) * 0.4f * limbSwingAmount;
        float yaw3 = -(float) Math.cos(phase * 2.0f + Math.PI / 2.0f) * 0.4f * limbSwingAmount;
        float yaw4 = -(float) Math.cos(phase * 2.0f + Math.PI * 1.5f) * 0.4f * limbSwingAmount;
        float roll1 = Math.abs((float) Math.sin(phase) * 0.4f) * limbSwingAmount;
        float roll2 = Math.abs((float) Math.sin(phase + Math.PI) * 0.4f) * limbSwingAmount;
        float roll3 = Math.abs((float) Math.sin(phase + Math.PI / 2.0f) * 0.4f) * limbSwingAmount;
        float roll4 = Math.abs((float) Math.sin(phase + Math.PI * 1.5f) * 0.4f) * limbSwingAmount;

        addLegRotation(leg1, 0, yaw1, roll1);
        addLegRotation(leg2, 0, -yaw1, -roll1);
        addLegRotation(leg3, 0, yaw2, roll2);
        addLegRotation(leg4, 0, -yaw2, -roll2);
        addLegRotation(leg5, 0, yaw3, roll3);
        addLegRotation(leg6, 0, -yaw3, -roll3);
        addLegRotation(leg7, 0, yaw4, roll4);
        addLegRotation(leg8, 0, -yaw4, -roll4);
    }

    private void addLegRotation(ModelPart leg, float x, float y, float z) {
        leg.setRotation(leg.getRotationX() + x, leg.getRotationY() + y, leg.getRotationZ() + z);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public void cleanup() {
        root.cleanup();
    }
}

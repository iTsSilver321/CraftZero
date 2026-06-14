package com.craftzero.graphics.model;

public class CreeperModel {
    public final ModelPart root;
    public final ModelPart head;
    public final ModelPart body;
    public final ModelPart rightHindLeg;
    public final ModelPart leftHindLeg;
    public final ModelPart rightFrontLeg;
    public final ModelPart leftFrontLeg;

    public CreeperModel() {
        root = new ModelPart();

        head = new ModelPart()
                .setTextureOffset(0, 0)
                .setPivot(0, 18, 0)
                .addBox(-4, 0, -4, 8, 8, 8);

        body = new ModelPart()
                .setTextureOffset(16, 16)
                .setPivot(0, 6, 0)
                .addBox(-4, 0, -2, 8, 12, 4);

        rightHindLeg = leg(-2, 6, 4);
        leftHindLeg = leg(2, 6, 4);
        rightFrontLeg = leg(-2, 6, -4);
        leftFrontLeg = leg(2, 6, -4);

        root.addChild(head);
        root.addChild(body);
        root.addChild(rightHindLeg);
        root.addChild(leftHindLeg);
        root.addChild(rightFrontLeg);
        root.addChild(leftFrontLeg);
    }

    private ModelPart leg(float x, float y, float z) {
        return new ModelPart()
                .setTextureOffset(0, 16)
                .setPivot(x, y, z)
                .addBox(-2, -6, -2, 4, 6, 4);
    }

    public void buildMeshes() {
        root.buildMesh();
    }

    public void animate(float limbSwing, float limbSwingAmount, float headYaw, float headPitch) {
        headYaw = Math.max(-60, Math.min(60, headYaw));
        headPitch = Math.max(-45, Math.min(45, headPitch));
        head.setRotation((float) Math.toRadians(headPitch), (float) Math.toRadians(headYaw), 0);
        body.setRotation(0, 0, 0);

        float phase = limbSwing * 0.6662f;
        float swingA = (float) Math.cos(phase) * 1.4f * limbSwingAmount;
        float swingB = (float) Math.cos(phase + Math.PI) * 1.4f * limbSwingAmount;
        rightHindLeg.setRotation(swingA, 0, 0);
        leftHindLeg.setRotation(swingB, 0, 0);
        rightFrontLeg.setRotation(swingB, 0, 0);
        leftFrontLeg.setRotation(swingA, 0, 0);
    }

    public void cleanup() {
        root.cleanup();
    }
}

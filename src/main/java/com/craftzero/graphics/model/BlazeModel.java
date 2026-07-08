package com.craftzero.graphics.model;

public class BlazeModel {
    private static final float MODEL_Y_BASE = 18.0f;
    private static final float TWO_RING_BOB_SPEED = 0.25f;
    private static final float LOWER_RING_BOB_SPEED = 0.5f;

    public final ModelPart root = new ModelPart();
    public final ModelPart head;
    public final ModelPart[] rods = new ModelPart[12];

    public BlazeModel() {
        head = new ModelPart()
                .setTextureOffset(0, 0)
                .setPivot(0, 18, 0)
                .addBox(-4, -4, -4, 8, 8, 8);
        root.addChild(head);
        for (int i = 0; i < rods.length; i++) {
            rods[i] = new ModelPart()
                    .setTextureOffset(0, 16)
                    .setPivot(0, 12, 0)
                    .addBox(-1, -4, -1, 2, 8, 2);
            root.addChild(rods[i]);
        }
    }

    public void buildMeshes() {
        root.buildMesh();
    }

    public void animate(float ageInTicks, float headYaw, float headPitch) {
        head.setRotation((float) Math.toRadians(headPitch), (float) Math.toRadians(headYaw), 0);
        float angle = ageInTicks * (float) Math.PI * -0.1f;
        for (int i = 0; i < 4; i++) {
            float sourceY = -2.0f + (float) Math.cos((i * 2.0f + ageInTicks) * TWO_RING_BOB_SPEED);
            setRodPose(i, angle, 9.0f, sourceY);
            angle += 1.0f;
        }

        angle = 0.7853982f + ageInTicks * (float) Math.PI * 0.03f;
        for (int i = 4; i < 8; i++) {
            float sourceY = 2.0f + (float) Math.cos((i * 2.0f + ageInTicks) * TWO_RING_BOB_SPEED);
            setRodPose(i, angle, 7.0f, sourceY);
            angle += 1.0f;
        }

        angle = 0.47123894f + ageInTicks * (float) Math.PI * -0.05f;
        for (int i = 8; i < 12; i++) {
            float sourceY = 11.0f + (float) Math.cos((i * 1.5f + ageInTicks) * LOWER_RING_BOB_SPEED);
            setRodPose(i, angle, 5.0f, sourceY);
            angle += 1.0f;
        }
    }

    private void setRodPose(int index, float angle, float radius, float sourceY) {
        rods[index].setPivot((float) Math.cos(angle) * radius, MODEL_Y_BASE - sourceY,
                (float) Math.sin(angle) * radius);
        rods[index].setRotation(0.0f, 0.0f, 0.0f);
    }

    public void cleanup() {
        root.cleanup();
    }
}

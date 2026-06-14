package com.craftzero.graphics.model;

public class BlazeModel {
    public final ModelPart root = new ModelPart();
    private final ModelPart head;
    private final ModelPart[] rods = new ModelPart[12];

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
        for (int i = 0; i < rods.length; i++) {
            float ring = i < 4 ? 0.0f : i < 8 ? 1.0f : 2.0f;
            float angle = ageInTicks * (0.08f + ring * 0.015f) + i * ((float) Math.PI / 2.0f);
            float radius = i < 4 ? 9.0f : i < 8 ? 7.0f : 5.0f;
            float y = i < 4 ? 18.0f : i < 8 ? 12.0f : 6.0f;
            rods[i].setPivot((float) Math.cos(angle) * radius, y, (float) Math.sin(angle) * radius);
            rods[i].setRotation(0, angle, 0);
        }
    }

    public void cleanup() {
        root.cleanup();
    }
}

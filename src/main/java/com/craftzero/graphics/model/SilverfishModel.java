package com.craftzero.graphics.model;

public class SilverfishModel {
    public final ModelPart root = new ModelPart();
    private final ModelPart[] segments = new ModelPart[5];

    public SilverfishModel() {
        for (int i = 0; i < segments.length; i++) {
            float w = i == 0 ? 3 : 4;
            float h = i == 0 ? 3 : 4;
            float d = 3.0f;
            segments[i] = new ModelPart()
                    .setTextureOffset(0, i * 5)
                    .setPivot(0, 2, -6 + i * 3)
                    .addBox(-w / 2.0f, 0, -d / 2.0f, w, h, d);
            root.addChild(segments[i]);
        }
    }

    public void buildMeshes() {
        root.buildMesh();
    }

    public void animate(float limbSwing, float limbSwingAmount) {
        for (int i = 0; i < segments.length; i++) {
            segments[i].setRotation(0, (float) Math.sin(limbSwing * 0.9f + i * 0.7f) * 0.35f * limbSwingAmount, 0);
        }
    }

    public void cleanup() {
        root.cleanup();
    }
}

package com.craftzero.graphics.model;

public class SlimeModel {
    public final ModelPart root = new ModelPart();
    public final ModelPart body;

    public SlimeModel() {
        body = new ModelPart()
                .setTextureOffset(0, 0)
                .setPivot(0, 0, 0)
                .addBox(-8, 0, -8, 16, 16, 16);
        root.addChild(body);
    }

    public void buildMeshes() {
        root.buildMesh();
    }

    public void animate(float ageInTicks) {
        float squish = 1.0f + (float) Math.sin(ageInTicks * 0.35f) * 0.04f;
        body.setScale(1.0f + (squish - 1.0f) * 0.5f, 2.0f - squish, 1.0f + (squish - 1.0f) * 0.5f);
    }

    public void cleanup() {
        root.cleanup();
    }
}

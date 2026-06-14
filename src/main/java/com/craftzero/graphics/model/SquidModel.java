package com.craftzero.graphics.model;

public class SquidModel {
    public final ModelPart root = new ModelPart();
    private final ModelPart body;
    private final ModelPart[] tentacles = new ModelPart[8];

    public SquidModel() {
        body = new ModelPart()
                .setTextureOffset(0, 0)
                .setPivot(0, 12, 0)
                .addBox(-6, -8, -6, 12, 16, 12);
        root.addChild(body);
        for (int i = 0; i < tentacles.length; i++) {
            double angle = Math.PI * 2.0 * i / tentacles.length;
            float px = (float) Math.cos(angle) * 5.0f;
            float pz = (float) Math.sin(angle) * 5.0f;
            tentacles[i] = new ModelPart()
                    .setTextureOffset(48, 0)
                    .setPivot(px, 4, pz)
                    .addBox(-1, -16, -1, 2, 16, 2);
            root.addChild(tentacles[i]);
        }
    }

    public void buildMeshes() {
        root.buildMesh();
    }

    public void animate(float ageInTicks) {
        for (int i = 0; i < tentacles.length; i++) {
            float wave = (float) Math.sin(ageInTicks * 0.2f + i * 0.7f) * 0.35f;
            tentacles[i].setRotation(wave, 0, 0);
        }
    }

    public void cleanup() {
        root.cleanup();
    }
}

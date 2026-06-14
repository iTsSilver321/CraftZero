package com.craftzero.graphics.model;

public class GhastModel {
    public final ModelPart root = new ModelPart();
    private final ModelPart[] tentacles = new ModelPart[9];

    public GhastModel() {
        ModelPart body = new ModelPart()
                .setTextureSize(64, 32)
                .setTextureOffset(0, 0)
                .setPivot(0, 20, 0)
                .addBox(-16, -16, -16, 32, 32, 32);
        root.addChild(body);
        int index = 0;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                tentacles[index] = new ModelPart()
                        .setTextureSize(64, 32)
                        .setTextureOffset(0, 16)
                        .setPivot(x * 10, 4, z * 10)
                        .addBox(-1.5f, -16, -1.5f, 3, 16, 3);
                root.addChild(tentacles[index]);
                index++;
            }
        }
    }

    public void buildMeshes() {
        root.buildMesh();
    }

    public void animate(float ageInTicks) {
        for (int i = 0; i < tentacles.length; i++) {
            tentacles[i].setRotation((float) Math.sin(ageInTicks * 0.12f + i) * 0.25f, 0, 0);
        }
    }

    public void cleanup() {
        root.cleanup();
    }
}

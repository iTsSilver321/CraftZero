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

    public void animate(float squishAmount, int size) {
        SquishScale scale = scaleFor(squishAmount, size);
        body.setScale(scale.horizontal(), scale.vertical(), scale.horizontal());
    }

    public static SquishScale scaleFor(float squishAmount, int size) {
        float safeSize = Math.max(1, size);
        float normalized = squishAmount / (safeSize * 0.5f + 1.0f);
        normalized = Math.max(-0.9f, normalized);
        float horizontal = 1.0f / (normalized + 1.0f);
        return new SquishScale(horizontal, 1.0f / horizontal);
    }

    public void cleanup() {
        root.cleanup();
    }

    public record SquishScale(float horizontal, float vertical) {
    }
}

package com.craftzero.inventory;

/**
 * Per-item hand rendering hints shared by first- and third-person renderers.
 */
public record ItemRenderProfile(
        ModelKind modelKind,
        float firstPersonScale,
        float thirdPersonScale,
        float firstPersonOffsetX,
        float firstPersonOffsetY,
        float firstPersonOffsetZ,
        float firstPersonEquipDrop,
        float firstPersonRotX,
        float firstPersonRotY,
        float firstPersonRotZ,
        float thirdPersonRotX,
        float thirdPersonRotY,
        float thirdPersonRotZ,
        float thirdPersonOffsetX,
        float thirdPersonOffsetY,
        float thirdPersonOffsetZ) {

    public enum ModelKind {
        BLOCK,
        SPRITE
    }

    public static ItemRenderProfile block() {
        return new ItemRenderProfile(
                ModelKind.BLOCK,
                0.46f,
                0.52f,
                0.56f, -0.54f, -0.78f, 0.70f,
                18.0f, 42.0f, 10.0f,
                0.0f, -45.0f, 0.0f,
                0.10f, -0.15f, -0.55f);
    }

    public static ItemRenderProfile toolSprite() {
        return sprite(0.52f, 0.40f, 0.0f, 15.0f, -45.0f);
    }

    public static ItemRenderProfile materialSprite() {
        return sprite(0.38f, 0.32f, 0.0f, 12.0f, -35.0f);
    }

    public static ItemRenderProfile skinnySprite() {
        return sprite(0.42f, 0.34f, 0.0f, 15.0f, -45.0f);
    }

    public static ItemRenderProfile largeSprite() {
        return sprite(0.45f, 0.36f, 0.0f, 12.0f, -35.0f);
    }

    private static ItemRenderProfile sprite(float firstScale, float thirdScale,
            float firstRotX, float firstRotY, float firstRotZ) {
        return new ItemRenderProfile(
                ModelKind.SPRITE,
                firstScale,
                thirdScale,
                0.58f, -0.50f, -0.72f, 0.70f,
                firstRotX, firstRotY, firstRotZ,
                180.0f, 0.0f, 45.0f,
                0.15f, -0.75f, -0.55f);
    }
}

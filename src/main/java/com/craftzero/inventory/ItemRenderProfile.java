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

    private static final float RELEASE_SPRITE_FIRST_PERSON_SCALE = 0.37f;
    private static final float RELEASE_SPRITE_FIRST_PERSON_X = 0.57f;
    private static final float RELEASE_SPRITE_FIRST_PERSON_Y = -0.57f;
    private static final float RELEASE_SPRITE_FIRST_PERSON_Z = -0.88f;
    private static final float RELEASE_SPRITE_FIRST_PERSON_YAW = 31.0f;
    private static final float RELEASE_SPRITE_FIRST_PERSON_EQUIP_DROP = 0.60f;

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
        return sprite(RELEASE_SPRITE_FIRST_PERSON_SCALE, 0.40f,
                0.0f, RELEASE_SPRITE_FIRST_PERSON_YAW, 0.0f);
    }

    public static ItemRenderProfile materialSprite() {
        return sprite(RELEASE_SPRITE_FIRST_PERSON_SCALE, 0.32f,
                0.0f, RELEASE_SPRITE_FIRST_PERSON_YAW, 0.0f);
    }

    public static ItemRenderProfile skinnySprite() {
        return sprite(RELEASE_SPRITE_FIRST_PERSON_SCALE, 0.34f,
                0.0f, RELEASE_SPRITE_FIRST_PERSON_YAW, 0.0f);
    }

    public static ItemRenderProfile largeSprite() {
        return sprite(RELEASE_SPRITE_FIRST_PERSON_SCALE, 0.36f,
                0.0f, RELEASE_SPRITE_FIRST_PERSON_YAW, 0.0f);
    }

    public static ItemRenderProfile terrainSprite() {
        return sprite(RELEASE_SPRITE_FIRST_PERSON_SCALE, 0.34f,
                0.0f, RELEASE_SPRITE_FIRST_PERSON_YAW, 0.0f);
    }

    private static ItemRenderProfile sprite(float firstScale, float thirdScale,
            float firstRotX, float firstRotY, float firstRotZ) {
        return sprite(firstScale, thirdScale, firstRotX, firstRotY, firstRotZ,
                RELEASE_SPRITE_FIRST_PERSON_X,
                RELEASE_SPRITE_FIRST_PERSON_Y,
                RELEASE_SPRITE_FIRST_PERSON_Z);
    }

    private static ItemRenderProfile sprite(float firstScale, float thirdScale,
            float firstRotX, float firstRotY, float firstRotZ,
            float firstOffsetX, float firstOffsetY, float firstOffsetZ) {
        return new ItemRenderProfile(
                ModelKind.SPRITE,
                firstScale,
                thirdScale,
                firstOffsetX, firstOffsetY, firstOffsetZ, RELEASE_SPRITE_FIRST_PERSON_EQUIP_DROP,
                firstRotX, firstRotY, firstRotZ,
                180.0f, 0.0f, 45.0f,
                0.15f, -0.75f, -0.55f);
    }
}

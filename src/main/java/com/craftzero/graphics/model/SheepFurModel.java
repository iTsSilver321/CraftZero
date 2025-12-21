package com.craftzero.graphics.model;

public class SheepFurModel extends QuadrupedModel {

        // 1. Leg Scale: 1.01f (Thin)
        private static final float LEG_SCALE = 1.01f;
        private static final float HEAD_SCALE = 1.05f;
        private static final float BODY_SCALE = 1.10f;

        public SheepFurModel() {
                super();

                // --- 1. LEGS (GLITCH FIXED & TALLER) ---
                // Offset Y: 6.0f
                // - LOCKED at 6.0f.
                // - This is the "Magic Number" that matches the bone pivot.
                // - Changing this causes the "see through" / "disappearing" glitch.
                // Height: 5.2f
                // - We increased height (was 4.5f) to make the fur visually reach higher
                // and cover the hip gap, without moving the anchor point.
                float legHeight = 5.2f;
                float legY = 6.0f;
                float legOffsetX = 2.8f;

                float legOffsetZFront = -4.0f;
                float legOffsetZBack = 5.0f;

                frontRightLeg = new ModelPart(4, legHeight, 4).setTextureOffset(0, 16)
                                .setOffset(-legOffsetX, legY, legOffsetZFront).setPivot(0, 12, 0);
                frontLeftLeg = new ModelPart(4, legHeight, 4).setTextureOffset(0, 16)
                                .setOffset(legOffsetX, legY, legOffsetZFront).setPivot(0, 12, 0);
                backRightLeg = new ModelPart(4, legHeight, 4).setTextureOffset(0, 16)
                                .setOffset(-legOffsetX, legY, legOffsetZBack).setPivot(0, 12, 0);
                backLeftLeg = new ModelPart(4, legHeight, 4).setTextureOffset(0, 16)
                                .setOffset(legOffsetX, legY, legOffsetZBack).setPivot(0, 12, 0);

                // Scale 1.01f
                frontRightLeg.setScale(LEG_SCALE, LEG_SCALE, LEG_SCALE);
                frontLeftLeg.setScale(LEG_SCALE, LEG_SCALE, LEG_SCALE);
                backRightLeg.setScale(LEG_SCALE, LEG_SCALE, LEG_SCALE);
                backLeftLeg.setScale(LEG_SCALE, LEG_SCALE, LEG_SCALE);

                // --- 2. BODY ---
                body = new ModelPart(7.9f, 14.0f, 6.2f)
                                .setTextureOffset(28, 8)
                                .setOffset(0, 15.1f, 1.0f)
                                .setPivot(0, 0, 0);

                body.setScale(BODY_SCALE, BODY_SCALE, BODY_SCALE);
                body.setRotation((float) Math.PI / 2.0f, 0, (float) Math.PI);

                // --- 3. HEAD ---
                head = new ModelPart(6, 6, 4)
                                .setTextureOffset(0, 0)
                                .setOffset(0, 18.0f, -5.9f)
                                .setPivot(0, 0, 4);

                head.setScale(HEAD_SCALE, HEAD_SCALE, HEAD_SCALE);

                root.addChild(frontRightLeg).addChild(frontLeftLeg)
                                .addChild(backRightLeg).addChild(backLeftLeg)
                                .addChild(body).addChild(head);
        }

        public static SheepFurModel create() {
                return new SheepFurModel();
        }
}
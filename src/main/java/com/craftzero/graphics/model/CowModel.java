package com.craftzero.graphics.model;

public class CowModel extends QuadrupedModel {

        public ModelPart hornL;
        public ModelPart hornR;
        public ModelPart udder;

        public CowModel() {
                super();

                // Legs: 4x12x4
                float legOffsetX = 4.0f;
                float legOffsetZFront = -5.0f;
                float legOffsetZBack = 9.0f;

                frontRightLeg = new ModelPart(4, 12, 4).setTextureOffset(0, 16)
                                .setOffset(-legOffsetX, 6, legOffsetZFront).setPivot(0, 12, 0);
                frontLeftLeg = new ModelPart(4, 12, 4).setTextureOffset(0, 16).setOffset(legOffsetX, 6, legOffsetZFront)
                                .setPivot(0, 12, 0);
                backRightLeg = new ModelPart(4, 12, 4).setTextureOffset(0, 16).setOffset(-legOffsetX, 6, legOffsetZBack)
                                .setPivot(0, 12, 0);
                backLeftLeg = new ModelPart(4, 12, 4).setTextureOffset(0, 16).setOffset(legOffsetX, 6, legOffsetZBack)
                                .setPivot(0, 12, 0);

                // --- BODY FIX ---
                // 1. Dimensions: 12 wide, 17.9f high, 10 deep.
                // CRITICAL: Using 17.9f instead of 18.0f fixes the "Hole" by keeping UVs inside
                // the image.
                body = new ModelPart(12, 17.9f, 10)
                                .setTextureOffset(18, 4)
                                .setOffset(0, 17.0f, 2.0f)
                                .setPivot(0, 0, 0);

                // 2. Rotation: Matches Pig (90 X, 180 Z).
                body.setRotation((float) Math.PI / 2.0f, 0, (float) Math.PI);

                // --- UDDER FIX ---
                // With Z-180 Rotation, the coordinate system is flipped.
                // We calculate the offset to place Udder at World Bottom (-Y) and Back (+Z).
                // In the rotated space, this corresponds to Local Back (Y=-6) and Down (Z=5).
                udder = new ModelPart(4, 6, 1)
                                .setTextureOffset(52, 0)
                                .setOffset(0, -6.0f, 5.0f)
                                .setPivot(0, 0, 0);

                // Head
                head = new ModelPart(8, 8, 6).setTextureOffset(0, 0).setOffset(0, 20.0f, -8.0f).setPivot(0, 0, 3);

                // Horns
                hornL = new ModelPart(1, 2, 1).setTextureOffset(22, 0).setOffset(-4.5f, 3.0f, -1.0f).setPivot(0, 0, 0);
                hornR = new ModelPart(1, 2, 1).setTextureOffset(22, 0).setOffset(4.5f, 3.0f, -1.0f).setPivot(0, 0, 0);

                head.addChild(hornL).addChild(hornR);
                body.addChild(udder);

                root.addChild(frontRightLeg).addChild(frontLeftLeg)
                                .addChild(backRightLeg).addChild(backLeftLeg)
                                .addChild(body).addChild(head);
        }
}

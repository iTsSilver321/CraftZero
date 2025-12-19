package com.craftzero.graphics.model;

/**
 * Sheep model.
 * Body is 16-long, 8-high, rotated 90 degrees.
 * No Fur (temporarily).
 */
public class SheepModel extends QuadrupedModel {

    public SheepModel() {
        super();

        // Sheep Dimensions: Body 10x16x8 (W, L, H), Legs 4x12
        float legOffsetX = 3.0f;
        float legOffsetZFront = -4.0f;
        float legOffsetZBack = 5.0f;

        frontRightLeg = new ModelPart(4, 12, 4).setTextureOffset(0, 16).setOffset(-legOffsetX, 6, legOffsetZFront)
                .setPivot(0, 12, 0);
        frontLeftLeg = new ModelPart(4, 12, 4).setTextureOffset(0, 16).setOffset(legOffsetX, 6, legOffsetZFront)
                .setPivot(0, 12, 0);
        backRightLeg = new ModelPart(4, 12, 4).setTextureOffset(0, 16).setOffset(-legOffsetX, 6, legOffsetZBack)
                .setPivot(0, 12, 0);
        backLeftLeg = new ModelPart(4, 12, 4).setTextureOffset(0, 16).setOffset(legOffsetX, 6, legOffsetZBack)
                .setPivot(0, 12, 0);

        // 2. Body: Box(10, 16, 8), UV (28, 8), Rotated 90X
        // Body Height is 8 (Y: 12 to 20). Body Length is 16 (Z: -7 to 9).
        body = new ModelPart(10, 16, 8)
                .setTextureOffset(28, 8)
                .setOffset(0, 16.0f, 1.0f)
                .setPivot(0, 0, 0);
        body.setRotation((float) Math.PI / 2.0f, 0, 0);

        // 3. Head: 6x6x8, UV (0, 0)
        // Natural height (Y=18) and deep overlap (Z=-6) to prevent gaps.
        head = new ModelPart(6, 6, 8)
                .setTextureOffset(0, 0)
                .setOffset(0, 18.0f, -6.0f)
                .setPivot(0, 0, 4);

        root.addChild(frontRightLeg).addChild(frontLeftLeg)
                .addChild(backRightLeg).addChild(backLeftLeg)
                .addChild(body).addChild(head);
    }

    public static SheepModel create() {
        return new SheepModel();
    }
}

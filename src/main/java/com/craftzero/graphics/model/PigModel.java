package com.craftzero.graphics.model;

/**
 * Pig model with snout.
 * Body is 16-long, 8-high, rotated 90 degrees for correct UV mapping.
 */
public class PigModel extends QuadrupedModel {

    public final ModelPart snout;

    public PigModel() {
        super();

        // Pig: Body 10x8x16 (world), Legs 4x6, Head 8x8x8
        float legOffsetX = 3.0f;
        float legOffsetZ = 5.0f;

        frontRightLeg = new ModelPart(4, 6, 4).setTextureOffset(0, 16).setOffset(-legOffsetX, 3, -legOffsetZ)
                .setPivot(0, 6, 0);
        frontLeftLeg = new ModelPart(4, 6, 4).setTextureOffset(0, 16).setOffset(legOffsetX, 3, -legOffsetZ).setPivot(0,
                6, 0);
        backRightLeg = new ModelPart(4, 6, 4).setTextureOffset(0, 16).setOffset(-legOffsetX, 3, legOffsetZ).setPivot(0,
                6, 0);
        backLeftLeg = new ModelPart(4, 6, 4).setTextureOffset(0, 16).setOffset(legOffsetX, 3, legOffsetZ).setPivot(0, 6,
                0);

        // 2. Body: Box(10, 16, 8), UV (28, 8), Rotated 90X
        // Width 10, Height 16 (Length), Depth 8 (Height)
        // 2. Body: Box(10, 16, 8), UV (28, 8), Rotated 90X
        // Width 10, Height 16 (Length), Depth 8 (Height)
        body = new ModelPart(10, 16, 8)
                .setTextureOffset(28, 8)
                .setOffset(0, 10.0f, 0.0f)
                .setPivot(0, 0, 0);
        // Rotate 90 X (to lay flat) and 180 Z (to flip top/bottom texture)
        body.setRotation((float) Math.PI / 2.0f, 0, (float) Math.PI);

        // 3. Head: 8x8x8, UV (0, 0)
        head = new ModelPart(8, 8, 8)
                .setTextureOffset(0, 0)
                .setOffset(0, 12.0f, -12.0f) // Moved forward to Z=-12
                .setPivot(0, 0, 4);

        // 4. Snout: 4x3x1, UV (16, 16)
        snout = new ModelPart(4, 3, 1)
                .setTextureOffset(16, 16)
                .setOffset(0, -2, -4.5f)
                .setPivot(0, 0, 0);

        head.addChild(snout);

        root.addChild(frontRightLeg).addChild(frontLeftLeg)
                .addChild(backRightLeg).addChild(backLeftLeg)
                .addChild(body).addChild(head);
    }

    public static PigModel create() {
        return new PigModel();
    }
}

package com.craftzero.graphics.model;

/**
 * Cow model with horns and udder.
 * Fixed for 90X rotation: Local Y -> World -Z (Front), Local Z -> World +Y
 * (Top).
 */
public class CowModel extends QuadrupedModel {

    public ModelPart hornL;
    public ModelPart hornR;
    public ModelPart udder;

    public CowModel() {
        super();

        // Cow Dimensions: Body 12x18x10 (W, L, H), Legs 4x12
        float legOffsetX = 4.0f;
        float legOffsetZFront = -5.0f;
        float legOffsetZBack = 9.0f;

        frontRightLeg = new ModelPart(4, 12, 4).setTextureOffset(0, 16).setOffset(-legOffsetX, 6, legOffsetZFront)
                .setPivot(0, 12, 0);
        frontLeftLeg = new ModelPart(4, 12, 4).setTextureOffset(0, 16).setOffset(legOffsetX, 6, legOffsetZFront)
                .setPivot(0, 12, 0);
        backRightLeg = new ModelPart(4, 12, 4).setTextureOffset(0, 16).setOffset(-legOffsetX, 6, legOffsetZBack)
                .setPivot(0, 12, 0);
        backLeftLeg = new ModelPart(4, 12, 4).setTextureOffset(0, 16).setOffset(legOffsetX, 6, legOffsetZBack)
                .setPivot(0, 12, 0);

        // 2. Body: Box(12, 18, 10), UV (18, 4), Rotated 90X
        // Box dims: Width 12, Height 18 (becomes Length), Depth 10 (becomes Height)
        body = new ModelPart(12, 18, 10).setTextureOffset(18, 4).setOffset(0, 17.0f, 2.0f).setPivot(0, 0, 0);
        body.setRotation((float) Math.PI / 2.0f, 0, 0);

        // 3. Head: 8x8x6, UV (0, 0)
        head = new ModelPart(8, 8, 6).setTextureOffset(0, 0).setOffset(0, 18.0f, -10.0f).setPivot(0, 0, 3);

        // 4. Horns: 1x2x1, UV (22, 0)
        hornL = new ModelPart(1, 2, 1).setTextureOffset(22, 0).setOffset(-4.5f, 3.0f, -1.0f).setPivot(0, 0, 0);
        hornR = new ModelPart(1, 2, 1).setTextureOffset(22, 0).setOffset(4.5f, 3.0f, -1.0f).setPivot(0, 0, 0);

        // 5. Udder: 4x6x1, UV (52, 0)
        // Body rotated 90X: Local +Z is World -Y (Bottom).
        // Offset (0, -5, 5.5): Back 5, Down 5.5.
        udder = new ModelPart(4, 6, 1).setTextureOffset(52, 0).setOffset(0, -5.0f, 5.5f).setPivot(0, 0, 0);

        head.addChild(hornL).addChild(hornR);
        body.addChild(udder);

        root.addChild(frontRightLeg).addChild(frontLeftLeg).addChild(backRightLeg).addChild(backLeftLeg).addChild(body)
                .addChild(head);
    }

    public static CowModel create() {
        return new CowModel();
    }
}

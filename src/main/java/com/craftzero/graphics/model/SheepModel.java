package com.craftzero.graphics.model;

public class SheepModel extends QuadrupedModel {

        public SheepModel() {
                super();

                // --- LEGS (OFFSET 2.8) ---
                // Increased by 0.4 from previous version.
                // The legs now sit significantly wider than the skinny body.
                float legOffsetX = 2.8f;

                float legOffsetZFront = -4.0f;
                float legOffsetZBack = 5.0f;

                frontRightLeg = new ModelPart(4, 12, 4).setTextureOffset(0, 16)
                                .setOffset(-legOffsetX, 6, legOffsetZFront).setPivot(0, 12, 0);
                frontLeftLeg = new ModelPart(4, 12, 4).setTextureOffset(0, 16).setOffset(legOffsetX, 6, legOffsetZFront)
                                .setPivot(0, 12, 0);
                backRightLeg = new ModelPart(4, 12, 4).setTextureOffset(0, 16).setOffset(-legOffsetX, 6, legOffsetZBack)
                                .setPivot(0, 12, 0);
                backLeftLeg = new ModelPart(4, 12, 4).setTextureOffset(0, 16).setOffset(legOffsetX, 6, legOffsetZBack)
                                .setPivot(0, 12, 0);

                // --- BODY (SAFE SIZE) ---
                // Width: 7.9f
                // Length: 14.4f
                // Depth: 6.2f
                // Position: 15.1f
                body = new ModelPart(7.9f, 14.4f, 6.2f)
                                .setTextureOffset(28, 8)
                                .setOffset(0, 15.1f, 1.0f)
                                .setPivot(0, 0, 0);

                body.setRotation((float) Math.PI / 2.0f, 0, (float) Math.PI);

                // --- HEAD ---
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
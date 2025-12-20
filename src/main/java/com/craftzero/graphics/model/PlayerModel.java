package com.craftzero.graphics.model;

/**
 * Player model (Steve) with Y+ UP coordinates (OpenGL standard).
 * 
 * Coordinate system:
 * - Y=0 is at feet level
 * - Y+ goes UP toward head
 * - Model is 24 units tall (1.5 blocks at 1/16 scale)
 * 
 * Pivot points are placed at joint locations.
 * Box coordinates are relative to pivot.
 */
public class PlayerModel {

        // Body parts
        public final ModelPart head;
        public final ModelPart body;
        public final ModelPart rightArm;
        public final ModelPart leftArm;
        public final ModelPart rightLeg;
        public final ModelPart leftLeg;

        // Root for transforms
        public final ModelPart root;

        public PlayerModel() {
                // Create root at feet level
                root = new ModelPart();

                // Measurements (in model units, 16 units = 1 block):
                // Total height: 32 units (2 blocks)
                // - Legs: 12 units (hip at Y=12)
                // - Body: 12 units (shoulder at Y=24)
                // - Head: 8 units (top at Y=32)

                // Head: 8x8x8
                // Pivot at neck (Y=24), head extends up
                head = new ModelPart()
                                .setTextureOffset(0, 0)
                                .setPivot(0, 24, 0)
                                .addBox(-4, 0, -4, 8, 8, 8);

                // Body: 8x12x4
                // Pivot at waist (Y=12), body extends up to Y=24
                body = new ModelPart()
                                .setTextureOffset(16, 16)
                                .setPivot(0, 12, 0)
                                .addBox(-4, 0, -2, 8, 12, 4);

                // Right Arm: 4x12x4
                // Pivot at right shoulder (Y=22, X=-6)
                rightArm = new ModelPart()
                                .setTextureOffset(40, 16)
                                .setPivot(-6, 22, 0)
                                .addBox(-2, -10, -2, 4, 12, 4);

                // Left Arm: 4x12x4
                // Pivot at left shoulder (Y=22, X=6)
                leftArm = new ModelPart()
                                .setTextureOffset(40, 16)
                                .setPivot(6, 22, 0)
                                .addBox(-2, -10, -2, 4, 12, 4);

                // Right Leg: 4x12x4
                // Pivot at right hip (Y=12, X=-2)
                rightLeg = new ModelPart()
                                .setTextureOffset(0, 16)
                                .setPivot(-2, 12, 0)
                                .addBox(-2, -12, -2, 4, 12, 4);

                // Left Leg: 4x12x4
                // Pivot at left hip (Y=12, X=2)
                leftLeg = new ModelPart()
                                .setTextureOffset(0, 16)
                                .setPivot(2, 12, 0)
                                .addBox(-2, -12, -2, 4, 12, 4);

                // Build hierarchy - all parts are children of root
                root.addChild(head);
                root.addChild(body);
                root.addChild(rightArm);
                root.addChild(leftArm);
                root.addChild(rightLeg);
                root.addChild(leftLeg);
        }

        /**
         * Build meshes for all parts.
         */
        public void buildMeshes() {
                root.buildMesh();
        }

        /**
         * Animate the model based on movement.
         * 
         * @param limbSwing       How far the entity has walked (for phase)
         * @param limbSwingAmount How fast the entity is moving (0-1 for amplitude)
         * @param ageInTicks      Entity age for idle animations
         * @param headYaw         Head yaw rotation (relative to body)
         * @param headPitch       Head pitch rotation
         */
        public void animate(float limbSwing, float limbSwingAmount, float ageInTicks,
                        float headYaw, float headPitch, float swingProgress, boolean isSneaking) {

                // Reset defaults first
                head.setPivot(0, 24, 0);
                body.setPivot(0, 12, 0);
                body.setRotation(0, 0, 0);
                rightArm.setPivot(-6, 22, 0);
                leftArm.setPivot(6, 22, 0);
                rightLeg.setPivot(-2, 12, 0);
                leftLeg.setPivot(2, 12, 0);

                // Sneaking Logic - Minecraft style: legs/hips shift back to balance torso lean
                if (isSneaking) {
                        // Rotate body forward (top leans forward)
                        body.setRotation(-0.4f, 0, 0);
                        // Hips move back (Z=4) and lower
                        body.setPivot(0, 10, 4);

                        // Head moves lower and forward relative to hips (at Z=0 total)
                        head.setPivot(0, 20, 0);

                        // Arms shift down and follow the torso's forward tilt
                        rightArm.setPivot(-6, 19, 0);
                        leftArm.setPivot(6, 19, 0);

                        // Legs move back to match hips
                        rightLeg.setPivot(-2, 12, 4);
                        leftLeg.setPivot(2, 12, 4);
                }

                // Head follows look direction
                head.setRotation(
                                (float) Math.toRadians(-headPitch),
                                (float) Math.toRadians(-headYaw),
                                0);

                // Leg swing animation - Faster multiplier for aggressive movement
                float legSwing = (float) Math.cos(limbSwing * 1.5f) * 1.4f * limbSwingAmount;
                rightLeg.setRotation(-legSwing, 0, 0);
                leftLeg.setRotation(legSwing, 0, 0);

                // Arm swing (opposite of legs)
                float armSwing = (float) Math.cos(limbSwing * 1.5f) * 1.0f * limbSwingAmount;

                // VISUAL RIGHT ARM (Named 'leftArm' in code, Pivot +6)
                // If swinging, we override the walking animation
                float visualRightArmRotX = swingProgress > 0 ? 0 : armSwing; // Override walking arm swing when hitting
                float visualRightArmRotY = 0;
                float visualRightArmRotZ = 0;

                // VISUAL LEFT ARM (Named 'rightArm' in code, Pivot -6)
                float visualLeftArmRotX = -armSwing;

                // Sync arms to torso tilt during sneak
                if (isSneaking) {
                        visualRightArmRotX -= 0.4f;
                        visualLeftArmRotX -= 0.4f;
                }

                // Attack Swing Logic (Applied to Visual Right Arm)
                if (swingProgress > 0) {
                        float swing = swingProgress;
                        float sinSqrtSwing = (float) Math.sin(Math.sqrt(swing) * Math.PI);

                        // Rotate body slightly
                        float currentBodyX = isSneaking ? -0.4f : 0.0f;
                        body.setRotation(currentBodyX, sinSqrtSwing * 0.2f, 0);

                        // Rotate arm (Authentic Minecraft Beta 1.7.3 hitting) - Flipped signs for
                        // forward swing
                        visualRightArmRotX = (float) -(Math.sin(Math.sqrt(swing) * Math.PI * 2.0D) * 0.2D);
                        visualRightArmRotX += (float) Math.toRadians(60.0f) * sinSqrtSwing; // High pitch arc forward
                        visualRightArmRotY = -sinSqrtSwing * 0.5f; // Side arc inward
                        visualRightArmRotZ = -sinSqrtSwing * 0.15f;
                }

                // Apply
                leftArm.setRotation(visualRightArmRotX, visualRightArmRotY, visualRightArmRotZ);
                rightArm.setRotation(visualLeftArmRotX, 0, 0);
        }

        /**
         * Cleanup resources.
         */
        public void cleanup() {
                root.cleanup();
        }
}

package com.craftzero.graphics.model;

/**
 * Humanoid mob model (Zombie, Skeleton).
 * Uses the SAME structure as PlayerModel with Y+ UP coordinates.
 * 
 * Coordinate system:
 * - Y=0 is at feet level
 * - Y+ goes UP toward head
 * - Model is 32 units tall (2 blocks at 1/16 scale)
 * 
 * Pivot points are placed at joint locations.
 * Box coordinates are relative to pivot using addBox().
 */
public class HumanoidModel {

        // Body parts
        public final ModelPart head;
        public final ModelPart body;
        public final ModelPart leftArm;
        public final ModelPart rightArm;
        public final ModelPart leftLeg;
        public final ModelPart rightLeg;

        // Root for transforms
        public final ModelPart root;

        public HumanoidModel() {
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
                // Arm hangs from shoulder, box extends DOWN from pivot
                rightArm = new ModelPart()
                                .setTextureOffset(40, 16)
                                .setPivot(-6, 22, 0)
                                .addBox(-3, -10, -2, 4, 12, 4);

                // Left Arm: 4x12x4
                // Pivot at left shoulder (Y=22, X=6)
                leftArm = new ModelPart()
                                .setTextureOffset(40, 16)
                                .setPivot(6, 22, 0)
                                .addBox(-1, -10, -2, 4, 12, 4);

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

                // Build hierarchy
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
         * @param limbSwingAmount How fast the entity is moving (for amplitude)
         * @param ageInTicks      Entity age for idle animations
         * @param headYaw         Head yaw rotation (relative to body)
         * @param headPitch       Head pitch rotation
         */
        public void animate(float limbSwing, float limbSwingAmount, float ageInTicks,
                        float headYaw, float headPitch) {
                // Clamp head rotation to prevent head stuck sideways
                headYaw = Math.max(-60, Math.min(60, headYaw));
                headPitch = Math.max(-45, Math.min(45, headPitch));

                // Head follows look direction
                head.setRotation(
                                (float) Math.toRadians(headPitch),
                                (float) Math.toRadians(headYaw),
                                0);

                // Leg swing animation - SLOWER multiplier (0.6662f matches Minecraft)
                float legSwing = (float) Math.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;
                rightLeg.setRotation(legSwing, 0, 0);
                leftLeg.setRotation(-legSwing, 0, 0);

                // Arm swing - zombie arms extend forward (~80 degrees)
                float armSwing = (float) Math.cos(limbSwing * 0.6662f) * 0.5f * limbSwingAmount;
                float zombieArmAngle = (float) Math.toRadians(-80); // Arms raised forward
                rightArm.setRotation(zombieArmAngle + armSwing, 0, 0);
                leftArm.setRotation(zombieArmAngle - armSwing, 0, 0);

                // Subtle idle breathing animation
                float breathe = (float) Math.sin(ageInTicks * 0.1f) * 0.02f;
                body.setRotation(breathe, 0, 0);
        }

        /**
         * Set attack animation (arm swing).
         * 
         * @param progress 0-1 attack progress
         */
        public void setAttackAnimation(float progress) {
                if (progress > 0) {
                        float swing = (float) Math.sin(progress * Math.PI) * 1.5f;
                        rightArm.setRotation(-swing, 0, 0);
                }
        }

        /**
         * Cleanup resources.
         */
        public void cleanup() {
                root.cleanup();
        }
}

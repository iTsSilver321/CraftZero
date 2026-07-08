package com.craftzero.graphics.model;

/**
 * Classic wolf-shaped quadruped model using the 64x32 wolf texture layout.
 */
public class WolfModel {
    private static final float STAND_HEAD_Y = 13.0f;
    private static final float STAND_HEAD_Z = -7.0f;
    private static final float STAND_BODY_Y = 6.0f;
    private static final float STAND_BODY_Z = 2.0f;
    private static final float STAND_MANE_Y = 7.0f;
    private static final float STAND_MANE_Z = -2.0f;
    private static final float STAND_TAIL_Y = 10.0f;
    private static final float STAND_TAIL_Z = 7.0f;
    private static final float STAND_FRONT_LEG_Y = 0.0f;
    private static final float STAND_FRONT_LEG_Z = -3.0f;
    private static final float STAND_BACK_LEG_Y = 0.0f;
    private static final float STAND_BACK_LEG_Z = 5.0f;
    private static final float SIT_BODY_X_ROT = 0.72f;
    private static final float SIT_MANE_X_ROT = 0.35f;
    private static final float SIT_BACK_LEG_X_ROT = 1.45f;
    private static final float SIT_FRONT_LEG_X_ROT = 0.18f;
    private static final float BEG_HEAD_ROLL = 0.25f;
    private static final float RELEASE_TAIL_HEALTH_BASE = 20.0f;
    private static final float RELEASE_SITTING_TAIL_PITCH = 1.7278761f;
    private static final float RELEASE_ANGRY_TAIL_PITCH = 1.5393804f;
    private static final float RELEASE_WILD_TAIL_PITCH = 0.62831855f;

    public final ModelPart root;
    public final ModelPart head;
    public final ModelPart snout;
    public final ModelPart leftEar;
    public final ModelPart rightEar;
    public final ModelPart body;
    public final ModelPart mane;
    public final ModelPart tail;
    public final ModelPart frontLeftLeg;
    public final ModelPart frontRightLeg;
    public final ModelPart backLeftLeg;
    public final ModelPart backRightLeg;

    public WolfModel() {
        root = new ModelPart();

        head = new ModelPart()
                .setTextureOffset(0, 0)
                .setPivot(0, STAND_HEAD_Y, STAND_HEAD_Z)
                .addBox(-3, -3, -2, 6, 6, 4);

        snout = new ModelPart()
                .setTextureOffset(0, 10)
                .setPivot(0, STAND_HEAD_Y, STAND_HEAD_Z)
                .addBox(-2, -1, -5, 4, 3, 3);

        leftEar = new ModelPart()
                .setTextureOffset(16, 14)
                .setPivot(0, STAND_HEAD_Y, STAND_HEAD_Z)
                .addBox(1, -5, -1, 2, 2, 1);

        rightEar = new ModelPart()
                .setTextureOffset(16, 14)
                .setPivot(0, STAND_HEAD_Y, STAND_HEAD_Z)
                .addBox(-3, -5, -1, 2, 2, 1);

        body = new ModelPart()
                .setTextureOffset(18, 14)
                .setPivot(0, STAND_BODY_Y, STAND_BODY_Z)
                .addBox(-3, 0, -5, 6, 6, 10);

        mane = new ModelPart()
                .setTextureOffset(21, 0)
                .setPivot(0, STAND_MANE_Y, STAND_MANE_Z)
                .addBox(-4, 0, -3, 8, 7, 6);

        tail = new ModelPart()
                .setTextureOffset(9, 18)
                .setPivot(0, STAND_TAIL_Y, STAND_TAIL_Z)
                .addBox(-1, 0, -1, 2, 8, 2);

        frontRightLeg = leg(-2, 0, -3);
        frontLeftLeg = leg(2, 0, -3);
        backRightLeg = leg(-2, 0, 5);
        backLeftLeg = leg(2, 0, 5);

        root.addChild(head);
        root.addChild(snout);
        root.addChild(leftEar);
        root.addChild(rightEar);
        root.addChild(body);
        root.addChild(mane);
        root.addChild(tail);
        root.addChild(frontRightLeg);
        root.addChild(frontLeftLeg);
        root.addChild(backRightLeg);
        root.addChild(backLeftLeg);
    }

    private ModelPart leg(float x, float y, float z) {
        return new ModelPart()
                .setTextureOffset(0, 18)
                .setPivot(x, y, z)
                .addBox(-1, 0, -1, 2, 8, 2);
    }

    public void buildMeshes() {
        root.buildMesh();
    }

    public void animate(float limbSwing, float limbSwingAmount, float ageInTicks,
            float headYaw, float headPitch) {
        animate(limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch,
                false, false, false);
    }

    public void animate(float limbSwing, float limbSwingAmount, float ageInTicks,
            float headYaw, float headPitch, boolean sitting, boolean begging, boolean angry) {
        animate(limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch,
                sitting, begging, angry, 0.0f, 0.0f, 0.0f);
    }

    public void animate(float limbSwing, float limbSwingAmount, float ageInTicks,
            float headYaw, float headPitch, boolean sitting, boolean begging, boolean angry,
            float headShakeRoll, float bodyShakeRoll, float tailShakeRoll) {
        animate(limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch,
                sitting, begging, angry, false, RELEASE_TAIL_HEALTH_BASE,
                headShakeRoll, bodyShakeRoll, tailShakeRoll);
    }

    public void animate(float limbSwing, float limbSwingAmount, float ageInTicks,
            float headYaw, float headPitch, boolean sitting, boolean begging, boolean angry,
            boolean tamed, float health, float headShakeRoll, float bodyShakeRoll, float tailShakeRoll) {
        headYaw = clamp(headYaw, -60, 60);
        headPitch = clamp(headPitch, -35, 35);
        float headPitchRad = (float) Math.toRadians(headPitch);
        float headYawRad = (float) Math.toRadians(headYaw);
        float headRoll = begging ? BEG_HEAD_ROLL + (float) Math.sin(ageInTicks * 0.12f) * 0.04f : 0.0f;
        headRoll += headShakeRoll;

        head.setRotation(headPitchRad, headYawRad, headRoll);
        snout.setRotation(headPitchRad, headYawRad, headRoll);
        leftEar.setRotation(headPitchRad, headYawRad, headRoll);
        rightEar.setRotation(headPitchRad, headYawRad, headRoll);

        if (sitting) {
            applySittingPose();
        } else {
            applyStandingPose();
            float swing = (float) Math.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;
            frontRightLeg.setRotation(swing, 0, 0);
            backLeftLeg.setRotation(swing, 0, 0);
            frontLeftLeg.setRotation(-swing, 0, 0);
            backRightLeg.setRotation(-swing, 0, 0);
        }
        body.setRotation(body.getRotationX(), body.getRotationY(), bodyShakeRoll);
        mane.setRotation(mane.getRotationX(), mane.getRotationY(), bodyShakeRoll);

        float tailWag = angry ? 0.0f : (float) Math.sin(ageInTicks * 0.18f) * 0.12f;
        float tailPitch = releaseTailPitch(sitting, angry, tamed, health);
        tail.setRotation(tailPitch, tailWag, tailShakeRoll);
    }

    public static float releaseTailPitch(boolean sitting, boolean angry, boolean tamed, float health) {
        if (sitting) {
            return RELEASE_SITTING_TAIL_PITCH;
        }
        if (angry) {
            return RELEASE_ANGRY_TAIL_PITCH;
        }
        if (tamed) {
            float clampedHealth = clampStatic(health, 0.0f, RELEASE_TAIL_HEALTH_BASE);
            return (0.55f - (RELEASE_TAIL_HEALTH_BASE - clampedHealth) * 0.02f) * (float) Math.PI;
        }
        return RELEASE_WILD_TAIL_PITCH;
    }

    private void applyStandingPose() {
        setHeadPivot(STAND_HEAD_Y, STAND_HEAD_Z);
        body.setPivot(0, STAND_BODY_Y, STAND_BODY_Z);
        mane.setPivot(0, STAND_MANE_Y, STAND_MANE_Z);
        tail.setPivot(0, STAND_TAIL_Y, STAND_TAIL_Z);
        frontRightLeg.setPivot(-2, STAND_FRONT_LEG_Y, STAND_FRONT_LEG_Z);
        frontLeftLeg.setPivot(2, STAND_FRONT_LEG_Y, STAND_FRONT_LEG_Z);
        backRightLeg.setPivot(-2, STAND_BACK_LEG_Y, STAND_BACK_LEG_Z);
        backLeftLeg.setPivot(2, STAND_BACK_LEG_Y, STAND_BACK_LEG_Z);
        body.setRotation(0, 0, 0);
        mane.setRotation(0, 0, 0);
    }

    private void applySittingPose() {
        setHeadPivot(STAND_HEAD_Y + 0.5f, STAND_HEAD_Z - 0.5f);
        body.setPivot(0, STAND_BODY_Y - 1.5f, STAND_BODY_Z + 0.5f);
        mane.setPivot(0, STAND_MANE_Y - 0.5f, STAND_MANE_Z - 0.5f);
        tail.setPivot(0, STAND_TAIL_Y - 5.0f, STAND_TAIL_Z);
        frontRightLeg.setPivot(-2, STAND_FRONT_LEG_Y, STAND_FRONT_LEG_Z);
        frontLeftLeg.setPivot(2, STAND_FRONT_LEG_Y, STAND_FRONT_LEG_Z);
        backRightLeg.setPivot(-2, STAND_BACK_LEG_Y + 1.2f, STAND_BACK_LEG_Z - 0.8f);
        backLeftLeg.setPivot(2, STAND_BACK_LEG_Y + 1.2f, STAND_BACK_LEG_Z - 0.8f);
        body.setRotation(SIT_BODY_X_ROT, 0, 0);
        mane.setRotation(SIT_MANE_X_ROT, 0, 0);
        frontRightLeg.setRotation(SIT_FRONT_LEG_X_ROT, 0, 0);
        frontLeftLeg.setRotation(SIT_FRONT_LEG_X_ROT, 0, 0);
        backRightLeg.setRotation(SIT_BACK_LEG_X_ROT, 0, 0);
        backLeftLeg.setRotation(SIT_BACK_LEG_X_ROT, 0, 0);
    }

    private void setHeadPivot(float y, float z) {
        head.setPivot(0, y, z);
        snout.setPivot(0, y, z);
        leftEar.setPivot(0, y, z);
        rightEar.setPivot(0, y, z);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampStatic(float value, float min, float max) {
        if (!Float.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    public void cleanup() {
        root.cleanup();
    }
}

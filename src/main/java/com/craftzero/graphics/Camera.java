package com.craftzero.graphics;

import lombok.Getter;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * First-person camera with mouse look and movement.
 * Handles view and projection matrix calculations.
 */
public class Camera {

    private static final float DEFAULT_FOV = 70.0f;
    private static final float DEFAULT_NEAR = 0.1f;
    private static final float DEFAULT_FAR = 1000.0f;
    private static final float PITCH_LIMIT = 89.9f;

    @Getter
    private Vector3f position;
    @Getter
    private float pitch; // Up/down rotation (X-axis)
    @Getter
    private float yaw; // Left/right rotation (Y-axis)

    @Getter
    private float fov;
    @Getter
    private float nearPlane;
    @Getter
    private float farPlane;
    private float aspectRatio;

    private Matrix4f viewMatrix;
    @Getter
    private Matrix4f projectionMatrix;

    // Cached direction vectors
    @Getter
    private Vector3f forward;
    @Getter
    private Vector3f right;
    @Getter
    private Vector3f up;

    public Camera() {
        this(new Vector3f(0, 80, 0));
    }

    public Camera(Vector3f position) {
        this.position = isFiniteVector(position) ? new Vector3f(position) : new Vector3f(0.0f, 80.0f, 0.0f);
        this.pitch = 0;
        this.yaw = 0;

        this.fov = DEFAULT_FOV;
        this.nearPlane = DEFAULT_NEAR;
        this.farPlane = DEFAULT_FAR;
        this.aspectRatio = 16.0f / 9.0f;

        this.viewMatrix = new Matrix4f();
        this.projectionMatrix = new Matrix4f();

        this.forward = new Vector3f();
        this.right = new Vector3f();
        this.up = new Vector3f(0, 1, 0);

        updateProjectionMatrix();
        updateViewMatrix();
    }

    /**
     * Rotate camera based on mouse movement.
     */
    public void rotate(float deltaYaw, float deltaPitch) {
        yaw = normalizeYaw(Float.isFinite(deltaYaw) ? yaw + deltaYaw : yaw);
        pitch = clampPitch(Float.isFinite(deltaPitch) ? pitch + deltaPitch : pitch);

        updateDirectionVectors();
    }

    /**
     * Move camera by offset.
     */
    public void move(Vector3f offset) {
        if (isFiniteVector(offset)) {
            position.add(offset);
        }
    }

    /**
     * Move camera forward/backward.
     */
    public void moveForward(float amount) {
        if (!Float.isFinite(amount)) {
            return;
        }
        position.add(forward.x * amount, 0, forward.z * amount);
    }

    /**
     * Move camera left/right (strafe).
     */
    public void moveRight(float amount) {
        if (!Float.isFinite(amount)) {
            return;
        }
        position.add(right.x * amount, 0, right.z * amount);
    }

    /**
     * Move camera up/down.
     */
    public void moveUp(float amount) {
        if (!Float.isFinite(amount)) {
            return;
        }
        position.y += amount;
    }

    private void updateDirectionVectors() {
        // Calculate forward vector from yaw and pitch
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        forward.x = (float) (Math.sin(yawRad) * Math.cos(pitchRad));
        forward.y = (float) (-Math.sin(pitchRad));
        forward.z = (float) (-Math.cos(yawRad) * Math.cos(pitchRad));
        if (forward.lengthSquared() <= 0.000001f || !isFiniteVector(forward)) {
            forward.set(0.0f, 0.0f, -1.0f);
        } else {
            forward.normalize();
        }

        // Right vector is perpendicular to forward and world up
        forward.cross(new Vector3f(0, 1, 0), right);
        if (right.lengthSquared() <= 0.000001f || !isFiniteVector(right)) {
            right.set(1.0f, 0.0f, 0.0f);
        } else {
            right.normalize();
        }

        // Camera up is perpendicular to right and forward
        right.cross(forward, up);
        if (up.lengthSquared() <= 0.000001f || !isFiniteVector(up)) {
            up.set(0.0f, 1.0f, 0.0f);
        } else {
            up.normalize();
        }
    }

    public void updateViewMatrix() {
        if (!isFiniteVector(position)) {
            position.set(0.0f, 80.0f, 0.0f);
        }
        updateDirectionVectors();

        Vector3f target = new Vector3f();
        position.add(forward, target);

        viewMatrix.identity();
        viewMatrix.lookAt(position, target, new Vector3f(0, 1, 0));
    }

    public void updateProjectionMatrix() {
        fov = clampFov(fov);
        nearPlane = clampPositive(nearPlane, DEFAULT_NEAR);
        farPlane = Math.max(nearPlane + 0.001f, clampPositive(farPlane, DEFAULT_FAR));
        aspectRatio = clampPositive(aspectRatio, 16.0f / 9.0f);
        projectionMatrix.identity();
        projectionMatrix.perspective(
                (float) Math.toRadians(fov),
                aspectRatio,
                nearPlane,
                farPlane);
    }

    public void setAspectRatio(float aspectRatio) {
        this.aspectRatio = clampPositive(aspectRatio, this.aspectRatio);
        updateProjectionMatrix();
    }

    public void setAspectRatio(int width, int height) {
        this.aspectRatio = height <= 0 ? this.aspectRatio : clampPositive((float) width / (float) height,
                this.aspectRatio);
        updateProjectionMatrix();
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    // Getters (simple ones generated by Lombok @Getter on fields)

    public void setPosition(Vector3f position) {
        if (isFiniteVector(position)) {
            this.position = new Vector3f(position);
        }
    }

    public void setPosition(float x, float y, float z) {
        if (allFinite(x, y, z)) {
            this.position.set(x, y, z);
        }
    }

    /**
     * Set a look-at target position and calculate yaw/pitch to face it.
     * Used for third-person front view where camera needs to look at player.
     */
    public void setLookTarget(float targetX, float targetY, float targetZ) {
        if (!allFinite(targetX, targetY, targetZ) || !isFiniteVector(position)) {
            return;
        }
        float dx = targetX - position.x;
        float dy = targetY - position.y;
        float dz = targetZ - position.z;
        float horizontalDist = (float) Math.sqrt(dx * dx + dz * dz);

        this.yaw = normalizeYaw((float) Math.toDegrees(Math.atan2(dx, -dz)));
        this.pitch = clampPitch((float) Math.toDegrees(-Math.atan2(dy, horizontalDist)));

        updateDirectionVectors();
    }

    public void setPitch(float pitch) {
        this.pitch = clampPitch(pitch);
        updateDirectionVectors();
    }

    public void setYaw(float yaw) {
        this.yaw = normalizeYaw(yaw);
        updateDirectionVectors();
    }

    private static float normalizeYaw(float yaw) {
        if (!Float.isFinite(yaw)) {
            return 0.0f;
        }
        float normalized = yaw % 360.0f;
        return normalized < 0.0f ? normalized + 360.0f : normalized;
    }

    private static float clampPitch(float pitch) {
        if (!Float.isFinite(pitch)) {
            return 0.0f;
        }
        return Math.max(-PITCH_LIMIT, Math.min(PITCH_LIMIT, pitch));
    }

    public Matrix4f getViewMatrix() {
        updateViewMatrix();
        return viewMatrix;
    }

    public void setFov(float fov) {
        this.fov = clampFov(fov);
        updateProjectionMatrix();
    }

    public void setFarPlane(float farPlane) {
        this.farPlane = Math.max(nearPlane + 0.001f, clampPositive(farPlane, DEFAULT_FAR));
        updateProjectionMatrix();
    }

    private static float clampFov(float value) {
        if (!Float.isFinite(value)) {
            return DEFAULT_FOV;
        }
        return Math.max(30.0f, Math.min(110.0f, value));
    }

    private static float clampPositive(float value, float fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
    }

    private static boolean isFiniteVector(Vector3f value) {
        return value != null
                && Float.isFinite(value.x)
                && Float.isFinite(value.y)
                && Float.isFinite(value.z);
    }

    private static boolean allFinite(float... values) {
        for (float value : values) {
            if (!Float.isFinite(value)) {
                return false;
            }
        }
        return true;
    }
}

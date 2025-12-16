package com.craftzero.graphics;

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
    
    private Vector3f position;
    private float pitch;  // Up/down rotation (X-axis)
    private float yaw;    // Left/right rotation (Y-axis)
    
    private float fov;
    private float nearPlane;
    private float farPlane;
    private float aspectRatio;
    
    private Matrix4f viewMatrix;
    private Matrix4f projectionMatrix;
    
    // Cached direction vectors
    private Vector3f forward;
    private Vector3f right;
    private Vector3f up;
    
    public Camera() {
        this(new Vector3f(0, 80, 0));
    }
    
    public Camera(Vector3f position) {
        this.position = position;
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
        yaw += deltaYaw;
        pitch += deltaPitch;
        
        // Clamp pitch to prevent flipping
        if (pitch > PITCH_LIMIT) {
            pitch = PITCH_LIMIT;
        }
        if (pitch < -PITCH_LIMIT) {
            pitch = -PITCH_LIMIT;
        }
        
        // Keep yaw in range [0, 360)
        while (yaw >= 360.0f) yaw -= 360.0f;
        while (yaw < 0.0f) yaw += 360.0f;
        
        updateDirectionVectors();
    }
    
    /**
     * Move camera by offset.
     */
    public void move(Vector3f offset) {
        position.add(offset);
    }
    
    /**
     * Move camera forward/backward.
     */
    public void moveForward(float amount) {
        position.add(forward.x * amount, 0, forward.z * amount);
    }
    
    /**
     * Move camera left/right (strafe).
     */
    public void moveRight(float amount) {
        position.add(right.x * amount, 0, right.z * amount);
    }
    
    /**
     * Move camera up/down.
     */
    public void moveUp(float amount) {
        position.y += amount;
    }
    
    private void updateDirectionVectors() {
        // Calculate forward vector from yaw and pitch
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        
        forward.x = (float) (Math.sin(yawRad) * Math.cos(pitchRad));
        forward.y = (float) (-Math.sin(pitchRad));
        forward.z = (float) (-Math.cos(yawRad) * Math.cos(pitchRad));
        forward.normalize();
        
        // Right vector is perpendicular to forward and world up
        forward.cross(new Vector3f(0, 1, 0), right);
        right.normalize();
        
        // Camera up is perpendicular to right and forward
        right.cross(forward, up);
        up.normalize();
    }
    
    public void updateViewMatrix() {
        updateDirectionVectors();
        
        Vector3f target = new Vector3f();
        position.add(forward, target);
        
        viewMatrix.identity();
        viewMatrix.lookAt(position, target, new Vector3f(0, 1, 0));
    }
    
    public void updateProjectionMatrix() {
        projectionMatrix.identity();
        projectionMatrix.perspective(
            (float) Math.toRadians(fov),
            aspectRatio,
            nearPlane,
            farPlane
        );
    }
    
    public void setAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
        updateProjectionMatrix();
    }
    
    public void setAspectRatio(int width, int height) {
        this.aspectRatio = (float) width / (float) height;
        updateProjectionMatrix();
    }
    
    // Getters
    public Vector3f getPosition() {
        return position;
    }
    
    public void setPosition(Vector3f position) {
        this.position = position;
    }
    
    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
    }
    
    public float getPitch() {
        return pitch;
    }
    
    public void setPitch(float pitch) {
        this.pitch = Math.max(-PITCH_LIMIT, Math.min(PITCH_LIMIT, pitch));
    }
    
    public float getYaw() {
        return yaw;
    }
    
    public void setYaw(float yaw) {
        this.yaw = yaw;
    }
    
    public Matrix4f getViewMatrix() {
        updateViewMatrix();
        return viewMatrix;
    }
    
    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }
    
    public Vector3f getForward() {
        return forward;
    }
    
    public Vector3f getRight() {
        return right;
    }
    
    public Vector3f getUp() {
        return up;
    }
    
    public float getFov() {
        return fov;
    }
    
    public void setFov(float fov) {
        this.fov = fov;
        updateProjectionMatrix();
    }
    
    public float getNearPlane() {
        return nearPlane;
    }
    
    public float getFarPlane() {
        return farPlane;
    }
}

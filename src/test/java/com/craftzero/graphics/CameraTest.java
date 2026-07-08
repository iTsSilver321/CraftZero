package com.craftzero.graphics;

import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CameraTest {
    @Test
    @DisplayName("Direct yaw and pitch setters should refresh cached direction vectors")
    void directAngleSettersRefreshDirectionVectors() {
        Camera camera = new Camera(new Vector3f(0.0f, 64.0f, 0.0f));

        camera.setYaw(90.0f);
        assertEquals(1.0f, camera.getForward().x, 0.0001f);
        assertEquals(0.0f, camera.getForward().y, 0.0001f);
        assertEquals(0.0f, camera.getForward().z, 0.0001f);

        camera.setYaw(0.0f);
        camera.setPitch(30.0f);
        assertEquals(0.0f, camera.getForward().x, 0.0001f);
        assertEquals(-0.5f, camera.getForward().y, 0.0001f);
        assertEquals(-(float) Math.cos(Math.toRadians(30.0)), camera.getForward().z, 0.0001f);
        assertEquals(1.0f, camera.getUp().length(), 0.0001f);
    }
}

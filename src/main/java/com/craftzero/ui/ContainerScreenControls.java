package com.craftzero.ui;

import com.craftzero.engine.Input;

import java.util.function.BooleanSupplier;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_E;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;

final class ContainerScreenControls {
    private ContainerScreenControls() {
    }

    static BooleanSupplier closeRequester(BooleanSupplier inventoryCloseRequested) {
        return inventoryCloseRequested == null ? ContainerScreenControls::defaultInventoryCloseRequested
                : inventoryCloseRequested;
    }

    static BooleanSupplier dropRequester(BooleanSupplier dropRequested) {
        return dropRequested == null ? ContainerScreenControls::defaultDropRequested : dropRequested;
    }

    static boolean shouldClose(BooleanSupplier inventoryCloseRequested) {
        return Input.isKeyPressed(GLFW_KEY_ESCAPE) || closeRequester(inventoryCloseRequested).getAsBoolean();
    }

    private static boolean defaultInventoryCloseRequested() {
        return Input.isKeyPressed(GLFW_KEY_E);
    }

    private static boolean defaultDropRequested() {
        return Input.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_Q);
    }
}

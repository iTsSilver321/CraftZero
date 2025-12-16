package com.craftzero.engine;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Static input manager for keyboard and mouse state.
 * Provides methods to check pressed, released, and held states.
 */
public class Input {

    private static final int MAX_KEYS = 350;
    private static final int MAX_BUTTONS = 8;

    private static boolean[] keys = new boolean[MAX_KEYS];
    private static boolean[] keysPressed = new boolean[MAX_KEYS];
    private static boolean[] keysReleased = new boolean[MAX_KEYS];

    private static boolean[] buttons = new boolean[MAX_BUTTONS];
    private static boolean[] buttonsPressed = new boolean[MAX_BUTTONS];
    private static boolean[] buttonsReleased = new boolean[MAX_BUTTONS];

    private static double mouseX, mouseY;
    private static double lastMouseX, lastMouseY;
    private static double deltaX, deltaY;
    private static double scrollX, scrollY;

    private static boolean cursorLocked = false;
    private static boolean firstMouse = true;

    private static long windowHandle;

    public static void init(Window window) {
        windowHandle = window.getHandle();

        // Keyboard callback
        glfwSetKeyCallback(windowHandle, (win, key, scancode, action, mods) -> {
            if (key >= 0 && key < MAX_KEYS) {
                if (action == GLFW_PRESS) {
                    keys[key] = true;
                    keysPressed[key] = true;
                } else if (action == GLFW_RELEASE) {
                    keys[key] = false;
                    keysReleased[key] = true;
                }
            }
        });

        // Mouse button callback
        glfwSetMouseButtonCallback(windowHandle, (win, button, action, mods) -> {
            if (button >= 0 && button < MAX_BUTTONS) {
                if (action == GLFW_PRESS) {
                    buttons[button] = true;
                    buttonsPressed[button] = true;
                } else if (action == GLFW_RELEASE) {
                    buttons[button] = false;
                    buttonsReleased[button] = true;
                }
            }
        });

        // Cursor position callback
        glfwSetCursorPosCallback(windowHandle, (win, xpos, ypos) -> {
            if (firstMouse) {
                lastMouseX = xpos;
                lastMouseY = ypos;
                firstMouse = false;
            }

            mouseX = xpos;
            mouseY = ypos;
        });

        // Scroll callback
        glfwSetScrollCallback(windowHandle, (win, xoffset, yoffset) -> {
            scrollX = xoffset;
            scrollY = yoffset;
        });
    }

    public static void update() {
        // Calculate mouse delta
        deltaX = mouseX - lastMouseX;
        deltaY = mouseY - lastMouseY;
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        // Reset single-frame states
        for (int i = 0; i < MAX_KEYS; i++) {
            keysPressed[i] = false;
            keysReleased[i] = false;
        }
        for (int i = 0; i < MAX_BUTTONS; i++) {
            buttonsPressed[i] = false;
            buttonsReleased[i] = false;
        }

        // Reset scroll
        scrollX = 0;
        scrollY = 0;
    }

    // Keyboard methods
    public static boolean isKeyDown(int key) {
        return key >= 0 && key < MAX_KEYS && keys[key];
    }

    public static boolean isKeyPressed(int key) {
        return key >= 0 && key < MAX_KEYS && keysPressed[key];
    }

    public static boolean isKeyReleased(int key) {
        return key >= 0 && key < MAX_KEYS && keysReleased[key];
    }

    // Mouse button methods
    public static boolean isButtonDown(int button) {
        return button >= 0 && button < MAX_BUTTONS && buttons[button];
    }

    public static boolean isButtonPressed(int button) {
        return button >= 0 && button < MAX_BUTTONS && buttonsPressed[button];
    }

    public static boolean isButtonReleased(int button) {
        return button >= 0 && button < MAX_BUTTONS && buttonsReleased[button];
    }

    // Mouse position methods
    public static double getMouseX() {
        return mouseX;
    }

    public static double getMouseY() {
        return mouseY;
    }

    public static double getDeltaX() {
        return deltaX;
    }

    public static double getDeltaY() {
        return deltaY;
    }

    public static double getScrollX() {
        return scrollX;
    }

    public static double getScrollY() {
        return scrollY;
    }

    public static void setCursorLocked(boolean locked) {
        cursorLocked = locked;
        if (locked) {
            glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            // Check if raw mouse motion is supported
            if (glfwRawMouseMotionSupported()) {
                glfwSetInputMode(windowHandle, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
            }
        } else {
            glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        }
        firstMouse = true;
    }

    public static boolean isCursorLocked() {
        return cursorLocked;
    }

    public static void resetFirstMouse() {
        firstMouse = true;
    }

}

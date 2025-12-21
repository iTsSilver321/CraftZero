package com.craftzero.engine;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Window management class using GLFW.
 * Handles window creation, OpenGL context, and resize callbacks.
 */
public class Window {

    private long handle;
    private int width;
    private int height;
    private String title;
    private boolean resized;
    private boolean vSync;
    private boolean fullscreen;
    private int windowedX, windowedY; // Position before fullscreen
    private int windowedWidth, windowedHeight; // Size before fullscreen

    public Window(String title, int width, int height, boolean vSync) {
        this.title = title;
        this.width = width;
        this.height = height;
        this.vSync = vSync;
        this.resized = false;
    }

    public void init() {
        // Setup error callback to print to System.err
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        // OpenGL 3.3 Core Profile
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        // Anti-aliasing
        glfwWindowHint(GLFW_SAMPLES, 4);

        // Create the window
        handle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (handle == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Setup resize callback
        glfwSetFramebufferSizeCallback(handle, (window, w, h) -> {
            this.width = w;
            this.height = h;
            this.resized = true;
        });

        // Setup key callback
        glfwSetKeyCallback(handle, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true);
            }
        });

        // Center the window
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            glfwGetWindowSize(handle, pWidth, pHeight);

            GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (vidMode != null) {
                glfwSetWindowPos(
                        handle,
                        (vidMode.width() - pWidth.get(0)) / 2,
                        (vidMode.height() - pHeight.get(0)) / 2);
            }
        }

        // Make the OpenGL context current
        glfwMakeContextCurrent(handle);

        // Enable VSync
        if (vSync) {
            glfwSwapInterval(1);
        }

        // Make the window visible
        glfwShowWindow(handle);

        // Initialize OpenGL bindings
        GL.createCapabilities();

        // Enable depth testing
        glEnable(GL_DEPTH_TEST);

        // Enable backface culling
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        // Enable multisampling
        glEnable(GL_MULTISAMPLE);

        // Set clear color to sky blue
        glClearColor(0.529f, 0.808f, 0.922f, 1.0f);

        System.out.println("OpenGL Version: " + glGetString(GL_VERSION));
        System.out.println("GLSL Version: " + glGetString(GL_SHADING_LANGUAGE_VERSION));
    }

    public void update() {
        glfwSwapBuffers(handle);
        glfwPollEvents();
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(handle);
    }

    public void cleanup() {
        glfwFreeCallbacks(handle);
        glfwDestroyWindow(handle);
        glfwTerminate();

        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
        }
    }

    public boolean isResized() {
        return resized;
    }

    public void setResized(boolean resized) {
        this.resized = resized;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public long getHandle() {
        return handle;
    }

    public boolean isVSync() {
        return vSync;
    }

    public void setVSync(boolean vSync) {
        this.vSync = vSync;
        glfwSwapInterval(vSync ? 1 : 0);
    }

    public void setCursorMode(int mode) {
        glfwSetInputMode(handle, GLFW_CURSOR, mode);
    }

    public void grabCursor() {
        glfwSetInputMode(handle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
    }

    public void releaseCursor() {
        glfwSetInputMode(handle, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
    }

    /**
     * Toggle between windowed and fullscreen mode.
     * Saves windowed position/size before going fullscreen.
     */
    public void toggleFullscreen() {
        fullscreen = !fullscreen;

        if (fullscreen) {
            // Save windowed state before going fullscreen
            try (MemoryStack stack = stackPush()) {
                IntBuffer xPos = stack.mallocInt(1);
                IntBuffer yPos = stack.mallocInt(1);
                glfwGetWindowPos(handle, xPos, yPos);
                windowedX = xPos.get(0);
                windowedY = yPos.get(0);
            }
            windowedWidth = width;
            windowedHeight = height;

            // Switch to fullscreen on primary monitor
            long monitor = glfwGetPrimaryMonitor();
            GLFWVidMode mode = glfwGetVideoMode(monitor);
            if (mode != null) {
                glfwSetWindowMonitor(handle, monitor, 0, 0, mode.width(), mode.height(), mode.refreshRate());
            }
        } else {
            // Restore windowed mode
            glfwSetWindowMonitor(handle, NULL, windowedX, windowedY, windowedWidth, windowedHeight, 0);
        }

        // Trigger resize handling to update GUI/camera
        resized = true;
    }

    public boolean isFullscreen() {
        return fullscreen;
    }
}

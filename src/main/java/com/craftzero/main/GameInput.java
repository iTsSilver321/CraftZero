package com.craftzero.main;

import com.craftzero.engine.Input;

/**
 * Runtime helpers for Release 1.0-style keyboard and mouse bindings.
 */
public final class GameInput {
    private static final int MOUSE_BINDING_OFFSET = 100;

    private GameInput() {
    }

    public static boolean isBindingDown(GameSettings settings, GameSettings.KeyBinding binding) {
        int code = keyCode(settings, binding);
        return code < 0 ? Input.isButtonDown(mouseButtonFromKeyCode(code)) : Input.isKeyDown(code);
    }

    public static boolean isBindingPressed(GameSettings settings, GameSettings.KeyBinding binding) {
        int code = keyCode(settings, binding);
        return code < 0 ? Input.isButtonPressed(mouseButtonFromKeyCode(code)) : Input.isKeyPressed(code);
    }

    public static boolean isBindingReleased(GameSettings settings, GameSettings.KeyBinding binding) {
        int code = keyCode(settings, binding);
        return code < 0 ? Input.isButtonReleased(mouseButtonFromKeyCode(code)) : Input.isKeyReleased(code);
    }

    public static int mouseButtonFromKeyCode(int keyCode) {
        return Math.max(0, keyCode + MOUSE_BINDING_OFFSET);
    }

    public static int keyCodeFromMouseButton(int button) {
        return button - MOUSE_BINDING_OFFSET;
    }

    private static int keyCode(GameSettings settings, GameSettings.KeyBinding binding) {
        GameSettings source = settings == null ? GameSettings.defaults() : settings;
        return source.getKeyBinding(binding);
    }
}

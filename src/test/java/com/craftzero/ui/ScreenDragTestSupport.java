package com.craftzero.ui;

import com.craftzero.engine.Input;

import java.lang.reflect.Field;

final class ScreenDragTestSupport {
    private ScreenDragTestSupport() {
    }

    static double[] point(double x, double y) {
        return new double[] { x, y };
    }

    static void drag(int button, Runnable update, double[]... points) throws Exception {
        setMousePosition(points[0][0], points[0][1]);
        setMouseButton(button, true, true, false);
        update.run();
        for (int i = 1; i < points.length; i++) {
            setMousePosition(points[i][0], points[i][1]);
            setMouseButton(button, true, false, false);
            update.run();
        }
        setMousePosition(points[points.length - 1][0], points[points.length - 1][1]);
        setMouseButton(button, false, false, true);
        update.run();
        setMouseButton(button, false, false, false);
    }

    static void clearMouseButtons(int... buttons) throws Exception {
        for (int button : buttons) {
            setMouseButton(button, false, false, false);
        }
    }

    static void pressKey(int key) throws Exception {
        setKey(key, true, true, false);
    }

    static void clearKeys(int... keys) throws Exception {
        for (int key : keys) {
            setKey(key, false, false, false);
        }
    }

    static void setMouseButton(int button, boolean down, boolean pressed, boolean released) throws Exception {
        setInputButtonArray("buttons", button, down);
        setInputButtonArray("buttonsPressed", button, pressed);
        setInputButtonArray("buttonsReleased", button, released);
    }

    static void setKey(int key, boolean down, boolean pressed, boolean released) throws Exception {
        setInputKeyArray("keys", key, down);
        setInputKeyArray("keysPressed", key, pressed);
        setInputKeyArray("keysReleased", key, released);
    }

    static void setMousePosition(double x, double y) throws Exception {
        Field mouseX = Input.class.getDeclaredField("mouseX");
        Field mouseY = Input.class.getDeclaredField("mouseY");
        mouseX.setAccessible(true);
        mouseY.setAccessible(true);
        mouseX.setDouble(null, x);
        mouseY.setDouble(null, y);
    }

    private static void setInputButtonArray(String fieldName, int button, boolean value) throws Exception {
        Field field = Input.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        ((boolean[]) field.get(null))[button] = value;
    }

    private static void setInputKeyArray(String fieldName, int key, boolean value) throws Exception {
        Field field = Input.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        ((boolean[]) field.get(null))[key] = value;
    }
}

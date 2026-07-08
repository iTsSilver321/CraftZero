package com.craftzero.ui;

import com.craftzero.engine.Input;
import com.craftzero.world.tile.SignTileEntity;

import static org.lwjgl.glfw.GLFW.*;

public class SignEditScreen {
    public static final int DONE_BUTTON_WIDTH = 400;
    public static final int DONE_BUTTON_HEIGHT = 40;
    private static final int DONE_BUTTON_BOTTOM_MARGIN = 36;

    private SignTileEntity sign;
    private boolean open;
    private int selectedLine;
    private boolean closeRequested;

    public void open(SignTileEntity sign) {
        this.sign = sign;
        this.open = sign != null;
        this.selectedLine = 0;
        this.closeRequested = false;
        if (open) {
            Input.setCursorLocked(false);
        }
    }

    public void close() {
        this.open = false;
        this.sign = null;
        this.selectedLine = 0;
        this.closeRequested = false;
        Input.setCursorLocked(true);
    }

    public void update() {
        update(0, 0);
    }

    public void update(int screenWidth, int screenHeight) {
        if (!open || sign == null) {
            return;
        }

        if (screenWidth > 0 && screenHeight > 0 && Input.isButtonPressed(GLFW_MOUSE_BUTTON_LEFT)
                && doneButtonBounds(screenWidth, screenHeight).contains(Input.getMouseX(), Input.getMouseY())) {
            closeRequested = true;
            return;
        }

        if (Input.isKeyPressed(GLFW_KEY_ENTER) || Input.isKeyPressed(GLFW_KEY_KP_ENTER)
                || Input.isKeyPressed(GLFW_KEY_DOWN)) {
            selectedLine = (selectedLine + 1) % sign.getLines().length;
        }
        if (Input.isKeyPressed(GLFW_KEY_UP)) {
            selectedLine = (selectedLine + sign.getLines().length - 1) % sign.getLines().length;
        }
        if (Input.isKeyPressed(GLFW_KEY_BACKSPACE)) {
            String line = sign.getLines()[selectedLine];
            if (!line.isEmpty()) {
                sign.setLine(selectedLine, line.substring(0, line.length() - 1));
            }
        }
        for (char c : Input.getTypedCharacters()) {
            if (isAllowedSignCharacter(c)) {
                String line = sign.getLines()[selectedLine];
                sign.setLine(selectedLine, line + c);
            }
        }
    }

    public boolean consumeCloseRequest() {
        boolean requested = closeRequested;
        closeRequested = false;
        return requested;
    }

    public static ButtonBounds doneButtonBounds(int screenWidth, int screenHeight) {
        int x = (screenWidth - DONE_BUTTON_WIDTH) / 2;
        int y = screenHeight - DONE_BUTTON_BOTTOM_MARGIN - DONE_BUTTON_HEIGHT;
        return new ButtonBounds(x, y, DONE_BUTTON_WIDTH, DONE_BUTTON_HEIGHT);
    }

    public static boolean isAllowedSignCharacter(char c) {
        return SignTileEntity.isAllowedSignCharacter(c);
    }

    public boolean isOpen() {
        return open;
    }

    public SignTileEntity getSign() {
        return sign;
    }

    public int getSelectedLine() {
        return selectedLine;
    }

    public record ButtonBounds(int x, int y, int width, int height) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width
                    && mouseY >= y && mouseY < y + height;
        }
    }
}

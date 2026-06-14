package com.craftzero.ui;

import com.craftzero.engine.Input;
import com.craftzero.world.tile.SignTileEntity;

import static org.lwjgl.glfw.GLFW.*;

public class SignEditScreen {
    private SignTileEntity sign;
    private boolean open;
    private int selectedLine;

    public void open(SignTileEntity sign) {
        this.sign = sign;
        this.open = sign != null;
        this.selectedLine = 0;
        Input.setCursorLocked(false);
    }

    public void close() {
        this.open = false;
        this.sign = null;
        this.selectedLine = 0;
    }

    public void update() {
        if (!open || sign == null) {
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
            if (c >= 32 && c < 127) {
                sign.setLine(selectedLine, sign.getLines()[selectedLine] + c);
            }
        }
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
}

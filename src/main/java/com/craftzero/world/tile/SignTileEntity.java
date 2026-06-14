package com.craftzero.world.tile;

/**
 * Four-line sign text tile entity. Release 1.0 signs store text only; editing is
 * handled by the sign screen after placement.
 */
public class SignTileEntity extends TileEntity {
    private static final int LINE_COUNT = 4;
    private static final int MAX_LINE_LENGTH = 15;

    private final String[] lines = new String[LINE_COUNT];

    public SignTileEntity(int x, int y, int z) {
        super(x, y, z);
        for (int i = 0; i < lines.length; i++) {
            lines[i] = "";
        }
    }

    @Override
    public String getTypeId() {
        return "sign";
    }

    public String[] getLines() {
        return lines;
    }

    public void setLine(int index, String value) {
        if (index < 0 || index >= lines.length) {
            return;
        }
        String text = value == null ? "" : value;
        lines[index] = text.length() > MAX_LINE_LENGTH ? text.substring(0, MAX_LINE_LENGTH) : text;
        markDirty();
    }
}

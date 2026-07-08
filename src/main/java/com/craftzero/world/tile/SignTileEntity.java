package com.craftzero.world.tile;

import java.util.Arrays;

/**
 * Four-line sign text tile entity. Release 1.0 signs store text only; editing is
 * handled by the sign screen after placement.
 */
public class SignTileEntity extends TileEntity {
    private static final int LINE_COUNT = 4;
    private static final int MAX_LINE_LENGTH = 15;
    private static final String ALLOWED_TEXT_CHARACTERS =
            " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"
                    + "\u2302\u00C7\u00FC\u00E9\u00E2\u00E4\u00E0\u00E5\u00E7\u00EA\u00EB\u00E8"
                    + "\u00EF\u00EE\u00EC\u00C4\u00C5\u00C9\u00E6\u00C6\u00F4\u00F6\u00F2"
                    + "\u00FB\u00F9\u00FF\u00D6\u00DC\u00A2\u00A3\u00A5\u20A7\u0192\u00E1"
                    + "\u00ED\u00F3\u00FA\u00F1\u00D1\u00AA\u00BA\u00BF\u2310\u00AC\u00BD"
                    + "\u00BC\u00A1\u00AB\u00BB";

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
        return Arrays.copyOf(lines, lines.length);
    }

    public void setLine(int index, String value) {
        if (index < 0 || index >= lines.length) {
            return;
        }
        lines[index] = sanitizeLine(value);
        markDirty();
    }

    private static String sanitizeLine(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder sanitized = new StringBuilder(Math.min(value.length(), MAX_LINE_LENGTH));
        for (int i = 0; i < value.length() && sanitized.length() < MAX_LINE_LENGTH; i++) {
            char c = value.charAt(i);
            if (isAllowedSignCharacter(c)) {
                sanitized.append(c);
            }
        }
        return sanitized.toString();
    }

    public static boolean isAllowedSignCharacter(char c) {
        return ALLOWED_TEXT_CHARACTERS.indexOf(c) >= 0;
    }
}

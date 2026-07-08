package com.craftzero.world;

final class ReleaseOneMath {
    private static final float[] SIN_TABLE = new float[65536];

    static {
        for (int i = 0; i < SIN_TABLE.length; i++) {
            SIN_TABLE[i] = (float) Math.sin((double) i * Math.PI * 2.0 / 65536.0);
        }
    }

    private ReleaseOneMath() {
    }

    static float sin(float value) {
        return SIN_TABLE[(int) (value * 10430.378F) & 0xFFFF];
    }

    static float cos(float value) {
        return SIN_TABLE[(int) (value * 10430.378F + 16384.0F) & 0xFFFF];
    }
}

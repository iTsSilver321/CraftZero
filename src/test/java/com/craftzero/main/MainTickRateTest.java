package com.craftzero.main;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainTickRateTest {
    @Test
    @DisplayName("Main simulation loop should advance at the Release 1.0 20Hz tick rate")
    void mainFixedStepUsesReleaseOneTickRate() throws ReflectiveOperationException {
        assertEquals(20, mainIntConstant("TARGET_UPS"));
        assertEquals(1.0f / 20.0f, mainFloatConstant("FIXED_DELTA"), 0.0001f);
    }

    private static int mainIntConstant(String name) throws ReflectiveOperationException {
        Field field = Main.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(null);
    }

    private static float mainFloatConstant(String name) throws ReflectiveOperationException {
        Field field = Main.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getFloat(null);
    }
}

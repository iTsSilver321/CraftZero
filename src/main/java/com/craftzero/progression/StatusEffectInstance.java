package com.craftzero.progression;

public record StatusEffectInstance(StatusEffectType type, int durationTicks, int amplifier) {
    public StatusEffectInstance {
        if (type == null) {
            throw new IllegalArgumentException("status effect type cannot be null");
        }
        durationTicks = Math.max(0, durationTicks);
        amplifier = Math.max(0, amplifier);
    }

    public StatusEffectInstance ticked() {
        return new StatusEffectInstance(type, Math.max(0, durationTicks - 1), amplifier);
    }

    public boolean expired() {
        return durationTicks <= 0;
    }
}

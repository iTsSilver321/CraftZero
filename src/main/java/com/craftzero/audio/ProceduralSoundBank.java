package com.craftzero.audio;

import org.lwjgl.BufferUtils;

import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Deterministic non-copyright fallback sounds for installs without old OGG
 * media. Real resource-pack assets always take precedence.
 */
final class ProceduralSoundBank {
    static final int SAMPLE_RATE = 22_050;
    private static final int MAX_VARIANTS = 4;
    private static final float TWO_PI = (float) (Math.PI * 2.0);

    private ProceduralSoundBank() {
    }

    static List<ShortBuffer> synthesize(String soundId) {
        String normalized = SoundAssetResolver.normalizeSoundId(soundId);
        if (normalized.isEmpty()
                || normalized.endsWith(".ogg")
                || normalized.equals("craftzero.records.stop")) {
            return List.of();
        }

        int variants = variantCount(normalized);
        List<ShortBuffer> buffers = new ArrayList<>(variants);
        for (int variant = 0; variant < variants; variant++) {
            buffers.add(render(specFor(normalized, variant), normalized, variant));
        }
        return List.copyOf(buffers);
    }

    private static int variantCount(String id) {
        if (id.startsWith("music.") || id.startsWith("records.")) {
            return 1;
        }
        if (id.startsWith("dig.") || id.startsWith("step.")
                || id.startsWith("mob.") || id.startsWith("random.")
                || id.startsWith("damage.") || id.startsWith("ambient.")) {
            return MAX_VARIANTS;
        }
        return 2;
    }

    private static SoundSpec specFor(String id, int variant) {
        float detune = 1.0f + (variant - 1.5f) * 0.035f;
        if (id.startsWith("music.")) {
            return new SoundSpec(8.0f, 174.0f * detune, 196.0f * detune,
                    0.42f, 0.05f, 0.26f, 0.45f, 1.2f, 0.25f, 0.18f, 0.55f, Wave.SINE);
        }
        if (id.startsWith("records.")) {
            return new SoundSpec(7.0f, 220.0f * detune, 330.0f * detune,
                    0.55f, 0.06f, 0.32f, 0.08f, 0.8f, 0.35f, 0.75f, 0.32f, Wave.TRIANGLE);
        }
        if (id.equals("note.bd")) {
            return new SoundSpec(0.34f, 96.0f * detune, 42.0f * detune,
                    0.62f, 0.34f, 0.58f, 0.001f, 0.10f, 7.0f, 0.0f, 0.0f, Wave.SINE);
        }
        if (id.equals("note.snare")) {
            return new SoundSpec(0.28f, 260.0f * detune, 190.0f * detune,
                    0.22f, 0.74f, 0.48f, 0.001f, 0.09f, 7.8f, 18.0f, 0.15f, Wave.NOISE);
        }
        if (id.equals("note.hat")) {
            return new SoundSpec(0.18f, 2200.0f * detune, 1200.0f * detune,
                    0.28f, 0.76f, 0.34f, 0.001f, 0.05f, 9.5f, 25.0f, 0.20f, Wave.NOISE);
        }
        if (id.equals("note.bass") || id.equals("note.bassattack")) {
            return new SoundSpec(0.50f, 130.0f * detune, 115.0f * detune,
                    0.78f, 0.04f, 0.48f, 0.003f, 0.12f, 4.8f, 0.0f, 0.0f, Wave.SAW);
        }
        if (id.startsWith("note.")) {
            return new SoundSpec(0.62f, 440.0f * detune, 440.0f * detune,
                    0.82f, 0.01f, 0.55f, 0.004f, 0.12f, 4.8f, 0.0f, 0.0f, Wave.SQUARE);
        }
        if (id.startsWith("dig.")) {
            return materialSpec(id, variant, true);
        }
        if (id.startsWith("step.")) {
            return materialSpec(id, variant, false);
        }

        return switch (id) {
            case "random.click" -> new SoundSpec(0.08f, 880.0f * detune, 560.0f * detune,
                    0.72f, 0.10f, 0.42f, 0.001f, 0.02f, 8.0f, 0.0f, 0.0f, Wave.SQUARE);
            case "random.pop" -> new SoundSpec(0.12f, 960.0f * detune, 1320.0f * detune,
                    0.72f, 0.05f, 0.36f, 0.001f, 0.04f, 6.5f, 0.0f, 0.0f, Wave.SINE);
            case "random.orb" -> new SoundSpec(0.20f, 1180.0f * detune, 1780.0f * detune,
                    0.78f, 0.03f, 0.32f, 0.002f, 0.08f, 4.5f, 11.0f, 0.20f, Wave.SINE);
            case "random.levelup" -> new SoundSpec(0.92f, 520.0f * detune, 1040.0f * detune,
                    0.78f, 0.02f, 0.40f, 0.01f, 0.24f, 1.2f, 7.5f, 0.28f, Wave.TRIANGLE);
            case "random.bow" -> new SoundSpec(0.22f, 420.0f * detune, 125.0f * detune,
                    0.60f, 0.14f, 0.38f, 0.001f, 0.08f, 7.5f, 18.0f, 0.25f, Wave.TRIANGLE);
            case "random.fuse" -> new SoundSpec(0.82f, 160.0f, 210.0f,
                    0.10f, 0.95f, 0.30f, 0.01f, 0.15f, 0.8f, 22.0f, 0.18f, Wave.NOISE);
            case "random.fizz", "fire.ignite" -> new SoundSpec(0.58f, 260.0f, 390.0f,
                    0.18f, 0.85f, 0.34f, 0.002f, 0.12f, 1.6f, 24.0f, 0.20f, Wave.NOISE);
            case "random.splash" -> new SoundSpec(0.54f, 180.0f * detune, 95.0f * detune,
                    0.28f, 0.65f, 0.42f, 0.001f, 0.18f, 3.0f, 10.0f, 0.16f, Wave.SINE);
            case "random.explode" -> new SoundSpec(1.25f, 82.0f * detune, 36.0f * detune,
                    0.62f, 0.72f, 0.62f, 0.002f, 0.42f, 3.5f, 18.0f, 0.22f, Wave.SINE);
            case "random.glass" -> new SoundSpec(0.38f, 1480.0f * detune, 680.0f * detune,
                    0.58f, 0.42f, 0.36f, 0.001f, 0.16f, 6.0f, 19.0f, 0.28f, Wave.TRIANGLE);
            case "random.eat" -> new SoundSpec(0.18f, 210.0f * detune, 170.0f * detune,
                    0.30f, 0.62f, 0.32f, 0.001f, 0.05f, 7.0f, 20.0f, 0.20f, Wave.NOISE);
            case "random.drink" -> new SoundSpec(0.35f, 260.0f * detune, 180.0f * detune,
                    0.34f, 0.46f, 0.30f, 0.003f, 0.12f, 2.2f, 12.0f, 0.28f, Wave.SINE);
            case "random.burp" -> new SoundSpec(0.32f, 170.0f * detune, 78.0f * detune,
                    0.62f, 0.20f, 0.38f, 0.005f, 0.08f, 4.0f, 9.0f, 0.20f, Wave.SAW);
            case "random.door_open", "random.door_close" -> new SoundSpec(0.42f, 150.0f * detune, 92.0f * detune,
                    0.42f, 0.38f, 0.34f, 0.003f, 0.12f, 2.8f, 7.0f, 0.22f, Wave.SAW);
            case "random.chestopen", "random.chestclosed" -> new SoundSpec(0.32f, 210.0f * detune, 130.0f * detune,
                    0.44f, 0.28f, 0.30f, 0.002f, 0.10f, 3.4f, 9.0f, 0.18f, Wave.TRIANGLE);
            case "tile.piston.out", "tile.piston.in" -> new SoundSpec(0.34f, 118.0f * detune, 74.0f * detune,
                    0.52f, 0.55f, 0.42f, 0.001f, 0.12f, 3.6f, 14.0f, 0.18f, Wave.SAW);
            case "damage.fallsmall", "damage.fallbig", "random.hurt",
                    "random.classic_hurt", "random.old_hurt", "damage.hurt",
                    "damage.hurtflesh", "damage.hit" -> new SoundSpec(
                    id.equals("damage.fallbig") ? 0.42f : 0.24f,
                    128.0f * detune, 72.0f * detune, 0.46f, 0.38f, 0.40f,
                    0.001f, 0.10f, 5.0f, 12.0f, 0.16f, Wave.SAW);
            case "portal.portal", "mob.endermen.portal" -> new SoundSpec(1.40f, 165.0f * detune, 247.0f * detune,
                    0.60f, 0.34f, 0.34f, 0.05f, 0.28f, 0.8f, 5.0f, 0.62f, Wave.SINE);
            case "ambient.cave.cave" -> new SoundSpec(2.80f, 54.0f * detune, 88.0f * detune,
                    0.54f, 0.38f, 0.42f, 0.20f, 1.00f, 0.35f, 2.8f, 0.46f, Wave.SINE);
            case "ambient.weather.rain" -> new SoundSpec(1.25f, 210.0f, 260.0f,
                    0.06f, 0.90f, 0.26f, 0.02f, 0.20f, 0.2f, 31.0f, 0.16f, Wave.NOISE);
            case "ambient.weather.thunder" -> new SoundSpec(2.20f, 72.0f * detune, 32.0f * detune,
                    0.66f, 0.58f, 0.65f, 0.006f, 0.80f, 2.2f, 5.0f, 0.28f, Wave.SINE);
            default -> mobOrGenericSpec(id, detune);
        };
    }

    private static SoundSpec materialSpec(String id, int variant, boolean dig) {
        float detune = 1.0f + (variant - 1.5f) * 0.04f;
        float seconds = dig ? 0.22f : 0.12f;
        float volume = dig ? 0.38f : 0.23f;
        if (id.endsWith(".wood") || id.endsWith(".ladder")) {
            return new SoundSpec(seconds, 260.0f * detune, 190.0f * detune,
                    0.46f, 0.34f, volume, 0.001f, 0.05f, 6.2f, 17.0f, 0.18f, Wave.TRIANGLE);
        }
        if (id.endsWith(".gravel") || id.endsWith(".sand") || id.endsWith(".snow")) {
            return new SoundSpec(seconds, 180.0f * detune, 130.0f * detune,
                    0.18f, 0.68f, volume, 0.001f, 0.06f, 7.5f, 22.0f, 0.16f, Wave.NOISE);
        }
        if (id.endsWith(".grass") || id.endsWith(".cloth")) {
            return new SoundSpec(seconds, 210.0f * detune, 160.0f * detune,
                    0.24f, 0.54f, volume, 0.001f, 0.05f, 7.0f, 18.0f, 0.18f, Wave.NOISE);
        }
        return new SoundSpec(seconds, 330.0f * detune, 150.0f * detune,
                0.44f, 0.44f, volume, 0.001f, 0.04f, 8.0f, 15.0f, 0.12f, Wave.SQUARE);
    }

    private static SoundSpec mobOrGenericSpec(String id, float detune) {
        if (id.contains("ghast")) {
            return new SoundSpec(1.20f, 230.0f * detune, 120.0f * detune,
                    0.70f, 0.25f, 0.40f, 0.04f, 0.30f, 1.2f, 4.5f, 0.45f, Wave.SINE);
        }
        if (id.contains("enderdragon")) {
            return new SoundSpec(2.00f, 98.0f * detune, 52.0f * detune,
                    0.70f, 0.42f, 0.55f, 0.02f, 0.55f, 1.7f, 3.0f, 0.36f, Wave.SAW);
        }
        if (id.contains("endermen")) {
            return new SoundSpec(0.95f, 142.0f * detune, 82.0f * detune,
                    0.68f, 0.30f, 0.38f, 0.025f, 0.24f, 2.0f, 7.0f, 0.48f, Wave.SINE);
        }
        if (id.contains("wolf")) {
            return new SoundSpec(0.46f, 390.0f * detune, 220.0f * detune,
                    0.62f, 0.22f, 0.34f, 0.004f, 0.12f, 3.2f, 12.0f, 0.26f, Wave.SAW);
        }
        if (id.contains("blaze") || id.contains("silverfish")) {
            return new SoundSpec(0.58f, 300.0f * detune, 190.0f * detune,
                    0.32f, 0.62f, 0.34f, 0.004f, 0.16f, 2.8f, 18.0f, 0.25f, Wave.NOISE);
        }
        if (id.contains("slime") || id.contains("magmacube")) {
            return new SoundSpec(0.24f, 145.0f * detune, 95.0f * detune,
                    0.54f, 0.30f, 0.35f, 0.001f, 0.08f, 5.0f, 14.0f, 0.22f, Wave.SINE);
        }
        if (id.contains("zombie") || id.contains("skeleton") || id.contains("creeper")
                || id.contains("spider")) {
            return new SoundSpec(0.55f, 185.0f * detune, 105.0f * detune,
                    0.58f, 0.32f, 0.36f, 0.006f, 0.16f, 3.0f, 8.0f, 0.26f, Wave.SAW);
        }
        if (id.contains("chicken") || id.contains("cow") || id.contains("pig") || id.contains("sheep")) {
            return new SoundSpec(0.42f, 310.0f * detune, 210.0f * detune,
                    0.58f, 0.18f, 0.32f, 0.004f, 0.14f, 3.4f, 11.0f, 0.24f, Wave.TRIANGLE);
        }
        return new SoundSpec(0.24f, 360.0f * detune, 180.0f * detune,
                0.48f, 0.28f, 0.30f, 0.002f, 0.08f, 4.8f, 12.0f, 0.18f, Wave.TRIANGLE);
    }

    private static ShortBuffer render(SoundSpec spec, String id, int variant) {
        int samples = Math.max(1, Math.round(spec.seconds() * SAMPLE_RATE));
        ShortBuffer buffer = BufferUtils.createShortBuffer(samples);
        Random random = new Random(31L * id.hashCode() + variant * 1_048_573L);
        float phase = random.nextFloat() * TWO_PI;
        float filteredNoise = 0.0f;
        for (int sample = 0; sample < samples; sample++) {
            float seconds = sample / (float) SAMPLE_RATE;
            float progress = samples <= 1 ? 1.0f : sample / (float) (samples - 1);
            float frequency = frequencyFor(spec, id, seconds, progress);
            phase += TWO_PI * frequency / SAMPLE_RATE;
            float tone = wave(spec.wave(), phase);
            filteredNoise = filteredNoise * 0.74f + (random.nextFloat() * 2.0f - 1.0f) * 0.26f;
            float tremolo = 1.0f;
            if (spec.tremoloHz() > 0.0f && spec.tremoloDepth() > 0.0f) {
                tremolo -= spec.tremoloDepth() * (0.5f + 0.5f * (float) Math.sin(TWO_PI * spec.tremoloHz() * seconds));
            }
            float value = (tone * spec.toneMix() + filteredNoise * spec.noiseMix())
                    * envelope(spec, seconds)
                    * tremolo
                    * spec.volume();
            buffer.put((short) Math.round(clamp(value, -1.0f, 1.0f) * Short.MAX_VALUE));
        }
        buffer.flip();
        return buffer;
    }

    private static float frequencyFor(SoundSpec spec, String id, float seconds, float progress) {
        if (id.startsWith("music.") || id.startsWith("records.")) {
            int[] phrase = id.startsWith("records.")
                    ? new int[] { 0, 3, 5, 7, 10, 7, 5, 3 }
                    : new int[] { 0, 2, 4, 7, 9, 7, 4, 2 };
            float stepRate = id.startsWith("records.") ? 2.4f : 1.35f;
            int step = Math.floorMod((int) Math.floor(seconds * stepRate), phrase.length);
            float root = lerp(spec.startHz(), spec.endHz(), progress * 0.35f);
            float vibrato = 1.0f + (float) Math.sin(TWO_PI * seconds * 4.0f) * 0.006f;
            return root * (float) Math.pow(2.0, phrase[step] / 12.0) * vibrato;
        }
        return lerp(spec.startHz(), spec.endHz(), progress);
    }

    private static float envelope(SoundSpec spec, float seconds) {
        float envelope = 1.0f;
        if (spec.attackSeconds() > 0.0f && seconds < spec.attackSeconds()) {
            envelope *= seconds / spec.attackSeconds();
        }
        float remaining = spec.seconds() - seconds;
        if (spec.releaseSeconds() > 0.0f && remaining < spec.releaseSeconds()) {
            envelope *= Math.max(0.0f, remaining / spec.releaseSeconds());
        }
        if (spec.decay() > 0.0f) {
            envelope *= (float) Math.exp(-spec.decay() * seconds / Math.max(0.001f, spec.seconds()));
        }
        return clamp(envelope, 0.0f, 1.0f);
    }

    private static float wave(Wave wave, float phase) {
        return switch (wave) {
            case SINE -> (float) Math.sin(phase);
            case SQUARE -> Math.sin(phase) >= 0.0 ? 1.0f : -1.0f;
            case TRIANGLE -> (float) (2.0 / Math.PI * Math.asin(Math.sin(phase)));
            case SAW -> 2.0f * (phase / TWO_PI - (float) Math.floor(phase / TWO_PI + 0.5f));
            case NOISE -> 0.0f;
        };
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float clamp(float value, float min, float max) {
        if (!Float.isFinite(value)) {
            return min <= 0.0f && max >= 0.0f ? 0.0f : min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private enum Wave {
        SINE,
        SQUARE,
        TRIANGLE,
        SAW,
        NOISE
    }

    private record SoundSpec(
            float seconds,
            float startHz,
            float endHz,
            float toneMix,
            float noiseMix,
            float volume,
            float attackSeconds,
            float releaseSeconds,
            float decay,
            float tremoloHz,
            float tremoloDepth,
            Wave wave) {
    }
}

package com.craftzero.graphics;

import java.util.HashMap;
import java.util.Map;

/**
 * Static texture cache for mob textures.
 * Ensures each texture is loaded only once per mob type.
 */
public class MobTexture {

    private static final Map<String, Texture> cache = new HashMap<>();

    /**
     * Get or load a texture from the cache.
     * 
     * @param path Resource path (e.g., "/textures/mob/zombie.png")
     * @return The texture
     */
    public static Texture get(String path) {
        return cache.computeIfAbsent(path, p -> {
            try {
                return new Texture(p);
            } catch (Exception e) {
                System.err.println("Failed to load mob texture: " + path);
                e.printStackTrace();
                return null;
            }
        });
    }

    /**
     * Preload common mob textures.
     */
    public static void preload() {
        String[] textures = {
                "/textures/mob/zombie.png",
                "/textures/mob/skeleton.png",
                "/textures/mob/creeper.png",
                "/textures/mob/spider.png",
                "/textures/mob/spider_eyes.png",
                "/textures/mob/pig.png",
                "/textures/mob/saddle.png",
                "/textures/mob/cow.png",
                "/textures/mob/redcow.png",
                "/textures/mob/sheep.png",
                "/textures/mob/sheep_fur.png",
                "/textures/mob/chicken.png",
                "/textures/mob/wolf.png",
                "/textures/mob/wolf_angry.png",
                "/textures/mob/wolf_tame.png",
                "/textures/armor/power.png",
                "/textures/mob/snowman.png"
        };

        for (String path : textures) {
            get(path);
        }
    }

    /**
     * Cleanup all cached textures.
     */
    public static void cleanup() {
        for (Texture texture : cache.values()) {
            if (texture != null) {
                texture.cleanup();
            }
        }
        cache.clear();
    }
}

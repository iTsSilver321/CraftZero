#version 330 core

in vec2 texCoord;
in vec3 fragPos;
in vec3 normal;
in vec3 vertexColor;
in float visibility;

out vec4 fragColor;

uniform sampler2D textureSampler;
uniform vec3 fogColor = vec3(0.6, 0.6, 0.6);
uniform bool fogEnabled;

// Lighting
uniform float ambientLight;
uniform vec3 lightDirection;
uniform vec3 lightColor;

// Day/Night: multiplies sky light (0.0 at midnight, 1.0 at noon)
uniform float sunBrightness = 1.0;

// Entity brightness: overrides vertex color lighting for entities/player
// When > 0, uses this instead of vertexColor for brightness
uniform float entityBrightness = 0.0;
uniform vec3 entityTint = vec3(1.0, 1.0, 1.0);

// Entity flash: 0.0 = no flash, 1.0 = full overlay color
uniform float hurtFlash = 0.0;
uniform vec3 hurtFlashColor = vec3(1.0, 0.4, 0.4);
uniform float alphaCutoff = 0.0;
uniform sampler2D glintSampler;
uniform bool glintMode = false;
uniform int glintPass = 0;
uniform float glintPhase = 0.0;
uniform vec3 glintColor = vec3(0.38, 0.19, 0.608);
uniform float glintAlpha = 0.58;
uniform bool anaglyphColorCorrection = false;
uniform bool solidColorMode = false;
uniform vec4 solidColor = vec4(1.0);

vec2 localItemUv(vec2 uv) {
    return fract(uv * 16.0);
}

vec2 glintUv(vec2 uv) {
    vec2 localUv = localItemUv(uv) - vec2(0.5);
    float angle = glintPass == 0 ? radians(-50.0) : radians(10.0);
    float c = cos(angle);
    float s = sin(angle);
    mat2 rotation = mat2(c, -s, s, c);
    float scroll = glintPass == 0 ? glintPhase * 2.0 : -glintPhase * 2.0;
    return rotation * localUv * 3.0 + vec2(0.5, scroll);
}

vec3 anaglyphCorrect(vec3 color) {
    if (!anaglyphColorCorrection) {
        return color;
    }
    return vec3(
        dot(color, vec3(0.30, 0.59, 0.11)),
        dot(color, vec3(0.30, 0.70, 0.00)),
        dot(color, vec3(0.30, 0.00, 0.70))
    );
}

void main() {
    // Small overlay primitives such as dynamic compass and clock needles render
    // as solid colored item-surface quads while still respecting fog/lighting.
    if (solidColorMode) {
        vec3 dynamicVertexColor;
        if (entityBrightness > 0.0) {
            dynamicVertexColor = vec3(entityBrightness * sunBrightness);
        } else {
            dynamicVertexColor = vertexColor * sunBrightness;
        }
        dynamicVertexColor = max(dynamicVertexColor, vec3(ambientLight * 0.15));

        vec3 result = solidColor.rgb * dynamicVertexColor;
        if (fogEnabled) {
            result = mix(fogColor, result, visibility);
        }
        result = anaglyphCorrect(result);
        fragColor = vec4(result, solidColor.a);
        return;
    }

    // Sample texture
    vec4 textureColor = texture(textureSampler, texCoord);
    
    // Discard alpha-tested pixels for cutout blocks and item sprites.
    if (textureColor.a <= alphaCutoff) {
        discard;
    }

    if (glintMode) {
        vec4 glintTextureColor = texture(glintSampler, glintUv(texCoord));
        float alpha = glintTextureColor.a * textureColor.a * glintAlpha;
        if (alpha <= 0.001) {
            discard;
        }

        vec3 result = glintTextureColor.rgb * glintColor;
        if (fogEnabled) {
            result = mix(fogColor, result, visibility);
        }
        result = anaglyphCorrect(result);
        fragColor = vec4(result, alpha);
        return;
    }
    
    // Determine lighting source
    vec3 dynamicVertexColor;
    if (entityBrightness > 0.0) {
        // Entity rendering: use uniform brightness instead of vertex color
        dynamicVertexColor = vec3(entityBrightness * sunBrightness);
    } else {
        // Block rendering: vertexColor contains biomeColor * faceShade * skyLight
        dynamicVertexColor = vertexColor * sunBrightness;
    }
    
    // Minimum floor for deep caves
    dynamicVertexColor = max(dynamicVertexColor, vec3(ambientLight * 0.15));
    
    // Apply to texture
    vec3 result = textureColor.rgb * entityTint * dynamicVertexColor;
    
    // Apply entity flash overlay
    if (hurtFlash > 0.0) {
        result = mix(result, hurtFlashColor, hurtFlash * 0.5);
    }
    
    // Apply fog
    if (fogEnabled) {
        result = mix(fogColor, result, visibility);
    }
    result = anaglyphCorrect(result);
    
    // Touch lightDirection and lightColor to prevent optimizer from removing them
    // (they may be used by other features like sun/moon rendering)
    result += lightColor * 0.0001 * max(0.0, dot(normalize(normal), normalize(lightDirection)));
    
    fragColor = vec4(result, textureColor.a);
}

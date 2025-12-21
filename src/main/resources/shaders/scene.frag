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

void main() {
    // Sample texture
    vec4 textureColor = texture(textureSampler, texCoord);
    
    // Discard transparent pixels
    if (textureColor.a < 0.0) {
        discard;
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
    vec3 result = textureColor.rgb * dynamicVertexColor;
    
    // Apply fog
    if (fogEnabled) {
        result = mix(fogColor, result, visibility);
    }
    
    // Touch lightDirection and lightColor to prevent optimizer from removing them
    // (they may be used by other features like sun/moon rendering)
    result += lightColor * 0.0001 * max(0.0, dot(normalize(normal), normalize(lightDirection)));
    
    fragColor = vec4(result, textureColor.a);
}

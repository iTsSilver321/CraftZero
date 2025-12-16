#version 330 core

in vec2 texCoord;
in vec3 fragPos;
in vec3 normal;
in float visibility;

out vec4 fragColor;

uniform sampler2D textureSampler;
uniform vec3 fogColor = vec3(0.6, 0.6, 0.6); // Default grey fog
uniform bool fogEnabled;

// Lighting
uniform float ambientLight;
uniform vec3 lightDirection;
uniform vec3 lightColor;

void main() {
    // Sample texture
    vec4 textureColor = texture(textureSampler, texCoord);
    
    // Discard transparent pixels
    if (textureColor.a < 0.1) {
        discard;
    }
    
    // Calculate lighting
    vec3 norm = normalize(normal);
    vec3 lightDir = normalize(lightDirection);
    
    // Diffuse lighting
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;
    
    // Combine ambient and diffuse
    vec3 lighting = ambientLight + diffuse * (1.0 - ambientLight);
    lighting = clamp(lighting, 0.0, 1.0);
    
    // Apply lighting to texture
    vec3 result = textureColor.rgb * lighting;
    
    // Apply fog
    if (fogEnabled) {
        result = mix(fogColor, result, visibility);
    }
    
    fragColor = vec4(result, textureColor.a);
}

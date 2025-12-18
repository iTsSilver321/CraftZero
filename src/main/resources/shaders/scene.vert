#version 330 core

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec2 aTexCoord;
layout (location = 2) in vec3 aNormal;
layout (location = 3) in vec3 aColor;

out vec2 texCoord;
out vec3 fragPos;
out vec3 normal;
out vec3 vertexColor;
out float visibility;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 modelMatrix;

uniform bool fogEnabled;
uniform float fogDensity;

void main() {
    vec4 worldPosition = modelMatrix * vec4(aPos, 1.0);
    vec4 positionRelativeToCam = viewMatrix * worldPosition;
    gl_Position = projectionMatrix * positionRelativeToCam;
    
    texCoord = aTexCoord;
    fragPos = worldPosition.xyz;
    normal = mat3(transpose(inverse(modelMatrix))) * aNormal;
    vertexColor = aColor;
    
    // Calculate fog visibility
    if (fogEnabled) {
        float distance = length(positionRelativeToCam.xyz);
        visibility = exp(-pow(distance * fogDensity, 2.0));
        visibility = clamp(visibility, 0.0, 1.0);
    } else {
        visibility = 1.0;
    }
}

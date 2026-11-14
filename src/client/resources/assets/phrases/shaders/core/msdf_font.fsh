#version 330

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

void main() {
    vec3 sample = texture(Sampler0, texCoord0).rgb;
    float signedDistance = median(sample.r, sample.g, sample.b) - 0.5;
    vec2 unitRange = vec2(8.0) / vec2(textureSize(Sampler0, 0));
    vec2 screenTexSize = 1.0 / fwidth(texCoord0);
    float screenPxRange = max(0.5 * dot(unitRange, screenTexSize), 1.0);
    float opacity = clamp(signedDistance * screenPxRange + 0.5, 0.0, 1.0);
    if (opacity < 0.01) discard;
    fragColor = vec4(vertexColor.rgb, vertexColor.a * opacity);
}

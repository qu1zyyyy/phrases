#version 330

in vec2 texCoord0;
in vec4 vertexColor;
out vec4 fragColor;

void main() {
    float distanceFromEdge = length(texCoord0 * 2.0 - 1.0) - 0.99;
    float edgeWidth = max(fwidth(distanceFromEdge), 0.001);
    float coverage = 1.0 - smoothstep(-edgeWidth, edgeWidth, distanceFromEdge);
    if (coverage <= 0.0) discard;
    fragColor = vec4(vertexColor.rgb, vertexColor.a * coverage);
}

#version 150
// Based on Modern UI, modified for Aaron Mod Chroma compatibility

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

#moj_import <modernui:chroma.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 texColor = textureLod(Sampler0, texCoord0, 0.0);

    float dist = texColor.a - 127./255. + 0.04;

    texColor.a = clamp(dist / fwidth(dist) + 0.5, 0.0, 1.0);

    vec4 color = texColor * vertexColor * ColorModulator;
    if (color.a < 0.01) discard;
    color = applyChroma(vertexColor, color);
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}

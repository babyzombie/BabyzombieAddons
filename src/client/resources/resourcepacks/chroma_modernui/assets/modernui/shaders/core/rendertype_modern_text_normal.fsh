#version 150
// Based on Modern UI, modified for Aaron Mod Chroma compatibility

#if !defined(IS_GUI)
#moj_import <minecraft:fog.glsl>
#endif
#moj_import <minecraft:dynamictransforms.glsl>

#moj_import <modernui:chroma.glsl>

uniform sampler2D Sampler0;

#if !defined(IS_GUI)
in float sphericalVertexDistance;
in float cylindricalVertexDistance;
#endif
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(Sampler0, texCoord0, -0.11875);
    vec4 color = texColor * vertexColor * ColorModulator;
    if (color.a < 0.01) discard;
    color = applyChroma(vertexColor, color);
#ifdef IS_GUI
    fragColor = color;
#else
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
#endif
}

#version 150
// Chroma implementation for Modern UI text pipelines
// Uses Chroma UBO (bound by Aaron Mod's RenderSystemMixin)

const vec3 BROWN = vec3(170.0, 85.0, 0.0) / 255.0;
const vec3 SHADOWED_BROWN = vec3(42.0, 21.0, 0.0) / 255.0;
const vec3 EPSILON = vec3(0.001);
const float NORMAL_VALUE = 1.0;
const float SHADOW_VALUE = 0.25;

layout(std140) uniform Chroma {
    float Ticks;
    float ChromaSize;
    float ChromaSpeed;
    float ChromaSaturation;
};

bool matchesColour(vec4 colour, vec3 targetColour) {
    return all(lessThan(abs(colour.rgb - targetColour), EPSILON));
}

vec3 hsv2rgb_smooth(vec3 c) {
    vec3 rgb = abs(mod(c.x * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0;
    rgb = smoothstep(0.0, 1.0, rgb);
    return c.z * mix(vec3(1.0), rgb, c.y);
}

vec4 applyChromaColourInternal(vec4 textColour, float v) {
    // Fallback defaults when Chroma UBO is not bound (Aaron Mod not installed)
    bool uboSet = ChromaSize + ChromaSpeed + ChromaSaturation + Ticks > 0.01;
    float size  = uboSet ? clamp(ChromaSize, 1.0, 200.0)       : 100.0;
    float speed = uboSet ? clamp(ChromaSpeed, 1.0, 64.0)       : 4.0;
    float sat   = uboSet ? clamp(ChromaSaturation, 0.0, 1.0)   : 0.85;

    float scale = size * 10.0;
    vec2 uv = gl_FragCoord.xy / scale;
    float offset = Ticks * (speed / 360.0);
    uv.x = uv.y - uv.x;
    uv.y = 0.0;
    float h = mod(offset + uv.x * 1.75, 1.0);
    vec3 hsv = vec3(h, sat, v);
    vec3 rgb = hsv2rgb_smooth(hsv);
    return vec4(rgb, textColour.a);
}

vec4 applyChroma(vec4 originalColour, vec4 textColour) {
    if (matchesColour(originalColour, BROWN)) {
        return applyChromaColourInternal(textColour, NORMAL_VALUE);
    }
    if (matchesColour(originalColour, SHADOWED_BROWN)) {
        return applyChromaColourInternal(textColour, SHADOW_VALUE);
    }
    return textColour;
}

#version 330

//WBOIT accumulation, vertex-color variant (scan frames, drag trail lines).

uniform vec4 ColorModulator;

in vec4 vColor;

layout(location = 0) out vec4 accum;
layout(location = 1) out vec4 reveal;

float weight(vec4 c) {
    float d = max(1.0 - gl_FragCoord.z * 0.9, 0.0);
    return clamp(pow(min(1.0, c.a * 10.0) + 0.01, 3.0) * 1e8 * pow(d, 3.0), 1e-2, 3e3);
}

void main() {
    vec4 c = vColor * ColorModulator;
    if (c.a <= 0.004) discard;
    float w = weight(c);
    accum = vec4(c.rgb * c.a, c.a) * w;
    reveal = vec4(c.a);
}

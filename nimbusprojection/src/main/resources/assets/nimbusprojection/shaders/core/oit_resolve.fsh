#version 330

//WBOIT resolve — Sampler0 = accum (RGBA16F), Sampler1 = reveal (R16F).
//Outputs premult coverage (acc.rgb / acc.a) with transmittance T as alpha;
//the caller blends (ONE_MINUS_SRC_ALPHA, SRC_ALPHA) over the scene.

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

in vec2 uv;

out vec4 fragColor;

void main() {
    vec4 acc = texture(Sampler0, uv);
    float T = texture(Sampler1, uv).r;
    //nothing accumulated (or a fully covered pixel, or an fp16 overflow from
    //the 1e8 weight term) — leave the scene untouched
    if (acc.a <= 1e-4 || T >= 0.99999 || isnan(acc.a) || isinf(acc.a)) discard;
    fragColor = vec4(acc.rgb / max(acc.a, 1e-4), T);
}

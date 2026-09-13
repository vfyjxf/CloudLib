#version 330

//WBOIT accumulation (McGuire & Bavoil 2013), textured fragment variant.
//Draw buffer 0 = premultiplied weighted color, draw buffer 1 = product of
//(1 - alpha) terms ("reveal"). Blend: (ONE, ONE) / (ZERO, ONE_MINUS_SRC_COLOR).

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;

in vec2 uv;

layout(location = 0) out vec4 accum;
layout(location = 1) out vec4 reveal;

float weight(vec4 c) {
    //self-normalizing variant: alpha-scaled, strongly depth-biased toward the
    //camera. pow() of a negative is undefined — clamp the depth term first.
    float d = max(1.0 - gl_FragCoord.z * 0.9, 0.0);
    return clamp(pow(min(1.0, c.a * 10.0) + 0.01, 3.0) * 1e8 * pow(d, 3.0), 1e-2, 3e3);
}

void main() {
    vec4 c = texture(Sampler0, uv) * ColorModulator;
    if (c.a <= 0.004) discard;
    float w = weight(c);
    // the panel FBO was filled by normal src-over blending, so its texels are
    // already premultiplied (rgb = color * alpha). Accumulating rgb*a again
    // would square the alpha and wash the whole panel out.
    accum = vec4(c.rgb, c.a) * w;
    reveal = vec4(c.a);
}

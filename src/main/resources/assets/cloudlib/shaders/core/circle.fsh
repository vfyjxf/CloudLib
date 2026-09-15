#version 150

// SDF ellipse / circle – optional border, optional texture.

in vec2 texCoord;

uniform sampler2D Sampler0;

uniform vec4  FillColor;

uniform vec2  Size;        // bounding-box size in pixels
uniform float BorderWidth;
uniform vec4  BorderColor;
uniform float Smoothing;
uniform int   HasTexture;

out vec4 fragColor;

float sdEllipse(vec2 p, vec2 r) {
    vec2 pn = p / r;
    float k = length(pn);
    return (k - 1.0) * min(r.x, r.y) / max(k, 1e-6);
}

void main() {
    vec2  pixel  = texCoord * Size;
    vec2  center = Size * 0.5;
    float d      = sdEllipse(pixel - center, center);

    float outer = 1.0 - smoothstep(-Smoothing, Smoothing, d);
    float inner = 1.0 - smoothstep(-Smoothing, Smoothing, d + BorderWidth);

    vec4 base = (HasTexture != 0)
              ? texture(Sampler0, texCoord) * FillColor
              : FillColor;

    fragColor = base * inner + BorderColor * (outer - inner);

    if (fragColor.a < 0.004) discard;
}

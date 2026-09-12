#version 330

//fullscreen triangle: a single (-1,-1)-(3,-1)-(-1,3) NDC triangle — the
//on-screen region maps uv to exactly 0..1

in vec3 Position;

out vec2 uv;

void main() {
    uv = Position.xy * 0.5 + 0.5;
    gl_Position = vec4(Position.xy, 0.0, 1.0);
}

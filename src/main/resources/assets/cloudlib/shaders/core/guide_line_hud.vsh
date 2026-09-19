#version 150

// The guide-line HUD pass is one quad per stroke: the geometry carries only
// the quad's corners, and every pixel of the stroke is resolved in the
// fragment from the Points uniform.

in vec3 Position;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 texCoord;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord    = UV0;
}

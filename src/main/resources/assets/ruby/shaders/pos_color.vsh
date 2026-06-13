#version 330 core

layout (location = 0) in vec4 pos;
layout (location = 1) in vec4 col;

layout (std140) uniform meshData {
    mat4 u_proj;
    mat4 u_modelView;
};

out vec4 vCol;
void main() {
    gl_Position = u_proj * u_modelView * pos;
    vCol = col;
}
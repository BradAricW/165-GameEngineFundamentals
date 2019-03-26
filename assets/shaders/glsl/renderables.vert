#version 410 core

// stage input(s)
layout (location = 0) in vec3 vertex_position;
layout (location = 1) in vec2 vertex_texcoord;
layout (location = 2) in vec3 vertex_normal;

// stage output(s)
out vertex_t
{
    vec2 vertex_texcoord;       // in texture-space
    vec3 vertex_position;       // in camera/view-space
    vec3 vertex_normal;         // in camera/view-space
} vs_out;

// uniform(s)/structure(s)
uniform struct matrix_t {
    mat4 model_view;            // view * model; transforms into view-space
    mat4 projection;            // transforms vertices into clip-space
    mat4 normal;                // inverse transpose of model-view matrix
} matrix;


void main()
{
    vec4 view_vertex        = matrix.model_view * vec4(vertex_position, 1);
    gl_Position             = matrix.projection * view_vertex;

    vs_out.vertex_texcoord  = vertex_texcoord;
    vs_out.vertex_normal    = mat3(matrix.normal) * vertex_normal;
    vs_out.vertex_position  = view_vertex.xyz;
}

#version 430 core

// stage output(s)
out vertex_t
{
    vec3 texcoord;
} vs_out;

// uniform(s)
uniform struct matrix_t {
    mat4 view;
} matrix;


void main()
{
    vec3[4] vertices = vec3[4](
        vec3(-1.0, -1.0, 1.0),
        vec3( 1.0, -1.0, 1.0),
        vec3(-1.0,  1.0, 1.0),
        vec3( 1.0,  1.0, 1.0)
    );
	mat3 v = mat3(matrix.view[0][0], matrix.view[1][0], matrix.view[2][0],
	              matrix.view[0][1], matrix.view[1][1], matrix.view[2][1],
				 -matrix.view[0][2],-matrix.view[1][2],-matrix.view[2][2]);
    vs_out.texcoord = v * vertices[gl_VertexID];
    gl_Position     = vec4(vertices[gl_VertexID], 1.0);
}

#version 430

// Texture map bindings for base diffuse color, height, and normals
layout (binding = 0) uniform sampler2D tex_color;
layout (binding = 1) uniform sampler2D tex_height;
layout (binding = 2) uniform sampler2D tex_normal;

// Stage input and outputs
in vec2 tc[];
out vec2 tcs_out[];
layout (vertices = 4) out;

// Uniforms
uniform mat4 mat4_norm;
uniform mat4 mat4_mvp;
uniform mat4 mat4_mv;
uniform mat4 mat4_p;
uniform float multiplier;
uniform float subdivisions;
uniform int  patchSize;
uniform int  hasTexture;
uniform int  hasHeightM;
uniform int  hasNormalM;
uniform struct ambient_light_t { vec4 intensity; } global_light;
uniform struct material_t
{
    vec4  ambient;
    vec4  diffuse;
    vec4  specular;
    vec4  emissive;
    float shininess;
} material;


/**
 * MAIN METHOD
 * 
 * Generates a plane of vertices (specified by a Tessellation's quality level). Depending on the
 * user-defined subdivisions, Level-of-Detail (LOD) operations will be performed as well.
 * LOD is performed as long as subdivisions are not zero.
 * 
 * If subdivisions are negative, the tessellation will still be generated, but it will cause
 * odd visual artifacts.
 */
void main(void)
{
	if (gl_InvocationID == 0)
	{ 	vec4 p0 = -mat4_mvp * gl_in[0].gl_Position;  p0 = p0 / p0.w;
		vec4 p1 = -mat4_mvp * gl_in[1].gl_Position;  p1 = p1 / p1.w;
		vec4 p2 = -mat4_mvp * gl_in[2].gl_Position;  p2 = p2 / p2.w;
		
		float width  = length(p1-p0) * subdivisions + 1.0;
		float height = length(p2-p0) * subdivisions + 1.0;
		gl_TessLevelOuter[0] = height;
		gl_TessLevelOuter[1] = width;
		gl_TessLevelOuter[2] = height;
		gl_TessLevelOuter[3] = width;
		gl_TessLevelInner[0] = width;
		gl_TessLevelInner[1] = height;
	}
	
	tcs_out[gl_InvocationID] = tc[gl_InvocationID];
	gl_out[gl_InvocationID].gl_Position = gl_in[gl_InvocationID].gl_Position;
}
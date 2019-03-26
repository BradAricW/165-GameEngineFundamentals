#version 430

// Texture map bindings for base diffuse color, height, and normals
layout (binding = 0) uniform sampler2D tex_color;
layout (binding = 1) uniform sampler2D tex_height;
layout (binding = 2) uniform sampler2D tex_normal;

// Stage output
out vec2 tc;

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
 * Identifies a plane of vertices (specified by a Tessellation's quality level), and determines texture coordinates.
 */
void main(void)
{	const vec2 patchTexCoords[] = vec2[] (vec2(0,0), vec2(1,0), vec2(0,1), vec2(1,1));
	
	// compute an offset for coordinates based on which instance this is
	int x = gl_InstanceID % patchSize;
	int y = gl_InstanceID / patchSize;
	
	// texture coordinates are distributed across specified patches
	tc = vec2( (x+patchTexCoords[gl_VertexID].x)/patchSize, (y+patchTexCoords[gl_VertexID].y)/patchSize);
	
	// vertex locations range from -0.5 to +0.5
	gl_Position = vec4(tc.x-0.5, 0.0, (1.0-tc.y)-0.5, 1.0);
}

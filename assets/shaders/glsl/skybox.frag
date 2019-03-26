#version 430 core

in vertex_t
{
    vec3 texcoord;
} fs_in;

out vec4 color;

layout (binding = 0) uniform samplerCube texture_sampler;


void main()
{
    color = texture(texture_sampler, fs_in.texcoord);
}

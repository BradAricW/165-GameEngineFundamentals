#version 430 core

// stage input(s)
in vertex_t
{
    vec2 vertex_texcoord;       // texture coordinate
    vec3 vertex_position;       // in camera/view-space
    vec3 vertex_normal;         // in camera/view-space
} fs_in;

// stage output(s)
out vec4 fragment;

// uniform(s)/structure(s)
uniform struct ambient_light_t
{
    vec4 intensity;
} global_light;

// XXX: This structure's definition including member order and byte sizes is
//      tightly coupled to ray.rage.rendersystem.shader.glsl.GlslRenderingProgram,
//      which needs to create organize data and take memory alignment into account.
//      Changing this server-side structure requires changes to the client-side
//      rendering program.
struct light_t
{
    // used by all lights
    vec4  ambient;
    vec4  diffuse;
    vec4  specular;
    vec4  position;             // in camera/view-space

    // only relevant for positional lights (i.e. point/spot);
    // avoid setting all of them to zero simultaneously; prevents division-by-zero
    float const_attenuation;
    float linear_attenuation;
    float quadratic_attenuation;
    float range;

    // only relevant for spot lights
    vec3  cone_direction;       // in camera/view-space, normalized by client
    float cone_cutoff_angle;    // in radians; range: [0, Math.PI/2), Math.PI
    float cone_falloff;
};

// https://www.khronos.org/opengl/wiki/Shader_Storage_Buffer_Object
// https://www.khronos.org/opengl/wiki/Interface_Block_(GLSL)
layout (std430, binding = 0) buffer ssbo_t
{
    light_t lights[];
} ssbo;

uniform struct material_t
{
    vec4  ambient;
    vec4  diffuse;
    vec4  specular;
    vec4  emissive;
    float shininess;
} material;

// bound to texture unit 0 by default
layout (binding = 0) uniform sampler2D texture_sampler;

// constant(s)
const float COMPENSATION = 3.0;


/**
 * Calculates the amount of attenuation that should be applied to the light after reaching
 * the surface being lit based on the light's distance from said surface. If the distance
 * exceeds the light's range, then the light does not illuminate the surface.
 */
float get_attenuation(
    light_t light
)
{
    float d = distance(fs_in.vertex_position, light.position.xyz);
    if (d > light.range)
        return 0.0;

    float dd  = d * d;
    float a0  = light.const_attenuation;
    float a1  = light.linear_attenuation;
    float a2  = light.quadratic_attenuation;
    return 1.0 / (a0 + a1 * d + a2 * dd);
}


/**
 * Calculates whether the fragment being lit is actually within the
 * light's area of effect (i.e. cone). If the fragment is outside this
 * cone, then the light does not illuminate the surface.
 *
 * NOTE: Point lights are a special case of spot lights, where the cone's
 * angle is set to 180 degrees, rather than limited to the [0, 90) range
 * for an actual spot light.
 */
float get_spot_factor(
    light_t light,
    vec3    light_direction
)
{
    // check for the special point light value and
    // return full light intensity if that's the case
    if (degrees(light.cone_cutoff_angle) == 180)
        return 1.0;

    // we have a spot light, so if the fragment is outside the
    // illumination cone, we completely eliminate the light's intensity
    float cos_angle = dot(-light_direction, light.cone_direction);
    if (acos(cos_angle) > light.cone_cutoff_angle)
        return 0.0;

    // the fragment must be inside the illumination cone, so we account for falloff
    // while also making sure we avoid the potential for negative bases, which
    // if combined with an odd falloff value, can turn light into darkness by
    // producing a negative scaling value that can result in the opposite of the
    // intended effect
    return pow(abs(cos_angle), light.cone_falloff);
}

/**
 * Calculates the amount of light that should be applied to the specific fragment,
 * taking the type of light, distance, attenuation, and other factors into account.
 */
vec4 get_light_effect(
    light_t     light,
    material_t  mat
)
{
    float attenuation;
    float spot_factor;
    vec3  light_dir;
    if (light.position.w == 0) {
        // we have a directional light, infinitely far away
        attenuation = 1.0;
        spot_factor = 1.0;
        light_dir   = normalize(light.position.xyz);
    } else {
        // we have a non-directional (i.e. point/spot) light
        light_dir   = normalize(light.position.xyz - fs_in.vertex_position);
        attenuation = get_attenuation(light);
        spot_factor = get_spot_factor(light, light_dir);
    }

    vec3 N = normalize(fs_in.vertex_normal);
    vec3 L = light_dir;                             // from light to vertex
    vec3 V = normalize(-fs_in.vertex_position);     // from vertex to viewer after negation
    vec3 H = normalize(L + V);                      // half-vector replaces: R = reflect(L)

    vec4 ambient  = light.ambient  * mat.ambient;
    vec4 diffuse  = light.diffuse  * mat.diffuse  * max(dot(N, L), 0);
    vec4 specular = light.specular * mat.specular * pow(max(dot(N, H), 0), mat.shininess * COMPENSATION);

    return ambient + attenuation * (diffuse + specular) * spot_factor + mat.emissive;
}


/**
 *
 */
void main()
{
    // account for global ambient light regardless of
    // whether local lights exist or not
    vec4 effect = material.ambient * global_light.intensity;

    for (int i = 0; i < ssbo.lights.length(); ++i)
        effect += get_light_effect(ssbo.lights[i], material);

    fragment = texture2D(texture_sampler, fs_in.vertex_texcoord) * effect;
}

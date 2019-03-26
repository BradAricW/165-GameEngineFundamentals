#version 430

// Texture map bindings for base diffuse color, height, and normals
layout (binding = 0) uniform sampler2D tex_color;
layout (binding = 1) uniform sampler2D tex_height;
layout (binding = 2) uniform sampler2D tex_normal;

// Stage inputs and output
in vec2 tes_out;
in vec3 varyingVertPos;
in vec3 varyingNormal;
out vec4 color;

// Constants
const float COMPENSATION = 3.0;

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

// Light Struct
struct light_t {
    vec4  ambient;
    vec4  diffuse;
    vec4  specular;
    vec4  position;

    float const_attenuation;
    float linear_attenuation;
    float quadratic_attenuation;
    float range;

    vec3  cone_direction;
    float cone_cutoff_angle;
    float cone_falloff;
};
layout (std430, binding = 0) buffer ssbo_t { light_t lights[]; } ssbo;


/**
 * Calculates the amount of attenuation that should be applied to the light after reaching
 * the surface being lit based on the light's distance from said surface. If the distance
 * exceeds the light's range, then the light does not illuminate the surface.
 */
float get_attenuation(light_t light) {
    float d = distance(varyingVertPos, light.position.xyz);

    if (d > light.range) return 0.0;

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
float get_spot_factor(light_t light, vec3 light_direction) {

	// Check for the special point light value and return full light intensity if that's the case
    if (degrees(light.cone_cutoff_angle) == 180) return 1.0;

    // For spot lights: If the fragment is outside the illumination cone, we completely eliminate the light's intensity
    float cos_angle = dot(-light_direction, light.cone_direction);
    if (acos(cos_angle) > light.cone_cutoff_angle) return 0.0;

    // The fragment must be inside the illumination cone, so we account for falloff
	// while also making sure we avoid the potential for negative bases, which
	// if combined with an odd falloff value, can turn light into darkness by
	// producing a negative scaling value that can result in the opposite of the
	// intended effect.
    return pow(abs(cos_angle), light.cone_falloff);
}


/**
 * Calculates the amount of light that should be applied to the specific fragment,
 * taking the type of light, distance, attenuation, and other factors into account.
 */
vec4 get_light_effect(light_t light, material_t mat) {

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
        light_dir   = normalize(light.position.xyz - varyingVertPos);
        attenuation = get_attenuation(light);
        spot_factor = get_spot_factor(light, light_dir);
    }

    vec3 N = varyingNormal;
    vec3 L = light_dir;                             // from light to vertex
    vec3 V = normalize(-varyingVertPos);            // from vertex to viewer after negation
    vec3 H = normalize(L + V);                      // half-vector replaces: R = reflect(L)

    vec4 ambient  = light.ambient  * mat.ambient;
    vec4 diffuse  = light.diffuse  * mat.diffuse  * max(dot(N, L), 0);
    vec4 specular = light.specular * mat.specular * pow(max(dot(N, H), 0), mat.shininess * COMPENSATION);

    return ambient + attenuation * (diffuse + specular) * spot_factor + mat.emissive;
}


/**
 * MAIN METHOD
 *
 * Determines the lighting for each fragment, and sends it to the next stage.
 */
void main() {
    // account for global ambient light regardless of
    // whether local lights exist or not
    vec4 effect = global_light.intensity;

    for (int i = 0; i < ssbo.lights.length(); ++i) {
        effect += get_light_effect(ssbo.lights[i], material);
    }

    // If the user has not binded a texture yet (or disabled it), simply output a plain white.
    if (hasTexture > 0) color = texture2D(tex_color, tes_out) * effect;
    else                color = vec4(1, 1, 1, 1) * effect;
}

#version 430 core

// stage input(s)
layout (location = 0) in vec3 vertex_position;
layout (location = 1) in vec2 vertex_texcoord;
layout (location = 2) in vec3 vertex_normal;
layout (location = 3) in vec3 vertex_bone_indices;
layout (location = 4) in vec3 vertex_bone_weights;


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

    mat4 skin_matrices[128];     // Skinning Matrices (supports up to 128 bones)
    mat3 skin_matrices_IT[128];   // IT of Skinning Matrices (used for transforming vertex normals)
} matrix;

//TODO: list of bone matrices
//TODO: additional vertex attributes for bone indices and weights

void main()
{
    // Calculating the model-space skinning transformation matrix for the vertex
    vec4 bone1_vert_pos;
    vec4 bone2_vert_pos;
    vec4 bone3_vert_pos;

    mat3 bone1_nor_mat3;
    mat3 bone2_nor_mat3;
    mat3 bone3_nor_mat3;

    int index;

    vec4 skinned_vert_pos = vec4(vertex_position,1.0);

    // Calculating bone 1's influence
    index = int(vertex_bone_indices.x);
    bone1_vert_pos = matrix.skin_matrices[index] * skinned_vert_pos;
    bone1_nor_mat3 = matrix.skin_matrices_IT[index];

    // Calculating bone 2's influence
    index = int(vertex_bone_indices.y);
    bone2_vert_pos = matrix.skin_matrices[index] * skinned_vert_pos;
    bone2_nor_mat3 = matrix.skin_matrices_IT[index];

    // Calculating bone 3's influence
    index = int(vertex_bone_indices.z);
    bone3_vert_pos = matrix.skin_matrices[index] * skinned_vert_pos;
    bone3_nor_mat3 = matrix.skin_matrices_IT[index];

    // Averaging all bone influences to get final vertex position
    skinned_vert_pos = bone1_vert_pos * vertex_bone_weights.x
            + bone2_vert_pos * vertex_bone_weights.y
            + bone3_vert_pos * vertex_bone_weights.z;

    // Calculate the skinned vertex normal
    // NOTE: We MUST normalize this result to get the correct skinned normal.
    // But don't normalize the result yet, if the vertex has no bones, normalizing will yield a normal
    // with inf / NaN values.
    // Normalize the normal matrix AFTER we handle vertices with no bones.
    vec3 skinned_vert_nor = (bone1_nor_mat3 * vertex_bone_weights.x
                         + bone2_nor_mat3 * vertex_bone_weights.y
                         + bone3_nor_mat3 * vertex_bone_weights.z) * vertex_normal;

    // If sum of weights is 0, return untransformed vertex data, else return transformed vertex data
    // This allows vertices that do not have weights for any bone.
    // Note: the file exporter ensures the total weight is either 1.0 or 0.0
    // (depending on whether or not the vertex is weighted to any bone at all)
    float total_weight = vertex_bone_weights.x + vertex_bone_weights.y + vertex_bone_weights.z;
    vec4 vert_pos = mix(vec4(vertex_position, 1.0), skinned_vert_pos, total_weight);
    vec3 vert_nor = mix(vertex_normal, skinned_vert_nor, total_weight);
    // Now we normalize the vertex normal
    vert_nor = normalize(vert_nor);




    vec4 view_vertex        = matrix.model_view * vert_pos;
    gl_Position             = matrix.projection * view_vertex;

    vs_out.vertex_texcoord  = vertex_texcoord;
    vs_out.vertex_normal    = mat3(matrix.normal) * vert_nor;
    vs_out.vertex_position  = view_vertex.xyz;
}

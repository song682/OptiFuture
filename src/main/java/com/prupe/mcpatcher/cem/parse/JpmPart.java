package com.prupe.mcpatcher.cem.parse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

/**
 * A JPM part model definition, either inline in a JEM file or loaded from an
 * external ".jpm" file, see "cem_part.txt" (L39-87).
 * <p>
 * JPM 部件模型定义，可内联于 JEM 文件或从外部 ".jpm" 文件加载，见 "cem_part.txt"（L39-87）。
 * <p>
 * Pure data holder filled by {@link JemParser}; all reference fields may be null.
 * <p>
 * 纯数据载体，由 {@link JemParser} 填充；引用类型字段缺省为 null。
 */
public class JpmPart {

    /** Resolved part texture, null to use the model texture. / 解析后的部件纹理，null 时用模型纹理。 */
    public ResourceLocation texture;

    /** Texture size [width, height] in pixels. / 纹理尺寸 [宽, 高]（像素）。 */
    public int[] textureSize;

    /** Axes to invert, e.g. "xyz". / 需要反转的轴，例如 "xyz"。 */
    public String invertAxis = "";

    /** Translation in pixels [x, y, z]. / 平移（像素）[x, y, z]。 */
    public float[] translate;

    /** Rotation in degrees [x, y, z]. / 旋转（角度）[x, y, z]。 */
    public float[] rotate;

    /** Texture axes to mirror, e.g. "uv". / 需要镜像的纹理轴，例如 "uv"。 */
    public String mirrorTexture = "";

    /** Attachment points: name to [tx, ty, tz]. / 附着点：名称 → [tx, ty, tz]。 */
    public Map<String, float[]> attachments = new LinkedHashMap<>();

    /** Boxes of this part. / 该部件的盒子列表。 */
    public List<JpmBox> boxes = new ArrayList<>();

    /** 3D sprites (depth 1 boxes). / 3D sprite（深度为 1 的盒子）。 */
    public List<JpmBox> sprites = new ArrayList<>();

    /** Sub-models ("submodel" plus "submodels"). / 子模型（合并 "submodel" 与 "submodels"）。 */
    public List<JpmPart> submodels = new ArrayList<>();

    /**
     * Sub-model ID from the JEM "id" field of inline submodels, used by hierarchical
     * animation references ("a:b:c"); null for anonymous submodels.
     * <p>
     * 内联子模型的 "id" 字段，供层级动画引用（"a:b:c"）使用；匿名子模型为 null。
     */
    public String id;
}

package com.prupe.mcpatcher.cem.parse;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.ResourceLocation;

/**
 * A parsed JEM entity model file, see "cem_model.txt" (L306-332).
 * <p>
 * 解析后的 JEM 实体模型文件，见 "cem_model.txt"（L306-332）。
 * <p>
 * Pure data holder filled by {@link JemParser}.
 * <p>
 * 纯数据载体，由 {@link JemParser} 填充。
 */
public class JemModel {

    /** Source .jem resource, used for logging and relative paths. / 源 .jem 资源，用于日志与相对路径。 */
    public final ResourceLocation source;

    /** Resolved default texture for all parts. / 所有部件的默认纹理（已解析）。 */
    public ResourceLocation texture;

    /** Texture size [width, height] in pixels. / 纹理尺寸 [宽, 高]（像素）。 */
    public int[] textureSize;

    /** Shadow size (0.0 - 1.0), NaN when unset. / 阴影大小（0.0 - 1.0），未设置为 NaN。 */
    public float shadowSize = Float.NaN;

    /** Part models in declaration order. / 按声明顺序的部件模型列表。 */
    public final List<JemModelPart> models = new ArrayList<>();

    public JemModel(ResourceLocation source) {
        this.source = source;
    }
}

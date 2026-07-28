package com.prupe.mcpatcher.cem.parse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One entry of the JEM "models" list, see "cem_model.txt" (L310-331): the part
 * model itself plus attachment information and animations.
 * <p>
 * JEM "models" 列表中的一个条目，见 "cem_model.txt"（L310-331）：部件模型本身
 * 加上挂接信息与动画。
 * <p>
 * Pure data holder filled by {@link JemParser}.
 * <p>
 * 纯数据载体，由 {@link JemParser} 填充。
 */
public class JemModelPart {

    /** Parent model ID for property inheritance. / 用于属性继承的父模型 ID。 */
    public String baseId;

    /** External ".jpm" file reference. / 外部 ".jpm" 文件引用。 */
    public String modelFile;

    /** Model ID, referable as baseId or from animations. / 模型 ID，可被 baseId 或动画引用。 */
    public String id;

    /** Entity part name this model attaches to / replaces. / 该模型挂接/替换的实体部件名。 */
    public String part;

    /** True: attach to the part, false: replace it. / true 挂接到部件，false 替换部件。 */
    public boolean attach;

    /** Render scale, default 1.0. / 渲染缩放，默认 1.0。 */
    public float scale = 1.0f;

    /**
     * The part model definition (inline fields merged with the external file, inline
     * wins). Never null after parsing.
     * <p>
     * 部件模型定义（内联字段与外部文件合并，内联优先）。解析后不为 null。
     */
    public JpmPart model = new JpmPart();

    /**
     * Animation entries in declaration order: target variable to expression source.
     * Compilation to {@link com.prupe.mcpatcher.cem.anim.CemAnimationEntry} happens later.
     * <p>
     * 按声明顺序的动画条目：目标变量 → 表达式源码。稍后再编译为
     * {@link com.prupe.mcpatcher.cem.anim.CemAnimationEntry}。
     */
    public final List<Map<String, String>> animations = new ArrayList<>();

    /**
     * Merge inherited properties from a parent part (baseId), see "cem_model.txt" L313.
     * Only fields not set locally are copied from the parent.
     * <p>
     * 从父部件（baseId）合并继承属性，见 "cem_model.txt" L313。仅复制本地未设置的字段。
     */
    public void inheritFrom(JemModelPart parent) {
        if (modelFile == null) {
            modelFile = parent.modelFile;
        }
        if (part == null) {
            part = parent.part;
        }
        JpmPart p = parent.model;
        if (model.texture == null) {
            model.texture = p.texture;
        }
        if (model.textureSize == null) {
            model.textureSize = p.textureSize;
        }
        if (model.invertAxis.isEmpty()) {
            model.invertAxis = p.invertAxis;
        }
        if (model.translate == null) {
            model.translate = p.translate;
        }
        if (model.rotate == null) {
            model.rotate = p.rotate;
        }
        if (model.mirrorTexture.isEmpty()) {
            model.mirrorTexture = p.mirrorTexture;
        }
        if (model.attachments.isEmpty()) {
            model.attachments = new LinkedHashMap<>(p.attachments);
        }
        if (model.boxes.isEmpty()) {
            model.boxes = new ArrayList<>(p.boxes);
        }
        if (model.sprites.isEmpty()) {
            model.sprites = new ArrayList<>(p.sprites);
        }
        if (model.submodels.isEmpty()) {
            model.submodels = new ArrayList<>(p.submodels);
        }
        if (animations.isEmpty()) {
            animations.addAll(parent.animations);
        }
    }
}

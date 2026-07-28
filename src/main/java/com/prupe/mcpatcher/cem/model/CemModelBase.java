package com.prupe.mcpatcher.cem.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.cem.anim.CemAnimationEntry;
import com.prupe.mcpatcher.cem.anim.ExpressionSyntaxException;
import com.prupe.mcpatcher.cem.anim.ModelVarType;
import com.prupe.mcpatcher.cem.parse.JemModel;
import com.prupe.mcpatcher.cem.parse.JemModelPart;

/**
 * A complete entity model built from a parsed JEM file: the top-level part
 * renderers, lookup indexes for animation references ("part", "id" and
 * hierarchical "a:b:c" paths) and the compiled animation entries in declaration
 * order. The rendering orchestration (replacing vanilla models, providing the
 * evaluation context) lives in the CEM orchestrator, not here.
 * <p>
 * 由解析后的 JEM 文件构建的完整实体模型：顶层部件渲染器、供动画引用使用的查找索引
 * （"part"、"id" 与层级 "a:b:c" 路径），以及按声明顺序编译的动画条目。渲染编排
 * （替换原版模型、提供求值上下文）由 CEM 编排器负责，不在本类中。
 */
public class CemModelBase extends ModelBase {

    private static final MCLogger logger = MCLogger.getLogger(MCLogger.Category.CUSTOM_ENTITY_MODELS);

    /**
     * One compiled animation entry bound to the part it was declared on ("this").
     * <p>
     * 一条编译后的动画条目，绑定到声明它的部件（"this"）。
     */
    public static final class PartAnimation {

        public final CemModelRenderer owner;
        public final CemAnimationEntry entry;

        PartAnimation(CemModelRenderer owner, CemAnimationEntry entry) {
            this.owner = owner;
            this.entry = entry;
        }
    }

    /** Model default texture, may be null. / 模型默认纹理，可能为 null。 */
    private final ResourceLocation texture;

    /** Shadow size (0.0 - 1.0), NaN when unset. / 阴影大小（0.0 - 1.0），未设置为 NaN。 */
    private final float shadowSize;

    /** Top-level parts in declaration order. / 按声明顺序的顶层部件。 */
    private final List<CemModelRenderer> parts = new ArrayList<>();

    /** Entity part name ("head", "leg1"...) to renderer. / 实体部件名 → 渲染器。 */
    private final Map<String, CemModelRenderer> byPart = new HashMap<>();

    /** Model "id" (including submodel ids) to renderer. / 模型 "id"（含子部件 id）→ 渲染器。 */
    private final Map<String, CemModelRenderer> byId = new HashMap<>();

    /** Compiled animations in declaration order. / 按声明顺序编译的动画。 */
    private final List<PartAnimation> animations = new ArrayList<>();

    /** Specs already warned about, to log each only once. / 已告警过的引用，避免重复日志。 */
    private final Set<String> warnedSpecs = new HashSet<>();

    public CemModelBase(JemModel jem) {
        // Set before creating renderers: the ModelRenderer constructor copies these
        // 需在创建渲染器前设置：ModelRenderer 构造函数会复制这两个值
        if (jem.textureSize != null) {
            textureWidth = jem.textureSize[0];
            textureHeight = jem.textureSize[1];
        }
        texture = jem.texture;
        shadowSize = jem.shadowSize;

        for (JemModelPart partEntry : jem.models) {
            CemModelRenderer renderer = new CemModelRenderer(this, partEntry.model, texture);
            if (partEntry.scale != 1.0f) {
                renderer.scaleX = partEntry.scale;
                renderer.scaleY = partEntry.scale;
                renderer.scaleZ = partEntry.scale;
            }
            parts.add(renderer);
            if (partEntry.part != null) {
                byPart.putIfAbsent(partEntry.part, renderer);
            }
            if (partEntry.id != null) {
                byId.putIfAbsent(partEntry.id, renderer);
            }
            registerSubmodelIds(renderer);
            compileAnimations(jem, partEntry, renderer);
        }
    }

    /** Register submodel ids recursively for direct references. / 递归注册子部件 id 以支持直接引用。 */
    private void registerSubmodelIds(CemModelRenderer renderer) {
        if (renderer.childModels == null) {
            return;
        }
        for (Object child : renderer.childModels) {
            if (child instanceof CemModelRenderer) {
                CemModelRenderer cem = (CemModelRenderer) child;
                if (cem.boxName != null) {
                    byId.putIfAbsent(cem.boxName, cem);
                }
                registerSubmodelIds(cem);
            }
        }
    }

    /** Compile the part's animation entries, skipping invalid ones. / 编译部件的动画条目，跳过非法条目。 */
    private void compileAnimations(JemModel jem, JemModelPart partEntry, CemModelRenderer renderer) {
        for (Map<String, String> group : partEntry.animations) {
            for (Map.Entry<String, String> entry : group.entrySet()) {
                try {
                    animations.add(new PartAnimation(renderer, CemAnimationEntry.compile(entry.getKey(), entry.getValue())));
                } catch (ExpressionSyntaxException e) {
                    logger.warning("%s: bad animation '%s': %s", jem.source, entry.getKey(), e.getMessage());
                }
            }
        }
    }

    /**
     * Resolve a model reference from an animation expression: "this", a part name,
     * an "id", or a hierarchical path like "a:b:c" descending by submodel ids.
     * <p>
     * 解析动画表达式中的模型引用："this"、部件名、"id"，或按子部件 id 逐级下钻的
     * 层级路径 "a:b:c"。
     *
     * @param self the part the expression was declared on / 声明该表达式的部件
     * @return the renderer, or null if not found (warned once) / 找到的渲染器，未找到返回 null（仅告警一次）
     */
    public CemModelRenderer resolvePart(String spec, CemModelRenderer self) {
        String[] segments = spec.split(":");
        CemModelRenderer current;
        if ("this".equals(segments[0])) {
            current = self;
        } else {
            current = byId.get(segments[0]);
            if (current == null) {
                current = byPart.get(segments[0]);
            }
        }
        for (int i = 1; current != null && i < segments.length; i++) {
            current = current.getChild(segments[i]);
        }
        if (current == null && warnedSpecs.add(spec)) {
            logger.warning("unknown model reference '%s'", spec);
        }
        return current;
    }

    /**
     * Read a model variable for the animation engine, 0 when unresolved.
     * <p>
     * 为动画引擎读取模型变量，未解析到时返回 0。
     */
    public double getModelVar(String spec, ModelVarType type, CemModelRenderer self) {
        CemModelRenderer renderer = resolvePart(spec, self);
        return renderer == null ? 0.0 : renderer.getVar(type);
    }

    /**
     * Write a model variable for the animation engine, ignored when unresolved.
     * <p>
     * 为动画引擎写入模型变量，未解析到时忽略。
     */
    public void setModelVar(String spec, ModelVarType type, double value, CemModelRenderer self) {
        CemModelRenderer renderer = resolvePart(spec, self);
        if (renderer != null) {
            renderer.setVar(type, value);
        }
    }

    /** Model default texture, may be null. / 模型默认纹理，可能为 null。 */
    public ResourceLocation getTexture() {
        return texture;
    }

    /** Shadow size, NaN when unset. / 阴影大小，未设置为 NaN。 */
    public float getShadowSize() {
        return shadowSize;
    }

    /** Renderer replacing / attaching to an entity part, or null. / 替换/挂接指定实体部件的渲染器，可为 null。 */
    public CemModelRenderer getPartRenderer(String partName) {
        return byPart.get(partName);
    }

    /** Top-level parts in declaration order. / 按声明顺序的顶层部件。 */
    public List<CemModelRenderer> getParts() {
        return parts;
    }

    /** Compiled animations in declaration order. / 按声明顺序编译的动画。 */
    public List<PartAnimation> getAnimations() {
        return animations;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float age, float headYaw,
        float headPitch, float scale) {
        for (CemModelRenderer part : parts) {
            part.render(scale);
        }
    }
}

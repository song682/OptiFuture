package com.prupe.mcpatcher.cem;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.cem.model.CemModelBase;
import com.prupe.mcpatcher.cem.model.CemModelRenderer;
import com.prupe.mcpatcher.cem.parse.JemModel;
import com.prupe.mcpatcher.cem.parse.JemModelPart;
import com.prupe.mcpatcher.cem.parse.JemParser;
import com.prupe.mcpatcher.mal.resource.TexturePackAPI;
import com.prupe.mcpatcher.mal.resource.TexturePackChangeHandler;

import decok.dfcdvadstf.optifuture.config.MCPatcherForgeConfig;

/**
 * Custom Entity Models orchestrator. On every resource pack (re)load it scans
 * "optifine/cem/&lt;entity&gt;.jem", builds {@link CemModelBase} instances and swaps
 * the {@code ModelRenderer} fields of the live vanilla models (via
 * {@link CemPartMapper}), so vanilla {@code setRotationAngles} keeps driving the
 * replaced parts. Animations are evaluated per rendered entity through
 * {@link #runAnimations}, invoked by the render hook after the vanilla rotation
 * angles are set.
 * <p>
 * 自定义实体模型编排器。每次资源包（重新）加载时扫描
 * "optifine/cem/&lt;entity&gt;.jem"，构建 {@link CemModelBase} 并（经
 * {@link CemPartMapper}）替换运行中原版模型的 {@code ModelRenderer} 字段，因此原版
 * {@code setRotationAngles} 仍会驱动被替换的部件。动画由渲染钩子在原版旋转角设置完成
 * 后按实体调用 {@link #runAnimations} 求值。
 */
public class CustomEntityModels {

    private static final MCLogger logger = MCLogger.getLogger(MCLogger.Category.CUSTOM_ENTITY_MODELS);

    /** OptiFine entity name to entity class, phase 1 coverage. / OptiFine 实体名 → 实体类，阶段 1 覆盖范围。 */
    private static final Map<String, Class<?>> ENTITY_CLASSES = new LinkedHashMap<>();

    /** Entity names whose model is the render pass model (e.g. sheep wool). / 模型位于第二渲染通道的实体名（如羊毛）。 */
    private static final Map<String, String> PASS_MODELS = new LinkedHashMap<>();

    static {
        ENTITY_CLASSES.put("creeper", net.minecraft.entity.monster.EntityCreeper.class);
        ENTITY_CLASSES.put("pig", net.minecraft.entity.passive.EntityPig.class);
        ENTITY_CLASSES.put("cow", net.minecraft.entity.passive.EntityCow.class);
        ENTITY_CLASSES.put("mooshroom", net.minecraft.entity.passive.EntityMooshroom.class);
        ENTITY_CLASSES.put("sheep", net.minecraft.entity.passive.EntitySheep.class);
        ENTITY_CLASSES.put("chicken", net.minecraft.entity.passive.EntityChicken.class);
        ENTITY_CLASSES.put("wolf", net.minecraft.entity.passive.EntityWolf.class);
        ENTITY_CLASSES.put("villager", net.minecraft.entity.passive.EntityVillager.class);
        ENTITY_CLASSES.put("zombie", net.minecraft.entity.monster.EntityZombie.class);
        ENTITY_CLASSES.put("zombie_pigman", net.minecraft.entity.monster.EntityPigZombie.class);
        ENTITY_CLASSES.put("skeleton", net.minecraft.entity.monster.EntitySkeleton.class);
        ENTITY_CLASSES.put("spider", net.minecraft.entity.monster.EntitySpider.class);
        ENTITY_CLASSES.put("cave_spider", net.minecraft.entity.monster.EntityCaveSpider.class);
        ENTITY_CLASSES.put("enderman", net.minecraft.entity.monster.EntityEnderman.class);
        ENTITY_CLASSES.put("witch", net.minecraft.entity.monster.EntityWitch.class);
        ENTITY_CLASSES.put("iron_golem", net.minecraft.entity.monster.EntityIronGolem.class);
        ENTITY_CLASSES.put("snow_golem", net.minecraft.entity.monster.EntitySnowman.class);
        ENTITY_CLASSES.put("giant", net.minecraft.entity.monster.EntityGiantZombie.class);
        // Render pass models of already-listed renderers / 已列出渲染器的第二通道模型
        PASS_MODELS.put("sheep_wool", "sheep");
        PASS_MODELS.put("pig_saddle", "pig");
    }

    /** One model part replacement, undoable. / 一次可撤销的模型部件替换。 */
    private static final class AppliedPart {

        final CemPartMapper.PartSlot slot;
        final ModelRenderer original;
        final CemModelRenderer replacement;
        final boolean attached;

        AppliedPart(CemPartMapper.PartSlot slot, ModelRenderer original, CemModelRenderer replacement,
            boolean attached) {
            this.slot = slot;
            this.original = original;
            this.replacement = replacement;
            this.attached = attached;
        }

        void restore() {
            if (attached) {
                original.childModels.remove(replacement);
            } else {
                slot.set(original);
            }
        }
    }

    /** All CEM data bound to one living renderer. / 绑定到一个生物渲染器的全部 CEM 数据。 */
    private static final class Binding {

        final List<CemModelBase> models = new ArrayList<>();
        final List<AppliedPart> appliedParts = new ArrayList<>();
    }

    private static final Map<RendererLivingEntity, Binding> bindings = new IdentityHashMap<>();
    private static final CemEvalContext context = new CemEvalContext();

    private static boolean animationsEnabled;

    // frame_time / frame_counter state, advanced once per client frame by the hook
    // frame_time / frame_counter 状态，由钩子每客户端帧推进一次
    private static long lastFrameNanos;
    private static double frameTime = 1.0 / 60.0;
    private static int frameCounter;

    private CustomEntityModels() {}

    /**
     * Register the resource reload hook, called once at startup when the module is
     * enabled.
     * <p>
     * 注册资源重载钩子，模块启用时启动阶段调用一次。
     */
    public static void init() {
        TexturePackChangeHandler.register(new TexturePackChangeHandler(MCPatcherUtils.CUSTOM_ENTITY_MODELS, 2) {

            @Override
            public void beforeChange() {
                restoreAll();
            }

            @Override
            public void afterChange() {
                reload();
            }
        });
    }

    /** Undo every part replacement of the previous pack. / 撤销上一个资源包的全部部件替换。 */
    private static void restoreAll() {
        for (Binding binding : bindings.values()) {
            for (AppliedPart part : binding.appliedParts) {
                part.restore();
            }
        }
        bindings.clear();
    }

    /** Scan and apply all .jem models for the current pack. / 为当前资源包扫描并应用全部 .jem 模型。 */
    private static void reload() {
        MCPatcherForgeConfig config = MCPatcherForgeConfig.instance();
        animationsEnabled = config.cemAnimations;
        if (!config.customEntityModelsEnabled) {
            return;
        }
        int count = 0;
        for (Map.Entry<String, Class<?>> entry : ENTITY_CLASSES.entrySet()) {
            if (loadModel(entry.getKey(), entry.getValue(), false)) {
                count++;
            }
        }
        for (Map.Entry<String, String> entry : PASS_MODELS.entrySet()) {
            if (loadModel(entry.getKey(), ENTITY_CLASSES.get(entry.getValue()), true)) {
                count++;
            }
        }
        if (count > 0) {
            logger.info("loaded %d custom entity models", count);
        }
    }

    /**
     * Load one .jem and bind it to the entity's renderer.
     * <p>
     * 加载一个 .jem 并绑定到实体的渲染器。
     *
     * @param passModel bind to the render pass model instead of the main model /
     *                  绑定到第二通道模型而非主模型
     */
    private static boolean loadModel(String name, Class<?> entityClass, boolean passModel) {
        ResourceLocation resource = new ResourceLocation(JemParser.CEM_FOLDER + name + ".jem");
        if (entityClass == null || !TexturePackAPI.hasResource(resource)) {
            return false;
        }
        Object render = RenderManager.instance.entityRenderMap.get(entityClass);
        if (!(render instanceof RendererLivingEntity)) {
            logger.warning("%s: no living renderer for %s", resource, entityClass.getName());
            return false;
        }
        RendererLivingEntity living = (RendererLivingEntity) render;
        ModelBase target = passModel ? getRenderPassModel(living) : getMainModel(living);
        if (target == null) {
            logger.warning("%s: cannot access the %s model", resource, passModel ? "render pass" : "main");
            return false;
        }
        JemModel jem = JemParser.parseJem(resource);
        if (jem == null) {
            return false;
        }
        Map<String, CemPartMapper.PartSlot> slots = CemPartMapper.map(target);
        if (slots.isEmpty()) {
            logger.warning("%s: unsupported model class %s", resource, target.getClass().getName());
            return false;
        }
        Binding binding = bindings.computeIfAbsent(living, r -> new Binding());
        CemModelBase cem = new CemModelBase(jem);
        binding.models.add(cem);

        // jem.models and cem.getParts() are parallel lists / jem.models 与 cem.getParts() 一一对应
        List<CemModelRenderer> parts = cem.getParts();
        for (int i = 0; i < jem.models.size(); i++) {
            JemModelPart partEntry = jem.models.get(i);
            if (partEntry.part == null) {
                continue;
            }
            CemPartMapper.PartSlot slot = slots.get(partEntry.part);
            if (slot == null || slot.get() == null) {
                logger.warning("%s: unknown part '%s'", resource, partEntry.part);
                continue;
            }
            ModelRenderer original = slot.get();
            CemModelRenderer replacement = parts.get(i);
            if (partEntry.attach) {
                // Attached parts compose with the vanilla part transform, so make the
                // pivot relative to it
                // 挂接部件与原版部件变换叠加，因此枢轴改为相对原版部件
                replacement.rotationPointX -= original.rotationPointX;
                replacement.rotationPointY -= original.rotationPointY;
                replacement.rotationPointZ -= original.rotationPointZ;
                original.addChild(replacement);
            } else {
                slot.set(replacement);
            }
            binding.appliedParts.add(new AppliedPart(slot, original, replacement, partEntry.attach));
        }
        logger.fine("%s: bound %d parts, %d animations", resource, binding.appliedParts.size(), cem.getAnimations().size());
        return true;
    }

    /**
     * Evaluate all animation entries for one entity about to be rendered. Called by
     * the render hook after the vanilla rotation angles are applied.
     * <p>
     * 为即将渲染的实体求值全部动画条目。由渲染钩子在原版旋转角应用后调用。
     */
    public static void runAnimations(RendererLivingEntity renderer, EntityLivingBase entity, float partialTick) {
        if (!animationsEnabled) {
            return;
        }
        Binding binding = bindings.get(renderer);
        if (binding == null) {
            return;
        }
        for (CemModelBase model : binding.models) {
            context.prepare(entity, model, partialTick, 0.0);
            for (CemModelBase.PartAnimation animation : model.getAnimations()) {
                context.setCurrentPart(animation.owner);
                try {
                    animation.entry.apply(context);
                } catch (RuntimeException e) {
                    // Never let a bad expression crash rendering / 表达式异常不允许拖垮渲染
                    logger.error("animation failed: %s", e);
                }
            }
        }
    }

    /** True if this renderer has CEM data bound. / 该渲染器绑定了 CEM 数据时为 true。 */
    public static boolean hasBinding(RendererLivingEntity renderer) {
        return bindings.containsKey(renderer);
    }

    /**
     * Advance the per-frame animation clock, called once per client frame by the hook.
     * <p>
     * 推进每帧动画时钟，由钩子每客户端帧调用一次。
     */
    public static void beginFrame() {
        long now = System.nanoTime();
        if (lastFrameNanos != 0L) {
            frameTime = (now - lastFrameNanos) / 1.0e9;
        }
        lastFrameNanos = now;
        // Same wrap-around as OptiFine / 与 OptiFine 相同的回绕值
        frameCounter = (frameCounter + 1) % 720720;
    }

    /** Duration of the last frame in seconds. / 上一帧的时长（秒）。 */
    public static double getFrameTime() {
        return frameTime;
    }

    /** Frame counter, wraps at 720720. / 帧计数器，到 720720 回绕。 */
    public static double getFrameCounter() {
        return frameCounter;
    }

    /** Main model of a living renderer (protected field). / 生物渲染器的主模型（protected 字段）。 */
    private static ModelBase getMainModel(RendererLivingEntity renderer) {
        return (ModelBase) getModelField(renderer, "mainModel", "field_77045_g");
    }

    /** Render pass model of a living renderer. / 生物渲染器的第二通道模型。 */
    private static ModelBase getRenderPassModel(RendererLivingEntity renderer) {
        return (ModelBase) getModelField(renderer, "renderPassModel", "field_77046_h");
    }

    /**
     * Reflective field read trying the MCP name first, then the SRG name; only used
     * at resource reload time, never per frame.
     * <p>
     * 反射读取字段，先试 MCP 名再试 SRG 名；仅在资源重载时使用，绝不逐帧调用。
     */
    private static Object getModelField(Object owner, String mcpName, String srgName) {
        for (String name : new String[] { mcpName, srgName }) {
            try {
                java.lang.reflect.Field field = RendererLivingEntity.class.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(owner);
            } catch (NoSuchFieldException e) {
                // try the next name / 尝试下一个名称
            } catch (ReflectiveOperationException e) {
                logger.error("cannot read %s: %s", mcpName, e);
                return null;
            }
        }
        logger.error("field %s/%s not found in RendererLivingEntity", mcpName, srgName);
        return null;
    }
}

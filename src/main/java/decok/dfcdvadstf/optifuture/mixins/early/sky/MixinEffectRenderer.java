package decok.dfcdvadstf.optifuture.mixins.early.sky;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.prupe.mcpatcher.sky.FireworksHelper;

/**
 * Adds a fifth particle layer dedicated to fireworks and lets the fireworks
 * helper decide layer assignment, iteration bounds, layer skipping and blend
 * mode, so custom firework rendering can hook into the particle pipeline.
 * <p>
 * 新增专用于烟花的第五个粒子层，并让烟花辅助器决定层分配、遍历
 * 边界、层跳过与混合模式，使自定义烟花渲染能够接入粒子流水线。
 */
@SuppressWarnings({ "rawtypes" })
@Mixin(EffectRenderer.class)
public abstract class MixinEffectRenderer {

    @Shadow
    private List[] fxLayers;

    /**
     * Reallocates the particle layer array with an extra fireworks layer.
     * <p>
     * 重新分配粒子层数组，额外增加一个烟花层。
     */
    @Inject(
        method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/client/renderer/texture/TextureManager;)V",
        at = @At("RETURN"))
    private void optiFuture$expandFxLayers(World world, TextureManager manager, CallbackInfo ci) {
        this.fxLayers = new List[5];
        for (int i = 0; i < this.fxLayers.length; ++i) {
            this.fxLayers[i] = new ArrayList();
        }
    }

    /**
     * Routes a new particle to its fireworks-aware layer.
     * <p>
     * 将新粒子路由到其感知烟花的层。
     */
    @Redirect(
        method = "addEffect(Lnet/minecraft/client/particle/EntityFX;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/EntityFX;getFXLayer()I"))
    private int optiFuture$routeEffectLayer(EntityFX instance) {
        return FireworksHelper.getFXLayer(instance);
    }

    /**
     * Widens the layer iteration bound during update/clear to cover the extra
     * layer.
     * <p>
     * 在更新/清理时放宽层遍历上界，以覆盖新增的层。
     */
    @ModifyConstant(
        method = { "updateEffects()V", "clearEffects(Lnet/minecraft/world/World;)V" },
        constant = @Constant(intValue = 4))
    private int optiFuture$expandUpdateLayerBound(int constant) {
        return 5;
    }

    /**
     * Widens the layer iteration bound during rendering to cover the extra
     * layer.
     * <p>
     * 在渲染时放宽层遍历上界，以覆盖新增的层。
     */
    @ModifyConstant(method = "renderParticles(Lnet/minecraft/entity/Entity;F)V", constant = @Constant(intValue = 3))
    private int optiFuture$expandRenderLayerBound(int constant) {
        return 5;
    }

    /**
     * Captures the current layer index so later injections can address the
     * matching layer.
     * <p>
     * 捕获当前层索引，使后续注入能够定位对应的层。
     */
    @Inject(
        method = "renderParticles(Lnet/minecraft/entity/Entity;F)V",
        at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"),
        locals = LocalCapture.CAPTURE_FAILHARD)
    private void optiFuture$captureLayerIndex(Entity player, float partialTickTime, CallbackInfo ci, float f1, float f2,
        float f3, float f4, float f5, int k, int i, @Share("renderParticlesIndex") LocalIntRef renderParticlesIndex) {
        renderParticlesIndex.set(i);
    }

    /**
     * Lets the fireworks helper decide whether the current layer should be
     * skipped.
     * <p>
     * 让烟花辅助器决定当前层是否应被跳过。
     */
    @Redirect(
        method = "renderParticles(Lnet/minecraft/entity/Entity;F)V",
        at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"))
    private boolean optiFuture$skipEmptyLayer(List layer,
        @Share("renderParticlesIndex") LocalIntRef renderParticlesIndex) {
        return FireworksHelper
            .skipThisLayer(this.fxLayers[renderParticlesIndex.get()].isEmpty(), renderParticlesIndex.get());
    }

    /**
     * Applies the fireworks-specific blend mode for the current layer.
     * <p>
     * 为当前层应用烟花专用的混合模式。
     */
    @Redirect(
        method = "renderParticles(Lnet/minecraft/entity/Entity;F)V",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glBlendFunc(II)V", remap = false))
    private void optiFuture$setLayerBlendMode(int sfactor, int dfactor,
        @Share("renderParticlesIndex") LocalIntRef renderParticlesIndex) {
        FireworksHelper.setParticleBlendMethod(renderParticlesIndex.get(), 0, true);
    }
}

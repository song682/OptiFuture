package decok.dfcdvadstf.optifuture.mixins.early.renderpass;

import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.IWorldAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.prupe.mcpatcher.renderpass.RenderPass;
import com.prupe.mcpatcher.renderpass.RenderPassMap;

/**
 * Grows the world renderer to accommodate the custom render-pass system: it
 * allocates extra display lists and render slots, remaps the vanilla 1.8-style
 * pass indices to the internal 1.7 layout, brackets each pass with pre/post
 * hooks and hands lightmap toggling to the pass system.
 * <p>
 * 扩充世界渲染器以容纳自定义渲染 pass 系统：分配额外的显示列表与
 * 渲染槽位，将原版 1.8 风格的 pass 索引重映射到内部 1.7 布局，为每个
 * pass 前后加钩子，并将光照贴图开关交由 pass 系统处理。
 */
@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobal implements IWorldAccess {

    /**
     * Allocates enough display lists for the extra render passes (3 -> 5 per
     * chunk).
     * <p>
     * 为额外的渲染 pass 分配足够的显示列表（每区块 3 -> 5）。
     */
    @Redirect(
        method = "<init>(Lnet/minecraft/client/Minecraft;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GLAllocation;generateDisplayLists(I)I",
            ordinal = 0))
    private int optiFuture$enlargeDisplayLists(int n) {
        return GLAllocation.generateDisplayLists(n / 3 * 5);
    }

    /**
     * Reserves two extra render-list slots so the additional passes fit.
     * <p>
     * 预留两个额外的渲染列表槽位，以容纳新增的 pass。
     */
    @ModifyVariable(
        method = "loadRenderers()V",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
            remap = false,
            shift = At.Shift.AFTER),
        ordinal = 1)
    private int optiFuture$reserveExtraRenderLists(int input) {
        return input + 2;
    }

    // Order important here!

    /**
     * Begins a render pass, cancelling the render when the pass should be
     * skipped.
     * <p>
     * 开始一个渲染 pass，当该 pass 应被跳过时取消渲染。
     */
    @Inject(
        method = "sortAndRender(Lnet/minecraft/entity/EntityLivingBase;ID)I",
        at = @At(value = "HEAD"),
        cancellable = true)
    private void optiFuture$beginRenderPass(EntityLivingBase entity, int map18To17, double partialTickTime,
        CallbackInfoReturnable<Integer> cir) {
        if (!RenderPass.preRenderPass(RenderPassMap.map17To18(map18To17))) {
            cir.setReturnValue(RenderPass.postRenderPass(0));
        }
    }

    /**
     * Remaps the incoming pass index from the external layout to the internal
     * one.
     * <p>
     * 将传入的 pass 索引从外部布局重映射到内部布局。
     */
    @ModifyVariable(
        method = "sortAndRender(Lnet/minecraft/entity/EntityLivingBase;ID)I",
        at = @At(value = "HEAD"),
        ordinal = 0,
        argsOnly = true)
    private int optiFuture$remapRenderPassIndex(int map18To17) {
        return RenderPassMap.map18To17(map18To17);
    }

    /**
     * Finalises the render pass by post-processing the returned pass count.
     * <p>
     * 通过对返回的 pass 计数做后处理来结束该渲染 pass。
     */
    @Inject(
        method = "sortAndRender(Lnet/minecraft/entity/EntityLivingBase;ID)I",
        at = @At(value = "RETURN"),
        cancellable = true)
    private void optiFuture$finishRenderPass(EntityLivingBase entity, int renderPass, double partialTickTime,
        CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(RenderPass.postRenderPass(cir.getReturnValue()));
    }

    /**
     * Delegates lightmap enabling/disabling during render-list playback to the
     * render-pass system.
     * <p>
     * 将渲染列表回放期间的光照贴图开关委托给渲染 pass 系统。
     */
    @Redirect(
        method = "renderAllRenderLists(ID)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;enableLightmap(D)V"))
    private void optiFuture$toggleRenderPassLightmap(EntityRenderer instance, double partialTick) {
        RenderPass.enableDisableLightmap(instance, partialTick);
    }
}

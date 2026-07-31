package decok.dfcdvadstf.optifuture.mixins.early.renderpass;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.EntityLivingBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.prupe.mcpatcher.renderpass.RenderPass;

/**
 * Extends world rendering with the extra render passes used by the custom
 * render-pass system: it keeps ambient occlusion in sync with the pass state
 * and issues additional solid and translucent passes (including a rain/snow
 * pass) beyond the two vanilla ones.
 * <p>
 * 为世界渲染扩展自定义渲染 pass 系统所需的额外 pass：使环境光遮蔽
 * 与 pass 状态保持一致，并在原版两个 pass 之外追加实体固体与半透明
 * pass（含一个雨雪 pass）。
 */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @Shadow
    private Minecraft mc;

    @Shadow
    protected abstract void renderRainSnow(float p_78474_1_);

    /**
     * Overrides the first shade-model toggle with the render-pass ambient
     * occlusion decision.
     * <p>
     * 用渲染 pass 的环境光遮蔽决策覆盖第一处着色模型开关。
     */
    @WrapWithCondition(
        method = "renderWorld(FJ)V",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glShadeModel(I)V", remap = false, ordinal = 0))
    private boolean optiFuture$syncAmbientOcclusionFirst(int i) {
        return RenderPass.setAmbientOcclusion(this.mc.gameSettings.ambientOcclusion != 0);
    }

    /**
     * Overrides the third shade-model toggle with the render-pass ambient
     * occlusion decision.
     * <p>
     * 用渲染 pass 的环境光遮蔽决策覆盖第三处着色模型开关。
     */
    @WrapWithCondition(
        method = "renderWorld(FJ)V",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glShadeModel(I)V", remap = false, ordinal = 2))
    private boolean optiFuture$syncAmbientOcclusionSecond(int i) {
        return RenderPass.setAmbientOcclusion(this.mc.gameSettings.ambientOcclusion != 0);
    }

    /**
     * Renders the extra solid render pass (pass 4) right after the vanilla
     * solid pass.
     * <p>
     * 在原版固体 pass 之后立即渲染额外的固体 pass（pass 4）。
     */
    @Redirect(
        method = "renderWorld(FJ)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderGlobal;sortAndRender(Lnet/minecraft/entity/EntityLivingBase;ID)I",
            ordinal = 0))
    private int optiFuture$renderExtraSolidPass(RenderGlobal instance, EntityLivingBase entitylivingbase, int k,
        double i1) {
        int returnValue = instance.sortAndRender(entitylivingbase, k, i1);
        instance.sortAndRender(entitylivingbase, 4, i1);
        return returnValue;
    }

    /**
     * Renders the extra translucent render pass (pass 5) and re-draws
     * rain/snow after the vanilla translucent pass.
     * <p>
     * 在原版半透明 pass 之后渲染额外的半透明 pass（pass 5）并重绘雨雪。
     */
    @Inject(
        method = "renderWorld(FJ)V",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDepthMask(Z)V", ordinal = 3, remap = false))
    private void optiFuture$renderExtraTranslucentPass(float partialTickTime, long p_78471_2_, CallbackInfo ci) {
        this.mc.renderGlobal.sortAndRender(this.mc.renderViewEntity, 5, partialTickTime);
        this.renderRainSnow(partialTickTime);
    }
}

package mist475.mcpatcherforge.mixins.early.gui;

import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.prupe.mcpatcher.gui.CustomPanorama;

/**
 * Hooks the main menu panorama rendering for CustomPanorama (background.properties).
 * 为 CustomPanorama（background.properties）挂钩主菜单全景图渲染。
 * <p>
 * Hook points / 挂钩点：
 * <ul>
 * <li>constructor - weighted random panorama selection / 构造器 - 加权随机选图</li>
 * <li>drawPanorama - texture replacement + blur1 (8x8 sample grid) / 贴图替换 + blur1（8x8 采样网格）</li>
 * <li>rotateAndBlurSkybox - blur2 (layers per pass, vanilla 3) / blur2（单次叠加层数，原版 3）</li>
 * <li>renderSkybox - blur3 (pass count, vanilla 7) / blur3（pass 次数，原版 7）</li>
 * <li>drawScreen - the two gradient overlays / 两个渐变覆盖层</li>
 * </ul>
 */
@Mixin(GuiMainMenu.class)
public abstract class MixinGuiMainMenu {

    @Inject(method = "<init>()V", at = @At("RETURN"))
    private void modifyConstructor(CallbackInfo ci) {
        // One random pick per menu instance, matching vanilla's per-visit feel.
        // 每个菜单实例随机选取一次，与原版“每次进入菜单”的节奏一致。
        CustomPanorama.selectPanorama();
    }

    // blur1: vanilla renders the panorama on an 8x8 jittered sample grid (64 samples).
    // blur1：原版以 8x8 抖动采样网格（64 次采样）绘制全景图。
    @ModifyConstant(method = "drawPanorama(IIF)V", constant = @Constant(intValue = 8))
    private int modifyPanoramaGridSize(int constant) {
        return CustomPanorama.getGridSize(constant);
    }

    @ModifyArg(
        method = "drawPanorama(IIF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V"))
    private ResourceLocation modifyPanoramaTexture(ResourceLocation location) {
        return CustomPanorama.getPanoramaTexture(location);
    }

    // blur2: layers blended within a single rotateAndBlurSkybox pass.
    // blur2：单次 rotateAndBlurSkybox 内叠加的模糊层数。
    @ModifyConstant(method = "rotateAndBlurSkybox(F)V", constant = @Constant(intValue = 3))
    private int modifyBlurLoops(int constant) {
        return CustomPanorama.getBlurLoops(constant);
    }

    @Inject(method = "renderSkybox(IIF)V", at = @At("HEAD"))
    private void modifyRenderSkybox(int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        CustomPanorama.resetBlurPasses();
    }

    // blur3: vanilla 1.7.10 invokes rotateAndBlurSkybox 7 times in a row; no ordinal is
    // given so the condition wraps all of them and the counter skips the excess passes.
    // blur3：1.7.10 原版连续调用 rotateAndBlurSkybox 7 次；不指定 ordinal 使条件包裹全部
    // 调用点，由计数器跳过多余的 pass。
    @WrapWithCondition(
        method = "renderSkybox(IIF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiMainMenu;rotateAndBlurSkybox(F)V"))
    private boolean modifyBlurPasses(GuiMainMenu instance, float partialTick) {
        return CustomPanorama.nextBlurPass();
    }

    // Overlay 1 (vanilla 0x80FFFFFF -> 0x00FFFFFF): skipped entirely when disabled.
    // 覆盖层 1（原版 0x80FFFFFF -> 0x00FFFFFF）：被禁用时整体跳过。
    @WrapOperation(
        method = "drawScreen(IIF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiMainMenu;drawGradientRect(IIIIII)V",
            ordinal = 0))
    private void modifyOverlay1(GuiMainMenu instance, int left, int top, int right, int bottom, int topColor,
        int bottomColor, Operation<Void> original) {
        int[] colors = CustomPanorama.getOverlayColors(1, topColor, bottomColor);
        if (colors != null) {
            original.call(instance, left, top, right, bottom, colors[0], colors[1]);
        }
    }

    // Overlay 2 (vanilla 0x00000000 -> 0x80000000): skipped entirely when disabled.
    // 覆盖层 2（原版 0x00000000 -> 0x80000000）：被禁用时整体跳过。
    @WrapOperation(
        method = "drawScreen(IIF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiMainMenu;drawGradientRect(IIIIII)V",
            ordinal = 1))
    private void modifyOverlay2(GuiMainMenu instance, int left, int top, int right, int bottom, int topColor,
        int bottomColor, Operation<Void> original) {
        int[] colors = CustomPanorama.getOverlayColors(2, topColor, bottomColor);
        if (colors != null) {
            original.call(instance, left, top, right, bottom, colors[0], colors[1]);
        }
    }
}

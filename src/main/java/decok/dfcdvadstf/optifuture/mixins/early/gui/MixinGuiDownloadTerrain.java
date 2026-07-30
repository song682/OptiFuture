package decok.dfcdvadstf.optifuture.mixins.early.gui;

import net.minecraft.client.gui.GuiDownloadTerrain;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.prupe.mcpatcher.gui.CustomLoadingScreens;

/**
 * Hooks the "Downloading terrain" screen background for CustomLoadingScreens
 * (loading.properties). The vanilla dirt background drawn by drawBackground(0)
 * is replaced by the per-dimension texture when one is defined.
 * 为 CustomLoadingScreens（loading.properties）挂钩"下载地形中"屏幕背景。
 * 当对应维度定义了贴图时，替换 drawBackground(0) 绘制的原版泥土背景。
 */
@Mixin(GuiDownloadTerrain.class)
public abstract class MixinGuiDownloadTerrain {

    @WrapOperation(
        method = "drawScreen(IIF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiDownloadTerrain;drawBackground(I)V"))
    private void modifyDrawBackground(GuiDownloadTerrain instance, int textureOffset, Operation<Void> original) {
        // Pass through to the vanilla dirt background when no custom screen matches.
        // 没有匹配的自定义屏幕时原样放行原版泥土背景。
        if (!CustomLoadingScreens.drawBackground(instance.width, instance.height)) {
            original.call(instance, textureOffset);
        }
    }
}

package decok.dfcdvadstf.optifuture.mixins.early.gui;

import net.minecraft.client.gui.GuiScreenWorking;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.prupe.mcpatcher.gui.CustomLoadingScreens;

/**
 * Hooks the generic progress screen ("Saving level", world loading, etc.) background for
 * CustomLoadingScreens (loading.properties). Both variants of drawDefaultBackground -
 * dirt without a world and dark gradient with one - are replaced by the per-dimension
 * texture when one is defined.
 * 为 CustomLoadingScreens（loading.properties）挂钩通用进度屏幕（"保存世界中"、世界加载等）背景。
 * drawDefaultBackground 的两种形态——无世界时的泥土与有世界时的暗色渐变——在对应维度
 * 定义了贴图时均被替换。
 */
@Mixin(GuiScreenWorking.class)
public abstract class MixinGuiScreenWorking {

    @WrapOperation(
        method = "drawScreen(IIF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreenWorking;drawDefaultBackground()V"))
    private void modifyDrawDefaultBackground(GuiScreenWorking instance, Operation<Void> original) {
        // Pass through to the vanilla background when no custom screen matches.
        // 没有匹配的自定义屏幕时原样放行原版背景。
        if (!CustomLoadingScreens.drawBackground(instance.width, instance.height)) {
            original.call(instance);
        }
    }
}

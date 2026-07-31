package decok.dfcdvadstf.optifuture.mixins.early.cc.client.renderer;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.settings.GameSettings;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.prupe.mcpatcher.cc.ColorizeWorld;

/**
 * Custom-colors hooks for the sky/cloud renderer: substitutes a pack-defined End
 * sky color and lets the pack force fancy cloud rendering on or off.
 * <p>
 * 天空/云层渲染的自定义颜色钩子：替换资源包定义的末地天空色，并允许资源包强制开启或关闭高级云渲染。
 */
@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobal {

    @ModifyArg(
        method = "renderSky(F)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Tessellator;setColorOpaque_I(I)V"))
    private int optiFuture$overrideEndSkyColor(int vanillaColor) {
        return ColorizeWorld.endSkyColor;
    }

    @Redirect(
        method = "renderClouds(F)V",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/GameSettings;fancyGraphics:Z"))
    private boolean optiFuture$overrideFancyClouds(GameSettings settings) {
        return ColorizeWorld.drawFancyClouds(settings.fancyGraphics);
    }
}

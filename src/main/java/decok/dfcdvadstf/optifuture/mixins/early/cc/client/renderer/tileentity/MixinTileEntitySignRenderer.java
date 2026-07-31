package decok.dfcdvadstf.optifuture.mixins.early.cc.client.renderer.tileentity;

import net.minecraft.client.renderer.tileentity.TileEntitySignRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.prupe.mcpatcher.cc.ColorizeWorld;

/**
 * Overrides the text color argument passed to the font renderer when drawing
 * signs, letting the pack define the sign text color.
 * <p>
 * 覆盖绘制告示牌时传给字体渲染器的文本颜色参数，使资源包能够定义告示牌文字颜色。
 */
@Mixin(TileEntitySignRenderer.class)
public abstract class MixinTileEntitySignRenderer {

    @ModifyArg(
        method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntitySign;DDDF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I"),
        index = 3)
    private int optiFuture$overrideSignTextColor(int vanillaColor) {
        return ColorizeWorld.colorizeSignText();
    }
}

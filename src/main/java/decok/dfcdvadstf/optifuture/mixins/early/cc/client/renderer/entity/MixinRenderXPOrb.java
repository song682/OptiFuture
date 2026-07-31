package decok.dfcdvadstf.optifuture.mixins.early.cc.client.renderer.entity;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderXPOrb;
import net.minecraft.entity.item.EntityXPOrb;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.prupe.mcpatcher.cc.ColorizeEntity;

/**
 * Recolors experience orbs through the custom-colors engine, feeding the orb's
 * animated color phase so the pack can drive the pulsing tint.
 * <p>
 * 通过自定义颜色引擎重着色经验球，传入经验球的动画颜色相位，使资源包能够驱动其脉动色调。
 */
@Mixin(RenderXPOrb.class)
public abstract class MixinRenderXPOrb {

    @Redirect(
        method = "doRender(Lnet/minecraft/entity/item/EntityXPOrb;DDDFF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Tessellator;setColorRGBA_I(II)V"))
    private void optiFuture$recolorOrb(Tessellator tessellator, int color, int alpha, EntityXPOrb orb, double x,
        double y, double z, float partialTicks, float colorPhase) {
        int tinted = ColorizeEntity.colorizeXPOrb(color, ((float) orb.xpColor + colorPhase) / 2.0F);
        tessellator.setColorRGBA_I(tinted, alpha);
    }
}

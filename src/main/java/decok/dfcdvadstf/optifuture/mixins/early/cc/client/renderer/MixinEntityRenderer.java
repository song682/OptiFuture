package decok.dfcdvadstf.optifuture.mixins.early.cc.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.enchantment.EnchantmentHelper;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.prupe.mcpatcher.cc.ColorizeWorld;
import com.prupe.mcpatcher.cc.Colorizer;
import com.prupe.mcpatcher.cc.Lightmap;

/**
 * Wires the custom-colors engine into world rendering: a custom lightmap can
 * fully replace vanilla lightmap generation, and the underwater / under-lava fog
 * colors are overridden right after vanilla writes the blue fog channel.
 * <p>
 * 将自定义颜色引擎接入世界渲染：自定义光照贴图可完全替换原版光照贴图生成；水下 / 岩浆中的
 * 雾颜色则在原版写入蓝色雾通道之后被覆盖。
 */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @Shadow
    private Minecraft mc;

    @Shadow
    float fogColorRed;

    @Shadow
    float fogColorGreen;

    @Shadow
    float fogColorBlue;

    @Shadow
    @Final
    private DynamicTexture lightmapTexture;

    @Shadow
    @Final
    private int[] lightmapColors;

    @Shadow
    private boolean lightmapUpdateNeeded;

    /** Replace vanilla lightmap with a pack-provided one when available. / 有自定义光照贴图时替换原版。 */
    @Inject(method = "updateLightmap(F)V", at = @At("HEAD"), cancellable = true)
    private void optiFuture$overrideLightmap(float partialTick, CallbackInfo ci) {
        if (Lightmap
            .computeLightmap((EntityRenderer) (Object) this, this.mc.theWorld, this.lightmapColors, partialTick)) {
            this.lightmapTexture.updateDynamicTexture();
            this.lightmapUpdateNeeded = false;
            ci.cancel();
        }
    }

    /** Underwater fog tint; respiration enchantment brightens it slightly. / 水下雾色，水下呼吸附魔略微提亮。 */
    @Inject(
        method = "updateFogColor(F)V",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/EntityRenderer;fogColorBlue:F",
            ordinal = 11,
            shift = At.Shift.AFTER))
    private void optiFuture$overrideUnderwaterFog(float partialTick, CallbackInfo ci) {
        float respirationBonus = (float) EnchantmentHelper.getRespiration(this.mc.renderViewEntity) * 0.2F;
        if (ColorizeWorld.computeUnderwaterColor()) {
            this.fogColorRed = Colorizer.setColor[0] + respirationBonus;
            this.fogColorGreen = Colorizer.setColor[1] + respirationBonus;
            this.fogColorBlue = Colorizer.setColor[2] + respirationBonus;
        }
    }

    /** Under-lava fog tint. / 岩浆中的雾色。 */
    @Inject(
        method = "updateFogColor(F)V",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/EntityRenderer;fogColorBlue:F",
            ordinal = 12,
            shift = At.Shift.AFTER))
    private void optiFuture$overrideUnderlavaFog(float partialTick, CallbackInfo ci) {
        if (ColorizeWorld.computeUnderlavaColor()) {
            this.fogColorRed = Colorizer.setColor[0];
            this.fogColorGreen = Colorizer.setColor[1];
            this.fogColorBlue = Colorizer.setColor[2];
        }
    }
}

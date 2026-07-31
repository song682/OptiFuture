package decok.dfcdvadstf.optifuture.mixins.early.cc.block.material;

import net.minecraft.block.material.MapColor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import decok.dfcdvadstf.optifuture.interfaces.MapColorExpansion;

/**
 * Remembers each {@link MapColor}'s original (vanilla) RGB value at construction
 * time and exposes it through {@link MapColorExpansion}. The custom-colors engine
 * mutates the live palette, so the untouched baseline must be captured before any
 * override is applied.
 * <p>
 * 在构造时记录每个 {@link MapColor} 的原始（原版）RGB 值，并通过 {@link MapColorExpansion}
 * 对外提供。自定义颜色引擎会改写实时调色板，因此必须在任何覆盖生效前保存这份未被修改的基准值。
 */
@Mixin(MapColor.class)
public abstract class MixinMapColor implements MapColorExpansion {

    @Unique
    private int optiFuture$originalColorValue;

    @Inject(method = "<init>(II)V", at = @At("RETURN"))
    private void optiFuture$captureOriginalColor(int colorIndex, int colorValue, CallbackInfo ci) {
        this.setOriginalColorValue(colorValue);
    }

    @Override
    public int getOriginalColorValue() {
        return optiFuture$originalColorValue;
    }

    @Override
    public void setOriginalColorValue(int value) {
        optiFuture$originalColorValue = value;
    }
}

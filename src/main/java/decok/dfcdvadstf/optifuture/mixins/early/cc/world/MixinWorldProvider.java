package decok.dfcdvadstf.optifuture.mixins.early.cc.world;

import net.minecraft.util.Vec3;
import net.minecraft.world.WorldProvider;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.prupe.mcpatcher.cc.ColorizeWorld;
import com.prupe.mcpatcher.cc.Colorizer;

/**
 * Overrides the overworld fog color when a custom fog color is configured. The
 * HEAD injection decides whether the custom color applies and shares that flag;
 * the three constant modifications then substitute the configured RGB channels
 * for the vanilla defaults (0.752, 0.847, 1.0).
 * <p>
 * 当配置了自定义雾颜色时，覆盖主世界的雾颜色。HEAD 注入判断是否应用
 * 自定义颜色并共享该标志；随后三处常量修改将配置的 RGB 分量替换掉
 * 原版默认值（0.752、0.847、1.0）。
 */
@Mixin(WorldProvider.class)
public abstract class MixinWorldProvider {

    @Inject(method = "getFogColor(FF)Lnet/minecraft/util/Vec3;", at = @At("HEAD"))
    private void optiFuture$prepareFogColor(float celestialAngle, float partialTicks,
        CallbackInfoReturnable<Vec3> cir, @Share("computeFogColor") LocalBooleanRef computeFogColor) {
        computeFogColor.set(ColorizeWorld.computeFogColor((WorldProvider) (Object) this, celestialAngle));
    }

    @ModifyConstant(method = "getFogColor(FF)Lnet/minecraft/util/Vec3;", constant = @Constant(floatValue = 0.7529412F))
    private float optiFuture$overrideFogRed(float original, @Share("computeFogColor") LocalBooleanRef computeFogColor) {
        return computeFogColor.get() ? Colorizer.setColor[0] : original;
    }

    @ModifyConstant(method = "getFogColor(FF)Lnet/minecraft/util/Vec3;", constant = @Constant(floatValue = 0.84705883F))
    private float optiFuture$overrideFogGreen(float original,
        @Share("computeFogColor") LocalBooleanRef computeFogColor) {
        return computeFogColor.get() ? Colorizer.setColor[1] : original;
    }

    @ModifyConstant(
        method = "getFogColor(FF)Lnet/minecraft/util/Vec3;",
        constant = @Constant(floatValue = 1.0F, ordinal = 2))
    private float optiFuture$overrideFogBlue(float original,
        @Share("computeFogColor") LocalBooleanRef computeFogColor) {
        return computeFogColor.get() ? Colorizer.setColor[2] : original;
    }
}

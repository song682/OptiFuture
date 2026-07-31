package decok.dfcdvadstf.optifuture.mixins.early.cc.world;

import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.prupe.mcpatcher.cc.ColorizeWorld;
import com.prupe.mcpatcher.cc.Colorizer;

/**
 * Overrides the sky color at the top of the world when a custom sky color is
 * configured. The HEAD injection decides whether the custom color applies and
 * shares that flag; the three {@code STORE} variable modifications then swap in
 * the configured RGB channels for the final vector.
 * <p>
 * 当配置了自定义天空颜色时，覆盖世界上方的天空颜色。HEAD 注入判断
 * 是否应用自定义颜色并共享该标志；随后三处 {@code STORE} 变量修改
 * 将配置的 RGB 分量替换进最终向量。
 */
@Mixin(World.class)
public abstract class MixinWorld {

    @Inject(
        method = "getSkyColorBody(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/util/Vec3;",
        at = @At("HEAD"),
        remap = false)
    private void optiFuture$prepareSkyColor(Entity entity, float partialTicks, CallbackInfoReturnable<Vec3> cir,
        @Share("computeSkyColor") LocalBooleanRef computeSkyColor) {
        computeSkyColor.set(ColorizeWorld.computeSkyColor((World) (Object) this, partialTicks));
    }

    @Inject(
        method = "getSkyColorBody(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/util/Vec3;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/client/ForgeHooksClient;getSkyBlendColour(Lnet/minecraft/world/World;III)I",
            remap = false),
        remap = false)
    private void optiFuture$setupSkyFog(Entity entity, float partialTicks, CallbackInfoReturnable<Vec3> cir) {
        ColorizeWorld.setupForFog(entity);
    }

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @ModifyVariable(
        method = "getSkyColorBody(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/util/Vec3;",
        at = @At(value = "STORE", ordinal = 0),
        ordinal = 3,
        remap = false)
    private float optiFuture$overrideSkyRed(float original, @Share("computeSkyColor") LocalBooleanRef computeSkyColor) {
        return computeSkyColor.get() ? Colorizer.setColor[0] : original;
    }

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @ModifyVariable(
        method = "getSkyColorBody(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/util/Vec3;",
        at = @At(value = "STORE", ordinal = 0),
        ordinal = 4,
        remap = false)
    private float optiFuture$overrideSkyGreen(float original,
        @Share("computeSkyColor") LocalBooleanRef computeSkyColor) {
        return computeSkyColor.get() ? Colorizer.setColor[1] : original;
    }

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @ModifyVariable(
        method = "getSkyColorBody(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/util/Vec3;",
        at = @At(value = "STORE", ordinal = 0),
        ordinal = 5,
        remap = false)
    private float optiFuture$overrideSkyBlue(float original,
        @Share("computeSkyColor") LocalBooleanRef computeSkyColor) {
        return computeSkyColor.get() ? Colorizer.setColor[2] : original;
    }
}

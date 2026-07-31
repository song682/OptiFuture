package decok.dfcdvadstf.optifuture.mixins.early.hd;

import net.minecraft.client.renderer.texture.TextureClock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.prupe.mcpatcher.hd.FancyDial;

/**
 * Hooks the vanilla clock texture into the {@link FancyDial} system so a custom
 * animated clock can take over its rendering. The mixin registers the texture
 * on construction and lets FancyDial cancel the vanilla frame update when it
 * provides its own animation.
 * <p>
 * 将原版时钟材质接入 {@link FancyDial} 系统，使自定义动画时钟能够接管
 * 其渲染。该 mixin 在构造时注册材质，并在 FancyDial 提供自有动画时
 * 取消原版的帧更新。
 */
@Mixin(TextureClock.class)
public abstract class MixinTextureClock {

    /**
     * Registers this clock texture with the FancyDial system after it is built.
     * <p>
     * 在时钟材质构造完成后，将其注册进 FancyDial 系统。
     */
    @Inject(method = "<init>(Ljava/lang/String;)V", at = @At("RETURN"))
    private void optiFuture$registerFancyDial(String iconName, CallbackInfo ci) {
        FancyDial.setup((TextureClock) (Object) this);
    }

    /**
     * Cancels the vanilla clock animation when FancyDial renders a custom frame.
     * <p>
     * 当 FancyDial 渲染自定义帧时，取消原版时钟动画。
     */
    @Inject(
        method = "updateAnimation()V",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/texture/TextureClock;field_94239_h:D",
            ordinal = 1,
            shift = At.Shift.AFTER),
        cancellable = true)
    private void optiFuture$overrideClockAnimation(CallbackInfo ci) {
        if (FancyDial.update((TextureClock) (Object) this, false)) {
            ci.cancel();
        }
    }
}

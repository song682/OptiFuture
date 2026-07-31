package decok.dfcdvadstf.optifuture.mixins.early.hd;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureCompass;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.prupe.mcpatcher.hd.FancyDial;

/**
 * Hooks the vanilla compass texture into the {@link FancyDial} system so a
 * custom animated compass can take over its rendering. A per-instance guard
 * ensures the FancyDial update runs at most once per compass update, since the
 * vanilla method reaches the injection point through several code paths.
 * <p>
 * 将原版指南针材质接入 {@link FancyDial} 系统，使自定义动画指南针能够
 * 接管其渲染。由于原版方法会经多条路径到达注入点，故用一个实例级
 * 标志保证每次指南针更新中 FancyDial 至多执行一次。
 */
@Mixin(TextureCompass.class)
public abstract class MixinTextureCompass extends TextureAtlasSprite {

    @Unique
    private boolean optiFuture$fancyDialUpdated = false;

    public MixinTextureCompass(String iconName) {
        super(iconName);
    }

    /**
     * Registers this compass texture with the FancyDial system after it is
     * built.
     * <p>
     * 在指南针材质构造完成后，将其注册进 FancyDial 系统。
     */
    @Inject(method = "<init>(Ljava/lang/String;)V", at = @At("RETURN"))
    private void optiFuture$registerFancyDial(String iconName, CallbackInfo ci) {
        FancyDial.setup((TextureCompass) (Object) this);
    }

    /**
     * Runs the FancyDial update once per compass update and cancels the vanilla
     * animation when a custom frame is drawn.
     * <p>
     * 每次指南针更新只执行一次 FancyDial 更新，并在绘制自定义帧时取消
     * 原版动画。
     */
    @Inject(
        method = "updateCompass(Lnet/minecraft/world/World;DDDZZ)V",
        at = @At(value = "JUMP", ordinal = 12, shift = At.Shift.BEFORE),
        cancellable = true)
    private void optiFuture$overrideCompassAnimation(World world, double x, double y, double cameraDirection,
        boolean p_94241_8_, boolean itemFrameRenderer, CallbackInfo ci) {
        if (!this.optiFuture$fancyDialUpdated) {
            if (FancyDial.update(this, itemFrameRenderer)) {
                ci.cancel();
            }
            this.optiFuture$fancyDialUpdated = true;
        }
    }

    /**
     * Resets the per-update guard once the compass update returns.
     * <p>
     * 在指南针更新返回后，重置该次更新的标志。
     */
    @Inject(method = "updateCompass(Lnet/minecraft/world/World;DDDZZ)V", at = @At("RETURN"))
    private void optiFuture$resetFancyDialGuard(World world, double x, double y, double cameraDirection,
        boolean p_94241_8_, boolean itemFrameRenderer, CallbackInfo ci) {
        this.optiFuture$fancyDialUpdated = false;
    }
}

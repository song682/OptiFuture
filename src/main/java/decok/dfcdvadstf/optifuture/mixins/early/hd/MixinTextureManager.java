package decok.dfcdvadstf.optifuture.mixins.early.hd;

import net.minecraft.client.renderer.texture.TextureManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.prupe.mcpatcher.hd.CustomAnimation;

/**
 * Drives the custom texture animation system from the vanilla texture manager
 * tick, so mcpatcher-style custom animations advance in step with the game.
 * <p>
 * 借助原版材质管理器的 tick 驱动自定义材质动画系统，使 mcpatcher 风格
 * 的自定义动画随游戏同步推进。
 */
@Mixin(TextureManager.class)
public abstract class MixinTextureManager {

    /**
     * Advances all registered custom animations on each texture manager tick.
     * <p>
     * 在材质管理器每次 tick 时推进所有已注册的自定义动画。
     */
    @Inject(method = "tick()V", at = @At("RETURN"))
    private void optiFuture$tickCustomAnimations(CallbackInfo ci) {
        CustomAnimation.updateAll();
    }
}

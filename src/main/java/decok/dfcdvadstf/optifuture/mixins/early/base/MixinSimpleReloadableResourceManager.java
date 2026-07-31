package decok.dfcdvadstf.optifuture.mixins.early.base;

import net.minecraft.client.resources.SimpleReloadableResourceManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.prupe.mcpatcher.mal.resource.TexturePackChangeHandler;

/**
 * Notifies the texture-pack change handler around resource reloads driven by
 * the resource manager, so mcpatcher subsystems can prepare before listeners
 * run and finalise afterwards.
 * <p>
 * 在资源管理器驱动的资源重载前后通知材质包变更处理器，
 * 使 mcpatcher 子系统能在监听器运行前做好准备、于其后完成收尾。
 */
@Mixin(SimpleReloadableResourceManager.class)
public abstract class MixinSimpleReloadableResourceManager {

    /**
     * Prepares the texture-pack change handler before reload listeners fire.
     * <p>
     * 在重载监听器触发前准备材质包变更处理器。
     */
    @Inject(method = "notifyReloadListeners()V", at = @At("HEAD"))
    private void optiFuture$beforeReload(CallbackInfo ci) {
        TexturePackChangeHandler.beforeChange1();
    }

    /**
     * Finalises the texture-pack change handler after reload listeners finish.
     * <p>
     * 在重载监听器完成后收尾材质包变更处理器。
     */
    @Inject(method = "notifyReloadListeners()V", at = @At("RETURN"))
    private void optiFuture$afterReload(CallbackInfo ci) {
        TexturePackChangeHandler.afterChange1();
    }
}

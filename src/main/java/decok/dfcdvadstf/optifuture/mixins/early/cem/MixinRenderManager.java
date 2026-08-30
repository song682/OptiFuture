package decok.dfcdvadstf.optifuture.mixins.early.cem;

import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.prupe.mcpatcher.cem.CustomEntityModels;

/**
 * Records the entity currently being rendered together with its interpolation
 * factor. {@code func_147939_a} is the single dispatch point every entity render
 * passes through, so the captured state stays valid even for renderers overriding
 * {@code doRender}.
 * <p>
 * 记录当前正在渲染的实体及其插值系数。{@code func_147939_a} 是所有实体渲染必经的
 * 分发点，即使渲染器覆写了 {@code doRender}，捕获的状态依然有效。
 */
@Mixin(RenderManager.class)
public abstract class MixinRenderManager {

    @Inject(
        method = "func_147939_a(Lnet/minecraft/entity/Entity;DDDFFZ)Z",
        at = @At("HEAD"))
    private void optiFuture$captureRenderState(Entity entity, double x, double y, double z, float yaw,
        float partialTick, boolean p_147939_10_, CallbackInfoReturnable<Boolean> cir) {
        CustomEntityModels.setRenderState(entity, partialTick);
    }
}

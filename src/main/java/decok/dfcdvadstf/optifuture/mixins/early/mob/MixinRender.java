package decok.dfcdvadstf.optifuture.mixins.early.mob;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.prupe.mcpatcher.mob.MobRandomizer;

/**
 * Routes every entity texture binding through the random-mob resolver so that
 * eligible entities receive their randomized skin variant.
 * <p>
 * 将每一次实体材质绑定经由随机生物解析器路由，使符合条件的实体
 * 获得其随机化的皮肤变体。
 */
@Mixin(Render.class)
public abstract class MixinRender {

    @Shadow
    protected abstract ResourceLocation getEntityTexture(Entity entity);

    @Redirect(
        method = "bindEntityTexture(Lnet/minecraft/entity/Entity;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/Render;getEntityTexture(Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/ResourceLocation;"))
    private ResourceLocation optiFuture$randomizeEntityTexture(Render renderer, Entity entity) {
        return MobRandomizer.randomTexture(entity, getEntityTexture(entity));
    }
}

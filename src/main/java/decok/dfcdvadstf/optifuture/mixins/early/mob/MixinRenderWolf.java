package decok.dfcdvadstf.optifuture.mixins.early.mob;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderWolf;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.prupe.mcpatcher.mob.MobRandomizer;

/**
 * Randomizes every texture binding in the wolf's render passes so both the wet
 * shaking overlay and the collar align with the randomized body skin.
 * <p>
 * 随机化狼渲染各阶段中的每一次材质绑定，使抖水叠加层与项圈均与
 * 随机化的身体皮肤保持一致。
 */
@Mixin(RenderWolf.class)
public abstract class MixinRenderWolf extends RenderLiving {

    public MixinRenderWolf(ModelBase modelBase, float shadowSize) {
        super(modelBase, shadowSize);
    }

    // No ordinal: vanilla shouldRenderPass binds twice (pass 0 = wet shaking wolf.png,
    // pass 1 = collar); both must go through the randomizer so the shaking overlay
    // matches the randomized body skin.
    // 不限定 ordinal：原版 shouldRenderPass 有两处 bindTexture（pass 0 = 湿身抖动 wolf.png，
    // pass 1 = 项圈），两处都需经过随机化，使抖水叠加层与随机皮肤一致。
    @Redirect(
        method = "shouldRenderPass(Lnet/minecraft/entity/passive/EntityWolf;IF)I",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/RenderWolf;bindTexture(Lnet/minecraft/util/ResourceLocation;)V"))
    private void optiFuture$randomizeWolfTexture(RenderWolf renderer, ResourceLocation texture, EntityWolf entity) {
        this.bindTexture(MobRandomizer.randomTexture(entity, texture));
    }
}

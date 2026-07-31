package decok.dfcdvadstf.optifuture.mixins.early.mob;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderSpider;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.prupe.mcpatcher.mob.MobRandomizer;

/**
 * Randomizes the spider's eyes overlay texture so the glow pass matches the
 * randomized body skin.
 * <p>
 * 随机化蜘蛛的眼睛叠加材质，使发光渲染阶段与随机化的身体皮肤一致。
 */
@Mixin(RenderSpider.class)
public abstract class MixinRenderSpider extends RenderLiving {

    public MixinRenderSpider(ModelBase modelBase, float shadowSize) {
        super(modelBase, shadowSize);
    }

    @Redirect(
        method = "shouldRenderPass(Lnet/minecraft/entity/monster/EntitySpider;IF)I",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/RenderSpider;bindTexture(Lnet/minecraft/util/ResourceLocation;)V"))
    private void optiFuture$randomizeEyesTexture(RenderSpider renderer, ResourceLocation texture, EntitySpider entity) {
        this.bindTexture(MobRandomizer.randomTexture(entity, texture));
    }
}

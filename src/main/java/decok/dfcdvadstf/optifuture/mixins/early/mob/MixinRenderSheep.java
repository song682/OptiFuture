package decok.dfcdvadstf.optifuture.mixins.early.mob;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderSheep;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.prupe.mcpatcher.mob.MobRandomizer;

/**
 * Randomizes the sheep's wool overlay texture so it matches the randomized
 * body skin during the wool render pass.
 * <p>
 * 随机化绵羊的羊毛叠加材质，使其在羊毛渲染阶段与随机化的身体皮肤一致。
 */
@Mixin(RenderSheep.class)
public abstract class MixinRenderSheep extends RenderLiving {

    public MixinRenderSheep(ModelBase modelBase, float shadowSize) {
        super(modelBase, shadowSize);
    }

    @Redirect(
        method = "shouldRenderPass(Lnet/minecraft/entity/passive/EntitySheep;IF)I",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/RenderSheep;bindTexture(Lnet/minecraft/util/ResourceLocation;)V"))
    private void optiFuture$randomizeWoolTexture(RenderSheep renderer, ResourceLocation texture, EntitySheep entity) {
        this.bindTexture(MobRandomizer.randomTexture(entity, texture));
    }
}

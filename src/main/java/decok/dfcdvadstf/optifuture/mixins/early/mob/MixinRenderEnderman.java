package decok.dfcdvadstf.optifuture.mixins.early.mob;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderEnderman;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.prupe.mcpatcher.mob.MobRandomizer;

/**
 * Randomizes the enderman's eyes overlay texture so the glow pass matches the
 * randomized body skin.
 * <p>
 * 随机化末影人的眼睛叠加材质，使发光渲染阶段与随机化的身体皮肤一致。
 */
@Mixin(RenderEnderman.class)
public abstract class MixinRenderEnderman extends RenderLiving {

    public MixinRenderEnderman(ModelBase modelBase, float shadowSize) {
        super(modelBase, shadowSize);
    }

    @Redirect(
        method = "shouldRenderPass(Lnet/minecraft/entity/monster/EntityEnderman;IF)I",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/RenderEnderman;bindTexture(Lnet/minecraft/util/ResourceLocation;)V"))
    private void optiFuture$randomizeEyesTexture(RenderEnderman renderer, ResourceLocation texture,
        EntityEnderman entity) {
        // Must actually bind the (possibly randomized) eyes texture; the redirect swallows
        // the original bindTexture call, so dropping the result would leave the previously
        // bound body texture active for the glow pass.
        // 必须真正绑定（可能被随机化的）眼睛纹理：redirect 吞掉了原版的 bindTexture 调用，
        // 若丢弃返回值，发光 pass 会沿用之前绑定的身体纹理。
        this.bindTexture(MobRandomizer.randomTexture(entity, texture));
    }
}

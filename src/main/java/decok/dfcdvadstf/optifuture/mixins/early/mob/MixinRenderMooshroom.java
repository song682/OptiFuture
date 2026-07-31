package decok.dfcdvadstf.optifuture.mixins.early.mob;

import net.minecraft.block.Block;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderMooshroom;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.prupe.mcpatcher.mob.MobOverlay;

/**
 * Redirects the mooshroom's mushroom-overlay rendering through the custom
 * overlay system: it swaps in the overlay texture, may skip the vanilla
 * mushroom block render when a custom overlay handles it, and tears the overlay
 * state down afterwards.
 * <p>
 * 将哞菇的蘑菇叠加渲染经由自定义叠加系统重定向：替换叠加材质，在
 * 自定义叠加接管时可跳过原版蘑菇方块渲染，并在结束后拆除叠加状态。
 */
@Mixin(RenderMooshroom.class)
public abstract class MixinRenderMooshroom extends RenderLiving {

    public MixinRenderMooshroom(ModelBase modelBase, float shadowSize) {
        super(modelBase, shadowSize);
    }

    @Inject(method = "renderEquippedItems(Lnet/minecraft/entity/passive/EntityMooshroom;F)V", at = @At("RETURN"))
    private void optiFuture$finishMooshroomOverlay(EntityMooshroom entity, float partialTicks, CallbackInfo ci) {
        MobOverlay.finishMooshroom();
    }

    @Redirect(
        method = "renderEquippedItems(Lnet/minecraft/entity/passive/EntityMooshroom;F)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/RenderMooshroom;bindTexture(Lnet/minecraft/util/ResourceLocation;)V"))
    private void optiFuture$bindMooshroomOverlay(RenderMooshroom renderer, ResourceLocation texture,
        EntityMooshroom entity, float partialTicks) {
        this.bindTexture(MobOverlay.setupMooshroom(entity, texture));
    }

    @WrapWithCondition(
        method = "renderEquippedItems(Lnet/minecraft/entity/passive/EntityMooshroom;F)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderBlocks;renderBlockAsItem(Lnet/minecraft/block/Block;IF)V"))
    private boolean optiFuture$skipVanillaMushroom(RenderBlocks renderBlocks, Block block, int meta, float brightness) {
        return !MobOverlay.renderMooshroomOverlay(0.0);
    }
}

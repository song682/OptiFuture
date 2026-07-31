package decok.dfcdvadstf.optifuture.mixins.early.cit.client.renderer.entity;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.prupe.mcpatcher.cit.CITUtils;

/**
 * Redirects the player's armor texture resolution through the CIT resolver so
 * custom armor textures apply in both the base and overlay armor render passes.
 * <p>
 * 将玩家的护甲材质解析经由 CIT 解析器重定向，使自定义护甲材质在基础
 * 与叠加两个护甲渲染阶段均生效。
 */
@Mixin(RenderPlayer.class)
public abstract class MixinRenderPlayer extends RendererLivingEntity {

    public MixinRenderPlayer(ModelBase modelBase, float shadowSize) {
        super(modelBase, shadowSize);
    }

    @Redirect(
        method = "shouldRenderPass(Lnet/minecraft/client/entity/AbstractClientPlayer;IF)I",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/RenderBiped;getArmorResource(Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;ILjava/lang/String;)Lnet/minecraft/util/ResourceLocation;",
            remap = false))
    private ResourceLocation optiFuture$resolveArmorTexture(Entity entity, ItemStack stack, int slot, String type,
        AbstractClientPlayer player) {
        return CITUtils
            .getArmorTexture(RenderBiped.getArmorResource(player, stack, slot, type), (EntityLivingBase) entity, stack);
    }

    @Redirect(
        method = "func_82408_c(Lnet/minecraft/client/entity/AbstractClientPlayer;IF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/RenderBiped;getArmorResource(Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;ILjava/lang/String;)Lnet/minecraft/util/ResourceLocation;",
            remap = false))
    private ResourceLocation optiFuture$resolveArmorOverlayTexture(Entity entity, ItemStack stack, int slot,
        String type, AbstractClientPlayer player) {
        return CITUtils
            .getArmorTexture(RenderBiped.getArmorResource(player, stack, slot, type), (EntityLivingBase) entity, stack);
    }
}

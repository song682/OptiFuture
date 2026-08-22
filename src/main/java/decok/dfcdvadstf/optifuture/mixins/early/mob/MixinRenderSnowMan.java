package decok.dfcdvadstf.optifuture.mixins.early.mob;

import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderSnowMan;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.prupe.mcpatcher.mob.MobOverlay;

/**
 * Suppresses the vanilla pumpkin item render on a snow golem's head whenever a
 * custom snowman overlay texture takes over the head decoration.
 * <p>
 * 当自定义雪人叠加材质接管头部装饰时，抑制原版在雪傀儡头部渲染南瓜
 * 物品的逻辑。
 */
@Mixin(RenderSnowMan.class)
public abstract class MixinRenderSnowMan {

    @WrapWithCondition(
        method = "renderEquippedItems(Lnet/minecraft/entity/monster/EntitySnowman;F)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemRenderer;renderItem(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;I)V"))
    private boolean optiFuture$skipPumpkinWhenOverlaid(ItemRenderer renderer, EntityLivingBase entity,
        ItemStack itemStack, int renderPass) {
        return !MobOverlay.renderSnowmanOverlay(entity);
    }
}

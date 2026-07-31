package decok.dfcdvadstf.optifuture.mixins.early.cit.client.renderer;

import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.prupe.mcpatcher.cit.CITUtils;

/**
 * Applies CIT (Custom Item Textures) overrides to the first-person held item:
 * the icon lookup is redirected through the CIT resolver, and the enchantment
 * glint is suppressed when a CIT enchantment overlay handles it instead.
 * <p>
 * 为第一人称手持物品应用 CIT（自定义物品材质）覆盖：图标查询经由
 * CIT 解析器重定向；当 CIT 附魔覆盖层接管时，抑制原版的附魔光效。
 */
@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {

    @Redirect(
        method = "renderItem(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;ILnet/minecraftforge/client/IItemRenderer$ItemRenderType;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/EntityLivingBase;getItemIcon(Lnet/minecraft/item/ItemStack;I)Lnet/minecraft/util/IIcon;"))
    private IIcon optiFuture$resolveHeldItemIcon(EntityLivingBase holder, ItemStack stack, int renderPass,
        EntityLivingBase entity, ItemStack itemStack, int pass) {
        return CITUtils.getIcon(entity.getItemIcon(itemStack, pass), itemStack, pass);
    }

    @Redirect(
        method = "renderItem(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;ILnet/minecraftforge/client/IItemRenderer$ItemRenderType;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;hasEffect(I)Z"),
        remap = false)
    private boolean optiFuture$suppressHeldGlint(ItemStack stack, int pass, EntityLivingBase entity,
        ItemStack itemStack, int renderPass) {
        return !CITUtils.renderEnchantmentHeld(stack, renderPass) && stack.hasEffect(pass);
    }
}

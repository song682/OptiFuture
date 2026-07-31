package decok.dfcdvadstf.optifuture.mixins.early.cit.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.prupe.mcpatcher.cit.CITUtils;

/**
 * Routes an item's inventory icon lookup through the CIT resolver so custom
 * item textures apply wherever the base icon index is requested.
 * <p>
 * 将物品的物品栏图标查询经由 CIT 解析器路由，使自定义物品材质
 * 在请求基础图标索引的任何地方均可生效。
 */
@Mixin(Item.class)
public abstract class MixinItem {

    @Shadow
    public abstract IIcon getIconFromDamage(int meta);

    @Redirect(
        method = "getIconIndex(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/util/IIcon;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/item/Item;getIconFromDamage(I)Lnet/minecraft/util/IIcon;"))
    private IIcon optiFuture$resolveIconIndex(Item item, int meta, ItemStack itemStack) {
        return CITUtils.getIcon(this.getIconFromDamage(meta), itemStack, 0);
    }
}

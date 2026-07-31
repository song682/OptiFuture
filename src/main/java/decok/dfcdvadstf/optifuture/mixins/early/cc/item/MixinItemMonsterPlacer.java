package decok.dfcdvadstf.optifuture.mixins.early.cc.item;

import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.prupe.mcpatcher.cc.ColorizeItem;

/**
 * Recolors spawn-egg overlay/base spots according to the active color
 * configuration once vanilla has computed its default value.
 * <p>
 * 在原版计算出默认取值之后，依据当前颜色配置重新着色刷怪蛋的
 * 底色与斑点色。
 */
@Mixin(ItemMonsterPlacer.class)
public abstract class MixinItemMonsterPlacer {

    @Inject(method = "getColorFromItemStack(Lnet/minecraft/item/ItemStack;I)I", at = @At("RETURN"), cancellable = true)
    private void optiFuture$recolorSpawnerEgg(ItemStack itemStack, int spots,
        CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(ColorizeItem.colorizeSpawnerEgg(cir.getReturnValue(), itemStack.getItemDamage(), spots));
    }
}

package decok.dfcdvadstf.optifuture.mixins.early.cc.item;

import net.minecraft.item.ItemArmor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.prupe.mcpatcher.cc.ColorizeEntity;

/**
 * Overrides the hard-coded default tint of undyed leather armor
 * (0xA06540) with the value supplied by the color configuration.
 * <p>
 * 将未染色皮革护甲的硬编码默认颜色（0xA06540）替换为
 * 颜色配置所提供的取值。
 */
@Mixin(ItemArmor.class)
public abstract class MixinItemArmor {

    @ModifyConstant(method = "getColor(Lnet/minecraft/item/ItemStack;)I", constant = @Constant(intValue = 10511680))
    private int optiFuture$replaceUndyedLeatherColor(int original) {
        return ColorizeEntity.undyedLeatherColor;
    }
}

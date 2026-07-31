package decok.dfcdvadstf.optifuture.mixins.early.cc.potion;

import net.minecraft.potion.PotionHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.prupe.mcpatcher.cc.ColorizeItem;

/**
 * Replaces the default water-bottle liquid color (0x385DC6) used when a
 * potion has no active effects with the value from the color configuration.
 * <p>
 * 将无任何效果时使用的默认水瓶液体颜色（0x385DC6）替换为
 * 颜色配置中所提供的取值。
 */
@Mixin(PotionHelper.class)
public abstract class MixinPotionHelper {

    @ModifyConstant(method = "calcPotionLiquidColor(Ljava/util/Collection;)I", constant = @Constant(intValue = 3694022))
    private static int optiFuture$replaceWaterBottleColor(int original) {
        return ColorizeItem.getWaterBottleColor();
    }
}

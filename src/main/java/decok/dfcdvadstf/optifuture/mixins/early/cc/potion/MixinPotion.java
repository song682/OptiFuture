package decok.dfcdvadstf.optifuture.mixins.early.cc.potion;

import net.minecraft.potion.Potion;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.prupe.mcpatcher.cc.ColorizeItem;

import decok.dfcdvadstf.optifuture.interfaces.PotionExpansion;

/**
 * Registers each potion for custom coloring once it is named and stores the
 * potion's original liquid color so it can be restored when needed.
 * <p>
 * 在药水完成命名后将其登记以进行自定义着色，并保存药水原始的
 * 液体颜色，以便在需要时恢复。
 */
@Mixin(Potion.class)
public abstract class MixinPotion implements PotionExpansion {

    /** Original liquid color captured before customization. / 自定义前捕获的原始液体颜色。 */
    @Unique
    private int optiFuture$originalColor;

    @Inject(method = "setPotionName(Ljava/lang/String;)Lnet/minecraft/potion/Potion;", at = @At("RETURN"))
    private void optiFuture$registerForColoring(String name, CallbackInfoReturnable<Potion> cir) {
        ColorizeItem.setupPotion((Potion) (Object) this);
    }

    @Override
    public void setOrigColor(int color) {
        this.optiFuture$originalColor = color;
    }

    @Override
    public int getOrigColor() {
        return this.optiFuture$originalColor;
    }
}

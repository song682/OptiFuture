package decok.dfcdvadstf.optifuture.mixins.early.at;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PotionHelper.class)
public class MixinPotionHelperAccess {

    @Shadow
    private static HashMap field_77925_n;

    @SuppressWarnings("unchecked")
    public static Map<Potion, Integer> getPotionColorCache() {
        return (Map<Potion, Integer>) field_77925_n;
    }
}

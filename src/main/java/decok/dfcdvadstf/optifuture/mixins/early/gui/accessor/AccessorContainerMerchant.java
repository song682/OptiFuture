package decok.dfcdvadstf.optifuture.mixins.early.gui.accessor;

import net.minecraft.inventory.ContainerMerchant;
import net.minecraft.entity.IMerchant;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor for the private merchant held by {@link ContainerMerchant},
 * used by CustomGuis to check villager professions.
 * 暴露 {@link ContainerMerchant} 私有的商人对象，供 CustomGuis 判断村民职业。
 */
@Mixin(ContainerMerchant.class)
public interface AccessorContainerMerchant {

    @Accessor("theMerchant")
    IMerchant getMerchant();
}

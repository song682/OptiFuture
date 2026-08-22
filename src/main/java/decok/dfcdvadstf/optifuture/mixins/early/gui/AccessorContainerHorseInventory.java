package decok.dfcdvadstf.optifuture.mixins.early.gui;

import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.inventory.ContainerHorseInventory;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor for the private horse entity held by {@link ContainerHorseInventory},
 * used by CustomGuis to check horse variants.
 * 暴露 {@link ContainerHorseInventory} 私有的马实体，供 CustomGuis 判断马的变体。
 */
@Mixin(ContainerHorseInventory.class)
public interface AccessorContainerHorseInventory {

    @Accessor("theHorse")
    EntityHorse getHorse();
}

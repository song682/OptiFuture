package decok.dfcdvadstf.optifuture.mixins.early.gui;

import net.minecraft.inventory.ContainerFurnace;
import net.minecraft.tileentity.TileEntityFurnace;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor for the private furnace tile entity held by {@link ContainerFurnace},
 * used by CustomGuis to read custom furnace names.
 * 暴露 {@link ContainerFurnace} 私有的熔炉方块实体，供 CustomGuis 读取
 * 熔炉自定义名称。
 */
@Mixin(ContainerFurnace.class)
public interface AccessorContainerFurnace {

    @Accessor("tileFurnace")
    TileEntityFurnace getTileFurnace();
}

package decok.dfcdvadstf.optifuture.mixins.early.gui;

import net.minecraft.inventory.ContainerDispenser;
import net.minecraft.tileentity.TileEntityDispenser;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor for the private dispenser tile entity held by {@link ContainerDispenser},
 * used by CustomGuis for dispenser/dropper variant checks and custom names.
 * 暴露 {@link ContainerDispenser} 私有的发射器方块实体，供 CustomGuis
 * 判断发射器/投掷器变体并读取自定义名称。
 */
@Mixin(ContainerDispenser.class)
public interface AccessorContainerDispenser {

    @Accessor("tileDispenser")
    TileEntityDispenser getTileDispenser();
}

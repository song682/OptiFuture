package decok.dfcdvadstf.optifuture.mixins.early.gui.accessor;

import net.minecraft.inventory.ContainerHopper;
import net.minecraft.inventory.IInventory;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor for the unnamed private hopper inventory held by {@link ContainerHopper}
 * (SRG field_94538_a, no MCP name), used by CustomGuis to read custom hopper names.
 * 暴露 {@link ContainerHopper} 私有的无 MCP 名称漏斗物品栏（SRG field_94538_a），
 * 供 CustomGuis 读取漏斗自定义名称。
 */
@Mixin(ContainerHopper.class)
public interface AccessorContainerHopper {

    @Accessor("field_94538_a")
    IInventory getHopperInventory();
}

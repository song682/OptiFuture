package decok.dfcdvadstf.optifuture.mixins.early.gui.accessor;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryLargeChest;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor for the private upper half inventory held by {@link InventoryLargeChest},
 * used by CustomGuis to resolve the chest tile entity of large chests.
 * 暴露 {@link InventoryLargeChest} 私有的上半部分物品栏，供 CustomGuis
 * 定位大箱子对应的箱子方块实体。
 */
@Mixin(InventoryLargeChest.class)
public interface AccessorInventoryLargeChest {

    @Accessor("upperChest")
    IInventory getUpperChest();
}

package decok.dfcdvadstf.optifuture.mixins.early.gui;

import net.minecraft.inventory.ContainerBeacon;
import net.minecraft.tileentity.TileEntityBeacon;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor for the private beacon tile entity held by {@link ContainerBeacon},
 * used by CustomGuis to read beacon levels and custom names.
 * 暴露 {@link ContainerBeacon} 私有的信标方块实体，供 CustomGuis 读取
 * 信标等级与自定义名称。
 */
@Mixin(ContainerBeacon.class)
public interface AccessorContainerBeacon {

    @Accessor("tileBeacon")
    TileEntityBeacon getTileBeacon();
}

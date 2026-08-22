package decok.dfcdvadstf.optifuture.mixins.early.cc.block.material.accessor;

import net.minecraft.block.material.MapColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MapColor.class)
public interface AccessorMapColor {

    @Accessor("colorValue")
    public int getColorValue();
}

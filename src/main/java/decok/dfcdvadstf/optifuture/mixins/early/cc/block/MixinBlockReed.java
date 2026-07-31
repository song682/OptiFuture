package decok.dfcdvadstf.optifuture.mixins.early.cc.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockReed;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.prupe.mcpatcher.cc.ColorizeBlock;

/**
 * Custom-colors hook for sugar cane. Only the position-aware tint needs
 * overriding so reeds can pick up pack-defined biome colors.
 * <p>
 * 甘蔗的自定义颜色钩子。只需覆盖结合坐标的染色，使甘蔗能采用资源包定义的生物群系颜色。
 */
@Mixin(BlockReed.class)
public abstract class MixinBlockReed {

    @Inject(method = "colorMultiplier(Lnet/minecraft/world/IBlockAccess;III)I", at = @At("HEAD"), cancellable = true)
    private void optiFuture$applyColorMultiplier(IBlockAccess worldIn, int x, int y, int z,
        CallbackInfoReturnable<Integer> cir) {
        if (ColorizeBlock.colorizeBlock((Block) (Object) this, worldIn, x, y, z)) {
            cir.setReturnValue(ColorizeBlock.blockColor);
        }
    }
}

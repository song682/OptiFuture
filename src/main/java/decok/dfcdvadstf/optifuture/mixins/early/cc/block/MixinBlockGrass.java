package decok.dfcdvadstf.optifuture.mixins.early.cc.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockGrass;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.prupe.mcpatcher.cc.ColorizeBlock;

/**
 * Routes grass tinting through the custom-colors engine so grass blocks can be
 * recolored per biome/resource pack instead of using the hard-coded vanilla
 * palette.
 * <p>
 * 将草方块的染色交给自定义颜色引擎处理，使草地能够按生物群系/资源包重新着色，
 * 而不是沿用原版写死的调色板。
 */
@Mixin(BlockGrass.class)
public abstract class MixinBlockGrass {

    @Inject(method = "getBlockColor()I", at = @At("HEAD"), cancellable = true)
    private void optiFuture$applyBlockColor(CallbackInfoReturnable<Integer> cir) {
        if (ColorizeBlock.colorizeBlock((Block) (Object) this)) {
            cir.setReturnValue(ColorizeBlock.blockColor);
        }
    }

    @Inject(method = "getRenderColor(I)I", at = @At("HEAD"), cancellable = true)
    private void optiFuture$applyRenderColor(int meta, CallbackInfoReturnable<Integer> cir) {
        if (ColorizeBlock.colorizeBlock((Block) (Object) this, meta)) {
            cir.setReturnValue(ColorizeBlock.blockColor);
        }
    }

    @Inject(method = "colorMultiplier(Lnet/minecraft/world/IBlockAccess;III)I", at = @At("HEAD"), cancellable = true)
    private void optiFuture$applyColorMultiplier(IBlockAccess worldIn, int x, int y, int z,
        CallbackInfoReturnable<Integer> cir) {
        if (ColorizeBlock.colorizeBlock((Block) (Object) this, worldIn, x, y, z)) {
            cir.setReturnValue(ColorizeBlock.blockColor);
        }
    }
}

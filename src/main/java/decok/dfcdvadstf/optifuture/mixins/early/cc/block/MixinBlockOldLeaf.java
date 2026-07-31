package decok.dfcdvadstf.optifuture.mixins.early.cc.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockOldLeaf;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.prupe.mcpatcher.cc.ColorizeBlock;

/**
 * Custom-colors hook for the classic leaf block. Only the metadata and world
 * tint queries need overriding here; the base palette color is inherited.
 * <p>
 * 经典树叶方块的自定义颜色钩子。此处只需覆盖元数据取色与世界染色，基础调色板颜色沿用父类。
 */
@Mixin(BlockOldLeaf.class)
public abstract class MixinBlockOldLeaf {

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

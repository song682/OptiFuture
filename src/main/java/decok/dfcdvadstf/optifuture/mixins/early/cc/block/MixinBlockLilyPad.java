package decok.dfcdvadstf.optifuture.mixins.early.cc.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLilyPad;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.prupe.mcpatcher.cc.ColorizeBlock;

/**
 * Applies custom colors to lily pads across the inventory, metadata and world
 * tint queries.
 * <p>
 * 为睡莲应用自定义颜色，覆盖物品栏、元数据与世界染色三处取色。
 */
@Mixin(BlockLilyPad.class)
public abstract class MixinBlockLilyPad {

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

package decok.dfcdvadstf.optifuture.mixins.early.cc.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStem;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.prupe.mcpatcher.cc.ColorizeBlock;

/**
 * Custom-colors hook for melon/pumpkin stems, letting packs drive the age-based
 * green-to-brown gradient through both the metadata and world tint queries.
 * <p>
 * 西瓜/南瓜茎的自定义颜色钩子，让资源包接管随生长阶段由绿转褐的渐变，覆盖元数据与世界染色两处取色。
 */
@Mixin(BlockStem.class)
public abstract class MixinBlockStem {

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

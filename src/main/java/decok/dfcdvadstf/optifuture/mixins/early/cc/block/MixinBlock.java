package decok.dfcdvadstf.optifuture.mixins.early.cc.block;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.prupe.mcpatcher.cc.ColorizeBlock;

/**
 * Base custom-colors hook for every {@link Block}. Each of the three vanilla
 * color queries is short-circuited: when the active resource pack defines a
 * custom color for this block the vanilla result is replaced, otherwise the
 * original value is left untouched.
 * <p>
 * 所有方块的自定义颜色基础钩子。拦截原版的三个取色入口——一旦资源包为该方块
 * 提供了自定义颜色便替换返回值，否则保持原版结果不变。
 */
@Mixin(Block.class)
public abstract class MixinBlock {

    /** Palette color used when the block has no world context. / 无世界上下文时的调色板取色。 */
    @Inject(method = "getBlockColor()I", at = @At("HEAD"), cancellable = true)
    private void optiFuture$applyBlockColor(CallbackInfoReturnable<Integer> cir) {
        if (ColorizeBlock.colorizeBlock((Block) (Object) this)) {
            cir.setReturnValue(ColorizeBlock.blockColor);
        }
    }

    /** Inventory / metadata based color. / 基于元数据（物品栏）的取色。 */
    @Inject(method = "getRenderColor(I)I", at = @At("HEAD"), cancellable = true)
    private void optiFuture$applyRenderColor(int meta, CallbackInfoReturnable<Integer> cir) {
        if (ColorizeBlock.colorizeBlock((Block) (Object) this, meta)) {
            cir.setReturnValue(ColorizeBlock.blockColor);
        }
    }

    /** Position-aware world tint. / 结合坐标的世界染色。 */
    @Inject(method = "colorMultiplier(Lnet/minecraft/world/IBlockAccess;III)I", at = @At("HEAD"), cancellable = true)
    private void optiFuture$applyColorMultiplier(IBlockAccess worldIn, int x, int y, int z,
        CallbackInfoReturnable<Integer> cir) {
        if (ColorizeBlock.colorizeBlock((Block) (Object) this, worldIn, x, y, z)) {
            cir.setReturnValue(ColorizeBlock.blockColor);
        }
    }
}

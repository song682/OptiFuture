package decok.dfcdvadstf.optifuture.mixins.early.base;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirt;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.prupe.mcpatcher.mal.block.RenderBlocksUtils;

@Mixin(BlockDirt.class)
public abstract class MixinBlockDirt {

    /** Podzol top icon. / 灰化土顶面图标。 */
    @Shadow
    private IIcon field_150008_b;

    @Inject(
        method = "getIcon(Lnet/minecraft/world/IBlockAccess;IIII)Lnet/minecraft/util/IIcon;",
        at = @At("HEAD"),
        cancellable = true)
    private void modifyGetIcon(IBlockAccess worldIn, int x, int y, int z, int side, CallbackInfoReturnable<IIcon> cir) {
        // Better Grass only applies to podzol (metadata 2), not plain or coarse dirt.
        // Better Grass 仅作用于灰化土（metadata 2），不影响普通泥土。
        if (worldIn.getBlockMetadata(x, y, z) != 2) {
            return;
        }
        final IIcon grassTexture = RenderBlocksUtils
            .getGrassTexture((Block) (Object) this, worldIn, x, y, z, side, this.field_150008_b);
        if (grassTexture != null) {
            cir.setReturnValue(grassTexture);
        }
    }
}

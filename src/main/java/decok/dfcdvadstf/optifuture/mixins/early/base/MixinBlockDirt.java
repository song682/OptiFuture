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

/**
 * Lets the Better Grass feature override the podzol dirt icon so its sides can
 * inherit the surrounding grass texture. Only podzol (metadata 2) is affected;
 * plain and coarse dirt keep their vanilla icons.
 * <p>
 * 让 Better Grass 功能接管灰化土（podzol）的图标，使其侧面能够
 * 继承周围的草方块材质。仅影响灰化土（metadata 2），普通泥土
 * 与粗糙泥土保持原版图标。
 */
@Mixin(BlockDirt.class)
public abstract class MixinBlockDirt {

    /** Podzol top icon. / 灰化土顶面图标。 */
    @Shadow
    private IIcon field_150008_b;

    /**
     * Substitutes the Better Grass icon for podzol sides when one is available.
     * <p>
     * 在可用时为灰化土侧面替换为 Better Grass 图标。
     */
    @Inject(
        method = "getIcon(Lnet/minecraft/world/IBlockAccess;IIII)Lnet/minecraft/util/IIcon;",
        at = @At("HEAD"),
        cancellable = true)
    private void optiFuture$overridePodzolIcon(IBlockAccess worldIn, int x, int y, int z, int side,
        CallbackInfoReturnable<IIcon> cir) {
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

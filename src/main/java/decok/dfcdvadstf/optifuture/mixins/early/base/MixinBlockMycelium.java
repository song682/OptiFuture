package decok.dfcdvadstf.optifuture.mixins.early.base;

import net.minecraft.block.Block;
import net.minecraft.block.BlockMycelium;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.prupe.mcpatcher.mal.block.RenderBlocksUtils;

/**
 * Lets the Better Grass feature override the mycelium block's side icon, so its
 * sides can blend into neighbouring mycelium instead of showing the default
 * dirt-with-fringe texture.
 * <p>
 * 让 Better Grass 功能接管菌丝方块的侧面图标，使其侧面能与相邻
 * 菌丝方块融合，而不是显示默认的带边泥土材质。
 */
@Mixin(BlockMycelium.class)
public abstract class MixinBlockMycelium {

    /** Mycelium top icon. / 菌丝顶面图标。 */
    @Shadow
    private IIcon field_150200_a;

    /**
     * Substitutes the Better Grass icon for mycelium sides when one is
     * available.
     * <p>
     * 在可用时为菌丝方块侧面替换为 Better Grass 图标。
     */
    @Inject(
        method = "getIcon(Lnet/minecraft/world/IBlockAccess;IIII)Lnet/minecraft/util/IIcon;",
        at = @At("HEAD"),
        cancellable = true)
    private void optiFuture$overrideMyceliumIcon(IBlockAccess worldIn, int x, int y, int z, int side,
        CallbackInfoReturnable<IIcon> cir) {
        final IIcon grassTexture = RenderBlocksUtils
            .getGrassTexture((Block) (Object) this, worldIn, x, y, z, side, this.field_150200_a);
        if (grassTexture != null) {
            cir.setReturnValue(grassTexture);
        }
    }
}

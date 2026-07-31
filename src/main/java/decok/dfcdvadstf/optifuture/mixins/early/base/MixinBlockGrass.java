package decok.dfcdvadstf.optifuture.mixins.early.base;

import net.minecraft.block.Block;
import net.minecraft.block.BlockGrass;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.prupe.mcpatcher.mal.block.RenderBlocksUtils;

/**
 * Lets the Better Grass feature override the grass block's side icon and its
 * biome-colored side overlay, so grass sides can blend into neighbouring grass
 * instead of showing the default dirt-with-fringe texture.
 * <p>
 * 让 Better Grass 功能接管草方块的侧面图标及其受群系染色的侧面
 * 覆盖层，使草方块侧面能与相邻草方块融合，而不是显示默认的
 * 带边泥土材质。
 */
@Mixin(BlockGrass.class)
public class MixinBlockGrass {

    @Shadow
    private IIcon field_149991_b;

    /**
     * Substitutes the Better Grass icon for grass sides when one is available.
     * <p>
     * 在可用时为草方块侧面替换为 Better Grass 图标。
     */
    @Inject(
        method = "getIcon(Lnet/minecraft/world/IBlockAccess;IIII)Lnet/minecraft/util/IIcon;",
        at = @At("HEAD"),
        cancellable = true)
    private void optiFuture$overrideGrassIcon(IBlockAccess worldIn, int x, int y, int z, int side,
        CallbackInfoReturnable<IIcon> cir) {
        final IIcon grassTexture = RenderBlocksUtils
            .getGrassTexture((Block) (Object) this, worldIn, x, y, z, side, this.field_149991_b);
        if (grassTexture != null) {
            cir.setReturnValue(grassTexture);
        }
    }

    /**
     * Replaces the biome-colored side overlay in Better Grass multilayer mode.
     * 在 Better Grass 多层模式下替换受群系染色的侧面覆盖层。
     */
    @Inject(method = "getIconSideOverlay()Lnet/minecraft/util/IIcon;", at = @At("HEAD"), cancellable = true)
    private static void optiFuture$overrideSideOverlay(CallbackInfoReturnable<IIcon> cir) {
        final IIcon overlayTexture = RenderBlocksUtils.getGrassSideOverlayTexture();
        if (overlayTexture != null) {
            cir.setReturnValue(overlayTexture);
        }
    }
}

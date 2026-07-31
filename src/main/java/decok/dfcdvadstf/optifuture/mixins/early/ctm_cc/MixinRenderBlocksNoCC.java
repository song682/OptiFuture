package decok.dfcdvadstf.optifuture.mixins.early.ctm_cc;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Redirects fluid icon lookups to the connected-textures aware variant when the
 * connected-textures feature is enabled but custom colors is not, so fluids can
 * still use per-position icons without the colour tinting path.
 * <p>
 * 当连接材质启用而自定义颜色未启用时，将流体图标查找重定向
 * 到感知连接材质的变体，使流体仍能使用逐位置图标，而不经过
 * 颜色染色路径。
 */
@Mixin(RenderBlocks.class)
public abstract class MixinRenderBlocksNoCC {

    @Shadow
    public IBlockAccess blockAccess;

    @Shadow
    public abstract IIcon getBlockIcon(Block block, IBlockAccess access, int x, int y, int z, int side);

    @Shadow
    public abstract IIcon getBlockIconFromSideAndMetadata(Block block, int side, int meta);

    @Redirect(
        method = "renderBlockLiquid(Lnet/minecraft/block/Block;III)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderBlocks;getBlockIconFromSideAndMetadata(Lnet/minecraft/block/Block;II)Lnet/minecraft/util/IIcon;",
            ordinal = 0))
    private IIcon optiFuture$redirectToGetBlockIcon(RenderBlocks instance, Block block, int side, int meta,
        Block specializedBlock, int x, int y, int z) {
        return (this.blockAccess == null) ? this.getBlockIconFromSideAndMetadata(block, side, meta)
            : this.getBlockIcon(block, this.blockAccess, x, y, z, side);
    }

    @Redirect(
        method = "renderBlockLiquid(Lnet/minecraft/block/Block;III)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderBlocks;getBlockIconFromSideAndMetadata(Lnet/minecraft/block/Block;II)Lnet/minecraft/util/IIcon;",
            ordinal = 2))
    private IIcon optiFuture$saveSideAndRedirectToGetBlockIcon(RenderBlocks instance, Block block, int side,
        int meta, Block specializedBlock, int x, int y, int z) {
        return (this.blockAccess == null) ? this.getBlockIconFromSideAndMetadata(block, side, meta)
            : this.getBlockIcon(block, this.blockAccess, x, y, z, side);
    }
}

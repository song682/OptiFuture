package decok.dfcdvadstf.optifuture.mixins.early.renderpass;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.prupe.mcpatcher.renderpass.RenderPass;

/**
 * Routes the block face-culling test through the custom render-pass system for
 * every standard block rendering path, so blocks whose render pass was remapped
 * are culled consistently with their reassigned pass.
 * <p>
 * 将标准方块渲染路径中的方块面剪除判定经由自定义渲染 pass
 * 系统处理，使被重映射过渲染 pass 的方块能与其重新分配的
 * pass 保持一致的剪除行为。
 */
@Mixin(RenderBlocks.class)
public abstract class MixinRenderBlocks {

    @Shadow
    public IBlockAccess blockAccess;

    /**
     * Replaces the vanilla side-visibility test with the render-pass aware one.
     * <p>
     * 用感知渲染 pass 的判定替换原版的侧面可见性测试。
     */
    @Redirect(
        method = { "renderBlockBed(Lnet/minecraft/block/Block;III)Z",
            "renderStandardBlockWithAmbientOcclusion(Lnet/minecraft/block/Block;IIIFFF)Z",
            "renderStandardBlockWithColorMultiplier(Lnet/minecraft/block/Block;IIIFFF)Z",
            "renderStandardBlockWithAmbientOcclusionPartial(Lnet/minecraft/block/Block;IIIFFF)Z",
            "renderBlockCactusImpl(Lnet/minecraft/block/Block;IIIFFF)Z",
            "renderBlockLiquid(Lnet/minecraft/block/Block;III)Z" },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;shouldSideBeRendered(Lnet/minecraft/world/IBlockAccess;IIII)Z"))
    private boolean optiFuture$shouldSideBeRendered(Block block, IBlockAccess worldIn, int x, int y, int z, int side) {
        return RenderPass.shouldSideBeRendered(block, worldIn, x, y, z, side);
    }
}

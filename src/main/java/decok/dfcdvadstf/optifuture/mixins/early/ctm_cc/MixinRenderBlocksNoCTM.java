package decok.dfcdvadstf.optifuture.mixins.early.ctm_cc;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.prupe.mcpatcher.cc.ColorizeBlock;

/**
 * Applies custom-colors liquid tinting to fluid rendering when the custom
 * colors feature is enabled but connected-textures is not: it captures the
 * block colour multiplier and applies per-channel smoothing/tinting to the
 * fluid faces while keeping the vanilla icon lookup.
 * <p>
 * 当自定义颜色启用而连接材质未启用时，为流体渲染应用自定义
 * 颜色的液体染色：捕获方块颜色乘数，并在保持原版图标查找的
 * 同时对流体面逐通道进行平滑/染色。
 */
@Mixin(RenderBlocks.class)
public abstract class MixinRenderBlocksNoCTM {

    @Shadow
    public IBlockAccess blockAccess;

    @Shadow
    public boolean enableAO;

    @Shadow
    public abstract IIcon getBlockIconFromSideAndMetadata(Block block, int side, int meta);

    // Capture the block color multiplier at HEAD: the former capture inside the top-face
    // icon call left the @Share refs at 0 whenever the top face was culled, blacking out
    // the bottom face. Channel mapping matches vanilla: red = >>16, green = >>8, blue = &255.
    // 在 HEAD 处捕获方块颜色乘数：旧实现在顶面图标调用内捕获，顶面被剔除时
    // @Share 引用保持 0，导致底面全黑。通道映射与原版一致：red = >>16、green = >>8、blue = &255。
    @Inject(method = "renderBlockLiquid(Lnet/minecraft/block/Block;III)Z", at = @At("HEAD"))
    private void optiFuture$captureColorMultiplier(Block block, int x, int y, int z,
        CallbackInfoReturnable<Boolean> cir, @Share("red") LocalFloatRef red, @Share("green") LocalFloatRef green,
        @Share("blue") LocalFloatRef blue) {
        int l = block.colorMultiplier(this.blockAccess, x, y, z);
        red.set((float) (l >> 16 & 255) / 255.0F);
        green.set((float) (l >> 8 & 255) / 255.0F);
        blue.set((float) (l & 255) / 255.0F);
    }

    // Capture needed value
    @Redirect(
        method = "renderBlockLiquid(Lnet/minecraft/block/Block;III)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderBlocks;getBlockIconFromSideAndMetadata(Lnet/minecraft/block/Block;II)Lnet/minecraft/util/IIcon;",
            ordinal = 2))
    private IIcon optiFuture$saveSideAndRedirectToGetBlockIcon(RenderBlocks instance, Block block, int side,
        int meta, Block specializedBlock, int x, int y, int z, @Share("requiredSide") LocalIntRef requiredSide) {
        requiredSide.set(side);
        return this.getBlockIconFromSideAndMetadata(block, side, meta);
    }

    @Redirect(
        method = "renderBlockLiquid(Lnet/minecraft/block/Block;III)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Tessellator;setColorOpaque_F(FFF)V",
            ordinal = 2))
    private void optiFuture$redirectColor10(Tessellator tessellator, float red, float green, float blue,
        Block block, int x, int y, int z, @Share("requiredSide") LocalIntRef requiredSide) {
        if (!(ColorizeBlock.isSmooth = ColorizeBlock.setupBlockSmoothing(
            (RenderBlocks) (Object) this,
            block,
            this.blockAccess,
            x,
            y,
            z,
            requiredSide.get() + 6))) {
            tessellator.setColorOpaque_F(red, green, blue);
        }
    }

    @Redirect(
        method = "renderBlockLiquid(Lnet/minecraft/block/Block;III)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Tessellator;setColorOpaque_F(FFF)V",
            ordinal = 1))
    private void optiFuture$redirectColor9(Tessellator tessellator, float red, float green, float blue, Block block,
        int x, int y, int z, @Share("red") LocalFloatRef redLocal, @Share("green") LocalFloatRef greenLocal,
        @Share("blue") LocalFloatRef blueLocal) {
        if (!(ColorizeBlock.isSmooth = ColorizeBlock
            .setupBlockSmoothing((RenderBlocks) (Object) this, block, this.blockAccess, x, y, z, 6))) {
            // Each channel must use its own captured multiplier (green previously used blue).
            // 每个通道必须乘各自捕获的乘数（旧实现的绿通道误用了蓝通道）。
            tessellator.setColorOpaque_F(red * redLocal.get(), green * greenLocal.get(), blue * blueLocal.get());
        }
        if (ColorizeBlock.isSmooth) {
            this.enableAO = true;
        }
    }
}

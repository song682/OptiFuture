package decok.dfcdvadstf.optifuture.mixins.early.cc.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.prupe.mcpatcher.cc.ColorizeBlock;
import com.prupe.mcpatcher.cc.Colorizer;

/**
 * Custom-colors hook for redstone wire. Two aspects are recolored: the wire's
 * rendered tint (blended by power level) and the color of the dust particles it
 * emits during its random display tick.
 * <p>
 * 红石线的自定义颜色钩子。重着色两处：线材本身按信号强度混合的渲染色，以及它在随机显示刻中
 * 溅出的红石粉尘粒子的颜色。
 */
@Mixin(BlockRedstoneWire.class)
public abstract class MixinBlockRedstoneWire {

    /**
     * Prefer an explicit per-block color if the pack defines one, otherwise fall
     * back to the power-blended redstone tint.
     * <p>
     * 若资源包为该方块提供了明确颜色则优先采用，否则退回按信号强度混合的红石色。
     */
    @ModifyReturnValue(method = "colorMultiplier(Lnet/minecraft/world/IBlockAccess;III)I", at = @At("RETURN"))
    public int optiFuture$recolorWire(int vanillaColor, IBlockAccess worldIn, int x, int y, int z) {
        if (ColorizeBlock.colorizeBlock((Block) (Object) this, worldIn, x, y, z)) {
            return ColorizeBlock.blockColor;
        }
        return ColorizeBlock.colorizeRedstoneWire(worldIn, x, y, z, vanillaColor);
    }

    /**
     * Tint the redstone dust particle to match the wire color at that metadata.
     * The spawnParticle RGB args (indices 4-6) are overwritten in place; negative
     * green/blue components are clamped to zero to stay within valid range.
     * <p>
     * 将红石粉尘粒子染成与该元数据下线材一致的颜色。就地覆写 spawnParticle 的 RGB 参数
     * （索引 4-6）；绿/蓝分量若为负则夹取到 0 以保持合法范围。
     */
    @ModifyArgs(
        method = "randomDisplayTick(Lnet/minecraft/world/World;IIILjava/util/Random;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnParticle(Ljava/lang/String;DDDDDD)V"))
    private void optiFuture$recolorDustParticle(Args args, World worldIn, int x, int y, int z, Random random) {
        if (!ColorizeBlock.computeRedstoneWireColor(worldIn.getBlockMetadata(x, y, z))) {
            return;
        }
        double red = Colorizer.setColor[0];
        double green = Math.max(0.0d, Colorizer.setColor[1]);
        double blue = Math.max(0.0d, Colorizer.setColor[2]);
        args.set(4, red);
        args.set(5, green);
        args.set(6, blue);
    }
}

package decok.dfcdvadstf.optifuture.mixins.early.cc.world;

import net.minecraft.util.Vec3;
import net.minecraft.world.WorldProviderEnd;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.prupe.mcpatcher.cc.ColorizeWorld;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Supplies the End fog color from the color configuration, replacing the fixed
 * purple tone that vanilla returns for this dimension.
 * <p>
 * 从颜色配置中提供末地的雾颜色，替换原版为该维度返回的固定紫色调。
 */
@Mixin(WorldProviderEnd.class)
public abstract class MixinWorldProviderEnd {

    /**
     * @author OptiFutureOptimized
     * @reason The End fog color is a constant vector, so the whole getter is
     *         replaced to source its channels from the color configuration.
     */
    @SideOnly(Side.CLIENT)
    @Overwrite
    public Vec3 getFogColor(float celestialAngle, float partialTicks) {
        return Vec3.createVectorHelper(
            ColorizeWorld.endFogColor[0],
            ColorizeWorld.endFogColor[1],
            ColorizeWorld.endFogColor[2]);
    }
}

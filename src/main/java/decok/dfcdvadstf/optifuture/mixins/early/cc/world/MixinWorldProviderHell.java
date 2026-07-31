package decok.dfcdvadstf.optifuture.mixins.early.cc.world;

import net.minecraft.util.Vec3;
import net.minecraft.world.WorldProviderHell;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.prupe.mcpatcher.cc.ColorizeWorld;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Supplies the Nether fog color from the color configuration, replacing the
 * fixed reddish tone that vanilla returns for this dimension.
 * <p>
 * 从颜色配置中提供下界的雾颜色，替换原版为该维度返回的固定红色调。
 */
@Mixin(WorldProviderHell.class)
public abstract class MixinWorldProviderHell {

    /**
     * @author OptiFutureOptimized
     * @reason The Nether fog color is a constant vector, so the whole getter is
     *         replaced to source its channels from the color configuration.
     */
    @SideOnly(Side.CLIENT)
    @Overwrite
    public Vec3 getFogColor(float celestialAngle, float partialTicks) {
        return Vec3.createVectorHelper(
            ColorizeWorld.netherFogColor[0],
            ColorizeWorld.netherFogColor[1],
            ColorizeWorld.netherFogColor[2]);
    }
}

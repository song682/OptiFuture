package decok.dfcdvadstf.optifuture.mixins.early.cc.client.particle;

import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.particle.EntityReddustFX;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.prupe.mcpatcher.cc.ColorizeBlock;
import com.prupe.mcpatcher.cc.Colorizer;

/**
 * Overrides the base redstone dust particle color with a pack-defined redstone
 * tint before the vanilla per-particle brightness jitter is applied.
 * <p>
 * 在原版对每个粒子施加亮度抖动之前，用资源包定义的红石色覆盖红石粉尘粒子的基础颜色。
 */
@Mixin(EntityReddustFX.class)
public abstract class MixinEntityRedDustFX extends EntityFX {

    protected MixinEntityRedDustFX(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/World;DDDFFFF)V", at = @At("RETURN"))
    private void optiFuture$tintRedDust(World world, double x, double y, double z, float p_i1224_8_, float red,
        float green, float blue, CallbackInfo ci) {
        // == 1.0f is needed as this runs after the rest of the constructor
        if (red == 0.0F || red == 1.0f) {
            red = 1.0F;
            // Injected block
            if (ColorizeBlock.computeRedstoneWireColor(15)) {
                red = Colorizer.setColor[0];
                green = Colorizer.setColor[1];
                blue = Colorizer.setColor[2];
            }
        }

        float f4 = (float) Math.random() * 0.4F + 0.6F;
        this.particleRed = ((float) (Math.random() * 0.20000000298023224D) + 0.8F) * red * f4;
        this.particleGreen = ((float) (Math.random() * 0.20000000298023224D) + 0.8F) * green * f4;
        this.particleBlue = ((float) (Math.random() * 0.20000000298023224D) + 0.8F) * blue * f4;
        // Only the color channels are recomputed here. Scale/maxAge/noClip were already set
        // by the vanilla constructor; repeating the scale multiplications at RETURN would
        // apply 0.75F and the scale parameter twice.
        // 这里只重新计算颜色通道。scale/maxAge/noClip 原版构造器已经设置完毕；
        // 在 RETURN 处重跑缩放乘算会把 0.75F 和 scale 参数乘两次。
    }
}

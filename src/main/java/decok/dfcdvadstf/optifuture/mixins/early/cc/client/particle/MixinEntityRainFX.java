package decok.dfcdvadstf.optifuture.mixins.early.cc.client.particle;

import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.particle.EntityRainFX;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.prupe.mcpatcher.cc.ColorizeBlock;
import com.prupe.mcpatcher.cc.Colorizer;

/**
 * Colors falling rain splash particles with the biome water color, defaulting to
 * the vanilla bluish tint (0.2, 0.3, 1.0) when no custom color is defined.
 * <p>
 * 用群系水色为下落的雨滴溅射粒子着色；未定义自定义颜色时回退到原版偏蓝色调 (0.2, 0.3, 1.0)。
 */
@Mixin(EntityRainFX.class)
public abstract class MixinEntityRainFX extends EntityFX {

    protected MixinEntityRainFX(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/World;DDD)V", at = @At("RETURN"))
    private void optiFuture$tintRain(World world, double x, double y, double z, CallbackInfo ci) {
        if (ColorizeBlock.computeWaterColor(false, (int) this.posX, (int) this.posY, (int) this.posZ)) {
            this.particleRed = Colorizer.setColor[0];
            this.particleGreen = Colorizer.setColor[1];
            this.particleBlue = Colorizer.setColor[2];
        } else {
            this.particleRed = 0.2f;
            this.particleGreen = 0.3f;
            this.particleBlue = 1.0f;
        }
    }
}

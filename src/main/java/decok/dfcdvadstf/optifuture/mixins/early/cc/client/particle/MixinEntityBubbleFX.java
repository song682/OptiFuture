package decok.dfcdvadstf.optifuture.mixins.early.cc.client.particle;

import net.minecraft.client.particle.EntityBubbleFX;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.prupe.mcpatcher.cc.ColorizeBlock;
import com.prupe.mcpatcher.cc.Colorizer;

/**
 * Tints underwater bubble particles with the biome water color once the particle
 * has finished vanilla construction, falling back to plain white when no custom
 * color applies.
 * <p>
 * 在气泡粒子完成原版构造后，用群系水色为其着色；若无自定义颜色则回退为纯白。
 */
@Mixin(EntityBubbleFX.class)
public abstract class MixinEntityBubbleFX extends EntityFX {

    protected MixinEntityBubbleFX(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/World;DDDDDD)V", at = @At("RETURN"))
    private void optiFuture$tintBubble(World world, double x, double y, double z, double motionX, double motionY,
        double motionZ, CallbackInfo ci) {
        if (ColorizeBlock.computeWaterColor(false, (int) this.posX, (int) this.posY, (int) this.posZ)) {
            this.particleRed = Colorizer.setColor[0];
            this.particleGreen = Colorizer.setColor[1];
            this.particleBlue = Colorizer.setColor[2];
        } else {
            this.particleRed = 1.0f;
            this.particleGreen = 1.0f;
            this.particleBlue = 1.0f;
        }
    }
}

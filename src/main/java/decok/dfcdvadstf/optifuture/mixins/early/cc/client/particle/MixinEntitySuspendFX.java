package decok.dfcdvadstf.optifuture.mixins.early.cc.client.particle;

import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.particle.EntitySuspendFX;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.prupe.mcpatcher.cc.ColorizeEntity;
import com.prupe.mcpatcher.cc.Colorizer;

/**
 * Recolors underwater "suspended" ambient particles. The compute call seeds the
 * shared color buffer from the vanilla default (0x666672) adjusted for position,
 * and the result is copied onto the particle unconditionally.
 * <p>
 * 重着色水下悬浮的环境粒子。计算调用会以原版默认色 (0x666672) 结合坐标为共享色缓冲赋值，
 * 随后无条件地将结果拷贝到该粒子上。
 */
@Mixin(EntitySuspendFX.class)
public abstract class MixinEntitySuspendFX extends EntityFX {

    protected MixinEntitySuspendFX(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/World;DDDDDD)V", at = @At("RETURN"))
    private void optiFuture$tintSuspend(World world, double x, double y, double z, double motionX, double motionY,
        double motionZ, CallbackInfo ci) {
        ColorizeEntity.computeSuspendColor(6710962, (int) x, (int) y, (int) z);
        this.particleRed = Colorizer.setColor[0];
        this.particleGreen = Colorizer.setColor[1];
        this.particleBlue = Colorizer.setColor[2];
    }
}

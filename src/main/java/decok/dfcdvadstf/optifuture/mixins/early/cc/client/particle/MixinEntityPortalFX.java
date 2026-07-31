package decok.dfcdvadstf.optifuture.mixins.early.cc.client.particle;

import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.particle.EntityPortalFX;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.prupe.mcpatcher.cc.ColorizeEntity;

/**
 * Applies a pack-defined portal color to portal particles by scaling the custom
 * RGB triple with the particle's own random brightness factor.
 * <p>
 * 通过将自定义 RGB 三元组乘以粒子自身的随机亮度因子，为传送门粒子应用资源包定义的颜色。
 */
@Mixin(EntityPortalFX.class)
public abstract class MixinEntityPortalFX extends EntityFX {

    protected MixinEntityPortalFX(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/World;DDDDDD)V", at = @At("RETURN"))
    private void optiFuture$tintPortal(World world, double x, double y, double z, double motionX, double motionY,
        double motionZ, CallbackInfo ci) {
        // Vanilla: red = green = blue = f, then green *= 0.3F and red *= 0.9F. Blue is left
        // untouched, so at RETURN it still holds the random brightness factor f. Recover f
        // from particleBlue (portalParticleScale is an unrelated random value).
        // 原版：red = green = blue = f，随后 green *= 0.3F、red *= 0.9F。blue 未被乘系数，
        // 因此在 RETURN 时仍保存着随机亮度因子 f；应从 particleBlue 取回 f
        // （portalParticleScale 是另一个无关的随机值）。
        float f = this.particleBlue;
        this.particleRed = f * ColorizeEntity.portalColor[0];
        this.particleGreen = f * ColorizeEntity.portalColor[1];
        this.particleBlue = f * ColorizeEntity.portalColor[2];
    }
}

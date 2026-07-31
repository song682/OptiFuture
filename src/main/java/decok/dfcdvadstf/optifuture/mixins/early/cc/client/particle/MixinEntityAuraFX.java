package decok.dfcdvadstf.optifuture.mixins.early.cc.client.particle;

import net.minecraft.client.particle.EntityAuraFX;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;

import com.prupe.mcpatcher.cc.ColorizeEntity;
import com.prupe.mcpatcher.cc.Colorizer;

import decok.dfcdvadstf.optifuture.interfaces.EntityAuraFXExpansion;

/**
 * Lets mycelium "town aura" particles adopt a pack-defined color. The tint is
 * applied lazily through {@link EntityAuraFXExpansion#colorize()} rather than in
 * the constructor, matching how the particle is post-processed at spawn time.
 * <p>
 * 让菌丝的"城镇光环"粒子采用资源包定义的颜色。着色通过 {@link EntityAuraFXExpansion#colorize()}
 * 延迟施加，而非在构造器内完成，与粒子在生成时被后处理的方式一致。
 */
@Mixin(EntityAuraFX.class)
public abstract class MixinEntityAuraFX extends EntityFX implements EntityAuraFXExpansion {

    protected MixinEntityAuraFX(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public EntityAuraFX colorize() {
        if (ColorizeEntity.computeMyceliumParticleColor()) {
            this.particleRed = Colorizer.setColor[0];
            this.particleGreen = Colorizer.setColor[1];
            this.particleBlue = Colorizer.setColor[2];
        }
        return (EntityAuraFX) (Object) this;
    }
}

package decok.dfcdvadstf.optifuture.mixins.early.at;

import net.minecraft.client.renderer.texture.TextureClock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TextureClock.class)
public interface AccessorTextureClock {

    @Accessor("field_94239_h")
    double getCurrentAngle();
}

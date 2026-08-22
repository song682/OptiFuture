package decok.dfcdvadstf.optifuture.mixins.early.at;

import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EntityRenderer.class)
public interface AccessorEntityRenderer {

    @Accessor("torchFlickerX")
    float getTorchFlickerX();

    @Invoker("getNightVisionBrightness")
    float callNightVisionBrightness(EntityPlayer player, float factor);
}

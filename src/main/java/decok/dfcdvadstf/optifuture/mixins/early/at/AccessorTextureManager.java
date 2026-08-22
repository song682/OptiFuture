package decok.dfcdvadstf.optifuture.mixins.early.at;

import java.util.Map;

import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TextureManager.class)
public interface AccessorTextureManager {

    @Accessor("mapTextureObjects")
    Map<ResourceLocation, ITextureObject> getMapTextureObjects();
}

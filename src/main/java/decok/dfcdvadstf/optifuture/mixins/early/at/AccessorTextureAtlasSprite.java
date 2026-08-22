package decok.dfcdvadstf.optifuture.mixins.early.at;

import java.util.List;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TextureAtlasSprite.class)
public interface AccessorTextureAtlasSprite {

    @Accessor("framesTextureData")
    List<int[][]> getFramesTextureData();

    @Accessor("framesTextureData")
    void setFramesTextureData(List<int[][]> framesTextureData);
}

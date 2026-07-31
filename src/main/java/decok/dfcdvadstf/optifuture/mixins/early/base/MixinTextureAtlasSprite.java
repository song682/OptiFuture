package decok.dfcdvadstf.optifuture.mixins.early.base;

import java.util.List;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Hardens {@link TextureAtlasSprite#getFrameCount()} against uninitialised
 * animation data: when the frame list has not been populated yet, it reports a
 * single frame instead of dereferencing a null list.
 * <p>
 * 为 {@link TextureAtlasSprite#getFrameCount()} 增加对未初始化动画数据的
 * 保护：当帧列表尚未填充时，返回单帧，而不是对 null 列表解引用。
 */
@Mixin(TextureAtlasSprite.class)
public abstract class MixinTextureAtlasSprite {

    // In 1.6 this is a List<int[]>
    @Shadow
    public List<int[][]> framesTextureData;

    /**
     * @author OptiFutureOptimized
     * @reason Guard the frame count against a null frame-data list.
     */
    @Overwrite
    public int getFrameCount() {
        if (this.framesTextureData != null) {
            return this.framesTextureData.size();
        }
        return 1;
    }
}

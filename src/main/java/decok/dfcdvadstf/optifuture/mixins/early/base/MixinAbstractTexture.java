package decok.dfcdvadstf.optifuture.mixins.early.base;

import net.minecraft.client.renderer.texture.AbstractTexture;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import decok.dfcdvadstf.optifuture.interfaces.AbstractTextureExpansion;

/**
 * Adds the ability to free a texture's GL handle on demand, exposed through
 * {@link AbstractTextureExpansion} so other subsystems can release textures
 * (e.g. when a resource pack changes).
 * <p>
 * 为材质增加按需释放其 GL 句柄的能力，并通过
 * {@link AbstractTextureExpansion} 暴露，供其它子系统在需要时释放材质
 * （例如资源包切换时）。
 */
@Mixin(AbstractTexture.class)
public abstract class MixinAbstractTexture implements AbstractTextureExpansion {

    @Shadow
    protected int glTextureId;

    /**
     * Deletes the backing GL texture and marks the handle as unallocated.
     * <p>
     * 删除底层 GL 材质并将句柄标记为未分配。
     */
    @Override
    public void unloadGLTexture() {
        if (this.glTextureId >= 0) {
            GL11.glDeleteTextures(this.glTextureId);
            this.glTextureId = -1;
        }
    }

}

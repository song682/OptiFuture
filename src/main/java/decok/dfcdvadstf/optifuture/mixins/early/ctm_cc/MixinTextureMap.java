package decok.dfcdvadstf.optifuture.mixins.early.ctm_cc;

import java.util.Map;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.prupe.mcpatcher.mal.tile.TileLoader;

/**
 * Hooks the texture atlas so the custom tile loader can register its own
 * sprites, override sprite resource paths, and mark special (loader-managed)
 * textures during icon registration.
 * <p>
 * 钩入材质图集，使自定义方块加载器能注册自己的精灵图、
 * 覆盖精灵资源路径，并在图标注册时标记特殊（由加载器管理的）材质。
 */
@Mixin(TextureMap.class)
public abstract class MixinTextureMap extends AbstractTexture {

    @Shadow
    @Final
    private Map<String, TextureAtlasSprite> mapRegisteredSprites;

    @Shadow
    @Final
    private String basePath;

    @Shadow
    protected abstract void registerIcons();

    /**
     * Registers the custom tile loader's sprites after the atlas is cleared.
     * <p>
     * 在图集被清空后注册自定义方块加载器的精灵图。
     */
    @Inject(
        method = "loadTextureAtlas(Lnet/minecraft/client/resources/IResourceManager;)V",
        at = @At(value = "INVOKE", target = "Ljava/util/List;clear()V", remap = false, shift = At.Shift.AFTER))
    private void optiFuture$registerCustomTiles(IResourceManager manager, CallbackInfo ci) {
        this.registerIcons();
        TileLoader.registerIcons((TextureMap) (Object) this, this.basePath, this.mapRegisteredSprites);
    }

    /**
     * Overrides the completed sprite resource path with the tile loader's
     * override.
     * <p>
     * 用方块加载器的覆盖路径替换拼接后的精灵资源路径。
     */
    @Redirect(
        method = "completeResourceLocation(Lnet/minecraft/util/ResourceLocation;I)Lnet/minecraft/util/ResourceLocation;",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/String;format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;",
            ordinal = 0,
            remap = false))
    private String optiFuture$overrideTexturePath(String format, Object[] args, ResourceLocation location,
        int p_147634_2_) {
        return TileLoader.getOverridePath("", this.basePath, location.getResourcePath(), ".png");
    }

    // Base game has s.indexOf(47) != -1 || s.indexOf(92) != -1
    // However, forge already removes this, so we don't have to patch that
    /**
     * Lets the tile loader claim special textures during icon registration.
     * <p>
     * 在图标注册期间让方块加载器接管特殊材质。
     */
    @Redirect(
        method = "registerIcon(Ljava/lang/String;)Lnet/minecraft/util/IIcon;",
        at = @At(value = "INVOKE", target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z"))
    private boolean optiFuture$treatSpecialTexture(String instance, Object toCompare) {
        return TileLoader.isSpecialTexture((TextureMap) (Object) this, toCompare.toString(), instance);
    }
}

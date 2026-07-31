package decok.dfcdvadstf.optifuture.mixins.early.base;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.Multimap;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.cc.Colorizer;
import com.prupe.mcpatcher.cit.CITUtils;
import com.prupe.mcpatcher.ctm.CTMUtils;
import com.prupe.mcpatcher.hd.FontUtils;
import com.prupe.mcpatcher.mal.block.BetterGrass;
import com.prupe.mcpatcher.mal.resource.TexturePackChangeHandler;
import com.prupe.mcpatcher.mal.tile.TileLoader;
import com.prupe.mcpatcher.mob.MobRandomizer;

import decok.dfcdvadstf.optifuture.config.MCPatcherForgeConfig;

/**
 * Bootstraps the mcpatcher subsystems from the vanilla {@link Minecraft}
 * lifecycle: it records the game directory, initialises the enabled features
 * during {@code startGame}, drives texture-pack change notifications around the
 * initial resource stitch, swaps the loading-screen logo and polls for
 * texture-pack changes each game loop.
 * <p>
 * 从原版 {@link Minecraft} 生命周期中引导 mcpatcher 子系统：记录游戏
 * 目录，在 {@code startGame} 期间初始化已启用的功能，在首次资源拼合
 * 前后驱动材质包变更通知，替换加载界面的 logo，并在每个游戏循环
 * 中轮询材质包变更。
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

    @Shadow
    public abstract IResourceManager getResourceManager();

    @Shadow
    @Final
    private static ResourceLocation locationMojangPng;

    /**
     * Records the game directory with mcpatcher once the client is constructed.
     * <p>
     * 在客户端构造完成后，将游戏目录登记到 mcpatcher。
     */
    @Inject(
        method = "<init>(Lnet/minecraft/util/Session;IIZZLjava/io/File;Ljava/io/File;Ljava/io/File;Ljava/net/Proxy;Ljava/lang/String;Lcom/google/common/collect/Multimap;Ljava/lang/String;)V",
        at = @At("RETURN"))
    private void optiFuture$captureDataDir(Session sessionIn, int displayWidth, int displayHeight, boolean fullscreen,
        boolean isDemo, File dataDir, File assetsDir, File resourcePackDir, Proxy proxy, String version,
        Multimap<String, String> twitchDetails, String assetsJsonVersion, CallbackInfo ci) {
        MCPatcherUtils.setMinecraft(dataDir);
    }

    /**
     * Initialises tile loading and every enabled mcpatcher feature just before
     * the first reload listener is registered.
     * <p>
     * 在注册首个重载监听器之前，初始化方块加载与每项已启用的
     * mcpatcher 功能。
     */
    @Inject(
        method = "startGame()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/IReloadableResourceManager;registerReloadListener(Lnet/minecraft/client/resources/IResourceManagerReloadListener;)V",
            ordinal = 0))
    private void optiFuture$initSubsystems(CallbackInfo ci) {
        TileLoader.init();
        CTMUtils.reset();
        // Register the Better Grass texture pack handler before the initial atlas stitch.
        // 在首次图集拼合之前注册 Better Grass 的材质包处理器。
        BetterGrass.init();
        MCPatcherForgeConfig config = MCPatcherForgeConfig.instance();
        if (config.customItemTexturesEnabled) {
            CITUtils.init();
        }
        if (config.extendedHDEnabled) {
            FontUtils.init();
        }
        if (config.randomMobsEnabled) {
            MobRandomizer.init();
        }
        if (config.customColorsEnabled) {
            Colorizer.init();
        }
    }

    /**
     * Prepares the texture-pack change handler before the initial atlas stitch.
     * <p>
     * 在首次图集拼合之前准备材质包变更处理器。
     */
    @Inject(
        method = "startGame()V",
        at = @At(
            value = "INVOKE",
            target = "Lcpw/mods/fml/client/FMLClientHandler;beginMinecraftLoading(Lnet/minecraft/client/Minecraft;Ljava/util/List;Lnet/minecraft/client/resources/IReloadableResourceManager;)V",
            remap = false,
            shift = At.Shift.AFTER))
    private void optiFuture$beforeInitialReload(CallbackInfo ci) {
        TexturePackChangeHandler.beforeChange1();
    }

    /**
     * Finalises the texture-pack change handler after the initial atlas stitch.
     * <p>
     * 在首次图集拼合之后收尾材质包变更处理器。
     */
    @Inject(
        method = "startGame()V",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/opengl/GL11;glViewport(IIII)V",
            remap = false,
            shift = At.Shift.AFTER))
    private void optiFuture$afterInitialReload(CallbackInfo ci) {
        TexturePackChangeHandler.afterChange1();
    }

    /**
     * Loads the branding logo from the resource pack instead of the bundled
     * dynamic texture, allowing texture packs to override it.
     * <p>
     * 从资源包加载品牌 logo，而非使用内置的动态材质，使材质包能够
     * 对其进行覆盖。
     */
    @Redirect(
        method = "loadScreen()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureManager;getDynamicTextureLocation(Ljava/lang/String;Lnet/minecraft/client/renderer/texture/DynamicTexture;)Lnet/minecraft/util/ResourceLocation;"))
    private ResourceLocation optiFuture$replaceMojangLogo(TextureManager renderEngine, String p_110578_1_,
        DynamicTexture p_110578_2_) throws IOException {
        return renderEngine.getDynamicTextureLocation(
            "logo",
            new DynamicTexture(
                ImageIO.read(
                    this.getResourceManager()
                        .getResource(locationMojangPng)
                        .getInputStream())));
    }

    /**
     * Checks for a texture-pack change at the start of every game loop.
     * <p>
     * 在每个游戏循环开头检查材质包是否发生变更。
     */
    @Inject(method = "runGameLoop()V", at = @At(value = "HEAD"))
    private void optiFuture$pollTexturePackChange(CallbackInfo ci) {
        TexturePackChangeHandler.checkForTexturePackChange();
    }
}

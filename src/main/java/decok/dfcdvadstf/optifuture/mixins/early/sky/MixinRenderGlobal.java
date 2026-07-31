package decok.dfcdvadstf.optifuture.mixins.early.sky;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.prupe.mcpatcher.sky.SkyRenderer;

/**
 * Routes sky rendering through the custom {@link SkyRenderer} so texture-pack
 * skyboxes can replace the vanilla sky: it sets up and draws the custom sky,
 * rebinds the sun/moon textures, suppresses the vanilla sky colour and display
 * list when the custom renderer is active, and shifts the void horizon height.
 * <p>
 * 将天空渲染经由自定义 {@link SkyRenderer} 处理，使材质包的天空盒
 * 能替换原版天空：设置并绘制自定义天空，重新绑定日/月材质，
 * 在自定义渲染器启用时抑制原版天空颜色与显示列表，并调整虚空
 * 地平线高度。
 */
@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobal {

    @Shadow
    private WorldClient theWorld;

    /**
     * Prepares the custom sky renderer at the start of sky rendering.
     * <p>
     * 在天空渲染开始时准备自定义天空渲染器。
     */
    @Inject(method = "renderSky(F)V", at = @At("HEAD"))
    private void optiFuture$setupSkyRenderer(float partialTick, CallbackInfo ci) {
        SkyRenderer.setup(this.theWorld, partialTick, this.theWorld.getCelestialAngle(partialTick));
    }

    /**
     * Draws all custom sky layers at the appropriate point in the sky render.
     * <p>
     * 在天空渲染的适当位置绘制所有自定义天空层。
     */
    @Inject(
        method = "renderSky(F)V",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glRotatef(FFFF)V", remap = false, ordinal = 9))
    private void optiFuture$renderCustomSky(float partialTick, CallbackInfo ci) {
        SkyRenderer.renderAll();
    }

    // Ordinal 0 shouldn't be redirected unfortunately
    /**
     * Lets the sky renderer override the sun texture binding.
     * <p>
     * 让天空渲染器覆盖太阳材质的绑定。
     */
    @ModifyArg(
        method = "renderSky(F)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V",
            ordinal = 1))
    private ResourceLocation optiFuture$bindSunTexture(ResourceLocation location) {
        return SkyRenderer.setupCelestialObject(location);
    }

    /**
     * Lets the sky renderer override the moon texture binding.
     * <p>
     * 让天空渲染器覆盖月亮材质的绑定。
     */
    @ModifyArg(
        method = "renderSky(F)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V",
            ordinal = 2))
    private ResourceLocation optiFuture$bindMoonTexture(ResourceLocation location) {
        return SkyRenderer.setupCelestialObject(location);
    }

    /**
     * Suppresses the vanilla sky colour call while the custom sky is active.
     * <p>
     * 在自定义天空启用期间抑制原版的天空颜色调用。
     */
    @WrapWithCondition(
        method = "renderSky(F)V",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glColor4f(FFFF)V", remap = false, ordinal = 1))
    private boolean optiFuture$suppressVanillaSkyColor(float f1, float f2, float f3, float f4) {
        return !SkyRenderer.active;
    }

    /**
     * Suppresses the vanilla sky display list while the custom sky is active.
     * <p>
     * 在自定义天空启用期间抑制原版的天空显示列表。
     */
    @WrapWithCondition(
        method = "renderSky(F)V",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glCallList(I)V", remap = false, ordinal = 1))
    private boolean optiFuture$suppressVanillaSkyList(int i) {
        return !SkyRenderer.active;
    }

    /**
     * Shifts the void-sky translation to the configurable horizon height.
     * <p>
     * 将虚空天空的平移量调整为可配置的地平线高度。
     */
    @ModifyArg(
        method = "renderSky(F)V",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTranslatef(FFF)V", remap = false, ordinal = 2),
        index = 1)
    private float optiFuture$adjustHorizonHeight(float input) {
        // -((d0 - 16.0D)) turned into -((d0 - SkyRenderer.horizonHeight))
        return (float) (input - 16f + SkyRenderer.horizonHeight);
    }
}

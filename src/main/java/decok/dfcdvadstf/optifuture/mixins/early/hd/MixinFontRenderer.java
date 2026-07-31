package decok.dfcdvadstf.optifuture.mixins.early.hd;

import java.awt.image.BufferedImage;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.prupe.mcpatcher.cc.ColorizeWorld;
import com.prupe.mcpatcher.hd.FontUtils;

import decok.dfcdvadstf.optifuture.interfaces.FontRendererExpansion;

/**
 * Wires the vanilla {@link FontRenderer} into the HD font pipeline: it swaps in
 * HD font textures, applies fractional glyph widths, remaps unicode pages and
 * routes text colours through the colorizer. The extra per-instance state is
 * exposed through {@link FontRendererExpansion}.
 * <p>
 * 将原版 {@link FontRenderer} 接入 HD 字体流水线：替换 HD 字体材质、
 * 应用小数字宽、重映射 unicode 分页，并将文本颜色交由着色器处理。
 * 额外的实例状态通过 {@link FontRendererExpansion} 对外暴露。
 */
@Mixin(FontRenderer.class)
public abstract class MixinFontRenderer implements FontRendererExpansion {

    @Shadow
    @Final
    private static ResourceLocation[] unicodePageLocations;

    @Shadow
    protected int[] charWidth;

    @Mutable
    @Shadow
    @Final
    protected ResourceLocation locationFontTexture;

    @Shadow(remap = false)
    protected abstract void bindTexture(ResourceLocation location);

    @Shadow(remap = false)
    protected abstract void setColor(float r, float g, float b, float a);

    @Unique
    private float[] optiFuture$charWidthf;

    @Unique
    private ResourceLocation optiFuture$defaultFont;

    @Unique
    private ResourceLocation optiFuture$hdFont;

    @Unique
    private boolean optiFuture$isHD;

    @Unique
    private float optiFuture$fontAdj;

    @Override
    public float[] getCharWidthf() {
        return optiFuture$charWidthf;
    }

    @Override
    public void setCharWidthf(float[] widthf) {
        optiFuture$charWidthf = widthf;
    }

    @Override
    public ResourceLocation getDefaultFont() {
        return optiFuture$defaultFont;
    }

    @Override
    public void setDefaultFont(ResourceLocation font) {
        optiFuture$defaultFont = font;
    }

    @Override
    public ResourceLocation getHDFont() {
        return optiFuture$hdFont;
    }

    @Override
    public void setHDFont(ResourceLocation font) {
        optiFuture$hdFont = font;
    }

    @Override
    public boolean getIsHD() {
        return optiFuture$isHD;
    }

    @Override
    public void setIsHD(boolean isHD) {
        this.optiFuture$isHD = isHD;
    }

    @Override
    public float getFontAdj() {
        return optiFuture$fontAdj;
    }

    @Override
    public void setFontAdj(float fontAdj) {
        this.optiFuture$fontAdj = fontAdj;
    }

    /**
     * Substitutes the font texture location with the HD variant before the
     * vanilla loader reads it.
     * <p>
     * 在原版加载器读取之前，将字体材质路径替换为 HD 变体。
     */
    @Redirect(
        method = "readFontTexture()V",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/gui/FontRenderer;locationFontTexture:Lnet/minecraft/util/ResourceLocation;"))
    private ResourceLocation optiFuture$redirectFontName(FontRenderer instance) {
        this.locationFontTexture = FontUtils.getFontName((FontRenderer) (Object) this, this.locationFontTexture, 0.0f);
        return this.locationFontTexture;
    }

    /**
     * Captures the freshly loaded glyph image and derives fractional character
     * widths from it.
     * <p>
     * 捕获刚加载的字形图像，并据此推导出小数形式的字符宽度。
     */
    @SuppressWarnings("InvalidInjectorMethodSignature") // IDEA plugin struggles with local capture
    @Inject(method = "readFontTexture()V", at = @At(value = "RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void optiFuture$captureCharWidths(CallbackInfo ci, BufferedImage bufferedimage, int i, int j, int[] aint) {
        setCharWidthf(
            FontUtils
                .computeCharWidthsf((FontRenderer) (Object) this, locationFontTexture, bufferedimage, aint, charWidth));
    }

    /**
     * Replaces the hard-coded space width with the measured HD space width.
     * <p>
     * 用测得的 HD 空格宽度替换硬编码的空格宽度。
     */
    @ModifyConstant(method = "renderCharAtPos(ICZ)F", constant = @Constant(floatValue = 4.0f))
    private float optiFuture$spaceWidthFromHdFont(float constant) {
        return this.optiFuture$charWidthf[32];
    }

    // renderDefaultChar has five 1.0F constants: ordinal 0 is the italic shear (italic ? 1.0F : 0.0F)
    // and must stay untouched, ordinals 1-4 are the "f3 - 1.0F" glyph width corrections that HD fonts
    // replace with fontAdj. An unrestricted @ModifyConstant also zeroed the shear (fontAdj is 0 for
    // HD fonts), breaking italic rendering.
    // renderDefaultChar 内共有 5 处 1.0F 常量：ordinal 0 是斜体切变（italic ? 1.0F : 0.0F）
    // 必须保持原样，ordinal 1-4 才是 HD 字体需要替换为 fontAdj 的 "f3 - 1.0F" 字宽修正。
    // 无限定的 @ModifyConstant 会连带把切变归零（HD 字体时 fontAdj 为 0），导致斜体失效。
    @ModifyConstant(
        method = "renderDefaultChar(IZ)F",
        constant = { @Constant(floatValue = 1.0f, ordinal = 1), @Constant(floatValue = 1.0f, ordinal = 2),
            @Constant(floatValue = 1.0f, ordinal = 3), @Constant(floatValue = 1.0f, ordinal = 4) })
    private float optiFuture$applyGlyphWidthAdjust(float constant) {
        return this.optiFuture$fontAdj;
    }

    /**
     * Overrides the returned glyph advance with the fractional HD width.
     * <p>
     * 用小数形式的 HD 字宽覆盖返回的字形步进值。
     */
    @Inject(method = "renderDefaultChar(IZ)F", at = @At("RETURN"), cancellable = true)
    private void optiFuture$overrideDefaultCharWidth(int ch, boolean b, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(FontUtils.getCharWidthf((FontRenderer) (Object) this, this.charWidth, ch));
    }

    /**
     * Remaps a unicode page location to its HD replacement.
     * <p>
     * 将 unicode 分页路径重映射到其 HD 替代资源。
     */
    @Inject(
        method = "getUnicodePageLocation(I)Lnet/minecraft/util/ResourceLocation;",
        at = @At("RETURN"),
        cancellable = true)
    private void optiFuture$redirectUnicodePage(int index, CallbackInfoReturnable<ResourceLocation> cir) {
        cir.setReturnValue(FontUtils.getUnicodePage(unicodePageLocations[index]));
    }

    /**
     * Records the current string index so the colorizer can address the exact
     * character being drawn.
     * <p>
     * 记录当前字符串索引，使着色器能够定位正在绘制的具体字符。
     */
    @Inject(
        method = "renderStringAtPos(Ljava/lang/String;Z)V",
        locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/FontRenderer;colorCode:[I"))
    private void optiFuture$captureColorCodeIndex(String string, boolean bool, CallbackInfo ci, int i, char c0, int j,
        @Share("renderStringAtPosIndex") LocalIntRef renderStringAtPosIndex) {
        renderStringAtPosIndex.set(j);
    }

    /**
     * Rewrites the per-character colour through the colorizer using the shared
     * string index.
     * <p>
     * 借助共享的字符串索引，将每个字符的颜色交由着色器改写。
     */
    // IDEA plugin really struggles with this for some reason
    @SuppressWarnings("InvalidInjectorMethodSignature")
    @ModifyVariable(
        method = "renderStringAtPos(Ljava/lang/String;Z)V",
        at = @At(value = "STORE", ordinal = 0),
        ordinal = 2)
    private int optiFuture$colorizeStringChar(int color,
        @Share("renderStringAtPosIndex") LocalIntRef renderStringAtPosIndex) {
        return ColorizeWorld.colorizeText(color, renderStringAtPosIndex.get());
    }

    /**
     * Routes the caller-supplied text colour through the colorizer.
     * <p>
     * 将调用方传入的文本颜色交由着色器处理。
     */
    @ModifyVariable(method = "renderString(Ljava/lang/String;IIIZ)I", at = @At("HEAD"), ordinal = 2, argsOnly = true)
    private int optiFuture$colorizeString(int colorizeText) {
        return ColorizeWorld.colorizeText(colorizeText);
    }

    /**
     * Reports the fractional HD string width when an HD font is active.
     * <p>
     * 在 HD 字体启用时返回小数形式的 HD 字符串宽度。
     */
    @Inject(method = "getStringWidth(Ljava/lang/String;)I", at = @At("HEAD"), cancellable = true)
    private void optiFuture$hdStringWidth(String p_78256_1_, CallbackInfoReturnable<Integer> cir) {
        if (getIsHD()) {
            cir.setReturnValue((int) FontUtils.getStringWidthf((FontRenderer) (Object) this, p_78256_1_));
        }
    }
}

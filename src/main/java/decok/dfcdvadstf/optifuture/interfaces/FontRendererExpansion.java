package decok.dfcdvadstf.optifuture.interfaces;

import net.minecraft.util.ResourceLocation;

/**
 * Exposes the internal state of FontRenderer used by the extended-hd font
 * rendering (glyph widths, default/HD font locations, HD flag and width
 * adjustment). Implemented by {@code MixinFontRenderer}; the core logic reads
 * and writes these values through this interface instead of reflection.
 * <p>
 * 向 extended-hd 字体渲染逻辑暴露 FontRenderer 的内部状态（字形宽度、
 * 默认/HD 字体位置、HD 标志与宽度修正）。由 {@code MixinFontRenderer}
 * 实现；核心逻辑通过本接口读写这些值，而非使用反射。
 */
public interface FontRendererExpansion {

    float[] getCharWidthf();

    void setCharWidthf(float[] widthf);

    ResourceLocation getDefaultFont();

    void setDefaultFont(ResourceLocation defaultFont);

    ResourceLocation getHDFont();

    void setHDFont(ResourceLocation hdFont);

    boolean getIsHD();

    void setIsHD(boolean isHD);

    float getFontAdj();

    void setFontAdj(float fontAdj);
}

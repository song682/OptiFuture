package decok.dfcdvadstf.optifuture.interfaces;

/**
 * Exposes the GL texture unload capability of AbstractTexture to the core
 * logic (used by the better-skies "unloadTextures" option). Implemented by
 * {@code MixinAbstractTexture}; the core logic casts AbstractTexture to this
 * interface instead of touching the private fields directly.
 * <p>
 * 向核心逻辑暴露 AbstractTexture 的 GL 纹理卸载能力（供 better skies 的
 * "unloadTextures" 选项使用）。由 {@code MixinAbstractTexture} 实现；核心
 * 逻辑将 AbstractTexture 强转为本接口，而不直接触碰其私有字段。
 */
public interface AbstractTextureExpansion {

    void unloadGLTexture();

}

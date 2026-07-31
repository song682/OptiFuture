package decok.dfcdvadstf.optifuture.interfaces;

/**
 * Exposes the original potion colour so the custom-colors potion tinting can
 * restore it when the custom colour is disabled. Implemented by
 * {@code MixinPotion}; the core logic saves the vanilla value here and reads
 * it back for rendering.
 * <p>
 * 暴露药水的原始颜色，使自定义颜色的药水染色在自定义颜色关闭时
 * 能恢复它。由 {@code MixinPotion} 实现；核心逻辑在此保存原版值，
 * 渲染时再读回。
 */
public interface PotionExpansion {

    void setOrigColor(int color);

    int getOrigColor();
}

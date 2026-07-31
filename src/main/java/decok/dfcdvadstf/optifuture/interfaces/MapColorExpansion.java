package decok.dfcdvadstf.optifuture.interfaces;

/**
 * Exposes the original colour value of MapColor so the custom-colors map
 * tinting can restore it after recoloring. Implemented by
 * {@code MixinMapColor}; the core logic stores the pristine value here before
 * applying the custom palette.
 * <p>
 * 暴露 MapColor 的原始颜色值，使自定义颜色的地图染色在重新着色后
 * 能够恢复它。由 {@code MixinMapColor} 实现；核心逻辑在应用自定义
 * 调色板前把原始值存入此处。
 */
public interface MapColorExpansion {

    int getOriginalColorValue();

    void setOriginalColorValue(int value);
}

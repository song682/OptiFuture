package decok.dfcdvadstf.optifuture.mixins;

/**
 * Enum of mods that feature groups in {@link Mixins} can target or exclude
 * (adapted from Hodgepodge). Each entry identifies a mod by its @Mod name,
 * coremod class and/or modid so the mixin registry can decide at startup
 * whether a feature group applies. Currently only VANILLA is declared.
 * <p>
 * {@link Mixins} 中功能组可针对或排除的模组枚举（改编自 Hodgepodge）。
 * 每个条目通过 @Mod 名称、coremod 类与/或 modid 标识一个模组，供 mixin
 * 注册表在启动期判断功能组是否适用。当前仅声明了 VANILLA。
 */
public enum TargetedMod {

    VANILLA("Minecraft", null);

    /** The "name" in the @Mod annotation */
    public final String modName;
    /** Class that implements the IFMLLoadingPlugin interface */
    public final String coreModClass;
    /** The "modid" in the @Mod annotation */
    public final String modId;

    TargetedMod(String modName, String coreModClass) {
        this(modName, coreModClass, null);
    }

    TargetedMod(String modName, String coreModClass, String modId) {
        this.modName = modName;
        this.coreModClass = coreModClass;
        this.modId = modId;
    }

    @Override
    public String toString() {
        return "TargetedMod{modName='" + modName + "', coreModClass='" + coreModClass + "', modId='" + modId + "'}";
    }
}

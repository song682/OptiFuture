package decok.dfcdvadstf.optifuture;

import cpw.mods.fml.common.Mod;

/**
 * Forge mod entry point. The class body is intentionally empty: the actual
 * features are injected at runtime by the mixin system (early mixins via
 * {@code MCPatcherForgeCore}, late mixins via {@code MCPatcherForgeLateMixins}),
 * so the {@link Mod} annotation only provides the metadata and lifecycle anchor
 * FML needs.
 * <p>
 * Forge 模组入口。类体刻意保持为空：实际功能由 mixin 系统在运行时注入
 * （early mixin 经 {@code MCPatcherForgeCore}，late mixin 经
 * {@code MCPatcherForgeLateMixins}），{@link Mod} 注解仅为 FML 提供
 * 元数据与生命周期锚点。
 */
@Mod(
    modid = Tags.MODID,
    version = Tags.VERSION,
    name = Tags.MODNAME,
    acceptedMinecraftVersions = "[1.7.10]",
    acceptableRemoteVersions = "*")
public class McPatcherForge {
}

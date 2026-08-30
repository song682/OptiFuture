package decok.dfcdvadstf.optifuture;

import cpw.mods.fml.common.Mod;

/**
 * Parent mod container for the bundled child mod {@code MCPatcherForge}.
 * <p>
 * The class body is intentionally empty: this mod only exists to act as the
 * parent anchor declared in mcmod.info ({@code "parent": "optifuture"}), so
 * FML groups MCPatcherForge under it and the mod list shows
 * "OptiFuture 1 child mod". All actual features live in the mixin system,
 * anchored by {@link McPatcherForge}.
 * <p>
 * 父模组容器，收纳内嵌的子模组 {@code MCPatcherForge}。类体刻意保持为空：
 * 该模组仅作为 mcmod.info 中声明的父锚点（{@code "parent": "optifuture"}），
 * 使 FML 将 MCPatcherForge 归入其下并在模组列表中显示 "OptiFuture 1 child mod"。
 * 实际功能全部由 mixin 系统提供，锚定于 {@link McPatcherForge}。
 */
@Mod(
    modid = "optifuture",
    version = Tags.VERSION,
    name = "OptiFuture",
    acceptedMinecraftVersions = "[1.7.10]",
    acceptableRemoteVersions = "*")
public class OptiFuture {
}

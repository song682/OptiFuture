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
 */
@Mod(
    modid = Tags.MODID,
    version = Tags.VERSION,
    name = Tags.NAME,
    useMetadata = true,
    acceptedMinecraftVersions = "[1.7.10]",
    acceptableRemoteVersions = "*")
public class OptiFuture {
}

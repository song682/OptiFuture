package mist475.mcpatcherforge.core;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// NOTE: must be the relocated ASM ClassNode (org.spongepowered.asm.lib), matching the binary
// signature of IMixinConfigPlugin in the UniMixins dev jar; org.objectweb.asm would not compile.
// 注意：必须使用重定位后的 ASM ClassNode（org.spongepowered.asm.lib），与 UniMixins dev jar 中
// IMixinConfigPlugin 的二进制签名一致；若用 org.objectweb.asm 将无法通过编译。
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Mixin config plugin (Sponge Mixin 0.8.5 API, bundled by UniMixins), attached to both
 * "mixins.mcpatcherforge.early.json" and "mixins.mcpatcherforge.json" via their "plugin" key.
 * <p>
 * Works together with the GTNHMixins-style loaders: {@link MCPatcherForgeCore}
 * (IEarlyMixinLoader) and {@link MCPatcherForgeLateMixins} (ILateMixinLoader) decide the mixin
 * list at startup and report it here through {@link #approve(String, Collection)};
 * {@link #shouldApplyMixin(String, String)} then re-validates every mixin at apply time so only
 * loader-approved classes ever get merged.
 * <p>
 * Mixin 配置插件（UniMixins 内置的 Sponge Mixin 0.8.5 标准 API），通过两个 JSON 的 "plugin"
 * 键同时挂载到 early 与 late 配置上。
 * <p>
 * 与 GTNHMixins 风格的加载器协同工作：{@link MCPatcherForgeCore}（IEarlyMixinLoader）与
 * {@link MCPatcherForgeLateMixins}（ILateMixinLoader）在启动期决定 mixin 清单，并经由
 * {@link #approve(String, Collection)} 同步到本类；{@link #shouldApplyMixin(String, String)}
 * 在应用期对每个 mixin 逐一复核，确保只有加载器批准过的类才会被合并。
 * <p>
 * Note: this class is instantiated by Mixin very early inside LaunchClassLoader, so it must
 * never reference Minecraft classes, and it must stay outside the registered mixin packages.
 * <p>
 * 注意：本类会在极早期由 Mixin 于 LaunchClassLoader 中实例化，因此绝不能引用 Minecraft 类，
 * 且必须位于已注册的 mixin 包之外。
 */
public class MCPatcherForgeMixinPlugin implements IMixinConfigPlugin {

    /** Mixin root package of the early config. / early 配置的 mixin 根包。 */
    public static final String EARLY_PACKAGE = "mist475.mcpatcherforge.mixins.early";
    /** Mixin root package of the late config. / late 配置的 mixin 根包。 */
    public static final String LATE_PACKAGE = "mist475.mcpatcherforge.mixins.late";

    /**
     * Per-package sets of loader-approved fully-qualified mixin class names.
     * 按 mixin 根包归档的、由加载器批准的 mixin 类全限定名集合。
     */
    private static final Map<String, Set<String>> APPROVED = new ConcurrentHashMap<>();

    /** The mixin root package this plugin instance serves. / 本插件实例所服务的 mixin 根包。 */
    private String mixinPackage;

    /**
     * Called by the early/late loaders to publish their approved mixin list.
     * 由 early/late 加载器调用，登记它们批准的 mixin 清单。
     *
     * @param mixinPackage       mixin root package of the owning config / 所属配置的 mixin 根包
     * @param relativeMixinClasses class names relative to that package / 相对于该根包的类名
     */
    public static void approve(String mixinPackage, Collection<String> relativeMixinClasses) {
        final Set<String> approved = APPROVED.computeIfAbsent(mixinPackage, k -> ConcurrentHashMap.newKeySet());
        for (String mixinClass : relativeMixinClasses) {
            approved.add(mixinPackage + "." + mixinClass);
        }
    }

    @Override
    public void onLoad(String mixinPackage) {
        // Normalize: Mixin may hand over the package with a trailing dot.
        // 规范化：Mixin 传入的包名可能带有末尾点号。
        this.mixinPackage = (mixinPackage != null && mixinPackage.endsWith("."))
            ? mixinPackage.substring(0, mixinPackage.length() - 1)
            : mixinPackage;
    }

    @Override
    public String getRefMapperConfig() {
        // Use the "refmap" declared in the json config. / 沿用 JSON 配置中声明的 "refmap"。
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        final Set<String> approved = APPROVED.get(this.mixinPackage);
        if (approved == null) {
            // Loader has not reported yet (abnormal path): the mixin list itself was injected
            // by the loader, so anything reaching here is trusted.
            // 加载器尚未登记（异常路径）：清单本身就是由加载器注入的，走到这里的类默认放行。
            return true;
        }
        final boolean allowed = approved.contains(mixinClassName);
        if (!allowed) {
            MCPatcherForgeCore.log
                .warn("Vetoing mixin {} for target {}: not approved by the loader", mixinClassName, targetClassName);
        }
        return allowed;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        // The mixin list is supplied by IEarlyMixinLoader / ILateMixinLoader, not by the plugin.
        // mixin 清单由 IEarlyMixinLoader / ILateMixinLoader 提供，插件不追加。
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}

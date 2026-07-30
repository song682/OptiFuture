package decok.dfcdvadstf.optifuture.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

import decok.dfcdvadstf.optifuture.mixins.Mixins;

/**
 * Late mixin entry point, discovered by UniMixins' GTNHMixins-compat layer via the
 * {@link LateMixin} annotation. Registers "mixins.mcpatcherforge.json" and feeds it the
 * {@link Mixins.Phase#LATE} entries from {@link Mixins}, mirroring how
 * {@link MCPatcherForgeCore} handles the EARLY phase.
 * <p>
 * Late mixin 入口，由 UniMixins 的 GTNHMixins 兼容层通过 {@link LateMixin} 注解发现。
 * 负责注册 "mixins.mcpatcherforge.json" 并向其注入 {@link Mixins} 中
 * {@link Mixins.Phase#LATE} 阶段的条目，与 {@link MCPatcherForgeCore} 处理 EARLY 阶段的方式对称。
 */
@LateMixin
public class MCPatcherForgeLateMixins implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.mcpatcherforge.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        final List<String> mixins = new ArrayList<>();
        final List<String> notLoading = new ArrayList<>();
        for (Mixins mixin : Mixins.values()) {
            if (mixin.phase == Mixins.Phase.LATE) {
                if (mixin.shouldLoad(Collections.emptySet(), loadedMods)) {
                    mixins.addAll(mixin.mixinClasses);
                } else {
                    notLoading.addAll(mixin.mixinClasses);
                }
            }
        }
        MCPatcherForgeCore.log.info("Not loading the following LATE mixins: {}", notLoading.toString());
        // Publish the approved list to the config plugin for apply-time re-validation.
        // 将批准清单同步给配置插件，供应用期逐个复核。
        MCPatcherForgeMixinPlugin.approve(MCPatcherForgeMixinPlugin.LATE_PACKAGE, mixins);
        return mixins;
    }
}

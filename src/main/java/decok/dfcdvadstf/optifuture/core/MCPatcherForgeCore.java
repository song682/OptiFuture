package decok.dfcdvadstf.optifuture.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import decok.dfcdvadstf.optifuture.mixins.Mixins;

/**
 * Coremod entry point and early-mixin loader (adapted from Hodgepodge). As an
 * {@link IFMLLoadingPlugin} it is instantiated by FML at startup; as an
 * {@link IEarlyMixinLoader} it collects the EARLY-phase mixin list from
 * {@link Mixins}, gates each entry by the current configuration and publishes
 * the approved list to {@link MCPatcherForgeMixinPlugin} for apply-time
 * re-validation. No raw ASM transformers are registered anymore: the former
 * transformers were ported to mixins (see {@link #getASMTransformerClass()}).
 * <p>
 * Coremod 入口与早期 mixin 加载器（改编自 Hodgepodge）。作为
 * {@link IFMLLoadingPlugin} 由 FML 在启动时实例化；作为
 * {@link IEarlyMixinLoader} 从 {@link Mixins} 收集 EARLY 阶段的 mixin 清单，
 * 依据当前配置逐项门控，并将批准清单同步给
 * {@link MCPatcherForgeMixinPlugin} 供应用期复核。不再注册任何原始 ASM
 * 转换器：原有转换器均已移植为 mixin（见 {@link #getASMTransformerClass()}）。
 */
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class MCPatcherForgeCore implements IFMLLoadingPlugin, IEarlyMixinLoader {

    public static final Logger log = LogManager.getLogger("MCPatcher");

    @Override
    public String getMixinConfig() {
        return "mixins.mcpatcherforge.early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        final List<String> mixins = new ArrayList<>();
        final List<String> notLoading = new ArrayList<>();
        for (Mixins mixin : Mixins.values()) {
            if (mixin.phase == Mixins.Phase.EARLY) {
                if (mixin.shouldLoad(loadedCoreMods, Collections.emptySet())) {
                    mixins.addAll(mixin.mixinClasses);
                } else {
                    notLoading.addAll(mixin.mixinClasses);
                }
            }
        }
        log.info("Not loading the following EARLY mixins: {}", notLoading.toString());
        // Publish the approved list to the config plugin for apply-time re-validation.
        // 将批准清单同步给配置插件，供应用期逐个复核。
        MCPatcherForgeMixinPlugin.approve(MCPatcherForgeMixinPlugin.EARLY_PACKAGE, mixins);
        return mixins;
    }

    @Override
    public String[] getASMTransformerClass() {
        // The former ASM transformers were ported to mixins: RenderBlocksTransformer lives on in
        // cc.client.renderer.MixinRenderBlocks and WorldRendererTransformer in
        // renderpass.MixinWorldRenderer, so no raw class transformers are registered anymore.
        // 原有的 ASM Transformer 已移植为 Mixin：RenderBlocksTransformer 的逻辑现在
        // cc.client.renderer.MixinRenderBlocks 中，WorldRendererTransformer 的逻辑在
        // renderpass.MixinWorldRenderer 中，因此不再注册任何原始类转换器。
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}

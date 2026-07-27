package mist475.mcpatcherforge.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import mist475.mcpatcherforge.mixins.Mixins;

/**
 * Adapted from Hodgepodge
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

package com.prupe.mcpatcher.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.mal.resource.PropertiesFile;
import com.prupe.mcpatcher.mal.resource.ResourceList;
import com.prupe.mcpatcher.mal.resource.TexturePackChangeHandler;

/**
 * Custom world loading screen support ("loading.properties", see doc/loading.properties).
 * 自定义世界加载屏幕支持（"loading.properties"，参见 doc/loading.properties）。
 * <p>
 * Resource layout / 资源布局：
 *
 * <pre>
 * assets/minecraft/{mcpatcher|optifine}/gui/loading/loading.properties  - global + per-dimension settings / 全局及按维度设置
 * assets/minecraft/{mcpatcher|optifine}/gui/loading/background&lt;dim&gt;.png - background texture per dimension id / 按维度 ID 的背景贴图
 * </pre>
 *
 * The dimension id may be negative (nether = -1, overworld = 0, the end = 1, modded dims too).
 * A background replaces the dirt/gradient background of {@code GuiDownloadTerrain} and
 * {@code GuiScreenWorking} for its dimension only.
 * 维度 ID 允许为负（下界 = -1，主世界 = 0，末地 = 1，模组维度同理）。
 * 背景贴图仅替换其所属维度下 {@code GuiDownloadTerrain} 与 {@code GuiScreenWorking} 的泥土/渐变背景。
 * <p>
 * All hooks are pass-through when no resource pack provides any background texture.
 * 若没有任何材质包提供背景贴图，所有挂钩点均原样放行。
 */
public class CustomLoadingScreens {

    private static final MCLogger logger = MCLogger.getLogger(MCLogger.Category.CUSTOM_LOADING_SCREENS);

    /** Both the mcpatcher and the optifine directory layout are accepted. / mcpatcher 与 optifine 两种目录布局均支持。 */
    private static final String[] BASE_DIRS = { "mcpatcher/gui/loading/", "optifine/gui/loading/" };
    private static final String PROPERTIES_NAME = "loading.properties";
    private static final String TEXTURE_PREFIX = "background";
    private static final String TEXTURE_SUFFIX = ".png";

    // Scale modes, matching the OptiFine documentation. / 缩放模式，与 OptiFine 文档一致。
    private static final int SCALE_MODE_FIXED = 0;
    private static final int SCALE_MODE_FULL = 1;
    private static final int SCALE_MODE_STRETCH = 2;
    /** Default scale is 2 for "fixed" (16*2=32px like vanilla dirt) and 1 otherwise. / "fixed" 默认 2（16*2=32 像素，同原版泥土），其余模式默认 1。 */
    private static final int DEFAULT_SCALE_FIXED = 2;
    private static final int DEFAULT_SCALE_OTHER = 1;

    /** Screens by dimension id, rebuilt on every texture pack change. / 按维度 ID 索引的屏幕表，材质包变更时重建。 */
    private static final Map<Integer, LoadingScreen> screens = new HashMap<>();

    static {
        TexturePackChangeHandler.register(new TexturePackChangeHandler(MCPatcherUtils.CUSTOM_LOADING_SCREENS, 3) {

            @Override
            public void beforeChange() {
                screens.clear();
            }

            @Override
            public void afterChange() {
                reload();
            }
        });
    }

    /**
     * Rebuilds the dimension -> screen table from the currently selected resource packs.
     * 依据当前启用的材质包重建 维度 -> 屏幕 映射表。
     */
    private static void reload() {
        // A single top-level properties file is shared by all screens; the mcpatcher
        // directory takes precedence over the optifine one, mirroring CustomPanorama.
        // 所有屏幕共享同一份顶层属性文件；mcpatcher 目录优先于 optifine 目录，与 CustomPanorama 一致。
        PropertiesFile properties = null;
        for (String baseDir : BASE_DIRS) {
            properties = PropertiesFile.get(logger, new ResourceLocation(baseDir + PROPERTIES_NAME));
            if (properties != null) {
                break;
            }
        }
        for (String baseDir : BASE_DIRS) {
            List<ResourceLocation> resources = ResourceList.getInstance()
                .listResources(baseDir, TEXTURE_SUFFIX, false, false, false);
            for (ResourceLocation resource : resources) {
                String path = resource.getResourcePath();
                String name = path.substring(path.lastIndexOf('/') + 1);
                if (!name.startsWith(TEXTURE_PREFIX)) {
                    continue;
                }
                String dimString = name.substring(TEXTURE_PREFIX.length(), name.length() - TEXTURE_SUFFIX.length());
                int dimension;
                try {
                    // May be negative, e.g. background-1.png for the nether.
                    // 允许负数，例如下界的 background-1.png。
                    dimension = Integer.parseInt(dimString);
                } catch (NumberFormatException e) {
                    logger.warning("invalid dimension id '%s' in %s", dimString, path);
                    continue;
                }
                // First base dir wins for duplicate dimension ids. / 维度 ID 重复时先扫描到的目录优先。
                if (!screens.containsKey(dimension)) {
                    screens.put(dimension, new LoadingScreen(resource, dimension, properties));
                }
            }
        }
        if (!screens.isEmpty()) {
            logger.fine("%d custom loading screen(s) available", screens.size());
        }
    }

    /**
     * Draws the custom background for the current dimension if one is defined.
     * Returns false to let the caller fall back to the vanilla background.
     * 若当前维度定义了自定义背景则绘制之；返回 false 表示由调用方回退到原版背景。
     *
     * @param width  scaled GUI width / 缩放后的 GUI 宽度
     * @param height scaled GUI height / 缩放后的 GUI 高度
     */
    public static boolean drawBackground(int width, int height) {
        LoadingScreen screen = getScreen();
        if (screen == null) {
            return false;
        }
        screen.draw(width, height);
        return true;
    }

    /**
     * Looks up the screen for the current dimension. When GuiDownloadTerrain is shown
     * the destination world is already loaded (see NetHandlerPlayClient.handleRespawn),
     * so the provider's dimension id is the one being entered; without a world
     * (e.g. GuiScreenWorking while a level is created) the overworld entry is used.
     * 按当前维度查找屏幕。GuiDownloadTerrain 显示时目标世界已加载完毕
     * （见 NetHandlerPlayClient.handleRespawn），故 provider 的维度 ID 即目的维度；
     * 无世界时（如创建存档期间的 GuiScreenWorking）退回主世界条目。
     */
    private static LoadingScreen getScreen() {
        if (screens.isEmpty()) {
            return null;
        }
        Minecraft mc = Minecraft.getMinecraft();
        int dimension = mc.theWorld != null ? mc.theWorld.provider.dimensionId : 0;
        return screens.get(dimension);
    }

    /**
     * Resolves "dim&lt;dim&gt;.&lt;key&gt;" first, then the global "&lt;key&gt;"; null when absent.
     * 先解析 "dim&lt;dim&gt;.&lt;key&gt;"，再回退全局 "&lt;key&gt;"；均缺失时返回 null。
     */
    private static String getProperty(PropertiesFile properties, String key, int dimension) {
        if (properties == null) {
            return null;
        }
        String value = properties.getString("dim" + dimension + "." + key, "")
            .trim();
        if (!value.isEmpty()) {
            return value;
        }
        value = properties.getString(key, "")
            .trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * One background texture with its parsed per-dimension properties.
     * 单张背景贴图及其解析后的按维度属性。
     */
    private static class LoadingScreen {

        final ResourceLocation texture;
        final int scaleMode;
        final int scale;
        final boolean center;

        LoadingScreen(ResourceLocation texture, int dimension, PropertiesFile properties) {
            this.texture = texture;
            scaleMode = parseScaleMode(properties, getProperty(properties, "scaleMode", dimension));
            int defaultScale = scaleMode == SCALE_MODE_FIXED ? DEFAULT_SCALE_FIXED : DEFAULT_SCALE_OTHER;
            scale = parseScale(properties, getProperty(properties, "scale", dimension), defaultScale);
            center = Boolean.parseBoolean(getProperty(properties, "center", dimension));
        }

        private static int parseScaleMode(PropertiesFile properties, String value) {
            if (value == null) {
                return SCALE_MODE_FIXED;
            }
            value = value.toLowerCase()
                .trim();
            if (value.equals("fixed")) {
                return SCALE_MODE_FIXED;
            } else if (value.equals("full")) {
                return SCALE_MODE_FULL;
            } else if (value.equals("stretch")) {
                return SCALE_MODE_STRETCH;
            } else {
                properties.warning("invalid scaleMode value '%s'", value);
                return SCALE_MODE_FIXED;
            }
        }

        private static int parseScale(PropertiesFile properties, String value, int defaultValue) {
            if (value == null) {
                return defaultValue;
            }
            try {
                int scale = Integer.parseInt(value.trim());
                if (scale >= 1) {
                    return scale;
                }
            } catch (NumberFormatException e) {
                // fall through to the warning below / 落入下方的警告分支
            }
            properties.warning("invalid scale value '%s'", value);
            return defaultValue;
        }

        /**
         * Fills the whole screen with the background texture; the UV math matches
         * OptiFine's CustomLoadingScreen.drawBackground, the GL state setup matches
         * vanilla GuiScreen.drawBackground.
         * 以背景贴图铺满整个屏幕；UV 计算与 OptiFine 的 CustomLoadingScreen.drawBackground
         * 一致，GL 状态设置与原版 GuiScreen.drawBackground 一致。
         */
        void draw(int width, int height) {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_FOG);
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(texture);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            // fixed: one texture covers 16*scale GUI pixels, tiled across the screen.
            // fixed：单张贴图覆盖 16*scale 个 GUI 像素，超出部分平铺。
            float textureSize = 16.0f * scale;
            float u = width / textureSize;
            float v = height / textureSize;
            float uOffset = 0.0f;
            float vOffset = 0.0f;
            if (center) {
                uOffset = (textureSize - width) / (textureSize * 2.0f);
                vOffset = (textureSize - height) / (textureSize * 2.0f);
            }
            switch (scaleMode) {
                case SCALE_MODE_FULL:
                    // full: keep aspect ratio, "scale" full textures fit the longer edge.
                    // full：保持宽高比，长边容纳 "scale" 张完整贴图。
                    textureSize = Math.max(width, height);
                    u = scale * width / textureSize;
                    v = scale * height / textureSize;
                    if (center) {
                        uOffset = scale * (textureSize - width) / (textureSize * 2.0f);
                        vOffset = scale * (textureSize - height) / (textureSize * 2.0f);
                    }
                    break;
                case SCALE_MODE_STRETCH:
                    // stretch: ignore aspect ratio, "scale" textures per axis.
                    // stretch：忽略宽高比，每个轴向铺 "scale" 张贴图。
                    u = scale;
                    v = scale;
                    uOffset = 0.0f;
                    vOffset = 0.0f;
                    break;
            }
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            tessellator.setColorOpaque_I(0xffffff);
            tessellator.addVertexWithUV(0.0d, height, 0.0d, uOffset, vOffset + v);
            tessellator.addVertexWithUV(width, height, 0.0d, uOffset + u, vOffset + v);
            tessellator.addVertexWithUV(width, 0.0d, 0.0d, uOffset + u, vOffset);
            tessellator.addVertexWithUV(0.0d, 0.0d, 0.0d, uOffset, vOffset);
            tessellator.draw();
        }
    }
}

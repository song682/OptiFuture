package com.prupe.mcpatcher.gui;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.util.ResourceLocation;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.mal.resource.PropertiesFile;
import com.prupe.mcpatcher.mal.resource.TexturePackAPI;
import com.prupe.mcpatcher.mal.resource.TexturePackChangeHandler;

/**
 * Custom main menu panorama support ("background.properties", see doc/background.properties).
 * 自定义主菜单全景图支持（"background.properties"，参见 doc/background.properties）。
 * <p>
 * Resource layout / 资源布局：
 *
 * <pre>
 * assets/minecraft/{mcpatcher|optifine}/gui/background.properties   - properties of the vanilla panorama / 原版全景图的属性
 * assets/minecraft/{mcpatcher|optifine}/gui/background1/            - alternative panorama folder / 备选全景图文件夹
 *     panorama_0.png ... panorama_5.png                             - 6 cube map faces / 六张立方体贴图
 *     background.properties (optional)                              - per-folder properties / 文件夹私有属性
 * </pre>
 *
 * One panorama is picked by weighted random selection each time the main menu is created.
 * The vanilla panorama is always part of the candidate pool.
 * 每次主菜单创建时按权重随机选取一个全景图；原版全景图始终参与随机。
 * <p>
 * All hooks are pass-through when no resource pack provides any of the files above.
 * 若没有任何材质包提供上述文件，所有挂钩点均原样放行。
 */
public class CustomPanorama {

    private static final MCLogger logger = MCLogger.getLogger(MCLogger.Category.CUSTOM_PANORAMA);

    /** Both the mcpatcher and the optifine directory layout are accepted. / mcpatcher 与 optifine 两种目录布局均支持。 */
    private static final String[] BASE_DIRS = { "mcpatcher/gui/", "optifine/gui/" };
    private static final String PROPERTIES_NAME = "background.properties";
    private static final String FOLDER_PREFIX = "background";
    /** Alternative folders are scanned with contiguous numbering starting at 1. / 备选文件夹从 1 开始连续编号扫描。 */
    private static final int MAX_ALTERNATIVES = 100;
    private static final int PANORAMA_SIDES = 6;

    // Vanilla defaults, matching the constants compiled into GuiMainMenu.
    // 原版默认值，与 GuiMainMenu 中编译出的常量一一对应。
    private static final int DEFAULT_BLUR1 = 64; // drawPanorama: 8x8 sample grid / 8x8 采样网格
    private static final int DEFAULT_BLUR2 = 3; // rotateAndBlurSkybox: layers per blur pass / 单次模糊的叠加层数
    // Note: 1.7.10 vanilla applies 7 blur passes (the doc range 1-3 stems from newer versions).
    // 注意：1.7.10 原版执行 7 次模糊 pass（文档的 1-3 取值范围来自更高版本）。
    private static final int DEFAULT_BLUR3 = 7;
    private static final int DEFAULT_OVERLAY1_TOP = 0x80ffffff;
    private static final int DEFAULT_OVERLAY1_BOTTOM = 0x00ffffff;
    private static final int DEFAULT_OVERLAY2_TOP = 0x00000000;
    private static final int DEFAULT_OVERLAY2_BOTTOM = 0x80000000;

    private static final Random random = new Random();

    /** Candidate pool rebuilt on every texture pack change. / 候选池，材质包变更时重建。 */
    private static final List<PanoramaEntry> panoramas = new ArrayList<>();
    /** Currently selected panorama, null = feature inactive. / 当前选中的全景图，null 表示功能未激活。 */
    private static PanoramaEntry current;
    /** Counts rotateAndBlurSkybox invocations within one renderSkybox call. / 统计单次 renderSkybox 内的模糊 pass 次数。 */
    private static int blurPassCounter;

    static {
        TexturePackChangeHandler.register(new TexturePackChangeHandler(MCPatcherUtils.CUSTOM_PANORAMA, 3) {

            @Override
            public void beforeChange() {
                panoramas.clear();
                current = null;
            }

            @Override
            public void afterChange() {
                reload();
                selectPanorama();
            }
        });
    }

    /**
     * Rebuilds the candidate pool from the currently selected resource packs.
     * 依据当前启用的材质包重建候选池。
     */
    private static void reload() {
        // Top-level properties describe the vanilla panorama. / 顶层属性文件描述原版全景图。
        PropertiesFile defaultProperties = null;
        for (String baseDir : BASE_DIRS) {
            defaultProperties = PropertiesFile.get(logger, new ResourceLocation(baseDir + PROPERTIES_NAME));
            if (defaultProperties != null) {
                break;
            }
        }
        List<PanoramaEntry> alternatives = new ArrayList<>();
        for (String baseDir : BASE_DIRS) {
            for (int i = 1; i <= MAX_ALTERNATIVES; i++) {
                String folder = baseDir + FOLDER_PREFIX + i;
                if (!resourceExists(new ResourceLocation(folder + "/panorama_0.png"))) {
                    // End of the contiguous numbering run for this base dir. / 该目录下连续编号到此为止。
                    break;
                }
                ResourceLocation[] textures = getFolderTextures(folder);
                if (textures == null) {
                    // Incomplete folder: skip it but keep scanning. / 文件夹贴图不全：跳过但继续扫描。
                    continue;
                }
                PropertiesFile properties = PropertiesFile
                    .get(logger, new ResourceLocation(folder + '/' + PROPERTIES_NAME));
                alternatives.add(new PanoramaEntry(folder, textures, properties));
            }
        }
        if (defaultProperties == null && alternatives.isEmpty()) {
            // Nothing custom present, stay fully vanilla. / 没有任何自定义资源，保持完全原版。
            return;
        }
        panoramas.add(new PanoramaEntry("(default)", null, defaultProperties));
        panoramas.addAll(alternatives);
        logger.fine("%d panorama(s) available", panoramas.size());
    }

    /**
     * Collects the 6 cube map faces of an alternative folder, null if any face is missing.
     * 收集备选文件夹的六张立方体贴图，任意一张缺失则返回 null。
     */
    private static ResourceLocation[] getFolderTextures(String folder) {
        ResourceLocation[] textures = new ResourceLocation[PANORAMA_SIDES];
        for (int side = 0; side < PANORAMA_SIDES; side++) {
            textures[side] = new ResourceLocation(folder + "/panorama_" + side + ".png");
            if (!resourceExists(textures[side])) {
                logger.warning("%s is missing, ignoring folder %s", textures[side], folder);
                return null;
            }
        }
        return textures;
    }

    /**
     * Lightweight existence check without decoding the image.
     * 轻量级存在性检查，避免解码整张图片。
     */
    private static boolean resourceExists(ResourceLocation resource) {
        InputStream is = TexturePackAPI.getInputStream(resource);
        MCPatcherUtils.close(is);
        return is != null;
    }

    /**
     * Weighted random selection, called once per GuiMainMenu instance and after pack changes.
     * 加权随机选取，主菜单每次创建及材质包变更后各调用一次。
     */
    public static void selectPanorama() {
        if (panoramas.isEmpty()) {
            current = null;
            return;
        }
        int totalWeight = 0;
        for (PanoramaEntry entry : panoramas) {
            totalWeight += entry.weight;
        }
        int roll = random.nextInt(totalWeight);
        PanoramaEntry picked = panoramas.get(panoramas.size() - 1);
        for (PanoramaEntry entry : panoramas) {
            if (roll < entry.weight) {
                picked = entry;
                break;
            }
            roll -= entry.weight;
        }
        current = picked;
        logger.fine("selected panorama %s", picked.name);
    }

    /**
     * Replaces one vanilla cube map face; the face index is parsed from "panorama_N.png".
     * 替换单张原版立方体贴图；面序号从 "panorama_N.png" 文件名解析。
     */
    public static ResourceLocation getPanoramaTexture(ResourceLocation defaultTexture) {
        PanoramaEntry entry = current;
        if (entry == null || entry.textures == null) {
            return defaultTexture;
        }
        String path = defaultTexture.getResourcePath();
        int suffix = path.lastIndexOf(".png");
        if (suffix >= 1) {
            int side = path.charAt(suffix - 1) - '0';
            if (side >= 0 && side < PANORAMA_SIDES) {
                return entry.textures[side];
            }
        }
        return defaultTexture;
    }

    /**
     * blur1 hook: sample grid edge length for drawPanorama (vanilla 8, i.e. 64 samples).
     * blur1 挂钩：drawPanorama 的采样网格边长（原版 8，即 64 次采样）。
     */
    public static int getGridSize(int defaultValue) {
        return current == null ? defaultValue : current.gridSize;
    }

    /**
     * blur2 hook: blur layers within one rotateAndBlurSkybox pass (vanilla 3).
     * blur2 挂钩：单次 rotateAndBlurSkybox 内的模糊叠加层数（原版 3）。
     */
    public static int getBlurLoops(int defaultValue) {
        return current == null ? defaultValue : current.blurLoops;
    }

    /**
     * Resets the blur pass counter at the start of renderSkybox.
     * 在 renderSkybox 开头重置模糊 pass 计数器。
     */
    public static void resetBlurPasses() {
        blurPassCounter = 0;
    }

    /**
     * blur3 hook: whether the next rotateAndBlurSkybox pass should run (vanilla runs all 7).
     * blur3 挂钩：判断下一次 rotateAndBlurSkybox 是否执行（原版全部执行 7 次）。
     */
    public static boolean nextBlurPass() {
        int passes = current == null ? DEFAULT_BLUR3 : current.blurPasses;
        return blurPassCounter++ < passes;
    }

    /**
     * Overlay hook: returns {top, bottom} ARGB colors for gradient overlay 1 or 2,
     * or null when that overlay is disabled (both colors zero).
     * 覆盖层挂钩：返回 1 号或 2 号渐变覆盖层的 {顶部, 底部} ARGB 颜色；
     * 若该覆盖层被禁用（两个颜色均为 0）则返回 null。
     */
    public static int[] getOverlayColors(int overlay, int defaultTop, int defaultBottom) {
        PanoramaEntry entry = current;
        if (entry == null) {
            return new int[] { defaultTop, defaultBottom };
        }
        int top = overlay == 1 ? entry.overlay1Top : entry.overlay2Top;
        int bottom = overlay == 1 ? entry.overlay1Bottom : entry.overlay2Bottom;
        if (top == 0 && bottom == 0) {
            return null;
        }
        return new int[] { top, bottom };
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Parses an ARGB hex color; Long.parseLong is required because values such as
     * "80FFFFFF" overflow Integer.parseInt(String, 16).
     * 解析 ARGB 十六进制颜色；必须使用 Long.parseLong，因为 "80FFFFFF" 这类
     * 高位带符号的值会使 Integer.parseInt(String, 16) 溢出。
     */
    private static int parseColor(PropertiesFile properties, String key, int defaultValue) {
        String value = properties.getString(key, "")
            .trim();
        if (value.isEmpty()) {
            return defaultValue;
        }
        try {
            return (int) Long.parseLong(value, 16);
        } catch (NumberFormatException e) {
            properties.warning("invalid %s value '%s'", key, value);
            return defaultValue;
        }
    }

    /**
     * One panorama candidate with its parsed properties.
     * 单个全景图候选项及其解析后的属性。
     */
    private static class PanoramaEntry {

        final String name;
        /** null = vanilla textures. / null 表示使用原版贴图。 */
        final ResourceLocation[] textures;
        final int weight;
        /**
         * Derived from blur1 (1-64 samples) as ceil(sqrt(blur1)), i.e. quantized to a square grid of 1-8.
         * 由 blur1（1-64 次采样）按 ceil(sqrt(blur1)) 推导，量化为 1-8 的方形网格边长。
         */
        final int gridSize;
        final int blurLoops;
        final int blurPasses;
        final int overlay1Top;
        final int overlay1Bottom;
        final int overlay2Top;
        final int overlay2Bottom;

        PanoramaEntry(String name, ResourceLocation[] textures, PropertiesFile properties) {
            this.name = name;
            this.textures = textures;
            if (properties == null) {
                weight = 1;
                gridSize = (int) Math.ceil(Math.sqrt(DEFAULT_BLUR1));
                blurLoops = DEFAULT_BLUR2;
                blurPasses = DEFAULT_BLUR3;
                overlay1Top = DEFAULT_OVERLAY1_TOP;
                overlay1Bottom = DEFAULT_OVERLAY1_BOTTOM;
                overlay2Top = DEFAULT_OVERLAY2_TOP;
                overlay2Bottom = DEFAULT_OVERLAY2_BOTTOM;
            } else {
                weight = Math.max(properties.getInt("weight", 1), 1);
                int blur1 = clamp(properties.getInt("blur1", DEFAULT_BLUR1), 1, DEFAULT_BLUR1);
                gridSize = (int) Math.ceil(Math.sqrt(blur1));
                blurLoops = clamp(properties.getInt("blur2", DEFAULT_BLUR2), 1, DEFAULT_BLUR2);
                blurPasses = clamp(properties.getInt("blur3", DEFAULT_BLUR3), 1, DEFAULT_BLUR3);
                overlay1Top = parseColor(properties, "overlay1.top", DEFAULT_OVERLAY1_TOP);
                overlay1Bottom = parseColor(properties, "overlay1.bottom", DEFAULT_OVERLAY1_BOTTOM);
                overlay2Top = parseColor(properties, "overlay2.top", DEFAULT_OVERLAY2_TOP);
                overlay2Bottom = parseColor(properties, "overlay2.bottom", DEFAULT_OVERLAY2_BOTTOM);
            }
        }
    }
}

package com.prupe.mcpatcher.natural;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.mal.resource.PropertiesFile;
import com.prupe.mcpatcher.mal.resource.TexturePackAPI;
import com.prupe.mcpatcher.mal.resource.TexturePackChangeHandler;
import com.prupe.mcpatcher.mal.tile.TileLoader;

/**
 * Loader for the OptiFine-style natural.properties file (see doc/natural.properties).
 * Each line maps a texture name to a {@link NaturalProperties} rotation/flip setting;
 * once the icons are resolved the per-face random values are derived from a
 * position-based random id so that neighbouring blocks show different orientations
 * without any per-frame state. When the file or a texture is missing the feature
 * degrades to vanilla rendering, exactly like the other mcpatcher modules.
 * <p>
 * OptiFine 风格 natural.properties 文件的加载器（参见 doc/natural.properties）。
 * 每行将一个纹理名映射到 {@link NaturalProperties} 旋转/翻转设置；图标解析后，
 * 每个面的随机值由基于方块坐标的随机 id 派生，使相邻方块呈现不同朝向且无需
 * 任何逐帧状态。文件或纹理缺失时功能退化为原版渲染，与其它 mcpatcher 模块一致。
 */
public class NaturalTextures {

    private static final MCLogger logger = MCLogger.getLogger(MCLogger.Category.NATURAL_TEXTURES);

    // Preferred MCPatcher location, with the documented OptiFine location as fallback.
    // 优先使用 MCPatcher 目录，其次回退到文档中给出的 OptiFine 目录。
    private static final ResourceLocation MCPATCHER_PROPERTIES = TexturePackAPI
        .newMCPatcherResourceLocation("natural.properties");
    private static final ResourceLocation OPTIFINE_PROPERTIES = new ResourceLocation("optifine/natural.properties");

    // Modern (1.13+) texture names used by the documented format, mapped to 1.7.10 atlas names.
    // 文档格式里使用的现代（1.13+）贴图名到 1.7.10 图集名的映射。
    private static final Map<String, String> MODERN_NAME_ALIASES = new HashMap<>();

    /** Random id of the block currently being rendered. / 当前正在渲染方块的随机 id。 */
    public static long randomBlockId;

    private static final Map<String, NaturalProperties> propertiesByName = new HashMap<>();
    private static final Map<IIcon, NaturalProperties> propertiesByIcon = new IdentityHashMap<>();

    private static boolean enabled;
    private static TileLoader tileLoader;

    static {
        MODERN_NAME_ALIASES.put("grass_block_side", "grass_side");
        MODERN_NAME_ALIASES.put("grass_block_side_overlay", "grass_side_overlay");
        MODERN_NAME_ALIASES.put("grass_block_snow", "grass_side_snowed");
        MODERN_NAME_ALIASES.put("podzol_top", "dirt_podzol_top");
        MODERN_NAME_ALIASES.put("podzol_side", "dirt_podzol_side");
        MODERN_NAME_ALIASES.put("farmland_moist", "farmland_wet");
        MODERN_NAME_ALIASES.put("oak_log", "log_oak");
        MODERN_NAME_ALIASES.put("spruce_log", "log_spruce");
        MODERN_NAME_ALIASES.put("birch_log", "log_birch");
        MODERN_NAME_ALIASES.put("jungle_log", "log_jungle");
        MODERN_NAME_ALIASES.put("acacia_log", "log_acacia");
        MODERN_NAME_ALIASES.put("dark_oak_log", "log_big_oak");
        MODERN_NAME_ALIASES.put("oak_log_top", "log_oak_top");
        MODERN_NAME_ALIASES.put("spruce_log_top", "log_spruce_top");
        MODERN_NAME_ALIASES.put("birch_log_top", "log_birch_top");
        MODERN_NAME_ALIASES.put("jungle_log_top", "log_jungle_top");
        MODERN_NAME_ALIASES.put("acacia_log_top", "log_acacia_top");
        MODERN_NAME_ALIASES.put("dark_oak_log_top", "log_big_oak_top");
        MODERN_NAME_ALIASES.put("oak_leaves", "leaves_oak");
        MODERN_NAME_ALIASES.put("spruce_leaves", "leaves_spruce");
        MODERN_NAME_ALIASES.put("birch_leaves", "leaves_birch");
        MODERN_NAME_ALIASES.put("jungle_leaves", "leaves_jungle");
        MODERN_NAME_ALIASES.put("dark_oak_leaves", "leaves_big_oak");
        MODERN_NAME_ALIASES.put("acacia_leaves", "leaves_acacia");
        MODERN_NAME_ALIASES.put("nether_quartz_ore", "quartz_ore");

        reset();

        TexturePackChangeHandler.register(new TexturePackChangeHandler("Natural Textures", 4) {

            @Override
            public void initialize() {}

            @Override
            public void beforeChange() {
                reset();
                if (!enabled) {
                    return;
                }
                tileLoader = new TileLoader("textures/blocks", logger);
                loadProperties();
            }

            @Override
            public void afterChange() {
                if (!enabled) {
                    return;
                }
                resolveIcons();
            }
        });
    }

    /** Force class loading so the texture pack handler gets registered. / 触发类加载以注册材质包处理器。 */
    public static void init() {}

    /**
     * Whether natural textures are active (a configuration file was found and
     * at least one texture resolved).
     * 自然纹理是否生效（找到了配置文件且至少解析出一个纹理）。
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Position based random id, taken from OptiFine so that existing worlds show
     * the same per-block orientation pattern as with the original implementation.
     * 基于坐标的随机 id，取自 OptiFine，使既有世界与原版实现呈现相同的逐方块朝向分布。
     */
    public static long getRandomBlockId(int x, int y, int z) {
        long id = (long) (x * 3129871) ^ (long) z * 116129781L ^ (long) y;
        id = id * id * 42317861L + id * 11L;
        return id >> 16;
    }

    /**
     * The 3 random bits assigned to one face of the block currently being
     * rendered: bit 0-1 select the rotation, bit 2 the flip.
     * 当前渲染方块某一面分配到的 3 个随机位：位 0-1 选择旋转，位 2 选择翻转。
     */
    public static int getRandomValue(int face) {
        return (int) (randomBlockId >> (face * 3));
    }

    /**
     * Natural properties for an icon, or null when the texture is not configured.
     * 图标对应的自然纹理属性，未配置时返回 null。
     */
    public static NaturalProperties getNaturalProperties(IIcon icon) {
        return icon == null ? null : propertiesByIcon.get(icon);
    }

    private static void reset() {
        propertiesByName.clear();
        propertiesByIcon.clear();
        enabled = false;
        randomBlockId = 0L;
        tileLoader = null;
    }

    private static void loadProperties() {
        PropertiesFile properties = PropertiesFile.get(logger, MCPATCHER_PROPERTIES);
        if (properties == null) {
            properties = PropertiesFile.get(logger, OPTIFINE_PROPERTIES);
        }
        if (properties == null) {
            // No file at all: keep vanilla rendering. / 完全没有文件时保持原版渲染。
            return;
        }
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey()
                .trim();
            String value = entry.getValue()
                .trim();
            if (key.isEmpty()) {
                continue;
            }
            NaturalProperties naturalProperties;
            try {
                naturalProperties = new NaturalProperties(value);
            } catch (IllegalArgumentException e) {
                properties.error("unknown value %s for %s", value, key);
                continue;
            }
            if (!naturalProperties.isValid()) {
                properties.error("unknown value %s for %s", value, key);
                continue;
            }
            // The documented OptiFine format may carry the blocks/ prefix; strip it.
            // 文档中的 OptiFine 格式可能带有 blocks/ 前缀，去掉它。
            if (key.startsWith("blocks/")) {
                key = key.substring("blocks/".length());
            }
            propertiesByName.put(key, naturalProperties);
        }
        properties.fine("loaded Natural Textures settings");
    }

    private static void resolveIcons() {
        if (propertiesByName.isEmpty()) {
            return;
        }
        for (Map.Entry<String, NaturalProperties> entry : propertiesByName.entrySet()) {
            String name = entry.getKey();
            IIcon icon = tileLoader.getIcon(name);
            if (icon == null) {
                String alias = MODERN_NAME_ALIASES.get(name);
                if (alias != null) {
                    icon = tileLoader.getIcon(alias);
                }
            }
            if (icon == null) {
                logger.warning("natural texture %s not found", name);
                continue;
            }
            propertiesByIcon.put(icon, entry.getValue());
        }
        enabled = !propertiesByIcon.isEmpty();
        propertiesByName.clear();
        if (enabled) {
            logger.config("%d natural texture(s) active", propertiesByIcon.size());
        }
    }
}

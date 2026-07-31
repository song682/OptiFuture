package com.prupe.mcpatcher.mal.block;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.mal.resource.PropertiesFile;
import com.prupe.mcpatcher.mal.resource.TexturePackAPI;
import com.prupe.mcpatcher.mal.resource.TexturePackChangeHandler;
import com.prupe.mcpatcher.mal.tile.TileLoader;

/**
 * Loader for the OptiFine-style bettergrass.properties file (see doc/bettergrass.properties).
 * Holds the per-block enable flags, snow flags, the multilayer flag and the resolved override
 * icons. All fields fall back to vanilla behavior when the file or a texture is missing, so the
 * Better Grass feature keeps working exactly as before without any resource pack support.
 * OptiFine 风格 bettergrass.properties 文件的加载器（参见 doc/bettergrass.properties）。
 * 持有各方块的启用开关、积雪开关、多层开关以及解析后的贴图覆盖。
 * 文件或贴图缺失时全部回退到原版行为，因此没有资源包支持时 Better Grass 的表现与从前完全一致。
 */
public class BetterGrass {

    /** Block type index for grass. / 草方块的类型索引。 */
    public static final int TYPE_GRASS = 0;
    /** Block type index for mycelium. / 菌丝的类型索引。 */
    public static final int TYPE_MYCELIUM = 1;
    /** Block type index for podzol (dirt with metadata 2). / 灰化土（metadata 2 的泥土）的类型索引。 */
    public static final int TYPE_PODZOL = 2;

    private static final MCLogger logger = MCLogger.getLogger(MCLogger.Category.CONNECTED_TEXTURES, "BetterGrass");

    // Preferred MCPatcher location, with the documented OptiFine location as fallback.
    // 优先使用 MCPatcher 目录，其次回退到文档中给出的 OptiFine 目录。
    private static final ResourceLocation MCPATCHER_PROPERTIES = TexturePackAPI
        .newMCPatcherResourceLocation("bettergrass.properties");
    private static final ResourceLocation OPTIFINE_PROPERTIES = new ResourceLocation(
        "optifine/bettergrass.properties");

    private static final String BLOCKS_PREFIX = "blocks/";

    // Default texture values use the 1.7.10 atlas names so the defaults always resolve.
    // 默认贴图值使用 1.7.10 的图集名称，保证默认配置总能解析成功。
    private static final String DEFAULT_TEXTURE_GRASS = "blocks/grass_top";
    private static final String DEFAULT_TEXTURE_GRASS_SIDE = "blocks/grass_side";
    private static final String DEFAULT_TEXTURE_MYCELIUM = "blocks/mycelium_top";
    private static final String DEFAULT_TEXTURE_PODZOL = "blocks/dirt_podzol_top";
    private static final String DEFAULT_TEXTURE_SNOW = "blocks/snow";

    // Modern (1.13+) texture names used by the documented format, mapped to 1.7.10 atlas names.
    // 文档格式里使用的现代（1.13+）贴图名到 1.7.10 图集名的映射。
    private static final Map<String, String> MODERN_NAME_ALIASES = new HashMap<>();

    private static final boolean[] blockEnabled = new boolean[3];
    private static final boolean[] snowEnabled = new boolean[3];
    private static boolean grassMultilayer;

    private static final String[] topTextures = new String[3];
    private static String grassSideTexture;
    private static String snowTexture;

    // Resolved icons; null means "use the vanilla icon". / 解析后的图标；null 表示使用原版图标。
    private static final IIcon[] topIcons = new IIcon[3];
    private static IIcon grassSideIcon;
    private static IIcon snowIcon;

    private static ResourceLocation propertiesSource;
    private static TileLoader tileLoader;

    static {
        MODERN_NAME_ALIASES.put("grass_block_top", "grass_top");
        MODERN_NAME_ALIASES.put("grass_block_side", "grass_side");
        MODERN_NAME_ALIASES.put("grass_block_side_overlay", "grass_side_overlay");
        MODERN_NAME_ALIASES.put("podzol_top", "dirt_podzol_top");
        MODERN_NAME_ALIASES.put("podzol_side", "dirt_podzol_side");

        reset();

        TexturePackChangeHandler.register(new TexturePackChangeHandler("Better Grass", 3) {

            @Override
            public void initialize() {}

            @Override
            public void beforeChange() {
                reset();
                if (!RenderBlocksUtils.enableBetterGrass) {
                    return;
                }
                tileLoader = new TileLoader("textures/blocks", logger);
                loadProperties();
                // Textures outside blocks/ are not on the atlas yet and must be preloaded.
                // blocks/ 之外的贴图尚未进入图集，需要预加载。
                preloadTexture(topTextures[TYPE_GRASS]);
                preloadTexture(topTextures[TYPE_MYCELIUM]);
                preloadTexture(topTextures[TYPE_PODZOL]);
                preloadTexture(grassSideTexture);
                preloadTexture(snowTexture);
            }

            @Override
            public void afterChange() {
                if (!RenderBlocksUtils.enableBetterGrass) {
                    return;
                }
                topIcons[TYPE_GRASS] = resolveIcon(topTextures[TYPE_GRASS]);
                topIcons[TYPE_MYCELIUM] = resolveIcon(topTextures[TYPE_MYCELIUM]);
                topIcons[TYPE_PODZOL] = resolveIcon(topTextures[TYPE_PODZOL]);
                grassSideIcon = resolveIcon(grassSideTexture);
                snowIcon = resolveIcon(snowTexture);
            }
        });
    }

    /** Force class loading so the texture pack handler gets registered. / 触发类加载以注册材质包处理器。 */
    public static void init() {}

    public static boolean isBlockTypeEnabled(int type) {
        return blockEnabled[type];
    }

    public static boolean isSnowEnabled(int type) {
        return snowEnabled[type];
    }

    public static boolean isGrassMultilayer() {
        return grassMultilayer;
    }

    public static IIcon getTopIcon(int type, IIcon fallback) {
        return topIcons[type] == null ? fallback : topIcons[type];
    }

    public static IIcon getSnowIcon(IIcon fallback) {
        return snowIcon == null ? fallback : snowIcon;
    }

    /**
     * Side texture used for unconnected grass sides in multilayer mode; null keeps the vanilla
     * grass_side icon.
     * 多层模式下未连接草侧面使用的贴图；null 表示保留原版 grass_side 图标。
     */
    public static IIcon getGrassSideIcon() {
        return grassSideIcon;
    }

    /**
     * Overlay texture for multilayer mode (the biome-colored "texture.grass"); null keeps the
     * vanilla grass_side_overlay icon.
     * 多层模式的覆盖层贴图（受群系染色的 "texture.grass"）；null 表示保留原版 grass_side_overlay 图标。
     */
    public static IIcon getGrassOverlayIcon() {
        return topIcons[TYPE_GRASS];
    }

    private static void reset() {
        for (int type = 0; type < 3; type++) {
            blockEnabled[type] = true;
            snowEnabled[type] = true;
            topIcons[type] = null;
        }
        grassMultilayer = false;
        topTextures[TYPE_GRASS] = DEFAULT_TEXTURE_GRASS;
        topTextures[TYPE_MYCELIUM] = DEFAULT_TEXTURE_MYCELIUM;
        topTextures[TYPE_PODZOL] = DEFAULT_TEXTURE_PODZOL;
        grassSideTexture = DEFAULT_TEXTURE_GRASS_SIDE;
        snowTexture = DEFAULT_TEXTURE_SNOW;
        grassSideIcon = null;
        snowIcon = null;
        propertiesSource = null;
        tileLoader = null;
    }

    private static void loadProperties() {
        PropertiesFile properties = PropertiesFile.get(logger, MCPATCHER_PROPERTIES);
        if (properties == null) {
            properties = PropertiesFile.get(logger, OPTIFINE_PROPERTIES);
        }
        if (properties == null) {
            // No file at all: the OptiFine defaults set by reset() apply.
            // 完全没有文件时，直接沿用 reset() 设定的 OptiFine 默认值。
            return;
        }
        propertiesSource = properties.getResource();
        blockEnabled[TYPE_GRASS] = properties.getBoolean("grass", true);
        blockEnabled[TYPE_MYCELIUM] = properties.getBoolean("mycelium", true);
        blockEnabled[TYPE_PODZOL] = properties.getBoolean("podzol", true);
        snowEnabled[TYPE_GRASS] = properties.getBoolean("grass.snow", true);
        snowEnabled[TYPE_MYCELIUM] = properties.getBoolean("mycelium.snow", true);
        snowEnabled[TYPE_PODZOL] = properties.getBoolean("podzol.snow", true);
        grassMultilayer = properties.getBoolean("grass.multilayer", false);
        topTextures[TYPE_GRASS] = properties.getString("texture.grass", DEFAULT_TEXTURE_GRASS);
        topTextures[TYPE_MYCELIUM] = properties.getString("texture.mycelium", DEFAULT_TEXTURE_MYCELIUM);
        topTextures[TYPE_PODZOL] = properties.getString("texture.podzol", DEFAULT_TEXTURE_PODZOL);
        grassSideTexture = properties.getString("texture.grass_side", DEFAULT_TEXTURE_GRASS_SIDE);
        snowTexture = properties.getString("texture.snow", DEFAULT_TEXTURE_SNOW);
        properties.fine("loaded Better Grass settings");
    }

    private static void preloadTexture(String value) {
        // blocks/ values refer to sprites already registered on the blocks atlas; preloading
        // them would only create duplicate sprites and break icon name checks.
        // blocks/ 值指向已经注册在方块图集上的精灵；预加载它们只会产生重复精灵并破坏图标名称检查。
        if (MCPatcherUtils.isNullOrEmpty(value) || value.startsWith(BLOCKS_PREFIX) || propertiesSource == null) {
            return;
        }
        ResourceLocation resource = TileLoader.parseTileAddress(propertiesSource, value);
        if (resource != null) {
            tileLoader.preloadTile(resource, false);
        }
    }

    private static IIcon resolveIcon(String value) {
        if (MCPatcherUtils.isNullOrEmpty(value) || tileLoader == null) {
            return null;
        }
        IIcon icon;
        if (value.startsWith(BLOCKS_PREFIX)) {
            String name = value.substring(BLOCKS_PREFIX.length());
            icon = tileLoader.getIcon(name);
            if (icon == null) {
                String alias = MODERN_NAME_ALIASES.get(name);
                if (alias != null) {
                    icon = tileLoader.getIcon(alias);
                }
            }
        } else {
            icon = tileLoader.getIcon(TileLoader.parseTileAddress(propertiesSource, value));
        }
        if (icon == null) {
            // Custom values deserve a warning; defaults may legitimately stay unresolved when
            // no atlas hook is active (neither CTM nor Custom Colors enabled).
            // 自定义值需要警告；默认值在没有图集钩子（CTM 与自定义颜色都关闭）时无法解析属于正常情况。
            if (propertiesSource != null) {
                logger.warning("bettergrass texture %s not found", value);
            } else {
                logger.fine("bettergrass texture %s not resolved, using vanilla icon", value);
            }
        }
        return icon;
    }
}

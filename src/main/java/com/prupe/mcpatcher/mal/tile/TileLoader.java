package com.prupe.mcpatcher.mal.tile;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.mal.resource.BlendMethod;
import com.prupe.mcpatcher.mal.resource.TexturePackAPI;
import com.prupe.mcpatcher.mal.resource.TexturePackChangeHandler;

import decok.dfcdvadstf.optifuture.config.MCPatcherForgeConfig;

public class TileLoader {

    private static final MCLogger logger = MCLogger.getLogger(MCLogger.Category.TILESHEET);

    private static final List<TileLoader> loaders = new ArrayList<>();

    private static final boolean debugTextures = MCPatcherForgeConfig.instance().debugTextures;
    private static final Map<String, String> specialTextures = new HashMap<>();

    private static final TexturePackChangeHandler changeHandler;
    private static boolean changeHandlerCalled;

    private static final long MAX_TILESHEET_SIZE;

    protected final String mapName;
    protected final MCLogger subLogger;

    private TextureMap baseTextureMap;
    private final Map<String, TextureAtlasSprite> baseTexturesByName = new HashMap<>();
    private final Set<ResourceLocation> tilesToRegister = new HashSet<>();
    private final Map<ResourceLocation, BufferedImage> tileImages = new HashMap<>();
    // Tiles whose pixels are generated in memory rather than loaded from a resource pack file
    // 像素由内存生成而非从资源包文件加载的贴图（例如 ctm_compact 烘焙出的贴图）
    private final Set<ResourceLocation> generatedTiles = new HashSet<>();
    private final Map<String, IIcon> iconMap = new HashMap<>();

    static {
        long maxSize = 4096L;
        maxSize = Minecraft.getGLMaximumTextureSize();
        MAX_TILESHEET_SIZE = (maxSize * maxSize * 4) * 7 / 8;
        logger.config("max texture size is %dx%d (%.1fMB)", maxSize, maxSize, MAX_TILESHEET_SIZE / 1048576.0f);

        changeHandler = new TexturePackChangeHandler("Tilesheet API", 2) {

            @Override
            public void initialize() {}

            @Override
            public void beforeChange() {
                changeHandlerCalled = true;
                loaders.clear();
                specialTextures.clear();
            }

            @Override
            public void afterChange() {
                for (TileLoader loader : loaders) {
                    if (!loader.tilesToRegister.isEmpty()) {
                        loader.subLogger.warning(
                            "could not load all %s tiles (%d remaining)",
                            loader.mapName,
                            loader.tilesToRegister.size());
                        loader.tilesToRegister.clear();
                    }
                }
                changeHandlerCalled = false;
            }

            @Override
            public void afterChange2() {
                for (TileLoader loader : loaders) {
                    loader.finish();
                }
            }
        };
        TexturePackChangeHandler.register(changeHandler);
    }

    public static void registerIcons(TextureMap textureMap, String mapName, Map<String, TextureAtlasSprite> map) {
        mapName = mapName.replaceFirst("/$", "");
        logger.fine("before registerIcons(%s) %d icons", mapName, map.size());
        if (!changeHandlerCalled) {
            logger.severe("beforeChange was not called, invoking directly");
            changeHandler.beforeChange();
        }
        for (TileLoader loader : loaders) {
            if (loader.isForThisMap(mapName)) {
                if (loader.baseTextureMap == null) {
                    loader.baseTextureMap = textureMap;
                    loader.baseTexturesByName.putAll(map);
                }
                if (!loader.tilesToRegister.isEmpty()) {
                    loader.subLogger
                        .fine("adding icons to %s (%d remaining)", mapName, loader.tilesToRegister.size(), mapName);
                    while (!loader.tilesToRegister.isEmpty() && loader.registerOneIcon(textureMap, mapName, map)) {
                        // nothing
                    }
                    loader.subLogger.fine(
                        "done adding icons to %s (%d remaining)",
                        mapName,
                        loader.tilesToRegister.size(),
                        mapName);
                }
            }
        }
        logger.fine("after registerIcons(%s) %d icons", mapName, map.size());
    }

    public static String getOverridePath(String prefix, String basePath, String name, String ext) {
        String path;
        if (name.endsWith(".png")) {
            path = name.replaceFirst("^/", "")
                .replaceFirst("\\.[^.]+$", "") + ext;
        } else {
            path = basePath;
            if (!basePath.endsWith("/")) {
                path += "/";
            }
            path += name;
            path += ext;
        }
        path = prefix + path;
        logger.finer("getOverridePath(%s, %s, %s, %s) -> %s", prefix, basePath, name, ext, path);
        return path;
    }

    public static boolean isSpecialTexture(TextureMap map, String texture, String special) {
        return special.equals(texture) || special.equals(specialTextures.get(texture));
    }

    public static BufferedImage generateDebugTexture(String text, int width, int height, boolean alternate) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = image.getGraphics();
        graphics.setColor(alternate ? new Color(0, 255, 255, 128) : Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(alternate ? Color.RED : Color.BLACK);
        int ypos = 10;
        if (alternate) {
            ypos += height / 2;
        }
        int charsPerRow = width / 8;
        if (charsPerRow <= 0) {
            return image;
        }
        StringBuilder textBuilder = new StringBuilder(text);
        while (textBuilder.length() % charsPerRow != 0) {
            textBuilder.append(" ");
        }
        text = textBuilder.toString();
        while (ypos < height && !text.isEmpty()) {
            graphics.drawString(text.substring(0, charsPerRow), 1, ypos);
            ypos += graphics.getFont()
                .getSize();
            text = text.substring(charsPerRow);
        }
        return image;
    }

    public static void init() {}

    public TileLoader(String mapName, MCLogger logger) {
        this.mapName = mapName;
        subLogger = logger;
        loaders.add(this);
    }

    private static long getTextureSize(TextureAtlasSprite texture) {
        return texture == null ? 0 : 4 * texture.getIconWidth() * texture.getIconHeight();
    }

    private static long getTextureSize(Collection<TextureAtlasSprite> textures) {
        long size = 0;
        for (TextureAtlasSprite texture : textures) {
            size += getTextureSize(texture);
        }
        return size;
    }

    public static ResourceLocation getDefaultAddress(ResourceLocation propertiesAddress) {
        return TexturePackAPI.transformResourceLocation(propertiesAddress, ".properties", ".png");
    }

    public static ResourceLocation parseTileAddress(ResourceLocation propertiesAddress, String value) {
        return parseTileAddress(propertiesAddress, value, BlendMethod.ALPHA.getBlankResource());
    }

    public static ResourceLocation parseTileAddress(ResourceLocation propertiesAddress, String value,
        ResourceLocation blankResource) {
        if (value == null) {
            return null;
        }
        if (value.equals("blank")) {
            return blankResource;
        }
        // <skip> and <default> are placeholders used by overlay methods; both map to
        // "no override for this ctm index" in the icon-swap architecture
        // <skip> 与 <default> 是 overlay 方法使用的占位符；在图标替换架构下均表示"该索引不覆盖"
        if (value.equals("null") || value.equals("none")
            || value.equals("default")
            || value.equals("<skip>")
            || value.equals("<default>")
            || value.isEmpty()) {
            return null;
        }
        if (!value.endsWith(".png")) {
            value += ".png";
        }
        return TexturePackAPI.parseResourceLocation(propertiesAddress, value);
    }

    public boolean preloadTile(ResourceLocation resource, boolean alternate, String special) {
        if (tileImages.containsKey(resource)) {
            return true;
        }
        BufferedImage image = null;
        if (!debugTextures) {
            image = TexturePackAPI.getImage(resource);
            if (image == null) {
                subLogger.warning("missing %s", resource);
            }
        }
        if (image == null) {
            image = generateDebugTexture(resource.getResourcePath(), 64, 64, alternate);
        }
        tilesToRegister.add(resource);
        tileImages.put(resource, image);
        if (special != null) {
            specialTextures.put(resource.toString(), special);
        }
        return true;
    }

    public boolean preloadTile(ResourceLocation resource, boolean alternate) {
        return preloadTile(resource, alternate, null);
    }

    /**
     * Preload a procedurally generated tile image (e.g. tiles baked by the ctm_compact method).
     * The image is registered later through a custom sprite so no backing resource file is needed.
     * 预加载程序生成的贴图（例如 ctm_compact 方法烘焙出的贴图）。
     * 该图像稍后通过自定义精灵注册，因此不需要对应的资源包文件。
     */
    public boolean preloadGeneratedTile(ResourceLocation resource, BufferedImage image) {
        if (tileImages.containsKey(resource)) {
            return true;
        }
        if (image == null) {
            return false;
        }
        tilesToRegister.add(resource);
        tileImages.put(resource, image);
        generatedTiles.add(resource);
        return true;
    }

    /**
     * Get the preloaded image for a tile, or null if not preloaded.
     * 获取贴图对应的预加载图像，未预加载时返回 null。
     */
    public BufferedImage getTileImage(ResourceLocation resource) {
        return resource == null ? null : tileImages.get(resource);
    }

    protected boolean isForThisMap(String mapName) {
        return mapName.equals("textures") || mapName.startsWith(this.mapName);
    }

    private boolean registerDefaultIcon(String name) {
        if (name.startsWith(mapName) && name.endsWith(".png") && baseTextureMap != null) {
            String defaultName = name.substring(mapName.length())
                .replaceFirst("\\.png$", "");
            TextureAtlasSprite texture = baseTexturesByName.get(defaultName);
            if (texture != null) {
                subLogger.finer("%s -> existing icon %s", name, defaultName);
                iconMap.put(name, texture);
                return true;
            }
        }
        return false;
    }

    private boolean registerOneIcon(TextureMap textureMap, String mapName, Map<String, TextureAtlasSprite> map) {
        ResourceLocation resource = tilesToRegister.iterator()
            .next();
        String name = resource.toString();
        if (registerDefaultIcon(name)) {
            tilesToRegister.remove(resource);
            return true;
        }
        BufferedImage image = tileImages.get(resource);
        if (image == null) {
            subLogger.error("tile for %s unexpectedly missing", resource);
            tilesToRegister.remove(resource);
            return true;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        long currentSize = getTextureSize(map.values());
        long newSize = 4 * width * width;
        if (newSize + currentSize > MAX_TILESHEET_SIZE) {
            float sizeMB = (float) currentSize / 1048576.0f;
            if (currentSize <= 0) {
                subLogger.error("%s too big for any tilesheet (%.1fMB), dropping", name, sizeMB);
                tilesToRegister.remove(resource);
                return true;
            } else {
                subLogger.warning("%s nearly full (%.1fMB), will start a new tilesheet", mapName, sizeMB);
                return false;
            }
        }
        IIcon icon;
        if (generatedTiles.contains(resource)) {
            // Generated tiles have no backing resource file; register a custom sprite that
            // loads its pixels from the in-memory image via the hasCustomLoader/load hooks.
            // 生成贴图没有对应的资源文件，注册自定义精灵，通过 hasCustomLoader/load 钩子从内存图像加载像素。
            TextureAtlasSprite sprite = new GeneratedTextureAtlasSprite(name, image);
            if (textureMap.setTextureEntry(name, sprite)) {
                icon = sprite;
            } else {
                subLogger.warning("%s already registered, falling back to default icon loading", name);
                icon = textureMap.registerIcon(name);
            }
        } else {
            icon = textureMap.registerIcon(name);
        }
        map.put(name, (TextureAtlasSprite) icon);
        iconMap.put(name, icon);
        String extra = (width == height ? "" : ", " + (height / width) + " frames");
        subLogger.finer("%s -> %s icon %dx%d%s", name, mapName, width, width, extra);
        tilesToRegister.remove(resource);
        return true;
    }

    public void finish() {
        tilesToRegister.clear();
        tileImages.clear();
        generatedTiles.clear();
    }

    public IIcon getIcon(String name) {
        if (MCPatcherUtils.isNullOrEmpty(name)) {
            return null;
        }
        IIcon icon = iconMap.get(name);
        if (icon == null) {
            icon = baseTexturesByName.get(name);
        }
        return icon;
    }

    public IIcon getIcon(ResourceLocation resource) {
        return resource == null ? null : getIcon(resource.toString());
    }

    /**
     * Sprite backed by an in-memory image instead of a resource pack file.
     * TextureMap calls hasCustomLoader/load during loadTextureAtlas; returning false from
     * load() tells TextureMap to add this sprite to the stitcher (inverted Forge convention).
     * 由内存图像而非资源包文件支撑的精灵。
     * TextureMap 在 loadTextureAtlas 中调用 hasCustomLoader/load；load() 返回 false
     * 表示让 TextureMap 将此精灵加入拼合器（Forge 的反向约定）。
     */
    private static final class GeneratedTextureAtlasSprite extends TextureAtlasSprite {

        private final BufferedImage image;

        GeneratedTextureAtlasSprite(String name, BufferedImage image) {
            super(name);
            this.image = image;
        }

        @Override
        public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location) {
            return true;
        }

        @Override
        public boolean load(IResourceManager manager, ResourceLocation location) {
            // Match the anisotropic filtering setting used by the texture map; mipmaps are
            // generated later by TextureMap.loadTextureAtlas for all registered sprites.
            // 与贴图集使用的各向异性过滤设置保持一致；mipmap 稍后由
            // TextureMap.loadTextureAtlas 统一为所有已注册精灵生成。
            boolean anisotropic = Minecraft.getMinecraft().gameSettings.anisotropicFiltering > 1;
            loadSprite(new BufferedImage[] { image }, null, anisotropic);
            return false;
        }
    }
}

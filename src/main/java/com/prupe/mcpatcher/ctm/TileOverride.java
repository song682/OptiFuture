package com.prupe.mcpatcher.ctm;

import static com.prupe.mcpatcher.ctm.RenderBlockState.CONNECT_BY_BLOCK;
import static com.prupe.mcpatcher.ctm.RenderBlockState.CONNECT_BY_MATERIAL;
import static com.prupe.mcpatcher.ctm.RenderBlockState.CONNECT_BY_TILE;
import static com.prupe.mcpatcher.ctm.RenderBlockState.NORMALS;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.block.Block;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.mal.biome.BiomeAPI;
import com.prupe.mcpatcher.mal.block.BlockAPI;
import com.prupe.mcpatcher.mal.block.BlockStateMatcher;
import com.prupe.mcpatcher.mal.block.RenderPassAPI;
import com.prupe.mcpatcher.mal.resource.PropertiesFile;
import com.prupe.mcpatcher.mal.resource.TexturePackAPI;
import com.prupe.mcpatcher.mal.tile.TileLoader;

abstract class TileOverride implements ITileOverride {

    private static final MCLogger logger = MCLogger.getLogger(MCLogger.Category.CONNECTED_TEXTURES, "CTM");

    private static final int META_MASK = 0xffff;

    private final PropertiesFile properties;
    private final String baseFilename;
    private final TileLoader tileLoader;
    private final int renderPass;
    private final int weight;
    private final List<BlockStateMatcher> matchBlocks;
    private final Set<String> matchTiles;
    private final int defaultMetaMask;
    private final BlockFaceMatcher faceMatcher;
    private final int connectType;
    private final boolean innerSeams;
    private final BitSet biomes;
    private final BitSet height;

    private final List<ResourceLocation> tileNames = new ArrayList<>();
    protected IIcon[] icons;
    private int matchMetadata = META_MASK;

    static TileOverride create(ResourceLocation propertiesFile, TileLoader tileLoader) {
        if (propertiesFile == null) {
            return null;
        }
        PropertiesFile properties = PropertiesFile.get(logger, propertiesFile);
        if (properties == null) {
            return null;
        }

        String method = properties.getString("method", "default")
            .toLowerCase();
        TileOverride override = null;

        // Overlay methods render through the Better Glass OVERLAY pass by default;
        // force it here (before construction) unless the renderPass is set explicitly.
        // overlay 系列方法默认通过 Better Glass 的 OVERLAY 通道渲染；
        // 若未显式设置 renderPass，则在构造前强制注入。
        if (method.startsWith("overlay") && properties.getString("renderPass", "")
            .isEmpty()) {
            properties.setProperty("renderPass", "overlay");
        }

        switch (method) {
            case "default":
            case "glass":
            case "ctm":
                override = new TileOverrideImpl.CTM(properties, tileLoader);
                break;
            case "ctm_compact":
            case "compact":
                override = new TileOverrideImpl.CTMCompact(properties, tileLoader);
                break;
            case "random":
                override = new TileOverrideImpl.Random1(properties, tileLoader);
                if (override.getNumberOfTiles() == 1) {
                    override = new TileOverrideImpl.Fixed(properties, tileLoader);
                }
                break;
            case "fixed":
            case "static":
                override = new TileOverrideImpl.Fixed(properties, tileLoader);
                break;
            case "bookshelf":
            case "horizontal":
                override = new TileOverrideImpl.Horizontal(properties, tileLoader);
                break;
            case "horizontal+vertical":
            case "h+v":
                override = new TileOverrideImpl.HorizontalVertical(properties, tileLoader);
                break;
            case "vertical":
                override = new TileOverrideImpl.Vertical(properties, tileLoader);
                break;
            case "vertical+horizontal":
            case "v+h":
                override = new TileOverrideImpl.VerticalHorizontal(properties, tileLoader);
                break;
            case "sandstone":
            case "top":
                override = new TileOverrideImpl.Top(properties, tileLoader);
                break;
            case "repeat":
            case "pattern":
                override = new TileOverrideImpl.Repeat(properties, tileLoader);
                break;
            case "overlay":
                override = new TileOverrideImpl.Overlay(properties, tileLoader);
                break;
            case "overlay_ctm":
                override = new TileOverrideImpl.OverlayCTM(properties, tileLoader);
                break;
            case "overlay_random":
                override = new TileOverrideImpl.OverlayRandom(properties, tileLoader);
                break;
            case "overlay_repeat":
                override = new TileOverrideImpl.OverlayRepeat(properties, tileLoader);
                break;
            case "overlay_fixed":
                override = new TileOverrideImpl.OverlayFixed(properties, tileLoader);
                break;
            case "overlay_horizontal":
                override = new TileOverrideImpl.OverlayHorizontal(properties, tileLoader);
                break;
            case "overlay_vertical":
                override = new TileOverrideImpl.OverlayVertical(properties, tileLoader);
                break;
            case "overlay_horizontal+vertical":
            case "overlay_h+v":
                override = new TileOverrideImpl.OverlayHorizontalVertical(properties, tileLoader);
                break;
            case "overlay_vertical+horizontal":
            case "overlay_v+h":
                override = new TileOverrideImpl.OverlayVerticalHorizontal(properties, tileLoader);
                break;
            default:
                properties.error("unknown method \"%s\"", method);
                break;
        }

        if (override != null && !properties.valid()) {
            String status = override.checkTileMap();
            if (status != null) {
                override.properties.error("invalid %s tile map: %s", override.getMethod(), status);
            }
        }

        return override == null || override.isDisabled() ? null : override;
    }

    protected TileOverride(PropertiesFile properties, TileLoader tileLoader) {
        this.properties = properties;
        String texturesDirectory = properties.getResource()
            .getResourcePath()
            .replaceFirst("/[^/]*$", "");
        baseFilename = properties.getResource()
            .getResourcePath()
            .replaceFirst(".*/", "")
            .replaceFirst("\\.properties$", "");
        this.tileLoader = tileLoader;

        String renderPassStr = properties.getString("renderPass", "");
        renderPass = RenderPassAPI.instance.parseRenderPass(renderPassStr);
        if (renderPassStr.matches("\\d+") && renderPass >= 0 && renderPass <= RenderPassAPI.MAX_EXTRA_RENDER_PASS) {
            properties.warning(
                "renderPass=%s is deprecated, use renderPass=%s instead",
                renderPassStr,
                RenderPassAPI.instance.getRenderPassName(renderPass));
        }

        loadIcons();
        if (tileNames.isEmpty()) {
            properties.error("no images found in %s/", texturesDirectory);
        }

        String value;
        if (baseFilename.matches("block\\d+.*")) {
            value = baseFilename.replaceFirst("block(\\d+).*", "$1");
        } else {
            value = "";
        }
        matchBlocks = getBlockList(properties.getString("matchBlocks", value), properties.getString("metadata", ""));
        matchTiles = getTileList("matchTiles");
        if (matchBlocks.isEmpty() && matchTiles.isEmpty()) {
            matchTiles.add(baseFilename);
        }
        int bits = 0;
        for (int i : properties.getIntList("metadata", 0, 15, "0-15")) {
            bits |= 1 << i;
        }
        defaultMetaMask = bits;

        faceMatcher = BlockFaceMatcher.create(properties.getString("faces", ""));

        String connectType1 = properties.getString("connect", "")
            .toLowerCase();
        switch (connectType1) {
            case "":
                connectType = matchTiles.isEmpty() ? CONNECT_BY_BLOCK : CONNECT_BY_TILE;
                break;
            case "block":
                connectType = CONNECT_BY_BLOCK;
                break;
            case "tile":
                connectType = CONNECT_BY_TILE;
                break;
            case "material":
                connectType = CONNECT_BY_MATERIAL;
                break;
            default:
                properties.error("invalid connect type %s", connectType1);
                connectType = CONNECT_BY_BLOCK;
                break;
        }

        innerSeams = properties.getBoolean("innerSeams", false);

        String biomeList = properties.getString("biomes", "");
        if (biomeList.isEmpty()) {
            biomes = null;
        } else {
            biomes = new BitSet();
            BiomeAPI.parseBiomeList(biomeList, biomes);
        }

        height = BiomeAPI.getHeightListProperty(properties, "");

        if (renderPass > RenderPassAPI.MAX_EXTRA_RENDER_PASS) {
            properties.error("invalid renderPass %s", renderPassStr);
        } else if (renderPass >= 0 && !matchTiles.isEmpty()) {
            properties.error(
                "renderPass=%s must be block-based not tile-based",
                RenderPassAPI.instance.getRenderPassName(renderPass));
        }

        weight = properties.getInt("weight", 0);
    }

    private boolean addIcon(ResourceLocation resource) {
        tileNames.add(resource);
        return tileLoader.preloadTile(resource, renderPass > RenderPassAPI.MAX_BASE_RENDER_PASS);
    }

    private void loadIcons() {
        tileNames.clear();
        String tileList = properties.getString("tiles", "");
        ResourceLocation blankResource = RenderPassAPI.instance.getBlankResource(renderPass);
        if (tileList.isEmpty()) {
            for (int i = 0;; i++) {
                ResourceLocation resource = TileLoader
                    .parseTileAddress(properties.getResource(), String.valueOf(i), blankResource);
                if (!TexturePackAPI.hasResource(resource)) {
                    break;
                }
                if (!addIcon(resource)) {
                    break;
                }
            }
        } else {
            Pattern range = Pattern.compile("(\\d+)-(\\d+)");
            for (String token : tileList.split("\\s+")) {
                Matcher matcher = range.matcher(token);
                if (token.isEmpty()) {
                    // nothing
                } else if (matcher.matches()) {
                    try {
                        int from = Integer.parseInt(matcher.group(1));
                        int to = Integer.parseInt(matcher.group(2));
                        for (int i = from; i <= to; i++) {
                            ResourceLocation resource = TileLoader
                                .parseTileAddress(properties.getResource(), String.valueOf(i), blankResource);
                            if (TexturePackAPI.hasResource(resource)) {
                                addIcon(resource);
                            } else {
                                properties.warning("could not find image %s", resource);
                                tileNames.add(null);
                            }
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                } else {
                    ResourceLocation resource = TileLoader
                        .parseTileAddress(properties.getResource(), token, blankResource);
                    if (resource == null) {
                        tileNames.add(null);
                    } else if (TexturePackAPI.hasResource(resource)) {
                        addIcon(resource);
                    } else {
                        properties.warning("could not find image %s", resource);
                        tileNames.add(null);
                    }
                }
            }
        }
    }

    /**
     * Parse a whitespace-separated block list into matchers. Also used by subclasses
     * to parse additional block list properties (e.g. connectBlocks for method=overlay).
     * 将空格分隔的方块列表解析为匹配器。子类也用它解析其他方块列表属性
     * （例如 method=overlay 的 connectBlocks）。
     */
    protected List<BlockStateMatcher> getBlockList(String property, String defaultMetadata) {
        List<BlockStateMatcher> blocks = new ArrayList<>();
        if (!MCPatcherUtils.isNullOrEmpty(defaultMetadata)) {
            defaultMetadata = ':' + defaultMetadata;
        }
        for (String token : property.split("\\s+")) {
            if (token.isEmpty()) {
                // nothing
            } else if (token.matches("\\d+-\\d+")) {
                for (int id : MCPatcherUtils.parseIntegerList(token, 0, 65535)) {
                    BlockStateMatcher matcher = BlockAPI.createMatcher(properties, id + defaultMetadata);
                    if (matcher == null) {
                        properties.warning("unknown block id %d", id);
                    } else {
                        blocks.add(matcher);
                    }
                }
            } else {
                BlockStateMatcher matcher = BlockAPI.createMatcher(properties, token + defaultMetadata);
                if (matcher == null) {
                    properties.warning("unknown block %s", token);
                } else {
                    blocks.add(matcher);
                }
            }
        }
        for (BlockStateMatcher matcher : blocks) {
            matcher.setData(this);
        }
        return blocks;
    }

    /**
     * Parse a whitespace-separated tile name list property. Also used by subclasses
     * to parse additional tile list properties (e.g. connectTiles for method=overlay).
     * 解析空格分隔的贴图名列表属性。子类也用它解析其他贴图列表属性
     * （例如 method=overlay 的 connectTiles）。
     */
    protected Set<String> getTileList(String key) {
        Set<String> list = new HashSet<>();
        String property = properties.getString(key, "");
        for (String token : property.split("\\s+")) {
            if (token.isEmpty()) {
                // nothing
            } else if (token.contains("/")) {
                if (!token.endsWith(".png")) {
                    token += ".png";
                }
                ResourceLocation resource = TexturePackAPI.parseResourceLocation(properties.getResource(), token);
                if (resource != null) {
                    list.add(resource.toString());
                }
            } else {
                list.add(token);
            }
        }
        return list;
    }

    protected int getNumberOfTiles() {
        return tileNames.size();
    }

    /**
     * Access to the mutable tile name list for subclasses that rewrite the tile map
     * after loading (e.g. ctm_compact baking 5 source tiles into 47 tiles).
     * 供加载后需要重写贴图表的子类访问可变贴图名列表
     * （例如 ctm_compact 将 5 张基础贴图烘焙为 47 张）。
     */
    protected List<ResourceLocation> getTileNames() {
        return tileNames;
    }

    String checkTileMap() {
        return null;
    }

    boolean requiresFace() {
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s[%s] (%d tiles)", getMethod(), properties, getNumberOfTiles());
    }

    @Override
    public final void registerIcons() {
        icons = new IIcon[tileNames.size()];
        for (int i = 0; i < icons.length; i++) {
            icons[i] = tileLoader.getIcon(tileNames.get(i));
        }
    }

    @Override
    final public boolean isDisabled() {
        return !properties.valid();
    }

    @Override
    final public List<BlockStateMatcher> getMatchingBlocks() {
        return matchBlocks;
    }

    @Override
    final public Set<String> getMatchingTiles() {
        if (MCPatcherUtils.isNullOrEmpty(matchTiles)) {
            return null;
        } else {
            return new HashSet<>(matchTiles);
        }
    }

    @Override
    final public int getRenderPass() {
        return renderPass;
    }

    @Override
    final public int getWeight() {
        return weight;
    }

    @Override
    public int compareTo(ITileOverride o) {
        int result = o.getWeight() - getWeight();
        if (result != 0) {
            return result;
        }
        if (o instanceof TileOverride) {
            return baseFilename.compareTo(((TileOverride) o).baseFilename);
        } else {
            return -1;
        }
    }

    final boolean shouldConnect(RenderBlockState renderBlockState, IIcon icon, int relativeDirection) {
        return shouldConnect(
            renderBlockState,
            icon,
            renderBlockState.getOffset(renderBlockState.getBlockFace(), relativeDirection));
    }

    final boolean shouldConnect(RenderBlockState renderBlockState, IIcon icon, int blockFace, int relativeDirection) {
        return shouldConnect(renderBlockState, icon, renderBlockState.getOffset(blockFace, relativeDirection));
    }

    private boolean shouldConnect(RenderBlockState renderBlockState, IIcon icon, int[] offset) {
        IBlockAccess blockAccess = renderBlockState.getBlockAccess();
        Block block = renderBlockState.getBlock();
        int i = renderBlockState.getI();
        int j = renderBlockState.getJ();
        int k = renderBlockState.getK();
        i += offset[0];
        j += offset[1];
        k += offset[2];
        Block neighbor = BlockAPI.getBlockAt(blockAccess, i, j, k);
        if (neighbor == null) {
            return false;
        }
        if (block == neighbor) {
            BlockStateMatcher filter = renderBlockState.getFilter();
            if (filter != null && !filter.match(blockAccess, i, j, k)) {
                return false;
            }
        }
        if (innerSeams) {
            int blockFace = renderBlockState.getBlockFace();
            if (blockFace >= 0) {
                int[] normal = NORMALS[blockFace];
                if (!BlockAPI.shouldSideBeRendered(
                    neighbor,
                    blockAccess,
                    i + normal[0],
                    j + normal[1],
                    k + normal[2],
                    blockFace)) {
                    return false;
                }
            }
        }
        switch (connectType) {
            case CONNECT_BY_TILE:
                return renderBlockState.shouldConnectByTile(neighbor, icon, i, j, k);
            case CONNECT_BY_BLOCK:
                return renderBlockState.shouldConnectByBlock(neighbor, i, j, k);
            case CONNECT_BY_MATERIAL:
                return ((decok.dfcdvadstf.optifuture.mixins.early.at.AccessorBlock) block).getBlockMaterial()
                    == ((decok.dfcdvadstf.optifuture.mixins.early.at.AccessorBlock) neighbor).getBlockMaterial();
            default:
                return false;
        }
    }

    @Override
    public final IIcon getTileWorld(RenderBlockState renderBlockState, IIcon origIcon) {
        if (icons == null) {
            properties.error("no images loaded, disabling");
            return null;
        }
        IBlockAccess blockAccess = renderBlockState.getBlockAccess();
        Block block = renderBlockState.getBlock();
        int i = renderBlockState.getI();
        int j = renderBlockState.getJ();
        int k = renderBlockState.getK();
        if (renderBlockState.getBlockFace() < 0 && requiresFace()) {
            properties.warning(
                "method=%s is not supported for non-standard block %s:%d @ %d %d %d",
                getMethod(),
                BlockAPI.getBlockName(block),
                BlockAPI.getMetadataAt(blockAccess, i, j, k),
                i,
                j,
                k);
            return null;
        }
        if (block == null || RenderPassAPI.instance.skipThisRenderPass(block, renderPass)) {
            return null;
        }
        BlockStateMatcher filter = renderBlockState.getFilter();
        if (filter != null && !filter.match(blockAccess, i, j, k)) {
            return null;
        }
        if (faceMatcher != null && !faceMatcher.match(renderBlockState)) {
            return null;
        }
        if (height != null && !height.get(j)) {
            return null;
        }
        if (biomes != null && !biomes.get(BiomeAPI.getBiomeIDAt(blockAccess, i, j, k))) {
            return null;
        }
        return getTileWorld_Impl(renderBlockState, origIcon);
    }

    @Override
    public final IIcon getTileHeld(RenderBlockState renderBlockState, IIcon origIcon) {
        if (icons == null) {
            properties.error("no images loaded, disabling");
            return null;
        }
        Block block = renderBlockState.getBlock();
        if (block == null || RenderPassAPI.instance.skipThisRenderPass(block, renderPass)) {
            return null;
        }
        int face = renderBlockState.getTextureFace();
        if (face < 0 && requiresFace()) {
            properties.warning("method=%s is not supported for non-standard block %s", getMethod(), renderBlockState);
            return null;
        }
        if (height != null || biomes != null) {
            return null;
        }
        if (faceMatcher != null && !faceMatcher.match(renderBlockState)) {
            return null;
        }
        return getTileHeld_Impl(renderBlockState, origIcon);
    }

    abstract String getMethod();

    abstract IIcon getTileWorld_Impl(RenderBlockState renderBlockState, IIcon origIcon);

    abstract IIcon getTileHeld_Impl(RenderBlockState renderBlockState, IIcon origIcon);
}

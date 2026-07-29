package com.prupe.mcpatcher.ctm;

import static com.prupe.mcpatcher.ctm.RenderBlockState.EAST_FACE;
import static com.prupe.mcpatcher.ctm.RenderBlockState.NORMALS;
import static com.prupe.mcpatcher.ctm.RenderBlockState.NORTH_FACE;
import static com.prupe.mcpatcher.ctm.RenderBlockState.REL_D;
import static com.prupe.mcpatcher.ctm.RenderBlockState.REL_DL;
import static com.prupe.mcpatcher.ctm.RenderBlockState.REL_DR;
import static com.prupe.mcpatcher.ctm.RenderBlockState.REL_L;
import static com.prupe.mcpatcher.ctm.RenderBlockState.REL_R;
import static com.prupe.mcpatcher.ctm.RenderBlockState.REL_U;
import static com.prupe.mcpatcher.ctm.RenderBlockState.REL_UL;
import static com.prupe.mcpatcher.ctm.RenderBlockState.REL_UR;
import static com.prupe.mcpatcher.ctm.RenderBlockState.TOP_FACE;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;

import com.prupe.mcpatcher.mal.block.BlockAPI;
import com.prupe.mcpatcher.mal.block.BlockStateMatcher;
import com.prupe.mcpatcher.mal.resource.PropertiesFile;
import com.prupe.mcpatcher.mal.tile.TileLoader;
import com.prupe.mcpatcher.mal.util.WeightedIndex;

class TileOverrideImpl {

    static class CTM extends TileOverride {

        // Index into this array is formed from these bit values:
        // 128 64 32
        // 1 * 16
        // 2 4 8
        private static final int[] neighborMap = new int[] { 0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15, 1, 2,
            1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14, 0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15, 1, 2,
            1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14, 36, 17, 36, 17, 24, 19, 24, 43, 36, 17, 36, 17, 24, 19, 24,
            43, 16, 18, 16, 18, 6, 46, 6, 21, 16, 18, 16, 18, 28, 9, 28, 22, 36, 17, 36, 17, 24, 19, 24, 43, 36, 17, 36,
            17, 24, 19, 24, 43, 37, 40, 37, 40, 30, 8, 30, 34, 37, 40, 37, 40, 25, 23, 25, 45, 0, 3, 0, 3, 12, 5, 12,
            15, 0, 3, 0, 3, 12, 5, 12, 15, 1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14, 0, 3, 0, 3, 12, 5, 12,
            15, 0, 3, 0, 3, 12, 5, 12, 15, 1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14, 36, 39, 36, 39, 24, 41,
            24, 27, 36, 39, 36, 39, 24, 41, 24, 27, 16, 42, 16, 42, 6, 20, 6, 10, 16, 42, 16, 42, 28, 35, 28, 44, 36,
            39, 36, 39, 24, 41, 24, 27, 36, 39, 36, 39, 24, 41, 24, 27, 37, 38, 37, 38, 30, 11, 30, 32, 37, 38, 37, 38,
            25, 33, 25, 26, };

        CTM(PropertiesFile properties, TileLoader tileLoader) {
            super(properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "ctm";
        }

        @Override
        String checkTileMap() {
            if (getNumberOfTiles() >= 47) {
                return null;
            } else {
                return "requires at least 47 tiles";
            }
        }

        @Override
        boolean requiresFace() {
            return true;
        }

        @Override
        IIcon getTileWorld_Impl(RenderBlockState renderBlockState, IIcon origIcon) {
            int neighborBits = 0;
            for (int bit = 0; bit < 8; bit++) {
                if (shouldConnect(renderBlockState, origIcon, bit)) {
                    neighborBits |= (1 << bit);
                }
            }
            return icons[neighborMap[neighborBits]];
        }

        @Override
        IIcon getTileHeld_Impl(RenderBlockState renderBlockState, IIcon origIcon) {
            return icons[0];
        }
    }

    /**
     * Compact 8-way connected textures (method=ctm_compact).
     * Bakes the full 47-tile CTM layout from 5 source tiles at load time using
     * OptiFine's compact composition table, then reuses the standard CTM
     * neighbor-lookup logic at render time.
     * 紧凑型 8 向连接纹理（method=ctm_compact）。
     * 加载期按 OptiFine 的紧凑合成表用 5 张基础贴图烘焙出完整的 47 张 CTM 贴图，
     * 运行时复用标准 CTM 的邻居查表逻辑。
     */
    final static class CTMCompact extends CTM {

        // Source tile meanings / 基础贴图含义:
        // 0 = no connections (borders on all sides) / 无连接（四周均有边框）
        // 1 = fully connected (no borders) / 全连接（无边框）
        // 2 = connected up and down (borders left/right) / 上下连接（左右有边框）
        // 3 = connected left and right (borders top/bottom) / 左右连接（上下有边框）
        // 4 = corner transitions / 拐角过渡
        //
        // Quadrant composition table indexed by ctm index (0-46); each entry is
        // {upLeft, upRight, downLeft, downRight} referring to source tiles 0-4.
        // Derived from OptiFine's ConnectedTexturesCompact composition rules.
        // 按 ctm 索引（0-46）索引的四象限合成表；每项为 {左上, 右上, 左下, 右下}，
        // 引用基础贴图 0-4。源自 OptiFine ConnectedTexturesCompact 的合成规则。
        private static final int[][] COMPACT_QUADRANT_MAP = new int[][] {
            // @formatter:off
            { 0, 0, 0, 0 }, // 0
            { 0, 3, 0, 3 }, // 1: H(0,3)
            { 3, 3, 3, 3 }, // 2
            { 3, 0, 3, 0 }, // 3: H(3,0)
            { 0, 3, 2, 4 }, // 4
            { 3, 0, 4, 2 }, // 5
            { 2, 4, 2, 4 }, // 6
            { 3, 3, 4, 4 }, // 7
            { 4, 1, 4, 4 }, // 8
            { 4, 4, 4, 1 }, // 9
            { 1, 4, 1, 4 }, // 10
            { 1, 1, 4, 4 }, // 11
            { 0, 0, 2, 2 }, // 12: V(0,2)
            { 0, 3, 2, 1 }, // 13
            { 3, 3, 1, 1 }, // 14: V(3,1)
            { 3, 0, 1, 2 }, // 15
            { 2, 4, 0, 3 }, // 16
            { 4, 2, 3, 0 }, // 17
            { 4, 4, 3, 3 }, // 18
            { 4, 2, 4, 2 }, // 19
            { 1, 4, 4, 4 }, // 20
            { 4, 4, 1, 4 }, // 21
            { 4, 4, 1, 1 }, // 22
            { 4, 1, 4, 1 }, // 23
            { 2, 2, 2, 2 }, // 24
            { 2, 1, 2, 1 }, // 25: H(2,1)
            { 1, 1, 1, 1 }, // 26
            { 1, 2, 1, 2 }, // 27: H(1,2)
            { 2, 4, 2, 1 }, // 28
            { 3, 3, 1, 4 }, // 29
            { 2, 1, 2, 4 }, // 30
            { 3, 3, 4, 1 }, // 31
            { 1, 1, 1, 4 }, // 32
            { 1, 1, 4, 1 }, // 33
            { 4, 1, 1, 4 }, // 34
            { 1, 4, 4, 1 }, // 35
            { 2, 2, 0, 0 }, // 36: V(2,0)
            { 2, 1, 0, 3 }, // 37
            { 1, 1, 3, 3 }, // 38: V(1,3)
            { 1, 2, 3, 0 }, // 39
            { 4, 1, 3, 3 }, // 40
            { 1, 2, 4, 2 }, // 41
            { 1, 4, 3, 3 }, // 42
            { 4, 2, 1, 2 }, // 43
            { 1, 4, 1, 1 }, // 44
            { 4, 1, 1, 1 }, // 45
            { 4, 4, 4, 4 }, // 46
            // @formatter:on
        };

        private static final int NUM_COMPACT_TILES = 47;

        private final PropertiesFile properties;
        private final TileLoader tileLoader;
        // Number of tiles listed in the properties file before baking replaces the tile map
        // 烘焙替换贴图表之前，properties 文件中定义的贴图数量
        private final int sourceTileCount;

        CTMCompact(PropertiesFile properties, TileLoader tileLoader) {
            super(properties, tileLoader);
            this.properties = properties;
            this.tileLoader = tileLoader;
            sourceTileCount = getNumberOfTiles();
            if (sourceTileCount < 5) {
                properties.error("ctm_compact requires at least 5 tiles");
            } else if (properties.valid()) {
                bakeCompactTiles();
            }
        }

        @Override
        String getMethod() {
            return "ctm_compact";
        }

        @Override
        String checkTileMap() {
            if (sourceTileCount >= 5) {
                return null;
            } else {
                return "requires at least 5 tiles";
            }
        }

        /**
         * Replace the 5-tile source map with 47 baked tiles matching the standard CTM layout.
         * Whole-tile entries and ctm.&lt;index&gt;=&lt;tile&gt; replacements reuse the original
         * tiles directly (keeping animation support); mixed entries are composed per quadrant.
         * 将 5 张基础贴图替换为符合标准 CTM 布局的 47 张烘焙贴图。
         * 整张引用及 ctm.&lt;index&gt;=&lt;tile&gt; 替换项直接复用原贴图（保留动画支持）；
         * 混合项按四象限合成。
         */
        private void bakeCompactTiles() {
            List<ResourceLocation> tileNames = getTileNames();
            BufferedImage[] sources = new BufferedImage[tileNames.size()];
            for (int i = 0; i < sources.length; i++) {
                sources[i] = tileLoader.getTileImage(tileNames.get(i));
            }
            for (int i = 0; i < 5; i++) {
                if (sources[i] == null) {
                    properties.error("ctm_compact source tile %d is missing", i);
                    return;
                }
            }
            int[] replacements = parseReplacementMap(tileNames.size());
            ResourceLocation propertiesResource = properties.getResource();
            String basePath = propertiesResource.getResourcePath()
                .replaceFirst("\\.properties$", "");
            List<ResourceLocation> baked = new ArrayList<>(NUM_COMPACT_TILES);
            for (int ctmIndex = 0; ctmIndex < NUM_COMPACT_TILES; ctmIndex++) {
                int[] quads = COMPACT_QUADRANT_MAP[ctmIndex];
                if (replacements[ctmIndex] >= 0) {
                    // Explicit replacement tile from the properties file
                    // properties 文件中显式指定的替换贴图
                    baked.add(tileNames.get(replacements[ctmIndex]));
                } else if (quads[0] == quads[1] && quads[0] == quads[2] && quads[0] == quads[3]) {
                    // Whole-tile entry: reuse the source tile directly
                    // 整张引用：直接复用原贴图
                    baked.add(tileNames.get(quads[0]));
                } else {
                    ResourceLocation name = new ResourceLocation(
                        propertiesResource.getResourceDomain(),
                        basePath + "_compact_" + ctmIndex + ".png");
                    BufferedImage image = composeQuadrants(sources, quads);
                    if (tileLoader.preloadGeneratedTile(name, image)) {
                        baked.add(name);
                    } else {
                        properties.warning("could not register baked tile %s", name);
                        baked.add(tileNames.get(quads[0]));
                    }
                }
            }
            tileNames.clear();
            tileNames.addAll(baked);
        }

        /**
         * Parse ctm.&lt;ctm_index&gt;=&lt;tile_index&gt; properties overriding individual baked tiles.
         * 解析 ctm.&lt;ctm_index&gt;=&lt;tile_index&gt; 属性，用于覆盖单张烘焙贴图。
         */
        private int[] parseReplacementMap(int numTiles) {
            int[] replacements = new int[NUM_COMPACT_TILES];
            Arrays.fill(replacements, -1);
            Pattern keyPattern = Pattern.compile("^ctm\\.(\\d+)$");
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                Matcher matcher = keyPattern.matcher(entry.getKey());
                if (!matcher.matches()) {
                    continue;
                }
                int ctmIndex = Integer.parseInt(matcher.group(1));
                if (ctmIndex >= NUM_COMPACT_TILES) {
                    properties.warning("invalid ctm index %d, must be 0-46", ctmIndex);
                    continue;
                }
                try {
                    int tileIndex = Integer.parseInt(
                        entry.getValue()
                            .trim());
                    if (tileIndex >= 0 && tileIndex < numTiles) {
                        replacements[ctmIndex] = tileIndex;
                    } else {
                        properties.warning("ctm.%d: tile index %d out of range", ctmIndex, tileIndex);
                    }
                } catch (NumberFormatException e) {
                    properties.warning("ctm.%d: invalid tile index %s", ctmIndex, entry.getValue());
                }
            }
            return replacements;
        }

        /**
         * Compose a full tile from the four quadrants of the given source tiles.
         * Only the first animation frame of each source is used.
         * 用给定基础贴图的四个象限合成一张完整贴图。动画贴图仅使用第一帧。
         */
        private static BufferedImage composeQuadrants(BufferedImage[] sources, int[] quads) {
            BufferedImage first = sources[quads[0]];
            int size = Math.min(first.getWidth(), first.getHeight());
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            int half = size / 2;
            drawQuadrant(graphics, sources[quads[0]], 0, 0, half, half, 0.0, 0.0, 0.5, 0.5);
            drawQuadrant(graphics, sources[quads[1]], half, 0, size, half, 0.5, 0.0, 1.0, 0.5);
            drawQuadrant(graphics, sources[quads[2]], 0, half, half, size, 0.0, 0.5, 0.5, 1.0);
            drawQuadrant(graphics, sources[quads[3]], half, half, size, size, 0.5, 0.5, 1.0, 1.0);
            graphics.dispose();
            return image;
        }

        private static void drawQuadrant(Graphics2D graphics, BufferedImage source, int dx1, int dy1, int dx2,
            int dy2, double sx1, double sy1, double sx2, double sy2) {
            // Source coordinates are scaled to the source frame size so differently
            // sized tiles can still be combined into one baked tile.
            // 源坐标按源帧尺寸缩放，使不同尺寸的贴图也能合成到同一张烘焙贴图中。
            int frame = Math.min(source.getWidth(), source.getHeight());
            graphics.drawImage(
                source,
                dx1,
                dy1,
                dx2,
                dy2,
                (int) (sx1 * frame),
                (int) (sy1 * frame),
                (int) (sx2 * frame),
                (int) (sy2 * frame),
                null);
        }
    }

    static class Horizontal extends TileOverride {

        // Index into this array is formed from these bit values:
        // 1 * 2
        private static final int[] neighborMap = new int[] { 3, 2, 0, 1, };

        Horizontal(PropertiesFile properties, TileLoader tileLoader) {
            super(properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "horizontal";
        }

        @Override
        String checkTileMap() {
            if (getNumberOfTiles() == 4) {
                return null;
            } else {
                return "requires exactly 4 tiles";
            }
        }

        @Override
        IIcon getTileWorld_Impl(RenderBlockState renderBlockState, IIcon origIcon) {
            int face = renderBlockState.getFaceForHV();
            if (face < 0) {
                return null;
            }
            int neighborBits = 0;
            if (shouldConnect(renderBlockState, origIcon, REL_L)) {
                neighborBits |= 1;
            }
            if (shouldConnect(renderBlockState, origIcon, REL_R)) {
                neighborBits |= 2;
            }
            return icons[neighborMap[neighborBits]];
        }

        @Override
        IIcon getTileHeld_Impl(RenderBlockState renderBlockState, IIcon origIcon) {
            return icons[3];
        }
    }

    static class HorizontalVertical extends Horizontal {

        // Index into this array is formed from these bit values:
        // 32 16 8
        // *
        // 1 2 4
        private static final int[] neighborMap = new int[] { 3, 3, 6, 3, 3, 3, 3, 3, 3, 3, 6, 3, 3, 3, 3, 3, 4, 4, 5, 4,
            4, 4, 4, 4, 3, 3, 6, 3, 3, 3, 3, 3, 3, 3, 6, 3, 3, 3, 3, 3, 3, 3, 6, 3, 3, 3, 3, 3, 3, 3, 6, 3, 3, 3, 3, 3,
            3, 3, 6, 3, 3, 3, 3, 3, };

        HorizontalVertical(PropertiesFile properties, TileLoader tileLoader) {
            super(properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "horizontal+vertical";
        }

        @Override
        String checkTileMap() {
            if (getNumberOfTiles() == 7) {
                return null;
            } else {
                return "requires exactly 7 tiles";
            }
        }

        @Override
        IIcon getTileWorld_Impl(RenderBlockState renderBlockState, IIcon origIcon) {
            IIcon icon = super.getTileWorld_Impl(renderBlockState, origIcon);
            if (icon != icons[3]) {
                return icon;
            }
            int neighborBits = 0;
            if (shouldConnect(renderBlockState, origIcon, REL_DL)) {
                neighborBits |= 1;
            }
            if (shouldConnect(renderBlockState, origIcon, REL_D)) {
                neighborBits |= 2;
            }
            if (shouldConnect(renderBlockState, origIcon, REL_DR)) {
                neighborBits |= 4;
            }
            if (shouldConnect(renderBlockState, origIcon, REL_UR)) {
                neighborBits |= 8;
            }
            if (shouldConnect(renderBlockState, origIcon, REL_U)) {
                neighborBits |= 16;
            }
            if (shouldConnect(renderBlockState, origIcon, REL_UL)) {
                neighborBits |= 32;
            }
            return icons[neighborMap[neighborBits]];
        }
    }

    static class Vertical extends TileOverride {

        // Index into this array is formed from these bit values:
        // 2
        // *
        // 1
        private static final int[] neighborMap = new int[] { 3, 2, 0, 1, };

        Vertical(PropertiesFile properties, TileLoader tileLoader) {
            super(properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "vertical";
        }

        @Override
        String checkTileMap() {
            if (getNumberOfTiles() == 4) {
                return null;
            } else {
                return "requires exactly 4 tiles";
            }
        }

        @Override
        IIcon getTileWorld_Impl(RenderBlockState renderBlockState, IIcon origIcon) {
            int face = renderBlockState.getFaceForHV();
            if (face < 0) {
                return null;
            }
            int neighborBits = 0;
            if (shouldConnect(renderBlockState, origIcon, REL_D)) {
                neighborBits |= 1;
            }
            if (shouldConnect(renderBlockState, origIcon, REL_U)) {
                neighborBits |= 2;
            }
            return icons[neighborMap[neighborBits]];
        }

        @Override
        IIcon getTileHeld_Impl(RenderBlockState renderBlockState, IIcon origIcon) {
            return icons[3];
        }
    }

    static class VerticalHorizontal extends Vertical {

        // Index into this array is formed from these bit values:
        // 32 16
        // 1 * 8
        // 2 4
        private static final int[] neighborMap = new int[] { 3, 6, 3, 3, 3, 6, 3, 3, 4, 5, 4, 4, 3, 6, 3, 3, 3, 6, 3, 3,
            3, 6, 3, 3, 3, 6, 3, 3, 3, 6, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
            3, 3, 3, 3, 3, 3, 3, 3, };

        VerticalHorizontal(PropertiesFile properties, TileLoader tileLoader) {
            super(properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "vertical+horizontal";
        }

        @Override
        String checkTileMap() {
            if (getNumberOfTiles() == 7) {
                return null;
            } else {
                return "requires exactly 7 tiles";
            }
        }

        @Override
        IIcon getTileWorld_Impl(RenderBlockState renderBlockState, IIcon origIcon) {
            IIcon icon = super.getTileWorld_Impl(renderBlockState, origIcon);
            if (icon != icons[3]) {
                return icon;
            }
            int neighborBits = 0;
            if (shouldConnect(renderBlockState, origIcon, REL_L)) {
                neighborBits |= 1;
            }
            if (shouldConnect(renderBlockState, origIcon, REL_DL)) {
                neighborBits |= 2;
            }
            if (shouldConnect(renderBlockState, origIcon, REL_DR)) {
                neighborBits |= 4;
            }
            if (shouldConnect(renderBlockState, origIcon, REL_R)) {
                neighborBits |= 8;
            }
            if (shouldConnect(renderBlockState, origIcon, REL_UR)) {
                neighborBits |= 16;
            }
            if (shouldConnect(renderBlockState, origIcon, REL_UL)) {
                neighborBits |= 32;
            }
            return icons[neighborMap[neighborBits]];
        }
    }

    final static class Top extends TileOverride {

        Top(PropertiesFile properties, TileLoader tileLoader) {
            super(properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "top";
        }

        @Override
        String checkTileMap() {
            if (getNumberOfTiles() == 1) {
                return null;
            } else {
                return "requires exactly 1 tile";
            }
        }

        @Override
        IIcon getTileWorld_Impl(RenderBlockState renderBlockState, IIcon origIcon) {
            int face = renderBlockState.getBlockFace();
            if (face < 0) {
                face = NORTH_FACE;
            } else if (face <= TOP_FACE) {
                return null;
            }
            if (shouldConnect(renderBlockState, origIcon, face, REL_U)) {
                return icons[0];
            }
            return null;
        }

        @Override
        IIcon getTileHeld_Impl(RenderBlockState renderBlockState, IIcon origIcon) {
            return null;
        }
    }

    static class Random1 extends TileOverride {

        private final int symmetry;
        private final boolean linked;
        private final WeightedIndex chooser;

        Random1(PropertiesFile properties, TileLoader tileLoader) {
            super(properties, tileLoader);

            String sym = properties.getString("symmetry", "none");
            if (sym.equals("all")) {
                symmetry = 6;
            } else if (sym.equals("opposite")) {
                symmetry = 2;
            } else {
                symmetry = 1;
            }

            linked = properties.getBoolean("linked", false);

            chooser = WeightedIndex.create(getNumberOfTiles(), properties.getString("weights", ""));
            if (chooser == null) {
                properties.error("invalid weights");
            }
        }

        @Override
        String getMethod() {
            return "random";
        }

        @Override
        IIcon getTileWorld_Impl(RenderBlockState renderBlockState, IIcon origIcon) {
            int face = renderBlockState.getBlockFace();
            if (face < 0) {
                face = 0;
            }
            int i = renderBlockState.getI();
            int j = renderBlockState.getJ();
            int k = renderBlockState.getK();
            if (linked && renderBlockState.setCoordOffsetsForRenderType()) {
                i += renderBlockState.getDI();
                j += renderBlockState.getDJ();
                k += renderBlockState.getDK();
            }
            long hash = WeightedIndex.hash128To64(i, j, k, face / symmetry);
            int index = chooser.choose(hash);
            return icons[index];
        }

        @Override
        IIcon getTileHeld_Impl(RenderBlockState renderBlockState, IIcon origIcon) {
            return icons[0];
        }
    }

    static class Repeat extends TileOverride {

        private final int width;
        private final int height;
        private final int symmetry;

        Repeat(PropertiesFile properties, TileLoader tileLoader) {
            super(properties, tileLoader);
            width = properties.getInt("width", 0);
            height = properties.getInt("height", 0);
            if (width <= 0 || height <= 0) {
                properties.error("invalid width and height (%dx%d)", width, height);
            }

            String sym = properties.getString("symmetry", "none");
            if (sym.equals("opposite")) {
                symmetry = ~1;
            } else {
                symmetry = -1;
            }
        }

        @Override
        String getMethod() {
            return "repeat";
        }

        @Override
        String checkTileMap() {
            if (getNumberOfTiles() == width * height) {
                return null;
            } else {
                return String.format("requires exactly %dx%d tiles", width, height);
            }
        }

        @Override
        IIcon getTileWorld_Impl(RenderBlockState renderBlockState, IIcon origIcon) {
            int face = renderBlockState.getBlockFace();
            if (face < 0) {
                face = 0;
            }
            face &= symmetry;
            int i = renderBlockState.getI();
            int j = renderBlockState.getJ();
            int k = renderBlockState.getK();
            int[] xOffset = renderBlockState.getOffset(face, REL_R);
            int[] yOffset = renderBlockState.getOffset(face, REL_D);
            int x = i * xOffset[0] + j * xOffset[1] + k * xOffset[2];
            int y = i * yOffset[0] + j * yOffset[1] + k * yOffset[2];
            if (face == NORTH_FACE || face == EAST_FACE) {
                x--;
            }
            x %= width;
            if (x < 0) {
                x += width;
            }
            y %= height;
            if (y < 0) {
                y += height;
            }
            return icons[width * y + x];
        }

        @Override
        IIcon getTileHeld_Impl(RenderBlockState renderBlockState, IIcon origIcon) {
            return icons[0];
        }
    }

    static class Fixed extends TileOverride {

        Fixed(PropertiesFile properties, TileLoader tileLoader) {
            super(properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "fixed";
        }

        @Override
        String checkTileMap() {
            if (getNumberOfTiles() == 1) {
                return null;
            } else {
                return "requires exactly 1 tile";
            }
        }

        @Override
        IIcon getTileWorld_Impl(RenderBlockState renderBlockState, IIcon origIcon) {
            return icons[0];
        }

        @Override
        IIcon getTileHeld_Impl(RenderBlockState renderBlockState, IIcon origIcon) {
            return icons[0];
        }
    }

    /**
     * Block transition overlays (method=overlay).
     * Draws border textures from a 17-tile template on the faces of matched blocks
     * where they touch "connecting" neighbor blocks (connectBlocks/connectTiles),
     * e.g. grass edges creeping onto adjacent dirt.
     * OptiFine renders up to four independent border quads per face; this icon-swap
     * port instead enumerates every neighbor flag combination at load time, bakes
     * each distinct tile stack into a single composite tile (reusing the generated
     * tile pipeline introduced for ctm_compact), and looks the result up per face
     * at render time. Rendering goes through the Better Glass OVERLAY pass.
     * 方块间过渡叠加（method=overlay）。
     * 在匹配方块紧邻“连接目标”方块（connectBlocks/connectTiles）的面上，
     * 用 17 瓦片模板绘制边框纹理，例如草皮边缘蔓延到相邻泥土上。
     * OptiFine 单面最多叠加四张独立边框 quad；本图标替换架构的移植改为
     * 加载期枚举全部邻居标志组合，把每种不同的瓦片叠加序列烘焙成单张
     * 合成贴图（复用 ctm_compact 引入的生成贴图管线），渲染时按面查表。
     * 渲染经由 Better Glass 的 OVERLAY 通道。
     */
    final static class Overlay extends TileOverride {

        // 17-tile template semantics (indices into the source tile list).
        // Neighbor flag layout mirrors the base CTM relative directions:
        // edges = {left, right, down, up}, corners = {down-right, down-left, up-right, up-left},
        // matching the order OptiFine uses in getConnectedTextureOverlay.
        // 17 瓦片模板语义（索引指向源贴图列表）。
        // 邻居标志布局与基础 CTM 的相对方向一致：
        // 边 = {左, 右, 下, 上}，角 = {右下, 左下, 右上, 左上}，
        // 与 OptiFine getConnectedTextureOverlay 的遍历顺序相同。
        private static final int NUM_OVERLAY_TILES = 17;

        private static final int EDGE_LEFT = 0;
        private static final int EDGE_RIGHT = 1;
        private static final int EDGE_DOWN = 2;
        private static final int EDGE_UP = 3;
        private static final int CORNER_DR = 0;
        private static final int CORNER_DL = 1;
        private static final int CORNER_UR = 2;
        private static final int CORNER_UL = 3;

        // Relative directions for the four edge and four corner neighbors, indexed as above
        // 四个边邻居与四个角邻居的相对方向，按上述索引排列
        private static final int[] EDGE_DIRECTIONS = new int[] { REL_L, REL_R, REL_D, REL_U };
        private static final int[] CORNER_DIRECTIONS = new int[] { REL_DR, REL_DL, REL_UR, REL_UL };

        private final PropertiesFile properties;
        private final TileLoader tileLoader;
        private final int sourceTileCount;
        // Neighbor matching criteria for the overlay source (the block whose texture creeps over)
        // 叠加源（纹理向外蔓延的那种方块）的邻居匹配条件
        private final List<BlockStateMatcher> connectBlocks;
        private final Set<String> connectTiles;

        // flagsToIcon[edgeBits | cornerBits << 4 | matchBits << 8] = icon index into icons[], or -1 for none
        // flagsToIcon[边标志 | 角标志 << 4 | 同类标志 << 8] = icons[] 的索引，-1 表示无叠加
        private final int[] flagsToIcon = new int[4096];

        Overlay(PropertiesFile properties, TileLoader tileLoader) {
            super(properties, tileLoader);
            this.properties = properties;
            this.tileLoader = tileLoader;
            sourceTileCount = getNumberOfTiles();
            connectBlocks = getBlockList(properties.getString("connectBlocks", ""), "");
            connectTiles = getTileList("connectTiles");
            if (sourceTileCount < NUM_OVERLAY_TILES) {
                properties.error("overlay requires at least 17 tiles");
            } else if (properties.valid()) {
                bakeOverlayTiles();
            }
        }

        @Override
        String getMethod() {
            return "overlay";
        }

        @Override
        String checkTileMap() {
            if (sourceTileCount >= NUM_OVERLAY_TILES) {
                return null;
            } else {
                return "requires at least 17 tiles";
            }
        }

        @Override
        boolean requiresFace() {
            return true;
        }

        /**
         * Enumerate all 4096 neighbor flag combinations, compute the tile stack OptiFine
         * would draw for each, and bake every distinct non-empty stack into one composite
         * tile. Single-tile stacks reuse the source tile directly (keeping animations).
         * 枚举全部 4096 种邻居标志组合，计算 OptiFine 在每种组合下会绘制的瓦片
         * 叠加序列，并把每个不同的非空序列烘焙成一张合成贴图。
         * 单瓦片序列直接复用源贴图（保留动画支持）。
         */
        private void bakeOverlayTiles() {
            List<ResourceLocation> tileNames = getTileNames();
            List<ResourceLocation> sources = new ArrayList<>(tileNames);
            ResourceLocation propertiesResource = properties.getResource();
            String basePath = propertiesResource.getResourcePath()
                .replaceFirst("\\.properties$", "");
            // tile stack key (e.g. "3,16") -> index into the rewritten tile list
            // 瓦片叠加序列键（如 "3,16"）-> 重写后贴图列表的索引
            Map<String, Integer> stackToIndex = new HashMap<>();
            List<ResourceLocation> baked = new ArrayList<>();
            Arrays.fill(flagsToIcon, -1);
            for (int flags = 0; flags < 4096; flags++) {
                List<Integer> stack = computeTileStack(flags & 0xf, (flags >> 4) & 0xf, (flags >> 8) & 0xf);
                // Drop tiles explicitly skipped via <skip> in the tiles list
                // 丢弃 tiles 列表中通过 <skip> 显式跳过的瓦片
                stack.removeIf(tile -> sources.get(tile) == null);
                if (stack.isEmpty()) {
                    continue;
                }
                StringBuilder key = new StringBuilder();
                for (int tile : stack) {
                    if (key.length() > 0) {
                        key.append(',');
                    }
                    key.append(tile);
                }
                Integer index = stackToIndex.get(key.toString());
                if (index == null) {
                    ResourceLocation name;
                    if (stack.size() == 1) {
                        name = sources.get(stack.get(0));
                    } else {
                        name = new ResourceLocation(
                            propertiesResource.getResourceDomain(),
                            basePath + "_overlay_" + key.toString()
                                .replace(',', '_') + ".png");
                        BufferedImage image = composeStack(sources, stack);
                        if (image == null || !tileLoader.preloadGeneratedTile(name, image)) {
                            properties.warning("could not bake overlay tile %s", name);
                            name = sources.get(stack.get(0));
                        }
                    }
                    index = baked.size();
                    baked.add(name);
                    stackToIndex.put(key.toString(), index);
                }
                flagsToIcon[flags] = index;
            }
            tileNames.clear();
            tileNames.addAll(baked);
        }

        /**
         * Port of OptiFine's getConnectedTextureOverlay tile selection: given the edge,
         * corner and same-block-matching neighbor flags of one face, return the template
         * tile indices to stack, in drawing order.
         * 移植 OptiFine getConnectedTextureOverlay 的选瓦逻辑：给定单面的边、角、
         * 同类匹配三组邻居标志，返回按绘制顺序叠加的模板瓦片索引。
         */
        private static List<Integer> computeTileStack(int edgeBits, int cornerBits, int matchBits) {
            List<Integer> stack = new ArrayList<>();
            boolean l = (edgeBits & (1 << EDGE_LEFT)) != 0;
            boolean r = (edgeBits & (1 << EDGE_RIGHT)) != 0;
            boolean d = (edgeBits & (1 << EDGE_DOWN)) != 0;
            boolean u = (edgeBits & (1 << EDGE_UP)) != 0;
            boolean dr = (cornerBits & (1 << CORNER_DR)) != 0;
            boolean dl = (cornerBits & (1 << CORNER_DL)) != 0;
            boolean ur = (cornerBits & (1 << CORNER_UR)) != 0;
            boolean ul = (cornerBits & (1 << CORNER_UL)) != 0;
            boolean ml = (matchBits & (1 << EDGE_LEFT)) != 0;
            boolean mr = (matchBits & (1 << EDGE_RIGHT)) != 0;
            boolean md = (matchBits & (1 << EDGE_DOWN)) != 0;
            boolean mu = (matchBits & (1 << EDGE_UP)) != 0;
            if (l && r && d && u) {
                stack.add(8);
            } else if (l && r && d) {
                stack.add(5);
            } else if (l && d && u) {
                stack.add(6);
            } else if (r && d && u) {
                stack.add(12);
            } else if (l && r && u) {
                stack.add(13);
            } else if (r && d) {
                stack.add(3);
                if (ul) {
                    stack.add(16);
                }
            } else if (l && d) {
                stack.add(4);
                if (ur) {
                    stack.add(14);
                }
            } else if (r && u) {
                stack.add(10);
                if (dl) {
                    stack.add(2);
                }
            } else if (l && u) {
                stack.add(11);
                if (dr) {
                    stack.add(0);
                }
            } else {
                if (l) {
                    stack.add(9);
                }
                if (r) {
                    stack.add(7);
                }
                if (d) {
                    stack.add(1);
                }
                if (u) {
                    stack.add(15);
                }
                if (dr && (mr || md) && !r && !d) {
                    stack.add(0);
                }
                if (dl && (ml || md) && !l && !d) {
                    stack.add(2);
                }
                if (ur && (mr || mu) && !r && !u) {
                    stack.add(14);
                }
                if (ul && (ml || mu) && !l && !u) {
                    stack.add(16);
                }
            }
            return stack;
        }

        /**
         * Alpha-composite the given template tiles (first animation frame each) in order.
         * 按顺序将给定模板瓦片（各取动画首帧）以 alpha 混合叠加合成。
         */
        private BufferedImage composeStack(List<ResourceLocation> sources, List<Integer> stack) {
            int size = 0;
            for (int tile : stack) {
                BufferedImage source = tileLoader.getTileImage(sources.get(tile));
                if (source == null) {
                    return null;
                }
                size = Math.max(size, Math.min(source.getWidth(), source.getHeight()));
            }
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            for (int tile : stack) {
                BufferedImage source = tileLoader.getTileImage(sources.get(tile));
                int frame = Math.min(source.getWidth(), source.getHeight());
                graphics.drawImage(source, 0, 0, size, size, 0, 0, frame, frame, null);
            }
            graphics.dispose();
            return image;
        }

        /**
         * Port of OptiFine's isNeighbourOverlay: true if the neighbor is a full-cube
         * block, passes the connectBlocks/connectTiles filters, its face is exposed (not
         * covered by an opaque block or snow layer on top faces), and it does NOT connect
         * to this block (the overlay only creeps onto different blocks; the connection
         * check honors the connect= property via the base shouldConnect logic).
         * 移植 OptiFine 的 isNeighbourOverlay：邻居需为完整立方体方块、通过
         * connectBlocks/connectTiles 过滤、对应面未被遮挡（顶面还需无雪层覆盖），
         * 且不能与本方块相连（叠加只蔓延到不同的方块上；连接判定复用基类
         * shouldConnect 逻辑，从而尊重 connect= 属性）。
         */
        private boolean isNeighbourOverlay(RenderBlockState renderBlockState, IIcon origIcon, int relativeDirection) {
            IBlockAccess blockAccess = renderBlockState.getBlockAccess();
            int[] offset = renderBlockState.getOffset(renderBlockState.getBlockFace(), relativeDirection);
            int i = renderBlockState.getI() + offset[0];
            int j = renderBlockState.getJ() + offset[1];
            int k = renderBlockState.getK() + offset[2];
            Block neighbor = BlockAPI.getBlockAt(blockAccess, i, j, k);
            if (neighbor == null || !neighbor.renderAsNormalBlock()) {
                return false;
            }
            if (!connectBlocks.isEmpty() && !matchesAny(connectBlocks, blockAccess, i, j, k)) {
                return false;
            }
            if (!connectTiles.isEmpty() && !matchesConnectTile(renderBlockState, neighbor, i, j, k)) {
                return false;
            }
            if (isFaceCovered(renderBlockState, blockAccess, i, j, k)) {
                return false;
            }
            // The overlay never creeps onto blocks it would connect to
            // 叠加不会蔓延到与本方块相连的方块上
            return !shouldConnect(renderBlockState, origIcon, relativeDirection);
        }

        /**
         * Port of OptiFine's isNeighbourMatching: true if the neighbor matches this
         * override's own matchBlocks/matchTiles criteria and its face is exposed.
         * 移植 OptiFine 的 isNeighbourMatching：邻居命中本规则自身的
         * matchBlocks/matchTiles 匹配条件且对应面未被遮挡时为真。
         */
        private boolean isNeighbourMatchingExposed(RenderBlockState renderBlockState, IIcon origIcon,
            int relativeDirection) {
            IBlockAccess blockAccess = renderBlockState.getBlockAccess();
            int[] offset = renderBlockState.getOffset(renderBlockState.getBlockFace(), relativeDirection);
            int i = renderBlockState.getI() + offset[0];
            int j = renderBlockState.getJ() + offset[1];
            int k = renderBlockState.getK() + offset[2];
            if (BlockAPI.getBlockAt(blockAccess, i, j, k) == null) {
                return false;
            }
            return isNeighbourMatching(renderBlockState, origIcon, i, j, k)
                && !isFaceCovered(renderBlockState, blockAccess, i, j, k);
        }

        private boolean isNeighbourMatching(RenderBlockState renderBlockState, IIcon origIcon, int i, int j, int k) {
            IBlockAccess blockAccess = renderBlockState.getBlockAccess();
            List<BlockStateMatcher> matchBlocks = getMatchingBlocks();
            if (!matchBlocks.isEmpty()) {
                return matchesAny(matchBlocks, blockAccess, i, j, k);
            }
            // Tile-based match: neighbor uses the same texture on this face
            // 基于贴图匹配：邻居在同一面上使用相同的纹理
            Block neighbor = BlockAPI.getBlockAt(blockAccess, i, j, k);
            return neighbor != null && renderBlockState.shouldConnectByTile(neighbor, origIcon, i, j, k);
        }

        private boolean matchesConnectTile(RenderBlockState renderBlockState, Block neighbor, int i, int j, int k) {
            IIcon neighborIcon = BlockAPI.getBlockIcon(
                neighbor,
                renderBlockState.getBlockAccess(),
                i,
                j,
                k,
                renderBlockState.getTextureFaceOrig());
            return neighborIcon != null && connectTiles.contains(neighborIcon.getIconName());
        }

        private static boolean matchesAny(List<BlockStateMatcher> matchers, IBlockAccess blockAccess, int i, int j,
            int k) {
            for (BlockStateMatcher matcher : matchers) {
                if (matcher != null && matcher.match(blockAccess, i, j, k)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * True if the neighbor's face on the current side is hidden: covered by an opaque
         * block, or (top faces only) by a snow layer, mirroring OptiFine's checks.
         * 当邻居在当前面上被遮挡时为真：被不透明方块覆盖，或（仅顶面）被雪层
         * 覆盖，与 OptiFine 的判定一致。
         */
        private static boolean isFaceCovered(RenderBlockState renderBlockState, IBlockAccess blockAccess, int i, int j,
            int k) {
            int face = renderBlockState.getBlockFace();
            int[] normal = NORMALS[face];
            Block cover = BlockAPI.getBlockAt(blockAccess, i + normal[0], j + normal[1], k + normal[2]);
            if (cover == null) {
                return false;
            }
            if (cover.isOpaqueCube()) {
                return true;
            }
            return face == TOP_FACE && cover == Blocks.snow_layer;
        }

        @Override
        IIcon getTileWorld_Impl(RenderBlockState renderBlockState, IIcon origIcon) {
            int edgeBits = 0;
            int cornerBits = 0;
            int matchBits = 0;
            for (int bit = 0; bit < 4; bit++) {
                if (isNeighbourOverlay(renderBlockState, origIcon, EDGE_DIRECTIONS[bit])) {
                    edgeBits |= (1 << bit);
                }
            }
            // Corner and same-block flags only influence tiles when not fully surrounded
            // 角标志与同类标志仅在非全包围时才影响选瓦，顺带省去无效查询
            if (edgeBits != 0xf) {
                for (int bit = 0; bit < 4; bit++) {
                    if (isNeighbourOverlay(renderBlockState, origIcon, CORNER_DIRECTIONS[bit])) {
                        cornerBits |= (1 << bit);
                    }
                }
                if (cornerBits != 0) {
                    for (int bit = 0; bit < 4; bit++) {
                        if (isNeighbourMatchingExposed(renderBlockState, origIcon, EDGE_DIRECTIONS[bit])) {
                            matchBits |= (1 << bit);
                        }
                    }
                }
            }
            int index = flagsToIcon[edgeBits | (cornerBits << 4) | (matchBits << 8)];
            return index < 0 ? null : icons[index];
        }

        @Override
        IIcon getTileHeld_Impl(RenderBlockState renderBlockState, IIcon origIcon) {
            // Transition overlays depend on world neighbors; no held/inventory variant
            // 过渡叠加依赖世界邻居，手持/物品栏形态不叠加
            return null;
        }
    }

    // ------------------------------------------------------------------------------------
    // Overlay method variants. These reuse the tile-selection logic of their base methods
    // but render through the Better Glass OVERLAY render pass (polygon offset + blending),
    // drawing the selected tile on top of the base block texture instead of replacing it.
    // The render pass is forced to "overlay" in TileOverride.create() unless the
    // properties file specifies one explicitly.
    // ------------------------------------------------------------------------------------
    // overlay 系列方法变体。它们复用各自基础方法的选图逻辑，但通过 Better Glass 的
    // OVERLAY 渲染通道（多边形偏移 + 混合）渲染，将选中的贴图叠加在方块基础纹理
    // 之上而非替换它。除非 properties 文件显式指定，否则 TileOverride.create() 会
    // 强制将渲染通道设为 "overlay"。
    // ------------------------------------------------------------------------------------

    final static class OverlayCTM extends CTM {

        OverlayCTM(PropertiesFile properties, TileLoader tileLoader) {
            super(properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "overlay_ctm";
        }
    }

    final static class OverlayRandom extends Random1 {

        OverlayRandom(PropertiesFile properties, TileLoader tileLoader) {
            super(properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "overlay_random";
        }
    }

    final static class OverlayRepeat extends Repeat {

        OverlayRepeat(PropertiesFile properties, TileLoader tileLoader) {
            super(properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "overlay_repeat";
        }
    }

    final static class OverlayFixed extends Fixed {

        OverlayFixed(PropertiesFile properties, TileLoader tileLoader) {
            super(properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "overlay_fixed";
        }
    }

    final static class OverlayHorizontal extends Horizontal {

        OverlayHorizontal(PropertiesFile properties, TileLoader tileLoader) {
            super(properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "overlay_horizontal";
        }
    }

    final static class OverlayVertical extends Vertical {

        OverlayVertical(PropertiesFile properties, TileLoader tileLoader) {
            super(properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "overlay_vertical";
        }
    }

    final static class OverlayHorizontalVertical extends HorizontalVertical {

        OverlayHorizontalVertical(PropertiesFile properties, TileLoader tileLoader) {
            super(properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "overlay_horizontal+vertical";
        }
    }

    final static class OverlayVerticalHorizontal extends VerticalHorizontal {

        OverlayVerticalHorizontal(PropertiesFile properties, TileLoader tileLoader) {
            super(properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "overlay_vertical+horizontal";
        }
    }
}

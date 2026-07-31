package com.prupe.mcpatcher.mal.block;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import decok.dfcdvadstf.optifuture.config.MCPatcherForgeConfig;

// Shared by both CTM and Custom Colors.
public class RenderBlocksUtils {

    public static final boolean enableBetterGrass = MCPatcherForgeConfig.instance().betterGrass;

    private static final Block grassBlock = BlockAPI.getFixedBlock("minecraft:grass");
    private static final Block myceliumBlock = BlockAPI.getFixedBlock("minecraft:mycelium");
    private static final Block dirtBlock = BlockAPI.getFixedBlock("minecraft:dirt");
    private static final Block snowBlock = BlockAPI.getFixedBlock("minecraft:snow_layer");
    private static final Block craftedSnowBlock = BlockAPI.getFixedBlock("minecraft:snow");

    // Podzol is dirt with metadata 2. / 灰化土是 metadata 为 2 的泥土。
    private static final int PODZOL_METADATA = 2;

    private static final int COLOR = 0;
    private static final int NONCOLOR = 1;
    private static final int COLOR_AND_NONCOLOR = 2;

    private static final int[] colorMultiplierType = new int[6];
    private static final float[][] nonAOMultipliers = new float[6][3];

    public static final float[] AO_BASE = new float[] { 0.5f, 1.0f, 0.8f, 0.8f, 0.6f, 0.6f };

    public static int layerIndex;
    public static IIcon blankIcon;

    public static void setupColorMultiplier(Block block, IBlockAccess blockAccess, int i, int j, int k,
        boolean haveOverrideTexture, float r, float g, float b) {
        if (haveOverrideTexture || !RenderPassAPI.instance.useColorMultiplierThisPass(block)) {
            colorMultiplierType[0] = COLOR;
            colorMultiplierType[2] = COLOR;
            colorMultiplierType[3] = COLOR;
            colorMultiplierType[4] = COLOR;
            colorMultiplierType[5] = COLOR;
        } else if (block == grassBlock) {
            colorMultiplierType[0] = NONCOLOR;
            // Better Grass side coloring only applies when the grass block type is enabled.
            // 仅在草方块类型启用时才应用 Better Grass 的侧面染色。
            if (enableBetterGrass && BetterGrass.isBlockTypeEnabled(BetterGrass.TYPE_GRASS)) {
                if (isSnowCovered(blockAccess, i, j, k)) {
                    colorMultiplierType[2] = NONCOLOR;
                    colorMultiplierType[3] = NONCOLOR;
                    colorMultiplierType[4] = NONCOLOR;
                    colorMultiplierType[5] = NONCOLOR;
                } else {
                    j--;
                    colorMultiplierType[2] = block == BlockAPI.getBlockAt(blockAccess, i, j, k - 1)
                        && !isSnowCovered(blockAccess, i, j, k - 1) ? COLOR : COLOR_AND_NONCOLOR;
                    colorMultiplierType[3] = block == BlockAPI.getBlockAt(blockAccess, i, j, k + 1)
                        && !isSnowCovered(blockAccess, i, j, k + 1) ? COLOR : COLOR_AND_NONCOLOR;
                    colorMultiplierType[4] = block == BlockAPI.getBlockAt(blockAccess, i - 1, j, k)
                        && !isSnowCovered(blockAccess, i - 1, j, k) ? COLOR : COLOR_AND_NONCOLOR;
                    colorMultiplierType[5] = block == BlockAPI.getBlockAt(blockAccess, i + 1, j, k)
                        && !isSnowCovered(blockAccess, i + 1, j, k) ? COLOR : COLOR_AND_NONCOLOR;
                }
            } else {
                colorMultiplierType[2] = COLOR_AND_NONCOLOR;
                colorMultiplierType[3] = COLOR_AND_NONCOLOR;
                colorMultiplierType[4] = COLOR_AND_NONCOLOR;
                colorMultiplierType[5] = COLOR_AND_NONCOLOR;
            }
        } else {
            colorMultiplierType[0] = COLOR;
            colorMultiplierType[2] = COLOR;
            colorMultiplierType[3] = COLOR;
            colorMultiplierType[4] = COLOR;
            colorMultiplierType[5] = COLOR;
        }
        if (!isAmbientOcclusionEnabled() || BlockAPI.getBlockLightValue(block) != 0) {
            setupColorMultiplier(0, r, g, b);
            setupColorMultiplier(1, r, g, b);
            setupColorMultiplier(2, r, g, b);
            setupColorMultiplier(3, r, g, b);
            setupColorMultiplier(4, r, g, b);
            setupColorMultiplier(5, r, g, b);
        }
    }

    public static void setupColorMultiplier(Block block, boolean useColor) {
        if (block == grassBlock || !useColor) {
            colorMultiplierType[0] = NONCOLOR;
            colorMultiplierType[2] = NONCOLOR;
            colorMultiplierType[3] = NONCOLOR;
            colorMultiplierType[4] = NONCOLOR;
            colorMultiplierType[5] = NONCOLOR;
        } else {
            colorMultiplierType[0] = COLOR;
            colorMultiplierType[2] = COLOR;
            colorMultiplierType[3] = COLOR;
            colorMultiplierType[4] = COLOR;
            colorMultiplierType[5] = COLOR;
        }
    }

    private static void setupColorMultiplier(int face, float r, float g, float b) {
        float[] mult = nonAOMultipliers[face];
        float ao = AO_BASE[face];
        mult[0] = ao;
        mult[1] = ao;
        mult[2] = ao;
        if (colorMultiplierType[face] != NONCOLOR) {
            mult[0] *= r;
            mult[1] *= g;
            mult[2] *= b;
        }
    }

    public static boolean useColorMultiplier(int face) {
        layerIndex = 0;
        return useColorMultiplier1(face);
    }

    private static boolean useColorMultiplier1(int face) {
        if (layerIndex == 0) {
            return colorMultiplierType[getFaceIndex(face)] == COLOR;
        } else {
            return colorMultiplierType[getFaceIndex(face)] != NONCOLOR;
        }
    }

    public static float getColorMultiplierRed(int face) {
        return nonAOMultipliers[getFaceIndex(face)][0];
    }

    public static float getColorMultiplierGreen(int face) {
        return nonAOMultipliers[getFaceIndex(face)][1];
    }

    public static float getColorMultiplierBlue(int face) {
        return nonAOMultipliers[getFaceIndex(face)][2];
    }

    private static int getFaceIndex(int face) {
        return face < 0 ? 1 : face % 6;
    }

    /**
     * Returns the Better Grass override icon for a side face, or null to keep the vanilla icon.
     * Supports grass, mycelium and podzol with the per-block, snow and multilayer settings from
     * bettergrass.properties.
     * 返回侧面的 Better Grass 覆盖图标，null 表示保留原版图标。
     * 支持草方块、菌丝和灰化土，并遵循 bettergrass.properties 中的各方块开关、积雪开关和多层设置。
     */
    public static IIcon getGrassTexture(Block block, IBlockAccess blockAccess, int i, int j, int k, int face,
        IIcon topIcon) {
        if (!enableBetterGrass || face < 2) {
            return null;
        }
        int type = getBetterGrassType(block);
        if (type < 0 || !BetterGrass.isBlockTypeEnabled(type)) {
            return null;
        }
        boolean isSnow = isSnowCovered(blockAccess, i, j, k);
        if (isSnow && !BetterGrass.isSnowEnabled(type)) {
            // Snow variant disabled: keep the vanilla snowed side texture.
            // 积雪变体被禁用：保留原版的积雪侧面贴图。
            return null;
        }
        j--;
        switch (face) {
            case 2:
                k--;
                break;

            case 3:
                k++;
                break;

            case 4:
                i--;
                break;

            case 5:
                i++;
                break;

            default:
                return null;
        }
        if (matchesBetterGrassNeighbor(block, blockAccess, i, j, k)
            && isSnow == isSnowCovered(blockAccess, i, j, k)) {
            return isSnow ? BetterGrass.getSnowIcon(BlockAPI.getBlockIcon(snowBlock, blockAccess, i, j, k, face))
                : BetterGrass.getTopIcon(type, topIcon);
        }
        // Multilayer mode: unconnected grass sides use the custom grass_side texture as layer 1;
        // the biome-colored overlay is added via getGrassSideOverlayTexture().
        // 多层模式：未连接的草侧面使用自定义 grass_side 贴图作为第一层；
        // 受群系染色的覆盖层由 getGrassSideOverlayTexture() 提供。
        if (!isSnow && type == BetterGrass.TYPE_GRASS && BetterGrass.isGrassMultilayer()) {
            return BetterGrass.getGrassSideIcon();
        }
        return null;
    }

    /**
     * Maps a block to its Better Grass type index, or -1 when the block is not handled.
     * 将方块映射到 Better Grass 类型索引，不支持的方块返回 -1。
     */
    private static int getBetterGrassType(Block block) {
        if (block == grassBlock) {
            return BetterGrass.TYPE_GRASS;
        }
        if (block == myceliumBlock) {
            return BetterGrass.TYPE_MYCELIUM;
        }
        if (block == dirtBlock) {
            return BetterGrass.TYPE_PODZOL;
        }
        return -1;
    }

    /**
     * Checks whether the neighbor block below the face continues the same surface. Podzol shares
     * its block id with plain dirt, so the neighbor must also carry the podzol metadata.
     * 检查侧面下方的相邻方块是否延续同一表面。灰化土与普通泥土共用方块 id，
     * 因此相邻方块还必须带有灰化土的 metadata。
     */
    private static boolean matchesBetterGrassNeighbor(Block block, IBlockAccess blockAccess, int i, int j, int k) {
        if (block != BlockAPI.getBlockAt(blockAccess, i, j, k)) {
            return false;
        }
        return block != dirtBlock || blockAccess.getBlockMetadata(i, j, k) == PODZOL_METADATA;
    }

    /**
     * Overlay icon for unconnected grass sides in multilayer mode; null keeps the vanilla
     * grass_side_overlay icon.
     * 多层模式下未连接草侧面的覆盖层图标；null 表示保留原版 grass_side_overlay 图标。
     */
    public static IIcon getGrassSideOverlayTexture() {
        if (enableBetterGrass && BetterGrass.isBlockTypeEnabled(BetterGrass.TYPE_GRASS)
            && BetterGrass.isGrassMultilayer()) {
            return BetterGrass.getGrassOverlayIcon();
        }
        return null;
    }

    private static boolean isSnowCovered(IBlockAccess blockAccess, int i, int j, int k) {
        Block topBlock = BlockAPI.getBlockAt(blockAccess, i, j + 1, k);
        return topBlock == snowBlock || topBlock == craftedSnowBlock;
    }

    public static boolean isAmbientOcclusionEnabled() {
        return Minecraft.isAmbientOcclusionEnabled();
    }
}

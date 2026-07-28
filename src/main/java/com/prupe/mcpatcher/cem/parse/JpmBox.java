package com.prupe.mcpatcher.cem.parse;

/**
 * One box (or sprite) of a JPM part model, see "cem_part.txt" (L51-75).
 * UV is either box format ("textureOffset") or six individual faces; the two
 * styles cannot be mixed.
 * <p>
 * JPM 部件模型中的一个盒子（或 sprite），见 "cem_part.txt"（L51-75）。
 * UV 要么是盒式展开（"textureOffset"），要么是六个面各自指定；两种方式不可混用。
 * <p>
 * Pure data holder filled by {@link JemParser}; all fields may be null when absent.
 * <p>
 * 纯数据载体，由 {@link JemParser} 填充；缺省字段为 null。
 */
public class JpmBox {

    /** Face index order used by {@link #faceUvs}. / {@link #faceUvs} 使用的面序。 */
    public static final int FACE_DOWN = 0;
    public static final int FACE_UP = 1;
    public static final int FACE_NORTH = 2;
    public static final int FACE_SOUTH = 3;
    public static final int FACE_WEST = 4;
    public static final int FACE_EAST = 5;

    /** Box format UV origin [u, v]. / 盒式 UV 起点 [u, v]。 */
    public float[] textureOffset;

    /**
     * Per-face UV [u1, v1, u2, v2] indexed by FACE_*, null when box format is used.
     * <p>
     * 按 FACE_* 索引的各面 UV [u1, v1, u2, v2]，使用盒式 UV 时为 null。
     */
    public float[][] faceUvs;

    /** Position and dimensions [x, y, z, width, height, depth]. / 位置与尺寸。 */
    public float[] coordinates;

    /** Uniform size increment. / 统一的尺寸增量。 */
    public float sizeAdd;

    /** Per-axis size increments [x, y, z], overrides sizeAdd. / 各轴尺寸增量，优先于 sizeAdd。 */
    public float[] sizesAdd;

    /**
     * True if any per-face UV was specified.
     * <p>
     * 指定了任一面 UV 时为 true。
     */
    public boolean hasFaceUvs() {
        return faceUvs != null;
    }
}

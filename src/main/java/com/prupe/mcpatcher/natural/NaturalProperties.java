package com.prupe.mcpatcher.natural;

/**
 * Rotation/flip behaviour for one texture as configured by natural.properties.
 * The value grammar is the OptiFine one: "4" = four 90 degree rotations,
 * "2" = two 180 degree rotations, "F" = horizontal flip, and the combined
 * "4F"/"2F" forms. Instances are immutable and shared by every icon that maps
 * to the same configuration line.
 * <p>
 * natural.properties 中为单个纹理配置的旋转/翻转行为。取值语法与 OptiFine
 * 一致："4" 表示四种 90 度旋转，"2" 表示两种 180 度旋转，"F" 表示水平翻转，
 * 以及组合形式 "4F"/"2F"。实例不可变，同一配置行对应的所有图标共享实例。
 */
public class NaturalProperties {

    /** Number of 90 degree rotations (1, 2 or 4). / 90 度旋转的数量（1、2 或 4）。 */
    public final int rotation;
    /** Whether the texture may be flipped horizontally. / 纹理是否允许水平翻转。 */
    public final boolean flip;

    /**
     * Parses one natural.properties value.
     * 解析 natural.properties 的一个取值。
     *
     * @param type value such as "4", "2", "F", "4F" or "2F"
     * @throws IllegalArgumentException on unknown values
     */
    public NaturalProperties(String type) {
        if ("4".equals(type)) {
            rotation = 4;
            flip = false;
        } else if ("2".equals(type)) {
            rotation = 2;
            flip = false;
        } else if ("F".equals(type)) {
            rotation = 1;
            flip = true;
        } else if ("4F".equals(type)) {
            rotation = 4;
            flip = true;
        } else if ("2F".equals(type)) {
            rotation = 2;
            flip = true;
        } else {
            throw new IllegalArgumentException("unknown natural texture type: " + type);
        }
    }

    /**
     * Whether this configuration actually changes anything.
     * 该配置是否真的会产生效果。
     */
    public boolean isValid() {
        return rotation == 2 || rotation == 4 || flip;
    }

    /**
     * Rotation (in 90 degree steps) for one face, derived from the 3 random
     * bits assigned to that face. "2" configurations only ever produce 0 or 2,
     * matching OptiFine's k / 2 * 2 quantisation.
     * 根据分配给该面的 3 个随机位计算该面的旋转量（以 90 度为步长）。
     * "2" 配置只会产生 0 或 2，与 OptiFine 的 k / 2 * 2 量化一致。
     *
     * @param randomBits random value of the face
     * @return rotation 0-3
     */
    public int getRotation(int randomBits) {
        int r = randomBits & 3;
        if (rotation == 2) {
            r &= 2;
        } else if (rotation == 1) {
            r = 0;
        }
        return r;
    }

    /**
     * Whether the face should be flipped horizontally.
     * 该面是否应水平翻转。
     *
     * @param randomBits random value of the face
     */
    public boolean getFlip(int randomBits) {
        return flip && (randomBits & 4) != 0;
    }
}

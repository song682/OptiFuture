package com.prupe.mcpatcher.cem.anim;

/**
 * The animatable variables of a model part, as defined in "cem_animation.txt".
 * <p>
 * 模型部件可被动画驱动的变量，定义见 "cem_animation.txt"。
 * <ul>
 * <li>tx, ty, tz - Translation / 平移</li>
 * <li>rx, ry, rz - Rotation / 旋转</li>
 * <li>sx, sy, sz - Scale / 缩放</li>
 * <li>visible - Show model and submodels / 显示部件与子部件</li>
 * <li>visible_boxes - Show model only / 仅显示部件本身</li>
 * </ul>
 */
public enum ModelVarType {

    TX("tx"),
    TY("ty"),
    TZ("tz"),
    RX("rx"),
    RY("ry"),
    RZ("rz"),
    SX("sx"),
    SY("sy"),
    SZ("sz"),
    VISIBLE("visible"),
    VISIBLE_BOXES("visible_boxes");

    public final String name;

    ModelVarType(String name) {
        this.name = name;
    }

    /**
     * Resolve a variable suffix (the part after the last dot) to a {@link ModelVarType}.
     * <p>
     * 将变量后缀（最后一个点之后的部分）解析为 {@link ModelVarType}。
     *
     * @param suffix variable suffix, e.g. "rx" / 变量后缀，例如 "rx"
     * @return matching type, or null if not a model variable / 匹配的类型，非模型变量时返回 null
     */
    public static ModelVarType fromSuffix(String suffix) {
        for (ModelVarType type : values()) {
            if (type.name.equals(suffix)) {
                return type;
            }
        }
        return null;
    }
}

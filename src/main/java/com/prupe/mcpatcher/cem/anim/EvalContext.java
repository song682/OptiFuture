package com.prupe.mcpatcher.cem.anim;

/**
 * Evaluation context for CEM animation expressions. Supplies all variable values
 * for one render frame of one entity, and receives assignments to model variables,
 * entity variables ("var.*"/"varb.*") and render variables ("render.*").
 * <p>
 * CEM 动画表达式的求值上下文。为单个实体的一帧渲染提供全部变量取值，
 * 并接收对模型变量、实体变量（"var.*"/"varb.*"）与渲染变量（"render.*"）的赋值。
 * <p>
 * All values are doubles; booleans are represented as 1.0 (true) / 0.0 (false).
 * <p>
 * 所有值均为 double；布尔以 1.0（真）/ 0.0（假）表示。
 */
public interface EvalContext {

    /**
     * Get the value of a named parameter (render/entity/time parameter or render variable).
     * <p>
     * 读取命名参数（渲染/实体/时间参数或渲染变量）的当前值。
     */
    double getParameter(AnimParameter parameter);

    /**
     * Assign a "render.*" variable.
     * <p>
     * 对 "render.*" 变量赋值。
     */
    void setParameter(AnimParameter parameter, double value);

    /**
     * Get a model variable, e.g. "head.rx". The model specifier follows "cem_animation.txt":
     * "this", "part", part name, custom model id or a hierarchical "a:b:c" path.
     * <p>
     * 读取模型变量，例如 "head.rx"。模型指示符遵循 "cem_animation.txt"：
     * "this"、"part"、部件名、自定义模型 id 或层级路径 "a:b:c"。
     *
     * @param model model specifier / 模型指示符
     * @param var   variable type / 变量类型
     * @return current value, or 0 if the model cannot be resolved / 当前值，模型无法解析时返回 0
     */
    double getModelVar(String model, ModelVarType var);

    /**
     * Assign a model variable.
     * <p>
     * 对模型变量赋值。
     */
    void setModelVar(String model, ModelVarType var, double value);

    /**
     * Get an entity variable ("var.<name>" / "varb.<name>"), default 0.
     * The full name including the prefix is used as key.
     * <p>
     * 读取实体变量（"var.<name>" / "varb.<name>"），默认值为 0。
     * 以含前缀的完整名称作为键。
     */
    double getEntityVar(String fullName);

    /**
     * Assign an entity variable, stored on the entity across frames.
     * <p>
     * 对实体变量赋值，随实体跨帧存储。
     */
    void setEntityVar(String fullName, double value);
}

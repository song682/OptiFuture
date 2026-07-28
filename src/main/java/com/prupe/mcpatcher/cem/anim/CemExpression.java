package com.prupe.mcpatcher.cem.anim;

/**
 * A compiled CEM animation expression (AST node). Expressions are compiled once
 * when the model is loaded and evaluated every rendered frame.
 * <p>
 * 编译后的 CEM 动画表达式（AST 节点）。表达式在模型加载时编译一次，每帧渲染求值。
 * <p>
 * Booleans are represented as 1.0 (true) / 0.0 (false); any non-zero value is truthy.
 * <p>
 * 布尔以 1.0（真）/ 0.0（假）表示；任何非零值视为真。
 */
public interface CemExpression {

    double eval(EvalContext context);

    /**
     * Interpret a numeric value as boolean.
     * <p>
     * 将数值解释为布尔值。
     */
    static boolean isTrue(double value) {
        return value != 0.0;
    }

    /**
     * Convert a boolean to its numeric representation.
     * <p>
     * 将布尔值转换为其数值表示。
     */
    static double bool(boolean value) {
        return value ? 1.0 : 0.0;
    }
}

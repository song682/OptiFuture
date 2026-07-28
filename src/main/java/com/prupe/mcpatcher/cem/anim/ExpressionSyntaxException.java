package com.prupe.mcpatcher.cem.anim;

/**
 * Thrown when a CEM animation expression cannot be parsed. The caller is expected
 * to catch this, log a warning and skip the offending animation entry.
 * <p>
 * CEM 动画表达式无法解析时抛出。调用方应捕获它、记录警告并跳过出错的动画条目。
 */
public class ExpressionSyntaxException extends IllegalArgumentException {

    public ExpressionSyntaxException(String message) {
        super(message);
    }
}

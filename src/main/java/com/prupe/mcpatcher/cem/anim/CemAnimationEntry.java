package com.prupe.mcpatcher.cem.anim;

/**
 * One compiled animation assignment, i.e. one {@code "variable": "expression"} entry
 * from the "animations" section of a JEM file. The left side may be a model variable
 * ("head.rx"), an entity variable ("var.x"/"varb.x") or a render variable
 * ("render.shadow_size"). Entries are applied in declaration order every frame.
 * <p>
 * 一条编译后的动画赋值，即 JEM 文件 "animations" 段中的一条 {@code "变量": "表达式"}。
 * 左侧可以是模型变量（"head.rx"）、实体变量（"var.x"/"varb.x"）或渲染变量
 * （"render.shadow_size"）。条目每帧按声明顺序应用。
 */
public final class CemAnimationEntry {

    /** Kind of assignment target. / 赋值目标的种类。 */
    private enum TargetKind {
        MODEL_VAR,
        ENTITY_VAR,
        RENDER_VAR
    }

    private final TargetKind kind;
    // MODEL_VAR: model specifier + variable type / 模型指示符 + 变量类型
    private final String model;
    private final ModelVarType modelVar;
    // ENTITY_VAR: full name including prefix / 含前缀的完整名称
    private final String entityVar;
    // RENDER_VAR: writable render parameter / 可写的渲染参数
    private final AnimParameter renderVar;

    private final CemExpression expression;

    private CemAnimationEntry(TargetKind kind, String model, ModelVarType modelVar, String entityVar,
        AnimParameter renderVar, CemExpression expression) {
        this.kind = kind;
        this.model = model;
        this.modelVar = modelVar;
        this.entityVar = entityVar;
        this.renderVar = renderVar;
        this.expression = expression;
    }

    /**
     * Compile a single animation entry.
     * <p>
     * 编译一条动画条目。
     *
     * @param target     left side, e.g. "this.rx" / 左侧目标，例如 "this.rx"
     * @param expression right side expression source / 右侧表达式源码
     * @throws ExpressionSyntaxException if the target or the expression is invalid /
     *                                   目标或表达式非法时抛出
     */
    public static CemAnimationEntry compile(String target, String expression) {
        CemExpression compiled = ExpressionParser.parse(expression);
        String name = target == null ? "" : target.trim();
        if (name.startsWith("var.") || name.startsWith("varb.")) {
            return new CemAnimationEntry(TargetKind.ENTITY_VAR, null, null, name, null, compiled);
        }
        AnimParameter parameter = AnimParameter.byName(name);
        if (parameter != null) {
            if (!parameter.isRenderVariable()) {
                throw new ExpressionSyntaxException("'" + name + "' is read-only and cannot be assigned");
            }
            return new CemAnimationEntry(TargetKind.RENDER_VAR, null, null, null, parameter, compiled);
        }
        int dot = name.lastIndexOf('.');
        if (dot > 0 && dot < name.length() - 1) {
            ModelVarType varType = ModelVarType.fromSuffix(name.substring(dot + 1));
            if (varType != null) {
                return new CemAnimationEntry(TargetKind.MODEL_VAR, name.substring(0, dot), varType, null, null,
                    compiled);
            }
        }
        throw new ExpressionSyntaxException("invalid animation target '" + name + "'");
    }

    /**
     * Evaluate the expression and write the result to the target.
     * <p>
     * 求值表达式并将结果写入目标。
     */
    public void apply(EvalContext context) {
        double value = expression.eval(context);
        switch (kind) {
            case MODEL_VAR:
                context.setModelVar(model, modelVar, value);
                break;
            case ENTITY_VAR:
                context.setEntityVar(entityVar, value);
                break;
            case RENDER_VAR:
                context.setParameter(renderVar, value);
                break;
        }
    }
}

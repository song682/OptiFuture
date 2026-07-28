package com.prupe.mcpatcher.cem.anim;

import java.util.ArrayList;
import java.util.List;

import com.prupe.mcpatcher.MCLogger;

/**
 * Compiler for CEM animation expressions ("cem_animation.txt"). Turns an expression
 * string into a {@link CemExpression} tree via a small tokenizer and a recursive
 * descent parser. Only the documented operators, constants, variables and functions
 * are accepted; anything else raises an {@link ExpressionSyntaxException}.
 * <p>
 * CEM 动画表达式（"cem_animation.txt"）的编译器。通过小型 tokenizer 与递归下降解析
 * 将表达式字符串编译为 {@link CemExpression} 树。仅接受文档列出的运算符、常量、变量
 * 与函数，其余一律抛出 {@link ExpressionSyntaxException}。
 * <p>
 * Operator precedence (low to high): || ; && ; comparisons ; + - ; * / % ; unary ! -
 * <p>
 * 运算符优先级（从低到高）：|| ；&& ；比较 ；+ - ；* / % ；一元 ! -
 */
public final class ExpressionParser {

    private static final MCLogger logger = MCLogger.getLogger(MCLogger.Category.CUSTOM_ENTITY_MODELS);

    /** Token kinds produced by the tokenizer. / tokenizer 产生的词法单元类别。 */
    private enum TokenType {
        NUMBER,
        IDENT,
        OPERATOR,
        LPAREN,
        RPAREN,
        COMMA,
        END
    }

    private final String source;
    private int pos;

    // Current token / 当前词法单元
    private TokenType type;
    private String text;
    private double numValue;

    private ExpressionParser(String source) {
        this.source = source;
    }

    /**
     * Compile an expression string into an evaluable tree.
     * <p>
     * 将表达式字符串编译为可求值的表达式树。
     *
     * @throws ExpressionSyntaxException on any lexical/syntax/name error / 任何词法、语法或名称错误
     */
    public static CemExpression parse(String source) {
        if (source == null || source.trim()
            .isEmpty()) {
            throw new ExpressionSyntaxException("empty expression");
        }
        ExpressionParser parser = new ExpressionParser(source);
        parser.next();
        CemExpression expression = parser.parseOr();
        parser.expect(TokenType.END);
        return expression;
    }

    // ------------------------------------------------------------------
    // Tokenizer / 词法分析
    // ------------------------------------------------------------------

    private void next() {
        while (pos < source.length() && Character.isWhitespace(source.charAt(pos))) {
            pos++;
        }
        if (pos >= source.length()) {
            type = TokenType.END;
            text = "";
            return;
        }
        char c = source.charAt(pos);
        if (Character.isDigit(c) || (c == '.' && pos + 1 < source.length() && Character.isDigit(source.charAt(pos + 1)))) {
            readNumber();
        } else if (Character.isLetter(c) || c == '_') {
            readIdentifier();
        } else {
            readOperator(c);
        }
    }

    /** Read a floating point literal. / 读取浮点数字面量。 */
    private void readNumber() {
        int start = pos;
        while (pos < source.length() && (Character.isDigit(source.charAt(pos)) || source.charAt(pos) == '.')) {
            pos++;
        }
        // Optional exponent / 可选的指数部分
        if (pos < source.length() && (source.charAt(pos) == 'e' || source.charAt(pos) == 'E')) {
            int mark = pos;
            pos++;
            if (pos < source.length() && (source.charAt(pos) == '+' || source.charAt(pos) == '-')) {
                pos++;
            }
            if (pos < source.length() && Character.isDigit(source.charAt(pos))) {
                while (pos < source.length() && Character.isDigit(source.charAt(pos))) {
                    pos++;
                }
            } else {
                pos = mark;
            }
        }
        text = source.substring(start, pos);
        try {
            numValue = Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw error("invalid number '" + text + "'");
        }
        type = TokenType.NUMBER;
    }

    /**
     * Read an identifier. Dots and colons are part of the token so that model variables
     * like "head.rx", "var.counter" or hierarchical "body:limb:tip.ry" form one unit.
     * <p>
     * 读取标识符。点与冒号属于同一词法单元，使 "head.rx"、"var.counter" 或层级形式
     * "body:limb:tip.ry" 这类模型变量成为一个整体。
     */
    private void readIdentifier() {
        int start = pos;
        while (pos < source.length()) {
            char c = source.charAt(pos);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == ':') {
                pos++;
            } else {
                break;
            }
        }
        text = source.substring(start, pos);
        type = TokenType.IDENT;
    }

    private void readOperator(char c) {
        switch (c) {
            case '(':
                type = TokenType.LPAREN;
                text = "(";
                pos++;
                break;
            case ')':
                type = TokenType.RPAREN;
                text = ")";
                pos++;
                break;
            case ',':
                type = TokenType.COMMA;
                text = ",";
                pos++;
                break;
            case '+':
            case '-':
            case '*':
            case '/':
            case '%':
                type = TokenType.OPERATOR;
                text = String.valueOf(c);
                pos++;
                break;
            case '&':
            case '|':
                if (pos + 1 < source.length() && source.charAt(pos + 1) == c) {
                    type = TokenType.OPERATOR;
                    text = String.valueOf(c) + c;
                    pos += 2;
                } else {
                    throw error("unexpected character '" + c + "'");
                }
                break;
            case '>':
            case '<':
            case '=':
            case '!':
                if (pos + 1 < source.length() && source.charAt(pos + 1) == '=') {
                    type = TokenType.OPERATOR;
                    text = c + "=";
                    pos += 2;
                } else if (c == '=') {
                    throw error("unexpected character '='");
                } else {
                    type = TokenType.OPERATOR;
                    text = String.valueOf(c);
                    pos++;
                }
                break;
            default:
                throw error("unexpected character '" + c + "'");
        }
    }

    private void expect(TokenType expected) {
        if (type != expected) {
            throw error("expected " + expected + " but found '" + text + "'");
        }
    }

    private ExpressionSyntaxException error(String message) {
        return new ExpressionSyntaxException(message + " at position " + pos + " in \"" + source + "\"");
    }

    // ------------------------------------------------------------------
    // Recursive descent parser / 递归下降解析
    // ------------------------------------------------------------------

    private boolean acceptOperator(String op) {
        if (type == TokenType.OPERATOR && text.equals(op)) {
            next();
            return true;
        }
        return false;
    }

    private CemExpression parseOr() {
        CemExpression left = parseAnd();
        while (acceptOperator("||")) {
            CemExpression l = left;
            CemExpression r = parseAnd();
            // Short-circuit evaluation / 短路求值
            left = ctx -> CemExpression.bool(CemExpression.isTrue(l.eval(ctx)) || CemExpression.isTrue(r.eval(ctx)));
        }
        return left;
    }

    private CemExpression parseAnd() {
        CemExpression left = parseComparison();
        while (acceptOperator("&&")) {
            CemExpression l = left;
            CemExpression r = parseComparison();
            left = ctx -> CemExpression.bool(CemExpression.isTrue(l.eval(ctx)) && CemExpression.isTrue(r.eval(ctx)));
        }
        return left;
    }

    private CemExpression parseComparison() {
        CemExpression left = parseAdditive();
        if (type != TokenType.OPERATOR) {
            return left;
        }
        String op = text;
        CemExpression l = left;
        switch (op) {
            case ">": {
                next();
                CemExpression r = parseAdditive();
                return ctx -> CemExpression.bool(l.eval(ctx) > r.eval(ctx));
            }
            case ">=": {
                next();
                CemExpression r = parseAdditive();
                return ctx -> CemExpression.bool(l.eval(ctx) >= r.eval(ctx));
            }
            case "<": {
                next();
                CemExpression r = parseAdditive();
                return ctx -> CemExpression.bool(l.eval(ctx) < r.eval(ctx));
            }
            case "<=": {
                next();
                CemExpression r = parseAdditive();
                return ctx -> CemExpression.bool(l.eval(ctx) <= r.eval(ctx));
            }
            case "==": {
                next();
                CemExpression r = parseAdditive();
                return ctx -> CemExpression.bool(l.eval(ctx) == r.eval(ctx));
            }
            case "!=": {
                next();
                CemExpression r = parseAdditive();
                return ctx -> CemExpression.bool(l.eval(ctx) != r.eval(ctx));
            }
            default:
                return left;
        }
    }

    private CemExpression parseAdditive() {
        CemExpression left = parseMultiplicative();
        while (true) {
            if (acceptOperator("+")) {
                CemExpression l = left;
                CemExpression r = parseMultiplicative();
                left = ctx -> l.eval(ctx) + r.eval(ctx);
            } else if (acceptOperator("-")) {
                CemExpression l = left;
                CemExpression r = parseMultiplicative();
                left = ctx -> l.eval(ctx) - r.eval(ctx);
            } else {
                return left;
            }
        }
    }

    private CemExpression parseMultiplicative() {
        CemExpression left = parseUnary();
        while (true) {
            if (acceptOperator("*")) {
                CemExpression l = left;
                CemExpression r = parseUnary();
                left = ctx -> l.eval(ctx) * r.eval(ctx);
            } else if (acceptOperator("/")) {
                CemExpression l = left;
                CemExpression r = parseUnary();
                left = ctx -> l.eval(ctx) / r.eval(ctx);
            } else if (acceptOperator("%")) {
                CemExpression l = left;
                CemExpression r = parseUnary();
                left = ctx -> l.eval(ctx) % r.eval(ctx);
            } else {
                return left;
            }
        }
    }

    private CemExpression parseUnary() {
        if (acceptOperator("-")) {
            CemExpression operand = parseUnary();
            return ctx -> -operand.eval(ctx);
        }
        if (acceptOperator("!")) {
            CemExpression operand = parseUnary();
            return ctx -> CemExpression.bool(!CemExpression.isTrue(operand.eval(ctx)));
        }
        return parsePrimary();
    }

    private CemExpression parsePrimary() {
        switch (type) {
            case NUMBER: {
                double value = numValue;
                next();
                return ctx -> value;
            }
            case LPAREN: {
                next();
                CemExpression inner = parseOr();
                expect(TokenType.RPAREN);
                next();
                return inner;
            }
            case IDENT: {
                String name = text;
                next();
                if (type == TokenType.LPAREN) {
                    return parseFunction(name);
                }
                return variable(name);
            }
            default:
                throw error("unexpected token '" + text + "'");
        }
    }

    private CemExpression parseFunction(String name) {
        // Consume '(' then arguments / 消耗 '(' 后读取参数列表
        next();
        List<CemExpression> args = new ArrayList<>();
        if (type != TokenType.RPAREN) {
            args.add(parseOr());
            while (type == TokenType.COMMA) {
                next();
                args.add(parseOr());
            }
        }
        expect(TokenType.RPAREN);
        next();
        return CemFunctions.build(name, args, this::error);
    }

    // ------------------------------------------------------------------
    // Variable resolution / 变量解析
    // ------------------------------------------------------------------

    /**
     * Resolve an identifier to a constant, entity variable, named parameter or model
     * variable, in that priority order (see "cem_animation.txt").
     * <p>
     * 按优先级把标识符解析为常量、实体变量、命名参数或模型变量（见 "cem_animation.txt"）。
     */
    private CemExpression variable(String name) {
        switch (name) {
            case "pi":
                return ctx -> Math.PI;
            case "true":
                return ctx -> 1.0;
            case "false":
                return ctx -> 0.0;
            default:
                break;
        }
        if (name.startsWith("var.") || name.startsWith("varb.")) {
            return ctx -> ctx.getEntityVar(name);
        }
        AnimParameter parameter = AnimParameter.byName(name);
        if (parameter != null) {
            return ctx -> ctx.getParameter(parameter);
        }
        int dot = name.lastIndexOf('.');
        if (dot > 0 && dot < name.length() - 1) {
            ModelVarType varType = ModelVarType.fromSuffix(name.substring(dot + 1));
            if (varType != null) {
                String model = name.substring(0, dot);
                return ctx -> ctx.getModelVar(model, varType);
            }
        }
        throw error("unknown variable '" + name + "'");
    }

    /** Logger shared with {@link CemFunctions} for print()/printb(). / 供 print()/printb() 使用的共享日志。 */
    static MCLogger logger() {
        return logger;
    }
}

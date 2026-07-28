package com.prupe.mcpatcher.cem.anim;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

/**
 * Builders for the built-in animation functions listed in "cem_animation.txt"
 * (L133-166). Each builder validates the argument count at compile time and
 * returns an evaluable {@link CemExpression} node.
 * <p>
 * "cem_animation.txt"（L133-166）所列内置动画函数的构建器。每个构建器在编译期校验
 * 参数个数，并返回可求值的 {@link CemExpression} 节点。
 */
final class CemFunctions {

    private CemFunctions() {}

    /**
     * Build the expression node for a function call.
     * <p>
     * 为一次函数调用构建表达式节点。
     *
     * @param name  function name / 函数名
     * @param args  compiled argument expressions / 已编译的参数表达式
     * @param error factory for syntax errors with source context / 携带源码上下文的语法错误工厂
     */
    static CemExpression build(String name, List<CemExpression> args,
        Function<String, ExpressionSyntaxException> error) {
        switch (name) {
            case "sin":
                return unary(name, args, error, Math::sin);
            case "cos":
                return unary(name, args, error, Math::cos);
            case "asin":
                return unary(name, args, error, Math::asin);
            case "acos":
                return unary(name, args, error, Math::acos);
            case "tan":
                return unary(name, args, error, Math::tan);
            case "atan":
                return unary(name, args, error, Math::atan);
            case "atan2":
                return binary(name, args, error, Math::atan2);
            case "torad":
                return unary(name, args, error, Math::toRadians);
            case "todeg":
                return unary(name, args, error, Math::toDegrees);
            case "min":
                return reduce(name, args, error, Math::min);
            case "max":
                return reduce(name, args, error, Math::max);
            case "clamp": {
                requireArgs(name, args, 3, error);
                CemExpression x = args.get(0);
                CemExpression lo = args.get(1);
                CemExpression hi = args.get(2);
                return ctx -> Math.max(lo.eval(ctx), Math.min(hi.eval(ctx), x.eval(ctx)));
            }
            case "abs":
                return unary(name, args, error, Math::abs);
            case "floor":
                return unary(name, args, error, Math::floor);
            case "ceil":
                return unary(name, args, error, Math::ceil);
            case "exp":
                return unary(name, args, error, Math::exp);
            case "frac":
                return unary(name, args, error, x -> x - Math.floor(x));
            case "log":
                return unary(name, args, error, Math::log);
            case "pow":
                return binary(name, args, error, Math::pow);
            case "random": {
                if (args.size() > 1) {
                    throw error.apply("random() takes 0 or 1 arguments");
                }
                if (args.isEmpty()) {
                    return ctx -> ThreadLocalRandom.current()
                        .nextDouble();
                }
                CemExpression seed = args.get(0);
                return ctx -> seededRandom(seed.eval(ctx));
            }
            case "round":
                return unary(name, args, error, x -> (double) Math.round(x));
            case "signum":
                return unary(name, args, error, Math::signum);
            case "sqrt":
                return unary(name, args, error, Math::sqrt);
            // fmod: result carries the sign of the divisor / fmod：结果符号与除数一致
            case "fmod":
                return binary(name, args, error, (x, y) -> x - Math.floor(x / y) * y);
            case "lerp": {
                requireArgs(name, args, 3, error);
                CemExpression k = args.get(0);
                CemExpression x = args.get(1);
                CemExpression y = args.get(2);
                return ctx -> {
                    double a = x.eval(ctx);
                    return a + k.eval(ctx) * (y.eval(ctx) - a);
                };
            }
            // if/ifb share evaluation logic: (cond, val)... pairs plus a final else value
            // if/ifb 求值逻辑相同：若干 (条件, 值) 对加最后一个 else 值
            case "if":
            case "ifb": {
                if (args.size() < 3 || args.size() % 2 == 0) {
                    throw error.apply(name + "() needs an odd number of arguments (at least 3)");
                }
                CemExpression[] array = args.toArray(new CemExpression[0]);
                return ctx -> {
                    int i = 0;
                    while (i < array.length - 1) {
                        if (CemExpression.isTrue(array[i].eval(ctx))) {
                            return array[i + 1].eval(ctx);
                        }
                        i += 2;
                    }
                    return array[array.length - 1].eval(ctx);
                };
            }
            case "between": {
                requireArgs(name, args, 3, error);
                CemExpression x = args.get(0);
                CemExpression lo = args.get(1);
                CemExpression hi = args.get(2);
                return ctx -> {
                    double v = x.eval(ctx);
                    return CemExpression.bool(v >= lo.eval(ctx) && v <= hi.eval(ctx));
                };
            }
            case "equals": {
                requireArgs(name, args, 3, error);
                CemExpression x = args.get(0);
                CemExpression y = args.get(1);
                CemExpression epsilon = args.get(2);
                return ctx -> CemExpression.bool(Math.abs(x.eval(ctx) - y.eval(ctx)) <= epsilon.eval(ctx));
            }
            case "in": {
                if (args.size() < 2) {
                    throw error.apply("in() needs at least 2 arguments");
                }
                CemExpression[] array = args.toArray(new CemExpression[0]);
                return ctx -> {
                    double v = array[0].eval(ctx);
                    for (int i = 1; i < array.length; i++) {
                        if (v == array[i].eval(ctx)) {
                            return 1.0;
                        }
                    }
                    return 0.0;
                };
            }
            // print/printb log every n-th frame and pass the value through
            // print/printb 每 n 帧打印一次并原样返回值
            case "print":
            case "printb": {
                requireArgs(name, args, 3, error);
                CemExpression id = args.get(0);
                CemExpression n = args.get(1);
                CemExpression x = args.get(2);
                boolean asBool = name.equals("printb");
                return ctx -> {
                    double value = x.eval(ctx);
                    long interval = (long) n.eval(ctx);
                    long frame = (long) ctx.getParameter(AnimParameter.FRAME_COUNTER);
                    if (interval > 0 && frame % interval == 0) {
                        if (asBool) {
                            ExpressionParser.logger()
                                .info("printb(%.0f): %s", id.eval(ctx), CemExpression.isTrue(value));
                        } else {
                            ExpressionParser.logger()
                                .info("print(%.0f): %f", id.eval(ctx), value);
                        }
                    }
                    return value;
                };
            }
            default:
                throw error.apply("unknown function '" + name + "'");
        }
    }

    private static void requireArgs(String name, List<CemExpression> args, int count,
        Function<String, ExpressionSyntaxException> error) {
        if (args.size() != count) {
            throw error.apply(name + "() needs " + count + " arguments, found " + args.size());
        }
    }

    private static CemExpression unary(String name, List<CemExpression> args,
        Function<String, ExpressionSyntaxException> error, DoubleUnaryOperator fn) {
        requireArgs(name, args, 1, error);
        CemExpression x = args.get(0);
        return ctx -> fn.applyAsDouble(x.eval(ctx));
    }

    private static CemExpression binary(String name, List<CemExpression> args,
        Function<String, ExpressionSyntaxException> error, DoubleBinaryOperator fn) {
        requireArgs(name, args, 2, error);
        CemExpression x = args.get(0);
        CemExpression y = args.get(1);
        return ctx -> fn.applyAsDouble(x.eval(ctx), y.eval(ctx));
    }

    /** Fold a variadic argument list (min/max). / 折叠可变参数列表（min/max）。 */
    private static CemExpression reduce(String name, List<CemExpression> args,
        Function<String, ExpressionSyntaxException> error, DoubleBinaryOperator fn) {
        if (args.isEmpty()) {
            throw error.apply(name + "() needs at least 1 argument");
        }
        CemExpression[] array = args.toArray(new CemExpression[0]);
        return ctx -> {
            double result = array[0].eval(ctx);
            for (int i = 1; i < array.length; i++) {
                result = fn.applyAsDouble(result, array[i].eval(ctx));
            }
            return result;
        };
    }

    /**
     * Deterministic pseudo-random value in [0, 1) derived from a seed, allocation-free
     * (SplitMix64-style bit mixing).
     * <p>
     * 由种子推导的 [0, 1) 区间确定性伪随机数，无内存分配（SplitMix64 风格位混合）。
     */
    private static double seededRandom(double seed) {
        long h = Double.doubleToLongBits(seed);
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return (h >>> 11) / (double) (1L << 53);
    }
}

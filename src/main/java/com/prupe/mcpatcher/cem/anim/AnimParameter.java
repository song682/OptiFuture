package com.prupe.mcpatcher.cem.anim;

import java.util.HashMap;
import java.util.Map;

/**
 * Named parameters usable in animation expressions, as listed in "cem_animation.txt":
 * global time values, render parameters, entity parameters (float and boolean) and
 * writable render variables ("render.*").
 * <p>
 * 动画表达式可引用的命名参数，清单见 "cem_animation.txt"：
 * 全局时间值、渲染参数、实体参数（浮点与布尔）以及可写的渲染变量（"render.*"）。
 * <p>
 * Boolean parameters evaluate to 1.0 (true) or 0.0 (false).
 * <p>
 * 布尔参数求值为 1.0（真）或 0.0（假）。
 */
public enum AnimParameter {

    // Global time values / 全局时间值
    TIME("time"),
    DAY_TIME("day_time"),
    DAY_COUNT("day_count"),

    // Render parameters / 渲染参数
    LIMB_SWING("limb_swing"),
    LIMB_SPEED("limb_speed"),
    AGE("age"),
    HEAD_PITCH("head_pitch"),
    HEAD_YAW("head_yaw"),
    PLAYER_POS_X("player_pos_x"),
    PLAYER_POS_Y("player_pos_y"),
    PLAYER_POS_Z("player_pos_z"),
    PLAYER_ROT_X("player_rot_x"),
    PLAYER_ROT_Y("player_rot_y"),
    FRAME_TIME("frame_time"),
    FRAME_COUNTER("frame_counter"),
    DIMENSION("dimension"),
    RULE_INDEX("rule_index"),

    // Entity parameters (float) / 实体参数（浮点）
    HEALTH("health"),
    HURT_TIME("hurt_time"),
    DEATH_TIME("death_time"),
    ANGER_TIME("anger_time"),
    ANGER_TIME_START("anger_time_start"),
    MAX_HEALTH("max_health"),
    POS_X("pos_x"),
    POS_Y("pos_y"),
    POS_Z("pos_z"),
    ROT_X("rot_x"),
    ROT_Y("rot_y"),
    SWING_PROGRESS("swing_progress"),
    ID("id"),

    // Entity parameters (boolean) / 实体参数（布尔）
    IS_AGGRESSIVE("is_aggressive"),
    IS_ALIVE("is_alive"),
    IS_BURNING("is_burning"),
    IS_CHILD("is_child"),
    IS_GLOWING("is_glowing"),
    IS_HURT("is_hurt"),
    IS_IN_HAND("is_in_hand"),
    IS_IN_ITEM_FRAME("is_in_item_frame"),
    IS_IN_GROUND("is_in_ground"),
    IS_IN_GUI("is_in_gui"),
    IS_IN_LAVA("is_in_lava"),
    IS_IN_WATER("is_in_water"),
    IS_INVISIBLE("is_invisible"),
    IS_ON_GROUND("is_on_ground"),
    IS_ON_HEAD("is_on_head"),
    IS_ON_SHOULDER("is_on_shoulder"),
    IS_RIDDEN("is_ridden"),
    IS_RIDING("is_riding"),
    IS_SITTING("is_sitting"),
    IS_SNEAKING("is_sneaking"),
    IS_SPRINTING("is_sprinting"),
    IS_TAMED("is_tamed"),
    IS_WET("is_wet"),

    // Render variables (readable and assignable) / 渲染变量（可读且可被赋值）
    SHADOW_SIZE("render.shadow_size", true),
    SHADOW_OPACITY("render.shadow_opacity", true),
    SHADOW_OFFSET_X("render.shadow_offset_x", true),
    SHADOW_OFFSET_Z("render.shadow_offset_z", true),
    LEASH_OFFSET_X("render.leash_offset_x", true),
    LEASH_OFFSET_Y("render.leash_offset_y", true),
    LEASH_OFFSET_Z("render.leash_offset_z", true);

    private static final Map<String, AnimParameter> BY_NAME = new HashMap<>();

    static {
        for (AnimParameter parameter : values()) {
            BY_NAME.put(parameter.name, parameter);
        }
    }

    public final String name;
    private final boolean renderVariable;

    AnimParameter(String name) {
        this(name, false);
    }

    AnimParameter(String name, boolean renderVariable) {
        this.name = name;
        this.renderVariable = renderVariable;
    }

    /**
     * True for "render.*" variables which may appear on the left side of an animation
     * assignment.
     * <p>
     * "render.*" 变量返回 true，它们可以出现在动画赋值的左侧。
     */
    public boolean isRenderVariable() {
        return renderVariable;
    }

    /**
     * Look up a parameter by its expression name, e.g. "limb_swing" or "render.shadow_size".
     * <p>
     * 按表达式中的名称查找参数，例如 "limb_swing" 或 "render.shadow_size"。
     *
     * @return matching parameter, or null if unknown / 匹配的参数，未知名称时返回 null
     */
    public static AnimParameter byName(String name) {
        return BY_NAME.get(name);
    }
}

package com.prupe.mcpatcher.cem;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;

import com.prupe.mcpatcher.cem.anim.AnimParameter;
import com.prupe.mcpatcher.cem.anim.EvalContext;
import com.prupe.mcpatcher.cem.anim.ModelVarType;
import com.prupe.mcpatcher.cem.model.CemModelBase;
import com.prupe.mcpatcher.cem.model.CemModelRenderer;

/**
 * Binds animation expressions to the live game state: one reusable instance is
 * refreshed per rendered entity ({@link #prepare}) and handed to the compiled
 * animation entries. Persistent "var.*"/"varb.*" values are stored per entity in a
 * weak map so they survive across frames but not entity unloading.
 * <p>
 * 将动画表达式与实时游戏状态绑定：单个可复用实例在每个被渲染实体上刷新
 * （{@link #prepare}）后交给已编译的动画条目。持久的 "var.*"/"varb.*" 值以弱引用
 * 映射按实体存储，跨帧保留但不阻止实体卸载。
 */
public class CemEvalContext implements EvalContext {

    /** Per-entity persistent expression variables. / 按实体存储的持久表达式变量。 */
    private static final Map<Entity, Map<String, Double>> ENTITY_VARS = new WeakHashMap<>();

    private EntityLivingBase entity;
    private CemModelBase model;
    private CemModelRenderer currentPart;
    private float partialTick;

    // Render parameters computed the same way vanilla passes them to the model
    // 按原版传给模型的方式计算的渲染参数
    private double limbSwing;
    private double limbSpeed;
    private double headYaw;
    private double headPitch;

    // Writable render variables, consumed by the render hook after evaluation
    // 可写渲染变量，求值后由渲染钩子消费
    public double shadowSize;
    public double shadowOpacity = 1.0;
    public double shadowOffsetX;
    public double shadowOffsetZ;
    public double leashOffsetX;
    public double leashOffsetY;
    public double leashOffsetZ;

    /**
     * Refresh the context for one entity about to be rendered.
     * <p>
     * 为即将渲染的实体刷新上下文。
     */
    public void prepare(EntityLivingBase entity, CemModelBase model, float partialTick, double shadowSize) {
        this.entity = entity;
        this.model = model;
        this.partialTick = partialTick;
        this.shadowSize = shadowSize;
        shadowOpacity = 1.0;
        shadowOffsetX = 0.0;
        shadowOffsetZ = 0.0;
        leashOffsetX = 0.0;
        leashOffsetY = 0.0;
        leashOffsetZ = 0.0;

        // Same formulas as RendererLivingEntity.doRender / 与 RendererLivingEntity.doRender 相同的公式
        limbSpeed = entity.prevLimbSwingAmount + (entity.limbSwingAmount - entity.prevLimbSwingAmount) * partialTick;
        limbSwing = entity.limbSwing - entity.limbSwingAmount * (1.0f - partialTick);
        float bodyYaw = interpolateRotation(entity.prevRenderYawOffset, entity.renderYawOffset);
        headYaw = interpolateRotation(entity.prevRotationYawHead, entity.rotationYawHead) - bodyYaw;
        headPitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTick;
    }

    /** Set before applying each animation entry, resolves "this". / 应用每条动画前设置，用于解析 "this"。 */
    public void setCurrentPart(CemModelRenderer part) {
        currentPart = part;
    }

    private float interpolateRotation(float previous, float current) {
        float delta = current - previous;
        while (delta < -180.0f) {
            delta += 360.0f;
        }
        while (delta >= 180.0f) {
            delta -= 360.0f;
        }
        return previous + partialTick * delta;
    }

    @Override
    public double getParameter(AnimParameter parameter) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.thePlayer;
        switch (parameter) {
            case TIME:
                return entity.worldObj.getTotalWorldTime() + partialTick;
            case DAY_TIME:
                return entity.worldObj.getWorldTime() % 24000L;
            case DAY_COUNT:
                return entity.worldObj.getWorldTime() / 24000L;

            case LIMB_SWING:
                return limbSwing;
            case LIMB_SPEED:
                return limbSpeed;
            case AGE:
                return entity.ticksExisted + partialTick;
            case HEAD_PITCH:
                return headPitch;
            case HEAD_YAW:
                return headYaw;
            case PLAYER_POS_X:
                return player == null ? 0.0 : player.prevPosX + (player.posX - player.prevPosX) * partialTick;
            case PLAYER_POS_Y:
                return player == null ? 0.0 : player.prevPosY + (player.posY - player.prevPosY) * partialTick;
            case PLAYER_POS_Z:
                return player == null ? 0.0 : player.prevPosZ + (player.posZ - player.prevPosZ) * partialTick;
            case PLAYER_ROT_X:
                return player == null ? 0.0
                    : player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTick;
            case PLAYER_ROT_Y:
                return player == null ? 0.0
                    : player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTick;
            case FRAME_TIME:
                return CustomEntityModels.getFrameTime();
            case FRAME_COUNTER:
                return CustomEntityModels.getFrameCounter();
            case DIMENSION:
                return entity.worldObj.provider.dimensionId;
            // Random model rules are not implemented yet / 随机模型规则尚未实现
            case RULE_INDEX:
                return 0.0;

            case HEALTH:
                return entity.getHealth();
            case HURT_TIME:
                return entity.hurtTime > 0 ? entity.hurtTime - partialTick : 0.0;
            case DEATH_TIME:
                return entity.deathTime > 0 ? entity.deathTime + partialTick : 0.0;
            // Anger timers are not exposed by 1.7.10 entities / 1.7.10 实体未公开愤怒计时
            case ANGER_TIME:
            case ANGER_TIME_START:
                return 0.0;
            case MAX_HEALTH:
                return entity.getMaxHealth();
            case POS_X:
                return entity.prevPosX + (entity.posX - entity.prevPosX) * partialTick;
            case POS_Y:
                return entity.prevPosY + (entity.posY - entity.prevPosY) * partialTick;
            case POS_Z:
                return entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTick;
            case ROT_X:
                return entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTick;
            case ROT_Y:
                return interpolateRotation(entity.prevRenderYawOffset, entity.renderYawOffset);
            case SWING_PROGRESS:
                return entity.prevSwingProgress + (entity.swingProgress - entity.prevSwingProgress) * partialTick;
            case ID:
                return entity.getEntityId();

            case IS_AGGRESSIVE:
                if (entity instanceof EntityWolf) {
                    return bool(((EntityWolf) entity).isAngry());
                }
                return bool(entity instanceof EntityLiving && ((EntityLiving) entity).getAttackTarget() != null);
            case IS_ALIVE:
                return bool(entity.isEntityAlive());
            case IS_BURNING:
                return bool(entity.isBurning());
            case IS_CHILD:
                return bool(entity.isChild());
            case IS_HURT:
                return bool(entity.hurtTime > 0);
            case IS_IN_LAVA:
                return bool(entity.handleLavaMovement());
            case IS_IN_WATER:
                return bool(entity.isInWater());
            case IS_INVISIBLE:
                return bool(entity.isInvisible());
            case IS_ON_GROUND:
                return bool(entity.onGround);
            case IS_RIDDEN:
                return bool(entity.riddenByEntity != null);
            case IS_RIDING:
                return bool(entity.isRiding());
            case IS_SITTING:
                return bool(entity instanceof EntityTameable && ((EntityTameable) entity).isSitting());
            case IS_SNEAKING:
                return bool(entity.isSneaking());
            case IS_SPRINTING:
                return bool(entity.isSprinting());
            case IS_TAMED:
                return bool(entity instanceof EntityTameable && ((EntityTameable) entity).isTamed());
            case IS_WET:
                return bool(entity.isWet());
            // Not applicable to 1.7.10 (glowing, item frames, GUIs...) / 1.7.10 不适用的参数
            case IS_GLOWING:
            case IS_IN_HAND:
            case IS_IN_ITEM_FRAME:
            case IS_IN_GROUND:
            case IS_IN_GUI:
            case IS_ON_HEAD:
            case IS_ON_SHOULDER:
                return 0.0;

            case SHADOW_SIZE:
                return shadowSize;
            case SHADOW_OPACITY:
                return shadowOpacity;
            case SHADOW_OFFSET_X:
                return shadowOffsetX;
            case SHADOW_OFFSET_Z:
                return shadowOffsetZ;
            case LEASH_OFFSET_X:
                return leashOffsetX;
            case LEASH_OFFSET_Y:
                return leashOffsetY;
            case LEASH_OFFSET_Z:
                return leashOffsetZ;
            default:
                return 0.0;
        }
    }

    @Override
    public void setParameter(AnimParameter parameter, double value) {
        switch (parameter) {
            case SHADOW_SIZE:
                shadowSize = value;
                break;
            case SHADOW_OPACITY:
                shadowOpacity = value;
                break;
            case SHADOW_OFFSET_X:
                shadowOffsetX = value;
                break;
            case SHADOW_OFFSET_Z:
                shadowOffsetZ = value;
                break;
            case LEASH_OFFSET_X:
                leashOffsetX = value;
                break;
            case LEASH_OFFSET_Y:
                leashOffsetY = value;
                break;
            case LEASH_OFFSET_Z:
                leashOffsetZ = value;
                break;
            default:
                // Read-only parameters are rejected at compile time already
                // 只读参数在编译期即被拒绝
                break;
        }
    }

    @Override
    public double getModelVar(String model, ModelVarType type) {
        return this.model.getModelVar(model, type, currentPart);
    }

    @Override
    public void setModelVar(String model, ModelVarType type, double value) {
        this.model.setModelVar(model, type, value, currentPart);
    }

    @Override
    public double getEntityVar(String fullName) {
        Map<String, Double> vars = ENTITY_VARS.get(entity);
        Double value = vars == null ? null : vars.get(fullName);
        return value == null ? 0.0 : value;
    }

    @Override
    public void setEntityVar(String fullName, double value) {
        ENTITY_VARS.computeIfAbsent(entity, e -> new HashMap<>())
            .put(fullName, value);
    }

    private static double bool(boolean value) {
        return value ? 1.0 : 0.0;
    }
}

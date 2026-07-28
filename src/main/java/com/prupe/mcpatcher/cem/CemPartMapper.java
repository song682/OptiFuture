package com.prupe.mcpatcher.cem;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelChicken;
import net.minecraft.client.model.ModelCreeper;
import net.minecraft.client.model.ModelIronGolem;
import net.minecraft.client.model.ModelQuadruped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSnowMan;
import net.minecraft.client.model.ModelSpider;
import net.minecraft.client.model.ModelVillager;
import net.minecraft.client.model.ModelWolf;

/**
 * Maps OptiFine CEM part names ("head", "leg1", "left_arm"...) to the
 * {@code ModelRenderer} fields of the vanilla model classes, see the entity table
 * in "cem_model.txt". Fields are accessed directly (not reflectively) so the
 * references are remapped correctly when the mod is reobfuscated; models whose
 * fields are not public (bat, blaze, horse, squid tail parts...) are added later
 * via access transformers.
 * <p>
 * 将 OptiFine CEM 部件名（"head"、"leg1"、"left_arm" 等）映射到原版模型类的
 * {@code ModelRenderer} 字段，实体表见 "cem_model.txt"。字段为直接访问（非反射），
 * 因此混淆重映射时引用会被正确转换；字段非 public 的模型（蝙蝠、烈焰人、马、鱿鱼的
 * 部分部件等）留待后续通过 access transformer 开放。
 */
public final class CemPartMapper {

    /**
     * Read/write access to one vanilla model part field.
     * <p>
     * 对一个原版模型部件字段的读写访问。
     */
    public interface PartSlot {

        ModelRenderer get();

        void set(ModelRenderer renderer);
    }

    private CemPartMapper() {}

    /**
     * Build the part-name map for a vanilla model instance.
     * <p>
     * 为原版模型实例构建部件名映射。
     *
     * @return map of OptiFine part name to field access, empty if the model is unsupported /
     *         OptiFine 部件名 → 字段访问的映射，不支持的模型返回空表
     */
    public static Map<String, PartSlot> map(ModelBase model) {
        Map<String, PartSlot> slots = new HashMap<>();
        if (model instanceof ModelCreeper) {
            final ModelCreeper m = (ModelCreeper) model;
            put(slots, "head", () -> m.head, r -> m.head = r);
            put(slots, "body", () -> m.body, r -> m.body = r);
            put(slots, "leg1", () -> m.leg1, r -> m.leg1 = r);
            put(slots, "leg2", () -> m.leg2, r -> m.leg2 = r);
            put(slots, "leg3", () -> m.leg3, r -> m.leg3 = r);
            put(slots, "leg4", () -> m.leg4, r -> m.leg4 = r);
        } else if (model instanceof ModelQuadruped) {
            final ModelQuadruped m = (ModelQuadruped) model;
            // pig, cow, mooshroom, sheep (body and wool) / 猪、牛、哞菇、羊（身体与羊毛）
            put(slots, "head", () -> m.head, r -> m.head = r);
            put(slots, "body", () -> m.body, r -> m.body = r);
            put(slots, "leg1", () -> m.leg1, r -> m.leg1 = r);
            put(slots, "leg2", () -> m.leg2, r -> m.leg2 = r);
            put(slots, "leg3", () -> m.leg3, r -> m.leg3 = r);
            put(slots, "leg4", () -> m.leg4, r -> m.leg4 = r);
        } else if (model instanceof ModelBiped) {
            final ModelBiped m = (ModelBiped) model;
            // zombie, skeleton, enderman, zombie pigman, giant / 僵尸、骷髅、末影人、僵尸猪人、巨人
            put(slots, "head", () -> m.bipedHead, r -> m.bipedHead = r);
            put(slots, "headwear", () -> m.bipedHeadwear, r -> m.bipedHeadwear = r);
            put(slots, "body", () -> m.bipedBody, r -> m.bipedBody = r);
            put(slots, "right_arm", () -> m.bipedRightArm, r -> m.bipedRightArm = r);
            put(slots, "left_arm", () -> m.bipedLeftArm, r -> m.bipedLeftArm = r);
            put(slots, "right_leg", () -> m.bipedRightLeg, r -> m.bipedRightLeg = r);
            put(slots, "left_leg", () -> m.bipedLeftLeg, r -> m.bipedLeftLeg = r);
        } else if (model instanceof ModelSpider) {
            final ModelSpider m = (ModelSpider) model;
            put(slots, "head", () -> m.spiderHead, r -> m.spiderHead = r);
            put(slots, "neck", () -> m.spiderNeck, r -> m.spiderNeck = r);
            put(slots, "body", () -> m.spiderBody, r -> m.spiderBody = r);
            put(slots, "leg1", () -> m.spiderLeg1, r -> m.spiderLeg1 = r);
            put(slots, "leg2", () -> m.spiderLeg2, r -> m.spiderLeg2 = r);
            put(slots, "leg3", () -> m.spiderLeg3, r -> m.spiderLeg3 = r);
            put(slots, "leg4", () -> m.spiderLeg4, r -> m.spiderLeg4 = r);
            put(slots, "leg5", () -> m.spiderLeg5, r -> m.spiderLeg5 = r);
            put(slots, "leg6", () -> m.spiderLeg6, r -> m.spiderLeg6 = r);
            put(slots, "leg7", () -> m.spiderLeg7, r -> m.spiderLeg7 = r);
            put(slots, "leg8", () -> m.spiderLeg8, r -> m.spiderLeg8 = r);
        } else if (model instanceof ModelChicken) {
            final ModelChicken m = (ModelChicken) model;
            put(slots, "head", () -> m.head, r -> m.head = r);
            put(slots, "body", () -> m.body, r -> m.body = r);
            put(slots, "right_leg", () -> m.rightLeg, r -> m.rightLeg = r);
            put(slots, "left_leg", () -> m.leftLeg, r -> m.leftLeg = r);
            put(slots, "right_wing", () -> m.rightWing, r -> m.rightWing = r);
            put(slots, "left_wing", () -> m.leftWing, r -> m.leftWing = r);
            put(slots, "bill", () -> m.bill, r -> m.bill = r);
            put(slots, "chin", () -> m.chin, r -> m.chin = r);
        } else if (model instanceof ModelWolf) {
            final ModelWolf m = (ModelWolf) model;
            // tail and mane are package-private, added later via AT / tail 与 mane 为包私有，后续经 AT 开放
            put(slots, "head", () -> m.wolfHeadMain, r -> m.wolfHeadMain = r);
            put(slots, "body", () -> m.wolfBody, r -> m.wolfBody = r);
            put(slots, "leg1", () -> m.wolfLeg1, r -> m.wolfLeg1 = r);
            put(slots, "leg2", () -> m.wolfLeg2, r -> m.wolfLeg2 = r);
            put(slots, "leg3", () -> m.wolfLeg3, r -> m.wolfLeg3 = r);
            put(slots, "leg4", () -> m.wolfLeg4, r -> m.wolfLeg4 = r);
        } else if (model instanceof ModelVillager) {
            final ModelVillager m = (ModelVillager) model;
            // villager and witch (witch hat is private) / 村民与女巫（女巫帽子为私有）
            put(slots, "head", () -> m.villagerHead, r -> m.villagerHead = r);
            put(slots, "body", () -> m.villagerBody, r -> m.villagerBody = r);
            put(slots, "arms", () -> m.villagerArms, r -> m.villagerArms = r);
            put(slots, "right_leg", () -> m.rightVillagerLeg, r -> m.rightVillagerLeg = r);
            put(slots, "left_leg", () -> m.leftVillagerLeg, r -> m.leftVillagerLeg = r);
            put(slots, "nose", () -> m.villagerNose, r -> m.villagerNose = r);
        } else if (model instanceof ModelIronGolem) {
            final ModelIronGolem m = (ModelIronGolem) model;
            put(slots, "head", () -> m.ironGolemHead, r -> m.ironGolemHead = r);
            put(slots, "body", () -> m.ironGolemBody, r -> m.ironGolemBody = r);
            put(slots, "right_arm", () -> m.ironGolemRightArm, r -> m.ironGolemRightArm = r);
            put(slots, "left_arm", () -> m.ironGolemLeftArm, r -> m.ironGolemLeftArm = r);
            put(slots, "right_leg", () -> m.ironGolemRightLeg, r -> m.ironGolemRightLeg = r);
            put(slots, "left_leg", () -> m.ironGolemLeftLeg, r -> m.ironGolemLeftLeg = r);
        } else if (model instanceof ModelSnowMan) {
            final ModelSnowMan m = (ModelSnowMan) model;
            put(slots, "head", () -> m.head, r -> m.head = r);
            put(slots, "body", () -> m.body, r -> m.body = r);
            put(slots, "body_bottom", () -> m.bottomBody, r -> m.bottomBody = r);
            put(slots, "right_hand", () -> m.rightHand, r -> m.rightHand = r);
            put(slots, "left_hand", () -> m.leftHand, r -> m.leftHand = r);
        }
        return slots;
    }

    private static void put(Map<String, PartSlot> slots, String name, java.util.function.Supplier<ModelRenderer> get,
        java.util.function.Consumer<ModelRenderer> set) {
        slots.put(name, new PartSlot() {

            @Override
            public ModelRenderer get() {
                return get.get();
            }

            @Override
            public void set(ModelRenderer renderer) {
                set.accept(renderer);
            }
        });
    }
}

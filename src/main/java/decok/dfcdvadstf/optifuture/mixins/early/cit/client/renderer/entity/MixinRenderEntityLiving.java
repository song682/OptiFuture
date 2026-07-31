package decok.dfcdvadstf.optifuture.mixins.early.cit.client.renderer.entity;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.prupe.mcpatcher.cit.CITUtils;

/**
 * Rewrites the living-entity render loop so CIT armor enchantment overlays can
 * replace the vanilla armor glint. When a CIT enchantment applies, it is drawn
 * for the pass; otherwise the code falls back to the original vanilla glint,
 * which is why the vanilla {@code if} becomes an {@code else if} here.
 * <p>
 * 重写生物实体的渲染循环，使 CIT 护甲附魔覆盖层能够替代原版护甲光效。
 * 当存在适用的 CIT 附魔时，为该渲染阶段绘制该附魔；否则回退到原版光效，
 * 这也是此处原版 {@code if} 变为 {@code else if} 的原因。
 */
@Mixin(RendererLivingEntity.class)
public abstract class MixinRenderEntityLiving extends Render {

    @Final
    @Shadow
    private static Logger logger;
    @Final
    @Shadow
    private static ResourceLocation RES_ITEM_GLINT;
    @Shadow
    protected ModelBase mainModel;
    @Shadow
    protected ModelBase renderPassModel;

    @Shadow
    protected abstract float interpolateRotation(float angle1, float angle2, float p_77034_3_);

    @Shadow
    protected abstract void renderModel(EntityLivingBase entityLivingBase, float p_77036_2_, float p_77036_3_,
        float p_77036_4_, float p_77036_5_, float p_77036_6_, float p_77036_7_);

    @Shadow
    protected abstract void renderLivingAt(EntityLivingBase entityLivingBase, double p_77039_2_, double p_77039_4_,
        double p_77039_6_);

    @Shadow
    protected abstract void rotateCorpse(EntityLivingBase entityLivingBase, float p_77043_2_, float p_77043_3_,
        float p_77043_4_);

    @Shadow
    protected abstract float renderSwingProgress(EntityLivingBase entityLivingBase, float p_77040_2_);

    @Shadow
    protected abstract float handleRotationFloat(EntityLivingBase entityLivingBase, float p_77044_2_);

    @Shadow
    protected abstract void renderEquippedItems(EntityLivingBase entityLivingBase, float p_77029_2_);

    @Shadow
    protected abstract int inheritRenderPass(EntityLivingBase entityLivingBase, int p_77035_2_, float p_77035_3_);

    @Shadow
    protected abstract int shouldRenderPass(EntityLivingBase entityLivingBase, int p_77032_2_, float p_77032_3_);

    @Shadow
    protected abstract void func_82408_c(EntityLivingBase entityLivingBase, int p_82408_2_, float p_82408_3_);

    @Shadow
    protected abstract int getColorMultiplier(EntityLivingBase entityLivingBase, float p_77030_2_, float p_77030_3_);

    @Shadow
    protected abstract void preRenderCallback(EntityLivingBase entityLivingBase, float p_77041_2_);

    @Shadow
    protected abstract void passSpecialRender(EntityLivingBase entityLivingBase, double p_77033_2_, double p_77033_4_,
        double p_77033_6_);

    /**
     * @author OptiFutureOptimized
     * @reason Insert CIT armor enchantment overlays into the render-pass loop;
     *         the vanilla glint block becomes an else-if fallback, which cannot
     *         be expressed with a simple injector.
     */
    @SuppressWarnings("DuplicatedCode")
    @Overwrite
    public void doRender(EntityLivingBase entity, double x, double y, double z, float yaw, float partialTicks) {
        if (MinecraftForge.EVENT_BUS
            .post(new RenderLivingEvent.Pre(entity, (RendererLivingEntity) (Object) this, x, y, z))) return;
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_CULL_FACE);
        this.mainModel.onGround = this.renderSwingProgress(entity, partialTicks);

        if (this.renderPassModel != null) {
            this.renderPassModel.onGround = this.mainModel.onGround;
        }

        this.mainModel.isRiding = entity.isRiding();

        if (this.renderPassModel != null) {
            this.renderPassModel.isRiding = this.mainModel.isRiding;
        }

        this.mainModel.isChild = entity.isChild();

        if (this.renderPassModel != null) {
            this.renderPassModel.isChild = this.mainModel.isChild;
        }

        try {
            float bodyYaw = this.interpolateRotation(entity.prevRenderYawOffset, entity.renderYawOffset, partialTicks);
            float headYaw = this.interpolateRotation(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks);

            if (entity.isRiding() && entity.ridingEntity instanceof EntityLivingBase) {
                EntityLivingBase mount = (EntityLivingBase) entity.ridingEntity;
                bodyYaw = this.interpolateRotation(mount.prevRenderYawOffset, mount.renderYawOffset, partialTicks);
                float yawDelta = MathHelper.wrapAngleTo180_float(headYaw - bodyYaw);

                if (yawDelta < -85.0F) {
                    yawDelta = -85.0F;
                }

                if (yawDelta >= 85.0F) {
                    yawDelta = 85.0F;
                }

                bodyYaw = headYaw - yawDelta;

                if (yawDelta * yawDelta > 2500.0F) {
                    bodyYaw += yawDelta * 0.2F;
                }
            }

            float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
            this.renderLivingAt(entity, x, y, z);
            float ageInTicks = this.handleRotationFloat(entity, partialTicks);
            this.rotateCorpse(entity, ageInTicks, bodyYaw, partialTicks);
            float scale = 0.0625F;
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            GL11.glScalef(-1.0F, -1.0F, 1.0F);
            this.preRenderCallback(entity, partialTicks);
            GL11.glTranslatef(0.0F, -24.0F * scale - 0.0078125F, 0.0F);
            float limbSwingAmount = entity.prevLimbSwingAmount
                + (entity.limbSwingAmount - entity.prevLimbSwingAmount) * partialTicks;
            float limbSwing = entity.limbSwing - entity.limbSwingAmount * (1.0F - partialTicks);

            if (entity.isChild()) {
                limbSwing *= 3.0F;
            }

            if (limbSwingAmount > 1.0F) {
                limbSwingAmount = 1.0F;
            }

            GL11.glEnable(GL11.GL_ALPHA_TEST);
            this.mainModel.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
            this.renderModel(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw - bodyYaw, pitch, scale);
            int colorMultiplier;

            for (int pass = 0; pass < 4; ++pass) {
                int passFlags = this.shouldRenderPass(entity, pass, partialTicks);

                if (passFlags > 0) {
                    this.renderPassModel.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
                    this.renderPassModel.render(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw - bodyYaw, pitch, scale);

                    if ((passFlags & 240) == 16) {
                        this.func_82408_c(entity, pass, partialTicks);
                        this.renderPassModel.render(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw - bodyYaw, pitch, scale);
                    }
                    // patch start
                    if (CITUtils.setupArmorEnchantments(entity, pass)) {
                        while (CITUtils.preRenderArmorEnchantment()) {
                            this.renderPassModel.render(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw - bodyYaw, pitch, scale);
                            CITUtils.postRenderArmorEnchantment();
                        }
                    } else if ((passFlags & 15) == 15) {
                        // if -> else if
                        // patch end
                        float glintTime = (float) entity.ticksExisted + partialTicks;
                        this.bindTexture(RES_ITEM_GLINT);
                        GL11.glEnable(GL11.GL_BLEND);
                        GL11.glColor4f(0.5F, 0.5F, 0.5F, 1.0F);
                        GL11.glDepthFunc(GL11.GL_EQUAL);
                        GL11.glDepthMask(false);

                        for (int layer = 0; layer < 2; ++layer) {
                            GL11.glDisable(GL11.GL_LIGHTING);
                            float glintShade = 0.76F;
                            GL11.glColor4f(0.5F * glintShade, 0.25F * glintShade, 0.8F * glintShade, 1.0F);
                            GL11.glBlendFunc(GL11.GL_SRC_COLOR, GL11.GL_ONE);
                            GL11.glMatrixMode(GL11.GL_TEXTURE);
                            GL11.glLoadIdentity();
                            float glintScroll = glintTime * (0.001F + (float) layer * 0.003F) * 20.0F;
                            float glintScale = 0.33333334F;
                            GL11.glScalef(glintScale, glintScale, glintScale);
                            GL11.glRotatef(30.0F - (float) layer * 60.0F, 0.0F, 0.0F, 1.0F);
                            GL11.glTranslatef(0.0F, glintScroll, 0.0F);
                            GL11.glMatrixMode(GL11.GL_MODELVIEW);
                            this.renderPassModel.render(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw - bodyYaw, pitch, scale);
                        }

                        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                        GL11.glMatrixMode(GL11.GL_TEXTURE);
                        GL11.glDepthMask(true);
                        GL11.glLoadIdentity();
                        GL11.glMatrixMode(GL11.GL_MODELVIEW);
                        GL11.glEnable(GL11.GL_LIGHTING);
                        GL11.glDisable(GL11.GL_BLEND);
                        GL11.glDepthFunc(GL11.GL_LEQUAL);
                    }

                    GL11.glDisable(GL11.GL_BLEND);
                    GL11.glEnable(GL11.GL_ALPHA_TEST);
                }
            }

            GL11.glDepthMask(true);
            this.renderEquippedItems(entity, partialTicks);
            float brightness = entity.getBrightness(partialTicks);
            colorMultiplier = this.getColorMultiplier(entity, brightness, partialTicks);
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);

            if ((colorMultiplier >> 24 & 255) > 0 || entity.hurtTime > 0 || entity.deathTime > 0) {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glDisable(GL11.GL_ALPHA_TEST);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glDepthFunc(GL11.GL_EQUAL);

                if (entity.hurtTime > 0 || entity.deathTime > 0) {
                    GL11.glColor4f(brightness, 0.0F, 0.0F, 0.4F);
                    this.mainModel.render(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw - bodyYaw, pitch, scale);

                    for (int hurtPass = 0; hurtPass < 4; ++hurtPass) {
                        if (this.inheritRenderPass(entity, hurtPass, partialTicks) >= 0) {
                            GL11.glColor4f(brightness, 0.0F, 0.0F, 0.4F);
                            this.renderPassModel.render(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw - bodyYaw, pitch, scale);
                        }
                    }
                }

                if ((colorMultiplier >> 24 & 255) > 0) {
                    float overlayRed = (float) (colorMultiplier >> 16 & 255) / 255.0F;
                    float overlayGreen = (float) (colorMultiplier >> 8 & 255) / 255.0F;
                    float overlayBlue = (float) (colorMultiplier & 255) / 255.0F;
                    float overlayAlpha = (float) (colorMultiplier >> 24 & 255) / 255.0F;
                    GL11.glColor4f(overlayRed, overlayGreen, overlayBlue, overlayAlpha);
                    this.mainModel.render(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw - bodyYaw, pitch, scale);

                    for (int tintPass = 0; tintPass < 4; ++tintPass) {
                        if (this.inheritRenderPass(entity, tintPass, partialTicks) >= 0) {
                            GL11.glColor4f(overlayRed, overlayGreen, overlayBlue, overlayAlpha);
                            this.renderPassModel.render(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw - bodyYaw, pitch, scale);
                        }
                    }
                }

                GL11.glDepthFunc(GL11.GL_LEQUAL);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glEnable(GL11.GL_ALPHA_TEST);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
            }

            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        } catch (Exception exception) {
            logger.error("Couldn't render entity", exception);
        }

        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glPopMatrix();
        this.passSpecialRender(entity, x, y, z);
        MinecraftForge.EVENT_BUS
            .post(new RenderLivingEvent.Post(entity, (RendererLivingEntity) (Object) this, x, y, z));
    }
}

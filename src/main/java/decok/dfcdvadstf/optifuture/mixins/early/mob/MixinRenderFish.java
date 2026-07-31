package decok.dfcdvadstf.optifuture.mixins.early.mob;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderFish;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.prupe.mcpatcher.mob.LineRenderer;

/**
 * Rewrites the fishing bobber rendering so a custom line renderer can replace
 * the vanilla fishing line. When the custom renderer handles the line, the
 * vanilla tessellated line is skipped; hence it is wrapped in an {@code if}.
 * <p>
 * 重写鱼漂渲染，使自定义线条渲染器能够替代原版的钓鱼线。当自定义
 * 渲染器接管该线条时，跳过原版通过 tessellator 绘制的线，因此将其
 * 包裹进 {@code if} 判断中。
 */
@Mixin(RenderFish.class)
public abstract class MixinRenderFish extends Render {

    /**
     * @author OptiFutureOptimized
     * @reason Wrap the tessellated fishing line behind a custom line renderer,
     *         which requires guarding a whole multi-statement block.
     */
    @SuppressWarnings({ "DuplicatedCode", "ExtractMethodRecommender" })
    @Overwrite
    public void doRender(EntityFishHook hook, double x, double y, double z, float yaw, float partialTicks) {
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, (float) z);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glScalef(0.5F, 0.5F, 0.5F);
        this.bindEntityTexture(hook);
        Tessellator tessellator = Tessellator.instance;
        byte iconColumn = 1;
        byte iconRow = 2;
        float uMin = (float) (iconColumn * 8) / 128.0F;
        float uMax = (float) (iconColumn * 8 + 8) / 128.0F;
        float vMin = (float) (iconRow * 8) / 128.0F;
        float vMax = (float) (iconRow * 8 + 8) / 128.0F;
        float quadSize = 1.0F;
        float halfWidth = 0.5F;
        float halfHeight = 0.5F;
        GL11.glRotatef(180.0F - this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, 1.0F, 0.0F);
        tessellator.addVertexWithUV(0.0F - halfWidth, 0.0F - halfHeight, 0.0D, uMin, vMax);
        tessellator.addVertexWithUV(quadSize - halfWidth, 0.0F - halfHeight, 0.0D, uMax, vMax);
        tessellator.addVertexWithUV(quadSize - halfWidth, 1.0F - halfHeight, 0.0D, uMax, vMin);
        tessellator.addVertexWithUV(0.0F - halfWidth, 1.0F - halfHeight, 0.0D, uMin, vMin);
        tessellator.draw();
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glPopMatrix();

        if (hook.field_146042_b != null) {
            float swing = hook.field_146042_b.getSwingProgress(partialTicks);
            float swingSin = MathHelper.sin(MathHelper.sqrt_float(swing) * (float) Math.PI);
            Vec3 handOffset = Vec3.createVectorHelper(-0.5D, 0.03D, 0.8D);
            handOffset.rotateAroundX(
                -(hook.field_146042_b.prevRotationPitch
                    + (hook.field_146042_b.rotationPitch - hook.field_146042_b.prevRotationPitch) * partialTicks)
                    * (float) Math.PI
                    / 180.0F);
            handOffset.rotateAroundY(
                -(hook.field_146042_b.prevRotationYaw
                    + (hook.field_146042_b.rotationYaw - hook.field_146042_b.prevRotationYaw) * partialTicks)
                    * (float) Math.PI
                    / 180.0F);
            handOffset.rotateAroundY(swingSin * 0.5F);
            handOffset.rotateAroundX(-swingSin * 0.7F);
            double holderX = hook.field_146042_b.prevPosX
                + (hook.field_146042_b.posX - hook.field_146042_b.prevPosX) * (double) partialTicks
                + handOffset.xCoord;
            double holderY = hook.field_146042_b.prevPosY
                + (hook.field_146042_b.posY - hook.field_146042_b.prevPosY) * (double) partialTicks
                + handOffset.yCoord;
            double holderZ = hook.field_146042_b.prevPosZ
                + (hook.field_146042_b.posZ - hook.field_146042_b.prevPosZ) * (double) partialTicks
                + handOffset.zCoord;
            double eyeOffset = hook.field_146042_b == Minecraft.getMinecraft().thePlayer ? 0.0D
                : (double) hook.field_146042_b.getEyeHeight();

            if (this.renderManager.options.thirdPersonView > 0
                || hook.field_146042_b != Minecraft.getMinecraft().thePlayer) {
                float bodyYawRad = (hook.field_146042_b.prevRenderYawOffset
                    + (hook.field_146042_b.renderYawOffset - hook.field_146042_b.prevRenderYawOffset) * partialTicks)
                    * (float) Math.PI
                    / 180.0F;
                double sinYaw = MathHelper.sin(bodyYawRad);
                double cosYaw = MathHelper.cos(bodyYawRad);
                holderX = hook.field_146042_b.prevPosX
                    + (hook.field_146042_b.posX - hook.field_146042_b.prevPosX) * (double) partialTicks
                    - cosYaw * 0.35D
                    - sinYaw * 0.85D;
                holderY = hook.field_146042_b.prevPosY + eyeOffset
                    + (hook.field_146042_b.posY - hook.field_146042_b.prevPosY) * (double) partialTicks
                    - 0.45D;
                holderZ = hook.field_146042_b.prevPosZ
                    + (hook.field_146042_b.posZ - hook.field_146042_b.prevPosZ) * (double) partialTicks
                    - sinYaw * 0.35D
                    + cosYaw * 0.85D;
            }

            double hookX = hook.prevPosX + (hook.posX - hook.prevPosX) * (double) partialTicks;
            double hookY = hook.prevPosY + (hook.posY - hook.prevPosY) * (double) partialTicks + 0.25D;
            double hookZ = hook.prevPosZ + (hook.posZ - hook.prevPosZ) * (double) partialTicks;
            double deltaX = (float) (holderX - hookX);
            double deltaY = (float) (holderY - hookY);
            double deltaZ = (float) (holderZ - hookZ);
            // patch start (= if statement)
            if (!LineRenderer.renderLine(0, x, y, z, deltaX, deltaY, deltaZ)) {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glDisable(GL11.GL_LIGHTING);
                tessellator.startDrawing(3);
                tessellator.setColorOpaque_I(0);
                byte segments = 16;

                for (int segment = 0; segment <= segments; ++segment) {
                    float progress = (float) segment / (float) segments;
                    tessellator.addVertex(
                        x + deltaX * (double) progress,
                        y + deltaY * (double) (progress * progress + progress) * 0.5D + 0.25D,
                        z + deltaZ * (double) progress);
                }

                tessellator.draw();
                GL11.glEnable(GL11.GL_LIGHTING);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
            }
        }
    }
}

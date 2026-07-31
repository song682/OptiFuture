package decok.dfcdvadstf.optifuture.mixins.early.mob;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.EntityLiving;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.prupe.mcpatcher.mob.LineRenderer;

/**
 * Rewrites the leash rendering so a custom line renderer can replace the
 * vanilla leash geometry. When the custom renderer handles the line, the
 * vanilla tessellated rope is skipped; hence the vanilla block is wrapped in an
 * {@code if}.
 * <p>
 * 重写拴绳渲染，使自定义线条渲染器能够替代原版拴绳几何体。当自定义
 * 渲染器接管该线条时，跳过原版通过 tessellator 绘制的绳索，因此原版
 * 代码块被包裹进 {@code if} 判断中。
 */
@Mixin(RenderLiving.class)
public abstract class MixinRenderLiving extends RendererLivingEntity {

    public MixinRenderLiving(ModelBase modelBase, float shadowSize) {
        super(modelBase, shadowSize);
    }

    @Shadow
    protected abstract double func_110828_a(double p_110828_1_, double p_110828_3_, double p_110828_5_);

    /**
     * @author OptiFutureOptimized
     * @reason Wrap the tessellated leash geometry behind a custom line renderer,
     *         which requires guarding a whole multi-statement block.
     */
    @SuppressWarnings("DuplicatedCode")
    @Overwrite
    protected void func_110827_b(EntityLiving leashed, double x, double y, double z, float n, float partialTicks) {
        Entity holder = leashed.getLeashedToEntity();

        if (holder != null) {
            y -= (1.6D - (double) leashed.height) * 0.5D;
            Tessellator tessellator = Tessellator.instance;
            double holderYaw = this.func_110828_a(holder.prevRotationYaw, holder.rotationYaw, partialTicks * 0.5F)
                * 0.01745329238474369D;
            double holderPitch = this.func_110828_a(holder.prevRotationPitch, holder.rotationPitch, partialTicks * 0.5F)
                * 0.01745329238474369D;
            double cosYaw = Math.cos(holderYaw);
            double sinYaw = Math.sin(holderYaw);
            double sinPitch = Math.sin(holderPitch);

            if (holder instanceof EntityHanging) {
                cosYaw = 0.0D;
                sinYaw = 0.0D;
                sinPitch = -1.0D;
            }

            double cosPitch = Math.cos(holderPitch);
            double holderX = this.func_110828_a(holder.prevPosX, holder.posX, partialTicks)
                - cosYaw * 0.7D - sinYaw * 0.5D * cosPitch;
            double holderY = this.func_110828_a(
                holder.prevPosY + (double) holder.getEyeHeight() * 0.7D,
                holder.posY + (double) holder.getEyeHeight() * 0.7D,
                partialTicks) - sinPitch * 0.5D - 0.25D;
            double holderZ = this.func_110828_a(holder.prevPosZ, holder.posZ, partialTicks)
                - sinYaw * 0.7D + cosYaw * 0.5D * cosPitch;
            double bodyYaw = this.func_110828_a(leashed.prevRenderYawOffset, leashed.renderYawOffset, partialTicks)
                * 0.01745329238474369D + (Math.PI / 2D);
            double offsetX = Math.cos(bodyYaw) * (double) leashed.width * 0.4D;
            double offsetZ = Math.sin(bodyYaw) * (double) leashed.width * 0.4D;
            double mobX = this.func_110828_a(leashed.prevPosX, leashed.posX, partialTicks) + offsetX;
            double mobY = this.func_110828_a(leashed.prevPosY, leashed.posY, partialTicks);
            double mobZ = this.func_110828_a(leashed.prevPosZ, leashed.posZ, partialTicks) + offsetZ;
            x += offsetX;
            z += offsetZ;
            double deltaX = (float) (holderX - mobX);
            double deltaY = (float) (holderY - mobY);
            double deltaZ = (float) (holderZ - mobZ);
            // patch start (only change is if-wrapper)
            if (!LineRenderer.renderLine(1, x, y, z, deltaX, deltaY, deltaZ)) {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glDisable(GL11.GL_CULL_FACE);
                tessellator.startDrawing(5);
                int segment;
                float progress;

                for (segment = 0; segment <= 24; ++segment) {
                    if (segment % 2 == 0) {
                        tessellator.setColorRGBA_F(0.5F, 0.4F, 0.3F, 1.0F);
                    } else {
                        tessellator.setColorRGBA_F(0.35F, 0.28F, 0.21000001F, 1.0F);
                    }

                    progress = (float) segment / 24.0F;
                    tessellator.addVertex(
                        x + deltaX * (double) progress + 0.0D,
                        y + deltaY * (double) (progress * progress + progress) * 0.5D
                            + (double) ((24.0F - (float) segment) / 18.0F + 0.125F),
                        z + deltaZ * (double) progress);
                    tessellator.addVertex(
                        x + deltaX * (double) progress + 0.025D,
                        y + deltaY * (double) (progress * progress + progress) * 0.5D
                            + (double) ((24.0F - (float) segment) / 18.0F + 0.125F)
                            + 0.025D,
                        z + deltaZ * (double) progress);
                }

                tessellator.draw();
                tessellator.startDrawing(5);

                for (segment = 0; segment <= 24; ++segment) {
                    if (segment % 2 == 0) {
                        tessellator.setColorRGBA_F(0.5F, 0.4F, 0.3F, 1.0F);
                    } else {
                        tessellator.setColorRGBA_F(0.35F, 0.28F, 0.21000001F, 1.0F);
                    }

                    progress = (float) segment / 24.0F;
                    tessellator.addVertex(
                        x + deltaX * (double) progress + 0.0D,
                        y + deltaY * (double) (progress * progress + progress) * 0.5D
                            + (double) ((24.0F - (float) segment) / 18.0F + 0.125F)
                            + 0.025D,
                        z + deltaZ * (double) progress);
                    tessellator.addVertex(
                        x + deltaX * (double) progress + 0.025D,
                        y + deltaY * (double) (progress * progress + progress) * 0.5D
                            + (double) ((24.0F - (float) segment) / 18.0F + 0.125F),
                        z + deltaZ * (double) progress + 0.025D);
                }

                tessellator.draw();
                GL11.glEnable(GL11.GL_LIGHTING);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
            }
            // patch end
            GL11.glEnable(GL11.GL_CULL_FACE);
        }
    }
}

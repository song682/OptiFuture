package decok.dfcdvadstf.optifuture.mixins.early.cc.client.particle;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.client.particle.EntityDropParticleFX;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.prupe.mcpatcher.cc.ColorizeBlock;
import com.prupe.mcpatcher.cc.ColorizeEntity;
import com.prupe.mcpatcher.cc.Colorizer;

/**
 * Custom-colors integration for dripping water/lava particles. The initial color
 * is set at construction time, while {@link #onUpdate()} recomputes the tint every
 * tick because vanilla itself refreshes the drip color on each update.
 * <p>
 * 为滴落的水/岩浆粒子接入自定义颜色。初始颜色在构造时设置；由于原版本身每 tick 都会刷新
 * 滴落颜色，因此 {@link #onUpdate()} 每 tick 重新计算色调。
 */
@Mixin(EntityDropParticleFX.class)
public abstract class MixinEntityDropParticleFX extends EntityFX {

    @Shadow
    private Material materialType;

    @Shadow
    private int bobTimer;

    protected MixinEntityDropParticleFX(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Inject(
        method = "<init>(Lnet/minecraft/world/World;DDDLnet/minecraft/block/material/Material;)V",
        at = @At("RETURN"))
    private void optiFuture$tintDropInit(World worldIn, double x, double y, double z, Material material,
        CallbackInfo ci) {
        if (material == Material.water) {
            if (ColorizeBlock.computeWaterColor(true, (int) this.posX, (int) this.posY, (int) this.posZ)) {
                this.particleRed = Colorizer.setColor[0];
                this.particleGreen = Colorizer.setColor[1];
                this.particleBlue = Colorizer.setColor[2];
            } else {
                this.particleRed = 0.2f;
                this.particleGreen = 0.3f;
                this.particleBlue = 1.0f;
            }
        } else {
            this.particleRed = 1.0F;
            this.particleGreen = 0.0F;
            this.particleBlue = 0.0F;
        }
    }

    /**
     * @author OptiFutureOptimized
     * @reason The tick color must be recomputed each frame; an injection would run
     *         after vanilla has already overwritten the channels, so the whole tick
     *         body is reimplemented with the custom-colors branch inlined.
     */
    @SuppressWarnings("DuplicatedCode")
    @Overwrite
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (this.materialType != Material.water) {
            // Patch start
            if (ColorizeEntity.computeLavaDropColor(40 - this.bobTimer)) {
                this.particleRed = Colorizer.setColor[0];
                this.particleGreen = Colorizer.setColor[1];
                this.particleBlue = Colorizer.setColor[2];
            } else {
                this.particleRed = 1.0f;
                this.particleGreen = 16.0f / (40 - this.bobTimer + 16);
                this.particleBlue = 4.0f / (40 - this.bobTimer + 8);
            }
            // Patch end
        } else {
            // Water drop branch: vanilla resets to (0.2, 0.3, 1.0) every tick; recompute the
            // biome water color like the constructor does instead of the lava formula that
            // was mistakenly copied here.
            // 水滴分支：原版每 tick 重置为 (0.2, 0.3, 1.0)；这里按构造器的方式重新计算
            // 群系水色，而不是之前误拷的岩浆颜色公式。
            if (ColorizeBlock.computeWaterColor(true, (int) this.posX, (int) this.posY, (int) this.posZ)) {
                this.particleRed = Colorizer.setColor[0];
                this.particleGreen = Colorizer.setColor[1];
                this.particleBlue = Colorizer.setColor[2];
            } else {
                this.particleRed = 0.2F;
                this.particleGreen = 0.3F;
                this.particleBlue = 1.0F;
            }
        }

        this.motionY -= this.particleGravity;

        if (this.bobTimer-- > 0) {
            this.motionX *= 0.02D;
            this.motionY *= 0.02D;
            this.motionZ *= 0.02D;
            this.setParticleTextureIndex(113);
        } else {
            this.setParticleTextureIndex(112);
        }

        this.moveEntity(this.motionX, this.motionY, this.motionZ);
        this.motionX *= 0.9800000190734863D;
        this.motionY *= 0.9800000190734863D;
        this.motionZ *= 0.9800000190734863D;

        if (this.particleMaxAge-- <= 0) {
            this.setDead();
        }

        if (this.onGround) {
            if (this.materialType == Material.water) {
                this.setDead();
                this.worldObj.spawnParticle("splash", this.posX, this.posY, this.posZ, 0.0D, 0.0D, 0.0D);
            } else {
                this.setParticleTextureIndex(114);
            }

            this.motionX *= 0.699999988079071D;
            this.motionZ *= 0.699999988079071D;
        }

        Material material = this.worldObj
            .getBlock(
                MathHelper.floor_double(this.posX),
                MathHelper.floor_double(this.posY),
                MathHelper.floor_double(this.posZ))
            .getMaterial();

        if (material.isLiquid() || material.isSolid()) {
            double d0 = (float) (MathHelper.floor_double(this.posY) + 1) - BlockLiquid.getLiquidHeightPercent(
                this.worldObj.getBlockMetadata(
                    MathHelper.floor_double(this.posX),
                    MathHelper.floor_double(this.posY),
                    MathHelper.floor_double(this.posZ)));

            if (this.posY < d0) {
                this.setDead();
            }
        }
    }
}

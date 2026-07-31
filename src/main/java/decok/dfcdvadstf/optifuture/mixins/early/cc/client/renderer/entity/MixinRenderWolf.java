package decok.dfcdvadstf.optifuture.mixins.early.cc.client.renderer.entity;

import net.minecraft.client.renderer.entity.RenderWolf;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityWolf;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.prupe.mcpatcher.cc.ColorizeEntity;

/**
 * Recolors the wolf's collar render pass so dyed collars follow pack-defined
 * colors instead of the fixed vanilla dye palette.
 * <p>
 * 重着色狼的项圈渲染阶段，使染色项圈跟随资源包定义的颜色，而非原版固定的染料调色板。
 */
@Mixin(RenderWolf.class)
public class MixinRenderWolf {

    @Redirect(
        method = "shouldRenderPass(Lnet/minecraft/entity/passive/EntityWolf;IF)I",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glColor3f(FFF)V", ordinal = 1, remap = false))
    private void optiFuture$recolorCollar(float red, float green, float blue, EntityWolf wolf) {
        int collarColor = wolf.getCollarColor();
        float[] rgb = ColorizeEntity.getWolfCollarColor(EntitySheep.fleeceColorTable[collarColor], collarColor);
        GL11.glColor3f(rgb[0], rgb[1], rgb[2]);
    }
}

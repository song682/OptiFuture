package decok.dfcdvadstf.optifuture.mixins.early.cit.client.renderer.entity;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.prupe.mcpatcher.cit.CITUtils;

/**
 * Redirects the armor texture binding of biped mobs through the CIT resolver so
 * custom armor textures apply in both the base and overlay armor render passes.
 * <p>
 * 将双足生物的护甲材质绑定经由 CIT 解析器重定向，使自定义护甲材质
 * 在基础与叠加两个护甲渲染阶段均生效。
 */
@Mixin(RenderBiped.class)
public abstract class MixinRenderBiped extends RenderLiving {

    public MixinRenderBiped(ModelBase modelBase, float shadowSize) {
        super(modelBase, shadowSize);
    }

    @Redirect(
        method = "shouldRenderPass(Lnet/minecraft/entity/EntityLiving;IF)I",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/RenderBiped;bindTexture(Lnet/minecraft/util/ResourceLocation;)V"))
    private void optiFuture$bindArmorTexture(RenderBiped renderer, ResourceLocation texture,
        EntityLiving entity, int slotId, float partialTicks) {
        this.bindTexture(CITUtils.getArmorTexture(texture, entity, entity.func_130225_q(3 - slotId)));
    }

    @Redirect(
        method = "func_82408_c(Lnet/minecraft/entity/EntityLiving;IF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/RenderBiped;bindTexture(Lnet/minecraft/util/ResourceLocation;)V"))
    private void optiFuture$bindArmorOverlayTexture(RenderBiped renderer, ResourceLocation texture,
        EntityLiving entity, int slotId, float partialTicks) {
        this.bindTexture(CITUtils.getArmorTexture(texture, entity, entity.func_130225_q(3 - slotId)));
    }
}

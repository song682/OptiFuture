package decok.dfcdvadstf.optifuture.mixins.early.cit.client.renderer.entity;

import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.prupe.mcpatcher.cit.CITUtils;

/**
 * Routes the icon of thrown/dropped item entities (snowballs, eggs, etc.)
 * through the CIT resolver so per-entity texture overrides apply.
 * <p>
 * 将投掷/掉落物实体（雪球、鸡蛋等）的图标经由 CIT 解析器路由，
 * 使按实体的材质覆盖生效。
 */
@Mixin(RenderSnowball.class)
public abstract class MixinRenderSnowball {

    @Redirect(
        method = "doRender(Lnet/minecraft/entity/Entity;DDDFF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/item/Item;getIconFromDamage(I)Lnet/minecraft/util/IIcon;"))
    private IIcon optiFuture$resolveEntityIcon(Item item, int damage, Entity entity) {
        return CITUtils.getEntityIcon(item.getIconFromDamage(damage), entity);
    }
}

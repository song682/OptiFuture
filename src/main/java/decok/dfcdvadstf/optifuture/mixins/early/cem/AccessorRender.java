package decok.dfcdvadstf.optifuture.mixins.early.cem;

import net.minecraft.client.renderer.entity.Render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Write access to the protected shadow size of entity renderers, used to apply the
 * "shadowSize" field of ".jem" models ("cem_model.txt").
 * <p>
 * 实体渲染器 protected 阴影大小的写访问，用于应用 ".jem" 模型的 "shadowSize"
 * 字段（"cem_model.txt"）。
 */
@Mixin(Render.class)
public interface AccessorRender {

    @Accessor("shadowSize")
    void setShadowSize(float shadowSize);
}

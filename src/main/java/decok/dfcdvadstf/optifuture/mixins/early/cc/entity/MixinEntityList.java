package decok.dfcdvadstf.optifuture.mixins.early.cc.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.prupe.mcpatcher.cc.ColorizeItem;

/**
 * Records each entity's vanilla spawn-egg shell/spot colors as they are
 * registered, so the custom-colors engine can later override spawn-egg tints by
 * entity name.
 * <p>
 * 在实体注册时记录其原版刷怪蛋的外壳/斑点颜色，使自定义颜色引擎稍后能够按实体名覆盖刷怪蛋色调。
 */
@Mixin(EntityList.class)
public abstract class MixinEntityList {

    @Inject(method = "addMapping(Ljava/lang/Class;Ljava/lang/String;III)V", at = @At("HEAD"))
    private static void optiFuture$captureSpawnEggColors(Class<? extends Entity> entityClass, String entityName,
        int entityId, int shellColor, int spotColor, CallbackInfo ci) {
        ColorizeItem.setupSpawnerEgg(entityName, entityId, shellColor, spotColor);
    }
}

package decok.dfcdvadstf.optifuture.mixins.early.at;

import java.util.HashMap;

import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the potion liquid-color cache ({@code field_77925_n}) so the
 * custom-colors logic can clear it when the colours are reloaded. The body is
 * never used at runtime: Mixin replaces it with a generated static accessor on
 * the target class during application.
 * <p>
 * 暴露药水液体颜色缓存（{@code field_77925_n}），使自定义颜色逻辑在颜色
 * 重载时能清空它。方法体在运行时不会被执行：Mixin 应用期间会用生成在
 * 目标类上的静态访问器替换它。
 */
@Mixin(PotionHelper.class)
public interface MixinPotionHelperAccess {

    @Accessor("field_77925_n")
    static HashMap<Potion, Integer> getColorCache() {
        throw new AssertionError("Untransformed @Accessor");
    }
}

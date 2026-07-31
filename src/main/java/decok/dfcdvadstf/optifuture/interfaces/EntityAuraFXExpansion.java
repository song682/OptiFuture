package decok.dfcdvadstf.optifuture.interfaces;

import net.minecraft.client.particle.EntityAuraFX;

/**
 * Exposes the particle colour factory of EntityAuraFX to the custom-colors
 * particle logic. Implemented by {@code MixinEntityAuraFX}; the core logic
 * calls {@link #colorize()} to obtain a recolored aura particle.
 * <p>
 * 向自定义颜色粒子逻辑暴露 EntityAuraFX 的粒子颜色工厂。由
 * {@code MixinEntityAuraFX} 实现；核心逻辑调用 {@link #colorize()} 获取
 * 重新着色后的 aura 粒子。
 */
public interface EntityAuraFXExpansion {

    EntityAuraFX colorize();
}

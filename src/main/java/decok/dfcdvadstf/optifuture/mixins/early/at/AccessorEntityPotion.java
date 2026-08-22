package decok.dfcdvadstf.optifuture.mixins.early.at;

import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityPotion.class)
public interface AccessorEntityPotion {

    @Accessor("potionDamage")
    ItemStack getPotionDamage();
}

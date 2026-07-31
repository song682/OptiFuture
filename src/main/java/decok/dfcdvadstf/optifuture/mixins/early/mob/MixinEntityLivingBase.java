package decok.dfcdvadstf.optifuture.mixins.early.mob;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.prupe.mcpatcher.mob.MobRandomizer;

/**
 * Persists the random-mob "extra info" (which random skin variant an entity
 * was assigned) alongside the entity's NBT so the choice survives save/load.
 * <p>
 * 将随机生物的"附加信息"（该实体被分配到的随机皮肤变体）随实体 NBT
 * 一同持久化，使该选择在存档保存/读取后保持不变。
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase {

    @Inject(method = "writeEntityToNBT(Lnet/minecraft/nbt/NBTTagCompound;)V", at = @At("HEAD"))
    private void optiFuture$saveMobVariant(NBTTagCompound tagCompound, CallbackInfo ci) {
        MobRandomizer.ExtraInfo.writeToNBT((EntityLivingBase) (Object) this, tagCompound);
    }

    @Inject(method = "readEntityFromNBT(Lnet/minecraft/nbt/NBTTagCompound;)V", at = @At("HEAD"))
    private void optiFuture$loadMobVariant(NBTTagCompound tagCompound, CallbackInfo ci) {
        MobRandomizer.ExtraInfo.readFromNBT((EntityLivingBase) (Object) this, tagCompound);
    }
}

package decok.dfcdvadstf.optifuture.mixins.early.cit.nbt;

import java.util.Collection;
import java.util.Map;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import decok.dfcdvadstf.optifuture.interfaces.NBTTagCompoundExpansion;

/**
 * Exposes the internal tag map of an {@link NBTTagCompound} as a read-only
 * collection, giving the CIT system a way to iterate stored NBT entries.
 * <p>
 * 将 {@link NBTTagCompound} 的内部标签映射以只读集合形式暴露，
 * 使 CIT 系统能够遍历已存储的 NBT 条目。
 */
@Mixin(NBTTagCompound.class)
public abstract class MixinNBTTagCompound implements NBTTagCompoundExpansion {

    @Shadow
    private Map<String, NBTBase> tagMap;

    @Override
    public Collection<NBTBase> getTags() {
        return this.tagMap.values();
    }
}

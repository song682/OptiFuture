package decok.dfcdvadstf.optifuture.mixins.early.cit.nbt;

import java.util.List;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import decok.dfcdvadstf.optifuture.interfaces.NBTTagListExpansion;

/**
 * Exposes indexed access to the raw entries of an {@link NBTTagList}, letting
 * the CIT system read list elements without the type-specific accessors.
 * <p>
 * 为 {@link NBTTagList} 的原始条目提供按索引访问，使 CIT 系统无需
 * 依赖按类型的访问方法即可读取列表元素。
 */
@Mixin(NBTTagList.class)
public class MixinNBTTagList implements NBTTagListExpansion {

    @Shadow
    private List<NBTBase> tagList;

    @Override
    public NBTBase tagAt(final int index) {
        return this.tagList.get(index);
    }
}

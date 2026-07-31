package decok.dfcdvadstf.optifuture.interfaces;

import java.util.Collection;

import net.minecraft.nbt.NBTBase;

/**
 * Exposes the tag collection of NBTTagCompound to the custom-item-textures
 * NBT traversal. Implemented by {@code MixinNBTTagCompound}; the core logic
 * iterates {@link #getTags()} to search for the CIT NBT markers.
 * <p>
 * 向自定义物品材质（CIT）的 NBT 遍历逻辑暴露 NBTTagCompound 的标签
 * 集合视图。由 {@code MixinNBTTagCompound} 实现；核心逻辑遍历
 * {@link #getTags()} 以查找 CIT 的 NBT 标记。
 */
public interface NBTTagCompoundExpansion {

    Collection<NBTBase> getTags();
}

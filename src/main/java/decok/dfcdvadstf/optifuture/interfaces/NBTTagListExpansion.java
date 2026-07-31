package decok.dfcdvadstf.optifuture.interfaces;

import net.minecraft.nbt.NBTBase;

/**
 * Exposes index-based tag access of NBTTagList to the custom-item-textures
 * NBT traversal. Implemented by {@code MixinNBTTagList}; the core logic calls
 * {@link #tagAt(int)} to reach individual entries of the list.
 * <p>
 * 向自定义物品材质（CIT）的 NBT 遍历逻辑暴露 NBTTagList 的按索引
 * 取标签能力。由 {@code MixinNBTTagList} 实现；核心逻辑调用
 * {@link #tagAt(int)} 访问列表中的单个条目。
 */
public interface NBTTagListExpansion {

    NBTBase tagAt(final int n);
}

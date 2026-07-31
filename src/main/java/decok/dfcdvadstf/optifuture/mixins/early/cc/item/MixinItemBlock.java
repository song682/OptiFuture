package decok.dfcdvadstf.optifuture.mixins.early.cc.item;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Routes the item-inventory tint of a placed block back to the block's own
 * render color, so custom color maps affect the held/inventory icon too.
 * <p>
 * 将方块物品在物品栏中的着色重新路由到方块自身的渲染颜色，
 * 使自定义颜色映射同样作用于手持/物品栏中的图标。
 */
@Mixin(ItemBlock.class)
public abstract class MixinItemBlock extends Item {

    /** The block backing this ItemBlock. / 该 ItemBlock 所对应的方块。 */
    @Final
    @Shadow
    public Block field_150939_a;

    @Override
    public int getColorFromItemStack(final ItemStack itemStack, final int meta) {
        final Block backingBlock = this.field_150939_a;
        if (backingBlock != null) {
            return backingBlock.getRenderColor(meta);
        }
        return super.getColorFromItemStack(itemStack, meta);
    }
}

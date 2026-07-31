package decok.dfcdvadstf.optifuture.mixins.early.cc.client.renderer;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.prupe.mcpatcher.cc.ColorizeBlock;

/**
 * Applies the biome water tint to held block items rendered in 2D, so a water
 * block held in hand matches its in-world custom color.
 * <p>
 * 为以 2D 方式渲染的手持方块物品应用群系水色，使手持的水方块与其世界中的自定义颜色一致。
 */
@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {

    @Inject(
        method = "renderItem(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;ILnet/minecraftforge/client/IItemRenderer$ItemRenderType;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemRenderer;renderItemIn2D(Lnet/minecraft/client/renderer/Tessellator;FFFFIIF)V",
            ordinal = 0))
    private void optiFuture$colorizeHeldBlock(EntityLivingBase entity, ItemStack itemStack, int renderPass,
        IItemRenderer.ItemRenderType type, CallbackInfo ci) {
        ColorizeBlock.colorizeWaterBlockGL(Block.getBlockFromItem(itemStack.getItem()));
    }
}

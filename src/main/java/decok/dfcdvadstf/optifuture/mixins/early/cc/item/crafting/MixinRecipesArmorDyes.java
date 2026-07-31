package decok.dfcdvadstf.optifuture.mixins.early.cc.item.crafting;

import net.minecraft.block.BlockColored;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.RecipesArmorDyes;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.prupe.mcpatcher.cc.ColorizeEntity;

/**
 * Reimplements leather-armor dye blending so that dye colors are pulled from
 * the customizable fleece color table instead of the vanilla constants.
 * <p>
 * 重写皮革护甲的染料混合逻辑，使染料颜色取自可自定义的羊毛
 * 颜色表，而非原版的固定常量。
 */
@Mixin(RecipesArmorDyes.class)
public abstract class MixinRecipesArmorDyes {

    /**
     * @author OptiFutureOptimized
     * @reason Redirect leather dye blending through the customizable fleece
     *         color table; a constant/argument redirect is not viable here
     *         because dye contributions are accumulated inside the loop.
     */
    @SuppressWarnings("DuplicatedCode")
    @Overwrite
    public ItemStack getCraftingResult(InventoryCrafting inventoryCrafting) {
        ItemStack dyedArmor = null;
        int[] channelSum = new int[3];
        int brightnessSum = 0;
        int contributions = 0;
        ItemArmor armorItem = null;

        for (int slot = 0; slot < inventoryCrafting.getSizeInventory(); ++slot) {
            ItemStack ingredient = inventoryCrafting.getStackInSlot(slot);
            if (ingredient == null) {
                continue;
            }

            if (ingredient.getItem() instanceof ItemArmor) {
                armorItem = (ItemArmor) ingredient.getItem();

                if (armorItem.getArmorMaterial() != ItemArmor.ArmorMaterial.CLOTH || dyedArmor != null) {
                    return null;
                }

                dyedArmor = ingredient.copy();
                dyedArmor.stackSize = 1;

                if (armorItem.hasColor(ingredient)) {
                    int existingColor = armorItem.getColor(dyedArmor);
                    float red = (float) (existingColor >> 16 & 255) / 255.0F;
                    float green = (float) (existingColor >> 8 & 255) / 255.0F;
                    float blue = (float) (existingColor & 255) / 255.0F;
                    brightnessSum += (int) (Math.max(red, Math.max(green, blue)) * 255.0F);
                    channelSum[0] += (int) (red * 255.0F);
                    channelSum[1] += (int) (green * 255.0F);
                    channelSum[2] += (int) (blue * 255.0F);
                    ++contributions;
                }
            } else {
                if (ingredient.getItem() != Items.dye) {
                    return null;
                }
                // patch: pull the dye color from the customizable fleece color table
                int dyeMeta = BlockColored.func_150032_b(ingredient.getItemDamage());
                float[] dyeColor = ColorizeEntity
                    .getArmorDyeColor(EntitySheep.fleeceColorTable[dyeMeta], dyeMeta);
                int red = (int) (dyeColor[0] * 255.0F);
                int green = (int) (dyeColor[1] * 255.0F);
                int blue = (int) (dyeColor[2] * 255.0F);
                brightnessSum += Math.max(red, Math.max(green, blue));
                channelSum[0] += red;
                channelSum[1] += green;
                channelSum[2] += blue;
                ++contributions;
            }
        }

        if (armorItem == null) {
            return null;
        }

        int avgRed = channelSum[0] / contributions;
        int avgGreen = channelSum[1] / contributions;
        int avgBlue = channelSum[2] / contributions;
        float targetBrightness = (float) brightnessSum / (float) contributions;
        float maxChannel = (float) Math.max(avgRed, Math.max(avgGreen, avgBlue));
        avgRed = (int) ((float) avgRed * targetBrightness / maxChannel);
        avgGreen = (int) ((float) avgGreen * targetBrightness / maxChannel);
        avgBlue = (int) ((float) avgBlue * targetBrightness / maxChannel);
        int packedColor = (avgRed << 8) + avgGreen;
        packedColor = (packedColor << 8) + avgBlue;
        armorItem.func_82813_b(dyedArmor, packedColor);
        return dyedArmor;
    }
}

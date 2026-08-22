package decok.dfcdvadstf.optifuture.mixins.early.gui.accessor;

import net.minecraft.client.gui.GuiEnchantment;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor for the unnamed private custom enchanting table name held by
 * {@link GuiEnchantment} (SRG field_147079_H, no MCP name), used by CustomGuis
 * to match name= rules for enchanting tables.
 * 暴露 {@link GuiEnchantment} 私有的无 MCP 名称附魔台自定义名称
 * （SRG field_147079_H），供 CustomGuis 匹配附魔台的 name= 规则。
 */
@Mixin(GuiEnchantment.class)
public interface AccessorGuiEnchantment {

    @Accessor("field_147079_H")
    String getCustomName();
}

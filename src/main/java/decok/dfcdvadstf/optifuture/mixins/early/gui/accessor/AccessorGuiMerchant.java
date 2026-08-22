package decok.dfcdvadstf.optifuture.mixins.early.gui.accessor;

import net.minecraft.client.gui.GuiMerchant;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor for the unnamed private merchant GUI title held by {@link GuiMerchant}
 * (SRG field_147040_A, no MCP name), used by CustomGuis to match name= rules
 * for villager trade screens.
 * 暴露 {@link GuiMerchant} 私有的无 MCP 名称交易界面标题（SRG field_147040_A），
 * 供 CustomGuis 匹配村民交易界面的 name= 规则。
 */
@Mixin(GuiMerchant.class)
public interface AccessorGuiMerchant {

    @Accessor("field_147040_A")
    String getGuiTitle();
}

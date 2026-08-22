package decok.dfcdvadstf.optifuture.mixins.early.gui;

import net.minecraft.client.gui.GuiEnchantment;
import net.minecraft.client.gui.GuiHopper;
import net.minecraft.client.gui.GuiMerchant;
import net.minecraft.client.gui.GuiRepair;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiBeacon;
import net.minecraft.client.gui.inventory.GuiBrewingStand;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.gui.inventory.GuiDispenser;
import net.minecraft.client.gui.inventory.GuiFurnace;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.gui.inventory.GuiScreenHorseInventory;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.prupe.mcpatcher.gui.CustomGuis;

/**
 * Hooks the container GUI background rendering for CustomGuis (custom_guis rules).
 * Applied to every vanilla container GUI class: each one binds its background texture
 * inside its own drawGuiContainerBackgroundLayer override, so a single ModifyArg on that
 * method intercepts all of them, including the creative inventory's per-tab textures.
 * 为 CustomGuis（custom_guis 规则）挂钩容器 GUI 背景渲染。该 mixin 同时应用于全部
 * 原版容器 GUI 类：每个类都在自身的 drawGuiContainerBackgroundLayer 重写中绑定背景
 * 纹理，因此对同一方法签名的单个 ModifyArg 即可拦截全部调用点，创造模式物品栏的
 * 分页纹理也包含在内。
 */
@Mixin(
    value = { GuiContainer.class, GuiChest.class, GuiCrafting.class, GuiFurnace.class, GuiDispenser.class,
        GuiEnchantment.class, GuiBrewingStand.class, GuiBeacon.class, GuiMerchant.class, GuiRepair.class,
        GuiHopper.class, GuiScreenHorseInventory.class, GuiInventory.class, GuiContainerCreative.class })
public abstract class MixinGuiContainer {

    @ModifyArg(
        method = "drawGuiContainerBackgroundLayer(FII)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V"))
    private ResourceLocation remapContainerTexture(ResourceLocation location) {
        return CustomGuis.remapTexture((GuiScreen) (Object) this, location);
    }
}

package decok.dfcdvadstf.optifuture.mixins.early.at;

import net.minecraft.client.gui.FontRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FontRenderer.class)
public interface AccessorFontRenderer {

    @Invoker("readFontTexture")
    void callReadFontTexture();
}

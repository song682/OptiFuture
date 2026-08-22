package decok.dfcdvadstf.optifuture.mixins.early.at;

import java.io.File;
import java.util.Map;

import net.minecraft.client.resources.DefaultResourcePack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DefaultResourcePack.class)
public interface AccessorDefaultResourcePack {

    @Accessor("field_152781_b")
    Map<String, File> getFileMap();
}

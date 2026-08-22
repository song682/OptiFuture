package decok.dfcdvadstf.optifuture.mixins.early.at;

import java.io.IOException;
import java.util.zip.ZipFile;

import net.minecraft.client.resources.FileResourcePack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FileResourcePack.class)
public interface AccessorFileResourcePack {

    @Invoker("getResourcePackZipFile")
    ZipFile callGetResourcePackZipFile() throws IOException;
}

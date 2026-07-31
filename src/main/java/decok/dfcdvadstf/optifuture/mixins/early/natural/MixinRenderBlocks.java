package decok.dfcdvadstf.optifuture.mixins.early.natural;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.prupe.mcpatcher.natural.NaturalProperties;
import com.prupe.mcpatcher.natural.NaturalTextures;

/**
 * Implements natural textures on top of the vanilla 1.7.10 {@link RenderBlocks}
 * pipeline: every block rendered through {@code renderBlockByRenderType} gets a
 * position based random id, and each of the six {@code renderFace*} methods
 * adds the configured rotation/flip to the face before drawing, restoring the
 * original {@code uvRotate*} / {@code flipTexture} values afterwards. Because
 * the whole pair is applied and reverted around every face call, specialised
 * renderers (cauldron, bed, flowerpot, ...) and the AO path are covered as
 * well, while item rendering keeps vanilla UVs since {@code blockAccess} is
 * null there (random id 0).
 * <p>
 * 基于原版 1.7.10 {@link RenderBlocks} 管线实现自然纹理：每个经由
 * {@code renderBlockByRenderType} 渲染的方块都会得到一个基于坐标的随机 id，
 * 六个 {@code renderFace*} 方法在绘制前为该面叠加配置的旋转/翻转，并在绘制
 * 后恢复原 {@code uvRotate*} / {@code flipTexture} 值。由于每组注入在每次面
 * 调用前后成对应用与还原，专用渲染器（炼药锅、床、花盆……）与 AO 路径同样
 * 生效；物品渲染时 {@code blockAccess} 为 null（随机 id 为 0），保持原版 UV。
 */
@Mixin(RenderBlocks.class)
public abstract class MixinRenderBlocks {

    @Shadow
    public IBlockAccess blockAccess;
    @Shadow
    public int uvRotateBottom;
    @Shadow
    public int uvRotateTop;
    @Shadow
    public int uvRotateNorth;
    @Shadow
    public int uvRotateSouth;
    @Shadow
    public int uvRotateWest;
    @Shadow
    public int uvRotateEast;
    @Shadow
    public boolean flipTexture;

    /** Saved uvRotate* value of the face currently being naturalised. / 当前被自然化的面的 uvRotate* 保存值。 */
    @Unique
    private int optiFuture$savedUvRotate;
    /** Saved flipTexture value of the face currently being naturalised. / 当前被自然化的面的 flipTexture 保存值。 */
    @Unique
    private boolean optiFuture$savedFlipTexture;
    /** Face index currently naturalised, -1 when none. / 当前被自然化的面索引，无时为 -1。 */
    @Unique
    private int optiFuture$naturalFace = -1;

    /**
     * Seeds the per-block random id at the entry of the block render dispatcher.
     * A null {@code blockAccess} means the call is an inventory render, which
     * must stay vanilla, so the id is forced to 0.
     * <p>
     * 在方块渲染分发器入口设置基于方块的随机 id。{@code blockAccess} 为 null
     * 表示这是物品栏渲染，必须保持原版外观，因此强制 id 为 0。
     */
    @Inject(method = "renderBlockByRenderType(Lnet/minecraft/block/Block;III)Z", at = @At("HEAD"))
    private void optiFuture$setRandomBlockId(Block block, int x, int y, int z,
        CallbackInfoReturnable<Boolean> cir) {
        NaturalTextures.randomBlockId = this.blockAccess == null ? 0L : NaturalTextures.getRandomBlockId(x, y, z);
    }

    // Face indices match the renderFace methods: 0 = YNeg (bottom), 1 = YPos (top),
    // 2 = ZNeg (north), 3 = ZPos (south), 4 = XNeg (west), 5 = XPos (east).
    // 面索引与 renderFace 方法对应：0 = YNeg（底）、1 = YPos（顶）、
    // 2 = ZNeg（北）、3 = ZPos（南）、4 = XNeg（西）、5 = XPos（东）。

    @Inject(method = "renderFaceYNeg(Lnet/minecraft/block/Block;DDDLnet/minecraft/util/IIcon;)V", at = @At("HEAD"))
    private void optiFuture$naturalFaceYNeg(Block block, double x, double y, double z, IIcon icon, CallbackInfo ci) {
        optiFuture$applyNatural(0, icon);
    }

    @Inject(method = "renderFaceYNeg(Lnet/minecraft/block/Block;DDDLnet/minecraft/util/IIcon;)V", at = @At("RETURN"))
    private void optiFuture$restoreFaceYNeg(Block block, double x, double y, double z, IIcon icon, CallbackInfo ci) {
        optiFuture$restoreNatural();
    }

    @Inject(method = "renderFaceYPos(Lnet/minecraft/block/Block;DDDLnet/minecraft/util/IIcon;)V", at = @At("HEAD"))
    private void optiFuture$naturalFaceYPos(Block block, double x, double y, double z, IIcon icon, CallbackInfo ci) {
        optiFuture$applyNatural(1, icon);
    }

    @Inject(method = "renderFaceYPos(Lnet/minecraft/block/Block;DDDLnet/minecraft/util/IIcon;)V", at = @At("RETURN"))
    private void optiFuture$restoreFaceYPos(Block block, double x, double y, double z, IIcon icon, CallbackInfo ci) {
        optiFuture$restoreNatural();
    }

    @Inject(method = "renderFaceZNeg(Lnet/minecraft/block/Block;DDDLnet/minecraft/util/IIcon;)V", at = @At("HEAD"))
    private void optiFuture$naturalFaceZNeg(Block block, double x, double y, double z, IIcon icon, CallbackInfo ci) {
        optiFuture$applyNatural(2, icon);
    }

    @Inject(method = "renderFaceZNeg(Lnet/minecraft/block/Block;DDDLnet/minecraft/util/IIcon;)V", at = @At("RETURN"))
    private void optiFuture$restoreFaceZNeg(Block block, double x, double y, double z, IIcon icon, CallbackInfo ci) {
        optiFuture$restoreNatural();
    }

    @Inject(method = "renderFaceZPos(Lnet/minecraft/block/Block;DDDLnet/minecraft/util/IIcon;)V", at = @At("HEAD"))
    private void optiFuture$naturalFaceZPos(Block block, double x, double y, double z, IIcon icon, CallbackInfo ci) {
        optiFuture$applyNatural(3, icon);
    }

    @Inject(method = "renderFaceZPos(Lnet/minecraft/block/Block;DDDLnet/minecraft/util/IIcon;)V", at = @At("RETURN"))
    private void optiFuture$restoreFaceZPos(Block block, double x, double y, double z, IIcon icon, CallbackInfo ci) {
        optiFuture$restoreNatural();
    }

    @Inject(method = "renderFaceXNeg(Lnet/minecraft/block/Block;DDDLnet/minecraft/util/IIcon;)V", at = @At("HEAD"))
    private void optiFuture$naturalFaceXNeg(Block block, double x, double y, double z, IIcon icon, CallbackInfo ci) {
        optiFuture$applyNatural(4, icon);
    }

    @Inject(method = "renderFaceXNeg(Lnet/minecraft/block/Block;DDDLnet/minecraft/util/IIcon;)V", at = @At("RETURN"))
    private void optiFuture$restoreFaceXNeg(Block block, double x, double y, double z, IIcon icon, CallbackInfo ci) {
        optiFuture$restoreNatural();
    }

    @Inject(method = "renderFaceXPos(Lnet/minecraft/block/Block;DDDLnet/minecraft/util/IIcon;)V", at = @At("HEAD"))
    private void optiFuture$naturalFaceXPos(Block block, double x, double y, double z, IIcon icon, CallbackInfo ci) {
        optiFuture$applyNatural(5, icon);
    }

    @Inject(method = "renderFaceXPos(Lnet/minecraft/block/Block;DDDLnet/minecraft/util/IIcon;)V", at = @At("RETURN"))
    private void optiFuture$restoreFaceXPos(Block block, double x, double y, double z, IIcon icon, CallbackInfo ci) {
        optiFuture$restoreNatural();
    }

    /**
     * Adds the natural rotation/flip for one face before it is drawn. The
     * vanilla uvRotate* / flipTexture values set by the caller are kept so the
     * rotation simply stacks on top of them; the original values are restored
     * by {@link #optiFuture$restoreNatural()} right after the face is drawn.
     * <p>
     * 在绘制一个面之前叠加其自然旋转/翻转。调用方设置的原版
     * uvRotate* / flipTexture 值被保留，旋转只是叠加在其上；面绘制完成后由
     * {@link #optiFuture$restoreNatural()} 立即恢复原值。
     *
     * @param face face index 0-5
     * @param icon icon of the face, may be null
     */
    @Unique
    private void optiFuture$applyNatural(int face, IIcon icon) {
        NaturalProperties properties = NaturalTextures.getNaturalProperties(icon);
        if (properties == null) {
            optiFuture$naturalFace = -1;
            return;
        }
        int random = NaturalTextures.getRandomValue(face);
        int rotation = properties.getRotation(random);
        optiFuture$savedUvRotate = optiFuture$getUvRotate(face);
        optiFuture$setUvRotate(face, (optiFuture$savedUvRotate + rotation) & 3);
        optiFuture$savedFlipTexture = this.flipTexture;
        if (properties.getFlip(random)) {
            this.flipTexture = !this.flipTexture;
        }
        optiFuture$naturalFace = face;
    }

    /**
     * Restores the uvRotate* / flipTexture values captured by
     * {@link #optiFuture$applyNatural(int, IIcon)} so that later faces (and the
     * caller's own state) are unaffected.
     * <p>
     * 恢复 {@link #optiFuture$applyNatural(int, IIcon)} 捕获的
     * uvRotate* / flipTexture 值，使后续的面与调用方自身状态不受影响。
     */
    @Unique
    private void optiFuture$restoreNatural() {
        if (optiFuture$naturalFace >= 0) {
            optiFuture$setUvRotate(optiFuture$naturalFace, optiFuture$savedUvRotate);
            this.flipTexture = optiFuture$savedFlipTexture;
            optiFuture$naturalFace = -1;
        }
    }

    /**
     * The uvRotate* field of one face. / 某一面对应的 uvRotate* 字段。
     */
    @Unique
    private int optiFuture$getUvRotate(int face) {
        switch (face) {
            case 0:
                return this.uvRotateBottom;
            case 1:
                return this.uvRotateTop;
            case 2:
                return this.uvRotateNorth;
            case 3:
                return this.uvRotateSouth;
            case 4:
                return this.uvRotateWest;
            case 5:
                return this.uvRotateEast;
            default:
                return 0;
        }
    }

    /**
     * Sets the uvRotate* field of one face. / 设置某一面对应的 uvRotate* 字段。
     */
    @Unique
    private void optiFuture$setUvRotate(int face, int value) {
        switch (face) {
            case 0:
                this.uvRotateBottom = value;
                break;
            case 1:
                this.uvRotateTop = value;
                break;
            case 2:
                this.uvRotateNorth = value;
                break;
            case 3:
                this.uvRotateSouth = value;
                break;
            case 4:
                this.uvRotateWest = value;
                break;
            case 5:
                this.uvRotateEast = value;
                break;
            default:
                break;
        }
    }
}

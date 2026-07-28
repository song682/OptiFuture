package com.prupe.mcpatcher.cem.model;

import java.util.List;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.prupe.mcpatcher.cem.anim.ModelVarType;
import com.prupe.mcpatcher.cem.parse.JpmBox;
import com.prupe.mcpatcher.cem.parse.JpmPart;
import com.prupe.mcpatcher.mal.resource.TexturePackAPI;

/**
 * A model part built from a JPM definition. Extends the vanilla {@link ModelRenderer}
 * so it can replace parts inside vanilla {@code ModelBase} fields (vanilla
 * {@code setRotationAngles} then animates it directly), but fully overrides
 * {@code render} because the vanilla display list machinery is private and CEM
 * needs extras: per-part scale (sx/sy/sz), a per-part texture override and the
 * "visible_boxes" semantic (hide own boxes but still render children).
 * <p>
 * 由 JPM 定义构建的模型部件。继承原版 {@link ModelRenderer}，因此可以替换原版
 * {@code ModelBase} 字段中的部件（原版 {@code setRotationAngles} 会直接驱动它）；
 * 但完全覆写 {@code render}，因为原版的 display list 机制是 private 的，且 CEM
 * 需要额外能力：部件级缩放（sx/sy/sz）、部件级纹理覆盖，以及 "visible_boxes"
 * 语义（隐藏自身盒子但仍渲染子部件）。
 * <p>
 * Coordinate conversion from JPM to vanilla model space (derived from the OptiFine
 * template models, e.g. creeper.jem vs. ModelCreeper):
 * <ul>
 * <li>inverted axis: c' = -c - size, otherwise c' = c</li>
 * <li>world position = c' + (0, 24, 0)</li>
 * <li>part pivot (world) = (-tx, ty + 24, -tz) from "translate"</li>
 * <li>box coordinates relative to pivot = world - pivot</li>
 * <li>rotation sign about one axis = product of the inversion signs of the other two</li>
 * </ul>
 * 从 JPM 到原版模型空间的坐标转换（由 OptiFine 模板模型反推，如 creeper.jem 对照
 * ModelCreeper）：反转轴 c' = -c - size；世界坐标 = c' + (0, 24, 0)；部件枢轴（世界）
 * = (-tx, ty + 24, -tz)；盒子坐标 = 世界坐标 - 枢轴；绕某轴旋转的符号 = 另两轴反转
 * 符号之积。
 */
public class CemModelRenderer extends ModelRenderer {

    /** Vanilla model space is offset 24 px above the ground. / 原版模型空间比地面高 24 像素。 */
    private static final float Y_OFFSET = 24.0f;

    private static final float RAD_TO_DEG = 180.0f / (float) Math.PI;

    /** Per-part texture override, null uses whatever is bound. / 部件级纹理覆盖，null 时沿用已绑定纹理。 */
    private final ResourceLocation texture;

    /** Part pivot in world (vanilla model) space, parent frame for submodels. / 部件枢轴的世界坐标，作为子部件的父坐标系。 */
    private final float[] pivotWorld;

    // Animatable scale, driven by "<model>.sx" etc. / 可动画缩放，由 "<model>.sx" 等驱动
    public float scaleX = 1.0f;
    public float scaleY = 1.0f;
    public float scaleZ = 1.0f;

    /** "visible_boxes": render own boxes; children are unaffected. / "visible_boxes"：是否渲染自身盒子；不影响子部件。 */
    public boolean visibleBoxes = true;

    private boolean compiled;
    private int displayList;

    /**
     * Build a top-level part renderer.
     * <p>
     * 构建一个顶层部件渲染器。
     *
     * @param base           owning model, receives this renderer in its boxList / 所属模型
     * @param part           parsed JPM part / 已解析的 JPM 部件
     * @param defaultTexture model default texture inherited when the part has none /
     *                       部件未指定纹理时继承的模型默认纹理
     */
    public CemModelRenderer(ModelBase base, JpmPart part, ResourceLocation defaultTexture) {
        this(base, part, null, defaultTexture);
    }

    private CemModelRenderer(ModelBase base, JpmPart part, CemModelRenderer parent, ResourceLocation inheritedTexture) {
        super(base, part.id);
        if (part.textureSize != null) {
            textureWidth = part.textureSize[0];
            textureHeight = part.textureSize[1];
        } else if (parent != null) {
            textureWidth = parent.textureWidth;
            textureHeight = parent.textureHeight;
        }
        texture = part.texture != null ? part.texture : inheritedTexture;

        boolean invertX = part.invertAxis.indexOf('x') >= 0;
        boolean invertY = part.invertAxis.indexOf('y') >= 0;
        boolean invertZ = part.invertAxis.indexOf('z') >= 0;
        float tx = part.translate != null ? part.translate[0] : 0.0f;
        float ty = part.translate != null ? part.translate[1] : 0.0f;
        float tz = part.translate != null ? part.translate[2] : 0.0f;

        // Pivot in world space; the rotation point is relative to the parent pivot
        // 世界坐标系下的枢轴；rotationPoint 相对于父枢轴
        pivotWorld = new float[] { -tx, ty + Y_OFFSET, -tz };
        if (parent == null) {
            setRotationPoint(pivotWorld[0], pivotWorld[1], pivotWorld[2]);
        } else {
            setRotationPoint(
                pivotWorld[0] - parent.pivotWorld[0],
                pivotWorld[1] - parent.pivotWorld[1],
                pivotWorld[2] - parent.pivotWorld[2]);
        }

        // Static rotation baked as the initial angles; vanilla or CEM animations may
        // overwrite them each frame
        // 静态旋转烘焙为初始角度；原版或 CEM 动画可能每帧覆盖
        if (part.rotate != null) {
            float signX = (invertY ? -1.0f : 1.0f) * (invertZ ? -1.0f : 1.0f);
            float signY = (invertX ? -1.0f : 1.0f) * (invertZ ? -1.0f : 1.0f);
            float signZ = (invertX ? -1.0f : 1.0f) * (invertY ? -1.0f : 1.0f);
            rotateAngleX = signX * (float) Math.toRadians(part.rotate[0]);
            rotateAngleY = signY * (float) Math.toRadians(part.rotate[1]);
            rotateAngleZ = signZ * (float) Math.toRadians(part.rotate[2]);
        }

        mirror = part.mirrorTexture.indexOf('u') >= 0;
        boolean mirrorV = part.mirrorTexture.indexOf('v') >= 0;

        addBoxes(part.boxes, invertX, invertY, invertZ, mirrorV);
        // Sprites are rendered as plain boxes for now (no per-pixel extrusion yet)
        // sprite 目前按普通盒子渲染（尚未实现逐像素挤出）
        addBoxes(part.sprites, invertX, invertY, invertZ, mirrorV);

        for (JpmPart submodel : part.submodels) {
            addChild(new CemModelRenderer(base, submodel, this, texture));
        }
    }

    /** Convert and add JPM boxes. / 转换并添加 JPM 盒子。 */
    private void addBoxes(List<JpmBox> boxes, boolean invertX, boolean invertY, boolean invertZ, boolean mirrorV) {
        for (JpmBox box : boxes) {
            float[] c = box.coordinates;
            float w = c[3];
            float h = c[4];
            float d = c[5];
            float x = (invertX ? -c[0] - w : c[0]) - pivotWorld[0];
            float y = (invertY ? -c[1] - h : c[1]) + Y_OFFSET - pivotWorld[1];
            float z = (invertZ ? -c[2] - d : c[2]) - pivotWorld[2];
            cubeList.add(new CemModelBox(this, box, x, y, z, w, h, d, mirrorV));
        }
    }

    /**
     * Read an animation variable, booleans map to 1.0/0.0.
     * <p>
     * 读取动画变量，布尔值映射为 1.0/0.0。
     */
    public double getVar(ModelVarType type) {
        switch (type) {
            case TX:
                return rotationPointX;
            case TY:
                return rotationPointY;
            case TZ:
                return rotationPointZ;
            case RX:
                return rotateAngleX;
            case RY:
                return rotateAngleY;
            case RZ:
                return rotateAngleZ;
            case SX:
                return scaleX;
            case SY:
                return scaleY;
            case SZ:
                return scaleZ;
            case VISIBLE:
                return showModel ? 1.0 : 0.0;
            case VISIBLE_BOXES:
                return visibleBoxes ? 1.0 : 0.0;
            default:
                return 0.0;
        }
    }

    /**
     * Write an animation variable, any non-zero value is true.
     * <p>
     * 写入动画变量，非零视为 true。
     */
    public void setVar(ModelVarType type, double value) {
        float f = (float) value;
        switch (type) {
            case TX:
                rotationPointX = f;
                break;
            case TY:
                rotationPointY = f;
                break;
            case TZ:
                rotationPointZ = f;
                break;
            case RX:
                rotateAngleX = f;
                break;
            case RY:
                rotateAngleY = f;
                break;
            case RZ:
                rotateAngleZ = f;
                break;
            case SX:
                scaleX = f;
                break;
            case SY:
                scaleY = f;
                break;
            case SZ:
                scaleZ = f;
                break;
            case VISIBLE:
                showModel = value != 0.0;
                break;
            case VISIBLE_BOXES:
                visibleBoxes = value != 0.0;
                break;
        }
    }

    /**
     * Find a direct child submodel by its "id".
     * <p>
     * 按 "id" 查找直接子部件。
     */
    public CemModelRenderer getChild(String id) {
        if (childModels != null) {
            for (Object child : childModels) {
                if (child instanceof CemModelRenderer && id.equals(((CemModelRenderer) child).boxName)) {
                    return (CemModelRenderer) child;
                }
            }
        }
        return null;
    }

    @Override
    public void render(float scale) {
        if (isHidden || !showModel) {
            return;
        }
        GL11.glPushMatrix();
        GL11.glTranslatef(offsetX, offsetY, offsetZ);
        GL11.glTranslatef(rotationPointX * scale, rotationPointY * scale, rotationPointZ * scale);
        // Same rotation order as the vanilla ModelRenderer: Z, then Y, then X
        // 与原版 ModelRenderer 相同的旋转顺序：Z、Y、X
        if (rotateAngleZ != 0.0f) {
            GL11.glRotatef(rotateAngleZ * RAD_TO_DEG, 0.0f, 0.0f, 1.0f);
        }
        if (rotateAngleY != 0.0f) {
            GL11.glRotatef(rotateAngleY * RAD_TO_DEG, 0.0f, 1.0f, 0.0f);
        }
        if (rotateAngleX != 0.0f) {
            GL11.glRotatef(rotateAngleX * RAD_TO_DEG, 1.0f, 0.0f, 0.0f);
        }
        if (scaleX != 1.0f || scaleY != 1.0f || scaleZ != 1.0f) {
            GL11.glScalef(scaleX, scaleY, scaleZ);
        }
        if (visibleBoxes && !cubeList.isEmpty()) {
            if (texture == null) {
                drawBoxes(scale);
            } else {
                // Bind the part texture and restore the previous one afterwards
                // 绑定部件纹理，绘制后恢复之前的绑定
                int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
                TexturePackAPI.bindTexture(texture);
                drawBoxes(scale);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, previous);
            }
        }
        if (childModels != null) {
            for (Object child : childModels) {
                ((ModelRenderer) child).render(scale);
            }
        }
        GL11.glPopMatrix();
    }

    /**
     * Draw own boxes via a private display list; geometry is static, only the
     * transforms above change per frame.
     * <p>
     * 通过自有 display list 绘制自身盒子；几何静态，仅上方的变换逐帧变化。
     */
    private void drawBoxes(float scale) {
        if (!compiled) {
            displayList = GLAllocation.generateDisplayLists(1);
            GL11.glNewList(displayList, GL11.GL_COMPILE);
            Tessellator tessellator = Tessellator.instance;
            for (Object box : cubeList) {
                ((ModelBox) box).render(tessellator, scale);
            }
            GL11.glEndList();
            compiled = true;
        }
        GL11.glCallList(displayList);
    }
}

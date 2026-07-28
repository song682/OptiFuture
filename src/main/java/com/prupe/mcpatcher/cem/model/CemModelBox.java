package com.prupe.mcpatcher.cem.model;

import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;
import net.minecraft.client.renderer.Tessellator;

import com.prupe.mcpatcher.cem.parse.JpmBox;

/**
 * A model box built from a JPM box definition. Extends the vanilla {@link ModelBox}
 * so it can live in {@code ModelRenderer.cubeList}, but generates its own quads to
 * support features the vanilla box lacks: fractional dimensions, per-axis size
 * increments ("sizesAdd"), individual per-face UV ("uvNorth"...) and V-axis texture
 * mirroring. Vertex layout and box-format UV unwrapping follow the vanilla
 * {@link ModelBox} exactly.
 * <p>
 * 由 JPM 盒子定义构建的模型盒。继承原版 {@link ModelBox} 以便放入
 * {@code ModelRenderer.cubeList}，但自行生成 quad，以支持原版盒子不具备的能力：
 * 小数尺寸、各轴独立的尺寸增量（"sizesAdd"）、逐面 UV（"uvNorth" 等）以及 V 轴纹理
 * 镜像。顶点布局与盒式 UV 展开与原版 {@link ModelBox} 完全一致。
 */
public class CemModelBox extends ModelBox {

    private final TexturedQuad[] quads = new TexturedQuad[6];

    /**
     * @param renderer parent renderer (texture size and U mirror flag) / 父渲染器（提供纹理尺寸与 U 镜像标志）
     * @param box      parsed JPM box; coordinates must already be in vanilla model space
     *                 (axis inversion and translate applied) /
     *                 已解析的 JPM 盒子；coordinates 必须已转换到原版模型空间（轴反转与平移已应用）
     * @param x        box origin / 盒子起点
     * @param mirrorV  mirror texture on the V axis / 纹理 V 轴镜像
     */
    public CemModelBox(ModelRenderer renderer, JpmBox box, float x, float y, float z, float w, float h, float d,
        boolean mirrorV) {
        // The vanilla superclass builds its own (unused) quad list; render() is overridden
        // 原版父类会构建一份（未使用的）quad 列表；render() 已被覆写
        super(renderer, 0, 0, x, y, z, Math.round(w), Math.round(h), Math.round(d), 0.0f);

        float addX = box.sizesAdd != null ? box.sizesAdd[0] : box.sizeAdd;
        float addY = box.sizesAdd != null ? box.sizesAdd[1] : box.sizeAdd;
        float addZ = box.sizesAdd != null ? box.sizesAdd[2] : box.sizeAdd;
        float x1 = x - addX;
        float y1 = y - addY;
        float z1 = z - addZ;
        float x2 = x + w + addX;
        float y2 = y + h + addY;
        float z2 = z + d + addZ;

        // Vertex layout identical to the vanilla ModelBox / 顶点布局与原版 ModelBox 一致
        PositionTextureVertex p0 = new PositionTextureVertex(x1, y1, z1, 0.0f, 0.0f);
        PositionTextureVertex p1 = new PositionTextureVertex(x2, y1, z1, 0.0f, 0.0f);
        PositionTextureVertex p2 = new PositionTextureVertex(x2, y2, z1, 0.0f, 0.0f);
        PositionTextureVertex p3 = new PositionTextureVertex(x1, y2, z1, 0.0f, 0.0f);
        PositionTextureVertex p4 = new PositionTextureVertex(x1, y1, z2, 0.0f, 0.0f);
        PositionTextureVertex p5 = new PositionTextureVertex(x2, y1, z2, 0.0f, 0.0f);
        PositionTextureVertex p6 = new PositionTextureVertex(x2, y2, z2, 0.0f, 0.0f);
        PositionTextureVertex p7 = new PositionTextureVertex(x1, y2, z2, 0.0f, 0.0f);

        float tw = renderer.textureWidth;
        float th = renderer.textureHeight;
        float u0 = box.textureOffset != null ? box.textureOffset[0] : 0.0f;
        float v0 = box.textureOffset != null ? box.textureOffset[1] : 0.0f;

        // Face UV rectangles [u1, v1, u2, v2]: from "uv<Face>" or the box-format unwrap
        // 各面 UV 矩形 [u1, v1, u2, v2]：来自 "uv<面>" 或盒式展开
        float[][] uv = new float[6][];
        if (box.hasFaceUvs()) {
            uv = box.faceUvs;
        } else {
            // Box-format layout, same regions as the vanilla ModelBox constructor
            // 盒式布局，与原版 ModelBox 构造函数的贴图区域一致
            uv[JpmBox.FACE_EAST] = new float[] { u0 + d + w, v0 + d, u0 + d + w + d, v0 + d + h };
            uv[JpmBox.FACE_WEST] = new float[] { u0, v0 + d, u0 + d, v0 + d + h };
            uv[JpmBox.FACE_UP] = new float[] { u0 + d, v0, u0 + d + w, v0 + d };
            uv[JpmBox.FACE_DOWN] = new float[] { u0 + d + w, v0 + d, u0 + d + w + w, v0 };
            uv[JpmBox.FACE_NORTH] = new float[] { u0 + d, v0 + d, u0 + d + w, v0 + d + h };
            uv[JpmBox.FACE_SOUTH] = new float[] { u0 + d + w + d, v0 + d, u0 + d + w + d + w, v0 + d + h };
        }

        // Same vertex winding per face as the vanilla ModelBox
        // 各面顶点环绕顺序与原版 ModelBox 一致
        quads[0] = makeQuad(new PositionTextureVertex[] { p5, p1, p2, p6 }, uv[JpmBox.FACE_EAST], tw, th);
        quads[1] = makeQuad(new PositionTextureVertex[] { p0, p4, p7, p3 }, uv[JpmBox.FACE_WEST], tw, th);
        quads[2] = makeQuad(new PositionTextureVertex[] { p5, p4, p0, p1 }, uv[JpmBox.FACE_UP], tw, th);
        quads[3] = makeQuad(new PositionTextureVertex[] { p2, p3, p7, p6 }, uv[JpmBox.FACE_DOWN], tw, th);
        quads[4] = makeQuad(new PositionTextureVertex[] { p1, p0, p3, p2 }, uv[JpmBox.FACE_NORTH], tw, th);
        quads[5] = makeQuad(new PositionTextureVertex[] { p4, p5, p6, p7 }, uv[JpmBox.FACE_SOUTH], tw, th);

        if (renderer.mirror || mirrorV) {
            for (TexturedQuad quad : quads) {
                if (quad != null) {
                    mirrorUv(quad, renderer.mirror, mirrorV);
                }
            }
        }
    }

    /**
     * Build one face quad; a missing UV rectangle skips the face.
     * <p>
     * 构建一个面的 quad；缺少 UV 矩形时跳过该面。
     */
    private static TexturedQuad makeQuad(PositionTextureVertex[] vertices, float[] uv, float tw, float th) {
        if (uv == null) {
            return null;
        }
        // Vertex UV pattern identical to the vanilla TexturedQuad constructor
        // 顶点 UV 分配方式与原版 TexturedQuad 构造函数一致
        vertices[0] = vertices[0].setTexturePosition(uv[2] / tw, uv[1] / th);
        vertices[1] = vertices[1].setTexturePosition(uv[0] / tw, uv[1] / th);
        vertices[2] = vertices[2].setTexturePosition(uv[0] / tw, uv[3] / th);
        vertices[3] = vertices[3].setTexturePosition(uv[2] / tw, uv[3] / th);
        return new TexturedQuad(vertices);
    }

    /**
     * Mirror the texture by swapping vertex UVs, leaving geometry and normals intact.
     * <p>
     * 通过交换顶点 UV 实现纹理镜像，不改变几何与法线。
     */
    private static void mirrorUv(TexturedQuad quad, boolean mirrorU, boolean mirrorV) {
        PositionTextureVertex[] v = quad.vertexPositions;
        if (mirrorU) {
            swapUv(v, 0, 1);
            swapUv(v, 2, 3);
        }
        if (mirrorV) {
            swapUv(v, 0, 3);
            swapUv(v, 1, 2);
        }
    }

    private static void swapUv(PositionTextureVertex[] v, int a, int b) {
        float u = v[a].texturePositionX;
        float t = v[a].texturePositionY;
        v[a] = v[a].setTexturePosition(v[b].texturePositionX, v[b].texturePositionY);
        v[b] = v[b].setTexturePosition(u, t);
    }

    @Override
    public void render(Tessellator tessellator, float scale) {
        for (TexturedQuad quad : quads) {
            if (quad != null) {
                quad.draw(tessellator, scale);
            }
        }
    }
}

package com.prupe.mcpatcher.cem.parse;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.mal.resource.TexturePackAPI;

/**
 * Tolerant parser for JEM entity models and JPM part models ("cem_model.txt",
 * "cem_part.txt"). JEM/JPM files are not strict JSON (they may contain comments),
 * so parsing goes through Gson's lenient mode. Malformed fields are logged and
 * skipped instead of failing the whole model.
 * <p>
 * JEM 实体模型与 JPM 部件模型（"cem_model.txt"、"cem_part.txt"）的容错解析器。
 * JEM/JPM 并非严格 JSON（可能含注释），因此使用 Gson 的 lenient 模式解析。
 * 畸形字段仅记录日志并跳过，不会导致整个模型失败。
 */
public final class JemParser {

    private static final MCLogger logger = MCLogger.getLogger(MCLogger.Category.CUSTOM_ENTITY_MODELS);

    /** Base folder of CEM resources. / CEM 资源的基础目录。 */
    public static final String CEM_FOLDER = "optifine/cem/";

    private JemParser() {}

    /**
     * Load and parse a ".jem" file. External ".jpm" references and "baseId"
     * inheritance are resolved before returning.
     * <p>
     * 加载并解析一个 ".jem" 文件。返回前会解析外部 ".jpm" 引用并处理 "baseId" 继承。
     *
     * @return parsed model, or null on any error (already logged) / 解析结果，出错时返回 null（已记录日志）
     */
    public static JemModel parseJem(ResourceLocation resource) {
        JsonObject root = readJson(resource);
        if (root == null) {
            return null;
        }
        try {
            JemModel jem = new JemModel(resource);
            jem.texture = resolveTexture(resource, getString(root, "texture"));
            jem.textureSize = getIntArray(root, "textureSize", 2);
            if (root.has("shadowSize")) {
                jem.shadowSize = getFloat(root, "shadowSize", 1.0f);
            }
            JsonArray models = getArray(root, "models");
            if (models != null) {
                for (JsonElement element : models) {
                    if (element.isJsonObject()) {
                        JemModelPart part = parseModelEntry(element.getAsJsonObject(), resource);
                        if (part != null) {
                            jem.models.add(part);
                        }
                    }
                }
            }
            applyInheritance(jem);
            if (jem.models.isEmpty()) {
                logger.warning("%s: no valid model entries", resource);
                return null;
            }
            return jem;
        } catch (RuntimeException e) {
            logger.severe("%s: failed to parse: %s", resource, e);
            return null;
        }
    }

    /** Parse one entry of the "models" list. / 解析 "models" 列表中的一个条目。 */
    private static JemModelPart parseModelEntry(JsonObject json, ResourceLocation baseResource) {
        JemModelPart part = new JemModelPart();
        part.baseId = getString(json, "baseId");
        part.modelFile = getString(json, "model");
        part.id = getString(json, "id");
        part.part = getString(json, "part");
        part.attach = getBoolean(json, "attach", false);
        part.scale = getFloat(json, "scale", 1.0f);

        // External ".jpm" file first, inline fields override it afterwards
        // 先加载外部 ".jpm" 文件，随后用内联字段覆盖
        if (part.modelFile != null) {
            ResourceLocation jpmResource = resolveFile(baseResource, part.modelFile);
            JsonObject jpmJson = jpmResource == null ? null : readJson(jpmResource);
            if (jpmJson != null) {
                parsePartInto(jpmJson, part.model, jpmResource);
            } else {
                logger.warning("%s: cannot load part model '%s'", baseResource, part.modelFile);
            }
        }
        parsePartInto(json, part.model, baseResource);
        part.model.id = part.id;

        JsonArray animations = getArray(json, "animations");
        if (animations != null) {
            for (JsonElement element : animations) {
                if (!element.isJsonObject()) {
                    continue;
                }
                // Gson keeps member order, which the animation engine relies on
                // Gson 保持成员顺序，动画引擎依赖该顺序
                Map<String, String> group = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject()
                    .entrySet()) {
                    if (entry.getValue()
                        .isJsonPrimitive()) {
                        group.put(
                            entry.getKey(),
                            entry.getValue()
                                .getAsString());
                    }
                }
                if (!group.isEmpty()) {
                    part.animations.add(group);
                }
            }
        }
        return part;
    }

    /**
     * Parse JPM part model fields from JSON into an existing part. Only fields present
     * in the JSON are overwritten, which implements the file/inline merge.
     * <p>
     * 将 JSON 中的 JPM 部件字段解析进已有部件。仅覆盖 JSON 中出现的字段，从而实现
     * 外部文件与内联定义的合并。
     */
    private static void parsePartInto(JsonObject json, JpmPart part, ResourceLocation baseResource) {
        String texture = getString(json, "texture");
        if (texture != null) {
            part.texture = resolveTexture(baseResource, texture);
        }
        int[] textureSize = getIntArray(json, "textureSize", 2);
        if (textureSize != null) {
            part.textureSize = textureSize;
        }
        String invertAxis = getString(json, "invertAxis");
        if (invertAxis != null) {
            part.invertAxis = invertAxis.toLowerCase();
        }
        float[] translate = getFloatArray(json, "translate", 3);
        if (translate != null) {
            part.translate = translate;
        }
        float[] rotate = getFloatArray(json, "rotate", 3);
        if (rotate != null) {
            part.rotate = rotate;
        }
        String mirrorTexture = getString(json, "mirrorTexture");
        if (mirrorTexture != null) {
            part.mirrorTexture = mirrorTexture.toLowerCase();
        }
        JsonObject attachments = getObject(json, "attachments");
        if (attachments != null) {
            for (Map.Entry<String, JsonElement> entry : attachments.entrySet()) {
                float[] offset = toFloatArray(entry.getValue(), 3, baseResource, entry.getKey());
                if (offset != null) {
                    part.attachments.put(entry.getKey(), offset);
                }
            }
        }
        JsonArray boxes = getArray(json, "boxes");
        if (boxes != null) {
            part.boxes.clear();
            for (JsonElement element : boxes) {
                if (element.isJsonObject()) {
                    JpmBox box = parseBox(element.getAsJsonObject(), baseResource);
                    if (box != null) {
                        part.boxes.add(box);
                    }
                }
            }
        }
        JsonArray sprites = getArray(json, "sprites");
        if (sprites != null) {
            part.sprites.clear();
            for (JsonElement element : sprites) {
                if (element.isJsonObject()) {
                    JpmBox sprite = parseBox(element.getAsJsonObject(), baseResource);
                    if (sprite != null) {
                        part.sprites.add(sprite);
                    }
                }
            }
        }
        JsonObject submodel = getObject(json, "submodel");
        JsonArray submodels = getArray(json, "submodels");
        if (submodel != null || submodels != null) {
            part.submodels.clear();
            if (submodel != null) {
                part.submodels.add(parseSubmodel(submodel, baseResource));
            }
            if (submodels != null) {
                for (JsonElement element : submodels) {
                    if (element.isJsonObject()) {
                        part.submodels.add(parseSubmodel(element.getAsJsonObject(), baseResource));
                    }
                }
            }
        }
    }

    /** Parse a nested submodel, keeping its "id" for animation lookup. / 解析嵌套子模型，保留其 "id" 供动画查找。 */
    private static JpmPart parseSubmodel(JsonObject json, ResourceLocation baseResource) {
        JpmPart submodel = new JpmPart();
        parsePartInto(json, submodel, baseResource);
        submodel.id = getString(json, "id");
        return submodel;
    }

    /** Parse a box or sprite. / 解析一个盒子或 sprite。 */
    private static JpmBox parseBox(JsonObject json, ResourceLocation baseResource) {
        JpmBox box = new JpmBox();
        box.coordinates = getFloatArray(json, "coordinates", 6);
        if (box.coordinates == null) {
            logger.warning("%s: box without coordinates ignored", baseResource);
            return null;
        }
        box.textureOffset = getFloatArray(json, "textureOffset", 2);
        float[][] faceUvs = new float[6][];
        boolean hasFace = false;
        hasFace |= (faceUvs[JpmBox.FACE_DOWN] = firstFloatArray(json, 4, "uvDown")) != null;
        hasFace |= (faceUvs[JpmBox.FACE_UP] = firstFloatArray(json, 4, "uvUp")) != null;
        hasFace |= (faceUvs[JpmBox.FACE_NORTH] = firstFloatArray(json, 4, "uvNorth", "uvFront")) != null;
        hasFace |= (faceUvs[JpmBox.FACE_SOUTH] = firstFloatArray(json, 4, "uvSouth", "uvBack")) != null;
        hasFace |= (faceUvs[JpmBox.FACE_WEST] = firstFloatArray(json, 4, "uvWest", "uvLeft")) != null;
        hasFace |= (faceUvs[JpmBox.FACE_EAST] = firstFloatArray(json, 4, "uvEast", "uvRight")) != null;
        if (hasFace) {
            box.faceUvs = faceUvs;
        }
        box.sizeAdd = getFloat(json, "sizeAdd", 0.0f);
        box.sizesAdd = getFloatArray(json, "sizesAdd", 3);
        return box;
    }

    /**
     * Resolve "baseId" parent references within one JEM file, see "cem_model.txt" L313.
     * <p>
     * 解析同一 JEM 文件内的 "baseId" 父引用，见 "cem_model.txt" L313。
     */
    private static void applyInheritance(JemModel jem) {
        Map<String, JemModelPart> byId = new LinkedHashMap<>();
        for (JemModelPart part : jem.models) {
            if (part.id != null && !byId.containsKey(part.id)) {
                byId.put(part.id, part);
            }
        }
        for (JemModelPart part : jem.models) {
            if (part.baseId != null) {
                JemModelPart parent = byId.get(part.baseId);
                if (parent != null && parent != part) {
                    part.inheritFrom(parent);
                } else {
                    logger.warning("%s: unknown baseId '%s'", jem.source, part.baseId);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // JSON field helpers (lenient: wrong types are ignored)
    // JSON 字段辅助（容错：类型不符时忽略）
    // ------------------------------------------------------------------

    private static String getString(JsonObject json, String name) {
        JsonElement element = json.get(name);
        if (element != null && element.isJsonPrimitive()) {
            return element.getAsString();
        }
        return null;
    }

    private static boolean getBoolean(JsonObject json, String name, boolean defaultValue) {
        JsonElement element = json.get(name);
        if (element != null && element.isJsonPrimitive()) {
            try {
                return element.getAsBoolean();
            } catch (RuntimeException e) {}
        }
        return defaultValue;
    }

    private static float getFloat(JsonObject json, String name, float defaultValue) {
        JsonElement element = json.get(name);
        if (element != null && element.isJsonPrimitive()) {
            try {
                return element.getAsFloat();
            } catch (RuntimeException e) {}
        }
        return defaultValue;
    }

    private static JsonArray getArray(JsonObject json, String name) {
        JsonElement element = json.get(name);
        if (element != null && element.isJsonArray()) {
            return element.getAsJsonArray();
        }
        return null;
    }

    private static JsonObject getObject(JsonObject json, String name) {
        JsonElement element = json.get(name);
        if (element != null && element.isJsonObject()) {
            return element.getAsJsonObject();
        }
        return null;
    }

    private static float[] getFloatArray(JsonObject json, String name, int length) {
        JsonElement element = json.get(name);
        return element == null ? null : toFloatArray(element, length, null, name);
    }

    private static int[] getIntArray(JsonObject json, String name, int length) {
        float[] floats = getFloatArray(json, name, length);
        if (floats == null) {
            return null;
        }
        int[] ints = new int[floats.length];
        for (int i = 0; i < floats.length; i++) {
            ints[i] = (int) floats[i];
        }
        return ints;
    }

    /** Return the first present array among aliased names. / 返回别名字段中第一个存在的数组。 */
    private static float[] firstFloatArray(JsonObject json, int length, String... names) {
        for (String name : names) {
            float[] value = getFloatArray(json, name, length);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static float[] toFloatArray(JsonElement element, int length, ResourceLocation source, String name) {
        if (element == null || !element.isJsonArray()) {
            return null;
        }
        JsonArray array = element.getAsJsonArray();
        if (array.size() != length) {
            logger.warning("%s: '%s' needs %d elements, found %d", source, name, length, array.size());
            return null;
        }
        float[] result = new float[length];
        for (int i = 0; i < length; i++) {
            try {
                result[i] = array.get(i)
                    .getAsFloat();
            } catch (RuntimeException e) {
                logger.warning("%s: '%s' element %d is not a number", source, name, i);
                return null;
            }
        }
        return result;
    }

    // ------------------------------------------------------------------
    // Resource helpers / 资源辅助
    // ------------------------------------------------------------------

    /**
     * Read a JEM/JPM file as a lenient JSON object (comments allowed).
     * <p>
     * 以 lenient JSON（允许注释）读取 JEM/JPM 文件。
     */
    static JsonObject readJson(ResourceLocation resource) {
        InputStream input = TexturePackAPI.getInputStream(resource);
        if (input == null) {
            return null;
        }
        try (JsonReader reader = new JsonReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            reader.setLenient(true);
            JsonElement element = new JsonParser().parse(reader);
            if (element != null && element.isJsonObject()) {
                return element.getAsJsonObject();
            }
            logger.severe("%s: not a JSON object", resource);
        } catch (IOException | RuntimeException e) {
            logger.severe("%s: invalid JSON: %s", resource, e);
        } finally {
            MCPatcherUtils.close(input);
        }
        return null;
    }

    /**
     * Resolve a texture path using the CEM rules of "cem_part.txt" L15-22 and append
     * the optional ".png" suffix. Unlike other MCPatcher features, "~/" is relative
     * to "assets/(namespace)/optifine/".
     * <p>
     * 按 "cem_part.txt" L15-22 的 CEM 规则解析纹理路径并补全可选的 ".png" 后缀。
     * 与其他 MCPatcher 特性不同，"~/" 相对于 "assets/(namespace)/optifine/"。
     */
    static ResourceLocation resolveTexture(ResourceLocation baseResource, String path) {
        if (MCPatcherUtils.isNullOrEmpty(path)) {
            return null;
        }
        path = path.trim();
        if (!path.endsWith(".png")) {
            path += ".png";
        }
        return resolveFile(baseResource, path);
    }

    /**
     * Resolve a file reference (texture or ".jpm") relative to the given base file.
     * <p>
     * 相对于给定基准文件解析文件引用（纹理或 ".jpm"）。
     */
    static ResourceLocation resolveFile(ResourceLocation baseResource, String path) {
        if (MCPatcherUtils.isNullOrEmpty(path)) {
            return null;
        }
        path = path.trim();
        if (path.startsWith("~/")) {
            // "~/folder/file" -> assets/(namespace)/optifine/folder/file
            return new ResourceLocation(baseResource.getResourceDomain(), "optifine/" + path.substring(2));
        }
        return TexturePackAPI.parseResourceLocation(baseResource, path);
    }
}

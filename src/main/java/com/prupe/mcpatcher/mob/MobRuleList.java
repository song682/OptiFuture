package com.prupe.mcpatcher.mob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.mal.resource.PropertiesFile;
import com.prupe.mcpatcher.mal.resource.TexturePackAPI;

/**
 * Manages the randomized texture variants and rule list of a single entity
 * texture. Alternate textures are searched for in three directories, in
 * priority order:
 * <ul>
 * <li>{@code mcpatcher/mob/} - legacy MCPatcher Random Mobs layout, mirrors
 * {@code textures/entity/}</li>
 * <li>{@code optifine/mob/} - legacy OptiFine Random Mobs layout, mirrors
 * {@code textures/entity/}</li>
 * <li>{@code optifine/random/} - modern OptiFine Random Entities layout,
 * mirrors the whole {@code textures/} tree (entities, paintings, ...)</li>
 * </ul>
 * The first directory containing at least one alternate texture wins.
 * <p>
 * 管理单个实体纹理的随机化变体与规则列表。替代纹理按以下优先级在三个
 * 目录中检索：
 * <ul>
 * <li>{@code mcpatcher/mob/} —— 旧版 MCPatcher Random Mobs 布局，镜像
 * {@code textures/entity/}</li>
 * <li>{@code optifine/mob/} —— 旧版 OptiFine Random Mobs 布局，镜像
 * {@code textures/entity/}</li>
 * <li>{@code optifine/random/} —— 现代 OptiFine Random Entities 布局，
 * 镜像整个 {@code textures/} 目录树（实体、画等）</li>
 * </ul>
 * 第一个包含至少一张替代纹理的目录生效。
 */
class MobRuleList {

    private static final MCLogger logger = MCLogger.getLogger(MCLogger.Category.RANDOM_MOBS);

    public static final String ALTERNATIVES_REGEX = "_(eyes|overlay|tame|angry|collar|fur|invulnerable|shooting)\\.properties$";

    private static final String TEXTURES_PREFIX = "textures/";
    private static final String ENTITY_PREFIX = "textures/entity/";
    private static final String RANDOM_SUBDIR = "random/";
    private static final String MOB_SUBDIR = "mob/";

    private static final Map<ResourceLocation, MobRuleList> allRules = new HashMap<>();

    private final ResourceLocation baseSkin;
    private final List<ResourceLocation> allSkins;
    private final int skinCount;
    private final List<MobRuleEntry> entries;

    private MobRuleList(ResourceLocation baseSkin) {
        this.baseSkin = baseSkin;
        ResourceLocation newSkin = resolveAlternatives(baseSkin);
        allSkins = new ArrayList<>();
        allSkins.add(baseSkin);
        for (int i = 2;; i++) {
            ResourceLocation skin = variantResource(newSkin, i);
            if (!TexturePackAPI.hasResource(skin)) {
                break;
            }
            allSkins.add(skin);
        }
        skinCount = allSkins.size();
        if (skinCount <= 1) {
            entries = null;
            return;
        }
        logger.fine("found %d variations for %s", skinCount, baseSkin);

        ResourceLocation filename = TexturePackAPI.transformResourceLocation(newSkin, ".png", ".properties");
        ResourceLocation altFilename = new ResourceLocation(
            newSkin.getResourceDomain(),
            filename.getResourcePath()
                .replaceFirst(ALTERNATIVES_REGEX, ".properties"));
        PropertiesFile properties = PropertiesFile.get(logger, filename);
        if (properties == null && !filename.equals(altFilename)) {
            properties = PropertiesFile.get(logger, altFilename);
            if (properties != null) {
                logger.fine("using %s for %s", altFilename, baseSkin);
            }
        }
        ArrayList<MobRuleEntry> tmpEntries = new ArrayList<>();
        if (properties != null) {
            for (int i = 0;; i++) {
                MobRuleEntry entry = MobRuleEntry.load(properties, i, skinCount);
                if (entry == null) {
                    if (i > 0) {
                        break;
                    }
                } else {
                    logger.fine("  %s", entry.toString());
                    tmpEntries.add(entry);
                }
            }
        }
        entries = tmpEntries.isEmpty() ? null : tmpEntries;
    }

    /**
     * Picks the texture to render for the given entity. Without a properties
     * file every available variant is cycled through by the random seed; with
     * one, the first matching rule determines the choice.
     * 为给定实体挑选要渲染的纹理。没有属性文件时按随机种子循环使用全部
     * 变体；有属性文件时由第一条匹配的规则决定选择结果。
     */
    ResourceLocation getSkin(Entity entity, MobRandomizer.ExtraInfo info) {
        long seed = info == null ? MobRandomizer.ExtraInfo.getSkinId(entity.getEntityId()) : info.getSkin();
        if (entries == null) {
            int index = (int) (seed % skinCount);
            if (index < 0) {
                index += skinCount;
            }
            return allSkins.get(index);
        } else {
            for (MobRuleEntry entry : entries) {
                if (entry.match(entity, info)) {
                    int index = entry.weightedIndex.choose(entry.adjustSeed(entity, seed));
                    return allSkins.get(entry.skins[index]);
                }
            }
        }
        return baseSkin;
    }

    static MobRuleList get(ResourceLocation texture) {
        MobRuleList list = allRules.get(texture);
        if (list == null) {
            list = new MobRuleList(texture);
            allRules.put(texture, list);
        }
        return list;
    }

    static void clear() {
        allRules.clear();
    }

    /**
     * Resolves which directory supplies the alternate textures of a vanilla
     * texture. The mcpatcher/mob directory is checked first (project-wide
     * mcpatcher-before-optifine convention), then optifine/mob, then the
     * modern optifine/random layout. Textures already inside the mcpatcher/
     * or optifine/ namespace directory (e.g. mob overlays) use their own
     * location. When no directory provides alternates the first candidate is
     * returned, which simply leaves the vanilla texture alone.
     * 解析由哪个目录提供原版纹理的替代纹理。按项目内 mcpatcher 优先于
     * optifine 的惯例依次检查 mcpatcher/mob、optifine/mob，最后是现代的
     * optifine/random 布局。已位于 mcpatcher/ 或 optifine/ 命名空间目录内
     * 的纹理（如生物覆盖层）直接使用其自身位置。所有目录均无替代纹理时
     * 返回第一个候选位置，其效果等同于仅保留原版纹理。
     */
    private static ResourceLocation resolveAlternatives(ResourceLocation baseSkin) {
        String domain = baseSkin.getResourceDomain();
        String path = baseSkin.getResourcePath();
        List<ResourceLocation> candidates = new ArrayList<>();
        if (path.startsWith(ENTITY_PREFIX)) {
            String relative = path.substring(ENTITY_PREFIX.length());
            candidates.add(new ResourceLocation(domain, TexturePackAPI.MCPATCHER_SUBDIR + MOB_SUBDIR + relative));
            candidates.add(new ResourceLocation(domain, TexturePackAPI.OPTIFINE_SUBDIR + MOB_SUBDIR + relative));
        }
        if (path.startsWith(TEXTURES_PREFIX)) {
            String relative = path.substring(TEXTURES_PREFIX.length());
            candidates.add(new ResourceLocation(domain, TexturePackAPI.OPTIFINE_SUBDIR + RANDOM_SUBDIR + relative));
        }
        if (path.startsWith(TexturePackAPI.MCPATCHER_SUBDIR) || path.startsWith(TexturePackAPI.OPTIFINE_SUBDIR)) {
            candidates.add(baseSkin);
        }
        if (candidates.isEmpty()) {
            return baseSkin;
        }
        for (ResourceLocation candidate : candidates) {
            if (TexturePackAPI.hasResource(variantResource(candidate, 2))) {
                return candidate;
            }
        }
        return candidates.get(0);
    }

    /**
     * Builds the resource location of alternate texture number {@code index}
     * (index 1 is the vanilla texture itself). Base names ending in a digit
     * use "." as the separator before the index, all others append the index
     * directly, matching the Random Entities documentation.
     * 构造编号为 {@code index} 的替代纹理的资源位置（索引 1 即原版纹理
     * 本身）。以数字结尾的基础名称在索引前使用 “.” 分隔符，其余名称直接
     * 追加索引，与 Random Entities 文档一致。
     */
    private static ResourceLocation variantResource(ResourceLocation base, int index) {
        String path = base.getResourcePath();
        String stem = path.replaceFirst("\\.png$", "");
        char last = stem.charAt(stem.length() - 1);
        String separator = last >= '0' && last <= '9' ? "." : "";
        return new ResourceLocation(base.getResourceDomain(), stem + separator + index + ".png");
    }
}

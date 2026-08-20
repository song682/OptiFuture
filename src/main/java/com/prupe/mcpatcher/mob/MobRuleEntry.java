package com.prupe.mcpatcher.mob;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.mal.biome.BiomeAPI;
import com.prupe.mcpatcher.mal.block.BlockAPI;
import com.prupe.mcpatcher.mal.block.BlockStateMatcher;
import com.prupe.mcpatcher.mal.nbt.NBTRule;
import com.prupe.mcpatcher.mal.resource.PropertiesFile;
import com.prupe.mcpatcher.mal.util.WeightedIndex;

/**
 * A single numbered rule of a Random Entities properties file. Each rule lists
 * the texture indices to pick from and zero or more conditions under which the
 * rule applies; the first rule whose conditions all match wins. Besides the
 * legacy Random Mobs conditions (skins, weights, biomes, heights) the full
 * modern OptiFine Random Entities condition set is supported: name, professions,
 * colors, baby, health, moonPhase, dayTime, weather, sizes, nbt, blocks,
 * seedOffset and seedSource.
 * <p>
 * Random Entities 属性文件中的单条编号规则。每条规则列出可供选择的纹理
 * 索引及若干适用条件；第一条所有条件均满足的规则生效。除旧版 Random Mobs
 * 条件（skins、weights、biomes、heights）外，完整支持现代 OptiFine Random
 * Entities 条件集：name、professions、colors、baby、health、moonPhase、
 * dayTime、weather、sizes、nbt、blocks、seedOffset 与 seedSource。
 */
class MobRuleEntry {

    /** Modern profession names mapped onto the 1.7.10 villager profession ids (0-4). / 现代职业名到 1.7.10 村民职业 id（0-4）的映射。 */
    private static final Map<String, Integer> PROFESSION_IDS = new HashMap<>();

    /** Modern biome names mapped onto their 1.7.10 biome names. / 现代生物群系名到 1.7.10 群系名的映射。 */
    private static final String[][] BIOME_ALIASES = {
        { "windswept_hills", "Extreme Hills" },
        { "windswept_gravelly_hills", "Extreme Hills M" },
        { "windswept_forest", "Extreme Hills+" },
        { "windswept_savanna", "Savanna M" },
        { "shattered_savanna", "Savanna M" },
        { "shattered_savanna_plateau", "Savanna Plateau M" },
        { "snowy_plains", "Ice Plains" },
        { "ice_flats", "Ice Plains" },
        { "ice_spikes", "Ice Plains Spikes" },
        { "snowy_taiga", "Cold Taiga" },
        { "snowy_taiga_hills", "Cold Taiga Hills" },
        { "snowy_beach", "Cold Beach" },
        { "stone_shore", "Stone Beach" },
        { "stony_shore", "Stone Beach" },
        { "dark_forest", "Roofed Forest" },
        { "swamp", "Swampland" },
        { "nether_wastes", "Hell" },
        { "the_end", "Sky" },
        { "sparse_jungle", "JungleEdge" },
        { "old_growth_pine_taiga", "Mega Taiga" },
        { "giant_tree_taiga", "Mega Taiga" },
        { "old_growth_pine_taiga_hills", "Mega Taiga Hills" },
        { "giant_tree_taiga_hills", "Mega Taiga Hills" },
        { "old_growth_spruce_taiga", "Mega Spruce Taiga" },
        { "giant_spruce_taiga", "Mega Spruce Taiga" },
        { "old_growth_birch_forest", "Birch Forest M" },
        { "tall_birch_forest", "Birch Forest M" },
        { "tall_birch_hills", "Birch Forest Hills M" },
        { "sunflower_plains", "Sunflower Plains" },
        { "flower_forest", "Flower Forest" },
        { "desert_lakes", "Desert M" },
        { "badlands", "Mesa" },
        { "eroded_badlands", "Mesa (Bryce)" },
        { "wooded_badlands", "Mesa Plateau F" },
        { "modified_wooded_badlands", "Mesa Plateau F M" },
        { "badlands_plateau", "Mesa Plateau" },
        { "modified_badlands_plateau", "Mesa Plateau M" },
    };

    private static final Map<String, String> BIOME_ALIAS_MAP = new HashMap<>();

    /** The 16 vanilla dye colors in metadata order, used for collars and fleece. / 按元数据顺序排列的 16 种原版染料颜色，用于项圈与羊毛。 */
    private static final String[] DYE_COLORS = {
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black" };

    static {
        for (String[] pair : BIOME_ALIASES) {
            BIOME_ALIAS_MAP.put(normalizeBiomeName(pair[0]), pair[1]);
        }
        // 1.7.10 villagers know five professions; modern names are folded onto them.
        // 1.7.10 村民只有五种职业，现代职业名向其归并。
        PROFESSION_IDS.put("farmer", 0);
        PROFESSION_IDS.put("fisherman", 0);
        PROFESSION_IDS.put("shepherd", 0);
        PROFESSION_IDS.put("fletcher", 0);
        PROFESSION_IDS.put("librarian", 1);
        PROFESSION_IDS.put("cartographer", 1);
        PROFESSION_IDS.put("cleric", 2);
        PROFESSION_IDS.put("priest", 2);
        PROFESSION_IDS.put("armorer", 3);
        PROFESSION_IDS.put("weaponsmith", 3);
        PROFESSION_IDS.put("toolsmith", 3);
        PROFESSION_IDS.put("blacksmith", 3);
        PROFESSION_IDS.put("mason", 3);
        PROFESSION_IDS.put("butcher", 4);
        PROFESSION_IDS.put("leatherworker", 4);
        // No 1.7.10 counterpart; parsed for compatibility but never matches naturally.
        // 1.7.10 无对应职业；为兼容性而解析，但正常情况下永不匹配。
        PROFESSION_IDS.put("none", -1);
        PROFESSION_IDS.put("nitwit", -1);
    }

    final int[] skins;
    final WeightedIndex weightedIndex;
    final int seedOffset;
    final boolean seedFromVehicle;

    private final BitSet biomes;
    private final BitSet height;
    private final NameMatcher name;
    private final int[] professions;
    private final BitSet colors;
    private final Boolean baby;
    private final HealthRange[] health;
    private final BitSet moonPhases;
    private final BitSet dayTimes;
    private final String[] weather;
    private final BitSet sizes;
    private final List<NBTRule> nbtRules;
    private final List<BlockStateMatcher> blocks;

    private MobRuleEntry(
        int[] skins,
        WeightedIndex weightedIndex,
        BitSet biomes,
        BitSet height,
        NameMatcher name,
        int[] professions,
        BitSet colors,
        Boolean baby,
        HealthRange[] health,
        BitSet moonPhases,
        BitSet dayTimes,
        String[] weather,
        BitSet sizes,
        List<NBTRule> nbtRules,
        List<BlockStateMatcher> blocks,
        int seedOffset,
        boolean seedFromVehicle) {
        this.skins = skins;
        this.weightedIndex = weightedIndex;
        this.biomes = biomes;
        this.height = height;
        this.name = name;
        this.professions = professions;
        this.colors = colors;
        this.baby = baby;
        this.health = health;
        this.moonPhases = moonPhases;
        this.dayTimes = dayTimes;
        this.weather = weather;
        this.sizes = sizes;
        this.nbtRules = nbtRules;
        this.blocks = blocks;
        this.seedOffset = seedOffset;
        this.seedFromVehicle = seedFromVehicle;
    }

    /**
     * Loads rule number {@code index} from a properties file. Returns null when
     * the rule does not exist (no textures/skins property).
     * 从属性文件加载编号为 {@code index} 的规则。规则不存在（无
     * textures/skins 属性）时返回 null。
     *
     * @param properties properties file / 属性文件
     * @param index      rule number / 规则编号
     * @param limit      number of available textures incl. the default / 含默认纹理在内的可用纹理数
     * @return parsed rule or null / 解析出的规则，或 null
     */
    static MobRuleEntry load(PropertiesFile properties, int index, int limit) {
        // "textures.<n>" is the modern Random Entities name; "skins.<n>" is the
        // legacy Random Mobs alias. Texture index 1 is the vanilla default texture.
        // “textures.<n>”是现代 Random Entities 的属性名，“skins.<n>”为旧版
        // Random Mobs 的别名。纹理索引 1 即原版默认纹理。
        String textureList = properties.getString("textures." + index, "");
        if (textureList.isEmpty()) {
            textureList = properties.getString("skins." + index, "");
        }
        textureList = textureList.toLowerCase();
        int[] skins;
        if (textureList.equals("*") || textureList.equals("all") || textureList.equals("any")) {
            skins = new int[limit];
            for (int i = 0; i < skins.length; i++) {
                skins[i] = i;
            }
        } else {
            skins = MCPatcherUtils.parseIntegerList(textureList, 1, limit);
            if (skins.length <= 0) {
                return null;
            }
            for (int i = 0; i < skins.length; i++) {
                skins[i]--;
            }
        }

        WeightedIndex chooser = WeightedIndex.create(skins.length, properties.getString("weights." + index, ""));
        if (chooser == null) {
            return null;
        }

        BitSet biomes = parseBiomeSet(properties, index);
        BitSet height = BiomeAPI.getHeightListProperty(properties, "." + index);
        NameMatcher name = NameMatcher.parse(properties, properties.getString("name." + index, ""));
        int[] professions = parseProfessions(properties, index);
        BitSet colors = parseColors(properties, index);
        String babyString = properties.getString("baby." + index, "");
        Boolean baby = babyString.isEmpty() ? null : Boolean.parseBoolean(babyString.toLowerCase());
        HealthRange[] health = parseHealth(properties, properties.getString("health." + index, ""));
        BitSet moonPhases = parseBitSet(properties.getString("moonPhase." + index, ""), 0, 7);
        BitSet dayTimes = parseBitSet(properties.getString("dayTime." + index, ""), 0, 24000);
        String[] weather = parseWeather(properties, properties.getString("weather." + index, ""));
        BitSet sizes = parseBitSet(properties.getString("sizes." + index, ""), 0, 255);
        List<NBTRule> nbtRules = parseNbtRules(properties, index);
        List<BlockStateMatcher> blocks = parseBlocks(properties, index);
        int seedOffset = properties.getInt("seedOffset." + index, 0);
        boolean seedFromVehicle = "vehicle".equalsIgnoreCase(properties.getString("seedSource." + index, ""));

        return new MobRuleEntry(
            skins,
            chooser,
            biomes,
            height,
            name,
            professions,
            colors,
            baby,
            health,
            moonPhases,
            dayTimes,
            weather,
            sizes,
            nbtRules,
            blocks,
            seedOffset,
            seedFromVehicle);
    }

    /**
     * Checks every condition of this rule against the given entity. Biome and
     * height use the coordinates captured when the entity spawned or was first
     * seen; all other conditions are evaluated against the entity's live state.
     * 针对给定实体检查本规则的全部条件。群系与高度使用实体生成或首次被
     * 看见时记录的坐标，其余条件均按实体的实时状态求值。
     */
    boolean match(Entity entity, MobRandomizer.ExtraInfo info) {
        if (biomes != null) {
            Integer biome = info == null ? null : info.getBiome();
            if (biome == null || !biomes.get(biome)) {
                return false;
            }
        }
        if (height != null) {
            int y = info == null ? 0 : info.getOrigY();
            if (y < 0) {
                y = 0;
            }
            if (!height.get(y)) {
                return false;
            }
        }
        if (name != null) {
            String mobName = "";
            if (entity instanceof EntityLiving && ((EntityLiving) entity).hasCustomNameTag()) {
                mobName = ((EntityLiving) entity).getCustomNameTag();
            }
            if (!name.matches(mobName)) {
                return false;
            }
        }
        if (professions != null) {
            if (!(entity instanceof EntityVillager)) {
                return false;
            }
            int profession = ((EntityVillager) entity).getProfession();
            boolean found = false;
            for (int p : professions) {
                if (p == profession) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        if (colors != null) {
            int color;
            if (entity instanceof EntityWolf) {
                color = ((EntityWolf) entity).getCollarColor();
            } else if (entity instanceof EntitySheep) {
                color = ((EntitySheep) entity).getFleeceColor();
            } else {
                return false;
            }
            if (color < 0 || !colors.get(color)) {
                return false;
            }
        }
        if (baby != null) {
            boolean isChild = entity instanceof EntityAgeable && ((EntityAgeable) entity).isChild();
            if (isChild != baby) {
                return false;
            }
        }
        if (health != null) {
            if (!(entity instanceof EntityLivingBase) || !matchHealth((EntityLivingBase) entity)) {
                return false;
            }
        }
        World world = entity.worldObj;
        if (moonPhases != null || dayTimes != null || weather != null) {
            if (world == null) {
                return false;
            }
            if (moonPhases != null && !moonPhases.get(world.getMoonPhase())) {
                return false;
            }
            if (dayTimes != null && !dayTimes.get((int) (world.getWorldTime() % 24000L))) {
                return false;
            }
            if (weather != null && !matchWeather(world)) {
                return false;
            }
        }
        if (sizes != null) {
            if (!(entity instanceof EntitySlime) || !sizes.get(((EntitySlime) entity).getSlimeSize())) {
                return false;
            }
        }
        if (nbtRules != null) {
            NBTTagCompound nbt = info == null ? null : info.getNbt();
            if (nbt == null) {
                return false;
            }
            for (NBTRule rule : nbtRules) {
                if (!rule.match(nbt)) {
                    return false;
                }
            }
        }
        if (blocks != null) {
            if (world == null || !matchBlocks(entity, world)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Applies this rule's seedSource and seedOffset to the base random seed.
     * 将本规则的 seedSource 与 seedOffset 应用于基础随机种子。
     */
    long adjustSeed(Entity entity, long seed) {
        if (seedFromVehicle && entity != null && entity.ridingEntity != null) {
            seed = MobRandomizer.ExtraInfo.getSkinId(entity.ridingEntity.getEntityId());
        }
        return seed + seedOffset;
    }

    private boolean matchHealth(EntityLivingBase entity) {
        float maxHealth = entity.getMaxHealth();
        for (HealthRange range : health) {
            float value = range.percent ? (maxHealth <= 0.0f ? 0.0f : entity.getHealth() * 100.0f / maxHealth)
                : entity.getHealth();
            if (value >= range.min && value <= range.max) {
                return true;
            }
        }
        return false;
    }

    private boolean matchWeather(World world) {
        boolean raining = world.isRaining();
        boolean thundering = world.isThundering();
        for (String w : weather) {
            if ("clear".equals(w) && !raining) {
                return true;
            }
            if ("rain".equals(w) && raining) {
                return true;
            }
            if ("thunder".equals(w) && thundering) {
                return true;
            }
        }
        return false;
    }

    private boolean matchBlocks(Entity entity, World world) {
        int x = MathHelper.floor_double(entity.posX);
        int y = MathHelper.floor_double(entity.posY);
        int z = MathHelper.floor_double(entity.posZ);
        for (BlockStateMatcher matcher : blocks) {
            // The entity may stand on the block below its feet or inside a
            // replaceable block such as water, so both layers are checked.
            // 实体可能站立在脚下方块之上，也可能身处水等可替换方块内，
            // 因此同时检查这两层。
            if (matcher.match(world, x, y, z) || matcher.match(world, x, y - 1, z)) {
                return true;
            }
        }
        return false;
    }

    private static BitSet parseBiomeSet(PropertiesFile properties, int index) {
        String list = properties.getString("biomes." + index, "");
        if (MCPatcherUtils.isNullOrEmpty(list)) {
            return null;
        }
        BitSet bits = new BitSet();
        for (String token : list.split(list.contains(",") ? "\\s*,\\s*" : "\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            BiomeGenBase biome = findBiomeModern(token);
            if (biome == null) {
                properties.warning("unknown biome %s", token);
            } else {
                bits.set(biome.biomeID);
            }
        }
        return bits;
    }

    /**
     * Finds a biome by modern or 1.7.10 name. Modern snake_case names are first
     * compared against the 1.7.10 names in normalized form, then looked up in
     * the alias table of renamed biomes.
     * 按现代或 1.7.10 名称查找群系。现代 snake_case 名称先与归一化后的
     * 1.7.10 名称比较，再查询已更名群系的别名表。
     */
    private static BiomeGenBase findBiomeModern(String name) {
        String lookup = name;
        if (lookup.startsWith("minecraft:")) {
            lookup = lookup.substring("minecraft:".length());
        }
        BiomeGenBase biome = BiomeAPI.findBiomeByName(lookup);
        if (biome != null) {
            return biome;
        }
        String normalized = normalizeBiomeName(lookup);
        for (BiomeGenBase candidate : BiomeGenBase.getBiomeGenArray()) {
            if (candidate != null && candidate.biomeName != null
                && normalizeBiomeName(candidate.biomeName).equals(normalized)) {
                return candidate;
            }
        }
        String alias = BIOME_ALIAS_MAP.get(normalized);
        return alias == null ? null : BiomeAPI.findBiomeByName(alias);
    }

    private static String normalizeBiomeName(String name) {
        return name.replace("_", "")
            .replace(" ", "")
            .toLowerCase();
    }

    private static int[] parseProfessions(PropertiesFile properties, int index) {
        String list = properties.getString("professions." + index, "");
        if (MCPatcherUtils.isNullOrEmpty(list)) {
            return null;
        }
        List<Integer> tmp = new ArrayList<>();
        for (String token : list.split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            String name = token;
            int colon = name.indexOf(':');
            String levels = null;
            if (colon >= 0) {
                levels = name.substring(colon + 1);
                name = name.substring(0, colon);
            }
            if (name.startsWith("minecraft:")) {
                name = name.substring("minecraft:".length());
            }
            Integer id = PROFESSION_IDS.get(name.toLowerCase());
            if (id == null) {
                try {
                    id = Integer.parseInt(name);
                } catch (NumberFormatException e) {
                    properties.warning("unknown profession %s", token);
                    continue;
                }
            }
            if (!MCPatcherUtils.isNullOrEmpty(levels)) {
                // 1.7.10 villagers have no career levels, so level filters are
                // accepted but ignored.
                // 1.7.10 村民没有职业等级，因此等级过滤会被接受但忽略。
                properties.finer("ignoring level list %s of profession %s (no levels in 1.7.10)", levels, token);
            }
            tmp.add(id);
        }
        if (tmp.isEmpty()) {
            return null;
        }
        int[] result = new int[tmp.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = tmp.get(i);
        }
        return result;
    }

    private static BitSet parseColors(PropertiesFile properties, int index) {
        String list = properties.getString("colors." + index, "");
        if (list.isEmpty()) {
            // Legacy Random Mobs property name. / 旧版 Random Mobs 属性名。
            list = properties.getString("collarColors." + index, "");
        }
        if (MCPatcherUtils.isNullOrEmpty(list)) {
            return null;
        }
        BitSet bits = new BitSet(DYE_COLORS.length);
        for (String token : list.split("\\s+")) {
            token = token.toLowerCase();
            if (token.isEmpty()) {
                continue;
            }
            int color = -1;
            for (int i = 0; i < DYE_COLORS.length; i++) {
                if (DYE_COLORS[i].equals(token)) {
                    color = i;
                    break;
                }
            }
            if (color < 0 && token.matches("\\d+")) {
                int value = Integer.parseInt(token);
                if (value >= 0 && value < DYE_COLORS.length) {
                    color = value;
                }
            }
            if (color < 0) {
                properties.warning("unknown color %s", token);
            } else {
                bits.set(color);
            }
        }
        return bits;
    }

    private static HealthRange[] parseHealth(PropertiesFile properties, String list) {
        if (MCPatcherUtils.isNullOrEmpty(list)) {
            return null;
        }
        List<HealthRange> ranges = new ArrayList<>();
        for (String token : list.split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            boolean percent = token.indexOf('%') >= 0;
            token = token.replace("%", "");
            String[] parts = token.split("-", 2);
            try {
                float min = Float.parseFloat(parts[0]);
                float max = parts.length > 1 && !parts[1].isEmpty() ? Float.parseFloat(parts[1]) : min;
                ranges.add(new HealthRange(min, max, percent));
            } catch (NumberFormatException e) {
                properties.warning("invalid health value %s", token);
            }
        }
        return ranges.isEmpty() ? null : ranges.toArray(new HealthRange[0]);
    }

    private static BitSet parseBitSet(String list, int minValue, int maxValue) {
        if (MCPatcherUtils.isNullOrEmpty(list)) {
            return null;
        }
        BitSet bits = new BitSet(maxValue + 1);
        for (int i : MCPatcherUtils.parseIntegerList(list, minValue, maxValue)) {
            bits.set(i);
        }
        return bits;
    }

    private static String[] parseWeather(PropertiesFile properties, String list) {
        if (MCPatcherUtils.isNullOrEmpty(list)) {
            return null;
        }
        List<String> values = new ArrayList<>();
        for (String token : list.toLowerCase()
            .split("\\s+")) {
            if (token.equals("clear") || token.equals("rain") || token.equals("thunder")) {
                values.add(token);
            } else if (!token.isEmpty()) {
                properties.warning("unknown weather value %s", token);
            }
        }
        return values.isEmpty() ? null : values.toArray(new String[0]);
    }

    private static List<NBTRule> parseNbtRules(PropertiesFile properties, int index) {
        String prefix = "nbt." + index + ".";
        List<NBTRule> rules = null;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (entry.getKey()
                .startsWith(prefix)) {
                // NBTRule expects the "nbt." prefix followed directly by the tag
                // path, so the rule index is stripped before creating the rule.
                // NBTRule 要求 “nbt.” 前缀后直接跟随标签路径，因此创建规则前
                // 先去掉规则编号。
                NBTRule rule = NBTRule.create(
                    "nbt." + entry.getKey()
                        .substring(prefix.length()),
                    entry.getValue());
                if (rule == null) {
                    properties.warning("invalid nbt rule %s", entry.getKey());
                } else {
                    if (rules == null) {
                        rules = new ArrayList<>();
                    }
                    rules.add(rule);
                }
            }
        }
        return rules;
    }

    private static List<BlockStateMatcher> parseBlocks(PropertiesFile properties, int index) {
        String list = properties.getString("blocks." + index, "");
        if (MCPatcherUtils.isNullOrEmpty(list)) {
            return null;
        }
        List<BlockStateMatcher> matchers = new ArrayList<>();
        for (String token : list.split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            // Same token format as ctm.properties matchBlocks.
            // 与 ctm.properties 的 matchBlocks 使用相同的标记格式。
            BlockStateMatcher matcher = BlockAPI.createMatcher(properties, token);
            if (matcher != null) {
                matchers.add(matcher);
            }
        }
        return matchers.isEmpty() ? null : matchers;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("skins:");
        for (int i : skins) {
            sb.append(' ')
                .append(i + 1);
        }
        if (biomes != null) {
            sb.append(", biomes:");
            for (int i = biomes.nextSetBit(0); i >= 0; i = biomes.nextSetBit(i + 1)) {
                sb.append(' ')
                    .append(i);
            }
        }
        if (height != null) {
            sb.append(", height:");
            for (int i = height.nextSetBit(0); i >= 0; i = height.nextSetBit(i + 1)) {
                sb.append(' ')
                    .append(i);
            }
        }
        if (name != null) {
            sb.append(", name: ")
                .append(name);
        }
        if (professions != null) {
            sb.append(", professions:");
            for (int p : professions) {
                sb.append(' ')
                    .append(p);
            }
        }
        if (colors != null) {
            sb.append(", colors:");
            for (int i = colors.nextSetBit(0); i >= 0; i = colors.nextSetBit(i + 1)) {
                sb.append(' ')
                    .append(DYE_COLORS[i]);
            }
        }
        if (baby != null) {
            sb.append(", baby: ")
                .append(baby);
        }
        if (health != null) {
            sb.append(", health ranges: ")
                .append(health.length);
        }
        if (moonPhases != null) {
            sb.append(", moonPhase:");
            for (int i = moonPhases.nextSetBit(0); i >= 0; i = moonPhases.nextSetBit(i + 1)) {
                sb.append(' ')
                    .append(i);
            }
        }
        if (dayTimes != null) {
            sb.append(", dayTime set");
        }
        if (weather != null) {
            sb.append(", weather:");
            for (String w : weather) {
                sb.append(' ')
                    .append(w);
            }
        }
        if (sizes != null) {
            sb.append(", sizes:");
            for (int i = sizes.nextSetBit(0); i >= 0; i = sizes.nextSetBit(i + 1)) {
                sb.append(' ')
                    .append(i);
            }
        }
        if (nbtRules != null) {
            sb.append(", nbt rules: ")
                .append(nbtRules.size());
        }
        if (blocks != null) {
            sb.append(", blocks:");
            for (BlockStateMatcher matcher : blocks) {
                sb.append(' ')
                    .append(matcher);
            }
        }
        if (seedOffset != 0) {
            sb.append(", seedOffset: ")
                .append(seedOffset);
        }
        if (seedFromVehicle) {
            sb.append(", seedSource: vehicle");
        }
        sb.append(", weights: ")
            .append(weightedIndex.toString());
        return sb.toString();
    }

    /** A parsed health range, absolute or percent of max health. / 解析出的生命值区间，可为绝对值或最大生命值的百分比。 */
    private static final class HealthRange {

        final float min;
        final float max;
        final boolean percent;

        HealthRange(float min, float max, boolean percent) {
            this.min = min;
            this.max = max;
            this.percent = percent;
        }
    }

    /**
     * Custom name tag matcher supporting the name= formats of the Random
     * Entities documentation: exact match, pattern/ipattern wildcards,
     * regex/iregex Java regular expressions and "!" negative matching.
     * 自定义名称牌匹配器，支持 Random Entities 文档中的 name= 格式：
     * 精确匹配、pattern/ipattern 通配符、regex/iregex Java 正则表达式以及
     * “!” 取反匹配。
     */
    private static final class NameMatcher {

        private final boolean negate;
        private final String exactValue;
        private final Pattern pattern;

        static NameMatcher parse(PropertiesFile properties, String input) {
            if (MCPatcherUtils.isNullOrEmpty(input)) {
                return null;
            }
            boolean negate = false;
            String value = input;
            if (value.startsWith("!")) {
                negate = true;
                value = value.substring(1);
            }
            try {
                if (value.startsWith("pattern:")) {
                    return new NameMatcher(negate, compileGlob(value.substring(8), true));
                } else if (value.startsWith("ipattern:")) {
                    return new NameMatcher(negate, compileGlob(value.substring(9), false));
                } else if (value.startsWith("regex:")) {
                    return new NameMatcher(negate, Pattern.compile(value.substring(6)));
                } else if (value.startsWith("iregex:")) {
                    return new NameMatcher(negate, Pattern.compile(value.substring(7), Pattern.CASE_INSENSITIVE));
                } else {
                    return new NameMatcher(negate, value);
                }
            } catch (PatternSyntaxException e) {
                properties.warning("invalid name pattern %s", input);
                return null;
            }
        }

        private NameMatcher(boolean negate, String exactValue) {
            this.negate = negate;
            this.exactValue = exactValue;
            pattern = null;
        }

        private NameMatcher(boolean negate, Pattern pattern) {
            this.negate = negate;
            exactValue = null;
            this.pattern = pattern;
        }

        boolean matches(String name) {
            boolean result = exactValue != null ? exactValue.equals(name)
                : pattern.matcher(name == null ? "" : name)
                    .matches();
            return negate != result;
        }

        /**
         * Compiles a wildcard pattern ("*" and "?", backslash escapes) into a
         * Java regular expression.
         * 将通配符模式（“*”与“?”，反斜杠转义）编译为 Java 正则表达式。
         */
        private static Pattern compileGlob(String glob, boolean caseSensitive) {
            StringBuilder regex = new StringBuilder();
            for (int i = 0; i < glob.length(); i++) {
                char c = glob.charAt(i);
                if (c == '\\' && i + 1 < glob.length()) {
                    regex.append(Pattern.quote(String.valueOf(glob.charAt(++i))));
                } else if (c == '*') {
                    regex.append(".*");
                } else if (c == '?') {
                    regex.append('.');
                } else {
                    regex.append(Pattern.quote(String.valueOf(c)));
                }
            }
            return Pattern.compile(regex.toString(), caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
        }

        @Override
        public String toString() {
            return (negate ? "!" : "") + (exactValue != null ? exactValue : pattern.pattern());
        }
    }
}

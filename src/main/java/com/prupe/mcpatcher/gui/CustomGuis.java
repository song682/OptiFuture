package com.prupe.mcpatcher.gui;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.minecraft.block.BlockDropper;
import net.minecraft.client.Minecraft;
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
import net.minecraft.entity.IMerchant;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerBeacon;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.ContainerDispenser;
import net.minecraft.inventory.ContainerFurnace;
import net.minecraft.inventory.ContainerHopper;
import net.minecraft.inventory.ContainerHorseInventory;
import net.minecraft.inventory.ContainerMerchant;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryEnderChest;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import decok.dfcdvadstf.optifuture.mixins.early.gui.accessor.AccessorContainerBeacon;
import decok.dfcdvadstf.optifuture.mixins.early.gui.accessor.AccessorContainerDispenser;
import decok.dfcdvadstf.optifuture.mixins.early.gui.accessor.AccessorContainerFurnace;
import decok.dfcdvadstf.optifuture.mixins.early.gui.accessor.AccessorContainerHopper;
import decok.dfcdvadstf.optifuture.mixins.early.gui.accessor.AccessorContainerHorseInventory;
import decok.dfcdvadstf.optifuture.mixins.early.gui.accessor.AccessorContainerMerchant;
import decok.dfcdvadstf.optifuture.mixins.early.gui.accessor.AccessorGuiEnchantment;
import decok.dfcdvadstf.optifuture.mixins.early.gui.accessor.AccessorGuiMerchant;
import decok.dfcdvadstf.optifuture.mixins.early.gui.accessor.AccessorInventoryLargeChest;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.mal.biome.BiomeAPI;
import com.prupe.mcpatcher.mal.resource.PropertiesFile;
import com.prupe.mcpatcher.mal.resource.ResourceList;
import com.prupe.mcpatcher.mal.resource.TexturePackAPI;
import com.prupe.mcpatcher.mal.resource.TexturePackChangeHandler;

/**
 * Custom container GUI textures ("custom_guis", see temps/doc-modern/custom_guis.properties).
 * 自定义容器 GUI 纹理（"custom_guis"，参见 temps/doc-modern/custom_guis.properties）。
 * <p>
 * Resource layout / 资源布局：
 *
 * <pre>
 * assets/minecraft/{mcpatcher|optifine}/gui/container/**&#47;*.properties - GUI rules / GUI 规则
 * </pre>
 *
 * Each properties file describes one replacement rule: which container GUI it applies to
 * (container=...), which textures to swap (texture= / texture.&lt;path&gt;=) and optional
 * matching conditions (name, biomes, heights, chest flags, beacon levels, villager
 * professions, horse/dispenser variants). The first matching rule wins; rules are
 * evaluated once per opened GUI instance.
 * 每个属性文件描述一条替换规则：作用于哪种容器 GUI（container=...）、替换哪些纹理
 * （texture= / texture.&lt;path&gt;=）以及可选的匹配条件（名称、生物群系、高度、箱子标志、
 * 信标等级、村民职业、马/发射器变体）。第一条匹配成功的规则生效；每个打开的 GUI
 * 实例只评估一次。
 * <p>
 * All hooks are pass-through when no resource pack provides any rule.
 * 若没有任何材质包提供规则，所有挂钩点均原样放行。
 */
public class CustomGuis {

    private static final MCLogger logger = MCLogger.getLogger(MCLogger.Category.CUSTOM_GUIS);

    /** Both the mcpatcher and the optifine directory layout are accepted. / mcpatcher 与 optifine 两种目录布局均支持。 */
    private static final String[] BASE_DIRS = { "mcpatcher/gui/container/", "optifine/gui/container/" };
    private static final String GUI_TEXTURE_PREFIX = "textures/gui/";
    private static final String CONTAINER_TEXTURE_PREFIX = "textures/gui/container/";
    private static final String TEXTURE_KEY_PREFIX = "texture.";

    // Container type ids, aligned with DEFAULT_CONTAINER_TEXTURES below.
    // 容器类型编号，与下方 DEFAULT_CONTAINER_TEXTURES 一一对应。
    public static final int TYPE_ANVIL = 0;
    public static final int TYPE_BEACON = 1;
    public static final int TYPE_BREWING_STAND = 2;
    public static final int TYPE_CHEST = 3;
    public static final int TYPE_CRAFTING = 4;
    public static final int TYPE_DISPENSER = 5;
    public static final int TYPE_ENCHANTMENT = 6;
    public static final int TYPE_FURNACE = 7;
    public static final int TYPE_HOPPER = 8;
    public static final int TYPE_HORSE = 9;
    public static final int TYPE_VILLAGER = 10;
    public static final int TYPE_CREATIVE = 11;
    public static final int TYPE_INVENTORY = 12;

    private static final Map<String, Integer> CONTAINER_TYPES = new HashMap<>();
    static {
        CONTAINER_TYPES.put("anvil", TYPE_ANVIL);
        CONTAINER_TYPES.put("beacon", TYPE_BEACON);
        CONTAINER_TYPES.put("brewing_stand", TYPE_BREWING_STAND);
        CONTAINER_TYPES.put("chest", TYPE_CHEST);
        CONTAINER_TYPES.put("crafting", TYPE_CRAFTING);
        CONTAINER_TYPES.put("crafting_table", TYPE_CRAFTING);
        CONTAINER_TYPES.put("dispenser", TYPE_DISPENSER);
        CONTAINER_TYPES.put("enchantment", TYPE_ENCHANTMENT);
        CONTAINER_TYPES.put("enchanting_table", TYPE_ENCHANTMENT);
        CONTAINER_TYPES.put("furnace", TYPE_FURNACE);
        CONTAINER_TYPES.put("hopper", TYPE_HOPPER);
        CONTAINER_TYPES.put("horse", TYPE_HORSE);
        CONTAINER_TYPES.put("villager", TYPE_VILLAGER);
        CONTAINER_TYPES.put("creative", TYPE_CREATIVE);
        CONTAINER_TYPES.put("inventory", TYPE_INVENTORY);
    }

    /**
     * Vanilla background texture per container type (relative to textures/gui/), null where
     * no single default exists (creative inventory uses per-tab path textures only).
     * 各容器类型的原版背景纹理（相对于 textures/gui/）；没有单一默认纹理的类型为 null
     * （创造模式物品栏仅使用分页路径纹理）。
     */
    private static final String[] DEFAULT_CONTAINER_TEXTURES = {
        "container/anvil.png",
        "container/beacon.png",
        "container/brewing_stand.png",
        "container/generic_54.png",
        "container/crafting_table.png",
        "container/dispenser.png",
        "container/enchanting_table.png",
        "container/furnace.png",
        "container/hopper.png",
        "container/horse.png",
        "container/villager.png",
        null,
        "container/inventory.png" };

    /**
     * 1.7.10 profession ids: 0 farmer, 1 librarian, 2 priest, 3 blacksmith, 4 butcher,
     * 5 generic villager. Modern profession names are mapped onto the closest legacy
     * equivalent. 1.7.10 职业编号：0 农民、1 图书管理员、2 牧师、3 铁匠、4 屠夫、
     * 5 普通村民；新版职业名映射到最接近的旧版职业。
     */
    private static final Map<String, Integer> PROFESSION_IDS = new HashMap<>();
    static {
        PROFESSION_IDS.put("farmer", 0);
        PROFESSION_IDS.put("fisherman", 0);
        PROFESSION_IDS.put("shepherd", 0);
        PROFESSION_IDS.put("fletcher", 0);
        PROFESSION_IDS.put("mason", 0);
        PROFESSION_IDS.put("leatherworker", 0);
        PROFESSION_IDS.put("librarian", 1);
        PROFESSION_IDS.put("cartographer", 1);
        PROFESSION_IDS.put("priest", 2);
        PROFESSION_IDS.put("cleric", 2);
        PROFESSION_IDS.put("blacksmith", 3);
        PROFESSION_IDS.put("armorer", 3);
        PROFESSION_IDS.put("toolsmith", 3);
        PROFESSION_IDS.put("weaponsmith", 3);
        PROFESSION_IDS.put("butcher", 4);
        PROFESSION_IDS.put("nitwit", 5);
    }

    private static final Map<String, Integer> HORSE_VARIANT_IDS = new HashMap<>();
    static {
        HORSE_VARIANT_IDS.put("horse", 0);
        HORSE_VARIANT_IDS.put("donkey", 1);
        HORSE_VARIANT_IDS.put("mule", 2);
        HORSE_VARIANT_IDS.put("zombie", 3);
        HORSE_VARIANT_IDS.put("skeleton", 4);
    }

    private static final Map<String, Integer> DISPENSER_VARIANT_IDS = new HashMap<>();
    static {
        DISPENSER_VARIANT_IDS.put("dispenser", 0);
        DISPENSER_VARIANT_IDS.put("dropper", 1);
    }

    /** Loaded rules in resource pack priority order. / 按材质包优先级排序的规则列表。 */
    private static final List<GuiEntry> entries = new ArrayList<>();
    /** Resolved rule per GUI instance; NO_MATCH records a negative result. / 每个 GUI 实例的求值结果；NO_MATCH 记录未匹配。 */
    private static final Map<GuiScreen, Object> resolvedCache = new WeakHashMap<>();
    private static final Object NO_MATCH = new Object();

    static {
        TexturePackChangeHandler.register(new TexturePackChangeHandler(MCPatcherUtils.CUSTOM_GUIS, 4) {

            @Override
            public void beforeChange() {
                entries.clear();
                resolvedCache.clear();
            }

            @Override
            public void afterChange() {
                reload();
            }
        });
    }

    /**
     * Rebuilds the rule list from the currently selected resource packs.
     * 依据当前启用的材质包重建规则列表。
     */
    private static void reload() {
        for (String baseDir : BASE_DIRS) {
            for (ResourceLocation resource : ResourceList.getInstance()
                .listResources(baseDir, ".properties", true)) {
                PropertiesFile properties = PropertiesFile.get(logger, resource);
                if (properties == null) {
                    continue;
                }
                GuiEntry entry = new GuiEntry(properties);
                if (entry.isValid()) {
                    entries.add(entry);
                }
            }
        }
        if (!entries.isEmpty()) {
            logger.fine("%d custom GUI rule(s) loaded", entries.size());
        }
    }

    /**
     * Hook invoked by the container GUI mixins: returns the replacement texture for the
     * given bindTexture target, or the original texture when no rule matches.
     * 容器 GUI mixin 调用的挂钩点：返回绑定纹理的替换项；无规则匹配时原样返回。
     */
    public static ResourceLocation remapTexture(GuiScreen gui, ResourceLocation texture) {
        if (texture == null || entries.isEmpty()) {
            return texture;
        }
        GuiEntry entry = resolveEntry(gui);
        if (entry == null) {
            return texture;
        }
        ResourceLocation replacement = entry.getReplacement(texture);
        return replacement != null ? replacement : texture;
    }

    private static GuiEntry resolveEntry(GuiScreen gui) {
        Object cached = resolvedCache.get(gui);
        if (cached != null) {
            return cached == NO_MATCH ? null : (GuiEntry) cached;
        }
        GuiEntry matched = null;
        int type = getGuiType(gui);
        if (type >= 0) {
            for (GuiEntry entry : entries) {
                if (entry.type == type && entry.match(gui)) {
                    matched = entry;
                    break;
                }
            }
        }
        resolvedCache.put(gui, matched == null ? NO_MATCH : matched);
        if (matched != null) {
            logger.fine("matched %s for %s", matched.properties, gui.getClass()
                .getSimpleName());
        }
        return matched;
    }

    /** Maps a GUI screen class onto its container type id, -1 when unsupported. / 将 GUI 类映射为容器类型编号，不支持时返回 -1。 */
    private static int getGuiType(GuiScreen gui) {
        if (gui instanceof GuiRepair) {
            return TYPE_ANVIL;
        } else if (gui instanceof GuiBeacon) {
            return TYPE_BEACON;
        } else if (gui instanceof GuiBrewingStand) {
            return TYPE_BREWING_STAND;
        } else if (gui instanceof GuiChest) {
            return TYPE_CHEST;
        } else if (gui instanceof GuiCrafting) {
            return TYPE_CRAFTING;
        } else if (gui instanceof GuiDispenser) {
            return TYPE_DISPENSER;
        } else if (gui instanceof GuiEnchantment) {
            return TYPE_ENCHANTMENT;
        } else if (gui instanceof GuiFurnace) {
            return TYPE_FURNACE;
        } else if (gui instanceof GuiHopper) {
            return TYPE_HOPPER;
        } else if (gui instanceof GuiScreenHorseInventory) {
            return TYPE_HORSE;
        } else if (gui instanceof GuiMerchant) {
            return TYPE_VILLAGER;
        } else if (gui instanceof GuiContainerCreative) {
            return TYPE_CREATIVE;
        } else if (gui instanceof GuiInventory) {
            return TYPE_INVENTORY;
        } else {
            return -1;
        }
    }

    /** Vanilla christmas chest check (Dec 24-26), copied from TileEntityChestRenderer. / 原版圣诞箱子判定（12 月 24-26 日），与 TileEntityChestRenderer 一致。 */
    private static boolean isChristmas() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.MONTH) + 1 == 12 && calendar.get(Calendar.DAY_OF_MONTH) >= 24
            && calendar.get(Calendar.DAY_OF_MONTH) <= 26;
    }

    /**
     * One parsed rule. All matching conditions are ANDed; an absent condition always passes.
     * 单条解析后的规则。所有匹配条件之间为 AND 关系；未指定的条件始终通过。
     */
    private static class GuiEntry {

        final PropertiesFile properties;
        final int type;
        /** Full vanilla texture path -> replacement. / 完整原版纹理路径 -> 替换纹理。 */
        final Map<String, ResourceLocation> textureMap = new HashMap<>();

        NameMatcher name;
        BitSet biomes;
        BitSet heights;
        Boolean large;
        Boolean trapped;
        Boolean christmas;
        Boolean ender;
        BitSet levels;
        BitSet professions;
        BitSet variants;
        /** Set when the rule uses something 1.7.10 cannot evaluate (e.g. shulker colors). / 规则包含 1.7.10 无法求值的条件（如潜影盒颜色）时置位。 */
        boolean unsupported;
        boolean invalid;

        GuiEntry(PropertiesFile properties) {
            this.properties = properties;
            String container = properties.getString("container", "")
                .trim()
                .toLowerCase();
            Integer typeId = CONTAINER_TYPES.get(container);
            if (typeId == null) {
                properties.warning("unknown or missing container type '%s'", container);
                invalid = true;
                type = -1;
                return;
            }
            type = typeId;

            parseTextures(properties);
            if (textureMap.isEmpty()) {
                properties.warning("no texture or texture.<path> specified");
                invalid = true;
            }

            name = NameMatcher.create(properties.getString("name", ""));

            String biomeList = properties.getString("biomes", "");
            if (!MCPatcherUtils.isNullOrEmpty(biomeList)) {
                biomes = new BitSet();
                BiomeAPI.parseBiomeList(biomeList, biomes);
                if (biomes.isEmpty()) {
                    properties.warning("no valid biomes in '%s'", biomeList);
                }
            }
            heights = BiomeAPI.getHeightListProperty(properties, "");

            large = getOptionalBoolean(properties, "large");
            trapped = getOptionalBoolean(properties, "trapped");
            christmas = getOptionalBoolean(properties, "christmas");
            ender = getOptionalBoolean(properties, "ender");
            levels = parseBitSetList(properties, "levels", 0, 100);
            professions = parseProfessions(properties);
            variants = parseVariants(properties);

            // Colors only apply to llamas and shulker boxes, neither of which exists in 1.7.10.
            // colors 仅适用于羊驼与潜影盒，两者在 1.7.10 中均不存在。
            if (!MCPatcherUtils.isNullOrEmpty(properties.getString("colors", ""))) {
                properties.warning("colors= is not supported on 1.7.10, rule will never match");
                unsupported = true;
            }
        }

        boolean isValid() {
            return !invalid;
        }

        private void parseTextures(PropertiesFile properties) {
            String defaultTexture = properties.getString("texture", "");
            if (!MCPatcherUtils.isNullOrEmpty(defaultTexture)) {
                if (type == TYPE_CREATIVE) {
                    properties.warning("the creative inventory has no default texture, use texture.<path> instead");
                } else {
                    ResourceLocation replacement = resolveTexture(properties, defaultTexture);
                    if (replacement != null) {
                        textureMap.put(CONTAINER_TEXTURE_PREFIX + DEFAULT_CONTAINER_TEXTURES[type], replacement);
                    }
                }
            }
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String key = entry.getKey();
                if (!key.startsWith(TEXTURE_KEY_PREFIX)) {
                    continue;
                }
                String path = key.substring(TEXTURE_KEY_PREFIX.length());
                ResourceLocation replacement = resolveTexture(properties, entry.getValue());
                if (replacement == null || path.isEmpty()) {
                    continue;
                }
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                if (path.startsWith("assets/minecraft/")) {
                    path = path.substring("assets/minecraft/".length());
                }
                if (path.startsWith(GUI_TEXTURE_PREFIX)) {
                    path = path.substring(GUI_TEXTURE_PREFIX.length());
                }
                if (!path.endsWith(".png")) {
                    path += ".png";
                }
                textureMap.put(GUI_TEXTURE_PREFIX + path, replacement);
            }
        }

        private ResourceLocation resolveTexture(PropertiesFile properties, String value) {
            if (MCPatcherUtils.isNullOrEmpty(value)) {
                return null;
            }
            ResourceLocation resource = TexturePackAPI.parseResourceLocation(properties.getResource(), value.trim());
            if (resource != null && !resource.getResourcePath()
                .endsWith(".png")) {
                resource = new ResourceLocation(resource.getResourceDomain(), resource.getResourcePath() + ".png");
            }
            if (resource == null) {
                properties.warning("invalid texture path '%s'", value);
            }
            return resource;
        }

        ResourceLocation getReplacement(ResourceLocation texture) {
            String path = texture.getResourcePath();
            if (!path.startsWith(GUI_TEXTURE_PREFIX)) {
                return null;
            }
            return textureMap.get(path);
        }

        boolean match(GuiScreen gui) {
            if (unsupported || !(gui instanceof GuiContainer)) {
                return false;
            }
            Container container = ((GuiContainer) gui).inventorySlots;
            if (container == null) {
                return false;
            }
            if (name != null && !name.match(getName(gui, container))) {
                return false;
            }
            if ((biomes != null || heights != null) && !matchPlayerPosition()) {
                return false;
            }
            switch (type) {
                case TYPE_CHEST:
                    return matchChest(container);

                case TYPE_BEACON:
                    return matchBeacon(container);

                case TYPE_VILLAGER:
                    return matchVillager(container);

                case TYPE_HORSE:
                    return matchHorse(container);

                case TYPE_DISPENSER:
                    return matchDispenser(container);

                default:
                    return true;
            }
        }

        private boolean matchPlayerPosition() {
            Minecraft minecraft = Minecraft.getMinecraft();
            EntityPlayer player = minecraft.thePlayer;
            World world = minecraft.theWorld;
            if (player == null || world == null) {
                return false;
            }
            int x = (int) Math.floor(player.posX);
            int y = (int) Math.floor(player.posY);
            int z = (int) Math.floor(player.posZ);
            if (biomes != null) {
                int biomeID = BiomeAPI.getBiomeIDAt(world, x, y, z);
                if (biomeID < 0 || !biomes.get(biomeID)) {
                    return false;
                }
            }
            return heights == null || (y >= 0 && heights.get(y));
        }

        private boolean matchChest(Container container) {
            if (!(container instanceof ContainerChest)) {
                return false;
            }
            IInventory lower = ((ContainerChest) container).getLowerChestInventory();
            if (lower == null) {
                return false;
            }
            if (large != null && lower instanceof InventoryLargeChest != large) {
                return false;
            }
            if (ender != null && lower instanceof InventoryEnderChest != ender) {
                return false;
            }
            if (trapped != null || christmas != null) {
                TileEntityChest chest = getChestTileEntity(lower);
                // Without a chest tile entity the chest cannot be trapped; christmas is a
                // global calendar condition and still applies to plain chests.
                // 拿不到箱子方块实体时视为非陷阱箱；圣诞是全局日期条件，对普通箱子仍然适用。
                boolean isTrapped = chest != null && chest.func_145980_j() == 1;
                if (trapped != null && isTrapped != trapped) {
                    return false;
                }
                if (christmas != null && isChristmas() != christmas) {
                    return false;
                }
            }
            return true;
        }

        private TileEntityChest getChestTileEntity(IInventory lower) {
            if (lower instanceof TileEntityChest) {
                return (TileEntityChest) lower;
            } else if (lower instanceof InventoryLargeChest) {
                IInventory side = ((AccessorInventoryLargeChest) lower).getUpperChest();
                return side instanceof TileEntityChest ? (TileEntityChest) side : null;
            } else {
                return null;
            }
        }

        private boolean matchBeacon(Container container) {
            if (levels == null) {
                return true;
            }
            if (!(container instanceof ContainerBeacon)) {
                return false;
            }
            TileEntityBeacon beacon = ((AccessorContainerBeacon) container).getTileBeacon();
            return beacon != null && levels.get(Math.max(beacon.getLevels(), 0));
        }

        private boolean matchVillager(Container container) {
            if (professions == null) {
                return true;
            }
            if (!(container instanceof ContainerMerchant)) {
                return false;
            }
            IMerchant merchant = ((AccessorContainerMerchant) container).getMerchant();
            return merchant instanceof EntityVillager
                && professions.get(((EntityVillager) merchant).getProfession());
        }

        private boolean matchHorse(Container container) {
            if (variants == null) {
                return true;
            }
            if (!(container instanceof ContainerHorseInventory)) {
                return false;
            }
            EntityHorse horse = ((AccessorContainerHorseInventory) container).getHorse();
            return horse != null && variants.get(horse.getHorseType());
        }

        private boolean matchDispenser(Container container) {
            if (variants == null) {
                return true;
            }
            if (!(container instanceof ContainerDispenser)) {
                return false;
            }
            TileEntityDispenser tile = ((AccessorContainerDispenser) container).getTileDispenser();
            if (tile == null || tile.getWorldObj() == null) {
                return false;
            }
            boolean dropper = tile.getWorldObj()
                .getBlock(tile.xCoord, tile.yCoord, tile.zCoord) instanceof BlockDropper;
            return variants.get(dropper ? 1 : 0);
        }

        /**
         * Custom entity/block entity name of the GUI, null when none is available. Only
         * explicitly set custom names participate, matching CIT's nbt.display.Name behavior.
         * GUI 对应的自定义实体/方块实体名；无可用名称时返回 null。与 CIT 的
         * nbt.display.Name 行为一致，仅显式设置的自定义名称参与匹配。
         */
        private String getName(GuiScreen gui, Container container) {
            switch (type) {
                case TYPE_CHEST:
                    if (container instanceof ContainerChest) {
                        return getInventoryName(((ContainerChest) container).getLowerChestInventory());
                    }
                    return null;

                case TYPE_BEACON:
                    if (container instanceof ContainerBeacon) {
                        return getInventoryName(((AccessorContainerBeacon) container).getTileBeacon());
                    }
                    return null;

                case TYPE_FURNACE:
                    if (container instanceof ContainerFurnace) {
                        return getInventoryName(((AccessorContainerFurnace) container).getTileFurnace());
                    }
                    return null;

                case TYPE_DISPENSER:
                    if (container instanceof ContainerDispenser) {
                        return getInventoryName(((AccessorContainerDispenser) container).getTileDispenser());
                    }
                    return null;

                case TYPE_HOPPER:
                    if (container instanceof ContainerHopper) {
                        return getInventoryName(((AccessorContainerHopper) container).getHopperInventory());
                    }
                    return null;

                case TYPE_ENCHANTMENT:
                    return gui instanceof GuiEnchantment ? ((AccessorGuiEnchantment) gui).getCustomName() : null;

                case TYPE_VILLAGER:
                    return gui instanceof GuiMerchant ? ((AccessorGuiMerchant) gui).getGuiTitle() : null;

                default:
                    return null;
            }
        }

        private String getInventoryName(IInventory inventory) {
            return inventory != null && inventory.hasCustomInventoryName() ? inventory.getInventoryName() : null;
        }

        private static Boolean getOptionalBoolean(PropertiesFile properties, String key) {
            String value = properties.getString(key, "")
                .trim();
            if (value.isEmpty()) {
                return null;
            }
            return value.equalsIgnoreCase("true");
        }

        private static BitSet parseBitSetList(PropertiesFile properties, String key, int minValue, int maxValue) {
            String value = properties.getString(key, "");
            if (MCPatcherUtils.isNullOrEmpty(value)) {
                return null;
            }
            BitSet bits = new BitSet();
            for (int i : MCPatcherUtils.parseIntegerList(value, minValue, maxValue)) {
                bits.set(i);
            }
            if (bits.isEmpty()) {
                properties.warning("no valid values for %s in '%s'", key, value);
                return null;
            }
            return bits;
        }

        /**
         * Parses professions=name1[:levels] name2... Level suffixes are accepted for
         * compatibility but ignored: 1.7.10 villagers have no levels.
         * 解析 professions=name1[:等级] name2... 语法；等级后缀为兼容性而接受，
         * 但会被忽略：1.7.10 的村民没有等级概念。
         */
        private BitSet parseProfessions(PropertiesFile properties) {
            String value = properties.getString("professions", "");
            if (MCPatcherUtils.isNullOrEmpty(value)) {
                return null;
            }
            BitSet bits = new BitSet();
            for (String token : value.split("\\s+")) {
                String profession = token.contains(":") ? token.substring(0, token.indexOf(':')) : token;
                profession = profession.trim()
                    .toLowerCase()
                    .replaceFirst("^minecraft:", "");
                Integer id = PROFESSION_IDS.get(profession);
                if (id == null) {
                    try {
                        id = Integer.parseInt(profession);
                    } catch (NumberFormatException e) {
                        properties.warning("unknown villager profession '%s'", profession);
                        continue;
                    }
                }
                bits.set(id);
            }
            return bits.isEmpty() ? null : bits;
        }

        private BitSet parseVariants(PropertiesFile properties) {
            String value = properties.getString("variants", "");
            if (MCPatcherUtils.isNullOrEmpty(value)) {
                return null;
            }
            Map<String, Integer> variantIds;
            if (type == TYPE_HORSE) {
                variantIds = HORSE_VARIANT_IDS;
            } else if (type == TYPE_DISPENSER) {
                variantIds = DISPENSER_VARIANT_IDS;
            } else {
                properties.warning("variants= only applies to horse and dispenser containers");
                return null;
            }
            BitSet bits = new BitSet();
            for (String token : value.split("\\s+")) {
                String variant = token.trim()
                    .toLowerCase();
                Integer id = variantIds.get(variant);
                if (id == null) {
                    if (type == TYPE_HORSE && "llama".equals(variant)) {
                        properties.warning("llamas do not exist on 1.7.10, rule will never match");
                        unsupported = true;
                    } else {
                        properties.warning("unknown variant '%s'", variant);
                    }
                    continue;
                }
                bits.set(id);
            }
            return bits.isEmpty() ? null : bits;
        }
    }

    /**
     * String matcher with the CIT name syntax: exact, pattern:/ipattern: wildcards,
     * regex:/iregex: regular expressions and a leading '!' for negation.
     * 采用 CIT 名称语法的字符串匹配器：精确匹配、pattern:/ipattern: 通配符、
     * regex:/iregex: 正则表达式，以及前导 '!' 表示取反。
     */
    private static class NameMatcher {

        private static final String REGEX_PREFIX = "regex:";
        private static final String IREGEX_PREFIX = "iregex:";
        private static final String GLOB_PREFIX = "pattern:";
        private static final String IGLOB_PREFIX = "ipattern:";

        private final boolean negate;
        /** null unless this is a regex rule. / 非正则规则时为 null。 */
        private final Pattern regex;
        /** null unless this is a glob rule. / 非通配符规则时为 null。 */
        private final String glob;
        private final boolean caseSensitive;
        /** null unless this is an exact rule. / 非精确规则时为 null。 */
        private final String exact;

        static NameMatcher create(String value) {
            if (MCPatcherUtils.isNullOrEmpty(value)) {
                return null;
            }
            boolean negate = false;
            if (value.startsWith("!")) {
                negate = true;
                value = value.substring(1);
            }
            try {
                if (value.startsWith(REGEX_PREFIX)) {
                    return new NameMatcher(negate, Pattern.compile(value.substring(REGEX_PREFIX.length())), null, true,
                        null);
                } else if (value.startsWith(IREGEX_PREFIX)) {
                    return new NameMatcher(negate,
                        Pattern.compile(value.substring(IREGEX_PREFIX.length()), Pattern.CASE_INSENSITIVE), null,
                        false, null);
                } else if (value.startsWith(GLOB_PREFIX)) {
                    return new NameMatcher(negate, null, value.substring(GLOB_PREFIX.length()), true, null);
                } else if (value.startsWith(IGLOB_PREFIX)) {
                    return new NameMatcher(negate, null, value.substring(IGLOB_PREFIX.length())
                        .toLowerCase(), false, null);
                } else {
                    return new NameMatcher(negate, null, null, true, value);
                }
            } catch (PatternSyntaxException e) {
                logger.warning("invalid name pattern '%s': %s", value, e.getMessage());
                return null;
            }
        }

        private NameMatcher(boolean negate, Pattern regex, String glob, boolean caseSensitive, String exact) {
            this.negate = negate;
            this.regex = regex;
            this.glob = glob;
            this.caseSensitive = caseSensitive;
            this.exact = exact;
        }

        boolean match(String value) {
            // No name available: the rule cannot match, even when negated.
            // 无可用名称：规则不匹配，取反时亦然。
            if (value == null) {
                return false;
            }
            boolean matched;
            if (regex != null) {
                matched = regex.matcher(value)
                    .matches();
            } else if (glob != null) {
                matched = matchGlob(value, 0, value.length(), 0, glob.length());
            } else {
                matched = caseSensitive ? value.equals(exact) : value.equalsIgnoreCase(exact);
            }
            return negate != matched;
        }

        /** Wildcard match over '*' (any run) and '?' (single char), ported from NBTRule. / '*'（任意串）与 '?'（单字符）的通配匹配，移植自 NBTRule。 */
        private boolean matchGlob(String value, int curV, int maxV, int curG, int maxG) {
            for (; curG < maxG; curG++, curV++) {
                char g = glob.charAt(curG);
                if (g == '*') {
                    while (true) {
                        if (matchGlob(value, curV, maxV, curG + 1, maxG)) {
                            return true;
                        }
                        if (curV >= maxV) {
                            break;
                        }
                        curV++;
                    }
                    return false;
                } else if (curV >= maxV) {
                    break;
                } else if (g == '?') {
                    continue;
                }
                if (g == '\\' && curG + 1 < maxG) {
                    curG++;
                    g = glob.charAt(curG);
                }
                char v = value.charAt(curV);
                if (g != (caseSensitive ? v : Character.toLowerCase(v))) {
                    return false;
                }
            }
            return curG == maxG && curV == maxV;
        }
    }
}

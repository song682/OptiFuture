package com.prupe.mcpatcher.mob;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.mal.biome.BiomeAPI;
import com.prupe.mcpatcher.mal.resource.TexturePackChangeHandler;

/**
 * Entry point of the Random Entities feature (the modern successor of Random
 * Mobs). Every entity texture binding is routed through
 * {@link #randomTexture(Entity, ResourceLocation)}, which picks a randomized
 * variant based on the entity's persistent extra info and the rules loaded by
 * {@link MobRuleList}. Besides living mobs, non-living entities with textures
 * such as paintings are supported as well.
 * <p>
 * Random Entities 特性（Random Mobs 的现代继任者）的入口。每一次实体纹理
 * 绑定都会经由 {@link #randomTexture(Entity, ResourceLocation)} 路由，根据
 * 实体的持久化附加信息与 {@link MobRuleList} 加载的规则挑选随机化变体。
 * 除生物外，画等拥有纹理的非生物实体同样受支持。
 */
public class MobRandomizer {

    private static final MCLogger logger = MCLogger.getLogger(MCLogger.Category.RANDOM_MOBS);
    private static final Map<String, ResourceLocation> cache = new LinkedHashMap<>();

    static {
        TexturePackChangeHandler.register(new TexturePackChangeHandler(MCPatcherUtils.RANDOM_MOBS, 2) {

            @Override
            public void beforeChange() {
                cache.clear();
            }

            @Override
            public void afterChange() {
                MobRuleList.clear();
                MobOverlay.reset();
                LineRenderer.reset();
            }
        });
    }

    public static void init() {}

    public static ResourceLocation randomTexture(Entity entity, ResourceLocation texture) {
        if (entity == null || texture == null || !texture.getResourcePath()
            .endsWith(".png")) {
            return texture;
        }
        String key = texture + ":" + entity.getEntityId();
        ResourceLocation newTexture = cache.get(key);
        if (newTexture == null) {
            ExtraInfo info = ExtraInfo.getInfo(entity);
            MobRuleList list = MobRuleList.get(texture);
            newTexture = list.getSkin(entity, info);
            cache.put(key, newTexture);
            logger.finer("entity %s using %s (cache: %d)", entity, newTexture, cache.size());
            if (cache.size() > 250) {
                while (cache.size() > 200) {
                    cache.remove(
                        cache.keySet()
                            .iterator()
                            .next());
                }
            }
        }
        return newTexture;
    }

    /**
     * Persistent per-entity randomization state. Originally keyed to living
     * mobs only, it now covers every entity type so that paintings and other
     * non-living entities can be randomized too. The info is shared between
     * the server and the client entity instances via the entity id, which is
     * what lets server-side data (NBT, saved spawn position) reach the client
     * renderer in single player.
     * <p>
     * 每个实体的持久化随机状态。最初仅针对生物，现在覆盖所有实体类型，
     * 使画等非生物实体也可被随机化。该信息通过实体 id 在服务端与客户端
     * 实体实例之间共享，这正是单人游戏中服务端数据（NBT、保存的生成
     * 位置）能够到达客户端渲染器的原因。
     */
    public static final class ExtraInfo {

        private static final String SKIN_TAG = "randomMobsSkin";
        private static final String ORIG_X_TAG = "origX";
        private static final String ORIG_Y_TAG = "origY";
        private static final String ORIG_Z_TAG = "origZ";

        private static final long MULTIPLIER = 0x5deece66dL;
        private static final long ADDEND = 0xbL;
        private static final long MASK = (1L << 48) - 1;

        private static final Map<Integer, ExtraInfo> allInfo = new HashMap<>();
        private static final Map<WeakReference<Entity>, ExtraInfo> allRefs = new HashMap<>();
        private static final ReferenceQueue<Entity> refQueue = new ReferenceQueue<>();

        private final int entityId;
        private final HashSet<WeakReference<Entity>> references;
        private final long skin;
        private final int origX;
        private final int origY;
        private final int origZ;
        private Integer origBiome;
        // Captured entity NBT for the nbt.<n> rule conditions. Only populated
        // in single player, since dedicated servers never send entity NBT to
        // the client.
        // 为 nbt.<n> 规则条件捕获的实体 NBT。仅在单人游戏中填充，因为
        // 专用服务器从不向客户端发送实体 NBT。
        private NBTTagCompound nbt;

        ExtraInfo(Entity entity) {
            this(entity, getSkinId(entity.getEntityId()), (int) entity.posX, (int) entity.posY, (int) entity.posZ);
        }

        ExtraInfo(Entity entity, long skin, int origX, int origY, int origZ) {
            entityId = entity.getEntityId();
            references = new HashSet<>();
            this.skin = skin;
            this.origX = origX;
            this.origY = origY;
            this.origZ = origZ;
        }

        long getSkin() {
            return skin;
        }

        int getOrigY() {
            return origY;
        }

        Integer getBiome() {
            return origBiome;
        }

        NBTTagCompound getNbt() {
            return nbt;
        }

        private void setBiome() {
            if (origBiome == null) {
                origBiome = BiomeAPI.getBiomeIDAt(BiomeAPI.getWorld(), origX, origY, origZ);
            }
        }

        @Override
        public String toString() {
            return String.format(
                "%s{%d, %d, %d, %d, %d, %s}",
                getClass().getSimpleName(),
                entityId,
                skin,
                origX,
                origY,
                origZ,
                origBiome);
        }

        private static void clearUnusedReferences() {
            synchronized (allInfo) {
                Reference<? extends Entity> ref;
                while ((ref = refQueue.poll()) != null) {
                    ExtraInfo info = allRefs.get(ref);
                    if (info != null) {
                        info.references.remove(ref);
                        if (info.references.isEmpty()) {
                            logger.finest("removing unused ref %d", info.entityId);
                            allInfo.remove(info.entityId);
                        }
                    }
                    allRefs.remove(ref);
                }
            }
        }

        static ExtraInfo getInfo(Entity entity) {
            ExtraInfo info;
            synchronized (allInfo) {
                clearUnusedReferences();
                info = allInfo.get(entity.getEntityId());
                if (info == null) {
                    info = new ExtraInfo(entity);
                    putInfo(entity, info);
                }
                boolean found = false;
                for (WeakReference<Entity> ref : info.references) {
                    if (ref.get() == entity) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    WeakReference<Entity> reference = new WeakReference<>(entity, refQueue);
                    info.references.add(reference);
                    allRefs.put(reference, info);
                    logger.finest(
                        "added ref #%d for %d (%d entities)",
                        info.references.size(),
                        entity.getEntityId(),
                        allInfo.size());
                }
                info.setBiome();
            }
            return info;
        }

        static void putInfo(Entity entity, ExtraInfo info) {
            synchronized (allInfo) {
                allInfo.put(entity.getEntityId(), info);
            }
        }

        static void clearInfo() {
            synchronized (allInfo) {
                allInfo.clear();
            }
        }

        static long getSkinId(int entityId) {
            long n = entityId;
            n = n ^ (n << 16) ^ (n << 32) ^ (n << 48);
            n = MULTIPLIER * n + ADDEND;
            n = MULTIPLIER * n + ADDEND;
            n &= MASK;
            return (n >> 32) ^ n;
        }

        public static void readFromNBT(EntityLivingBase entity, NBTTagCompound nbt) {
            synchronized (allInfo) {
                ExtraInfo info;
                long skin = nbt.getLong(SKIN_TAG);
                if (skin != 0L) {
                    int x = nbt.getInteger(ORIG_X_TAG);
                    int y = nbt.getInteger(ORIG_Y_TAG);
                    int z = nbt.getInteger(ORIG_Z_TAG);
                    info = new ExtraInfo(entity, skin, x, y, z);
                    putInfo(entity, info);
                } else {
                    info = allInfo.get(entity.getEntityId());
                }
                // Keep the full entity NBT around so nbt.<n> rule conditions
                // can be matched on the client side.
                // 保留完整的实体 NBT，使 nbt.<n> 规则条件能在客户端被匹配。
                if (info != null) {
                    info.nbt = nbt;
                }
            }
        }

        public static void writeToNBT(EntityLivingBase entity, NBTTagCompound nbt) {
            synchronized (allInfo) {
                ExtraInfo info = allInfo.get(entity.getEntityId());
                if (info != null) {
                    nbt.setLong(SKIN_TAG, info.skin);
                    nbt.setInteger(ORIG_X_TAG, info.origX);
                    nbt.setInteger(ORIG_Y_TAG, info.origY);
                    nbt.setInteger(ORIG_Z_TAG, info.origZ);
                }
            }
        }
    }
}

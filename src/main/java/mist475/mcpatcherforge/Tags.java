package mist475.mcpatcherforge;

/**
 * Mod metadata constants. build.gradle reads MODID / MODNAME / VERSION from this file
 * (needTagsToIdentify=true), so the values here drive the jar name, mcmod.info and the
 * generated refmap file name (mixins.{MODID}.refmap.json).
 * <p>
 * 模组元数据常量。build.gradle 会从本文件读取 MODID / MODNAME / VERSION（needTagsToIdentify=true），
 * 因此这里的值决定了 jar 名称、mcmod.info 以及生成的 refmap 文件名（mixins.{MODID}.refmap.json）。
 * MODID 必须与资源目录中 mixin 配置引用的 "mixins.mcpatcherforge.refmap.json" 保持一致。
 */
public class Tags {
    public static final String MODID = "mcpatcherforge";
    public static final String MODNAME = "MCPatcherForge";
    public static final String VERSION = "1.0.0";
}

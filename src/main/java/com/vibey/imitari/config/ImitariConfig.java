package com.vibey.imitari.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.List;

public class ImitariConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // Gameplay Settings
    public static final ForgeConfigSpec.BooleanValue CONSUMES_ITEMS_IN_SURVIVAL;
    public static final ForgeConfigSpec.BooleanValue ALLOW_SURVIVAL_REMOVAL;

    // Dynamic System Settings
    public static final ForgeConfigSpec.BooleanValue ENABLE_DYNAMIC_TAGS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TAG_BLACKLIST;
    public static final ForgeConfigSpec.BooleanValue COPY_HARDNESS;
    public static final ForgeConfigSpec.BooleanValue COPY_RESISTANCE;

    static {
        BUILDER.push("Gameplay Settings");

        CONSUMES_ITEMS_IN_SURVIVAL = BUILDER
                .comment("Whether placing blocks into CopyBlocks consumes items in survival mode")
                .define("consumesItemsInSurvival", true);

        ALLOW_SURVIVAL_REMOVAL = BUILDER
                .comment("Whether shift+empty hand removes copied blocks in survival mode (always works in creative)")
                .define("allowSurvivalRemoval", false);

        BUILDER.pop();

        BUILDER.push("Dynamic Systems");

        ENABLE_DYNAMIC_TAGS = BUILDER
                .comment("Enable dynamic tag inheritance from copied blocks")
                .define("enableDynamicTags", true);

        TAG_BLACKLIST = BUILDER
                .comment("List of tags that CopyBlocks should NOT inherit from copied blocks",
                        "Format: 'namespace:path' (e.g., 'minecraft:dragon_immune', 'forge:ores')")
                .defineList("tagBlacklist", List.of(), obj -> obj instanceof String);

        COPY_HARDNESS = BUILDER
                .comment("Whether CopyBlocks inherit hardness (mining speed) from copied blocks")
                .define("copyHardness", true);

        COPY_RESISTANCE = BUILDER
                .comment("Whether CopyBlocks inherit explosion resistance from copied blocks")
                .define("copyResistance", true);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SPEC, "imitari-server.toml");
    }
}
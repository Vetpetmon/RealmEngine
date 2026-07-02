package com.vetpetmon.realmengine.common;

import net.neoforged.neoforge.common.ModConfigSpec;

public class CommonConfig {

    // Enum for scaling mode (log, exponential, linear)
    public enum ScalingMode {
        LOGARITHMIC,
        EXPONENTIAL,
        LINEAR
    }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue disableNetherPortal = BUILDER
            .comment("Disable Nether Portal functionality (entities won't be able to use them). Default is false.")
            .define("disableNetherPortal", false);
    public static final ModConfigSpec.BooleanValue disableEndPortal = BUILDER
            .comment("Disable End Portal functionality (entities won't be able to use them). Default is false.")
            .define("disableEndPortal", false);

    public static final ModConfigSpec.BooleanValue tieringEnabled = BUILDER
            .comment("Enable tiered mob scaling based on distance from world spawn. Default is true.")
            .define("tieringEnabled", true);

    public static final ModConfigSpec.IntValue noTieringZoneRadius = BUILDER
            .comment("Distance before tiering starts, to make starting areas far less dangerous. Default is 500.")
            .defineInRange("noTieringZoneRadius", 500, 1, 1000);
    public static final ModConfigSpec.IntValue distancePerTier = BUILDER
            .comment("Distance per tier. Default is 250.")
            .defineInRange("distancePerTier", 250, 1, 1000);
    public static final ModConfigSpec.DoubleValue healthMultiplierPerTier = BUILDER
            .comment("Health multiplier per tier. Default is 1.25 (25% increase per tier).")
            .defineInRange("healthMultiplierPerTier", 1.25, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue armorMultiplierPerTier = BUILDER
            .comment("Armor multiplier per tier. Default is 1.10 (10% increase per tier).")
            .defineInRange("armorMultiplierPerTier", 1.10, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue attackMultiplierPerTier = BUILDER
            .comment("Attack multiplier per tier. Default is 1.15 (15% increase per tier).")
            .defineInRange("attackMultiplierPerTier", 1.15, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue primarySkillMultiplierPerTier = BUILDER
            .comment("Primary skill multiplier per tier. Default is 1.10 (10% increase per tier).")
            .defineInRange("primarySkillMultiplierPerTier", 1.10, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue secondarySkillMultiplierPerTier = BUILDER
            .comment("Secondary skill multiplier per tier. Default is 1.10 (10% increase per tier).")
            .defineInRange("secondarySkillMultiplierPerTier", 1.10, 0.0, 10.0);
    public static final ModConfigSpec.EnumValue<ScalingMode> scalingMode = BUILDER
            .comment("Scaling mode for tier multipliers. Default is LOGARITHMIC. LOGARITHMIC provides diminishing returns, EXPONENTIAL provides increasing returns, and LINEAR provides consistent scaling.")
            .defineEnum("scalingMode", ScalingMode.LOGARITHMIC);
    public static final ModConfigSpec.DoubleValue logBase = BUILDER
            .comment("Base for logarithmic scaling (if LOGARITHMIC mode is selected). Default is 5.0.")
            .defineInRange("logBase", 5.0, 1.0, 100.0);

    public static final ModConfigSpec SPEC = BUILDER.build();
}

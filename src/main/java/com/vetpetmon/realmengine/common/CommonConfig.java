package com.vetpetmon.realmengine.common;

import net.minecraftforge.common.ForgeConfigSpec;

public class CommonConfig {

    public static ForgeConfigSpec.IntValue noTieringZoneRadius, distancePerTier;
    public static ForgeConfigSpec.DoubleValue healthMultiplierPerTier, attackMultiplierPerTier, armorMultiplierPerTier, primarySkillMultiplierPerTier, secondarySkillMultiplierPerTier, logBase;
    // Enum for scaling mode (log, exponential, linear)
    public enum ScalingMode {
        LOGARITHMIC,
        EXPONENTIAL,
        LINEAR
    }
    public static ForgeConfigSpec.EnumValue<ScalingMode> scalingMode;
    public static ForgeConfigSpec.BooleanValue disableNetherPortal, disableEndPortal, tieringEnabled;

    public CommonConfig(ForgeConfigSpec.Builder builder) {
        // DAM stopped working for us, so we wrote our own dimensional access system.
        builder.push("Dimension Control");
        disableNetherPortal = builder
                .comment("Disable Nether Portal functionality (entities won't be able to use them). Default is false.")
                .define("disableNetherPortal", false);
        disableEndPortal = builder
                .comment("Disable End Portal functionality (entities won't be able to use them). Default is false.")
                .define("disableEndPortal", false);
        builder.pop();

        builder.push("Tiered Mobs");
        tieringEnabled = builder
                .comment("Enable tiered mob scaling based on distance from world spawn. Default is true.")
                .define("tieringEnabled", true);
        noTieringZoneRadius = builder
            .comment("Distance before tiering starts, to make starting areas far less dangerous. Default is 500.")
            .defineInRange("noTieringZoneRadius", 500, 1, 1000);
        distancePerTier = builder
                .comment("Distance per tier. Default is 250.")
                .defineInRange("distancePerTier", 250, 1, 1000);
        healthMultiplierPerTier = builder
                .comment("Health multiplier per tier. Default is 1.25 (25% increase per tier).")
                .defineInRange("healthMultiplierPerTier", 1.25, 0.0, 10.0);
        armorMultiplierPerTier = builder
                .comment("Armor multiplier per tier. Default is 1.10 (10% increase per tier).")
                .defineInRange("armorMultiplierPerTier", 1.10, 0.0, 10.0);
        attackMultiplierPerTier = builder
                .comment("Attack multiplier per tier. Default is 1.15 (15% increase per tier).")
                .defineInRange("attackMultiplierPerTier", 1.15, 0.0, 10.0);
        primarySkillMultiplierPerTier = builder
                .comment("Primary skill multiplier per tier. Default is 0.10 (10% increase per tier).")
                .defineInRange("primarySkillMultiplierPerTier", 1.10, 0.0, 10.0);
        secondarySkillMultiplierPerTier = builder
                .comment("Secondary skill multiplier per tier. Default is 0.10 (10% increase per tier).")
                .defineInRange("secondarySkillMultiplierPerTier", 1.10, 0.0, 10.0);
        scalingMode = builder
                .comment("Scaling mode for tier multipliers. Default is LOGARITHMIC. LOGARITHMIC provides diminishing returns, EXPONENTIAL provides increasing returns, and LINEAR provides consistent scaling.")
                .defineEnum("scalingMode", ScalingMode.LOGARITHMIC);
        logBase = builder
                .comment("Base for logarithmic scaling (if LOGARITHMIC mode is selected). Default is 2.0.")
                .defineInRange("logBase", 5.0, 1, 100.0);
        builder.pop();
    }
}

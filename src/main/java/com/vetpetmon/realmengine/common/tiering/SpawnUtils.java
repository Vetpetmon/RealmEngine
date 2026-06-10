package com.vetpetmon.realmengine.common.tiering;

import com.vetpetmon.realmengine.common.CommonConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Objects;

public class SpawnUtils {
    // Compare distance from spawn (USUALLY 0,0) to current position)
    // Does not care about Y level, only X and Z.
    // Used often to determine if an entity should spawn with certain attributes or not.
    public static long distanceFromSpawn(BlockPos position, ServerLevel world) {
        long     spawnX = world.getSharedSpawnPos().getX(),
                spawnZ = world.getSharedSpawnPos().getZ(),
                deltaX = position.getX() - spawnX,
                deltaZ = position.getZ() - spawnZ;
        // if X = 1000, Z = 1000, distance = sqrt(1000^2 + 1000^2) = 1414
        return (long) Math.sqrt(Math.abs(deltaX * deltaX) + Math.abs(deltaZ * deltaZ));
    }

    /**
     * Using distance from spawn, use stepped distances to determine a "tier" for spawning.
     * @param position Current position to check distance from spawn.
     * @param world Server world.
     * @param step Distance step for each tier. For example, if step = 500, then:
     *             0-499 = tier 0;
     *             500-999 = tier 1;
     *             1000-1499 = tier 2;
     *             etc.
     * @return Tier number based on distance from spawn and step.
     */
    public static int spawnTier(BlockPos position, ServerLevel world, int step) {
        return (int) Mth.clamp(distanceFromSpawn(position, world),0,Integer.MAX_VALUE) / step; // Ensure we don't overflow or underflow
    }

    // Given a tier, return a multiplier for scaling attributes. Use logarithmic scaling.
    // Tier 0 = 1.0, Tier 1 = 1.69, Tier 2 = 2.09, Tier 3 = 2.39, Tier 4 = 2.61, Tier 5 = 2.79, etc.
    // This ensures that higher tiers have diminishing returns, preventing excessive scaling.
    public static double tierMultiplier(int tier) {
        return Math.log1p(tier/7.0d) + 1; // log1p ensures tier 0 returns 1.0
    }

    // Scales the Mob's health attribute based on its spawn tier.
    public static void scaleHealthByTier(LivingEntity entity, int tier, double baseHealth) {
        Objects.requireNonNull(entity.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(baseHealth * SpawnUtils.tierMultiplier(tier));
        entity.setHealth(entity.getMaxHealth());
    }

    // Scales the Mob's attack damage attribute based on its spawn tier.
    public static void scaleAttackByTier(LivingEntity entity, int tier, double baseAttack) {
        Objects.requireNonNull(entity.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(baseAttack * (SpawnUtils.tierMultiplier(tier)*0.75));
    }

    // Scales the Mob's armor attribute based on its spawn tier.
    public static void scaleArmorByTier(LivingEntity entity, int tier, double baseArmor) {
        Objects.requireNonNull(entity.getAttribute(Attributes.ARMOR)).setBaseValue(baseArmor * (SpawnUtils.tierMultiplier(tier)/2));
    }

    // Scales the Mob's Primary Skill Strength attribute based on its spawn tier.
    public static void scalePrimarySkillByTier(LivingEntity entity, int tier, double basePrimarySkill) {
        Attribute attr = TieringRegistry.getPrimarySkillStrengthAttribute();
        if (attr != null) {
            Objects.requireNonNull(entity.getAttribute(attr)).setBaseValue(basePrimarySkill * SpawnUtils.tierMultiplier(tier));
        }
    }

    // Scales the Mob's Secondary Skill Strength attribute based on its spawn tier.
    public static void scaleSecondarySkillByTier(LivingEntity entity, int tier, double baseSecondarySkill) {
        Attribute attr = TieringRegistry.getSecondarySkillStrengthAttribute();
        if (attr != null) {
            Objects.requireNonNull(entity.getAttribute(attr)).setBaseValue(baseSecondarySkill * SpawnUtils.tierMultiplier(tier));
        }
    }


    // Get players near a position within a certain radius.
    public static int getPlayersNearPosition(ServerLevel world, BlockPos position, int radius) {
        return (int) world.players().stream()
                .filter(player -> player.gameMode.getGameModeForPlayer().isSurvival() && player.distanceToSqr(position.getX(), position.getY(), position.getZ()) <= radius * radius)
                .count();
    }

    /**
     * Combines spawn tier based on distance from spawn and number of players nearby to determine final spawn tier. Recommended for boss scaling.
     * <p>
     *     Each player within the specified radius increases the tier by 1, up to a maximum of maxPlayers.
     *     This encourages more challenging spawns in areas with higher player activity.
     * </p>
     * @param position Current position to check distance from spawn.
     * @param world Server world.
     * @param step Distance step for each tier.
     * @param playerRadius Radius to check for players around the position.
     * @param maxPlayers Maximum number of players to consider for tier increase.
     * @return Final tier number based on distance from spawn and nearby players.
     */
    public static int spawnTierWithPlayers(BlockPos position, ServerLevel world, int step, int playerRadius, int maxPlayers) {
        int baseTier = spawnTier(position, world, step);
        int playerCount = Mth.clamp(getPlayersNearPosition(world, position, playerRadius),0,maxPlayers);
        return baseTier + playerCount; // Each player nearby increases the tier by 1
    }

    // Given a tier, return a multiplier for scaling attributes. Use logarithmic scaling with a scale factor.
    public static double tierMultiplierScalable(int tier, float scale) {
        return (Math.log1p(tier/7.0d) * scale) + 1; // log1p ensures tier 0 returns 1.0
    }

    // Scales the Mob's health attribute based on its spawn tier.
    public static void scaleHealthByTier(LivingEntity entity, int tier, double baseHealth, float scale) {
        Objects.requireNonNull(entity.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(baseHealth * SpawnUtils.tierMultiplierScalable(tier,scale));
        entity.setHealth(entity.getMaxHealth());
    }

    // Scales the Mob's attack damage attribute based on its spawn tier.
    public static void scaleAttackByTier(LivingEntity entity, int tier, double baseAttack, float scale) {
        Objects.requireNonNull(entity.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(baseAttack * (SpawnUtils.tierMultiplierScalable(tier,scale)*0.75));
    }

    // Scales the Mob's armor attribute based on its spawn tier.
    public static void scaleArmorByTier(LivingEntity entity, int tier, double baseArmor, float scale) {
        Objects.requireNonNull(entity.getAttribute(Attributes.ARMOR)).setBaseValue(baseArmor * (SpawnUtils.tierMultiplierScalable(tier,scale)/2));
    }

    // ==================== CONFIG-AWARE SCALING METHODS ====================

    /**
     * Calculate tier based on distance from spawn using config values.
     * Takes into account the no-tiering zone radius from config.
     *
     * @param position Current position
     * @param world Server world
     * @return Tier number (0 if within no-tiering zone)
     */
    public static int spawnTierFromConfig(BlockPos position, ServerLevel world) {
        long distance = distanceFromSpawn(position, world);
        long noTieringZone = CommonConfig.noTieringZoneRadius.get();
        
        if (distance < noTieringZone) {
            return 0;
        }
        
        int distancePerTier = CommonConfig.distancePerTier.get();
        return (int) ((distance - noTieringZone) / distancePerTier) + 1;
    }

    /**
     * Calculate tier multiplier using configured scaling mode.
     * Supports LOGARITHMIC, EXPONENTIAL, and LINEAR scaling.
     *
     * @param tier The tier number
     * @return Multiplier based on configured scaling mode
     */
    public static double tierMultiplierFromConfig(int tier) {
        CommonConfig.ScalingMode mode = CommonConfig.scalingMode.get();
        
        return switch (mode) {
            case LINEAR -> 1.0d + (tier * 0.1d); // 10% per tier
            case EXPONENTIAL -> Math.pow(1.15d, tier); // 15% compound
            case LOGARITHMIC -> (Math.log1p(tier / CommonConfig.logBase.get()) / Math.log1p(10.0d / CommonConfig.logBase.get())) + 1.0d;
        };
    }

    /**
     * Apply config-based scaling to all standard attributes of an entity.
     * Uses current attribute values as base and applies multipliers from config.
     * Only scales attributes that actually exist on the entity.
     * Respects the configured scaling mode (LOGARITHMIC, LINEAR, EXPONENTIAL).
     *
     * @param entity The entity to scale
     * @param tier The tier to apply
     */
    public static void applyConfigScaling(LivingEntity entity, int tier) {
        if (tier <= 0) return; // No scaling for tier 0
        CompoundTag tag = entity.getPersistentData();
        if (tag.getBoolean("realmengine_scaled")) return;

        // Get the base tier multiplier using the configured scaling mode
        double tierMultiplier = tierMultiplierFromConfig(tier);
        
        double healthMultiplier = CommonConfig.healthMultiplierPerTier.get();
        double attackMultiplier = CommonConfig.attackMultiplierPerTier.get();
        double armorMultiplier = CommonConfig.armorMultiplierPerTier.get();

        // Scale health (always exists)
        var healthAttr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            // Use the attribute's base value (original base) instead of the current effective value
            double baseHealth = healthAttr.getBaseValue();
            double scaledHealth = baseHealth * CommonConfig.healthMultiplierPerTier.get() * tierMultiplier;
            healthAttr.setBaseValue(scaledHealth);
            entity.setHealth((float) Math.min(entity.getHealth(), scaledHealth));
        }

        // Scale attack damage (may not exist for all entities)
        var attackAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            double baseAttack = attackAttr.getBaseValue();
            double scaledAttack = baseAttack * CommonConfig.attackMultiplierPerTier.get() * tierMultiplier;
            attackAttr.setBaseValue(scaledAttack);
        }

        // Scale armor (may not exist for all entities)
        var armorAttr = entity.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            double baseArmor = armorAttr.getBaseValue();
            double scaledArmor = baseArmor * CommonConfig.armorMultiplierPerTier.get() * tierMultiplier;
            armorAttr.setBaseValue(scaledArmor);
        }
        
        // Scale skill attributes if available
        Attribute primarySkill = TieringRegistry.getPrimarySkillStrengthAttribute();
        if (primarySkill != null) {
            var primaryAttr = entity.getAttribute(primarySkill);
            if (primaryAttr != null) {
                double primaryMultiplier = CommonConfig.primarySkillMultiplierPerTier.get();
                double currentPrimary = entity.getAttributeValue(primarySkill);
                double scaledPrimary = currentPrimary * primaryMultiplier * tierMultiplier;
                primaryAttr.setBaseValue(scaledPrimary);
            }
        }
        
        Attribute secondarySkill = TieringRegistry.getSecondarySkillStrengthAttribute();
        if (secondarySkill != null) {
            var secondaryAttr = entity.getAttribute(secondarySkill);
            if (secondaryAttr != null) {
                double secondaryMultiplier = CommonConfig.secondarySkillMultiplierPerTier.get();
                double currentSecondary = entity.getAttributeValue(secondarySkill);
                double scaledSecondary = currentSecondary * secondaryMultiplier * tierMultiplier;
                secondaryAttr.setBaseValue(scaledSecondary);
            }
        }
        tag.putBoolean("realmengine_scaled", true);
    }
}




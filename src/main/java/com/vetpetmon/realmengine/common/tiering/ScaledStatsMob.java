package com.vetpetmon.realmengine.common.tiering;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Base interface for entities with scaled stats based on spawn tier.
 * Allows mods to define how entity attributes should scale based on distance from spawn.
 */
public interface ScaledStatsMob {

    /**
     * Scales the entity's attributes based on spawn tier (player-conscious variant).
     * 
     * @param entity The entity to scale
     * @param worldIn Server world
     * @param baseHealth Entity's base health value
     * @param baseAttack Entity's base attack damage
     * @param baseArmor Entity's base armor value
     * @param basePrimarySkill Entity's base primary skill strength
     * @param baseSecondarySkill Entity's base secondary skill strength
     * @param step Distance step for each tier
     * @param scale Scaling factor for attribute scaling
     * @param playerRadius Radius to check for players around the position
     * @param maxPlayers Maximum number of players to consider for tier increase
     * @return The tier the entity spawned at
     */
    @SuppressWarnings("unused")
    default int tieredSpawnPlayerConsciousScaled(LivingEntity entity, ServerLevelAccessor worldIn, 
            double baseHealth, double baseAttack, double baseArmor, 
            double basePrimarySkill, double baseSecondarySkill,
            int step, float scale, int playerRadius, int maxPlayers) {
        int tier = 0;
        if (worldIn instanceof Level level && level.getServer() != null)
            tier = SpawnUtils.spawnTierWithPlayers(entity.blockPosition(), worldIn.getLevel(), step, playerRadius, maxPlayers);
        SpawnUtils.scaleHealthByTier(entity, tier, baseHealth, scale);
        SpawnUtils.scaleAttackByTier(entity, tier, baseAttack, scale);
        SpawnUtils.scaleArmorByTier(entity, tier, baseArmor, scale);
        return tier;
    }

    /**
     * Scales the entity's attributes based on spawn tier (scaled variant).
     * 
     * @param entity The entity to scale
     * @param worldIn Server world
     * @param baseHealth Entity's base health value
     * @param baseAttack Entity's base attack damage
     * @param baseArmor Entity's base armor value
     * @param step Distance step for each tier
     * @param scale Scaling factor for attribute scaling
     * @return The tier the entity spawned at
     */
    default int tieredSpawnScaled(LivingEntity entity, ServerLevelAccessor worldIn,
            double baseHealth, double baseAttack, double baseArmor,
            int step, float scale) {
        int tier = 0;
        if (worldIn instanceof Level level && level.getServer() != null)
            tier = SpawnUtils.spawnTier(entity.blockPosition(), worldIn.getLevel(), step);
        SpawnUtils.scaleHealthByTier(entity, tier, baseHealth, scale);
        SpawnUtils.scaleAttackByTier(entity, tier, baseAttack, scale);
        SpawnUtils.scaleArmorByTier(entity, tier, baseArmor, scale);
        return tier;
    }

    /**
     * Scales the entity's attributes based on spawn tier (standard variant).
     * Includes skill attributes if available via TieringRegistry.
     * 
     * @param entity The entity to scale
     * @param worldIn Server world
     * @param baseHealth Entity's base health value
     * @param baseAttack Entity's base attack damage
     * @param baseArmor Entity's base armor value
     * @param basePrimarySkill Entity's base primary skill strength
     * @param baseSecondarySkill Entity's base secondary skill strength
     * @param step Distance step for each tier
     * @return The tier the entity spawned at
     */
    default int tieredSpawn(LivingEntity entity, ServerLevelAccessor worldIn,
            double baseHealth, double baseAttack, double baseArmor,
            double basePrimarySkill, double baseSecondarySkill,
            int step) {
        int tier = 0;
        if (worldIn instanceof Level level && level.getServer() != null)
            tier = SpawnUtils.spawnTier(entity.blockPosition(), worldIn.getLevel(), step);
        SpawnUtils.scaleHealthByTier(entity, tier, baseHealth);
        SpawnUtils.scaleAttackByTier(entity, tier, baseAttack);
        SpawnUtils.scaleArmorByTier(entity, tier, baseArmor);
        SpawnUtils.scalePrimarySkillByTier(entity, tier, basePrimarySkill);
        SpawnUtils.scaleSecondarySkillByTier(entity, tier, baseSecondarySkill);
        return tier;
    }
}




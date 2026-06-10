package com.vetpetmon.realmengine.common.tiering;

import com.vetpetmon.realmengine.common.CommonConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Automatic entity scaling listener that applies tiering to any LivingEntity
 * that doesn't already implement ScaledStatsMob.
 * This listener uses values from CommonConfig to determine tier and apply scaling.
 */
@Mod.EventBusSubscriber(modid = "realmengine", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EntityTieringEventListener {

    /**
     * Apply automatic tiering to entities when they join a level.
     * Skips entities that already implement ScaledStatsMob to avoid double-scaling.
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || !CommonConfig.tieringEnabled.get()) {
            return;
        }

        // Skip if already implementing ScaledStatsMob (handled elsewhere)
        if (entity instanceof ScaledStatsMob) {
            return;
        }

        // Only process on server side and in ServerLevel
        if (!(event.getLevel() instanceof ServerLevel world)) {
            return;
        }

        // Skip players
        if (entity instanceof Player) {
            return;
        }

        // Calculate tier based on distance from spawn
        int tier = SpawnUtils.spawnTierFromConfig(entity.blockPosition(), world);

        // Apply config-based scaling
        if (tier > 0) {
            SpawnUtils.applyConfigScaling(entity, tier);
            // Set entity's health to new max health after scaling
            double newMaxHealth = entity.getAttributeValue(Attributes.MAX_HEALTH);
            entity.heal((float) newMaxHealth);
            // Add NBT tag with entity tier (for boosting loot tables and other mechanics)
            entity.getPersistentData().putInt("realmengine_tier", tier);
        }
    }
}




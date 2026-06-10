package com.vetpetmon.realmengine.common.tiering.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import org.jetbrains.annotations.NotNull;

/**
 * Loot condition that checks if an entity has a tier value within a specified range.
 * Used in loot tables to conditionally apply drops based on entity tier.
 * Example in loot table JSON:
 * {
 *   "condition": "realmengine:tier",
 *   "min": 4,
 *   "max": 99
 * }
 */
public record TierCondition(int min, int max) implements LootItemCondition {

    public static final Codec<TierCondition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.optionalFieldOf("min", 0).forGetter(TierCondition::min),
                    Codec.INT.optionalFieldOf("max", 9999).forGetter(TierCondition::max)
            ).apply(instance, TierCondition::new)
    );

    @Override
    public @NotNull LootItemConditionType getType() {
        return LootConditions.TIER.get();
    }

    @Override
    public boolean test(LootContext lootContext) {
        Entity entity = lootContext.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (entity == null) {
            return false;
        }

        // Check for the realmengine_tier NBT tag on the entity
        int tier = entity.getPersistentData().getInt("realmengine_tier");

        // Check if tier is within the specified range
        return tier >= this.min && tier <= this.max;
    }
}



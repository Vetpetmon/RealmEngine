package com.vetpetmon.realmengine.common.tiering.loot;

import com.mojang.serialization.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.stream.Stream;

/**
 * Registry for RealmEngine's custom loot conditions.
 * Handles registration of loot conditions to the Forge registry.
 */
public class LootConditions {
    public static final ResourceKey<Registry<LootItemConditionType>> REGISTRY_KEY = Registries.LOOT_CONDITION_TYPE;
    public static final DeferredRegister<LootItemConditionType> LOOT_CONDITIONS = 
            DeferredRegister.create(REGISTRY_KEY, "realmengine");

    // Register the tier condition
    // updated to Neoforge 1.21.1, hopefully
    public static final DeferredHolder<LootItemConditionType, LootItemConditionType> TIER = LOOT_CONDITIONS.register("tier",
            () -> new LootItemConditionType(new MapCodec<TierCondition>() { //This is so amazingly jank Im surprised if it works 1:1
                @Override
                public <T> RecordBuilder<T> encode(TierCondition input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                    return prefix.add("min", ops.createInt(input.min()))
                                 .add("max", ops.createInt(input.max()));
                }

                @Override
                public <T> DataResult<TierCondition> decode(DynamicOps<T> ops, MapLike<T> input) {
                    // Using hashCode as a workaround for flatMap not being available
                    int min, max;
                    if (input.get("min") == null) min = 0;
                    else min = ops.getNumberValue(input.get("min")).map(Number::intValue).result().orElse(0);

                    if (input.get("max") == null) max = 9999;
                    else max = ops.getNumberValue(input.get("max")).map(Number::intValue).result().orElse(9999);
                    return DataResult.success(new TierCondition(min, max));
                }

                @Override
                public <T> Stream<T> keys(DynamicOps<T> ops) {
                    return Stream.of(ops.createString("min"), ops.createString("max"));
                }
            }));


    /**
     * Register the loot conditions with the mod event bus.
     * Call this during mod initialization (in the mod's constructor).
     * 
     * @param modEventBus The mod's event bus
     */
    public static void register(IEventBus modEventBus) {
        LOOT_CONDITIONS.register(modEventBus);
    }
}




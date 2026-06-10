package com.vetpetmon.realmengine.common.tiering.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.Serializer;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

/**
 * Registry for RealmEngine's custom loot conditions.
 * Handles registration of loot conditions to the Forge registry.
 */
public class LootConditions {
    public static final ResourceKey<Registry<LootItemConditionType>> REGISTRY_KEY = Registries.LOOT_CONDITION_TYPE;
    public static final DeferredRegister<LootItemConditionType> LOOT_CONDITIONS = 
            DeferredRegister.create(REGISTRY_KEY, "realmengine");

    // Register the tier condition
    public static final RegistryObject<LootItemConditionType> TIER = LOOT_CONDITIONS.register("tier", 
            () -> new LootItemConditionType(new Serializer<TierCondition>() {
                @Override
                public void serialize(@NotNull JsonObject jsonObject, @NotNull TierCondition tierCondition, @NotNull JsonSerializationContext jsonSerializationContext) {
                    jsonObject.addProperty("min", tierCondition.min());
                    jsonObject.addProperty("max", tierCondition.max());
                }

                @Override
                public @NotNull TierCondition deserialize(@NotNull JsonObject jsonObject, @NotNull JsonDeserializationContext jsonDeserializationContext) {
                    int min = jsonObject.has("min") ? jsonObject.get("min").getAsInt() : 0;
                    int max = jsonObject.has("max") ? jsonObject.get("max").getAsInt() : 9999;
                    return new TierCondition(min, max);
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




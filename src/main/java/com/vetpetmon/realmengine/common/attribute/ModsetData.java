package com.vetpetmon.realmengine.common.attribute;

import com.vetpetmon.realmengine.RealmEngine;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Data structure for modsets loaded from JSON.
 * Modsets define random-value attribute modifiers that can be applied to gear.
 */
public class ModsetData {

    // Global storage: namespace -> modset_id -> Modset
    public static final Map<String, Map<String, Modset>> MODSETS = new ConcurrentHashMap<>();

    /**
     * A modset contains a list of modifier definitions with random value ranges.
     */
    public static class Modset {
        public final String id;
        public final List<ModifierDefinition> modifiers;
        private RealmEngine.Weekday boostedStatDay = RealmEngine.Weekday.NONE;
        private double boostedStatRate = 1.0;

        public Modset(String id) {
            this.id = id;
            this.modifiers = new ArrayList<>();
        }

        /**
         * Add a modifier definition to this modset
         */
        public void addModifier(ModifierDefinition modifier) {
            this.modifiers.add(modifier);
        }

        /**
         * Get all modifier definitions
         */
        public List<ModifierDefinition> getModifiers() {return modifiers;}
        public RealmEngine.Weekday getBoostedStatDay() {return boostedStatDay;}
        public void setBoostedStatDay(RealmEngine.Weekday boostedStatDay) {this.boostedStatDay = boostedStatDay != null ? boostedStatDay : RealmEngine.Weekday.NONE;}
        public double getBoostedStatRate() {return boostedStatRate;}
        public void setBoostedStatRate(double boostedStatRate) {this.boostedStatRate = boostedStatRate;}

        /**
         * Create RealmfallAttributeMod instances with randomized values for all modifiers in this set.
         * Each call will generate new random values within the defined ranges.
         */
        public List<RealmEngineAttributeMod> generateRandomMods() {
            List<RealmEngineAttributeMod> result = new ArrayList<>();
            for (ModifierDefinition def : modifiers) {
                RealmEngineAttributeMod mod = def.createRandomMod();
                if (mod != null) result.add(mod);
            }
            return result;
        }
    }

    /**
     * Definition of a single modifier with min/max value range
     */
    public static class ModifierDefinition {
        public final String name, attributeName; // Registry name of the attribute
        public final AttributeModifier.Operation operation;
        public final double minValue,maxValue;

        public ModifierDefinition(String name, String attributeName, AttributeModifier.Operation operation, double minValue, double maxValue) {
            this.name = name;
            this.attributeName = attributeName;
            this.operation = operation;
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        /**
         * Create a RealmfallAttributeMod with a random value between min and max.
         * Uses suppliers so the value is captured at creation time but UUID/name are stable.
         */
        public RealmEngineAttributeMod createRandomMod() {
            // Generate random value once
            double randomValue = randomDoubleInRange(minValue, maxValue);

            // Create suppliers that return consistent values
            Supplier<java.util.UUID> uuidSupplier = () -> java.util.UUID.nameUUIDFromBytes((name + ":" + attributeName).getBytes());
            Supplier<String> nameSupplier = () -> name;
            Supplier<Double> valueSupplier = () -> randomValue;

            // Create the appropriate modifier type based on operation
            return switch (operation) {
                case MULTIPLY_BASE -> RealmEngineAttributeMod.createMultiplierModByAttributeName(attributeName, uuidSupplier, nameSupplier, valueSupplier);
                case MULTIPLY_TOTAL -> RealmEngineAttributeMod.createTotalMultiplierModByAttributeName(attributeName, uuidSupplier, nameSupplier, valueSupplier);
                default -> RealmEngineAttributeMod.createFixedModByAttributeName(attributeName, uuidSupplier, nameSupplier, valueSupplier);
            };
        }

        /**
         * Generate a random double within the specified range
         */
        private static double randomDoubleInRange(double min, double max) {return min + (Math.random() * (max - min));}
    }

    /**
     * Lookup a modset by ResourceLocation string (e.g., "bisccel:helmet_set")
     */
    public static Modset getModset(String modsetId) {
        if (modsetId == null || modsetId.isEmpty()) return null;
        ResourceLocation rl = ResourceLocation.tryParse(modsetId);
        if (rl == null) return null;
        Map<String, Modset> namespaceMap = MODSETS.get(rl.getNamespace());
        if (namespaceMap == null) return null;
        return namespaceMap.get(rl.getPath());
    }

    /**
     * Clear all modsets (useful for reload)
     */
    public static void clearAll() {MODSETS.clear();}

    /**
     * Clear modsets for a specific namespace & path
     */
    public static void clearEntry(ResourceLocation resourceLocation) {MODSETS.remove(resourceLocation.toString());}
}

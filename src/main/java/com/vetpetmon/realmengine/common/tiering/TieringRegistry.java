package com.vetpetmon.realmengine.common.tiering;

import net.minecraft.world.entity.ai.attributes.Attribute;
import org.jetbrains.annotations.Nullable;

/**
 * Global registry for tiering system attribute providers.
 * Mods should register their attribute provider during pre-init stage.
 */
@SuppressWarnings("unused")
public class TieringRegistry {

    private static AttributeRegistry attributeRegistry = null;

    /**
     * Register a custom attribute provider.
     * Should be called during pre-init stage of mod initialization.
     *
     * @param registry The attribute registry implementation.
     * @throws IllegalStateException if a registry has already been registered.
     */
    public static void registerAttributeRegistry(AttributeRegistry registry) {
        if (attributeRegistry != null) {
            throw new IllegalStateException("An AttributeRegistry has already been registered!");
        }
        attributeRegistry = registry;
    }

    /**
     * Get the primary skill strength attribute from the registered provider.
     * @return The attribute, or null if no registry is registered or the attribute is not available.
     */
    @Nullable
    public static Attribute getPrimarySkillStrengthAttribute() {
        return attributeRegistry != null ? attributeRegistry.getPrimarySkillStrengthAttribute() : null;
    }

    /**
     * Get the secondary skill strength attribute from the registered provider.
     * @return The attribute, or null if no registry is registered or the attribute is not available.
     */
    @Nullable
    public static Attribute getSecondarySkillStrengthAttribute() {
        return attributeRegistry != null ? attributeRegistry.getSecondarySkillStrengthAttribute() : null;
    }

    /**
     * Check if an attribute registry is registered.
     * @return true if a registry has been registered, false otherwise.
     */
    public static boolean isRegistered() {
        return attributeRegistry != null;
    }

    /**
     * Reset the registry (for testing purposes).
     */
    public static void reset() {
        attributeRegistry = null;
    }
}



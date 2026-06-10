package com.vetpetmon.realmengine.common.tiering;

import net.minecraft.world.entity.ai.attributes.Attribute;

/**
 * Registry interface for custom attributes used by the tiering system.
 * Allows mods to provide custom attributes without hard-coding dependencies.
 */
public interface AttributeRegistry {

    /**
     * Get the primary skill strength attribute.
     * @return The attribute, or null if not registered.
     */
    Attribute getPrimarySkillStrengthAttribute();

    /**
     * Get the secondary skill strength attribute.
     * @return The attribute, or null if not registered.
     */
    Attribute getSecondarySkillStrengthAttribute();
}


package com.vetpetmon.realmengine.common.attribute;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public abstract class RealmEngineAttributeMod {
    private final Attribute attribute;
    private final UUID modiId;
    private final String modiName;
    private final AttributeModifier.Operation operation;

    public RealmEngineAttributeMod(Attribute attribute, UUID modifierId, String modifierName) {
        this.attribute = attribute;
        this.modiId = modifierId;
        this.modiName = modifierName;
        this.operation = AttributeModifier.Operation.ADDITION;
    }

    public RealmEngineAttributeMod(Attribute attribute, UUID modifierId, String modifierName, AttributeModifier.Operation operation) {
        this.attribute = attribute;
        this.modiId = modifierId;
        this.modiName = modifierName;
        this.operation = operation;
    }

    /**
     * Create the AttributeModifier instance
     * @return AttributeModifier
     */
    public AttributeModifier createModifier() {return new AttributeModifier(modiId, modiName, getAmount(), operation);}

    /**
     * Get the attribute this modifier modifies
     * @return Attribute
     */
    public Attribute getAttribute() {return attribute;}

    /**
     * Get the attribute from string
     */
    public static Attribute getAttributeFromString(String attributeString) {
        if (attributeString == null || attributeString.isEmpty()) return null;
        try {
            ResourceLocation rl = ResourceLocation.tryParse(attributeString);
            if (rl == null) return null;
            return ForgeRegistries.ATTRIBUTES.getValue(rl);
        } catch (Exception ex) {
            // Be defensive: on any parsing/lookup error, return null rather than throwing
            return null;
        }
    }

    /**
     * Get the modifier UUID
     * @return UUID of the modifier
     */
    public UUID getModiId() {return modiId;}

    /**
     * Get the modifier name
     * @return String name of the modifier
     */
    public String getModiName() {return modiName;}


    /**
     * Get the amount of the modifier
     * @return double amount
     */
    public abstract double getAmount();

    /**
     * Get the operation of the modifier
     * @return AttributeModifier.Operation
     */
    public AttributeModifier.Operation getOperation() {
        return operation;
    }

    public void onAttributeUpdate(LivingEntity entity) {}

    /**
     * Create a fixed amount modifier
     * @param attribute Attribute
     * @param modifierId UUID
     * @param modifierName String
     * @param amount Supplier<Double>
     * @return RealmfallAttributeMod
     */
    public static RealmEngineAttributeMod createFixedMod(Attribute attribute, Supplier<UUID> modifierId, Supplier<String> modifierName, Supplier<Double> amount) {
        return new RealmEngineAttributeMod(attribute, modifierId.get(), modifierName.get()) {
            public double getAmount() {
                return amount.get();
            }
        };
    }

    /**
     * Get the attribute registry name if available (for lazy-resolving mods).
     * Default implementation returns null.
     * @return ResourceLocation string or null
     */
    public String getAttributeRegistryName() { return null; }

    /** Create fixed mod by attribute registry name; resolves Attribute lazily. */
    public static RealmEngineAttributeMod createFixedModByAttributeName(String attributeRegistryName, Supplier<UUID> modifierId, Supplier<String> modifierName, Supplier<Double> amount) {
        return new RealmEngineAttributeMod(null, modifierId.get(), modifierName.get()) {
            public double getAmount() { return amount.get(); }
            public Attribute getAttribute() { return RealmEngineAttributeMod.getAttributeFromString(attributeRegistryName); }
            public String getAttributeRegistryName() { return attributeRegistryName; }
        };
    }

    /**
     * Create a multiplier modifier, for MULTIPLY_BASE operation
     * @param attribute Attribute
     * @param modifierId UUID
     * @param modifierName String
     * @param amount Supplier<Double>
     * @return RealmfallAttributeMod
     */
    public static RealmEngineAttributeMod createMultiplierMod(Attribute attribute, Supplier<UUID> modifierId, Supplier<String> modifierName, Supplier<Double> amount) {
        return new RealmEngineAttributeMod(attribute, modifierId.get(), modifierName.get(), AttributeModifier.Operation.MULTIPLY_BASE) {
            public double getAmount() {
                return amount.get();
            }
        };
    }

    /** Create multiplier mod by attribute registry name; resolves Attribute lazily. */
    public static RealmEngineAttributeMod createMultiplierModByAttributeName(String attributeRegistryName, Supplier<UUID> modifierId, Supplier<String> modifierName, Supplier<Double> amount) {
        return new RealmEngineAttributeMod(null, modifierId.get(), modifierName.get(), AttributeModifier.Operation.MULTIPLY_BASE) {
             public double getAmount() { return amount.get(); }
             public Attribute getAttribute() { return RealmEngineAttributeMod.getAttributeFromString(attributeRegistryName); }
             public String getAttributeRegistryName() { return attributeRegistryName; }
        };
    }

    /**
     * Create a multiplier modifier, for MULTIPLY_TOTAL operation
     * @param attribute Attribute
     * @param modifierId UUID
     * @param modifierName String
     * @param amount Supplier<Double>
     * @return RealmfallAttributeMod
     */
    public static RealmEngineAttributeMod createTotalMultiplierMod(Attribute attribute, Supplier<UUID> modifierId, Supplier<String> modifierName, Supplier<Double> amount) {
        return new RealmEngineAttributeMod(attribute, modifierId.get(), modifierName.get(), AttributeModifier.Operation.MULTIPLY_TOTAL) {
            public double getAmount() {return amount.get();}
        };
    }

    /** Create total multiplier mod by attribute registry name; resolves Attribute lazily. */
    public static RealmEngineAttributeMod createTotalMultiplierModByAttributeName(String attributeRegistryName, Supplier<UUID> modifierId, Supplier<String> modifierName, Supplier<Double> amount) {
        return new RealmEngineAttributeMod(null, modifierId.get(), modifierName.get(), AttributeModifier.Operation.MULTIPLY_TOTAL) {
             public double getAmount() { return amount.get(); }
             public Attribute getAttribute() { return RealmEngineAttributeMod.getAttributeFromString(attributeRegistryName); }
             public String getAttributeRegistryName() { return attributeRegistryName; }
        };
    }

}

package com.vetpetmon.realmengine.common.armor;

import com.vetpetmon.realmengine.common.attribute.RealmEngineAttributeMod;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * @param mod               the attribute mod this armor mod provides
 * @param armorItemSupplier list of armor items that can have this mod
 */
public record ArmorModPiece(@Nullable RealmEngineAttributeMod mod, Supplier<List<ArmorItem>> armorItemSupplier) {

    private static final String MODIFIER_TAG = "modifiers"; // NBT tag for armor mods

    // Save to NBT tag
    public void saveToTag(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        CompoundTag modsTag = new CompoundTag();
        // Implementation to save modTemp data to modsTag
        RealmEngineAttributeMod modTemp = mod;
        if (modTemp != null) {
            modsTag.putString("mod_name", modTemp.getModiName());
            modsTag.putString("mod_uuid", modTemp.getModiId().toString());
            modsTag.putDouble("mod_amount", modTemp.getAmount());
            // Store the attribute as its registry name (ResourceLocation string)
            String attrName = modTemp.getAttributeRegistryName();
            if (attrName == null || attrName.isEmpty())
                if (modTemp.getAttribute() != null && BuiltInRegistries.ATTRIBUTE.getKey(modTemp.getAttribute()) != null)
                    attrName = Objects.requireNonNull(BuiltInRegistries.ATTRIBUTE.getKey(modTemp.getAttribute())).toString();
            if (attrName != null && !attrName.isEmpty())
                modsTag.putString("mod_attribute", attrName);
        }
        tag.put(MODIFIER_TAG, modsTag);
    }

    // Load from NBT tag
    public static ArmorModPiece loadFromTag(ItemStack stack, Supplier<List<ArmorItem>> armorItemSupplier) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (tag != null && tag.contains(MODIFIER_TAG)) {
            CompoundTag modsTag = tag.getCompound(MODIFIER_TAG);
            // Read mod data from modsTag
            String modName = modsTag.contains("mod_name") ? modsTag.getString("mod_name") : "";
            String modUuidStr = modsTag.contains("mod_uuid") ? modsTag.getString("mod_uuid") : "";
            double modAmount = modsTag.contains("mod_amount") ? modsTag.getDouble("mod_amount") : 0.0;
            String modAttributeStr = modsTag.contains("mod_attribute") ? modsTag.getString("mod_attribute") : "";

            if (modName.isEmpty() || modAttributeStr.isEmpty())
                // Missing essential data
                return new ArmorModPiece(null, armorItemSupplier);

            // Resolve attribute from registry string
            final var attribute = RealmEngineAttributeMod.getAttributeFromString(modAttributeStr);
            RealmEngineAttributeMod rm;
            UUID uuid;
            try {
                uuid = modUuidStr.isEmpty() ? UUID.randomUUID() : UUID.fromString(modUuidStr);
            } catch (IllegalArgumentException ex) {
                uuid = UUID.randomUUID();
            }
            UUID finalUuid = uuid;
            if (attribute == null)
                // Create a mod that will resolve its attribute lazily
                rm = RealmEngineAttributeMod.createFixedModByAttributeName(modAttributeStr, () -> finalUuid, () -> modName, () -> modAmount);
            else
                rm = RealmEngineAttributeMod.createFixedMod((Holder<Attribute>) attribute, () -> finalUuid, () -> modName, () -> modAmount);

            return new ArmorModPiece(rm, armorItemSupplier);
        }
        return new ArmorModPiece(null, armorItemSupplier);
    }

    // Can apply to given armor item
    public boolean canApplyTo(ArmorItem armorItem) {
        List<ArmorItem> applicableItems = armorItemSupplier.get();
        if (applicableItems == null) return false;
        for (ArmorItem candidate : applicableItems) {
            if (candidate == null) continue;
            // Direct instance match
            if (candidate == armorItem) return true;
            // Registry name match (preferred, robust)
            ResourceLocation k1 = BuiltInRegistries.ITEM.getKey(candidate), k2 = BuiltInRegistries.ITEM.getKey(armorItem);
            if (k1 != null && k1.equals(k2)) return true;
            // Fallback: compare description id (less strict than registry but safer than class equality)
            if (candidate.getDescriptionId().equals(armorItem.getDescriptionId())) return true;
        }
        return false;
    }


}

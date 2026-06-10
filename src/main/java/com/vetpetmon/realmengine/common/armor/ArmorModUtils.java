package com.vetpetmon.realmengine.common.armor;

import com.vetpetmon.realmengine.common.attribute.RealmEngineAttributeMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@SuppressWarnings("unused")
public class ArmorModUtils {
    // Use "gear_mods" to match IRandomizedGear.MODS_TAG and integrate with GearCapability
    public static String MODS_TAG = "gear_mods";

    /**
     * Read ArmorModPiece(s) applicable to the given ArmorItem from the ItemStack's NBT.
     */
    public static List<ArmorModPiece> readArmorModsForArmorItem(ArmorItem armorItem, ItemStack stack) {
        // Legacy flow: behaviors saved by ArmorModPiece.saveToTag() under "modifiers"
        ArmorModPiece piece = ArmorModPiece.loadFromTag(stack, () -> List.of(armorItem));
        if (piece.mod() != null) {
            if (!piece.canApplyTo(armorItem)) return List.of();
            return List.of(piece);
        }

        // If legacy data empty, try datapack-driven marker stored under modifiers -> mod_item_id
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("modifiers")) {
            CompoundTag modsTag = tag.getCompound("modifiers");
            if (modsTag.contains("mod_item_id")) {
                String modId = modsTag.getString("mod_item_id");
                if (!modId.isEmpty()) {
                    // Attempt to locate the registered Item for this mod id
                    ResourceLocation rl = ResourceLocation.tryParse(modId);
                    if (rl != null) {
                        Item modItem = ForgeRegistries.ITEMS.getValue(rl);
                        if (modItem instanceof ItemApplicableArmorMod iam) {
                            // If the mod item was registered with an ArmorModPiece, use that
                            ArmorModPiece legacyPiece = iam.getArmorModPiece();
                            if (legacyPiece != null && legacyPiece.mod() != null && legacyPiece.canApplyTo(armorItem)) return List.of(legacyPiece);
                        }
                        // Create a placeholder ArmorModPiece with no explicit attribute mod but with the single armorItem supplier
                        ArmorModPiece placeholder = new ArmorModPiece(null, () -> List.of(armorItem));
                        return List.of(placeholder);
                    }
                }
            }
        }

        return List.of();
    }

    /**
     * Apply armor mod data (stored under "modifiers") into the armor_mods tag.
     * This keeps armor mods separate from randomized gear_mods.
     * gear_mods = randomized modifiers from modsets
     * armor_mods = applied armor mod piece modifiers
     */
    public static void applyArmorModsToStack(ItemStack stack, ArmorItem armorItem) {
        List<ArmorModPiece> pieces = readArmorModsForArmorItem(armorItem, stack);
        CompoundTag tag = stack.getOrCreateTag();

        // Create armor_mods tag (separate from gear_mods)
        CompoundTag armorMods = new CompoundTag();

        // First handle legacy ArmorModPiece
        for (ArmorModPiece p : pieces) {
            RealmEngineAttributeMod mod = p.mod();
            if (mod == null) continue;
            String key = mod.getModiName(); // Use mod name for armor_mods
            armorMods.putDouble(key, mod.getAmount());
        }

        // Handle datapack-driven mod marker
        CompoundTag modifiers = tag.contains("modifiers") ? tag.getCompound("modifiers") : null;
        if (modifiers != null && modifiers.contains("mod_item_id")) {
            String modId = modifiers.getString("mod_item_id");

            // Find the ArmorMod definition for this specific armor item
            ArmorPropertiesData.ArmorMod armorMod = findArmorModForItem(modId, armorItem);
            if (armorMod != null && armorMod.getModEffects() != null) {
                // Check if we have an armor_mods compound with effect names
                if (tag.contains("armor_mods")) {
                    CompoundTag existingArmorMods = tag.getCompound("armor_mods");

                    // Iterate through stored effect names
                    for (String effectName : existingArmorMods.getAllKeys()) {
                        // Look up the effect definition from ArmorPropertiesData
                        RealmEngineAttributeMod effectMod = armorMod.getModEffects().get(effectName);
                        if (effectMod != null)
                            // Use effect name (not internal mod name)
                            armorMods.putDouble(effectName, effectMod.getAmount());
                    }
                } else {
                    // No armor_mods compound, apply all effects from definition
                    for (var entry : armorMod.getModEffects().entrySet()) {
                        String effectName = entry.getKey(); // Use effect name from JSON
                        RealmEngineAttributeMod ram = entry.getValue();
                        if (ram != null) armorMods.putDouble(effectName, ram.getAmount());
                    }
                }
            }
        }

        // Store in armor_mods tag (NOT gear_mods)
        if (!armorMods.isEmpty()) tag.put("armor_mods", armorMods);
        else tag.remove("armor_mods");

        // Note: gear_mods is left untouched - it contains randomized modifiers
    }

    /**
     * Search for an applied armor mod name in the ItemStack's NBT.
     * @param stack The ItemStack to check
     * @param modName The armor mod name to search for
     * @return true if found, false otherwise
     */
    public static boolean hasArmorModByName(ItemStack stack, String modName) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("modifiers")) {
            CompoundTag modsTag = tag.getCompound("modifiers");
            if (modsTag.contains("mod_name")) {
                String storedModName = modsTag.getString("mod_name");
                return storedModName.equals(modName);
            }
            if (modsTag.contains("mod_item_id")) {
                // If mod_item_id is present, attempt to resolve the registered item and check its ArmorModPiece
                String modId = modsTag.getString("mod_item_id");
                ResourceLocation rl = ResourceLocation.tryParse(modId);
                if (rl != null) {
                    Item item = ForgeRegistries.ITEMS.getValue(rl);
                    if (item instanceof ItemApplicableArmorMod iam) {
                        ArmorModPiece ap = iam.getArmorModPiece();
                        if (ap != null && ap.mod() != null) return ap.mod().getModiName().equals(modName);
                    }
                }
            }
        }
        return false;
    }

    /**
     * Find the ArmorMod definition for a given mod item ID that applies to the specific armor item.
     * If armorItem is null, returns the first match (legacy behavior).
     */
    public static ArmorPropertiesData.ArmorMod findArmorModForItem(String modItemId, ArmorItem armorItem) {
        if (modItemId == null || modItemId.isEmpty()) return null;

        for (var ns : ArmorPropertiesData.ARMOR_PROPERTIES.values()) {
            for (var props : ns.values()) {
                if (props.applicableMods == null) continue;
                for (var am : props.applicableMods) {
                    if (modItemId.equals(am.getItemID())) {
                        // If armorItem is provided, check if this ArmorMod applies to it
                        if (armorItem != null) {
                            // Check if this armor item is in the armor properties' armor items list
                            if (props.armorItems != null && props.armorItems.contains(armorItem)) return am; // Found the correct ArmorMod for this specific armor
                        } else return am; // No armor item specified, return first match (legacy)
                    }
                }
            }
        }
        return null;
    }

    /**
     * Legacy method for backward compatibility - finds first match without armor item check
     */
    public static ArmorPropertiesData.ArmorMod findArmorModForItem(String modItemId) {return findArmorModForItem(modItemId, null);}

    /**
     * Check if an ArmorMod can be applied to a specific ArmorItem
     * Uses the same matching logic as ItemApplicableArmorMod.use()
     * Priority order:
     * 1. Check slots if defined (most reliable for datapack-driven mods)
     * 2. Check applicableItems list (for specific item matches)
     */
    public static boolean canApplyModToArmor(ArmorPropertiesData.ArmorMod armorMod, ArmorItem targetArmor) {
        if (armorMod == null || targetArmor == null) return false;

        // PRIMARY: Check slots if defined - this is the most reliable check for datapack mods
        if (armorMod.getSlots() != null && !armorMod.getSlots().isEmpty()) {
            var targetSlot = ArmorPropertiesData.ArmorMod.fromEquipmentSlot(targetArmor.getEquipmentSlot());
            if (targetSlot != null && armorMod.getSlots().contains(targetSlot)) return true;
        }

        // SECONDARY: Check applicableItems if present (specific item whitelist)
        if (armorMod.getApplicableItems() != null && !armorMod.getApplicableItems().isEmpty()) {
            for (Item it : armorMod.getApplicableItems()) {
                if (!(it instanceof ArmorItem ai)) continue;
                // Direct instance match
                if (ai == targetArmor) return true;
                // Registry key match
                ResourceLocation k1 = ForgeRegistries.ITEMS.getKey(ai), k2 = ForgeRegistries.ITEMS.getKey(targetArmor);
                if (k1 != null && k1.equals(k2)) return true;
                // Description ID match (important for modded armor)
                if (ai.getDescriptionId().equals(targetArmor.getDescriptionId())) return true;
            }
            // If applicableItems is defined but target doesn't match, only reject if slots weren't checked
            if (armorMod.getSlots() == null || armorMod.getSlots().isEmpty()) return false;
        }

        // If we get here and slots were defined and matched, return true
        if (armorMod.getSlots() != null && !armorMod.getSlots().isEmpty()) {
            var targetSlot = ArmorPropertiesData.ArmorMod.fromEquipmentSlot(targetArmor.getEquipmentSlot());
            return targetSlot != null && armorMod.getSlots().contains(targetSlot);
        }

        return false;
    }
}
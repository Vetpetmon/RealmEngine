package com.vetpetmon.realmengine.common.armor;

import com.vetpetmon.realmengine.common.attribute.IRandomizedGear;
import com.vetpetmon.realmengine.common.attribute.RealmEngineAttributeMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused") // This is a LIBRARY, we will use these later.
public class ArmorPropertiesData {

    public static final Map<String, Map<String, ArmorProperties>> ARMOR_PROPERTIES = new ConcurrentHashMap<>();

    /**
     * Entry for an armor item with its associated modset
     * @param item the armor item (e.g. "realmengine:celestial_helmet")
     * @param modsetId e.g., "bisccel:helmet_set"
     */
    public record ArmorItemEntry(Item item, String modsetId) { }

    public static class ArmorProperties {
        public final String id; // Unique identifier (name) for these armor properties
        // List of armor items for set
        public List<Item> armorItems;
        // List of armor items with their modset references
        public List<ArmorItemEntry> armorItemEntries;
        // Defined later when reading JSON: SetEffects
        public Map<String, SetEffect> setEffects;
        // List of applicable mods for this armor set (now ArmorMod objects with more metadata)
        public List<ArmorMod> applicableMods;
        public int randomModCount = 0; // Track how many random mods this set has for use in mod application logic

        public ArmorProperties(String id) {
            this.id = id;
        }

        // Check if a full set is equipped
        public boolean hasFullSet(List<Item> equippedArmorItems) {
            if (armorItems == null || armorItems.isEmpty()) return false;
            for (Item item : armorItems) {
                if (!equippedArmorItems.contains(item)) {
                    return false; // Missing an item from the set
                }
            }
            return true;
        }
        
        /**
         * Get the modset ID for a specific armor item
         * @param item the armor item to look up
         * @return the modset ID or null if not found
         */
        public String getModsetIdForItem(Item item) {
            if (armorItemEntries == null) return null;
            for (ArmorItemEntry entry : armorItemEntries) if (entry.item() == item) return entry.modsetId();
            return null;
        }

        // Checks if the mod is applicable to this armor set
        public boolean isModApplicable(Item modItem) {
            if (applicableMods == null) return false;
            for (ArmorMod am : applicableMods) {
                Item candidate = am.getModItem();
                if (candidate != null) if (candidate == modItem) return true;
                    // Direct identity check (registry instances should be same) and fallback to equals
            }
            return false;
        }

        // Get set effects for processing
        public Map<String, SetEffect> getSetEffects() {
            return setEffects;
        }

        // Add set effect
        public void addSetEffect(String effectName, SetEffect effect) {
            if (setEffects == null) {
                setEffects = new ConcurrentHashMap<>();
            }
            setEffects.put(effectName, effect);
        }

    }

    // Armor mods class
    public static class ArmorMod {
        public enum SLOT_NAME {HEAD, CHEST, LEGS, BOOTS} // Fetched by/from JSON as strings and converted to enum for easier handling

        // Item ID of the item used to apply this mod, e.g. "realmengine:strength_mod"
        public final String itemID;
        // The actual mod Item if resolvable from registries (null if optional/missing)
        private final Item modItem;
        // Applicable items for this mod, e.g. helmet/chest/legs/boots or any combination thereof
        public final List<Item> applicableItems;
        // Optional flag from JSON that suppresses errors when mod item missing
        public final boolean optional;
        // Slots this mod applies to (if known)
        public final List<SLOT_NAME> slots;
        // Per-mod attribute modifiers defined in JSON (mod_effects)
        public final Map<String, RealmEngineAttributeMod> modEffects;

        public ArmorMod(String itemID, Item modItem, List<Item> applicableItems, List<SLOT_NAME> slots, boolean optional) {
            this(itemID, modItem, applicableItems, slots, optional, null);
        }

        public ArmorMod(String itemID, Item modItem, List<Item> applicableItems, List<SLOT_NAME> slots, boolean optional, Map<String, RealmEngineAttributeMod> modEffects) {
            this.itemID = itemID;
            this.modItem = modItem;
            this.applicableItems = applicableItems;
            this.optional = optional;
            this.slots = slots;
            this.modEffects = modEffects;
        }

        public List<Item> getApplicableItems() {
            return applicableItems;
        }
        public boolean isApplicableTo(Item item) {
            return applicableItems != null && applicableItems.contains(item);
        }
        public String getItemID() {
            return itemID;
        }

        public Item getModItem() { return modItem; }

        public List<SLOT_NAME> getSlots() { return slots; }

        public boolean isOptional() { return optional; }

        // Get per-mod effects (could be null)
        public Map<String, RealmEngineAttributeMod> getModEffects() { return modEffects; }

        // Helper: convert an EquipmentSlot to SLOT_NAME
        public static SLOT_NAME fromEquipmentSlot(EquipmentSlot es) {
            return switch (es) {
                case HEAD -> SLOT_NAME.HEAD;
                case CHEST -> SLOT_NAME.CHEST;
                case LEGS -> SLOT_NAME.LEGS;
                case FEET -> SLOT_NAME.BOOTS;
                default -> null;
            };
        }

    }

    // Set effect class
    public static class SetEffect {
        public final String name;
        // Attributes to build
        public Map<String, RealmEngineAttributeMod> attributeModifiers;


        public SetEffect(String name) {
            this.name = name;
        }

        // Get attribute modifiers
        public Map<String, RealmEngineAttributeMod> getAttributeModifiers() {
            return attributeModifiers;
        }

        // Construct attribute modifiers from JSON data
        public void addAttributeModifier(String attributeName, RealmEngineAttributeMod mod) {
            if (attributeModifiers == null) attributeModifiers = new ConcurrentHashMap<>();
            attributeModifiers.put(attributeName, mod);
        }

    }


    /**
     * Apply individual armor mod effects using ItemAttributeModifierEvent
     * Reads from both:
     * - "gear_mods" NBT tag (randomized modifiers from modsets)
     * - "armor_mods" NBT tag (applied armor mod pieces)
     * This is called by Minecraft when calculating item attributes for equipped armor
     */
    @SubscribeEvent
    public void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();

        // Only process armor items
        if (!(stack.getItem() instanceof ArmorItem armorItem)) return;

        // Only process if this is the correct slot for this armor piece
        if (event.getSlotType() != armorItem.getEquipmentSlot()) return;

        CompoundTag tag = stack.getTag();
        if (tag == null) return;

        // Read mod_item_id to find the ArmorMod definition for operation types
        String modItemId = null;
        if (tag.contains("modifiers")) {
            CompoundTag modifiers = tag.getCompound("modifiers");
            if (modifiers.contains("mod_item_id")) modItemId = modifiers.getString("mod_item_id");
        }

        // Find the ArmorMod definition for this specific armor item
        ArmorMod armorMod = null;
        if (modItemId != null) armorMod = ArmorModUtils.findArmorModForItem(modItemId, armorItem);


        // Part 1: Apply randomized modifiers from gear_mods
        if (tag.contains("gear_mods")) {
            var gearMods = tag.getCompound("gear_mods");
            for (String modName : gearMods.getAllKeys()) {
                double amount = gearMods.getDouble(modName);

                // Try to resolve the attribute from the mod name
                var attribute = RealmEngineAttributeMod.getAttributeFromString(modName);
                if (attribute == null) continue;

                // Find the corresponding RealmfallAttributeMod to get operation type
                AttributeModifier.Operation operation = getOperationByName(modName, armorMod);

                // Build a unique UUID for this modifier on this specific item
                UUID modUuid = deriveGearModifierUUID(stack, modName);
                String modifierName = "gear_mod_" + modName;

                // Add the modifier to the event
                event.addModifier(attribute, new AttributeModifier(modUuid, modifierName, amount, operation));
            }
        }

        // Part 2: Apply armor mod piece modifiers from armor_mods
        if (tag.contains("armor_mods")) {
            CompoundTag armorMods = tag.getCompound("armor_mods");
            for (String modName : armorMods.getAllKeys()) {
                double amount = armorMods.getDouble(modName);

                // Look up the effect definition from ArmorMod if available
                RealmEngineAttributeMod effectMod = null;
                if (armorMod != null && armorMod.getModEffects() != null) effectMod = armorMod.getModEffects().get(modName);


                // Try to resolve the attribute
                Attribute attribute = effectMod != null ? effectMod.getAttribute() : RealmEngineAttributeMod.getAttributeFromString(modName);
                if (attribute == null) continue;

                // Get operation type
                AttributeModifier.Operation operation = effectMod != null ? effectMod.getOperation() : AttributeModifier.Operation.ADDITION;

                // Build a unique UUID for this modifier
                UUID modUuid = deriveGearModifierUUID(stack, "armor_mod_" + modName);
                String modifierName = "armor_mod_" + modName;

                // Add the modifier to the event
                event.addModifier(attribute, new AttributeModifier(modUuid, modifierName, amount, operation));
            }
        }
    }

    private AttributeModifier.Operation getOperationByName(String modName, ArmorMod armorMod) {
        RealmEngineAttributeMod modDef = null;
        if (armorMod != null && armorMod.getModEffects() != null) {
            // Try to find by mod name
            modDef = armorMod.getModEffects().get(modName);
            if (modDef == null) {
                // Also try to find by attribute registry name (fallback)
                for (var effectMod : armorMod.getModEffects().values()) {
                    if (modName.equals(effectMod.getAttributeRegistryName()) || modName.equals(effectMod.getModiName())) {
                        modDef = effectMod;
                        break;
                    }
                }
            }
        }

        // Determine operation (default to ADDITION if we can't find the definition)
        AttributeModifier.Operation operation = AttributeModifier.Operation.ADDITION;
        if (modDef != null) operation = modDef.getOperation();
        return operation;
    }

    /**
     * Derive a deterministic UUID for a gear modifier on a specific item stack
     * Uses item description ID and attribute key to ensure uniqueness
     */
    private static UUID deriveGearModifierUUID(ItemStack stack, String attrKey) {
        String base = "gear_mod|" + stack.getItem().getDescriptionId() + "|" + attrKey;
        return UUID.nameUUIDFromBytes(base.getBytes(StandardCharsets.UTF_8));
    }

    @SubscribeEvent
    public void gearAttributeMods(ItemAttributeModifierEvent event) {

        ItemStack stack = event.getItemStack();

        if (stack.getItem() instanceof IRandomizedGear gear) {
            EquipmentSlot slot = event.getSlotType();

            // If item is ArmorItem, only apply when slot matches armor slot (slot must be non-null)
            if (stack.getItem() instanceof ArmorItem armorItem) {
                if (slot != null && slot == armorItem.getEquipmentSlot()) {
                    for (RealmEngineAttributeMod mod : gear.readModsFromStack(stack)) {
                        // Defensive: some mods may be defined by name only and not yet resolve to an Attribute on the client.
                        if (mod.getAttribute() == null) continue;
                        UUID derived = IRandomizedGear.deriveInstanceUUID(mod, stack, slot);
                        String derivedName = IRandomizedGear.deriveInstanceName(mod, stack, slot);
                        event.addModifier(mod.getAttribute(), new AttributeModifier(derived, derivedName, mod.getAmount(), mod.getOperation()));
                    }
                    for (RealmEngineAttributeMod mod : gear.readArmorModsForArmorItem(armorItem, stack)) {
                        if (mod.getAttribute() == null) continue;
                        UUID derived = IRandomizedGear.deriveInstanceUUID(mod, stack, slot);
                        String derivedName = IRandomizedGear.deriveInstanceName(mod, stack, slot);
                        event.addModifier(mod.getAttribute(), new AttributeModifier(derived, derivedName, mod.getAmount(), mod.getOperation()));
                    }
                }
            } else {
                // Non-armor IRandomizedGear: if the item reports itself as a Curio, treat as curio (apply ONLY when actually equipped as a curio)
                if (gear.isCurio()) {
                    // Only apply curio modifiers when the event slot is null (curio context). This prevents modifiers
                    // from being shown/applied for every equipment slot in inventory/tooltips.
                    if (event.getSlotType() != null) return;

                    for (RealmEngineAttributeMod mod : gear.readModsFromStack(stack)) {
                        if (mod.getAttribute() == null) continue;
                        UUID derived = IRandomizedGear.deriveInstanceUUID(mod, stack, null);
                        String derivedName = IRandomizedGear.deriveInstanceName(mod, stack, null);
                        event.addModifier(mod.getAttribute(), new AttributeModifier(derived, derivedName, mod.getAmount(), mod.getOperation()));
                    }
                } else {
                    // fallback: use provided slot (slot must be non-null)
                    if (slot != null && slot == gear.getGearEquipmentSlot())
                        for (RealmEngineAttributeMod mod : gear.readModsFromStack(stack)) {
                            if (mod.getAttribute() == null) continue;
                            UUID derived = IRandomizedGear.deriveInstanceUUID(mod, stack, slot);
                            String derivedName = IRandomizedGear.deriveInstanceName(mod, stack, slot);
                            event.addModifier(mod.getAttribute(), new AttributeModifier(derived, derivedName, mod.getAmount(), mod.getOperation()));
                        }
                }
            }

        }
    }

    // Subscriber to apply armor attribute mods to players
    @SubscribeEvent
    public void applyArmorEffects(LivingEquipmentChangeEvent event) {
        // Only run on server side
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            // Build list of currently equipped armor items (after the change)
            List<ItemStack> armorSlots = player.getInventory().armor;
            List<Item> equippedArmorItems = new ArrayList<>();
            for (ItemStack stack : armorSlots)
                if (!stack.isEmpty()) equippedArmorItems.add(stack.getItem());

            // Iterate every defined armor properties set and ensure modifiers are applied/removed
            for (Map<String, ArmorProperties> namespaceMap : ARMOR_PROPERTIES.values()) {
                for (ArmorProperties props : namespaceMap.values()) {
                    boolean hasFullSet = props.hasFullSet(equippedArmorItems);
                    if (props.getSetEffects() == null) continue;

                    Map<String, SetEffect> effects = props.getSetEffects();

                    for (SetEffect effect : effects.values()) {
                        if (effect.getAttributeModifiers() == null) continue;
                        Map<String, RealmEngineAttributeMod> mods = effect.getAttributeModifiers();

                        for (RealmEngineAttributeMod mod : mods.values()) {
                            // Resolve attribute lazily if necessary
                            Attribute attribute = mod.getAttribute();
                            if (attribute == null) {
                                String regName = mod.getAttributeRegistryName();
                                if (regName != null) attribute = RealmEngineAttributeMod.getAttributeFromString(regName);
                            }
                            if (attribute == null || player.getAttribute(attribute) == null) continue;
                            AttributeInstance instance = player.getAttribute(attribute);
                            assert instance != null; // Should never be null here due to checks above

                            // Build deterministic per-player modifier UUID so we can remove later
                            UUID derived = derivePlayerModifierUUID(mod, player, props, effect);
                            String derivedName = derivePlayerModifierName(mod, player, props, effect);

                            if (hasFullSet) {
                                // Apply modifier if missing or different value
                                AttributeModifier existing = instance.getModifier(derived);
                                if (existing == null || Double.compare(existing.getAmount(), mod.getAmount()) != 0 || existing.getOperation() != mod.getOperation()) {
                                    // remove any stale modifier and add new transient one
                                    instance.removeModifier(derived);
                                    instance.addTransientModifier(new AttributeModifier(derived, derivedName, mod.getAmount(), mod.getOperation()));
                                    try { mod.onAttributeUpdate(player); } catch (Exception ignored) {}
                                }
                            } else {
                                // Remove modifier if present
                                if (instance.getModifier(derived) != null) {
                                    instance.removeModifier(derived);
                                    try { mod.onAttributeUpdate(player); } catch (Exception ignored) {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Derive a deterministic per-player modifier UUID for a given armor-set modifier so we can add/remove it reliably.
     */
    private static UUID derivePlayerModifierUUID(RealmEngineAttributeMod mod, Player player, ArmorProperties props, SetEffect effect) {
        String base = player.getUUID() + "|" + (mod.getModiId() != null ? mod.getModiId().toString() : "null") + "|" + props.id + "|" + effect.name;
        return UUID.nameUUIDFromBytes(base.getBytes(StandardCharsets.UTF_8));
    }

    private static String derivePlayerModifierName(RealmEngineAttributeMod mod, Player player, ArmorProperties props, SetEffect effect) {
        return mod.getModiName() + "@" + props.id + ":" + effect.name + "@" + player.getUUID();
    }



}

package com.vetpetmon.realmengine.common.attribute;

import com.vetpetmon.realmengine.RealmEngine;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

/**
 * Interface for gear that has randomized stats
 */
public interface IRandomizedGear {

    String MODS_TAG = "gear_mods";
    String ARMOR_MODIFIER_TAG = "modifiers"; // tag used by ArmorModPiece

    /**
     * Get gear mod count
     * @return Mod count
     */
    int getGearModCount();

    /**
     * Get if this item is a Curio
     */
    default boolean isCurio() {
        return false;
    }

    /** Get equipment slot for this gear
     * <p> Defaults to main hand slot</p>
     * @return EquipmentSlot
     */
    default EquipmentSlot getGearEquipmentSlot() {
        return EquipmentSlot.MAINHAND;
    }

    /**set gear mod count
     * @param count New mod count
     */
    void setGearModCount(int count);

    /**
     * Get possible mods for this gear
     * @return List of possible mods for this gear
     */
    List<RealmEngineAttributeMod> getPossibleMods();
    
    /**
     * Get the modset ID for this gear item, if one is defined.
     * This should be overridden by implementations to return the modset ID from JSON or other configuration.
     * @return modset ID string (e.g., "bisccel:helmet_set") or null if not using modsets
     */
    default String getModsetId() {
        return null;
    }
    
    /**
     * Generate random mods from a modset. This creates new RealmfallAttributeMod instances
     * with randomized values based on the modset's min/max ranges.
     * @param modsetId the modset ID to use (e.g., "bisccel:helmet_set")
     * @return list of randomized mods, or empty list if modset not found
     */
    default List<RealmEngineAttributeMod> generateModsFromModset(String modsetId) {
        if (modsetId == null || modsetId.isEmpty()) return Collections.emptyList();
        
        ModsetData.Modset modset = ModsetData.getModset(modsetId);
        if (modset == null) return Collections.emptyList();
        
        return modset.generateRandomMods();
    }

    /**
     * Get random double in range
     * @param min Min
     * @param max Max
     * @return Random value between min and max
     */
    static Double randomDoubleInRange(double min, double max) {
        return Math.random() * (max - min) + min;
    }

    /**
     * Get random int in range as double
     * @param min Min
     * @param max Max
     * @return Random value between min and max
     */
    static Double randomIntInRange(int min, int max) {
        return (double)(min + (int)(Math.random() * ((max - min) + 1)));
    }

    /**
     * Generate gear mods and store them in the item's NBT
     * @param stack ItemStack to store mods in
     * @param mods List of possible mods (can be empty if using modsets)
     */
    default void generateGearMods(ItemStack stack, List<RealmEngineAttributeMod> mods) {
        CompoundTag tag = stack.getOrCreateTag();
        CompoundTag modsTag = new CompoundTag();
        
        // Try to use modset first if available
        String modsetId = getModsetId();
        List<RealmEngineAttributeMod> modsToUse = mods;
        
        if (modsetId != null && !modsetId.isEmpty()) {
            List<RealmEngineAttributeMod> modsetMods = generateModsFromModset(modsetId);
            if (!modsetMods.isEmpty()) {
                modsToUse = modsetMods;
            }
        }
        
        // If we have no mods to work with, create empty tag to mark checked
        if (modsToUse == null || modsToUse.isEmpty()) {
            tag.put(MODS_TAG, modsTag);
            return;
        }
        
        List<RealmEngineAttributeMod> shuffled = new ArrayList<>(modsToUse);
        Collections.shuffle(shuffled);
        int modCount = Math.min(getGearModCount(), shuffled.size());
        
        // if the item mod count is zero, we still want to make the tag to mark that RNG mods were checked, just leave it empty
        if (modCount == 0) {
            tag.put(MODS_TAG, modsTag);
            return;
        }
        
        for (int i = 0; i < modCount; i++) {
            RealmEngineAttributeMod mod = shuffled.get(i);
            double boost = getRarityBoost(modsetId);
            RealmEngine.LOGGER.debug("Generating mod {}: base amount={}, rarity boost={}, final amount={}", mod.getModiName(), mod.getAmount(), boost, mod.getAmount()*boost);
            modsTag.putDouble(mod.getModiName(), mod.getAmount()*boost);
        }
        tag.put(MODS_TAG, modsTag);
    }

    /**
     * Get the attribute mods for the given ItemStack, ensuring they exist.
     * @param stack the ItemStack to get mods for
     * @param level the Level context
     * @return the list of BeastcuitAttributeMods
     */
    default List<RealmEngineAttributeMod> getAttributeMods(ItemStack stack, Level level){
        ensureModsExist(stack, level);
        return readModsFromStack(stack);
    }

    /**
     * Read the mods from the ItemStack's NBT.
     * @param stack the ItemStack to read mods from
     * @return the list of BeastcuitAttributeMods
     */
    default List<RealmEngineAttributeMod> readModsFromStack(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(MODS_TAG))
            return Collections.emptyList();
        CompoundTag modsTag = tag.getCompound(MODS_TAG);
        if (modsTag.isEmpty())
            return Collections.emptyList();
        List<RealmEngineAttributeMod> mods = new ArrayList<>();
        for (RealmEngineAttributeMod baseMod : getPossibleMods()) {
            if (modsTag.contains(baseMod.getModiName())) {
                double amount = modsTag.getDouble(baseMod.getModiName());
                mods.add(RealmEngineAttributeMod.createFixedMod(
                        baseMod.getAttribute(), baseMod::getModiId, baseMod::getModiName, () -> amount
                ));
            }
        }
        return mods;
    }

    /**
     * Read armor modifier data saved by `ArmorModPiece` from an ItemStack and return corresponding mods.
     * This mirrors the structure used by ArmorModPiece.saveToTag/loadFromTag.
     * @param armorItem the armor item (unused here but provided for API symmetry)
     * @param stack the ItemStack to read
     * @return list containing a single RealmfallAttributeMod if present, otherwise empty
     */
    default List<RealmEngineAttributeMod> readArmorModsForArmorItem(ArmorItem armorItem, ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ARMOR_MODIFIER_TAG))
            return Collections.emptyList();
        CompoundTag modsTag = tag.getCompound(ARMOR_MODIFIER_TAG);
        if (modsTag.isEmpty())
            return Collections.emptyList();

        String modName = modsTag.contains("mod_name") ? modsTag.getString("mod_name") : "";
        String modUuidStr = modsTag.contains("mod_uuid") ? modsTag.getString("mod_uuid") : "";
        double modAmount = modsTag.contains("mod_amount") ? modsTag.getDouble("mod_amount") : 0.0;
        String modAttributeStr = modsTag.contains("mod_attribute") ? modsTag.getString("mod_attribute") : "";

        if (modName.isEmpty() || modAttributeStr.isEmpty())
            return Collections.emptyList();

        final var attribute = RealmEngineAttributeMod.getAttributeFromString(modAttributeStr);

        java.util.UUID uuid;
        try {
            uuid = modUuidStr.isEmpty() ? java.util.UUID.randomUUID() : java.util.UUID.fromString(modUuidStr);
        } catch (IllegalArgumentException ex) {
            uuid = java.util.UUID.randomUUID();
        }

        RealmEngineAttributeMod rm;
        java.util.UUID finalUuid = uuid;
        if (attribute == null) {
            // Create a lazy resolver mod so the attribute can be resolved later
            rm = RealmEngineAttributeMod.createFixedModByAttributeName(modAttributeStr, () -> finalUuid, () -> modName, () -> modAmount);
        } else {
            rm = RealmEngineAttributeMod.createFixedMod(attribute, () -> finalUuid, () -> modName, () -> modAmount);
        }
        return List.of(rm);
    }

    /**
     * Check if an ArmorModPiece exists on the given ItemStack.
     * Useful for checking if cold resistance mod is applied to armor so that a bone in the model renders.
     * @param stack the ItemStack to check
     * @param name the name of the armor mod to look for
     * @return true if an ArmorModPiece exists, false otherwise
     */
    static boolean hasArmorModPiece(ItemStack stack, String name) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ARMOR_MODIFIER_TAG))
            return false;
        CompoundTag modsTag = tag.getCompound(ARMOR_MODIFIER_TAG);
        if (modsTag.isEmpty())
            return false;
        String modName = modsTag.contains("mod_item_id") ? modsTag.getString("mod_item_id") : "";
        return modName.equals(name);
    }

    /**
     * Calculate the multiplier for mod amounts based with day boosts.
     * @return the rarity boost multiplier
     */
    default double getRarityBoost(String modsetId) {
        double base = 1.0;
        if (modsetId == null || modsetId.isEmpty()) return base;

        ModsetData.Modset modset = ModsetData.getModset(modsetId);
        if (modset == null) return base;

        RealmEngine.Weekday boostedDay = modset.getBoostedStatDay();
        RealmEngine.Weekday today = RealmEngine.PROXY != null ? RealmEngine.PROXY.getWeekday() : RealmEngine.Weekday.NONE;
        if (today == boostedDay) return modset.getBoostedStatRate();
        return base;
    }


    /**
     * Ensure that the mods exist in the ItemStack's NBT. If not, generate them.
     * @param stack the ItemStack to check
     * @param level the Level context
     */
    default void ensureModsExist(ItemStack stack, Level level) {
        if (level == null || level.isClientSide)
            return;
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(MODS_TAG) || tag.getCompound(MODS_TAG).isEmpty())
            generateRandomMods(stack);
    }

    /**
     * Generate random mods for the beastcuit and store them in NBT.
     * @param stack the ItemStack to generate mods for
     */
    default void generateRandomMods(ItemStack stack){
        generateGearMods(stack, getPossibleMods());
    }

    /**
     * Derive a deterministic but unique UUID for this mod instance based on the base mod UUID,
     * the item stack contents and the equipment slot. This keeps NBT untouched but ensures
     * multiple equipped copies of the same mod can be applied simultaneously.
     */
    static UUID deriveInstanceUUID(RealmEngineAttributeMod mod, ItemStack stack, EquipmentSlot slot) {
        // If slot is null, try to detect if the item is a Curio and use a CURIO marker so curio-equipped items
        // still get deterministic but distinct UUIDs.
        String slotName;
        if (slot == null)
            slotName = (stack.getItem() instanceof IRandomizedGear gear && gear.isCurio()) ? "CURIO" : "UNKNOWN" ;
        else
            slotName = slot.name();
        String base = mod.getModiId().toString() + "|" + slotName + "|" + (stack.hasTag() ? Objects.requireNonNull(stack.getTag()).toString() : "");
        return UUID.nameUUIDFromBytes(base.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Derive a readable modifier name for this instance (not stored in NBT).
     */
    static String deriveInstanceName(RealmEngineAttributeMod mod, ItemStack stack, EquipmentSlot slot) {
        String slotName = (slot == null) ? ((stack.getItem() instanceof IRandomizedGear gear && gear.isCurio()) ? "CURIO" : "UNKNOWN") : slot.name();
        return mod.getModiName() + "@" + slotName;
    }

    DecimalFormat ATTRIBUTE_MODIFIER_FORMAT = Util.make(new DecimalFormat("#.####"), (p_41704_) -> p_41704_.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT)));

    default void attachItemTooltips(String modID , @NotNull ItemStack stack, Level world, @NotNull List<Component> tooltipList) {

        MutableComponent text1 = Component.translatable(modID+".tooltip." + this);
        tooltipList.add(text1.withStyle(ChatFormatting.LIGHT_PURPLE));

        tooltipList.add(Component.empty());

        for (RealmEngineAttributeMod mod : getAttributeMods(stack, world)) {
            double amount = mod.getAmount();

            if (mod.getOperation() == AttributeModifier.Operation.ADDITION) {
                if (mod.getAttribute().equals(Attributes.KNOCKBACK_RESISTANCE) || mod.getAttribute().equals(Attributes.MOVEMENT_SPEED)) amount *= 10;
            }
            else amount *= 100;

            String sign = (mod.getAmount() >= 0) ? "attribute.modifier.plus." : "attribute.modifier.take.";

            amount = Math.abs(amount); // show absolute value in tooltip

            tooltipList.add(Component.translatable(
                            sign + mod.getOperation().toValue(),
                            ATTRIBUTE_MODIFIER_FORMAT.format(amount),
                            Component.translatable(mod.getAttribute().getDescriptionId()))
                    .withStyle(ChatFormatting.BLUE));
        }
    }

    /**
     * Apply attribute modifiers to the wearer each tick.
     * @param stack the ItemStack being worn
     * @param world the Level context
     * @param wearer the LivingEntity wearing the item
     */
    default void onWornTick(ItemStack stack, Level world, LivingEntity wearer) {
        if (!world.isClientSide) {
            EquipmentSlot slot;
            if (stack.getItem() instanceof ArmorItem ai) slot = ai.getEquipmentSlot();
            else slot = getGearEquipmentSlot();

            for (RealmEngineAttributeMod mod : getAttributeMods(stack, world)) {
                AttributeInstance instance = wearer.getAttribute(mod.getAttribute());
                if (instance != null) {
                    UUID derived = deriveInstanceUUID(mod, stack, slot);
                    AttributeModifier existingMod = instance.getModifier(derived);
                    if (existingMod == null || existingMod.getAmount() != mod.getAmount()) {
                        instance.removeModifier(derived);
                        instance.addTransientModifier(new AttributeModifier(derived, deriveInstanceName(mod, stack, slot), mod.getAmount(), mod.getOperation()));
                        mod.onAttributeUpdate(wearer);
                    }
                }
            }
        }
    }

    /**
     * Remove attribute modifiers from the wearer when unequipped.
     * @param stack the ItemStack being unequipped
     * @param world the Level context
     * @param wearer the LivingEntity wearing the item
     */
    default void onUnequip(ItemStack stack, Level world, LivingEntity wearer) {
        if (!world.isClientSide) {
            EquipmentSlot slot;
            if (stack.getItem() instanceof ArmorItem ai) slot = ai.getEquipmentSlot();
            else slot = getGearEquipmentSlot();

            for (RealmEngineAttributeMod mod : getAttributeMods(stack, world)) {
                AttributeInstance instance = wearer.getAttribute(mod.getAttribute());
                if (instance != null) {
                    UUID derived = deriveInstanceUUID(mod, stack, slot);
                    instance.removeModifier(derived);
                    mod.onAttributeUpdate(wearer);
                }
            }
        }
    }

    default void onEquip(ItemStack stack, Level world, LivingEntity wearer) {
        if (!world.isClientSide) {
            EquipmentSlot slot;
            if (stack.getItem() instanceof ArmorItem ai) slot = ai.getEquipmentSlot();
            else slot = getGearEquipmentSlot();

            for (RealmEngineAttributeMod mod : getAttributeMods(stack, world)) {
                AttributeInstance instance = wearer.getAttribute(mod.getAttribute());
                if (instance != null) {
                    UUID derived = deriveInstanceUUID(mod, stack, slot);
                    instance.removeModifier(derived);
                    AttributeModifier modifier = new AttributeModifier(derived, deriveInstanceName(mod, stack, slot), mod.getAmount(), mod.getOperation());
                    instance.addTransientModifier(modifier);
                    mod.onAttributeUpdate(wearer);
                }
            }
        }
    }


}

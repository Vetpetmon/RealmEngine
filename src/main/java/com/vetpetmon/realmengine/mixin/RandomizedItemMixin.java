package com.vetpetmon.realmengine.mixin;

import com.vetpetmon.realmengine.common.armor.ArmorPropertiesData;
import com.vetpetmon.realmengine.common.item.ItemPropertiesData;
import com.vetpetmon.realmengine.common.attribute.IRandomizedGear;
import com.vetpetmon.realmengine.common.attribute.ModsetData;
import com.vetpetmon.realmengine.common.attribute.RealmEngineAttributeMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;

@Mixin(Item.class)
@Implements(@Interface(iface = IRandomizedGear.class, prefix = "rnggear$"))
public abstract class RandomizedItemMixin implements IRandomizedGear {

    @Unique
    private int realmengine$gearModCount = 3; // Fallback default if not found in ArmorProperties

    /**
     * Ensure armor items get randomized mods when crafted
     */
    @Inject(method = "onCraftedBy", at = @At("HEAD"))
    public void onCraftedBy(ItemStack stack, Level level, Player player, CallbackInfo ci) {
        if (!level.isClientSide && realmengine$shouldEnsureMods()) {
            ensureModsExist(stack, level);
        }
    }

    /**
     * Ensure armor items get randomized mods during inventory ticks (backup for items not crafted)
     */
    @Inject(method = "inventoryTick", at = @At("HEAD"))
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected, CallbackInfo ci) {
        if (!level.isClientSide && realmengine$shouldEnsureMods()) {
            ensureModsExist(stack, level);
        }
    }

    // IRandomizedGear implementation methods with rnggear$ prefix for mixin interface

    /**
     * Get gear mod count from ArmorProperties if available, otherwise use default
     */
    public int rnggear$getGearModCount() {
        if ((Object) this instanceof ArmorItem) {
            ArmorPropertiesData.ArmorProperties props = realmengine$findArmorProperties();
            if (props != null && props.randomModCount > 0) return props.randomModCount;
        } else {
            ItemPropertiesData.ItemProperties props = realmengine$findItemProperties();
            if (props != null && props.randomModCount > 0) return props.randomModCount;
        }
        return realmengine$gearModCount;
    }

    public void rnggear$setGearModCount(int count) {
        this.realmengine$gearModCount = count;
    }

    /**
     * Get possible mods from modset. This is the key fix - we need to actually generate
     * the mods from the modset here so the legacy system can use them.
     */
    public List<RealmEngineAttributeMod> rnggear$getPossibleMods() {
        String modsetId = rnggear$getModsetId();
        if (modsetId != null && !modsetId.isEmpty()) {
            ModsetData.Modset modset = ModsetData.getModset(modsetId);
            if (modset != null) return modset.generateRandomMods();
        }
        return Collections.emptyList();

    }

    /**
     * Get modset ID by looking up from ArmorPropertiesData.
     * This allows vanilla armor items to use modsets defined in armor properties JSON.
     */
    public String rnggear$getModsetId() {
        if ((Object) this instanceof ArmorItem) {
            for (var namespaceMap : ArmorPropertiesData.ARMOR_PROPERTIES.values()) {
                for (var props : namespaceMap.values()) {
                    if (props.armorItemEntries != null) for (var entry : props.armorItemEntries) {
                        if (entry.item() == (Object) this) {
                            String modsetId = entry.modsetId();
                            if (modsetId != null && !modsetId.isEmpty()) return modsetId;
                        }
                    }
                }
            }
        } else {
            for (var namespaceMap : ItemPropertiesData.ITEM_PROPERTIES.values()) {
                for (var props : namespaceMap.values()) {
                    if (props.itemEntries != null) for (var entry : props.itemEntries) {
                        if (entry.item() == (Object) this) {
                            String modsetId = entry.modsetId();
                            if (modsetId != null && !modsetId.isEmpty()) return modsetId;
                        }
                    }
                }
            }
        }
        return null;

    }

    /**
     * Helper method to find the ArmorProperties for this item
     */
    @SuppressWarnings("SuspiciousMethodCalls") // supress warning; mixin resolves "this" to Item at runtime, so contains() check is valid
    @Unique
    private ArmorPropertiesData.ArmorProperties realmengine$findArmorProperties() {
        for (var namespaceMap : ArmorPropertiesData.ARMOR_PROPERTIES.values()) for (var props : namespaceMap.values())
            if (props.armorItems != null && props.armorItems.contains((Object) this)) return props;
        return null;
    }

    @Unique
    @SuppressWarnings("SuspiciousMethodCalls")
    private ItemPropertiesData.ItemProperties realmengine$findItemProperties() {
        for (var namespaceMap : ItemPropertiesData.ITEM_PROPERTIES.values()) for (var props : namespaceMap.values())
            if (props.items != null && props.items.contains((Object) this)) return props;
        return null;
    }

    @Unique
    private boolean realmengine$shouldEnsureMods() {
        if ((Object) this instanceof ArmorItem) return true;
        return realmengine$findItemProperties() != null;
    }
}

package com.vetpetmon.realmengine.common.armor;

import com.vetpetmon.realmengine.common.attribute.RealmEngineAttributeMod;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ItemApplicableArmorMod extends Item {

    private final ArmorModPiece armorModPiece; // may be null for datapack-driven items

    // New constructor: no ArmorModPiece required (datapack-driven compatible armor lookup)
    public ItemApplicableArmorMod(Properties properties) {
        super(properties);
        this.armorModPiece = null;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.pass(held);

        // Determine supported armor items: prefer armorModPiece supplier if present, otherwise look up via ArmorPropertiesData
        List<ArmorItem> supported = new ArrayList<>();
        if (armorModPiece != null) {
            supported = armorModPiece.armorItemSupplier().get();
        } else {
            // Find ArmorMod entries that reference this item
            var key = ForgeRegistries.ITEMS.getKey(this);
            String idStr = key != null ? key.toString() : null;
            if (idStr != null) for (var nsMap : ArmorPropertiesData.ARMOR_PROPERTIES.values())
                for (var props : nsMap.values()) {
                    if (props.applicableMods == null) continue;
                    for (var am : props.applicableMods) if (idStr.equals(am.getItemID()) || (am.getModItem() != null && am.getModItem() == this))
                        // Convert applicable Items to ArmorItem where possible
                        if (am.getApplicableItems() != null)
                            for (var it : am.getApplicableItems()) if (it instanceof ArmorItem ai) supported.add(ai);
                }
        }

        for (ArmorItem armorItem : supported) {
            EquipmentSlot slot = armorItem.getEquipmentSlot();
            ItemStack target = player.getItemBySlot(slot);
            if (!target.isEmpty() && target.getItem() instanceof ArmorItem wornArmor) {
                // Determine whether this mod can apply to the worn armor
                boolean canApply;
                if (armorModPiece != null) canApply = armorModPiece.canApplyTo(wornArmor);
                else {
                    // check against supported list
                    boolean matched = false;
                    for (ArmorItem ai : supported) {
                        if (ai == null) continue;
                        if (ai == wornArmor) { matched = true; break; }
                        ResourceLocation k1 = ForgeRegistries.ITEMS.getKey(ai), k2 = ForgeRegistries.ITEMS.getKey(wornArmor);
                        if (k1 != null && k1.equals(k2)) { matched = true; break; }
                        if (ai.getDescriptionId().equals(wornArmor.getDescriptionId())) { matched = true; break; }
                    }
                    canApply = matched;
                }

                if (canApply) {
                    // If we have an ArmorModPiece (legacy), write its modifiers to the target stack
                    if (armorModPiece != null) {
                        armorModPiece.saveToTag(target);
                    } else {
                        // For datapack-driven mods, we will write a minimal tag identifying the mod item id
                        // so other systems can handle the application. We'll use the same 'modifiers' structure
                        // as ArmorModPiece.saveToTag would do when attribute data is available.
                        // Since we don't have attribute data here, just store the mod item id so downstream
                        // code can interpret it.
                        var tag = target.getOrCreateTag();
                        var modsTag = new CompoundTag();
                        var key = ForgeRegistries.ITEMS.getKey(this);
                        if (key != null) modsTag.putString("mod_item_id", key.toString());
                        tag.put("modifiers", modsTag);
                    }

                    // apply gear mods to stack (existing code expects modifiers to be present)
                    ArmorModUtils.applyArmorModsToStack(target, wornArmor);

                    // consume one mod item
                    if (!player.isCreative()) held.shrink(1);

                    player.getInventory().setChanged();

                    // Notify the player on the server
                    player.sendSystemMessage(Component.translatable("realmengine.message.applied_armor_mod").withStyle(ChatFormatting.GREEN));
                    // play sound effect
                    level.playSound(player, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_PLACE, player.getSoundSource(), 1.0f, 1.5f);
                    return InteractionResultHolder.success(held);
                }
            }
        }

        // If we reach here, no valid equipped armor was found to apply to
        player.sendSystemMessage(Component.translatable("realmengine.message.no_applicable_armor").withStyle(ChatFormatting.RED));
        // play sound effect
        level.playSound(player, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.get(), player.getSoundSource(), 1.0f, 1.0f);
        return InteractionResultHolder.pass(held);
    }

    // Add to tooltips the effect and applicable armor items
    @Override
    public void appendHoverText(@NotNull ItemStack stack, Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        MutableComponent text1 = Component.translatable("armormod.tooltip." + this);
        tooltip.add(text1.withStyle(ChatFormatting.LIGHT_PURPLE));

        if (armorModPiece != null && armorModPiece.mod() != null) {
            // Get mod info from legacy ArmorModPiece
            RealmEngineAttributeMod mod = armorModPiece.mod();
            double amount = mod.getAmount();
            Attribute attr = mod.getAttribute(); // may be null if attribute registry wasn't ready / missing

            if (attr != null) {
                // Preserve existing behavior when attribute is available
                if (mod.getOperation() == AttributeModifier.Operation.ADDITION)
                    if (attr.equals(Attributes.KNOCKBACK_RESISTANCE)) amount *= 10;

                String sign = (amount >= 0) ? "+" : "-";
                String name = Component.translatable(attr.getDescriptionId()).getString();
                String unit = (mod.getOperation() == AttributeModifier.Operation.ADDITION)
                        ? (!(attr.equals(Attributes.KNOCKBACK_RESISTANCE)) ? " pts" : "%")
                        : "";
                tooltip.add(Component.literal("Mod: " + name + " " + sign + Math.abs(amount) + unit).withStyle(ChatFormatting.GRAY));
            } else {
                // Fallback when the Attribute object isn't available: use the modifier name as translation key
                String sign = (amount >= 0) ? "+" : "-";
                String name = Component.translatable(mod.getModiName()).getString();
                tooltip.add(Component.literal("Mod: " + name + " " + sign + Math.abs(amount) + " pts").withStyle(ChatFormatting.GRAY));
            }
        }

        StringBuilder applicable = new StringBuilder("Applies to: ");
        List<ArmorItem> items = new ArrayList<>();
        if (armorModPiece != null) {
            items = armorModPiece.armorItemSupplier().get();
        } else {
            // derive items via ArmorPropertiesData
            var key = ForgeRegistries.ITEMS.getKey(this);
            String idStr = key != null ? key.toString() : null;
            if (idStr != null) {
                for (var nsMap : ArmorPropertiesData.ARMOR_PROPERTIES.values()) {
                    for (var props : nsMap.values()) {
                        if (props.applicableMods == null) continue;
                        for (var am : props.applicableMods) {
                            if (idStr.equals(am.getItemID()) || (am.getModItem() != null && am.getModItem() == this)) {
                                if (am.getApplicableItems() != null)
                                    for (var it : am.getApplicableItems()) if (it instanceof ArmorItem ai) items.add(ai);
                            }
                        }
                    }
                }
            }
        }
        int listSize = items.size(), index = 0, sizeBeforeTrunication = 10;
        // TODO: Display item inventory icons because listing all applicable items can fill up the screen in heavily modded contexts.
        for (ArmorItem i : items) {
            applicable.append(Component.translatable(i.getDescriptionId()).getString());
            index++;
            if (index < listSize) applicable.append(", ");
            if (index >= sizeBeforeTrunication) {
                applicable.append(String.format("... (%d more)", listSize - sizeBeforeTrunication));
                break;
            }
        }
        tooltip.add(Component.literal(applicable.toString()).withStyle(ChatFormatting.DARK_GRAY));
        // End of text.
    }

    public ArmorModPiece getArmorModPiece() {
        return this.armorModPiece;
    }

}

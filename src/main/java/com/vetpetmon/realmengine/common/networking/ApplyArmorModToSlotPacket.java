package com.vetpetmon.realmengine.common.networking;

import com.vetpetmon.realmengine.common.armor.ArmorModPiece;
import com.vetpetmon.realmengine.common.armor.ArmorModUtils;
import com.vetpetmon.realmengine.common.armor.ArmorPropertiesData;
import com.vetpetmon.realmengine.common.armor.ItemApplicableArmorMod;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public record ApplyArmorModToSlotPacket(int slotIndex, String modItemId) {

    public static void encode(ApplyArmorModToSlotPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.slotIndex);
        buf.writeUtf(pkt.modItemId);
    }

    public static ApplyArmorModToSlotPacket decode(FriendlyByteBuf buf) {
        return new ApplyArmorModToSlotPacket(buf.readInt(), buf.readUtf());
    }

    public static void handle(ApplyArmorModToSlotPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            var menu = player.containerMenu;
            if (pkt.slotIndex < 0 || pkt.slotIndex >= menu.slots.size()) return;

            var slot = menu.slots.get(pkt.slotIndex);
            ItemStack target = slot.getItem();
            if (target.isEmpty()) return;
            if (!(target.getItem() instanceof ArmorItem wornArmor)) return;

            // Resolve mod item from modItemId
            var modRl = ResourceLocation.tryParse(pkt.modItemId);
            if (modRl == null) return;

            var modItem = ForgeRegistries.ITEMS.getValue(modRl);
            if (!(modItem instanceof ItemApplicableArmorMod applicableMod)) return;

            // Get carried stack (for consumption)
            ItemStack carried = menu.getCarried();
            if (carried.isEmpty()) {
                // Fallback to main/offhand
                if (!player.getMainHandItem().isEmpty() && player.getMainHandItem().getItem() == modItem)
                    carried = player.getMainHandItem();
                else if (!player.getOffhandItem().isEmpty() && player.getOffhandItem().getItem() == modItem)
                    carried = player.getOffhandItem();
                else return;
            }

            ArmorModPiece piece = applicableMod.getArmorModPiece();

            // Determine supported armor items using the SAME LOGIC as ItemApplicableArmorMod.use()
            java.util.List<ArmorItem> supported = new java.util.ArrayList<>();
            if (piece != null) {
                supported = piece.armorItemSupplier().get();
            } else {
                // Find ArmorMod entries that reference this item (SAME as ItemApplicableArmorMod)
                var key = ForgeRegistries.ITEMS.getKey(applicableMod);
                String idStr = key != null ? key.toString() : null;
                if (idStr != null) {
                    for (var nsMap : ArmorPropertiesData.ARMOR_PROPERTIES.values()) {
                        for (var props : nsMap.values()) {
                            if (props.applicableMods == null) continue;
                            for (var am : props.applicableMods) {
                                if (idStr.equals(am.getItemID()) || (am.getModItem() != null && am.getModItem() == applicableMod)) {
                                    // Convert applicable Items to ArmorItem where possible
                                    if (am.getApplicableItems() != null)
                                        for (var it : am.getApplicableItems()) if (it instanceof ArmorItem ai) supported.add(ai);
                                }
                            }
                        }
                    }
                }
            }

            // Check if target armor matches any supported armor (SAME logic as ItemApplicableArmorMod)
            boolean canApply = false;
            if (piece != null) {
                canApply = piece.canApplyTo(wornArmor);
            } else {
                // check against supported list
                for (ArmorItem ai : supported) {
                    if (ai == null) continue;
                    if (ai == wornArmor) { canApply = true; break; }
                    var k1 = ForgeRegistries.ITEMS.getKey(ai);
                    var k2 = ForgeRegistries.ITEMS.getKey(wornArmor);
                    if (k1 != null && k1.equals(k2)) { canApply = true; break; }
                    if (ai.getDescriptionId().equals(wornArmor.getDescriptionId())) { canApply = true; break; }
                }
            }

            if (!canApply) {
                player.sendSystemMessage(Component.translatable("realmengine.message.no_applicable_armor").withStyle(ChatFormatting.RED));
                player.playSound(SoundEvents.NOTE_BLOCK_BASS.get(), 1.0f, 1.0f);
                return;
            }

            // Apply the mod (same for both legacy and datapack)
            if (piece != null) {
                piece.saveToTag(target);
            } else {
                // For datapack-driven mods, write mod data to target
                var tag = target.getOrCreateTag();
                var modifiers = new CompoundTag();
                modifiers.putString("mod_item_id", pkt.modItemId);
                tag.put("modifiers", modifiers);

                // Find the ArmorMod for this specific armor item
                var matched = ArmorModUtils.findArmorModForItem(pkt.modItemId, wornArmor);
                if (matched != null && matched.getModEffects() != null && !matched.getModEffects().isEmpty()) {
                    var armorModsCompound = new CompoundTag();
                    for (var me : matched.getModEffects().entrySet()) {
                        armorModsCompound.putDouble(me.getKey(), me.getValue().getAmount());
                    }
                    tag.put("armor_mods", armorModsCompound);
                }
            }

            ArmorModUtils.applyArmorModsToStack(target, wornArmor);

            // Consume item
            if (!player.isCreative()) carried.shrink(1);
            if (carried == menu.getCarried()) menu.setCarried(carried);
            player.inventoryMenu.broadcastChanges();

            player.sendSystemMessage(Component.translatable("realmengine.message.applied_armor_mod").withStyle(ChatFormatting.GREEN));
            player.playSound(SoundEvents.AMETHYST_BLOCK_PLACE, 1.0f, 1.5f);
        });
        ctx.setPacketHandled(true);
    }
}


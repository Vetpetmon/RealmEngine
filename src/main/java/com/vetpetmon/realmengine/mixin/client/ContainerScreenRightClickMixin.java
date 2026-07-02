package com.vetpetmon.realmengine.mixin.client;

import com.vetpetmon.realmengine.RealmEngine;
import com.vetpetmon.realmengine.common.armor.ItemApplicableArmorMod;
import com.vetpetmon.realmengine.common.networking.ApplyArmorModToSlotPacket;
import com.vetpetmon.realmengine.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Mixin(AbstractContainerScreen.class)
public class ContainerScreenRightClickMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        // Right click (button 1)
        if (button != 1) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!(mc.screen instanceof AbstractContainerScreen<?> screen)) return;

        int left = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int top =  ((AbstractContainerScreenAccessor) screen).getTopPos();

        // Find slot under mouse by iterating slots and checking bounds
        Slot found = null;
        for (Slot s : screen.getMenu().slots) {
            int sx = left + s.x;
            int sy = top + s.y;
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                found = s;
                break;
            }
        }
        if (found == null) return;

        ItemStack carried = screen.getMenu().getCarried();
        if (carried.isEmpty()) return;
        if (!(carried.getItem() instanceof ItemApplicableArmorMod)) return;

        // Determine registry id of carried mod item and send with packet so server can resolve it even if cursor unsynced
        String modItemId = "";
        var key = ForgeRegistries.ITEMS.getKey(carried.getItem());
        if (key != null) modItemId = key.toString();

        // Send packet to server to apply this mod to the clicked slot
        RealmEngine.PACKET_HANDLER.sendToServer(new ApplyArmorModToSlotPacket(found.index, modItemId));

        // Consume the click locally to prevent default behavior
        cir.setReturnValue(true);
    }
}

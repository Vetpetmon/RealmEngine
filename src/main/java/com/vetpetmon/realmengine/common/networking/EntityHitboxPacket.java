package com.vetpetmon.realmengine.common.networking;

import com.vetpetmon.realmengine.RealmEngine;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EntityHitboxPacket {
    public int parentID, playerID, interactionType;
    public float damage;

    public EntityHitboxPacket(int parentID, int playerID, int interactionType, float damage) {
        this.parentID = parentID;
        this.playerID = playerID;
        this.interactionType = interactionType;
        this.damage = damage;
    }

    public EntityHitboxPacket(){}

    public static EntityHitboxPacket read(FriendlyByteBuf buf) {
        return new EntityHitboxPacket(buf.readInt(), buf.readInt(), buf.readInt(), buf.readFloat());
    }

    public static void write(EntityHitboxPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.parentID);
        buf.writeInt(packet.playerID);
        buf.writeInt(packet.interactionType);
        buf.writeFloat(packet.damage);
    }

    // Handle the packet on the server side
    public static void handle(EntityHitboxPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Player player = context.get().getSender();
            if (context.get().getDirection().getReceptionSide() == LogicalSide.CLIENT){
                player = RealmEngine.PROXY.getClientPlayer();
            }

            Entity parentEntity = player.level().getEntity(packet.parentID), interactionEntity = player.level().getEntity(packet.playerID);
            if (parentEntity != null && interactionEntity != null) {
                if (packet.interactionType == 0 && parentEntity.isMultipartEntity()) {

                    if (packet.interactionType == 0 && interactionEntity.distanceTo(parentEntity) < 16) {
                        if (interactionEntity instanceof Player playerInt)
                            parentEntity.interact(playerInt, playerInt.getUsedItemHand());
                    }
                    else
                        parentEntity.hurt(parentEntity.damageSources().generic(), (float) packet.damage);
                }
            }
        });

        context.get().setPacketHandled(true);
    }
}

package com.vetpetmon.realmengine.common.networking;

import com.vetpetmon.realmengine.RealmEngine;
import com.vetpetmon.realmengine.common.attribute.ModsetData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Packet to sync modset data from server to client.
 * This is needed so that tooltip calculations have access to modset definitions on dedicated servers.
 */
public class SyncModsetsPacket {
    private final Map<String, Map<String, SerializedModset>> data;

    public SyncModsetsPacket(Map<String, Map<String, ModsetData.Modset>> sourceData) {
        this.data = new HashMap<>();

        // Serialize the modset data
        for (var namespaceEntry : sourceData.entrySet()) {
            String namespace = namespaceEntry.getKey();
            Map<String, SerializedModset> serializedMap = new HashMap<>();

            for (var modsetEntry : namespaceEntry.getValue().entrySet()) {
                String id = modsetEntry.getKey();
                ModsetData.Modset modset = modsetEntry.getValue();
                serializedMap.put(id, new SerializedModset(modset));
            }

            data.put(namespace, serializedMap);
        }
    }

    public SyncModsetsPacket(FriendlyByteBuf buf) {
        this.data = new HashMap<>();

        int namespaceCount = buf.readInt();
        for (int i = 0; i < namespaceCount; i++) {
            String namespace = buf.readUtf();
            int modsetCount = buf.readInt();
            Map<String, SerializedModset> modsetMap = new HashMap<>();

            for (int j = 0; j < modsetCount; j++) {
                String id = buf.readUtf();
                SerializedModset modset = new SerializedModset(buf);
                modsetMap.put(id, modset);
            }

            data.put(namespace, modsetMap);
        }
    }

    public static void encode(SyncModsetsPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.data.size());

        for (var namespaceEntry : pkt.data.entrySet()) {
            buf.writeUtf(namespaceEntry.getKey());
            Map<String, SerializedModset> modsetMap = namespaceEntry.getValue();
            buf.writeInt(modsetMap.size());

            for (var modsetEntry : modsetMap.entrySet()) {
                buf.writeUtf(modsetEntry.getKey());
                modsetEntry.getValue().encode(buf);
            }
        }
    }

    public static SyncModsetsPacket decode(FriendlyByteBuf buf) {
        return new SyncModsetsPacket(buf);
    }

    public static void handle(SyncModsetsPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getDirection().getReceptionSide().isClient()) {
                // Clear existing client data
                ModsetData.MODSETS.clear();

                // Deserialize and populate client-side modsets
                for (var namespaceEntry : pkt.data.entrySet()) {
                    String namespace = namespaceEntry.getKey();
                    Map<String, ModsetData.Modset> modsetMap = new ConcurrentHashMap<>();

                    for (var serializedEntry : namespaceEntry.getValue().entrySet()) {
                        String id = serializedEntry.getKey();
                        SerializedModset serialized = serializedEntry.getValue();
                        ModsetData.Modset modset = serialized.deserialize();
                        modsetMap.put(id, modset);
                    }

                    ModsetData.MODSETS.put(namespace, modsetMap);
                }

                RealmEngine.LOGGER.info("Synced modset data from server: {} namespaces", pkt.data.size());
            }
        });
        ctx.setPacketHandled(true);
    }

    /**
     * Serializable version of Modset for network transmission
     */
    private static class SerializedModset {
        String id;
        RealmEngine.Weekday boostedStatDay;
        double boostedStatRate;
        List<SerializedModifierDefinition> modifiers;

        SerializedModset(ModsetData.Modset modset) {
            this.id = modset.id;
            this.boostedStatDay = modset.getBoostedStatDay();
            this.boostedStatRate = modset.getBoostedStatRate();
            this.modifiers = new ArrayList<>();

            for (ModsetData.ModifierDefinition modDef : modset.getModifiers())
                this.modifiers.add(new SerializedModifierDefinition(modDef));
        }

        SerializedModset(FriendlyByteBuf buf) {
            this.id = buf.readUtf();
            String boostedDay = buf.readUtf();
            this.boostedStatDay = boostedDay.isEmpty() ? RealmEngine.Weekday.NONE : RealmEngine.Weekday.valueOf(boostedDay);
            this.boostedStatRate = buf.readDouble();
            int modCount = buf.readInt();
            this.modifiers = new ArrayList<>();
            for (int i = 0; i < modCount; i++)
                this.modifiers.add(new SerializedModifierDefinition(buf));
        }

        void encode(FriendlyByteBuf buf) {
            buf.writeUtf(id);
            buf.writeUtf(boostedStatDay != null ? boostedStatDay.name() : "");
            buf.writeDouble(boostedStatRate);
            buf.writeInt(modifiers.size());

            for (SerializedModifierDefinition mod : modifiers) mod.encode(buf);
        }

        ModsetData.Modset deserialize() {
            ModsetData.Modset modset = new ModsetData.Modset(id);
            modset.setBoostedStatDay(boostedStatDay);
            modset.setBoostedStatRate(boostedStatRate);

            for (SerializedModifierDefinition serialized : modifiers) {
                ModsetData.ModifierDefinition modDef = serialized.deserialize();
                modset.addModifier(modDef);
            }

            return modset;
        }
    }

    /**
     * Serializable version of ModifierDefinition for network transmission
     */
    private static class SerializedModifierDefinition {
        String name;
        String attributeName;
        int operationOrdinal;
        double minValue;
        double maxValue;

        SerializedModifierDefinition(ModsetData.ModifierDefinition modDef) {
            this.name = modDef.name;
            this.attributeName = modDef.attributeName;
            this.operationOrdinal = modDef.operation.ordinal();
            this.minValue = modDef.minValue;
            this.maxValue = modDef.maxValue;
        }

        SerializedModifierDefinition(FriendlyByteBuf buf) {
            this.name = buf.readUtf();
            this.attributeName = buf.readUtf();
            this.operationOrdinal = buf.readInt();
            this.minValue = buf.readDouble();
            this.maxValue = buf.readDouble();
        }

        void encode(FriendlyByteBuf buf) {
            buf.writeUtf(name);
            buf.writeUtf(attributeName);
            buf.writeInt(operationOrdinal);
            buf.writeDouble(minValue);
            buf.writeDouble(maxValue);
        }

        ModsetData.ModifierDefinition deserialize() {
            AttributeModifier.Operation operation = AttributeModifier.Operation.values()[operationOrdinal];
            return new ModsetData.ModifierDefinition(name, attributeName, operation, minValue, maxValue);
        }
    }
}


package com.vetpetmon.realmengine.common.networking;

import com.vetpetmon.realmengine.RealmEngine;
import com.vetpetmon.realmengine.common.attribute.ModsetData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record SyncModsetsPayload(Map<String, Map<String, SyncModsetsPayload.SerializedModset>> data) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncModsetsPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RealmEngine.MODID, "sync_modsets"));

    public static final StreamCodec<ByteBuf, SyncModsetsPayload> STREAM_CODEC = StreamCodec.composite(
            //TODO: Implement a proper codec for SyncModsetsPayload
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    /**
     * Serializable version of Modset for network transmission
     */
    private static class SerializedModset {
        String id;
        RealmEngine.Weekday boostedStatDay;
        double boostedStatRate;
        List<SyncModsetsPayload.SerializedModifierDefinition> modifiers;

        SerializedModset(ModsetData.Modset modset) {
            this.id = modset.id;
            this.boostedStatDay = modset.getBoostedStatDay();
            this.boostedStatRate = modset.getBoostedStatRate();
            this.modifiers = new ArrayList<>();

            for (ModsetData.ModifierDefinition modDef : modset.getModifiers())
                this.modifiers.add(new SyncModsetsPayload.SerializedModifierDefinition(modDef));
        }

        SerializedModset(FriendlyByteBuf buf) {
            this.id = buf.readUtf();
            String boostedDay = buf.readUtf();
            this.boostedStatDay = boostedDay.isEmpty() ? RealmEngine.Weekday.NONE : RealmEngine.Weekday.valueOf(boostedDay);
            this.boostedStatRate = buf.readDouble();
            int modCount = buf.readInt();
            this.modifiers = new ArrayList<>();
            for (int i = 0; i < modCount; i++)
                this.modifiers.add(new SyncModsetsPayload.SerializedModifierDefinition(buf));
        }

        void encode(FriendlyByteBuf buf) {
            buf.writeUtf(id);
            buf.writeUtf(boostedStatDay != null ? boostedStatDay.name() : "");
            buf.writeDouble(boostedStatRate);
            buf.writeInt(modifiers.size());

            for (SyncModsetsPayload.SerializedModifierDefinition mod : modifiers) mod.encode(buf);
        }

        ModsetData.Modset deserialize() {
            ModsetData.Modset modset = new ModsetData.Modset(id);
            modset.setBoostedStatDay(boostedStatDay);
            modset.setBoostedStatRate(boostedStatRate);

            for (SyncModsetsPayload.SerializedModifierDefinition serialized : modifiers) {
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

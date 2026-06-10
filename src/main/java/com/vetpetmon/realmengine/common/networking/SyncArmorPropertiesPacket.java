package com.vetpetmon.realmengine.common.networking;

import com.vetpetmon.realmengine.RealmEngine;
import com.vetpetmon.realmengine.common.armor.ArmorPropertiesData;
import com.vetpetmon.realmengine.common.attribute.RealmEngineAttributeMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Packet to sync armor properties data from server to client.
 * This is needed so that ItemAttributeModifierEvent can properly show tooltips on dedicated servers.
 */
public class SyncArmorPropertiesPacket {
    private final Map<String, Map<String, SerializedArmorProperties>> data;

    public SyncArmorPropertiesPacket(Map<String, Map<String, ArmorPropertiesData.ArmorProperties>> sourceData) {
        this.data = new HashMap<>();

        // Serialize the armor properties data
        for (var namespaceEntry : sourceData.entrySet()) {
            String namespace = namespaceEntry.getKey();
            Map<String, SerializedArmorProperties> serializedMap = new HashMap<>();

            for (var propsEntry : namespaceEntry.getValue().entrySet()) {
                String id = propsEntry.getKey();
                ArmorPropertiesData.ArmorProperties props = propsEntry.getValue();
                serializedMap.put(id, new SerializedArmorProperties(props));
            }

            data.put(namespace, serializedMap);
        }
    }

    public SyncArmorPropertiesPacket(FriendlyByteBuf buf) {
        this.data = new HashMap<>();

        int namespaceCount = buf.readInt();
        for (int i = 0; i < namespaceCount; i++) {
            String namespace = buf.readUtf();
            int propsCount = buf.readInt();
            Map<String, SerializedArmorProperties> propsMap = new HashMap<>();

            for (int j = 0; j < propsCount; j++) {
                String id = buf.readUtf();
                SerializedArmorProperties props = new SerializedArmorProperties(buf);
                propsMap.put(id, props);
            }

            data.put(namespace, propsMap);
        }
    }

    public static void encode(SyncArmorPropertiesPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.data.size());

        for (var namespaceEntry : pkt.data.entrySet()) {
            buf.writeUtf(namespaceEntry.getKey());
            Map<String, SerializedArmorProperties> propsMap = namespaceEntry.getValue();
            buf.writeInt(propsMap.size());

            for (var propsEntry : propsMap.entrySet()) {
                buf.writeUtf(propsEntry.getKey());
                propsEntry.getValue().encode(buf);
            }
        }
    }

    public static SyncArmorPropertiesPacket decode(FriendlyByteBuf buf) {
        return new SyncArmorPropertiesPacket(buf);
    }

    public static void handle(SyncArmorPropertiesPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getDirection().getReceptionSide().isClient()) {
                // Clear existing client data
                ArmorPropertiesData.ARMOR_PROPERTIES.clear();

                // Deserialize and populate client-side armor properties
                for (var namespaceEntry : pkt.data.entrySet()) {
                    String namespace = namespaceEntry.getKey();
                    Map<String, ArmorPropertiesData.ArmorProperties> propsMap = new ConcurrentHashMap<>();

                    for (var serializedEntry : namespaceEntry.getValue().entrySet()) {
                        String id = serializedEntry.getKey();
                        SerializedArmorProperties serialized = serializedEntry.getValue();
                        ArmorPropertiesData.ArmorProperties props = serialized.deserialize();
                        propsMap.put(id, props);
                    }

                    ArmorPropertiesData.ARMOR_PROPERTIES.put(namespace, propsMap);
                }

                RealmEngine.LOGGER.info("Synced armor properties data from server: {} entries", pkt.data.size());
            }
        });
        ctx.setPacketHandled(true);
    }

    /**
     * Serializable version of ArmorProperties for network transmission
     */
    private static class SerializedArmorProperties {
        String id;
        List<ResourceLocation> armorItemIds;
        List<SerializedArmorItemEntry> armorItemEntries;
        int randomModCount;
        List<SerializedArmorMod> applicableMods;

        SerializedArmorProperties(ArmorPropertiesData.ArmorProperties props) {
            this.id = props.id;
            this.randomModCount = props.randomModCount;

            // Serialize armor items as ResourceLocations
            this.armorItemIds = new ArrayList<>();
            if (props.armorItems != null) {
                for (Item item : props.armorItems) {
                    ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
                    if (rl != null) this.armorItemIds.add(rl);
                }
            }

            // Serialize armor item entries
            this.armorItemEntries = new ArrayList<>();
            if (props.armorItemEntries != null) {
                for (ArmorPropertiesData.ArmorItemEntry entry : props.armorItemEntries) {
                    ResourceLocation rl = ForgeRegistries.ITEMS.getKey(entry.item());
                    if (rl != null) this.armorItemEntries.add(new SerializedArmorItemEntry(rl, entry.modsetId()));
                }
            }

            // Serialize armor mods
            this.applicableMods = new ArrayList<>();
            if (props.applicableMods != null)
                for (ArmorPropertiesData.ArmorMod mod : props.applicableMods)
                    this.applicableMods.add(new SerializedArmorMod(mod));
        }

        SerializedArmorProperties(FriendlyByteBuf buf) {
            this.id = buf.readUtf();
            this.randomModCount = buf.readInt();

            int itemCount = buf.readInt();
            this.armorItemIds = new ArrayList<>();
            for (int i = 0; i < itemCount; i++) this.armorItemIds.add(buf.readResourceLocation());

            int entryCount = buf.readInt();
            this.armorItemEntries = new ArrayList<>();
            for (int i = 0; i < entryCount; i++) {
                ResourceLocation rl = buf.readResourceLocation();
                String modsetId = buf.readUtf();
                this.armorItemEntries.add(new SerializedArmorItemEntry(rl, modsetId));
            }

            int modCount = buf.readInt();
            this.applicableMods = new ArrayList<>();
            for (int i = 0; i < modCount; i++) this.applicableMods.add(new SerializedArmorMod(buf));
        }

        void encode(FriendlyByteBuf buf) {
            buf.writeUtf(id);
            buf.writeInt(randomModCount);

            buf.writeInt(armorItemIds.size());
            for (ResourceLocation rl : armorItemIds) buf.writeResourceLocation(rl);

            buf.writeInt(armorItemEntries.size());
            for (SerializedArmorItemEntry entry : armorItemEntries) {
                buf.writeResourceLocation(entry.itemId);
                buf.writeUtf(entry.modsetId != null ? entry.modsetId : "");
            }

            buf.writeInt(applicableMods.size());
            for (SerializedArmorMod mod : applicableMods) mod.encode(buf);
        }

        ArmorPropertiesData.ArmorProperties deserialize() {
            ArmorPropertiesData.ArmorProperties props = new ArmorPropertiesData.ArmorProperties(id);
            props.randomModCount = randomModCount;

            // Deserialize armor items
            props.armorItems = new ArrayList<>();
            for (ResourceLocation rl : armorItemIds) {
                Item item = ForgeRegistries.ITEMS.getValue(rl);
                if (item != null) props.armorItems.add(item);
            }

            // Deserialize armor item entries
            props.armorItemEntries = new ArrayList<>();
            for (SerializedArmorItemEntry entry : armorItemEntries) {
                Item item = ForgeRegistries.ITEMS.getValue(entry.itemId);
                if (item != null) props.armorItemEntries.add(new ArmorPropertiesData.ArmorItemEntry(item, entry.modsetId));
            }

            // Deserialize armor mods
            props.applicableMods = new ArrayList<>();
            for (SerializedArmorMod serialized : applicableMods) {
                ArmorPropertiesData.ArmorMod mod = serialized.deserialize();
                props.applicableMods.add(mod);
            }

            return props;
        }
    }

    private static class SerializedArmorItemEntry {
        ResourceLocation itemId;
        String modsetId;

        SerializedArmorItemEntry(ResourceLocation itemId, String modsetId) {
            this.itemId = itemId;
            this.modsetId = modsetId;
        }
    }

    /**
     * Serializable version of ArmorMod for network transmission
     */
    private static class SerializedArmorMod {
        String itemID;
        List<ResourceLocation> applicableItemIds;
        List<ArmorPropertiesData.ArmorMod.SLOT_NAME> slots;
        boolean optional;
        Map<String, SerializedAttributeMod> modEffects;

        SerializedArmorMod(ArmorPropertiesData.ArmorMod mod) {
            this.itemID = mod.itemID;
            this.optional = mod.optional;
            this.slots = mod.slots;

            // Serialize applicable items
            this.applicableItemIds = new ArrayList<>();
            if (mod.applicableItems != null) {
                for (Item item : mod.applicableItems) {
                    ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
                    if (rl != null) this.applicableItemIds.add(rl);
                }
            }

            // Serialize mod effects
            this.modEffects = new HashMap<>();
            if (mod.modEffects != null) for (var entry : mod.modEffects.entrySet())
                this.modEffects.put(entry.getKey(), new SerializedAttributeMod(entry.getValue()));
        }

        SerializedArmorMod(FriendlyByteBuf buf) {
            this.itemID = buf.readUtf();
            this.optional = buf.readBoolean();

            int slotCount = buf.readInt();
            this.slots = new ArrayList<>();
            for (int i = 0; i < slotCount; i++) {
                String slotName = buf.readUtf();
                try {
                    this.slots.add(ArmorPropertiesData.ArmorMod.SLOT_NAME.valueOf(slotName));
                } catch (IllegalArgumentException e) {
                    // Skip invalid slot names
                }
            }

            int itemCount = buf.readInt();
            this.applicableItemIds = new ArrayList<>();
            for (int i = 0; i < itemCount; i++) this.applicableItemIds.add(buf.readResourceLocation());

            int effectCount = buf.readInt();
            this.modEffects = new HashMap<>();
            for (int i = 0; i < effectCount; i++) {
                String key = buf.readUtf();
                SerializedAttributeMod effect = new SerializedAttributeMod(buf);
                this.modEffects.put(key, effect);
            }
        }

        void encode(FriendlyByteBuf buf) {
            buf.writeUtf(itemID);
            buf.writeBoolean(optional);

            buf.writeInt(slots != null ? slots.size() : 0);
            if (slots != null) for (ArmorPropertiesData.ArmorMod.SLOT_NAME slot : slots)
                buf.writeUtf(slot.name());


            buf.writeInt(applicableItemIds.size());
            for (ResourceLocation rl : applicableItemIds) buf.writeResourceLocation(rl);

            buf.writeInt(modEffects.size());
            for (var entry : modEffects.entrySet()) {
                buf.writeUtf(entry.getKey());
                entry.getValue().encode(buf);
            }
        }

        ArmorPropertiesData.ArmorMod deserialize() {
            // Resolve mod item from registry
            ResourceLocation modRl = ResourceLocation.tryParse(itemID);
            Item modItem = modRl != null ? ForgeRegistries.ITEMS.getValue(modRl) : null;

            // Deserialize applicable items
            List<Item> applicableItems = new ArrayList<>();
            for (ResourceLocation rl : applicableItemIds) {
                Item item = ForgeRegistries.ITEMS.getValue(rl);
                if (item != null) applicableItems.add(item);
            }

            // Deserialize mod effects
            Map<String, RealmEngineAttributeMod> deserializedEffects = new HashMap<>();
            for (var entry : modEffects.entrySet()) {
                RealmEngineAttributeMod effect = entry.getValue().deserialize();
                if (effect != null) deserializedEffects.put(entry.getKey(), effect);
            }

            return new ArmorPropertiesData.ArmorMod(
                itemID,
                modItem,
                applicableItems,
                slots,
                optional,
                deserializedEffects
            );
        }
    }

    /**
     * Serializable version of RealmfallAttributeMod for network transmission
     */
    private static class SerializedAttributeMod {
        String attributeName;
        String modName;
        double amount;
        int operationOrdinal;

        SerializedAttributeMod(RealmEngineAttributeMod mod) {
            this.attributeName = mod.getAttributeRegistryName();
            this.modName = mod.getModiName();
            this.amount = mod.getAmount();
            this.operationOrdinal = mod.getOperation().ordinal();
        }

        SerializedAttributeMod(FriendlyByteBuf buf) {
            this.attributeName = buf.readUtf();
            this.modName = buf.readUtf();
            this.amount = buf.readDouble();
            this.operationOrdinal = buf.readInt();
        }

        void encode(FriendlyByteBuf buf) {
            buf.writeUtf(attributeName);
            buf.writeUtf(modName);
            buf.writeDouble(amount);
            buf.writeInt(operationOrdinal);
        }

        RealmEngineAttributeMod deserialize() {
            AttributeModifier.Operation operation = AttributeModifier.Operation.values()[operationOrdinal];

            // Create a RealmfallAttributeMod based on the operation type
            return switch (operation) {
                case MULTIPLY_BASE -> RealmEngineAttributeMod.createMultiplierModByAttributeName(
                        attributeName,
                        () -> java.util.UUID.nameUUIDFromBytes((modName + ":" + attributeName).getBytes()),
                        () -> modName,
                        () -> amount
                );
                case MULTIPLY_TOTAL -> RealmEngineAttributeMod.createTotalMultiplierModByAttributeName(
                        attributeName,
                        () -> java.util.UUID.nameUUIDFromBytes((modName + ":" + attributeName).getBytes()),
                        () -> modName,
                        () -> amount
                );
                default -> RealmEngineAttributeMod.createFixedModByAttributeName(
                        attributeName,
                        () -> java.util.UUID.nameUUIDFromBytes((modName + ":" + attributeName).getBytes()),
                        () -> modName,
                        () -> amount
                );
            };
        }
    }
}


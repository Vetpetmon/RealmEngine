package com.vetpetmon.realmengine.common.networking;

import com.vetpetmon.realmengine.RealmEngine;
import com.vetpetmon.realmengine.common.item.ItemPropertiesData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
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
 * Packet to sync item properties data from server to client.
 */
public class SyncItemPropertiesPacket {
    private final Map<String, Map<String, SerializedItemProperties>> data;

    public SyncItemPropertiesPacket(Map<String, Map<String, ItemPropertiesData.ItemProperties>> sourceData) {
        this.data = new HashMap<>();

        for (var namespaceEntry : sourceData.entrySet()) {
            String namespace = namespaceEntry.getKey();
            Map<String, SerializedItemProperties> serializedMap = new HashMap<>();

            for (var propsEntry : namespaceEntry.getValue().entrySet()) {
                String id = propsEntry.getKey();
                ItemPropertiesData.ItemProperties props = propsEntry.getValue();
                serializedMap.put(id, new SerializedItemProperties(props));
            }

            data.put(namespace, serializedMap);
        }
    }

    public SyncItemPropertiesPacket(FriendlyByteBuf buf) {
        this.data = new HashMap<>();

        int namespaceCount = buf.readInt();
        for (int i = 0; i < namespaceCount; i++) {
            String namespace = buf.readUtf();
            int propsCount = buf.readInt();
            Map<String, SerializedItemProperties> propsMap = new HashMap<>();

            for (int j = 0; j < propsCount; j++) {
                String id = buf.readUtf();
                SerializedItemProperties props = new SerializedItemProperties(buf);
                propsMap.put(id, props);
            }

            data.put(namespace, propsMap);
        }
    }

    public static void encode(SyncItemPropertiesPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.data.size());

        for (var namespaceEntry : pkt.data.entrySet()) {
            buf.writeUtf(namespaceEntry.getKey());
            Map<String, SerializedItemProperties> propsMap = namespaceEntry.getValue();
            buf.writeInt(propsMap.size());

            for (var propsEntry : propsMap.entrySet()) {
                buf.writeUtf(propsEntry.getKey());
                propsEntry.getValue().encode(buf);
            }
        }
    }

    public static SyncItemPropertiesPacket decode(FriendlyByteBuf buf) {
        return new SyncItemPropertiesPacket(buf);
    }

    public static void handle(SyncItemPropertiesPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getDirection().getReceptionSide().isClient()) {
                ItemPropertiesData.ITEM_PROPERTIES.clear();

                for (var namespaceEntry : pkt.data.entrySet()) {
                    String namespace = namespaceEntry.getKey();
                    Map<String, ItemPropertiesData.ItemProperties> propsMap = new ConcurrentHashMap<>();

                    for (var serializedEntry : namespaceEntry.getValue().entrySet()) {
                        String id = serializedEntry.getKey();
                        SerializedItemProperties serialized = serializedEntry.getValue();
                        ItemPropertiesData.ItemProperties props = serialized.deserialize();
                        propsMap.put(id, props);
                    }

                    ItemPropertiesData.ITEM_PROPERTIES.put(namespace, propsMap);
                }

                RealmEngine.LOGGER.info("Synced item properties data from server: {} entries", pkt.data.size());
            }
        });
        ctx.setPacketHandled(true);
    }

    private static class SerializedItemProperties {
        String id;
        List<ResourceLocation> itemIds;
        List<SerializedItemEntry> itemEntries;
        int randomModCount;

        SerializedItemProperties(ItemPropertiesData.ItemProperties props) {
            this.id = props.id;
            this.randomModCount = props.randomModCount;

            this.itemIds = new ArrayList<>();
            if (props.items != null) {
                for (Item item : props.items) {
                    ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
                    if (rl != null) this.itemIds.add(rl);
                }
            }

            this.itemEntries = new ArrayList<>();
            if (props.itemEntries != null) {
                for (ItemPropertiesData.ItemEntry entry : props.itemEntries) {
                    ResourceLocation rl = ForgeRegistries.ITEMS.getKey(entry.item());
                    if (rl != null) this.itemEntries.add(new SerializedItemEntry(rl, entry.modsetId()));
                }
            }
        }

        SerializedItemProperties(FriendlyByteBuf buf) {
            this.id = buf.readUtf();
            this.randomModCount = buf.readInt();

            int itemCount = buf.readInt();
            this.itemIds = new ArrayList<>();
            for (int i = 0; i < itemCount; i++) this.itemIds.add(buf.readResourceLocation());

            int entryCount = buf.readInt();
            this.itemEntries = new ArrayList<>();
            for (int i = 0; i < entryCount; i++) {
                ResourceLocation rl = buf.readResourceLocation();
                String modsetId = buf.readUtf();
                this.itemEntries.add(new SerializedItemEntry(rl, modsetId));
            }
        }

        void encode(FriendlyByteBuf buf) {
            buf.writeUtf(id);
            buf.writeInt(randomModCount);

            buf.writeInt(itemIds.size());
            for (ResourceLocation rl : itemIds) buf.writeResourceLocation(rl);

            buf.writeInt(itemEntries.size());
            for (SerializedItemEntry entry : itemEntries) {
                buf.writeResourceLocation(entry.itemId);
                buf.writeUtf(entry.modsetId != null ? entry.modsetId : "");
            }
        }

        ItemPropertiesData.ItemProperties deserialize() {
            ItemPropertiesData.ItemProperties props = new ItemPropertiesData.ItemProperties(id);
            props.randomModCount = randomModCount;

            props.items = new ArrayList<>();
            for (ResourceLocation rl : itemIds) {
                Item item = ForgeRegistries.ITEMS.getValue(rl);
                if (item != null) props.items.add(item);
            }

            props.itemEntries = new ArrayList<>();
            for (SerializedItemEntry entry : itemEntries) {
                Item item = ForgeRegistries.ITEMS.getValue(entry.itemId);
                if (item != null) props.itemEntries.add(new ItemPropertiesData.ItemEntry(item, entry.modsetId));
            }

            return props;
        }
    }

    private static class SerializedItemEntry {
        ResourceLocation itemId;
        String modsetId;

        SerializedItemEntry(ResourceLocation itemId, String modsetId) {
            this.itemId = itemId;
            this.modsetId = modsetId;
        }
    }
}


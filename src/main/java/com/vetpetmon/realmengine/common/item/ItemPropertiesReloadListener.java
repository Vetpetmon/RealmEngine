package com.vetpetmon.realmengine.common.item;

import com.google.gson.*;
import com.vetpetmon.realmengine.RealmEngine;
import com.vetpetmon.realmengine.common.networking.SyncItemPropertiesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.vetpetmon.realmengine.common.item.ItemPropertiesData.ITEM_PROPERTIES;

public class ItemPropertiesReloadListener extends SimpleJsonResourceReloadListener {
    // String identifier for item properties in datapacks
    public static final String ITEM_PROPERTIES_RESOURCE_FOLDER = "item_properties";

    private static final Gson GSON = new GsonBuilder().create();

    public ItemPropertiesReloadListener() {
        super(GSON, ITEM_PROPERTIES_RESOURCE_FOLDER);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceLocationJsonElementMap, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        for (Map.Entry<ResourceLocation, JsonElement> e : resourceLocationJsonElementMap.entrySet()) {
            ResourceLocation rl = e.getKey();
            JsonElement je = e.getValue();
            try {
                JsonObject root = je.getAsJsonObject();
                String id = root.has("id") ? root.get("id").getAsString() : rl.getPath();

                ItemPropertiesData.ItemProperties props = new ItemPropertiesData.ItemProperties(id);

                // Optional flag: if true, skip error logging for missing items
                boolean optional = root.has("optional") && root.get("optional").isJsonPrimitive() && root.get("optional").getAsBoolean();

                // Parse item entries ("items")
                JsonArray itemsArray = null;
                if (root.has("items") && root.get("items").isJsonArray()) itemsArray = root.getAsJsonArray("items");

                List<Item> items = new ArrayList<>();
                List<ItemPropertiesData.ItemEntry> itemEntries = new ArrayList<>();
                if (itemsArray != null) {
                    for (JsonElement itemEl : itemsArray) {
                        String itemId = null, modsetId = null;

                        if (itemEl.isJsonObject()) {
                            JsonObject itemObj = itemEl.getAsJsonObject();
                            if (itemObj.has("item_id")) itemId = itemObj.get("item_id").getAsString();
                            if (itemObj.has("modset")) modsetId = itemObj.get("modset").getAsString();
                        } else if (itemEl.isJsonPrimitive()) itemId = itemEl.getAsString();

                        if (itemId != null) {
                            ResourceLocation itemRl = ResourceLocation.tryParse(itemId);
                            if (itemRl != null) {
                                Item item = ForgeRegistries.ITEMS.getValue(itemRl);
                                if (item != null) {
                                    if (item instanceof ArmorItem) {
                                        if (!optional) RealmEngine.LOGGER.warn("ItemProperties: armor item '{}' is not allowed in {}; skipping", itemId, rl); continue;
                                    }
                                    items.add(item);
                                    itemEntries.add(new ItemPropertiesData.ItemEntry(item, modsetId));
                                } else if (!optional) {RealmEngine.LOGGER.error("ItemProperties: unknown item '{}' in {}", itemId, rl);}
                            }
                        }
                    }
                }
                props.items = items;
                props.itemEntries = itemEntries;

                // Parse random mod count if present
                if (root.has("random_mod_count") && root.get("random_mod_count").isJsonPrimitive())
                    props.randomModCount = root.get("random_mod_count").getAsInt();

                // Store into ITEM_PROPERTIES under the namespace (mod id)
                String namespace = rl.getNamespace();
                if (root.has("replace") && root.get("replace").isJsonPrimitive() && root.get("replace").getAsBoolean())
                    ITEM_PROPERTIES.put(namespace, new ConcurrentHashMap<>());

                Map<String, ItemPropertiesData.ItemProperties> namespaceMap = ITEM_PROPERTIES.computeIfAbsent(namespace, k -> new ConcurrentHashMap<>());
                namespaceMap.put(props.id, props);

            } catch (Exception ex) {
                RealmEngine.LOGGER.error("Failed to parse item properties from {}: {}", e.getKey(), ex.getMessage());
            }
        }

        syncToAllPlayers();
    }

    /**
     * Sync item properties data to all connected players
     */
    private void syncToAllPlayers() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                RealmEngine.PACKET_HANDLER.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new SyncItemPropertiesPacket(ITEM_PROPERTIES)
                );
            }
            RealmEngine.LOGGER.info("Synced item properties to {} connected players", server.getPlayerList().getPlayerCount());
        }
    }
}


package com.vetpetmon.realmengine.common.item;

import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused") // Library data container.
public class ItemPropertiesData {

    public static final Map<String, Map<String, ItemProperties>> ITEM_PROPERTIES = new ConcurrentHashMap<>();

    /**
     * Entry for a non-armor item with its associated modset.
     * @param item the item (e.g. "realmengine:celestial_sword")
     * @param modsetId e.g., "bisccel:common_sword"
     */
    public record ItemEntry(Item item, String modsetId) { }

    public static class ItemProperties {
        public final String id; // Unique identifier (name) for these item properties
        // List of items for set
        public List<Item> items;
        // List of item entries with modset references
        public List<ItemEntry> itemEntries;
        public int randomModCount = 0; // Track how many random mods this set has for use in mod application logic

        public ItemProperties(String id) {
            this.id = id;
        }

        /**
         * Get the modset ID for a specific item
         * @param item the item to look up
         * @return the modset ID or null if not found
         */
        public String getModsetIdForItem(Item item) {
            if (itemEntries == null) return null;
            for (ItemEntry entry : itemEntries) if (entry.item() == item) return entry.modsetId();
            return null;
        }
    }
}


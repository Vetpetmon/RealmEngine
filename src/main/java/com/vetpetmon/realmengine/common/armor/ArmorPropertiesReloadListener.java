package com.vetpetmon.realmengine.common.armor;

import com.google.gson.*;
import com.vetpetmon.realmengine.RealmEngine;
import com.vetpetmon.realmengine.common.attribute.RealmEngineAttributeMod;
import com.vetpetmon.realmengine.common.networking.SyncArmorPropertiesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static com.vetpetmon.realmengine.common.armor.ArmorPropertiesData.ARMOR_PROPERTIES;

public class ArmorPropertiesReloadListener extends SimpleJsonResourceReloadListener {
    // String identifier for armor properties in datapacks
    public static final String ARMOR_PROPERTIES_RESOURCE_FOLDER = "armor_properties";

    private static final Gson GSON = new GsonBuilder().create();

    public ArmorPropertiesReloadListener() {
        super(GSON, ARMOR_PROPERTIES_RESOURCE_FOLDER);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceLocationJsonElementMap, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        // Clear or reload namespace entries as appropriate. We'll replace maps for namespaces encountered.
        for (Map.Entry<ResourceLocation, JsonElement> e : resourceLocationJsonElementMap.entrySet()) {
            ResourceLocation rl = e.getKey();
            JsonElement je = e.getValue();
            try {
                JsonObject root = je.getAsJsonObject();
                String id = root.has("id") ? root.get("id").getAsString() : rl.getPath();

                ArmorPropertiesData.ArmorProperties props = new ArmorPropertiesData.ArmorProperties(id);

                // Check for "optional" boolean flag: if true, if a missing item or mod is encountered, skip loading this armor properties entry rather than erroring
                boolean optional = root.has("optional") && root.get("optional").isJsonPrimitive() && root.get("optional").getAsBoolean();
                // AKA: if "optional" is true, then if we encounter any unknown items or mods while parsing,
                // we will skip error logging.

                // Parse armor items
                List<Item> armorItems = new ArrayList<>();
                List<ArmorPropertiesData.ArmorItemEntry> armorItemEntries = new ArrayList<>();
                if (root.has("armor_items") && root.get("armor_items").isJsonArray()) {
                    JsonArray arr = root.getAsJsonArray("armor_items");
                    for (JsonElement itemEl : arr) {
                        String itemId = null, modsetId = null;
                        
                        if (itemEl.isJsonObject()) {
                            JsonObject itemObj = itemEl.getAsJsonObject();
                            if (itemObj.has("item_id")) itemId = itemObj.get("item_id").getAsString();
                            if (itemObj.has("modset")) modsetId = itemObj.get("modset").getAsString();
                        }
                        else if (itemEl.isJsonPrimitive()) itemId = itemEl.getAsString();
                        
                        if (itemId != null) {
                            ResourceLocation itemRl = ResourceLocation.tryParse(itemId);
                            if (itemRl != null) {
                                Item item = ForgeRegistries.ITEMS.getValue(itemRl);
                                if (item != null) {
                                    armorItems.add(item);
                                    armorItemEntries.add(new ArmorPropertiesData.ArmorItemEntry(item, modsetId));
                                }
                                else if (!optional) RealmEngine.LOGGER.error("ArmorProperties: unknown item '{}' in {}", itemId, rl);
                            }
                        }
                    }
                }
                props.armorItems = armorItems;
                props.armorItemEntries = armorItemEntries;

                // Parse random mod count if present
                if (root.has("random_mod_count") && root.get("random_mod_count").isJsonPrimitive()) {
                    props.randomModCount = root.get("random_mod_count").getAsInt();
                }

                // Parse applicable armor mods
                List<ArmorPropertiesData.ArmorMod> applicableMods = new ArrayList<>();
                if (root.has("applicable_armor_mods") && root.get("applicable_armor_mods").isJsonArray()) {
                    JsonArray arr = root.getAsJsonArray("applicable_armor_mods");
                    for (JsonElement modEl : arr) {
                        String modId = null;
                        boolean modOptional = false; // per-mod optional flag
                        List<ArmorPropertiesData.ArmorMod.SLOT_NAME> slots = null;
                        Map<String, RealmEngineAttributeMod> modEffects = null;
                        if (modEl.isJsonObject()) {
                            JsonObject mo = modEl.getAsJsonObject();
                            if (mo.has("id")) modId = mo.get("id").getAsString();
                            if (mo.has("optional") && mo.get("optional").isJsonPrimitive()) modOptional = mo.get("optional").getAsBoolean();
                            if (mo.has("slots") && mo.get("slots").isJsonArray()) {
                                slots = new ArrayList<>();
                                for (JsonElement s : mo.getAsJsonArray("slots")) {
                                    if (s.isJsonPrimitive()) {
                                        try {
                                            String sl = s.getAsString();
                                            ArmorPropertiesData.ArmorMod.SLOT_NAME sn = ArmorPropertiesData.ArmorMod.SLOT_NAME.valueOf(sl.toUpperCase());
                                            slots.add(sn);
                                        } catch (Exception ex) {
                                            RealmEngine.LOGGER.warn("Unknown slot '{}' for armor mod in {}", s.getAsString(), rl);
                                        }
                                    }
                                }
                            }

                            // Parse per-mod effects: support both 'mod_effects' and 'mod_effects:' keys to be tolerant
                            String[] modEffectsKeys = new String[]{"mod_effects", "mod_effects:"};
                            for (String key : modEffectsKeys) {
                                if (mo.has(key) && mo.get(key).isJsonArray()) {
                                    JsonArray effs = mo.getAsJsonArray(key);
                                    modEffects = new ConcurrentHashMap<>();
                                    for (JsonElement effEl : effs) {
                                        if (!effEl.isJsonObject()) continue;
                                        JsonObject effObj = effEl.getAsJsonObject();
                                        String effectName = effObj.has("name") ? effObj.get("name").getAsString() : null;
                                        // Create the attribute modifier; use modOptional OR top-level optional to decide logging
                                        RealmEngineAttributeMod ram = parseAttributeModifier(effObj, (modOptional || optional));
                                        if (ram != null) {
                                            String keyName = effectName != null ? effectName : ram.getModiName();
                                            modEffects.put(keyName, ram);
                                        }
                                    }
                                    break; // stop after first present key
                                }
                            }
                        } else if (modEl.isJsonPrimitive()) modId = modEl.getAsString();
                        if (modId != null) {
                            ResourceLocation modRl = ResourceLocation.tryParse(modId);
                            Item item = null;
                            if (modRl != null) {
                                item = ForgeRegistries.ITEMS.getValue(modRl);
                                if (item == null && !modOptional && !optional) RealmEngine.LOGGER.error("ArmorProperties: unknown mod item '{}' in {}", modId, rl);
                            }
                            // Build applicableItems: if slots are provided, we'll filter armorItems by EquipmentSlot
                            List<Item> appItems = new ArrayList<>();
                            if (slots != null && !slots.isEmpty()) {
                                for (Item armorItem : armorItems) {
                                    if (armorItem instanceof ArmorItem ai) {
                                        EquipmentSlot es = ai.getEquipmentSlot();
                                        // If the slot matches one of the defined slots, add to applicable items
                                        if (slots.contains(ArmorPropertiesData.ArmorMod.fromEquipmentSlot(es)))
                                            appItems.add(armorItem);
                                    }
                                }
                            }
                            ArmorPropertiesData.ArmorMod am = new ArmorPropertiesData.ArmorMod(modId, item, appItems, slots, modOptional, modEffects);
                            applicableMods.add(am);
                        }
                    }
                }
                props.applicableMods = applicableMods;

                // Parse set effects
                if (root.has("set_effects") && root.get("set_effects").isJsonArray()) {
                    JsonArray arr = root.getAsJsonArray("set_effects");
                    for (JsonElement effectEl : arr) {
                        if (!effectEl.isJsonObject()) continue;
                        JsonObject effObj = effectEl.getAsJsonObject();
                        String effectName = effObj.has("name") ? effObj.get("name").getAsString() : null;
                        if (effectName == null) continue;
                        ArmorPropertiesData.SetEffect setEffect = new ArmorPropertiesData.SetEffect(effectName);

                        String attributeName = effObj.has("attribute_name") ? effObj.get("attribute_name").getAsString() : null;
                        RealmEngineAttributeMod mod = parseAttributeModifier(effObj, optional); // if optional is true, we'll skip logging errors for unknown attributes and just not add the modifier
                        if (attributeName != null && mod != null) setEffect.addAttributeModifier(attributeName, mod);

                        props.addSetEffect(effectName, setEffect);
                    }
                    RealmEngine.LOGGER.debug("Loaded armor properties '{}' with {} armor items, {} applicable mods, and {} set effects from {}",
                            props.id, props.armorItems.size(), props.applicableMods.size(), props.setEffects.size(), rl);
                }

                // Store into ARMOR_PROPERTIES under the namespace (mod id)
                String namespace = rl.getNamespace();
                // Honor a top-level "replace" boolean: if true, clear any existing entries for this namespace
                if (root.has("replace") && root.get("replace").isJsonPrimitive() && root.get("replace").getAsBoolean()) {
                    ARMOR_PROPERTIES.put(namespace, new ConcurrentHashMap<>());
                }

                Map<String, ArmorPropertiesData.ArmorProperties> namespaceMap = ARMOR_PROPERTIES.computeIfAbsent(namespace, k -> new ConcurrentHashMap<>());
                namespaceMap.put(props.id, props);

            } catch (Exception ex) {
                RealmEngine.LOGGER.error("Failed to parse armor properties from {}: {}", e.getKey(), ex.getMessage());
                // stack traces are noisy in datapack parsing; message logged above
            }
        }

        // After all armor properties are loaded, sync to all connected clients
        syncToAllPlayers();
    }

    // Parse each attribute modifier from JSON and build RealmfallAttributeMod objects
    private RealmEngineAttributeMod parseAttributeModifier(JsonElement json, boolean optional) {
        if (json == null || !json.isJsonObject()) return null;
        JsonObject obj = json.getAsJsonObject();
        String attributeName = obj.has("attribute_name") ? obj.get("attribute_name").getAsString() : null;
        if (attributeName == null || attributeName.isEmpty()) return null;

        // Normalize attribute name early so supplier UUIDs include normalized name
        String normalizedAttributeName = normalizeAttributeName(attributeName);
        // Check if it resolves to a valid attribute; if not, log error and skip this modifier
        if (RealmEngineAttributeMod.getAttributeFromString(normalizedAttributeName) == null) {
            if (!optional) RealmEngine.LOGGER.error("Unknown attribute '{}' for armor set effect in {}", attributeName, json);
            return null;
        }

        double value = obj.has("value") ? obj.get("value").getAsDouble() : 0.0;
        String op = obj.has("modifier_operation") ? obj.get("modifier_operation").getAsString().toLowerCase() : "addition";
        String effectName = obj.has("name") ? obj.get("name").getAsString() : ("mod_" + normalizedAttributeName);

        // Supplier for UUID and name and value (lazy)
        Supplier<UUID> uuidSupplier = () -> UUID.nameUUIDFromBytes((effectName + ":" + normalizedAttributeName).getBytes());
        Supplier<String> nameSupplier = () -> effectName;
        Supplier<Double> valueSupplier = () -> value;

        try {
            if (op.contains("multiply") && op.contains("base"))
                return RealmEngineAttributeMod.createMultiplierModByAttributeName(normalizedAttributeName, uuidSupplier, nameSupplier, valueSupplier);
            else if (op.contains("multiply") && (op.contains("total") || op.contains("overall")))
                return RealmEngineAttributeMod.createTotalMultiplierModByAttributeName(normalizedAttributeName, uuidSupplier, nameSupplier, valueSupplier);
            else // default to addition
                return RealmEngineAttributeMod.createFixedModByAttributeName(normalizedAttributeName, uuidSupplier, nameSupplier, valueSupplier);
        } catch (Exception ex) {
            RealmEngine.LOGGER.error("Failed to create attribute modifier for attribute '{}': {}", attributeName, ex.getMessage());
            return null;
        }
    }

    // Normalize attribute registry names and correct common misnames (returns original if no better match found)
    private String normalizeAttributeName(String attributeName) {
        if (attributeName == null) return null;
        // If already resolves, return as-is
        if (RealmEngineAttributeMod.getAttributeFromString(attributeName) != null) return attributeName;

        // Try common toughness alias: replace "toughness" with "armor_toughness"
        if (attributeName.contains("toughness") && !attributeName.contains("armor_toughness")) {
            String candidate = attributeName.replace("toughness", "armor_toughness");
            if (RealmEngineAttributeMod.getAttributeFromString(candidate) != null) return candidate;
        }

        // If no namespace provided, try adding minecraft: prefix
        if (!attributeName.contains(":")) {
            String candidate = "minecraft:" + attributeName;
            if (RealmEngineAttributeMod.getAttributeFromString(candidate) != null) return candidate;
            if (attributeName.contains("toughness")) {
                String candidate2 = "minecraft:" + attributeName.replace("toughness", "armor_toughness");
                if (RealmEngineAttributeMod.getAttributeFromString(candidate2) != null) return candidate2;
            }
        }

        // No normalization found, return original
        return attributeName;
    }

    /**
     * Sync armor properties data to all connected players
     */
    private void syncToAllPlayers() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                RealmEngine.PACKET_HANDLER.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new SyncArmorPropertiesPacket(ARMOR_PROPERTIES)
                );
            }
            RealmEngine.LOGGER.info("Synced armor properties to {} connected players", server.getPlayerList().getPlayerCount());
        }
    }
}

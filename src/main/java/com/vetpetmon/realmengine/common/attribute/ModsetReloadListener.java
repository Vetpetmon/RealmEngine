package com.vetpetmon.realmengine.common.attribute;

import com.google.gson.*;
import com.vetpetmon.realmengine.RealmEngine;
import com.vetpetmon.realmengine.common.networking.SyncModsetsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModsetReloadListener extends SimpleJsonResourceReloadListener {
    // String identifier for randomly picked armor and gear mods:
    public static final String MODSETS_RESOURCE_FOLDER = "modsets";

    private static final Gson GSON = new GsonBuilder().create();

    public ModsetReloadListener() {
        super(GSON, MODSETS_RESOURCE_FOLDER);
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, JsonElement> resourceLocationJsonElementMap, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        // Clear existing modsets or prepare for reload
        for (Map.Entry<ResourceLocation, JsonElement> entry : resourceLocationJsonElementMap.entrySet()) {
            ResourceLocation resourceLocation = entry.getKey();
            JsonElement jsonElement = entry.getValue();

            try {
                if (!jsonElement.isJsonObject()) {
                    RealmEngine.LOGGER.warn("Modset file {} is not a valid JSON object, skipping", resourceLocation);
                    continue;
                }

                JsonObject root = jsonElement.getAsJsonObject();

                // Read the modset ID
                String modsetId = root.has("id") ? root.get("id").getAsString() : resourceLocation.getPath();

                // Check for "replace" flag - if true, clear existing entry for this modset ID before loading new one
                boolean replace = root.has("replace") && root.get("replace").isJsonPrimitive() && root.get("replace").getAsBoolean();

                String namespace = resourceLocation.getNamespace();
                if (replace) ModsetData.clearEntry(resourceLocation); // used to override existing data with same ID across multiple files


                // Create a new Modset
                ModsetData.Modset modset = new ModsetData.Modset(modsetId);

                // Parse optional boosted stat settings
                if (root.has("boosted_stat_day") && root.get("boosted_stat_day").isJsonPrimitive()) {
                    String dayRaw = root.get("boosted_stat_day").getAsString();
                    modset.setBoostedStatDay(parseWeekday(dayRaw));
                } else {
                    modset.setBoostedStatDay(RealmEngine.Weekday.NONE);
                }
                if (root.has("boosted_stat_rate") && root.get("boosted_stat_rate").isJsonPrimitive())
                    modset.setBoostedStatRate(root.get("boosted_stat_rate").getAsDouble());

                RealmEngine.LOGGER.debug("Loaded modset '{}' boosted_stat_day={} boosted_stat_rate={}", modsetId, modset.getBoostedStatDay(), modset.getBoostedStatRate());

                // Parse the modifiers array
                if (root.has("modifiers") && root.get("modifiers").isJsonArray()) {
                    JsonArray modifiersArray = root.getAsJsonArray("modifiers");

                    for (JsonElement modElement : modifiersArray) {
                        if (!modElement.isJsonObject()) continue;

                        JsonObject modObj = modElement.getAsJsonObject();

                        // Parse modifier fields
                        String name = modObj.has("name") ? modObj.get("name").getAsString() : null;
                        String attributeName = modObj.has("attribute_name") ? modObj.get("attribute_name").getAsString() : null;
                        String operationStr = modObj.has("modifier_operation") ? modObj.get("modifier_operation").getAsString().toLowerCase() : "addition";
                        double minValue = modObj.has("min_value") ? modObj.get("min_value").getAsDouble() : 0.0;
                        double maxValue = modObj.has("max_value") ? modObj.get("max_value").getAsDouble() : 0.0;

                        // Validate required fields
                        if (name == null || attributeName == null) {
                            RealmEngine.LOGGER.warn("Modset {} has modifier with missing name or attribute_name, skipping", resourceLocation); continue;
                        }
                        // Normalize attribute name
                        String normalizedAttributeName = normalizeAttributeName(attributeName);
                        // Parse operation type
                        AttributeModifier.Operation operation = parseOperation(operationStr);
                        // Create the modifier definition
                        ModsetData.ModifierDefinition modDef = new ModsetData.ModifierDefinition(
                            name, normalizedAttributeName, operation, minValue, maxValue
                        );

                        modset.addModifier(modDef);
                    }
                }

                // Store the modset
                Map<String, ModsetData.Modset> namespaceMap = ModsetData.MODSETS.computeIfAbsent(
                    namespace, k -> new ConcurrentHashMap<>()
                );
                namespaceMap.put(modsetId, modset);

                RealmEngine.LOGGER.debug("Loaded modset '{}' with {} modifiers from {}",
                    modsetId, modset.getModifiers().size(), resourceLocation);

            } catch (Exception ex) {
                RealmEngine.LOGGER.error("Failed to parse modset from {}: {}", resourceLocation, ex.getMessage(), ex);
            }
        }

        // After all modsets are loaded, sync to all connected clients
        syncToAllPlayers();
    }

    /**
     * Parse operation string to AttributeModifier.Operation enum
     */
    private AttributeModifier.Operation parseOperation(String operationStr) {
        if (operationStr == null) return AttributeModifier.Operation.ADDITION;

        String normalized = operationStr.toLowerCase().trim();
        if (normalized.contains("multiply") && normalized.contains("base"))
            return AttributeModifier.Operation.MULTIPLY_BASE;
        else if (normalized.contains("multiply") && (normalized.contains("total") || normalized.contains("overall")))
            return AttributeModifier.Operation.MULTIPLY_TOTAL;
        else
            return AttributeModifier.Operation.ADDITION;
    }

    /**
     * Normalize attribute registry names and correct common misnames
     */
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

    private RealmEngine.Weekday parseWeekday(String dayRaw) {
        if (dayRaw == null || dayRaw.isBlank()) return RealmEngine.Weekday.NONE;
        try {
            return RealmEngine.Weekday.valueOf(dayRaw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            RealmEngine.LOGGER.warn("Unknown boosted_stat_day '{}' in modset JSON", dayRaw);
            return RealmEngine.Weekday.NONE;
        }
    }

    /**
     * Sync modset data to all connected players
     */
    private void syncToAllPlayers() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers())
                RealmEngine.PACKET_HANDLER.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new SyncModsetsPacket(ModsetData.MODSETS)
                );
            RealmEngine.LOGGER.info("Synced modsets to {} connected players", server.getPlayerList().getPlayerCount());
        }
    }
}
